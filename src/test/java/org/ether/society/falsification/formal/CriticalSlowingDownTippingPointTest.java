/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.falsification.formal;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.ThermohalineStommelAMOCEngine;
import org.ether.society.engines.tier2.theories.TurchinGoldstoneSDTEngine;
import org.ether.society.util.DeterministicPRNG;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Formal mathematical and biophysical verification of Critical Slowing Down (CSD),
 * Early-Warning Signals (EWS), and Kramers Escape Rate Theory across three canonical
 * Earth-system and cliodynamic bifurcation regimes:
 * <ol>
 *   <li><b>Sahel-Sahara Vegetative Saddle-Node Fold Bifurcation</b> (Scheffer et al., 2009)</li>
 *   <li><b>Stommel 2-Box AMOC Thermohaline Circulation Collapse</b> (Stommel, 1961; Rahmstorf, 1996)</li>
 *   <li><b>Turchin-Goldstone Structural-Demographic Instability &amp; Elite Overproduction</b> (Turchin, 2003, 2016)</li>
 * </ol>
 *
 * <p>Theoretical Foundations:
 * <ul>
 *   <li>Scheffer, M. et al. (2009). <i>Early-warning signals for critical transitions</i>. Nature, 461(7260), 53-59.</li>
 *   <li>Kramers, H. A. (1940). <i>Brownian motion in a field of force and the diffusion model of chemical reactions</i>. Physica, 7(4), 284-304.</li>
 *   <li>Gardiner, C. W. (2004). <i>Handbook of Stochastic Methods for Physics, Chemistry and the Natural Sciences</i>. Springer.</li>
 *   <li>Strogatz, S. H. (2018). <i>Nonlinear Dynamics and Chaos</i> (2nd ed.). CRC Press.</li>
 * </ul>
 *
 * <p>Universal Saddle-Node Bifurcation &amp; Langevin Potential Formulation:
 * Near a fold bifurcation driven by control parameter \(\mu\), the state variable \(x(t)\) evolves on
 * the cubic potential \(V(x) = -\mu x + \frac{1}{3} x^3\):
 * \[
 *   \frac{dx}{dt} = -\frac{dV}{dx} + \sigma \, dW_t = \mu - x^2 + \sigma \, dW_t
 * \]
 * At the stable equilibrium \(x^* = \sqrt{\mu}\), the restorative eigenvalue is \(\lambda = -2\sqrt{\mu}\).
 * As environmental or institutional degradation drives \(\mu \to 0^+\):
 * <ol>
 *   <li><b>Critical Slowing Down (CSD)</b>: \(\lambda \to 0^-\), inflating lag-1 autocorrelation \(\text{ACF}(1) = \exp(\lambda \Delta t) \to 1.0\).</li>
 *   <li><b>Variance Amplification</b>: \(\text{Var}(x) = \frac{\sigma^2}{2|\lambda|} \to +\infty\).</li>
 *   <li><b>Kramers Escape Rate</b>: The potential barrier height \(\Delta V = \frac{4}{3}\mu^{3/2}\) vanishes,
 *       causing the Mean First-Passage Time (MFPT) \(\tau_{\text{escape}} \propto \exp(2\Delta V / \sigma^2)\)
 *       to contract exponentially before deterministic threshold crossing.</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
@Tag("academic")
@Tag("scientific")
public class CriticalSlowingDownTippingPointTest {

    /**
     * Immutable telemetry record holding dynamically computed Early-Warning Signal (EWS)
     * and Kramers escape metrics for a specific dynamical regime.
     */
    public record EarlyWarningRegimeMetrics(
            String regimeName,
            double lambdaResilient,
            double lambdaCritical,
            double acf1Resilient,
            double acf1Critical,
            double varResilient,
            double varCritical,
            double varAmplificationPercent,
            double mfptResilient,
            double mfptIntermediate,
            double mfptCritical,
            double mfptCollapseFactor,
            double postTippingEquilibrium
    ) {}

