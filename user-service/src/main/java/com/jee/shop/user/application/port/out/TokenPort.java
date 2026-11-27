package com.jee.shop.user.application.port.out;

import com.jee.shop.user.application.dto.TokenPayload;
import com.jee.shop.user.domain.model.UserRole;

import java.util.UUID;

/** JWT creation and verification boundary. */
public interface TokenPort {
    String generateToken(UUID userId, String username, UserRole role);
    TokenPayload validateToken(String token);
}
