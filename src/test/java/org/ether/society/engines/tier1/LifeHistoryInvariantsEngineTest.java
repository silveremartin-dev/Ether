/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Epistemic verification and validation test suite for {@link LifeHistoryInvariantsEngine}.
 * Verifies Charnov's dimensionless invariant, optimal maturity age shift, juvenile survivorship,
 * 500-cell crisis demographic adaptation, and bitwise determinism.
 */
public class LifeHistoryInvariantsEngineTest {

    @Test
    @DisplayName("4.1 Charnov Invariant & Optimal Reproductive Maturity Age")
    public void testCharnovInvariants() {
        // Under baseline adult mortality hazard (mu = 0.08 / yr)
        double alphaBaseline = LifeHistoryInvariantsEngine.calculateOptimalReproductiveAge(0.08);
        // alpha = 1.80 / 0.08 = 22.5 years
        assertEquals(22.5, alphaBaseline, 1e-4, "Baseline adult mortality (0.08) must result in maturity age ~22.5 years.");

        // Under high mortality paleolithic / epidemic conditions (mu = 0.15 / yr)
        double alphaCrisis = LifeHistoryInvariantsEngine.calculateOptimalReproductiveAge(0.15);
        // alpha = 1.80 / 0.15 = 12.0 years (accelerated maturation)
        assertEquals(12.0, alphaCrisis, 1e-4, "High adult mortality must accelerate reproductive maturity age to ~12.0 years.");

        // Dimensionless product M * alpha must equal Charnov invariant (within clamping limits)
        double muTest = 0.10;
        double alphaTest = LifeHistoryInvariantsEngine.calculateOptimalReproductiveAge(muTest);
        assertEquals(LifeHistoryInvariantsEngine.CHARNOV_INVARIANT_C, muTest * alphaTest, 1e-6,
                "Dimensionless product mu * alpha must equal Charnov's invariant C = 1.80.");
    }

    @Test
    @DisplayName("4.2 Juvenile Survivorship & Equilibrium Birth Rates")
    public void testSurvivorshipAndFertility() {
        // High mortality reduces survival to maturity
        double survLowMort = LifeHistoryInvariantsEngine.calculateJuvenileSurvivorship(0.08, 22.5);
        double survHighMort = LifeHistoryInvariantsEngine.calculateJuvenileSurvivorship(0.15, 12.0);

        assertTrue(survLowMort > 0.0 && survLowMort <= 1.0);
        assertTrue(survHighMort > 0.0 && survHighMort <= 1.0);

        // Required birth rate to maintain replacement increases under high mortality
        double birthRateLowMort = LifeHistoryInvariantsEngine.calculateEquilibriumBirthRate(0.08, survLowMort);
        double birthRateHighMort = LifeHistoryInvariantsEngine.calculateEquilibriumBirthRate(0.15, survHighMort);

        assertTrue(birthRateHighMort > birthRateLowMort,
                "Populations experiencing higher adult mortality must increase reproductive allocation/birth rates.");
    }

    @Test
    @DisplayName("4.3 500-Cell Demographic Adaptation & Crisis Resilience")
    public void test500CellDemographicAdaptation() {
        int cellCount = 500;
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(cellCount);

        for (int i = 0; i < cellCount; i++) {
            buf.getH3Indexes()[i] = 400000L + i;
            buf.getBiomassHuman()[i] = (float) (1000 * 0.06);
            buf.getTemperature()[i] = (i < 250) ? 18.0f : -25.0f;
        }

        LifeHistoryInvariantsEngine engine = new LifeHistoryInvariantsEngine();
        for (int yr = 0; yr < 50; yr++) {
            engine.tick(buf, 1.0, 12.0);
        }

        for (int i = 0; i < 250; i++) {
            double pop = buf.getBiomassHuman()[i] / 0.06;
            assertTrue(pop >= 900.0,
                    "Hospitable cells must maintain stable demographic persistence.");
        }

        for (int i = 250; i < 500; i++) {
            double pop = buf.getBiomassHuman()[i] / 0.06;
            assertTrue(pop < 1000.0,
                    "Populations subjected to severe thermal stress must contract.");
            assertTrue(pop > 0.0,
                    "Adaptive fertility response prevents instantaneous demographic extinction.");
        }
    }

    @Test
    @DisplayName("4.4 Bitwise Determinism Verification")
    public void testBitwiseDeterminism() {
        org.ether.society.core.dod.WorldBuffer run1 = createPopulationBuffer(100);
        org.ether.society.core.dod.WorldBuffer run2 = createPopulationBuffer(100);

        LifeHistoryInvariantsEngine engine = new LifeHistoryInvariantsEngine();
        for (int yr = 0; yr < 10; yr++) {
            engine.tick(run1, 1.0, 12.0);
            engine.tick(run2, 1.0, 12.0);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(run1.getBiomassHuman()[i], run2.getBiomassHuman()[i],
                    "Life history demographic trajectories must be bitwise identical.");
        }
    }

    private org.ether.society.core.dod.WorldBuffer createPopulationBuffer(int count) {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(count);
        for (int i = 0; i < count; i++) {
            buf.getH3Indexes()[i] = 80000L + i;
            buf.getBiomassHuman()[i] = (float) (1500 * 0.06);
            buf.getTemperature()[i] = (i % 2 == 0) ? 15.0f : -15.0f;
        }
        return buf;
    }
}
