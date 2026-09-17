/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot & Gemini AI
 */
package org.ether.society.data;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * High-Performance Temporal Cartographic Tensor Manager.
 * Resolves, caches, and seamlessly interpolates planetary raster maps across geological,
 * paleoclimatic (-100,000 BP to 0), and historical/industrial epochs (1800 to 2026+).
 *
 * Dedicated storage: data/maps/ether/<planet>/<year>/<layer>.png
 */
public class TemporalMapTensorManager {
    private static final Logger logger = LoggerFactory.getLogger(TemporalMapTensorManager.class);

    public static final long[] STANDARD_EARTH_EPOCHS = {
        -100000L, // Out of Africa / Early Glacial Inception
        -20000L,  // Last Glacial Maximum (LGM, Sea Level -120m, Mammoth Steppe)
        -10000L,  // Younger Dryas Recovery & Neolithization
        -6000L,   // Holocene Climate Optimum & Green Sahara / Mega-Chad
        -3000L,   // Bronze Age / First Hydraulic Civilizations
        0L,       // Roman Optimum / Antiquity Baseline
        1000L,    // Medieval Climate Optimum
        1800L,    // Industrial Revolution Dawn (100% Virgin Resource Stocks)
        1900L,    // Early Industrial Coal/Oil Expansion
        1950L,    // Great Acceleration (Deep Aquifer Pumping, Global Hydrocarbons)
        2026L     // Anthropocene / Present-Day Empirical Baseline (GEM/USGS/WHYMAP)
    };

    private static final Map<String, Image> memoryCache = new HashMap<>();

    /**
     * Resolves the canonical directory name for a planet (e.g. terre -> earth).
     */
    public static String normalizePlanet(String planet) {
        if (planet == null || planet.isBlank()) return "earth";
        String p = planet.trim().toLowerCase();
        return switch (p) {
            case "terre", "earth", "terra", "gaia" -> "earth";
            case "lune", "moon", "luna", "selene" -> "moon";
            case "mars", "ares" -> "mars";
            case "venus", "hesperos", "aphrodite" -> "venus";
            case "mercure", "mercury", "hermes" -> "mercury";
            default -> p;
        };
    }

    /**
     * Standardizes layer filenames (e.g. "elevation" -> "earth_elevation.png").
     */
    public static String normalizeLayerFilename(String planet, String layerName) {
        String p = normalizePlanet(planet);
        if (layerName == null || layerName.isBlank()) return p + "_elevation.png";
        String base = layerName.trim().toLowerCase();
        if (base.startsWith("/")) base = base.substring(1);
        if (base.contains("/")) base = base.substring(base.lastIndexOf('/') + 1);
        if (!base.endsWith(".png")) base = base + ".png";
        if (!base.startsWith(p + "_")) {
            // Remove previous planet prefix if any
            for (String prefix : List.of("earth_", "mars_", "moon_", "venus_", "mercury_", "terre_", "lune_", "mercure_")) {
                if (base.startsWith(prefix)) {
                    base = base.substring(prefix.length());
                    break;
                }
            }
            base = p + "_" + base;
        }
        return base;
    }

