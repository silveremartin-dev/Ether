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
    @DisplayName("Validate ABC parameter estimation on non-linear Malthus-Boserup agricultural intensification")
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
    }
}
