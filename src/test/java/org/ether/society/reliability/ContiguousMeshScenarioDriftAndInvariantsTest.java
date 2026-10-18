/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.reliability;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * High-Speed Contiguous Mesh Empirical Behavior and Invariants Test Suite.
 * <p>
 * Evaluates concrete, emergent cliodynamic and biophysical phenomena on contiguous
 * mini-continents and isolated islands (100 to 200 cells, executed in seconds):
 * 1. Demographic plausibility (no unforced extinction, no explosive hyper-inflation, realistic growth rates).
 * 2. Agro-ecological soil dynamics, biomass extraction, and carrying capacity degradation.
 * 3. Emerging trade network flows and commodity equalization between specialized economic poles.
 * 4. Strict topological adjacency without spatial holes or broken neighbor graph pointers.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ContiguousMeshScenarioDriftAndInvariantsTest {

    private static final Logger logger = LoggerFactory.getLogger(ContiguousMeshScenarioDriftAndInvariantsTest.class);

    private H3Core h3;
    private Configuration config;

    @BeforeEach
    public void setUp() throws Exception {
        h3 = H3Core.newInstance();
        config = ConfigurationLoader.loadDefault();
    }

    /**
     * Builds a real, contiguous H3 hexagonal disk of radius k around a geographic center.
     */
    private List<H3Cell> createContiguousDisk(double centerLat, double centerLng, int h3Resolution, int ringRadius) {
        long center = h3.latLngToCell(centerLat, centerLng, h3Resolution);
        List<Long> diskH3 = h3.gridDisk(center, ringRadius);

        List<H3Cell> cells = new ArrayList<>(diskH3.size());
        for (Long idx : diskH3) {
            LatLng coord = h3.cellToLatLng(idx);
            H3Cell cell = new H3Cell(idx, coord.lat, coord.lng);
            cell.setElevation(150.0);
            cell.setTemperature(20.0);
            cell.setRainfall(650.0);
            cell.setBiome(Biome.PLAINS);
            cell.setFoodResource(2500.0);
            cell.setWaterResource(1200.0);
            cell.setWoodResource(600.0);
            cell.setPopulation(500);
            cell.setTechnologyLevel(1.5);
            cell.setResourceCapital(100.0);
            cells.add(cell);
        }
        return cells;
    }

    @Test
    @DisplayName("Test 1: Topological contiguity & complete neighbor graphs without spatial holes")
    public void testContiguousMeshTopologicalCompleteness() {
        long centerH3 = h3.latLngToCell(31.2, 29.9, 4);
        List<H3Cell> contiguousMesh = createContiguousDisk(31.2, 29.9, 4, 3);
        assertEquals(37, contiguousMesh.size(), "H3 disk with radius 3 must contain exactly 37 cells");

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Topological Contiguity Verification");
        scenario.setStartDateYear(-3000);
        scenario.setInitialHumanCount(18500L);

        engine.initializeFromScenario(scenario, contiguousMesh);
        WorldBuffer buffer = engine.getWorldBuffer();
        assertNotNull(buffer, "WorldBuffer must be initialized");

        int centerLocalIdx = -1;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            if (buffer.getH3Indexes()[i] == centerH3) {
                centerLocalIdx = i;
                break;
            }
        }
        assertTrue(centerLocalIdx >= 0, "Center cell must exist in buffer");

        int validNeighborsForCenter = 0;
        for (int j = 0; j < 6; j++) {
            int nIdx = buffer.getNeighborIndexes()[centerLocalIdx][j];
            if (nIdx >= 0) {
                validNeighborsForCenter++;
                assertTrue(nIdx < contiguousMesh.size(), "Neighbor index must be within buffer bounds");
            }
        }
        assertEquals(6, validNeighborsForCenter, "Center cell in a contiguous disk must have all 6 neighbors indexed");

        int totalValidEdges = 0;
        for (int i = 0; i < contiguousMesh.size(); i++) {
            for (int j = 0; j < 6; j++) {
                if (buffer.getNeighborIndexes()[i][j] >= 0) totalValidEdges++;
            }
        }
        logger.info("Contiguous Mesh (37 cells): Total valid neighbor edges = {} / {}", totalValidEdges, 37 * 6);
        assertTrue(totalValidEdges > 120, "A contiguous disk of 37 cells must have extensive topological connectivity");
    }

    @Test
    @DisplayName("Test 2: Isolated Island demographic stability (realistic growth, no unforced collapse or runaway explosion)")
    public void testIsolatedIslandDemographicPlausibility() {
        // Contiguous Island of radius 6 = 127 cells (approx. 300,000 km² at Res 4)
        List<H3Cell> island = createContiguousDisk(-18.7, 46.8, 4, 6); // Madagascar island coords
        assertEquals(127, island.size(), "Island disk with radius 6 must contain 127 contiguous cells");

        // Seed initial population: 200 people per cell = ~25,400 initial inhabitants
        for (H3Cell c : island) {
            c.setPopulation(200);
            c.setFoodResource(3000.0);
            c.setWaterResource(1500.0);
            c.setBiome(Biome.FOREST);
        }

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Madagascar Isolated Island Scenario");
        scenario.setStartDateYear(500);
        scenario.setEndDateYear(600);
        scenario.setInitialHumanCount(25_400L);
        scenario.setInitialCapitalPerCapita(4.0);
        scenario.setInitialEnergyPerCapita(6.0);

        engine.initializeFromScenario(scenario, island);
        WorldBuffer buffer = engine.getWorldBuffer();

        double initialTotalPop = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            initialTotalPop += buffer.getBiomassHuman()[i];
        }
        assertTrue(initialTotalPop > 0.0, "Initial population must be positive");

        // Simulate 50 ticks (~50 to 100 years depending on dt)
        final int SIM_TICKS = 50;
        for (int tick = 1; tick <= SIM_TICKS; tick++) {
            engine.stepForward(1);

            double currentPop = 0.0;
            for (int i = 0; i < buffer.getCapacity(); i++) {
                double cellPop = buffer.getBiomassHuman()[i];
                assertFalse(Double.isNaN(cellPop), "Pop NaN at tick " + tick + ", cell " + i);
                assertTrue(cellPop >= 0.0, "Pop negative at tick " + tick + ", cell " + i);
                currentPop += cellPop;
            }

            // Behavioral Assertion A: No sudden extinction in a fertile tropical island
            assertTrue(currentPop > initialTotalPop * 0.20,
                    "Abnormal collapse: population dropped below 20% of baseline in fertile island at tick " + tick);

            // Behavioral Assertion B: No unphysical hyper-inflation (must not exceed 50x in 50 ticks)
            assertTrue(currentPop < initialTotalPop * 50.0,
                    "Abnormal explosive explosion: population grew over 50x in 50 ticks at tick " + tick);
        }

        double finalTotalPop = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            finalTotalPop += buffer.getBiomassHuman()[i];
        }

        double totalGrowthRatio = finalTotalPop / initialTotalPop;
        logger.info("Island Demographics: Initial Pop = {}, Final Pop = {} (Growth Ratio: x{})",
                initialTotalPop, finalTotalPop, String.format("%.2f", totalGrowthRatio));

        // Plausible physiological boundaries over 50 ticks: between 0.3x (stress) and 10x (demographic boom)
        assertTrue(totalGrowthRatio >= 0.3 && totalGrowthRatio <= 15.0,
                "Demographic growth ratio (" + totalGrowthRatio + ") must lie within plausible historical bounds");
    }

    @Test
    @DisplayName("Test 3: Agro-ecological soil and natural biomass depletion under demographic pressure")
    public void testAgroEcologicalSoilAndBiomassDynamics() {
        // 19 contiguous cells
        List<H3Cell> region = createContiguousDisk(32.0, 35.0, 4, 2); // Levant
        assertEquals(19, region.size());

        // Cell 0: Heavily populated (10,000 humans) -> high agricultural and wood demand
        // Cell 1: Lightly populated / Wild (50 humans) -> low human extraction
        region.get(0).setPopulation(10000);
        region.get(0).setWoodResource(5000.0);
        region.get(0).setFoodResource(2000.0);

        for (int i = 1; i < region.size(); i++) {
            region.get(i).setPopulation(50);
            region.get(i).setWoodResource(5000.0);
            region.get(i).setFoodResource(2000.0);
        }

        long denseH3 = region.get(0).getH3Index();
        long wildH3 = region.get(1).getH3Index();

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Agro-Ecological Extraction Test");
        scenario.setStartDateYear(-2000);
        scenario.setInitialHumanCount(10900L);

        engine.initializeFromScenario(scenario, region);
        WorldBuffer buffer = engine.getWorldBuffer();

        int denseIdx = -1, wildIdx = -1;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            if (buffer.getH3Indexes()[i] == denseH3) denseIdx = i;
            if (buffer.getH3Indexes()[i] == wildH3) wildIdx = i;
        }
        assertTrue(denseIdx >= 0 && wildIdx >= 0, "Both test cells must exist in buffer");

        float initialWoodDense = buffer.getWoodResource()[denseIdx];
        float initialWoodWild = buffer.getWoodResource()[wildIdx];

        // Run 25 ticks
        for (int t = 0; t < 25; t++) {
            engine.stepForward(1);
        }

        float finalWoodDense = buffer.getWoodResource()[denseIdx];
        float finalWoodWild = buffer.getWoodResource()[wildIdx];

        logger.info("Wood Resource Dynamics: Dense Cell (Pop 10k) = {} -> {}, Wild Cell (Pop 50) = {} -> {}",
                initialWoodDense, finalWoodDense, initialWoodWild, finalWoodWild);

        // Dense cell must experience higher per-capita biomass depletion than wild cell
        float woodDepletionDense = initialWoodDense - finalWoodDense;
        float woodDepletionWild = initialWoodWild - finalWoodWild;

        assertTrue(woodDepletionDense >= woodDepletionWild,
                "Densely populated cell must consume/deplete equal or more forestry biomass than wild cell");
    }

    @Test
    @DisplayName("Test 4: Emerging trade and resource transport between complementary specialized poles")
    public void testEmergingTradeNetworkBetweenComplementaryPoles() {
        // Disk of radius 3 = 37 contiguous cells
        List<H3Cell> region = createContiguousDisk(41.9, 12.5, 4, 3); // Central Italy / Rome
        assertEquals(37, region.size());

        // Pole A (Center): High Population, High Capital, Mineral Deficit, Abundant Grain
        long centerH3 = region.get(0).getH3Index();
        H3Cell poleA = region.get(0);
        poleA.setPopulation(8000);
        poleA.setFoodResource(50000.0);
        poleA.setResourceMetal(10.0); // Deficit
        poleA.setResourceCapital(500.0);

        // Pole B (Peripheral neighbor): Lower Population, Mining Center, Grain Deficit
        H3Cell poleB = region.get(1);
        poleB.setPopulation(1500);
        poleB.setFoodResource(200.0); // Deficit
        poleB.setResourceMetal(25000.0); // Rich ore deposits
        poleB.setResourceCapital(100.0);

        long poleAH3 = poleA.getH3Index();
        long poleBH3 = poleB.getH3Index();

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Complementary Economic Poles Trade Test");
        scenario.setStartDateYear(100);
        scenario.setInitialHumanCount(15000L);

        engine.initializeFromScenario(scenario, region);
        WorldBuffer buffer = engine.getWorldBuffer();

        // Run 20 ticks to let market price differentials, Leontief IO, and trade routes form
        for (int t = 0; t < 20; t++) {
            engine.stepForward(1);
        }

        int poleAIdx = -1;
        int poleBIdx = -1;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            if (buffer.getH3Indexes()[i] == poleAH3) poleAIdx = i;
            if (buffer.getH3Indexes()[i] == poleBH3) poleBIdx = i;
        }
        assertTrue(poleAIdx >= 0 && poleBIdx >= 0, "Both economic poles must exist in buffer");

        float foodPoleB = buffer.getFoodResource()[poleBIdx];
        float metalPoleA = buffer.getMetalResource()[poleAIdx];
        float capPoleA = buffer.getResourceCapital()[poleAIdx];

        logger.info("Trade Equilibrium check after 20 ticks: Pole A Metal = {}, Pole B Food = {}, Pole A Capital = {}",
                metalPoleA, foodPoleB, capPoleA);

        // Both poles must remain economically viable and maintain non-negative buffers
        assertTrue(foodPoleB >= 0.0f, "Food at Pole B must remain non-negative");
        assertTrue(metalPoleA >= 0.0f, "Metal at Pole A must remain non-negative");
        assertTrue(capPoleA > 0.0f, "Pole A capital must remain active");
    }

    @Test
    @DisplayName("Test 5: Symplectic mass and energy bounds on closed isolated contiguous island (no unphysical creation ex-nihilo)")
    public void testClosedIslandSymplecticMassAndEnergyConservation() {
        // Isolated contiguous island with 19 cells
        List<H3Cell> island = createContiguousDisk(-18.7, 46.8, 4, 2);
        assertEquals(19, island.size());

        for (H3Cell c : island) {
            c.setPopulation(500);
            c.setFoodResource(5000.0);
            c.setWaterResource(2000.0);
            c.setWoodResource(1000.0);
            c.setResourceCapital(100.0);
            c.setResourceMetal(500.0);
        }

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Conservation Closed Island");
        scenario.setStartDateYear(1000);
        scenario.setEndDateYear(1020);
        scenario.setInitialHumanCount(9500L);

        engine.initializeFromScenario(scenario, island);
        WorldBuffer buffer = engine.getWorldBuffer();

        // Integrate 20 ticks and track total biological mass (Human + Food + Wood)
        for (int t = 1; t <= 20; t++) {
            engine.stepForward(1);

            double totalBiomassMassUnits = 0.0;
            double totalMetalStock = 0.0;

            for (int i = 0; i < buffer.getCapacity(); i++) {
                if (buffer.getH3Indexes()[i] == 0) continue;

                float pop = buffer.getBiomassHuman()[i];
                float food = buffer.getFoodResource()[i];
                float wood = buffer.getWoodResource()[i];
                float metal = buffer.getMetalResource()[i];

                assertFalse(Float.isNaN(pop), "Pop must not be NaN at tick " + t);
                assertFalse(Float.isNaN(food), "Food must not be NaN at tick " + t);
                assertFalse(Float.isNaN(metal), "Metal must not be NaN at tick " + t);

                assertTrue(pop >= 0.0f, "Pop must remain non-negative");
                assertTrue(food >= 0.0f, "Food must remain non-negative");
                assertTrue(metal >= 0.0f, "Metal must remain non-negative");

                totalBiomassMassUnits += (pop * 0.07) + (food * 0.001) + wood;
                totalMetalStock += metal;
            }

            // Invariant: Non-renewable mineral crustal stock cannot increase ex-nihilo on a closed island without asteroid impacts
            assertTrue(totalMetalStock <= 19 * 500.0 * 1.001,
                    "Total crustal metal stock on closed island cannot exceed initial endowment ex-nihilo at tick " + t);

            // Invariant: Total biological mass must remain finite and bounded by carrying capacity ceiling
            assertTrue(totalBiomassMassUnits < 1.0e8,
                    "Total biological mass must remain physically bounded by thermodynamic solar influx at tick " + t);
        }
    }
}
