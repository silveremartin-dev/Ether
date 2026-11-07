/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.multiscale;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.ether.society.falsification.multiscale.ArchipelagoValidationFixtures.*;
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
@Tag("academic")
@Tag("scientific")
@Tag("multiscale")
public class ArchipelagoMaritimeValidationSuite extends IslandValidationBase {

    @Test
    @DisplayName("Study 10: Henrich Tasmanian Cultural Loss Hypothesis on Isolated Islet (N < 5,000)")
    public void testTasmanianCulturalRegressionOnIsolatedIslet() {
        List<H3Cell> isletA = createContiguousIsland(-15.0, 50.0, 4, 0, 250.0, 2.5);
        List<H3Cell> isletB = createContiguousIsland(-15.0, 50.0, 4, 0, 250.0, 2.5);
        assertEquals(1, isletA.size(), "Islet must have exactly 1 cell");

        final int YEARS = 40;

        H3SimulationEngine engineA = new H3SimulationEngine(config);
        Scenario scenarioA = new Scenario();
        scenarioA.setName("With Tasmanian Regression");
        scenarioA.setStartDateYear(1000);
        scenarioA.setEndDateYear(1040);
        scenarioA.setTemporalResolutionDays(365.25);
        scenarioA.getTypeBEngineStates().put("TasmanianCulturalRegressionEngine", true);
        engineA.initializeFromScenario(scenarioA, isletA);

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

        double finalTechA = engineA.getWorldBuffer().computeEmergentTechnologyDiagnostic(0);
        double finalTechB = engineB.getWorldBuffer().computeEmergentTechnologyDiagnostic(0);

        logger.info("=== TASMANIAN CULTURAL REGRESSION TEST (40 Years, N=250) ===");
        logger.info("  Initial Tech: 2.50");
        logger.info("  Condition A (WITH Tasmanian Regression)    : Final Tech = {}", String.format("%.3f", finalTechA));
        logger.info("  Condition B (WITHOUT Tasmanian Regression) : Final Tech = {}", String.format("%.3f", finalTechB));

        assertTrue(finalTechA < 2.50, "Isolated population below Henrich threshold must regress culturally (TechA: " + finalTechA + ")");
        assertTrue(finalTechA < finalTechB, "Tasmanian engine must produce lower tech than control group (TechA: " + finalTechA + ", TechB: " + finalTechB + ")");
    }

    @Test
    @DisplayName("Study 11: Multi-Trophic Biomass Dynamics & Marine Fishery Regeneration (Schaefer Logistic & Overfishing Feedback)")
    public void testMarineFisheryRegenerationAndTrophicHarvest() {
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
        for (int i = 0; i < buffer.getCapacity(); i++) {
            initialFish += buffer.getBiomassFish()[i];
        }

        for (int y = 1; y <= 15; y++) {
            engine.stepForward(1);
        }

        double midFish = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            midFish += buffer.getBiomassFish()[i];
            assertEquals(coastalMesh.get(i).getBiomassFish(), (double) buffer.getBiomassFish()[i], 0.01,
                    "Cell biomassFish must be in lockstep with WorldBuffer at year 15");
        }

        logger.info("=== MARINE FISHERY REGENERATION RESULTS ===");
        logger.info("Initial Fish Stock: {}, Fish Stock at Year 15: {} (Harvest Depletion: {}%)",
                (long) initialFish, (long) midFish, String.format("%.1f", (initialFish - midFish) / initialFish * 100.0));

