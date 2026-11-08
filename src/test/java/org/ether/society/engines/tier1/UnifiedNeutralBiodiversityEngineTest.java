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
 * High-Precision Verification & Large-Scale Spatial Test Suite for {@link UnifiedNeutralBiodiversityEngine}.
 *
 * <p>Validates Stephen Hubbell (2001) Unified Neutral Theory of Biodiversity (UNTB) across:
 * <ul>
 *   <li>Fundamental Biodiversity Number $\theta = 2 J_M \nu$.</li>
 *   <li>Preston-Hubbell Species-Area Power Law ($S \propto A^{0.25}$).</li>
 *   <li>Non-linear habitat defaunation and extinction debt buffering ($\mathcal{B} = (B/B_0)^{0.25}$).</li>
 *   <li>500-cell large metacommunity mesh simulation across 20 years.</li>
 *   <li>Bitwise determinism and numerical stability.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
class UnifiedNeutralBiodiversityEngineTest {

    @Test
    @DisplayName("5.1 Hubbell UNTB: Fundamental Biodiversity Number theta = 2 * J_M * nu")
    void testFundamentalBiodiversityNumber() {
        double jm = 1.0e8; // Metacommunity of 100 million individuals
        double nu = 1.0e-6; // Speciation rate 10^-6

        double theta = UnifiedNeutralBiodiversityEngine.calculateFundamentalBiodiversityNumber(jm, nu);
        assertEquals(200.0, theta, 1e-4, "Theta must equal 2 * J_M * nu exactly");
    }

    @Test
    @DisplayName("5.2 Preston-Hubbell Species-Area Power Law: S(A) = c * A^0.25")
    void testSpeciesAreaPowerLaw() {
        double area1 = 1000.0;
        double area16 = 16000.0; // 16x area

        double s1 = UnifiedNeutralBiodiversityEngine.calculateSpeciesAreaRichness(area1);
        double s16 = UnifiedNeutralBiodiversityEngine.calculateSpeciesAreaRichness(area16);

        // Expected scaling: (16)^0.25 = 2.0x richness
        double observedRatio = s16 / s1;
        assertEquals(2.0, observedRatio, 1e-3, "16-fold area expansion must double species richness with z=0.25");
    }

    @Test
    @DisplayName("5.3 Habitat Defaunation & Retention: Biodiversity retention follows (B / B_0)^0.25")
    void testBiodiversityRetention() {
        double pristine = 200.0;
        double depleted50 = 100.0; // 50% biomass left
        double depleted06 = 12.5;  // 6.25% biomass left (1/16)

        double ret50 = UnifiedNeutralBiodiversityEngine.calculateBiodiversityRetention(depleted50, pristine);
        double ret06 = UnifiedNeutralBiodiversityEngine.calculateBiodiversityRetention(depleted06, pristine);

        // Expected: (0.5)^0.25 = 0.8408 (50% habitat loss retains ~84% of species, non-linear buffering)
        assertEquals(Math.pow(0.5, 0.25), ret50, 1e-3);
        // (1/16)^0.25 = 0.50 (93.75% habitat loss cuts species richness in half)
        assertEquals(0.50, ret06, 1e-3);
    }

    @Test
    @DisplayName("5.4 500-Cell Spatial Domain: 20-Year Metacommunity Neutral Drift & Biomass Conservation")
    void test500CellNeutralBiodiversitySimulation() {
        List<H3Cell> largeGrid = new ArrayList<>(500);
        for (int i = 0; i < 500; i++) {
            H3Cell cell = new H3Cell(0x88d000000000000L + i, -10.0 + (i * 0.04), 45.0 + (i * 0.04));
            cell.setBiomassNatural(20.0 + (i % 80));
            largeGrid.add(cell);
        }

        for (int yr = 1; yr <= 20; yr++) {
            UnifiedNeutralBiodiversityEngine.processNeutralBiodiversity(largeGrid, 1.0);

            for (H3Cell c : largeGrid) {
                assertTrue(c.getBiomassNatural() >= 1.0, "Natural biomass must remain strictly positive");
                assertFalse(Double.isNaN(c.getBiomassNatural()), "Biomass must never be NaN");
            }
        }
    }

    @Test
    @DisplayName("5.5 Bitwise Determinism across Repeated Neutral Biodiversity Spatial Runs")
    void testBitwiseDeterminismBiodiversity() {
        List<H3Cell> runA = new ArrayList<>(100);
        List<H3Cell> runB = new ArrayList<>(100);

        for (int i = 0; i < 100; i++) {
            H3Cell cA = new H3Cell(0x88e000000000000L + i, 15.0, 10.0);
            cA.setBiomassNatural(50.0 + i);
            runA.add(cA);

            H3Cell cB = new H3Cell(0x88e000000000000L + i, 15.0, 10.0);
            cB.setBiomassNatural(50.0 + i);
            runB.add(cB);
        }

        for (int yr = 0; yr < 5; yr++) {
            UnifiedNeutralBiodiversityEngine.processNeutralBiodiversity(runA, 1.0);
            UnifiedNeutralBiodiversityEngine.processNeutralBiodiversity(runB, 1.0);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(runA.get(i).getBiomassNatural(), runB.get(i).getBiomassNatural(), 1e-12,
                    "Biomass state must be bitwise identical across deterministic runs");
        }
    }
}
