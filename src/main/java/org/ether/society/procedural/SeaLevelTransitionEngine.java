/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Dynamic Sea Level Transition & Coastal Inundation/Emergence Engine.
 *
 * Models physical coastlines, continental shelf emergence, and marine inundation:
 * 1. Glacial Regression (e.g. -120m during LGM): Exposes continental shelves (Doggerland, Beringia, Sundaland, Sahul).
 *    Submerged cells rising above sea level dynamically transform into terrestrial biomes (Beach, Tundra, Plains).
 * 2. Marine Inundation (e.g. +10m to +70m deglaciation): Coastal land cells falling below sea level submerge
 *    into Ocean / Deep Ocean, driving population relocation and shifting trade routes.
 * 3. Updates coastal status, movement friction, and potable water availability across all cells.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class SeaLevelTransitionEngine {

    private static final Logger logger = LoggerFactory.getLogger(SeaLevelTransitionEngine.class);

    /**
     * Applies a global sea level offset (in meters) across all H3 cells.
     *
     * @param cells List of H3 cells to process
     * @param targetSeaLevelOffsetMeters Sea level offset in meters (e.g., -120.0 for LGM, 0.0 for modern, +70.0 for full ice melt)
     */
    public static void applySeaLevelTransition(List<H3Cell> cells, double targetSeaLevelOffsetMeters) {
        if (cells == null || cells.isEmpty()) return;

        logger.info("🌊 Applying Dynamic Sea Level Transition: offset = {} meters across {} cells",
                targetSeaLevelOffsetMeters, cells.size());

        int submergedCount = 0;
        int emergedCount = 0;

        for (H3Cell cell : cells) {
            cell.setSeaLevelOffsetMeters(targetSeaLevelOffsetMeters);
            Double rawElevation = cell.getElevation();
            if (rawElevation == null) rawElevation = 0.0;

            double lat = cell.getLatitude();
            double absLat = Math.abs(lat);
            Biome currentBiome = cell.getBiome();

            // Cell is submerged if its absolute elevation is below the sea level offset
            boolean isSubmerged = (rawElevation < targetSeaLevelOffsetMeters);

            if (isSubmerged) {
                if (currentBiome != Biome.OCEAN && currentBiome != Biome.DEEP_OCEAN) {
                    submergedCount++;
                }

                // Submerged cell biome assignment
                if (rawElevation < (targetSeaLevelOffsetMeters - 200.0)) {
                    cell.setBiome(Biome.DEEP_OCEAN);
                } else {
                    cell.setBiome(Biome.OCEAN);
                }

                // Water resource for ocean cells
                cell.setWaterResource(0.0);
                cell.setMovementFriction(0.2); // Low friction for maritime navigation
            } else {
                // Cell is above sea level (land)
                if (currentBiome == Biome.OCEAN || currentBiome == Biome.DEEP_OCEAN) {
                    emergedCount++;
                    // Re-evaluate biome for newly emerged continental shelf (e.g., Doggerland, Beringia)
                    double effectiveElevationAboveSea = rawElevation - targetSeaLevelOffsetMeters;

                    if (effectiveElevationAboveSea <= 10.0) {
                        cell.setBiome(Biome.BEACH);
                    } else if (absLat > 70.0) {
                        cell.setBiome(Biome.GLACIER);
                    } else if (absLat > 58.0) {
                        cell.setBiome(Biome.TUNDRA);
                    } else if (absLat > 40.0) {
                        cell.setBiome(Biome.FOREST);
                    } else if (absLat < 20.0) {
                        cell.setBiome(Biome.SAVANNAH);
                    } else {
                        cell.setBiome(Biome.PLAINS);
                    }
                }

                // Ensure non-zero water resource and terrestrial friction for land cells
                if (cell.getWaterResource() == null || cell.getWaterResource() <= 0.0) {
                    double precipFactor = Math.max(100.0, 1000.0 * (1.0 - absLat / 90.0));
                    cell.setWaterResource(precipFactor);
                }
                if (cell.getMovementFriction() == null || cell.getMovementFriction() <= 0.2) {
                    cell.setMovementFriction(1.0);
                }
            }
        }

        logger.info("🌊 Sea Level Transition Complete: {} cells submerged, {} cells emerged.",
                submergedCount, emergedCount);
    }
}

