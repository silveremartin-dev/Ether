/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.procedural.typeb.*;
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
 * @version 4.0.0
 */
public class HistoricalBehaviorsTestSuite {

    private List<H3Cell> testCells;

    @BeforeEach
    public void setUp() {
        ProceduralEngineRegistry.clearPlugins();
        testCells = new ArrayList<>();
        H3Cell cell = new H3Cell(613503380827930701L, 35.0, 35.0);
        cell.setPopulation(10000);
        cell.setResourceCapital(1000.0);
        cell.setResourceWork(50.0);
        cell.setFoodResource(2000.0);
        cell.setTechnologyLevel(1.5);
        cell.setLifespan(40.0);
        cell.setBiomassNatural(500.0);
        cell.setSoilOrganicCarbon(20.0);
        cell.setBiome(Biome.PLAINS);

        testCells.add(cell);
    }

    @Test
    public void testRomeImperialCliodynamics() {
        H3Cell cell = testCells.get(0);
        double initialCapital = cell.getResourceCapital();

        RomanImperialCliodynamicEngine.setImperialRadiusKm(2000.0); // Expanded empire
        RomanImperialCliodynamicEngine.processHybrid(testCells, 1.0);

        assertTrue(cell.getResourceCapital() < initialCapital,
            "Roman engine: Quadratic administrative friction O(R^2) and elite overproduction should reduce net capital.");
    }

    @Test
    public void testEdoJapanIsolationEquilibrium() {
        H3Cell cell = testCells.get(0);
        cell.setPopulation(50000); // Exceeds 30k cell target cap
        cell.setPollutionLevel(20.0);

        EdoJapanIsolationEngine.setSakokuIsolationActive(true);
        EdoJapanIsolationEngine.processHybrid(testCells, 1.0);

        assertTrue(cell.getPopulation() < 50000,
            "Edo Japan engine: Sakoku equilibrium should regulate population towards carrying capacity.");
        assertTrue(cell.getPollutionLevel() < 20.0,
            "Edo Japan engine: Organic recycling should reduce pollution levels.");
    }

    @Test
    public void testAmerindianEpidemicsAndDraftConstraint() {
        H3Cell cell = testCells.get(0);
        double initialPop = cell.getPopulation();

        // Trigger European/Old World contact epidemic shock
        AmerindianEcosystemEngine.setOldWorldContactTriggered(true);
        AmerindianEcosystemEngine.processHybrid(testCells, 1.0);

        assertTrue(cell.getPopulation() < initialPop * 0.95,
            "Amerindian engine: Virgin-soil epidemic shock should cause severe demographic crash upon contact.");
        assertTrue(cell.getResourceWork() <= 40.0,
            "Amerindian engine: Lack of draft animals should cap mechanical work output.");

        AmerindianEcosystemEngine.setOldWorldContactTriggered(false); // Reset
    }

    @Test
    public void testFertileCrescentSalinization() {
        H3Cell cell = testCells.get(0);
        double initialFood = cell.getFoodResource();

        FertileCrescentSalinizationEngine.processHybrid(testCells, 100.0); // 1 century of irrigation

        assertTrue(cell.getFoodResource() < initialFood,
            "Fertile Crescent engine: Canal irrigation topsoil salinization should degrade food yields over time.");
    }

    @Test
    public void testPleistoceneMegafaunaOverkill() {
        H3Cell cell = testCells.get(0);
        cell.setPopulation(200); // 200 humans vs 500 wild biomass
        double initialBiomass = cell.getBiomassNatural();

        MegafaunaEcosystemEngine.processMegafaunaEcosystem(testCells);

        assertTrue(cell.getBiomassNatural() < initialBiomass,
            "Megafauna engine: High hunting pressure ratio should trigger megafauna stock decline.");
    }

    @Test
    public void testProtestantWorkEthic() {
        H3Cell cell = testCells.get(0);
        double initialCapital = cell.getResourceCapital();
        double initialTech = cell.getTechnologyLevel();

        ProtestantWorkEthicEngine.processHybrid(testCells, 1.0);

        assertTrue(cell.getResourceCapital() > initialCapital,
            "Protestant Work Ethic: High savings rate should boost capital formation.");
        assertTrue(cell.getTechnologyLevel() > initialTech,
            "Protestant Work Ethic: Accelerated literacy and frugality should speed tech innovation.");
    }

    @Test
    public void testCelibateMonasticDemographicBuffer() {
        H3Cell cell = testCells.get(0);
        cell.setFoodResource(500.0);
        cell.setPopulation(10000); // Food stress: food / pop = 0.05 < 0.10 threshold

        double initialPop = cell.getPopulation();
        MonasticDemographicBufferEngine.processHybrid(testCells, 1.0);

        assertTrue(cell.getPopulation() < initialPop,
            "Celibate Clergy engine: Monastic vocation under Malthusian stress should absorb demographic surplus.");
    }
}
