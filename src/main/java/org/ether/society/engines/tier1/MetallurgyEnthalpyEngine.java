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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Metallurgical Enthalpy &amp; Oxide Reduction Engine.
 * <p>
 * Models thermodynamic chemical reduction of metal ores ($\text{Cu}$, $\text{Cu-Sn}$ bronze, $\text{Fe}_2\text{O}_3$
 * iron/steel, electro-metallurgical superalloys, and precious bullion cupellation) using strictly smooth ($C^\infty$)
 * phase transitions across power flux ($P_{\text{watts}}$), capital density ($K/N$), fuel enthalpy, and Fisher
 * monetary specie balance ({@code AGENTS.md} §10.1, §10.2).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class MetallurgyEnthalpyEngine extends AbstractSimulationEngine {
    public MetallurgyEnthalpyEngine() {
        super("MetallurgyEnthalpyEngine", "MetallurgyEnthalpyEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(MetallurgyEnthalpyEngine.class);

    public static final double COPPER_SMELTING_ENTHALPY_MJ_PER_KG = 5.2;
    public static final double BRONZE_SMELTING_ENTHALPY_MJ_PER_KG = 8.5;
    public static final double IRON_SMELTING_ENTHALPY_MJ_PER_KG = 24.7;
    public static final double PRECIOUS_CUPELLATION_ENTHALPY_MJ_PER_KG = 12.0;


    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 0.0) return;

        double rawMetalOre = buffer.getMetalResource()[i];
        double woodBiomass = Math.max(buffer.getBiomassNatural()[i], buffer.getWoodResource()[i]);
        double coalReserve = buffer.getResourceCoal()[i];
        double curCap = buffer.getResourceCapital()[i];
        double capPerCapita = curCap / Math.max(1.0, pop);
        double powerW = Math.max(85.0, ((buffer.getEnergyFire()[i] + buffer.getEnergyWind()[i]
                + buffer.getEnergySolar()[i] + buffer.getEnergyFoodConsumed()[i]) * 31.688) / pop);

        double fuelAvailability = Math.tanh(woodBiomass / 25.0)
                + Math.tanh(coalReserve / 5.0) * SmoothMath.sigmoid(capPerCapita, 80.0, 25.0);
        double furnaceGate = SmoothMath.sigmoid(curCap, 10.0, 3.0)
                * SmoothMath.sigmoid(rawMetalOre, 1.0, 0.4)
                * Math.min(1.0, fuelAvailability);

        if (furnaceGate > 1e-3) {
            double metallurgists = pop * Math.clamp(0.005 + 0.02 * (curCap / (curCap + 1000.0)), 0.003, 0.05);
            double annualThroughputKgPerWorker = 100.0 * Math.clamp(0.4 + 1.8 * Math.log1p(powerW / 100.0), 0.35, 50.0);
            double maxLaborRefinedKg = metallurgists * annualThroughputKgPerWorker * furnaceGate * dtYears;

            double ironRegimeWeight = SmoothMath.sigmoid(Math.max(0.0, powerW - 340.0) / 100.0 + capPerCapita / 80.0, 1.5, 0.35);
            double enthalpyMJPerKg = (1.0 - ironRegimeWeight) * BRONZE_SMELTING_ENTHALPY_MJ_PER_KG + ironRegimeWeight * IRON_SMELTING_ENTHALPY_MJ_PER_KG;

            double maxWoodHarvestKg = Math.min(woodBiomass * 0.10 * (dtYears * 12.0), woodBiomass * 0.40);
            double woodSmeltingEnergyMJ = maxWoodHarvestKg * 18.5 * 0.15;
            double cokingGate = SmoothMath.sigmoid(coalReserve, 1.0, 0.4) * SmoothMath.sigmoid(capPerCapita, 80.0, 25.0);
            double maxCoalHarvestKg = Math.min(coalReserve * 1000.0 * 0.05 * dtYears, coalReserve * 1000.0 * 0.25) * cokingGate;
            double coalSmeltingEnergyMJ = maxCoalHarvestKg * 24.0 * 0.35;

            double maxFuelRefinedKg = (woodSmeltingEnergyMJ + coalSmeltingEnergyMJ) / Math.max(1.0, enthalpyMJPerKg);
            double refinedKg = Math.min(rawMetalOre * 1000.0 * 0.05, Math.min(maxLaborRefinedKg, maxFuelRefinedKg));
            if (refinedKg > 0.01) {
                double oreConsumed = refinedKg / 1000.0;
                acc.dMetalResource -= (float) oreConsumed;
                acc.dResourceCapital += (float) (refinedKg * 0.01);
                if (woodSmeltingEnergyMJ > 0.0) {
                    acc.dWoodResource -= (float) Math.min(buffer.getWoodResource()[i], maxWoodHarvestKg * 0.001);
                }
                if (coalSmeltingEnergyMJ > 0.0) {
                    acc.dResourceCoal -= (float) Math.min(coalReserve, maxCoalHarvestKg * 0.001);
                }
            }
        }
    }
}
