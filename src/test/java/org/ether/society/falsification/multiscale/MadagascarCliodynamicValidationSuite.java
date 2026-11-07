/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.multiscale;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.model.ClimateEvent;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <h1>Madagascar Cliodynamic & Biogeographical Multi-Scale Validation Suite</h1>
 *
 * <p>Validates coupled physical and cliodynamic simulations across Madagascar's insular ecology:
 * carrying capacity limits, deforestation dynamics, wet-rice hydraulic intensification,
 * and resistance to recurrent climate shocks (cyclones, droughts, epidemics).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
@DisplayName("Madagascar Insular Cliodynamic & Ecological Validation Suite")
public class MadagascarCliodynamicValidationSuite extends IslandValidationBase {

    private static final Logger logger = LoggerFactory.getLogger(MadagascarCliodynamicValidationSuite.class);

    @Test
    @DisplayName("Study 6: True Monthly-Stepped (1,200 Ticks) Madagascar Century Simulation")
    public void testMonthlySteppedMadagascarCenturySimulation() {
        List<H3Cell> island = createContiguousIsland(-18.7, 46.8, 4, 6, 250.0);
        assertEquals(127, island.size(), "Madagascar contiguous mesh must have 127 cells at radius 6");

        var res = MadagascarValidationFixtures.runMonthlyCenturySimulation(island, config, logger);

        assertTrue(res.totalExpansionFactor() >= 1.15 && res.totalExpansionFactor() <= 25.0,
                "100-year population expansion must be in realistic historical range [1.15x, 25.0x]: " + res.totalExpansionFactor());
        assertTrue(res.compoundAnnualGrowth() >= 0.001 && res.compoundAnnualGrowth() <= 0.035,
                "Compound annual growth rate must be realistic [0.1%, 3.5%]: " + res.compoundAnnualGrowth());
        assertTrue(res.finalAgri() > 500.0, "Agricultural biomass must emerge and expand with population");
        assertTrue(res.finalWood() < res.initialWood(), "Wood resources must decline as population expands");
        assertTrue(res.finalCapital() > 0.0, "Capital stock must remain active and positive");
    }

    @Test
    @DisplayName("Study 7.1: Prehistoric Madagascar (~500 CE) at Res 5, dt=7 Days (Forager Balance)")
    public void testResolution5WeeklyPrehistoricMadagascar() {
        List<H3Cell> island = createContiguousIsland(-18.7, 46.8, 5, 10, 15.0, 0.8);
        assertEquals(331, island.size(), "Res 5 disk must have 331 contiguous cells at radius 10");

        var res = MadagascarValidationFixtures.runWeeklySimulation(
                island, 500, 20, 331 * 15L, 2.0, 15.0, false, config, logger);

        assertTrue(res.compoundAnnualGrowth() <= 0.008,
                "Pre-agricultural forager growth must be near-stationary (< 0.8%/year): " + res.compoundAnnualGrowth());
        assertTrue((res.initialWood() - res.finalWood()) / res.initialWood() < 0.10,
                "Prehistoric hunter-gatherers must not deforest virgin forests (> 90% retained)");
        assertTrue(res.finalAgri() < 1000.0, "Agriculture must not spontaneously emerge at industrial scale in 500 CE");
    }

    @Test
    @DisplayName("Study 7.2: Medieval Highland Madagascar (~1500 CE) at Res 5, dt=7 Days (Agrarian Transition)")
    public void testResolution5WeeklyMedievalMadagascar() {
        List<H3Cell> island = createContiguousIsland(-18.7, 46.8, 5, 10, 75.0, 2.0);

        var res = MadagascarValidationFixtures.runWeeklySimulation(
                island, 1500, 25, 331 * 75L, 15.0, 25.0, true, config, logger);

        assertTrue(res.compoundAnnualGrowth() >= 0.002 && res.compoundAnnualGrowth() <= 0.018,
                "Medieval agrarian growth rate must be bounded [0.2%, 1.8%/year]: " + (res.compoundAnnualGrowth() * 100.0) + "%");
        assertTrue(res.finalAgri() > 1000.0, "Wet-rice agriculture must expand significantly in 1500 CE");
        assertTrue(res.finalWood() < res.initialWood(), "Agricultural expansion must consume standing forest timber");
    }

    @Test
    @DisplayName("Study 7.3: Proto-Industrial Madagascar (~1890 CE) at Res 5, dt=7 Days (Technology Progression)")
    public void testResolution5WeeklyIndustrialMadagascarAndTechProgression() {
        List<H3Cell> island = createContiguousIsland(-18.7, 46.8, 5, 10, 150.0, 3.5);

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Madagascar Industrial Res 5 Weekly (1890 CE)");
        scenario.setStartDateYear(1890);
        scenario.setEndDateYear(1905);
        scenario.setTemporalResolutionDays(7.0);
        scenario.setInitialHumanCount((long) (331 * 150));
        scenario.setInitialCapitalPerCapita(40.0);
        scenario.setInitialEnergyPerCapita(60.0);
        scenario.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);
        scenario.getTypeBEngineStates().put("ArthurCombinatorialTechnologyEngine", true);

