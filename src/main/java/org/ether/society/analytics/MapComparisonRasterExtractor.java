/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.awt.image.BufferedImage;

/**
 * Cartographic raster grid extraction and discrepancy heatmap visualization utility.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class MapComparisonRasterExtractor {

    private MapComparisonRasterExtractor() {
    }

    public static double[] extractLuminanceGridWindowed(
            BufferedImage img, int minX, int minY, int subW, int subH, int fullW, int fullH) {
        double[] grid = new double[subW * subH];
        int imgW = img.getWidth();
        int imgH = img.getHeight();
        int idx = 0;
        for (int y = 0; y < subH; y++) {
            int globalY = minY + y;
            int srcY = (int) Math.min(imgH - 1, ((long) globalY * imgH) / fullH);
            for (int x = 0; x < subW; x++) {
                int globalX = minX + x;
                int srcX = (int) Math.min(imgW - 1, ((long) globalX * imgW) / fullW);
                int rgb = img.getRGB(srcX, srcY);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                grid[idx++] = 0.299 * r + 0.587 * g + 0.114 * b;
            }
        }
        return grid;
    }

    public static int[] extractCategoryGridWindowed(
            BufferedImage img, int minX, int minY, int subW, int subH, int fullW, int fullH) {
        int[] cat = new int[subW * subH];
        int imgW = img.getWidth();
        int imgH = img.getHeight();
        int idx = 0;
        for (int y = 0; y < subH; y++) {
            int globalY = minY + y;
            int srcY = (int) Math.min(imgH - 1, ((long) globalY * imgH) / fullH);
            for (int x = 0; x < subW; x++) {
                int globalX = minX + x;
                int srcX = (int) Math.min(imgW - 1, ((long) globalX * imgW) / fullW);
                int rgb = img.getRGB(srcX, srcY);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (r == g && g == b) {
                    cat[idx++] = (r >= 2) ? 1 : 0;
                } else {
                    cat[idx++] = rgb;
                }
            }
        }
        return cat;
    }

    public static double[] extractLuminanceGrid(BufferedImage img, int width, int height) {
        double[] grid = new double[width * height];
        int imgW = img.getWidth();
        int imgH = img.getHeight();
        int idx = 0;
        for (int y = 0; y < height; y++) {
            int srcY = (int) Math.min(imgH - 1, ((long) y * imgH) / height);
            for (int x = 0; x < width; x++) {
                int srcX = (int) Math.min(imgW - 1, ((long) x * imgW) / width);
                int rgb = img.getRGB(srcX, srcY);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // Standard NTSC Grayscale Luminance Formula
                grid[idx++] = 0.299 * r + 0.587 * g + 0.114 * b;
            }
        }
        return grid;
    }

    public static int[] extractCategoryGrid(BufferedImage img, int width, int height) {
        int[] cat = new int[width * height];
        int imgW = img.getWidth();
        int imgH = img.getHeight();
        int idx = 0;
        for (int y = 0; y < height; y++) {
            int srcY = (int) Math.min(imgH - 1, ((long) y * imgH) / height);
            for (int x = 0; x < width; x++) {
                int srcX = (int) Math.min(imgW - 1, ((long) x * imgW) / width);
                int rgb = img.getRGB(srcX, srcY);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (r == g && g == b) {
                    cat[idx++] = (r >= 2) ? 1 : 0;
                } else {
                    cat[idx++] = rgb;
                }
            }
        }
        return cat;
    }

    /**
     * Generates a 2D Discrepancy Heatmap highlighting spatial divergences between Map A and Map B.
     * Color Legend:
     * - Dark Slate: Neutral / Unpopulated matching areas
     * - Bright Emerald / Cyan: High spatial agreement
     * - Intense Amber / Red: Overestimation (A > B)
     * - Intense Indigo / Blue: Underestimation (A < B)
     */
    public static BufferedImage generateDiscrepancyHeatmap(BufferedImage imgA, BufferedImage imgB) {
        if (imgA == null || imgB == null) return null;

        int width = Math.min(imgA.getWidth(), imgB.getWidth());
        int height = Math.min(imgA.getHeight(), imgB.getHeight());

        BufferedImage diffMap = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        double[] gridA = extractLuminanceGrid(imgA, width, height);
        double[] gridB = extractLuminanceGrid(imgB, width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                double valA = gridA[idx];
                double valB = gridB[idx];
                double delta = valA - valB; // Range -255 to +255

                int rgba;
                if (Math.abs(valA) < 5.0 && Math.abs(valB) < 5.0) {
                    // Ocean / Empty Cell: Dark Slate
                    rgba = 0xFF0F172A;
                } else if (Math.abs(delta) <= 4.0) {
                    // Exact or near-zero discrepancy (A == B): Neutral baseline Slate Gray (no divergence)
                    rgba = 0xFF1E293B;
                } else if (Math.abs(delta) <= 12.0) {
                    // Subtle alignment: Soft muted slate-emerald
                    rgba = 0xFF064E3B;
                } else if (delta > 0) {
                    // A is greater than B (Overestimation): Amber to Red gradient
                    double norm = Math.min(1.0, (delta - 12.0) / 168.0);
                    int r = (int) (245 + norm * 10);
                    int g = (int) (158 - norm * 110);
                    int b = (int) (11 - norm * 11);
                    rgba = (0xFF << 24) | (Math.min(255, r) << 16) | (Math.max(0, g) << 8) | Math.max(0, b);
                } else {
                    // A is less than B (Underestimation): Cyan to Royal Blue gradient
                    double norm = Math.min(1.0, (-delta - 12.0) / 168.0);
                    int r = (int) (56 - norm * 40);
                    int g = (int) (189 - norm * 120);
                    int b = (int) (248 + norm * 7);
                    rgba = (0xFF << 24) | (Math.max(0, r) << 16) | (Math.max(0, g) << 8) | Math.min(255, b);
                }
                diffMap.setRGB(x, y, rgba);
            }
        }
        return diffMap;
    }
}
