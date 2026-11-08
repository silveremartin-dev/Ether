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
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

        for (H3Cell c : cells) {
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            if (windowed && (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng)) continue;

            long pop = c.getPopulationOr(0);
            double area = Math.max(1.0, c.getCellAreaKm2());
            double simDensity = (double) pop / area;
            double simNorm = Math.clamp(Math.log10(1.0 + simDensity) / maxSimLog * 255.0, 0.0, 255.0);

            double obsVal = ResourceDepositMapReader.sampleDensityFromImageTensor(groundTruthRaster, lat, lng);
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

    private static double extractCellValue(H3Cell c, String chLower) {
        if (chLower.contains("temp") || chLower.contains("climat")) {
            return c.getTemperature() != null ? c.getTemperature() : 15.0;
        } else if (chLower.contains("aquif") || chLower.contains("eau") || chLower.contains("water")) {
            return c.getFreshwaterAquifer() != null ? c.getFreshwaterAquifer() : 0.0;
        } else if (chLower.contains("biomass") || chLower.contains("agricol") || chLower.contains("agr")) {
            return c.getBiomassAgriculture() != null ? c.getBiomassAgriculture() : 0.0;
        } else if (chLower.contains("tech") || chLower.contains("outill")) {
            return H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c);
        } else {
            long pop = c.getPopulationOr(0);
            double area = Math.max(1.0, c.getCellAreaKm2());
            return Math.log10(1.0 + ((double) pop / area));
        }
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
            double v = extractCulturalCellValue(c, chLower);
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

        for (H3Cell c : cells) {
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            if (windowed && (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng)) continue;

            double simVal = extractCulturalCellValue(c, chLower);
            double simNorm = Math.clamp(simVal / maxSim * 255.0, 0.0, 255.0);

            double obsVal = ResourceDepositMapReader.sampleDensityFromImageTensor(groundTruthRaster, lat, lng);
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
        if (isSporadicChannel(chLower)) {
            double[] softOverlap = computeSoftSpatialProximityOverlap(
                    cells, windowed, minLat, maxLat, minLng, maxLng, chLower, groundTruthRaster);
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

    private static double extractCulturalCellValue(H3Cell c, String chLower) {
        long pop = c.getPopulationOr(0);
        if (chLower.contains("tech") || chLower.contains("material")) {
            return H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c);
        } else if (chLower.contains("institut") || chLower.contains("admin")) {
            double kpc = (c.getResourceCapital() != null && pop > 0) ? (c.getResourceCapital() / pop) : 0.0;
            return (Math.log10(Math.max(1.0, pop)) / 6.0) * 0.6 + (Math.min(1.0, kpc / 1500.0) * 0.4);
        } else if (chLower.contains("ecolog") || chLower.contains("carri")) {
            return c.getSoilOrganicCarbonOr(25.0);
        } else if (chLower.contains("pathog") || chLower.contains("sanit")) {
            double area = Math.max(1.0, c.getCellAreaKm2());
            double endemicUrbanLoad = pop > 0 ? Math.log10(1.0 + ((double) pop / area)) * 0.35 : 0.0;
            return (double) c.getEpidemicRecoveredOr(0) + (double) c.getEpidemicInfectedOr(0) + endemicUrbanLoad;
        } else if (chLower.contains("trade") || chLower.contains("market")) {
            double area = Math.max(1.0, c.getCellAreaKm2());
            double explicitTrade = c.getTradeNetworkOr(0.0) + c.getChokePointScoreOr(0.0);
            double settlementMarket = pop > 0 ? Math.log10(1.0 + ((double) pop / area)) * 0.45 : 0.0;
            return explicitTrade + settlementMarket;
        } else {
            // Categorical dimensions (isogloss, kinship, rituals, sovereignty): active demographic presence
            double area = Math.max(1.0, c.getCellAreaKm2());
            return Math.log10(1.0 + ((double) pop / area));
        }
    }

    private static double calculateMean(double[] arr, int len) {
        double sum = 0.0;
        for (int i = 0; i < len; i++) {
            sum += arr[i];
        }
        return sum / len;
    }

    static boolean isSporadicChannel(String chLower) {
        if (chLower == null) return false;
        return chLower.contains("lazaret")
                || chLower.contains("quarantine")
                || chLower.contains("comptoir")
                || chLower.contains("outpost")
                || chLower.contains("trade_post")
                || chLower.contains("trading_post")
                || chLower.contains("tradenetwork")
                || chLower.contains("pathogen")
                || chLower.contains("sanit")
                || chLower.contains("mine")
                || chLower.contains("sporadic");
    }

    /**
     * Evaluates continuous Wasserstein-regularized Gaussian proximity overlap for sporadic,
     * point-like geographical rasters (trading posts, lazarets, quarantine outposts).
     *
     * @return 2-element array containing [jaccardSoft, diceSoft]
     */
    static double[] computeSoftSpatialProximityOverlap(
            List<H3Cell> cells, boolean windowed, double minLat, double maxLat,
            double minLng, double maxLng, String chLower, BufferedImage groundTruthRaster) {
        if (cells == null || cells.isEmpty() || groundTruthRaster == null) {
            return new double[]{1.0, 1.0};
        }

        record Point(double lat, double lng, double weight) {}
        List<Point> simPoints = new ArrayList<>();
        List<Point> obsPoints = new ArrayList<>();

        for (H3Cell c : cells) {
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            if (windowed && (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng)) continue;

            double simVal = extractCulturalCellValue(c, chLower);
            if (simVal > 0.001) {
                simPoints.add(new Point(lat, lng, Math.min(1.0, simVal)));
            }

            double obsVal = ResourceDepositMapReader.sampleDensityFromImageTensor(groundTruthRaster, lat, lng);
            if (obsVal > 0.005) {
                obsPoints.add(new Point(lat, lng, Math.min(1.0, obsVal)));
            }
        }

        if (simPoints.isEmpty() && obsPoints.isEmpty()) {
            return new double[]{1.0, 1.0};
        }
        if (simPoints.isEmpty() || obsPoints.isEmpty()) {
            return new double[]{0.0, 0.0};
        }

        // Gaussian kernel parameters: sigma = 75 km (typical natural harbor / regional district catchment)
        double sigmaKm = 75.0;
        double twoSigmaSq = 2.0 * sigmaKm * sigmaKm;
        double maxDistKm = 2.5 * sigmaKm; // 187.5 km cutoff
        double bucketSizeDeg = 2.0; // ~220 km spatial hash bucket (guarantees all <= 187.5 km pairs reside in 3x3 neighbor buckets)

        Map<Long, List<Point>> simGrid = new HashMap<>();
        for (Point sp : simPoints) {
            long bx = (long) Math.floor((sp.lat + 90.0) / bucketSizeDeg);
            long by = (long) Math.floor((sp.lng + 180.0) / bucketSizeDeg);
            simGrid.computeIfAbsent((bx << 32) | (by & 0xFFFFFFFFL), k -> new ArrayList<>()).add(sp);
        }

        Map<Long, List<Point>> obsGrid = new HashMap<>();
        for (Point op : obsPoints) {
            long bx = (long) Math.floor((op.lat + 90.0) / bucketSizeDeg);
            long by = (long) Math.floor((op.lng + 180.0) / bucketSizeDeg);
            obsGrid.computeIfAbsent((bx << 32) | (by & 0xFFFFFFFFL), k -> new ArrayList<>()).add(op);
        }

        double sumIntersection = 0.0;
        double sumUnion = 0.0;

        for (H3Cell c : cells) {
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            if (windowed && (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng)) continue;

            long cBx = (long) Math.floor((lat + 90.0) / bucketSizeDeg);
            long cBy = (long) Math.floor((lng + 180.0) / bucketSizeDeg);

            double simField = 0.0;
            double obsField = 0.0;

            for (long dx = -1; dx <= 1; dx++) {
                long nx = cBx + dx;
                for (long dy = -1; dy <= 1; dy++) {
                    long ny = (cBy + dy + 180) % 180;
                    long key = (nx << 32) | ((cBy + dy) & 0xFFFFFFFFL);

                    List<Point> sBucket = simGrid.get(key);
                    if (sBucket != null) {
                        for (Point sp : sBucket) {
                            if (Math.abs(lat - sp.lat) > 1.8) continue;
                            double d = org.ether.society.engines.tier1.ThermodynamicMigrationEngine.calculateHaversineDistance(lat, lng, sp.lat, sp.lng);
                            if (d <= maxDistKm) {
                                simField += sp.weight * Math.exp(-(d * d) / twoSigmaSq);
                            }
                        }
                    }

                    List<Point> oBucket = obsGrid.get(key);
                    if (oBucket != null) {
                        for (Point op : oBucket) {
                            if (Math.abs(lat - op.lat) > 1.8) continue;
                            double d = org.ether.society.engines.tier1.ThermodynamicMigrationEngine.calculateHaversineDistance(lat, lng, op.lat, op.lng);
                            if (d <= maxDistKm) {
                                obsField += op.weight * Math.exp(-(d * d) / twoSigmaSq);
                            }
                        }
                    }
                }
            }

            if (simField > 1e-4 || obsField > 1e-4) {
                sumIntersection += Math.min(simField, obsField);
                sumUnion += Math.max(simField, obsField);
            }
        }

        if (sumUnion <= 1e-6) {
            return new double[]{1.0, 1.0};
        }
        double jaccard = Math.clamp(sumIntersection / sumUnion, 0.0, 1.0);
        double dice = Math.clamp((2.0 * sumIntersection) / (sumIntersection + sumUnion), 0.0, 1.0);
        return new double[]{jaccard, dice};
    }
}
