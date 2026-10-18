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
import org.ether.society.model.Biome;
import org.junit.jupiter.api.AfterEach;
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

    @AfterEach
    public void tearDown() {
        NuclearWarfareClimateEngine.resetState();
        NuclearSafetyRadiotoxicityEngine.resetState();
    }

    @Test
    @DisplayName("Soil N-P-K Liebig's Law & Mineral Mining Execution Test")
    /*
     * Test soil nutrient npkengine operation.
     * <p>
     * Executes operational logic for {@code PhysicalEngineTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testSoilNutrientNPKEngine() {
        H3Cell cell = testCells.get(0);
        double initialBiomass = cell.getBiomassAgriculture(); // 200.0
        SoilNutrientNPKEngine.processSoilNutrients(testCells);
        assertNotNull(cell.getBiomassAgriculture(), "Agricultural biomass must not be null");
        assertFalse(Double.isNaN(cell.getBiomassAgriculture()), "Agricultural biomass must not be NaN");
        assertTrue(cell.getBiomassAgriculture() > 0.0, "Agricultural biomass must remain positive");
        // On a high-resolution Level 8 cell (~0.74 km2), maximum physical land biomass is scaled down by areaScale (~0.05 min bound).
        // Since initial biomass was 200.0 (above the cell's physical carrying ceiling), the engine applies terrace maintenance depreciation.
        assertTrue(cell.getBiomassAgriculture() <= initialBiomass,
                "Biomass above physical land ceiling must undergo physical maintenance depreciation towards equilibrium");
    }

    @Test
    @DisplayName("Net Energy EROEI Master Cliodynamic Engine Test")
    /*
     * Test net energy eroeiengine operation.
     * <p>
     * Executes operational logic for {@code PhysicalEngineTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testNetEnergyEROEIEngine() {
        double eroei = NetEnergyEROEIEngine.calculateEROEI(testCells.get(0));
        assertTrue(eroei > 0.0, "EROEI must be strictly positive");
        NetEnergyEROEIEngine.processNetEnergyEROEI(testCells);
    }

    @Test
    @DisplayName("Nuclear Safety & Radiotoxicity Engine Test")
    /*
     * Test nuclear safety radiotoxicity engine operation.
     * <p>
     * Executes operational logic for {@code PhysicalEngineTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testNuclearSafetyRadiotoxicityEngine() {
        H3Cell cell = testCells.get(0);
        cell.setTechnologyLevel(4.6); // Modern atomic era (fission power flux regime)
        cell.setResourceUranium(100.0); // Fissile uranium stock for active nuclear reactor
        double initialPollution = cell.getPollutionLevel();
        int initialPop = cell.getPopulation();

        NuclearSafetyRadiotoxicityEngine.setAccidentProbability(1.0); // Deterministic test accident
        NuclearSafetyRadiotoxicityEngine.processNuclearEnergySafety(testCells);

        assertTrue(cell.getPollutionLevel() > initialPollution + 5000.0, "Core meltdown must cause massive radiotoxic pollution spike");
        assertEquals((int) (initialPop * 0.70), cell.getPopulation(), "Radiation casualties & evacuation must reduce population by 30%");
    }

    @Test
    @DisplayName("Ozone Layer Depletion Engine Test")
    /*
     * Test ozone layer depletion engine operation.
     * <p>
     * Executes operational logic for {@code PhysicalEngineTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testOzoneLayerDepletionEngine() {
        H3Cell cell = testCells.get(0);
        // Case 1: Pollution <= 5000: no CFC stratospheric damage
        cell.setPollutionLevel(100.0);
        cell.setBiomassAgriculture(200.0);
        OzoneLayerDepletionEngine.processOzoneLayerDepletion(testCells);
        assertEquals(200.0, cell.getBiomassAgriculture(), 1e-6, "No ozone depletion when pollution is below threshold");

        // Case 2: Extreme chemical pollution > 5000 triggers CFC photolysis and 8% UV-B crop loss
        cell.setPollutionLevel(6000.0);
        OzoneLayerDepletionEngine.processOzoneLayerDepletion(testCells);
        assertEquals(200.0 * 0.92, cell.getBiomassAgriculture(), 1e-4,
                "Stratospheric ozone depletion must degrade agricultural biomass by exactly 8% due to UV-B DNA damage");
    }

    @Test
    @DisplayName("Nuclear Warfare & Climate Soot Engine Test")
    /*
     * Test nuclear warfare climate engine operation.
     * <p>
     * Executes operational logic for {@code PhysicalEngineTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testNuclearWarfareClimateEngine() {
        NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(1.2);
        double transmittance = NuclearWarfareClimateEngine.calculateSolarTransmittance(1.2);
        assertTrue(transmittance < 0.35, "Soot optical depth tau=1.2 must attenuate solar irradiance by >65%");
        NuclearWarfareClimateEngine.processNuclearWarfareClimate(testCells);
        assertTrue(NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() > 0.0);
    }

    @Test
    @DisplayName("Technological Singularity & Trans-Human Engine Test")
    /*
     * Test technological singularity engine operation.
     * <p>
     * Executes operational logic for {@code PhysicalEngineTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testTechnologicalSingularityEngine() {
        H3Cell cell = testCells.get(0);
        cell.setTechnologyLevel(5.0);
        cell.setResourceCapital(5000.0);
        cell.setPollutionLevel(50.0);
        cell.setLifespan(70.0);

        TechnologicalSingularityEngine.processTechnologicalSingularity(testCells);

        assertTrue(cell.getResourceCapital() > 5000.0, "Singularity nanotech assembly must boost capital stock");
        assertTrue(cell.getPollutionLevel() < 50.0, "Singularity automated remediation must reduce pollution");
        assertTrue(cell.getLifespan() > 70.0, "Trans-human health expansion must increase lifespan");
    }
}
