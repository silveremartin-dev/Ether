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
 * Nordhaus DICE Hybrid Model Variant B8.2 (Nordhaus DICE + Ether H3 Grid Integration).
 * Injects Nordhaus quadratic climate damage function D(T_cell) directly into Ether H3 cell capital.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class NordhausDiceHybridEngine {
    private static final Logger logger = LoggerFactory.getLogger(NordhausDiceHybridEngine.class);

    public static void processPlugin(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getTemperature() == null) continue;

            double temp = cell.getTemperature();
            double tempAnomaly = Math.max(0.0, temp - 15.0); // Anomaly relative to 15°C baseline
            double damageFraction = Math.min(0.50, 0.00236 * Math.pow(tempAnomaly, 2.0));

            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            cell.setResourceCapital(capital * (1.0 - damageFraction * deltaYears));
        }
    }
}
