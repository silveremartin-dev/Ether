/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying Tier 2 macro-sociological and institutional plugins:
 * 1. Turchin-Goldstone Structural-Demographic Theory (SDT / PSI) Engine
 * 2. Granovetter Revolutionary Threshold Cascade Engine
 * 3. Acemoglu-Robinson Inclusive vs Extractive Institutions Engine
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class Tier2MacroSociologyTest {

    @Test
    @DisplayName("Turchin-Goldstone SDT: Political Stress Index and state breakdown")
    public void testTurchinGoldstoneSDT() {
        TurchinGoldstoneSDTEngine engine = new TurchinGoldstoneSDTEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("PSI"));

        // High inequality (Gini 0.65) + acute food scarcity (food 100) -> High PSI breakdown
        WorldBuffer buffer = new WorldBuffer(1);
        buffer.getHumanPopulation()[0] = 10_000.0f;
        buffer.getGiniIndex()[0] = 0.65f;
        buffer.getFoodResource()[0] = 100.0f;
        buffer.getResourceCapital()[0] = 1000.0f;
        buffer.getInstitutionalComplexity()[0] = 1.0f;

        float capitalBefore = buffer.getResourceCapital()[0];
        engine.tick(buffer, 1.0, 12.0);
        float capitalAfter = buffer.getResourceCapital()[0];

        assertTrue(capitalAfter < capitalBefore, "High PSI must cause capital destruction and unrest");
    }

    @Test
    @DisplayName("Granovetter Threshold Cascade: Non-linear collective action tipping points")
    public void testGranovetterThresholdCascade() {
        GranovetterThresholdCascadeEngine engine = new GranovetterThresholdCascadeEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Granovetter"));

        WorldBuffer buffer = new WorldBuffer(1);
        buffer.getHumanPopulation()[0] = 5000.0f;
        buffer.getGiniIndex()[0] = 0.60f;
        buffer.getFoodResource()[0] = 150.0f;
        buffer.getResourceCapital()[0] = 2000.0f;
        buffer.getInstitutionalComplexity()[0] = 0.5f;

        float capBefore = buffer.getResourceCapital()[0];
        float instBefore = buffer.getInstitutionalComplexity()[0];
        engine.tick(buffer, 1.0, 12.0);

        assertTrue(buffer.getResourceCapital()[0] < capBefore, "High grievances must cause civil defiance capital disruption");
        assertTrue(buffer.getInstitutionalComplexity()[0] < instBefore, "High grievances must erode institutional complexity");
    }

    @Test
    @DisplayName("Acemoglu-Robinson Institutions: Inclusive institutions promoting growth")
    public void testAcemogluRobinsonInstitutions() {
        AcemogluRobinsonInstitutionsEngine engine = new AcemogluRobinsonInstitutionsEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Acemoglu"));

        WorldBuffer buffer = new WorldBuffer(1);
        buffer.getHumanPopulation()[0] = 2000.0f;
        buffer.getGiniIndex()[0] = 0.25f; // Inclusive, low inequality
        buffer.getResourceCapital()[0] = 300_000.0f; // k_pc = 150 kg/cap > 100 property-rights activation threshold

        float capitalBefore = buffer.getResourceCapital()[0];
        engine.tick(buffer, 1.0, 12.0);
        float capitalAfter = buffer.getResourceCapital()[0];

        assertTrue(capitalAfter > capitalBefore, "Inclusive institutions must promote capital accumulation");
    }
}