    /**
     * Discovers all available snapshot year folders on disk for a given planet.
     */
    public static List<Long> getAvailableEpochYears(String planet) {
        String p = normalizePlanet(planet);
        File planetDir = new File("data/maps/ether/" + p);
        List<Long> years = new ArrayList<>();
        if (!planetDir.exists() || !planetDir.isDirectory()) {
            return years;
        }
        File[] subDirs = planetDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File d : subDirs) {
                try {
                    long y = Long.parseLong(d.getName());
                    years.add(y);
                } catch (NumberFormatException ignored) {}
            }
        }
        Collections.sort(years);
        return years;
    }

    /**
     * Loads a temporal map image for a given planet, requested year, and layer.
     * Performs automatic temporal fallback or continuous bilinear cross-epoch interpolation.
     */
    public static Image loadTemporalMapImage(String planet, long requestedYear, String layerName) {
        String p = normalizePlanet(planet);
        String baseFilename = normalizeLayerFilename(p, layerName);
        String cacheKey = p + ":" + requestedYear + ":" + baseFilename;

        synchronized (memoryCache) {
            if (memoryCache.containsKey(cacheKey)) {
                return memoryCache.get(cacheKey);
            }
        }

        // 1. Direct hit on exact year folder
        File exactFile = new File("data/maps/ether/" + p + "/" + requestedYear + "/" + baseFilename);
        if (exactFile.exists() && exactFile.isFile()) {
            try (InputStream is = new FileInputStream(exactFile)) {
                Image img = new Image(is);
                synchronized (memoryCache) {
                    memoryCache.put(cacheKey, img);
                }
                return img;
            } catch (Exception e) {
                logger.warn("Failed to load map from {}", exactFile.getAbsolutePath(), e);
            }
        }

        // 2. Discover available epochs for this planet
        List<Long> epochs = getAvailableEpochYears(p);
        if (epochs.isEmpty()) {
            // Check unversioned fallback
            File unversioned = new File("data/maps/ether/" + p + "/" + baseFilename);
            if (unversioned.exists()) {
                try (InputStream is = new FileInputStream(unversioned)) {
                    Image img = new Image(is);
                    synchronized (memoryCache) {
                        memoryCache.put(cacheKey, img);
                    }
                    return img;
                } catch (Exception ignored) {}
            }
            return null;
        }

        // 3. For prehistoric resources (coal, oil, gas, aquifers) before 1800, clamp to 1800 (virgin stock)
        boolean isDepletableResource = baseFilename.contains("coal") || baseFilename.contains("oil") ||
                                       baseFilename.contains("gas") || baseFilename.contains("aquifers");
        if (isDepletableResource && requestedYear < 1800L) {
            requestedYear = 1800L;
        }

        // 4. Find bounding epochs [Y0, Y1]
        long y0 = epochs.get(0);
        long y1 = epochs.get(epochs.size() - 1);

        if (requestedYear <= y0) {
            return loadExactEpochImage(p, y0, baseFilename, cacheKey);
        }
        if (requestedYear >= y1) {
            return loadExactEpochImage(p, y1, baseFilename, cacheKey);
        }

        long prevYear = y0;
        long nextYear = y1;
        for (long ep : epochs) {
            if (ep <= requestedYear) {
                prevYear = ep;
            }
            if (ep >= requestedYear && nextYear == y1) {
                nextYear = ep;
                break;
            }
        }

        if (prevYear == nextYear || baseFilename.contains("biomes")) {
            // Categorical biomes snap to nearest epoch to avoid blended invalid color IDs
            long nearest = Math.abs(requestedYear - prevYear) <= Math.abs(requestedYear - nextYear) ? prevYear : nextYear;
            return loadExactEpochImage(p, nearest, baseFilename, cacheKey);
        }

        // 5. Continuous Temporal Interpolation for Grayscale Scalar Tensors
        Image img0 = loadExactEpochImage(p, prevYear, baseFilename, null);
        Image img1 = loadExactEpochImage(p, nextYear, baseFilename, null);

        if (img0 == null && img1 == null) return null;
        if (img0 == null) return img1;
        if (img1 == null) return img0;

        double t = (double) (requestedYear - prevYear) / (double) (nextYear - prevYear);
        Image interpolated = interpolateImages(img0, img1, t);
        if (interpolated != null) {
            synchronized (memoryCache) {
                memoryCache.put(cacheKey, interpolated);
            }
        }
        return interpolated;
    }

    private static Image loadExactEpochImage(String planet, long epoch, String filename, String cacheKey) {
        File f = new File("data/maps/ether/" + planet + "/" + epoch + "/" + filename);
        if (!f.exists()) {
            f = new File("data/maps/ether/" + planet + "/" + filename);
        }
        if (f.exists() && f.isFile()) {
            try (InputStream is = new FileInputStream(f)) {
                Image img = new Image(is);
                if (cacheKey != null) {
                    synchronized (memoryCache) {
                        memoryCache.put(cacheKey, img);
                    }
                }
                return img;
            } catch (Exception e) {
                logger.warn("Could not read epoch file: {}", f.getAbsolutePath());
            }
        }
        return null;
    }

    /**
     * Performs linear cross-fade blending between two grayscale / scalar raster maps.
     */
    public static Image interpolateImages(Image img0, Image img1, double t) {
        t = Math.clamp(t, 0.0, 1.0);
        int w = (int) Math.min(img0.getWidth(), img1.getWidth());
        int h = (int) Math.min(img0.getHeight(), img1.getHeight());

        javafx.scene.image.WritableImage result = new javafx.scene.image.WritableImage(w, h);
        javafx.scene.image.PixelReader r0 = img0.getPixelReader();
        javafx.scene.image.PixelReader r1 = img1.getPixelReader();
        javafx.scene.image.PixelWriter pw = result.getPixelWriter();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                javafx.scene.paint.Color c0 = r0.getColor(x, y);
                javafx.scene.paint.Color c1 = r1.getColor(x, y);

                double red = c0.getRed() + t * (c1.getRed() - c0.getRed());
                double green = c0.getGreen() + t * (c1.getGreen() - c0.getGreen());
                double blue = c0.getBlue() + t * (c1.getBlue() - c0.getBlue());
                double opacity = c0.getOpacity() + t * (c1.getOpacity() - c0.getOpacity());

                pw.setColor(x, y, new javafx.scene.paint.Color(
                    Math.clamp(red, 0.0, 1.0),
                    Math.clamp(green, 0.0, 1.0),
                    Math.clamp(blue, 0.0, 1.0),
                    Math.clamp(opacity, 0.0, 1.0)
                ));
            }
        }
        return result;
    }

    /**
     * Clears all cached in-memory raster textures.
     */
    public static void clearCache() {
        synchronized (memoryCache) {
            memoryCache.clear();
        }
    }
}
