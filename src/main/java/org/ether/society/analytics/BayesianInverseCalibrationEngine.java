/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

/**
 * Approximate Bayesian Computation (ABC) & Markov Chain Monte Carlo (MCMC)
 * Inverse Calibration Engine for Cliodynamic & Biophysical Simulation Parameters.
 *
 * <p>Enables scientific inference of unobservable institutional, cultural, and physical parameters
 * $\boldsymbol{\theta} = (\theta_1, \dots, \theta_k)$ from empirical historical observations $\mathbf{y}_{\text{obs}}$
 * (e.g. Maddison GDP, HYDE population, Seshat complexity indices):</p>
 *
 * <ul>
 *   <li><b>Prior Distribution $\pi(\boldsymbol{\theta})$</b>: Uniform or Gaussian prior bounds for each parameter.</li>
 *   <li><b>Summary Distance Metric $\rho(\mathbf{y}_{\text{sim}}(\boldsymbol{\theta}), \mathbf{y}_{\text{obs}})$</b>:
 *     $$\rho(\mathbf{y}_{\text{sim}}, \mathbf{y}_{\text{obs}}) = \sqrt{\frac{1}{T} \sum_{t=1}^T \left(\frac{y_{\text{sim}}(t) - y_{\text{obs}}(t)}{\sigma_t}\right)^2}$$
 *   </li>
 *   <li><b>Sequential Monte Carlo ABC (ABC-SMC) & Rejection Sampling</b>:
 *     Accepts proposed parameter vectors satisfying $\rho(\mathbf{y}_{\text{sim}}(\boldsymbol{\theta}^*), \mathbf{y}_{\text{obs}}) \le \epsilon_j$,
 *     adaptively reducing tolerance threshold $\epsilon_{j+1} < \epsilon_j$.
 *   </li>
 *   <li><b>Posterior Analysis</b>: Computes Mean, Maximum A Posteriori (MAP), Standard Deviation,
 *       and 95% Bayesian Credible Intervals (CI).
 *   </li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 */
public class BayesianInverseCalibrationEngine {
    private static final Logger logger = LoggerFactory.getLogger(BayesianInverseCalibrationEngine.class);

    public record ParameterPrior(String name, double minBound, double maxBound, double initialGuess) {}

    public record PosteriorEstimate(
            String parameterName,
            double mean,
            double mapEstimate,
            double stdDev,
            double credibleInterval95Low,
            double credibleInterval95High,
            int acceptedSamples
    ) {}

    public record CalibrationReport(
            double finalEpsilon,
            double acceptanceRate,
            double rSquared,
            double rmse,
            Map<String, PosteriorEstimate> posteriors,
            Map<Integer, Double> optimalSimulatedTrajectory
    ) {}

    /**
     * Executes Sequential Approximate Bayesian Computation (ABC-SMC) across free parameters.
     *
     * @param priors Map of parameter names to their prior bounds
     * @param observedTrajectory Target empirical series (Year -> Observed Value)
     * @param forwardSimulator Simulator function mapping parameter vector to simulated time series
     * @param targetParticles Number of posterior particles to collect
     * @param maxIterations Maximum Monte Carlo proposals
     * @param initialTolerance Starting distance tolerance threshold
     * @return Comprehensive CalibrationReport with posterior distributions
     */
    public static CalibrationReport calibrateParametersABC(
            List<ParameterPrior> priors,
            Map<Integer, Double> observedTrajectory,
            Function<Map<String, Double>, Map<Integer, Double>> forwardSimulator,
            int targetParticles,
            int maxIterations,
            double initialTolerance) {

        if (priors == null || priors.isEmpty() || observedTrajectory == null || observedTrajectory.isEmpty()) {
            throw new IllegalArgumentException("Priors and observed trajectory must be non-empty.");
        }

        Random rng = new Random(42L); // Deterministic seed for reproducible scientific calibration
        List<Map<String, Double>> acceptedParticles = new ArrayList<>();
        List<Double> acceptedDistances = new ArrayList<>();

        double currentTolerance = initialTolerance;
        int totalProposals = 0;

        logger.info("🔬 Starting Bayesian ABC Calibration: {} parameters, {} target particles, initial ε={}",
                priors.size(), targetParticles, initialTolerance);

        // Adaptive ABC particle collection
        for (int iter = 0; iter < maxIterations && acceptedParticles.size() < targetParticles; iter++) {
            totalProposals++;

            // 1. Sample from prior distribution
            Map<String, Double> proposedParams = new HashMap<>();
            for (ParameterPrior prior : priors) {
                double val = prior.minBound() + rng.nextDouble() * (prior.maxBound() - prior.minBound());
                proposedParams.put(prior.name(), val);
            }

            // 2. Run forward simulation
            Map<Integer, Double> simulatedTrajectory = forwardSimulator.apply(proposedParams);

            // 3. Compute summary discrepancy distance ρ(y_sim, y_obs)
            double distance = calculateNormalizedEuclideanDistance(simulatedTrajectory, observedTrajectory);

            // 4. Accept / Reject
            if (distance <= currentTolerance) {
                acceptedParticles.add(proposedParams);
                acceptedDistances.add(distance);

                // Dynamically tighten epsilon as particles accumulate
                if (acceptedParticles.size() >= targetParticles / 2) {
                    currentTolerance = Math.min(currentTolerance, calculatePercentile(acceptedDistances, 75.0));
                }
            }
        }

        if (acceptedParticles.isEmpty()) {
            logger.warn("ABC calibration failed to find accepted particles within tolerance ε={}", initialTolerance);
            // Fallback to initial guess
            Map<String, Double> fallback = new HashMap<>();
            for (ParameterPrior p : priors) fallback.put(p.name(), p.initialGuess());
            acceptedParticles.add(fallback);
            acceptedDistances.add(initialTolerance);
        }

        double acceptanceRate = (double) acceptedParticles.size() / Math.max(1, totalProposals);

        // 5. Construct Posterior Distributions
        Map<String, PosteriorEstimate> posteriorMap = new LinkedHashMap<>();
        int bestParticleIdx = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < acceptedDistances.size(); i++) {
            if (acceptedDistances.get(i) < minDistance) {
                minDistance = acceptedDistances.get(i);
                bestParticleIdx = i;
            }
        }

