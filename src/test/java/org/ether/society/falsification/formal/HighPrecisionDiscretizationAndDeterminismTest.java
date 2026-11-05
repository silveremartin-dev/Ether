/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.formal;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.falsification.formal.DeterministicExecutionHashSupport.ClusterScheduleMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.ether.society.falsification.formal.DeterministicExecutionHashSupport.executeDeterministicRunAndHash;
import static org.ether.society.falsification.formal.DeterministicExecutionHashSupport.executeFullEnginePipelineAndHash;
import static org.ether.society.falsification.formal.DeterministicExecutionHashSupport.executeSpatialClusteredSimulationAndHash;
import static org.ether.society.falsification.formal.DeterministicExecutionHashSupport.stepRK4;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dual-Engine High-Precision Discretization Drift Bounding &amp; Bit-Exact Determinism Verification.
 *
 * <p>Formally certifies critical numerical and architectural determinism properties of the Ether Engine:</p>
 * <ul>
 *   <li><b>Discretization Drift Bound</b>: Bounds single-precision 32-bit float numerical integration
 *       error against a 64-bit double-precision 4th-order Runge-Kutta (RK4) analytical reference integrator.</li>
 *   <li><b>Cryptographic Bit-Exact Reproducibility</b>: Proves that repeated multi-threaded simulation runs
 *       from identical seeds produce 100% bit-identical SHA-256 memory state hashes.</li>
 *   <li><b>Spatial Cluster Domain Decomposition &amp; Out-of-Order Invariance</b>: Verifies over 60 ticks that
 *       partitioning the planetary grid into spatial clusters with inter-cluster boundary flux preserves
 *       100% bit-exactness across forward, reversed, permuted, and concurrent multi-threaded schedules.</li>
 *   <li><b>End-to-End Full Engine Multi-Tick Determinism</b>: Certifies that complete {@link H3SimulationEngine}
 *       pipelines produce bit-identical state hashes over multi-tick horizons.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see DeterministicExecutionHashSupport
 */
@Tag("academic")
@Tag("scientific")
@Tag("formal")
public class HighPrecisionDiscretizationAndDeterminismTest {

    /** Diagnostic logger for formal numerical drift and SHA-256 hash parity telemetry. */
    private static final Logger logger = LoggerFactory.getLogger(HighPrecisionDiscretizationAndDeterminismTest.class);

    /**
     * Bounds single-precision 32-bit float Euler DOD discretization drift against both a 64-bit
     * 4th-order Runge-Kutta (RK4) solver and the closed-form analytical Verhulst logistic solution
     * \(P(t) = \frac{K}{1 + \frac{K - P_0}{P_0} e^{-rt}}\) over a 200-year horizon.
     */
    @Test
    @DisplayName("Dual-Engine: Bound single-precision DOD drift against 64-bit RK4 reference solver")
    void testDiscretizationDriftAgainstRK4Reference() {
        double r = 0.03; // 3% annual growth
        double K = 1_000_000.0; // Carrying capacity
        double p0 = 10_000.0;

        int totalYears = 200;
        double dt = 1.0;

        // 1. Single-Precision Vector DOD Integration (Simulation Mode)
        float currentPFloat = (float) p0;
        for (int t = 0; t < totalYears; t++) {
            float deltaP = (float) (r * currentPFloat * (1.0f - currentPFloat / (float) K));
            currentPFloat += deltaP;
        }

        // 2. High-Precision 64-bit Double Runge-Kutta 4th Order Integrator (Reference Mode)
        double currentPDouble = p0;
        for (int t = 0; t < totalYears; t++) {
            currentPDouble = stepRK4(currentPDouble, r, K, dt);
        }

        // 3. Analytical Exact Solution: P(t) = K / (1 + ((K - P0)/P0) * e^(-r*t))
        double analyticalExact = K / (1.0 + ((K - p0) / p0) * Math.exp(-r * totalYears));

        double rk4Error = Math.abs(currentPDouble - analyticalExact) / analyticalExact;
        double floatDiscretizationError = Math.abs((double) currentPFloat - analyticalExact) / analyticalExact;

        logger.info("  Analytical Exact: {}", analyticalExact);
        logger.info("  64-bit RK4 Reference: {} (Rel Error: {})", currentPDouble, String.format("%.2e", rk4Error));
        logger.info("  32-bit Float DOD Step: {} (Rel Error: {})", currentPFloat, String.format("%.2e", floatDiscretizationError));

        assertTrue(rk4Error < 1e-4, "64-bit RK4 integrator must match analytical exact within 0.01%");
        assertTrue(floatDiscretizationError < 0.015,
                "Single-precision discretization error must remain bounded below 1.5% over multi-century runs");
    }

    /**
     * Verifies cryptographic SHA-256 bit-exact reproducibility across repeated runs from identical seeds
     * and sensitivity to seed perturbation.
     *
     * @throws NoSuchAlgorithmException if SHA-256 digest algorithm is unavailable
     */
    @Test
    @DisplayName("Determinism: Cryptographic SHA-256 Bit-Exact Parity across Repeated Runs")
    void testBitExactDeterministicReproducibility() throws NoSuchAlgorithmException {
        long fixedSeed = 0xDEAD_BEEF_CAFE_BABEL;
        int capacity = 500;
        int ticks = 1000;

        String hashRun1 = executeDeterministicRunAndHash(fixedSeed, capacity, ticks);
        String hashRun2 = executeDeterministicRunAndHash(fixedSeed, capacity, ticks);
        String hashRun3 = executeDeterministicRunAndHash(fixedSeed, capacity, ticks);

        logger.info("  Run 1 SHA-256 Hash: {}", hashRun1);
        logger.info("  Run 2 SHA-256 Hash: {}", hashRun2);
        logger.info("  Run 3 SHA-256 Hash: {}", hashRun3);

        assertEquals(hashRun1, hashRun2, "Run 1 and Run 2 must produce 100% bit-identical state hashes.");
        assertEquals(hashRun2, hashRun3, "Run 2 and Run 3 must produce 100% bit-identical state hashes.");

        String hashDifferentSeed = executeDeterministicRunAndHash(fixedSeed + 1, capacity, ticks);
        assertNotEquals(hashRun1, hashDifferentSeed, "Different random seed must produce divergent hash.");
    }

