/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Jevons Paradox & Rebound Effect Engine (Jancovici & Bihouix Rebound Law).
 * Models how technological efficiency gains increase total aggregate resource consumption (William Stanley Jevons, 1865; Alcott, 2005).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class JevonsParadoxEngine {
    private static final Logger logger = LoggerFactory.getLogger(JevonsParadoxEngine.class);

    private static double reboundCoefficient = 0.80; // 80% rebound effect

    /**
     * Process Jevons rebound across spatial cells with continuous efficiency activation.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processJevonsRebound(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            double tech = cell.getTechnologyLevelOr(0.0);
            // Continuous technology efficiency factor eta_eff in [0, 1]
            // Replaces knife-edge `tech > 2.0` with continuous Hill function (K=2.0, n=2)
            double techEfficiencyFactor = (tech * tech) / (4.0 + tech * tech);
            if (techEfficiencyFactor > 0.05) {
                // High technology efficiency drives expanded aggregate physical energy demand
                double reboundDemand = tech * reboundCoefficient * techEfficiencyFactor * 0.005 * deltaYears;

                // Boost electrical/mechanical capacity in the cell (industrial exergy rebound)
                double habitatKw = cell.getHabitatEnergyKwOr(0.0);
                cell.setHabitatEnergyKw(habitatKw + reboundDemand * 100.0);

                // Biologically bounded somatic food intake: human metabolic limits (~2500 kcal/day)
                // prevent runaway somatic calorie consumption while preserving legacy compatibility
                double currentFoodEnergy = cell.getEnergyFoodConsumedOr(0.0);
                double maxMetabolicFood = Math.max(10.0, cell.getPopulationOr(100) * 1.5);
                cell.setEnergyFoodConsumed(Math.min(maxMetabolicFood, currentFoodEnergy + reboundDemand));
            }
        }
    }

    /**
     * Get rebound coefficient.
     *
     * @return the resulting computation or state reference
     */
    public static double getReboundCoefficient() { return reboundCoefficient; }

    /**
     * Set rebound coefficient operation.
     *
     * @param coeff the coeff argument (double)
     */
    public static void setReboundCoefficient(double coeff) { reboundCoefficient = Math.max(0.0, Math.min(2.0, coeff)); }

    /**
     * Resets rebound coefficient for scenario isolation.
     */
    public static void resetState() {
        reboundCoefficient = 0.80;
    }
}
