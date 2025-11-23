/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author: Silvere Martin-Michiellot (silvere.martin@gmail.com)
 * Contributors: AI Assistant (Antigravity/Claude)
 */
package com.ether.society.core;

import com.ether.society.config.Configuration;
import com.ether.society.model.ClimateSystem;
import com.ether.society.model.World;
import com.ether.society.model.events.EventManager;
import com.ether.society.util.TerrainGenerator;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Core simulation engine that drives the game loop using Virtual Threads.
 * Manages time progression, world updates, and agent ticking.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 1.0.0
 */
public class SimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(SimulationEngine.class);

    private final Configuration config;
    private final TimeManager timeManager;
    private final EventManager eventManager;
    private final World world;
    private final ClimateSystem climateSystem;
    private final EventBus eventBus;

    private ScheduledExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private int speedMultiplier = 1;

    /**
     * Creates a simulation engine with the given configuration.
     *
     * @param config Simulation configuration
     */
    public SimulationEngine(Configuration config) {
        this.config = config;
        this.timeManager = new TimeManager(config.simulation().startYear());
        this.eventManager = new EventManager();
        this.world = new World(
                config.world().width(), config.world().height());
        this.climateSystem = new ClimateSystem(config.climate());
        this.eventBus = new EventBus();

        initialize();
    }

    /**
     * Initializes the world and agents.
     */
    private void initialize() {
        logger.info("Initializing simulation...");
        // Generate terrain
        TerrainGenerator generator = new TerrainGenerator(config.world().seed());
        generator.generate(world);
        logger.info(
                "World generated: {}x{} with seed {}",
                world.getWidth(),
                world.getHeight(),
                config.world().seed());
    }

    /**
     * Starts the simulation.
     */
    public void start() {
        if (running.getAndSet(true)) {
            logger.warn("Simulation is already running");
            return;
        }

        logger.info("Starting simulation at year {}", timeManager.getFormattedDate());
        startGameLoop();
    }

    /**
     * Pauses the simulation.
     */
    public void pause() {
        if (!running.getAndSet(false)) {
            logger.warn("Simulation is not running");
            return;
        }

        if (executorService != null) {
            executorService.shutdownNow();
        }
        logger.info("Simulation paused");
    }

    /**
     * Resets the simulation to its initial state.
     */
    public void reset() {
        pause();
        timeManager.reset(config.simulation().startYear());
        initialize();
        logger.info("Simulation reset");
        eventBus.publish(new SimulationResetEvent());
    }

    /**
     * Sets the simulation speed multiplier.
     *
     * @param multiplier Speed multiplier (1, 5, or 20)
     */
    public void setSpeed(int multiplier) {
        this.speedMultiplier = multiplier;
        if (running.get()) {
            pause();
            start();
        }
        logger.info("Simulation speed set to {}x", multiplier);
    }

    /**
     * Starts the game loop using a scheduled executor.
     */
    private void startGameLoop() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        long period = config.simulation().tickRateMs() / speedMultiplier;

        executorService.scheduleAtFixedRate(this::tick, 0, period, TimeUnit.MILLISECONDS);
    }

    /**
     * Executes one simulation tick (one month).
     */
    private void tick() {
        if (!running.get())
            return;

        try {
            // 1. Advance time
            timeManager.advanceMonth();

            // 2. Update climate
            climateSystem.update(world, timeManager.getCurrentMonth());

            // 3. Process events
            eventManager.tick(world);

            // 4. Update agents (using Virtual Threads for parallelism)
            world.getAgents()
                    .parallelStream()
                    .forEach(agent -> Thread.ofVirtual().start(() -> agent.tick(world)));

            // 5. Notify UI
            Platform.runLater(() -> eventBus.publish(new TickEvent(timeManager)));

            // Log periodically (every year)
            if (timeManager.getCurrentMonth() == 0) {
                logger.debug(
                        "Year: {}, Population: {}",
                        timeManager.getFormattedDate(),
                        world.getAgents().size());
            }

        } catch (Exception e) {
            logger.error("Error during simulation tick", e);
        }
    }

    // Getters

    public TimeManager getTimeManager() {
        return timeManager;
    }

    public World getWorld() {
        return world;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public Configuration getConfig() {
        return config;
    }

    // Events

    /** Event published when a tick completes. */
    public record TickEvent(TimeManager timeManager) {
    }

    /** Event published when simulation is reset. */
    public record SimulationResetEvent() {
    }
}
