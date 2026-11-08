/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import java.util.Collections;
import java.util.List;
import org.ether.society.ui.scenario.ScenarioEngineCatalog.EngineMeta;

/**
 * Catalog of Optional and Pluggable Tier 2 Type B Simulation Engines across Paleolithic,
 * Historical, Industrial, and Prospective eras.
 *
 * <p>Every engine entry returned by {@link #getOptionalEngines()} is strictly backed by
 * an executable Java class in {@code org.ether.society.engines.tier2.theories},
 * {@code org.ether.society.engines.tier2.historical}, or optional anthropogenic modules
 * in {@code org.ether.society.engines.tier1}. Permanent Tier 1 biophysical conservation
 * laws are documented in {@link ScenarioEngineCatalog#getCoreEngines()}.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see ScenarioHistoricalEnginesCatalog
 * @see org.ether.society.model.AutoEpochScenarioHelper
 */
public final class ScenarioOptionalEnginesCatalog {

    private ScenarioOptionalEnginesCatalog() {
        // Utility catalog class
    }

    /**
     * Returns the complete ordered catalog of the 86 verified optional Tier 2 Type B
     * simulation engines.
     *
     * @return unmodifiable list of {@link EngineMeta} entries
     */
    public static List<EngineMeta> getOptionalEngines() {
        return Collections.unmodifiableList(ScenarioHistoricalEnginesCatalog.getHistoricalEngines());
    }

    /**
     * Returns the historical emergence threshold year (BCE/CE) for the specified
     * Tier 2 Type B engine identifier.
     *
     * @param key engine class identifier
     * @return emergence year in BCE/CE
     */
    public static long getEngineApparitionYear(String key) {
        return ScenarioHistoricalEnginesCatalog.getHistoricalApparitionYear(key);
    }

    /**
     * Returns default parameter values for configurable Type B engine parameters in UI panels.
     *
     * @param engineKey engine class identifier
     * @param paramKey parameter identifier
     * @return default numerical parameter value
     */
    public static double getTypeBParamDefault(String engineKey, String paramKey) {
        if ("MaritimeHighwayEngine".equals(engineKey)) {
            if ("capitalBoostRate".equals(paramKey)) return 0.05;
            if ("frictionMultiplier".equals(paramKey)) return 0.20;
        } else if ("HydrologicalEngineeringEngine".equals(engineKey)) {
            if ("tenochtitlanTech".equals(paramKey)) return 3.5;
            if ("tenochtitlanCapital".equals(paramKey)) return 150.0;
            if ("aralRainfallThreshold".equals(paramKey)) return 300.0;
            if ("damMinElevation".equals(paramKey)) return 300.0;
        } else if ("SoilSalinizationHydrologyEngine".equals(engineKey)) {
            if ("salinizationRate".equals(paramKey)) return 0.02;
        } else if ("LandReclamationEngine".equals(engineKey)) {
            if ("polderTechThreshold".equals(paramKey)) return 4.0;
            if ("polderCapitalThreshold".equals(paramKey)) return 100.0;
        }
        return 0.0;
    }
}
