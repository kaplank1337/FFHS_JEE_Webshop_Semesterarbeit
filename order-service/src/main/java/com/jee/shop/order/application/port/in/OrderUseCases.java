package com.jee.shop.order.application.port.in;

import com.jee.shop.order.domain.model.OrderEntity;
import com.jee.shop.order.domain.model.OrderStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Inbound use cases for reading and managing orders. */
public interface OrderUseCases {
    List<OrderEntity> getUserOrders(UUID userId);
    Optional<OrderEntity> getOrderById(UUID orderId);
    Optional<OrderEntity> getOrderByIdForUser(UUID orderId, UUID userId);
    List<OrderEntity> getOrdersByStatus(OrderStatus status);
    OrderEntity updateOrderStatus(UUID orderId, UUID userId, OrderStatus newStatus);
    OrderEntity updateOrderStatus(UUID orderId, OrderStatus newStatus);
    void deleteOrder(UUID orderId, UUID userId);
    long getUserOrderCount(UUID userId);
}
