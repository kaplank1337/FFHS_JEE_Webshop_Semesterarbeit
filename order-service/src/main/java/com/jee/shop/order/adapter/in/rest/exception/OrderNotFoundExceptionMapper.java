package com.jee.shop.order.adapter.in.rest.exception;

import com.jee.shop.order.domain.exception.OrderNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class OrderNotFoundExceptionMapper implements ExceptionMapper<OrderNotFoundException> {

    @Override
    public Response toResponse(OrderNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Not Found", "message", exception.getMessage()))
                .build();
    }
}
