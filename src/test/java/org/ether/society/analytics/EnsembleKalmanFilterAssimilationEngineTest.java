/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class EnsembleKalmanFilterAssimilationEngineTest {

    @Test
    @DisplayName("Verify EnKF data assimilation converges ensemble towards historical observations and reduces variance")
    void testEnKFAssimilationConvergence() {
        int ensembleSize = 25;
        int stateDim = 3;
        double[][] ensemble = new double[ensembleSize][stateDim];

        // Initialize ensemble around biased mean [100, 200, 300]
        Random initRng = new Random(777);
        for (int i = 0; i < ensembleSize; i++) {
            ensemble[i][0] = 100.0 + initRng.nextGaussian() * 15.0;
            ensemble[i][1] = 200.0 + initRng.nextGaussian() * 20.0;
            ensemble[i][2] = 300.0 + initRng.nextGaussian() * 25.0;
        }

        // Empirical observation vector (Seshat ground truth: [150, 250, 350])
        double[] observation = new double[]{150.0, 250.0, 350.0};

        EnsembleKalmanFilterAssimilationEngine enkf = new EnsembleKalmanFilterAssimilationEngine(
                ensembleSize, 5.0, new Random(888)
        );

        var result = enkf.assimilate(ensemble, observation);

        assertNotNull(result);
        assertTrue(result.epistemicDiscrepancy() > 0.0, "Epistemic discrepancy must capture state adjustment.");
        assertTrue(result.varianceReduction() > 0.0, "Assimilation of observations must reduce ensemble spread.");

        // Verify analysis mean shifted towards observation
        for (int j = 0; j < stateDim; j++) {
            double forecastDist = Math.abs(result.ensembleMeanForecast()[j] - observation[j]);
            double analysisDist = Math.abs(result.ensembleMeanAnalysis()[j] - observation[j]);
            assertTrue(analysisDist < forecastDist, "Analysis mean must be closer to observation than forecast mean.");
        }
    }
}
