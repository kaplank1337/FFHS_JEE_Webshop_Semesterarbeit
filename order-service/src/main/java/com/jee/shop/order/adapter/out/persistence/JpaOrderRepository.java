package com.jee.shop.order.adapter.out.persistence;

import com.jee.shop.order.domain.model.OrderEntity;
import com.jee.shop.order.domain.model.OrderStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order Repository.
 * Handles database operations for OrderEntity.
 *
 * Lernziel: JPQL Queries, Complex queries with parameters
 * JPQL Query #4: Find orders by user, sorted by date
 * JPQL Query #5: Find orders by status
 */
@ApplicationScoped
public class JpaOrderRepository implements com.jee.shop.order.application.port.out.OrderStore {

    private static final Logger logger = LoggerFactory.getLogger(JpaOrderRepository.class);

    @PersistenceContext(unitName = "orderServicePU")
    private EntityManager entityManager;

    /**
     * Finds an order by ID.
     *
     * @param id the order ID
     * @return Optional containing the order if found
     */
    public Optional<OrderEntity> findById(UUID id) {
        logger.debug("Searching for order with ID: {}", id);
        OrderEntity order = entityManager.find(OrderEntity.class, id);
        return Optional.ofNullable(order);
    }

    /**
     * Finds all orders for a specific user, sorted by creation date (newest first).
     * JPQL Query #4: Demonstrates filtering by user ID and sorting
     *
     * @param userId the user ID
     * @return list of orders for the user
     */
    public List<OrderEntity> findByUserId(UUID userId) {
        logger.debug("Searching for orders for user: {}", userId);

        return entityManager.createQuery(
                "SELECT o FROM OrderEntity o " +
                "WHERE o.userId = :userId " +
                "ORDER BY o.createdAt DESC",
                OrderEntity.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    /**
     * Finds all orders with a specific status.
     * JPQL Query #5: Demonstrates filtering by enum status
     *
     * @param status the order status
     * @return list of orders with that status
     */
    public List<OrderEntity> findByStatus(OrderStatus status) {
        logger.debug("Searching for orders with status: {}", status);

        return entityManager.createQuery(
                "SELECT o FROM OrderEntity o " +
                "WHERE o.status = :status " +
                "ORDER BY o.createdAt DESC",
                OrderEntity.class)
                .setParameter("status", status)
                .getResultList();
    }

    /**
     * Finds all orders for a user with a specific status.
     *
     * @param userId the user ID
     * @param status the order status
     * @return list of orders
     */
    public List<OrderEntity> findByUserIdAndStatus(UUID userId, OrderStatus status) {
        logger.debug("Searching for orders for user {} with status: {}", userId, status);

        return entityManager.createQuery(
                "SELECT o FROM OrderEntity o " +
                "WHERE o.userId = :userId AND o.status = :status " +
                "ORDER BY o.createdAt DESC",
                OrderEntity.class)
                .setParameter("userId", userId)
                .setParameter("status", status)
                .getResultList();
    }

    /**
     * Finds all orders.
     *
     * @return list of all orders
     */
    public List<OrderEntity> findAll() {
        logger.debug("Fetching all orders");

        return entityManager.createQuery(
                "SELECT o FROM OrderEntity o ORDER BY o.createdAt DESC",
                OrderEntity.class)
                .getResultList();
    }

    /**
     * Persists a new order.
     *
     * @param order the order to persist
     * @return the persisted order
     */
    public OrderEntity save(OrderEntity order) {
        logger.info("Persisting order: {} for user: {}", order.getId(), order.getUserId());
        entityManager.persist(order);
        return order;
    }

    @Override
    public void flush() {
        entityManager.flush();
    }

    /**
     * Updates an existing order.
     *
     * @param order the order to update
     * @return the updated order
     */
    public OrderEntity update(OrderEntity order) {
        logger.info("Updating order: {}", order.getId());
        return entityManager.merge(order);
    }

    /**
     * Deletes an order.
     *
     * @param order the order to delete
     */
    public void delete(OrderEntity order) {
        logger.info("Deleting order: {}", order.getId());
        entityManager.remove(order);
    }

    /**
     * Counts orders for a user.
     *
     * @param userId the user ID
     * @return count of orders
     */
    public long countByUserId(UUID userId) {
        return entityManager.createQuery(
                "SELECT COUNT(o) FROM OrderEntity o WHERE o.userId = :userId",
                Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    /**
     * Counts orders with a specific status.
     *
     * @param status the order status
     * @return count of orders
     */
    public long countByStatus(OrderStatus status) {
        return entityManager.createQuery(
                "SELECT COUNT(o) FROM OrderEntity o WHERE o.status = :status",
                Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }
}

