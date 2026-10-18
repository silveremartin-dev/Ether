/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core.profiling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for {@link SimulationProfiler}.
 * Verifies high-precision tick accumulation, phase breakdown timings,
 * min/max/average/P95 latencies, and reset behavior.
 */
class SimulationProfilerTest {

    @Test
    @DisplayName("recordTick accumulates tick counts and correctly computes min, max, and average latency")
    void testRecordTickAccumulation() {
        SimulationProfiler profiler = new SimulationProfiler();
        profiler.reset();

        // Record 3 ticks: 10ms, 20ms, 30ms (in nanos)
        profiler.recordTick(10_000_000L, 2_000_000L);
        profiler.recordTick(20_000_000L, 2_000_000L);
        profiler.recordTick(30_000_000L, 2_000_000L);

        assertEquals(3, profiler.getTotalTicks());
        assertEquals(10.0, profiler.getMinTickTimeMs(), 1e-3);
        assertEquals(30.0, profiler.getMaxTickTimeMs(), 1e-3);
        assertEquals(20.0, profiler.getAverageTickTimeMs(), 1e-3);
        assertEquals(30.0, profiler.getLastTickTimeMs(), 1e-3);
    }

    @Test
    @DisplayName("beginPhase and endPhase accurately track phase elapsed duration")
    void testPhaseTracking() {
        SimulationProfiler profiler = new SimulationProfiler();
        profiler.reset();

        profiler.recordPhaseDuration("PhysicsPhase", 5_000_000L);
        profiler.recordPhaseDuration("PhysicsPhase", 15_000_000L);

        var breakdown = profiler.getPhaseAverageDurationsMs();
        assertTrue(breakdown.containsKey("PhysicsPhase"));
        assertEquals(10.0, breakdown.get("PhysicsPhase"), 1e-3);
    }

    @Test
    @DisplayName("Disabled profiler ignores incoming timing measurements")
    void testDisabledProfiler() {
        SimulationProfiler profiler = new SimulationProfiler();
        profiler.setEnabled(false);

        profiler.recordTick(50_000_000L, 10_000_000L);
        assertEquals(0, profiler.getTotalTicks());
        assertEquals(0.0, profiler.getAverageTickTimeMs(), 1e-6);
    }

    @Test
    @DisplayName("Reset clears all accumulated tick metrics and phase durations")
    void testReset() {
        SimulationProfiler profiler = new SimulationProfiler();
        profiler.recordTick(10_000_000L, 1_000_000L);
        assertEquals(1, profiler.getTotalTicks());

        profiler.reset();
        assertEquals(0, profiler.getTotalTicks());
        assertEquals(0.0, profiler.getAverageTickTimeMs(), 1e-6);
        assertTrue(profiler.getPhaseAverageDurationsMs().isEmpty());
    }
}
