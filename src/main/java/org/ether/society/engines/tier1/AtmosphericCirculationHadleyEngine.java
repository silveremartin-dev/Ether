/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.h3.H3Service;
import org.ether.society.model.PhysicalUnitsHelper;
import org.ether.society.model.SpatialSurfaceHelper;

/**
 * Geophysical Fluid Dynamics Atmospheric Circulation &amp; Hadley Cell Engine.
 *
 * <p>Models planetary zonal winds and meridional convective circulation cells under Coriolis acceleration:</p>
 * <ul>
 *   <li><b>Coriolis Parameter</b>: \(f = 2 \Omega \sin\phi\) with \(\Omega = 7.2921159 \times 10^{-5}\,\text{rad/s}\).</li>
 *   <li><b>Hadley Convective Cell (\(0^\circ\) to \(30^\circ\))</b>: Equatorial ascent (ITCZ), poleward upper tropospheric flow,
 *       subtropical descent (Horse Latitudes / Deserts), and surface easterly Trade Winds (\(u_z &lt; 0\)).</li>
 *   <li><b>Ferrel Mid-Latitude Cell (\(30^\circ\) to \(60^\circ\))</b>: Geostrophic Westerlies (\(u_z &gt; 0\), Roaring Forties).</li>
 *   <li><b>Polar Cell (\(60^\circ\) to \(90^\circ\))</b>: Cold polar subsidence and Polar Easterlies (\(u_z &lt; 0\)).</li>
 *   <li><b>Intertropical Convergence Zone (ITCZ)</b>: Seasonal latitudinal shift driven by solar declination \(\delta_{\text{sol}}\).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class AtmosphericCirculationHadleyEngine extends AbstractSimulationEngine {

    /** Planetary angular rotation velocity \(\Omega\) in \(\text{rad/s}\) (Earth: \(2\pi / 86164\,\text{s}\)). */
    public static final double EARTH_ANGULAR_VELOCITY_RAD_S = 7.2921159e-5;

    /**
     * Surface wind velocity vector and atmospheric circulation cell classification.
     *
     * @param zonalSpeedM_S      eastward zonal velocity \(u\) in \(\text{m/s}\)
     * @param meridionalSpeedM_S northward meridional velocity \(v\) in \(\text{m/s}\)
     * @param cellType           circulation cell regime identifier
     */
    public record WindVector(double zonalSpeedM_S, double meridionalSpeedM_S, String cellType) {}

    public AtmosphericCirculationHadleyEngine() {
        super("AtmosphericCirculationHadleyEngine", "AtmosphericCirculationHadleyEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    /**
     * Calculates Coriolis parameter \(f\) (in \(\text{s}^{-1}\)) at latitude \(\phi\).
     *
     * @param latDeg latitude in degrees \([-90, +90]\)
     * @return Coriolis parameter in \(\text{s}^{-1}\)
     */
    public static double calculateCoriolisParameter(double latDeg) {
        return 2.0 * EARTH_ANGULAR_VELOCITY_RAD_S * Math.sin(Math.toRadians(latDeg));
    }

    /**
     * Calculates analytical zonal and meridional surface wind speeds (\(\text{m/s}\)) and atmospheric
     * cell regime at latitude \(\phi\) and seasonal month.
     *
     * @param latDeg latitude in degrees \([-90, +90]\)
     * @param month  current simulation month \([1, 12]\)
     * @return {@link WindVector} instance
     */
    public static WindVector calculateAtmosphericWind(double latDeg, int month) {
        double itczShiftDeg = 5.0 * Math.sin(2.0 * Math.PI * (month - 3) / 12.0);
        double effectiveLat = latDeg - itczShiftDeg;
        double absLat = Math.abs(effectiveLat);
        double signLat = Math.signum(effectiveLat);

        double uZonal;
        double vMeridional;
        String cellType;

        if (absLat < 30.0) {
            cellType = "HADLEY_TRADE_WINDS";
            double intensity = Math.sin(Math.PI * absLat / 30.0);
            uZonal = -7.0 * intensity;
            vMeridional = -signLat * 2.5 * (1.0 - absLat / 30.0);
        } else if (absLat < 60.0) {
            cellType = "FERREL_WESTERLIES";
            double relPos = (absLat - 30.0) / 30.0;
            double intensity = Math.sin(Math.PI * relPos);
            uZonal = 12.0 * intensity;
            vMeridional = signLat * 2.0 * intensity;
        } else {
            cellType = "POLAR_EASTERLIES";
            double relPos = (absLat - 60.0) / 30.0;
            double intensity = Math.sin(Math.PI * relPos);
            uZonal = -5.0 * intensity;
            vMeridional = -signLat * 1.5 * (1.0 - relPos);
        }

        return new WindVector(uZonal, vMeridional, cellType);
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        if (buffer == null || buffer.getCapacity() == 0 || dtYears <= 0.0) {
            return;
        }
        final double airDensityKgM3 = 1.225;
        double fractionalYear = buffer.getCurrentYear() - Math.floor(buffer.getCurrentYear());
        int month = Math.clamp((int) Math.floor(fractionalYear * 12.0) + 1, 1, 12);
        double alpha = Math.min(1.0, dtYears);

        float[] popArr = buffer.getHumanPopulation();
        float[] capArr = buffer.getResourceCapital();
        float[] windArr = buffer.getEnergyWind();
        long[] h3Arr = buffer.getH3Indexes();
        int n = buffer.getCapacity();
        H3Service h3Service = H3Service.getInstance();

        for (int i = 0; i < n; i++) {
            double pop = popArr != null ? popArr[i] : 0.0;
            if (pop <= 0.0) {
                windArr[i] = 0.0f;
                continue;
            }
            double kpc = capArr != null ? Math.max(0.0, capArr[i] / pop) : 0.0;
            if (kpc <= 0.0) {
                windArr[i] = 0.0f;
                continue;
            }

            double latDeg = 30.0;
            if (h3Arr != null && h3Arr[i] != 0L) {
                try {
                    latDeg = h3Service.cellToLatLng(h3Arr[i]).lat;
                } catch (Exception ignored) {
                    latDeg = 30.0;
                }
            }
            WindVector wind = calculateAtmosphericWind(latDeg, month);
            double totalWindSpeed = Math.hypot(wind.zonalSpeedM_S(), wind.meridionalSpeedM_S());
            double kineticPowerDensityWM2 = 0.5 * airDensityKgM3 * Math.pow(Math.max(1.0, totalWindSpeed), 3.0) * 0.40;

            double cellAreaM2 = SpatialSurfaceHelper.resolveCellAreaKm2(buffer, i)
                    * PhysicalUnitsHelper.SQ_METERS_PER_SQ_KM;
            double maxKleidonCellWindWatts = RenewableEnergyPhysicsEngine.KLEIDON_MAX_WIND_EXTRACTION_W_PER_M2 * cellAreaM2;
            double targetCapturedWindGJYr = PhysicalUnitsHelper.wattsToGjPerYear(
                    Math.min(maxKleidonCellWindWatts, pop * RenewableEnergyPhysicsEngine.computeCapturedWindWattsPerCapita(kineticPowerDensityWM2, kpc)));
            double curWindGJYr = windArr[i];
            windArr[i] = (float) (curWindGJYr + (targetCapturedWindGJYr - curWindGJYr) * alpha);
        }
    }
}
