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
 * Dedicated storage: data/ether/<planet>/<year>/<layer>.png
 */
public class TemporalMapTensorManager {
    private static final Logger logger = LoggerFactory.getLogger(TemporalMapTensorManager.class);

    /**
     * Data resolution & fallback strategy when exact empirical measurements for a target date are missing.
     */
    public enum DataFallbackStrategy {
        CONTINUOUS_INTERPOLATION("scenario.fallback.interpolation", "ðŸ”„ Interpolation temporelle continue (Fondu barycentrique)"),
        PREVIOUS_EARLIER_EPOCH("scenario.fallback.earlier_epoch", "â®ï¸ Repli conservateur sur l'Ã©poque antÃ©rieure connue (Ã‰vite tout anachronisme)"),
        CLOSEST_ANCHOR_EPOCH("scenario.fallback.closest_anchor", "ðŸŽ¯ Jalon Ã©talonnÃ© le plus proche (Distance chronologique minimale)");

        /** Internal state: i18n key (String). */
        private final String i18nKey;
        /** Internal state: default label (String). */
        private final String defaultLabel;

        DataFallbackStrategy(String i18nKey, String defaultLabel) {
            this.i18nKey = i18nKey;
            this.defaultLabel = defaultLabel;
        }

        /**
         * Get i18n key.
         *
         * @return the resulting computation or state reference
         */
        public String getI18nKey() { return i18nKey; }
        /**
         * Get default label.
         *
         * @return the resulting computation or state reference
         */
        public String getDefaultLabel() { return defaultLabel; }
        /**
         * Get localized name.
         *
         * @return the resulting computation or state reference
         */
        public String getLocalizedName() {
            try {
                return org.ether.society.i18n.I18n.getOrDefault(i18nKey, defaultLabel);
            } catch (Exception ignored) {
                return defaultLabel;
            }
        }
    }

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
    private static final Map<String, BufferedImage> bufferedMemoryCache = new HashMap<>();

    /**
     * Immutable resolution record representing either a direct epoch target or an interpolation bracket.
     *
     * @param direct true if a single epoch snapshot should be used directly
     * @param directEpoch the target epoch year when direct is true
     * @param prevEpoch the bounding earlier epoch year when direct is false
     * @param nextEpoch the bounding later epoch year when direct is false
     * @param t the normalized barycentric interpolation factor in [0.0, 1.0]
     */
    public record TemporalEpochResolution(boolean direct, long directEpoch, long prevEpoch, long nextEpoch, double t) {}

    /**
     * Resolves the canonical directory name for a planet (e.g. terre -> earth).
     *
     * @param planet raw planet identifier
     * @return sanitized canonical planet directory name
     */
    public static String normalizePlanet(String planet) {
        return TemporalRasterInterpolator.normalizePlanet(planet);
    }

    /**
     * Standardizes layer name into short canonical tag (e.g. "density", "pathogen", "elevation", "coal").
     *
     * @param layerName raw layer name or filename
     * @return canonical layer tag
     */
    public static String canonicalLayerTag(String layerName) {
        return TemporalRasterInterpolator.canonicalLayerTag(layerName);
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
        File planetDir = org.ether.society.config.EtherPaths.getEtherDir().resolve(p).toFile(); if (!planetDir.exists()) planetDir = new File("data/maps/ether/" + p); if (!planetDir.exists()) planetDir = new File("data/ether/" + p);
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
     * Resolves the target epoch or temporal interpolation bracket according to simulation rules.
     *
     * @param planet the planet identifier (e.g., "earth")
     * @param requestedYear the target year
     * @param layerName the canonical or alias layer name
     * @param strategy the fallback strategy
     * @return the resolved {@link TemporalEpochResolution}, or null if no epochs exist for the planet
     */
    public static TemporalEpochResolution resolveTemporalEpoch(String planet, long requestedYear, String layerName, DataFallbackStrategy strategy) {
        if (strategy == null) {
            strategy = DataFallbackStrategy.CONTINUOUS_INTERPOLATION;
        }
        String p = normalizePlanet(planet);
        String tag = canonicalLayerTag(layerName);

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
            return new TemporalEpochResolution(true, requestedYear, 0L, 0L, 0.0);
        }

        // 4. Find bounding epochs [prevYear, nextYear]
        long y0 = epochs.get(0);
        long y1 = epochs.get(epochs.size() - 1);

        if (requestedYear <= y0) {
            return new TemporalEpochResolution(true, y0, 0L, 0L, 0.0);
        }
        if (requestedYear >= y1) {
            return new TemporalEpochResolution(true, y1, 0L, 0L, 0.0);
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
            return new TemporalEpochResolution(true, prevYear, 0L, 0L, 0.0);
        }

        // 5. Strategy Evaluation:
        if (strategy == DataFallbackStrategy.PREVIOUS_EARLIER_EPOCH) {
            return new TemporalEpochResolution(true, prevYear, 0L, 0L, 0.0);
        }

        if (strategy == DataFallbackStrategy.CLOSEST_ANCHOR_EPOCH) {
            long closest = Math.abs(requestedYear - prevYear) <= Math.abs(requestedYear - nextYear) ? prevYear : nextYear;
            return new TemporalEpochResolution(true, closest, 0L, 0L, 0.0);
        }

        // 6. CONTINUOUS_INTERPOLATION (Default & Standard):
        if (tag.equals("biomes")) {
            long nearest = Math.abs(requestedYear - prevYear) <= Math.abs(requestedYear - nextYear) ? prevYear : nextYear;
            return new TemporalEpochResolution(true, nearest, 0L, 0L, 0.0);
        }

        boolean isCausalHistorical = tag.equals("pathogen") || tag.equals("sovereignty") ||
                                    tag.equals("technology") || tag.equals("isogloss") ||
                                    tag.equals("kinship") || tag.equals("rituals") ||
                                    tag.equals("institutional");
        if (isCausalHistorical) {
            if (prevYear <= 1491L && nextYear >= 1492L && requestedYear < 1492L) {
                return new TemporalEpochResolution(true, prevYear, 0L, 0L, 0.0);
            }
            if (prevYear <= 1800L && nextYear > 1800L && requestedYear < 1800L) {
                return new TemporalEpochResolution(true, prevYear, 0L, 0L, 0.0);
            }
        }

        double t = (double) (requestedYear - prevYear) / (double) (nextYear - prevYear);
        return new TemporalEpochResolution(false, 0L, prevYear, nextYear, t);
    }

