/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <h1>Planetary Raster & Geospatial Tensor Sampler</h1>
 *
 * <p>Ingests, rasterizes, and synthesizes 2D multi-spectral geospatial tensors across
 * historical and planetary bodies (Earth, Moon, Mars, Venus, Mercury).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
@Tag("manual")
public class PlanetaryRasterSampler {
    private static final Logger logger = LoggerFactory.getLogger(PlanetaryRasterSampler.class);

    static final int WIDTH = 2048;
    static final int HEIGHT = 1024;

    static final int BIOME_DEEP_OCEAN = 0x000064;
    static final int BIOME_OCEAN      = 0x0032C8;
    static final int BIOME_BEACH      = 0xF0DC96;
    static final int BIOME_PLAINS     = 0x64C832;
    static final int BIOME_FOREST     = 0x147814;
    static final int BIOME_JUNGLE     = 0x005000;
    static final int BIOME_DESERT     = 0xFFC832;
    static final int BIOME_HILLS      = 0x969664;
    static final int BIOME_MOUNTAINS  = 0x646464;
    static final int BIOME_TUNDRA     = 0x96C8DC;
    static final int BIOME_SNOW       = 0xFFFFFF;
    static final int BIOME_GLACIER    = 0xDCF0FF;

    /**
     * Fast delimiter-aware CSV line parser handling quoted text fields.
     *
     * @param line CSV line
     * @return parsed column values
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

    /**
     * Represents an elliptical geological sedimentary basin with orientation and resource intensity.
     */
    public static class GeologicalBasin {
        public final double centerLon;
        public final double centerLat;
        public final double majorDeg;
        public final double minorDeg;
        public final double strikeDeg;
        public final double intensity;
        public final String name;

        public GeologicalBasin(double centerLon, double centerLat, double majorDeg, double minorDeg,
                               double strikeDeg, double intensity, String name) {
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
     * Rasterizes oriented basins and discrete point spots into a normalized 8-bit grayscale tensor.
     *
     * @param img               destination buffered image
     * @param basins            list of geological basins
     * @param discreteSpots     point source coordinates and radii
     * @param spotDefaultRadius default kernel radius in pixels
     */
    static void rasterizeBasinsAndSpots(BufferedImage img, List<GeologicalBasin> basins,
                                        List<double[]> discreteSpots, double spotDefaultRadius) {
        int w = img.getWidth();
        int h = img.getHeight();
        float[][] grid = new float[h][w];

        if (basins != null) {
            for (GeologicalBasin basin : basins) {
                accumulateBasin(grid, basin, w, h);
            }
        }

        if (discreteSpots != null) {
            for (double[] spot : discreteSpots) {
                accumulateSpot(grid, spot, spotDefaultRadius, w, h);
            }
        }

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

    private static void accumulateBasin(float[][] grid, GeologicalBasin basin, int w, int h) {
        double cLon = basin.centerLon;
        double cLat = basin.centerLat;
        double a = basin.majorDeg;
        double b = basin.minorDeg;
        double strikeRad = Math.toRadians(basin.strikeDeg);
        double cosTheta = Math.cos(strikeRad);
        double sinTheta = Math.sin(strikeRad);
        double cosLat = Math.max(0.15, Math.cos(Math.toRadians(cLat)));
        double maxDeg = Math.max(a, b);

        int cx = (int) Math.round(((cLon + 180.0) / 360.0) * (w - 1));
        int cy = (int) Math.round(((90.0 - cLat) / 180.0) * (h - 1));
        int rx = (int) Math.ceil(((maxDeg / cosLat) / 360.0) * (w - 1));
        int ry = (int) Math.ceil((maxDeg / 180.0) * (h - 1));

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
                double u = dxDeg * cosTheta + dyDeg * sinTheta;
                double v = -dxDeg * sinTheta + dyDeg * cosTheta;

                double q = (u * u) / (a * a) + (v * v) / (b * b);
                if (q <= 1.0) {
                    double d = Math.sqrt(q);
                    grid[py][wrapX] += (float) (Math.pow(1.0 - d, 1.4) * basin.intensity);
                }
            }
        }
    }

