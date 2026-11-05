/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.ether.society.data.PlanetaryRasterSampler.BIOME_DESERT;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_GLACIER;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_HILLS;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_MOUNTAINS;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_PLAINS;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_SNOW;
import static org.ether.society.data.PlanetaryRasterSampler.HEIGHT;
import static org.ether.society.data.PlanetaryRasterSampler.WIDTH;
import static org.ether.society.data.PlanetaryRasterSampler.elevationToHypsometricColor;
import static org.ether.society.data.PlanetaryRasterSampler.rasterizeAlphaDensity;
import static org.ether.society.data.PlanetaryRasterSampler.rasterizeTieredDensity;
import static org.ether.society.data.PlanetaryRasterSampler.saveMapImage;

/**
 * Extraterrestrial Cartographic Tensor Generator for Moon, Mars, Venus, and Mercury.
 *
 * <p>Generates 2048x1024 equirectangular physical, climatic, and mineral rasters from
 * NASA PDS LOLA, MOLA, Magellan, and MESSENGER planetary datasets.</p>
 *
 * @author Silvere Martin-Michiellot
 * @see InnerSolarPlanetRasterHelper
 * @see PlanetaryRasterSampler
 */
public final class ExtraterrestrialPlanetaryRasterSampler {

    /** Diagnostic logger for extraterrestrial raster generation. */
    private static final Logger logger = LoggerFactory.getLogger(ExtraterrestrialPlanetaryRasterSampler.class);

    /**
     * Private constructor preventing instantiation of static raster utility.
     */
    private ExtraterrestrialPlanetaryRasterSampler() {
    }

    /**
     * Generates the full physical, volatile, and mineral raster suite for the Moon using NASA LOLA DEM.
     */
    public static void generateMoonMaps() {
        logger.info("Generating Moon Cartography & Resources in Grayscale [0..255] on Black Background...");

        // 1. Elevation (NASA LOLA)
        File lolaFile = new File("data/maps/nasa_pds/lunar_lola_dem_downsampled.png");
        BufferedImage srcLola = null;
        if (lolaFile.exists()) {
            try {
                srcLola = ImageIO.read(lolaFile);
            } catch (Exception ignored) {
            }
        }
        BufferedImage imgElev = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x / (double) WIDTH) * 360.0;
                double norm;
                if (srcLola != null) {
                    int sx = (int) ((x / (double) WIDTH) * srcLola.getWidth());
                    int sy = (int) ((y / (double) HEIGHT) * srcLola.getHeight());
                    sx = Math.clamp(sx, 0, srcLola.getWidth() - 1);
                    sy = Math.clamp(sy, 0, srcLola.getHeight() - 1);
                    Color c = new Color(srcLola.getRGB(sx, sy));
                    norm = (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114) / 255.0;
                } else {
                    double spaDist = Math.hypot((lon - 180.0) * Math.cos(Math.toRadians(lat)), lat - (-53.0));
                    double spaDepression = Math.max(0.0, 1.0 - spaDist / 45.0) * 0.35;
                    norm = 0.52 - spaDepression + 0.10 * Math.sin(Math.toRadians(lon * 4.0)) * Math.cos(Math.toRadians(lat * 3.0));
                }
                int gray = (int) Math.clamp(norm * 255.0, 0.0, 255.0);
                imgElev.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        saveMapImage(imgElev, "moon_elevation.png", "moon");

        // 2. Helium-3 (Mare Basalts Volatiles)
        BufferedImage imgHe3 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        double[][] mareHelium3Spots = {
            {-45.0, 20.0, 75, 2.8},  // Oceanus Procellarum
            {30.0, 8.0, 60, 2.6},    // Mare Tranquillitatis
            {18.0, 28.0, 55, 2.5},   // Mare Serenitatis
            {-17.0, 35.0, 65, 2.6},  // Mare Imbrium
            {60.0, 17.0, 48, 2.4},   // Mare Crisium
            {20.0, -18.0, 52, 2.4},  // Mare Nectaris
            {-20.0, -15.0, 55, 2.5}, // Mare Nubium
            {35.0, -3.0, 50, 2.2}    // Mare Fecunditatis
        };
        List<double[]> he3List = new ArrayList<>();
        for (double[] s : mareHelium3Spots) {
            he3List.add(s);
        }
        rasterizeAlphaDensity(imgHe3, he3List, 35.0);
        saveMapImage(imgHe3, "moon_helium3.png", "moon");

