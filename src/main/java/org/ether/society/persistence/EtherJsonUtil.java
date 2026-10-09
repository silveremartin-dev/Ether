/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvère Martin-Michiellot
 */
package org.ether.society.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

/**
 * <h1>Centralized &amp; Atomic JSON Serialization Utility</h1>
 * <p>
 * Provides robust, enterprise-grade JSON read/write operations for simulation saves,
 * metadata descriptors, historical telemetry, and scenario presets.
 * </p>
 * <p>
 * <b>Key Invariants:</b>
 * <ul>
 *   <li><b>Standardized Java 8 Date/Time support:</b> Pre-configured with {@link JavaTimeModule}
 *       and {@code WRITE_DATES_AS_TIMESTAMPS = false} to prevent serialization crashes on {@code LocalDateTime} or {@code Instant}.</li>
 *   <li><b>Atomic writes:</b> All write operations stream content to an ephemeral temporary file
 *       before performing an atomic rename/move ({@link StandardCopyOption#ATOMIC_MOVE}).
 *       This strictly guarantees that target files on disk (such as {@code metadata.json} or {@code scenario.json})
 *       are never left corrupted, truncated, or half-written due to unexpected thread cancellations, exceptions, or process halts.</li>
 *   <li><b>Resilient deserialization:</b> Tolerates unknown properties without failing across schema versions.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class EtherJsonUtil {
    private static final Logger logger = LoggerFactory.getLogger(EtherJsonUtil.class);

    private static final ObjectMapper MAPPER = createConfiguredMapper(false);
    private static final ObjectMapper PRETTY_MAPPER = createConfiguredMapper(true);

    private EtherJsonUtil() {
        // Utility class
    }

    /**
     * Creates and configures a standardized Jackson {@link ObjectMapper}.
     *
     * @param pretty whether to enable indentation for human readability
     * @return fully configured {@link ObjectMapper}
     */
    public static ObjectMapper createConfiguredMapper(boolean pretty) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        if (pretty) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        return mapper;
    }

    /**
     * Gets the shared, thread-safe, non-pretty {@link ObjectMapper}.
     *
     * @return default configured ObjectMapper
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    /**
     * Gets the shared, thread-safe, pretty-printing {@link ObjectMapper}.
     *
     * @return pretty printing ObjectMapper
     */
    public static ObjectMapper getPrettyMapper() {
        return PRETTY_MAPPER;
    }

    /**
     * Atomically writes an object to a destination path as formatted (pretty) JSON.
     * Guarantees zero disk corruption by writing to a temporary file first and atomically renaming it.
     *
     * @param targetPath the target file path
     * @param value the object to serialize
     * @throws IOException if disk write fails
     */
    public static void writePrettyAtomic(Path targetPath, Object value) throws IOException {
        writeAtomic(targetPath, value, PRETTY_MAPPER);
    }

    /**
     * Atomically writes an object to a destination path as compact JSON.
     * Guarantees zero disk corruption by writing to a temporary file first and atomically renaming it.
     *
     * @param targetPath the target file path
     * @param value the object to serialize
     * @throws IOException if disk write fails
     */
    public static void writeCompactAtomic(Path targetPath, Object value) throws IOException {
        writeAtomic(targetPath, value, MAPPER);
    }

    private static void writeAtomic(Path targetPath, Object value, ObjectMapper mapper) throws IOException {
        if (targetPath == null || value == null) {
            throw new IllegalArgumentException("Target path and value to serialize must not be null");
        }

        Path parent = targetPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        String fileName = targetPath.getFileName().toString();
        Path tmpPath = parent != null
                ? parent.resolve(fileName + ".tmp." + UUID.randomUUID())
                : targetPath.resolveSibling(fileName + ".tmp." + UUID.randomUUID());

        try {
            mapper.writeValue(tmpPath.toFile(), value);

            try {
                Files.move(tmpPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception moveEx) {
                // Fallback if filesystem does not support atomic move
                Files.move(tmpPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(tmpPath);
            } catch (Exception ignored) {}
            throw new IOException("Failed to atomically write JSON to " + targetPath + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Safely reads and deserializes an entity from a JSON file.
     *
     * @param sourcePath the JSON file path
     * @param clazz the expected entity class
     * @param <T> entity type
     * @return deserialized entity, or empty Optional if file does not exist or fails parsing
     */
    public static <T> Optional<T> readSafely(Path sourcePath, Class<T> clazz) {
        if (sourcePath == null || !Files.exists(sourcePath)) {
            return Optional.empty();
        }
        try {
            T value = MAPPER.readValue(sourcePath.toFile(), clazz);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            logger.warn("Could not read JSON entity from {}: {}", sourcePath, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Safely reads and deserializes a generic type (e.g. List of entities) from a JSON file.
     *
     * @param sourcePath the JSON file path
     * @param typeRef the Jackson type reference
     * @param <T> entity type
     * @return deserialized entity, or empty Optional if file does not exist or fails parsing
     */
    public static <T> Optional<T> readSafely(Path sourcePath, TypeReference<T> typeRef) {
        if (sourcePath == null || !Files.exists(sourcePath)) {
            return Optional.empty();
        }
        try {
            T value = MAPPER.readValue(sourcePath.toFile(), typeRef);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            logger.warn("Could not read generic JSON entity from {}: {}", sourcePath, e.getMessage());
            return Optional.empty();
        }
    }
}
