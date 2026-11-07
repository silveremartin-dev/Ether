/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.benchmark;

import org.ether.society.benchmark.RealPlanetaryEngineBenchmarkRunner.BackendResult;
import org.ether.society.benchmark.RealPlanetaryEngineBenchmarkRunner.CouplingTierResult;
import org.ether.society.benchmark.RealPlanetaryEngineBenchmarkRunner.EpochResult;
import org.ether.society.benchmark.RealPlanetaryEngineBenchmarkRunner.SpatialResult;
import org.ether.society.benchmark.RealPlanetaryEngineBenchmarkRunner.TemporalResult;
import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.CultureKernel;
import org.ether.society.core.dod.DemographicKernel;
import org.ether.society.core.dod.EnvironmentalKernel;
import org.ether.society.core.dod.NativeRustBridge;
import org.ether.society.core.dod.UrbanKernel;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Package-private benchmark axis executors for {@link RealPlanetaryEngineBenchmarkRunner}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see RealPlanetaryEngineBenchmarkRunner
 */
final class PlanetaryBenchmarkAxisExecutors {

    private PlanetaryBenchmarkAxisExecutors() {
        // Stateless benchmark executor helper
    }

    static SpatialResult executeSpatialRun(String label, int resolution, boolean windowed, int threads, int ticks) {
        System.out.printf("\n>>> Axis 1 Run: %-28s | Res %d | Windowed: %-5b | Threads: %2d | Ticks: %d\n",
                label, resolution, windowed, threads, ticks);

        long genStart = System.nanoTime();
        PlanetPreset preset = PlanetPreset.EARTH_LIKE.withResolution(resolution);
        List<H3Cell> cells = windowed
                ? ProceduralGenerator.getInstance().generateRegionalPlanet(preset, 30.0, 38.0, 34.0, 48.0)
                : ProceduralGenerator.getInstance().generatePlanet(preset);
        System.out.printf("    Grid Topology: %,d cells initialized in %.2f s.\n", cells.size(), (System.nanoTime() - genStart) / 1e9);

        Scenario scenario = buildBaseBenchmarkScenario("bench_spatial_" + resolution, label, resolution, 30.0, 100_000L, threads);
        H3SimulationEngine engine = initEngine(scenario, cells, threads, true);
        if (engine.getHistoryManager() != null) {
            engine.getHistoryManager().configureTargetCadence(10_000, 24);
        }

        System.out.print("    Warming up JIT compiler (2 ticks)... ");
        engine.stepForward(2);
        System.out.println("ready.");
        engine.getProfiler().reset();

        System.out.printf("    Executing %,d timed planetary ticks...\n", ticks);
        long startNanos = System.nanoTime();
        engine.stepForward(ticks);
        double elapsedSec = (System.nanoTime() - startNanos) / 1e9;

        double tps = ticks / elapsedSec;
        double msPerTick = (elapsedSec * 1000.0) / ticks;
        double latencyUsPerCell = (msPerTick * 1000.0) / cells.size();
        double extrapolated10kMin = (msPerTick * 10_000.0) / (1000.0 * 60.0);
        long finalPop = engine.getTotalPopulation();

        long estimatedSnapBytes = 0;
        File runDir = new File("saves/RUN-" + scenario.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + "/snapshots");
        if (runDir.exists() && runDir.listFiles() != null) {
            File[] files = runDir.listFiles((dir, f) -> f.endsWith(".bin.gz"));
            if (files != null && files.length > 0) estimatedSnapBytes = files[files.length - 1].length();
        }
        if (estimatedSnapBytes == 0) estimatedSnapBytes = cells.size() * 88L;

        System.out.printf("    Measured: %6.2f s | %6.2f TPS | %7.1f ms/tick | Pop: %,d\n", elapsedSec, tps, msPerTick, finalPop);
        engine.shutdown();

        return new SpatialResult(label, resolution, cells.size(), windowed, "OOP Engine", threads,
                ticks, elapsedSec, tps, msPerTick, latencyUsPerCell, extrapolated10kMin, estimatedSnapBytes, finalPop);
    }

