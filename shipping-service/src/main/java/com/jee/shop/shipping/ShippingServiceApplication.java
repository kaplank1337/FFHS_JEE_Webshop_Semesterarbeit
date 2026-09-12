package com.jee.shop.shipping;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application for Shipping Service.
 */
@ApplicationPath("/shipments")
public class ShippingServiceApplication extends Application {
    // Implicit scanning of @Path annotated resources
}

