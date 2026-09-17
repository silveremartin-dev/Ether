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
 * Deforestation Soil Erosion & Root Retention Loss Engine.
 * Models:
 * 1. <b>Root Retention Loss</b>: Clear-cutting timber biomass removes root soil binding.
 * 2. <b>Runoff Soil Carbon Washout</b>: Rainfall on deforested slopes washes away soil organic carbon (SOC).
 * 3. <b>Arid Biome Degradation</b>: Severe SOC loss converts fertile forest/hills into barren desert terrain.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class DeforestationErosionEngine {
    private static final Logger logger = LoggerFactory.getLogger(DeforestationErosionEngine.class);

    /**
     * Executes one deforestation soil erosion tick across cells.
     */
    public static void processDeforestationErosion(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int erodedCells = 0;

        for (H3Cell cell : cells) {
            double naturalBiomass = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 500.0;
            double soc = cell.getSoilOrganicCarbon() != null ? cell.getSoilOrganicCarbon() : 20.0;
            double rain = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
            double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;

            // Severe deforestation (biomass < 100.0) on sloped terrain with high rainfall causes erosion
            if (naturalBiomass < 100.0 && elev > 200.0 && rain > 400.0) {
                erodedCells++;
                double socLoss = (rain / 1000.0) * 0.5;
                cell.setSoilOrganicCarbon(Math.max(2.0, soc - socLoss));

                // Soil degradation converts hills to arid desert if SOC < 5.0
                if (cell.getSoilOrganicCarbon() < 5.0 && cell.getBiome() == Biome.HILLS) {
                    cell.setBiome(Biome.DESERT);
                }
            }
        }

        if (erodedCells > 0) {
            logger.info("Deforestation Erosion Engine: Soil carbon runoff active across {} deforested sloped cells.", erodedCells);
        }
    }
}

