/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.data.EmpiricalBiomeClassifier;
import org.ether.society.data.WorldClimEmpiricalRasterLoader;

/**
 * 2.5D Physical Planetary Atmosphere, Hydrology &amp; Seasonality Solver.
 * <p>
 * Universally applicable to arbitrary procedural and extraterrestrial planetary bodies
 * (Earth, Mars, Venus, Moon, Exoplanets) with zero calendar-year or Earth coordinate hardcoding
 * ({@code AGENTS.md} Â§1, Â§10.2, Â§10.3).
 * </p>
 *
 * @author Silvere Martin-Michiellot &amp; Gemini AI
 * @version 1.0.0-beta.2
 */
public class PhysicalPlanetaryAtmosphereSolver {

    /**
     * Solved 2D climatological fields across an equirectangular planetary grid.
     *
     * @param temperatureGrid   mean annual surface temperature (Â°C)
     * @param precipitationGrid annual precipitation (mm/yr)
     * @param seasonalityGrid   annual temperature range (Â°C)
     * @param biomeGrid         24-bit RGB Whittaker biome classification
     */
    public record PlanetaryClimateResult(
        float[][] temperatureGrid,
        float[][] precipitationGrid,
        float[][] seasonalityGrid,
        int[][] biomeGrid
    ) {}

    /**
     * Solves coupled 2.5D planetary climatology over a heightmap grid.
     *
     * @param elevGrid         elevation grid in meters (negative = ocean/depression, positive = land/peaks)
     * @param width            grid width
     * @param height           grid height
     * @param solarFluxRatio   solar irradiance ratio relative to Earth ($S / S_0$)
     * @param greenhouseFactor atmospheric greenhouse opacity factor
     * @param oceanCoverage    fraction of planet surface covered by liquid bodies in $[0.0, 1.0]$
     * @param axialTiltDeg     axial tilt obliquity in degrees
     * @return solved temperature, precipitation, seasonality, and physical biome grids
     */
    public static PlanetaryClimateResult solveClimate(
            float[][] elevGrid,
            int width,
            int height,
            double solarFluxRatio,
            double greenhouseFactor,
            double oceanCoverage,
            double axialTiltDeg) {

        float[][] temp = new float[height][width];
        float[][] precip = new float[height][width];
        float[][] season = new float[height][width];
        int[][] biomes = new int[height][width];

        double baseEquatorT = (28.0 * Math.sqrt(Math.max(0.0, solarFluxRatio))) + (greenhouseFactor - 1.0) * 15.0;
        double basePoleT = (-35.0 * Math.sqrt(Math.max(0.0, solarFluxRatio))) + (greenhouseFactor - 1.0) * 12.0;

        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y + 0.5) * 180.0 / height;
            double absLat = Math.abs(lat);

