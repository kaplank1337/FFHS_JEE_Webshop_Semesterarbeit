package com.jee.shop.user.dto;

/**
 * Token Response DTO.
 * Returned after successful authentication.
 *
 * Lernziel: Java Records, API Response Design
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}

