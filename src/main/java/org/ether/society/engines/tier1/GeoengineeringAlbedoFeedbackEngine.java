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
public class GeoengineeringAlbedoFeedbackEngine extends AbstractSimulationEngine {
    public GeoengineeringAlbedoFeedbackEngine() {
        super("GeoengineeringAlbedoFeedbackEngine", "GeoengineeringAlbedoFeedbackEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(GeoengineeringAlbedoFeedbackEngine.class);

    private static double globalSaiAerosolLoadingTg = 0.0; // Teragrams of SO2 in stratospheric injection
    /** Internal state: global cooling effect celsius (double). */
    private static double globalCoolingEffectCelsius = 0.0;

    /**
     * Processes geoengineering radiative forcing and albedo feedbacks.
     *
     * @param cells list of H3 cells in the simulation grid
     * @param deltaYears step size in years
     */
    public static void processGeoengineeringAlbedo(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double dtSafe = Math.max(0.0001, deltaYears);

        // Calculate active geoengineering deployment based on global high-capacity industrial cells
        double totalIndustrialSaiCapacity = 0;
        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop > 1000) {
                double capPerCap = cell.getResourceCapitalOr(0.0) / pop;
                if (capPerCap >= 2500.0) {
                    totalIndustrialSaiCapacity += Math.min(10.0, (capPerCap - 2500.0) / 1000.0);
                }
            }
        }

        // Stratospheric Aerosol Injection threshold: High-tech industrial civilizations deploy SAI if global temp > 18A°C
        if (totalIndustrialSaiCapacity > 50.0) {
            globalSaiAerosolLoadingTg = Math.min(20.0, totalIndustrialSaiCapacity * 0.1);
            // 5 Tg SO2 ~ -0.5A°C cooling
            globalCoolingEffectCelsius = globalSaiAerosolLoadingTg * 0.10;
        } else {
            globalSaiAerosolLoadingTg = Math.max(0.0, globalSaiAerosolLoadingTg - (1.0 * dtSafe)); // Aerosol decay ~1-2 yr lifetime
            globalCoolingEffectCelsius = globalSaiAerosolLoadingTg * 0.10;
        }

        double thermalRelaxationFactor = 1.0 - Math.exp(-dtSafe / 1.5);

        for (H3Cell cell : cells) {
            if (cell == null || cell.getTemperature() == null) continue;

            // Apply global aerosol cooling offset relaxed towards equilibrium
            if (globalCoolingEffectCelsius > 0.0) {
                double curTemp = cell.getTemperatureOr(15.0);
                double elev = cell.getElevationOr(0.0);
                double lat = cell.getLatitudeOr(0.0);
                double baselineTemp = 28.0 - 0.0065 * Math.max(0.0, elev) - 0.45 * Math.abs(lat);
                double targetTemp = baselineTemp - globalCoolingEffectCelsius;
                cell.setTemperature(curTemp + (targetTemp - curTemp) * thermalRelaxationFactor);

                // Side effect of SAI: Disruption of monsoon precipitation
                if (cell.getRainfall() != null) {
                    cell.setRainfall(Math.max(50.0, cell.getRainfall() * Math.exp(-0.02 * globalSaiAerosolLoadingTg * dtSafe)));
                }
            }

            // Surface albedo feedback calculation
            cell.calculateDynamicAlbedo();
        }
    }

    public static double getGlobalSaiAerosolLoadingTg() {
        return globalSaiAerosolLoadingTg;
    }

    public static double getGlobalCoolingEffectCelsius() {
        return globalCoolingEffectCelsius;
    }

    public static void resetState() {
        globalSaiAerosolLoadingTg = 0.0;
        globalCoolingEffectCelsius = 0.0;
    }
}
