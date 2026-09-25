/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LyapunovChaosTrackerEngineTest {

    @Test
    @DisplayName("Track exponentially diverging chaotic system and detect positive Lyapunov exponent")
    void testChaoticDivergenceDetection() {
        LyapunovChaosTrackerEngine tracker = new LyapunovChaosTrackerEngine(1e-4);

        double[] baseState = new double[]{1.0, 2.0, 3.0};
        double[] shadowState = new double[]{1.0 + 1e-4, 2.0, 3.0};

        // Simulate 20 steps of exponential divergence: delta(t) = delta(0) * e^(0.2 * t)
        for (int step = 0; step < 30; step++) {
            // Apply exponential separation before tracking
            for (int i = 0; i < shadowState.length; i++) {
                double diff = shadowState[i] - baseState[i];
                shadowState[i] = baseState[i] + diff * Math.exp(0.15); // positive growth
            }
            shadowState = tracker.trackStep(baseState, shadowState, 1.0);
        }

        assertTrue(tracker.getCurrentLyapunovExponent() > 0.05, "Exponentially separating trajectories must yield positive Lyapunov exponent.");
        assertEquals(LyapunovChaosTrackerEngine.DynamicRegime.DETERMINISTIC_CHAOS, tracker.classifyRegime());
    }

    @Test
    @DisplayName("Track contractive stable attractor and detect negative Lyapunov exponent")
    void testStableAttractorDetection() {
        LyapunovChaosTrackerEngine tracker = new LyapunovChaosTrackerEngine(1e-4);

        double[] baseState = new double[]{1.0, 2.0, 3.0};
        double[] shadowState = new double[]{1.0 + 1e-4, 2.0, 3.0};

        for (int step = 0; step < 30; step++) {
            // Apply contraction
            for (int i = 0; i < shadowState.length; i++) {
                double diff = shadowState[i] - baseState[i];
                shadowState[i] = baseState[i] + diff * Math.exp(-0.2); // negative contraction
            }
            shadowState = tracker.trackStep(baseState, shadowState, 1.0);
        }

        assertTrue(tracker.getCurrentLyapunovExponent() < -0.05, "Contracting trajectories must yield negative Lyapunov exponent.");
        assertEquals(LyapunovChaosTrackerEngine.DynamicRegime.STABLE_ATTRACTOR, tracker.classifyRegime());
    }
}
