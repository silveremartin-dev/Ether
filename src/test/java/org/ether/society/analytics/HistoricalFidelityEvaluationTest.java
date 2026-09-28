/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.h3.H3Service;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Epistemic Historical Fidelity Evaluation Suite.
 * Quantifies the spatial cartographic fidelity, border precision (Jaccard IoU),
 * and demographic correlation ($R^2$, Pearson $r$) across discrete H3 resolutions
 * against empirical ground truth historical rasters (1800 AD Sovereign Polities & HYDE 3.4).
 */
public class HistoricalFidelityEvaluationTest {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalFidelityEvaluationTest.class);

    private static final String SOVEREIGNTY_PATH = "data/maps/ether/earth/1800/earth_1800_sovereignty.png";
    private static final String DENSITY_PATH = "data/maps/ether/earth/1800/earth_1800_density.png";

    // 18 Authentic Historical Entities of 1800 CE
    private static final String[] POLITY_NAMES = {
        "British Empire", "French Republic", "Russian Empire", "Qing Empire",
        "Habsburg Monarchy", "Kingdom of Prussia", "United States", "Ottoman Empire",
        "Spanish Empire", "Portuguese Empire", "Maratha Confederacy", "Tokugawa Shogunate",
        "Qajar Persia", "Durrani Empire", "Kingdom of Siam", "Sokoto Caliphate",
        "Ethiopian Empire", "Joseon Dynasty"
    };

    public record FidelityMetrics(
        int resolution,
        int cellCount,
        double meanCellEdgeKm,
        double sovereigntyMacroIoU,
        double demographicPearsonR,
        double demographicRMSE,
        double compositeFidelityScore
    ) {}

    @Test
    public void evaluateMultiResolutionHistoricalFidelity() throws Exception {
        File sovFile = new File(SOVEREIGNTY_PATH);
        File denFile = new File(DENSITY_PATH);

        if (!sovFile.exists() || !denFile.exists()) {
            logger.warn("Cartographic layers for 1800 AD not found on disk. Skipping test.");
            return;
        }

        BufferedImage imgSovereignty = ImageIO.read(sovFile);
        BufferedImage imgDensity = ImageIO.read(denFile);

        Assertions.assertNotNull(imgSovereignty);
        Assertions.assertNotNull(imgDensity);

        int imgW = imgSovereignty.getWidth();
        int imgH = imgSovereignty.getHeight();

        logger.info("=========================================================================================");
        logger.info("       EPISTEMIC HISTORICAL FIDELITY EVALUATION ACROSS H3 RESOLUTIONS (1800 AD)          ");
        logger.info("=========================================================================================");

        int[] resolutions = {1, 2, 3}; // Res 1 (842), Res 2 (5,882), Res 3 (41,162)
        Map<Integer, FidelityMetrics> results = new HashMap<>();

        for (int res : resolutions) {
            int totalCells = (int) (2 + 120 * Math.pow(7, res));
            double avgEdgeKm = getApproxEdgeLengthKm(res);

            // 1. Evaluate Discrete Sovereignty Border IoU
            // Sample continuous ground truth map at cell centers, then reconstruct raster & compute IoU
            int[] reconstructedSovereignty = new int[imgW * imgH];
            double[] reconstructedDensity = new double[imgW * imgH];

            // Build nearest cell lookup / Voronoi rasterization for evaluation
            // For efficiency across high resolutions, evaluate sample points across global grid
            int sampleStep = 4; // Sample every 4th pixel (512x256 test grid)
            int sampledW = imgW / sampleStep;
            int sampledH = imgH / sampleStep;

            int matchCount = 0;
            int unionCount = 0;

            double sumDiffSq = 0.0;
            double sumA = 0.0, sumB = 0.0;
            double sumA2 = 0.0, sumB2 = 0.0, sumAB = 0.0;
            int totalValidSamples = 0;

            for (int sy = 0; sy < sampledH; sy++) {
                int py = sy * sampleStep;
                double lat = 90.0 - ((double) py / imgH) * 180.0;

                for (int sx = 0; sx < sampledW; sx++) {
                    int px = sx * sampleStep;
                    double lon = -180.0 + ((double) px / imgW) * 360.0;

                    // Ground truth
                    int trueSov = imgSovereignty.getRGB(px, py);
                    int rgbDen = imgDensity.getRGB(px, py);
                    double trueDen = ((rgbDen >> 16) & 0xFF) / 255.0;

                    // Sample nearest H3 cell center
                    long h3Index = H3Service.getInstance().latLngToH3(lat, lon, res);
                    com.uber.h3core.util.LatLng center = H3Service.getInstance().cellToLatLng(h3Index);
                    
                    int sampledSov;
                    double sampledDen;
                    if (center != null) {
                        int cx = (int) Math.round(((center.lng + 180.0) / 360.0) * (imgW - 1));
                        int cy = (int) Math.round(((90.0 - center.lat) / 180.0) * (imgH - 1));
                        cx = Math.max(0, Math.min(imgW - 1, cx));
                        cy = Math.max(0, Math.min(imgH - 1, cy));
                        sampledSov = imgSovereignty.getRGB(cx, cy);
                        sampledDen = (((imgDensity.getRGB(cx, cy)) >> 16) & 0xFF) / 255.0;
                    } else {
                        sampledSov = trueSov;
                        sampledDen = trueDen;
                    }

                    // Sovereignty Jaccard / Overlap
                    boolean activeTrue = (trueSov != 0 && (trueSov & 0x00FFFFFF) != 0);
                    boolean activeSamp = (sampledSov != 0 && (sampledSov & 0x00FFFFFF) != 0);

                    if (activeTrue || activeSamp) {
                        unionCount++;
                        if (trueSov == sampledSov) {
                            matchCount++;
                        }
                    }

                    // Demographics Correlation
                    double diff = trueDen - sampledDen;
                    sumDiffSq += diff * diff;
                    sumA += trueDen;
                    sumB += sampledDen;
                    sumA2 += trueDen * trueDen;
                    sumB2 += sampledDen * sampledDen;
                    sumAB += trueDen * sampledDen;
                    totalValidSamples++;
                }
            }

            double jaccardIoU = unionCount > 0 ? ((double) matchCount / unionCount) : 1.0;
            double rmse = Math.sqrt(sumDiffSq / Math.max(1, totalValidSamples));

            double n = totalValidSamples;
            double num = sumAB - (sumA * sumB) / n;
            double den = Math.sqrt((sumA2 - (sumA * sumA) / n) * (sumB2 - (sumB * sumB) / n));
            double pearsonR = den > 0 ? (num / den) : 1.0;

            // Composite Historical Fidelity Formula:
            // F(r) = 0.50 * IoU_sov + 0.35 * Pearson_den + 0.15 * (1 - RMSE)
            double fidelityScore = 0.50 * jaccardIoU + 0.35 * Math.max(0, pearsonR) + 0.15 * Math.max(0, 1.0 - rmse);

            FidelityMetrics metric = new FidelityMetrics(res, totalCells, avgEdgeKm, jaccardIoU, pearsonR, rmse, fidelityScore);
            results.put(res, metric);

            logger.info(String.format(
                "| H3 Res %d | Cells: %6d | Avg Edge: %6.1f km | Sovereignty IoU: %6.2f%% | Density Pearson r: %.4f | RMSE: %.4f | Composite Fidelity F(r): %6.2f%% |",
                res, totalCells, avgEdgeKm, jaccardIoU * 100.0, pearsonR, rmse, fidelityScore * 100.0
            ));
        }

        logger.info("=========================================================================================");

        // Verify that fidelity strictly increases with resolution
        if (results.containsKey(1) && results.containsKey(2)) {
            Assertions.assertTrue(results.get(2).compositeFidelityScore() > results.get(1).compositeFidelityScore(),
                "Resolution 2 must have higher historical fidelity than Resolution 1");
        }
        if (results.containsKey(2) && results.containsKey(3)) {
            Assertions.assertTrue(results.get(3).compositeFidelityScore() > results.get(2).compositeFidelityScore(),
                "Resolution 3 must have higher historical fidelity than Resolution 2");
        }
    }

    private static double getApproxEdgeLengthKm(int res) {
        return switch (res) {
            case 0 -> 1107.7;
            case 1 -> 418.6;
            case 2 -> 158.2;
            case 3 -> 59.8;
            case 4 -> 22.6;
            case 5 -> 8.5;
            default -> 1.0;
        };
    }
}
