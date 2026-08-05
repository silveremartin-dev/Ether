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
 * Procedural engine modeling dynamic hydrographic flows, river sediment transport,
 * and harbor siltation (historical harbour siltation such as Ephesus and Ostia).
 *
 * <p>Calculates river discharge based on upstream rainfall and deforestation erosion,
 * sediment deposition at estuarine coastlines, and updates navigation movement friction
 * as harbours envase over multi-century timescales.</p>

 * @author Silvere Martin-Michiellot
 * @version 4.1.0
 */
public class DynamicHydrographicSiltationEngine {
    private static final Logger logger = LoggerFactory.getLogger(DynamicHydrographicSiltationEngine.class);

    /**
     * Processes river discharge, sediment erosion, and estuarine harbor siltation.
     *
     * @param cells list of H3 cells in the simulation grid
     * @param deltaYears step size in years
     */
    public static void processHydrographicSiltation(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell.getElevation() == null) continue;

            // Coastal / Estuary cells (elevation near sea level: -10m to +25m)
            boolean isEstuaryOrCoast = cell.getElevation() >= -10.0 && cell.getElevation() <= 25.0
                    && (cell.getBiome() == Biome.BEACH || cell.getBiome() == Biome.PLAINS);

            if (isEstuaryOrCoast) {
                // High rainfall + depleted biomassNatural -> high soil erosion & siltation rate
                double rainfall = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
                double naturalVeg = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 500.0;
                double erosionIndex = Math.max(0.1, (rainfall / 1000.0) * (1.0 - Math.clamp(naturalVeg / 1000.0, 0.0, 1.0)));

                // Silt accumulation increases land elevation slightly and increases harbor friction
                double siltationIncrement = erosionIndex * 0.05 * deltaYears; // meters of sediment per year
                cell.setElevation(cell.getElevation() + siltationIncrement);

                // Movement friction penalty: Harbor siltation increases maritime navigation cost
                double currentFriction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
                if (siltationIncrement > 0.01) {
                    double newFriction = Math.min(15.0, currentFriction + (siltationIncrement * 2.0));
                    cell.setMovementFriction(newFriction);
                }
            }
        }
    }
}
