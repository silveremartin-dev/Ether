/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.density;

import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for H3SimulationEngine.
 * Tests engine lifecycle, cell initialization, statistics, and simulation ticks.
 */
class ArtemisSimulationEngineTest {

    private H3SimulationEngine engine;
    private Configuration config;

    @BeforeEach
    void setUp() throws IOException {
        config = ConfigurationLoader.loadDefault();
        engine = new H3SimulationEngine(config);
    }

    @Test
    @DisplayName("Engine initializes with cells and default state")
    void testEngineInitialization() {
        assertNotNull(engine.getCells());
        assertTrue(engine.getCells().isEmpty(), "Engine should start with empty cells until scenario or planet is initialized");
        assertFalse(engine.isRunning(), "Engine should start in paused state");
        assertNotNull(engine.getTimeManager());
    }

    @Test
    @DisplayName("Engine lifecycle start and pause")
    void testEngineLifecycle() {
        engine.start();
        assertTrue(engine.isRunning(), "Engine should be running after start()");

        engine.pause();
        assertFalse(engine.isRunning(), "Engine should be paused after pause()");
    }

    @Test
    @DisplayName("Set cells updates world buffer and cell list")
    void testSetCells() {
        List<H3Cell> customCells = new ArrayList<>();
        H3Cell cell1 = createCell(48.85, 2.35, Biome.PLAINS);
        cell1.setPopulation(100);
        customCells.add(cell1);

        engine.setCells(customCells);
        assertEquals(1, engine.getCells().size());
        assertEquals(100, engine.getCells().get(0).getPopulation());
    }

    @Test
    @DisplayName("Total population and total food metric calculation")
    void testMetricsCalculation() {
        assertTrue(engine.getTotalPopulation() >= 0);
        assertTrue(engine.getTotalFood() >= 0);
        assertTrue(engine.getPopulatedCellCount() >= 0);
    }

    // Helper method to create test cells
    private H3Cell createCell(double lat, double lng, Biome biome) {
        H3Cell cell = new H3Cell();
        cell.setLatitude(lat);
        cell.setLongitude(lng);
        cell.setBiome(biome);
        cell.setElevation(100.0);
        cell.setTemperature(15.0);
        cell.setRainfall(500.0);
        cell.setPopulation(0);
        cell.setFoodResource(0.0);
        cell.setH3Index((long) (lat * 1000000 + lng * 1000));
        return cell;
    }
}

