/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Dynamic Albedo & Climate Coupling Engine.
 * Modifies local surface albedo and planetary energy balance based on land cover,
 * deforestation, and polar ice extent.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.5.0
 */
public class AlbedoClimateEngine {
    private static final Logger logger = LoggerFactory.getLogger(AlbedoClimateEngine.class);

    /**
     * Updates dynamic albedo and adjusts local temperature feedback loops across all cells.
     */
    public static void processAlbedoFeedback(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            double albedo = cell.calculateDynamicAlbedo();

            // Radiative forcing feedback approximation:
            // High albedo (ice/snow 0.8) reflects solar radiation -> lowers temperature
            // Low albedo (dark forest 0.12) absorbs heat -> increases temperature
            double currentTemp = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
            double albedoDelta = (0.30 - albedo) * 2.5; // Baseline albedo 0.30

            cell.setTemperature(currentTemp + albedoDelta * 0.10);
        }
    }
}
