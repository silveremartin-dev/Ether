/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

import org.ether.society.database.H3Cell;
import org.ether.society.analytics.ScenarioBranchingTree;
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
 * @version 1.0.0-beta.3
 */
public class AdvancedPhysicalFeaturesTestSuite {

    private List<H3Cell> testCells;

    @BeforeEach
    /*
     * Set up operation.
     * <p>
     * Executes operational logic for {@code AdvancedPhysicalFeaturesTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
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
    /*
     * Test terraforming engine operation.
     * <p>
     * Executes operational logic for {@code AdvancedPhysicalFeaturesTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testTerraformingEngine() {
        TerraformingEngine.setCo2Ppm(800.0);
        TerraformingEngine.setSolarMirrorInsolationMultiplier(1.05);
        TerraformingEngine.setAsteroidMiningFluxTonnesPerYear(1000.0);

        TerraformingEngine.setTerraformingActive(true);
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getTemperature()[0] = testCells.get(0).getTemperature() != null ? testCells.get(0).getTemperature().floatValue() : 15.0f;
        buf.getBiomassHuman()[0] = (float) testCells.get(0).getPopulationOr(1000);
        buf.getMetalResource()[0] = (float) testCells.get(0).getResourceMetalOr(1.0);

        double initialTemp = buf.getTemperature()[0];
        new TerraformingEngine().tick(buf, 1.0, 1.0);

        assertTrue(buf.getTemperature()[0] > initialTemp, "Terraforming radiative forcing should increase temperature.");
        assertTrue(buf.getMetalResource()[0] > 1.0f, "Asteroid mining influx should increase mineral density.");
    }

    @Test
    /*
     * Test trophic ecosystem engine operation.
     * <p>
     * Executes operational logic for {@code AdvancedPhysicalFeaturesTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testTrophicEcosystemEngine() {
        TrophicEcosystemEngine.setPleistoceneRewildingActive(true);
        TrophicEcosystemEngine.setMegafaunaDensityPerKm2(2.0);

        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(testCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(testCells, buf, false);
        double initialSoc = buf.getSoilOrganicCarbon()[0];
        new TrophicEcosystemEngine().tick(buf, 1.0, 12.0);

        assertTrue(buf.getSoilOrganicCarbon()[0] > initialSoc, "Pleistocene rewilding megafauna should accumulate soil organic carbon.");
    }

    @Test
    /*
     * Test physical supply chain engine operation.
     */
    public void testPhysicalSupplyChainEngine() {
        PhysicalSupplyChainEngine.setMaritimeChokepointBlockadeActive(true);
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(testCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(testCells, buf, false);
        buf.getEnergyFire()[0] = 100.0f;
        double initialWork = buf.getEnergyFire()[0];

        new PhysicalSupplyChainEngine().tick(buf, 1.0, 12.0);

        assertTrue(buf.getEnergyFire()[0] < initialWork, "Chokepoint blockade logistics friction should lower work output.");
    }

    @Test
    /*
     * Test urban thermodynamics engine operation.
     */
    public void testUrbanThermodynamicsEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(testCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(testCells, buf, false);
        double initialTemp = buf.getTemperature()[0];

        new UrbanThermodynamicsEngine().tick(buf, 1.0, 12.0);

        assertTrue(buf.getTemperature()[0] > initialTemp, "Urban Heat Island effect should increase city temperature.");
    }

    @Test
    /*
     * Test scenario branching tree operation.
     * <p>
     * Executes operational logic for {@code AdvancedPhysicalFeaturesTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testScenarioBranchingTree() {
        ScenarioBranchingTree tree = new ScenarioBranchingTree();
        assertEquals(1, tree.getBranches().size());

        var newBranch = tree.createBranch("Branche Fusion 2040", 2040, testCells);
        assertEquals(2, tree.getBranches().size());
        assertEquals("Branche Fusion 2040", newBranch.getName());
    }
}

