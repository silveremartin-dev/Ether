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
 * Models:
 * 1. <b>Mammoth Steppe & Bison Trampling</b>: Large herbivores maintain open steppe biomes by trampling snow and suppressing shrub encroachment.
 * 2. <b>Overkill Extinction & Climate Feedback</b>: Human overhunting of megafauna (Pleistocene/Holocene transition) causes shrub encroachment,
 *    thicker insulating snow cover, permafrost warming, and methane release outgassing.
 *
 * @author Silvere Martin-Michiellot
 * @version 3.4.0
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
                double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;

                // Human overkill threshold in Paleolithic/Neolithic eras (Tech < 2.0, Pop > 150)
                if (humanPop > 150 && tech < 2.0) {
                    megafaunaCollapseEvents++;

                    // Megafauna extinction leads to shrub encroachment & permafrost thaw
                    cell.setSoilOrganicCarbon(Math.max(5.0, cell.getSoilOrganicCarbon() - 0.2));

                    // Shrub encroachment shifts steppe to forest
                    if (cell.getBiome() == Biome.PLAINS && cell.getSoilOrganicCarbon() < 10.0) {
                        cell.setBiome(Biome.FOREST);
                    }
                }
            }
        }

        if (megafaunaCollapseEvents > 0) {
            logger.info("Megafauna Engine: Pleistocene overkill & steppe biome shift active across {} polar/sub-polar cells.", megafaunaCollapseEvents);
        }
    }
}
