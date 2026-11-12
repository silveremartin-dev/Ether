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
 * @version 1.0.0-beta.2
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

            double solar = cell.getEnergySolarOr(0.0);
            double wind = cell.getEnergyWindOr(0.0);
            double fire = cell.getEnergyFireOr(0.0);
            double coal = cell.getResourceCoalOr(0.0);
            double oil = cell.getResourceOilOr(0.0);
            double gas = cell.getResourceGasOr(0.0);
            double uranium = cell.getResourceUraniumOr(0.0);

            double currentCapital = cell.getResourceCapitalOr(0.0);
            double currentMetal = cell.getResourceMetalOr(0.0);
            double currentWood = cell.getWoodResourceOr(0.0);
            double currentClay = cell.getResourceClayOr(0.0);
            double currentFish = cell.getBiomassFishOr(0.0);
            double currentLivestock = cell.getBiomassLivestockOr(0.0);
            double currentRareEarths = cell.getResourceRareEarthsOr(0.0);
            double currentPrecious = cell.getResourcePreciousMetalOr(0.0);

            int pop = cell.getPopulation();
            double kPerCapita = currentCapital / Math.max(1.0, (double) pop);

            // 1. Material Entropy Spectrum Omega_mat(k_pc): smooth C^1 physical activation of input channels
            // Grounded in Ashby (2012) "Materials and the Environment":
            // - Biogenic/Ceramic channels (clay, wood, fish, livestock): accessible at all capital levels
            // - Metallurgical smelting channel: activates smoothly around k_pc = 80 kg/cap (Chalcolithic/Bronze kilns)
            // - Coal/Steam thermal channel: activates smoothly around k_pc = 900 kg/cap (Early Industrial furnaces)
            // - Hydrocarbon petrochemical channel: activates smoothly around k_pc = 2200 kg/cap (Refineries & internal combustion)
            // - Critical REE/Semiconductor/Nuclear channel: activates smoothly around k_pc = 6000 kg/cap (Precision cleanrooms)
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
            if (currentRareEarths > 0.05 || currentPrecious > 0.05 || uranium > 0.05) {
                activeMaterialChannels += highTechActivation;
            }
            double omegaMatMultiplier = 1.0 + 0.08 * activeMaterialChannels;

            // 2. Eco-Material Niche Envelope: societies adapt capital tools to local biome resources
            // (Horticultural clay/wood, Maritime timber/fish-oil, Pastoral bone/hide/draft, Metallurgical ore)
            double nicheBiogenicSupport = (currentWood * 0.5) + (currentClay * 0.3)
                    + (currentFish * 0.15) + (currentLivestock * 0.20);

            // Target capital expansion based on population, physical tool stock per capita, and Material Entropy Spectrum
            // Prehistoric deep-time baseline (AGENTS.md §8 & §10): Paleolithic tool production (k_pc < 15 kg/cap) is bounded by somatic labor
            double neolithicTransition = 1.0 / (1.0 + Math.exp(-(kPerCapita - 15.0) / 5.0));
            double toolStockMultiplier = (0.20 * (1.0 - neolithicTransition))
                    + (neolithicTransition * (1.0 + 0.12 * Math.log1p(kPerCapita / 20.0)));
            double targetCapitalOutput = pop * 0.5 * toolStockMultiplier * omegaMatMultiplier * deltaYears;

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

            // High-tech critical mineral bottleneck (smoothly weighted by highTechActivation for k_pc >= 6000 kg/cap)
            double highTechFeasibility = 1.0;
            if (highTechActivation > 0.05) {
                double requiredCritical = targetCapitalOutput * 0.005 * highTechActivation;
                double availableCritical = currentRareEarths + currentPrecious * 0.5;
                double rawCriticalFeasibility = Math.clamp(0.35 + 0.65 * Math.min(1.0, availableCritical / Math.max(1e-6, requiredCritical)), 0.35, 1.0);
                highTechFeasibility = (1.0 - highTechActivation) + highTechActivation * rawCriticalFeasibility;
            }

            double actualOutputRatio = Math.min(Math.min(metalFeasibility, energyFeasibility), Math.min(clayFeasibility, highTechFeasibility));
            double actualCapitalProduced = targetCapitalOutput * actualOutputRatio;

            // 5. Consume inputs based on actual realized output with closed mass conservation
            double consumedMetal = Math.min(currentMetal, actualCapitalProduced * A_CAPITAL_METAL * 0.7);
            double consumedClay = Math.min(currentClay, actualCapitalProduced * 0.15);
            cell.setResourceMetal(Math.max(0.0, currentMetal - consumedMetal));
            cell.setResourceClay(Math.max(0.0, currentClay - consumedClay));
            if (highTechActivation > 0.05 && actualCapitalProduced > 0) {
                double consumedRee = Math.min(currentRareEarths, actualCapitalProduced * 0.002 * highTechActivation);
                cell.setResourceRareEarths(Math.max(0.0, currentRareEarths - consumedRee));
            }
            cell.setResourceCapital(currentCapital + actualCapitalProduced);
        }
    }
}


