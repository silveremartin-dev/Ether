/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Joseph Henrich Tasmanian Cultural Loss Engine Variant B26.1 (Pure) & B26.2 (Hybrid).
 * Henrich (2004) Law: Demographic size and connectivity determine cumulative technological complexity.
 * Isolated populations below critical threshold (N < 5000) lose complex technological skills over generations.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class TasmanianCulturalRegressionEngine {
    private static final Logger logger = LoggerFactory.getLogger(TasmanianCulturalRegressionEngine.class);

    /** Internal state: isolation population threshold (double). */
    private static double isolationPopulationThreshold = 5000.0;

    /**
     * Process hybrid.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double totalPop = 0.0;
        for (H3Cell c : cells) {
            if (c != null && c.getPopulation() != null && c.getPopulation() > 0) {
                totalPop += c.getPopulation();
            }
        }

        // Henrich (2004) law: demographic connectivity determines cumulative technological retention
        // Continuous sigmoidal transmission fidelity loss replacing piecewise linear cutoff at N = 5000
        double lossIntensity = 1.0 / (1.0 + Math.exp((totalPop - 2500.0) / 800.0));
        double lossRate = 0.04 * lossIntensity * deltaYears;
        if (lossRate > 1e-6) {
            for (H3Cell cell : cells) {
                if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;
                double tech = cell.getTechnologyLevelOr(1.0);
                if (tech > 1.0) {
                    cell.setTechnologyLevel(Math.max(1.0, tech - lossRate));
                }
            }
        }
    }

    /**
     * Get isolation population threshold.
     *
     * @return the resulting computation or state reference
     */
    public static double getIsolationPopulationThreshold() { return isolationPopulationThreshold; }

    public static void setIsolationPopulationThreshold(double threshold) { isolationPopulationThreshold = Math.max(100.0, threshold); }

    /**
     * Resets isolation population threshold for scenario isolation.
     */
    public static void resetState() {
        isolationPopulationThreshold = 5000.0;
    }
}


