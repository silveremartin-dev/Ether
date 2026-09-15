/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.util;

/**
 * High-Performance Lock-Free Deterministic Pseudo-Random Number Generator.
 * Based on 64-bit SplitMix64 non-linear bit mixing algorithm.
 * Eliminates thread contention, CAS locks, and state sharing while guaranteeing
 * 100% bitwise deterministic reproducibility across concurrent threads.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public final class DeterministicPRNG {

    private static final long GAMMA = 0x9E3779B97F4A7C15L;
    private static final double DOUBLE_UNIT = 0x1.0p-53; // 1.0 / (1L << 53)

    private DeterministicPRNG() {}

    /**
     * Computes a 64-bit pseudo-random hash from coordinates (seed, tick, cellIndex, streamId).
     */
    public static long splitMix64(long seed, long tick, long cellIndex, long streamId) {
        long z = seed + (tick * 0xBF58476D1CE4E5B9L) + (cellIndex * GAMMA) + (streamId * 0x94D049BB133111EBL);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public static long splitMix64(long seed, long tick, long cellIndex) {
        return splitMix64(seed, tick, cellIndex, 0L);
    }

    /**
     * Generates a uniformly distributed double in [0.0, 1.0) deterministically.
     */
    public static double nextDouble(long seed, long tick, long cellIndex, long streamId) {
        long bits = splitMix64(seed, tick, cellIndex, streamId) >>> 11;
        return bits * DOUBLE_UNIT;
    }

    public static double nextDouble(long seed, long tick, long cellIndex) {
        return nextDouble(seed, tick, cellIndex, 0L);
    }

    /**
     * Generates a uniformly distributed integer in [0, bound) deterministically.
     */
    public static int nextInt(long seed, long tick, long cellIndex, int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        double d = nextDouble(seed, tick, cellIndex, 0L);
        return (int) (d * bound);
    }

    /**
     * Generates a boolean outcome based on a given probability [0.0, 1.0].
     */
    public static boolean nextBoolean(long seed, long tick, long cellIndex, double probability) {
        if (probability <= 0.0) return false;
        if (probability >= 1.0) return true;
        return nextDouble(seed, tick, cellIndex) < probability;
    }

    /**
     * Generates a normally distributed standard Gaussian value N(0, 1) using Box-Muller transform.
     */
    public static double nextGaussian(long seed, long tick, long cellIndex) {
        double u1 = Math.max(1e-15, nextDouble(seed, tick, cellIndex, 1L));
        double u2 = nextDouble(seed, tick, cellIndex, 2L);
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }
}