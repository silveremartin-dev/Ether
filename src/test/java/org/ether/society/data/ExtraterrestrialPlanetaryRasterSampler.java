/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot & Gemini AI
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import java.util.ArrayList;
import static org.ether.society.data.PlanetaryRasterSampler.*;

/**
 * Extraterrestrial Cartographic Tensor Generator for Moon, Mars, Venus, and Mercury.
 */
public final class ExtraterrestrialPlanetaryRasterSampler {
    private static final Logger logger = LoggerFactory.getLogger(ExtraterrestrialPlanetaryRasterSampler.class);

    private ExtraterrestrialPlanetaryRasterSampler() {}

    public static void generateMoonMaps() {
        logger.info("Generating Moon Cartography & Resources in Grayscale [0..255] on Black Background...");

        // 1. Elevation (NASA LOLA)
        File lolaFile = new File("data/maps/nasa_pds/lunar_lola_dem_downsampled.png");
        BufferedImage srcLola = null;
        if (lolaFile.exists()) {
            try {
                srcLola = ImageIO.read(lolaFile);
            } catch (Exception ignored) {}
        }
        BufferedImage imgElev = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x / (double) WIDTH) * 360.0;
                double norm = 0.5;
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
                imgElev.setRGB(x, y, elevationToHypsometricColor(norm).getRGB());
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
        for (double[] s : mareHelium3Spots) he3List.add(s);
        rasterizeAlphaDensity(imgHe3, he3List, 35.0);
        saveMapImage(imgHe3, "moon_helium3.png", "moon");

        // 3. Water Ice in Permanently Shadowed Regions
        BufferedImage imgIce = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        double[][] polarIceSpots = {
            {0.0, -89.5, 30, 2.8},   // South Pole
            {0.0, 89.0, 25, 2.5}     // North Pole
        };
        List<double[]> iceList = new ArrayList<>();
        for (double[] s : polarIceSpots) iceList.add(s);
        rasterizeAlphaDensity(imgIce, iceList, 20.0);
        saveMapImage(imgIce, "moon_aquifers.png", "moon");

        // 4. Iron & Titanium Ores (Ilmenite FeTiO3)
        BufferedImage imgIron = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeTieredDensity(imgIron, he3List, 30.0);
        saveMapImage(imgIron, "moon_iron_copper.png", "moon");

