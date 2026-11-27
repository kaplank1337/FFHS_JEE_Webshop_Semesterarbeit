package com.jee.shop.order.application.dto;

import com.jee.shop.order.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Order DTO for REST responses.
 */
public record OrderDto(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal totalPrice,
        List<OrderItemDto> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

