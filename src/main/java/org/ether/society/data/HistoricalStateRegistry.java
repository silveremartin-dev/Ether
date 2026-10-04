/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * Copyright (c) 2024 Gemini AI Assistant
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */
package org.ether.society.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Piecewise-Constant Historical State Registry.
 *
 * <p>Loads {@code data/history/treaties_and_transitions.json} at first use and provides
 * O(log N) lookup of the geopolitical epoch active at any requested year.
 *
 * <p><b>Design contract:</b> NO linear interpolation between intervals.
 * The function {@code getActiveEpoch(year)} returns the <em>step-constant</em> sovereign
 * configuration of interval {@code [t_start, t_end)} that brackets {@code year}.
 * If the year falls before the earliest interval, the earliest interval is returned.
 * If it falls after the last interval, the last interval is returned.
 *
 * <p>This matches the user requirement: transitions are discrete events (wars, treaties,
 * revolutions) that change the world state abruptly, not gradually.
 *
 * <p><b>Singleton pattern with lazy initialisation.</b> The registry is loaded once and
 * cached in a static field, enabling zero-latency subsequent queries.
 *
 * @author Silvere Martin-Michiellot &amp; Gemini AI
 * @version 1.0.0
 * @see <a href="data/history/treaties_and_transitions.json">treaties_and_transitions.json</a>
 */
