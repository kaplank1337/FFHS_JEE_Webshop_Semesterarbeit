package com.jee.shop.order.messaging;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DeliveryStatusChangedEvent DTO for JMS messaging.
 * Published by Shipping Service when shipment status changes.
 * Consumed by Order Service and WebSocket Broadcaster.
 *
 * Lernziel: Event DTO für Topic-basierte Kommunikation
 */
public class DeliveryStatusChangedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID eventId;
    private UUID orderId;
    private UUID shipmentId;
    private String status;  // ShippingStatus as String (for serialization)
    private String trackingNumber;
    private LocalDateTime timestamp;

    // Constructors
    public DeliveryStatusChangedEvent() {
    }

    public DeliveryStatusChangedEvent(UUID eventId, UUID orderId, UUID shipmentId,
                                      String status, String trackingNumber, LocalDateTime timestamp) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.shipmentId = shipmentId;
        this.status = status;
        this.trackingNumber = trackingNumber;
        this.timestamp = timestamp;
    }

    // Getters & Setters
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

    public UUID getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(UUID shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "DeliveryStatusChangedEvent{" +
                "eventId=" + eventId +
                ", orderId=" + orderId +
                ", shipmentId=" + shipmentId +
                ", status='" + status + '\'' +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

