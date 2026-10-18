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
 * 1. <b>Specific Smelting Enthalpy (Î”H_smelt = +24.7 MJ/kg Fe)</b>: Required energy input to reduce raw iron oxide ore (Feâ‚‚Oâ‚ƒ) into pure metallic iron.
 * 2. <b>Fuel Consumption (Charcoal/Coal)</b>: Conversion efficiency depends on furnace temperature and technology era.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class MetallurgyEnthalpyEngine {
    private static final Logger logger = LoggerFactory.getLogger(MetallurgyEnthalpyEngine.class);

    /* Specific smelting enthalpy for Iron Oxide reduction in MJ/kg */
    public static final double IRON_SMELTING_ENTHALPY_MJ_PER_KG = 24.7;
    /* Specific thermal cupellation and refining enthalpy for precious bullion ore in MJ/kg */
    public static final double PRECIOUS_CUPELLATION_ENTHALPY_MJ_PER_KG = 12.0;

    /*
     * Executes one chemical ore smelting tick across cells using default monthly step.
     */
    public static void processOreSmelting(List<H3Cell> cells) {
        processOreSmelting(cells, 1.0 / 12.0);
    }

    /*
     * Executes chemical ore smelting constrained by labor throughput, fuel kinetics, and dtYears.
     */
    public static void processOreSmelting(List<H3Cell> cells, double dtYears) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        if (cells == null || cells.isEmpty()) return;
        double dtSafe = Math.max(0.0001, dtYears);

        int smeltingHubs = 0;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double rawMetalOre = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
            double woodBiomass = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 0.0;
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;

            // Smelting requires raw metal ore and wood/charcoal fuel (Tech >= 2.5)
            if (tech >= 2.5 && rawMetalOre > 1.0 && woodBiomass > 20.0) {
                // 1. Labor constraint: fraction of population dedicated to mining & metallurgy
                double metallurgists = pop * Math.clamp(0.005 * tech, 0.005, 0.05);
                double annualThroughputKgPerWorker = 100.0 * Math.clamp(Math.pow(tech / 2.5, 2.0), 1.0, 50.0);
                double maxLaborRefinedKg = metallurgists * annualThroughputKgPerWorker * dtSafe;

                // 2. Fuel harvesting constraint: sustainable logging rate per tick
                double maxFuelHarvestKg = Math.min(woodBiomass * 0.10 * (dtSafe * 12.0), woodBiomass * 0.40);
                double availableSmeltingEnergyMJ = maxFuelHarvestKg * 18.5 * 0.15; // 18.5 MJ/kg wood, 15% bloomery furnace efficiency
                double maxFuelRefinedKg = availableSmeltingEnergyMJ / IRON_SMELTING_ENTHALPY_MJ_PER_KG;

                // 3. Thermodynamic reduction bounded by ore, labor, and fuel
                double actualRefinedMetal = Math.min(rawMetalOre, Math.min(maxLaborRefinedKg, maxFuelRefinedKg));
                if (actualRefinedMetal < 0.001) continue;

                smeltingHubs++;

                // 4. Exact stoichiometric mass conservation
                double woodConsumedKg = (actualRefinedMetal * IRON_SMELTING_ENTHALPY_MJ_PER_KG) / (18.5 * 0.15);
                cell.setResourceMetal(Math.max(0.0, rawMetalOre - actualRefinedMetal));
                cell.setBiomassNatural(Math.max(0.0, woodBiomass - woodConsumedKg));

                // 5. Expand cell physical capital (tools, weapons, plows)
                // Metal is converted directly into physical durable tooling/infrastructure (1 kg refined metal -> 1 kg durable tool stock)
                // modulated by metallurgical alloy quality (carbon steel, bronze) without arbitrary magic fractions.
                double curCap = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
                double alloyQuality = Math.clamp(0.4 + 0.2 * (tech - 2.5), 0.4, 1.2);
                double durableToolInvestment = actualRefinedMetal * alloyQuality;
                // Physical capital depreciation from mechanical wear and rust/corrosion
                double materialWearRate = Math.max(0.01, 0.05 / Math.max(0.5, tech * 0.4)) * dtSafe;
                double newCapital = Math.max(0.0, (curCap + durableToolInvestment) * (1.0 - materialWearRate));
                cell.setResourceCapital(newCapital);
            }

            // 6. Precious Metal Extraction & Cupellation (Gold, Silver, Electrum)
            // Placer gold panning at tech >= 1.2; deep vein shaft mining and cupellation at tech >= 1.8.
            double rawPreciousOre = cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal() : 0.0;
            if (tech >= 1.2 && rawPreciousOre > 0.05) {
                double preciousMiners = pop * Math.clamp(0.001 * tech, 0.0005, 0.015);
                double extractionRateKgPerWorker = 2.0 * Math.clamp(tech / 1.5, 0.5, 8.0);
                double maxLaborPreciousKg = preciousMiners * extractionRateKgPerWorker * dtSafe;

                double actualExtractedPreciousKg = Math.min(rawPreciousOre, maxLaborPreciousKg);
                if (actualExtractedPreciousKg > 0.0001) {
                    // Thermal cupellation energy (wood fuel) required at tech >= 1.8
                    if (tech >= 1.8 && woodBiomass > 5.0) {
                        double cupelFuelKg = (actualExtractedPreciousKg * PRECIOUS_CUPELLATION_ENTHALPY_MJ_PER_KG) / (18.5 * 0.20);
                        double woodDeduction = Math.min(woodBiomass * 0.05, cupelFuelKg);
                        cell.setBiomassNatural(Math.max(0.0, woodBiomass - woodDeduction));
                    }
                    cell.setResourcePreciousMetal(Math.max(0.0, rawPreciousOre - actualExtractedPreciousKg));

                    // Refined precious bullion increases durable liquid capital stock and lowers transaction friction
                    double curCap = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
                    double bullionValueFactor = Math.clamp(2.5 - 0.2 * (tech - 1.2), 1.0, 3.0);
                    cell.setResourceCapital(curCap + actualExtractedPreciousKg * bullionValueFactor);
                }
            }
        }

        if (smeltingHubs > 0) {
            logger.debug("Metallurgy Engine: Chemical ore smelting active across {} furnace cells.", smeltingHubs);
        }
    }
}


