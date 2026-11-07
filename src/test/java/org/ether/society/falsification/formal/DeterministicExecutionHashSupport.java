/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.formal;

import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.DemographicKernel;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.ether.society.util.DeterministicPRNG;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Numerical integration and cryptographic state-hashing support utilities for
 * {@link HighPrecisionDiscretizationAndDeterminismTest}.
 *
 * <p>Provides reference 4th-order Runge-Kutta (RK4) integration, Data-Oriented Design (DOD)
 * multi-tick SHA-256 buffer digest evaluation, spatial cluster domain decomposition with
 * double-buffered Halo boundary exchanges, and full {@link H3SimulationEngine} trajectory hashing.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see HighPrecisionDiscretizationAndDeterminismTest
 */
final class DeterministicExecutionHashSupport {

    /**
     * Execution schedule mode for spatial cluster domain decomposition invariance verification.
     */
    enum ClusterScheduleMode {
        /** Evaluates spatial clusters sequentially from index {@code 0} to {@code C - 1}. */
        SEQUENTIAL_FORWARD,
        /** Evaluates spatial clusters sequentially in reverse order from {@code C - 1} down to {@code 0}. */
        SEQUENTIAL_REVERSED,
        /** Evaluates spatial clusters in a deterministic Fisher-Yates permuted order per tick. */
        PERMUTED_INTERLEAVED,
        /** Evaluates spatial clusters concurrently across a fixed multi-threaded worker pool. */
        PARALLEL_WORKERS
    }

    /**
     * Private constructor preventing instantiation of stateless support utility.
     */
    private DeterministicExecutionHashSupport() {
    }

    /**
     * Advances the continuous Verhulst logistic differential equation
     * \(\frac{dP}{dt} = r P \left(1 - \frac{P}{K}\right)\) by one step \(\Delta t\)
     * using classical 64-bit 4th-order Runge-Kutta (RK4) integration.
     *
     * @param p  current population state \(P(t)\) (individuals)
     * @param r  intrinsic Malthusian growth rate \(r\) (\(\text{yr}^{-1}\))
     * @param k  ecological carrying capacity \(K\) (individuals)
     * @param dt integration time-step \(\Delta t\) (years)
     * @return updated population \(P(t + \Delta t)\) in 64-bit double precision
     */
    static double stepRK4(double p, double r, double k, double dt) {
        double k1 = r * p * (1.0 - p / k);
        double k2 = r * (p + 0.5 * dt * k1) * (1.0 - (p + 0.5 * dt * k1) / k);
        double k3 = r * (p + 0.5 * dt * k2) * (1.0 - (p + 0.5 * dt * k2) / k);
        double k4 = r * (p + dt * k3) * (1.0 - (p + dt * k3) / k);
        return p + (dt / 6.0) * (k1 + 2.0 * k2 + 2.0 * k3 + k4);
    }

