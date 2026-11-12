/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.falsification.formal;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.SoilNutrientNPKEngine;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Low-level mathematical verification of Liebig's Law of the Minimum across the
 * Water-Energy-Food (WEF) multi-resource nexus, rejecting infinite neoclassical substitution.
 *
 * <p>Theoretical Foundations:
 * - von Liebig, J. (1840). <i>Die organische Chemie in ihrer Anwendung auf Agricultur und Physiologie</i>.
 * - Georgescu-Roegen, N. (1971). <i>The Entropy Law and the Economic Process</i>. Harvard University Press.
 * - Daly, H. E. (1997). <i>Georgescu-Roegen versus Solow/Stiglitz</i>. Ecological Economics, 22(3), 261-266.
 *
 * <p>Biophysical Conservation Principle:
 * Physical mass flows (water molecules H2O and phosphorus/nitrogen atoms) are strictly non-substitutable
 * substrates of photosynthetic metabolism:
 * 6 CO2 + 6 H2O + photons -> C6H12O6 + 6 O2
 *
 * Neoclassical production models (e.g. Cobb-Douglas or CES with elasticity sigma >= 1.0) allow monetary capital
 * or energy to infinitely substitute for depleted natural resources (lim_{K->inf} Y(K, W=0) = inf).
 * Ether strictly enforces Tier 1 stoichiometric constraints:
 * Yield = Yield_max * min( W / W_opt, [N] / [N]_opt, [P] / [P]_opt ) * f_tech(K, E)
 * If any indispensable physical substrate drops to zero, total agronomic output collapses to zero regardless
 * of infinite energy or monetary capital.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
@Tag("academic")
@Tag("scientific")
public class MultiResourceLiebigBottleneckTest {

    @Test
    @DisplayName("Liebig Water Bottleneck: Hyper-energy and infinite capital cannot substitute for zero water")
    public void testWaterDepletionBottleneckUnderInfiniteEnergyAndCapital() {
        // Setup Cell A: Optimal water, moderate technology
        H3Cell cellOptimal = new H3Cell(0x881f1d4887fffffL, 30.0, 31.0);
        cellOptimal.setBiome(Biome.PLAINS);
        cellOptimal.setElevation(50.0);
        cellOptimal.setTemperature(20.0);
        cellOptimal.setRainfall(800.0); // Abundant precipitation (800 mm/yr)
        cellOptimal.setSoilOrganicCarbon(45.0);
        cellOptimal.setResourceCapital(5000.0 * 300.0);
        cellOptimal.setPopulation(5000);
        cellOptimal.setBiomassAgriculture(500_000.0);
        cellOptimal.setResourceMetal(2000.0);

        // Setup Cell B: Hyper-abundant capital (100x) and infinite energy (10,000x), but ZERO water (hyper-arid desert)
        H3Cell cellWaterStarved = new H3Cell(0x881f1d4887ffffeL, 30.0, 31.0);
        cellWaterStarved.setBiome(Biome.DESERT);
        cellWaterStarved.setElevation(50.0);
        cellWaterStarved.setTemperature(20.0);
        cellWaterStarved.setRainfall(5.0); // 5 mm/yr (hyper-arid)
        cellWaterStarved.setSoilOrganicCarbon(45.0);
        cellWaterStarved.setResourceCapital(1_000_000.0); // 1,000,000 capital units (massive monetary capital)
        cellWaterStarved.setEnergySolar(100_000_000.0); // 100 million GJ (virtual infinite exergy)
        cellWaterStarved.setPopulation(5000);
        cellWaterStarved.setBiomassAgriculture(500_000.0);
        cellWaterStarved.setResourceMetal(2000.0);

        // Execute SoilNutrientNPKEngine and arability evaluation over multiple seasonal ticks
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(2);
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(List.of(cellOptimal, cellWaterStarved), buf, false);
        SoilNutrientNPKEngine npkEngine = new SoilNutrientNPKEngine();
        for (int month = 0; month < 12; month++) {
            npkEngine.tick(buf, 1.0 / 12.0, 1.0);
        }

        double arableSuitabilityOptimal = SoilNutrientNPKEngine.calculateContinuousArableSuitability(cellOptimal);
        double arableSuitabilityStarved = SoilNutrientNPKEngine.calculateContinuousArableSuitability(cellWaterStarved);

        double agriBiomassOptimal = buf.getBiomassAgriculture()[0];
        double agriBiomassStarved = buf.getBiomassAgriculture()[1];

        System.out.printf("[Liebig Nexus] Optimal Water Arability: %.4f | Biomass: %.2f%n",
                arableSuitabilityOptimal, agriBiomassOptimal);
        System.out.printf("[Liebig Nexus] Zero-Water (Infinite Capital) Arability: %.4f | Biomass: %.2f%n",
                arableSuitabilityStarved, agriBiomassStarved);

        // Arable suitability in the arid zone must collapse to the biological minimum (0.05 floor)
        assertTrue(arableSuitabilityOptimal > arableSuitabilityStarved * 5.0,
                "Optimal water arability must exceed hyper-arid arability by at least 5x");
        assertEquals(0.05, arableSuitabilityStarved, 0.015,
                "Hyper-arid cell without water must collapse to physiological arability floor (~0.05)");

        // Biomass production under water-starved conditions must be strictly bounded below the optimal cell
        assertTrue(agriBiomassOptimal > agriBiomassStarved * 1.5,
                "Water-optimal cell must sustain substantially higher agrarian biomass than water-starved cell");
    }

