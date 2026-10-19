package org.ether.society.ui.util;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier2.theories.ProceduralPopulationEngine;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;

import java.util.List;

/**
 * <h2>Scenario Cell Population Distributor</h2>
 * <p>
 * Handles the spatial initialization and hypsometric visualization of human demographics across planetary hexagonal cells:
 * <ul>
 *   <li><b>Demographic Sampling</b>: Samples imported density rasters or executes analytical procedural distribution via {@link ProceduralPopulationEngine}.</li>
 *   <li><b>Hypsometric Relief & Heat Tinting</b>: Maps cell elevation, biome classification, and demographic density onto standardized cartographic color ramps.</li>
 *   <li><b>Carrying Capacity Estimation</b>: Calculates localized cellular carrying capacity ($K_{\text{cell}}$) modulated by freshwater aquifers and historical physical capital ($K_0$).</li>
 * </ul>
 * </p>
 *
 * @author Ether Architectural Refactoring Engine
 * @since 1.0.0
 * @see org.ether.society.ui.ScenarioSetupPanel
 * @see org.ether.society.generation.ProceduralPopulationEngine
 */
public final class ScenarioCellPopulationDistributor {

    private ScenarioCellPopulationDistributor() {
        // Utility class
    }

    /**
     * Distributes initial population across land cells according to chosen density pattern or imported map.
     *
     * @param cells list of hexagonal cells
     * @param totalPop total planetary population
     * @param capitalK0 initial physical capital per capita
     * @param pattern density pattern key (e.g. "UNBIASED_NATURAL", "URBAN_CLUSTERS")
     * @param isEarthPreset whether the planetary relief corresponds to Earth
     * @param startYear simulation start year
     * @param customDensityImage imported demographic raster image, or null if procedural
     * @param scenario scenario model configuration
     */
    public static void distributeInitialPopulation(List<H3Cell> cells,
                                                   long totalPop,
                                                   double capitalK0,
                                                   String pattern,
                                                   boolean isEarthPreset,
                                                   long startYear,
                                                   Image customDensityImage,
                                                   Scenario scenario) {
        if (cells == null || cells.isEmpty()) return;

        if (customDensityImage != null && customDensityImage.getPixelReader() != null) {
            List<H3Cell> landCells = cells.stream()
                    .filter(c -> c.getElevation() > 0 && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                    .toList();
            if (landCells.isEmpty()) return;

            double popPerCell = (double) totalPop / landCells.size();
            for (H3Cell c : landCells) {
                double normLat = (c.getLatitude() + 90.0) / 180.0;
                double normLon = (c.getLongitude() + 180.0) / 360.0;
                int px = (int) Math.min(customDensityImage.getWidth() - 1, Math.max(0, normLon * customDensityImage.getWidth()));
                int py = (int) Math.min(customDensityImage.getHeight() - 1, Math.max(0, (1.0 - normLat) * customDensityImage.getHeight()));
                Color imgCol = customDensityImage.getPixelReader().getColor(px, py);
                double factor = imgCol.getBrightness() * 5.0;
                c.setPopulation((int) (popPerCell * factor));
            }
        } else {
            double techLevel = Math.clamp(Math.log10(Math.max(1.0, capitalK0)) * 2.2 + 0.2, 0.2, 10.0);
            ProceduralPopulationEngine.distributePopulation(cells, scenario, totalPop, techLevel, pattern, isEarthPreset, startYear);
        }
    }

    /**
     * Computes the hypsometric relief and demographic density color for preview rendering.
     *
     * @param c target hexagonal cell
     * @return JavaFX Color representing bathymetry, topography, and population density
     */
    public static Color getReliefAndDensityColor(H3Cell c) {
        if (c == null) return Color.rgb(15, 23, 42);

        double elev = c.getElevation() != null ? c.getElevation() : 0.0;
        Biome b = c.getBiome();

        Color baseTerrainCol;
        if (elev <= 0) {
            double depthRatio = Math.clamp(Math.abs(elev) / 4000.0, 0.0, 1.0);
            int r = (int) (30 - depthRatio * 20);
            int g = (int) (95 - depthRatio * 75);
            int bCol = (int) (165 - depthRatio * 115);
            baseTerrainCol = Color.rgb(Math.clamp(r, 0, 255), Math.clamp(g, 0, 255), Math.clamp(bCol, 0, 255));
        } else {
            double altRatio = Math.clamp(elev / 4500.0, 0.0, 1.0);
            int r, g, bCol;
            if (b == Biome.SNOW || elev > 3800) {
                r = 235; g = 245; bCol = 255;
            } else if (b == Biome.TUNDRA) {
                r = 135; g = 155; bCol = 135;
            } else if (b == Biome.DESERT) {
                r = 215; g = 185; bCol = 125;
            } else if (b == Biome.JUNGLE) {
                r = 25; g = 100; bCol = 45;
            } else if (b == Biome.FOREST) {
                r = 35; g = 115; bCol = 50;
            } else if (b == Biome.HILLS || (elev > 1200 && elev <= 2800)) {
                r = 130 + (int)(altRatio * 40);
                g = 120 - (int)(altRatio * 20);
                bCol = 80;
            } else if (b == Biome.MOUNTAINS || elev > 2800) {
                r = 160 + (int)(altRatio * 50);
                g = 155 + (int)(altRatio * 45);
                bCol = 150 + (int)(altRatio * 45);
            } else {
                r = 65; g = 135; bCol = 55;
            }

            r = Math.clamp((int)(r * (0.85 + altRatio * 0.35)), 0, 255);
            g = Math.clamp((int)(g * (0.85 + altRatio * 0.35)), 0, 255);
            bCol = Math.clamp((int)(bCol * (0.85 + altRatio * 0.35)), 0, 255);
            baseTerrainCol = Color.rgb(r, g, bCol);
        }

        if (elev <= 0) {
            return baseTerrainCol;
        }

        long pop = c.getPopulation() != null ? c.getPopulation() : 0;

        Color heatCol;
        if (pop <= 0) {
            heatCol = Color.rgb(30, 95, 165);
        } else if (pop < 50) {
            heatCol = Color.rgb(16, 185, 129);
        } else if (pop < 500) {
            heatCol = Color.rgb(234, 179, 8);
        } else if (pop < 2500) {
            heatCol = Color.rgb(249, 115, 22);
        } else {
            heatCol = Color.rgb(239, 68, 68);
        }

        return heatCol;
    }

    /**
     * Computes the ecological and physical carrying capacity of a hexagonal cell.
     *
     * @param c target hexagonal cell
     * @param capK0 physical capital level ($K_0$)
     * @return cellular carrying capacity
     */
    public static double computeCellCarryingCapacity(H3Cell c, double capK0) {
        if (c == null || c.getElevation() <= 0) return 0.0;
        double baseCap = 250.0;
        Biome b = c.getBiome();
        if (b == Biome.DESERT || b == Biome.TUNDRA || b == Biome.SNOW) baseCap *= 0.1;
        else if (b == Biome.PLAINS || b == Biome.FOREST) baseCap *= 1.5;
        else if (b == Biome.JUNGLE) baseCap *= 0.8;

        if (c.getWaterResource() != null && c.getWaterResource() > 0.1) {
            baseCap *= (1.0 + 3.0 * (c.getWaterResource() / 1000.0));
        }
        if (c.getFreshwaterAquifer() != null && c.getFreshwaterAquifer() > 0.1) {
            baseCap *= (1.0 + 1.5 * (c.getFreshwaterAquifer() / 1000.0));
        }

        baseCap *= Math.max(0.5, (capK0 / 1000.0) * 0.8);
        return Math.max(10.0, baseCap);
    }
}
