/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 * Loads simulation data from map images (Heightmaps/Biomemaps).
 * Assumes Equirectangular projection (Plate Carrée).
 * 
 * - Elevation Map: Grayscale (Black=Deep Ocean, White=High Mountain) or
 * specific encoding.
 * - Biome Map: Color coded.
 */
public class ImageMapLoader {
    private static final Logger logger = LoggerFactory.getLogger(ImageMapLoader.class);

    /**
     * Map image data to the given list of H3 cells.
     * 
     * @param cells                List of cells to populate
     * @param elevationImageStream Input stream for elevation image (can be null)
     * @param biomeImageStream     Input stream for biome image (can be null)
     */
    public void mapDataToCells(List<H3Cell> cells, InputStream elevationImageStream, InputStream biomeImageStream) {
        Image elevImg = elevationImageStream != null ? new Image(elevationImageStream) : null;
        Image biomeImg = biomeImageStream != null ? new Image(biomeImageStream) : null;

        if (elevImg == null && biomeImg == null) {
            logger.warn("No images provided for map loading");
            return;
        }

        PixelReader elevReader = elevImg != null ? elevImg.getPixelReader() : null;
        PixelReader biomeReader = biomeImg != null ? biomeImg.getPixelReader() : null;

        double wElev = elevImg != null ? elevImg.getWidth() : 0;
        double hElev = elevImg != null ? elevImg.getHeight() : 0;

        double wBiome = biomeImg != null ? biomeImg.getWidth() : 0;
        double hBiome = biomeImg != null ? biomeImg.getHeight() : 0;

        for (H3Cell cell : cells) {
            double lat = cell.getLatitude();
            double lng = cell.getLongitude();

            // Map Lat/Lng to UV coordinates (0..1)
            // Lng: -180..180 -> 0..1
            double u = (lng + 180.0) / 360.0;
            // Lat: 90..-90 -> 0..1 (Image Y is usually top-down)
            double v = (90.0 - lat) / 180.0;

            // Sample Elevation
            if (elevReader != null) {
                int x = (int) (Math.min(u * wElev, wElev - 1));
                int y = (int) (Math.min(v * hElev, hElev - 1));

                Color c = elevReader.getColor(x, y);
                // Assume grayscale brightness = elevation
                // Map 0..1 -> -11000m .. +9000m
                // Ocean level usually at 0.5? Or black=deepest?
                // Standard heightmaps usually scale linearly.
                // Let's assume 0 = -10km, 1 = +10km, 0.5 = Sea Level?
                // Or better: Real ETOPO1 grayscale usually maps black to lowest point.
                double brightness = c.getBrightness();
                double elevation = (brightness * 20000.0) - 10000.0; // Range -10km to +10km
                cell.setElevation(elevation); // Range -10km to +10km
            }

            // Sample Biome
            if (biomeReader != null) {
                int x = (int) (Math.min(u * wBiome, wBiome - 1));
                int y = (int) (Math.min(v * hBiome, hBiome - 1));
                Color c = biomeReader.getColor(x, y);
                cell.setBiome(matchBiomeColor(c));
            }
        }

        logger.info("Mapped data from images to {} cells", cells.size());
    }

    /**
     * Match pixel color to nearest Biome.
     */
    private Biome matchBiomeColor(Color c) {
        // Simple Euclidean distance in RGB
        Biome best = Biome.OCEAN;
        double minDist = Double.MAX_VALUE;

        for (Biome b : Biome.values()) {
            Color bc = getBiomeTargetColor(b);
            double dist = colorDistance(c, bc);
            if (dist < minDist) {
                minDist = dist;
                best = b;
            }
        }
        return best;
    }

    private double colorDistance(Color c1, Color c2) {
        return Math.pow(c1.getRed() - c2.getRed(), 2) +
                Math.pow(c1.getGreen() - c2.getGreen(), 2) +
                Math.pow(c1.getBlue() - c2.getBlue(), 2);
    }

    /**
     * Define expected colors for biomes in the input map.
     * Users should use these colors when painting biome maps.
     */
    private Color getBiomeTargetColor(Biome b) {
        return switch (b) {
            case DEEP_OCEAN -> Color.rgb(0, 0, 100); // Dark Blue
            case OCEAN -> Color.rgb(0, 50, 200); // Blue
            case BEACH -> Color.rgb(240, 220, 150); // Sand
            case PLAINS -> Color.rgb(100, 200, 50); // Light Green
            case FOREST -> Color.rgb(20, 120, 20); // Green
            case JUNGLE -> Color.rgb(0, 80, 0); // Dark Green
            case DESERT -> Color.rgb(255, 200, 50); // Yellow/Orange
            case HILLS -> Color.rgb(150, 150, 100); // Gray-Green
            case MOUNTAINS -> Color.rgb(100, 100, 100); // Gray
            case TUNDRA -> Color.rgb(150, 200, 220); // Cyan-Gray
            case SNOW -> Color.rgb(255, 255, 255); // White
        };
    }
}
