/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import com.uber.h3core.H3Core;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.h3.H3Service;
import org.ether.society.model.Biome;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * High-performance deterministic spatial binning coordinator transferring spherical rasters
 * onto discrete H3 hexagonal meshes via parallel area-weighted quadrature.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 * @see H3RasterAggregator
 */
public final class H3RasterBinningCoordinator {

    /** Approximate mean hexagon circumradius in degrees latitude at H3 resolutions 0..10. */
    public static final double[] H3_RADIUS_DEG = {
        11.5, 4.35, 1.64, 0.62, 0.235, 0.089, 0.0335, 0.0127, 0.0048, 0.0018, 0.00068
    };

    private H3RasterBinningCoordinator() {
        // Pure static utility coordinator
    }

    /**
     * Spatial grid topology metadata describing raster-to-H3 mesh correspondence.
     */
    public record GridTopology(
            boolean usePixelBinning,
            int h3Res,
            int subDiv,
            H3Core h3Core,
            Map<Long, Integer> cellIndexMap,
            double minLat,
            double maxLat,
            double minLng,
            double maxLng,
            boolean fullLongitudeSpan) {}

    /**
     * Analyzes mesh geometry and raster dimensions to determine optimal binning strategy.
     */
    public static GridTopology analyzeTopology(List<H3Cell> cells, int rasterW, int rasterH) {
        H3Core h3Core = null;
        int detectedRes = -1;
        Map<Long, Integer> indexMap = HashMap.newHashMap(cells.size());

        double minLat = 90.0, maxLat = -90.0;
        double minLng = 180.0, maxLng = -180.0;
        boolean allValidH3 = true;

        try {
            h3Core = H3Service.getInstance().getH3();
            for (int i = 0; i < cells.size(); i++) {
                H3Cell c = cells.get(i);
                if (c == null || c.getH3Index() == null || c.getH3Index() == 0L
                        || c.getLatitude() == null || c.getLongitude() == null) {
                    allValidH3 = false;
                    break;
                }
                int r = h3Core.getResolution(c.getH3Index());
                if (detectedRes < 0) detectedRes = r;
                indexMap.put(c.getH3Index(), i);

                double lat = c.getLatitude();
                double lng = c.getLongitude();
                if (lat < minLat) minLat = lat;
                if (lat > maxLat) maxLat = lat;
                if (lng < minLng) minLng = lng;
                if (lng > maxLng) maxLng = lng;
            }
        } catch (Throwable t) {
            allValidH3 = false;
        }

        if (!allValidH3 || detectedRes < 0 || detectedRes > 10 || indexMap.size() != cells.size()) {
            return new GridTopology(false, Math.max(0, detectedRes), 1, h3Core, Map.of(), -90, 90, -180, 180, true);
        }

        double expectedGlobalCells = 122.0 * Math.pow(7.0, detectedRes);
        double pxPerHex = ((double) rasterW * rasterH) / expectedGlobalCells;

        if (rasterW < 64 || rasterH < 32 || pxPerHex < 1.5) {
            return new GridTopology(false, detectedRes, 1, h3Core, Map.of(), minLat, maxLat, minLng, maxLng, true);
        }

        int subDiv = (pxPerHex < 20.0) ? 2 : 1;
        double marginDeg = H3_RADIUS_DEG[Math.min(detectedRes, H3_RADIUS_DEG.length - 1)] * 2.5 + 1.0;
        double padLatMin = Math.max(-90.0, minLat - marginDeg);
        double padLatMax = Math.min(90.0, maxLat + marginDeg);
        double cosMin = Math.max(0.15, Math.cos(Math.toRadians(Math.max(Math.abs(padLatMin), Math.abs(padLatMax)))));
        double marginLng = marginDeg / cosMin;
        boolean fullLon = (maxLng - minLng + 2.0 * marginLng) >= 340.0;
        double padLngMin = fullLon ? -180.0 : Math.max(-180.0, minLng - marginLng);
        double padLngMax = fullLon ? 180.0 : Math.min(180.0, maxLng + marginLng);

        return new GridTopology(true, detectedRes, subDiv, h3Core, indexMap, padLatMin, padLatMax, padLngMin, padLngMax, fullLon);
    }

