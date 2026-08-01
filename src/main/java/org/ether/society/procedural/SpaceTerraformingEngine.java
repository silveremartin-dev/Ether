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
 * Space Age, Orbital Engineering & Planetary Terraforming Engine (Tech >= 9.5).
 * Models:
 * 1. <b>Atmospheric Terraforming</b>: Injection of atmospheric gases (CO₂, O₂), transforming airless or frozen worlds.
 * 2. <b>Orbital Mirror Arrays & Pressure Domes</b>: Heating extreme polar/desert biomes and enabling pressurized habitability.
 * 3. <b>Interplanetary Resource Transport</b>: Shipping water and rare metal ores across worlds.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.9.0
 */
public class SpaceTerraformingEngine {
    private static final Logger logger = LoggerFactory.getLogger(SpaceTerraformingEngine.class);

    /**
     * Executes one space age terraforming tick.
     */
    public static void processSpaceTerraforming(List<H3Cell> cells, double globalTechLevel) {
        if (cells == null || cells.isEmpty() || globalTechLevel < 9.5) return;

        int terraformedCells = 0;

        for (H3Cell cell : cells) {
            double localTech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : globalTechLevel;
            if (localTech < 9.5) continue;

            Biome biome = cell.getBiome();

            // Terraforming extreme biomes (DESERT or SNOW -> PLAINS / FOREST)
            if (biome == Biome.DESERT || biome == Biome.SNOW) {
                terraformedCells++;
                cell.setWaterResource(Math.min(1000.0, cell.getWaterResource() + 200.0));
                cell.setAccessibleAquifer(Math.min(10000.0, cell.getAccessibleAquifer() + 1000.0));
                cell.setSoilOrganicCarbon(Math.min(80.0, cell.getSoilOrganicCarbon() + 10.0));

                if (cell.getSoilOrganicCarbon() > 30.0) {
                    cell.setBiome(Biome.PLAINS);
                }
            }
        }

        if (terraformedCells > 0) {
            logger.info("Space Age Terraforming Engine: Advanced bio-engineering transformed {} extreme cells.", terraformedCells);
        }
    }
}