    /**
     * Loads a temporal map image for a given planet, requested year, and layer using default continuous interpolation.
     *
     * @param planet the planet identifier (e.g. "earth")
     * @param requestedYear the simulation epoch
     * @param layerName the canonical or alias layer name
     * @return the resolved JavaFX Image, or null if unavailable
     */
    public static Image loadTemporalMapImage(String planet, long requestedYear, String layerName) {
        return loadTemporalMapImage(planet, requestedYear, layerName, DataFallbackStrategy.CONTINUOUS_INTERPOLATION);
    }

    /**
     * Loads a temporal map image for a given planet, requested year, layer, and explicit data fallback strategy.
     * Performs automatic temporal fallback, causal forward step transitions for historical/epidemiological events,
     * or continuous bilinear cross-epoch interpolation for physical fields.
     *
     * @param planet the planet identifier (e.g. "earth")
     * @param requestedYear the simulation epoch
     * @param layerName the canonical or alias layer name
     * @param strategy the fallback strategy
     * @return the resolved JavaFX Image, or null if unavailable
     */
    public static Image loadTemporalMapImage(String planet, long requestedYear, String layerName, DataFallbackStrategy strategy) {
        if (strategy == null) {
            strategy = DataFallbackStrategy.CONTINUOUS_INTERPOLATION;
        }
        String p = normalizePlanet(planet);
        String tag = canonicalLayerTag(layerName);
        String cacheKey = p + ":" + requestedYear + ":" + tag + ":" + strategy.name();

        synchronized (memoryCache) {
            if (memoryCache.containsKey(cacheKey)) {
                return memoryCache.get(cacheKey);
            }
        }

        TemporalEpochResolution res = resolveTemporalEpoch(p, requestedYear, tag, strategy);
        if (res == null) {
            return null;
        }

        if (res.direct()) {
            return loadExactEpochImage(p, res.directEpoch(), tag, cacheKey);
        }

        Image img0 = loadExactEpochImage(p, res.prevEpoch(), tag, null);
        Image img1 = loadExactEpochImage(p, res.nextEpoch(), tag, null);

        if (img0 == null && img1 == null) return null;
        if (img0 == null) return img1;
        if (img1 == null) return img0;

        Image interpolated = interpolateImages(img0, img1, res.t());
        if (interpolated != null) {
            synchronized (memoryCache) {
                memoryCache.put(cacheKey, interpolated);
            }
        }
        return interpolated;
    }

    /**
     * Loads a headless {@link BufferedImage} for a given planet, requested year, and layer using default continuous interpolation.
     *
     * @param planet the planet identifier (e.g. "earth")
     * @param requestedYear the simulation epoch
     * @param layerName the canonical or alias layer name
     * @return the resolved {@link BufferedImage}, or null if unavailable
     */
    public static BufferedImage loadTemporalMapBufferedImage(String planet, long requestedYear, String layerName) {
        return loadTemporalMapBufferedImage(planet, requestedYear, layerName, DataFallbackStrategy.CONTINUOUS_INTERPOLATION);
    }

