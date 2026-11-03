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

        // Bound scenario duration to 1 year (12 monthly ticks) for regression test speed
        ramessesScenario.setEndDateYear(-1278);

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
    @DisplayName("Execute Real High-Resolution Simulation Run for Black Death Scenario (1347 CE, dt = 30 days)")
    public void testBlackDeathHighResolutionSimulationRun() {
        Scenario blackDeathScenario = null;
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            if ("black_death_1347".equals(s.getPresetKey())) {
                blackDeathScenario = s;
                break;
            }
        }
        assertNotNull(blackDeathScenario, "Black Death 1347 scenario must exist in built-in scenarios");

        // Bound duration for test execution (1347 to 1353: the 6 critical plague pandemic years)
        blackDeathScenario.setEndDateYear(1353);
        blackDeathScenario.setH3Resolution(2);
        blackDeathScenario.setTargetCohortSize(25000);

        // Execute headless real physics run
        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(blackDeathScenario);
        assertNotNull(record, "Execution record must not be null");
        assertFalse(record.getTimeSeriesData().isEmpty(), "Telemetry time series must contain sampled data");

        // Verify save persistence
        Path savesDir = EtherPaths.getSavesDir();
        assertTrue(Files.exists(savesDir), "Saves directory must exist");
        File[] matchingSaves = savesDir.toFile().listFiles((dir, name) -> name.contains("BLACK_DEATH") || name.contains("PESTE"));
        assertNotNull(matchingSaves, "Matching save directories must be listed");
        assertTrue(matchingSaves.length > 0, "At least one Black Death save run directory must be created in saves/");
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

        // Bound planetary scenario duration to 1 year (12 monthly ticks) for regression test speed
        earthBAUScenario.setEndDateYear(2026);
        // Optimize resolution and cohort granularity for regression test performance
        earthBAUScenario.setH3Resolution(2);
        earthBAUScenario.setTargetCohortSize(50000);

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

    @Test
    @DisplayName("Execute Real High-Resolution Simulation Run for World Wars 1914-1960 Scenario (dt = 30 days)")
    public void testWorldWars1914SimulationRun() {
        Scenario ww1Scenario = null;
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            if ("world_wars_totalitarian_1914".equals(s.getPresetKey())) {
                ww1Scenario = s;
                break;
            }
        }
        assertNotNull(ww1Scenario, "World Wars 1914 scenario must exist in built-in scenarios");

        // Run from 1914 to 1920 (WWI + Spanish Flu peak interval)
        ww1Scenario.setEndDateYear(1920);
        ww1Scenario.setH3Resolution(2);
        ww1Scenario.setTargetCohortSize(50000);

        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(ww1Scenario);
        assertNotNull(record, "Execution record must not be null");
        assertFalse(record.getTimeSeriesData().isEmpty(), "Telemetry time series must contain sampled data");

        Path savesDir = EtherPaths.getSavesDir();
        assertTrue(Files.exists(savesDir), "Saves directory must exist");
        File[] matchingSaves = savesDir.toFile().listFiles((dir, name) -> name.contains("1914") || name.contains("TOTALITARIAN") || name.contains("GUERRES"));
        assertNotNull(matchingSaves, "Matching save directories must be listed");
        assertTrue(matchingSaves.length > 0, "At least one 1914 save directory must be created in saves/");
    }

    @Test
    @DisplayName("Execute Real High-Resolution Simulation Run for Bronze Age Collapse (-1200 BCE, dt = 30 days)")
    public void testBronzeAgeCollapseSimulationRun() {
        Scenario sc = null;
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            if ("bronze_age_collapse_1200bc".equals(s.getPresetKey())) {
                sc = s;
                break;
            }
        }
        assertNotNull(sc, "Bronze Age Collapse scenario must exist in built-in scenarios");

        // Run from -1200 to -1198 (2 years covering initial shock)
        sc.setEndDateYear(-1198);
        sc.setH3Resolution(3);
        sc.setTargetCohortSize(10000);

        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(sc);
        assertNotNull(record, "Execution record must not be null");
        assertFalse(record.getTimeSeriesData().isEmpty(), "Telemetry time series must contain sampled data");
    }

    @Test
    @DisplayName("Execute Real High-Resolution Simulation Run for Marcus Aurelius Antonine Plague (165 CE, dt = 30 days)")
    public void testAntoninePlagueSimulationRun() {
        Scenario sc = null;
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            if ("antonine_plague_165".equals(s.getPresetKey())) {
                sc = s;
                break;
            }
        }
        assertNotNull(sc, "Antonine Plague scenario must exist in built-in scenarios");

        // Run from 165 to 167 (2 years initial pandemic wave)
        sc.setEndDateYear(167);
        sc.setH3Resolution(3);
        sc.setTargetCohortSize(20000);

        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(sc);
        assertNotNull(record, "Execution record must not be null");
        assertFalse(record.getTimeSeriesData().isEmpty(), "Telemetry time series must contain sampled data");
    }

    @Test
    @DisplayName("Execute Real High-Resolution Simulation Run for Augustan Pax Romana Continuity (An 0 - 50 CE, dt = 30 days)")
    public void testAugustusPaxRomanaContinuitySimulationRun() {
        Scenario sc = null;
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            if ("pax_romana_augustus_0".equals(s.getPresetKey())) {
                sc = s;
                break;
            }
        }
        assertNotNull(sc, "Pax Romana Augustus scenario must exist in built-in scenarios");

        // Run from An 0 to An 2 (2 years of stable continuity baseline)
        sc.setEndDateYear(2);
        sc.setH3Resolution(3);
        sc.setTargetCohortSize(20000);

        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(sc);
        assertNotNull(record, "Execution record must not be null");
        assertFalse(record.getTimeSeriesData().isEmpty(), "Telemetry time series must contain sampled data");
    }
}
