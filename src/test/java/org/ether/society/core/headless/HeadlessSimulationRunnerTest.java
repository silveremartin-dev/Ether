/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.headless;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HeadlessSimulationRunnerTest {

    @Test
    public void testHeadlessExecutionWithoutGUI() throws Exception {
        HeadlessSimulationRunner runner = new HeadlessSimulationRunner();

        List<H3Cell> cells = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            H3Cell c = new H3Cell();
            c.setH3Index((long) (i + 1));
            c.setBiome(Biome.FOREST);
            c.setPopulation(500);
            c.setBiomassHuman(200.0);
            c.setTemperature(15.0);
            c.setElevation(100.0);
            c.setFoodResource(1000.0);
            c.setLatitude(45.0);
            c.setLongitude(10.0);
            c.setWaterResource(100.0);
            cells.add(c);
        }

        Scenario scenario = new Scenario();
        scenario.setName("Headless Server Test Scenario");

        assertDoesNotThrow(() -> {
            runner.initialize(scenario, cells);
            assertTrue(runner.isInitialized());
            runner.step(3);
        }, "Headless runner must execute simulation steps without GUI dependencies");

        assertNotNull(runner.getEngine());
        assertTrue(runner.getEngine().getTotalPopulation() > 0);
    }
}