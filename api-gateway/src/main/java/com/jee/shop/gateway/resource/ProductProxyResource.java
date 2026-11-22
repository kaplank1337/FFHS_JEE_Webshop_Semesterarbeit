package com.jee.shop.gateway.resource;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Exposes the Order-Service product catalog through the API Gateway. */
@Path("/products")
@RequestScoped
public class ProductProxyResource {

    private static final Logger logger = LoggerFactory.getLogger(ProductProxyResource.class);
    private static final String ORDER_SERVICE_API_URL = System.getenv().getOrDefault(
            "ORDER_SERVICE_API_URL",
            "http://localhost:11080/order-service-1.0.0/api");
    private static final Client CLIENT = ClientBuilder.newClient();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProducts() {
        return forward("");
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProduct(@PathParam("id") String id) {
        return forward(id);
    }

    private Response forward(String path) {
        String targetUrl = ORDER_SERVICE_API_URL + "/products" + (path.isEmpty() ? "" : "/" + path);
        logger.info("Proxying product catalog request to {}", targetUrl);

        try (Response upstream = CLIENT.target(targetUrl).request(MediaType.APPLICATION_JSON).get()) {
            if (upstream.getStatus() == Response.Status.NO_CONTENT.getStatusCode() || !upstream.hasEntity()) {
                return Response.status(upstream.getStatus()).build();
            }

            String responseBody = upstream.readEntity(String.class);
            return Response.status(upstream.getStatus())
                    .type(upstream.getMediaType() == null
                            ? MediaType.APPLICATION_JSON_TYPE
                            : upstream.getMediaType())
                    .entity(responseBody)
                    .build();
        } catch (Exception e) {
            logger.error("Error proxying product catalog request to Order-Service", e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("{\"error\":\"Order-Service unavailable\"}")
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
    }
}
