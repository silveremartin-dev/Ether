/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;

/**
 * Selective Species Domestication &amp; Cultivar Harvest-Index Evolution Engine.
 * <p>
 * Models continuous ($C^\infty$) agronomic selection of crop harvest index and livestock feed conversion
 * driven purely by cultivated agricultural biomass ($B_{\text{agr}}$) and physical agrarian capital per capita ($K/N$)
 * (Fuller et al. 2014; Smil 2000), with zero synthetic {@code techLevel} dependency ({@code AGENTS.md} §10.1, §10.2).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class SelectiveBreedingEngine extends AbstractSimulationEngine {
    public SelectiveBreedingEngine() {
        super("SelectiveBreedingEngine", "SelectiveBreedingEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }


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

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || acc == null) return;
        double currentAgri = buffer.getBiomassAgriculture()[i];
        double cultivationGate = SmoothMath.sigmoid(currentAgri, 10.0, 3.0);
        if (cultivationGate > 1e-4) {
            double dtSafe = Math.max(0.0001, dtYears);
            double pop = buffer.getBiomassHuman()[i] / 0.06;
            double capPerCap = buffer.getResourceCapital()[i] / Math.max(1.0, pop);
            double breedingFactor = calculateSelectiveBreedingYieldFactor(currentAgri, capPerCap);
            double laborCultivatedBiomass = pop * (400.0 + 150.0 * SmoothMath.softmin(4.0, Math.log1p(capPerCap / 50.0), 4.0));
            double landCeiling = Math.max(laborCultivatedBiomass * 1.5,
                    (360000.0 + 550000.0 * SmoothMath.softmin(5.0, 1.0 + capPerCap / 100.0, 2.0)));
            double maxBreedableBiomass = landCeiling * SmoothMath.softmin(1.5, breedingFactor * 0.85, 6.0);
            double growthFactor = 1.0 + cultivationGate * (breedingFactor - 1.0) * 0.005 * dtSafe;
            double nextAgri = Math.min(maxBreedableBiomass, currentAgri * growthFactor);
            acc.dBiomassAgriculture += (float) (nextAgri - currentAgri);
        }
    }

    /**
     * Executes one monthly selective breeding tick across DoD buffer.
     */
    public static void processSelectiveBreeding(WorldBuffer buffer) {
        processSelectiveBreeding(buffer, 1.0 / 12.0);
    }

    /**
     * Executes one selective breeding tick across DoD buffer scaled by {@code dtYears}.
     */
    public static void processSelectiveBreeding(WorldBuffer buffer, double dtYears) {
        if (buffer == null || buffer.getCapacity() == 0) return;
        double dtSafe = Math.max(0.0001, dtYears);
        float[] agriArr = buffer.getBiomassAgriculture();
        float[] bioHuman = buffer.getBiomassHuman();
        float[] capArr = buffer.getResourceCapital();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            double currentAgri = agriArr[i];
            double cultivationGate = SmoothMath.sigmoid(currentAgri, 10.0, 3.0);
            if (cultivationGate > 1e-4) {
                double pop = bioHuman[i] / 0.06;
                double capPerCap = capArr[i] / Math.max(1.0, pop);
                double breedingFactor = calculateSelectiveBreedingYieldFactor(currentAgri, capPerCap);
                double laborCultivatedBiomass = pop * (400.0 + 150.0 * SmoothMath.softmin(4.0, Math.log1p(capPerCap / 50.0), 4.0));
                double landCeiling = Math.max(laborCultivatedBiomass * 1.5,
                        (360000.0 + 550000.0 * SmoothMath.softmin(5.0, 1.0 + capPerCap / 100.0, 2.0)));
                double maxBreedableBiomass = landCeiling * SmoothMath.softmin(1.5, breedingFactor * 0.85, 6.0);
                double growthFactor = 1.0 + cultivationGate * (breedingFactor - 1.0) * 0.005 * dtSafe;
                agriArr[i] = (float) Math.min(maxBreedableBiomass, currentAgri * growthFactor);
            }
        }
    }
}
