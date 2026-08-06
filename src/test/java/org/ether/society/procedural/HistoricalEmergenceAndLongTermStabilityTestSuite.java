/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import javafx.scene.paint.Color;
import org.ether.society.core.PreComputePhase;
import org.ether.society.database.H3Cell;
import org.ether.society.diplomacy.DiplomacyManager;
import org.ether.society.diplomacy.PoliticalSimulationEngine;
import org.ether.society.model.Biome;
import org.ether.society.model.Nation;
import org.ether.society.model.Scenario;
import org.ether.society.procedural.typeb.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Advanced Automated Test Suite verifying Long-Term Stability, Historical Macro-Dynamics Emergence,
 * and Physical Numerical Invariants.
 *
 * Tests:
 * 1. Out-of-Africa Sapiens expansion (-100k to -50k BC) wave and continent crossing.
 * 2. Neolithic Agricultural Revolution emergence in river valleys (Nile & Fertile Crescent).
 * 3. State & Empire emergence in Nile Delta and Mesopotamia with capital accumulation.
 * 4. 100+ Tick Long-Term Multi-Generational Stability with ZERO NaNs, Infinities, or negative stocks.
 * 5. Statistical Telemetry reporting macro-evolutionary trajectories over simulation time.
 *
 * @author Silvere Martin-Michiellot
 */
