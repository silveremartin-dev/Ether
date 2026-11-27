/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Jevons Paradox &amp; Rebound Effect Engine (Jancovici &amp; Bihouix Rebound Law).
 *
 * <p>Models how technological efficiency gains increase total aggregate resource consumption
 * (William Stanley Jevons, 1865; Alcott, 2005).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class JevonsParadoxEngine extends AbstractSimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(JevonsParadoxEngine.class);

    private static double reboundCoefficient = 0.80; // 80% rebound effect

    /**
     * Characteristic physical transducer capital \(K_{\text{Jevons}} = 350.0\,\text{kg/capita}\) (waterwheels, furnaces,
     * early steam engines; Jevons 1865; Saunders 1992).
     */
    public static final double JEVONS_HALF_SATURATION_CAPITAL_KG_PC = 350.0;

    public JevonsParadoxEngine() {
        super("JevonsParadoxEngine", "Jevons Paradox & Rebound Effect", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getHumanPopulation() == null) return;
        float pop = buffer.getHumanPopulation()[i];
        if (pop > 0.001f) {
            float cap = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 0.0f;
            double kPerCapita = cap / pop;
            double kRatio = kPerCapita / JEVONS_HALF_SATURATION_CAPITAL_KG_PC;
            double transducerEfficiencyFactor = (kRatio * kRatio) / (1.0 + kRatio * kRatio);
            if (transducerEfficiencyFactor > 0.05) {
                double machineryIntensity = Math.log1p(kPerCapita / 50.0);
                double reboundDemand = machineryIntensity * reboundCoefficient * transducerEfficiencyFactor * 0.005 * dtYears;
                double reboundFireGJ = reboundDemand * 100.0;
                // Lavoisier fuel conservation (Jevons 1865): combustion rebound is strictly bounded by real coal (24 GJ/t)
                // or timber (15 GJ/t) stocks — zero ex-nihilo thermal energy when no combustible fuel exists.
                float coalStock = buffer.getResourceCoal() != null ? Math.max(0.0f, buffer.getResourceCoal()[i]) : 0.0f;
                float woodStock = buffer.getWoodResource() != null ? Math.max(0.0f, buffer.getWoodResource()[i]) : 0.0f;
                if (coalStock > 0.0f) {
                    double coalTonnesNeeded = reboundFireGJ / 24.0;
                    double actualCoalTonnes = Math.min(coalStock * 0.05 * dtYears, coalTonnesNeeded);
                    acc.dResourceCoal -= (float) actualCoalTonnes;
                    acc.dEnergyFire += (float) (actualCoalTonnes * 24.0);
                } else if (woodStock > 0.0f) {
                    double woodTonnesNeeded = reboundFireGJ / 15.0;
                    double actualWoodTonnes = Math.min(woodStock * 0.02 * dtYears, woodTonnesNeeded);
                    acc.dWoodResource -= (float) actualWoodTonnes;
                    acc.dEnergyFire += (float) (actualWoodTonnes * 15.0);
                }
                float foodStock = buffer.getFoodResource() != null ? Math.max(0.0f, buffer.getFoodResource()[i]) : 0.0f;
                if (foodStock > 0.0f) {
                    double actualFoodRebound = Math.min(foodStock * 0.02 * dtYears, reboundDemand);
                    acc.dFoodResource -= (float) actualFoodRebound;
                    acc.dEnergyFoodConsumed += (float) actualFoodRebound;
                }
            }
        }
    }

    public static double getReboundCoefficient() {
        return reboundCoefficient;
    }

    public static void setReboundCoefficient(double coeff) {
        reboundCoefficient = Math.max(0.0, Math.min(2.0, coeff));
    }

    public static void resetState() {
        reboundCoefficient = 0.80;
    }

    @Override
    public void reset() {
        resetState();
    }
}
