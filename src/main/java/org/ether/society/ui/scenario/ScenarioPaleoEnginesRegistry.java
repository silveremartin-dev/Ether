/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import java.util.List;
import java.util.OptionalLong;
import org.ether.society.ui.scenario.ScenarioEngineRegistry.EngineMeta;

/**
 * Legacy helper catalog for Plio-Pleistocene and Paleolithic optional engines (-3,000,000 BP to -10,000 BP).
 *
 * <p>All verified, executable deep-time Tier 2 Type B engines are registered directly in
 * {@link ScenarioHistoricalEnginesRegistry#getHistoricalEngines()} to guarantee a strict 1-to-1
 * bijection between UI checkboxes and actual Java engine classes.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see ScenarioHistoricalEnginesRegistry
 */
final class ScenarioPaleoEnginesRegistry {

    private ScenarioPaleoEnginesRegistry() {
        // Utility catalog class
    }

    /**
     * Returns an empty list as all real Plio-Pleistocene and Paleolithic Type B engines
     * are registered in {@link ScenarioHistoricalEnginesRegistry#getHistoricalEngines()}.
     *
     * @return empty immutable list
     */
    static List<EngineMeta> getPaleoEngines() {
        return List.of();
    }

    /**
     * Resolves Paleolithic apparition year if defined.
     *
     * @param key engine class identifier
     * @return empty optional as all emergence thresholds are resolved by
     *         {@link ScenarioHistoricalEnginesRegistry#getHistoricalApparitionYear(String)}
     */
    static OptionalLong getPaleoApparitionYear(String key) {
        return OptionalLong.empty();
    }
}
