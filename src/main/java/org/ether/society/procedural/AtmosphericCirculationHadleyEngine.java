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
 * Geophysical Fluid Dynamics Atmospheric Circulation & Hadley Cell Engine.
 *
 * <p>Models planetary zonal winds and meridional convective circulation cells under Coriolis acceleration:</p>
 * <ul>
 *   <li><b>Coriolis Parameter</b>: $f = 2 \Omega \sin\phi$ with $\Omega = 7.2921159 \times 10^{-5}\text{ rad/s}$.</li>
 *   <li><b>Hadley Convective Cell (0° to 30°)</b>: Equatorial ascent (ITCZ), poleward upper tropospheric flow,
 *       subtropical descent (Horse Latitudes / Deserts), and surface easterly Trade Winds ($u_z < 0$).</li>
 *   <li><b>Ferrel Mid-Latitude Cell (30° to 60°)</b>: Geostrophic Westerlies ($u_z > 0$, Roaring Forties).</li>
 *   <li><b>Polar Cell (60° to 90°)</b>: Cold polar subsidence and Polar Easterlies ($u_z < 0$).</li>
 *   <li><b>Intertropical Convergence Zone (ITCZ)</b>: Seasonal latitudinal shift driven by solar declination $\delta_{sol}$.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 4.4.0
 */
public class AtmosphericCirculationHadleyEngine {
    private static final Logger logger = LoggerFactory.getLogger(AtmosphericCirculationHadleyEngine.class);

    /** Planetary angular rotation velocity Ω in rad/s (Earth: 2π / 86164 s) */
    public static final double EARTH_ANGULAR_VELOCITY_RAD_S = 7.2921159e-5;

    public record WindVector(double zonalSpeedM_S, double meridionalSpeedM_S, String cellType) {}

    /**
     * Calculates Coriolis parameter f (in s⁻¹) at latitude φ.
     */
    public static double calculateCoriolisParameter(double latDeg) {
        return 2.0 * EARTH_ANGULAR_VELOCITY_RAD_S * Math.sin(Math.toRadians(latDeg));
    }

    /**
     * Calculates analytical zonal wind speed (m/s) and atmospheric cell regime at latitude φ and seasonal month.
     *
     * @param latDeg Latitude in degrees [-90, +90]
     * @param month Current simulation month [1, 12]
     * @return WindVector instance
     */
    public static WindVector calculateAtmosphericWind(double latDeg, int month) {
        // ITCZ seasonal shift (±5° latitude depending on solar zenith)
        double itczShiftDeg = 5.0 * Math.sin(2.0 * Math.PI * (month - 3) / 12.0);
        double effectiveLat = latDeg - itczShiftDeg;
        double absLat = Math.abs(effectiveLat);
        double signLat = Math.signum(effectiveLat);

        double uZonal;
        double vMeridional;
        String cellType;

        if (absLat < 30.0) {
            // 1. Hadley Cell (0° - 30°): Trade winds blowing East-to-West (negative zonal speed)
            cellType = "HADLEY_TRADE_WINDS";
            double intensity = Math.sin(Math.PI * absLat / 30.0);
            uZonal = -7.0 * intensity; // -7 m/s easterlies
            vMeridional = -signLat * 2.5 * (1.0 - absLat / 30.0); // Equatorward surface return
        } else if (absLat < 60.0) {
            // 2. Ferrel Cell (30° - 60°): Dominant Westerlies (positive zonal speed)
            cellType = "FERREL_WESTERLIES";
            double relPos = (absLat - 30.0) / 30.0;
            double intensity = Math.sin(Math.PI * relPos);
            uZonal = 12.0 * intensity; // +12 m/s westerlies (Roaring Forties)
            vMeridional = signLat * 2.0 * intensity;
        } else {
            // 3. Polar Cell (60° - 90°): Polar Easterlies
            cellType = "POLAR_EASTERLIES";
            double relPos = (absLat - 60.0) / 30.0;
            double intensity = Math.sin(Math.PI * relPos);
            uZonal = -5.0 * intensity; // -5 m/s easterlies
            vMeridional = -signLat * 1.5 * (1.0 - relPos);
        }

        return new WindVector(uZonal, vMeridional, cellType);
    }

    /**
     * Processes atmospheric wind circulation and updates kinetic wind energy across H3 cells.
     */
    public static void processAtmosphericCirculation(List<H3Cell> cells, int month) {
        if (cells == null || cells.isEmpty()) return;

        final double airDensityKgM3 = 1.225;

        for (H3Cell cell : cells) {
            WindVector wind = calculateAtmosphericWind(cell.getLatitude(), month);
            double totalWindSpeed = Math.sqrt(wind.zonalSpeedM_S() * wind.zonalSpeedM_S()
                    + wind.meridionalSpeedM_S() * wind.meridionalSpeedM_S());

            // Betz kinetic wind power density: P = 0.5 * ρ_air * v³ * C_p (Betz limit C_p ≈ 0.40 real-world)
            double kineticPowerDensityW_M2 = 0.5 * airDensityKgM3 * Math.pow(Math.max(1.0, totalWindSpeed), 3.0) * 0.40;
            cell.setEnergyWind(kineticPowerDensityW_M2);
        }
    }
}
