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

/**
 * W. Arthur Lewis Dual-Sector Model &amp; Ranis-Fei Turning Point Engine (Lewis 1954; Ranis &amp; Fei 1961).
 *
 * <p>Implements the canonical two-sector developmental growth model formulated by W. Arthur Lewis
 * (1954, <i>The Manchester School</i> 22(2): 139–191) and mathematically formalized by Gustav Ranis
 * &amp; John C. H. Fei (1961, <i>American Economic Review</i> 51(4): 533–565):</p>
 * <ul>
 *   <li><b>Subsistence Sector Marginal Product</b>: \(MP_{L,A} = \beta_A \frac{Y_A}{N}\). While rural labor
 *       remains in disguised surplus (\(MP_{L,A} &lt; \bar{w}_S\)), the modern capitalist sector absorbs labor
 *       at fixed institutional subsistence wage \(\bar{w}_S\), reinvesting capitalist surplus \(\pi_M = Y_M - \bar{w}_S L_M\).</li>
 *   <li><b>Commercialization / Lewis Turning Point</b>: Once rural surplus labor is absorbed such that
 *       \(MP_{L,A} \ge \bar{w}_S\) alongside commercial market integration and industrial capital intensity
 *       (\(k_{\text{pc}} \ge k_{\text{turn}}\)), real wages rise with marginal productivity (\(w = MP_{L,A}\)),
 *       inducing capital-for-labor mechanization, rising labor income share (falling Gini), and formal labor productivity.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.5
 */
public class LewisDualSectorTurningPointEngine extends AbstractSimulationEngine {

    /** Capital per capita onset of modern dual-sector manufacturing (\(\text{kg/cap}\)). */
    public static final double DUAL_SECTOR_KPC_ONSET = 80.0;

    /** Capital per capita threshold of full modern sector commercialization (\(\text{kg/cap}\)). */
    public static final double LEWIS_TURNING_POINT_KPC = 250.0;

    /** Institutional subsistence real wage benchmark \(\bar{w}_S\) in agricultural output units per worker. */
    public static final double INSTITUTIONAL_SUBSISTENCE_WAGE = 0.65;

    /** Agricultural labor elasticity \(\beta_A\) in Ranis-Fei commercialization condition \(MP_{L,A} = \beta_A (Y_A / N)\). */
    public static final double AGRARIAN_ELASTICITY_BETA = 0.65;

    /**
     * Constructs the Lewis / Ranis-Fei dual-sector turning point engine.
     */
    public LewisDualSectorTurningPointEngine() {
        super("LewisDualSectorTurningPointEngine", "LewisDualSectorTurningPointEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public String getName() {
        return "LewisDualSectorTurningPointEngine";
    }

    @Override
    public String getDescription() {
        return "Models Lewis (1954) & Ranis-Fei (1961) dual-sector development: unlimited rural labor absorption at fixed wage transitions at the commercialization turning point (MP_L,A >= w_bar) to wage growth and mechanization.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Lewis & Ranis-Fei Dual-Sector Model (Lewis 1954; Ranis & Fei 1961)]
               • Rural Marginal Product: MP_L,A = β_A · (Y_agr / N)
               • Surplus Labor Phase:    MP_L,A < w_bar => w = w_bar, dK/dt = s_π · (Y_M - w_bar · L_M)
               • Lewis Turning Point:    MP_L,A >= w_bar => w = MP_L,A (rising real wage & induced mechanization)
               Parameters: β_A = 0.65, w_bar = 0.65, k_onset = 80 kg/cap, k_turn = 250 kg/cap
               Ref: W. A. Lewis (1954, Manchester School 22:139); G. Ranis & J. C. H. Fei (1961, AER 51:533)
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Developmental Macroeconomics";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getHumanPopulation() == null || dtYears <= 0.0) return;
        double pop = buffer.getHumanPopulation()[i];
        if (pop <= 50.0) return;

        double capital = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 0.0;
        double kpc = capital / pop;
        if (kpc < DUAL_SECTOR_KPC_ONSET) return;

        double agriOutput = buffer.getBiomassAgriculture() != null ? Math.max(0.0, buffer.getBiomassAgriculture()[i]) : 0.0;
        double mpLaborAgri = AGRARIAN_ELASTICITY_BETA * ((agriOutput + 1.0) / pop);
        double trade = buffer.getCommercialFluxVolume() != null ? buffer.getCommercialFluxVolume()[i] : 0.0;

        // Ranis-Fei (1961) commercialization condition: MP_L,A crosses institutional wage w_bar
        double ruralScarcityTransition = SmoothMath.sigmoid(mpLaborAgri, INSTITUTIONAL_SUBSISTENCE_WAGE, 0.25);
        double modernCapitalDepth = SmoothMath.sigmoid(kpc, LEWIS_TURNING_POINT_KPC, 40.0);
        double marketIntegration = SmoothMath.sigmoid(trade, 20.0, 5.0);

        // Phase 1: Unlimited supplies of labor (MP_L,A < w_bar, early industrial capital widening)
        double surplusLaborPhase = SmoothMath.sigmoid(kpc, DUAL_SECTOR_KPC_ONSET, 25.0) * (1.0 - modernCapitalDepth);
        if (surplusLaborPhase > 0.05) {
            double wideningDeltaK = capital * 0.030 * surplusLaborPhase * dtYears;
            acc.dResourceCapital += (float) wideningDeltaK;
            capital += wideningDeltaK;
        }

        // Phase 2: Post-Lewis Turning Point (exhausted rural surplus + commercialized capital deepening)
        double turningPointIntensity = modernCapitalDepth * marketIntegration * (0.50 + 0.50 * ruralScarcityTransition);
        if (turningPointIntensity > 0.05) {
            acc.dResourceCapital += (float) (capital * 0.045 * turningPointIntensity * dtYears);
            acc.dGiniIndex -= (float) (0.015 * turningPointIntensity * dtYears);
            acc.dInstitutionalComplexity += (float) (0.012 * turningPointIntensity * dtYears);
        }
    }
}
