*
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

    /**
     * Processes physical input-output balances and computes production bottlenecks
     * using the Eco-Material Niche Envelope ($\min(\Phi_E, \Phi_M, \Phi_L)$) and
     * the Material Entropy Spectrum ($\Omega_{\text{mat}}(T)$).
     *
     * <p>Rather than enforcing a rigid single archetype, the engine evaluates available
     * niche resources dynamically (Maritime/Halieutic canoes &amp; nets via {@code woodResource} + {@code biomassFish},
     * Horticultural ceramics &amp; gardens via {@code resourceClay} + {@code biomassNatural},
     * Pastoral draft &amp; hides via {@code biomassLivestock}, and Metallurgical/High-Tech via
     * {@code resourceMetal}, {@code resourceCoal}, {@code resourceRareEarths}, {@code resourcePreciousMetal}),
     * while enforcing strict Liebig bottlenecks as material complexity $\Omega_{\text{mat}}(T)$ rises.</p>
     *
     * @param cells      list of H3 cells in the simulation grid
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
            double gas = cell.getResourceGas() != null ? cell.getResourceGas() : 0.0;
            double uranium = cell.getResourceUranium() != null ? cell.getResourceUranium() : 0.0;

            double currentCapital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            double currentMetal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
            double currentWood = cell.getWoodResource() != null ? cell.getWoodResource() : 0.0;
            double currentClay = cell.getResourceClay() != null ? cell.getResourceClay() : 0.0;
            double currentFish = cell.getBiomassFish() != null ? cell.getBiomassFish() : 0.0;
            double currentLivestock = cell.getBiomassLivestock() != null ? cell.getBiomassLivestock() : 0.0;
            double currentRareEarths = cell.getResourceRareEarths() != null ? cell.getResourceRareEarths() : 0.0;
            double currentPrecious = cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal() : 0.0;

            double techLevel = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;

            // 1. Material Entropy Spectrum Omega_mat(T): count active physical input channels
            int activeMaterialChannels = 0;
            if (currentClay > 1.0) activeMaterialChannels++;
            if (currentWood > 1.0) activeMaterialChannels++;
            if (currentFish > 1.0 || currentLivestock > 1.0) activeMaterialChannels++;
            if (currentMetal > 0.5 && techLevel >= 1.5) activeMaterialChannels++;
            if (coal > 0.5 && techLevel >= 3.0) activeMaterialChannels++;
            if ((oil > 0.5 || gas > 0.5) && techLevel >= 3.4) activeMaterialChannels++;
            if ((currentRareEarths > 0.05 || currentPrecious > 0.05 || uranium > 0.05) && techLevel >= 3.8) activeMaterialChannels++;
            double omegaMatMultiplier = 1.0 + 0.08 * activeMaterialChannels;

            // 2. Eco-Material Niche Envelope: societies adapt capital tools to local biome resources
            // (Horticultural clay/wood, Maritime timber/fish-oil, Pastoral bone/hide/draft, Metallurgical ore)
            double nicheBiogenicSupport = (currentWood * 0.5) + (currentClay * 0.3)
                    + (currentFish * 0.15) + (currentLivestock * 0.20);

            // Target capital expansion based on population, technology level, and Material Entropy Spectrum
            double targetCapitalOutput = cell.getPopulation() * 0.5 * (1.0 + techLevel * 0.1) * omegaMatMultiplier * deltaYears;

            // 3. Calculate required inputs via Leontief matrix coefficients
            double currentEnergy = (solar + wind + fire + coal * 0.05 + oil * 0.08 + gas * 0.06) * 0.1;
            double requiredMetal = targetCapitalOutput * A_CAPITAL_METAL;
            double requiredEnergy = targetCapitalOutput * A_CAPITAL_ENERGY;
            double requiredEarthenClay = targetCapitalOutput * 0.20; // Construction ceramics, bricks, cement

            // 4. Master Bottleneck Determination: min(Phi_M, Phi_E, Phi_Clay, Phi_HighTech)
            double structuralMaterialStock = currentMetal + nicheBiogenicSupport;
            double metalFeasibility = requiredMetal > 0 ? Math.min(1.0, structuralMaterialStock / requiredMetal) : 1.0;
            double energyFeasibility = requiredEnergy > 0 ? Math.min(1.0, (currentEnergy + 1.0) / requiredEnergy) : 1.0;
            double clayFeasibility = requiredEarthenClay > 0 ? Math.min(1.0, (currentClay + currentWood * 0.4 + 1.0) / requiredEarthenClay) : 1.0;

            // High-tech critical mineral bottleneck (T >= 3.8 requires Rare Earths or industrial Precious Metals)
            double highTechFeasibility = 1.0;
            if (techLevel >= 3.8) {
                double requiredCritical = targetCapitalOutput * 0.005;
                double availableCritical = currentRareEarths + currentPrecious * 0.5;
                highTechFeasibility = Math.clamp(0.35 + 0.65 * Math.min(1.0, availableCritical / Math.max(1e-6, requiredCritical)), 0.35, 1.0);
            }

            double actualOutputRatio = Math.min(Math.min(metalFeasibility, energyFeasibility), Math.min(clayFeasibility, highTechFeasibility));
            double actualCapitalProduced = targetCapitalOutput * actualOutputRatio;

            // 5. Consume inputs based on actual realized output with closed mass conservation
            double consumedMetal = Math.min(currentMetal, actualCapitalProduced * A_CAPITAL_METAL * 0.7);
            double consumedClay = Math.min(currentClay, actualCapitalProduced * 0.15);
            cell.setResourceMetal(Math.max(0.0, currentMetal - consumedMetal));
            cell.setResourceClay(Math.max(0.0, currentClay - consumedClay));
            if (techLevel >= 3.8 && actualCapitalProduced > 0) {
                double consumedRee = Math.min(currentRareEarths, actualCapitalProduced * 0.002);
                cell.setResourceRareEarths(Math.max(0.0, currentRareEarths - consumedRee));
            }
            cell.setResourceCapital(currentCapital + actualCapitalProduced);
        }
    }
}


