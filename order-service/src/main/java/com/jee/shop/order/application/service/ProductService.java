package com.jee.shop.order.application.service;

import com.jee.shop.order.domain.model.ProductEntity;
import com.jee.shop.order.application.port.out.ProductStore;
import com.jee.shop.order.application.port.in.ProductCatalogUseCases;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Product Service.
 * Handles product catalog operations.
 *
 * Lernziel: EJB @Stateless, Business Service Layer, Transaction Management
 */
@Stateless
public class ProductService implements ProductCatalogUseCases {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Inject
    private ProductStore productRepository;

    /**
     * Retrieves all active products.
     *
     * @return list of active products
     */
    public List<ProductEntity> getAllActiveProducts() {
        logger.debug("Fetching all active products");
        return productRepository.findAllActive();
    }

    /**
     * Retrieves a product by ID.
     *
     * @param id the product ID
     * @return Optional containing product if found
     */
    public Optional<ProductEntity> getProductById(UUID id) {
        logger.debug("Fetching product with ID: {}", id);
        return productRepository.findById(id);
    }

    /**
     * Creates a new product.
     *
     * @param product the product to create
     * @return the created product
     */
    public ProductEntity createProduct(ProductEntity product) {
        logger.info("Creating product: {}", product.getName());

        if (productRepository.existsByName(product.getName())) {
            throw new IllegalArgumentException("Product with name '" + product.getName() + "' already exists");
        }

        return productRepository.save(product);
    }

    /**
     * Updates an existing product.
     *
     * @param product the product to update
     * @return the updated product
     */
    public ProductEntity updateProduct(ProductEntity product) {
        logger.info("Updating product: {}", product.getId());

        if (!productRepository.findById(product.getId()).isPresent()) {
            throw new IllegalArgumentException("Product not found: " + product.getId());
        }

        return productRepository.update(product);
    }

    /**
     * Deletes a product.
     *
     * @param id the product ID
     */
    public void deleteProduct(UUID id) {
        logger.info("Deleting product: {}", id);

        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        productRepository.delete(product);
    }

    /**
     * Toggles product active status.
     *
     * @param id the product ID
     * @param active the new active status
     * @return the updated product
     */
    public ProductEntity toggleActiveStatus(UUID id, boolean active) {
        logger.info("Toggling product {} active status to: {}", id, active);

        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        product.setActive(active);
        return productRepository.update(product);
    }

    /**
     * Gets total count of active products.
     *
     * @return count
     */
    public long getActiveProductCount() {
        return productRepository.countActive();
    }
}

