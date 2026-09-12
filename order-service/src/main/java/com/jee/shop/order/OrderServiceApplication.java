package com.jee.shop.order;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application for Order Service.
 * Defines the root path for all REST endpoints in this service.
 *
 * Lernziel: JAX-RS Application, @ApplicationPath
 */
@ApplicationPath("/orders")
public class OrderServiceApplication extends Application {
    // Implicit scanning of @Path annotated resources
}

