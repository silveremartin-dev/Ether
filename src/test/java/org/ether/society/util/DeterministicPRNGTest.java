/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DeterministicPRNGTest {

    @Test
    public void testStrictBitwiseDeterminism() {
        long seed = 123456789L;
        long tick = 42L;
        long cellIndex = 100L;

        double val1 = DeterministicPRNG.nextDouble(seed, tick, cellIndex);
        double val2 = DeterministicPRNG.nextDouble(seed, tick, cellIndex);

        assertEquals(val1, val2, "Identical inputs must produce identical double bitwise output");
        assertTrue(val1 >= 0.0 && val1 < 1.0);
    }

    @Test
    public void testDifferentCoordinatesProduceDifferentValues() {
        long seed = 123456789L;

        double valA = DeterministicPRNG.nextDouble(seed, 1L, 100L);
        double valB = DeterministicPRNG.nextDouble(seed, 2L, 100L);
        double valC = DeterministicPRNG.nextDouble(seed, 1L, 101L);

        assertNotEquals(valA, valB);
        assertNotEquals(valA, valC);
    }

    @Test
    public void testBoundedIntAndBoolean() {
        long seed = 987654321L;

        int integerVal = DeterministicPRNG.nextInt(seed, 10L, 5L, 100);
        assertTrue(integerVal >= 0 && integerVal < 100);

        boolean alwaysFalse = DeterministicPRNG.nextBoolean(seed, 10L, 5L, 0.0);
        assertFalse(alwaysFalse);

        boolean alwaysTrue = DeterministicPRNG.nextBoolean(seed, 10L, 5L, 1.0);
        assertTrue(alwaysTrue);
    }
}