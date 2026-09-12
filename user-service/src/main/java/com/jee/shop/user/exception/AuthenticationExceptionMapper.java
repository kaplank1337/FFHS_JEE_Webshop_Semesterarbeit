package com.jee.shop.user.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception Mapper for AuthenticationException.
 * Maps auth exceptions to HTTP 401 Unauthorized responses.
 *
 * Lernziel: JAX-RS ExceptionMapper, @Provider, Global Exception Handling
 */
@Provider
public class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationExceptionMapper.class);

    @Override
    public Response toResponse(AuthenticationException exception) {
        logger.warn("Authentication failed: {}", exception.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 401);
        error.put("error", "Unauthorized");
        error.put("message", exception.getMessage());

        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(error)
                .build();
    }
}

