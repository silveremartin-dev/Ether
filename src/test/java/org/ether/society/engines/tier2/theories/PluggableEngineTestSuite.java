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

import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite validating the Pluggable Engine Architecture (ProceduralEngineRegistry & SimulationEngineModule).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class PluggableEngineTestSuite {

    private WorldBuffer testBuffer;

    @BeforeEach
    /*
     * Set up operation.
     */
    public void setUp() {
        SimulationEngineRegistry.getInstance().resetAll();
        testBuffer = new WorldBuffer(1);
        testBuffer.getHumanPopulation()[0] = 1000.0f;
        testBuffer.getTemperature()[0] = 15.0f;
    }

    @Test
    /*
     * Test custom plugin registration and execution operation.
     */
    public void testCustomPluginRegistrationAndExecution() {
        // Register a custom user engine that increases temperature by 1.0 degree per year
        SimulationEngineRegistry.getInstance().register(new AbstractSimulationEngine("CustomGeothermalBooster", "CustomGeothermalBooster", EngineTier.TIER_1_BIOPHYSICAL) {
            @Override
            public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
                if (buffer == null || buffer.getTemperature() == null) return;
                float[] temps = buffer.getTemperature();
                for (int i = 0; i < buffer.getCapacity(); i++) {
                    temps[i] += (float) (1.0 * dtYears);
                }
            }
        });

        assertNotNull(SimulationEngineRegistry.getInstance().getEngine("CustomGeothermalBooster"));

        float initialTemp = testBuffer.getTemperature()[0];
        SimulationEngineRegistry.getInstance().getEngine("CustomGeothermalBooster").tick(testBuffer, 1.0, 12.0);

        assertEquals(initialTemp + 1.0f, testBuffer.getTemperature()[0], 0.001f, "Custom plugin should execute and modify buffer state.");
    }
}

