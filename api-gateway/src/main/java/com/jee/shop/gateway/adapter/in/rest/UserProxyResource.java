package com.jee.shop.gateway.adapter.in.rest;

import com.jee.shop.gateway.application.port.in.GatewayRoutingUseCases;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** REST inbound adapter for User-Service routes. */
@Path("/users")
@RequestScoped
public class UserProxyResource {
    @Inject
    private GatewayRoutingUseCases routing;

    @POST
    @Path("/{path: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(@PathParam("path") String path, String body) {
        return Responses.from(routing.routeUser("POST", path, body, null));
    }

    @GET
    @Path("/{path: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("path") String path,
                        @HeaderParam("Authorization") String authorization) {
        return Responses.from(routing.routeUser("GET", path, null, authorization));
    }

    @DELETE
    @Path("/{path: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("path") String path,
                           @HeaderParam("Authorization") String authorization) {
        return Responses.from(routing.routeUser("DELETE", path, null, authorization));
    }
}
