/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.ScenarioBranchingTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite validating Terraforming, Trophic Ecosystems, Supply Chains,
 * Urban Thermodynamics, and Scenario Multiverse Branching.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class AdvancedPhysicalFeaturesTestSuite {

    private List<H3Cell> testCells;

    @BeforeEach
    public void setUp() {
        testCells = new ArrayList<>();
        H3Cell cell1 = new H3Cell(613503380827930701L, 65.0, 10.0);
        cell1.setPopulation(60000);
        cell1.setTemperature(10.0);
        cell1.setBiomassNatural(100.0);
        cell1.setSoilOrganicCarbon(50.0);
        cell1.setDynamicAlbedo(0.20);
        cell1.setResourceMetal(1.0);
        cell1.setResourceWork(10.0);
        cell1.setAccessibleAquifer(100.0);

        testCells.add(cell1);
    }

    @Test
    public void testTerraformingEngine() {
        TerraformingEngine.setCo2Ppm(800.0);
        TerraformingEngine.setSolarMirrorInsolationMultiplier(1.05);
        TerraformingEngine.setAsteroidMiningFluxTonnesPerYear(1000.0);

        double initialTemp = testCells.get(0).getTemperature();
        TerraformingEngine.processTerraforming(testCells, 1.0);

        assertTrue(testCells.get(0).getTemperature() > initialTemp, "Terraforming radiative forcing should increase temperature.");
        assertTrue(testCells.get(0).getResourceMetal() > 1.0, "Asteroid mining influx should increase mineral density.");
    }

    @Test
    public void testTrophicEcosystemEngine() {
        TrophicEcosystemEngine.setPleistoceneRewildingActive(true);
        TrophicEcosystemEngine.setMegafaunaDensityPerKm2(2.0);

        double initialSoc = testCells.get(0).getSoilOrganicCarbon();
        TrophicEcosystemEngine.processTrophicEcosystem(testCells, 1.0);

        assertTrue(testCells.get(0).getSoilOrganicCarbon() > initialSoc, "Pleistocene rewilding megafauna should accumulate soil organic carbon.");
    }

    @Test
    public void testPhysicalSupplyChainEngine() {
        PhysicalSupplyChainEngine.setMaritimeChokepointBlockadeActive(true);
        double initialWork = testCells.get(0).getResourceWork();

        PhysicalSupplyChainEngine.processSupplyChains(testCells, 1.0);

        assertTrue(testCells.get(0).getResourceWork() < initialWork, "Chokepoint blockade logistics friction should lower work output.");
    }

    @Test
    public void testUrbanThermodynamicsEngine() {
        double initialTemp = testCells.get(0).getTemperature();

        UrbanThermodynamicsEngine.processUrbanThermodynamics(testCells, 1.0);

        assertTrue(testCells.get(0).getTemperature() > initialTemp, "Urban Heat Island effect should increase city temperature.");
    }

    @Test
    public void testScenarioBranchingTree() {
        ScenarioBranchingTree tree = new ScenarioBranchingTree();
        assertEquals(1, tree.getBranches().size());

        var newBranch = tree.createBranch("Branche Fusion 2040", 2040, testCells);
        assertEquals(2, tree.getBranches().size());
        assertEquals("Branche Fusion 2040", newBranch.getName());
    }
}

