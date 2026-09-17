/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

/**
 * Advanced Mathematical Spatial Tensor & Map Comparison Engine.
 * Provides quantitative cartographic metrics to compare real-world historical spatial data
 * against simulation scenario states, or to compare multi-channel spatial tensors across scenario runs.
 *
 * Metrics implemented:
 * - Root Mean Square Error (RMSE)
 * - Pearson Spatial Cross-Correlation (r)
 * - Structural Similarity Index Measure (SSIM)
 * - Jaccard Index / Dice Coefficient (for categorical sovereignty & isogloss layers)
 * - Kullback-Leibler (KL) Spatial Divergence
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class MapComparisonMetrics {
    private static final Logger logger = LoggerFactory.getLogger(MapComparisonMetrics.class);

    public static class MapComparisonResult {
        private final double rmse;
        private final double pearsonR;
        private final double ssim;
        private final double jaccardIndex;
        private final double diceCoefficient;
        private final double klDivergence;
        private final double maxDeltaValue;
        private final double maxDeltaLng;
        private final double maxDeltaLat;
        private final String diagnosticSummary;

        public MapComparisonResult(double rmse, double pearsonR, double ssim, double jaccardIndex,
                                   double diceCoefficient, double klDivergence, double maxDeltaValue,
                                   double maxDeltaLng, double maxDeltaLat, String diagnosticSummary) {
            this.rmse = rmse;
            this.pearsonR = pearsonR;
            this.ssim = ssim;
            this.jaccardIndex = jaccardIndex;
            this.diceCoefficient = diceCoefficient;
            this.klDivergence = klDivergence;
            this.maxDeltaValue = maxDeltaValue;
            this.maxDeltaLng = maxDeltaLng;
            this.maxDeltaLat = maxDeltaLat;
            this.diagnosticSummary = diagnosticSummary;
        }

        public double getRmse() { return rmse; }
        public double getPearsonR() { return pearsonR; }
        public double getSsim() { return ssim; }
        public double getJaccardIndex() { return jaccardIndex; }
        public double getDiceCoefficient() { return diceCoefficient; }
        public double getKlDivergence() { return klDivergence; }
        public double getMaxDeltaValue() { return maxDeltaValue; }
        public double getMaxDeltaLng() { return maxDeltaLng; }
        public double getMaxDeltaLat() { return maxDeltaLat; }
        public String getDiagnosticSummary() { return diagnosticSummary; }

        public String getFormattedReport() {
            return String.format(
                "🗺️ SPATIAL TENSOR & CARTOGRAPHIC COMPARISON REPORT:\n" +
                "  - RMSE (Spatial Grid Error): %.4f\n" +
                "  - Pearson Correlation (r): %.4f\n" +
                "  - SSIM (Structural Similarity): %.4f\n" +
                "  - Categorical Jaccard Index: %.2f%%\n" +
                "  - Dice Overlap Coefficient: %.2f%%\n" +
                "  - Kullback-Leibler Divergence: %.4f nats\n" +
                "  - Maximum Spatial Divergence: Delta = %.2f at (Lat: %.2f°, Lng: %.2f°)\n" +
                "  - Diagnostic Assessment: %s",
                rmse, pearsonR, ssim, jaccardIndex * 100.0, diceCoefficient * 100.0,
                klDivergence, maxDeltaValue, maxDeltaLat, maxDeltaLng, diagnosticSummary
            );
        }
    }

    /**
     * Compares two multi-channel spatial images (e.g. Density, Sovereignty, or Isogloss rasters).
     */
    public static MapComparisonResult compareImages(BufferedImage imgA, BufferedImage imgB) {
        if (imgA == null || imgB == null) {
            throw new IllegalArgumentException("Input images must not be null.");
        }

        int width = Math.min(imgA.getWidth(), imgB.getWidth());
        int height = Math.min(imgA.getHeight(), imgB.getHeight());

        double[] gridA = extractLuminanceGrid(imgA, width, height);
        double[] gridB = extractLuminanceGrid(imgB, width, height);

        int[] catA = extractCategoryGrid(imgA, width, height);
        int[] catB = extractCategoryGrid(imgB, width, height);

        return compareGrids(gridA, gridB, catA, catB, width, height);
    }

    /**
     * Core mathematical comparison logic between two 1D array representations of 2D grids.
     */
    public static MapComparisonResult compareGrids(double[] gridA, double[] gridB, int[] catA, int[] catB, int width, int height) {
        int n = width * height;
        if (gridA.length < n || gridB.length < n) {
            throw new IllegalArgumentException("Grid array dimensions smaller than width * height.");
        }

        // 1. Root Mean Square Error (RMSE)
        double sumSqErr = 0.0;
        double maxDelta = 0.0;
        int maxDeltaIdx = 0;

        for (int i = 0; i < n; i++) {
            double diff = Math.abs(gridA[i] - gridB[i]);
            sumSqErr += diff * diff;
            if (diff > maxDelta) {
                maxDelta = diff;
                maxDeltaIdx = i;
            }
        }
        double rmse = Math.sqrt(sumSqErr / n);

        // Convert max delta index to Lat / Lng (Equirectangular projection mapping)
        int maxDeltaX = maxDeltaIdx % width;
        int maxDeltaY = maxDeltaIdx / width;
        double maxDeltaLng = -180.0 + ((double) maxDeltaX / width) * 360.0;
        double maxDeltaLat = 90.0 - ((double) maxDeltaY / height) * 180.0;

        // 2. Pearson Spatial Correlation (r)
        double meanA = calculateMean(gridA, n);
        double meanB = calculateMean(gridB, n);

        double num = 0.0;
        double denA = 0.0;
        double denB = 0.0;

        for (int i = 0; i < n; i++) {
            double dA = gridA[i] - meanA;
            double dB = gridB[i] - meanB;
            num += dA * dB;
            denA += dA * dA;
            denB += dB * dB;
        }

        double pearsonR = (denA > 0 && denB > 0) ? (num / (Math.sqrt(denA) * Math.sqrt(denB))) : 1.0;

        // 3. Structural Similarity Index (SSIM)
        double varA = denA / n;
        double varB = denB / n;
        double covAB = num / n;

        double c1 = 0.01 * 0.01 * 255.0 * 255.0;
        double c2 = 0.03 * 0.03 * 255.0 * 255.0;

        double ssim = ((2.0 * meanA * meanB + c1) * (2.0 * covAB + c2)) /
                      ((meanA * meanA + meanB * meanB + c1) * (varA + varB + c2));
        ssim = Math.max(-1.0, Math.min(1.0, ssim));

        // 4. Categorical Jaccard & Dice Coefficients
        double jaccardIndex = 1.0;
        double diceCoeff = 1.0;

        if (catA != null && catB != null && catA.length >= n && catB.length >= n) {
            int matchCount = 0;
            int unionCount = 0;
            for (int i = 0; i < n; i++) {
                boolean activeA = (catA[i] != 0 && catA[i] != 0xFF000000);
                boolean activeB = (catB[i] != 0 && catB[i] != 0xFF000000);

                if (activeA || activeB) {
                    unionCount++;
                    if (catA[i] == catB[i]) {
                        matchCount++;
                    }
                }
            }
            if (unionCount > 0) {
                jaccardIndex = (double) matchCount / unionCount;
                diceCoeff = (2.0 * matchCount) / (matchCount + unionCount);
            }
        }

        // 5. Kullback-Leibler (KL) Divergence
        double sumA = 0.0, sumB = 0.0;
        for (int i = 0; i < n; i++) {
            sumA += Math.max(1e-6, gridA[i]);
            sumB += Math.max(1e-6, gridB[i]);
        }

        double klDiv = 0.0;
        for (int i = 0; i < n; i++) {
            double p = Math.max(1e-6, gridA[i]) / sumA;
            double q = Math.max(1e-6, gridB[i]) / sumB;
            klDiv += p * Math.log(p / q);
        }

        // Diagnostic Assessment Summary
        String summary;
        if (rmse < 10.0 && pearsonR > 0.85 && ssim > 0.80) {
            summary = "EXCELLENT CONVERGENCE - Simulated map matches ground truth cartography with high fidelity.";
        } else if (pearsonR > 0.60 && ssim > 0.50) {
            summary = "MODERATE FIDELITY - General spatial structure is aligned, but localized density/sovereignty shifts exist.";
        } else {
            summary = "HIGH DIVERGENCE - Significant spatial distortion detected. Model parameter re-calibration recommended.";
        }

        return new MapComparisonResult(rmse, pearsonR, ssim, jaccardIndex, diceCoeff, klDiv, maxDelta, maxDeltaLng, maxDeltaLat, summary);
    }

    private static double[] extractLuminanceGrid(BufferedImage img, int width, int height) {
        double[] grid = new double[width * height];
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // Standard NTSC Grayscale Luminance Formula
                grid[idx++] = 0.299 * r + 0.587 * g + 0.114 * b;
            }
        }
        return grid;
    }

    private static int[] extractCategoryGrid(BufferedImage img, int width, int height) {
        int[] cat = new int[width * height];
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cat[idx++] = img.getRGB(x, y);
            }
        }
        return cat;
    }

    private static double calculateMean(double[] arr, int len) {
        double sum = 0.0;
        for (int i = 0; i < len; i++) {
            sum += arr[i];
        }
        return sum / len;
    }
}

