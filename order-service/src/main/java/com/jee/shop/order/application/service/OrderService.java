package com.jee.shop.order.application.service;

import com.jee.shop.order.domain.model.OrderEntity;
import com.jee.shop.order.domain.model.OrderStatus;
import com.jee.shop.order.domain.model.ProductEntity;
import com.jee.shop.order.domain.exception.OrderNotFoundException;
import com.jee.shop.order.application.port.out.OrderStore;
import com.jee.shop.order.application.port.out.ProductStore;
import com.jee.shop.order.application.port.in.OrderUseCases;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
public class OrderService implements OrderUseCases {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Inject
    private OrderStore orderRepository;

    @Inject
    private ProductStore productRepository;

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

    /** Returns an order only when it belongs to the authenticated user. */
    public Optional<OrderEntity> getOrderByIdForUser(UUID orderId, UUID userId) {
        return orderRepository.findById(orderId)
                .filter(order -> order.getUserId().equals(userId));
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
    @Transactional
    public OrderEntity updateOrderStatus(UUID orderId, UUID userId, OrderStatus newStatus) {
        logger.info("Updating order {} status to: {}", orderId, newStatus);

        OrderEntity order = orderRepository.findById(orderId)
                .filter(candidate -> candidate.getUserId().equals(userId))
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        order.setStatus(newStatus);
        return orderRepository.update(order);
    }

    /** Trusted internal status update used by delivery events. */
    @Transactional
    public OrderEntity updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        order.setStatus(newStatus);
        return orderRepository.update(order);
    }

    /** Deletes an order owned by the authenticated user. Reserved stock is returned for unshipped orders. */
    @Transactional
    public void deleteOrder(UUID orderId, UUID userId) {
        OrderEntity order = orderRepository.findById(orderId)
                .filter(candidate -> candidate.getUserId().equals(userId))
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.SHIPPING
                && order.getStatus() != OrderStatus.SHIPPED
                && order.getStatus() != OrderStatus.DELIVERED) {
            order.getItems().forEach(item -> productRepository.findById(item.getProductId())
                    .ifPresent(product -> {
                        product.increaseStock(item.getQuantity());
                        productRepository.update(product);
                    }));
        }

        orderRepository.delete(order);
        logger.info("Deleted order {} for user {}", orderId, userId);
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

