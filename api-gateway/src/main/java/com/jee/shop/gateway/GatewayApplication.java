package com.jee.shop.gateway;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import com.jee.shop.gateway.filter.CorsFilter;
import com.jee.shop.gateway.resource.HealthResource;
import com.jee.shop.gateway.resource.OrderProxyResource;
import com.jee.shop.gateway.resource.ProductProxyResource;
import com.jee.shop.gateway.resource.UserProxyResource;

import java.util.Set;

/**
 * JAX-RS Application for API Gateway.
 * Defines the root path for all REST endpoints.
 */
@ApplicationPath("/api")
public class GatewayApplication extends Application {

    /** Explicit registration ensures the global CORS filter is active on GlassFish. */
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                CorsFilter.class,
                HealthResource.class,
                OrderProxyResource.class,
                ProductProxyResource.class,
                UserProxyResource.class
        );
    }
}