    /**
     * Executes a deterministic multi-tick simulation using the production {@link DemographicKernel}
     * and computes a cryptographic SHA-256 hex digest over the resulting float state arrays.
     *
     * @param seed     deterministic scenario initialization seed
     * @param capacity number of cells in the {@link WorldBuffer}
     * @param ticks    number of 30-day ticks to step
     * @return lowercase hexadecimal SHA-256 digest of final cellular state tensors
     * @throws NoSuchAlgorithmException if SHA-256 is unavailable in the JVM security provider
     */
    static String executeDeterministicRunAndHash(long seed, int capacity, int ticks) throws NoSuchAlgorithmException {
        Random rng = new Random(seed);
        WorldBuffer buffer = new WorldBuffer(capacity);
        AgentBuffer agents = new AgentBuffer(capacity);
        DemographicKernel kernel = new DemographicKernel();

        for (int i = 0; i < capacity; i++) {
            buffer.getH3Indexes()[i] = 0x882681a33900000L + i;
            float initPop = 100.0f + (float) rng.nextDouble() * 50.0f;
            buffer.getBiomassHuman()[i] = initPop;
            buffer.getBiomassAgriculture()[i] = 200.0f;
            buffer.getBiomassNatural()[i] = 500.0f;
            buffer.getFoodResource()[i] = 500.0f + (float) rng.nextDouble() * 200.0f;
            buffer.getResourceCapital()[i] = 100.0f + (float) rng.nextDouble() * 50.0f;
            buffer.getGiniIndex()[i] = 0.25f + (float) rng.nextDouble() * 0.2f;

            agents.getHexIds()[i] = i;
            agents.getMass()[i] = initPop;
            agents.getEnergy()[i] = 100.0f;
        }

        for (int t = 0; t < ticks; t++) {
            kernel.tick(buffer, agents, 30.0f);
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        ByteBuffer byteBuf = ByteBuffer.allocate(capacity * 4 * 4);
        for (int i = 0; i < capacity; i++) {
            byteBuf.putFloat(buffer.getBiomassHuman()[i]);
            byteBuf.putFloat(buffer.getFoodResource()[i]);
            byteBuf.putFloat(buffer.getResourceCapital()[i]);
            byteBuf.putFloat(buffer.getGiniIndex()[i]);
        }
        return HexFormat.of().formatHex(digest.digest(byteBuf.array()));
    }

    /**
     * Executes a multi-tick spatial cluster domain-decomposed simulation with double-buffered
     * Halo boundary exchange and computes the resulting SHA-256 state hash.
     *
     * @param seed         deterministic scenario seed anchoring initialization and {@link DeterministicPRNG}
     * @param totalCells   total number of cells in the toroidal spatial ring
     * @param clusterCount number of disjoint spatial clusters partitioning the grid
     * @param ticks        number of discrete ticks to integrate
     * @param mode         cluster scheduling strategy to evaluate order/concurrency invariance
     * @return lowercase hexadecimal SHA-256 digest of final cellular state arrays
     * @throws Exception if worker thread execution fails or times out
     */
    static String executeSpatialClusteredSimulationAndHash(
            long seed, int totalCells, int clusterCount, int ticks, ClusterScheduleMode mode) throws Exception {
        Random rng = new Random(seed);
        WorldBuffer world = new WorldBuffer(totalCells);
        float[] pop = world.getBiomassHuman();
        float[] food = world.getFoodResource();
        float[] tech = new float[totalCells];
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

    /**
     * Builds the cluster evaluation permutation array for a given tick and schedule mode.
     *
     * @param clusterCount number of spatial clusters
     * @param mode         target scheduling mode
     * @param tick         current simulation tick index
     * @return ordered array of cluster indices in {@code [0, clusterCount - 1]}
     */
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

    /**
     * Evaluates local metabolic, technological, and Halo-diffusive fluxes for a contiguous slice
     * of cells {@code [start, end)}, reading strictly from immutable step-{@code t} arrays and
     * writing exclusively into disjoint step-{@code t+1} output buffers.
     *
     * @param start       inclusive starting cell index
     * @param end         exclusive ending cell index
     * @param totalCells  total ring size for toroidal Halo neighbor lookup
     * @param tick        current tick index
     * @param seed        scenario master seed
     * @param pop         immutable step-t population buffer
     * @param food        immutable step-t food reserve buffer
     * @param tech        immutable step-t technology buffer
     * @param capital     immutable step-t capital stock buffer
     * @param gini        immutable step-t Gini inequality buffer
     * @param nextPop     writable step-(t+1) population buffer
     * @param nextFood    writable step-(t+1) food reserve buffer
     * @param nextTech    writable step-(t+1) technology buffer
     * @param nextCapital writable step-(t+1) capital stock buffer
     * @param nextGini    writable step-(t+1) Gini inequality buffer
     */
    private static void evaluateClusterSlice(
            int start, int end, int totalCells, int tick, long seed,
            float[] pop, float[] food, float[] tech, float[] capital, float[] gini,
            float[] nextPop, float[] nextFood, float[] nextTech, float[] nextCapital, float[] nextGini) {
        for (int i = start; i < end; i++) {
            int left = (i - 1 + totalCells) % totalCells;
            int right = (i + 1) % totalCells;

            float p = pop[i];
            float f = food[i];
            float tc = tech[i];
            float cap = capital[i];
            float g = gini[i];

            float neighborPopAvg = 0.5f * (pop[left] + pop[right]);
            float migrationFlux = 0.02f * (neighborPopAvg - p);

            float neighborTechMax = Math.max(tech[left], tech[right]);
            float techDiffusion = 0.01f * Math.max(0.0f, neighborTechMax - tc);

            float carryingCapacity = f * (0.4f + 0.15f * tc);
            float logisticRate = 0.025f * (1.0f - (p / Math.max(1.0f, carryingCapacity)));
            float updatedPop = Math.max(1.0f, p * (1.0f + logisticRate) + migrationFlux);

            double noise = DeterministicPRNG.nextDouble(seed, tick, i, 7L);
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
     * full simulation steps, and computes a SHA-256 digest over all resulting cellular state tensors.
     *
     * @param seed  deterministic scenario seed
     * @param ticks number of full engine ticks to execute
     * @return lowercase hexadecimal SHA-256 digest of sorted final {@link H3Cell} state vectors
     * @throws Exception if configuration loading or SHA-256 hashing fails
     */
    static String executeFullEnginePipelineAndHash(long seed, int ticks) throws Exception {
        Configuration config = ConfigurationLoader.loadDefault();
        H3SimulationEngine engine = new H3SimulationEngine(config);

        Random rng = new Random(seed);
        int cellCount = 16;
        List<H3Cell> cells = new ArrayList<>(cellCount);
        H3Service h3 = H3Service.getInstance();
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
        scenario.setStrictDeterminism(true);

        engine.initializeFromScenario(scenario, cells);
        engine.stepForward(ticks);

        List<H3Cell> finalCells = new ArrayList<>(engine.getCells());
        finalCells.sort(Comparator.comparing(H3Cell::getH3Index));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        ByteBuffer buf = ByteBuffer.allocate(finalCells.size() * 8 * 8);
        for (H3Cell c : finalCells) {
            buf.putDouble(c.getPopulation() != null ? c.getPopulation() : 0.0);
            buf.putDouble(c.getBiomassHuman() != null ? c.getBiomassHuman() : 0.0);
            buf.putDouble(c.getTemperature() != null ? c.getTemperature() : 0.0);
            buf.putDouble(c.getFoodResource() != null ? c.getFoodResource() : 0.0);
            buf.putDouble(c.getWaterResource() != null ? c.getWaterResource() : 0.0);
            buf.putDouble(c.getResourceCapital() != null ? c.getResourceCapital() : 0.0);
            buf.putDouble(org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c));
            buf.putDouble(c.getPollutionLevel() != null ? c.getPollutionLevel() : 0.0);
        }
        return HexFormat.of().formatHex(digest.digest(buf.array()));
    }
}
