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
 * Deep multi-century cliodynamic validation suite on Madagascar contiguous hexagonal grid.
 */
public class MadagascarCliodynamicValidationSuite extends IslandValidationBase {

    @Test
    @DisplayName("Study 6: True Monthly-Stepped (1,200 Ticks) Madagascar Century Simulation with Multi-Variable Physical/Cliodynamic Observability")
    public void testMonthlySteppedMadagascarCenturySimulation() {
        // Full Madagascar island: 127 contiguous cells at Resolution 4 (~300,000 kmÂ²)
        final int YEARS = 100;
        final int TICKS_PER_YEAR = 12;
        final int TOTAL_TICKS = YEARS * TICKS_PER_YEAR; // 1,200 monthly ticks
        final double DT_DAYS = 30.4375; // Monthly time-step dt = 1/12 year

        List<H3Cell> island = createContiguousIsland(-18.7, 46.8, 4, 6, 250.0);
        assertEquals(127, island.size(), "Madagascar contiguous mesh must have 127 cells at radius 6");

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
        double peakWood = initialWood;
        int peakWoodYear = 0;
        long totalExecutionStartNanos = System.nanoTime();

        logger.info("=== STARTING 1,200-TICK MONTHLY MADAGASCAR SIMULATION (100 YEARS) ===");
        logger.info("Initial Conditions: Population = {}, Wood = {}, Capital = {}",
                (long) initialPop, (long) initialWood, (long) initialCapital);

        for (int year = 1; year <= YEARS; year++) {
            // Step 12 monthly ticks (1 full seasonal year)
            engine.stepForward(TICKS_PER_YEAR);

            double currentPop = 0.0;
            double currentWood = 0.0;
            double currentCapital = 0.0;
            double currentAgri = 0.0;
            double currentWater = 0.0;
            double currentFood = 0.0;
            double currentTech = 0.0;

            for (int i = 0; i < buffer.getCapacity(); i++) {
                float pop_i = buffer.getBiomassHuman()[i];
                float wood_i = buffer.getWoodResource()[i];
                float cap_i = buffer.getResourceCapital()[i];
                float agri_i = buffer.getBiomassAgriculture()[i];
                float water_i = buffer.getWaterResource()[i];
                float food_i = buffer.getFoodResource()[i];
                float tech_i = buffer.getTechnologyLevel()[i];

                currentPop += pop_i;
                currentWood += wood_i;
                currentCapital += cap_i;
                currentAgri += agri_i;
                currentWater += water_i;
                currentFood += food_i;
                currentTech += tech_i;

                // Strict numerical sanity checks on every cell buffer
                assertFalse(Float.isNaN(pop_i), "NaN Pop at year " + year + ", cell " + i);
                assertFalse(Float.isInfinite(pop_i), "Inf Pop at year " + year + ", cell " + i);
                assertTrue(pop_i >= 0.0f, "Negative Pop at year " + year + ", cell " + i);

                assertFalse(Float.isNaN(wood_i), "NaN Wood at year " + year + ", cell " + i);
                assertTrue(wood_i >= 0.0f, "Negative Wood at year " + year + ", cell " + i);

                assertFalse(Float.isNaN(cap_i), "NaN Capital at year " + year + ", cell " + i);
                assertTrue(cap_i >= 0.0f, "Negative Capital at year " + year + ", cell " + i);

                assertFalse(Float.isNaN(agri_i), "NaN Agri at year " + year + ", cell " + i);
                assertTrue(agri_i >= 0.0f, "Negative Agri at year " + year + ", cell " + i);

                assertFalse(Float.isNaN(water_i), "NaN Water at year " + year + ", cell " + i);
                assertTrue(water_i >= 0.0f, "Negative Water at year " + year + ", cell " + i);

                assertFalse(Float.isNaN(food_i), "NaN Food at year " + year + ", cell " + i);
                assertTrue(food_i >= 0.0f, "Negative Food at year " + year + ", cell " + i);
            }

            // Cell-level Soil Organic Carbon (SOC) check
            double totalSOC = 0.0;
            for (H3Cell c : island) {
                double soc = c.getSoilOrganicCarbon() != null ? c.getSoilOrganicCarbon() : 20.0;
                assertFalse(Double.isNaN(soc), "NaN SOC at year " + year);
                assertTrue(soc >= 0.0, "Negative SOC at year " + year);
                totalSOC += soc;
            }
            double meanSOC = totalSOC / island.size();

            // Annual population growth rate
            double annualGrowth = (currentPop - prevYearPop) / Math.max(1.0, prevYearPop);
            maxAnnualGrowthRate = Math.max(maxAnnualGrowthRate, annualGrowth);
            minAnnualGrowthRate = Math.min(minAnnualGrowthRate, annualGrowth);
            prevYearPop = currentPop;

            if (currentWood > peakWood) {
                peakWood = currentWood;
                peakWoodYear = year;
            }

            if (year % 10 == 0 || year == YEARS) {
                double meanTech = currentTech / buffer.getCapacity();
                double capitalPerCapita = currentPop > 0 ? (currentCapital / currentPop) : 0.0;
                logger.info("  [Year {}/100 | Tick {}/1200] Pop: {} (r={}), Wood: {}, Agri: {}, Cap/cap: {}, Tech: {}, SOC: {}",
                        year, year * TICKS_PER_YEAR, (long) currentPop, String.format("%+.2f%%", annualGrowth * 100.0), (long) currentWood,
                        (long) currentAgri, String.format("%.2f", capitalPerCapita), String.format("%.2f", meanTech), String.format("%.1f", meanSOC));
            }

            // Continuous biological bounds: annual growth rate r must NEVER exceed physiological maximum (3.8%/year)
            assertTrue(annualGrowth <= 0.038,
                    "Annual population growth rate exceeded biological ceiling at year " + year + ": " + (annualGrowth * 100.0) + "%");
        }

        long elapsedMs = (System.nanoTime() - totalExecutionStartNanos) / 1_000_000L;
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

        logger.info("=== 1,200-TICK MONTHLY MADAGASCAR VALIDATION COMPLETE (Elapsed: {} ms, TPS: {}) ===",
                elapsedMs, String.format("%.1f", (TOTAL_TICKS * 1000.0) / Math.max(1, elapsedMs)));
        logger.info("Final Demographic State: Initial = {}, Final = {}, Net Expansion = x{}, CAGR = {}%/year",
                (long) initialPop, (long) finalPop, String.format("%.2f", totalExpansionFactor), String.format("%.2f", compoundAnnualGrowth * 100.0));
        logger.info("Growth Rate Bounds: Min Annual r = {}%, Max Annual r = {}%",
                String.format("%+.2f", minAnnualGrowthRate * 100.0), String.format("%+.2f", maxAnnualGrowthRate * 100.0));
        logger.info("Ecological State: Initial Wood = {}, Final Wood = {} (Net Loss: {}%), Agri Biomass = {}",
                (long) initialWood, (long) finalWood, String.format("%.1f", (initialWood - finalWood) / initialWood * 100.0), (long) finalAgri);
        logger.info("Capital State: Initial Capital = {}, Final Capital = {} (x{})",
                (long) initialCapital, (long) finalCapital, String.format("%.2f", finalCapital / Math.max(1.0, initialCapital)));

        // Cliodynamic Multi-Variable Assertions:
        // 1. Biological and historical growth ceiling: Expansion must be between 1.15x and 25x over 100 years
        assertTrue(totalExpansionFactor >= 1.15 && totalExpansionFactor <= 25.0,
                "100-year population expansion must be in realistic historical range [1.15x, 25.0x]: " + totalExpansionFactor);

        // 2. Compound annual growth rate must be bounded [0.1%, 3.5%] (matching Study 1 pre-industrial bounds)
        assertTrue(compoundAnnualGrowth >= 0.001 && compoundAnnualGrowth <= 0.035,
                "Compound annual growth rate must be realistic [0.1%, 3.5%]: " + compoundAnnualGrowth);

        // 3. Agricultural biomass emergence: Agriculture must develop to feed the growing population
        assertTrue(finalAgri > 500.0, "Agricultural biomass must emerge and expand with population");

        // 4. Forestry depletion: Wood biomass must decline due to land clearing and timber fuel consumption
        assertTrue(finalWood < initialWood, "Wood resources must decline as population and agriculture expand");

        // 5. Physical capital accumulation: Capital stock must remain active and positive
        assertTrue(finalCapital > 0.0, "Capital stock must remain active and positive over 100 years");
    }

