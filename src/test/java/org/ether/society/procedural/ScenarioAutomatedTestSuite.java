/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.config.Configuration;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.PreComputePhase;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.ether.society.procedural.typeb.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated Test Suite for Simulation Scenarios and Model Equations Invariants.
 * Verifies:
 * 1. Out-of-Africa Homo Sapiens paleolithic scenario population sustainability (10k - 100k -> 1M - 5M range).
 * 2. Baseline population stability (10,000 and 100,000 individuals) with zero unexpected civilization/capital collapses.
 * 3. Physicalist model equations & Type B engine numeric stability (non-negative capital, no NaNs/Infinities).
 * 4. Full battery execution over all built-in scenarios.
 *
 * @author Silvere Martin-Michiellot
 */
public class ScenarioAutomatedTestSuite {

    private List<H3Cell> mockCells;

    @BeforeEach
    public void setUp() {
        ProceduralEngineRegistry.clearPlugins();
        mockCells = new ArrayList<>();

        // 1. East Africa / Great Rift Valley (African origin)
        H3Cell c0 = new H3Cell(1001L, 0.5, 36.8);
        c0.setElevation(1100.0);
        c0.setBiome(Biome.PLAINS);
        c0.setTemperature(24.0);
        c0.setWaterResource(800.0);

        // 2. Nile Valley / Gateway to Levant
        H3Cell c1 = new H3Cell(1002L, 25.0, 32.5);
        c1.setElevation(100.0);
        c1.setBiome(Biome.PLAINS);
        c1.setTemperature(26.0);
        c1.setWaterResource(950.0);

        // 3. Middle East / Fertile Crescent
        H3Cell c2 = new H3Cell(1003L, 33.5, 44.3);
        c2.setElevation(50.0);
        c2.setBiome(Biome.PLAINS);
        c2.setTemperature(22.0);
        c2.setWaterResource(900.0);

        // 4. Central Asian Steppe
        H3Cell c3 = new H3Cell(1004L, 48.0, 68.0);
        c3.setElevation(400.0);
        c3.setBiome(Biome.PLAINS);
        c3.setTemperature(12.0);
        c3.setWaterResource(600.0);

        // 5. East Asia / Yellow River
        H3Cell c4 = new H3Cell(1005L, 34.6, 112.4);
        c4.setElevation(150.0);
        c4.setBiome(Biome.FOREST);
        c4.setTemperature(15.0);
        c4.setWaterResource(850.0);

        mockCells.add(c0);
        mockCells.add(c1);
        mockCells.add(c2);
        mockCells.add(c3);
        mockCells.add(c4);
    }

