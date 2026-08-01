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
 * Ocean Acidification & Coral Bleaching Engine.
 * Models:
 * 1. Oceanic absorption of CO₂ lowering ocean pH.
 * 2. Degradation of intertidal biomes (BEACH) and coral bleaching, reducing marine fish stocks (biomassFish).
 *
 * @author Silvere Martin-Michiellot
 * @version 2.8.0
 */
public class OceanAcidificationEngine {
    private static final Logger logger = LoggerFactory.getLogger(OceanAcidificationEngine.class);

    /**
     * Executes one ocean acidification tick.
     *
     * @param cells   List of H3 cells
     * @param co2Ppm  Global atmospheric CO₂ concentration in ppm
     */
    public static void processOceanAcidification(List<H3Cell> cells, double co2Ppm) {
        if (cells == null || cells.isEmpty()) return;

        // Baseline CO₂ = 280 ppm. Acidification occurs when CO₂ > 400 ppm
        if (co2Ppm <= 400.0) return;

        double acidificationFactor = (co2Ppm - 400.0) / 600.0; // 0.0 to 1.0
        int impactedCells = 0;

        for (H3Cell cell : cells) {
            Biome biome = cell.getBiome();
            if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.BEACH) {
                impactedCells++;
                double fish = cell.getBiomassFish() != null ? cell.getBiomassFish() : 500.0;
                // Acidification & coral reef degradation reduces fish nursery capacity
                double updatedFish = Math.max(0.0, fish * (1.0 - 0.05 * acidificationFactor));
                cell.setBiomassFish(updatedFish);
            }
        }

        if (impactedCells > 0 && acidificationFactor > 0.1) {
            logger.info("Ocean Acidification Engine: Marine stock degradation across {} ocean/coastal cells (CO2: {} ppm).",
                    impactedCells, String.format("%.1f", co2Ppm));
        }
    }
}
