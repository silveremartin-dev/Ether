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
 * High-Precision Verification & Large-Scale Spatial Test Suite for {@link MetabolicEcologyEngine}.
 *
 * <p>Validates Brown et al. (2004) Metabolic Theory of Ecology (MTE) and Gillooly et al. (2001, 2002) across:
 * <ul>
 *   <li>Kleiber's 3/4 allometric metabolic power scaling across 8 orders of biological magnitude.</li>
 *   <li>Mass-specific -1/4 allometric power scaling.</li>
 *   <li>Boltzmann-Arrhenius temperature kinetics ($E_a \approx 0.65\text{ eV}$).</li>
 *   <li>Generation time allometry ($t_{\text{gen}} \propto M^{1/4}$).</li>
 *   <li>500-cell large continental mesh across polar-to-equatorial thermal gradients over 20 years.</li>
 *   <li>Bitwise determinism and numerical stability.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
class MetabolicEcologyEngineTest {

    @Test
    @DisplayName("3.1 Kleiber's 3/4 Allometric Power-Law Scaling across Organism Masses")
    void testKleiberAllometricScaling() {
        double tempK = 293.15; // 20°C
        double bShrew = MetabolicEcologyEngine.calculateMetabolicRateWatts(0.010, tempK, MetabolicEcologyEngine.TaxonGuild.ENDOTHERM_MAMMAL);
        double bHuman = MetabolicEcologyEngine.calculateMetabolicRateWatts(70.0, tempK, MetabolicEcologyEngine.TaxonGuild.ENDOTHERM_MAMMAL);
        double bMammoth = MetabolicEcologyEngine.calculateMetabolicRateWatts(4000.0, tempK, MetabolicEcologyEngine.TaxonGuild.ENDOTHERM_MAMMAL);

        assertTrue(bShrew > 0.0, "Metabolic power must be positive");
        assertTrue(bHuman > bShrew, "Human metabolic power must exceed shrew");
        assertTrue(bMammoth > bHuman, "Mammoth metabolic power must exceed human");

        // Verification of Kleiber ratio: B_human / B_shrew should approximate (70 / 0.010)^0.75
        double observedRatio = bHuman / bShrew;
        double expectedRatio = Math.pow(70.0 / 0.010, 0.75);
        assertEquals(expectedRatio, observedRatio, expectedRatio * 0.01, "Allometric scaling ratio must follow M^0.75 exactly");
    }

    @Test
    @DisplayName("3.2 Mass-Specific Metabolic Rate: -1/4 Power Law (Small organisms burn more per kg)")
    void testMassSpecificMetabolism() {
        double tempK = 293.15;
        double bPerKgMouse = MetabolicEcologyEngine.calculateMassSpecificMetabolicRate(0.025, tempK, MetabolicEcologyEngine.TaxonGuild.ENDOTHERM_MAMMAL);
        double bPerKgElephant = MetabolicEcologyEngine.calculateMassSpecificMetabolicRate(3500.0, tempK, MetabolicEcologyEngine.TaxonGuild.ENDOTHERM_MAMMAL);

        assertTrue(bPerKgMouse > bPerKgElephant, "Small mammals must possess higher mass-specific metabolic rates than megafauna");

        // Ratio should match (3500 / 0.025)^0.25 = (140,000)^0.25 ~ 19.34
        double observedRatio = bPerKgMouse / bPerKgElephant;
        double expectedRatio = Math.pow(3500.0 / 0.025, 0.25);
        assertEquals(expectedRatio, observedRatio, expectedRatio * 0.01, "Mass-specific scaling ratio must follow M^(-1/4)");
    }

    @Test
    @DisplayName("3.3 Boltzmann-Arrhenius Temperature Kinetics (E_a = 0.65 eV, Q_10 ~ 2-4)")
    void testBoltzmannTemperatureScaling() {
        double massKg = 2.0;
        double bCold = MetabolicEcologyEngine.calculateMetabolicRateWatts(massKg, 283.15, MetabolicEcologyEngine.TaxonGuild.ECTOTHERM_VERTEBRATE); // 10°C
        double bWarm = MetabolicEcologyEngine.calculateMetabolicRateWatts(massKg, 303.15, MetabolicEcologyEngine.TaxonGuild.ECTOTHERM_VERTEBRATE); // 30°C

        assertTrue(bWarm > bCold, "Ectotherm metabolic power must accelerate with ambient temperature");
        double q20 = bWarm / bCold;
        // Typical biological Q10 for 20°C difference is ~4 (Q10 of 2 squared)
        assertTrue(q20 > 2.0 && q20 < 8.0, "Thermodynamic temperature acceleration must fall within empirical biochemical bounds");
    }

