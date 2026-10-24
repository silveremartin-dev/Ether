/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.analytics.HistoricalScenarioCalibrationHarness.CalibrationScenarioDefinition;
import org.ether.society.analytics.HistoricalScenarioCalibrationHarness.ScenarioCalibrationResult;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.network.codec.CellTopologyWireCodec;
import org.ether.society.network.codec.WorldBufferWireCodec;
import org.ether.society.persistence.SaveMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Persistence Manager for Reference Historical Calibration Runs.
 *
 * <p>Extracted from {@link HistoricalScenarioCalibrationHarness} to satisfy the
 * Single Responsibility Principle (SRP). Serializes spatial topology, active DOD
 * world state buffer, replay snapshots, and JSON telemetry metadata atomically.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 */
public final class HistoricalCalibrationPersistenceManager {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalCalibrationPersistenceManager.class);

    private HistoricalCalibrationPersistenceManager() {
        // Pure static persistence manager
    }

    /**
     * Persist result to disk and repository.
     *
     * @param def the def parameter (CalibrationScenarioDefinition)
     * @param result the result parameter (ScenarioCalibrationResult)
     * @param h3Cells the h3 cells parameter (List&lt;H3Cell&gt;)
     * @param world the world parameter (WorldBuffer)
     * @param checkpointSnapshots the checkpoint snapshots parameter (Map&lt;Long, byte[]&gt;)
     */
    public static void persistResultToDiskAndRepository(CalibrationScenarioDefinition def,
                                                         ScenarioCalibrationResult result,
                                                         List<H3Cell> h3Cells,
                                                         WorldBuffer world,
                                                         Map<Long, byte[]> checkpointSnapshots) {
        try {
            String runId = "RUN-" + def.scenarioKey();
            SimulationRunRecord record = new SimulationRunRecord(runId, def.displayName(), def.historicalRegimeDescription(), null);

            List<HistorySnapshot> snapshots = new ArrayList<>();
            for (Map.Entry<Integer, Double> entry : result.simulatedPopulationTrajectory.entrySet()) {
                int yr = entry.getKey();
                long pop = Math.round(entry.getValue() * 1_000_000.0);
                double tech = def.initialTechLevel() + (yr - def.startYear()) * 0.005;
                double food = pop * 1.2;
                double wealth = pop * (def.initialCapitalPerCapita() * 0.5);
                double energy = pop * (12.0 + tech * 1.5);
                double gdp = pop * (tech * 0.85);

                Map<String, Double> metrics = new LinkedHashMap<>();
                metrics.put("population", (double) pop);
                metrics.put("worldPopulation", (double) pop);
                metrics.put("foodPerCapita", 1.2);
                metrics.put("builtCapital", wealth);
                metrics.put("avgTechLevel", tech);
                metrics.put("technology", tech);
                metrics.put("grossWorldProduct", gdp);
                metrics.put("gdpTotal", gdp);
                metrics.put("primaryEnergy", energy);
                metrics.put("energyCaptured", energy);
                metrics.put("lifeExpectancy", 30.0 + tech * 4.0);
                metrics.put("giniIndex", 0.40);
                metrics.put("asabiyyah", 80.0);
                metrics.put("systemComplexity", 10.0 + tech * 5.0);

                record.addSnapshot(yr, pop, food, tech, 80.0, 100, metrics);

                snapshots.add(new HistorySnapshot(
                    yr, 1, pop, food, wealth, 30.0 + tech * 4.0, 0.40, tech,
                    energy, 0.5, 75.0, 5.0, gdp, 15.0, 10.0 + tech * 5.0, 100.0, 0.5, 5.0, 60.0, 0.0, 5000.0, 20000.0, 3.5, 1, 0.1
                ));
            }

            SimulationRunRepository.getInstance().saveRun(record);

            Path saveDir = org.ether.society.config.EtherPaths.getSavesDir().resolve(runId);
            Files.createDirectories(saveDir);
            Path snapshotsDir = saveDir.resolve("snapshots");
            Files.createDirectories(snapshotsDir);

            // 1. Persist Spatial Topology (topology.bin.gz)
            if (h3Cells != null && !h3Cells.isEmpty()) {
                Path topologyPath = saveDir.resolve("topology.bin.gz");
                CellTopologyWireCodec.saveToFile(topologyPath, h3Cells);
            }

            // 2. Persist Active World State (state.bin.gz)
            long endTick = Math.max(0, (long) (def.endYear() - def.startYear()) * 12L);
            if (world != null) {
                byte[] stateBytes = WorldBufferWireCodec.encodeChunk(world, 0, world.getCapacity(), endTick);
                Path statePath = saveDir.resolve("state.bin.gz");
                try (OutputStream fos = Files.newOutputStream(statePath);
                     GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
                    gzos.write(stateBytes);
                    gzos.finish();
                }
            }

            // 3. Persist Intermediate Replay Snapshots (snapshots/snapshot_tick_*.bin.gz)
            if (checkpointSnapshots != null && !checkpointSnapshots.isEmpty()) {
                for (Map.Entry<Long, byte[]> entry : checkpointSnapshots.entrySet()) {
                    long snapTick = entry.getKey();
                    String snapFileName = String.format("snapshot_tick_%010d.bin.gz", snapTick);
                    Path snapPath = snapshotsDir.resolve(snapFileName);
                    Path tmpSnapPath = snapshotsDir.resolve(snapFileName + ".tmp." + UUID.randomUUID());
                    try {
                        try (OutputStream fos = Files.newOutputStream(tmpSnapPath);
                             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
                            gzos.write(entry.getValue());
                            gzos.finish();
                        }
                        try {
                            Files.move(tmpSnapPath, snapPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception moveEx) {
                            Files.move(tmpSnapPath, snapPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (Exception snapEx) {
                        try {
                            Files.deleteIfExists(tmpSnapPath);
                        } catch (Exception ignored) {}
                        logger.warn("Could not write snapshot tick {} to {}: {}", snapTick, snapPath, snapEx.getMessage());
                    }
                }
            }

            // 4. Persist Scenario Configuration (scenario.json) atomically
            org.ether.society.model.Scenario scenario = new org.ether.society.model.Scenario();
            scenario.setName(def.displayName());
            scenario.setDescription(def.historicalRegimeDescription());
            scenario.setStartDateYear(def.startYear());
            scenario.setEndDateYear(def.endYear());
            scenario.setInitialHumanCount(Math.round(def.initialWorldPopMillions() * 1_000_000.0));
            scenario.setInitialCapitalPerCapita(def.initialCapitalPerCapita());
            scenario.setPlanetPreset(PlanetPreset.EARTH_LIKE);
            org.ether.society.persistence.EtherJsonUtil.writePrettyAtomic(saveDir.resolve("scenario.json"), scenario);

            // 5. Persist Historical Telemetry (history.json) atomically
            org.ether.society.persistence.EtherJsonUtil.writePrettyAtomic(saveDir.resolve("history.json"), snapshots);

            // 6. Persist Metadata (metadata.json) atomically
            SaveMetadata meta = new SaveMetadata(
                    runId,
                    def.displayName(),
                    def.endYear(),
                    1,
                    def.displayName()
            );
            org.ether.society.persistence.EtherJsonUtil.writePrettyAtomic(saveDir.resolve("metadata.json"), meta);

            logger.info("✅ Reference calibration run '{}' fully persisted with topology & snapshots in {}", def.scenarioKey(), saveDir);

        } catch (Exception ex) {
            logger.warn("Could not persist calibration result to disk for {}: {}", def.scenarioKey(), ex.getMessage(), ex);
        }
    }
}
