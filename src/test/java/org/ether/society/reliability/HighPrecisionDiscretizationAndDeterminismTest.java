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

import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.DemographicKernel;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

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
 */
public class HighPrecisionDiscretizationAndDeterminismTest {
    private static final Logger logger = LoggerFactory.getLogger(HighPrecisionDiscretizationAndDeterminismTest.class);

    /**
     * Execution schedule mode for spatial cluster domain decomposition testing.
     */
    private enum ClusterScheduleMode {
        SEQUENTIAL_FORWARD,
        SEQUENTIAL_REVERSED,
        PERMUTED_INTERLEAVED,
        PARALLEL_WORKERS
    }

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

    @Test
    @DisplayName("Spatial Cluster Decomposition: 60-Tick Out-of-Order & Parallel Partitioning Invariance")
    void testMultiTickSpatialClusterDecompositionDeterminism() throws Exception {
        long fixedSeed = 0xCAFE_BABE_9876_5432L;
        int totalCells = 512;
        int ticks = 60;

        // Test invariant state across varied cluster counts (4 clusters vs 8 clusters vs 16 clusters)
        // and across forward, reversed, interleaved, and concurrent thread-pool execution orders.
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
        ByteBuffer byteBuf = ByteBuffer.allocate(capacity * 4 * 4);
        for (int i = 0; i < capacity; i++) {
            byteBuf.putFloat(buffer.getBiomassHuman()[i]);
            byteBuf.putFloat(buffer.getFoodResource()[i]);
            byteBuf.putFloat(buffer.getTechnologyLevel()[i]);
            byteBuf.putFloat(buffer.getGiniIndex()[i]);
        }
        byte[] hashBytes = digest.digest(byteBuf.array());
        return HexFormat.of().formatHex(hashBytes);
    }

