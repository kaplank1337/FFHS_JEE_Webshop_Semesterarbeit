package com.jee.shop.order.application.mapper;

import com.jee.shop.order.application.dto.ProductDto;
import com.jee.shop.order.domain.model.ProductEntity;

/**
 * Mapper for Product entity to DTO.
 */
public class ProductMapper {

    public static ProductDto toDto(ProductEntity entity) {
        if (entity == null) {
            return null;
        }

        return new ProductDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getPrice(),
                entity.getStockQuantity(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static ProductEntity toEntity(ProductDto dto) {
        if (dto == null) {
            return null;
        }

        ProductEntity entity = new ProductEntity(
                dto.name(),
                dto.description(),
                dto.category(),
                dto.price(),
                dto.stockQuantity()
        );
        entity.setId(dto.id());
        entity.setActive(dto.active());
        entity.setCreatedAt(dto.createdAt());
        entity.setUpdatedAt(dto.updatedAt());

        return entity;
    }
}

