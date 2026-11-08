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
        List<H3Cell> cells = new ArrayList<>(cellCount);

        for (int i = 0; i < cellCount; i++) {
            H3Cell cell = new H3Cell();
            cell.setH3Index((long) (400000 + i));
            cell.setPopulation(1000);

            if (i < 250) {
                // Temperate hospitable environment (18°C)
                cell.setTemperature(18.0);
            } else {
                // Severe thermal stress environment (-25°C periglacial or +45°C hyperthermic)
                cell.setTemperature(-25.0);
            }
            cells.add(cell);
        }

        // Simulate 50 years of demographic and life-history adaptation
        for (int yr = 0; yr < 50; yr++) {
            LifeHistoryInvariantsEngine.processLifeHistoryDynamics(cells, 1.0);
        }

        // Hospitable cells should maintain viable populations
        for (int i = 0; i < 250; i++) {
            assertTrue(cells.get(i).getPopulation() >= 900,
                    "Hospitable cells must maintain stable demographic persistence.");
        }

        // Stressed cells should show demographic contraction due to extreme thermal mortality hazard
        for (int i = 250; i < 500; i++) {
            assertTrue(cells.get(i).getPopulation() < 1000,
                    "Populations subjected to severe thermal stress must contract.");
            assertTrue(cells.get(i).getPopulation() > 0,
                    "Adaptive fertility response prevents instantaneous demographic extinction.");
        }
    }

    @Test
    @DisplayName("4.4 Bitwise Determinism Verification")
    public void testBitwiseDeterminism() {
        List<H3Cell> run1 = createPopulationDomain(100);
        List<H3Cell> run2 = createPopulationDomain(100);

        for (int yr = 0; yr < 10; yr++) {
            LifeHistoryInvariantsEngine.processLifeHistoryDynamics(run1, 1.0);
            LifeHistoryInvariantsEngine.processLifeHistoryDynamics(run2, 1.0);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(run1.get(i).getPopulation(), run2.get(i).getPopulation(),
                    "Life history demographic trajectories must be bitwise identical.");
        }
    }

    private List<H3Cell> createPopulationDomain(int count) {
        List<H3Cell> cells = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            H3Cell cell = new H3Cell();
            cell.setH3Index((long) (80000 + i));
            cell.setPopulation(1500);
            cell.setTemperature(i % 2 == 0 ? 15.0 : -15.0);
            cells.add(cell);
        }
        return cells;
    }
}
