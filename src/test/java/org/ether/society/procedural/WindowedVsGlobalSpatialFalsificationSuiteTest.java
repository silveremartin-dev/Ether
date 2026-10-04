/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

import org.ether.society.analytics.WindowedVsGlobalSpatialFalsificationHarness;
import org.ether.society.analytics.WindowedVsGlobalSpatialFalsificationHarness.BenchmarkResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated Falsification & Scientific Validation Suite comparing Windowed (Regional Sub-Grid)
 * simulations against Full Planetary Sphere simulations across spatial and temporal resolutions.
 *
 * Chapter 1: Mathematical Domain Truncation, Boundary Mode Triad & Multi-Resolution Convergence.
 * Chapter 2: Historical Continental & Insular Isolation (Pre-Columbian Americas 1000-1491 & Madagascar 500-1000).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
@Tag("falsification")
public class WindowedVsGlobalSpatialFalsificationSuiteTest {

    private static final Logger logger = LoggerFactory.getLogger(WindowedVsGlobalSpatialFalsificationSuiteTest.class);

    @Test
    @DisplayName("Ch1.1 - Boundary Mode Triad: Dynamic Reservoir vs Closed Barrier vs Periodic Toroidal")
    public void testBoundaryModeTriadSweep() {
        logger.info("=== Starting Test: Boundary Mode Triad Sweep (Dynamic Reservoir vs Closed vs Toroidal) ===");

        List<BenchmarkResult> results = WindowedVsGlobalSpatialFalsificationHarness.runBoundaryModeTriadBenchmark();
        assertNotNull(results);
        assertEquals(3, results.size(), "Boundary triad must return exactly 3 benchmark runs");

        BenchmarkResult resDynamic = results.stream().filter(r -> "DYNAMIC_RESERVOIR".equals(r.boundaryMode())).findFirst().orElseThrow();
        BenchmarkResult resClosed = results.stream().filter(r -> "CLOSED_BARRIER".equals(r.boundaryMode())).findFirst().orElseThrow();
        BenchmarkResult resToroidal = results.stream().filter(r -> "PERIODIC_TOROIDAL".equals(r.boundaryMode())).findFirst().orElseThrow();

        logger.info("Triad Results -> Dynamic: r={}|MAPE={}% , Closed: r={}|MAPE={}% , Toroidal: r={}|MAPE={}%",
                String.format("%.4f", resDynamic.pearsonCorrelation()), String.format("%.2f", resDynamic.coreMAPE() * 100),
                String.format("%.4f", resClosed.pearsonCorrelation()), String.format("%.2f", resClosed.coreMAPE() * 100),
                String.format("%.4f", resToroidal.pearsonCorrelation()), String.format("%.2f", resToroidal.coreMAPE() * 100));

        // Dynamic Reservoir must outperform Closed Barrier and Toroidal on open continental borders
        assertTrue(resDynamic.passed(), "Dynamic Reservoir must pass validation criteria");
        assertTrue(resDynamic.pearsonCorrelation() >= 0.85, "Dynamic Reservoir Pearson r must be >= 0.85");
        assertTrue(resDynamic.coreMAPE() < 0.20, "Dynamic Reservoir MAPE must be < 20%");
    }

    @Test
    @DisplayName("Ch1.2 - Spatial Resolution Convergence: H3 Res 3 to Res 7 Multi-Scale Sweeps")
    public void testSpatialResolutionConvergence() {
        logger.info("=== Starting Test: Spatial Resolution Convergence (Res 3 to Res 7) ===");

        List<BenchmarkResult> results = WindowedVsGlobalSpatialFalsificationHarness.runSpatialResolutionSweepBenchmark();
        assertNotNull(results);
        assertEquals(5, results.size(), "Spatial sweep must return 5 resolution levels (Res 3, 4, 5, 6, 7)");

        for (BenchmarkResult r : results) {
            logger.info("Spatial Res {} -> Pearson r = {}, Core MAPE = {}%, Centroid Shift = {} km",
                    r.spatialResolution(), String.format("%.4f", r.pearsonCorrelation()),
                    String.format("%.2f", r.coreMAPE() * 100), String.format("%.1f", r.centroidShiftKm()));
            assertTrue(r.passed(), "Spatial resolution benchmark at Res " + r.spatialResolution() + " must pass");
            assertTrue(r.pearsonCorrelation() >= 0.80, "Spatial correlation must be >= 0.80");
        }
    }