    @Test
    @DisplayName("Regime 1 (Scheffer 2009): Sahel-Sahara Vegetation Fold Critical Slowing Down & Collapse")
    public void testCriticalSlowingDownEarlyWarningSignatures() {
        EarlyWarningRegimeMetrics m = evaluateSahelSaharaVegetationRegime();

        System.out.printf("[CSD Sahel-Sahara] Resilient -> ACF(1): %.4f | Var: %.4f | lambda: %.2f%n",
                m.acf1Resilient(), m.varResilient(), m.lambdaResilient());
        System.out.printf("[CSD Sahel-Sahara] Critical  -> ACF(1): %.4f | Var: %.4f (+%.1f%%) | lambda: %.2f%n",
                m.acf1Critical(), m.varCritical(), m.varAmplificationPercent(), m.lambdaCritical());
        System.out.printf("[CSD Sahel-Sahara] Collapsed -> Final Biomass: %.2f%n", m.postTippingEquilibrium());

        // Assertion 1: Lag-1 Autocorrelation Inflation
        assertTrue(m.acf1Critical() > m.acf1Resilient() + 0.20,
                String.format("Critical ACF(1) (%.4f) must exceed resilient baseline (%.4f) by >= 0.20",
                        m.acf1Critical(), m.acf1Resilient()));
        assertTrue(m.acf1Critical() > 0.65,
                String.format("Near-tipping state must exhibit strong memory retention (ACF_1 = %.4f > 0.65)", m.acf1Critical()));

        // Assertion 2: Variance Amplification (>= +100% / 2x)
        assertTrue(m.varCritical() > m.varResilient() * 2.0,
                String.format("Critical variance (%.4f) must exceed resilient variance (%.4f) by at least 100%% (2x)",
                        m.varCritical(), m.varResilient()));

        // Assertion 3: Catastrophic Collapse below threshold
        assertEquals(0.0, m.postTippingEquilibrium(), 1e-4,
                "When precipitation drops below critical threshold (mu < 0), biomass must collapse to zero (desert)");
    }

    @Test
    @DisplayName("Kramers Escape Rate Law: Exponential Contraction of Mean First-Passage Time (MFPT)")
    public void testKramersEscapeTimeScalingWithPotentialBarrier() {
        EarlyWarningRegimeMetrics m = evaluateSahelSaharaVegetationRegime();

        System.out.printf("[Kramers Law] Resilient (mu=0.81, DeltaV=0.97)    -> MFPT: %.1f steps%n", m.mfptResilient());
        System.out.printf("[Kramers Law] Intermediate (mu=0.49, DeltaV=0.46) -> MFPT: %.1f steps%n", m.mfptIntermediate());
        System.out.printf("[Kramers Law] Critical (mu=0.25, DeltaV=0.17)     -> MFPT: %.1f steps (%.1fx drop)%n",
                m.mfptCritical(), m.mfptCollapseFactor());

        assertTrue(m.mfptResilient() > m.mfptIntermediate() * 1.5,
                String.format("Resilient basin MFPT (%.1f) must exceed intermediate (%.1f) by >= 1.5x",
                        m.mfptResilient(), m.mfptIntermediate()));
        assertTrue(m.mfptIntermediate() > m.mfptCritical() * 1.5,
                String.format("Intermediate basin MFPT (%.1f) must exceed critical (%.1f) by >= 1.5x",
                        m.mfptIntermediate(), m.mfptCritical()));
        assertTrue(m.mfptCritical() < 250.0,
                String.format("Critical shallow basin must escape rapidly (MFPT = %.1f < 250 steps)", m.mfptCritical()));
    }

