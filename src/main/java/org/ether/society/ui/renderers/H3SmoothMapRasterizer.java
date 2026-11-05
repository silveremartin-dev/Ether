/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.renderers;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.ether.society.database.H3Cell;

import java.util.List;

/**
 * 2D spatial rasterizer for continuous smooth texture interpolation across H3 cells.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class H3SmoothMapRasterizer {

    private H3SmoothMapRasterizer() {}

    /**
     * Rasterizes a continuous smooth 2D equirectangular texture from discrete H3 cells.
     */
    public static WritableImage rasterizeSmoothMap(List<H3Cell> cells, H3CellColorResolver colorResolver,
                                                   boolean showHillshading, boolean showSolarTerminator,
                                                   double minLat, double maxLat, double minLng, double maxLng) {
        if (cells == null || cells.isEmpty()) return null;

        int width = 720;
        int height = 360;
        WritableImage img = new WritableImage(width, height);
        PixelWriter pw = img.getPixelWriter();

        float[][] rGrid = new float[height][width];
        float[][] gGrid = new float[height][width];
        float[][] bGrid = new float[height][width];
        float[][] wGrid = new float[height][width];

        double spanLng = maxLng - minLng;
        double spanLat = maxLat - minLat;
        if (spanLng <= 0) spanLng = 360.0;
        if (spanLat <= 0) spanLat = 180.0;

        double kernelRadiusPx = Math.max(3.0, ((double) width / Math.sqrt(cells.size())) * 1.6);
        int rInt = (int) Math.ceil(kernelRadiusPx);

        for (int i = 0; i < cells.size(); i++) {
            H3Cell cell = cells.get(i);
            double lng = cell.getLongitude();
            double lat = cell.getLatitude();
            if (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng) continue;

            int cx = (int) Math.clamp(Math.floor(((lng - minLng) / spanLng) * width), 0, width - 1);
            int cy = (int) Math.clamp(Math.floor(((maxLat - lat) / spanLat) * height), 0, height - 1);

            Color col = colorResolver.getBufferOrCellColor(cell);
            if (showHillshading || showSolarTerminator) {
                double f = (showHillshading ? colorResolver.computeHillshadeFactor(cell) : 1.0)
                         * (showSolarTerminator ? colorResolver.computeSolarTerminatorFactor(cell) : 1.0);
                col = Color.color(
                        Math.clamp(col.getRed() * f, 0.0, 1.0),
                        Math.clamp(col.getGreen() * f, 0.0, 1.0),
                        Math.clamp(col.getBlue() * f, 0.0, 1.0),
                        col.getOpacity()
                );
            }

            float r = (float) col.getRed();
            float g = (float) col.getGreen();
            float b = (float) col.getBlue();

            int minX = Math.max(0, cx - rInt);
            int maxX = Math.min(width - 1, cx + rInt);
            int minY = Math.max(0, cy - rInt);
            int maxY = Math.min(height - 1, cy + rInt);

            for (int py = minY; py <= maxY; py++) {
                double dy = py - cy;
                for (int px = minX; px <= maxX; px++) {
                    double dx = px - cx;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist <= kernelRadiusPx) {
                        float w = (float) Math.pow(1.0 - (dist / kernelRadiusPx), 2.0);
                        rGrid[py][px] += r * w;
                        gGrid[py][px] += g * w;
                        bGrid[py][px] += b * w;
                        wGrid[py][px] += w;
                    }
                }
            }
        }

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                float w = wGrid[py][px];
                if (w > 0.0001f) {
                    float r = Math.min(1.0f, Math.max(0.0f, rGrid[py][px] / w));
                    float g = Math.min(1.0f, Math.max(0.0f, gGrid[py][px] / w));
                    float b = Math.min(1.0f, Math.max(0.0f, bGrid[py][px] / w));
                    pw.setColor(px, py, Color.color(r, g, b, 1.0));
                } else {
                    pw.setColor(px, py, Color.rgb(15, 23, 42));
                }
            }
        }

        return img;
    }
}
