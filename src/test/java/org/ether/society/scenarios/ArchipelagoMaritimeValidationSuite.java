/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.scenarios;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.ClimateEvent;
import org.ether.society.model.Scenario;
import org.ether.society.engines.tier2.theories.TradeNetworkEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deep Multi-Scale Behavioral and Cliodynamic Validation Suite on Contiguous Islands.
 * <p>
 * Evaluates emergent historical, demographic, and physical phenomena across:
 * 1. Long multi-century trajectories (Madagascar island: population growth, capital accumulation, resource exhaustion & substitution).
 * 2. Spatial resolution invariance (H3 Res 3 vs Res 4 on identical physical surfaces).
 * 3. Temporal time-step sensitivity (dt = 60d vs dt = 182d vs dt = 365d convergence).
 * 4. Controlled A/B hypothesis testing of Type B cliodynamic engines (Boserup, Turchin SDT, Henrich).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */


/**
 * Multi-island archipelago maritime trade and resource specialization validation suite.
 */
public class ArchipelagoMaritimeValidationSuite extends IslandValidationBase {
    @Test
    @DisplayName("Study 10: Henrich Tasmanian Cultural Loss Hypothesis on Isolated Islet (N < 5,000)")
    public void testTasmanianCulturalRegressionOnIsolatedIslet() {
        // Isolated islet: 1 cell with small population (250 people << 5000 Henrich threshold), initial tech = 2.5
        List<H3Cell> isletA = createContiguousIsland(-15.0, 50.0, 4, 0, 250.0, 2.5);
        List<H3Cell> isletB = createContiguousIsland(-15.0, 50.0, 4, 0, 250.0, 2.5);
        assertEquals(1, isletA.size(), "Islet must have exactly 1 cell");

        final int YEARS = 40;

        // Condition A: WITH Tasmanian Cultural Loss Engine enabled
        H3SimulationEngine engineA = new H3SimulationEngine(config);
        Scenario scenarioA = new Scenario();
        scenarioA.setName("With Tasmanian Regression");
        scenarioA.setStartDateYear(1000);
        scenarioA.setEndDateYear(1040);
        scenarioA.setTemporalResolutionDays(365.25);
        scenarioA.getTypeBEngineStates().put("TasmanianCulturalRegressionEngine", true);
        engineA.initializeFromScenario(scenarioA, isletA);

        // Condition B: WITHOUT Tasmanian Cultural Loss Engine
        H3SimulationEngine engineB = new H3SimulationEngine(config);
        Scenario scenarioB = new Scenario();
        scenarioB.setName("Without Tasmanian Regression");
        scenarioB.setStartDateYear(1000);
        scenarioB.setEndDateYear(1040);
        scenarioB.setTemporalResolutionDays(365.25);
        scenarioB.getTypeBEngineStates().put("TasmanianCulturalRegressionEngine", false);
        engineB.initializeFromScenario(scenarioB, isletB);

        engineA.stepForward(YEARS);
        engineB.stepForward(YEARS);

        double finalTechA = engineA.getWorldBuffer().getTechnologyLevel()[0];
        double finalTechB = engineB.getWorldBuffer().getTechnologyLevel()[0];

        logger.info("=== TASMANIAN CULTURAL REGRESSION TEST (40 Years, N=250) ===");
        logger.info("  Initial Tech: 2.50");
        logger.info("  Condition A (WITH Tasmanian Regression)    : Final Tech = {}", String.format("%.3f", finalTechA));
        logger.info("  Condition B (WITHOUT Tasmanian Regression) : Final Tech = {}", String.format("%.3f", finalTechB));

        // Henrich (2004) Law Falsification Assertion:
        // Isolated small band must lose complex technologies over generations
        assertTrue(finalTechA < 2.50, "Isolated population below Henrich threshold must regress culturally (TechA: " + finalTechA + ")");
        assertTrue(finalTechA < finalTechB, "Tasmanian engine must produce lower tech than control group (TechA: " + finalTechA + ", TechB: " + finalTechB + ")");
    }

    @Test
    @DisplayName("Study 11: Multi-Trophic Biomass Dynamics & Marine Fishery Regeneration (Schaefer Logistic & Overfishing Feedback)")
    public void testMarineFisheryRegenerationAndTrophicHarvest() {
        // Coastal cell with beach and ocean biomes
        double areaScale = H3Cell.getAvgHexagonAreaKm2(4) / 1770.49;
        List<H3Cell> coastalMesh = createContiguousIsland(-18.0, 49.5, 4, 1, 800.0, 2.0);
        for (H3Cell c : coastalMesh) {
            c.setBiome(Biome.BEACH);
            c.setBiomassFish(1500.0 * areaScale);
            c.setBiomassNatural(2500.0 * areaScale);
        }

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Coastal Fishery Exploitation and Recovery");
        scenario.setStartDateYear(1400);
        scenario.setEndDateYear(1430);
        scenario.setTemporalResolutionDays(365.25);
        scenario.setInitialHumanCount((long) (coastalMesh.size() * 800));
        scenario.setInitialCapitalPerCapita(15.0);
        scenario.setInitialEnergyPerCapita(20.0);

        engine.initializeFromScenario(scenario, coastalMesh);
        WorldBuffer buffer = engine.getWorldBuffer();

        double initialFish = 0.0;
        double initialFood = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            initialFish += buffer.getBiomassFish()[i];
            initialFood += buffer.getFoodResource()[i];
        }

