package com.jee.shop.shipping.messaging;

import com.jee.shop.shipping.entity.ShipmentEntity;
import com.jee.shop.shipping.entity.ShippingStatus;
import com.jee.shop.shipping.service.ShipmentDeliverySimulator;
import com.jee.shop.shipping.service.ShippingService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.inject.Inject;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.UUID;

/** Consumes OrderCreated JSON messages and creates/schedules their shipments. */
@Singleton
@Startup
@DependsOn("ShippingDatabaseMigrator")
public class OrderCreatedEventListener implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreatedEventListener.class);

    @Inject
    private ShippingService shippingService;

    @Inject
    private ShipmentDeliverySimulator deliverySimulator;

    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    @PostConstruct
    public void start() {
        String brokerUrl = "tcp://" + System.getenv().getOrDefault("JMS_HOST", "localhost")
                + ":" + System.getenv().getOrDefault("JMS_PORT", "61616");
        try {
            connection = new ActiveMQConnectionFactory(brokerUrl).createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue("order.created");
            consumer = session.createConsumer(queue);
            consumer.setMessageListener(this);
            connection.start();
            logger.info("Listening to ActiveMQ queue order.created at {}", brokerUrl);
        } catch (JMSException e) {
            close();
            throw new IllegalStateException("Could not subscribe to order.created", e);
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
            logger.warn("Ignoring non-text OrderCreated message: {}", message.getClass().getName());
            return;
        }

        try (JsonReader reader = Json.createReader(new StringReader(textMessage.getText()))) {
            JsonObject event = reader.readObject();
            UUID orderId = UUID.fromString(event.getString("orderId"));
            UUID userId = UUID.fromString(event.getString("userId"));

            if (shippingService.getShipmentByOrderId(orderId).isPresent()) {
                logger.info("Ignoring duplicate OrderCreated event for order {}", orderId);
                return;
            }

            ShipmentEntity shipment = shippingService.createShipmentForOrder(orderId, userId);
            // Move through the shipment states immediately so the order service can
            // publish "Wird geliefert" while the simulated delivery is in progress.
            shippingService.updateShipmentStatus(shipment.getId(), ShippingStatus.READY_FOR_SHIPMENT);
            shippingService.updateShipmentStatus(shipment.getId(), ShippingStatus.SHIPPED);
            long seconds = deliverySimulator.schedule(shipment.getId());
            logger.info("Shipment {} created for order {}; demo delivery is scheduled in {} seconds",
                    shipment.getId(), orderId, seconds);
        } catch (Exception e) {
            logger.error("Could not process OrderCreated message", e);
            throw new RuntimeException("OrderCreated message processing failed", e);
        }
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
