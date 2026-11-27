package com.jee.shop.order.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/**
 * Order Item Request DTO.
 * Part of CreateOrderRequest.
 */
public record OrderItemRequest(
        @NotNull(message = "Product ID is required")
        UUID productId,

        @Positive(message = "Quantity must be positive")
        int quantity
) {
}

