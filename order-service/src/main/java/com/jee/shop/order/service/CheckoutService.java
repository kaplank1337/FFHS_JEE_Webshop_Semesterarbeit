package com.jee.shop.order.service;

import com.jee.shop.order.dto.CreateOrderRequest;
import com.jee.shop.order.dto.OrderDto;
import com.jee.shop.order.entity.OrderEntity;
import com.jee.shop.order.entity.OrderItemEntity;
import com.jee.shop.order.entity.OrderStatus;
import com.jee.shop.order.entity.ProductEntity;
import com.jee.shop.order.exception.InsufficientStockException;
import com.jee.shop.order.exception.ProductNotFoundException;
import com.jee.shop.order.mapper.OrderMapper;
import com.jee.shop.order.repository.OrderRepository;
import com.jee.shop.order.repository.ProductRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Checkout Service.
 * Handles order creation, validation, and stock management.
 *
 * Lernziel: EJB @Stateless mit @Transactional, Complex Business Logic,
 *           Validation, Transaction Management
 */
@Stateless
public class CheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    @Inject
    private ProductRepository productRepository;

    @Inject
    private OrderRepository orderRepository;

    @PersistenceContext(unitName = "orderServicePU")
    private EntityManager entityManager;

    /**
     * Creates a new order with checkout.
     * Validates products, checks stock, calculates totals.
     *
     * @param request the checkout request
     * @param userId the user ID
     * @return the created order DTO
     */
    @Transactional
    public OrderDto createOrder(CreateOrderRequest request, UUID userId) {
        logger.info("Creating order for user: {}", userId);

        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        OrderEntity order = new OrderEntity(userId);
        order.setStatus(OrderStatus.CREATED);

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (var itemRequest : request.items()) {
            logger.debug("Processing item - Product: {}, Quantity: {}", itemRequest.productId(), itemRequest.quantity());

            ProductEntity product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found: " + itemRequest.productId()));

            if (itemRequest.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }

            if (!product.canSell(itemRequest.quantity())) {
                throw new InsufficientStockException(
                    String.format("Insufficient stock for %s. Available: %d, Requested: %d",
                            product.getName(), product.getStockQuantity(), itemRequest.quantity())
                );
            }

            OrderItemEntity item = new OrderItemEntity(
                    product.getId(),
                    product.getName(),
                    itemRequest.quantity(),
                    product.getPrice()
            );

            item.calculateTotalPrice();
            order.addItem(item);

            product.decreaseStock(itemRequest.quantity());
            productRepository.update(product);

            totalPrice = totalPrice.add(item.getTotalPrice());

            logger.debug("Item added - Product: {}, Quantity: {}, Total: {}",
                product.getName(), itemRequest.quantity(), item.getTotalPrice());
        }

        order.setTotalPrice(totalPrice);
        order.calculateTotalPrice();

        logger.info("Order validated - Total items: {}, Total price: {}", order.getItems().size(), totalPrice);

        orderRepository.save(order);
        entityManager.flush();

        logger.info("Order persisted: {} with {} items", order.getId(), order.getItems().size());
        logger.info("OrderCreatedEvent would be published here (async JMS integration)");

        return OrderMapper.toDto(order);
    }
}