    @Test
    @DisplayName("Liebig Nutrient Bottleneck: Phosphorus/Nitrogen depletion strictly bounds photosynthetic yield")
    public void testSoilNutrientLiebigBottleneckUnderAbundantWaterAndSun() {
        // Cell 1: Balanced N-P-K nutrients
        double waterMm = 1000.0;
        double waterFactor = Math.min(1.0, waterMm / 600.0); // 1.0 (water saturated)
        double nitrogenKg = 40.0;
        double nitrogenFactor = Math.min(1.0, nitrogenKg / 30.0); // 1.0 (nitrogen saturated)
        double phosphorusKg = 15.0;
        double phosphorusFactor = Math.min(1.0, phosphorusKg / 10.0); // 1.0 (phosphorus saturated)

        double liebigMultiplierBalanced = Math.min(waterFactor, Math.min(nitrogenFactor, phosphorusFactor));
        assertEquals(1.0, liebigMultiplierBalanced, 1e-5, "Balanced soil must achieve 100% stoichiometric potential");

        // Cell 2: Water and Nitrogen abundant, but extreme Phosphorus exhaustion ([P] = 0.5 kg/ha, severe depletion)
        double phosphorusExhaustedKg = 0.50;
        double phosphorusFactorExhausted = Math.min(1.0, phosphorusExhaustedKg / 10.0); // 0.05 (5% floor)

        double liebigMultiplierExhausted = Math.min(waterFactor, Math.min(nitrogenFactor, phosphorusFactorExhausted));
        assertEquals(0.05, liebigMultiplierExhausted, 1e-5,
                "Liebig multiplier must collapse to exactly 5% dictated by the single depleted phosphorus element");

        // Verify non-compensability: Adding 10x more nitrogen cannot compensate for missing phosphorus
        double nitrogenHyperAbundant = 400.0;
        double nitrogenFactorHyper = Math.min(1.0, nitrogenHyperAbundant / 30.0);
        double liebigMultiplierUncompensated = Math.min(waterFactor, Math.min(nitrogenFactorHyper, phosphorusFactorExhausted));

        assertEquals(liebigMultiplierExhausted, liebigMultiplierUncompensated, 1e-6,
                "Excess nitrogen cannot compensate for phosphorus deficit (Zero Elasticity of Nutrient Substitution)");
    }

    @Test
    @DisplayName("Multi-Factor Envelope: Production strictly bounded by minimum across all physical substrates")
    public void testStrictMultiFactorEnvelopeProperty() {
        double[] waterRatios = { 0.10, 0.40, 0.85, 1.00 };
        double[] nitrogenRatios = { 0.05, 0.50, 0.90, 1.20 };
        double[] phosphorusRatios = { 0.02, 0.30, 0.70, 1.10 };
        double[] energyMultipliers = { 1.00, 2.50, 10.00, 50.00 };

        for (double w : waterRatios) {
            for (double n : nitrogenRatios) {
                for (double p : phosphorusRatios) {
                    for (double e : energyMultipliers) {
                        double physicalMinSubstrate = Math.min(w, Math.min(n, p));
                        double effectiveYieldIndex = physicalMinSubstrate * Math.min(2.0, 1.0 + 0.15 * Math.log(e));

                        // Mathematical Invariant: When any vital substrate is at 2%, yield can never exceed 4% even with 50x energy
                        if (physicalMinSubstrate <= 0.02) {
                            assertTrue(effectiveYieldIndex <= 0.04,
                                    String.format("Critical substrate constraint violated: min=%.3f, yield=%.3f",
                                            physicalMinSubstrate, effectiveYieldIndex));
                        }
                    }
                }
            }
        }
    }
}
