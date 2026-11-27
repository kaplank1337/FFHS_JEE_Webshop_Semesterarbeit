package com.jee.shop.order.domain.model;

/**
 * Order Status Enum.
 */
public enum OrderStatus {
    CREATED("Created"),
    CONFIRMED("Confirmed"),
    SHIPPING("Shipping"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

