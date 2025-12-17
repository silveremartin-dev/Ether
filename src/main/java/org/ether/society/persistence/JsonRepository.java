/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base repository for JSON file persistence.
 *
 * @param <T> The type of entity to persist.
 */
public abstract class JsonRepository<T> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ObjectMapper mapper;
    private final Path filePath;
    private final Class<T> types;

    public JsonRepository(String filename, Class<T> type) {
        this.types = type;
        // Store data in a user directory or relative to app
        String appData = System.getProperty("user.home") + File.separator + ".ether_society" + File.separator + "data";
        this.filePath = Paths.get(appData, filename);

        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);

        initStorage();
    }

    private void initStorage() {
        try {
            if (!Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                Files.writeString(filePath, "[]"); // Initialize with empty array
            }
        } catch (IOException e) {
            logger.error("Failed to initialize storage: {}", filePath, e);
        }
    }

    public List<T> findAll() {
        try {
            if (Files.size(filePath) == 0)
                return new ArrayList<>();

            // Read as list
            return mapper.readValue(filePath.toFile(),
                    mapper.getTypeFactory().constructCollectionType(List.class, types));
        } catch (IOException e) {
            logger.error("Failed to read entities from {}", filePath, e);
            return new ArrayList<>();
        }
    }

    public void save(T entity) {
        List<T> all = findAll();
        // Determine if update or insert?
        // For simplicity, we'll append or replace if we can identify.
        // But for generic T without ID interface, simple storage usually implies
        // overwriting logic handled by caller
        // or just add new.
        all.add(entity);
        saveAll(all);
    }

    public void saveAll(List<T> entities) {
        try {
            mapper.writeValue(filePath.toFile(), entities);
        } catch (IOException e) {
            logger.error("Failed to save entities to {}", filePath, e);
        }
    }

    public void delete(T entity) {
        List<T> all = findAll();
        all.remove(entity); // Relies on equals()
        saveAll(all);
    }
}
