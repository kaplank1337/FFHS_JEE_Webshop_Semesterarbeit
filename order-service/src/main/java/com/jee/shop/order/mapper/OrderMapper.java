package com.jee.shop.order.mapper;

import com.jee.shop.order.dto.OrderDto;
import com.jee.shop.order.dto.OrderItemDto;
import com.jee.shop.order.dto.ProductDto;
import com.jee.shop.order.entity.OrderEntity;
import com.jee.shop.order.entity.OrderItemEntity;
import com.jee.shop.order.entity.ProductEntity;

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


