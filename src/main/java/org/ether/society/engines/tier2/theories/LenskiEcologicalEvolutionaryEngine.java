/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.engine.SimulationEngineModule;
import org.ether.society.database.H3Cell;

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
 * @version 1.0.0-beta.3
 * @see LenskiPureEngine
 */
public class LenskiEcologicalEvolutionaryEngine extends AbstractSimulationEngine {
    public LenskiEcologicalEvolutionaryEngine() {
        super("LenskiEcologicalEvolutionaryEngine", "LenskiEcologicalEvolutionaryEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

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
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (buffer == null || buffer.getHumanPopulation() == null || dtYears <= 0.0) return;
        float p = buffer.getHumanPopulation()[i];
        if (p <= 0.001f) return;
        float capital = (buffer.getResourceCapital() != null) ? buffer.getResourceCapital()[i] : 10.0f;
        float kPerCap = capital / p;
        double sMin = 15.0;
        double gMax = Math.clamp(1.0 - (sMin / Math.max(sMin + 1.0, (double) kPerCap)), 0.0, 0.85);
        double complexity = (buffer.getInstitutionalComplexity() != null) ? buffer.getInstitutionalComplexity()[i] : 0.0;
        double rMinusG = 0.05 - 0.02 * Math.tanh(kPerCap / 1000.0);
        double muExtract = 0.20 + 0.70 * org.ether.society.core.math.SmoothMath.sigmoid(rMinusG, 0.0, 0.02)
                * org.ether.society.core.math.SmoothMath.sigmoid(complexity, 0.25, 0.10);
        double lenskiBaseGini = LenskiPureEngine.calculateLenskiGini(kPerCap);
        double targetGini = Math.clamp(Math.max(lenskiBaseGini, 0.20 + muExtract * gMax), 0.20, 0.75);
        if (buffer.getGiniIndex() != null) {
            float currentGini = buffer.getGiniIndex()[i] > 0.0f ? buffer.getGiniIndex()[i] : 0.35f;
            float nextGini = (float) Math.clamp(currentGini + 0.04 * (targetGini - currentGini) * dtYears, 0.18, 0.80);
            acc.dGiniIndex += (nextGini - currentGini);
        }
        if (targetGini > 0.45 && kPerCap > sMin) {
            double drag = Math.min(0.25, 0.025 * (targetGini - 0.45) * dtYears);
            double nextCap = Math.max(p * sMin, capital * (1.0 - drag));
            acc.dResourceCapital += (float) (nextCap - capital);
        }
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        super.tick(buffer, dtYears, dtMonthly);
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
}
