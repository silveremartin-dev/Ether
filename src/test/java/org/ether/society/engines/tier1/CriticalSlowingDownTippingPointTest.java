/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Low-level mathematical verification of Critical Slowing Down (CSD) and early-warning indicators
 * preceding catastrophic ecological bifurcations and desertification tipping points.
 *
 * <p>Theoretical Foundations:
 * - Scheffer, M. et al. (2009). <i>Early-warning signals for critical transitions</i>. Nature, 461(7260), 53-59.
 * - Strogatz, S. H. (2018). <i>Nonlinear Dynamics and Chaos: With Applications to Physics, Biology, Chemistry, and Engineering</i>. CRC Press.
 *
 * <p>Universal Saddle-Node Bifurcation Normal Form:
 * Near an ecological regime shift (e.g. Sahel-Sahara vegetation collapse driven by declining precipitation P):
 * dx/dt = (P - P_crit) / P_scale - x^2 + sigma * dW_t
 *
 * At the stable vegetated equilibrium x* = sqrt(mu), the restorative eigenvalue is lambda = -2 * sqrt(mu).
 * As mu -&gt; 0^+:
 * 1. <b>Critical Slowing Down (CSD)</b>: The restorative force vanishes (lambda -&gt; 0^-),
 *    so the system takes longer to recover from perturbations, inflating lag-1 autocorrelation:
 *    ACF_1 = exp(lambda * dt) -&gt; 1.0.
 * 2. <b>Variance Amplification</b>: Perturbations accumulate in the flattening potential well:
 *    Var(x) = sigma^2 / (2 * |lambda|) -&gt; infinity.
 * 3. <b>Catastrophic Tipping</b>: When mu &lt; 0, the equilibrium vanishes and biomass collapses irreversibly.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class CriticalSlowingDownTippingPointTest {

    @Test
    @DisplayName("Critical Slowing Down: Autocorrelation & Variance Spikes Preceding Ecological Collapse")
    public void testCriticalSlowingDownEarlyWarningSignatures() {
        int sampleSteps = 300;
        double dt = 0.20; // Observation interval
        double noiseSigma = 0.25; // Stochastic weather volatility

        // Phase 1: Deep in Resilient Basin (Abundant Rain: P = 800 mm, mu = 4.0)
        double rainfallResilient = 800.0;
        double[] seriesResilient = simulateVegetationDynamics(rainfallResilient, sampleSteps, dt, noiseSigma, 424242L);
        double acf1Resilient = computeLag1Autocorrelation(seriesResilient);
        double varResilient = computeVariance(seriesResilient);

        // Phase 2: Near Critical Tipping Point (Aridifying: P = 425 mm, mu = 0.25)
        double rainfallNearBifurcation = 425.0;
        double[] seriesCritical = simulateVegetationDynamics(rainfallNearBifurcation, sampleSteps, dt, noiseSigma, 424242L);
        double acf1Critical = computeLag1Autocorrelation(seriesCritical);
        double varCritical = computeVariance(seriesCritical);

        // Phase 3: Post-Bifurcation Regime (Drought below threshold: P = 350 mm, mu = -0.50)
        double rainfallPostTipping = 350.0;
        double[] seriesCollapsed = simulateVegetationDynamics(rainfallPostTipping, sampleSteps, dt, noiseSigma, 424242L);
        double finalBiomassPostTipping = seriesCollapsed[sampleSteps - 1];

        // System Diagnostic Log
        System.out.printf("[CSD Test] Resilient State   -> Mean: %.2f | ACF(1): %.4f | Variance: %.4f%n",
                computeMean(seriesResilient), acf1Resilient, varResilient);
        System.out.printf("[CSD Test] Critical State    -> Mean: %.2f | ACF(1): %.4f | Variance: %.4f%n",
                computeMean(seriesCritical), acf1Critical, varCritical);
        System.out.printf("[CSD Test] Collapsed State   -> Final Biomass: %.2f (Vegetation Collapsed)%n",
                finalBiomassPostTipping);

        // ── Assertion 1: Critical Slowing Down (Lag-1 Autocorrelation Inflation) ──
        // Loin du seuil, ACF_1 est modéré (~0.30 - 0.45). Près du seuil, ACF_1 monte (> 0.65).
        assertTrue(acf1Critical > acf1Resilient + 0.20,
                String.format("Critical ACF(1) (%.4f) must exceed resilient baseline (%.4f) by >= 0.20",
                        acf1Critical, acf1Resilient));
        assertTrue(acf1Critical > 0.65,
                String.format("Near-tipping state must exhibit strong memory retention (ACF_1 = %.4f > 0.65)", acf1Critical));

        // ── Assertion 2: Variance Amplification (Potential Well Flattening) ──
        // La variance quadruple théoriquement quand lambda passe de -4 à -1. On exige au moins +100% (+2x).
        assertTrue(varCritical > varResilient * 2.0,
                String.format("Critical variance (%.4f) must exceed resilient variance (%.4f) by at least 100%% (2x)",
                        varCritical, varResilient));

        // ── Assertion 3: Catastrophic Collapse when Rainfall Falls Below Threshold ──
        assertEquals(0.0, finalBiomassPostTipping, 1e-4,
                "When precipitation drops below critical threshold (mu < 0), biomass must collapse to zero (desert)");
    }

    /**
     * Integrates the non-linear bifurcation dynamics:
     * dx/dt = mu - x^2 + sigma * dW
     * where mu = (rainfall - 400.0) / 100.0
     * with an absorbing lower boundary at x = 0 (biological extinction / desert).
     */
    private double[] simulateVegetationDynamics(double rainfallMm, int steps, double dt, double noiseAmp, long seed) {
        double[] trajectory = new double[steps];
        java.util.Random prng = new java.util.Random(seed);

        double pCrit = 400.0;
        double pScale = 100.0;
        double mu = (rainfallMm - pCrit) / pScale;

        // Equilibrium point for positive mu: x* = sqrt(mu)
        double x = mu > 0.0 ? Math.sqrt(mu) : 0.0;

        // Sub-step integration for numerical fidelity (10 sub-steps per dt)
        int subSteps = 10;
        double subDt = dt / subSteps;

        // Burn-in relaxation
        for (int b = 0; b < 200; b++) {
            double drift = mu - x * x;
            double kick = noiseAmp * prng.nextGaussian() * Math.sqrt(subDt);
            x = Math.max(0.0, x + drift * subDt + kick);
        }

        // Sampling
        for (int i = 0; i < steps; i++) {
            for (int s = 0; s < subSteps; s++) {
                double drift = mu - x * x;
                double kick = noiseAmp * prng.nextGaussian() * Math.sqrt(subDt);
                x = Math.max(0.0, x + drift * subDt + kick);
                if (mu < 0.0 && x < 0.05) {
                    x = 0.0; // Absorbing extinction boundary
                }
            }
            trajectory[i] = x;
        }

        return trajectory;
    }

    private double computeMean(double[] series) {
        double sum = 0.0;
        for (double v : series) sum += v;
        return sum / series.length;
    }

    private double computeVariance(double[] series) {
        double mean = computeMean(series);
        double sumSq = 0.0;
        for (double v : series) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return sumSq / (series.length - 1);
    }

    private double computeLag1Autocorrelation(double[] series) {
        int n = series.length;
        double mean = computeMean(series);
        double numerator = 0.0;
        double denominator = 0.0;

        for (int t = 0; t < n; t++) {
            double diff = series[t] - mean;
            denominator += diff * diff;
            if (t > 0) {
                double diffPrev = series[t - 1] - mean;
                numerator += diff * diffPrev;
            }
        }
        return denominator > 1e-12 ? numerator / denominator : 0.0;
    }
}
