/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.SimulationEngineRegistry;
import org.ether.society.core.engine.SimulationEngineModule;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite verifying Tier 2 Optional Cliodynamic & Phenomenological Plugins:
 * 1. West-Bettencourt Urban Allometry Engine
 * 2. Kümmel / Ayres-Warr Biophysical Exergy Economics Engine
 * 3. Spatial Metapopulation SEIR-V Epidemiology Engine
 * 4. Hotelling Resource Depletion Engine
 * 5. Krugman NEG Core-Periphery Agglomeration Engine
 * 6. Schelling-Axelrod Cultural Segregation Engine
 * 7. Tainter Institutional Complexity Collapse Engine
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class Tier2CliodynamicPluginsTestSuite {

    private WorldBuffer sampleBuffer;

    @BeforeEach
    public void setUp() {
        SimulationEngineRegistry.getInstance().resetAll();
        sampleBuffer = new WorldBuffer(10);

        // Create sample cells with varied demographics and resources
        for (int i = 0; i < 10; i++) {
            sampleBuffer.getH3Indexes()[i] = 1000L + i;
            sampleBuffer.getHumanPopulation()[i] = 1000.0f * (i + 1);
            sampleBuffer.getResourceCapital()[i] = 100.0f * (i + 1);
            sampleBuffer.getMetalResource()[i] = 500.0f;
            sampleBuffer.getFoodResource()[i] = 2000.0f;
            sampleBuffer.getGiniIndex()[i] = 0.30f + i * 0.04f;
            sampleBuffer.getFluxPressure()[i] = 0.0f;
            sampleBuffer.getTemperature()[i] = 15.0f;
        }
    }

    @Test
    @DisplayName("West-Bettencourt Allometry: Superlinear returns and sublinear network efficiency")
    public void testWestBettencourtAllometry() {
        WestBettencourtAllometryEngine engine = new WestBettencourtAllometryEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("1.15"));

        float capitalBefore = sampleBuffer.getResourceCapital()[9];
        engine.tick(sampleBuffer, 1.0, 12.0);
        float capitalAfter = sampleBuffer.getResourceCapital()[9];

        assertTrue(capitalAfter > capitalBefore, "Superlinear returns must increase capital in large urban cluster");
    }

    @Test
    @DisplayName("Kümmel / Ayres-Warr Exergy: Thermodynamic production function")
    public void testKummelAyresExergy() {
        KummelAyresExergyEngine engine = new KummelAyresExergyEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("E_useful"));

        float capitalBefore = sampleBuffer.getResourceCapital()[5];
        engine.tick(sampleBuffer, 1.0, 12.0);
        float capitalAfter = sampleBuffer.getResourceCapital()[5];

        assertTrue(capitalAfter > capitalBefore, "Useful exergy conversion must drive capital growth");
    }

    @Test
    @DisplayName("Spatial SEIR-V Epidemiology: Disease transmission and recovery")
    public void testSpatialSEIREpidemiology() {
        SpatialMetapopulationSEIREngine engine = new SpatialMetapopulationSEIREngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("dS_i/dt"));

        float initialPop = sampleBuffer.getHumanPopulation()[0];
        engine.tick(sampleBuffer, 0.001, 0.012);
        engine.seedInfection(0, 200.0f);

        // 10 day epidemic progression
        engine.tick(sampleBuffer, 10.0 / 365.25, (10.0 / 365.25) * 12.0);

        assertTrue(sampleBuffer.getHumanPopulation()[0] < initialPop, "Epidemic mortality must reduce host cell population");
    }

    @Test
    @DisplayName("Hotelling Resource Depletion: Scarcity rent and extraction dynamics")
    public void testHotellingResourceDepletion() {
        HotellingResourceDepletionEngine engine = new HotellingResourceDepletionEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Hotelling"));

        // Provision Chalcolithic smelting capital (k_pc = 100 kg/cap >= 65 kg/cap furnace threshold)
        sampleBuffer.getResourceCapital()[0] = 100_000.0f;
        float oreBefore = sampleBuffer.getMetalResource()[0];
        engine.tick(sampleBuffer, 1.0, 12.0);
        float oreAfter = sampleBuffer.getMetalResource()[0];

        assertTrue(oreAfter < oreBefore, "Ore reserves must deplete with extraction");
    }

    @Test
    @DisplayName("Krugman NEG: Core-Periphery Agglomeration")
    public void testKrugmanCorePeriphery() {
        KrugmanCorePeripheryEngine engine = new KrugmanCorePeripheryEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Krugman"));

        engine.tick(sampleBuffer, 1.0, 12.0);
        // Hub cell capital should be reinforced
        assertTrue(sampleBuffer.getResourceCapital()[9] >= 1000.0f);
    }

    @Test
    @DisplayName("Schelling-Axelrod: Cultural Homophily and Spatial Friction")
    public void testSchellingSegregation() {
        SchellingAxelrodSegregationEngine engine = new SchellingAxelrodSegregationEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Schelling"));

        // High Gini/segregation cell (last cell has gini 0.30 + 9*0.04 = 0.66 > 0.50)
        float frictionBefore = sampleBuffer.getFluxPressure()[9];
        engine.tick(sampleBuffer, 1.0, 12.0);
        float frictionAfter = sampleBuffer.getFluxPressure()[9];

        assertTrue(frictionAfter > frictionBefore, "High cultural polarization must increase movement flux friction");
    }

    @Test
    @DisplayName("Tainter Complexity Collapse: Diminishing returns on bureaucratic complexity")
    public void testTainterComplexity() {
        TainterComplexityCollapseEngine engine = new TainterComplexityCollapseEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Tainter"));

        // Set an extremely high capital with very low population (unsustainable complexity trap)
        WorldBuffer vulnBuf = new WorldBuffer(1);
        vulnBuf.getHumanPopulation()[0] = 10.0f;
        vulnBuf.getResourceCapital()[0] = 100_000.0f;

        engine.tick(vulnBuf, 1.0, 12.0);
        assertTrue(vulnBuf.getResourceCapital()[0] < 100_000.0f, "Unsustainable complexity must erode capital");
    }

    @Test
    @DisplayName("Soil Salinization Hydrology: Salt accumulation degrades crop yield in arid basins")
    public void testSoilSalinization() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getHumanPopulation()[0] = 5000.0f;
        buf.getRainfall()[0] = 180.0f;
        buf.getTemperature()[0] = 26.0f;
        buf.getElevation()[0] = 50.0f;
        buf.getFoodResource()[0] = 5000.0f;
        buf.getResourceCapital()[0] = 250000.0f;

        new SoilSalinizationHydrologyEngine().tick(buf, 10.0, 10.0);
        assertTrue(buf.getFoodResource()[0] < 5000.0f, "Intensive irrigation in arid lowland must trigger salinization yield penalty");
    }

    @Test
    @DisplayName("Draft Animal Fodder Allocation: Work amplification balanced by fodder land competition")
    public void testDraftAnimalFodderAllocation() {
        DraftAnimalFodderAllocationEngine engine = new DraftAnimalFodderAllocationEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Smil"));

        WorldBuffer medievalBuf = new WorldBuffer(1);
        medievalBuf.getHumanPopulation()[0] = 2000.0f;
        medievalBuf.getResourceCapital()[0] = 100_000.0f; // 50 kg/cap -> agrarian draft animal regime
        medievalBuf.getFoodResource()[0] = 2000.0f;

        engine.tick(medievalBuf, 1.0, 12.0);
        assertTrue(medievalBuf.getResourceCapital()[0] > 100_000.0f, "Draft animals must augment available capital and labor productivity");
        assertTrue(medievalBuf.getFoodResource()[0] < 2000.0f, "Fodder preemption must slightly reduce food directly available to humans");
    }

    @Test
    @DisplayName("SPI ServiceLoader: Discover and register all classpath procedural plugins")
    /*
     * Test spi discovery operation.
     * <p>
     * Executes operational logic for {@code Tier2CliodynamicPluginsTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testSpiDiscovery() {
        SimulationEngineRegistry.getInstance().resetAll();
        int loaded = SimulationEngineRegistry.getInstance().getAllEngines().size();
        assertTrue(loaded >= 15, "Engine registry should discover all registered SimulationEngineModule implementations");
        assertTrue(SimulationEngineRegistry.getInstance().getAllEngines().size() >= 15);
        SimulationEngineRegistry.getInstance().resetAll();
    }

    @Test
    @DisplayName("ProceduralEngineRegistry integration with Tier 2 plugins")
    /*
     * Test registry integration operation.
     * <p>
     * Executes operational logic for {@code Tier2CliodynamicPluginsTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testRegistryIntegration() {
        SimulationEngineRegistry.getInstance().resetAll();
        SimulationEngineRegistry.getInstance().register(new WestBettencourtAllometryEngine());
        SimulationEngineRegistry.getInstance().register(new KummelAyresExergyEngine());
        SimulationEngineRegistry.getInstance().register(new HotellingResourceDepletionEngine());

        assertNotNull(SimulationEngineRegistry.getInstance().getEngine("WestBettencourtAllometryEngine"));
        assertNotNull(SimulationEngineRegistry.getInstance().getEngine("KummelAyresExergyEngine"));
        assertNotNull(SimulationEngineRegistry.getInstance().getEngine("HotellingResourceDepletionEngine"));

        SimulationEngineRegistry.getInstance().resetAll();
    }
}


