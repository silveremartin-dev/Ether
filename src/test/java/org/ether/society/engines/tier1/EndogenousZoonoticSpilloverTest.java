/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Endogenous Zoonotic Spillover & Malnutrition-Coupled Pathogens.
 * Verifies that livestock density + urban density under caloric deficit triggers emergent outbreaks,
 * while healthy, livestock-free populations remain disease-free.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class EndogenousZoonoticSpilloverTest {

    @Test
    /*
     * Test high livestock and malnutrition triggers endogenous spillover.
     */
    public void testHighLivestockAndMalnutritionTriggersSpillover() {
        List<H3Cell> cells = new ArrayList<>();

        H3Cell highRiskCell = new H3Cell();
        highRiskCell.setH3Index(5001L);
        highRiskCell.setPopulation(800);
        highRiskCell.setBiomassHuman(800.0);
        highRiskCell.setBiomassLivestock(1200.0); // Dense domestic livestock herd
        highRiskCell.setFoodResource(10.0);       // Caloric deficit / starvation
        highRiskCell.setLifespan(38.0);
        highRiskCell.setTechnologyLevel(1.0);
        cells.add(highRiskCell);

        // Run monthly ticks
        boolean outbreakTriggered = false;
        for (int m = 0; m < 24; m++) {
            BioMolecularEpidemiologyEngine.processBioMolecularImmunity(cells, 1.0 / 12.0);
            if (highRiskCell.getEpidemicInfected() != null && highRiskCell.getEpidemicInfected() > 0) {
                outbreakTriggered = true;
                break;
            }
        }

        assertTrue(outbreakTriggered, "High livestock density under malnutrition must trigger an emergent zoonotic spillover.");
        assertNotNull(highRiskCell.getActivePathogenName(), "Active pathogen name must be populated upon spillover.");
        assertTrue(highRiskCell.getEpidemicInfected() > 0, "Infected count must be greater than zero.");
    }

    @Test
    /*
     * Test well-nourished cell with no livestock remains disease-free.
     */
    public void testHealthyIsolatedCellRemainsDiseaseFree() {
        List<H3Cell> cells = new ArrayList<>();

        H3Cell safeCell = new H3Cell();
        safeCell.setH3Index(5002L);
        safeCell.setPopulation(300);
        safeCell.setBiomassHuman(300.0);
        safeCell.setBiomassLivestock(0.0);     // No livestock
        safeCell.setFoodResource(50_000.0);    // Abundant granary reserves
        safeCell.setLifespan(45.0);
        safeCell.setTechnologyLevel(2.0);
        cells.add(safeCell);

        for (int m = 0; m < 36; m++) {
            BioMolecularEpidemiologyEngine.processBioMolecularImmunity(cells, 1.0 / 12.0);
        }

        assertEquals(0, safeCell.getEpidemicInfected() != null ? safeCell.getEpidemicInfected() : 0,
                "A well-fed population with no livestock should not suffer endogenous zoonotic spillover.");
        assertNull(safeCell.getActivePathogenName(), "No pathogen should be active in safe population.");
    }

    @Test
    /*
     * Test pathogen recovery and mortality dynamics over annual integration.
     */
    public void testPathogenProgressionAndRecovery() {
        List<H3Cell> cells = new ArrayList<>();

        H3Cell infectedCell = new H3Cell();
        infectedCell.setH3Index(5003L);
        infectedCell.setPopulation(1000);
        infectedCell.setBiomassHuman(1000.0);
        infectedCell.setBiomassLivestock(50.0);
        infectedCell.setFoodResource(1500.0); // Moderate food
        infectedCell.setLifespan(40.0);
        infectedCell.setEpidemicInfected(200); // Already infected
        infectedCell.setActivePathogenName("Test Pathogen");
        cells.add(infectedCell);

        int initialPop = infectedCell.getPopulation();

        // Integrate for 1 year (12 monthly ticks)
        for (int m = 0; m < 12; m++) {
            BioMolecularEpidemiologyEngine.processBioMolecularImmunity(cells, 1.0 / 12.0);
        }

        assertTrue(infectedCell.getEpidemicRecovered() != null && infectedCell.getEpidemicRecovered() > 0,
                "Infected population must generate recovered individuals over time.");
        assertTrue(infectedCell.getPopulation() <= initialPop,
                "Virulence should cause non-zero mortality during the epidemic cycle.");
    }
}
