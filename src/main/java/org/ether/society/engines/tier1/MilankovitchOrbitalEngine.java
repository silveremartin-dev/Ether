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
 * Astronomical Milankovitch Orbital Forcing & Insolation Engine.
 *
 * <p>Models celestial orbital mechanics and solar insolation variations across multi-millennial timescales:</p>
 * <ul>
 *   <li><b>Orbital Eccentricity ($e$, ~100 ka cycle)</b>: Modulates the total annual solar radiation received by Earth ($e \in [0.005, 0.058]$).</li>
 *   <li><b>Obliquity / Axial Tilt ($\varepsilon$, ~41 ka cycle)</b>: Varies between $22.1^\circ$ and $24.5^\circ$, modulating the equator-to-pole thermal gradient and seasonality.</li>
 *   <li><b>Climatic Precession ($\varpi$, ~23 ka cycle)</b>: Modulates the timing of perihelion relative to summer/winter solstice.</li>
 *   <li><b>Daily Top-of-Atmosphere (TOA) Insolation</b>:
 *     $$S(\phi, \delta) = \frac{S_0}{\pi} \left(\frac{1 + e\cos\nu}{1 - e^2}\right)^2 \cdot \left[ H_0 \sin\phi \sin\delta + \cos\phi \cos\delta \sin H_0 \right]$$
 *   </li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class MilankovitchOrbitalEngine extends AbstractSimulationEngine {
    public MilankovitchOrbitalEngine() {
        super("MilankovitchOrbitalEngine", "MilankovitchOrbitalEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(MilankovitchOrbitalEngine.class);

    /** Solar Constant TOA in W/mÂ² */
    public static final double SOLAR_CONSTANT_TOA_W_M2 = 1361.0;

    /** Mean orbital eccentricity baseline */
    public static final double MEAN_ECCENTRICITY = 0.0167;
    /** Internal state: eccentricity amplitude (double). */
    public static final double ECCENTRICITY_AMPLITUDE = 0.020;
    /** Internal state: eccentricity period years (double). */
    public static final double ECCENTRICITY_PERIOD_YEARS = 100_000.0;

    /** Mean obliquity (axial tilt) in degrees */
    public static final double MEAN_OBLIQUITY_DEG = 23.44;
    /** Internal state: obliquity amplitude deg (double). */
    public static final double OBLIQUITY_AMPLITUDE_DEG = 1.20;
    /** Internal state: obliquity period years (double). */
    public static final double OBLIQUITY_PERIOD_YEARS = 41_000.0;

    /** Precession period in years */
    public static final double PRECESSION_PERIOD_YEARS = 23_000.0;

    public record MilankovitchParameters(double eccentricity, double obliquityDeg, double precessionAngleRad) {}

    /**
     * Computes Milankovitch orbital parameters for a given calendar year (negative for BCE/BP)
     * anchored to J2000.0 astronomical ephemerides (Berger 1978, <i>J. Atmos. Sci.</i> 35:2362; Laskar et al. 2004):
     * <ul>
     *   <li>Eccentricity $e(2000) \approx 0.0167$, slowly decreasing from Late Pleistocene values.</li>
     *   <li>Obliquity $\varepsilon(2000) \approx 23.439^\circ$, decreasing from Early Holocene maximum ($\sim -8\,700\,\text{BP}$, $24.23^\circ$).</li>
     *   <li>Geocentric solar ecliptic longitude of perihelion $\varpi(2000) = 282.93^\circ$ (January 3 perihelion; July 4 aphelion).</li>
     * </ul>
     *
     * @param year Current simulation year (negative for BCE/years before present)
     * @return MilankovitchParameters instance
     */
    public static MilankovitchParameters computeOrbitalParameters(double year) {
        double t = year;
        // Eccentricity e(t) anchored to J2000 (e_2000 = 0.0167, Laskar et al. 2004)
        double e = 0.0267 - 0.0100 * Math.cos(2.0 * Math.PI * (t - 2000.0) / ECCENTRICITY_PERIOD_YEARS);
        e = Math.clamp(e, 0.005, 0.060);

        // Obliquity eps(t) in degrees anchored to Early Holocene maximum (-8700 BP: ~24.23 deg -> J2000: ~23.44 deg)
        double eps = 23.32 + 0.91 * Math.cos(2.0 * Math.PI * (t - (-8700.0)) / OBLIQUITY_PERIOD_YEARS);

        // Geocentric solar ecliptic longitude of perihelion anchored to J2000 (\varpi_0 = 282.93 deg = 102.93 + 180 deg)
        double varpi = Math.toRadians(282.93) + (2.0 * Math.PI * (t - 2000.0) / PRECESSION_PERIOD_YEARS);
        varpi = varpi % (2.0 * Math.PI);
        if (varpi < 0.0) varpi += 2.0 * Math.PI;

        return new MilankovitchParameters(e, eps, varpi);
    }

    /**
     * Computes daily average TOA insolation (W/mÂ²) for a given latitude and day of year.
     *
     * @param latDeg Latitude in degrees [-90, +90]
     * @param dayOfYear Day of year [1, 365]
     * @param params MilankovitchParameters
     * @return Daily average insolation in W/mÂ²
     */
    public static double calculateDailyInsolation(double latDeg, int dayOfYear, MilankovitchParameters params) {
        double phi = Math.toRadians(latDeg);
        double eps = Math.toRadians(params.obliquityDeg());

        // Solar true anomaly / longitude I»_s
        double lambdaS = 2.0 * Math.PI * ((double) (dayOfYear - 80) / 365.25);
        double sinDelta = Math.sin(eps) * Math.sin(lambdaS);
        double delta = Math.asin(sinDelta); // Solar declination

        // Hour angle of sunrise/sunset H_0
        double tanPhiTanDelta = Math.tan(phi) * Math.tan(delta);
        double cosH0 = Math.clamp(-tanPhiTanDelta, -1.0, 1.0);
        double h0 = Math.acos(cosH0);

        // Distance factor rho = r_mean / r
        double distanceFactor = (1.0 + params.eccentricity() * Math.cos(lambdaS - params.precessionAngleRad()))
                / (1.0 - params.eccentricity() * params.eccentricity());
        double distSq = distanceFactor * distanceFactor;

        // Daily average TOA insolation
        double term = h0 * Math.sin(phi) * Math.sin(delta) + Math.cos(phi) * Math.cos(delta) * Math.sin(h0);
        return (SOLAR_CONSTANT_TOA_W_M2 / Math.PI) * distSq * Math.max(0.0, term);
    }

    @Override
    public void tick(WorldBuffer buf, double dtYears, double dtMonthly) {
        double year = buf != null ? buf.getCurrentYear() : 2000.0;
        applyMilankovitchForcing(buf, year, dtYears);
    }

    /**
     * Applies Milankovitch insolation forcing anomaly to surface temperatures across all planetary cells with default monthly step.
     *
     * @param buffer WorldBuffer DoD state
     * @param currentYear Simulation year
     */
    public static void applyMilankovitchForcing(WorldBuffer buffer, double currentYear) {
        applyMilankovitchForcing(buffer, currentYear, 1.0 / 12.0);
    }

    /**
     * Applies Milankovitch insolation forcing anomaly to surface temperatures across all planetary cells scaled by dtYears.
     *
     * @param buffer WorldBuffer DoD state
     * @param currentYear Simulation year
     * @param deltaYears Simulation time step in years
     */
    public static void applyMilankovitchForcing(WorldBuffer buffer, double currentYear, double deltaYears) {
        if (buffer == null || buffer.getCapacity() == 0) return;

        MilankovitchParameters params = computeOrbitalParameters(currentYear);

        // Compute high-latitude summer insolation anomaly (65 N at summer solstice day 172)
        double refInsolation65N = calculateDailyInsolation(65.0, 172, new MilankovitchParameters(MEAN_ECCENTRICITY, MEAN_OBLIQUITY_DEG, 0.0));
        double curInsolation65N = calculateDailyInsolation(65.0, 172, params);
        double anomalyW_M2 = curInsolation65N - refInsolation65N;

        // Radiative forcing scaling: ~0.08 C per W/m^2 of 65 N summer insolation anomaly
        double globalTempShiftC = anomalyW_M2 * 0.08;
        double dtSafe = Math.max(0.0001, deltaYears);
        double thermalRelaxationFactor = 1.0 - Math.exp(-dtSafe / 2.0);

        float[] tempArr = buffer.getTemperature();
        float[] baseTempArr = buffer.getBaselineTemperature();
        long[] h3Arr = buffer.getH3Indexes();
        H3Service h3Service = H3Service.getInstance();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            double curTemp = tempArr[i];
            if (Float.isNaN(baseTempArr[i])) {
                baseTempArr[i] = (float) curTemp;
            }
            double baselineTemp = baseTempArr[i];
            double lat = 0.0;
            if (h3Arr != null && h3Arr[i] != 0L) {
                try {
                    lat = h3Service.cellToLatLng(h3Arr[i]).lat;
                } catch (Exception ignored) {
                    lat = 0.0;
                }
            }
            double latFactor = Math.cos(Math.toRadians(lat));
            // Polar amplification: higher latitudes experience greater orbital temperature shifts relative to baseline climate
            double localShift = globalTempShiftC * (0.5 + 1.0 * (1.0 - latFactor));
            double targetTemp = baselineTemp + localShift;
            tempArr[i] = (float) (curTemp + (targetTemp - curTemp) * thermalRelaxationFactor);
        }
    }
}
