/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.tier2;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class StructuralDemographicBifurcationEngineTest {

    @Test
    @DisplayName("Verify that high structural stress (PSI > threshold) triggers Poisson dissipation jumps")
    void testCrisisJumpTriggering() {
        List<H3Cell> cells = new ArrayList<>();

        // Create high-stress cell: massive population, food deficit, ultra-high Gini (0.75), large capital
        H3Cell cell = new H3Cell(0x882681a339fffffL, 0.0, 0.0);
        cell.setPopulation(5000);
        cell.setFoodResource(200.0); // severe food deficit
        cell.setGiniIndex(0.75);
        cell.setResourceCapital(1000.0);
        cells.add(cell);

        // Deterministic RNG with high jump probability
        StructuralDemographicBifurcationEngine engine = new StructuralDemographicBifurcationEngine(
                3.0, 0.5, new Random(101)
        );

        // Run multi-step evolution
        for (int yr = 0; yr < 10; yr++) {
            engine.process(cells, 1.0);
        }

        assertTrue(engine.getTotalCrisesTriggered() > 0, "High PSI state must trigger at least one structural collapse jump.");
        assertTrue(cell.getGiniIndex() < 0.75, "Crisis jump must reduce inequality Gini (Great Leveler effect).");
        assertTrue(cell.getResourceCapital() < 1000.0, "Crisis jump must destroy physical capital.");
        assertTrue(cell.getPopulation() < 5000, "Crisis jump must induce excess mortality.");
    }

    @Test
    @DisplayName("Verify that low structural stress remains stable with zero crisis jumps")
    void testLowStressStability() {
        List<H3Cell> cells = new ArrayList<>();

        // Create stable agrarian cell: low population, food surplus, low Gini
        H3Cell cell = new H3Cell(0x882681a339fffffL, 0.0, 0.0);
        cell.setPopulation(300);
        cell.setFoodResource(5000.0);
        cell.setGiniIndex(0.25);
        cell.setResourceCapital(50.0);
        cells.add(cell);

        StructuralDemographicBifurcationEngine engine = new StructuralDemographicBifurcationEngine(
                5.0, 0.01, new Random(42)
        );

        for (int yr = 0; yr < 10; yr++) {
            engine.process(cells, 1.0);
        }

        assertEquals(0, engine.getTotalCrisesTriggered(), "Low PSI conditions must not trigger crisis jumps.");
        assertEquals(0.25f, cell.getGiniIndex(), 1e-4);
    }
}
