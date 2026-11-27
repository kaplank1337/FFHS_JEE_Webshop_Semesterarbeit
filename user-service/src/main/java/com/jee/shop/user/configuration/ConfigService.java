package com.jee.shop.user.configuration;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration Service.
 * Reads configuration from environment variables.
 *
 * Lernziel: CDI @ApplicationScoped, Environment-based Configuration
 */
@ApplicationScoped
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    /**
     * Gets the JWT secret from environment or default.
     *
     * @return JWT secret key
     */
    public String getJwtSecret() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isEmpty()) {
            logger.warn("JWT_SECRET not set, using default (INSECURE for production!)");
            secret = "your-super-secret-jwt-key-change-in-production";
        }
        return secret;
    }

    /**
     * Gets JWT expiration time in seconds.
     *
     * @return expiration time
     */
    public long getJwtExpirationSeconds() {
        String exp = System.getenv("JWT_EXPIRATION_SECONDS");
        if (exp == null || exp.isEmpty()) {
            return 3600; // 1 hour default
        }
        return Long.parseLong(exp);
    }

    /**
     * Gets database host.
     *
     * @return DB host
     */
    public String getDbHost() {
        String host = System.getenv("DB_HOST");
        return host != null ? host : "localhost";
    }

    /**
     * Gets database port.
     *
     * @return DB port
     */
    public int getDbPort() {
        String port = System.getenv("DB_PORT");
        return port != null ? Integer.parseInt(port) : 5432;
    }

    /**
     * Gets database name.
     *
     * @return DB name
     */
    public String getDbName() {
        String name = System.getenv("DB_NAME");
        return name != null ? name : "webshop";
    }

    /**
     * Gets database user.
     *
     * @return DB user
     */
    public String getDbUser() {
        String user = System.getenv("DB_USER");
        return user != null ? user : "webshop";
    }

    /**
     * Gets database password.
     *
     * @return DB password
     */
    public String getDbPassword() {
        String password = System.getenv("DB_PASSWORD");
        return password != null ? password : "webshop123";
    }

    /**
     * Gets database schema.
     *
     * @return DB schema
     */
    public String getDbSchema() {
        String schema = System.getenv("DB_SCHEMA");
        return schema != null ? schema : "app_user";
    }

    /**
     * Gets JMS host.
     *
     * @return JMS host
     */
    public String getJmsHost() {
        String host = System.getenv("JMS_HOST");
        return host != null ? host : "localhost";
    }

    /**
     * Gets JMS port.
     *
     * @return JMS port
     */
    public int getJmsPort() {
        String port = System.getenv("JMS_PORT");
        return port != null ? Integer.parseInt(port) : 61616;
    }
}

