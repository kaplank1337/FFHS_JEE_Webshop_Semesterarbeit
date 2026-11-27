package com.jee.shop.order.application.port.out;

import java.util.UUID;

/** Resolves and validates the identity represented by an authorization header. */
public interface CurrentUserPort {
    UUID requireUserId(String authorizationHeader);
}
