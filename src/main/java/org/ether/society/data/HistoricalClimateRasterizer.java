/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.generation.SimplexNoise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

/**
 * High-precision Geophysical and Paleoclimatic Rasterizer for Historical Earth Cartography.
 * 
 * <p>Extracted from {@code HistoricalMapGenerator} to satisfy the Single Responsibility Principle (SRP).
 * Synthesizes planetary temperature distributions, annual precipitation patterns, seasonality amplitudes,
 * and biomes across historical and paleolithic epochs (-100,000 BP to present-day).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class HistoricalClimateRasterizer {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalClimateRasterizer.class);

    public static final int WIDTH = 2048;
    public static final int HEIGHT = 1024;

    public static final int BIOME_DEEP_OCEAN = 0x000032; // RGB(0, 0, 50)
    public static final int BIOME_OCEAN      = 0x001464; // RGB(0, 20, 100)
    public static final int BIOME_BEACH      = 0xF0DC96; // RGB(240, 220, 150)
    public static final int BIOME_PLAINS     = 0x64C832; // RGB(100, 200, 50)
    public static final int BIOME_FOREST     = 0x147814; // RGB(20, 120, 20)
    public static final int BIOME_JUNGLE     = 0x005000; // RGB(0, 80, 0)
    public static final int BIOME_DESERT     = 0xFFC832; // RGB(255, 200, 50)
    public static final int BIOME_HILLS      = 0x969664; // RGB(150, 150, 100)
    public static final int BIOME_MOUNTAINS  = 0x646464; // RGB(100, 100, 100)
    public static final int BIOME_TUNDRA     = 0x96C8DC; // RGB(150, 200, 220)
    public static final int BIOME_SNOW       = 0xFFFFFF; // RGB(255, 255, 255)
    public static final int BIOME_GLACIER    = 0xDCF0FF; // RGB(220, 240, 255)

    private static final SimplexNoise CLIMATE_NOISE = new SimplexNoise(424242L);

    private HistoricalClimateRasterizer() {
        // Pure static rasterization engine
    }

    /**
     * Computes the mean annual surface temperature at a given geographic coordinate and elevation.
     *
     * @param lat latitude in degrees [-90, +90]
     * @param lon longitude in degrees [-180, +180]
     * @param elevM elevation above sea level in meters
     * @param year historical epoch year (negative for BCE/BP)
     * @return surface temperature in degrees Celsius
     */
    public static double computeSurfaceTemperature(double lat, double lon, double elevM, long year) {
        double radLat = Math.toRadians(lat);
        double radLon = Math.toRadians(lon);
        double cosLat = Math.cos(radLat);
        double sinLat = Math.sin(radLat);

        double nx = cosLat * Math.cos(radLon);
        double ny = cosLat * Math.sin(radLon);
        double nz = sinLat;

        double tEq = 28.0;
        double tNorthPole = -30.0;
        double tSouthPole = -48.0;

        if (year <= -70000L) {
            tEq = 28.5; tNorthPole = -26.0; tSouthPole = -44.0; // Eemian / MIS 5e
        } else if (year <= -40000L) {
            tEq = 25.5; tNorthPole = -40.0; tSouthPole = -54.0; // MIS 3
        } else if (year <= -22000L) {
            tEq = 24.0; tNorthPole = -48.0; tSouthPole = -58.0; // LGM Onset
        } else if (year <= -18000L) {
            tEq = 23.0; tNorthPole = -52.0; tSouthPole = -62.0; // LGM Peak
        } else if (year <= -10500L) {
            tEq = 24.5; tNorthPole = -44.0; tSouthPole = -54.0; // Younger Dryas
        } else if (year <= -9000L) {
            tEq = 26.8; tNorthPole = -34.0; tSouthPole = -50.0; // Early Holocene
        } else if (year <= -7000L) {
            tEq = 27.2; tNorthPole = -31.0; tSouthPole = -49.0; // Early Neolithic
        } else if (year <= -4500L) {
            tEq = 28.0; tNorthPole = -28.0; tSouthPole = -47.0; // Holocene Optimum
        }

        // 1. Asymmetric hemispheric thermal baseline (thermal equator is naturally at ~6°N)
        double baseTemp;
        if (lat >= 6.0) {
            double f = Math.sin(Math.toRadians((lat - 6.0) * (90.0 / 84.0)));
            baseTemp = tEq * (1.0 - f * f) + tNorthPole * (f * f);
        } else {
            double f = Math.sin(Math.toRadians((6.0 - lat) * (90.0 / 96.0)));
            baseTemp = tEq * (1.0 - f * f) + tSouthPole * (f * f);
        }

        double tempC = baseTemp;

        // 2. Global Ocean Currents & Western/Eastern Boundary Gyres
        if (year > -10500L || year <= -70000L) {
            double dGulf = Math.exp(-(Math.pow(lat - 56.0, 2) / 220.0 + Math.pow(lon - 5.0, 2) / 600.0));
            tempC += dGulf * 7.5;
        }
        double dKuroshio = Math.exp(-(Math.pow(lat - 35.0, 2) / 120.0 + Math.pow(lon - 140.0, 2) / 300.0));
        tempC += dKuroshio * 3.5;

        double dHumboldt = Math.exp(-(Math.pow(lat - (-22.0), 2) / 250.0 + Math.pow(lon - (-75.0), 2) / 60.0));
        tempC -= dHumboldt * 4.5;
        double dBenguela = Math.exp(-(Math.pow(lat - (-24.0), 2) / 200.0 + Math.pow(lon - 12.0, 2) / 50.0));
        tempC -= dBenguela * 4.0;
        double dCalif = Math.exp(-(Math.pow(lat - 32.0, 2) / 150.0 + Math.pow(lon - (-122.0), 2) / 60.0));
        tempC -= dCalif * 3.5;
        double dCanary = Math.exp(-(Math.pow(lat - 24.0, 2) / 150.0 + Math.pow(lon - (-18.0), 2) / 60.0));
        tempC -= dCanary * 3.0;
        double dLabrador = Math.exp(-(Math.pow(lat - 54.0, 2) / 100.0 + Math.pow(lon - (-56.0), 2) / 100.0));
        tempC -= dLabrador * 6.5;
        double dOyashio = Math.exp(-(Math.pow(lat - 50.0, 2) / 100.0 + Math.pow(lon - 155.0, 2) / 120.0));
        tempC -= dOyashio * 5.0;

        // 3. Deep Continental Winter Cold Poles (Continentality)
        if (elevM >= 0.0) {
            double dSiberia = (Math.pow(lat - 64.0, 2) / 220.0) + (Math.pow(lon - 125.0, 2) / 800.0);
            if (dSiberia < 4.0) tempC -= (16.0 * Math.exp(-dSiberia * 0.5));

            double dCanada = (Math.pow(lat - 62.0, 2) / 180.0) + (Math.pow(lon - (-105.0), 2) / 600.0);
            if (dCanada < 4.0) tempC -= (11.0 * Math.exp(-dCanada * 0.5));
        }

        // 4. Elevation Lapse Rate (-6.5 °C per 1000m on land)
        if (elevM > 0.0) {
            tempC -= 0.0065 * elevM;
        }

        // 5. Glacial Cold Dome & Albedo Cooling
        if (year <= -18000L) {
            double dLaurentide = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_LGM);
            double dFenno = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_LGM);
            double dAlps = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_ALPS_LGM);
            double dPatagonia = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_PATAGONIA_LGM);

            double coolLaurentide = 16.0 / (1.0 + Math.exp(dLaurentide / 2.5));
            double coolFenno = 14.0 / (1.0 + Math.exp(dFenno / 2.0));
            double coolAlps = 9.0 / (1.0 + Math.exp(dAlps / 1.5));
            double coolPatagonia = 8.0 / (1.0 + Math.exp(dPatagonia / 1.5));
            tempC -= (coolLaurentide + coolFenno + coolAlps + coolPatagonia);
        } else if (year <= -40000L) {
            double dLaurentide = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_MIS3);
            double dFenno = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_MIS3);
            double coolLaurentide = 11.0 / (1.0 + Math.exp(dLaurentide / 2.5));
            double coolFenno = 10.0 / (1.0 + Math.exp(dFenno / 2.0));
            tempC -= (coolLaurentide + coolFenno);
        } else if (year <= -10500L) {
            double dLaurentide = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_YD);
            double dFenno = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_YD);
            double coolLaurentide = 12.0 / (1.0 + Math.exp(dLaurentide / 2.5));
            double coolFenno = 11.0 / (1.0 + Math.exp(dFenno / 2.0));
            double amocPlume = Math.exp(-(Math.pow(lat - 56.0, 2) + Math.pow(lon - (-15.0), 2)) / 250.0);
            tempC -= (coolLaurentide + coolFenno + amocPlume * 9.0);
        }

        // 6. Spherical Harmonic & Planetary Wave Meander Noise (~1.5°C)
        double thermalNoise = CLIMATE_NOISE.noise(nx * 2.5, ny * 2.5, nz * 2.5) * 1.8
                            + CLIMATE_NOISE.noise(nx * 6.0, ny * 6.0, nz * 6.0) * 0.7;
        tempC += thermalNoise;

        return Math.clamp(tempC, -50.0, 50.0);
    }

    /**
     * Computes the mean annual precipitation in millimeters at a given geographic coordinate.
     *
     * @param lat latitude in degrees [-90, +90]
     * @param lon longitude in degrees [-180, +180]
     * @param elevM elevation above sea level in meters
     * @param year historical epoch year
     * @return annual precipitation in millimeters (mm)
     */
    public static double computeAnnualPrecipitation(double lat, double lon, double elevM, long year) {
        double radLat = Math.toRadians(lat);
        double radLon = Math.toRadians(lon);
        double cosLat = Math.cos(radLat);
        double sinLat = Math.sin(radLat);
        double nx = cosLat * Math.cos(radLon);
        double ny = cosLat * Math.sin(radLon);
        double nz = sinLat;

        // 1. Asymmetric undulating ITCZ
        double itczLat = 6.0 + 3.0 * Math.sin(radLon * 2.0 + 0.5) + 2.0 * Math.cos(radLon * 3.0);
        double dItcz = Math.abs(lat - itczLat);
        double rainMm = 2400.0 * Math.exp(-(dItcz * dItcz) / 100.0);

        // 2. Subtropical Hadley Descending Arid Belts
        double dryNorth = Math.exp(-Math.pow(lat - 26.0, 2) / 65.0);
        double drySouth = Math.exp(-Math.pow(lat - (-24.0), 2) / 60.0);
        rainMm *= (1.0 - Math.max(dryNorth, drySouth) * 0.78);

        // 3. Mid-latitude Storm Tracks
        double stormNorth = Math.exp(-Math.pow(lat - 50.0, 2) / 80.0) * 1050.0;
        double stormSouth = Math.exp(-Math.pow(lat - (-48.0), 2) / 75.0) * 1250.0;
        rainMm += (stormNorth + stormSouth);

        // 4. Polar Aridification (> 62° N/S)
        double absLat = Math.abs(lat);
        if (absLat > 62.0) {
            rainMm = Math.max(60.0, rainMm * Math.exp(-(absLat - 62.0) / 8.5));
        }

        // 5. Regional Wind Advection & Major Monsoons
        double dAmazon = Math.exp(-(Math.pow(lat - (-3.0), 2) / 120.0 + Math.pow(lon - (-62.0), 2) / 250.0));
        rainMm += dAmazon * 1400.0;
        double dCongo = Math.exp(-(Math.pow(lat - 0.0, 2) / 80.0 + Math.pow(lon - 22.0, 2) / 120.0));
        rainMm += dCongo * 1200.0;
        double dGuinea = Math.exp(-(Math.pow(lat - 6.5, 2) / 30.0 + Math.pow(lon - (-2.0), 2) / 180.0));
        rainMm += dGuinea * 1100.0;
        double dSunda = Math.exp(-(Math.pow(lat - 0.0, 2) / 120.0 + Math.pow(lon - 120.0, 2) / 450.0));
        rainMm += dSunda * 1500.0;

        double dIndia = Math.exp(-(Math.pow(lat - 22.0, 2) / 90.0 + Math.pow(lon - 82.0, 2) / 160.0));
        rainMm += dIndia * 950.0;
        double dEastAsia = Math.exp(-(Math.pow(lat - 28.0, 2) / 100.0 + Math.pow(lon - 118.0, 2) / 180.0));
        rainMm += dEastAsia * 750.0;

        double dAtacama = Math.exp(-(Math.pow(lat - (-22.0), 2) / 120.0 + Math.pow(lon - (-70.0), 2) / 25.0));
        rainMm *= (1.0 - dAtacama * 0.92);
        double dNamib = Math.exp(-(Math.pow(lat - (-24.0), 2) / 80.0 + Math.pow(lon - 14.5, 2) / 20.0));
        rainMm *= (1.0 - dNamib * 0.90);
        double dSomalia = Math.exp(-(Math.pow(lat - 7.0, 2) / 60.0 + Math.pow(lon - 46.0, 2) / 60.0));
        rainMm *= (1.0 - dSomalia * 0.75);
        double dSaharaCore = Math.exp(-(Math.pow(lat - 24.0, 2) / 70.0 + Math.pow(lon - 12.0, 2) / 350.0));
        rainMm *= (1.0 - dSaharaCore * 0.85);
        double dArabiaCore = Math.exp(-(Math.pow(lat - 23.0, 2) / 50.0 + Math.pow(lon - 48.0, 2) / 100.0));
        rainMm *= (1.0 - dArabiaCore * 0.85);

        double dPacNW = Math.exp(-(Math.pow(lat - 48.0, 2) / 60.0 + Math.pow(lon - (-125.0), 2) / 40.0));
        rainMm += dPacNW * 1300.0;
        double dWestEuro = Math.exp(-(Math.pow(lat - 54.0, 2) / 70.0 + Math.pow(lon - (-5.0), 2) / 70.0));
        rainMm += dWestEuro * 850.0;
        double dChile = Math.exp(-(Math.pow(lat - (-46.0), 2) / 70.0 + Math.pow(lon - (-74.0), 2) / 30.0));
        rainMm += dChile * 1600.0;
        double dNZ = Math.exp(-(Math.pow(lat - (-43.0), 2) / 30.0 + Math.pow(lon - 171.0, 2) / 30.0));
        rainMm += dNZ * 1800.0;

        double dGobi = Math.exp(-(Math.pow(lat - 41.0, 2) / 80.0 + Math.pow(lon - 90.0, 2) / 350.0));
        rainMm *= (1.0 - dGobi * 0.80);

        // 6. Orographic Precipitation & Elevation Coupling
        if (elevM > 350.0 && absLat < 65.0) {
            double oro = Math.min(1000.0, (elevM - 350.0) * 0.38);
            rainMm += oro;
        }

        // 7. Paleoclimatic Epoch Monsoon Shifts
        if (year <= -70000L || (year <= -4500L && year >= -10000L)) {
            double greenSahara = Math.exp(-(Math.pow(lat - 21.0, 2) / 90.0 + Math.pow(lon - 14.0, 2) / 500.0));
            double greenArabia = Math.exp(-(Math.pow(lat - 22.0, 2) / 55.0 + Math.pow(lon - 48.0, 2) / 120.0));
            rainMm += (greenSahara * 950.0 + greenArabia * 650.0);
        } else if (year <= -18000L && year >= -25000L) {
            rainMm *= (1.0 - 0.40 / (1.0 + Math.exp(-(absLat - 30.0) / 6.0)));
        }

        // 8. Atmospheric Turbulence Noise
        double fluidNoise = CLIMATE_NOISE.noise(nx * 3.0 + 50.0, ny * 3.0 + 50.0, nz * 3.0 + 50.0) * 0.15
                          + CLIMATE_NOISE.noise(nx * 7.0 + 80.0, ny * 7.0 + 80.0, nz * 7.0 + 80.0) * 0.08;
        rainMm *= (1.0 + fluidNoise);

        return Math.clamp(rainMm, 0.0, 3000.0);
    }

    /**
     * Computes the seasonal thermal amplitude in degrees Celsius.
     *
     * @param lat latitude in degrees
     * @param lon longitude in degrees
     * @param elevM elevation in meters
     * @param year historical epoch year
     * @return seasonality thermal amplitude in degrees Celsius
     */
    public static double computeSeasonalityAmplitude(double lat, double lon, double elevM, long year) {
        double radLat = Math.toRadians(lat);
        double radLon = Math.toRadians(lon);
        double cosLat = Math.cos(radLat);
        double sinLat = Math.sin(radLat);
        double nx = cosLat * Math.cos(radLon);
        double ny = cosLat * Math.sin(radLon);
        double nz = sinLat;

        double absLat = Math.abs(lat);

        // 1. Orbital Obliquity Amplitude (Milankovitch)
        double obliqFactor = (year <= -70000L) ? 1.05 : ((year <= -18000L) ? 0.96 : 1.0);
        double insolationSeasonality = 35.0 * Math.sin(Math.toRadians(absLat)) * obliqFactor;

        // 2. Continentality vs Maritime Thermal Inertia
        double continentality = 1.0;
        if (elevM < 0.0) {
            continentality = 0.22;
        } else {
            double dSiberia = (Math.pow(lat - 62.0, 2) / 250.0) + (Math.pow(lon - 120.0, 2) / 900.0);
            if (dSiberia < 4.0) continentality += (1.4 * Math.exp(-dSiberia * 0.5));

            double dCanada = (Math.pow(lat - 58.0, 2) / 200.0) + (Math.pow(lon - (-100.0), 2) / 700.0);
            if (dCanada < 4.0) continentality += (1.1 * Math.exp(-dCanada * 0.5));
        }

        double ampC = (insolationSeasonality * continentality) * 0.85;

        double seasonNoise = CLIMATE_NOISE.noise(nx * 3.5 + 200.0, ny * 3.5 + 200.0, nz * 3.5 + 200.0) * 1.5;
        ampC += seasonNoise;

        return Math.clamp(ampC, 1.0, 65.0);
    }

    /**
     * Rasterizes the global historical biome classification map.
     *
     * @param year epoch year
     * @return RGB BufferedImage of biomes
     */
    public static BufferedImage rasterizeBiomesMap(long year) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        float[][] etopo = EtopoGeoTiffReader.loadEtopoGrid(WIDTH, HEIGHT);
        BufferedImage elevMask = HistoricalMapGenerator.loadElevationMask();

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) * 180.0 / HEIGHT;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) * 360.0 / WIDTH;
                double elevM = (etopo != null) ? etopo[y][x] : ((elevMask != null && (elevMask.getRGB(x, y) & 0xFF) > 128) ? 100.0 : -100.0);
                boolean isLand = elevM >= 0.0;

                if (!isLand) {
                    img.setRGB(x, y, (elevM < -2000.0) ? BIOME_DEEP_OCEAN : BIOME_OCEAN);
                    continue;
                }

                double tempC = WorldClimEmpiricalRasterLoader.getTemperature(lat, lon, elevM, year);
                double rainMm = WorldClimEmpiricalRasterLoader.getPrecipitation(lat, lon, elevM, year);

                int bColor = WorldClimEmpiricalRasterLoader.classifyBiome(tempC, rainMm, elevM, lat, lon, year);
                img.setRGB(x, y, bColor);
            }
        }
        return img;
    }

    /**
     * Rasterizes the global temperature map encoded in grayscale.
     *
     * @param year epoch year
     * @return grayscale BufferedImage [-50°C, +50°C]
     */
    public static BufferedImage rasterizeTemperatureMap(long year) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        float[][] etopo = EtopoGeoTiffReader.loadEtopoGrid(WIDTH, HEIGHT);
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;
                double elevM = (etopo != null) ? etopo[y][x] : ((mask != null && (mask.getRaster().getSample(x, y, 0) > 0)) ? 100.0 : -100.0);

                double tempC = WorldClimEmpiricalRasterLoader.getTemperature(lat, lon, elevM, year);

                double norm = Math.clamp((tempC + 50.0) / 100.0, 0.0, 1.0);
                int gray = (int) Math.round(norm * 255.0);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return img;
    }

    /**
     * Rasterizes the global annual precipitation map encoded in perceptual square-root grayscale.
     *
     * @param year epoch year
     * @return grayscale BufferedImage
     */
    public static BufferedImage rasterizePrecipitationMap(long year) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        float[][] etopo = EtopoGeoTiffReader.loadEtopoGrid(WIDTH, HEIGHT);
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;
                double elevM = (etopo != null) ? etopo[y][x] : ((mask != null && (mask.getRaster().getSample(x, y, 0) > 0)) ? 100.0 : -100.0);

                double rainMm = WorldClimEmpiricalRasterLoader.getPrecipitation(lat, lon, elevM, year);

                double norm = Math.clamp(Math.sqrt(rainMm / 4000.0), 0.0, 1.0);
                int gray = (int) Math.round(norm * 255.0);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return img;
    }

    /**
     * Rasterizes the global seasonal amplitude map encoded in grayscale.
     *
     * @param year epoch year
     * @return grayscale BufferedImage [1°C, 65°C]
     */
    public static BufferedImage rasterizeSeasonalityMap(long year) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        float[][] etopo = EtopoGeoTiffReader.loadEtopoGrid(WIDTH, HEIGHT);
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / WIDTH * 360.0;
                double elevM = (etopo != null) ? etopo[y][x] : ((mask != null && (mask.getRaster().getSample(x, y, 0) > 0)) ? 100.0 : -100.0);

                double ampC = WorldClimEmpiricalRasterLoader.getSeasonality(lat, lon, elevM, year);

                double norm = Math.clamp((ampC - 1.0) / 60.0, 0.0, 1.0);
                int gray = (int) Math.round(norm * 255.0);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return img;
    }
}
