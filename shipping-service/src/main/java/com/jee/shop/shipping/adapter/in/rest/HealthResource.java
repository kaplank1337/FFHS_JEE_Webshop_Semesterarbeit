package com.jee.shop.shipping.adapter.in.rest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Shipping Service Health Check Resource.
 */
@Path("/health")
@RequestScoped
public class HealthResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok("{\"status\": \"UP\"}").build();
    }

    @GET
    @Path("/ready")
    @Produces(MediaType.APPLICATION_JSON)
    public Response ready() {
        return Response.ok("{\"status\": \"READY\"}").build();
    }
}

