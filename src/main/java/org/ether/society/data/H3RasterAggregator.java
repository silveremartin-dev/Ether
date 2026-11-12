/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import com.uber.h3core.H3Core;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.h3.H3Service;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * <h1>H3RasterAggregator</h1>
 * <p>
 * State-of-the-art spherical GIS aggregator transferring equirectangular rasters onto discrete
 * Uber H3 hexagonal meshes ({@link H3Cell}) using resolution-adaptive conservative integration:
 * </p>
 *
 * <ul>
 *   <li><b>Coarse-to-Medium H3 Regime</b> ($\text{pixels}/\text{hex} \ge 1.5$, e.g. $2048\times 1024$ onto H3 Res 0–4):
 *       Full parallel pixel-to-H3 spatial binning (with $2\times 2$ sub-pixel quadrature when $\text{pixels}/\text{hex} &lt; 20$),
 *       guaranteeing deterministic fixed-order reduction and exact spherical area weighting ($\propto \cos\phi$):
 *       <ul>
 *         <li><i>Extensive fields</i> (population density integral): $W_i = \sum_{s \in \text{hex}_i} \rho(s)\,A_{\text{sub}}(s)$,
 *             satisfying $\sum_{i \in \text{Globe}} W_i \equiv \sum_{x,y} \rho(x,y)\,A_{\text{px}}(y)$.</li>
 *         <li><i>Intensive scalar fields</i> (elevation, temperature, rainfall, seasonality, resources):
 *             $\bar{q}_i = \frac{\sum_{s \in \text{hex}_i} q(s)\,A_{\text{sub}}(s)}{\sum_{s \in \text{hex}_i} A_{\text{sub}}(s)}$.</li>
 *         <li><i>Categorical biomes</i>: spherical-area-weighted modal class $\arg\max_b \sum_{s \in \text{hex}_i} \mathbf{1}[b(s)=b]\,A_{\text{sub}}(s)$.</li>
 *       </ul></li>
 *   <li><b>Fine H3 / High-Resolution Regime</b> ($\text{pixels}/\text{hex} &lt; 1.5$, e.g. H3 Res 5–10 regional windows):
 *       7-point hexagonal Gauss-style cubature combined with continuous spherical bilinear interpolation
 *       of raster channels so sub-pixel gradients remain $C^0$-smooth without nearest-neighbor staircasing.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see ConservativeRasterResampler
 */
public final class H3RasterAggregator {
    private static final Logger logger = LoggerFactory.getLogger(H3RasterAggregator.class);

    /** Approximate mean hexagon circumradius in degrees latitude at H3 resolutions 0..10. */
    public static final double[] H3_RADIUS_DEG = H3RasterBinningCoordinator.H3_RADIUS_DEG;

    private H3RasterAggregator() {
        // Pure static GIS aggregator
    }

    /**
     * Unified immutable snapshot of an ARGB raster backed by a flat primitive pixel array.
     */
    public record RasterArgbBuffer(int width, int height, int[] argb) {

        /**
         * Extracts a flat ARGB pixel buffer from a headless {@link BufferedImage}.
         *
         * @param img source buffered image (may be {@code null})
         * @return extracted buffer, or {@code null} when input is null/empty
         */
        public static RasterArgbBuffer fromBufferedImage(BufferedImage img) {
            if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) return null;
            int w = img.getWidth();
            int h = img.getHeight();
            int[] pixels = new int[w * h];
            img.getRGB(0, 0, w, h, pixels, 0, w);
            return new RasterArgbBuffer(w, h, pixels);
        }

