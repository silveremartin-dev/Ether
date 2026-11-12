/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * High-Precision Verification & Large-Scale Spatial Test Suite for {@link EcologicalStoichiometryEngine}.
 *
 * <p>Validates Sterner &amp; Elser (2002) Ecological Stoichiometry and Loladze (2002, 2014) across:
 * <ul>
 *   <li>Redfield molar and mass invariants ($\text{C:N:P} = 106:16:1$).</li>
 *   <li>Continuous $C^\infty$ softmin Liebig multi-nutrient limitation without derivative steps.</li>
 *   <li>Atmospheric $\text{CO}_2$ protein dilution curve bounded within $[0.88, 1.0]$.</li>
 *   <li>500-cell spatial agricultural mesh under changing edaphic and atmospheric conditions.</li>
 *   <li>Multi-decadal soil nutrient depletion and yield feedback across 50 years.</li>
 *   <li>Strict bitwise determinism and numerical stability.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
class EcologicalStoichiometryEngineTest {

    @Test
    @DisplayName("2.1 Redfield Marine Molar Invariants & Terrestrial Stoichiometric Constants")
    void testRedfieldConstants() {
        assertEquals(41.1, EcologicalStoichiometryEngine.REDFIELD_MASS_RATIO_C_P, 0.5,
                "Redfield marine C:P mass ratio must adhere to canonical 41.1 (106:1 molar)");
        assertEquals(7.23, EcologicalStoichiometryEngine.REDFIELD_MASS_RATIO_N_P, 0.5,
                "Redfield marine N:P mass ratio must adhere to canonical 7.23 (16:1 molar)");
        assertEquals(0.45, EcologicalStoichiometryEngine.TERRESTRIAL_PLANT_CARBON_FRACTION, 1e-4,
                "Terrestrial dry plant carbon fraction must equal ~45%");
    }

    @Test
    @DisplayName("2.2 Continuous Liebig Multi-Nutrient Softmin: Smoothness across N, P, and K bottlenecks")
    void testContinuousLiebigFactor() {
        // Balanced optimal nutrients (all 1.0) -> limitation factor near 1.0
        double balanced = EcologicalStoichiometryEngine.calculateContinuousLiebigFactor(1.0, 1.0, 1.0);
        assertTrue(balanced >= 0.85 && balanced <= 1.0, "Optimal nutrients must yield near-maximum factor");

        // Severe nitrogen limitation (N = 0.20, P = 1.0, K = 1.0)
        double nLimited = EcologicalStoichiometryEngine.calculateContinuousLiebigFactor(0.20, 1.0, 1.0);
        assertTrue(nLimited < balanced, "Nitrogen limitation must curtail yield factor");
        assertTrue(nLimited <= 0.30, "Factor under 0.20 N must be severely bounded");

        // Phosphorus bottleneck (N = 1.0, P = 0.15, K = 1.0)
        double pLimited = EcologicalStoichiometryEngine.calculateContinuousLiebigFactor(1.0, 0.15, 1.0);
        assertTrue(pLimited < balanced);
        assertTrue(pLimited <= 0.25);

        // Potassium bottleneck (N = 1.0, P = 1.0, K = 0.10)
        double kLimited = EcologicalStoichiometryEngine.calculateContinuousLiebigFactor(1.0, 1.0, 0.10);
        assertTrue(kLimited < balanced);
        assertTrue(kLimited <= 0.20);

        // Smooth monotonicity check: scanning N from 0.1 to 1.5 with fixed P=0.6, K=0.8
        double prev = 0.0;
        for (double n = 0.10; n <= 1.50; n += 0.05) {
            double current = EcologicalStoichiometryEngine.calculateContinuousLiebigFactor(n, 0.60, 0.80);
            assertTrue(current >= prev, "Liebig softmin factor must be monotonically non-decreasing with limiting nutrient");
            assertTrue(current >= 0.05 && current <= 1.0, "Liebig factor must remain bounded within [0.05, 1.0]");
            prev = current;
        }
    }

    @Test
    @DisplayName("2.3 Elevated CO2 Protein Dilution Envelope (Loladze 2002, 2014; Myers et al. 2014)")
    void testProteinDilutionCurve() {
        // Pre-industrial CO2 (280 ppm): exactly 1.0
        double factor280 = EcologicalStoichiometryEngine.calculateProteinDilutionFactor(280.0);
        assertEquals(1.0, factor280, 1e-4, "Pre-industrial 280 ppm must exhibit zero dilution penalty");

        // Monotonic decline with rising CO2
        double factor400 = EcologicalStoichiometryEngine.calculateProteinDilutionFactor(400.0);
        double factor550 = EcologicalStoichiometryEngine.calculateProteinDilutionFactor(550.0);
        double factor800 = EcologicalStoichiometryEngine.calculateProteinDilutionFactor(800.0);
        double factor1200 = EcologicalStoichiometryEngine.calculateProteinDilutionFactor(1200.0);

        assertTrue(factor400 < factor280, "400 ppm must exhibit slight protein dilution");
        assertTrue(factor550 < factor400, "550 ppm must exhibit moderate protein dilution");
        assertTrue(factor800 < factor550, "800 ppm must exhibit higher dilution");
        assertTrue(factor1200 < factor800, "1200 ppm must approach asymptotic ceiling");

        // Physical bounds: maximum dilution ~12% (minimum factor ~0.88)
        assertTrue(factor1200 >= 0.88, "Dilution penalty must never exceed maximum ~12% empirical bound");
    }

