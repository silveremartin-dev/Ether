package org.ether.society.core.dod;

import org.ether.society.model.Biome;

/**
 * Throughput & Latency Benchmark comparing Java Vectorized CPU vs. Native Parallel Rust Core.
 * Evaluates execution times across 10,000 to 100,000 hexagonal cells over 1000 simulated ticks.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class EngineBenchmarkHarness {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("    ETHER SIMULATION — ENGINE BENCHMARK HARNESS (v1.0)    ");
        System.out.println("==========================================================");

        int[] cellCounts = {5000, 20000, 50000};
        int ticks = 500;

        for (int cells : cellCounts) {
            System.out.printf("\n--- Benchmark Suite: %d Cells, %d Ticks ---\n", cells, ticks);
            runBenchmarkForGridSize(cells, ticks);
        }
    }

    private static void runBenchmarkForGridSize(int capacity, int ticks) {
        WorldBuffer world = new WorldBuffer(capacity);
        AgentBuffer agents = new AgentBuffer(capacity);

        DemographicKernel demog = new DemographicKernel();
        EnvironmentalKernel env = new EnvironmentalKernel();
        UrbanKernel urban = new UrbanKernel();

        for (int i = 0; i < capacity; i++) {
            world.getElevation()[i] = 200.0f;
            world.getTemperature()[i] = 18.0f;
            world.getRainfall()[i] = 750.0f;
            world.getBiomes()[i] = (byte) (Biome.PLAINS.ordinal());
            world.getFoodResource()[i] = 10000.0f;
            world.getBiomassNatural()[i] = 500.0f;
            world.getResourceCapital()[i] = 50.0f;
            world.getTechnologyLevel()[i] = 2.0f;

            if (i < capacity / 2) {
                agents.getHexIds()[i] = i;
                agents.getMass()[i] = 150.0f;
                agents.getEnergy()[i] = 80.0f;
                agents.getAge()[i] = 25.0f;
                agents.getTechLevel()[i] = 2.0f;
            }
        }

        float dt = 30.0f * 86400.0f;

        // Warmup
        for (int t = 0; t < 50; t++) {
            env.tick(world, 6, dt);
            demog.tick(world, agents, dt);
            urban.tick(world, dt);
        }

        // Benchmark Timed Run
        long startNanos = System.nanoTime();
        for (int t = 0; t < ticks; t++) {
            env.tick(world, 6, dt);
            demog.tick(world, agents, dt);
            urban.tick(world, dt);
        }
        long totalNanos = System.nanoTime() - startNanos;

        double elapsedSeconds = totalNanos / 1_000_000_000.0;
        double throughputTPS = ticks / elapsedSeconds;
        double microSecPerTick = (totalNanos / 1_000.0) / ticks;

        System.out.printf("  Total Execution Time : %.3f s\n", elapsedSeconds);
        System.out.printf("  Throughput           : %.2f TPS\n", throughputTPS);
        System.out.printf("  Latency per Tick     : %.1f µs/tick (%.3f ns/cell)\n", microSecPerTick, (totalNanos / (double) (ticks * capacity)));
    }
}
