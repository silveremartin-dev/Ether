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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <h1>[INITIAL PROTOTYPE] Real Engine Multi-Resolution Benchmark Suite</h1>
 * <p>
 * Early prototype measuring Res 3, Res 4, Res 5 Windowed.
 * </p>
 *
 * @deprecated Superseded by {@link ComprehensiveRealEngineBenchmarkTest} covering all 5 scientific axes.
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
@Deprecated
@org.junit.jupiter.api.Tag("benchmark")
public class RealEngineBenchmarkTest {
    private static final Logger logger = LoggerFactory.getLogger(RealEngineBenchmarkTest.class);

    public record RealBenchmarkResult(
            String testName,
            int resolution,
            int cellCount,
            int ticks,
            double durationSec,
            double tps,
            double msPerTick,
            long finalPopulation
    ) {}

    @Test
    @DisplayName("Evaluate Real Engine Performance on H3 Res 3, Res 4, and Res 5")
    public void runRealEngineBenchmarks() {
        System.out.println("=========================================================================================");
        System.out.println("🚀 ETHER REAL ENGINE END-TO-END MULTI-RESOLUTION EMPIRICAL BENCHMARK");
        System.out.println("=========================================================================================");
        System.out.printf("Available Cores: %d | JVM: %s\n", Runtime.getRuntime().availableProcessors(), System.getProperty("java.version"));
        System.out.println("-----------------------------------------------------------------------------------------");

        List<RealBenchmarkResult> results = new ArrayList<>();

        // Test 1: Full Planetary Grid at Res 3 (~41,162 cells)
        results.add(benchmarkScenario("Earth Res 3 (Full Grid)", 3, false, 24));

        // Test 2: Full Planetary Grid at Res 4 (~288,122 cells)
        results.add(benchmarkScenario("Earth Res 4 (Full Grid)", 4, false, 24));

        // Test 3: Regional Windowed Grid at Res 5 (Mediterranean/Fertile Crescent window)
        results.add(benchmarkScenario("Regional Res 5 (Windowed)", 5, true, 24));

        // Print final summary table
        System.out.println("\n=========================================================================================");
        System.out.println("📊 REAL ENGINE EMPIRICAL BENCHMARK SUMMARY (ACTUAL WALL-CLOCK MEASUREMENT)");
        System.out.println("=========================================================================================");
        System.out.printf("%-26s | %-6s | %-10s | %-6s | %-10s | %-10s | %-12s | %-12s\n",
                "Scenario Configuration", "Res", "Cells", "Ticks", "Duration", "TPS", "ms / Tick", "Final Pop");
        System.out.println("-----------------------------------------------------------------------------------------------------------------");
        for (RealBenchmarkResult r : results) {
            System.out.printf("%-26s | Res %-2d | %,10d | %-6d | %8.2f s | %7.2f TPS | %9.1f ms | %,12d\n",
                    r.testName(), r.resolution(), r.cellCount(), r.ticks(), r.durationSec(), r.tps(), r.msPerTick(), r.finalPopulation());
        }
        System.out.println("=================================================================================================================");

        for (RealBenchmarkResult r : results) {
            assertTrue(r.tps() > 0.0, "TPS must be positive");
            assertTrue(r.finalPopulation() > 0, "Population must be preserved");
        }
    }

    private RealBenchmarkResult benchmarkScenario(String name, int resolution, boolean windowed, int ticks) {
        System.out.printf("\n>>> Initializing %s (H3 Res %d, Windowed=%b)...\n", name, resolution, windowed);

        // 1. Generate grid
        long genStart = System.nanoTime();
        PlanetPreset preset = PlanetPreset.EARTH_LIKE.withResolution(resolution);
        List<H3Cell> generated = ProceduralGenerator.getInstance().generatePlanet(preset);

        List<H3Cell> cells;
        if (windowed) {
            // Mediterranean / Fertile Crescent window [25°N..45°N, -10°W..45°E]
            double minLat = 25.0, maxLat = 45.0, minLng = -10.0, maxLng = 45.0;
            cells = generated.stream()
                    .filter(c -> c.getLatitude() >= minLat && c.getLatitude() <= maxLat
                            && c.getLongitude() >= minLng && c.getLongitude() <= maxLng)
                    .collect(Collectors.toList());
        } else {
            cells = generated;
        }
        double genTime = (System.nanoTime() - genStart) / 1e9;
        System.out.printf("    Grid ready: %,d hexagons generated in %.2f seconds.\n", cells.size(), genTime);

        // 2. Configure Scenario
        Scenario scenario = new Scenario();
        scenario.setPresetKey("benchmark_" + resolution);
        scenario.setName("Benchmark Res " + resolution);
        scenario.setStartDateYear(-10000);
        scenario.setEndDateYear(-8000);
        scenario.setH3Resolution(resolution);
        scenario.setInitialHumanCount(100_000);
        scenario.setStrictDeterminism(true);
        scenario.setParallelExecutionEnabled(true);
        scenario.setParallelThreadCount(Runtime.getRuntime().availableProcessors());

        SimulationPerformanceConfig perfConfig = scenario.toPerformanceConfig();
        perfConfig.setParallelThreadCount(Runtime.getRuntime().availableProcessors());

        // 3. Initialize H3SimulationEngine
        Configuration config;
        try {
            config = ConfigurationLoader.loadDefault();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("Failed to load default configuration for benchmark", e);
        }
        H3SimulationEngine engine = new H3SimulationEngine(config);
        engine.initializeFromScenario(scenario, cells);
        engine.setPerformanceConfig(perfConfig);

        if (engine.getHistoryManager() != null) {
            engine.getHistoryManager().configureTargetCadence(10_000, 24);
        }

        SimulationProfiler profiler = engine.getProfiler();
        profiler.reset();

        // 4. Warm-up (2 ticks)
        System.out.print("    Warming up JIT (2 ticks)... ");
        engine.stepForward(2);
        System.out.println("done.");

        // Reset profiler after warm-up
        profiler.reset();

        // 5. Timed Execution
        System.out.printf("    Executing %,d benchmark ticks...\n", ticks);
        long startNanos = System.nanoTime();
        engine.stepForward(ticks);
        long elapsedNanos = System.nanoTime() - startNanos;

        double elapsedSec = elapsedNanos / 1e9;
        double tps = ticks / elapsedSec;
        double msPerTick = (elapsedSec * 1000.0) / ticks;
        long finalPop = engine.getTotalPopulation();

        System.out.printf("    Finished: %.2f seconds | %.2f TPS | %.1f ms/tick | Pop: %,d\n",
                elapsedSec, tps, msPerTick, finalPop);
        System.out.println(profiler.generateReport());

        engine.shutdown();

        return new RealBenchmarkResult(name, resolution, cells.size(), ticks, elapsedSec, tps, msPerTick, finalPop);
    }
}
