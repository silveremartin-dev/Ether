/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Approximate Bayesian Computation (ABC) inverse parameter inference.
 */
public class BayesianInverseCalibrationEngineTest {

    @Test
    @DisplayName("Validate ABC parameter estimation, covariance matrix, and credible intervals on Boserup trajectory")
    void testBayesianABCCalibrationOnBoserupTrajectory() {
        // True unobservable parameters
        final double trueAlphaBoserup = 0.035;
        final double trueLaborDrag = 0.015;

        // Generate synthetic historical observation from true parameters
        Map<Integer, Double> observedTrajectory = new LinkedHashMap<>();
        double currentYield = 100.0;
        for (int year = 1000; year <= 1500; year += 50) {
            observedTrajectory.put(year, currentYield);
            double popDensity = 10.0 + (year - 1000) * 0.15;
            currentYield *= (1.0 + (trueAlphaBoserup * Math.log(popDensity) - trueLaborDrag));
        }

        // Define Bayesian priors
        List<BayesianInverseCalibrationEngine.ParameterPrior> priors = List.of(
                new BayesianInverseCalibrationEngine.ParameterPrior("alphaBoserup", 0.005, 0.080, 0.020),
                new BayesianInverseCalibrationEngine.ParameterPrior("laborDrag", 0.001, 0.050, 0.010)
        );

        // Forward simulator function mapping parameters to simulated time series
        BayesianInverseCalibrationEngine.CalibrationReport report =
                BayesianInverseCalibrationEngine.calibrateParametersABC(
                        priors,
                        observedTrajectory,
                        params -> {
                            double a = params.get("alphaBoserup");
                            double d = params.get("laborDrag");
                            Map<Integer, Double> sim = new LinkedHashMap<>();
                            double y = 100.0;
                            for (int year = 1000; year <= 1500; year += 50) {
                                sim.put(year, y);
                                double popDensity = 10.0 + (year - 1000) * 0.15;
                                y *= (1.0 + (a * Math.log(popDensity) - d));
                            }
                            return sim;
                        },
                        100,
                        5000,
                        0.25
                );

        assertNotNull(report);
        assertTrue(report.rSquared() > 0.95, "Optimal MAP parameters must achieve R^2 > 0.95 on historical series");
        assertTrue(report.rmse() < 25.0, "Optimal trajectory RMSE must be bounded");

        BayesianInverseCalibrationEngine.PosteriorEstimate alphaPost = report.posteriors().get("alphaBoserup");
        assertNotNull(alphaPost);
        assertTrue(alphaPost.credibleInterval95Low() <= trueAlphaBoserup && trueAlphaBoserup <= alphaPost.credibleInterval95High(),
                "95% Bayesian credible interval must enclose the true parameter value");

        assertTrue(report.acceptanceRate() > 0.0, "ABC must find valid accepted particles");

        // Verify covariance and correlation matrices
        Map<String, Map<String, Double>> cov = report.covarianceMatrix();
        assertNotNull(cov);
        assertTrue(cov.containsKey("alphaBoserup"));
        assertTrue(cov.get("alphaBoserup").get("alphaBoserup") > 0.0, "Variance must be positive");

        Map<String, Map<String, Double>> corr = report.correlationMatrix();
        assertNotNull(corr);
        assertEquals(1.0, corr.get("alphaBoserup").get("alphaBoserup"), 1e-6, "Diagonal correlation must be 1.0");
    }

    @Test
    @DisplayName("Validate Leave-One-Century-Out (LOCO) out-of-sample cross-validation")
    void testOutOfSampleCrossValidation() {
        Map<Integer, Double> observedTrajectory = new LinkedHashMap<>();
        double val = 50.0;
        for (int year = 1000; year <= 1600; year += 100) {
            observedTrajectory.put(year, val);
            val *= 1.15;
        }

        List<BayesianInverseCalibrationEngine.ParameterPrior> priors = List.of(
                new BayesianInverseCalibrationEngine.ParameterPrior("growthRate", 0.05, 0.25, 0.10)
        );

        BayesianInverseCalibrationEngine.CrossValidationReport cvReport =
                BayesianInverseCalibrationEngine.crossValidateOutOfSample(
                        priors,
                        observedTrajectory,
                        params -> {
                            double r = params.get("growthRate");
                            Map<Integer, Double> sim = new LinkedHashMap<>();
                            double v = 50.0;
                            for (int year = 1000; year <= 1600; year += 100) {
                                sim.put(year, v);
                                v *= (1.0 + r);
                            }
                            return sim;
                        },
                        50,
                        2000,
                        0.30,
                        3
                );

        assertNotNull(cvReport);
        assertTrue(cvReport.inSampleRSquared() > 0.90, "In-sample R^2 must be high");
        assertTrue(cvReport.outOfSampleRSquared() > 0.85, "Out-of-sample R^2 must remain robust");
        assertEquals(3, cvReport.foldCount());
    }
}
