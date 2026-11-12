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
 * @version 1.0.0-beta.3
 */
public class MapComparisonMetrics {
    private static final Logger logger = LoggerFactory.getLogger(MapComparisonMetrics.class);

    public static class MapComparisonResult extends org.ether.society.analytics.MapComparisonResult {
        public MapComparisonResult(double rmse, double pearsonR, double ssim, double jaccardIndex,
                                   double diceCoefficient, double klDivergence, double maxDeltaValue,
                                   double maxDeltaLng, double maxDeltaLat, String diagnosticSummary) {
            super(rmse, pearsonR, ssim, jaccardIndex, diceCoefficient, klDivergence, maxDeltaValue, maxDeltaLng, maxDeltaLat, diagnosticSummary);
        }
    }

    /**
     * Compares an H3 discrete cellular mesh directly against an authentic empirical ground-truth raster
     * by evaluating density at cell centroids without introducing equirectangular rasterization discretization error.
     *
     * @param cells list of simulation cells with spatial coordinates and population/density values
     * @param groundTruthRaster authentic empirical reference raster (HYDE 3.4 / WorldClim)
     * @param minLat minimum latitude (-90 to 90)
     * @param maxLat maximum latitude (-90 to 90)
     * @param minLng minimum longitude (-180 to 180)
     * @param maxLng maximum longitude (-180 to 180)
     * @param windowed whether the evaluation is restricted to a regional bounding box
     * @return quantitative comparison result over the cellular domain
     */
    public static MapComparisonResult compareCellsToRaster(
            java.util.List<org.ether.society.database.H3Cell> cells, BufferedImage groundTruthRaster,
            double minLat, double maxLat, double minLng, double maxLng, boolean windowed) {
        return CellularMapComparisonMetrics.compareCellsToRaster(cells, groundTruthRaster, minLat, maxLat, minLng, maxLng, windowed);
    }

    /**
     * Compares an H3 discrete cellular mesh directly against an authentic empirical cultural/geophysical ground-truth raster.
     *
     * @param cells list of simulation cells with spatial coordinates and tensor values
     * @param groundTruthRaster authentic empirical reference raster
     * @param channelKey target cultural or physical channel key
     * @param minLat minimum latitude (-90 to 90)
     * @param maxLat maximum latitude (-90 to 90)
     * @param minLng minimum longitude (-180 to 180)
     * @param maxLng maximum longitude (-180 to 180)
     * @param windowed whether the evaluation is restricted to a regional bounding box
     * @return quantitative comparison result over the cellular domain
     */
    public static MapComparisonResult compareCellsToChannelRaster(
            java.util.List<org.ether.society.database.H3Cell> cells, BufferedImage groundTruthRaster, String channelKey,
            double minLat, double maxLat, double minLng, double maxLng, boolean windowed) {
        return CellularMapComparisonMetrics.compareCellsToChannelRaster(cells, groundTruthRaster, channelKey, minLat, maxLat, minLng, maxLng, windowed);
    }