            for (int x = 0; x < width; x++) {
                float elev = (elevGrid != null) ? elevGrid[y][x] : 0.0f;
                boolean isLand = elev >= 0.0f;

                double insolFrac = Math.pow(Math.cos(Math.toRadians(absLat)), 1.2);
                double t = basePoleT + (baseEquatorT - basePoleT) * insolFrac;

                if (isLand && elev > 0) {
                    t -= 0.0065 * elev;
                }

                temp[y][x] = (float) t;

                double insolSummer = Math.cos(Math.toRadians(Math.max(0.0, absLat - axialTiltDeg)));
                double insolWinter = Math.cos(Math.toRadians(Math.min(90.0, absLat + axialTiltDeg)));
                double deltaInsol = Math.abs(insolSummer - insolWinter) * Math.abs(baseEquatorT - basePoleT) * 0.65;

                if (!isLand && oceanCoverage > 0.05) {
                    season[y][x] = (float) Math.clamp(2.0 + deltaInsol * 0.15, 1.5, 8.0);
                } else {
                    double continentalityBoost = SmoothMath.softplus(absLat - 35.0, 0.25) * 0.45;
                    season[y][x] = (float) Math.clamp(deltaInsol + continentalityBoost, 2.0, 65.0);
                }
            }
        }

        // Smooth C^1 zonal wind profile across Hadley, Ferrel, and Polar cells
        double[][] uWind = new double[height][width];
        double[][] vWind = new double[height][width];

        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y + 0.5) * 180.0 / height;
            double absLat = Math.abs(lat);
            double signLat = Math.signum(lat);
            // Smooth 3-cell zonal wind harmonic: Easterlies (0-30A°), Westerlies (30-60A°), Polar Easterlies (60-90A°)
            double u = -9.0 * Math.cos(Math.toRadians(absLat * 3.0)) * (1.0 - 0.35 * (absLat / 90.0));

            for (int x = 0; x < width; x++) {
                uWind[y][x] = u;
                vWind[y][x] = -signLat * 2.0 * Math.sin(Math.toRadians(absLat * 2.0));
            }
        }

        if (oceanCoverage <= 0.01) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    precip[y][x] = 0.0f;
                }
            }
        } else {
            float[][] vaporGrid = new float[height][width];

            for (int y = 0; y < height; y++) {
                double lat = 90.0 - (y + 0.5) * 180.0 / height;
                double absLat = Math.abs(lat);

                for (int x = 0; x < width; x++) {
                    float elev = (elevGrid != null) ? elevGrid[y][x] : 0.0f;
                    boolean isOcean = elev < 0.0f;
                    double t = temp[y][x];
                    double effT = SmoothMath.softmax(-5.0, t, 0.5);
                    double satVapor = 6.112 * Math.exp((17.67 * effT) / (effT + 243.5));

                    if (isOcean) {
                        vaporGrid[y][x] = (float) (satVapor * 32.0);
                    } else {
                        double warmVegetationGate = SmoothMath.sigmoid(t, 5.0, 2.5);
                        double latFactor = SmoothMath.softplus(1.0 - absLat / 70.0, 8.0);
                        double bioticRecycle = satVapor * (5.0 + 13.0 * warmVegetationGate * latFactor);
                        vaporGrid[y][x] = (float) bioticRecycle;
                    }
                }
            }

            int advectionSteps = 24;
            for (int step = 0; step < advectionSteps; step++) {
                float[][] nextVapor = new float[height][width];
                for (int y = 0; y < height; y++) {
                    int ym = Math.clamp(y - 1, 0, height - 1);
                    int yp = Math.clamp(y + 1, 0, height - 1);

                    for (int x = 0; x < width; x++) {
                        int xm = (x - 1 + width) % width;
                        int xp = (x + 1) % width;

                        double u = uWind[y][x];
                        double v = vWind[y][x];
                        double wx = SmoothMath.sigmoid(u, 0.0, 1.0);
                        double wy = SmoothMath.sigmoid(v, 0.0, 0.5);
                        double upwindX = wx * vaporGrid[y][xm] + (1.0 - wx) * vaporGrid[y][xp];
                        double upwindY = wy * vaporGrid[yp][x] + (1.0 - wy) * vaporGrid[ym][x];

                        double advX = 0.55 * upwindX + 0.45 * vaporGrid[y][x];
                        double advY = 0.30 * upwindY + 0.70 * advX;
                        nextVapor[y][x] = (float) advY;
                    }
                }
                vaporGrid = nextVapor;
            }

            for (int y = 0; y < height; y++) {
                double lat = 90.0 - (y + 0.5) * 180.0 / height;
                double absLat = Math.abs(lat);

                for (int x = 0; x < width; x++) {
                    int xm = (x - 1 + width) % width;
                    int xp = (x + 1) % width;

                    float elev = (elevGrid != null) ? elevGrid[y][x] : 0.0f;
                    boolean isLand = elev >= 0.0f;

                    double itczConvection = 2200.0 * Math.exp(-(absLat * absLat) / 140.0)
                            * SmoothMath.sigmoid(temp[y][x], 21.0, 3.0);
                    double stormTrack = 750.0 * Math.exp(-Math.pow(absLat - 52.0, 2) / 100.0);
                    double hadleyDesiccation = Math.exp(-Math.pow(absLat - 25.0, 2) / 60.0);

                    if (!isLand) {
                        double marineRain = (vaporGrid[y][x] * 1.6) + itczConvection * 0.9 + stormTrack;
                        marineRain *= (1.0 - hadleyDesiccation * 0.55);
                        precip[y][x] = (float) Math.clamp(marineRain, 60.0, 3800.0);
                    } else {
                        float dzdx = ((elevGrid != null ? elevGrid[y][xp] : 0) - (elevGrid != null ? elevGrid[y][xm] : 0)) / 2.0f;
                        double u = uWind[y][x];

                        double orographicLift = SmoothMath.softplus(-u * dzdx * 0.15, 0.2);
                        double rain = (vaporGrid[y][x] * 1.35) + itczConvection + stormTrack * 0.85 + orographicLift;
                        rain *= (1.0 - hadleyDesiccation * 0.78);

                        // Smooth C^infty leeward Foehn rain-shadow attenuation
                        double leewardShadow = SmoothMath.sigmoid(u * dzdx, 60.0, 20.0);
                        rain *= (1.0 - 0.65 * leewardShadow);

                        precip[y][x] = (float) Math.clamp(rain, 10.0, 4200.0);
                    }
                }
            }
        }

        // 4. Pure Physical Whittaker Biome Classification (Zero hardcoded calendar year or Earth lat/lon boxes)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float elev = (elevGrid != null) ? elevGrid[y][x] : 0.0f;
                if (elev < 0.0f) {
                    biomes[y][x] = (elev < -2000.0f)
                            ? WorldClimEmpiricalRasterLoader.BIOME_DEEP_OCEAN
                            : WorldClimEmpiricalRasterLoader.BIOME_OCEAN;
                } else {
                    biomes[y][x] = EmpiricalBiomeClassifier.classifyPurePhysicalWhittakerBiome(
                            temp[y][x], precip[y][x], elev);
                }
            }
        }

        return new PlanetaryClimateResult(temp, precip, season, biomes);
    }
}
