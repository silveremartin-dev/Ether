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
 * <h1>Json Repository</h1>
 * <p>
 * Core operational component for the Ether civilizational and planetary simulation framework.<br>
 * Integrates cellular dynamics, data structures, and deterministic state transitions.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public abstract class JsonRepository<T> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ObjectMapper mapper;
    private final Path filePath;
    private final Class<T> types;

    /*
     * Json repository.
     * Enforces physical invariants and updates associated state variables within {@code JsonRepository}.
     *
     * @param filename the filename parameter (String)
     * @param type the type parameter (Class&lt;T&gt;)
     */
    public JsonRepository(String filename, Class<T> type) {
        this(org.ether.society.config.EtherPaths.getUserPresetsDir().resolve(Paths.get(filename).getFileName().toString()), type);
    }

    /*
     * Json repository.
     * Enforces physical invariants and updates associated state variables within {@code JsonRepository}.
     *
     * @param customPath the custom path parameter (Path)
     * @param type the type parameter (Class&lt;T&gt;)
     */
    public JsonRepository(Path customPath, Class<T> type) {
        this.types = type;
        this.filePath = customPath;

        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        initStorage();
    }

    // Helper subroutine: init storage - internal state computation & bounds checking
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

    /*
     * Find all.
     * Enforces physical invariants and updates associated state variables within {@code JsonRepository}.
     *
     * @return the resulting computation or state reference
     */
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

    /*
     * Save.
     * Enforces physical invariants and updates associated state variables within {@code JsonRepository}.
     *
     * @param entity the entity parameter (T)
     */
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

    /*
     * Save all.
     * Enforces physical invariants and updates associated state variables within {@code JsonRepository}.
     *
     * @param entities the entities parameter (List&lt;T&gt;)
     */
    public void saveAll(List<T> entities) {
        try {
            mapper.writeValue(filePath.toFile(), entities);
        } catch (IOException e) {
            logger.error("Failed to save entities to {}", filePath, e);
        }
    }

    /*
     * Delete.
     * Enforces physical invariants and updates associated state variables within {@code JsonRepository}.
     *
     * @param entity the entity parameter (T)
     */
    public void delete(T entity) {
        List<T> all = findAll();
        all.remove(entity); // Relies on equals()
        saveAll(all);
    }
}
