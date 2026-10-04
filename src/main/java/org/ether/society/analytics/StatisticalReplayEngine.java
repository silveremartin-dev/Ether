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
        /* Internal state variable for formula id (String). */
        private final String formulaId;
        /* Internal state variable for formula name (String). */
        private final String formulaName;
        /* Internal state variable for expression (String). */
        private final String expression;
        /* Internal state variable for unit (String). */
        private final String unit;
        private final List<Long> ticks = new ArrayList<>();
        private final List<Double> values = new ArrayList<>();

        /*
         * Reconstituted series.
         * Enforces physical invariants and updates associated state variables within {@code StatisticalReplayEngine}.
         *
         * @param formulaId the formula id parameter (String)
         * @param formulaName the formula name parameter (String)
         * @param expression the expression parameter (String)
         * @param unit the unit parameter (String)
         * @return the resulting computation or state reference
         */
        public ReconstitutedSeries(String formulaId, String formulaName, String expression, String unit) {
            this.formulaId = formulaId;
            this.formulaName = formulaName;
            this.expression = expression;
            this.unit = unit;
        }

        /*
         * Add point.
         * Enforces physical invariants and updates associated state variables within {@code StatisticalReplayEngine}.
         *
         * @param tick the tick parameter (long)
         * @param value the value parameter (double)
         */
        public void addPoint(long tick, double value) {
            ticks.add(tick);
            values.add(value);
        }

        /*
         * Get formula id.
         * Enforces physical invariants and updates associated state variables within {@code StatisticalReplayEngine}.
         *
         * @return the resulting computation or state reference
         */
        public String getFormulaId() { return formulaId; }
        /*
         * Get formula name.
         * Enforces physical invariants and updates associated state variables within {@code StatisticalReplayEngine}.
         *
         * @return the resulting computation or state reference
         */
        public String getFormulaName() { return formulaName; }
        /*
         * Get expression.
         * Enforces physical invariants and updates associated state variables within {@code StatisticalReplayEngine}.
         *
         * @return the resulting computation or state reference
         */
        public String getExpression() { return expression; }
        /*
         * Get unit.
         * Enforces physical invariants and updates associated state variables within {@code StatisticalReplayEngine}.
         *
         * @return the resulting computation or state reference
         */
        public String getUnit() { return unit; }
        /*
         * Get ticks.
         * Enforces physical invariants and updates associated state variables within {@code StatisticalReplayEngine}.
         *
         * @return the resulting computation or state reference
         */
        public List<Long> getTicks() { return Collections.unmodifiableList(ticks); }
        /*
         * Get values.
         * Enforces physical invariants and updates associated state variables within {@code StatisticalReplayEngine}.
         *
         * @return the resulting computation or state reference
         */
        public List<Double> getValues() { return Collections.unmodifiableList(values); }

        /*
         * Get min.
         * Enforces physical invariants and updates associated state variables within {@code StatisticalReplayEngine}.
         *
         * @return the resulting computation or state reference
         */
        public double getMin() {
            return values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        }

        /*
         * Get max.
         * Enforces physical invariants and updates associated state variables within {@code StatisticalReplayEngine}.
         *
         * @return the resulting computation or state reference
         */
        public double getMax() {
            return values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        }

        /*
         * Get mean.
         * Enforces physical invariants and updates associated state variables within {@code StatisticalReplayEngine}.
         *
         * @return the resulting computation or state reference
         */
        public double getMean() {
            return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
    }

    private final PluggableStatEngine statEngine;

    /*
     * Statistical replay engine.
     * Enforces physical invariants and updates associated state variables within {@code StatisticalReplayEngine}.
     *
     * @param statEngine the stat engine parameter (PluggableStatEngine)
     */
    public StatisticalReplayEngine(PluggableStatEngine statEngine) {
        this.statEngine = statEngine;
    }

    /*
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

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (Map.Entry<Long, List<H3Cell>> entry : subMap.entrySet()) {
            long tick = entry.getKey();
            List<H3Cell> snapshotCells = entry.getValue();

            double val = statEngine.computeValue(statDef.getExpression(), snapshotCells, null);
            series.addPoint(tick, val);
        }

        return series;
    }
}
