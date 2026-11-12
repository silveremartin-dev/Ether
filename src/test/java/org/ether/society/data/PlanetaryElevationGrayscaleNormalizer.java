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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Converts extraterrestrial planetary elevation rasters from hypsometric color maps
 * to pure 1D Grayscale Digital Elevation Models (DEM), preserving continuous relief,
 * crater depth, and mountain elevation profiles without altering biomes or other layers.
 *
 * <p>Uses exact orthogonal projection onto the 8-segment 3D RGB hypsometric polyline
 * of {@link PlanetaryRasterSampler#elevationToHypsometricColor(double)} to invert color to altitude.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class PlanetaryElevationGrayscaleNormalizer {

    private static final Logger logger = LoggerFactory.getLogger(PlanetaryElevationGrayscaleNormalizer.class);

    private record Segment(double normA, double normB, double[] a, double[] b) {}

    private static final Segment[] HYPSO_SEGMENTS = new Segment[] {
            // Segment 0: 0.00 -> 0.20 (5, 12, 36) -> (15, 35, 80)
            new Segment(0.000, 0.200, new double[]{5, 12, 36}, new double[]{15, 35, 80}),
            // Segment 1: 0.20 -> 0.40 (15, 35, 80) -> (24, 72, 135)
            new Segment(0.200, 0.400, new double[]{15, 35, 80}, new double[]{24, 72, 135}),
            // Segment 2: 0.40 -> 0.475 (24, 72, 135) -> (40, 145, 175)
            new Segment(0.400, 0.475, new double[]{24, 72, 135}, new double[]{40, 145, 175}),
            // Segment 3: 0.475 -> 0.485 (40, 145, 175) -> (225, 210, 155)
            new Segment(0.475, 0.485, new double[]{40, 145, 175}, new double[]{225, 210, 155}),
            // Segment 4: 0.485 -> 0.58 (45, 135, 55) -> (115, 175, 45)
            new Segment(0.485, 0.580, new double[]{45, 135, 55}, new double[]{115, 175, 45}),
            // Segment 5: 0.58 -> 0.70 (205, 165, 50) -> (180, 120, 40)
            new Segment(0.580, 0.700, new double[]{205, 165, 50}, new double[]{180, 120, 40}),
            // Segment 6: 0.70 -> 0.85 (140, 65, 30) -> (105, 95, 95)
            new Segment(0.700, 0.850, new double[]{140, 65, 30}, new double[]{105, 95, 95}),
            // Segment 7: 0.85 -> 1.00 (105, 95, 95) -> (255, 255, 255)
            new Segment(0.850, 1.000, new double[]{105, 95, 95}, new double[]{255, 255, 255})
    };

    private PlanetaryElevationGrayscaleNormalizer() {}

    /**
     * Decodes an RGB hypsometric pixel to its exact continuous elevation normalized value in [0.0, 1.0].
     *
     * @param r red channel in [0, 255]
     * @param g green channel in [0, 255]
     * @param b blue channel in [0, 255]
     * @return continuous elevation fraction in [0.0, 1.0]
     */
    public static double decodeHypsometricRgbToNorm(int r, int g, int b) {
        // If already grayscale, preserve directly
        if (r == g && g == b) {
            return r / 255.0;
        }

        double bestDistSq = Double.MAX_VALUE;
        double bestNorm = 0.5;

        for (Segment seg : HYPSO_SEGMENTS) {
            double vX = seg.b[0] - seg.a[0];
            double vY = seg.b[1] - seg.a[1];
            double vZ = seg.b[2] - seg.a[2];
            double lenSq = vX * vX + vY * vY + vZ * vZ;
            if (lenSq < 1e-6) continue;

            double pX = r - seg.a[0];
            double pY = g - seg.a[1];
            double pZ = b - seg.a[2];

            double t = (pX * vX + pY * vY + pZ * vZ) / lenSq;
            t = Math.clamp(t, 0.0, 1.0);

            double projX = seg.a[0] + t * vX;
            double projY = seg.a[1] + t * vY;
            double projZ = seg.a[2] + t * vZ;

            double dX = r - projX;
            double dY = g - projY;
            double dZ = b - projZ;
            double distSq = dX * dX + dY * dY + dZ * dZ;

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestNorm = seg.normA + t * (seg.normB - seg.normA);
            }
        }

        return Math.clamp(bestNorm, 0.0, 1.0);
    }

    /**
     * Converts a single elevation raster file to pure 1D grayscale [0..255].
     *
     * @param file target image file to convert in-place
     */
    public static void convertElevationFileToGrayscale(File file) {
        if (file == null || !file.exists()) return;
        try {
            BufferedImage src = ImageIO.read(file);
            if (src == null) return;

            int w = src.getWidth();
            int h = src.getHeight();
            BufferedImage grayImg = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = src.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    double norm = decodeHypsometricRgbToNorm(r, g, b);
                    int gray = Math.clamp((int) Math.round(norm * 255.0), 0, 255);
                    int grayRgb = (gray << 16) | (gray << 8) | gray;
                    grayImg.setRGB(x, y, grayRgb);
                }
            }

            ImageIO.write(grayImg, "PNG", file);
            logger.info("Successfully converted elevation raster {} ({}x{}) to 1D Grayscale DEM.", file.getName(), w, h);
        } catch (Exception e) {
            logger.error("Failed to convert elevation raster {}: {}", file.getAbsolutePath(), e.getMessage());
        }
    }

    /**
     * Executes batch normalization of all extraterrestrial elevation rasters in canonical data/ether/.
     */
    public static void normalizeAllExtraterrestrialElevationRasters() {
        String[] planets = {"mars", "moon", "mercury", "venus", "titan"};
        for (String planet : planets) {
            processPlanetDir(new File("data/maps/ether/" + planet));
        }
    }

    private static void processPlanetDir(File planetDir) {
        if (!planetDir.exists() || !planetDir.isDirectory()) return;
        File[] subDirs = planetDir.listFiles(File::isDirectory);
        if (subDirs == null) return;

        for (File dir : subDirs) {
            File[] files = dir.listFiles((d, name) -> name.endsWith("_elevation.png"));
            if (files != null) {
                for (File f : files) {
                    convertElevationFileToGrayscale(f);
                }
            }
        }
    }
}
