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
        -50000L,  // MIS 3 / Sahul Colonization
        -25000L,  // LGM Onset / Proto-Beringia
        -20000L,  // Last Glacial Maximum (LGM, Sea Level -120m, Mammoth Steppe)
        -10900L,  // Younger Dryas Cold Event
        -10000L,  // Younger Dryas Recovery & Neolithization
        -8000L,   // 8.2 ka Event / Early Agriculture
        -6000L,   // Holocene Climate Optimum & Green Sahara / Mega-Chad
        -3000L,   // Early Bronze Age
        -1900L,   // Middle Bronze Age / Hammurabi & Shang
        -1000L,   // Early Iron Age / Phoenicians & Zhou
        0L,       // Roman Optimum / Antiquity Baseline
        536L,     // Late Antique Little Ice Age / Volcanic Winter
        1000L,    // Medieval Climate Optimum
        1324L,    // Mali Empire / Mansa Musa Pilgrimage
        1492L,    // Age of Discovery / Columbian Exchange
        1639L,    // Early Modern / Treaty of Zuhab
        1800L,    // Industrial Revolution Dawn (100% Virgin Resource Stocks)
        1900L,    // Early Industrial Coal/Oil Expansion
        1950L,    // Great Acceleration (Deep Aquifer Pumping, Global Hydrocarbons)
        2000L,    // Digital Revolution & Global Trade
        2026L,    // Anthropocene / Present-Day Empirical Baseline (GEM/USGS/WHYMAP)
        2035L,    // Near-Future Energy Transition
        2045L,    // Mid-Century Technological Singularity
        2050L,    // Mid-Century Demographic Peak
        2060L     // Post-Transition Stable State
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
     * Standardizes layer name into short canonical tag (e.g. "density", "pathogen", "elevation", "coal").
     */
    public static String canonicalLayerTag(String layerName) {
        if (layerName == null || layerName.isBlank()) return "elevation";
        String base = layerName.trim().toLowerCase(Locale.ROOT);
        if (base.startsWith("/")) base = base.substring(1);
        if (base.contains("/")) base = base.substring(base.lastIndexOf('/') + 1);
        if (base.endsWith(".png")) base = base.substring(0, base.length() - 4);
        // Strip planet prefix if present
        for (String prefix : List.of("earth_", "mars_", "moon_", "venus_", "mercury_", "terre_", "lune_", "mercure_")) {
            if (base.startsWith(prefix)) {
                base = base.substring(prefix.length());
                break;
            }
        }
        // Strip any leading year digits if present (e.g. 1000_density -> density)
        if (base.matches("^-?\\d+_.+")) {
            base = base.substring(base.indexOf('_') + 1);
        }
        // Canonical aliases
        return switch (base) {
            case "geothermal", "mantleheat", "mantle_heat" -> "geothermal";
            case "trade", "tradenetwork", "trade_network" -> "tradenetwork";
            case "ironcopper", "iron_copper" -> "iron_copper";
            case "preciousmetals", "precious_metals", "preciousree", "precious_ree" -> "precious_metals";
            case "rareearths", "rare_earths" -> "rare_earths";
            case "aquifer", "aquifers" -> "aquifers";
            case "he3", "helium3", "helium_3" -> "helium3";
            case "temp", "temperature" -> "temperature";
            case "rain", "rainfall", "precipitation" -> "precipitation";
            case "elev", "elevation", "dem" -> "elevation";
            case "biome", "biomes" -> "biomes";
            case "pop", "density", "population" -> "density";
            case "sovereignty", "borders", "empire", "polity" -> "sovereignty";
            case "language", "isogloss", "dialects" -> "isogloss";
            case "kinship", "clans", "family" -> "kinship";
            case "religion", "rituals", "sacred" -> "rituals";
            case "tech", "technology" -> "technology";
            case "inst", "institutional", "complexity" -> "institutional";
            case "eco", "ecological", "footprint" -> "ecological";
            case "pathogen", "disease", "immunity" -> "pathogen";
            default -> base;
        };
    }

    /**
     * Standardizes full dated layer filename (e.g. "earth_1000_density.png").
     */
    public static String buildStandardFilename(String planet, long epoch, String layerTag) {
        String p = normalizePlanet(planet);
        String tag = canonicalLayerTag(layerTag);
        return p + "_" + epoch + "_" + tag + ".png";
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
     * Performs automatic temporal fallback, causal forward step transitions for historical/epidemiological events,
     * or continuous bilinear cross-epoch interpolation for physical fields.
     */
    public static Image loadTemporalMapImage(String planet, long requestedYear, String layerName) {
        String p = normalizePlanet(planet);
        String tag = canonicalLayerTag(layerName);
        String cacheKey = p + ":" + requestedYear + ":" + tag;

        synchronized (memoryCache) {
            if (memoryCache.containsKey(cacheKey)) {
                return memoryCache.get(cacheKey);
            }
        }

        // 1. Discover available epochs for this planet
        List<Long> epochs = getAvailableEpochYears(p);
        if (epochs.isEmpty()) {
            return null;
        }

        // 2. Depletable resources prior to 1800 clamp to 1800 (virgin stock)
        boolean isDepletableResource = tag.equals("coal") || tag.equals("oil") || tag.equals("gas") || tag.equals("aquifers");
        if (isDepletableResource && requestedYear < 1800L) {
            requestedYear = 1800L;
        }

        // 3. Check for exact match in epoch folder
        if (epochs.contains(requestedYear)) {
            Image direct = loadExactEpochImage(p, requestedYear, tag, cacheKey);
            if (direct != null) return direct;
        }

        // 4. Find bounding epochs [prevYear, nextYear]
        long y0 = epochs.get(0);
        long y1 = epochs.get(epochs.size() - 1);

        if (requestedYear <= y0) {
            return loadExactEpochImage(p, y0, tag, cacheKey);
        }
        if (requestedYear >= y1) {
            return loadExactEpochImage(p, y1, tag, cacheKey);
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

        if (prevYear == nextYear) {
            return loadExactEpochImage(p, prevYear, tag, cacheKey);
        }

        // 5. Layer-Specific Interpolation Dynamics:
        // A. Categorical biomes: snap to nearest epoch
        if (tag.equals("biomes")) {
            long nearest = Math.abs(requestedYear - prevYear) <= Math.abs(requestedYear - nextYear) ? prevYear : nextYear;
            return loadExactEpochImage(p, nearest, tag, cacheKey);
        }

        // B. Causal Historical / Epidemiological / Cultural Tensors:
        // For discontinuous events (e.g. 1492 contact), do NOT leak future events backward.
        // Before 1492, the Americas were in isolation; smallpox or European sovereignty must not appear in 1450.
        boolean isCausalHistorical = tag.equals("pathogen") || tag.equals("sovereignty") ||
                                    tag.equals("technology") || tag.equals("isogloss") ||
                                    tag.equals("kinship") || tag.equals("rituals") ||
                                    tag.equals("institutional");
        if (isCausalHistorical) {
            // Check for key historical shock boundaries: e.g. 1492 (Columbian Contact), 1800 (Industrialization)
            if (prevYear <= 1491L && nextYear >= 1492L && requestedYear < 1492L) {
                // Strictly stay on the pre-contact baseline (1491 or earlier) without backward contamination
                return loadExactEpochImage(p, prevYear, tag, cacheKey);
            }
            if (prevYear <= 1800L && nextYear > 1800L && requestedYear < 1800L) {
                return loadExactEpochImage(p, prevYear, tag, cacheKey);
            }
        }

        // C. Continuous Linear Bilinear Cross-Fade Interpolation
        Image img0 = loadExactEpochImage(p, prevYear, tag, null);
        Image img1 = loadExactEpochImage(p, nextYear, tag, null);

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

    /**
     * Loads an exact epoch image with multi-pattern fallback.
     */
    public static Image loadExactEpochImage(String planet, long epoch, String layerTag, String cacheKey) {
        String p = normalizePlanet(planet);
        String tag = canonicalLayerTag(layerTag);
        String dirPath = "data/maps/ether/" + p + "/" + epoch + "/";

        // Candidate file names in priority order:
        // 1. Standard: earth_1000_density.png
        // 2. Short prefix: earth_density.png
        // 3. Raw tag: density.png
        String[] candidateNames = {
            p + "_" + epoch + "_" + tag + ".png",
            p + "_" + tag + ".png",
            tag + ".png"
        };

        for (String cName : candidateNames) {
            File f = new File(dirPath + cName);
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
