/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.benchmark;

import org.ether.society.benchmark.RealPlanetaryEngineBenchmarkRunner.BackendResult;
import org.ether.society.benchmark.RealPlanetaryEngineBenchmarkRunner.CouplingTierResult;
import org.ether.society.benchmark.RealPlanetaryEngineBenchmarkRunner.EpochResult;
import org.ether.society.benchmark.RealPlanetaryEngineBenchmarkRunner.FullSuiteReport;
import org.ether.society.benchmark.RealPlanetaryEngineBenchmarkRunner.SpatialResult;
import org.ether.society.benchmark.RealPlanetaryEngineBenchmarkRunner.TemporalResult;
import org.ether.society.core.dod.NativeRustBridge;

import java.util.Locale;

/**
 * Package-private console banner and 5-axis summary table formatter for {@link RealPlanetaryEngineBenchmarkRunner}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see RealPlanetaryEngineBenchmarkRunner
 */
final class PlanetaryBenchmarkReportFormatter {

    private PlanetaryBenchmarkReportFormatter() {
        // Stateless console formatter
    }

    static void printStartupBanner(int availableCores, long maxHeapMb) {
        System.out.println("====================================================================================================================");
        System.out.println("🌍 ETHER REAL PLANETARY SIMULATION ENGINE — 5-AXIS EMPIRICAL BENCHMARK SUITE");
        System.out.println("====================================================================================================================");
        System.out.printf("Hardware Architecture : %s (%s) | Available Processors: %d vCPUs | JVM Max Heap: %d MB\n",
                System.getProperty("os.name"), System.getProperty("os.arch"), availableCores, maxHeapMb);
        System.out.printf("JVM Runtime           : %s (%s)\n",
                System.getProperty("java.version"), System.getProperty("java.vendor"));
        System.out.printf("Native Rust Core      : %s\n", NativeRustBridge.isNativeAvailable()
                ? "DETECTED (" + NativeRustBridge.getLoadedBinaryPath() + ")"
                : "NOT DETECTED (Running in pure Java SIMD/DoD fallback)");
        System.out.println("Determinism Mode      : Strict IEEE-754 Bit-Level (strictDeterminism = true)");
        System.out.println("Snapshots Architecture: In-Memory Rolling Buffer + Asynchronous Writer Worker");
        System.out.println("====================================================================================================================");
    }

    static void printSectionHeader(String title) {
        System.out.println("\n----------------------------------------------------------------------------------------------------");
        System.out.println(title);
        System.out.println("----------------------------------------------------------------------------------------------------");
    }

