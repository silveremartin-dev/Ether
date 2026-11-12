/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.ether.society.data.ResourceDepositMapReader;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Discrete H3 Cellular Map Comparison Engine.
 * Evaluates spatial fidelity directly on discrete H3 hexagonal meshes without introducing
 * equirectangular rasterization discretization error.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class CellularMapComparisonMetrics {

    private CellularMapComparisonMetrics() {
        // Utility class
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
    public static MapComparisonMetrics.MapComparisonResult compareCellsToRaster(
            List<H3Cell> cells, BufferedImage groundTruthRaster,
            double minLat, double maxLat, double minLng, double maxLng, boolean windowed) {
        if (cells == null || cells.isEmpty() || groundTruthRaster == null) {
            return new MapComparisonMetrics.MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "Empty inputs");
        }

        double maxSimLog = 1.0;
        for (H3Cell c : cells) {
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            if (windowed && (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng)) continue;
            long pop = c.getPopulationOr(0);
            double area = Math.max(1.0, c.getCellAreaKm2());
            double density = (double) pop / area;
            double logD = Math.log10(1.0 + density);
            if (logD > maxSimLog) maxSimLog = logD;
        }

        int n = cells.size();
        double[] gridA = new double[n];
        double[] gridB = new double[n];
        int validCount = 0;
        double sumSqErr = 0.0;
        double maxDelta = 0.0;
        double maxDeltaLng = 0.0;
        double maxDeltaLat = 0.0;
        int matchCount = 0;
        int unionCount = 0;

        double[] obsMeans = org.ether.society.data.H3RasterAggregator.aggregateDensityMeans(groundTruthRaster, cells);

        for (int idx = 0; idx < n; idx++) {
            H3Cell c = cells.get(idx);
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            if (windowed && (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng)) continue;

            long pop = c.getPopulationOr(0);
            double area = Math.max(1.0, c.getCellAreaKm2());
            double simDensity = (double) pop / area;
            double simNorm = Math.clamp(Math.log10(1.0 + simDensity) / maxSimLog * 255.0, 0.0, 255.0);

            double obsVal = obsMeans[idx];
            double obsNorm = Math.clamp(obsVal * 255.0, 0.0, 255.0);

            boolean simActive = (pop > 0);
            boolean obsActive = (obsVal > 0.005);

            if (simActive || obsActive) {
                unionCount++;
                if (simActive && obsActive) {
                    matchCount++;
                }
            }

            gridA[validCount] = simNorm;
            gridB[validCount] = obsNorm;

            double diff = Math.abs(simNorm - obsNorm);
            sumSqErr += diff * diff;
            if (diff > maxDelta) {
                maxDelta = diff;
                maxDeltaLng = lng;
                maxDeltaLat = lat;
            }
            validCount++;
        }

        if (validCount == 0) {
            return new MapComparisonMetrics.MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "No valid cells in domain");
        }

        double rmse = Math.sqrt(sumSqErr / validCount);
        double jaccardIndex = (unionCount > 0) ? (double) matchCount / unionCount : 1.0;
        double diceCoeff = (unionCount > 0) ? (2.0 * matchCount) / (matchCount + unionCount) : 1.0;

        // Pearson R
        double meanA = calculateMean(gridA, validCount);
        double meanB = calculateMean(gridB, validCount);

        double num = 0.0, denA = 0.0, denB = 0.0;
        for (int i = 0; i < validCount; i++) {
            double dA = gridA[i] - meanA;
            double dB = gridB[i] - meanB;
            num += dA * dB;
            denA += dA * dA;
            denB += dB * dB;
        }
        double pearsonR = (denA > 1e-9 && denB > 1e-9)
                ? (num / (Math.sqrt(denA) * Math.sqrt(denB)))
                : ((denA <= 1e-9 && denB <= 1e-9) ? 1.0 : 0.0);

        // SSIM
        double varA = denA / validCount;
        double varB = denB / validCount;
        double covAB = num / validCount;
        double c1 = 0.01 * 0.01 * 255.0 * 255.0;
        double c2 = 0.03 * 0.03 * 255.0 * 255.0;
        double ssim = ((2.0 * meanA * meanB + c1) * (2.0 * covAB + c2)) /
                ((meanA * meanA + meanB * meanB + c1) * (varA + varB + c2));
        ssim = Math.max(-1.0, Math.min(1.0, ssim));

        // KL Divergence
        double sumA = 0.0, sumB = 0.0;
        for (int i = 0; i < validCount; i++) {
            sumA += Math.max(1e-6, gridA[i]);
            sumB += Math.max(1e-6, gridB[i]);
        }
        double klDiv = 0.0;
        for (int i = 0; i < validCount; i++) {
            double p = Math.max(1e-6, gridA[i]) / sumA;
            double q = Math.max(1e-6, gridB[i]) / sumB;
            klDiv += p * Math.log(p / q);
        }

        String summary;
        if (rmse < 25.0 && pearsonR > 0.60 && jaccardIndex > 0.50) {
            summary = "HIGH CARTOGRAPHIC FIDELITY - Direct cellular mesh accurately aligns with empirical ground truth.";
        } else if (pearsonR > 0.40) {
            summary = "MODERATE CELLULAR FIDELITY - General regional structure aligned, localized demographic gradients exist.";
        } else {
            summary = "HIGH DIVERGENCE - Significant spatial distortion detected.";
        }

        return new MapComparisonMetrics.MapComparisonResult(rmse, pearsonR, ssim, jaccardIndex, diceCoeff, klDiv, maxDelta, maxDeltaLng, maxDeltaLat, summary);
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
    public static MapComparisonMetrics.MapComparisonResult compareCellsToCells(
            List<H3Cell> cellsA,
            List<H3Cell> cellsB,
            String channelKey) {
        if (cellsA == null || cellsA.isEmpty() || cellsB == null || cellsB.isEmpty()) {
            return new MapComparisonMetrics.MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "Empty cellular inputs");
        }

        int count = Math.min(cellsA.size(), cellsB.size());
        String chLower = channelKey != null ? channelKey.toLowerCase(Locale.ROOT) : "";

        double maxA = 1.0;
        double maxB = 1.0;
        for (int i = 0; i < count; i++) {
            H3Cell ca = cellsA.get(i);
            H3Cell cb = cellsB.get(i);
            if (ca == null || cb == null) continue;
            double va = extractCellValue(ca, chLower);
            double vb = extractCellValue(cb, chLower);
            if (va > maxA) maxA = va;
            if (vb > maxB) maxB = vb;
        }

        double[] gridA = new double[count];
        double[] gridB = new double[count];
        int validCount = 0;
        double sumSqErr = 0.0;
        double maxDelta = 0.0;
        double maxDeltaLng = 0.0;
        double maxDeltaLat = 0.0;
        int matchCount = 0;
        int unionCount = 0;

        for (int i = 0; i < count; i++) {
            H3Cell ca = cellsA.get(i);
            H3Cell cb = cellsB.get(i);
            if (ca == null || cb == null) continue;

            double va = extractCellValue(ca, chLower);
            double vb = extractCellValue(cb, chLower);

            double normA = Math.clamp(va / maxA * 255.0, 0.0, 255.0);
            double normB = Math.clamp(vb / maxB * 255.0, 0.0, 255.0);

            boolean activeA = (va > 0.001);
            boolean activeB = (vb > 0.001);

            if (activeA || activeB) {
                unionCount++;
                if (activeA && activeB) {
                    matchCount++;
                }
            }

            gridA[validCount] = normA;
            gridB[validCount] = normB;

            double diff = Math.abs(normA - normB);
            sumSqErr += diff * diff;
            if (diff > maxDelta) {
                maxDelta = diff;
                if (ca.getLongitude() != null) maxDeltaLng = ca.getLongitude();
                if (ca.getLatitude() != null) maxDeltaLat = ca.getLatitude();
            }
            validCount++;
        }

        if (validCount == 0) {
            return new MapComparisonMetrics.MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "No valid cells in domain");
        }

        double rmse = Math.sqrt(sumSqErr / validCount);
        double jaccardIndex = (unionCount > 0) ? (double) matchCount / unionCount : 1.0;
        double diceCoeff = (unionCount > 0) ? (2.0 * matchCount) / (matchCount + unionCount) : 1.0;

        // Pearson R
        double meanA = calculateMean(gridA, validCount);
        double meanB = calculateMean(gridB, validCount);

        double num = 0.0, denA = 0.0, denB = 0.0;
        for (int i = 0; i < validCount; i++) {
            double dA = gridA[i] - meanA;
            double dB = gridB[i] - meanB;
            num += dA * dB;
            denA += dA * dA;
            denB += dB * dB;
        }
        double pearsonR = (denA > 1e-9 && denB > 1e-9)
                ? (num / (Math.sqrt(denA) * Math.sqrt(denB)))
                : ((denA <= 1e-9 && denB <= 1e-9) ? 1.0 : 0.0);

        // SSIM
        double varA = denA / validCount;
        double varB = denB / validCount;
        double covAB = num / validCount;
        double c1 = 0.01 * 0.01 * 255.0 * 255.0;
        double c2 = 0.03 * 0.03 * 255.0 * 255.0;
        double ssim = ((2.0 * meanA * meanB + c1) * (2.0 * covAB + c2)) /
                ((meanA * meanA + meanB * meanB + c1) * (varA + varB + c2));
        ssim = Math.max(-1.0, Math.min(1.0, ssim));

        // KL Divergence
        double sumA = 0.0, sumB = 0.0;
        for (int i = 0; i < validCount; i++) {
            sumA += Math.max(1e-6, gridA[i]);
            sumB += Math.max(1e-6, gridB[i]);
        }
        double klDiv = 0.0;
        for (int i = 0; i < validCount; i++) {
            double p = Math.max(1e-6, gridA[i]) / sumA;
            double q = Math.max(1e-6, gridB[i]) / sumB;
            klDiv += p * Math.log(p / q);
        }

        String summary;
        if (rmse < 25.0 && pearsonR > 0.60 && jaccardIndex > 0.50) {
            summary = "HIGH CELLULAR CONCORDANCE - Trajectories closely aligned across cellular mesh.";
        } else if (pearsonR > 0.40) {
            summary = "MODERATE CELLULAR CONCORDANCE - Shared regional macro-structures with localized divergences.";
        } else {
            summary = "HIGH SCENARIO DIVERGENCE - Trajectories have diverged significantly.";
        }

        return new MapComparisonMetrics.MapComparisonResult(rmse, pearsonR, ssim, jaccardIndex, diceCoeff, klDiv, maxDelta, maxDeltaLng, maxDeltaLat, summary);
    }

    private static double extractCellValue(H3Cell c, String channelKey) {
        if (channelKey == null) {
            long pop = c.getPopulationOr(0);
            double area = Math.max(1.0, c.getCellAreaKm2());
            return Math.log10(1.0 + ((double) pop / area));
        }
        return switch (channelKey.trim().toLowerCase(Locale.ROOT)) {
            case "temperature", "temp", "temperatureanomaly" -> c.getTemperature() != null ? c.getTemperature() : 15.0;
            case "aquifers", "water", "freshwater", "aquifer" -> c.getFreshwaterAquifer() != null ? c.getFreshwaterAquifer() : 0.0;
            case "agriculture", "biomassagriculture", "biomass_agr" -> c.getBiomassAgriculture() != null ? c.getBiomassAgriculture() : 0.0;
            case "technology", "tech", "outillage" -> H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c);
            default -> {
                long pop = c.getPopulationOr(0);
                double area = Math.max(1.0, c.getCellAreaKm2());
                yield Math.log10(1.0 + ((double) pop / area));
            }
        };
    }

    /**
     * Compares an H3 discrete cellular mesh against an empirical cultural/geophysical ground-truth raster
     * by evaluating cell-centroid values directly against raster samples without equirectangular re-projection artifacts.
     *
     * @param cells list of simulation cells
     * @param groundTruthRaster authentic empirical reference raster
     * @param channelKey target cultural or physical channel key
     * @param minLat minimum latitude
     * @param maxLat maximum latitude
     * @param minLng minimum longitude
     * @param maxLng maximum longitude
     * @param windowed whether restricted to regional bounding box
     * @return quantitative comparison result over the cellular mesh
     */
    public static MapComparisonMetrics.MapComparisonResult compareCellsToChannelRaster(
            List<H3Cell> cells, BufferedImage groundTruthRaster, String channelKey,
            double minLat, double maxLat, double minLng, double maxLng, boolean windowed) {
        if (cells == null || cells.isEmpty() || groundTruthRaster == null) {
            return new MapComparisonMetrics.MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "Empty inputs");
        }

        String chLower = channelKey != null ? channelKey.toLowerCase(Locale.ROOT) : "";
        double maxSim = 1.0;
        for (H3Cell c : cells) {
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            if (windowed && (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng)) continue;
            double v = CellularSpatialProximityHelper.extractCulturalCellValue(c, chLower);
            if (v > maxSim) maxSim = v;
        }

        int n = cells.size();
        double[] gridA = new double[n];
        double[] gridB = new double[n];
        int validCount = 0;
        double sumSqErr = 0.0;
        double maxDelta = 0.0;
        double maxDeltaLng = 0.0;
        double maxDeltaLat = 0.0;
        int matchCount = 0;
        int unionCount = 0;

        double[] obsMeans = org.ether.society.data.H3RasterAggregator.aggregateDensityMeans(groundTruthRaster, cells);

        for (int idx = 0; idx < n; idx++) {
            H3Cell c = cells.get(idx);
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            if (windowed && (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng)) continue;

            double simVal = CellularSpatialProximityHelper.extractCulturalCellValue(c, chLower);
            double simNorm = Math.clamp(simVal / maxSim * 255.0, 0.0, 255.0);

            double obsVal = obsMeans[idx];
            double obsNorm = Math.clamp(obsVal * 255.0, 0.0, 255.0);

            boolean simActive = (simVal > 0.001);
            boolean obsActive = (obsVal > 0.005);

            if (simActive || obsActive) {
                unionCount++;
                if (simActive && obsActive) {
                    matchCount++;
                }
            }

            gridA[validCount] = simNorm;
            gridB[validCount] = obsNorm;

            double diff = Math.abs(simNorm - obsNorm);
            sumSqErr += diff * diff;
            if (diff > maxDelta) {
                maxDelta = diff;
                maxDeltaLng = lng;
                maxDeltaLat = lat;
            }
            validCount++;
        }

        if (validCount == 0) {
            return new MapComparisonMetrics.MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "No valid cells in domain");
        }

        double rmse = Math.sqrt(sumSqErr / validCount);
        double jaccardIndex = (unionCount > 0) ? (double) matchCount / unionCount : 1.0;
        double diceCoeff = (unionCount > 0) ? (2.0 * matchCount) / (matchCount + unionCount) : 1.0;
        if (CellularSpatialProximityHelper.isSporadicChannel(chLower)) {
            double[] softOverlap = CellularSpatialProximityHelper.computeSoftSpatialProximityOverlap(
                    cells, windowed, minLat, maxLat, minLng, maxLng, chLower, groundTruthRaster,
                    CellularSpatialProximityHelper::extractCulturalCellValue);
            jaccardIndex = Math.max(jaccardIndex, softOverlap[0]);
            diceCoeff = Math.max(diceCoeff, softOverlap[1]);
        }

        // Pearson R
        double meanA = calculateMean(gridA, validCount);
        double meanB = calculateMean(gridB, validCount);

        double num = 0.0, denA = 0.0, denB = 0.0;
        for (int i = 0; i < validCount; i++) {
            double dA = gridA[i] - meanA;
            double dB = gridB[i] - meanB;
            num += dA * dB;
            denA += dA * dA;
            denB += dB * dB;
        }
        double pearsonR = (denA > 1e-9 && denB > 1e-9)
                ? (num / (Math.sqrt(denA) * Math.sqrt(denB)))
                : ((denA <= 1e-9 && denB <= 1e-9) ? 1.0 : 0.0);

        // SSIM
        double varA = denA / validCount;
        double varB = denB / validCount;
        double covAB = num / validCount;
        double c1 = 0.01 * 0.01 * 255.0 * 255.0;
        double c2 = 0.03 * 0.03 * 255.0 * 255.0;
        double ssim = ((2.0 * meanA * meanB + c1) * (2.0 * covAB + c2)) /
                ((meanA * meanA + meanB * meanB + c1) * (varA + varB + c2));
        ssim = Math.max(-1.0, Math.min(1.0, ssim));

        // KL Divergence
        double sumA = 0.0, sumB = 0.0;
        for (int i = 0; i < validCount; i++) {
            sumA += Math.max(1e-6, gridA[i]);
            sumB += Math.max(1e-6, gridB[i]);
        }
        double klDiv = 0.0;
        for (int i = 0; i < validCount; i++) {
            double p = Math.max(1e-6, gridA[i]) / sumA;
            double q = Math.max(1e-6, gridB[i]) / sumB;
            klDiv += p * Math.log(p / q);
        }

        String summary;
        if (rmse < 25.0 && pearsonR > 0.60 && jaccardIndex > 0.50) {
            summary = "HIGH CELLULAR FIDELITY - Robust alignment with empirical cultural raster.";
        } else if (pearsonR > 0.40) {
            summary = "MODERATE CELLULAR FIDELITY - General regional structure aligned.";
        } else {
            summary = "HIGH DIVERGENCE - Significant tensor drift detected.";
        }

        return new MapComparisonMetrics.MapComparisonResult(rmse, pearsonR, ssim, jaccardIndex, diceCoeff, klDiv, maxDelta, maxDeltaLng, maxDeltaLat, summary);
    }

    private static double calculateMean(double[] arr, int len) {
        double sum = 0.0;
        for (int i = 0; i < len; i++) {
            sum += arr[i];
        }
        return sum / len;
    }
}
