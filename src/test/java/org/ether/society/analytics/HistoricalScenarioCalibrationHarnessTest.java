/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit & Integration Test Suite for HistoricalScenarioCalibrationHarness.
 * Verifies multi-scenario before/after calibration on continuous non-bifurcating historical regimes,
 * cartographic tensor comparison, root cause drift decomposition, and multi-scale sensitivity matrices.
 */
public class HistoricalScenarioCalibrationHarnessTest {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalScenarioCalibrationHarnessTest.class);

    @Test
    @DisplayName("Should execute calibration across all 5 non-bifurcating historical scenarios with high R² fit")
    void testRunAllScenarioCalibrations() {
        List<HistoricalScenarioCalibrationHarness.ScenarioCalibrationResult> results =
                HistoricalScenarioCalibrationHarness.runAllScenarioCalibrations();

        assertNotNull(results, "Calibration results must not be null");
        assertEquals(5, results.size(), "Should calibrate exactly 5 canonical non-bifurcating scenarios");

        for (HistoricalScenarioCalibrationHarness.ScenarioCalibrationResult res : results) {
            logger.info("Validated Calibration for Scenario '{}': R² = {}, RMSE = {}, Mean MAPE = {}%",
                    res.scenario.displayName(), res.compositeRSquared, res.compositeRmse, res.meanMape);

            assertTrue(res.compositeRSquared >= 0.85, "Composite R² should be >= 0.85 for calibrated baseline");
            assertTrue(res.meanMape < 15.0, "Mean MAPE deviation should be < 15% across telemetry metrics");
            assertNotNull(res.densityMapComparison, "Spatial density map comparison should be computed");
            assertTrue(res.densityMapComparison.getPearsonR() > 0.80, "Spatial cross-correlation r must be > 0.80");
            assertTrue(res.densityMapComparison.getSsim() > 0.75, "Spatial SSIM must be > 0.75");
        }
    }

    @Test
    @DisplayName("Should decompose drift causes and generate parameter tuning remediations")
    void testDriftRootCauseDecomposition() {
        HistoricalScenarioCalibrationHarness.CalibrationScenarioDefinition def =
                HistoricalScenarioCalibrationHarness.CALIBRATION_SCENARIOS.get(3); // SECOND_INDUSTRIAL_ACCELERATION

        HistoricalScenarioCalibrationHarness.ScenarioCalibrationResult result =
                HistoricalScenarioCalibrationHarness.runScenarioCalibration(def);

        assertNotNull(result, "Scenario result must not be null");
        assertFalse(result.simulatedPopulationTrajectory.isEmpty(), "Trajectory must contain snapshots");

        // Verify drift diagnoses and parameter corrections
        for (HistoricalScenarioCalibrationHarness.DriftDiagnosis diag : result.driftDiagnoses) {
            assertNotNull(diag.category(), "Drift category must be classified");
            assertNotNull(diag.parameterToTune(), "Parameter to tune must be identified");
            assertNotNull(diag.recommendedValue(), "Recommended value must be computed");
            assertTrue(diag.tuningMultiplier() > 0.0, "Tuning factor must be strictly positive");
        }
    }

    @Test
    @DisplayName("Should generate multi-scale sensitivity matrix across spatial, temporal, and cohort dimensions")
    void testMultiScaleSensitivityMatrix() {
        HistoricalScenarioCalibrationHarness.CalibrationScenarioDefinition def =
                HistoricalScenarioCalibrationHarness.CALIBRATION_SCENARIOS.get(0); // CLASSICAL_AGRARIAN_EXPANSION

        HistoricalScenarioCalibrationHarness.MultiScaleSensitivityMatrix matrix =
                HistoricalScenarioCalibrationHarness.runMultiScaleSensitivityMatrix(def);

        assertNotNull(matrix, "MultiScaleSensitivityMatrix must not be null");
        assertEquals(4, matrix.spatialEntries.size(), "Should evaluate 4 spatial resolutions (Res 2 to 5)");
        assertEquals(5, matrix.temporalEntries.size(), "Should evaluate 5 temporal steps (30d to 1825d)");
        assertEquals(5, matrix.cohortEntries.size(), "Should evaluate 5 cohort granularity sizes");

        // Check spatial convergence: finer resolution -> lower spatial RMSE and higher SSIM
        for (int i = 1; i < matrix.spatialEntries.size(); i++) {
            assertTrue(matrix.spatialEntries.get(i).spatialRmse() <= matrix.spatialEntries.get(i - 1).spatialRmse(),
                    "Spatial RMSE must decrease or stay stable with finer H3 resolution");
        }

        // Check report generation
        List<HistoricalScenarioCalibrationHarness.ScenarioCalibrationResult> results =
                HistoricalScenarioCalibrationHarness.runAllScenarioCalibrations();
        String mdReport = HistoricalScenarioCalibrationHarness.generateMarkdownCalibrationReport(results, matrix);

        assertNotNull(mdReport, "Generated markdown report must not be null");
        assertTrue(mdReport.contains("ETHER CLIODYNAMIC ENGINE"), "Report should contain main title");
        assertTrue(mdReport.contains("Multi-Scale Discretization & Numerical Sensitivity Matrix"), "Report should contain sensitivity section");
    }
}
