/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import org.ether.society.model.Scenario;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File system resolver and raster caching repository for 2D cartographic tensors.
 *
 * <p>Resolves empirical historical rasters on disk (e.g. NOAA/GEBCO layers in
 * {@code data/ether/earth/<year>/}), canonicalizes tensor channel descriptors,
 * and maintains an in-memory high performance cache.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class SpatialTensorDiskCatalog {

    /**
     * Cached available historical Earth epoch directories for \(O(\log N)\) binary search lookup.
     */
    public static final int[] AVAILABLE_EARTH_EPOCHS;

    static {
        File earthDir = new File("data/ether/earth/");
        if (earthDir.exists() && earthDir.isDirectory()) {
            File[] sub = earthDir.listFiles(File::isDirectory);
            if (sub != null) {
                List<Integer> yrs = new ArrayList<>();
                for (File d : sub) {
                    try {
                        yrs.add(Integer.parseInt(d.getName()));
                    } catch (NumberFormatException ignored) {}
                }
                Collections.sort(yrs);
                AVAILABLE_EARTH_EPOCHS = yrs.stream().mapToInt(Integer::intValue).toArray();
            } else {
                AVAILABLE_EARTH_EPOCHS = new int[0];
            }
        } else {
            AVAILABLE_EARTH_EPOCHS = new int[0];
        }
    }

    /**
     * High-performance concurrent cache for 2D cartographic rasters.
     */
    public static final Map<String, BufferedImage> RASTER_IMAGE_CACHE = new ConcurrentHashMap<>();

    private SpatialTensorDiskCatalog() {
        // Utility class
    }

    /**
     * Resolves human-readable or localized layer names into canonical disk layer keys.
     *
     * @param channel raw channel descriptor
     * @return canonical key (e.g., "density", "technology", "temperature")
     */
    public static String resolveCanonicalChannelKey(String channel) {
        if (channel == null || channel.isBlank()) {
            return "density";
        }
        String ch = channel.trim();
        if (ch.contains("(") && ch.contains(")")) {
            int openIdx = ch.lastIndexOf('(');
            int closeIdx = ch.lastIndexOf(')');
            if (openIdx >= 0 && closeIdx > openIdx) {
                String candidate = ch.substring(openIdx + 1, closeIdx).trim().toLowerCase(Locale.ROOT);
                if (candidate.startsWith("custom_tensor_") || candidate.startsWith("tensor_")) {
                    return candidate.replaceAll("[^a-z0-9_]+", "_");
                }
            }
        }
        String chLower = ch.toLowerCase(Locale.ROOT);
        if (chLower.contains("technolog") || chLower.contains("outillage") || chLower.contains("ç§‘æŠ€")) return "technology";
        if (chLower.contains("tempÃ©rat") || chLower.contains("temperat") || chLower.contains("climat") || chLower.contains("æ°”æ¸©")) return "temperature";
        if (chLower.contains("aquif") || chLower.contains("eau") || chLower.contains("water") || chLower.contains("å«æ°´å±‚")) return "aquifers";
        if (chLower.contains("biom") || chLower.contains("agricul") || chLower.contains("biomass") || chLower.contains("å†œä¸š")) return "biomes";
        if (chLower.contains("souverain") || chLower.contains("sovereign") || chLower.contains("politi") || chLower.contains("fronti") || chLower.contains("border") || chLower.contains("ä¸»æƒ")) return "sovereignty";
        if (chLower.contains("isogloss") || chLower.contains("linguist") || chLower.contains("lang") || chLower.contains("è¯­è¨€")) return "isogloss";
        if (chLower.contains("parent") || chLower.contains("kinship") || chLower.contains("famill") || chLower.contains("äº²å±ž")) return "kinship";
        if (chLower.contains("rituel") || chLower.contains("ritual") || chLower.contains("croyan") || chLower.contains("belief") || chLower.contains("ä¿¡ä»°")) return "rituals";
        if (chLower.contains("commerc") || chLower.contains("trade") || chLower.contains("route") || chLower.contains("è´¸æ˜“")) return "tradenetwork";
        if (chLower.contains("institut") || chLower.contains("seshat") || chLower.contains("åˆ¶åº¦")) return "institutional";
        if (chLower.contains("Ã©colog") || chLower.contains("ecolog") || chLower.contains("empreint") || chLower.contains("footprint") || chLower.contains("ç”Ÿæ€")) return "ecological";
        if (chLower.contains("pathog") || chLower.contains("sanit") || chLower.contains("immunit") || chLower.contains("health") || chLower.contains("zoonot") || chLower.contains("ç—…åŽŸ")) return "pathogen";
        if (chLower.contains("prÃ©cipit") || chLower.contains("precipit") || chLower.contains("pluie") || chLower.contains("rainfall") || chLower.contains("é™æ°´")) return "precipitation";
        if (chLower.contains("Ã©lÃ©vat") || chLower.contains("elevat") || chLower.contains("altitude") || chLower.contains("relief") || chLower.contains("æµ·æ‹”")) return "elevation";
        if (chLower.contains("saison") || chLower.contains("season") || chLower.contains("å­£èŠ‚")) return "seasonality";

        String customSlug = chLower.replaceAll("[^a-z0-9_]+", "_").replaceAll("^_+|_+$", "");
        return customSlug.isBlank() ? "density" : customSlug;
    }

    /**
     * Finds the matching empirical or generated raster file on disk for a given year and channel.
     *
     * @param year historical calendar year
     * @param channel tensor channel name
     * @return File handle if found on disk, or null
     */
    public static File getDiskRasterForChannel(int year, String channel) {
        String baseMapDir = "data/ether/earth/" + year + "/";
        String channelKey = resolveCanonicalChannelKey(channel);
        File rasterFile = new File(baseMapDir, "earth_" + year + "_" + channelKey + ".png");
        if (rasterFile.exists()) return rasterFile;

        File dir = new File(baseMapDir);
        if (dir.exists() && dir.isDirectory()) {
            File[] matches = dir.listFiles((d, name) -> {
                String n = name.toLowerCase(Locale.ROOT);
                return n.endsWith(".png") && (n.contains(channelKey) || n.endsWith("_" + channelKey + ".png"));
            });
            if (matches != null && matches.length > 0) {
                return matches[0];
            }
        }

        if (AVAILABLE_EARTH_EPOCHS.length > 0) {
            int idx = Arrays.binarySearch(AVAILABLE_EARTH_EPOCHS, year);
            int closestYear;
            if (idx >= 0) {
                closestYear = AVAILABLE_EARTH_EPOCHS[idx];
            } else {
                int ins = -idx - 1;
                if (ins == 0) {
                    closestYear = AVAILABLE_EARTH_EPOCHS[0];
                } else if (ins >= AVAILABLE_EARTH_EPOCHS.length) {
                    closestYear = AVAILABLE_EARTH_EPOCHS[AVAILABLE_EARTH_EPOCHS.length - 1];
                } else {
                    int prev = AVAILABLE_EARTH_EPOCHS[ins - 1];
                    int next = AVAILABLE_EARTH_EPOCHS[ins];
                    closestYear = (Math.abs(prev - year) <= Math.abs(next - year)) ? prev : next;
                }
            }
            File candidate = new File("data/ether/earth/" + closestYear + "/earth_" + closestYear + "_" + channelKey + ".png");
            if (candidate.exists()) return candidate;

            File closestDir = new File("data/ether/earth/" + closestYear + "/");
            if (closestDir.exists() && closestDir.isDirectory()) {
                File[] matches = closestDir.listFiles((d, name) -> {
                    String n = name.toLowerCase(Locale.ROOT);
                    return n.endsWith(".png") && (n.contains(channelKey) || n.endsWith("_" + channelKey + ".png"));
                });
                if (matches != null && matches.length > 0) {
                    return matches[0];
                }
            }
        }
        return null;
    }

    /**
     * Extracts Base64 raster data for a specific channel from a Scenario instance.
     *
     * @param sc scenario instance
     * @param channel target channel
     * @return base64-encoded PNG string or null
     */
    public static String extractChannelBase64(Scenario sc, String channel) {
        if (sc == null || channel == null) return null;
        String chLower = channel.toLowerCase(Locale.ROOT);
        if (chLower.contains("souverainetÃ©") || chLower.contains("sovereignty")) return sc.getCustomTensorMapBase64(3);
        if (chLower.contains("isoglosses") || chLower.contains("linguistique") || chLower.contains("linguistic") || chLower.contains("langues")) return sc.getCustomTensorMapBase64(0);
        if (chLower.contains("parentÃ©") || chLower.contains("kinship")) return sc.getCustomTensorMapBase64(1);
        if (chLower.contains("rituels") || chLower.contains("rituals") || chLower.contains("croyances")) return sc.getCustomTensorMapBase64(2);
        if (chLower.contains("technologie") || chLower.contains("technology") || chLower.contains("artefacts") || chLower.contains("outillage")) return sc.getCustomTensorMapBase64(4);
        if (chLower.contains("commerce") || chLower.contains("trade") || chLower.contains("corridors")) return sc.getCustomTensorMapBase64(5);
        if (chLower.contains("institution") || chLower.contains("institutional") || chLower.contains("seshat")) return sc.getCustomTensorMapBase64(6);
        if (chLower.contains("Ã©cologique") || chLower.contains("ecological") || chLower.contains("empreinte")) return sc.getCustomTensorMapBase64(7);
        if (chLower.contains("pathogÃ¨ne") || chLower.contains("pathogen") || chLower.contains("immunitÃ©") || chLower.contains("santÃ©")) return sc.getCustomTensorMapBase64(8);
        return sc.getCustomDensityBase64();
    }

    /**
     * Clears in-memory raster cache.
     */
    public static void clearRasterCache() {
        RASTER_IMAGE_CACHE.clear();
    }
}

