package com.jee.shop.shipping.repository;

import com.jee.shop.shipping.entity.ShipmentEntity;
import com.jee.shop.shipping.entity.ShippingStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Shipment Repository.
 * Handles database operations for ShipmentEntity.
 *
 * Lernziel: JPQL Queries, Query filtering
 * JPQL Query #6: Find shipments by order ID (demonstrates single-result query)
 */
public class ShipmentRepository {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentRepository.class);

    @PersistenceContext(unitName = "shippingServicePU")
    private EntityManager entityManager;

    /**
     * Finds a shipment by ID.
     *
     * @param id the shipment ID
     * @return Optional containing the shipment if found
     */
    public Optional<ShipmentEntity> findById(UUID id) {
        logger.debug("Searching for shipment with ID: {}", id);
        ShipmentEntity shipment = entityManager.find(ShipmentEntity.class, id);
        return Optional.ofNullable(shipment);
    }

    /**
     * Finds a shipment by order ID.
     * JPQL Query #6: Demonstrates finding related entity by foreign key
     *
     * @param orderId the order ID
     * @return Optional containing the shipment if found
     */
    public Optional<ShipmentEntity> findByOrderId(UUID orderId) {
        logger.debug("Searching for shipment for order: {}", orderId);

        try {
            ShipmentEntity shipment = entityManager.createQuery(
                    "SELECT s FROM ShipmentEntity s WHERE s.orderId = :orderId",
                    ShipmentEntity.class)
                    .setParameter("orderId", orderId)
                    .getSingleResult();

            logger.debug("Shipment found for order: {}", orderId);
            return Optional.of(shipment);

        } catch (jakarta.persistence.NoResultException e) {
            logger.debug("No shipment found for order: {}", orderId);
            return Optional.empty();
        }
    }

    /**
     * Finds all shipments for a user.
     *
     * @param userId the user ID
     * @return list of shipments for the user
     */
    public List<ShipmentEntity> findByUserId(UUID userId) {
        logger.debug("Searching for shipments for user: {}", userId);

        return entityManager.createQuery(
                "SELECT s FROM ShipmentEntity s WHERE s.userId = :userId ORDER BY s.createdAt DESC",
                ShipmentEntity.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    /**
     * Finds all shipments with a specific status.
     *
     * @param status the shipping status
     * @return list of shipments with that status
     */
    public List<ShipmentEntity> findByStatus(ShippingStatus status) {
        logger.debug("Searching for shipments with status: {}", status);

        return entityManager.createQuery(
                "SELECT s FROM ShipmentEntity s WHERE s.shippingStatus = :status ORDER BY s.createdAt DESC",
                ShipmentEntity.class)
                .setParameter("status", status)
                .getResultList();
    }

    /**
     * Finds all shipments.
     *
     * @return list of all shipments
     */
    public List<ShipmentEntity> findAll() {
        logger.debug("Fetching all shipments");

        return entityManager.createQuery(
                "SELECT s FROM ShipmentEntity s ORDER BY s.createdAt DESC",
                ShipmentEntity.class)
                .getResultList();
    }

    /**
     * Persists a new shipment.
     *
     * @param shipment the shipment to persist
     * @return the persisted shipment
     */
    public ShipmentEntity save(ShipmentEntity shipment) {
        logger.info("Persisting shipment: {} for order: {}", shipment.getId(), shipment.getOrderId());
        entityManager.persist(shipment);
        return shipment;
    }

    /**
     * Updates an existing shipment.
     *
     * @param shipment the shipment to update
     * @return the updated shipment
     */
    public ShipmentEntity update(ShipmentEntity shipment) {
        logger.info("Updating shipment: {}", shipment.getId());
        return entityManager.merge(shipment);
    }

    /**
     * Deletes a shipment.
     *
     * @param shipment the shipment to delete
     */
    public void delete(ShipmentEntity shipment) {
        logger.info("Deleting shipment: {}", shipment.getId());
        entityManager.remove(shipment);
    }

    /**
     * Counts shipments for a user.
     *
     * @param userId the user ID
     * @return count of shipments
     */
    public long countByUserId(UUID userId) {
        return entityManager.createQuery(
                "SELECT COUNT(s) FROM ShipmentEntity s WHERE s.userId = :userId",
                Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    /**
     * Counts shipments with a specific status.
     *
     * @param status the shipping status
     * @return count of shipments
     */
    public long countByStatus(ShippingStatus status) {
        return entityManager.createQuery(
                "SELECT COUNT(s) FROM ShipmentEntity s WHERE s.shippingStatus = :status",
                Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }
}

