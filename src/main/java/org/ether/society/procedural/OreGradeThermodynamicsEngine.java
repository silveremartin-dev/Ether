/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

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
 * @version 1.0.0-beta.1
 */
public class OreGradeThermodynamicsEngine {
    private static final Logger logger = LoggerFactory.getLogger(OreGradeThermodynamicsEngine.class);

    private static double referenceOreConcentration = 0.05; // 5% reference metal concentration
    private static double minOreConcentrationFloor = 0.001; // 0.1% physical limit floor

    public static void processOreDepletion(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

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

    public static double getReferenceOreConcentration() { return referenceOreConcentration; }
    public static void setReferenceOreConcentration(double conc) { referenceOreConcentration = Math.max(0.001, conc); }
}

