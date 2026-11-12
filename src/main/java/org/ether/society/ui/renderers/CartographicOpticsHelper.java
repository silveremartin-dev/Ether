/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.renderers;

import javafx.scene.paint.Color;
import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;

import java.util.List;
import java.util.Map;

/**
 * <h1>CartographicOpticsHelper</h1>
 * <p>
 * Lambertian topographical hillshading, diurnal astronomical solar terminator shading,
 * and alpha-composite color blending calculations for planetary cartographic views.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class CartographicOpticsHelper {

    private CartographicOpticsHelper() {
        // Utility class
    }

    /**
     * Computes Lambertian hillshading illumination factor based on topographical elevation gradients.
     *
     * @param cell            target hexagonal cell
     * @param showHillshading whether hillshading is enabled
     * @param h3Service       H3 spatial index service
     * @param cellMap         mapping of cell indexes to cells
     * @return illumination multiplier factor
     */
    public static double computeHillshadeFactor(H3Cell cell, boolean showHillshading, H3Service h3Service, Map<Long, H3Cell> cellMap) {
        if (cell == null || !showHillshading) return 1.0;
        double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
        if (elev <= 0) return 1.0;

        double dzDx = 0.0;
        double dzDy = 0.0;

        if (h3Service != null && cellMap != null) {
            try {
                List<Long> neighbors = h3Service.getNeighbors(cell.getH3Index());
                if (neighbors != null && !neighbors.isEmpty()) {
                    double cLat = cell.getLatitude();
                    double cLng = cell.getLongitude();
                    double cosLat = Math.cos(Math.toRadians(cLat));
                    int validNeighbors = 0;
                    for (Long nIdx : neighbors) {
                        H3Cell neighbor = cellMap.get(nIdx);
                        if (neighbor != null) {
                            double nElev = neighbor.getElevation() != null ? neighbor.getElevation() : 0.0;
                            double dLng = (neighbor.getLongitude() - cLng) * cosLat;
                            double dLat = neighbor.getLatitude() - cLat;
                            double dist = Math.hypot(dLng, dLat);
                            if (dist > 1e-4) {
                                double slope = (nElev - elev) / (dist * 111320.0);
                                dzDx += slope * (dLng / dist);
                                dzDy += slope * (dLat / dist);
                                validNeighbors++;
                            }
                        }
                    }
                    if (validNeighbors > 0) {
                        dzDx /= validNeighbors;
                        dzDy /= validNeighbors;
                    }
                }
            } catch (Exception ignored) {
                org.slf4j.LoggerFactory.getLogger(CartographicOpticsHelper.class).trace("Ignored topology error during hillshade: {}", ignored.getMessage());
            }
        }

        double azimuthRad = Math.toRadians(315.0); // Light from North-West
        double altitudeRad = Math.toRadians(45.0);
        double slopeRad = Math.atan(Math.hypot(dzDx, dzDy) * 5.0);
        double aspectRad = Math.atan2(dzDy, -dzDx);

        double cosIncidence = Math.sin(altitudeRad) * Math.cos(slopeRad)
                + Math.cos(altitudeRad) * Math.sin(slopeRad) * Math.cos(azimuthRad - aspectRad);

        return Math.clamp(0.45 + 0.55 * Math.max(0.0, cosIncidence), 0.35, 1.25);
    }

    /**
     * Computes the astronomical solar terminator diurnal day/night shading factor.
     *
     * @param cell                target cell
     * @param showSolarTerminator whether solar terminator is enabled
     * @return lighting attenuation factor
     */
    public static double computeSolarTerminatorFactor(H3Cell cell, boolean showSolarTerminator) {
        if (cell == null || !showSolarTerminator) return 1.0;
        double latRad = Math.toRadians(cell.getLatitude());
        double subSolarLatRad = Math.toRadians(12.0); // Mild summer tilt approximation
        double subSolarLng = 0.0; // Prime meridian subsolar point
        double lngDiffRad = Math.toRadians(cell.getLongitude() - subSolarLng);

        double cosZenith = Math.sin(latRad) * Math.sin(subSolarLatRad)
                + Math.cos(latRad) * Math.cos(subSolarLatRad) * Math.cos(lngDiffRad);

        if (cosZenith > 0.05) {
            return 1.0; // Full daylight
        } else if (cosZenith < -0.10) {
            return 0.30; // Deep night
        } else {
            // Twilight penumbra transition band
            double t = (cosZenith - (-0.10)) / 0.15;
            return 0.30 + 0.70 * t;
        }
    }

    /**
     * Linearly blends two JavaFX colors according to an alpha weight \(\in [0, 1]\).
     *
     * @param base    background color
     * @param overlay foreground overlay color
     * @param alpha   blending factor
     * @return composite blended color
     */
    public static Color blendColors(Color base, Color overlay, double alpha) {
        if (alpha <= 0.0 || overlay == null) return base;
        if (alpha >= 1.0 || base == null) return overlay;
        double inv = 1.0 - alpha;
        double r = base.getRed() * inv + overlay.getRed() * alpha;
        double g = base.getGreen() * inv + overlay.getGreen() * alpha;
        double b = base.getBlue() * inv + overlay.getBlue() * alpha;
        return Color.color(Math.clamp(r, 0.0, 1.0), Math.clamp(g, 0.0, 1.0), Math.clamp(b, 0.0, 1.0));
    }
}