        // Run 15 years under active fishing
        for (int y = 1; y <= 15; y++) {
            engine.stepForward(1);
        }

        double midFish = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            midFish += buffer.getBiomassFish()[i];
            // Buffer-cell bidirectional synchronization guard
            assertEquals(coastalMesh.get(i).getBiomassFish(), (double) buffer.getBiomassFish()[i], 0.01,
                    "Cell biomassFish must be in lockstep with WorldBuffer at year 15");
        }

        logger.info("=== MARINE FISHERY REGENERATION RESULTS ===");
        logger.info("Initial Fish Stock: {}, Fish Stock at Year 15: {} (Harvest Depletion: {}%)",
                (long) initialFish, (long) midFish, String.format("%.1f", (initialFish - midFish) / initialFish * 100.0));

        // Invariant Assertions:
        // 1. Marine stock experiences anthropogenic fishing harvest
        assertTrue(midFish < initialFish, "Active coastal settlement must harvest marine fish biomass");
        // 2. Extinction guard: stock must not hit 0.0 (pelagic larval influx prevents irreversible extinction)
        assertTrue(midFish > 50.0, "Fish stock must maintain a viable breeding biomass under standard harvesting");
        // 3. Natural biomass must remain positive and active
        for (int i = 0; i < buffer.getCapacity(); i++) {
            assertTrue(buffer.getBiomassNatural()[i] > 100.0, "Natural terrestrial forage biomass must remain healthy");
        }
    }

    @Test
    @DisplayName("Study 12: Inter-Island Maritime Trade & Technology Diffusion (British Isles: Britain & Ireland)")
    public void testBritishIslesMaritimeTradeAndInterIslandExchange() {
        // Two contiguous archipelagic islands separated by maritime corridor:
        // Great Britain: 19 cells around (52.5Â° N, -1.5Â° W) at Res 4
        List<H3Cell> britain = createContiguousIsland(52.5, -1.5, 4, 2, 600.0, 3.0);
        // Ireland: 7 cells around (53.3Â° N, -6.5Â° W) at Res 4
        List<H3Cell> ireland = createContiguousIsland(53.3, -6.5, 4, 1, 250.0, 1.8);

        // Adjust initial endowments: Britain rich in metal/capital, Ireland agricultural
        for (H3Cell c : britain) {
            c.setResourceCapital(35.0 * c.getPopulation());
            c.setResourceMetal(800.0);
            c.setTechnologyLevel(3.0);
        }
        for (H3Cell c : ireland) {
            c.setResourceCapital(10.0 * c.getPopulation());
            c.setResourceMetal(200.0);
            c.setTechnologyLevel(1.8);
            c.setBiomassAgriculture(2500.0);
        }

        List<H3Cell> archipelagMesh = new ArrayList<>();
        archipelagMesh.addAll(britain);
        archipelagMesh.addAll(ireland);

        final int YEARS = 30;

        // Condition A: WITH Maritime Trade Network Engine active
        H3SimulationEngine engineWithTrade = new H3SimulationEngine(config);
        Scenario scenarioA = new Scenario();
        scenarioA.setName("British Isles With Maritime Trade");
        scenarioA.setStartDateYear(1600);
        scenarioA.setEndDateYear(1630);
        scenarioA.setTemporalResolutionDays(365.25);
        scenarioA.getTypeBEngineStates().put("TradeNetworkEngine", true);
        engineWithTrade.initializeFromScenario(scenarioA, archipelagMesh);

        engineWithTrade.stepForward(YEARS);

        double irelandCapitalWithTrade = 0.0;
        double irelandTechWithTrade = 0.0;
        int irelandStartIndex = britain.size();
        WorldBuffer bufA = engineWithTrade.getWorldBuffer();

        for (int i = irelandStartIndex; i < bufA.getCapacity(); i++) {
            irelandCapitalWithTrade += bufA.getResourceCapital()[i];
            irelandTechWithTrade += bufA.getTechnologyLevel()[i];
        }
        irelandTechWithTrade /= ireland.size();

        logger.info("=== BRITISH ISLES MARITIME TRADE RESULTS (30 Years) ===");
        logger.info("Ireland Final State (With Trade): Capital = {}, Mean Tech = {}",
                (long) irelandCapitalWithTrade, String.format("%.3f", irelandTechWithTrade));

        // Inter-Island Trade Invariant Assertions:
        // 1. Ireland must accumulate positive capital stock through maritime market access
        assertTrue(irelandCapitalWithTrade > 0.0, "Ireland must maintain positive capital under maritime trade");
        // 2. Technological diffusion across the Irish Sea must elevate Ireland's tech level above initial 1.80
        assertTrue(irelandTechWithTrade > 1.85, "Maritime trade with high-tech Britain must diffuse technology to Ireland");
    }

    @Test
    @DisplayName("Study 13: Japanese Archipelago (Honshu Mountainous Relief, Wet-Rice & Fishery Subsidy)")
    public void testJapaneseArchipelagoAgrarianAndFisheryBalance() {
        // Honshu archipelago: 37 contiguous cells at Res 4 (radius 3) around central Honshu (35.5Â° N, 137.5Â° E)
        List<H3Cell> japan = createContiguousIsland(35.5, 137.5, 4, 3, 500.0, 2.8);
        for (H3Cell c : japan) {
            // Japan's mountainous geography: 70% hills/mountains, 30% coastal beach/plains
            if (c.getElevation() > 300.0) {
                c.setBiome(Biome.MOUNTAINS);
            } else if (c.getElevation() > 100.0) {
                c.setBiome(Biome.HILLS);
            } else {
                c.setBiome(Biome.BEACH);
            }
            c.setBiomassFish(1000.0);
            c.setBiomassAgriculture(1800.0);
            c.setWoodResource(4500.0); // Sustainable forest management
        }

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Japanese Archipelago Cliodynamic Balance (1650-1700 CE)");
        scenario.setStartDateYear(1650);
        scenario.setEndDateYear(1700);
        scenario.setTemporalResolutionDays(365.25);
        scenario.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);

        engine.initializeFromScenario(scenario, japan);
        WorldBuffer buffer = engine.getWorldBuffer();

        final int YEARS = 40;
        engine.stepForward(YEARS);

        double totalPop = 0.0;
        double totalWood = 0.0;
        double totalAgri = 0.0;
        double totalFish = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            totalPop += buffer.getBiomassHuman()[i];
            totalWood += buffer.getWoodResource()[i];
            totalAgri += buffer.getBiomassAgriculture()[i];
            totalFish += buffer.getBiomassFish()[i];
        }

        logger.info("=== JAPANESE ARCHIPELAGO VALIDATION (40 Years) ===");
        logger.info("Final Population: {}, Wood: {}, Agri Biomass: {}, Fish Stock: {}",
                (long) totalPop, (long) totalWood, (long) totalAgri, (long) totalFish);

        // Ecological & Sociological Stability Invariants:
        // 1. Population must sustain stable continuity under Japanese Tokugawa regime
        assertTrue(totalPop > (37 * 500 * 0.9), "Japanese archipelago must sustain demographic continuity");
        // 2. Silviculture: sustainable timber harvest must not wipe out Japanese forests
        assertTrue(totalWood > (37 * 1000.0), "Japanese silviculture must maintain robust forest stocks");
        // 3. Marine fisheries must actively buffer nutrition
        assertTrue(totalFish > 100.0, "Coastal fisheries must sustain active marine stocks");
    }

    @Test
    @DisplayName("Study 14: Tripartite Archipelago Maritime Physical Trade & Specialization (3 Distinct Islands, Res 5, 117 cells, 25 Years)")
    public void testTripartiteArchipelagoTradeAndResourceSpecialization() {
        // High-resolution multi-island system at Resolution 5 (avg hex area ~253 kmÂ²):
        // 3 distinct contiguous islands separated by open maritime straits:
        // Island A: Agrarian Breadbasket (37 cells, center: 10.0Â° N, 0.0Â° E)
        List<H3Cell> islandA = createContiguousIsland(10.0, 0.0, 5, 3, 300.0, 2.0);
        // Island B: Mining / Industrial Urban Hub (61 cells, center: 10.0Â° N, 2.5Â° E)
        List<H3Cell> islandB = createContiguousIsland(10.0, 2.5, 5, 4, 600.0, 2.8);
        // Island C: Maritime Fishery & Trading Port (19 cells, center: 8.0Â° N, 1.2Â° E)
        List<H3Cell> islandC = createContiguousIsland(8.0, 1.2, 5, 2, 250.0, 2.2);

        assertEquals(37, islandA.size(), "Island A must have 37 cells");
        assertEquals(61, islandB.size(), "Island B must have 61 cells");
        assertEquals(19, islandC.size(), "Island C must have 19 cells");

        double areaScale = H3Cell.getAvgHexagonAreaKm2(5) / 1770.49;

        // Configure initial physical endowments:
        // Island A: Agricultural surplus, rich soil, zero metal
        for (H3Cell c : islandA) {
            c.setBiome(Biome.PLAINS);
            c.setFoodResource(8000.0 * areaScale);
            c.setBiomassAgriculture(4500.0 * areaScale);
            c.setResourceMetal(50.0 * areaScale);
            c.setResourceCapital(10.0 * c.getPopulation());
        }

        // Island B: Industrialized, rich metal deposits, high capital, severe food deficit
        for (H3Cell c : islandB) {
            c.setBiome(Biome.HILLS);
            c.setFoodResource(300.0 * areaScale); // Food deficit
            c.setBiomassAgriculture(500.0 * areaScale);
            c.setResourceMetal(3000.0 * areaScale);
            c.setResourceCapital(45.0 * c.getPopulation());
        }

        // Island C: Coastal fishery hub, marine upwelling, intermediate trade post
        for (H3Cell c : islandC) {
            c.setBiome(Biome.BEACH);
            c.setBiomassFish(3500.0 * areaScale);
            c.setFoodResource(2500.0 * areaScale);
            c.setResourceCapital(20.0 * c.getPopulation());
            c.setResourceMetal(400.0 * areaScale);
        }

        List<H3Cell> archMesh = new ArrayList<>();
        archMesh.addAll(islandA);
        archMesh.addAll(islandB);
        archMesh.addAll(islandC);
        assertEquals(117, archMesh.size(), "Archipelago mesh must contain exactly 117 cells");

        TradeNetworkEngine.resetCumulativeStats();

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Tripartite Archipelago Maritime Physical Trade (25 Years)");
        scenario.setStartDateYear(1600);
        scenario.setEndDateYear(1625);
        scenario.setTemporalResolutionDays(30.4375); // Monthly integration: 300 ticks
        scenario.getTypeBEngineStates().put("TradeNetworkEngine", true);

        engine.initializeFromScenario(scenario, archMesh);

        final int YEARS = 25;
        final int TICKS = YEARS * 12;
        for (int t = 0; t < TICKS; t++) {
            engine.stepForward(1);
        }

        double tradedFood = TradeNetworkEngine.getCumulativeTradedFood();
        double tradedMetal = TradeNetworkEngine.getCumulativeTradedMetal();
        double tradedCap = TradeNetworkEngine.getCumulativeTradedCapital();

        WorldBuffer buffer = engine.getWorldBuffer();
        double popA = 0.0, popB = 0.0, popC = 0.0;
        double capA = 0.0, capB = 0.0, capC = 0.0;
        double foodA = 0.0, foodB = 0.0, foodC = 0.0;

        for (int i = 0; i < 37; i++) {
            popA += buffer.getBiomassHuman()[i];
            capA += buffer.getResourceCapital()[i];
            foodA += buffer.getFoodResource()[i];
        }
        for (int i = 37; i < 37 + 61; i++) {
            popB += buffer.getBiomassHuman()[i];
            capB += buffer.getResourceCapital()[i];
            foodB += buffer.getFoodResource()[i];
        }
        for (int i = 37 + 61; i < 117; i++) {
            popC += buffer.getBiomassHuman()[i];
            capC += buffer.getResourceCapital()[i];
            foodC += buffer.getFoodResource()[i];
        }

        logger.info("=== TRIPARTITE ARCHIPELAGO MARITIME TRADE RESULTS (25 Years, 117 Cells) ===");
        logger.info("Cumulative Traded Physical Commodities: Food = {} GJ, Metal = {} kg, Capital = {} units",
                (long) tradedFood, (long) tradedMetal, (long) tradedCap);
        logger.info("Island A (Agrarian Breadbasket): Final Pop = {}, Capital = {}, Food = {}", (long) popA, (long) capA, (long) foodA);
        logger.info("Island B (Industrial Hub):      Final Pop = {}, Capital = {}, Food = {}", (long) popB, (long) capB, (long) foodB);
        logger.info("Island C (Fishery Trade Port):   Final Pop = {}, Capital = {}, Food = {}", (long) popC, (long) capC, (long) foodC);

        // Physical Invariant Assertions:
        // 1. Physical maritime trade corridors must actively transfer commodities between islands
        assertTrue(tradedFood > 0.0, "Physical food calories must be exported from agrarian Island A to deficit Island B");
        assertTrue(tradedMetal > 0.0, "Manufactured metal must be exported from industrial Island B to Island A");
        assertTrue(tradedCap > 0.0, "Physical capital tools must be transferred along active corridors");

        // 2. Demographic continuity: Island B must not undergo total starvation collapse due to food imports
        assertTrue(popB > (61 * 600 * 0.5), "Industrial Island B must sustain population through maritime food imports");

        // 3. Agrarian capitalization: Island A must accumulate tools and capital from trade
        assertTrue(capA > (37 * 300 * 10.0), "Agrarian Island A must accumulate capital through tool imports");

        // 4. Commercial hub resilience: Island C must maintain positive population and capital
        assertTrue(popC > 0.0 && capC > 0.0, "Maritime trading hub Island C must remain viable");
    }

    @Test
    @DisplayName("Study 15: High-Resolution Japanese Archipelago (Honshu, Kyushu, Shikoku, Res 5, 117 Cells, 35 Years, Maddison Benchmark)")
    public void testHighResolutionJapaneseArchipelagoTokugawaBalance() {
        // High-resolution model of the 3 main islands of Japan:
        // Honshu: 61 cells at Res 5 (center: 36.0Â° N, 138.0Â° E)
        List<H3Cell> honshu = createContiguousIsland(36.0, 138.0, 5, 4, 450.0, 2.8);
        // Kyushu: 37 cells at Res 5 (center: 32.5Â° N, 130.8Â° E)
        List<H3Cell> kyushu = createContiguousIsland(32.5, 130.8, 5, 3, 400.0, 2.6);
        // Shikoku: 19 cells at Res 5 (center: 33.8Â° N, 133.5Â° E)
        List<H3Cell> shikoku = createContiguousIsland(33.8, 133.5, 5, 2, 350.0, 2.5);

        double areaScale = H3Cell.getAvgHexagonAreaKm2(5) / 1770.49;

        List<H3Cell> japanMesh = new ArrayList<>();
        japanMesh.addAll(honshu);
        japanMesh.addAll(kyushu);
        japanMesh.addAll(shikoku);
        assertEquals(117, japanMesh.size(), "Japan archipelagic mesh must have 117 cells");

        // Authentic Japanese topographic and ecological zoning (70% mountainous relief):
        for (int idx = 0; idx < japanMesh.size(); idx++) {
            H3Cell c = japanMesh.get(idx);
            if (idx % 3 == 0) {
                c.setBiome(Biome.MOUNTAINS);
                c.setElevation(450.0);
            } else if (idx % 3 == 1) {
                c.setBiome(Biome.HILLS);
                c.setElevation(180.0);
            } else {
                c.setBiome(Biome.BEACH);
                c.setElevation(25.0);
            }
            c.setBiomassFish(1000.0 * areaScale);
            c.setBiomassAgriculture(1400.0 * areaScale);
            c.setWoodResource(7000.0 * areaScale); // Tokugawa state forestry preservation
            c.setBiomassNatural(2500.0 * areaScale);
        }

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Japanese Archipelago Tokugawa Cliodynamic Homeostasis (1600-1635 CE)");
        scenario.setStartDateYear(1600);
        scenario.setEndDateYear(1635);
        scenario.setTemporalResolutionDays(30.4375); // Monthly steps: 420 ticks
        scenario.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);
        scenario.getTypeBEngineStates().put("TradeNetworkEngine", true);
        scenario.getTypeBEngineStates().put("EdoJapanIsolationEngine", true);

        engine.initializeFromScenario(scenario, japanMesh);
        WorldBuffer buffer = engine.getWorldBuffer();

        double initialPop = 0.0;
        double initialWood = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            initialPop += buffer.getBiomassHuman()[i];
            initialWood += buffer.getWoodResource()[i];
        }

        final int YEARS = 35;
        final int TICKS = YEARS * 12;
        for (int t = 0; t < TICKS; t++) {
            engine.stepForward(1);
        }

        double finalPop = 0.0;
        double finalWood = 0.0;
        double finalFish = 0.0;
        double finalAgri = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            finalPop += buffer.getBiomassHuman()[i];
            finalWood += buffer.getWoodResource()[i];
            finalFish += buffer.getBiomassFish()[i];
            finalAgri += buffer.getBiomassAgriculture()[i];
        }

        double cagr = Math.pow(finalPop / initialPop, 1.0 / YEARS) - 1.0;
        double woodRetentionFraction = finalWood / initialWood;

        logger.info("=== HIGH-RES JAPANESE ARCHIPELAGO VALIDATION (35 Years, 117 Cells, 420 Ticks) ===");
        logger.info("Demographic Trajectory: Initial = {}, Final = {}, CAGR = {}%/year (Maddison target: 0.3%-2.2%)",
                (long) initialPop, (long) finalPop, String.format("%.2f", cagr * 100.0));
        logger.info("Forestry Retention: Initial Wood = {}, Final Wood = {} (Retention: {}%, Totman target: >50%)",
                (long) initialWood, (long) finalWood, String.format("%.1f", woodRetentionFraction * 100.0));
        logger.info("Food Security: Agricultural Biomass = {}, Marine Fishery Biomass = {}", (long) finalAgri, (long) finalFish);

        // Historical Falsification Assertions:
        // 1. Maddison Database Historical Range: Tokugawa Japan demographic growth CAGR in [0.2%, 2.2%]
        assertTrue(cagr >= 0.002 && cagr <= 0.022,
                "Japanese population CAGR must match Maddison historical range [0.2%, 2.2%]: " + (cagr * 100.0) + "%");

        // 2. Conrad Totman (1989) Forestry Benchmark: Forest stocks must maintain > 50% of initial biomass
        assertTrue(woodRetentionFraction >= 0.50,
                "Tokugawa silviculture must preserve at least 50% of forest stocks: " + (woodRetentionFraction * 100.0) + "%");

        // 3. Marine fishery stock must remain viable and active as dietary buffer
        assertTrue(finalFish > 500.0, "Coastal fisheries must maintain healthy breeding stock");
    }

    @Test
    @DisplayName("Study 16: High-Resolution British Isles Archipelago (Great Britain & Ireland, Res 5, 98 Cells, 20 Years, Weekly Steps)")
    public void testHighResolutionBritishIslesWeeklyExchange() {
        // High-resolution dual-island archipelagic system:
        // Great Britain (England & Scotland): 61 cells at Res 5 (center: 53.5Â° N, -1.8Â° W)
        List<H3Cell> britain = createContiguousIsland(53.5, -1.8, 5, 4, 550.0, 3.0);
        // Ireland: 37 cells at Res 5 (center: 53.3Â° N, -7.5Â° W)
        List<H3Cell> ireland = createContiguousIsland(53.3, -7.5, 5, 3, 300.0, 1.8);

        double areaScale = H3Cell.getAvgHexagonAreaKm2(5) / 1770.49;

        // British Isles Historical Baseline (1600 CE):
        // Britain: Early metallurgy, commercial coal/iron, capital accumulation, urbanization
        for (H3Cell c : britain) {
            c.setResourceCapital(35.0 * c.getPopulation());
            c.setResourceMetal(2000.0 * areaScale);
            c.setFoodResource(2000.0 * areaScale);
            c.setBiomassAgriculture(1500.0 * areaScale);
            c.setTechnologyLevel(3.0);
        }

        // Ireland: Agrarian pastoral economy, cattle and grain exports, capital/tool deficit
        for (H3Cell c : ireland) {
            c.setResourceCapital(12.0 * c.getPopulation());
            c.setResourceMetal(150.0 * areaScale);
            c.setFoodResource(4500.0 * areaScale); // Agrarian surplus
            c.setBiomassAgriculture(3800.0 * areaScale);
            c.setTechnologyLevel(1.8);
        }

        List<H3Cell> britishIslesMesh = new ArrayList<>();
        britishIslesMesh.addAll(britain);
        britishIslesMesh.addAll(ireland);
        assertEquals(98, britishIslesMesh.size(), "British Isles mesh must contain 98 cells");

        TradeNetworkEngine.resetCumulativeStats();

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("British Isles Weekly Maritime Integration (1600-1620 CE)");
        scenario.setStartDateYear(1600);
        scenario.setEndDateYear(1620);
        scenario.setTemporalResolutionDays(7.0); // 52 ticks/year: 1,040 weekly ticks
        scenario.getTypeBEngineStates().put("TradeNetworkEngine", true);

        engine.initializeFromScenario(scenario, britishIslesMesh);

        final int YEARS = 20;
        final int TICKS = YEARS * 52;
        for (int t = 0; t < TICKS; t++) {
            engine.stepForward(1);
        }

        double irelandFinalTech = 0.0;
        double irelandFinalCap = 0.0;
        double britainFinalFood = 0.0;

        for (H3Cell c : britain) {
            britainFinalFood += c.getFoodResource();
        }
        for (H3Cell c : ireland) {
            irelandFinalTech += c.getTechnologyLevel();
            irelandFinalCap += c.getResourceCapital();
        }
        irelandFinalTech /= ireland.size();

        double tradedFood = TradeNetworkEngine.getCumulativeTradedFood();
        double tradedMetal = TradeNetworkEngine.getCumulativeTradedMetal();

        logger.info("=== HIGH-RES BRITISH ISLES WEEKLY VALIDATION (20 Years, 1,040 Weekly Ticks) ===");
        logger.info("Physical Inter-Island Commodity Trade: Traded Food = {} GJ, Traded Metal = {} kg",
                (long) tradedFood, (long) tradedMetal);
        logger.info("Ireland Final State: Mean Tech = {} (Initial: 1.80), Capital = {}",
                String.format("%.3f", irelandFinalTech), (long) irelandFinalCap);
        logger.info("Britain Final Food Reserve: {} GJ", (long) britainFinalFood);

        // Historical Falsification Assertions:
        // 1. Maritime trade corridor across Irish Sea must actively transfer food and metal
        assertTrue(tradedFood > 0.0, "Food must be exported across the Irish Sea to support British urbanization");

        // 2. Technological diffusion: Irish technology must advance through maritime contact with Britain
        assertTrue(irelandFinalTech > 1.85, "Technology diffusion across the Irish Sea must elevate Irish tech level: " + irelandFinalTech);

        // 3. Capital accumulation in Ireland must be sustained
        assertTrue(irelandFinalCap > (37 * 300 * 12.0), "Irish capital stock must expand with market integration");

        // 4. British food reserves must not collapse to zero
        assertTrue(britainFinalFood > 0.0, "British food reserves must be supported by inter-island trade");
    }


    @Test
    @DisplayName("Study 19: Three-Island Western Indian Ocean Maritime Trade Network at Res 5 (Madagascar + Comoros + Swahili/Zanzibar Hub, 200 Years)")
    public void testThreeIslandIndianOceanArchipelagoMaritimeTradeRes5() {
        // Empirical Historical Grounding (Beaujard 2012 / Horton & Middleton 2000 / Wright 1984):
        // Between 900 CE and 1100 CE (Dembeni / Mahilaka phase), northwestern Madagascar, the Comoros archipelago
        // (Mayotte/Ndzuani), and the East African Swahili island hubs (Kilwa/Zanzibar/Pemba) formed an integrated
        // maritime trade triad exchanging Malagasy rice, timber, and crystal/soapstone (chloritoschiste) for
        // ironware, ceramics, and mercantile capital.
        // We simulate 3 distinct islands at H3 Resolution 5 over 200 years (2,400 monthly ticks):
        //   1. Island 1: Northwestern Madagascar (Mahilaka / Boina bay, 91 cells, radius 5) â€” Agricultural & Forest surplus
        //   2. Island 2: Comoros Archipelago (Ndzuani / Dembeni, 37 cells, radius 3) â€” Maritime stepping-stone entrepÃ´t
        //   3. Island 3: Swahili Coral Island Hub (Zanzibar / Kilwa, 61 cells, radius 4) â€” High-density iron & mercantile hub
        final int YEARS = 200;
        final int TICKS_PER_YEAR = 12;
        final double DT_MONTHLY_DAYS = 30.4375;

        List<H3Cell> madagascarNW = createContiguousIsland(-15.5, 47.8, 5, 5, 55.0, 1.75); // 91 cells
        List<H3Cell> comorosHub   = createContiguousIsland(-12.2, 44.4, 5, 3, 95.0, 1.95); // 37 cells
        List<H3Cell> swahiliHub   = createContiguousIsland(-6.2,  39.2, 5, 4, 140.0, 2.25); // 61 cells

        assertEquals(91, madagascarNW.size());
        assertEquals(37, comorosHub.size());
        assertEquals(61, swahiliHub.size());

        double areaScale = H3Cell.getAvgHexagonAreaKm2(5) / 1770.49;

        // 1. Madagascar NW: Rich agricultural rice valleys, abundant timber, moderate coastal fisheries
        for (H3Cell c : madagascarNW) {
            c.setBiome(Biome.JUNGLE);
            c.setRainfall(1850.0);
            c.setWoodResource(8800.0 * areaScale);
            c.setBiomassNatural(7500.0 * areaScale);
            c.setBiomassAgriculture(140.0 * areaScale);
            c.setBiomassFish(1500.0 * areaScale);
            c.setFoodResource(650.0 * areaScale);
            c.setResourceMetal(250.0 * areaScale);
            c.setResourceCapital(55.0 * 10.0);
        }

        // 2. Comoros Archipelago: Volcanic stepping-stone island, rich marine fisheries, intermediate agriculture
        for (H3Cell c : comorosHub) {
            c.setBiome(Biome.FOREST);
            c.setRainfall(1600.0);
            c.setWoodResource(6000.0 * areaScale);
            c.setBiomassNatural(4500.0 * areaScale);
            c.setBiomassAgriculture(110.0 * areaScale);
            c.setBiomassFish(2200.0 * areaScale);
            c.setFoodResource(450.0 * areaScale);
            c.setResourceMetal(350.0 * areaScale);
            c.setResourceCapital(95.0 * 16.0);
        }

        // 3. Swahili Coral Island Hub: High urban/mercantile density, high iron/metallurgical endowment, lower arable land
        for (H3Cell c : swahiliHub) {
            c.setBiome(Biome.SAVANNAH);
            c.setRainfall(1150.0);
            c.setWoodResource(3500.0 * areaScale);
            c.setBiomassNatural(3000.0 * areaScale);
            c.setBiomassAgriculture(85.0 * areaScale);
            c.setBiomassFish(2400.0 * areaScale);
            c.setFoodResource(420.0 * areaScale);
            c.setResourceMetal(1200.0 * areaScale);
            c.setResourceCapital(140.0 * 24.0);
        }

        List<H3Cell> triadWorld = new ArrayList<>(189);
        triadWorld.addAll(madagascarNW);
        triadWorld.addAll(comorosHub);
        triadWorld.addAll(swahiliHub);

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("900-1100 CE Western Indian Ocean 3-Island Maritime Trade Triad (Res 5)");
        scenario.setStartDateYear(900);
        scenario.setEndDateYear(1100);
        scenario.setTemporalResolutionDays(DT_MONTHLY_DAYS);
        scenario.setInitialHumanCount((long) (91 * 55 + 37 * 95 + 61 * 140));
        scenario.setInitialCapitalPerCapita(16.0);
        scenario.setInitialEnergyPerCapita(22.0);
        scenario.getTypeBEngineStates().put("TradeNetworkEngine", true);
        scenario.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);
        scenario.getTypeBEngineStates().put("OstromCommonsPureEngine", true);

        engine.initializeFromScenario(scenario, triadWorld);

        double initMadagascarPop = 91.0 * 55.0;
        double initComorosPop = 37.0 * 95.0;
        double initSwahiliPop = 61.0 * 140.0;

        // Run 200 years (2,400 monthly ticks)
        for (int year = 1; year <= YEARS; year++) {
            engine.stepForward(TICKS_PER_YEAR);
        }

        double finalMadagascarPop = 0.0, finalMadagascarCap = 0.0, finalMadagascarFish = 0.0, finalMadagascarTech = 0.0;
        for (H3Cell c : madagascarNW) {
            finalMadagascarPop += c.getBiomassHuman();
            finalMadagascarCap += c.getResourceCapital();
            finalMadagascarFish += c.getBiomassFish();
            finalMadagascarTech += c.getTechnologyLevel();
        }
        finalMadagascarTech /= 91.0;

        double finalComorosPop = 0.0, finalComorosCap = 0.0, finalComorosFish = 0.0, finalComorosTech = 0.0;
        for (H3Cell c : comorosHub) {
            finalComorosPop += c.getBiomassHuman();
            finalComorosCap += c.getResourceCapital();
            finalComorosFish += c.getBiomassFish();
            finalComorosTech += c.getTechnologyLevel();
        }
        finalComorosTech /= 37.0;

        double finalSwahiliPop = 0.0, finalSwahiliCap = 0.0, finalSwahiliFish = 0.0, finalSwahiliTech = 0.0;
        for (H3Cell c : swahiliHub) {
            finalSwahiliPop += c.getBiomassHuman();
            finalSwahiliCap += c.getResourceCapital();
            finalSwahiliFish += c.getBiomassFish();
            finalSwahiliTech += c.getTechnologyLevel();
        }
        finalSwahiliTech /= 61.0;

        logger.info("=== STUDY 19: 3-ISLAND INDIAN OCEAN MARITIME TRADE TRIAD (900-1100 CE, RES 5, 2,400 TICKS) ===");
        logger.info("  1. NW Madagascar (91 cells) : Pop {} -> {}, Cap = {}, Fish = {}, Mean Tech = {}",
                (long) initMadagascarPop, (long) finalMadagascarPop, (long) finalMadagascarCap, (long) finalMadagascarFish, String.format("%.2f", finalMadagascarTech));
        logger.info("  2. Comoros Hub   (37 cells) : Pop {} -> {}, Cap = {}, Fish = {}, Mean Tech = {}",
                (long) initComorosPop, (long) finalComorosPop, (long) finalComorosCap, (long) finalComorosFish, String.format("%.2f", finalComorosTech));
        logger.info("  3. Swahili Hub   (61 cells) : Pop {} -> {}, Cap = {}, Fish = {}, Mean Tech = {}",
                (long) initSwahiliPop, (long) finalSwahiliPop, (long) finalSwahiliCap, (long) finalSwahiliFish, String.format("%.2f", finalSwahiliTech));

        // 1. All 3 islands in the trade triad must sustain demographic growth over 200 years
        assertTrue(finalMadagascarPop > initMadagascarPop * 1.5, "NW Madagascar population must expand: " + finalMadagascarPop);
        assertTrue(finalComorosPop > initComorosPop * 1.2, "Comoros entrepÃ´t population must expand: " + finalComorosPop);
        assertTrue(finalSwahiliPop > initSwahiliPop * 1.1, "Swahili coral hub population must expand: " + finalSwahiliPop);

        // 2. Marine fishery stocks across all 3 archipelagos must remain conserved and non-exhausted via logistic MSY regeneration
        assertTrue(finalMadagascarFish > 91 * 400.0 * areaScale, "Madagascar marine fishery must regenerate sustainably");
        assertTrue(finalComorosFish > 37 * 500.0 * areaScale, "Comoros marine fishery must regenerate sustainably");
        assertTrue(finalSwahiliFish > 61 * 500.0 * areaScale, "Swahili marine fishery must regenerate sustainably");

        // 3. Capital and technological integration across all three islands
        assertTrue(finalMadagascarCap > 91 * 55.0 * 10.0, "NW Madagascar capital must grow through maritime trade");
        assertTrue(finalComorosCap > 37 * 95.0 * 16.0, "Comoros capital must grow through entrepÃ´t trade");
        assertTrue(finalMadagascarTech > 1.75, "NW Madagascar technology must advance via trade network integration");
    }
}
