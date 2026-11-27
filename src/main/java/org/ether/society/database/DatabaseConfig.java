/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.database;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Database configuration and EntityManagerFactory setup.
 * Uses HikariCP for connection pooling and Hibernate for JPA.
 */
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static EntityManagerFactory entityManagerFactory;
    private static HikariDataSource dataSource;
    /** Latches offline mode once PostgreSQL is confirmed unreachable to avoid repeated 1s HikariCP timeouts per snapshot. */
    private static volatile boolean offlineModeDetected = false;

    /**
     * Gets or lazily initializes the JPA {@link EntityManagerFactory}.
     * Latches offline mode automatically on first unreachable PostgreSQL probe to prevent repeated pool timeouts.
     *
     * @return active {@link EntityManagerFactory}, or {@code null} when running in standalone offline mode
     */
    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (offlineModeDetected) {
            return null;
        }
        if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
            entityManagerFactory = createEntityManagerFactory();
            if (entityManagerFactory == null) {
                offlineModeDetected = true;
            }
        }
        return entityManagerFactory;
    }

    /**
     * Resets the latched offline database flag so the next call re-probes PostgreSQL availability.
     */
    public static synchronized void resetOfflineMode() {
        offlineModeDetected = false;
    }

    /**
     * Create EntityManagerFactory with Hibernate configuration.
     */
    private static EntityManagerFactory createEntityManagerFactory() {
        logger.info("Creating EntityManagerFactory...");

        // Load connection parameters from environment variables with defaults
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:54320/ether_simulation");
        String dbUser = System.getenv().getOrDefault("DB_USER", "ether");
        String dbPass = System.getenv().getOrDefault("DB_PASSWORD", "dev_password");

        Configuration configuration = new Configuration();

        // Database connection settings
        Properties properties = new Properties();
        properties.put(Environment.JAKARTA_JDBC_URL, dbUrl);
        properties.put(Environment.JAKARTA_JDBC_USER, dbUser);
        properties.put(Environment.JAKARTA_JDBC_PASSWORD, dbPass);
        properties.put(Environment.JAKARTA_JDBC_DRIVER, "org.postgresql.Driver");
        properties.put(Environment.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");

        // HikariCP settings (fast connection check)
        properties.put("hibernate.hikari.minimumIdle", "1");
        properties.put("hibernate.hikari.maximumPoolSize", "5");
        properties.put("hibernate.hikari.idleTimeout", "30000");
        properties.put("hibernate.hikari.connectionTimeout", "2000");
        properties.put("hibernate.hikari.initializationFailTimeout", "1000");

        // Hibernate settings
        properties.put(Environment.SHOW_SQL, "false");
        properties.put(Environment.FORMAT_SQL, "true");
        properties.put(Environment.HBM2DDL_AUTO, "update"); // or "validate" in production
        properties.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        properties.put(Environment.CONNECTION_PROVIDER, "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
        properties.put("hibernate.cache.use_second_level_cache", "false");
        properties.put("hibernate.cache.use_query_cache", "false");

        configuration.setProperties(properties);

        // Add annotated classes
        configuration.addAnnotatedClass(org.ether.society.database.H3Cell.class);
        configuration.addAnnotatedClass(org.ether.society.database.WorldMap.class);

        try {
            EntityManagerFactory emf = configuration.buildSessionFactory().unwrap(EntityManagerFactory.class);
            logger.info("EntityManagerFactory created successfully");
            return emf;
        } catch (Exception e) {
            logger.info("Database offline mode active: PostgreSQL server not available at {} ({})", dbUrl, e.getMessage());
            // Return null instead of throwing, to allow offline mode
            return null;
        }
    }

    /**
     * Close the EntityManagerFactory and datasource.
     */
    public static synchronized void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            logger.info("Closing EntityManagerFactory...");
            entityManagerFactory.close();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing DataSource...");
            dataSource.close();
        }
    }

    /**
     * Checks whether the PostgreSQL database backend is reachable and initialized.
     *
     * @return {@code true} if an active {@link EntityManagerFactory} is available, {@code false} in offline mode
     */
    public static boolean isDatabaseAvailable() {
        try {
            return getEntityManagerFactory() != null;
        } catch (Exception e) {
            logger.warn("Database not available: {}", e.getMessage());
            return false;
        }
    }
}

