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
 * Krugman Core-Periphery &amp; New Economic Geography Engine (Paul Krugman, Nobel 1991).
 *
 * <p>Models endogenous spatial agglomeration, trade costs, and forward-backward linkages:</p>
 * <pre>
 *   Ω_i = Σ_j [ Y_j · exp(-τ · d_ij) ]
 *   dK_i/dt = Centripetal_Force(Ω_i) - Centrifugal_Congestion(K_i / Area_i)
 * </pre>
 * where:
 * <ul>
 *   <li><b>Centripetal Forces</b>: Increasing returns in manufacturing, labor market pooling, low transport costs.</li>
 *   <li><b>Centrifugal Forces</b>: Agricultural dispersion, land rents, transport congestion.</li>
 *   <li><b>Bifurcation Point</b>: Below critical transport friction $\tau &lt; \tau^*$, symmetrical distribution collapses into core-periphery clustering.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class KrugmanCorePeripheryEngine extends AbstractSimulationEngine {
    public KrugmanCorePeripheryEngine() {
        super("KrugmanCorePeripheryEngine", "KrugmanCorePeripheryEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }
    private static final Logger logger = LoggerFactory.getLogger(KrugmanCorePeripheryEngine.class);

    /**
     * Get name.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getName() {
        return "Krugman Core-Periphery Spatial Agglomeration (NEG)";
    }

    /**
     * Get description.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getDescription() {
        return "Models endogenous industrial clustering, forward/backward linkages, and bifurcation from dispersed to core-periphery geography.";
    }

    /**
     * Get equations tooltip.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getEquationsTooltip() {
        return """
               [Krugman New Economic Geography (JPE 1991)]
               • Market Potential:      Ω_i = Σ_j [ Income_j · exp(-τ · dist_ij) ]
               • Agglomeration Factor:  A_i = σ(2.0 · (K_i / K_mean - 1)) · (Access_i / (1 + Access_i))
               • Friction Reduction:    Δμ = -2% · A_i · Δt
               • Bifurcation Threshold: Symmetrical distribution destabilizes when τ falls below critical value.
               Units: Ω [potentiel de marché sans dimension], τ [coefficient de friction transport km^-1]
               Ref: P. Krugman (1991) "Increasing Returns and Economic Geography", Journal of Political Economy
               """;
    }

    /**
     * Get category.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getCategory() {
        return "Tier 2: Spatial Economics";
    }

    private transient double meanCapital = 10.0;

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void prepareTick(WorldBuffer buffer, double dtYears) {
        meanCapital = 10.0;
        if (buffer == null || buffer.getResourceCapital() == null) return;
        float[] capArr = buffer.getResourceCapital();
        double sum = 0.0;
        int n = buffer.getCapacity();
        for (int i = 0; i < n; i++) sum += capArr[i];
        meanCapital = n > 0 ? Math.max(1.0, sum / n) : 10.0;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getBiomassHuman() == null || dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 1.0) return;

        double capital = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 10.0;
        double friction = buffer.getFluxPressure() != null ? Math.max(0.2, 1.0 + buffer.getFluxPressure()[i]) : 1.0;

        double accessibilityIndex = (pop * 0.001) / friction;
        double relCapital = capital / meanCapital;
        double capitalAdvantage = 1.0 / (1.0 + Math.exp(-2.0 * (relCapital - 1.0)));
        double accessAdvantage = accessibilityIndex / (1.0 + accessibilityIndex);
        double centripetalFactor = 2.0 * capitalAdvantage * accessAdvantage;

        acc.dFluxPressure -= (float) (0.02 * centripetalFactor * dtYears);
        double centrifugalCongestion = 0.01 * (relCapital / Math.max(1.0, accessibilityIndex));
        double netAgglomerationRate = Math.clamp((0.015 * centripetalFactor - centrifugalCongestion) * dtYears, -0.05, 0.08);
        acc.dResourceCapital += (float) (capital * netAgglomerationRate);
    }
}
