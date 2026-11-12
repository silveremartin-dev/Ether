/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Maximum Entropy Production Principle (MEPP) &amp; Planetary Heat Engine.
 *
 * <p>Formalizes Axel Kleidon's non-equilibrium planetary thermodynamics
 * (Kleidon 2010, 2012, 2016, <i>Thermodynamic Foundations of the Earth System</i>, Cambridge University Press;
 * Paltridge 1975, <i>Q.J.R. Meteorol. Soc.</i>):</p>
 *
 * <p>Models Earth's coupled climate, hydrological cycle, and wind circulation as a macroscopic
 * planetary Carnot heat engine operating at its maximum thermodynamic power output (MEP state):</p>
 * <ol>
 *   <li><b>Planetary Carnot Thermodynamic Efficiency (\(\eta_{\text{carnot}}\))</b>:
 *       \[
 *       \eta_{\text{carnot}} = \frac{T_s - T_e}{T_s} \approx \frac{288.15 - 255.0}{288.15} \approx 0.115 \quad (11.5\%)
 *       \]
 *   <li><b>Maximum Kinetic Power of the Atmospheric Circulation</b>:
 *       \[
 *       G_{\text{ke}} = \frac{1}{4} \eta_{\text{carnot}} \cdot I_{\text{net}} \approx 2.5\text{--}3.5\text{ W/m}^2
 *       \]
 *   <li><b>Equilibrium Hydrological Evapo-Precipitation</b>:
 *       Partitions net surface solar irradiance \(R_n\) into latent heat flux \(\lambda E\) and sensible heat \(H\):
 *       \[
 *       \lambda E = \left(\frac{\Delta}{\Delta + \gamma}\right) R_n, \quad P_{\text{precip}} = E = \frac{\lambda E}{\lambda_v}
 *       \]
 *       where \(\lambda_v \approx 2.45 \times 10^6\text{ J/kg}\) is the latent heat of vaporization of water.</li>
 *   <li><b>Thermodynamic Limit on Renewable Wind Power Extraction</b> (Kleidon &amp; Miller 2011):
 *       Wind turbines cannot extract more than \(\approx 1.0\text{--}1.5\text{ W/m}^2\) of continuous kinetic power
 *       without damping atmospheric pressure gradients and collapsing local wind generation.</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://doi.org/10.1017/CBO9781139343366">Kleidon (2016), Thermodynamic Foundations of the Earth System</a>
 */
