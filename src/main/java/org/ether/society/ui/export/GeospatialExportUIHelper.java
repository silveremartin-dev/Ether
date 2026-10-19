*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.export;

import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.data.GeospatialRasterExporter;
import org.ether.society.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.function.Function;

/**
 * JavaFX presentation dialog helper for geospatial raster map exporting.
 * Integrates interactive file dialogs with multi-format raster serialization.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class GeospatialExportUIHelper {
    private static final Logger logger = LoggerFactory.getLogger(GeospatialExportUIHelper.class);

    private GeospatialExportUIHelper() {
        // Utility class
    }

    /**
     * Opens an interactive JavaFX file save dialog to export a 2D raster in GIS or image formats.
     *
     * @param parentWindow parent JavaFX window for modal scoping
     * @param grid 2D floating-point matrix
     * @param defaultBaseName default output file name
     * @param dialogTitle custom dialog title
     * @param colorMapper color mapping function
     * @param minVal minimum scalar value
     * @param maxVal maximum scalar value
     * @param physicalUnit physical unit label
     */
    public static void exportRasterWithDialog(Window parentWindow,
                                             float[][] grid,
                                             String defaultBaseName,
                                             String dialogTitle,
                                             Function<Float, Color> colorMapper,
                                             float minVal,
                                             float maxVal,
                                             String physicalUnit) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) {
            logger.warn("Cannot export empty raster grid.");
            return;
        }

        int height = grid.length;
        int width = grid[0].length;

        FileChooser chooser = new FileChooser();
        chooser.setTitle(dialogTitle != null ? dialogTitle : I18n.getOrDefault("raster.export.title", "Export Geospatial Raster Map"));

        FileChooser.ExtensionFilter filterPngColor = new FileChooser.ExtensionFilter(
                I18n.getOrDefault("raster.filter.png_color", "Universal Color Image (*.png + .tfw)"), "*.png");
        FileChooser.ExtensionFilter filterGeoTiff = new FileChooser.ExtensionFilter(
                I18n.getOrDefault("raster.filter.geotiff", "Scientific GeoTIFF Float32 DEM (*.tif, *.tiff)"), "*.tif", "*.tiff");
        FileChooser.ExtensionFilter filterPng16 = new FileChooser.ExtensionFilter(
                I18n.getOrDefault("raster.filter.png16", "16-bit Grayscale Heightmap (*.png + .tfw)"), "*.png");
        FileChooser.ExtensionFilter filterAscii = new FileChooser.ExtensionFilter(
                I18n.getOrDefault("raster.filter.ascii", "ESRI ASCII Grid (*.asc)"), "*.asc");
        FileChooser.ExtensionFilter filterJpg = new FileChooser.ExtensionFilter(
                I18n.getOrDefault("raster.filter.jpg", "JPEG Image (*.jpg + .jgw)"), "*.jpg", "*.jpeg");

        chooser.getExtensionFilters().addAll(filterPngColor, filterGeoTiff, filterPng16, filterAscii, filterJpg);
        chooser.setInitialFileName(defaultBaseName != null ? defaultBaseName : "raster_map.png");

        File targetFile = chooser.showSaveDialog(parentWindow);
        if (targetFile == null) return;

        FileChooser.ExtensionFilter selectedFilter = chooser.getSelectedExtensionFilter();
        String path = targetFile.getAbsolutePath();
        String lower = path.toLowerCase();

        GeospatialRasterExporter.RasterColorProvider colorProvider = val -> {
            Color c = colorMapper != null ? colorMapper.apply(val) : Color.color(0.5, 0.5, 0.5);
            int a = (int) (c.getOpacity() * 255.0);
            int r = (int) (c.getRed() * 255.0);
            int g = (int) (c.getGreen() * 255.0);
            int b = (int) (c.getBlue() * 255.0);
            return (a << 24) | (r << 16) | (g << 8) | b;
        };

        try {
            if (lower.endsWith(".tif") || lower.endsWith(".tiff") || selectedFilter == filterGeoTiff) {
                if (!lower.endsWith(".tif") && !lower.endsWith(".tiff")) {
                    targetFile = new File(path + ".tif");
                }
                GeospatialRasterExporter.writeFloat32GeoTiff(grid, targetFile, -180.0, 180.0, -90.0, 90.0,
                        "Ether Planetary Simulation Engine - " + (physicalUnit != null ? physicalUnit : "Raw"));
            } else if (lower.endsWith(".asc") || selectedFilter == filterAscii) {
                if (!lower.endsWith(".asc")) {
                    targetFile = new File(path + ".asc");
                }
                double cellSize = 360.0 / width;
                GeospatialRasterExporter.writeEsriAsciiGrid(grid, targetFile, -180.0, -90.0, cellSize, -9999.0f);
            } else if (selectedFilter == filterPng16) {
                if (!lower.endsWith(".png")) {
                    targetFile = new File(path + ".png");
                }
                GeospatialRasterExporter.write16BitPng(grid, minVal, maxVal, targetFile);
            } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || selectedFilter == filterJpg) {
                if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg")) {
                    targetFile = new File(path + ".jpg");
                }
                GeospatialRasterExporter.writeArgbColorJpg(grid, colorProvider, targetFile);
            } else {
                if (!lower.endsWith(".png")) {
                    targetFile = new File(path + ".png");
                }
                GeospatialRasterExporter.writeArgbColorPng(grid, colorProvider, targetFile, true);
            }
            logger.info("Successfully exported raster map ({}x{}) to {}", width, height, targetFile.getAbsolutePath());
        } catch (Exception ex) {
            logger.error("Failed to export raster map to {}", targetFile.getAbsolutePath(), ex);
        }
    }
}
