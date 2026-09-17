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
 * Dust Storm & Desertification Engine.
 * Models:
 * 1. Severe topsoil loss & overgrazing in arid biomes generating airborne dust storms.
 * 2. Downwind particulate dispersion reducing solar energy, air quality, and crop yields in neighboring cells.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class DustStormEngine {
    private static final Logger logger = LoggerFactory.getLogger(DustStormEngine.class);

    /**
     * Executes one dust storm generation and dispersion step.
     */
    public static void processDustStorms(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int stormSources = 0;
        int count = cells.size();

        for (int i = 0; i < count; i++) {
            H3Cell src = cells.get(i);
            if (src.getBiome() != Biome.DESERT && src.getSoilOrganicCarbon() > 10.0) continue;

            double rain = src.getRainfall() != null ? src.getRainfall() : 0.0;
            double temp = src.getTemperature() != null ? src.getTemperature() : 15.0;

            // Arid conditions with high thermal convective lift generate dust storms
            if (rain < 150.0 && temp > 30.0) {
                stormSources++;

                // Downwind particulate dispersion to neighboring cells
                int range = Math.min(count, 10);
                int start = Math.max(0, i - range / 2);
                int end = Math.min(count - 1, i + range / 2);

                for (int j = start; j <= end; j++) {
                    if (j != i) {
                        H3Cell target = cells.get(j);
                        // Dust deposition reduces solar energy & agricultural yield
                        target.setEnergySolar(Math.max(0.0, target.getEnergySolar() * 0.85));
                        target.setBiomassAgriculture(Math.max(0.0, target.getBiomassAgriculture() * 0.92));
                    }
                }
            }
        }

        if (stormSources > 0) {
            logger.info("DustStorm Engine: Sand and particulate dust storms generated from {} desert cells.", stormSources);
        }
    }
}

