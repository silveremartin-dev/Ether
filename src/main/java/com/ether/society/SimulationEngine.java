package com.ether.society;

import com.ether.society.model.Cell;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(SimulationEngine.class);

    private final TimeManager timeManager;
    private final com.ether.society.model.events.EventManager eventManager;
    private final com.ether.society.model.World world;
    private final com.ether.society.model.ClimateSystem climateSystem;
    private final com.ether.society.model.ResourceSystem resourceSystem;
    private ScheduledExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private int speedMultiplier = 1; // 1x, 5x, 20x
    private static final int BASE_TICK_RATE_MS = 1000; // 1 second per year at 1x

    public SimulationEngine() {
        this.timeManager = new TimeManager();
        this.eventManager = new com.ether.society.model.events.EventManager();
        this.world = new com.ether.society.model.World(100, 100); // Default size
        this.climateSystem = new com.ether.society.model.ClimateSystem();
        this.resourceSystem = new com.ether.society.model.ResourceSystem();

        new com.ether.society.util.TerrainGenerator(System.currentTimeMillis()).generate(world);

        // Initialize with some humans
        initializePopulation();
    }

    private void initializePopulation() {
        // Start with 50 humans in random locations
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(world.getWidth());
            int y = random.nextInt(world.getHeight());
            com.ether.society.model.Human human = new com.ether.society.model.Human(x, y, "GEN_" + i, "CULTURE_A");
            // Give them starting resources
            human.addToInventory("Food", 10.0);
            human.addToInventory("Water", 10.0);
            world.addAgent(human);
        }
        logger.info("Initialized population with 50 humans");
    }

    public void start() {
        if (running.get())
            return;

        running.set(true);
        startLoop();
        logger.info("Simulation started.");
    }

    public void pause() {
        running.set(false);
        if (executorService != null) {
            executorService.shutdownNow();
        }
        logger.info("Simulation paused.");
    }

    public void reset() {
        pause();
        timeManager.reset();
        // Reset world state here
        logger.info("Simulation reset.");
    }

    public void setSpeed(int multiplier) {
        this.speedMultiplier = multiplier;
        if (running.get()) {
            // Restart loop with new speed
            pause();
            start();
        }
        logger.info("Simulation speed set to {}x", multiplier);
    }

    private void startLoop() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        long period = BASE_TICK_RATE_MS / speedMultiplier;

        executorService.scheduleAtFixedRate(this::tick, 0, period, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        if (!running.get())
            return;

        try {
            // 1. Update Time
            timeManager.advanceMonth();

            // 2. Update World
            // Update Climate
            climateSystem.update(world, timeManager.getCurrentMonth());

            // Update Resources
            resourceSystem.update(world);

            // Update Event Manager
            eventManager.tick(world);

            // Update Agents
            for (com.ether.society.model.Agent agent : world.getAgents()) {
                agent.tick(world);
            }

            // Update cell human counts for rendering
            updateCellPopulation();

            // Remove dead agents
            world.getAgents().removeIf(agent -> !agent.isAlive());

            // 3. Notify UI
            Platform.runLater(() -> {
                // Update UI components
                logger.debug("Year: {}, Population: {}", timeManager.getFormattedYear(), world.getAgents().size());
            });

        } catch (Exception e) {
            logger.error("Error in simulation tick", e);
        }
    }

    private void updateCellPopulation() {
        // Reset all cell counts
        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                Cell cell = world.getCell(x, y);
                if (cell != null) {
                    cell.setHumanCount(0);
                }
            }
        }

        // Count agents in each cell
        for (com.ether.society.model.Agent agent : world.getAgents()) {
            if (agent instanceof com.ether.society.model.Human && agent.isAlive()) {
                Cell cell = world.getCell(agent.getX(), agent.getY());
                if (cell != null) {
                    cell.setHumanCount(cell.getHumanCount() + 1);
                }
            }
        }
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }

    public com.ether.society.model.World getWorld() {
        return world;
    }
}
