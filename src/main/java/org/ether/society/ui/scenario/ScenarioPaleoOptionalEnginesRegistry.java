/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import java.util.List;
import org.ether.society.ui.scenario.ScenarioEngineRegistry.EngineMeta;

/**
 * Legacy helper catalog for Paleolithic optional engines.
 *
 * <p>All verified, executable deep-time and Paleolithic Tier 2 Type B engines
 * ({@code KinSelectionHamiltonEngine}, {@code SelfDomesticationEngine},
 * {@code SexualSelectionMatingEngine}, {@code CulturalSociologyEngine},
 * {@code LanguageLinguisticEngine}, {@code DynamicMaritimeRoutingGraph},
 * {@code TasmanianCulturalRegressionEngine}, and {@code HenrichCumulativeCultureEngine})
 * are consolidated directly in {@link ScenarioHistoricalEnginesRegistry#getHistoricalEngines()}
 * to guarantee a strict 1-to-1 bijection between UI checkboxes and actual Java engine classes.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see ScenarioHistoricalEnginesRegistry
 */
public final class ScenarioPaleoOptionalEnginesRegistry {

    private ScenarioPaleoOptionalEnginesRegistry() {
        // Utility catalog class
    }

    /**
     * Returns an empty list as all real Paleolithic Type B engines are registered in
     * {@link ScenarioHistoricalEnginesRegistry#getHistoricalEngines()}.
     *
     * @return empty immutable list
     */
    public static List<EngineMeta> getPaleoOptionalEngines() {
        return List.of();
    }
}
