/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.ether.society.engines.tier2.historical.LenskiPureEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Gerhard Lenski Ecological-Evolutionary Theory Engine.
 *
 * <p>Implements Gerhard Lenski's ecological-evolutionary theory of societal stratification
 * (Lenski 1966, <i>Power and Privilege: A Theory of Social Stratification</i>;
 * Milanovic, Lindert &amp; Williamson 2011):</p>
 * <ul>
 *   <li>Economic inequality (Gini) is fundamentally shaped by the societal mode of production and capital density:
 *       low in foraging bands, peaking in agrarian empires, and contracting/stabilizing in industrial societies.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see LenskiPureEngine
 */
public class LenskiEcologicalEvolutionaryEngine implements ProceduralEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(LenskiEcologicalEvolutionaryEngine.class);

    @Override
    public String getName() {
        return "LenskiEcologicalEvolutionaryEngine";
    }

    @Override
    public String getDescription() {
        return "Models Gerhard Lenski (1966) ecological-evolutionary inequality curve: Gini index peaks in agrarian regimes and stabilizes under industrial capital.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Lenski Ecological-Evolutionary Stratification (Lenski 1966, Milanovic 2011)]
               • Gini Curve: G*(k_pc) = G_forager + ΔG_agr · exp(-[ln((k_pc+10)/(k_agr+10))]^2 / (2σ^2)) + ΔG_ind · k_pc^2/(k_pc^2 + k_ind^2)
               Ref: G. Lenski (1966), "Power and Privilege", McGraw-Hill.
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Sociological Stratification";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        LenskiPureEngine.processHybrid(cells, deltaYears);
    }
}
