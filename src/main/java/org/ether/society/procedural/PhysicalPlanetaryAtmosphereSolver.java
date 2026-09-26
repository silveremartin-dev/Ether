/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import java.awt.image.BufferedImage;
import org.ether.society.data.WorldClimEmpiricalRasterLoader;

/**
 * 2.5D Physical Planetary Atmosphere, Hydrology & Seasonality Solver.
 * Applicable to arbitrary procedural and extraterrestrial planetary bodies (Mars, Venus, Moon, Exoplanets).
 *
 * Physical Principles:
 * 1. Zonal Atmospheric Circulation & Wind Vectors: Hadley, Ferrel, Polar cells, Coriolis trade winds, Mid-latitude westerlies.
 * 2. Clausius-Clapeyron Thermodynamic Saturation: e_s(T) = 6.112 * exp((17.67 * T) / (T + 243.5)).
 * 3. 2D Moisture Advection-Diffusion: Vapor transport with wind velocity fields v(x, y).
 * 4. Directional Orographic Lift Condensation: Condensation occurs proportional to max(0, v . grad(z)), generating authentic windward downpours and leeward desert rain shadows.
 * 5. Radiative Equilibrium & Thermal Capacity Convolution for Seasonality: Differentiates ocean heat capacity buffer from continental landmass extremes.
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 1.0.0-beta.1
 */
public class PhysicalPlanetaryAtmosphereSolver {

    public record PlanetaryClimateResult(
        float[][] temperatureGrid,   // °C
        float[][] precipitationGrid, // mm/year
        float[][] seasonalityGrid,   // °C annual range
        int[][] biomeGrid           // RGB biome color
    ) {}

