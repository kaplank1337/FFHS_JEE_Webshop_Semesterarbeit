package com.jee.shop.order.domain.model;

/**
 * Product Category Enum.
 */
public enum ProductCategory {
    WATER("Wasser"),
    SOFT_DRINKS("Softgetränke"),
    JUICE("Säfte"),
    BEER("Bier"),
    COFFEE("Kaffee");

    private final String displayName;

    ProductCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

