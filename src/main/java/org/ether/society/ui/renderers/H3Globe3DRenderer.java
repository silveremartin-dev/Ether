/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.renderers;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import org.ether.society.database.H3Cell;

import java.util.Arrays;
import java.util.List;

/**
 * 3D Orthographic Globe Renderer and continuous spherical texture projector.
 *
 * <p>Extracted from {@code H3MapCanvas} to fulfill SRP and decouple 3D spherical trigonometry,
 * depth buffer sorting, atmosphere scattering shaders, and planetary limb shadows
 * from the main canvas coordinator.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class H3Globe3DRenderer {

    private double[] polyZBuf = new double[0];
    private double[][] polyPxBuf = new double[0][6];
    private double[][] polyPyBuf = new double[0][6];
    private Color[] polyColorBuf = new Color[0];
    private boolean[] polyBorderBuf = new boolean[0];
    private Integer[] polyIndexBuf = new Integer[0];

    private WritableImage cachedSmoothImage;
    private boolean smoothImageDirty = true;

    /**
     * Marks the smooth texture image cache as dirty.
     */
    public void invalidateSmoothCache() {
        this.smoothImageDirty = true;
    }

    /**
     * Renders 3D orthographic spherical globe with hexagonal cells or continuous smooth surface.
     */
    public void draw3D(GraphicsContext gc, List<H3Cell> cells, H3CellColorResolver colorResolver,
                       boolean smoothMap, boolean showHexGrid, boolean showFloatingLayers,
                       boolean showHillshading, boolean showSolarTerminator,
                       double verticalExaggeration, double zoomFactor,
                       double centerLat, double centerLng,
                       double minLat, double maxLat, double minLng, double maxLng,
                       double canvasWidth, double canvasHeight) {
        if (cells == null || cells.isEmpty()) return;

        double radius = Math.min(canvasWidth, canvasHeight) * 0.45 * zoomFactor;
        double cx = canvasWidth / 2.0;
        double cy = canvasHeight / 2.0;

        double radRotationY = Math.toRadians(-centerLng);
        double radTilt = Math.toRadians(centerLat);

        // 1. Draw Ocean Base Sphere (Deep navy gradient body)
        RadialGradient oceanGrad = new RadialGradient(
                0, 0, cx - radius * 0.25, cy - radius * 0.25, radius * 1.25, false,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(16, 50, 95)),
                new Stop(0.65, Color.rgb(10, 30, 65)),
                new Stop(1.0, Color.rgb(4, 15, 40))
        );
        gc.setFill(oceanGrad);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // 2. Draw Atmosphere Glow Rings
        gc.setLineWidth(1.5);
        gc.setStroke(Color.rgb(56, 189, 248, 0.40));
        gc.strokeOval(cx - radius - 1, cy - radius - 1, (radius + 1) * 2, (radius + 1) * 2);

        gc.setLineWidth(4.0);
        gc.setStroke(Color.rgb(14, 165, 233, 0.22));
        gc.strokeOval(cx - radius - 4, cy - radius - 4, (radius + 4) * 2, (radius + 4) * 2);

        gc.setLineWidth(9.0);
        gc.setStroke(Color.rgb(56, 189, 248, 0.08));
        gc.strokeOval(cx - radius - 8, cy - radius - 8, (radius + 8) * 2, (radius + 8) * 2);

        // 3. Fast Zero-Allocation Buffer Check
        int cellCount = cells.size();
        if (polyZBuf.length < cellCount) {
            polyZBuf = new double[cellCount];
            polyPxBuf = new double[cellCount][6];
            polyPyBuf = new double[cellCount][6];
            polyColorBuf = new Color[cellCount];
            polyBorderBuf = new boolean[cellCount];
            polyIndexBuf = new Integer[cellCount];
        }

        double hexRadiusRad = Math.sqrt(4.0 * Math.PI / Math.max(1, cellCount)) * 0.58;
        double screenCellRadius = (radius / Math.sqrt(Math.max(1, cellCount))) * 0.95;

        double effectiveHexRad = smoothMap ? hexRadiusRad * 1.04 : hexRadiusRad;
        double tanHexRad = Math.tan(effectiveHexRad);

        double cosR = Math.cos(radRotationY);
        double sinR = Math.sin(radRotationY);
        double cosT = Math.cos(radTilt);
        double sinT = Math.sin(radTilt);

        double radCamLat = Math.toRadians(centerLat);
        double viewWidth = canvasWidth;
        double viewHeight = canvasHeight;

        if (smoothMap) {
            WritableImage smoothImg = getOrUpdateSmoothMapImage(cells, colorResolver, showHillshading, showSolarTerminator, minLat, maxLat, minLng, maxLng);
            drawSmoothGlobeSurface(gc, cx, cy, radius, radRotationY, radTilt, centerLat, centerLng, minLat, maxLat, minLng, maxLng, showSolarTerminator, smoothImg);

            if (showHexGrid) {
                for (int i = 0; i < cellCount; i++) {
                    H3Cell cell = cells.get(i);
                    double lat = cell.getLatitude();
                    double lng = cell.getLongitude();

                    double radLat1 = Math.toRadians(lat);
                    double radLngDiff = Math.toRadians(lng - centerLng);
                    double dotProd = Math.sin(radLat1) * Math.sin(radCamLat) + Math.cos(radLat1) * Math.cos(radCamLat) * Math.cos(radLngDiff);
                    if (dotProd < -0.05) continue;

                    double latRad = Math.toRadians(lat);
                    double lngRad = Math.toRadians(lng);
                    double nx = Math.cos(latRad) * Math.sin(lngRad);
                    double ny = Math.sin(latRad);
                    double nz = Math.cos(latRad) * Math.cos(lngRad);

                    double xr = nx * cosR + nz * sinR;
                    double zr = -nx * sinR + nz * cosR;
                    double yrt = ny * cosT - zr * sinT;
                    double zrt = ny * sinT + zr * cosT;

                    if (zrt <= 0.02) continue;

                    double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
                    double elevationRatio = (elev / 6371000.0) * verticalExaggeration;
                    double r = radius * (1.0 + elevationRatio);

                    double cxCenter = cx + xr * r;
                    double cyCenter = cy - yrt * r;

                    if (cxCenter < -40 || cxCenter > viewWidth + 40 || cyCenter < -40 || cyCenter > viewHeight + 40) {
                        continue;
                    }

                    double[] px = new double[6];
                    double[] py = new double[6];

                    double ux, uy, uz;
                    if (Math.abs(ny) < 0.99) {
                        double uLen = Math.hypot(nz, nx);
                        ux = -nz / uLen; uy = 0.0; uz = nx / uLen;
                    } else {
                        double uLen = Math.hypot(nz, ny);
                        ux = 0.0; uy = -nz / uLen; uz = ny / uLen;
                    }
                    double vx = ny * uz - nz * uy;
                    double vy = nz * ux - nx * uz;
                    double vz = nx * uy - ny * ux;

                    for (int k = 0; k < 6; k++) {
                        double angleRad = (Math.PI / 3.0) * k - (Math.PI / 6.0);
                        double cosA = Math.cos(angleRad);
                        double sinA = Math.sin(angleRad);

                        double dx = tanHexRad * (cosA * ux + sinA * vx);
                        double dy = tanHexRad * (cosA * uy + sinA * vy);
                        double dz = tanHexRad * (cosA * uz + sinA * vz);

                        double vx3 = nx + dx;
                        double vy3 = ny + dy;
                        double vz3 = nz + dz;
                        double vLen = Math.sqrt(vx3 * vx3 + vy3 * vy3 + vz3 * vz3);
                        vx3 /= vLen; vy3 /= vLen; vz3 /= vLen;

                        double vxr = vx3 * cosR + vz3 * sinR;
                        double vzr = -vx3 * sinR + vz3 * cosR;
                        double vyrt = vy3 * cosT - vzr * sinT;

                        px[k] = cx + vxr * r;
                        py[k] = cy - vyrt * r;
                    }

                    gc.setStroke(Color.rgb(15, 23, 42, 0.35));
                    gc.setLineWidth(0.6);
                    gc.strokePolygon(px, py, 6);
                }
            }

            RadialGradient limbGradient = new RadialGradient(
                    0, 0, cx, cy, radius, false,
                    CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.rgb(0, 0, 0, 0.0)),
                    new Stop(0.85, Color.rgb(0, 0, 0, 0.05)),
                    new Stop(0.98, Color.rgb(0, 0, 0, 0.45)),
                    new Stop(1.0, Color.rgb(0, 0, 0, 0.70))
            );
            gc.setFill(limbGradient);
            gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            return;
        }

        int visibleCount = 0;

        for (int i = 0; i < cellCount; i++) {
            H3Cell cell = cells.get(i);
            double lat = cell.getLatitude();
            double lng = cell.getLongitude();

            double radLat1 = Math.toRadians(lat);
            double radLngDiff = Math.toRadians(lng - centerLng);
            double dotProd = Math.sin(radLat1) * Math.sin(radCamLat) + Math.cos(radLat1) * Math.cos(radCamLat) * Math.cos(radLngDiff);
            if (dotProd < -0.05) continue;

            double latRad = Math.toRadians(lat);
            double lngRad = Math.toRadians(lng);
            double nx = Math.cos(latRad) * Math.sin(lngRad);
            double ny = Math.sin(latRad);
            double nz = Math.cos(latRad) * Math.cos(lngRad);

            double xr = nx * cosR + nz * sinR;
            double zr = -nx * sinR + nz * cosR;
            double yrt = ny * cosT - zr * sinT;
            double zrt = ny * sinT + zr * cosT;

            if (zrt <= 0.02) continue;

            double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
            double elevationRatio = (elev / 6371000.0) * verticalExaggeration;
            if (showFloatingLayers && colorResolver.hasThematicOverlay(cell, null)) {
                elevationRatio += 0.015;
            }
            double r = radius * (1.0 + elevationRatio);

            double cxCenter = cx + xr * r;
            double cyCenter = cy - yrt * r;

            if (cxCenter < -40 || cxCenter > viewWidth + 40 || cyCenter < -40 || cyCenter > viewHeight + 40) {
                continue;
            }

            double[] px = polyPxBuf[visibleCount];
            double[] py = polyPyBuf[visibleCount];

            double ux, uy, uz;
            if (Math.abs(ny) < 0.99) {
                double uLen = Math.hypot(nz, nx);
                ux = -nz / uLen; uy = 0.0; uz = nx / uLen;
            } else {
                double uLen = Math.hypot(nz, ny);
                ux = 0.0; uy = -nz / uLen; uz = ny / uLen;
            }
            double vx = ny * uz - nz * uy;
            double vy = nz * ux - nx * uz;
            double vz = nx * uy - ny * ux;

            for (int k = 0; k < 6; k++) {
                double angleRad = (Math.PI / 3.0) * k - (Math.PI / 6.0);
                double cosA = Math.cos(angleRad);
                double sinA = Math.sin(angleRad);

                double dx = tanHexRad * (cosA * ux + sinA * vx);
                double dy = tanHexRad * (cosA * uy + sinA * vy);
                double dz = tanHexRad * (cosA * uz + sinA * vz);

                double vx3 = nx + dx;
                double vy3 = ny + dy;
                double vz3 = nz + dz;
                double vLen = Math.sqrt(vx3 * vx3 + vy3 * vy3 + vz3 * vz3);
                vx3 /= vLen; vy3 /= vLen; vz3 /= vLen;

                double vxr = vx3 * cosR + vz3 * sinR;
                double vzr = -vx3 * sinR + vz3 * cosR;
                double vyrt = vy3 * cosT - vzr * sinT;

                px[k] = cx + vxr * r;
                py[k] = cy - vyrt * r;
            }

            Color baseColor = colorResolver.getBufferOrCellColor(cell);
            double lightFactor = 0.45 + 0.55 * Math.max(0.0, zrt);
            if (showHillshading) {
                lightFactor *= colorResolver.computeHillshadeFactor(cell);
            }
            if (showSolarTerminator) {
                lightFactor *= colorResolver.computeSolarTerminatorFactor(cell);
            }
            Color shadedColor = Color.color(
                    Math.clamp(baseColor.getRed() * lightFactor, 0.0, 1.0),
                    Math.clamp(baseColor.getGreen() * lightFactor, 0.0, 1.0),
                    Math.clamp(baseColor.getBlue() * lightFactor, 0.0, 1.0),
                    baseColor.getOpacity()
            );

            polyZBuf[visibleCount] = zrt;
            polyColorBuf[visibleCount] = shadedColor;
            polyBorderBuf[visibleCount] = showHexGrid && (screenCellRadius > 1.4);
            polyIndexBuf[visibleCount] = visibleCount;

            visibleCount++;
        }

        final int count = visibleCount;
        Arrays.sort(polyIndexBuf, 0, count, (a, b) -> Double.compare(polyZBuf[a], polyZBuf[b]));

        gc.save();
        gc.beginPath();
        gc.arc(cx, cy, radius, radius, 0, 360);
        gc.closePath();
        gc.clip();

        for (int idx = 0; idx < count; idx++) {
            int i = polyIndexBuf[idx];
            gc.setFill(polyColorBuf[i]);
            gc.fillPolygon(polyPxBuf[i], polyPyBuf[i], 6);

            if (polyBorderBuf[i]) {
                gc.setStroke(Color.rgb(0, 0, 0, 0.20));
                gc.setLineWidth(0.5);
                gc.strokePolygon(polyPxBuf[i], polyPyBuf[i], 6);
            }
        }

        gc.restore();

        RadialGradient limbGradient = new RadialGradient(
                0, 0, cx, cy, radius, false,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(0, 0, 0, 0.0)),
                new Stop(0.85, Color.rgb(0, 0, 0, 0.05)),
                new Stop(0.98, Color.rgb(0, 0, 0, 0.45)),
                new Stop(1.0, Color.rgb(0, 0, 0, 0.70))
        );
        gc.setFill(limbGradient);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }

    /**
     * Projects continuous 2D scalar raster image onto a shaded 3D sphere.
     */
    public void drawSmoothGlobeSurface(GraphicsContext gc, double cx, double cy, double radius,
                                       double radRotationY, double radTilt,
                                       double centerLat, double centerLng,
                                       double minLat, double maxLat, double minLng, double maxLng,
                                       boolean showSolarTerminator, WritableImage smoothImg) {
        if (smoothImg == null || radius < 5) return;

        int rInt = (int) Math.ceil(radius);
        int diam = rInt * 2;
        int imgW = (int) smoothImg.getWidth();
        int imgH = (int) smoothImg.getHeight();

        PixelReader pr = smoothImg.getPixelReader();
        if (pr == null) return;

        int[] srcPixels = new int[imgW * imgH];
        pr.getPixels(0, 0, imgW, imgH, PixelFormat.getIntArgbInstance(), srcPixels, 0, imgW);

        WritableImage globeImg = new WritableImage(diam, diam);
        PixelWriter pw = globeImg.getPixelWriter();
        int[] globePixels = new int[diam * diam];

        double cosR = Math.cos(radRotationY);
        double sinR = Math.sin(radRotationY);
        double cosT = Math.cos(radTilt);
        double sinT = Math.sin(radTilt);
        double solarLngRad = Math.toRadians(-centerLng);

        double spanLng = maxLng - minLng;
        double spanLat = maxLat - minLat;
        if (spanLng <= 0) spanLng = 360.0;
        if (spanLat <= 0) spanLat = 180.0;

        for (int py = 0; py < diam; py++) {
            double dy = (rInt - py) / radius;
            double dy2 = dy * dy;
            if (dy2 >= 1.0) continue;
            double dxMax = Math.sqrt(Math.max(0.0, 1.0 - dy2));
            int minPx = Math.max(0, (int) Math.floor(rInt - dxMax * radius));
            int maxPx = Math.min(diam - 1, (int) Math.ceil(rInt + dxMax * radius));

            for (int px = minPx; px <= maxPx; px++) {
                double dx = (px - rInt) / radius;
                double r2 = dx * dx + dy2;
                if (r2 >= 1.0) continue;

                double zr = Math.sqrt(Math.max(0.0, 1.0 - r2));

                double yrt = dy * cosT - zr * sinT;
                double zrt = dy * sinT + zr * cosT;
                double xr = dx * cosR - zrt * sinR;
                double zr_orig = dx * sinR + zrt * cosR;

                double latDeg = Math.toDegrees(Math.asin(Math.clamp(yrt, -1.0, 1.0)));
                double lngDeg = Math.toDegrees(Math.atan2(xr, zr_orig));

                int texX = (int) Math.clamp(Math.floor(((lngDeg - minLng) / spanLng) * imgW), 0, imgW - 1);
                int texY = (int) Math.clamp(Math.floor(((maxLat - latDeg) / spanLat) * imgH), 0, imgH - 1);

                int srcColor = srcPixels[texY * imgW + texX];
                int a = (srcColor >> 24) & 0xff;
                int r = (srcColor >> 16) & 0xff;
                int g = (srcColor >> 8) & 0xff;
                int b = srcColor & 0xff;

                double light = 0.40 + 0.60 * zr;
                if (showSolarTerminator) {
                    double latRad = Math.toRadians(latDeg);
                    double lngRad = Math.toRadians(lngDeg);
                    double cosZenith = Math.cos(latRad) * Math.cos(lngRad - solarLngRad);
                    double termFactor = (cosZenith < -0.08) ? 0.35 : (cosZenith > 0.08 ? 1.0 : (0.35 + (cosZenith + 0.08) / 0.16 * 0.65));
                    light *= termFactor;
                }

                int nr = (int) Math.clamp(r * light, 0, 255);
                int ng = (int) Math.clamp(g * light, 0, 255);
                int nb = (int) Math.clamp(b * light, 0, 255);

                globePixels[py * diam + px] = (a << 24) | (nr << 16) | (ng << 8) | nb;
            }
        }

        pw.setPixels(0, 0, diam, diam, PixelFormat.getIntArgbInstance(), globePixels, 0, diam);
        gc.drawImage(globeImg, cx - rInt, cy - rInt);
    }

    /**
     * Builds or retrieves cached continuous 2D raster image interpolation.
     */
    public WritableImage getOrUpdateSmoothMapImage(List<H3Cell> cells, H3CellColorResolver colorResolver,
                                                  boolean showHillshading, boolean showSolarTerminator,
                                                  double minLat, double maxLat, double minLng, double maxLng) {
        if (cells == null || cells.isEmpty()) return null;
        if (cachedSmoothImage != null && !smoothImageDirty) {
            return cachedSmoothImage;
        }

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

        this.cachedSmoothImage = img;
        this.smoothImageDirty = false;
        return img;
    }
}
