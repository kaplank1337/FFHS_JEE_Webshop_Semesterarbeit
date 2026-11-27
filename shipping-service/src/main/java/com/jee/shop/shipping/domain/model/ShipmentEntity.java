package com.jee.shop.shipping.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shipment Entity.
 * Represents a shipment/delivery for an order.
 *
 * Lernziel: JPA Entity, @Entity, @Table, @Enumerated, Business Entity
 */
@Entity
@Table(name = "shipment", schema = "shipping")
public class ShipmentEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "order_id", nullable = false, columnDefinition = "UUID")
    private UUID orderId;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_status", nullable = false, length = 30)
    private ShippingStatus shippingStatus;

    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public ShipmentEntity() {
        this.id = UUID.randomUUID();
        this.shippingStatus = ShippingStatus.PREPARING;
        this.createdAt = LocalDateTime.now();
        this.trackingNumber = generateTrackingNumber();
    }

    public ShipmentEntity(UUID orderId, UUID userId) {
        this();
        this.orderId = orderId;
        this.userId = userId;
    }

    // Lifecycle callbacks
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Business methods
    private String generateTrackingNumber() {
        // Generate simple tracking number: SHIP-<timestamp>-<random>
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return String.format("SHIP-%d-%d", timestamp, random);
    }

    public boolean canTransitionTo(ShippingStatus newStatus) {
        // Simple state machine for shipping status transitions
        return switch (this.shippingStatus) {
            case PREPARING -> newStatus == ShippingStatus.READY_FOR_SHIPMENT || newStatus == ShippingStatus.CANCELLED;
            case READY_FOR_SHIPMENT -> newStatus == ShippingStatus.SHIPPED || newStatus == ShippingStatus.CANCELLED;
            case SHIPPED -> newStatus == ShippingStatus.DELIVERED || newStatus == ShippingStatus.CANCELLED;
            case DELIVERED -> false; // Terminal state
            case CANCELLED -> false; // Terminal state
        };
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public ShippingStatus getShippingStatus() {
        return shippingStatus;
    }

    public void setShippingStatus(ShippingStatus shippingStatus) {
        this.shippingStatus = shippingStatus;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ShipmentEntity{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", userId=" + userId +
                ", shippingStatus=" + shippingStatus +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