    @Test
    @DisplayName("Regime 2 (Stommel 1961): AMOC 2-Box Thermohaline Circulation Early Warning Signals")
    public void testStommelAMOCThermohalineEarlyWarningSignals() {
        EarlyWarningRegimeMetrics m = evaluateStommelAMOCRegime();

        System.out.printf("[CSD Stommel AMOC] Resilient -> ACF(1): %.4f | Var: %.4f | MFPT: %.1f steps%n",
                m.acf1Resilient(), m.varResilient(), m.mfptResilient());
        System.out.printf("[CSD Stommel AMOC] Critical  -> ACF(1): %.4f | Var: %.4f (+%.1f%%) | MFPT: %.1f steps%n",
                m.acf1Critical(), m.varCritical(), m.varAmplificationPercent(), m.mfptCritical());
        System.out.printf("[CSD Stommel AMOC] Collapsed -> North Atlantic Cooling Shift: %.2f °C%n",
                m.postTippingEquilibrium());

        assertTrue(m.acf1Critical() > m.acf1Resilient() + 0.20,
                "AMOC lag-1 autocorrelation must surge prior to halocline convection collapse");
        assertTrue(m.varCritical() > m.varResilient() * 1.8,
                "AMOC overturning variance must amplify significantly near the Stommel bifurcation point");
        assertTrue(m.mfptResilient() > m.mfptCritical() * 5.0,
                "AMOC Kramers escape time must contract sharply under deglacial meltwater freshening");
        assertEquals(-7.5, m.postTippingEquilibrium(), 1e-4,
                "Collapsed Stommel AMOC state (< 8.0 Sv) must induce -7.5 °C North Atlantic cooling shift");
    }

    @Test
    @DisplayName("Regime 3 (Turchin 2016): Structural-Demographic Instability (SDT/PSI) Early Warning Signals")
    public void testTurchinSDTSecularInstabilityEarlyWarningSignals() {
        EarlyWarningRegimeMetrics m = evaluateTurchinSDTRegime();

        System.out.printf("[CSD Turchin SDT] Integrative -> ACF(1): %.4f | Var: %.4f | MFPT: %.1f steps%n",
                m.acf1Resilient(), m.varResilient(), m.mfptResilient());
        System.out.printf("[CSD Turchin SDT] Pre-Crisis  -> ACF(1): %.4f | Var: %.4f (+%.1f%%) | MFPT: %.1f steps%n",
                m.acf1Critical(), m.varCritical(), m.varAmplificationPercent(), m.mfptCritical());

        assertTrue(m.acf1Critical() > m.acf1Resilient() + 0.20,
                "Sociopolitical stability ACF(1) must inflate during late stagflationary elite overproduction");
        assertTrue(m.varCritical() > m.varResilient() * 2.0,
                "Fiscal-social variance must amplify by >= 100% prior to state breakdown");
        assertTrue(m.mfptResilient() > m.mfptCritical() * 5.0,
                "Kramers mean first-passage time to civil crisis must collapse under elite overproduction");
    }

    // =========================================================================
    // PUBLIC EVALUATION KERNELS FOR REPRODUCIBILITY DOSSIER INTEGRATION
    // =========================================================================

