/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

import org.ether.society.database.H3Cell;
import org.ether.society.procedural.ProceduralEngineRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite validating Henrich Tasmanian Loss, Braudel Mediterranean Sea Highway,
 * Hamilton Kin Selection, Turchin Frontier Asabiyyah, and Buss Sexual Selection Mating models.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class AdvancedCliodynamicTypeBSuiteTest {

    private List<H3Cell> testCells;

    @BeforeEach
    public void setUp() {
        ProceduralEngineRegistry.clearPlugins();
        testCells = new ArrayList<>();

        H3Cell cell = new H3Cell(613503380827930701L, 35.0, 15.0); // Mediterranean latitude 35°N
        cell.setPopulation(3000); // Below 5000 Tasmanian threshold
        cell.setTechnologyLevel(3.0);
        cell.setFoodResource(10.0); // Scarcity
        cell.setResourceCapital(1200.0); // High capital center
        cell.setResourceWork(50.0);

        testCells.add(cell);
    }

    @Test
    public void testTasmanianCulturalRegressionEngine() {
        double initialTech = testCells.get(0).getTechnologyLevel();
        TasmanianCulturalRegressionEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getTechnologyLevel() < initialTech, "Henrich Tasmanian loss: small isolated population (N=3000) should lose technology level.");
    }

    @Test
    public void testMediterraneanSeaHighwayEngine() {
        double initialCapital = testCells.get(0).getResourceCapital();
        MediterraneanSeaHighwayEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getResourceCapital() > initialCapital, "Braudel Mediterranean highway: latitude 35°N should boost capital trade efficiency.");
    }

    @Test
    public void testKinSelectionHamiltonEngine() {
        assertTrue(KinSelectionHamiltonEngine.checkHamiltonRule(0.50, 10.0, 4.0), "Hamilton rule r * B > C should return true for r=0.5, B=10, C=4.");
        double initialWork = testCells.get(0).getResourceWork();
        KinSelectionHamiltonEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getResourceWork() > initialWork, "Hamilton kin selection: food scarcity should boost military work output toward outgroups.");
    }

    @Test
    public void testFrontierAsabiyyahEngine() {
        double initialWork = testCells.get(0).getResourceWork();
        FrontierAsabiyyahEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getResourceWork() < initialWork, "Turchin Asabiyyah: high capital hinterland (>1000) should suffer Asabiyyah luxury decay.");
    }

    @Test
    public void testSexualSelectionMatingEngine() {
        double initialWork = testCells.get(0).getResourceWork();
        SexualSelectionMatingEngine.processHybrid(testCells, 1.0);
        assertTrue(testCells.get(0).getResourceWork() > initialWork, "Buss sexual selection: high capital inequality should mobilize young male military force.");
    }

    @Test
    public void testAdvancedCliodynamicPluginsCumulativeExecution() {
        ProceduralEngineRegistry.registerPlugin("B26_TasmanianLoss", TasmanianCulturalRegressionEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B27_MediterraneanHighway", MediterraneanSeaHighwayEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B28_KinSelectionHamilton", KinSelectionHamiltonEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B29_FrontierAsabiyyah", FrontierAsabiyyahEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B30_SexualSelectionMating", SexualSelectionMatingEngine::processHybrid);

        assertEquals(5, ProceduralEngineRegistry.getPluginCount(), "Registry should hold 5 new advanced Cliodynamic plugins.");

        assertDoesNotThrow(() -> {
            ProceduralEngineRegistry.processPlugins(testCells, 1.0);
        }, "Cumulative execution of all 5 advanced Cliodynamic plugins should complete without errors.");
    }
}
