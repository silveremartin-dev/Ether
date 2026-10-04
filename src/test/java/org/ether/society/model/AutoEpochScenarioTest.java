/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.generation.PlanetPreset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Automatic Epoch Scenario Generator.
 * Verifies epistemic validity, demographic scalings, and physical/anthropological Type B engine selection across history.
 */
class AutoEpochScenarioTest {

    @Test
    @DisplayName("Generate Paleolithic Earth Scenario (-100,000 BP)")
    void testPaleolithicScenario() {
        Scenario s = AutoEpochScenarioGenerator.generateScenarioForEpoch("earth", -100000L);
        assertNotNull(s);
        assertEquals(-100000L, s.getStartDateYear());
        assertTrue(s.getEndDateYear() > s.getStartDateYear());
        assertEquals(50_000L, s.getInitialHumanCount());
        assertTrue(s.getInitialCapitalPerCapita() < 10.0);

        Map<String, Boolean> engines = s.getTypeBEngineStates();
        assertNotNull(engines);
        assertTrue(engines.getOrDefault("PaleoLanguageDriftEngine", false));
        assertTrue(engines.getOrDefault("KarstCaveShelterEngine", false));
        assertFalse(engines.getOrDefault("FrontierAsabiyyahEngine", false));
        assertFalse(engines.getOrDefault("AutoRegulationPureEngine", false));
    }

    @Test
    @DisplayName("Generate LGM Earth Scenario (-20,000 BP)")
    void testLGMScenario() {
        Scenario s = AutoEpochScenarioGenerator.generateScenarioForEpoch("earth", -20000L);
        assertNotNull(s);
        assertEquals(-20000L, s.getStartDateYear());
        assertEquals(500_000L, s.getInitialHumanCount());
        
        Map<String, Boolean> engines = s.getTypeBEngineStates();
        assertTrue(engines.getOrDefault("PaleoLanguageDriftEngine", false));
        assertTrue(engines.getOrDefault("TailoredClothingThermalEngine", false));
    }

    @Test
    @DisplayName("Generate Classical Antiquity Scenario (0 AD)")
    void testAntiquityScenario() {
        Scenario s = AutoEpochScenarioGenerator.generateScenarioForEpoch("earth", 0L);
        assertNotNull(s);
        assertEquals(0L, s.getStartDateYear());
        assertEquals(200_000_000L, s.getInitialHumanCount());
        assertTrue(s.getInitialCapitalPerCapita() >= 100.0);

        Map<String, Boolean> engines = s.getTypeBEngineStates();
        assertFalse(engines.getOrDefault("PaleoLanguageDriftEngine", true));
        assertTrue(engines.getOrDefault("FrontierAsabiyyahEngine", false));
        assertFalse(engines.getOrDefault("AutoRegulationPureEngine", false));
    }

    @Test
    @DisplayName("Generate Industrial Revolution Scenario (1800 AD)")
    void testIndustrialScenario() {
        Scenario s = AutoEpochScenarioGenerator.generateScenarioForEpoch("earth", 1800L);
        assertNotNull(s);
        assertEquals(1800L, s.getStartDateYear());
        assertEquals(1_000_000_000L, s.getInitialHumanCount());
        assertTrue(s.getInitialCapitalPerCapita() >= 1000.0);

        Map<String, Boolean> engines = s.getTypeBEngineStates();
        assertFalse(engines.getOrDefault("PaleoLanguageDriftEngine", true));
    }

    @Test
    @DisplayName("Generate Mars Outpost Scenario")
    void testMarsScenario() {
        Scenario s = AutoEpochScenarioGenerator.generateScenarioForEpoch("mars", 2026L);
        assertNotNull(s);
        assertEquals(2026L, s.getStartDateYear());
        assertNotNull(s.getPlanetPreset());
        assertTrue(s.getName().toLowerCase().contains("mars"));
    }

    @Test
    @DisplayName("Verify DataFallbackStrategy options in scenario generation")
    void testFallbackStrategies() {
        // Continuous interpolation
        Scenario sInterp = AutoEpochScenarioGenerator.generateScenarioForEpoch("earth", 1450L, false, 
                org.ether.society.data.TemporalMapTensorManager.DataFallbackStrategy.CONTINUOUS_INTERPOLATION);
        assertNotNull(sInterp);

        // Previous earlier epoch
        Scenario sPrev = AutoEpochScenarioGenerator.generateScenarioForEpoch("earth", 1450L, false, 
                org.ether.society.data.TemporalMapTensorManager.DataFallbackStrategy.PREVIOUS_EARLIER_EPOCH);
        assertNotNull(sPrev);

        // Closest anchor epoch
        Scenario sClosest = AutoEpochScenarioGenerator.generateScenarioForEpoch("earth", 1450L, false, 
                org.ether.society.data.TemporalMapTensorManager.DataFallbackStrategy.CLOSEST_ANCHOR_EPOCH);
        assertNotNull(sClosest);
    }
}

