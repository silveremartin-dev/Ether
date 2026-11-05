/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

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
public class MetallurgyEnthalpyEngine {
    private static final Logger logger = LoggerFactory.getLogger(MetallurgyEnthalpyEngine.class);

    public static final double COPPER_SMELTING_ENTHALPY_MJ_PER_KG = 5.2;
    public static final double BRONZE_SMELTING_ENTHALPY_MJ_PER_KG = 8.5;
    public static final double IRON_SMELTING_ENTHALPY_MJ_PER_KG = 24.7;
    public static final double PRECIOUS_CUPELLATION_ENTHALPY_MJ_PER_KG = 12.0;

    /** Executes one monthly chemical ore smelting tick across cells. */
    public static void processOreSmelting(List<H3Cell> cells) {
        processOreSmelting(cells, 1.0 / 12.0);
    }

    /** Alias for {@link #processOreSmelting(List, double)}. */
    public static void processMetallurgyAndSmelting(List<H3Cell> cells, double dtYears) {
        processOreSmelting(cells, dtYears);
    }

    /**
     * Executes chemical ore smelting constrained smoothly ($C^\infty$) by furnace thermal power,
     * labor throughput, fuel enthalpy (wood charcoal vs mineral coke), hydrostatic mine drainage,
     * and Fisher specie monetization vs Dutch Disease inflation.
     */
    public static void processOreSmelting(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty()) return;
        double dtSafe = Math.max(0.0001, dtYears);

