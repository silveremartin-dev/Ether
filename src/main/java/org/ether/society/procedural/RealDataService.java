/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.h3.H3Service;
import com.uber.h3core.util.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to load and interpolate "Real Earth" data.
 * Currently uses procedural approximations or placeholders where data is
 * missing.
 */
public class RealDataService {
    private static final Logger logger = LoggerFactory.getLogger(RealDataService.class);
    // private static final int H3_RESOLUTION = 6; // Coarser for "Earth" demo to
    // avoid too many cells

    /**
     * Generates/Loads Earth data.
     * In a real implementation, this would read from GeoTIFF or database.
     * Here we approximate Earth-like continents.
     */
    public static List<H3Cell> loadEarthData() {
        logger.info("Loading 'Real Earth' data...");
        H3Service h3Service = H3Service.getInstance();

        // 1. Try to load from images
        try (var elevStream = RealDataService.class.getResourceAsStream("/maps/earth_elevation.png");
                var biomeStream = RealDataService.class.getResourceAsStream("/maps/earth_biomes.png")) {

            if (elevStream != null || biomeStream != null) {
                logger.info("Found map images in /maps/. Importing...");

                // Generate blank cells first based on resolution
                // Earth needs high resolution, let's use res 6 (approx 5000 cells?)
                // Res 5 = 2000 cells, Res 6 = 14000 cells
                // Let's use 5 (fewer cells) for performance unless strictly necessary
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
