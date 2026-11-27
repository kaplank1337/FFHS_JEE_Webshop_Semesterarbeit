package com.jee.shop.shipping.application.port.out;

import java.util.UUID;

/** Resolves and validates the identity represented by an authorization header. */
public interface CurrentUserPort {
    UUID requireUserId(String authorizationHeader);
}
