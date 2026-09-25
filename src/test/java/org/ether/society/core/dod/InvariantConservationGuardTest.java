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

class InvariantConservationGuardTest {

    @Test
    @DisplayName("Verify mass and energy conservation contracts on clean transitions")
    void testCleanConservationTransition() {
        WorldBuffer buffer = new WorldBuffer(10);
        buffer.getH3Indexes()[0] = 0x882681a339fffffL;
        buffer.getBiomassHuman()[0] = 100.0f;
        buffer.getBiomassLivestock()[0] = 50.0f;
        buffer.getFoodResource()[0] = 500.0f;
        buffer.getEnergySolar()[0] = 1000.0f;
        buffer.getResourceCapital()[0] = 200.0f;
        buffer.getGiniIndex()[0] = 0.35f;

        InvariantConservationGuard guard = new InvariantConservationGuard(1e-4, false);
        var snapshot1 = guard.captureSnapshot(buffer);

        assertTrue(snapshot1.boundsValid());
        assertEquals(650.0, snapshot1.totalBiomass(), 1e-4);
        assertEquals(1000.0, snapshot1.totalEnergy(), 1e-4);

        // Simulate step: 50 units of food converted to 50 units of biomass (zero net loss)
        buffer.getFoodResource()[0] = 450.0f;
        buffer.getBiomassHuman()[0] = 150.0f;

        var snapshot2 = guard.captureSnapshot(buffer);
        boolean ok = guard.verifyTransition(snapshot1, snapshot2, 0.0, 0.0);

        assertTrue(ok, "Zero-loss mass transition must pass invariant verification.");
        assertEquals(0, guard.getViolationCount());
    }

    @Test
    @DisplayName("Detect invariant violation on unphysical mass creation or out-of-bound Gini")
    void testViolationDetection() {
        WorldBuffer buffer = new WorldBuffer(10);
        buffer.getH3Indexes()[0] = 0x882681a339fffffL;
        buffer.getBiomassHuman()[0] = 100.0f;
        buffer.getGiniIndex()[0] = 0.35f;

        InvariantConservationGuard guard = new InvariantConservationGuard(1e-4, false);
        var snapshot1 = guard.captureSnapshot(buffer);

        // Violate mass balance: biomass doubles without external input
        buffer.getBiomassHuman()[0] = 500.0f;
        var snapshot2 = guard.captureSnapshot(buffer);

        boolean ok = guard.verifyTransition(snapshot1, snapshot2, 0.0, 0.0);
        assertFalse(ok, "Unphysical biomass creation must fail verification.");
        assertEquals(1, guard.getViolationCount());

        // Violate bounds: negative food or Gini > 1.0
        buffer.getGiniIndex()[0] = 1.5f;
        var snapshot3 = guard.captureSnapshot(buffer);
        assertFalse(snapshot3.boundsValid());
    }
}
