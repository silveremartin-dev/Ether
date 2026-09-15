/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying Tier 1 oceanographic and geomorphological engines:
 * 1. Thermohaline Stommel AMOC 2-Box Ocean Circulation & Tipping Point Engine
 * 2. Manning-Strickler River Hydrodynamics & Floodplain Inundation Engine
 * 3. Airy-Heiskanen Tectonic Isostasy & Crustal Root Engine
 *
 * @author Silvere Martin-Michiellot
 * @version 4.5.0
 */
public class Tier1OceanAndHydraulicsTest {

    @Test
    @DisplayName("Thermohaline Stommel AMOC Engine: Seawater density, overturning flux, and tipping point")
    public void testThermohalineStommelAMOC() {
        // Cold salty water must be denser than warm fresh water
        double densityColdSalty = ThermohalineStommelAMOCEngine.calculateSeawaterDensity(2.0, 35.0);
        double densityWarmFresh = ThermohalineStommelAMOCEngine.calculateSeawaterDensity(25.0, 32.0);
        assertTrue(densityColdSalty > densityWarmFresh, "Cold salty water must have higher density");

        // Normal AMOC state (equator warm/salty, pole cold/salty)
        ThermohalineStommelAMOCEngine.StommelState normalState =
                ThermohalineStommelAMOCEngine.calculateStommelAMOC(28.0, 2.0, 36.5, 34.5);
        assertFalse(normalState.isCollapsed(), "Baseline AMOC must be active");
        assertTrue(normalState.amocFlowSv() > 10.0, "Baseline AMOC flow must be > 10 Sv");

        // Meltwater freshening pulse -> AMOC collapse
        ThermohalineStommelAMOCEngine.StommelState collapsedState =
                ThermohalineStommelAMOCEngine.calculateStommelAMOC(28.0, 2.0, 36.5, 29.0);
        assertTrue(collapsedState.isCollapsed(), "Extreme freshening must trigger AMOC collapse");
        assertTrue(collapsedState.northAtlanticCoolingShiftC() < -5.0, "Collapse must cause severe North Atlantic cooling");
    }

    @Test
    @DisplayName("Manning-Strickler Hydrodynamics Engine: Flow velocity, discharge, and floodplain fertilization")
    public void testManningStricklerHydrodynamics() {
        // Flow velocity in natural river (Rh = 2m, slope = 0.001)
        double v = ManningStricklerHydrodynamicsEngine.calculateFlowVelocity(2.0, 0.001, 0.035);
        assertTrue(v > 1.0 && v < 2.0, "Natural river flow velocity must be ~1.4 m/s");

        // Volumetric discharge for 50m wide, 3m deep river
        double discharge = ManningStricklerHydrodynamicsEngine.calculateDischarge(50.0, 3.0, 0.001);
        assertTrue(discharge > 150.0, "River discharge must be substantial (>150 m³/s)");

        // Process grid with floodplain
        List<H3Cell> cells = new ArrayList<>();
        H3Cell nileDelta = new H3Cell();
        nileDelta.setBiome(Biome.PLAINS);
        nileDelta.setRainfall(800.0);
        nileDelta.setBiomassAgriculture(50.0);
        cells.add(nileDelta);

        ManningStricklerHydrodynamicsEngine.processRiverHydrodynamics(cells, 1.0);
        assertTrue(nileDelta.getBiomassAgriculture() > 50.0, "Fertile floodplain must boost agricultural biomass");
    }

    @Test
    @DisplayName("Airy Isostasy Engine: Mountain crustal roots and crustal thickness")
    public void testAiryIsostasyCrustalRoot() {
        // 4000m peak must have 18000m root
        double root = AiryIsostasyCrustalRootEngine.calculateMountainRootDepthMeters(4000.0);
        assertEquals(18000.0, root, 1e-3);

        // Total crust thickness under 4000m mountain = 35 + 4 + 18 = 57 km
        double totalCrust = AiryIsostasyCrustalRootEngine.calculateTotalCrustThicknessKm(4000.0);
        assertEquals(57.0, totalCrust, 1e-3);

        // Process cells
        List<H3Cell> cells = new ArrayList<>();
        H3Cell himalayas = new H3Cell();
        himalayas.setElevation(5000.0);
        himalayas.setResourceMetal(100.0);
        cells.add(himalayas);

        AiryIsostasyCrustalRootEngine.processAiryIsostasy(cells);
        assertTrue(himalayas.getResourceMetal() > 100.0, "Orogenic roots must concentrate hydrothermal metal ores");
    }
}
