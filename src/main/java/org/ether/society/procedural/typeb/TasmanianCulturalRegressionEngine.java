/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

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
 * @version 4.0.0
 */
public class TasmanianCulturalRegressionEngine {
    private static final Logger logger = LoggerFactory.getLogger(TasmanianCulturalRegressionEngine.class);

    private static double isolationPopulationThreshold = 5000.0;

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            double pop = cell.getPopulation();
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;

            // Henrich loss condition: small isolated populations suffer technology regression
            if (pop < isolationPopulationThreshold && tech > 1.0) {
                double lossRate = 0.01 * (1.0 - pop / isolationPopulationThreshold) * deltaYears;
                cell.setTechnologyLevel(Math.max(1.0, tech - lossRate));
            }
        }
    }

    public static double getIsolationPopulationThreshold() { return isolationPopulationThreshold; }
}
