/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.ether.society.data.PlanetaryRasterSampler.BIOME_BEACH;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_DEEP_OCEAN;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_DESERT;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_FOREST;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_GLACIER;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_HILLS;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_JUNGLE;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_MOUNTAINS;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_OCEAN;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_PLAINS;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_SNOW;
import static org.ether.society.data.PlanetaryRasterSampler.BIOME_TUNDRA;
import static org.ether.society.data.PlanetaryRasterSampler.HEIGHT;
import static org.ether.society.data.PlanetaryRasterSampler.WIDTH;
import static org.ether.society.data.PlanetaryRasterSampler.saveMapImage;

/**
 * Climate and biome raster generation helper for Earth epochs.
 *
 * <p>Processes elevation from NOAA ETOPO, temperatures and precipitation from PaleoClim/CHELSA,
 * seasonality tensors, and MODIS-derived paleobiome classifications across multi-millennial epochs.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
final class EarthClimateEpochRasterSampler {

    private static final Logger logger = LoggerFactory.getLogger(EarthClimateEpochRasterSampler.class);

    private EarthClimateEpochRasterSampler() {
        // Utility class
    }

    static float[][] getEtopoGrid() {
        return EtopoGeoTiffReader.loadEtopoGrid(WIDTH, HEIGHT);
    }

    static File getPaleoClimFile(long epoch, String bioName) {
        String base;
        if (epoch <= -100000L) {
            base = "data/maps/paleoclim/LIG_v1_2_5m/" + bioName;
        } else if (epoch <= -20000L) {
            base = "data/maps/paleoclim/chelsa_LGM_v1_2B_r2_5m/2_5min/" + bioName;
        } else if (epoch <= -10000L) {
            base = "data/maps/paleoclim/EH_v1_2_5m/" + bioName;
        } else if (epoch <= -6000L) {
            base = "data/maps/paleoclim/MH_v1_2_5m/" + bioName;
        } else if (epoch <= -3000L) {
            base = "data/maps/paleoclim/LH_v1_2_5m/" + bioName;
        } else {
            base = "data/maps/paleoclim/CHELSA_cur_V1_2B_r2_5m/2_5min/" + bioName;
        }
        File f = new File(base);
        if (f.exists()) return f;
        return new File("data/maps/paleoclim/CHELSA_cur_V1_2B_r2_5m/2_5min/" + bioName);
    }

    static float sampleGeoTiff(BufferedImage img, double lat, double lon) {
        if (img == null) return -99999f;
        int w = img.getWidth();
        int h = img.getHeight();

        double latMax = 90.0;
        double latMin = -90.0;
        if (h == 4176) {
            latMax = 84.0;
            latMin = -90.0;
        } else if (h == 4285) {
            latMax = 88.54152885;
            latMin = -90.0;
        } else if (h == 3565 || (w == 8640 && h < 4000)) {
            latMax = 88.54152885;
            latMin = -60.0;
        }

        if (lat > latMax || lat < latMin) return -99999f;

        int sx = (int) Math.clamp(((lon + 180.0) / 360.0) * (w - 1), 0, w - 1);
        int sy = (int) Math.clamp(((latMax - lat) / (latMax - latMin)) * (h - 1), 0, h - 1);
        return img.getRaster().getSampleFloat(sx, sy, 0);
    }

    static void generateEarthMapsForEpoch(long epoch) {
        logger.info("Generating Earth Elevation, Biomes, and Empirical PaleoClim Climate Tensors for epoch {}...", epoch);

        // 1. Elevation directly from NOAA ETOPO 2022 Global Relief Model
        BufferedImage imgElev = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        float[][] etopo = getEtopoGrid();
        double[][] gridNorm = new double[HEIGHT][WIDTH];

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                float altM = etopo[y][x];
                double norm = (altM < 0)
                        ? 0.478 * (1.0 + Math.max(-11000.0f, altM) / 11000.0)
                        : 0.478 + 0.522 * Math.min(1.0, altM / 8848.0);
                norm = Math.clamp(norm, 0.0, 1.0);
                gridNorm[y][x] = norm;
                int gray = (int) Math.clamp(norm * 255.0, 0.0, 255.0);
                imgElev.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        saveMapImage(imgElev, "earth_elevation.png", "earth", epoch);

        // 2. Temperature (-50°C to +50°C -> [0..255]) sourced from PaleoClim/CHELSA
        BufferedImage imgTemp = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        File tempFile = getPaleoClimFile(epoch, "bio_1.tif");
        BufferedImage srcTemp = null;
        if (tempFile.exists()) {
            try { srcTemp = ImageIO.read(tempFile); } catch (Exception e) { logger.warn("Error reading temp GeoTIFF: {}", e.getMessage()); }
        }
        double[][] gridTemp = new double[HEIGHT][WIDTH];

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x / (double) WIDTH) * 360.0;
                double norm = gridNorm[y][x];
                boolean isLand = (norm >= 0.478);
                double absLat = Math.abs(lat);
                double oceanTemp = -1.8 + 30.3 * Math.pow(Math.cos(Math.toRadians(Math.min(90.0, absLat))), 1.4)
                        - Math.max(0.0, (absLat - 68.0) / 22.0) * 22.0;

                double tempC;
                if (isLand) {
                    float sample = sampleGeoTiff(srcTemp, lat, lon);
                    if (sample > -1000 && sample < 1000) {
                        double rawTemp = sample / 10.0;
                        if (lat < -55.0) {
                            double altM = Math.max(0.0, (norm - 0.478) / 0.522) * 4000.0;
                            double antarcticLandTarget = -15.0 - (absLat - 60.0) * 1.0 - altM * 0.007;
                            double t = Math.clamp((-55.0 - lat) / 5.0, 0.0, 1.0);
                            double blend = t * t * (3.0 - 2.0 * t);
                            tempC = (1.0 - blend) * rawTemp + blend * antarcticLandTarget;
                        } else if (lat > 80.0) {
                            double arcticTarget = -24.0 - Math.max(0.0, lat - 80.0) * 0.8;
                            double t = Math.clamp((lat - 80.0) / 4.0, 0.0, 1.0);
                            double blend = t * t * (3.0 - 2.0 * t);
                            tempC = (1.0 - blend) * rawTemp + blend * arcticTarget;
                        } else {
                            tempC = rawTemp;
                        }
                    } else {
                        double altM = Math.max(0.0, (norm - 0.478) / 0.522) * 4000.0;
                        tempC = -15.0 - (absLat - 60.0) * 1.0 - altM * 0.007;
                    }
                } else {
                    tempC = oceanTemp;
                }
                gridTemp[y][x] = tempC;
                int tGray = (int) (Math.clamp((tempC + 50.0) / 100.0, 0.0, 1.0) * 255.0);
                imgTemp.setRGB(x, y, (tGray << 16) | (tGray << 8) | tGray);
            }
        }
        saveMapImage(imgTemp, "earth_temperature.png", "earth", epoch);

        // 3. Precipitation (0 to 3000 mm/yr) sourced from PaleoClim/CHELSA
        BufferedImage imgPrecip = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        File precipFile = getPaleoClimFile(epoch, "bio_12.tif");
        BufferedImage srcPrecip = null;
        if (precipFile.exists()) {
            try { srcPrecip = ImageIO.read(precipFile); } catch (Exception e) { logger.warn("Error reading precip GeoTIFF: {}", e.getMessage()); }
        }
        double[][] gridPrecip = new double[HEIGHT][WIDTH];

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x / (double) WIDTH) * 360.0;
                double norm = gridNorm[y][x];
                boolean isLand = (norm >= 0.478);
                double absLat = Math.abs(lat);
                double itcz = Math.exp(-Math.pow(lat / 12.0, 2)) * 1400.0;
                double stormTracks = Math.exp(-Math.pow((absLat - 50.0) / 14.0, 2)) * 900.0;
                double polarDecay = Math.max(0.0, Math.cos(Math.toRadians(Math.min(90.0, absLat))));
                double oceanPrecip = 350.0 + itcz + stormTracks * polarDecay;

                double pv;
                if (isLand) {
                    float sample = sampleGeoTiff(srcPrecip, lat, lon);
                    if (sample >= 0 && sample <= 30000) {
                        double rawPrecip = sample;
                        if (lat < -55.0) {
                            double altM = Math.max(0.0, (norm - 0.478) / 0.522) * 4000.0;
                            double antarcticLandPrecip = 140.0 * Math.exp(-(absLat - 60.0) / 14.0) * Math.exp(-altM / 2000.0) + 25.0;
                            double t = Math.clamp((-55.0 - lat) / 5.0, 0.0, 1.0);
                            double blend = t * t * (3.0 - 2.0 * t);
                            pv = (1.0 - blend) * rawPrecip + blend * antarcticLandPrecip;
                        } else if (lat > 80.0) {
                            double t = Math.clamp((lat - 80.0) / 4.0, 0.0, 1.0);
                            double blend = t * t * (3.0 - 2.0 * t);
                            pv = (1.0 - blend) * rawPrecip + blend * 120.0;
                        } else {
                            pv = rawPrecip;
                        }
                    } else {
                        double altM = Math.max(0.0, (norm - 0.478) / 0.522) * 4000.0;
                        pv = 140.0 * Math.exp(-(absLat - 60.0) / 14.0) * Math.exp(-altM / 2000.0) + 25.0;
                    }
                } else {
                    pv = oceanPrecip;
                }
                gridPrecip[y][x] = pv;
                int pGray = (int) (Math.clamp(pv / 3000.0, 0.0, 1.0) * 255.0);
                imgPrecip.setRGB(x, y, (pGray << 16) | (pGray << 8) | pGray);
            }
        }
        saveMapImage(imgPrecip, "earth_precipitation.png", "earth", epoch);

        // 4. Seasonality sourced from PaleoClim/CHELSA
        BufferedImage imgSeason = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        File seasonFile = getPaleoClimFile(epoch, "bio_4.tif");
        BufferedImage srcSeason = null;
        if (seasonFile.exists()) {
            try { srcSeason = ImageIO.read(seasonFile); } catch (Exception e) { logger.warn("Error reading season GeoTIFF: {}", e.getMessage()); }
        }
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x / (double) WIDTH) * 360.0;
                double norm = gridNorm[y][x];
                boolean isLand = (norm >= 0.478);
                double absLat = Math.abs(lat);
                double oceanSeasonNorm = Math.clamp((2.5 + (absLat / 90.0) * 16.0) / 25.0, 0.0, 1.0);

                double sNorm;
                if (isLand) {
                    float sv = sampleGeoTiff(srcSeason, lat, lon);
                    if (sv >= 0 && sv <= 50000) {
                        double rawNorm = Math.clamp((sv / 100.0) / 25.0, 0.0, 1.0);
                        if (lat < -55.0) {
                            double antarcticLandSeason = Math.clamp((16.0 + (absLat - 60.0) * 0.35) / 25.0, 0.0, 1.0);
                            double t = Math.clamp((-55.0 - lat) / 5.0, 0.0, 1.0);
                            double blend = t * t * (3.0 - 2.0 * t);
                            sNorm = (1.0 - blend) * rawNorm + blend * antarcticLandSeason;
                        } else if (lat > 80.0) {
                            double arcticLandSeason = Math.clamp((18.0 + Math.max(0.0, lat - 80.0) * 0.4) / 25.0, 0.0, 1.0);
                            double t = Math.clamp((lat - 80.0) / 4.0, 0.0, 1.0);
                            double blend = t * t * (3.0 - 2.0 * t);
                            sNorm = (1.0 - blend) * rawNorm + blend * arcticLandSeason;
                        } else {
                            sNorm = rawNorm;
                        }
                    } else {
                        sNorm = Math.clamp((16.0 + (absLat - 60.0) * 0.35) / 25.0, 0.0, 1.0);
                    }
                } else {
                    sNorm = oceanSeasonNorm;
                }
                int sGray = (int) (Math.clamp(sNorm, 0.0, 1.0) * 255.0);
                imgSeason.setRGB(x, y, (sGray << 16) | (sGray << 8) | sGray);
            }
        }
        saveMapImage(imgSeason, "earth_seasonality.png", "earth", epoch);

        // 5. Empirical Biomes Derived from Reference MODIS + Paleo-Environmental Adaptation
        generateBiomeMap(epoch, imgElev, gridNorm, gridTemp, gridPrecip);
    }

    private static void generateBiomeMap(long epoch, BufferedImage imgElev, double[][] gridNorm, double[][] gridTemp, double[][] gridPrecip) {
        BufferedImage imgBiomes = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage refBiome = null;
        File refFile = new File("data/maps/reference_earth_biomes.png");
        if (!refFile.exists()) refFile = new File("data/maps/scratch_db000ab_earth_biomes.png");
        if (refFile.exists()) {
            try { refBiome = ImageIO.read(refFile); } catch (Exception e) { logger.warn("Failed reading reference biomes: {}", e.getMessage()); }
        }

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / (double) HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / (double) WIDTH * 360.0;
                double norm = gridNorm[y][x];
                double tempC = gridTemp[y][x];
                double precipMm = gridPrecip[y][x];
                int bColor;

                if (norm < 0.478) {
                    bColor = (norm < 0.45) ? BIOME_DEEP_OCEAN : BIOME_OCEAN;
                } else if (lat < -60.0) {
                    bColor = (tempC < -20.0 || norm > 0.65) ? BIOME_GLACIER : BIOME_SNOW;
                } else if (lat > 75.0 && lon > -70.0 && lon < -15.0) {
                    bColor = BIOME_GLACIER;
                } else if (refBiome != null) {
                    int rx = Math.clamp((int) (((lon + 180.0) / 360.0) * (refBiome.getWidth() - 1)), 0, refBiome.getWidth() - 1);
                    int ry = Math.clamp((int) (((90.0 - lat) / 180.0) * (refBiome.getHeight() - 1)), 0, refBiome.getHeight() - 1);
                    int baseColor = refBiome.getRGB(rx, ry) & 0xFFFFFF;

                    if (baseColor == BIOME_DEEP_OCEAN || baseColor == BIOME_OCEAN || baseColor == 0x000000) {
                        bColor = classifyLandBiome(norm, tempC, precipMm);
                    } else if (epoch <= -70000L) {
                        if (lat >= 14.0 && lat <= 26.0 && lon >= -15.0 && lon <= 55.0 && (baseColor == BIOME_DESERT || baseColor == 0xFFD232)) {
                            bColor = BIOME_PLAINS;
                        } else if (lat >= 48.0 && lat <= 62.0 && lon >= -10.0 && lon <= 120.0 && baseColor == BIOME_TUNDRA) {
                            bColor = (precipMm > 300.0) ? BIOME_PLAINS : BIOME_TUNDRA;
                        } else if (lat > 66.0 && (lon > 10.0 && lon < 40.0)) {
                            bColor = BIOME_GLACIER;
                        } else {
                            bColor = baseColor;
                        }
                    } else {
                        bColor = baseColor;
                    }
                } else {
                    bColor = classifyLandBiome(norm, tempC, precipMm);
                }
                imgBiomes.setRGB(x, y, bColor);
            }
        }
        saveMapImage(imgBiomes, "earth_biomes.png", "earth", epoch);
        if (epoch == 2026L) {
            try {
                ImageIO.write(imgElev, "png", new File("data/maps/reference_earth_elevation.png"));
                ImageIO.write(imgBiomes, "png", new File("data/maps/reference_earth_biomes.png"));
            } catch (Exception ignored) { /* harmless test raster write */ }
        }
    }

    private static int classifyLandBiome(double norm, double tempC, double precipMm) {
        if (norm < 0.485 && tempC > 5.0) return BIOME_BEACH;
        if (norm > 0.80 || tempC < -12.0) return BIOME_GLACIER;
        if (norm > 0.72 || tempC < -3.0) return BIOME_SNOW;
        if (tempC < 4.0) return (precipMm < 400) ? BIOME_TUNDRA : BIOME_FOREST;
        if (norm > 0.65) return BIOME_MOUNTAINS;
        if (norm > 0.55) return BIOME_HILLS;
        if (precipMm < 180) return BIOME_DESERT;
        if (tempC >= 20.0 && precipMm >= 1800) return BIOME_JUNGLE;
        if (precipMm >= 700) return BIOME_FOREST;
        return BIOME_PLAINS;
    }
}
