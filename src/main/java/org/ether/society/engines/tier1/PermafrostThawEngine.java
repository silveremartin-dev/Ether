/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Permafrost Thaw & Methane (CHâ‚„) Outgassing Engine.
 * Models:
 * 1. Polar & tundra permafrost thaw when temperature exceeds 0Â°C.
 * 2. Methane gas (CHâ‚„) release into atmosphere, accelerating global greenhouse warming.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PermafrostThawEngine extends AbstractSimulationEngine {
    public PermafrostThawEngine() {
        super("PermafrostThawEngine", "PermafrostThawEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(PermafrostThawEngine.class);

    /**
     * Executes one permafrost thaw update step with default monthly step.
     *
     * @param cells List of H3 cells
     * @return Total methane outgassing amount in abstract units (ppm equivalent)
     */
    public static double processPermafrostThaw(List<H3Cell> cells) {
        return processPermafrostThaw(cells, 1.0 / 12.0);
    }

    /**
     * Executes one permafrost thaw update step scaled by dtYears.
     *
     * @param cells List of H3 cells
     * @param dtYears Time step in years
     * @return Total methane outgassing amount in abstract units (ppm equivalent)
     */
    public static double processPermafrostThaw(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty()) return 0.0;

        double dtSafe = Math.max(0.0001, dtYears);
        double totalMethaneOutgassing = 0.0;
        int thawingCells = 0;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            Biome biome = cell.getBiome();
            double temp = cell.getTemperature() != null ? cell.getTemperature() : -10.0;

            // Polar / Tundra permafrost thawing condition (T > 0A°C)
            if ((biome == Biome.SNOW || biome == Biome.TUNDRA) && temp > 0.0) {
                thawingCells++;
                double methaneRelease = temp * 0.15 * dtSafe; // Methane release proportional to temperature excess and time step
                totalMethaneOutgassing += methaneRelease;

                // Ground thaw converts tundra to wetland / taiga, boosting soil organic carbon loss
                double soc = cell.getSoilOrganicCarbonOr(20.0);
                cell.setSoilOrganicCarbon(Math.max(5.0, soc - 0.2 * dtSafe));
            }
        }

        if (thawingCells > 0) {
            logger.info("Permafrost Engine: Thawing permafrost across {} polar/tundra cells released {} ppm CH4 equivalent.",
                    thawingCells, String.format(java.util.Locale.ROOT, "%.2f", totalMethaneOutgassing));
        }

        return totalMethaneOutgassing;
    }
}
