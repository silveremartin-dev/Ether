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
 * Permafrost Thaw & Methane (CH₄) Outgassing Engine.
 * Models:
 * 1. Polar & tundra permafrost thaw when temperature exceeds 0°C.
 * 2. Methane gas (CH₄) release into atmosphere, accelerating global greenhouse warming.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class PermafrostThawEngine {
    private static final Logger logger = LoggerFactory.getLogger(PermafrostThawEngine.class);

    /**
     * Executes one permafrost thaw update step.
     *
     * @param cells List of H3 cells
     * @return Total methane outgassing amount in abstract units (ppm equivalent)
     */
    public static double processPermafrostThaw(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return 0.0;

        double totalMethaneOutgassing = 0.0;
        int thawingCells = 0;

        for (H3Cell cell : cells) {
            Biome biome = cell.getBiome();
            double temp = cell.getTemperature() != null ? cell.getTemperature() : -10.0;

            // Polar / Tundra permafrost thawing condition (T > 0°C)
            if ((biome == Biome.SNOW || biome == Biome.TUNDRA) && temp > 0.0) {
                thawingCells++;
                double methaneRelease = temp * 0.15; // Methane release proportional to temperature excess
                totalMethaneOutgassing += methaneRelease;

                // Ground thaw converts tundra to wetland / taiga, boosting soil organic carbon loss
                cell.setSoilOrganicCarbon(Math.max(5.0, cell.getSoilOrganicCarbon() - 0.2));
            }
        }

        if (thawingCells > 0) {
            logger.info("Permafrost Engine: Thawing permafrost across {} polar/tundra cells released {} ppm CH4 equivalent.",
                    thawingCells, String.format("%.2f", totalMethaneOutgassing));
        }

        return totalMethaneOutgassing;
    }
}

