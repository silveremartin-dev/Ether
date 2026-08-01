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
 * Leslie White Model Variant B10.1 (Pure) & B10.2 (Hybrid).
 * Leslie White Law: Cultural Complexity C = E * T (Energy per capita * Technological efficiency).
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class LeslieWhitePureEngine {
    private static final Logger logger = LoggerFactory.getLogger(LeslieWhitePureEngine.class);

    public static double calculateCulturalComplexity(double energyPerCapita, double techEfficiency) {
        return Math.max(1.0, energyPerCapita * techEfficiency);
    }

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            double energy = cell.getEnergyFoodConsumed() != null ? cell.getEnergyFoodConsumed() : 1.0;
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;

            double culturalComplexity = calculateCulturalComplexity(energy / cell.getPopulation(), tech);
            // Cultural complexity boosts technological progress speed
            cell.setTechnologyLevel(tech + culturalComplexity * 0.0001 * deltaYears);
        }
    }
}
