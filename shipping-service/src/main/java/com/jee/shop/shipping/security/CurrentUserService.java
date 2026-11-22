package com.jee.shop.shipping.security;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.StringReader;
import java.util.UUID;

/** Resolves the authenticated user's ID by asking User-Service to validate the JWT. */
@ApplicationScoped
public class CurrentUserService {

    private static final String USER_SERVICE_URL = System.getenv().getOrDefault(
            "USER_SERVICE_URL", "http://localhost:10080/user-service-1.0.0/users");

    private final Client client = ClientBuilder.newClient();

    @PreDestroy
    public void close() {
        client.close();
    }

    public UUID requireUserId(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)
                || authorizationHeader.substring(7).isBlank()) {
            throw new NotAuthorizedException("A valid Bearer token is required");
        }

        try (Response response = client.target(USER_SERVICE_URL + "/me")
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", authorizationHeader)
                .get()) {
            if (response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()
                    || response.getStatus() == Response.Status.FORBIDDEN.getStatusCode()) {
                throw new NotAuthorizedException("Bearer token is invalid or expired");
            }
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                throw new ServiceUnavailableException(
                        "User-Service could not verify the token (HTTP " + response.getStatus() + ")");
            }

            String body = response.readEntity(String.class);
            try (JsonReader reader = Json.createReader(new StringReader(body))) {
                JsonObject user = reader.readObject();
                return UUID.fromString(user.getString("id"));
            }
        } catch (NotAuthorizedException | ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceUnavailableException(
                    Response.status(Response.Status.SERVICE_UNAVAILABLE)
                            .entity("Could not contact User-Service to verify the token")
                            .build(), e);
        }
    }
}
