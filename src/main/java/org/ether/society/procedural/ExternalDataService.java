/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 *
 * Author: Silvere Martin-Michiellot
 * Contributors: Gemini AI
 * Since: 1.0
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.h3.H3Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

/**
 * Service to load and interpolate "External" data (Real Earth, etc.).
 * Currently uses procedural approximations or images.
 * 
 * @author Silvere Martin-Michiellot
 * @since 1.0
 */
public class ExternalDataService {
    private static final Logger logger = LoggerFactory.getLogger(ExternalDataService.class);

    /**
     * Generates/Loads Earth data.
     * In a real implementation, this would read from GeoTIFF or database.
     * Here we approximate Earth-like continents using maps or procedural fallback.
     */
    public static List<H3Cell> loadEarthData() {
        logger.info("Loading 'External Earth' data...");
        H3Service h3Service = H3Service.getInstance();

        // 1. Try to load from images
        try (var elevStream = ExternalDataService.class.getResourceAsStream("/maps/earth_elevation.png");
                var biomeStream = ExternalDataService.class.getResourceAsStream("/maps/earth_biomes.png")) {

            if (elevStream != null || biomeStream != null) {
                logger.info("Found map images in /maps/. Importing...");

                // Generate blank cells first based on resolution
                // Earth needs high resolution, let's use res 6 (approx 14k cells)
                int res = 5;
                List<H3Cell> cells = h3Service.generateGlobalMetadata(res);

                // Map image data
                org.ether.society.data.ImageMapLoader loader = new org.ether.society.data.ImageMapLoader();
                loader.mapDataToCells(cells, elevStream, biomeStream);

                // Post-process: Ensure basic data consistency
                for (H3Cell c : cells) {
                    if (c.getBiome() == null)
                        c.setBiome(Biome.OCEAN);
                    // Initialize other fields
                    c.setTemperature(15.0); // Default
                    c.setRainfall(1000.0);
                }

                return cells;
            } else {
                logger.warn("No 'earth_elevation.png' found in /maps/. Using procedural approximation.");
            }
        } catch (Exception e) {
            logger.error("Failed to load map images", e);
        }

        // 2. Fallback to Procedural "Earth-like"
        // Use a known seed that produces earth-like continents
        org.ether.society.procedural.PlanetPreset earthConfig = new org.ether.society.procedural.PlanetPreset(
                "Earth-like",
                5, // Resolution 5
                112358L, // Seed
                0.5, // Freq
                1.5, // Scale
                0.65, // Water
                40.0 // Temp Gradient
        );

        return new org.ether.society.procedural.ProceduralGenerator().generatePlanet(earthConfig);
    }
}
