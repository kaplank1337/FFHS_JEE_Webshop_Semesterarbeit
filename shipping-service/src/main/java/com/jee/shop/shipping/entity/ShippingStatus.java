package com.jee.shop.shipping.entity;

/**
 * Shipping Status Enum.
 */
public enum ShippingStatus {
    PREPARING("Preparing"),
    READY_FOR_SHIPMENT("Ready for Shipment"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String displayName;

    ShippingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