public class HistoricalStateRegistry {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalStateRegistry.class);

    /* Primary path relative to the working directory (Maven project root). */
    private static final String PRIMARY_PATH = "data/history/treaties_and_transitions.json";

    /* Fallback for classpath resources during unit tests. */
    private static final String CLASSPATH_RESOURCE = "/treaties_and_transitions.json";

    // -------------------------------------------------------------------------
    // Public data structures
    // -------------------------------------------------------------------------

    /*
     * Immutable snapshot of a single geopolitical polity as defined in the registry.
     */
    public record PolityState(
            String name,
            double coreLon,
            double coreLat,
            int colorRgb,
            double radiusDeg
    ) {}

    /*
     * Immutable geopolitical epoch covering the half-open interval {@code [tStart, tEnd)}.
     *
     * <p>{@code tStart} and {@code tEnd} are expressed as years CE (negative = BCE).
     * {@code tEnd == Long.MAX_VALUE} for the terminal open interval.
     */
    public record HistoricalEpoch(
            String id,
            long tStart,
            long tEnd,
            String label,
            String triggerEvent,
            List<PolityState> polities
    ) {}

    // -------------------------------------------------------------------------
    // Singleton registry
    // -------------------------------------------------------------------------

    /*
     * Sorted list of all epochs by {@code tStart} ascending.
     * Populated lazily on first access.
     */
    private static volatile List<HistoricalEpoch> REGISTRY = null;
    private static final Object LOCK = new Object();

    /*
     * Returns the active geopolitical epoch for the given year (step-constant lookup).
     *
     * <p>The epoch whose interval {@code [t_start, t_end)} brackets {@code year} is returned.
     * Years before the earliest known epoch return the earliest epoch.
     * Years after the last epoch return the last epoch.
     *
     * @param year year CE (negative for BCE, e.g. {@code -3000} = 3000 BCE)
     * @return the active {@link HistoricalEpoch}, never {@code null}
     */
    public static HistoricalEpoch getActiveEpoch(long year) {
        List<HistoricalEpoch> registry = getRegistry();
        if (registry.isEmpty()) {
            throw new IllegalStateException("HistoricalStateRegistry: no epochs loaded — check " + PRIMARY_PATH);
        }

        // Binary search: find last epoch whose tStart <= year
        int lo = 0, hi = registry.size() - 1, best = 0;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (registry.get(mid).tStart() <= year) {
                best = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        HistoricalEpoch candidate = registry.get(best);
        // The candidate interval covers [tStart, tEnd): verify tEnd bound
        // If year >= tEnd, fall through to next (but due to binary search this should not happen
        // unless data gaps exist — return candidate defensively).
        return candidate;
    }

    /*
     * Returns all loaded epochs in chronological order.
     *
     * @return unmodifiable sorted list of {@link HistoricalEpoch}
     */
    public static List<HistoricalEpoch> getAllEpochs() {
        return Collections.unmodifiableList(getRegistry());
    }

    /*
     * Returns all epochs whose interval overlaps the range {@code [yearFrom, yearTo]}.
     *
     * @param yearFrom start of query range (inclusive)
     * @param yearTo   end of query range (inclusive)
     * @return list of overlapping epochs, may be empty
     */
    public static List<HistoricalEpoch> getEpochsInRange(long yearFrom, long yearTo) {
        List<HistoricalEpoch> registry = getRegistry();
        List<HistoricalEpoch> result = new ArrayList<>();
        for (HistoricalEpoch e : registry) {
            if (e.tStart() <= yearTo && e.tEnd() > yearFrom) {
                result.add(e);
            }
        }
        return result;
    }

    /*
     * Retrieves the polity configuration active at {@code year} as a list of
     * {@link OrographicGlottologPropagator.CulturalSeed} objects, ready for cost-distance
     * propagation.
     *
     * <p>This provides a direct integration point between the registry and the
     * orographic sovereignty rasterizer.
     *
     * @param year year to query
     * @return list of cultural seeds derived from the active epoch's polity states
     */
    public static List<OrographicGlottologPropagator.CulturalSeed> getActiveSovereigntySeeds(long year) {
        HistoricalEpoch epoch = getActiveEpoch(year);
        List<OrographicGlottologPropagator.CulturalSeed> seeds = new ArrayList<>();
        for (PolityState p : epoch.polities()) {
            // expansionWeight proportional to radius — larger states propagate further
            double weight = p.radiusDeg() / 18.0;
            seeds.add(new OrographicGlottologPropagator.CulturalSeed(
                    p.coreLon(), p.coreLat(), p.colorRgb(), weight, p.name()
            ));
        }
        return seeds;
    }

    // -------------------------------------------------------------------------
    // Internal loading
    // -------------------------------------------------------------------------

    private static List<HistoricalEpoch> getRegistry() {
        if (REGISTRY == null) {
            synchronized (LOCK) {
                if (REGISTRY == null) {
                    REGISTRY = loadRegistry();
                }
            }
        }
        return REGISTRY;
    }

    private static List<HistoricalEpoch> loadRegistry() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;

        // 1. Try filesystem path (runtime / Maven execution)
        Path fsPath = Path.of(PRIMARY_PATH);
        if (Files.exists(fsPath)) {
            try {
                root = mapper.readTree(fsPath.toFile());
                logger.info("HistoricalStateRegistry: loaded from filesystem: {}", fsPath.toAbsolutePath());
            } catch (IOException e) {
                logger.warn("HistoricalStateRegistry: filesystem read failed for {}: {}", fsPath, e.getMessage());
            }
        }

        // 2. Fallback: classpath resource (unit tests)
        if (root == null) {
            try (InputStream is = HistoricalStateRegistry.class.getResourceAsStream(CLASSPATH_RESOURCE)) {
                if (is != null) {
                    root = mapper.readTree(is);
                    logger.info("HistoricalStateRegistry: loaded from classpath resource {}", CLASSPATH_RESOURCE);
                }
            } catch (IOException e) {
                logger.warn("HistoricalStateRegistry: classpath read failed: {}", e.getMessage());
            }
        }

        if (root == null) {
            logger.error("HistoricalStateRegistry: FATAL — could not load treaties_and_transitions.json from {} or classpath", PRIMARY_PATH);
            return List.of();
        }

        // Parse transitions array
        List<HistoricalEpoch> epochs = new ArrayList<>();
        JsonNode transitions = root.path("transitions");
        if (!transitions.isArray()) {
            logger.error("HistoricalStateRegistry: 'transitions' key missing or not an array");
            return List.of();
        }

        for (JsonNode t : transitions) {
            try {
                String id = t.path("id").asText("UNKNOWN");
                long tStart = t.path("t_start").asLong();
                long tEnd = t.has("t_end") ? t.path("t_end").asLong() : Long.MAX_VALUE;
                String label = t.path("label").asText("");
                String trigger = t.path("trigger_event").asText("");

                List<PolityState> polities = new ArrayList<>();
                JsonNode pArr = t.path("polity_states");
                if (pArr.isArray()) {
                    for (JsonNode p : pArr) {
                        String name = p.path("name").asText("");
                        double lon = p.path("core_lon").asDouble(0.0);
                        double lat = p.path("core_lat").asDouble(0.0);
                        String colorHex = p.path("color").asText("#888888");
                        int rgb = parseHexColor(colorHex);
                        double radius = p.path("radius_deg").asDouble(10.0);
                        polities.add(new PolityState(name, lon, lat, rgb, radius));
                    }
                }

                epochs.add(new HistoricalEpoch(id, tStart, tEnd, label, trigger,
                        Collections.unmodifiableList(polities)));
            } catch (Exception ex) {
                logger.warn("HistoricalStateRegistry: error parsing transition node: {}", ex.getMessage());
            }
        }

        // Sort by tStart ascending
        epochs.sort(Comparator.comparingLong(HistoricalEpoch::tStart));

        // Patch tEnd gaps: if t_end of epoch[i] == t_start of epoch[i+1], they are already
        // correct. Otherwise patch open-ended intervals.
        for (int i = 0; i < epochs.size() - 1; i++) {
            HistoricalEpoch cur = epochs.get(i);
            HistoricalEpoch next = epochs.get(i + 1);
            if (cur.tEnd() > next.tStart()) {
                // Overlap — clamp cur.tEnd to next.tStart
                epochs.set(i, new HistoricalEpoch(cur.id(), cur.tStart(), next.tStart(),
                        cur.label(), cur.triggerEvent(), cur.polities()));
            }
        }

        logger.info("HistoricalStateRegistry: loaded {} epochs spanning {} to {} CE",
                epochs.size(),
                epochs.isEmpty() ? "N/A" : epochs.get(0).tStart(),
                epochs.isEmpty() ? "N/A" : epochs.get(epochs.size() - 1).tEnd());

        return Collections.unmodifiableList(epochs);
    }

    /*
     * Parses a CSS hex color string (e.g. {@code "#D97706"} or {@code "D97706"}) to an int RGB.
     */
    static int parseHexColor(String hex) {
        if (hex == null || hex.isBlank()) return 0x888888;
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return (int) Long.parseLong(h, 16);
        } catch (NumberFormatException e) {
            return 0x888888;
        }
    }
}
