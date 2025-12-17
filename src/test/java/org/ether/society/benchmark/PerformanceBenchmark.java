package org.ether.society.benchmark;

import org.ether.society.data.SampleDataGenerator;
import org.ether.society.database.H3Cell;

import java.util.ArrayList;
import java.util.List;

/**
 * Performance benchmarks for data generation.
 * 
 * Run with: mvn test -Dtest=PerformanceBenchmark
 */
public class PerformanceBenchmark {

    private static final int WARMUP_ITERATIONS = 3;
    private static final int BENCHMARK_ITERATIONS = 10;

    public static void main(String[] args) {
        System.out.println("=== Ether Simulation Performance Benchmark ===\n");
        benchmarkDataGeneration();
    }

    private static void benchmarkDataGeneration() {
        System.out.println("## Data Generation Benchmark");

        // Warmup
        System.out.println("Warming up...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            SampleDataGenerator.generateEuropeSample();
        }

        // Benchmark
        List<Long> times = new ArrayList<>();
        List<H3Cell> cells = null;

        System.out.println("Running benchmark...");
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            cells = SampleDataGenerator.generateEuropeSample();
            long end = System.nanoTime();

            long durationMs = (end - start) / 1_000_000;
            times.add(durationMs);
            System.out.printf("  Iteration %d: %d ms%n", i + 1, durationMs);
        }

        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
        long min = times.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = times.stream().mapToLong(Long::longValue).max().orElse(0);

        System.out.println("\n### Results:");
        System.out.println("  Cell count: " + cells.size());
        System.out.println("  Average: " + String.format("%.2f", avg) + " ms");
        System.out.println("  Min: " + min + " ms");
        System.out.println("  Max: " + max + " ms");
        System.out.println("  Target: <3000 ms");
        System.out.println("  Status: " + (avg < 3000 ? "✅ PASS" : "❌ FAIL"));

        // Memory stats
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        System.out.println("\n### Memory:");
        System.out.println("  Used: " + usedMemory + " MB");
        System.out.println("  Total: " + runtime.totalMemory() / (1024 * 1024) + " MB");
        System.out.println("  Max: " + runtime.maxMemory() / (1024 * 1024) + " MB");
    }
}
