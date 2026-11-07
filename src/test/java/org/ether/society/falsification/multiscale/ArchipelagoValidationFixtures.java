/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.multiscale;

import org.ether.society.config.Configuration;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.ether.society.engines.tier2.theories.TradeNetworkEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixture setups and multi-island simulation runners for archipelago and maritime trade validation.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public final class ArchipelagoValidationFixtures {

    private static final Logger logger = LoggerFactory.getLogger(ArchipelagoValidationFixtures.class);

    private ArchipelagoValidationFixtures() {
        // Prevent instantiation
    }

    public record TripartiteTradeResult(
            double tradedFood, double tradedMetal, double tradedCap,
            double popA, double popB, double popC,
            double capA, double capB, double capC,
            double foodA, double foodB, double foodC
    ) {}

    public record JapanTokugawaResult(
            double initialPop, double finalPop, double cagr,
            double initialWood, double finalWood, double woodRetentionFraction,
            double finalAgri, double finalFish
    ) {}

    public record BritishIslesWeeklyResult(
            double tradedFood, double tradedMetal,
            double irelandFinalTech, double irelandFinalCap,
            double britainFinalFood
    ) {}

    public record IndianOceanTriadResult(
            double initMadagascarPop, double finalMadagascarPop, double finalMadagascarCap, double finalMadagascarFish, double finalMadagascarTech,
            double initComorosPop, double finalComorosPop, double finalComorosCap, double finalComorosFish, double finalComorosTech,
            double initSwahiliPop, double finalSwahiliPop, double finalSwahiliCap, double finalSwahiliFish, double finalSwahiliTech,
            double areaScale
    ) {}

    public static TripartiteTradeResult runTripartiteTrade(Configuration config, IslandValidationBase base) {
        List<H3Cell> islandA = base.createContiguousIsland(10.0, 0.0, 5, 3, 300.0, 2.0);
        List<H3Cell> islandB = base.createContiguousIsland(10.0, 2.5, 5, 4, 600.0, 2.8);
        List<H3Cell> islandC = base.createContiguousIsland(8.0, 1.2, 5, 2, 250.0, 2.2);

        double areaScale = H3Cell.getAvgHexagonAreaKm2(5) / 1770.49;

        for (H3Cell c : islandA) {
            c.setBiome(Biome.PLAINS);
            c.setFoodResource(8000.0 * areaScale);
            c.setBiomassAgriculture(4500.0 * areaScale);
            c.setResourceMetal(50.0 * areaScale);
            c.setResourceCapital(10.0 * c.getPopulation());
        }

        for (H3Cell c : islandB) {
            c.setBiome(Biome.HILLS);
            c.setFoodResource(300.0 * areaScale);
            c.setBiomassAgriculture(500.0 * areaScale);
            c.setResourceMetal(3000.0 * areaScale);
            c.setResourceCapital(45.0 * c.getPopulation());
        }

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

        TradeNetworkEngine.resetCumulativeStats();

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Tripartite Archipelago Maritime Physical Trade (25 Years)");
        scenario.setStartDateYear(1600);
        scenario.setEndDateYear(1625);
        scenario.setTemporalResolutionDays(30.4375);
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

        return new TripartiteTradeResult(tradedFood, tradedMetal, tradedCap, popA, popB, popC, capA, capB, capC, foodA, foodB, foodC);
    }

    public static JapanTokugawaResult runHighResJapaneseTokugawa(Configuration config, IslandValidationBase base) {
        List<H3Cell> honshu = base.createContiguousIsland(36.0, 138.0, 5, 4, 450.0, 2.8);
        List<H3Cell> kyushu = base.createContiguousIsland(32.5, 130.8, 5, 3, 400.0, 2.6);
        List<H3Cell> shikoku = base.createContiguousIsland(33.8, 133.5, 5, 2, 350.0, 2.5);

        double areaScale = H3Cell.getAvgHexagonAreaKm2(5) / 1770.49;

        List<H3Cell> japanMesh = new ArrayList<>();
        japanMesh.addAll(honshu);
        japanMesh.addAll(kyushu);
        japanMesh.addAll(shikoku);

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
            c.setWoodResource(7000.0 * areaScale);
            c.setBiomassNatural(2500.0 * areaScale);
        }

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Japanese Archipelago Tokugawa Cliodynamic Homeostasis (1600-1635 CE)");
        scenario.setStartDateYear(1600);
        scenario.setEndDateYear(1635);
        scenario.setTemporalResolutionDays(30.4375);
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
        return new JapanTokugawaResult(initialPop, finalPop, cagr, initialWood, finalWood, woodRetentionFraction, finalAgri, finalFish);
    }

    public static BritishIslesWeeklyResult runHighResBritishIslesWeekly(Configuration config, IslandValidationBase base) {
        List<H3Cell> britain = base.createContiguousIsland(53.5, -1.8, 5, 4, 550.0, 3.0);
        List<H3Cell> ireland = base.createContiguousIsland(53.3, -7.5, 5, 3, 300.0, 1.8);

        double areaScale = H3Cell.getAvgHexagonAreaKm2(5) / 1770.49;

        for (H3Cell c : britain) {
            c.setResourceCapital(35.0 * c.getPopulation());
            c.setResourceMetal(2000.0 * areaScale);
            c.setFoodResource(2000.0 * areaScale);
            c.setBiomassAgriculture(1500.0 * areaScale);
        }

        for (H3Cell c : ireland) {
            c.setResourceCapital(12.0 * c.getPopulation());
            c.setResourceMetal(150.0 * areaScale);
            c.setFoodResource(4500.0 * areaScale);
            c.setBiomassAgriculture(3800.0 * areaScale);
        }

        List<H3Cell> britishIslesMesh = new ArrayList<>();
        britishIslesMesh.addAll(britain);
        britishIslesMesh.addAll(ireland);

        TradeNetworkEngine.resetCumulativeStats();

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("British Isles Weekly Maritime Integration (1600-1620 CE)");
        scenario.setStartDateYear(1600);
        scenario.setEndDateYear(1620);
        scenario.setTemporalResolutionDays(7.0);
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
            irelandFinalTech += H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c);
            irelandFinalCap += c.getResourceCapital();
        }
        irelandFinalTech /= ireland.size();

        double tradedFood = TradeNetworkEngine.getCumulativeTradedFood();
        double tradedMetal = TradeNetworkEngine.getCumulativeTradedMetal();

        return new BritishIslesWeeklyResult(tradedFood, tradedMetal, irelandFinalTech, irelandFinalCap, britainFinalFood);
    }

    public static IndianOceanTriadResult runIndianOceanTriadTrade(Configuration config, IslandValidationBase base) {
        final int YEARS = 200;
        final int TICKS_PER_YEAR = 12;
        final double DT_MONTHLY_DAYS = 30.4375;

        List<H3Cell> madagascarNW = base.createContiguousIsland(-15.5, 47.8, 5, 5, 55.0, 1.75);
        List<H3Cell> comorosHub   = base.createContiguousIsland(-12.2, 44.4, 5, 3, 95.0, 1.95);
        List<H3Cell> swahiliHub   = base.createContiguousIsland(-6.2,  39.2, 5, 4, 140.0, 2.25);

        double areaScale = H3Cell.getAvgHexagonAreaKm2(5) / 1770.49;

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

        for (int year = 1; year <= YEARS; year++) {
            engine.stepForward(TICKS_PER_YEAR);
        }

        double finalMadagascarPop = 0.0, finalMadagascarCap = 0.0, finalMadagascarFish = 0.0, finalMadagascarTech = 0.0;
        for (H3Cell c : madagascarNW) {
            finalMadagascarPop += c.getBiomassHuman();
            finalMadagascarCap += c.getResourceCapital();
            finalMadagascarFish += c.getBiomassFish();
            finalMadagascarTech += H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c);
        }
        finalMadagascarTech /= 91.0;

        double finalComorosPop = 0.0, finalComorosCap = 0.0, finalComorosFish = 0.0, finalComorosTech = 0.0;
        for (H3Cell c : comorosHub) {
            finalComorosPop += c.getBiomassHuman();
            finalComorosCap += c.getResourceCapital();
            finalComorosFish += c.getBiomassFish();
            finalComorosTech += H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c);
        }
        finalComorosTech /= 37.0;

        double finalSwahiliPop = 0.0, finalSwahiliCap = 0.0, finalSwahiliFish = 0.0, finalSwahiliTech = 0.0;
        for (H3Cell c : swahiliHub) {
            finalSwahiliPop += c.getBiomassHuman();
            finalSwahiliCap += c.getResourceCapital();
            finalSwahiliFish += c.getBiomassFish();
            finalSwahiliTech += H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c);
        }
        finalSwahiliTech /= 61.0;

        return new IndianOceanTriadResult(
                initMadagascarPop, finalMadagascarPop, finalMadagascarCap, finalMadagascarFish, finalMadagascarTech,
                initComorosPop, finalComorosPop, finalComorosCap, finalComorosFish, finalComorosTech,
                initSwahiliPop, finalSwahiliPop, finalSwahiliCap, finalSwahiliFish, finalSwahiliTech,
                areaScale
        );
    }
}
