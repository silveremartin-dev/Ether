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
 * <p>Models the diminishing marginal returns to bureaucratic and technological complexity,
 * and the vulnerability of over-complex societies to catastrophic fiscal/energetic collapse:</p>
 * <pre>
 *   C_i = ln(1 + 0.1 · K_i)
 *   Σ_maint = C_i^1.15 · E_base
 *   dK_i/dt = Production_i · s - Σ_maint
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
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
        if (dtYears <= 0.0) return;

        double capital = buffer.getResourceCapital()[i];
        if (capital < 50.0) return;

        double pop = buffer.getBiomassHuman()[i] / 0.06;
        double kPerCap = (pop > 0.0) ? (capital / pop) : capital;
        double surplusFlow = (pop * (0.05 + 0.020 * kPerCap)) * dtYears;

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
