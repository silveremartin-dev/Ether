/*
 * Ether - Human Society Simulation
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * MIT License
 */
package org.ether.society.analytics;

import org.ether.society.core.dod.PluggableStatEngine;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Replay & Statistical Reconstitution Engine.
 * Allows on-demand historical reconstruction of statistical metrics over recorded world snapshots,
 * eliminating the need to permanently store heavy statistical series in memory during simulation.
 *
 * @author Silvere Martin-Michiellot
 */
public class StatisticalReplayEngine {
    private static final Logger logger = LoggerFactory.getLogger(StatisticalReplayEngine.class);

    public static class ReconstitutedSeries {
        private final String formulaId;
        private final String formulaName;
        private final String expression;
        private final String unit;
        private final List<Long> ticks = new ArrayList<>();
        private final List<Double> values = new ArrayList<>();

        public ReconstitutedSeries(String formulaId, String formulaName, String expression, String unit) {
            this.formulaId = formulaId;
            this.formulaName = formulaName;
            this.expression = expression;
            this.unit = unit;
        }

        public void addPoint(long tick, double value) {
            ticks.add(tick);
            values.add(value);
        }

        public String getFormulaId() { return formulaId; }
        public String getFormulaName() { return formulaName; }
        public String getExpression() { return expression; }
        public String getUnit() { return unit; }
        public List<Long> getTicks() { return Collections.unmodifiableList(ticks); }
        public List<Double> getValues() { return Collections.unmodifiableList(values); }

        public double getMin() {
            return values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        }

        public double getMax() {
            return values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        }

        public double getMean() {
            return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
    }

    private final PluggableStatEngine statEngine;

    public StatisticalReplayEngine(PluggableStatEngine statEngine) {
        this.statEngine = statEngine;
    }

    /**
     * Reconstructs a statistical time-series for a given formula across recorded snapshots.
     */
    public ReconstitutedSeries reconstituteFromSnapshots(
            PluggableStatEngine.StatDefinition statDef,
            NavigableMap<Long, List<H3Cell>> snapshots,
            long startTick,
            long endTick) {

        ReconstitutedSeries series = new ReconstitutedSeries(
                statDef.getId(), statDef.getName(), statDef.getExpression(), statDef.getUnit());

        if (snapshots == null || snapshots.isEmpty()) {
            return series;
        }

        NavigableMap<Long, List<H3Cell>> subMap = snapshots.subMap(startTick, true, endTick, true);
        logger.info("Reconstructing statistical series '{}' over {} snapshots (ticks {} to {})",
                statDef.getName(), subMap.size(), startTick, endTick);

        for (Map.Entry<Long, List<H3Cell>> entry : subMap.entrySet()) {
            long tick = entry.getKey();
            List<H3Cell> snapshotCells = entry.getValue();

            double val = statEngine.computeValue(statDef.getExpression(), snapshotCells, null);
            series.addPoint(tick, val);
        }

        return series;
    }
}
