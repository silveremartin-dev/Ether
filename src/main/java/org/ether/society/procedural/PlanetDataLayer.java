/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

/**
 * Types of data layers that can be loaded/generated for a planet.
 * Each layer can exist at multiple resolutions.
 */
public enum PlanetDataLayer {
    /**
     * Elevation data (meters above/below sea level).
     * Sources: ETOPO1, SRTM, procedural generation.
     */
    ELEVATION("Elevation", "m", -11000, 9000),

    /**
     * Biome classification (forest, desert, tundra, etc.).
     * Sources: WWF Ecoregions, procedural based on climate.
     */
    BIOME("Biome", "category", 0, 20),

    /**
     * Underground resource deposits (ores, coal, oil).
     * Sources: USGS MRDS, procedural.
     */
    UNDERGROUND_RESOURCES("Underground Resources", "density", 0, 100),

    /**
     * Fresh water availability (rivers, lakes, aquifers).
     * Scale: 0 = desert, 1 = abundant.
     */
    FRESHWATER("Fresh Water", "index", 0, 1),

    /**
     * Sea surface temperature for ocean cells.
     */
    SEA_TEMPERATURE("Sea Temperature", "°C", -2, 35),

    /**
     * Annual precipitation/rainfall.
     */
    PRECIPITATION("Precipitation", "mm/year", 0, 5000),

    /**
     * Soil fertility (agricultural potential).
     */
    SOIL_FERTILITY("Soil Fertility", "index", 0, 1),

    /**
     * Natural vegetation density.
     */
    VEGETATION_DENSITY("Vegetation", "index", 0, 1);

    private final String displayName;
    private final String unit;
    private final double minValue;
    private final double maxValue;

    PlanetDataLayer(String displayName, String unit, double minValue, double maxValue) {
        this.displayName = displayName;
        this.unit = unit;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUnit() {
        return unit;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    /**
     * Normalize a value to 0-1 range for visualization.
     */
    public double normalize(double value) {
        if (maxValue == minValue)
            return 0.5;
        return (value - minValue) / (maxValue - minValue);
    }

    /**
     * Denormalize from 0-1 back to actual range.
     */
    public double denormalize(double normalizedValue) {
        return minValue + normalizedValue * (maxValue - minValue);
    }
}
