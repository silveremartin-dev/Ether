/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Epistemic verification and validation test suite for {@link ThermohalineStommelAMOCEngine}.
 * Verifies non-linear 2-box dynamics, multi-centennial haline hysteresis, bipolar seesaw,
 * and 500-cell spatial mesh execution.
 */
public class ThermohalineStommelAMOCEngineTest {

    @BeforeEach
    public void setUp() {
        ThermohalineStommelAMOCEngine.resetState();
    }

    @Test
    @DisplayName("1.1 Baseline Overturning Equilibrium & Physical Seawater Equation of State")
    public void testBaselineOverturningAndDensity() {
        // Equator: 28°C, 36.5 PSU; Pole: 2°C, 34.8 PSU
        double rhoEq = ThermohalineStommelAMOCEngine.calculateSeawaterDensity(28.0, 36.5);
        double rhoPole = ThermohalineStommelAMOCEngine.calculateSeawaterDensity(2.0, 34.8);

        assertTrue(rhoPole > rhoEq, "Polar cold dense water must have higher density than warm tropical water.");

        double initialFlow = ThermohalineStommelAMOCEngine.calculateOverturningFlowSv(28.0, 2.0, 36.5, 34.8);
        assertTrue(initialFlow >= 16.0 && initialFlow <= 20.0,
                "Baseline unperturbed AMOC flux must be ~18 Sverdrups (observed: " + initialFlow + ")");
    }

    @Test
    @DisplayName("1.2 Multi-Centennial Haline Hysteresis & Bistability under Freshwater Pulse")
    public void testAMOCHysteresisAndSaltMemory() {
        // Step 1: Pre-industrial baseline steady state (meltwater = 0)
        ThermohalineStommelAMOCEngine.StommelState baseline = ThermohalineStommelAMOCEngine.stepStommelDynamics(0.0, 10.0);
        assertFalse(baseline.isCollapsed(), "Baseline AMOC must be in active convective overturning mode.");
        assertTrue(baseline.amocFlowSv() > 15.0);

        // Step 2: Severe 100-year meltwater pulse (F_melt = 0.45 Sv, e.g. Heinrich event / deglacial meltwater)
        ThermohalineStommelAMOCEngine.StommelState collapsedState = null;
        for (int yr = 0; yr < 100; yr += 10) {
            collapsedState = ThermohalineStommelAMOCEngine.stepStommelDynamics(0.45, 10.0);
        }
        assertNotNull(collapsedState);
        assertTrue(collapsedState.isCollapsed(), "100-year meltwater pulse must trigger convective AMOC collapse.");
        assertTrue(collapsedState.amocFlowSv() < 8.0, "Collapsed AMOC flux must drop below 8 Sv.");
        assertTrue(collapsedState.northAtlanticCoolingShiftC() <= -6.0,
                "AMOC collapse must induce deep cooling anomaly in North Atlantic (observed: " + collapsedState.northAtlanticCoolingShiftC() + "°C)");

        // Step 3: Cessation of meltwater pulse (F_melt = 0.0)
        // Due to slow haline relaxation (tau_diff = 500 yr), the AMOC MUST REMAIN COLLAPSED for the first 50 years!
        ThermohalineStommelAMOCEngine.StommelState lingeringState = ThermohalineStommelAMOCEngine.stepStommelDynamics(0.0, 50.0);
        assertTrue(lingeringState.isCollapsed(),
                "AMOC must exhibit multi-centennial hysteresis and remain collapsed 50 years after meltwater cessation due to salt memory.");

        // Step 4: Long-term multi-centennial recovery (600 years of slow haline diffusion and gyre exchange)
        ThermohalineStommelAMOCEngine.StommelState recoveredState = null;
        for (int yr = 0; yr < 600; yr += 50) {
            recoveredState = ThermohalineStommelAMOCEngine.stepStommelDynamics(0.0, 50.0);
        }
        assertNotNull(recoveredState);
        assertFalse(recoveredState.isCollapsed(), "AMOC must eventually recover convective overturning after ~600 years.");
        assertTrue(recoveredState.amocFlowSv() > 14.0, "Recovered AMOC flux must return toward baseline.");
    }

