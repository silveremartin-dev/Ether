/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.generation;

import org.ether.society.model.Biome;

import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

/**
 * Procedural Planet Raster Synthesizer.
 * Synthesizes equirectangular raster suites (elevation, biomes, temperature, rainfall, seasonality)
 * using deterministic spherical Simplex noise.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ProceduralPlanetRasterSynthesizer {

    private ProceduralPlanetRasterSynthesizer() {
        // Utility class
    }

    /**
     * Immutable suite of synthesized equirectangular rasters.
     */
    public record ProceduralRasterSuite(
            BufferedImage elevation,
            BufferedImage biomes,
            BufferedImage temperature,
            BufferedImage rainfall,
            BufferedImage seasonality) {}

    /**
     * Synthesizes a complete set of procedural equirectangular rasters ($2048 \times 1024$)
     * using deterministic spherical Simplex noise so procedural planets pass through the exact same
     * spherical H3 aggregation pipeline as empirical maps.
     *
     * @param generator parent procedural generator
     * @param preset planet generation preset
     * @param width target raster width
     * @param height target raster height
     * @return immutable {@link ProceduralRasterSuite}
     */
    public static ProceduralRasterSuite synthesizeProceduralPlanetRasters(
            ProceduralGenerator generator, PlanetPreset preset, int width, int height) {
        int w = width > 0 ? width : 2048;
        int h = height > 0 ? height : 1024;
        var elevImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var biomeImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var tempImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var rainImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var seasImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        double minAlt = preset.minAltitudeMeters();
        double maxAlt = preset.maxAltitudeMeters();
        double altRange = Math.max(1.0, maxAlt - minAlt);

        IntStream.range(0, h).parallel().forEach(y -> {
            double lat = 90.0 - ((y + 0.5) / h) * 180.0;
            for (int x = 0; x < w; x++) {
                double lon = -180.0 + ((x + 0.5) / w) * 360.0;
                ProceduralGenerator.PlanetPoint p = generator.getPlanetPoint(lat, lon, preset);

                double elevNorm = Math.clamp((p.elevationMeters() - minAlt) / altRange, 0.0, 1.0);
                int eGray = (int) Math.round(elevNorm * 255.0);
                elevImg.setRGB(x, y, (eGray << 16) | (eGray << 8) | eGray);

                biomeImg.setRGB(x, y, biomeToStandardRgb(p.biome()));

                double tNorm = Math.clamp((p.temperature() + 50.0) / 100.0, 0.0, 1.0);
                int tGray = (int) Math.round(tNorm * 255.0);
                tempImg.setRGB(x, y, (tGray << 16) | (tGray << 8) | tGray);

                double rNorm = Math.clamp(p.rainfall(), 0.0, 1.0);
                int rGray = (int) Math.round(rNorm * 255.0);
                rainImg.setRGB(x, y, (rGray << 16) | (rGray << 8) | rGray);

                double sNorm = Math.clamp(p.seasonality(), 0.0, 1.0);
                int sGray = (int) Math.round(sNorm * 255.0);
                seasImg.setRGB(x, y, (sGray << 16) | (sGray << 8) | sGray);
            }
        });

        return new ProceduralRasterSuite(elevImg, biomeImg, tempImg, rainImg, seasImg);
    }

    public static Biome determineBiome(double lat, double elevation, double elevMeters,
                                       double tempC, double rainfall, PlanetPreset preset, double tidalForce) {
        double waterLevel = preset.waterLevel();
        double pAtmo = preset.atmospherePressureAtm();

        // Airless Worlds
        if (pAtmo < 0.01) {
            if (tempC < -50.0) return Biome.SNOW;
            if (elevation > 0.6) return Biome.MOUNTAINS;
            return elevation > 0.3 ? Biome.HILLS : Biome.DESERT;
        }

        // Ocean & Polar Ice Cap biomes
        if (elevation < waterLevel) {
            if (tempC < -2.0 || Math.abs(lat) > 72.0) return Biome.GLACIER;
            return (elevation < waterLevel - 0.40) ? Biome.DEEP_OCEAN : Biome.OCEAN;
        }

        // Coastal intertidal beach strip (width expands with gravitational tidal forces)
        double intertidalWidth = 0.03 + 0.02 * Math.min(3.0, tidalForce);
        if (elevation < waterLevel + intertidalWidth) return Biome.BEACH;
        if (tempC < -15.0 || elevation > 0.85 || Math.abs(lat) > 75.0) return Biome.GLACIER;
        if (tempC < -8.0 || elevation > 0.75) return Biome.SNOW;
        if (elevation > 0.55) return Biome.MOUNTAINS;
        if (elevation > 0.35) return Biome.HILLS;
        if (tempC < 0.0) return Biome.TUNDRA;
        if (rainfall < 0.20) return Biome.DESERT;
        if (tempC > 18.0 && rainfall > 0.70) return Biome.JUNGLE;
        return rainfall > 0.50 ? Biome.FOREST : Biome.PLAINS;
    }

    public static int biomeToStandardRgb(Biome b) {
        return switch (b) {
            case DEEP_OCEAN -> 0x000028;
            case OCEAN -> 0x0032C8;
            case BEACH -> 0xF0DC96;
            case PLAINS -> 0x64C832;
            case FOREST -> 0x147814;
            case JUNGLE -> 0x005000;
            case DESERT -> 0xFFC832;
            case HILLS -> 0x969664;
            case MOUNTAINS -> 0x646464;
            case SAVANNAH -> 0xB4C846;
            case GLACIER -> 0xDCF0FF;
            case TUNDRA -> 0x96C8DC;
            case SNOW -> 0xFFFFFF;
            case LAKE -> 0x1E78DC;
        };
    }
}
