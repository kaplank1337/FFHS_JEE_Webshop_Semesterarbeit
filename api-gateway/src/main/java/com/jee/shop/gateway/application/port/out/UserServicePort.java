package com.jee.shop.gateway.application.port.out;

import com.jee.shop.gateway.application.dto.ProxyResult;

/** Outbound HTTP contract for User-Service. */
public interface UserServicePort {
    ProxyResult forwardUser(String method, String path, String body, String authorization);
}
