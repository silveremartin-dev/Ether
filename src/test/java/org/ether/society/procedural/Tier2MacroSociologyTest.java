/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.procedural.tier2.AcemogluRobinsonInstitutionsEngine;
import org.ether.society.procedural.tier2.GranovetterThresholdCascadeEngine;
import org.ether.society.procedural.tier2.TurchinGoldstoneSDTEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying Tier 2 macro-sociological and institutional plugins:
 * 1. Turchin-Goldstone Structural-Demographic Theory (SDT / PSI) Engine
 * 2. Granovetter Revolutionary Threshold Cascade Engine
 * 3. Acemoglu-Robinson Inclusive vs Extractive Institutions Engine
 *
 * @author Silvere Martin-Michiellot
 * @version 4.5.0
 */
public class Tier2MacroSociologyTest {

    @Test
    @DisplayName("Turchin-Goldstone SDT: Political Stress Index and state breakdown")
    public void testTurchinGoldstoneSDT() {
        TurchinGoldstoneSDTEngine engine = new TurchinGoldstoneSDTEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("PSI"));

        // High inequality (Gini 0.65) + acute food scarcity (food 100) -> High PSI breakdown
        List<H3Cell> cells = new ArrayList<>();
        H3Cell preRevolutionaryEmpire = new H3Cell();
        preRevolutionaryEmpire.setPopulation(10_000);
        preRevolutionaryEmpire.setGiniIndex(0.65);
        preRevolutionaryEmpire.setFoodResource(100.0);
        preRevolutionaryEmpire.setResourceCapital(1000.0);
        preRevolutionaryEmpire.setMovementFriction(1.0);
        cells.add(preRevolutionaryEmpire);

        double capitalBefore = preRevolutionaryEmpire.getResourceCapital();
        engine.process(cells, 1.0);
        double capitalAfter = preRevolutionaryEmpire.getResourceCapital();

        assertTrue(capitalAfter < capitalBefore, "High PSI must cause capital destruction and unrest");
    }

    @Test
    @DisplayName("Granovetter Threshold Cascade: Non-linear collective action tipping points")
    public void testGranovetterThresholdCascade() {
        GranovetterThresholdCascadeEngine engine = new GranovetterThresholdCascadeEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Granovetter"));

        List<H3Cell> cells = new ArrayList<>();
        H3Cell aggrievedCell = new H3Cell();
        aggrievedCell.setPopulation(5000);
        aggrievedCell.setGiniIndex(0.60);
        aggrievedCell.setFoodResource(150.0);
        aggrievedCell.setResourceWork(200.0);
        cells.add(aggrievedCell);

        double workBefore = aggrievedCell.getResourceWork();
        engine.process(cells, 1.0);
        double workAfter = aggrievedCell.getResourceWork();

        assertTrue(workAfter < workBefore, "High grievances must cause strike/civil defiance labor reduction");
    }

    @Test
    @DisplayName("Acemoglu-Robinson Institutions: Inclusive institutions promoting growth")
    public void testAcemogluRobinsonInstitutions() {
        AcemogluRobinsonInstitutionsEngine engine = new AcemogluRobinsonInstitutionsEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Acemoglu"));

        List<H3Cell> cells = new ArrayList<>();
        H3Cell inclusivePolity = new H3Cell();
        inclusivePolity.setPopulation(2000);
        inclusivePolity.setGiniIndex(0.25); // Inclusive, low inequality
        inclusivePolity.setTechnologyLevel(4.0);
        inclusivePolity.setResourceCapital(500.0);
        cells.add(inclusivePolity);

        double capitalBefore = inclusivePolity.getResourceCapital();
        engine.process(cells, 1.0);
        double capitalAfter = inclusivePolity.getResourceCapital();

        assertTrue(capitalAfter > capitalBefore, "Inclusive institutions must promote capital accumulation");
    }
}
