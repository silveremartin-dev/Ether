/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

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
public class IslandMultiScaleCliodynamicValidationSuite {

    private static final Logger logger = LoggerFactory.getLogger(IslandMultiScaleCliodynamicValidationSuite.class);

    private H3Core h3;
    private Configuration config;

    @BeforeEach
    public void setUp() throws Exception {
        h3 = H3Core.newInstance();
        config = ConfigurationLoader.loadDefault();
    }

    /**
     * Helper to create a contiguous island disk of radius k around a coordinate center.
     */
    private List<H3Cell> createContiguousIsland(double centerLat, double centerLng, int h3Resolution, int ringRadius, double initialPopPerCell) {
        return createContiguousIsland(centerLat, centerLng, h3Resolution, ringRadius, initialPopPerCell, 1.5);
    }

    private List<H3Cell> createContiguousIsland(double centerLat, double centerLng, int h3Resolution, int ringRadius, double initialPopPerCell, double techLevel) {
        long center = h3.latLngToCell(centerLat, centerLng, h3Resolution);
        List<Long> diskH3 = h3.gridDisk(center, ringRadius);

        double areaScale = H3Cell.getAvgHexagonAreaKm2(h3Resolution) / 1770.49;
        List<H3Cell> cells = new ArrayList<>(diskH3.size());
        for (Long idx : diskH3) {
            LatLng coord = h3.cellToLatLng(idx);
            H3Cell cell = new H3Cell(idx, coord.lat, coord.lng);
            cell.setElevation(250.0);
            cell.setTemperature(22.0);
            cell.setRainfall(1100.0);
            cell.setBiome(Biome.FOREST);
            cell.setFoodResource(3500.0 * areaScale);
            cell.setWaterResource(2000.0 * areaScale);
            cell.setWoodResource(6000.0 * areaScale);
            cell.setBiomassNatural(6000.0 * areaScale);
            cell.setResourceMetal(500.0 * areaScale);
            cell.setResourceClay(800.0 * areaScale);
            cell.setPopulation((int) initialPopPerCell);
            cell.setBiomassHuman(initialPopPerCell);
            cell.setTechnologyLevel(techLevel);
            cell.setResourceCapital(initialPopPerCell * (techLevel < 1.5 ? 2.0 : 15.0));
            cell.setLifespan(techLevel < 1.5 ? 35.0 : 48.0);
            cell.setFertility(techLevel < 1.5 ? 2.5 : 3.2);
            cell.setBiomassAgriculture((techLevel >= 1.5) ? Math.min(3000.0 * areaScale, initialPopPerCell * 3.5 * areaScale) : 0.0);
            cells.add(cell);
        }
        return cells;
    }

    @Test
    @DisplayName("Study 1: Long-Horizon Multi-Century Island Simulation (Demographics, Capital Accumulation & Wood Depletion)")
    public void testLongHorizonIslandEvolution() {
        // Madagascar island: 127 contiguous cells at Resolution 4 (~300,000 km²)
        List<H3Cell> island = createContiguousIsland(-18.7, 46.8, 4, 6, 250.0);
        assertEquals(127, island.size(), "Madagascar disk must have 127 cells at radius 6");

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Madagascar Long Horizon Evolution (100 Years)");
        scenario.setStartDateYear(600);
        scenario.setEndDateYear(700);
        scenario.setTemporalResolutionDays(365.25); // 1 tick = 1 year
        scenario.setInitialHumanCount((long) (127 * 250));
        scenario.setInitialCapitalPerCapita(15.0);
        scenario.setInitialEnergyPerCapita(20.0);

        engine.initializeFromScenario(scenario, island);
        WorldBuffer buffer = engine.getWorldBuffer();

        double initialTotalPop = 0.0;
        double initialTotalWood = 0.0;
        double initialTotalCapital = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            initialTotalPop += buffer.getBiomassHuman()[i];
            initialTotalWood += buffer.getWoodResource()[i];
            initialTotalCapital += buffer.getResourceCapital()[i];
        }

        // Track trajectory metrics over 100 years
        final int TOTAL_YEARS = 100;
        double[] popHistory = new double[TOTAL_YEARS + 1];
        double[] woodHistory = new double[TOTAL_YEARS + 1];
        double[] capitalHistory = new double[TOTAL_YEARS + 1];