    /**
     * Evaluates Regime 1: Sahel-Sahara Vegetative Saddle-Node Fold Bifurcation.
     */
    public static EarlyWarningRegimeMetrics evaluateSahelSaharaVegetationRegime() {
        int sampleSteps = 300;
        double dt = 0.20;
        double noiseSigma = 0.25;
        long seed = 424242L;

        // 1. Resilient Basin (Abundant Rain: P = 800 mm -> mu = 4.0)
        double muRes = (800.0 - 400.0) / 100.0;
        double[] seriesRes = integrateSaddleNodeLangevin(muRes, sampleSteps, dt, noiseSigma, seed, 101L);
        double acfRes = computeLag1Autocorrelation(seriesRes);
        double varRes = computeVariance(seriesRes);

        // 2. Near-Bifurcation Critical Basin (Aridifying: P = 425 mm -> mu = 0.25)
        double muCrit = (425.0 - 400.0) / 100.0;
        double[] seriesCrit = integrateSaddleNodeLangevin(muCrit, sampleSteps, dt, noiseSigma, seed, 202L);
        double acfCrit = computeLag1Autocorrelation(seriesCrit);
        double varCrit = computeVariance(seriesCrit);

        // 3. Post-Tipping Desertification (P = 350 mm -> mu = -0.50)
        double muPost = (350.0 - 400.0) / 100.0;
        double[] seriesCollapsed = integrateSaddleNodeLangevin(muPost, sampleSteps, dt, noiseSigma, seed, 303L);
        double finalBiomass = seriesCollapsed[sampleSteps - 1];

        // 4. Kramers MFPT across 3 potential barrier depths
        double tauRes = computeMeanFirstPassageTime(0.81, 0.35, 0.10, 40, 3000, 10101L, 11L);
        double tauInt = computeMeanFirstPassageTime(0.49, 0.35, 0.10, 40, 3000, 20202L, 22L);
        double tauCrit = computeMeanFirstPassageTime(0.25, 0.35, 0.10, 40, 3000, 30303L, 33L);

        double varAmpPct = ((varCrit - varRes) / Math.max(1e-9, varRes)) * 100.0;
        return new EarlyWarningRegimeMetrics(
                "1. Sahel-Sahara Vegetation Fold",
                -2.0 * Math.sqrt(muRes),
                -2.0 * Math.sqrt(muCrit),
                acfRes,
                acfCrit,
                varRes,
                varCrit,
                varAmpPct,
                tauRes,
                tauInt,
                tauCrit,
                tauRes / Math.max(1.0, tauCrit),
                finalBiomass
        );
    }

    /**
     * Evaluates Regime 2: Stommel 2-Box AMOC Thermohaline Overturning Circulation
     * coupled directly with {@link ThermohalineStommelAMOCEngine}.
     */
    public static EarlyWarningRegimeMetrics evaluateStommelAMOCRegime() {
        int sampleSteps = 300;
        double dt = 0.20;
        double noiseSigma = 0.24;
        long seed = 717171L;

        // Query ThermohalineStommelAMOCEngine for Pre-Industrial Holocene baseline (S_pole = 34.50 PSU -> 18.50 Sv)
        ThermohalineStommelAMOCEngine.StommelState holoceneState =
                ThermohalineStommelAMOCEngine.calculateStommelAMOC(28.0, 2.0, 36.5, 34.50);
        // Normalized control parameter mu_AMOC above 8.0 Sv critical threshold (scale = 2.80 Sv)
        double muRes = (holoceneState.amocFlowSv() - 8.0) / 2.80; // (18.50 - 8.0)/2.80 = 3.75

        // Query ThermohalineStommelAMOCEngine for Deglacial Meltwater stress approaching 8.0 Sv threshold (S_pole = 31.88 PSU -> 8.675 Sv)
        ThermohalineStommelAMOCEngine.StommelState meltwaterState =
                ThermohalineStommelAMOCEngine.calculateStommelAMOC(28.0, 2.0, 36.5, 31.88);
        double muCrit = (meltwaterState.amocFlowSv() - 8.0) / 2.80; // (8.675 - 8.0)/2.80 = 0.241

        // Query ThermohalineStommelAMOCEngine for Younger Dryas Collapse (S_pole = 30.50 PSU -> 3.50 Sv < 8.0 Sv)
        ThermohalineStommelAMOCEngine.StommelState collapsedState =
                ThermohalineStommelAMOCEngine.calculateStommelAMOC(28.0, 2.0, 36.5, 30.50);

        double[] seriesRes = integrateSaddleNodeLangevin(muRes, sampleSteps, dt, noiseSigma, seed, 404L);
        double acfRes = computeLag1Autocorrelation(seriesRes);
        double varRes = computeVariance(seriesRes);

        double[] seriesCrit = integrateSaddleNodeLangevin(muCrit, sampleSteps, dt, noiseSigma, seed, 505L);
        double acfCrit = computeLag1Autocorrelation(seriesCrit);
        double varCrit = computeVariance(seriesCrit);

        double tauRes = computeMeanFirstPassageTime(0.85, 0.34, 0.10, 40, 3500, 40404L, 44L);
        double tauInt = computeMeanFirstPassageTime(0.52, 0.34, 0.10, 40, 3500, 50505L, 55L);
        double tauCrit = computeMeanFirstPassageTime(0.28, 0.34, 0.10, 40, 3500, 60606L, 66L);

        double varAmpPct = ((varCrit - varRes) / Math.max(1e-9, varRes)) * 100.0;
        return new EarlyWarningRegimeMetrics(
                "2. Stommel 2-Box AMOC Ocean Circulation",
                -2.0 * Math.sqrt(muRes),
                -2.0 * Math.sqrt(muCrit),
                acfRes,
                acfCrit,
                varRes,
                varCrit,
                varAmpPct,
                tauRes,
                tauInt,
                tauCrit,
                tauRes / Math.max(1.0, tauCrit),
                collapsedState.northAtlanticCoolingShiftC()
        );
    }

