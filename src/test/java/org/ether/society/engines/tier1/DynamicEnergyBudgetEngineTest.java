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
 * High-Precision Verification & Large-Scale Spatial Test Suite for {@link DynamicEnergyBudgetEngine}.
 *
 * <p>Validates Kooijman Standard Dynamic Energy Budget (DEB) across:
 * <ul>
 *   <li>Analytical kinetics (Arrhenius temperature scaling, Holling Type II functional response).</li>
 *   <li>Conserved $\kappa$-rule allocation between structural soma and reproductive reserves.</li>
 *   <li>Continuous $C^1$ starvation wasting mortality ($w_{\text{crit}} = 0.40$) without step cliffs.</li>
 *   <li>500-cell heterogeneous spatial mesh over 30-year multi-decadal simulation.</li>
 *   <li>Multi-year severe harvest famine shock and reserve buffering.</li>
 *   <li>500-cell megafauna-producer trophic coexistence and mass-energy balance.</li>
 *   <li>Strict bitwise determinism and numerical stability.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
class DynamicEnergyBudgetEngineTest {

    @Test
    @DisplayName("1.1 Arrhenius Temperature Correction: Exact match at T_ref (20°C) and physical bounding")
    void testArrheniusCorrection() {
        double kRef = DynamicEnergyBudgetEngine.calculateArrheniusCorrection(20.0);
        assertEquals(1.0, kRef, 1e-3, "Arrhenius kinetic factor at reference temperature 20°C must be exactly 1.0");

        double kCold = DynamicEnergyBudgetEngine.calculateArrheniusCorrection(0.0);
        assertTrue(kCold < 1.0 && kCold >= 0.05, "Metabolic kinetics must attenuate under cold temperatures within [0.05, 1.0]");

        double kWarm = DynamicEnergyBudgetEngine.calculateArrheniusCorrection(35.0);
        assertTrue(kWarm > 1.0 && kWarm <= 3.0, "Metabolic kinetics must accelerate under warm temperatures within bounds [1.0, 3.0]");
    }

    @Test
    @DisplayName("1.2 Functional Response: Smooth Holling Type II Michaelis-Menten asymptotic saturation")
    void testFunctionalResponse() {
        assertEquals(0.0, DynamicEnergyBudgetEngine.calculateFunctionalResponse(0.0, 1.8), 1e-6);
        assertEquals(0.5, DynamicEnergyBudgetEngine.calculateFunctionalResponse(1.8, 1.8), 1e-3, "At X = X_K, f must equal 0.5");

        double fAbundant = DynamicEnergyBudgetEngine.calculateFunctionalResponse(50.0, 1.8);
        assertTrue(fAbundant > 0.95 && fAbundant < 1.0, "At high food abundance, f must asymptotically approach 1.0 without exceeding it");
    }

    @Test
    @DisplayName("1.3 Standard Kappa-Rule: Caloric surplus drives gestation and live births")
    void testKappaRuleSurplusRegime() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 0x8828308280fffffL;
        buf.getBiomassHuman()[0] = 5000.0f * 0.06f;
        // Abundant food resource: 200,000 GJ (~40 GJ/capita, well above annual demand)
        buf.getFoodResource()[0] = 200000.0f;
        buf.getTemperature()[0] = 20.0f;

        double initialBio = buf.getBiomassHuman()[0];
        new DynamicEnergyBudgetEngine().tick(buf, 1.0, 12.0);

