/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.PlanetPreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Procedural Terraforming & Orbital Resource Physics Engine.
 * Models planetary atmospheric pressure adaptation (P_atm), greenhouse gas injection,
 * orbital solar mirror insolation multipliers, Sabatier O2/CH4 conversion,
 * and off-world asteroid mining EROEI logistics.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class TerraformingEngine {
    private static final Logger logger = LoggerFactory.getLogger(TerraformingEngine.class);

    // Global Terraforming State Variables
    private static double atmosphericPressureAtm = 1.0;
    private static double co2Ppm = 420.0;
    private static double solarMirrorInsolationMultiplier = 1.0; // Multiplier from orbital mirrors
    private static double asteroidMiningFluxTonnesPerYear = 0.0;
    private static double offWorldEroeiRatio = 15.0; // Asteroid mining EROEI

    public static void processTerraforming(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        // Apply orbital mirror solar forcing & greenhouse atmospheric warming
        double radiativeForcingWm2 = 5.35 * Math.log(co2Ppm / 280.0) + (solarMirrorInsolationMultiplier - 1.0) * 1361.0;
        double globalTempDelta = radiativeForcingWm2 * 0.8 * deltaYears;

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            // Update cell temperature based on global terraforming radiative forcing
            double currentTemp = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
            cell.setTemperature(currentTemp + globalTempDelta);

            // Asteroid mining influx boosts local metal/mineral density
            if (asteroidMiningFluxTonnesPerYear > 0 && cell.getPopulation() > 1000) {
                double mineralBoost = (asteroidMiningFluxTonnesPerYear / cells.size()) * 0.0001;
                cell.setResourceMetal((cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0) + mineralBoost);
            }
        }
    }

    // Getters and Setters
    public static double getAtmosphericPressureAtm() { return atmosphericPressureAtm; }
    public static void setAtmosphericPressureAtm(double pressure) { atmosphericPressureAtm = Math.max(0.01, pressure); }

    public static double getCo2Ppm() { return co2Ppm; }
    public static void setCo2Ppm(double ppm) { co2Ppm = Math.max(10.0, ppm); }

    public static double getSolarMirrorInsolationMultiplier() { return solarMirrorInsolationMultiplier; }
    public static void setSolarMirrorInsolationMultiplier(double mult) { solarMirrorInsolationMultiplier = Math.max(0.5, Math.min(5.0, mult)); }

    public static double getAsteroidMiningFluxTonnesPerYear() { return asteroidMiningFluxTonnesPerYear; }
    public static void setAsteroidMiningFluxTonnesPerYear(double flux) { asteroidMiningFluxTonnesPerYear = Math.max(0.0, flux); }

    public static double getOffWorldEroeiRatio() { return offWorldEroeiRatio; }
    public static void setOffWorldEroeiRatio(double eroei) { offWorldEroeiRatio = Math.max(0.1, eroei); }
}
