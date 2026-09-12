package com.jee.shop.user;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application for User Service.
 * Defines the root path for all REST endpoints in this service.
 *
 * Lernziel: JAX-RS Application, @ApplicationPath
 */
@ApplicationPath("/users")
public class UserServiceApplication extends Application {
    // Implicit scanning of @Path annotated resources
}

