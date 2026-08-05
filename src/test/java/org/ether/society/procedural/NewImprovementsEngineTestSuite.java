/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NewImprovementsEngineTestSuite {

    private List<H3Cell> testCells;

    @BeforeEach
    public void setUp() {
        testCells = new ArrayList<>();
        H3Cell cell1 = new H3Cell(0x8828308281fffffL, 48.8566, 2.3522);
        cell1.setElevation(5.0);
        cell1.setRainfall(1200.0);
        cell1.setBiome(Biome.BEACH);
        cell1.setPopulation(5000);
        cell1.setFoodResource(400.0);
        cell1.setBiomassNatural(100.0); // Low veg -> high erosion
        cell1.setTechnologyLevel(6.0);
        cell1.setResourceCapital(100.0);
        cell1.setResourceMetal(500.0);
        cell1.setWoodResource(200.0);
        cell1.setEnergySolar(100.0);
        cell1.setTemperature(20.0);

        H3Cell cell2 = new H3Cell(0x8828308287fffffL, 45.7640, 4.8357);
        cell2.setElevation(150.0);
        cell2.setRainfall(800.0);
        cell2.setBiome(Biome.PLAINS);
        cell2.setPopulation(2000);
        cell2.setFoodResource(300.0);
        cell2.setBiomassNatural(800.0);
        cell2.setTechnologyLevel(5.5);
        cell2.setResourceCapital(50.0);
        cell2.setResourceMetal(300.0);
        cell2.setWoodResource(100.0);
        cell2.setEnergyWind(50.0);
        cell2.setTemperature(18.0);

        testCells.add(cell1);
        testCells.add(cell2);
    }

    @Test
    public void testBifurcationChaosEngine() {
        assertDoesNotThrow(() -> BifurcationChaosEngine.processBifurcationAnalysis(testCells, 1.0));
        assertTrue(BifurcationChaosEngine.getGlobalSystemVariance() >= 0.0);
    }

    @Test
    public void testDynamicHydrographicSiltationEngine() {
        double initialElevation = testCells.get(0).getElevation();
        DynamicHydrographicSiltationEngine.processHydrographicSiltation(testCells, 1.0);
        assertTrue(testCells.get(0).getElevation() >= initialElevation, "Estuarine siltation should increase ground elevation");
    }

    @Test
    public void testPhysicalLeontiefInputOutputEngine() {
        double initialCapital = testCells.get(0).getResourceCapital();
        double initialMetal = testCells.get(0).getResourceMetal();
        PhysicalLeontiefInputOutputEngine.processLeontiefInputOutput(testCells, 1.0);
        assertTrue(testCells.get(0).getResourceCapital() > initialCapital, "Capital should expand under Leontief production");
        assertTrue(testCells.get(0).getResourceMetal() < initialMetal, "Metal inputs should be consumed");
    }

    @Test
    public void testGeoengineeringAlbedoFeedbackEngine() {
        GeoengineeringAlbedoFeedbackEngine.processGeoengineeringAlbedo(testCells, 1.0);
        assertTrue(GeoengineeringAlbedoFeedbackEngine.getGlobalCoolingEffectCelsius() >= 0.0);
    }
}
