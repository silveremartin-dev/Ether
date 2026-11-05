/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Executes lossless orthographic projection from hypsometric color ramps to 1D Grayscale DEMs
 * for all extraterrestrial bodies (Moon, Mars, Venus, Mercury, Titan), leaving all biomes and
 * ecological layers untouched.
 */
public class NormalizePlanetaryElevationTest {

    @Test
    @DisplayName("Normalize All Extraterrestrial Elevation Rasters to Grayscale DEMs")
    public void testNormalizeExtraterrestrialElevationRasters() {
        PlanetaryElevationGrayscaleNormalizer.normalizeAllExtraterrestrialElevationRasters();
    }

    @Test
    @DisplayName("Audit Rasters Across All Planets for Flatness or Color Anomalies")
    public void testAuditPlanetaryRasters() {
        String[] planets = {"mars", "moon", "mercury", "venus", "titan"};
        for (String p : planets) {
            File dir = new File("data/maps/ether/" + p);
            if (!dir.exists()) continue;
            auditDir(dir);
        }
    }

    private void auditDir(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                auditDir(f);
            } else if (f.getName().endsWith(".png")) {
                auditImageFile(f);
            }
        }
    }

    private void auditImageFile(File f) {
        try {
            BufferedImage img = ImageIO.read(f);
            if (img == null) {
                System.err.println("[UNREADABLE] " + f.getPath());
                return;
            }
            int w = img.getWidth();
            int h = img.getHeight();
            int minR = 255, maxR = 0;
            int minG = 255, maxG = 0;
            int minB = 255, maxB = 0;
            boolean isGrayscale = true;
            Set<Integer> sampleColors = new HashSet<>();

            for (int y = 0; y < h; y += Math.max(1, h / 50)) {
                for (int x = 0; x < w; x += Math.max(1, w / 50)) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    minR = Math.min(minR, r); maxR = Math.max(maxR, r);
                    minG = Math.min(minG, g); maxG = Math.max(maxG, g);
                    minB = Math.min(minB, b); maxB = Math.max(maxB, b);
                    if (r != g || g != b) isGrayscale = false;
                    sampleColors.add(rgb & 0xFFFFFF);
                }
            }

            boolean isFlat = (minR == maxR && minG == maxG && minB == maxB);
            if (isFlat) {
                System.out.println("[FLAT/UNIFORM] " + f.getPath() + " (" + w + "x" + h + ") RGB=" + minR + "," + minG + "," + minB);
            } else if (f.getName().contains("biomes") && isGrayscale) {
                System.out.println("[BIOME-GRAYSCALE-WARNING] " + f.getPath() + " distinctColors=" + sampleColors.size() + " rangeR=[" + minR + ".." + maxR + "]");
            } else if (f.getName().contains("biomes")) {
                System.out.println("[BIOME-OK-COLOR] " + f.getPath() + " distinctColors=" + sampleColors.size() + " rangeR=[" + minR + ".." + maxR + "]");
            } else if (f.getName().contains("elevation")) {
                System.out.println("[ELEVATION-STATUS] " + f.getPath() + " isGrayscale=" + isGrayscale + " distinctElevations=" + sampleColors.size() + " rangeGray=[" + minR + ".." + maxR + "]");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] " + f.getPath() + ": " + e.getMessage());
        }
    }
}
