package com.jee.shop.order.application.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * OrderCreatedEvent DTO for JMS messaging.
 * Sent after successful order creation.
 *
 * Lernziel: Serializable Event for JMS ObjectMessage
 */
public class OrderCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID eventId;
    private UUID orderId;
    private UUID userId;
    private LocalDateTime createdAt;
    private List<OrderCreatedEventItem> items;
    private BigDecimal totalPrice;

    // Constructors
    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(UUID eventId, UUID orderId, UUID userId, LocalDateTime createdAt,
                            List<OrderCreatedEventItem> items, BigDecimal totalPrice) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.items = items;
        this.totalPrice = totalPrice;
    }

    // Getters
    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderCreatedEventItem> getItems() {
        return items;
    }

    public void setItems(List<OrderCreatedEventItem> items) {
        this.items = items;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    @Override
    public String toString() {
        return "OrderCreatedEvent{" +
                "eventId=" + eventId +
                ", orderId=" + orderId +
                ", userId=" + userId +
                ", createdAt=" + createdAt +
                ", items=" + items +
                ", totalPrice=" + totalPrice +
                '}';
    }

    /**
     * Item within OrderCreatedEvent.
     */
    public static class OrderCreatedEventItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private UUID productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;

        public OrderCreatedEventItem() {
        }

        public OrderCreatedEventItem(UUID productId, String productName, int quantity,
                                    BigDecimal unitPrice, BigDecimal totalPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
        }

        public UUID getProductId() {
            return productId;
        }

        public void setProductId(UUID productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public BigDecimal getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
        }

        @Override
        public String toString() {
            return "OrderCreatedEventItem{" +
                    "productId=" + productId +
                    ", productName='" + productName + '\'' +
                    ", quantity=" + quantity +
                    ", unitPrice=" + unitPrice +
                    ", totalPrice=" + totalPrice +
                    '}';
        }
    }
}

