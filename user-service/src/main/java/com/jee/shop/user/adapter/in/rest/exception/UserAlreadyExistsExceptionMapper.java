package com.jee.shop.user.adapter.in.rest.exception;

import com.jee.shop.user.domain.exception.UserAlreadyExistsException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception Mapper for UserAlreadyExistsException.
 * Maps duplicate user exceptions to HTTP 409 Conflict responses.
 */
@Provider
public class UserAlreadyExistsExceptionMapper implements ExceptionMapper<UserAlreadyExistsException> {

    private static final Logger logger = LoggerFactory.getLogger(UserAlreadyExistsExceptionMapper.class);

    @Override
    public Response toResponse(UserAlreadyExistsException exception) {
        logger.warn("User already exists: {}", exception.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 409);
        error.put("error", "Conflict");
        error.put("message", exception.getMessage());

        return Response.status(Response.Status.CONFLICT)
                .entity(error)
                .build();
    }
}

