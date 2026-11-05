/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.benchmark;

import org.ether.society.core.H3SimulationEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <h1>Real Planetary Simulation Engine Benchmark Runner</h1>
 * <p>
 * Ground-truth, non-extrapolated empirical benchmark runner executing the complete
 * {@link H3SimulationEngine} simulation pipeline across <b>5 distinct scientific axes</b>:
 * </p>
 * <ol>
 *   <li><b>Axis 1 (Spatial Grid Scaling)</b>: Full planetary Earth from Res 2 up to Res 7.</li>
 *   <li><b>Axis 2 (Temporal Step Resolution $\Delta t$)</b>: Daily (1d), Weekly (7d), Monthly (30d), Annual (365d).</li>
 *   <li><b>Axis 3 (Compute Backend Architecture)</b>: Java JIT Monocore vs Java SIMD Multicore vs Native Rust Core.</li>
 *   <li><b>Axis 4 (Physics Coupling Tiers)</b>: Tier 1 conservation PDEs only vs Tier 1 + Tier 2 full cliodynamics.</li>
 *   <li><b>Axis 5 (Historical Epochs &amp; Demographic Scale)</b>: Out of Africa (-100k) to Modern (+2026).</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class RealPlanetaryEngineBenchmarkRunner {

    public record SpatialResult(
            String testLabel, int resolution, int cellCount, boolean windowed,
            String engineMode, int threads, int ticks, double durationSec,
            double tps, double msPerTick, double latencyUsPerCell,
            double extrapolated10kMinutes, long estimatedSnapshotBytes, long finalPopulation
    ) {}

    public record TemporalResult(
            String stepLabel, double stepDays, double simYearsPer10k,
            int ticks, double durationSec, double tps, double msPerTick,
            double latencyUsPerCell, double execution10kMinutes
    ) {}

    public record BackendResult(
            String backendName, int resolution, int cellCount, int threads,
            int ticks, double durationMs, double tps, double msPerTick,
            double latencyNsPerCell, double speedupVsMonocore
    ) {}

    public record CouplingTierResult(
            String tierLabel, boolean tier2Enabled, int cellCount, int ticks,
            double durationSec, double tps, double msPerTick,
            double latencyUsPerCell, double overheadPercentVsTier1
    ) {}

    public record EpochResult(
            String epochName, long year, long initialPopulation, double techLevel,
            int cellCount, int ticks, double durationSec, double tps,
            double msPerTick, double latencyUsPerCell, long finalPopulation
    ) {}

    public record FullSuiteReport(
            List<SpatialResult> spatialResults,
            List<TemporalResult> temporalResults,
            List<BackendResult> backendResults,
            List<CouplingTierResult> tierResults,
            List<EpochResult> epochResults
    ) {}

    public static void main(String[] args) {
        runBenchmarks();
    }

    public static FullSuiteReport runBenchmarks() {
        Locale.setDefault(Locale.US);
        int availableCores = Runtime.getRuntime().availableProcessors();
        long maxHeapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        PlanetaryBenchmarkReportFormatter.printStartupBanner(availableCores, maxHeapMb);

        List<SpatialResult> spatialResults = new ArrayList<>();
        List<TemporalResult> temporalResults = new ArrayList<>();
        List<BackendResult> backendResults = new ArrayList<>();
        List<CouplingTierResult> tierResults = new ArrayList<>();
        List<EpochResult> epochResults = new ArrayList<>();

        PlanetaryBenchmarkReportFormatter.printSectionHeader(
                "📍 AXIS 1: SPATIAL GRID SCALING SWEEP (Full Earth Global Res 2 to Res 7, Unified Native Rust Core)");
        spatialResults.add(executeNativeSpatialRun("Res 2 Global Macro", 2, 5_882, availableCores, 10));
        spatialResults.add(executeNativeSpatialRun("Res 3 Global Standard", 3, 41_162, availableCores, 10));
        spatialResults.add(executeNativeSpatialRun("Res 4 Global High-Fi", 4, 288_122, availableCores, 10));
        spatialResults.add(executeNativeSpatialRun("Res 5 Global Ultra-Fi", 5, 2_016_842, availableCores, 5));
        spatialResults.add(executeNativeSpatialRun("Res 6 Global Continental", 6, 14_117_882, availableCores, 3));
        spatialResults.add(executeNativeSpatialRun("Res 7 Global Sub-Kilometer", 7, 98_825_162, availableCores, 2));

        PlanetaryBenchmarkReportFormatter.printSectionHeader(
                "⏱️ AXIS 2: TEMPORAL INTEGRATION STEP RESOLUTION SWEEP (Res 3 Global Earth, 41,162 Hexagons)");
        temporalResults.add(executeTemporalRun("Daily (Δt = 1d)", 3, 1.0, availableCores, 10));
        temporalResults.add(executeTemporalRun("Weekly (Δt = 7d)", 3, 7.0, availableCores, 10));
        temporalResults.add(executeTemporalRun("Monthly (Δt = 30d)", 3, 30.0, availableCores, 10));
        temporalResults.add(executeTemporalRun("Annual (Δt = 365d)", 3, 365.25, availableCores, 10));

        PlanetaryBenchmarkReportFormatter.printSectionHeader(
                "⚙️ AXIS 3: COMPUTE BACKEND ENGINE ARCHITECTURE (Res 3 Global Earth, 41,162 Hexagons)");
        BackendResult monocore = executeBackendRun("Java JIT (Monocœur, 1 Thread)", 3, 41_162, 1, 10, 1.0);
        backendResults.add(monocore);
        double baselineTps = monocore.tps();
        backendResults.add(executeBackendRun("Java 25 SIMD Vector (Multicœur)", 3, 41_162, availableCores, 10, baselineTps));
        backendResults.add(executeBackendRun("Native Rust Core (AVX-512 + Rayon)", 3, 41_162, availableCores, 10, baselineTps));

        PlanetaryBenchmarkReportFormatter.printSectionHeader(
                "🔬 AXIS 4: PHYSICS COUPLING TIERS (Tier 1 Conservation PDEs vs Tier 1 + Tier 2 Cliodynamics)");
        CouplingTierResult tier1Result = executeCouplingTierRun("Tier 1 Invariant Conservation PDEs Only", false, 3, availableCores, 10, 0.0);
        tierResults.add(tier1Result);
        tierResults.add(executeCouplingTierRun("Tier 1 + Tier 2 Coupled Cliodynamics", true, 3, availableCores, 10, tier1Result.msPerTick()));

        PlanetaryBenchmarkReportFormatter.printSectionHeader(
                "⏳ AXIS 5: HISTORICAL EPOCHS & DEMOGRAPHIC SCALE SWEEP (Res 3 Global, 41,162 Hexagons)");
        epochResults.add(executeEpochRun("Out of Africa", -100_000, 100_000L, 1.0, 3, availableCores, 10));
        epochResults.add(executeEpochRun("Neolithic Revolution", -10_000, 5_000_000L, 2.0, 3, availableCores, 10));
        epochResults.add(executeEpochRun("Classical Antiquity", -500, 150_000_000L, 3.5, 3, availableCores, 10));
        epochResults.add(executeEpochRun("Industrial Revolution", 1800, 1_000_000_000L, 6.0, 3, availableCores, 10));
        epochResults.add(executeEpochRun("Modern Anthropocene", 2026, 8_000_000_000L, 9.5, 3, availableCores, 10));

        FullSuiteReport report = new FullSuiteReport(spatialResults, temporalResults, backendResults, tierResults, epochResults);
        printFullSummaryReport(report);
        return report;
    }

    public static SpatialResult executeSpatialRun(String label, int resolution, boolean windowed, int threads, int ticks) {
        return PlanetaryBenchmarkAxisExecutors.executeSpatialRun(label, resolution, windowed, threads, ticks);
    }

    public static SpatialResult executeNativeSpatialRun(String label, int resolution, int targetCells, int threads, int ticks) {
        return PlanetaryBenchmarkAxisExecutors.executeNativeSpatialRun(label, resolution, targetCells, threads, ticks);
    }

    public static TemporalResult executeTemporalRun(String label, int resolution, double stepDays, int threads, int ticks) {
        return PlanetaryBenchmarkAxisExecutors.executeTemporalRun(label, resolution, stepDays, threads, ticks);
    }

    public static BackendResult executeBackendRun(String label, int resolution, int cells, int threads, int ticks, double baselineTps) {
        return PlanetaryBenchmarkAxisExecutors.executeBackendRun(label, resolution, cells, threads, ticks, baselineTps);
    }

    public static CouplingTierResult executeCouplingTierRun(String label, boolean enableTier2, int resolution, int threads, int ticks, double baselineMs) {
        return PlanetaryBenchmarkAxisExecutors.executeCouplingTierRun(label, enableTier2, resolution, threads, ticks, baselineMs);
    }

    public static EpochResult executeEpochRun(String label, long year, long initPop, double techLevel, int resolution, int threads, int ticks) {
        return PlanetaryBenchmarkAxisExecutors.executeEpochRun(label, year, initPop, techLevel, resolution, threads, ticks);
    }

    public static void printFullSummaryReport(FullSuiteReport report) {
        PlanetaryBenchmarkReportFormatter.printFullSummaryReport(report);
    }
}
