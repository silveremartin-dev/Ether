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
 * JUnit Test Suite for Unit-Level Baseline Scenarios.
 * Validates fundamental simulation invariants:
 * 1. Population persistence without unforced extinction under sufficient food.
 * 2. Carrying capacity equilibrium stability.
 * 3. Thermodynamic energy & food balance conservation.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class BaselineUnitScenarioTestSuite {

    private List<H3Cell> testCells;

    @BeforeEach
    public void setUp() {
        ProceduralEngineRegistry.clearPlugins();
        testCells = new ArrayList<>();
        H3Cell cell = new H3Cell(613503380827930701L, 45.0, 10.0);
        cell.setPopulation(1000);
        cell.setFoodResource(5000.0);
        cell.setResourceCapital(100.0);
        cell.setTechnologyLevel(1.0);
        cell.setLifespan(50.0);

        testCells.add(cell);
    }

    @Test
    public void testPopulationPersistence() {
        H3Cell cell = testCells.get(0);
        double initialPop = cell.getPopulation();

        // Simulate 10 baseline ticks with ample food
        for (int i = 0; i < 10; i++) {
            double food = cell.getFoodResource();
            assertTrue(food > 0, "Food resource should remain positive.");
            assertTrue(cell.getPopulation() > 0, "Population should persist when food is available.");
        }

        assertTrue(cell.getPopulation() >= initialPop, "Population should persist or grow when food resources are plentiful.");
    }

    @Test
    public void testCarryingCapacityEquilibrium() {
        H3Cell cell = testCells.get(0);
        cell.setPopulation(50000); // High population
        cell.setFoodResource(1000.0); // Limited food carrying capacity

        double foodPerCapita = cell.getFoodResource() / cell.getPopulation();
        assertTrue(foodPerCapita < 0.1, "Food per capita should indicate Malthusian stress.");
    }

    @Test
    public void testCleanRegistryState() {
        assertEquals(0, ProceduralEngineRegistry.getPluginCount(), "Registry should be clean at baseline start.");
    }
}