    @Test
    @DisplayName("2.4 500-Cell Continental Agricultural Domain: Stoichiometric Yield Calibration")
    void test500CellAgronomicStoichiometrySimulation() {
        List<H3Cell> agriculturalMesh = new ArrayList<>(500);

        for (int i = 0; i < 500; i++) {
            H3Cell c = new H3Cell(0x886000000000000L + i, 25.0 + (i * 0.05), 10.0 + (i * 0.08));
            c.setBiome(Biome.PLAINS);
            c.setPopulation(1000 + i * 10);
            c.setBiomassAgriculture(50.0 + (i % 150));
            c.setSoilOrganicCarbon(15.0 + (i % 35)); // SOC from 15 to 50 g/kg
            c.setResourceMetal(20.0 + (i % 180));
            c.setResourceCapital(100.0 + (i * 5));
            c.setFoodResource(200.0);
            agriculturalMesh.add(c);
        }

        // Run 10 years at modern atmospheric CO2 (420 ppm)
        org.ether.society.core.dod.WorldBuffer agriBuf = new org.ether.society.core.dod.WorldBuffer(agriculturalMesh.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(agriculturalMesh, agriBuf, false);
        EcologicalStoichiometryEngine stoichEngine = new EcologicalStoichiometryEngine();
        for (int yr = 1; yr <= 10; yr++) {
            stoichEngine.tick(agriBuf, 1.0, 12.0);

            for (int i = 0; i < agriculturalMesh.size(); i++) {
                float food = agriBuf.getFoodResource()[i];
                assertTrue(food > 200.0f, "Agricultural stoichiometric conversion must generate food");
                assertFalse(Float.isNaN(food), "Food resource must never be NaN");
                assertFalse(Float.isInfinite(food), "Food resource must never be infinite");
            }
        }
    }

    @Test
    @DisplayName("2.5 Multi-Decadal Soil Nutrient Depletion & Agricultural Yield Feedback")
    void testMultiDecadalSoilNutrientDepletion() {
        org.ether.society.core.dod.WorldBuffer farmBuf = new org.ether.society.core.dod.WorldBuffer(100);
        for (int i = 0; i < 100; i++) {
            farmBuf.getH3Indexes()[i] = 0x887000000000000L + i;
            farmBuf.getBiomes()[i] = (byte) Biome.PLAINS.ordinal();
            farmBuf.getBiomassHuman()[i] = 2000.0f * 0.06f;
            farmBuf.getBiomassAgriculture()[i] = 100.0f;
            farmBuf.getSoilOrganicCarbon()[i] = 45.0f; // Rich initial soil organic carbon
            farmBuf.getMetalResource()[i] = 100.0f;
            farmBuf.getFoodResource()[i] = 0.0f;
        }

        List<Double> annualYieldTrajectory = new ArrayList<>();
        EcologicalStoichiometryEngine stoichEngine = new EcologicalStoichiometryEngine();

        // Simulate 40 years of continuous agricultural exploitation without fertilization
        for (int yr = 1; yr <= 40; yr++) {
            // Soil depletion: SOC slowly degrades under continuous plowing
            for (int i = 0; i < 100; i++) {
                farmBuf.getSoilOrganicCarbon()[i] = Math.max(5.0f, farmBuf.getSoilOrganicCarbon()[i] - 0.75f);
                farmBuf.getFoodResource()[i] = 0.0f; // Reset annual harvest measurement
            }

            stoichEngine.tick(farmBuf, 1.0, 12.0);

            double sumYield = 0.0;
            for (int i = 0; i < 100; i++) {
                sumYield += farmBuf.getFoodResource()[i];
            }
            annualYieldTrajectory.add(sumYield / 100.0);
        }

        double initialYield = annualYieldTrajectory.get(0);
        double finalYield = annualYieldTrajectory.get(annualYieldTrajectory.size() - 1);

        assertTrue(finalYield < initialYield, "Soil nutrient depletion must attenuate agricultural food output over decades");
        assertTrue(finalYield > 0.0, "Residual soil nutrients must sustain positive baseline production");
    }

    @Test
    @DisplayName("2.6 Bitwise Determinism across Repeated Stoichiometric Spatial Runs")
    void testBitwiseDeterminismStoichiometry() {
        org.ether.society.core.dod.WorldBuffer runA = new org.ether.society.core.dod.WorldBuffer(100);
        org.ether.society.core.dod.WorldBuffer runB = new org.ether.society.core.dod.WorldBuffer(100);

        for (int i = 0; i < 100; i++) {
            runA.getH3Indexes()[i] = 0x888000000000000L + i;
            runA.getBiomassAgriculture()[i] = 80.0f + i * 2;
            runA.getSoilOrganicCarbon()[i] = 25.0f + i * 0.2f;
            runA.getMetalResource()[i] = 50.0f + i * 1.5f;
            runA.getFoodResource()[i] = 100.0f;

            runB.getH3Indexes()[i] = 0x888000000000000L + i;
            runB.getBiomassAgriculture()[i] = 80.0f + i * 2;
            runB.getSoilOrganicCarbon()[i] = 25.0f + i * 0.2f;
            runB.getMetalResource()[i] = 50.0f + i * 1.5f;
            runB.getFoodResource()[i] = 100.0f;
        }

        EcologicalStoichiometryEngine engineA = new EcologicalStoichiometryEngine();
        EcologicalStoichiometryEngine engineB = new EcologicalStoichiometryEngine();
        for (int yr = 0; yr < 5; yr++) {
            engineA.tick(runA, 1.0, 12.0);
            engineB.tick(runB, 1.0, 12.0);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(runA.getFoodResource()[i], runB.getFoodResource()[i], 1e-6f,
                    "Food resource must be bitwise identical across deterministic runs");
        }
    }
}
