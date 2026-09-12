package com.jee.shop.user.security;

import com.jee.shop.user.config.ConfigService;
import com.jee.shop.user.dto.TokenPayload;
import com.jee.shop.user.entity.UserRole;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * JWT Token Service.
 * Generates and validates JWT Bearer tokens for authentication.
 *
 * Lernziel: CDI @ApplicationScoped, JWT, Security, Stateless Authentication
 */
@ApplicationScoped
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    private static final String ALGORITHM = "HS256";
    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    @Inject
    private ConfigService configService;

    /**
     * Generates a JWT token for a user.
     *
     * @param userId the user ID
     * @param username the username
     * @param role the user role
     * @return JWT token
     */
    public String generateToken(UUID userId, String username, UserRole role) {
        logger.info("Generating token for user: {}", username);

        long now = System.currentTimeMillis() / 1000;
        long expirationSeconds = configService.getJwtExpirationSeconds();
        long expiresAt = now + expirationSeconds;

        String payload = String.format(
                "{\"sub\":\"%s\",\"username\":\"%s\",\"role\":\"%s\",\"iat\":%d,\"exp\":%d}",
                userId,
                username,
                role.name(),
                now,
                expiresAt
        );

        try {
            String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    HEADER.getBytes(StandardCharsets.UTF_8));
            String payloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    payload.getBytes(StandardCharsets.UTF_8));

            String message = header + "." + payloadEncoded;

            String signature = generateSignature(message);
            String token = message + "." + signature;

            logger.debug("Token generated successfully");
            return token;

        } catch (Exception e) {
            logger.error("Error generating token", e);
            throw new RuntimeException("Failed to generate token", e);
        }
    }

    /**
     * Validates a JWT token and extracts the payload.
     *
     * @param token the JWT token
     * @return TokenPayload if valid, null if invalid
     */
    public TokenPayload validateToken(String token) {
        logger.debug("Validating token");

        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                logger.warn("Invalid token format");
                return null;
            }

            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];

            // Verify signature
            String message = header + "." + payload;
            String expectedSignature = generateSignature(message);

            if (!signature.equals(expectedSignature)) {
                logger.warn("Invalid token signature");
                return null;
            }

            // Decode and parse payload
            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(payload),
                    StandardCharsets.UTF_8
            );

            TokenPayload tokenPayload = parsePayload(payloadJson);

            // Check expiration
            if (tokenPayload.getExp() < System.currentTimeMillis() / 1000) {
                logger.warn("Token expired");
                return null;
            }

            logger.debug("Token valid");
            return tokenPayload;

        } catch (Exception e) {
            logger.warn("Error validating token", e);
            return null;
        }
    }

    /**
     * Generates HMAC-SHA256 signature for JWT.
     *
     * @param message the message to sign (header.payload)
     * @return Base64 URL-encoded signature
     */
    private String generateSignature(String message) throws Exception {
        String secret = configService.getJwtSecret();

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                0,
                secret.getBytes(StandardCharsets.UTF_8).length,
                "HmacSHA256"
        );

        mac.init(secretKey);
        byte[] signatureBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

        return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
    }

    /**
     * Parses the JWT payload JSON.
     *
     * @param payloadJson the JSON payload
     * @return TokenPayload object
     */
    private TokenPayload parsePayload(String payloadJson) {
        // Simple JSON parsing without external library
        // In production, use Jackson

        TokenPayload payload = new TokenPayload();

        // Extract "sub" (userId)
        String subPattern = "\"sub\":\"";
        int subStart = payloadJson.indexOf(subPattern);
        if (subStart != -1) {
            int subEnd = payloadJson.indexOf("\"", subStart + subPattern.length());
            String userId = payloadJson.substring(subStart + subPattern.length(), subEnd);
            payload.setSub(UUID.fromString(userId));
        }

        // Extract "username"
        String usernamePattern = "\"username\":\"";
        int usernameStart = payloadJson.indexOf(usernamePattern);
        if (usernameStart != -1) {
            int usernameEnd = payloadJson.indexOf("\"", usernameStart + usernamePattern.length());
            String username = payloadJson.substring(usernameStart + usernamePattern.length(), usernameEnd);
            payload.setUsername(username);
        }

        // Extract "role"
        String rolePattern = "\"role\":\"";
        int roleStart = payloadJson.indexOf(rolePattern);
        if (roleStart != -1) {
            int roleEnd = payloadJson.indexOf("\"", roleStart + rolePattern.length());
            String role = payloadJson.substring(roleStart + rolePattern.length(), roleEnd);
            payload.setRole(UserRole.valueOf(role));
        }

        // Extract "exp" (expiration)
        String expPattern = "\"exp\":";
        int expStart = payloadJson.indexOf(expPattern);
        if (expStart != -1) {
            int expEnd = payloadJson.indexOf(",", expStart);
            if (expEnd == -1) {
                expEnd = payloadJson.indexOf("}", expStart);
            }
            String exp = payloadJson.substring(expStart + expPattern.length(), expEnd).trim();
            payload.setExp(Long.parseLong(exp));
        }

        return payload;
    }
}

