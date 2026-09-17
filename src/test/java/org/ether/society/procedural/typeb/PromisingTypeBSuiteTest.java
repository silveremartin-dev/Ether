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
 * JUnit Test Suite validating Pinker, Scott (Against the Grain), AI Regulation, Ostrom Commons, and Smil Material Transitions.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class PromisingTypeBSuiteTest {

    private List<H3Cell> testCells;

    @BeforeEach
    public void setUp() {
        ProceduralEngineRegistry.clearPlugins();
        testCells = new ArrayList<>();
        H3Cell cell = new H3Cell(613503380827930701L, 45.0, 10.0);
        cell.setPopulation(10000);
        cell.setTechnologyLevel(2.0); // Early agrarian state
        cell.setLifespan(40.0);
        cell.setPollutionLevel(10.0);
        cell.setAccessibleAquifer(30.0);
        cell.setResourceCapital(1000.0);
        cell.setResourceWork(50.0);

        testCells.add(cell);
    }

    @Test
    public void testPinkerViolenceDeclineEngine() {
        double rateHigh = PinkerViolenceDeclinePureEngine.calculateViolentDeathRatePerCapita(0.1, 0.1, 0.1);
        double rateLow = PinkerViolenceDeclinePureEngine.calculateViolentDeathRatePerCapita(0.9, 0.9, 0.9);

        assertTrue(rateLow < rateHigh, "Pinker engine: violent mortality rate should drop with high literacy and state strength.");
    }

    @Test
    public void testScottAgainstTheGrainEngine() {
        double initialLifespan = testCells.get(0).getLifespan();
        ScottAgainstTheGrainPureEngine.processHybrid(testCells, 1.0);

        assertTrue(testCells.get(0).getLifespan() < initialLifespan, "Scott engine: early agrarian state transition should reduce lifespan.");
    }

    @Test
    public void testAiAutonomousRegulationEngine() {
        testCells.get(0).setTechnologyLevel(7.0); // High AI technology
        double initialWork = testCells.get(0).getResourceWork();
        AiAutonomousRegulationPureEngine.processHybrid(testCells, 1.0);

        assertTrue(testCells.get(0).getResourceWork() > initialWork, "AI Autonomous Regulation engine: high tech should optimize resource work output.");
    }

    @Test
    public void testOstromCommonsEngine() {
        double initialAquifer = testCells.get(0).getAccessibleAquifer();
        OstromCommonsPureEngine.processHybrid(testCells, 1.0);

        assertTrue(testCells.get(0).getAccessibleAquifer() > initialAquifer, "Ostrom engine: polycentric governance should protect depleted aquifers.");
    }

    @Test
    public void testSmilMaterialTransitionsEngine() {
        double initialCapital = testCells.get(0).getResourceCapital();
        SmilMaterialTransitionsPureEngine.processHybrid(testCells, 1.0);

        assertTrue(testCells.get(0).getResourceCapital() < initialCapital, "Smil engine: 60-year material turnover inertia should depreciate capital.");
    }

    @Test
    public void testAllPromisingTypeBPluginsCumulativeExecution() {
        ProceduralEngineRegistry.registerPlugin("B16_Pinker", PinkerViolenceDeclinePureEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B17_Scott", ScottAgainstTheGrainPureEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B18_AiRegulation", AiAutonomousRegulationPureEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B19_Ostrom", OstromCommonsPureEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B20_Smil", SmilMaterialTransitionsPureEngine::processHybrid);

        assertEquals(5, ProceduralEngineRegistry.getPluginCount(), "Registry should hold 5 new Type B plugins.");

        assertDoesNotThrow(() -> {
            ProceduralEngineRegistry.processPlugins(testCells, 1.0);
        }, "Cumulative execution of all 5 new Type B plugins should complete without errors.");
    }
}

