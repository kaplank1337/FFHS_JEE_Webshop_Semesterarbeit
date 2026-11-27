package com.jee.shop.order.configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/** Applies the Order-Service database migrations when the application starts. */
@Singleton
@Startup
public class OrderDatabaseMigrator {

    private static final Logger logger = LoggerFactory.getLogger(OrderDatabaseMigrator.class);

    @Resource(lookup = "java:app/jdbc/orderDS")
    private DataSource dataSource;

    @PostConstruct
    public void migrate() {
        logger.info("Applying Order-Service database migrations");
        Flyway.configure()
                .dataSource(dataSource)
                .schemas("orders")
                .defaultSchema("orders")
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
