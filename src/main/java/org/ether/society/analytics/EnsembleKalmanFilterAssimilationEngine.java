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
import java.util.Random;

/**
 * Sequential Data Assimilation via Ensemble Kalman Filter (EnKF).
 *
 * <p>Integrates empirical historical time-series checkpoints (HYDE 3.4, Seshat, Maddison)
 * into a running ensemble of $M$ parallel planetary simulation trajectories:</p>
 *
 * <pre>
 *   Forecast:   x_i^{f}(t_k) = f(x_i^{a}(t_{k-1}))
 *   Kalman Gain: K_k = P_k^{f} H^T (H P_k^{f} H^T + R)^{-1}
 *   Analysis:   x_i^{a}(t_k) = x_i^{f}(t_k) + K_k [ y_k + ε_i - H x_i^{f}(t_k) ]
 * </pre>
 *
 * <p>Computes the <b>Epistemic Discrepancy Metric</b> &Omega;<sub>k</sub> = ||K<sub>k</sub> (y<sub>k</sub> - H x̄<sup>f</sup>)||,
 * diagnosing the degree of structural resistance between physical model predictions and historical empirical ground truth.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class EnsembleKalmanFilterAssimilationEngine {
    private static final Logger logger = LoggerFactory.getLogger(EnsembleKalmanFilterAssimilationEngine.class);

    private final int ensembleSize;
    private final double measurementVariance;
    private final Random random;

    private final List<Double> epistemicDiscrepancyHistory = new ArrayList<>();
    private long assimilationCycles = 0;

    public record AssimilationResult(
            double[] ensembleMeanForecast,
            double[] ensembleMeanAnalysis,
            double[] innovation,
            double epistemicDiscrepancy,
            double varianceReduction
    ) {}

    public EnsembleKalmanFilterAssimilationEngine() {
        this(20, 0.05, new Random(1234));
    }

    public EnsembleKalmanFilterAssimilationEngine(int ensembleSize, double measurementVariance, Random random) {
        this.ensembleSize = Math.max(5, ensembleSize);
        this.measurementVariance = Math.max(1e-6, measurementVariance);
        this.random = random != null ? random : new Random(1234);
    }

    /**
     * Executes a sequential EnKF analysis update over an ensemble of state vectors.
     *
     * @param ensembleForecast array of $M$ forecast state vectors (each of dimension $N$)
     * @param observation observed historical data vector (dimension $N$)
     * @return updated ensemble analysis state vectors and assimilation diagnostic metrics
     */
    public AssimilationResult assimilate(double[][] ensembleForecast, double[] observation) {
        if (ensembleForecast == null || ensembleForecast.length == 0 || observation == null) {
            throw new IllegalArgumentException("Ensemble and observation vectors must not be null or empty.");
        }

        int m = ensembleForecast.length;
        int n = observation.length;

        // 1. Compute Forecast Ensemble Mean
        double[] meanForecast = new double[n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                meanForecast[j] += ensembleForecast[i][j];
            }
        }
        for (int j = 0; j < n; j++) {
            meanForecast[j] /= m;
        }

        // 2. Compute Forecast Variance per state dimension
        double[] forecastVar = new double[n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                double diff = ensembleForecast[i][j] - meanForecast[j];
                forecastVar[j] += diff * diff;
            }
        }
        for (int j = 0; j < n; j++) {
            forecastVar[j] /= Math.max(1, m - 1);
        }

        // 3. Compute Scalar Kalman Gain per component: K_j = Var_f / (Var_f + R)
        double[] kalmanGain = new double[n];
        double[] innovation = new double[n];
        for (int j = 0; j < n; j++) {
            kalmanGain[j] = forecastVar[j] / (forecastVar[j] + measurementVariance);
            innovation[j] = observation[j] - meanForecast[j];
        }

        // 4. Update each ensemble member with perturbed observations
        double[][] ensembleAnalysis = new double[m][n];
        double[] meanAnalysis = new double[n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                double perturbedObs = observation[j] + random.nextGaussian() * Math.sqrt(measurementVariance);
                double residual = perturbedObs - ensembleForecast[i][j];
                ensembleAnalysis[i][j] = ensembleForecast[i][j] + kalmanGain[j] * residual;
                meanAnalysis[j] += ensembleAnalysis[i][j];
            }
        }
        for (int j = 0; j < n; j++) {
            meanAnalysis[j] /= m;
        }

        // 5. Compute Analysis Variance
        double[] analysisVar = new double[n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                double diff = ensembleAnalysis[i][j] - meanAnalysis[j];
                analysisVar[j] += diff * diff;
            }
        }
        for (int j = 0; j < n; j++) {
            analysisVar[j] /= Math.max(1, m - 1);
        }

        // 6. Compute Epistemic Discrepancy: Euclidean norm of mean shift
        double discrepancySq = 0.0;
        double sumVarF = 0.0;
        double sumVarA = 0.0;
        for (int j = 0; j < n; j++) {
            double shift = meanAnalysis[j] - meanForecast[j];
            discrepancySq += shift * shift;
            sumVarF += forecastVar[j];
            sumVarA += analysisVar[j];
        }
        double epistemicDiscrepancy = Math.sqrt(discrepancySq);
        double varReduction = sumVarF > 0 ? (1.0 - sumVarA / sumVarF) : 0.0;

        epistemicDiscrepancyHistory.add(epistemicDiscrepancy);
        assimilationCycles++;

        // Copy updated states back into input forecast array
        for (int i = 0; i < m; i++) {
            System.arraycopy(ensembleAnalysis[i], 0, ensembleForecast[i], 0, n);
        }

        return new AssimilationResult(meanForecast, meanAnalysis, innovation, epistemicDiscrepancy, varReduction);
    }

    public int getEnsembleSize() { return ensembleSize; }
    public double getMeasurementVariance() { return measurementVariance; }
    public long getAssimilationCycles() { return assimilationCycles; }
    public List<Double> getEpistemicDiscrepancyHistory() { return List.copyOf(epistemicDiscrepancyHistory); }
}
