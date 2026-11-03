/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Metallurgical Enthalpy & Oxide Reduction Engine.
 * Replaces abstract metal resource pools with thermodynamic chemical reduction:
 * 1. <b>Specific Smelting Enthalpy (ΔH_smelt = +24.7 MJ/kg Fe)</b>: Required energy input to reduce raw iron oxide ore (Fe₂O₃) into pure metallic iron.
 * 2. <b>Fuel Consumption (Charcoal/Coal)</b>: Conversion efficiency depends on furnace temperature and technology era.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class MetallurgyEnthalpyEngine {
    private static final Logger logger = LoggerFactory.getLogger(MetallurgyEnthalpyEngine.class);

    /** Specific smelting enthalpy for Native/Oxide Copper reduction (T >= 1085 C) in MJ/kg */
    public static final double COPPER_SMELTING_ENTHALPY_MJ_PER_KG = 5.2;
    /** Specific smelting enthalpy for Tin-Bronze alloying & reduction (T >= 1085 C) in MJ/kg */
    public static final double BRONZE_SMELTING_ENTHALPY_MJ_PER_KG = 8.5;
    /** Specific smelting enthalpy for Iron Oxide reduction (T >= 1538 C) in MJ/kg */
    public static final double IRON_SMELTING_ENTHALPY_MJ_PER_KG = 24.7;
    /** Specific thermal cupellation and refining enthalpy for precious bullion ore in MJ/kg */
    public static final double PRECIOUS_CUPELLATION_ENTHALPY_MJ_PER_KG = 12.0;

    /**
     * Executes one chemical ore smelting tick across cells using default monthly step.
     */
    public static void processOreSmelting(List<H3Cell> cells) {
        processOreSmelting(cells, 1.0 / 12.0);
    }

    /**
     * Alias for processOreSmelting for metallurgical conservation and historical bottleneck tests.
     */
    public static void processMetallurgyAndSmelting(List<H3Cell> cells, double dtYears) {
        processOreSmelting(cells, dtYears);
    }

    /**
     * Executes chemical ore smelting constrained by furnace activation temperature thresholds,
     * labor throughput, fuel kinetics (charcoal vs mineral coke), alloying bottlenecks (Late Bronze Age Tin crisis),
     * and dual-regime precious metal dynamics (pre-industrial monetary specie & Dutch Disease vs modern high-tech catalysts).
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

// =========================================================================
// PART A: Structural Metallurgy along the Furnace Temperature Continuum
// - P < 200 W, K/N < 50: Chalcolithic Copper / Bronze (T_furnace >= 1085 C)
// - P >= 200 W, K/N >= 50: Bloomery Iron Age (T_furnace >= 1538 C)
// - P >= 1000 W, Coal > 0: Industrial Coke Blast Furnace & Bessemer Steel
// - P >= 2500 W: Electro-Metallurgy & Superalloys (Liebig bottleneck on REE & Precious Metals)
// =========================================================================
boolean hasThermalFuel = (woodBiomass > 20.0) || (coalReserve > 5.0 && capPerCapita > 80.0);
if (curCap > 10.0 && rawMetalOre > 1.0 && hasThermalFuel) {
    double metallurgists = pop * Math.clamp(0.005 + 0.02 * (curCap / (curCap + 1000.0)), 0.003, 0.05);
    double annualThroughputKgPerWorker = 100.0 * Math.clamp(0.4 + 1.8 * Math.log1p(powerW / 100.0), 0.35, 50.0);
    double maxLaborRefinedKg = metallurgists * annualThroughputKgPerWorker * dtSafe;

    // Determine specific reduction enthalpy (MJ/kg) and alloy mechanical quality Q_alloy
    double enthalpyMJPerKg;
    double alloyQuality;
    if (powerW < 200.0 && capPerCapita < 50.0) {
        // Chalcolithic / Native & Oxide Copper / Bronze: soft metal, rapid tool deformation
        double localPolymetallicTin = (cell.getResourceRareEarthsOr(0.0))
                + (cell.getResourcePreciousMetalOr(0.0)) * 2.0;
        double tradeAccess = (cell.getChokePointScoreOr(0.0))
                + (cell.getTransitTollRevenueOr(0.0));
        double effectiveTinSupply = localPolymetallicTin + tradeAccess * 50.0;
        enthalpyMJPerKg = BRONZE_SMELTING_ENTHALPY_MJ_PER_KG;
        alloyQuality = 0.18 + 0.47 * (1.0 - Math.exp(-effectiveTinSupply / 1.5));
    } else {
        // Iron, Steel & Superalloy Ages (T >= 1538 C)
        // Continuous blast furnace enthalpy reduction via mineral coking coal (Smil 2017; Allen 2009):
        double blastFurnaceTransition = 1.0 / (1.0 + Math.exp(-0.005 * (powerW - 800.0)));
        double coalAvailability = coalReserve / (1.0 + coalReserve);
        enthalpyMJPerKg = IRON_SMELTING_ENTHALPY_MJ_PER_KG - (IRON_SMELTING_ENTHALPY_MJ_PER_KG - 19.5) * blastFurnaceTransition * coalAvailability;
        alloyQuality = Math.clamp(0.4 + 0.4 * (1.0 - Math.exp(-capPerCapita / 200.0)), 0.4, 1.2);

        // High-Power Electro-Metallurgy (powerW >= 2500): Liebig bottleneck on Rare Earths & Precious Catalysts
        if (powerW >= 2500.0) {
            double ree = cell.getResourceRareEarthsOr(0.0);
            double prec = cell.getResourcePreciousMetalOr(0.0);
            double criticalMineralFeasibility = Math.min(1.0, (ree + prec * 2.0) / Math.max(0.5, pop * 0.0005));
            alloyQuality = 0.85 + 0.65 * criticalMineralFeasibility;
        }
    }

    // Fuel energy availability: wood charcoal vs mineral coal coke
    double maxWoodHarvestKg = Math.min(woodBiomass * 0.10 * (dtSafe * 12.0), woodBiomass * 0.40);
    double woodSmeltingEnergyMJ = maxWoodHarvestKg * 18.5 * 0.15;
    double requiredLaborEnergyMJ = maxLaborRefinedKg * enthalpyMJPerKg;

    // Universal Hydrostatic Mine-Dewatering Law (applies to all deep subsurface shaft extraction):
    // Subsurface shafts flood proportionally to groundwater head (freshwaterAquifer).
    // Accessible seam depth scales with mechanical pumping work vs hydrostatic head.
    double aquiferFlooding = cell.getFreshwaterAquiferOr(0.0);
    double hydrostaticHead = Math.max(1.0, aquiferFlooding * 0.01);
    double mineDrainageEfficiency = Math.clamp(
            0.20 + 0.80 * Math.min(1.0, (curCap * Math.max(0.1, powerW / 200.0)) / hydrostaticHead),
            0.20, 1.0);

    double coalSmeltingEnergyMJ = 0.0;
    if (coalReserve > 1.0 && capPerCapita > 80.0) {
        double cokingUnlockFactor = Math.clamp(capPerCapita / 300.0, 0.15, 1.0);
        double maxCoalHarvestKg = Math.min(coalReserve * 1000.0 * 0.05 * dtSafe, coalReserve * 1000.0 * 0.25)
                * mineDrainageEfficiency * cokingUnlockFactor;
        coalSmeltingEnergyMJ = maxCoalHarvestKg * 24.0 * 0.35; // 24 MJ/kg coke, 35% blast furnace efficiency
    }

                double maxFuelRefinedKg = (woodSmeltingEnergyMJ + coalSmeltingEnergyMJ) / enthalpyMJPerKg;
                double actualRefinedMetal = Math.min(rawMetalOre, Math.min(maxLaborRefinedKg, maxFuelRefinedKg));

                if (actualRefinedMetal >= 0.001) {
                    smeltingHubs++;
                    cell.setResourceMetal(Math.max(0.0, rawMetalOre - actualRefinedMetal));

                    // Deduct thermal fuel (prioritize mineral coke in industrial era, otherwise wood charcoal)
                    double totalEnergyRequiredMJ = actualRefinedMetal * enthalpyMJPerKg;
                    if (coalSmeltingEnergyMJ > 0.0) {
                        double coalShareMJ = Math.min(totalEnergyRequiredMJ, coalSmeltingEnergyMJ);
                        double coalConsumedTons = (coalShareMJ / (24.0 * 0.35)) / 1000.0;
                        cell.setResourceCoal(Math.max(0.0, coalReserve - coalConsumedTons));
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
                    }        // In advanced high-power metallurgy (powerW >= 2500), deduct trace critical rare earths & precious catalysts
        if (powerW >= 2500.0) {
            double ree = cell.getResourceRareEarthsOr(0.0);
            if (ree > 0.0) {
                cell.setResourceRareEarths(Math.max(0.0, ree - actualRefinedMetal * 0.005));
            }
        }

        // Expand cell physical capital (tools, weapons, plows, machinery) minus mechanical wear & rust
        curCap = cell.getResourceCapitalOr(0.0);
        double durableToolInvestment = actualRefinedMetal * alloyQuality;
        double materialWearRate = Math.max(0.001, 0.004 / (0.5 + 0.5 * alloyQuality)) * dtSafe;
        double newCapital = Math.max(0.0, curCap * (1.0 - materialWearRate) + durableToolInvestment);
        cell.setResourceCapital(newCapital);

        // Part C: Endogenous Boserup-Allen Induced Innovation & Mineral Coal Transition (Allen 2009)
        // Severe timber depletion forces substitution to mineral coal for blast furnace smelting
        if (woodBiomass < 50.0 && coalReserve > 50.0) {
            double inducedInnovation = 0.05 * (1.0 - woodBiomass / 50.0) * dtSafe;
            double currentTech = cell.getTechnologyLevelOr(1.0);
            cell.setTechnologyLevel(currentTech + inducedInnovation);
            double coalBurn = Math.min(coalReserve * 0.05, (pop * 0.01 + actualRefinedMetal * 0.5) * dtSafe);
            cell.setResourceCoal(Math.max(0.0, coalReserve - coalBurn));
        }
    }
}

// =========================================================================
// PART B: Precious Metal Extraction & Cupellation (Gold, Silver, Platinum)
// Historical Dual-Regime Physics:
// 1. Pre-Industrial (Low Power / Low Capital): Gold/silver are too soft for tools.
//    They become monetary bullion (Nation treasury & commercial liquidity).
//    Excessive bullion extraction relative to real physical capital triggers
//    Dutch Disease / Price Revolution inflation (eroding productive capital).
// 2. Modern High-Tech (powerW >= 2500, K/N >= 600): Precious metals act as physical
//    catalytic and microelectronic inputs (gold bonding wires, platinum catalysts).
// =========================================================================
double rawPreciousOre = cell.getResourcePreciousMetalOr(0.0);
if (rawPreciousOre > 0.05 && curCap > 5.0) {
    double preciousMiners = pop * Math.clamp(0.004 + 0.008 * (rawPreciousOre / (rawPreciousOre + 500.0)), 0.002, 0.025);
    double extractionRateKgPerWorker = 2.5 * Math.clamp(1.0 + Math.log1p(powerW / 100.0), 1.0, 8.0);
    double maxLaborPreciousKg = preciousMiners * extractionRateKgPerWorker * dtSafe;

    double actualExtractedPreciousKg = Math.min(rawPreciousOre, maxLaborPreciousKg);
    if (actualExtractedPreciousKg > 0.0001) {
        if (woodBiomass > 5.0) {
            double cupelFuelKg = (actualExtractedPreciousKg * PRECIOUS_CUPELLATION_ENTHALPY_MJ_PER_KG) / (18.5 * 0.20);
            double woodDeduction = Math.min(woodBiomass * 0.05, cupelFuelKg);
            cell.setBiomassNatural(Math.max(0.0, woodBiomass - woodDeduction));
        }
        cell.setResourcePreciousMetal(Math.max(0.0, rawPreciousOre - actualExtractedPreciousKg));

        double curCapLocal = cell.getResourceCapitalOr(0.0);
        if (powerW >= 2500.0 && capPerCapita > 600.0) {
                        // Modern industrial regime: precious metals form high-reliability microelectronics & catalytic capital
                        cell.setResourceCapital(curCap + actualExtractedPreciousKg * 3.0);
                    } else {
                        // Pre-industrial monetary regime: accumulates in Sovereign Treasury & commercial specie
                        if (cell.getOwner() != null) {
                            cell.getOwner().setTreasuryBullion(cell.getOwner().getTreasuryBullion() + actualExtractedPreciousKg);
                        }
                        double curTollSpecie = cell.getTransitTollRevenueOr(0.0);
                        cell.setTransitTollRevenue(curTollSpecie + actualExtractedPreciousKg);

                        // Fisher Monetary Balance & Dutch Disease (Potosi / Spanish Price Revolution):
                        // Moderate specie monetizes local markets, facilitating artisan division of labor (+capital).
                        // Excessive bullion relative to real physical output (Capital + Food) triggers hyperinflation & Dutch Disease!
                        double foodRes = cell.getFoodResourceOr(0.0);
                        double bullionIntensity = (curTollSpecie + actualExtractedPreciousKg) / Math.max(10.0, curCap + foodRes * 0.5);
                        if (bullionIntensity <= 1.2) {
                            // Healthy monetization: lowers local barter friction, enabling artisan capital accumulation
                            cell.setResourceCapital(curCap + actualExtractedPreciousKg * 1.5);
                        } else {
                            // Dutch Disease / Bullion Inflation: excess specie crowds out local productive manufacturing
                            // and spikes wealth inequality (Gini)
                            double inflationPenalty = Math.clamp((bullionIntensity - 1.2) * 0.08 * dtSafe, 0.0, 0.15);
                            cell.setResourceCapital(Math.max(0.0, curCap * (1.0 - inflationPenalty)));
                            double curGini = cell.getGiniIndexOr(0.30);
                            cell.setGiniIndex(Math.clamp(curGini + 0.03 * dtSafe, 0.0, 0.95));
                        }
                    }
                }
            }
        }

        if (smeltingHubs > 0) {
            logger.debug("Metallurgy Engine: Chemical ore smelting active across {} furnace cells.", smeltingHubs);
        }
    }
}


