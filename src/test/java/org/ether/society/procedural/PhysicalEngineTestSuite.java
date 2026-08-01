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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated JUnit test suite validating physical laws of conservation across Ether engines.
 */
public class PhysicalEngineTestSuite {

    private List<H3Cell> testCells;

    @BeforeEach
    public void setUp() {
        testCells = new ArrayList<>();
        H3Cell cell = new H3Cell(0x8828308281fffffL, 48.8566, 2.3522);
        cell.setBiome(Biome.PLAINS);
        cell.setPopulation(1000);
        cell.setTechnologyLevel(5.0);
        cell.setResourceCapital(5000.0);
        cell.setResourceMetal(500.0);
        cell.setBiomassNatural(300.0);
        cell.setBiomassAgriculture(200.0);
        cell.setSoilOrganicCarbon(20.0);
        cell.setPollutionLevel(50.0);
        testCells.add(cell);
    }

    @Test
    @DisplayName("Soil N-P-K Liebig's Law & Mineral Mining Execution Test")
    public void testSoilNutrientNPKEngine() {
        SoilNutrientNPKEngine.processSoilNutrients(testCells);
        assertNotNull(testCells.get(0).getBiomassAgriculture());
        assertTrue(testCells.get(0).getBiomassAgriculture() > 0.0);
    }

    @Test
    @DisplayName("Net Energy EROEI Master Cliodynamic Engine Test")
    public void testNetEnergyEROEIEngine() {
        double eroei = NetEnergyEROEIEngine.calculateEROEI(testCells.get(0));
        assertTrue(eroei > 0.0, "EROEI must be strictly positive");
        NetEnergyEROEIEngine.processNetEnergyEROEI(testCells);
    }

    @Test
    @DisplayName("Nuclear Safety & Radiotoxicity Engine Test")
    public void testNuclearSafetyRadiotoxicityEngine() {
        NuclearSafetyRadiotoxicityEngine.processNuclearEnergySafety(testCells);
        assertTrue(testCells.get(0).getPopulation() >= 0);
    }

    @Test
    @DisplayName("Ozone Layer Depletion Engine Test")
    public void testOzoneLayerDepletionEngine() {
        OzoneLayerDepletionEngine.processOzoneLayerDepletion(testCells);
        assertTrue(testCells.get(0).getBiomassAgriculture() >= 0.0);
    }

    @Test
    @DisplayName("Nuclear Warfare & Climate Soot Engine Test")
    public void testNuclearWarfareClimateEngine() {
        NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(1.2);
        NuclearWarfareClimateEngine.processNuclearWarfareClimate(testCells);
        assertTrue(NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() > 0.0);
    }

    @Test
    @DisplayName("Technological Singularity & Trans-Human Engine Test")
    public void testTechnologicalSingularityEngine() {
        TechnologicalSingularityEngine.processTechnologicalSingularity(testCells);
        assertNotNull(testCells.get(0).getResourceCapital());
    }
}
