package com.jee.shop.shipping.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/** Applies Shipping-Service database migrations when the application starts. */
@Singleton
@Startup
public class ShippingDatabaseMigrator {

    private static final Logger logger = LoggerFactory.getLogger(ShippingDatabaseMigrator.class);

    @Resource(lookup = "java:app/jdbc/shippingDS")
    private DataSource dataSource;

    @PostConstruct
    public void migrate() {
        logger.info("Applying Shipping-Service database migrations");
        Flyway.configure()
                .dataSource(dataSource)
                .schemas("shipping")
                .defaultSchema("shipping")
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