        assertTrue(midFish < initialFish, "Active coastal settlement must harvest marine fish biomass");
        assertTrue(midFish > 50.0, "Fish stock must maintain a viable breeding biomass under standard harvesting");
        for (int i = 0; i < buffer.getCapacity(); i++) {
            assertTrue(buffer.getBiomassNatural()[i] > 100.0, "Natural terrestrial forage biomass must remain healthy");
        }
    }

    @Test
    @DisplayName("Study 12: Inter-Island Maritime Trade & Technology Diffusion (British Isles: Britain & Ireland)")
    public void testBritishIslesMaritimeTradeAndInterIslandExchange() {
        List<H3Cell> britain = createContiguousIsland(52.5, -1.5, 4, 2, 600.0, 3.0);
        List<H3Cell> ireland = createContiguousIsland(53.3, -6.5, 4, 1, 250.0, 1.8);

        for (H3Cell c : britain) {
            c.setResourceCapital(35.0 * c.getPopulation());
            c.setResourceMetal(800.0);
        }
        for (H3Cell c : ireland) {
            c.setResourceCapital(10.0 * c.getPopulation());
            c.setResourceMetal(200.0);
            c.setBiomassAgriculture(2500.0);
        }

        List<H3Cell> archipelagMesh = new ArrayList<>();
        archipelagMesh.addAll(britain);
        archipelagMesh.addAll(ireland);

        final int YEARS = 30;

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
            irelandTechWithTrade += bufA.computeEmergentTechnologyDiagnostic(i);
        }
        irelandTechWithTrade /= ireland.size();

        logger.info("=== BRITISH ISLES MARITIME TRADE RESULTS (30 Years) ===");
        logger.info("Ireland Final State (With Trade): Capital = {}, Mean Tech = {}",
                (long) irelandCapitalWithTrade, String.format("%.3f", irelandTechWithTrade));

        assertTrue(irelandCapitalWithTrade > 0.0, "Ireland must maintain positive capital under maritime trade");
        assertTrue(irelandTechWithTrade > 1.85, "Maritime trade with high-tech Britain must diffuse technology to Ireland");
    }

    @Test
    @DisplayName("Study 13: Japanese Archipelago (Honshu Mountainous Relief, Wet-Rice & Fishery Subsidy)")
    public void testJapaneseArchipelagoAgrarianAndFisheryBalance() {
        List<H3Cell> japan = createContiguousIsland(35.5, 137.5, 4, 3, 500.0, 2.8);
        for (H3Cell c : japan) {
            if (c.getElevation() > 300.0) {
                c.setBiome(Biome.MOUNTAINS);
            } else if (c.getElevation() > 100.0) {
                c.setBiome(Biome.HILLS);
            } else {
                c.setBiome(Biome.BEACH);
            }
            c.setBiomassFish(1000.0);
            c.setBiomassAgriculture(1800.0);
            c.setWoodResource(4500.0);
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

        assertTrue(totalPop > (37 * 500 * 0.9), "Japanese archipelago must sustain demographic continuity");
        assertTrue(totalWood > (37 * 1000.0), "Japanese silviculture must maintain robust forest stocks");
        assertTrue(totalFish > 100.0, "Coastal fisheries must sustain active marine stocks");
    }

    @Test
    @DisplayName("Study 14: Tripartite Archipelago Maritime Physical Trade & Specialization (3 Distinct Islands, Res 5, 117 cells, 25 Years)")
    public void testTripartiteArchipelagoTradeAndResourceSpecialization() {
        TripartiteTradeResult res = runTripartiteTrade(config, this);

        logger.info("=== TRIPARTITE ARCHIPELAGO MARITIME TRADE RESULTS (25 Years, 117 Cells) ===");
        logger.info("Cumulative Traded Physical Commodities: Food = {} GJ, Metal = {} kg, Capital = {} units",
                (long) res.tradedFood(), (long) res.tradedMetal(), (long) res.tradedCap());
        logger.info("Island A (Agrarian Breadbasket): Final Pop = {}, Capital = {}, Food = {}", (long) res.popA(), (long) res.capA(), (long) res.foodA());
        logger.info("Island B (Industrial Hub):      Final Pop = {}, Capital = {}, Food = {}", (long) res.popB(), (long) res.capB(), (long) res.foodB());
        logger.info("Island C (Fishery Trade Port):   Final Pop = {}, Capital = {}, Food = {}", (long) res.popC(), (long) res.capC(), (long) res.foodC());

        assertTrue(res.tradedFood() > 0.0, "Physical food calories must be exported from agrarian Island A to deficit Island B");
        assertTrue(res.tradedMetal() > 0.0, "Manufactured metal must be exported from industrial Island B to Island A");
        assertTrue(res.tradedCap() > 0.0, "Physical capital tools must be transferred along active corridors");
        assertTrue(res.popB() > (61 * 600 * 0.5), "Industrial Island B must sustain population through maritime food imports");
        assertTrue(res.capA() > (37 * 300 * 10.0), "Agrarian Island A must accumulate capital through tool imports");
        assertTrue(res.popC() > 0.0 && res.capC() > 0.0, "Maritime trading hub Island C must remain viable");
    }

    @Test
    @DisplayName("Study 15: High-Resolution Japanese Archipelago (Honshu, Kyushu, Shikoku, Res 5, 117 Cells, 35 Years, Maddison Benchmark)")
    public void testHighResolutionJapaneseArchipelagoTokugawaBalance() {
        JapanTokugawaResult res = runHighResJapaneseTokugawa(config, this);

        logger.info("=== HIGH-RES JAPANESE ARCHIPELAGO VALIDATION (35 Years, 117 Cells, 420 Ticks) ===");
        logger.info("Demographic Trajectory: Initial = {}, Final = {}, CAGR = {}%/year (Maddison target: 0.3%-2.2%)",
                (long) res.initialPop(), (long) res.finalPop(), String.format("%.2f", res.cagr() * 100.0));
        logger.info("Forestry Retention: Initial Wood = {}, Final Wood = {} (Retention: {}%, Totman target: >50%)",
                (long) res.initialWood(), (long) res.finalWood(), String.format("%.1f", res.woodRetentionFraction() * 100.0));
        logger.info("Food Security: Agricultural Biomass = {}, Marine Fishery Biomass = {}", (long) res.finalAgri(), (long) res.finalFish());

        assertTrue(res.cagr() >= 0.002 && res.cagr() <= 0.022,
                "Japanese population CAGR must match Maddison historical range [0.2%, 2.2%]: " + (res.cagr() * 100.0) + "%");
        assertTrue(res.woodRetentionFraction() >= 0.50,
                "Tokugawa silviculture must preserve at least 50% of forest stocks: " + (res.woodRetentionFraction() * 100.0) + "%");
        assertTrue(res.finalFish() > 500.0, "Coastal fisheries must maintain healthy breeding stock");
    }

    @Test
    @DisplayName("Study 16: High-Resolution British Isles Archipelago (Great Britain & Ireland, Res 5, 98 Cells, 20 Years, Weekly Steps)")
    public void testHighResolutionBritishIslesWeeklyExchange() {
        BritishIslesWeeklyResult res = runHighResBritishIslesWeekly(config, this);

        logger.info("=== HIGH-RES BRITISH ISLES WEEKLY VALIDATION (20 Years, 1,040 Weekly Ticks) ===");
        logger.info("Physical Inter-Island Commodity Trade: Traded Food = {} GJ, Traded Metal = {} kg",
                (long) res.tradedFood(), (long) res.tradedMetal());
        logger.info("Ireland Final State: Mean Tech = {} (Initial: 1.80), Capital = {}",
                String.format("%.3f", res.irelandFinalTech()), (long) res.irelandFinalCap());
        logger.info("Britain Final Food Reserve: {} GJ", (long) res.britainFinalFood());

        assertTrue(res.tradedFood() > 0.0, "Food must be exported across the Irish Sea to support British urbanization");
        assertTrue(res.irelandFinalTech() > 1.85, "Technology diffusion across the Irish Sea must elevate Irish tech level: " + res.irelandFinalTech());
        assertTrue(res.irelandFinalCap() > (37 * 300 * 12.0), "Irish capital stock must expand with market integration");
        assertTrue(res.britainFinalFood() > 0.0, "British food reserves must be supported by inter-island trade");
    }

    @Test
    @DisplayName("Study 19: Three-Island Western Indian Ocean Maritime Trade Network at Res 5 (Madagascar + Comoros + Swahili/Zanzibar Hub, 200 Years)")
    public void testThreeIslandIndianOceanArchipelagoMaritimeTradeRes5() {
        IndianOceanTriadResult res = runIndianOceanTriadTrade(config, this);

        logger.info("=== STUDY 19: 3-ISLAND INDIAN OCEAN MARITIME TRADE TRIAD (900-1100 CE, RES 5, 2,400 TICKS) ===");
        logger.info("  1. NW Madagascar (91 cells) : Pop {} -> {}, Cap = {}, Fish = {}, Mean Tech = {}",
                (long) res.initMadagascarPop(), (long) res.finalMadagascarPop(), (long) res.finalMadagascarCap(), (long) res.finalMadagascarFish(), String.format("%.2f", res.finalMadagascarTech()));
        logger.info("  2. Comoros Hub   (37 cells) : Pop {} -> {}, Cap = {}, Fish = {}, Mean Tech = {}",
                (long) res.initComorosPop(), (long) res.finalComorosPop(), (long) res.finalComorosCap(), (long) res.finalComorosFish(), String.format("%.2f", res.finalComorosTech()));
        logger.info("  3. Swahili Hub   (61 cells) : Pop {} -> {}, Cap = {}, Fish = {}, Mean Tech = {}",
                (long) res.initSwahiliPop(), (long) res.finalSwahiliPop(), (long) res.finalSwahiliCap(), (long) res.finalSwahiliFish(), String.format("%.2f", res.finalSwahiliTech()));

        assertTrue(res.finalMadagascarPop() > res.initMadagascarPop() * 1.5, "NW Madagascar population must expand: " + res.finalMadagascarPop());
        assertTrue(res.finalComorosPop() > res.initComorosPop() * 1.2, "Comoros entrepôt population must expand: " + res.finalComorosPop());
        assertTrue(res.finalSwahiliPop() > res.initSwahiliPop() * 1.1, "Swahili coral hub population must expand: " + res.finalSwahiliPop());

        assertTrue(res.finalMadagascarFish() > 91 * 400.0 * res.areaScale(), "Madagascar marine fishery must regenerate sustainably");
        assertTrue(res.finalComorosFish() > 37 * 500.0 * res.areaScale(), "Comoros marine fishery must regenerate sustainably");
        assertTrue(res.finalSwahiliFish() > 61 * 500.0 * res.areaScale(), "Swahili marine fishery must regenerate sustainably");

        assertTrue(res.finalMadagascarCap() > 91 * 55.0 * 10.0, "NW Madagascar capital must grow through maritime trade");
        assertTrue(res.finalComorosCap() > 37 * 95.0 * 16.0, "Comoros capital must grow through entrepôt trade");
        assertTrue(res.finalMadagascarTech() > 1.75, "NW Madagascar technology must advance via trade network integration");
    }
}
