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
        Path baseSaveDir = Paths.get(SAVE_DIR).toAbsolutePath().normalize();
        Path savePath = baseSaveDir.resolve(saveId).normalize();

        if (!savePath.startsWith(baseSaveDir)) {
            throw new IllegalArgumentException("Security Exception: Invalid save path");
        }

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
            logger.info("World loaded: {} cells.", cells.size());

        } catch (Exception e) {
            logger.error("Failed to load game", e);
            throw new RuntimeException("Load failed", e);
        }
    }

    /**
     * Saves a 60-tick lightweight intermediate checkpoint to disk with a bounded rolling window.
     */
    public void saveCheckpoint(H3SimulationEngine engine, int tickCounter) {
        if (engine == null || engine.getCells() == null || engine.getCells().isEmpty()) return;
        try {
            Path dir = Paths.get("saves/checkpoints");
            Files.createDirectories(dir);

            SaveMetadata metadata = new SaveMetadata(
                "checkpoint_" + tickCounter,
                "AutoCheckPoint_Tick_" + tickCounter,
                engine.getTimeManager().getCurrentYear(),
                engine.getTimeManager().getCurrentMonth(),
                engine.getCurrentScenario() != null ? engine.getCurrentScenario().getName() : "AutoSave"
            );

            // 1. Overwrite latest autosave for instant application resume
            File latestFile = dir.resolve("checkpoint_latest.json").toFile();
            objectMapper.writeValue(latestFile, metadata);

            // 2. Manage rolling checkpoint window (max 5 checkpoint files on disk)
            try (Stream<Path> stream = Files.list(dir)) {
                List<Path> files = stream.filter(p -> p.getFileName().toString().startsWith("checkpoint_tick_"))
                                         .sorted(java.util.Comparator.comparingLong(p -> p.toFile().lastModified()))
                                         .collect(Collectors.toList());
                while (files.size() >= 5) {
                    Path old = files.remove(0);
                    Files.deleteIfExists(old);
                }
            }

            File tickFile = dir.resolve("checkpoint_tick_" + tickCounter + ".json").toFile();
            objectMapper.writeValue(tickFile, metadata);

        } catch (Exception ex) {
            logger.error("Failed to save 60-tick checkpoint", ex);
        }
    }

    /**
     * Lists all available saved game snapshots across normal saves and auto-checkpoints.
     */
    public List<SaveMetadata> listSaves() {
        List<SaveMetadata> list = new java.util.ArrayList<>();
        Path baseSaveDir = Paths.get(SAVE_DIR);
        if (!Files.exists(baseSaveDir)) return list;

        try (Stream<Path> stream = Files.walk(baseSaveDir, 3)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                  .forEach(jsonFile -> {
                      try {
                          SaveMetadata meta = objectMapper.readValue(jsonFile.toFile(), SaveMetadata.class);
                          if (meta != null && meta.getId() != null) {
                              // Avoid duplicate entries if checkpoint_latest points to same tick
                              boolean exists = list.stream().anyMatch(existing -> existing.getId().equals(meta.getId()));
                              if (!exists) {
                                  list.add(meta);
                              }
                          }
                      } catch (Exception ignored) {}
                  });
        } catch (IOException e) {
            logger.error("Failed to list saves", e);
        }

        // Sort newest first
        list.sort((a, b) -> {
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });

        return list;
    }
}

