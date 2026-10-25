/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Cartographic helper providing subnational kernel blending, elevation land masking,
 * and global trade corridor / logistics hub rasterization for modern epochs.
 */
public final class NaturalEarthSubnationalLayerHelper {

    private NaturalEarthSubnationalLayerHelper() {
        // Utility class
    }

    /**
     * Blends sub-national cultural, linguistic, or ritual centers onto a rasterized map using Gaussian spatial influence.
     *
     * @param img target raster image
     * @param centers array of center definitions: [lon, lat, hexColor, sigmaRadius]
     * @param width image width
     * @param height image height
     * @param elevationMask optional elevation mask
     */
    public static void applySubnationalCenters(BufferedImage img, double[][] centers, int width, int height, BufferedImage elevationMask) {
        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y + 0.5) / height * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = -180.0 + (x + 0.5) / width * 360.0;
                int currentRgb = img.getRGB(x, y) & 0xFFFFFF;
                if (currentRgb == 0x000000) continue;

                double maxInfl = 0.0;
                int bestCol = currentRgb;

                for (double[] sc : centers) {
                    double dLat = lat - sc[1];
                    double dLon = (lon - sc[0]) * Math.cos(Math.toRadians((lat + sc[1]) * 0.5));
                    double d2 = dLat * dLat + dLon * dLon;
                    double sigma = sc[3];
                    double infl = Math.exp(-d2 / (2.0 * sigma * sigma));
                    if (infl > maxInfl) {
                        maxInfl = infl;
                        bestCol = (int) sc[2];
                    }
                }

