package com.jee.shop.gateway.adapter.in.rest.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.container.PreMatching;

import java.io.IOException;
import java.util.List;

/** Applies permissive CORS headers to every API Gateway route. */
@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String ALLOW_ORIGIN = "*";
    private static final String ALLOW_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD";
    private static final String DEFAULT_ALLOW_HEADERS = "Accept, Authorization, Content-Type, Origin, X-Requested-With";

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        Response.ResponseBuilder preflight = Response.noContent()
                .header("Access-Control-Allow-Origin", ALLOW_ORIGIN)
                .header("Access-Control-Allow-Methods", ALLOW_METHODS)
                .header("Access-Control-Allow-Headers", allowedHeaders(request))
                .header("Access-Control-Max-Age", "86400")
                .header("Vary", "Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
        request.abortWith(preflight.build());
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        response.getHeaders().putSingle("Access-Control-Allow-Origin", ALLOW_ORIGIN);
        response.getHeaders().putSingle("Access-Control-Allow-Methods", ALLOW_METHODS);
        response.getHeaders().putSingle("Access-Control-Allow-Headers", allowedHeaders(request));
        response.getHeaders().putSingle("Access-Control-Max-Age", "86400");
        response.getHeaders().putSingle("Vary", "Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
    }

    private String allowedHeaders(ContainerRequestContext request) {
        String requestedHeaders = request.getHeaderString("Access-Control-Request-Headers");
        return requestedHeaders == null || requestedHeaders.isBlank()
                ? DEFAULT_ALLOW_HEADERS
                : requestedHeaders;
    }
}
