/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite validating the Pluggable Engine Architecture (ProceduralEngineRegistry & ProceduralEnginePlugin).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class PluggableEngineTestSuite {

    private List<H3Cell> testCells;

    @BeforeEach
    public void setUp() {
        ProceduralEngineRegistry.clearPlugins();
        testCells = new ArrayList<>();
        H3Cell cell = new H3Cell(613503380827930701L, 45.0, 10.0);
        cell.setPopulation(1000);
        cell.setTemperature(15.0);
        testCells.add(cell);
    }

    @Test
    public void testCustomPluginRegistrationAndExecution() {
        // Register a custom user plugin that increases temperature by 1.0 degree per tick
        ProceduralEngineRegistry.registerPlugin("CustomGeothermalBooster", (cells, deltaYears) -> {
            for (H3Cell cell : cells) {
                if (cell != null) {
                    cell.setTemperature(cell.getTemperature() + 1.0 * deltaYears);
                }
            }
        });

        assertEquals(1, ProceduralEngineRegistry.getPluginCount(), "Registry should have 1 custom plugin.");

        double initialTemp = testCells.get(0).getTemperature();
        ProceduralEngineRegistry.processPlugins(testCells, 1.0);

        assertEquals(initialTemp + 1.0, testCells.get(0).getTemperature(), 0.001, "Custom plugin should execute and modify cell state.");

        ProceduralEngineRegistry.unregisterPlugin("CustomGeothermalBooster");
        assertEquals(0, ProceduralEngineRegistry.getPluginCount(), "Registry should be empty after unregistering.");
    }
}

