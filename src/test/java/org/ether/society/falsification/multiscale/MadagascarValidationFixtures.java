/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.multiscale;

import org.ether.society.config.Configuration;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fixture initializers and multi-tick cliodynamic runners for Madagascar validation studies.
 *
 * <p>Supports Study 6 (monthly 1,200-tick century run), Study 17 (multi-century 3,600-tick Res 5 trajectory),
 * and Study 18 (Type A vs Type B highland intensification under climatic shocks).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
final class MadagascarValidationFixtures {

    private MadagascarValidationFixtures() {
        // Utility class
    }

    /**
     * Telemetry result of the 100-year monthly Madagascar simulation (Study 6).
     */
    record MonthlyCenturyResult(
            double initialPop,
            double finalPop,
            double totalExpansionFactor,
            double compoundAnnualGrowth,
            double minAnnualGrowthRate,
            double maxAnnualGrowthRate,
            double initialWood,
            double finalWood,
            double finalAgri,
            double finalCapital
    ) {}

    /**
     * Executes the true monthly-stepped (1,200 ticks) Madagascar century simulation (Study 6).
     */
    static MonthlyCenturyResult runMonthlyCenturySimulation(
            List<H3Cell> island,
            Configuration config,
            Logger logger) {

        final int YEARS = 100;
        final int TICKS_PER_YEAR = 12;
        final double DT_DAYS = 30.4375;

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Madagascar Monthly 1200-Tick Century Simulation");
        scenario.setStartDateYear(600);
        scenario.setEndDateYear(700);
        scenario.setTemporalResolutionDays(DT_DAYS);
        scenario.setInitialHumanCount((long) (127 * 250));
        scenario.setInitialCapitalPerCapita(15.0);
        scenario.setInitialEnergyPerCapita(20.0);
        scenario.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);

        engine.initializeFromScenario(scenario, island);
        WorldBuffer buffer = engine.getWorldBuffer();