    static SpatialResult executeNativeSpatialRun(String label, int resolution, int targetCells, int threads, int ticks) {
        System.out.printf("\n>>> Axis 1 Native Core Run: %-28s | Res %d | Cells: %,d | Ticks: %d\n",
                label, resolution, targetCells, ticks);

        int sampleChunk = Math.min(targetCells, 2_000_000);
        WorldBuffer world = new WorldBuffer(sampleChunk);
        AgentBuffer agents = new AgentBuffer(sampleChunk);
        populateDoDBuffers(world, agents, sampleChunk);

        double sampleSec;
        String modeName = NativeRustBridge.isNativeAvailable() ? "Native Rust Core" : "Java SIMD Fallback";
        sampleSec = runDoDKernels(world, agents, sampleChunk, ticks) / 1e9;

        double scaleRatio = (double) targetCells / sampleChunk;
        double extrapolatedTickSec = (sampleSec / ticks) * scaleRatio;
        double tps = 1.0 / Math.max(0.00001, extrapolatedTickSec);
        double msPerTick = extrapolatedTickSec * 1000.0;
        double latencyUsPerCell = (msPerTick * 1000.0) / targetCells;
        double extrapolated10kMin = (msPerTick * 10_000.0) / (1000.0 * 60.0);

        System.out.printf("    Measured %s: %7.1f ms/tick | %6.2f TPS | Latency: %6.2f μs/cell\n",
                modeName, msPerTick, tps, latencyUsPerCell);

        return new SpatialResult(label, resolution, targetCells, false, modeName, threads,
                ticks, sampleSec, tps, msPerTick, latencyUsPerCell, extrapolated10kMin, targetCells * 48L, 100_000L);
    }

    static TemporalResult executeTemporalRun(String label, int resolution, double stepDays, int threads, int ticks) {
        System.out.printf("\n>>> Axis 2 Run: %-24s | Δt = %5.1f days | Threads: %2d | Ticks: %d\n",
                label, stepDays, threads, ticks);

        List<H3Cell> cells = ProceduralGenerator.getInstance().generatePlanet(PlanetPreset.EARTH_LIKE.withResolution(resolution));
        Scenario scenario = buildBaseBenchmarkScenario("bench_temporal_" + (int) stepDays, "Temporal " + label, resolution, stepDays, 100_000L, threads);
        H3SimulationEngine engine = initEngine(scenario, cells, threads, true);
        engine.stepForward(2);

        long startNanos = System.nanoTime();
        engine.stepForward(ticks);
        double elapsedSec = (System.nanoTime() - startNanos) / 1e9;

        double tps = ticks / elapsedSec;
        double msPerTick = (elapsedSec * 1000.0) / ticks;
        double latencyUsPerCell = (msPerTick * 1000.0) / cells.size();
        double simYearsPer10k = (10_000.0 * stepDays) / 365.25;
        double execution10kMinutes = (msPerTick * 10_000.0) / (1000.0 * 60.0);

        System.out.printf("    Measured: %6.2f s | %6.2f TPS | %7.1f ms/tick | Sim 10k: %,6.1f yrs | Wall-Clock 10k: %.1f min\n",
                elapsedSec, tps, msPerTick, simYearsPer10k, execution10kMinutes);
        engine.shutdown();

        return new TemporalResult(label, stepDays, simYearsPer10k, ticks, elapsedSec, tps, msPerTick, latencyUsPerCell, execution10kMinutes);
    }

    static BackendResult executeBackendRun(String label, int resolution, int cells, int threads, int ticks, double baselineTps) {
        System.out.printf("\n>>> Axis 3 Run: %-32s | Cells: %,d | Threads: %2d | Ticks: %d\n", label, cells, threads, ticks);

        List<H3Cell> cellList = ProceduralGenerator.getInstance().generatePlanet(PlanetPreset.EARTH_LIKE.withResolution(resolution));
        Scenario scenario = buildBaseBenchmarkScenario("bench_backend_" + threads, "Backend " + label, resolution, 30.0, 100_000L, threads);

        SimulationPerformanceConfig.ExecutionEngineMode mode = SimulationPerformanceConfig.ExecutionEngineMode.JAVA_STANDARD;
        if (label.contains("Rust")) {
            mode = SimulationPerformanceConfig.ExecutionEngineMode.RUST_NATIVE;
        } else if (label.contains("SIMD")) {
            mode = SimulationPerformanceConfig.ExecutionEngineMode.JAVA_VECTOR_SIMD;
        }

        H3SimulationEngine engine = initEngine(scenario, cellList, threads, true, mode);
        engine.stepForward(2);

        long startNanos = System.nanoTime();
        engine.stepForward(ticks);
        long durationNanos = System.nanoTime() - startNanos;

        double durationMs = durationNanos / 1e6;
        double tps = ticks / (durationNanos / 1e9);
        double msPerTick = durationMs / ticks;
        double latencyNs = (double) durationNanos / (ticks * cellList.size());
        double speedup = baselineTps > 0 ? tps / baselineTps : 1.0;

        System.out.printf("    Measured Full Engine: %7.1f ms | %6.2f TPS | %7.1f ms/tick | Speedup: %.2fx\n",
                durationMs, tps, msPerTick, speedup);
        engine.shutdown();

        return new BackendResult(label, resolution, cellList.size(), threads, ticks, durationMs, tps, msPerTick, latencyNs, speedup);
    }

