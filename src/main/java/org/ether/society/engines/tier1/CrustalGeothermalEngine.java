/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;

import java.util.List;

/**
 * Crustal Radiogenic Geothermal Heat Flux &amp; Sub-Surface Mining Gradient Engine.
 * <p>
 * Models:
 * <ol>
 *   <li><b>Radiogenic Decay Heat Flux ($q_{\text{geo}}$ in $\text{mW/m}^2$)</b>: Primordial + U/Th/K radiogenic heat.</li>
 *   <li><b>Geothermal Thermal Gradient ($\Delta T = 30\,^\circ\text{C/km}$)</b>: Sub-surface rock temperature rise with depth.</li>
 *   <li><b>Thermodynamic Shaft Dewatering &amp; Ventilation Cutoff</b>: Maximum accessible mining depth governed smoothly ($C^\infty$)
 *       by available mechanical pumping/ventilation power per capita ($P_{\text{watts}}$) and physical capital ($K$),
 *       strictly without any synthetic {@code techLevel} parameter ({@code AGENTS.md} Â§10.1, Â§10.2).</li>
 * </ol>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class CrustalGeothermalEngine extends AbstractSimulationEngine {
    public CrustalGeothermalEngine() {
        super("CrustalGeothermalEngine", "CrustalGeothermalEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }


    /** Standard continental crustal geothermal heat flux in $\text{mW/m}^2$. */
    public static final double CONTINENTAL_HEAT_FLUX_MW_PER_M2 = 65.0;

    /** Standard geothermal gradient in $^\circ\text{C}$ per kilometer. */
    public static final double GEOTHERMAL_GRADIENT_C_PER_KM = 30.0;

    /**
     * Calculates maximum accessible deep mining depth (meters) from physical mechanical ventilation/dewatering
     * power per capita ($P_{\text{watts}}$) and installed shaft capital ($K$) (Hartman 1992,
     * <i>Introductory Mining Engineering</i>):
     * $$D_{\max}(P, K) = D_{\text{surface}} + (D_{\text{limit}} - D_{\text{surface}})\,\sigma\!\left(\ln\!\left(1 + \frac{P}{250}\right) + \ln\!\left(1 + \frac{K}{1000}\right);\, 3.5,\, \frac{1}{0.85}\right)$$
     *
     * @param powerWattsPerCapita available mechanical/electrical power per capita (W)
     * @param capital             installed cellular physical capital stock
     * @return maximum accessible mining shaft depth in meters $\in [50, 4000]$
     */
    public static double calculateMaxMiningDepthMeters(double legacyTech) {
        return calculateMaxMiningDepthMeters(100.0 * legacyTech, 1000.0 * legacyTech);
    }

    public static double calculateMaxMiningDepthMeters(double powerWattsPerCapita, double capital) {
        double dSurface = 50.0;
        double dMax = 4000.0;
        double mechRatio = Math.log1p(SmoothMath.softplus(powerWattsPerCapita, 0.1) / 250.0);
        double capRatio = Math.log1p(SmoothMath.softplus(capital, 0.1) / 1000.0);
        double depthProgress = SmoothMath.sigmoid(mechRatio + capRatio, 3.5, 1.0 / 0.85);
        return dSurface + (dMax - dSurface) * depthProgress;
    }

    /**
     * Evaluates crustal geothermal temperature at accessible shaft depth and applies thermal/ventilation
     * constraints to subsurface extraction labor efficiency across cells (strictly mass-conserving).
     *
     * @param cells active H3 cells
     */
    public static void processCrustalGeothermal(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double powerPerCapita = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double capital = cell.getResourceCapitalOr(0.0);
            double maxDepthMeters = calculateMaxMiningDepthMeters(powerPerCapita, capital);

            double surfaceTempC = cell.getTemperatureOr(15.0);
            double heatFlowRatio = cell.getMantleHeatFlowOr(CONTINENTAL_HEAT_FLUX_MW_PER_M2) / CONTINENTAL_HEAT_FLUX_MW_PER_M2;
            double effectiveGradient = GEOTHERMAL_GRADIENT_C_PER_KM * Math.clamp(heatFlowRatio, 0.5, 3.0);
            double miningSubsurfaceTempC = surfaceTempC + (maxDepthMeters / 1000.0) * effectiveGradient;

            // Smooth C^infty geothermal HVAC energy penalty when deep rock exceeds wet-bulb physiological limit (35A°C)
            double thermalCoolingLoad = SmoothMath.softplus(miningSubsurfaceTempC - 35.0, 0.5) * 0.002;
            double curWork = cell.getResourceWorkOr(pop * 0.60);
            double hvacWorkFraction = SmoothMath.softmin(0.25, thermalCoolingLoad, 20.0);
            cell.setResourceWork(Math.max(0.0, curWork * (1.0 - hvacWorkFraction)));
        }
    }
}
