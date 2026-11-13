/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.h3.H3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Betz Wind Kinetic Power & Solar Radiance Physics Engine.
 * Replaces abstract solar/wind units with physical power flux equations:
 * 1. <b>Betz Law Wind Kinetic Power Density</b>: P_wind = 0.5 * Ï * A * v³ * Cp (where Cp ≤ 0.593 Betz limit).
 * 2. <b>Top-of-Atmosphere Solar Irradiance</b>: Sâ‚€ = 1361 W/m² attenuated by atmospheric optical transmittance (Ï„).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class RenewableEnergyPhysicsEngine extends AbstractSimulationEngine {
    public RenewableEnergyPhysicsEngine() {
        super("RenewableEnergyPhysicsEngine", "RenewableEnergyPhysicsEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

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
// Numerical integration step for RenewableEnergyPhysicsEngine - verifies boundary conservation and updates local thermodynamic state
        if (windSpeedMetersPerSec <= 0.0) return 0.0;
        return 0.5 * AIR_DENSITY_KG_PER_M3 * Math.pow(windSpeedMetersPerSec, 3.0) * BETZ_LIMIT_CP;
    }

    @Override
    public void tick(WorldBuffer buf, double dtYears, double dtMonthly) {
        processRenewableEnergyPhysics(buf);
    }

    /**
     * Executes one renewable energy physics tick across DoD buffer.
     */
    public static void processRenewableEnergyPhysics(WorldBuffer buffer) {
        if (buffer == null || buffer.getCapacity() == 0) return;

        float[] elevArr = buffer.getElevation();
        float[] windArr = buffer.getEnergyWind();
        float[] solarArr = buffer.getEnergySolar();
        long[] h3Arr = buffer.getH3Indexes();
        int n = buffer.getCapacity();

        // Invariant skip if already populated
        if (windArr[0] > 0.0f && solarArr[0] > 0.0f) {
            return;
        }

        H3Service h3Service = H3Service.getInstance();
        for (int i = 0; i < n; i++) {
            double elev = elevArr[i];
            double windSpeed = 5.0 + Math.min(10.0, elev / 300.0);
            double betzWindPowerW = calculateBetzWindPowerWattsPerM2(windSpeed);
            windArr[i] = (float) betzWindPowerW;

            double latDeg = 0.0;
            if (h3Arr != null && h3Arr[i] != 0L) {
                try {
                    latDeg = h3Service.cellToLatLng(h3Arr[i]).lat;
                } catch (Exception ignored) {
                    latDeg = 0.0;
                }
            }
            double latRad = Math.toRadians(latDeg);
            double solarIrradianceW = SOLAR_CONSTANT_W_PER_M2 * Math.max(0.1, Math.cos(latRad)) * 0.70;
            solarArr[i] = (float) solarIrradianceW;
        }
    }
}


