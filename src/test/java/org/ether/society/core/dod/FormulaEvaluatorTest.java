/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit & Algorithmic Test Suite for {@link FormulaEvaluator}.
 * Tests statistical aggregation functions (SUM, AVG, MEAN, MEDIAN, VAR, STDDEV, MIN, MAX, GINI, COUNT),
 * nested arithmetic expressions, precedence, and resolver bindings.
 */
public class FormulaEvaluatorTest {

    private final FormulaEvaluator evaluator = new FormulaEvaluator();

    @Test
    @DisplayName("Verify basic arithmetic expression evaluation")
    public void testBasicArithmetic() {
        double result = evaluator.evaluate("10 + 5 * 2", var -> null, var -> 0.0);
        assertEquals(20.0, result, 1e-4);

        double resultWithParens = evaluator.evaluate("(10 + 5) * 2", var -> null, var -> 0.0);
        assertEquals(30.0, resultWithParens, 1e-4);

        double division = evaluator.evaluate("100 / 4 - 5", var -> null, var -> 0.0);
        assertEquals(20.0, division, 1e-4);
    }

    @Test
    @DisplayName("Verify statistical functions: SUM, AVG, MIN, MAX, COUNT")
    public void testStatisticalAggregationFunctions() {
        float[] wealthData = { 10.0f, 20.0f, 30.0f, 40.0f };

        FormulaEvaluator.VariableResolver arrayResolver = var -> {
            if ("wealth".equalsIgnoreCase(var)) return wealthData;
            return new float[0];
        };

        double sum = evaluator.evaluate("SUM(wealth)", arrayResolver, var -> 0.0);
        assertEquals(100.0, sum, 1e-4);

        double avg = evaluator.evaluate("AVG(wealth)", arrayResolver, var -> 0.0);
        assertEquals(25.0, avg, 1e-4);

        double min = evaluator.evaluate("MIN(wealth)", arrayResolver, var -> 0.0);
        assertEquals(10.0, min, 1e-4);

        double max = evaluator.evaluate("MAX(wealth)", arrayResolver, var -> 0.0);
        assertEquals(40.0, max, 1e-4);

        double count = evaluator.evaluate("COUNT(wealth)", arrayResolver, var -> 0.0);
        assertEquals(4.0, count, 1e-4);
    }

    @Test
    @DisplayName("Verify dispersion metrics: VAR, STDDEV, MEDIAN, GINI")
    public void testDispersionMetrics() {
        float[] data = { 2.0f, 4.0f, 4.0f, 4.0f, 5.0f, 5.0f, 7.0f, 9.0f };

        FormulaEvaluator.VariableResolver arrayResolver = var -> {
            if ("sample".equalsIgnoreCase(var)) return data;
            return new float[0];
        };

        double median = evaluator.evaluate("MEDIAN(sample)", arrayResolver, var -> 0.0);
        assertEquals(4.5, median, 1e-4);

        double stddev = evaluator.evaluate("STDDEV(sample)", arrayResolver, var -> 0.0);
        assertTrue(stddev > 0.0, "Standard deviation must be strictly positive");

        double varVal = evaluator.evaluate("VAR(sample)", arrayResolver, var -> 0.0);
        assertEquals(stddev * stddev, varVal, 0.1, "Variance must equal square of standard deviation");

        double gini = evaluator.evaluate("GINI(sample)", arrayResolver, var -> 0.0);
        assertTrue(gini >= 0.0 && gini <= 1.0, "Gini coefficient must be bounded in [0, 1]");
    }

    @Test
    @DisplayName("Verify combined cliodynamic expressions: SUM(wealth) / COUNT(population)")
    public void testCliodynamicCombinedExpression() {
        float[] food = { 500.0f, 1500.0f, 1000.0f };
        float[] pop = { 10.0f, 20.0f, 20.0f };

        FormulaEvaluator.VariableResolver arrayResolver = var -> {
            if ("food".equalsIgnoreCase(var)) return food;
            if ("population".equalsIgnoreCase(var)) return pop;
            return new float[0];
        };

        double foodPerCapita = evaluator.evaluate("SUM(food) / COUNT(population)", arrayResolver, var -> 0.0);
        // SUM(food) = 3000, COUNT(pop) = 3 -> 3000 / 3 = 1000
        assertEquals(1000.0, foodPerCapita, 1e-3);
    }

    @Test
    @DisplayName("Verify scalar resolver binding")
    public void testScalarResolver() {
        FormulaEvaluator.ScalarResolver scalarResolver = var -> {
            if ("taxRate".equalsIgnoreCase(var)) return 0.25;
            if ("baseGDP".equalsIgnoreCase(var)) return 1000.0;
            return 0.0;
        };

        double revenue = evaluator.evaluate("baseGDP * taxRate", var -> null, scalarResolver);
        assertEquals(250.0, revenue, 1e-4);
    }

    @Test
    @DisplayName("Verify graceful handling of null, blank or invalid expressions")
    public void testInvalidExpressions() {
        assertEquals(0.0, evaluator.evaluate(null, var -> null, var -> 0.0));
        assertEquals(0.0, evaluator.evaluate("", var -> null, var -> 0.0));
        assertEquals(0.0, evaluator.evaluate("   ", var -> null, var -> 0.0));
        // Malformed expression should return 0.0 without throwing unhandled exception
        assertEquals(0.0, evaluator.evaluate("10 + / 5", var -> null, var -> 0.0));
    }
}
