package com.jee.shop.order.repository;

import com.jee.shop.order.entity.ProductEntity;
import com.jee.shop.order.entity.ProductCategory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Product Repository.
 * Handles database operations for ProductEntity.
 *
 * Lernziel: JPQL Queries, Repository Pattern, Query filtering
 * JPQL Query #2: Find products by category and active status
 * JPQL Query #3: Find all active products ordered by creation date
 */
public class ProductRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProductRepository.class);

    @PersistenceContext(unitName = "orderServicePU")
    private EntityManager entityManager;

    /**
     * Finds a product by ID.
     *
     * @param id the product ID
     * @return Optional containing the product if found
     */
    public Optional<ProductEntity> findById(UUID id) {
        logger.debug("Searching for product with ID: {}", id);
        ProductEntity product = entityManager.find(ProductEntity.class, id);
        return Optional.ofNullable(product);
    }

    /**
     * Finds all active products, ordered by creation date (newest first).
     * JPQL Query #3
     *
     * @return list of active products
     */
    public List<ProductEntity> findAllActive() {
        logger.debug("Fetching all active products");

        return entityManager.createQuery(
                "SELECT p FROM ProductEntity p WHERE p.active = true ORDER BY p.createdAt DESC",
                ProductEntity.class)
                .getResultList();
    }

    /**
     * Finds products by category and active status.
     * JPQL Query #2: Demonstrates filtering by multiple criteria
     *
     * @param category the product category
     * @return list of products matching category and active status
     */
    public List<ProductEntity> findByCategoryAndActive(ProductCategory category) {
        logger.debug("Searching for active products in category: {}", category);

        return entityManager.createQuery(
                "SELECT p FROM ProductEntity p " +
                "WHERE p.category = :category AND p.active = true " +
                "ORDER BY p.name ASC",
                ProductEntity.class)
                .setParameter("category", category)
                .getResultList();
    }

    /**
     * Finds all products (including inactive).
     *
     * @return list of all products
     */
    public List<ProductEntity> findAll() {
        logger.debug("Fetching all products");

        return entityManager.createQuery(
                "SELECT p FROM ProductEntity p ORDER BY p.name ASC",
                ProductEntity.class)
                .getResultList();
    }

    /**
     * Persists a new product.
     *
     * @param product the product to persist
     * @return the persisted product
     */
    public ProductEntity save(ProductEntity product) {
        logger.info("Persisting product: {}", product.getName());
        entityManager.persist(product);
        return product;
    }

    /**
     * Updates an existing product.
     *
     * @param product the product to update
     * @return the updated product
     */
    public ProductEntity update(ProductEntity product) {
        logger.info("Updating product: {}", product.getId());
        return entityManager.merge(product);
    }

    /**
     * Deletes a product.
     *
     * @param product the product to delete
     */
    public void delete(ProductEntity product) {
        logger.info("Deleting product: {}", product.getId());
        entityManager.remove(product);
    }

    /**
     * Checks if a product name already exists.
     *
     * @param name the product name
     * @return true if product exists, false otherwise
     */
    public boolean existsByName(String name) {
        Long count = entityManager.createQuery(
                "SELECT COUNT(p) FROM ProductEntity p WHERE p.name = :name",
                Long.class)
                .setParameter("name", name)
                .getSingleResult();

        return count > 0;
    }

    /**
     * Gets the total count of active products.
     *
     * @return count of active products
     */
    public long countActive() {
        return entityManager.createQuery(
                "SELECT COUNT(p) FROM ProductEntity p WHERE p.active = true",
                Long.class)
                .getSingleResult();
    }
}

