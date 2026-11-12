/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.scenarios;

import org.ether.society.core.engine.SimulationEngineRegistry;
import org.ether.society.core.engine.SimulationEngineModule;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.engines.tier2.historical.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite validating the 7 key historical case studies requested:
 * 1. <b>Rome</b>: Imperial expansion administrative friction O(R^2) & elite overproduction.
 * 2. <b>Japan (Edo Sakoku)</b>: Zero-growth sustainable ecological equilibrium & forest conservation.
 * 3. <b>Amerindian Empires</b>: Virgin-soil epidemic crash (90% mortality) & lack of draft animals.
 * 4. <b>Fertile Crescent</b>: Canal irrigation topsoil salinization & yield degradation.
 * 5. <b>Pleistocene Megafauna Hunting</b>: Overkill hypothesis & wild biomass depletion.
 * 6. <b>Protestant Work Ethic</b>: Frugal capital formation boost & accelerated tech innovation.
 * 7. <b>Celibate Monastic Clergy</b>: Non-reproducing religious elites buffering Malthusian pressure.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class HistoricalBehaviorsTestSuite {

    private List<H3Cell> testCells;

    @BeforeEach
    /*
     * Set up operation.
     * <p>
     * Executes operational logic for {@code HistoricalBehaviorsTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void setUp() {
        RomanImperialCliodynamicEngine.resetState();
        EdoJapanIsolationEngine.resetState();
        AmerindianEcosystemEngine.resetState();

        SimulationEngineRegistry.getInstance().resetAll();
        testCells = new ArrayList<>();
        H3Cell cell = new H3Cell(613503380827930701L, 35.0, 35.0);
        cell.setPopulation(10000);
        cell.setResourceCapital(1000.0);
        cell.setResourceWork(50.0);
        cell.setFoodResource(2000.0);
        cell.setLifespan(40.0);
        cell.setBiomassNatural(500.0);
        cell.setSoilOrganicCarbon(20.0);
        cell.setBiome(Biome.PLAINS);

        testCells.add(cell);
    }

    @AfterEach
    public void tearDown() {
        RomanImperialCliodynamicEngine.resetState();
        EdoJapanIsolationEngine.resetState();
        AmerindianEcosystemEngine.resetState();
    }

    @Test
    /*
     * Test rome imperial cliodynamics operation.
     * <p>
     * Executes operational logic for {@code HistoricalBehaviorsTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testRomeImperialCliodynamics() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getBiomassHuman()[0] = (float) testCells.get(0).getPopulationOr(0);
        buf.getResourceCapital()[0] = (float) testCells.get(0).getResourceCapitalOr(0.0);
        double initialCapital = buf.getResourceCapital()[0];

        RomanImperialCliodynamicEngine.setImperialRadiusKm(2000.0); // Expanded empire
        new RomanImperialCliodynamicEngine().tick(buf, 1.0, 1.0);

        assertTrue(buf.getResourceCapital()[0] < initialCapital,
            "Roman engine: Quadratic administrative friction O(R^2) and elite overproduction should reduce net capital.");
    }

    @Test
    /*
     * Test edo japan isolation equilibrium operation.
     * <p>
     * Executes operational logic for {@code HistoricalBehaviorsTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testEdoJapanIsolationEquilibrium() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getBiomassHuman()[0] = 50000.0f; // Exceeds 30k cell target cap
        buf.getPollutionLevel()[0] = 20.0f;
        buf.getFertility()[0] = 6.0f;
        double initialFertility = buf.getFertility()[0];

        EdoJapanIsolationEngine.setSakokuIsolationActive(true);
        new EdoJapanIsolationEngine().tick(buf, 1.0, 1.0);

        assertTrue(buf.getPollutionLevel()[0] < 20.0f,
            "Edo Japan engine: Organic recycling should reduce pollution levels.");
    }

    @Test
    /*
     * Test amerindian epidemics and draft constraint operation.
     * <p>
     * Executes operational logic for {@code HistoricalBehaviorsTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testAmerindianEpidemicsAndDraftConstraint() {
        H3Cell cell = testCells.get(0);
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        double initialBiomass = cell.getPopulation() * 0.06;
        double initialCapital = Math.max(100.0, cell.getResourceCapitalOr(100.0));
        buf.getBiomassHuman()[0] = (float) initialBiomass;
        buf.getBiomassLivestock()[0] = 0.0f;
        buf.getResourceCapital()[0] = (float) initialCapital;

        // Trigger European/Old World contact epidemic shock
        AmerindianEcosystemEngine.setOldWorldContactTriggered(true);
        new AmerindianEcosystemEngine().tick(buf, 1.0, 1.0);

        assertTrue(buf.getBiomassHuman()[0] < initialBiomass * 0.95,
            "Amerindian engine: Virgin-soil epidemic shock should cause severe demographic crash upon contact.");
        assertTrue(buf.getResourceCapital()[0] < initialCapital,
            "Amerindian engine: Lack of draft animals should constrain capital accumulation.");

        AmerindianEcosystemEngine.setOldWorldContactTriggered(false); // Reset
    }

    @Test
    /*
     * Test fertile crescent salinization operation.
     * <p>
     * Executes operational logic for {@code HistoricalBehaviorsTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testFertileCrescentSalinization() {
        H3Cell cell = testCells.get(0);
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        double initialFood = cell.getFoodResource();
        buf.getBiomassHuman()[0] = (float) (cell.getPopulation() * 0.06);
        buf.getRainfall()[0] = 200.0f;
        buf.getTemperature()[0] = 25.0f;
        buf.getElevation()[0] = 50.0f;
        buf.getFoodResource()[0] = (float) initialFood;
        buf.getResourceCapital()[0] = (float) cell.getResourceCapitalOr(10000.0);

        new org.ether.society.engines.tier2.theories.SoilSalinizationHydrologyEngine().tick(buf, 100.0, 100.0); // 1 century of irrigation

        assertTrue(buf.getFoodResource()[0] < initialFood,
            "Fertile Crescent engine: Canal irrigation topsoil salinization should degrade food yields over time.");
    }

    @Test
    /*
     * Test pleistocene megafauna overkill operation.
     * <p>
     * Executes operational logic for {@code HistoricalBehaviorsTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testPleistoceneMegafaunaOverkill() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        buf.getBiomassHuman()[0] = 200.0f * 0.06f; // 200 humans vs 500 wild biomass
        buf.getBiomassNatural()[0] = 500.0f;
        double initialBiomass = buf.getBiomassNatural()[0];

        new MegafaunaEcosystemEngine().tick(buf, 1.0, 12.0);

        assertTrue(buf.getBiomassNatural()[0] < initialBiomass,
            "Megafauna engine: High hunting pressure ratio should trigger megafauna stock decline.");
    }

    @Test
    /*
     * Test protestant work ethic operation.
     * <p>
     * Executes operational logic for {@code HistoricalBehaviorsTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testProtestantWorkEthic() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getBiomassHuman()[0] = (float) testCells.get(0).getPopulationOr(0);
        buf.getResourceCapital()[0] = (float) testCells.get(0).getResourceCapitalOr(0.0);
        double initialCapital = buf.getResourceCapital()[0];

        new ProtestantWorkEthicEngine().tick(buf, 1.0, 1.0);

        assertTrue(buf.getResourceCapital()[0] > initialCapital,
            "Protestant Work Ethic: High savings rate should boost capital formation.");
    }

    @Test
    /*
     * Test celibate monastic demographic buffer operation.
     * <p>
     * Executes operational logic for {@code HistoricalBehaviorsTestSuite} within the interactive JavaFX visualization and presentation layer.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testCelibateMonasticDemographicBuffer() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getFoodResource()[0] = 500.0f;
        buf.getBiomassHuman()[0] = 10000.0f; // Food stress: food / pop = 0.05 < 0.10 threshold
        buf.getFertility()[0] = 6.0f;
        double initialFertility = buf.getFertility()[0];

        new MonasticDemographicBufferEngine().tick(buf, 1.0, 1.0);

        assertTrue(buf.getFertility()[0] < initialFertility,
            "Celibate Clergy engine: Monastic vocation under Malthusian stress should absorb demographic surplus.");
    }
}


