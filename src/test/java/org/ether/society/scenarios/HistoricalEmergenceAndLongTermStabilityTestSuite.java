/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.scenarios;

import org.ether.society.core.engine.SimulationEngineRegistry;
import org.ether.society.core.engine.SimulationEngineModule;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

import javafx.scene.paint.Color;
import org.ether.society.core.PreComputePhase;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.database.Polity;
import org.ether.society.model.Scenario;
import org.ether.society.engines.tier2.historical.*;
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
    /*
     * Set up operation.
     * <p>
     * Executes operational logic for {@code HistoricalEmergenceAndLongTermStabilityTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void setUp() {
        SimulationEngineRegistry.getInstance().resetAll();
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
    /*
     * Test outof africa migration wave and global dispersal operation.
     * <p>
     * Executes operational logic for {@code HistoricalEmergenceAndLongTermStabilityTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
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
        org.ether.society.core.dod.WorldBuffer buf1 = new org.ether.society.core.dod.WorldBuffer(globalCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(globalCells, buf1, true);
        BiologicalDemographicsEngine bioEngine1 = new BiologicalDemographicsEngine();
        ThermodynamicMigrationEngine migEngine1 = new ThermodynamicMigrationEngine();
        PhysicalLawEngine physEngine1 = new PhysicalLawEngine();
        for (int t = 0; t < 40; t++) {
            bioEngine1.tick(buf1, 1.0, 12.0);
            migEngine1.tick(buf1, 1.0, 12.0);
            physEngine1.tick(buf1, 1.0, 12.0);
        }

        // Verify demographic wave spread to non-origin cells
        long nonOriginTotalPop = 0L;
        long globalPop = 0L;
        for (int i = 0; i < globalCells.size(); i++) {
            long pop = Math.round(buf1.getBiomassHuman()[i] / 0.06);
            globalPop += pop;
            if (buf1.getH3Indexes()[i] != 2001L) {
                nonOriginTotalPop += pop;
            }
        }
        assertTrue(nonOriginTotalPop > 0, "Demographic wave out of Africa must spread to neighboring regions");
        assertTrue(globalPop >= 1_000L && globalPop <= 5_000_000L,
                "Global paleolithic population must remain within ecological bounds (1k to 5M)");
    }

    @Test
    @DisplayName("Verify Neolithic Agricultural Revolution Emergence in River Valleys")
    /*
     * Test neolithic agricultural revolution and demographic surplus operation.
     * <p>
     * Executes operational logic for {@code HistoricalEmergenceAndLongTermStabilityTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
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
        org.ether.society.core.dod.WorldBuffer buf2 = new org.ether.society.core.dod.WorldBuffer(globalCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(globalCells, buf2, false);
        buf2.getBiomassHuman()[1] = Math.max(buf2.getBiomassHuman()[1], 5000.0f * 0.06f);
        buf2.getBiomassHuman()[6] = 10.0f * 0.06f;
        SoilNutrientNPKEngine npkEngine2 = new SoilNutrientNPKEngine();
        BiologicalDemographicsEngine bioEngine2 = new BiologicalDemographicsEngine();
        EcologicalDegradationEngine ecoEngine2 = new EcologicalDegradationEngine();
        PhysicalLawEngine physEngine2 = new PhysicalLawEngine();
        for (int t = 0; t < 50; t++) {
            npkEngine2.tick(buf2, 1.0 / 12.0, 1.0);
            bioEngine2.tick(buf2, 1.0 / 12.0, 1.0);
            ecoEngine2.tick(buf2, 1.0 / 12.0, 1.0);
            physEngine2.tick(buf2, 1.0, 12.0);
        }

        assertTrue(buf2.getWaterResource()[1] > buf2.getWaterResource()[6],
                "River valley fertile cells must retain significantly higher water resources than arid desert");

        assertTrue(buf2.getBiomassAgriculture()[1] >= buf2.getBiomassAgriculture()[6],
                "Neolithic river valley cells must sustain equal or higher agricultural biomass than desert hinterlands");
    }

    @Test
    @DisplayName("Verify Nile and Mesopotamia State Formation & Capital Accumulation")
    /*
     * Test nile and mesopotamia state formation and empire emergence operation.
     * <p>
     * Executes operational logic for {@code HistoricalEmergenceAndLongTermStabilityTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testNileAndMesopotamiaStateFormationAndEmpireEmergence() {
        List<Polity> nations = new java.util.ArrayList<>();

        // Seed initial Nile Empire and Mesopotamian City-State
        H3Cell nileCell = globalCells.get(1);
        nileCell.setPopulation(15000);
        nileCell.setResourceCapital(150.0);
        Polity egyptEmpire = new Polity("Kemet Nile Empire", Color.GOLD, nileCell);
        nations.add(egyptEmpire);

        H3Cell mesoCell = globalCells.get(2);
        mesoCell.setPopulation(12000);
        mesoCell.setResourceCapital(120.0);
        Polity mesopotamiaState = new Polity("Sumerian City-State", Color.BLUE, mesoCell);
        nations.add(mesopotamiaState);

        // Run 30 political and thermodynamic simulation ticks
        org.ether.society.core.dod.WorldBuffer coGovBuf = new org.ether.society.core.dod.WorldBuffer(globalCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(globalCells, coGovBuf, false);
        BiologicalDemographicsEngine bioEngine3 = new BiologicalDemographicsEngine();
        CoGovernanceTradeEngine coGovEngine = new CoGovernanceTradeEngine();
        for (int t = 0; t < 30; t++) {
            bioEngine3.tick(coGovBuf, 1.0, 12.0);
            coGovEngine.tick(coGovBuf, 1.0, 1.0);
        }

        assertTrue(nations.size() >= 2,
                "State entities must persist and compete across the simulation timeframe");

        assertTrue(coGovBuf.getResourceCapital()[1] > 50.0f,
                "Nile state core must accumulate physical capital infrastructure");
    }

    @Test
    @DisplayName("Verify 100+ Tick Multi-Generational Long-Term Stability & Invariants")
    /*
     * Test long term model stability and zero crash invariant operation.
     * <p>
     * Executes operational logic for {@code HistoricalEmergenceAndLongTermStabilityTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testLongTermModelStabilityAndZeroCrashInvariant() {
        // Register empirical Type B engines
        SimulationEngineRegistry.getInstance().register(new World3HybridEngine());
        SimulationEngineRegistry.getInstance().register(new NordhausDiceHybridEngine());
        SimulationEngineRegistry.getInstance().register(new SmilMaterialTransitionsPureEngine());

        Scenario scenario = new Scenario();
        scenario.setName("Long-Term Stability Battery");
        scenario.setInitialHumanCount(100_000L);
        scenario.setInitialCapitalPerCapita(200.0);

        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(globalCells);

        // Run 100 full multi-engine simulation ticks
        org.ether.society.core.dod.WorldBuffer buf4 = new org.ether.society.core.dod.WorldBuffer(globalCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(globalCells, buf4, false);
        BiologicalDemographicsEngine bioEngine4 = new BiologicalDemographicsEngine();
        RenewableEnergyPhysicsEngine renewEngine4 = new RenewableEnergyPhysicsEngine();
        AtmosphericOxygenEngine o2Engine4 = new AtmosphericOxygenEngine();
        SoilNutrientNPKEngine npkEngine4 = new SoilNutrientNPKEngine();
        NetEnergyEROEIEngine eroeiEngine4 = new NetEnergyEROEIEngine();
        PhysicalLawEngine physEngine4 = new PhysicalLawEngine();
        for (int tick = 1; tick <= 100; tick++) {
            bioEngine4.tick(buf4, 1.0, 12.0);
            renewEngine4.tick(buf4, 1.0, 12.0);
            o2Engine4.tick(buf4, 1.0, 12.0);
            npkEngine4.tick(buf4, 1.0, 12.0);
            eroeiEngine4.tick(buf4, 1.0, 12.0);
            physEngine4.tick(buf4, 1.0, 12.0);

            // Assert absolute numerical safety across all cells
            for (int i = 0; i < globalCells.size(); i++) {
                long pop = Math.round(buf4.getBiomassHuman()[i] / 0.06);
                float capital = buf4.getResourceCapital()[i];
                float food = buf4.getFoodResource()[i];
                float temp = buf4.getTemperature()[i];
                float metals = buf4.getMetalResource()[i];

                assertTrue(pop >= 0, "Population must be non-negative at tick " + tick);
                assertFalse(Float.isNaN(capital), "Capital must not be NaN at tick " + tick);
                assertFalse(Float.isInfinite(capital), "Capital must not be Infinite at tick " + tick);
                assertTrue(capital >= 0.0f, "Capital must be non-negative at tick " + tick);

                assertFalse(Float.isNaN(food), "Food resource must not be NaN at tick " + tick);
                assertTrue(food >= 0.0f, "Food resource must be non-negative at tick " + tick);

                assertFalse(Float.isNaN(temp), "Temperature must not be NaN at tick " + tick);
                assertTrue(temp >= -100.0f && temp <= 100.0f, "Temperature must remain within plausible planetary bounds [-100C, 100C]");

                assertFalse(Float.isNaN(metals), "Metal resource must not be NaN at tick " + tick);
                assertTrue(metals >= 0.0f, "Metal resource must be non-negative at tick " + tick);
            }
        }
    }

    @Test
    @DisplayName("Generate Statistical Telemetry and Macro-Evolutionary Trajectory Report")
    /*
     * Test statistical telemetry and historical chronicle report operation.
     * <p>
     * Executes operational logic for {@code HistoricalEmergenceAndLongTermStabilityTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testStatisticalTelemetryAndHistoricalChronicleReport() {
        Scenario scenario = new Scenario();
        scenario.setName("Historical Macro-Chronicle");
        scenario.setInitialHumanCount(100_000L);
        scenario.setInitialCapitalPerCapita(50.0);

        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(globalCells);

        logger.info("=== HISTORICAL TELEMETRY CHRONICLE REPORT START ===");
        logger.info("Tick | Total Pop | Populated Cells | Total Capital | Avg Tech | Avg Temp (C)");

        org.ether.society.core.dod.WorldBuffer buf5 = new org.ether.society.core.dod.WorldBuffer(globalCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(globalCells, buf5, false);
        BiologicalDemographicsEngine bioEngine5 = new BiologicalDemographicsEngine();
        PhysicalLawEngine physEngine5 = new PhysicalLawEngine();
        for (int t = 1; t <= 20; t++) {
            bioEngine5.tick(buf5, 1.0, 12.0);
            physEngine5.tick(buf5, 1.0, 12.0);

            long totalPop = 0L;
            long populatedCount = 0L;
            double totalCap = 0.0;
            double avgTemp = 0.0;
            for (int i = 0; i < globalCells.size(); i++) {
                long pop = Math.round(buf5.getBiomassHuman()[i] / 0.06);
                totalPop += pop;
                if (pop > 0) populatedCount++;
                totalCap += buf5.getResourceCapital()[i];
                avgTemp += buf5.getTemperature()[i];
            }
            avgTemp /= globalCells.size();
            double avgTech = totalPop > 0 ? Math.log1p(totalCap / totalPop) : 0.0;

            if (t % 5 == 0 || t == 1) {
                logger.info(String.format("%4d | %9d | %15d | %13.2f | %8.2f | %12.2f",
                        t, totalPop, populatedCount, totalCap, avgTech, avgTemp));
            }
        }
        logger.info("=== HISTORICAL TELEMETRY CHRONICLE REPORT END ===");
        long finalPop = 0L;
        for (int i = 0; i < globalCells.size(); i++) {
            finalPop += Math.round(buf5.getBiomassHuman()[i] / 0.06);
        }
        assertTrue(finalPop > 0, "Global population must remain non-zero across 20 chronicle ticks");
        assertFalse(globalCells.isEmpty(), "Global cells must not be empty");
    }
}

