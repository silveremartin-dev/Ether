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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * Loads and exports simulation data from/to standard map images (Heightmaps/Biomemaps/Resource maps).
 * Assumes Equirectangular projection (Plate Carrée).
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>Direct headless {@link BufferedImage} mapping for high-throughput batch and CLI runs</li>
 *   <li>Optimized JavaFX {@link Image} mapping with zero-allocation integer ARGB sampling</li>
 *   <li>PNG / JPEG / GeoTIFF image mapping</li>
 *   <li>ESRI World File (.tfw / .wld) export &amp; import for spatial metadata alignment</li>
 *   <li>Multi-channel Geology &amp; Resource mapping (Red=Metals, Green=Flora, Blue=Water)</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see ImageMapCodecAndBufferedMapper
 */
public class ImageMapLoader {
    private static final Logger logger = LoggerFactory.getLogger(ImageMapLoader.class);

    /**
     * Map data to cells using input streams.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param elevationImageStream the elevation image stream parameter (InputStream)
     * @param biomeImageStream the biome image stream parameter (InputStream)
     */
    public void mapDataToCells(List<H3Cell> cells, InputStream elevationImageStream, InputStream biomeImageStream) {
        mapDataToCells(cells, elevationImageStream, biomeImageStream, null);
    }

    /**
     * Map data to cells using input streams.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param elevationImageStream the elevation image stream parameter (InputStream)
     * @param biomeImageStream the biome image stream parameter (InputStream)
     * @param resourceImageStream the resource image stream parameter (InputStream)
     */
    public void mapDataToCells(List<H3Cell> cells, InputStream elevationImageStream, InputStream biomeImageStream, InputStream resourceImageStream) {
        Image elevImg = elevationImageStream != null ? new Image(elevationImageStream) : null;
        Image biomeImg = biomeImageStream != null ? new Image(biomeImageStream) : null;
        Image resourceImg = resourceImageStream != null ? new Image(resourceImageStream) : null;

        mapImagesToCells(cells, elevImg, biomeImg, resourceImg, -11000.0, 8848.0);
    }

    /**
     * Maps headless data streams directly to cells using {@link BufferedImage} and ImageIO.
     *
     * @param cells the simulation cells
     * @param elevationStream input stream of elevation map
     * @param biomeStream input stream of biome map
     * @param resourceStream input stream of resource map
     */
    public void mapBufferedDataToCells(List<H3Cell> cells, InputStream elevationStream, InputStream biomeStream, InputStream resourceStream) {
        try {
            BufferedImage elevImg = elevationStream != null ? ImageIO.read(elevationStream) : null;
            BufferedImage biomeImg = biomeStream != null ? ImageIO.read(biomeStream) : null;
            BufferedImage resourceImg = resourceStream != null ? ImageIO.read(resourceStream) : null;
            mapBufferedImagesToCells(cells, elevImg, biomeImg, resourceImg, null, null, null, -11000.0, 8848.0);
        } catch (Exception e) {
            logger.error("Failed to map buffered data streams to cells", e);
        }
    }

    /**
     * Map images to cells.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param elevImg the elev img parameter (Image)
     * @param biomeImg the biome img parameter (Image)
     * @param resourceImg the resource img parameter (Image)
     * @param minAlt the min alt parameter (double)
     * @param maxAlt the max alt parameter (double)
     */
    public void mapImagesToCells(List<H3Cell> cells, Image elevImg, Image biomeImg, Image resourceImg, double minAlt, double maxAlt) {
        mapImagesToCells(cells, elevImg, biomeImg, resourceImg, null, null, null, minAlt, maxAlt);
    }

