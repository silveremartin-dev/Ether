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
 * @version 1.0.0-beta.1
 */
public class MilankovitchOrbitalEngine {
    private static final Logger logger = LoggerFactory.getLogger(MilankovitchOrbitalEngine.class);

    /** Solar Constant TOA in W/m² */
    public static final double SOLAR_CONSTANT_TOA_W_M2 = 1361.0;

    /** Mean orbital eccentricity baseline */
    public static final double MEAN_ECCENTRICITY = 0.0167;
    public static final double ECCENTRICITY_AMPLITUDE = 0.020;
    public static final double ECCENTRICITY_PERIOD_YEARS = 100_000.0;

    /** Mean obliquity (axial tilt) in degrees */
    public static final double MEAN_OBLIQUITY_DEG = 23.44;
    public static final double OBLIQUITY_AMPLITUDE_DEG = 1.20;
    public static final double OBLIQUITY_PERIOD_YEARS = 41_000.0;

    /** Precession period in years */
    public static final double PRECESSION_PERIOD_YEARS = 23_000.0;

    public record MilankovitchParameters(double eccentricity, double obliquityDeg, double precessionAngleRad) {}

    /**
     * Computes Milankovitch orbital parameters for a given year before present (or relative to year 0 CE).
     *
     * @param year Current simulation year (negative for BCE/years before present)
     * @return MilankovitchParameters instance
     */
    public static MilankovitchParameters computeOrbitalParameters(double year) {
        double t = year;
        // Eccentricity e(t)
        double e = MEAN_ECCENTRICITY + ECCENTRICITY_AMPLITUDE * Math.sin(2.0 * Math.PI * t / ECCENTRICITY_PERIOD_YEARS);
        e = Math.clamp(e, 0.005, 0.060);

        // Obliquity ε(t) in degrees
        double eps = MEAN_OBLIQUITY_DEG + OBLIQUITY_AMPLITUDE_DEG * Math.sin(2.0 * Math.PI * t / OBLIQUITY_PERIOD_YEARS);

        // Precession longitude of perihelion
        double varpi = (2.0 * Math.PI * t / PRECESSION_PERIOD_YEARS) % (2.0 * Math.PI);

        return new MilankovitchParameters(e, eps, varpi);
    }

    /**
     * Computes daily average TOA insolation (W/m²) for a given latitude and day of year.
     *
     * @param latDeg Latitude in degrees [-90, +90]
     * @param dayOfYear Day of year [1, 365]
     * @param params MilankovitchParameters
     * @return Daily average insolation in W/m²
     */
    public static double calculateDailyInsolation(double latDeg, int dayOfYear, MilankovitchParameters params) {
        double phi = Math.toRadians(latDeg);
        double eps = Math.toRadians(params.obliquityDeg());

        // Solar true anomaly / longitude λ_s
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

    /**
     * Applies Milankovitch insolation forcing anomaly to surface temperatures across all planetary cells.
     *
     * @param cells Simulation H3 cells
     * @param currentYear Simulation year
     */
    public static void applyMilankovitchForcing(List<H3Cell> cells, double currentYear) {
        if (cells == null || cells.isEmpty()) return;

        MilankovitchParameters params = computeOrbitalParameters(currentYear);

        // Compute high-latitude summer insolation anomaly (65°N at summer solstice day 172)
        double refInsolation65N = calculateDailyInsolation(65.0, 172, new MilankovitchParameters(MEAN_ECCENTRICITY, MEAN_OBLIQUITY_DEG, 0.0));
        double curInsolation65N = calculateDailyInsolation(65.0, 172, params);
        double anomalyW_M2 = curInsolation65N - refInsolation65N;

        // Radiative forcing scaling: ~0.08 °C per W/m² of 65°N summer insolation anomaly
        double globalTempShiftC = anomalyW_M2 * 0.08;

        for (H3Cell cell : cells) {
            double baseTemp = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
            double latFactor = Math.cos(Math.toRadians(cell.getLatitude()));
            // Polar amplification: higher latitudes experience greater orbital temperature shifts
            double localShift = globalTempShiftC * (0.5 + 1.0 * (1.0 - latFactor));
            cell.setTemperature(baseTemp + localShift * 0.01);
        }
    }
}