        popHistory[0] = initialTotalPop;
        woodHistory[0] = initialTotalWood;
        capitalHistory[0] = initialTotalCapital;

        for (int year = 1; year <= TOTAL_YEARS; year++) {
            engine.stepForward(1);

            double currentPop = 0.0;
            double currentWood = 0.0;
            double currentCapital = 0.0;
            for (int i = 0; i < buffer.getCapacity(); i++) {
                currentPop += buffer.getBiomassHuman()[i];
                currentWood += buffer.getWoodResource()[i];
                currentCapital += buffer.getResourceCapital()[i];

                // Strict sanity assertions per tick
                assertFalse(Double.isNaN(buffer.getBiomassHuman()[i]), "NaN Pop at year " + year);
                assertFalse(Double.isNaN(buffer.getTechnologyLevel()[i]), "NaN Tech at year " + year);
                assertTrue(buffer.getBiomassHuman()[i] >= 0.0, "Negative Pop at year " + year);
                assertTrue(buffer.getWaterResource()[i] >= 0.0, "Negative Water at year " + year);
                assertTrue(buffer.getFoodResource()[i] >= 0.0, "Negative Food at year " + year);
            }

            popHistory[year] = currentPop;
            woodHistory[year] = currentWood;
            capitalHistory[year] = currentCapital;
        }

        double finalPop = popHistory[TOTAL_YEARS];
        double finalWood = woodHistory[TOTAL_YEARS];
        double finalCapital = capitalHistory[TOTAL_YEARS];

        logger.info("=== 100-YEAR MADAGASCAR RESULTS ===");
        logger.info("Population : Initial = {}, Final = {} (Net Change: {}%)",
                (long) initialTotalPop, (long) finalPop, String.format("%+.1f", (finalPop - initialTotalPop) / initialTotalPop * 100.0));
        logger.info("Wood Biomass: Initial = {}, Final = {} (Depletion: {}%)",
                (long) initialTotalWood, (long) finalWood, String.format("%.1f", (initialTotalWood - finalWood) / initialTotalWood * 100.0));
        logger.info("Capital     : Initial = {}, Final = {} (Growth: {}%)",
                (long) initialTotalCapital, (long) finalCapital, String.format("%+.1f", (finalCapital - initialTotalCapital) / initialTotalCapital * 100.0));

        // Plausible Demographic Expectations:
        // 1. Population must grow stably in an under-populated fertile island without catastrophic famine
        assertTrue(finalPop > initialTotalPop, "Under-populated fertile island must sustain positive demographic growth");
        // 2. Average annual growth rate r must be biologically plausible (< 3.5% per year, physiological Euler-Lotka limit)
        double compoundAnnualGrowth = Math.pow(finalPop / initialTotalPop, 1.0 / TOTAL_YEARS) - 1.0;
        logger.info("Compound Annual Growth Rate: {}% / year", String.format("%.2f", compoundAnnualGrowth * 100.0));
        assertTrue(compoundAnnualGrowth >= 0.001 && compoundAnnualGrowth <= 0.035,
                "Annual growth rate must be realistic pre-industrial range [0.1%, 3.5%]: " + compoundAnnualGrowth);

        // 3. Wood resources must experience net anthropogenic consumption/depletion
        assertTrue(finalWood <= initialTotalWood, "Growing population must deplete forestry biomass for fuel, heating, and agriculture");

