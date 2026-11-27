package com.jee.shop.shipping.application.port.in;

import com.jee.shop.shipping.domain.model.ShipmentEntity;
import com.jee.shop.shipping.domain.model.ShippingStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Inbound use cases for shipment tracking and status management. */
public interface ShippingUseCases {
    ShipmentEntity createShipmentForOrder(UUID orderId, UUID userId);
    ShipmentEntity updateShipmentStatus(UUID shipmentId, ShippingStatus newStatus);
    Optional<ShipmentEntity> getShipment(UUID shipmentId);
    Optional<ShipmentEntity> getShipmentForUser(UUID shipmentId, UUID userId);
    ShipmentEntity updateShipmentStatus(UUID shipmentId, UUID userId, ShippingStatus newStatus);
    List<ShipmentEntity> getUserShipments(UUID userId);
    Optional<ShipmentEntity> getShipmentByOrderId(UUID orderId);
}
