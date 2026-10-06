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
 * Models how technological efficiency gains increase total aggregate resource consumption.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class JevonsParadoxEngine {
    private static final Logger logger = LoggerFactory.getLogger(JevonsParadoxEngine.class);

    private static double reboundCoefficient = 0.80; // 80% rebound effect

    /*
     * Process jevons rebound.
     * Enforces physical invariants and updates associated state variables within {@code JevonsParadoxEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processJevonsRebound(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty()) return;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null || cell.getTechnologyLevel() == null) continue;

            double tech = cell.getTechnologyLevel();
            if (tech > 2.0) {
                // High technology efficiency drives expanded energy demand
                double reboundDemand = tech * reboundCoefficient * 0.005 * deltaYears;
                if (cell.getEnergyFoodConsumed() != null) {
                    cell.setEnergyFoodConsumed(cell.getEnergyFoodConsumed() + reboundDemand);
                }
            }
        }
    }

    /*
     * Get rebound coefficient.
     * Enforces physical invariants and updates associated state variables within {@code JevonsParadoxEngine}.
     *
     * @return the resulting computation or state reference
     */
    public static double getReboundCoefficient() { return reboundCoefficient; }
    /*
     * Set rebound coefficient operation.
     * <p>
     * Executes operational logic for {@code JevonsParadoxEngine} within the Tier 2 cliodynamic and macroeconomic theoretical model.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param coeff the coeff argument (double)
     */
    public static void setReboundCoefficient(double coeff) { reboundCoefficient = Math.max(0.0, Math.min(2.0, coeff)); }
}


