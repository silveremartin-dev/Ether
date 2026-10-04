/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Cliodynamic Cultural Sociology & Values Vector Engine.
 * Models societal value vectors (Collectivism vs Individualism, Environmental Stewardship, Risk Aversion)
 * and their physical impact on per-capita energy footprints and soil maintenance labor.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class CulturalSociologyEngine {
    private static final Logger logger = LoggerFactory.getLogger(CulturalSociologyEngine.class);

    private static double globalCollectivismIndex = 0.5; // 0.0 Individualist -> 1.0 Collectivist
    private static double globalEnvironmentalStewardship = 0.5; // 0.0 Exploitative -> 1.0 Sustainable

    /*
     * Process cultural sociology.
     * Enforces physical invariants and updates associated state variables within {@code CulturalSociologyEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processCulturalSociology(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            // Environmental stewardship allocates labor to Soil Organic Carbon (SOC) preservation
            if (globalEnvironmentalStewardship > 0.6) {
                double soc = cell.getSoilOrganicCarbon() != null ? cell.getSoilOrganicCarbon() : 0.0;
                cell.setSoilOrganicCarbon(soc + (globalEnvironmentalStewardship - 0.5) * 0.1 * deltaYears);
            }

            // High collectivism reduces per-capita luxury energy dissipation and pollution
            if (globalCollectivismIndex > 0.6 && cell.getPollutionLevel() != null) {
                double pollution = cell.getPollutionLevel();
                cell.setPollutionLevel(Math.max(0.0, pollution - (globalCollectivismIndex - 0.5) * 0.5 * deltaYears));
            }
        }
    }

    // Getters and Setters
    /*
     * Get global collectivism index.
     * Enforces physical invariants and updates associated state variables within {@code CulturalSociologyEngine}.
     *
     * @return the resulting computation or state reference
     */
    public static double getGlobalCollectivismIndex() { return globalCollectivismIndex; }
    public static void setGlobalCollectivismIndex(double idx) { globalCollectivismIndex = Math.max(0.0, Math.min(1.0, idx)); }

    /*
     * Get global environmental stewardship.
     * Enforces physical invariants and updates associated state variables within {@code CulturalSociologyEngine}.
     *
     * @return the resulting computation or state reference
     */
    public static double getGlobalEnvironmentalStewardship() { return globalEnvironmentalStewardship; }
    public static void setGlobalEnvironmentalStewardship(double stw) { globalEnvironmentalStewardship = Math.max(0.0, Math.min(1.0, stw)); }
}