    /**
     * Map images to cells.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param elevImg the elev img parameter (Image)
     * @param biomeImg the biome img parameter (Image)
     * @param resourceImg the resource img parameter (Image)
     * @param climateImg the climate img parameter (Image)
     * @param minAlt the min alt parameter (double)
     * @param maxAlt the max alt parameter (double)
     */
    public void mapImagesToCells(List<H3Cell> cells, Image elevImg, Image biomeImg, Image resourceImg, Image climateImg, double minAlt, double maxAlt) {
        mapImagesToCells(cells, elevImg, biomeImg, resourceImg, climateImg, null, null, minAlt, maxAlt);
    }

    /**
     * Map images to cells.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param elevImg the elev img parameter (Image)
     * @param biomeImg the biome img parameter (Image)
     * @param resourceImg the resource img parameter (Image)
     * @param climateImg the climate img parameter (Image)
     * @param rainfallImg the rainfall img parameter (Image)
     * @param minAlt the min alt parameter (double)
     * @param maxAlt the max alt parameter (double)
     */
    public void mapImagesToCells(List<H3Cell> cells, Image elevImg, Image biomeImg, Image resourceImg, Image climateImg, Image rainfallImg, double minAlt, double maxAlt) {
        mapImagesToCells(cells, elevImg, biomeImg, resourceImg, climateImg, rainfallImg, null, minAlt, maxAlt);
    }

