package com.jee.shop.shipping.mapper;

import com.jee.shop.shipping.dto.ShipmentDto;
import com.jee.shop.shipping.entity.ShipmentEntity;

/**
 * Mapper for Shipment entity to DTO.
 */
public class ShipmentMapper {

    public static ShipmentDto toDto(ShipmentEntity entity) {
        if (entity == null) {
            return null;
        }

        return new ShipmentDto(
                entity.getId(),
                entity.getOrderId(),
                entity.getUserId(),
                entity.getShippingStatus(),
                entity.getTrackingNumber(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static ShipmentEntity toEntity(ShipmentDto dto) {
        if (dto == null) {
            return null;
        }

        ShipmentEntity entity = new ShipmentEntity(dto.orderId(), dto.userId());
        entity.setId(dto.id());
        entity.setShippingStatus(dto.shippingStatus());
        entity.setTrackingNumber(dto.trackingNumber());
        entity.setCreatedAt(dto.createdAt());
        entity.setUpdatedAt(dto.updatedAt());

        return entity;
    }
}

