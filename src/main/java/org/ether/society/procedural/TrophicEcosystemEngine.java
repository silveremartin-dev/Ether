/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

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
 * @version 4.0.0
 */
public class TrophicEcosystemEngine {
    private static final Logger logger = LoggerFactory.getLogger(TrophicEcosystemEngine.class);

    private static boolean pleistoceneRewildingActive = false;
    private static double megafaunaDensityPerKm2 = 0.0;
    private static double globalBiodiversityIndex = 1.0; // 0.0 (total extinction) to 1.0 (intact)

    public static void processTrophicEcosystem(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            double naturalBiomass = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 0.0;

            // Rewilding effect: Megafauna trampling snow increases winter soil cooling,
            // preserving permafrost and sequestering Soil Organic Carbon (SOC).
            if (pleistoceneRewildingActive && cell.getLatitude() > 50.0) {
                double SOC = cell.getSoilOrganicCarbon() != null ? cell.getSoilOrganicCarbon() : 0.0;
                cell.setSoilOrganicCarbon(SOC + megafaunaDensityPerKm2 * 0.05 * deltaYears);

                // Slight albedo increase due to grassland maintenance vs dark shrub encroachment
                double albedo = cell.getDynamicAlbedo() != null ? cell.getDynamicAlbedo() : 0.30;
                cell.setDynamicAlbedo(Math.min(0.80, albedo + 0.001 * megafaunaDensityPerKm2));
            }

            // Biodiversity loss degrades natural ecosystem resilience
            if (globalBiodiversityIndex < 0.5) {
                cell.setBiomassNatural(Math.max(0.0, naturalBiomass * (1.0 - (0.5 - globalBiodiversityIndex) * 0.1 * deltaYears)));
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
}
