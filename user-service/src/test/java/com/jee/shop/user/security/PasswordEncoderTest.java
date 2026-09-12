package com.jee.shop.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for PasswordEncoder.
 * Tests password encoding and verification functionality.
 *
 * Lernziel: Unit Testing, JUnit 5, Security Testing
 */
@DisplayName("PasswordEncoder Tests")
class PasswordEncoderTest {

    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new PasswordEncoder();
    }

    @Test
    @DisplayName("Should encode password successfully")
    void testEncodePassword() {
        String plainPassword = "mySecurePassword123";
        String encoded = encoder.encode(plainPassword);

        assertNotNull(encoded);
        assertNotEmpty(encoded);
        assertNotEquals(plainPassword, encoded);
    }

    @Test
    @DisplayName("Should generate different hashes for same password")
    void testEncodingShouldCreateDifferentHashes() {
        String plainPassword = "mySecurePassword123";
        String encoded1 = encoder.encode(plainPassword);
        String encoded2 = encoder.encode(plainPassword);

        // Different hashes due to different salt
        assertNotEquals(encoded1, encoded2);
    }

    @Test
    @DisplayName("Should verify correct password")
    void testVerifyCorrectPassword() {
        String plainPassword = "mySecurePassword123";
        String encoded = encoder.encode(plainPassword);

        assertTrue(encoder.matches(plainPassword, encoded));
    }

    @Test
    @DisplayName("Should reject incorrect password")
    void testRejectIncorrectPassword() {
        String plainPassword = "mySecurePassword123";
        String wrongPassword = "wrongPassword";
        String encoded = encoder.encode(plainPassword);

        assertFalse(encoder.matches(wrongPassword, encoded));
    }

    @Test
    @DisplayName("Should handle special characters in password")
    void testSpecialCharactersInPassword() {
        String plainPassword = "P@ssw0rd!#$%^&*()[]{}";
        String encoded = encoder.encode(plainPassword);

        assertTrue(encoder.matches(plainPassword, encoded));
    }

    @Test
    @DisplayName("Should be case sensitive")
    void testCaseSensitive() {
        String plainPassword = "MyPassword";
        String wrongPassword = "mypassword";
        String encoded = encoder.encode(plainPassword);

        assertFalse(encoder.matches(wrongPassword, encoded));
    }

    private void assertNotEmpty(String value) {
        assertNotNull(value);
        assertFalse(value.isEmpty());
    }
}