    /**
     * Performs parallel deterministic binning of density pixels onto H3 cells.
     */
    public static void binDensityPixelsToCells(
            H3RasterAggregator.RasterArgbBuffer buf, GridTopology topo, double[] outSumW, double[] outSumArea) {
        int w = buf.width(), h = buf.height(), sub = topo.subDiv();
        int effW = w * sub, effH = h * sub, nCells = outSumW.length;

        int yStart = Math.max(0, (int) Math.floor(((90.0 - topo.maxLat()) / 180.0) * effH));
        int yEnd = Math.min(effH, (int) Math.ceil(((90.0 - topo.minLat()) / 180.0) * effH) + 1);
        int xStart = topo.fullLongitudeSpan() ? 0 : Math.max(0, (int) Math.floor(((topo.minLng() + 180.0) / 360.0) * effW));
        int xEnd = topo.fullLongitudeSpan() ? effW : Math.min(effW, (int) Math.ceil(((topo.maxLng() + 180.0) / 360.0) * effW) + 1);

        int workers = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), 16));
        if (yEnd - yStart < 64) workers = 1;

        double[][] wSumWorker = new double[workers][nCells];
        double[][] wAreaWorker = new double[workers][nCells];
        int res = topo.h3Res();
        H3Core h3 = topo.h3Core();
        Map<Long, Integer> idxMap = topo.cellIndexMap();
        int[] pxArr = buf.argb();

        final int totalRows = yEnd - yStart;
        final int numWorkers = workers;
        IntStream.range(0, numWorkers).parallel().forEach(workerId -> {
            int rowStart = yStart + (int) ((long) workerId * totalRows / numWorkers);
            int rowEnd = yStart + (int) ((long) (workerId + 1) * totalRows / numWorkers);
            double[] localW = wSumWorker[workerId];
            double[] localA = wAreaWorker[workerId];

            for (int sy = rowStart; sy < rowEnd; sy++) {
                double lat = 90.0 - ((sy + 0.5) / effH) * 180.0;
                double areaSub = ConservativeRasterResampler.sphericalPixelAreaKm2(sy, effW, effH);
                int srcRowOff = (sy / sub) * w;

                for (int sx = xStart; sx < xEnd; sx++) {
                    double lon = -180.0 + ((sx + 0.5) / effW) * 360.0;
                    long h3Idx = h3.latLngToCell(lat, lon, res);
                    Integer cellPos = idxMap.get(h3Idx);
                    if (cellPos == null) continue;

                    int pos = cellPos;
                    double rho = argbToGrayNorm(pxArr[srcRowOff + (sx / sub)]);
                    localW[pos] += rho * areaSub;
                    localA[pos] += areaSub;
                }
            }
        });

        for (int wId = 0; wId < numWorkers; wId++) {
            double[] lw = wSumWorker[wId], la = wAreaWorker[wId];
            for (int i = 0; i < nCells; i++) {
                outSumW[i] += lw[i];
                outSumArea[i] += la[i];
            }
        }
    }

    /**
     * Bins all raster layers onto simulation cells concurrently with deterministic reduction.
     */
    public static void binAllLayersToCells(
            GridTopology topo, int refW, int refH,
            H3RasterAggregator.RasterArgbBuffer elevBuf, double minAlt, double maxAlt, double[] sumElev, double[] areaElev,
            H3RasterAggregator.RasterArgbBuffer biomeBuf, double[][] biomeAreas,
            H3RasterAggregator.RasterArgbBuffer resBuf, double[] sumR, double[] sumG, double[] sumB, double[] areaRes,
            H3RasterAggregator.RasterArgbBuffer climateBuf, double[] sumTemp, double[] sumClimRain, double[] areaClim,
            H3RasterAggregator.RasterArgbBuffer rainBuf, double[] sumRain, double[] areaRain,
            H3RasterAggregator.RasterArgbBuffer seasonBuf, double[] sumSeas, double[] areaSeas) {

        int sub = topo.subDiv();
        int effW = refW * sub, effH = refH * sub;
        int nCells = topo.cellIndexMap().size();

        int yStart = Math.max(0, (int) Math.floor(((90.0 - topo.maxLat()) / 180.0) * effH));
        int yEnd = Math.min(effH, (int) Math.ceil(((90.0 - topo.minLat()) / 180.0) * effH) + 1);
        int xStart = topo.fullLongitudeSpan() ? 0 : Math.max(0, (int) Math.floor(((topo.minLng() + 180.0) / 360.0) * effW));
        int xEnd = topo.fullLongitudeSpan() ? effW : Math.min(effW, (int) Math.ceil(((topo.maxLng() + 180.0) / 360.0) * effW) + 1);

        int workers = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), 16));
        if (yEnd - yStart < 64) workers = 1;

        double[][] wElev = elevBuf != null ? new double[workers][nCells] : null;
        double[][] aElev = elevBuf != null ? new double[workers][nCells] : null;
        double[][][] wBiome = biomeBuf != null ? new double[workers][nCells][Biome.VALUES.length] : null;
        double[][] wResR = resBuf != null ? new double[workers][nCells] : null;
        double[][] wResG = resBuf != null ? new double[workers][nCells] : null;
        double[][] wResB = resBuf != null ? new double[workers][nCells] : null;
        double[][] aRes = resBuf != null ? new double[workers][nCells] : null;
        double[][] wTemp = climateBuf != null ? new double[workers][nCells] : null;
        double[][] wClimRain = (climateBuf != null && rainBuf == null) ? new double[workers][nCells] : null;
        double[][] aClim = climateBuf != null ? new double[workers][nCells] : null;
        double[][] wRain = rainBuf != null ? new double[workers][nCells] : null;
        double[][] aRain = rainBuf != null ? new double[workers][nCells] : null;
        double[][] wSeas = seasonBuf != null ? new double[workers][nCells] : null;
        double[][] aSeas = seasonBuf != null ? new double[workers][nCells] : null;

        int res = topo.h3Res();
        H3Core h3 = topo.h3Core();
        Map<Long, Integer> idxMap = topo.cellIndexMap();
        int totalRows = yEnd - yStart;
        int numWorkers = workers;

        IntStream.range(0, numWorkers).parallel().forEach(workerId -> {
            int rowStart = yStart + (int) ((long) workerId * totalRows / numWorkers);
            int rowEnd = yStart + (int) ((long) (workerId + 1) * totalRows / numWorkers);

            for (int sy = rowStart; sy < rowEnd; sy++) {
                double vNorm = (sy + 0.5) / effH;
                double areaSub = ConservativeRasterResampler.sphericalPixelAreaKm2(sy, effW, effH);

                for (int sx = xStart; sx < xEnd; sx++) {
                    double uNorm = (sx + 0.5) / effW;
                    double lon = -180.0 + uNorm * 360.0;
                    double lat = 90.0 - vNorm * 180.0;
                    long h3Idx = h3.latLngToCell(lat, lon, res);
                    Integer cellPos = idxMap.get(h3Idx);
                    if (cellPos == null) continue;
                    int pos = cellPos;

                    if (elevBuf != null) {
                        int argb = sampleNormalized(elevBuf, uNorm, vNorm);
                        double zM = PlanetPreset.waterLevelToMeters(argbToMaxChannelNorm(argb), minAlt, maxAlt);
                        wElev[workerId][pos] += zM * areaSub;
                        aElev[workerId][pos] += areaSub;
                    }
                    if (biomeBuf != null) {
                        int argb = sampleNormalized(biomeBuf, uNorm, vNorm);
                        Biome b = ImageMapLoader.matchBiomeArgb(argb);
                        wBiome[workerId][pos][b.ordinal()] += areaSub;
                    }
                    if (resBuf != null) {
                        int argb = sampleNormalized(resBuf, uNorm, vNorm);
                        wResR[workerId][pos] += (((argb >> 16) & 0xFF) / 255.0) * areaSub;
                        wResG[workerId][pos] += (((argb >> 8) & 0xFF) / 255.0) * areaSub;
                        wResB[workerId][pos] += ((argb & 0xFF) / 255.0) * areaSub;
                        aRes[workerId][pos] += areaSub;
                    }
                    if (climateBuf != null) {
                        int argb = sampleNormalized(climateBuf, uNorm, vNorm);
                        double rNorm = ((argb >> 16) & 0xFF) / 255.0;
                        wTemp[workerId][pos] += (-50.0 + rNorm * 100.0) * areaSub;
                        if (wClimRain != null) {
                            double gNorm = ((argb >> 8) & 0xFF) / 255.0;
                            wClimRain[workerId][pos] += (gNorm * 3000.0) * areaSub;
                        }
                        aClim[workerId][pos] += areaSub;
                    }
                    if (rainBuf != null) {
                        int argb = sampleNormalized(rainBuf, uNorm, vNorm);
                        wRain[workerId][pos] += (argbToMaxChannelNorm(argb) * 3000.0) * areaSub;
                        aRain[workerId][pos] += areaSub;
                    }
                    if (seasonBuf != null) {
                        int argb = sampleNormalized(seasonBuf, uNorm, vNorm);
                        wSeas[workerId][pos] += (argbToMaxChannelNorm(argb) * 50.0) * areaSub;
                        aSeas[workerId][pos] += areaSub;
                    }
                }
            }
        });

        for (int wId = 0; wId < numWorkers; wId++) {
            for (int i = 0; i < nCells; i++) {
                if (elevBuf != null) {
                    sumElev[i] += wElev[wId][i];
                    areaElev[i] += aElev[wId][i];
                }
                if (biomeBuf != null) {
                    double[] srcB = wBiome[wId][i], dstB = biomeAreas[i];
                    for (int b = 0; b < Biome.VALUES.length; b++) dstB[b] += srcB[b];
                }
                if (resBuf != null) {
                    sumR[i] += wResR[wId][i];
                    sumG[i] += wResG[wId][i];
                    sumB[i] += wResB[wId][i];
                    areaRes[i] += aRes[wId][i];
                }
                if (climateBuf != null) {
                    sumTemp[i] += wTemp[wId][i];
                    if (sumClimRain != null) sumClimRain[i] += wClimRain[wId][i];
                    areaClim[i] += aClim[wId][i];
                }
                if (rainBuf != null) {
                    sumRain[i] += wRain[wId][i];
                    areaRain[i] += aRain[wId][i];
                }
                if (seasonBuf != null) {
                    sumSeas[i] += wSeas[wId][i];
                    areaSeas[i] += aSeas[wId][i];
                }
            }
        }
    }

    public static int sampleNormalized(H3RasterAggregator.RasterArgbBuffer buf, double u, double v) {
        int px = Math.min((int) (u * buf.width()), buf.width() - 1);
        int py = Math.min((int) (v * buf.height()), buf.height() - 1);
        return buf.argb()[py * buf.width() + px];
    }

    public static double argbToGrayNorm(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return ((r + g + b) / 3.0) / 255.0;
    }

    public static double argbToMaxChannelNorm(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return Math.max(r, Math.max(g, b)) / 255.0;
    }

    public static double normalizeLon(double lon) {
        double wrapped = ((lon + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
        return (wrapped == -180.0 && lon > 0.0) ? 180.0 : wrapped;
    }
}