        double initialPop = 0.0;
        double initialWood = 0.0;
        double initialCapital = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            initialPop += buffer.getBiomassHuman()[i];
            initialWood += buffer.getWoodResource()[i];
            initialCapital += buffer.getResourceCapital()[i];
        }

        double prevYearPop = initialPop;
        double maxAnnualGrowthRate = -1.0;
        double minAnnualGrowthRate = 1.0;

        for (int year = 1; year <= YEARS; year++) {
            engine.stepForward(TICKS_PER_YEAR);

            double currentPop = 0.0;
            for (int i = 0; i < buffer.getCapacity(); i++) {
                float pop_i = buffer.getBiomassHuman()[i];
                float wood_i = buffer.getWoodResource()[i];
                float cap_i = buffer.getResourceCapital()[i];
                float agri_i = buffer.getBiomassAgriculture()[i];

                currentPop += pop_i;

                assertFalse(Float.isNaN(pop_i), "NaN Pop at year " + year);
                assertTrue(pop_i >= 0.0f, "Negative Pop at year " + year);
                assertFalse(Float.isNaN(wood_i), "NaN Wood at year " + year);
                assertTrue(wood_i >= 0.0f, "Negative Wood at year " + year);
                assertFalse(Float.isNaN(cap_i), "NaN Capital at year " + year);
                assertTrue(cap_i >= 0.0f, "Negative Capital at year " + year);
                assertFalse(Float.isNaN(agri_i), "NaN Agri at year " + year);
                assertTrue(agri_i >= 0.0f, "Negative Agri at year " + year);
            }

            for (H3Cell c : island) {
                double soc = c.getSoilOrganicCarbon() != null ? c.getSoilOrganicCarbon() : 20.0;
                assertFalse(Double.isNaN(soc), "NaN SOC at year " + year);
                assertTrue(soc >= 0.0, "Negative SOC at year " + year);
            }

            double annualGrowth = (currentPop - prevYearPop) / Math.max(1.0, prevYearPop);
            maxAnnualGrowthRate = Math.max(maxAnnualGrowthRate, annualGrowth);
            minAnnualGrowthRate = Math.min(minAnnualGrowthRate, annualGrowth);
            prevYearPop = currentPop;

            assertTrue(annualGrowth <= 0.038,
                    "Annual population growth rate exceeded biological ceiling: " + (annualGrowth * 100.0) + "%");
        }

        double finalPop = prevYearPop;
        double finalWood = 0.0;
        double finalCapital = 0.0;
        double finalAgri = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            finalWood += buffer.getWoodResource()[i];
            finalCapital += buffer.getResourceCapital()[i];
            finalAgri += buffer.getBiomassAgriculture()[i];
        }

        double compoundAnnualGrowth = Math.pow(finalPop / initialPop, 1.0 / YEARS) - 1.0;
        double totalExpansionFactor = finalPop / initialPop;

        return new MonthlyCenturyResult(
                initialPop, finalPop, totalExpansionFactor, compoundAnnualGrowth,
                minAnnualGrowthRate, maxAnnualGrowthRate, initialWood, finalWood, finalAgri, finalCapital);
    }

    /**
     * Telemetry result of the 300-year multi-century resolution 5 simulation (Study 17).
     */
    record MultiCenturyTrajectoryResult(
            double initialPop, double finalPop, double expansionRatio,
            double initialWood, double finalWood, double initialAgri, double finalAgri,
            double initialCapital, double finalCapital, double finalFood,
            double meanTechDiagnostic
    ) {}

    /**
     * Executes the multi-century (300-year, 3,600-tick) resolution 5 trajectory simulation (Study 17).
     */
    static MultiCenturyTrajectoryResult runMultiCenturyTrajectorySimulation(
            List<H3Cell> highlands,
            Configuration config,
            Logger logger) {

        final int YEARS = 300;
        final int TICKS_PER_YEAR = 12;
        final double DT_MONTHLY_DAYS = 30.4375;

        setupHeterogeneousHighlandGrid(highlands);

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Madagascar Multi-Century Res 5 Secular Trajectory (800-1100 CE)");
        scenario.setStartDateYear(800);
        scenario.setEndDateYear(1100);
        scenario.setTemporalResolutionDays(DT_MONTHLY_DAYS);
        scenario.setInitialHumanCount((long) (331 * 35));
        scenario.setInitialCapitalPerCapita(12.0);
        scenario.setInitialEnergyPerCapita(20.0);

        scenario.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);
        scenario.getTypeBEngineStates().put("TurchinGoldstoneSDTEngine", true);
        scenario.getTypeBEngineStates().put("TradeNetworkEngine", true);
        scenario.getTypeBEngineStates().put("OstromCommonsPureEngine", true);

        engine.initializeFromScenario(scenario, highlands);
        WorldBuffer buffer = engine.getWorldBuffer();

        double initialPop = 0.0;
        double initialWood = 0.0;
        double initialCapital = 0.0;
        double initialAgri = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            initialPop += buffer.getBiomassHuman()[i];
            initialWood += buffer.getWoodResource()[i];
            initialCapital += buffer.getResourceCapital()[i];
            initialAgri += buffer.getBiomassAgriculture()[i];
        }

        for (int year = 1; year <= YEARS; year++) {
            engine.stepForward(TICKS_PER_YEAR);
        }

        double finalPop = 0.0;
        double finalWood = 0.0;
        double finalAgri = 0.0;
        double finalCapital = 0.0;
        double finalFood = 0.0;
        double totalTech = 0.0;

        for (int i = 0; i < buffer.getCapacity(); i++) {
            finalPop += buffer.getBiomassHuman()[i];
            finalWood += buffer.getWoodResource()[i];
            finalAgri += buffer.getBiomassAgriculture()[i];
            finalCapital += buffer.getResourceCapital()[i];
            finalFood += buffer.getFoodResource()[i];
            totalTech += buffer.computeEmergentTechnologyDiagnostic(i);
        }

        double expansionRatio = finalPop / initialPop;
        double meanTechDiagnostic = totalTech / buffer.getCapacity();

        return new MultiCenturyTrajectoryResult(
                initialPop, finalPop, expansionRatio, initialWood, finalWood,
                initialAgri, finalAgri, initialCapital, finalCapital, finalFood,
                meanTechDiagnostic);
    }

    /**
     * Populates heterogeneous biomes and initial resources on a Madagascar Res 5 highland grid.
     */
    static void setupHeterogeneousHighlandGrid(List<H3Cell> highlands) {
        double areaScale = H3Cell.getAvgHexagonAreaKm2(5) / 1770.49;
        for (int i = 0; i < highlands.size(); i++) {
            H3Cell c = highlands.get(i);
            if (i % 4 == 0) {
                c.setBiome(Biome.JUNGLE);
                c.setElevation(800.0);
                c.setWoodResource(9000.0 * areaScale);
                c.setBiomassNatural(8500.0 * areaScale);
            } else if (i % 4 == 1) {
                c.setBiome(Biome.SAVANNAH);
                c.setElevation(1200.0);
                c.setWoodResource(4000.0 * areaScale);
                c.setBiomassNatural(4500.0 * areaScale);
            } else if (i % 4 == 2) {
                c.setBiome(Biome.FOREST);
                c.setElevation(1450.0);
                c.setWoodResource(7500.0 * areaScale);
                c.setBiomassNatural(6000.0 * areaScale);
            } else {
                c.setBiome(Biome.BEACH);
                c.setElevation(150.0);
                c.setWoodResource(2500.0 * areaScale);
                c.setBiomassFish(1200.0 * areaScale);
            }
            c.setSoilOrganicCarbon(24.0);
            c.setAccessibleAquifer(85.0);
            c.setResourceClay(600.0 * areaScale);
            c.setResourceMetal(400.0 * areaScale);
        }
    }

    /**
     * Telemetry result of Study 18 Type A vs Type B highland confrontation under climatic shocks.
     */
    record HighlandIntensificationResult(
            double finalPopA, double finalPopAB,
            double finalWoodA, double finalWoodAB,
            double finalAgriA, double finalAgriAB,
            double finalCapitalA, double finalCapitalAB,
            double finalTechA, double finalTechAB
    ) {}

    /**
     * Executes the Type A vs Type B parallel simulation with periodic climatic shocks (Study 18).
     */
    static HighlandIntensificationResult runHighlandIntensificationSimulation(
            List<H3Cell> cellsA,
            List<H3Cell> cellsAB,
            Configuration config,
            Logger logger) {

        final int YEARS = 300;
        final int TICKS_PER_YEAR = 12;
        final double DT_MONTHLY_DAYS = 30.4375;

        setupHighlandPaddyAndForestGrid(cellsA, cellsAB);

        H3SimulationEngine engineA = new H3SimulationEngine(config);
        Scenario scenarioA = new Scenario();
        scenarioA.setName("Madagascar 1500-1800 CE - Type A Only");
        scenarioA.setStartDateYear(1500);
        scenarioA.setEndDateYear(1800);
        scenarioA.setTemporalResolutionDays(DT_MONTHLY_DAYS);
        scenarioA.setInitialHumanCount(331L * 85L);
        scenarioA.setInitialCapitalPerCapita(15.0);
        scenarioA.setInitialEnergyPerCapita(22.0);
        scenarioA.getTypeBEngineStates().clear();
        engineA.initializeFromScenario(scenarioA, cellsA);
        WorldBuffer bufA = engineA.getWorldBuffer();

        H3SimulationEngine engineAB = new H3SimulationEngine(config);
        Scenario scenarioAB = new Scenario();
        scenarioAB.setName("Madagascar 1500-1800 CE - Coupled Type A+B");
        scenarioAB.setStartDateYear(1500);
        scenarioAB.setEndDateYear(1800);
        scenarioAB.setTemporalResolutionDays(DT_MONTHLY_DAYS);
        scenarioAB.setInitialHumanCount(331L * 85L);
        scenarioAB.setInitialCapitalPerCapita(15.0);
        scenarioAB.setInitialEnergyPerCapita(22.0);
        scenarioAB.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);
        scenarioAB.getTypeBEngineStates().put("OstromCommonsPureEngine", true);
        scenarioAB.getTypeBEngineStates().put("TurchinGoldstoneSDTEngine", true);
        scenarioAB.getTypeBEngineStates().put("TradeNetworkEngine", true);
        engineAB.initializeFromScenario(scenarioAB, cellsAB);
        WorldBuffer bufAB = engineAB.getWorldBuffer();

        for (int year = 1; year <= YEARS; year++) {
            int calYear = 1500 + year;
            if (calYear == 1580 || calYear == 1660 || calYear == 1740) {
                applyClimaticShock(bufA, engineA);
                applyClimaticShock(bufAB, engineAB);
            }
            engineA.stepForward(TICKS_PER_YEAR);
            engineAB.stepForward(TICKS_PER_YEAR);
        }

        double finalPopA = 0; double finalWoodA = 0; double finalAgriA = 0; double finalCapA = 0; double totalTechA = 0;
        for (int i = 0; i < bufA.getCapacity(); i++) {
            finalPopA += bufA.getBiomassHuman()[i];
            finalWoodA += bufA.getWoodResource()[i];
            finalAgriA += bufA.getBiomassAgriculture()[i];
            finalCapA += bufA.getResourceCapital()[i];
            totalTechA += bufA.computeEmergentTechnologyDiagnostic(i);
        }

        double finalPopAB = 0; double finalWoodAB = 0; double finalAgriAB = 0; double finalCapAB = 0; double totalTechAB = 0;
        for (int i = 0; i < bufAB.getCapacity(); i++) {
            finalPopAB += bufAB.getBiomassHuman()[i];
            finalWoodAB += bufAB.getWoodResource()[i];
            finalAgriAB += bufAB.getBiomassAgriculture()[i];
            finalCapAB += bufAB.getResourceCapital()[i];
            totalTechAB += bufAB.computeEmergentTechnologyDiagnostic(i);
        }

        double finalTechA = totalTechA / bufA.getCapacity();
        double finalTechAB = totalTechAB / bufAB.getCapacity();

        return new HighlandIntensificationResult(
                finalPopA, finalPopAB, finalWoodA, finalWoodAB,
                finalAgriA, finalAgriAB, finalCapA, finalCapAB,
                finalTechA, finalTechAB);
    }

    private static void applyClimaticShock(WorldBuffer buf, H3SimulationEngine engine) {
        for (int i = 0; i < buf.getCapacity(); i++) {
            buf.getFoodResource()[i] *= 0.65f;
            buf.getBiomassAgriculture()[i] *= 0.80f;
        }
        for (H3Cell c : engine.getCells()) {
            c.setFoodResource(c.getFoodResource() * 0.65);
            c.setBiomassAgriculture(c.getBiomassAgriculture() * 0.80);
        }
    }

    private static void setupHighlandPaddyAndForestGrid(List<H3Cell> cellsA, List<H3Cell> cellsAB) {
        double areaScale = H3Cell.getAvgHexagonAreaKm2(5) / 1770.49;
        for (int i = 0; i < 331; i++) {
            for (H3Cell c : List.of(cellsA.get(i), cellsAB.get(i))) {
                if (i % 4 == 0) {
                    c.setBiome(Biome.FOREST);
                    c.setElevation(1350.0);
                    c.setWoodResource(7500.0 * areaScale);
                    c.setBiomassNatural(6000.0 * areaScale);
                } else if (i % 4 == 1) {
                    c.setBiome(Biome.SAVANNAH);
                    c.setElevation(1250.0);
                    c.setWoodResource(4000.0 * areaScale);
                    c.setBiomassNatural(4500.0 * areaScale);
                } else if (i % 4 == 2) {
                    c.setBiome(Biome.PLAINS);
                    c.setElevation(1200.0);
                    c.setWoodResource(2500.0 * areaScale);
                    c.setBiomassNatural(3800.0 * areaScale);
                } else {
                    c.setBiome(Biome.JUNGLE);
                    c.setElevation(950.0);
                    c.setWoodResource(9000.0 * areaScale);
                    c.setBiomassNatural(8000.0 * areaScale);
                }
                c.setSoilOrganicCarbon(26.0);
                c.setAccessibleAquifer(90.0);
                c.setResourceClay(700.0 * areaScale);
                c.setResourceMetal(500.0 * areaScale);
            }
        }
    }

    /**
     * Telemetry result of weekly resolution simulations.
     */
    record WeeklySimulationResult(
            double initialPop, double finalPop, double initialWood,
            double finalWood, double finalAgri, double compoundAnnualGrowth, double meanTech
    ) {}

    /**
     * Executes a weekly (52 ticks/year, dt=7 days) simulation loop over an island mesh.
     */
    static WeeklySimulationResult runWeeklySimulation(
            List<H3Cell> island, int startYear, int years, long popCount,
            double capPerCap, double energyPerCap, boolean boserup,
            Configuration config, Logger logger) {

        final int TICKS_PER_YEAR = 52;
        final double DT_DAYS = 7.0;

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Madagascar Weekly Res 5");
        scenario.setStartDateYear(startYear);
        scenario.setEndDateYear(startYear + years);
        scenario.setTemporalResolutionDays(DT_DAYS);
        scenario.setInitialHumanCount(popCount);
        scenario.setInitialCapitalPerCapita(capPerCap);
        scenario.setInitialEnergyPerCapita(energyPerCap);
        if (boserup) {
            scenario.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);
        }

        engine.initializeFromScenario(scenario, island);
        WorldBuffer buffer = engine.getWorldBuffer();

        double initialPop = 0.0;
        double initialWood = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            initialPop += buffer.getBiomassHuman()[i];
            initialWood += buffer.getWoodResource()[i];
        }

        for (int year = 1; year <= years; year++) {
            engine.stepForward(TICKS_PER_YEAR);
        }

        double finalPop = 0.0;
        double finalWood = 0.0;
        double finalAgri = 0.0;
        double totalTech = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            finalPop += buffer.getBiomassHuman()[i];
            finalWood += buffer.getWoodResource()[i];
            finalAgri += buffer.getBiomassAgriculture()[i];
            totalTech += buffer.computeEmergentTechnologyDiagnostic(i);
        }

        double cagr = Math.pow(finalPop / initialPop, 1.0 / years) - 1.0;
        double meanTech = totalTech / buffer.getCapacity();

        return new WeeklySimulationResult(initialPop, finalPop, initialWood, finalWood, finalAgri, cagr, meanTech);
    }
}
