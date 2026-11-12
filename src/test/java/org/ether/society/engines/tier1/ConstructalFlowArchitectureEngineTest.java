/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * High-Precision Verification & Large-Scale Spatial Test Suite for {@link ConstructalFlowArchitectureEngine}.
 *
 * <p>Validates Adrian Bejan (1996, 2016) Constructal Law across:
 * <ul>
 *   <li>Allometric cruising speed scaling ($V \propto M^{1/6}$) across aerial, terrestrial, and aquatic mediums.</li>
 *   <li>Murray's Law ($d_0^3 = \sum d_i^3$) cubic vascular and hydraulic tree minimization.</li>
 *   <li>Hack's Law ($L \propto A^{0.57}$) river catchment stream power length scaling.</li>
 *   <li>500-cell spatial domain continuous friction minimization.</li>
 *   <li>Bitwise determinism and numerical stability.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
class ConstructalFlowArchitectureEngineTest {

    @Test
    @DisplayName("4.1 Constructal Locomotion: Cruising velocity scales strictly with M^1/6 across land, air, and water")
    void testConstructalLocomotionScaling() {
        // Test masses spanning 5 orders of magnitude: 0.050 kg to 500 kg
        double massSmall = 0.050; // 50 grams
        double massLarge = 500.0; // 500 kg

        double vRunSmall = ConstructalFlowArchitectureEngine.calculateConstructalSpeedMS(massSmall, ConstructalFlowArchitectureEngine.LocomotionMedium.TERRESTRIAL_RUNNING);
        double vRunLarge = ConstructalFlowArchitectureEngine.calculateConstructalSpeedMS(massLarge, ConstructalFlowArchitectureEngine.LocomotionMedium.TERRESTRIAL_RUNNING);

        assertTrue(vRunLarge > vRunSmall, "Larger animals must attain higher constructal cruising speeds");

        // Theoretical scaling ratio: (500 / 0.050)^(1/6) = (10,000)^(1/6) ~ 4.6416
        double expectedRatio = Math.pow(massLarge / massSmall, 1.0 / 6.0);
        double observedRatio = vRunLarge / vRunSmall;
        assertEquals(expectedRatio, observedRatio, 1e-4, "Locomotion speed must scale exactly as M^(1/6)");

        // Air velocity > Land velocity > Water velocity for identical mass
        double vFly = ConstructalFlowArchitectureEngine.calculateConstructalSpeedMS(10.0, ConstructalFlowArchitectureEngine.LocomotionMedium.AERIAL_FLYING);
        double vRun = ConstructalFlowArchitectureEngine.calculateConstructalSpeedMS(10.0, ConstructalFlowArchitectureEngine.LocomotionMedium.TERRESTRIAL_RUNNING);
        double vSwim = ConstructalFlowArchitectureEngine.calculateConstructalSpeedMS(10.0, ConstructalFlowArchitectureEngine.LocomotionMedium.AQUATIC_SWIMMING);

        assertTrue(vFly > vRun, "Aerial fliers cruise faster than terrestrial runners");
        assertTrue(vRun > vSwim, "Terrestrial runners cruise faster than aquatic swimmers");
    }

    @Test
    @DisplayName("4.2 Murray's Law: Hydraulic bifurcation minimizes dissipation when parent cube equals sum of daughter cubes")
    void testMurraysLawBifurcation() {
        double[] daughters = { 10.0, 10.0 };
        double parentD = ConstructalFlowArchitectureEngine.calculateMurrayOptimalParentDiameter(daughters);

        // Expected: (10^3 + 10^3)^(1/3) = (2000)^(1/3) = 12.599 mm
        double expected = Math.cbrt(2000.0);
        assertEquals(expected, parentD, 1e-3, "Parent diameter must satisfy Murray's cubic conservation");
    }

    @Test
    @DisplayName("4.3 Hack's Law: Mainstream drainage channel length scales as A^0.57 with catchment area")
    void testHacksLawRiverCatchment() {
        double basinSmall = 100.0; // 100 km2
        double basinLarge = 10000.0; // 10,000 km2

        double lSmall = ConstructalFlowArchitectureEngine.calculateHacksStreamLengthKm(basinSmall);
        double lLarge = ConstructalFlowArchitectureEngine.calculateHacksStreamLengthKm(basinLarge);

        double expectedRatio = Math.pow(basinLarge / basinSmall, 0.57);
        double observedRatio = lLarge / lSmall;
        assertEquals(expectedRatio, observedRatio, 1e-3, "Hack's law scaling exponent must be exactly 0.57");
    }

    @Test
    @DisplayName("4.4 500-Cell Spatial Domain: Continuous Constructal Friction Minimization across Topography")
    void test500CellConstructalSimulation() {
        org.ether.society.core.dod.WorldBuffer largeGrid = new org.ether.society.core.dod.WorldBuffer(500);
        for (int i = 0; i < 500; i++) {
            largeGrid.getH3Indexes()[i] = 0x88b000000000000L + i;
            largeGrid.getBiomassNatural()[i] = 150.0f;
            largeGrid.getFluxPressure()[i] = 1.0f;
        }

        ConstructalFlowArchitectureEngine engine = new ConstructalFlowArchitectureEngine();
        for (int yr = 1; yr <= 10; yr++) {
            engine.tick(largeGrid, 1.0, 12.0);

            for (int i = 0; i < 500; i++) {
                float f = largeGrid.getFluxPressure()[i];
                assertTrue(f <= 1.0f, "Constructal movement must reduce corridor friction");
                assertTrue(f >= 0.20f, "Friction must remain bounded by physical topography");
                assertFalse(Float.isNaN(f), "Friction must never be NaN");
            }
        }
    }

    @Test
    @DisplayName("4.5 Bitwise Determinism across Repeated Constructal Flow Executions")
    void testBitwiseDeterminismConstructal() {
        org.ether.society.core.dod.WorldBuffer runA = new org.ether.society.core.dod.WorldBuffer(100);
        org.ether.society.core.dod.WorldBuffer runB = new org.ether.society.core.dod.WorldBuffer(100);

        for (int i = 0; i < 100; i++) {
            runA.getH3Indexes()[i] = 0x88c000000000000L + i;
            runA.getBiomassNatural()[i] = 120.0f + i;
            runA.getFluxPressure()[i] = 0.85f;

            runB.getH3Indexes()[i] = 0x88c000000000000L + i;
            runB.getBiomassNatural()[i] = 120.0f + i;
            runB.getFluxPressure()[i] = 0.85f;
        }

        ConstructalFlowArchitectureEngine engine = new ConstructalFlowArchitectureEngine();
        for (int yr = 0; yr < 5; yr++) {
            engine.tick(runA, 1.0, 12.0);
            engine.tick(runB, 1.0, 12.0);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(runA.getFluxPressure()[i], runB.getFluxPressure()[i], 1e-12,
                    "Movement friction must be bitwise identical across deterministic runs");
        }
    }
}
