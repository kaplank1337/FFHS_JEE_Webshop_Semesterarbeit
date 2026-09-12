package com.jee.shop.shipping.messaging;

import com.jee.shop.shipping.service.ShippingService;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * OrderCreatedEventListener (Message-Driven Bean).
 * Consumes OrderCreatedEvents from Order Service.
 * Creates shipments automatically.
 *
 * Lernziel: Message-Driven Beans, Queue Consumer, Async Processing,
 *           Idempotent Message Handling
 */
@MessageDriven(
    name = "OrderCreatedEventListener",
    activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "order.created"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "AUTO_ACKNOWLEDGE"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "10")
    }
)
public class OrderCreatedEventListener implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreatedEventListener.class);

    @Inject
    private ShippingService shippingService;

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof ObjectMessage) {
                ObjectMessage objMessage = (ObjectMessage) message;
                Object payload = objMessage.getObject();

                // Handle both Event class types (Order Service's OrderCreatedEvent)
                if (payload instanceof OrderCreatedEvent) {
                    OrderCreatedEvent event = (OrderCreatedEvent) payload;

                    logger.info("Received OrderCreatedEvent: {}", event.getEventId());
                    logger.debug("Order: {}, User: {}, Items: {}",
                        event.getOrderId(), event.getUserId(), event.getItems());

                    // Create shipment
                    shippingService.createShipmentForOrder(event.getOrderId(), event.getUserId());

                    logger.info("Shipment created for Order: {}", event.getOrderId());

                } else {
                    logger.warn("Unknown message type: {}", payload.getClass());
                }

            } else {
                logger.warn("Invalid message type: {}", message.getClass());
            }

        } catch (JMSException e) {
            logger.error("Error processing OrderCreatedEvent", e);
            throw new RuntimeException("Message processing failed", e);
        } catch (Exception e) {
            logger.error("Unexpected error in OrderCreatedEventListener", e);
            throw new RuntimeException("Unexpected error", e);
        }
    }

    /**
     * OrderCreatedEvent DTO (matching Order Service's event).
     * Needed for deserialization.
     */
    public static class OrderCreatedEvent implements Serializable {
        private static final long serialVersionUID = 1L;

        private UUID eventId;
        private UUID orderId;
        private UUID userId;
        private LocalDateTime createdAt;
        private List<OrderCreatedEventItem> items;
        private BigDecimal totalPrice;

        public UUID getEventId() { return eventId; }
        public UUID getOrderId() { return orderId; }
        public UUID getUserId() { return userId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public List<OrderCreatedEventItem> getItems() { return items; }
        public BigDecimal getTotalPrice() { return totalPrice; }

        public static class OrderCreatedEventItem implements Serializable {
            private static final long serialVersionUID = 1L;

            private UUID productId;
            private String productName;
            private int quantity;
            private BigDecimal unitPrice;
            private BigDecimal totalPrice;

            public UUID getProductId() { return productId; }
            public String getProductName() { return productName; }
            public int getQuantity() { return quantity; }
            public BigDecimal getUnitPrice() { return unitPrice; }
            public BigDecimal getTotalPrice() { return totalPrice; }
        }
    }
}