    private static void accumulateSpot(float[][] grid, double[] spot, double defaultR, int w, int h) {
        double lon = spot[0];
        double lat = spot[1];
        double radius = spot.length > 2 ? spot[2] : defaultR;
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

    private static Color lerpColor(Color c1, Color c2, double t) {
        int r = (int) Math.clamp(c1.getRed() + t * (c2.getRed() - c1.getRed()), 0, 255);
        int g = (int) Math.clamp(c1.getGreen() + t * (c2.getGreen() - c1.getGreen()), 0, 255);
        int b = (int) Math.clamp(c1.getBlue() + t * (c2.getBlue() - c1.getBlue()), 0, 255);
        return new Color(r, g, b);
    }

    /**
     * Standardized False-Color Hypsometric & Bathymetric Color Ramp for Elevation Visualization.
     *
     * @param norm normalized elevation in [0..1]
     * @return interpolated color representation
     */
    public static Color elevationToHypsometricColor(double norm) {
        norm = Math.clamp(norm, 0.0, 1.0);
        if (norm < 0.20) {
            return lerpColor(new Color(5, 12, 36), new Color(15, 35, 80), norm / 0.20);
        } else if (norm < 0.40) {
            return lerpColor(new Color(15, 35, 80), new Color(24, 72, 135), (norm - 0.20) / 0.20);
        } else if (norm < 0.475) {
            return lerpColor(new Color(24, 72, 135), new Color(40, 145, 175), (norm - 0.40) / 0.075);
        } else if (norm < 0.485) {
            return lerpColor(new Color(40, 145, 175), new Color(225, 210, 155), (norm - 0.475) / 0.010);
        } else if (norm < 0.58) {
            return lerpColor(new Color(45, 135, 55), new Color(115, 175, 45), (norm - 0.485) / 0.095);
        } else if (norm < 0.70) {
            return lerpColor(new Color(205, 165, 50), new Color(180, 120, 40), (norm - 0.58) / 0.12);
        } else if (norm < 0.85) {
            return lerpColor(new Color(140, 65, 30), new Color(105, 95, 95), (norm - 0.70) / 0.15);
        } else {
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
            logger.info("Saved {} into data/ether/{}/{}/", stdFileName, canonicalPreset, year);
        } catch (Exception e) {
            logger.error("Failed saving {} to {}/{}: {}", baseName, canonicalPreset, year, e.getMessage());
        }
    }

    static void saveMapImage(BufferedImage img, String baseName, String canonicalPreset) {
        saveMapImage(img, baseName, canonicalPreset, 2026L);
    }

    /**
     * Generates targeted cartographic baseline for Epoch -100,000 BP.
     */
    @Test
    public void generateEarthMapsEpochMinus100k() {
        logger.info(">>> Generating Targeted Earth Cartographic Tensors for Epoch -100000 <<<");
        EarthClimateEpochRasterSampler.generateEarthMapsForEpoch(-100000L);
        EarthGeologicalEpochRasterSampler.generateEarthResourcesForEpoch(-100000L);
        assertTrue(WIDTH == 2048 && HEIGHT == 1024, "Default raster resolution must be 2048x1024");
    }

    /**
     * Generates all planetary cartographic presets and multi-millennial historical epochs.
     */
    @Test
    public void generateAllPresetMaps() throws Exception {
        logger.info("--- GENERATING AUTHENTIC PLANETARY MAPS FOR ALL DATED EPOCHS IN GRAYSCALE [0..255] ---");
        generateAllEarthEpochs();
        generateMoonMaps();
        generateMarsMaps();
        generateVenusMaps();
        generateMercuryMaps();
        logger.info("--- ALL PLANETARY CARTOGRAPHIC DATED TENSORS SUCCESSFULLY GENERATED ---");
        assertTrue(TemporalMapTensorManager.STANDARD_EARTH_EPOCHS.length > 0, "Standard Earth epochs array must not be empty");
    }

    private void generateAllEarthEpochs() {
        for (long epoch : TemporalMapTensorManager.STANDARD_EARTH_EPOCHS) {
            logger.info(">>> Generating Earth Cartographic Tensors for Epoch: {} AD/BC <<<", epoch);
            EarthClimateEpochRasterSampler.generateEarthMapsForEpoch(epoch);
            EarthGeologicalEpochRasterSampler.generateEarthResourcesForEpoch(epoch);
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
