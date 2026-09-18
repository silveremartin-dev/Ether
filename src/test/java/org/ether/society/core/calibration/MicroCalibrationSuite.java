/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.calibration;

import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.headless.HeadlessSimulationRunner;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Micro-calibration test suite for validating core physics, demographics, and ecology
 * on small geographic patches (1 to 30 H3 cells) and short temporal durations across key epochs.
 */
public class MicroCalibrationSuite {

    private Configuration config;

    @BeforeEach
    void setUp() throws Exception {
        config = ConfigurationLoader.loadDefault();
    }

    private List<H3Cell> createCellPatch(int count, Biome biome, int popPerCell, double foodPerCell, double waterPerCell, double temp) {
        List<H3Cell> cells = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            H3Cell c = new H3Cell();
            c.setH3Index((long) (1000 + i));
            c.setBiome(biome);
            c.setPopulation(popPerCell);
            c.setBiomassHuman(popPerCell * 0.45);
            c.setFoodResource(foodPerCell);
            c.setWaterResource(waterPerCell);
            c.setTemperature(temp);
            c.setElevation(150.0);
            c.setLatitude(45.0 + (i * 0.1));
            c.setLongitude(5.0 + (i * 0.1));
            c.setResourceCapital(popPerCell * 2.0);
            c.setEnergyWind(50.0);
            cells.add(c);
        }
        return cells;
    }

    @Test
    @DisplayName("Micro 1: Paleolithic Band Survival (-50,000 BP, 3 cells, pop 150)")
    void testPaleolithicBandSurvival() throws Exception {
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner(config);

        List<H3Cell> cells = createCellPatch(3, Biome.FOREST, 50, 3000.0, 500.0, 10.0);

        Scenario scenario = new Scenario();
        scenario.setName("Micro Paleolithic Band");
        scenario.setStartDateYear(-50000);
        scenario.setEndDateYear(-49000);
        scenario.setTargetCohortSize(50);
        scenario.setInitialHumanCount(150);

        runner.initialize(scenario, cells);
        runner.step(20);

        long pop = runner.getEngine().getTotalPopulation();
        assertTrue(pop > 0, "Paleolithic population must not go extinct instantly under normal food conditions");
        assertTrue(pop < 5000, "Paleolithic band must not experience unbounded exponential explosion in 20 steps");
        assertFalse(Float.isNaN(runner.getEngine().getCurrentGini()), "Gini must remain a valid real number");
    }

    @Test
    @DisplayName("Micro 2: Neolithic Agrarian Settlement (-7,000 BP, 8 cells, pop 800)")
    void testNeolithicAgrarianSettlement() throws Exception {
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner(config);

        List<H3Cell> cells = createCellPatch(8, Biome.PLAINS, 100, 8000.0, 1000.0, 18.0);

        Scenario scenario = new Scenario();
        scenario.setName("Micro Neolithic Village");
        scenario.setStartDateYear(-7000);
        scenario.setEndDateYear(-6000);
        scenario.setTargetCohortSize(100);
        scenario.setInitialHumanCount(800);

        runner.initialize(scenario, cells);
        runner.step(25);

        long pop = runner.getEngine().getTotalPopulation();
        assertTrue(pop >= 100, "Neolithic settlement must maintain viable agrarian population");
        assertTrue(runner.getEngine().getWorldBuffer() != null, "World buffer must be allocated");
        assertTrue(runner.getEngine().getWorldBuffer().getFoodResource()[0] >= 0.0f, "Food resource must remain non-negative");
    }

    @Test
    @DisplayName("Micro 3: Bronze Age City-State Micro-Economy (-2,500 BP, 12 cells, pop 6,000)")
    void testBronzeAgeCityStateMicroEconomy() throws Exception {
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner(config);

        List<H3Cell> cells = createCellPatch(12, Biome.HILLS, 500, 20000.0, 2000.0, 22.0);

        Scenario scenario = new Scenario();
        scenario.setName("Micro Bronze Age City State");
        scenario.setStartDateYear(-2500);
        scenario.setEndDateYear(-2000);
        scenario.setTargetCohortSize(150);
        scenario.setInitialHumanCount(6000);

        runner.initialize(scenario, cells);
        runner.step(30);

        float gini = runner.getEngine().getCurrentGini();
        assertTrue(gini >= 0.0f && gini <= 1.0f, "Gini inequality must remain bounded in [0, 1], got: " + gini);
        assertTrue(runner.getEngine().getTotalPopulation() > 0, "Population should remain active");
    }

    @Test
    @DisplayName("Micro 4: Classical Antiquity Urbanization (100 AD, 20 cells, pop 20,000)")
    void testClassicalAntiquityUrbanization() throws Exception {
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner(config);

        List<H3Cell> cells = createCellPatch(20, Biome.PLAINS, 1000, 50000.0, 5000.0, 20.0);

        Scenario scenario = new Scenario();
        scenario.setName("Micro Classical Antiquity");
        scenario.setStartDateYear(100);
        scenario.setEndDateYear(300);
        scenario.setTargetCohortSize(150);
        scenario.setInitialHumanCount(20000);

        runner.initialize(scenario, cells);
        runner.step(30);

        double vuln = runner.getEngine().getCollapseVulnerability();
        assertTrue(vuln >= 0.0 && vuln <= 100.0, "Collapse vulnerability must stay in valid percentage range [0, 100]");
        assertTrue(runner.getEngine().getDivisionOfLaborIndex() >= 0.0, "Division of labor index must be non-negative");
    }

    @Test
    @DisplayName("Micro 5: Medieval Demographic Stress (1347 AD, 15 cells, pop 15,000)")
    void testMedievalDemographicStress() throws Exception {
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner(config);

        List<H3Cell> cells = createCellPatch(15, Biome.FOREST, 1000, 10000.0, 1000.0, 12.0);

        Scenario scenario = new Scenario();
        scenario.setName("Micro Medieval Stress");
        scenario.setStartDateYear(1347);
        scenario.setEndDateYear(1450);
        scenario.setTargetCohortSize(150);
        scenario.setInitialHumanCount(15000);

        runner.initialize(scenario, cells);
        runner.step(20);

        long pop = runner.getEngine().getTotalPopulation();
        assertTrue(pop >= 0, "Population must never drop below 0 under high stress/mortality");
    }

    @Test
    @DisplayName("Micro 6: Industrial Energy Transition (1850 AD, 10 cells, pop 50,000)")
    void testIndustrialEnergyTransition() throws Exception {
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner(config);

        List<H3Cell> cells = createCellPatch(10, Biome.HILLS, 5000, 100000.0, 10000.0, 14.0);
        for (H3Cell c : cells) {
            c.setResourceMetal(30000.0);
            c.setResourceCapital(50000.0);
        }

        Scenario scenario = new Scenario();
        scenario.setName("Micro Industrial Ramp");
        scenario.setStartDateYear(1850);
        scenario.setEndDateYear(2000);
        scenario.setTargetCohortSize(150);
        scenario.setInitialHumanCount(50000);

        runner.initialize(scenario, cells);
        runner.step(25);

        assertTrue(runner.getEngine().getTotalPopulation() > 0, "Industrial population must remain viable");
        assertFalse(Double.isNaN(runner.getEngine().getCarbonFootprint()), "Carbon footprint should compute without NaN");
    }

    @Test
    @DisplayName("Micro 7: Island Isolation Carrying Capacity (-5,000 BP, 1 cell, pop 100)")
    void testIslandIsolationCarryingCapacity() throws Exception {
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner(config);

        List<H3Cell> cells = createCellPatch(1, Biome.BEACH, 100, 1500.0, 300.0, 24.0);

        Scenario scenario = new Scenario();
        scenario.setName("Micro Island Isolation");
        scenario.setStartDateYear(-5000);
        scenario.setEndDateYear(-4000);
        scenario.setTargetCohortSize(50);
        scenario.setInitialHumanCount(100);

        runner.initialize(scenario, cells);
        runner.step(40);

        long pop = runner.getEngine().getTotalPopulation();
        assertTrue(pop >= 0, "Isolated population must stay non-negative");
        assertTrue(pop <= 5000, "Isolated 1-cell ecosystem must respect local carrying capacity limits");
    }

    @Test
    @DisplayName("Micro 8: Extreme Arid Desert Scarcity (500 AD, 5 cells, pop 250)")
    void testExtremeAridDesertScarcity() throws Exception {
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner(config);

        List<H3Cell> cells = createCellPatch(5, Biome.DESERT, 50, 100.0, 20.0, 42.0);

        Scenario scenario = new Scenario();
        scenario.setName("Micro Arid Scarcity");
        scenario.setStartDateYear(500);
        scenario.setEndDateYear(600);
        scenario.setTargetCohortSize(50);
        scenario.setInitialHumanCount(250);

        runner.initialize(scenario, cells);

        assertDoesNotThrow(() -> runner.step(15), "Engine must gracefully simulate extreme arid scarcity without numerical breakdown");
        long pop = runner.getEngine().getTotalPopulation();
        assertTrue(pop >= 0, "Desert population must remain non-negative");
    }
}
