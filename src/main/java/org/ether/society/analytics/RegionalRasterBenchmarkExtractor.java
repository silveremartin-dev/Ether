/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.data.ConservativeRasterResampler;
import org.ether.society.data.Hyde34GridReader;
import org.ether.society.data.Hyde34LandUseReader;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Extracts true regional empirical ground-truth trajectories inside windowed geographic bounds
 * {@code [minLat, maxLat, minLng, maxLng]} directly from 2D conserved spherical rasters
 * (HYDE 3.4 population counts {@code popc_*.asc} and land-use archives {@code *_lu.zip}).
 *
 * <p>Eliminates the legacy static global-to-regional extensive ratio approximation by integrating
 * physical empirical grid cells directly inside the simulated spatial domain.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see Hyde34GridReader
 * @see Hyde34LandUseReader
 */
public final class RegionalRasterBenchmarkExtractor {

    private static final int[] HYDE_ANCHOR_EPOCHS = {
            -10000, -5000, -3000, -1000, 0, 500, 1000, 1500, 1700, 1800, 1900, 1950, 2000, 2020
    };

    private static final Map<String, Map<Integer, Double>> WINDOW_SERIES_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Map<Integer, Double>> eldest) {
                    return size() > 64;
                }
            });

    private RegionalRasterBenchmarkExtractor() {
        // Pure static helper
    }

    /**
     * Parses serialized regional window bounds from {@link SimulationRunRecord#getParameterMatrix()}.
     *
     * @param run simulation run record
     * @return {@code double[]{minLat, maxLat, minLng, maxLng}} if windowed regional bounds exist, else {@code null}
     */
    public static double[] parseWindowBounds(SimulationRunRecord run) {
        if (run == null || run.getParameterMatrix() == null) {
            return null;
        }
        String raw = run.getParameterMatrix().get("Window Bounds");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = raw.replace("[", "").replace("]", "").trim();
        String[] parts = cleaned.split(",");
        if (parts.length != 4) {
            return null;
        }
        try {
            double minLat = Double.parseDouble(parts[0].trim());
            double maxLat = Double.parseDouble(parts[1].trim());
            double minLng = Double.parseDouble(parts[2].trim());
            double maxLng = Double.parseDouble(parts[3].trim());
            if (minLat >= maxLat || minLng >= maxLng) {
                return null;
            }
            if (minLat <= -89.9 && maxLat >= 89.9 && minLng <= -179.9 && maxLng >= 179.9) {
                return null;
            }
            return new double[]{minLat, maxLat, minLng, maxLng};
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Indicates whether the canonical benchmark variable has a genuine 2D empirical raster backing
     * for regional windowed comparisons.
     *
     * @param benchKey canonical benchmark key
     * @return {@code true} if regional raster ground-truth extraction is supported
     */
    public static boolean supportsRegionalRaster(String benchKey) {
        return "worldPopulation".equals(benchKey)
                || "population".equals(benchKey)
                || "deforestationRate".equals(benchKey)
                || "forestCoverRemainingPct".equals(benchKey);
    }

    /**
     * Indicates whether an extensive global indicator lacks a spatial 2D ground-truth dataset
     * and therefore cannot be fairly compared by downscaling a global scalar aggregate.
     *
     * @param benchKey canonical benchmark key
     * @return {@code true} if global scalar scaling would be scientifically invalid in a regional run
     */
    public static boolean isUnrasteredExtensiveGlobalMetric(String benchKey) {
        return "grossWorldProduct".equals(benchKey) || "primaryEnergy".equals(benchKey);
    }

    /**
     * Sums conserved regional population inside {@code [minLat, maxLat, minLng, maxLng]} directly
     * from a conserved count raster indexed {@code [height][width]}.
     *
     * @param counts conserved headcount matrix {@code [y][x]}
     * @param minLat southern latitude bound in degrees
     * @param maxLat northern latitude bound in degrees
     * @param minLng western longitude bound in degrees
     * @param maxLng eastern longitude bound in degrees
     * @return exact regional population headcount integral
     */
    public static double sumConservedRegion(float[][] counts,
                                            double minLat, double maxLat,
                                            double minLng, double maxLng) {
        if (counts == null || counts.length == 0 || counts[0].length == 0) {
            return Double.NaN;
        }
        int h = counts.length;
        int w = counts[0].length;
        int yMin = Math.clamp((int) Math.floor(((90.0 - maxLat) / 180.0) * h), 0, h - 1);
        int yMax = Math.clamp((int) Math.floor(((90.0 - minLat) / 180.0) * h), 0, h - 1);
        int xMin = Math.clamp((int) Math.floor(((minLng + 180.0) / 360.0) * w), 0, w - 1);
        int xMax = Math.clamp((int) Math.floor(((maxLng + 180.0) / 360.0) * w), 0, w - 1);

        double sum = 0.0;
        for (int y = yMin; y <= yMax; y++) {
            float[] row = counts[y];
            for (int x = xMin; x <= xMax; x++) {
                float v = row[x];
                if (v > 0.0f && Float.isFinite(v)) {
                    sum += v;
                }
            }
        }
        return sum;
    }

    /**
     * Computes remaining climax forest retention (%) inside the spatial window from HYDE 3.4 land-use grids.
     */
    public static double computeRegionalForestRetentionPct(Hyde34LandUseReader.LandUseFields lu,
                                                           double minLat, double maxLat,
                                                           double minLng, double maxLng) {
        if (lu == null) return 99.0;
        int h = Hyde34LandUseReader.ETHER_HEIGHT;
        int w = Hyde34LandUseReader.ETHER_WIDTH;
        int yMin = Math.clamp((int) Math.floor(((90.0 - maxLat) / 180.0) * h), 0, h - 1);
        int yMax = Math.clamp((int) Math.floor(((90.0 - minLat) / 180.0) * h), 0, h - 1);
        int xMin = Math.clamp((int) Math.floor(((minLng + 180.0) / 360.0) * w), 0, w - 1);
        int xMax = Math.clamp((int) Math.floor(((maxLng + 180.0) / 360.0) * w), 0, w - 1);

        double clearedAreaKm2 = 0.0;
        double totalWindowAreaKm2 = 0.0;
        for (int y = yMin; y <= yMax; y++) {
            double pxArea = ConservativeRasterResampler.sphericalPixelAreaKm2(y, w, h);
            float[] crop = lu.croplandFrac()[y];
            float[] past = lu.pastureFrac()[y];
            float[] urb = lu.urbanFrac()[y];
            for (int x = xMin; x <= xMax; x++) {
                double frac = Math.clamp(crop[x] + 0.5 * past[x] + urb[x], 0.0, 1.0);
                clearedAreaKm2 += frac * pxArea;
                totalWindowAreaKm2 += pxArea;
            }
        }
        if (totalWindowAreaKm2 <= 0.0) return 99.0;
        return Math.clamp(100.0 * (1.0 - (clearedAreaKm2 / totalWindowAreaKm2)), 10.0, 99.5);
    }

    /**
     * Extracts and linearly/monotonically interpolates a true regional empirical benchmark series
     * across {@code sampleYears} from locally present 2D HYDE 3.4 rasters.
     *
     * @param benchKey     canonical benchmark key
     * @param windowBounds {@code double[]{minLat, maxLat, minLng, maxLng}}
     * @param sampleYears  years to evaluate
     * @return year -> empirical regional ground-truth value (empty map if unavailable on disk)
     */
    public static Map<Integer, Double> extractRegionalSeries(String benchKey,
                                                             double[] windowBounds,
                                                             Collection<Integer> sampleYears) {
        if (benchKey == null || windowBounds == null || windowBounds.length != 4
                || sampleYears == null || sampleYears.isEmpty() || !supportsRegionalRaster(benchKey)) {
            return Collections.emptyMap();
        }

        double minLat = windowBounds[0];
        double maxLat = windowBounds[1];
        double minLng = windowBounds[2];
        double maxLng = windowBounds[3];
        String cacheKey = String.format(java.util.Locale.ROOT, "%s|%.3f,%.3f,%.3f,%.3f",
                benchKey, minLat, maxLat, minLng, maxLng);

        Map<Integer, Double> anchorSeries = WINDOW_SERIES_CACHE.get(cacheKey);
        if (anchorSeries == null) {
            NavigableMap<Integer, Double> built = new TreeMap<>();
            for (int epoch : HYDE_ANCHOR_EPOCHS) {
                if ("worldPopulation".equals(benchKey) || "population".equals(benchKey)) {
                    float[][] counts = Hyde34GridReader.readConservedPopulationCountsIfPresent(epoch);
                    if (counts != null) {
                        double headcount = sumConservedRegion(counts, minLat, maxLat, minLng, maxLng);
                        built.put(epoch, "worldPopulation".equals(benchKey) ? (headcount / 1_000_000.0) : headcount);
                    }
                } else if ("deforestationRate".equals(benchKey) || "forestCoverRemainingPct".equals(benchKey)) {
                    Hyde34LandUseReader.LandUseFields lu = Hyde34LandUseReader.loadForYear(epoch);
                    if (lu != null) {
                        built.put(epoch, computeRegionalForestRetentionPct(lu, minLat, maxLat, minLng, maxLng));
                    }
                }
            }
            anchorSeries = Collections.unmodifiableMap(built);
            WINDOW_SERIES_CACHE.put(cacheKey, anchorSeries);
        }

        if (anchorSeries.isEmpty()) {
            return Collections.emptyMap();
        }

        NavigableMap<Integer, Double> nav = new TreeMap<>(anchorSeries);
        Map<Integer, Double> interpolated = new LinkedHashMap<>();
        List<Integer> yrs = sampleYears.stream().sorted().toList();
        for (int yr : yrs) {
            Map.Entry<Integer, Double> floor = nav.floorEntry(yr);
            Map.Entry<Integer, Double> ceil = nav.ceilingEntry(yr);
            if (floor == null && ceil != null) {
                interpolated.put(yr, ceil.getValue());
            } else if (ceil == null && floor != null) {
                interpolated.put(yr, floor.getValue());
            } else if (floor != null && ceil != null) {
                if (floor.getKey().equals(ceil.getKey())) {
                    interpolated.put(yr, floor.getValue());
                } else {
                    double t = (double) (yr - floor.getKey()) / (double) (ceil.getKey() - floor.getKey());
                    interpolated.put(yr, floor.getValue() + t * (ceil.getValue() - floor.getValue()));
                }
            }
        }
        return interpolated;
    }
}
