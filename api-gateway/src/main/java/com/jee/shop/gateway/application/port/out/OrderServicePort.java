package com.jee.shop.gateway.application.port.out;

import com.jee.shop.gateway.application.dto.ProxyResult;

/** Outbound HTTP contract for Order-Service. */
public interface OrderServicePort {
    ProxyResult forwardOrder(String method, String path, String body, String authorization);
}
