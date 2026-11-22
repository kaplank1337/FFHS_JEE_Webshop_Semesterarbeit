package com.jee.shop.shipping.service;

import com.jee.shop.shipping.entity.ShipmentEntity;
import com.jee.shop.shipping.entity.ShippingStatus;
import com.jee.shop.shipping.messaging.DeliveryStatusChangedEventPublisher;
import com.jee.shop.shipping.messaging.ShippingStatusWebSocketEndpoint;
import com.jee.shop.shipping.repository.ShipmentRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Shipping Service.
 * Handles shipment creation, status management, and event publishing.
 *
 * Lernziel: EJB @Stateless, Business Logic, Transaction Management
 */
@Stateless
public class ShippingService {

    private static final Logger logger = LoggerFactory.getLogger(ShippingService.class);

    @Inject
    private ShipmentRepository shipmentRepository;

    @Inject
    private DeliveryStatusChangedEventPublisher eventPublisher;

    @PersistenceContext(unitName = "shippingServicePU")
    private EntityManager entityManager;

    /**
     * Creates a new shipment for an order.
     * Called when OrderCreatedEvent is received.
     *
     * @param orderId the order ID
     * @param userId the user ID
     * @return the created shipment
     */
    @Transactional(rollbackOn = Exception.class)
    public ShipmentEntity createShipmentForOrder(UUID orderId, UUID userId) {
        logger.info("Creating shipment for order: {}", orderId);

        // Check if shipment already exists (idempotence)
        Optional<ShipmentEntity> existing = shipmentRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            logger.warn("Shipment already exists for order: {}", orderId);
            return existing.get();
        }

        // Create shipment
        ShipmentEntity shipment = new ShipmentEntity(orderId, userId);
        shipment.setShippingStatus(ShippingStatus.PREPARING);

        // Persist
        shipmentRepository.save(shipment);
        entityManager.flush();

        logger.info("Shipment created: {} for Order: {}", shipment.getId(), orderId);
        publishStatusChanged(shipment);
        return shipment;
    }

    /**
     * Updates shipment status and publishes event.
     *
     * @param shipmentId the shipment ID
     * @param newStatus the new status
     * @return the updated shipment
     */
    @Transactional
    public ShipmentEntity updateShipmentStatus(UUID shipmentId, ShippingStatus newStatus) {
        logger.info("Updating shipment {} status to: {}", shipmentId, newStatus);

        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));

        // Validate state transition
        if (!shipment.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                String.format("Cannot transition from %s to %s",
                    shipment.getShippingStatus(), newStatus)
            );
        }

        // Update status
        shipment.setShippingStatus(newStatus);
        shipmentRepository.update(shipment);
        entityManager.flush();

        logger.info("Shipment status updated to: {} for Shipment: {}", newStatus, shipmentId);
        publishStatusChanged(shipment);

        return shipment;
    }

    private void publishStatusChanged(ShipmentEntity shipment) {
        try {
            eventPublisher.publishDeliveryStatusChangedEvent(shipment);
            ShippingStatusWebSocketEndpoint.publishStatusChanged(
                    shipment.getUserId(), shipment.getOrderId(), mapToOrderStatus(shipment.getShippingStatus()));
        } catch (Exception e) {
            logger.error("Failed to publish status for shipment {}", shipment.getId(), e);
            throw new IllegalStateException("Could not publish delivery status event", e);
        }
    }

    private String mapToOrderStatus(ShippingStatus status) {
        return switch (status) {
            case PREPARING -> "CONFIRMED";
            case READY_FOR_SHIPMENT -> "SHIPPING";
            case SHIPPED -> "SHIPPED";
            case DELIVERED -> "DELIVERED";
            case CANCELLED -> "CANCELLED";
        };
    }

    /**
     * Retrieves a shipment by ID.
     *
     * @param shipmentId the shipment ID
     * @return Optional containing shipment if found
     */
    public Optional<ShipmentEntity> getShipment(UUID shipmentId) {
        logger.debug("Fetching shipment: {}", shipmentId);
        return shipmentRepository.findById(shipmentId);
    }

    /**
     * Retrieves shipments for a user.
     *
     * @param userId the user ID
     * @return list of shipments
     */
    public List<ShipmentEntity> getUserShipments(UUID userId) {
        logger.debug("Fetching shipments for user: {}", userId);
        return shipmentRepository.findByUserId(userId);
    }

    /**
     * Retrieves shipment by order ID.
     *
     * @param orderId the order ID
     * @return Optional containing shipment if found
     */
    public Optional<ShipmentEntity> getShipmentByOrderId(UUID orderId) {
        logger.debug("Fetching shipment for order: {}", orderId);
        return shipmentRepository.findByOrderId(orderId);
    }
}

