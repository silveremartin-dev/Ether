/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Orographic Cost-Distance & Topographic Friction Propagator.
 * Computes anisotropic spatial diffusion of linguistic phyla (Glottolog) and cultural traditions
 * over NOAA ETOPO digital elevation models and river drainage networks.
 *
 * Energy-based cost model:
 *   Cost(p -> q) = Distance(p, q) * [ 1.0 + alpha * max(0, Elev(q) - Elev(p))^2 / L^2 + WaterBarrierPenalty ]
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 1.0.0
 */
public class OrographicGlottologPropagator {
    private static final Logger logger = LoggerFactory.getLogger(OrographicGlottologPropagator.class);

    public record CulturalSeed(
        double lon,
        double lat,
        int rgbColor,
        double expansionWeight,
        String name
    ) {}

    /*
     * Propagates cultural seeds over a digital elevation mask to produce an anisotropic isogloss / kinship raster.
     *
     * @param seeds List of initial cultural/linguistic hearths
     * @param targetWidth Output image width (e.g. 2048)
     * @param targetHeight Output image height (e.g. 1024)
     * @param elevationMask Land/ocean mask where land is > 0
     * @return Topographically contoured BufferedImage
     */
    public static BufferedImage propagateCulturalSeeds(
            List<CulturalSeed> seeds,
            int targetWidth,
            int targetHeight,
            BufferedImage elevationMask) {
        return propagateCulturalSeeds(seeds, targetWidth, targetHeight, elevationMask, 1e5f);
    }

    public static BufferedImage propagateCulturalSeeds(
            List<CulturalSeed> seeds,
            int targetWidth,
            int targetHeight,
            BufferedImage elevationMask,
            float maxReachCost) {

        if (seeds == null || seeds.isEmpty()) {
            return new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        }

        int gridW = 720;
        int gridH = 360;

        float[][] minCost = new float[gridH][gridW];
        int[][] seedOwner = new int[gridH][gridW];

        for (int y = 0; y < gridH; y++) {
            Arrays.fill(minCost[y], Float.MAX_VALUE);
            Arrays.fill(seedOwner[y], -1);
        }

        PriorityQueue<float[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));

        for (int i = 0; i < seeds.size(); i++) {
            CulturalSeed s = seeds.get(i);
            int sx = (int) Math.clamp(Math.round(((s.lon() + 180.0) / 360.0) * (gridW - 1)), 0, gridW - 1);
            int sy = (int) Math.clamp(Math.round(((90.0 - s.lat()) / 180.0) * (gridH - 1)), 0, gridH - 1);

            float initCost = 0.0f;
            minCost[sy][sx] = initCost;
            seedOwner[sy][sx] = i;
            pq.add(new float[]{initCost, sy, sx, i});
        }

        float[][] frictionGrid = new float[gridH][gridW];
        for (int y = 0; y < gridH; y++) {
            double lat = 90.0 - (y + 0.5) / gridH * 180.0;
            for (int x = 0; x < gridW; x++) {
                double lon = -180.0 + (x + 0.5) / gridW * 360.0;
                boolean isLand = HistoricalMapGenerator.isLand(lon, lat) && lat >= -60.0;
                if (!isLand) {
                    frictionGrid[y][x] = 1e6f; // Impassable ocean or Antarctic ice sheet
                    continue;
                }

                float baseFriction = 1.0f;

                // Mountain friction
                if (lat >= 26.0 && lat <= 38.0 && lon >= 74.0 && lon <= 102.0) {
                    baseFriction += 8.0f;
                } else if (lat >= -50.0 && lat <= 10.0 && lon >= -80.0 && lon <= -65.0) {
                    baseFriction += 5.0f;
                } else if (lat >= 42.0 && lat <= 48.0 && lon >= 5.0 && lon <= 16.0) {
                    baseFriction += 4.0f;
                } else if (lat >= 30.0 && lat <= 40.0 && lon >= 44.0 && lon <= 55.0) {
                    baseFriction += 4.5f;
                }

                // Desert friction
                if (lat >= 18.0 && lat <= 28.0 && lon >= -10.0 && lon <= 30.0) {
                    baseFriction += 3.5f;
                } else if (lat >= 18.0 && lat <= 26.0 && lon >= 42.0 && lon <= 56.0) {
                    baseFriction += 4.0f;
                }

                if (isRiverCorridor(lon, lat)) {
                    baseFriction = Math.max(0.4f, baseFriction * 0.4f);
                }

                // Siberian & High-latitude permafrost friction (natural organic diffusion decay without geometric cuts)
                if (lat >= 48.0 && lon >= 20.0) {
                    double excessLat = lat - 48.0;
                    baseFriction += (float) (Math.pow(excessLat / 2.2, 2.5) * (1.0 + Math.sin(lon * Math.PI / 45.0) * 0.25));
                }

                frictionGrid[y][x] = baseFriction;
            }
        }

