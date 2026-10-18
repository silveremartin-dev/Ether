/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for W.D. Hamilton & E.O. Wilson Kin Selection Theory,
 * Hamilton's Rule (r * B > C), and Altruistic Outgroup Military Filtering.
 */
public class KinSelectionHamiltonEngineTest {

    @Test
    @DisplayName("Hamilton's Rule correctly evaluates genetic relatedness vs altruism cost")
    public void testHamiltonRuleBoundaries() {
        // Full siblings (r = 0.5): Altruism favored if benefit B = 100 > cost C / 0.5 (C = 40)
        assertTrue(KinSelectionHamiltonEngine.checkHamiltonRule(0.5, 100.0, 40.0),
                "Hamilton's rule r*B > C must be true when 0.5 * 100 > 40");

        // First cousins (r = 0.125): Altruism rejected if cost C = 20 > 0.125 * 100 (12.5)
        assertFalse(KinSelectionHamiltonEngine.checkHamiltonRule(0.125, 100.0, 20.0),
                "Hamilton's rule r*B > C must be false when 0.125 * 100 <= 20");

        // Clonal / Identical twins (r = 1.0)
        assertTrue(KinSelectionHamiltonEngine.checkHamiltonRule(1.0, 50.0, 49.9),
                "Hamilton's rule must hold for r=1.0 when B > C");
    }

    @Test
    @DisplayName("Severe caloric scarcity triggers Hamilton outgroup hostility and increases military expenditure")
    public void testSevereScarcityTriggersOutgroupHostility() {
        H3Cell cell = new H3Cell(1001L, 45.0, 5.0);
        cell.setPopulation(1000);
        cell.setResourceWork(100.0);

        // Case 1: Plentiful food (food/pop = 500 / 1000 = 0.50 >= 0.05 threshold)
        cell.setFoodResource(500.0);
        KinSelectionHamiltonEngine.processHybrid(List.of(cell), 1.0);
        assertEquals(100.0, cell.getResourceWork(), 1e-6,
                "Under sufficient food resources, outgroup hostility remains inactive and military work unchanged");

        // Case 2: Severe famine (food/pop = 20 / 1000 = 0.02 < 0.05 threshold)
        // Expected work boost: 100.0 * (1.0 + 0.08 * dt) = 108.0 for dt = 1.0 year
        cell.setFoodResource(20.0);
        KinSelectionHamiltonEngine.processHybrid(List.of(cell), 1.0);
        assertEquals(108.0, cell.getResourceWork(), 1e-4,
                "Acute famine below 0.05 food/capita must trigger an exact 8%/yr military work expenditure surge");
    }
}
