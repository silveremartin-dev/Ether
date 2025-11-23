/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author: Silvere Martin-Michiellot (silvere.martin@gmail.com)
 * Contributors: AI Assistant (Antigravity/Claude)
 */
package com.ether.society.config;

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
                throw new IOException("Default configuration file not found");
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
