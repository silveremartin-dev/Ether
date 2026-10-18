/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TechRadiativeMitigationTest {

    @Test
    /*
     * Test technological mitigation reduces forcing operation.
     * <p>
     * Executes operational logic for {@code TechRadiativeMitigationTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testTechnologicalMitigationReducesForcing() {
        GreenhouseRadiativeEngine engine = new GreenhouseRadiativeEngine(560.0, 1800.0); // 2x CO2

        double rawForcing = engine.computeRadiativeForcingWpm2();
        assertTrue(rawForcing > 3.0, "Elevated greenhouse concentrations must generate positive forcing");

        // High technology level (Tech 8.0: Direct Air Capture & Geoengineering)
        engine.applyTechnologicalMitigation(8.0);
        assertTrue(engine.getTechnologicalMitigationEfficiency() > 0.0);

        double mitigatedForcing = engine.computeRadiativeForcingWpm2();
        assertTrue(mitigatedForcing < rawForcing, "Technological mitigation must reduce net radiative forcing");
    }
}