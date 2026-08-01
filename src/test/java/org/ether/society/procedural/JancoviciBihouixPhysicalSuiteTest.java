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
 * JUnit Test Suite validating Ore Grade Depletion, Infrastructure Inertia, Entropic Metal Dissipation, and Jevons Paradox.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class JancoviciBihouixPhysicalSuiteTest {

    private List<H3Cell> testCells;

    @BeforeEach
    public void setUp() {
        testCells = new ArrayList<>();
        H3Cell cell = new H3Cell(613503380827930701L, 45.0, 10.0);
        cell.setPopulation(10000);
        cell.setResourceMetal(50.0);
        cell.setResourceCapital(1000.0);
        cell.setResourceWork(100.0);
        cell.setTechnologyLevel(4.0);
        cell.setEnergyFoodConsumed(10.0);
        cell.setPollutionLevel(10.0);

        testCells.add(cell);
    }

    @Test
    public void testOreGradeThermodynamicsEngine() {
        double initialWork = testCells.get(0).getResourceWork();
        OreGradeThermodynamicsEngine.processOreDepletion(testCells, 1.0);

        assertTrue(testCells.get(0).getResourceWork() < initialWork, "Lowering ore grade should increase energy cost and reduce available work.");
    }

    @Test
    public void testInfrastructureInertiaEngine() {
        double initialCapital = testCells.get(0).getResourceCapital();
        InfrastructureInertiaEngine.processInfrastructureInertia(testCells, 1.0);

        assertTrue(testCells.get(0).getResourceCapital() < initialCapital, "Physical turnover and aging should depreciate capital.");
    }

    @Test
    public void testEntropicMetalDissipationEngine() {
        double initialMetal = testCells.get(0).getResourceMetal();
        EntropicMetalDissipationEngine.processEntropicDissipation(testCells, 1.0);

        assertTrue(testCells.get(0).getResourceMetal() < initialMetal, "2nd law of thermodynamics should dissipate metal stock.");
    }

    @Test
    public void testJevonsParadoxEngine() {
        double initialEnergy = testCells.get(0).getEnergyFoodConsumed();
        JevonsParadoxEngine.processJevonsRebound(testCells, 1.0);

        assertTrue(testCells.get(0).getEnergyFoodConsumed() > initialEnergy, "Jevons paradox should increase total energy consumption when tech level is high.");
    }
}
