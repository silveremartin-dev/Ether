/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.telemetry;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.ui.analytics.SpatialTensorRasterRenderer;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * <h1>Telemetry Spatial Query Evaluator</h1>
 *
 * <p>Executes 2D cellular and buffer spatial queries, calculating zonal statistics
 * (min, max, mean, sum) and rendering projected equirectangular cartographic rasters.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see CompiledTelemetryPlan
 * @see TelemetrySubscription.SpatialRasterQuery
 */
final class TelemetrySpatialQueryHelper {

    private TelemetrySpatialQueryHelper() {
        // Pure stateless utility
    }

    /**
     * Evaluates a spatial raster slice and/or zonal statistical aggregation.
     *
     * @param q     spatial query specification
     * @param world world buffer SoA
     * @param cells discrete H3 cells (optional, used when rendering or window filtering)
     * @return evaluated raster result
     */
    static TelemetryEvaluationResult.SpatialRasterResult evaluate(
            TelemetrySubscription.SpatialRasterQuery q,
            WorldBuffer world,
            List<H3Cell> cells) {
        double sum = 0.0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        int matched = 0;

        if (cells != null && !cells.isEmpty()) {
            boolean windowed = q.isWindowed();
            for (int i = 0; i < cells.size(); i++) {
                H3Cell cell = cells.get(i);
                if (windowed) {
                    double lat = cell.getLatitude();
                    double lon = cell.getLongitude();
                    if (lat < q.minLat() || lat > q.maxLat() || lon < q.minLng() || lon > q.maxLng()) {
                        continue;
                    }
                }
                double val = extractSpatialCellValue(q.channel(), cell, world, i);
                if (Double.isNaN(val)) continue;
                sum += val;
                if (val < min) min = val;
                if (val > max) max = val;
                matched++;
            }
        } else if (world != null) {
            int n = world.getCapacity();
            for (int i = 0; i < n; i++) {
                double val = extractSpatialBufferValue(q.channel(), world, i);
                if (Double.isNaN(val)) continue;
                sum += val;
                if (val < min) min = val;
                if (val > max) max = val;
                matched++;
            }
        }

        if (matched == 0) {
            min = 0.0;
            max = 0.0;
        }
        double mean = matched > 0 ? sum / matched : 0.0;
        BufferedImage img = null;
        if (q.renderImage() && cells != null && !cells.isEmpty()) {
            img = SpatialTensorRasterRenderer.rasterizeCellsToImage(
                    cells, q.channel().getRendererChannelKey(), q.width(), q.height());
        }
        return new TelemetryEvaluationResult.SpatialRasterResult(
                q.queryId(), q.channel(), sum, mean, min, max, matched, img);
    }

    private static double extractSpatialCellValue(
            TelemetrySubscription.SpatialChannel ch, H3Cell cell, WorldBuffer wb, int idx) {
        if (wb != null && idx >= 0 && idx < wb.getCapacity()) {
            return extractSpatialBufferValue(ch, wb, idx);
        }
        return switch (ch) {
            case POPULATION_DENSITY -> cell.getBiomassHuman() != null ? cell.getBiomassHuman() : cell.getPopulationOr(0);
            case BUILT_CAPITAL -> cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            case TEMPERATURE -> cell.getTemperature() != null ? cell.getTemperature() : 14.5;
            case BIOME -> cell.getBiome() != null ? cell.getBiome().ordinal() : 0.0;
            case ECOLOGICAL_FOOTPRINT -> {
                double pop = cell.getPopulationOr(0);
                double bioNat = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 0.0;
                double bioAgr = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0;
                yield (bioAgr + pop * 0.05) / Math.max(1.0, bioNat + bioAgr + pop * 0.05);
            }
            case FOOD_RESERVES -> cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
            case WATER_RESERVES -> cell.getWaterResource() != null ? cell.getWaterResource() : 0.0;
            case NATURAL_BIOMASS -> cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 0.0;
            case AGRICULTURAL_BIOMASS -> cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0;
        };
    }

    private static double extractSpatialBufferValue(
            TelemetrySubscription.SpatialChannel ch, WorldBuffer wb, int i) {
        return switch (ch) {
            case POPULATION_DENSITY -> wb.getBiomassHuman()[i];
            case BUILT_CAPITAL -> wb.getResourceCapital()[i];
            case TEMPERATURE -> wb.getTemperature()[i];
            case BIOME -> wb.getBiomes()[i];
            case ECOLOGICAL_FOOTPRINT -> {
                double pop = wb.getBiomassHuman()[i];
                double nat = wb.getBiomassNatural()[i];
                double agr = wb.getBiomassAgriculture()[i];
                yield Math.clamp((agr + pop * 0.05) / Math.max(1.0, nat + agr + pop * 0.05), 0.0, 1.0);
            }
            case FOOD_RESERVES -> wb.getFoodResource()[i];
            case WATER_RESERVES -> wb.getWaterResource()[i];
            case NATURAL_BIOMASS -> wb.getBiomassNatural()[i];
            case AGRICULTURAL_BIOMASS -> wb.getBiomassAgriculture()[i];
        };
    }
}
