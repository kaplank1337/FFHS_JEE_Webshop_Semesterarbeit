package com.jee.shop.user.resource;

import com.jee.shop.user.dto.UserDto;
import com.jee.shop.user.exception.UserNotFoundException;
import com.jee.shop.user.service.AuthService;
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
    private AuthService authService;

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
            throw new UserNotFoundException("Authorization header is missing");
        }

        UserDto user = authService.getUserFromToken(authHeader)
                .orElseThrow(() -> new UserNotFoundException("User not found or invalid token"));

        logger.info("Retrieved user profile: {}", user.id());
        return Response.ok(user).build();
    }
}