    /**
     * Map images to cells with optimized integer ARGB sampling to prevent heap allocation.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param elevImg the elev img parameter (Image)
     * @param biomeImg the biome img parameter (Image)
     * @param resourceImg the resource img parameter (Image)
     * @param climateImg the climate img parameter (Image)
     * @param rainfallImg the rainfall img parameter (Image)
     * @param seasonalityImg the seasonality img parameter (Image)
     * @param minAlt the min alt parameter (double)
     * @param maxAlt the max alt parameter (double)
     */
    public void mapImagesToCells(List<H3Cell> cells, Image elevImg, Image biomeImg, Image resourceImg, Image climateImg, Image rainfallImg, Image seasonalityImg, double minAlt, double maxAlt) {
        if (elevImg == null && biomeImg == null && resourceImg == null && climateImg == null && rainfallImg == null && seasonalityImg == null) {
            logger.warn("No map images provided for cell mapping");
            return;
        }

        PixelReader elevReader = elevImg != null ? elevImg.getPixelReader() : null;
        PixelReader biomeReader = biomeImg != null ? biomeImg.getPixelReader() : null;
        PixelReader resourceReader = resourceImg != null ? resourceImg.getPixelReader() : null;
        PixelReader climateReader = climateImg != null ? climateImg.getPixelReader() : null;
        PixelReader rainfallReader = rainfallImg != null ? rainfallImg.getPixelReader() : null;
        PixelReader seasonalityReader = seasonalityImg != null ? seasonalityImg.getPixelReader() : null;

        double wElev = elevImg != null ? elevImg.getWidth() : 0;
        double hElev = elevImg != null ? elevImg.getHeight() : 0;
        double wBiome = biomeImg != null ? biomeImg.getWidth() : 0;
        double hBiome = biomeImg != null ? biomeImg.getHeight() : 0;
        double wRes = resourceImg != null ? resourceImg.getWidth() : 0;
        double hRes = resourceImg != null ? resourceImg.getHeight() : 0;
        double wClimate = climateImg != null ? climateImg.getWidth() : 0;
        double hClimate = climateImg != null ? climateImg.getHeight() : 0;
        double wRain = rainfallImg != null ? rainfallImg.getWidth() : 0;
        double hRain = rainfallImg != null ? rainfallImg.getHeight() : 0;
        double wSeason = seasonalityImg != null ? seasonalityImg.getWidth() : 0;
        double hSeason = seasonalityImg != null ? seasonalityImg.getHeight() : 0;

        for (H3Cell cell : cells) {
            double lat = cell.getLatitude();
            double lng = cell.getLongitude();

            double u = Math.clamp((lng + 180.0) / 360.0, 0.0, 1.0);
            double v = Math.clamp((90.0 - lat) / 180.0, 0.0, 1.0);

            if (elevReader != null) {
                int x = (int) Math.min(u * wElev, wElev - 1);
                int y = (int) Math.min(v * hElev, hElev - 1);
                int argb = elevReader.getArgb(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                double brightness = Math.max(r, Math.max(g, b)) / 255.0;
                double elevation = org.ether.society.generation.PlanetPreset.waterLevelToMeters(brightness, minAlt, maxAlt);
                cell.setElevation(elevation);
            }

            if (biomeReader != null) {
                int x = (int) Math.min(u * wBiome, wBiome - 1);
                int y = (int) Math.min(v * hBiome, hBiome - 1);
                cell.setBiome(matchBiomeArgb(biomeReader.getArgb(x, y)));
            } else if (elevReader != null) {
                double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
                if (elev < 0) cell.setBiome(Biome.OCEAN);
                else if (elev > 0.85 * maxAlt) cell.setBiome(Biome.SNOW);
                else if (elev > 0.65 * maxAlt) cell.setBiome(Biome.MOUNTAINS);
                else if (elev > 0.35 * maxAlt) cell.setBiome(Biome.HILLS);
                else cell.setBiome(Biome.PLAINS);
            }

            if (resourceReader != null) {
                int x = (int) Math.min(u * wRes, wRes - 1);
                int y = (int) Math.min(v * hRes, hRes - 1);
                int argb = resourceReader.getArgb(x, y);
                double r = ((argb >> 16) & 0xFF) / 255.0;
                double g = ((argb >> 8) & 0xFF) / 255.0;
                double b = (argb & 0xFF) / 255.0;

                cell.setResourceMetal(r * 1000.0);
                cell.setResourceClay((r * 0.5 + g * 0.5) * 500.0);
                cell.setWoodResource(g * 1000.0);
                cell.setBiomassNatural(g * 1000.0);
                cell.setFoodResource(g * 500.0);
                cell.setWaterResource(b * 1000.0);
                cell.setBiomassFish(b * 1000.0);
            }

            if (climateReader != null) {
                int x = (int) Math.min(u * wClimate, wClimate - 1);
                int y = (int) Math.min(v * hClimate, hClimate - 1);
                int argb = climateReader.getArgb(x, y);
                double r = ((argb >> 16) & 0xFF) / 255.0;
                cell.setTemperature(-50.0 + r * 100.0);
                if (rainfallReader == null) {
                    double g = ((argb >> 8) & 0xFF) / 255.0;
                    cell.setRainfall(g * 3000.0);
                }
            }

            if (rainfallReader != null) {
                int x = (int) Math.min(u * wRain, wRain - 1);
                int y = (int) Math.min(v * hRain, hRain - 1);
                int argb = rainfallReader.getArgb(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                cell.setRainfall((Math.max(r, Math.max(g, b)) / 255.0) * 3000.0);
            }

            if (seasonalityReader != null) {
                int x = (int) Math.min(u * wSeason, wSeason - 1);
                int y = (int) Math.min(v * hSeason, hSeason - 1);
                int argb = seasonalityReader.getArgb(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                double seasonalDelta = (Math.max(r, Math.max(g, b)) / 255.0) * 50.0;
                cell.setTemperature(cell.getTemperature() + (Math.sin(Math.toRadians(lat)) * seasonalDelta * 0.5));
            }
        }

        logger.info("Mapped data from map images (Elevation: {}, Biome: {}, Resource: {}, Climate: {}, Rain: {}, Season: {}) to {} cells",
                elevImg != null, biomeImg != null, resourceImg != null, climateImg != null, rainfallImg != null, seasonalityImg != null, cells.size());
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
    public void mapBufferedImagesToCells(List<H3Cell> cells, BufferedImage elevImg, BufferedImage biomeImg, BufferedImage resourceImg,
                                         BufferedImage climateImg, BufferedImage rainfallImg, BufferedImage seasonalityImg,
                                         double minAlt, double maxAlt) {
        ImageMapCodecAndBufferedMapper.mapBufferedImagesToCells(
                cells, elevImg, biomeImg, resourceImg, climateImg, rainfallImg, seasonalityImg, minAlt, maxAlt);
    }

    private static final Color[] BIOME_TARGET_COLORS = new Color[Biome.VALUES.length];
    private static final int[] BIOME_TARGET_ARGBS = new int[Biome.VALUES.length];

    static {
        for (Biome b : Biome.VALUES) {
            Color c = switch (b) {
                case DEEP_OCEAN -> Color.rgb(0, 0, 40);
                case OCEAN -> Color.rgb(0, 50, 200);
                case BEACH -> Color.rgb(240, 220, 150);
                case PLAINS -> Color.rgb(100, 200, 50);
                case FOREST -> Color.rgb(20, 120, 20);
                case JUNGLE -> Color.rgb(0, 80, 0);
                case DESERT -> Color.rgb(255, 200, 50);
                case HILLS -> Color.rgb(150, 150, 100);
                case MOUNTAINS -> Color.rgb(100, 100, 100);
                case SAVANNAH -> Color.rgb(180, 200, 70);
                case GLACIER -> Color.rgb(220, 240, 255);
                case TUNDRA -> Color.rgb(150, 200, 220);
                case SNOW -> Color.rgb(255, 255, 255);
                case LAKE -> Color.rgb(30, 120, 220);
            };
            BIOME_TARGET_COLORS[b.ordinal()] = c;
            int r = (int) Math.round(c.getRed() * 255.0);
            int g = (int) Math.round(c.getGreen() * 255.0);
            int bVal = (int) Math.round(c.getBlue() * 255.0);
            BIOME_TARGET_ARGBS[b.ordinal()] = (255 << 24) | (r << 16) | (g << 8) | bVal;
        }
    }

    /**
     * Matches raw 32-bit ARGB pixel integer to the nearest {@link Biome} based on minimal Euclidean color distance.
     * Operates completely in primitive integer space with zero heap allocation.
     *
     * @param argb the 32-bit ARGB packed integer
     * @return the closest matching Biome
     */
    public static Biome matchBiomeArgb(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int maxChannel = Math.max(r, Math.max(g, b));
        if (maxChannel < 20) {
            return Biome.DEEP_OCEAN;
        }
        Biome best = Biome.OCEAN;
        int minSqDist = Integer.MAX_VALUE;
        for (Biome bm : Biome.VALUES) {
            int target = BIOME_TARGET_ARGBS[bm.ordinal()];
            int tr = (target >> 16) & 0xFF;
            int tg = (target >> 8) & 0xFF;
            int tb = target & 0xFF;
            int dr = r - tr;
            int dg = g - tg;
            int db = b - tb;
            int sqDist = dr * dr + dg * dg + db * db;
            if (sqDist < minSqDist) {
                minSqDist = sqDist;
                best = bm;
            }
        }
        return best;
    }

    /**
     * Match pixel color to nearest Biome.
     *
     * @param c the color to match
     * @return the nearest Biome
     */
    public Biome matchBiomeColor(Color c) {
        if (c == null) return Biome.OCEAN;
        int r = (int) Math.round(c.getRed() * 255.0);
        int g = (int) Math.round(c.getGreen() * 255.0);
        int b = (int) Math.round(c.getBlue() * 255.0);
        return matchBiomeArgb((255 << 24) | (r << 16) | (g << 8) | b);
    }

    /**
     * Define expected colors for biomes in the input map.
     *
     * @param b the biome
     * @return the target Color
     */
    public Color getBiomeTargetColor(Biome b) {
        return b != null ? BIOME_TARGET_COLORS[b.ordinal()] : BIOME_TARGET_COLORS[Biome.OCEAN.ordinal()];
    }

    /**
     * Export an image map to a standard PNG file along with a standard ESRI World File (.tfw).
     *
     * @param image the JavaFX Image
     * @param targetPngFile destination file
     */
    public void exportMapToPngAndWorldFile(Image image, File targetPngFile) {
        ImageMapCodecAndBufferedMapper.exportJavaFxMapToPngAndWorldFile(image, targetPngFile);
    }

    /**
     * Export a standard headless {@link BufferedImage} to PNG along with standard ESRI World File (.tfw).
     *
     * @param bufferedImage the buffered image
     * @param targetPngFile destination file
     */
    public void exportMapToPngAndWorldFile(BufferedImage bufferedImage, File targetPngFile) {
        ImageMapCodecAndBufferedMapper.exportBufferedMapToPngAndWorldFile(bufferedImage, targetPngFile);
    }

    /**
     * Convert a JavaFX Image to a compressed Base64 PNG string for JSON preset persistence.
     *
     * @param image the JavaFX Image
     * @return base64 encoded PNG string
     */
    public static String imageToBase64Png(Image image) {
        return ImageMapCodecAndBufferedMapper.imageToBase64Png(image);
    }

    /**
     * Loads a map image prioritizing the single canonical location {@code data/ether/<planet>/<year>/}
     * using {@link TemporalMapTensorManager} with automatic epoch fallback and continuous interpolation.
     *
     * @param mapFileName the file name or layer tag
     * @return the resolved JavaFX Image, or null
     */
    public static Image loadMapImage(String mapFileName) {
        return loadMapImage("earth", 2026L, mapFileName);
    }

    /**
     * Loads a map image for a specific planet, epoch year, and layer.
     *
     * @param planet the planet identifier
     * @param year the epoch year
     * @param mapFileName the file name or layer tag
     * @return the resolved JavaFX Image, or null
     */
    public static Image loadMapImage(String planet, long year, String mapFileName) {
        if (mapFileName == null || mapFileName.isBlank()) return null;
        return TemporalMapTensorManager.loadTemporalMapImage(planet, year, mapFileName);
    }

    /**
     * Loads a headless {@link BufferedImage} using the default epoch (2026).
     *
     * @param mapFileName the file name or layer tag
     * @return the resolved BufferedImage, or null
     */
    public static BufferedImage loadMapBufferedImage(String mapFileName) {
        return loadMapBufferedImage("earth", 2026L, mapFileName);
    }

    /**
     * Loads a headless {@link BufferedImage} for a specific planet, epoch year, and layer.
     *
     * @param planet the planet identifier
     * @param year the epoch year
     * @param mapFileName the file name or layer tag
     * @return the resolved BufferedImage, or null
     */
    public static BufferedImage loadMapBufferedImage(String planet, long year, String mapFileName) {
        if (mapFileName == null || mapFileName.isBlank()) return null;
        return TemporalMapTensorManager.loadTemporalMapBufferedImage(planet, year, mapFileName);
    }

    /**
     * Convert a Base64 PNG string back into a JavaFX Image with high-performance caching.
     *
     * @param base64 the Base64 PNG string
     * @return the reconstructed JavaFX Image
     */
    public static Image base64PngToImage(String base64) {
        return ImageMapCodecAndBufferedMapper.base64PngToImage(base64);
    }

    /**
     * Validation result for map images.
     *
     * @param valid whether the image is valid
     * @param message descriptive status message
     * @param width image width in pixels
     * @param height image height in pixels
     * @param aspectRatio aspect ratio width / height
     */
    public record ImageValidationResult(boolean valid, String message, int width, int height, double aspectRatio) {}

    /**
     * Checks effective raster image loading and verifies dimensions, pixel reader accessibility, and sanity.
     *
     * @param image the JavaFX Image to validate
     * @return the validation result
     */
    public static ImageValidationResult validateMapImage(Image image) {
        return ImageMapCodecAndBufferedMapper.validateMapImage(image);
    }
}
