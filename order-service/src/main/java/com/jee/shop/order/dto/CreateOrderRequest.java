package com.jee.shop.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

/**
 * Create Order Request DTO.
 * Sent by client to create a new order.
 */
public record CreateOrderRequest(
        @NotNull(message = "Items list is required")
        List<OrderItemRequest> items
) {
}

