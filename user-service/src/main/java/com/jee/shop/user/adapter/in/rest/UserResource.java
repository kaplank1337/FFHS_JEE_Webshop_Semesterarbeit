package com.jee.shop.user.adapter.in.rest;

import com.jee.shop.user.application.dto.UserDto;
import com.jee.shop.user.domain.exception.AuthenticationException;
import com.jee.shop.user.application.port.in.UserAccountUseCases;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User REST Resource.
 * Handles user profile endpoints.
 */
@Path("/me")
@RequestScoped
public class UserResource {

    private static final Logger logger = LoggerFactory.getLogger(UserResource.class);

    @Inject
    private UserAccountUseCases userAccountUseCases;

    /**
     * Retrieves the current user's profile.
     *
     * GET /users/me
     *
     * @param headers HTTP headers (used to extract Authorization header)
     * @return 200 OK with user data, or 401 Unauthorized
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUser(@Context HttpHeaders headers) {
        logger.debug("Received request for current user profile");

        String authHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isEmpty()) {
            logger.warn("No authorization header provided");
            throw new AuthenticationException("Authorization header is missing");
        }

        UserDto user = userAccountUseCases.getUserFromToken(authHeader)
                .orElseThrow(() -> new AuthenticationException("User not found or invalid token"));

        logger.info("Retrieved user profile: {}", user.id());
        return Response.ok(user).build();
    }

    /**
     * Deletes the account represented by the bearer token.
     * DELETE /users/me
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCurrentUser(@Context HttpHeaders headers) {
        String authHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isBlank()) {
            throw new AuthenticationException("Authorization header is missing");
        }

        userAccountUseCases.deleteUserFromToken(authHeader);
        return Response.noContent().build();
    }
}