    /**
     * Certifies that concurrent multi-threaded execution of independent simulation instances
     * yields zero shared-state race conditions and 100% bit-identical SHA-256 digests.
     *
     * @throws Exception if thread pool execution fails
     */
    @Test
    @DisplayName("Multithreading: Concurrent Parallel Runs Guarantee Bit-Identical SHA-256 Hash")
    void testConcurrentMultiThreadedDeterminismBitExactness() throws Exception {
        long fixedSeed = 0x1234_5678_9ABC_DEF0L;
        int capacity = 500;
        int ticks = 500;
        int threadCount = 8;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> executeDeterministicRunAndHash(fixedSeed, capacity, ticks)));
        }

        List<String> hashes = new ArrayList<>();
        for (Future<String> f : futures) {
            hashes.add(f.get(30, TimeUnit.SECONDS));
        }
        executor.shutdown();

        String referenceHash = hashes.get(0);
        logger.info("  Multithreaded Concurrent Reference SHA-256: {}", referenceHash);

        for (int i = 1; i < hashes.size(); i++) {
            assertEquals(referenceHash, hashes.get(i),
                    "Thread " + i + " output diverged from thread 0 under concurrent multithreading!");
        }
    }

    /**
     * Certifies over 60 ticks that spatial domain decomposition across 4, 8, and 16 clusters
     * produces 100% bit-exact state digests regardless of forward, reversed, permuted, or parallel scheduling.
     *
     * @throws Exception if parallel cluster worker execution fails
     */
    @Test
    @DisplayName("Spatial Cluster Decomposition: 60-Tick Out-of-Order & Parallel Partitioning Invariance")
    void testMultiTickSpatialClusterDecompositionDeterminism() throws Exception {
        long fixedSeed = 0xCAFE_BABE_9876_5432L;
        int totalCells = 512;
        int ticks = 60;

        String hashForward4 = executeSpatialClusteredSimulationAndHash(
                fixedSeed, totalCells, 4, ticks, ClusterScheduleMode.SEQUENTIAL_FORWARD);
        String hashReversed8 = executeSpatialClusteredSimulationAndHash(
                fixedSeed, totalCells, 8, ticks, ClusterScheduleMode.SEQUENTIAL_REVERSED);
        String hashPermuted8 = executeSpatialClusteredSimulationAndHash(
                fixedSeed, totalCells, 8, ticks, ClusterScheduleMode.PERMUTED_INTERLEAVED);
        String hashParallel16 = executeSpatialClusteredSimulationAndHash(
                fixedSeed, totalCells, 16, ticks, ClusterScheduleMode.PARALLEL_WORKERS);

        logger.info("  Cluster [4-Forward]     SHA-256: {}", hashForward4);
        logger.info("  Cluster [8-Reversed]    SHA-256: {}", hashReversed8);
        logger.info("  Cluster [8-Permuted]    SHA-256: {}", hashPermuted8);
        logger.info("  Cluster [16-Parallel]   SHA-256: {}", hashParallel16);

        assertEquals(hashForward4, hashReversed8,
                "Reversing spatial cluster evaluation order and changing cluster granularity (4 vs 8) must preserve bit-exact state over 60 ticks.");
        assertEquals(hashForward4, hashPermuted8,
                "Interleaved permutation of spatial clusters must preserve bit-exact state over 60 ticks.");
        assertEquals(hashForward4, hashParallel16,
                "Concurrent 16-cluster parallel worker execution with double-buffered Halo exchange must match sequential state bit-for-bit.");
    }

    /**
     * Certifies end-to-end 25-tick bit-exact trajectory reproducibility across independent
     * {@link H3SimulationEngine} instances with {@code strictDeterminism = true}.
     *
     * @throws Exception if engine pipeline execution fails
     */
    @Test
    @DisplayName("End-to-End Engine: Full H3SimulationEngine 25-Tick Trajectory Bit-Exact Determinism")
    void testFullSimulationEngineMultiTickDeterminism() throws Exception {
        long scenarioSeed = 0x7777_8888_9999_AAAAL;
        int ticks = 25;

        String hashEngineRun1 = executeFullEnginePipelineAndHash(scenarioSeed, ticks);
        String hashEngineRun2 = executeFullEnginePipelineAndHash(scenarioSeed, ticks);

        logger.info("  Full H3SimulationEngine (25 Ticks) Run 1 SHA-256: {}", hashEngineRun1);
        logger.info("  Full H3SimulationEngine (25 Ticks) Run 2 SHA-256: {}", hashEngineRun2);

        assertEquals(hashEngineRun1, hashEngineRun2,
                "Independent H3SimulationEngine instances stepped over 25 full ticks must yield 100% bit-identical SHA-256 cellular state digests.");
    }
}
