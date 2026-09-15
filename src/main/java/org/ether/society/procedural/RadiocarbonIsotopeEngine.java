/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

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
 *     - C3 plants (forest, temperate crops like wheat, rice): $\delta^{13}\text{C} \approx -28\text{ ‰}$.
 *     - C4 plants (savannah grasses, maize, millet, sugarcane): $\delta^{13}\text{C} \approx -12\text{ ‰}$.
 *     - Marine carbon (carbonate, coastal fish): $\delta^{13}\text{C} \approx 0\text{ ‰}$ to $-2\text{ ‰}$.
 *   </li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 4.4.0
 */
public class RadiocarbonIsotopeEngine {
    private static final Logger logger = LoggerFactory.getLogger(RadiocarbonIsotopeEngine.class);

    /** Half-life of Carbon-14 in years */
    public static final double C14_HALF_LIFE_YEARS = 5730.0;

    /** Nuclear decay constant λ_14 in yr⁻¹ */
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
     * Determines characteristic $\delta^{13}\text{C}$ isotopic signature (in ‰ vs VPDB) based on biome and flora.
     */
    public static double determineIsotopicSignatureDelta13C(Biome biome) {
        if (biome == null) return -26.0;
        return switch (biome) {
            case SAVANNAH, PLAINS -> -12.0; // Dominant C4 grasses
            case JUNGLE, FOREST -> -28.0;   // Dominant C3 canopy
            case OCEAN, DEEP_OCEAN, BEACH -> -1.5; // Marine dissolved inorganic carbon
            default -> -25.0; // Mixed temperate C3/C4
        };
    }

    /**
     * Processes radiocarbon tracking and soil organic carbon isotopic signatures across cells.
     */
    public static void processIsotopicDecay(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            double soc = cell.getSoilOrganicCarbon() != null ? cell.getSoilOrganicCarbon() : 50.0;
            // Decay of organic carbon pool
            double decayedSOC = soc * Math.exp(-LAMBDA_14 * deltaYears);
            cell.setSoilOrganicCarbon(Math.max(1.0, decayedSOC));
        }
    }
}
