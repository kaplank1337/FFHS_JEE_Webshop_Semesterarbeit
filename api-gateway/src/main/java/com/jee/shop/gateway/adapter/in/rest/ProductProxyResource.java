package com.jee.shop.gateway.adapter.in.rest;

import com.jee.shop.gateway.application.port.in.GatewayRoutingUseCases;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** REST inbound adapter for product catalog routes. */
@Path("/products")
@RequestScoped
public class ProductProxyResource {
    @Inject
    private GatewayRoutingUseCases routing;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProducts() {
        return Responses.from(routing.routeProduct(""));
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProduct(@PathParam("id") String id) {
        return Responses.from(routing.routeProduct(id));
    }
}
