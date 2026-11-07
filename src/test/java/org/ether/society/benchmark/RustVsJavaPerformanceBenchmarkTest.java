/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.benchmark;

import org.ether.society.core.dod.*;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <h1>Empirical Engine Performance Benchmark: Native Rust Core vs. Java SIMD Vector vs. Java OOP</h1>
 * <p>
 * Evaluates execution throughput (TPS), latency per tick (ms), and per-cell latency (ns/cell)
 * across three distinct simulation scales:
 * <ul>
 *   <li>10,000 cells (Regional windowed basin)</li>
 *   <li>41,162 cells (H3 Resolution 3 global grid)</li>
 *   <li>288,122 cells (H3 Resolution 4 canonical Phase 1 planetary mesh)</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class RustVsJavaPerformanceBenchmarkTest {
    private static final Logger logger = LoggerFactory.getLogger(RustVsJavaPerformanceBenchmarkTest.class);

    private record BenchmarkEntry(
            String engineName,
            int cellCount,
            int ticks,
            double durationMs,
            double tps,
            double msPerTick,
            double latencyNsPerCell,
            double speedupVsJava
    ) {}

    @Test
    @DisplayName("Evaluate Native Rust Core vs Java SIMD DoD Performance Benchmark")
    public void testRustVsJavaComparativePerformance() {
        Locale.setDefault(Locale.US);
        boolean nativeAvailable = NativeRustBridge.isNativeAvailable();
        logger.info("⚡ Native Rust Bridge Available: {} (Binary: {})",
                nativeAvailable, NativeRustBridge.getLoadedBinaryPath());

        int[] scales = {10_000, 41_162, 288_122};
        int ticks = 10;

        StringBuilder sb = new StringBuilder();
        sb.append("\n====================================================================================================\n");
        sb.append("   🚀 ETHER PLANETARY ENGINE BENCHMARK: NATIVE RUST CORE vs. JAVA 25 SIMD VECTOR\n");
        sb.append("====================================================================================================\n");
        sb.append(String.format("| %-32s | %-12s | %-10s | %-12s | %-14s | %-10s |\n",
                "Engine Backend Architecture", "Grid Scale", "Throughput", "Per-Tick Lat", "Cell Latency", "Speedup"));
        sb.append("|----------------------------------|--------------|------------|--------------|----------------|------------|\n");

        for (int cells : scales) {
            WorldBuffer world = new WorldBuffer(cells);
            AgentBuffer agents = new AgentBuffer(cells);
            populateBuffers(world, agents, cells);

            // 1. Benchmark Java SIMD DoD Kernels
            DemographicKernel demog = new DemographicKernel();
            EnvironmentalKernel env = new EnvironmentalKernel();
            UrbanKernel urban = new UrbanKernel();
            CultureKernel culture = new CultureKernel();
            float dt = 30.0f * 86400.0f;

            // Warmup
            for (int t = 0; t < 2; t++) {
                env.tick(world, 6, dt);
                demog.tick(world, agents, dt);
                urban.tick(world, dt);
                culture.tick(world, agents, dt);
            }

            long startJava = System.nanoTime();
            for (int t = 0; t < ticks; t++) {
                env.tick(world, 6, dt);
                demog.tick(world, agents, dt);
                urban.tick(world, dt);
                culture.tick(world, agents, dt);
            }
            long durationNanosJava = System.nanoTime() - startJava;
            double durationMsJava = durationNanosJava / 1e6;
            double tpsJava = ticks / (durationNanosJava / 1e9);
            double msPerTickJava = durationMsJava / ticks;
            double latencyNsJava = (double) durationNanosJava / (ticks * cells);

            sb.append(String.format(Locale.US, "| %-32s | %,9d h3 | %7.2f TPS | %8.2f ms | %8.1f ns/cell |  Baseline  |\n",
                    "Java 25 SIMD Vector DoD", cells, tpsJava, msPerTickJava, latencyNsJava));

            // 2. Benchmark Native Rust Core (if available)
            if (nativeAvailable) {
                try (NativeRustBridge.NativeWorldArena arena = new NativeRustBridge.NativeWorldArena(cells)) {
                    arena.syncFromJavaBuffers(world, agents);

                    // Warmup
                    for (int t = 0; t < 2; t++) {
                        arena.stepUnifiedCycle(30.0f);
                    }

                    long startRust = System.nanoTime();
                    for (int t = 0; t < ticks; t++) {
                        arena.stepUnifiedCycle(30.0f);
                    }
                    long durationNanosRust = System.nanoTime() - startRust;
                    arena.syncToJavaBuffers(world, agents);

                    double durationMsRust = durationNanosRust / 1e6;
                    double tpsRust = ticks / (durationNanosRust / 1e9);
                    double msPerTickRust = durationMsRust / ticks;
                    double latencyNsRust = (double) durationNanosRust / (ticks * cells);
                    double speedup = tpsRust / Math.max(0.001, tpsJava);

                    sb.append(String.format(Locale.US, "| %-32s | %,9d h3 | %7.2f TPS | %8.2f ms | %8.1f ns/cell |   %5.2fx   |\n",
                            "Native Rust Core (AVX-512)", cells, tpsRust, msPerTickRust, latencyNsRust, speedup));
                }
            }
            sb.append("|----------------------------------|--------------|------------|--------------|----------------|------------|\n");
        }
        sb.append("====================================================================================================\n");

        System.out.println(sb.toString());
        logger.info("Comparative benchmark complete.");
        assertTrue(true);
    }

    private static void populateBuffers(WorldBuffer world, AgentBuffer agents, int count) {
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
}
