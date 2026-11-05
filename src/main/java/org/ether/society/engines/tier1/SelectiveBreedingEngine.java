/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;

import java.util.List;

/**
 * Selective Species Domestication &amp; Cultivar Harvest-Index Evolution Engine.
 * <p>
 * Models continuous ($C^\infty$) agronomic selection of crop harvest index and livestock feed conversion
 * driven purely by cultivated agricultural biomass ($B_{\text{agr}}$) and physical agrarian capital per capita ($K/N$)
 * (Fuller et al. 2014; Smil 2000), with zero synthetic {@code techLevel} dependency ({@code AGENTS.md} §10.1, §10.2).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SelectiveBreedingEngine {

    /**
     * Calculates smooth ($C^\infty$) cultivar harvest-index selection multiplier from cultivated
     * agricultural biomass stock ($B_{\text{agr}}$) and physical capital per capita ($K/N$).
     *
     * @param agBiomass        standing cultivated agricultural biomass (kg)
     * @param capitalPerCapita physical agrarian/scientific capital per capita
     * @return crop yield breeding factor bounded smoothly in $[1.0, 3.5]$
     */
    public static double calculateSelectiveBreedingYieldFactor(double capitalPerCapita) {
        return calculateSelectiveBreedingYieldFactor(1000.0, capitalPerCapita);
    }

    public static double calculateSelectiveBreedingYieldFactor(double agBiomass, double capitalPerCapita) {
        double posAg = SmoothMath.softplus(agBiomass, 0.2);
        double posKpc = SmoothMath.softplus(capitalPerCapita, 0.2);
        double domesticationEffect = 1.0 + 1.2 * (1.0 - Math.exp(-posAg / 2000.0));
        double breedingCapitalEffect = 1.0 + 1.3 * Math.tanh(posKpc / 350.0);
        return SmoothMath.softmin(3.5, domesticationEffect * breedingCapitalEffect, 4.0);
    }

    /**
     * Executes one monthly selective breeding tick across cells.
     */
    public static void processSelectiveBreeding(List<H3Cell> cells) {
        processSelectiveBreeding(cells, 1.0 / 12.0);
    }

    /**
     * Executes one selective breeding tick across cells scaled by {@code dtYears} and cell surface area.
     */
    public static void processSelectiveBreeding(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty()) return;
        double dtSafe = Math.max(0.0001, dtYears);

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double currentAgri = cell.getBiomassAgricultureOr(0.0);
            double cultivationGate = SmoothMath.sigmoid(currentAgri, 10.0, 3.0);
            if (cultivationGate > 1e-4) {
                int pop = cell.getPopulationOr(0);
                double areaScale = Math.max(0.05, cell.getEffectiveSurfaceAreaKm2() / 1770.0);
                double capPerCap = cell.getResourceCapitalOr(0.0) / Math.max(1.0, pop);
                double breedingFactor = calculateSelectiveBreedingYieldFactor(currentAgri, capPerCap);
                double laborCultivatedBiomass = pop * (400.0 + 150.0 * SmoothMath.softmin(4.0, Math.log1p(capPerCap / 50.0), 4.0));
                double landCeiling = Math.max(laborCultivatedBiomass * 1.5,
                        (360000.0 + 550000.0 * SmoothMath.softmin(5.0, 1.0 + capPerCap / 100.0, 2.0)) * areaScale);
                double maxBreedableBiomass = landCeiling * SmoothMath.softmin(1.5, breedingFactor * 0.85, 6.0);
                double growthFactor = 1.0 + cultivationGate * (breedingFactor - 1.0) * 0.005 * dtSafe;
                cell.setBiomassAgriculture(Math.min(maxBreedableBiomass, currentAgri * growthFactor));
            }
        }
    }
}