        // 3. Water Ice in Permanently Shadowed Regions
        BufferedImage imgIce = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        double[][] polarIceSpots = {
            {0.0, -89.5, 30, 2.8},   // South Pole
            {0.0, 89.0, 25, 2.5}     // North Pole
        };
        List<double[]> iceList = new ArrayList<>();
        for (double[] s : polarIceSpots) {
            iceList.add(s);
        }
        rasterizeAlphaDensity(imgIce, iceList, 20.0);
        saveMapImage(imgIce, "moon_aquifers.png", "moon");

        // 4. Iron & Titanium Ores (Ilmenite FeTiO3), Uranium, Precious Metals, Geothermal
        BufferedImage imgIron = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeTieredDensity(imgIron, he3List, 30.0);
        saveMapImage(imgIron, "moon_iron_copper.png", "moon");

        BufferedImage imgUranium = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgUranium, he3List, 25.0);
        saveMapImage(imgUranium, "moon_uranium.png", "moon");

        BufferedImage imgPrecious = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgPrecious, he3List, 25.0);
        saveMapImage(imgPrecious, "moon_precious_metals.png", "moon");

        BufferedImage imgGeo = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgGeo, he3List, 25.0);
        saveMapImage(imgGeo, "moon_geothermal.png", "moon");

        // 8-9. Zero Resource & Seasonality maps
        BufferedImage zeroMap = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        saveMapImage(zeroMap, "moon_coal.png", "moon");
        saveMapImage(zeroMap, "moon_oil.png", "moon");
        saveMapImage(zeroMap, "moon_gas.png", "moon");
        saveMapImage(zeroMap, "moon_precipitation.png", "moon");
        saveMapImage(zeroMap, "moon_seasonality.png", "moon");

        // 10. Temperature in Grayscale [0..255]
        BufferedImage imgTemp = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double tempC = -130.0 + 150.0 * Math.cos(Math.toRadians(lat));
                if (Math.abs(lat) > 87.5) tempC = -230.0;
                int gray = (int) (Math.clamp((tempC + 230.0) / 360.0, 0.0, 1.0) * 255.0);
                imgTemp.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        saveMapImage(imgTemp, "moon_temperature.png", "moon");

        // 11. Biomes
        BufferedImage imgBiomes = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                int biomeColor;
                if (Math.abs(lat) > 87.5) {
                    biomeColor = BIOME_SNOW;
                } else {
                    int elevRgb = imgElev.getRGB(x, y);
                    int r = (elevRgb >> 16) & 0xFF;
                    int g = (elevRgb >> 8) & 0xFF;
                    int b = elevRgb & 0xFF;
                    double brightness = (r * 0.299 + g * 0.587 + b * 0.114) / 255.0;
                    biomeColor = (brightness < 0.46) ? BIOME_PLAINS : (brightness < 0.70 ? BIOME_HILLS : BIOME_MOUNTAINS);
                }
                imgBiomes.setRGB(x, y, biomeColor);
            }
        }
        saveMapImage(imgBiomes, "moon_biomes.png", "moon");
    }

    /**
     * Generates the full physical, climatic, and mineral raster suite for Mars using NASA MOLA altimetry.
     */
    public static void generateMarsMaps() {
        logger.info("Generating Mars Cartography & Resources in Grayscale [0..255] on Black Background...");

        // 1. Elevation (NASA MOLA)
        File molaFile = new File("data/maps/nasa_pds/high_res_flat_mola.tif");
        BufferedImage srcMola = null;
        if (molaFile.exists()) {
            try {
                srcMola = ImageIO.read(molaFile);
            } catch (Exception ignored) {
            }
        }

        BufferedImage imgElev = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        double[][] molaAltGrid = new double[HEIGHT][WIDTH];

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x / (double) WIDTH) * 360.0;
                double norm;
                if (srcMola != null) {
                    double yNorm = y / (double) HEIGHT;
                    double safeTop = 0.12, safeBot = 0.75;
                    double syNorm = Math.clamp(yNorm, safeTop, safeBot);
                    int sx = Math.clamp((int) ((x / (double) WIDTH) * srcMola.getWidth()), 0, srcMola.getWidth() - 1);
                    int sy = Math.clamp((int) (syNorm * srcMola.getHeight()), 0, srcMola.getHeight() - 1);
                    Color c = new Color(srcMola.getRGB(sx, sy));
                    norm = (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114) / 255.0;
                    if (yNorm < safeTop) {
                        double blend = Math.clamp(1.0 - yNorm / safeTop, 0.0, 1.0);
                        norm = norm + blend * (0.85 - norm);
                    } else if (yNorm > safeBot) {
                        double blend = Math.clamp((yNorm - safeBot) / (1.0 - safeBot), 0.0, 1.0);
                        norm = norm + blend * (0.88 - norm);
                    }
                } else {
                    norm = computeSyntheticMarsElevationNorm(lat, lon);
                }

                molaAltGrid[y][x] = norm;
                int gray = (int) Math.clamp(norm * 255.0, 0.0, 255.0);
                imgElev.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        saveMapImage(imgElev, "mars_elevation.png", "mars");

        // 2-5. Biomes, Temperature, Frost Precipitation, Seasonality
        BufferedImage imgBiomes = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage imgTemp = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage imgPrecip = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage imgSeason = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            double absLat = Math.abs(lat);
            double baseVariance = 18.0 + 26.0 * Math.sin(Math.toRadians(absLat)) - 6.0 * Math.sin(Math.toRadians(lat));
            int sGray = (int) Math.clamp((baseVariance / 50.0) * 255.0, 0, 255);
            int sRgb = (sGray << 16) | (sGray << 8) | sGray;

            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x / (double) WIDTH) * 360.0;
                double altNorm = molaAltGrid[y][x];

                double northCapLimit = 76.0 + 3.5 * Math.sin(Math.toRadians(lon * 3.0 + 45.0));
                double southCapLimit = -73.0 + 4.0 * Math.cos(Math.toRadians(lon * 2.0 - 30.0));
                int biomeColor = (lat >= northCapLimit || lat <= southCapLimit)
                        ? (absLat > 83.0 ? BIOME_GLACIER : BIOME_SNOW)
                        : (altNorm > 0.78 ? BIOME_MOUNTAINS : (altNorm > 0.60 ? BIOME_HILLS : (altNorm < 0.35 ? BIOME_PLAINS : BIOME_DESERT)));
                imgBiomes.setRGB(x, y, biomeColor);

                double tempC = -55.0 - 55.0 * Math.sin(Math.toRadians(absLat)) - (altNorm - 0.45) * 45.0 + 6.0 * Math.cos(Math.toRadians(lon * 2.0));
                int tGray = (int) (Math.clamp((tempC + 130.0) / 160.0, 0.0, 1.0) * 255.0);
                imgTemp.setRGB(x, y, (tGray << 16) | (tGray << 8) | tGray);

                double orographicCloud = (altNorm > 0.75) ? (altNorm - 0.75) * 1200.0 : 0.0;
                double polarVapor = (absLat > 65.0) ? Math.pow((absLat - 65.0) / 25.0, 1.5) * 600.0 : 0.0;
                double waveMod = 80.0 * Math.max(0.0, Math.sin(Math.toRadians(lon * 2.0 + lat)));
                int pGray = (int) Math.clamp((Math.max(0.0, orographicCloud + polarVapor + waveMod) / 600.0) * 255.0, 0.0, 255.0);
                imgPrecip.setRGB(x, y, (pGray << 16) | (pGray << 8) | pGray);

                imgSeason.setRGB(x, y, sRgb);
            }
        }
        saveMapImage(imgBiomes, "mars_biomes.png", "mars");
        saveMapImage(imgTemp, "mars_temperature.png", "mars");
        saveMapImage(imgPrecip, "mars_precipitation.png", "mars");
        saveMapImage(imgSeason, "mars_seasonality.png", "mars");

        // 6-10. Mineral, Permafrost & Volatile Resources
        double[][] marsIronSpots = {
            {-2.0, 0.0, 65, 2.8}, {-21.0, 2.5, 55, 2.5}, {137.4, -4.6, 50, 2.4}, {-133.0, 18.0, 75, 2.8},
            {-112.0, 9.0, 65, 2.6}, {-119.0, 0.5, 60, 2.5}, {-120.0, -9.0, 60, 2.5}, {147.0, 25.0, 60, 2.5}
        };
        List<double[]> ironList = new ArrayList<>();
        for (double[] s : marsIronSpots) ironList.add(s);

        BufferedImage imgIron = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeTieredDensity(imgIron, ironList, 35.0);
        saveMapImage(imgIron, "mars_iron_copper.png", "mars");

        double[][] marsIceSpots = {
            {0.0, 86.0, 75, 3.0}, {0.0, -86.0, 70, 2.8}, {110.0, 45.0, 80, 2.6}, {-170.0, 40.0, 70, 2.4}, {-150.0, -5.0, 60, 2.2}
        };
        List<double[]> marsIceList = new ArrayList<>();
        for (double[] s : marsIceSpots) marsIceList.add(s);

        BufferedImage imgIce = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgIce, marsIceList, 35.0);
        saveMapImage(imgIce, "mars_aquifers.png", "mars");

        BufferedImage imgGeo = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgGeo, ironList, 35.0);
        saveMapImage(imgGeo, "mars_geothermal.png", "mars");

        BufferedImage imgUranium = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgUranium, ironList, 25.0);
        saveMapImage(imgUranium, "mars_uranium.png", "mars");

        BufferedImage imgPrecious = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgPrecious, ironList, 25.0);
        saveMapImage(imgPrecious, "mars_precious_metals.png", "mars");

        BufferedImage imgHe3 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgHe3, ironList, 20.0);
        saveMapImage(imgHe3, "mars_helium3.png", "mars");

        BufferedImage imgGas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        List<double[]> gasList = List.of(new double[]{137.4, -4.6, 40, 2.0}, new double[]{77.0, 21.0, 45, 2.2});
        rasterizeAlphaDensity(imgGas, gasList, 20.0);
        saveMapImage(imgGas, "mars_gas.png", "mars");

        BufferedImage zeroMap = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        saveMapImage(zeroMap, "mars_coal.png", "mars");
        saveMapImage(zeroMap, "mars_oil.png", "mars");
    }

    /**
     * Computes analytical Martian hypsometric elevation fallback (Tharsis Montes, Elysium, Hellas, Argyre)
     * when the high-resolution MOLA GeoTIFF is absent.
     *
     * @param lat latitude in degrees
     * @param lon longitude in degrees
     * @return normalized elevation in {@code [0.05, 0.98]}
     */
    private static double computeSyntheticMarsElevationNorm(double lat, double lon) {
        double cosLat = Math.cos(Math.toRadians(lat));
        double elev = 0.45 + (lat > 0 ? -0.15 : 0.12) * Math.sin(Math.toRadians(lat * 1.5));
        double olympusDist = Math.hypot((lon - (-133.8)) * cosLat, lat - 18.65);
        if (olympusDist < 12.0) elev += 0.52 * Math.pow(1.0 - olympusDist / 12.0, 2.0);
        double ascraeusDist = Math.hypot((lon - (-104.5)) * cosLat, lat - 11.9);
        if (ascraeusDist < 8.0) elev += 0.42 * Math.pow(1.0 - ascraeusDist / 8.0, 2.0);
        double pavonisDist = Math.hypot((lon - (-112.9)) * cosLat, lat - 0.8);
        if (pavonisDist < 8.0) elev += 0.40 * Math.pow(1.0 - pavonisDist / 8.0, 2.0);
        double arsiaDist = Math.hypot((lon - (-120.9)) * cosLat, lat - (-8.4));
        if (arsiaDist < 8.5) elev += 0.41 * Math.pow(1.0 - arsiaDist / 8.5, 2.0);
        double elysiumDist = Math.hypot((lon - 147.2) * cosLat, lat - 25.0);
        if (elysiumDist < 9.0) elev += 0.35 * Math.pow(1.0 - elysiumDist / 9.0, 2.0);
        double hellasDist = Math.hypot((lon - 70.5) * cosLat, lat - (-42.4));
        if (hellasDist < 22.0) elev -= 0.35 * Math.pow(1.0 - hellasDist / 22.0, 1.8);
        double argyreDist = Math.hypot((lon - (-43.6)) * cosLat, lat - (-49.7));
        if (argyreDist < 14.0) elev -= 0.25 * Math.pow(1.0 - argyreDist / 14.0, 1.8);
        return Math.clamp(elev, 0.05, 0.98);
    }

    /**
     * Generates the full physical, climatic, and mineral raster suite for Venus.
     */
    public static void generateVenusMaps() {
        InnerSolarPlanetRasterHelper.generateVenusMaps();
    }

    /**
     * Generates the full physical, climatic, and mineral raster suite for Mercury.
     */
    public static void generateMercuryMaps() {
        InnerSolarPlanetRasterHelper.generateMercuryMaps();
    }
}