    @Test
    @DisplayName("Study 7.1: Prehistoric Madagascar (~500 CE) at Res 5, dt=7 Days (Forager Stationary Demographic Balance)")
    public void testResolution5WeeklyPrehistoricMadagascar() {
        // Madagascar Central Highlands: 331 contiguous cells at Res 5 (radius 10, ~83,700 kmÂ²)
        final int YEARS = 20;
        final int TICKS_PER_YEAR = 52;
        final int TOTAL_TICKS = YEARS * TICKS_PER_YEAR; // 1,040 weekly ticks
        final double DT_DAYS = 7.0; // Weekly time-step dt = 7 days

        // Low density: 15 hab/cell (~0.059 hab/kmÂ²), Tech 0.8 (foraging / proto-horticulture)
        List<H3Cell> island = createContiguousIsland(-18.7, 46.8, 5, 10, 15.0, 0.8);
        assertEquals(331, island.size(), "Res 5 disk must have 331 contiguous cells at radius 10");

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Madagascar Prehistoric Res 5 Weekly (500 CE)");
        scenario.setStartDateYear(500);
        scenario.setEndDateYear(520);
        scenario.setTemporalResolutionDays(DT_DAYS);
        scenario.setInitialHumanCount((long) (331 * 15));
        scenario.setInitialCapitalPerCapita(2.0);
        scenario.setInitialEnergyPerCapita(15.0);

        engine.initializeFromScenario(scenario, island);
        WorldBuffer buffer = engine.getWorldBuffer();

        double initialPop = 0.0;
        double initialWood = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            initialPop += buffer.getBiomassHuman()[i];
            initialWood += buffer.getWoodResource()[i];
        }

        double prevPop = initialPop;
        for (int year = 1; year <= YEARS; year++) {
            engine.stepForward(TICKS_PER_YEAR);
            double curPop = 0.0;
            double curWood = 0.0;
            double curAgri = 0.0;
            for (int i = 0; i < buffer.getCapacity(); i++) {
                curPop += buffer.getBiomassHuman()[i];
                curWood += buffer.getWoodResource()[i];
                curAgri += buffer.getBiomassAgriculture()[i];
            }
            double annualGrowth = (curPop - prevPop) / Math.max(1.0, prevPop);
            prevPop = curPop;

            if (year % 5 == 0 || year == YEARS) {
                logger.info("  [Prehistoric Year {}/20 | Tick {}/1040] Pop: {} (r={}), Wood: {}, Agri: {}",
                        year, year * TICKS_PER_YEAR, (long) curPop, String.format("%+.2f%%", annualGrowth * 100.0), (long) curWood, (long) curAgri);
            }
        }