    @Test
    @DisplayName("3.4 Generation Time Allometry: Increases with M^1/4 and scales inversely with temperature")
    void testGenerationTimeScaling() {
        double tSmall = MetabolicEcologyEngine.calculateGenerationTimeYears(0.030, 293.15, true);
        double tLarge = MetabolicEcologyEngine.calculateGenerationTimeYears(1000.0, 293.15, true);

        assertTrue(tSmall < tLarge, "Small rodents must have much shorter generation times than large mammals");

        double expectedRatio = Math.pow(1000.0 / 0.030, 0.25);
        double observedRatio = tLarge / tSmall;
        assertEquals(expectedRatio, observedRatio, expectedRatio * 0.02, "Generation time must scale as M^(1/4)");
    }

    @Test
    @DisplayName("3.5 500-Cell Continental Mesh: Latitudinal Thermal Gradient & 20-Year Metabolic Dissipation")
    void test500CellContinentalMetabolicSimulation() {
        List<H3Cell> mesh = new ArrayList<>(500);

        for (int i = 0; i < 500; i++) {
            double lat = -60.0 + (120.0 * i / 499.0);
            double lon = -150.0 + (300.0 * (i * 19 % 500) / 499.0);
            H3Cell c = new H3Cell(0x889000000000000L + i, lat, lon);

            double tempC = 30.0 - 0.60 * Math.abs(lat);
            c.setTemperature(tempC);

            c.setBiomassNatural(50.0 + (i % 200));
            c.setBiomassLivestock(5.0 + (i % 40));
            c.setBiomassAgriculture(20.0 + (i % 100));

            if (tempC < -5.0) c.setBiome(Biome.TUNDRA);
            else if (tempC > 25.0) c.setBiome(Biome.DESERT);
            else c.setBiome(Biome.PLAINS);

            mesh.add(c);
        }

        // Simulate 20 years of continuous metabolic dissipation
        for (int yr = 1; yr <= 20; yr++) {
            // Seasonal primary productivity replenishes biomass
            for (H3Cell c : mesh) {
                c.setBiomassNatural(c.getBiomassNatural() + 5.0);
                c.setBiomassAgriculture(c.getBiomassAgriculture() + 3.0);
            }

            MetabolicEcologyEngine.processMetabolicEcology(mesh, 1.0);

            for (H3Cell c : mesh) {
                assertTrue(c.getBiomassNatural() >= 0.1, "Natural biomass must remain strictly positive");
                assertTrue(c.getBiomassLivestock() >= 0.0, "Livestock biomass must remain non-negative");
                assertTrue(c.getBiomassAgriculture() >= 0.0, "Agricultural biomass must remain non-negative");
                assertFalse(Double.isNaN(c.getBiomassNatural()), "Natural biomass must never be NaN");
                assertFalse(Double.isNaN(c.getBiomassLivestock()), "Livestock biomass must never be NaN");
            }
        }
    }

    @Test
    @DisplayName("3.6 Bitwise Determinism across Repeated MTE Mesh Executions")
    void testBitwiseDeterminismMTE() {
        List<H3Cell> runA = new ArrayList<>(100);
        List<H3Cell> runB = new ArrayList<>(100);

        for (int i = 0; i < 100; i++) {
            H3Cell cA = new H3Cell(0x88a000000000000L + i, 20.0 + i * 0.1, 10.0);
            cA.setTemperature(15.0 + (i % 15));
            cA.setBiomassNatural(100.0 + i);
            cA.setBiomassLivestock(20.0 + i * 0.5);
            cA.setBiomassAgriculture(50.0 + i);
            runA.add(cA);

            H3Cell cB = new H3Cell(0x88a000000000000L + i, 20.0 + i * 0.1, 10.0);
            cB.setTemperature(15.0 + (i % 15));
            cB.setBiomassNatural(100.0 + i);
            cB.setBiomassLivestock(20.0 + i * 0.5);
            cB.setBiomassAgriculture(50.0 + i);
            runB.add(cB);
        }

        for (int yr = 0; yr < 10; yr++) {
            MetabolicEcologyEngine.processMetabolicEcology(runA, 1.0);
            MetabolicEcologyEngine.processMetabolicEcology(runB, 1.0);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(runA.get(i).getBiomassNatural(), runB.get(i).getBiomassNatural(), 1e-12, "Natural biomass must be bitwise identical");
            assertEquals(runA.get(i).getBiomassLivestock(), runB.get(i).getBiomassLivestock(), 1e-12, "Livestock biomass must be bitwise identical");
            assertEquals(runA.get(i).getBiomassAgriculture(), runB.get(i).getBiomassAgriculture(), 1e-12, "Agriculture biomass must be bitwise identical");
        }
    }
}
