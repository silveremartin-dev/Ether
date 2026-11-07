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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Historical empire core coordinates and paleolithic cultural seed catalog across epochs.
 * Externalized from Java code to JSON provenance dataset:
 * {@code data/history/ether/earth/paleo/historical_empire_cores.json}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class HistoricalEmpireCoresCatalog {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalEmpireCoresCatalog.class);
    private static final String JSON_PATH = "data/history/ether/earth/paleo/historical_empire_cores.json";
    private static final NavigableMap<Long, List<double[]>> EPOCHS_CACHE = new TreeMap<>();

    static {
        loadCatalog();
    }

    private HistoricalEmpireCoresCatalog() {
    }

    private static synchronized void loadCatalog() {
        EPOCHS_CACHE.clear();
        ObjectMapper mapper = EtherJsonUtil.getMapper();
        JsonNode root = null;

        try {
            Path file = Paths.get(JSON_PATH);
            if (Files.exists(file)) {
                root = mapper.readTree(file.toFile());
            } else {
                InputStream is = HistoricalEmpireCoresCatalog.class.getClassLoader().getResourceAsStream(JSON_PATH);
                if (is != null) {
                    root = mapper.readTree(is);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not load externalized empire cores catalog from {}: {}", JSON_PATH, e.getMessage());
        }

        if (root != null && root.has("epochs") && root.get("epochs").isArray()) {
            for (JsonNode epochNode : root.get("epochs")) {
                long maxYear = epochNode.path("maxYear").asLong();
                List<double[]> cores = new ArrayList<>();
                JsonNode coresArr = epochNode.path("cores");
                if (coresArr.isArray()) {
                    for (JsonNode c : coresArr) {
                        double lon = c.path("lon").asDouble();
                        double lat = c.path("lat").asDouble();
                        String colorStr = c.path("color").asText("0x000000");
                        double colorVal;
                        try {
                            colorVal = Long.decode(colorStr);
                        } catch (NumberFormatException nfe) {
                            colorVal = 0.0;
                        }
                        double sigma = c.path("sigma").asDouble(20.0);
                        cores.add(new double[]{lon, lat, colorVal, sigma});
                    }
                }
                EPOCHS_CACHE.put(maxYear, Collections.unmodifiableList(cores));
            }
            logger.info("Loaded {} historical empire core epochs from {}", EPOCHS_CACHE.size(), JSON_PATH);
        } else {
            logger.warn("Empire cores JSON dataset empty or unreadable at {}", JSON_PATH);
        }
    }

    /**
     * Retrieves the list of empirical civilization cores and sovereign seeds for the specified historical year.
     *
     * @param year historical calendar year (negative for BCE / BP)
     * @return immutable list of empire core vectors [longitude, latitude, colorRGB, radiusSigma]
     */
    public static List<double[]> getEmpireCoresForYear(long year) {
        if (year <= -70000L) {
            return Collections.emptyList();
        }
        Map.Entry<Long, List<double[]>> entry = EPOCHS_CACHE.ceilingEntry(year);
        if (entry != null) {
            return entry.getValue();
        }
        Map.Entry<Long, List<double[]>> last = EPOCHS_CACHE.lastEntry();
        return last != null ? last.getValue() : Collections.emptyList();
    }
}
