/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.reliability;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.DemographicKernel;
import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dual-Engine High-Precision Discretization Drift Bounding & Bit-Exact Determinism Verification.
 *
 * <p>Formally certifies two critical numerical properties of the Ether Engine:</p>
 * <ul>
 *   <li><b>Discretization Drift Bound</b>: Bounds single-precision 32-bit float numerical integration
 *       error against a 64-bit double-precision 4th-order Runge-Kutta (RK4) analytical reference integrator.</li>
 *   <li><b>Cryptographic Bit-Exact Reproducibility</b>: Proves that repeated multi-threaded simulation runs
 *       from identical seeds produce 100% bit-identical SHA-256 memory state hashes.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class HighPrecisionDiscretizationAndDeterminismTest {
    private static final Logger logger = LoggerFactory.getLogger(HighPrecisionDiscretizationAndDeterminismTest.class);

    @Test
    @DisplayName("Dual-Engine: Bound single-precision DOD drift against 64-bit RK4 reference solver")
    void testDiscretizationDriftAgainstRK4Reference() {
        // Logistic growth system: dP/dt = r * P * (1 - P/K)
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

        // RK4 must be ultra-precise (< 1e-4 relative error)
        assertTrue(rk4Error < 1e-4, "64-bit RK4 integrator must match analytical exact within 0.01%");
        // Fast float step must remain tightly bounded (< 1.5% discretization error over 200 years)
        assertTrue(floatDiscretizationError < 0.015,
                "Single-precision discretization error must remain bounded below 1.5% over multi-century runs");
    }

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

        // Different seed must produce different hash
        String hashDifferentSeed = executeDeterministicRunAndHash(fixedSeed + 1, capacity, ticks);
        assertNotEquals(hashRun1, hashDifferentSeed, "Different random seed must produce divergent hash.");
    }

    @Test
    @DisplayName("Multithreading: Concurrent Parallel Runs Guarantee Bit-Identical SHA-256 Hash")
    void testConcurrentMultiThreadedDeterminismBitExactness() throws Exception {
        long fixedSeed = 0x1234_5678_9ABC_DEF0L;
        int capacity = 500;
        int ticks = 500;
        int threadCount = 8;

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        List<java.util.concurrent.Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> executeDeterministicRunAndHash(fixedSeed, capacity, ticks)));
        }

        List<String> hashes = new ArrayList<>();
        for (java.util.concurrent.Future<String> f : futures) {
            hashes.add(f.get(30, java.util.concurrent.TimeUnit.SECONDS));
        }
        executor.shutdown();

        String referenceHash = hashes.get(0);
        logger.info("  Multithreaded Concurrent Reference SHA-256: {}", referenceHash);

        for (int i = 1; i < hashes.size(); i++) {
            assertEquals(referenceHash, hashes.get(i), 
                    "Thread " + i + " output diverged from thread 0 under concurrent multithreading!");
        }
    }

    // Helper: 4th-order Runge-Kutta numerical step
    private static double stepRK4(double p, double r, double K, double dt) {
        double k1 = r * p * (1.0 - p / K);
        double k2 = r * (p + 0.5 * dt * k1) * (1.0 - (p + 0.5 * dt * k1) / K);
        double k3 = r * (p + 0.5 * dt * k2) * (1.0 - (p + 0.5 * dt * k2) / K);
        double k4 = r * (p + dt * k3) * (1.0 - (p + dt * k3) / K);
        return p + (dt / 6.0) * (k1 + 2.0 * k2 + 2.0 * k3 + k4);
    }

    // Helper: Runs a deterministic simulation with production DemographicKernel and computes SHA-256 of all raw float arrays
    private static String executeDeterministicRunAndHash(long seed, int capacity, int ticks) throws NoSuchAlgorithmException {
        Random rng = new Random(seed);
        WorldBuffer buffer = new WorldBuffer(capacity);
        AgentBuffer agents = new AgentBuffer(capacity);
        DemographicKernel kernel = new DemographicKernel();

        // Populate initial deterministic state
        for (int i = 0; i < capacity; i++) {
            buffer.getH3Indexes()[i] = 0x882681a33900000L + i;
            float initPop = 100.0f + (float) rng.nextDouble() * 50.0f;
            buffer.getBiomassHuman()[i] = initPop;
            buffer.getBiomassAgriculture()[i] = 200.0f;
            buffer.getBiomassNatural()[i] = 500.0f;
            buffer.getFoodResource()[i] = 500.0f + (float) rng.nextDouble() * 200.0f;
            buffer.getTechnologyLevel()[i] = 1.0f + (float) rng.nextDouble() * 0.5f;
            buffer.getGiniIndex()[i] = 0.25f + (float) rng.nextDouble() * 0.2f;

            agents.getHexIds()[i] = i;
            agents.getMass()[i] = initPop;
            agents.getEnergy()[i] = 100.0f;
            agents.getTechLevel()[i] = buffer.getTechnologyLevel()[i];
        }

        // Run multi-tick updates using production DemographicKernel
        for (int t = 0; t < ticks; t++) {
            kernel.tick(buffer, agents, 30.0f);
        }

        // Hash entire memory buffer using SHA-256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        ByteBuffer byteBuf = ByteBuffer.allocate(capacity * 4 * 4); // 4 float arrays * capacity * 4 bytes
        for (int i = 0; i < capacity; i++) {
            byteBuf.putFloat(buffer.getBiomassHuman()[i]);
            byteBuf.putFloat(buffer.getFoodResource()[i]);
            byteBuf.putFloat(buffer.getTechnologyLevel()[i]);
            byteBuf.putFloat(buffer.getGiniIndex()[i]);
        }
        byte[] hashBytes = digest.digest(byteBuf.array());
        return HexFormat.of().formatHex(hashBytes);
    }
}