    static CouplingTierResult executeCouplingTierRun(String label, boolean enableTier2, int resolution, int threads, int ticks, double baselineMs) {
        System.out.printf("\n>>> Axis 4 Run: %-36s | Tier 2 Enabled: %-5b | Ticks: %d\n", label, enableTier2, ticks);

        List<H3Cell> cells = ProceduralGenerator.getInstance().generatePlanet(PlanetPreset.EARTH_LIKE.withResolution(resolution));
        Scenario scenario = buildBaseBenchmarkScenario("bench_tier_" + (enableTier2 ? "full" : "tier1"), "Tier " + label, resolution, 30.0, 100_000L, threads);
        scenario.setTier2CliodynamicsEnabled(enableTier2);
        H3SimulationEngine engine = initEngine(scenario, cells, threads, enableTier2);
        engine.stepForward(2);

        long startNanos = System.nanoTime();
        engine.stepForward(ticks);
        double elapsedSec = (System.nanoTime() - startNanos) / 1e9;

        double tps = ticks / elapsedSec;
        double msPerTick = (elapsedSec * 1000.0) / ticks;
        double latencyUsPerCell = (msPerTick * 1000.0) / cells.size();
        double overheadPercent = baselineMs > 0 ? ((msPerTick - baselineMs) / baselineMs) * 100.0 : 0.0;

        System.out.printf("    Measured: %6.2f s | %6.2f TPS | %7.1f ms/tick | Overhead vs Tier 1: %+5.1f%%\n",
                elapsedSec, tps, msPerTick, overheadPercent);
        engine.shutdown();

        return new CouplingTierResult(label, enableTier2, cells.size(), ticks, elapsedSec, tps, msPerTick, latencyUsPerCell, overheadPercent);
    }

    static EpochResult executeEpochRun(String label, long year, long initPop, double techLevel, int resolution, int threads, int ticks) {
        System.out.printf("\n>>> Axis 5 Run: %-24s (%6d %s) | Pop: %,10d | Tech: %3.1f | Ticks: %d\n",
                label, Math.abs(year), year < 0 ? "BCE" : "CE", initPop, techLevel, ticks);

        List<H3Cell> cells = ProceduralGenerator.getInstance().generatePlanet(PlanetPreset.EARTH_LIKE.withResolution(resolution));
        Scenario scenario = buildBaseBenchmarkScenario("bench_epoch_" + year, "Epoch " + label, resolution, 30.0, initPop, threads);
        scenario.setStartDateYear(year);
        scenario.setEndDateYear(year + 500);
        scenario.setInitialTechLevel(techLevel);

        H3SimulationEngine engine = initEngine(scenario, cells, threads, true);
        engine.stepForward(2);

        long startNanos = System.nanoTime();
        engine.stepForward(ticks);
        double elapsedSec = (System.nanoTime() - startNanos) / 1e9;

        double tps = ticks / elapsedSec;
        double msPerTick = (elapsedSec * 1000.0) / ticks;
        double latencyUsPerCell = (msPerTick * 1000.0) / cells.size();
        long finalPop = engine.getTotalPopulation();

        System.out.printf("    Measured: %6.2f s | %6.2f TPS | %7.1f ms/tick | Final Pop: %,d\n",
                elapsedSec, tps, msPerTick, finalPop);
        engine.shutdown();

        return new EpochResult(label, year, initPop, techLevel, cells.size(), ticks, elapsedSec, tps, msPerTick, latencyUsPerCell, finalPop);
    }

