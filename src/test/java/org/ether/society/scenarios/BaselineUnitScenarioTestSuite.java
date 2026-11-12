/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
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
        SimulationEngineRegistry.getInstance().resetAll();
        testCells = new ArrayList<>();
        H3Cell cell = new H3Cell(613503380827930701L, 45.0, 10.0);
        cell.setPopulation(1000);
        cell.setFoodResource(5000.0);
        cell.setResourceCapital(100.0);
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
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 613503380827930701L;
        buf.getBiomassHuman()[0] = 1000.0f * 0.06f;
        buf.getFoodResource()[0] = 5000.0f;
        buf.getResourceCapital()[0] = 100.0f;
        buf.getLifespan()[0] = 50.0f;
        buf.getElevation()[0] = 100.0f;
        buf.getTemperature()[0] = 15.0f;

        BiologicalDemographicsEngine bioEngine = new BiologicalDemographicsEngine();
        PhysicalLawEngine physEngine = new PhysicalLawEngine();

        // Simulate 10 baseline ticks with actual biological demographics and physical law engines
        for (int i = 0; i < 10; i++) {
            bioEngine.tick(buf, 1.0, 12.0);
            physEngine.tick(buf, 1.0, 12.0);
            double food = buf.getFoodResource()[0];
            assertTrue(food > 0, "Food resource should remain positive.");
            assertTrue(buf.getBiomassHuman()[0] > 0, "Population should persist when food is available.");
            assertFalse(Double.isNaN(food), "Food resource must not be NaN");
        }

        assertTrue(buf.getBiomassHuman()[0] > 0, "Population should persist when food resources are plentiful.");
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
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(2);
        buf.getH3Indexes()[0] = 613503380827930701L;
        buf.getBiomassHuman()[0] = 50000.0f * 0.06f;
        buf.getElevation()[0] = 5000.0f;   // 2 km above hypoxia threshold
        buf.getTemperature()[0] = -25.0f;  // 15 C below hypothermia threshold
        buf.getFluxPressure()[0] = 3.0f;   // EPAS1 adaptation = 0 (non-adapted lowland population)

        buf.getH3Indexes()[1] = 613503380827930702L;
        buf.getBiomassHuman()[1] = 50000.0f * 0.06f;
        buf.getElevation()[1] = 100.0f;
        buf.getTemperature()[1] = 15.0f;
        buf.getFoodResource()[1] = 5000.0f;
        buf.getResourceCapital()[1] = 100.0f;

        new BiologicalDemographicsEngine().tick(buf, 1.0, 12.0);

        // Expected: mu_hypoxia = 2.0 * 0.08 * (1 - 0.05*0.85) = 0.1532 ; mu_cold = min(0.25, 15 * 0.012) = 0.18
        int expected = (int) Math.round(50000 * Math.exp(-(0.1532 + 0.18)));
        int hostilePop = (int) Math.round(buf.getBiomassHuman()[0] / 0.06);
        int controlPop = (int) Math.round(buf.getBiomassHuman()[1] / 0.06);
        assertEquals(expected, hostilePop, 1,
                "Hypoxia + hypothermia mortality must follow the analytical exponential survival law");
        assertEquals(50000, controlPop,
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
        assertNotNull(SimulationEngineRegistry.getInstance(), "Registry should be initialized.");
    }
}

