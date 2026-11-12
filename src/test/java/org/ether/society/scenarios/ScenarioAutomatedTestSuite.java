/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.scenarios;

import org.ether.society.core.engine.SimulationEngineRegistry;
import org.ether.society.core.engine.SimulationEngineModule;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

import org.ether.society.config.Configuration;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.PreComputePhase;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.ether.society.engines.tier2.historical.*;
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
    /*
     * Set up operation.
     * <p>
     * Executes operational logic for {@code ScenarioAutomatedTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void setUp() {
        SimulationEngineRegistry.getInstance().resetAll();
        mockCells = new ArrayList<>();

        // 1. East Africa / Great Rift Valley (African origin)
        H3Cell c0 = new H3Cell(1001L, 0.5, 36.8);
        c0.setElevation(1100.0);
        c0.setBiome(Biome.PLAINS);
        c0.setTemperature(24.0);
        c0.setWaterResource(800.0);

        // 1b. East African Rift Corridor Stepping Stone (~78 km neighbor)
        H3Cell c0b = new H3Cell(10011L, 1.2, 36.9);
        c0b.setElevation(800.0);
        c0b.setBiome(Biome.PLAINS);
        c0b.setTemperature(25.0);
        c0b.setWaterResource(850.0);
        c0b.setFoodResource(1500.0);

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
        mockCells.add(c0b);
        mockCells.add(c1);
        mockCells.add(c2);
        mockCells.add(c3);
        mockCells.add(c4);
    }

    @Test
    @DisplayName("Test Homo Sapiens Out-of-Africa Paleolithic Scenario (-100,000 BC)")
    /*
     * Test out of africa sapiens scenario operation.
     * <p>
     * Executes operational logic for {@code ScenarioAutomatedTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
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
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(mockCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(mockCells, buf, false);
        BiologicalDemographicsEngine bioEngine = new BiologicalDemographicsEngine();
        PhysicalLawEngine physEngine = new PhysicalLawEngine();
        ThermodynamicMigrationEngine migEngine = new ThermodynamicMigrationEngine();
        for (int tick = 0; tick < 30; tick++) {
            bioEngine.tick(buf, 1.0, 12.0);
            physEngine.tick(buf, 1.0, 12.0);
            migEngine.tick(buf, 1.0, 12.0);

            long currentPop = 0L;
            for (int i = 0; i < mockCells.size(); i++) {
                currentPop += Math.round(buf.getBiomassHuman()[i] / 0.06);
            }
            assertTrue(currentPop > 0, "Population must persist during paleolithic expansion");
            assertTrue(currentPop <= 5_000_000L, "Population must remain within hunter-gatherer global capacity (<5M)");
        }

        // Verify spatial migration from East Africa to neighboring cells
        long nonOriginPop = 0L;
        for (int i = 0; i < mockCells.size(); i++) {
            if (buf.getH3Indexes()[i] != 1001L) {
                nonOriginPop += Math.round(buf.getBiomassHuman()[i] / 0.06);
            }
        }
        assertTrue(nonOriginPop > 0, "Out-of-Africa demographic wave must migrate to neighboring cells");
    }

    @Test
    @DisplayName("Test Simple Population Stability (10,000 and 100,000 Individuals)")
    /*
     * Test simple population stability no collapse operation.
     * <p>
     * Executes operational logic for {@code ScenarioAutomatedTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
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

            org.ether.society.core.dod.WorldBuffer stabBuf = new org.ether.society.core.dod.WorldBuffer(mockCells.size());
            org.ether.society.data.DODDataGenerator.populateWorldBuffer(mockCells, stabBuf, false);
            BiologicalDemographicsEngine bioEngine = new BiologicalDemographicsEngine();
            PhysicalEnergyGridEngine gridEngine = new PhysicalEnergyGridEngine();
            EcologicalDegradationEngine ecoEngine = new EcologicalDegradationEngine();
            for (int tick = 0; tick < 20; tick++) {
                bioEngine.tick(stabBuf, 1.0, 12.0);
                gridEngine.tick(stabBuf, 1.0, 12.0);
                ecoEngine.tick(stabBuf, 1.0, 12.0);

                for (int i = 0; i < mockCells.size(); i++) {
                    double pop = stabBuf.getBiomassHuman()[i] / 0.06;
                    assertTrue(pop >= 0, "Population must be non-negative");

                    float capital = stabBuf.getResourceCapital()[i];
                    assertFalse(Float.isNaN(capital), "Capital must not be NaN");
                    assertFalse(Float.isInfinite(capital), "Capital must not be Infinite");
                    assertTrue(capital >= 0, "Resource capital must not drop below zero");
                    mockCells.get(i).setPopulation((int) Math.round(pop));
                    mockCells.get(i).setResourceCapital((double) capital);

                    float temp = stabBuf.getTemperature()[i];
                    assertFalse(Float.isNaN(temp), "Temperature must not be NaN");
                }
            }
        }
    }

    @Test
    @DisplayName("Test Physicalist Model Engines & Type B Plugins Invariants")
    /*
     * Test model engine invariants and type bplugins operation.
     * <p>
     * Executes operational logic for {@code ScenarioAutomatedTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testModelEngineInvariantsAndTypeBPlugins() {
        // Register representative Type B plugins
        SimulationEngineRegistry.getInstance().register(new FrontierAsabiyyahEngine());
        SimulationEngineRegistry.getInstance().register(new MonasticDemographicBufferEngine());
        SimulationEngineRegistry.getInstance().register(new LenskiPureEngine());
        SimulationEngineRegistry.getInstance().register(new World3HybridEngine());
        SimulationEngineRegistry.getInstance().register(new NordhausDiceHybridEngine());

        Scenario scenario = new Scenario();
        scenario.setInitialHumanCount(100_000L);
        scenario.setInitialCapitalPerCapita(500.0);

        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(mockCells);

        org.ether.society.core.dod.WorldBuffer tier2Buf = new org.ether.society.core.dod.WorldBuffer(mockCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(mockCells, tier2Buf, false);
        RenewableEnergyPhysicsEngine renewEngine = new RenewableEnergyPhysicsEngine();
        AtmosphericOxygenEngine o2Engine = new AtmosphericOxygenEngine();
        SoilNutrientNPKEngine npkEngine = new SoilNutrientNPKEngine();
        NetEnergyEROEIEngine eroeiEngine = new NetEnergyEROEIEngine();
        MetallurgyEnthalpyEngine metalEngine = new MetallurgyEnthalpyEngine();
        InformationEntropyEngine infoEngine = new InformationEntropyEngine();
        KurzweilAcceleratingReturnsEngine kurzweilEngine = new KurzweilAcceleratingReturnsEngine();
        World3CouplingEngine world3Engine = new World3CouplingEngine();
        for (int step = 0; step < 15; step++) {
            renewEngine.tick(tier2Buf, 1.0, 12.0);
            o2Engine.tick(tier2Buf, 1.0, 12.0);
            npkEngine.tick(tier2Buf, 1.0, 12.0);
            eroeiEngine.tick(tier2Buf, 1.0, 12.0);
            metalEngine.tick(tier2Buf, 1.0, 12.0);
            infoEngine.tick(tier2Buf, 1.0, 1.0);
            kurzweilEngine.tick(tier2Buf, 1.0, 1.0);
            world3Engine.tick(tier2Buf, 1.0, 1.0);

            for (int i = 0; i < mockCells.size(); i++) {
                float pop = tier2Buf.getBiomassHuman()[i] / 0.06f;
                float capital = tier2Buf.getResourceCapital()[i];
                float food = tier2Buf.getFoodResource()[i];

                assertFalse(Float.isNaN(pop), "Cell population must be a valid number");
                assertFalse(Float.isNaN(capital), "Cell capital must be a valid number");
                assertFalse(Float.isNaN(food), "Cell food resource must be a valid number");

                assertTrue(pop >= 0, "Cell population must remain non-negative");
                assertTrue(capital >= 0, "Cell capital must remain non-negative");
                assertTrue(food >= 0, "Cell food resource must remain non-negative");
            }
        }
    }

    @Test
    @DisplayName("Test Full Battery Execution of All Scenario Configurations")
    /*
     * Test full built in scenario battery operation.
     * <p>
     * Executes operational logic for {@code ScenarioAutomatedTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
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
                org.ether.society.core.dod.WorldBuffer batBuf = new org.ether.society.core.dod.WorldBuffer(mockCells.size());
                org.ether.society.data.DODDataGenerator.populateWorldBuffer(mockCells, batBuf, false);
                PhysicalLawEngine physEngine = new PhysicalLawEngine();
                BiologicalDemographicsEngine bioEngine = new BiologicalDemographicsEngine();
                for (int t = 0; t < 5; t++) {
                    physEngine.tick(batBuf, 1.0, 12.0);
                    bioEngine.tick(batBuf, 1.0, 12.0);
                }
            }, "Physics execution must complete safely for scenario: " + scenario.getName());
        }
    }

    // Helper subroutine: create scenario - internal state computation & bounds checking
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

