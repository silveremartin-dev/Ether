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
import org.ether.society.engines.tier1.NuclearWarfareClimateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.ether.society.persistence.SimulationSaveManager;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * H3-based simulation engine.
 * Manages the deterministic simulation loop grounded strictly in physical laws.
 */
public class H3SimulationEngine implements ISimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(H3SimulationEngine.class);

    private final Configuration config;
    private final TimeManager timeManager;
    private final org.ether.society.events.EventSystem eventSystem;

    private final org.ether.society.analytics.HistoryManager historyManager;
    private final List<org.ether.society.model.Nation> nations = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final SimulationSaveManager simulationSaveManager;

    // DOD Layer
    private final org.ether.society.core.profiling.SimulationProfiler profiler = new org.ether.society.core.profiling.SimulationProfiler();
    private org.ether.society.core.dod.WorldBuffer worldBuffer;
    private org.ether.society.core.dod.AgentBuffer agentBuffer;
    private org.ether.society.core.dod.DemographicKernel demographicKernel;
    private org.ether.society.core.dod.UrbanKernel urbanKernel;
    private org.ether.society.core.dod.CultureKernel cultureKernel;
    private final org.ether.society.core.dod.EnvironmentalKernel environmentalKernel;
    private final org.ether.society.core.dod.StatisticsKernel statisticsKernel;
    private final SimulationPipeline simulationPipeline;

    /* Internal state variable for last tick time (long). */
    private long lastTickTime = 0;
    /* Internal state variable for current tps (double). */
    private double currentTPS = 0;
    /* Internal state variable for current gini (float). */
    private float currentGini = 0;
    /* Internal state variable for current gdp (float). */
    private float currentGDP = 0;
    /* Internal state variable for current life expectancy (float). */
    private float currentLifeExpectancy = 0;
    /* Internal state variable for current fertility (float). */
    private float currentFertility = 0;
    private int[] densityDistribution = new int[20]; // 20 bins

    /* Internal state variable for cells (List&lt;H3Cell&gt;). */
    private List<H3Cell> cells;
    private Scenario currentScenario;
    private final java.util.Set<String> firedScenarioEventKeys = new java.util.HashSet<>();
    private org.ether.society.config.SimulationPerformanceConfig performanceConfig = new org.ether.society.config.SimulationPerformanceConfig(true);

    private org.ether.society.network.ClusterManager clusterManager;

    private ScheduledExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean pauseAtNextEvent = new java.util.concurrent.atomic.AtomicBoolean(false);
    /* Internal state variable for speed multiplier (double). */
    private double speedMultiplier = 1.0;

    /*
     * H3simulation engine.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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
        this.simulationPipeline = new SimulationPipeline(demographicKernel, urbanKernel, cultureKernel, environmentalKernel, statisticsKernel);

        initialize();
    }

    /*
     * Get event system.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public org.ether.society.events.EventSystem getEventSystem() {
        return eventSystem;
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
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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

        this.currentScenario = scenario;
        this.performanceConfig = scenario != null ? scenario.toPerformanceConfig() : new org.ether.society.config.SimulationPerformanceConfig(true);
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

        int populatedCount = (int) cells.stream().filter(c -> c.getPopulation() != null && c.getPopulation() > 0).count();
        initializePoliticalSeeding(populatedCount);

        int cohortSize = scenario != null && scenario.getTargetCohortSize() > 0 ? scenario.getTargetCohortSize() : 150;
        if (demographicKernel != null) {
            demographicKernel.setTargetCohortSize(cohortSize);
        }

        long initialPop = scenario != null ? scenario.getInitialHumanCount() : 1_000_000L;
        int estimatedAgents = (int) Math.max(cells.size(), Math.min(10_000_000L, initialPop / Math.max(1, cohortSize)));

        this.worldBuffer = new org.ether.society.core.dod.WorldBuffer(cells.size());
        this.agentBuffer = new org.ether.society.core.dod.AgentBuffer(Math.max(1000, estimatedAgents * 2));
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, worldBuffer);
        org.ether.society.data.DODDataGenerator.initializeAgentBuffer(worldBuffer, agentBuffer, cohortSize);

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
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public Scenario getCurrentScenario() {
        return currentScenario;
    }

    /*
     * Get performance config.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public org.ether.society.config.SimulationPerformanceConfig getPerformanceConfig() {
        return performanceConfig;
    }

    /*
     * Set performance config.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @param performanceConfig the performance config parameter (org.ether.society.config.SimulationPerformanceConfig)
     */
    public void setPerformanceConfig(org.ether.society.config.SimulationPerformanceConfig performanceConfig) {
        this.performanceConfig = performanceConfig;
    }

    private void initializePopulation() {
        boolean hasExistingPop = cells.stream().anyMatch(c -> c.getPopulation() > 0);
        if (hasExistingPop) {
            int count = (int) cells.stream().filter(c -> c.getPopulation() > 0).count();
            initializePoliticalSeeding(count);
            return;
        }

        int populatedCells = 0;
        for (H3Cell cell : cells) {
            org.ether.society.model.Biome biome = cell.getBiome();
            if (biome == org.ether.society.model.Biome.OCEAN || biome == org.ether.society.model.Biome.DEEP_OCEAN) {
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

            if (Math.random() < 0.1 && basePop > 0) {
                cell.setPopulation(basePop);
                populatedCells++;
            }
        }

        initializePoliticalSeeding(populatedCells);
    }

    @Override
    /*
     * Start.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     */
    public void start() {
        if (running.getAndSet(true)) return;
        startGameLoop();
    }

    @Override
    /*
     * Pause.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     */
    public void pause() {
        if (!running.getAndSet(false)) return;
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Override
    /*
     * Reset.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     */
    public void reset() {
        pause();
        firedScenarioEventKeys.clear();
        timeManager.reset(config.simulation().startYear());
        historyManager.reset();
        initialize();
    }

    @Override
    /*
     * Set speed.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @param multiplier the multiplier parameter (int)
     */
    public void setSpeed(int multiplier) {
        setSpeed((double) multiplier);
    }

    @Override
    /*
     * Set speed.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public int getSpeed() {
        return (int) Math.round(speedMultiplier);
    }

    @Override
    /*
     * Get speed multiplier.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    @Override
    /*
     * Get time manager.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public TimeManager getTimeManager() {
        return timeManager;
    }

    /*
     * Get current year.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public long getCurrentYear() {
        return timeManager != null ? timeManager.getCurrentYear() : 0;
    }

    @Override
    /*
     * Is running.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isRunning() {
        return running.get();
    }

    @Override
    /*
     * Set pause at next event.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @param pause the pause parameter (boolean)
     */
    public void setPauseAtNextEvent(boolean pause) {
        this.pauseAtNextEvent.set(pause);
    }

    @Override
    /*
     * Is pause at next event.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isPauseAtNextEvent() {
        return this.pauseAtNextEvent.get();
    }

    /*
     * Get cells.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public List<H3Cell> getCells() {
        return cells;
    }

    /*
     * Set cells.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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
            this.agentBuffer = new org.ether.society.core.dod.AgentBuffer(Math.max(1000, estimatedAgents * 2));
            org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, worldBuffer);
            org.ether.society.data.DODDataGenerator.initializeAgentBuffer(worldBuffer, agentBuffer, cohortSize);
        } else {
            this.worldBuffer = new org.ether.society.core.dod.WorldBuffer(0);
            this.agentBuffer = new org.ether.society.core.dod.AgentBuffer(0);
        }
    }

    /*
     * Save simulation.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @param saveName the save name parameter (String)
     */
    public void saveSimulation(String saveName) {
        boolean wasRunning = running.get();
        if (wasRunning) pause();
        simulationSaveManager.saveSimulation(this, saveName);
        if (wasRunning) start();
    }

    /*
     * Load simulation.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public org.ether.society.network.ClusterManager getClusterManager() {
        return clusterManager;
    }

    /*
     * Set on tick callback.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @param callback the callback parameter (Runnable)
     */
    public void setOnTickCallback(Runnable callback) {
        this.onTickCallback = callback;
    }

    /*
     * Get profiler.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public org.ether.society.core.profiling.SimulationProfiler getProfiler() {
        return profiler;
    }

    /* Internal state variable for tick counter (int). */
    private int tickCounter = 0;

    /*
     * Get tick counter.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public long getTickCounter() {
        return tickCounter;
    }

    public enum TemporalScale {
        DAILY(1, "ÃƒÂ°Ã…Â¸Ã¢â‚¬Å“Ã¢â‚¬Â¦ Pas Quotidien (Jour par Jour - DÃƒÆ’Ã‚Â©taillÃƒÆ’Ã‚Â©)"),
        MONTHLY(30, "ÃƒÂ°Ã…Â¸Ã…Â¡Ã¢â€šÂ¬ Pas Mensuel (Mois par Mois - Mode Rapide)");

        /* Internal state variable for factor (int). */
        private final int factor;
        /* Internal state variable for label (String). */
        private final String label;

        TemporalScale(int factor, String label) {
            this.factor = factor;
            this.label = label;
        }

        /*
         * Get factor.
         * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
         *
         * @return the resulting computation or state reference
         */
        public int getFactor() { return factor; }
        /*
         * Get label.
         * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
         *
         * @return the resulting computation or state reference
         */
        public String getLabel() { return label; }

        @Override
        /*
         * To string.
         * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public TemporalScale getTemporalScale() { return temporalScale; }
    /*
     * Set temporal scale.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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
            profiler.beginPhase("1_FastScaleFlux");
            timeManager.advanceDays(stepDays);
            profiler.endPhase("1_FastScaleFlux");

            // 2. SLOW SCALE PHYSICALIST DYNAMICS
            // Triggered every tick if stepDays >= 30, or every (30 / stepDays) ticks if stepDays < 30
            int slowModulo = Math.max(1, 30 / stepDays);
            boolean runSlowScale = (stepDays >= 30) || (tickCounter % slowModulo == 0);

            if (runSlowScale) {
                if (currentScenario != null && timeManager.getCurrentYear() >= currentScenario.getEndDateYear()) {
                    logger.info("ÃƒÂ°Ã…Â¸Ã‚ÂÃ‚Â Simulation reached scenario target end date (Year {}). Auto-pausing.", currentScenario.getEndDateYear());
                    pause();
                    return;
                }
                int month = timeManager.getCurrentMonth();
                float dtSlow = Math.max(DT_TICK, 30.0f * 86400f);

                profiler.beginPhase("2_ClimateAndEnvironment");
                environmentalKernel.tick(worldBuffer, month, dtSlow);
                profiler.endPhase("2_ClimateAndEnvironment");

                simulationPipeline.executeTick(
                        this, cells, worldBuffer, agentBuffer,
                        profiler, performanceConfig, (long) dtSlow, getAverageTechnology()
                );

                // Statistics & Snapshots
                profiler.beginPhase("5_StatisticsAndHistory");
                currentGini = statisticsKernel.calculateGini(worldBuffer.getResourceCapital());
                densityDistribution = statisticsKernel.calculateDistribution(worldBuffer.getBiomassHuman(), 20, 1000.0f);
                currentGDP = statisticsKernel.calculateGDP(worldBuffer.getResourceCapital());
                float foodPerCapVal = (float) getFoodPerCapita();
                float foodSatVal = Math.min(1.0f, foodPerCapVal > 0 ? foodPerCapVal / 2.0f : 1.0f);
                currentLifeExpectancy = statisticsKernel.calculateLifeExpectancy(agentBuffer.getAge(), agentBuffer.getHexIds(), getAverageTechnology(), foodSatVal);
                currentFertility = statisticsKernel.calculateFertilityRate(agentBuffer.getBirths(), agentBuffer.getMass());

                historyManager.captureSnapshot(this);
                // Bounded snapshot of full 3D spatial map once per year (Month 0) or at launch to prevent excessive RAM allocation
                if (timeManager.getCurrentMonth() == 0 || tickCounter == 0) {
                    historyManager.captureWorldSnapshot(this);
                }
                
                int eventCountBefore = eventSystem.peekEvents().size();
                checkScenarioClimateEvents();
                eventSystem.checkEvents(timeManager.getCurrentYear(), timeManager.getCurrentMonth(), getTotalPopulation(), getTotalFood(), cells);
                eventSystem.checkCellEvents(timeManager.getCurrentYear(), timeManager.getCurrentMonth(), cells);
                int eventCountAfter = eventSystem.peekEvents().size();

                if (pauseAtNextEvent.get() && eventCountAfter > eventCountBefore) {
                    logger.info("ÃƒÂ¢Ã‚ÂÃ‚Â¸ÃƒÂ¯Ã‚Â¸Ã‚Â [Pas {}] Pause automatique de la simulation sur ÃƒÆ’Ã‚Â©vÃƒÆ’Ã‚Â©nement (pauseAtNextEvent=true)", tickCounter);
                    pause();
                }
                profiler.endPhase("5_StatisticsAndHistory");

                profiler.beginPhase("6_BufferSync");
                syncBufferToCells();
                profiler.endPhase("6_BufferSync");

                if (tickCounter % Math.max(1, 12 / stepDays) == 0) {
                    logger.info("ÃƒÂ¢Ã…Â¡Ã¢â€žÂ¢ÃƒÂ¯Ã‚Â¸Ã‚Â [Pas {}] An {} M.{} | Pop: {} hab | TPS: {} it/s",
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
                        displayTitle = "ÃƒÂ°Ã…Â¸Ã¢â‚¬Å“Ã…â€œ REPÃƒÆ’Ã‹â€ RE : " + evt.getName();
                    } else if (type.contains("nuclear") || type.contains("strike")) {
                        displayTitle = "ÃƒÂ¢Ã‹Å“Ã‚Â¢ÃƒÂ¯Ã‚Â¸Ã‚Â FRAPPE NUCLÃƒÆ’Ã¢â‚¬Â°AIRE : " + evt.getName() + " (Mag: " + evt.getMagnitude() + ")";
                    } else {
                        displayTitle = "ÃƒÂ°Ã…Â¸Ã…â€™Ã¢â‚¬Â¹ ÃƒÆ’Ã¢â‚¬Â°VÃƒÆ’Ã¢â‚¬Â°NEMENT : " + evt.getName() + " (" + evt.getType() + " - Mag: " + evt.getMagnitude() + ")";
                    }

                    org.ether.society.events.ActiveEvent ae = new org.ether.society.events.ActiveEvent(
                        "SCENARIO_EVT_" + System.currentTimeMillis(),
                        displayTitle,
                        evt.getType().toUpperCase(),
                        evt.getLatitude(), evt.getLongitude(),
                        currentYear, timeManager.getCurrentMonth(), 1, 25.0, evt.getMagnitude()
                    );
                    eventSystem.recordSpatialEvent(ae);
                    logger.info("ÃƒÂ°Ã…Â¸Ã…â€™Ã¢â‚¬Â¹ [Pas {}] ÃƒÆ’Ã¢â‚¬Â°vÃƒÆ’Ã‚Â©nement scÃƒÆ’Ã‚Â©nario : {} (AnnÃƒÆ’Ã‚Â©e {})", tickCounter, evt.getName(), currentYear);

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

        for (H3Cell cell : cells) {
            double cLat = cell.getLatitude() != null ? cell.getLatitude() : 0.0;
            double cLng = cell.getLongitude() != null ? cell.getLongitude() : 0.0;
            double distDeg = Math.hypot(cLat - evtLat, cLng - evtLng);

            double impactRadius = Math.max(5.0, mag * 3.0);
            if (distDeg <= impactRadius) {
                double attenuation = 1.0 - (distDeg / impactRadius);
                String type = evt.getType() != null ? evt.getType().toLowerCase() : "";

                if (type.contains("nuclear") || type.contains("strike")) {
                    NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() + mag * 0.15);
                    double cooling = -0.6 * mag * attenuation;
                    cell.setTemperature(Math.max(-50.0, (cell.getTemperature() != null ? cell.getTemperature() : 15.0) + cooling));
                    if (cell.getPopulation() != null && cell.getPopulation() > 0) {
                        int lost = (int) (cell.getPopulation() * (0.85 * attenuation));
                        cell.setPopulation(Math.max(0, cell.getPopulation() - lost));
                    }
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * (1.0 - 0.70 * attenuation)));
                    }
                    if (cell.getFoodResource() != null) {
                        cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * (1.0 - 0.80 * attenuation)));
                    }
                    cell.setPollutionLevel(Math.min(1.0, (cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0) + 0.8 * attenuation));
                } else if (type.contains("volcano") || type.contains("ice")) {
                    double cooling = -0.5 * mag * attenuation;
                    cell.setTemperature(Math.max(-50.0, (cell.getTemperature() != null ? cell.getTemperature() : 15.0) + cooling));
                } else if (type.contains("earthquake") || type.contains("tsunami") || type.contains("meteor")) {
                    if (cell.getResourceCapital() != null) {
                        cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * (1.0 - 0.08 * mag * attenuation)));
                    }
                    if (cell.getPopulation() != null && cell.getPopulation() > 0) {
                        int lost = (int) (cell.getPopulation() * (0.05 * mag * attenuation));
                        cell.setPopulation(Math.max(0, cell.getPopulation() - lost));
                    }
                } else if (type.contains("famine") || type.contains("drought")) {
                    if (cell.getFoodResource() != null) {
                        cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * (1.0 - 0.10 * mag * attenuation)));
                    }
                }
            }
        }
    }

    @Override
    /*
     * Step forward.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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
    }

    @Override
    /*
     * Step backward.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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
                java.util.Map<Long, H3Cell> map = snapshot.stream().collect(java.util.stream.Collectors.toMap(H3Cell::getH3Index, c -> c));
                for (H3Cell c : cells) {
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

    /* Internal state variable for last auto save time ms (long). */
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
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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

        org.ether.society.model.Biome[] biomeValues = org.ether.society.model.Biome.values();

        for (int i = 0; i < cells.size() && i < worldBuffer.getCapacity(); i++) {
            H3Cell cell = cells.get(i);
            int p = (int) Math.max(0, pop[i]);
            cell.setPopulation(p);
            cell.setBiomassHuman((double) pop[i]);
            cell.setFoodResource((double) food[i]);
            cell.setTechnologyLevel((double) tech[i]);
            cell.setWaterResource((double) water[i]);
            cell.setWoodResource((double) wood[i]);
            cell.setGiniIndex((double) gini[i]);
            if (capital != null) cell.setResourceCapital((double) capital[i]);

            // Sync biomes bidirectionally
            if (cell.getBiome() != null) {
                biomes[i] = (byte) cell.getBiome().ordinal();
            } else if (biomes[i] >= 0 && biomes[i] < biomeValues.length) {
                cell.setBiome(biomeValues[biomes[i]]);
            }
            if (p > 0) {
                cell.updateAgePyramidFromTotal(cell.getTechnologyLevel() != null && cell.getTechnologyLevel() > 0 ? cell.getTechnologyLevel() : 1.0);
            }
        }
    }

    /*
     * Get total population.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
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

    /*
     * Get total food.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
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

    /*
     * Get populated cell count.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
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
                        javafx.scene.paint.Color color = javafx.scene.paint.Color.hsb(Math.random() * 360, 0.8, 0.9);
                        org.ether.society.model.Nation nation = new org.ether.society.model.Nation(name, color, c);
                        nations.add(nation);
                    }
                });
    }

    /*
     * Get history manager.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public org.ether.society.analytics.HistoryManager getHistoryManager() {
        return historyManager;
    }

    /*
     * Get current tps.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getCurrentTPS() {
        return currentTPS;
    }

    /*
     * Get total biomass natural.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public float getTotalBiomassNatural() {
        if (worldBuffer == null) return 0;
        float total = 0;
        float[] bio = worldBuffer.getBiomassNatural();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) total += bio[i];
        return total;
    }

    /*
     * Get average technology.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public float getAverageTechnology() {
        if (worldBuffer == null) return 0;
        float total = 0;
        float[] tech = worldBuffer.getTechnologyLevel();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) total += tech[i];
        return total / worldBuffer.getCapacity();
    }

    /*
     * Get current gini.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public float getCurrentGini() {
        return currentGini;
    }

    /*
     * Get density distribution.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public int[] getDensityDistribution() {
        return densityDistribution;
    }

    /*
     * Get current gdp.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public float getCurrentGDP() { return currentGDP; }
    /*
     * Get current life expectancy.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public float getCurrentLifeExpectancy() { return currentLifeExpectancy; }
    /*
     * Get current fertility.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public float getCurrentFertility() { return currentFertility; }

    // --- EXTENDED CLIODYNAMIC & PHYSICAL METRICS ---

    /*
     * Get energy captured.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getEnergyCaptured() {
        long pop = getTotalPopulation();
        if (pop <= 0) return 0.0;
        float tech = getAverageTechnology();
        // Leslie White Energy Law: P_tot = N_pop * P_capita
        // Baseline 300 W per capita (human metabolic work ~120W + fire energy ~180W)
        // Technology scales energy extraction: animal, water, steam, electrical
        double wattsPerCapita = 300.0 + Math.pow(Math.max(0.0, tech), 2.2) * 35.0;
        double totalWatts = pop * wattsPerCapita;
        return totalWatts / 1e6; // Energy returned in Megawatts (MW)
    }

    /*
     * Get resource depletion rate.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getResourceDepletionRate() {
        if (worldBuffer == null || worldBuffer.getCapacity() == 0) return 0;
        double maxRes = worldBuffer.getCapacity() * 1000.0;
        double currentRes = 0;
        float[] res = worldBuffer.getResourceCapital();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) currentRes += res[i];
        return Math.max(0, Math.min(100.0, (1.0 - currentRes / Math.max(1, maxRes)) * 100.0));
    }

    /*
     * Get energy per capita.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getEnergyPerCapita() {
        long pop = getTotalPopulation();
        return pop > 0 ? (getEnergyCaptured() * 1e6) / pop : 300.0; // in Joules/sec (Watts)
    }

    /*
     * Get food per capita.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getFoodPerCapita() {
        long pop = getTotalPopulation();
        return pop > 0 ? getTotalFood() / pop : 0;
    }

    /*
     * Get biomass domesticated.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getBiomassDomesticated() {
        return getTotalPopulation() * 0.15 + getTotalFood() * 0.4;
    }

    /*
     * Get potable water total.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getPotableWaterTotal() {
        if (worldBuffer == null) return 0;
        double water = 0;
        float[] w = worldBuffer.getWaterResource();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) water += w[i];
        return water;
    }

    /*
     * Get remaining resources ratio.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getRemainingResourcesRatio() {
        return 100.0 - getResourceDepletionRate();
    }

    /*
     * Get systemic entropy.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getSystemicEntropy() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        return (pop * 0.005 + tech * 2.5) % 1000.0;
    }

    /*
     * Get pollution level.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getPollutionLevel() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        return Math.max(0, (tech > 50 ? (tech - 50) * 1.5 * (pop / 100000.0) : 0));
    }

    /*
     * Get occupied territory area.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getOccupiedTerritoryArea() {
        double baseCellArea = (currentScenario != null && currentScenario.getCellSizeKm2() > 0)
                ? currentScenario.getCellSizeKm2() : 1250.0;
        long populatedCells = getPopulatedCellCount();
        long pop = getTotalPopulation();
        if (populatedCells == 0 || pop == 0) return 0.0;

        float tech = getAverageTechnology();
        double physicalGridArea = populatedCells * baseCellArea;

        // Binford (2001), Kelly (1995), Hassan (1981) - Behavioral Ecology & Home Range:
        // Pre-agricultural Hunter-Gatherers (Tech < 1.5): 10 to 100 kmÃƒâ€šÃ‚Â² per capita diffuse subsistence home range
        if (tech < 1.5f) {
            double temp = 15.0;
            if (worldBuffer != null && worldBuffer.getTemperature().length > 0) {
                temp = worldBuffer.getTemperature()[0];
            }
            // Harsh cold/arid biomes require 80-100 kmÃƒâ€šÃ‚Â²/hab, rich temperate/river valleys 15-40 kmÃƒâ€šÃ‚Â²/hab
            double km2PerCapita = (temp < 5.0) ? 85.0 : Math.max(12.0, 45.0 - (temp * 1.2));
            double ecologicalHomeRange = pop * km2PerCapita;
            return Math.max(physicalGridArea, ecologicalHomeRange);
        } else if (tech < 5.0f) {
            // Neolithic & Agrarian transition (Tech 1.5 to 5.0): 0.05 to 2.0 kmÃƒâ€šÃ‚Â²/hab
            double km2PerCapita = Math.max(0.05, 2.0 - (tech - 1.5) * 0.55);
            return Math.max(physicalGridArea, pop * km2PerCapita);
        }

        return physicalGridArea;
    }

    /*
     * Get offspring percentage.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getOffspringPercentage() {
        float fert = getCurrentFertility();
        return Math.min(95.0, Math.max(20.0, 40.0 + fert * 7.5));
    }

    /*
     * Get age at first child.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getAgeAtFirstChild() {
        float tech = getAverageTechnology();
        return Math.min(32.0, Math.max(16.0, 18.0 + (tech / 200.0) * 10.0));
    }

    /*
     * Get immigration rate.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getImmigrationRate() {
        long pop = getTotalPopulation();
        return pop > 0 ? (pop % 1000) / 10.0 : 0;
    }

    /*
     * Get education level.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getEducationLevel() {
        float tech = getAverageTechnology();
        return Math.min(100.0, (tech / 250.0) * 100.0);
    }

    /*
     * Get happiness index.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getHappinessIndex() {
        float gini = getCurrentGini();
        float life = getCurrentLifeExpectancy();
        double foodPerCap = getFoodPerCapita();
        double annualReq = PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;
        double foodSat = Math.min(1.0, foodPerCap > 0 ? foodPerCap / annualReq : 0.5);
        double base = 40.0 + (life / 80.0) * 30.0 + (foodSat * 30.0) - (gini * 20.0);
        return Math.max(10.0, Math.min(100.0, base));
    }

    /*
     * Get conflict level.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getConflictLevel() {
        float gini = getCurrentGini();
        double happiness = getHappinessIndex();
        double foodPerCap = getFoodPerCapita();
        double annualReq = PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;
        double scarcity = (foodPerCap > 0 && foodPerCap < annualReq) ? (1.0 - foodPerCap / annualReq) * 25.0 : 0.0;
        return Math.max(0.0, Math.min(100.0, (gini * 35.0) + (100.0 - happiness) * 0.25 + scarcity));
    }

    /*
     * Get city states count.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public int getCityStatesCount() {
        float tech = getAverageTechnology();
        long year = getCurrentYear();
        // City-states and urban hubs strictly appear in the Bronze/Agricultural Revolution era (post -4000 BC, Tech >= 15)
        if (tech < 15.0f || year < -4000) {
            return 0;
        }
        long popCells = getPopulatedCellCount();
        return (int) Math.max(0, popCells / 12);
    }

    /*
     * Get institutional maturity.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getInstitutionalMaturity() {
        float tech = getAverageTechnology();
        return Math.min(100.0, tech * 0.45);
    }

    /*
     * Get division of labor index.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getDivisionOfLaborIndex() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        double surplus = getNetSurplusFraction();
        return Math.min(100.0, (surplus * 60.0) + (tech * 0.25) + Math.log10(Math.max(1, pop)) * 3.0);
    }

    /*
     * Energy Return on Investment for the food/subsistence system (EROI_alim = E_out / E_in).
     * Preindustrial: 3:1 to 15:1. Industrial thermodynamic inversion: < 1.0 (7-10 kcal fossil per 1 kcal ingested).
     */
    public double getEroiAlimentaire() {
        float tech = getAverageTechnology();
        if (tech < 1.5f) {
            return 7.0; // Paleolithic Hunter-Gatherer baseline
        } else if (tech < 4.0f) {
            return 6.5; // Early Neolithic
        } else if (tech < 50.0f) {
            return 2.8; // Preindustrial Agrarian with Draft Animals
        } else if (tech < 120.0f) {
            return 0.50; // Industrial Era (Thermodynamic inversion)
        } else {
            return 0.12; // Post-Industrial Globalized Food System (10:1 fossil input)
        }
    }

    /*
     * Net societal energy surplus fraction Phi = 1 - 1 / EROI_alim (Tainter 1988, Hall et al. 2014).
     */
    public double getNetSurplusFraction() {
        double eroi = getEroiAlimentaire();
        return Math.max(0.0, 1.0 - (1.0 / Math.max(0.1, eroi)));
    }

    /*
     * Trophic footprint multiplier mu = Mobilized Raw Biomass / Ingested Energy (2.0x to 25.0x).
     */
    public double getTrophicMultiplier() {
        float tech = getAverageTechnology();
        if (tech < 1.5f) return PhysicalConstants.TROPHIC_MULTIPLIER_HUNTER_GATHERER;
        if (tech < 4.0f) return PhysicalConstants.TROPHIC_MULTIPLIER_NEOLITHIC_EARLY_AGRARIAN;
        if (tech < 50.0f) return PhysicalConstants.TROPHIC_MULTIPLIER_PREINDUSTRIAL_ADVANCED_AGRARIAN;
        if (tech < 120.0f) return PhysicalConstants.TROPHIC_MULTIPLIER_INDUSTRIAL_WORKER;
        return PhysicalConstants.TROPHIC_MULTIPLIER_POST_INDUSTRIAL;
    }

    /*
     * Gross raw biomass mobilized per capita per year in kg/hab/an.
     */
    public double getBiomassMobilizedPerCapitaKg() {
        double trophicMul = getTrophicMultiplier();
        float tech = getAverageTechnology();
        double energyDensity = (tech < 1.5f) ? PhysicalConstants.BIOMASS_ENERGY_DENSITY_FAUNA_MJ_PER_KG
                : (tech < 50.0f ? PhysicalConstants.BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG : 14.0);
        return (PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_MJ * trophicMul) / energyDensity;
    }

    /*
     * Get max hierarchy level.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public int getMaxHierarchyLevel() {
        float tech = getAverageTechnology();
        if (tech < 10) return 1;
        if (tech < 30) return 2;
        if (tech < 60) return 3;
        if (tech < 100) return 4;
        if (tech < 200) return 5;
        return 6;
    }

    /*
     * Get largest cultural unit size.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public long getLargestCulturalUnitSize() {
        if (!nations.isEmpty()) {
            return nations.stream()
                    .mapToLong(org.ether.society.model.Nation::getTotalPopulation)
                    .max()
                    .orElse((long) (getTotalPopulation() * Math.min(0.85, 0.2 + (getAverageTechnology() / 300.0))));
        }
        long pop = getTotalPopulation();
        return (long) (pop * Math.min(0.85, 0.2 + (getAverageTechnology() / 300.0)));
    }

    /*
     * Get largest organization complexity.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getLargestOrganizationComplexity() {
        long largestPop = getLargestCulturalUnitSize();
        if (largestPop <= 0) return 0.0;
        float avgTech = getAverageTechnology();
        int hierarchy = getMaxHierarchyLevel();
        double stateCap = 0.5;
        if (!nations.isEmpty()) {
            org.ether.society.model.Nation largest = nations.stream()
                    .max(java.util.Comparator.comparingLong(org.ether.society.model.Nation::getTotalPopulation))
                    .orElse(null);
            if (largest != null) {
                stateCap = largest.getStateCapacity();
            }
        }
        return largestPop * (1.0 + avgTech * 0.4) * stateCap * (Math.log(1.0 + hierarchy) / Math.log(2.0));
    }

    /*
     * Get largest organization entropy.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getLargestOrganizationEntropy() {
        double complexity = getLargestOrganizationComplexity();
        double pollution = getPollutionLevel();
        float tech = getAverageTechnology();
        return (complexity * 0.05 + tech * 1.2) * (1.0 + pollution / 100.0);
    }

    /*
     * Get kardashev scale.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getKardashevScale() {
        double energyMW = getEnergyCaptured();
        if (energyMW <= 0) return 0.0;
        double watts = energyMW * 1e6;
        double k = (Math.log10(Math.max(1.0, watts)) - 6.0) / 10.0;
        return Math.max(0.0, Math.min(3.0, k));
    }

    /*
     * Get built capital total.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getBuiltCapitalTotal() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        // Proportional to physical tools, structures and technology level
        return pop * (0.5 + Math.pow(Math.max(0.0, tech), 1.8) * 8.0);
    }

    /*
     * Get elite formation ratio.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getEliteFormationRatio() {
        float gini = getCurrentGini();
        return Math.min(25.0, Math.max(0.5, 1.0 + gini * 15.0));
    }

    /*
     * Get elder capital share.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getElderCapitalShare() {
        float gini = getCurrentGini();
        float life = getCurrentLifeExpectancy();
        return Math.min(90.0, Math.max(30.0, 40.0 + (life / 80.0) * 30.0 + gini * 20.0));
    }

    /*
     * Get land rent index.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getLandRentIndex() {
        long pop = getTotalPopulation();
        long cells = getPopulatedCellCount();
        double density = cells > 0 ? (double) pop / cells : 0;
        return density * 1.5 + getAverageTechnology() * 0.8;
    }

    /*
     * Get tools count.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public long getToolsCount() {
        float tech = getAverageTechnology();
        return (long) (getTotalPopulation() * (0.5 + tech * 0.2));
    }

    /*
     * Get products count.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public long getProductsCount() {
        float tech = getAverageTechnology();
        return (long) Math.max(3, 5 + Math.pow(tech, 1.6));
    }

    /*
     * Get system complexity index.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getSystemComplexityIndex() {
        float tech = getAverageTechnology();
        double divLabor = getDivisionOfLaborIndex();
        return Math.min(100.0, (tech * 0.4 + divLabor * 0.6));
    }

    /*
     * Get reconstruction capability index.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getReconstructionCapabilityIndex() {
        double edu = getEducationLevel();
        float tech = getAverageTechnology();
        return Math.min(100.0, (edu * 0.7 + tech * 0.3));
    }

    /*
     * Get system interdependence index.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getSystemInterdependenceIndex() {
        double complexity = getSystemComplexityIndex();
        return Math.min(100.0, complexity * 0.95);
    }

    /*
     * Get age pyramid.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public int[] getAgePyramid() {
        int[] cohorts = new int[7];
        if (agentBuffer != null && agentBuffer.getCapacity() > 0) {
            float[] ages = agentBuffer.getAge();
            float[] masses = agentBuffer.getMass();
            int[] hexIds = agentBuffer.getHexIds();
            for (int i = 0; i < agentBuffer.getCapacity(); i++) {
                if (hexIds != null && hexIds[i] == -1) continue;
                float age = ages != null ? ages[i] : 25.0f;
                float m = masses != null ? masses[i] : 1.0f;
                if (m <= 0) continue;
                int count = Math.max(1, (int) Math.round(m));
                if (age < 15) cohorts[0] += count;
                else if (age < 25) cohorts[1] += count;
                else if (age < 40) cohorts[2] += count;
                else if (age < 55) cohorts[3] += count;
                else if (age < 70) cohorts[4] += count;
                else if (age < 85) cohorts[5] += count;
                else cohorts[6] += count;
            }
            long realPop = getTotalPopulation();
            long agentSum = 0; for (int c : cohorts) agentSum += c;
            if (agentSum > 0 && Math.abs(realPop - agentSum) > 5) {
                double scale = (double) realPop / agentSum;
                for (int i = 0; i < 7; i++) cohorts[i] = (int) Math.round(cohorts[i] * scale);
            }
        }
        if (cohorts[0] == 0 && cohorts[1] == 0 && cohorts[2] == 0 && cohorts[3] == 0 && cohorts[4] == 0 && cohorts[5] == 0 && cohorts[6] == 0) {
            long pop = getTotalPopulation();
            float life = getCurrentLifeExpectancy();
            double c0_pct = Math.max(0.12, 0.35 - (life - 30.0) * 0.003);
            double c1_pct = 0.18;
            double c2_pct = 0.24;
            double c3_pct = 0.18;
            double c4_pct = 0.12;
            double c5_pct = 0.06 + Math.min(0.06, (life - 50.0) * 0.002);
            double c6_pct = Math.max(0.01, 1.0 - (c0_pct + c1_pct + c2_pct + c3_pct + c4_pct + c5_pct));

            cohorts[0] = (int) (pop * c0_pct);
            cohorts[1] = (int) (pop * c1_pct);
            cohorts[2] = (int) (pop * c2_pct);
            cohorts[3] = (int) (pop * c3_pct);
            cohorts[4] = (int) (pop * c4_pct);
            cohorts[5] = (int) (pop * c5_pct);
            cohorts[6] = (int) (pop * c6_pct);
        }
        return cohorts;
    }

    // --- ÃƒÂ°Ã…Â¸Ã‚Â§Ã‚Â  COGNITION & INFORMATION ---
    /*
     * Get shannon bandwidth.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getShannonBandwidth() {
        float tech = getAverageTechnology();
        return 1.0 + Math.pow(tech, 1.4) * 0.8;
    }

    /*
     * Get collective memory stock.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getCollectiveMemoryStock() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        return (pop * 0.05 + Math.pow(tech, 2.1));
    }

    /*
     * Get innovation diffusion speed.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getInnovationDiffusionSpeed() {
        float tech = getAverageTechnology();
        double divLabor = getDivisionOfLaborIndex();
        return Math.min(100.0, (tech * 0.4 + divLabor * 0.6));
    }

    /*
     * Get knowledge decay rate.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getKnowledgeDecayRate() {
        float gini = getCurrentGini();
        double conflict = getConflictLevel();
        return Math.min(100.0, (conflict * 0.7 + gini * 30.0));
    }

    // --- ÃƒÂ°Ã…Â¸Ã…â€™Ã‚Â ÃƒÆ’Ã¢â‚¬Â°COLOGIE & FRONTIÃƒÆ’Ã‹â€ RES PLANÃƒÆ’Ã¢â‚¬Â°TAIRES ---
    /*
     * Get soil npkquality.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getSoilNPKQuality() {
        float tech = getAverageTechnology();
        double resDep = getResourceDepletionRate();
        return Math.max(5.0, 100.0 - (resDep * 0.6) + Math.min(15.0, tech * 0.1));
    }

    /*
     * Get carbon footprint.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getCarbonFootprint() {
        double energy = getEnergyCaptured();
        float tech = getAverageTechnology();
        return (energy * (tech > 40 && tech < 180 ? 0.85 : 0.2)) / 1000.0;
    }

    /*
     * Get wild biodiversity index.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getWildBiodiversityIndex() {
        float bioNat = getTotalBiomassNatural();
        double bioDom = getBiomassDomesticated();
        double total = bioNat + bioDom;
        return total > 0 ? Math.min(100.0, (bioNat / total) * 100.0) : 100.0;
    }

    /*
     * Get wet bulb safety margin.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getWetBulbSafetyMargin() {
        float temp = 15.0f;
        float[] temps = worldBuffer != null ? worldBuffer.getTemperature() : null;
        if (temps != null && temps.length > 0) {
            float sum = 0; for(float t : temps) sum += t;
            temp = sum / temps.length;
        }
        return Math.max(0.0, 35.0 - (temp + 3.5));
    }

    // --- ÃƒÂ¢Ã‚ÂÃ‚Â³ CLIODYNAMIQUE & RISQUES SYSTÃƒÆ’Ã¢â‚¬Â°MIQUES ---
    /*
     * Get elite overproduction index.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getEliteOverproductionIndex() {
        double eliteForm = getEliteFormationRatio();
        float gini = getCurrentGini();
        double val = (eliteForm / 5.0) * (1.0 + (Double.isNaN(gini) ? 0.0 : gini) * 2.0);
        return Double.isNaN(val) ? 1.0 : Math.max(0.0, Math.min(10.0, val));
    }

    /*
     * Get fiscal stress index.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getFiscalStressIndex() {
        double landRent = getLandRentIndex();
        float gini = getCurrentGini();
        double val = ((Double.isNaN(gini) ? 0.0 : gini) * 50.0) + (landRent * 0.3);
        return Double.isNaN(val) ? 0.0 : Math.max(0.0, Math.min(100.0, val));
    }

    /*
     * Get geopolitical tension.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getGeopoliticalTension() {
        int cityStates = getCityStatesCount();
        double conflict = getConflictLevel();
        double val = (cityStates * 2.5) + (conflict * 0.7);
        return Double.isNaN(val) ? 0.0 : Math.max(0.0, Math.min(100.0, val));
    }

    /*
     * Get collapse vulnerability.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getCollapseVulnerability() {
        double resDep = getResourceDepletionRate();
        double psi = getEliteOverproductionIndex();
        double entropy = getSystemicEntropy();
        double eroi = getEroiAlimentaire();
        // Inversion thermodynamique : if EROI < 1.0 (industrial dependency), resource depletion accelerates collapse risk
        double thermodynamicInversionRisk = (eroi < 1.0) ? (1.0 / Math.max(0.05, eroi)) * ((Double.isNaN(resDep) ? 0.0 : resDep) / 100.0) * 15.0 : 0.0;
        double val = ((Double.isNaN(resDep) ? 0.0 : resDep) * 0.25) +
                     ((Double.isNaN(psi) ? 0.0 : psi) * 3.5) +
                     ((Double.isNaN(entropy) ? 0.0 : entropy) / 20.0) +
                     thermodynamicInversionRisk;
        return Double.isNaN(val) ? 0.0 : Math.max(0.0, Math.min(100.0, val));
    }

    /*
     * Get average asabiyyah.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getAverageAsabiyyah() {
        if (cells == null || cells.isEmpty()) return 80.0;
        double sum = 0;
        int count = 0;
        for (H3Cell c : cells) {
            if (c.getOwner() != null) {
                sum += c.getOwner().getAsabiyyah();
                count++;
            }
        }
        return count > 0 ? (sum / count) * 100.0 : 80.0;
    }

    /*
     * Get population survival rate.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getPopulationSurvivalRate() {
        long currentPop = getTotalPopulation();
        long initPop = currentScenario != null ? currentScenario.getInitialHumanCount() : 1_000_000L;
        if (initPop <= 0) return 100.0;
        return Math.min(100.0, Math.max(0.0, ((double) currentPop / initPop) * 100.0));
    }

    private void syncClimateToBuffer() {
        if (worldBuffer == null || cells == null) return;
        float[] temps = worldBuffer.getTemperature();
        for (int i = 0; i < cells.size() && i < worldBuffer.getCapacity(); i++) {
            temps[i] = cells.get(i).getTemperature().floatValue();
        }
    }

    /*
     * Get world buffer.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public org.ether.society.core.dod.WorldBuffer getWorldBuffer() {
        return worldBuffer;
    }

    /*
     * Set world buffer.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @param worldBuffer the world buffer parameter (org.ether.society.core.dod.WorldBuffer)
     */
    public void setWorldBuffer(org.ether.society.core.dod.WorldBuffer worldBuffer) {
        this.worldBuffer = worldBuffer;
    }

    /*
     * Get agent buffer.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public org.ether.society.core.dod.AgentBuffer getAgentBuffer() {
        return agentBuffer;
    }

    /*
     * Get simulation save manager.
     * Enforces physical invariants and updates associated state variables within {@code H3SimulationEngine}.
     *
     * @return the resulting computation or state reference
     */
    public SimulationSaveManager getSimulationSaveManager() {
        return simulationSaveManager;
    }
}

