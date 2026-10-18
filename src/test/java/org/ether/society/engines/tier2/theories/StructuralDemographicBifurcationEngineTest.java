/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Peter Turchin & Jack Goldstone's
 * Structural-Demographic Theory (SDT) and Poisson Catastrophe Jump Engine.
 */
public class StructuralDemographicBifurcationEngineTest {

    @Test
    @DisplayName("Stable low-inequality and food-abundant cells remain below critical PSI threshold with 0 crises")
    public void testSubCriticalEquilibriumNoCrisis() {
        StructuralDemographicBifurcationEngine engine = new StructuralDemographicBifurcationEngine(10.0, 0.05, new Random(42));

        H3Cell cell = new H3Cell(1001L, 45.0, 5.0);
        cell.setPopulation(1000);
        cell.setFoodResource(50000.0); // Extreme abundance: requiredFood (2000) / 50000 = 0.04 -> MMP floored at 0.5
        cell.setGiniIndex(0.25);       // Low inequality
        cell.setResourceCapital(100.0); // Moderate capital

        engine.process(List.of(cell), 1.0);

        assertEquals(0, engine.getTotalCrisesTriggered(), "Subcritical cell must not experience Poisson catastrophe jump");
        assertEquals(0.25, cell.getGiniIndex(), 1e-4, "Gini must remain unchanged when no crisis occurs");
        assertEquals(1000, cell.getPopulation(), "Population must remain intact when no crisis occurs");
        assertTrue(engine.getMeanPlanetaryPSI() < 10.0, "Mean PSI must remain below critical threshold");
    }

    @Test
    @DisplayName("Extreme elite overproduction and mass immiseration triggers Scheidel Great Leveler collapse jump")
    public void testSuperCriticalBifurcationJump() {
        // Deterministic PRNG seeded to guarantee jump under high hazard
        StructuralDemographicBifurcationEngine engine = new StructuralDemographicBifurcationEngine(2.0, 0.99, new Random(1));

        H3Cell stressedCell = new H3Cell(1002L, 45.0, 5.0);
        stressedCell.setPopulation(10000);
        stressedCell.setFoodResource(100.0);   // Severe famine: requiredFood (20000) / 100 = 200 -> MMP = 200
        stressedCell.setGiniIndex(0.85);        // Extreme elite oligarchic concentration
        stressedCell.setResourceCapital(2000.0); // Enormous elite surplus -> EMP = 20 * (0.85^2 * 3) = 43.35
                                                // SF = 2.5 -> PSI = 200 * 43.35 * 2.5 = 21,675 >> 2.0

        int initialPop = stressedCell.getPopulation();
        double initialCapital = stressedCell.getResourceCapital();
        double initialGini = stressedCell.getGiniIndex();

        engine.process(List.of(stressedCell), 1.0);

        assertEquals(1, engine.getTotalCrisesTriggered(), "Catastrophe jump must fire under extreme supercritical PSI");
        
        // Assert Walter Scheidel's 'The Great Leveler' inequality and capital dissipation
        assertEquals(initialGini * 0.70, stressedCell.getGiniIndex(), 1e-3,
                "Crisis jump must compress Gini inequality index by exactly 30% (elite estate redistribution)");
        assertEquals(initialCapital * 0.75, stressedCell.getResourceCapital(), 1e-3,
                "Crisis jump must destroy 25% of elite physical capital stock (warfare/fires)");
        assertEquals((int) (initialPop * 0.85), stressedCell.getPopulation(),
                "Crisis jump must incur 15% excess mortality shock from sociopolitical breakdown");
    }
}
