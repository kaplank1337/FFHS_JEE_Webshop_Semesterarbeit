package com.jee.shop.order.service;

import com.jee.shop.order.entity.OrderEntity;
import com.jee.shop.order.entity.OrderStatus;
import com.jee.shop.order.repository.OrderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order Service.
 * Handles order queries and status management.
 *
 * Lernziel: CDI @ApplicationScoped, Query-focused Service
 */
@ApplicationScoped
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Inject
    private OrderRepository orderRepository;

    /**
     * Retrieves all orders for a user.
     *
     * @param userId the user ID
     * @return list of user's orders
     */
    public List<OrderEntity> getUserOrders(UUID userId) {
        logger.debug("Fetching orders for user: {}", userId);
        return orderRepository.findByUserId(userId);
    }

    /**
     * Retrieves a specific order.
     *
     * @param orderId the order ID
     * @return Optional containing order if found
     */
    public Optional<OrderEntity> getOrderById(UUID orderId) {
        logger.debug("Fetching order: {}", orderId);
        return orderRepository.findById(orderId);
    }

    /**
     * Retrieves orders by status.
     *
     * @param status the order status
     * @return list of orders with that status
     */
    public List<OrderEntity> getOrdersByStatus(OrderStatus status) {
        logger.debug("Fetching orders with status: {}", status);
        return orderRepository.findByStatus(status);
    }

    /**
     * Updates order status.
     *
     * @param orderId the order ID
     * @param newStatus the new status
     * @return updated order
     */
    public OrderEntity updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        logger.info("Updating order {} status to: {}", orderId, newStatus);

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        order.setStatus(newStatus);
        return orderRepository.update(order);
    }

    /**
     * Gets count of orders for a user.
     *
     * @param userId the user ID
     * @return count
     */
    public long getUserOrderCount(UUID userId) {
        return orderRepository.countByUserId(userId);
    }
}

