/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.engine.SimulationEngineModule;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.ether.society.model.Scenario;
import org.ether.society.engines.tier2.historical.LenskiPureEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Gerhard Lenski Ecological-Evolutionary Theory Engine.
 *
 * <p>Implements Gerhard Lenski's ecological-evolutionary theory of societal stratification
 * (Lenski 1966, <i>Power and Privilege: A Theory of Social Stratification</i>;
 * Milanovic, Lindert &amp; Williamson 2011; Piketty 2014):</p>
 * <ul>
 *   <li>Economic inequality (Gini) is fundamentally shaped by the societal mode of production,
 *       surplus above subsistence, and the Piketty $r > g$ dynamic extraction ratio.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see LenskiPureEngine
 */
public class LenskiEcologicalEvolutionaryEngine implements ProceduralEnginePlugin, SimulationEngineModule {

    private static final Logger logger = LoggerFactory.getLogger(LenskiEcologicalEvolutionaryEngine.class);

    @Override
    public String id() {
        return "LenskiEcologicalEvolutionaryEngine";
    }

    @Override
    public String name() {
        return getName();
    }

    @Override
    public EngineTier tier() {
        return EngineTier.TIER_2_CLIODYNAMIC;
    }

    @Override
    public boolean isEnabled(Scenario scenario) {
        return scenario != null && scenario.getTypeBEngineStates() != null
                && (Boolean.TRUE.equals(scenario.getTypeBEngineStates().get(id()))
                || Boolean.TRUE.equals(scenario.getTypeBEngineStates().get("LenskiPureEngine")));
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        processBuffer(buffer, dtYears);
    }

    @Override
    public void reset() {
        // Stateless physical operator
    }

    @Override
    public String getName() {
        return "LenskiEcologicalEvolutionaryEngine";
    }

    @Override
    public String getDescription() {
        return "Models Gerhard Lenski & Milanovic (2011) / Piketty (2014) dynamic inequality possibility frontier and capital extraction ODE.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Milanovic-Piketty-Lenski Inequality Frontier (IPF / ODE)]
               • Max Feasible Gini:  G_max(y) = 1 - s_min / y (s_min = 15 kg/cap)
               • Extraction Ratio:   μ = μ_0 + Δμ · σ(r - g) · σ(Complexity)
               • Dynamic Gini ODE:   dG/dt = λ · (μ · G_max - G)
               Ref: B. Milanovic (2011) "Pre-Industrial Inequality"; T. Piketty (2014) "Capital in the 21st Century"
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Sociological Stratification";
    }

    @Override
    public void processBuffer(WorldBuffer buffer, double deltaYears) {
        LenskiPureEngine.processLenskiPikettyInequality(buffer, deltaYears);
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        LenskiPureEngine.processHybrid(cells, deltaYears);
    }
}
