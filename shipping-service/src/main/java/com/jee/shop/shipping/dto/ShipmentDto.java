package com.jee.shop.shipping.dto;

import com.jee.shop.shipping.entity.ShippingStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shipment DTO for REST responses.
 */
public record ShipmentDto(
        UUID id,
        UUID orderId,
        UUID userId,
        ShippingStatus shippingStatus,
        String trackingNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

