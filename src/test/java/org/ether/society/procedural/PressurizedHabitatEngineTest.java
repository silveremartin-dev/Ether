/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.PressurizedHabitatEngine;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.model.HabitatType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Extraterrestrial Pressurized Habitats, ECLSS Life Support,
 * Aging/Wear, Maintenance Economics, and Catastrophic Failure Dynamics.
 */
public class PressurizedHabitatEngineTest {

    @Test
    @DisplayName("Hostile planet detection correctly classifies Mars, Moon, and Venus as lethal without habitats")
    public void testHostilePlanetDetection() {
        assertTrue(PressurizedHabitatEngine.isHostileEnvironment(PlanetPreset.MARS_LIKE), "Mars should be classified as hostile");
        assertTrue(PressurizedHabitatEngine.isHostileEnvironment(PlanetPreset.MOON_LIKE), "Moon should be classified as hostile");
        assertTrue(PressurizedHabitatEngine.isHostileEnvironment(PlanetPreset.VENUS_LIKE), "Venus should be classified as hostile");
        assertFalse(PressurizedHabitatEngine.isHostileEnvironment(PlanetPreset.EARTH_MODERN), "Modern Earth should be habitable without habitats");
    }

    @Test
    @DisplayName("Pioneer outpost initialization adapts habitat type to planetary geology")
    public void testPioneerOutpostInitialization() {
        H3Cell marsCell = new H3Cell(0x881f1d4887fffffL, 18.0, 77.0);
        marsCell.setElevation(2500.0); // High mountain/volcanic
        PressurizedHabitatEngine.initializePioneerOutpost(marsCell, PlanetPreset.MARS_LIKE, 500);

        assertEquals(HabitatType.LAVA_TUBE, marsCell.getHabitatType(), "High elevation on Mars should default to lava tube");
        assertEquals(1.0, marsCell.getHabitatIntegrity(), 1e-5);
        assertTrue(marsCell.getHabitatCapacity() >= 500.0);

        H3Cell venusCell = new H3Cell(0x881f1d4887ffffeL, 0.0, 0.0);
        PressurizedHabitatEngine.initializePioneerOutpost(venusCell, PlanetPreset.VENUS_LIKE, 2000);
        assertEquals(HabitatType.VENUS_AEROSTAT, venusCell.getHabitatType(), "Venus should default to high-altitude aerostat");
    }

    @Test
    @DisplayName("Populations without pressurized habitats suffer catastrophic exposure mortality on hostile worlds")
    public void testUnprotectedExposureMortality() {
        H3Cell unprotected = new H3Cell(0x881f1d4887fffffL, 0.0, 0.0);
        unprotected.setPopulation(1000);
        unprotected.setHabitatType(HabitatType.NONE);

        PressurizedHabitatEngine.processPressurizedHabitats(List.of(unprotected), PlanetPreset.MARS_LIKE, 1.0);

        assertTrue(unprotected.getPopulation() < 100, "Unprotected population on Mars must suffer severe collapse (>90% mortality)");
    }

    @Test
    @DisplayName("Sheltered habitats undergo structural wear, but well-funded maintenance preserves integrity")
    public void testHabitatWearAndMaintenance() {
        H3Cell cellWellMaintained = new H3Cell(0x881f1d4887fffffL, 0.0, 0.0);
        cellWellMaintained.setPopulation(1000);
        cellWellMaintained.setHabitatType(HabitatType.SURFACE_DOME);
        cellWellMaintained.setHabitatCapacity(2000.0);
        cellWellMaintained.setHabitatIntegrity(1.0);
        cellWellMaintained.setResourceCapital(50000.0); // Abundant capital
        cellWellMaintained.setResourceMetal(10000.0);    // Abundant metal

        H3Cell cellNeglected = new H3Cell(0x881f1d4887ffffeL, 0.0, 0.0);
        cellNeglected.setPopulation(1000);
        cellNeglected.setHabitatType(HabitatType.SURFACE_DOME);
        cellNeglected.setHabitatCapacity(2000.0);
        cellNeglected.setHabitatIntegrity(1.0);
        cellNeglected.setResourceCapital(0.0); // Zero capital
        cellNeglected.setResourceMetal(0.0);    // Zero metal

        // Simulate 5 years of wear
        for (int i = 0; i < 5; i++) {
            PressurizedHabitatEngine.processPressurizedHabitats(List.of(cellWellMaintained, cellNeglected), PlanetPreset.MARS_LIKE, 1.0);
        }

        assertTrue(cellWellMaintained.getHabitatIntegrity() > cellNeglected.getHabitatIntegrity(),
                "Well maintained habitat must retain higher integrity than neglected one");
        assertTrue(cellNeglected.getHabitatIntegrity() < 0.90, "Neglected habitat must show notable wear");
    }

    @Test
    @DisplayName("Severe vétusté (integrity < 0.30) triggers acute mortality from atmospheric failure")
    public void testSevereVetusteMortality() {
        H3Cell criticallyDegraded = new H3Cell(0x881f1d4887fffffL, 0.0, 0.0);
        criticallyDegraded.setPopulation(1000);
        criticallyDegraded.setHabitatType(HabitatType.SURFACE_DOME);
        criticallyDegraded.setHabitatCapacity(2000.0);
        criticallyDegraded.setHabitatIntegrity(0.10); // Critical degradation (10% integrity)
        criticallyDegraded.setResourceCapital(0.0);
        criticallyDegraded.setResourceMetal(0.0);

        PressurizedHabitatEngine.processPressurizedHabitats(List.of(criticallyDegraded), PlanetPreset.MOON_LIKE, 1.0);

        assertTrue(criticallyDegraded.getPopulation() < 1000, "Critically degraded habitat must incur mortality due to breach risk");
    }
}