    static void printFullSummaryReport(FullSuiteReport report) {
        System.out.println("\n==============================================================================================================================================");
        System.out.println("📊 ETHER PLANETARY SIMULATION ENGINE — MASTER 5-AXIS BENCHMARK REPORT");
        System.out.println("==============================================================================================================================================");

        System.out.println("\n📍 AXIS 1: SPATIAL GRID SCALING SWEEP (Full Earth Global Res 2 to Res 7)");
        System.out.printf("%-28s | %-5s | %-10s | %-13s | %-9s | %-10s | %-11s | %-14s | %-12s\n",
                "Scenario Configuration", "Grid", "Cells", "Engine Mode", "TPS", "ms/Tick", "μs/Cell/Tick", "10k Ticks Est", "Snap (24x)");
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------");
        for (SpatialResult r : report.spatialResults()) {
            String snapFootprintStr = String.format(Locale.US, "%.1f MB", (r.estimatedSnapshotBytes() * 24.0) / (1024.0 * 1024.0));
            String extrapolatedStr = r.extrapolated10kMinutes() >= 60.0
                    ? String.format(Locale.US, "%.1fh (%dmin)", r.extrapolated10kMinutes() / 60.0, (int) r.extrapolated10kMinutes())
                    : String.format(Locale.US, "%.1f min", r.extrapolated10kMinutes());
            System.out.printf("%-28s | Res%-2d | %,10d | %-13s | %6.2f TPS | %7.1f ms | %8.2f μs | %-14s | %-12s\n",
                    r.testLabel(), r.resolution(), r.cellCount(), r.engineMode(),
                    r.tps(), r.msPerTick(), r.latencyUsPerCell(), extrapolatedStr, snapFootprintStr);
        }

        System.out.println("\n⏱️ AXIS 2: TEMPORAL INTEGRATION STEP RESOLUTION SWEEP (Res 3 Global, 41,162 Hexagons)");
        System.out.printf("%-24s | %-10s | %-28s | %-26s | %-9s | %-10s\n",
                "Temporal Step (Δt)", "Step Days", "Temps Simulé (10 000 ticks)", "Temps Exécution (10k ticks)", "TPS", "ms/Tick");
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------");
        for (TemporalResult r : report.temporalResults()) {
            String simTimeStr = String.format(Locale.US, "%,.1f ans (%s)", r.simYearsPer10k(),
                    r.simYearsPer10k() < 100
                            ? String.format(Locale.US, "%.1f siècles", r.simYearsPer10k() / 100.0)
                            : String.format(Locale.US, "%.1f millénaires", r.simYearsPer10k() / 1000.0));
            String execTimeStr = r.execution10kMinutes() >= 60.0
                    ? String.format(Locale.US, "%.1fh (%d min)", r.execution10kMinutes() / 60.0, (int) r.execution10kMinutes())
                    : String.format(Locale.US, "%.1f min", r.execution10kMinutes());
            System.out.printf("%-24s | %6.1fd   | %-28s | %-26s | %6.2f TPS | %7.1f ms\n",
                    r.stepLabel(), r.stepDays(), simTimeStr, execTimeStr, r.tps(), r.msPerTick());
        }

        System.out.println("\n⚙️ AXIS 3: COMPUTE BACKEND ENGINE ARCHITECTURE (Res 3 Global, 41,162 Hexagons)");
        System.out.printf("%-34s | %-8s | %-9s | %-10s | %-14s | %-10s\n",
                "Backend Architecture Mode", "Threads", "TPS", "ms/Tick", "ns/Cell/Tick", "Speedup vs 1C");
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------");
        for (BackendResult r : report.backendResults()) {
            System.out.printf("%-34s | %-8d | %6.2f TPS | %7.1f ms | %8.1f ns    | %5.2fx\n",
                    r.backendName(), r.threads(), r.tps(), r.msPerTick(), r.latencyNsPerCell(), r.speedupVsMonocore());
        }

        System.out.println("\n🔬 AXIS 4: PHYSICS COUPLING TIERS (Res 3 Global, 41,162 Hexagons)");
        System.out.printf("%-42s | %-12s | %-9s | %-10s | %-11s | %-16s\n",
                "Physics Coupling Tier Configuration", "Tier 2 State", "TPS", "ms/Tick", "μs/Cell/Tick", "Overhead vs Tier 1");
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------");
        for (CouplingTierResult r : report.tierResults()) {
            String overheadStr = r.overheadPercentVsTier1() == 0.0
                    ? "0.0% (Baseline)"
                    : String.format(Locale.US, "%+5.1f%%", r.overheadPercentVsTier1());
            System.out.printf("%-42s | %-12s | %6.2f TPS | %7.1f ms | %8.2f μs | %-16s\n",
                    r.tierLabel(), r.tier2Enabled() ? "ENABLED" : "DISABLED",
                    r.tps(), r.msPerTick(), r.latencyUsPerCell(), overheadStr);
        }

        System.out.println("\n⏳ AXIS 5: HISTORICAL EPOCHS & DEMOGRAPHIC SCALE (Res 3 Global, 41,162 Hexagons)");
        System.out.printf("%-24s | %-12s | %-16s | %-7s | %-9s | %-10s | %-11s\n",
                "Historical Epoch Preset", "Epoch Year", "Initial Pop (T₀)", "Tech", "TPS", "ms/Tick", "Final Pop");
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------");
        for (EpochResult r : report.epochResults()) {
            String epochYearStr = String.format("%d %s", Math.abs(r.year()), r.year() < 0 ? "BCE" : "CE");
            System.out.printf("%-24s | %-12s | %,16d | %5.1f | %6.2f TPS | %7.1f ms | %,11d\n",
                    r.epochName(), epochYearStr, r.initialPopulation(), r.techLevel(),
                    r.tps(), r.msPerTick(), r.finalPopulation());
        }

        System.out.println("\n👥 AXIS 6: DEMOGRAPHIC COHORT SIZE GRANULARITY SWEEP (Res 3 Global, 41,162 Hexagons, N = 100M Pop)");
        System.out.printf("%-28s | %-12s | %-14s | %-9s | %-10s | %-18s | %-12s\n",
                "Cohort Granularity Regime", "Hab/Cohort", "Active Nodes", "TPS", "ms/Tick", "Speedup vs Dunbar", "Final Pop");
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------");
        for (RealPlanetaryEngineBenchmarkRunner.CohortResult r : report.cohortResults()) {
            String speedupStr = String.format(Locale.US, "%.2fx", r.speedupVsDunbar());
            System.out.printf("%-28s | %,12d | %,14d | %6.2f TPS | %7.1f ms | %-18s | %,12d\n",
                    r.cohortLabel(), r.targetCohortSize(), r.activeAgentNodes(),
                    r.tps(), r.msPerTick(), speedupStr, r.finalPopulation());
        }
        System.out.println("==============================================================================================================================================");
    }
}
