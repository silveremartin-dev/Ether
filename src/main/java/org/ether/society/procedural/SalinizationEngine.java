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
 * Soil & Groundwater Salinization Simulation Engine.
 * Models:
 * 1. Coastal saltwater intrusion from sea-level surges.
 * 2. Agricultural soil salinization from over-irrigation in arid/semi-arid regions,
 *    reducing agricultural biomass yields and soil carbon fertility.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.8.0
 */
public class SalinizationEngine {
    private static final Logger logger = LoggerFactory.getLogger(SalinizationEngine.class);

    /**
     * Executes one salinization update step across cells.
     */
    public static void processSalinization(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int salinizedCells = 0;

        for (H3Cell cell : cells) {
            if (cell.getElevation() <= 0 || cell.getBiome() == Biome.OCEAN || cell.getBiome() == Biome.DEEP_OCEAN) {
                continue;
            }

            double rain = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
            double temp = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
            double aquifer = cell.getAccessibleAquifer() != null ? cell.getAccessibleAquifer() : 0.0;
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;

            // 1. Over-irrigation in arid climates (high temp, low rain, high pop)
            if (rain < 350.0 && temp > 25.0 && pop > 100 && aquifer < 500.0) {
                salinizedCells++;
                // Salt accumulation degrades crop yield and soil organic carbon
                cell.setBiomassAgriculture(Math.max(0.0, cell.getBiomassAgriculture() * 0.94));
                cell.setSoilOrganicCarbon(Math.max(5.0, cell.getSoilOrganicCarbon() - 0.3));
            }

            // 2. Coastal saltwater intrusion (BEACH & low elevation < 10m)
            if (cell.getBiome() == Biome.BEACH && cell.getElevation() < 10.0) {
                salinizedCells++;
                cell.setWaterResource(Math.max(10.0, cell.getWaterResource() * 0.90));
            }
        }

        if (salinizedCells > 0) {
            logger.info("Salinization Engine: Soil salinization affecting {} cells.", salinizedCells);
        }
    }
}
