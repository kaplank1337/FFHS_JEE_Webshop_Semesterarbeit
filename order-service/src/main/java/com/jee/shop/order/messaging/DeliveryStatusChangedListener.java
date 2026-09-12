package com.jee.shop.order.messaging;

import com.jee.shop.order.entity.OrderStatus;
import com.jee.shop.order.service.OrderService;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DeliveryStatusChangedListener (Message-Driven Bean).
 * Consumes delivery status updates from Shipping Service.
 * Updates Order status accordingly.
 *
 * Lernziel: Message-Driven Beans, @MessageDriven, Async Message Consumption,
 *           Topic Subscription, Idempotent Processing
 */
@MessageDriven(
    name = "DeliveryStatusChangedListener",
    activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "delivery.status.changed"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "AUTO_ACKNOWLEDGE"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "OrderServiceSubscriber"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "DeliveryStatusChangedOrderSubscription")
    }
)
public class DeliveryStatusChangedListener implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryStatusChangedListener.class);

    @Inject
    private OrderService orderService;

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof ObjectMessage) {
                ObjectMessage objMessage = (ObjectMessage) message;
                DeliveryStatusChangedEvent event = (DeliveryStatusChangedEvent) objMessage.getObject();

                logger.info("Received DeliveryStatusChangedEvent: {}", event.getEventId());
                logger.debug("Order: {}, Status: {}", event.getOrderId(), event.getStatus());

                // Map shipping status to order status
                OrderStatus orderStatus = mapShippingToOrderStatus(event.getStatus());

                // Update order status
                orderService.updateOrderStatus(event.getOrderId(), orderStatus);

                logger.info("Order status updated to: {} for Order: {}",
                    orderStatus, event.getOrderId());

            } else {
                logger.warn("Invalid message type: {}", message.getClass());
            }

        } catch (JMSException e) {
            logger.error("Error processing DeliveryStatusChangedEvent", e);
            // Redelivery will be handled by MDB framework
            throw new RuntimeException("Message processing failed", e);
        } catch (Exception e) {
            logger.error("Unexpected error in DeliveryStatusChangedListener", e);
            throw new RuntimeException("Unexpected error", e);
        }
    }

    private OrderStatus mapShippingToOrderStatus(String shippingStatus) {
        return switch (shippingStatus) {
            case "PREPARING" -> OrderStatus.CONFIRMED;
            case "READY_FOR_SHIPMENT" -> OrderStatus.SHIPPING;
            case "SHIPPED" -> OrderStatus.SHIPPED;
            case "DELIVERED" -> OrderStatus.SHIPPED;  // Final state
            case "CANCELLED" -> OrderStatus.CANCELLED;
            default -> OrderStatus.CREATED;
        };
    }
}

