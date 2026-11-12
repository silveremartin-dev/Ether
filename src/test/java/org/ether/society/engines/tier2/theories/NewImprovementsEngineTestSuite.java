/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

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
    /*
     * Set up operation.
     * <p>
     * Executes operational logic for {@code NewImprovementsEngineTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void setUp() {
        testCells = new ArrayList<>();
        H3Cell cell1 = new H3Cell(0x8828308281fffffL, 48.8566, 2.3522);
        cell1.setElevation(5.0);
        cell1.setRainfall(1200.0);
        cell1.setBiome(Biome.BEACH);
        cell1.setPopulation(5000);
        cell1.setFoodResource(400.0);
        cell1.setBiomassNatural(100.0); // Low veg -> high erosion
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
        cell2.setResourceCapital(50.0);
        cell2.setResourceMetal(300.0);
        cell2.setWoodResource(100.0);
        cell2.setEnergyWind(50.0);
        cell2.setTemperature(18.0);

        testCells.add(cell1);
        testCells.add(cell2);
    }

    @Test
    /*
     * Test bifurcation chaos engine operation.
     * <p>
     * Executes operational logic for {@code NewImprovementsEngineTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testBifurcationChaosEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(2);
        buf.getBiomassHuman()[0] = 5000.0f;
        buf.getFoodResource()[0] = 400.0f;
        buf.getBiomassHuman()[1] = 2000.0f;
        buf.getFoodResource()[1] = 300.0f;
        // meanPop = 3500. diffs = +1500, -1500. diff^2 = 2.25e6.
        // variance = (2.25e6 + 2.25e6) / 2 = 2,250,000.0.
        new BifurcationChaosEngine().tick(buf, 1.0, 1.0);
        assertEquals(2_250_000.0, BifurcationChaosEngine.getGlobalSystemVariance(), 1e-1,
                "Analytical variance for {5000, 2000} must equal exactly 2.25e6");
        assertFalse(Double.isNaN(BifurcationChaosEngine.getGlobalLyapunovExponent()),
                "Lyapunov exponent must not be NaN");
    }

    @Test
    /*
     * Test dynamic hydrographic siltation engine operation.
     * <p>
     * Executes operational logic for {@code NewImprovementsEngineTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testDynamicHydrographicSiltationEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(testCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(testCells, buf, false);
        double initialElevation = buf.getElevation()[0];
        new DynamicHydrographicSiltationEngine().tick(buf, 1.0, 12.0);
        assertTrue(buf.getElevation()[0] >= initialElevation, "Estuarine siltation should increase ground elevation");
    }

    @Test
    /*
     * Test physical leontief input output engine operation.
     * <p>
     * Executes operational logic for {@code NewImprovementsEngineTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testPhysicalLeontiefInputOutputEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getBiomassHuman()[0] = 5000.0f;
        buf.getResourceCapital()[0] = 100.0f;
        buf.getMetalResource()[0] = 500.0f;
        buf.getEnergySolar()[0] = 100.0f;
        double initialCapital = buf.getResourceCapital()[0];
        double initialMetal = buf.getMetalResource()[0];
        new PhysicalLeontiefInputOutputEngine().tick(buf, 1.0, 1.0);
        assertTrue(buf.getResourceCapital()[0] > initialCapital, "Capital should expand under Leontief production");
        assertTrue(buf.getMetalResource()[0] < initialMetal, "Metal inputs should be consumed");
    }

    @Test
    public void testGeoengineeringAlbedoFeedbackEngine() {
        GeoengineeringAlbedoFeedbackEngine.resetState();
        org.ether.society.core.dod.WorldBuffer baseBuf = new org.ether.society.core.dod.WorldBuffer(testCells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(testCells, baseBuf, false);
        double initialTemp = baseBuf.getTemperature()[0]; // 20.0 C

        GeoengineeringAlbedoFeedbackEngine engine = new GeoengineeringAlbedoFeedbackEngine();
        // Case 1: Initial state without massive tech cluster, cooling effect should be 0.0
        engine.tick(baseBuf, 1.0, 12.0);
        assertEquals(0.0, GeoengineeringAlbedoFeedbackEngine.getGlobalCoolingEffectCelsius(), 1e-4,
                "Baseline without active SAI deployment should have zero artificial cooling");
        assertEquals(initialTemp, baseBuf.getTemperature()[0], 1e-4, "Temperature should remain unchanged without SAI");

        // Case 2: Deploy planetary SAI with active high-tech industrial civilizations (totalTech5Plus > 50)
        org.ether.society.core.dod.WorldBuffer indBuf = new org.ether.society.core.dod.WorldBuffer(30);
        for (int i = 0; i < 30; i++) {
            indBuf.getH3Indexes()[i] = 0x882830828100000L + i;
            indBuf.getBiomassHuman()[i] = 10000.0f * 0.06f;
            indBuf.getResourceCapital()[i] = 4500.0f * 10000.0f; // (4500 - 2500)/1000 = 2.0 contribution per cell -> 60.0 total
            indBuf.getTemperature()[i] = 20.0f;
            indBuf.getRainfall()[i] = 1000.0f;
        }

        engine.tick(indBuf, 1.0, 12.0);
        // totalTech5Plus = 60.0 -> globalSaiAerosolLoadingTg = min(20.0, 60.0 * 0.1) = 6.0 Tg SO2
        // globalCoolingEffectCelsius = 6.0 * 0.10 = 0.60 °C cooling
        assertEquals(6.0, GeoengineeringAlbedoFeedbackEngine.getGlobalSaiAerosolLoadingTg(), 1e-3,
                "SAI aerosol loading must equal exactly 6.0 Tg SO2 for 30 high-tech nodes");
        assertEquals(0.60, GeoengineeringAlbedoFeedbackEngine.getGlobalCoolingEffectCelsius(), 1e-3,
                "Stratospheric cooling effect must equal exactly 0.60 °C");
        assertEquals(19.708, indBuf.getTemperature()[0], 1e-2,
                "Cell temperature must reflect thermal relaxation toward the 0.60 °C aerosol cooling offset");
    }
}