        int smeltingHubs = 0;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double rawMetalOre = cell.getResourceMetalOr(0.0);
            double bioNat = cell.getBiomassNaturalOr(0.0);
            double woodRes = cell.getWoodResourceOr(0.0);
            double woodBiomass = Math.max(bioNat, woodRes);
            double coalReserve = cell.getResourceCoalOr(0.0);
            double curCap = cell.getResourceCapitalOr(0.0);
            double capPerCapita = curCap / Math.max(1.0, pop);
            double powerW = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);

            // Smooth furnace activation gate
            double fuelAvailability = Math.tanh(woodBiomass / 25.0)
                    + Math.tanh(coalReserve / 5.0) * SmoothMath.sigmoid(capPerCapita, 80.0, 25.0);
            double furnaceGate = SmoothMath.sigmoid(curCap, 10.0, 3.0)
                    * SmoothMath.sigmoid(rawMetalOre, 1.0, 0.4)
                    * Math.min(1.0, fuelAvailability);

            if (furnaceGate > 1e-3) {
                double metallurgists = pop * Math.clamp(0.005 + 0.02 * (curCap / (curCap + 1000.0)), 0.003, 0.05);
                double annualThroughputKgPerWorker = 100.0 * Math.clamp(0.4 + 1.8 * Math.log1p(powerW / 100.0), 0.35, 50.0);
                double maxLaborRefinedKg = metallurgists * annualThroughputKgPerWorker * furnaceGate * dtSafe;

                // Smooth C^infty transition between Bronze regime (T_furnace ~ 1085 C) and Iron/Steel/Electroalloy regimes (T >= 1538 C)
                double ironRegimeWeight = SmoothMath.sigmoid(powerW / 200.0 + capPerCapita / 50.0, 1.8, 0.35);
                double electroAlloyWeight = SmoothMath.sigmoid(powerW, 2500.0, 400.0);

                double localPolymetallicTin = cell.getResourceRareEarthsOr(0.0) + cell.getResourcePreciousMetalOr(0.0) * 2.0;
                double tradeAccess = cell.getChokePointScoreOr(0.0) + cell.getTransitTollRevenueOr(0.0);
                double effectiveTinSupply = localPolymetallicTin + tradeAccess * 50.0;
                double bronzeQuality = 0.18 + 0.47 * (1.0 - Math.exp(-effectiveTinSupply / 1.5));

                double blastFurnaceTransition = SmoothMath.sigmoid(powerW, 800.0, 200.0);
                double coalFrac = coalReserve / (1.0 + coalReserve);
                double ironEnthalpy = IRON_SMELTING_ENTHALPY_MJ_PER_KG
                        - (IRON_SMELTING_ENTHALPY_MJ_PER_KG - 19.5) * blastFurnaceTransition * coalFrac;
                double steelQuality = Math.clamp(0.4 + 0.8 * (1.0 - Math.exp(-capPerCapita / 200.0)), 0.4, 1.2);

                double ree = cell.getResourceRareEarthsOr(0.0);
                double prec = cell.getResourcePreciousMetalOr(0.0);
                double criticalMineralFeasibility = Math.tanh((ree + prec * 2.0) / Math.max(0.5, pop * 0.0005));
                double superAlloyQuality = 0.85 + 0.65 * criticalMineralFeasibility;

                double enthalpyMJPerKg = (1.0 - ironRegimeWeight) * BRONZE_SMELTING_ENTHALPY_MJ_PER_KG
                        + ironRegimeWeight * ironEnthalpy;
                double highTempQuality = (1.0 - electroAlloyWeight) * steelQuality + electroAlloyWeight * superAlloyQuality;
                double alloyQuality = (1.0 - ironRegimeWeight) * bronzeQuality + ironRegimeWeight * highTempQuality;

                double maxWoodHarvestKg = Math.min(woodBiomass * 0.10 * (dtSafe * 12.0), woodBiomass * 0.40);
                double woodSmeltingEnergyMJ = maxWoodHarvestKg * 18.5 * 0.15;

                double aquiferFlooding = cell.getFreshwaterAquiferOr(0.0);
                double hydrostaticHead = Math.max(1.0, aquiferFlooding * 0.01);
                double mineDrainageEfficiency = Math.clamp(
                        0.20 + 0.80 * Math.tanh((curCap * Math.max(0.1, powerW / 200.0)) / hydrostaticHead),
                        0.20, 1.0);

                double cokingGate = SmoothMath.sigmoid(coalReserve, 1.0, 0.4) * SmoothMath.sigmoid(capPerCapita, 80.0, 25.0);
                double cokingUnlockFactor = SmoothMath.smoothClamp(capPerCapita / 300.0, 0.15, 1.0, 6.0);
                double maxCoalHarvestKg = Math.min(coalReserve * 1000.0 * 0.05 * dtSafe, coalReserve * 1000.0 * 0.25)
                        * mineDrainageEfficiency * cokingUnlockFactor * cokingGate;
                double coalSmeltingEnergyMJ = maxCoalHarvestKg * 24.0 * 0.35;

                double maxFuelRefinedKg = (woodSmeltingEnergyMJ + coalSmeltingEnergyMJ) / Math.max(1.0, enthalpyMJPerKg);
                double actualRefinedMetal = Math.min(rawMetalOre, Math.min(maxLaborRefinedKg, maxFuelRefinedKg));

                if (actualRefinedMetal >= 0.001) {
                    smeltingHubs++;
                    cell.setResourceMetal(Math.max(0.0, rawMetalOre - actualRefinedMetal));

                    double totalEnergyRequiredMJ = actualRefinedMetal * enthalpyMJPerKg;
                    if (coalSmeltingEnergyMJ > 0.0) {
                        double coalShareMJ = Math.min(totalEnergyRequiredMJ, coalSmeltingEnergyMJ);
                        double coalConsumedTons = (coalShareMJ / (24.0 * 0.35)) / 1000.0;
                        cell.setResourceCoal(Math.max(0.0, coalReserve - coalConsumedTons));
                        coalReserve = cell.getResourceCoalOr(0.0);
                        totalEnergyRequiredMJ -= coalShareMJ;
                    }
                    if (totalEnergyRequiredMJ > 0.0) {
                        double woodConsumedKg = totalEnergyRequiredMJ / (18.5 * 0.15);
                        if (bioNat > 0.0) {
                            cell.setBiomassNatural(Math.max(0.0, bioNat - woodConsumedKg));
                        }
                        if (woodRes > 0.0) {
                            cell.setWoodResource(Math.max(0.0, woodRes - woodConsumedKg));
                        }
                        woodBiomass = Math.max(0.0, woodBiomass - woodConsumedKg);
                    }

                    if (electroAlloyWeight > 0.01 && ree > 0.0) {
                        cell.setResourceRareEarths(Math.max(0.0, ree - actualRefinedMetal * 0.005 * electroAlloyWeight));
                    }

                    curCap = cell.getResourceCapitalOr(0.0);
                    double durableToolInvestment = actualRefinedMetal * alloyQuality;
                    double materialWearRate = Math.max(0.001, 0.004 / (0.5 + 0.5 * alloyQuality)) * dtSafe;
                    double newCapital = Math.max(0.0, curCap * (1.0 - materialWearRate) + durableToolInvestment);
                    cell.setResourceCapital(newCapital);

                    // Allen (2009) induced timber-to-coal substitution
                    double timberScarcity = 1.0 - SmoothMath.sigmoid(woodBiomass, 60.0, 25.0);
                    double coalAvailability = Math.tanh(coalReserve / 100.0);
                    double substitutionIntensity = timberScarcity * coalAvailability;
                    if (substitutionIntensity > 0.001) {
                        double coalBurn = Math.min(coalReserve * 0.05, (pop * 0.01 + actualRefinedMetal * 0.5) * substitutionIntensity * dtSafe);
                        cell.setResourceCoal(Math.max(0.0, coalReserve - coalBurn));
                        cell.setResourceCapital(newCapital + 10.0 * substitutionIntensity * dtSafe);
                    }
                }
            }

            // PART B: Smooth Precious Metal Extraction, Monetary Liquidity & Dutch Disease Inflation
            double rawPreciousOre = cell.getResourcePreciousMetalOr(0.0);
            curCap = cell.getResourceCapitalOr(0.0);
            capPerCapita = curCap / Math.max(1.0, pop);
            if (rawPreciousOre > 0.05 && curCap > 5.0) {
                double preciousMiners = pop * Math.clamp(0.004 + 0.008 * (rawPreciousOre / (rawPreciousOre + 500.0)), 0.002, 0.025);
                double extractionRateKgPerWorker = 2.5 * Math.clamp(1.0 + Math.log1p(powerW / 100.0), 1.0, 8.0);
                double actualExtractedPreciousKg = Math.min(rawPreciousOre, preciousMiners * extractionRateKgPerWorker * dtSafe);

                if (actualExtractedPreciousKg > 0.0001) {
                    if (woodBiomass > 5.0) {
                        double cupelFuelKg = (actualExtractedPreciousKg * PRECIOUS_CUPELLATION_ENTHALPY_MJ_PER_KG) / (18.5 * 0.20);
                        double woodDeduction = Math.min(woodBiomass * 0.05, cupelFuelKg);
                        cell.setBiomassNatural(Math.max(0.0, cell.getBiomassNaturalOr(0.0) - woodDeduction));
                    }
                    cell.setResourcePreciousMetal(Math.max(0.0, rawPreciousOre - actualExtractedPreciousKg));

                    // Smooth industrial catalytic/microelectronic share vs monetary bullion share
                    double industrialCatalystWeight = SmoothMath.sigmoid(powerW, 2500.0, 400.0)
                            * SmoothMath.sigmoid(capPerCapita, 600.0, 150.0);
                    double bullionShare = 1.0 - industrialCatalystWeight;

                    if (bullionShare > 0.01) {
                        double mintedBullion = actualExtractedPreciousKg * bullionShare;
                        if (cell.getOwner() != null) {
                            cell.getOwner().setTreasuryBullion(cell.getOwner().getTreasuryBullion() + mintedBullion);
                        }
                        double curTollSpecie = cell.getTransitTollRevenueOr(0.0);
                        cell.setTransitTollRevenue(curTollSpecie + mintedBullion);

                        // Smooth C^infty Fisher Monetary Balance & Dutch Disease curve (zero cliff at bullionIntensity = 1.2)
                        double foodRes = cell.getFoodResourceOr(0.0);
                        double bullionIntensity = (curTollSpecie + mintedBullion) / Math.max(10.0, curCap + foodRes * 0.5);
                        double dutchDiseaseWeight = SmoothMath.sigmoid(bullionIntensity, 1.2, 0.25);
                        double monetizationGain = (1.0 - dutchDiseaseWeight) * mintedBullion * 1.5;
                        double excessBullion = SmoothMath.softplus(bullionIntensity - 1.2, 4.0);
                        double inflationPenalty = dutchDiseaseWeight * SmoothMath.softmin(0.15, excessBullion * 0.08 * dtSafe, 20.0);

                        curCap = Math.max(0.0, (curCap + monetizationGain) * (1.0 - inflationPenalty));
                        if (dutchDiseaseWeight > 0.05) {
                            cell.setGiniIndex(Math.clamp(cell.getGiniIndexOr(0.30) + 0.03 * dutchDiseaseWeight * dtSafe, 0.0, 0.95));
                        }
                    }

                    curCap += actualExtractedPreciousKg * industrialCatalystWeight * 3.0;
                    cell.setResourceCapital(curCap);
                }
            }
        }

        if (smeltingHubs > 0) {
            logger.debug("Metallurgy Engine: Chemical ore smelting active across {} furnace cells.", smeltingHubs);
        }
    }
}
