package org.ether.society.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellRepository;
import org.ether.society.database.DatabaseConfig;
import org.ether.society.network.codec.CellTopologyWireCodec;
import org.ether.society.network.codec.WorldBufferWireCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Unified High-Performance Simulation Persistence & Replay Manager.
 * Orchestrates saving and loading of:
 * 1. Topology (topology.bin.gz) — Static/cold spatial baselines via CellTopologyWireCodec
 * 2. World State (state.bin.gz & snapshots/) — Dynamic simulation arrays via WorldBufferWireCodec
 * 3. Metadata (metadata.json) — Scenario identification, timeline, and tick indexes
 * 4. Telemetry (history.json) — Time series analytics and historical snapshots
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SimulationSaveManager {
    private static final Logger logger = LoggerFactory.getLogger(SimulationSaveManager.class);
    /* Internal state variable for metadata file (String). */
    private static final String METADATA_FILE = "metadata.json";
    /* Internal state variable for scenario file (String). */
    private static final String SCENARIO_FILE = "scenario.json";
    /* Internal state variable for topology file (String). */
    private static final String TOPOLOGY_FILE = "topology.bin.gz";
    /* Internal state variable for state file (String). */
    private static final String STATE_FILE = "state.bin.gz";
    /* Internal state variable for history file (String). */
    private static final String HISTORY_FILE = "history.json";
    /* Internal state variable for snapshots dir (String). */
    private static final String SNAPSHOTS_DIR = "snapshots";

    private final H3CellRepository cellRepository;
    private final ObjectMapper objectMapper;

    /*
     * Get save directory.
     * Enforces physical invariants and updates associated state variables within {@code SimulationSaveManager}.
     *
     * @return the resulting computation or state reference
     */
    public static Path getSaveDirectory() {
        return org.ether.society.config.EtherPaths.getSavesDir();
    }

    /*
     * Simulation save manager.
     * Enforces physical invariants and updates associated state variables within {@code SimulationSaveManager}.
     *
     */
    public SimulationSaveManager() {
        this.cellRepository = new H3CellRepository(DatabaseConfig.getEntityManagerFactory());
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        try {
            Files.createDirectories(getSaveDirectory());
        } catch (IOException e) {
            logger.error("Failed to create save directory", e);
        }
    }

    /*
     * Saves full simulation state and initial topology to disk with an auto-generated UUID.
     */
    public void saveSimulation(H3SimulationEngine engine, String saveName) {
        saveSimulation(engine, UUID.randomUUID().toString(), saveName);
    }

    /*
     * Saves full simulation state, topology, and all intermediate snapshots to disk with a specified save ID.
     */
    public void saveSimulation(H3SimulationEngine engine, String saveId, String saveName) {
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

            List<H3Cell> cells = engine.getCells();
            if (cells == null || cells.isEmpty()) {
                logger.warn("Cannot save simulation with empty cells.");
                return;
            }

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
            try (OutputStream fos = Files.newOutputStream(statePath);
                 GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
                gzos.write(stateBytes);
                gzos.finish();
            }

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
                        try (OutputStream fos = Files.newOutputStream(snapPath);
                             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
                            gzos.write(snapBytes);
                            gzos.finish();
                        }
                    }
                }
            }

            // 3. Save Scenario Configuration (scenario.json)
            if (engine.getCurrentScenario() != null) {
                objectMapper.writeValue(savePath.resolve(SCENARIO_FILE).toFile(), engine.getCurrentScenario());
            }

            // 4. Save Historical Telemetry & Analytics (history.json)
            if (engine.getHistoryManager() != null && engine.getHistoryManager().getHistory() != null) {
                File historyFile = savePath.resolve(HISTORY_FILE).toFile();
                objectMapper.writeValue(historyFile, engine.getHistoryManager().getHistory().getSnapshots());
            }

            // 5. Save Metadata (metadata.json)
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
            objectMapper.writeValue(savePath.resolve(METADATA_FILE).toFile(), metadata);

            // 6. Optional Database persistence
            if (DatabaseConfig.isDatabaseAvailable()) {
                logger.info("Persisting {} cells to database...", cells.size());
                cellRepository.saveAll(cells);
            }

            logger.info("✅ Simulation saved successfully: '{}' [{}] ({} cells, Tick {})",
                    saveName, saveId, cells.size(), currentTick);

        } catch (Exception e) {
            logger.error("Failed to save simulation", e);
            throw new RuntimeException("Save failed: " + e.getMessage(), e);
        }
    }

    /*
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
                    throw new FileNotFoundException("Save directory not found: " + savePath);
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
                try (InputStream fis = Files.newInputStream(statePath);
                     GZIPInputStream gzis = new GZIPInputStream(fis);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = gzis.read(buf)) != -1) {
                        baos.write(buf, 0, r);
                    }
                    byte[] rawState = baos.toByteArray();
                    WorldBufferWireCodec.decodeChunkInto(rawState, worldBuffer);
                    engine.syncBufferToCells();
                    logger.info("Restored dynamic world state ({} cells) from {}", cells.size(), statePath.getFileName());
                }
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

    /*
     * Loads a specific tick snapshot from the save's snapshots/ directory into the engine.
     */
    public boolean loadTickSnapshot(Path savePath, long targetTick, H3SimulationEngine engine) {
        try {
            Path snapDir = savePath.resolve(SNAPSHOTS_DIR);
            String snapFileName = String.format("snapshot_tick_%010d.bin.gz", targetTick);
            Path snapPath = snapDir.resolve(snapFileName);

            if (!Files.exists(snapPath)) {
                // Fallback to searching matching tick in filename
                File[] files = snapDir.toFile().listFiles((d, n) -> n.contains(String.valueOf(targetTick)) && (n.endsWith(".bin.gz") || n.endsWith(".gz")));
                if (files != null && files.length > 0) {
                    snapPath = files[0].toPath();
                } else {
                    return false;
                }
            }

            try (InputStream fis = Files.newInputStream(snapPath);
                 GZIPInputStream gzis = new GZIPInputStream(fis);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = gzis.read(buf)) != -1) {
                    baos.write(buf, 0, r);
                }
                byte[] rawState = baos.toByteArray();
                WorldBufferWireCodec.decodeChunkInto(rawState, engine.getWorldBuffer());
                engine.syncBufferToCells();
                return true;
            }
        } catch (Exception ex) {
            logger.error("Failed to load tick snapshot {}", targetTick, ex);
            return false;
        }
    }

    private static final java.util.concurrent.ExecutorService asyncDbExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ether-async-db-persistence");
        t.setDaemon(true);
        return t;
    });

    /*
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

    /*
     * Lists all available saved simulation snapshots across normal saves and auto-checkpoints.
     */
    public List<SaveMetadata> listSaves() {
        List<SaveMetadata> list = new ArrayList<>();
        Path baseSaveDir = getSaveDirectory();
        if (!Files.exists(baseSaveDir)) return list;

        try (Stream<Path> stream = Files.list(baseSaveDir)) {
            stream.filter(Files::isDirectory)
                    .forEach(dir -> {
                        Path metaFile = dir.resolve(METADATA_FILE);
                        if (Files.exists(metaFile)) {
                            try {
                                SaveMetadata meta = objectMapper.readValue(metaFile.toFile(), SaveMetadata.class);
                                if (meta != null && meta.getId() != null) {
                                    list.add(meta);
                                }
                            } catch (Exception ignored) {}
                        }
                    });
        } catch (IOException e) {
            logger.error("Failed to list saves", e);
        }

        list.sort((a, b) -> {
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });

        return list;
    }
}
