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
    private static final String RESOURCE_PATH = "/data/scenarios/earth_historical_leaders.json";
    private static final String FILE_PATH = "data/scenarios/earth_historical_leaders.json";

    private static final HistoricalInterventionCatalog INSTANCE = new HistoricalInterventionCatalog();

    private final List<HistoricalIntervention> interventions = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public static HistoricalInterventionCatalog getInstance() {
        return INSTANCE;
    }

    public HistoricalInterventionCatalog() {
        loadCatalog();
    }

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

            // 2. Try Local File
            File f = new File(FILE_PATH);
            if (f.exists()) {
                List<HistoricalIntervention> loaded = mapper.readValue(f, new TypeReference<List<HistoricalIntervention>>() {});
                interventions.addAll(loaded);
                logger.info("Successfully loaded {} historical interventions from local file {}.", loaded.size(), FILE_PATH);
                return;
            }

            logger.warn("No earth_historical_leaders.json found on classpath or filesystem. Initializing with default set.");
            initFallbackCatalog();
        } catch (Exception e) {
            logger.error("Failed to parse earth_historical_leaders.json: {}", e.getMessage(), e);
            initFallbackCatalog();
        }
    }

    private void initFallbackCatalog() {
        interventions.add(new HistoricalIntervention("ALEXANDER_FALLBACK", "Alexandre le Grand", "Conquête fulgurante et hellénisation.", -334, 11, 40.64, 22.94, 3500.0, LeaderArchetype.MILITARY_CONQUEROR, 9.5));
        interventions.add(new HistoricalIntervention("AUGUSTUS_FALLBACK", "Auguste & Pax Romana", "Réseau routier et centralisation impériale.", -27, 41, 41.90, 12.49, 2200.0, LeaderArchetype.INFRASTRUCTURE_BUILDER, 9.0));
        interventions.add(new HistoricalIntervention("GUTENBERG_FALLBACK", "Johannes Gutenberg", "Révolution de l'imprimerie et diffusion des savoirs.", 1440, 28, 49.99, 8.27, 2000.0, LeaderArchetype.INFRASTRUCTURE_BUILDER, 9.5));
    }

    public List<HistoricalIntervention> getInterventions() {
        return Collections.unmodifiableList(new ArrayList<>(interventions));
    }

    public List<HistoricalIntervention> getInterventionsStartingInYear(int year) {
        List<HistoricalIntervention> res = new ArrayList<>();
        for (HistoricalIntervention hi : interventions) {
            if (hi.getYearStart() == year) {
                res.add(hi);
            }
        }
        return res;
    }

    public List<HistoricalIntervention> getActiveInterventionsInYear(int year) {
        List<HistoricalIntervention> res = new ArrayList<>();
        for (HistoricalIntervention hi : interventions) {
            if (hi.isActive(year)) {
                res.add(hi);
            }
        }
        return res;
    }

    public void addCustomIntervention(HistoricalIntervention intervention) {
        if (intervention != null) {
            interventions.add(intervention);
        }
    }
}
