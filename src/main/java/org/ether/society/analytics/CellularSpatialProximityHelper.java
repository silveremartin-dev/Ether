/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.ToDoubleBiFunction;

/**
 * <h1>Cellular Soft Spatial Proximity Evaluator</h1>
 *
 * <p>Evaluates continuous Wasserstein-regularized Gaussian proximity overlap for sporadic,
 * point-like geographical rasters (trading posts, lazarets, quarantine outposts).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 * @see CellularMapComparisonMetrics
 */
final class CellularSpatialProximityHelper {

    private CellularSpatialProximityHelper() {
        // Pure stateless evaluator
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
            double minLng, double maxLng, String chLower, BufferedImage groundTruthRaster,
            ToDoubleBiFunction<H3Cell, String> cellValueExtractor) {
        if (cells == null || cells.isEmpty() || groundTruthRaster == null) {
            return new double[]{1.0, 1.0};
        }

        record Point(double lat, double lng, double weight) {}
        List<Point> simPoints = new ArrayList<>();
        List<Point> obsPoints = new ArrayList<>();

        double[] obsMeans = org.ether.society.data.H3RasterAggregator.aggregateDensityMeans(groundTruthRaster, cells);
        for (int idx = 0; idx < cells.size(); idx++) {
            H3Cell c = cells.get(idx);
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            if (windowed && (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng)) continue;

            double simVal = cellValueExtractor.applyAsDouble(c, chLower);
            if (simVal > 0.001) {
                simPoints.add(new Point(lat, lng, Math.min(1.0, simVal)));
            }

            double obsVal = obsMeans[idx];
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

        // Precompute spatial bucket footprints to prune cells that have zero proximity to any simulated or observed points
        Set<Long> activeQueryBuckets = new HashSet<>();
        for (Long k : simGrid.keySet()) {
            long bx = (k >> 32);
            long by = (k & 0xFFFFFFFFL);
            for (long dx = -1; dx <= 1; dx++) {
                for (long dy = -1; dy <= 1; dy++) {
                    activeQueryBuckets.add(((bx + dx) << 32) | ((by + dy) & 0xFFFFFFFFL));
                }
            }
        }
        for (Long k : obsGrid.keySet()) {
            long bx = (k >> 32);
            long by = (k & 0xFFFFFFFFL);
            for (long dx = -1; dx <= 1; dx++) {
                for (long dy = -1; dy <= 1; dy++) {
                    activeQueryBuckets.add(((bx + dx) << 32) | ((by + dy) & 0xFFFFFFFFL));
                }
            }
        }

        double maxDistKmSq = maxDistKm * maxDistKm;
        double sumIntersection = 0.0;
        double sumUnion = 0.0;

        for (H3Cell c : cells) {
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            if (windowed && (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng)) continue;

            long cBx = (long) Math.floor((lat + 90.0) / bucketSizeDeg);
            long cBy = (long) Math.floor((lng + 180.0) / bucketSizeDeg);
            long cellBucketKey = (cBx << 32) | (cBy & 0xFFFFFFFFL);

            // If the cell's bucket has no overlap with active query footprint, both simField and obsField are strictly 0.0
            if (!activeQueryBuckets.contains(cellBucketKey)) {
                continue;
            }

            double cosLat = Math.cos(Math.toRadians(lat));
            double degToKmLat = 111.139;
            double degToKmLng = 111.139 * cosLat;

            double simField = 0.0;
            double obsField = 0.0;

            for (long dx = -1; dx <= 1; dx++) {
                long nx = cBx + dx;
                for (long dy = -1; dy <= 1; dy++) {
                    long key = (nx << 32) | ((cBy + dy) & 0xFFFFFFFFL);

                    List<Point> sBucket = simGrid.get(key);
                    if (sBucket != null) {
                        for (Point sp : sBucket) {
                            double dLatKm = (lat - sp.lat) * degToKmLat;
                            if (Math.abs(dLatKm) > maxDistKm) continue;
                            double dLngKm = (lng - sp.lng) * degToKmLng;
                            double dSq = dLatKm * dLatKm + dLngKm * dLngKm;
                            if (dSq <= maxDistKmSq) {
                                simField += sp.weight * Math.exp(-dSq / twoSigmaSq);
                            }
                        }
                    }

                    List<Point> oBucket = obsGrid.get(key);
                    if (oBucket != null) {
                        for (Point op : oBucket) {
                            double dLatKm = (lat - op.lat) * degToKmLat;
                            if (Math.abs(dLatKm) > maxDistKm) continue;
                            double dLngKm = (lng - op.lng) * degToKmLng;
                            double dSq = dLatKm * dLatKm + dLngKm * dLngKm;
                            if (dSq <= maxDistKmSq) {
                                obsField += op.weight * Math.exp(-dSq / twoSigmaSq);
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

    static double extractCulturalCellValue(H3Cell c, String channelKey) {
        long pop = c.getPopulationOr(0);
        if (channelKey == null) {
            double area = Math.max(1.0, c.getCellAreaKm2());
            return Math.log10(1.0 + ((double) pop / area));
        }
        return switch (channelKey.trim().toLowerCase(Locale.ROOT)) {
            case "technology", "material", "tech" -> H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c);
            case "institutions", "admin", "institutional" -> {
                double kpc = (c.getResourceCapital() != null && pop > 0) ? (c.getResourceCapital() / pop) : 0.0;
                yield (Math.log10(Math.max(1.0, pop)) / 6.0) * 0.6 + (Math.min(1.0, kpc / 1500.0) * 0.4);
            }
            case "ecology", "anthrome", "footprint", "carrying_capacity" -> {
                double bioAgr = c.getBiomassAgricultureOr(0.0);
                double bioLiv = c.getBiomassLivestock() != null ? c.getBiomassLivestock() : 0.0;
                double bioNat = c.getBiomassNaturalOr(0.0);
                double clearanceRatio = (bioAgr + bioLiv) / (bioAgr + bioLiv + bioNat + 100.0);
                double area = Math.max(1.0, c.getCellAreaKm2());
                double densityStrain = pop > 0 ? Math.min(1.0, Math.log1p((double) pop / area) / 6.0) : 0.0;
                yield Math.clamp(0.75 * clearanceRatio + 0.25 * densityStrain, 0.0, 1.0);
            }
            case "pathogens", "sanitary", "epidemic" -> {
                double area = Math.max(1.0, c.getCellAreaKm2());
                double endemicUrbanLoad = pop > 0 ? Math.log10(1.0 + ((double) pop / area)) * 0.35 : 0.0;
                yield (double) c.getEpidemicRecoveredOr(0) + (double) c.getEpidemicInfectedOr(0) + endemicUrbanLoad;
            }
            case "trade", "market", "trade_network" -> {
                double area = Math.max(1.0, c.getCellAreaKm2());
                double explicitTrade = c.getTradeNetworkOr(0.0) + c.getChokePointScoreOr(0.0);
                double settlementMarket = pop > 0 ? Math.log10(1.0 + ((double) pop / area)) * 0.45 : 0.0;
                yield explicitTrade + settlementMarket;
            }
            default -> {
                double area = Math.max(1.0, c.getCellAreaKm2());
                yield Math.log10(1.0 + ((double) pop / area));
            }
        };
    }
}
