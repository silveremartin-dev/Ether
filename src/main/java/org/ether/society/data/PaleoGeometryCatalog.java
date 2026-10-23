/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ether.society.persistence.EtherJsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * On-demand Paleogeographical & Archaeological Geometry Loader for Ether.
 * 
 * <p>Provides decoupled, dynamic ingestion of empirical archaeological hearths,
 * Quaternary continental ice sheets, and paleolithic migration arteries from
 * lightweight JSON / GeoJSON assets (stored in {@code data/history/paleo/} or packaged
 * in classpath resources).</p>
 *
 * <p>Replaces monolithic hardcoded coordinate matrices with lazy, cached data loaders
 * adhering to the Single Responsibility Principle (SRP) and Cartographic Tensor Provenance standards.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class PaleoGeometryCatalog {

    private static final Logger logger = LoggerFactory.getLogger(PaleoGeometryCatalog.class);
    private static final ObjectMapper mapper = EtherJsonUtil.getMapper();

    private static final Map<String, double[][]> HEARTHS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, double[][]> ICE_SHEETS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, double[][]> ROUTES_CACHE = new ConcurrentHashMap<>();

    private PaleoGeometryCatalog() {
        // Static utility catalog
    }

    /**
     * Loads all archaeological hominin hearth coordinates on demand.
     *
     * @return unmodifiable map of hearth identifier to coordinate array [N][2] {lon, lat}
     */
    public static Map<String, double[][]> loadHomininHearths() {
        if (HEARTHS_CACHE.isEmpty()) {
            synchronized (HEARTHS_CACHE) {
                if (HEARTHS_CACHE.isEmpty()) {
                    loadHearthsInternal();
                }
            }
        }
        return Collections.unmodifiableMap(HEARTHS_CACHE);
    }

    /**
     * Loads a specific hominin hearth group by key.
     *
     * @param key hearth identifier (e.g. "HEARTHS_SAPIENS_AFRICA")
     * @return coordinate array [N][2] {lon, lat}, or empty array if not found
     */
    public static double[][] loadHearth(String key) {
        return loadHomininHearths().getOrDefault(key, new double[0][0]);
    }

    /**
     * Loads all Quaternary ice sheet boundary polygons on demand.
     * Applies Chaikin smoothing dynamically according to metadata iterations.
     *
     * @return unmodifiable map of ice sheet identifier to smoothed polygon coordinates [N][2] {lon, lat}
     */
    public static Map<String, double[][]> loadIceSheets() {
        if (ICE_SHEETS_CACHE.isEmpty()) {
            synchronized (ICE_SHEETS_CACHE) {
                if (ICE_SHEETS_CACHE.isEmpty()) {
                    loadIceSheetsInternal();
                }
            }
        }
        return Collections.unmodifiableMap(ICE_SHEETS_CACHE);
    }

    /**
     * Loads a specific ice sheet polygon by key.
     *
     * @param key ice sheet identifier (e.g. "POLY_LAURENTIDE_LGM")
     * @return coordinate array [N][2] {lon, lat}, or empty array if not found
     */
    public static double[][] loadIceSheet(String key) {
        return loadIceSheets().getOrDefault(key, new double[0][0]);
    }

    /**
     * Loads all paleolithic migration corridor polylines on demand.
     *
     * @return unmodifiable map of route identifier to polyline coordinates [M][2] {lon, lat}
     */
    public static Map<String, double[][]> loadMigrationRoutes() {
        if (ROUTES_CACHE.isEmpty()) {
            synchronized (ROUTES_CACHE) {
                if (ROUTES_CACHE.isEmpty()) {
                    loadRoutesInternal();
                }
            }
        }
        return Collections.unmodifiableMap(ROUTES_CACHE);
    }

    /**
     * Loads a specific migration corridor polyline by key.
     *
     * @param key route identifier (e.g. "ROUTE_PACIFIC_KELP_HIGHWAY")
     * @return polyline coordinate array [M][2] {lon, lat}, or empty array if not found
     */
    public static double[][] loadRoute(String key) {
        return loadMigrationRoutes().getOrDefault(key, new double[0][0]);
    }

    // =========================================================================
    // INTERNAL PARSING ROUTINES
    // =========================================================================

    private static void loadHearthsInternal() {
        JsonNode root = readJsonResource("hominin_hearths.json");
        if (root != null && root.has("hearths")) {
            JsonNode hearths = root.get("hearths");
            hearths.fieldNames().forEachRemaining(key -> {
                JsonNode arr = hearths.get(key);
                HEARTHS_CACHE.put(key, parseCoordinateArray(arr));
            });
            logger.info("Loaded {} archaeological hominin hearths from external JSON dataset.", HEARTHS_CACHE.size());
        }
    }

    private static void loadIceSheetsInternal() {
        JsonNode root = readJsonResource("ice_sheets.json");
        if (root != null && root.has("iceSheets")) {
            JsonNode sheets = root.get("iceSheets");
            sheets.fieldNames().forEachRemaining(key -> {
                JsonNode sheetNode = sheets.get(key);
                JsonNode coordsNode = sheetNode.has("coordinates") ? sheetNode.get("coordinates") : sheetNode;
                int iterations = sheetNode.has("iterations") ? sheetNode.get("iterations").asInt(2) : 2;
                double[][] raw = parseCoordinateArray(coordsNode);
                double[][] smoothed = PaleoGeographyCoordinatesCatalog.chaikinSmoothPolygon(raw, iterations);
                ICE_SHEETS_CACHE.put(key, smoothed);
            });
            logger.info("Loaded {} Quaternary ice sheet polygons with dynamic Chaikin smoothing from JSON dataset.", ICE_SHEETS_CACHE.size());
        }
    }

    private static void loadRoutesInternal() {
        JsonNode root = readJsonResource("migration_routes.json");
        if (root != null && root.has("routes")) {
            JsonNode routes = root.get("routes");
            routes.fieldNames().forEachRemaining(key -> {
                JsonNode arr = routes.get(key);
                ROUTES_CACHE.put(key, parseCoordinateArray(arr));
            });
            logger.info("Loaded {} paleolithic migration routes from external JSON dataset.", ROUTES_CACHE.size());
        }
    }

    private static double[][] parseCoordinateArray(JsonNode arr) {
        if (arr == null || !arr.isArray()) return new double[0][0];
        int size = arr.size();
        double[][] result = new double[size][2];
        for (int i = 0; i < size; i++) {
            JsonNode item = arr.get(i);
            if (item != null && item.isArray() && item.size() >= 2) {
                result[i][0] = item.get(0).asDouble();
                result[i][1] = item.get(1).asDouble();
            }
        }
        return result;
    }

    private static JsonNode readJsonResource(String filename) {
        // 1. Check local file on disk in data/history/paleo/
        File diskFile = new File("data/history/paleo", filename);
        if (diskFile.exists() && diskFile.isFile()) {
            try {
                return mapper.readTree(diskFile);
            } catch (Exception ex) {
                logger.warn("Failed reading paleogeography file from disk '{}': {}", diskFile.getAbsolutePath(), ex.getMessage());
            }
        }

        // 2. Fallback to classpath resource
        String resPath = "/data/history/paleo/" + filename;
        try (InputStream is = PaleoGeometryCatalog.class.getResourceAsStream(resPath)) {
            if (is != null) {
                return mapper.readTree(is);
            }
        } catch (Exception ex) {
            logger.warn("Failed reading paleogeography classpath resource '{}': {}", resPath, ex.getMessage());
        }

        logger.error("Could not locate paleogeographical JSON dataset: {}", filename);
        return null;
    }
}