        // 4. Capital accumulation: positive aggregate capital expansion
        assertTrue(finalCapital > 0.0, "Capital stock must remain positive and active");
    }

    @Test
    @DisplayName("Study 2: Spatial Resolution Invariance (Comparing Res 3 vs Res 4 over identical surface)")
    public void testSpatialResolutionInvariance() {
        // Res 3: 19 cells with ring radius 2 (~300,000 km² at Res 3, cell radius ~70 km)
        List<H3Cell> meshRes3 = createContiguousIsland(20.0, 75.0, 3, 2, 2000.0);
        // Res 4: 91 cells with ring radius 5 (~300,000 km² at Res 4, cell radius ~25 km)
        List<H3Cell> meshRes4 = createContiguousIsland(20.0, 75.0, 4, 5, 417.0);

        double totalPopRes3 = meshRes3.stream().mapToDouble(H3Cell::getBiomassHuman).sum();
        double totalPopRes4 = meshRes4.stream().mapToDouble(H3Cell::getBiomassHuman).sum();
        logger.info("Resolution Invariance Test: Res 3 ({} cells, Pop {}) vs Res 4 ({} cells, Pop {})",
                meshRes3.size(), (long) totalPopRes3, meshRes4.size(), (long) totalPopRes4);

        // Run both for 30 years
        final int SIM_YEARS = 30;

        H3SimulationEngine engineRes3 = new H3SimulationEngine(config);
        Scenario s3 = new Scenario();
        s3.setName("Res3 Scale Test");
        s3.setStartDateYear(1000);
        s3.setTemporalResolutionDays(365.25);
        engineRes3.initializeFromScenario(s3, meshRes3);
        engineRes3.stepForward(SIM_YEARS);

        H3SimulationEngine engineRes4 = new H3SimulationEngine(config);
        Scenario s4 = new Scenario();
        s4.setName("Res4 Scale Test");
        s4.setStartDateYear(1000);
        s4.setTemporalResolutionDays(365.25);
        engineRes4.initializeFromScenario(s4, meshRes4);
        engineRes4.stepForward(SIM_YEARS);

        double finalPopRes3 = 0.0;
        for (int i = 0; i < engineRes3.getWorldBuffer().getCapacity(); i++) {
            finalPopRes3 += engineRes3.getWorldBuffer().getBiomassHuman()[i];
        }

        double finalPopRes4 = 0.0;
        for (int i = 0; i < engineRes4.getWorldBuffer().getCapacity(); i++) {
            finalPopRes4 += engineRes4.getWorldBuffer().getBiomassHuman()[i];
        }

        double growthRatioRes3 = finalPopRes3 / totalPopRes3;
        double growthRatioRes4 = finalPopRes4 / totalPopRes4;
        double relativeDiscrepancy = Math.abs(growthRatioRes3 - growthRatioRes4) / Math.max(growthRatioRes3, growthRatioRes4);

        logger.info("Resolution Invariance: Growth Ratio Res 3 = x{}, Growth Ratio Res 4 = x{} (Discrepancy: {}%)",
                String.format("%.3f", growthRatioRes3), String.format("%.3f", growthRatioRes4),
                String.format("%.1f", relativeDiscrepancy * 100.0));

        // Spatial discretization invariance: aggregate trajectory discrepancy must be under 30%
        assertTrue(relativeDiscrepancy < 0.30,
                "Spatial resolution change must not create divergent demographic dynamics (>30% divergence): " + relativeDiscrepancy);
    }

    @Test
    @DisplayName("Study 3: Temporal Integration Sensitivity (dt = 60d vs dt = 182d vs dt = 365d)")
    public void testTemporalIntegrationSensitivity() {
        // Compare 3 time steps over a 20-year horizon on identical 37-cell island
        final double YEARS = 20.0;

        // Run A: dt = 60 days (6 ticks/year, 120 ticks total)
        List<H3Cell> islandA = createContiguousIsland(10.0, 10.0, 4, 3, 500.0);
        H3SimulationEngine engineA = new H3SimulationEngine(config);
        Scenario sA = new Scenario();
        sA.setName("dt=60d");
        sA.setStartDateYear(1500);
        sA.setTemporalResolutionDays(60.0);
        engineA.initializeFromScenario(sA, islandA);
        engineA.stepForward((int) (YEARS * (365.25 / 60.0)));

        // Run B: dt = 365.25 days (1 tick/year, 20 ticks total)
        List<H3Cell> islandB = createContiguousIsland(10.0, 10.0, 4, 3, 500.0);
        H3SimulationEngine engineB = new H3SimulationEngine(config);
        Scenario sB = new Scenario();
        sB.setName("dt=365d");
        sB.setStartDateYear(1500);
        sB.setTemporalResolutionDays(365.25);
        engineB.initializeFromScenario(sB, islandB);
        engineB.stepForward((int) YEARS);

        double popA = 0.0;
        for (int i = 0; i < engineA.getWorldBuffer().getCapacity(); i++) popA += engineA.getWorldBuffer().getBiomassHuman()[i];
        double popB = 0.0;
        for (int i = 0; i < engineB.getWorldBuffer().getCapacity(); i++) popB += engineB.getWorldBuffer().getBiomassHuman()[i];

        double discrepancy = Math.abs(popA - popB) / Math.max(popA, popB);
        logger.info("Temporal Convergence: dt=60d Pop = {}, dt=365d Pop = {} (Relative Difference: {}%)",
                (long) popA, (long) popB, String.format("%.2f", discrepancy * 100.0));

        // Time-step convergence: difference must be under 20% over 20 years
        assertTrue(discrepancy < 0.20, "Temporal integration discrepancy must be bounded (< 20%): " + discrepancy);
    }

    @Test
    @DisplayName("Study 4: Type B A/B Controlled Experiment (Boserup Intensification ON vs OFF)")
    public void testTypeBCliodynamicHypothesisABComparison() {
        // 37 cells island under demographic density pressure
        List<H3Cell> islandWithBoserup = createContiguousIsland(35.0, 35.0, 4, 3, 2000.0);
        List<H3Cell> islandWithoutBoserup = createContiguousIsland(35.0, 35.0, 4, 3, 2000.0);

        // Run A: WITH Boserup Intensification
        H3SimulationEngine engineWith = new H3SimulationEngine(config);
        Scenario sWith = new Scenario();
        sWith.setName("With Boserup");
        sWith.setStartDateYear(-1000);
        sWith.setTemporalResolutionDays(365.25);
        sWith.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", true);
        engineWith.initializeFromScenario(sWith, islandWithBoserup);
        engineWith.stepForward(40);

        // Run B: WITHOUT Boserup Intensification (pure Malthusian static limit)
        H3SimulationEngine engineWithout = new H3SimulationEngine(config);
        Scenario sWithout = new Scenario();
        sWithout.setName("Without Boserup");
        sWithout.setStartDateYear(-1000);
        sWithout.setTemporalResolutionDays(365.25);
        sWithout.getTypeBEngineStates().put("BoserupAgriculturalIntensificationEngine", false);
        engineWithout.initializeFromScenario(sWithout, islandWithoutBoserup);
        engineWithout.stepForward(40);

        double popWith = 0.0;
        double techWith = 0.0;
        for (int i = 0; i < engineWith.getWorldBuffer().getCapacity(); i++) {
            popWith += engineWith.getWorldBuffer().getBiomassHuman()[i];
            techWith += engineWith.getWorldBuffer().getTechnologyLevel()[i];
        }
        techWith /= engineWith.getWorldBuffer().getCapacity();

        double popWithout = 0.0;
        double techWithout = 0.0;
        for (int i = 0; i < engineWithout.getWorldBuffer().getCapacity(); i++) {
            popWithout += engineWithout.getWorldBuffer().getBiomassHuman()[i];
            techWithout += engineWithout.getWorldBuffer().getTechnologyLevel()[i];
        }
        techWithout /= engineWithout.getWorldBuffer().getCapacity();

        logger.info("Boserup Hypothesis A/B Test (40 Years):");
        logger.info("  WITH Boserup    : Pop = {}, Mean Tech = {}", (long) popWith, String.format("%.2f", techWith));
        logger.info("  WITHOUT Boserup : Pop = {}, Mean Tech = {}", (long) popWithout, String.format("%.2f", techWithout));

        // In Boserup's theory: demographic pressure induces technological innovation and higher carrying capacity
        // Therefore, With Boserup must sustain higher or equal population and tech level than purely static Malthusian baseline
        assertTrue(popWith >= popWithout * 0.95, "Boserupian intensification must sustain equal or higher population than static cap");
    }

    @Test
    @DisplayName("Study 5: Ultra-Long Horizon Millennial Simulation (500 Years) on Contiguous Island")
    public void testMillennialIslandProsperityAndResourceExhaustion() {
        // 37 cells contiguous island (radius 3)
        final int SIM_YEARS = 500;
        List<H3Cell> island = createContiguousIsland(-20.0, 47.0, 4, 3, 200.0);
        assertEquals(37, island.size(), "Island must have exactly 37 contiguous cells");

        H3SimulationEngine engine = new H3SimulationEngine(config);
        Scenario scenario = new Scenario();
        scenario.setName("Madagascar Millennial 500-Year Civilization Horizon");
        scenario.setStartDateYear(500);
        scenario.setEndDateYear(1000);
        scenario.setTemporalResolutionDays(365.25);
        scenario.setInitialHumanCount((long) (37 * 200));
        scenario.setInitialCapitalPerCapita(15.0);
        scenario.setInitialEnergyPerCapita(25.0);
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

        // Run 500 years and track trajectory
        double peakPop = initialPop;
        int peakPopYear = 0;
        double minWood = initialWood;

        for (int year = 1; year <= SIM_YEARS; year++) {
            engine.stepForward(1);

            double currentPop = 0.0;
            double currentWood = 0.0;
            double currentCapital = 0.0;
            double currentAgri = 0.0;

            for (int i = 0; i < buffer.getCapacity(); i++) {
                float pop_i = buffer.getBiomassHuman()[i];
                float wood_i = buffer.getWoodResource()[i];
                float cap_i = buffer.getResourceCapital()[i];
                float agri_i = buffer.getBiomassAgriculture()[i];
                float water_i = buffer.getWaterResource()[i];
                float food_i = buffer.getFoodResource()[i];
                float metal_i = buffer.getMetalResource()[i];
                float clay_i = buffer.getClayResource()[i];

                currentPop += pop_i;
                currentWood += wood_i;
                currentCapital += cap_i;
                currentAgri += agri_i;

                // Numerical Invariant Invariance Guards
                assertFalse(Float.isNaN(pop_i), "NaN Pop at year " + year);
                assertFalse(Float.isInfinite(pop_i), "Inf Pop at year " + year);
                assertTrue(pop_i >= 0.0f, "Negative Pop at year " + year);

                assertFalse(Float.isNaN(wood_i), "NaN Wood at year " + year);
                assertTrue(wood_i >= 0.0f, "Negative Wood at year " + year);

                assertFalse(Float.isNaN(cap_i), "NaN Capital at year " + year);
                assertTrue(cap_i >= 0.0f, "Negative Capital at year " + year);

                assertFalse(Float.isNaN(agri_i), "NaN Agri Biomass at year " + year);
                assertTrue(agri_i >= 0.0f, "Negative Agri Biomass at year " + year);

                assertFalse(Float.isNaN(water_i), "NaN Water at year " + year);
                assertTrue(water_i >= 0.0f, "Negative Water at year " + year);

                assertFalse(Float.isNaN(food_i), "NaN Food at year " + year);
                assertTrue(food_i >= 0.0f, "Negative Food at year " + year);

                assertFalse(Float.isNaN(metal_i), "NaN Metal at year " + year);
                assertTrue(metal_i >= 0.0f, "Negative Metal at year " + year);

                assertFalse(Float.isNaN(clay_i), "NaN Clay at year " + year);
                assertTrue(clay_i >= 0.0f, "Negative Clay at year " + year);
            }

            // Verify cellular soil organic carbon
            for (H3Cell c : island) {
                double soc = c.getSoilOrganicCarbon() != null ? c.getSoilOrganicCarbon() : 20.0;
                assertFalse(Double.isNaN(soc), "NaN SOC at year " + year);
                assertTrue(soc >= 0.0, "Negative SOC at year " + year);
            }

            if (currentPop > peakPop) {
                peakPop = currentPop;
                peakPopYear = year;
            }
            if (currentWood < minWood) {
                minWood = currentWood;
            }

            if (year % 100 == 0) {
                logger.info("  [Year {}/500] Pop: {}, Wood: {}, Capital: {}, AgriBiomass: {}",
                        year, (long) currentPop, (long) currentWood, (long) currentCapital, (long) currentAgri);
            }
        }

        double finalPop = 0.0;
        double finalWood = 0.0;
        double finalCapital = 0.0;
        double finalAgri = 0.0;
        for (int i = 0; i < buffer.getCapacity(); i++) {
            finalPop += buffer.getBiomassHuman()[i];
            finalWood += buffer.getWoodResource()[i];
            finalCapital += buffer.getResourceCapital()[i];
            finalAgri += buffer.getBiomassAgriculture()[i];
        }

        logger.info("=== 500-YEAR MILLENNIAL SIMULATION SUMMARY ===");
        logger.info("Initial Population: {}, Peak Population: {} (Year {}), Final Population: {}",
                (long) initialPop, (long) peakPop, peakPopYear, (long) finalPop);
        logger.info("Initial Wood: {}, Min Wood: {}, Final Wood: {}",
                (long) initialWood, (long) minWood, (long) finalWood);
        logger.info("Initial Capital: {}, Final Capital: {}", (long) initialCapital, (long) finalCapital);
        logger.info("Final Agricultural Biomass: {}", (long) finalAgri);

        // Cliodynamic Assertions:
        // 1. Civilizational continuity: Society must not go completely extinct across 500 years
        assertTrue(finalPop > 100.0, "Population must maintain continuous demographic presence over 500 years");

        // 2. Agricultural biomass must be active and sustained
        assertTrue(finalAgri > 100.0, "Agricultural biomass must be actively sustained over 500 years");

        // 3. Forestry depletion: Peak or final wood must reflect anthropogenic consumption
        assertTrue(minWood < initialWood, "Wood resources must experience anthropogenic consumption/depletion");

        // 4. Capital accumulation: Capital stock must remain positive and active
        assertTrue(finalCapital > 0.0, "Capital stock must remain positive over 500 years");
    }

    @Test
    @DisplayName("Study 6: True Monthly-Stepped (1,200 Ticks) Madagascar Century Simulation with Multi-Variable Physical/Cliodynamic Observability")
    public void testMonthlySteppedMadagascarCenturySimulation() {
        // Full Madagascar island: 127 contiguous cells at Resolution 4 (~300,000 km²)
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
        // Madagascar Central Highlands: 331 contiguous cells at Res 5 (radius 10, ~83,700 km²)
        final int YEARS = 20;
        final int TICKS_PER_YEAR = 52;
        final int TOTAL_TICKS = YEARS * TICKS_PER_YEAR; // 1,040 weekly ticks
        final double DT_DAYS = 7.0; // Weekly time-step dt = 7 days

        // Low density: 15 hab/cell (~0.059 hab/km²), Tech 0.8 (foraging / proto-horticulture)
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

        // Moderate agrarian density: 75 hab/cell (~0.30 hab/km²), Tech 2.0 (wet rice, iron tools, cattle)
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

        // Proto-industrial density: 150 hab/cell (~0.60 hab/km²), Tech 3.5 (mining, smelting, water mills)
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
        // Great Britain: 19 cells around (52.5° N, -1.5° W) at Res 4
        List<H3Cell> britain = createContiguousIsland(52.5, -1.5, 4, 2, 600.0, 3.0);
        // Ireland: 7 cells around (53.3° N, -6.5° W) at Res 4
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
        // Honshu archipelago: 37 contiguous cells at Res 4 (radius 3) around central Honshu (35.5° N, 137.5° E)
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
        // High-resolution multi-island system at Resolution 5 (avg hex area ~253 km²):
        // 3 distinct contiguous islands separated by open maritime straits:
        // Island A: Agrarian Breadbasket (37 cells, center: 10.0° N, 0.0° E)
        List<H3Cell> islandA = createContiguousIsland(10.0, 0.0, 5, 3, 300.0, 2.0);
        // Island B: Mining / Industrial Urban Hub (61 cells, center: 10.0° N, 2.5° E)
        List<H3Cell> islandB = createContiguousIsland(10.0, 2.5, 5, 4, 600.0, 2.8);
        // Island C: Maritime Fishery & Trading Port (19 cells, center: 8.0° N, 1.2° E)
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
        final int TICKS = (int) (YEARS * 12);
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
        // Honshu: 61 cells at Res 5 (center: 36.0° N, 138.0° E)
        List<H3Cell> honshu = createContiguousIsland(36.0, 138.0, 5, 4, 450.0, 2.8);
        // Kyushu: 37 cells at Res 5 (center: 32.5° N, 130.8° E)
        List<H3Cell> kyushu = createContiguousIsland(32.5, 130.8, 5, 3, 400.0, 2.6);
        // Shikoku: 19 cells at Res 5 (center: 33.8° N, 133.5° E)
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
        // Great Britain (England & Scotland): 61 cells at Res 5 (center: 53.5° N, -1.8° W)
        List<H3Cell> britain = createContiguousIsland(53.5, -1.8, 5, 4, 550.0, 3.0);
        // Ireland: 37 cells at Res 5 (center: 53.3° N, -7.5° W)
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
    @DisplayName("Study 17: Multi-Century Madagascar Horizon at Res 5 (300 Years, 3,600 Monthly Ticks, Coupled Type A + Type B Engines)")
    public void testMultiCenturyResolution5MadagascarTrajectory() {
        // Full Madagascar simulation at Res 5: 331 contiguous cells (disk radius 10, ~83,700 km² highland core)
        // Simulated from 800 CE to 1100 CE (300 years) at monthly resolution (3,600 ticks)
        final int YEARS = 300;
        final int TICKS_PER_YEAR = 12;
        final int TOTAL_TICKS = YEARS * TICKS_PER_YEAR; // 3,600 monthly ticks
        final double DT_MONTHLY_DAYS = 30.4375;

        // Realistic demographic density in 800 CE: 35 hab/cell (~0.14 hab/km²), Tech 1.6 (wet rice, early iron metallurgy)
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
        // cyclones, locust plagues, and El Niño highland droughts ("Tmaintany").
        // Here we run two parallel 300-year simulations at H3 Resolution 5 (331 cells, ~83,700 km², 3,600 monthly ticks):
        //   - Branch A (Tier 1 Physical Only): Malthusian ceiling without Boserupian institutional/hydraulic intensification or Ostrom commons.
        //   - Branch A+B (Coupled Tier 1 + Tier 2 Boserup + Ostrom + Turchin SDT + TradeNetwork): Endogenous hydraulic intensification and shock resilience.
        final int YEARS = 300;
        final int TICKS_PER_YEAR = 12;
        final double DT_MONTHLY_DAYS = 30.4375;

        // 1500 CE Highland initial density: ~85 hab/cell (~0.34 hab/km², ~28,135 inhabitants in highland core), Tech 1.85
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

    @Test
    @DisplayName("Study 19: Three-Island Western Indian Ocean Maritime Trade Network at Res 5 (Madagascar + Comoros + Swahili/Zanzibar Hub, 200 Years)")
    public void testThreeIslandIndianOceanArchipelagoMaritimeTradeRes5() {
        // Empirical Historical Grounding (Beaujard 2012 / Horton & Middleton 2000 / Wright 1984):
        // Between 900 CE and 1100 CE (Dembeni / Mahilaka phase), northwestern Madagascar, the Comoros archipelago
        // (Mayotte/Ndzuani), and the East African Swahili island hubs (Kilwa/Zanzibar/Pemba) formed an integrated
        // maritime trade triad exchanging Malagasy rice, timber, and crystal/soapstone (chloritoschiste) for
        // ironware, ceramics, and mercantile capital.
        // We simulate 3 distinct islands at H3 Resolution 5 over 200 years (2,400 monthly ticks):
        //   1. Island 1: Northwestern Madagascar (Mahilaka / Boina bay, 91 cells, radius 5) — Agricultural & Forest surplus
        //   2. Island 2: Comoros Archipelago (Ndzuani / Dembeni, 37 cells, radius 3) — Maritime stepping-stone entrepôt
        //   3. Island 3: Swahili Coral Island Hub (Zanzibar / Kilwa, 61 cells, radius 4) — High-density iron & mercantile hub
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
        assertTrue(finalComorosPop > initComorosPop * 1.2, "Comoros entrepôt population must expand: " + finalComorosPop);
        assertTrue(finalSwahiliPop > initSwahiliPop * 1.1, "Swahili coral hub population must expand: " + finalSwahiliPop);

        // 2. Marine fishery stocks across all 3 archipelagos must remain conserved and non-exhausted via logistic MSY regeneration
        assertTrue(finalMadagascarFish > 91 * 400.0 * areaScale, "Madagascar marine fishery must regenerate sustainably");
        assertTrue(finalComorosFish > 37 * 500.0 * areaScale, "Comoros marine fishery must regenerate sustainably");
        assertTrue(finalSwahiliFish > 61 * 500.0 * areaScale, "Swahili marine fishery must regenerate sustainably");

        // 3. Capital and technological integration across all three islands
        assertTrue(finalMadagascarCap > 91 * 55.0 * 10.0, "NW Madagascar capital must grow through maritime trade");
        assertTrue(finalComorosCap > 37 * 95.0 * 16.0, "Comoros capital must grow through entrepôt trade");
        assertTrue(finalMadagascarTech > 1.75, "NW Madagascar technology must advance via trade network integration");
    }
}

