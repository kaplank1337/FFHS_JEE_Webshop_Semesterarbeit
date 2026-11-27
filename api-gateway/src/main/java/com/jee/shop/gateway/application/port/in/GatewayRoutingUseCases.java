package com.jee.shop.gateway.application.port.in;

import com.jee.shop.gateway.application.dto.ProxyResult;

/** Inbound routing use cases exposed by the API Gateway. */
public interface GatewayRoutingUseCases {
    ProxyResult routeUser(String method, String path, String body, String authorization);
    ProxyResult routeOrder(String method, String path, String body, String authorization);
    ProxyResult routeProduct(String path);
}
