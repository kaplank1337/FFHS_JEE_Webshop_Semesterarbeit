package com.jee.shop.gateway.adapter.in.rest;

import com.jee.shop.gateway.application.port.in.GatewayRoutingUseCases;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** REST inbound adapter for Order-Service routes. */
@Path("/orders")
@RequestScoped
public class OrderProxyResource {
    @Inject
    private GatewayRoutingUseCases routing;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrders(@HeaderParam("Authorization") String authorization) {
        return Responses.from(routing.routeOrder("GET", "", null, authorization));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrder(String body, @HeaderParam("Authorization") String authorization) {
        return Responses.from(routing.routeOrder("POST", "", body, authorization));
    }

    @GET
    @Path("/{path: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrder(@PathParam("path") String path,
                             @HeaderParam("Authorization") String authorization) {
        return Responses.from(routing.routeOrder("GET", path, null, authorization));
    }

    @PUT
    @Path("/{path: .+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOrder(@PathParam("path") String path, String body,
                                @HeaderParam("Authorization") String authorization) {
        return Responses.from(routing.routeOrder("PUT", path, body, authorization));
    }

    @DELETE
    @Path("/{path: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteOrder(@PathParam("path") String path,
                                @HeaderParam("Authorization") String authorization) {
        return Responses.from(routing.routeOrder("DELETE", path, null, authorization));
    }
}
