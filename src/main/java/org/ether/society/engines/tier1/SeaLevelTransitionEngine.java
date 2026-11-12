/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @version 1.0.0-beta.3
 */
public class SeaLevelTransitionEngine extends AbstractSimulationEngine {
    public SeaLevelTransitionEngine() {
        super("SeaLevelTransitionEngine", "SeaLevelTransitionEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }


    private static final Logger logger = LoggerFactory.getLogger(SeaLevelTransitionEngine.class);

    @Override
    public void tick(WorldBuffer buf, double dtYears, double dtMonthly) {
        // Sea level transition is an epoch/climate boundary event applied explicitly via applySeaLevelTransition(WorldBuffer, double).
    }

    /**
     * Applies a global sea level offset (in meters) across all DoD buffer cells.
     *
     * @param buffer WorldBuffer DoD state
     * @param targetSeaLevelOffsetMeters Sea level offset in meters (e.g., -120.0 for LGM, 0.0 for modern, +70.0 for full ice melt)
     */
    public static void applySeaLevelTransition(WorldBuffer buffer, double targetSeaLevelOffsetMeters) {
        if (buffer == null || buffer.getCapacity() == 0) return;

        int n = buffer.getCapacity();
        logger.info("Applying Dynamic Sea Level Transition: offset = {} meters across {} cells",
                targetSeaLevelOffsetMeters, n);

        int submergedCount = 0;
        int emergedCount = 0;

        float[] elevArr = buffer.getElevation();
        byte[] biomes = buffer.getBiomes();
        float[] waterArr = buffer.getWaterResource();
        float[] frictionArr = buffer.getFluxPressure();
        long[] h3Arr = buffer.getH3Indexes();
        H3Service h3Service = H3Service.getInstance();

        byte oceanOrd = (byte) Biome.OCEAN.ordinal();
        byte deepOceanOrd = (byte) Biome.DEEP_OCEAN.ordinal();
        byte beachOrd = (byte) Biome.BEACH.ordinal();
        byte glacierOrd = (byte) Biome.GLACIER.ordinal();
        byte tundraOrd = (byte) Biome.TUNDRA.ordinal();
        byte forestOrd = (byte) Biome.FOREST.ordinal();
        byte savannahOrd = (byte) Biome.SAVANNAH.ordinal();
        byte plainsOrd = (byte) Biome.PLAINS.ordinal();

        for (int i = 0; i < n; i++) {
            double rawElevation = elevArr[i];
            double lat = 0.0;
            if (h3Arr != null && h3Arr[i] != 0L) {
                try {
                    lat = h3Service.cellToLatLng(h3Arr[i]).lat;
                } catch (Exception ignored) {
                    lat = 0.0;
                }
            }
            double absLat = Math.abs(lat);
            byte currentBiome = biomes[i];

            boolean isSubmerged = (rawElevation < targetSeaLevelOffsetMeters);

            if (isSubmerged) {
                if (currentBiome != oceanOrd && currentBiome != deepOceanOrd) {
                    submergedCount++;
                }

                if (rawElevation < (targetSeaLevelOffsetMeters - 200.0)) {
                    biomes[i] = deepOceanOrd;
                } else {
                    biomes[i] = oceanOrd;
                }

                waterArr[i] = 0.0f;
                frictionArr[i] = 0.2f;
            } else {
                if (currentBiome == oceanOrd || currentBiome == deepOceanOrd) {
                    emergedCount++;
                    double effectiveElevationAboveSea = rawElevation - targetSeaLevelOffsetMeters;

                    if (effectiveElevationAboveSea <= 10.0) {
                        biomes[i] = beachOrd;
                    } else if (absLat > 70.0) {
                        biomes[i] = glacierOrd;
                    } else if (absLat > 58.0) {
                        biomes[i] = tundraOrd;
                    } else if (absLat > 40.0) {
                        biomes[i] = forestOrd;
                    } else if (absLat < 20.0) {
                        biomes[i] = savannahOrd;
                    } else {
                        biomes[i] = plainsOrd;
                    }
                }

                if (waterArr[i] <= 0.0f) {
                    double precipFactor = Math.max(100.0, 1000.0 * (1.0 - absLat / 90.0));
                    waterArr[i] = (float) precipFactor;
                }
                if (frictionArr[i] <= 0.2f) {
                    frictionArr[i] = 1.0f;
                }
            }
        }

        logger.info("Sea Level Transition Complete: {} cells submerged, {} cells emerged.",
                submergedCount, emergedCount);
    }

    /**
     * Applies a global sea level offset (in meters) to a single {@link H3Cell} during initial world setup.
     *
     * @param cell target H3 cell
     * @param targetSeaLevelOffsetMeters sea level offset in meters
     */
    public static void applySeaLevelTransitionCell(H3Cell cell, double targetSeaLevelOffsetMeters) {
        if (cell == null) return;
        cell.setSeaLevelOffsetMeters(targetSeaLevelOffsetMeters);
        Double rawElevation = cell.getElevation();
        if (rawElevation == null) rawElevation = 0.0;

        double lat = cell.getLatitude();
        double absLat = Math.abs(lat);
        Biome currentBiome = cell.getBiome();

        if (rawElevation < targetSeaLevelOffsetMeters) {
            if (rawElevation < (targetSeaLevelOffsetMeters - 200.0)) {
                cell.setBiome(Biome.DEEP_OCEAN);
            } else {
                cell.setBiome(Biome.OCEAN);
            }
            cell.setWaterResource(0.0);
            cell.setMovementFriction(0.2);
        } else {
            if (currentBiome == Biome.OCEAN || currentBiome == Biome.DEEP_OCEAN) {
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
            if (cell.getWaterResource() == null || cell.getWaterResource() <= 0.0) {
                double precipFactor = Math.max(100.0, 1000.0 * (1.0 - absLat / 90.0));
                cell.setWaterResource(precipFactor);
            }
            if (cell.getMovementFriction() == null || cell.getMovementFriction() <= 0.2) {
                cell.setMovementFriction(1.0);
            }
        }
    }
}


