/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Procedural engine implementing a physical multi-sector Leontief Input-Output matrix.
 *
 * <p>Models physical resource interdependencies between economic sectors:
 * Agriculture, Metallurgy, Infrastructure Capital, and Energy. Enforces technical
 * production coefficients (A_ij) to calculate material bottlenecks and economic multipliers
 * across H3 cells.</p>

 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class PhysicalLeontiefInputOutputEngine extends AbstractSimulationEngine {
    public PhysicalLeontiefInputOutputEngine() {
        super("PhysicalLeontiefInputOutputEngine", "PhysicalLeontiefInputOutputEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(PhysicalLeontiefInputOutputEngine.class);

    // Technical Production Coefficients (A_ij: inputs required per unit of output)
    // Sector 0: Agriculture, Sector 1: Metallurgy, Sector 2: Capital/Infrastructure
    private static final double A_AGRI_ENERGY = 0.15; // Energy needed per unit of agri output
    private static final double A_METAL_ENERGY = 0.45; // High energy needed for metallurgy
    private static final double A_CAPITAL_METAL = 0.35; // Metals needed for infrastructure capital
    private static final double A_CAPITAL_ENERGY = 0.25; // Energy needed for infrastructure capital

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(
            org.ether.society.core.dod.WorldBuffer buf,
            int i,
            double dtYears,
            double dtMonthly,
            org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (buf == null || dtYears <= 0.0 || i < 0 || i >= buf.getCapacity()) return;
        double pop = buf.getHumanPopulation()[i];
        if (pop <= 0.0) return;

        double solar = buf.getEnergySolar() != null ? buf.getEnergySolar()[i] : 0.0;
        double wind = buf.getEnergyWind() != null ? buf.getEnergyWind()[i] : 0.0;
        double fire = buf.getEnergyFire() != null ? buf.getEnergyFire()[i] : 0.0;
        double coal = buf.getResourceCoal() != null ? buf.getResourceCoal()[i] : 0.0;
        double oil = buf.getResourceOil() != null ? buf.getResourceOil()[i] : 0.0;
        double gas = buf.getResourceGas() != null ? buf.getResourceGas()[i] : 0.0;
        double uranium = buf.getResourceUranium() != null ? buf.getResourceUranium()[i] : 0.0;

        double currentCapital = buf.getResourceCapital()[i];
        double currentMetal = buf.getMetalResource() != null ? buf.getMetalResource()[i] : 0.0;
        double currentWood = buf.getWoodResource() != null ? buf.getWoodResource()[i] : 0.0;
        double currentClay = buf.getClayResource() != null ? buf.getClayResource()[i] : 0.0;
        double currentFish = buf.getBiomassFish() != null ? buf.getBiomassFish()[i] : 0.0;
        double currentLivestock = buf.getBiomassLivestock() != null ? buf.getBiomassLivestock()[i] : 0.0;

        double kPerCapita = currentCapital / Math.max(1.0, pop);

        double activeMaterialChannels = 0.0;
        if (currentClay > 1.0) activeMaterialChannels += 1.0;
        if (currentWood > 1.0) activeMaterialChannels += 1.0;
        if (currentFish > 1.0 || currentLivestock > 1.0) activeMaterialChannels += 1.0;
        if (currentMetal > 0.5) {
            activeMaterialChannels += 1.0 / (1.0 + Math.exp(-(kPerCapita - 80.0) / 25.0));
        }
        if (coal > 0.5) {
            activeMaterialChannels += 1.0 / (1.0 + Math.exp(-(kPerCapita - 900.0) / 250.0));
        }
        if (oil > 0.5 || gas > 0.5) {
            activeMaterialChannels += 1.0 / (1.0 + Math.exp(-(kPerCapita - 2200.0) / 500.0));
        }
        double highTechActivation = 1.0 / (1.0 + Math.exp(-(kPerCapita - 6000.0) / 1500.0));
        if (uranium > 0.05) {
            activeMaterialChannels += highTechActivation;
        }
        double omegaMatMultiplier = 1.0 + 0.08 * activeMaterialChannels;

        double nicheBiogenicSupportTonnes = (currentWood * 0.5) + (currentClay * 0.3)
                + (currentFish * 0.15) + (currentLivestock * 0.20);

        double neolithicTransition = 1.0 / (1.0 + Math.exp(-(kPerCapita - 15.0) / 5.0));
        double toolStockMultiplier = (0.20 * (1.0 - neolithicTransition))
                + (neolithicTransition * (1.0 + 0.12 * Math.log1p(kPerCapita / 20.0)));
        double targetCapitalOutputKg = pop * 0.5 * toolStockMultiplier * omegaMatMultiplier * dtYears;
        double targetCapitalOutputTonnes = org.ether.society.model.PhysicalUnitsHelper.kgToMetricTonnes(targetCapitalOutputKg);

        double currentEnergyGj = (solar + wind + fire) * dtYears;
        double requiredMetalTonnes = targetCapitalOutputTonnes * A_CAPITAL_METAL;
        double requiredEnergyGj = targetCapitalOutputKg * org.ether.society.model.PhysicalUnitsHelper.megajoulesToGigajoules(15.0) * A_CAPITAL_ENERGY;
        double requiredClayTonnes = targetCapitalOutputTonnes * 0.20;

        double structuralMaterialTonnes = currentMetal + nicheBiogenicSupportTonnes;
        double metalFeasibility = requiredMetalTonnes > 0 ? Math.min(1.0, structuralMaterialTonnes / requiredMetalTonnes) : 1.0;
        double energyFeasibility = requiredEnergyGj > 0 ? Math.min(1.0, (currentEnergyGj + 0.1) / requiredEnergyGj) : 1.0;
        double clayFeasibility = requiredClayTonnes > 0 ? Math.min(1.0, (currentClay + currentWood * 0.4 + 0.1) / requiredClayTonnes) : 1.0;

        double actualOutputRatio = Math.min(Math.min(metalFeasibility, energyFeasibility), clayFeasibility);
        double actualCapitalProducedKg = targetCapitalOutputKg * actualOutputRatio;
        acc.dResourceCapital += (float) actualCapitalProducedKg;
    }

    @Override
    public void tick(org.ether.society.core.dod.WorldBuffer buf, double dtYears, double dtMonthly) {
        if (buf == null || buf.getCapacity() == 0) return;
        org.ether.society.core.engine.DerivativeAccumulator acc = new org.ether.society.core.engine.DerivativeAccumulator();
        for (int i = 0; i < buf.getCapacity(); i++) {
            acc.clear();
            accumulateCellularDerivatives(buf, i, dtYears, dtMonthly, acc);
            if (acc.dResourceCapital > 0.0f) {
                double consumedTonnes = org.ether.society.model.PhysicalUnitsHelper.kgToMetricTonnes(acc.dResourceCapital);
                if (buf.getMetalResource() != null) {
                    acc.dMetalResource -= (float) Math.min(buf.getMetalResource()[i], consumedTonnes * 0.05);
                }
                if (buf.getClayResource() != null) {
                    buf.getClayResource()[i] = (float) Math.max(0.0, buf.getClayResource()[i] - Math.min(buf.getClayResource()[i], consumedTonnes * 0.10));
                }
                AbstractSimulationEngine.applyAccumulatorToCell(buf, i, acc);
            }
        }
    }
}


