/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rigorous Unit & Invariant Verification Suite for {@link StatisticsKernel}.
 * Tests Gini index analytical formula, histogram binning, and numerical stability.
 */
public class StatisticsKernelTest {

    private final StatisticsKernel kernel = new StatisticsKernel();

    @Test
    @DisplayName("Verify Gini Index: Perfect equality must equal 0.0")
    void testGiniPerfectEquality() {
        float[] equalWealth = new float[]{100.0f, 100.0f, 100.0f, 100.0f, 100.0f};
        float gini = kernel.calculateGini(equalWealth);
        assertEquals(0.0f, gini, 1e-4f, "Perfect equality distribution must have Gini = 0.0");
    }

    @Test
    @DisplayName("Verify Gini Index: Maximum inequality converges towards (n-1)/n")
    void testGiniExtremeInequality() {
        // In a population of 100 where 1 person has all wealth and 99 have 0: Gini = 99/100 = 0.99
        int n = 100;
        float[] extremeWealth = new float[n];
        extremeWealth[n - 1] = 10_000.0f;

        float gini = kernel.calculateGini(extremeWealth);
        assertEquals(0.99f, gini, 1e-2f, "Extreme concentration with N=100 must yield Gini close to 0.99");
    }

    @Test
    @DisplayName("Verify Gini robustness against NaNs, infinities, and empty buffers")
    void testGiniRobustness() {
        assertEquals(0.0f, kernel.calculateGini(null), "Null array must return Gini 0.0");
        assertEquals(0.0f, kernel.calculateGini(new float[0]), "Empty array must return Gini 0.0");

        float[] corrupted = new float[]{Float.NaN, Float.POSITIVE_INFINITY, -50.0f, 0.0f};
        assertDoesNotThrow(() -> {
            float gini = kernel.calculateGini(corrupted);
            assertTrue(gini >= 0.0f && gini <= 1.0f, "Gini must remain strictly clamped to [0, 1]");
        });
    }

    @Test
    @DisplayName("Verify histogram distribution binning and bounds preservation")
    void testDistributionBinning() {
        float[] values = new float[]{5.0f, 15.0f, 25.0f, 35.0f, 45.0f, 95.0f};
        int bins = 5;
        float maxVal = 100.0f; // Bins: [0-20), [20-40), [40-60), [60-80), [80-100]

        int[] dist = kernel.calculateDistribution(values, bins, maxVal);
        assertNotNull(dist);
        assertEquals(bins, dist.length, "Histogram must have requested number of bins");

        int totalCount = 0;
        for (int c : dist) totalCount += c;
        assertEquals(values.length, totalCount, "Sum of bin counts must equal array length");
    }
}