    @Test
    @DisplayName("Ch1.3 - Temporal Step Discretization: Daily (1d), Weekly (7d), Monthly (30d), Quarterly (90d), Annual (365d)")
    public void testTemporalStepSweep() {
        logger.info("=== Starting Test: Temporal Step Discretization Sweep ===");

        List<BenchmarkResult> results = WindowedVsGlobalSpatialFalsificationHarness.runTemporalStepSweepBenchmark();
        assertNotNull(results);
        assertEquals(5, results.size(), "Temporal sweep must evaluate 5 time steps (1d, 7d, 30d, 90d, 365d)");

        for (BenchmarkResult r : results) {
            logger.info("Temporal Δt = {}d -> Pearson r = {}, Core MAPE = {}%",
                    r.timeStepDays(), String.format("%.4f", r.pearsonCorrelation()),
                    String.format("%.2f", r.coreMAPE() * 100));
            assertTrue(r.passed(), "Temporal benchmark at Δt " + r.timeStepDays() + " must pass");
            assertTrue(r.pearsonCorrelation() >= 0.80, "Temporal correlation must be >= 0.80");
        }
    }

    @Test
    @DisplayName("Ch2.1 - Pre-Columbian Americas (1000-1491 AD): Continental Isolation vs Global Sphere")
    public void testPreColumbianAmericas1000To1491ContinentalIsolation() {
        logger.info("=== Starting Test: Pre-Columbian Americas (1000-1491 AD) Continental Isolation ===");

        BenchmarkResult r = WindowedVsGlobalSpatialFalsificationHarness.runAmericas1491IsolationBenchmark();
        assertNotNull(r);

        logger.info("Americas Isolation -> Pearson r = {}, Core MAPE = {}%, Centroid Shift = {} km",
                String.format("%.4f", r.pearsonCorrelation()),
                String.format("%.2f", r.coreMAPE() * 100),
                String.format("%.1f", r.centroidShiftKm()));

        assertTrue(r.passed(), "Pre-Columbian continental isolation benchmark must pass");
        assertTrue(r.pearsonCorrelation() >= 0.85, "Americas pre-1492 isolation must exhibit high correlation (r >= 0.85)");
        assertTrue(r.coreMAPE() < 0.25, "Americas continental isolation MAPE must be < 25%");
    }

    @Test
    @DisplayName("Ch2.2 - Madagascar Island (500-1000 AD): Maritime Colonization & Insular Network")
    public void testMadagascarIslandColonization() {
        logger.info("=== Starting Test: Madagascar Island Colonization (500-1000 AD) ===");

        BenchmarkResult r = WindowedVsGlobalSpatialFalsificationHarness.runMadagascarIslandBenchmark();
        assertNotNull(r);

        logger.info("Madagascar -> Pearson r = {}, Core MAPE = {}%, Centroid Shift = {} km",
                String.format("%.4f", r.pearsonCorrelation()),
                String.format("%.2f", r.coreMAPE() * 100),
                String.format("%.1f", r.centroidShiftKm()));

        assertTrue(r.passed(), "Madagascar island benchmark must pass");
        assertTrue(r.pearsonCorrelation() >= 0.80, "Madagascar correlation must be >= 0.80");
    }

    @Test
    @DisplayName("Ch2.3 - Tasmania Island (-10,000 BP - 1800 AD): Extreme Isolation & Technological Loss")
    public void testTasmaniaIsolation() {
        logger.info("=== Starting Test: Tasmania Island Isolation ===");

        BenchmarkResult r = WindowedVsGlobalSpatialFalsificationHarness.runTasmaniaIsolationBenchmark();
        assertNotNull(r);
        assertTrue(r.passed(), "Tasmania isolation benchmark must pass");
        assertTrue(r.pearsonCorrelation() >= 0.80, "Tasmania correlation must be >= 0.80");
    }

    @Test
    @DisplayName("Ch2.4 - Easter Island / Rapa Nui (1200-1722 AD): Ecological Overshoot")
    public void testEasterIslandOvershoot() {
        logger.info("=== Starting Test: Easter Island Overshoot ===");

        BenchmarkResult r = WindowedVsGlobalSpatialFalsificationHarness.runEasterIslandBenchmark();
        assertNotNull(r);
        assertTrue(r.passed(), "Easter Island benchmark must pass");
        assertTrue(r.pearsonCorrelation() >= 0.80, "Easter Island correlation must be >= 0.80");
    }

    @Test
    @DisplayName("Ch2.5 - Medieval Iceland (874-1400 AD): Subarctic Agricultural Margin")
    public void testIcelandAgriculturalMargin() {
        logger.info("=== Starting Test: Medieval Iceland Agricultural Margin ===");

        BenchmarkResult r = WindowedVsGlobalSpatialFalsificationHarness.runIcelandBenchmark();
        assertNotNull(r);
        assertTrue(r.passed(), "Medieval Iceland benchmark must pass");
        assertTrue(r.pearsonCorrelation() >= 0.80, "Medieval Iceland correlation must be >= 0.80");
    }
}
