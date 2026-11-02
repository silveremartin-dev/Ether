/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Global Ocean Salinity (PSU) & Thermohaline Circulation (AMOC) Engine.
 * Models:
 * 1. <b>Practical Salinity Units (PSU)</b>: Baseline oceanic salinity (35.0 PSU).
 * 2. <b>Polar Ice Melt Dilution</b>: Polar ice cap melting dilutes ocean water density.
 * 3. <b>Thermohaline AMOC Collapse</b>: When polar ocean salinity drops below 32.0 PSU, deep-water convection halts,
 *    causing an 8.0°C cooling shock across mid-to-high latitude cells.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ThermohalineOceanEngine {
    private static final Logger logger = LoggerFactory.getLogger(ThermohalineOceanEngine.class);

    /** Standard baseline ocean salinity in Practical Salinity Units (PSU) */
    public static final double STANDARD_OCEAN_SALINITY_PSU = 35.0;

    /**
     * Executes one thermohaline circulation and ocean salinity tick across cells with default monthly step.
     */
    public static void processThermohalineCirculation(List<H3Cell> cells, double globalTemperatureAnomaly) {
        processThermohalineCirculation(cells, globalTemperatureAnomaly, 1.0 / 12.0);
    }

    /**
     * Executes one thermohaline circulation and ocean salinity tick across cells scaled by dtYears.
     */
    public static void processThermohalineCirculation(List<H3Cell> cells, double globalTemperatureAnomaly, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        // Polar ice meltwater dilution (high temperature anomaly -> lower PSU)
        double oceanSalinityPSU = Math.max(25.0, STANDARD_OCEAN_SALINITY_PSU - (globalTemperatureAnomaly * 1.2));

        boolean amocCollapsed = oceanSalinityPSU < 32.0;

        if (amocCollapsed) {
            logger.warn("Thermohaline Engine: AMOC circulation collapse triggered (Ocean Salinity = {} PSU). Mid-latitude thermal drop active.", oceanSalinityPSU);
        }

        double dtSafe = Math.max(0.0001, deltaYears);
        double thermalRelaxationFactor = 1.0 - Math.exp(-dtSafe / 2.0);

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            if (cell.getBiome() == Biome.OCEAN || cell.getBiome() == Biome.DEEP_OCEAN) {
                // Ocean thermal buffer
                continue;
            }

            // AMOC collapse cooling shock on high latitude cells (> 45° latitude) relaxed towards equilibrium
            if (amocCollapsed && cell.getLatitude() != null && Math.abs(cell.getLatitude()) > 45.0) {
                double currentTemp = cell.getTemperatureOr(10.0);
                double elev = cell.getElevationOr(0.0);
                double lat = cell.getLatitude();
                double baselineTemp = 28.0 - 0.0065 * Math.max(0.0, elev) - 0.45 * Math.abs(lat);
                double targetTemp = baselineTemp - 8.0; // 8°C thermal drop at equilibrium
                cell.setTemperature(currentTemp + (targetTemp - currentTemp) * thermalRelaxationFactor);
            }
        }
    }
}
