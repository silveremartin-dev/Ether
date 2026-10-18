/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.core.dod.PluggableStatEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for {@link StatisticalReplayEngine}.
 * Verifies on-demand time-series reconstruction over recorded historical snapshots.
 */
class StatisticalReplayEngineTest {

    @Test
    @DisplayName("ReconstitutedSeries records data points and correctly calculates min, max, and mean")
    void testReconstitutedSeriesBasicStats() {
        StatisticalReplayEngine.ReconstitutedSeries series =
                new StatisticalReplayEngine.ReconstitutedSeries("test_pop", "Total Pop", "sum(population)", "souls");

        series.addPoint(10L, 100.0);
        series.addPoint(20L, 300.0);
        series.addPoint(30L, 200.0);

        assertEquals("test_pop", series.getFormulaId());
        assertEquals("Total Pop", series.getFormulaName());
        assertEquals(3, series.getTicks().size());
        assertEquals(100.0, series.getMin(), 1e-6);
        assertEquals(300.0, series.getMax(), 1e-6);
        assertEquals(200.0, series.getMean(), 1e-6);
    }

    @Test
    @DisplayName("reconstituteFromSnapshots computes formula trajectory over snapshot time range")
    void testReconstituteFromSnapshots() {
        PluggableStatEngine statEngine = new PluggableStatEngine();
        StatisticalReplayEngine replayEngine = new StatisticalReplayEngine(statEngine);

        NavigableMap<Long, List<H3Cell>> snapshots = new TreeMap<>();

        H3Cell c1 = new H3Cell(1L, 0.0, 0.0);
        c1.setPopulation(1000);
        snapshots.put(100L, List.of(c1));

        H3Cell c2 = new H3Cell(1L, 0.0, 0.0);
        c2.setPopulation(2500);
        snapshots.put(200L, List.of(c2));

        H3Cell c3 = new H3Cell(1L, 0.0, 0.0);
        c3.setPopulation(4000);
        snapshots.put(300L, List.of(c3));

        PluggableStatEngine.StatDefinition statDef =
                new PluggableStatEngine.StatDefinition("pop_stat", "Pop", "Demographics", "sum(population)", "people", "Total Population", true);

        StatisticalReplayEngine.ReconstitutedSeries series =
                replayEngine.reconstituteFromSnapshots(statDef, snapshots, 100L, 250L);

        // Only ticks 100 and 200 fall in [100, 250]
        assertEquals(2, series.getTicks().size());
        assertEquals(100L, series.getTicks().get(0));
        assertEquals(200L, series.getTicks().get(1));
        assertEquals(1000.0, series.getValues().get(0), 1e-6);
        assertEquals(2500.0, series.getValues().get(1), 1e-6);
    }
}
