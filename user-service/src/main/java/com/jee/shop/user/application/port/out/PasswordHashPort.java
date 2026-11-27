package com.jee.shop.user.application.port.out;

/** Password hashing boundary implemented by the security adapter. */
public interface PasswordHashPort {
    String encode(String password);
    boolean matches(String password, String encodedPassword);
}
