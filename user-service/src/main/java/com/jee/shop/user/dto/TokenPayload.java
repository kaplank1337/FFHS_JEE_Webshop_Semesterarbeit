package com.jee.shop.user.dto;

import com.jee.shop.user.entity.UserRole;
import java.util.UUID;

/**
 * JWT Token Payload.
 * Represents the claims in a JWT token.
 */
public class TokenPayload {

    private UUID sub; // subject (userId)
    private String username;
    private UserRole role;
    private long exp; // expiration time in seconds

    // Constructors
    public TokenPayload() {
    }

    public TokenPayload(UUID sub, String username, UserRole role, long exp) {
        this.sub = sub;
        this.username = username;
        this.role = role;
        this.exp = exp;
    }

    // Getters and Setters
    public UUID getSub() {
        return sub;
    }

    public void setSub(UUID sub) {
        this.sub = sub;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public long getExp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }

    @Override
    public String toString() {
        return "TokenPayload{" +
                "sub=" + sub +
                ", username='" + username + '\'' +
                ", role=" + role +
                ", exp=" + exp +
                '}';
    }
}

