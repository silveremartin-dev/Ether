/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Standalone Theoretical Reference Engines:
 * 1. World3 Meadows Pure Model (POP, IC, NR, PPOL dynamics).
 * 2. William Nordhaus DICE Pure Model (Quadratic climate damage function).
 * 3. NASA HANDY Pure Model (Elites, Commoners, Nature, Wealth dynamics).
 */
public class MacroeconomicAndLimitsPureEnginesTest {

    @Test
    @DisplayName("World3 Meadows: Industrial output accelerates non-renewable resource depletion")
    public void testWorld3PureDynamics() {
        World3PureEngine engine = new World3PureEngine();

        assertEquals(1.6e9, engine.getPopulation(), 1e5);
        assertEquals(1.0e12, engine.getNonRenewableResources(), 1e6);

        // Run 10 annual ticks
        for (int i = 0; i < 10; i++) {
            engine.processTick(1.0);
        }

        assertTrue(engine.getNonRenewableResources() < 1.0e12,
                "Industrial output must deplete non-renewable mineral/fossil stock");
        assertTrue(engine.getPersistentPollution() > 2.5e7,
                "Industrial capital production must generate persistent environmental pollution");
        assertFalse(Double.isNaN(engine.getPopulation()), "Population must not be NaN");
    }

    @Test
    @DisplayName("Nordhaus DICE: GDP growth produces emissions increasing temperature anomaly and climate damage")
    public void testNordhausDicePureDynamics() {
        NordhausDicePureEngine engine = new NordhausDicePureEngine();

        double initialGdp = engine.getGdp(); // 100.0
        double initialTemp = engine.getTemperatureAnomaly(); // 1.2 C

        // Run 5 annual ticks
        for (int i = 0; i < 5; i++) {
            engine.processTick(1.0);
        }

        assertTrue(engine.getTemperatureAnomaly() > initialTemp,
                "Economic production emissions must increase global temperature anomaly");
        assertTrue(engine.getGdp() > initialGdp,
                "Net economy must grow under baseline 2% technological progress");
    }

    @Test
    @DisplayName("NASA HANDY: Commoners extract nature to produce wealth consumed asymmetrically by elites, triggering famine bifurcation")
    public void testHandyNasaPureDynamics() {
        HandyNasaPureEngine engine = new HandyNasaPureEngine();

        assertEquals(100.0, engine.getCommonersX(), 1e-4);
        assertEquals(1.0, engine.getElitesY(), 1e-4);
        double initialNature = engine.getNatureX();

        // Run multi-decadal simulation (80 annual ticks) to evaluate non-linear dynamics
        for (int i = 0; i < 80; i++) {
            engine.processTick(1.0);
        }

        assertTrue(engine.getWealthK() > 0.0,
                "Commoner labor coupled with natural resources must accumulate wealth capital");
        assertTrue(engine.getNatureX() < initialNature,
                "Intensive labor and elite consumption must exert depletion drag on carrying capacity");
        assertTrue(engine.getCommonersX() > 0.0, "Commoner population must remain non-negative");
        assertTrue(engine.getElitesY() > 0.0, "Elite population must remain non-negative");
        assertFalse(Double.isNaN(engine.getCommonersX()), "Commoners population must not be NaN");
        assertFalse(Double.isNaN(engine.getElitesY()), "Elites population must not be NaN");
    }
}
