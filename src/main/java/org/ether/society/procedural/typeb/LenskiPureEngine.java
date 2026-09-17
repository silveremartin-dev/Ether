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
 * Gerhard Lenski Ecological-Evolutionary Model Variant B9.1 (Pure) & B9.2 (Hybrid).
 * Lenski Law: Gini inequality peaks in Agrarian societies and is modulated by technology stage.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class LenskiPureEngine {
    private static final Logger logger = LoggerFactory.getLogger(LenskiPureEngine.class);

    /**
     * Computes theoretical Lenski Gini coefficient based on technology stage (1.0 = Hunter-Gatherer, 3.0 = Agrarian, 5.0 = Informational).
     */
    public static double calculateLenskiGini(double techStage) {
        if (techStage <= 1.5) return 0.15; // Low inequality in Hunter-Gatherer societies
        if (techStage <= 3.5) return 0.65; // Peak inequality in Agrarian / Feudal societies
        return 0.35; // Redistribution in Industrial / Informational societies
    }

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getTechnologyLevel() == null) continue;
            double tech = cell.getTechnologyLevel();
            double theoreticalGini = calculateLenskiGini(tech);
            // Apply Lenski inequality curve to capital distribution
            if (cell.getResourceCapital() != null) {
                cell.setResourceCapital(cell.getResourceCapital() * (1.0 - theoreticalGini * 0.05 * deltaYears));
            }
        }
    }
}

