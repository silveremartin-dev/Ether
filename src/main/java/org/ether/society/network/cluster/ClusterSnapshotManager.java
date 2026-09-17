/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.cluster;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.network.codec.WorldBufferWireCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Fault-tolerant Delta & Checkpoint Persistence Manager for distributed planetary clusters.
 * Periodically captures compressed binary state snapshots of the WorldBuffer to disk,
 * allowing recovering nodes or late-joining compute workers to restore simulation state instantly.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class ClusterSnapshotManager {
    private static final Logger logger = LoggerFactory.getLogger(ClusterSnapshotManager.class);

    private final Path snapshotDir;
    private final int maxRetainedSnapshots;

    public ClusterSnapshotManager() {
        this(Paths.get("saves", "cluster_snapshots"), 5);
    }

    public ClusterSnapshotManager(Path snapshotDir, int maxRetainedSnapshots) {
        this.snapshotDir = snapshotDir;
        this.maxRetainedSnapshots = Math.max(1, maxRetainedSnapshots);
        try {
            Files.createDirectories(this.snapshotDir);
        } catch (IOException e) {
            logger.warn("Could not create snapshot directory {}: {}", snapshotDir, e.getMessage());
        }
    }

    /**
     * Captures a compressed binary checkpoint of the entire WorldBuffer.
     */
    public synchronized Path saveSnapshot(long tickId, WorldBuffer buffer) throws IOException {
        if (buffer == null || buffer.getCapacity() <= 0) return null;

        String fileName = String.format("snapshot_tick_%010d.gz", tickId);
        Path targetFile = snapshotDir.resolve(fileName);

        byte[] rawBytes = WorldBufferWireCodec.encodeChunk(buffer, 0, buffer.getCapacity(), tickId);

        try (OutputStream fos = Files.newOutputStream(targetFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            gzos.write(rawBytes);
            gzos.finish();
        }

        logger.info("💾 Saved cluster snapshot for Tick {} ({} KB) -> {}",
                tickId, rawBytes.length / 1024, targetFile.getFileName());

        pruneOldSnapshots();
        return targetFile;
    }

    /**
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
            logger.info("♻️ Restored cluster snapshot from {} (Tick {}, {} cells)",
                    snapshotFile.getFileName(), payload.getTickId(), payload.getChunkCount());
            return payload;
        }
    }

    /**
     * Finds the latest available snapshot in the directory.
     */
    public synchronized Path getLatestSnapshot() {
        try {
            if (!Files.exists(snapshotDir)) return null;
            File[] files = snapshotDir.toFile().listFiles((dir, name) -> name.startsWith("snapshot_tick_") && name.endsWith(".gz"));
            if (files == null || files.length == 0) return null;

            Arrays.sort(files, Comparator.comparing(File::getName).reversed());
            return files[0].toPath();
        } catch (Exception e) {
            logger.warn("Error finding latest snapshot: {}", e.getMessage());
            return null;
        }
    }

    private void pruneOldSnapshots() {
        try {
            File[] files = snapshotDir.toFile().listFiles((dir, name) -> name.startsWith("snapshot_tick_") && name.endsWith(".gz"));
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

