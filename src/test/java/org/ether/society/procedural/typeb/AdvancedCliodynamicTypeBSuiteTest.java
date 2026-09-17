/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

import org.ether.society.database.H3Cell;
import org.ether.society.procedural.ProceduralEngineRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite validating Henrich Tasmanian Loss, Braudel Mediterranean Sea Highway,
 * Hamilton Kin Selection, Turchin Frontier Asabiyyah, and Buss Sexual Selection Mating models.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class AdvancedCliodynamicTypeBSuiteTest {

    private List<H3Cell> testCells;

    @BeforeEach
    public void setUp() {
        ProceduralEngineRegistry.clearPlugins();
        testCells = new ArrayList<>();

        H3Cell cell = new H3Cell(613503380827930701L, 35.0, 15.0); // Mediterranean latitude 35°N
        cell.setIsCoastal(true);
        cell.setPopulation(3000); // Below 5000 Tasmanian threshold
        cell.setTechnologyLevel(3.0);
        cell.setFoodResource(10.0); // Scarcity
        cell.setResourceCapital(1200.0); // High capital center
        cell.setResourceWork(50.0);

        testCells.add(cell);
    }

    @Test
    public void testTasmanianCulturalRegressionEngine() {
        double initialTech = testCells.get(0).getTechnologyLevel();
        TasmanianCulturalRegressionEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getTechnologyLevel() < initialTech, "Henrich Tasmanian loss: small isolated population (N=3000) should lose technology level.");
    }

    @Test
    public void testMaritimeHighwayEngine() {
        double initialCapital = testCells.get(0).getResourceCapital();
        MaritimeHighwayEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getResourceCapital() > initialCapital, "Generic Maritime highway: coastal cell with positive temperature should boost capital trade efficiency.");
    }

    @Test
    public void testLandReclamationEngine() {
        testCells.get(0).setTechnologyLevel(5.0);
        testCells.get(0).setResourceCapital(500.0);
        LandReclamationEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getIsPolder(), "Land Reclamation: Tech >= 4.0 & Capital >= 100.0 on coastal cell should create polder.");
    }

    @Test
    public void testKinSelectionHamiltonEngine() {
        assertTrue(KinSelectionHamiltonEngine.checkHamiltonRule(0.50, 10.0, 4.0), "Hamilton rule r * B > C should return true for r=0.5, B=10, C=4.");
        double initialWork = testCells.get(0).getResourceWork();
        KinSelectionHamiltonEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getResourceWork() > initialWork, "Hamilton kin selection: food scarcity should boost military work output toward outgroups.");
    }

    @Test
    public void testFrontierAsabiyyahEngine() {
        double initialWork = testCells.get(0).getResourceWork();
        FrontierAsabiyyahEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getResourceWork() < initialWork, "Turchin Asabiyyah: high capital hinterland (>1000) should suffer Asabiyyah luxury decay.");
    }

    @Test
    public void testSexualSelectionMatingEngine() {
        double initialWork = testCells.get(0).getResourceWork();
        SexualSelectionMatingEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getResourceWork() > initialWork, "Buss sexual selection: high capital inequality should mobilize young male military force.");
    }

    @Test
    public void testDynamicMaritimeRoutingGraph() {
        DynamicMaritimeRoutingGraph.clearCache();
        H3Cell coastal1 = new H3Cell(613503380827930702L, 36.0, 15.0);
        coastal1.setIsCoastal(true);
        coastal1.setPopulation(5000);
        coastal1.setTechnologyLevel(5.0);
        coastal1.setResourceCapital(1000.0);
        coastal1.setMovementFriction(2.0);

        H3Cell coastal2 = new H3Cell(613503380827930703L, 37.0, 16.0);
        coastal2.setIsCoastal(true);
        coastal2.setPopulation(4000);
        coastal2.setTechnologyLevel(5.0);
        coastal2.setResourceCapital(800.0);
        coastal2.setMovementFriction(2.0);

        List<H3Cell> network = List.of(coastal1, coastal2);
        DynamicMaritimeRoutingGraph.processHybrid(network, 1.0);

        assertTrue(coastal1.getMovementFriction() < 2.0, "Dynamic maritime graph: active oceanic lane should reduce movement friction.");
        assertTrue(coastal1.getResourceCapital() > 1000.0, "Dynamic maritime graph: route connectivity should boost capital trade.");
    }

    @Test
    public void testMarineSubmersionEngineEvacuation() {
        H3Cell floodedCell = new H3Cell(613503380827930704L, 35.0, 15.0);
        floodedCell.setElevation(-2.0);
        floodedCell.setSeaLevelOffsetMeters(2.0); // Flood level above elevation
        floodedCell.setPopulation(10000);
        floodedCell.setTechnologyLevel(6.0); // High tech early warning foresight
        floodedCell.setResourceCapital(2000.0);

        H3Cell safeNeighbor = new H3Cell(613503380827930705L, 35.1, 15.1);
        safeNeighbor.setElevation(25.0); // High elevation inland safe ground
        safeNeighbor.setPopulation(1000);
        safeNeighbor.setResourceCapital(500.0);

        List<H3Cell> region = List.of(floodedCell, safeNeighbor);
        MarineSubmersionEngine.processHybrid(region, 1.0);

        assertTrue(safeNeighbor.getPopulation() > 1000, "Marine Submersion Engine: High-tech early warning should evacuate population to safe higher-elevation cell.");
        assertTrue(floodedCell.getPopulation() < 2000, "Marine Submersion Engine: Over 80% of flooded cell population should safely evacuate rather than suffer mass mortality.");
    }

    @Test
    public void testHydrologicalEngineeringEngine() {
        // 1. Tenochtitlan Texcoco Lake Draining
        H3Cell texcocoCell = new H3Cell(613503380827930706L, 19.4, -99.1);
        texcocoCell.setBiome(org.ether.society.model.Biome.LAKE);
        texcocoCell.setPopulation(3000);
        texcocoCell.setTechnologyLevel(4.0);
        texcocoCell.setResourceCapital(200.0);

        // 2. Mountain Reservoir Dam
        H3Cell mountainCell = new H3Cell(613503380827930707L, 45.0, 6.0);
        mountainCell.setElevation(600.0);
        mountainCell.setMovementFriction(2.0);
        mountainCell.setTechnologyLevel(5.0);
        mountainCell.setResourceCapital(400.0);

        List<H3Cell> hydroCells = List.of(texcocoCell, mountainCell);
        HydrologicalEngineeringEngine.processHybrid(hydroCells, 1.0);

        assertEquals(org.ether.society.model.Biome.PLAINS, texcocoCell.getBiome(), "Hydrological engineering: Tenochtitlan model should drain lake Texcoco into urban agricultural plains.");
        assertTrue(texcocoCell.getIsPolder(), "Hydrological engineering: Drained Texcoco basin should be flagged as reclaimed land (polder).");
        assertEquals(1000.0, mountainCell.getWaterResource(), "Hydrological engineering: Mountain dam construction should maximize water reservoir retention.");
    }

    @Test
    public void testAdvancedCliodynamicPluginsCumulativeExecution() {
        ProceduralEngineRegistry.registerPlugin("B26_TasmanianLoss", TasmanianCulturalRegressionEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B27_MaritimeHighway", MaritimeHighwayEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B28_KinSelectionHamilton", KinSelectionHamiltonEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B29_FrontierAsabiyyah", FrontierAsabiyyahEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B30_SexualSelectionMating", SexualSelectionMatingEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B31_DynamicMaritimeGraph", DynamicMaritimeRoutingGraph::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B32_MarineSubmersion", MarineSubmersionEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B33_HydrologicalEngineering", HydrologicalEngineeringEngine::processHybrid);

        assertEquals(8, ProceduralEngineRegistry.getPluginCount(), "Registry should hold 8 advanced Cliodynamic plugins.");

        assertDoesNotThrow(() -> {
            ProceduralEngineRegistry.processPlugins(testCells, 1.0);
        }, "Cumulative execution of all 8 advanced Cliodynamic plugins should complete without errors.");
    }

    @Test
    public void testPerformanceConfigMapping() {
        org.ether.society.model.Scenario scenario = new org.ether.society.model.Scenario();
        scenario.setStrictDeterminism(false);
        scenario.setClimateTickFrequency(10);
        scenario.setParallelThreadCount(8);
        scenario.setParallelExecutionEnabled(true);
        scenario.setOceanMultiRateTickingEnabled(true);

        org.ether.society.procedural.SimulationPerformanceConfig config = scenario.toPerformanceConfig();
        assertFalse(config.isStrictDeterminism(), "Strict determinism should be false when optimizations are enabled.");
        assertTrue(config.isEnableParallelExecution(), "Parallel execution should be enabled.");
        assertEquals(8, config.getParallelThreadCount(), "Parallel thread count should be mapped to 8.");
        assertTrue(config.isEnableMultiFreqClimateTicks(), "Multi-freq climate ticks should be enabled.");
        assertEquals(10, config.getClimateTickFrequency(), "Climate tick frequency should be mapped to 10.");

        scenario.setStrictDeterminism(true);
        org.ether.society.procedural.SimulationPerformanceConfig configStrict = scenario.toPerformanceConfig();
        assertTrue(configStrict.isStrictDeterminism(), "Strict determinism master gate should override sub-optimizations.");
        assertFalse(configStrict.isEnableParallelExecution(), "Parallel execution should be disabled under strict determinism.");
        assertEquals(1, configStrict.getClimateTickFrequency(), "Climate tick frequency should fall back to 1:1 under strict determinism.");
    }

    @Test
    public void testBitIdenticalSimulationDeterminism() throws Exception {
        org.ether.society.model.Scenario scenario = new org.ether.society.model.Scenario();
        scenario.setName("Audit Bit-Identical Scenario");
        scenario.setStrictDeterminism(true);

        H3Cell cellA1 = new H3Cell(613503380827930801L, 40.0, 10.0);
        cellA1.setPopulation(1000);
        cellA1.setFoodResource(50.0);
        cellA1.setWaterResource(100.0);
        cellA1.setTechnologyLevel(5.0);

        H3Cell cellA2 = new H3Cell(613503380827930802L, 40.1, 10.1);
        cellA2.setPopulation(500);
        cellA2.setFoodResource(30.0);
        cellA2.setWaterResource(80.0);
        cellA2.setTechnologyLevel(4.5);

        List<H3Cell> cellsRunA = List.of(cellA1, cellA2);

        H3Cell cellB1 = new H3Cell(613503380827930801L, 40.0, 10.0);
        cellB1.setPopulation(1000);
        cellB1.setFoodResource(50.0);
        cellB1.setWaterResource(100.0);
        cellB1.setTechnologyLevel(5.0);

        H3Cell cellB2 = new H3Cell(613503380827930802L, 40.1, 10.1);
        cellB2.setPopulation(500);
        cellB2.setFoodResource(30.0);
        cellB2.setWaterResource(80.0);
        cellB2.setTechnologyLevel(4.5);

        List<H3Cell> cellsRunB = List.of(cellB1, cellB2);

        org.ether.society.config.Configuration config = org.ether.society.config.ConfigurationLoader.loadDefault();
        org.ether.society.core.H3SimulationEngine engineA = new org.ether.society.core.H3SimulationEngine(config);
        engineA.initializeFromScenario(scenario, cellsRunA);

        org.ether.society.core.H3SimulationEngine engineB = new org.ether.society.core.H3SimulationEngine(config);
        engineB.initializeFromScenario(scenario, cellsRunB);

        engineA.stepForward(30);
        engineB.stepForward(30);

        assertEquals(cellsRunA.get(0).getPopulation(), cellsRunB.get(0).getPopulation(), "Population must be bit-identical after 30 ticks.");
        assertEquals(cellsRunA.get(0).getFoodResource(), cellsRunB.get(0).getFoodResource(), "Food resources must be bit-identical after 30 ticks.");
        assertEquals(cellsRunA.get(0).getTechnologyLevel(), cellsRunB.get(0).getTechnologyLevel(), "Technology level must be bit-identical after 30 ticks.");
        assertEquals(cellsRunA.get(1).getPopulation(), cellsRunB.get(1).getPopulation(), "Neighbor cell population must be bit-identical after 30 ticks.");
    }

    @Test
    public void testTypeBEngineParametersPersistenceAndExecution() {
        org.ether.society.model.Scenario scenario = new org.ether.society.model.Scenario();
        java.util.Map<String, java.util.Map<String, Double>> params = new java.util.HashMap<>();

        java.util.Map<String, Double> maritimeParams = new java.util.HashMap<>();
        maritimeParams.put("capitalBoostRate", 0.10);
        maritimeParams.put("frictionMultiplier", 0.10);
        params.put("MaritimeHighwayEngine", maritimeParams);

        java.util.Map<String, Double> hydroParams = new java.util.HashMap<>();
        hydroParams.put("tenochtitlanTech", 2.0);
        hydroParams.put("tenochtitlanCapital", 50.0);
        params.put("HydrologicalEngineeringEngine", hydroParams);

        scenario.setTypeBEngineParameters(params);

        java.util.Map<String, java.util.Map<String, Double>> retrieved = scenario.getTypeBEngineParameters();
        assertNotNull(retrieved, "Retrieved Type B engine parameters should not be null.");
        assertEquals(0.10, retrieved.get("MaritimeHighwayEngine").get("capitalBoostRate"), 0.001);
        assertEquals(2.0, retrieved.get("HydrologicalEngineeringEngine").get("tenochtitlanTech"), 0.001);

        // Test parameterized execution of MaritimeHighwayEngine
        H3Cell coastalCell = new H3Cell(613503380827930901L, 30.0, 30.0);
        coastalCell.setIsCoastal(true);
        coastalCell.setPopulation(1000);
        coastalCell.setResourceCapital(100.0);
        coastalCell.setMovementFriction(1.0);
        coastalCell.setTemperature(20.0);

        List<H3Cell> cellList = List.of(coastalCell);
        double boost = retrieved.get("MaritimeHighwayEngine").get("capitalBoostRate");
        double mult = retrieved.get("MaritimeHighwayEngine").get("frictionMultiplier");
        MaritimeHighwayEngine.processHybrid(cellList, 1.0, boost, mult);

        assertEquals(110.0, coastalCell.getResourceCapital(), 0.001, "Parameterized MaritimeHighwayEngine should apply 10% boost.");
        assertEquals(0.10, coastalCell.getMovementFriction(), 0.001, "Parameterized MaritimeHighwayEngine should apply 0.10 friction multiplier.");
    }

    @Test
    public void testOceanWavePhysicsAndCabotage() {
        // 1. Continental Shelf vs Abyssal Wave Height (Hs)
        double coastalHs = DynamicMaritimeRoutingGraph.calculateSignificantWaveHeight(-50.0, 0.0);
        double abyssalStormHs = DynamicMaritimeRoutingGraph.calculateSignificantWaveHeight(-4000.0, 45.0);

        assertTrue(coastalHs < 2.0, "Sheltered shallow shelf waters should have low significant wave height (Hs < 2.0m).");
        assertTrue(abyssalStormHs > 3.5, "Deep abyssal storm waters should generate large ocean swells (Hs > 3.5m).");

        // 2. Primitive vs Advanced Hull Wave Clearance Tolerance
        double primitiveClearance = DynamicMaritimeRoutingGraph.calculateFleetWaveClearance(10.0, 1.0);
        double advancedClearance = DynamicMaritimeRoutingGraph.calculateFleetWaveClearance(1500.0, 6.0);

        assertTrue(primitiveClearance < abyssalStormHs, "Primitive coastal rafts (K=10) should have hull clearance below deep abyssal storm swells.");
        assertTrue(advancedClearance > abyssalStormHs, "Advanced ocean freighters (K=1500, Tech=6.0) should easily exceed deep ocean swell heights.");
    }
}

