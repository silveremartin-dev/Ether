/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.ether.society.engines.tier2.historical.LeslieWhitePureEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Leslie White Energy and Cultural Evolution Engine.
 *
 * <p>Implements Leslie White's Law of Cultural Evolution (1943, 1949, <i>The Science of Culture</i>):
 * cultural development and structural complexity scale with the per-capita exergy harvested annually
 * multiplied by the physical efficiency of mechanical energy transducers (\(C = E \cdot T\)).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see LeslieWhitePureEngine
 */
public class WhiteEnergyEvolutionEngine implements ProceduralEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(WhiteEnergyEvolutionEngine.class);

    @Override
    public String getName() {
        return "WhiteEnergyEvolutionEngine";
    }

    @Override
    public String getDescription() {
        return "Models Leslie White (1949) law of cultural evolution: societal complexity scales with per-capita energy capture and transducer efficiency.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Leslie White Law of Cultural Evolution (White 1949)]
               • Cultural Complexity: C = E · T
               • Energy Capture:      E = E_food + E_fire + E_wind + E_solar + E_livestock + E_fossil
               • Capital Deepening:   ΔK_active = f(E / Pop) · Efficiency
               Ref: L. A. White (1949), "The Science of Culture", Farrar, Straus and Giroux.
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Anthropological Theories";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        LeslieWhitePureEngine.processHybrid(cells, deltaYears);
    }
}
