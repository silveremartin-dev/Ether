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
 * JUnit Test Suite validating Meadows World3 Coupled Equations & Kurzweil LOAR.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class World3AndKurzweilTestSuite {

    private List<H3Cell> testCells;

    @BeforeEach
    public void setUp() {
        testCells = new ArrayList<>();
        H3Cell cell = new H3Cell(613503380827930701L, 45.0, 10.0);
        cell.setPopulation(20000);
        cell.setResourceCapital(500.0);
        cell.setResourceMetal(100.0);
        cell.setPollutionLevel(50.0);
        cell.setTechnologyLevel(1.0);
        cell.setFoodResource(2500.0);
        cell.setLifespan(40.0);

        testCells.add(cell);
    }

    @Test
    public void testWorld3CouplingEngine() {
        double initialLifespan = testCells.get(0).getLifespan();
        World3CouplingEngine.processWorld3System(testCells, 1.0);

        assertTrue(testCells.get(0).getLifespan() > initialLifespan, "Sufficient food and capital should improve World3 life expectancy LE.");
    }

    @Test
    public void testKurzweilAcceleratingReturnsEngine() {
        double initialTech = testCells.get(0).getTechnologyLevel();
        KurzweilAcceleratingReturnsEngine.processAcceleratingReturns(testCells, 1.0);

        assertTrue(testCells.get(0).getTechnologyLevel() > initialTech, "Kurzweil LOAR double-exponential knowledge growth should increase tech level.");
    }
}
