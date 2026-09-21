package com.jee.shop.gateway.resource;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API Gateway Proxy for User Service.
 * Routes all user-related requests to the User Service.
 */
@Path("/users")
@RequestScoped
public class UserProxyResource {

    private static final Logger logger = LoggerFactory.getLogger(UserProxyResource.class);
    private static final String USER_SERVICE_URL = "http://localhost:8081/users";
    private static final Client client = ClientBuilder.newClient();

    /**
     * Proxy POST requests to User Service (/auth/register, /auth/login)
     */
    @POST
    @Path("/{path: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxyPost(@PathParam("path") String path, String body) {
        String targetUrl = USER_SERVICE_URL + "/" + path;
        logger.info("Proxying POST request to: {}", targetUrl);

        try {
            Response response = client.target(targetUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(body, MediaType.APPLICATION_JSON));

            logger.info("Response status: {}", response.getStatus());
            return Response.status(response.getStatus())
                    .entity(response.readEntity(String.class))
                    .build();
        } catch (Exception e) {
            logger.error("Error proxying request", e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("{\"error\": \"User Service unavailable\"}")
                    .build();
        }
    }

    /**
     * Proxy GET requests to User Service (/me)
     */
    @GET
    @Path("/{path: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxyGet(@PathParam("path") String path,
                            @HeaderParam("Authorization") String authHeader) {
        String targetUrl = USER_SERVICE_URL + "/" + path;
        logger.info("Proxying GET request to: {}", targetUrl);

        try {
            var request = client.target(targetUrl).request(MediaType.APPLICATION_JSON);
            if (authHeader != null) {
                request = request.header("Authorization", authHeader);
            }

            Response response = request.get();
            logger.info("Response status: {}", response.getStatus());
            return Response.status(response.getStatus())
                    .entity(response.readEntity(String.class))
                    .build();
        } catch (Exception e) {
            logger.error("Error proxying request", e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("{\"error\": \"User Service unavailable\"}")
                    .build();
        }
    }
}

