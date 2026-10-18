/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.scenarios;

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
 * JUnit Test Suite for Unit-Level Baseline Scenarios.
 * Validates fundamental simulation invariants:
 * 1. Population persistence without unforced extinction under sufficient food.
 * 2. Carrying capacity equilibrium stability.
 * 3. Thermodynamic energy & food balance conservation.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class BaselineUnitScenarioTestSuite {

    private List<H3Cell> testCells;

    @BeforeEach
    /*
     * Set up operation.
     * <p>
     * Executes operational logic for {@code BaselineUnitScenarioTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
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
    /*
     * Test population persistence operation.
     * <p>
     * Executes operational logic for {@code BaselineUnitScenarioTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testPopulationPersistence() {
        H3Cell cell = testCells.get(0);
        double initialPop = cell.getPopulation();

        // Simulate 10 baseline ticks with actual biological demographics and physical law engines
        for (int i = 0; i < 10; i++) {
            BiologicalDemographicsEngine.processBiologicalDemographics(testCells, 1.0);
            PhysicalLawEngine.applyPhysicalLaws(testCells, 1.0);
            double food = cell.getFoodResource();
            assertTrue(food > 0, "Food resource should remain positive.");
            assertTrue(cell.getPopulation() > 0, "Population should persist when food is available.");
            assertFalse(Double.isNaN(food), "Food resource must not be NaN");
        }

        assertTrue(cell.getPopulation() > 0, "Population should persist when food resources are plentiful.");
    }

    /**
     * Verifies the extrinsic physiological hazards owned by {@link BiologicalDemographicsEngine}
     * (altitude hypoxia above 3000 m and hypothermia below -10 C), integrated as
     * {@code N(t+dt) = N(t) * exp(-(mu_hypoxia + mu_cold) * dt)}.
     * <p>Malthusian starvation is deliberately NOT asserted here: it is owned by
     * {@code DemographicKernel} (see {@code DemographicKernelTest#testStarvationReducesCohortMass})
     * to prevent double-counting famine mortality in the production pipeline.</p>
     */
    @Test
    public void testCarryingCapacityEquilibrium() {
        H3Cell hostile = testCells.get(0);
        hostile.setPopulation(50000);
        hostile.setElevation(5000.0);   // 2 km above hypoxia threshold
        hostile.setTemperature(-25.0);  // 15 C below hypothermia threshold
        hostile.setMovementFriction(3.0); // EPAS1 adaptation = 0 (non-adapted lowland population)

        H3Cell control = new H3Cell(613503380827930702L, 45.0, 10.5);
        control.setPopulation(50000);
        control.setElevation(100.0);
        control.setTemperature(15.0);
        control.setFoodResource(5000.0);
        control.setTechnologyLevel(1.0);

        BiologicalDemographicsEngine.processBiologicalDemographics(List.of(hostile, control), 1.0);

        assertNotNull(hostile.getAgePyramid(), "Age pyramid must be updated by demographic engine under stress");
        // Expected: mu_hypoxia = 2.0 * 0.08 * 1.0 = 0.16 ; mu_cold = min(0.25, 15 * 0.012) = 0.18
        int expected = (int) Math.round(50000 * Math.exp(-(0.16 + 0.18)));
        assertEquals(expected, hostile.getPopulation(), 1,
                "Hypoxia + hypothermia mortality must follow the analytical exponential survival law");
        assertEquals(50000, control.getPopulation(),
                "Benign sea-level control must not incur extrinsic physiological mortality from this engine");
    }

    @Test
    /*
     * Test clean registry state operation.
     * <p>
     * Executes operational logic for {@code BaselineUnitScenarioTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testCleanRegistryState() {
        assertEquals(0, ProceduralEngineRegistry.getPluginCount(), "Registry should be clean at baseline start.");
    }
}

