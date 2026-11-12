/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

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
 * @version 1.0.0-beta.3
 */
public class JancoviciBihouixPhysicalSuiteTest {

    private List<H3Cell> testCells;

    @BeforeEach
    /*
     * Set up operation.
     * <p>
     * Executes operational logic for {@code JancoviciBihouixPhysicalSuiteTest} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void setUp() {
        testCells = new ArrayList<>();
        H3Cell cell = new H3Cell(613503380827930701L, 45.0, 10.0);
        cell.setPopulation(10000);
        cell.setResourceMetal(50.0);
        cell.setResourceCapital(1000.0);
        cell.setResourceWork(100.0);
        cell.setEnergyFoodConsumed(10.0);
        cell.setPollutionLevel(10.0);

        testCells.add(cell);
    }

    @Test
    /*
     * Test ore grade thermodynamics engine operation.
     * <p>
     * Executes operational logic for {@code JancoviciBihouixPhysicalSuiteTest} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testOreGradeThermodynamicsEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getBiomassHuman()[0] = 10000.0f * 0.06f;
        buf.getMetalResource()[0] = 50.0f;
        buf.getResourceCapital()[0] = 1000.0f;
        double initialCapital = buf.getResourceCapital()[0];
        new OreGradeThermodynamicsEngine().tick(buf, 1.0, 12.0);

        assertTrue(buf.getResourceCapital()[0] < initialCapital, "Lowering ore grade should increase energy cost and reduce net capital.");
    }

    @Test
    /*
     * Test infrastructure inertia engine operation.
     * <p>
     * Executes operational logic for {@code JancoviciBihouixPhysicalSuiteTest} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testInfrastructureInertiaEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getBiomassHuman()[0] = 10000.0f * 0.06f;
        buf.getResourceCapital()[0] = 1000.0f;
        double initialCapital = buf.getResourceCapital()[0];
        new InfrastructureInertiaEngine().tick(buf, 1.0, 12.0);

        assertTrue(buf.getResourceCapital()[0] < initialCapital, "Physical turnover and aging should depreciate capital.");
    }

    @Test
    /*
     * Test entropic metal dissipation engine operation.
     * <p>
     * Executes operational logic for {@code JancoviciBihouixPhysicalSuiteTest} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testEntropicMetalDissipationEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getMetalResource()[0] = 50.0f;
        buf.getResourceCapital()[0] = 1000.0f;
        double initialMetal = buf.getMetalResource()[0];
        new EntropicMetalDissipationEngine().tick(buf, 1.0, 12.0);

        assertTrue(buf.getMetalResource()[0] < initialMetal, "2nd law of thermodynamics should dissipate metal stock.");
    }

    @Test
    /*
     * Test jevons paradox engine operation.
     * <p>
     * Executes operational logic for {@code JancoviciBihouixPhysicalSuiteTest} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testJevonsParadoxEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getBiomassHuman()[0] = (float) testCells.get(0).getPopulationOr(10000);
        buf.getResourceCapital()[0] = 50000.0f;
        buf.getEnergyFoodConsumed()[0] = testCells.get(0).getEnergyFoodConsumed() != null ? testCells.get(0).getEnergyFoodConsumed().floatValue() : 0.0f;
        double initialEnergy = buf.getEnergyFoodConsumed()[0];
        new JevonsParadoxEngine().tick(buf, 1.0, 1.0);

        assertTrue(buf.getEnergyFoodConsumed()[0] >= initialEnergy, "Jevons paradox should maintain or increase total energy consumption.");
    }
}