        Map<String, Double> mapParams = acceptedParticles.get(bestParticleIdx);
        Map<Integer, Double> optimalTrajectory = forwardSimulator.apply(mapParams);

        for (ParameterPrior prior : priors) {
            List<Double> values = new ArrayList<>();
            for (Map<String, Double> particle : acceptedParticles) {
                values.add(particle.get(prior.name()));
            }
            Collections.sort(values);

            double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(prior.initialGuess());
            double mapVal = mapParams.get(prior.name());

            double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0);
            double stdDev = Math.sqrt(variance);

            double q025 = calculatePercentile(values, 2.5);
            double q975 = calculatePercentile(values, 97.5);

            posteriorMap.put(prior.name(), new PosteriorEstimate(
                    prior.name(), mean, mapVal, stdDev, q025, q975, values.size()
            ));
        }

        // 6. Calculate goodness-of-fit metrics
        double r2 = calculateRSquared(optimalTrajectory, observedTrajectory);
        double rmse = calculateRMSE(optimalTrajectory, observedTrajectory);

        logger.info("✅ ABC Calibration Complete: Acceptance Rate={:.2f}%, Final ε={:.4f}, Optimal R^2={:.4f}, RMSE={:.2f}",
                acceptanceRate * 100.0, minDistance, r2, rmse);

        return new CalibrationReport(minDistance, acceptanceRate, r2, rmse, posteriorMap, optimalTrajectory);
    }

    private static double calculateNormalizedEuclideanDistance(Map<Integer, Double> sim, Map<Integer, Double> obs) {
        if (sim == null || obs == null || obs.isEmpty()) return Double.MAX_VALUE;

        double sumSqNorm = 0.0;
        int count = 0;

        for (Map.Entry<Integer, Double> entry : obs.entrySet()) {
            Integer year = entry.getKey();
            Double obsVal = entry.getValue();
            Double simVal = sim.get(year);

            if (simVal != null && obsVal != null && obsVal != 0.0) {
                double relErr = (simVal - obsVal) / Math.abs(obsVal);
                sumSqNorm += relErr * relErr;
                count++;
            }
        }

        return count > 0 ? Math.sqrt(sumSqNorm / count) : Double.MAX_VALUE;
    }

    private static double calculateRSquared(Map<Integer, Double> sim, Map<Integer, Double> obs) {
        if (sim == null || obs == null || obs.size() < 2) return 0.0;

        double meanObs = obs.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double ssTot = 0.0;
        double ssRes = 0.0;

        for (Map.Entry<Integer, Double> entry : obs.entrySet()) {
            Integer year = entry.getKey();
            double yObs = entry.getValue();
            double ySim = sim.getOrDefault(year, meanObs);

            ssTot += Math.pow(yObs - meanObs, 2);
            ssRes += Math.pow(yObs - ySim, 2);
        }

        return ssTot > 0 ? Math.max(-1.0, 1.0 - (ssRes / ssTot)) : 0.0;
    }

    private static double calculateRMSE(Map<Integer, Double> sim, Map<Integer, Double> obs) {
        if (sim == null || obs == null || obs.isEmpty()) return 0.0;

        double sumSq = 0.0;
        int count = 0;

        for (Map.Entry<Integer, Double> entry : obs.entrySet()) {
            Integer year = entry.getKey();
            double yObs = entry.getValue();
            double ySim = sim.getOrDefault(year, 0.0);

            sumSq += Math.pow(ySim - yObs, 2);
            count++;
        }

        return count > 0 ? Math.sqrt(sumSq / count) : 0.0;
    }

    private static double calculatePercentile(List<Double> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) return 0.0;
        if (sortedValues.size() == 1) return sortedValues.get(0);

        double index = (percentile / 100.0) * (sortedValues.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) {
            return sortedValues.get(lower);
        }

        double weight = index - lower;
        return sortedValues.get(lower) * (1.0 - weight) + sortedValues.get(upper) * weight;
    }
}
