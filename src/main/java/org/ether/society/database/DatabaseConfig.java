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

    /**
     * Get or create the EntityManagerFactory.
     */
    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
            entityManagerFactory = createEntityManagerFactory();
        }
        return entityManagerFactory;
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
        properties.put(Environment.DIALECT, "org.hibernate.spatial.dialect.postgis.PostgisDialect");

        // HikariCP settings
        properties.put("hibernate.hikari.minimumIdle", "2");
        properties.put("hibernate.hikari.maximumPoolSize", "10");
        properties.put("hibernate.hikari.idleTimeout", "300000");
        properties.put("hibernate.hikari.connectionTimeout", "20000");

        // Hibernate settings
        properties.put(Environment.SHOW_SQL, "false");
        properties.put(Environment.FORMAT_SQL, "true");
        properties.put(Environment.HBM2DDL_AUTO, "update"); // or "validate" in production
        properties.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        properties.put(Environment.CONNECTION_PROVIDER, "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");

        configuration.setProperties(properties);

        // Add annotated classes
        configuration.addAnnotatedClass(org.ether.society.database.H3Cell.class);
        configuration.addAnnotatedClass(org.ether.society.database.WorldMap.class);

        try {
            EntityManagerFactory emf = configuration.buildSessionFactory().unwrap(EntityManagerFactory.class);
            logger.info("EntityManagerFactory created successfully");
            return emf;
        } catch (Exception e) {
            logger.error("Failed to create EntityManagerFactory", e);
            throw new RuntimeException("Could not create EntityManagerFactory", e);
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
     * Check if database is available.
     */
    public static boolean isDatabaseAvailable() {
        try {
            getEntityManagerFactory();
            return true;
        } catch (Exception e) {
            logger.warn("Database not available: {}", e.getMessage());
            return false;
        }
    }
}

