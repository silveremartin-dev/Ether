/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

/**
 * Climate presets for biome generation.
 * 
 * Controls temperature, precipitation, and resulting biome distribution.
 * Independent of terrain elevation shape.
 */
public enum ClimatePreset {
    TEMPERATE("Temperate",
            "Moderate temperatures with distinct seasons (Earth-like)",
            15.0, 1000.0, 0.5),

    TROPICAL("Tropical",
            "Hot and humid with jungles and rainforests",
            28.0, 2500.0, 0.7),

    ARID("Arid/Desert",
            "Hot and dry with sparse vegetation",
            30.0, 200.0, 0.2),

    ARCTIC("Arctic",
            "Cold with tundra and ice coverage",
            -10.0, 300.0, 0.4),

    ICE_AGE("Ice Age",
            "Glacial period with expanded ice sheets",
            -5.0, 400.0, 0.5),

    GREENHOUSE("Greenhouse",
            "Warm pole-to-pole with no ice caps",
            22.0, 1500.0, 0.6),

    VARIABLE("Variable",
            "Highly varied climate zones (Earth maximum diversity)",
            12.0, 1200.0, 0.6),

    MONSOON("Monsoon",
            "Seasonal wet/dry cycles with heavy rains",
            25.0, 3000.0, 0.8);

    private final String displayName;
    private final String description;
    private final double avgTemperatureCelsius;
    private final double avgPrecipitationMm;
    private final double seasonality; // 0-1 how much seasons vary

    ClimatePreset(String displayName, String description,
            double avgTemperatureCelsius, double avgPrecipitationMm, double seasonality) {
        this.displayName = displayName;
        this.description = description;
        this.avgTemperatureCelsius = avgTemperatureCelsius;
        this.avgPrecipitationMm = avgPrecipitationMm;
        this.seasonality = seasonality;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public double getAvgTemperatureCelsius() {
        return avgTemperatureCelsius;
    }

    public double getAvgPrecipitationMm() {
        return avgPrecipitationMm;
    }

    public double getSeasonality() {
        return seasonality;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
