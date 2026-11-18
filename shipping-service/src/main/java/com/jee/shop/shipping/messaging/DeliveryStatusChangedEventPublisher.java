package com.jee.shop.shipping.messaging;

import com.jee.shop.shipping.entity.ShipmentEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.jms.Connection;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

/** Publishes delivery state changes as JSON text messages to the ActiveMQ topic. */
@ApplicationScoped
public class DeliveryStatusChangedEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryStatusChangedEventPublisher.class);

    public void publishDeliveryStatusChangedEvent(ShipmentEntity shipment) throws JMSException {
        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(
                UUID.randomUUID(), shipment.getOrderId(), shipment.getId(),
                shipment.getShippingStatus().name(), shipment.getTrackingNumber(), LocalDateTime.now());
        String brokerUrl = "tcp://" + System.getenv().getOrDefault("JMS_HOST", "localhost")
                + ":" + System.getenv().getOrDefault("JMS_PORT", "61616");

        try (Jsonb jsonb = JsonbBuilder.create();
             Connection connection = new ActiveMQConnectionFactory(brokerUrl).createConnection();
             Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Topic topic = session.createTopic("delivery.status.changed");
            try (MessageProducer producer = session.createProducer(topic)) {
                producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                TextMessage message = session.createTextMessage(jsonb.toJson(event));
                message.setStringProperty("eventType", "DeliveryStatusChanged");
                connection.start();
                producer.send(message);
            }
            logger.info("Published delivery status {} for order {} to delivery.status.changed",
                    event.getStatus(), event.getOrderId());
        } catch (JMSException e) {
            logger.error("Failed to publish delivery status for shipment {}", shipment.getId(), e);
            throw e;
        } catch (Exception e) {
            JMSException jmsException = new JMSException("Could not serialize delivery status event");
            jmsException.setLinkedException(e);
            throw jmsException;
        }
    }
}
