/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

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
 * @version 1.0.0-beta.1
 */
public class AdvancedSociologyAndLawsTest {

    private List<H3Cell> testCells;

    @BeforeEach
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
    public void testPhysicalLawEngine() {
        PhysicalLawEngine.setPhotosyntheticEfficiencyMultiplier(2.0);
        double initialBiomass = testCells.get(0).getBiomassNatural();

        PhysicalLawEngine.applyPhysicalLaws(testCells, 1.0);
        assertTrue(testCells.get(0).getBiomassNatural() > initialBiomass, "Doubled photosynthetic yield should increase natural biomass.");
    }

    @Test
    public void testCulturalSociologyEngine() {
        CulturalSociologyEngine.setGlobalEnvironmentalStewardship(0.9);
        double initialSoc = testCells.get(0).getSoilOrganicCarbon();

        CulturalSociologyEngine.processCulturalSociology(testCells, 1.0);
        assertTrue(testCells.get(0).getSoilOrganicCarbon() > initialSoc, "High environmental stewardship should increase soil organic carbon.");
    }

    @Test
    public void testCoGovernanceTradeEngine() {
        CoGovernanceTradeEngine.setGlobalCarbonQuotaTreatyActive(true);
        double initialPollution = testCells.get(0).getPollutionLevel();

        CoGovernanceTradeEngine.processTradeAndGovernance(testCells, 1.0);
        assertTrue(testCells.get(0).getPollutionLevel() < initialPollution, "Carbon quota treaty should reduce high pollution levels.");
    }

    @Test
    public void testCliodynamicChronicleEngine() {
        ScenarioTimeline timeline = new ScenarioTimeline();
        timeline.addEntry(1080, "SONG", "Époque Song", "Pré-industrialisation et charbon de bois", false);

        var prose = CliodynamicChronicleEngine.generateChronicles(timeline);
        assertNotNull(prose);
        assertFalse(prose.isEmpty());
        assertTrue(prose.get(0).contains("1080"));
    }
}