        /**
         * Extracts a flat ARGB pixel buffer from a JavaFX {@link Image}.
         *
         * @param img source JavaFX image (may be {@code null})
         * @return extracted buffer, or {@code null} when input is null/unreadable
         */
        public static RasterArgbBuffer fromFxImage(Image img) {
            if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) return null;
            PixelReader reader = img.getPixelReader();
            if (reader == null) return null;
            int w = (int) img.getWidth();
            int h = (int) img.getHeight();
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                int off = y * w;
                for (int x = 0; x < w; x++) {
                    pixels[off + x] = reader.getArgb(x, y);
                }
            }
            return new RasterArgbBuffer(w, h, pixels);
        }

        /** Samples exact nearest-pixel ARGB integer at spherical coordinates (lat, lon). */
        public int sampleNearestArgb(double lat, double lon) {
            double u = Math.clamp((lon + 180.0) / 360.0, 0.0, 1.0);
            double v = Math.clamp((90.0 - lat) / 180.0, 0.0, 1.0);
            int px = Math.min((int) (u * width), width - 1);
            int py = Math.min((int) (v * height), height - 1);
            return argb[py * width + px];
        }

        /** Bilinearly samples a scalar channel extractor at spherical coordinates (lat, lon). */
        public double sampleBilinearChannel(double lat, double lon, IntToDoubleScalar extractor) {
            double gx = Math.clamp((lon + 180.0) / 360.0, 0.0, 1.0) * width - 0.5;
            double gy = Math.clamp((90.0 - lat) / 180.0, 0.0, 1.0) * height - 0.5;

            int x0 = (int) Math.floor(gx);
            int y0 = (int) Math.floor(gy);
            double fx = gx - x0;
            double fy = gy - y0;

            int ix0 = ((x0 % width) + width) % width;
            int ix1 = (((x0 + 1) % width) + width) % width;
            int iy0 = Math.clamp(y0, 0, height - 1);
            int iy1 = Math.clamp(y0 + 1, 0, height - 1);

            double v00 = extractor.apply(argb[iy0 * width + ix0]);
            double v10 = extractor.apply(argb[iy0 * width + ix1]);
            double v01 = extractor.apply(argb[iy1 * width + ix0]);
            double v11 = extractor.apply(argb[iy1 * width + ix1]);

            return (1.0 - fx) * (1.0 - fy) * v00
                    + fx * (1.0 - fy) * v10
                    + (1.0 - fx) * fy * v01
                    + fx * fy * v11;
        }
    }

    @FunctionalInterface
    public interface IntToDoubleScalar {
        double apply(int argb);
    }

    /**
     * Aggregates an empirical demographic density raster onto a target list of H3 cells,
     * integrating density over each hexagon's spherical footprint ($\iint_{\text{hex}} \rho\,dA$).
     * Falls back seamlessly to 7-point bilinear hexagonal cubature $\times \text{cellAreaKm2}$
     * for ultra-fine regional H3 resolutions or non-H3 synthetic cells.
     *
     * @param densityImg empirical population density raster (grayscale brightness $\in [0, 1]$)
     * @param cells      target list of simulation cells
     * @return deterministic array of integrated areal weights aligned with {@code cells} indices
     */
    public static double[] aggregateDensityWeights(BufferedImage densityImg, List<H3Cell> cells) {
        return aggregateDensityInternal(densityImg, cells, false);
    }

    /**
     * Aggregates an empirical density raster onto a target list of H3 cells, returning the
     * area-weighted mean normalized density $\bar{\rho}_i = \frac{\iint_{\text{hex}_i} \rho\,dA}{\iint_{\text{hex}_i} dA} \in [0, 1]$
     * per hexagon for metrology and cellular-to-raster comparison.
     *
     * @param densityImg empirical population density raster (grayscale brightness $\in [0, 1]$)
     * @param cells      target list of simulation cells
     * @return deterministic array of mean densities $\in [0, 1]$ aligned with {@code cells} indices
     */
    public static double[] aggregateDensityMeans(BufferedImage densityImg, List<H3Cell> cells) {
        return aggregateDensityInternal(densityImg, cells, true);
    }

    private static double[] aggregateDensityInternal(BufferedImage densityImg, List<H3Cell> cells, boolean returnMean) {
        if (cells == null || cells.isEmpty()) return new double[0];
        double[] out = new double[cells.size()];
        RasterArgbBuffer buf = RasterArgbBuffer.fromBufferedImage(densityImg);
        if (buf == null) return out;

        H3RasterBinningCoordinator.GridTopology topo = H3RasterBinningCoordinator.analyzeTopology(cells, buf.width(), buf.height());
        if (topo.usePixelBinning()) {
            double[] sumW = new double[cells.size()], sumArea = new double[cells.size()];
            H3RasterBinningCoordinator.binDensityPixelsToCells(buf, topo, sumW, sumArea);

            for (int i = 0; i < cells.size(); i++) {
                H3Cell c = cells.get(i);
                if (sumArea[i] > 0.0) {
                    out[i] = returnMean ? (sumW[i] / sumArea[i]) : sumW[i];
                } else if (c != null && c.getLatitude() != null && c.getLongitude() != null) {
                    double meanRho = evaluateScalarCubature(buf, c.getLatitude(), c.getLongitude(), topo.h3Res(),
                            H3RasterAggregator::argbToGrayNorm);
                    if (returnMean) {
                        out[i] = meanRho;
                    } else {
                        double areaKm2 = c.getEffectiveSurfaceAreaKm2() > 0 ? c.getEffectiveSurfaceAreaKm2() : 1770.49;
                        out[i] = meanRho * areaKm2;
                    }
                }
            }
            return out;
        }

        // Fine resolution / synthetic grid fallback: 7-point bilinear cubature
        int res = Math.max(0, Math.min(10, topo.h3Res() >= 0 ? topo.h3Res() : 4));
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            double meanRho = evaluateScalarCubature(buf, c.getLatitude(), c.getLongitude(), res,
                    H3RasterAggregator::argbToGrayNorm);
            if (returnMean) {
                out[i] = meanRho;
            } else {
                double areaKm2 = c.getEffectiveSurfaceAreaKm2() > 0 ? c.getEffectiveSurfaceAreaKm2() : 1770.49;
                out[i] = meanRho * areaKm2;
            }
        }
        return out;
    }

    /**
     * Computes the exact global spherical area integral of a density raster across all pixels:
     * $$W_{\text{globe}} = \sum_{y=0}^{h-1} A_{\text{px}}(y) \sum_{x=0}^{w-1} \rho(x, y)$$
     * Mathematically invariant with respect to H3 grid resolution for resolution-independent
     * conservation in windowed regional simulations.
     *
     * @param densityImg empirical population density raster
     * @return global spherical-area-weighted integral
     */
    public static double computeGlobalDensityIntegral(BufferedImage densityImg) {
        RasterArgbBuffer buf = RasterArgbBuffer.fromBufferedImage(densityImg);
        if (buf == null) return 0.0;

        int w = buf.width();
        int h = buf.height();
        int[] px = buf.argb();
        double total = 0.0;

        for (int y = 0; y < h; y++) {
            double pxArea = ConservativeRasterResampler.sphericalPixelAreaKm2(y, w, h);
            int rowOff = y * w;
            double rowSum = 0.0;
            for (int x = 0; x < w; x++) {
                rowSum += argbToGrayNorm(px[rowOff + x]);
            }
            total += rowSum * pxArea;
        }
        return total;
    }

    /**
     * Maps any combination of equirectangular raster layers (elevation, biomes, resources, climate,
     * precipitation, seasonality) onto {@link H3Cell}s via spherical-area-weighted conservative binning
     * or 7-point bilinear hexagonal cubature.
     */
    public static void mapRastersToCells(
            List<H3Cell> cells,
            RasterArgbBuffer elevBuf,
            RasterArgbBuffer biomeBuf,
            RasterArgbBuffer resBuf,
            RasterArgbBuffer climateBuf,
            RasterArgbBuffer rainBuf,
            RasterArgbBuffer seasonBuf,
            double minAlt,
            double maxAlt) {
        if (cells == null || cells.isEmpty()) return;

        // Reference raster dimensions (preferring highest-resolution non-null layer)
        int refW = 0, refH = 0;
        RasterArgbBuffer[] allBufs = {elevBuf, biomeBuf, resBuf, climateBuf, rainBuf, seasonBuf};
        for (RasterArgbBuffer b : allBufs) {
            if (b != null && b.width() * b.height() > refW * refH) {
                refW = b.width();
                refH = b.height();
            }
        }
        if (refW == 0 || refH == 0) return;

        H3RasterBinningCoordinator.GridTopology topo = H3RasterBinningCoordinator.analyzeTopology(cells, refW, refH);
        int n = cells.size();

        if (topo.usePixelBinning()) {
            double[] sumElev = elevBuf != null ? new double[n] : null, areaElev = elevBuf != null ? new double[n] : null;
            double[][] biomeAreas = biomeBuf != null ? new double[n][Biome.VALUES.length] : null;
            double[] sumR = resBuf != null ? new double[n] : null, sumG = resBuf != null ? new double[n] : null;
            double[] sumB = resBuf != null ? new double[n] : null, areaRes = resBuf != null ? new double[n] : null;
            double[] sumTemp = climateBuf != null ? new double[n] : null, areaClim = climateBuf != null ? new double[n] : null;
            double[] sumClimRain = (climateBuf != null && rainBuf == null) ? new double[n] : null;
            double[] sumRain = rainBuf != null ? new double[n] : null, areaRain = rainBuf != null ? new double[n] : null;
            double[] sumSeas = seasonBuf != null ? new double[n] : null, areaSeas = seasonBuf != null ? new double[n] : null;

            H3RasterBinningCoordinator.binAllLayersToCells(topo, refW, refH,
                    elevBuf, minAlt, maxAlt, sumElev, areaElev,
                    biomeBuf, biomeAreas,
                    resBuf, sumR, sumG, sumB, areaRes,
                    climateBuf, sumTemp, sumClimRain, areaClim,
                    rainBuf, sumRain, areaRain,
                    seasonBuf, sumSeas, areaSeas);

            int fallbackRes = Math.max(0, Math.min(10, topo.h3Res()));
            for (int i = 0; i < n; i++) {
                H3Cell cell = cells.get(i);
                if (cell == null || cell.getLatitude() == null || cell.getLongitude() == null) continue;
                double lat = cell.getLatitude();
                double lng = cell.getLongitude();

                if (elevBuf != null) {
                    double elev = (areaElev[i] > 0.0)
                            ? (sumElev[i] / areaElev[i])
                            : evaluateScalarCubature(elevBuf, lat, lng, fallbackRes,
                            argb -> PlanetPreset.waterLevelToMeters(argbToMaxChannelNorm(argb), minAlt, maxAlt));
                    cell.setElevation(elev);
                }

                if (biomeBuf != null) {
                    Biome modal = selectModalBiome(biomeAreas[i]);
                    cell.setBiome(modal != null ? modal : evaluateBiomeCubature(biomeBuf, lat, lng, fallbackRes));
                } else if (elevBuf != null) {
                    applyElevationDerivedBiome(cell, maxAlt);
                }

                if (resBuf != null) {
                    double rNorm = (areaRes[i] > 0.0) ? (sumR[i] / areaRes[i])
                            : evaluateScalarCubature(resBuf, lat, lng, fallbackRes, argb -> ((argb >> 16) & 0xFF) / 255.0);
                    double gNorm = (areaRes[i] > 0.0) ? (sumG[i] / areaRes[i])
                            : evaluateScalarCubature(resBuf, lat, lng, fallbackRes, argb -> ((argb >> 8) & 0xFF) / 255.0);
                    double bNorm = (areaRes[i] > 0.0) ? (sumB[i] / areaRes[i])
                            : evaluateScalarCubature(resBuf, lat, lng, fallbackRes, argb -> (argb & 0xFF) / 255.0);
                    applyResourceNorms(cell, rNorm, gNorm, bNorm);
                }

                if (climateBuf != null) {
                    double temp = (areaClim[i] > 0.0) ? (sumTemp[i] / areaClim[i])
                            : evaluateScalarCubature(climateBuf, lat, lng, fallbackRes, argb -> -50.0 + (((argb >> 16) & 0xFF) / 255.0) * 100.0);
                    cell.setTemperature(temp);
                    if (rainBuf == null) {
                        double cRain = (areaClim[i] > 0.0) ? (sumClimRain[i] / areaClim[i])
                                : evaluateScalarCubature(climateBuf, lat, lng, fallbackRes, argb -> (((argb >> 8) & 0xFF) / 255.0) * 3000.0);
                        cell.setRainfall(cRain);
                    }
                }

                if (rainBuf != null) {
                    double rain = (areaRain[i] > 0.0) ? (sumRain[i] / areaRain[i])
                            : evaluateScalarCubature(rainBuf, lat, lng, fallbackRes, argb -> argbToMaxChannelNorm(argb) * 3000.0);
                    cell.setRainfall(rain);
                }

                if (seasonBuf != null) {
                    double seasonalDelta = (areaSeas[i] > 0.0) ? (sumSeas[i] / areaSeas[i])
                            : evaluateScalarCubature(seasonBuf, lat, lng, fallbackRes, argb -> argbToMaxChannelNorm(argb) * 50.0);
                    double baseT = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
                    cell.setTemperature(baseT + (Math.sin(Math.toRadians(lat)) * seasonalDelta * 0.5));
                }
            }
            return;
        }

        // Fine resolution / small raster cubature path
        int res = Math.max(0, Math.min(10, topo.h3Res() >= 0 ? topo.h3Res() : 4));
        for (H3Cell cell : cells) {
            if (cell == null || cell.getLatitude() == null || cell.getLongitude() == null) continue;
            double lat = cell.getLatitude();
            double lng = cell.getLongitude();

            if (elevBuf != null) {
                double elev = evaluateScalarCubature(elevBuf, lat, lng, res,
                        argb -> PlanetPreset.waterLevelToMeters(argbToMaxChannelNorm(argb), minAlt, maxAlt));
                cell.setElevation(elev);
            }

            if (biomeBuf != null) {
                cell.setBiome(evaluateBiomeCubature(biomeBuf, lat, lng, res));
            } else if (elevBuf != null) {
                applyElevationDerivedBiome(cell, maxAlt);
            }

            if (resBuf != null) {
                double rNorm = evaluateScalarCubature(resBuf, lat, lng, res, argb -> ((argb >> 16) & 0xFF) / 255.0);
                double gNorm = evaluateScalarCubature(resBuf, lat, lng, res, argb -> ((argb >> 8) & 0xFF) / 255.0);
                double bNorm = evaluateScalarCubature(resBuf, lat, lng, res, argb -> (argb & 0xFF) / 255.0);
                applyResourceNorms(cell, rNorm, gNorm, bNorm);
            }

            if (climateBuf != null) {
                double temp = evaluateScalarCubature(climateBuf, lat, lng, res,
                        argb -> -50.0 + (((argb >> 16) & 0xFF) / 255.0) * 100.0);
                cell.setTemperature(temp);
                if (rainBuf == null) {
                    double rain = evaluateScalarCubature(climateBuf, lat, lng, res,
                            argb -> (((argb >> 8) & 0xFF) / 255.0) * 3000.0);
                    cell.setRainfall(rain);
                }
            }

            if (rainBuf != null) {
                double rain = evaluateScalarCubature(rainBuf, lat, lng, res,
                        argb -> argbToMaxChannelNorm(argb) * 3000.0);
                cell.setRainfall(rain);
            }

            if (seasonBuf != null) {
                double seasonalDelta = evaluateScalarCubature(seasonBuf, lat, lng, res,
                        argb -> argbToMaxChannelNorm(argb) * 50.0);
                double baseT = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
                cell.setTemperature(baseT + (Math.sin(Math.toRadians(lat)) * seasonalDelta * 0.5));
            }
        }
    }

    // -------------------------------------------------------------------------
    // 7-Point Hexagonal Cubature + Bilinear Continuous Interpolation
    // -------------------------------------------------------------------------

    private static double evaluateScalarCubature(
            RasterArgbBuffer buf, double lat, double lon, int res, IntToDoubleScalar extractor) {
        double radiusDeg = H3_RADIUS_DEG[Math.clamp(res, 0, H3_RADIUS_DEG.length - 1)] * 0.6;
        double cosLat = Math.max(0.15, Math.cos(Math.toRadians(lat)));

        double sum = 0.25 * buf.sampleBilinearChannel(lat, lon, extractor);
        for (int k = 0; k < 6; k++) {
            double ang = k * (Math.PI / 3.0);
            double sLat = Math.clamp(lat + radiusDeg * Math.sin(ang), -89.99, 89.99);
            double sLon = normalizeLon(lon + (radiusDeg / cosLat) * Math.cos(ang));
            sum += 0.125 * buf.sampleBilinearChannel(sLat, sLon, extractor);
        }
        return sum;
    }

    private static Biome evaluateBiomeCubature(RasterArgbBuffer buf, double lat, double lon, int res) {
        double radiusDeg = H3_RADIUS_DEG[Math.clamp(res, 0, H3_RADIUS_DEG.length - 1)] * 0.6;
        double cosLat = Math.max(0.15, Math.cos(Math.toRadians(lat)));

        double[] votes = new double[Biome.VALUES.length];
        Biome centerBiome = ImageMapLoader.matchBiomeArgb(buf.sampleNearestArgb(lat, lon));
        votes[centerBiome.ordinal()] += 0.251; // Slight tie-breaker favors centroid

        for (int k = 0; k < 6; k++) {
            double ang = k * (Math.PI / 3.0);
            double sLat = Math.clamp(lat + radiusDeg * Math.sin(ang), -89.99, 89.99);
            double sLon = normalizeLon(lon + (radiusDeg / cosLat) * Math.cos(ang));
            Biome ringBiome = ImageMapLoader.matchBiomeArgb(buf.sampleNearestArgb(sLat, sLon));
            votes[ringBiome.ordinal()] += 0.125;
        }
        Biome best = selectModalBiome(votes);
        return best != null ? best : centerBiome;
    }

    private static Biome selectModalBiome(double[] votes) {
        Biome best = null;
        double bestW = 0.0;
        for (int b = 0; b < Biome.VALUES.length; b++) {
            if (votes[b] > bestW) {
                bestW = votes[b];
                best = Biome.VALUES[b];
            }
        }
        return best;
    }

    private static void applyElevationDerivedBiome(H3Cell cell, double maxAlt) {
        double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
        if (elev < 0) cell.setBiome(Biome.OCEAN);
        else if (elev > 0.85 * maxAlt) cell.setBiome(Biome.SNOW);
        else if (elev > 0.65 * maxAlt) cell.setBiome(Biome.MOUNTAINS);
        else if (elev > 0.35 * maxAlt) cell.setBiome(Biome.HILLS);
        else cell.setBiome(Biome.PLAINS);
    }

    private static void applyResourceNorms(H3Cell cell, double rNorm, double gNorm, double bNorm) {
        cell.setResourceMetal(rNorm * 1000.0);
        cell.setResourceClay((rNorm * 0.5 + gNorm * 0.5) * 500.0);
        cell.setWoodResource(gNorm * 1000.0);
        cell.setBiomassNatural(gNorm * 1000.0);
        cell.setFoodResource(gNorm * 500.0);
        cell.setWaterResource(bNorm * 1000.0);
        cell.setBiomassFish(bNorm * 1000.0);
    }

    static int sampleNormalized(RasterArgbBuffer buf, double u, double v) {
        return H3RasterBinningCoordinator.sampleNormalized(buf, u, v);
    }

    public static double argbToGrayNorm(int argb) {
        return H3RasterBinningCoordinator.argbToGrayNorm(argb);
    }

    public static double argbToMaxChannelNorm(int argb) {
        return H3RasterBinningCoordinator.argbToMaxChannelNorm(argb);
    }

    public static double normalizeLon(double lon) {
        return H3RasterBinningCoordinator.normalizeLon(lon);
    }
}
