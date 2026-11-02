/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Trophic Ecosystem, Rewilding & Biodiversity Engine.
 * Models 3-tier trophic biomass dynamics (Producers -> Herbivores -> Predators),
 * Pleistocene Rewilding (Mammoth/Bison surrogates maintaining permafrost albedo & SOC),
 * and species hybridization vs extinction dynamics.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class TrophicEcosystemEngine {
    private static final Logger logger = LoggerFactory.getLogger(TrophicEcosystemEngine.class);

    /** Internal state: pleistocene rewilding active (boolean). */
    private static boolean pleistoceneRewildingActive = false;
    /** Internal state: megafauna density per km2 (double). */
    private static double megafaunaDensityPerKm2 = 0.0;
    private static double globalBiodiversityIndex = 1.0; // 0.0 (total extinction) to 1.0 (intact)

    /**
     * Process trophic ecosystem.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processTrophicEcosystem(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        if (!pleistoceneRewildingActive && globalBiodiversityIndex >= 0.5) {
            return;
        }

        double dtSafe = Math.max(0.0001, deltaYears);

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null) continue;

            double naturalBiomass = cell.getBiomassNaturalOr(0.0);

            // Rewilding effect: Megafauna trampling snow increases winter soil cooling,
            // preserving permafrost and sequestering Soil Organic Carbon (SOC) in cold/periglacial biomes.
            double temp = cell.getTemperatureOr(10.0);
            double absLat = cell.getLatitude() != null ? Math.abs(cell.getLatitude()) : 0.0;
            if (pleistoceneRewildingActive && (temp <= 4.0 || absLat > 45.0)) {
                double soc = cell.getSoilOrganicCarbonOr(0.0);
                cell.setSoilOrganicCarbon(soc + megafaunaDensityPerKm2 * 0.05 * dtSafe);

                // Grassland albedo maintenance vs dark shrub encroachment, bounded by physical optics
                double baseAlbedo = cell.calculateDynamicAlbedo();
                double rewildingAlbedoBonus = Math.min(0.04, 0.002 * megafaunaDensityPerKm2);
                cell.setDynamicAlbedo(Math.min(0.80, baseAlbedo + rewildingAlbedoBonus));
            }

            // Biodiversity loss degrades natural ecosystem resilience
            if (globalBiodiversityIndex < 0.5) {
                cell.setBiomassNatural(Math.max(0.0, naturalBiomass * (1.0 - (0.5 - globalBiodiversityIndex) * 0.1 * dtSafe)));
            }
        }
    }

    // Getters and Setters
    public static boolean isPleistoceneRewildingActive() { return pleistoceneRewildingActive; }
    public static void setPleistoceneRewildingActive(boolean active) { pleistoceneRewildingActive = active; }

    public static double getMegafaunaDensityPerKm2() { return megafaunaDensityPerKm2; }
    public static void setMegafaunaDensityPerKm2(double density) { megafaunaDensityPerKm2 = Math.max(0.0, density); }

    public static double getGlobalBiodiversityIndex() { return globalBiodiversityIndex; }
    public static void setGlobalBiodiversityIndex(double index) { globalBiodiversityIndex = Math.max(0.0, Math.min(1.0, index)); }

    public static void resetState() {
        pleistoceneRewildingActive = false;
        megafaunaDensityPerKm2 = 0.0;
        globalBiodiversityIndex = 1.0;
    }
}
