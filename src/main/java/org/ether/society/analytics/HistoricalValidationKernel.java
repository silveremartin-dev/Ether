/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Historical Telemetry Validation & Model Best-Fit Kernel.
 * Compares simulated trajectories against empirical historical dataset baselines (10,000 BCE - 2026 CE)
 * to compute Root Mean Square Error (RMSE) and R^2 coefficient of determination.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class HistoricalValidationKernel {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalValidationKernel.class);

    /** Empirical Historical World Population Dataset (Year -> Population in Millions) */
    private static final Map<Integer, Double> HISTORICAL_WORLD_POPULATION = new HashMap<>();

    static {
        HISTORICAL_WORLD_POPULATION.put(-10000, 4.0);
        HISTORICAL_WORLD_POPULATION.put(-5000, 5.0);
        HISTORICAL_WORLD_POPULATION.put(-1000, 50.0);
        HISTORICAL_WORLD_POPULATION.put(1, 170.0);
        HISTORICAL_WORLD_POPULATION.put(1000, 265.0);
        HISTORICAL_WORLD_POPULATION.put(1500, 425.0);
        HISTORICAL_WORLD_POPULATION.put(1800, 900.0);
        HISTORICAL_WORLD_POPULATION.put(1900, 1650.0);
        HISTORICAL_WORLD_POPULATION.put(1950, 2525.0);
        HISTORICAL_WORLD_POPULATION.put(2000, 6127.0);
        HISTORICAL_WORLD_POPULATION.put(2026, 8100.0);
    }

    /**
     * Calculates Root Mean Square Error (RMSE) between simulated trajectory map and historical benchmark.
     */
    public static double calculateRmse(Map<Integer, Double> simulatedData) {
        if (simulatedData == null || simulatedData.isEmpty()) return Double.MAX_VALUE;

        double sumSquaredErrors = 0.0;
        int count = 0;

        for (Map.Entry<Integer, Double> entry : HISTORICAL_WORLD_POPULATION.entrySet()) {
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
     * Calculates R-squared (Coefficient of Determination) best fit score [0.0 ... 1.0].
     */
    public static double calculateRSquared(Map<Integer, Double> simulatedData) {
        if (simulatedData == null || simulatedData.isEmpty()) return 0.0;

        double meanObserved = HISTORICAL_WORLD_POPULATION.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double ssTot = 0.0;
        double ssRes = 0.0;

        for (Map.Entry<Integer, Double> entry : HISTORICAL_WORLD_POPULATION.entrySet()) {
            int year = entry.getKey();
            if (simulatedData.containsKey(year)) {
                double observed = entry.getValue();
                double simulated = simulatedData.get(year);
                ssTot += Math.pow(observed - meanObserved, 2.0);
                ssRes += Math.pow(observed - simulated, 2.0);
            }
        }

        if (ssTot == 0.0) return 1.0;
        return Math.max(0.0, 1.0 - (ssRes / ssTot));
    }

    public static Map<Integer, Double> getHistoricalWorldPopulation() {
        return HISTORICAL_WORLD_POPULATION;
    }
}
