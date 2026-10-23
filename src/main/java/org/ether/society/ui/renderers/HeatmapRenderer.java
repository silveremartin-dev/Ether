/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.renderers;

import javafx.scene.paint.Color;
import org.ether.society.database.H3Cell;
import org.ether.society.ui.ScientificColorMap;

/**
 * Thematic Layer Renderer for continuous demographic, thermodynamic, hydrologic, and macroeconomic heatmaps.
 * 
 * <p>Extracted from {@code H3MapCanvas} to decouple the core canvas rendering loop and camera
 * projection from thematic domain coloring, ensuring modularity, high maintainability, and clean SRP.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class HeatmapRenderer {

    private final ScientificColorMap scientificColorMap;

    /**
     * Constructs a HeatmapRenderer.
     *
     * @param colorMap scientific colormap
     */
    public HeatmapRenderer(ScientificColorMap colorMap) {
        this.scientificColorMap = colorMap != null ? colorMap : ScientificColorMap.TURBO;
    }

    /**
     * Computes population density heatmap color.
     *
     * @param population head count
     * @param isWater true if ocean / water cell
     * @return JavaFX Color
     */
    public Color getPopulationColor(int population, boolean isWater) {
        if (isWater) {
            return Color.rgb(12, 24, 50);
        }
        if (population <= 0) {
            return Color.rgb(25, 35, 45);
        }

        double normalized = Math.clamp(Math.log1p(population) / Math.log1p(500), 0.0, 1.0);
        if (scientificColorMap != null && scientificColorMap != ScientificColorMap.TERRAIN) {
            return scientificColorMap.getColor(normalized);
        }

        if (normalized < 0.25) {
            double t = normalized / 0.25;
            return Color.rgb(0, (int) (120 + t * 135), 255);
        } else if (normalized < 0.5) {
            double t = (normalized - 0.25) / 0.25;
            return Color.rgb(0, 255, (int) (255 * (1 - t)));
        } else if (normalized < 0.75) {
            double t = (normalized - 0.5) / 0.25;
            return Color.rgb((int) (255 * t), 255, 0);
        } else {
            double t = (normalized - 0.75) / 0.25;
            return Color.rgb(255, (int) (255 * (1 - t)), 0);
        }
    }

    /**
     * Computes temperature heatmap color.
     *
     * @param tempCelsius temperature in °C
     * @return JavaFX Color
     */
    public Color getTemperatureColor(double tempCelsius) {
        double normalized = Math.clamp((tempCelsius + 20.0) / 60.0, 0.0, 1.0);
        if (scientificColorMap != null && scientificColorMap != ScientificColorMap.TERRAIN) {
            return scientificColorMap.getColor(normalized);
        }

        if (normalized < 0.25) {
            double t = normalized / 0.25;
            return Color.rgb((int) (200 + t * 55), (int) (220 + t * 35), 255);
        } else if (normalized < 0.5) {
            double t = (normalized - 0.25) / 0.25;
            return Color.rgb(0, (int) (100 + t * 155), 255);
        } else if (normalized < 0.75) {
            double t = (normalized - 0.5) / 0.25;
            return Color.rgb((int) (t * 255), 255, (int) ((1 - t) * 255));
        } else {
            double t = (normalized - 0.75) / 0.25;
            return Color.rgb(255, (int) (255 * (1 - t)), 0);
        }
    }

    /**
     * Computes precipitation heatmap color.
     *
     * @param precipMm precipitation in mm
     * @return JavaFX Color
     */
    public Color getPrecipitationColor(double precipMm) {
        double normalized = Math.clamp(precipMm / 2500.0, 0.0, 1.0);
        if (scientificColorMap != null && scientificColorMap != ScientificColorMap.TERRAIN) {
            return scientificColorMap.getColor(normalized);
        }

        if (normalized < 0.2) {
            double t = normalized / 0.2;
            return Color.rgb((int) (220 - t * 40), (int) (220 - t * 10), (int) (200 - t * 60));
        } else if (normalized < 0.5) {
            double t = (normalized - 0.2) / 0.3;
            return Color.rgb((int) (180 - t * 120), (int) (210 - t * 20), (int) (140 + t * 20));
        } else if (normalized < 0.8) {
            double t = (normalized - 0.5) / 0.3;
            return Color.rgb((int) (60 - t * 30), (int) (190 - t * 60), (int) (160 + t * 60));
        } else {
            double t = (normalized - 0.8) / 0.2;
            return Color.rgb((int) (30 - t * 20), (int) (130 - t * 80), (int) (220 - t * 60));
        }
    }

    /**
     * Computes technological advancement level color.
     *
     * @param techLevel technology level scalar
     * @return JavaFX Color
     */
    public Color getTechColor(Double techLevel) {
        if (techLevel == null || techLevel <= 0) {
            return Color.rgb(30, 30, 30);
        }
        double normalized = Math.clamp(techLevel / 10.0, 0.0, 1.0);
        if (scientificColorMap != null && scientificColorMap != ScientificColorMap.TERRAIN) {
            return scientificColorMap.getColor(normalized);
        }
        int r = (int) (80 + normalized * 100);
        int g = (int) (20 + normalized * 235);
        int b = (int) (150 + normalized * 105);
        return Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    /**
     * Computes Gini inequality index heatmap color.
     *
     * @param gini Gini scalar [0, 1]
     * @return JavaFX Color
     */
    public Color getGiniColor(Double gini) {
        if (gini == null || gini <= 0) return Color.rgb(30, 30, 40);
        double normalized = Math.clamp(gini, 0.0, 1.0);
        if (scientificColorMap != null && scientificColorMap != ScientificColorMap.TERRAIN) {
            return scientificColorMap.getColor(normalized);
        }
        int r = (int) (normalized * 255);
        int g = (int) ((1.0 - normalized) * 220);
        return Color.rgb(r, g, 40);
    }

    /**
     * Computes macroeconomic GDP / aggregate capital density color.
     *
     * @param cell H3 cell
     * @return JavaFX Color
     */
    public Color getGdpWealthColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
        double gdp = Math.min(1.0, (pop * tech) / 2500.0);
        return Color.color(gdp, gdp * 0.8, 0.2 * (1.0 - gdp));
    }

    /**
     * Computes epidemiological pathogen infection outbreak color.
     *
     * @param cell H3 cell
     * @return JavaFX Color
     */
    public Color getEpidemicColor(H3Cell cell) {
        int infected = cell.getEpidemicInfected() != null ? cell.getEpidemicInfected() : 0;
        if (infected == 0) return Color.rgb(40, 50, 60);
        double norm = Math.min(1.0, infected / 200.0);
        return Color.rgb(255, (int) ((1 - norm) * 100), (int) ((1 - norm) * 100));
    }
}
