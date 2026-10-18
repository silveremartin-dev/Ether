/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for {@link CalibrationDiagnosticRules}.
 * Validates expert diagnostic rule generation across optimal models and divergent scenarios.
 */
class CalibrationDiagnosticRulesTest {

    @Test
    @DisplayName("Optimally calibrated model triggers clean confirmation without correction actions")
    void testOptimallyCalibratedModel() {
        Map<String, Double> mapes = new HashMap<>();
        mapes.put("Population", 6.5);
        mapes.put("Agriculture", 8.2);
        mapes.put("Urbanization", 5.0);

        String suggestions = CalibrationDiagnosticRules.generateTuningSuggestions(mapes, 0.98, 0);

        assertNotNull(suggestions);
        assertTrue(suggestions.contains("Optimally Calibrated Model") || suggestions.contains("alignés avec la réalité"));
    }

    @Test
    @DisplayName("High demographic discrepancy triggers carrying capacity and growth tuning recommendations")
    void testHighDemographicDivergence() {
        Map<String, Double> mapes = new HashMap<>();
        mapes.put("Population", 42.0); // Severe divergence > 35%

        String suggestions = CalibrationDiagnosticRules.generateTuningSuggestions(mapes, 0.72, 1750);

        assertNotNull(suggestions);
        assertTrue(suggestions.contains("Demographic") || suggestions.contains("Démographique"));
        assertTrue(suggestions.contains("carryingCapacity") || suggestions.contains("agricultural_spread_rate") || suggestions.contains("Diagnostic"));
    }

    @Test
    @DisplayName("Moderate demographic discrepancy triggers cohort grouping recommendations")
    void testModerateDemographicDivergence() {
        Map<String, Double> mapes = new HashMap<>();
        mapes.put("Population", 22.0); // Moderate divergence between 15% and 35%

        String suggestions = CalibrationDiagnosticRules.generateTuningSuggestions(mapes, 0.88, 1200);

        assertNotNull(suggestions);
        assertTrue(suggestions.contains("Demographic") || suggestions.contains("Démographique"));
    }
}
