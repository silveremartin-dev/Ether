/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TechRadiativeMitigationTest {

    @Test
    public void testTechnologicalMitigationReducesForcing() {
        GreenhouseRadiativeEngine engine = new GreenhouseRadiativeEngine(560.0, 1800.0); // 2x CO2

        double rawForcing = engine.computeRadiativeForcingWpm2();
        assertTrue(rawForcing > 3.0, "Elevated greenhouse concentrations must generate positive forcing");

        // Physical Direct Air Capture / scrubbing: directly removes atmospheric CO2
        engine.addEmissions(-150.0, -300.0);
        double mitigatedForcing = engine.computeRadiativeForcingWpm2();
        assertTrue(mitigatedForcing < rawForcing, "Physical greenhouse gas reduction must reduce net radiative forcing");
        assertTrue(engine.getCurrentCo2Ppm() < 560.0);
    }
}
