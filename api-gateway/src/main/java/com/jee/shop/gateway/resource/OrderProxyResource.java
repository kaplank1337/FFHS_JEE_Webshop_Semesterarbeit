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

/** Routes order REST calls through the API Gateway to Order-Service. */
@Path("/orders")
@RequestScoped
public class OrderProxyResource {

    private static final Logger logger = LoggerFactory.getLogger(OrderProxyResource.class);
    private static final String ORDER_SERVICE_URL = System.getenv().getOrDefault(
            "ORDER_SERVICE_URL",
            "http://localhost:11080/order-service-1.0.0/api/orders");
    private static final Client CLIENT = ClientBuilder.newClient();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrders(@HeaderParam("Authorization") String authorization) {
        return forward("GET", "", null, authorization);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrder(String body, @HeaderParam("Authorization") String authorization) {
        return forward("POST", "", body, authorization);
    }

    @GET
    @Path("/{path: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrder(@PathParam("path") String path,
                             @HeaderParam("Authorization") String authorization) {
        return forward("GET", path, null, authorization);
    }

    @PUT
    @Path("/{path: .+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOrder(@PathParam("path") String path, String body,
                                @HeaderParam("Authorization") String authorization) {
        return forward("PUT", path, body, authorization);
    }

    @DELETE
    @Path("/{path: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteOrder(@PathParam("path") String path,
                                @HeaderParam("Authorization") String authorization) {
        return forward("DELETE", path, null, authorization);
    }

    private Response forward(String method, String path, String body, String authorization) {
        String targetUrl = ORDER_SERVICE_URL + (path.isEmpty() ? "" : "/" + path);
        logger.info("Proxying {} request to {}", method, targetUrl);

        try {
            var request = CLIENT.target(targetUrl).request(MediaType.APPLICATION_JSON);
            if (authorization != null) {
                request.header("Authorization", authorization);
            }

            Response upstream = switch (method) {
                case "GET" -> request.get();
                case "POST" -> request.post(Entity.entity(body, MediaType.APPLICATION_JSON));
                case "PUT" -> request.put(Entity.entity(body, MediaType.APPLICATION_JSON));
                case "DELETE" -> request.delete();
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            };

            try (upstream) {
                if (upstream.getStatus() == Response.Status.NO_CONTENT.getStatusCode()
                        || !upstream.hasEntity()) {
                    return Response.status(upstream.getStatus()).build();
                }

                String responseBody = upstream.readEntity(String.class);
                return Response.status(upstream.getStatus())
                        .type(upstream.getMediaType() == null
                                ? MediaType.APPLICATION_JSON_TYPE
                                : upstream.getMediaType())
                        .entity(responseBody)
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error proxying {} request to Order-Service", method, e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("{\"error\":\"Order-Service unavailable\"}")
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
    }
}
