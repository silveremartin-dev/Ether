/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.util.*;

/**
 * Analytical engine computing differences, divergence points, and parameter impact explanations.
 */
public class RootCauseAnalyzer {

    public static class MetricDelta {
        private final String metricName;
        private final double baselineValue;
        private final double targetValue;
        private final double absoluteChange;
        private final double percentageChange;

        public MetricDelta(String metricName, double baselineValue, double targetValue) {
            this.metricName = metricName;
            this.baselineValue = baselineValue;
            this.targetValue = targetValue;
            this.absoluteChange = targetValue - baselineValue;
            this.percentageChange = baselineValue != 0 ? (absoluteChange / baselineValue) * 100.0 : 0.0;
        }

        public String getMetricName() { return metricName; }
        public double getBaselineValue() { return baselineValue; }
        public double getTargetValue() { return targetValue; }
        public double getAbsoluteChange() { return absoluteChange; }
        public double getPercentageChange() { return percentageChange; }
    }

    public static class ComparisonResult {
        private final SimulationRunRecord baselineRun;
        private final SimulationRunRecord targetRun;
        private final int divergenceYear;
        private final List<MetricDelta> finalDeltas;
        private final List<String> keyDifferences;
        private final String primaryRootCauseExplanation;

        public ComparisonResult(SimulationRunRecord baselineRun, SimulationRunRecord targetRun,
                                int divergenceYear, List<MetricDelta> finalDeltas,
                                List<String> keyDifferences, String primaryRootCauseExplanation) {
            this.baselineRun = baselineRun;
            this.targetRun = targetRun;
            this.divergenceYear = divergenceYear;
            this.finalDeltas = finalDeltas;
            this.keyDifferences = keyDifferences;
            this.primaryRootCauseExplanation = primaryRootCauseExplanation;
        }

        public SimulationRunRecord getBaselineRun() { return baselineRun; }
        public SimulationRunRecord getTargetRun() { return targetRun; }
        public int getDivergenceYear() { return divergenceYear; }
        public List<MetricDelta> getFinalDeltas() { return finalDeltas; }
        public List<String> getKeyDifferences() { return keyDifferences; }
        public String getPrimaryRootCauseExplanation() { return primaryRootCauseExplanation; }
    }

    public ComparisonResult compareRuns(SimulationRunRecord baseline, SimulationRunRecord target) {
        if (baseline == null || target == null) {
            throw new IllegalArgumentException("Baseline and target runs must not be null.");
        }

        // 1. Identify parameter differences
        List<String> keyDifferences = new ArrayList<>();
        Set<String> allKeys = new LinkedHashSet<>(baseline.getParameterMatrix().keySet());
        allKeys.addAll(target.getParameterMatrix().keySet());

        for (String key : allKeys) {
            String valA = baseline.getParameterMatrix().getOrDefault(key, "N/A");
            String valB = target.getParameterMatrix().getOrDefault(key, "N/A");
            if (!valA.equalsIgnoreCase(valB)) {
                keyDifferences.add(String.format("%s : '%s' ➔ '%s'", key, valA, valB));
            }
        }

        // 2. Find divergence year T_divergence (where population or tech differs by > 5%)
        int divergenceYear = -1;
        TreeMap<Integer, SimulationRunRecord.MetricSnapshot> tsA = baseline.getTimeSeriesData();
        TreeMap<Integer, SimulationRunRecord.MetricSnapshot> tsB = target.getTimeSeriesData();

        for (int year : tsA.keySet()) {
            SimulationRunRecord.MetricSnapshot sA = tsA.get(year);
            SimulationRunRecord.MetricSnapshot sB = tsB.get(year);
            if (sA != null && sB != null && sA.getPopulation() > 0) {
                double popDiffPct = Math.abs((double) (sB.getPopulation() - sA.getPopulation()) / sA.getPopulation()) * 100.0;
                if (popDiffPct >= 5.0 && divergenceYear == -1) {
                    divergenceYear = year;
                    break;
                }
            }
        }

        // 3. Calculate deltas at final common year
        int finalYear = Math.min(baseline.getEndYear(), target.getEndYear());
        SimulationRunRecord.MetricSnapshot finalA = baseline.getSnapshotAt(finalYear);
        SimulationRunRecord.MetricSnapshot finalB = target.getSnapshotAt(finalYear);

        List<MetricDelta> deltas = new ArrayList<>();
        if (finalA != null && finalB != null) {
            deltas.add(new MetricDelta("Population Totale", finalA.getPopulation(), finalB.getPopulation()));
            deltas.add(new MetricDelta("Réserves Alimentaires", finalA.getFood(), finalB.getFood()));
            deltas.add(new MetricDelta("Niveau Technologique Moyen", finalA.getAvgTech(), finalB.getAvgTech()));
            deltas.add(new MetricDelta("Indice de Stabilité", finalA.getStability(), finalB.getStability()));
        }

        // 4. Generate root cause explanation string
        StringBuilder explanation = new StringBuilder();
        if (divergenceYear != -1) {
            explanation.append(String.format("Divergence majeure détectée à l'An %d. ", divergenceYear));
        } else {
            explanation.append("Les trajectoires demeurent quasiment parallèles sur toute la durée. ");
        }

        if (!keyDifferences.isEmpty()) {
            explanation.append("Origine principale des écarts : ").append(String.join(", ", keyDifferences)).append(". ");
        }

        MetricDelta popDelta = deltas.stream().filter(d -> d.getMetricName().contains("Population")).findFirst().orElse(null);
        if (popDelta != null) {
            explanation.append(String.format("Impact final à l'An %d : %+.1f%% sur la population.", finalYear, popDelta.getPercentageChange()));
        }

        return new ComparisonResult(baseline, target, divergenceYear, deltas, keyDifferences, explanation.toString());
    }
}
