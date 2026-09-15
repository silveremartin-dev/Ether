/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.headless;

import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.ether.society.persistence.SimulationSaveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Pure Headless Simulation Runner for Server, HPC & Containerized CLI execution.
 * Completely decoupled from JavaFX or graphical dependencies.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class HeadlessSimulationRunner {
    private static final Logger logger = LoggerFactory.getLogger(HeadlessSimulationRunner.class);

    private final H3SimulationEngine engine;
    private final SimulationSaveManager saveManager;
    private boolean initialized = false;

    public HeadlessSimulationRunner() throws IOException {
        Configuration config = ConfigurationLoader.loadDefault();
        this.engine = new H3SimulationEngine(config);
        this.saveManager = new SimulationSaveManager();
    }

    public HeadlessSimulationRunner(Configuration config) {
        this.engine = new H3SimulationEngine(config);
        this.saveManager = new SimulationSaveManager();
    }

    /**
     * Initializes the simulation engine with a scenario and world cells without GUI.
     */
    public void initialize(Scenario scenario, List<H3Cell> cells) {
        if (scenario == null || cells == null) {
            throw new IllegalArgumentException("Scenario and cells must not be null");
        }
        engine.initializeFromScenario(scenario, cells);
        this.initialized = true;
        logger.info("⚡ Headless simulation runner initialized with scenario '{}' ({} cells).",
                scenario.getName(), cells.size());
    }

    /**
     * Advances the simulation by a specified number of ticks.
     */
    public void step(int ticks) {
        if (!initialized) {
            throw new IllegalStateException("Runner must be initialized before stepping");
        }
        for (int i = 0; i < ticks; i++) {
            engine.stepForward(1);
        }
        logger.info("Completed {} ticks in headless mode. Current year: {}, Population: {}",
                ticks, engine.getTimeManager().getCurrentYear(), engine.getTotalPopulation());
    }

    /**
     * Saves the current world state to disk (with optional encryption).
     */
    public void save(String saveName) {
        if (engine != null) {
            saveManager.saveSimulation(engine, saveName);
        }
    }

    public H3SimulationEngine getEngine() {
        return engine;
    }

    public boolean isInitialized() {
        return initialized;
    }
}