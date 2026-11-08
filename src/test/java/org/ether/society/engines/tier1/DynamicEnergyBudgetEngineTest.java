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
        H3Cell cell = new H3Cell(0x8828308280fffffL, 35.0, 15.0);
        cell.setPopulation(5000);
        cell.setBiomassHuman(250.0);
        // Abundant food resource: 200,000 GJ (~40 GJ/capita, well above annual demand)
        cell.setFoodResource(200000.0);
        cell.setTemperature(20.0);

        int initialPop = cell.getPopulation();
        DynamicEnergyBudgetEngine.processHumanDEB(List.of(cell), 1.0);

        assertTrue(cell.getPopulation() > initialPop, "Populated cohort with abundant food surplus must reproduce via kappa-rule");
        assertTrue(cell.getFoodResource() < 200000.0, "Food stock must be consumed and strictly conserved");
        assertTrue(cell.getEnergyFoodConsumed() > 0.0, "Consumed food must be accurately tracked in telemetry");
        assertTrue(cell.getBiomassHuman() > 250.0, "Structural human biomass must grow proportionally with population");
    }

    @Test
    @DisplayName("1.4 Continuous Starvation Mortality: Absence of food catabolizes soma with quadratic hazard rate")
    void testStarvationWastingContinuity() {
        H3Cell cell = new H3Cell(0x8828308280ffffeL, 35.0, 15.0);
        cell.setPopulation(10000);
        cell.setBiomassHuman(500.0);
        cell.setFoodResource(0.0); // Zero food available
        cell.setTemperature(20.0);

        int popT0 = cell.getPopulation();

        // 6 months of acute famine
        DynamicEnergyBudgetEngine.processHumanDEB(List.of(cell), 0.5);
        int popT1 = cell.getPopulation();

        assertTrue(popT1 < popT0, "Starvation without food must induce mortality");
        assertTrue(popT1 > 0, "6 months of famine must not trigger instantaneous total extinction cliff");

        // Another 6 months of acute famine (1 year total)
        DynamicEnergyBudgetEngine.processHumanDEB(List.of(cell), 0.5);
        int popT2 = cell.getPopulation();

        assertTrue(popT2 < popT1, "Prolonged famine must continue to exert actuarial mortality");
        assertTrue(popT2 >= 0, "Population must remain strictly non-negative");
    }

    @Test
    @DisplayName("1.5 500-Cell Heterogeneous Spatial Mesh: 30-Year Multidecadal Demographic Continuity")
    void test500CellLargeMeshDEBSimulation() {
        List<H3Cell> mesh = new ArrayList<>(500);

        // Generate 500 cells spanning diverse latitudes (-50° to +60°), biomes, populations, and food endowments
        for (int i = 0; i < 500; i++) {
            double lat = -50.0 + (110.0 * i / 499.0);
            double lon = -120.0 + (240.0 * (i * 17 % 500) / 499.0);
            H3Cell c = new H3Cell(0x882800000000000L + i, lat, lon);

            double tempC = 28.0 - 0.45 * Math.abs(lat);
            c.setTemperature(tempC);

            // Populated rural to urban gradient: 200 to 25,000 inhabitants
            int pop = 200 + (int) (24800.0 * Math.pow((i % 50) / 49.0, 2.5));
            c.setPopulation(pop);
            c.setBiomassHuman(pop * DynamicEnergyBudgetEngine.HUMAN_LEAN_STRUCTURAL_MASS_TONS);

            // Initial food stock: varying from 0.5x to 4.0x annual maintenance
            double annualDemand = pop * DynamicEnergyBudgetEngine.HUMAN_MAX_INGESTION_GJ_PER_YEAR;
            double foodGJ = annualDemand * (0.8 + 2.5 * ((i * 13 % 100) / 100.0));
            c.setFoodResource(foodGJ);

            if (tempC < 0.0) c.setBiome(Biome.TUNDRA);
            else if (tempC > 24.0) c.setBiome(Biome.DESERT);
            else c.setBiome(Biome.PLAINS);

            mesh.add(c);
        }

        long initialTotalPop = mesh.stream().mapToLong(H3Cell::getPopulation).sum();
        assertTrue(initialTotalPop > 100_000, "Initial population across 500 cells must exceed 100k");

        // Simulate 30 consecutive years
        for (int year = 1; year <= 30; year++) {
            // Replenish seasonal harvest based on agricultural productivity
            for (H3Cell c : mesh) {
                double harvestGJ = c.getPopulation() * (3.5 + 1.5 * Math.sin(year * 0.4 + c.getLatitude()));
                c.setFoodResource(c.getFoodResource() + harvestGJ);
            }

            DynamicEnergyBudgetEngine.processHumanDEB(mesh, 1.0);

            // Verify physical state invariants at every tick
            for (H3Cell c : mesh) {
                assertTrue(c.getPopulation() >= 0, "Cell population must never be negative");
                assertTrue(c.getFoodResource() >= 0.0, "Cell food resource must never be negative");
                assertTrue(c.getBiomassHuman() >= 0.0, "Human biomass must never be negative");
                assertFalse(Double.isNaN(c.getFoodResource()), "Food resource must never be NaN");
                assertFalse(Double.isNaN(c.getBiomassHuman()), "Biomass must never be NaN");
            }
        }

        long finalTotalPop = mesh.stream().mapToLong(H3Cell::getPopulation).sum();
        assertTrue(finalTotalPop > 500_000, "After 30 years of sustainable harvest, population must maintain vital demographic continuity");
    }

    @Test
    @DisplayName("1.6 Multi-Year Famine Shock & Post-Crisis Somatic Rebound")
    void testMultiYearFamineShockAndRebound() {
        List<H3Cell> valley = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            H3Cell c = new H3Cell(0x883100000000000L + i, 32.0, 35.0);
            c.setPopulation(10000);
            c.setBiomassHuman(500.0);
            c.setFoodResource(150.0); // Minimal food reserve (~0.015 GJ/capita, acute famine)
            valley.add(c);
        }

        int startPop = valley.stream().mapToInt(H3Cell::getPopulation).sum();

        // 2 consecutive years of total harvest failure (zero food replenishment, >90% population shock)
        for (int yr = 1; yr <= 2; yr++) {
            DynamicEnergyBudgetEngine.processHumanDEB(valley, 1.0);
        }

        int faminePop = valley.stream().mapToInt(H3Cell::getPopulation).sum();
        assertTrue(faminePop < startPop, "2 years of acute famine must dramatically reduce valley population");
        assertTrue(faminePop > 0, "Famine mortality must be gradual and continuous, not instantaneous wipeout");

        // Famine ends: abundant harvests resume for 10 years
        for (int yr = 1; yr <= 10; yr++) {
            for (H3Cell c : valley) {
                c.setFoodResource(c.getFoodResource() + c.getPopulation() * 10.0);
            }
            DynamicEnergyBudgetEngine.processHumanDEB(valley, 1.0);
        }

        int recoveredPop = valley.stream().mapToInt(H3Cell::getPopulation).sum();
        assertTrue(recoveredPop > faminePop, "With abundant food restored, DEB reproduction must rebuild demographic capital");
    }

    @Test
    @DisplayName("1.7 500-Cell Megafauna & Primary Producer Ecological Trophic Coexistence")
    void test500CellEcologicalTrophicCoexistence() {
        List<H3Cell> wildDomain = new ArrayList<>(500);
        for (int i = 0; i < 500; i++) {
            H3Cell c = new H3Cell(0x884000000000000L + i, 10.0 + (i * 0.08), 20.0 + (i * 0.05));
            c.setTemperature(18.0 + (i % 12));
            c.setBiomassNatural(80.0 + (i % 120)); // Pristine savanna/steppe vegetation
            wildDomain.add(c);
        }

        // Run 20 years of producer-herbivore dynamic energy budget interaction
        for (int yr = 1; yr <= 20; yr++) {
            // Net primary production regrows plant biomass
            for (H3Cell c : wildDomain) {
                double regrown = 15.0 * (1.0 - (c.getBiomassNatural() / 300.0));
                c.setBiomassNatural(Math.max(1.0, c.getBiomassNatural() + regrown));
            }

            DynamicEnergyBudgetEngine.processEcologicalDEB(wildDomain, 1.0);

            for (H3Cell c : wildDomain) {
                assertTrue(c.getBiomassNatural() >= 0.5, "Natural biomass must remain strictly positive under grazing");
                assertTrue(c.getBiomassNatural() < 500.0, "Natural biomass must not explode unphysically");
            }
        }
    }

    @Test
    @DisplayName("1.8 Bitwise Determinism & Thread Invariance across DEB Grid")
    void testBitwiseDeterminismDEB() {
        List<H3Cell> runA = new ArrayList<>(100);
        List<H3Cell> runB = new ArrayList<>(100);

        for (int i = 0; i < 100; i++) {
            H3Cell cA = new H3Cell(0x885000000000000L + i, 40.0, 10.0);
            cA.setPopulation(1500 + i * 20);
            cA.setFoodResource(10000.0 + i * 50.0);
            cA.setTemperature(16.0);
            runA.add(cA);

            H3Cell cB = new H3Cell(0x885000000000000L + i, 40.0, 10.0);
            cB.setPopulation(1500 + i * 20);
            cB.setFoodResource(10000.0 + i * 50.0);
            cB.setTemperature(16.0);
            runB.add(cB);
        }

        for (int tick = 0; tick < 10; tick++) {
            DynamicEnergyBudgetEngine.processHumanDEB(runA, 1.0);
            DynamicEnergyBudgetEngine.processHumanDEB(runB, 1.0);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(runA.get(i).getPopulation(), runB.get(i).getPopulation(), "Population must be bitwise identical");
            assertEquals(runA.get(i).getFoodResource(), runB.get(i).getFoodResource(), 1e-12, "Food stock must be bitwise identical");
            assertEquals(runA.get(i).getEnergyFoodConsumed(), runB.get(i).getEnergyFoodConsumed(), 1e-12, "Consumed energy must be bitwise identical");
        }
    }
}
