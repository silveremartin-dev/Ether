/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.renderers;

import javafx.scene.paint.Color;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.HabitatType;
import org.ether.society.ui.ScientificColorMap;

/**
 * Thematic Layer Renderer for planetary terrain, biomes, bathymetry, albedo, and cryosphere.
 * 
 * <p>Extracted from {@code H3MapCanvas} to decouple the core canvas rendering loop and camera
 * projection from thematic domain coloring, ensuring modularity, high maintainability, and clean SRP.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class TerrainLayerRenderer {

    private final ScientificColorMap scientificColorMap;

    /**
     * Constructs a TerrainLayerRenderer with optional scientific colormap shader.
     *
     * @param colorMap scientific colormap (e.g. TURBO, TERRAIN, VIRIDIS)
     */
    public TerrainLayerRenderer(ScientificColorMap colorMap) {
        this.scientificColorMap = colorMap != null ? colorMap : ScientificColorMap.TERRAIN;
    }

    /**
     * Computes the display color for a biome classification.
     *
     * @param biome terrestrial or marine biome
     * @return JavaFX Color
     */
    public Color getBiomeColor(Biome biome) {
        if (biome == null) return Color.GRAY;

        if (scientificColorMap != null && scientificColorMap != ScientificColorMap.TURBO && scientificColorMap != ScientificColorMap.TERRAIN) {
            double norm = (double) biome.ordinal() / Math.max(1.0, Biome.VALUES.length - 1);
            return scientificColorMap.getColor(norm);
        }

        return switch (biome) {
            case OCEAN -> Color.rgb(25, 50, 150);
            case DEEP_OCEAN -> Color.rgb(15, 30, 100);
            case BEACH -> Color.rgb(238, 214, 175);
            case DESERT -> Color.rgb(237, 201, 175);
            case PLAINS -> Color.rgb(124, 252, 0);
            case SAVANNAH -> Color.rgb(180, 200, 70);
            case FOREST -> Color.rgb(34, 139, 34);
            case JUNGLE -> Color.rgb(0, 100, 0);
            case MOUNTAINS -> Color.rgb(139, 137, 137);
            case HILLS -> Color.rgb(160, 160, 120);
            case TUNDRA -> Color.rgb(221, 221, 187);
            case SNOW -> Color.rgb(255, 250, 250);
            case GLACIER -> Color.rgb(220, 240, 255);
            case LAKE -> Color.rgb(25, 50, 150);
        };
    }

    /**
     * Computes surface albedo reflectance color.
     *
     * @param cell H3 cell
     * @return grayscale reflectance color
     */
    public Color getAlbedoColor(H3Cell cell) {
        if (cell == null) return Color.rgb(40, 40, 40);
        double albedo = cell.getDynamicAlbedo() != null ? cell.getDynamicAlbedo() : 0.30;
        return Color.gray(Math.clamp(albedo, 0.05, 0.95));
    }

    /**
     * Computes ocean acidification pH gradient color.
     *
     * @param cell H3 cell
     * @return color indicating sea surface pH
     */
    public Color getOceanPhColor(H3Cell cell) {
        if (cell == null) return Color.rgb(20, 30, 60);
        if (cell.getBiome() != Biome.OCEAN && cell.getBiome() != Biome.DEEP_OCEAN && cell.getBiome() != Biome.BEACH) {
            return Color.rgb(40, 45, 55); // Grayed-out land
        }
        double temp = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
        double ph = org.ether.society.engines.tier1.OceanAcidificationEngine.calculateHenrySolubility(temp);
        double norm = Math.clamp((ph - 0.03) / 0.05, 0.0, 1.0);
        int r = (int) (30 + norm * 200);
        int g = (int) (140 - norm * 60);
        int b = (int) (220 - norm * 150);
        return Color.rgb(Math.clamp(r, 0, 255), Math.clamp(g, 0, 255), Math.clamp(b, 0, 255));
    }

    /**
     * Computes permafrost stability or active thaw color.
     *
     * @param cell H3 cell
     * @return color indicating permafrost condition
     */
    public Color getPermafrostColor(H3Cell cell) {
        if (cell == null) return Color.rgb(30, 40, 50);
        double lat = Math.abs(cell.getLatitude() != null ? cell.getLatitude() : 0.0);
        if (lat < 50.0) return Color.rgb(50, 60, 70); // Outside boreal zone
        double temp = cell.getTemperature() != null ? cell.getTemperature() : -5.0;
        if (temp < -2.0) return Color.rgb(180, 220, 255); // Stable permafrost
        if (temp < 3.0) return Color.rgb(240, 180, 60);   // Active thaw
        return Color.rgb(220, 50, 50);                     // Thermal collapse
    }

    /**
     * Computes pressurized extraterrestrial habitat infrastructure integrity color.
     *
     * @param cell H3 cell
     * @return status color
     */
    public Color getHabitatInfrastructureColor(H3Cell cell) {
        if (cell == null || cell.getHabitatType() == null || cell.getHabitatType() == HabitatType.NONE) {
            return Color.rgb(30, 35, 45);
        }
        double integrity = cell.getHabitatIntegrity() != null ? cell.getHabitatIntegrity() : 1.0;
        if (integrity >= 0.80) {
            return Color.rgb(16, 185, 129); // Nominal
        } else if (integrity >= 0.40) {
            return Color.rgb(245, 158, 11);  // Degraded
        } else {
            return Color.rgb(239, 68, 68);   // Critical breach
        }
    }
}
