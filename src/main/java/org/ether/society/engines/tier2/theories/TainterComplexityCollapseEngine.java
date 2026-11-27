/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
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
 * Institutional Complexity &amp; Declining Marginal Returns Engine (Joseph Tainter, 1988).
 *
 * <p>Models the diminishing marginal returns to socio-political and technological complexity,
 * contrasting sub-linear logarithmic per-capita surplus generation against super-linear
 * institutional maintenance costs:</p>
 * <ul>
 *   <li>Per-capita capital: $k_i = K_i / N_i$</li>
 *   <li>Logarithmic surplus flow: $S_i = N_i \cdot (0.05 + 2.5 \ln(1 + k_i / 200)) \cdot \Delta t_{\text{yr}}$</li>
 *   <li>Complexity index: $C_i = \ln(1 + 0.02 \cdot k_i)$</li>
 *   <li>Super-linear maintenance burden: $\Sigma_{\text{maint}, i} = (0.015 K_i + \max(1, N_i) \cdot 0.08 \cdot C_i^{2.2}) \cdot \Delta t_{\text{yr}}$</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.5
 */
public class TainterComplexityCollapseEngine extends AbstractSimulationEngine {
    public TainterComplexityCollapseEngine() {
        super("TainterComplexityCollapseEngine", "TainterComplexityCollapseEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }
    private static final Logger logger = LoggerFactory.getLogger(TainterComplexityCollapseEngine.class);

    /**
     * Get name.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getName() {
        return "Tainter Institutional Complexity & Declining Returns";
    }

    /**
     * Get description.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getDescription() {
        return "Models the diminishing marginal returns on socio-political complexity and fiscal-energetic collapse vulnerabilities.";
    }

    /**
     * Get equations tooltip.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getEquationsTooltip() {
        return """
               [Tainter Law of Diminishing Marginal Returns on Complexity (1988)]
               • Complexity Level:   C_i = ln(1 + 0.05 · Capital_i)
               • Maintenance Burden: Σ_maint = C_i^1.20 · 50.0 Joules
               • Net Growth Rate:    dCapital/dt = NetSurplus_i - Σ_maint
               • Collapse Condition: If Σ_maint > Surplus -> Rapid Simplification / Capital Decay
               Units: C [score complexité sans dimension], Σ_maint [Joules maintenance institutionnelle/an]
               Ref: J. A. Tainter (1988) "The Collapse of Complex Societies", Cambridge Univ. Press
               """;
    }

    /**
     * Get category.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getCategory() {
        return "Tier 2: Institutional Cliodynamics";
    }

    /**
     * Indicates that this engine operates directly on Data-Oriented Design (DOD) cellular derivatives.
     *
     * @return {@code true} since cellular derivatives are supported
     */
    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    /**
     * Accumulates Tainter institutional complexity maintenance and capital erosion derivatives for cell {@code i}.
     *
     * @param buffer    contiguous DOD world state buffer
     * @param i         target H3 cell index
     * @param dtYears   integration time step in Julian years [$\text{yr}$]
     * @param dtMonthly integration time step in calendar months [$\text{mo}$]
     * @param acc       cellular derivative accumulator
     */
    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;

        double capital = buffer.getResourceCapital()[i];
        if (capital < 50.0) return;

        double pop = buffer.getHumanPopulation()[i];
        double kPerCap = (pop > 0.0) ? (capital / pop) : capital;
        // Tainter (1988) diminishing marginal returns: sub-linear logarithmic benefit against super-linear maintenance complexity
        double surplusFlow = (pop * (0.05 + 2.5 * Math.log1p(kPerCap / 200.0))) * dtYears;

        double complexity = Math.log(1.0 + 0.02 * kPerCap);
        double maintenanceCost = (0.015 * capital + Math.max(1.0, pop) * Math.pow(complexity, 2.2) * 0.08) * dtYears;

        if (maintenanceCost > surplusFlow) {
            double erosion = (maintenanceCost - surplusFlow) * 0.8;
            double newCap = Math.max(10.0, capital - erosion);
            acc.dResourceCapital += (newCap - capital);
            acc.dInstitutionalComplexity -= 0.01 * Math.min(0.5, erosion / Math.max(1.0, capital));
        }
    }
}
