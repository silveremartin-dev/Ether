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

    /* Specific smelting enthalpy for Native/Oxide Copper reduction (T >= 1085 C) in MJ/kg */
    public static final double COPPER_SMELTING_ENTHALPY_MJ_PER_KG = 5.2;
    /* Specific smelting enthalpy for Tin-Bronze alloying & reduction (T >= 1085 C) in MJ/kg */
    public static final double BRONZE_SMELTING_ENTHALPY_MJ_PER_KG = 8.5;
    /* Specific smelting enthalpy for Iron Oxide reduction (T >= 1538 C) in MJ/kg */
    public static final double IRON_SMELTING_ENTHALPY_MJ_PER_KG = 24.7;
    /* Specific thermal cupellation and refining enthalpy for precious bullion ore in MJ/kg */
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
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double rawMetalOre = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
            double bioNat = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 0.0;
            double woodRes = cell.getWoodResource() != null ? cell.getWoodResource() : 0.0;
            double woodBiomass = Math.max(bioNat, woodRes);
            double coalReserve = cell.getResourceCoal() != null ? cell.getResourceCoal() : 0.0;
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;

            // =========================================================================
            // PART A: Structural Metallurgy along the Furnace Temperature Continuum
            // - Tech 1.5..1.8 (T_furnace >= 1085 C): Chalcolithic Copper (soft, sigma_yield ~ 70 MPa)
            // - Tech 1.8..2.5 (T_furnace >= 1085 C + Sn): Bronze Age (hard, sigma_yield ~ 250 MPa; vulnerable to Tin trade collapse)
            // - Tech 2.5..3.2 (T_furnace >= 1538 C): Bloomery Iron Age (abundant ore, charcoal-bounded)
            // - Tech 3.2..3.8 (T_furnace >= 1600 C): Industrial Coke Blast Furnace & Bessemer Steel
            // - Tech >= 3.8   (T_furnace >= 2000 C): Electro-Metallurgy & Superalloys (Liebig bottleneck on REE & Precious Metals)
            // =========================================================================
            boolean hasThermalFuel = (woodBiomass > 20.0) || (tech >= 2.6 && coalReserve > 5.0);
            if (tech >= 1.5 && rawMetalOre > 1.0 && hasThermalFuel) {
                double metallurgists = pop * Math.clamp(0.005 * tech, 0.003, 0.05);
                double annualThroughputKgPerWorker = 100.0 * Math.clamp(Math.pow(Math.max(0.6, tech / 2.5), 2.0), 0.35, 50.0);
                double maxLaborRefinedKg = metallurgists * annualThroughputKgPerWorker * dtSafe;

                // Determine specific reduction enthalpy (MJ/kg) and alloy mechanical quality Q_alloy
                double enthalpyMJPerKg;
                double alloyQuality;
                if (tech < 1.8) {
                    // Chalcolithic / Native & Oxide Copper: soft metal, rapid tool deformation
                    enthalpyMJPerKg = COPPER_SMELTING_ENTHALPY_MJ_PER_KG;
                    alloyQuality = 0.25;
                } else if (tech < 2.5) {
                    // Bronze Age (Cu + 10% Sn): requires scarce Tin (polymetallic geology) or active long-distance trade corridor
                    enthalpyMJPerKg = BRONZE_SMELTING_ENTHALPY_MJ_PER_KG;
                    double localPolymetallicTin = (cell.getResourceRareEarths() != null ? cell.getResourceRareEarths() : 0.0)
                            + (cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal() : 0.0) * 2.0;
                    double tradeAccess = (cell.getChokePointScore() != null ? cell.getChokePointScore() : 0.0)
                            + (cell.getTransitTollRevenue() != null ? cell.getTransitTollRevenue() : 0.0);
                    boolean hasTinSupply = (localPolymetallicTin > 1.0) || (tradeAccess > 0.01);
                    // Late Bronze Age Collapse bottleneck: if trade is severed and no local tin exists,
                    // bronze alloying fails and metallurgy reverts to brittle/soft unalloyed copper (Q = 0.18)!
                    alloyQuality = hasTinSupply ? 0.65 : 0.18;
                } else {
                    // Iron, Steel & Superalloy Ages (T >= 1538 C)
                    enthalpyMJPerKg = (tech >= 3.2 && coalReserve > 1.0) ? 19.5 : IRON_SMELTING_ENTHALPY_MJ_PER_KG;
                    alloyQuality = Math.clamp(0.4 + 0.2 * (tech - 2.5), 0.4, 1.2);

                    // Modern High-Tech Metallurgy (tech >= 3.8): Liebig bottleneck on Rare Earths & Precious Catalysts
                    if (tech >= 3.8) {
                        double ree = cell.getResourceRareEarths() != null ? cell.getResourceRareEarths() : 0.0;
                        double prec = cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal() : 0.0;
                        double criticalMineralFeasibility = Math.min(1.0, (ree + prec * 2.0) / Math.max(0.5, pop * 0.0005));
                        alloyQuality = 0.85 + 0.65 * criticalMineralFeasibility;
                    }
                }

                // Fuel energy availability: wood charcoal vs mineral coal coke
                double maxWoodHarvestKg = Math.min(woodBiomass * 0.10 * (dtSafe * 12.0), woodBiomass * 0.40);
                double woodSmeltingEnergyMJ = maxWoodHarvestKg * 18.5 * 0.15;
                double requiredLaborEnergyMJ = maxLaborRefinedKg * enthalpyMJPerKg;

                // Universal Bottleneck-Induced Directed Technical Change (Boserup-Allen-Acemoglu Law):
                // When thermal fuel or alloying supply falls below labor smelting capacity, scarcity stress
                // directs innovation toward unlocking the next latent geological resource tier.
                if (woodSmeltingEnergyMJ < requiredLaborEnergyMJ * 0.8) {
                    double inducedDeltaTech = org.ether.society.engines.tier2.theories.TechTreeEngine
                            .calculateBottleneckInducedSubstitution(cell, dtSafe);
                    if (inducedDeltaTech > 0.0) {
                        tech += inducedDeltaTech;
                        cell.setTechnologyLevel(tech);
                    }
                }

                // Universal Hydrostatic Mine-Dewatering Law (applies to all deep subsurface shaft extraction):
                // Subsurface shafts flood proportionally to groundwater head (freshwaterAquifer).
                // Accessible seam depth scales with mechanical pumping work (K * tech) vs hydrostatic head.
                double curCap = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
                double aquiferFlooding = cell.getFreshwaterAquifer() != null ? cell.getFreshwaterAquifer() : 0.0;
                double hydrostaticHead = Math.max(1.0, aquiferFlooding * 0.01);
                double mineDrainageEfficiency = Math.clamp(
                        0.20 + 0.80 * Math.min(1.0, (curCap * Math.max(0.1, tech - 1.5)) / hydrostaticHead),
                        0.20, 1.0);

                double coalSmeltingEnergyMJ = 0.0;
                if (tech >= 3.0 && coalReserve > 1.0) {
                    double cokingUnlockFactor = Math.clamp((tech - 2.9) / 0.3, 0.15, 1.0);
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
                    }

                    // In high-tech era (tech >= 3.8), deduct trace critical rare earths & precious catalysts used in alloys/electronics
                    if (tech >= 3.8) {
                        double ree = cell.getResourceRareEarths() != null ? cell.getResourceRareEarths() : 0.0;
                        if (ree > 0.0) {
                            cell.setResourceRareEarths(Math.max(0.0, ree - actualRefinedMetal * 0.005));
                        }
                    }

                    // Expand cell physical capital (tools, weapons, plows, machinery) minus mechanical wear & rust
                    curCap = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
                    double durableToolInvestment = actualRefinedMetal * alloyQuality;
                    double materialWearRate = Math.max(0.01, 0.05 / Math.max(0.5, tech * 0.4)) * dtSafe;
                    double newCapital = Math.max(0.0, (curCap + durableToolInvestment) * (1.0 - materialWearRate));
                    cell.setResourceCapital(newCapital);
                }
            }

            // =========================================================================
            // PART B: Precious Metal Extraction & Cupellation (Gold, Silver, Platinum)
            // Historical Dual-Regime Physics:
            // 1. Pre-Industrial (tech < 3.8): Gold/silver are too soft for plows or weapons.
            //    They become monetary bullion (Nation treasury & commercial liquidity).
            //    Excessive bullion extraction relative to real physical capital triggers
            //    Dutch Disease / Price Revolution inflation (eroding productive capital).
            // 2. Modern High-Tech (tech >= 3.8): Precious metals act as physical catalytic
            //    and microelectronic inputs (gold bonding wires, silver contacts, platinum catalysts).
            // =========================================================================
            double rawPreciousOre = cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal() : 0.0;
            if (tech >= 1.2 && rawPreciousOre > 0.05) {
                double preciousMiners = pop * Math.clamp(0.001 * tech, 0.0005, 0.015);
                double extractionRateKgPerWorker = 2.0 * Math.clamp(tech / 1.5, 0.5, 8.0);
                double maxLaborPreciousKg = preciousMiners * extractionRateKgPerWorker * dtSafe;

                double actualExtractedPreciousKg = Math.min(rawPreciousOre, maxLaborPreciousKg);
                if (actualExtractedPreciousKg > 0.0001) {
                    if (tech >= 1.8 && woodBiomass > 5.0) {
                        double cupelFuelKg = (actualExtractedPreciousKg * PRECIOUS_CUPELLATION_ENTHALPY_MJ_PER_KG) / (18.5 * 0.20);
                        double woodDeduction = Math.min(woodBiomass * 0.05, cupelFuelKg);
                        cell.setBiomassNatural(Math.max(0.0, woodBiomass - woodDeduction));
                    }
                    cell.setResourcePreciousMetal(Math.max(0.0, rawPreciousOre - actualExtractedPreciousKg));

                    double curCap = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
                    if (tech >= 3.8) {
                        // Modern industrial regime: precious metals form high-reliability microelectronics & catalytic capital
                        cell.setResourceCapital(curCap + actualExtractedPreciousKg * 3.0);
                    } else {
                        // Pre-industrial monetary regime: accumulates in Sovereign Treasury & commercial specie
                        if (cell.getOwner() != null) {
                            cell.getOwner().setTreasuryBullion(cell.getOwner().getTreasuryBullion() + actualExtractedPreciousKg);
                        }
                        double curTollSpecie = cell.getTransitTollRevenue() != null ? cell.getTransitTollRevenue() : 0.0;
                        cell.setTransitTollRevenue(curTollSpecie + actualExtractedPreciousKg);

                        // Fisher Monetary Balance & Dutch Disease (Potosi / Spanish Price Revolution):
                        // Moderate specie monetizes local markets, facilitating artisan division of labor (+capital).
                        // Excessive bullion relative to real physical output (Capital + Food) triggers hyperinflation & Dutch Disease!
                        double foodRes = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
                        double bullionIntensity = (curTollSpecie + actualExtractedPreciousKg) / Math.max(10.0, curCap + foodRes * 0.5);
                        if (bullionIntensity <= 1.2) {
                            // Healthy monetization: lowers local barter friction, enabling artisan capital accumulation
                            cell.setResourceCapital(curCap + actualExtractedPreciousKg * 1.5);
                        } else {
                            // Dutch Disease / Bullion Inflation: excess specie crowds out local productive manufacturing
                            // and spikes wealth inequality (Gini)
                            double inflationPenalty = Math.clamp((bullionIntensity - 1.2) * 0.08 * dtSafe, 0.0, 0.15);
                            cell.setResourceCapital(Math.max(0.0, curCap * (1.0 - inflationPenalty)));
                            double curGini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.30;
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


