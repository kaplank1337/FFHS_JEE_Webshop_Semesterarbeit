package com.jee.shop.user.service;

import com.jee.shop.user.dto.LoginRequest;
import com.jee.shop.user.dto.RegisterRequest;
import com.jee.shop.user.dto.TokenResponse;
import com.jee.shop.user.dto.TokenPayload;
import com.jee.shop.user.dto.UserDto;
import com.jee.shop.user.entity.UserEntity;
import com.jee.shop.user.entity.UserRole;
import com.jee.shop.user.exception.AuthenticationException;
import com.jee.shop.user.exception.UserAlreadyExistsException;
import com.jee.shop.user.mapper.UserMapper;
import com.jee.shop.user.repository.UserRepository;
import com.jee.shop.user.security.PasswordEncoder;
import com.jee.shop.user.security.TokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Authentication Service.
 * Handles user registration, login, and token generation.
 *
 * Lernziel: CDI @ApplicationScoped, @Transactional, Business Logic,
 *           Security Patterns, Dependency Injection
 */
@ApplicationScoped
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Inject
    private TokenService tokenService;

    @Inject
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder = new PasswordEncoder();

    /**
     * Registers a new user.
     *
     * @param request the registration request
     * @return the newly created user DTO
     * @throws UserAlreadyExistsException if username or email already exists
     */
    @Transactional
    public UserDto register(RegisterRequest request) {
        logger.info("User registration attempt for username: {}", request.username());

        // Check if username already exists
        if (userRepository.existsByUsername(request.username())) {
            logger.warn("Registration failed: Username already exists: {}", request.username());
            throw new UserAlreadyExistsException("Username already exists: " + request.username());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            logger.warn("Registration failed: Email already exists: {}", request.email());
            throw new UserAlreadyExistsException("Email already exists: " + request.email());
        }

        // Create new user with hashed password
        String passwordHash = passwordEncoder.encode(request.password());
        UserEntity user = new UserEntity(
                request.username(),
                request.email(),
                passwordHash,
                request.firstName(),
                request.lastName()
        );

        // Set default role
        user.setRole(UserRole.CUSTOMER);

        // Persist user
        userRepository.save(user);

        logger.info("User registered successfully: {} (ID: {})", request.username(), user.getId());
        return UserMapper.toDto(user);
    }

    /**
     * Authenticates a user and returns a token.
     *
     * @param request the login request
     * @return token response with access token
     * @throws AuthenticationException if credentials are invalid
     */
    public TokenResponse login(LoginRequest request) {
        logger.info("Login attempt for username: {}", request.username());

        // Find user by username
        Optional<UserEntity> userOpt = userRepository.findByUsername(request.username());
        if (userOpt.isEmpty()) {
            logger.warn("Login failed: User not found: {}", request.username());
            throw new AuthenticationException("Invalid username or password");
        }

        UserEntity user = userOpt.get();

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            logger.warn("Login failed: Invalid password for user: {}", request.username());
            throw new AuthenticationException("Invalid username or password");
        }

        // Generate token
        String token = tokenService.generateToken(user.getId(), user.getUsername(), user.getRole());

        long expirationSeconds = 3600; // TODO: Get from config

        logger.info("Login successful for user: {}", request.username());
        return new TokenResponse(token, "Bearer", expirationSeconds);
    }

    /**
     * Retrieves user information by username.
     *
     * @param username the username
     * @return user DTO if found
     */
    public Optional<UserDto> getUserByUsername(String username) {
        logger.debug("Fetching user: {}", username);
        return userRepository.findByUsername(username)
                .map(UserMapper::toDto);
    }

    /**
     * Validates a token and returns the user associated with it.
     *
     * @param token the JWT token
     * @return user DTO if token is valid
     */
    public Optional<UserDto> getUserFromToken(String token) {
        logger.debug("Validating token and retrieving user");

        var payload = tokenService.validateToken(token);
        if (payload == null) {
            logger.warn("Invalid token");
            return Optional.empty();
        }

        return userRepository.findById(payload.getSub())
                .map(UserMapper::toDto);
    }

    /**
     * Deletes the account identified by the supplied bearer token.
     * The token is checked before its subject is used, so callers cannot select
     * another account by supplying an ID in the request.
     */
    @Transactional
    public void deleteUserFromToken(String authorizationHeader) {
        TokenPayload payload = tokenService.validateToken(authorizationHeader);
        if (payload == null) {
            throw new AuthenticationException("Missing or invalid bearer token");
        }

        UserEntity user = userRepository.findById(payload.getSub())
                .orElseThrow(() -> new AuthenticationException("Token user no longer exists"));
        userRepository.delete(user);
        logger.info("Deleted account for user ID: {}", payload.getSub());
    }
}

