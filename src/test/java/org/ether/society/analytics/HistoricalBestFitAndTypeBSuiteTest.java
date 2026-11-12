/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.engines.tier2.historical.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite validating isolated Type B macro-sociological models (Lenski, Leslie White, Kardashev, Psychohistory)
 * and the Historical Telemetry Best-Fit Kernel (RMSE and R^2).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class HistoricalBestFitAndTypeBSuiteTest {

    /**
     * Validates RMSE and \(R^2\) sensitivity to quantitative trajectory perturbations as well as
     * monotonic error escalation when simulated trajectories diverge from historical benchmarks.
     */
    @Test
    public void testHistoricalBestFitCalculations() {
        Map<Integer, Double> exactData = new HashMap<>();
        exactData.put(1900, 1650.0);
        exactData.put(1950, 2525.0);
        exactData.put(2000, 6127.0);

        double exactRmse = HistoricalValidationKernel.calculateRmse(exactData);
        double exactR2 = HistoricalValidationKernel.calculateRSquared(exactData);
        assertEquals(0.0, exactRmse, 1e-6, "Zero residual trajectory must yield 0.0 RMSE.");
        assertEquals(1.0, exactR2, 1e-6, "Zero residual trajectory must yield R^2 = 1.0.");

        // Introduce known +100.0 M perturbation at each point -> analytical RMSE must equal 100.0
        Map<Integer, Double> perturbedData = new HashMap<>();
        perturbedData.put(1900, 1750.0);
        perturbedData.put(1950, 2625.0);
        perturbedData.put(2000, 6227.0);

        double perturbedRmse = HistoricalValidationKernel.calculateRmse(perturbedData);
        double perturbedR2 = HistoricalValidationKernel.calculateRSquared(perturbedData);
        assertEquals(100.0, perturbedRmse, 0.01, "Uniform +100 perturbation must produce exact RMSE = 100.0.");
        assertTrue(perturbedR2 < exactR2 && perturbedR2 > 0.99, "Small perturbation must slightly reduce R^2 while remaining > 0.99.");

        // Severe anti-correlated trajectory must produce negative R^2 and high RMSE
        Map<Integer, Double> divergentData = new HashMap<>();
        divergentData.put(1900, 6000.0);
        divergentData.put(1950, 2500.0);
        divergentData.put(2000, 1000.0);
        assertTrue(HistoricalValidationKernel.calculateRmse(divergentData) > 3000.0, "Divergent trajectory must yield high RMSE.");
        assertEquals(0.0, HistoricalValidationKernel.calculateRSquared(divergentData), 1e-6, "Anti-correlated trajectory must collapse clamped R^2 to 0.0.");
    }

    /**
     * Validates isolated biophysical macro-sociological equations across Paleolithic, Agrarian,
     * and Industrial capital regimes (Lenski-Milanovic curve, Leslie White exergy law, Kardashev scale).
     */
    @Test
    public void testIsolatedMacroEngines() {
        double foragerGini = LenskiPureEngine.calculateLenskiGini(3.0);
        double agrarianGini = LenskiPureEngine.calculateLenskiGini(280.0);
        double industrialGini = LenskiPureEngine.calculateLenskiGini(15000.0);

        assertTrue(foragerGini < 0.30, "Paleolithic foraging regime (3 kg/cap) must exhibit low Gini (< 0.30).");
        assertEquals(0.65, agrarianGini, 0.01, "Agrarian stage (280 kg/cap) must yield peak Gini (~0.65) in Lenski-Milanovic model.");
        assertTrue(industrialGini < agrarianGini && industrialGini > foragerGini,
                "Industrial regime (15,000 kg/cap) must moderate Gini below agrarian peak (~0.38).");

        assertTrue(LeslieWhitePureEngine.calculateCulturalComplexity(100.0, 2.5) > 200.0, "White's law C = E * eta must scale complexity.");
        assertTrue(org.ether.society.analytics.PostHocCliodynamicMetricsExtractor.computeKardashevScale(1e16 / 1e9, (long) 1e9) >= 1.0, "Type I threshold must be reached at 10^16 Watts.");
    }

    /**
     * Validates quantitative physical state updates across Type B hybrid macro-engines
     * (Lenski inequality friction, Leslie White capital accumulation, Cultural Materialism caloric return).
     */
    @Test
    public void testEngineHybridsExecution() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomassHuman()[0] = 10000.0f;
        double initialCapital = 280.0 * 10000.0;
        buf.getResourceCapital()[0] = (float) initialCapital;
        buf.getEnergyFoodConsumed()[0] = 500.0f;

        new LenskiPureEngine().tick(buf, 1.0, 1.0);
        assertTrue(buf.getGiniIndex()[0] > 0.0f,
                "Lenski engine must compute positive inequality Gini index for agrarian capital.");

        double beforeWhite = buf.getResourceCapital()[0];
        new LeslieWhitePureEngine().tick(buf, 1.0, 1.0);
        assertTrue(buf.getResourceCapital()[0] > beforeWhite,
                "Leslie White exergy harnessing must accumulate physical capital proportional to power flux.");

        new CulturalMaterialismPureEngine().tick(buf, 1.0, 1.0);
        assertTrue(buf.getFoodResource()[0] >= 0.0f && buf.getResourceCapital()[0] > 0.0f,
                "Cultural materialism step must preserve non-negative physical stocks.");
    }
}


