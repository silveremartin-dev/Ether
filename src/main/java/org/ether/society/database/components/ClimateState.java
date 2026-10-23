/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.database.components;

/**
 * High-performance Data-Oriented immutable component encapsulating planetary atmospheric, thermal, hydrologic and climate variables.
 *
 * @param temperature Celsius surface temperature
 * @param rainfall annual precipitation in millimeters
 * @param dynamicAlbedo surface albedo [0.10 dark forest to 0.85 snow/ice]
 * @param iceSheetThicknessMeters continental glacier thickness in meters
 * @param seaLevelOffsetMeters eustatic sea level offset relative to present in meters
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public record ClimateState(
        double temperature,
        double rainfall,
        double dynamicAlbedo,
        double iceSheetThicknessMeters,
        double seaLevelOffsetMeters
) {
    /** Standard terrestrial Holocene baseline climate state. */
    public static final ClimateState DEFAULT = new ClimateState(15.0, 800.0, 0.30, 0.0, 0.0);
}
