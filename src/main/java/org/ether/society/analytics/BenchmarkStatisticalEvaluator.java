/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <h1>BenchmarkStatisticalEvaluator</h1>
 * <p>
 * Statistical verification and goodness-of-fit engine (RMSE, R-squared, Pearson correlation r)
 * comparing simulated cliodynamic trajectories against empirical historical ground truth (HYDE 3.4, Maddison, EPICA).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class BenchmarkStatisticalEvaluator {

    private BenchmarkStatisticalEvaluator() {
        // Utility class
    }

    /**
     * Calculates Root-Mean-Square Error (RMSE) between simulated and empirical time-series.
     *
     * @param simulatedData simulated trajectory
     * @param benchmarkData empirical benchmark data
     * @return RMSE value
     */
    public static double calculateRmse(Map<Integer, Double> simulatedData, Map<Integer, Double> benchmarkData) {
        if (simulatedData == null || simulatedData.isEmpty() || benchmarkData == null || benchmarkData.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double sumSquaredErrors = 0.0;
        int count = 0;

        for (Map.Entry<Integer, Double> entry : benchmarkData.entrySet()) {
            int year = entry.getKey();
            if (simulatedData.containsKey(year)) {
                double observed = entry.getValue();
                double simulated = simulatedData.get(year);
                sumSquaredErrors += Math.pow(simulated - observed, 2.0);
                count++;
            }
        }

        if (count == 0) return Double.MAX_VALUE;
        return Math.sqrt(sumSquaredErrors / count);
    }

    /**
     * Calculates coefficient of determination \(R^2\) between simulated and empirical time-series.
     *
     * @param simulatedData simulated trajectory
     * @param benchmarkData empirical benchmark data
     * @return \(R^2\) score in $[0.0, 1.0]$
     */
    public static double calculateRSquared(Map<Integer, Double> simulatedData, Map<Integer, Double> benchmarkData) {
        if (simulatedData == null || simulatedData.isEmpty() || benchmarkData == null || benchmarkData.isEmpty()) {
            return 0.0;
        }

        double meanObserved = benchmarkData.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double ssTot = 0.0;
        double ssRes = 0.0;
        int count = 0;

        for (Map.Entry<Integer, Double> entry : benchmarkData.entrySet()) {
            int year = entry.getKey();
            if (simulatedData.containsKey(year)) {
                double observed = entry.getValue();
                double simulated = simulatedData.get(year);
                ssTot += Math.pow(observed - meanObserved, 2.0);
                ssRes += Math.pow(observed - simulated, 2.0);
                count++;
            }
        }

        if (count == 0 || ssTot == 0.0) return 1.0;
        return Math.max(0.0, 1.0 - (ssRes / ssTot));
    }

    /**
     * Calculates Pearson correlation coefficient \(r \in [-1.0, 1.0]\).
     *
     * @param simulatedData simulated trajectory
     * @param benchmarkData empirical benchmark data
     * @return Pearson correlation r
     */
    public static double calculatePearsonCorrelation(Map<Integer, Double> simulatedData, Map<Integer, Double> benchmarkData) {
        if (simulatedData == null || benchmarkData == null) return 0.0;
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : benchmarkData.entrySet()) {
            int year = entry.getKey();
            if (simulatedData.containsKey(year)) {
                x.add(simulatedData.get(year));
                y.add(entry.getValue());
            }
        }
        int n = x.size();
        if (n < 2) return 0.0;
        double meanX = x.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double meanY = y.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double num = 0.0, denX = 0.0, denY = 0.0;
        for (int i = 0; i < n; i++) {
            double dx = x.get(i) - meanX;
            double dy = y.get(i) - meanY;
            num += dx * dy;
            denX += dx * dx;
            denY += dy * dy;
        }
        if (denX <= 0.0 || denY <= 0.0) return 0.0;
        return num / Math.sqrt(denX * denY);
    }
}
