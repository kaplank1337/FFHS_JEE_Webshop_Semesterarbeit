package com.jee.shop.user.adapter.out.persistence;

import com.jee.shop.user.domain.model.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * User Repository.
 * Handles database operations for UserEntity.
 *
 * Lernziel: JPQL Queries, @Inject, Repository Pattern
 * JPQL Query #1: Find user by username
 */
@ApplicationScoped
public class JpaUserRepository implements com.jee.shop.user.application.port.out.UserStore {

    private static final Logger logger = LoggerFactory.getLogger(JpaUserRepository.class);

    @PersistenceContext(unitName = "userServicePU")
    private EntityManager entityManager;

    /**
     * Finds a user by their unique username.
     * JPQL Query: Simple SELECT with WHERE clause
     *
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    public Optional<UserEntity> findByUsername(String username) {
        logger.debug("Searching for user with username: {}", username);

        try {
            UserEntity user = entityManager.createQuery(
                    "SELECT u FROM UserEntity u WHERE u.username = :username",
                    UserEntity.class)
                    .setParameter("username", username)
                    .getSingleResult();

            logger.debug("User found: {}", user.getId());
            return Optional.of(user);

        } catch (NoResultException e) {
            logger.debug("User not found with username: {}", username);
            return Optional.empty();
        }
    }

    /**
     * Finds a user by their unique ID.
     *
     * @param id the user ID
     * @return Optional containing the user if found
     */
    public Optional<UserEntity> findById(UUID id) {
        logger.debug("Searching for user with ID: {}", id);

        UserEntity user = entityManager.find(UserEntity.class, id);
        if (user != null) {
            logger.debug("User found: {}", user.getUsername());
            return Optional.of(user);
        }

        logger.debug("User not found with ID: {}", id);
        return Optional.empty();
    }

    /**
     * Persists a new user.
     *
     * @param user the user to persist
     * @return the persisted user with generated ID
     */
    @Transactional
    public UserEntity save(UserEntity user) {
        logger.info("Persisting user: {}", user.getUsername());
        entityManager.persist(user);
        return user;
    }

    /**
     * Deletes a user.
     *
     * @param user the user to delete
     */
    @Transactional
    public void delete(UserEntity user) {
        logger.info("Deleting user: {}", user.getId());
        entityManager.remove(entityManager.contains(user) ? user : entityManager.merge(user));
    }

    /**
     * Checks if a username already exists.
     *
     * @param username the username to check
     * @return true if username exists, false otherwise
     */
    public boolean existsByUsername(String username) {
        Long count = entityManager.createQuery(
                "SELECT COUNT(u) FROM UserEntity u WHERE u.username = :username",
                Long.class)
                .setParameter("username", username)
                .getSingleResult();

        return count > 0;
    }

    /**
     * Checks if an email already exists.
     *
     * @param email the email to check
     * @return true if email exists, false otherwise
     */
    public boolean existsByEmail(String email) {
        Long count = entityManager.createQuery(
                "SELECT COUNT(u) FROM UserEntity u WHERE u.email = :email",
                Long.class)
                .setParameter("email", email)
                .getSingleResult();

        return count > 0;
    }
}

