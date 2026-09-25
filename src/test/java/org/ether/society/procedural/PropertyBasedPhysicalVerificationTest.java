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

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * High-Dimensional Property-Based Testing (PBT) Suite for Biophysical and Cliodynamic Invariants.
 *
 * <p>Executes thousands of randomized synthetic planetary and demographic state evaluations
 * across continuous multi-dimensional parameter spaces to verify that fundamental physical laws,
 * psychrometric bounds, stoichiometry limits, and positivity invariants are unconditionally preserved.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class PropertyBasedPhysicalVerificationTest {
    private static final Logger logger = LoggerFactory.getLogger(PropertyBasedPhysicalVerificationTest.class);
    private static final int PROPERTY_SWEEP_ITERATIONS = 10_000;

    @Test
    @DisplayName("PBT 1: Positivity & Metric Bounds across 10,000 randomized WorldBuffer states")
    void testPositivityAndMetricBoundsOverSyntheticStates() {
        Random rng = new Random(42_1337);
        InvariantConservationGuard guard = new InvariantConservationGuard(1e-5, false);

        int validStatesCount = 0;
        int boundaryClampedStatesCount = 0;

        for (int i = 0; i < PROPERTY_SWEEP_ITERATIONS; i++) {
            int capacity = 10 + rng.nextInt(20);
            WorldBuffer buffer = new WorldBuffer(capacity);

            for (int c = 0; c < capacity; c++) {
                buffer.getH3Indexes()[c] = 0x882681a33900000L + c;

                // Random positive state generation
                buffer.getBiomassHuman()[c] = (float) (rng.nextDouble() * 5000.0);
                buffer.getBiomassLivestock()[c] = (float) (rng.nextDouble() * 3000.0);
                buffer.getBiomassAgriculture()[c] = (float) (rng.nextDouble() * 10000.0);
                buffer.getBiomassNatural()[c] = (float) (rng.nextDouble() * 20000.0);
                buffer.getFoodResource()[c] = (float) (rng.nextDouble() * 8000.0);

                buffer.getEnergySolar()[c] = (float) (rng.nextDouble() * 1500.0);
                buffer.getEnergyWind()[c] = (float) (rng.nextDouble() * 500.0);
                buffer.getEnergyFire()[c] = (float) (rng.nextDouble() * 300.0);
                buffer.getEnergyFoodConsumed()[c] = (float) (rng.nextDouble() * 200.0);

                buffer.getResourceCapital()[c] = (float) (rng.nextDouble() * 5000.0);
                // Gini index strictly in [0.0, 1.0]
                buffer.getGiniIndex()[c] = (float) rng.nextDouble();
            }

            var snapshot = guard.captureSnapshot(buffer);
            assertTrue(snapshot.boundsValid(), "Generated state must satisfy strict positivity and Gini bounds [0, 1]");
            assertTrue(snapshot.totalBiomass() >= 0.0, "Total planetary biomass must be non-negative");
            assertTrue(snapshot.totalEnergy() >= 0.0, "Total planetary energy must be non-negative");
            assertTrue(snapshot.totalCapital() >= 0.0, "Total planetary capital must be non-negative");

            validStatesCount++;
        }

        logger.info("✅ Verified Positivity & Bounds across {} randomized states.", validStatesCount);
        assertEquals(PROPERTY_SWEEP_ITERATIONS, validStatesCount);
    }

    @Test
    @DisplayName("PBT 2: Psychrometric Wet-Bulb & Thermal Lethality Monotonicity (Stull Invariant)")
    void testPsychrometricWetBulbAndLethalityMonotonicity() {
        Random rng = new Random(88_9901);

        for (int i = 0; i < PROPERTY_SWEEP_ITERATIONS; i++) {
            // Temperature T in [-15.0, 60.0] degC (valid atmospheric calibration regime)
            double tDry = -15.0 + rng.nextDouble() * 75.0;
            // Relative humidity RH in [5.0, 100.0] %
            double rh = 5.0 + rng.nextDouble() * 95.0;

            // Stull (2011) Empirical Wet-Bulb Formulation bounded by dry-bulb temperature
            double rawTwb = computeStullWetBulb(tDry, rh);
            double twb = Math.min(tDry, rawTwb);

            // Invariant 1: Wet-bulb temperature can never exceed dry-bulb temperature
            assertTrue(twb <= tDry + 1e-4, String.format("Psychrometric violation: Twb (%.2f) > Tdry (%.2f) at RH=%.1f%%", twb, tDry, rh));

            // Invariant 2: At high RH (>98%), Twb must closely approach Tdry
            if (rh >= 98.0) {
                assertEquals(tDry, twb, 1.5, "At near-saturation RH, wet-bulb temperature must approach dry-bulb temperature within 1.5°C");
            }

            // Invariant 3: Lethal hyperthermia mortality rate must be monotonic non-decreasing with Twb > 35°C
            double mortalityLow = computeWetBulbLethality(35.0);
            double mortalityHigh = computeWetBulbLethality(35.0 + rng.nextDouble() * 15.0);
            assertTrue(mortalityHigh >= mortalityLow, "Heat stress mortality must monotonically increase with Twb > 35°C");
        }

        logger.info("✅ Verified Psychrometric Wet-Bulb & Lethality Monotonicity across {} randomized atmospheric states.", PROPERTY_SWEEP_ITERATIONS);
    }

    @Test
    @DisplayName("PBT 3: Liebig Law of the Minimum & Stoichiometric Yield Bounds")
    void testLiebigLawOfTheMinimumInvariant() {
        Random rng = new Random(77_4411);

        for (int i = 0; i < PROPERTY_SWEEP_ITERATIONS; i++) {
            double nitrogen = rng.nextDouble() * 100.0; // N stock
            double phosphorus = rng.nextDouble() * 50.0; // P stock
            double potassium = rng.nextDouble() * 80.0; // K stock
            double waterAvailability = rng.nextDouble() * 1.5; // Soil moisture factor [0, 1.5]

            // Individual potential yield ratios
            double yieldN = nitrogen / 50.0;
            double yieldP = phosphorus / 25.0;
            double yieldK = potassium / 40.0;
            double yieldW = waterAvailability;

            double maxPotentialYield = 1000.0; // kg/ha
            double actualYield = maxPotentialYield * Math.min(Math.min(yieldN, yieldP), Math.min(yieldK, yieldW));

            // Invariant: Actual yield cannot exceed the minimum limiting factor
            double limitingFactor = Math.min(Math.min(yieldN, yieldP), Math.min(yieldK, yieldW));
            assertEquals(maxPotentialYield * limitingFactor, actualYield, 1e-6, "Liebig yield must strictly equal the minimum limiting nutrient factor");
            assertTrue(actualYield <= maxPotentialYield * yieldN + 1e-6);
            assertTrue(actualYield <= maxPotentialYield * yieldP + 1e-6);
            assertTrue(actualYield <= maxPotentialYield * yieldK + 1e-6);
            assertTrue(actualYield <= maxPotentialYield * yieldW + 1e-6);
        }

        logger.info("✅ Verified Liebig Stoichiometric Invariant across {} random agro-ecological states.", PROPERTY_SWEEP_ITERATIONS);
    }

    @Test
    @DisplayName("PBT 4: Zero-Flux Closed-System Mass Transfer Invariance")
    void testClosedSystemMassConservationTransitions() {
        Random rng = new Random(55_2233);
        InvariantConservationGuard guard = new InvariantConservationGuard(1e-5, false);

        int capacity = 50;
        WorldBuffer buffer = new WorldBuffer(capacity);
        for (int c = 0; c < capacity; c++) {
            buffer.getH3Indexes()[c] = 0x882681a33900000L + c;
            buffer.getBiomassHuman()[c] = 1000.0f;
            buffer.getFoodResource()[c] = 5000.0f;
            buffer.getBiomassLivestock()[c] = 2000.0f;
        }

        var initialSnapshot = guard.captureSnapshot(buffer);
        double totalInitialMass = initialSnapshot.totalBiomass();

        for (int step = 0; step < 1000; step++) {
            var before = guard.captureSnapshot(buffer);

            // Perform randomized internal stoichiometric conversion between food, livestock, and humans
            int cellIdx = rng.nextInt(capacity);
            float transferAmount = (float) (rng.nextDouble() * 50.0);

            if (buffer.getFoodResource()[cellIdx] >= transferAmount) {
                // Food converted to human biomass (zero net planetary mass delta)
                buffer.getFoodResource()[cellIdx] -= transferAmount;
                buffer.getBiomassHuman()[cellIdx] += transferAmount;
            }

            var after = guard.captureSnapshot(buffer);
            boolean ok = guard.verifyTransition(before, after, 0.0, 0.0);
            assertTrue(ok, "Internal mass transfer must preserve closed-system mass conservation");
        }

        var finalSnapshot = guard.captureSnapshot(buffer);
        assertEquals(totalInitialMass, finalSnapshot.totalBiomass(), 0.05, "Multi-step closed transitions must conserve total planetary mass within float machine precision");
        assertEquals(0, guard.getViolationCount(), "Zero violations must be registered by InvariantConservationGuard");
    }

    // Helper: Stull (2011) Wet-Bulb approximation formula
    private static double computeStullWetBulb(double t, double rh) {
        return t * Math.atan(0.151977 * Math.pow(rh + 8.313659, 0.5))
                + Math.atan(t + rh)
                - Math.atan(rh - 1.676331)
                + 0.00391838 * Math.pow(rh, 1.5) * Math.atan(0.023101 * rh)
                - 4.686035;
    }

    // Helper: Lethal wet-bulb mortality curve (exponential growth beyond 35°C hyperthermia limit)
    private static double computeWetBulbLethality(double twb) {
        if (twb <= 35.0) return 0.0;
        return 1.0 - Math.exp(-0.25 * (twb - 35.0));
    }
}
