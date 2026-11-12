/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.util.*;

/**
 * Trajectory extraction, sampling and parameter comparison helper for
 * {@link GroundTruthAuditReportBuilder}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
final class GroundTruthTrajectoryHelper {

    private GroundTruthTrajectoryHelper() {
        // Utility helper
    }

    static List<Integer> sampleYearsUniformly(java.util.Set<Integer> unionYears) {
        List<Integer> sorted = new ArrayList<>(unionYears);
        if (sorted.size() <= 80) {
            return sorted;
        }
        List<Integer> sampled = new ArrayList<>();
        int step = Math.max(1, sorted.size() / 80);
        for (int i = 0; i < sorted.size(); i += step) {
            sampled.add(sorted.get(i));
        }
        if (!sampled.contains(sorted.get(sorted.size() - 1))) {
            sampled.add(sorted.get(sorted.size() - 1));
        }
        return sampled;
    }

    static void addExtraSimulatedVariables(
            Map<String, String> variableMap,
            SimulationRunRecord runA,
            SimulationRunRecord runB,
            List<Integer> intersectionYears) {
        if (runA == null || runB == null || intersectionYears.isEmpty()) return;
        java.util.Set<String> existingKeys = new java.util.HashSet<>(variableMap.values());
        java.util.Set<String> candidateKeys = new java.util.LinkedHashSet<>();
        for (int yr : intersectionYears) {
            SimulationRunRecord.MetricSnapshot sA = runA.getSnapshotAt(yr);
            SimulationRunRecord.MetricSnapshot sB = runB.getSnapshotAt(yr);
            if (sA != null && sA.getMetricsMap() != null) candidateKeys.addAll(sA.getMetricsMap().keySet());
            if (sB != null && sB.getMetricsMap() != null) candidateKeys.addAll(sB.getMetricsMap().keySet());
        }
        for (String key : candidateKeys) {
            if (existingKeys.contains(key)) continue;
            if (Set.of("population", "regionalPopulation", "gdp", "gdpTotal", "regionalGdp", "energyCaptured",
                    "regionalEnergy", "gini", "giniIndex", "asabiyyah", "psiStressIndex", "conflict",
                    "lifeExpectancy", "educationLevel", "carbonFootprint", "systemInterdependence").contains(key)) {
                continue;
            }
            MetricDescriptor desc = MetricRegistry.getInstance().getDescriptor(key);
            String label = (desc != null && desc.getDisplayName() != null)
                    ? desc.getDisplayName() + " (" + key + ")"
                    : key;
            variableMap.put(label, key);
            existingKeys.add(key);
        }
    }

    static double extractFromTrajectory(
            GroundTruthAuditReportBuilder.ComparableTrajectory traj,
            String varKey,
            int year,
            HistoricalValidationKernel.InterpolationMethod method,
            Map<Integer, Double> regionalRasterSeries,
            boolean hasWindowBounds) {
        if (traj == null) return Double.NaN;
        if (traj.isGroundTruth()) {
            return (regionalRasterSeries != null && !regionalRasterSeries.isEmpty())
                    ? regionalRasterSeries.getOrDefault(year, Double.NaN)
                    : HistoricalValidationKernel.getInterpolatedBenchmarkValue(varKey, year, method);
        }
        if (traj.isPredictive()) {
            return HistoricalValidationKernel.getPredictiveBenchmarkValue(traj.runId(), varKey, year, method);
        }
        SimulationRunRecord run = traj.run();
        if (run == null || run.getTimeSeriesData().isEmpty()) return Double.NaN;

        SimulationRunRecord.MetricSnapshot snap = interpolateOrFloorSnapshot(run, year);
        if (snap == null) return Double.NaN;
        double v = GroundTruthAuditGenerator.extractValue(snap, varKey, year);
        if (Double.isNaN(v) || Double.isInfinite(v)) return Double.NaN;
        if ("milankovitchInsolation".equals(varKey) && v <= 0.0) return Double.NaN;
        return GroundTruthStatisticsKernel.normalizeSimulatedUnits(varKey, v);
    }

    static SimulationRunRecord.MetricSnapshot interpolateOrFloorSnapshot(SimulationRunRecord run, int year) {
        var ts = run.getTimeSeriesData();
        if (ts.containsKey(year)) return ts.get(year);
        Map.Entry<Integer, SimulationRunRecord.MetricSnapshot> floor = ts.floorEntry(year);
        Map.Entry<Integer, SimulationRunRecord.MetricSnapshot> ceil = ts.ceilingEntry(year);
        if (floor == null) return ceil != null ? ceil.getValue() : null;
        if (ceil == null) return floor.getValue();
        return Math.abs(year - floor.getKey()) <= Math.abs(ceil.getKey() - year) ? floor.getValue() : ceil.getValue();
    }

    static List<String> computeParameterDiffs(SimulationRunRecord baseline, SimulationRunRecord target) {
        if (baseline == null || target == null) return Collections.emptyList();
        List<String> diffs = new ArrayList<>();
        java.util.Set<String> allKeys = new java.util.LinkedHashSet<>(baseline.getParameterMatrix().keySet());
        allKeys.addAll(target.getParameterMatrix().keySet());
        for (String key : allKeys) {
            String vA = baseline.getParameterMatrix().getOrDefault(key, "OFF / N/A");
            String vB = target.getParameterMatrix().getOrDefault(key, "OFF / N/A");
            if (!vA.equalsIgnoreCase(vB)) {
                diffs.add(String.format("`%s` : `%s` ➔ `%s`", key, vA, vB));
            }
        }
        return diffs;
    }
}
