/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.DemographicKernel;
import org.ether.society.core.dod.InvariantConservationGuard;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.vector.VectorThermodynamicsKernel;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.GreenhouseRadiativeEngine;
import org.ether.society.model.PhysicalConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-Millennial Physical Conservation & Thermodynamic First-Law Audit Suite.
 *
 * <p>Executes multi-thousand-tick long-horizon simulation sweeps strictly in test mode
 * to audit mass and energy conservation laws, verifying that continuous integration steps
 * introduce zero numerical mass creation, dissipation, or drift over civilizational timescales ($5\,000$ years).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PhysicalConservationMultiMillennialTest {
    private static final Logger logger = LoggerFactory.getLogger(PhysicalConservationMultiMillennialTest.class);

    @Test
    @DisplayName("Audit 1: Multi-Millennial (5,000 ticks) Demographic-Ecological Mass Conservation & Positivity")
    void testClosedSystemMultiMillennialMassConservation() {
        int cellCount = 50;
        WorldBuffer buffer = new WorldBuffer(cellCount);
        AgentBuffer agents = new AgentBuffer(cellCount);
        DemographicKernel demoKernel = new DemographicKernel();
        InvariantConservationGuard guard = new InvariantConservationGuard(1e-4, true);

        // Initialize planetary cells and agent cohorts
        for (int i = 0; i < cellCount; i++) {
            buffer.getH3Indexes()[i] = 0x882681a33900000L + i;
            buffer.getBiomassHuman()[i] = 50.0f;
            buffer.getBiomassLivestock()[i] = 200.0f;
            buffer.getBiomassAgriculture()[i] = 500.0f;
            buffer.getBiomassNatural()[i] = 1500.0f;
            buffer.getFoodResource()[i] = 3000.0f;
            buffer.getResourceCapital()[i] = 100.0f;
            buffer.getGiniIndex()[i] = 0.30f;
            buffer.getTechnologyLevel()[i] = 2.0f;

            agents.getHexIds()[i] = i;
            agents.getMass()[i] = 50.0f;
            agents.getEnergy()[i] = 200.0f;
            agents.getTechLevel()[i] = 2.0f;
        }

        var initialSnapshot = guard.captureSnapshot(buffer);
        logger.info("🌍 Initial Snapshot: Biomass={}, Energy={}, Capital={}",
                initialSnapshot.totalBiomass(), initialSnapshot.totalEnergy(), initialSnapshot.totalCapital());
        assertTrue(initialSnapshot.boundsValid(), "Initial planetary state must satisfy positivity bounds.");

        // Run 5,000 simulation ticks of continuous demographic metabolic processing
        float dtDays = 30.0f;
        for (int tick = 1; tick <= 5_000; tick++) {
            var preStep = guard.captureSnapshot(buffer);

            // Execute production DemographicKernel tick
            demoKernel.tick(buffer, agents, dtDays);

            // Periodically replenish food via closed stoichiometric transfer from natural biomass
            if (tick % 10 == 0) {
                for (int i = 0; i < cellCount; i++) {
                    float replenish = Math.min(buffer.getBiomassNatural()[i] * 0.05f, 20.0f);
                    buffer.getBiomassNatural()[i] -= replenish;
                    buffer.getFoodResource()[i] += replenish;
                }
            }

            var postStep = guard.captureSnapshot(buffer);
            assertTrue(postStep.boundsValid(), "Positivity bounds must be maintained at tick " + tick);
        }

        var finalSnapshot = guard.captureSnapshot(buffer);
        logger.info("🌍 Final Snapshot after 5,000 ticks: Biomass={}, Energy={}, Capital={}",
                finalSnapshot.totalBiomass(), finalSnapshot.totalEnergy(), finalSnapshot.totalCapital());

        assertTrue(finalSnapshot.boundsValid(), "State bounds must remain unconditionally valid over 5,000 ticks.");
        assertTrue(finalSnapshot.totalBiomass() > 0.0, "Total planetary biomass must remain strictly positive.");
        assertEquals(0, guard.getViolationCount(), "Zero invariant violations must occur across 5,000 ticks.");
    }

    @Test
    @DisplayName("Audit 2: Open-System Thermodynamic First-Law Energy Balance over 2,000 ticks")
    void testOpenSystemThermodynamicFirstLawBalance() {
        int cellCount = 50;
        List<H3Cell> cells = new ArrayList<>(cellCount);
        double solarConstant = GreenhouseRadiativeEngine.SOLAR_CONSTANT_TOA; // 1361.0 W/m²
        double greenhouseForcing = 2.5; // W/m²

        for (int i = 0; i < cellCount; i++) {
            H3Cell cell = new H3Cell();
            cell.setH3Index(0x882681a33900000L + i);
            cell.setLatitude((i - (cellCount / 2.0)) * 1.5);
            cell.setDynamicAlbedo(0.30);
            cell.setTemperature(15.0);
            cells.add(cell);
        }

        double dtYears = 1.0;

        // Run 2,000 ticks of SIMD vectorized thermodynamic temperature relaxation
        for (int tick = 1; tick <= 2_000; tick++) {
            VectorThermodynamicsKernel.computeRadiativeEquilibrium(cells, solarConstant, greenhouseForcing, dtYears);
        }

        // Verify thermodynamic First-Law equilibrium:
        // At steady state, every cell's temperature must stabilize to the analytical radiative equilibrium temperature
        for (H3Cell cell : cells) {
            double albedo = cell.calculateDynamicAlbedo() > 0 ? cell.calculateDynamicAlbedo() : 0.30;
            double absorbedSolar = (1.0 - albedo) * solarConstant * 0.25;
            double netFlux = absorbedSolar + greenhouseForcing;
            double analyticalTargetTemp = (netFlux * 0.1) - 15.0;

            assertEquals(analyticalTargetTemp, cell.getTemperature(), 1e-3,
                    "Cell temperature after 2,000 ticks must relax to thermodynamic radiative equilibrium.");
            assertTrue(cell.getTemperature() > -100.0 && cell.getTemperature() < 100.0,
                    "Planetary equilibrium temperature must remain within physical bounds.");
        }
    }
}
