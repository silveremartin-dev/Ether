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
import org.ether.society.engines.ProceduralEnginePlugin;
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
 * @version 1.0.0-beta.2
 */
public class JevonsParadoxEngine extends AbstractSimulationEngine implements ProceduralEnginePlugin {

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
        if (buffer == null || buffer.getBiomassHuman() == null) return;
        float pop = buffer.getBiomassHuman()[i];
        if (pop > 0.001f) {
            float cap = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 0.0f;
            double kPerCapita = cap / pop;
            double kRatio = kPerCapita / JEVONS_HALF_SATURATION_CAPITAL_KG_PC;
            double transducerEfficiencyFactor = (kRatio * kRatio) / (1.0 + kRatio * kRatio);
            if (transducerEfficiencyFactor > 0.05) {
                double machineryIntensity = Math.log1p(kPerCapita / 50.0);
                double reboundDemand = machineryIntensity * reboundCoefficient * transducerEfficiencyFactor * 0.005 * dtYears;
                acc.dEnergyFoodConsumed += (float) reboundDemand;
                acc.dEnergyFire += (float) (reboundDemand * 100.0);
            }
        }
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        processJevonsRebound(cells, deltaYears);
    }

    /**
     * Process Jevons rebound across spatial cells with continuous \(C^1\) physical transducer efficiency activation.
     *
     * @param cells the spatial cells list
     * @param deltaYears time step in years
     */
    public static void processJevonsRebound(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            int pop = Math.max(1, cell.getPopulationOr(100));
            double kPerCapita = cell.getResourceCapitalOr(0.0) / pop;

            double kRatio = kPerCapita / JEVONS_HALF_SATURATION_CAPITAL_KG_PC;
            double transducerEfficiencyFactor = (kRatio * kRatio) / (1.0 + kRatio * kRatio);
            if (transducerEfficiencyFactor > 0.05) {
                double machineryIntensity = Math.log1p(kPerCapita / 50.0);
                double reboundDemand = machineryIntensity * reboundCoefficient * transducerEfficiencyFactor * 0.005 * deltaYears;

                double habitatKw = cell.getHabitatEnergyKwOr(0.0);
                cell.setHabitatEnergyKw(habitatKw + reboundDemand * 100.0);

                double currentFoodEnergy = cell.getEnergyFoodConsumedOr(0.0);
                double maxMetabolicFood = Math.max(10.0, pop * 1.5);
                cell.setEnergyFoodConsumed(Math.min(maxMetabolicFood, currentFoodEnergy + reboundDemand));
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
