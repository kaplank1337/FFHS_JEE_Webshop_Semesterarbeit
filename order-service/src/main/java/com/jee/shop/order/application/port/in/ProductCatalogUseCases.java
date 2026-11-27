package com.jee.shop.order.application.port.in;

import com.jee.shop.order.domain.model.ProductEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Inbound use cases for the product catalog. */
public interface ProductCatalogUseCases {
    List<ProductEntity> getAllActiveProducts();
    Optional<ProductEntity> getProductById(UUID id);
    ProductEntity createProduct(ProductEntity product);
    ProductEntity updateProduct(ProductEntity product);
    void deleteProduct(UUID id);
    ProductEntity toggleActiveStatus(UUID id, boolean active);
    long getActiveProductCount();
}
