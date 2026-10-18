/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.util;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rigorous boundary and invariant verification test suite for {@link SimulationInvariantGuard}.
 * Ensures that physical invariants (non-negative population, bounded Gini, lifespan, albedo)
 * are strictly enforced without numerical leakage.
 */
class SimulationInvariantGuardTest {

    @Test
    @DisplayName("Null or empty cell collections are handled gracefully without exceptions")
    void testNullOrEmptyCells() {
        assertEquals(0, SimulationInvariantGuard.validateAndEnforceInvariants(null));
        assertEquals(0, SimulationInvariantGuard.validateAndEnforceInvariants(new ArrayList<>()));
    }

    @Test
    @DisplayName("Negative population anomalies are strictly clamped to zero")
    void testNegativePopulationCorrection() {
        H3Cell cell = new H3Cell(1L, 0.0, 0.0);
        cell.setPopulation(-500);

        List<H3Cell> cells = List.of(cell);
        int corrections = SimulationInvariantGuard.validateAndEnforceInvariants(cells);

        assertTrue(corrections >= 1);
        assertEquals(0, cell.getPopulation(), "Population must never be negative");
    }

    @Test
    @DisplayName("Gini index is clamped strictly to the unit interval [0.0, 1.0]")
    void testGiniIndexClamping() {
        H3Cell underCell = new H3Cell(2L, 0.0, 0.0);
        underCell.setGiniIndex(-0.25);

        H3Cell overCell = new H3Cell(3L, 0.0, 0.0);
        overCell.setGiniIndex(1.45);

        List<H3Cell> cells = List.of(underCell, overCell);
        int corrections = SimulationInvariantGuard.validateAndEnforceInvariants(cells);

        assertEquals(2, corrections);
        assertEquals(0.0, underCell.getGiniIndex(), 1e-6);
        assertEquals(1.0, overCell.getGiniIndex(), 1e-6);
    }

    @Test
    @DisplayName("Human lifespan is clamped strictly within [10.0, 150.0] years")
    void testLifespanClamping() {
        H3Cell youngCell = new H3Cell(4L, 0.0, 0.0);
        youngCell.setLifespan(4.5);

        H3Cell ancientCell = new H3Cell(5L, 0.0, 0.0);
        ancientCell.setLifespan(250.0);

        List<H3Cell> cells = List.of(youngCell, ancientCell);
        int corrections = SimulationInvariantGuard.validateAndEnforceInvariants(cells);

        assertEquals(2, corrections);
        assertEquals(10.0, youngCell.getLifespan(), 1e-6);
        assertEquals(150.0, ancientCell.getLifespan(), 1e-6);
    }

    @Test
    @DisplayName("Dynamic albedo is clamped within physical thermodynamic limits [0.01, 0.95]")
    void testAlbedoClamping() {
        H3Cell absorbCell = new H3Cell(6L, 0.0, 0.0);
        absorbCell.setDynamicAlbedo(0.001);

        H3Cell mirrorCell = new H3Cell(7L, 0.0, 0.0);
        mirrorCell.setDynamicAlbedo(0.999);

        List<H3Cell> cells = List.of(absorbCell, mirrorCell);
        int corrections = SimulationInvariantGuard.validateAndEnforceInvariants(cells);

        assertEquals(2, corrections);
        assertEquals(0.01, absorbCell.getDynamicAlbedo(), 1e-6);
        assertEquals(0.95, mirrorCell.getDynamicAlbedo(), 1e-6);
    }

    @Test
    @DisplayName("Already valid cells require zero corrections")
    void testValidCellsNoCorrection() {
        H3Cell validCell = new H3Cell(8L, 0.0, 0.0);
        validCell.setPopulation(1000);
        validCell.setGiniIndex(0.35);
        validCell.setLifespan(65.0);
        validCell.setDynamicAlbedo(0.25);

        List<H3Cell> cells = List.of(validCell);
        int corrections = SimulationInvariantGuard.validateAndEnforceInvariants(cells);

        assertEquals(0, corrections);
        assertEquals(1000, validCell.getPopulation());
        assertEquals(0.35, validCell.getGiniIndex(), 1e-6);
        assertEquals(65.0, validCell.getLifespan(), 1e-6);
        assertEquals(0.25, validCell.getDynamicAlbedo(), 1e-6);
    }
}
