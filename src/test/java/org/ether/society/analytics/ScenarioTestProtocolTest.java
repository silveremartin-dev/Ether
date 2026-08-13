/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.model.Scenario;
import org.ether.society.persistence.ScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit & Integration Test Suite for the Scenario Testing Protocol ("Début connu, Fin à comparer, Expliquer et Corriger").
 * Verifies:
 * 1. Known Start - Expected End execution & telemetry tracking.
 * 2. Root cause divergence analysis & T_divergence detection.
 * 3. Automated Parameter Correction generation & application.
 * 4. Iterative Auto-Calibration convergence loop.
 *
 * @author Silvere Martin-Michiellot
 */
class ScenarioTestProtocolTest {

    private ScenarioTestProtocol protocolEngine;
    private ScenarioRepository scenarioRepository;

    @BeforeEach
    void setUp() {
        protocolEngine = new ScenarioTestProtocol();
        scenarioRepository = new ScenarioRepository();
    }

    @Test
    @DisplayName("Test Known Start - Expected End Protocol Execution and Divergence Analysis")
    void testKnownStartExpectedEndProtocol() {
        Scenario scenario = new Scenario();
        scenario.setName("Roman Empire Validation Protocol");
        scenario.setStartDateYear(0);
        scenario.setEndDateYear(400);
        scenario.setInitialHumanCount(50_000_000L);
        scenario.setInitialCapitalPerCapita(40.0);
        scenario.setInitialFoodReserveMonths(3.0); // Intentionally low food reserve

        // Create target expected run (synthetic historical trajectory)
        String targetRunId = "TARGET-ROMAN-EMPIRE-BENCHMARK";
        Map<String, String> targetParams = new LinkedHashMap<>();
        targetParams.put("Population Initiale", "50,000,000");
        targetParams.put("Réserves Food F0", "12.0 mois");
        targetParams.put("Capital Physique K0", "50.0 kg/hab");

        SimulationRunRecord targetRun = new SimulationRunRecord(targetRunId, scenario.getName(), "Expected Historical Target", targetParams);
        targetRun.addSnapshot(0, 50_000_000L, 50_000_000.0, 10.0, 85.0, 500);
        targetRun.addSnapshot(100, 55_000_000L, 55_000_000.0, 12.0, 80.0, 600);
        targetRun.addSnapshot(200, 60_000_000L, 60_000_000.0, 15.0, 75.0, 700);
        targetRun.addSnapshot(300, 65_000_000L, 65_000_000.0, 18.0, 70.0, 800);
        targetRun.addSnapshot(400, 70_000_000L, 70_000_000.0, 20.0, 65.0, 900);

        // Run Protocol
        ScenarioTestProtocol.ProtocolReport report = protocolEngine.runProtocol(scenario, targetRun);

        assertNotNull(report, "Protocol report should not be null");
        assertNotNull(report.getExecutionRecord(), "Execution record should be generated");
        assertNotNull(report.getComparisonResult(), "Comparison result should not be null");
        assertNotNull(report.getComparisonResult().getPrimaryRootCauseExplanation(), "Explanation must be present");
        assertFalse(report.getComparisonResult().getProposedCorrections().isEmpty(), "Correction proposals should be generated for parameter mismatch");

        // Verify report formatting
        String summary = report.getSummary();
        assertTrue(summary.contains("Protocol Report"), "Summary should contain header");
        assertTrue(summary.contains("Roman Empire Validation Protocol"), "Summary should mention scenario name");
    }

    @Test
    @DisplayName("Test Automated Parameter Correction Application ('Corriger')")
    void testParameterCorrectionApplication() {
        Scenario scenario = new Scenario();
        scenario.setName("Fertile Crescent Calibration");
        scenario.setStartDateYear(-8000);
        scenario.setEndDateYear(-6000);
        scenario.setInitialHumanCount(25_000L);
        scenario.setInitialFoodReserveMonths(4.0);
        scenario.setInitialCapitalPerCapita(15.0);

        double originalFood = scenario.getInitialFoodReserveMonths();

        RootCauseAnalyzer.ParameterCorrection foodCorrection = new RootCauseAnalyzer.ParameterCorrection(
            "initialFoodReserveMonths", "4.0 mois", "8.0 mois", 2.0, "Correction for food deficit"
        );
        RootCauseAnalyzer.ParameterCorrection capCorrection = new RootCauseAnalyzer.ParameterCorrection(
            "initialCapitalPerCapita", "15.0 kg/hab", "30.0 kg/hab", 2.0, "Correction for capital deficit"
        );

        protocolEngine.applyCorrections(scenario, java.util.List.of(foodCorrection, capCorrection));

        assertEquals(8.0, scenario.getInitialFoodReserveMonths(), 0.01, "Food reserve months should be updated to 8.0");
        assertEquals(30.0, scenario.getInitialCapitalPerCapita(), 0.01, "Capital per capita should be updated to 30.0");
    }

    @Test
    @DisplayName("Test Iterative Self-Calibration Loop ('AutoCalibrate')")
    void testIterativeAutoCalibrationLoop() {
        Scenario scenario = new Scenario();
        scenario.setName("Iterative Calibration Test");
        scenario.setStartDateYear(1000);
        scenario.setEndDateYear(1500);
        scenario.setInitialHumanCount(100_000L);
        scenario.setInitialFoodReserveMonths(2.0);

        // Target record expecting higher food / growth
        SimulationRunRecord targetRun = new SimulationRunRecord("TARGET-ITERATIVE", scenario.getName(), "Target", Map.of("Réserves Food F0", "12.0 mois"));
        targetRun.addSnapshot(1000, 100_000L, 100_000.0, 10.0, 80.0, 100);
        targetRun.addSnapshot(1250, 200_000L, 200_000.0, 15.0, 80.0, 200);
        targetRun.addSnapshot(1500, 400_000L, 400_000.0, 20.0, 80.0, 400);

        ScenarioTestProtocol.ProtocolReport finalReport = protocolEngine.autoCalibrate(scenario, targetRun, 3, 50.0);

        assertNotNull(finalReport, "Final report should be generated");
        assertTrue(scenario.getInitialFoodReserveMonths() > 2.0, "Scenario food reserves should have increased after auto-calibration iterations");
    }

    @Test
    @DisplayName("Test Benchmark Protocol Execution against Historical Validation Kernel")
    void testBenchmarkProtocolExecution() {
        Scenario scenario = new Scenario();
        scenario.setName("Deep Horizon Benchmark Test");
        scenario.setStartDateYear(-100000);
        scenario.setEndDateYear(2026);
        scenario.setInitialHumanCount(50_000L);

        ScenarioTestProtocol.ProtocolReport report = protocolEngine.runProtocolAgainstBenchmark(
            scenario, HistoricalValidationKernel.EpochWindow.DEEP_HORIZON
        );

        assertNotNull(report, "Benchmark protocol report should not be null");
        assertNotNull(report.getExecutionRecord(), "Execution record must be created");
        assertNotNull(report.getComparisonResult(), "Comparison result must be populated");
    }
}
