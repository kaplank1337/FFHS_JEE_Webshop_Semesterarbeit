package com.jee.shop.gateway.application.service;

import com.jee.shop.gateway.application.dto.ProxyResult;
import com.jee.shop.gateway.application.port.in.GatewayRoutingUseCases;
import com.jee.shop.gateway.application.port.out.OrderServicePort;
import com.jee.shop.gateway.application.port.out.ProductCatalogPort;
import com.jee.shop.gateway.application.port.out.UserServicePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Coordinates Gateway requests with the downstream service ports. */
@ApplicationScoped
public class GatewayRoutingService implements GatewayRoutingUseCases {
    @Inject
    private UserServicePort userService;

    @Inject
    private OrderServicePort orderService;

    @Inject
    private ProductCatalogPort productCatalog;

    @Override
    public ProxyResult routeUser(String method, String path, String body, String authorization) {
        return userService.forwardUser(method, path, body, authorization);
    }

    @Override
    public ProxyResult routeOrder(String method, String path, String body, String authorization) {
        return orderService.forwardOrder(method, path, body, authorization);
    }

    @Override
    public ProxyResult routeProduct(String path) {
        return productCatalog.find(path);
    }
}
