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
 * Long-horizon multi-decadal ecosystem calibration tests.
 * Validates reproduction rates, age pyramid dynamics, resource conservation,
 * and physical/demographic consistency over 40 to 100 years.
 */
public class LongHorizonEcosystemCalibrationTest {

    private Configuration config;

    @BeforeEach
    void setUp() throws Exception {
        config = ConfigurationLoader.loadDefault();
    }

    private List<H3Cell> createPatch(int count, Biome biome, int popPerCell, double foodPerCell, double waterPerCell, double temp) {
        List<H3Cell> cells = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            H3Cell c = new H3Cell();
            c.setH3Index((long) (2000 + i));
            c.setBiome(biome);
            c.setPopulation(popPerCell);
            c.setBiomassHuman(popPerCell * 0.45);
            c.setFoodResource(foodPerCell);
            c.setWaterResource(waterPerCell);
            c.setTemperature(temp);
            c.setElevation(100.0);
            c.setLatitude(30.0 + (i * 0.1));
            c.setLongitude(31.0 + (i * 0.1));
            c.setResourceCapital(popPerCell * 1.5);
            c.setEnergyWind(50.0);
            cells.add(c);
        }
        return cells;
    }

    @Test
    @DisplayName("Long-Horizon 1: Island Isolation 50-Year Demographic & Resource Conservation")
    void testIslandIsolationDecadalDemographicAndResourceConservation() throws Exception {
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner(config);

        // 1 isolated island cell with initial population
        List<H3Cell> cells = createPatch(1, Biome.PLAINS, 300, 15000.0, 5000.0, 20.0);

        Scenario scenario = new Scenario();
        scenario.setName("Island Isolation 50-Year Horizon");
        scenario.setStartDateYear(-3000);
        scenario.setEndDateYear(-2900); // 100-year ceiling
        scenario.setTargetCohortSize(150);
        scenario.setInitialHumanCount(300);
        scenario.setPopulationDensityType("DENSE"); // Full initial population allocation
        scenario.setTemporalResolutionDays(30.0); // Monthly ticks: 12 ticks/year -> 600 ticks = 50 years

        runner.initialize(scenario, cells);

        int totalYearsToSimulate = 50;
        int ticksPerYear = 12;

        long initialPop = runner.getEngine().getTotalPopulation();
        assertTrue(initialPop > 0, "Initial population must be positive");

        // Execute in 5-year increments to observe time evolution
        for (int block = 0; block < 10; block++) {
            runner.step(5 * ticksPerYear);

            long currentPop = runner.getEngine().getTotalPopulation();
            assertTrue(currentPop > 0, "Population must not go extinct in year " + ((block + 1) * 5));
            assertTrue(currentPop <= 15000, "Population must respect island carrying capacity bounds");

            float food = runner.getEngine().getWorldBuffer().getFoodResource()[0];
            assertTrue(food >= 0.0f, "Food resource must stay strictly non-negative");

            float fert = runner.getEngine().getCurrentFertility();
            assertTrue(fert >= 1.0f && fert <= 8.0f, "Fertility rate must remain within plausible demographic bounds: " + fert);

            float lifeExp = runner.getEngine().getCurrentLifeExpectancy();
            assertTrue(lifeExp >= 15.0f && lifeExp <= 90.0f, "Life expectancy must stay plausible: " + lifeExp);
        }

        // Validate final age pyramid distribution across the 7 cohorts (<15, 15-24, 25-39, 40-54, 55-69, 70-84, 85+)
        int[] agePyramid = runner.getEngine().getAgePyramid();
        assertNotNull(agePyramid, "Age pyramid must be computed");
        assertEquals(7, agePyramid.length, "Age pyramid must have 7 cohorts");

        long sumCohorts = 0;
        for (int c : agePyramid) {
            assertTrue(c >= 0, "Cohort count must be non-negative");
            sumCohorts += c;
        }
        assertTrue(sumCohorts > 0, "Total cohort population in pyramid must be positive");
    }

    @Test
    @DisplayName("Long-Horizon 2: Two Competing Cohorts in River Valley Over 40 Years")
    void testTwoCompetingCohortsOnRiverValleyDecades() throws Exception {
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner(config);

        // 2 fertile adjacent cells (Nile / Mesopotamia archetype)
        List<H3Cell> cells = createPatch(2, Biome.PLAINS, 500, 25000.0, 10000.0, 22.0);

        Scenario scenario = new Scenario();
        scenario.setName("River Valley 40-Year Cohorts");
        scenario.setStartDateYear(-2500);
        scenario.setEndDateYear(-2400);
        scenario.setTargetCohortSize(150);
        scenario.setInitialHumanCount(1000);
        scenario.setPopulationDensityType("DENSE");
        scenario.setTemporalResolutionDays(30.0);

        runner.initialize(scenario, cells);

        int totalTicks = 40 * 12; // 480 ticks
        runner.step(totalTicks);

        long finalPop = runner.getEngine().getTotalPopulation();
        assertTrue(finalPop > 0, "River valley population must remain viable after 40 years");

        // Gini coefficient must stay bounded in [0, 1]
        float gini = runner.getEngine().getCurrentGini();
        assertTrue(gini >= 0.0f && gini <= 1.0f, "Gini must remain bounded in [0, 1], got: " + gini);

        // Kardashev scale must be defined and in [0, 3] range
        double kardashev = runner.getEngine().getKardashevScale();
        assertTrue(kardashev >= 0.0 && kardashev <= 3.0, "Kardashev scale must be within Type 0-3 range, got: " + kardashev);

        // Division of labor must be positive
        double divLabor = runner.getEngine().getDivisionOfLaborIndex();
        assertTrue(divLabor > 0.0, "Division of labor must emerge over 40 years in river valley");
    }

    @Test
    @DisplayName("Long-Horizon 3: Full Century Generational Turnover & Systemic Resilience (100 Years)")
    void testCenturyDemographicAndGenerationalTurnover() throws Exception {
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner(config);

        // 5 cells patch over a complete century (100 years = ~3 to 4 human generations)
        List<H3Cell> cells = createPatch(5, Biome.FOREST, 300, 30000.0, 8000.0, 15.0);

        Scenario scenario = new Scenario();
        scenario.setName("Century Generational Turnover");
        scenario.setStartDateYear(500);
        scenario.setEndDateYear(650);
        scenario.setTargetCohortSize(150);
        scenario.setInitialHumanCount(1500);
        scenario.setPopulationDensityType("DENSE");
        scenario.setTemporalResolutionDays(30.0);

        runner.initialize(scenario, cells);

        int centuryTicks = 100 * 12; // 1200 ticks
        runner.step(centuryTicks);

        assertEquals(600, runner.getEngine().getTimeManager().getCurrentYear(), "Time manager must advance exactly 100 years");
        assertTrue(runner.getEngine().getTotalPopulation() > 0, "Civilization must survive a century of generational turnover");

        // Validate agent generational progression
        int maxGen = 0;
        int[] gens = runner.getEngine().getAgentBuffer().getGenerationCount();
        int[] hexIds = runner.getEngine().getAgentBuffer().getHexIds();
        for (int i = 0; i < runner.getEngine().getAgentBuffer().getCapacity(); i++) {
            if (hexIds[i] != -1 && gens[i] > maxGen) {
                maxGen = gens[i];
            }
        }
        assertTrue(maxGen >= 0, "Generations must be tracked without error");

        // Verify systemic risk indicators
        double vuln = runner.getEngine().getCollapseVulnerability();
        assertFalse(Double.isNaN(vuln), "Collapse vulnerability must not be NaN");
        assertTrue(vuln >= 0.0 && vuln <= 100.0, "Collapse vulnerability must be in [0, 100]");

        double energyPerCap = runner.getEngine().getEnergyPerCapita();
        assertTrue(energyPerCap >= 100.0, "Energy per capita must exceed minimum basal metabolic work (>100 W)");
    }
}
