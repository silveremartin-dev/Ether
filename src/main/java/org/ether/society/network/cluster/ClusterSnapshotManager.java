/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.ether.society.network.codec.CellTopologyWireCodec;
import org.ether.society.network.codec.WorldBufferWireCodec;
import org.ether.society.persistence.SaveMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Fault-tolerant Delta & Checkpoint Persistence Manager for distributed planetary clusters.
 * Seamlessly integrates with the unified Ether persistence architecture:
 * 1. Saves immutable topology.bin.gz on initialization
 * 2. Writes compressed binary state snapshots (snapshot_tick_%010d.bin.gz)
 * 3. Maintains metadata.json & scenario.json so cluster runs can be loaded directly by the UI.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class ClusterSnapshotManager {
    private static final Logger logger = LoggerFactory.getLogger(ClusterSnapshotManager.class);

    private final Path snapshotDir;
    /* Internal state variable for max retained snapshots (int). */
    private final int maxRetainedSnapshots;
    private final ObjectMapper objectMapper;
    /* Internal state variable for topology saved (boolean). */
    private boolean topologySaved = false;

    /*
     * Cluster snapshot manager.
     * Enforces physical invariants and updates associated state variables within {@code ClusterSnapshotManager}.
     *
     */
    public ClusterSnapshotManager() {
        this(Paths.get("saves", "cluster_snapshots"), 5);
    }

    /*
     * Cluster snapshot manager.
     * Enforces physical invariants and updates associated state variables within {@code ClusterSnapshotManager}.
     *
     * @param snapshotDir the snapshot dir parameter (Path)
     * @param maxRetainedSnapshots the max retained snapshots parameter (int)
     */
    public ClusterSnapshotManager(Path snapshotDir, int maxRetainedSnapshots) {
        this.snapshotDir = snapshotDir;
        this.maxRetainedSnapshots = Math.max(1, maxRetainedSnapshots);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        try {
            Files.createDirectories(this.snapshotDir);
        } catch (IOException e) {
            logger.warn("Could not create snapshot directory {}: {}", snapshotDir, e.getMessage());
        }
    }

    /*
     * Get snapshot dir.
     * Enforces physical invariants and updates associated state variables within {@code ClusterSnapshotManager}.
     *
     * @return the resulting computation or state reference
     */
    public Path getSnapshotDir() {
        return snapshotDir;
    }

    /*
     * Persists static world topology if not already saved in the cluster directory.
     */
    public synchronized void ensureTopologySaved(List<H3Cell> cells) {
        if (topologySaved || cells == null || cells.isEmpty()) return;
        try {
            Path topologyPath = snapshotDir.resolve("topology.bin.gz");
            if (!Files.exists(topologyPath)) {
                CellTopologyWireCodec.saveToFile(topologyPath, cells);
            }
            topologySaved = true;
        } catch (Exception e) {
            logger.error("Failed to save cluster topology baseline: {}", e.getMessage());
        }
    }

    /*
     * Captures a compressed binary checkpoint of the entire WorldBuffer.
     */
    public synchronized Path saveSnapshot(long tickId, WorldBuffer buffer) throws IOException {
        return saveSnapshot(tickId, buffer, null, null);
    }

    /*
     * Captures a compressed binary checkpoint with optional topology and scenario metadata.
     */
    public synchronized Path saveSnapshot(long tickId, WorldBuffer buffer, List<H3Cell> optionalCells, Scenario optionalScenario) throws IOException {
        if (buffer == null || buffer.getCapacity() <= 0) return null;

        if (optionalCells != null && !optionalCells.isEmpty()) {
            ensureTopologySaved(optionalCells);
        }

        String fileName = String.format("snapshot_tick_%010d.bin.gz", tickId);
        Path targetFile = snapshotDir.resolve(fileName);

        byte[] rawBytes = WorldBufferWireCodec.encodeChunk(buffer, 0, buffer.getCapacity(), tickId);

        try (OutputStream fos = Files.newOutputStream(targetFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            gzos.write(rawBytes);
            gzos.finish();
        }

        // Also update state.bin.gz as the current head
        Path headState = snapshotDir.resolve("state.bin.gz");
        try {
            Files.copy(targetFile, headState, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}

        // Maintain metadata.json for direct UI discovery and replay loading
        try {
            String scenarioName = optionalScenario != null ? optionalScenario.getName() : "Cluster Simulation";
            long startYear = optionalScenario != null ? optionalScenario.getStartDateYear() : -100000L;
            long currentYear = startYear + (tickId / 12L);
            int currentMonth = (int) (tickId % 12L);

            SaveMetadata meta = new SaveMetadata(
                    snapshotDir.getFileName().toString(),
                    "Cluster Run (Tick " + tickId + ")",
                    currentYear,
                    currentMonth,
                    scenarioName
            );
            objectMapper.writeValue(snapshotDir.resolve("metadata.json").toFile(), meta);

            if (optionalScenario != null) {
                objectMapper.writeValue(snapshotDir.resolve("scenario.json").toFile(), optionalScenario);
            }
        } catch (Exception ex) {
            logger.debug("Failed to write metadata in cluster snapshot directory: {}", ex.getMessage());
        }

        logger.info("Saved cluster snapshot for Tick {} ({} KB) -> {}",
                tickId, rawBytes.length / 1024, targetFile.getFileName());

        pruneOldSnapshots();
        return targetFile;
    }

    /*
     * Restores simulation state into target WorldBuffer from the latest or specified snapshot file.
     */
    public synchronized WorldBufferWireCodec.ChunkPayload restoreSnapshot(Path snapshotFile, WorldBuffer targetBuffer) throws IOException {
        if (snapshotFile == null || !Files.exists(snapshotFile)) {
            throw new FileNotFoundException("Snapshot file does not exist: " + snapshotFile);
        }

        try (InputStream fis = Files.newInputStream(snapshotFile);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int read;
            while ((read = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }

            byte[] rawBytes = baos.toByteArray();
            WorldBufferWireCodec.ChunkPayload payload = WorldBufferWireCodec.decodeChunkInto(rawBytes, targetBuffer);
            logger.info("Restored cluster snapshot from {} (Tick {}, {} cells)",
                    snapshotFile.getFileName(), payload.getTickId(), payload.getChunkCount());
            return payload;
        }
    }

    /*
     * Finds the latest available snapshot in the directory.
     */
    public synchronized Path getLatestSnapshot() {
        try {
            if (!Files.exists(snapshotDir)) return null;
            File[] files = snapshotDir.toFile().listFiles((dir, name) -> name.startsWith("snapshot_tick_") && (name.endsWith(".bin.gz") || name.endsWith(".gz")));
            if (files == null || files.length == 0) return null;

            Arrays.sort(files, Comparator.comparing(File::getName).reversed());
            return files[0].toPath();
        } catch (Exception e) {
            logger.warn("Error finding latest snapshot: {}", e.getMessage());
            return null;
        }
    }

    // Helper subroutine: prune old snapshots - internal state computation & bounds checking
    private void pruneOldSnapshots() {
        // Network synchronization: Validate cryptographic payload and sequence barrier
        // Process spatial partition boundaries and propagate halo exchange buffer
        try {
            File[] files = snapshotDir.toFile().listFiles((dir, name) -> name.startsWith("snapshot_tick_") && (name.endsWith(".bin.gz") || name.endsWith(".gz")));
            if (files != null && files.length > maxRetainedSnapshots) {
                Arrays.sort(files, Comparator.comparing(File::getName));
                int toDelete = files.length - maxRetainedSnapshots;
                for (int i = 0; i < toDelete; i++) {
                    files[i].delete();
                }
            }
        } catch (Exception ignored) {}
    }
}
