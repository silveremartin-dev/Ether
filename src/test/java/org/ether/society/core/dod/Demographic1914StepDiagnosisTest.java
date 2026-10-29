package org.ether.society.core.dod;

import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.model.Scenario;
import org.ether.society.database.H3Cell;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.config.ConfigurationLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

public class Demographic1914StepDiagnosisTest {

    @Test
    public void testIsolateDrop() throws Exception {
        Scenario ww1Scenario = null;
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            if ("world_wars_totalitarian_1914".equals(s.getPresetKey())) {
                ww1Scenario = s;
                break;
            }
        }
        ww1Scenario.setH3Resolution(2);
        ww1Scenario.setTargetCohortSize(50000);

        List<H3Cell> cells = ProceduralGenerator.getInstance().generatePlanet(PlanetPreset.EARTH_LIKE.withResolution(2));
        var config = ConfigurationLoader.loadDefault();
        H3SimulationEngine engine = new H3SimulationEngine(config);
        engine.initializeFromScenario(ww1Scenario, cells);

        System.out.println("0. Initial pop in engine: " + engine.getTotalPopulation());
        long p0 = 0;
        for (H3Cell c : cells) if (c.getPopulation() != null) p0 += c.getPopulation();
        System.out.println("0. Initial pop in cells: " + p0);

        // Step by step inside stepForward
        engine.syncCellsToBuffer();
        long pBuffer = 0;
        for (float v : engine.getWorldBuffer().getBiomassHuman()) pBuffer += (long) v;
        System.out.println("0. Pop in WorldBuffer after syncCellsToBuffer: " + pBuffer);

        // Run stepForward(1)
        engine.stepForward(1);
        System.out.println("1. Engine pop after stepForward(1): " + engine.getTotalPopulation());
        long p1 = 0;
        for (H3Cell c : cells) if (c.getPopulation() != null) p1 += c.getPopulation();
        System.out.println("1. Cells pop after stepForward(1): " + p1);
    }
}
