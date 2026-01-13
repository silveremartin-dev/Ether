/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.config.Configuration;
import org.ether.society.data.SampleDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.density.ArtemisSimulationEngine;
import org.ether.society.density.H3ClimateSystem;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.ether.society.persistence.GameSaveManager;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * H3-based simulation engine.
 * Manages the simulation loop for the hexagonal grid system.
 * 
 * Implements density-based simulation:
 * - Climate updates (seasonal temperature changes)
 * - Food resource production (seasonal, biome-based)
 * - Population dynamics (growth/starvation)
 * - Migration as density flow between cells
 */
public class H3SimulationEngine implements ISimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(H3SimulationEngine.class);

    private final Configuration config;
    private final TimeManager timeManager;
    private final org.ether.society.events.EventSystem eventSystem;

    // Density-based simulation systems
    private final H3ClimateSystem climateSystem;
    private final ArtemisSimulationEngine densityEngine;
    private final org.ether.society.agents.AgentManager agentManager;
    private final org.ether.society.gpu.GPUManager gpuManager;

    private final org.ether.society.analytics.HistoryManager historyManager;
    private final org.ether.society.diplomacy.DiplomacyManager diplomacyManager;
    private final org.ether.society.diplomacy.PoliticalSimulationEngine politicalEngine;
    private final GameSaveManager gameSaveManager;

    private List<H3Cell> cells;
    private Scenario currentScenario;

    private ScheduledExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private int speedMultiplier = 1;

    public H3SimulationEngine(Configuration config) {
        this.config = config;
        this.timeManager = new TimeManager(config.simulation().startYear());
        this.eventSystem = new org.ether.society.events.EventSystem();
        this.climateSystem = new H3ClimateSystem();
        this.densityEngine = new ArtemisSimulationEngine();
        this.densityEngine.setEventSystem(this.eventSystem); // Inject EventSystem
        // Initialize AgentManager - need H3Service which isn't here?
        // Actually H3SimulationEngine creates data or loads it.
        // We'll create H3Service instance if not passed, or just instantiate logic.
        // Assuming H3Service is singleton or accessible.
        // Initialize AgentManager
        this.agentManager = new org.ether.society.agents.AgentManager(org.ether.society.h3.H3Service.getInstance());

        // Initialize GPU Manager
        this.gpuManager = new org.ether.society.gpu.GPUManager();
        this.climateSystem.setGpuManager(this.gpuManager);

        // Initialize History Manager
        this.historyManager = new org.ether.society.analytics.HistoryManager();

        // Initialize Diplomacy & Politics
        this.diplomacyManager = new org.ether.society.diplomacy.DiplomacyManager();
        this.politicalEngine = new org.ether.society.diplomacy.PoliticalSimulationEngine(this.diplomacyManager);
        this.gameSaveManager = new GameSaveManager();

        initialize();
    }

    public org.ether.society.events.EventSystem getEventSystem() {
        return eventSystem;
    }

    private void initialize() {
        logger.info("Initializing H3 simulation...");
        // Generate sample data for Europe
        this.cells = SampleDataGenerator.generateEuropeSample();

        // Initialize some starting population
        initializePopulation();

        logger.info("H3 World generated: {} cells", cells.size());
    }

    /**
     * Initialize simulation from a Scenario configuration.
     * Uses PreComputePhase for climate, resources, and population.
     * 
     * @param scenario The scenario configuration
     * @param cells    Pre-generated cells (from PlanetGenerator or loaded)
     */
    public void initializeFromScenario(Scenario scenario, List<H3Cell> cells) {
        logger.info("Initializing from scenario: {}", scenario.getName());

        boolean wasRunning = running.get();
        if (wasRunning) {
            pause();
        }

        this.currentScenario = scenario;
        this.cells = cells;

        // Reset time to scenario start year
        timeManager.reset((int) scenario.getStartDateYear());

        // Run pre-computation phase
        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(cells);

        logger.info("Scenario '{}' initialized: {} cells, start year {}",
                scenario.getName(), cells.size(), scenario.getStartDateYear());
    }

    /**
     * Get the current scenario (if initialized via scenario).
     */
    public Scenario getCurrentScenario() {
        return currentScenario;
    }

    /**
     * Initialize starting population in habitable cells.
     */
    private void initializePopulation() {
        int populatedCells = 0;
        for (H3Cell cell : cells) {
            // Skip ocean cells
            org.ether.society.model.Biome biome = cell.getBiome();
            if (biome == org.ether.society.model.Biome.OCEAN ||
                    biome == org.ether.society.model.Biome.DEEP_OCEAN) {
                continue;
            }

            // Add initial population based on biome productivity
            int basePop = switch (biome) {
                case PLAINS -> 50;
                case FOREST -> 30;
                case JUNGLE -> 40;
                case HILLS -> 20;
                case BEACH -> 25;
                case MOUNTAINS -> 10;
                case TUNDRA -> 5;
                case DESERT -> 3;
                case SNOW -> 2;
                default -> 0;
            };

            // Only populate ~10% of cells initially
            if (Math.random() < 0.1 && basePop > 0) {
                cell.setPopulation(basePop);
                populatedCells++;
            }
        }

        logger.info("Initialized population in {} cells", populatedCells);

        // Initialize Seeds for Nations
        initializePoliticalSeeding(populatedCells);
    }

    @Override
    public void start() {
        if (running.getAndSet(true)) {
            logger.warn("Simulation is already running");
            return;
        }

        logger.info("Starting H3 simulation at year {}", timeManager.getFormattedDate());
        startGameLoop();
    }

    @Override
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

    @Override
    public void reset() {
        pause();
        timeManager.reset(config.simulation().startYear());
        historyManager.reset();
        initialize();
        logger.info("Simulation reset");
    }

    @Override
    public void setSpeed(int multiplier) {
        this.speedMultiplier = multiplier;
        if (running.get()) {
            pause();
            start();
        }
        logger.info("Simulation speed set to {}x", multiplier);
    }

    @Override
    public TimeManager getTimeManager() {
        return timeManager;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    public List<H3Cell> getCells() {
        return cells;
    }

    /**
     * Replace current world with new cells.
     * Starts simulation fresh with these cells.
     */
    public void setCells(List<H3Cell> newCells) {
        boolean wasRunning = running.get();
        if (wasRunning) {
            pause();
        }

        this.cells = newCells;

        // Initialize population if fresh generation didn't do it
        // (PlanetGenerator might not populate people, only biomes)
        initializePopulation();

        logger.info("World replaced with {} new cells", cells.size());

        // Don't auto-restart, let user decide
    }

    /**
     * Save the game state.
     * @param saveName The name of the save
     */
    public void saveGame(String saveName) {
        boolean wasRunning = running.get();
        if (wasRunning) {
            pause();
        }
        
        gameSaveManager.saveGame(this, saveName);
        
        if (wasRunning) {
            start();
        }
    }

    /**
     * Load the game state.
     * @param saveId The ID of the save (currently ignored for single-state DB)
     */
    public void loadGame(String saveId) {
        boolean wasRunning = running.get();
        if (wasRunning) {
            pause();
        }
        
        gameSaveManager.loadGame(saveId, this);
        // TimeManager reset might be needed here based on loading logic, but currently simple content load.
    }

    public H3ClimateSystem getClimateSystem() {
        return climateSystem;
    }

    public ArtemisSimulationEngine getDensityEngine() {
        return densityEngine;
    }

    private void startGameLoop() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        long period = config.simulation().tickRateMs() / speedMultiplier;

        executorService.scheduleAtFixedRate(this::tick, 0, period, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        if (!running.get())
            return;

        try {
            // 1. Advance time
            timeManager.advanceMonth();
            int month = timeManager.getCurrentMonth();
            int year = timeManager.getCurrentYear();

            // 2. Update climate (seasonal temperatures)
            climateSystem.updateClimate(cells, month);

            // 3. Run density simulation (food production, population, migration)
            densityEngine.tick(cells, month, year);

            // 4. Update agents
            agentManager.update();

            // 5. Check for events
            eventSystem.checkEvents(year, getTotalPopulation(), getTotalFood());

            // 6. Run Political Simulation
            politicalEngine.tick(cells);

            // 7. Capture Analytics (e.g. at end of month)
            historyManager.captureSnapshot(this);

        } catch (Exception e) {
            logger.error("Error during simulation tick", e);
        }
    }

    /**
     * Get total population across all cells.
     */
    public long getTotalPopulation() {
        return cells.stream().mapToLong(H3Cell::getPopulation).sum();
    }

    /**
     * Get total food across all cells.
     */
    public double getTotalFood() {
        return cells.stream().mapToDouble(H3Cell::getFoodResource).sum();
    }

    /**
     * Get number of populated cells.
     */
    public long getPopulatedCellCount() {
        return cells.stream().filter(c -> c.getPopulation() > 0).count();
    }

    private void initializePoliticalSeeding(int populatedCount) {
        if (populatedCount < 10)
            return;

        // Pick top 3 populated cells to start nations
        cells.stream()
                .filter(c -> c.getPopulation() > 0)
                .sorted(java.util.Comparator.comparingInt(H3Cell::getPopulation).reversed())
                .limit(3)
                .forEach(c -> {
                    if (c.getOwner() == null) {
                        String name = "Nation of " + c.getH3Index(); // Placeholder name
                        // Random vivid color
                        javafx.scene.paint.Color color = javafx.scene.paint.Color.hsb(Math.random() * 360, 0.8, 0.9);

                        org.ether.society.model.Nation nation = new org.ether.society.model.Nation(name, color, c);
                        diplomacyManager.registerNation(nation);
                    }
                });

        logger.info("Initialized {} starter nations", diplomacyManager.getNations().size());
    }

    public org.ether.society.agents.AgentManager getAgentManager() {
        return agentManager;
    }

    public org.ether.society.analytics.HistoryManager getHistoryManager() {
        return historyManager;
    }
}
