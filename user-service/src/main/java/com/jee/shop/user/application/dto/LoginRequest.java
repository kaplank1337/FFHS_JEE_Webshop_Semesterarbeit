package com.jee.shop.user.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login Request DTO.
 * Sent by clients to authenticate and receive a token.
 */
public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}