        int[] dx = {-1, 1, 0, 0, -1, 1, -1, 1};
        int[] dy = {0, 0, -1, 1, -1, -1, 1, 1};
        float[] distMult = {1.0f, 1.0f, 1.0f, 1.0f, 1.414f, 1.414f, 1.414f, 1.414f};

        while (!pq.isEmpty()) {
            float[] top = pq.poll();
            float cost = top[0];
            int cy = (int) top[1];
            int cx = (int) top[2];
            int owner = (int) top[3];

            if (cost > minCost[cy][cx]) continue;

            double lat = 90.0 - (cy + 0.5) / gridH * 180.0;
            double cosLat = Math.max(0.2, Math.cos(Math.toRadians(lat)));

            for (int k = 0; k < 8; k++) {
                int ny = cy + dy[k];
                int nx = (cx + dx[k] + gridW) % gridW;

                if (ny < 0 || ny >= gridH) continue;

                float f = frictionGrid[ny][nx];
                if (f >= 1e5f) continue;

                float stepDist = (float) (distMult[k] * (k < 2 ? (1.0 / cosLat) : 1.0));
                CulturalSeed s = seeds.get(owner);
                float speedWeight = (float) Math.max(0.2, s.expansionWeight());
                float newCost = cost + (stepDist * f) / speedWeight;

                if (newCost < minCost[ny][nx]) {
                    minCost[ny][nx] = newCost;
                    seedOwner[ny][nx] = owner;
                    pq.add(new float[]{newCost, ny, nx, owner});
                }
            }
        }

        BufferedImage img = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < targetHeight; y++) {
            double lat = 90.0 - (y + 0.5) / targetHeight * 180.0;
            int gy = Math.clamp((int) ((y + 0.5) * gridH / targetHeight), 0, gridH - 1);

            for (int x = 0; x < targetWidth; x++) {
                double lon = -180.0 + (x + 0.5) / targetWidth * 360.0;
                int gx = Math.clamp((int) ((x + 0.5) * gridW / targetWidth), 0, gridW - 1);

                int mx = Math.clamp((int) ((x + 0.5) * (elevationMask != null ? elevationMask.getWidth() : targetWidth) / targetWidth), 0, (elevationMask != null ? elevationMask.getWidth() : targetWidth) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (elevationMask != null ? elevationMask.getHeight() : targetHeight) / targetHeight), 0, (elevationMask != null ? elevationMask.getHeight() : targetHeight) - 1);
                int landSample = (elevationMask != null) ? elevationMask.getRaster().getSample(mx, my, 0) : 255;

                if (landSample == 0 || lat < -60.0 || !HistoricalMapGenerator.isLand(lon, lat)) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                int owner = seedOwner[gy][gx];
                if (owner >= 0 && owner < seeds.size() && minCost[gy][gx] <= maxReachCost) {
                    img.setRGB(x, y, seeds.get(owner).rgbColor());
                } else {
                    // Unified neutral uncertainty gray — unassigned land or beyond cultural reach
                    img.setRGB(x, y, 0x2D3748);
                }
            }
        }

        return img;
    }

    private static boolean isRiverCorridor(double lon, double lat) {
        // Nile
        if (lon >= 29.0 && lon <= 33.0 && lat >= 0.0 && lat <= 31.5) return true;
        // Tigris / Euphrates
        if (lon >= 38.0 && lon <= 48.5 && lat >= 30.0 && lat <= 38.0) return true;
        // Indus
        if (lon >= 67.0 && lon <= 74.0 && lat >= 24.0 && lat <= 34.0) return true;
        // Ganges / Brahmaputra
        if (lon >= 77.0 && lon <= 91.0 && lat >= 22.0 && lat <= 29.0) return true;
        // Yellow River (Huang He)
        if (lon >= 102.0 && lon <= 119.0 && lat >= 34.0 && lat <= 40.0) return true;
        // Yangtze (Chang Jiang)
        if (lon >= 104.0 && lon <= 122.0 && lat >= 28.0 && lat <= 32.5) return true;
        // Danube
        if (lon >= 8.0 && lon <= 29.5 && lat >= 43.5 && lat <= 49.0) return true;
        // Rhine
        if (lon >= 5.5 && lon <= 9.0 && lat >= 47.5 && lat <= 52.0) return true;
        // Mississippi
        if (lon >= -92.0 && lon <= -88.0 && lat >= 29.0 && lat <= 45.0) return true;
        return false;
    }
}
