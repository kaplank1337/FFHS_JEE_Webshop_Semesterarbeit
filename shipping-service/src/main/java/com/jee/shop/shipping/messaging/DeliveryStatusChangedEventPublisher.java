package com.jee.shop.shipping.messaging;

import com.jee.shop.shipping.entity.ShipmentEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DeliveryStatusChangedEvent Publisher.
 * Publishes events when shipment status changes.
 *
 * Lernziel: JMS Topic Producer, Event Publishing to Topic
 */
@ApplicationScoped
public class DeliveryStatusChangedEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryStatusChangedEventPublisher.class);

    private ConnectionFactory connectionFactory;
    private Topic deliveryStatusTopic;

    /**
     * Publishes a DeliveryStatusChangedEvent.
     *
     * @param shipment the shipment with updated status
     * @throws JMSException if JMS operation fails
     */
    public void publishDeliveryStatusChangedEvent(ShipmentEntity shipment) throws JMSException {
        logger.info("Publishing DeliveryStatusChangedEvent for shipment: {}", shipment.getId());

        // Create event
        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(
                UUID.randomUUID(),
                shipment.getOrderId(),
                shipment.getId(),
                shipment.getShippingStatus().toString(),
                shipment.getTrackingNumber(),
                LocalDateTime.now()
        );

        // Send via JMS
        try (
                Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        ) {
            ObjectMessage message = session.createObjectMessage(event);
            MessageProducer producer = session.createProducer(deliveryStatusTopic);

            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(message);

            logger.info("DeliveryStatusChangedEvent published: {} for Shipment: {}",
                event.getEventId(), shipment.getId());

        } catch (JMSException e) {
            logger.error("Failed to publish DeliveryStatusChangedEvent", e);
            throw e;
        }
    }
}

