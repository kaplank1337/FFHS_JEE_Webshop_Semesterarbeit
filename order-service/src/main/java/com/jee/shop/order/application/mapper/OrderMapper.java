package com.jee.shop.order.application.mapper;

import com.jee.shop.order.application.dto.OrderDto;
import com.jee.shop.order.application.dto.OrderItemDto;
import com.jee.shop.order.application.dto.ProductDto;
import com.jee.shop.order.domain.model.OrderEntity;
import com.jee.shop.order.domain.model.OrderItemEntity;
import com.jee.shop.order.domain.model.ProductEntity;

import java.util.stream.Collectors;

/**
 * Mapper for Order and Product entities to DTOs.
 */
public class OrderMapper {

    public static OrderDto toDto(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        return new OrderDto(
                entity.getId(),
                entity.getUserId(),
                entity.getStatus(),
                entity.getTotalPrice(),
                entity.getItems().stream()
                        .map(OrderMapper::toDto)
                        .collect(Collectors.toList()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static OrderItemDto toDto(OrderItemEntity entity) {
        if (entity == null) {
            return null;
        }

        return new OrderItemDto(
                entity.getId(),
                entity.getProductId(),
                entity.getProductName(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getTotalPrice()
        );
    }
}


