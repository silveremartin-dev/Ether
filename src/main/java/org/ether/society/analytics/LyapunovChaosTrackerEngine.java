/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Real-Time Maximal Lyapunov Exponent (&lambda;<sub>max</sub>) Tracking Engine.
 *
 * <p>Employs the Benettin-Wolf tangent perturbation method to continuously quantify
 * sensitive dependence on initial conditions and distinguish true physical/cliodynamic chaos
 * from numerical integration artifacts:</p>
 *
 * <pre>
 *   &lambda;<sub>max</sub> = lim_{t &rarr; &infin;} (1/t) &sum; ln(||&delta;x(t<sub>k</sub>)|| / ||&delta;x(0)||)
 * </pre>
 *
 * <ul>
 *   <li><b>&lambda; &lt; 0</b>: Asymptotic stability / fixed-point contraction (e.g. static carrying capacity equilibrium).</li>
 *   <li><b>&lambda; &approx; 0</b>: Conservative / periodic limit cycle (e.g. pure seasonal oscillations).</li>
 *   <li><b>&lambda; &gt; 0</b>: Deterministic chaos (e.g. non-linear Turchin structural demographic bifurcations).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class LyapunovChaosTrackerEngine {
    private static final Logger logger = LoggerFactory.getLogger(LyapunovChaosTrackerEngine.class);

    private final double initialPerturbationMagnitude;
    private final List<Double> lyapunovHistory = new ArrayList<>();
    private double cumulativeLyapunovSum = 0.0;
    private long stepCount = 0;
    private double currentLyapunovExponent = 0.0;

    public enum DynamicRegime {
        STABLE_ATTRACTOR,
        LIMIT_CYCLE,
        DETERMINISTIC_CHAOS
    }

    public LyapunovChaosTrackerEngine() {
        this(1e-6);
    }

    public LyapunovChaosTrackerEngine(double initialPerturbationMagnitude) {
        this.initialPerturbationMagnitude = Math.max(1e-12, initialPerturbationMagnitude);
    }

    /**
     * Updates the Lyapunov estimation given the base state vector and perturbed shadow state vector.
     *
     * @param baseState base simulation state vector
     * @param shadowState perturbed simulation state vector
     * @param deltaT time step elapsed (in years or days)
     * @return updated shadow state vector rescaled to initial perturbation magnitude along perturbation axis
     */
    public double[] trackStep(double[] baseState, double[] shadowState, double deltaT) {
        if (baseState == null || shadowState == null || baseState.length != shadowState.length || deltaT <= 0) {
            return shadowState;
        }

        int n = baseState.length;
        double normSq = 0.0;
        double[] delta = new double[n];

        for (int i = 0; i < n; i++) {
            delta[i] = shadowState[i] - baseState[i];
            normSq += delta[i] * delta[i];
        }

        double currentNorm = Math.sqrt(normSq);
        if (currentNorm < 1e-15) {
            // Re-seed perturbation if collapsed to zero
            delta[0] = initialPerturbationMagnitude;
            currentNorm = initialPerturbationMagnitude;
        }

        // Instantaneous expansion rate
        double expansionRatio = currentNorm / initialPerturbationMagnitude;
        double localExponent = Math.log(Math.max(1e-12, expansionRatio)) / deltaT;

        cumulativeLyapunovSum += localExponent;
        stepCount++;
        currentLyapunovExponent = cumulativeLyapunovSum / stepCount;
        lyapunovHistory.add(currentLyapunovExponent);

        // Gram-Schmidt / Benettin rescaling: bring perturbation back to initial magnitude along same direction
        double rescaleFactor = initialPerturbationMagnitude / currentNorm;
        double[] rescaledShadow = new double[n];
        for (int i = 0; i < n; i++) {
            rescaledShadow[i] = baseState[i] + delta[i] * rescaleFactor;
        }

        return rescaledShadow;
    }

    /**
     * Classifies the current dynamic regime based on the estimated maximal Lyapunov exponent.
     *
     * @return dynamic regime classification
     */
    public DynamicRegime classifyRegime() {
        if (currentLyapunovExponent > 0.05) {
            return DynamicRegime.DETERMINISTIC_CHAOS;
        } else if (currentLyapunovExponent < -0.05) {
            return DynamicRegime.STABLE_ATTRACTOR;
        } else {
            return DynamicRegime.LIMIT_CYCLE;
        }
    }

    public double getInitialPerturbationMagnitude() { return initialPerturbationMagnitude; }
    public double getCurrentLyapunovExponent() { return currentLyapunovExponent; }
    public long getStepCount() { return stepCount; }
    public List<Double> getLyapunovHistory() { return List.copyOf(lyapunovHistory); }
    public void reset() {
        lyapunovHistory.clear();
        cumulativeLyapunovSum = 0.0;
        stepCount = 0;
        currentLyapunovExponent = 0.0;
    }
}
