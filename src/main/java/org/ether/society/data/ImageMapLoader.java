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
        mapImagesToCells(cells, elevImg, biomeImg, resourceImg, null, null, null, minAlt, maxAlt);
    }

    public void mapImagesToCells(List<H3Cell> cells, Image elevImg, Image biomeImg, Image resourceImg, Image climateImg, double minAlt, double maxAlt) {
        mapImagesToCells(cells, elevImg, biomeImg, resourceImg, climateImg, null, null, minAlt, maxAlt);
    }

    public void mapImagesToCells(List<H3Cell> cells, Image elevImg, Image biomeImg, Image resourceImg, Image climateImg, Image rainfallImg, double minAlt, double maxAlt) {
        mapImagesToCells(cells, elevImg, biomeImg, resourceImg, climateImg, rainfallImg, null, minAlt, maxAlt);
    }

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
                double elevation;
                if (Math.abs(minAlt + maxAlt) < 500.0) {
                    // Standard linear grayscale heightmap (-Alt to +Alt)
                    elevation = minAlt + brightness * (maxAlt - minAlt);
                } else {
                    double waterLvl = 0.35; // Standard sea level baseline threshold
                    if (brightness < waterLvl) {
                        elevation = minAlt * (1.0 - brightness / waterLvl);
                    } else {
                        elevation = maxAlt * ((brightness - waterLvl) / (1.0 - waterLvl));
                    }
                }
                cell.setElevation(elevation);
            }

            // 2. Sample Biome
            if (biomeReader != null) {
                int x = (int) Math.min(u * wBiome, wBiome - 1);
                int y = (int) Math.min(v * hBiome, hBiome - 1);
                Color c = biomeReader.getColor(x, y);
                cell.setBiome(matchBiomeColor(c));
            } else if (elevReader != null) {
                double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
                if (elev < 0) {
                    cell.setBiome(Biome.OCEAN);
                } else if (elev > 0.85 * maxAlt) {
                    cell.setBiome(Biome.SNOW);
                } else if (elev > 0.65 * maxAlt) {
                    cell.setBiome(Biome.MOUNTAINS);
                } else if (elev > 0.35 * maxAlt) {
                    cell.setBiome(Biome.HILLS);
                } else {
                    cell.setBiome(Biome.PLAINS);
                }
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

            // 4. Sample Climate / Temperature Map
            if (climateReader != null) {
                int x = (int) Math.min(u * wClimate, wClimate - 1);
                int y = (int) Math.min(v * hClimate, hClimate - 1);
                Color c = climateReader.getColor(x, y);
                // Red channel or brightness represents temperature (-50°C to +50°C)
                double tempC = -50.0 + c.getRed() * 100.0;
                cell.setTemperature(tempC);
                // Green channel represents rainfall (0 to 3000 mm/yr) if no separate rainfall map
                if (rainfallReader == null) {
                    cell.setRainfall(c.getGreen() * 3000.0);
                }
            }

            // 5. Sample Separate Rainfall / Moisture Map
            if (rainfallReader != null) {
                int x = (int) Math.min(u * wRain, wRain - 1);
                int y = (int) Math.min(v * hRain, hRain - 1);
                Color c = rainfallReader.getColor(x, y);
                cell.setRainfall(c.getBrightness() * 3000.0);
            }

            // 6. Sample Separate Seasonality / Temp Amplitude Map
            if (seasonalityReader != null) {
                int x = (int) Math.min(u * wSeason, wSeason - 1);
                int y = (int) Math.min(v * hSeason, hSeason - 1);
                Color c = seasonalityReader.getColor(x, y);
                // Brightness represents seasonal temperature delta (0°C to 50°C)
                double seasonalDelta = c.getBrightness() * 50.0;
                cell.setTemperature(cell.getTemperature() + (Math.sin(Math.toRadians(lat)) * seasonalDelta * 0.5));
            }
        }

        logger.info("Mapped data from map images (Elevation: {}, Biome: {}, Resource: {}, Climate: {}, Rain: {}, Season: {}) to {} cells",
                elevImg != null, biomeImg != null, resourceImg != null, climateImg != null, rainfallImg != null, seasonalityImg != null, cells.size());
    }

    /**
     * Match pixel color to nearest Biome.
     */
    public Biome matchBiomeColor(Color c) {
        if (c == null) return Biome.OCEAN;
        if (c.getBrightness() < 0.08) {
            return Biome.DEEP_OCEAN;
        }
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

    /**
     * Convert a JavaFX Image to a compressed Base64 PNG string for JSON preset persistence.
     */
    public static String imageToBase64Png(Image image) {
        if (image == null) return null;
        try {
            int width = (int) image.getWidth();
            int height = (int) image.getHeight();
            if (width <= 0 || height <= 0) return null;
            java.awt.image.BufferedImage bImage = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            PixelReader reader = image.getPixelReader();
            if (reader != null) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        bImage.setRGB(x, y, reader.getArgb(x, y));
                    }
                }
            }
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(bImage, "png", baos);
            return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            logger.error("Failed to convert image to Base64 PNG", e);
            return null;
        }
    }

    /**
     * Loads a map image prioritizing the single canonical location data/maps/ether/<planet>/
     * (e.g. data/maps/ether/earth/earth_elevation.png), with transparent alias resolution
     * (terre->earth, lune->moon, mercure->mercury) without duplicating files on disk.
     */
    public static Image loadMapImage(String mapFileName) {
        if (mapFileName == null || mapFileName.isBlank()) return null;
        String clean = mapFileName.startsWith("/") ? mapFileName.substring(1) : mapFileName;
        if (clean.startsWith("maps/")) clean = clean.substring(5);
        if (clean.startsWith("ether/")) clean = clean.substring(6);

        String subDir = null;
        String baseName = clean;
        int slashIdx = clean.lastIndexOf('/');
        if (slashIdx >= 0) {
            subDir = clean.substring(0, slashIdx);
            baseName = clean.substring(slashIdx + 1);
        }

        // Canonical preset mapping
        String canonicalSub = normalizePresetDir(subDir != null ? subDir : deducePresetFromFileName(baseName));

        // 1. Check primary canonical path: data/maps/ether/<canonicalSub>/<baseName>
        if (canonicalSub != null) {
            File targetFile = new File("data/maps/ether/" + canonicalSub + "/" + baseName);
            if (targetFile.exists() && targetFile.isFile()) {
                try {
                    return new Image(new java.io.FileInputStream(targetFile));
                } catch (Exception e) {
                    logger.warn("Failed to load map from {}", targetFile.getAbsolutePath(), e);
                }
            }
        }

        // 2. Scan standard canonical planetary subdirectories under data/maps/ether/
        String[] canonicalDirs = {"earth", "moon", "mars", "venus", "mercury"};
        for (String cDir : canonicalDirs) {
            File subFile = new File("data/maps/ether/" + cDir + "/" + baseName);
            if (subFile.exists() && subFile.isFile()) {
                try {
                    return new Image(new java.io.FileInputStream(subFile));
                } catch (Exception e) {
                    logger.warn("Failed to load map from {}", subFile.getAbsolutePath(), e);
                }
            }
        }

        // 3. Fallback to direct path in data/maps/
        File directFile = new File("data/maps/" + clean);
        if (directFile.exists() && directFile.isFile()) {
            try {
                return new Image(new java.io.FileInputStream(directFile));
            } catch (Exception e) {
                logger.warn("Failed to load map from {}", directFile.getAbsolutePath(), e);
            }
        }

        // 4. Fallback to classpath /maps/
        var stream = ImageMapLoader.class.getResourceAsStream("/maps/" + clean);
        if (stream != null) {
            return new Image(stream);
        }
        for (String cDir : canonicalDirs) {
            var subStream = ImageMapLoader.class.getResourceAsStream("/maps/" + cDir + "/" + baseName);
            if (subStream != null) return new Image(subStream);
        }
        return null;
    }

    private static String normalizePresetDir(String dir) {
        if (dir == null) return null;
        String lower = dir.toLowerCase().trim();
        return switch (lower) {
            case "terre", "earth" -> "earth";
            case "lune", "moon" -> "moon";
            case "mars", "ares" -> "mars";
            case "venus", "hesperos" -> "venus";
            case "mercure", "mercury", "hermes" -> "mercury";
            default -> lower;
        };
    }

    private static String deducePresetFromFileName(String fileName) {
        if (fileName == null) return null;
        String lower = fileName.toLowerCase();
        if (lower.startsWith("earth_") || lower.startsWith("terre_")) return "earth";
        if (lower.startsWith("moon_") || lower.startsWith("lune_")) return "moon";
        if (lower.startsWith("mars_")) return "mars";
        if (lower.startsWith("venus_")) return "venus";
        if (lower.startsWith("mercury_") || lower.startsWith("mercure_")) return "mercury";
        return null;
    }

    /**
     * Convert a Base64 PNG string back into a JavaFX Image.
     */
    public static Image base64PngToImage(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(base64.trim());
            return new Image(new java.io.ByteArrayInputStream(bytes));
        } catch (Exception e) {
            logger.error("Failed to convert Base64 PNG to image", e);
            return null;
        }
    }

    /**
     * Validation result for map images.
     */
    public record ImageValidationResult(boolean valid, String message, int width, int height, double aspectRatio) {}

    /**
     * Checks effective raster image loading and verifies dimensions, pixel reader accessibility, and sanity.
     */
    public static ImageValidationResult validateMapImage(Image image) {
        if (image == null) {
            return new ImageValidationResult(false, "Image non fournie (null)", 0, 0, 0.0);
        }
        if (image.isError()) {
            String err = image.getException() != null ? image.getException().getMessage() : "Format ou fichier illisible";
            return new ImageValidationResult(false, "Fichier corrompu ou illisible: " + err, 0, 0, 0.0);
        }
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        if (w <= 0 || h <= 0) {
            return new ImageValidationResult(false, "Dimensions d'image invalides (0x0)", w, h, 0.0);
        }
        PixelReader reader = image.getPixelReader();
        if (reader == null) {
            return new ImageValidationResult(false, "Impossible de lire la matrice de pixels (PixelReader null)", w, h, 0.0);
        }
        try {
            // Sample test pixels
            reader.getColor(0, 0);
            reader.getColor(w / 2, h / 2);
            reader.getColor(w - 1, h - 1);
        } catch (Exception ex) {
            return new ImageValidationResult(false, "Échec de lecture des pixels raster: " + ex.getMessage(), w, h, 0.0);
        }
        double ratio = (double) w / (double) h;
        return new ImageValidationResult(true, String.format("Valide (%dx%d, ratio %.2f)", w, h, ratio), w, h, ratio);
    }
}

