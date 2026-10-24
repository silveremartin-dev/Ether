/*
 * MIT License

 * Copyright (c) 2024 Gemini AI Assistant

 * Copyright (c) 2024 Silvere Martin-Michiellot

 */

package org.ether.society.core;

import org.ether.society.config.Configuration;

import org.ether.society.data.SampleDataGenerator;

import org.ether.society.database.H3Cell;

import org.ether.society.model.PhysicalConstants;

import org.ether.society.model.Scenario;

import org.ether.society.generation.PlanetPreset;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.ether.society.persistence.SimulationSaveManager;

import java.util.List;

import java.util.concurrent.Executors;

import java.util.concurrent.ScheduledExecutorService;

import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicBoolean;

/**

 * <h1>H3Simulation Engine</h1>

 * <p>

 * Core operational component for the Ether civilizational and planetary simulation framework.<br>

 * Integrates cellular dynamics, data structures, and deterministic state transitions.

 * </p>

 * 

 * @author Silvere Martin-Michiellot

 * @version 1.0.0-beta.2

 */

public class H3SimulationEngine implements ISimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(H3SimulationEngine.class);

    private final Configuration config;

    private final TimeManager timeManager;

    private final org.ether.society.events.EventSystem eventSystem;

    private final org.ether.society.analytics.HistoryManager historyManager;

    private final List<org.ether.society.database.Polity> nations = new java.util.concurrent.CopyOnWriteArrayList<>();

    private final SimulationSaveManager simulationSaveManager;

    // DOD Layer

    private final org.ether.society.core.profiling.SimulationProfiler profiler = new org.ether.society.core.profiling.SimulationProfiler();

    private org.ether.society.core.dod.WorldBuffer worldBuffer;

    private org.ether.society.core.dod.AgentBuffer agentBuffer;

    private org.ether.society.core.dod.DemographicKernel demographicKernel;

    private org.ether.society.core.dod.UrbanKernel urbanKernel;

    private org.ether.society.core.dod.CultureKernel cultureKernel;
    private final org.ether.society.core.dod.FluxKernel fluxKernel;
    private final org.ether.society.core.dod.EnvironmentalKernel environmentalKernel;
    private final org.ether.society.core.dod.StatisticsKernel statisticsKernel;
    private final SimulationPipeline simulationPipeline;
    private final SimulationMetricsAggregator metricsAggregator;

    /** Internal state: last tick time (long). */

    private long lastTickTime = 0;

    /** Internal state: current tps (double). */

    private double currentTPS = 0;

    /** Internal state: current gini (float). */

    private float currentGini = 0;

    /** Internal state: current gdp (float). */

    private float currentGDP = 0;

    /** Internal state: current life expectancy (float). */

    private float currentLifeExpectancy = 0;

    /** Internal state: current fertility (float). */

    private float currentFertility = 0;

    private int[] densityDistribution = new int[20]; // 20 bins

    // High-performance single-pass global aggregate cache (invalidated per tick)
    private long cachedTotalPopulation = -1;
    private double cachedTotalFood = -1;
    private double cachedPotableWater = -1;
    private float cachedBiomassNatural = -1;
    private float cachedAverageTechnology = -1;
    private double cachedResourceCapital = -1;
    private long lastCacheTick = -1;

    /** Internal state: cells (List&lt;H3Cell&gt;). */

    private List<H3Cell> cells;

    private Scenario currentScenario;

    private final java.util.Set<String> firedScenarioEventKeys = new java.util.HashSet<>();

    private org.ether.society.config.SimulationPerformanceConfig performanceConfig = new org.ether.society.config.SimulationPerformanceConfig(true);

    private org.ether.society.network.ClusterManager clusterManager;

    private ScheduledExecutorService executorService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final java.util.concurrent.atomic.AtomicBoolean pauseAtNextEvent = new java.util.concurrent.atomic.AtomicBoolean(false);

    /** Internal state: speed multiplier (double). */

    private double speedMultiplier = 1.0;

    /*

     * H3simulation engine.

     *

     * @param config the config parameter (Configuration)

     */

    public H3SimulationEngine(Configuration config) {

        this.config = config;

        this.timeManager = new TimeManager(config.simulation().startYear());

        this.eventSystem = new org.ether.society.events.EventSystem();

        // Initialize History Manager

        this.historyManager = new org.ether.society.analytics.HistoryManager();

        this.simulationSaveManager = new SimulationSaveManager();

        this.demographicKernel = new org.ether.society.core.dod.DemographicKernel();

        this.urbanKernel = new org.ether.society.core.dod.UrbanKernel();

        this.cultureKernel = new org.ether.society.core.dod.CultureKernel();

        this.environmentalKernel = new org.ether.society.core.dod.EnvironmentalKernel();

        this.statisticsKernel = new org.ether.society.core.dod.StatisticsKernel();

        this.fluxKernel = new org.ether.society.core.dod.FluxKernel();
        this.simulationPipeline = new SimulationPipeline(demographicKernel, urbanKernel, cultureKernel, environmentalKernel, statisticsKernel, fluxKernel);
        this.metricsAggregator = new SimulationMetricsAggregator(this);

        initialize();

    }

    /*

     * Get event system.

     *

     * @return the resulting computation or state reference

     */

    public org.ether.society.events.EventSystem getEventSystem() {
        return eventSystem;
    }

    public org.ether.society.model.ScenarioTimeline getTimeline() {
        return eventSystem != null ? eventSystem.getTimeline() : null;
    }

    public void setTimeline(org.ether.society.model.ScenarioTimeline timeline) {
        if (eventSystem != null) {
            eventSystem.setTimeline(timeline);
        }
    }

    private void initialize() {

        logger.info("Initializing H3 simulation engine (idle state)...");

        this.cells = new java.util.ArrayList<>();

        nations.clear();

        this.worldBuffer = new org.ether.society.core.dod.WorldBuffer(0);

        this.agentBuffer = new org.ether.society.core.dod.AgentBuffer(0);

    }

    /*

     * Initialize from scenario.

     *

     * @param scenario the scenario parameter (Scenario)

     * @param cells the cells parameter (List&lt;H3Cell&gt;)

     */

    public void initializeFromScenario(Scenario scenario, List<H3Cell> cells) {

        logger.info("Initializing from scenario: {}", scenario.getName());

        boolean wasRunning = running.get();

        if (wasRunning) {

            pause();

        }

        SimulationPipeline.resetGlobalSimulationState();
        this.currentScenario = scenario;
        this.performanceConfig = scenario != null ? scenario.toPerformanceConfig() : new org.ether.society.config.SimulationPerformanceConfig(true);
        try {
            String hwPref = java.util.prefs.Preferences.userNodeForPackage(org.ether.society.ui.ExecutionContextPanel.class)
                    .get("ether_hardware_mode", "JAVA_VECTOR_SIMD");
            if ("NATIVE_RUST".equalsIgnoreCase(hwPref) && org.ether.society.core.dod.NativeRustBridge.isNativeAvailable()) {
                this.performanceConfig.setExecutionEngineMode(org.ether.society.config.SimulationPerformanceConfig.ExecutionEngineMode.RUST_NATIVE);
            } else if ("CPU_JIT".equalsIgnoreCase(hwPref) || "GPU_OFF".equalsIgnoreCase(hwPref)) {
                this.performanceConfig.setExecutionEngineMode(org.ether.society.config.SimulationPerformanceConfig.ExecutionEngineMode.JAVA_STANDARD);
            } else {
                this.performanceConfig.setExecutionEngineMode(org.ether.society.config.SimulationPerformanceConfig.ExecutionEngineMode.JAVA_VECTOR_SIMD);
            }
        } catch (Throwable ignored) {}

        this.cells = cells;

        timeManager.reset((int) scenario.getStartDateYear());

        firedScenarioEventKeys.clear();

        if (eventSystem != null) {

            eventSystem.reset();

            if (scenario != null) {

                eventSystem.setSeed(scenario.getSeed());

                eventSystem.setEnableRandomEvents(scenario.isRandomEventsEnabled());

                eventSystem.setEnableHistoricalMilestones(scenario.isRandomEventsEnabled());

                eventSystem.setEnableEarthHistoricalLeaders(scenario.isEarthHistoricalLeadersEnabled());

                eventSystem.setEnableProceduralLeaders(scenario.isProceduralLeadersEnabled());

            } else {

                eventSystem.setEnableRandomEvents(true);

                eventSystem.setEnableHistoricalMilestones(true);

            }

        }

        nations.clear();

        org.ether.society.network.spatial.H3SpatialPartitioner.sortCellsByHilbertCurve(cells);

        PreComputePhase preCompute = new PreComputePhase(scenario);

        preCompute.execute(cells);

        int populatedCount = 0;
        for (H3Cell c : cells) {
            if (c != null && c.getPopulation() != null && c.getPopulation() > 0) {
                populatedCount++;
            }
        }

        initializePoliticalSeeding(populatedCount);

        int cohortSize = scenario != null && scenario.getTargetCohortSize() > 0 ? scenario.getTargetCohortSize() : 150;

        if (demographicKernel != null) {
            demographicKernel.setTargetCohortSize(cohortSize);
            long sSeed = scenario != null && scenario.getSeed() != 0L ? scenario.getSeed() : 42L;
            demographicKernel.setBaseSeed(sSeed);
        }
        if (cultureKernel != null) {
            cultureKernel.resetState();
            long sSeed = scenario != null && scenario.getSeed() != 0L ? scenario.getSeed() : 42L;
            long cSeed = scenario != null && scenario.getCulturalSeed() != 0L ? scenario.getCulturalSeed() : sSeed;
            cultureKernel.setBaseSeed(cSeed);
        }
        if (simulationPipeline != null) {
            simulationPipeline.configureScenario(scenario);
        }

        long initialPop = scenario != null ? scenario.getInitialHumanCount() : 1_000_000L;

        int estimatedAgents = (int) Math.max(cells.size(), Math.min(10_000_000L, initialPop / Math.max(1, cohortSize)));

        this.worldBuffer = new org.ether.society.core.dod.WorldBuffer(cells.size());

        int cDims = currentScenario != null ? currentScenario.getCultureVectorDimensions() : 9;
        int agentCapacity = Math.min(10_000_000, Math.max(8192, Math.max(cells.size() * 24, estimatedAgents * 8)));
        this.agentBuffer = new org.ether.society.core.dod.AgentBuffer(agentCapacity, cDims);

        org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, worldBuffer);

        long sSeed = currentScenario != null ? currentScenario.getSeed() : 42L;
        org.ether.society.data.DODDataGenerator.initializeAgentBuffer(worldBuffer, agentBuffer, cohortSize, sSeed);

        if (clusterManager != null) {

            clusterManager.setWorldBuffer(worldBuffer);

        }

        if (historyManager != null) {

            historyManager.reset();

            historyManager.captureSnapshot(this);

            historyManager.captureWorldSnapshot(this);

        }

    }

    /*

     * Get current scenario.

     *

     * @return the resulting computation or state reference

     */

    /**
     * Sets the current scenario.
     *
     * @param currentScenario the scenario to set
     */
    public void setCurrentScenario(Scenario currentScenario) {
        this.currentScenario = currentScenario;
    }
    public Scenario getCurrentScenario() {

        return currentScenario;

    }

    /*

     * Get performance config.

     *

     * @return the resulting computation or state reference

     */

    public org.ether.society.config.SimulationPerformanceConfig getPerformanceConfig() {

        return performanceConfig;

    }

    /*

     * Set performance config.

     *

     * @param performanceConfig the performance config parameter (org.ether.society.config.SimulationPerformanceConfig)

     */

    public void setPerformanceConfig(org.ether.society.config.SimulationPerformanceConfig performanceConfig) {

        this.performanceConfig = performanceConfig;

    }

    private void initializePopulation() {

        int existingCount = 0;
        for (H3Cell c : cells) {
            if (c != null && c.getPopulation() != null && c.getPopulation() > 0) {
                existingCount++;
            }
        }

        if (existingCount > 0) {
            initializePoliticalSeeding(existingCount);
            return;
        }

        int populatedCells = 0;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            org.ether.society.model.Biome biome = cell.getBiome();
            if (biome == null || biome == org.ether.society.model.Biome.OCEAN || biome == org.ether.society.model.Biome.DEEP_OCEAN) {
                continue;
            }

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

            long cellId = cell.getH3Index() != null ? cell.getH3Index() : 0L;
            long sSeed = currentScenario != null && currentScenario.getSeed() != 0L ? currentScenario.getSeed() : 42L;
            if (org.ether.society.util.DeterministicPRNG.nextDouble(sSeed, 0L, cellId, 503L) < 0.1 && basePop > 0) {
                cell.setPopulation(basePop);
                populatedCells++;
            }

        }

        initializePoliticalSeeding(populatedCells);

    }

    @Override

    /*

     * Start.

     *

     */

    public void start() {

        if (running.getAndSet(true)) return;

        startGameLoop();

    }

    @Override

    /*

     * Pause.

     *

     */

    public void pause() {
        if (!running.getAndSet(false)) return;
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(200, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override

    /*

     * Reset.

     *

     */

    public void reset() {
        pause();
        SimulationPipeline.resetGlobalSimulationState();
        firedScenarioEventKeys.clear();

        timeManager.reset(config.simulation().startYear());

        historyManager.reset();

        initialize();

    }

    @Override

    /*

     * Set speed.

     *

     * @param multiplier the multiplier parameter (int)

     */

    public void setSpeed(int multiplier) {

        setSpeed((double) multiplier);

    }

    @Override

    /*

     * Set speed.

     *

     * @param multiplier the multiplier parameter (double)

     */

    public void setSpeed(double multiplier) {

        this.speedMultiplier = Math.max(0.01, multiplier);

        if (running.get()) {

            pause();

            start();

        }

    }

    @Override

    /*

     * Get speed.

     *

     * @return the resulting computation or state reference

     */

    public int getSpeed() {

        return (int) Math.round(speedMultiplier);

    }

    @Override

    /*

     * Get speed multiplier.

     *

     * @return the resulting computation or state reference

     */

    public double getSpeedMultiplier() {

        return speedMultiplier;

    }

    @Override

    /*

     * Get time manager.

     *

     * @return the resulting computation or state reference

     */

    public TimeManager getTimeManager() {

        return timeManager;

    }

    /*

     * Get current year.

     *

     * @return the resulting computation or state reference

     */

    public long getCurrentYear() {

        return timeManager != null ? timeManager.getCurrentYear() : 0;

    }

    @Override

    /*

     * Is running.

     *

     * @return the resulting computation or state reference

     */

    public boolean isRunning() {

        return running.get();

    }

    @Override

    /*

     * Set pause at next event.

     *

     * @param pause the pause parameter (boolean)

     */

    public void setPauseAtNextEvent(boolean pause) {

        this.pauseAtNextEvent.set(pause);

    }

    @Override

    /*

     * Is pause at next event.

     *

     * @return the resulting computation or state reference

     */

    public boolean isPauseAtNextEvent() {

        return this.pauseAtNextEvent.get();

    }

    /*

     * Get cells.

     *

     * @return the resulting computation or state reference

     */

    public List<H3Cell> getCells() {

        return cells;

    }

    /*

     * Set cells.

     *

     * @param newCells the new cells parameter (List&lt;H3Cell&gt;)

     */

    public void setCells(List<H3Cell> newCells) {

        boolean wasRunning = running.get();

        if (wasRunning) pause();

        this.cells = newCells;

        nations.clear();

        if (newCells != null && !newCells.isEmpty()) {

            if (currentScenario != null) {

                PreComputePhase preCompute = new PreComputePhase(currentScenario);

                preCompute.execute(newCells);

            } else {

                initializePopulation();

            }

            int cohortSize = currentScenario != null && currentScenario.getTargetCohortSize() > 0 ? currentScenario.getTargetCohortSize() : 150;

            if (demographicKernel != null) {

                demographicKernel.setTargetCohortSize(cohortSize);

            }

            long initialPop = currentScenario != null ? currentScenario.getInitialHumanCount() : 1_000_000L;

            int estimatedAgents = (int) Math.max(cells.size(), Math.min(10_000_000L, initialPop / Math.max(1, cohortSize)));

            this.worldBuffer = new org.ether.society.core.dod.WorldBuffer(cells.size());

            int cDims = currentScenario != null ? currentScenario.getCultureVectorDimensions() : 9;
            int agentCapacity = Math.min(10_000_000, Math.max(8192, Math.max(cells.size() * 24, estimatedAgents * 8)));
            this.agentBuffer = new org.ether.society.core.dod.AgentBuffer(agentCapacity, cDims);

            org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, worldBuffer);

            long sSeed = currentScenario != null ? currentScenario.getSeed() : 42L;
            org.ether.society.data.DODDataGenerator.initializeAgentBuffer(worldBuffer, agentBuffer, cohortSize, sSeed);

        } else {

            this.worldBuffer = new org.ether.society.core.dod.WorldBuffer(0);

            this.agentBuffer = new org.ether.society.core.dod.AgentBuffer(0);

        }

    }

    /*

     * Save simulation.

     *

     * @param saveName the save name parameter (String)

     */

    public void saveSimulation(String saveName) {
        saveSimulation(null, saveName);
    }

    public void saveSimulation(String saveId, String saveName) {
        boolean wasRunning = running.get();
        if (wasRunning) pause();

        simulationSaveManager.saveSimulation(this, saveId, saveName);

        if (wasRunning) start();
    }

    /*

     * Load simulation.

     *

     * @param saveId the save id parameter (String)

     */

    public void loadSimulation(String saveId) {

        boolean wasRunning = running.get();

        if (wasRunning) pause();

        simulationSaveManager.loadSimulation(saveId, this);

    }

    private void startGameLoop() {

        if (executorService != null && !executorService.isShutdown()) {

            executorService.shutdownNow();

        }

        executorService = Executors.newSingleThreadScheduledExecutor(r -> {

            Thread t = new Thread(r, "H3SimulationEngine-Thread");

            t.setDaemon(true);

            return t;

        });

        if (speedMultiplier >= 999.0) {

            // MAX speed: continuous non-accumulating loop with minimum fixed delay (1 ms)

            executorService.scheduleWithFixedDelay(this::tick, 0, 1, TimeUnit.MILLISECONDS);

        } else {

            long delay = Math.max(1L, Math.round(config.simulation().tickRateMs() / Math.max(0.01, speedMultiplier)));

            executorService.scheduleWithFixedDelay(this::tick, 0, delay, TimeUnit.MILLISECONDS);

        }

    }

    /*

     * Shutdown.

     *

     */

    public void shutdown() {

        running.set(false);

        if (executorService != null && !executorService.isShutdown()) {

            executorService.shutdownNow();

        }

        if (saveExecutor != null && !saveExecutor.isShutdown()) {

            saveExecutor.shutdownNow();

        }

    }

    private Runnable onTickCallback;

    /*

     * Set cluster manager.

     *

     * @param clusterManager the cluster manager parameter (org.ether.society.network.ClusterManager)

     */

    public void setClusterManager(org.ether.society.network.ClusterManager clusterManager) {

        this.clusterManager = clusterManager;

        if (this.clusterManager != null && this.worldBuffer != null) {

            this.clusterManager.setWorldBuffer(this.worldBuffer);

        }

    }

    /*

     * Get cluster manager.

     *

     * @return the resulting computation or state reference

     */

    public org.ether.society.network.ClusterManager getClusterManager() {

        return clusterManager;

    }

    /*

     * Set on tick callback.

     *

     * @param callback the callback parameter (Runnable)

     */

    public void setOnTickCallback(Runnable callback) {

        this.onTickCallback = callback;

    }

    /*

     * Get profiler.

     *

     * @return the resulting computation or state reference

     */

    public org.ether.society.core.profiling.SimulationProfiler getProfiler() {

        return profiler;

    }

    /** Internal state: tick counter (int). */

    private int tickCounter = 0;

    /*

     * Get tick counter.

     *

     * @return the resulting computation or state reference

     */

    public long getTickCounter() {

        return tickCounter;

    }

    public enum TemporalScale {

        DAILY(1, "📅 Pas Quotidien (Jour par Jour - Détaillé)"),

        MONTHLY(30, "🚀 Pas Mensuel (Mois par Mois - Mode Rapide)");

        /** Internal state: factor (int). */

        private final int factor;

        /** Internal state: label (String). */

        private final String label;

        TemporalScale(int factor, String label) {

            this.factor = factor;

            this.label = label;

        }

        /*

         * Get factor.

         *

         * @return the resulting computation or state reference

         */

        public int getFactor() { return factor; }

        /*

         * Get label.

         *

         * @return the resulting computation or state reference

         */

        public String getLabel() { return label; }

        @Override

        /*

         * To string.

         *

         * @return the resulting computation or state reference

         */

        public String toString() {

            return label;

        }

    }

    private TemporalScale temporalScale = TemporalScale.DAILY;

    /*

     * Get temporal scale.

     *

     * @return the resulting computation or state reference

     */

    public TemporalScale getTemporalScale() { return temporalScale; }

    /*

     * Set temporal scale.

     *

     * @param scale the scale parameter (TemporalScale)

     */

    public void setTemporalScale(TemporalScale scale) {

        if (scale != null) {

            this.temporalScale = scale;

            logger.info("Temporal scale updated to: {}", scale);

        }

    }

    private void tick() {

        if (!running.get()) return;

        long tickStartNanos = System.nanoTime();

        if (lastTickTime != 0) {

            double diff = (tickStartNanos - lastTickTime) / 1_000_000_000.0;

            if (diff > 0) {

                double instantTPS = 1.0 / diff;

                currentTPS = (currentTPS <= 0) ? instantTPS : (currentTPS * 0.90 + instantTPS * 0.10);

            }

        }

        lastTickTime = tickStartNanos;

        try {

            int stepDays = (currentScenario != null && currentScenario.getTemporalResolutionDays() > 0)

                    ? (int) Math.round(currentScenario.getTemporalResolutionDays()) : 1;

            stepDays = Math.max(1, stepDays);

            final float DT_TICK = stepDays * 86400f; // Delta time for this tick in seconds

            // 1. FAST SCALE DYNAMICS (Scaled to stepDays)

            int yearBeforeTick = timeManager.getCurrentYear();
            timeManager.advanceDays(stepDays);
            boolean yearChanged = (timeManager.getCurrentYear() != yearBeforeTick);
            profiler.endPhase("1_FastScaleFlux");

            // 2. SLOW SCALE PHYSICALIST DYNAMICS

            // Triggered every tick when strictDeterminism is true or multi-frequency climate ticks are disabled
            boolean multiFreq = performanceConfig != null
                    && performanceConfig.isEnableMultiFreqClimateTicks()
                    && !performanceConfig.isStrictDeterminism();
            int slowModulo = multiFreq ? Math.max(1, 30 / stepDays) : 1;
            boolean runSlowScale = (stepDays >= 30) || (tickCounter % slowModulo == 0);

            if (runSlowScale) {

                if (currentScenario != null && timeManager.getCurrentYear() > currentScenario.getEndDateYear()) {

                    logger.info("Simulation reached scenario target end date (Year {}). Auto-pausing.", currentScenario.getEndDateYear());

                    if (historyManager != null) {
                        historyManager.captureSnapshot(this);
                        historyManager.captureWorldSnapshot(this);
                    }
                    pause();

                    return;

                }

                int month = timeManager.getCurrentMonth();

                float dtSlow = multiFreq ? Math.max(DT_TICK, slowModulo * DT_TICK) : DT_TICK;

                simulationPipeline.executeTick(

                        this, cells, worldBuffer, agentBuffer,

                        profiler, performanceConfig, (long) dtSlow, getAverageTechnology()

                );

                // Statistics & Snapshots

                profiler.beginPhase("5_StatisticsAndHistory");
                updateGlobalAggregatesCache();

                currentGini = statisticsKernel.calculateGini(worldBuffer.getResourceCapital());

                densityDistribution = statisticsKernel.calculateDistribution(worldBuffer.getBiomassHuman(), 20, 1000.0f);

                currentGDP = statisticsKernel.calculateGDP(worldBuffer.getResourceCapital());

                float foodPerCapVal = (float) getFoodPerCapita();

                float foodSatVal = Math.min(1.0f, foodPerCapVal > 0 ? foodPerCapVal / 2.0f : 1.0f);

                currentLifeExpectancy = statisticsKernel.calculateLifeExpectancy(agentBuffer.getAge(), agentBuffer.getHexIds(), getAverageTechnology(), foodSatVal);

                currentFertility = statisticsKernel.calculateFertilityRate(agentBuffer.getBirths(), agentBuffer.getMass());

                historyManager.captureSnapshot(this);

                if (historyManager != null && historyManager.shouldCaptureWorldSnapshot(tickCounter, false, yearChanged)) {
                    historyManager.captureWorldSnapshot(this);
                }

                int eventCountBefore = eventSystem.peekEvents().size();

                checkScenarioClimateEvents();

                eventSystem.checkEvents(timeManager.getCurrentYear(), timeManager.getCurrentMonth(), getTotalPopulation(), getTotalFood(), cells);

                eventSystem.checkCellEvents(timeManager.getCurrentYear(), timeManager.getCurrentMonth(), cells);

                int eventCountAfter = eventSystem.peekEvents().size();

                if (pauseAtNextEvent.get() && eventCountAfter > eventCountBefore) {

                    logger.info("[Step {}] Automatic simulation pause on event (pauseAtNextEvent=true)", tickCounter);

                    pause();

                }

                profiler.endPhase("5_StatisticsAndHistory");

                profiler.beginPhase("6_BufferSync");
                // Cells are already synchronized with WorldBuffer from Phase 2 (syncCellsToBuffer).
                // Phase 6 is preserved as a lightweight boundary phase avoiding redundant O(N) object traversals.
                profiler.endPhase("6_BufferSync");

                if (tickCounter % Math.max(1, 12 / stepDays) == 0) {

                    logger.info("[Step {}] Year {} Month {} | Pop: {} | TPS: {} it/s",

                            tickCounter, timeManager.getCurrentYear(),

                            String.format("%02d", timeManager.getCurrentMonth() + 1),

                            String.format("%,d", getTotalPopulation()),

                            String.format("%.1f", currentTPS));

                }

            }

            // Every 60 Ticks: trigger rolling checkpoint auto-save

            if (tickCounter > 0 && tickCounter % 60 == 0) {

                autoSaveCheckpoint();

            }

            tickCounter++;

            long tickDuration = System.nanoTime() - tickStartNanos;

            long renderStartNanos = System.nanoTime();

            if (onTickCallback != null) {

                onTickCallback.run();

            }

            long renderDuration = System.nanoTime() - renderStartNanos;

            profiler.recordTick(tickDuration, renderDuration);

        } catch (Exception e) {

            logger.error("Error during simulation tick", e);

        }

    }

    private void checkScenarioClimateEvents() {

        if (currentScenario == null || currentScenario.getClimateEvents() == null || currentScenario.getClimateEvents().isEmpty()) return;

        int currentYear = timeManager.getCurrentYear();

        for (org.ether.society.model.ClimateEvent evt : currentScenario.getClimateEvents()) {

            if (evt.getYear() == currentYear) {

                String eventKey = evt.getName() + "_" + evt.getYear() + "_" + evt.getType();

                if (!firedScenarioEventKeys.contains(eventKey)) {

                    firedScenarioEventKeys.add(eventKey);

                    String type = evt.getType() != null ? evt.getType().toLowerCase() : "";

                    String displayTitle;

                    if (type.contains("milestone") || type.contains("hist")) {

                        displayTitle = "📜 REPÈRE : " + evt.getName();

                    } else if (type.contains("nuclear") || type.contains("strike")) {

                        displayTitle = "☢️ FRAPPE NUCLÉAIRE : " + evt.getName() + " (Mag: " + evt.getMagnitude() + ")";

                    } else {

                        displayTitle = "🌋 ÉVÉNEMENT : " + evt.getName() + " (" + evt.getType() + " - Mag: " + evt.getMagnitude() + ")";

                    }

                    org.ether.society.events.ActiveEvent ae = new org.ether.society.events.ActiveEvent(

                        "SCENARIO_EVT_" + currentYear + "_" + tickCounter,

                        displayTitle,

                        evt.getType().toUpperCase(),

                        evt.getLatitude(), evt.getLongitude(),

                        currentYear, timeManager.getCurrentMonth(), 1, 25.0, evt.getMagnitude()

                    );

                    eventSystem.recordSpatialEvent(ae);

                    logger.info("🌋 [Tick {}] Scenario event triggered: {} (Year {})", tickCounter, evt.getName(), currentYear);

                    applyClimateEventImpact(evt);

                }

            }

        }

    }

    private void applyClimateEventImpact(org.ether.society.model.ClimateEvent evt) {

        if (cells == null || cells.isEmpty()) return;

        double evtLat = evt.getLatitude();

        double evtLng = evt.getLongitude();

        double mag = evt.getMagnitude();

        int totalCells = cells.size();
        float[] cellSurvival = null;
        boolean anyAgentMortality = false;

        for (int i = 0; i < totalCells; i++) {
            H3Cell cell = cells.get(i);
            if (cell == null) continue;
            double cLat = cell.getLatitude() != null ? cell.getLatitude() : 0.0;
            double cLng = cell.getLongitude() != null ? cell.getLongitude() : 0.0;
            double distDeg = Math.hypot(cLat - evtLat, cLng - evtLng);

            double impactRadius = Math.max(5.0, mag * 3.0);
            if (distDeg <= impactRadius) {
                double attenuation = 1.0 - (distDeg / impactRadius);
                String type = evt.getType() != null ? evt.getType().toLowerCase() : "";
                int lost = 0;
                int prevPop = cell.getPopulation() != null ? cell.getPopulation() : 0;

                if (type.contains("nuclear") || type.contains("strike")) {
                    double cooling = -0.6 * mag * attenuation;
                    cell.setTemperature(Math.max(-50.0, (cell.getTemperature() != null ? cell.getTemperature() : 15.0) + cooling));
                    if (prevPop > 0) {
                        double mortalityFrac = Math.min(0.95, 0.85 * attenuation);
                        lost = (int) Math.round(prevPop * mortalityFrac);
                        cell.setPopulation(Math.max(0, prevPop - lost));
                    }
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-1.2 * attenuation)));
                    }
                    if (cell.getFoodResource() != null) {
                        cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-1.6 * attenuation)));
                    }
                    cell.setPollutionLevel(Math.min(1.0, (cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0) + 0.8 * attenuation));
                } else if (type.contains("volcano") || type.contains("ice")) {
                    double cooling = -0.5 * mag * attenuation;
                    cell.setTemperature(Math.max(-50.0, (cell.getTemperature() != null ? cell.getTemperature() : 15.0) + cooling));
                } else if (type.contains("cyclone") || type.contains("hurricane") || type.contains("storm") || type.contains("flood")) {
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.15 * mag * attenuation)));
                    }
                    if (cell.getFoodResource() != null) {
                        cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-0.25 * mag * attenuation)));
                    }
                    if (prevPop > 0) {
                        double survival = Math.exp(-0.04 * mag * attenuation);
                        lost = (int) Math.round(prevPop * (1.0 - survival));
                        cell.setPopulation(Math.max(0, prevPop - lost));
                    }
                } else if (type.contains("epidemic") || type.contains("pandemic") || type.contains("plague")) {
                    if (prevPop > 0) {
                        double survival = Math.exp(-0.15 * mag * attenuation);
                        lost = (int) Math.round(prevPop * (1.0 - survival));
                        cell.setPopulation(Math.max(0, prevPop - lost));
                    }
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.05 * mag * attenuation)));
                    }
                } else if (type.contains("earthquake") || type.contains("tsunami") || type.contains("meteor")) {
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.08 * mag * attenuation)));
                    }
                    if (prevPop > 0) {
                        double survival = Math.exp(-0.05 * mag * attenuation);
                        lost = (int) Math.round(prevPop * (1.0 - survival));
                        cell.setPopulation(Math.max(0, prevPop - lost));
                    }
                } else if (type.contains("famine") || type.contains("drought")) {
                    if (cell.getFoodResource() != null) {
                        cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-0.10 * mag * attenuation)));
                    }
                }

                // Synchronize changes to DOD buffers so downstream kernels and syncBufferToCells persist impacts
                if (worldBuffer != null && i < worldBuffer.getCapacity()) {
                    if (cell.getTemperature() != null) {
                        worldBuffer.getTemperature()[i] = cell.getTemperature().floatValue();
                    }
                    if (cell.getResourceCapital() != null) {
                        worldBuffer.getResourceCapital()[i] = cell.getResourceCapital().floatValue();
                    }
                    if (cell.getFoodResource() != null) {
                        worldBuffer.getFoodResource()[i] = cell.getFoodResource().floatValue();
                    }
                    if (cell.getPopulation() != null) {
                        worldBuffer.getBiomassHuman()[i] = cell.getPopulation().floatValue();
                    }
                }

                if (agentBuffer != null && lost > 0 && prevPop > 0) {
                    if (cellSurvival == null) {
                        cellSurvival = new float[totalCells];
                        java.util.Arrays.fill(cellSurvival, 1.0f);
                    }
                    float mortalityFraction = Math.clamp((float) lost / Math.max(1.0f, (float) prevPop), 0.0f, 1.0f);
                    cellSurvival[i] = 1.0f - mortalityFraction;
                    anyAgentMortality = true;
                }
            }
        }

        // Single O(N_agents) pass instead of O(N_impactedCells * N_agents) nested iteration
        if (anyAgentMortality && agentBuffer != null && cellSurvival != null) {
            int cap = agentBuffer.getCapacity();
            int[] hexIds = agentBuffer.getHexIds();
            float[] mass = agentBuffer.getMass();
            float[] deaths = agentBuffer.getDeaths();
            for (int a = 0; a < cap; a++) {
                int hId = hexIds[a];
                if (hId >= 0 && hId < totalCells && mass[a] > 0.0f) {
                    float surv = cellSurvival[hId];
                    if (surv < 1.0f) {
                        float oldM = mass[a];
                        float newM = oldM * surv;
                        mass[a] = newM;
                        deaths[a] += (oldM - newM);
                    }
                }
            }
        }

    }

    @Override

    /*

     * Step forward.

     *

     * @param ticks the ticks parameter (int)

     */

    public void stepForward(int ticks) {
        pause();
        for (int i = 0; i < Math.max(1, ticks); i++) {
            boolean wasRunning = running.getAndSet(true);
            try {
                tick();
            } finally {
                running.set(wasRunning);
            }
        }
        if (historyManager != null) {
            historyManager.captureWorldSnapshot(this);
            historyManager.flushSnapshots();
        }
    }

    @Override

    /*

     * Step backward.

     *

     * @param ticks the ticks parameter (int)

     */

    public void stepBackward(int ticks) {

        pause();

        if (historyManager == null || cells == null || cells.isEmpty()) return;

        long currentTicks = timeManager.getTotalTicks();

        long targetTicks = Math.max(0, currentTicks - ticks);

        seekToTick(targetTicks);

    }

    @Override

    /*

     * Seek to end.

     *

     */

    public void seekToEnd() {

        pause();

        if (historyManager != null && !historyManager.getWorldSnapshots().isEmpty()) {

            long lastTick = historyManager.getWorldSnapshots().lastKey();

            seekToTick(lastTick);

        } else if (currentScenario != null) {

            long initialYear = currentScenario.getStartDateYear();

            long endYear = currentScenario.getEndDateYear();

            int stepDaysVal = (currentScenario.getTemporalResolutionDays() > 0)

                    ? (int) Math.round(currentScenario.getTemporalResolutionDays()) : 1;

            long totalDays = Math.max(0, (endYear - initialYear) * 365L);

            long maxTicks = totalDays / stepDaysVal;

            seekToTick(maxTicks);

        }

    }

    /*

     * Seek to tick.

     *

     * @param targetTicks the target ticks parameter (long)

     */

    public void seekToTick(long targetTicks) {

        pause();

        if (historyManager == null || cells == null || cells.isEmpty()) return;

        java.util.NavigableMap<Long, List<H3Cell>> snapshots = historyManager.getWorldSnapshots();

        if (!snapshots.isEmpty()) {

            java.util.Map.Entry<Long, List<H3Cell>> entry = snapshots.floorEntry(targetTicks);

            if (entry == null) {

                entry = snapshots.firstEntry();

            }

            if (entry != null && entry.getValue() != null) {

                List<H3Cell> snapshot = entry.getValue();

                java.util.Map<Long, H3Cell> map = snapshot.stream()
                        .filter(c -> c != null && c.getH3Index() != null)
                        .collect(java.util.stream.Collectors.toMap(H3Cell::getH3Index, c -> c, (a, b) -> a));
                // Iterate over spatial cell domains and apply localized cellular state transformations
                for (H3Cell c : cells) {
                    if (c == null || c.getH3Index() == null) continue;
                    H3Cell snap = map.get(c.getH3Index());

                    if (snap != null) {

                        c.setPopulation(snap.getPopulation());

                        c.setTemperature(snap.getTemperature());

                        c.setFoodResource(snap.getFoodResource());

                        c.setWaterResource(snap.getWaterResource());

                        c.setFreshwaterAquifer(snap.getFreshwaterAquifer());

                        c.setTechnologyLevel(snap.getTechnologyLevel());

                        c.setBiomassHuman(snap.getBiomassHuman());

                        c.setBiomassNatural(snap.getBiomassNatural());

                        c.setBiomassLivestock(snap.getBiomassLivestock());

                        c.setBiomassAgriculture(snap.getBiomassAgriculture());

                        c.setResourceCapital(snap.getResourceCapital());

                        c.setPollutionLevel(snap.getPollutionLevel());

                    }

                }

                if (worldBuffer != null) {

                    org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, worldBuffer);

                }

            }

        }

        // Compute new year, month, and day based on targetTicks

        long initialYear = currentScenario != null ? currentScenario.getStartDateYear() : -20000;

        int stepDaysVal = (currentScenario != null && currentScenario.getTemporalResolutionDays() > 0)

                ? (int) Math.round(currentScenario.getTemporalResolutionDays()) : 1;

        long totalDays = targetTicks * stepDaysVal;

        long totalMonths = totalDays / 30;

        int targetYear = (int) (initialYear + (totalMonths / 12));

        int targetMonth = (int) (totalMonths % 12);

        int targetDay = (int) ((totalDays % 30) + 1);

        timeManager.setTime(targetYear, targetMonth, targetDay, targetTicks);

        this.tickCounter = (int) targetTicks;

        // Recalculate metrics for the current time

        if (worldBuffer != null && statisticsKernel != null) {

            currentGini = statisticsKernel.calculateGini(worldBuffer.getResourceCapital());

            densityDistribution = statisticsKernel.calculateDistribution(worldBuffer.getBiomassHuman(), 20, 1000.0f);

            currentGDP = statisticsKernel.calculateGDP(worldBuffer.getResourceCapital());

            if (agentBuffer != null) {

                currentLifeExpectancy = statisticsKernel.calculateLifeExpectancy(agentBuffer.getAge(), agentBuffer.getHexIds());

                currentFertility = statisticsKernel.calculateFertilityRate(agentBuffer.getBirths(), agentBuffer.getMass());

            }

        }

    }

    private final java.util.concurrent.ExecutorService saveExecutor = Executors.newSingleThreadExecutor(r -> {

        Thread t = new Thread(r, "Ether-AutoSave-Thread");

        t.setDaemon(true);

        return t;

    });

    /** Internal state: last auto save time ms (long). */

    private long lastAutoSaveTimeMs = 0;

    private void autoSaveCheckpoint() {

        if (cells == null || cells.isEmpty()) return;

        long now = System.currentTimeMillis();

        if (now - lastAutoSaveTimeMs < 30_000L) {

            return;

        }

        lastAutoSaveTimeMs = now;

        final int currentTick = tickCounter;

        saveExecutor.submit(() -> {

            try {

                simulationSaveManager.saveCheckpoint(this, currentTick);

            } catch (Exception ex) {

                logger.error("Failed to save periodic checkpoint", ex);

            }

        });

    }

    /*

     * Sync buffer to cells.

     *

     */

    public void syncBufferToCells() {
        if (worldBuffer == null || cells == null) return;
        float[] pop = worldBuffer.getBiomassHuman();
        float[] food = worldBuffer.getFoodResource();
        float[] tech = worldBuffer.getTechnologyLevel();
        float[] water = worldBuffer.getWaterResource();
        float[] wood = worldBuffer.getWoodResource();
        float[] gini = worldBuffer.getGiniIndex();
        float[] capital = worldBuffer.getResourceCapital();
        byte[] biomes = worldBuffer.getBiomes();

        org.ether.society.model.Biome[] biomeValues = org.ether.society.model.Biome.VALUES;
        int limit = Math.min(cells.size(), worldBuffer.getCapacity());

        boolean parallel = limit >= 10000 && performanceConfig != null && (performanceConfig.isEnableParallelExecution() || performanceConfig.getParallelThreadCount() > 1);
        if (parallel) {
            java.util.stream.IntStream.range(0, limit).parallel().forEach(i -> {
                H3Cell cell = cells.get(i);
                if (cell == null) return;
                int p = Math.max(0, Math.round(pop[i]));
                cell.setPopulation(p);
                cell.setBiomassHuman((double) pop[i]);
                cell.setFoodResource((double) food[i]);
                cell.setTechnologyLevel((double) tech[i]);
                cell.setWaterResource((double) water[i]);
                cell.setWoodResource((double) wood[i]);
                cell.setGiniIndex((double) gini[i]);
                if (capital != null) cell.setResourceCapital((double) capital[i]);
                if (worldBuffer.getMetalResource() != null) cell.setResourceMetal((double) worldBuffer.getMetalResource()[i]);
                if (worldBuffer.getClayResource() != null) cell.setResourceClay((double) worldBuffer.getClayResource()[i]);
                if (worldBuffer.getBiomassAgriculture() != null) cell.setBiomassAgriculture((double) worldBuffer.getBiomassAgriculture()[i]);
                if (worldBuffer.getBiomassNatural() != null) cell.setBiomassNatural((double) worldBuffer.getBiomassNatural()[i]);
                if (worldBuffer.getBiomassFish() != null) cell.setBiomassFish((double) worldBuffer.getBiomassFish()[i]);

                if (cell.getBiome() != null) {
                    biomes[i] = (byte) cell.getBiome().ordinal();
                } else {
                    int bIdx = biomes[i] & 0xFF;
                    if (bIdx < biomeValues.length) {
                        cell.setBiome(biomeValues[bIdx]);
                    }
                }
            });
        } else {
            for (int i = 0; i < limit; i++) {
                H3Cell cell = cells.get(i);
                if (cell == null) continue;
                int p = Math.max(0, Math.round(pop[i]));
                cell.setPopulation(p);
                cell.setBiomassHuman((double) pop[i]);
                cell.setFoodResource((double) food[i]);
                cell.setTechnologyLevel((double) tech[i]);
                cell.setWaterResource((double) water[i]);
                cell.setWoodResource((double) wood[i]);
                cell.setGiniIndex((double) gini[i]);
                if (capital != null) cell.setResourceCapital((double) capital[i]);
                if (worldBuffer.getMetalResource() != null) cell.setResourceMetal((double) worldBuffer.getMetalResource()[i]);
                if (worldBuffer.getClayResource() != null) cell.setResourceClay((double) worldBuffer.getClayResource()[i]);
                if (worldBuffer.getBiomassAgriculture() != null) cell.setBiomassAgriculture((double) worldBuffer.getBiomassAgriculture()[i]);
                if (worldBuffer.getBiomassNatural() != null) cell.setBiomassNatural((double) worldBuffer.getBiomassNatural()[i]);
                if (worldBuffer.getBiomassFish() != null) cell.setBiomassFish((double) worldBuffer.getBiomassFish()[i]);

                if (cell.getBiome() != null) {
                    biomes[i] = (byte) cell.getBiome().ordinal();
                } else {
                    int bIdx = biomes[i] & 0xFF;
                    if (bIdx < biomeValues.length) {
                        cell.setBiome(biomeValues[bIdx]);
                    }
                }
            }
        }
    }

    /**
     * Synchronizes domain states from individual H3Cell objects back into the high-performance WorldBuffer DOD arrays.
     * Enforces continuous state coherence between object-oriented tier 1/2 engines and vectorized DOD kernels.
     */
    public void syncCellsToBuffer() {
        if (worldBuffer == null || cells == null) return;

        float[] pop = worldBuffer.getBiomassHuman();
        float[] food = worldBuffer.getFoodResource();
        float[] tech = worldBuffer.getTechnologyLevel();
        float[] water = worldBuffer.getWaterResource();
        float[] wood = worldBuffer.getWoodResource();
        float[] metal = worldBuffer.getMetalResource();
        float[] clay = worldBuffer.getClayResource();
        float[] gini = worldBuffer.getGiniIndex();
        float[] capital = worldBuffer.getResourceCapital();
        float[] lifespan = worldBuffer.getLifespan();
        float[] fertility = worldBuffer.getFertility();
        float[] biomassAgri = worldBuffer.getBiomassAgriculture();
        float[] biomassNat = worldBuffer.getBiomassNatural();
        float[] biomassFish = worldBuffer.getBiomassFish();
        byte[] biomes = worldBuffer.getBiomes();

        int limit = Math.min(cells.size(), worldBuffer.getCapacity());
        boolean popModified = false;
        boolean[] cellPopModified = new boolean[limit];

        boolean parallel = limit >= 10000 && performanceConfig != null && (performanceConfig.isEnableParallelExecution() || performanceConfig.getParallelThreadCount() > 1);
        if (parallel) {
            java.util.stream.IntStream.range(0, limit).parallel().forEach(i -> {
                H3Cell cell = cells.get(i);
                if (cell == null) return;
                if (pop != null && cell.getPopulation() != null) {
                    int cellIntPop = cell.getPopulation();
                    int bufferRoundedPop = Math.max(0, Math.round(pop[i]));
                    if (cellIntPop != bufferRoundedPop) {
                        int delta = cellIntPop - bufferRoundedPop;
                        pop[i] = Math.max(0.0f, pop[i] + delta);
                        cellPopModified[i] = true;
                    }
                }
                if (food != null && cell.getFoodResource() != null) food[i] = cell.getFoodResource().floatValue();
                if (tech != null && cell.getTechnologyLevel() != null) tech[i] = cell.getTechnologyLevel().floatValue();
                if (water != null && cell.getWaterResource() != null) water[i] = cell.getWaterResource().floatValue();
                if (wood != null && cell.getWoodResource() != null) wood[i] = cell.getWoodResource().floatValue();
                if (metal != null && cell.getResourceMetal() != null) metal[i] = cell.getResourceMetal().floatValue();
                if (clay != null && cell.getResourceClay() != null) clay[i] = cell.getResourceClay().floatValue();
                if (gini != null && cell.getGiniIndex() != null) gini[i] = cell.getGiniIndex().floatValue();
                if (capital != null && cell.getResourceCapital() != null) capital[i] = cell.getResourceCapital().floatValue();
                if (lifespan != null && cell.getLifespan() != null) lifespan[i] = cell.getLifespan().floatValue();
                if (fertility != null && cell.getFertility() != null) fertility[i] = cell.getFertility().floatValue();
                if (biomassAgri != null && cell.getBiomassAgriculture() != null) biomassAgri[i] = cell.getBiomassAgriculture().floatValue();
                if (biomassNat != null && cell.getBiomassNatural() != null) biomassNat[i] = cell.getBiomassNatural().floatValue();
                if (biomassFish != null && cell.getBiomassFish() != null) biomassFish[i] = cell.getBiomassFish().floatValue();
                if (biomes != null && cell.getBiome() != null) biomes[i] = (byte) cell.getBiome().ordinal();
            });
            for (int i = 0; i < limit; i++) {
                if (cellPopModified[i]) {
                    popModified = true;
                    break;
                }
            }
        } else {
            for (int i = 0; i < limit; i++) {
                H3Cell cell = cells.get(i);
                if (cell == null) continue;
                if (pop != null && cell.getPopulation() != null) {
                    int cellIntPop = cell.getPopulation();
                    int bufferRoundedPop = Math.max(0, Math.round(pop[i]));
                    if (cellIntPop != bufferRoundedPop) {
                        int delta = cellIntPop - bufferRoundedPop;
                        pop[i] = Math.max(0.0f, pop[i] + delta);
                        cellPopModified[i] = true;
                        popModified = true;
                    }
                }
                if (food != null && cell.getFoodResource() != null) food[i] = cell.getFoodResource().floatValue();
                if (tech != null && cell.getTechnologyLevel() != null) tech[i] = cell.getTechnologyLevel().floatValue();
                if (water != null && cell.getWaterResource() != null) water[i] = cell.getWaterResource().floatValue();
                if (wood != null && cell.getWoodResource() != null) wood[i] = cell.getWoodResource().floatValue();
                if (metal != null && cell.getResourceMetal() != null) metal[i] = cell.getResourceMetal().floatValue();
                if (clay != null && cell.getResourceClay() != null) clay[i] = cell.getResourceClay().floatValue();
                if (gini != null && cell.getGiniIndex() != null) gini[i] = cell.getGiniIndex().floatValue();
                if (capital != null && cell.getResourceCapital() != null) capital[i] = cell.getResourceCapital().floatValue();
                if (lifespan != null && cell.getLifespan() != null) lifespan[i] = cell.getLifespan().floatValue();
                if (fertility != null && cell.getFertility() != null) fertility[i] = cell.getFertility().floatValue();
                if (biomassAgri != null && cell.getBiomassAgriculture() != null) biomassAgri[i] = cell.getBiomassAgriculture().floatValue();
                if (biomassNat != null && cell.getBiomassNatural() != null) biomassNat[i] = cell.getBiomassNatural().floatValue();
                if (biomassFish != null && cell.getBiomassFish() != null) biomassFish[i] = cell.getBiomassFish().floatValue();
                if (biomes != null && cell.getBiome() != null) biomes[i] = (byte) cell.getBiome().ordinal();
            }
        }

        // Propagate cell-level population and technology updates to AgentBuffer cohorts so DemographicKernel
        // preserves procedural engine mortality/growth and technological era transitions on the next tick.
        if (agentBuffer != null && pop != null) {
            int agentCap = agentBuffer.getCapacity();
            int[] hexIds = agentBuffer.getHexIds();
            float[] mass = agentBuffer.getMass();
            float[] agentTech = agentBuffer.getTechLevel();

            if (popModified) {
                float[] currentCohortSum = new float[limit];
                for (int a = 0; a < agentCap; a++) {
                    int h = hexIds[a];
                    if (h >= 0 && h < limit && mass[a] > 0.0f) {
                        currentCohortSum[h] += mass[a];
                    }
                }
                for (int a = 0; a < agentCap; a++) {
                    int h = hexIds[a];
                    if (h >= 0 && h < limit) {
                        if (tech != null) {
                            agentTech[a] = tech[h];
                        }
                        if (cellPopModified[h]) {
                            float sum = currentCohortSum[h];
                            if (sum > 0.001f && Math.abs(pop[h] - sum) > 0.01f) {
                                float ratio = Math.max(0.0f, pop[h] / sum);
                                mass[a] *= ratio;
                            }
                        }
                    }
                }
            } else if (tech != null) {
                for (int a = 0; a < agentCap; a++) {
                    int h = hexIds[a];
                    if (h >= 0 && h < limit) {
                        agentTech[a] = tech[h];
                    }
                }
            }
        }
    }

    /*

     * Get total population.

     *

     * @return the resulting computation or state reference

     */

    /**
     * Updates cached scalar aggregates in a single fused O(N) cache-aligned pass over WorldBuffer.
     * Prevents dozens of redundant array traversals per simulation tick across statistics and history managers.
     */
    public void updateGlobalAggregatesCache() {
        if (worldBuffer == null) return;
        int cap = worldBuffer.getCapacity();
        float[] pop = worldBuffer.getBiomassHuman();
        float[] food = worldBuffer.getFoodResource();
        float[] water = worldBuffer.getWaterResource();
        float[] bio = worldBuffer.getBiomassNatural();
        float[] tech = worldBuffer.getTechnologyLevel();
        float[] res = worldBuffer.getResourceCapital();

        long sumPop = 0;
        double sumFood = 0;
        double sumWater = 0;
        float sumBio = 0;
        float sumTech = 0;
        double sumRes = 0;

        for (int i = 0; i < cap; i++) {
            if (pop != null) sumPop += (long) pop[i];
            if (food != null) sumFood += food[i];
            if (water != null) sumWater += water[i];
            if (bio != null) sumBio += bio[i];
            if (tech != null) sumTech += tech[i];
            if (res != null) sumRes += res[i];
        }

        this.cachedTotalPopulation = sumPop;
        this.cachedTotalFood = sumFood;
        this.cachedPotableWater = sumWater;
        this.cachedBiomassNatural = sumBio;
        this.cachedAverageTechnology = cap > 0 ? (sumTech / cap) : 0.0f;
        this.cachedResourceCapital = sumRes;
        this.lastCacheTick = tickCounter;
    }

    public long getTotalPopulation() {
        if (lastCacheTick == tickCounter && cachedTotalPopulation >= 0) {
            return cachedTotalPopulation;
        }
        if (worldBuffer == null) return 0;
        long total = 0;
        float[] pop = worldBuffer.getBiomassHuman();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) {
            total += (long) pop[i];
        }
        return total;
    }

    public double getTotalFood() {
        if (lastCacheTick == tickCounter && cachedTotalFood >= 0) {
            return cachedTotalFood;
        }
        if (worldBuffer == null) return 0;
        double total = 0;
        float[] food = worldBuffer.getFoodResource();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) {
            total += food[i];
        }
        return total;
    }

    /*

     * Get populated cell count.

     *

     * @return the resulting computation or state reference

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

        if (populatedCount < 3 || cells == null || cells.isEmpty()) return;

        nations.clear();

        cells.stream()

                .filter(c -> c.getPopulation() != null && c.getPopulation() > 0)

                .sorted(java.util.Comparator.comparingInt(H3Cell::getPopulation).reversed())

                .limit(3)

                .forEach(c -> {

                    if (c.getOwner() == null) {

                        String name = "Realm of Hex " + Long.toHexString(c.getH3Index()).toUpperCase();

                        javafx.scene.paint.Color color = javafx.scene.paint.Color.hsb(org.ether.society.util.DeterministicPRNG.nextDouble(c.getH3Index(), 777) * 360.0, 0.8, 0.9);

                        org.ether.society.database.Polity nation = new org.ether.society.database.Polity(name, color, c);

                        nations.add(nation);

                    }

                });

    }

    /*

     * Get history manager.

     *

     * @return the resulting computation or state reference

     */

    public java.util.List<org.ether.society.database.Polity> getNations() {
        return nations;
    }

    public org.ether.society.analytics.HistoryManager getHistoryManager() {

        return historyManager;

    }

    /*

     * Get current tps.

     *

     * @return the resulting computation or state reference

     */

    public double getCurrentTPS() {

        return currentTPS;

    }

    /*

     * Get total biomass natural.

     *

     * @return the resulting computation or state reference

     */

    public float getTotalBiomassNatural() {
        if (lastCacheTick == tickCounter && cachedBiomassNatural >= 0) {
            return cachedBiomassNatural;
        }
        if (worldBuffer == null) return 0;
        float total = 0;
        float[] bio = worldBuffer.getBiomassNatural();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) total += bio[i];
        return total;
    }

    public float getAverageTechnology() {
        if (lastCacheTick == tickCounter && cachedAverageTechnology >= 0) {
            return cachedAverageTechnology;
        }
        if (worldBuffer == null) return 0;
        float total = 0;
        float[] tech = worldBuffer.getTechnologyLevel();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) total += tech[i];
        return total / worldBuffer.getCapacity();
    }

    /*

     * Get current gini.

     *

     * @return the resulting computation or state reference

     */

    public float getCurrentGini() {

        return currentGini;

    }

    /*

     * Get density distribution.

     *

     * @return the resulting computation or state reference

     */

    public int[] getDensityDistribution() {

        return densityDistribution;

    }

    /*

     * Get current gdp.

     *

     * @return the resulting computation or state reference

     */

    public float getCurrentGDP() { return currentGDP; }

    /*

     * Get current life expectancy.

     *

     * @return the resulting computation or state reference

     */

    public float getCurrentLifeExpectancy() { return currentLifeExpectancy; }

    /*

     * Get current fertility.

     *

     * @return the resulting computation or state reference

     */

    public float getCurrentFertility() { return currentFertility; }

    // --- EXTENDED CLIODYNAMIC & PHYSICAL METRICS (Delegated to SimulationMetricsAggregator) ---

    /**
     * Evaluates getEnergyCaptured via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getEnergyCaptured() {
        return metricsAggregator.getEnergyCaptured();
    }

    /**
     * Evaluates getResourceDepletionRate via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getResourceDepletionRate() {
        return metricsAggregator.getResourceDepletionRate();
    }

    /**
     * Evaluates getEnergyPerCapita via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getEnergyPerCapita() {
        return metricsAggregator.getEnergyPerCapita();
    }

    /**
     * Evaluates getFoodPerCapita via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getFoodPerCapita() {
        return metricsAggregator.getFoodPerCapita();
    }

    /**
     * Evaluates getBiomassDomesticated via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getBiomassDomesticated() {
        return metricsAggregator.getBiomassDomesticated();
    }

    /**
     * Evaluates getPotableWaterTotal via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getPotableWaterTotal() {
        return metricsAggregator.getPotableWaterTotal();
    }

    /**
     * Evaluates getRemainingResourcesRatio via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getRemainingResourcesRatio() {
        return metricsAggregator.getRemainingResourcesRatio();
    }

    /**
     * Evaluates getSystemicEntropy via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getSystemicEntropy() {
        return metricsAggregator.getSystemicEntropy();
    }

    /**
     * Evaluates getPollutionLevel via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getPollutionLevel() {
        return metricsAggregator.getPollutionLevel();
    }

    /**
     * Evaluates getOccupiedTerritoryArea via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getOccupiedTerritoryArea() {
        return metricsAggregator.getOccupiedTerritoryArea();
    }

    /**
     * Evaluates getOffspringPercentage via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getOffspringPercentage() {
        return metricsAggregator.getOffspringPercentage();
    }

    /**
     * Evaluates getAgeAtFirstChild via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getAgeAtFirstChild() {
        return metricsAggregator.getAgeAtFirstChild();
    }

    /**
     * Evaluates getImmigrationRate via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getImmigrationRate() {
        return metricsAggregator.getImmigrationRate();
    }

    /**
     * Evaluates getEducationLevel via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getEducationLevel() {
        return metricsAggregator.getEducationLevel();
    }

    /**
     * Evaluates getHappinessIndex via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getHappinessIndex() {
        return metricsAggregator.getHappinessIndex();
    }

    /**
     * Evaluates getConflictLevel via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getConflictLevel() {
        return metricsAggregator.getConflictLevel();
    }

    /**
     * Evaluates getCityStatesCount via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public int getCityStatesCount() {
        return metricsAggregator.getCityStatesCount();
    }

    /**
     * Evaluates getInstitutionalMaturity via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getInstitutionalMaturity() {
        return metricsAggregator.getInstitutionalMaturity();
    }

    /**
     * Evaluates getDivisionOfLaborIndex via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getDivisionOfLaborIndex() {
        return metricsAggregator.getDivisionOfLaborIndex();
    }

    /**
     * Evaluates getEroiAlimentaire via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getEroiAlimentaire() {
        return metricsAggregator.getEroiAlimentaire();
    }

    /**
     * Evaluates getNetSurplusFraction via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getNetSurplusFraction() {
        return metricsAggregator.getNetSurplusFraction();
    }

    /**
     * Evaluates getTrophicMultiplier via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getTrophicMultiplier() {
        return metricsAggregator.getTrophicMultiplier();
    }

    /**
     * Evaluates getBiomassMobilizedPerCapitaKg via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getBiomassMobilizedPerCapitaKg() {
        return metricsAggregator.getBiomassMobilizedPerCapitaKg();
    }

    /**
     * Evaluates getMaxHierarchyLevel via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public int getMaxHierarchyLevel() {
        return metricsAggregator.getMaxHierarchyLevel();
    }

    /**
     * Evaluates getLargestCulturalUnitSize via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public long getLargestCulturalUnitSize() {
        return metricsAggregator.getLargestCulturalUnitSize();
    }

    /**
     * Evaluates getLargestOrganizationComplexity via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getLargestOrganizationComplexity() {
        return metricsAggregator.getLargestOrganizationComplexity();
    }

    /**
     * Evaluates getLargestOrganizationEntropy via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getLargestOrganizationEntropy() {
        return metricsAggregator.getLargestOrganizationEntropy();
    }

    /**
     * Evaluates getKardashevScale via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getKardashevScale() {
        return metricsAggregator.getKardashevScale();
    }

    /**
     * Evaluates getBuiltCapitalTotal via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getBuiltCapitalTotal() {
        return metricsAggregator.getBuiltCapitalTotal();
    }

    /**
     * Evaluates getEliteFormationRatio via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getEliteFormationRatio() {
        return metricsAggregator.getEliteFormationRatio();
    }

    /**
     * Evaluates getElderCapitalShare via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getElderCapitalShare() {
        return metricsAggregator.getElderCapitalShare();
    }

    /**
     * Evaluates getLandRentIndex via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getLandRentIndex() {
        return metricsAggregator.getLandRentIndex();
    }

    /**
     * Evaluates getToolsCount via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public long getToolsCount() {
        return metricsAggregator.getToolsCount();
    }

    /**
     * Evaluates getProductsCount via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public long getProductsCount() {
        return metricsAggregator.getProductsCount();
    }

    /**
     * Evaluates getSystemComplexityIndex via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getSystemComplexityIndex() {
        return metricsAggregator.getSystemComplexityIndex();
    }

    /**
     * Evaluates getReconstructionCapabilityIndex via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getReconstructionCapabilityIndex() {
        return metricsAggregator.getReconstructionCapabilityIndex();
    }

    /**
     * Evaluates getSystemInterdependenceIndex via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getSystemInterdependenceIndex() {
        return metricsAggregator.getSystemInterdependenceIndex();
    }

    /**
     * Evaluates getAgePyramid via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public int[] getAgePyramid() {
        return metricsAggregator.getAgePyramid();
    }

    /**
     * Evaluates getShannonBandwidth via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getShannonBandwidth() {
        return metricsAggregator.getShannonBandwidth();
    }

    /**
     * Evaluates getCollectiveMemoryStock via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getCollectiveMemoryStock() {
        return metricsAggregator.getCollectiveMemoryStock();
    }

    /**
     * Evaluates getInnovationDiffusionSpeed via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getInnovationDiffusionSpeed() {
        return metricsAggregator.getInnovationDiffusionSpeed();
    }

    /**
     * Evaluates getKnowledgeDecayRate via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getKnowledgeDecayRate() {
        return metricsAggregator.getKnowledgeDecayRate();
    }

    /**
     * Evaluates getSoilNPKQuality via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getSoilNPKQuality() {
        return metricsAggregator.getSoilNPKQuality();
    }

    /**
     * Evaluates getCarbonFootprint via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getCarbonFootprint() {
        return metricsAggregator.getCarbonFootprint();
    }

    /**
     * Evaluates getWildBiodiversityIndex via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getWildBiodiversityIndex() {
        return metricsAggregator.getWildBiodiversityIndex();
    }

    /**
     * Evaluates getWetBulbSafetyMargin via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getWetBulbSafetyMargin() {
        return metricsAggregator.getWetBulbSafetyMargin();
    }

    /**
     * Evaluates getEliteOverproductionIndex via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getEliteOverproductionIndex() {
        return metricsAggregator.getEliteOverproductionIndex();
    }

    /**
     * Evaluates getFiscalStressIndex via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getFiscalStressIndex() {
        return metricsAggregator.getFiscalStressIndex();
    }

    /**
     * Evaluates getGeopoliticalTension via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getGeopoliticalTension() {
        return metricsAggregator.getGeopoliticalTension();
    }

    /**
     * Evaluates getCollapseVulnerability via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getCollapseVulnerability() {
        return metricsAggregator.getCollapseVulnerability();
    }

    /**
     * Evaluates getAverageAsabiyyah via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getAverageAsabiyyah() {
        return metricsAggregator.getAverageAsabiyyah();
    }

    /**
     * Evaluates getPopulationSurvivalRate via {\@link SimulationMetricsAggregator}.
     *
     * \@return computed metric
     */
    public double getPopulationSurvivalRate() {
        return metricsAggregator.getPopulationSurvivalRate();
    }

    private void syncClimateToBuffer() {
        if (worldBuffer == null || cells == null) return;
        float[] temps = worldBuffer.getTemperature();
        if (temps == null) return;
        int limit = Math.min(cells.size(), Math.min(worldBuffer.getCapacity(), temps.length));
        for (int i = 0; i < limit; i++) {
            H3Cell c = cells.get(i);
            if (c != null && c.getTemperature() != null) {
                temps[i] = c.getTemperature().floatValue();
            }
        }
    }

    /*

     * Get world buffer.

     *

     * @return the resulting computation or state reference

     */

    public org.ether.society.core.dod.WorldBuffer getWorldBuffer() {

        return worldBuffer;

    }

    /*

     * Set world buffer.

     *

     * @param worldBuffer the world buffer parameter (org.ether.society.core.dod.WorldBuffer)

     */

    public void setWorldBuffer(org.ether.society.core.dod.WorldBuffer worldBuffer) {

        this.worldBuffer = worldBuffer;

    }

    /*

     * Get agent buffer.

     *

     * @return the resulting computation or state reference

     */

    public org.ether.society.core.dod.AgentBuffer getAgentBuffer() {

        return agentBuffer;

    }

    /*

     * Get simulation save manager.

     *

     * @return the resulting computation or state reference

     */

    public SimulationSaveManager getSimulationSaveManager() {
        return simulationSaveManager;
    }

    /**
     * Gets the data-oriented hexagonal trade and resource diffusion flux kernel.
     *
     * @return the flux kernel instance
     */
    public org.ether.society.core.dod.FluxKernel getFluxKernel() {
        return fluxKernel;
    }

    long getLastCacheTick() { return lastCacheTick; }
    double getCachedPotableWater() { return cachedPotableWater; }
    double getCachedResourceCapital() { return cachedResourceCapital; }
    public SimulationMetricsAggregator getMetricsAggregator() { return metricsAggregator; }
}

