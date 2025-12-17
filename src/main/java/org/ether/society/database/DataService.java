/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.database;

import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service layer for database operations.
 * Provides high-level business logic for simulation data persistence.
 */
public class DataService {
    private static final Logger logger = LoggerFactory.getLogger(DataService.class);

    private final H3CellRepository cellRepository;
    private final WorldMapRepository mapRepository;
    private final EntityManagerFactory emf;

    public DataService() {
        this.emf = DatabaseConfig.getEntityManagerFactory();
        this.cellRepository = new H3CellRepository(emf);
        this.mapRepository = new WorldMapRepository(emf);
    }

    /**
     * Save a complete simulation state (map + cells).
     */
    public void saveSimulation(String mapName, List<H3Cell> cells) {
        logger.info("Saving simulation '{}' with {} cells...", mapName, cells.size());

        // Save or update map metadata
        WorldMap map = mapRepository.findByName(mapName)
                .orElse(new WorldMap());
        map.setName(mapName);
        map.setTotalCells((long) cells.size());
        mapRepository.save(map);

        // Save cells in batch
        long startTime = System.currentTimeMillis();
        cellRepository.saveAll(cells);
        long duration = System.currentTimeMillis() - startTime;

        logger.info("Simulation saved successfully in {}ms", duration);
    }

    /**
     * Load simulation by map name.
     */
    public List<H3Cell> loadSimulation(String mapName) {
        logger.info("Loading simulation '{}'...", mapName);

        mapRepository.findByName(mapName)
                .orElseThrow(() -> new RuntimeException("Map not found: " + mapName));

        List<H3Cell> cells = cellRepository.findAll();
        logger.info("Loaded {} cells", cells.size());

        return cells;
    }

    /**
     * Get all available map names.
     */
    public List<String> getAvailableMaps() {
        return mapRepository.findAll().stream()
                .map(WorldMap::getName)
                .toList();
    }

    /**
     * Clear all simulation data (use with caution!).
     */
    public void clearDatabase() {
        logger.warn("Clearing all database data...");
        cellRepository.deleteAll();
        logger.info("Database cleared");
    }

    /**
     * Get cell count.
     */
    public long getCellCount() {
        return cellRepository.count();
    }

    /**
     * Check if database is connected and available.
     */
    public boolean isDatabaseAvailable() {
        return DatabaseConfig.isDatabaseAvailable();
    }

    /**
     * Close database connections.
     */
    public void close() {
        DatabaseConfig.close();
    }
}
