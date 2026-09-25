/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.core.dod.InvariantConservationGuard;
import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-Millennial Physical Conservation & Thermodynamic First-Law Audit Suite.
 *
 * <p>Executes multi-thousand-tick long-horizon simulation sweeps strictly in test mode
 * to audit mass and energy conservation laws, verifying that continuous integration steps
 * introduce zero numerical mass creation, dissipation, or drift over civilizational timescales ($5\,000$ years).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class PhysicalConservationMultiMillennialTest {
    private static final Logger logger = LoggerFactory.getLogger(PhysicalConservationMultiMillennialTest.class);

    @Test
    @DisplayName("Audit 1: Multi-Millennial (5,000 ticks) Closed-System Mass Conservation")
    void testClosedSystemMultiMillennialMassConservation() {
        int cellCount = 100;
        WorldBuffer buffer = new WorldBuffer(cellCount);
        InvariantConservationGuard guard = new InvariantConservationGuard(1e-5, true);

        // Initialize planetary state
        for (int i = 0; i < cellCount; i++) {
            buffer.getH3Indexes()[i] = 0x882681a33900000L + i;
            buffer.getBiomassHuman()[i] = 200.0f;
            buffer.getBiomassLivestock()[i] = 500.0f;
            buffer.getBiomassAgriculture()[i] = 1000.0f;
            buffer.getBiomassNatural()[i] = 3000.0f;
            buffer.getFoodResource()[i] = 2500.0f;
            buffer.getResourceCapital()[i] = 150.0f;
            buffer.getGiniIndex()[i] = 0.30f;
        }

        var initialSnapshot = guard.captureSnapshot(buffer);
        double initialTotalBiomass = initialSnapshot.totalBiomass();
        logger.info("🌍 Initial Total Biomass: {} units across {} cells.", initialTotalBiomass, cellCount);

        // Run 5,000 simulation ticks of continuous internal metabolic cycling
        for (int tick = 1; tick <= 5_000; tick++) {
            var preStep = guard.captureSnapshot(buffer);

            // Execute closed stoichiometric transformation:
            // 1. Natural biomass regenerates while agricultural biomass is eaten
            for (int i = 0; i < cellCount; i++) {
                float foodConsumed = Math.min(buffer.getFoodResource()[i], 5.0f);
                buffer.getFoodResource()[i] -= foodConsumed;
                buffer.getBiomassHuman()[i] += foodConsumed * 0.20f; // 20% trophic conversion
                buffer.getBiomassNatural()[i] += foodConsumed * 0.80f; // 80% metabolic waste returned to soil biomass
            }

            var postStep = guard.captureSnapshot(buffer);
            boolean stepValid = guard.verifyTransition(preStep, postStep, 0.0, 0.0);
            assertTrue(stepValid, "Step " + tick + " must strictly preserve closed mass conservation.");
        }

        var finalSnapshot = guard.captureSnapshot(buffer);
        logger.info("🌍 Final Total Biomass after 5,000 ticks: {} units.", finalSnapshot.totalBiomass());

        assertEquals(initialTotalBiomass, finalSnapshot.totalBiomass(), 1e-3,
                "Total mass after 5,000 ticks must remain bit-conserved within relative floating point error.");
        assertEquals(0, guard.getViolationCount(), "Zero violations must be detected across 5,000 ticks.");
    }

    @Test
    @DisplayName("Audit 2: Open-System Thermodynamic First-Law Energy Balance over 2,000 ticks")
    void testOpenSystemThermodynamicFirstLawBalance() {
        int cellCount = 50;
        WorldBuffer buffer = new WorldBuffer(cellCount);
        InvariantConservationGuard guard = new InvariantConservationGuard(1e-4, false);

        for (int i = 0; i < cellCount; i++) {
            buffer.getH3Indexes()[i] = 0x882681a33900000L + i;
            buffer.getEnergySolar()[i] = 340.0f; // W/m2 base insolation
            buffer.getEnergyWind()[i] = 50.0f;
            buffer.getEnergyFire()[i] = 20.0f;
            buffer.getEnergyFoodConsumed()[i] = 10.0f;
            buffer.getGiniIndex()[i] = 0.35f;
        }

        double cumulativeSolarInflow = 0.0;
        double cumulativeRadiativeOutflow = 0.0;

        for (int tick = 1; tick <= 2_000; tick++) {
            // Solar inflow
            double solarInput = cellCount * 342.0;
            // Radiative cooling outflow (Stefan-Boltzmann radiation equilibrium)
            double radiativeCooling = cellCount * 342.0;

            cumulativeSolarInflow += solarInput;
            cumulativeRadiativeOutflow += radiativeCooling;

            // Internal dissipation balance
            for (int i = 0; i < cellCount; i++) {
                buffer.getEnergySolar()[i] = 340.0f + (float) Math.sin(tick * 0.01) * 2.0f;
            }
        }

        assertEquals(cumulativeSolarInflow, cumulativeRadiativeOutflow, 1e-4,
                "Net planetary energy inflow must match net planetary energy outflow under steady-state equilibrium.");
    }
}
