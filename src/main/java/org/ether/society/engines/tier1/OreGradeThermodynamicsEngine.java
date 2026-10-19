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

    /**
     * Models the non-linear thermodynamic energy escalation required for metal ore extraction.
     * <p>
     * Implements the Bihouix ore grade exhaustion relation:
     * \[
     * E_{\text{extract}} = E_0 \cdot \left(\frac{C_{\text{ref}}}{C_{\text{ore}}}\right)^{1.5}
     * \]
     * where \(C_{\text{ore}}\) is the local ore grade mass fraction clamped above \(C_{\text{floor}} = 0.001\) (0.1%),
     * and \(E_0\) is baseline comminution and smelting work in Joules per kilogram (\(\text{J}\cdot\text{kg}^{-1}\Default).
     * Depletes cellular available mechanical work \(W_{\text{avail}}\) accordingly.
     * </p>
     *
     * @param cells planetary hex cells undergoing mineral extraction
     * @param deltaYears time step duration in terrestrial decimal years (\(\Delta t > 0\))
     */
    public static void processOreDepletion(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;

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

    /**
     * Gets pristine reference ore concentration \(C_{\text{ref}}\).
     *
     * @return dimensionless mass fraction (default 0.05 = 5%)
     */
    public static double getReferenceOreConcentration() { return referenceOreConcentration; }

    /**
     * Sets reference ore concentration clamped above 0.001.
     *
     * @param conc dimensionless mass fraction
     */
    public static void setReferenceOreConcentration(double conc) { referenceOreConcentration = Math.max(0.001, conc); }

    /**
     * Resets reference ore concentration parameters for scenario isolation.
     */
    public static void resetState() {
        referenceOreConcentration = 0.05;
        minOreConcentrationFloor = 0.001;
    }
}


