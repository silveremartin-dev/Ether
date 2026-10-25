/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;

import java.util.*;

/**
 * <h1>SpatialFalsificationMetrics</h1>
 * <p>
 * Mathematical utility suite providing spatial statistical metrics for falsification
 * and sensitivity benchmarks across windowed sub-grids and full planetary spheres.
 * </p>
 *
 * <p>Implements:
 * <ul>
 *   <li>Spatial Pearson correlation coefficient \(r\).</li>
 *   <li>Mean Absolute Percentage Error (\(\text{MAPE}\)) on common core spatial cells.</li>
 *   <li>Demographic centroid center-of-mass shift \(\Delta R\) (km) via Great-Circle Haversine distance.</li>
 *   <li>Boundary reflection index \(I_{\mathrm{refl}}\) evaluating boundary-to-core demographic ratios.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class SpatialFalsificationMetrics {

    private SpatialFalsificationMetrics() {
        // Private constructor for static utility class
    }

    /**
     * Computes the spatial Pearson correlation coefficient \(r\) between two scalar spatial cell maps.
     *
     * @param mapA first scalar map indexed by H3 index
     * @param mapB second scalar map indexed by H3 index
     * @return spatial Pearson correlation coefficient \(r \in [-1, 1]\)
     */
    public static double computeSpatialPearsonCorrelation(Map<Long, Double> mapA, Map<Long, Double> mapB) {
        Set<Long> commonKeys = new HashSet<>(mapA.keySet());
        commonKeys.retainAll(mapB.keySet());
        if (commonKeys.size() < 2) return 1.0;

        double sumA = 0.0, sumB = 0.0;
        for (Long k : commonKeys) { sumA += mapA.get(k); sumB += mapB.get(k); }
        double meanA = sumA / commonKeys.size(), meanB = sumB / commonKeys.size();

        double num = 0.0, denA = 0.0, denB = 0.0;
        for (Long k : commonKeys) {
            double diffA = mapA.get(k) - meanA, diffB = mapB.get(k) - meanB;
            num += diffA * diffB;
            denA += diffA * diffA;
            denB += diffB * diffB;
        }
        double denom = Math.sqrt(denA * denB);
        return (denom > 1e-9) ? (num / denom) : 1.0;
    }

    /**
     * Computes the core Mean Absolute Percentage Error (\(\text{MAPE}\)) between global and windowed runs.
     *
     * @param mapGlobal global planetary simulation demographic density map
     * @param mapWindow windowed sub-grid simulation demographic density map
     * @return core MAPE value
     */
    public static double computeCoreMAPE(Map<Long, Double> mapGlobal, Map<Long, Double> mapWindow) {
        Set<Long> commonKeys = new HashSet<>(mapGlobal.keySet());
        commonKeys.retainAll(mapWindow.keySet());
        if (commonKeys.isEmpty()) return 0.0;

        double totalRelativeError = 0.0;
        for (Long k : commonKeys) {
            double gVal = mapGlobal.get(k), wVal = mapWindow.get(k);
            totalRelativeError += Math.abs(wVal - gVal) / Math.max(10.0, gVal);
        }
        return totalRelativeError / commonKeys.size();
    }

    /**
     * Computes the spatial demographic centroid shift \(\Delta R\) in kilometers between two runs.
     *
     * @param mapGlobal global demographic density map
     * @param mapWindow windowed demographic density map
     * @param cellsRef  reference cellular collection providing latitude and longitude
     * @return Great-Circle centroid shift distance in kilometers
     */
    public static double computeCentroidShiftKm(Map<Long, Double> mapGlobal, Map<Long, Double> mapWindow, List<H3Cell> cellsRef) {
        Map<Long, H3Cell> cellLookup = new HashMap<>();
        if (cellsRef != null) {
            for (H3Cell c : cellsRef) cellLookup.put(c.getH3Index(), c);
        }

        double latG = 0.0, lonG = 0.0, popG = 0.0;
        for (Map.Entry<Long, Double> entry : mapGlobal.entrySet()) {
            H3Cell c = cellLookup.get(entry.getKey());
            if (c != null) {
                latG += c.getLatitude() * entry.getValue();
                lonG += c.getLongitude() * entry.getValue();
                popG += entry.getValue();
            }
        }
        double latW = 0.0, lonW = 0.0, popW = 0.0;
        for (Map.Entry<Long, Double> entry : mapWindow.entrySet()) {
            H3Cell c = cellLookup.get(entry.getKey());
            if (c != null) {
                latW += c.getLatitude() * entry.getValue();
                lonW += c.getLongitude() * entry.getValue();
                popW += entry.getValue();
            }
        }
        if (popG <= 0 || popW <= 0) return 0.0;
        latG /= popG; lonG /= popG;
        latW /= popW; lonW /= popW;

        return calculateHaversineDistance(latG, lonG, latW, lonW, 6371.0);
    }

    /**
     * Computes the boundary reflection index \(I_{\mathrm{refl}}\).
     *
     * @param mapGlobal global demographic map
     * @param mapWindow windowed demographic map
     * @param cellsRef  reference cellular collection
     * @return boundary reflection index
     */
    public static double computeBoundaryReflectionIndex(Map<Long, Double> mapGlobal, Map<Long, Double> mapWindow, List<H3Cell> cellsRef) {
        if (cellsRef == null || cellsRef.isEmpty()) return 1.0;

        double bPopG = 0.0, cPopG = 0.0;
        double bPopW = 0.0, cPopW = 0.0;

        for (H3Cell c : cellsRef) {
            long id = c.getH3Index();
            double gVal = mapGlobal.getOrDefault(id, 0.0);
            double wVal = mapWindow.getOrDefault(id, 0.0);

            if (c.isBoundaryCell()) {
                bPopG += gVal;
                bPopW += wVal;
            } else {
                cPopG += gVal;
                cPopW += wVal;
            }
        }

        double ratioG = (bPopG + 1.0) / (cPopG + 1.0);
        double ratioW = (bPopW + 1.0) / (cPopW + 1.0);
        return ratioW / Math.max(1e-6, ratioG);
    }

    /**
     * Computes Great-Circle distance using Haversine formula.
     *
     * @param lat1 latitude 1 in degrees
     * @param lon1 longitude 1 in degrees
     * @param lat2 latitude 2 in degrees
     * @param lon2 longitude 2 in degrees
     * @param radiusKm planetary radius in kilometers
     * @return distance in kilometers
     */
    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2, double radiusKm) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return radiusKm * c;
    }

    /**
     * Performs a deep clone of a scenario configuration.
     *
     * @param s scenario to clone
     * @return cloned scenario
     */
    public static Scenario cloneScenario(Scenario s) {
        if (s == null) return null;
        Scenario copy = new Scenario();
        copy.setName(s.getName());
        copy.setStartDateYear(s.getStartDateYear());
        copy.setEndDateYear(s.getEndDateYear());
        copy.setInitialHumanCount(s.getInitialHumanCount());
        copy.setH3Resolution(s.getH3Resolution());
        copy.setTemporalResolutionDays(s.getTemporalResolutionDays());
        copy.setSeed(s.getSeed());
        copy.setClippingEnabled(s.isClippingEnabled());
        copy.setMinLat(s.getMinLat());
        copy.setMaxLat(s.getMaxLat());
        copy.setMinLng(s.getMinLng());
        copy.setMaxLng(s.getMaxLng());
        copy.setBoundaryMode(s.getBoundaryMode());
        copy.setPlanetPreset(s.getPlanetPreset());
        copy.setEcologyPreset(s.getEcologyPreset());
        return copy;
    }
}
