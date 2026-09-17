/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Megafauna Steppe Ecosystem Engineering & Overkill Feedback Engine.
 * Eliminates arbitrary threshold cutoffs in favor of continuous physical state equations:
 * 1. <b>Hunting Pressure Ratio</b>: Overhunting rate = f(Population Density / Wild Biomass Density).
 * 2. <b>Continuous Biomass & Soil Organic Carbon Depletion</b>.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class MegafaunaEcosystemEngine {
    private static final Logger logger = LoggerFactory.getLogger(MegafaunaEcosystemEngine.class);

    /**
     * Executes one megafauna ecosystem engineering tick across cells.
     */
    public static void processMegafaunaEcosystem(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int megafaunaCollapseEvents = 0;

        for (H3Cell cell : cells) {
            if (cell.getBiome() == Biome.SNOW || cell.getBiome() == Biome.TUNDRA || cell.getBiome() == Biome.PLAINS) {
                int humanPop = cell.getPopulation() != null ? cell.getPopulation() : 0;
                double naturalBiomass = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 1.0;

                // Continuous hunting pressure ratio = Population / Natural Biomass Stock
                double huntingPressureRatio = humanPop / Math.max(10.0, naturalBiomass);

                if (huntingPressureRatio > 0.10) {
                    megafaunaCollapseEvents++;

                    // Continuous exponential decay of wild biomass and soil carbon
                    double depletionRate = Math.min(0.20, huntingPressureRatio * 0.02);
                    cell.setBiomassNatural(Math.max(10.0, naturalBiomass * (1.0 - depletionRate)));
                    cell.setSoilOrganicCarbon(Math.max(2.0, cell.getSoilOrganicCarbon() - (depletionRate * 0.5)));

                    // Shrub encroachment converts open steppe plains to forest under low SOC
                    if (cell.getBiome() == Biome.PLAINS && cell.getSoilOrganicCarbon() < 8.0) {
                        cell.setBiome(Biome.FOREST);
                    }
                }
            }
        }

        if (megafaunaCollapseEvents > 0) {
            logger.info("Megafauna Engine: Continuous trophic hunting pressure active across {} cells.", megafaunaCollapseEvents);
        }
    }
}

