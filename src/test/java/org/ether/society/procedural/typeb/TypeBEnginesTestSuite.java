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
 * JUnit Test Suite validating Type B Empirical Engines (World3, HANDY NASA, Nordhaus DICE)
 * in both Pure Standalone and Ether Hybrid cumulative variants.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class TypeBEnginesTestSuite {

    private List<H3Cell> testCells;

    @BeforeEach
    public void setUp() {
        ProceduralEngineRegistry.clearPlugins();
        testCells = new ArrayList<>();
        H3Cell cell = new H3Cell(613503380827930701L, 45.0, 10.0);
        cell.setPopulation(10000);
        cell.setResourceCapital(1000.0);
        cell.setResourceMetal(20.0);
        cell.setPollutionLevel(50.0);
        cell.setTemperature(20.0); // +5°C anomaly
        cell.setLifespan(70.0);

        testCells.add(cell);
    }

    @Test
    public void testWorld3PureAndHybridEngines() {
        World3PureEngine pureWorld3 = new World3PureEngine();
        pureWorld3.processTick(1.0);
        assertTrue(pureWorld3.getPopulation() > 0, "World3 Pure model should compute population dynamics.");

        double initialCapital = testCells.get(0).getResourceCapital();
        World3HybridEngine.processPlugin(testCells, 1.0);
        assertTrue(testCells.get(0).getResourceCapital() < initialCapital, "World3 Hybrid FCAOR sink should reduce capital output.");
    }

    @Test
    public void testHandyNasaPureAndHybridEngines() {
        HandyNasaPureEngine pureHandy = new HandyNasaPureEngine();
        pureHandy.processTick(1.0);
        assertTrue(pureHandy.getCommonersX() > 0, "NASA HANDY Pure model should compute Commoners state.");

        double initialBiomass = testCells.get(0).getBiomassNatural() != null ? testCells.get(0).getBiomassNatural() : 100.0;
        testCells.get(0).setBiomassNatural(initialBiomass);
        HandyNasaHybridEngine.processPlugin(testCells, 1.0);
        assertTrue(testCells.get(0).getBiomassNatural() < initialBiomass, "NASA HANDY Hybrid should deplete biomass carrying capacity.");
    }

    @Test
    public void testNordhausDicePureAndHybridEngines() {
        NordhausDicePureEngine pureDice = new NordhausDicePureEngine();
        pureDice.processTick(1.0);
        assertTrue(pureDice.getGdp() > 0, "Nordhaus DICE Pure model should compute GDP.");

        double initialCapital = testCells.get(0).getResourceCapital();
        NordhausDiceHybridEngine.processPlugin(testCells, 1.0);
        assertTrue(testCells.get(0).getResourceCapital() < initialCapital, "Nordhaus DICE Hybrid should penalize capital based on temperature anomaly.");
    }

    @Test
    public void testCumulativePluginsExecution() {
        // Register 3 Type B hybrid plugins simultaneously
        ProceduralEngineRegistry.registerPlugin("B1_2_World3Hybrid", World3HybridEngine::processPlugin);
        ProceduralEngineRegistry.registerPlugin("B7_2_HandyNasaHybrid", HandyNasaHybridEngine::processPlugin);
        ProceduralEngineRegistry.registerPlugin("B8_2_NordhausDiceHybrid", NordhausDiceHybridEngine::processPlugin);

        assertEquals(3, ProceduralEngineRegistry.getPluginCount(), "Registry should hold 3 active Type B plugins.");

        assertDoesNotThrow(() -> {
            ProceduralEngineRegistry.processPlugins(testCells, 1.0);
        }, "Cumulative execution of multiple Type B plugins should complete without exceptions.");
    }
}

