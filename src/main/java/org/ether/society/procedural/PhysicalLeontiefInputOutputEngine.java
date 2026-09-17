/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

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
 * @version 1.0.0-beta.1
 */
public class PhysicalLeontiefInputOutputEngine {
    private static final Logger logger = LoggerFactory.getLogger(PhysicalLeontiefInputOutputEngine.class);

    // Technical Production Coefficients (A_ij: inputs required per unit of output)
    // Sector 0: Agriculture, Sector 1: Metallurgy, Sector 2: Capital/Infrastructure
    private static final double A_AGRI_ENERGY = 0.15; // Energy needed per unit of agri output
    private static final double A_METAL_ENERGY = 0.45; // High energy needed for metallurgy
    private static final double A_CAPITAL_METAL = 0.35; // Metals needed for infrastructure capital
    private static final double A_CAPITAL_ENERGY = 0.25; // Energy needed for infrastructure capital

    /**
     * Processes physical input-output balances and computes production bottlenecks.
     *
     * @param cells list of H3 cells in the simulation grid
     * @param deltaYears step size in years
     */
    public static void processLeontiefInputOutput(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell.getPopulation() == null || cell.getPopulation() <= 0) continue;

            double currentCapital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            double currentMetal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
            double currentWood = cell.getWoodResource() != null ? cell.getWoodResource() : 0.0;
            double currentEnergy = (cell.getEnergySolar() + cell.getEnergyWind() + cell.getEnergyFire()) * 0.1;

            // Target capital expansion based on population and technology level
            double techLevel = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
            double targetCapitalOutput = cell.getPopulation() * 0.5 * (1.0 + techLevel * 0.1) * deltaYears;

            // Calculate required inputs via Leontief matrix coefficients
            double requiredMetal = targetCapitalOutput * A_CAPITAL_METAL;
            double requiredEnergy = targetCapitalOutput * A_CAPITAL_ENERGY;

            // Bottleneck determination (Leontief minimum function)
            double metalFeasibility = requiredMetal > 0 ? Math.min(1.0, (currentMetal + currentWood * 0.5) / requiredMetal) : 1.0;
            double energyFeasibility = requiredEnergy > 0 ? Math.min(1.0, (currentEnergy + 1.0) / requiredEnergy) : 1.0;

            double actualOutputRatio = Math.min(metalFeasibility, energyFeasibility);
            double actualCapitalProduced = targetCapitalOutput * actualOutputRatio;

            // Consume inputs based on actual realized output
            double consumedMetal = actualCapitalProduced * A_CAPITAL_METAL;
            cell.setResourceMetal(Math.max(0.0, currentMetal - consumedMetal));
            cell.setResourceCapital(currentCapital + actualCapitalProduced);
        }
    }
}

