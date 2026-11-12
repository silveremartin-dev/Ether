/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Granovetter Revolutionary Threshold & Collective Action Cascade Engine (Mark Granovetter, 1978).
 *
 * <p>Models non-linear tipping points and spontaneous collective mobilization cascades:</p>
 * <pre>
 *   Decision_i(t + 1) = 1  if  [ N_active(t) / N_total ] ≥ θ_i
 *   Decision_i(t + 1) = 0  otherwise
 * </pre>
 * where:
 * <ul>
 *   <li><b>θ_i</b>: Individual risk-tolerance threshold drawn from distribution $\theta_i \sim \mathcal{N}(\mu, \sigma^2)$.</li>
 *   <li><b>Cascade Condition</b>: A small radical minority ($\theta \approx 0$) can trigger an avalanche if intermediate thresholds are present.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class GranovetterThresholdCascadeEngine extends AbstractSimulationEngine {
    public GranovetterThresholdCascadeEngine() {
        super("GranovetterThresholdCascadeEngine", "GranovetterThresholdCascadeEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }
    private static final Logger logger = LoggerFactory.getLogger(GranovetterThresholdCascadeEngine.class);

    /**
     * Get name.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getName() {
        return "Granovetter Threshold Cascade Dynamics";
    }

    /**
     * Get description.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getDescription() {
        return "Models non-linear collective action tipping points, spontaneous social avalanches, and revolutionary cascades.";
    }

    /**
     * Get equations tooltip.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getEquationsTooltip() {
        return """
               [Granovetter Collective Action Threshold Model (AJS 1978)]
               • Individual Rule:     Act_i(t+1) = 1  iff  f_active(t) ≥ θ_i
               • Threshold Density:   θ_i ~ Normal(μ_tolerance, σ_variance^2)
               • Avalanche Cascade:   df_active/dt = ∫_0^{f_active} P(θ) dθ - f_active
               • Critical Phase Shift: Complete regime shift when distribution contains uninterrupted chain of thresholds.
               Units: f_active [fraction mobilisée [0, 1]], θ_i [seuil de bascule individuel [0, 1]]
               Ref: M. Granovetter (1978) "Threshold Models of Collective Behavior", American Journal of Sociology
               """;
    }

    /**
     * Get category.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getCategory() {
        return "Tier 2: Behavioral Sociology";
    }

    /**
     * Process.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getBiomassHuman() == null || dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 1.0) return;

        double gini = buffer.getGiniIndex() != null ? buffer.getGiniIndex()[i] : 0.35;
        double food = buffer.getFoodResource() != null ? buffer.getFoodResource()[i] : 500.0;

        double giniGrievance = 1.0 / (1.0 + Math.exp(-12.0 * (gini - 0.45)));
        double caloricGrievance = 1.0 / (1.0 + Math.exp(0.015 * (food - 350.0)));
        double grievance = 0.5 * (giniGrievance + caloricGrievance);
        double mobilizationFraction = 1.0 / (1.0 + Math.exp(-8.0 * (grievance - 0.40)));

        double cap = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 0.0;
        acc.dResourceCapital -= (float) (cap * 0.02 * mobilizationFraction * dtYears);
        acc.dInstitutionalComplexity -= (float) (0.04 * mobilizationFraction * dtYears);
    }
}