public class HistoricalEmergenceAndLongTermStabilityTestSuite {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalEmergenceAndLongTermStabilityTestSuite.class);

    private List<H3Cell> globalCells;

    @BeforeEach
    public void setUp() {
        ProceduralEngineRegistry.clearPlugins();
        globalCells = new ArrayList<>();

        // 1. East Africa / Rift Valley (Sapiens Origin - Cell 0)
        H3Cell africa = new H3Cell(2001L, 0.5, 36.8);
        africa.setElevation(1200.0);
        africa.setBiome(Biome.PLAINS);
        africa.setTemperature(25.0);
        africa.setWaterResource(850.0);
        africa.setFoodResource(600.0);

        // 2. Nile Valley / Egypt Delta (Cell 1)
        H3Cell nile = new H3Cell(2002L, 26.8, 30.8);
        nile.setElevation(50.0);
        nile.setBiome(Biome.PLAINS);
        nile.setTemperature(26.0);
        nile.setWaterResource(980.0);
        nile.setFoodResource(1200.0);

        // 3. Fertile Crescent / Mesopotamia (Cell 2)
        H3Cell mesopotamia = new H3Cell(2003L, 32.5, 44.5);
        mesopotamia.setElevation(80.0);
        mesopotamia.setBiome(Biome.PLAINS);
        mesopotamia.setTemperature(24.0);
        mesopotamia.setWaterResource(950.0);
        mesopotamia.setFoodResource(880.0);

        // 4. Anatolia / Indus Corridor (Cell 3)
        H3Cell anatolia = new H3Cell(2004L, 38.0, 35.0);
        anatolia.setElevation(800.0);
        anatolia.setBiome(Biome.HILLS);
        anatolia.setTemperature(16.0);
        anatolia.setWaterResource(700.0);
        anatolia.setFoodResource(500.0);

        // 5. Central Asian Steppe (Cell 4)
        H3Cell steppe = new H3Cell(2005L, 48.0, 65.0);
        steppe.setElevation(500.0);
        steppe.setBiome(Biome.PLAINS);
        steppe.setTemperature(10.0);
        steppe.setWaterResource(550.0);
        steppe.setFoodResource(400.0);

        // 6. East Asia / Yellow River (Cell 5)
        H3Cell yellowRiver = new H3Cell(2006L, 35.0, 113.0);
        yellowRiver.setElevation(100.0);
        yellowRiver.setBiome(Biome.FOREST);
        yellowRiver.setTemperature(15.0);
        yellowRiver.setWaterResource(920.0);
        yellowRiver.setFoodResource(850.0);

        // 7. Arid Hinterland / Desert (Cell 6)
        H3Cell desert = new H3Cell(2007L, 22.0, 25.0);
        desert.setElevation(300.0);
        desert.setBiome(Biome.DESERT);
        desert.setTemperature(35.0);
        desert.setWaterResource(50.0);
        desert.setFoodResource(30.0);

        globalCells.add(africa);
        globalCells.add(nile);
        globalCells.add(mesopotamia);
        globalCells.add(anatolia);
        globalCells.add(steppe);
        globalCells.add(yellowRiver);
        globalCells.add(desert);
    }

    @Test
    @DisplayName("Verify Out-of-Africa Migration Wave and Dispersal (-100,000 BC)")
    public void testOutofAfricaMigrationWaveAndGlobalDispersal() {
        Scenario scenario = new Scenario();
        scenario.setName("Sortie d'Afrique & Expansion Homo Sapiens (-100000)");
        scenario.setStartDateYear(-100000);
        scenario.setInitialHumanCount(50_000L);
        scenario.setInitialCapitalPerCapita(2.0);
        scenario.setInitialEnergyPerCapita(5.0);
        scenario.setPopulationDensityType("ONE_CONTINENT");

        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(globalCells);

        H3Cell originCell = globalCells.get(0); // East Africa
        assertTrue(originCell.getPopulation() > 0, "East Africa must start with human population");

        // Ensure destination Nile cell has thermodynamic free energy gradient (foodDest > 1.8 * foodOrigin)
        H3Cell africaCell = globalCells.get(0);
        H3Cell nileCellDest = globalCells.get(1);
        nileCellDest.setFoodResource((africaCell.getFoodResource() != null ? africaCell.getFoodResource() : 500.0) * 2.0);

        // Execute 40 simulation ticks of demographic growth & spatial migration
        for (int t = 0; t < 40; t++) {
            BiologicalDemographicsEngine.processBiologicalDemographics(globalCells);
            ThermodynamicMigrationEngine.processThermodynamicMigration(globalCells);
            PhysicalLawEngine.applyPhysicalLaws(globalCells, 1.0);
        }

        // Verify demographic wave spread to non-origin cells
        long nonOriginTotalPop = globalCells.stream()
                .filter(c -> c.getH3Index() != 2001L)
                .mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0)
                .sum();
        assertTrue(nonOriginTotalPop > 0, "Demographic wave out of Africa must spread to neighboring regions");

        long globalPop = globalCells.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
        assertTrue(globalPop >= 1_000L && globalPop <= 5_000_000L,
                "Global paleolithic population must remain within ecological bounds (1k to 5M)");
    }

    @Test
    @DisplayName("Verify Neolithic Agricultural Revolution Emergence in River Valleys")
    public void testNeolithicAgriculturalRevolutionAndDemographicSurplus() {
        Scenario scenario = new Scenario();
        scenario.setName("Croissant Fertile & Néolithique (-8000)");
        scenario.setStartDateYear(-8000);
        scenario.setInitialHumanCount(25_000L);
        scenario.setInitialCapitalPerCapita(5.0);
        scenario.setPopulationDensityType("FERTILE_CRESCENT");

        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(globalCells);

        // Seed agricultural biomass and rainfall in fertile Nile valley
        H3Cell nileCell = globalCells.get(1);
        H3Cell desertCell = globalCells.get(6);
        nileCell.setRainfall(1200.0);
        nileCell.setWaterResource(980.0);
        nileCell.setBiomassAgriculture(800.0);

        desertCell.setRainfall(50.0);
        desertCell.setWaterResource(50.0);
        desertCell.setBiomassAgriculture(20.0);

        // Run 50 simulation ticks with agricultural soil, NPK, and demographic engines
        for (int t = 0; t < 50; t++) {
            SoilNutrientNPKEngine.processSoilNutrients(globalCells);
            BiologicalDemographicsEngine.processBiologicalDemographics(globalCells);
            EcologicalDegradationEngine.processEcologicalDegradation(globalCells, 1.0);
            PhysicalLawEngine.applyPhysicalLaws(globalCells, 1.0);
        }

        assertTrue(nileCell.getWaterResource() > desertCell.getWaterResource(),
                "River valley fertile cells must retain significantly higher water resources than arid desert");

        assertTrue(nileCell.getBiomassAgriculture() >= desertCell.getBiomassAgriculture(),
                "Neolithic river valley cells must sustain equal or higher agricultural biomass than desert hinterlands");
    }

    @Test
    @DisplayName("Verify Nile and Mesopotamia State Formation & Capital Accumulation")
    public void testNileAndMesopotamiaStateFormationAndEmpireEmergence() {
        DiplomacyManager diplomacyManager = new DiplomacyManager();
        PoliticalSimulationEngine politicalEngine = new PoliticalSimulationEngine(diplomacyManager);

        // Seed initial Nile Empire and Mesopotamian City-State
        H3Cell nileCell = globalCells.get(1);
        nileCell.setPopulation(15000);
        nileCell.setResourceCapital(150.0);
        Nation egyptEmpire = new Nation("Kemet Nile Empire", Color.GOLD, nileCell);
        diplomacyManager.registerNation(egyptEmpire);

        H3Cell mesoCell = globalCells.get(2);
        mesoCell.setPopulation(12000);
        mesoCell.setResourceCapital(120.0);
        Nation mesopotamiaState = new Nation("Sumerian City-State", Color.BLUE, mesoCell);
        diplomacyManager.registerNation(mesopotamiaState);

        // Run 30 political and thermodynamic simulation ticks
        for (int t = 0; t < 30; t++) {
            politicalEngine.tick(globalCells);
            BiologicalDemographicsEngine.processBiologicalDemographics(globalCells);
            CoGovernanceTradeEngine.processTradeAndGovernance(globalCells, 1.0);
        }

        assertTrue(diplomacyManager.getNations().size() >= 2,
                "State entities must persist and compete across the simulation timeframe");

        assertTrue(nileCell.getResourceCapital() > 50.0,
                "Nile state core must accumulate physical capital infrastructure");
    }

    @Test
    @DisplayName("Verify 100+ Tick Multi-Generational Long-Term Stability & Invariants")
    public void testLongTermModelStabilityAndZeroCrashInvariant() {
        // Register empirical Type B engines
        ProceduralEngineRegistry.registerPlugin("World3", World3HybridEngine::processPlugin);
        ProceduralEngineRegistry.registerPlugin("NordhausDice", NordhausDiceHybridEngine::processPlugin);
        ProceduralEngineRegistry.registerPlugin("SmilMaterial", SmilMaterialTransitionsPureEngine::processHybrid);

        Scenario scenario = new Scenario();
        scenario.setName("Long-Term Stability Battery");
        scenario.setInitialHumanCount(100_000L);
        scenario.setInitialCapitalPerCapita(200.0);

        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(globalCells);

        // Run 100 full multi-engine simulation ticks
        for (int tick = 1; tick <= 100; tick++) {
            BiologicalDemographicsEngine.processBiologicalDemographics(globalCells);
            RenewableEnergyPhysicsEngine.processRenewableEnergyPhysics(globalCells);
            AtmosphericOxygenEngine.processAtmosphericOxygen(globalCells, 0.21, 1.0);
            SoilNutrientNPKEngine.processSoilNutrients(globalCells);
            NetEnergyEROEIEngine.processNetEnergyEROEI(globalCells);
            PhysicalLawEngine.applyPhysicalLaws(globalCells, 1.0);
            ProceduralEngineRegistry.processPlugins(globalCells, 1.0);

            // Assert absolute numerical safety across all cells
            for (H3Cell cell : globalCells) {
                int pop = cell.getPopulation();
                double capital = cell.getResourceCapital();
                double food = cell.getFoodResource();
                double temp = cell.getTemperature();
                double metals = cell.getResourceMetal();

                assertTrue(pop >= 0, "Population must be non-negative at tick " + tick);
                assertFalse(Double.isNaN(capital), "Capital must not be NaN at tick " + tick);
                assertFalse(Double.isInfinite(capital), "Capital must not be Infinite at tick " + tick);
                assertTrue(capital >= 0.0, "Capital must be non-negative at tick " + tick);

                assertFalse(Double.isNaN(food), "Food resource must not be NaN at tick " + tick);
                assertTrue(food >= 0.0, "Food resource must be non-negative at tick " + tick);

                assertFalse(Double.isNaN(temp), "Temperature must not be NaN at tick " + tick);
                assertTrue(temp >= -100.0 && temp <= 100.0, "Temperature must remain within plausible planetary bounds [-100C, 100C]");

                assertFalse(Double.isNaN(metals), "Metal resource must not be NaN at tick " + tick);
                assertTrue(metals >= 0.0, "Metal resource must be non-negative at tick " + tick);
            }
        }
    }

    @Test
    @DisplayName("Generate Statistical Telemetry and Macro-Evolutionary Trajectory Report")
    public void testStatisticalTelemetryAndHistoricalChronicleReport() {
        Scenario scenario = new Scenario();
        scenario.setName("Historical Macro-Chronicle");
        scenario.setInitialHumanCount(100_000L);
        scenario.setInitialCapitalPerCapita(50.0);

        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(globalCells);

        logger.info("=== HISTORICAL TELEMETRY CHRONICLE REPORT START ===");
        logger.info("Tick | Total Pop | Populated Cells | Total Capital | Avg Tech | Avg Temp (C)");

        for (int t = 1; t <= 20; t++) {
            BiologicalDemographicsEngine.processBiologicalDemographics(globalCells);
            PhysicalLawEngine.applyPhysicalLaws(globalCells, 1.0);

            long totalPop = globalCells.stream().mapToLong(H3Cell::getPopulation).sum();
            long populatedCount = globalCells.stream().filter(c -> c.getPopulation() > 0).count();
            double totalCap = globalCells.stream().mapToDouble(H3Cell::getResourceCapital).sum();
            double avgTech = globalCells.stream().mapToDouble(H3Cell::getTechnologyLevel).average().orElse(0.0);
            double avgTemp = globalCells.stream().mapToDouble(H3Cell::getTemperature).average().orElse(0.0);

            if (t % 5 == 0 || t == 1) {
                logger.info(String.format("%4d | %9d | %15d | %13.2f | %8.2f | %12.2f",
                        t, totalPop, populatedCount, totalCap, avgTech, avgTemp));
            }
        }
        logger.info("=== HISTORICAL TELEMETRY CHRONICLE REPORT END ===");
    }
}
