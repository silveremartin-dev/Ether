/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Epistemic verification and validation test suite for {@link MethaneClathrateStabilityEngine}.
 * Verifies hydrostatic pressure, Sloan-Buffett phase boundary, GHSZ thickness,
 * 500-cell hyperthermal dissociation simulation, and bitwise determinism.
 */
public class MethaneClathrateStabilityEngineTest {

    @Test
    @DisplayName("3.1 Hydrostatic Pressure & Phase Stability Analytical Calculations")
    public void testPhaseStabilityCalculations() {
        // Hydrostatic pressure at depth
        double p500m = MethaneClathrateStabilityEngine.calculateHydrostaticPressureMPa(500.0);
        // rho * g * h = 1025 * 9.80665 * 500 / 1e6 ~ 5.0259 MPa
        assertEquals(5.0259, p500m, 0.05, "Hydrostatic pressure at 500m depth must be ~5.03 MPa.");

        // Shallow water (< 250m) below clathrate stability onset
        double p100m = MethaneClathrateStabilityEngine.calculateHydrostaticPressureMPa(100.0);
        double tEqShallow = MethaneClathrateStabilityEngine.calculateEquilibriumPhaseTemperatureCelsius(p100m);
        assertTrue(tEqShallow < -20.0, "Hydrates are unstable at shallow shelf depths (< 250m).");

        // Intermediate depth (800m)
        double p800m = MethaneClathrateStabilityEngine.calculateHydrostaticPressureMPa(800.0);
        double tEq800m = MethaneClathrateStabilityEngine.calculateEquilibriumPhaseTemperatureCelsius(p800m);
        assertTrue(tEq800m > 7.0 && tEq800m < 11.0, "At 800m depth, clathrate phase boundary must be ~8-10°C.");

        // Deep ocean (2500m)
        double p2500m = MethaneClathrateStabilityEngine.calculateHydrostaticPressureMPa(2500.0);
        double tEq2500m = MethaneClathrateStabilityEngine.calculateEquilibriumPhaseTemperatureCelsius(p2500m);
        assertTrue(tEq2500m > 18.0, "At 2500m depth, clathrate phase boundary must exceed 18°C.");
    }

    @Test
    @DisplayName("3.2 Gas Hydrate Stability Zone (GHSZ) Thickness")
    public void testGHSZThickness() {
        // At 1000m depth, bottom water temp = 2.0°C (cold baseline)
        double thicknessCold = MethaneClathrateStabilityEngine.calculateGHSZThicknessMeters(1000.0, 2.0);
        assertTrue(thicknessCold > 200.0, "Under cold 2°C bottom water, GHSZ thickness must be substantial (>200m).");

        // Warming bottom water to 6.0°C thins the GHSZ
        double thicknessWarm = MethaneClathrateStabilityEngine.calculateGHSZThicknessMeters(1000.0, 6.0);
        assertTrue(thicknessWarm < thicknessCold, "Benthic warming must shrink the GHSZ thickness.");
        // deltaT = 4°C / (0.035°C/m) ~ 114m thinning
        assertEquals(4.0 / 0.035, thicknessCold - thicknessWarm, 1.0,
                "Thinning must adhere to exact geothermal gradient (4°C / 0.035°C/m ~ 114.3m).");

        // Shallow depth has 0 thickness
        assertEquals(0.0, MethaneClathrateStabilityEngine.calculateGHSZThicknessMeters(150.0, 2.0), 1e-6);
    }

    @Test
    @DisplayName("3.3 500-Cell Hyperthermal Dissociation Simulation (PETM / Extreme Warming)")
    public void test500CellClathrateDissociationSimulation() {
        int cellCount = 500;
        List<H3Cell> cells = new ArrayList<>(cellCount);

        for (int i = 0; i < cellCount; i++) {
            H3Cell cell = new H3Cell();
            cell.setH3Index((long) (300000 + i));

            if (i < 100) {
                // Land cells (elevation > 0)
                cell.setElevation(250.0);
                cell.setBiome(Biome.FOREST);
                cell.setTemperature(15.0);
            } else if (i < 200) {
                // Shallow coastal continental shelf (< 200m depth)
                cell.setElevation(-120.0);
                cell.setBiome(Biome.OCEAN);
                cell.setTemperature(16.0);
            } else if (i < 350) {
                // Continental slope (hydrate-rich: 500m to 1200m depth)
                double slopeDepth = 500.0 + (i - 200) * 4.67; // -500m to -1200m
                cell.setElevation(-slopeDepth);
                cell.setBiome(Biome.OCEAN);
                cell.setTemperature(12.0);
            } else {
                // Abyssal deep ocean (> 2500m depth)
                cell.setElevation(-3500.0);
                cell.setBiome(Biome.DEEP_OCEAN);
                cell.setTemperature(2.0);
            }
            cells.add(cell);
        }

        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(cells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, buf, false);

        // Scenario 1: Pre-industrial baseline conditions (anomaly = 0.0°C) -> No dissociation
        double outgassingBaseline = MethaneClathrateStabilityEngine.processClathrateDissociation(buf, 1.0, 0.0);
        assertEquals(0.0, outgassingBaseline, 1e-6, "Under baseline thermal equilibrium, no clathrate outgassing occurs.");

        // Scenario 2: Moderate ocean warming anomaly (+2.5°C benthic warming)
        double outgassingModerate = MethaneClathrateStabilityEngine.processClathrateDissociation(buf, 1.0, 2.5);
        assertTrue(outgassingModerate > 0.5, "Moderate benthic warming must trigger clathrate dissociation.");

        // Scenario 3: Severe hyperthermal pulse (PETM analogue, +6.0°C deep ocean warming)
        double outgassingExtreme = MethaneClathrateStabilityEngine.processClathrateDissociation(buf, 1.0, 6.0);
        assertTrue(outgassingExtreme > outgassingModerate * 2.0,
                "Hyperthermal pulse must release substantially greater methane than moderate warming.");
    }

    @Test
    @DisplayName("3.4 Bitwise Determinism Verification")
    public void testBitwiseDeterminism() {
        List<H3Cell> run1 = createMarineDomain(100);
        List<H3Cell> run2 = createMarineDomain(100);
        org.ether.society.core.dod.WorldBuffer buf1 = new org.ether.society.core.dod.WorldBuffer(run1.size());
        org.ether.society.core.dod.WorldBuffer buf2 = new org.ether.society.core.dod.WorldBuffer(run2.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(run1, buf1, false);
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(run2, buf2, false);

        double out1 = MethaneClathrateStabilityEngine.processClathrateDissociation(buf1, 10.0, 3.5);
        double out2 = MethaneClathrateStabilityEngine.processClathrateDissociation(buf2, 10.0, 3.5);

        assertEquals(out1, out2, 0.0, "Clathrate outgassing outputs must be bitwise identical.");
    }

    private List<H3Cell> createMarineDomain(int count) {
        List<H3Cell> cells = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            H3Cell cell = new H3Cell();
            cell.setH3Index((long) (70000 + i));
            cell.setElevation(-800.0);
            cell.setBiome(Biome.OCEAN);
            cell.setTemperature(10.0);
            cells.add(cell);
        }
        return cells;
    }
}
