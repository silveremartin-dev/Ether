/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acemoglu-Robinson Institutional Political Economy Engine.
 *
 * <p>Formalizes the property-rights and expropriation-risk investment equation of Acemoglu, Johnson
 * &amp; Robinson (2001, <i>American Economic Review</i> 91(5): 1369–1401; 2005, <i>Handbook of Economic Growth</i>
 * Ch. 6) and Acemoglu &amp; Robinson (2012, <i>Why Nations Fail</i>):</p>
 * \[
 * r_{\text{net}} = (1 - \tau_{\text{extr}}) r_K - \delta_{\text{inst}}, \qquad
 * \frac{dK}{dt} = r_{\text{net}} \cdot \Phi_{\text{pr}}(k_{\text{pc}}) \cdot K
 * \]
 * <p>Inclusive institutions restrain elite expropriation (\(\tau_{\text{extr}} \ll 1 - \delta_{\text{inst}}/r_K\)),
 * fostering private capital deepening, whereas extractive concentration (\(\tau_{\text{extr}} &gt; 1 - \delta_{\text{inst}}/r_K\))
 * depresses private net return below the institutional hold-up hurdle rate \(\delta_{\text{inst}}\).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class AcemogluRobinsonInstitutionsEngine extends AbstractSimulationEngine {

    /** Gross private marginal return on productive physical capital (\(r_K = 0.020\,\text{yr}^{-1}\)). */
    public static final double GROSS_CAPITAL_RETURN_PER_YR = 0.020;

    /** Institutional hold-up / maintenance hurdle rate (\(\delta_{\text{inst}} = 0.007\,\text{yr}^{-1}\)). */
    public static final double INSTITUTIONAL_HURDLE_RATE_PER_YR = 0.007;

    public AcemogluRobinsonInstitutionsEngine() {
        super("AcemogluRobinsonInstitutionsEngine", "AcemogluRobinsonInstitutionsEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }
    private static final Logger logger = LoggerFactory.getLogger(AcemogluRobinsonInstitutionsEngine.class);

    @Override
    public String getName() {
        return "AcemogluRobinsonInstitutionsEngine";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Acemoglu-Johnson-Robinson Expropriation Wedge (2001, 2005, 2012)]
               • Expropriation Risk:     τ_extr = smoothClamp(Gini, 0.05, 0.95)
               • Net Private Return:     r_net = (1 - τ_extr) · r_K - δ_inst
               • Capital Accumulation:   dK/dt = r_net · σ(k_pc; 100.0, 30.0) · K
               Ref: Acemoglu, Johnson & Robinson (2005), Handbook of Economic Growth, Ch. 6
               """;
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getHumanPopulation() == null || dtYears <= 0.0) return;
        double pop = buffer.getHumanPopulation()[i];
        if (pop <= 1.0) return;

        double gini = buffer.getGiniIndex() != null ? buffer.getGiniIndex()[i] : 0.35;
        double capital = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 10.0;
        double kPerCapita = capital / pop;

        double expropriationRisk = SmoothMath.smoothClamp(gini, 0.05, 0.95, 20.0);
        double netPrivateReturn = (1.0 - expropriationRisk) * GROSS_CAPITAL_RETURN_PER_YR - INSTITUTIONAL_HURDLE_RATE_PER_YR;
        double propertyRightsActivation = SmoothMath.sigmoid(kPerCapita, 100.0, 30.0);
        acc.dResourceCapital += (float) (capital * netPrivateReturn * propertyRightsActivation * dtYears);
    }
}
