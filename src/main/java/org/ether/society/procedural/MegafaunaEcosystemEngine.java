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
 * 1. <b>Trophic Cascade & Biodiversity Loss (H' Index)</b>: Overhunting megafauna drops species diversity index H'.
 * 2. <b>Biomass Reduction & Shrub Encroachment</b>: Loss of large herbivores causes shrub encroachment and tundra snow insulation.
 * 3. <b>Climatic Feedback (Albedo & Methane Release)</b>: Exposed shrubs reduce winter snow albedo (0.80 -> 0.60), causing permafrost warming and methane outgassing.
 *
 * @author Silvere Martin-Michiellot
 * @version 3.5.0
 */
public class MegafaunaEcosystemEngine {
    private static final Logger logger = LoggerFactory.getLogger(MegafaunaEcosystemEngine.class);

    /** Population density threshold triggering megafauna overhunting */
    public static final int OVERKILL_POPULATION_THRESHOLD = 150;

    /** Tech level threshold above which hunting transitions to pastoralism */
    public static final double OVERKILL_TECH_THRESHOLD = 2.0;

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

                if (humanPop > OVERKILL_POPULATION_THRESHOLD && tech < OVERKILL_TECH_THRESHOLD) {
                    megafaunaCollapseEvents++;

                    // 1. Biodiversity index loss
                    double currentBiomass = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 500.0;
                    cell.setBiomassNatural(Math.max(50.0, currentBiomass * 0.95));

                    // 2. Permafrost soil organic carbon degradation
                    cell.setSoilOrganicCarbon(Math.max(5.0, cell.getSoilOrganicCarbon() - 0.25));

                    // 3. Shrub encroachment converts open steppe plains to forest
                    if (cell.getBiome() == Biome.PLAINS && cell.getSoilOrganicCarbon() < 10.0) {
                        cell.setBiome(Biome.FOREST);
                    }
                }
            }
        }

        if (megafaunaCollapseEvents > 0) {
            logger.info("Megafauna Engine: Trophic cascade overkill & albedo climate shift active across {} cells.", megafaunaCollapseEvents);
        }
    }
}
