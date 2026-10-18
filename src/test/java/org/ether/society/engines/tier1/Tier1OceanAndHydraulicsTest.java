/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

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
 * @version 1.0.0-beta.2
 */
public class Tier1OceanAndHydraulicsTest {

    @Test
    @DisplayName("Thermohaline Stommel AMOC Engine: Seawater density, overturning flux, and tipping point")
    /*
     * Test thermohaline stommel amoc operation.
     * <p>
     * Executes operational logic for {@code Tier1OceanAndHydraulicsTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
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
    /*
     * Test manning strickler hydrodynamics operation.
     * <p>
     * Executes operational logic for {@code Tier1OceanAndHydraulicsTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
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
    @DisplayName("Manning-Strickler Upstream Catchment: Highland rainfall runoff accumulates in lowland river floodplains")
    /*
     * Test upstream catchment runoff accumulation operation.
     * <p>
     * Executes operational logic for {@code Tier1OceanAndHydraulicsTest} within the automated verification and regression test suite.
     * Enforces physical invariants: mass conservation of runoff, monotonic discharge growth, and alluvial silt enrichment.
     * </p>
     */
    public void testManningStricklerUpstreamCatchmentRunoffAccumulation() {
        // Setup 1: Isolated arid lowland delta without upstream catchment
        H3Cell deltaIsolated = new H3Cell(0x8828308281fffffL, 30.5, 31.5);
        deltaIsolated.setBiome(Biome.PLAINS);
        deltaIsolated.setElevation(25.0);
        deltaIsolated.setRainfall(100.0); // Arid desert rainfall (Nile Delta local climate)
        deltaIsolated.setBiomassAgriculture(50.0);
        deltaIsolated.setPopulation(5000);

        List<H3Cell> isolatedList = new ArrayList<>(List.of(deltaIsolated));
        ManningStricklerHydrodynamicsEngine.processRiverHydrodynamics(isolatedList, 1.0);
        double bioIsolated = deltaIsolated.getBiomassAgriculture();

        // Setup 2: Arid lowland delta coupled with elevated mountain catchment cells (Ethiopian highlands)
        H3Cell deltaCoupled = new H3Cell(0x8828308281fffffL, 30.5, 31.5);
        deltaCoupled.setBiome(Biome.PLAINS);
        deltaCoupled.setElevation(25.0);
        deltaCoupled.setRainfall(100.0);
        deltaCoupled.setBiomassAgriculture(50.0);
        deltaCoupled.setPopulation(5000);

        List<H3Cell> watershed = new ArrayList<>();
        watershed.add(deltaCoupled);
        for (int i = 0; i < 4; i++) {
            H3Cell mountain = new H3Cell(0x882830828100000L + i, 12.0 + i, 38.0);
            mountain.setBiome(Biome.MOUNTAINS);
            mountain.setElevation(2500.0); // Highlands > 100m
            mountain.setRainfall(1600.0); // Heavy tropical monsoon rainfall
            watershed.add(mountain);
        }

        ManningStricklerHydrodynamicsEngine.processRiverHydrodynamics(watershed, 1.0);
        double bioCoupled = deltaCoupled.getBiomassAgriculture();

        // Assert that upstream watershed runoff significantly boosted lowland agricultural biomass via river discharge
        assertTrue(bioCoupled > bioIsolated,
                "Lowland delta with upstream mountain catchment must receive higher river discharge and fertile silt than isolated arid delta");
        assertTrue(bioCoupled > 50.0, "Floodplain biomass must experience alluvial fertility boost");
        assertTrue(bioCoupled <= 3000.0, "Floodplain agricultural biomass must respect upper physical carrying bound (3000.0)");
    }

    @Test
    @DisplayName("Airy Isostasy Engine: Mountain crustal roots and crustal thickness")
    /*
     * Test airy isostasy crustal root operation.
     * <p>
     * Executes operational logic for {@code Tier1OceanAndHydraulicsTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
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

