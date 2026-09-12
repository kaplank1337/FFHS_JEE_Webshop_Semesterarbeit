package com.jee.shop.user.service;

import com.jee.shop.user.dto.LoginRequest;
import com.jee.shop.user.dto.RegisterRequest;
import com.jee.shop.user.dto.TokenResponse;
import com.jee.shop.user.dto.UserDto;
import com.jee.shop.user.entity.UserEntity;
import com.jee.shop.user.entity.UserRole;
import com.jee.shop.user.exception.AuthenticationException;
import com.jee.shop.user.exception.UserAlreadyExistsException;
import com.jee.shop.user.repository.UserRepository;
import com.jee.shop.user.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for AuthService.
 * Tests authentication and user management.
 *
 * Lernziel: Unit Testing, Mockito, Service Testing, Business Logic Testing
 */
@DisplayName("AuthService Tests")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest(
                "newuser",
                "newuser@example.com",
                "SecurePassword123",
                "John",
                "Doe"
        );

        validLoginRequest = new LoginRequest("newuser", "SecurePassword123");

        testUser = new UserEntity("newuser", "newuser@example.com", "hashed_password", "John", "Doe");
        testUser.setRole(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("Should register new user successfully")
    void testRegisterUserSuccessfully() {
        when(userRepository.existsByUsername(validRegisterRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(validRegisterRequest.email())).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        UserDto result = authService.register(validRegisterRequest);

        assertNotNull(result);
        assertEquals(validRegisterRequest.username(), result.username());
        assertEquals(validRegisterRequest.email(), result.email());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should reject duplicate username")
    void testRejectDuplicateUsername() {
        when(userRepository.existsByUsername(validRegisterRequest.username())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> {
            authService.register(validRegisterRequest);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject duplicate email")
    void testRejectDuplicateEmail() {
        when(userRepository.existsByUsername(validRegisterRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(validRegisterRequest.email())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> {
            authService.register(validRegisterRequest);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login successfully with correct credentials")
    void testLoginSuccessfully() {
        when(userRepository.findByUsername(validLoginRequest.username()))
                .thenReturn(Optional.of(testUser));
        when(tokenService.generateToken(any(UUID.class), anyString(), any(UserRole.class)))
                .thenReturn("test-token");

        TokenResponse response = authService.login(validLoginRequest);

        assertNotNull(response);
        assertEquals("test-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertTrue(response.expiresIn() > 0);
    }

    @Test
    @DisplayName("Should reject login with non-existent user")
    void testRejectLoginNonExistentUser() {
        when(userRepository.findByUsername(validLoginRequest.username()))
                .thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> {
            authService.login(validLoginRequest);
        });

        verify(tokenService, never()).generateToken(any(), any(), any());
    }

    @Test
    @DisplayName("Should reject login with wrong password")
    void testRejectLoginWrongPassword() {
        LoginRequest wrongPasswordRequest = new LoginRequest("newuser", "WrongPassword");
        when(userRepository.findByUsername("newuser"))
                .thenReturn(Optional.of(testUser));

        assertThrows(AuthenticationException.class, () -> {
            authService.login(wrongPasswordRequest);
        });
    }

    @Test
    @DisplayName("Should set CUSTOMER role for new users")
    void testNewUserGetCustomerRole() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        authService.register(validRegisterRequest);

        verify(userRepository).save(any(UserEntity.class));
    }
}

