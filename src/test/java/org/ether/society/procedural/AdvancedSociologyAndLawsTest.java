/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

import org.ether.society.database.H3Cell;
import org.ether.society.model.ScenarioTimeline;
import org.ether.society.util.CliodynamicChronicleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite for Physical Laws, Cultural Sociology, Co-Governance, and Chronicles.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class AdvancedSociologyAndLawsTest {

    private List<H3Cell> testCells;

    @BeforeEach
    /*
     * Set up operation.
     * <p>
     * Executes operational logic for {@code AdvancedSociologyAndLawsTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void setUp() {
        testCells = new ArrayList<>();
        H3Cell cell = new H3Cell(613503380827930701L, 45.0, 10.0);
        cell.setPopulation(5000);
        cell.setBiomassNatural(100.0);
        cell.setSoilOrganicCarbon(40.0);
        cell.setPollutionLevel(300.0);
        testCells.add(cell);
    }

    @Test
    /*
     * Test physical law engine operation.
     * <p>
     * Executes operational logic for {@code AdvancedSociologyAndLawsTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testPhysicalLawEngine() {
        PhysicalLawEngine.setPhotosyntheticEfficiencyMultiplier(2.0);
        double initialBiomass = testCells.get(0).getBiomassNatural();

        PhysicalLawEngine.applyPhysicalLaws(testCells, 1.0);
        assertTrue(testCells.get(0).getBiomassNatural() > initialBiomass, "Doubled photosynthetic yield should increase natural biomass.");
    }

    @Test
    /*
     * Test cultural sociology engine operation.
     * <p>
     * Executes operational logic for {@code AdvancedSociologyAndLawsTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testCulturalSociologyEngine() {
        CulturalSociologyEngine.setGlobalEnvironmentalStewardship(0.9);
        double initialSoc = testCells.get(0).getSoilOrganicCarbon();

        CulturalSociologyEngine.processCulturalSociology(testCells, 1.0);
        assertTrue(testCells.get(0).getSoilOrganicCarbon() > initialSoc, "High environmental stewardship should increase soil organic carbon.");
    }

    @Test
    /*
     * Test co governance trade engine operation.
     * <p>
     * Executes operational logic for {@code AdvancedSociologyAndLawsTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testCoGovernanceTradeEngine() {
        CoGovernanceTradeEngine.setGlobalCarbonQuotaTreatyActive(true);
        double initialPollution = testCells.get(0).getPollutionLevel();

        CoGovernanceTradeEngine.processTradeAndGovernance(testCells, 1.0);
        assertTrue(testCells.get(0).getPollutionLevel() < initialPollution, "Carbon quota treaty should reduce high pollution levels.");
    }

    @Test
    /*
     * Test cliodynamic chronicle engine operation.
     * <p>
     * Executes operational logic for {@code AdvancedSociologyAndLawsTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testCliodynamicChronicleEngine() {
        ScenarioTimeline timeline = new ScenarioTimeline();
        timeline.addEntry(1080, "SONG", "Époque Song", "Pré-industrialisation et charbon de bois", false);

        var prose = CliodynamicChronicleEngine.generateChronicles(timeline);
        assertNotNull(prose);
        assertFalse(prose.isEmpty());
        assertTrue(prose.get(0).contains("1080"));
    }
}

