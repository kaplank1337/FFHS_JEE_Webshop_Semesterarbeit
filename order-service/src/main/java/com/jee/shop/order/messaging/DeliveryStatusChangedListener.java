package com.jee.shop.order.messaging;

import com.jee.shop.order.entity.OrderStatus;
import com.jee.shop.order.service.OrderService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.UUID;

/** Consumes shipping status events and updates the corresponding order. */
@Singleton
@Startup
@DependsOn("OrderDatabaseMigrator")
public class DeliveryStatusChangedListener implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryStatusChangedListener.class);

    @Inject
    private OrderService orderService;

    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    @PostConstruct
    public void start() {
        String brokerUrl = "tcp://" + System.getenv().getOrDefault("JMS_HOST", "localhost")
                + ":" + System.getenv().getOrDefault("JMS_PORT", "61616");
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            connection = factory.createConnection();
            connection.setClientID("order-service-delivery-status");
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic("delivery.status.changed");
            consumer = session.createDurableConsumer(topic, "OrderServiceSubscriber");
            consumer.setMessageListener(this);
            connection.start();
            logger.info("Listening to ActiveMQ topic delivery.status.changed at {}", brokerUrl);
        } catch (JMSException e) {
            close();
            throw new IllegalStateException("Could not subscribe to delivery.status.changed", e);
        }
    }

    @PreDestroy
    public void close() {
        closeQuietly(consumer);
        closeQuietly(session);
        closeQuietly(connection);
    }

    @Override
    public void onMessage(Message message) {
        if (!(message instanceof TextMessage textMessage)) {
            logger.warn("Ignoring non-text DeliveryStatusChanged message: {}", message.getClass().getName());
            return;
        }

        try (JsonReader reader = Json.createReader(new StringReader(textMessage.getText()))) {
            JsonObject event = reader.readObject();
            UUID orderId = UUID.fromString(event.getString("orderId"));
            String shippingStatus = event.getString("status");
            OrderStatus orderStatus = mapShippingToOrderStatus(shippingStatus);
            orderService.updateOrderStatus(orderId, orderStatus);
            logger.info("Order {} status changed to {} from delivery event", orderId, orderStatus);
        } catch (Exception e) {
            logger.error("Could not process DeliveryStatusChanged event", e);
        }
    }

    private OrderStatus mapShippingToOrderStatus(String shippingStatus) {
        return switch (shippingStatus) {
            case "PREPARING" -> OrderStatus.CONFIRMED;
            case "READY_FOR_SHIPMENT" -> OrderStatus.SHIPPING;
            case "SHIPPED" -> OrderStatus.SHIPPED;
            case "DELIVERED" -> OrderStatus.DELIVERED;
            case "CANCELLED" -> OrderStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Unknown shipping status: " + shippingStatus);
        };
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            logger.debug("Error closing ActiveMQ resource", e);
        }
    }
}
