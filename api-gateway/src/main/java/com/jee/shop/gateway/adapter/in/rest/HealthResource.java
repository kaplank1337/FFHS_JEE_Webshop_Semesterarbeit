package com.jee.shop.gateway.adapter.in.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Health Check Resource for API Gateway.
 */
@Path("/health")
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

