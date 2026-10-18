/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.benchmark;

import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.profiling.SimulationProfiler;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <h1>Comprehensive Real Planetary Simulation Engine Benchmark Suite</h1>
 * <p>
 * Evaluates genuine wall-clock performance of the complete {@link H3SimulationEngine}
 * coupled pipeline across spatial resolutions (Res 2, Res 3, Res 4, Res 5 Windowed),
 * temporal resolutions (Daily, Weekly, Monthly, Annual), and threading configurations
 * (Single-Core vs Multi-Core).
 * </p>
 * <p>
 * Strict IEEE-754 determinism ({@code strictDeterminism = true}) is enforced across
 * all runs. All 25 geoscientific map rasters, Tier 1 biophysical conservation PDEs,
 * demographic/urban/cultural DOD kernels, exact analytical Gini metrics, and
 * asynchronous rolling memory snapshot capture are fully executed.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ComprehensiveRealEngineBenchmarkTest {
    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveRealEngineBenchmarkTest.class);

    public record BenchmarkResult(
            String testLabel,
            int resolution,
            int cellCount,
            boolean windowed,
            double timeStepDays,
            int threads,
            int ticks,
            double durationSec,
            double tps,
            double msPerTick,
            double latencyUsPerCell,
            double cellThroughputPerSec,
            double extrapolated10kMinutes,
            long estimatedSnapshotBytes,
            long finalPopulation
    ) {}

    public static void main(String[] args) {
        new ComprehensiveRealEngineBenchmarkTest().runComprehensiveSuite();
    }

    @Test
    @DisplayName("Comprehensive Real Planetary Simulation Engine Empirical Benchmark")
    public void runComprehensiveSuite() {
        Locale.setDefault(Locale.US);
        int availableCores = Runtime.getRuntime().availableProcessors();

        System.out.println("====================================================================================================================");
        System.out.println("🌍 ETHER COMPREHENSIVE REAL PLANETARY ENGINE EMPIRICAL BENCHMARK SUITE");
        System.out.println("====================================================================================================================");
        System.out.printf("Hardware Architecture: %s (%s) | Available Processors: %d | JVM: %s (%s)\n",
                System.getProperty("os.name"), System.getProperty("os.arch"),
                availableCores, System.getProperty("java.version"), System.getProperty("java.vendor"));
        System.out.println("Determinism Mode: Strict IEEE-754 Bit-Level (strictDeterminism = true)");
        System.out.println("Snapshots: In-Memory LRU Rolling Buffer + Asynchronous Writer Worker");
        System.out.println("====================================================================================================================");

        List<BenchmarkResult> results = new ArrayList<>();

        // -------------------------------------------------------------------------------------------------
        // SECTION 1: SPATIAL GRID SCALING SWEEP (Monthly Δt = 30 days, Multi-Core)
        // -------------------------------------------------------------------------------------------------
        System.out.println("\n----------------------------------------------------------------------------------------------------");
        System.out.println("📍 SECTION 1: SPATIAL GRID SCALING SWEEP (Monthly Δt = 30 days, Parallel Multi-Core)");
        System.out.println("----------------------------------------------------------------------------------------------------");

        // 1.1 Res 2 Global Grid (~5,882 cells) - 20 ticks
        results.add(executeRun("Res 2 Global Macro", 2, false, 30.0, availableCores, 20));

        // 1.2 Res 3 Global Grid (~41,162 cells) - 15 ticks
        results.add(executeRun("Res 3 Global Standard", 3, false, 30.0, availableCores, 15));

        // 1.3 Res 5 Regional Windowed Grid (~40,305 cells) - 15 ticks
        results.add(executeRun("Res 5 Regional Windowed", 5, true, 30.0, availableCores, 15));

        // 1.4 Res 4 Global Grid (~288,122 cells) - 10 ticks
        results.add(executeRun("Res 4 Global High-Fi", 4, false, 30.0, availableCores, 10));

        // -------------------------------------------------------------------------------------------------
        // SECTION 2: TEMPORAL RESOLUTION SWEEP (Res 3 Global, Multi-Core)
        // -------------------------------------------------------------------------------------------------
        System.out.println("\n----------------------------------------------------------------------------------------------------");
        System.out.println("⏱️ SECTION 2: TEMPORAL STEP RESOLUTION SWEEP (Res 3 Global, Parallel Multi-Core)");
        System.out.println("----------------------------------------------------------------------------------------------------");

        // 2.1 Daily (Δt = 1 day) - 15 ticks
        results.add(executeRun("Res 3 Daily (Δt = 1d)", 3, false, 1.0, availableCores, 15));

        // 2.2 Weekly (Δt = 7 days) - 15 ticks
        results.add(executeRun("Res 3 Weekly (Δt = 7d)", 3, false, 7.0, availableCores, 15));

        // 2.3 Annual (Δt = 365 days) - 15 ticks
        results.add(executeRun("Res 3 Annual (Δt = 365d)", 3, false, 365.25, availableCores, 15));

        // -------------------------------------------------------------------------------------------------
        // SECTION 3: THREADING SCALING (Single-Core vs Multi-Core on Res 3)
        // -------------------------------------------------------------------------------------------------
        System.out.println("\n----------------------------------------------------------------------------------------------------");
        System.out.println("⚙️ SECTION 3: THREAD SCALING (Res 3 Global, Single-Core 1 Thread Baseline)");
        System.out.println("----------------------------------------------------------------------------------------------------");

        // 3.1 Single-Core (1 thread) - 10 ticks
        results.add(executeRun("Res 3 Single-Core (1 Thread)", 3, false, 30.0, 1, 10));

        // -------------------------------------------------------------------------------------------------
        // SYNTHESIS & REPORT PRINTING
        // -------------------------------------------------------------------------------------------------
        printSummaryReport(results, availableCores);

        for (BenchmarkResult r : results) {
            assertTrue(r.tps() > 0.0, "Throughput must be positive for " + r.testLabel());
            assertTrue(r.finalPopulation() > 0, "Simulation population must be physically non-zero for " + r.testLabel());
        }
    }

    private BenchmarkResult executeRun(String label, int resolution, boolean windowed, double stepDays, int threads, int ticks) {
        System.out.printf("\n>>> Setup: %-28s | Res %d | Windowed: %-5b | Δt: %5.1fd | Threads: %2d | Ticks: %d\n",
                label, resolution, windowed, stepDays, threads, ticks);

        long genStart = System.nanoTime();
        PlanetPreset preset = PlanetPreset.EARTH_LIKE.withResolution(resolution);
        List<H3Cell> generated = ProceduralGenerator.getInstance().generatePlanet(preset);

        List<H3Cell> cells;
        if (windowed) {
            // Mediterranean & Fertile Crescent window [25°N..45°N, -10°W..45°E]
            double minLat = 25.0, maxLat = 45.0, minLng = -10.0, maxLng = 45.0;
            cells = generated.stream()
                    .filter(c -> c.getLatitude() >= minLat && c.getLatitude() <= maxLat
                            && c.getLongitude() >= minLng && c.getLongitude() <= maxLng)
                    .collect(Collectors.toList());
        } else {
            cells = generated;
        }
        double genTimeSec = (System.nanoTime() - genStart) / 1e9;
        System.out.printf("    Grid Topology: %,d cells initialized in %.2f s.\n", cells.size(), genTimeSec);

        Scenario scenario = new Scenario();
        scenario.setPresetKey("bench_" + resolution + "_" + (int) stepDays + "_" + threads);
        scenario.setName(label);
        scenario.setStartDateYear(-10000);
        scenario.setEndDateYear(-5000);
        scenario.setH3Resolution(resolution);
        scenario.setTemporalResolutionDays(stepDays);
        scenario.setInitialHumanCount(100_000);
        scenario.setStrictDeterminism(true);
        scenario.setParallelExecutionEnabled(threads > 1);
        scenario.setParallelThreadCount(threads);

        SimulationPerformanceConfig perfConfig = scenario.toPerformanceConfig();
        perfConfig.setParallelThreadCount(threads);

        Configuration config;
        try {
            config = ConfigurationLoader.loadDefault();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }

        H3SimulationEngine engine = new H3SimulationEngine(config);
        engine.initializeFromScenario(scenario, cells);
        engine.setPerformanceConfig(perfConfig);

        if (engine.getHistoryManager() != null) {
            engine.getHistoryManager().configureTargetCadence(10_000, 24);
        }

        SimulationProfiler profiler = engine.getProfiler();
        profiler.reset();

        // Warm-up JIT (2 ticks)
        System.out.print("    Warming up JIT compiler (2 ticks)... ");
        engine.stepForward(2);
        System.out.println("ready.");

        profiler.reset();

        // Timed run
        System.out.printf("    Executing %,d timed planetary ticks...\n", ticks);
        long startNanos = System.nanoTime();
        engine.stepForward(ticks);
        long elapsedNanos = System.nanoTime() - startNanos;

        double elapsedSec = elapsedNanos / 1e9;
        double tps = ticks / elapsedSec;
        double msPerTick = (elapsedSec * 1000.0) / ticks;
        double latencyUsPerCell = (msPerTick * 1000.0) / cells.size();
        double cellThroughput = cells.size() * tps;
        double extrapolated10kMin = (msPerTick * 10_000.0) / (1000.0 * 60.0);
        long finalPop = engine.getTotalPopulation();

        // Estimate snapshot size
        long estimatedSnapBytes = 0;
        File runDir = new File("saves/RUN-" + scenario.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + "/snapshots");
        if (runDir.exists() && runDir.listFiles() != null) {
            File[] files = runDir.listFiles((dir, f) -> f.endsWith(".bin.gz"));
            if (files != null && files.length > 0) {
                estimatedSnapBytes = files[files.length - 1].length();
            }
        }
        if (estimatedSnapBytes == 0) {
            // Analytical estimate: ~85 bytes per cell compressed GZIP
            estimatedSnapBytes = (long) (cells.size() * 88L);
        }

        System.out.printf("    Measured: %6.2f s | %6.2f TPS | %7.1f ms/tick | Pop: %,d\n",
                elapsedSec, tps, msPerTick, finalPop);

        engine.shutdown();

        return new BenchmarkResult(
                label, resolution, cells.size(), windowed, stepDays, threads,
                ticks, elapsedSec, tps, msPerTick, latencyUsPerCell, cellThroughput,
                extrapolated10kMin, estimatedSnapBytes, finalPop
        );
    }

    private static void printSummaryReport(List<BenchmarkResult> results, int cores) {
        System.out.println("\n==============================================================================================================================================");
        System.out.println("📊 EMPIRICAL GROUND-TRUTH PLANETARY SIMULATION ENGINE BENCHMARK MATRIX");
        System.out.println("==============================================================================================================================================");
        System.out.printf("%-28s | %-5s | %-10s | %-5s | %-4s | %-5s | %-9s | %-10s | %-11s | %-13s | %-12s\n",
                "Scenario Configuration", "Grid", "Cells", "Δt", "Thrd", "Ticks", "TPS", "ms/Tick", "μs/Cell/Tick", "10k Ticks Est", "Snap (24x)");
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------");

        for (BenchmarkResult r : results) {
            String stepStr = r.timeStepDays() >= 365.0 ? "365d" : (r.timeStepDays() == 30.0 ? "30d" : (r.timeStepDays() == 7.0 ? "7d" : "1d"));
            String snapFootprintStr = String.format(Locale.US, "%.1f MB", (r.estimatedSnapshotBytes() * 24.0) / (1024.0 * 1024.0));
            String extrapolatedStr;
            if (r.extrapolated10kMinutes() >= 60.0) {
                extrapolatedStr = String.format(Locale.US, "%.1fh (%dmin)", r.extrapolated10kMinutes() / 60.0, (int) r.extrapolated10kMinutes());
            } else {
                extrapolatedStr = String.format(Locale.US, "%.1f min", r.extrapolated10kMinutes());
            }

            System.out.printf("%-28s | Res%-2d | %,10d | %-5s | %-4d | %-5d | %6.2f TPS | %7.1f ms | %8.2f μs | %-13s | %-12s\n",
                    r.testLabel(), r.resolution(), r.cellCount(), stepStr, r.threads(), r.ticks(),
                    r.tps(), r.msPerTick(), r.latencyUsPerCell(), extrapolatedStr, snapFootprintStr);
        }
        System.out.println("==============================================================================================================================================");
    }
}
