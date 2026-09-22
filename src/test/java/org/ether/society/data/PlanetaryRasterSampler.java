/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PlanetaryRasterSampler {
    private static final Logger logger = LoggerFactory.getLogger(PlanetaryRasterSampler.class);

    private static final int WIDTH = 2048;
    private static final int HEIGHT = 1024;

    public static List<String> fastParseCsv(String line) {
        List<String> list = new ArrayList<>(30);
        StringBuilder sb = new StringBuilder(64);
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString().trim());
        return list;
    }

    private static List<double[]> extractMrdsDeposits(String... keywords) {
        List<double[]> list = new ArrayList<>();
        Path zipPath = Paths.get("data", "maps", "usgs_mrds", "mrds-csv.zip");
        if (!Files.exists(zipPath)) return list;

        try (ZipFile zip = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry entry = zip.getEntry("mrds.csv");
            if (entry == null) return list;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8), 65536)) {
                String line = br.readLine(); // header
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    List<String> parts = fastParseCsv(line);
                    if (parts.size() > 17) {
                        try {
                            double lat = Double.parseDouble(parts.get(5).replace("\"", "").trim());
                            double lon = Double.parseDouble(parts.get(6).replace("\"", "").trim());
                            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) continue;

                            String comms = (parts.get(11) + " " + parts.get(12) + " " + parts.get(13) + " " + parts.get(14)).toLowerCase();
                            boolean match = false;
                            for (String kw : keywords) {
                                if (comms.contains(kw.toLowerCase())) {
                                    match = true;
                                    break;
                                }
                            }

                            if (match) {
                                String prodSize = parts.get(17).toUpperCase();
                                double weight = 1.0;
                                double radius = 5.0;
                                if (prodSize.contains("L") || prodSize.contains("Y")) {
                                    weight = 2.5;
                                    radius = 12.0;
                                } else if (prodSize.contains("M")) {
                                    weight = 1.8;
                                    radius = 8.0;
                                } else if (prodSize.contains("S")) {
                                    weight = 1.2;
                                    radius = 6.0;
                                }
                                list.add(new double[]{lon, lat, radius, weight});
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error reading MRDS CSV: {}", e.getMessage());
        }
        return list;
    }

    public static class GeologicalBasin {
        public final double centerLon;
        public final double centerLat;
        public final double majorDeg;
        public final double minorDeg;
        public final double strikeDeg;
        public final double intensity;
        public final String name;

        public GeologicalBasin(double centerLon, double centerLat, double majorDeg, double minorDeg, double strikeDeg, double intensity, String name) {
            this.centerLon = centerLon;
            this.centerLat = centerLat;
            this.majorDeg = majorDeg;
            this.minorDeg = minorDeg;
            this.strikeDeg = strikeDeg;
            this.intensity = intensity;
            this.name = name;
        }
    }

    /**
     * Rasterizes oriented basins and discrete point spots into a normalized 8-bit grayscale tensor [0..255]
     * with an absolute black background (value 0).
     */
    private static void rasterizeBasinsAndSpots(BufferedImage img, List<GeologicalBasin> basins, List<double[]> discreteSpots, double spotDefaultRadius) {
        int w = img.getWidth();
        int h = img.getHeight();
        float[][] grid = new float[h][w];

        // 1. Accumulate Oriented Sedimentary Basins
        if (basins != null) {
            for (GeologicalBasin basin : basins) {
                double cLon = basin.centerLon;
                double cLat = basin.centerLat;
                double a = basin.majorDeg;
                double b = basin.minorDeg;
                double strikeRad = Math.toRadians(basin.strikeDeg);
                double cosTheta = Math.cos(strikeRad);
                double sinTheta = Math.sin(strikeRad);
                double intensity = basin.intensity;

                double cosLat = Math.max(0.15, Math.cos(Math.toRadians(cLat)));
                double maxDeg = Math.max(a, b);
                double dLonDeg = maxDeg / cosLat;
                double dLatDeg = maxDeg;

                int cx = (int) Math.round(((cLon + 180.0) / 360.0) * (w - 1));
                int cy = (int) Math.round(((90.0 - cLat) / 180.0) * (h - 1));
                int rx = (int) Math.ceil((dLonDeg / 360.0) * (w - 1));
                int ry = (int) Math.ceil((dLatDeg / 180.0) * (h - 1));

                int minY = Math.max(0, cy - ry);
                int maxY = Math.min(h - 1, cy + ry);

                for (int py = minY; py <= maxY; py++) {
                    double lat = 90.0 - (py / (double) (h - 1)) * 180.0;
                    double dyDeg = lat - cLat;

                    for (int px = cx - rx; px <= cx + rx; px++) {
                        int wrapX = (px % w + w) % w;
                        double lon = -180.0 + (wrapX / (double) (w - 1)) * 360.0;
                        double dLon = lon - cLon;
                        if (dLon > 180.0) dLon -= 360.0;
                        else if (dLon < -180.0) dLon += 360.0;

                        double dxDeg = dLon * cosLat;

                        // Rotate by -strikeRad
                        double u = dxDeg * cosTheta + dyDeg * sinTheta;
                        double v = -dxDeg * sinTheta + dyDeg * cosTheta;

                        double q = (u * u) / (a * a) + (v * v) / (b * b);
                        if (q <= 1.0) {
                            double d = Math.sqrt(q);
                            double falloff = Math.pow(1.0 - d, 1.4);
                            grid[py][wrapX] += (float) (falloff * intensity);
                        }
                    }
                }
            }
        }

        // 2. Accumulate Discrete Fields & MRDS Occurrences
        if (discreteSpots != null) {
            for (double[] spot : discreteSpots) {
                double lon = spot[0];
                double lat = spot[1];
                double radius = spot.length > 2 ? spot[2] : spotDefaultRadius;
                double intensity = spot.length > 3 ? spot[3] : 1.0;

                int cx = (int) Math.round(((lon + 180.0) / 360.0) * (w - 1));
                int cy = (int) Math.round(((90.0 - lat) / 180.0) * (h - 1));
                int r = (int) Math.ceil(radius);

                int minY = Math.max(0, cy - r);
                int maxY = Math.min(h - 1, cy + r);
                int minX = cx - r;
                int maxX = cx + r;

                for (int py = minY; py <= maxY; py++) {
                    double dy = py - cy;
                    for (int px = minX; px <= maxX; px++) {
                        int wrapX = (px % w + w) % w;
                        double dx = px - cx;
                        double d = Math.sqrt(dx * dx + dy * dy);
                        if (d <= radius) {
                            double norm = 1.0 - (d / radius);
                            grid[py][wrapX] += (float) (Math.pow(norm, 1.4) * intensity * 0.75);
                        }
                    }
                }
            }
        }

        // 3. Render Pure Grayscale [0..255] on Black Background (0,0,0)
        Graphics2D g2fill = img.createGraphics();
        g2fill.setColor(Color.BLACK);
        g2fill.fillRect(0, 0, w, h);
        g2fill.dispose();

        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                float v = grid[py][px];
                if (v > 0.005f) {
                    double norm = Math.clamp(1.0 - Math.exp(-v * 0.45), 0.0, 1.0);
                    int gray = (int) Math.clamp(norm * 255.0, 0.0, 255.0);
                    img.setRGB(px, py, (gray << 16) | (gray << 8) | gray);
                } else {
                    img.setRGB(px, py, 0x000000);
                }
            }
        }
    }

    private static Color lerpColor(Color c1, Color c2, double t) {
        int r = (int) Math.clamp(c1.getRed() + t * (c2.getRed() - c1.getRed()), 0, 255);
        int g = (int) Math.clamp(c1.getGreen() + t * (c2.getGreen() - c1.getGreen()), 0, 255);
        int b = (int) Math.clamp(c1.getBlue() + t * (c2.getBlue() - c1.getBlue()), 0, 255);
        return new Color(r, g, b);
    }

    private static final int BIOME_DEEP_OCEAN = 0x000064; // RGB(0, 0, 100)
    private static final int BIOME_OCEAN      = 0x0032C8; // RGB(0, 50, 200)
    private static final int BIOME_BEACH      = 0xF0DC96; // RGB(240, 220, 150)
    private static final int BIOME_PLAINS     = 0x64C832; // RGB(100, 200, 50)
    private static final int BIOME_FOREST     = 0x147814; // RGB(20, 120, 20)
    private static final int BIOME_JUNGLE     = 0x005000; // RGB(0, 80, 0)
    private static final int BIOME_DESERT     = 0xFFC832; // RGB(255, 200, 50)
    private static final int BIOME_HILLS      = 0x969664; // RGB(150, 150, 100)
    private static final int BIOME_MOUNTAINS  = 0x646464; // RGB(100, 100, 100)
    private static final int BIOME_TUNDRA     = 0x96C8DC; // RGB(150, 200, 220)
    private static final int BIOME_SNOW       = 0xFFFFFF; // RGB(255, 255, 255)
    private static final int BIOME_GLACIER    = 0xDCF0FF; // RGB(220, 240, 255)

    /**
     * Standardized False-Color Hypsometric & Bathymetric Color Ramp for Elevation Visualization.
     * Deep Navy Abyssal -> Continental Shelf Cyan -> Sandy Shoreline -> Lush Plains -> Ochre Hills -> Rust Mountains -> Alpine Snow.
     */
    public static Color elevationToHypsometricColor(double norm) {
        norm = Math.clamp(norm, 0.0, 1.0);
        if (norm < 0.20) {
            // Deep Oceanic Trench (0.00) -> Abyssal Plain (0.20)
            return lerpColor(new Color(5, 12, 36), new Color(15, 35, 80), norm / 0.20);
        } else if (norm < 0.40) {
            // Abyssal Plain (0.20) -> Oceanic Basin (0.40)
            return lerpColor(new Color(15, 35, 80), new Color(24, 72, 135), (norm - 0.20) / 0.20);
        } else if (norm < 0.475) {
            // Oceanic Basin (0.40) -> Continental Shelf Cyan (0.475)
            return lerpColor(new Color(24, 72, 135), new Color(40, 145, 175), (norm - 0.40) / 0.075);
        } else if (norm < 0.485) {
            // Coastline / Shoreline (0.475 -> 0.485)
            return lerpColor(new Color(40, 145, 175), new Color(225, 210, 155), (norm - 0.475) / 0.010);
        } else if (norm < 0.58) {
            // Lowlands & Plains (0.485 -> 0.58)
            return lerpColor(new Color(45, 135, 55), new Color(115, 175, 45), (norm - 0.485) / 0.095);
        } else if (norm < 0.70) {
            // Plateaus & Hills (0.58 -> 0.70)
            return lerpColor(new Color(205, 165, 50), new Color(180, 120, 40), (norm - 0.58) / 0.12);
        } else if (norm < 0.85) {
            // High Mountains (0.70 -> 0.85)
            return lerpColor(new Color(140, 65, 30), new Color(105, 95, 95), (norm - 0.70) / 0.15);
        } else {
            // Alpine Peaks & Ice (0.85 -> 1.00)
            return lerpColor(new Color(105, 95, 95), new Color(255, 255, 255), (norm - 0.85) / 0.15);
        }
    }

    private static void rasterizeAlphaDensity(BufferedImage img, List<double[]> spots, double defaultRadius) {
        rasterizeBasinsAndSpots(img, null, spots, defaultRadius);
    }

    private static void rasterizeTieredDensity(BufferedImage img, List<double[]> spots, double defaultRadius) {
        rasterizeBasinsAndSpots(img, null, spots, defaultRadius);
    }

    private static void saveMapImage(BufferedImage img, String baseName, String canonicalPreset, long year) {
        try {
            File etherSubDir = new File("data/maps/ether/" + canonicalPreset + "/" + year);
            etherSubDir.mkdirs();
            String stdFileName = TemporalMapTensorManager.buildStandardFilename(canonicalPreset, year, baseName);
            File fEtherSub = new File(etherSubDir, stdFileName);
            ImageIO.write(img, "PNG", fEtherSub);

            logger.info("Saved {} into data/maps/ether/{}/{}/", stdFileName, canonicalPreset, year);
        } catch (Exception e) {
            logger.error("Failed saving {} to {}/{}: {}", baseName, canonicalPreset, year, e.getMessage());
        }
    }

    private static void saveMapImage(BufferedImage img, String baseName, String canonicalPreset) {
        saveMapImage(img, baseName, canonicalPreset, 2026L);
    }

    private static File getPaleoClimFile(long epoch, String bioName) {
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

    private static float sampleGeoTiff(BufferedImage img, double lat, double lon) {
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

        if (lat > latMax || lat < latMin) {
            return -99999f;
        }

        int sx = (int) Math.clamp(((lon + 180.0) / 360.0) * (w - 1), 0, w - 1);
        int sy = (int) Math.clamp(((latMax - lat) / (latMax - latMin)) * (h - 1), 0, h - 1);
        return img.getRaster().getSampleFloat(sx, sy, 0);
    }

    @Test
    public void generateEarthMapsEpochMinus100k() {
        logger.info(">>> Generating Targeted Earth Cartographic Tensors for Epoch -100000 (Out of Africa Baseline) <<<");
        generateEarthMapsForEpoch(-100000L);
        generateEarthResourcesForEpoch(-100000L);
    }

    @Test
    public void generateAllPresetMaps() throws Exception {
        logger.info("--- GENERATING AUTHENTIC PLANETARY MAPS FOR ALL DATED EPOCHS IN GRAYSCALE [0..255] ---");

        // 1. EARTH / TERRE ALL EPOCHS (-100000, -20000, -10000, -6000, -3000, 0, 1000, 1800, 1900, 1950, 2026)
        generateAllEarthEpochs();

        // 2. MOON / LUNE
        generateMoonMaps();

        // 3. MARS / ARES
        generateMarsMaps();

        // 4. VENUS / HESPEROS
        generateVenusMaps();

        // 5. MERCURY / HERMES
        generateMercuryMaps();

        logger.info("--- ALL PLANETARY CARTOGRAPHIC DATED TENSORS SUCCESSFULLY GENERATED ---");
    }

    private void generateAllEarthEpochs() {
        for (long epoch : TemporalMapTensorManager.STANDARD_EARTH_EPOCHS) {
            logger.info(">>> Generating Earth Cartographic Tensors for Epoch: {} AD/BC <<<", epoch);
            generateEarthMapsForEpoch(epoch);
            generateEarthResourcesForEpoch(epoch);
        }
    }

    private static float[][] getEtopoGrid() {
        return EtopoGeoTiffReader.loadEtopoGrid(WIDTH, HEIGHT);
    }

    private void generateEarthMapsForEpoch(long epoch) {
        logger.info("Generating Earth Elevation, Biomes, and Empirical PaleoClim Climate Tensors for epoch {}...", epoch);

        // 1. Elevation directly from NOAA ETOPO 2022 Global Relief Model (Pure Grayscale Heightmap)
        BufferedImage imgElev = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        float[][] etopo = getEtopoGrid();
        double[][] gridNorm = new double[HEIGHT][WIDTH];

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                float altM = etopo[y][x];
                double norm;
                if (altM < 0) {
                    norm = 0.478 * (1.0 + Math.max(-11000.0f, altM) / 11000.0);
                } else {
                    norm = 0.478 + 0.522 * Math.min(1.0, altM / 8848.0);
                }
                norm = Math.clamp(norm, 0.0, 1.0);
                gridNorm[y][x] = norm;
                int gray = (int) Math.clamp(norm * 255.0, 0.0, 255.0);
                imgElev.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        saveMapImage(imgElev, "earth_elevation.png", "earth", epoch);

        // 2. Temperature (-50°C to +50°C -> [0..255]) sourced from PaleoClim/CHELSA with globally continuous ocean model
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

                // Continuous global oceanic temperature model (-24°C at poles to +28.5°C at equator)
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
                        // Antarctica or missing polar land
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

        // 3. Precipitation (0 to 3000 mm/yr) sourced from PaleoClim/CHELSA with globally continuous ocean model
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

                // Continuous global oceanic precipitation model (ITCZ, Subtropical dry belts, Mid-latitude storm tracks)
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

        // 4. Seasonality sourced from PaleoClim/CHELSA with globally continuous ocean model
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
        BufferedImage imgBiomes = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        BufferedImage refBiome = null;
        File refFile = new File("data/maps/reference_earth_biomes.png");
        if (!refFile.exists()) {
            refFile = new File("data/maps/scratch_db000ab_earth_biomes.png");
        }
        if (refFile.exists()) {
            try {
                refBiome = ImageIO.read(refFile);
            } catch (Exception e) {
                logger.warn("Failed reading reference biomes: {}", e.getMessage());
            }
        }

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / (double) HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lon = -180.0 + (x + 0.5) / (double) WIDTH * 360.0;
                double norm = gridNorm[y][x];
                double tempC = gridTemp[y][x];
                double precipMm = gridPrecip[y][x];

                int bColor;
                // Bathymetric land/ocean separation:
                if (norm < 0.478) {
                    // Oceanic pixels: strictly ocean water colors, never land glaciers/snow
                    if (norm < 0.45) {
                        bColor = BIOME_DEEP_OCEAN;
                    } else {
                        bColor = BIOME_OCEAN;
                    }
                } else {
                    // Land pixels:
                    if (lat < -60.0) {
                        bColor = (tempC < -20.0 || norm > 0.65) ? BIOME_GLACIER : BIOME_SNOW;
                    } else if (lat > 75.0 && lon > -70.0 && lon < -15.0) {
                        bColor = BIOME_GLACIER; // Greenland ice sheet
                    } else if (refBiome != null) {
                        int rx = Math.clamp((int) (((lon + 180.0) / 360.0) * (refBiome.getWidth() - 1)), 0, refBiome.getWidth() - 1);
                        int ry = Math.clamp((int) (((90.0 - lat) / 180.0) * (refBiome.getHeight() - 1)), 0, refBiome.getHeight() - 1);
                        int baseColor = refBiome.getRGB(rx, ry) & 0xFFFFFF;

                        // If sampled color from refBiome was ocean, classify land biome properly
                        if (baseColor == BIOME_DEEP_OCEAN || baseColor == BIOME_OCEAN || baseColor == 0x000000) {
                            if (norm < 0.485 && tempC > 5.0) bColor = BIOME_BEACH;
                            else if (norm > 0.68) bColor = BIOME_MOUNTAINS;
                            else if (norm > 0.58) bColor = BIOME_HILLS;
                            else if (precipMm < 180) bColor = BIOME_DESERT;
                            else if (tempC >= 20.0 && precipMm >= 1800) bColor = BIOME_JUNGLE;
                            else if (precipMm >= 700) bColor = BIOME_FOREST;
                            else bColor = BIOME_PLAINS;
                        } else if (epoch <= -70000L) {
                            // Prehistory / Middle Paleolithic (-100,000 BP MIS 5e/5d transition):
                            // 1. Green Sahara & Arabian wet corridors (Savanna / Grassland corridors)
                            if (lat >= 14.0 && lat <= 26.0 && lon >= -15.0 && lon <= 55.0 && (baseColor == BIOME_DESERT || baseColor == 0xFFD232)) {
                                bColor = BIOME_PLAINS; // Savanna & grassy corridors
                            } else if (lat >= 48.0 && lat <= 62.0 && lon >= -10.0 && lon <= 120.0 && baseColor == BIOME_TUNDRA) {
                                bColor = (precipMm > 300.0) ? BIOME_PLAINS : BIOME_TUNDRA; // Mammoth steppe / Periglacial plains
                            } else if (lat > 66.0 && (lon > 10.0 && lon < 40.0)) {
                                bColor = BIOME_GLACIER; // Scandinavian incipient ice
                            } else {
                                bColor = baseColor;
                            }
                        } else {
                            bColor = baseColor;
                        }
                    } else {
                        // Fallback algorithmic biome
                        if (norm < 0.485 && tempC > 5.0) {
                            bColor = BIOME_BEACH;
                        } else if (norm > 0.80 || tempC < -12.0) {
                            bColor = BIOME_GLACIER;
                        } else if (norm > 0.72 || tempC < -3.0) {
                            bColor = BIOME_SNOW;
                        } else if (tempC < 4.0) {
                            bColor = (precipMm < 400) ? BIOME_TUNDRA : BIOME_FOREST;
                        } else if (norm > 0.65) {
                            bColor = BIOME_MOUNTAINS;
                        } else if (norm > 0.55) {
                            bColor = BIOME_HILLS;
                        } else if (precipMm < 180) {
                            bColor = BIOME_DESERT;
                        } else if (tempC >= 20.0 && precipMm >= 1800) {
                            bColor = BIOME_JUNGLE;
                        } else if (precipMm >= 700) {
                            bColor = BIOME_FOREST;
                        } else {
                            bColor = BIOME_PLAINS;
                        }
                    }
                }
                imgBiomes.setRGB(x, y, bColor);
            }
        }
        saveMapImage(imgBiomes, "earth_biomes.png", "earth", epoch);
        if (epoch == 2026L) {
            try {
                ImageIO.write(imgElev, "png", new File("data/maps/reference_earth_elevation.png"));
                ImageIO.write(imgBiomes, "png", new File("data/maps/reference_earth_biomes.png"));
            } catch (Exception ignored) {}
        }
    }

    private void generateEarthResourcesForEpoch(long epoch) {
        logger.info("Generating Earth Geological & Mineral Tensors for epoch {}...", epoch);

        double depletionMultiplier = 1.0;
        if (epoch > 1800L) {
            double progress = (epoch - 1800.0) / (2026.0 - 1800.0);
            depletionMultiplier = Math.clamp(1.0 - progress * 0.35, 0.65, 1.0);
        }

        // 1. COAL BASINS & DEPOSITS
        BufferedImage imgCoal = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        List<GeologicalBasin> coalBasins = new ArrayList<>();
        coalBasins.add(new GeologicalBasin(-80.0, 38.5, 6.5, 2.2, 45.0, 3.8 * depletionMultiplier, "Appalachian Basin"));
        coalBasins.add(new GeologicalBasin(-89.0, 38.5, 3.5, 2.8, 0.0, 3.2 * depletionMultiplier, "Illinois Basin"));
        coalBasins.add(new GeologicalBasin(-105.8, 44.5, 3.2, 2.0, 115.0, 4.2 * depletionMultiplier, "Powder River Basin"));
        coalBasins.add(new GeologicalBasin(-102.5, 47.5, 3.0, 2.2, 0.0, 2.8 * depletionMultiplier, "Williston Fort Union Lignite"));
        coalBasins.add(new GeologicalBasin(-95.0, 36.0, 3.5, 2.0, 40.0, 2.6 * depletionMultiplier, "Western Interior / Arkoma"));
        coalBasins.add(new GeologicalBasin(-95.5, 31.5, 5.5, 1.2, 60.0, 2.4 * depletionMultiplier, "Gulf Coast Wilcox Lignite"));
        coalBasins.add(new GeologicalBasin(-107.5, 41.5, 2.5, 1.8, 0.0, 2.8 * depletionMultiplier, "Green River / Hanna Basin"));
        coalBasins.add(new GeologicalBasin(-109.5, 39.5, 2.8, 1.8, 120.0, 2.8 * depletionMultiplier, "Uinta-Piceance Coal"));
        coalBasins.add(new GeologicalBasin(-108.0, 36.5, 2.2, 1.8, 0.0, 3.0 * depletionMultiplier, "San Juan Fruitland Coal"));
        coalBasins.add(new GeologicalBasin(-116.5, 53.0, 6.0, 2.0, 135.0, 3.4 * depletionMultiplier, "Alberta Foothills Coal"));
        coalBasins.add(new GeologicalBasin(-60.2, 46.2, 1.5, 1.0, 60.0, 2.4 * depletionMultiplier, "Sydney Basin Nova Scotia"));
        coalBasins.add(new GeologicalBasin(-151.0, 61.5, 2.5, 1.2, 30.0, 2.4 * depletionMultiplier, "Cook Inlet Beluga Alaska"));
        coalBasins.add(new GeologicalBasin(-101.5, 27.8, 2.0, 1.2, 130.0, 2.4 * depletionMultiplier, "Sabinas Basin Mexico"));
        coalBasins.add(new GeologicalBasin(-72.7, 11.1, 2.2, 1.0, 45.0, 3.8 * depletionMultiplier, "Cerrejon Colombia"));
        coalBasins.add(new GeologicalBasin(-73.5, 5.5, 2.0, 1.0, 35.0, 2.8 * depletionMultiplier, "Boyaca-Cundinamarca Colombia"));
        coalBasins.add(new GeologicalBasin(-72.3, 10.9, 1.5, 0.8, 40.0, 2.8 * depletionMultiplier, "Guasare Basin Venezuela"));
        coalBasins.add(new GeologicalBasin(-51.5, -29.5, 4.5, 2.0, 90.0, 3.0 * depletionMultiplier, "Parana Basin Brazil"));
        coalBasins.add(new GeologicalBasin(-72.3, -51.5, 1.8, 1.0, 0.0, 2.4 * depletionMultiplier, "Rio Turbio Argentina"));
        coalBasins.add(new GeologicalBasin(-77.0, -10.5, 1.5, 0.8, 140.0, 2.2 * depletionMultiplier, "Oyon Basin Peru"));
        coalBasins.add(new GeologicalBasin(19.0, 50.2, 2.2, 1.6, 120.0, 4.0 * depletionMultiplier, "Upper Silesian Basin Poland/Czechia"));
        coalBasins.add(new GeologicalBasin(23.0, 51.3, 1.8, 1.0, 135.0, 2.8 * depletionMultiplier, "Lublin Coal Basin Poland"));
        coalBasins.add(new GeologicalBasin(7.3, 51.5, 2.0, 1.2, 70.0, 3.8 * depletionMultiplier, "Ruhr Basin Germany"));
        coalBasins.add(new GeologicalBasin(6.8, 49.3, 1.5, 0.8, 60.0, 2.8 * depletionMultiplier, "Saar-Lorraine Basin"));
        coalBasins.add(new GeologicalBasin(6.5, 50.9, 1.2, 0.8, 135.0, 3.5 * depletionMultiplier, "Rhineland Lignite District"));
        coalBasins.add(new GeologicalBasin(13.5, 51.6, 2.5, 1.5, 0.0, 3.2 * depletionMultiplier, "Lusatian / Central German Lignite"));
        coalBasins.add(new GeologicalBasin(3.0, 50.4, 2.5, 0.6, 80.0, 2.8 * depletionMultiplier, "Nord-Pas-de-Calais France/Belgium"));
        coalBasins.add(new GeologicalBasin(-1.3, 53.5, 2.0, 1.2, 0.0, 3.0 * depletionMultiplier, "Yorkshire / East Midlands UK"));
        coalBasins.add(new GeologicalBasin(-3.6, 51.7, 1.5, 0.8, 90.0, 2.8 * depletionMultiplier, "South Wales Coalfield"));
        coalBasins.add(new GeologicalBasin(-3.8, 55.9, 1.2, 0.6, 70.0, 2.4 * depletionMultiplier, "Scottish Central Coalfield"));
        coalBasins.add(new GeologicalBasin(-5.8, 43.3, 1.5, 0.8, 90.0, 2.6 * depletionMultiplier, "Asturias Basin Spain"));
        coalBasins.add(new GeologicalBasin(23.3, 45.4, 1.0, 0.5, 90.0, 2.6 * depletionMultiplier, "Jiu Valley Romania"));
        coalBasins.add(new GeologicalBasin(26.0, 42.2, 1.5, 1.0, 90.0, 2.8 * depletionMultiplier, "Maritsa Iztok Lignite Bulgaria"));
        coalBasins.add(new GeologicalBasin(21.7, 40.5, 1.5, 0.8, 140.0, 2.6 * depletionMultiplier, "Ptolemaida-Florina Greece"));
        coalBasins.add(new GeologicalBasin(20.3, 44.4, 1.8, 1.0, 120.0, 2.8 * depletionMultiplier, "Kolubara-Kostolac Serbia"));
        coalBasins.add(new GeologicalBasin(31.8, 41.4, 1.5, 0.8, 75.0, 2.8 * depletionMultiplier, "Zonguldak Basin Turkey"));
        coalBasins.add(new GeologicalBasin(27.6, 39.2, 1.2, 0.8, 45.0, 2.6 * depletionMultiplier, "Soma Lignite Basin Turkey"));
        coalBasins.add(new GeologicalBasin(38.2, 48.2, 4.5, 1.8, 110.0, 4.2 * depletionMultiplier, "Donbas"));
        coalBasins.add(new GeologicalBasin(87.0, 54.5, 3.8, 2.2, 160.0, 4.8 * depletionMultiplier, "Kuzbass"));
        coalBasins.add(new GeologicalBasin(93.5, 56.0, 6.5, 2.0, 80.0, 4.4 * depletionMultiplier, "Kansk-Achinsk"));
        coalBasins.add(new GeologicalBasin(98.0, 64.0, 8.0, 6.0, 0.0, 3.8 * depletionMultiplier, "Tunguska Basin"));
        coalBasins.add(new GeologicalBasin(126.0, 65.0, 7.0, 4.5, 0.0, 3.6 * depletionMultiplier, "Lena Basin"));
        coalBasins.add(new GeologicalBasin(60.5, 66.5, 3.5, 2.0, 45.0, 3.8 * depletionMultiplier, "Pechora Basin"));
        coalBasins.add(new GeologicalBasin(73.1, 49.8, 2.5, 1.5, 90.0, 3.8 * depletionMultiplier, "Karaganda"));
        coalBasins.add(new GeologicalBasin(75.3, 51.7, 1.8, 1.2, 45.0, 4.0 * depletionMultiplier, "Ekibastuz"));
        coalBasins.add(new GeologicalBasin(112.5, 37.8, 5.5, 2.5, 25.0, 5.0 * depletionMultiplier, "Shanxi Datong"));
        coalBasins.add(new GeologicalBasin(109.5, 39.0, 4.5, 3.5, 0.0, 5.0 * depletionMultiplier, "Ordos Shenfu"));
        coalBasins.add(new GeologicalBasin(86.2, 23.7, 3.5, 1.2, 90.0, 4.5 * depletionMultiplier, "Damodar Valley"));
        coalBasins.add(new GeologicalBasin(103.8, -3.7, 3.5, 1.8, 135.0, 3.8 * depletionMultiplier, "South Sumatra"));
        coalBasins.add(new GeologicalBasin(116.8, -1.0, 4.0, 2.0, 0.0, 4.4 * depletionMultiplier, "East Kalimantan"));
        coalBasins.add(new GeologicalBasin(29.2, -26.0, 2.8, 1.8, 90.0, 4.4 * depletionMultiplier, "Witbank South Africa"));
        coalBasins.add(new GeologicalBasin(148.5, -22.5, 6.0, 2.0, 160.0, 4.8 * depletionMultiplier, "Bowen Basin Australia"));
        coalBasins.add(new GeologicalBasin(150.8, -32.8, 3.5, 1.8, 90.0, 4.2 * depletionMultiplier, "Sydney Basin Australia"));

        List<double[]> coalSpots = EmpiricalGeospatialDatasetIngestion.getEmpiricalCoalOccurrences();
        rasterizeBasinsAndSpots(imgCoal, coalBasins, coalSpots, 6.0);
        saveMapImage(imgCoal, "earth_coal.png", "earth", epoch);

        // 2. CRUDE OIL BASINS & WELLS
        BufferedImage imgOil = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        List<GeologicalBasin> oilBasins = new ArrayList<>();
        oilBasins.add(new GeologicalBasin(49.3, 25.5, 6.5, 3.0, 15.0, 5.0 * depletionMultiplier, "Ghawar Saudi Arabia"));
        oilBasins.add(new GeologicalBasin(48.0, 29.1, 2.8, 2.0, 0.0, 4.8 * depletionMultiplier, "Burgan Kuwait"));
        oilBasins.add(new GeologicalBasin(47.2, 30.5, 6.5, 2.5, 135.0, 4.8 * depletionMultiplier, "Rumaila Iraq"));
        oilBasins.add(new GeologicalBasin(49.8, 31.3, 5.5, 2.0, 135.0, 4.8 * depletionMultiplier, "Zagros Oil Belt"));
        oilBasins.add(new GeologicalBasin(76.5, 61.2, 7.5, 5.0, 0.0, 5.0 * depletionMultiplier, "West Siberia Samotlor"));
        oilBasins.add(new GeologicalBasin(52.5, 54.8, 6.0, 4.0, 0.0, 4.5 * depletionMultiplier, "Volga-Ural Romashkino"));
        oilBasins.add(new GeologicalBasin(51.8, 46.5, 5.0, 3.5, 0.0, 5.0 * depletionMultiplier, "Tengiz/Kashagan Kazakhstan"));
        oilBasins.add(new GeologicalBasin(50.5, 40.0, 4.0, 2.5, 135.0, 4.6 * depletionMultiplier, "South Caspian Baku"));
        oilBasins.add(new GeologicalBasin(-102.5, 31.8, 4.5, 3.2, 140.0, 5.0 * depletionMultiplier, "Permian Basin TX/NM"));
        oilBasins.add(new GeologicalBasin(-90.5, 27.5, 5.0, 2.5, 90.0, 4.8 * depletionMultiplier, "Deepwater Gulf of Mexico"));
        oilBasins.add(new GeologicalBasin(-148.5, 70.2, 5.0, 1.8, 90.0, 4.8 * depletionMultiplier, "Prudhoe Bay Alaska"));
        oilBasins.add(new GeologicalBasin(-111.5, 56.8, 6.5, 3.5, 135.0, 5.0 * depletionMultiplier, "Athabasca Oil Sands"));
        oilBasins.add(new GeologicalBasin(-71.5, 10.0, 3.0, 2.0, 0.0, 4.8 * depletionMultiplier, "Maracaibo Basin"));
        oilBasins.add(new GeologicalBasin(-64.0, 8.5, 6.5, 1.5, 90.0, 5.0 * depletionMultiplier, "Faja del Orinoco"));
        oilBasins.add(new GeologicalBasin(-43.0, -24.5, 6.0, 3.0, 45.0, 5.0 * depletionMultiplier, "Santos Pre-Salt Brazil"));
        oilBasins.add(new GeologicalBasin(6.0, 4.8, 5.0, 3.5, 0.0, 5.0 * depletionMultiplier, "Niger Delta"));
        oilBasins.add(new GeologicalBasin(11.8, -6.5, 5.0, 2.5, 140.0, 4.8 * depletionMultiplier, "Lower Congo Deepwater"));
        oilBasins.add(new GeologicalBasin(19.5, 29.0, 4.5, 3.5, 0.0, 4.6 * depletionMultiplier, "Sirte Basin Libya"));
        oilBasins.add(new GeologicalBasin(6.0, 31.5, 4.0, 3.0, 0.0, 4.6 * depletionMultiplier, "Hassi Messaoud Algeria"));
        oilBasins.add(new GeologicalBasin(2.5, 57.5, 6.0, 3.0, 0.0, 4.8 * depletionMultiplier, "North Sea Ekofisk"));
        oilBasins.add(new GeologicalBasin(125.0, 46.5, 4.5, 2.5, 25.0, 4.8 * depletionMultiplier, "Daqing Songliao"));
        oilBasins.add(new GeologicalBasin(118.5, 38.0, 4.0, 3.0, 40.0, 4.6 * depletionMultiplier, "Bohai Bay Shengli"));
        oilBasins.add(new GeologicalBasin(101.5, 0.8, 4.5, 2.0, 135.0, 4.6 * depletionMultiplier, "Central Sumatra Minas"));
        oilBasins.add(new GeologicalBasin(72.0, 19.3, 3.5, 2.0, 0.0, 4.4 * depletionMultiplier, "Mumbai High"));
        oilBasins.add(new GeologicalBasin(148.5, -38.5, 2.5, 1.2, 90.0, 4.2 * depletionMultiplier, "Gippsland Basin Australia"));

        List<double[]> oilSpots = EmpiricalGeospatialDatasetIngestion.getEmpiricalOilOccurrences();
        rasterizeBasinsAndSpots(imgOil, oilBasins, oilSpots, 6.0);
        saveMapImage(imgOil, "earth_oil.png", "earth", epoch);

        // 3. NATURAL GAS BASINS & FIELDS
        BufferedImage imgGas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        List<GeologicalBasin> gasBasins = new ArrayList<>();
        gasBasins.add(new GeologicalBasin(51.8, 26.5, 4.0, 3.0, 150.0, 5.0 * depletionMultiplier, "North Field Qatar"));
        gasBasins.add(new GeologicalBasin(69.5, 70.5, 5.5, 3.5, 0.0, 5.0 * depletionMultiplier, "Yamal Bovanenkovo"));
        gasBasins.add(new GeologicalBasin(77.5, 66.0, 6.0, 4.0, 0.0, 5.0 * depletionMultiplier, "Urengoy/Yamburg Russia"));
        gasBasins.add(new GeologicalBasin(62.2, 37.3, 4.5, 3.0, 135.0, 5.0 * depletionMultiplier, "Galkynysh Turkmenistan"));
        gasBasins.add(new GeologicalBasin(-78.5, 40.5, 6.5, 3.0, 45.0, 5.0 * depletionMultiplier, "Appalachian Marcellus Shale"));
        gasBasins.add(new GeologicalBasin(-93.8, 32.2, 3.0, 2.2, 120.0, 4.6 * depletionMultiplier, "Haynesville Shale"));
        gasBasins.add(new GeologicalBasin(-120.0, 56.0, 6.0, 3.0, 135.0, 4.8 * depletionMultiplier, "Montney Formation Canada"));
        gasBasins.add(new GeologicalBasin(6.8, 53.3, 4.5, 2.0, 90.0, 4.8 * depletionMultiplier, "Groningen Netherlands"));
        gasBasins.add(new GeologicalBasin(3.5, 60.6, 5.0, 2.5, 0.0, 5.0 * depletionMultiplier, "Troll Norwegian North Sea"));
        gasBasins.add(new GeologicalBasin(3.3, 32.9, 3.5, 2.5, 0.0, 5.0 * depletionMultiplier, "Hassi R'Mel Algeria"));
        gasBasins.add(new GeologicalBasin(40.8, -11.0, 3.5, 1.5, 0.0, 5.0 * depletionMultiplier, "Rovuma Mozambique"));
        gasBasins.add(new GeologicalBasin(106.0, 30.5, 4.5, 3.0, 40.0, 5.0 * depletionMultiplier, "Sichuan Basin"));
        gasBasins.add(new GeologicalBasin(115.5, -19.5, 5.0, 2.2, 50.0, 5.0 * depletionMultiplier, "Gorgon NW Shelf Australia"));
        gasBasins.add(new GeologicalBasin(-72.8, -11.8, 2.5, 1.5, 135.0, 4.8 * depletionMultiplier, "Camisea Peru"));

        List<double[]> gasSpots = EmpiricalGeospatialDatasetIngestion.getEmpiricalGasOccurrences();
        rasterizeBasinsAndSpots(imgGas, gasBasins, gasSpots, 6.0);
        saveMapImage(imgGas, "earth_gas.png", "earth", epoch);

        // 4. Uranium Deposits
        BufferedImage imgUranium = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        List<double[]> uSpots = extractMrdsDeposits("uranium", "thorium", "uraninite", "pitchblende", "carnotite");
        double[][] uBasins = {
            {-105.0, 58.0, 42, 2.8}, {136.9, -30.4, 38, 2.8}, {68.0, 44.0, 48, 3.0},
            {7.4, 18.7, 36, 2.4}, {27.5, -26.2, 36, 2.4}, {118.0, 50.0, 38, 2.4},
            {15.0, -22.5, 38, 2.4}, {132.8, -12.7, 35, 2.2}, {-109.5, 38.5, 38, 2.2}
        };
        for (double[] b : uBasins) uSpots.add(b);
        rasterizeAlphaDensity(imgUranium, uSpots, 8.0);
        saveMapImage(imgUranium, "earth_uranium.png", "earth", epoch);

        // 5. Helium-3 (0 on Earth)
        BufferedImage imgHe3 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        saveMapImage(imgHe3, "earth_helium3.png", "earth", epoch);

        // 6. Iron & Copper Formations
        BufferedImage imgIronCopper = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        List<double[]> feCuSpots = extractMrdsDeposits("iron", "copper", "magnetite", "hematite", "chalcopyrite", "bornite", "taconite");
        double[][] majorFeCu = {
            {118.0, -22.5, 45, 2.8}, {120.5, -23.0, 40, 2.5}, {-50.0, -6.0, 48, 3.0}, {-43.5, -20.0, 40, 2.4},
            {37.0, 51.5, 48, 2.8}, {33.5, 48.0, 42, 2.5}, {-91.5, 47.5, 40, 2.4}, {-66.5, 54.0, 42, 2.4},
            {20.0, 67.8, 35, 2.2}, {85.5, 22.0, 42, 2.4}, {-69.0, -24.0, 48, 2.8}, {-69.5, -22.3, 45, 2.6},
            {-70.5, -34.0, 45, 2.6}, {137.0, -4.0, 40, 2.5}, {-111.0, 33.5, 38, 2.2}, {28.0, -12.5, 45, 2.6},
            {102.0, 25.0, 38, 2.2}, {88.0, 38.0, 32, 2.0}, {-108.0, 32.5, 32, 2.0}
        };
        for (double[] b : majorFeCu) feCuSpots.add(b);
        rasterizeTieredDensity(imgIronCopper, feCuSpots, 8.0);
        saveMapImage(imgIronCopper, "earth_iron_copper.png", "earth", epoch);

        // 7. Precious Metals (strictly Gold, Silver, Platinum, Palladium, Electrum)
        BufferedImage imgPrecious = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        List<double[]> preciousSpots = extractMrdsDeposits("gold", "silver", "platinum", "palladium", "electrum");
        double[][] majorPrecious = {
            {27.0, -26.5, 48, 2.8},   // Witwatersrand (South Africa - Giant Gold)
            {29.0, -24.5, 45, 2.6},   // Bushveld Complex Platinum (South Africa)
            {-116.0, 40.8, 42, 2.5},  // Carlin Trend Nevada (USA - Gold)
            {-65.7, -19.6, 45, 2.7},  // Potosí Cerro Rico (Bolivia - Silver)
            {121.5, -30.7, 40, 2.4},  // Kalgoorlie Super Pit (Australia - Gold)
            {64.6, 41.5, 42, 2.5},    // Muruntau Gold (Uzbekistan)
            {88.2, 69.3, 45, 2.6},    // Norilsk-Talnakh PGMs (Russia)
            {-81.0, 46.5, 38, 2.3},   // Sudbury Basin (Canada - PGMs/Au)
            {-78.5, -7.0, 40, 2.4},   // Yanacocha (Peru - Gold)
            {137.1, -4.0, 42, 2.5}    // Grasberg (Indonesia - Gold/Copper)
        };
        for (double[] b : majorPrecious) preciousSpots.add(b);
        rasterizeAlphaDensity(imgPrecious, preciousSpots, 7.0);
        saveMapImage(imgPrecious, "earth_precious_metals.png", "earth", epoch);

        // 8. Rare Earths & Critical Minerals (REE, Bastnasite, Monazite, Lithium Salars & Spodumene)
        BufferedImage imgRareEarths = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        List<double[]> reeSpots = extractMrdsDeposits("rare earth", "bastnasite", "monazite", "xenotime", "neodymium", "dysprosium", "yttrium", "lanthanum", "cerium", "lithium", "spodumene", "carbonatite", "loparite", "allanite");
        double[][] majorREE = {
            {109.9, 41.8, 55, 3.0},   // Bayan Obo (Inner Mongolia, China - Giant REE/Fe)
            {-115.5, 35.5, 42, 2.6},  // Mountain Pass (California, USA - Bastnäsite)
            {122.5, -28.7, 45, 2.7},  // Mount Weld (Western Australia - Carbonatite REE)
            {-46.0, 60.9, 45, 2.6},   // Kvanefjeld / Ilímaussaq (Greenland - REE/U)
            {116.5, 71.0, 45, 2.6},   // Tomtor (Yakutia, Russia - Carbonatite Nb/REE)
            {34.6, 67.8, 40, 2.4},    // Lovozero (Kola Peninsula, Russia - Loparite REE)
            {115.0, 25.5, 50, 2.8},   // Ganzhou / Jiangxi (South China - Heavy Ionic Clays)
            {103.5, 22.4, 38, 2.3},   // Dong Pao (Vietnam - Bastnäsite)
            {-46.9, -19.6, 42, 2.5},  // Araxá (Minas Gerais, Brazil - Carbonatite Nb/REE)
            {-67.5, -21.0, 52, 2.8},  // Salar de Atacama (Chile - Lithium Brines)
            {-68.0, -23.5, 50, 2.7},  // Salar de Uyuni (Bolivia - Lithium Brines)
            {116.0, -33.8, 42, 2.5},  // Greenbushes (Australia - Spodumene Lithium)
            {14.6, 58.1, 35, 2.2},    // Norra Kärr (Sweden - Heavy REE)
            {20.2, 67.8, 38, 2.3},    // Kiruna / Per Geijer (Sweden - Apatite REE)
            {-64.2, 56.3, 40, 2.4},   // Strange Lake (Quebec/Labrador, Canada)
            {-112.6, 62.1, 38, 2.3}   // Nechalacho (NWT, Canada - REE/Zr)
        };
        for (double[] r : majorREE) reeSpots.add(r);
        rasterizeAlphaDensity(imgRareEarths, reeSpots, 7.0);
        saveMapImage(imgRareEarths, "earth_rare_earths.png", "earth", epoch);

        // 9. Geothermal / Mantle Heat (Davies 2013 in Grayscale [0..255] on Black Background)
        BufferedImage imgGeothermal = HistoricalMapGenerator.rasterizeMantleHeatMap("EARTH", null);
        if (imgGeothermal != null) {
            saveMapImage(imgGeothermal, "earth_geothermal.png", "earth", epoch);
        }

        // 10. Freshwater Aquifers & Groundwater Systems
        BufferedImage imgAquifers = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        List<double[]> aqSpots = EmpiricalGeospatialDatasetIngestion.getEmpiricalAquiferOccurrences();
        double[][] majorAquifers = {
            {25.0, 22.0, 75, 2.5 * depletionMultiplier},   // Nubian Sandstone Aquifer
            {-100.0, 38.0, 58, 2.2 * depletionMultiplier}, // Ogallala Aquifer
            {-54.0, -25.0, 70, 2.4 * depletionMultiplier}, // Guaraní Aquifer
            {138.0, -26.0, 75, 2.4 * depletionMultiplier}, // Great Artesian Basin
            {10.0, 30.0, 65, 2.2 * depletionMultiplier},   // Northern Sahara Aquifer
            {80.0, 27.0, 65, 2.2 * depletionMultiplier},   // Indo-Gangetic Basin
            {2.0, 47.0, 48, 1.8 * depletionMultiplier},    // Paris / Aquitaine Basins
            {-60.0, -3.0, 80, 2.5 * depletionMultiplier},  // Amazon Aquifer System
            {22.0, -1.0, 70, 2.2 * depletionMultiplier},   // Congo Basin Aquifer
            {75.0, 60.0, 75, 2.4 * depletionMultiplier},   // West Siberian Basin Aquifer
            {122.0, -18.0, 58, 2.0 * depletionMultiplier}, // Canning Basin Australia
            {82.0, 39.0, 52, 1.8 * depletionMultiplier},   // Tarim Basin Aquifer
            {-48.0, -1.5, 48, 1.8 * depletionMultiplier},  // Marajó Aquifer System
            {-118.0, 36.0, 42, 1.8 * depletionMultiplier}, // California Central Valley Aquifer
            {45.0, 25.0, 52, 2.0 * depletionMultiplier}    // Arabian Aquifer System
        };
        for (double[] a : majorAquifers) aqSpots.add(a);
        rasterizeAlphaDensity(imgAquifers, aqSpots, 8.0);
        saveMapImage(imgAquifers, "earth_aquifers.png", "earth", epoch);
    }

    private void generateMoonMaps() {
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

        // 9. Seasonality (0 on Moon due to 1.54° tilt)
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

    private void generateMarsMaps() {
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
                // At lat = +90 (North): deltaT = 18 + 26 - 6 = 38°C
                // At lat = 0 (Equator): deltaT = 18°C (smooth C0 & C1 continuity!)
                // At lat = -90 (South): deltaT = 18 + 26 + 6 = 50°C
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

    private void generateVenusMaps() {
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

    private void generateMercuryMaps() {
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

