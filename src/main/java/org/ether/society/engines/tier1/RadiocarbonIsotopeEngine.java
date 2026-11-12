/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Radiocarbon ($^{14}\text{C}$) Decay & Stable Isotope Fractionation ($\delta^{13}\text{C}$) Engine.
 *
 * <p>Models isotope geochemistry and nuclear decay for simulated radiometric dating and paleodiet reconstruction:</p>
 * <ul>
 *   <li><b>Radioactive Decay Law</b>:
 *     $$N(t) = N_0 \exp(-\lambda_{14} \cdot t) \quad \text{with} \quad \lambda_{14} = \frac{\ln 2}{t_{1/2}} \approx \frac{\ln 2}{5730\text{ yr}} \approx 1.2097 \times 10^{-4}\text{ yr}^{-1}$$
 *   </li>
 *   <li><b>Radiocarbon Age Equation</b>:
 *     $$t_{\text{BP}} = -\frac{1}{\lambda_{14}} \ln\left(\frac{A}{A_0}\right) = -8033 \ln\left(\frac{A}{A_0}\right)$$
 *   </li>
 *   <li><b>Photosynthetic Fractionation ($\delta^{13}\text{C}$)</b>:
 *     Continuous ecophysiological crossover between C3 and C4 pathways (Ehleringer, Cerling & Helliker 1997; Still et al. 2003)
 *     based on growing season thermal crossover ($T_{\text{cross}} \approx 22^\circ\text{C}$) and precipitation.
 *   </li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class RadiocarbonIsotopeEngine extends AbstractSimulationEngine {
    public RadiocarbonIsotopeEngine() {
        super("RadiocarbonIsotopeEngine", "RadiocarbonIsotopeEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(RadiocarbonIsotopeEngine.class);

    /** Half-life of Carbon-14 in years */
    public static final double C14_HALF_LIFE_YEARS = 5730.0;

    /** Nuclear decay constant Î»_14 in yrâ»Â¹ */
    public static final double LAMBDA_14 = Math.log(2.0) / C14_HALF_LIFE_YEARS; // ~1.20968e-4

    /** Mean Libby lifetime in years (8033 yr) */
    public static final double LIBBY_MEAN_LIFETIME_YEARS = 8033.0;

    /**
     * Calculates remaining fraction of Carbon-14 activity after elapsed years.
     *
     * @param elapsedYears Time elapsed in years
     * @return Remaining activity fraction A / A_0
     */
    public static double calculateRemainingC14Activity(double elapsedYears) {
        if (elapsedYears <= 0.0) return 1.0;
        return Math.exp(-LAMBDA_14 * elapsedYears);
    }

    /**
     * Calculates conventional radiocarbon age before present (BP) from measured activity fraction A / A_0.
     *
     * @param activityFraction Ratio A / A_0 in ]0, 1]
     * @return Age in radiocarbon years BP
     */
    public static double calculateRadiocarbonAgeBP(double activityFraction) {
        if (activityFraction <= 0.0) return Double.POSITIVE_INFINITY;
        return -LIBBY_MEAN_LIFETIME_YEARS * Math.log(activityFraction);
    }

    /**
     * Determines characteristic $\delta^{13}\text{C}$ isotopic signature (in â€° vs VPDB) continuously
     * based on thermodynamic temperature and precipitation crossover (Ehleringer et al. 1997, Still et al. 2003).
     *
     * @param cell target cell with biophysical state variables
     * @return isotopic signature in â€° vs VPDB
     */
    public static double determineIsotopicSignatureDelta13C(H3Cell cell) {
        if (cell == null) return -26.0;
        double elev = cell.getElevationOr(0.0);
        Biome biome = cell.getBiome();
        if (elev < 0.0 || biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.BEACH) {
            return -1.5; // Marine dissolved inorganic carbon
        }

        double tempC = cell.getTemperatureOr(15.0);
        double rainMm = cell.getRainfallOr(600.0);
        return calculateContinuousDelta13C(tempC, rainMm);
    }

    /**
     * Continuous analytical calculation of $\delta^{13}\text{C}$ from climatic parameters.
     *
     * @param tempC mean annual surface temperature in Â°C
     * @param rainMm mean annual precipitation in mm
     * @return isotopic signature in â€° vs VPDB
     */
    public static double calculateContinuousDelta13C(double tempC, double rainMm) {
        // C4 quantum yield crossover occurs at T >= 22A°C with moderate-to-low rainfall (Ehleringer 1997)
        double thermalAffinity = 1.0 / (1.0 + Math.exp(-0.35 * (tempC - 22.0)));
        double moistureAffinity = 1.0 / (1.0 + Math.exp(0.003 * (rainMm - 1200.0)));
        double c4Fraction = Math.clamp(thermalAffinity * moistureAffinity, 0.0, 1.0);

        double delta13C_C4 = -12.0; // C4 photosynthetic pathway (maize, sorghum, warm savannah grasses)
        double delta13C_C3 = -28.0; // C3 photosynthetic pathway (temperate forest canopy, wheat, rice)
        return c4Fraction * delta13C_C4 + (1.0 - c4Fraction) * delta13C_C3;
    }

    /**
     * Fallback overload for legacy calls using {@link Biome}.
     */
    public static double determineIsotopicSignatureDelta13C(Biome biome) {
        if (biome == null) return -26.0;
        if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.BEACH) return -1.5;
        if (biome == Biome.SAVANNAH || biome == Biome.PLAINS) return calculateContinuousDelta13C(25.0, 600.0);
        if (biome == Biome.JUNGLE || biome == Biome.FOREST) return calculateContinuousDelta13C(18.0, 1500.0);
        return calculateContinuousDelta13C(14.0, 800.0);
    }

    /**
     * Processes radiocarbon tracking and soil organic carbon isotopic signatures across cells.
     */
    public static void processIsotopicDecay(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double soc = cell.getSoilOrganicCarbonOr(50.0);
            // Decay of organic carbon pool
            double decayedSOC = soc * Math.exp(-LAMBDA_14 * deltaYears);
            cell.setSoilOrganicCarbon(Math.max(1.0, decayedSOC));
        }
    }
}
