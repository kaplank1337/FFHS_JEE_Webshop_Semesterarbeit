package com.jee.shop.gateway.adapter.in.rest;

import com.jee.shop.gateway.application.dto.ProxyResult;
import jakarta.ws.rs.core.Response;

/** Maps application-level proxy results to JAX-RS responses. */
final class Responses {
    private Responses() {}

    static Response from(ProxyResult result) {
        Response.ResponseBuilder response = Response.status(result.status());
        if (result.body() != null && !result.body().isEmpty()) {
            response.type(result.contentType()).entity(result.body());
        }
        return response.build();
    }
}
