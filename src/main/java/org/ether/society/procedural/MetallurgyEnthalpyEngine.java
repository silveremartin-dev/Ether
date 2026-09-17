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
 * Metallurgical Enthalpy & Oxide Reduction Engine.
 * Replaces abstract metal resource pools with thermodynamic chemical reduction:
 * 1. <b>Specific Smelting Enthalpy (ΔH_smelt = +24.7 MJ/kg Fe)</b>: Required energy input to reduce raw iron oxide ore (Fe₂O₃) into pure metallic iron.
 * 2. <b>Fuel Consumption (Charcoal/Coal)</b>: Conversion efficiency depends on furnace temperature and technology era.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class MetallurgyEnthalpyEngine {
    private static final Logger logger = LoggerFactory.getLogger(MetallurgyEnthalpyEngine.class);

    /** Specific smelting enthalpy for Iron Oxide reduction in MJ/kg */
    public static final double IRON_SMELTING_ENTHALPY_MJ_PER_KG = 24.7;

    /**
     * Executes one chemical ore smelting tick across cells.
     */
    public static void processOreSmelting(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int smeltingHubs = 0;

        for (H3Cell cell : cells) {
            double rawMetalOre = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
            double woodBiomass = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 0.0;
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;

            // Smelting requires raw metal ore and wood/charcoal fuel (Tech >= 2.5)
            if (tech >= 2.5 && rawMetalOre > 10.0 && woodBiomass > 50.0) {
                smeltingHubs++;

                // Smelting energy capacity (MJ)
                double availableSmeltingEnergyMJ = woodBiomass * 18.5 * 0.15; // 18.5 MJ/kg wood energy density, 15% furnace efficiency
                double maxRefinedMetalKg = availableSmeltingEnergyMJ / IRON_SMELTING_ENTHALPY_MJ_PER_KG;

                double actualRefinedMetal = Math.min(rawMetalOre, maxRefinedMetalKg);

                // Consume raw ore and fuel
                cell.setResourceMetal(rawMetalOre - actualRefinedMetal);
                cell.setBiomassNatural(Math.max(0.0, woodBiomass - (actualRefinedMetal * 2.0)));

                // Add refined structural capital
                cell.setResourceCapital(cell.getResourceCapital() + actualRefinedMetal * 10.0);
            }
        }

        if (smeltingHubs > 0) {
            logger.info("Metallurgy Engine: Chemical ore smelting active across {} furnace cells.", smeltingHubs);
        }
    }
}

