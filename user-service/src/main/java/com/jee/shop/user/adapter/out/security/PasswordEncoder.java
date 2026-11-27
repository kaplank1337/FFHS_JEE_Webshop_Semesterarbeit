package com.jee.shop.user.adapter.out.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password Encoder using SHA-256 with Salt.
 * Provides secure password hashing without storing plain text passwords.
 *
 * Lernziel: Security, Password Hashing, Salt
 */
@ApplicationScoped
public class PasswordEncoder implements com.jee.shop.user.application.port.out.PasswordHashPort {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoder.class);

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    private final SecureRandom random = new SecureRandom();

    /**
     * Encodes (hashes) a plain text password.
     *
     * @param plainPassword the plain text password
     * @return the encoded password with salt (format: salt$hash)
     */
    public String encode(String plainPassword) {
        logger.debug("Encoding password");

        try {
            // Generate random salt
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // Hash the password with salt
            byte[] hash = hashPassword(plainPassword, salt);

            // Combine salt and hash, encode as Base64
            byte[] combined = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hash, 0, combined, salt.length, hash.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            logger.error("Error encoding password", e);
            throw new RuntimeException("Password encoding failed", e);
        }
    }

    /**
     * Matches a plain text password against an encoded password.
     *
     * @param plainPassword the plain text password to verify
     * @param encodedPassword the encoded password (with salt)
     * @return true if password matches, false otherwise
     */
    public boolean matches(String plainPassword, String encodedPassword) {
        logger.debug("Matching password");

        try {
            // Decode the stored password (contains salt + hash)
            byte[] combined = Base64.getDecoder().decode(encodedPassword);

            // Extract salt (first SALT_LENGTH bytes)
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);

            // Extract stored hash (remaining bytes)
            byte[] storedHash = new byte[combined.length - SALT_LENGTH];
            System.arraycopy(combined, SALT_LENGTH, storedHash, 0, storedHash.length);

            // Hash the provided password with the same salt
            byte[] newHash = hashPassword(plainPassword, salt);

            // Compare hashes (constant-time comparison)
            return constantTimeEquals(newHash, storedHash);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid encoded password format", e);
            return false;
        } catch (Exception e) {
            logger.error("Error matching password", e);
            return false;
        }
    }

    /**
     * Hashes a password with the given salt using SHA-256.
     *
     * @param password the password to hash
     * @param salt the salt
     * @return the hash
     */
    private byte[] hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(ALGORITHM);

        // Add salt to digest
        md.update(salt);

        // Hash the password
        return md.digest(password.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Constant-time byte array comparison to prevent timing attacks.
     *
     * @param a first array
     * @param b second array
     * @return true if arrays are equal
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }

        return result == 0;
    }
}

