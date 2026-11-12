/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.model.Scenario;

import java.util.List;

/**
 * Geometric bounding box locator for spatial benchmark sub-grids.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class SpatialWindowBoundingBoxFinder {

    private SpatialWindowBoundingBoxFinder() {
    }

    public record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {}

    public static BoundingBox findDenseLandBoundingBox(Scenario s, double spanLat, double spanLng) {
        int h3Res = s.getH3Resolution() > 0 ? Math.min(3, s.getH3Resolution()) : 1;
        PlanetPreset preset = s.getPlanetPreset() != null 
            ? s.getPlanetPreset().withResolution(h3Res) 
            : PlanetPreset.EARTH_LIKE.withResolution(h3Res);
        List<H3Cell> allCells = ProceduralGenerator.getInstance().generatePlanet(preset);
        
        H3Cell bestCenter = null;
        int maxLandCount = -1;
        for (H3Cell c : allCells) {
            if (c.getElevation() != null && c.getElevation() > 0.05 && Math.abs(c.getLatitude()) <= 55.0) {
                int count = 0;
                for (H3Cell other : allCells) {
                    if (other.getElevation() != null && other.getElevation() > 0.0 &&
                        Math.abs(other.getLatitude() - c.getLatitude()) <= spanLat / 2.0 &&
                        Math.abs(other.getLongitude() - c.getLongitude()) <= spanLng / 2.0) {
                        count++;
                    }
                }
                if (count > maxLandCount) {
                    maxLandCount = count;
                    bestCenter = c;
                }
            }
        }
        if (bestCenter == null) {
            return new BoundingBox(-20.0, 20.0, -20.0, 20.0);
        }
        double centerLat = bestCenter.getLatitude();
        double centerLng = bestCenter.getLongitude();
        return new BoundingBox(
            Math.max(-75.0, centerLat - spanLat / 2.0),
            Math.min(75.0, centerLat + spanLat / 2.0),
            Math.max(-170.0, centerLng - spanLng / 2.0),
            Math.min(170.0, centerLng + spanLng / 2.0)
        );
    }

    public static BoundingBox findIsolatedIslandBoundingBox(Scenario s, double spanLat, double spanLng) {
        int h3Res = s.getH3Resolution() > 0 ? Math.min(3, s.getH3Resolution()) : 1;
        PlanetPreset preset = s.getPlanetPreset() != null 
            ? s.getPlanetPreset().withResolution(h3Res) 
            : PlanetPreset.EARTH_LIKE.withResolution(h3Res);
        List<H3Cell> allCells = ProceduralGenerator.getInstance().generatePlanet(preset);
        
        H3Cell bestCenter = null;
        int bestTargetCount = 999999;
        for (H3Cell c : allCells) {
            if (c.getElevation() != null && c.getElevation() > 0.05 && Math.abs(c.getLatitude()) <= 50.0) {
                int count = 0;
                for (H3Cell other : allCells) {
                    if (other.getElevation() != null && other.getElevation() > 0.0 &&
                        Math.abs(other.getLatitude() - c.getLatitude()) <= spanLat / 2.0 &&
                        Math.abs(other.getLongitude() - c.getLongitude()) <= spanLng / 2.0) {
                        count++;
                    }
                }
                if (count >= 5 && count < bestTargetCount) {
                    bestTargetCount = count;
                    bestCenter = c;
                }
            }
        }
        if (bestCenter == null) {
            return new BoundingBox(-25.0, -15.0, 40.0, 50.0);
        }
        double centerLat = bestCenter.getLatitude();
        double centerLng = bestCenter.getLongitude();
        return new BoundingBox(
            Math.max(-75.0, centerLat - spanLat / 2.0),
            Math.min(75.0, centerLat + spanLat / 2.0),
            Math.max(-170.0, centerLng - spanLng / 2.0),
            Math.min(170.0, centerLng + spanLng / 2.0)
        );
    }
}