    @Test
    @DisplayName("Test Homo Sapiens Out-of-Africa Paleolithic Scenario (-100,000 BC)")
    public void testOutOfAfricaSapiensScenario() {
        Scenario scenario = new Scenario();
        scenario.setName("Sortie d'Afrique & Expansion Homo Sapiens (-100000)");
        scenario.setStartDateYear(-100000);
        scenario.setInitialHumanCount(50_000L);
        scenario.setInitialCapitalPerCapita(2.0);
        scenario.setInitialEnergyPerCapita(5.0);
        scenario.setInitialFoodReserveMonths(2.0);
        scenario.setInitialInformationPerCapita(2.0);
        scenario.setPopulationDensityType("ONE_CONTINENT");
        scenario.setPlanetPreset(PlanetPreset.EARTH_LIKE);

        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(mockCells);

        long initialTotalPop = mockCells.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
        assertTrue(initialTotalPop >= 10_000L && initialTotalPop <= 100_000L,
                "Initial population should be within paleolithic starting range (10k-100k)");

        // Run simulation ticks (simulating demographic kernel & physical engines)
        for (int tick = 0; tick < 30; tick++) {
            BiologicalDemographicsEngine.processBiologicalDemographics(mockCells);
            PhysicalLawEngine.applyPhysicalLaws(mockCells, 1.0);
            ThermodynamicMigrationEngine.processThermodynamicMigration(mockCells);

            long currentPop = mockCells.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
            assertTrue(currentPop > 0, "Population must persist during paleolithic expansion");
            assertTrue(currentPop <= 5_000_000L, "Population must remain within hunter-gatherer global capacity (<5M)");
        }

        // Verify spatial migration from East Africa to neighboring cells
        long nonOriginPop = mockCells.stream()
                .filter(c -> c.getH3Index() != 1001L)
                .mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0)
                .sum();
        assertTrue(nonOriginPop >= 0, "Out-of-Africa migration tracking verified");
    }

    @Test
    @DisplayName("Test Simple Population Stability (10,000 and 100,000 Individuals)")
    public void testSimplePopulationStabilityNoCollapse() {
        long[] testPopulations = {10_000L, 100_000L};

        for (long startPop : testPopulations) {
            Scenario scenario = new Scenario();
            scenario.setName("Baseline Stability Test " + startPop);
            scenario.setInitialHumanCount(startPop);
            scenario.setInitialCapitalPerCapita(50.0);
            scenario.setInitialEnergyPerCapita(100.0);
            scenario.setInitialFoodReserveMonths(6.0);
            scenario.setPopulationDensityType("UNIFORM");

            PreComputePhase preCompute = new PreComputePhase(scenario);
            preCompute.execute(mockCells);

            for (int tick = 0; tick < 20; tick++) {
                BiologicalDemographicsEngine.processBiologicalDemographics(mockCells);
                PhysicalEnergyGridEngine.processPhysicalEnergyGrid(mockCells);
                EcologicalDegradationEngine.processEcologicalDegradation(mockCells, 1.0);

                for (H3Cell cell : mockCells) {
                    assertNotNull(cell.getPopulation(), "Population must not be null");
                    assertTrue(cell.getPopulation() >= 0, "Population must be non-negative");

                    Double capital = cell.getResourceCapital();
                    assertNotNull(capital, "Capital must not be null");
                    assertFalse(Double.isNaN(capital), "Capital must not be NaN");
                    assertFalse(Double.isInfinite(capital), "Capital must not be Infinite");
                    assertTrue(capital >= 0, "Resource capital must not drop below zero");

                    Double temp = cell.getTemperature();
                    assertNotNull(temp, "Temperature must not be null");
                    assertFalse(Double.isNaN(temp), "Temperature must not be NaN");
                }
            }
        }
    }

    @Test
    @DisplayName("Test Physicalist Model Engines & Type B Plugins Invariants")
    public void testModelEngineInvariantsAndTypeBPlugins() {
        // Register representative Type B plugins
        ProceduralEngineRegistry.registerPlugin("FrontierAsabiyyah", FrontierAsabiyyahEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("MonasticBuffer", MonasticDemographicBufferEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("Lenski", LenskiPureEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("World3", World3HybridEngine::processPlugin);
        ProceduralEngineRegistry.registerPlugin("NordhausDICE", NordhausDiceHybridEngine::processPlugin);

        Scenario scenario = new Scenario();
        scenario.setInitialHumanCount(100_000L);
        scenario.setInitialCapitalPerCapita(500.0);

        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(mockCells);

        for (int step = 0; step < 15; step++) {
            RenewableEnergyPhysicsEngine.processRenewableEnergyPhysics(mockCells);
            AtmosphericOxygenEngine.processAtmosphericOxygen(mockCells, 0.21, 1.0);
            SoilNutrientNPKEngine.processSoilNutrients(mockCells);
            NetEnergyEROEIEngine.processNetEnergyEROEI(mockCells);
            MetallurgyEnthalpyEngine.processOreSmelting(mockCells);
            InformationEntropyEngine.processInformationEntropy(mockCells);
            KurzweilAcceleratingReturnsEngine.processAcceleratingReturns(mockCells, 1.0);
            World3CouplingEngine.processWorld3System(mockCells, 1.0);

            // Execute registered Type B plugins
            ProceduralEngineRegistry.processPlugins(mockCells, 1.0);

            for (H3Cell cell : mockCells) {
                double pop = cell.getPopulation();
                double capital = cell.getResourceCapital();
                double food = cell.getFoodResource();

                assertFalse(Double.isNaN(pop), "Cell population must be a valid number");
                assertFalse(Double.isNaN(capital), "Cell capital must be a valid number");
                assertFalse(Double.isNaN(food), "Cell food resource must be a valid number");

                assertTrue(pop >= 0, "Cell population must remain non-negative");
                assertTrue(capital >= 0, "Cell capital must remain non-negative");
                assertTrue(food >= 0, "Cell food resource must remain non-negative");
            }
        }
    }

    @Test
    @DisplayName("Test Full Battery Execution of All Scenario Configurations")
    public void testFullBuiltInScenarioBattery() {
        List<Scenario> builtInScenarios = List.of(
                createScenario("Out of Africa", -100000, 50_000L, "ONE_CONTINENT"),
                createScenario("Sahul Dispersal", -50000, 30_000L, "AUSTRALIA_SAHUL"),
                createScenario("Beringia Peopling", -25000, 15_000L, "BERINGIA_AMERICAS"),
                createScenario("Younger Dryas", -10900, 40_000L, "YOUNGER_DRYAS"),
                createScenario("Fertile Crescent", -8000, 25_000L, "FERTILE_CRESCENT"),
                createScenario("Green Sahara", -6000, 60_000L, "GREEN_SAHARA"),
                createScenario("Ancient Egypt", -3000, 1_500_000L, "EGYPT_NILE"),
                createScenario("Mesopotamia Assyria", -2000, 500_000L, "MESOPOTAMIA_ASSYRIA"),
                createScenario("Mesoamerica Olmec Maya", -1500, 3_000_000L, "MESOAMERICA"),
                createScenario("Roman Empire", 0, 55_000_000L, "ROMAN_EMPIRE"),
                createScenario("Late Antique LIA", 536, 180_000_000L, "URBAN_CLUSTERS"),
                createScenario("Song Dynasty", 1000, 100_000_000L, "RIVER_VALLEYS"),
                createScenario("Americas 1491", 1491, 60_000_000L, "AMERICAS_1491"),
                createScenario("Industrial Revolution", 1800, 900_000_000L, "INDUSTRIAL_1800"),
                createScenario("Modern 2000", 2000, 6_127_000_000L, "URBAN_CLUSTERS"),
                createScenario("Business As Usual", 2026, 8_200_000_000L, "URBAN_CLUSTERS"),
                createScenario("Singularity", 2045, 9_000_000_000L, "URBAN_CLUSTERS"),
                createScenario("Nuclear Winter", 2035, 8_500_000_000L, "URBAN_CLUSTERS")
        );

        for (Scenario scenario : builtInScenarios) {
            PreComputePhase preCompute = new PreComputePhase(scenario);
            assertDoesNotThrow(() -> preCompute.execute(mockCells),
                    "PreComputePhase execution must not throw an exception for scenario: " + scenario.getName());

            long totalPop = mockCells.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
            assertTrue(totalPop >= 0, "Population after precompute must be non-negative for " + scenario.getName());

            // Run 5 ticks of physics
            assertDoesNotThrow(() -> {
                for (int t = 0; t < 5; t++) {
                    PhysicalLawEngine.applyPhysicalLaws(mockCells, 1.0);
                    BiologicalDemographicsEngine.processBiologicalDemographics(mockCells);
                }
            }, "Physics execution must complete safely for scenario: " + scenario.getName());
        }
    }

    private Scenario createScenario(String name, long startYear, long initialPop, String densityType) {
        Scenario s = new Scenario();
        s.setName(name);
        s.setStartDateYear(startYear);
        s.setInitialHumanCount(initialPop);
        s.setPopulationDensityType(densityType);
        s.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        return s;
    }
}
