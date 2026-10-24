/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.renderers;

import com.uber.h3core.util.LatLng;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.DisplayMode;
import org.ether.society.ui.ViewMode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Renders vector GIS overlays, topographic contours, directional flux vector fields,
 * mineral/aquifer resource badges, coordinate HUD badges, and analytical distribution legends.
 *
 * <p>Extracted from {@code H3MapCanvas} to fulfill SRP and decouple spatial vector geometries
 * from raw raster canvas pixel pipelines.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class H3VectorLayerRenderer {

    /**
     * 3D projected coordinate structure containing screen coordinates and depth factor.
     */
    public static class ProjectedPoint {
        public double screenX, screenY, z;

        public ProjectedPoint() {}

        public ProjectedPoint(double screenX, double screenY, double z) {
            this.screenX = screenX;
            this.screenY = screenY;
            this.z = z;
        }
    }

    /**
     * Projects spherical coordinates \((lat, lng, elevation)\) to 2D orthographic screen space.
     */
    public static ProjectedPoint project3D(double lat, double lng, double elevation,
                                          double radRotationY, double radTilt,
                                          double radius, double cx, double cy,
                                          double verticalExaggeration) {
        double latRad = Math.toRadians(lat);
        double lngRad = Math.toRadians(lng);

        // Spherical to Cartesian
        double x = Math.cos(latRad) * Math.sin(lngRad);
        double y = Math.sin(latRad);
        double z = Math.cos(latRad) * Math.cos(lngRad);

        // Apply Y-axis rotation (Longitude)
        double cosR = Math.cos(radRotationY);
        double sinR = Math.sin(radRotationY);
        double xr = x * cosR + z * sinR;
        double zr = -x * sinR + z * cosR;

        // Apply X-axis tilt (Latitude)
        double cosT = Math.cos(radTilt);
        double sinT = Math.sin(radTilt);
        double yrt = y * cosT - zr * sinT;
        double zrt = y * sinT + zr * cosT;

        // Backface culling
        if (zrt <= 0.02) return null;

        // Screen projection with heightmap 3D vertical exaggeration factor (default 25x)
        double elevationRatio = (elevation / 6371000.0) * verticalExaggeration;
        double r = radius * (1.0 + elevationRatio);

        ProjectedPoint p = new ProjectedPoint();
        p.screenX = cx + xr * r;
        p.screenY = cy - yrt * r;
        p.z = zrt;
        return p;
    }

    /**
     * Draws subtle coordinate overlay badge (Center Lat/Lng, Bounds, Zoom) on canvas.
     */
    public void drawCoordinateOverlay(GraphicsContext gc, double minLat, double maxLat,
                                      double minLng, double maxLng, double scale,
                                      double offsetX, double offsetY, double centerLat,
                                      double centerLng, double zoomFactor,
                                      double canvasWidth, double canvasHeight) {
        double viewTopLat = maxLat - (0 - offsetY) / scale;
        double viewBottomLat = maxLat - (canvasHeight - offsetY) / scale;
        double viewLeftLng = minLng + (0 - offsetX) / scale;
        double viewRightLng = minLng + (canvasWidth - offsetX) / scale;

        double cTopLat = Math.min(90.0, Math.max(-90.0, viewTopLat));
        double cBotLat = Math.min(90.0, Math.max(-90.0, viewBottomLat));
        double cLeftLng = Math.min(180.0, Math.max(-180.0, viewLeftLng));
        double cRightLng = Math.min(180.0, Math.max(-180.0, viewRightLng));

        String coordsText = String.format(
            "📍 Center: %.2f°N, %.2f°E | Bounds: [%.2f°N, %.2f°E] → [%.2f°N, %.2f°E] | Zoom: %.1fx",
            centerLat, centerLng, cTopLat, cLeftLng, cBotLat, cRightLng, zoomFactor
        );

        gc.setFont(Font.font("Consolas", 11));
        gc.setFill(Color.rgb(15, 23, 42, 0.8));
        gc.fillRoundRect(12, 12, 540, 24, 8, 8);
        gc.setStroke(Color.rgb(56, 189, 248, 0.4));
        gc.setLineWidth(1);
        gc.strokeRoundRect(12, 12, 540, 24, 8, 8);

        gc.setFill(Color.rgb(226, 232, 240));
        gc.fillText(coordsText, 20, 28);
    }

    /**
     * Draws hypsometric contour line isolines across adjacent hexagonal boundaries.
     */
    public void drawContours(GraphicsContext gc, boolean showContours, List<H3Cell> cells,
                             Map<Long, H3Cell> cellMap, H3Service h3Service, ViewMode viewMode,
                             double zoomFactor, double minLat, double maxLat, double minLng,
                             double scale, double offsetX, double offsetY,
                             double canvasWidth, double canvasHeight,
                             double centerLat, double centerLng, double verticalExaggeration) {
        if (!showContours || cells == null || cellMap == null || h3Service == null) return;

        gc.setStroke(Color.rgb(56, 189, 248, 0.75));
        gc.setLineWidth(Math.max(1.0, 1.2 * Math.sqrt(Math.max(0.5, zoomFactor))));

        double maxElev = 0;
        for (H3Cell c : cells) {
            if (c == null) continue;
            double e = c.getElevation() != null ? c.getElevation() : 0;
            if (e > maxElev) maxElev = e;
        }
        double adaptiveZoomScale = Math.max(0.5, zoomFactor);
        double step = (maxElev > 100) ? Math.min(1000.0, (maxElev / 6.0) / adaptiveZoomScale) : (50.0 / adaptiveZoomScale);
        if (step < 20) step = 20;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getH3Index() == null) continue;
            if (viewMode == ViewMode.VIEW_2D) {
                double lat = cell.getLatitude() != null ? cell.getLatitude() : 0.0;
                double top = maxLat - (0 - offsetY) / scale;
                double bottom = maxLat - (canvasHeight - offsetY) / scale;
                if (lat > top + 10 || lat < bottom - 10) continue;
            }

            double elev = cell.getElevation() != null ? cell.getElevation() : 0;
            int level = (int) (elev / step);

            try {
                List<Long> neighbors = h3Service.getNeighbors(cell.getH3Index());
                for (Long nIdx : neighbors) {
                    H3Cell neighbor = cellMap.get(nIdx);
                    if (neighbor == null) continue;

                    double nElev = neighbor.getElevation() != null ? neighbor.getElevation() : 0;
                    int nLevel = (int) (nElev / step);

                    if (level > nLevel) {
                        long edge = h3Service.getDirectedEdge(cell.getH3Index(), nIdx);
                        List<LatLng> boundary = h3Service.getEdgeBoundary(edge);
                        if (boundary != null && boundary.size() >= 2) {
                            if (viewMode == ViewMode.VIEW_2D) {
                                LatLng p1 = boundary.get(0);
                                LatLng p2 = boundary.get(1);
                                if (Math.abs(p1.lng - p2.lng) > 180.0) continue;

                                double x1 = (p1.lng - minLng) * scale + offsetX;
                                double y1 = (maxLat - p1.lat) * scale + offsetY;
                                double x2 = (p2.lng - minLng) * scale + offsetX;
                                double y2 = (maxLat - p2.lat) * scale + offsetY;

                                gc.strokeLine(x1, y1, x2, y2);
                            } else if (viewMode == ViewMode.VIEW_3D) {
                                double radius = Math.min(canvasWidth, canvasHeight) * 0.45 * zoomFactor;
                                double cx = canvasWidth / 2;
                                double cy = canvasHeight / 2;
                                double radRotationY = Math.toRadians(-centerLng);
                                double radTilt = Math.toRadians(centerLat);

                                LatLng p1 = boundary.get(0);
                                LatLng p2 = boundary.get(1);

                                ProjectedPoint pp1 = project3D(p1.lat, p1.lng, level * step, radRotationY, radTilt, radius, cx, cy, verticalExaggeration);
                                ProjectedPoint pp2 = project3D(p2.lat, p2.lng, level * step, radRotationY, radTilt, radius, cx, cy, verticalExaggeration);

                                if (pp1 != null && pp2 != null) {
                                    gc.strokeLine(pp1.screenX, pp1.screenY, pp2.screenX, pp2.screenY);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Draws directional vector field lines for demographic, trade, or cultural fluxes.
     */
    public void drawFlowVectors(GraphicsContext gc, boolean showFlowVectors, List<H3Cell> cells,
                                Map<Long, H3Cell> cellMap, H3Service h3Service, ViewMode viewMode,
                                double zoomFactor, double minLng, double maxLat,
                                double scale, double offsetX, double offsetY,
                                double canvasWidth, double canvasHeight,
                                double centerLat, double centerLng) {
        if (!showFlowVectors || cells == null || cellMap == null || zoomFactor < 0.3) return;

        gc.setLineWidth(1.5);
        gc.setStroke(Color.rgb(251, 146, 60, 0.85));
        gc.setFill(Color.rgb(251, 146, 60, 0.90));

        double arrowLength = Math.max(8.0, 14.0 * zoomFactor);

        for (H3Cell cell : cells) {
            if (cell == null || cell.getH3Index() == null) continue;
            double flux = cell.getFluxPressure();
            if (flux <= 0.001) continue;

            List<Long> neighbors = h3Service != null ? h3Service.getNeighbors(cell.getH3Index()) : null;
            if (neighbors == null || neighbors.isEmpty()) continue;

            double maxNeighborFlux = flux;
            H3Cell targetNeighbor = null;

            for (Long nIdx : neighbors) {
                H3Cell neighbor = cellMap.get(nIdx);
                if (neighbor != null && neighbor.getFluxPressure() > maxNeighborFlux) {
                    maxNeighborFlux = neighbor.getFluxPressure();
                    targetNeighbor = neighbor;
                }
            }

            double cellLat = cell.getLatitude() != null ? cell.getLatitude() : 0.0;
            double cellLng = cell.getLongitude() != null ? cell.getLongitude() : 0.0;
            double angle;
            if (targetNeighbor != null && targetNeighbor.getLatitude() != null && targetNeighbor.getLongitude() != null) {
                double dLat = targetNeighbor.getLatitude() - cellLat;
                double dLng = targetNeighbor.getLongitude() - cellLng;
                angle = Math.atan2(-dLat, dLng);
            } else {
                angle = Math.toRadians((cell.getH3Index() % 360));
            }

            if (viewMode == ViewMode.VIEW_2D) {
                double cx = (cellLng - minLng) * scale + offsetX;
                double cy = (maxLat - cellLat) * scale + offsetY;

                if (cx < -20 || cx > canvasWidth + 20 || cy < -20 || cy > canvasHeight + 20) continue;

                double endX = cx + arrowLength * Math.cos(angle);
                double endY = cy + arrowLength * Math.sin(angle);

                gc.strokeLine(cx, cy, endX, endY);
                double headAngle1 = angle + Math.toRadians(150);
                double headAngle2 = angle - Math.toRadians(150);
                double headLen = arrowLength * 0.35;
                gc.strokeLine(endX, endY, endX + headLen * Math.cos(headAngle1), endY + headLen * Math.sin(headAngle1));
                gc.strokeLine(endX, endY, endX + headLen * Math.cos(headAngle2), endY + headLen * Math.sin(headAngle2));
            } else if (viewMode == ViewMode.VIEW_3D) {
                double radius = Math.min(canvasWidth, canvasHeight) * 0.45 * zoomFactor;
                double screenCx = canvasWidth / 2.0;
                double screenCy = canvasHeight / 2.0;
                double radRotationY = Math.toRadians(-centerLng);
                double radTilt = Math.toRadians(centerLat);

                ProjectedPoint pt = project3D(cellLat, cellLng, cell.getElevation() != null ? cell.getElevation() : 0, radRotationY, radTilt, radius, screenCx, screenCy, 25.0);
                if (pt != null) {
                    double endX = pt.screenX + arrowLength * Math.cos(angle);
                    double endY = pt.screenY + arrowLength * Math.sin(angle);
                    gc.strokeLine(pt.screenX, pt.screenY, endX, endY);
                }
            }
        }
    }

    /**
     * Draws mineral, precious metal, and deep aquifer geological badges.
     */
    public void drawResourceOverlay(GraphicsContext gc, boolean showResourceOverlay, List<H3Cell> cells,
                                    WorldBuffer worldBuffer, Map<Long, Integer> h3ToBufferIndexMap,
                                    ViewMode viewMode, double zoomFactor, double minLng, double maxLat,
                                    double scale, double offsetX, double offsetY,
                                    double canvasWidth, double canvasHeight,
                                    double centerLat, double centerLng) {
        if (!showResourceOverlay || cells == null) return;

        double markerSize = Math.max(3.0, 6.0 * zoomFactor);

        for (int i = 0; i < cells.size(); i++) {
            H3Cell cell = cells.get(i);
            if (cell == null) continue;
            double metal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0;
            double precious = cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal() : 0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0;
            double aquifer = cell.getFreshwaterAquifer() != null ? cell.getFreshwaterAquifer() : 0;

            if (worldBuffer != null && h3ToBufferIndexMap != null && !h3ToBufferIndexMap.isEmpty()) {
                Integer idx = h3ToBufferIndexMap.get(cell.getH3Index());
                if (idx != null && idx >= 0 && idx < worldBuffer.getCapacity()) {
                    if (worldBuffer.getWaterResource() != null) aquifer = Math.max(aquifer, worldBuffer.getWaterResource()[idx]);
                }
            }

            if (metal <= 5.0 && precious <= 0.0 && capital <= 5.0 && aquifer <= 10.0) continue;

            double cx = 0, cy = 0;
            boolean visible = false;

            if (viewMode == ViewMode.VIEW_2D) {
                double lat = cell.getLatitude() != null ? cell.getLatitude() : 0.0;
                double lng = cell.getLongitude() != null ? cell.getLongitude() : 0.0;
                cx = (lng - minLng) * scale + offsetX;
                cy = (maxLat - lat) * scale + offsetY;
                if (cx >= -20 && cx <= canvasWidth + 20 && cy >= -20 && cy <= canvasHeight + 20) {
                    visible = true;
                }
            } else if (viewMode == ViewMode.VIEW_3D) {
                double radius = Math.min(canvasWidth, canvasHeight) * 0.45 * zoomFactor;
                double screenCx = canvasWidth / 2.0;
                double screenCy = canvasHeight / 2.0;
                double radRotationY = Math.toRadians(-centerLng);
                double radTilt = Math.toRadians(centerLat);

                ProjectedPoint pt = project3D(cell.getLatitude(), cell.getLongitude(), cell.getElevation() != null ? cell.getElevation() : 0, radRotationY, radTilt, radius, screenCx, screenCy, 25.0);
                if (pt != null) {
                    cx = pt.screenX;
                    cy = pt.screenY;
                    visible = true;
                }
            }

            if (!visible) continue;

            if (precious > 0 || metal > 10.0) {
                gc.setFill(precious > 0 ? Color.rgb(234, 179, 8, 0.85) : Color.rgb(148, 163, 184, 0.85));
                double[] xPoints = {cx, cx + markerSize, cx, cx - markerSize};
                double[] yPoints = {cy - markerSize, cy, cy + markerSize, cy};
                gc.fillPolygon(xPoints, yPoints, 4);
            } else if (aquifer > 30.0) {
                gc.setFill(Color.rgb(6, 182, 212, 0.80));
                gc.fillOval(cx - markerSize * 0.7, cy - markerSize * 0.7, markerSize * 1.4, markerSize * 1.4);
            } else if (capital > 10.0) {
                gc.setFill(Color.rgb(168, 85, 247, 0.80));
                gc.fillRect(cx - markerSize * 0.6, cy - markerSize * 0.6, markerSize * 1.2, markerSize * 1.2);
            }
        }
    }

    /**
     * Draws statistical distribution histogram and continuous color spectrum legend badge.
     */
    public void drawLegendOverlay(GraphicsContext gc, List<H3Cell> cells,
                                  DisplayMode primaryMode, H3CellColorResolver colorResolver,
                                  double canvasWidth, double canvasHeight) {
        if (cells == null || cells.isEmpty() || primaryMode == null || colorResolver == null) return;

        double legendWidth = 320.0;
        double legendHeight = 112.0;
        double marginRight = 30.0;
        double marginBottom = 35.0;
        double lx = Math.max(10.0, canvasWidth - legendWidth - marginRight);
        double ly = Math.max(10.0, canvasHeight - legendHeight - marginBottom);

        gc.setFill(Color.rgb(15, 23, 42, 0.94));
        gc.fillRoundRect(lx, ly, legendWidth, legendHeight, 10, 10);
        gc.setStroke(Color.rgb(56, 189, 248, 0.50));
        gc.setLineWidth(1.4);
        gc.strokeRoundRect(lx, ly, legendWidth, legendHeight, 10, 10);

        gc.setFill(Color.rgb(56, 189, 248));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        gc.fillText(I18n.getOrDefault("sim.legend.title", "LEGEND — ") + primaryMode.getDisplayName().toUpperCase(), lx + 12, ly + 18);

        double minVal = Double.MAX_VALUE;
        double maxVal = -Double.MAX_VALUE;
        double sum = 0.0;
        double[] values = new double[cells.size()];

        for (int i = 0; i < cells.size(); i++) {
            double v = colorResolver.getCellDisplayValue(cells.get(i), primaryMode, primaryMode);
            values[i] = v;
            if (v < minVal) minVal = v;
            if (v > maxVal) maxVal = v;
            sum += v;
        }

        if (minVal == Double.MAX_VALUE) minVal = 0.0;
        if (maxVal == -Double.MAX_VALUE) maxVal = 1.0;
        double meanVal = sum / cells.size();

        Arrays.sort(values);
        double medianVal = (values.length % 2 == 0)
                ? (values[values.length / 2 - 1] + values[values.length / 2]) / 2.0
                : values[values.length / 2];

        double range = Math.max(1e-6, maxVal - minVal);
        double barX = lx + 12;
        double barWidth = legendWidth - 24;

        int numBins = 20;
        int[] binCounts = new int[numBins];
        int maxBinCount = 1;
        for (double v : values) {
            int binIdx = (int) Math.clamp(((v - minVal) / range) * numBins, 0, numBins - 1);
            binCounts[binIdx]++;
            if (binCounts[binIdx] > maxBinCount) {
                maxBinCount = binCounts[binIdx];
            }
        }

        double histoY = ly + 46;
        double histoMaxH = 18.0;
        double binW = barWidth / numBins;

        gc.setFill(Color.rgb(56, 189, 248, 0.45));
        for (int b = 0; b < numBins; b++) {
            if (binCounts[b] == 0) continue;
            double bh = ((double) binCounts[b] / maxBinCount) * histoMaxH;
            gc.fillRect(barX + b * binW, histoY - bh, binW - 0.8, bh);
        }

        double barY = ly + 48;
        double barHeight = 14;

        LinearGradient grad = new LinearGradient(
                barX, barY, barX + barWidth, barY, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(30, 58, 138)),
                new Stop(0.25, Color.rgb(6, 182, 212)),
                new Stop(0.50, Color.rgb(34, 197, 94)),
                new Stop(0.75, Color.rgb(234, 179, 8)),
                new Stop(1.0, Color.rgb(239, 68, 68))
        );

        gc.setFill(grad);
        gc.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);
        gc.setStroke(Color.rgb(255, 255, 255, 0.4));
        gc.strokeRoundRect(barX, barY, barWidth, barHeight, 4, 4);

        double meanNorm = Math.clamp((meanVal - minVal) / range, 0.0, 1.0);
        double meanX = barX + meanNorm * barWidth;
        gc.setFill(Color.rgb(56, 189, 248));
        gc.fillPolygon(new double[]{meanX - 4, meanX + 4, meanX}, new double[]{barY - 5, barY - 5, barY}, 3);

        double medNorm = Math.clamp((medianVal - minVal) / range, 0.0, 1.0);
        double medX = barX + medNorm * barWidth;
        gc.setFill(Color.rgb(245, 158, 11));
        gc.fillPolygon(new double[]{medX - 4, medX + 4, medX}, new double[]{barY + barHeight + 5, barY + barHeight + 5, barY + barHeight}, 3);

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9.5));
        gc.setFill(Color.rgb(148, 163, 184));
        gc.fillText(String.format("Min: %.1f", minVal), barX, ly + 88);

        gc.setFill(Color.rgb(56, 189, 248));
        gc.fillText(String.format("μ: %.1f", meanVal), barX + barWidth * 0.28, ly + 88);

        gc.setFill(Color.rgb(245, 158, 11));
        gc.fillText(String.format("x̃: %.1f", medianVal), barX + barWidth * 0.54, ly + 88);

        gc.setFill(Color.rgb(248, 113, 113));
        String maxStr = String.format("Max: %.1f", maxVal);
        gc.fillText(maxStr, barX + barWidth - (maxStr.length() * 5.8), ly + 88);
    }
}