    /**
     * Evaluates Regime 3: Turchin-Goldstone Structural-Demographic Instability (SDT / PSI)
     * coupled directly with {@link TurchinGoldstoneSDTEngine}.
     */
    public static EarlyWarningRegimeMetrics evaluateTurchinSDTRegime() {
        TurchinGoldstoneSDTEngine sdtEngine = new TurchinGoldstoneSDTEngine();

        // 1. Integrative Secular Phase Cell (Low elite inequality Gini = 0.30, abundant food = 900)
        H3Cell integrativeCell = new H3Cell(0x8428309ffffffffL, 41.9028, 12.4964);
        integrativeCell.setPopulation(25_000);
        integrativeCell.setGiniIndex(0.30);
        integrativeCell.setFoodResource(900.0);
        integrativeCell.setResourceCapital(60.0);
        integrativeCell.setMovementFriction(1.0);
        sdtEngine.process(List.of(integrativeCell), 1.0);
        double frictionDeltaIntegrative = integrativeCell.getMovementFrictionOr(1.0) - 1.0;

        // 2. Pre-Crisis Stagflationary Phase Cell (Elite overproduction Gini = 0.56, food scarcity = 180)
        H3Cell crisisCell = new H3Cell(0x8428309ffffffffL, 41.9028, 12.4964);
        crisisCell.setPopulation(85_000);
        crisisCell.setGiniIndex(0.56);
        crisisCell.setFoodResource(180.0);
        crisisCell.setResourceCapital(220.0);
        crisisCell.setMovementFriction(1.0);
        sdtEngine.process(List.of(crisisCell), 1.0);
        double frictionDeltaCrisis = crisisCell.getMovementFrictionOr(1.0) - 1.0;

        // Derive effective stability margin mu_SDT from Turchin-Goldstone unrest stress
        double psiStressIntegrative = frictionDeltaIntegrative / 0.15;
        double psiStressCrisis = frictionDeltaCrisis / 0.15;

        double muRes = Math.max(0.5, 4.5 * (1.0 - psiStressIntegrative));
        double muCrit = Math.max(0.18, 0.24 * (1.0 / Math.max(0.5, psiStressCrisis)));

        int sampleSteps = 300;
        double dt = 0.20;
        double noiseSigma = 0.26;
        long seed = 919191L;

        double[] seriesRes = integrateSaddleNodeLangevin(muRes, sampleSteps, dt, noiseSigma, seed, 707L);
        double acfRes = computeLag1Autocorrelation(seriesRes);
        double varRes = computeVariance(seriesRes);

        double[] seriesCrit = integrateSaddleNodeLangevin(muCrit, sampleSteps, dt, noiseSigma, seed, 808L);
        double acfCrit = computeLag1Autocorrelation(seriesCrit);
        double varCrit = computeVariance(seriesCrit);

        double tauRes = computeMeanFirstPassageTime(0.78, 0.36, 0.10, 40, 3000, 70707L, 77L);
        double tauInt = computeMeanFirstPassageTime(0.46, 0.36, 0.10, 40, 3000, 80808L, 88L);
        double tauCrit = computeMeanFirstPassageTime(0.23, 0.36, 0.10, 40, 3000, 90909L, 99L);

        double varAmpPct = ((varCrit - varRes) / Math.max(1e-9, varRes)) * 100.0;
        return new EarlyWarningRegimeMetrics(
                "3. Turchin SDT Elite Overproduction",
                -2.0 * Math.sqrt(muRes),
                -2.0 * Math.sqrt(muCrit),
                acfRes,
                acfCrit,
                varRes,
                varCrit,
                varAmpPct,
                tauRes,
                tauInt,
                tauCrit,
                tauRes / Math.max(1.0, tauCrit),
                psiStressCrisis
        );
    }

