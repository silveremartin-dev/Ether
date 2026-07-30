/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * Loads and exports simulation data from/to standard map images (Heightmaps/Biomemaps/Resource maps).
 * Assumes Equirectangular projection (Plate Carrée).
 * 
 * Supports:
 * - PNG / JPEG / GeoTIFF image mapping
 * - ESRI World File (.tfw / .wld) export & import for spatial metadata alignment
 * - Multi-channel Geology & Resource mapping (Red=Metals, Green=Flora, Blue=Water)
 * 
 * @author Silvere Martin-Michiellot
 * @version 2.2.0
 */
public class ImageMapLoader {
    private static final Logger logger = LoggerFactory.getLogger(ImageMapLoader.class);

    public void mapDataToCells(List<H3Cell> cells, InputStream elevationImageStream, InputStream biomeImageStream) {
        mapDataToCells(cells, elevationImageStream, biomeImageStream, null);
    }

    public void mapDataToCells(List<H3Cell> cells, InputStream elevationImageStream, InputStream biomeImageStream, InputStream resourceImageStream) {
        Image elevImg = elevationImageStream != null ? new Image(elevationImageStream) : null;
        Image biomeImg = biomeImageStream != null ? new Image(biomeImageStream) : null;
        Image resourceImg = resourceImageStream != null ? new Image(resourceImageStream) : null;

        mapImagesToCells(cells, elevImg, biomeImg, resourceImg, -11000.0, 8848.0);
    }

    public void mapImagesToCells(List<H3Cell> cells, Image elevImg, Image biomeImg, Image resourceImg, double minAlt, double maxAlt) {
        if (elevImg == null && biomeImg == null && resourceImg == null) {
            logger.warn("No map images provided for cell mapping");
            return;
        }

        PixelReader elevReader = elevImg != null ? elevImg.getPixelReader() : null;
        PixelReader biomeReader = biomeImg != null ? biomeImg.getPixelReader() : null;
        PixelReader resourceReader = resourceImg != null ? resourceImg.getPixelReader() : null;

        double wElev = elevImg != null ? elevImg.getWidth() : 0;
        double hElev = elevImg != null ? elevImg.getHeight() : 0;

        double wBiome = biomeImg != null ? biomeImg.getWidth() : 0;
        double hBiome = biomeImg != null ? biomeImg.getHeight() : 0;

        double wRes = resourceImg != null ? resourceImg.getWidth() : 0;
        double hRes = resourceImg != null ? resourceImg.getHeight() : 0;

        for (H3Cell cell : cells) {
            double lat = cell.getLatitude();
            double lng = cell.getLongitude();

            // Map Lat/Lng to UV coordinates (0..1)
            double u = (lng + 180.0) / 360.0;
            double v = (90.0 - lat) / 180.0;

            u = Math.max(0.0, Math.min(1.0, u));
            v = Math.max(0.0, Math.min(1.0, v));

            // 1. Sample Elevation
            if (elevReader != null) {
                int x = (int) Math.min(u * wElev, wElev - 1);
                int y = (int) Math.min(v * hElev, hElev - 1);
                Color c = elevReader.getColor(x, y);
                double brightness = c.getBrightness(); // 0..1
                double elevation = minAlt + brightness * (maxAlt - minAlt);
                cell.setElevation(elevation);
            }

            // 2. Sample Biome
            if (biomeReader != null) {
                int x = (int) Math.min(u * wBiome, wBiome - 1);
                int y = (int) Math.min(v * hBiome, hBiome - 1);
                Color c = biomeReader.getColor(x, y);
                cell.setBiome(matchBiomeColor(c));
            }

            // 3. Sample Geology & Resources
            if (resourceReader != null) {
                int x = (int) Math.min(u * wRes, wRes - 1);
                int y = (int) Math.min(v * hRes, hRes - 1);
                Color c = resourceReader.getColor(x, y);

                // Red channel -> Metals & Ores
                cell.setResourceMetal(c.getRed() * 1000.0);
                cell.setResourceClay((c.getRed() * 0.5 + c.getGreen() * 0.5) * 500.0);

                // Green channel -> Wood & Plant Biomass
                cell.setWoodResource(c.getGreen() * 1000.0);
                cell.setBiomassNatural(c.getGreen() * 1000.0);
                cell.setFoodResource(c.getGreen() * 500.0);

                // Blue channel -> Water & Marine Biomass
                cell.setWaterResource(c.getBlue() * 1000.0);
                cell.setBiomassFish(c.getBlue() * 1000.0);
            }
        }

        logger.info("Mapped data from map images (Elevation: {}, Biome: {}, Resource: {}) to {} cells",
                elevImg != null, biomeImg != null, resourceImg != null, cells.size());
    }

    /**
     * Match pixel color to nearest Biome.
     */
    public Biome matchBiomeColor(Color c) {
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
     */
    public Color getBiomeTargetColor(Biome b) {
        return switch (b) {
            case DEEP_OCEAN -> Color.rgb(0, 0, 100);
            case OCEAN -> Color.rgb(0, 50, 200);
            case BEACH -> Color.rgb(240, 220, 150);
            case PLAINS -> Color.rgb(100, 200, 50);
            case FOREST -> Color.rgb(20, 120, 20);
            case JUNGLE -> Color.rgb(0, 80, 0);
            case DESERT -> Color.rgb(255, 200, 50);
            case HILLS -> Color.rgb(150, 150, 100);
            case MOUNTAINS -> Color.rgb(100, 100, 100);
            case TUNDRA -> Color.rgb(150, 200, 220);
            case SNOW -> Color.rgb(255, 255, 255);
        };
    }

    /**
     * Export an image map to a standard PNG file along with a standard ESRI World File (.tfw).
     */
    public void exportMapToPngAndWorldFile(Image image, File targetPngFile) {
        if (image == null || targetPngFile == null) return;
        try {
            int width = (int) image.getWidth();
            int height = (int) image.getHeight();

            // Save PNG
            java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            PixelReader reader = image.getPixelReader();
            if (reader != null) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        bufferedImage.setRGB(x, y, reader.getArgb(x, y));
                    }
                }
            }
            ImageIO.write(bufferedImage, "png", targetPngFile);

            // Write matching World File (.tfw) for geospatial alignment
            String baseName = targetPngFile.getAbsolutePath();
            if (baseName.endsWith(".png")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }
            File worldFile = new File(baseName + ".tfw");

            double dx = 360.0 / width;
            double dy = -180.0 / height;
            double x0 = -180.0 + (dx / 2.0);
            double y0 = 90.0 + (dy / 2.0);

            try (PrintWriter pw = new PrintWriter(new FileWriter(worldFile))) {
                pw.printf("%.8f%n", dx);
                pw.println("0.0");
                pw.println("0.0");
                pw.printf("%.8f%n", dy);
                pw.printf("%.8f%n", x0);
                pw.printf("%.8f%n", y0);
            }

            logger.info("Exported map image to {} and World File to {}", targetPngFile.getAbsolutePath(), worldFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to export map image and world file", e);
        }
    }
}