    /**
     * Loads a headless {@link BufferedImage} for a given planet, requested year, layer, and explicit fallback strategy.
     *
     * @param planet the planet identifier (e.g. "earth")
     * @param requestedYear the simulation epoch
     * @param layerName the canonical or alias layer name
     * @param strategy the fallback strategy
     * @return the resolved {@link BufferedImage}, or null if unavailable
     */
    public static BufferedImage loadTemporalMapBufferedImage(String planet, long requestedYear, String layerName, DataFallbackStrategy strategy) {
        if (strategy == null) {
            strategy = DataFallbackStrategy.CONTINUOUS_INTERPOLATION;
        }
        String p = normalizePlanet(planet);
        String tag = canonicalLayerTag(layerName);
        String cacheKey = p + ":" + requestedYear + ":" + tag + ":" + strategy.name();

        synchronized (bufferedMemoryCache) {
            if (bufferedMemoryCache.containsKey(cacheKey)) {
                return bufferedMemoryCache.get(cacheKey);
            }
        }

        TemporalEpochResolution res = resolveTemporalEpoch(p, requestedYear, tag, strategy);
        if (res == null) {
            return null;
        }

        if (res.direct()) {
            return loadExactEpochBufferedImage(p, res.directEpoch(), tag, cacheKey);
        }

        BufferedImage img0 = loadExactEpochBufferedImage(p, res.prevEpoch(), tag, null);
        BufferedImage img1 = loadExactEpochBufferedImage(p, res.nextEpoch(), tag, null);

        if (img0 == null && img1 == null) return null;
        if (img0 == null) return img1;
        if (img1 == null) return img0;

        BufferedImage interpolated = interpolateBufferedImages(img0, img1, res.t());
        if (interpolated != null) {
            synchronized (bufferedMemoryCache) {
                bufferedMemoryCache.put(cacheKey, interpolated);
            }
        }
        return interpolated;
    }

    /**
     * Loads an exact epoch image with multi-pattern fallback as a JavaFX {@link Image}.
     *
     * @param planet the planet identifier
     * @param epoch the target epoch year
     * @param layerTag the canonical layer tag
     * @param cacheKey the optional cache key (or null)
     * @return the loaded JavaFX Image, or null if not found on disk
     */
    public static Image loadExactEpochImage(String planet, long epoch, String layerTag, String cacheKey) {
        String p = normalizePlanet(planet);
        String tag = canonicalLayerTag(layerTag);
        File epochDir = org.ether.society.config.EtherPaths.getEtherDir().resolve(p).resolve(String.valueOf(epoch)).toFile(); if (!epochDir.exists()) epochDir = new File("data/maps/ether/" + p + "/" + epoch); String dirPath = epochDir.getAbsolutePath() + File.separator;

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
     * Loads an exact epoch image with multi-pattern fallback as a standard headless {@link BufferedImage}.
     *
     * @param planet the planet identifier
     * @param epoch the target epoch year
     * @param layerTag the canonical layer tag
     * @param cacheKey the optional cache key (or null)
     * @return the loaded BufferedImage, or null if not found on disk
     */
    public static BufferedImage loadExactEpochBufferedImage(String planet, long epoch, String layerTag, String cacheKey) {
        String p = normalizePlanet(planet);
        String tag = canonicalLayerTag(layerTag);
        File epochDir = org.ether.society.config.EtherPaths.getEtherDir().resolve(p).resolve(String.valueOf(epoch)).toFile(); if (!epochDir.exists()) epochDir = new File("data/maps/ether/" + p + "/" + epoch); String dirPath = epochDir.getAbsolutePath() + File.separator;

        String[] candidateNames = {
            p + "_" + epoch + "_" + tag + ".png",
            p + "_" + tag + ".png",
            tag + ".png"
        };

        for (String cName : candidateNames) {
            File f = new File(dirPath + cName);
            if (f.exists() && f.isFile()) {
                try {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) {
                        if (cacheKey != null) {
                            synchronized (bufferedMemoryCache) {
                                bufferedMemoryCache.put(cacheKey, img);
                            }
                        }
                        return img;
                    }
                } catch (Exception e) {
                    logger.warn("Could not read epoch BufferedImage file: {}", f.getAbsolutePath());
                }
            }
        }
        return null;
    }

    /**
     * Performs linear cross-fade blending between two headless {@link BufferedImage} scalar raster maps.
     *
     * @param img0 the earlier baseline raster
     * @param img1 the later target raster
     * @param t    the interpolation factor in [0.0, 1.0]
     * @return the interpolated {@link BufferedImage}
     */
    public static BufferedImage interpolateBufferedImages(BufferedImage img0, BufferedImage img1, double t) {
        return TemporalRasterInterpolator.interpolateBufferedImages(img0, img1, t);
    }

    /**
     * Performs linear cross-fade blending between two JavaFX {@link Image} raster maps.
     *
     * @param img0 the earlier baseline raster
     * @param img1 the later target raster
     * @param t    the interpolation factor in [0.0, 1.0]
     * @return the interpolated JavaFX WritableImage
     */
    public static Image interpolateImages(Image img0, Image img1, double t) {
        return TemporalRasterInterpolator.interpolateImages(img0, img1, t);
    }

    /**
     * Clears all cached in-memory raster textures.
     */
    public static void clearCache() {
        synchronized (memoryCache) {
            memoryCache.clear();
        }
        synchronized (bufferedMemoryCache) {
            bufferedMemoryCache.clear();
        }
    }
}

