/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rigorous Unit & Numerical Invariance Test for {@link CultureKernel}.
 * Verifies Langevin SDE stochastic drift, memetic spatial diffusion across neighboring hexes,
 * cultural tensor forcing, non-negativity, and boundary conditions $\in [0, 1]$.
 */
public class CultureKernelTest {

    @Test
    @DisplayName("Verify CultureKernel Langevin SDE drift and bounds in [0.0, 1.0]")
    public void testLangevinDriftAndBounds() {
        CultureKernel kernel = new CultureKernel();
        kernel.setMutationRate(0.05f);
        kernel.setDiffusionRate(0.0f); // Isolate drift
        kernel.setForcingRate(0.0f);

        int capacity = 100;
        WorldBuffer world = new WorldBuffer(capacity);
        AgentBuffer agents = new AgentBuffer(capacity);

        // Populate agents
        for (int i = 0; i < capacity; i++) {
            agents.getHexIds()[i] = i;
            for (int d = 0; d < 4; d++) {
                agents.getCulture()[d][i] = 0.5f;
            }
        }

        // Run 50 ticks
        for (int t = 0; t < 50; t++) {
            kernel.tick(world, agents, 1.0f);
        }

        // Assert all values strictly in [0.0, 1.0], no NaN/Inf, and non-zero drift occurred
        boolean driftOccurred = false;
        for (int i = 0; i < capacity; i++) {
            for (int d = 0; d < 4; d++) {
                float val = agents.getCulture()[d][i];
                assertFalse(Float.isNaN(val), "Culture value must not be NaN at agent " + i);
                assertFalse(Float.isInfinite(val), "Culture value must not be Infinite at agent " + i);
                assertTrue(val >= 0.0f && val <= 1.0f, "Culture value must remain bounded in [0, 1]");
                if (Math.abs(val - 0.5f) > 1e-4) {
                    driftOccurred = true;
                }
            }
        }
        assertTrue(driftOccurred, "Langevin stochastic drift must alter initial uniform culture values.");
    }

    @Test
    @DisplayName("Verify Memetic Diffusion between neighboring hexes")
    public void testMemeticDiffusionBetweenNeighbors() {
        CultureKernel kernel = new CultureKernel();
        kernel.setMutationRate(0.0f); // Zero noise to isolate pure diffusion
        kernel.setDiffusionRate(0.5f);
        kernel.setForcingRate(0.0f);

        int capacity = 7;
        WorldBuffer world = new WorldBuffer(capacity);
        AgentBuffer agents = new AgentBuffer(capacity);

        // Center hex 0 has culture 1.0, 6 surrounding neighbors have culture 0.0
        // Set neighbor topology: hex 0 neighbors are 1..6
        int[][] neighbors = world.getNeighborIndexes();
        for (int j = 0; j < 6; j++) {
            neighbors[0][j] = j + 1;
            neighbors[j + 1][0] = 0; // Reciprocal link
            for (int k = 1; k < 6; k++) {
                neighbors[j + 1][k] = -1;
            }
        }

        // Assign 1 agent per hex
        for (int i = 0; i < capacity; i++) {
            agents.getHexIds()[i] = i;
            for (int d = 0; d < 4; d++) {
                agents.getCulture()[d][i] = (i == 0) ? 1.0f : 0.0f;
            }
        }

        float initialCenter = agents.getCulture()[0][0];
        float initialNeighbor = agents.getCulture()[0][1];
        assertEquals(1.0f, initialCenter, 1e-5f);
        assertEquals(0.0f, initialNeighbor, 1e-5f);

        // Tick diffusion
        kernel.tick(world, agents, 1.0f);

        float postCenter = agents.getCulture()[0][0];
        float postNeighbor = agents.getCulture()[0][1];

        // Center should diffuse outward (decrease), neighbor should absorb culture (increase)
        assertTrue(postCenter < initialCenter, "Center hex culture must decrease as it diffuses outward");
        assertTrue(postNeighbor > initialNeighbor, "Neighbor hex culture must increase as it absorbs culture");
        assertTrue(postCenter >= 0.0f && postNeighbor <= 1.0f, "Culture values must remain within [0, 1]");
    }

    @Test
    @DisplayName("Verify Setter / Getter configuration and non-negative clamping")
    public void testConfigurationClamping() {
        CultureKernel kernel = new CultureKernel();
        kernel.setDiffusionRate(-0.1f);
        kernel.setMutationRate(-0.05f);
        kernel.setForcingRate(-0.02f);

        assertEquals(0.0f, kernel.getDiffusionRate(), 1e-6f, "Diffusion rate must be clamped to non-negative");
        assertEquals(0.0f, kernel.getMutationRate(), 1e-6f, "Mutation rate must be clamped to non-negative");
        assertEquals(0.0f, kernel.getForcingRate(), 1e-6f, "Forcing rate must be clamped to non-negative");

        kernel.setDiffusionRate(0.12f);
        kernel.setMutationRate(0.03f);
        kernel.setForcingRate(0.07f);

        assertEquals(0.12f, kernel.getDiffusionRate(), 1e-6f);
        assertEquals(0.03f, kernel.getMutationRate(), 1e-6f);
        assertEquals(0.07f, kernel.getForcingRate(), 1e-6f);
    }
}
