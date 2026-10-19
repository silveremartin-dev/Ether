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
 * Rigorous physical verification and validation suite for {@link FluxKernel}.
 * Verifies finite-volume mass conservation (\(\Phi_{ij} = -\Phi_{ji}\)),
 * price gradient driven Onsager diffusion, and topographic friction impedance.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class FluxKernelTest {

    @Test
    @DisplayName("Thermodynamic Mass Conservation: Total global food stock is conserved exactly (zero net drift)")
    public void testStrictMassConservation() {
        int capacity = 7; // Central cell + 6 hexagonal neighbors
        WorldBuffer world = new WorldBuffer(capacity);

        float[] food = world.getFoodResource();
        float[] pop = world.getBiomassHuman();
        float[] elev = world.getElevation();
        int[][] neighbors = world.getNeighborIndexes();

        // Setup asymmetric initial conditions
        food[0] = 1000.0f; // High food exporter
        pop[0] = 50.0f;

        for (int i = 1; i < capacity; i++) {
            food[i] = 100.0f; // Low food importer
            pop[i] = 200.0f;
            elev[i] = 10.0f;
        }

        // Establish symmetric adjacency topology: cell 0 is connected to 1..6, and each 1..6 is connected back to 0
        neighbors[0] = new int[] {1, 2, 3, 4, 5, 6};
        for (int i = 1; i < capacity; i++) {
            neighbors[i] = new int[] {0, -1, -1, -1, -1, -1};
        }

        float initialTotalFood = 0.0f;
        for (int i = 0; i < capacity; i++) {
            initialTotalFood += food[i];
        }

        FluxKernel kernel = new FluxKernel(0.1f);

        // Run 30 daily integration cycles
        for (int step = 0; step < 30; step++) {
            kernel.tick(world, 86400.0f); // 1 day in seconds
        }

        float finalTotalFood = 0.0f;
        for (int i = 0; i < capacity; i++) {
            finalTotalFood += food[i];
            assertTrue(food[i] >= 0.0f, "Food stock must remain strictly non-negative at cell " + i);
        }

        // Total food mass must be conserved within machine floating-point precision
        assertEquals(initialTotalFood, finalTotalFood, 1e-3f,
                "Global finite-volume trade flux must conserve 100% of physical food mass.");
        
        // Exporter food should have decreased and importer food should have increased
        assertTrue(food[0] < 1000.0f, "Central surplus producer must diffuse food towards hungry neighbors.");
        assertTrue(food[1] > 100.0f, "Peripheral consumer cell must receive imported food.");
    }

    @Test
    @DisplayName("Topographic Friction: Mountain elevation gradient impedes trade conductivity")
    public void testTopographicFrictionImpedance() {
        int capacity = 3;
        WorldBuffer flatWorld = new WorldBuffer(capacity);
        WorldBuffer mountainWorld = new WorldBuffer(capacity);

        // Flat world: elev = 0 everywhere
        flatWorld.getFoodResource()[0] = 1000.0f;
        flatWorld.getFoodResource()[1] = 100.0f;
        flatWorld.getBiomassHuman()[0] = 10.0f;
        flatWorld.getBiomassHuman()[1] = 500.0f;
        flatWorld.getNeighborIndexes()[0] = new int[] {1, -1, -1, -1, -1, -1};
        flatWorld.getNeighborIndexes()[1] = new int[] {0, -1, -1, -1, -1, -1};

        // Mountain world: massive elevation gradient Delta h = 3000m between 0 and 1
        mountainWorld.getFoodResource()[0] = 1000.0f;
        mountainWorld.getFoodResource()[1] = 100.0f;
        mountainWorld.getBiomassHuman()[0] = 10.0f;
        mountainWorld.getBiomassHuman()[1] = 500.0f;
        mountainWorld.getElevation()[0] = 0.0f;
        mountainWorld.getElevation()[1] = 3000.0f;
        mountainWorld.getNeighborIndexes()[0] = new int[] {1, -1, -1, -1, -1, -1};
        mountainWorld.getNeighborIndexes()[1] = new int[] {0, -1, -1, -1, -1, -1};

        FluxKernel kernel = new FluxKernel(0.05f);

        kernel.tick(flatWorld, 86400.0f);
        kernel.tick(mountainWorld, 86400.0f);

        float flatTransfer = 1000.0f - flatWorld.getFoodResource()[0];
        float mountainTransfer = 1000.0f - mountainWorld.getFoodResource()[0];

        assertTrue(flatTransfer > mountainTransfer,
                "Mountainous topography must impose frictional drag, reducing Onsager trade flux.");
    }
}
