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
 * Crustal Radiogenic Geothermal Heat Flux & Sub-Surface Mining Gradient Engine.
 * Models:
 * 1. <b>Radiogenic Decay Heat Flux (q_geo in mW/m²)</b>: Primordial + Uranium, Thorium, Potassium radiogenic decay heat.
 * 2. <b>Geothermal Thermal Gradient (ΔT = 30°C/km)</b>: Sub-surface temperature increases with depth.
 * 3. <b>Deep Mining Depth Cutoff</b>: Limits maximum mineral extraction depth based on cooling technology era.
 *
 * @author Silvere Martin-Michiellot
 * @version 3.2.0
 */
public class CrustalGeothermalEngine {
    private static final Logger logger = LoggerFactory.getLogger(CrustalGeothermalEngine.class);

    /** Standard continental crustal geothermal heat flux in mW/m² */
    public static final double CONTINENTAL_HEAT_FLUX_MW_PER_M2 = 65.0;

    /** Standard geothermal gradient in °C per kilometer */
    public static final double GEOTHERMAL_GRADIENT_C_PER_KM = 30.0;

    /**
     * Calculates max accessible deep mining depth (meters) based on cooling tech.
     *
     * @param techLevel Technology era level
     * @return Maximum depth in meters
     */
    public static double calculateMaxMiningDepthMeters(double techLevel) {
        if (techLevel < 2.5) return 100.0;   // Surface mining only
        if (techLevel < 5.0) return 500.0;   // Shaft mining
        if (techLevel < 7.5) return 1500.0;  // Deep shaft mining
        return 4000.0;                       // Ultra-deep air-conditioned mining
    }

    /**
     * Executes one crustal geothermal and deep mining check across cells.
     */
    public static void processCrustalGeothermal(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            double maxDepthMeters = calculateMaxMiningDepthMeters(tech);

            // Sub-surface temperature at max mining depth
            double surfaceTempC = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
            double miningSubsurfaceTempC = surfaceTempC + (maxDepthMeters / 1000.0) * GEOTHERMAL_GRADIENT_C_PER_KM;

            // Deep mining yields additional metal ores if tech can withstand subsurface heat (> 60°C)
            if (tech >= 5.0 && miningSubsurfaceTempC > 60.0) {
                double metalOre = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
                cell.setResourceMetal(metalOre + 50.0);
            }
        }
    }
}
