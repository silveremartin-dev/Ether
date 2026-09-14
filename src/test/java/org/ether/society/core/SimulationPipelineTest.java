/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationPipelineTest {

    @Test
    public void testSimulationPipelineExecution() throws Exception {
        Configuration config = ConfigurationLoader.loadDefault();
        H3SimulationEngine engine = new H3SimulationEngine(config);

        List<H3Cell> cells = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            H3Cell cell = new H3Cell();
            cell.setH3Index((long) (i + 1));
            cell.setBiome(Biome.FOREST);
            cell.setPopulation(1000);
            cell.setBiomassHuman(500.0);
            cell.setTemperature(15.0);
            cell.setFoodResource(2000.0);
            cell.setElevation(100.0);
            cell.setLatitude(45.0);
            cell.setLongitude(5.0);
            cell.setWaterResource(50.0);
            cell.setEnergyWind(100.0);
            cells.add(cell);
        }

        Scenario scenario = new Scenario();
        scenario.setName("Pipeline Test Scenario");
        scenario.setDescription("Pipeline Test Scenario Description");

        engine.initializeFromScenario(scenario, cells);

        assertDoesNotThrow(() -> {
            engine.stepForward(1);
        }, "SimulationPipeline must execute tick without throwing exceptions");

        assertTrue(engine.getCells().size() > 0, "Cells must remain intact after pipeline tick");
    }
}
