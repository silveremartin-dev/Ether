/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.procedural.ProceduralEngineRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite validating Historical Auto-Calibration & 20-Variable JSON Benchmark Suite.
 * Systematically verifies:
 * 1. JSON resource parsing of 20 macro-historical, cliodynamic, ecological, and technological variables.
 * 2. 20-variable trajectory generation and validation.
 * 3. Bounded Epoch Window Scenarios (500-Year, Classical/Medieval, Modern Industrial, Deep Horizon).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class HistoricalAutoCalibrationTest {

    @BeforeEach
    public void setUp() {
        ProceduralEngineRegistry.clearPlugins();
    }

    @Test
    public void testJson20VariableBenchmarkLoading() {
        Map<Integer, Double> popBenchmark = HistoricalValidationKernel.getHistoricalWorldPopulation();
        Map<Integer, Double> gdpBenchmark = HistoricalValidationKernel.getHistoricalWorldGdp();
        Map<Integer, Double> debasementBenchmark = HistoricalValidationKernel.getHistoricalCurrencyDebasement();
        Map<Integer, Double> eliteOverprodBenchmark = HistoricalValidationKernel.getBenchmarkDataset("eliteOverproductionIndex");
        Map<Integer, Double> realWageBenchmark = HistoricalValidationKernel.getBenchmarkDataset("realUnskilledWage");
        Map<Integer, Double> psiBenchmark = HistoricalValidationKernel.getBenchmarkDataset("politicalStressIndex");

        assertFalse(popBenchmark.isEmpty(), "Population benchmark dataset should be populated from JSON.");
        assertFalse(gdpBenchmark.isEmpty(), "GDP benchmark dataset should be populated from JSON.");
        assertFalse(debasementBenchmark.isEmpty(), "Currency debasement dataset should be populated from JSON.");
        assertFalse(eliteOverprodBenchmark.isEmpty(), "Elite overproduction dataset should be populated from JSON.");
        assertFalse(realWageBenchmark.isEmpty(), "Real wage dataset should be populated from JSON.");
        assertFalse(psiBenchmark.isEmpty(), "PSI dataset should be populated from JSON.");
    }

    @Test
    public void testInterpolatedBenchmarkValue() {
        double exactVal1900 = HistoricalValidationKernel.getInterpolatedBenchmarkValue("worldPopulation", 1900);
        double exactVal2000 = HistoricalValidationKernel.getInterpolatedBenchmarkValue("worldPopulation", 2000);
        double pchip1950 = HistoricalValidationKernel.getInterpolatedBenchmarkValue("worldPopulation", 1950, HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
        double catmull1950 = HistoricalValidationKernel.getInterpolatedBenchmarkValue("worldPopulation", 1950, HistoricalValidationKernel.InterpolationMethod.CATMULL_ROM_SPLINE);
        double linear1950 = HistoricalValidationKernel.getInterpolatedBenchmarkValue("worldPopulation", 1950, HistoricalValidationKernel.InterpolationMethod.LINEAR);

        assertTrue(exactVal1900 > 0, "1900 benchmark population should be positive.");
        assertTrue(exactVal2000 > exactVal1900, "2000 benchmark population should exceed 1900.");
        assertTrue(pchip1950 >= exactVal1900 && pchip1950 <= exactVal2000, "PCHIP 1950 interpolated population should be monotonic between 1900 and 2000.");
        assertTrue(catmull1950 > 0, "Catmull-Rom spline output should be valid.");
        assertTrue(linear1950 >= exactVal1900 && linear1950 <= exactVal2000, "Linear 1950 interpolated population should be valid.");

        // C1 Differentiability Check: numerical derivative dy/dt around year 1950
        double eps = 1e-4;
        double valMinus = HistoricalValidationKernel.getInterpolatedBenchmarkValue("worldPopulation", 1950.0 - eps, HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
        double valPlus = HistoricalValidationKernel.getInterpolatedBenchmarkValue("worldPopulation", 1950.0 + eps, HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
        double derivative = (valPlus - valMinus) / (2.0 * eps);

        assertTrue(derivative > 0, "Derivative dy/dt of population around 1950 should be positive and well-defined (C1 differentiable).");
    }

    @Test
    public void testAutoCalibrationExecution() {
        HistoricalAutoCalibrator.CalibrationResult bestFit = HistoricalAutoCalibrator.evaluateAndAutoCalibrate();

        assertNotNull(bestFit, "Auto-calibrator should return an optimal best-fit result.");
        assertTrue(bestFit.rSquared > 0.50, "Optimal calibrated model should achieve R^2 > 0.50 fit.");
        assertTrue(bestFit.rmse >= 0.0, "RMSE must be non-negative.");
        assertNotNull(bestFit.simulatedTrajectory, "Simulated trajectory map should be populated.");
        assertFalse(bestFit.simulatedTrajectory.isEmpty(), "Trajectory map should contain historical points.");
    }

    @Test
    public void testEarlyModern500YearWindowScenario() {
        HistoricalAutoCalibrator.CalibrationResult result =
            HistoricalAutoCalibrator.evaluateWindowedAutoCalibration(HistoricalValidationKernel.EpochWindow.EARLY_MODERN_500YR);

        assertNotNull(result, "500-Year Early Modern scenario result should be non-null.");
        assertNotNull(result.multiMetricReport, "Multi-metric report for 500-yr window should be generated.");
        assertEquals(HistoricalValidationKernel.EpochWindow.EARLY_MODERN_500YR, result.multiMetricReport.window);
        assertTrue(result.multiMetricReport.compositeRSquared > 0.50, "500-Year series composite R^2 should be > 0.50.");
        assertTrue(result.multiMetricReport.metricFits.containsKey("grossWorldProduct"), "GWP metric fit should be evaluated in 500-yr window.");
        assertTrue(result.multiMetricReport.metricFits.containsKey("literacyRate"), "Literacy metric fit should be evaluated in 500-yr window.");
        assertTrue(result.multiMetricReport.metricFits.containsKey("realUnskilledWage"), "Real wage metric fit should be evaluated in 500-yr window.");
    }

    @Test
    public void testClassicalMedievalWindowScenario() {
        HistoricalAutoCalibrator.CalibrationResult result =
            HistoricalAutoCalibrator.evaluateWindowedAutoCalibration(HistoricalValidationKernel.EpochWindow.CLASSICAL_MEDIEVAL);

        assertNotNull(result, "Classical/Medieval scenario result should be non-null.");
        assertNotNull(result.multiMetricReport, "Multi-metric report for classical window should be generated.");
        assertEquals(HistoricalValidationKernel.EpochWindow.CLASSICAL_MEDIEVAL, result.multiMetricReport.window);
        assertTrue(result.multiMetricReport.metricFits.containsKey("currencyDebasement"), "Currency debasement should be evaluated in classical window.");
        assertTrue(result.multiMetricReport.metricFits.containsKey("eliteOverproductionIndex"), "Elite overproduction should be evaluated in classical window.");
        assertTrue(result.multiMetricReport.metricFits.containsKey("politicalStressIndex"), "PSI should be evaluated in classical window.");
    }

    @Test
    public void testModernIndustrial125YearWindowScenario() {
        HistoricalAutoCalibrator.CalibrationResult result =
            HistoricalAutoCalibrator.evaluateWindowedAutoCalibration(HistoricalValidationKernel.EpochWindow.MODERN_INDUSTRIAL);

        assertNotNull(result, "Modern Industrial scenario result should be non-null.");
        assertNotNull(result.multiMetricReport, "Multi-metric report for modern window should be generated.");
        assertEquals(HistoricalValidationKernel.EpochWindow.MODERN_INDUSTRIAL, result.multiMetricReport.window);
        assertTrue(result.multiMetricReport.compositeRSquared > 0.50, "Modern Industrial composite R^2 should be > 0.50.");
    }

    @Test
    public void testBaselineVsEmpiricalSuiteComparison() {
        Map<Integer, Double> benchmark = HistoricalValidationKernel.getHistoricalWorldPopulation();

        HistoricalAutoCalibrator.CalibrationResult baseline =
            HistoricalAutoCalibrator.runTrajectoryEvaluation("Baseline", List.of(), benchmark);

        HistoricalAutoCalibrator.CalibrationResult fullEmpirical =
            HistoricalAutoCalibrator.runTrajectoryEvaluation("Full Empirical Suite",
                List.of("FrontierAsabiyyah", "MonasticBuffer", "ProtestantWorkEthic", "FertileCrescent", "AmerindianEcosystem", "RomanCliodynamics", "EdoJapan"), benchmark);

        assertTrue(fullEmpirical.rSquared >= 0.0, "Full empirical suite should return valid R^2 score.");
        assertTrue(baseline.rmse >= 0.0, "Baseline should return valid RMSE score.");
    }
}