        engine.initializeFromScenario(scenario, island);
        WorldBuffer buffer = engine.getWorldBuffer();

        double initialPop = 0.0, initialCap = 0.0, initialTech = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            initialPop += buffer.getBiomassHuman()[i];
            initialCap += buffer.getResourceCapital()[i];
            initialTech += buffer.computeEmergentTechnologyDiagnostic(i);
        }
        initialTech /= buffer.getCapacity();

        for (int year = 1; year <= 15; year++) {
            engine.stepForward(52);
        }

        double finalPop = 0.0, finalCap = 0.0, finalTech = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            finalPop += buffer.getBiomassHuman()[i];
            finalCap += buffer.getResourceCapital()[i];
            finalTech += buffer.computeEmergentTechnologyDiagnostic(i);
        }
        finalTech /= buffer.getCapacity();

        assertTrue(finalTech >= initialTech, "Technology level must maintain or progress endogenously");
        assertTrue(finalCap > 0.0, "Capital stock must remain active and positive");
        assertTrue(finalPop > initialPop * 0.9, "Population must maintain continuity under industrial era");
    }

    @Test
    @DisplayName("Study 8: Catastrophic Climate & Demographic Shocks (Cyclones, Droughts, Epidemics)")
    public void testIslandCatastrophicClimateShocks() {
        List<H3Cell> island = createContiguousIsland(-18.7, 46.8, 4, 6, 300.0, 2.0);
        assertEquals(127, island.size(), "Madagascar must have 127 cells at Res 4");

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Madagascar Multi-Shock Catastrophe Trajectory (1200-1260 CE)");
        scenario.setStartDateYear(1200);
        scenario.setEndDateYear(1260);
        scenario.setTemporalResolutionDays(365.25);
        scenario.setInitialHumanCount((long) (127 * 300));
        scenario.setInitialCapitalPerCapita(20.0);
        scenario.setInitialEnergyPerCapita(30.0);

        scenario.getClimateEvents().add(new ClimateEvent("cyclone", "Mega-Cyclone", 1210, -18.7, 46.8, 0.0, 7.5));
        scenario.getClimateEvents().add(new ClimateEvent("drought", "Decadal Drought", 1225, -18.7, 46.8, 0.0, 8.0));
        scenario.getClimateEvents().add(new ClimateEvent("epidemic", "Plague Outbreak", 1240, -18.7, 46.8, 0.0, 6.5));

        engine.initializeFromScenario(scenario, island);
        WorldBuffer buffer = engine.getWorldBuffer();

        double cap1209 = 0, cap1210 = 0, food1224 = 0, food1225 = 0, pop1239 = 0, pop1240 = 0;

        for (int year = 1201; year <= 1260; year++) {
            engine.stepForward(1);
            double curPop = 0, curCap = 0, curFood = 0;
            for (int i = 0; i < buffer.getCapacity(); i++) {
                curPop += buffer.getBiomassHuman()[i];
                curCap += buffer.getResourceCapital()[i];
                curFood += buffer.getFoodResource()[i];
            }
            if (year == 1209) cap1209 = curCap;
            if (year == 1210) cap1210 = curCap;
            if (year == 1224) food1224 = curFood;
            if (year == 1225) food1225 = curFood;
            if (year == 1239) pop1239 = curPop;
            if (year == 1240) pop1240 = curPop;
        }

        assertTrue(cap1210 < cap1209, "Tropical cyclone must cause capital stock destruction");
        assertTrue((cap1209 - cap1210) / cap1209 >= 0.10, "Cyclone capital loss must be at least 10%");
        assertTrue(food1225 < food1224, "Mega-drought must deplete agrarian food reserves");
        assertTrue(pop1240 < pop1239, "Plague epidemic must cause net population reduction");
        assertTrue((pop1239 - pop1240) / pop1239 >= 0.05, "Plague mortality must exceed 5% in shock year");
    }

    @Test
    @DisplayName("Study 9: Type B Engine Ablation on Madagascar Res 5 (Arthur Combinatorial Innovation)")
    public void testTypeBEnginesAblationMadagascarRes5() {
        List<H3Cell> islandA = createContiguousIsland(-18.7, 46.8, 5, 6, 80.0, 2.5);
        List<H3Cell> islandB = createContiguousIsland(-18.7, 46.8, 5, 6, 80.0, 2.5);

        H3SimulationEngine engineA = new H3SimulationEngine(config);
        Scenario scenarioA = new Scenario();
        scenarioA.setName("With Arthur Engine");
        scenarioA.setStartDateYear(1700);
        scenarioA.setEndDateYear(1720);
        scenarioA.setTemporalResolutionDays(7.0);
        scenarioA.getTypeBEngineStates().put("ArthurCombinatorialTechnologyEngine", true);
        scenarioA.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);
        engineA.initializeFromScenario(scenarioA, islandA);

        H3SimulationEngine engineB = new H3SimulationEngine(config);
        Scenario scenarioB = new Scenario();
        scenarioB.setName("Without Arthur Engine");
        scenarioB.setStartDateYear(1700);
        scenarioB.setEndDateYear(1720);
        scenarioB.setTemporalResolutionDays(7.0);
        scenarioB.getTypeBEngineStates().put("ArthurCombinatorialTechnologyEngine", false);
        scenarioB.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);
        engineB.initializeFromScenario(scenarioB, islandB);

        for (int year = 1; year <= 20; year++) {
            engineA.stepForward(52);
            engineB.stepForward(52);
        }

        double meanTechA = 0.0, meanTechB = 0.0;
        WorldBuffer bufA = engineA.getWorldBuffer();
        WorldBuffer bufB = engineB.getWorldBuffer();
        for (int i = 0; i < bufA.getCapacity(); i++) {
            meanTechA += bufA.computeEmergentTechnologyDiagnostic(i);
            meanTechB += bufB.computeEmergentTechnologyDiagnostic(i);
        }
        meanTechA /= bufA.getCapacity();
        meanTechB /= bufB.getCapacity();

        assertTrue(meanTechA > meanTechB,
                "Arthur Combinatorial Engine must yield higher technological progress than static baseline");
    }

    @Test
    @DisplayName("Study 17: Multi-Century Madagascar Trajectory at Resolution 5 (800-1100 CE, Coupled Type B)")
    public void testMultiCenturyResolution5MadagascarTrajectory() {
        List<H3Cell> madagascarHighlands = createContiguousIsland(-18.7, 46.8, 5, 10, 35.0, 1.6);
        assertEquals(331, madagascarHighlands.size(), "Highlands disk at Res 5, radius 10 must have exactly 331 cells");

        var res = MadagascarValidationFixtures.runMultiCenturyTrajectorySimulation(madagascarHighlands, config, logger);

        double expansionFactor = res.expansionRatio();
        double cagr = Math.pow(expansionFactor, 1.0 / 300) - 1.0;
        double woodRetention = res.finalWood() / res.initialWood();

        assertTrue(expansionFactor >= 1.5 && expansionFactor <= 30.0,
                "300-year population expansion must remain in realistic pre-industrial range [1.5x, 30.0x]: " + expansionFactor);
        assertTrue(cagr >= 0.001 && cagr <= 0.018,
                "Compound annual growth rate over 300 years must be bounded [0.1%, 1.8%]: " + (cagr * 100.0) + "%");
        assertTrue(res.finalAgri() > res.initialAgri() * 1.5,
                "Agricultural biomass must expand endogenously via Boserupian intensification");
        assertTrue(woodRetention >= 0.40 && woodRetention < 1.0,
                "Forest resources must decline under land clearing but retain > 40% biomass: " + (woodRetention * 100.0) + "%");
        assertTrue(res.finalCapital() > res.initialCapital(),
                "Physical capital stock must expand over 300 years of agrarian development");
        assertTrue(res.meanTechDiagnostic() > 1.6, "Technology must not stagnate below initial state: " + res.meanTechDiagnostic());
    }

    @Test
    @DisplayName("Study 18: Highland Wet-Rice Intensification & Catastrophic Shocks (1500-1800 CE, Type A vs Type A+B)")
    public void testMadagascarHighlandIntensificationAndCatastrophicShocksTypeAvsTypeB() {
        List<H3Cell> cellsTypeA = createContiguousIsland(-18.9, 47.5, 5, 10, 85.0, 1.85);
        List<H3Cell> cellsTypeAB = createContiguousIsland(-18.9, 47.5, 5, 10, 85.0, 1.85);
        assertEquals(331, cellsTypeA.size());
        assertEquals(331, cellsTypeAB.size());

        var res = MadagascarValidationFixtures.runHighlandIntensificationSimulation(cellsTypeA, cellsTypeAB, config, logger);

        double initialPop = 331.0 * 85.0;
        assertTrue(res.finalPopA() > initialPop * 1.3, "Type A branch must survive catastrophic shocks and expand: " + res.finalPopA());
        assertTrue(res.finalPopAB() > initialPop * 1.5, "Coupled Type A+B branch must survive catastrophic shocks and expand: " + res.finalPopAB());
        assertTrue(res.finalTechAB() > res.finalTechA(),
                "Coupled Type A+B must achieve higher technological level via Boserupian intensification (" + res.finalTechAB() + " vs " + res.finalTechA() + ")");
        assertTrue(res.finalAgriAB() >= res.finalAgriA() * 0.98,
                "Coupled Type A+B must sustain at least as much or higher cultivated agricultural biomass (" + res.finalAgriAB() + " vs " + res.finalAgriA() + ")");
    }
}