                if (maxInfl >= 0.45) {
                    img.setRGB(x, y, bestCol);
                }
            }
        }
    }

    /**
     * Masks out ocean and glaciated high-latitude zones according to elevation mask.
     *
     * @param img source raster image
     * @param elevationMask elevation mask
     * @return masked raster image
     */
    public static BufferedImage applyElevationMask(BufferedImage img, BufferedImage elevationMask) {
        if (elevationMask == null) return img;
        int w = img.getWidth();
        int h = img.getHeight();
        int ew = elevationMask.getWidth();
        int eh = elevationMask.getHeight();

        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y + 0.5) / h * 180.0;
            int my = Math.clamp(y * eh / h, 0, eh - 1);
            for (int x = 0; x < w; x++) {
                int mx = Math.clamp(x * ew / w, 0, ew - 1);
                int land = elevationMask.getRaster().getSample(mx, my, 0);
                if (land == 0 || lat < -60.0) {
                    img.setRGB(x, y, 0x000000);
                }
            }
        }
        return img;
    }

    /**
     * Rasterizes maritime shipping lanes, major canals (Suez, Panama), straits, and global logistics hubs.
     *
     * @param year epoch year
     * @param width image width
     * @param height image height
     * @param elevationMask optional elevation mask
     * @return trade corridor raster image
     */
    public static BufferedImage rasterizeModernTradeNetworkMap(long year, int width, int height, BufferedImage elevationMask) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // Global Maritime Shipping Lanes & Chokepoints (lon1, lat1, lon2, lat2, intensityColor, strokeWidth)
        double[][] tradeCorridors = {
            // Asia - Europe Main Arterial via Malacca & Suez
            {121.5, 31.2, 114.2, 22.3, 0x00FFFF, 3.5},
            {114.2, 22.3, 103.8, 1.35, 0x00FFFF, 4.0},
            {103.8, 1.35, 80.2, 6.0, 0x00E5FF, 3.5},
            {80.2, 6.0, 51.5, 12.0, 0x00E5FF, 3.5},
            {51.5, 12.0, 43.5, 12.6, 0xFF3D00, 4.5},
            {43.5, 12.6, 32.5, 27.8, 0x00E5FF, 3.5},
            {32.5, 27.8, 32.3, 31.2, 0xFF3D00, 5.0},
            {32.3, 31.2, 14.5, 36.0, 0x00E5FF, 3.5},
            {14.5, 36.0, -5.6, 36.0, 0x00E5FF, 3.5},
            {-5.6, 36.0, -9.5, 38.7, 0xFF3D00, 4.5},
            {-9.5, 38.7, -4.5, 48.4, 0x00E5FF, 3.5},
            {-4.5, 48.4, 1.4, 51.0, 0xFF3D00, 4.5},
            {1.4, 51.0, 4.0, 51.9, 0x00FFFF, 4.5},

            // Persian Gulf Oil Artery
            {48.0, 29.5, 56.5, 26.5, 0x00E5FF, 4.0},
            {56.5, 26.5, 59.0, 23.5, 0xFF3D00, 5.0},
            {59.0, 23.5, 72.8, 18.9, 0x00E5FF, 3.5},
            {59.0, 23.5, 80.2, 6.0, 0x00E5FF, 3.5},

            // Trans-Pacific Shipping Lanes
            {139.7, 35.6, -122.4, 37.8, 0x00E5FF, 3.5},
            {121.5, 31.2, -118.2, 33.7, 0x00FFFF, 4.0},
            {129.0, 35.1, -123.1, 49.3, 0x00E5FF, 3.0},

            // Trans-Atlantic Shipping Lanes
            {-74.0, 40.7, -5.0, 50.0, 0x00E5FF, 3.5},
            {-80.0, 25.8, -5.6, 36.0, 0x00E5FF, 3.0},

            // Panama Canal Interoceanic Corridor
            {-79.5, 8.9, -79.9, 9.3, 0xFF3D00, 5.0},
            {-118.2, 33.7, -79.5, 8.9, 0x00E5FF, 3.0},
            {-79.9, 9.3, -74.0, 40.7, 0x00E5FF, 3.5},
            {-79.9, 9.3, 4.0, 51.9, 0x00E5FF, 3.0},

            // Cape of Good Hope South Atlantic / Indian
            {18.4, -34.0, 80.2, 6.0, 0x00E5FF, 2.5},
            {-43.2, -22.9, 18.4, -34.0, 0x00E5FF, 2.5},
            {18.4, -34.0, -9.5, 38.7, 0x00E5FF, 2.5},

            // Eurasian Overland Freight Corridors (New Silk Road / Trans-Siberian)
            {116.4, 39.9, 87.6, 43.8, 0xF59E0B, 2.5},
            {87.6, 43.8, 71.4, 51.1, 0xF59E0B, 2.5},
            {71.4, 51.1, 37.6, 55.7, 0xF59E0B, 2.5},
            {37.6, 55.7, 21.0, 52.2, 0xF59E0B, 2.5},
            {21.0, 52.2, 13.4, 52.5, 0xF59E0B, 2.5},
            {13.4, 52.5, 6.9, 50.9, 0xF59E0B, 2.5}
        };

        for (double[] tc : tradeCorridors) {
            double x1 = ((tc[0] + 180.0) / 360.0) * (width - 1);
            double y1 = ((90.0 - tc[1]) / 180.0) * (height - 1);
            double x2 = ((tc[2] + 180.0) / 360.0) * (width - 1);
            double y2 = ((90.0 - tc[3]) / 180.0) * (height - 1);

            g.setColor(new Color((int) tc[4]));
            g.setStroke(new BasicStroke((float) tc[5], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine((int) Math.round(x1), (int) Math.round(y1), (int) Math.round(x2), (int) Math.round(y2));
        }

        // Global Logistics Super-Hub Emporia (lon, lat, sizeRadius)
        double[][] superHubs = {
            {103.8, 1.35, 6.0},
            {121.5, 31.2, 6.0},
            {114.2, 22.3, 5.0},
            {129.0, 35.1, 5.0},
            {4.0, 51.9, 5.5},
            {4.4, 51.2, 5.0},
            {55.3, 25.3, 5.5},
            {-118.2, 33.7, 5.5},
            {-74.0, 40.7, 5.0},
            {-79.5, 8.9, 5.0}
        };

        for (double[] sh : superHubs) {
            double x = ((sh[0] + 180.0) / 360.0) * (width - 1);
            double y = ((90.0 - sh[1]) / 180.0) * (height - 1);
            int r = (int) Math.round(sh[2]);

            g.setColor(Color.WHITE);
            g.fillOval((int) x - r, (int) y - r, r * 2, r * 2);
            g.setColor(new Color(0xFF3D00));
            g.drawOval((int) x - r, (int) y - r, r * 2, r * 2);
        }

        g.dispose();
        return img;
    }
}
