/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

/**
 * Elevation presets for terrain generation.
 * 
 * Controls the shape and distribution of landmasses independent of climate.
 */
public enum ElevationPreset {
    FLAT("Flat Terrain",
            "Minimal elevation variation - plains and shallow seas",
            0.3, 0.1, 1000),

    HILLY("Rolling Hills",
            "Moderate hills and valleys with gentle slopes",
            0.5, 0.3, 3000),

    MOUNTAINOUS("Mountainous",
            "Dramatic elevation with high mountain ranges",
            0.6, 0.6, 8000),

    OCEANIC("Oceanic World",
            "Large oceans with scattered island chains",
            0.75, 0.2, 2000),

    PANGEA("Supercontinent",
            "Single large landmass surrounded by ocean",
            0.5, 0.4, 5000),

    ARCHIPELAGO("Island Archipelago",
            "Many small to medium islands spread across oceans",
            0.7, 0.25, 2500),

    CONTINENTAL("Continental",
            "Earth-like with varied continents and oceans",
            0.55, 0.5, 6000),

    EXTREME("Extreme Relief",
            "Very deep trenches and towering peaks",
            0.5, 0.9, 12000);

    private final String displayName;
    private final String description;
    private final double waterCoverage; // 0-1 how much is ocean
    private final double roughness; // 0-1 terrain variation
    private final int maxElevationMeters; // Peak height

    ElevationPreset(String displayName, String description,
            double waterCoverage, double roughness, int maxElevationMeters) {
        this.displayName = displayName;
        this.description = description;
        this.waterCoverage = waterCoverage;
        this.roughness = roughness;
        this.maxElevationMeters = maxElevationMeters;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public double getWaterCoverage() {
        return waterCoverage;
    }

    public double getRoughness() {
        return roughness;
    }

    public int getMaxElevationMeters() {
        return maxElevationMeters;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
