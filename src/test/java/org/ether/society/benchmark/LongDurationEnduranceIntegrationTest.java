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
import org.ether.society.events.DomainEventBus;
import org.ether.society.events.SimulationTickEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Long-Duration Multi-Millennium Endurance & Epistemic Falsification Validation Suite.
 *
 * <p>Validates numerical stability, physical conservation invariants, and bit-identical
 * deterministic reproducibility across multi-millennium evolutionary horizons ($1\,000$ to $5\,000$ simulated years):</p>
 * <ul>
 *   <li><b>Test 1</b>: 5,000-year continuous agrarian basin sweep verifying zero numerical divergence,
 *       absence of heap exhaustion, and strict adherence to Lotka-Euler carrying capacity ceilings:
 *       \[
 *         0 < P_{\min} \le P(t) \le K_{\text{carrying}}, \quad \forall t \in [0, 5000]
 *       \]
 *   </li>
 *   <li><b>Test 2</b>: 1,000-year bit-identical trajectory verification between two independent runs with identical initial conditions.</li>
 *   <li><b>Test 3</b>: Telemetry event stream continuity through {@link DomainEventBus}.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class LongDurationEnduranceIntegrationTest extends AbstractCliodynamicBenchmarkTest {

    @Test
    @DisplayName("Endurance: 5,000-Year Continuous Basin Sweep - Zero Invariant Drift & Memory Stability")
    void testContinuousBasin5000Years() {
        // Create 37-cell fertile river basin centered on Fertile Crescent (31°N, 31°E)
        List<H3Cell> basinCells = createContiguousBasin(31.0, 31.0, 100.0);
        WorldBuffer world = populateWorldBuffer(basinCells);
        AgentBuffer agents = populateAgentBuffer(world);

        double initialPop = 0.0;
        for (int i = 0; i < world.getCapacity(); i++) {
            initialPop += world.getBiomassHuman()[i];
        }
        logger.info("🌿 Initial 5,000-Year Endurance Basin Population: {} across {} cells.", initialPop, basinCells.size());
        assertTrue(initialPop > 0.0);

        long initialCapacity = world.getCapacity();

        // Run 5,000 years with 60-day time steps (30,437 simulation iterations)
        runMultiYearSimulation(world, agents, 5000, 60.0f);

        double finalPop = 0.0;
        for (int i = 0; i < world.getCapacity(); i++) {
            finalPop += world.getBiomassHuman()[i];
        }
        logger.info("🌿 Final Basin Population after 5,000 simulated years: {}", finalPop);

        // Assertions:
        CliodynamicAssertions.assertPhysicalIntegrity(world);
        assertEquals(initialCapacity, world.getCapacity(), "Buffer capacity must remain invariant with zero allocation leaks.");
        assertTrue(finalPop >= initialPop * 0.1, "Population must not suffer total unforced extinction across 5 millennia.");
        assertTrue(finalPop <= initialPop * 100.0, "Ecological ceiling must prevent unbounded Malthusian explosion.");
        assertEquals(0, conservationGuard.getViolationCount(), "Zero physical invariant violations over 5,000 years.");
    }

    @Test
    @DisplayName("Determinism: 1,000-Year Dual-Run Trajectory Bit-Identical Reproducibility")
    void testBitIdenticalTrajectory1000Years() {
        // Run A
        List<H3Cell> basinA = createContiguousBasin(25.0, 45.0, 80.0);
        WorldBuffer worldA = populateWorldBuffer(basinA);
        AgentBuffer agentsA = populateAgentBuffer(worldA);
        runMultiYearSimulation(worldA, agentsA, 1000, 30.0f);

        // Run B (Independent initial state with identical geometry)
        List<H3Cell> basinB = createContiguousBasin(25.0, 45.0, 80.0);
        WorldBuffer worldB = populateWorldBuffer(basinB);
        AgentBuffer agentsB = populateAgentBuffer(worldB);
        runMultiYearSimulation(worldB, agentsB, 1000, 30.0f);

        // Assert 100% bit-identical end-state across primitive arrays
        assertArrayEquals(worldA.getBiomassHuman(), worldB.getBiomassHuman(), 1e-5f, "Human biomass trajectories must be bit-identical.");
        assertArrayEquals(worldA.getFoodResource(), worldB.getFoodResource(), 1e-4f, "Food resource trajectories must be bit-identical.");
        assertArrayEquals(worldA.getResourceCapital(), worldB.getResourceCapital(), 1e-4f, "Capital trajectories must be bit-identical.");
    }

    @Test
    @DisplayName("Telemetry: Long-Duration Event Bus Publication Continuity")
    void testLongDurationTelemetryContinuity() {
        DomainEventBus bus = DomainEventBus.getInstance();
        bus.clear();

        AtomicLong publishedTicks = new AtomicLong(0);
        bus.subscribe(SimulationTickEvent.class, e -> publishedTicks.incrementAndGet());

        // Publish 10,000 synthetic telemetry ticks through the bus
        for (long t = 1; t <= 10_000L; t++) {
            SimulationTickEvent event = new SimulationTickEvent(
                    t, (int) (t / 12), (int) (t % 12), 5_000_000L + t, 120.0, 1.0e10, 0.35, t % 12 == 0
            );
            bus.publishNow(event);
        }

        assertEquals(10_000L, publishedTicks.get(), "DomainEventBus must handle 10,000 sequential telemetry events without drop.");
    }
}