    /**
     * Directly compares two H3 cellular meshes (e.g. Scenario A vs Scenario B) across physical channels,
     * computing direct Pearson spatial correlation, ecumene overlap (Dice/Jaccard), SSIM, and RMSE
     * without any intermediate 2D equirectangular rasterization.
     *
     * @param cellsA list of H3 cells for scenario A
     * @param cellsB list of H3 cells for scenario B
     * @param channelKey target physical channel name (e.g. "density", "temperature", "aquifer")
     * @return quantitative comparison result over the cellular mesh domain
     */
    public static MapComparisonResult compareCellsToCells(
            java.util.List<org.ether.society.database.H3Cell> cellsA,
            java.util.List<org.ether.society.database.H3Cell> cellsB,
            String channelKey) {
        return CellularMapComparisonMetrics.compareCellsToCells(cellsA, cellsB, channelKey);
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
     * Compares two multi-channel spatial images restricted to a specific geographic bounding box.
     * Essential for windowed/regional scenarios (e.g. Nile Valley, Mediterranean, Fertile Crescent)
     * to avoid false divergence against unsimulated regions.
     *
     * @param imgA simulated raster
     * @param imgB empirical ground truth raster
     * @param minLat minimum latitude (-90 to 90)
     * @param maxLat maximum latitude (-90 to 90)
     * @param minLng minimum longitude (-180 to 180)
     * @param maxLng maximum longitude (-180 to 180)
     * @return quantitative comparison result over the windowed sub-grid
     */
    public static MapComparisonResult compareImagesWindowed(
            BufferedImage imgA, BufferedImage imgB,
            double minLat, double maxLat, double minLng, double maxLng) {
        if (imgA == null || imgB == null) {
            throw new IllegalArgumentException("Input images must not be null.");
        }

        int width = Math.min(imgA.getWidth(), imgB.getWidth());
        int height = Math.min(imgA.getHeight(), imgB.getHeight());

        int minX = (int) Math.max(0, Math.floor(((minLng + 180.0) / 360.0) * width));
        int maxX = (int) Math.min(width, Math.ceil(((maxLng + 180.0) / 360.0) * width));
        int minY = (int) Math.max(0, Math.floor(((90.0 - maxLat) / 180.0) * height));
        int maxY = (int) Math.min(height, Math.ceil(((90.0 - minLat) / 180.0) * height));

        int subW = Math.max(1, maxX - minX);
        int subH = Math.max(1, maxY - minY);

        double[] gridA = extractLuminanceGridWindowed(imgA, minX, minY, subW, subH, width, height);
        double[] gridB = extractLuminanceGridWindowed(imgB, minX, minY, subW, subH, width, height);

        int[] catA = extractCategoryGridWindowed(imgA, minX, minY, subW, subH, width, height);
        int[] catB = extractCategoryGridWindowed(imgB, minX, minY, subW, subH, width, height);

        return compareGridsWindowed(gridA, gridB, catA, catB, subW, subH, minX, minY, width, height);
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

    /**
     * Mathematical comparison logic between two 1D array representations of a windowed sub-grid.
     */
    public static MapComparisonResult compareGridsWindowed(
            double[] gridA, double[] gridB, int[] catA, int[] catB,
            int subW, int subH, int minX, int minY, int fullW, int fullH) {
        int n = subW * subH;
        if (gridA.length < n || gridB.length < n) {
            throw new IllegalArgumentException("Grid array dimensions smaller than subW * subH.");
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

        // Convert max delta local index to global Lat / Lng
        int localX = maxDeltaIdx % subW;
        int localY = maxDeltaIdx / subW;
        int globalX = minX + localX;
        int globalY = minY + localY;
        double maxDeltaLng = -180.0 + ((double) globalX / fullW) * 360.0;
        double maxDeltaLat = 90.0 - ((double) globalY / fullH) * 180.0;

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
            summary = "EXCELLENT CONVERGENCE - Windowed simulated map matches ground truth cartography with high fidelity.";
        } else if (pearsonR > 0.60 && ssim > 0.50) {
            summary = "MODERATE FIDELITY - General regional structure is aligned, but localized shifts exist.";
        } else {
            summary = "HIGH DIVERGENCE - Significant regional distortion detected. Model parameter re-calibration recommended.";
        }

        return new MapComparisonResult(rmse, pearsonR, ssim, jaccardIndex, diceCoeff, klDiv, maxDelta, maxDeltaLng, maxDeltaLat, summary);
    }

    public static BufferedImage generateDiscrepancyHeatmap(BufferedImage imgA, BufferedImage imgB) {
        return MapComparisonRasterExtractor.generateDiscrepancyHeatmap(imgA, imgB);
    }

    public static double[] extractLuminanceGrid(BufferedImage img, int width, int height) {
        return MapComparisonRasterExtractor.extractLuminanceGrid(img, width, height);
    }

    public static int[] extractCategoryGrid(BufferedImage img, int width, int height) {
        return MapComparisonRasterExtractor.extractCategoryGrid(img, width, height);
    }

    public static double[] extractLuminanceGridWindowed(BufferedImage img, int minX, int minY, int subW, int subH, int fullW, int fullH) {
        return MapComparisonRasterExtractor.extractLuminanceGridWindowed(img, minX, minY, subW, subH, fullW, fullH);
    }

    public static int[] extractCategoryGridWindowed(BufferedImage img, int minX, int minY, int subW, int subH, int fullW, int fullH) {
        return MapComparisonRasterExtractor.extractCategoryGridWindowed(img, minX, minY, subW, subH, fullW, fullH);
    }

    private static double calculateMean(double[] arr, int len) {
        // Benchmark evaluation: Record metric snapshot and calculate residual variance
        // Compare simulated trajectories against empirical historical ground truth
        double sum = 0.0;
        for (int i = 0; i < len; i++) {
            sum += arr[i];
        }
        return sum / len;
    }
}

