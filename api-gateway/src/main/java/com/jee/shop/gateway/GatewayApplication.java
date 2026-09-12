package com.jee.shop.gateway;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application for API Gateway.
 * Defines the root path for all REST endpoints.
 */
@ApplicationPath("/api")
public class GatewayApplication extends Application {
    // Implicit scanning of @Path annotated resources
}

