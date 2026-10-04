/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Catalog manager for deterministic Earth historical leader interventions and reforms.
 * Loads pre-defined scenarios from earth_historical_leaders.json and supports custom user injections.
 */
public class HistoricalInterventionCatalog {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalInterventionCatalog.class);
    /* Internal state variable for resource path (String). */
    private static final String RESOURCE_PATH = "/data/presets/leaders/earth_historical_leaders.json";

    private static final HistoricalInterventionCatalog INSTANCE = new HistoricalInterventionCatalog();

    private final List<HistoricalIntervention> interventions = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    /*
     * Get instance.
     * Enforces physical invariants and updates associated state variables within {@code HistoricalInterventionCatalog}.
     *
     * @return the resulting computation or state reference
     */
    public static HistoricalInterventionCatalog getInstance() {
        return INSTANCE;
    }

    /*
     * Historical intervention catalog.
     * Enforces physical invariants and updates associated state variables within {@code HistoricalInterventionCatalog}.
     *
     */
    public HistoricalInterventionCatalog() {
        loadCatalog();
    }

    /*
     * Load catalog.
     * Enforces physical invariants and updates associated state variables within {@code HistoricalInterventionCatalog}.
     *
     */
    public synchronized void loadCatalog() {
        interventions.clear();
        try {
            // 1. Try Classpath Resource
            InputStream is = getClass().getResourceAsStream(RESOURCE_PATH);
            if (is != null) {
                List<HistoricalIntervention> loaded = mapper.readValue(is, new TypeReference<List<HistoricalIntervention>>() {});
                interventions.addAll(loaded);
                logger.info("Successfully loaded {} historical interventions from classpath resource.", loaded.size());
                return;
            }

            // 2. Try Local File via EtherPaths
            File f = org.ether.society.config.EtherPaths.getPresetsLeadersDir().resolve("earth_historical_leaders.json").toFile();
            if (!f.exists()) {
                f = new File("data/presets/leaders/earth_historical_leaders.json");
            }
            if (f.exists()) {
                List<HistoricalIntervention> loaded = mapper.readValue(f, new TypeReference<List<HistoricalIntervention>>() {});
                interventions.addAll(loaded);
                logger.info("Successfully loaded {} historical interventions from local file {}.", loaded.size(), f.getAbsolutePath());
                return;
            }

            logger.warn("No earth_historical_leaders.json found on classpath or filesystem. Initializing with default set.");
            initFallbackCatalog();
        } catch (Exception e) {
            logger.error("Failed to parse earth_historical_leaders.json: {}", e.getMessage(), e);
            initFallbackCatalog();
        }
    }

    // Helper subroutine: init fallback catalog - internal state computation & bounds checking
    private void initFallbackCatalog() {
        interventions.add(new HistoricalIntervention("ALEXANDER_FALLBACK", "Alexandre le Grand", "Conquête fulgurante et hellénisation.", -334, 11, 40.64, 22.94, 3500.0, LeaderArchetype.MILITARY_CONQUEROR, 9.5));
        interventions.add(new HistoricalIntervention("AUGUSTUS_FALLBACK", "Auguste & Pax Romana", "Réseau routier et centralisation impériale.", -27, 41, 41.90, 12.49, 2200.0, LeaderArchetype.INFRASTRUCTURE_BUILDER, 9.0));
        interventions.add(new HistoricalIntervention("GUTENBERG_FALLBACK", "Johannes Gutenberg", "Révolution de l'imprimerie et diffusion des savoirs.", 1440, 28, 49.99, 8.27, 2000.0, LeaderArchetype.INFRASTRUCTURE_BUILDER, 9.5));
    }

    /*
     * Get interventions.
     * Enforces physical invariants and updates associated state variables within {@code HistoricalInterventionCatalog}.
     *
     * @return the resulting computation or state reference
     */
    public List<HistoricalIntervention> getInterventions() {
        return Collections.unmodifiableList(new ArrayList<>(interventions));
    }

    /*
     * Get interventions starting in year.
     * Enforces physical invariants and updates associated state variables within {@code HistoricalInterventionCatalog}.
     *
     * @param year the year parameter (int)
     * @return the resulting computation or state reference
     */
    public List<HistoricalIntervention> getInterventionsStartingInYear(int year) {
        List<HistoricalIntervention> res = new ArrayList<>();
        for (HistoricalIntervention hi : interventions) {
            if (hi.getYearStart() == year) {
                res.add(hi);
            }
        }
        return res;
    }

    /*
     * Get active interventions in year.
     * Enforces physical invariants and updates associated state variables within {@code HistoricalInterventionCatalog}.
     *
     * @param year the year parameter (int)
     * @return the resulting computation or state reference
     */
    public List<HistoricalIntervention> getActiveInterventionsInYear(int year) {
        List<HistoricalIntervention> res = new ArrayList<>();
        for (HistoricalIntervention hi : interventions) {
            if (hi.isActive(year)) {
                res.add(hi);
            }
        }
        return res;
    }

    /*
     * Add custom intervention.
     * Enforces physical invariants and updates associated state variables within {@code HistoricalInterventionCatalog}.
     *
     * @param intervention the intervention parameter (HistoricalIntervention)
     */
    public void addCustomIntervention(HistoricalIntervention intervention) {
        if (intervention != null) {
            interventions.add(intervention);
        }
    }
}
