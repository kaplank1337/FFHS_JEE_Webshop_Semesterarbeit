package com.jee.shop.order.application.port.out;

import com.jee.shop.order.domain.model.OrderEntity;
import com.jee.shop.order.domain.model.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound persistence contract for orders. */
public interface OrderStore {
    Optional<OrderEntity> findById(UUID id);
    List<OrderEntity> findByUserId(UUID userId);
    List<OrderEntity> findByStatus(OrderStatus status);
    List<OrderEntity> findByUserIdAndStatus(UUID userId, OrderStatus status);
    List<OrderEntity> findAll();
    OrderEntity save(OrderEntity order);
    OrderEntity update(OrderEntity order);
    void delete(OrderEntity order);
    long countByUserId(UUID userId);
    long countByStatus(OrderStatus status);
    void flush();
}
