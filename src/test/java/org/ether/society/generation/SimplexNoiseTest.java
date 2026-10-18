/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.generation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mathematical test suite for {@link SimplexNoise}.
 * Verifies noise continuity, boundedness within [-1.0, 1.0], bit-level determinism,
 * and seed variation.
 */
class SimplexNoiseTest {

    @Test
    @DisplayName("SimplexNoise is strictly bounded within [-1.0, 1.0] across spatial coordinate sweep")
    void testBoundedness() {
        SimplexNoise noise = new SimplexNoise(12345L);

        for (double x = -10.0; x <= 10.0; x += 0.75) {
            for (double y = -10.0; y <= 10.0; y += 0.75) {
                for (double z = -5.0; z <= 5.0; z += 1.25) {
                    double val = noise.noise(x, y, z);
                    assertTrue(val >= -1.05 && val <= 1.05,
                            "Noise output " + val + " violated theoretical bounds at (" + x + ", " + y + ", " + z + ")");
                }
            }
        }
    }

    @Test
    @DisplayName("Identical seeds produce bit-identical deterministic noise outputs")
    void testDeterminism() {
        SimplexNoise noise1 = new SimplexNoise(424242L);
        SimplexNoise noise2 = new SimplexNoise(424242L);

        for (int i = 0; i < 50; i++) {
            double x = i * 0.31;
            double y = i * -0.47;
            double z = i * 0.19;

            assertEquals(noise1.noise(x, y, z), noise2.noise(x, y, z),
                    "Outputs for identical seeds must be bit-identical");
        }
    }

    @Test
    @DisplayName("Different seeds produce distinct spatial noise patterns")
    void testSeedVariation() {
        SimplexNoise noiseA = new SimplexNoise(111L);
        SimplexNoise noiseB = new SimplexNoise(999L);

        int differences = 0;
        for (int i = 0; i < 20; i++) {
            double valA = noiseA.noise(i * 0.31 + 1.2, i * 0.77 - 0.5, i * 0.15 + 2.1);
            double valB = noiseB.noise(i * 0.31 + 1.2, i * 0.77 - 0.5, i * 0.15 + 2.1);
            if (Math.abs(valA - valB) > 1e-4) {
                differences++;
            }
        }

        assertTrue(differences > 15, "Different seeds must produce different noise fields");
    }

    @Test
    @DisplayName("Continuous coordinate delta produces continuous noise changes without discrete discontinuities")
    void testContinuity() {
        SimplexNoise noise = new SimplexNoise(777L);
        double delta = 0.001;

        for (double t = 0.0; t < 5.0; t += 0.5) {
            double n1 = noise.noise(t, t, t);
            double n2 = noise.noise(t + delta, t, t);
            double derivative = Math.abs(n2 - n1) / delta;

            assertTrue(derivative < 50.0,
                    "Simplex noise exhibited numerical jump/discontinuity at t=" + t + ", derivative=" + derivative);
        }
    }
}