    /**
     * Solve coupled 2.5D planetary climatology over a heightmap grid.
     *
     * @param elevGrid Elevation grid in meters (negative = ocean/depression, positive = land/peaks)
     * @param width Grid width (e.g. 2048)
     * @param height Grid height (e.g. 1024)
     * @param solarFluxRatio Solar irradiance ratio relative to Earth (e.g. Earth=1.0, Mars=0.43, Venus=1.91)
     * @param greenhouseFactor Atmospheric greenhouse opacity (e.g. Earth=1.0, Mars=0.05, Venus=75.0)
     * @param oceanCoverage Fraction of planet surface with liquid bodies (0.0 to 1.0)
     * @param axialTiltDeg Axial tilt obliquity in degrees (e.g. Earth=23.44, Mars=25.19, Venus=177.3)
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

        // 1. Planetary Temperature Baseline & Radiative Balance
        double baseEquatorT = (28.0 * Math.sqrt(solarFluxRatio)) + (greenhouseFactor - 1.0) * 15.0;
        double basePoleT    = (-35.0 * Math.sqrt(solarFluxRatio)) + (greenhouseFactor - 1.0) * 12.0;

        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y + 0.5) * 180.0 / height;
            double absLat = Math.abs(lat);
            double cosLat = Math.cos(Math.toRadians(lat));
            double sinLat = Math.sin(Math.toRadians(lat));

            for (int x = 0; x < width; x++) {
                float elev = (elevGrid != null) ? elevGrid[y][x] : 0.0f;
                boolean isLand = elev >= 0.0f;

                // Latitude insolation curve
                double insolFrac = Math.pow(Math.cos(Math.toRadians(absLat)), 1.2);
                double t = basePoleT + (baseEquatorT - basePoleT) * insolFrac;

                // Adiabatic Lapse Rate (-6.5°C/km on land)
                if (isLand && elev > 0) {
                    t -= 0.0065 * elev;
                }

                temp[y][x] = (float) t;

                // Seasonality: Insolation range modulated by axial tilt and surface heat capacity
                double insolSummer = Math.cos(Math.toRadians(Math.max(0.0, absLat - axialTiltDeg)));
                double insolWinter = Math.cos(Math.toRadians(Math.min(90.0, absLat + axialTiltDeg)));
                double deltaInsol = Math.abs(insolSummer - insolWinter) * (baseEquatorT - basePoleT) * 0.65;

                if (!isLand && oceanCoverage > 0.05) {
                    // Ocean thermal inertia dampens seasonal amplitude strongly (2°C to 7°C)
                    season[y][x] = (float) Math.clamp(2.0 + deltaInsol * 0.15, 1.5, 8.0);
                } else {
                    // Continental land experiences large seasonal amplitude
                    double continentalityBoost = (absLat > 35.0) ? (absLat - 35.0) * 0.45 : 0.0;
                    season[y][x] = (float) Math.clamp(deltaInsol + continentalityBoost, 2.0, 65.0);
                }
            }
        }

        // 2. Wind Vectors & 2D Moisture Advection
        // Wind field: Trade Winds (0-30°: East to West), Westerlies (30-60°: West to East), Polar Easterlies (60-90°: East to West)
        double[][] uWind = new double[height][width]; // zonal wind (Eastward > 0)
        double[][] vWind = new double[height][width]; // meridional wind (Northward > 0)

        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y + 0.5) * 180.0 / height;
            double absLat = Math.abs(lat);
            double signLat = Math.signum(lat);

            double u;
            if (absLat < 30.0) {
                u = -8.0 * Math.cos(Math.toRadians(absLat * (90.0 / 30.0))); // Trade winds (Easterly < 0)
            } else if (absLat < 60.0) {
                u = 12.0 * Math.sin(Math.toRadians((absLat - 30.0) * (180.0 / 30.0))); // Westerlies (Westerly > 0)
            } else {
                u = -5.0 * Math.sin(Math.toRadians((absLat - 60.0) * (90.0 / 30.0))); // Polar Easterlies
            }

            for (int x = 0; x < width; x++) {
                uWind[y][x] = u;
                vWind[y][x] = -signLat * 2.0 * Math.sin(Math.toRadians(absLat * 2.0)); // Hadley/Ferrel meridional flow
            }
        }

        // 3. Vapor Evaporation, 2D Advection, ITCZ Convection & Biotic Moisture Recycling
        if (oceanCoverage <= 0.01) {
            // Desiccated dry world (e.g. Mars/Moon)
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    precip[y][x] = 0.0f;
                }
            }
        } else {
            // Wet world with liquid bodies & active hydrological cycle
            float[][] vaporGrid = new float[height][width];

            // 3.1 Initial Evaporation & Biotic Evapotranspiration (Clausius-Clapeyron)
            for (int y = 0; y < height; y++) {
                double lat = 90.0 - (y + 0.5) * 180.0 / height;
                double absLat = Math.abs(lat);

                for (int x = 0; x < width; x++) {
                    float elev = (elevGrid != null) ? elevGrid[y][x] : 0.0f;
                    boolean isOcean = elev < 0.0f;
                    double t = temp[y][x];
                    double satVapor = 6.112 * Math.exp((17.67 * Math.max(-5.0, t)) / (Math.max(-5.0, t) + 243.5));

                    if (isOcean) {
                        vaporGrid[y][x] = (float) (satVapor * 32.0);
                    } else {
                        // Biotic pump evapotranspiration over vegetated/warm landmasses (Amazon, Congo, Eurasia)
                        double bioticRecycle = (t > 5.0) ? (satVapor * 18.0 * Math.max(0.0, 1.0 - absLat / 70.0)) : (satVapor * 5.0);
                        vaporGrid[y][x] = (float) bioticRecycle;
                    }
                }
            }

            // 3.2 2D Multi-step Advection-Diffusion across Wind Vectors
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
                        int srcX = (u >= 0) ? xm : xp;
                        int srcY = (v >= 0) ? yp : ym;

                        double advX = 0.55 * vaporGrid[y][srcX] + 0.45 * vaporGrid[y][x];
                        double advY = 0.30 * vaporGrid[srcY][x] + 0.70 * advX;
                        nextVapor[y][x] = (float) advY;
                    }
                }
                vaporGrid = nextVapor;
            }

            // 3.3 Directional Orographic Lift Condensation + ITCZ Tropical Convective Downpours
            for (int y = 0; y < height; y++) {
                double lat = 90.0 - (y + 0.5) * 180.0 / height;
                double absLat = Math.abs(lat);
                int ym = Math.clamp(y - 1, 0, height - 1);
                int yp = Math.clamp(y + 1, 0, height - 1);

                for (int x = 0; x < width; x++) {
                    int xm = (x - 1 + width) % width;
                    int xp = (x + 1) % width;

                    float elev = (elevGrid != null) ? elevGrid[y][x] : 0.0f;
                    boolean isLand = elev >= 0.0f;

                    // Deep Tropical ITCZ Convection (Hadley cell convergence, Amazon / Congo / SE Asia)
                    double itczConvection = 2200.0 * Math.exp(-(absLat * absLat) / 140.0) * Math.clamp((temp[y][x] - 15.0) / 12.0, 0.0, 1.0);

                    // Mid-latitude storm track frontal precipitation (45° to 60°)
                    double stormTrack = 750.0 * Math.exp(-Math.pow(absLat - 52.0, 2) / 100.0);

                    // Subtropical Hadley Arid Subsidence Desiccation (20° to 32°)
                    double hadleyDesiccation = Math.exp(-Math.pow(absLat - 25.0, 2) / 60.0);

                    if (!isLand) {
                        double marineRain = (vaporGrid[y][x] * 1.6) + itczConvection * 0.9 + stormTrack;
                        marineRain *= (1.0 - hadleyDesiccation * 0.55);
                        precip[y][x] = (float) Math.clamp(marineRain, 60.0, 3800.0);
                    } else {
                        float dzdx = ((elevGrid != null ? elevGrid[y][xp] : 0) - (elevGrid != null ? elevGrid[y][xm] : 0)) / 2.0f;
                        double u = uWind[y][x];

                        // Windward slope condensation (max(0, -u * dz/dx))
                        double orographicLift = Math.max(0.0, -u * dzdx * 0.15);
                        double rain = (vaporGrid[y][x] * 1.35) + itczConvection + stormTrack * 0.85 + orographicLift;

                        // Subtropical desert suppression (Sahara, Arabia, Gobi, Atacama, Australian Outback)
                        rain *= (1.0 - hadleyDesiccation * 0.78);

                        // Leeward desiccation (Rain Shadow)
                        if (u * dzdx > 60.0) {
                            rain *= 0.35; // Sharp rain shadow behind mountain ridge
                        }

                        precip[y][x] = (float) Math.clamp(rain, 10.0, 4200.0);
                    }
                }
            }
        }

        // 4. Biome Classification
        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y + 0.5) * 180.0 / height;
            for (int x = 0; x < width; x++) {
                float elev = (elevGrid != null) ? elevGrid[y][x] : 0.0f;
                if (elev < 0.0f) {
                    biomes[y][x] = (elev < -2000.0f) ? WorldClimEmpiricalRasterLoader.BIOME_DEEP_OCEAN : WorldClimEmpiricalRasterLoader.BIOME_OCEAN;
                } else {
                    biomes[y][x] = WorldClimEmpiricalRasterLoader.classifyBiome(
                        temp[y][x], precip[y][x], elev, lat, (x + 0.5) * 360.0 / width - 180.0, 2025
                    );
                }
            }
        }

        return new PlanetaryClimateResult(temp, precip, season, biomes);
    }
}
