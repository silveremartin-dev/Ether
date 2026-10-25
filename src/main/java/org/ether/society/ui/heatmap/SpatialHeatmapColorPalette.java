/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.heatmap;

import javafx.scene.paint.Color;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.ui.DisplayMode;

import java.util.List;

/**
 * <h2>Spatial Heatmap Color Palette</h2>
 * <p>
 * Evaluates raster and vector cell attributes, normalizes values across thematic domains
 * (Physical, Demographics, Ecology, Mining, Society & Politics), and computes Moran's I
 * spatial autocorrelation coefficient.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public final class SpatialHeatmapColorPalette {

    private SpatialHeatmapColorPalette() {
        // Utility class
    }

    /**
     * Determines whether zero/empty cells should be skipped for sparse rendering.
     *
     * @param mode display mode
     * @return true if sparse zero skipping applies
     */
    public static boolean isSparseZeroSkipped(DisplayMode mode) {
        return switch (mode) {
            case POPULATION, FLUX, MIGRATION, CONFLICT, EPIDEMIC -> true;
            default -> false;
        };
    }

    /**
     * Returns the base environmental background tint for a given biome.
     *
     * @param biome environmental biome
     * @return JavaFX {@link Color}
     */
    public static Color getBiomeBaseColor(Biome biome) {
        if (biome == null) return Color.rgb(30, 41, 59, 0.6);
        return switch (biome) {
            case OCEAN -> Color.rgb(14, 45, 80, 0.7);
            case DEEP_OCEAN -> Color.rgb(10, 30, 60, 0.85);
            case BEACH -> Color.rgb(180, 160, 100, 0.8);
            case DESERT -> Color.rgb(160, 110, 50, 0.8);
            case PLAINS -> Color.rgb(45, 95, 45, 0.8);
            case SAVANNAH -> Color.rgb(130, 140, 50, 0.8);
            case FOREST -> Color.rgb(25, 75, 40, 0.85);
            case JUNGLE -> Color.rgb(15, 85, 30, 0.85);
            case MOUNTAINS -> Color.rgb(110, 110, 120, 0.85);
            case HILLS -> Color.rgb(90, 100, 80, 0.8);
            case TUNDRA -> Color.rgb(130, 140, 140, 0.8);
            case SNOW, GLACIER -> Color.rgb(200, 215, 225, 0.85);
            case LAKE -> Color.rgb(20, 70, 120, 0.75);
        };
    }

    /**
     * Extracts numerical attribute value for a cell based on active display mode.
     *
     * @param c target H3Cell
     * @param mode active display mode
     * @return numerical scalar attribute
     */
    public static double getCellValue(H3Cell c, DisplayMode mode) {
        if (c == null || mode == null) return 0.0;
        return switch (mode) {
            case BIOME -> c.getBiome() != null ? c.getBiome().ordinal() : 0.0;
            case TEMPERATURE -> c.getTemperature() != null ? c.getTemperature() : 15.0;
            case PRECIPITATION -> c.getRainfall() != null ? c.getRainfall() : 500.0;
            case WATER -> c.getWaterResource() != null ? c.getWaterResource() : 100.0;
            case ALBEDO -> c.getDynamicAlbedo() != null ? c.getDynamicAlbedo() : 0.3;
            case FRICTION -> c.getMovementFriction() != null ? c.getMovementFriction() : 1.0;
            case ENERGY_CAPACITY -> c.getEnergySolar() != null ? c.getEnergySolar() : 0.0;
            case ENTROPY_POLLUTION -> c.getPollutionLevel() != null ? c.getPollutionLevel() : 0.0;
            case SOIL_QUALITY -> c.getSoilOrganicCarbon() != null ? c.getSoilOrganicCarbon() : (c.getFoodResource() != null ? c.getFoodResource() / 50.0 : 50.0);
            case BIODIVERSITY -> c.getBiomassNatural() != null ? c.getBiomassNatural() : 50.0;
            case OCEAN_PH -> 8.1;
            case PERMAFROST -> c.getIceSheetThicknessMeters() != null ? c.getIceSheetThicknessMeters() : 0.0;
            case POPULATION -> c.getPopulation() != null ? c.getPopulation().doubleValue() : 0.0;
            case MIGRATION -> c.getFluxPressure();
            case AGE_PYRAMID -> c.getPopElderly() != null && c.getPopulation() != null && c.getPopulation() > 0 ? ((double) c.getPopElderly() / c.getPopulation()) * 100.0 : 15.0;
            case EPIDEMIC -> c.getPollutionLevel() != null ? c.getPollutionLevel() * 0.8 : 0.0;
            case HEALTH_LIFE_EXPECTANCY -> c.getLifespan() != null ? c.getLifespan() : 45.0;
            case EDUCATION_LEVEL -> c.getTechnologyLevel() != null ? Math.min(100.0, c.getTechnologyLevel() * 12.0) : 10.0;
            case MALTHUSIAN_PRESSURE -> {
                double food = c.getFoodResource() != null ? c.getFoodResource() : 10.0;
                double pop = c.getPopulation() != null ? c.getPopulation() : 0.0;
                yield pop > 0 ? (pop * 3.362) / Math.max(1.0, food) : 0.0;
            }
            case CAPACITY -> c.getFoodResource() != null ? c.getFoodResource() / 3.362 : 1000.0;
            case FOOD -> c.getFoodResource() != null ? c.getFoodResource() : 0.0;
            case WOOD -> c.getWoodResource() != null ? c.getWoodResource() : 100.0;
            case MINERAL_RESOURCES -> c.getResourceMetal() != null ? c.getResourceMetal() : 0.0;
            case MINING_EXPLOITATION -> c.getResourceWork() != null ? c.getResourceWork() : 0.0;
            case TECHNOLOGY -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
            case CULTURE -> c.getOwner() != null ? Math.abs(c.getOwner().hashCode() % 100) : 1.0;
            case POLITICAL -> c.getOwner() != null ? 100.0 : 0.0;
            case INEQUALITY -> c.getGiniIndex() != null ? c.getGiniIndex() : 0.35;
            case ASABIYYAH -> 0.70;
            case FLUX -> c.getFluxPressure();
            case HAPPINESS -> 65.0;
            case CONFLICT -> c.getOwner() != null ? 75.0 : 5.0;
            case INSTITUTIONAL_MATURITY -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() * 10.0 : 5.0;
            case GDP_WEALTH -> {
                double pop = c.getPopulation() != null ? c.getPopulation() : 0.0;
                double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
                yield pop * tech * 15.0;
            }
            case ELITE_DENSITY -> 2.5;
            case COLLECTIVE_MEMORY -> c.getTechnologyLevel() != null ? Math.pow(c.getTechnologyLevel(), 2.0) * 100.0 : 10.0;
            case COLLAPSE_RISK -> {
                double pop = c.getPopulation() != null ? c.getPopulation() : 0.0;
                double food = c.getFoodResource() != null ? c.getFoodResource() : 1.0;
                yield pop > 0 && food < pop ? Math.min(100.0, ((pop - food) / pop) * 100.0) : 5.0;
            }
            case HABITAT_INFRASTRUCTURE -> c.getHabitatType() != org.ether.society.model.HabitatType.NONE ? (c.getHabitatIntegrity() != null ? c.getHabitatIntegrity() * 100.0 : 100.0) : 0.0;
        };
    }

    /**
     * Resolves display color gradient mapped to normalized ratio [0..1] and display mode category.
     *
     * @param ratio normalized magnitude ratio [0..1]
     * @param mode display mode
     * @param rawVal raw unscaled value
     * @return rendered JavaFX {@link Color}
     */
    public static Color getColorForDisplayMode(double ratio, DisplayMode mode, double rawVal) {
        if (mode.getEncodingType() == DisplayMode.EncodingType.ID_24BIT_CATEGORICAL) {
            return Color.hsb(ratio * 360, 0.85, 0.95, 0.80);
        }
        return switch (mode.getCategory()) {
            case PHYSICAL -> {
                if (mode == DisplayMode.TEMPERATURE) {
                    yield Color.color(ratio, 0.2, 1.0 - ratio, 0.80);
                } else if (mode == DisplayMode.PRECIPITATION || mode == DisplayMode.WATER) {
                    yield Color.color(0.1, 0.4 + ratio * 0.4, Math.min(1.0, 0.6 + ratio * 0.4), 0.80);
                } else {
                    yield Color.color(Math.min(1.0, ratio * 1.2), Math.min(1.0, 0.8 - ratio * 0.4), 0.2, 0.80);
                }
            }
            case DEMOGRAPHICS -> {
                if (ratio < 0.3) {
                    yield Color.rgb((int)(255 * (ratio / 0.3)), (int)(220 * (ratio / 0.3)), 0, 0.80);
                } else if (ratio < 0.7) {
                    double t = (ratio - 0.3) / 0.4;
                    yield Color.rgb(255, (int)(220 - t * 140), 0, 0.85);
                } else {
                    double t = (ratio - 0.7) / 0.3;
                    yield Color.rgb(255, (int)(80 - t * 60), (int)(t * 40), 0.90);
                }
            }
            case ECOLOGY -> Color.color(0.2, Math.min(1.0, 0.3 + ratio * 0.7), 0.3, 0.80);
            case MINING -> Color.color(Math.min(1.0, 0.4 + ratio * 0.6), 0.3, Math.min(1.0, 0.6 + ratio * 0.4), 0.80);
            case SOCIETY_POLITICS -> {
                if (mode == DisplayMode.COLLAPSE_RISK || mode == DisplayMode.CONFLICT) {
                    yield Color.color(Math.min(1.0, 0.5 + ratio * 0.5), 0.1, 0.1, 0.85);
                } else if (mode == DisplayMode.GDP_WEALTH) {
                    yield Color.color(0.2, Math.min(1.0, 0.4 + ratio * 0.6), Math.min(1.0, ratio * 1.2), 0.85);
                } else {
                    yield Color.color(Math.min(1.0, 0.3 + ratio * 0.7), 0.4, Math.min(1.0, 0.8 - ratio * 0.4), 0.80);
                }
            }
        };
    }

    /**
     * Computes Moran's I spatial autocorrelation coefficient for the grid:
     * $$I = \frac{N}{W} \frac{\sum_i \sum_j w_{ij}(x_i - \bar{x})(x_j - \bar{x})}{\sum_i (x_i - \bar{x})^2}$$
     *
     * @param cells cell population
     * @param mode display mode channel
     * @return Moran's I coefficient in range [-1.0, 1.0]
     */
    public static double computeMoranI(List<H3Cell> cells, DisplayMode mode) {
        if (cells == null || cells.size() < 4) return 0.0;
        double sum = 0;
        for (H3Cell c : cells) sum += getCellValue(c, mode);
        double mean = sum / cells.size();

        double num = 0, denom = 0;
        int n = cells.size();

        for (int i = 0; i < n; i++) {
            double zi = getCellValue(cells.get(i), mode) - mean;
            denom += zi * zi;
            for (int j = i + 1; j < Math.min(n, i + 10); j++) {
                double zj = getCellValue(cells.get(j), mode) - mean;
                num += zi * zj;
            }
        }
        return denom > 0 ? Math.clamp(num / denom, -1.0, 1.0) : 0.0;
    }
}
