package com.jee.shop.user.application.port.in;

import com.jee.shop.user.application.dto.LoginRequest;
import com.jee.shop.user.application.dto.RegisterRequest;
import com.jee.shop.user.application.dto.TokenResponse;
import com.jee.shop.user.application.dto.UserDto;

import java.util.Optional;

/** Inbound use cases exposed by the user account service. */
public interface UserAccountUseCases {
    UserDto register(RegisterRequest request);
    TokenResponse login(LoginRequest request);
    Optional<UserDto> getUserFromToken(String token);
    void deleteUserFromToken(String authorizationHeader);
}
