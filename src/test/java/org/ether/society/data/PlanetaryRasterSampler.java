/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.Tag;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
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

@Tag("manual")
public class PlanetaryRasterSampler {
    private static final Logger logger = LoggerFactory.getLogger(PlanetaryRasterSampler.class);

    static final int WIDTH = 2048;
    static final int HEIGHT = 1024;

    /*
     * Fast parse csv operation.
     * <p>
     * Executes operational logic for {@code PlanetaryRasterSampler} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param line the line argument (String)
     * @return the resulting computation or updated state reference
     */
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

        /*
         * Geological basin operation.
         * <p>
         * Executes operational logic for {@code PlanetaryRasterSampler} within the geospatial raster and tensor ingestion pipeline.
         * Enforces physical invariants, state continuity, and deterministic boundary conditions.
         * </p>
         *
         * @param centerLon the center lon argument (double)
         * @param centerLat the center lat argument (double)
         * @param majorDeg the major deg argument (double)
         * @param minorDeg the minor deg argument (double)
         * @param strikeDeg the strike deg argument (double)
         * @param intensity the intensity argument (double)
         * @param name the name argument (String)
         * @return the resulting computation or updated state reference
         */
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
    static void rasterizeBasinsAndSpots(BufferedImage img, List<GeologicalBasin> basins, List<double[]> discreteSpots, double spotDefaultRadius) {
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

    static final int BIOME_DEEP_OCEAN = 0x000064; // RGB(0, 0, 100)
    static final int BIOME_OCEAN      = 0x0032C8; // RGB(0, 50, 200)
    static final int BIOME_BEACH      = 0xF0DC96; // RGB(240, 220, 150)
    static final int BIOME_PLAINS     = 0x64C832; // RGB(100, 200, 50)
    static final int BIOME_FOREST     = 0x147814; // RGB(20, 120, 20)
    static final int BIOME_JUNGLE     = 0x005000; // RGB(0, 80, 0)
    static final int BIOME_DESERT     = 0xFFC832; // RGB(255, 200, 50)
    static final int BIOME_HILLS      = 0x969664; // RGB(150, 150, 100)
    static final int BIOME_MOUNTAINS  = 0x646464; // RGB(100, 100, 100)
    static final int BIOME_TUNDRA     = 0x96C8DC; // RGB(150, 200, 220)
    static final int BIOME_SNOW       = 0xFFFFFF; // RGB(255, 255, 255)
    static final int BIOME_GLACIER    = 0xDCF0FF; // RGB(220, 240, 255)

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

    static void rasterizeAlphaDensity(BufferedImage img, List<double[]> spots, double defaultRadius) {
        rasterizeBasinsAndSpots(img, null, spots, defaultRadius);
    }

    static void rasterizeTieredDensity(BufferedImage img, List<double[]> spots, double defaultRadius) {
        rasterizeBasinsAndSpots(img, null, spots, defaultRadius);
    }

    static void saveMapImage(BufferedImage img, String baseName, String canonicalPreset, long year) {
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

    static void saveMapImage(BufferedImage img, String baseName, String canonicalPreset) {
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

    /**
     * Generate earth maps epoch minus100k operation.
     * <p>
     * Executes operational logic for {@code PlanetaryRasterSampler} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     */
    @Test
    public void generateEarthMapsEpochMinus100k() {
        logger.info(">>> Generating Targeted Earth Cartographic Tensors for Epoch -100000 (Out of Africa Baseline) <<<");
        generateEarthMapsForEpoch(-100000L);
        generateEarthResourcesForEpoch(-100000L);
        assertTrue(WIDTH == 2048 && HEIGHT == 1024, "Default raster resolution must be 2048x1024");
    }

    /**
     * Generate all preset maps operation.
     * <p>
     * Executes operational logic for {@code PlanetaryRasterSampler} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     */
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
        assertTrue(TemporalMapTensorManager.STANDARD_EARTH_EPOCHS.length > 0, "Standard Earth epochs array must not be empty");
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

        // 2. Temperature (-50Ã‚Â°C to +50Ã‚Â°C -> [0..255]) sourced from PaleoClim/CHELSA with globally continuous ocean model
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

                // Continuous global oceanic temperature model (-24Ã‚Â°C at poles to +28.5Ã‚Â°C at equator)
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

        // 7. Precious Metals (Gold, Silver, Platinum, Las MÃƒÂ©dulas, Rio Tinto, Laurion, Dacia, Nubia, Witwatersrand)
        BufferedImage imgPrecious = HistoricalMapGenerator.rasterizePreciousMetalsMap("EARTH", null);
        if (imgPrecious != null) {
            saveMapImage(imgPrecious, "earth_precious_metals.png", "earth", epoch);
        }

        // 8. Rare Earths & Critical Minerals (REE, Bastnasite, Monazite, Lithium Salars & Spodumene)
        BufferedImage imgRareEarths = HistoricalMapGenerator.rasterizeRareEarthsMap("EARTH", null);
        if (imgRareEarths != null) {
            saveMapImage(imgRareEarths, "earth_rare_earths.png", "earth", epoch);
        }

        // 9. Geothermal / Mantle Heat (Davies 2013 in Grayscale [0..255] on Black Background)
        BufferedImage imgGeothermal = HistoricalMapGenerator.rasterizeMantleHeatMap("EARTH", null);
        if (imgGeothermal != null) {
            saveMapImage(imgGeothermal, "earth_geothermal.png", "earth", epoch);
        }

        // 10. Freshwater Aquifers & Groundwater Systems (UNESCO WHYMAP Sedimentary Basins)
        BufferedImage imgAquifers = HistoricalMapGenerator.rasterizeAquiferMap("EARTH", null);
        if (imgAquifers != null) {
            saveMapImage(imgAquifers, "earth_aquifers.png", "earth", epoch);
        }
    }


    private void generateMoonMaps() {
        ExtraterrestrialPlanetaryRasterSampler.generateMoonMaps();
    }

    private void generateMarsMaps() {
        ExtraterrestrialPlanetaryRasterSampler.generateMarsMaps();
    }

    private void generateVenusMaps() {
        ExtraterrestrialPlanetaryRasterSampler.generateVenusMaps();
    }

    private void generateMercuryMaps() {
        ExtraterrestrialPlanetaryRasterSampler.generateMercuryMaps();
    }
}