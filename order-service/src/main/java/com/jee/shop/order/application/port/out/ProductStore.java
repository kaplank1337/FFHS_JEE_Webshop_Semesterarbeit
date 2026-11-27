package com.jee.shop.order.application.port.out;

import com.jee.shop.order.domain.model.ProductCategory;
import com.jee.shop.order.domain.model.ProductEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound persistence contract for the product catalog. */
public interface ProductStore {
    Optional<ProductEntity> findById(UUID id);
    List<ProductEntity> findAllActive();
    List<ProductEntity> findByCategoryAndActive(ProductCategory category);
    List<ProductEntity> findAll();
    ProductEntity save(ProductEntity product);
    ProductEntity update(ProductEntity product);
    void delete(ProductEntity product);
    boolean existsByName(String name);
    long countActive();
}
