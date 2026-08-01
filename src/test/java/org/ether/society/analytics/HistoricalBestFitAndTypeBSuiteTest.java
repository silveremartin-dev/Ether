/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.database.H3Cell;
import org.ether.society.procedural.typeb.*;
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
 * @version 4.0.0
 */
public class HistoricalBestFitAndTypeBSuiteTest {

    @Test
    public void testHistoricalBestFitCalculations() {
        Map<Integer, Double> simulatedData = new HashMap<>();
        simulatedData.put(1900, 1650.0);
        simulatedData.put(1950, 2525.0);
        simulatedData.put(2000, 6127.0);

        double rmse = HistoricalValidationKernel.calculateRmse(simulatedData);
        assertEquals(0.0, rmse, 0.001, "Perfect match should yield 0.0 RMSE.");

        double r2 = HistoricalValidationKernel.calculateRSquared(simulatedData);
        assertTrue(r2 > 0.90, "Good fit should yield R^2 > 0.90.");
    }

    @Test
    public void testIsolatedMacroEngines() {
        assertEquals(0.65, LenskiPureEngine.calculateLenskiGini(3.0), 0.001, "Agrarian stage should yield peak Gini in Lenski model.");
        assertTrue(LeslieWhitePureEngine.calculateCulturalComplexity(100.0, 2.5) > 200.0, "White's law C = E * T should scale complexity.");
        assertTrue(KardashevPureEngine.calculateKardashevScale(1e16) >= 1.0, "Type I threshold should be reached at 10^16 Watts.");
        assertTrue(PsychohistoryPureEngine.calculateSeldonCrisisProbability(8e9, 0.50) > 0.40, "Psychohistory should compute Seldon crisis probability.");
    }

    @Test
    public void testEngineHybridsExecution() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell cell = new H3Cell(613503380827930701L, 45.0, 10.0);
        cell.setPopulation(10000);
        cell.setTechnologyLevel(3.0);
        cell.setEnergyFoodConsumed(500.0);
        cells.add(cell);

        assertDoesNotThrow(() -> {
            LenskiPureEngine.processHybrid(cells, 1.0);
            LeslieWhitePureEngine.processHybrid(cells, 1.0);
            KardashevPureEngine.processHybrid(cells, 1.0);
            PsychohistoryPureEngine.processHybrid(cells, 1.0);
            CulturalMaterialismPureEngine.processHybrid(cells, 1.0);
        }, "Isolated Type B hybrid executions should complete cleanly.");
    }
}