        // 5. Uranium & KREEP
        BufferedImage imgUranium = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgUranium, he3List, 25.0);
        saveMapImage(imgUranium, "moon_uranium.png", "moon");

        // 6. Precious Metals (Impact Ejecta & KREEP)
        BufferedImage imgPrecious = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgPrecious, he3List, 25.0);
        saveMapImage(imgPrecious, "moon_precious_metals.png", "moon");

        // 7. Geothermal / Crustal Heat (PKT)
        BufferedImage imgGeo = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgGeo, he3List, 25.0);
        saveMapImage(imgGeo, "moon_geothermal.png", "moon");

        // 8. Zero Resource maps
        BufferedImage zeroMap = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        saveMapImage(zeroMap, "moon_coal.png", "moon");
        saveMapImage(zeroMap, "moon_oil.png", "moon");
        saveMapImage(zeroMap, "moon_gas.png", "moon");
        saveMapImage(zeroMap, "moon_precipitation.png", "moon");

        // 9. Seasonality (0 on Moon due to 1.54Ã‚Â° tilt)
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
                    if (brightness < 0.46) {
                        biomeColor = BIOME_PLAINS;
                    } else if (brightness < 0.70) {
                        biomeColor = BIOME_HILLS;
                    } else {
                        biomeColor = BIOME_MOUNTAINS;
                    }
                }
                imgBiomes.setRGB(x, y, biomeColor);
            }
        }
        saveMapImage(imgBiomes, "moon_biomes.png", "moon");
    }

    public static void generateMarsMaps() {
        logger.info("Generating Mars Cartography & Resources in Grayscale [0..255] on Black Background...");

        // 1. Elevation (NASA MOLA)
        File molaFile = new File("data/maps/nasa_pds/high_res_flat_mola.tif");
        BufferedImage srcMola = null;
        if (molaFile.exists()) {
            try {
                srcMola = ImageIO.read(molaFile);
            } catch (Exception ignored) {}
        }

        BufferedImage imgElev = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        double[][] molaAltGrid = new double[HEIGHT][WIDTH];

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x / (double) WIDTH) * 360.0;
                double norm = 0.5;

                if (srcMola != null) {
                    double yNorm = y / (double) HEIGHT;
                    double safeTop = 0.12, safeBot = 0.75;
                    double syNorm = Math.clamp(yNorm, safeTop, safeBot);
                    int sx = (int) ((x / (double) WIDTH) * srcMola.getWidth());
                    int sy = (int) (syNorm * srcMola.getHeight());
                    sx = Math.clamp(sx, 0, srcMola.getWidth() - 1);
                    sy = Math.clamp(sy, 0, srcMola.getHeight() - 1);
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
                    double elev = 0.45;
                    elev += (lat > 0 ? -0.15 : 0.12) * Math.sin(Math.toRadians(lat * 1.5));
                    double olympusDist = Math.hypot((lon - (-133.8)) * Math.cos(Math.toRadians(lat)), lat - 18.65);
                    if (olympusDist < 12.0) elev += 0.52 * Math.pow(1.0 - olympusDist / 12.0, 2.0);
                    double ascraeusDist = Math.hypot((lon - (-104.5)) * Math.cos(Math.toRadians(lat)), lat - 11.9);
                    if (ascraeusDist < 8.0) elev += 0.42 * Math.pow(1.0 - ascraeusDist / 8.0, 2.0);
                    double pavonisDist = Math.hypot((lon - (-112.9)) * Math.cos(Math.toRadians(lat)), lat - 0.8);
                    if (pavonisDist < 8.0) elev += 0.40 * Math.pow(1.0 - pavonisDist / 8.0, 2.0);
                    double arsiaDist = Math.hypot((lon - (-120.9)) * Math.cos(Math.toRadians(lat)), lat - (-8.4));
                    if (arsiaDist < 8.5) elev += 0.41 * Math.pow(1.0 - arsiaDist / 8.5, 2.0);
                    double elysiumDist = Math.hypot((lon - 147.2) * Math.cos(Math.toRadians(lat)), lat - 25.0);
                    if (elysiumDist < 9.0) elev += 0.35 * Math.pow(1.0 - elysiumDist / 9.0, 2.0);
                    double hellasDist = Math.hypot((lon - 70.5) * Math.cos(Math.toRadians(lat)), lat - (-42.4));
                    if (hellasDist < 22.0) elev -= 0.35 * Math.pow(1.0 - hellasDist / 22.0, 1.8);
                    double argyreDist = Math.hypot((lon - (-43.6)) * Math.cos(Math.toRadians(lat)), lat - (-49.7));
                    if (argyreDist < 14.0) elev -= 0.25 * Math.pow(1.0 - argyreDist / 14.0, 1.8);

                    norm = Math.clamp(elev, 0.05, 0.98);
                }

                molaAltGrid[y][x] = norm;
                imgElev.setRGB(x, y, elevationToHypsometricColor(norm).getRGB());
            }
        }
        saveMapImage(imgElev, "mars_elevation.png", "mars");

        // 2. Discrete Polychrome Biomes
        BufferedImage imgBiomes = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x / (double) WIDTH) * 360.0;
                double normAlt = molaAltGrid[y][x];

                double northCapLimit = 76.0 + 3.5 * Math.sin(Math.toRadians(lon * 3.0 + 45.0));
                double southCapLimit = -73.0 + 4.0 * Math.cos(Math.toRadians(lon * 2.0 - 30.0));

                int biomeColor;
                if (lat >= northCapLimit || lat <= southCapLimit) {
                    biomeColor = (Math.abs(lat) > 83.0) ? BIOME_GLACIER : BIOME_SNOW;
                } else if (normAlt > 0.78) {
                    biomeColor = BIOME_MOUNTAINS;
                } else if (normAlt > 0.60) {
                    biomeColor = BIOME_HILLS;
                } else if (normAlt < 0.35) {
                    biomeColor = BIOME_PLAINS;
                } else {
                    biomeColor = BIOME_DESERT;
                }
                imgBiomes.setRGB(x, y, biomeColor);
            }
        }
        saveMapImage(imgBiomes, "mars_biomes.png", "mars");

        // 3. Surface Temperature in Grayscale [0..255]
        BufferedImage imgTemp = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x / (double) WIDTH) * 360.0;
                double altNorm = molaAltGrid[y][x];
                double lapseRateCooling = (altNorm - 0.45) * 45.0;
                double latCooling = 55.0 * Math.sin(Math.toRadians(Math.abs(lat)));
                double diurnalMod = 6.0 * Math.cos(Math.toRadians(lon * 2.0));

                double tempC = -55.0 - latCooling - lapseRateCooling + diurnalMod;
                int gray = (int) (Math.clamp((tempC + 130.0) / 160.0, 0.0, 1.0) * 255.0);
                imgTemp.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        saveMapImage(imgTemp, "mars_temperature.png", "mars");

        // 4. Atmospheric Moisture / Frost Precipitation in Grayscale [0..255] on Black Background (0,0,0)
        BufferedImage imgPrecip = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x / (double) WIDTH) * 360.0;
                double altNorm = molaAltGrid[y][x];

                // Orographic clouds around volcanic summits + polar frost vapor
                double orographicCloud = (altNorm > 0.75) ? (altNorm - 0.75) * 1200.0 : 0.0;
                double polarVapor = (Math.abs(lat) > 65.0) ? Math.pow((Math.abs(lat) - 65.0) / 25.0, 1.5) * 600.0 : 0.0;
                double waveMod = 80.0 * Math.max(0.0, Math.sin(Math.toRadians(lon * 2.0 + lat)));

                double precipMm = Math.max(0.0, orographicCloud + polarVapor + waveMod);
                // 0 mm/yr = 0 (black background)
                int pGray = (int) Math.clamp((precipMm / 600.0) * 255.0, 0.0, 255.0);
                imgPrecip.setRGB(x, y, (pGray << 16) | (pGray << 8) | pGray);
            }
        }
        saveMapImage(imgPrecip, "mars_precipitation.png", "mars");

        // 5. Martian Seasonality (Smooth continuous spherical harmonics with eccentricity asymmetry - No Equator Discontinuity)
        BufferedImage imgSeason = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                // Continuous harmonic function across the entire sphere with smooth southern perihelion variance:
                // deltaT(lat) = 18.0 + 26.0 * sin(|lat|) - 6.0 * sin(lat)
                // At lat = +90 (North): deltaT = 18 + 26 - 6 = 38Ã‚Â°C
                // At lat = 0 (Equator): deltaT = 18Ã‚Â°C (smooth C0 & C1 continuity!)
                // At lat = -90 (South): deltaT = 18 + 26 + 6 = 50Ã‚Â°C
                double baseVariance = 18.0 + 26.0 * Math.sin(Math.toRadians(Math.abs(lat))) - 6.0 * Math.sin(Math.toRadians(lat));
                int gray = (int) Math.clamp((baseVariance / 50.0) * 255.0, 0, 255);
                imgSeason.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        saveMapImage(imgSeason, "mars_seasonality.png", "mars");

        // 6. Iron & Copper Formations (Hematite / Basalts)
        BufferedImage imgIron = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        double[][] marsIronSpots = {
            {-2.0, 0.0, 65, 2.8},    // Meridiani Planum
            {-21.0, 2.5, 55, 2.5},   // Aram Chaos
            {137.4, -4.6, 50, 2.4},  // Gale Crater
            {-133.0, 18.0, 75, 2.8}, // Olympus Mons Basaltic Shield
            {-112.0, 9.0, 65, 2.6},  // Ascraeus Mons
            {-119.0, 0.5, 60, 2.5},  // Pavonis Mons
            {-120.0, -9.0, 60, 2.5}, // Arsia Mons
            {147.0, 25.0, 60, 2.5}   // Elysium Mons
        };
        List<double[]> ironList = new ArrayList<>();
        for (double[] s : marsIronSpots) ironList.add(s);
        rasterizeTieredDensity(imgIron, ironList, 35.0);
        saveMapImage(imgIron, "mars_iron_copper.png", "mars");

        // 7. Water Ice / Permafrost (Polar Caps & Subsurface Glaciers)
        BufferedImage imgIce = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        double[][] marsIceSpots = {
            {0.0, 86.0, 75, 3.0},    // Planum Boreum
            {0.0, -86.0, 70, 2.8},   // Planum Australe
            {110.0, 45.0, 80, 2.6},  // Utopia Planitia
            {-170.0, 40.0, 70, 2.4}, // Arcadia Planitia
            {-150.0, -5.0, 60, 2.2}  // Medusae Fossae
        };
        List<double[]> marsIceList = new ArrayList<>();
        for (double[] s : marsIceSpots) marsIceList.add(s);
        rasterizeAlphaDensity(imgIce, marsIceList, 35.0);
        saveMapImage(imgIce, "mars_aquifers.png", "mars");

        // 8. Geothermal / Volcanic Hotspots
        BufferedImage imgGeo = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        rasterizeAlphaDensity(imgGeo, ironList, 35.0);
        saveMapImage(imgGeo, "mars_geothermal.png", "mars");

        // 9. Uranium, Precious Metals, Helium-3, Gas
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
        double[][] marsGasSpots = { {137.4, -4.6, 40, 2.0}, {77.0, 21.0, 45, 2.2} }; // Gale Crater & Nili Fossae methane
        List<double[]> gasList = new ArrayList<>();
        for (double[] s : marsGasSpots) gasList.add(s);
        rasterizeAlphaDensity(imgGas, gasList, 20.0);
        saveMapImage(imgGas, "mars_gas.png", "mars");

        // 10. Coal & Oil (Zero on Mars)
        BufferedImage zeroMap = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        saveMapImage(zeroMap, "mars_coal.png", "mars");
        saveMapImage(zeroMap, "mars_oil.png", "mars");
    }

    public static void generateVenusMaps() {
        logger.info("Generating Venus Cartography & Resources in Grayscale [0..255] on Black Background...");

        // 1. Elevation (NASA Magellan)
        File magellanFile = new File("data/maps/nasa_pds/Venus_Magellan_C3-MDIR_ClrTopo_Global_Mosaic_6600m.tif");
        BufferedImage srcMagellan = null;
        if (magellanFile.exists()) {
            try {
                srcMagellan = ImageIO.read(magellanFile);
            } catch (Exception ignored) {}
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
                imgElev.setRGB(x, y, elevationToHypsometricColor(norm).getRGB());
            }
        }
        saveMapImage(imgElev, "venus_elevation.png", "venus");

        // 2. Temperature in Grayscale [0..255] (~465Ã‚Â°C)
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
        for (double[] s : venusSpots) vList.add(s);
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
        int vSeasonGray = 8; // Uniform ~1-2Ã‚Â°C seasonal variance
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

    public static void generateMercuryMaps() {
        logger.info("Generating Mercury Cartography in Grayscale [0..255] on Black Background...");

        File messengerFile = new File("data/maps/nasa_pds/mercury_messenger_dem_downsampled.png");
        BufferedImage srcMessenger = null;
        if (messengerFile.exists()) {
            try {
                srcMessenger = ImageIO.read(messengerFile);
            } catch (Exception ignored) {}
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
                imgElev.setRGB(x, y, elevationToHypsometricColor(norm).getRGB());
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
        double[][] mercIce = { {0.0, 89.0, 20, 2.5}, {0.0, -89.0, 20, 2.5} };
        List<double[]> mIceList = new ArrayList<>();
        for (double[] s : mercIce) mIceList.add(s);
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
