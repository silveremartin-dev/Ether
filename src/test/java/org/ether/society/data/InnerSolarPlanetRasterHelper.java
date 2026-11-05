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
 * Inner solar system cartographic raster generator for Venus (NASA Magellan radar altimetry)
 * and Mercury (NASA MESSENGER DEM and 3:2 spin-orbit thermal variance).
 *
 * @author Silvere Martin-Michiellot
 * @see ExtraterrestrialPlanetaryRasterSampler
 */
final class InnerSolarPlanetRasterHelper {

    /** Diagnostic logger for inner solar system raster generation. */
    private static final Logger logger = LoggerFactory.getLogger(InnerSolarPlanetRasterHelper.class);

    /**
     * Private constructor preventing instantiation of stateless helper.
     */
    private InnerSolarPlanetRasterHelper() {
    }

    /**
     * Generates the full physical, climatic, and mineral 2048x1024 cartographic raster suite
     * for Venus using NASA Magellan global mosaic relief and super-critical \(\text{CO}_2\) thermal profiles.
     */
    static void generateVenusMaps() {
        logger.info("Generating Venus Cartography & Resources in Grayscale [0..255] on Black Background...");

        // 1. Elevation (NASA Magellan)
        File magellanFile = new File("data/maps/nasa_pds/Venus_Magellan_C3-MDIR_ClrTopo_Global_Mosaic_6600m.tif");
        BufferedImage srcMagellan = null;
        if (magellanFile.exists()) {
            try {
                srcMagellan = ImageIO.read(magellanFile);
            } catch (Exception ignored) {
            }
        }
        BufferedImage imgElev = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double norm = 0.5;
                if (srcMagellan != null) {
                    int sx = (int) ((x / (double) WIDTH) * srcMagellan.getWidth());
                    int sy = (int) ((y / (double) HEIGHT) * srcMagellan.getHeight());
                    sx = Math.clamp(sx, 0, srcMagellan.getWidth() - 1);
                    sy = Math.clamp(sy, 0, srcMagellan.getHeight() - 1);
                    Color c = new Color(srcMagellan.getRGB(sx, sy));
                    norm = (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114) / 255.0;
                }
                int gray = (int) Math.clamp(norm * 255.0, 0.0, 255.0);
                imgElev.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        saveMapImage(imgElev, "venus_elevation.png", "venus");

        // 2. Temperature in Grayscale [0..255] (~465°C)
        BufferedImage imgTemp = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        int vTempGray = (int) (Math.clamp((465.0 - 400.0) / 100.0, 0.0, 1.0) * 255.0);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                imgTemp.setRGB(x, y, (vTempGray << 16) | (vTempGray << 8) | vTempGray);
            }
        }
        saveMapImage(imgTemp, "venus_temperature.png", "venus");

        // 3. Biomes
        BufferedImage imgBiomes = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int elevRgb = imgElev.getRGB(x, y);
                int r = (elevRgb >> 16) & 0xFF;
                int g = (elevRgb >> 8) & 0xFF;
                int b = elevRgb & 0xFF;
                double brightness = (r * 0.299 + g * 0.587 + b * 0.114) / 255.0;
                int biomeColor = (brightness > 0.75) ? BIOME_MOUNTAINS : (brightness > 0.55 ? BIOME_HILLS : BIOME_PLAINS);
                imgBiomes.setRGB(x, y, biomeColor);
            }
        }
        saveMapImage(imgBiomes, "venus_biomes.png", "venus");

        // 4. Geothermal Hotspots
        BufferedImage imgGeo = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        double[][] venusSpots = {
            {-165.0, 0.5, 75, 2.8},  // Maat Mons
            {-172.0, 12.5, 65, 2.6}, // Sapas Mons
            {-78.0, 25.0, 70, 2.8}   // Beta Regio
        };
        List<double[]> vList = new ArrayList<>();
        for (double[] s : venusSpots) {
            vList.add(s);
        }
        rasterizeAlphaDensity(imgGeo, vList, 40.0);
        saveMapImage(imgGeo, "venus_geothermal.png", "venus");

        // 5. Iron, Precious Metals, Uranium
        BufferedImage imgIron = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeTieredDensity(imgIron, vList, 35.0);
        saveMapImage(imgIron, "venus_iron_copper.png", "venus");

        BufferedImage imgPrecious = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgPrecious, vList, 30.0);
        saveMapImage(imgPrecious, "venus_precious_metals.png", "venus");

        BufferedImage imgUranium = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgUranium, vList, 30.0);
        saveMapImage(imgUranium, "venus_uranium.png", "venus");

        // 6. Precipitation & Seasonality in Grayscale [0..255]
        BufferedImage imgPrecip = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        int vPrecipGray = 25; // Trace sulfuric acid moisture
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                imgPrecip.setRGB(x, y, (vPrecipGray << 16) | (vPrecipGray << 8) | vPrecipGray);
            }
        }
        saveMapImage(imgPrecip, "venus_precipitation.png", "venus");

        BufferedImage imgSeason = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        int vSeasonGray = 8; // Uniform ~1-2°C seasonal variance
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                imgSeason.setRGB(x, y, (vSeasonGray << 16) | (vSeasonGray << 8) | vSeasonGray);
            }
        }
        saveMapImage(imgSeason, "venus_seasonality.png", "venus");

        // 7. Zeros
        BufferedImage zeroMap = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        saveMapImage(zeroMap, "venus_coal.png", "venus");
        saveMapImage(zeroMap, "venus_oil.png", "venus");
        saveMapImage(zeroMap, "venus_gas.png", "venus");
        saveMapImage(zeroMap, "venus_helium3.png", "venus");
        saveMapImage(zeroMap, "venus_aquifers.png", "venus");
    }

    /**
     * Generates the full physical, climatic, and mineral 2048x1024 cartographic raster suite
     * for Mercury using NASA MESSENGER DEM relief, polar shadowed ice traps, and 3:2 spin-orbit insolation.
     */
    static void generateMercuryMaps() {
        logger.info("Generating Mercury Cartography in Grayscale [0..255] on Black Background...");

        File messengerFile = new File("data/maps/nasa_pds/mercury_messenger_dem_downsampled.png");
        BufferedImage srcMessenger = null;
        if (messengerFile.exists()) {
            try {
                srcMessenger = ImageIO.read(messengerFile);
            } catch (Exception ignored) {
            }
        }
        BufferedImage imgElev = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double norm = 0.5;
                if (srcMessenger != null) {
                    int sx = (int) ((x / (double) WIDTH) * srcMessenger.getWidth());
                    int sy = (int) ((y / (double) HEIGHT) * srcMessenger.getHeight());
                    sx = Math.clamp(sx, 0, srcMessenger.getWidth() - 1);
                    sy = Math.clamp(sy, 0, srcMessenger.getHeight() - 1);
                    Color c = new Color(srcMessenger.getRGB(sx, sy));
                    norm = (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114) / 255.0;
                }
                int gray = (int) Math.clamp(norm * 255.0, 0.0, 255.0);
                imgElev.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        saveMapImage(imgElev, "mercury_elevation.png", "mercury");

        // Temperature in Grayscale [0..255]
        BufferedImage imgTemp = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x / (double) WIDTH) * 360.0;
                double tempC = 100.0 + 330.0 * Math.max(0.0, Math.cos(Math.toRadians(lon)) * Math.cos(Math.toRadians(lat)));
                if (Math.abs(lat) > 85.0) tempC = -180.0;
                int gray = (int) (Math.clamp((tempC + 180.0) / 610.0, 0.0, 1.0) * 255.0);
                imgTemp.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        saveMapImage(imgTemp, "mercury_temperature.png", "mercury");

        // Biomes
        BufferedImage imgBiomes = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                int biomeColor;
                if (Math.abs(lat) > 86.0) {
                    biomeColor = BIOME_SNOW;
                } else {
                    int elevRgb = imgElev.getRGB(x, y);
                    int r = (elevRgb >> 16) & 0xFF;
                    int g = (elevRgb >> 8) & 0xFF;
                    int b = elevRgb & 0xFF;
                    double brightness = (r * 0.299 + g * 0.587 + b * 0.114) / 255.0;
                    biomeColor = (brightness > 0.70) ? BIOME_MOUNTAINS : (brightness > 0.50 ? BIOME_HILLS : BIOME_PLAINS);
                }
                imgBiomes.setRGB(x, y, biomeColor);
            }
        }
        saveMapImage(imgBiomes, "mercury_biomes.png", "mercury");

        // Polar Shadowed Water Ice
        BufferedImage imgIce = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        double[][] mercIce = {{0.0, 89.0, 20, 2.5}, {0.0, -89.0, 20, 2.5}};
        List<double[]> mIceList = new ArrayList<>();
        for (double[] s : mercIce) {
            mIceList.add(s);
        }
        rasterizeAlphaDensity(imgIce, mIceList, 15.0);
        saveMapImage(imgIce, "mercury_aquifers.png", "mercury");

        // Helium-3, Iron, Precious Metals, Uranium, Geothermal
        BufferedImage imgHe3 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgHe3, mIceList, 25.0);
        saveMapImage(imgHe3, "mercury_helium3.png", "mercury");

        BufferedImage imgIron = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeTieredDensity(imgIron, mIceList, 30.0);
        saveMapImage(imgIron, "mercury_iron_copper.png", "mercury");

        BufferedImage imgPrecious = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgPrecious, mIceList, 25.0);
        saveMapImage(imgPrecious, "mercury_precious_metals.png", "mercury");

        BufferedImage imgUranium = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgUranium, mIceList, 25.0);
        saveMapImage(imgUranium, "mercury_uranium.png", "mercury");

        BufferedImage imgGeo = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgGeo, mIceList, 25.0);
        saveMapImage(imgGeo, "mercury_geothermal.png", "mercury");

        // Seasonality (3:2 spin-orbit resonance diurnal variance)
        BufferedImage imgSeason = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x / (double) WIDTH) * 360.0;
                double var = Math.abs(Math.cos(Math.toRadians(lon * 2.0))) * Math.cos(Math.toRadians(lat));
                int gray = (int) (var * 255.0);
                imgSeason.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        saveMapImage(imgSeason, "mercury_seasonality.png", "mercury");

        // Zeros
        BufferedImage zeroMap = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        saveMapImage(zeroMap, "mercury_coal.png", "mercury");
        saveMapImage(zeroMap, "mercury_oil.png", "mercury");
        saveMapImage(zeroMap, "mercury_gas.png", "mercury");
        saveMapImage(zeroMap, "mercury_precipitation.png", "mercury");
    }
}
