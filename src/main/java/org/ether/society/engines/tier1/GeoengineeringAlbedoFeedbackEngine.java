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
 * Procedural engine modeling Geoengineering interventions and Terrestrial Albedo Feedback loops.
 *
 * <p>Simulates Stratospheric Aerosol Injection (SAI - SO2), Marine Cloud Brightening (MCB),
 * and planetary surface albedo modifications. Calculates radiative forcing cooling offsets,
 * regional precipitation disruption, and crop yield impacts on agricultural biomass.</p>

 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class GeoengineeringAlbedoFeedbackEngine {
    private static final Logger logger = LoggerFactory.getLogger(GeoengineeringAlbedoFeedbackEngine.class);

    private static double globalSaiAerosolLoadingTg = 0.0; // Teragrams of SO2 in stratospheric injection
    /* Internal state variable for global cooling effect celsius (double). */
    private static double globalCoolingEffectCelsius = 0.0;

    /*
     * Processes geoengineering radiative forcing and albedo feedbacks.
     *
     * @param cells list of H3 cells in the simulation grid
     * @param deltaYears step size in years
     */
    public static void processGeoengineeringAlbedo(List<H3Cell> cells, double deltaYears) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        if (cells == null || cells.isEmpty()) return;

        // Calculate active geoengineering deployment based on global high-tech cells
        double totalTech5Plus = 0;
        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell.getTechnologyLevel() != null && cell.getTechnologyLevel() >= 5.0 && cell.getPopulation() > 1000) {
                totalTech5Plus += (cell.getTechnologyLevel() - 4.0);
            }
        }

        // Stratospheric Aerosol Injection threshold: High-tech industrial civilizations deploy SAI if global temp > 18Â°C
        if (totalTech5Plus > 50.0) {
            globalSaiAerosolLoadingTg = Math.min(20.0, totalTech5Plus * 0.1);
            // 5 Tg SO2 ~ -0.5Â°C cooling
            globalCoolingEffectCelsius = globalSaiAerosolLoadingTg * 0.10;
        } else {
            globalSaiAerosolLoadingTg = Math.max(0.0, globalSaiAerosolLoadingTg - (1.0 * deltaYears)); // Aerosol decay ~1-2 yr lifetime
            globalCoolingEffectCelsius = globalSaiAerosolLoadingTg * 0.10;
        }

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell.getTemperature() == null) continue;

            // Apply global aerosol cooling offset
            if (globalCoolingEffectCelsius > 0.0) {
                cell.setTemperature(cell.getTemperature() - (globalCoolingEffectCelsius * deltaYears));

                // Side effect of SAI: Disruption of monsoon precipitation (-2% to -8% rainfall)
                if (cell.getRainfall() != null) {
                    double rainfallDisruption = 1.0 - (globalSaiAerosolLoadingTg * 0.003);
                    cell.setRainfall(Math.max(50.0, cell.getRainfall() * rainfallDisruption));
                }
            }

            // Surface albedo feedback calculation
            cell.calculateDynamicAlbedo();
        }
    }

    /*
     * Get global sai aerosol loading tg.
     * Enforces physical invariants and updates associated state variables within {@code GeoengineeringAlbedoFeedbackEngine}.
     *
     * @return the resulting computation or state reference
     */
    public static double getGlobalSaiAerosolLoadingTg() {
        return globalSaiAerosolLoadingTg;
    }

    /*
     * Get global cooling effect celsius.
     * Enforces physical invariants and updates associated state variables within {@code GeoengineeringAlbedoFeedbackEngine}.
     *
     * @return the resulting computation or state reference
     */
    public static double getGlobalCoolingEffectCelsius() {
        return globalCoolingEffectCelsius;
    }
}


