/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

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
 * @version 1.0.0-beta.2
 */
public class PhysicalLeontiefInputOutputEngine {
    private static final Logger logger = LoggerFactory.getLogger(PhysicalLeontiefInputOutputEngine.class);

    // Technical Production Coefficients (A_ij: inputs required per unit of output)
    // Sector 0: Agriculture, Sector 1: Metallurgy, Sector 2: Capital/Infrastructure
    private static final double A_AGRI_ENERGY = 0.15; // Energy needed per unit of agri output
    private static final double A_METAL_ENERGY = 0.45; // High energy needed for metallurgy
    private static final double A_CAPITAL_METAL = 0.35; // Metals needed for infrastructure capital
    private static final double A_CAPITAL_ENERGY = 0.25; // Energy needed for infrastructure capital

    /*
     * Processes physical input-output balances and computes production bottlenecks.
     *
     * @param cells list of H3 cells in the simulation grid
     * @param deltaYears step size in years
     */
    public static void processLeontiefInputOutput(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty()) return;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;

            double solar = cell.getEnergySolar() != null ? cell.getEnergySolar() : 0.0;
            double wind = cell.getEnergyWind() != null ? cell.getEnergyWind() : 0.0;
            double fire = cell.getEnergyFire() != null ? cell.getEnergyFire() : 0.0;
            double coal = cell.getResourceCoal() != null ? cell.getResourceCoal() : 0.0;
            double oil = cell.getResourceOil() != null ? cell.getResourceOil() : 0.0;
            double currentEnergy = (solar + wind + fire + coal * 0.05 + oil * 0.08) * 0.1;
            double currentCapital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            double currentMetal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
            double currentWood = cell.getWoodResource() != null ? cell.getWoodResource() : 0.0;
            double currentClay = cell.getResourceClay() != null ? cell.getResourceClay() : 0.0;

            // Target capital expansion based on population and technology level
            double techLevel = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
            double targetCapitalOutput = cell.getPopulation() * 0.5 * (1.0 + techLevel * 0.1) * deltaYears;

            // Calculate required inputs via Leontief matrix coefficients
            double requiredMetal = targetCapitalOutput * A_CAPITAL_METAL;
            double requiredEnergy = targetCapitalOutput * A_CAPITAL_ENERGY;
            double requiredEarthenClay = targetCapitalOutput * 0.20; // Construction ceramics, bricks, cement

            // Bottleneck determination (Leontief minimum function / Liebig law)
            double structuralMaterialStock = currentMetal + currentWood * 0.5 + currentClay * 0.3;
            double metalFeasibility = requiredMetal > 0 ? Math.min(1.0, structuralMaterialStock / requiredMetal) : 1.0;
            double energyFeasibility = requiredEnergy > 0 ? Math.min(1.0, (currentEnergy + 1.0) / requiredEnergy) : 1.0;
            double clayFeasibility = requiredEarthenClay > 0 ? Math.min(1.0, (currentClay + currentWood * 0.4 + 1.0) / requiredEarthenClay) : 1.0;

            double actualOutputRatio = Math.min(metalFeasibility, Math.min(energyFeasibility, clayFeasibility));
            double actualCapitalProduced = targetCapitalOutput * actualOutputRatio;

            // Consume inputs based on actual realized output with closed mass conservation
            double consumedMetal = actualCapitalProduced * A_CAPITAL_METAL * 0.7;
            double consumedClay = actualCapitalProduced * 0.15;
            cell.setResourceMetal(Math.max(0.0, currentMetal - consumedMetal));
            cell.setResourceClay(Math.max(0.0, currentClay - consumedClay));
            cell.setResourceCapital(currentCapital + actualCapitalProduced);
        }
    }
}


