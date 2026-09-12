package com.jee.shop.user.entity;

/**
 * User roles in the system.
 *
 * Lernziel: Java Enums, Jakarta Role-Based Security Model
 */
public enum UserRole {
    CUSTOMER("Customer"),
    ADMIN("Administrator");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

