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
    public void tick(org.ether.society.core.dod.WorldBuffer buf, double dtYears, double dtMonthly) {
        if (buf == null || buf.getCapacity() == 0) return;
        int n = buf.getCapacity();

        float[] popArr = buf.getBiomassHuman();
        float[] solarArr = buf.getEnergySolar();
        float[] windArr = buf.getEnergyWind();
        float[] fireArr = buf.getEnergyFire();
        float[] coalArr = buf.getResourceCoal();
        float[] oilArr = buf.getResourceOil();
        float[] gasArr = buf.getResourceGas();
        float[] uraniumArr = buf.getResourceUranium();
        float[] capArr = buf.getResourceCapital();
        float[] metalArr = buf.getMetalResource();
        float[] woodArr = buf.getWoodResource();
        float[] clayArr = buf.getClayResource();
        float[] fishArr = buf.getBiomassFish();
        float[] livestockArr = buf.getBiomassLivestock();

        for (int i = 0; i < n; i++) {
            double pop = popArr[i];
            if (pop <= 0.0) continue;

            double solar = solarArr[i];
            double wind = windArr[i];
            double fire = fireArr[i];
            double coal = coalArr[i];
            double oil = oilArr[i];
            double gas = gasArr[i];
            double uranium = uraniumArr[i];

            double currentCapital = capArr[i];
            double currentMetal = metalArr[i];
            double currentWood = woodArr[i];
            double currentClay = clayArr[i];
            double currentFish = fishArr[i];
            double currentLivestock = livestockArr[i];

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

            double nicheBiogenicSupport = (currentWood * 0.5) + (currentClay * 0.3)
                    + (currentFish * 0.15) + (currentLivestock * 0.20);

            double neolithicTransition = 1.0 / (1.0 + Math.exp(-(kPerCapita - 15.0) / 5.0));
            double toolStockMultiplier = (0.20 * (1.0 - neolithicTransition))
                    + (neolithicTransition * (1.0 + 0.12 * Math.log1p(kPerCapita / 20.0)));
            double targetCapitalOutput = pop * 0.5 * toolStockMultiplier * omegaMatMultiplier * dtYears;

            double currentEnergy = (solar + wind + fire + coal * 0.05 + oil * 0.08 + gas * 0.06) * 0.1;
            double requiredMetal = targetCapitalOutput * A_CAPITAL_METAL;
            double requiredEnergy = targetCapitalOutput * A_CAPITAL_ENERGY;
            double requiredEarthenClay = targetCapitalOutput * 0.20;

            double structuralMaterialStock = currentMetal + nicheBiogenicSupport;
            double metalFeasibility = requiredMetal > 0 ? Math.min(1.0, structuralMaterialStock / requiredMetal) : 1.0;
            double energyFeasibility = requiredEnergy > 0 ? Math.min(1.0, (currentEnergy + 1.0) / requiredEnergy) : 1.0;
            double clayFeasibility = requiredEarthenClay > 0 ? Math.min(1.0, (currentClay + currentWood * 0.4 + 1.0) / requiredEarthenClay) : 1.0;

            double actualOutputRatio = Math.min(Math.min(metalFeasibility, energyFeasibility), clayFeasibility);
            double actualCapitalProduced = targetCapitalOutput * actualOutputRatio;

            double consumedMetal = Math.min(currentMetal, actualCapitalProduced * A_CAPITAL_METAL * 0.7);
            double consumedClay = Math.min(currentClay, actualCapitalProduced * 0.15);
            metalArr[i] = (float) Math.max(0.0, currentMetal - consumedMetal);
            clayArr[i] = (float) Math.max(0.0, currentClay - consumedClay);
            capArr[i] = (float) (currentCapital + actualCapitalProduced);
        }
    }
}


