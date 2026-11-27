package com.jee.shop.user.application.port.out;

import com.jee.shop.user.domain.model.UserEntity;

import java.util.Optional;
import java.util.UUID;

/** Outbound persistence contract for user accounts. */
public interface UserStore {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findById(UUID id);
    UserEntity save(UserEntity user);
    void delete(UserEntity user);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
