/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.SimulationEngineRegistry;
import org.ether.society.core.engine.SimulationEngineModule;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

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
 * JUnit Test Suite validating the Pluggable Engine Architecture (ProceduralEngineRegistry & SimulationEngineModule).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class PluggableEngineTestSuite {

    private List<H3Cell> testCells;

    @BeforeEach
    /*
     * Set up operation.
     * <p>
     * Executes operational logic for {@code PluggableEngineTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void setUp() {
        SimulationEngineRegistry.getInstance().resetAll();
        testCells = new ArrayList<>();
        H3Cell cell = new H3Cell(613503380827930701L, 45.0, 10.0);
        cell.setPopulation(1000);
        cell.setTemperature(15.0);
        testCells.add(cell);
    }

    @Test
    /*
     * Test custom plugin registration and execution operation.
     */
    public void testCustomPluginRegistrationAndExecution() {
        // Register a custom user engine that increases temperature by 1.0 degree per tick
        SimulationEngineRegistry.getInstance().register(new AbstractSimulationEngine("CustomGeothermalBooster", "CustomGeothermalBooster", EngineTier.TIER_1_BIOPHYSICAL) {
            @Override
            public void tick(org.ether.society.core.dod.WorldBuffer buffer, double dtYears, double dtMonthly) {
                // Buffer tick
            }

            @Override
            public void process(List<H3Cell> cells, double deltaYears) {
                for (H3Cell cell : cells) {
                    if (cell != null) {
                        cell.setTemperature(cell.getTemperature() + 1.0 * deltaYears);
                    }
                }
            }
        });

        assertNotNull(SimulationEngineRegistry.getInstance().getEngine("CustomGeothermalBooster"));

        double initialTemp = testCells.get(0).getTemperature();
        SimulationEngineRegistry.getInstance().getEngine("CustomGeothermalBooster").process(testCells, 1.0);

        assertEquals(initialTemp + 1.0, testCells.get(0).getTemperature(), 0.001, "Custom plugin should execute and modify cell state.");
    }
}