    @Test
    @DisplayName("1.3 Bipolar Seesaw Verification: North Atlantic Cooling vs Southern Ocean Warming")
    public void testBipolarSeesawEnergyBalance() {
        // Trigger AMOC collapse with severe 80-year meltwater forcing (0.40 Sv)
        ThermohalineStommelAMOCEngine.StommelState state = null;
        for (int yr = 0; yr < 80; yr += 10) {
            state = ThermohalineStommelAMOCEngine.stepStommelDynamics(0.40, 10.0);
        }

        assertTrue(state.northAtlanticCoolingShiftC() < -4.0,
                "North Atlantic must cool significantly during AMOC collapse.");
        assertTrue(state.southernOceanWarmingShiftC() > 0.8,
                "Southern Ocean must experience compensatory bipolar seesaw warming (observed: " + state.southernOceanWarmingShiftC() + "°C)");
        assertEquals(-0.22, state.southernOceanWarmingShiftC() / state.northAtlanticCoolingShiftC(), 1e-4,
                "Bipolar seesaw warming must adhere to exact Broecker/EPICA ratio (-0.22).");
    }

    @Test
    @DisplayName("1.4 500-Cell Global Mesh Spatial Verification with Milankovitch Coupling")
    public void test500CellMeshSpatialAMOC() {
        List<H3Cell> mesh = new ArrayList<>();
        // Generate 500 cells distributed across latitudes (-75 to +75) and longitudes (-180 to +180)
        for (int i = 0; i < 500; i++) {
            double lat = -75.0 + (150.0 * i / 499.0);
            double lon = -180.0 + (360.0 * (i * 7 % 500) / 499.0);
            H3Cell c = new H3Cell(0x880000000000000L + i, lat, lon);
            c.setTemperature(15.0);
            c.setBiome(Biome.OCEAN);
            mesh.add(c);
        }

        // Apply AMOC with 0.40 Sv freshwater pulse
        ThermohalineStommelAMOCEngine.processThermohalineCirculationWithMeltwater(mesh, 0.40, 2.0);

        // Find a North Atlantic subpolar cell (e.g. 55°N, -25°E)
        H3Cell northAtlanticCell = mesh.stream()
                .filter(c -> c.getLatitude() > 50.0 && c.getLatitude() < 65.0 && c.getLongitude() > -40.0 && c.getLongitude() < 0.0)
                .findFirst().orElse(null);

        // Find a Southern Ocean cell (e.g. -55°S, -10°E)
        H3Cell southernOceanCell = mesh.stream()
                .filter(c -> c.getLatitude() < -45.0 && c.getLatitude() > -65.0 && c.getLongitude() > -40.0 && c.getLongitude() < 10.0)
                .findFirst().orElse(null);

        // Find an Equatorial Pacific cell (e.g. 0°N, 160°E - unperturbed by AMOC gyre)
        H3Cell pacificCell = mesh.stream()
                .filter(c -> Math.abs(c.getLatitude()) < 15.0 && c.getLongitude() > 140.0)
                .findFirst().orElse(null);

        assertNotNull(northAtlanticCell, "North Atlantic test cell must be present in 500-cell mesh");
        assertNotNull(southernOceanCell, "Southern Ocean test cell must be present in 500-cell mesh");
        assertNotNull(pacificCell, "Pacific test cell must be present in 500-cell mesh");

        assertTrue(northAtlanticCell.getTemperature() < 15.0,
                "North Atlantic cell must cool due to AMOC shutdown.");
        assertTrue(southernOceanCell.getTemperature() > 15.0,
                "Southern Ocean cell must warm due to bipolar seesaw heat accumulation.");
        assertEquals(15.0, pacificCell.getTemperature(), 1e-3,
                "Equatorial Pacific cell outside the Atlantic envelopes must remain unperturbed.");
    }
}
