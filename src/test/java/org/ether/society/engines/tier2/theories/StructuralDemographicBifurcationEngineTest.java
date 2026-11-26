/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Peter Turchin & Jack Goldstone's
 * Structural-Demographic Theory (SDT) and Poisson Catastrophe Jump Engine.
 */
public class StructuralDemographicBifurcationEngineTest {

    @Test
    @DisplayName("Stable low-inequality and food-abundant cells remain below critical PSI threshold with 0 crises")
    public void testSubCriticalEquilibriumNoCrisis() {
        StructuralDemographicBifurcationEngine engine = new StructuralDemographicBifurcationEngine(10.0, 0.05, 42L);

        WorldBuffer buffer = new WorldBuffer(1);
        buffer.getHumanPopulation()[0] = 1000.0f;
        buffer.getFoodResource()[0] = 50000.0f;
        buffer.getGiniIndex()[0] = 0.25f;
        buffer.getResourceCapital()[0] = 100.0f;

        engine.tick(buffer, 1.0, 12.0);

        assertEquals(0, engine.getTotalCrisesTriggered(), "Subcritical cell must not experience Poisson catastrophe jump");
        assertEquals(0.25f, buffer.getGiniIndex()[0], 1e-4f, "Gini must remain unchanged when no crisis occurs");
        assertEquals(1000.0f, buffer.getHumanPopulation()[0], 1e-3f, "Population must remain intact when no crisis occurs");
        assertTrue(engine.getMeanPlanetaryPSI() < 10.0, "Mean PSI must remain below critical threshold");
    }

    @Test
    @DisplayName("Extreme elite overproduction and mass immiseration triggers Scheidel Great Leveler collapse jump")
    public void testSuperCriticalBifurcationJump() {
        StructuralDemographicBifurcationEngine engine = new StructuralDemographicBifurcationEngine(2.0, 0.99, 1L);

        WorldBuffer buffer = new WorldBuffer(1);
        buffer.getHumanPopulation()[0] = 300000.0f; // High density (~169 hab/km2 on H3 Res-4 hex) triggering supercritical MMP
        buffer.getFoodResource()[0] = 100.0f;
        buffer.getGiniIndex()[0] = 0.85f;
        buffer.getResourceCapital()[0] = 2000.0f;

        float initialPop = buffer.getHumanPopulation()[0];
        float initialCapital = buffer.getResourceCapital()[0];
        float initialGini = buffer.getGiniIndex()[0];

        engine.tick(buffer, 1.0, 12.0);

        assertEquals(1, engine.getTotalCrisesTriggered(), "Catastrophe jump must fire under extreme supercritical PSI");

        // Assert Walter Scheidel's 'The Great Leveler' inequality and capital dissipation
        assertEquals(initialGini * 0.70f, buffer.getGiniIndex()[0], 1e-3f,
                "Crisis jump must compress Gini inequality index by 30% (elite estate redistribution)");
        assertEquals(initialCapital * 0.75f, buffer.getResourceCapital()[0], 1e-2f,
                "Crisis jump must destroy 25% of elite physical capital stock (warfare/fires)");
        assertEquals(initialPop * 0.85f, buffer.getHumanPopulation()[0], 1.0f,
                "Crisis jump must incur 15% excess mortality shock from sociopolitical breakdown");
    }
}
