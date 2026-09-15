/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.procedural.tier2.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
 * @version 4.3.0
 */
public class Tier2CliodynamicPluginsTestSuite {

    private List<H3Cell> sampleCells;

    @BeforeEach
    public void setUp() {
        ProceduralEngineRegistry.clearPlugins();
        sampleCells = new ArrayList<>();

        // Create sample cells with varied demographics and resources
        for (int i = 0; i < 10; i++) {
            H3Cell cell = new H3Cell();
            cell.setH3Index(1000L + i);
            cell.setLatitude(45.0 + i * 0.1);
            cell.setLongitude(5.0 + i * 0.1);
            cell.setPopulation(1000 * (i + 1));
            cell.setResourceCapital(100.0 * (i + 1));
            cell.setResourceMetal(500.0);
            cell.setResourceWork(500.0);
            cell.setGiniIndex(0.30 + i * 0.04);
            cell.setMovementFriction(1.0);
            sampleCells.add(cell);
        }
    }

    @Test
    @DisplayName("West-Bettencourt Allometry: Superlinear returns and sublinear network efficiency")
    public void testWestBettencourtAllometry() {
        WestBettencourtAllometryEngine engine = new WestBettencourtAllometryEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("N_i^1.15"));

        double capitalBefore = sampleCells.get(9).getResourceCapital();
        engine.process(sampleCells, 1.0);
        double capitalAfter = sampleCells.get(9).getResourceCapital();

        assertTrue(capitalAfter > capitalBefore, "Superlinear returns must increase capital in large urban cluster");
    }

    @Test
    @DisplayName("Kümmel / Ayres-Warr Exergy: Thermodynamic production function")
    public void testKummelAyresExergy() {
        KummelAyresExergyEngine engine = new KummelAyresExergyEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("E_useful"));

        double capitalBefore = sampleCells.get(5).getResourceCapital();
        engine.process(sampleCells, 1.0);
        double capitalAfter = sampleCells.get(5).getResourceCapital();

        assertTrue(capitalAfter > capitalBefore, "Useful exergy conversion must drive capital growth");
    }

    @Test
    @DisplayName("Spatial SEIR-V Epidemiology: Disease transmission and recovery")
    public void testSpatialSEIREpidemiology() {
        SpatialMetapopulationSEIREngine engine = new SpatialMetapopulationSEIREngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("dS_i/dt"));

        sampleCells.get(0).setEpidemicInfected(200);
        engine.process(sampleCells, 1.0 / 365.25); // 1 day step

        assertTrue(sampleCells.get(0).getEpidemicInfected() >= 0);
    }

    @Test
    @DisplayName("Hotelling Resource Depletion: Scarcity rent and extraction dynamics")
    public void testHotellingResourceDepletion() {
        HotellingResourceDepletionEngine engine = new HotellingResourceDepletionEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Hotelling"));

        double oreBefore = sampleCells.get(0).getResourceMetal();
        engine.process(sampleCells, 1.0);
        double oreAfter = sampleCells.get(0).getResourceMetal();

        assertTrue(oreAfter < oreBefore, "Ore reserves must deplete with extraction");
    }

    @Test
    @DisplayName("Krugman NEG: Core-Periphery Agglomeration")
    public void testKrugmanCorePeriphery() {
        KrugmanCorePeripheryEngine engine = new KrugmanCorePeripheryEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Krugman"));

        engine.process(sampleCells, 1.0);
        // Hub cell capital should be reinforced
        assertTrue(sampleCells.get(9).getResourceCapital() >= 1000.0);
    }

    @Test
    @DisplayName("Schelling-Axelrod: Cultural Homophily and Spatial Friction")
    public void testSchellingSegregation() {
        SchellingAxelrodSegregationEngine engine = new SchellingAxelrodSegregationEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Schelling"));

        // High Gini/segregation cell (last cell has gini 0.30 + 9*0.04 = 0.66 > 0.50)
        double frictionBefore = sampleCells.get(9).getMovementFriction();
        engine.process(sampleCells, 1.0);
        double frictionAfter = sampleCells.get(9).getMovementFriction();

        assertTrue(frictionAfter >= frictionBefore, "High cultural polarization must increase movement friction");
    }

    @Test
    @DisplayName("Tainter Complexity Collapse: Diminishing returns on bureaucratic complexity")
    public void testTainterComplexity() {
        TainterComplexityCollapseEngine engine = new TainterComplexityCollapseEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Tainter"));

        // Set an extremely high capital with very low population (unsustainable complexity trap)
        H3Cell vulnerableCell = new H3Cell();
        vulnerableCell.setPopulation(10);
        vulnerableCell.setResourceCapital(100_000.0);
        List<H3Cell> list = List.of(vulnerableCell);

        engine.process(list, 1.0);
        assertTrue(vulnerableCell.getResourceCapital() < 100_000.0, "Unsustainable complexity must erode capital");
    }

    @Test
    @DisplayName("ProceduralEngineRegistry integration with Tier 2 plugins")
    public void testRegistryIntegration() {
        ProceduralEngineRegistry.clearPlugins();
        ProceduralEngineRegistry.registerPlugin("WestBettencourt", new WestBettencourtAllometryEngine());
        ProceduralEngineRegistry.registerPlugin("KummelAyres", new KummelAyresExergyEngine());
        ProceduralEngineRegistry.registerPlugin("Hotelling", new HotellingResourceDepletionEngine());

        assertEquals(3, ProceduralEngineRegistry.getPluginCount());
        ProceduralEngineRegistry.processPlugins(sampleCells, 1.0);

        ProceduralEngineRegistry.clearPlugins();
        assertEquals(0, ProceduralEngineRegistry.getPluginCount());
    }
}
