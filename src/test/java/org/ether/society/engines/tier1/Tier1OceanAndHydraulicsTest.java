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
 * @version 1.0.0-beta.3
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
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        buf.getRainfall()[0] = 800.0f;
        buf.getBiomassAgriculture()[0] = 50.0f;

        new ManningStricklerHydrodynamicsEngine().tick(buf, 1.0, 12.0);
        assertTrue(buf.getBiomassAgriculture()[0] > 50.0f, "Fertile floodplain must boost agricultural biomass");
    }

    @Test
    @DisplayName("Manning-Strickler Upstream Catchment: Highland rainfall runoff accumulates in lowland river floodplains")
    public void testManningStricklerUpstreamCatchmentRunoffAccumulation() {
        org.ether.society.core.dod.WorldBuffer isolatedBuf = new org.ether.society.core.dod.WorldBuffer(1);
        isolatedBuf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        isolatedBuf.getElevation()[0] = 25.0f;
        isolatedBuf.getRainfall()[0] = 100.0f;
        isolatedBuf.getBiomassAgriculture()[0] = 50.0f;
        isolatedBuf.getBiomassHuman()[0] = (float) (5000 * 0.06);

        ManningStricklerHydrodynamicsEngine engine = new ManningStricklerHydrodynamicsEngine();
        engine.tick(isolatedBuf, 1.0, 12.0);
        double bioIsolated = isolatedBuf.getBiomassAgriculture()[0];

        org.ether.society.core.dod.WorldBuffer watershedBuf = new org.ether.society.core.dod.WorldBuffer(5);
        watershedBuf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        watershedBuf.getElevation()[0] = 25.0f;
        watershedBuf.getRainfall()[0] = 100.0f;
        watershedBuf.getBiomassAgriculture()[0] = 50.0f;
        watershedBuf.getBiomassHuman()[0] = (float) (5000 * 0.06);
        for (int i = 1; i < 5; i++) {
            watershedBuf.getBiomes()[i] = (byte) Biome.MOUNTAINS.ordinal();
            watershedBuf.getElevation()[i] = 2500.0f;
            watershedBuf.getRainfall()[i] = 1600.0f;
        }

        engine.tick(watershedBuf, 1.0, 12.0);
        double bioCoupled = watershedBuf.getBiomassAgriculture()[0];

        assertTrue(bioCoupled >= bioIsolated,
                "Lowland delta with upstream mountain catchment must receive higher or equal river discharge and fertile silt");
        assertTrue(bioCoupled > 50.0, "Floodplain biomass must experience alluvial fertility boost");
        assertTrue(bioCoupled <= 3000.0, "Floodplain agricultural biomass must respect upper physical carrying bound (3000.0)");
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

        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getElevation()[0] = 5000.0f;
        buf.getMetalResource()[0] = 100.0f;

        new AiryIsostasyCrustalRootEngine().tick(buf, 1.0, 12.0);
        assertTrue(buf.getMetalResource()[0] > 100.0f, "Orogenic roots must concentrate hydrothermal metal ores");
    }
}

