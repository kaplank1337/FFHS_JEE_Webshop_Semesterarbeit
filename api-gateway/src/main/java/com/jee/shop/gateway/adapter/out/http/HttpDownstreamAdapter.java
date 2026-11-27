package com.jee.shop.gateway.adapter.out.http;

import com.jee.shop.gateway.application.dto.ProxyResult;
import com.jee.shop.gateway.application.port.out.OrderServicePort;
import com.jee.shop.gateway.application.port.out.ProductCatalogPort;
import com.jee.shop.gateway.application.port.out.UserServicePort;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HTTP adapter for the Gateway's User-Service and Order-Service ports. */
@ApplicationScoped
public class HttpDownstreamAdapter implements UserServicePort, OrderServicePort, ProductCatalogPort {
    private static final Logger logger = LoggerFactory.getLogger(HttpDownstreamAdapter.class);
    private static final String USER_URL = System.getenv().getOrDefault(
            "USER_SERVICE_URL", "http://localhost:10080/user-service-1.0.0/users");
    private static final String ORDER_URL = System.getenv().getOrDefault(
            "ORDER_SERVICE_URL", "http://localhost:11080/order-service-1.0.0/api/orders");
    private static final String PRODUCT_URL = System.getenv().getOrDefault(
            "ORDER_SERVICE_API_URL", "http://localhost:11080/order-service-1.0.0/api");

    private final Client client = ClientBuilder.newClient();

    @Override
    public ProxyResult forwardUser(String method, String path, String body, String authorization) {
        return request(method, USER_URL + suffix(path), body, authorization, "User-Service");
    }

    @Override
    public ProxyResult forwardOrder(String method, String path, String body, String authorization) {
        return request(method, ORDER_URL + suffix(path), body, authorization, "Order-Service");
    }

    @Override
    public ProxyResult find(String path) {
        return request("GET", PRODUCT_URL + "/products" + suffix(path), null, null, "Order-Service");
    }

    @PreDestroy
    public void close() {
        client.close();
    }

    private ProxyResult request(String method, String url, String body, String authorization, String serviceName) {
        try {
            var request = client.target(url).request(MediaType.APPLICATION_JSON_TYPE);
            if (authorization != null && !authorization.isBlank()) {
                request.header("Authorization", authorization);
            }
            Response upstream = switch (method) {
                case "GET" -> request.get();
                case "POST" -> request.post(Entity.entity(body == null ? "" : body, MediaType.APPLICATION_JSON_TYPE));
                case "PUT" -> request.put(Entity.entity(body == null ? "" : body, MediaType.APPLICATION_JSON_TYPE));
                case "DELETE" -> request.delete();
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            };
            try (upstream) {
                String responseBody = upstream.hasEntity() ? upstream.readEntity(String.class) : null;
                String contentType = upstream.getMediaType() == null
                        ? MediaType.APPLICATION_JSON
                        : upstream.getMediaType().toString();
                return new ProxyResult(upstream.getStatus(), contentType, responseBody);
            }
        } catch (Exception exception) {
            logger.error("Could not forward {} request to {}", method, serviceName, exception);
            return ProxyResult.unavailable(serviceName);
        }
    }

    private String suffix(String path) {
        return path == null || path.isBlank() ? "" : "/" + path;
    }
}
