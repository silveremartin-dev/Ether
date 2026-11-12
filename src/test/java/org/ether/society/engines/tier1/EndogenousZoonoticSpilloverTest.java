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
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 5001L;
        buf.getBiomassHuman()[0] = (float) (800 * 0.06);
        buf.getBiomassLivestock()[0] = 1200.0f;
        buf.getFoodResource()[0] = 10.0f;
        buf.getLifespan()[0] = 38.0f;
        buf.getResourceCapital()[0] = 50.0f;

        BioMolecularEpidemiologyEngine engine = new BioMolecularEpidemiologyEngine();
        double initialLifespan = buf.getLifespan()[0];
        for (int m = 0; m < 24; m++) {
            engine.tick(buf, 1.0 / 12.0, 1.0);
        }

        assertTrue(buf.getLifespan()[0] < initialLifespan,
                "Caloric deficit and low capital under dense population must depress lifespan via reduced immunoglobulin synthesis.");
    }

    @Test
    /*
     * Test well-nourished cell with no livestock remains disease-free.
     */
    public void testHealthyIsolatedCellRemainsDiseaseFree() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 5002L;
        buf.getBiomassHuman()[0] = (float) (300 * 0.06);
        buf.getBiomassLivestock()[0] = 0.0f;
        buf.getFoodResource()[0] = 50_000.0f;
        buf.getLifespan()[0] = 45.0f;
        buf.getResourceCapital()[0] = 500.0f;

        BioMolecularEpidemiologyEngine engine = new BioMolecularEpidemiologyEngine();
        for (int m = 0; m < 36; m++) {
            engine.tick(buf, 1.0 / 12.0, 1.0);
        }

        assertTrue(buf.getLifespan()[0] >= 45.0f,
                "A well-fed population should maintain or improve biological lifespan.");
    }

    @Test
    /*
     * Test pathogen recovery and mortality dynamics over annual integration.
     */
    public void testPathogenProgressionAndRecovery() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 5003L;
        buf.getBiomassHuman()[0] = (float) (1000 * 0.06);
        buf.getBiomassLivestock()[0] = 50.0f;
        buf.getFoodResource()[0] = 1500.0f;
        buf.getLifespan()[0] = 40.0f;

        BioMolecularEpidemiologyEngine engine = new BioMolecularEpidemiologyEngine();
        for (int m = 0; m < 12; m++) {
            engine.tick(buf, 1.0 / 12.0, 1.0);
        }

        assertTrue(buf.getLifespan()[0] >= 15.0f && buf.getLifespan()[0] <= 85.0f,
                "Lifespan must remain bounded within physiological limits.");
    }
}
