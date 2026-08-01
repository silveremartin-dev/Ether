/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Mesopotamian Irrigation & Fertile Crescent Salinization Engine Variant B33.1 (Pure) & B33.2 (Hybrid).
 * Models progressive topsoil salinization in canal-irrigated alluvial river basins (Tigris/Euphrates):
 * 1. High evaporation deposits sodium salts (Na+) in topsoil.
 * 2. Crop yield degradation forces shift from salt-sensitive wheat to salt-tolerant barley.
 * 3. Severe salinization causes agricultural crash and historical shift of civilizational center north.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class FertileCrescentSalinizationEngine {
    private static final Logger logger = LoggerFactory.getLogger(FertileCrescentSalinizationEngine.class);

    private static double salinizationRatePerCentury = 0.05; // 5% soil sodium buildup per century of irrigation

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            double food = cell.getFoodResource() != null ? cell.getFoodResource() : 1000.0;
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;

            // Early agrarian irrigation (Tech level 1.0 to 3.0) in arid/plains biomes
            if (tech >= 1.0 && tech <= 3.5) {
                // Progressive salinization degrades food output over time
                double salinizationFactor = Math.max(0.20, 1.0 - (salinizationRatePerCentury * (deltaYears / 100.0)));
                cell.setFoodResource(food * salinizationFactor);
            }
        }
    }

    public static double getSalinizationRatePerCentury() { return salinizationRatePerCentury; }
    public static void setSalinizationRatePerCentury(double rate) { salinizationRatePerCentury = Math.max(0.0, rate); }
}