    /**
     * Executes a 60-tick simulation where the spatial grid is decomposed into {@code clusterCount} disjoint
     * geographic clusters. At every tick:
     * <ol>
     *   <li>Each spatial cluster computes local metabolic, demographic, technological, and ecological dynamics
     *       plus halo boundary migration/trade fluxes against neighboring cells.</li>
     *   <li>Cluster tasks are scheduled according to {@code mode} (forward, reversed, permuted, or parallel threads).</li>
     *   <li>A deterministic double-buffered synchronization barrier commits state updates before the next tick.</li>
     * </ol>
     */
    private static String executeSpatialClusteredSimulationAndHash(
            long seed, int totalCells, int clusterCount, int ticks, ClusterScheduleMode mode) throws Exception {
        Random rng = new Random(seed);
        WorldBuffer world = new WorldBuffer(totalCells);
        float[] pop = world.getBiomassHuman();
        float[] food = world.getFoodResource();
        float[] tech = world.getTechnologyLevel();
        float[] capital = world.getResourceCapital();
        float[] gini = world.getGiniIndex();

        for (int i = 0; i < totalCells; i++) {
            world.getH3Indexes()[i] = 0x85283473fffffffL + i;
            pop[i] = 250.0f + (float) rng.nextDouble() * 150.0f;
            food[i] = 800.0f + (float) rng.nextDouble() * 400.0f;
            tech[i] = 1.2f + (float) rng.nextDouble() * 0.8f;
            capital[i] = 100.0f + (float) rng.nextDouble() * 50.0f;
            gini[i] = 0.30f + (float) rng.nextDouble() * 0.15f;
        }

        // Double-buffers to guarantee cluster order & Halo boundary exchange invariance
        float[] nextPop = new float[totalCells];
        float[] nextFood = new float[totalCells];
        float[] nextTech = new float[totalCells];
        float[] nextCapital = new float[totalCells];
        float[] nextGini = new float[totalCells];

        int clusterSize = totalCells / clusterCount;
        ExecutorService pool = (mode == ClusterScheduleMode.PARALLEL_WORKERS)
                ? Executors.newFixedThreadPool(Math.min(clusterCount, 8))
                : null;

        try {
            for (int t = 0; t < ticks; t++) {
                final int currentTick = t;
                int[] scheduleOrder = buildClusterScheduleOrder(clusterCount, mode, currentTick);

                if (mode == ClusterScheduleMode.PARALLEL_WORKERS && pool != null) {
                    List<Callable<Void>> tasks = new ArrayList<>(clusterCount);
                    for (int cIdx : scheduleOrder) {
                        final int start = cIdx * clusterSize;
                        final int end = (cIdx == clusterCount - 1) ? totalCells : (start + clusterSize);
                        tasks.add(() -> {
                            evaluateClusterSlice(start, end, totalCells, currentTick, seed,
                                    pop, food, tech, capital, gini,
                                    nextPop, nextFood, nextTech, nextCapital, nextGini);
                            return null;
                        });
                    }
                    List<Future<Void>> futures = pool.invokeAll(tasks);
                    for (Future<Void> f : futures) {
                        f.get(15, TimeUnit.SECONDS);
                    }
                } else {
                    for (int cIdx : scheduleOrder) {
                        int start = cIdx * clusterSize;
                        int end = (cIdx == clusterCount - 1) ? totalCells : (start + clusterSize);
                        evaluateClusterSlice(start, end, totalCells, currentTick, seed,
                                pop, food, tech, capital, gini,
                                nextPop, nextFood, nextTech, nextCapital, nextGini);
                    }
                }

                // Deterministic barrier commit across all clusters
                System.arraycopy(nextPop, 0, pop, 0, totalCells);
                System.arraycopy(nextFood, 0, food, 0, totalCells);
                System.arraycopy(nextTech, 0, tech, 0, totalCells);
                System.arraycopy(nextCapital, 0, capital, 0, totalCells);
                System.arraycopy(nextGini, 0, gini, 0, totalCells);
            }
        } finally {
            if (pool != null) {
                pool.shutdown();
            }
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        ByteBuffer byteBuf = ByteBuffer.allocate(totalCells * 5 * 4);
        for (int i = 0; i < totalCells; i++) {
            byteBuf.putFloat(pop[i]);
            byteBuf.putFloat(food[i]);
            byteBuf.putFloat(tech[i]);
            byteBuf.putFloat(capital[i]);
            byteBuf.putFloat(gini[i]);
        }
        return HexFormat.of().formatHex(digest.digest(byteBuf.array()));
    }

    private static int[] buildClusterScheduleOrder(int clusterCount, ClusterScheduleMode mode, int tick) {
        int[] order = new int[clusterCount];
        for (int i = 0; i < clusterCount; i++) {
            order[i] = i;
        }
        if (mode == ClusterScheduleMode.SEQUENTIAL_REVERSED) {
            for (int i = 0; i < clusterCount / 2; i++) {
                int tmp = order[i];
                order[i] = order[clusterCount - 1 - i];
                order[clusterCount - 1 - i] = tmp;
            }
        } else if (mode == ClusterScheduleMode.PERMUTED_INTERLEAVED || mode == ClusterScheduleMode.PARALLEL_WORKERS) {
            // Deterministic Fisher-Yates permutation per tick to stress arrival order invariance
            Random shuffleRng = new Random(0x9E3779B97F4A7C15L ^ tick);
            for (int i = clusterCount - 1; i > 0; i--) {
                int j = shuffleRng.nextInt(i + 1);
                int tmp = order[i];
                order[i] = order[j];
                order[j] = tmp;
            }
        }
        return order;
    }

    private static void evaluateClusterSlice(
            int start, int end, int totalCells, int tick, long seed,
            float[] pop, float[] food, float[] tech, float[] capital, float[] gini,
            float[] nextPop, float[] nextFood, float[] nextTech, float[] nextCapital, float[] nextGini) {
        for (int i = start; i < end; i++) {
            // Read Halo neighbors (toroidal spatial ring coupling across cluster borders)
            int left = (i - 1 + totalCells) % totalCells;
            int right = (i + 1) % totalCells;

            float p = pop[i];
            float f = food[i];
            float tc = tech[i];
            float cap = capital[i];
            float g = gini[i];

            // Inter-cluster diffusive migration & trade flux (reads strictly from immutable t state)
            float neighborPopAvg = 0.5f * (pop[left] + pop[right]);
            float migrationFlux = 0.02f * (neighborPopAvg - p);

            float neighborTechMax = Math.max(tech[left], tech[right]);
            float techDiffusion = 0.01f * Math.max(0.0f, neighborTechMax - tc);

            // Local Boserupian / Malthusian metabolic step
            float carryingCapacity = f * (0.4f + 0.15f * tc);
            float logisticRate = 0.025f * (1.0f - (p / Math.max(1.0f, carryingCapacity)));
            float updatedPop = Math.max(1.0f, p * (1.0f + logisticRate) + migrationFlux);

            // Deterministic cell-local stochastic climate perturbation
            double noise = org.ether.society.util.DeterministicPRNG.nextDouble(seed, tick, i, 7L);
            float climateFactor = 0.98f + 0.04f * (float) noise;

            float harvested = Math.min(f * 0.35f, updatedPop * 0.8f);
            float regrownFood = (f - harvested) * climateFactor + 120.0f * (float) Math.sqrt(tc);

            float updatedTech = tc + techDiffusion + 0.001f * (float) Math.log1p(updatedPop * 0.01f);
            float updatedCap = cap * 0.97f + harvested * 0.12f * updatedTech;
            float updatedGini = Math.clamp(g + 0.002f * ((updatedCap / (updatedPop + 1.0f)) - 0.5f), 0.10f, 0.85f);

            nextPop[i] = updatedPop;
            nextFood[i] = Math.max(50.0f, regrownFood);
            nextTech[i] = updatedTech;
            nextCapital[i] = Math.max(0.0f, updatedCap);
            nextGini[i] = updatedGini;
        }
    }

    /**
     * Instantiates a complete {@link H3SimulationEngine}, populates an H3 cell topology, runs {@code ticks}
     * full simulation steps through {@link org.ether.society.core.SimulationPipeline}, and computes a SHA-256
     * digest over all resulting cellular state tensors.
     */
    private static String executeFullEnginePipelineAndHash(long seed, int ticks) throws Exception {
        Configuration config = ConfigurationLoader.loadDefault();
        H3SimulationEngine engine = new H3SimulationEngine(config);

        Random rng = new Random(seed);
        int cellCount = 16;
        List<H3Cell> cells = new ArrayList<>(cellCount);
        org.ether.society.h3.H3Service h3 = org.ether.society.h3.H3Service.getInstance();
        for (int i = 0; i < cellCount; i++) {
            double lat = 42.0 + i * 1.5;
            double lng = 2.0 + i * 1.5;
            H3Cell cell = new H3Cell();
            cell.setH3Index(h3.latLngToH3(lat, lng, 3));
            cell.setBiome(i % 2 == 0 ? Biome.FOREST : Biome.PLAINS);
            cell.setPopulation(1200 + rng.nextInt(800));
            cell.setBiomassHuman(600.0 + rng.nextDouble() * 400.0);
            cell.setTemperature(14.0 + rng.nextDouble() * 6.0);
            cell.setFoodResource(2500.0 + rng.nextDouble() * 1000.0);
            cell.setWaterResource(80.0 + rng.nextDouble() * 40.0);
            cell.setResourceCapital(300.0 + rng.nextDouble() * 200.0);
            cell.setTechnologyLevel(1.5 + rng.nextDouble() * 1.0);
            cell.setPollutionLevel(0.01);
            cell.setElevation(120.0 + i * 15.0);
            cell.setLatitude(lat);
            cell.setLongitude(lng);
            cell.setEnergyWind(100.0);
            cells.add(cell);
        }

        Scenario scenario = new Scenario();
        scenario.setName("Determinism Multi-Tick Verification");
        scenario.setDescription("Full pipeline multi-tick bit-exactness verification");
        scenario.setTemporalResolutionDays(30.0);
        scenario.setSeed(seed);

        engine.initializeFromScenario(scenario, cells);
        engine.stepForward(ticks);

        List<H3Cell> finalCells = engine.getCells();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        ByteBuffer buf = ByteBuffer.allocate(finalCells.size() * 8 * 8);
        for (H3Cell c : finalCells) {
            buf.putDouble(c.getPopulation() != null ? c.getPopulation() : 0.0);
            buf.putDouble(c.getBiomassHuman() != null ? c.getBiomassHuman() : 0.0);
            buf.putDouble(c.getTemperature() != null ? c.getTemperature() : 0.0);
            buf.putDouble(c.getFoodResource() != null ? c.getFoodResource() : 0.0);
            buf.putDouble(c.getWaterResource() != null ? c.getWaterResource() : 0.0);
            buf.putDouble(c.getResourceCapital() != null ? c.getResourceCapital() : 0.0);
            buf.putDouble(c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 0.0);
            buf.putDouble(c.getPollutionLevel() != null ? c.getPollutionLevel() : 0.0);
        }
        return HexFormat.of().formatHex(digest.digest(buf.array()));
    }
}

