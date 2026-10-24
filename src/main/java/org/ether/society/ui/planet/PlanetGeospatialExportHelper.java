/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.planet;

import javafx.scene.paint.Color;
import javafx.stage.Window;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.export.GeospatialExportUIHelper;

import java.util.function.Function;

/**
 * Utility helper for exporting planetary digital elevation models (DEM) and macroclimate rasters
 * to geospatial GIS formats (GeoTIFF, 16-bit PNG, ESRI ASCII Grid, Polychrome PNG with WorldFiles).
 *
 * <p>Extracted from {@code PlanetGeneratorPanel} to satisfy the Single Responsibility Principle (SRP).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class PlanetGeospatialExportHelper {

    private PlanetGeospatialExportHelper() {
        // Utility class
    }

    /**
     * Exports the procedural heightmap digital elevation model.
     *
     * @param owner hosting window for modal file dialogs
     * @param preset active planetary configuration preset
     * @param generator procedural noise generator
     * @param tempSeed thermal simulation seed
     * @param precipSeed hygrometric simulation seed
     * @param seasonSeed seasonality simulation seed
     * @param hypsometricColorMapper color mapping function for elevations
     */
    public static void exportProceduralHeightmap(Window owner,
                                                 PlanetPreset preset,
                                                 ProceduralGenerator generator,
                                                 long tempSeed,
                                                 long precipSeed,
                                                 long seasonSeed,
                                                 Function<Double, Color> hypsometricColorMapper) {
        double circumference = 2 * Math.PI * preset.radiusKm();
        int recW = circumference > 60000 ? 2048 : (circumference > 20000 ? 1024 : 512);
        int recH = recW / 2;

        float[][] grid = new float[recH][recW];
        double seaM = PlanetPreset.waterLevelToMeters(preset.waterLevel(), preset.minAltitudeMeters(), preset.maxAltitudeMeters());

        for (int y = 0; y < recH; y++) {
            double lat = 90.0 - ((double) y / recH) * 180.0;
            for (int x = 0; x < recW; x++) {
                double lon = ((double) x / recW) * 360.0 - 180.0;
                ProceduralGenerator.PlanetPoint pt = generator.getPlanetPoint(lat, lon, preset, tempSeed, precipSeed, seasonSeed);
                grid[y][x] = (float) pt.elevationMeters();
            }
        }

        Function<Float, Color> colorMapper = elevM -> {
            if (elevM < seaM && preset.waterLevel() > -0.4) {
                return Color.rgb(15, 23, 42); // Deep ocean navy
            } else {
                double norm = (elevM - seaM) / (preset.maxAltitudeMeters() - seaM + 0.001);
                return hypsometricColorMapper.apply(norm);
            }
        };

        String defaultName = String.format("ether-heightmap-%s-%dx%d.png",
                preset.name().toLowerCase().replaceAll("[^a-z0-9]", "-"), recW, recH);
        GeospatialExportUIHelper.exportRasterWithDialog(
                owner,
                grid,
                defaultName,
                I18n.getOrDefault("planet.dialog.export_heightmap", "Export Heightmap / Digital Elevation Model (GeoTIFF / PNG / ASCII)"),
                colorMapper,
                (float) preset.minAltitudeMeters(),
                (float) preset.maxAltitudeMeters(),
                "m"
        );
    }

    /**
     * Exports procedural macroclimate maps (Temperature, Precipitation, Seasonality).
     *
     * @param owner hosting window for modal file dialogs
     * @param type climate map type ("temp", "precip", or "season")
     * @param preset active planetary configuration preset
     * @param generator procedural noise generator
     * @param tempSeed thermal simulation seed
     * @param precipSeed hygrometric simulation seed
     * @param seasonSeed seasonality simulation seed
     * @param tempColorMapper color mapping function for temperatures
     * @param precipColorMapper color mapping function for precipitation
     * @param seasonColorMapper color mapping function for seasonality
     */
    public static void exportProceduralClimateMap(Window owner,
                                                  String type,
                                                  PlanetPreset preset,
                                                  ProceduralGenerator generator,
                                                  long tempSeed,
                                                  long precipSeed,
                                                  long seasonSeed,
                                                  Function<Double, Color> tempColorMapper,
                                                  Function<Double, Color> precipColorMapper,
                                                  Function<Double, Color> seasonColorMapper) {
        double circumference = 2 * Math.PI * preset.radiusKm();
        int recW = circumference > 60000 ? 2048 : (circumference > 20000 ? 1024 : 512);
        int recH = recW / 2;

        float[][] grid = new float[recH][recW];
        double co2Forcing = 3.0 * (Math.log(Math.max(1.0, preset.co2Ppm()) / 280.0) / Math.log(2.0));
        double pressureBoost = (preset.atmospherePressureAtm() - 1.0) * 8.0;
        double minTemp = preset.averageTempC() + co2Forcing + pressureBoost - preset.temperatureGradient() - 20.0;
        double maxTemp = preset.averageTempC() + co2Forcing + pressureBoost + preset.temperatureGradient();
        double maxAmp = Math.min(60.0, (preset.axialTiltDegrees() / 23.5) * 35.0);

        Function<Float, Color> colorMapper;
        float minVal, maxVal;
        String unit;

        if ("temp".equalsIgnoreCase(type)) {
            minVal = (float) minTemp;
            maxVal = (float) maxTemp;
            unit = "°C";
            for (int y = 0; y < recH; y++) {
                double lat = 90.0 - ((double) y / recH) * 180.0;
                for (int x = 0; x < recW; x++) {
                    double lon = ((double) x / recW) * 360.0 - 180.0;
                    ProceduralGenerator.PlanetPoint pt = generator.getPlanetPoint(lat, lon, preset, tempSeed, precipSeed, seasonSeed);
                    grid[y][x] = (float) pt.temperature();
                }
            }
            colorMapper = val -> tempColorMapper.apply((double) val);
        } else if ("precip".equalsIgnoreCase(type)) {
            minVal = 0.0f;
            maxVal = 3000.0f;
            unit = "mm/an";
            for (int y = 0; y < recH; y++) {
                double lat = 90.0 - ((double) y / recH) * 180.0;
                for (int x = 0; x < recW; x++) {
                    double lon = ((double) x / recW) * 360.0 - 180.0;
                    ProceduralGenerator.PlanetPoint pt = generator.getPlanetPoint(lat, lon, preset, tempSeed, precipSeed, seasonSeed);
                    grid[y][x] = (float) (pt.rainfall() * 3000.0);
                }
            }
            colorMapper = val -> precipColorMapper.apply(val / 3000.0);
        } else {
            minVal = 0.0f;
            maxVal = (float) maxAmp;
            unit = "°C";
            for (int y = 0; y < recH; y++) {
                double lat = 90.0 - ((double) y / recH) * 180.0;
                for (int x = 0; x < recW; x++) {
                    double lon = ((double) x / recW) * 360.0 - 180.0;
                    ProceduralGenerator.PlanetPoint pt = generator.getPlanetPoint(lat, lon, preset, tempSeed, precipSeed, seasonSeed);
                    grid[y][x] = (float) (pt.seasonality() * maxAmp);
                }
            }
            colorMapper = val -> seasonColorMapper.apply(val / Math.max(0.001, maxAmp));
        }

        String defaultName = String.format("ether-%s-%s-%dx%d.png",
                type, preset.name().toLowerCase().replaceAll("[^a-z0-9]", "-"), recW, recH);
        GeospatialExportUIHelper.exportRasterWithDialog(
                owner,
                grid,
                defaultName,
                I18n.getOrDefault("planet.dialog.export_" + type, "Export Climate Map (GeoTIFF / PNG / ASCII)"),
                colorMapper,
                minVal,
                maxVal,
                unit
        );
    }
}
