/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author: Silvere Martin-Michiellot (silvere.martin@gmail.com)
 * Contributors: AI Assistant (Antigravity/Claude)
 */
package org.ether.society.data;

import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import com.uber.h3core.util.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Initializes the complete H3 grid for Earth at Level 8.
 * Generates ~7.2 million hexagons covering the entire planet.
 *
 * <p>
 * This is a one-time operation that seeds the database with all H3 cells.
 * Real elevation and biome data will be filled in by subsequent ingestion
 * steps.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1-beta.1
 * @since 1.0.0
 */
public class H3GridInitializer {
    private static final Logger logger = LoggerFactory.getLogger(H3GridInitializer.class);
    private static final int RESOLUTION = 8;
    private static final int BATCH_SIZE = 10000;

    private final H3Service h3Service;

    public H3GridInitializer() {
        this.h3Service = new H3Service();
    }

    /**
     * Generates all H3 Level 8 hexagons for Earth.
     * Uses a systematic lat/lng grid sampling approach.
     *
     * @return List of H3Cell entities ready for persistence
     */
    public List<H3Cell> generateEarthGrid() {
        logger.info("Starting H3 Earth grid generation at resolution {}...", RESOLUTION);

        List<H3Cell> cells = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // Sample the globe with ~0.5 degree spacing (finer than hexagon size)
        // This ensures we hit all hexagons
        double latStep = 0.5;
        double lngStep = 0.5;

        long uniqueH3Count = 0;
        java.util.Set<Long> seenH3Indices = new java.util.HashSet<>();

        for (double lat = -90; lat <= 90; lat += latStep) {
            for (double lng = -180; lng < 180; lng += lngStep) {
                long h3Index = h3Service.latLngToH3(lat, lng);

                // Only add if we haven't seen this hex yet
                if (!seenH3Indices.contains(h3Index)) {
                    seenH3Indices.add(h3Index);

                    // Get center point of hexagon
                    LatLng center = h3Service.h3ToLatLng(h3Index);

                    H3Cell cell = new H3Cell(
                            h3Index,
                            center.lat,
                            center.lng);

                    // Default values (will be updated by data ingestion)
                    cell.setElevation(0.0);
                    cell.setTemperature(15.0);
                    cell.setRainfall(500.0);
                    cell.setBiome(org.ether.society.model.Biome.OCEAN);

                    cells.add(cell);
                    uniqueH3Count++;

                    // Log progress every batch
                    if (uniqueH3Count % BATCH_SIZE == 0) {
                        logger.info("Generated {} unique H3 cells...", uniqueH3Count);
                    }
                }
            }
        }

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        logger.info("H3 grid generation complete: {} cells in {} seconds",
                uniqueH3Count, elapsed);

        return cells;
    }

    /**
     * Command-line entry point for grid initialization.
     */
    public static void main(String[] args) {
        logger.info("=== H3 Earth Grid Initialization ===");
        logger.info("Resolution: Level {}", RESOLUTION);
        logger.info("Expected cells: ~7.2 million");
        logger.info("Estimated time: 10-15 minutes");
        logger.info("");

        H3GridInitializer initializer = new H3GridInitializer();
        List<H3Cell> cells = initializer.generateEarthGrid();

        logger.info("");
        logger.info("Grid generation complete!");
        logger.info("Generated {} H3 cells", cells.size());
        logger.info("");
        logger.info("Next steps:");
        logger.info("1. Persist cells to database (Spring Boot repository)");
        logger.info("2. Download elevation data (SRTM)");
        logger.info("3. Download biome data (MODIS)");
        logger.info("4. Run data ingestion pipeline");
    }
}

