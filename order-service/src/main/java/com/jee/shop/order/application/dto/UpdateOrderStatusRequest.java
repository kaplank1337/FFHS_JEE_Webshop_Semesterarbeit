package com.jee.shop.order.application.dto;

import com.jee.shop.order.domain.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Order status is required")
        OrderStatus status
) {
}