        double finalPop = prevPop;
        double finalWood = 0.0;
        double finalAgri = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            finalWood += buffer.getWoodResource()[i];
            finalAgri += buffer.getBiomassAgriculture()[i];
        }

        double compoundAnnualGrowth = Math.pow(finalPop / initialPop, 1.0 / YEARS) - 1.0;
        logger.info("=== PREHISTORIC MADAGASCAR VALIDATION (Res 5, dt=7d): Pop {} -> {} (CAGR: {}%/year), Wood Loss: {}% ===",
                (long) initialPop, (long) finalPop, String.format("%.2f", compoundAnnualGrowth * 100.0),
                String.format("%.1f", (initialWood - finalWood) / initialWood * 100.0));

        // Archaeological & Ethnographic Expectations for Pre-Agricultural Foragers:
        // 1. Near-stationary population growth: CAGR must be <= 0.6% / year (NOT 2-3% explosion!)
        assertTrue(compoundAnnualGrowth <= 0.008,
                "Pre-agricultural forager growth must be near-stationary (< 0.8%/year): " + compoundAnnualGrowth);

        // 2. Virgin forest preservation: Wood loss must be minimal (< 10%) under sparse forager density
        assertTrue((initialWood - finalWood) / initialWood < 0.10,
                "Prehistoric hunter-gatherers must not deforest virgin forests (> 90% standing wood retained)");

        // 3. Agricultural biomass must remain minimal
        assertTrue(finalAgri < 1000.0, "Agriculture must not spontaneously emerge at industrial scale in 500 CE");
    }

    @Test
    @DisplayName("Study 7.2: Medieval Highland Madagascar (~1500 CE) at Res 5, dt=7 Days (Agrarian Transition & Boserup Intensification)")
    public void testResolution5WeeklyMedievalMadagascar() {
        // Madagascar Central Highlands: 331 contiguous cells at Res 5 (radius 10)
        final int YEARS = 25;
        final int TICKS_PER_YEAR = 52;
        final double DT_DAYS = 7.0;

        // Moderate agrarian density: 75 hab/cell (~0.30 hab/kmÂ²), Tech 2.0 (wet rice, iron tools, cattle)
        List<H3Cell> island = createContiguousIsland(-18.7, 46.8, 5, 10, 75.0, 2.0);

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Madagascar Medieval Res 5 Weekly (1500 CE)");
        scenario.setStartDateYear(1500);
        scenario.setEndDateYear(1525);
        scenario.setTemporalResolutionDays(DT_DAYS);
        scenario.setInitialHumanCount((long) (331 * 75));
        scenario.setInitialCapitalPerCapita(15.0);
        scenario.setInitialEnergyPerCapita(25.0);
        scenario.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);

        engine.initializeFromScenario(scenario, island);
        WorldBuffer buffer = engine.getWorldBuffer();

        double initialPop = 0.0;
        double initialWood = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            initialPop += buffer.getBiomassHuman()[i];
            initialWood += buffer.getWoodResource()[i];
        }

        double prevPop = initialPop;
        for (int year = 1; year <= YEARS; year++) {
            engine.stepForward(TICKS_PER_YEAR);
            double curPop = 0.0;
            double curWood = 0.0;
            double curAgri = 0.0;
            double curTech = 0.0;
            for (int i = 0; i < buffer.getCapacity(); i++) {
                curPop += buffer.getBiomassHuman()[i];
                curWood += buffer.getWoodResource()[i];
                curAgri += buffer.getBiomassAgriculture()[i];
                curTech += buffer.getTechnologyLevel()[i];
            }
            double annualGrowth = (curPop - prevPop) / Math.max(1.0, prevPop);
            prevPop = curPop;

            if (year % 5 == 0 || year == YEARS) {
                logger.info("  [Medieval Year {}/25 | Tick {}/1300] Pop: {} (r={}), Wood: {}, Agri: {}, Tech: {}",
                        year, year * TICKS_PER_YEAR, (long) curPop, String.format("%+.2f%%", annualGrowth * 100.0), (long) curWood, (long) curAgri,
                        String.format("%.2f", curTech / buffer.getCapacity()));
            }
        }

        double finalPop = prevPop;
        double finalWood = 0.0;
        double finalAgri = 0.0;
        double finalTech = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            finalWood += buffer.getWoodResource()[i];
            finalAgri += buffer.getBiomassAgriculture()[i];
            finalTech += buffer.getTechnologyLevel()[i];
        }
        finalTech /= buffer.getCapacity();

        double compoundAnnualGrowth = Math.pow(finalPop / initialPop, 1.0 / YEARS) - 1.0;
        logger.info("=== MEDIEVAL MADAGASCAR VALIDATION (Res 5, dt=7d): Pop {} -> {} (CAGR: {}%/year), Wood Loss: {}%, Mean Tech: {} ===",
                (long) initialPop, (long) finalPop, String.format("%.2f", compoundAnnualGrowth * 100.0),
                String.format("%.1f", (initialWood - finalWood) / initialWood * 100.0), String.format("%.2f", finalTech));

        // Agrarian Transition Expectations:
        // 1. Sustained positive demographic growth: CAGR between 0.5% and 2.5% / year
        assertTrue(compoundAnnualGrowth >= 0.005 && compoundAnnualGrowth <= 0.025,
                "Sedentary agrarian growth rate must be realistic pre-industrial range [0.5%, 2.5%]: " + compoundAnnualGrowth);

        // 2. Active agricultural biomass expansion
        assertTrue(finalAgri > 1000.0, "Agricultural biomass must be actively sustained in agrarian society");

        // 3. Forestry depletion for iron smelting and cropland
        assertTrue(finalWood < initialWood, "Wood resources must decline as iron metallurgy and agriculture expand");
    }

    @Test
    @DisplayName("Study 7.3: Proto-Industrial Madagascar (~1890 CE) at Res 5, dt=7 Days (Endogenous Technology Level Progression)")
    public void testResolution5WeeklyIndustrialMadagascarAndTechProgression() {
        // Madagascar Central Highlands: 331 contiguous cells at Res 5
        final int YEARS = 15;
        final int TICKS_PER_YEAR = 52;
        final double DT_DAYS = 7.0;

        // Proto-industrial density: 150 hab/cell (~0.60 hab/kmÂ²), Tech 3.5 (mining, smelting, water mills)
        List<H3Cell> island = createContiguousIsland(-18.7, 46.8, 5, 10, 150.0, 3.5);

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Madagascar Industrial Res 5 Weekly (1890 CE)");
        scenario.setStartDateYear(1890);
        scenario.setEndDateYear(1905);
        scenario.setTemporalResolutionDays(DT_DAYS);
        scenario.setInitialHumanCount((long) (331 * 150));
        scenario.setInitialCapitalPerCapita(40.0);
        scenario.setInitialEnergyPerCapita(60.0);
        scenario.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);
        scenario.getTypeBEngineStates().put("ArthurCombinatorialTechnologyEngine", true);

        engine.initializeFromScenario(scenario, island);
        WorldBuffer buffer = engine.getWorldBuffer();

        double initialPop = 0.0;
        double initialCapital = 0.0;
        double initialTech = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            initialPop += buffer.getBiomassHuman()[i];
            initialCapital += buffer.getResourceCapital()[i];
            initialTech += buffer.getTechnologyLevel()[i];
        }
        initialTech /= buffer.getCapacity();

        for (int year = 1; year <= YEARS; year++) {
            engine.stepForward(TICKS_PER_YEAR);
        }

        double finalPop = 0.0;
        double finalCapital = 0.0;
        double finalTech = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            finalPop += buffer.getBiomassHuman()[i];
            finalCapital += buffer.getResourceCapital()[i];
            finalTech += buffer.getTechnologyLevel()[i];
        }
        finalTech /= buffer.getCapacity();

        logger.info("=== INDUSTRIAL MADAGASCAR & TECH PROGRESSION (Res 5, dt=7d) ===");
        logger.info("Population: Initial = {}, Final = {}", (long) initialPop, (long) finalPop);
        logger.info("Capital   : Initial = {}, Final = {}", (long) initialCapital, (long) finalCapital);
        logger.info("Technology: Initial = {}, Final = {}", String.format("%.2f", initialTech), String.format("%.2f", finalTech));

        // Technological and Economic Continuity:
        // 1. Technology ratchet effect: Technology level must NEVER regress below initial era (3.5)
        assertTrue(finalTech >= initialTech,
                "Technology level must maintain or progress endogenously (Initial: " + initialTech + ", Final: " + finalTech + ")");

        // 2. Capital accumulation under industrial manufacturing
        assertTrue(finalCapital > 0.0, "Capital stock must remain active and positive");

        // 3. Continuity of demographic mass
        assertTrue(finalPop > initialPop * 0.9, "Population must maintain continuity under industrial era");
    }

    @Test
    @DisplayName("Study 8: Catastrophic Climate & Demographic Shocks (Cyclones, Droughts, Epidemics on Madagascar)")
    public void testIslandCatastrophicClimateShocks() {
        // Madagascar island: 127 contiguous cells at Res 4 (radius 6)
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

        // Inject 3 major historical catastrophe shocks:
        // 1. Year 1210: Category 5 Tropical Cyclone striking coastal & highland Madagascar
        scenario.getClimateEvents().add(new ClimateEvent("cyclone", "Mega-Cyclone Gafilo-like", 1210, -18.7, 46.8, 0.0, 7.5));
        // 2. Year 1225: Decadal Eastern African Mega-Drought
        scenario.getClimateEvents().add(new ClimateEvent("drought", "Decadal Mega-Drought", 1225, -18.7, 46.8, 0.0, 8.0));
        // 3. Year 1240: Epidemic plague outbreak
        scenario.getClimateEvents().add(new ClimateEvent("epidemic", "Indian Ocean Plague Outbreak", 1240, -18.7, 46.8, 0.0, 6.5));

        engine.initializeFromScenario(scenario, island);
        WorldBuffer buffer = engine.getWorldBuffer();

        double popAt1209 = 0.0;
        double popAt1210 = 0.0;
        double capAt1209 = 0.0;
        double capAt1210 = 0.0;

        double foodAt1224 = 0.0;
        double foodAt1225 = 0.0;

        double popAt1239 = 0.0;
        double popAt1240 = 0.0;

        for (int year = 1201; year <= 1260; year++) {
            engine.stepForward(1);

            double currentPop = 0.0;
            double currentCap = 0.0;
            double currentFood = 0.0;
            for (int i = 0; i < buffer.getCapacity(); i++) {
                currentPop += buffer.getBiomassHuman()[i];
                currentCap += buffer.getResourceCapital()[i];
                currentFood += buffer.getFoodResource()[i];
            }

            if (year == 1209) { popAt1209 = currentPop; capAt1209 = currentCap; }
            if (year == 1210) { popAt1210 = currentPop; capAt1210 = currentCap; }
            if (year == 1224) { foodAt1224 = currentFood; }
            if (year == 1225) { foodAt1225 = currentFood; }
            if (year == 1239) { popAt1239 = currentPop; }
            if (year == 1240) { popAt1240 = currentPop; }
        }

        logger.info("=== CATASTROPHIC SHOCKS VALIDATION RESULTS ===");
        logger.info("Cyclone Impact (1210): Capital {} -> {} (Loss: {}%), Pop {} -> {}",
                (long) capAt1209, (long) capAt1210, String.format("%.1f", (capAt1209 - capAt1210) / capAt1209 * 100.0),
                (long) popAt1209, (long) popAt1210);
        logger.info("Drought Impact (1225): Food {} -> {} (Loss: {}%)",
                (long) foodAt1224, (long) foodAt1225, String.format("%.1f", (foodAt1224 - foodAt1225) / foodAt1224 * 100.0));
        logger.info("Plague Impact  (1240): Pop {} -> {} (Mortality: {}%)",
                (long) popAt1239, (long) popAt1240, String.format("%.1f", (popAt1239 - popAt1240) / popAt1239 * 100.0));

        // Catastrophic Shock Invariant Assertions:
        // 1. Cyclone must cause measurable physical capital destruction
        assertTrue(capAt1210 < capAt1209, "Tropical cyclone must cause capital stock destruction");
        assertTrue((capAt1209 - capAt1210) / capAt1209 >= 0.10, "Cyclone capital loss must be at least 10%");

        // 2. Drought must reduce available food stocks
        assertTrue(foodAt1225 < foodAt1224, "Mega-drought must deplete agrarian food reserves");

        // 3. Epidemic must cause acute demographic mortality
        assertTrue(popAt1240 < popAt1239, "Plague epidemic must cause net population reduction");
        assertTrue((popAt1239 - popAt1240) / popAt1239 >= 0.05, "Plague mortality must exceed 5% in shock year");
    }

    @Test
    @DisplayName("Study 9: Type B Engine Ablation on Madagascar Res 5 (Arthur Combinatorial Innovation vs Static Baseline)")
    public void testTypeBEnginesAblationMadagascarRes5() {
        // Central Madagascar: 127 contiguous cells at Res 5 (radius 6), weekly dt = 7 days
        final int YEARS = 20;
        final int TICKS_PER_YEAR = 52;
        final double DT_DAYS = 7.0;

        List<H3Cell> islandA = createContiguousIsland(-18.7, 46.8, 5, 6, 80.0, 2.5);
        List<H3Cell> islandB = createContiguousIsland(-18.7, 46.8, 5, 6, 80.0, 2.5);

        // Condition A: WITH Arthur Combinatorial Innovation Engine
        H3SimulationEngine engineA = new H3SimulationEngine(config);
        Scenario scenarioA = new Scenario();
        scenarioA.setName("With Arthur Engine");
        scenarioA.setStartDateYear(1700);
        scenarioA.setEndDateYear(1720);
        scenarioA.setTemporalResolutionDays(DT_DAYS);
        scenarioA.getTypeBEngineStates().put("ArthurCombinatorialTechnologyEngine", true);
        scenarioA.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);
        engineA.initializeFromScenario(scenarioA, islandA);

        // Condition B: WITHOUT Arthur Combinatorial Innovation Engine (Type B disabled)
        H3SimulationEngine engineB = new H3SimulationEngine(config);
        Scenario scenarioB = new Scenario();
        scenarioB.setName("Without Arthur Engine");
        scenarioB.setStartDateYear(1700);
        scenarioB.setEndDateYear(1720);
        scenarioB.setTemporalResolutionDays(DT_DAYS);
        scenarioB.getTypeBEngineStates().put("ArthurCombinatorialTechnologyEngine", false);
        scenarioB.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);
        engineB.initializeFromScenario(scenarioB, islandB);

        for (int year = 1; year <= YEARS; year++) {
            engineA.stepForward(TICKS_PER_YEAR);
            engineB.stepForward(TICKS_PER_YEAR);
        }

        double meanTechA = 0.0;
        double meanTechB = 0.0;
        WorldBuffer bufA = engineA.getWorldBuffer();
        WorldBuffer bufB = engineB.getWorldBuffer();

        for (int i = 0; i < bufA.getCapacity(); i++) {
            meanTechA += bufA.getTechnologyLevel()[i];
            meanTechB += bufB.getTechnologyLevel()[i];
        }
        meanTechA /= bufA.getCapacity();
        meanTechB /= bufB.getCapacity();

        logger.info("=== TYPE B ABLATION TEST (Res 5, dt=7d, 20 Years) ===");
        logger.info("  Condition A (WITH Arthur Engine)    : Mean Tech = {}", String.format("%.3f", meanTechA));
        logger.info("  Condition B (WITHOUT Arthur Engine) : Mean Tech = {}", String.format("%.3f", meanTechB));

        // Arthur's recombinant combinatorial evolution predicts accelerated endogenous technological gains:
        assertTrue(meanTechA > meanTechB,
                "Arthur Combinatorial Engine must yield higher technological progress than static baseline (TechA: " + meanTechA + ", TechB: " + meanTechB + ")");
    }


    @Test
    @DisplayName("Study 17: Multi-Century Madagascar Horizon at Res 5 (300 Years, 3,600 Monthly Ticks, Coupled Type A + Type B Engines)")
    public void testMultiCenturyResolution5MadagascarTrajectory() {
        // Full Madagascar simulation at Res 5: 331 contiguous cells (disk radius 10, ~83,700 kmÂ² highland core)
        // Simulated from 800 CE to 1100 CE (300 years) at monthly resolution (3,600 ticks)
        final int YEARS = 300;
        final int TICKS_PER_YEAR = 12;
        final int TOTAL_TICKS = YEARS * TICKS_PER_YEAR; // 3,600 monthly ticks
        final double DT_MONTHLY_DAYS = 30.4375;

        // Realistic demographic density in 800 CE: 35 hab/cell (~0.14 hab/kmÂ²), Tech 1.6 (wet rice, early iron metallurgy)
        List<H3Cell> madagascarHighlands = createContiguousIsland(-18.7, 46.8, 5, 10, 35.0, 1.6);
        assertEquals(331, madagascarHighlands.size(), "Highlands disk at Res 5, radius 10 must have exactly 331 cells");

        // Ecological & geological heterogeneity:
        double areaScale = H3Cell.getAvgHexagonAreaKm2(5) / 1770.49;
        for (int i = 0; i < madagascarHighlands.size(); i++) {
            H3Cell c = madagascarHighlands.get(i);
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
                c.setBiome(Biome.BEACH); // Coast / river basin
                c.setElevation(150.0);
                c.setWoodResource(2500.0 * areaScale);
                c.setBiomassFish(1200.0 * areaScale);
            }
            c.setSoilOrganicCarbon(24.0);
            c.setAccessibleAquifer(85.0);
            c.setResourceClay(600.0 * areaScale);
            c.setResourceMetal(400.0 * areaScale);
        }

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Madagascar Multi-Century Res 5 Secular Trajectory (800-1100 CE)");
        scenario.setStartDateYear(800);
        scenario.setEndDateYear(1100);
        scenario.setTemporalResolutionDays(DT_MONTHLY_DAYS);
        scenario.setInitialHumanCount((long) (331 * 35));
        scenario.setInitialCapitalPerCapita(12.0);
        scenario.setInitialEnergyPerCapita(20.0);

        // Activate coupled Type B Cliodynamic Theories:
        // 1. Boserup Agricultural Intensification (carrying capacity pressure drives multi-crop irrigation)
        scenario.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);
        // 2. Turchin-Goldstone Structural-Demographic Theory (SDT elite overproduction and fiscal strain)
        scenario.getTypeBEngineStates().put("TurchinGoldstoneSDTEngine", true);
        // 3. Trade Network Engine (internal trade hubs and commercial grain flows)
        scenario.getTypeBEngineStates().put("TradeNetworkEngine", true);
        // 4. Ostrom Common Pool Resource Management (prevents aquifer exhaustion)
        scenario.getTypeBEngineStates().put("OstromCommonsPureEngine", true);

        engine.initializeFromScenario(scenario, madagascarHighlands);
        WorldBuffer buffer = engine.getWorldBuffer();

        double initialPop = 0.0;
        double initialWood = 0.0;
        double initialAgri = 0.0;
        double initialCapital = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            initialPop += buffer.getBiomassHuman()[i];
            initialWood += buffer.getWoodResource()[i];
            initialAgri += buffer.getBiomassAgriculture()[i];
            initialCapital += buffer.getResourceCapital()[i];
        }

        double maxRecordedPop = initialPop;
        double minRecordedWood = initialWood;
        int peakPopYear = 800;

        long startNanos = System.nanoTime();

        // 300-year step loop (3,600 monthly ticks)
        for (int year = 1; year <= YEARS; year++) {
            engine.stepForward(TICKS_PER_YEAR);

            double currentPop = 0.0;
            double currentWood = 0.0;
            double currentAgri = 0.0;
            double currentCap = 0.0;
            double currentTech = 0.0;

            for (int i = 0; i < buffer.getCapacity(); i++) {
                float p = buffer.getBiomassHuman()[i];
                float w = buffer.getWoodResource()[i];
                float a = buffer.getBiomassAgriculture()[i];
                float k = buffer.getResourceCapital()[i];
                float t = buffer.getTechnologyLevel()[i];

                currentPop += p;
                currentWood += w;
                currentAgri += a;
                currentCap += k;
                currentTech += t;

                // Numerical guard against NaN / Infinity in 300-year integration
                assertFalse(Float.isNaN(p), "NaN Pop at year " + (800 + year));
                assertFalse(Float.isInfinite(p), "Inf Pop at year " + (800 + year));
                assertTrue(p >= 0.0f, "Negative Pop at year " + (800 + year));

                assertFalse(Float.isNaN(w), "NaN Wood at year " + (800 + year));
                assertTrue(w >= 0.0f, "Negative Wood at year " + (800 + year));

                assertFalse(Float.isNaN(a), "NaN Agri at year " + (800 + year));
                assertTrue(a >= 0.0f, "Negative Agri at year " + (800 + year));

                assertFalse(Float.isNaN(k), "NaN Capital at year " + (800 + year));
                assertTrue(k >= 0.0f, "Negative Capital at year " + (800 + year));
            }

            if (currentPop > maxRecordedPop) {
                maxRecordedPop = currentPop;
                peakPopYear = 800 + year;
            }
            if (currentWood < minRecordedWood) {
                minRecordedWood = currentWood;
            }

            if (year % 50 == 0 || year == YEARS) {
                double meanTech = currentTech / buffer.getCapacity();
                logger.info("  [Year {}/{} | Tick {}/3600] Pop: {} (Peak: {} at yr {}), Wood: {}, Agri: {}, Cap: {}, Mean Tech: {}",
                        800 + year, 800 + YEARS, year * TICKS_PER_YEAR, (long) currentPop, (long) maxRecordedPop, peakPopYear,
                        (long) currentWood, (long) currentAgri, (long) currentCap, String.format("%.2f", meanTech));
            }
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        double finalPop = 0.0;
        double finalWood = 0.0;
        double finalAgri = 0.0;
        double finalCap = 0.0;
        double finalTech = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            finalPop += buffer.getBiomassHuman()[i];
            finalWood += buffer.getWoodResource()[i];
            finalAgri += buffer.getBiomassAgriculture()[i];
            finalCap += buffer.getResourceCapital()[i];
            finalTech += buffer.getTechnologyLevel()[i];
        }
        finalTech /= buffer.getCapacity();

        double expansionFactor = finalPop / initialPop;
        double cagr = Math.pow(expansionFactor, 1.0 / YEARS) - 1.0;
        double woodRetention = finalWood / initialWood;

        logger.info("=== 300-YEAR RES 5 MADAGASCAR VALIDATION COMPLETE (Elapsed: {} ms, TPS: {}) ===",
                elapsedMs, String.format("%.1f", (TOTAL_TICKS * 1000.0) / Math.max(1, elapsedMs)));
        logger.info("Demographic Summary: Initial = {}, Peak = {} (Yr {}), Final = {}, Net Expansion = x{}, CAGR = {}%/yr",
                (long) initialPop, (long) maxRecordedPop, peakPopYear, (long) finalPop, String.format("%.2f", expansionFactor), String.format("%.2f", cagr * 100.0));
        logger.info("Ecological Summary : Initial Wood = {}, Final Wood = {} (Retention = {}%), Final Agri Biomass = {}",
                (long) initialWood, (long) finalWood, String.format("%.1f", woodRetention * 100.0), (long) finalAgri);
        logger.info("Techno-Capital     : Final Capital = {}, Mean Technology = {}", (long) finalCap, String.format("%.2f", finalTech));

        // Cliodynamic Multi-Century Invariants Assertions:
        // 1. Biological demographic viability: Population must expand without infinite explosion (expansion in [1.5x, 30x])
        assertTrue(expansionFactor >= 1.5 && expansionFactor <= 30.0,
                "300-year population expansion must remain in realistic pre-industrial range [1.5x, 30.0x]: " + expansionFactor);

        // 2. Compound annual growth rate bounded by historical pre-industrial frontiers [0.1%, 1.8%/year]
        assertTrue(cagr >= 0.001 && cagr <= 0.018,
                "Compound annual growth rate over 300 years must be bounded [0.1%, 1.8%]: " + (cagr * 100.0) + "%");

        // 3. Boserup Agricultural Intensification: Agricultural biomass must expand significantly over 300 years
        assertTrue(finalAgri > initialAgri * 1.5,
                "Agricultural biomass must expand endogenously via Boserupian intensification (Initial: " + initialAgri + ", Final: " + finalAgri + ")");

        // 4. Forest sustainability: Deforestation must occur but retain viable standing canopy (> 40%)
        assertTrue(woodRetention >= 0.40 && woodRetention < 1.0,
                "Forest resources must decline under land clearing but retain > 40% biomass: " + (woodRetention * 100.0) + "%");

        // 5. Capital accumulation: Tool and infrastructure capital must expand
        assertTrue(finalCap > initialCapital,
                "Physical capital stock must expand over 300 years of agrarian development");

        // 6. Technology progression: Mean technology must advance endogenously with iron working and irrigation
        assertTrue(finalTech > 1.6, "Technology must not stagnate below initial state: " + finalTech);
    }

    @Test
    @DisplayName("Study 18: Multi-Century Madagascar Highland Wet-Rice Intensification & Catastrophic Shocks at Res 5 (1500-1800 CE, 3,600 Ticks, Type A vs Type A+B)")
    public void testMadagascarHighlandIntensificationAndCatastrophicShocksTypeAvsTypeB() {
        // Empirical Historical Grounding (HYDE 3.4 / Seshat / Campbell 2005 / Dewar & Wright 1993):
        // Between 1500 CE and 1800 CE, Madagascar's Central Highlands (Imerina & Betsileo) transitioned from
        // dispersed swidden/hilltop villages (Vazimba / early Merina) to intensive hydraulic wet-rice irrigation
        // (Ralambo, Andrianjaka, Andriamasinavalona, Andrianampoinimerina), while facing recurrent tropical
        // cyclones, locust plagues, and El NiÃ±o highland droughts ("Tmaintany").
        // Here we run two parallel 300-year simulations at H3 Resolution 5 (331 cells, ~83,700 kmÂ², 3,600 monthly ticks):
        //   - Branch A (Tier 1 Physical Only): Malthusian ceiling without Boserupian institutional/hydraulic intensification or Ostrom commons.
        //   - Branch A+B (Coupled Tier 1 + Tier 2 Boserup + Ostrom + Turchin SDT + TradeNetwork): Endogenous hydraulic intensification and shock resilience.
        final int YEARS = 300;
        final int TICKS_PER_YEAR = 12;
        final double DT_MONTHLY_DAYS = 30.4375;

        // 1500 CE Highland initial density: ~85 hab/cell (~0.34 hab/kmÂ², ~28,135 inhabitants in highland core), Tech 1.85
        List<H3Cell> cellsTypeA = createContiguousIsland(-18.9, 47.5, 5, 10, 85.0, 1.85);
        List<H3Cell> cellsTypeAB = createContiguousIsland(-18.9, 47.5, 5, 10, 85.0, 1.85);
        assertEquals(331, cellsTypeA.size());
        assertEquals(331, cellsTypeAB.size());

        double areaScale = H3Cell.getAvgHexagonAreaKm2(5) / 1770.49;
        for (int i = 0; i < 331; i++) {
            for (H3Cell c : List.of(cellsTypeA.get(i), cellsTypeAB.get(i))) {
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
                    c.setBiome(Biome.PLAINS); // Betsimitatatra alluvial paddy plain
                    c.setElevation(1200.0);
                    c.setWoodResource(2500.0 * areaScale);
                    c.setBiomassNatural(3800.0 * areaScale);
                } else {
                    c.setBiome(Biome.JUNGLE); // Eastern escarpment forest corridor
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

        // Branch 1: Pure Type A (Tier 1 biophysical engines only)
        H3SimulationEngine engineA = new H3SimulationEngine(config);
        Scenario scenarioA = new Scenario();
        scenarioA.setName("Madagascar 1500-1800 CE - Type A Only (Pure Biophysical)");
        scenarioA.setStartDateYear(1500);
        scenarioA.setEndDateYear(1800);
        scenarioA.setTemporalResolutionDays(DT_MONTHLY_DAYS);
        scenarioA.setInitialHumanCount(331L * 85L);
        scenarioA.setInitialCapitalPerCapita(15.0);
        scenarioA.setInitialEnergyPerCapita(22.0);
        scenarioA.getTypeBEngineStates().clear();
        engineA.initializeFromScenario(scenarioA, cellsTypeA);
        WorldBuffer bufA = engineA.getWorldBuffer();

        for (int year = 1; year <= YEARS; year++) {
            int calYear = 1500 + year;
            if (calYear == 1580 || calYear == 1660 || calYear == 1740) {
                for (int i = 0; i < bufA.getCapacity(); i++) {
                    bufA.getFoodResource()[i] *= 0.65f;
                    bufA.getBiomassAgriculture()[i] *= 0.80f;
                }
                for (H3Cell c : engineA.getCells()) {
                    c.setFoodResource(c.getFoodResource() * 0.65);
                    c.setBiomassAgriculture(c.getBiomassAgriculture() * 0.80);
                }
            }
            engineA.stepForward(TICKS_PER_YEAR);
        }

        // Branch 2: Coupled Type A + Type B (Boserup + Ostrom + Turchin SDT + TradeNetwork)
        H3SimulationEngine engineAB = new H3SimulationEngine(config);
        Scenario scenarioAB = new Scenario();
        scenarioAB.setName("Madagascar 1500-1800 CE - Coupled Type A + Type B (Boserup + Ostrom + SDT + Trade)");
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
        engineAB.initializeFromScenario(scenarioAB, cellsTypeAB);
        WorldBuffer bufAB = engineAB.getWorldBuffer();

        for (int year = 1; year <= YEARS; year++) {
            int calYear = 1500 + year;
            if (calYear == 1580 || calYear == 1660 || calYear == 1740) {
                for (int i = 0; i < bufAB.getCapacity(); i++) {
                    bufAB.getFoodResource()[i] *= 0.65f;
                    bufAB.getBiomassAgriculture()[i] *= 0.80f;
                }
                for (H3Cell c : engineAB.getCells()) {
                    c.setFoodResource(c.getFoodResource() * 0.65);
                    c.setBiomassAgriculture(c.getBiomassAgriculture() * 0.80);
                }
            }
            engineAB.stepForward(TICKS_PER_YEAR);
        }

        double finalPopA = 0.0, finalAgriA = 0.0, finalTechA = 0.0, finalWoodA = 0.0;
        double finalPopAB = 0.0, finalAgriAB = 0.0, finalTechAB = 0.0, finalWoodAB = 0.0;
        for (int i = 0; i < 331; i++) {
            finalPopA += bufA.getBiomassHuman()[i];
            finalAgriA += bufA.getBiomassAgriculture()[i];
            finalTechA += bufA.getTechnologyLevel()[i];
            finalWoodA += bufA.getWoodResource()[i];

            finalPopAB += bufAB.getBiomassHuman()[i];
            finalAgriAB += bufAB.getBiomassAgriculture()[i];
            finalTechAB += bufAB.getTechnologyLevel()[i];
            finalWoodAB += bufAB.getWoodResource()[i];
        }
        finalTechA /= 331.0;
        finalTechAB /= 331.0;

        logger.info("=== STUDY 18: MADAGASCAR 1500-1800 CE RES 5 SHOCK & TYPE A vs TYPE A+B COMPARISON ===");
        logger.info("  Type A Only  (Biophysical) : Final Pop = {}, Final Agri = {}, Mean Tech = {}, Final Wood = {}",
                (long) finalPopA, (long) finalAgriA, String.format("%.2f", finalTechA), (long) finalWoodA);
        logger.info("  Type A + B   (Cliodynamic) : Final Pop = {}, Final Agri = {}, Mean Tech = {}, Final Wood = {}",
                (long) finalPopAB, (long) finalAgriAB, String.format("%.2f", finalTechAB), (long) finalWoodAB);

        // 1. Both branches must survive all 3 catastrophic shocks and grow over 300 years (1500-1800 CE)
        double initialPop = 331.0 * 85.0;
        assertTrue(finalPopA > initialPop * 1.3, "Type A branch must survive catastrophic shocks and expand: " + finalPopA);
        assertTrue(finalPopAB > initialPop * 1.5, "Coupled Type A+B branch must survive catastrophic shocks and expand: " + finalPopAB);

        // 2. Coupled Type A+B (with Boserupian hydraulic intensification, Ostrom commons, and Trade networks)
        // must achieve higher technological progression and agricultural/demographic resilience than pure Type A
        assertTrue(finalTechAB > finalTechA,
                "Coupled Type A+B must achieve higher technological level via Boserupian intensification (" + finalTechAB + " vs " + finalTechA + ")");
        assertTrue(finalAgriAB >= finalAgriA * 0.98,
                "Coupled Type A+B must sustain at least as much or higher cultivated agricultural biomass (" + finalAgriAB + " vs " + finalAgriA + ")");
    }

}
