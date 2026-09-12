package com.jee.shop.user.dto;

import com.jee.shop.user.entity.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Data Transfer Object.
 * Returned when fetching user profile information.
 */
public record UserDto(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

