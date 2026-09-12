package com.jee.shop.user.security;

import com.jee.shop.user.config.ConfigService;
import com.jee.shop.user.dto.TokenPayload;
import com.jee.shop.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit Tests for TokenService.
 * Tests JWT token generation and validation.
 *
 * Lernziel: Unit Testing, Mockito, JWT Testing
 */
@DisplayName("TokenService Tests")
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private ConfigService configService;

    @InjectMocks
    private TokenService tokenService;

    private UUID testUserId;
    private String testUsername;
    private UserRole testRole;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUsername = "testuser";
        testRole = UserRole.CUSTOMER;

        when(configService.getJwtSecret()).thenReturn("test-secret-key-for-jwt-testing-purposes-only");
        when(configService.getJwtExpirationSeconds()).thenReturn(3600L);
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void testGenerateToken() {
        String token = tokenService.generateToken(testUserId, testUsername, testRole);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("Should validate correct token")
    void testValidateCorrectToken() {
        String token = tokenService.generateToken(testUserId, testUsername, testRole);

        TokenPayload payload = tokenService.validateToken(token);

        assertNotNull(payload);
        assertEquals(testUserId, payload.getSub());
        assertEquals(testUsername, payload.getUsername());
        assertEquals(testRole, payload.getRole());
    }

    @Test
    @DisplayName("Should reject invalid token signature")
    void testRejectInvalidSignature() {
        String token = tokenService.generateToken(testUserId, testUsername, testRole);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        TokenPayload payload = tokenService.validateToken(tampered);

        assertNull(payload);
    }

    @Test
    @DisplayName("Should strip Bearer prefix from token")
    void testStripBearerPrefix() {
        String token = tokenService.generateToken(testUserId, testUsername, testRole);
        String bearerToken = "Bearer " + token;

        TokenPayload payload = tokenService.validateToken(bearerToken);

        assertNotNull(payload);
        assertEquals(testUserId, payload.getSub());
    }

    @Test
    @DisplayName("Should reject malformed token")
    void testRejectMalformedToken() {
        String malformedToken = "not.a.valid.token";

        TokenPayload payload = tokenService.validateToken(malformedToken);

        assertNull(payload);
    }

    @Test
    @DisplayName("Should include role in token")
    void testRoleInToken() {
        UserRole adminRole = UserRole.ADMIN;
        String token = tokenService.generateToken(testUserId, testUsername, adminRole);

        TokenPayload payload = tokenService.validateToken(token);

        assertNotNull(payload);
        assertEquals(adminRole, payload.getRole());
    }
}

