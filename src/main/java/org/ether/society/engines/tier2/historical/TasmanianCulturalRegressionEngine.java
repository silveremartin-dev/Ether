/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tasmanian Cultural Regression &amp; Demographic Isolation Engine.
 *
 * <p>Formalizes Joseph Henrich's Gumbel extreme-value model of cultural skill transmission
 * (2004, <i>American Antiquity</i> 69(2): 197–214, Eq. 3):</p>
 * \[
 * \Delta \bar{z} = -\alpha + \beta \left(\gamma_E + \ln N_e\right) = \beta \ln\!\left(\frac{N_e}{N^*}\right),
 * \qquad N^* = \exp\!\left(\frac{\alpha}{\beta} - \gamma_E\right)
 * \]
 * <p>where \(\gamma_E \approx 0.5772156649\) is the Euler-Mascheroni constant and \(N^*\) is the critical
 * interacting learner pool size required to offset transmission error \(\alpha\). When \(N_e &lt; N^*\),
 * \(\Delta \bar{z} &lt; 0\) induces logarithmic skill and physical tooling decay proportional to
 * \(\ln(N^* / N_e)\).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class TasmanianCulturalRegressionEngine extends AbstractSimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(TasmanianCulturalRegressionEngine.class);

    /** Euler-Mascheroni constant (\(\gamma_E \approx 0.5772156649\)) in Henrich's (2004) Gumbel skill model. */
    public static final double EULER_MASCHERONI_GAMMA = 0.5772156649015329;

    /** Skill transmission dispersion coupling rate (\(\beta\) in \(\text{yr}^{-1}\)). */
    public static final double HENRICH_DISPERSION_BETA_PER_YR = 0.025;

    private static double isolationPopulationThreshold = 5000.0;
    private double currentLossRate = 0.0;

    public TasmanianCulturalRegressionEngine() {
        super("TasmanianCulturalRegressionEngine", "Tasmanian Cultural Regression", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    /**
     * Evaluates Henrich's (2004, Eq. 3) logarithmic skill-loss rate \(\Delta \bar{z} = \beta \ln(N_e / N^*)\)
     * across the active learner pool before cellular derivative sweeps.
     *
     * @param buffer contiguous world memory buffer
     * @param dtYears elapsed time in Julian years
     */
    public void prepareTick(WorldBuffer buffer, double dtYears) {
        if (buffer == null || buffer.getHumanPopulation() == null || dtYears <= 0.0) {
            currentLossRate = 0.0;
            return;
        }
        float[] pop = buffer.getHumanPopulation();
        double effectivePool = 0.0;
        int cap = buffer.getCapacity();
        for (int i = 0; i < cap; i++) {
            if (pop[i] > 0.0f) effectivePool += pop[i];
        }
        if (effectivePool <= 0.0) {
            currentLossRate = 0.0;
            return;
        }
        double logPoolDeficit = Math.log(isolationPopulationThreshold / Math.max(1.0, effectivePool));
        double smoothDeficit = SmoothMath.softplus(logPoolDeficit, 0.0, 20.0);
        currentLossRate = Math.min(0.25, HENRICH_DISPERSION_BETA_PER_YR * smoothDeficit) * dtYears;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (currentLossRate > 1e-6 && buffer != null && buffer.getResourceCapital() != null) {
            float cap = buffer.getResourceCapital()[i];
            if (cap > 0.0f) {
                acc.dResourceCapital -= (float) (cap * currentLossRate);
            }
        }
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        if (buffer == null) return;
        prepareTick(buffer, dtYears);
        super.tick(buffer, dtYears, dtMonthly);
    }

    public static double getIsolationPopulationThreshold() {
        return isolationPopulationThreshold;
    }

    public static void setIsolationPopulationThreshold(double threshold) {
        isolationPopulationThreshold = Math.max(100.0, threshold);
    }

    public static void resetState() {
        isolationPopulationThreshold = 5000.0;
    }

    @Override
    public void reset() {
        resetState();
        currentLossRate = 0.0;
    }
}
