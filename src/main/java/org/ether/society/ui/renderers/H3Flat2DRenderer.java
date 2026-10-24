/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.renderers;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.ether.society.database.H3Cell;

import java.util.List;

/**
 * 2D Equirectangular Flat Map Renderer for H3 hexagonal planetary grids.
 *
 * <p>Extracted from {@code H3MapCanvas} to fulfill SRP and symmetrically separate
 * 2D equirectangular viewport projection, binary latitude culling, cosine latitude stretching,
 * and floating layer elevation shadows from 3D globe rendering.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class H3Flat2DRenderer {

    /**
     * Renders the 2D equirectangular hexagonal grid or continuous raster map onto the canvas.
     */
    public void draw2D(GraphicsContext gc, List<H3Cell> cells,
                       H3CellColorResolver colorResolver, H3Globe3DRenderer globeRenderer,
                       boolean smoothMap, boolean showHexGrid, boolean showFloatingLayers,
                       boolean showHillshading, boolean showSolarTerminator,
                       double minLat, double maxLat, double minLng, double maxLng,
                       double scale, double offsetX, double offsetY,
                       double canvasWidth, double canvasHeight) {
        if (cells == null || cells.isEmpty()) return;

        double viewTopLat = maxLat - (0 - offsetY) / scale;
        double viewBottomLat = maxLat - (canvasHeight - offsetY) / scale;

        double cullMaxLat = Math.min(90, viewTopLat + 10.0);
        double cullMinLat = Math.max(-90, viewBottomLat - 10.0);

        int startIndex = findLatIndex(cells, cullMinLat);

        double lngSpan = Math.max(1.0, maxLng - minLng);
        double cellSpacing = (lngSpan / Math.sqrt(Math.max(1, cells.size()))) * scale;
        double cellSize = Math.max(5.0, cellSpacing * 1.45);
        double radiusY = Math.max(1.0, cellSize / 2.0);

        if (smoothMap) {
            double worldX = offsetX;
            double worldY = offsetY;
            double worldW = (maxLng - minLng) * scale;
            double worldH = (maxLat - minLat) * scale;

            WritableImage smoothImg = globeRenderer.getOrUpdateSmoothMapImage(
                    cells, colorResolver, showHillshading, showSolarTerminator, minLat, maxLat, minLng, maxLng);
            if (smoothImg != null) {
                gc.drawImage(smoothImg, worldX, worldY, worldW, worldH);
            }

            if (showHexGrid && radiusY >= 3.5) {
                double strokeAlpha = Math.min(0.35, (radiusY - 3.5) / 3.5 * 0.35);
                gc.setStroke(Color.rgb(15, 23, 42, strokeAlpha));
                gc.setLineWidth(0.8);
                for (int i = startIndex; i < cells.size(); i++) {
                    H3Cell cell = cells.get(i);
                    if (cell.getLatitude() > cullMaxLat) break;

                    double x = (cell.getLongitude() - minLng) * scale + offsetX;
                    double y = (maxLat - cell.getLatitude()) * scale + offsetY;

                    if (x < -cellSize * 2 || x > canvasWidth + cellSize * 2 || y < -cellSize * 2 || y > canvasHeight + cellSize * 2) {
                        continue;
                    }

                    double cellRadiusY = Math.max(1.0, cellSize / 2.0);
                    double absLat = Math.abs(cell.getLatitude());
                    double cosLat = Math.cos(Math.toRadians(Math.min(88.0, absLat)));
                    double radiusX = Math.max(cellRadiusY * 0.45, (cellSize / 2.0) / Math.max(0.12, cosLat));

                    if (absLat < 78.0) {
                        strokeHexCell2D(gc, x, y, radiusX, cellRadiusY);
                    }
                }
            }
            return;
        }

        // Discrete Hexagonal Polygon Rendering (when smoothMap == false)
        for (int i = startIndex; i < cells.size(); i++) {
            H3Cell cell = cells.get(i);
            if (cell.getLatitude() > cullMaxLat) break;

            double x = (cell.getLongitude() - minLng) * scale + offsetX;
            double y = (maxLat - cell.getLatitude()) * scale + offsetY;

            if (x < -cellSize * 2 || x > canvasWidth + cellSize * 2 || y < -cellSize * 2 || y > canvasHeight + cellSize * 2) {
                continue;
            }

            Color color = colorResolver.getBufferOrCellColor(cell);
            if (showHillshading || showSolarTerminator) {
                double f = (showHillshading ? colorResolver.computeHillshadeFactor(cell) : 1.0)
                         * (showSolarTerminator ? colorResolver.computeSolarTerminatorFactor(cell) : 1.0);
                color = Color.color(
                        Math.clamp(color.getRed() * f, 0.0, 1.0),
                        Math.clamp(color.getGreen() * f, 0.0, 1.0),
                        Math.clamp(color.getBlue() * f, 0.0, 1.0),
                        color.getOpacity()
                );
            }

            double cellRadiusY = Math.max(1.0, cellSize / 2.0);
            double absLat = Math.abs(cell.getLatitude());
            double cosLat = Math.cos(Math.toRadians(Math.min(88.0, absLat)));
            double radiusX = Math.max(cellRadiusY * 0.45, (cellSize / 2.0) / Math.max(0.12, cosLat));

            boolean floating = showFloatingLayers && colorResolver.hasThematicOverlay(cell, null);
            if (floating) {
                Color baseBiomeColor = colorResolver.getBiomeColor(cell.getBiome());
                if (showHillshading || showSolarTerminator) {
                    double f = (showHillshading ? colorResolver.computeHillshadeFactor(cell) : 1.0)
                             * (showSolarTerminator ? colorResolver.computeSolarTerminatorFactor(cell) : 1.0);
                    baseBiomeColor = Color.color(
                            Math.clamp(baseBiomeColor.getRed() * f, 0.0, 1.0),
                            Math.clamp(baseBiomeColor.getGreen() * f, 0.0, 1.0),
                            Math.clamp(baseBiomeColor.getBlue() * f, 0.0, 1.0),
                            1.0
                    );
                }
                gc.setFill(baseBiomeColor);
                drawHexCell2D(gc, x, y, radiusX, cellRadiusY);

                gc.setFill(Color.rgb(0, 0, 0, 0.32));
                drawHexCell2D(gc, x + 1.8, y + 1.8, radiusX * 0.94, cellRadiusY * 0.94);

                gc.setFill(color);
                drawHexCell2D(gc, x - 0.9, y - 0.9, radiusX * 0.96, cellRadiusY * 0.96);
                gc.setStroke(Color.rgb(255, 255, 255, 0.35));
                gc.setLineWidth(0.8);
                strokeHexCell2D(gc, x - 0.9, y - 0.9, radiusX * 0.96, cellRadiusY * 0.96);
            } else {
                gc.setFill(color);
                drawHexCell2D(gc, x, y, radiusX, cellRadiusY);
            }

            if (showHexGrid && cellRadiusY >= 3.5 && absLat < 78.0 && !floating) {
                double strokeAlpha = Math.min(0.35, (cellRadiusY - 3.5) / 3.5 * 0.35);
                gc.setStroke(Color.rgb(15, 23, 42, strokeAlpha));
                gc.setLineWidth(0.8);
                strokeHexCell2D(gc, x, y, radiusX, cellRadiusY);
            }
        }
    }

    /**
     * Fills a 2D hexagonal polygon centered at \((cx, cy)\) with anisotropic radii \((r_x, r_y)\).
     */
    public static void drawHexCell2D(GraphicsContext gc, double cx, double cy, double radiusX, double radiusY) {
        double[] xs = new double[6];
        double[] ys = new double[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i - 30);
            xs[i] = cx + radiusX * Math.cos(angle);
            ys[i] = cy + radiusY * Math.sin(angle);
        }
        gc.fillPolygon(xs, ys, 6);
    }

    /**
     * Strokes a 2D hexagonal polygon outline centered at \((cx, cy)\) with anisotropic radii \((r_x, r_y)\).
     */
    public static void strokeHexCell2D(GraphicsContext gc, double cx, double cy, double radiusX, double radiusY) {
        double[] xs = new double[6];
        double[] ys = new double[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i - 30);
            xs[i] = cx + radiusX * Math.cos(angle);
            ys[i] = cy + radiusY * Math.sin(angle);
        }
        gc.strokePolygon(xs, ys, 6);
    }

    private int findLatIndex(List<H3Cell> cells, double targetLat) {
        int low = 0;
        int high = cells.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            double midVal = cells.get(mid).getLatitude();

            if (midVal < targetLat)
                low = mid + 1;
            else if (midVal > targetLat)
                high = mid - 1;
            else
                return mid;
        }
        return low;
    }
}
