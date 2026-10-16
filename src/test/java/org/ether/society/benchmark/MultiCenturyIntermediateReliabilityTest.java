/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.benchmark;

import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-Century Intermediate Reliability & Macro-Consistency Validation Suite.
 *
 * <p>Validates simulation engine stability, bio-energetic conservation, and demographic trajectory
 * across intermediate multi-century spans ($200$ to $300$ years) before macro-scenario deployment:</p>
 * <ul>
 *   <li><b>Test 1</b>: 200-year agrarian basin sweep (37 contiguous cells) verifying Euler-Lotka demographic bounds and zero numerical drift.</li>
 *   <li><b>Test 2</b>: 300-year isolated insular sweep (127 cells, Madagascar-scale) verifying closed ecological carrying capacity ceiling and non-explosion.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class MultiCenturyIntermediateReliabilityTest extends AbstractCliodynamicBenchmarkTest {

    @Test
    @DisplayName("Level 2: 200-Year Contiguous Basin Sweep - Euler-Lotka Growth & Invariant Stability")
    void testContiguousBasin200Years() {
        // Create 37-cell fertile river basin centered on 31°N, 31°E
        List<H3Cell> basinCells = createContiguousBasin(31.0, 31.0, 80.0);
        WorldBuffer world = populateWorldBuffer(basinCells);
        AgentBuffer agents = populateAgentBuffer(world);

        double initialPop = 0.0;
        for (int i = 0; i < world.getCapacity(); i++) {
            initialPop += world.getBiomassHuman()[i];
        }
        logger.info("🌿 Initial Basin Population: {} across {} cells.", initialPop, basinCells.size());
        assertTrue(initialPop > 0.0);

        // Run 200 years of monthly simulation steps (dt = 30 days)
        runMultiYearSimulation(world, agents, 200, 30.0f);

        double finalPop = 0.0;
        for (int i = 0; i < world.getCapacity(); i++) {
            finalPop += world.getBiomassHuman()[i];
        }
        logger.info("🌿 Final Basin Population after 200 years: {}", finalPop);

        // Assertions:
        CliodynamicAssertions.assertPhysicalIntegrity(world);
        CliodynamicAssertions.assertEulerLotkaPlausible(initialPop, finalPop, 200.0);
        assertTrue(finalPop >= initialPop * 0.5, "Population must maintain long-term stability and not suffer unforced total extinction.");
        assertEquals(0, conservationGuard.getViolationCount(), "Zero physical invariant violations over 200 years.");
    }

    @Test
    @DisplayName("Level 2: 300-Year Isolated Island Sweep - Carrying Capacity Ceiling & Bounded Allometry")
    void testIsolatedIsland300Years() {
        // Create 127-cell closed island centered on Madagascar (-18.7°S, 46.8°E)
        List<H3Cell> islandCells = createIsolatedIsland(-18.7, 46.8, 50.0);
        WorldBuffer world = populateWorldBuffer(islandCells);
        AgentBuffer agents = populateAgentBuffer(world);

        double initialPop = 0.0;
        for (int i = 0; i < world.getCapacity(); i++) {
            initialPop += world.getBiomassHuman()[i];
        }
        logger.info("🏝️ Initial Island Population: {} across {} cells.", initialPop, islandCells.size());

        // Run 300 years of bi-monthly simulation steps (dt = 60 days)
        runMultiYearSimulation(world, agents, 300, 60.0f);

        double finalPop = 0.0;
        for (int i = 0; i < world.getCapacity(); i++) {
            finalPop += world.getBiomassHuman()[i];
        }
        logger.info("🏝️ Final Island Population after 300 years: {}", finalPop);

        // Assertions:
        CliodynamicAssertions.assertPhysicalIntegrity(world);
        CliodynamicAssertions.assertEulerLotkaPlausible(initialPop, finalPop, 300.0);
        assertTrue(finalPop <= initialPop * 50.0, "Carrying capacity bounds must prevent explosive hyper-inflation.");
        assertEquals(0, conservationGuard.getViolationCount(), "Zero physical invariant violations over 300 years.");
    }
}