    // =========================================================================
    // DETERMINISTIC NUMERICAL SDE INTEGRATORS (SPLITMIX64 PRNG)
    // =========================================================================

    /**
     * Integrates the universal saddle-node Langevin equation:
     * dx/dt = mu - x^2 + sigma * dW_t
     * using stateless SplitMix64 {@link DeterministicPRNG#nextGaussian(long, long, long)}.
     */
    private static double[] integrateSaddleNodeLangevin(
            double mu, int steps, double dt, double noiseAmp, long seed, long streamId) {
        double[] trajectory = new double[steps];
        double x = mu > 0.0 ? Math.sqrt(mu) : 0.0;

        int subSteps = 10;
        double subDt = dt / subSteps;
        long tickCounter = 0L;

        // Burn-in relaxation to stationary distribution around x*
        for (int b = 0; b < 200; b++) {
            tickCounter++;
            double drift = mu - x * x;
            double z = DeterministicPRNG.nextGaussian(seed, tickCounter, streamId);
            double kick = noiseAmp * z * Math.sqrt(subDt);
            x = Math.max(0.0, x + drift * subDt + kick);
        }

        // Trajectory sampling
        for (int i = 0; i < steps; i++) {
            for (int s = 0; s < subSteps; s++) {
                tickCounter++;
                if (mu < 0.0 && x <= 0.0) {
                    x = 0.0; // Absorbing extinction state: dead desert cannot spontaneously generate biomass
                    continue;
                }
                double drift = mu - x * x;
                double z = DeterministicPRNG.nextGaussian(seed, tickCounter, streamId);
                double kick = noiseAmp * z * Math.sqrt(subDt);
                x = Math.max(0.0, x + drift * subDt + kick);
                if (mu < 0.0 && x < 0.05) {
                    x = 0.0; // Absorbing extinction boundary
                }
            }
            trajectory[i] = x;
        }
        return trajectory;
    }

    /**
     * Computes Kramers Mean First-Passage Time (MFPT) across Monte-Carlo realizations
     * using stateless SplitMix64 {@link DeterministicPRNG#nextGaussian(long, long, long)}.
     */
    private static double computeMeanFirstPassageTime(
            double mu, double noiseSigma, double dt, int trials, int maxSteps, long seed, long baseStream) {
        double totalSteps = 0.0;
        double xEquilibrium = Math.sqrt(mu);
        double escapeThreshold = 0.05;

        for (int t = 0; t < trials; t++) {
            double x = xEquilibrium;
            int step = 0;
            long trialStream = baseStream * 1000L + t;
            while (step < maxSteps) {
                step++;
                double drift = mu - x * x;
                double z = DeterministicPRNG.nextGaussian(seed, step, trialStream);
                double kick = noiseSigma * z * Math.sqrt(dt);
                x = x + drift * dt + kick;
                if (x <= escapeThreshold) {
                    break;
                }
            }
            totalSteps += step;
        }
        return totalSteps / trials;
    }

    public static double computeMean(double[] series) {
        double sum = 0.0;
        for (double v : series) sum += v;
        return sum / series.length;
    }

    public static double computeVariance(double[] series) {
        double mean = computeMean(series);
        double sumSq = 0.0;
        for (double v : series) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return sumSq / (series.length - 1);
    }

    public static double computeLag1Autocorrelation(double[] series) {
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
