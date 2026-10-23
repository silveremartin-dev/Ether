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
import org.ether.society.ui.ScientificColorMap;

/**
 * Thematic Layer Renderer for biological, agricultural, forestry, and ecological exergy tensors.
 * 
 * <p>Extracted from {@code H3MapCanvas} to decouple the core canvas rendering loop and camera
 * projection from thematic domain coloring, ensuring modularity, high maintainability, and clean SRP.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class BiomassLayerRenderer {

    private final ScientificColorMap scientificColorMap;

    /**
     * Constructs a BiomassLayerRenderer with optional scientific colormap shader.
     *
     * @param colorMap scientific colormap
     */
    public BiomassLayerRenderer(ScientificColorMap colorMap) {
        this.scientificColorMap = colorMap != null ? colorMap : ScientificColorMap.TERRAIN;
    }

    /**
     * Computes food resource caloric stockpile color.
     *
     * @param food caloric stockpile in GJ
     * @return JavaFX Color
     */
    public Color getFoodColor(double food) {
        if (food < 10 && scientificColorMap == ScientificColorMap.TERRAIN) {
            return Color.rgb(60, 40, 20); // Dark brown
        }
        double normalized = Math.clamp(food / 800.0, 0.0, 1.0);
        if (scientificColorMap != null && scientificColorMap != ScientificColorMap.TERRAIN) {
            return scientificColorMap.getColor(normalized);
        }
        return Color.color(0.2 + (1.0 - normalized) * 0.4, 0.2 + normalized * 0.7, 0.1);
    }

    /**
     * Computes water resource availability color.
     *
     * @param water available freshwater
     * @return JavaFX Color
     */
    public Color getWaterColor(Double water) {
        double val = water != null ? water : 0.0;
        double normalized = Math.clamp(val / 1000.0, 0.0, 1.0);
        return Color.color(0.1, 0.2 + normalized * 0.5, 0.4 + normalized * 0.6);
    }

    /**
     * Computes standing timber and combustible forestry biomass color.
     *
     * @param wood standing timber in tonnes
     * @return JavaFX Color
     */
    public Color getWoodColor(Double wood) {
        double val = wood != null ? wood : 0.0;
        double normalized = Math.clamp(val / 800.0, 0.0, 1.0);
        return Color.color(0.3 + (1.0 - normalized) * 0.3, 0.15 + normalized * 0.6, 0.05);
    }

    /**
     * Computes soil fertility and N-P-K stoichiometry health color.
     *
     * @param cell H3 cell
     * @return JavaFX Color
     */
    public Color getSoilQualityColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 30, 60);
        double wood = cell.getWoodResource() != null ? cell.getWoodResource() : 100.0;
        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 100.0;
        double npk = Math.min(1.0, (wood + food) / 1200.0);
        return Color.color(0.4 * (1.0 - npk), 0.2 + npk * 0.7, 0.1);
    }

    /**
     * Computes local ecological biodiversity index color.
     *
     * @param cell H3 cell
     * @return JavaFX Color
     */
    public Color getBiodiversityColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(20, 45, 80);
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        double wood = cell.getWoodResource() != null ? cell.getWoodResource() : 100.0;
        double pristine = Math.max(0.0, Math.min(1.0, (wood / 800.0) - (pop / 1000.0)));
        return Color.color(0.1, 0.3 + pristine * 0.6, 0.2 + pristine * 0.3);
    }
}
