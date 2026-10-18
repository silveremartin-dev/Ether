/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.benchmark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <h1>Comprehensive Real Planetary Simulation Engine Benchmark Suite</h1>
 * <p>
 * JUnit 5 test harness triggering the master 5-axis empirical benchmark suite
 * defined in {@link RealPlanetaryEngineBenchmarkRunner}.
 * Evaluates:
 * <ul>
 *   <li>Axis 1: Spatial Grid Scaling (Res 2 to Res 7)</li>
 *   <li>Axis 2: Temporal Integration Step Resolution (Daily to Annual)</li>
 *   <li>Axis 3: Compute Backend Architecture (Java JIT, SIMD Multicore, Native Rust)</li>
 *   <li>Axis 4: Physics Coupling Tiers (Tier 1 vs Tier 1 + Tier 2)</li>
 *   <li>Axis 5: Historical Epochs &amp; Demographic Scale (-100k BP to +2026 CE)</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ComprehensiveRealEngineBenchmarkTest {
    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveRealEngineBenchmarkTest.class);

    @Test
    @DisplayName("Evaluate 5-Axis Real Planetary Simulation Engine Benchmarks")
    public void runComprehensiveSuite() {
        RealPlanetaryEngineBenchmarkRunner.FullSuiteReport report = RealPlanetaryEngineBenchmarkRunner.runBenchmarks();

        assertNotNull(report, "Benchmark report must not be null");
        assertFalse(report.spatialResults().isEmpty(), "Spatial results must not be empty");
        assertFalse(report.temporalResults().isEmpty(), "Temporal results must not be empty");
        assertFalse(report.backendResults().isEmpty(), "Backend results must not be empty");
        assertFalse(report.tierResults().isEmpty(), "Coupling tier results must not be empty");
        assertFalse(report.epochResults().isEmpty(), "Epoch results must not be empty");

        for (RealPlanetaryEngineBenchmarkRunner.SpatialResult r : report.spatialResults()) {
            // Validate measurement integrity without imposing arbitrary hardware-dependent throughput gates
            assertTrue(!Double.isNaN(r.tps()) && !Double.isInfinite(r.tps()) && r.tps() > 0.0,
                    "Throughput must be a valid positive empirical measurement for " + r.testLabel());
            assertTrue(!Double.isNaN(r.msPerTick()) && !Double.isInfinite(r.msPerTick()) && r.msPerTick() >= 0.0,
                    "Per-tick latency must be a valid non-negative empirical measurement for " + r.testLabel());
            assertTrue(r.finalPopulation() > 0, "Simulation run must preserve non-zero human/planetary population for " + r.testLabel());
        }

        // Backend measurement check: ensure valid empirical throughput recording
        for (RealPlanetaryEngineBenchmarkRunner.BackendResult b : report.backendResults()) {
            assertTrue(!Double.isNaN(b.tps()) && !Double.isInfinite(b.tps()) && b.tps() > 0.0,
                    "Compute backend " + b.backendName() + " must record a valid positive throughput measurement");
        }
    }

    public static void main(String[] args) {
        RealPlanetaryEngineBenchmarkRunner.main(args);
    }
}
