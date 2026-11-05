/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import static org.ether.society.data.WorldClimEmpiricalRasterLoader.*;

/**
 * <h1>EmpiricalBiomeClassifier</h1>
 * <p>
 * Whittaker (1975) / Holdridge (1947) ecological biome classification driven by physical
 * temperature, precipitation, and elevation observables, with optional empirical ICE-6G
 * glacial boundary polygons for Earth historical raster ingestion ({@code AGENTS.md} §3, §6).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class EmpiricalBiomeClassifier {

    private EmpiricalBiomeClassifier() {
    }

    /**
     * Pure physical Whittaker/Holdridge terrestrial biome classifier applicable to any planetary
     * body (Earth, Mars, Venus, exoplanets) strictly from physical $(T, P, z)$ observables,
     * with zero calendar-year or Earth coordinate hardcoding.
     *
     * @param tempC    mean annual surface temperature (°C)
     * @param precipMm annual precipitation (mm/yr)
     * @param elevM    surface elevation above datum (m)
     * @return 24-bit RGB biome identifier code
     */
    public static int classifyPurePhysicalWhittakerBiome(double tempC, double precipMm, double elevM) {
        if (tempC < -15.0 && precipMm >= 80.0) {
            return BIOME_GLACIER;
        }
        if (elevM > 5200.0 || (tempC < -12.0 && elevM > 3800.0)) {
            return BIOME_SNOW;
        }
        if (elevM > 3800.0 && tempC < -4.0) {
            return BIOME_MOUNTAINS;
        }
        if (tempC < -2.0) {
            return BIOME_TUNDRA;
        }
        if (elevM > 3200.0 && tempC < 6.0 && precipMm < 600.0) {
            return BIOME_TUNDRA;
        }
        if (elevM > 1800.0 && precipMm < 800.0 && tempC < 14.0) {
            return BIOME_HILLS;
        }
        if (tempC < 15.0) {
            if (precipMm < 200.0) {
                return BIOME_DESERT;
            } else if (precipMm < 450.0) {
                return BIOME_PLAINS;
            } else {
                return BIOME_FOREST;
            }
        }
        if (precipMm < 150.0) {
            return BIOME_DESERT;
        } else if (precipMm < 750.0) {
            return BIOME_PLAINS;
        } else if (tempC >= 19.0 && precipMm >= 1350.0) {
            return BIOME_JUNGLE;
        } else {
            return BIOME_FOREST;
        }
    }

    /**
     * Classifies an Earth coordinate during empirical paleoclimate raster ingestion ($t = t_0$),
     * combining ICE-6G paleoglacial ice-sheet boundaries with Whittaker physical biome rules.
     */
    public static int classifyBiome(double tempC, double precipMm, double elevM, double lat, double lon, long year) {
        if (lat < -60.0 && tempC < 2.0) return BIOME_GLACIER;
        if (lat > 60.0 && lon > -55.0 && lon < -18.0 && tempC < 2.0) return BIOME_GLACIER;

        if (year <= -40000L && year > -70000L) {
            if (HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_FENNOSCANDIA_MIS3) <= 0 ||
                HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_LAURENTIDE_MIS3) <= 0) {
                return BIOME_GLACIER;
            }
        } else if (year <= -16000L) {
            if (HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_LAURENTIDE_LGM) <= 0 ||
                HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_FENNOSCANDIA_LGM) <= 0 ||
                HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_ALPS_LGM) <= 0 ||
                HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_PATAGONIA_LGM) <= 0) {
                return BIOME_GLACIER;
            }
        } else if (year <= -10500L && year > -16000L) {
            if (HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_LAURENTIDE_YD) <= 0 ||
                HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_FENNOSCANDIA_YD) <= 0) {
                return BIOME_GLACIER;
            }
        }

        return classifyPurePhysicalWhittakerBiome(tempC, precipMm, elevM);
    }
}