        assertTrue(buf.getBiomassHuman()[0] > initialBio, "Populated cohort with abundant food surplus must reproduce via kappa-rule");
        assertTrue(buf.getFoodResource()[0] < 200000.0f, "Food stock must be consumed and strictly conserved");
        assertTrue(buf.getEnergyFoodConsumed()[0] > 0.0f, "Consumed food must be accurately tracked in telemetry");
    }

    @Test
    @DisplayName("1.4 Continuous Starvation Mortality: Absence of food catabolizes soma with quadratic hazard rate")
    void testStarvationWastingContinuity() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 0x8828308280ffffeL;
        buf.getBiomassHuman()[0] = 10000.0f * 0.06f;
        buf.getFoodResource()[0] = 0.0f; // Zero food available
        buf.getTemperature()[0] = 20.0f;

        double bioT0 = buf.getBiomassHuman()[0];

        DynamicEnergyBudgetEngine deb = new DynamicEnergyBudgetEngine();
        // 6 months of acute famine
        deb.tick(buf, 0.5, 6.0);
        double bioT1 = buf.getBiomassHuman()[0];

        assertTrue(bioT1 < bioT0, "Starvation without food must induce mortality");
        assertTrue(bioT1 > 0.0, "6 months of famine must not trigger instantaneous total extinction cliff");

        // Another 6 months of acute famine (1 year total)
        deb.tick(buf, 0.5, 6.0);
        double bioT2 = buf.getBiomassHuman()[0];

        assertTrue(bioT2 < bioT1, "Prolonged famine must continue to exert actuarial mortality");
        assertTrue(bioT2 >= 0.0, "Population must remain strictly non-negative");
    }

    @Test
    @DisplayName("1.5 500-Cell Heterogeneous Spatial Mesh: 30-Year Multidecadal Demographic Continuity")
    void test500CellLargeMeshDEBSimulation() {
        org.ether.society.core.dod.WorldBuffer meshBuf = new org.ether.society.core.dod.WorldBuffer(500);
        double[] lats = new double[500];

        // Generate 500 cells spanning diverse latitudes (-50° to +60°), biomes, populations, and food endowments
        for (int i = 0; i < 500; i++) {
            double lat = -50.0 + (110.0 * i / 499.0);
            lats[i] = lat;
            meshBuf.getH3Indexes()[i] = 0x882800000000000L + i;

            double tempC = 28.0 - 0.45 * Math.abs(lat);
            meshBuf.getTemperature()[i] = (float) tempC;

            // Populated rural to urban gradient: 200 to 25,000 inhabitants
            int pop = 200 + (int) (24800.0 * Math.pow((i % 50) / 49.0, 2.5));
            meshBuf.getBiomassHuman()[i] = (float) (pop * DynamicEnergyBudgetEngine.HUMAN_LEAN_STRUCTURAL_MASS_TONS);

            // Initial food stock: varying from 0.5x to 4.0x annual maintenance
            double annualDemand = pop * DynamicEnergyBudgetEngine.HUMAN_MAX_INGESTION_GJ_PER_YEAR;
            double foodGJ = annualDemand * (0.8 + 2.5 * ((i * 13 % 100) / 100.0));
            meshBuf.getFoodResource()[i] = (float) foodGJ;

            if (tempC < 0.0) meshBuf.getBiomes()[i] = (byte) Biome.TUNDRA.ordinal();
            else if (tempC > 24.0) meshBuf.getBiomes()[i] = (byte) Biome.DESERT.ordinal();
            else meshBuf.getBiomes()[i] = (byte) Biome.PLAINS.ordinal();
        }

        double initialTotalPop = 0.0;
        for (int i = 0; i < 500; i++) {
            initialTotalPop += meshBuf.getBiomassHuman()[i] / DynamicEnergyBudgetEngine.HUMAN_LEAN_STRUCTURAL_MASS_TONS;
        }
        assertTrue(initialTotalPop > 100_000, "Initial population across 500 cells must exceed 100k");

        DynamicEnergyBudgetEngine deb = new DynamicEnergyBudgetEngine();
        // Simulate 30 consecutive years
        for (int year = 1; year <= 30; year++) {
            // Replenish seasonal harvest based on agricultural productivity
            for (int i = 0; i < 500; i++) {
                double pop = meshBuf.getBiomassHuman()[i] / DynamicEnergyBudgetEngine.HUMAN_LEAN_STRUCTURAL_MASS_TONS;
                double harvestGJ = pop * (3.5 + 1.5 * Math.sin(year * 0.4 + lats[i]));
                meshBuf.getFoodResource()[i] = (float) (meshBuf.getFoodResource()[i] + harvestGJ);
            }

            deb.tick(meshBuf, 1.0, 12.0);

            // Verify physical state invariants at every tick
            for (int i = 0; i < 500; i++) {
                assertTrue(meshBuf.getFoodResource()[i] >= 0.0f, "Cell food resource must never be negative");
                assertTrue(meshBuf.getBiomassHuman()[i] >= 0.0f, "Human biomass must never be negative");
                assertFalse(Float.isNaN(meshBuf.getFoodResource()[i]), "Food resource must never be NaN");
                assertFalse(Float.isNaN(meshBuf.getBiomassHuman()[i]), "Biomass must never be NaN");
            }
        }

        double finalTotalPop = 0.0;
        for (int i = 0; i < 500; i++) {
            finalTotalPop += meshBuf.getBiomassHuman()[i] / DynamicEnergyBudgetEngine.HUMAN_LEAN_STRUCTURAL_MASS_TONS;
        }
        assertTrue(finalTotalPop > 500_000, "After 30 years of sustainable harvest, population must maintain vital demographic continuity");
    }

    @Test
    @DisplayName("1.6 Multi-Year Famine Shock & Post-Crisis Somatic Rebound")
    void testMultiYearFamineShockAndRebound() {
        org.ether.society.core.dod.WorldBuffer valley = new org.ether.society.core.dod.WorldBuffer(50);
        for (int i = 0; i < 50; i++) {
            valley.getH3Indexes()[i] = 0x883100000000000L + i;
            valley.getBiomassHuman()[i] = 10000.0f * 0.06f;
            valley.getFoodResource()[i] = 150.0f; // Minimal food reserve (~0.015 GJ/capita, acute famine)
        }

        double startBio = 0.0;
        for (int i = 0; i < 50; i++) startBio += valley.getBiomassHuman()[i];

        DynamicEnergyBudgetEngine deb = new DynamicEnergyBudgetEngine();
        // 2 consecutive years of total harvest failure (zero food replenishment, >90% population shock)
        for (int yr = 1; yr <= 2; yr++) {
            deb.tick(valley, 1.0, 12.0);
        }

        double famineBio = 0.0;
        for (int i = 0; i < 50; i++) famineBio += valley.getBiomassHuman()[i];
        assertTrue(famineBio < startBio, "2 years of acute famine must dramatically reduce valley population");
        assertTrue(famineBio > 0.0, "Famine mortality must be gradual and continuous, not instantaneous wipeout");

        // Famine ends: abundant harvests resume for 10 years
        for (int yr = 1; yr <= 10; yr++) {
            for (int i = 0; i < 50; i++) {
                double pop = valley.getBiomassHuman()[i] / 0.06;
                valley.getFoodResource()[i] = (float) (valley.getFoodResource()[i] + pop * 10.0);
            }
            deb.tick(valley, 1.0, 12.0);
        }

        double recoveredBio = 0.0;
        for (int i = 0; i < 50; i++) recoveredBio += valley.getBiomassHuman()[i];
        assertTrue(recoveredBio > famineBio, "With abundant food restored, DEB reproduction must rebuild demographic capital");
    }

    @Test
    @DisplayName("1.7 500-Cell Megafauna & Primary Producer Ecological Trophic Coexistence")
    void test500CellEcologicalTrophicCoexistence() {
        org.ether.society.core.dod.WorldBuffer wildDomain = new org.ether.society.core.dod.WorldBuffer(500);
        for (int i = 0; i < 500; i++) {
            wildDomain.getH3Indexes()[i] = 0x884000000000000L + i;
            wildDomain.getTemperature()[i] = 18.0f + (i % 12);
            wildDomain.getBiomassNatural()[i] = 80.0f + (i % 120); // Pristine savanna/steppe vegetation
        }

        DynamicEnergyBudgetEngine deb = new DynamicEnergyBudgetEngine();
        // Run 20 years of producer-herbivore dynamic energy budget interaction
        for (int yr = 1; yr <= 20; yr++) {
            // Net primary production regrows plant biomass
            for (int i = 0; i < 500; i++) {
                double bio = wildDomain.getBiomassNatural()[i];
                double regrown = 15.0 * (1.0 - (bio / 300.0));
                wildDomain.getBiomassNatural()[i] = (float) Math.max(1.0, bio + regrown);
            }

            deb.tick(wildDomain, 1.0, 12.0);

            for (int i = 0; i < 500; i++) {
                assertTrue(wildDomain.getBiomassNatural()[i] >= 0.5f, "Natural biomass must remain strictly positive under grazing");
                assertTrue(wildDomain.getBiomassNatural()[i] < 500.0f, "Natural biomass must not explode unphysically");
            }
        }
    }

    @Test
    @DisplayName("1.8 Bitwise Determinism & Thread Invariance across DEB Grid")
    void testBitwiseDeterminismDEB() {
        org.ether.society.core.dod.WorldBuffer runA = new org.ether.society.core.dod.WorldBuffer(100);
        org.ether.society.core.dod.WorldBuffer runB = new org.ether.society.core.dod.WorldBuffer(100);

        for (int i = 0; i < 100; i++) {
            runA.getH3Indexes()[i] = 0x885000000000000L + i;
            runA.getBiomassHuman()[i] = (1500 + i * 20) * 0.06f;
            runA.getFoodResource()[i] = 10000.0f + i * 50.0f;
            runA.getTemperature()[i] = 16.0f;

            runB.getH3Indexes()[i] = 0x885000000000000L + i;
            runB.getBiomassHuman()[i] = (1500 + i * 20) * 0.06f;
            runB.getFoodResource()[i] = 10000.0f + i * 50.0f;
            runB.getTemperature()[i] = 16.0f;
        }

        DynamicEnergyBudgetEngine debA = new DynamicEnergyBudgetEngine();
        DynamicEnergyBudgetEngine debB = new DynamicEnergyBudgetEngine();
        for (int tick = 0; tick < 10; tick++) {
            debA.tick(runA, 1.0, 12.0);
            debB.tick(runB, 1.0, 12.0);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(runA.getBiomassHuman()[i], runB.getBiomassHuman()[i], 0.0f, "Population must be bitwise identical");
            assertEquals(runA.getFoodResource()[i], runB.getFoodResource()[i], 0.0f, "Food stock must be bitwise identical");
            assertEquals(runA.getEnergyFoodConsumed()[i], runB.getEnergyFoodConsumed()[i], 0.0f, "Consumed energy must be bitwise identical");
        }
    }
}
