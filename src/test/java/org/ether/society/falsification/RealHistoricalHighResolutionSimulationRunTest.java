/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.falsification;

import org.ether.society.analytics.HeadlessBatchRunner;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.config.EtherPaths;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * High-Resolution Multi-Scale Physical Simulation Validation Suite.
 * Executes full step-by-step physical simulation runs (Zero-Skip) for:
 * 1. Ramesses II Scenario (-1279 to -1200 BCE) at H3 Resolution 5 (Egypt & Near East Regional Grid, dt = 30 days)
 * 2. Earth 2025-2055 Business-As-Usual (BAU) Scenario at Planetary Grid Resolution (dt = 30 days)
 *
 * Verifies that full snapshots, compressed topologies, and telemetry files are generated in saves/.
 */
public class RealHistoricalHighResolutionSimulationRunTest {

    @Test
    @DisplayName("Execute Real High-Resolution Simulation Run for Ramesses II Scenario (Res 5, dt = 30 days)")
    public void testRamessesIIHighResolutionSimulationRun() {
        Scenario ramessesScenario = null;
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            if ("ramesses_ii".equals(s.getPresetKey())) {
                ramessesScenario = s;
                break;
            }
        }
        assertNotNull(ramessesScenario, "Ramesses II scenario must exist in built-in scenarios");

        // Execute headless real physics run
        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(ramessesScenario);
        assertNotNull(record, "Execution record must not be null");
        assertFalse(record.getTimeSeriesData().isEmpty(), "Telemetry time series must contain sampled data");

        // Verify save persistence
        Path savesDir = EtherPaths.getSavesDir();
        assertTrue(Files.exists(savesDir), "Saves directory must exist");
        File[] matchingSaves = savesDir.toFile().listFiles((dir, name) -> name.contains("RAMS") || name.contains("RAMESSES"));
        assertNotNull(matchingSaves, "Matching save directories must be listed");
        assertTrue(matchingSaves.length > 0, "At least one Ramesses II save run directory must be created in saves/");
    }

    @Test
    @DisplayName("Execute Real High-Resolution Simulation Run for Earth 2025 BAU Scenario (Planetary Grid, dt = 30 days)")
    public void testEarth2025BAUHighResolutionSimulationRun() {
        Scenario earthBAUScenario = null;
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            if ("business_as_usual".equals(s.getPresetKey())) {
                earthBAUScenario = s;
                break;
            }
        }
        assertNotNull(earthBAUScenario, "Earth 2025 BAU scenario must exist in built-in scenarios");

        // Execute headless real physics run
        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(earthBAUScenario);
        assertNotNull(record, "Execution record must not be null");
        assertFalse(record.getTimeSeriesData().isEmpty(), "Telemetry time series must contain sampled data");

        // Verify save persistence
        Path savesDir = EtherPaths.getSavesDir();
        assertTrue(Files.exists(savesDir), "Saves directory must exist");
        File[] matchingSaves = savesDir.toFile().listFiles((dir, name) -> name.contains("TERRE-2025") || name.contains("BUSINESS-AS-USUAL"));
        assertNotNull(matchingSaves, "Matching save directories must be listed");
        assertTrue(matchingSaves.length > 0, "At least one Earth 2025 BAU save run directory must be created in saves/");
    }
}