public class MaximumEntropyProductionEngine extends AbstractSimulationEngine {
    public MaximumEntropyProductionEngine() {
        super("MaximumEntropyProductionEngine", "MaximumEntropyProductionEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(MaximumEntropyProductionEngine.class);

    // --- Universal Planetary Thermodynamic Constants (Kleidon 2010, 2016) ---

    /** Planetary effective emission temperature to space in Kelvin (\(T_e \approx 255.0\text{ K} = -18^\circ\text{C}\)). */
    public static final double T_EMISSION_KELVIN = 255.0;

    /** Mean global solar absorbed radiative flux (\(I_{\text{net}} \approx 240.0\text{ W/m}^2\)). */
    public static final double MEAN_NET_SOLAR_FLUX_W_M2 = 240.0;

    /** Latent heat of vaporization of water (\(\lambda_v \approx 2.45 \times 10^6\text{ J/kg}\)). */
    public static final double LATENT_HEAT_VAPORIZATION_J_KG = 2.45e6;

    /** Psychrometric constant at sea level (\(\gamma \approx 0.66\text{ hPa/K}\)). */
    public static final double PSYCHROMETRIC_CONSTANT_HPA_K = 0.66;

    /** Physical thermodynamic Betz-Kleidon upper limit for kinetic wind power extraction (\(\sim 1.25\text{ W/m}^2\)). */
    public static final double MAX_KINETIC_WIND_EXTRACTION_W_M2 = 1.25;

    /**
     * Calculates the planetary Carnot thermodynamic efficiency:
     * \[
     * \eta_{\text{carnot}} = \max\left(0, \frac{T_{\text{surf}} - T_e}{T_{\text{surf}}}\right)
     * \]
     *
     * @param surfaceTempKelvin surface temperature in Kelvin
     * @return Carnot efficiency fraction in \([0, 1]\)
     */
    public static double calculateCarnotEfficiency(double surfaceTempKelvin) {
        if (surfaceTempKelvin <= T_EMISSION_KELVIN) return 0.0;
        return (surfaceTempKelvin - T_EMISSION_KELVIN) / surfaceTempKelvin;
    }

    /**
     * Calculates maximum atmospheric kinetic power generation in Watts per square meter (\(\text{W/m}^2\)):
     * \[
     * G_{\text{ke}} = \frac{1}{4} \eta_{\text{carnot}} \cdot I_{\text{net}}
     * \]
     *
     * @param surfaceTempKelvin surface temperature in Kelvin
     * @param netSolarFluxWM2   net absorbed solar flux in \(\text{W/m}^2\)
     * @return kinetic dissipation rate in \(\text{W/m}^2\)
     */
    public static double calculateMaxKineticPowerWM2(double surfaceTempKelvin, double netSolarFluxWM2) {
        double eta = calculateCarnotEfficiency(surfaceTempKelvin);
        return 0.25 * eta * Math.max(0.0, netSolarFluxWM2);
    }

    /**
     * Calculates thermodynamic equilibrium precipitation/evaporation in millimeters per year (\(\text{mm/yr}\)):
     * \[
     * E = \frac{\lambda E}{\lambda_v} = \left(\frac{\Delta}{\Delta + \gamma}\right) \frac{R_n}{\lambda_v} \cdot 3.15576 \times 10^7\text{ s/yr}
     * \]
     *
     * @param surfaceTempC surface temperature in \(^\circ\text{C}\)
     * @param netRadWM2    net surface absorbed radiation in \(\text{W/m}^2\)
     * @return equilibrium precipitation in \(\text{mm/year}\)
     */
    public static double calculateEquilibriumPrecipitationMmYr(double surfaceTempC, double netRadWM2) {
        if (netRadWM2 <= 0.0) return 0.0;

        // Clausius-Clapeyron saturation vapor pressure curve slope Delta (hPa/K)
        double t = Math.clamp(surfaceTempC, -40.0, 50.0);
        double delta = (4098.0 * 6.1078 * Math.exp((17.27 * t) / (t + 237.3))) / Math.pow(t + 237.3, 2);

        // Priestley-Taylor equilibrium fraction: Delta / (Delta + gamma)
        double ptFraction = delta / (delta + PSYCHROMETRIC_CONSTANT_HPA_K);

        // Latent heat flux: lambda_E = ptFraction * netRad (W/m2 = J/(s m2))
        double latentHeatWM2 = ptFraction * netRadWM2;

        // Convert J/(s m2) to kg/(m2 yr) = mm/yr of water: 1 W/m2 = 1 J/(s m2)
        // (1 W/m2 / 2.45e6 J/kg) * 31,557,600 s/yr * 1 mm/(kg/m2) = 12.88 mm/(yr * (W/m2))
        return (latentHeatWM2 / LATENT_HEAT_VAPORIZATION_J_KG) * 3.15576e7;
    }

    /**
     * Executes one time-stepped MEPP thermodynamic rate calibration across spatial cells.
     * Couples surface temperature, solar insolation, and rainfall physics.
     *
     * @param cells   spatial H3 cell collection
     * @param dtYears elapsed simulation step in years
     */
    public static void processMaximumEntropyProduction(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty() || dtYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            double tempC = cell.getTemperatureOr(15.0);
            double tempK = tempC + 273.15;
            double latRad = Math.toRadians(cell.getLatitudeOr(0.0));
            double insolation = cell.getEnergySolarOr(0.0) > 0.0
                    ? cell.getEnergySolarOr(0.0)
                    : 340.0 * Math.max(0.15, Math.cos(latRad));

            // 1. Kinetic wind dissipation and turbine power cap
            double maxWindKineticWM2 = calculateMaxKineticPowerWM2(tempK, insolation);
            double sustainableWindHarvest = Math.min(maxWindKineticWM2 * 0.35, MAX_KINETIC_WIND_EXTRACTION_W_M2);

            // 2. Thermodynamic equilibrium rainfall boundary
            double mepRainMm = calculateEquilibriumPrecipitationMmYr(tempC, insolation * 0.60);
            double currentRain = cell.getRainfallOr(600.0);

            // Smooth thermodynamic relaxation of precipitation toward the Carnot-Penman optimum
            if (currentRain > 0.0) {
                double relaxedRain = currentRain * (1.0 - 0.05 * dtYears) + mepRainMm * (0.05 * dtYears);
                cell.setRainfall(Math.max(10.0, relaxedRain));
            }
        }
    }
}
