package com.jee.shop.order.dto;

import com.jee.shop.order.entity.ProductCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Product DTO for REST responses.
 */
public record ProductDto(
        UUID id,
        String name,
        String description,
        ProductCategory category,
        BigDecimal price,
        int stockQuantity,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

