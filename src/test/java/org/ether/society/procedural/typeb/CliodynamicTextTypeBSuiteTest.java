/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEngineRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite validating the 5 new Cliodynamic engines extracted from the text:
 * Spatial City Fractals, Self-Domestication, Monastic Buffer, Military Tech Shock, and Asymmetric Colonial Trade.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class CliodynamicTextTypeBSuiteTest {

    private List<H3Cell> testCells;

    @BeforeEach
    /*
     * Set up operation.
     * <p>
     * Executes operational logic for {@code CliodynamicTextTypeBSuiteTest} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void setUp() {
        ProceduralEngineRegistry.clearPlugins();
        testCells = new ArrayList<>();

        H3Cell cell1 = new H3Cell(613503380827930701L, 45.0, 10.0);
        cell1.setPopulation(50000);
        cell1.setTechnologyLevel(3.5);
        cell1.setFoodResource(2000.0);
        cell1.setResourceWork(100.0);
        cell1.setResourceMetal(50.0);
        cell1.setResourceCapital(500.0);

        H3Cell cell2 = new H3Cell(613503380827930702L, 46.0, 11.0);
        cell2.setPopulation(10000);
        cell2.setTechnologyLevel(3.5);

        testCells.add(cell1);
        testCells.add(cell2);
    }

    @Test
    /*
     * Test spatial city fractal engine operation.
     * <p>
     * Executes operational logic for {@code CliodynamicTextTypeBSuiteTest} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testSpatialCityFractalEngine() {
        SpatialCityFractalEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getPopulation() > testCells.get(1).getPopulation(), "Primary city should remain larger than secondary city under Zipf-Mori spatial power law.");
    }

    @Test
    /*
     * Test self domestication engine operation.
     * <p>
     * Executes operational logic for {@code CliodynamicTextTypeBSuiteTest} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testSelfDomesticationEngine() {
        double initialTech = testCells.get(0).getTechnologyLevel();
        SelfDomesticationEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getTechnologyLevel() > initialTech, "High social density self-domestication should boost technology learning.");
    }

    @Test
    /*
     * Test monastic demographic buffer engine operation.
     * <p>
     * Executes operational logic for {@code CliodynamicTextTypeBSuiteTest} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testMonasticDemographicBufferEngine() {
        testCells.get(0).setFoodResource(100.0); // Create food scarcity
        int initialPop = testCells.get(0).getPopulation();
        MonasticDemographicBufferEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getPopulation() < initialPop, "Monastic demographic buffer should absorb population overpressure during food scarcity.");
    }

    @Test
    /*
     * Test military tech shock engine operation.
     * <p>
     * Executes operational logic for {@code CliodynamicTextTypeBSuiteTest} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testMilitaryTechShockEngine() {
        double initialWork = testCells.get(0).getResourceWork();
        MilitaryTechShockEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getResourceWork() > initialWork, "Military tech shock (arquebus) should boost military work output.");
    }

    @Test
    /*
     * Test asymmetric colonial trade engine operation.
     * <p>
     * Executes operational logic for {@code CliodynamicTextTypeBSuiteTest} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testAsymmetricColonialTradeEngine() {
        double initialMetal = testCells.get(0).getResourceMetal();
        AsymmetricColonialTradeEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getResourceMetal() < initialMetal, "Asymmetric colonial trade (silver for spices) should drain bullion stock.");
    }

    @Test
    /*
     * Test cliodynamic text plugins cumulative execution operation.
     * <p>
     * Executes operational logic for {@code CliodynamicTextTypeBSuiteTest} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testCliodynamicTextPluginsCumulativeExecution() {
        ProceduralEngineRegistry.registerPlugin("B21_SpatialFractal", SpatialCityFractalEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B22_SelfDomestication", SelfDomesticationEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B23_MonasticBuffer", MonasticDemographicBufferEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B24_MilitaryTechShock", MilitaryTechShockEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B25_AsymmetricTrade", AsymmetricColonialTradeEngine::processHybrid);

        assertEquals(5, ProceduralEngineRegistry.getPluginCount(), "Registry should hold 5 new Cliodynamic plugins.");

        assertDoesNotThrow(() -> {
            ProceduralEngineRegistry.processPlugins(testCells, 1.0);
        }, "Cumulative execution of all 5 Cliodynamic text plugins should complete without errors.");
    }
}


