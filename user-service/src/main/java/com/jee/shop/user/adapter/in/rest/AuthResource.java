package com.jee.shop.user.adapter.in.rest;

import com.jee.shop.user.application.dto.LoginRequest;
import com.jee.shop.user.application.dto.RegisterRequest;
import com.jee.shop.user.application.dto.TokenResponse;
import com.jee.shop.user.application.dto.UserDto;
import com.jee.shop.user.application.port.in.UserAccountUseCases;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication REST Resource.
 * Handles user registration, login, and token generation endpoints.
 *
 * Lernziel: JAX-RS @Path, @POST, @Produces, @Consumes, @Valid, DTOs,
 *           REST Resource, HTTP Status Codes, Response Building
 */
@Path("/auth")
@RequestScoped
public class  AuthResource {

    private static final Logger logger = LoggerFactory.getLogger(AuthResource.class);

    @Inject
    private UserAccountUseCases userAccountUseCases;

    /**
     * Registers a new user account.
     *
     * POST /users/auth/register
     *
     * @param request the registration request
     * @return 201 Created with user data, or 409 Conflict if user exists
     */
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(@Valid RegisterRequest request) {
        logger.info("Received registration request for username: {}", request.username());

        UserDto user = userAccountUseCases.register(request);

        logger.info("User registered successfully: {}", user.id());
        return Response.status(Response.Status.CREATED)
                .entity(user)
                .build();
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * POST /users/auth/login
     *
     * @param request the login request
     * @return 200 OK with token, or 401 Unauthorized
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@Valid LoginRequest request) {
        logger.info("Received login request for username: {}", request.username());

        TokenResponse response = userAccountUseCases.login(request);

        logger.info("Login successful for user: {}", request.username());
        return Response.ok(response).build();
    }
}

