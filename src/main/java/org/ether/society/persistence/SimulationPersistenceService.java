package org.ether.society.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.DatabaseConfig;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellRepository;
import org.ether.society.network.codec.CellTopologyWireCodec;
import org.ether.society.network.codec.WorldBufferWireCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Unified High-Performance Simulation Persistence &amp; Replay Manager.
 * Orchestrates saving and loading of:
 * 1. Topology (topology.bin.gz) â€” Static/cold spatial baselines via CellTopologyWireCodec
 * 2. World State (state.bin.gz &amp; snapshots/) â€” Dynamic simulation arrays via WorldBufferWireCodec
 * 3. Metadata (metadata.json) â€” Scenario identification, timeline, and tick indexes
 * 4. Telemetry (history.json) â€” Time series analytics and historical snapshots
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SimulationPersistenceService {
    private static final Logger logger = LoggerFactory.getLogger(SimulationPersistenceService.class);
    /** Internal state: metadata file (String). */
    private static final String METADATA_FILE = "metadata.json";
    /** Internal state: scenario file (String). */
    private static final String SCENARIO_FILE = "scenario.json";
    /** Internal state: topology file (String). */
    private static final String TOPOLOGY_FILE = "topology.bin.gz";
    /** Internal state: state file (String). */
    private static final String STATE_FILE = "state.bin.gz";
    /** Internal state: history file (String). */
    private static final String HISTORY_FILE = "history.json";
    private static final String CHRONICLES_JSON_FILE = "chronicles.json";
    private static final String CHRONICLES_CSV_FILE = "chronicles.csv";
    /** Internal state: snapshots dir (String). */
    private static final String SNAPSHOTS_DIR = "snapshots";

    private final H3CellRepository cellRepository;
    private final ObjectMapper objectMapper;
    private final Path customSaveDir;

    /**
     * Get save directory.
     *
     * @return the resulting computation or state reference
     */
    public Path getSaveDirectory() {
        return customSaveDir != null ? customSaveDir : org.ether.society.config.EtherPaths.getSavesDir();
    }

    public static Path getDefaultSaveDirectory() {
        return org.ether.society.config.EtherPaths.getSavesDir();
    }

    /**
     * Simulation save manager.
     */
    public SimulationPersistenceService() {
        this(null);
    }

    public SimulationPersistenceService(Path customSaveDir) {
        this.customSaveDir = customSaveDir;
        this.cellRepository = new H3CellRepository(DatabaseConfig.getEntityManagerFactory());
        this.objectMapper = EtherJsonHelper.getPrettyMapper();

        try {
            Files.createDirectories(getSaveDirectory());
        } catch (IOException e) {
            logger.error("Failed to create save directory", e);
        }
    }

    /**
     * Saves full simulation state and initial topology to disk with an auto-generated UUID.
     */
    public void saveSimulation(H3SimulationEngine engine, String saveName) {
        saveSimulation(engine, UUID.randomUUID().toString(), saveName);
    }

    /**
     * Saves full simulation state, topology, and all intermediate snapshots to disk with a specified save ID.
     */
    public void saveSimulation(H3SimulationEngine engine, String saveId, String saveName) {
        if (engine == null) {
            logger.warn("Cannot save simulation: engine is null.");
            return;
        }

        List<H3Cell> cells = engine.getCells();
        if (cells == null || cells.isEmpty()) {
            logger.warn("Cannot save simulation with empty cells.");
            return;
        }

        if (saveId == null || saveId.isBlank()) {
            saveId = UUID.randomUUID().toString();
        }
        Path baseSaveDir = getSaveDirectory();
        Path savePath = baseSaveDir.resolve(saveId).normalize();

        if (!savePath.startsWith(baseSaveDir)) {
            throw new IllegalArgumentException("Security Exception: Invalid save path");
        }

        try {
            Files.createDirectories(savePath);
            Path snapshotsDir = savePath.resolve(SNAPSHOTS_DIR);
            Files.createDirectories(snapshotsDir);

            // 1. Save Static Spatial Topology (topology.bin.gz)
            Path topologyPath = savePath.resolve(TOPOLOGY_FILE);
            CellTopologyWireCodec.saveToFile(topologyPath, cells);

            // 2. Save Active WorldBuffer State (state.bin.gz)
            WorldBuffer worldBuffer = engine.getWorldBuffer();
            if (worldBuffer == null || worldBuffer.getCapacity() == 0) {
                worldBuffer = new WorldBuffer(cells.size());
                org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, worldBuffer);
            }

            long currentTick = engine.getTickCounter();
            byte[] stateBytes = WorldBufferWireCodec.encodeChunk(worldBuffer, 0, worldBuffer.getCapacity(), currentTick);
            Path statePath = savePath.resolve(STATE_FILE);
            SimulationSaveIOHelper.writeGzipBytes(statePath, stateBytes);

            // Also copy initial/current state as a discrete replay snapshot
            String currentSnapFileName = String.format("snapshot_tick_%010d.bin.gz", currentTick);
            Path currentSnapPath = snapshotsDir.resolve(currentSnapFileName);
            Files.copy(statePath, currentSnapPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Persist all recorded world replay snapshots if present
            if (engine.getHistoryManager() != null && !engine.getHistoryManager().getWorldSnapshots().isEmpty()) {
                for (Map.Entry<Long, List<H3Cell>> entry : engine.getHistoryManager().getWorldSnapshots().entrySet()) {
                    long snapTick = entry.getKey();
                    String snapFileName = String.format("snapshot_tick_%010d.bin.gz", snapTick);
                    Path snapPath = snapshotsDir.resolve(snapFileName);
                    if (!Files.exists(snapPath)) {
                        List<H3Cell> snapCells = entry.getValue();
                        WorldBuffer snapBuffer = new WorldBuffer(snapCells.size());
                        org.ether.society.data.DODDataGenerator.populateWorldBuffer(snapCells, snapBuffer);
                        byte[] snapBytes = WorldBufferWireCodec.encodeChunk(snapBuffer, 0, snapBuffer.getCapacity(), snapTick);
                        SimulationSaveIOHelper.writeGzipBytes(snapPath, snapBytes);
                    }
                }
            }

            // 3. Save Scenario Configuration (scenario.json) atomically
            if (engine.getCurrentScenario() != null) {
                EtherJsonHelper.writePrettyAtomic(savePath.resolve(SCENARIO_FILE), engine.getCurrentScenario());
            }

            // 4. Save Historical Telemetry & Analytics (history.json) atomically
            if (engine.getHistoryManager() != null && engine.getHistoryManager().getHistory() != null) {
                EtherJsonHelper.writePrettyAtomic(savePath.resolve(HISTORY_FILE), engine.getHistoryManager().getHistory().getSnapshots());
            }

            // 4.b Save Chronicle History & Timeline (chronicles.json and chronicles.csv)
            if (engine.getEventSystem() != null) {
                List<org.ether.society.events.ActiveEvent> chronicleEvents = engine.getEventSystem().getChronicleHistory();
                if (chronicleEvents != null && !chronicleEvents.isEmpty()) {
                    EtherJsonHelper.writePrettyAtomic(savePath.resolve(CHRONICLES_JSON_FILE), chronicleEvents);
                    Path csvPath = savePath.resolve(CHRONICLES_CSV_FILE);
                    StringBuilder csv = new StringBuilder();
                    csv.append("Year,Month,Day,Type,Magnitude,Latitude,Longitude,DurationDays,Title\n");
                    for (org.ether.society.events.ActiveEvent evt : chronicleEvents) {
                        String cleanTitle = evt.getTitle() != null ? evt.getTitle().replace("\"", "\"\"") : "";
                        csv.append(String.format(java.util.Locale.ROOT, "%d,%d,%d,%s,%.2f,%.4f,%.4f,%d,\"%s\"\n",
                                evt.getYear(), evt.getMonth(), evt.getDay(),
                                evt.getType() != null ? evt.getType() : "EVENT",
                                evt.getMagnitude(), evt.getLatitude(), evt.getLongitude(),
                                evt.getDurationDays(), cleanTitle));
                    }
                    Files.writeString(csvPath, csv.toString(), java.nio.charset.StandardCharsets.UTF_8);
                }
            }

            // 5. Save Metadata (metadata.json) atomically
            int h3Res = engine.getCurrentScenario() != null ? engine.getCurrentScenario().getH3Resolution() : 3;
            SaveMetadata metadata = new SaveMetadata(
                    saveId,
                    saveName,
                    engine.getTimeManager().getCurrentYear(),
                    engine.getTimeManager().getCurrentMonth(),
                    engine.getCurrentScenario() != null ? engine.getCurrentScenario().getName() : "Unknown",
                    cells.size(),
                    h3Res
            );
            EtherJsonHelper.writePrettyAtomic(savePath.resolve(METADATA_FILE), metadata);

            // 6. Optional Database persistence
            if (DatabaseConfig.isDatabaseAvailable()) {
                logger.info("Persisting {} cells to database...", cells.size());
                cellRepository.saveAll(cells);
            }

            logger.info("âœ… Simulation saved successfully: '{}' [{}] ({} cells, Tick {})",
                    saveName, saveId, cells.size(), currentTick);

        } catch (Exception e) {
            logger.error("Failed to save simulation", e);
            throw new RuntimeException("Save failed: " + e.getMessage(), e);
        }
    }

    /**
     * Loads simulation state and topology from disk into the target engine.
     */
    public void loadSimulation(String saveId, H3SimulationEngine engine) {
        try {
            Path savePath;
            if (saveId == null || saveId.isBlank()) {
                List<SaveMetadata> saves = listSaves();
                if (saves.isEmpty()) {
                    logger.warn("No existing saves available to load.");
                    return;
                }
                savePath = getSaveDirectory().resolve(saves.get(0).getId()).normalize();
            } else {
                savePath = getSaveDirectory().resolve(saveId).normalize();
            }

            if (!Files.exists(savePath)) {
                // Check if saveId is an absolute or relative directory path directly
                Path directPath = Paths.get(saveId);
                if (Files.exists(directPath)) {
                    savePath = directPath;
                } else {
                    // Fallback: search save directories where directory name or metadata id matches
                    Path resolvedFallback = null;
                    try (Stream<Path> s = Files.list(getSaveDirectory())) {
                        var found = s.filter(Files::isDirectory).filter(d -> {
                            if (d.getFileName().toString().equalsIgnoreCase(saveId)) return true;
                            Path m = d.resolve(METADATA_FILE);
                            if (Files.exists(m)) {
                                try {
                                    SaveMetadata sm = objectMapper.readValue(m.toFile(), SaveMetadata.class);
                                    return sm != null && saveId.equalsIgnoreCase(sm.getId());
                                } catch (Exception ignored) { org.slf4j.LoggerFactory.getLogger(SimulationPersistenceService.class).debug("Non-fatal condition in SimulationPersistenceService: {}", ignored.getMessage()); }
                            }
                            return false;
                        }).findFirst();
                        if (found.isPresent()) {
                            resolvedFallback = found.get();
                        }
                    } catch (IOException ignored) { org.slf4j.LoggerFactory.getLogger(SimulationPersistenceService.class).debug("Non-fatal condition in SimulationPersistenceService: {}", ignored.getMessage()); }
                    if (resolvedFallback != null) {
                        savePath = resolvedFallback;
                    } else {
                        throw new FileNotFoundException("Save directory not found: " + savePath);
                    }
                }
            }

            logger.info("Loading unified simulation save from: {}", savePath);

            // 1. Restore Metadata, Scenario & Time
            File scenarioFile = savePath.resolve(SCENARIO_FILE).toFile();
            if (scenarioFile.exists()) {
                try {
                    org.ether.society.model.Scenario loadedScenario = objectMapper.readValue(scenarioFile, org.ether.society.model.Scenario.class);
                    if (loadedScenario != null) {
                        engine.setCurrentScenario(loadedScenario);
                    }
                } catch (Exception ex) {
                    logger.warn("Could not deserialize scenario.json from save: {}", ex.getMessage());
                }
            }

            File metaFile = savePath.resolve(METADATA_FILE).toFile();
            if (metaFile.exists()) {
                SaveMetadata metadata = objectMapper.readValue(metaFile, SaveMetadata.class);
                if (metadata != null && engine.getTimeManager() != null) {
                    long totalTicks = 0;
                    if (engine.getCurrentScenario() != null) {
                        long startYear = engine.getCurrentScenario().getStartDateYear();
                        double dtDays = engine.getCurrentScenario().getTemporalResolutionDays() > 0 ? engine.getCurrentScenario().getTemporalResolutionDays() : 30.0;
                        totalTicks = Math.max(0, (long) ((metadata.getYear() - startYear) * (365.25 / dtDays) + metadata.getMonth() * (30.0 / dtDays)));
                    }
                    engine.getTimeManager().setTime((int) metadata.getYear(), metadata.getMonth(), 1, totalTicks);
                }
            }

            // 2. Restore Spatial Topology (topology.bin.gz)
            Path topologyPath = savePath.resolve(TOPOLOGY_FILE);
            List<H3Cell> cells = null;
            if (Files.exists(topologyPath)) {
                cells = CellTopologyWireCodec.loadFromFile(topologyPath);
            }

            // 3. Fallback to database if topology file is missing
            if (cells == null || cells.isEmpty()) {
                logger.warn("No topology.bin.gz found, falling back to database...");
                cells = cellRepository.findAll();
            }

            if (cells.isEmpty()) {
                logger.warn("No saved world found in save directory or database.");
                return;
            }

            // 4. Instantiate & populate WorldBuffer
            engine.setCells(cells);
            WorldBuffer worldBuffer = engine.getWorldBuffer();
            if (worldBuffer != null) {
                org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, worldBuffer);
            }

            // 5. Restore Dynamic State (state.bin.gz or latest snapshot)
            Path statePath = savePath.resolve(STATE_FILE);
            if (!Files.exists(statePath)) {
                // Look for latest snapshot in snapshots/
                Path snapDir = savePath.resolve(SNAPSHOTS_DIR);
                if (Files.exists(snapDir)) {
                    File[] snapFiles = snapDir.toFile().listFiles((d, n) -> n.endsWith(".bin.gz") || n.endsWith(".gz"));
                    if (snapFiles != null && snapFiles.length > 0) {
                        Arrays.sort(snapFiles, Comparator.comparing(File::getName).reversed());
                        statePath = snapFiles[0].toPath();
                    }
                }
            }

            if (Files.exists(statePath)) {
                byte[] rawState = SimulationSaveIOHelper.readGzipBytes(statePath);
                WorldBufferWireCodec.decodeChunkInto(rawState, worldBuffer);
                engine.syncBufferToCells();
                logger.info("Restored dynamic world state ({} cells) from {}", cells.size(), statePath.getFileName());
            }

            // 6. Restore Historical Telemetry (history.json)
            File historyFile = savePath.resolve(HISTORY_FILE).toFile();
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

            logger.info("World successfully loaded: {} cells.", cells.size());

        } catch (Exception e) {
            logger.error("Failed to load simulation", e);
            throw new RuntimeException("Load failed: " + e.getMessage(), e);
        }
    }

    /**
     * Loads a specific tick snapshot from the save's snapshots/ directory into the engine.
     */
    public boolean loadTickSnapshot(Path savePath, long targetTick, H3SimulationEngine engine) {
        return SimulationSaveIOHelper.loadTickSnapshot(savePath, targetTick, engine);
    }

    private static final java.util.concurrent.ExecutorService asyncDbExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ether-async-db-persistence");
        t.setDaemon(true);
        return t;
    });

    /**
     * Saves a 60-tick periodic simulation snapshot into the central database asynchronously without blocking the simulation loop.
     */
    public void saveCheckpoint(H3SimulationEngine engine, int tickCounter) {
        if (engine == null || engine.getCells() == null || engine.getCells().isEmpty()) return;
        if (!DatabaseConfig.isDatabaseAvailable()) {
            return;
        }

        final List<H3Cell> snapshotCells = new ArrayList<>(engine.getCells());
        asyncDbExecutor.submit(() -> {
            try {
                cellRepository.saveAll(snapshotCells);
            } catch (Exception ex) {
                logger.error("Failed to save periodic DB checkpoint asynchronously", ex);
            }
        });
    }

    /**
     * Lists all available saved simulation snapshots across normal saves and auto-checkpoints.
     */
    public List<SaveMetadata> listSaves() {
        return SimulationSaveIOHelper.listSaves(getSaveDirectory(), objectMapper);
    }
}


