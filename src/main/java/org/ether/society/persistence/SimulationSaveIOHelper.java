/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.network.codec.WorldBufferWireCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * File I/O and disk compression helper for {@link SimulationPersistenceService}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class SimulationSaveIOHelper {
    private static final Logger logger = LoggerFactory.getLogger(SimulationSaveIOHelper.class);

    private static final String METADATA_FILE = "metadata.json";
    private static final String SCENARIO_FILE = "scenario.json";
    private static final String HISTORY_FILE = "history.json";
    private static final String SNAPSHOTS_DIR = "snapshots";

    private SimulationSaveIOHelper() {
        // Utility class
    }

    /**
     * Reads and decompresses GZIP-compressed binary bytes from a file.
     *
     * @param path source file path
     * @return decompressed raw byte array
     * @throws IOException on I/O failure
     */
    public static byte[] readGzipBytes(Path path) throws IOException {
        try (InputStream fis = Files.newInputStream(path);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = gzis.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Compresses and writes raw bytes to a GZIP-compressed file.
     *
     * @param path target destination path
     * @param data uncompressed raw byte array
     * @throws IOException on I/O failure
     */
    public static void writeGzipBytes(Path path, byte[] data) throws IOException {
        try (OutputStream fos = Files.newOutputStream(path);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            gzos.write(data);
            gzos.finish();
        }
    }

    /**
     * Loads a specific tick snapshot from the save's snapshots/ directory into the engine.
     *
     * @param savePath directory path of the saved scenario
     * @param targetTick tick index to restore
     * @param engine simulation engine to restore state into
     * @return true if snapshot was found and restored successfully
     */
    public static boolean loadTickSnapshot(Path savePath, long targetTick, H3SimulationEngine engine) {
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

            byte[] rawState = readGzipBytes(snapPath);
            WorldBufferWireCodec.decodeChunkInto(rawState, engine.getWorldBuffer());
            engine.syncBufferToCells();
            return true;
        } catch (Exception ex) {
            logger.error("Failed to load tick snapshot {}", targetTick, ex);
            return false;
        }
    }

    /**
     * Lists all available saved simulation snapshots across normal saves and auto-checkpoints.
     *
     * @param baseSaveDir base directory containing saves
     * @param objectMapper Jackson object mapper for deserializing metadata
     * @return sorted list of save metadata objects
     */
    public static List<SaveMetadata> listSaves(Path baseSaveDir, ObjectMapper objectMapper) {
        List<SaveMetadata> list = new ArrayList<>();
        if (!Files.exists(baseSaveDir)) return list;

        try (Stream<Path> stream = Files.list(baseSaveDir)) {
            stream.filter(Files::isDirectory)
                    .forEach(dir -> {
                        Path metaFile = dir.resolve(METADATA_FILE);
                        SaveMetadata meta = null;
                        if (Files.exists(metaFile)) {
                            try {
                                meta = objectMapper.readValue(metaFile.toFile(), SaveMetadata.class);
                            } catch (Exception ex) {
                                logger.debug("Could not deserialize metadata.json in {}: {}", dir.getFileName(), ex.getMessage());
                            }
                        }

                        if (meta == null || meta.getId() == null) {
                            // Fallback & Auto-repair: Recover metadata from scenario.json and history.json
                            Path scenarioFile = dir.resolve(SCENARIO_FILE);
                            Path historyFile = dir.resolve(HISTORY_FILE);
                            if (Files.exists(scenarioFile) || Files.exists(historyFile)) {
                                String dirName = dir.getFileName().toString();
                                String scName = dirName;
                                long endYear = 0;
                                int cellCount = 0;
                                int res = 3;

                                if (Files.exists(scenarioFile)) {
                                    try {
                                        org.ether.society.model.Scenario sc = objectMapper.readValue(scenarioFile.toFile(), org.ether.society.model.Scenario.class);
                                        if (sc != null) {
                                            if (sc.getName() != null && !sc.getName().isBlank()) scName = sc.getName();
                                            endYear = sc.getEndDateYear();
                                            res = sc.getH3Resolution();
                                        }
                                    } catch (Exception ignored) { org.slf4j.LoggerFactory.getLogger(SimulationSaveIOHelper.class).debug("Non-fatal condition in SimulationSaveIOHelper: {}", ignored.getMessage()); }
                                }

                                if (Files.exists(historyFile)) {
                                    try {
                                        List<org.ether.society.analytics.HistorySnapshot> snaps = objectMapper.readValue(historyFile.toFile(), objectMapper.getTypeFactory().constructCollectionType(List.class, org.ether.society.analytics.HistorySnapshot.class));
                                        if (snaps != null && !snaps.isEmpty()) {
                                            endYear = snaps.get(snaps.size() - 1).year();
                                        }
                                    } catch (Exception ignored) { org.slf4j.LoggerFactory.getLogger(SimulationSaveIOHelper.class).debug("Non-fatal condition in SimulationSaveIOHelper: {}", ignored.getMessage()); }
                                }

                                meta = new SaveMetadata(dirName, scName, endYear, 1, scName, cellCount, res);
                                try {
                                    EtherJsonHelper.writePrettyAtomic(metaFile, meta);
                                    logger.info("Auto-repaired corrupted or missing metadata.json for save: {}", dirName);
                                } catch (Exception ignored) { org.slf4j.LoggerFactory.getLogger(SimulationSaveIOHelper.class).debug("Non-fatal condition in SimulationSaveIOHelper: {}", ignored.getMessage()); }
                            }
                        }

                        if (meta != null) {
                            // Directory name on disk is the authoritative filesystem identifier
                            meta.setId(dir.getFileName().toString());
                            list.add(meta);
                        }
                    });
        } catch (IOException e) {
            logger.error("Failed to list saves", e);
        }

        list.sort(Comparator.comparing(SaveMetadata::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }
}


