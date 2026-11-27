package com.jee.shop.shipping.application.port.out;

import com.jee.shop.shipping.domain.model.ShipmentEntity;
import com.jee.shop.shipping.domain.model.ShippingStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound persistence contract for shipments. */
public interface ShipmentStore {
    Optional<ShipmentEntity> findById(UUID id);
    Optional<ShipmentEntity> findByOrderId(UUID orderId);
    List<ShipmentEntity> findByUserId(UUID userId);
    List<ShipmentEntity> findByStatus(ShippingStatus status);
    List<ShipmentEntity> findAll();
    ShipmentEntity save(ShipmentEntity shipment);
    ShipmentEntity update(ShipmentEntity shipment);
    void delete(ShipmentEntity shipment);
    long countByUserId(UUID userId);
    long countByStatus(ShippingStatus status);
    void flush();
}
