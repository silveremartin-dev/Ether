/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Headless {@link BufferedImage} cell mapper, ESRI World File (`.tfw`) exporter, Base64 PNG codec,
 * and raster integrity validator supporting {@link ImageMapLoader}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see ImageMapLoader
 */
final class ImageMapCodecAndBufferedMapper {

    private static final Logger logger = LoggerFactory.getLogger(ImageMapCodecAndBufferedMapper.class);

    private static final int MAX_BASE64_CACHE_SIZE = 120;
    private static final Map<String, Image> base64ImageCache = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                    return size() > MAX_BASE64_CACHE_SIZE;
                }
            }
    );

    private ImageMapCodecAndBufferedMapper() {
        // Static helper class
    }

    /**
     * Maps headless {@link BufferedImage} rasters to simulation cells without requiring JavaFX runtime or Color instances.
     *
     * @param cells the target H3 hex cells to populate
     * @param elevImg the elevation raster
     * @param biomeImg the categorical biome raster
     * @param resourceImg the geology &amp; resource raster
     * @param climateImg the mean annual temperature raster
     * @param rainfallImg the precipitation raster
     * @param seasonalityImg the seasonal thermal amplitude raster
     * @param minAlt the minimum elevation baseline in meters
     * @param maxAlt the maximum elevation ceiling in meters
     */
    static void mapBufferedImagesToCells(List<H3Cell> cells, BufferedImage elevImg, BufferedImage biomeImg,
                                         BufferedImage resourceImg, BufferedImage climateImg,
                                         BufferedImage rainfallImg, BufferedImage seasonalityImg,
                                         double minAlt, double maxAlt) {
        if (elevImg == null && biomeImg == null && resourceImg == null
                && climateImg == null && rainfallImg == null && seasonalityImg == null) {
            logger.warn("No buffered map images provided for cell mapping");
            return;
        }

        int wElev = elevImg != null ? elevImg.getWidth() : 0;
        int hElev = elevImg != null ? elevImg.getHeight() : 0;
        int wBiome = biomeImg != null ? biomeImg.getWidth() : 0;
        int hBiome = biomeImg != null ? biomeImg.getHeight() : 0;
        int wRes = resourceImg != null ? resourceImg.getWidth() : 0;
        int hRes = resourceImg != null ? resourceImg.getHeight() : 0;
        int wClimate = climateImg != null ? climateImg.getWidth() : 0;
        int hClimate = climateImg != null ? climateImg.getHeight() : 0;
        int wRain = rainfallImg != null ? rainfallImg.getWidth() : 0;
        int hRain = rainfallImg != null ? rainfallImg.getHeight() : 0;
        int wSeason = seasonalityImg != null ? seasonalityImg.getWidth() : 0;
        int hSeason = seasonalityImg != null ? seasonalityImg.getHeight() : 0;

        for (H3Cell cell : cells) {
            double lat = cell.getLatitude();
            double lng = cell.getLongitude();

            // Map Lat/Lng to UV coordinates (0..1)
            double u = Math.clamp((lng + 180.0) / 360.0, 0.0, 1.0);
            double v = Math.clamp((90.0 - lat) / 180.0, 0.0, 1.0);

            // 1. Sample Elevation
            if (elevImg != null) {
                int x = (int) Math.min(u * wElev, wElev - 1);
                int y = (int) Math.min(v * hElev, hElev - 1);
                int argb = elevImg.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                double brightness = Math.max(r, Math.max(g, b)) / 255.0;
                double elevation = org.ether.society.generation.PlanetPreset.waterLevelToMeters(brightness, minAlt, maxAlt);
                cell.setElevation(elevation);
            }

            // 2. Sample Biome
            if (biomeImg != null) {
                int x = (int) Math.min(u * wBiome, wBiome - 1);
                int y = (int) Math.min(v * hBiome, hBiome - 1);
                cell.setBiome(ImageMapLoader.matchBiomeArgb(biomeImg.getRGB(x, y)));
            } else if (elevImg != null) {
                double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
                if (elev < 0) cell.setBiome(Biome.OCEAN);
                else if (elev > 0.85 * maxAlt) cell.setBiome(Biome.SNOW);
                else if (elev > 0.65 * maxAlt) cell.setBiome(Biome.MOUNTAINS);
                else if (elev > 0.35 * maxAlt) cell.setBiome(Biome.HILLS);
                else cell.setBiome(Biome.PLAINS);
            }

            // 3. Sample Geology & Resources
            if (resourceImg != null) {
                int x = (int) Math.min(u * wRes, wRes - 1);
                int y = (int) Math.min(v * hRes, hRes - 1);
                int argb = resourceImg.getRGB(x, y);
                double rNorm = ((argb >> 16) & 0xFF) / 255.0;
                double gNorm = ((argb >> 8) & 0xFF) / 255.0;
                double bNorm = (argb & 0xFF) / 255.0;

                cell.setResourceMetal(rNorm * 1000.0);
                cell.setResourceClay((rNorm * 0.5 + gNorm * 0.5) * 500.0);
                cell.setWoodResource(gNorm * 1000.0);
                cell.setBiomassNatural(gNorm * 1000.0);
                cell.setFoodResource(gNorm * 500.0);
                cell.setWaterResource(bNorm * 1000.0);
                cell.setBiomassFish(bNorm * 1000.0);
            }

            // 4. Sample Climate / Temperature Map
            if (climateImg != null) {
                int x = (int) Math.min(u * wClimate, wClimate - 1);
                int y = (int) Math.min(v * hClimate, hClimate - 1);
                int argb = climateImg.getRGB(x, y);
                double rNorm = ((argb >> 16) & 0xFF) / 255.0;
                cell.setTemperature(-50.0 + rNorm * 100.0);
                if (rainfallImg == null) {
                    double gNorm = ((argb >> 8) & 0xFF) / 255.0;
                    cell.setRainfall(gNorm * 3000.0);
                }
            }

            // 5. Sample Separate Rainfall / Moisture Map
            if (rainfallImg != null) {
                int x = (int) Math.min(u * wRain, wRain - 1);
                int y = (int) Math.min(v * hRain, hRain - 1);
                int argb = rainfallImg.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                cell.setRainfall((Math.max(r, Math.max(g, b)) / 255.0) * 3000.0);
            }

            // 6. Sample Separate Seasonality / Temp Amplitude Map
            if (seasonalityImg != null) {
                int x = (int) Math.min(u * wSeason, wSeason - 1);
                int y = (int) Math.min(v * hSeason, hSeason - 1);
                int argb = seasonalityImg.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                double seasonalDelta = (Math.max(r, Math.max(g, b)) / 255.0) * 50.0;
                cell.setTemperature(cell.getTemperature() + (Math.sin(Math.toRadians(lat)) * seasonalDelta * 0.5));
            }
        }

        logger.info("Mapped data from BufferedImage maps to {} cells", cells.size());
    }

    /**
     * Exports a JavaFX {@link Image} map to a PNG file along with an ESRI World File (`.tfw`).
     *
     * @param image the JavaFX Image
     * @param targetPngFile destination file
     */
    static void exportJavaFxMapToPngAndWorldFile(Image image, File targetPngFile) {
        if (image == null || targetPngFile == null) return;
        try {
            int width = (int) image.getWidth();
            int height = (int) image.getHeight();
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            PixelReader reader = image.getPixelReader();
            if (reader != null) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        bufferedImage.setRGB(x, y, reader.getArgb(x, y));
                    }
                }
            }
            exportBufferedMapToPngAndWorldFile(bufferedImage, targetPngFile);
        } catch (Exception e) {
            logger.error("Failed to export map image and world file", e);
        }
    }

    /**
     * Exports a headless {@link BufferedImage} to PNG along with a standard ESRI World File (`.tfw`).
     *
     * @param bufferedImage the buffered image
     * @param targetPngFile destination file
     */
    static void exportBufferedMapToPngAndWorldFile(BufferedImage bufferedImage, File targetPngFile) {
        if (bufferedImage == null || targetPngFile == null) return;
        try {
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            ImageIO.write(bufferedImage, "png", targetPngFile);

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
                pw.printf(Locale.US, "%.8f%n", dx);
                pw.println("0.0");
                pw.println("0.0");
                pw.printf(Locale.US, "%.8f%n", dy);
                pw.printf(Locale.US, "%.8f%n", x0);
                pw.printf(Locale.US, "%.8f%n", y0);
            }

            logger.info("Exported buffered map image to {} and World File to {}",
                    targetPngFile.getAbsolutePath(), worldFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to export buffered map image and world file", e);
        }
    }

    /**
     * Converts a JavaFX {@link Image} to a compressed Base64 PNG string for JSON preset persistence.
     *
     * @param image the JavaFX Image
     * @return Base64 encoded PNG string, or {@code null} on error
     */
    static String imageToBase64Png(Image image) {
        if (image == null) return null;
        try {
            int width = (int) image.getWidth();
            int height = (int) image.getHeight();
            if (width <= 0 || height <= 0) return null;
            BufferedImage bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            PixelReader reader = image.getPixelReader();
            if (reader != null) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        bImage.setRGB(x, y, reader.getArgb(x, y));
                    }
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bImage, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            logger.error("Failed to convert image to Base64 PNG", e);
            return null;
        }
    }

    /**
     * Converts a Base64 PNG string back into a JavaFX {@link Image} with bounded LRU caching.
     *
     * @param base64 the Base64 PNG string
     * @return the reconstructed JavaFX Image, or {@code null}
     */
    static Image base64PngToImage(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        Image cached = base64ImageCache.get(base64);
        if (cached != null) {
            return cached;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64.trim());
            Image img = new Image(new ByteArrayInputStream(bytes));
            base64ImageCache.put(base64, img);
            return img;
        } catch (Exception e) {
            logger.error("Failed to convert Base64 PNG to image", e);
            return null;
        }
    }

    /**
     * Validates raster dimensions, {@link PixelReader} availability, and corner/center pixel readability.
     *
     * @param image the JavaFX Image to validate
     * @return structured {@link ImageMapLoader.ImageValidationResult}
     */
    static ImageMapLoader.ImageValidationResult validateMapImage(Image image) {
        if (image == null) {
            return new ImageMapLoader.ImageValidationResult(false, "Image non fournie (null)", 0, 0, 0.0);
        }
        if (image.isError()) {
            String err = image.getException() != null ? image.getException().getMessage() : "Format ou fichier illisible";
            return new ImageMapLoader.ImageValidationResult(false, "Fichier corrompu ou illisible: " + err, 0, 0, 0.0);
        }
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        if (w <= 0 || h <= 0) {
            return new ImageMapLoader.ImageValidationResult(false, "Dimensions d'image invalides (0x0)", w, h, 0.0);
        }
        PixelReader reader = image.getPixelReader();
        if (reader == null) {
            return new ImageMapLoader.ImageValidationResult(false, "Impossible de lire la matrice de pixels (PixelReader null)", w, h, 0.0);
        }
        try {
            reader.getColor(0, 0);
            reader.getColor(w / 2, h / 2);
            reader.getColor(w - 1, h - 1);
        } catch (Exception ex) {
            return new ImageMapLoader.ImageValidationResult(false, "Échec de lecture des pixels raster: " + ex.getMessage(), w, h, 0.0);
        }
        double ratio = (double) w / (double) h;
        return new ImageMapLoader.ImageValidationResult(true, String.format("Valide (%dx%d, ratio %.2f)", w, h, ratio), w, h, ratio);
    }
}