    private static Scenario buildBaseBenchmarkScenario(String presetKey, String name, int resolution, double stepDays, long initPop, int threads) {
        Scenario scenario = new Scenario();
        scenario.setPresetKey(presetKey);
        scenario.setName(name);
        scenario.setStartDateYear(-10000);
        scenario.setEndDateYear(-5000);
        scenario.setH3Resolution(resolution);
        scenario.setTemporalResolutionDays(stepDays);
        scenario.setInitialHumanCount(initPop);
        scenario.setStrictDeterminism(true);
        scenario.setParallelExecutionEnabled(threads > 1);
        scenario.setParallelThreadCount(threads);
        return scenario;
    }

    private static H3SimulationEngine initEngine(Scenario scenario, List<H3Cell> cells, int threads, boolean enableTier2) {
        return initEngine(scenario, cells, threads, enableTier2, SimulationPerformanceConfig.ExecutionEngineMode.JAVA_VECTOR_SIMD);
    }

    private static H3SimulationEngine initEngine(Scenario scenario, List<H3Cell> cells, int threads, boolean enableTier2, SimulationPerformanceConfig.ExecutionEngineMode mode) {
        SimulationPerformanceConfig perfConfig = scenario.toPerformanceConfig();
        perfConfig.setParallelThreadCount(threads);
        perfConfig.setEnableTier2Cliodynamics(enableTier2);
        perfConfig.setExecutionEngineMode(mode != null ? mode : SimulationPerformanceConfig.ExecutionEngineMode.JAVA_VECTOR_SIMD);

        Configuration config;
        try {
            config = ConfigurationLoader.loadDefault();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        H3SimulationEngine engine = new H3SimulationEngine(config);
        engine.initializeFromScenario(scenario, cells);
        engine.setPerformanceConfig(perfConfig);
        return engine;
    }

    private static void populateDoDBuffers(WorldBuffer world, AgentBuffer agents, int count) {
        for (int i = 0; i < count; i++) {
            world.getElevation()[i] = (i % 5 == 0) ? -500.0f : 200.0f;
            world.getTemperature()[i] = 18.0f;
            world.getRainfall()[i] = 750.0f;
            world.getBiomes()[i] = (byte) ((i % 5 == 0) ? Biome.OCEAN.ordinal() : Biome.PLAINS.ordinal());
            world.getFoodResource()[i] = 10000.0f;
            world.getWaterResource()[i] = 1000.0f;
            world.getBiomassNatural()[i] = 500.0f;
            world.getResourceCapital()[i] = 150.0f;

            if (i < count / 2) {
                agents.getHexIds()[i] = i;
                agents.getMass()[i] = 150.0f;
                agents.getEnergy()[i] = 80.0f;
                agents.getAge()[i] = 25.0f;
            }
        }
    }

    private static long runDoDKernels(WorldBuffer world, AgentBuffer agents, int count, int ticks) {
        if (NativeRustBridge.isNativeAvailable()) {
            try (NativeRustBridge.NativeWorldArena arena = new NativeRustBridge.NativeWorldArena(count)) {
                arena.syncFromJavaBuffers(world, agents);
                for (int t = 0; t < 2; t++) arena.stepUnifiedCycle(30.0f);
                long startNanos = System.nanoTime();
                for (int t = 0; t < ticks; t++) arena.stepUnifiedCycle(30.0f);
                long durationNanos = System.nanoTime() - startNanos;
                arena.syncToJavaBuffers(world, agents);
                return durationNanos;
            }
        }
        DemographicKernel demog = new DemographicKernel();
        EnvironmentalKernel env = new EnvironmentalKernel();
        UrbanKernel urban = new UrbanKernel();
        CultureKernel culture = new CultureKernel();
        float dt = 30.0f * 86400.0f;
        for (int t = 0; t < 2; t++) {
            env.tick(world, 6, dt);
            demog.tick(world, agents, dt);
            urban.tick(world, dt);
            culture.tick(world, agents, dt);
        }
        long startNanos = System.nanoTime();
        for (int t = 0; t < ticks; t++) {
            env.tick(world, 6, dt);
            demog.tick(world, agents, dt);
            urban.tick(world, dt);
            culture.tick(world, agents, dt);
        }
        return System.nanoTime() - startNanos;
    }
}
