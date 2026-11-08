/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.data.HistoricalMapGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.ether.society.generation.PlanetPreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Scientific Falsification & Validation Harness: Windowed Sub-Grids vs. Full Planetary Sphere.
 *
 * <p>Executes multi-resolution spatial sweeps (H3 Res 3 to Res 7), temporal integration sweeps
 * (1d, 7d, 30d, 90d, 365d), boundary mode comparisons (DYNAMIC_RESERVOIR vs. CLOSED_BARRIER vs.
 * PERIODIC_TOROIDAL), and historical isolation case studies (Pre-Columbian Americas 1000-1491 AD,
 * Madagascar 500-1000 AD, Tasmania -10k BP, Easter Island 1200 AD, Iceland 880 AD, and Tokugawa Japan 1639 AD).</p>
 *
 * <p>Strictly adheres to AGENTS.md directives: empirical planetary rasters define physical ground truth,
 * biophysical states derive from closed-form models, and zero arbitrary human counts are injected.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class WindowedVsGlobalSpatialFalsificationHarness {

    private static final Logger logger = LoggerFactory.getLogger(WindowedVsGlobalSpatialFalsificationHarness.class);

    public record BenchmarkResult(
            String scenarioName,
            String benchmarkCategory,
            String boundaryMode,
            int spatialResolution,
            double timeStepDays,
            double pearsonCorrelation,
            double coreMAPE,
            double centroidShiftKm,
            double boundaryReflectionIndex,
            boolean passed,
            String diagnostics
    ) {}

    /**
     * Main CLI entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        logger.info("=========================================================================================");
        logger.info("  ETHER: WINDOWED VS. GLOBAL SPATIAL FALSIFICATION & MULTI-RESOLUTION HARNESS           ");
        logger.info("=========================================================================================");

        List<BenchmarkResult> results = runFullValidationCampaign();

        logger.info("=========================================================================================");
        logger.info("                               CAMPAIGN EXECUTION SUMMARY                                ");
        logger.info("=========================================================================================");
        int passCount = 0;
        for (BenchmarkResult r : results) {
            String status = r.passed() ? "✅ PASSED" : "❌ FAILED";
            logger.info(String.format("• %-36s | %-16s | Res %d | Δt %3.0fd | r: %6.4f | MAPE: %5.2f%% | ΔR: %5.1f km | %s",
                    r.scenarioName(), r.boundaryMode(), r.spatialResolution(), r.timeStepDays(),
                    r.pearsonCorrelation(), r.coreMAPE() * 100, r.centroidShiftKm(), status));
            if (r.passed()) passCount++;
        }
        logger.info("-----------------------------------------------------------------------------------------");
        logger.info(String.format("Final Benchmark Score: %d / %d tests passed (%.1f%%)",
                passCount, results.size(), (double) passCount / Math.max(1, results.size()) * 100.0));
        logger.info("=========================================================================================");

        try {
            File reportDir = new File("logs/windowed_validation");
            if (!reportDir.exists()) reportDir.mkdirs();
            File reportFile = new File(reportDir, "windowed_vs_global_academic_report.md");
            try (FileWriter fw = new FileWriter(reportFile, StandardCharsets.UTF_8)) {
                fw.write(generateAcademicMarkdownReport(results));
            }
            logger.info("Academic Markdown Report written to: {}", reportFile.getAbsolutePath());
        } catch (Exception e) {
            logger.warn("Could not save markdown report file: {}", e.getMessage());
        }
    }

    /**
     * Run full validation campaign.
     *
     * @return the list of benchmark results
     */
    public static List<BenchmarkResult> runFullValidationCampaign() {
        List<BenchmarkResult> results = new ArrayList<>();

        // ── 1. Boundary Mode Triad Sweep (Fertile Crescent / East-Med -8000 to -7970 BP) ──
        logger.info(">>> Running Benchmark 1: Boundary Mode Comparison Triad (Reservoir vs Closed vs Toroidal)...");
        results.addAll(runBoundaryModeTriadBenchmark());

        // ── 2. Spatial Resolution Sweep (H3 Res 3 to Res 6 Global/Window, and Res 7 Regional Window) ──
        logger.info(">>> Running Benchmark 2: Multi-Resolution Spatial Convergence Sweep (Res 3 to 7)...");
        results.addAll(runSpatialResolutionSweepBenchmark());

        // ── 3. Temporal Resolution Sweep (Daily 1d, Weekly 7d, Monthly 30d, Quarterly 90d, Annual 365d) ──
        logger.info(">>> Running Benchmark 3: Temporal Step Discretization Sweep (1d to 365d)...");
        results.addAll(runTemporalStepSweepBenchmark());

        // ── 4. Pre-Columbian Americas (1000 AD to 1491 AD) Continental Isolation ──
        logger.info(">>> Running Benchmark 4: Pre-Columbian Americas (1000-1491 AD) Continental Isolation...");
        results.add(runAmericas1491IsolationBenchmark());

        // ── 5. Madagascar Island (500 AD to 1000 AD) Maritime vs Insular Isolation ──
        logger.info(">>> Running Benchmark 5: Madagascar Island (500-1000 AD) Maritime Network Isolation...");
        results.add(runMadagascarIslandBenchmark());

        // ── 6. Tasmania Island (-10,000 BP to 1800 AD) Extreme Isolation & Technological Threshold ──
        logger.info(">>> Running Benchmark 6: Tasmania Insular Isolation & Tech Dynamics...");
        results.add(runTasmaniaIsolationBenchmark());

        // ── 7. Easter Island / Rapa Nui (1200 AD to 1722 AD) Ecological Carrying Capacity Overshoot ──
        logger.info(">>> Running Benchmark 7: Easter Island (Rapa Nui) Ecological Overshoot...");
        results.add(runEasterIslandBenchmark());

        // ── 8. Medieval Iceland (880 AD to 1400 AD) Subarctic Agricultural Margin & Maritime Connectivity ──
        logger.info(">>> Running Benchmark 8: Medieval Iceland Subarctic Margin & Connectivity...");
        results.add(runIcelandBenchmark());

        // ── 9. Tokugawa Japan (1639 AD to 1669 AD) Insular Archipelago Sakoku Isolation ──
        logger.info(">>> Running Benchmark 9: Tokugawa Japan Sakoku Maritime Isolation...");
        results.add(runTokugawaSakoku1639IsolationBenchmark());

        return results;
    }

    public record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {}

    public static BoundingBox findDenseLandBoundingBox(Scenario s, double spanLat, double spanLng) {
        var b = SpatialWindowBoundingBoxFinder.findDenseLandBoundingBox(s, spanLat, spanLng);
        return new BoundingBox(b.minLat(), b.maxLat(), b.minLng(), b.maxLng());
    }

    public static BoundingBox findIsolatedIslandBoundingBox(Scenario s, double spanLat, double spanLng) {
        var b = SpatialWindowBoundingBoxFinder.findIsolatedIslandBoundingBox(s, spanLat, spanLng);
        return new BoundingBox(b.minLat(), b.maxLat(), b.minLng(), b.maxLng());
    }

    // =========================================================================
    // BENCHMARK 1: BOUNDARY MODE COMPARISON TRIAD
    // =========================================================================

    /**
     * Executes the Boundary Mode Comparison Triad on the Neolithic Fertile Crescent (-8000 to -7970 BP).
     *
     * @return 3 benchmark results: DYNAMIC_RESERVOIR, CLOSED_BARRIER, PERIODIC_TOROIDAL
     */
    public static List<BenchmarkResult> runBoundaryModeTriadBenchmark() {
        List<BenchmarkResult> list = new ArrayList<>();

        Scenario base = new Scenario();
        base.setName("EastMed_Neolithic_Base");
        base.setStartDateYear(-8000);
        base.setEndDateYear(-7970);
        base.setPlanetPreset(PlanetPreset.EARTH_LIKE.withResolution(4));
        base.setH3Resolution(4);
        base.setTemporalResolutionDays(30.0);
        base.setSeed(424242L);
        HistoricalMapGenerator.populateScenarioHistoricalMaps(base);

        // Fertile Crescent / Eastern Mediterranean window
        double minLat = 28.0, maxLat = 42.0;
        double minLng = 26.0, maxLng = 48.0;

        // Ground truth: Global Sphere
        Scenario global = cloneScenario(base);
        global.setName("EastMed_Global_Sphere");
        global.setClippingEnabled(false);
        SimulationRunRecord recGlobal = HeadlessBatchRunner.executeScenarioHeadless(global);
        int finalYear = (int) base.getEndDateYear();
        List<H3Cell> gCells = recGlobal.getSpatialSnapshotAt(finalYear);

        Map<Long, Double> gMap = extractPopDensityMap(gCells, minLat, maxLat, minLng, maxLng);

        // Test A: DYNAMIC_RESERVOIR (Quadratic Sponge Layer)
        Scenario winReservoir = cloneScenario(base);
        winReservoir.setName("EastMed_Window_Reservoir");
        winReservoir.setClippingEnabled(true);
        winReservoir.setMinLat(minLat); winReservoir.setMaxLat(maxLat);
        winReservoir.setMinLng(minLng); winReservoir.setMaxLng(maxLng);
        winReservoir.setBoundaryMode("DYNAMIC_RESERVOIR");
        SimulationRunRecord recReservoir = HeadlessBatchRunner.executeScenarioHeadless(winReservoir);
        List<H3Cell> wCellsRes = recReservoir.getSpatialSnapshotAt(finalYear);
        Map<Long, Double> wMapRes = extractPopDensityMap(wCellsRes, minLat, maxLat, minLng, maxLng);

        double pRes = computeSpatialPearsonCorrelation(gMap, wMapRes);
        double mapeRes = computeCoreMAPE(gMap, wMapRes);
        double shiftRes = computeCentroidShiftKm(gMap, wMapRes, wCellsRes);
        double reflRes = computeBoundaryReflectionIndex(gMap, wMapRes, wCellsRes);
        boolean passRes = pRes >= 0.85 && mapeRes < 0.25;
        list.add(new BenchmarkResult("Eastern Mediterranean Neolithic", "Boundary Regime", "DYNAMIC_RESERVOIR", 4, 30.0,
                pRes, mapeRes, shiftRes, reflRes, passRes, "Sponge layer smoothly absorbs outbound flux"));

        // Test B: CLOSED_BARRIER (Reflective Neumann)
        Scenario winClosed = cloneScenario(winReservoir);
        winClosed.setName("EastMed_Window_Closed");
        winClosed.setBoundaryMode("CLOSED_BARRIER");
        SimulationRunRecord recClosed = HeadlessBatchRunner.executeScenarioHeadless(winClosed);
        List<H3Cell> wCellsClosed = recClosed.getSpatialSnapshotAt(finalYear);
        Map<Long, Double> wMapClosed = extractPopDensityMap(wCellsClosed, minLat, maxLat, minLng, maxLng);

        double pClosed = computeSpatialPearsonCorrelation(gMap, wMapClosed);
        double mapeClosed = computeCoreMAPE(gMap, wMapClosed);
        double shiftClosed = computeCentroidShiftKm(gMap, wMapClosed, wCellsClosed);
        double reflClosed = computeBoundaryReflectionIndex(gMap, wMapClosed, wCellsClosed);
        boolean passClosed = pClosed >= 0.70 && mapeClosed < 0.25;
        list.add(new BenchmarkResult("Eastern Mediterranean Neolithic", "Boundary Regime", "CLOSED_BARRIER", 4, 30.0,
                pClosed, mapeClosed, shiftClosed, reflClosed, passClosed, "Reflective boundary induces marginal boundary pileup"));

        // Test C: PERIODIC_TOROIDAL (Periodic Wrap-Around)
        Scenario winToroidal = cloneScenario(winReservoir);
        winToroidal.setName("EastMed_Window_Toroidal");
        winToroidal.setBoundaryMode("PERIODIC_TOROIDAL");
        SimulationRunRecord recToroidal = HeadlessBatchRunner.executeScenarioHeadless(winToroidal);
        List<H3Cell> wCellsTor = recToroidal.getSpatialSnapshotAt(finalYear);
        Map<Long, Double> wMapTor = extractPopDensityMap(wCellsTor, minLat, maxLat, minLng, maxLng);

        double pTor = computeSpatialPearsonCorrelation(gMap, wMapTor);
        double mapeTor = computeCoreMAPE(gMap, wMapTor);
        double shiftTor = computeCentroidShiftKm(gMap, wMapTor, wCellsTor);
        double reflTor = computeBoundaryReflectionIndex(gMap, wMapTor, wCellsTor);
        boolean passTor = pTor >= 0.60 && mapeTor < 0.25;
        list.add(new BenchmarkResult("Eastern Mediterranean Neolithic", "Boundary Regime", "PERIODIC_TOROIDAL", 4, 30.0,
                pTor, mapeTor, shiftTor, reflTor, passTor, "Toroidal wrap reinjects migrants on opposite edge"));

        recGlobal.getSpatialSnapshots().clear();
        recReservoir.getSpatialSnapshots().clear();
        recClosed.getSpatialSnapshots().clear();
        recToroidal.getSpatialSnapshots().clear();

        return list;
    }

    // =========================================================================
    // BENCHMARK 2: SPATIAL RESOLUTION SWEEP (H3 Res 3 to Res 7)
    // =========================================================================

    /**
     * Executes the Spatial Resolution Convergence Sweep from H3 Res 3 to Res 7.
     *
     * @return list of 5 benchmark results
     */
    public static List<BenchmarkResult> runSpatialResolutionSweepBenchmark() {
        List<BenchmarkResult> list = new ArrayList<>();

        double minLat = 28.0, maxLat = 42.0;
        double minLng = 26.0, maxLng = 48.0;

        // Focused regional window around Levant / Nile Delta for High-Res 5, 6, 7 comparison
        double rMinLat = 30.0, rMaxLat = 34.0;
        double rMinLng = 31.0, rMaxLng = 35.0;
        Map<Long, Double> globalRes4LevantMap = new java.util.HashMap<>();
        List<H3Cell> globalRes4Cells = new ArrayList<>();

        // Test Res 3, 4 with Global Sphere vs Window
        for (int res : new int[] { 3, 4 }) {
            Scenario base = new Scenario();
            base.setName("Spatial_Res_" + res + "_Base");
            base.setStartDateYear(-5000);
            base.setEndDateYear(-4995);
            base.setPlanetPreset(PlanetPreset.EARTH_LIKE.withResolution(res));
            base.setH3Resolution(res);
            base.setTemporalResolutionDays(30.0);
            base.setSeed(1000L + res);
            HistoricalMapGenerator.populateScenarioHistoricalMaps(base);

            Scenario global = cloneScenario(base);
            global.setName("Spatial_Global_Res_" + res);
            global.setClippingEnabled(false);
            SimulationRunRecord recGlobal = HeadlessBatchRunner.executeScenarioHeadless(global);

            Scenario win = cloneScenario(base);
            win.setName("Spatial_Win_Res_" + res);
            win.setClippingEnabled(true);
            win.setMinLat(minLat); win.setMaxLat(maxLat);
            win.setMinLng(minLng); win.setMaxLng(maxLng);
            win.setBoundaryMode("DYNAMIC_RESERVOIR");
            SimulationRunRecord recWin = HeadlessBatchRunner.executeScenarioHeadless(win);

            int finalYear = (int) base.getEndDateYear();
            List<H3Cell> gCells = recGlobal.getSpatialSnapshotAt(finalYear);
            List<H3Cell> wCells = recWin.getSpatialSnapshotAt(finalYear);

            if (res == 4 && gCells != null) {
                globalRes4LevantMap = extractPopDensityMap(gCells, rMinLat, rMaxLat, rMinLng, rMaxLng);
                globalRes4Cells = new ArrayList<>(gCells);
            }

            Map<Long, Double> gMap = extractPopDensityMap(gCells, minLat, maxLat, minLng, maxLng);
            Map<Long, Double> wMap = extractPopDensityMap(wCells, minLat, maxLat, minLng, maxLng);

            double p = computeSpatialPearsonCorrelation(gMap, wMap);
            double mape = computeCoreMAPE(gMap, wMap);
            double shift = computeCentroidShiftKm(gMap, wMap, wCells);
            double refl = computeBoundaryReflectionIndex(gMap, wMap, wCells);
            boolean pass = p >= 0.80 && mape < 0.25;

            recGlobal.getSpatialSnapshots().clear();
            recWin.getSpatialSnapshots().clear();

            list.add(new BenchmarkResult("Spatial Resolution Scaling", "Spatial Resolution", "DYNAMIC_RESERVOIR", res, 30.0,
                    p, mape, shift, refl, pass, String.format("H3 Res %d evaluated (global vs windowed sub-grid)", res)));
        }

        // Test High-Res 5, 6, 7 (Windowed High-Resolution Execution compared against Global Res 4 parent cells)
        com.uber.h3core.H3Core h3Core = null;
        try {
            h3Core = com.uber.h3core.H3Core.newInstance();
        } catch (Exception ignored) {
        }

        for (int res : new int[] { 5, 6, 7 }) {
            Scenario winHigh = new Scenario();
            winHigh.setName("Spatial_Win_HighRes_" + res);
            winHigh.setStartDateYear(-5000);
            winHigh.setEndDateYear(-4995);
            winHigh.setPlanetPreset(PlanetPreset.EARTH_LIKE.withResolution(res));
            winHigh.setH3Resolution(res);
            winHigh.setTemporalResolutionDays(30.0);
            winHigh.setSeed(2000L + res);
            HistoricalMapGenerator.populateScenarioHistoricalMaps(winHigh);

            winHigh.setClippingEnabled(true);
            winHigh.setMinLat(rMinLat); winHigh.setMaxLat(rMaxLat);
            winHigh.setMinLng(rMinLng); winHigh.setMaxLng(rMaxLng);
            winHigh.setBoundaryMode("DYNAMIC_RESERVOIR");

            SimulationRunRecord recHigh = HeadlessBatchRunner.executeScenarioHeadless(winHigh);
            int finalYear = (int) winHigh.getEndDateYear();
            List<H3Cell> wCells = recHigh.getSpatialSnapshotAt(finalYear);

            // Aggregate High-Res child cells to their Resolution 4 parent H3 hexagons for genuine comparison against Global Res 4
            Map<Long, Double> wAggregatedToRes4 = new java.util.HashMap<>();
            if (wCells != null && h3Core != null) {
                for (H3Cell c : wCells) {
                    if (c.getLatitude() >= rMinLat && c.getLatitude() <= rMaxLat &&
                        c.getLongitude() >= rMinLng && c.getLongitude() <= rMaxLng) {
                        long parentRes4 = h3Core.cellToParent(c.getH3Index(), 4);
                        double pop = c.getPopulation() != null ? c.getPopulation().doubleValue() : 0.0;
                        wAggregatedToRes4.merge(parentRes4, pop, Double::sum);
                    }
                }
            }

            double p = computeSpatialPearsonCorrelation(globalRes4LevantMap, wAggregatedToRes4);
            double mape = computeCoreMAPE(globalRes4LevantMap, wAggregatedToRes4);
            double shift = computeCentroidShiftKm(globalRes4LevantMap, wAggregatedToRes4, globalRes4Cells);
            double refl = computeBoundaryReflectionIndex(globalRes4LevantMap, wAggregatedToRes4, globalRes4Cells);
            boolean pass = wCells != null && !wCells.isEmpty() && p >= 0.75;

            recHigh.getSpatialSnapshots().clear();

            list.add(new BenchmarkResult("Spatial Resolution Scaling", "Spatial Resolution (High-Res)", "DYNAMIC_RESERVOIR", res, 30.0,
                    p, mape, shift, refl, pass, String.format("H3 Res %d regional simulation executed across %d cells vs Global Res 4 parent mesh", res, wCells != null ? wCells.size() : 0)));
        }

        return list;
    }

    // =========================================================================
    // BENCHMARK 3: TEMPORAL STEP SWEEP (1d, 7d, 30d, 90d, 365d)
    // =========================================================================

    /**
     * Executes the Temporal Step Discretization Sweep across 1d, 7d, 30d, 90d, and 365d.
     *
     * @return list of 5 benchmark results
     */
    public static List<BenchmarkResult> runTemporalStepSweepBenchmark() {
        List<BenchmarkResult> list = new ArrayList<>();

        // Baseline reference: Monthly Δt = 30d at Resolution 4
        Scenario refSc = new Scenario();
        refSc.setName("Temporal_Ref_30d");
        refSc.setStartDateYear(-2000);
        refSc.setEndDateYear(-1995);
        refSc.setPlanetPreset(PlanetPreset.EARTH_LIKE.withResolution(4));
        refSc.setH3Resolution(4);
        refSc.setTemporalResolutionDays(30.0);
        refSc.setSeed(9876L);
        HistoricalMapGenerator.populateScenarioHistoricalMaps(refSc);

        double minLat = 28.0, maxLat = 42.0;
        double minLng = 26.0, maxLng = 48.0;

        refSc.setClippingEnabled(true);
        refSc.setMinLat(minLat); refSc.setMaxLat(maxLat);
        refSc.setMinLng(minLng); refSc.setMaxLng(maxLng);
        refSc.setBoundaryMode("DYNAMIC_RESERVOIR");

        SimulationRunRecord recRef = HeadlessBatchRunner.executeScenarioHeadless(refSc);
        int finalYear = (int) refSc.getEndDateYear();
        List<H3Cell> refCells = recRef.getSpatialSnapshotAt(finalYear);
        Map<Long, Double> refMap = extractPopDensityMap(refCells, minLat, maxLat, minLng, maxLng);

        for (double dt : new double[] { 1.0, 7.0, 30.0, 90.0, 365.25 }) {
            Scenario testSc = cloneScenario(refSc);
            testSc.setName("Temporal_Test_" + (int) dt + "d");
            testSc.setTemporalResolutionDays(dt);

            SimulationRunRecord recTest = HeadlessBatchRunner.executeScenarioHeadless(testSc);
            List<H3Cell> testCells = recTest.getSpatialSnapshotAt(finalYear);
            Map<Long, Double> testMap = extractPopDensityMap(testCells, minLat, maxLat, minLng, maxLng);

            double p = computeSpatialPearsonCorrelation(refMap, testMap);
            double mape = computeCoreMAPE(refMap, testMap);
            double shift = computeCentroidShiftKm(refMap, testMap, testCells);
            double refl = computeBoundaryReflectionIndex(refMap, testMap, testCells);
            boolean pass = p >= 0.85 && mape < 0.30;

            recTest.getSpatialSnapshots().clear();

            String label = dt <= 1.0 ? "Daily (1d)" : (dt <= 7.0 ? "Weekly (7d)" : (dt <= 30.0 ? "Monthly (30d)" : (dt <= 90.0 ? "Quarterly (90d)" : "Annual (365d)")));
            list.add(new BenchmarkResult("Temporal Step Sensitivity", "Temporal Discretization", "DYNAMIC_RESERVOIR", 4, dt,
                    p, mape, shift, refl, pass, String.format("Time step %s vs Monthly baseline", label)));
        }
        recRef.getSpatialSnapshots().clear();
        return list;
    }

    // =========================================================================
    // HISTORICAL BENCHMARKS (DELEGATED TO HistoricalSpatialIsolationBenchmarks)
    // =========================================================================

    public static BenchmarkResult runAmericas1491IsolationBenchmark() {
        return HistoricalSpatialIsolationBenchmarks.runAmericas1491IsolationBenchmark();
    }

    public static BenchmarkResult runMadagascarIslandBenchmark() {
        return HistoricalSpatialIsolationBenchmarks.runMadagascarIslandBenchmark();
    }

    public static BenchmarkResult runTasmaniaIsolationBenchmark() {
        return HistoricalSpatialIsolationBenchmarks.runTasmaniaIsolationBenchmark();
    }

    public static BenchmarkResult runEasterIslandBenchmark() {
        return HistoricalSpatialIsolationBenchmarks.runEasterIslandBenchmark();
    }

    public static BenchmarkResult runIcelandBenchmark() {
        return HistoricalSpatialIsolationBenchmarks.runIcelandBenchmark();
    }

    public static BenchmarkResult runTokugawaSakoku1639IsolationBenchmark() {
        return HistoricalSpatialIsolationBenchmarks.runTokugawaSakoku1639IsolationBenchmark();
    }

    // =========================================================================
    // SPATIAL MATHEMATICS & STATISTICAL METRICS
    // =========================================================================

    public static Map<Long, Double> extractPopDensityMap(List<H3Cell> cells, double minLat, double maxLat, double minLng, double maxLng) {
        return SpatialFalsificationMetrics.extractPopDensityMap(cells, minLat, maxLat, minLng, maxLng);
    }

    public static long extractPopulationInBBox(SimulationRunRecord rec, int year, double minLat, double maxLat, double minLng, double maxLng) {
        return SpatialFalsificationMetrics.extractPopulationInBBox(rec, year, minLat, maxLat, minLng, maxLng);
    }

    public static double computeSpatialPearsonCorrelation(Map<Long, Double> mapA, Map<Long, Double> mapB) {
        return SpatialFalsificationMetrics.computeSpatialPearsonCorrelation(mapA, mapB);
    }

    public static double computeCoreMAPE(Map<Long, Double> mapGlobal, Map<Long, Double> mapWindow) {
        return SpatialFalsificationMetrics.computeCoreMAPE(mapGlobal, mapWindow);
    }

    public static double computeCentroidShiftKm(Map<Long, Double> mapGlobal, Map<Long, Double> mapWindow, List<H3Cell> cellsRef) {
        return SpatialFalsificationMetrics.computeCentroidShiftKm(mapGlobal, mapWindow, cellsRef);
    }

    public static double computeBoundaryReflectionIndex(Map<Long, Double> mapGlobal, Map<Long, Double> mapWindow, List<H3Cell> cellsRef) {
        return SpatialFalsificationMetrics.computeBoundaryReflectionIndex(mapGlobal, mapWindow, cellsRef);
    }

    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2, double radiusKm) {
        return SpatialFalsificationMetrics.calculateHaversineDistance(lat1, lon1, lat2, lon2, radiusKm);
    }

    public static Scenario cloneScenario(Scenario s) {
        return SpatialFalsificationMetrics.cloneScenario(s);
    }

    private static String generateAcademicMarkdownReport(List<BenchmarkResult> results) {
        return SpatialFalsificationReportGenerator.generateAcademicMarkdownReport(results);
    }
}
