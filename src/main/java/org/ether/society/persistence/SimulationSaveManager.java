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
 * Manages simulation persistence (Save/Load).
 * Orchestrates saving Metadata (JSON) and World State (Database).
 */
public class SimulationSaveManager {
    private static final Logger logger = LoggerFactory.getLogger(SimulationSaveManager.class);
    private static final String SAVE_DIR = "saves";
    private static final String METADATA_FILE = "metadata.json";

    private final H3CellRepository cellRepository;
    private final ObjectMapper objectMapper;

    public SimulationSaveManager() {
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
    public void saveSimulation(H3SimulationEngine engine, String saveName) {
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

            // 3. Save World State (Self-contained JSON snapshot & DB)
            File cellsFile = savePath.resolve("cells.json").toFile();
            objectMapper.writeValue(cellsFile, engine.getCells());

            // 4. Save Historical Telemetry & Analytics (JSON)
            if (engine.getHistoryManager() != null && engine.getHistoryManager().getHistory() != null) {
                File historyFile = savePath.resolve("history.json").toFile();
                objectMapper.writeValue(historyFile, engine.getHistoryManager().getHistory().getSnapshots());
            }

            if (DatabaseConfig.isDatabaseAvailable()) {
                logger.info("Persisting {} cells to database...", engine.getCells().size());
                cellRepository.saveAll(engine.getCells());
            }
            
            logger.info("Simulation saved successfully: {} ({})", saveName, saveId);

        } catch (Exception e) {
            logger.error("Failed to save simulation", e);
            throw new RuntimeException("Save failed", e);
        }
    }

    /**
     * Loads a simulation state.
     * 
     * @param saveId The ID of the save to load
     * @param engine The engine to populate
     */
    public void loadSimulation(String saveId, H3SimulationEngine engine) {
        try {
            Path savePath = Paths.get(SAVE_DIR).resolve(saveId).normalize();
            
            // Restore Metadata & Time
            File metaFile = savePath.resolve(METADATA_FILE).toFile();
            if (metaFile.exists()) {
                SaveMetadata metadata = objectMapper.readValue(metaFile, SaveMetadata.class);
                if (metadata != null && engine.getTimeManager() != null) {
                    long totalTicks = 0;
                    if (engine.getCurrentScenario() != null) {
                        long startYear = engine.getCurrentScenario().getStartDateYear();
                        totalTicks = Math.max(0, (metadata.getYear() - startYear) * 12 + metadata.getMonth());
                    }
                    engine.getTimeManager().setTime((int) metadata.getYear(), metadata.getMonth(), 1, totalTicks);
                }
            }

            // Restore Historical Telemetry
            File historyFile = savePath.resolve("history.json").toFile();
            if (historyFile.exists() && engine.getHistoryManager() != null) {
                try {
                    List<org.ether.society.analytics.HistorySnapshot> loadedSnapshots = objectMapper.readValue(historyFile,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, org.ether.society.analytics.HistorySnapshot.class));
                    if (loadedSnapshots != null) {
                        engine.getHistoryManager().getHistory().clear();
                        for (org.ether.society.analytics.HistorySnapshot snap : loadedSnapshots) {
                            engine.getHistoryManager().getHistory().addSnapshot(snap);
                        }
                    }
                } catch (Exception ex) {
                    logger.warn("Failed to deserialize history.json from save", ex);
                }
            }

            File cellsFile = savePath.resolve("cells.json").toFile();

            if (cellsFile.exists()) {
                logger.info("Loading cells from save snapshot: {}", cellsFile.getAbsolutePath());
                byte[] rawBytes = Files.readAllBytes(cellsFile.toPath());
                byte[] jsonBytes = org.ether.society.security.SaveEncryptionVault.isEncrypted(rawBytes)
                        ? org.ether.society.security.SaveEncryptionVault.decrypt(rawBytes)
                        : rawBytes;
                List<H3Cell> cells = objectMapper.readValue(jsonBytes, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, H3Cell.class));
                if (cells != null && !cells.isEmpty()) {
                    engine.setCells(cells);
                    if (engine.getWorldBuffer() != null) {
                        org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, engine.getWorldBuffer());
                    }
                    logger.info("World loaded from snapshot: {} cells.", cells.size());
                    return;
                }
            }

            // Fallback to database
            logger.info("Loading world from database...");
            List<H3Cell> cells = cellRepository.findAll();
            
            if (cells.isEmpty()) {
                logger.warn("No saved world found in database or snapshot.");
                return;
            }

            engine.setCells(cells);
            if (engine.getWorldBuffer() != null) {
                org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, engine.getWorldBuffer());
            }
            logger.info("World loaded from database: {} cells.", cells.size());

        } catch (Exception e) {
            logger.error("Failed to load simulation", e);
            throw new RuntimeException("Load failed", e);
        }
    }

    /**
     * Saves a 60-tick periodic simulation snapshot into the central database.
     */
    public void saveCheckpoint(H3SimulationEngine engine, int tickCounter) {
        if (engine == null || engine.getCells() == null || engine.getCells().isEmpty()) return;
        try {
            if (DatabaseConfig.isDatabaseAvailable()) {
                logger.info("Persisting 60-tick snapshot (tick {}) to database...", tickCounter);
                cellRepository.saveAll(engine.getCells());
            } else {
                logger.debug("Database offline mode: 60-tick snapshot retained in HistoryManager memory.");
            }
        } catch (Exception ex) {
            logger.error("Failed to save periodic DB checkpoint", ex);
        }
    }

    /**
     * Lists all available saved simulation snapshots across normal saves and auto-checkpoints.
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
