/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.config.Configuration;
import org.ether.society.data.SampleDataGenerator;
import org.ether.society.database.H3Cell;
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
    private final org.ether.society.agents.AgentManager agentManager;
    private final org.ether.society.gpu.GPUManager gpuManager;

    private final org.ether.society.analytics.HistoryManager historyManager;
    private final org.ether.society.diplomacy.DiplomacyManager diplomacyManager;
    private final org.ether.society.diplomacy.PoliticalSimulationEngine politicalEngine;
    private final GameSaveManager gameSaveManager;

    // DOD Layer
    private org.ether.society.core.dod.WorldBuffer worldBuffer;
    private org.ether.society.core.dod.AgentBuffer agentBuffer;
    private org.ether.society.flux.FluxEngine fluxEngine;
    private org.ether.society.core.dod.DemographicKernel demographicKernel;
    private org.ether.society.core.dod.UrbanKernel urbanKernel;
    private org.ether.society.core.dod.CultureKernel cultureKernel;
    private org.ether.society.core.dod.EnvironmentalKernel environmentalKernel;
    private org.ether.society.core.dod.StatisticsKernel statisticsKernel;

    private long lastTickTime = 0;
    private double currentTPS = 0;
    private float currentGini = 0;
    private float currentGDP = 0;
    private float currentLifeExpectancy = 0;
    private float currentFertility = 0;
    private int[] densityDistribution = new int[20]; // 20 bins

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

        this.fluxEngine = new org.ether.society.flux.FluxEngine();
        this.demographicKernel = new org.ether.society.core.dod.DemographicKernel();
        this.urbanKernel = new org.ether.society.core.dod.UrbanKernel();
        this.cultureKernel = new org.ether.society.core.dod.CultureKernel();
        this.environmentalKernel = new org.ether.society.core.dod.EnvironmentalKernel();
        this.statisticsKernel = new org.ether.society.core.dod.StatisticsKernel();

        initialize();
    }

    public org.ether.society.events.EventSystem getEventSystem() {
        return eventSystem;
    }

    private void initialize() {
        logger.info("Initializing H3 simulation...");
        // Generate sample data for Europe
        this.cells = SampleDataGenerator.generateEuropeSample();

        // Initialize population
        initializePopulation();

        // Initialize DOD buffers
        this.worldBuffer = new org.ether.society.core.dod.WorldBuffer(cells.size());
        this.agentBuffer = new org.ether.society.core.dod.AgentBuffer(cells.size() / 10); // Assume 10% cells have agents initially
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, worldBuffer);
        org.ether.society.data.DODDataGenerator.initializeAgentBuffer(worldBuffer, agentBuffer);

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

    private void startGameLoop() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        long period = config.simulation().tickRateMs() / speedMultiplier;

        executorService.scheduleAtFixedRate(this::tick, 0, period, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        if (!running.get())
            return;

        long now = System.nanoTime();
        if (lastTickTime != 0) {
            double diff = (now - lastTickTime) / 1_000_000_000.0;
            currentTPS = 1.0 / diff;
        }
        lastTickTime = now;

        try {
            // 1. Advance time
            timeManager.advanceMonth();
            int month = timeManager.getCurrentMonth();
            int year = timeManager.getCurrentYear();

            // 2. Update climate (seasonal temperatures)
            climateSystem.updateClimate(cells, month);
            // Sync climate changes to WorldBuffer
            syncClimateToBuffer();

            // 3. Run DOD kernels (Replaces Artemis)
            float dt = 1.0f; // 1 month
            environmentalKernel.tick(worldBuffer, month, dt);
            fluxEngine.tick(worldBuffer, dt);
            demographicKernel.tick(worldBuffer, agentBuffer, dt);
            urbanKernel.tick(worldBuffer, dt);
            cultureKernel.tick(worldBuffer, agentBuffer, dt);

            // 3b. Update Advanced Statistics
            currentGini = statisticsKernel.calculateGini(worldBuffer.getResourceCapital());
            densityDistribution = statisticsKernel.calculateDistribution(worldBuffer.getBiomassHuman(), 20, 1000.0f);
            currentGDP = statisticsKernel.calculateGDP(worldBuffer.getResourceCapital());
            currentLifeExpectancy = statisticsKernel.calculateLifeExpectancy(agentBuffer.getAge(), agentBuffer.getHexIds());
            currentFertility = statisticsKernel.calculateFertilityRate(agentBuffer.getBirths(), agentBuffer.getMass());

            // 4. Update agents (Legacy Units)
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
        if (worldBuffer == null) return 0;
        long total = 0;
        float[] pop = worldBuffer.getBiomassHuman();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) {
            total += (long)pop[i];
        }
        return total;
    }

    /**
     * Get total food across all cells.
     */
    public double getTotalFood() {
        if (worldBuffer == null) return 0;
        double total = 0;
        float[] food = worldBuffer.getFoodResource();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) {
            total += food[i];
        }
        return total;
    }

    /**
     * Get number of populated cells.
     */
    public long getPopulatedCellCount() {
        if (worldBuffer == null) return 0;
        long count = 0;
        float[] pop = worldBuffer.getBiomassHuman();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) {
            if (pop[i] > 0.1f) count++;
        }
        return count;
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

    public double getCurrentTPS() {
        return currentTPS;
    }

    public float getTotalBiomassNatural() {
        if (worldBuffer == null) return 0;
        float total = 0;
        float[] bio = worldBuffer.getBiomassNatural();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) total += bio[i];
        return total;
    }

    public float getAverageTechnology() {
        if (worldBuffer == null) return 0;
        float total = 0;
        float[] tech = worldBuffer.getTechnologyLevel();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) total += tech[i];
        return total / worldBuffer.getCapacity();
    }

    public float getCurrentGini() {
        return currentGini;
    }

    public int[] getDensityDistribution() {
        return densityDistribution;
    }

    public float getCurrentGDP() { return currentGDP; }
    public float getCurrentLifeExpectancy() { return currentLifeExpectancy; }
    public float getCurrentFertility() { return currentFertility; }

    private void syncClimateToBuffer() {
        if (worldBuffer == null || cells == null) return;
        float[] temps = worldBuffer.getTemperature();
        for (int i = 0; i < cells.size() && i < worldBuffer.getCapacity(); i++) {
            temps[i] = cells.get(i).getTemperature().floatValue();
        }
    }

    public org.ether.society.core.dod.WorldBuffer getWorldBuffer() {
        return worldBuffer;
    }

    public org.ether.society.core.dod.AgentBuffer getAgentBuffer() {
        return agentBuffer;
    }
}
