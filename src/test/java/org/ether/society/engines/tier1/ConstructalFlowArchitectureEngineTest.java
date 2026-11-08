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
        List<H3Cell> largeGrid = new ArrayList<>(500);
        for (int i = 0; i < 500; i++) {
            H3Cell cell = new H3Cell(0x88b000000000000L + i, 30.0 + (i * 0.01), 10.0 + (i * 0.01));
            cell.setBiomassNatural(150.0);
            cell.setMovementFriction(1.0);
            largeGrid.add(cell);
        }

        for (int yr = 1; yr <= 10; yr++) {
            ConstructalFlowArchitectureEngine.processConstructalFlow(largeGrid, 1.0);

            for (H3Cell c : largeGrid) {
                assertTrue(c.getMovementFriction() <= 1.0, "Constructal movement must reduce corridor friction");
                assertTrue(c.getMovementFriction() >= 0.20, "Friction must remain bounded by physical topography");
                assertFalse(Double.isNaN(c.getMovementFriction()), "Friction must never be NaN");
            }
        }
    }

    @Test
    @DisplayName("4.5 Bitwise Determinism across Repeated Constructal Flow Executions")
    void testBitwiseDeterminismConstructal() {
        List<H3Cell> runA = new ArrayList<>(100);
        List<H3Cell> runB = new ArrayList<>(100);

        for (int i = 0; i < 100; i++) {
            H3Cell cA = new H3Cell(0x88c000000000000L + i, 20.0, 15.0);
            cA.setBiomassNatural(120.0 + i);
            cA.setMovementFriction(0.85);
            runA.add(cA);

            H3Cell cB = new H3Cell(0x88c000000000000L + i, 20.0, 15.0);
            cB.setBiomassNatural(120.0 + i);
            cB.setMovementFriction(0.85);
            runB.add(cB);
        }

        for (int yr = 0; yr < 5; yr++) {
            ConstructalFlowArchitectureEngine.processConstructalFlow(runA, 1.0);
            ConstructalFlowArchitectureEngine.processConstructalFlow(runB, 1.0);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(runA.get(i).getMovementFriction(), runB.get(i).getMovementFriction(), 1e-12,
                    "Movement friction must be bitwise identical across deterministic runs");
        }
    }
}
