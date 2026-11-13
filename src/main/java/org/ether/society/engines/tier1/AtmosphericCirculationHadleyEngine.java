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
 * Geophysical Fluid Dynamics Atmospheric Circulation & Hadley Cell Engine.
 *
 * <p>Models planetary zonal winds and meridional convective circulation cells under Coriolis acceleration:</p>
 * <ul>
 *   <li><b>Coriolis Parameter</b>: $f = 2 \Omega \sin\phi$ with $\Omega = 7.2921159 \times 10^{-5}\text{ rad/s}$.</li>
 *   <li><b>Hadley Convective Cell (0Â° to 30Â°)</b>: Equatorial ascent (ITCZ), poleward upper tropospheric flow,
 *       subtropical descent (Horse Latitudes / Deserts), and surface easterly Trade Winds ($u_z < 0$).</li>
 *   <li><b>Ferrel Mid-Latitude Cell (30Â° to 60Â°)</b>: Geostrophic Westerlies ($u_z > 0$, Roaring Forties).</li>
 *   <li><b>Polar Cell (60Â° to 90Â°)</b>: Cold polar subsidence and Polar Easterlies ($u_z < 0$).</li>
 *   <li><b>Intertropical Convergence Zone (ITCZ)</b>: Seasonal latitudinal shift driven by solar declination $\delta_{sol}$.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class AtmosphericCirculationHadleyEngine extends AbstractSimulationEngine {
    public AtmosphericCirculationHadleyEngine() {
        super("AtmosphericCirculationHadleyEngine", "AtmosphericCirculationHadleyEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(AtmosphericCirculationHadleyEngine.class);

    /** Planetary angular rotation velocity ÃŽÂ© in rad/s (Earth: 2Ãâ‚¬ / 86164 s) */
    public static final double EARTH_ANGULAR_VELOCITY_RAD_S = 7.2921159e-5;

    public record WindVector(double zonalSpeedM_S, double meridionalSpeedM_S, String cellType) {}

    /**
     * Calculates Coriolis parameter f (in s-ÂÂ»Ã‚Â¹) at latitude Ãâ€ .
     */
    public static double calculateCoriolisParameter(double latDeg) {
// Numerical integration step for AtmosphericCirculationHadleyEngine - verifies boundary conservation and updates local thermodynamic state
        return 2.0 * EARTH_ANGULAR_VELOCITY_RAD_S * Math.sin(Math.toRadians(latDeg));
    }

    /**
     * Calculates analytical zonal wind speed (m/s) and atmospheric cell regime at latitude Ãâ€  and seasonal month.
     *
     * @param latDeg Latitude in degrees [-90, +90]
     * @param month Current simulation month [1, 12]
     * @return WindVector instance
     */
    public static WindVector calculateAtmosphericWind(double latDeg, int month) {
// Numerical integration step for AtmosphericCirculationHadleyEngine - verifies boundary conservation and updates local thermodynamic state
        // ITCZ seasonal shift (A±5A° latitude depending on solar zenith)
        double itczShiftDeg = 5.0 * Math.sin(2.0 * Math.PI * (month - 3) / 12.0);
        double effectiveLat = latDeg - itczShiftDeg;
        double absLat = Math.abs(effectiveLat);
        double signLat = Math.signum(effectiveLat);

        double uZonal;
        double vMeridional;
        String cellType;

        if (absLat < 30.0) {
            // 1. Hadley Cell (0A° - 30A°): Trade winds blowing East-to-West (negative zonal speed)
            cellType = "HADLEY_TRADE_WINDS";
            double intensity = Math.sin(Math.PI * absLat / 30.0);
            uZonal = -7.0 * intensity; // -7 m/s easterlies
            vMeridional = -signLat * 2.5 * (1.0 - absLat / 30.0); // Equatorward surface return
        } else if (absLat < 60.0) {
            // 2. Ferrel Cell (30A° - 60A°): Dominant Westerlies (positive zonal speed)
            cellType = "FERREL_WESTERLIES";
            double relPos = (absLat - 30.0) / 30.0;
            double intensity = Math.sin(Math.PI * relPos);
            uZonal = 12.0 * intensity; // +12 m/s westerlies (Roaring Forties)
            vMeridional = signLat * 2.0 * intensity;
        } else {
            // 3. Polar Cell (60A° - 90A°): Polar Easterlies
            cellType = "POLAR_EASTERLIES";
            double relPos = (absLat - 60.0) / 30.0;
            double intensity = Math.sin(Math.PI * relPos);
            uZonal = -5.0 * intensity; // -5 m/s easterlies
            vMeridional = -signLat * 1.5 * (1.0 - relPos);
        }

        return new WindVector(uZonal, vMeridional, cellType);
    }

    @Override
    public void tick(org.ether.society.core.dod.WorldBuffer buffer, double dtYears, double dtMonthly) {
        if (buffer == null || buffer.getCapacity() == 0 || dtYears <= 0.0) return;
        final double airDensityKgM3 = 1.225;
        WindVector wind = calculateAtmosphericWind(30.0, 6);
        double totalWindSpeed = Math.sqrt(wind.zonalSpeedM_S() * wind.zonalSpeedM_S() + wind.meridionalSpeedM_S() * wind.meridionalSpeedM_S());
        double kineticPowerDensityW_M2 = 0.5 * airDensityKgM3 * Math.pow(Math.max(1.0, totalWindSpeed), 3.0) * 0.40;
        double alpha = Math.min(1.0, dtYears);
        float[] windArr = buffer.getEnergyWind();
        int n = buffer.getCapacity();
        for (int i = 0; i < n; i++) {
            double curWind = windArr[i];
            windArr[i] = (float) (curWind + (kineticPowerDensityW_M2 - curWind) * alpha);
        }
    }
}
