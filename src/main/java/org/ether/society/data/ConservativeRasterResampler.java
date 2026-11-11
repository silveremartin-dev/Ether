/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.data;

/**
 * <h1>ConservativeRasterResampler</h1>
 * <p>
 * State-of-the-art spherical equirectangular GIS regridding engine implementing first-order
 * area-conservative overlap integration for extensive fields (population counts, agricultural
 * land-use areas) and spherical-area-weighted conservative averaging (with NoData masking)
 * for intensive continuous geophysical fields (elevation, temperature, precipitation, seasonality).
 * </p>
 *
 * <h2>Mathematical Formulations</h2>
 * <ul>
 *   <li><b>Spherical pixel solid-angle area</b> on a sphere of IUGG mean radius $R_\oplus = 6371.0088\,\text{km}$:
 *       $$A_{\text{px}}(y) = \frac{2\pi}{W}\, R_\oplus^2 \big(\sin\phi_+(y) - \sin\phi_-(y)\big)$$</li>
 *   <li><b>Exact conservative transfer fraction</b> of source cell $(r,c)$ into target pixel $(y,x)$:
 *       $$\omega_{r,c \to y,x} = f_\lambda(x,c) \cdot \frac{\max\big(0,\, \min(\sin\phi_r^+, \sin\Phi_y^+) - \max(\sin\phi_r^-, \sin\Phi_y^-)\big)}{\sin\phi_r^+ - \sin\phi_r^-}$$
 *       satisfying $\sum_{y,x} \omega_{r,c \to y,x} \equiv 1$ identically, guaranteeing exact global
 *       mass/count conservation without peak-blending bias or high-latitude area distortion.</li>
 *   <li><b>Intensive area-weighted aggregation</b> with valid-mask preservation:
 *       $$\bar{q}_{y,x} = \frac{\sum_{(r,c)\in\mathcal{V}} q_{r,c}\,\Omega_{y,x;r,c}}{\sum_{(r,c)\in\mathcal{V}} \Omega_{y,x;r,c}}$$
 *       where $\Omega_{y,x;r,c} = f_\lambda(x,c)\cdot\Delta(\sin\phi)_{y,r}$ is the exact intersecting
 *       spherical solid angle and $\mathcal{V}$ excludes NoData/ocean masks.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ConservativeRasterResampler {

    /** IUGG mean Earth volumetric radius in kilometers. */
    public static final double EARTH_MEAN_RADIUS_KM = 6371.0088;

    private ConservativeRasterResampler() {
        // Pure static utility
    }

    /**
     * Functional predicate for scalar validity checks during masked intensive resampling.
     */
    @FunctionalInterface
    public interface FloatValidityPredicate {
        /**
         * Returns {@code true} if the source sample is physically valid (not NoData).
         *
         * @param value source cell scalar value
         * @return {@code true} when valid
         */
        boolean isValid(float value);
    }

    /**
     * Computes the exact spherical surface area in $\text{km}^2$ of a single equirectangular
     * pixel at row {@code y} in a global $[-180^\circ, 180^\circ] \times [-90^\circ, 90^\circ]$ grid
     * indexed top-down ($y=0$ at $+90^\circ\text{N}$).
     *
     * @param y      pixel row index in $[0, \text{height}-1]$
     * @param width  grid width in pixels
     * @param height grid height in pixels
     * @return exact spherical quadrangle area in $\text{km}^2$
     */
    public static double sphericalPixelAreaKm2(int y, int width, int height) {
        if (width <= 0 || height <= 0 || y < 0 || y >= height) {
            return 0.0;
        }
        double phiTop = Math.PI * 0.5 - ((double) y / height) * Math.PI;
        double phiBot = Math.PI * 0.5 - ((double) (y + 1) / height) * Math.PI;
        double sinSpan = Math.max(0.0, Math.sin(phiTop) - Math.sin(phiBot));
        double dLonRad = (2.0 * Math.PI) / width;
        return EARTH_MEAN_RADIUS_KM * EARTH_MEAN_RADIUS_KM * dLonRad * sinSpan;
    }

    /**
     * Resamples an extensive global grid (counts or physical areas per source cell) onto a target
     * equirectangular grid with exact global mass conservation via first-order spherical box overlap.
     *
     * @param src    source array indexed {@code [srcH][srcW]} top-down (north-to-south)
     * @param srcW   source column count
     * @param srcH   source row count
     * @param nodata source NoData sentinel (cells {@code <= 0} or matching {@code nodata} are ignored)
     * @param dstW   target width
     * @param dstH   target height
     * @return conservatively regridded array {@code [dstH][dstW]} preserving $\sum \text{dst} \equiv \sum \text{src}$
     */
    public static float[][] resampleExtensive(float[][] src, int srcW, int srcH, float nodata, int dstW, int dstH) {
        if (src == null || srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0) {
            return new float[Math.max(0, dstH)][Math.max(0, dstW)];
        }

        double[] srcSinTop = new double[srcH];
        double[] srcSinBot = new double[srcH];
        double[] srcSinSpan = new double[srcH];
        for (int r = 0; r < srcH; r++) {
            srcSinTop[r] = Math.sin(Math.PI * 0.5 - ((double) r / srcH) * Math.PI);
            srcSinBot[r] = Math.sin(Math.PI * 0.5 - ((double) (r + 1) / srcH) * Math.PI);
            srcSinSpan[r] = Math.max(1e-15, srcSinTop[r] - srcSinBot[r]);
        }

        // Precompute longitude overlap mapping from each target col x -> source cols [c0..c1]
        double cScale = (double) srcW / dstW;
        int[] cStart = new int[dstW];
        int[] cEnd = new int[dstW];
        double[][] cxWeights = new double[dstW][];
        for (int x = 0; x < dstW; x++) {
            double cMin = x * cScale;
            double cMax = (x + 1) * cScale;
            int c0 = Math.max(0, (int) Math.floor(cMin));
            int c1 = Math.min(srcW - 1, (int) Math.floor(Math.nextDown(cMax)));
            if (c1 < c0) c1 = c0;
            cStart[x] = c0;
            cEnd[x] = c1;
            double[] w = new double[c1 - c0 + 1];
            for (int c = c0; c <= c1; c++) {
                double overlap = Math.min(c + 1.0, cMax) - Math.max((double) c, cMin);
                w[c - c0] = Math.max(0.0, overlap);
            }
            cxWeights[x] = w;
        }

        // Target rows using double accumulation to avoid catastrophic rounding drift across pixels
        double rScale = (double) srcH / dstH;
        float[][] dst = new float[dstH][dstW];
        double[] rowAccum = new double[dstW];

        for (int y = 0; y < dstH; y++) {
            double phiTop = Math.sin(Math.PI * 0.5 - ((double) y / dstH) * Math.PI);
            double phiBot = Math.sin(Math.PI * 0.5 - ((double) (y + 1) / dstH) * Math.PI);

            int r0 = Math.max(0, (int) Math.floor(y * rScale));
            int r1 = Math.min(srcH - 1, (int) Math.floor(Math.nextDown((y + 1) * rScale)));
            if (r1 < r0) r1 = r0;

            java.util.Arrays.fill(rowAccum, 0.0);

            for (int r = r0; r <= r1; r++) {
                double sinOverlap = Math.min(srcSinTop[r], phiTop) - Math.max(srcSinBot[r], phiBot);
                if (sinOverlap <= 0.0) continue;
                double fy = sinOverlap / srcSinSpan[r];
                float[] srcRow = src[r];
                if (srcRow == null) continue;

                for (int x = 0; x < dstW; x++) {
                    int c0 = cStart[x];
                    int c1 = cEnd[x];
                    double[] wx = cxWeights[x];
                    double colSum = 0.0;
                    for (int c = c0; c <= c1; c++) {
                        float v = srcRow[c];
                        if (v > 0.0f && Float.isFinite(v) && v != nodata) {
                            colSum += v * wx[c - c0];
                        }
                    }
                    rowAccum[x] += colSum * fy;
                }
            }

            float[] dstRow = dst[y];
            for (int x = 0; x < dstW; x++) {
                dstRow[x] = (float) rowAccum[x];
            }
        }
        return dst;
    }

    /**
     * Resamples an intensive continuous global grid (temperature, precipitation, seasonality, elevation)
     * using spherical-area-weighted box-overlap averaging, masking out invalid/ocean cells so NoData
     * values do not bleed across coastal boundaries.
     *
     * @param src           source grid indexed {@code [srcH][srcW]}
     * @param srcW          source width
     * @param srcH          source height
     * @param dstW          target width
     * @param dstH          target height
     * @param validity      validity predicate identifying physical values vs NoData sentinels
     * @param fallbackValue fallback value assigned when a target pixel has zero valid source overlap
     *                      (pass {@link Float#NaN} to detect oceanic/unmapped pixels downstream)
     * @return conservatively averaged array {@code [dstH][dstW]}
     */
    public static float[][] resampleIntensiveMasked(
            float[][] src, int srcW, int srcH, int dstW, int dstH,
            FloatValidityPredicate validity, float fallbackValue) {
        if (src == null || srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0) {
            return new float[Math.max(0, dstH)][Math.max(0, dstW)];
        }

        double[] srcSinTop = new double[srcH];
        double[] srcSinBot = new double[srcH];
        for (int r = 0; r < srcH; r++) {
            srcSinTop[r] = Math.sin(Math.PI * 0.5 - ((double) r / srcH) * Math.PI);
            srcSinBot[r] = Math.sin(Math.PI * 0.5 - ((double) (r + 1) / srcH) * Math.PI);
        }

        double cScale = (double) srcW / dstW;
        int[] cStart = new int[dstW];
        int[] cEnd = new int[dstW];
        double[][] cxWeights = new double[dstW][];
        for (int x = 0; x < dstW; x++) {
            double cMin = x * cScale;
            double cMax = (x + 1) * cScale;
            int c0 = Math.max(0, (int) Math.floor(cMin));
            int c1 = Math.min(srcW - 1, (int) Math.floor(Math.nextDown(cMax)));
            if (c1 < c0) c1 = c0;
            cStart[x] = c0;
            cEnd[x] = c1;
            double[] w = new double[c1 - c0 + 1];
            for (int c = c0; c <= c1; c++) {
                w[c - c0] = Math.max(0.0, Math.min(c + 1.0, cMax) - Math.max((double) c, cMin));
            }
            cxWeights[x] = w;
        }

        double rScale = (double) srcH / dstH;
        float[][] dst = new float[dstH][dstW];
        double[] rowVal = new double[dstW];
        double[] rowWgt = new double[dstW];

        for (int y = 0; y < dstH; y++) {
            double phiTop = Math.sin(Math.PI * 0.5 - ((double) y / dstH) * Math.PI);
            double phiBot = Math.sin(Math.PI * 0.5 - ((double) (y + 1) / dstH) * Math.PI);

            int r0 = Math.max(0, (int) Math.floor(y * rScale));
            int r1 = Math.min(srcH - 1, (int) Math.floor(Math.nextDown((y + 1) * rScale)));
            if (r1 < r0) r1 = r0;

            java.util.Arrays.fill(rowVal, 0.0);
            java.util.Arrays.fill(rowWgt, 0.0);

            for (int r = r0; r <= r1; r++) {
                double sinOverlap = Math.min(srcSinTop[r], phiTop) - Math.max(srcSinBot[r], phiBot);
                if (sinOverlap <= 0.0) continue;
                float[] srcRow = src[r];
                if (srcRow == null) continue;

                for (int x = 0; x < dstW; x++) {
                    int c0 = cStart[x];
                    int c1 = cEnd[x];
                    double[] wx = cxWeights[x];
                    for (int c = c0; c <= c1; c++) {
                        float v = srcRow[c];
                        if (Float.isFinite(v) && (validity == null || validity.isValid(v))) {
                            double w = sinOverlap * wx[c - c0];
                            rowVal[x] += v * w;
                            rowWgt[x] += w;
                        }
                    }
                }
            }

            float[] dstRow = dst[y];
            for (int x = 0; x < dstW; x++) {
                dstRow[x] = (rowWgt[x] > 0.0) ? (float) (rowVal[x] / rowWgt[x]) : fallbackValue;
            }
        }
        return dst;
    }

    /**
     * Streaming spherical-area-weighted intensive accumulator for ultra-high-resolution tiled rasters
     * (such as NOAA ETOPO 2022 at $21600 \times 10800$) that cannot be materialized whole in heap memory.
     */
    public static final class IntensiveStreamingAccumulator {
        private final int dstW;
        private final int dstH;
        private final int srcW;
        private final int srcH;
        private final double[] sumVal;
        private final double[] sumWgt;
        private final int[] gy0;
        private final int[] gy1;
        private final double[] sinTopDst;
        private final double[] sinBotDst;
        private final double[] sinTopSrc;
        private final double[] sinBotSrc;
        private final int[] gx0;
        private final int[] gx1;

        /**
         * Initializes a streaming spherical-area accumulator from source dimensions to target dimensions.
         *
         * @param srcW source raster width
         * @param srcH source raster height
         * @param dstW target raster width
         * @param dstH target raster height
         */
        public IntensiveStreamingAccumulator(int srcW, int srcH, int dstW, int dstH) {
            this.srcW = srcW;
            this.srcH = srcH;
            this.dstW = dstW;
            this.dstH = dstH;
            this.sumVal = new double[dstW * dstH];
            this.sumWgt = new double[dstW * dstH];

            this.sinTopDst = new double[dstH];
            this.sinBotDst = new double[dstH];
            for (int y = 0; y < dstH; y++) {
                sinTopDst[y] = Math.sin(Math.PI * 0.5 - ((double) y / dstH) * Math.PI);
                sinBotDst[y] = Math.sin(Math.PI * 0.5 - ((double) (y + 1) / dstH) * Math.PI);
            }

            double yScale = (double) dstH / srcH;
            this.gy0 = new int[srcH];
            this.gy1 = new int[srcH];
            this.sinTopSrc = new double[srcH];
            this.sinBotSrc = new double[srcH];
            for (int r = 0; r < srcH; r++) {
                sinTopSrc[r] = Math.sin(Math.PI * 0.5 - ((double) r / srcH) * Math.PI);
                sinBotSrc[r] = Math.sin(Math.PI * 0.5 - ((double) (r + 1) / srcH) * Math.PI);
                int y0 = Math.max(0, (int) Math.floor(r * yScale));
                int y1 = Math.min(dstH - 1, (int) Math.floor(Math.nextDown((r + 1) * yScale)));
                if (y1 < y0) y1 = y0;
                gy0[r] = y0;
                gy1[r] = y1;
            }

            double xScale = (double) dstW / srcW;
            this.gx0 = new int[srcW];
            this.gx1 = new int[srcW];
            for (int c = 0; c < srcW; c++) {
                int x0 = Math.max(0, (int) Math.floor(c * xScale));
                int x1 = Math.min(dstW - 1, (int) Math.floor(Math.nextDown((c + 1) * xScale)));
                if (x1 < x0) x1 = x0;
                gx0[c] = x0;
                gx1[c] = x1;
            }
        }

        /**
         * Accumulates one source tile directly using exact spherical box overlap.
         *
         * @param tileFloats row-major tile buffer of dimensions {@code [tileH * tileW]}
         * @param minX       global source starting X coordinate of tile
         * @param minY       global source starting Y coordinate of tile
         * @param maxX       exclusive global source ending X coordinate (clamped to {@code srcW})
         * @param maxY       exclusive global source ending Y coordinate (clamped to {@code srcH})
         * @param tileW      allocated tile stride width
         */
        public void addTile(float[] tileFloats, int minX, int minY, int maxX, int maxY, int tileW) {
            double xScale = (double) dstW / srcW;
            for (int py = minY; py < maxY; py++) {
                int tileRowOff = (py - minY) * tileW;
                int y0 = gy0[py];
                int y1 = gy1[py];
                double sTop = sinTopSrc[py];
                double sBot = sinBotSrc[py];

                for (int px = minX; px < maxX; px++) {
                    float val = tileFloats[tileRowOff + (px - minX)];
                    if (!Float.isFinite(val)) continue;

                    double gMinX = px * xScale;
                    double gMaxX = (px + 1) * xScale;
                    int x0 = gx0[px];
                    int x1 = gx1[px];

                    if (y0 == y1 && x0 == x1) {
                        double w = (sTop - sBot) * (gMaxX - gMinX);
                        int idx = y0 * dstW + x0;
                        sumVal[idx] += val * w;
                        sumWgt[idx] += w;
                    } else {
                        for (int gy = y0; gy <= y1; gy++) {
                            double fy = Math.min(sTop, sinTopDst[gy]) - Math.max(sBot, sinBotDst[gy]);
                            if (fy <= 0.0) continue;
                            int rowOff = gy * dstW;
                            for (int gx = x0; gx <= x1; gx++) {
                                double fx = Math.min(gMaxX, gx + 1.0) - Math.max(gMinX, (double) gx);
                                if (fx <= 0.0) continue;
                                double w = fy * fx;
                                int idx = rowOff + gx;
                                sumVal[idx] += val * w;
                                sumWgt[idx] += w;
                            }
                        }
                    }
                }
            }
        }

        /**
         * Finalizes accumulated weights into the target {@code [dstH][dstW]} grid.
         *
         * @return conservatively area-averaged grid
         */
        public float[][] toGrid() {
            float[][] out = new float[dstH][dstW];
            for (int y = 0; y < dstH; y++) {
                int rowOff = y * dstW;
                float[] outRow = out[y];
                for (int x = 0; x < dstW; x++) {
                    int idx = rowOff + x;
                    double w = sumWgt[idx];
                    outRow[x] = (w > 0.0) ? (float) (sumVal[idx] / w) : 0.0f;
                }
            }
            return out;
        }
    }
}
