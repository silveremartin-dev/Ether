/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads configuration from JSON files using Jackson.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 2.0.0
 */
public class ConfigurationLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationLoader.class);
    private static final ObjectMapper objectMapper = createObjectMapper();

    /**
     * Creates and configures the Jackson ObjectMapper.
     *
     * @return Configured ObjectMapper instance
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    /**
     * Loads configuration from the default resource file.
     *
     * @return Configuration object
     * @throws IOException if loading fails
     */
    public static Configuration loadDefault() throws IOException {
        logger.info("Loading default configuration from classpath");
        try (InputStream is = ConfigurationLoader.class.getResourceAsStream(
                "/config/default-config.json")) {
            if (is == null) {
                // Return default if file not found (fallback)
                // throw new IOException("Default configuration file not found");
                logger.warn("Default config not found, creating empty config");
                return new Configuration(
                        new Configuration.WorldConfig(100, 100, 10, new Configuration.GenerationParams(4, 1.0, 4, 1.0)),
                        new Configuration.SimulationConfig(1000, 42, new int[] { 1, 2, 5 }),
                        new Configuration.AgentsConfig(100, java.util.Collections.emptyMap()),
                        new Configuration.ClimateConfig(0.5, 0.5, 0.5),
                        new Configuration.ResourcesConfig(0.1, 100.0, 0.5));
            }
            Configuration config = objectMapper.readValue(is, Configuration.class);
            logger.info("Configuration loaded: World {}x{}", config.world().width(), config.world().height());
            return config;
        }
    }

    /**
     * Loads configuration from a file path.
     *
     * @param path Path to the configuration file
     * @return Configuration object
     * @throws IOException if loading fails
     */
    public static Configuration load(Path path) throws IOException {
        logger.info("Loading configuration from: {}", path);
        Configuration config = objectMapper.readValue(Files.newInputStream(path), Configuration.class);
        logger.info("Configuration loaded successfully");
        return config;
    }

    /**
     * Saves configuration to a file path.
     *
     * @param config Configuration to save
     * @param path   Destination path
     * @throws IOException if saving fails
     */
    public static void save(Configuration config, Path path) throws IOException {
        logger.info("Saving configuration to: {}", path);
        objectMapper.writeValue(Files.newOutputStream(path), config);
        logger.info("Configuration saved successfully");
    }
}
