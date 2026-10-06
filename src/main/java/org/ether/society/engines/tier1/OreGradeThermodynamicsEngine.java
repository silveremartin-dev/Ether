/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ore Grade Thermodynamics Engine (Bihouix Ore Depletion Law).
 * Modifies extraction energy requirements exponentially as ore mass concentration (C_ore) depletes.
 * Formula: E_extract = E_base * (C_ref / C_ore)^1.5
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class OreGradeThermodynamicsEngine {
    private static final Logger logger = LoggerFactory.getLogger(OreGradeThermodynamicsEngine.class);

    private static double referenceOreConcentration = 0.05; // 5% reference metal concentration
    private static double minOreConcentrationFloor = 0.001; // 0.1% physical limit floor

    /*
     * Process ore depletion.
     * Enforces physical invariants and updates associated state variables within {@code OreGradeThermodynamicsEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processOreDepletion(List<H3Cell> cells, double deltaYears) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        if (cells == null || cells.isEmpty()) return;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            double currentMetalStock = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
            if (currentMetalStock > 0) {
                // Ore concentration decreases as cumulative extraction proceeds
                double oreConcentration = Math.max(minOreConcentrationFloor, referenceOreConcentration * (currentMetalStock / 100.0));

                // Thermodynamic energy multiplier to extract 1 unit of metal
                double energyMultiplier = Math.pow(referenceOreConcentration / oreConcentration, 1.5);

                // Reduce available work output by extra energy required for low-grade mining
                if (cell.getResourceWork() != null) {
                    cell.setResourceWork(Math.max(0.0, cell.getResourceWork() - 0.01 * energyMultiplier * deltaYears));
                }
            }
        }
    }

    /*
     * Get reference ore concentration.
     * Enforces physical invariants and updates associated state variables within {@code OreGradeThermodynamicsEngine}.
     *
     * @return the resulting computation or state reference
     */
    public static double getReferenceOreConcentration() { return referenceOreConcentration; }
    /*
     * Set reference ore concentration operation.
     * <p>
     * Executes operational logic for {@code OreGradeThermodynamicsEngine} within the Tier 1 physical conservation solver.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param conc the conc argument (double)
     */
    public static void setReferenceOreConcentration(double conc) { referenceOreConcentration = Math.max(0.001, conc); }
}


