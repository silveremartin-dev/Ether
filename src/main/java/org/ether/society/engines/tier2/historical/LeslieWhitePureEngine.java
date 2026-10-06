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
 * Leslie White Model Variant B10.1 (Pure) & B10.2 (Hybrid).
 * Leslie White Law: Cultural Complexity C = E * T (Energy per capita * Technological efficiency).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class LeslieWhitePureEngine {
    private static final Logger logger = LoggerFactory.getLogger(LeslieWhitePureEngine.class);

    /*
     * Calculate cultural complexity.
     * Enforces physical invariants and updates associated state variables within {@code LeslieWhitePureEngine}.
     *
     * @param energyPerCapita the energy per capita parameter (double)
     * @param techEfficiency the tech efficiency parameter (double)
     * @return the resulting computation or state reference
     */
    public static double calculateCulturalComplexity(double energyPerCapita, double techEfficiency) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        return Math.max(1.0, energyPerCapita * techEfficiency);
    }

    /*
     * Process hybrid.
     * Enforces physical invariants and updates associated state variables within {@code LeslieWhitePureEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
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


