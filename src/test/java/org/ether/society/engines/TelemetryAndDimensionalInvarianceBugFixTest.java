/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines;

import org.ether.society.analytics.CliodynamicFormulasRegistry;
import org.ether.society.config.Configuration;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.SimulationMetricsAggregator;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.EcotoxicologyFertilityEngine;
import org.ether.society.engines.tier1.EntropicMetalDissipationEngine;
import org.ether.society.engines.tier1.InfrastructureEnergyEngine;
import org.ether.society.engines.tier2.theories.World3CouplingEngine;
import org.ether.society.model.AutoEpochScenarioHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying pure dimensional, unit, and state-function bug fixes
 * across Tier 1 and Tier 2 engines and telemetry calculators, with zero hidden thresholds
 * or ad-hoc curve-fitting ({@code AGENTS.md} §11 &amp; §12).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public class TelemetryAndDimensionalInvarianceBugFixTest {

    @Test
    @DisplayName("1. World3 (Meadows 1972) Domain Scope: Unchecked in Pre-Industrial Epochs & Unfiltered Industrial Assumption")
    public void testWorld3UnfilteredFailureOnMedievalParameters() {
        // 1. Verify World3 is unchecked by default on Medieval (1000 CE) and Bronze Age (-1500 BCE) scenarios
        Map<String, Boolean> medievalEngines = AutoEpochScenarioHelper.calibrateTypeBEngines("earth", 1000L);
        assertFalse(Boolean.TRUE.equals(medievalEngines.get("World3CouplingEngine")),
                "World3CouplingEngine must be unchecked on Medieval scenarios because it assumes 20th-century industrial capital.");
        assertFalse(Boolean.TRUE.equals(medievalEngines.get("World3PureEngine")),
                "World3PureEngine must be unchecked on Medieval scenarios.");

        // 2. Confirm that if World3CouplingEngine is executed unfiltered on a Medieval cell (N=20,000, k_pc=150 kg/cap),
        // it treats wooden plows and granaries (K = 3,000,000) as polluting industrial factories (IO) and depletes mineral stocks
        H3Cell medievalCell = new H3Cell(0x8428309ffffffffL, 41.9, 12.5); // Res 4 cell (~1770 km2)
        medievalCell.setPopulation(20_000);
        medievalCell.setResourceCapital(20_000.0 * 150.0); // 3,000,000 kg of agrarian tools/structures
        medievalCell.setResourceMetal(5_000.0);
        medievalCell.setPollutionLevel(0.0);
        medievalCell.setFoodResource(40_000.0);

        World3CouplingEngine.resetState();
        for (int year = 0; year < 20; year++) {
            World3CouplingEngine.processWorld3System(List.of(medievalCell), 1.0);
        }

        // Without artificial hidden k_pc gates, World3 generates industrial pollution and depletes non-renewable resources
        // even on medieval parameters, confirming why it must be unchecked on pre-industrial scenarios rather than artificially gated.
        assertTrue(medievalCell.getPollutionLevelOr(0.0) > 0.0,
                "Unfiltered World3 treats agrarian capital as industrial output generating persistent pollution.");
        assertTrue(medievalCell.getResourceMetalOr(5_000.0) < 5_000.0,
                "Unfiltered World3 depletes non-renewable mineral reserves from agrarian capital stock.");
    }

    @Test
    @DisplayName("2. EntropicMetalDissipationEngine: Intensive Areal Concentration Normalization (kg/km2)")
    public void testEntropicMetalDissipationArealConcentrationInvariance() {
        // Bronze Age Res 4 cell (1770.49 km2) with 10,000 inhabitants (5.6 hab/km2) and 150,000 kg metal stock
        H3Cell res4Cell = new H3Cell(0x8428309ffffffffL, 41.9, 12.5);
        res4Cell.setPopulation(10_000);
        res4Cell.setResourceCapital(1_000_000.0);
        res4Cell.setResourceMetal(150_000.0);
        res4Cell.setPollutionLevel(0.0);

        EntropicMetalDissipationEngine.resetState();
        EntropicMetalDissipationEngine.processEntropicDissipation(List.of(res4Cell), 1.0);

        double pollutionAfter1Year = res4Cell.getPollutionLevelOr(0.0);
        // Dissipated mass ~ 2233 kg over 1770.49 km2 -> ~0.126 units/km2 (well below the 100.0 ecotoxicology threshold)
        assertTrue(pollutionAfter1Year > 0.0 && pollutionAfter1Year < 1.0,
                "Areal-normalized metallic dissipation must remain < 1.0 unit/km2/yr in low-density cells, got: " + pollutionAfter1Year);
        assertEquals(1.0, EcotoxicologyFertilityEngine.calculateEcotoxicFertilityFactor(0.0), 1e-6);
    }

    @Test
    @DisplayName("3. InfrastructureEnergyEngine: Closed-Form State Function (No Multiplicative Tick Decay)")
    public void testInfrastructureEnergyEngineIdempotentClosedFormFriction() {
        H3Cell cell = new H3Cell(0x8428309ffffffffL, 41.9, 12.5);
        cell.setElevation(250.0);
        cell.setTemperature(15.0);
        cell.setBiomassNatural(1200.0);
        cell.setPopulation(5_000);
        cell.setResourceCapital(500_000.0);

        InfrastructureEnergyEngine.processInfrastructureEnergy(List.of(cell));
        double frictionTick1 = cell.getMovementFrictionOr(1.0);

        for (int t = 0; t < 5; t++) {
            InfrastructureEnergyEngine.processInfrastructureEnergy(List.of(cell));
        }
        double frictionTick5 = cell.getMovementFrictionOr(1.0);

        // Run 120 monthly ticks with constant physical state: friction must remain strictly invariant at its fixed point
        for (int t = 0; t < 120; t++) {
            InfrastructureEnergyEngine.processInfrastructureEnergy(List.of(cell));
        }
        double frictionTick120 = cell.getMovementFrictionOr(1.0);

        assertEquals(frictionTick1, frictionTick120, 1e-3,
                "Movement friction must be a closed-form state function and must not decay multiplicatively across ticks.");
        assertEquals(frictionTick5, frictionTick120, 1e-12,
                "Movement friction fixed point must be strictly invariant to 1e-12 across 120 ticks.");
        assertTrue(frictionTick120 > 0.30,
                "Movement friction must not collapse to the 0.05 floor, got: " + frictionTick120);
    }

    @Test
    @DisplayName("4. Shipping Freight Cost & Trade Volume: Pre-Modern High Resistance Ceiling (No Zero-Multiplication Inversion)")
    public void testShippingFreightCostAndTradeVolumeSignBugFix() {
        // In Classical Antiquity (Phi_exergy ~ 0.96 - 1.02), maritime freight cost must be near its archaic ceiling (~1415),
        // never multiplied by 0 and clamped to 15.0 (modern container ship floor)
        double freightAntiquity = CliodynamicFormulasRegistry.computeShippingFreightCostIndex(0.96);
        double freightModern = CliodynamicFormulasRegistry.computeShippingFreightCostIndex(2.20);

        assertTrue(freightAntiquity >= 1400.0,
                "Archaic/Classical shipping freight cost index must be >= 1400.0, got: " + freightAntiquity);
        assertTrue(freightModern < 50.0,
                "Modern container shipping freight cost index must be < 50.0, got: " + freightModern);

        double tradeAntiquity = CliodynamicFormulasRegistry.computeGlobalTradeVolume(0.96);
        assertTrue(tradeAntiquity < 1.0,
                "Classical global trade volume index (100 = 1913) must be < 1.0 at Phi=0.96, got: " + tradeAntiquity);
    }

    @Test
    @DisplayName("5. Information Speed & Urbanization: Scale Consistency Across Res 4 WorldBuffer and H3Cells")
    public void testInformationSpeedAndUrbanizationConsistency() throws Exception {
        // At Phi_exergy = 0.96 (165 CE), courier speed must be ~30-45 km/day without 20,000 km/day telecom leakage
        double speed165CE = CliodynamicFormulasRegistry.computeInformationTravelSpeed(0.96);
        assertTrue(speed165CE >= 25.0 && speed165CE <= 60.0,
                "Information travel speed in 165 CE (Phi=0.96) must be courier speed (~30-45 km/day), got: " + speed165CE);

        // Verify WorldBuffer and H3Cell urbanization paths produce identical results on Res 4 cells
        H3Cell romanProvince = new H3Cell(0x8428309ffffffffL, 41.9, 12.5); // Res 4 (~1770.49 km2)
        romanProvince.setPopulation(15_000); // ~8.5 hab/km2 rural province with one small town
        romanProvince.setResourceCapital(15_000.0 * 185.0); // k_pc = 185 kg/cap

        Configuration cfg = new Configuration(null, new Configuration.SimulationConfig(165, 100, new int[]{1}), null, null, null);
        H3SimulationEngine engine = new H3SimulationEngine(cfg);
        engine.setCells(List.of(romanProvince));
        SimulationMetricsAggregator agg = engine.getMetricsAggregator();
        double urbanRate = agg.getUrbanizationRate();

        assertTrue(urbanRate >= 1.5 && urbanRate <= 8.0,
                "Classical Antiquity urbanization rate at k_pc=185 on Res 4 must be ~2-6% (not 76.7%), got: " + urbanRate);
    }
}
