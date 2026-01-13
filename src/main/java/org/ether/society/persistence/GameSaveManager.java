package org.ether.society.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellRepository;
import org.ether.society.database.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages game persistence (Save/Load).
 * Orchestrates saving Metadata (JSON) and World State (Database).
 */
public class GameSaveManager {
    private static final Logger logger = LoggerFactory.getLogger(GameSaveManager.class);
    private static final String SAVE_DIR = "saves";
    private static final String METADATA_FILE = "metadata.json";

    private final H3CellRepository cellRepository;
    private final ObjectMapper objectMapper;

    public GameSaveManager() {
        this.cellRepository = new H3CellRepository(DatabaseConfig.getEntityManagerFactory());
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Ensure save directory exists
        try {
            Files.createDirectories(Paths.get(SAVE_DIR));
        } catch (IOException e) {
            logger.error("Failed to create save directory", e);
        }
    }

    /**
     * Saves the current simulation state.
     * 
     * @param engine The simulation engine to save
     * @param saveName User-friendly name for the save
     */
    public void saveGame(H3SimulationEngine engine, String saveName) {
        String saveId = UUID.randomUUID().toString();
        Path savePath = Paths.get(SAVE_DIR, saveId);

        try {
            Files.createDirectories(savePath);

            // 1. Save Metadata (JSON)
            SaveMetadata metadata = new SaveMetadata(
                saveId,
                saveName,
                engine.getTimeManager().getCurrentYear(),
                engine.getTimeManager().getCurrentMonth(),
                engine.getCurrentScenario() != null ? engine.getCurrentScenario().getName() : "Unknown"
            );
            
            objectMapper.writeValue(savePath.resolve(METADATA_FILE).toFile(), metadata);

            // 2. Save Scenario Configuration (JSON)
            if (engine.getCurrentScenario() != null) {
                objectMapper.writeValue(savePath.resolve("scenario.json").toFile(), engine.getCurrentScenario());
            }

            // 3. Save World State (Database)
            // In a real DB setup, we might use a separate schema or tag rows with saveId.
            // For this embedded single-state implementation, we'll overwrite the DB content 
            // or we need a way to snapshot. 
            // APPROACH: The H3CellRepository currently manages the "Active" world.
            // To "Save", we commit the in-memory cells to the persistent store.
            
            // Note: H3Cell currently doesn't have a 'saveId'.
            // For MVP, "Save" implies flushing current state to disk.
            // If we want multiple slots, we'd need to export DB or add saveId to H3Cell.
            // Let's implement MVP: Save = Persist current state to DB.
            
            logger.info("Persisting {} cells to database...", engine.getCells().size());
            // Clear existing for now (Single slot mode effectively, or overwrite)
            cellRepository.deleteAll(); 
            cellRepository.saveAll(engine.getCells());
            
            logger.info("Game saved successfully: {} ({})", saveName, saveId);

        } catch (IOException e) {
            logger.error("Failed to save game", e);
            throw new RuntimeException("Save failed", e);
        }
    }

    /**
     * Loads a game state.
     * 
     * @param saveId The ID of the save to load (not used in single-db MVP but reserved)
     * @param engine The engine to populate
     */
    public void loadGame(String saveId, H3SimulationEngine engine) {
        try {
            // For MVP single-DB: just load from DB.
            // Future: Load specific save snapshot.
            
            logger.info("Loading world from database...");
            List<H3Cell> cells = cellRepository.findAll();
            
            if (cells.isEmpty()) {
                logger.warn("No saved world found in database.");
                return;
            }

            engine.setCells(cells);
            
            // In real multi-save, we'd load metadata to restore TimeManager too.
            // Assuming single active DB for now.
            logger.info("World loaded: {} cells.", cells.size());

        } catch (Exception e) {
            logger.error("Failed to load game", e);
            throw new RuntimeException("Load failed", e);
        }
    }
}
