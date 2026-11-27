package com.jee.shop.shipping.application.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DeliveryStatusChangedEvent DTO for JMS messaging.
 * Published by Shipping Service when status changes.
 */
public class DeliveryStatusChangedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID eventId;
    private UUID orderId;
    private UUID shipmentId;
    private String status;
    private String trackingNumber;
    private LocalDateTime timestamp;

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

    // Getters
    public UUID getEventId() { return eventId; }
    public UUID getOrderId() { return orderId; }
    public UUID getShipmentId() { return shipmentId; }
    public String getStatus() { return status; }
    public String getTrackingNumber() { return trackingNumber; }
    public LocalDateTime getTimestamp() { return timestamp; }

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

