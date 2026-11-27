package com.jee.shop.order.adapter.in.rest.exception;

import com.jee.shop.order.domain.exception.InsufficientStockException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Provider
public class InsufficientStockExceptionMapper implements ExceptionMapper<InsufficientStockException> {

    private static final Logger logger = LoggerFactory.getLogger(InsufficientStockExceptionMapper.class);

    @Override
    public Response toResponse(InsufficientStockException exception) {
        logger.warn("Insufficient stock: {}", exception.getMessage());

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

