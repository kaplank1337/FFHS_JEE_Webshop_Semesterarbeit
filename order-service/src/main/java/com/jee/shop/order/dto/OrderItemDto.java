package com.jee.shop.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Order Item DTO for REST responses.
 */
public record OrderItemDto(
        UUID id,
        UUID productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}

