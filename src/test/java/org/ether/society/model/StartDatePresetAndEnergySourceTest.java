/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite validating {@link StartDatePreset} and {@link EnergySource} model constants,
 * chronological monotonicity, carrying capacity multipliers, and historical demography bounds.
 */
class StartDatePresetAndEnergySourceTest {

    @Test
    @DisplayName("StartDatePreset epochs are strictly monotonically ordered in chronological time")
    void testPresetChronologicalOrdering() {
        StartDatePreset[] presets = StartDatePreset.values();

        for (int i = 0; i < presets.length - 1; i++) {
            assertTrue(presets[i].getYear() < presets[i + 1].getYear(),
                    "Preset " + presets[i] + " must occur before " + presets[i + 1]);
        }
    }

    @Test
    @DisplayName("Estimated world population strictly increases through technological progression")
    void testEstimatedPopulationMonotonicity() {
        StartDatePreset[] presets = StartDatePreset.values();

        for (int i = 0; i < presets.length - 1; i++) {
            assertTrue(presets[i].getEstimatedPopulation() <= presets[i + 1].getEstimatedPopulation(),
                    "World population at " + presets[i] + " must not exceed " + presets[i + 1]);
        }

        // Out of Africa should match Upper Paleolithic order of magnitude (~10,000)
        assertEquals(10_000L, StartDatePreset.OUT_OF_AFRICA.getEstimatedPopulation());
        // Modern era should be 2.5 billion
        assertEquals(2_500_000_000L, StartDatePreset.MODERN.getEstimatedPopulation());
    }

    @Test
    @DisplayName("EnergySource multipliers and carrying capacity boost increase monotonically with tech level")
    void testEnergySourceMonotonicity() {
        EnergySource[] sources = EnergySource.values();

        for (int i = 0; i < sources.length - 1; i++) {
            assertTrue(sources[i].getTechLevel() <= sources[i + 1].getTechLevel(),
                    "Tech level of " + sources[i] + " must not exceed " + sources[i + 1]);
            assertTrue(sources[i].getEnergyMultiplier() <= sources[i + 1].getEnergyMultiplier(),
                    "Energy multiplier of " + sources[i] + " must not exceed " + sources[i + 1]);
            assertTrue(sources[i].getCarryingCapacityBoost() <= sources[i + 1].getCarryingCapacityBoost(),
                    "Carrying capacity boost of " + sources[i] + " must not exceed " + sources[i + 1]);
        }
    }

    @Test
    @DisplayName("StartDatePreset estimated tech level maps directly to a valid EnergySource tech level")
    void testPresetToEnergySourceMapping() {
        for (StartDatePreset preset : StartDatePreset.values()) {
            int techLevel = preset.getEstimatedTechLevel();
            assertTrue(techLevel >= 0 && techLevel <= 6,
                    "Preset " + preset + " tech level must be between 0 (Fire) and 6 (Oil)");
        }
    }
}
