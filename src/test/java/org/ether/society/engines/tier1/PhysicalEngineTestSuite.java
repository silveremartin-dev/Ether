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
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(testCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(testCells, buf, false);
        double initialBiomass = buf.getBiomassAgriculture()[0];
        SoilNutrientNPKEngine.processSoilNutrients(buf);
        assertFalse(Float.isNaN(buf.getBiomassAgriculture()[0]), "Agricultural biomass must not be NaN");
        assertTrue(buf.getBiomassAgriculture()[0] > 0.0f, "Agricultural biomass must remain positive");
        assertTrue(buf.getBiomassAgriculture()[0] <= initialBiomass,
                "Biomass above physical land ceiling must undergo physical maintenance depreciation towards equilibrium");
    }

    @Test
    @DisplayName("Net Energy EROEI Master Cliodynamic Engine Test")
    public void testNetEnergyEROEIEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(testCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(testCells, buf, false);
        new NetEnergyEROEIEngine().tick(buf, 1.0 / 12.0, 1.0);
        assertTrue(buf.getResourceCapital()[0] >= 0.0f);
    }

    @Test
    @DisplayName("Nuclear Safety & Radiotoxicity Engine Test")
    public void testNuclearSafetyRadiotoxicityEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(testCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(testCells, buf, false);
        buf.getResourceUranium()[0] = 100.0f;
        buf.getResourceCapital()[0] = 300000.0f;
        double initialPollution = buf.getPollutionLevel()[0];
        double initialBioHuman = buf.getBiomassHuman()[0];

        NuclearSafetyRadiotoxicityEngine.setAccidentProbability(1.0);
        NuclearSafetyRadiotoxicityEngine.processNuclearEnergySafety(buf);

        assertTrue(buf.getPollutionLevel()[0] > initialPollution + 5000.0, "Core meltdown must cause massive radiotoxic pollution spike");
        assertEquals(initialBioHuman * 0.70, buf.getBiomassHuman()[0], 1e-2, "Radiation casualties & evacuation must reduce population by 30%");
    }

    @Test
    @DisplayName("Ozone Layer Depletion Engine Test")
    public void testOzoneLayerDepletionEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(testCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(testCells, buf, false);

        buf.getPollutionLevel()[0] = 100.0f;
        buf.getBiomassAgriculture()[0] = 200.0f;
        OzoneLayerDepletionEngine.processOzoneLayerDepletion(buf);
        assertEquals(200.0f, buf.getBiomassAgriculture()[0], 1e-4f, "No ozone depletion when pollution is below threshold");

        buf.getPollutionLevel()[0] = 6000.0f;
        OzoneLayerDepletionEngine.processOzoneLayerDepletion(buf);
        assertEquals(200.0 * 0.92, buf.getBiomassAgriculture()[0], 0.05,
                "Stratospheric ozone depletion must degrade agricultural biomass by exactly 8% due to UV-B DNA damage");
    }

    @Test
    @DisplayName("Nuclear Warfare & Climate Soot Engine Test")
    public void testNuclearWarfareClimateEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(testCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(testCells, buf, false);
        NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(1.2);
        double transmittance = NuclearWarfareClimateEngine.calculateSolarTransmittance(1.2);
        assertTrue(transmittance < 0.35, "Soot optical depth tau=1.2 must attenuate solar irradiance by >65%");
        NuclearWarfareClimateEngine.processNuclearWarfareClimate(buf);
        assertTrue(NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() > 0.0);
    }
}
