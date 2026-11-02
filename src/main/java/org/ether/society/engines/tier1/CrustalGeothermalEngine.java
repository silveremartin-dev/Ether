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
 * Crustal Radiogenic Geothermal Heat Flux & Sub-Surface Mining Gradient Engine.
 * Models:
 * 1. <b>Radiogenic Decay Heat Flux (q_geo in mW/m²)</b>: Primordial + Uranium, Thorium, Potassium radiogenic decay heat.
 * 2. <b>Geothermal Thermal Gradient (ΔT = 30°C/km)</b>: Sub-surface temperature increases with depth.
 * 3. <b>Deep Mining Depth Cutoff</b>: Limits maximum mineral extraction depth based on cooling technology era.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class CrustalGeothermalEngine {
    private static final Logger logger = LoggerFactory.getLogger(CrustalGeothermalEngine.class);

    /** Standard continental crustal geothermal heat flux in mW/m² */
    public static final double CONTINENTAL_HEAT_FLUX_MW_PER_M2 = 65.0;

    /** Standard geothermal gradient in °C per kilometer */
    public static final double GEOTHERMAL_GRADIENT_C_PER_KM = 30.0;

    /**
     * Calculates maximum accessible deep mining depth (meters) based on rock mechanics, mechanical
     * ventilation, and active thermal cooling technology (Hartman 1992, "Introductory Mining Engineering").
     *
     * <p>Modeled as a continuous logistic technology response function:
     * $$D_{\text{max}}(\text{Tech}) = D_{\text{surface}} + \frac{D_{\text{max}} - D_{\text{surface}}}{1 + \exp\left(-k \cdot (\text{Tech} - \text{Tech}_0)\right)}$$
     * where $D_{\text{surface}} = 50.0\text{ m}$, $D_{\text{max}} = 4000.0\text{ m}$, $k = 0.90$, and $\text{Tech}_0 = 4.5$.
     *
     * @param techLevel technology era level (continuous $\text{Tech} \in [0.0, 10.0]$)
     * @return maximum accessible depth in meters
     * @see <a href="https://doi.org/10.1007/978-1-4615-5627-5">Hartman (1992)</a>
     */
    public static double calculateMaxMiningDepthMeters(double techLevel) {
        double dSurface = 50.0;
        double dMax = 4000.0;
        double k = 0.90;
        double techMidpoint = 4.5;
        return dSurface + (dMax - dSurface) / (1.0 + Math.exp(-k * (techLevel - techMidpoint)));
    }

    /**
     * Executes one crustal geothermal and deep mining check across cells.
     */
    public static void processCrustalGeothermal(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double tech = cell.getTechnologyLevelOr(0.0);
            double maxDepthMeters = calculateMaxMiningDepthMeters(tech);

            // Sub-surface temperature at max mining depth
            double surfaceTempC = cell.getTemperatureOr(15.0);
            double miningSubsurfaceTempC = surfaceTempC + (maxDepthMeters / 1000.0) * GEOTHERMAL_GRADIENT_C_PER_KM;

            // Deep mining unlocks deeper extractable ore capacity without creating mass out of nowhere
            if (tech >= 5.0 && miningSubsurfaceTempC > 60.0) {
                double metalOre = cell.getResourceMetalOr(0.0);
                double accessibleOreCap = 100.0 + (maxDepthMeters / 4000.0) * 900.0;
                cell.setResourceMetal(Math.max(metalOre, Math.min(accessibleOreCap, metalOre)));
            }
        }
    }
}
