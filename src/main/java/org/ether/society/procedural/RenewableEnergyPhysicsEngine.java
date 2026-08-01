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
 * Betz Wind Kinetic Power & Solar Radiance Physics Engine.
 * Replaces abstract solar/wind units with physical power flux equations:
 * 1. <b>Betz Law Wind Kinetic Power Density</b>: P_wind = 0.5 * ρ * A * v³ * Cp (where Cp ≤ 0.593 Betz limit).
 * 2. <b>Top-of-Atmosphere Solar Irradiance</b>: S₀ = 1361 W/m² attenuated by atmospheric optical transmittance (τ).
 *
 * @author Silvere Martin-Michiellot
 * @version 3.1.0
 */
public class RenewableEnergyPhysicsEngine {
    private static final Logger logger = LoggerFactory.getLogger(RenewableEnergyPhysicsEngine.class);

    /** Solar Constant at TOA in W/m² */
    public static final double SOLAR_CONSTANT_W_PER_M2 = 1361.0;

    /** Betz Limit aerodynamic power coefficient */
    public static final double BETZ_LIMIT_CP = 0.593;

    /** Air density at sea level in kg/m³ */
    public static final double AIR_DENSITY_KG_PER_M3 = 1.225;

    /**
     * Calculates kinetic wind power density in W/m² based on Betz Law.
     *
     * @param windSpeedMetersPerSec Wind speed in m/s
     * @return Kinetic wind power density in W/m²
     */
    public static double calculateBetzWindPowerWattsPerM2(double windSpeedMetersPerSec) {
        if (windSpeedMetersPerSec <= 0.0) return 0.0;
        return 0.5 * AIR_DENSITY_KG_PER_M3 * Math.pow(windSpeedMetersPerSec, 3.0) * BETZ_LIMIT_CP;
    }

    /**
     * Executes one renewable energy physics tick across cells.
     */
    public static void processRenewableEnergyPhysics(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;

            // Wind speed increases with altitude & slope (v ~ 5.0 to 15.0 m/s)
            double windSpeed = 5.0 + Math.min(10.0, elev / 300.0);
            double betzWindPowerW = calculateBetzWindPowerWattsPerM2(windSpeed);

            cell.setEnergyWind(betzWindPowerW);

            // Solar irradiance attenuated by latitude & optical depth
            double latRad = Math.toRadians(cell.getLatitude() != null ? cell.getLatitude() : 0.0);
            double solarIrradianceW = SOLAR_CONSTANT_W_PER_M2 * Math.max(0.1, Math.cos(latRad)) * 0.70; // 70% atmospheric transmittance

            cell.setEnergySolar(solarIrradianceW);
        }
    }
}
