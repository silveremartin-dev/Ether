/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.analytics.HistoryManager;
import org.ether.society.config.Configuration;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.CultureKernel;
import org.ether.society.core.dod.DemographicKernel;
import org.ether.society.core.dod.EnvironmentalKernel;
import org.ether.society.core.dod.FluxKernel;
import org.ether.society.core.dod.NativeRustBridge;
import org.ether.society.core.dod.StatisticsKernel;
import org.ether.society.core.dod.UrbanKernel;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.profiling.SimulationProfiler;
import org.ether.society.data.DODDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
import org.ether.society.events.DomainEventBus;
import org.ether.society.events.EventSystem;
import org.ether.society.events.SimulationTickEvent;
import org.ether.society.events.SimulationTickListener;
import org.ether.society.model.Scenario;
import org.ether.society.model.ScenarioTimeline;
import org.ether.society.network.ClusterManager;
import org.ether.society.network.spatial.H3SpatialPartitioner;
import org.ether.society.persistence.SimulationSaveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

/**
 * <h1>H3SimulationEngine</h1>
 * <p>
 * Core operational simulation engine for the Ether planetary and civilizational framework.<br>
 * Orchestrates the multi-scale simulation loop, Data-Oriented Design (DOD) kernels, Tier 1/2
 * physical and cliodynamic pipelines, event triggers, and historical snapshot recording.
 * </p>
 * <p>
 * Buffer synchronization, scenario event shock waves, and political seeding are delegated to
 * {@link H3BufferAndEventCoordinator}; macro-social and thermodynamic telemetry is delegated to
 * {@link SimulationMetricsAggregator}.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see H3BufferAndEventCoordinator
 * @see SimulationPipeline
 * @see SimulationMetricsAggregator
 */
public class H3SimulationEngine implements ISimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(H3SimulationEngine.class);

    private final Configuration config;
    private final TimeManager timeManager;
    private final EventSystem eventSystem;
    private final HistoryManager historyManager;
    private final List<Polity> nations = new CopyOnWriteArrayList<>();
    private final SimulationSaveManager simulationSaveManager;

    // DOD Layer
    private final SimulationProfiler profiler = new SimulationProfiler();
    private WorldBuffer worldBuffer;
    private AgentBuffer agentBuffer;
    private final DemographicKernel demographicKernel;
    private final UrbanKernel urbanKernel;
    private final CultureKernel cultureKernel;
    private final FluxKernel fluxKernel;
    private final EnvironmentalKernel environmentalKernel;
    private final StatisticsKernel statisticsKernel;
    private final SimulationPipeline simulationPipeline;
    private final SimulationMetricsAggregator metricsAggregator;

    private long lastTickTime = 0;
    private double currentTPS = 0;
    private float currentGini = 0;
    private float currentGDP = 0;
    private float currentLifeExpectancy = 0;
    private float currentFertility = 0;
    private int[] densityDistribution = new int[20];

    // High-performance single-pass global aggregate cache (invalidated per tick)
    private long cachedTotalPopulation = -1;
    private double cachedTotalFood = -1;
    private double cachedPotableWater = -1;
    private float cachedBiomassNatural = -1;
    private float cachedAverageTechnology = -1;
    private double cachedResourceCapital = -1;
    private long lastCacheTick = -1;

    private List<H3Cell> cells;
    private Scenario currentScenario;
    private final Set<String> firedScenarioEventKeys = new HashSet<>();
    private SimulationPerformanceConfig performanceConfig = new SimulationPerformanceConfig(true);
    private ClusterManager clusterManager;
    private ScheduledExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean pauseAtNextEvent = new AtomicBoolean(false);
    private double speedMultiplier = 1.0;
    private Runnable onTickCallback;
    private int tickCounter = 0;

    /**
     * Temporal stepping scale mode for simulation ticks.
     */
    public enum TemporalScale {
        DAILY(1, "📅 Pas Quotidien (Jour par Jour - Détaillé)"),
        MONTHLY(30, "🚀 Pas Mensuel (Mois par Mois - Mode Rapide)");

        private final int factor;
        private final String label;

        TemporalScale(int factor, String label) {
            this.factor = factor;
            this.label = label;
        }

        public int getFactor() { return factor; }
        public String getLabel() { return label; }

        @Override
        public String toString() { return label; }
    }

    private TemporalScale temporalScale = TemporalScale.DAILY;

    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Ether-AutoSave-Thread");
        t.setDaemon(true);
        return t;
    });
    private long lastAutoSaveTimeMs = 0;

    /**
     * Constructs a new {@link H3SimulationEngine} with the specified configuration.
     *
     * @param config global simulation configuration
     */
    public H3SimulationEngine(Configuration config) {
        this.config = config;
        this.timeManager = new TimeManager(config.simulation().startYear());
        this.eventSystem = new EventSystem();
        this.historyManager = new HistoryManager();
        this.simulationSaveManager = new SimulationSaveManager();
        this.demographicKernel = new DemographicKernel();
        this.urbanKernel = new UrbanKernel();
        this.cultureKernel = new CultureKernel();
        this.environmentalKernel = new EnvironmentalKernel();
        this.statisticsKernel = new StatisticsKernel();
        this.fluxKernel = new FluxKernel();
        this.simulationPipeline = new SimulationPipeline(
                demographicKernel, urbanKernel, cultureKernel, environmentalKernel, statisticsKernel, fluxKernel);
        this.metricsAggregator = new SimulationMetricsAggregator(this);
        initialize();
    }

    public EventSystem getEventSystem() { return eventSystem; }

    public ScenarioTimeline getTimeline() {
        return eventSystem != null ? eventSystem.getTimeline() : null;
    }

    public void setTimeline(ScenarioTimeline timeline) {
        if (eventSystem != null) {
            eventSystem.setTimeline(timeline);
        }
    }

    private void initialize() {
        logger.info("Initializing H3 simulation engine (idle state)...");
        this.cells = new ArrayList<>();
        nations.clear();
        this.worldBuffer = new WorldBuffer(0);
        this.agentBuffer = new AgentBuffer(0);
    }

    /**
     * Initializes the engine state, spatial Hilbert curve ordering, DOD buffers, and kernels from a scenario.
     *
     * @param scenario target scenario configuration
     * @param cells    spatial H3 cell list
     */
    public void initializeFromScenario(Scenario scenario, List<H3Cell> cells) {
        logger.info("Initializing from scenario: {}", scenario.getName());
        boolean wasRunning = running.get();
        if (wasRunning) {
            pause();
        }
        SimulationPipeline.resetGlobalSimulationState();
        this.currentScenario = scenario;
        this.performanceConfig = scenario != null ? scenario.toPerformanceConfig() : new SimulationPerformanceConfig(true);
        try {
            String hwPref = Preferences.userRoot().node("org/ether/society/ui")
                    .get("ether_hardware_mode", "JAVA_VECTOR_SIMD");
            if ("NATIVE_RUST".equalsIgnoreCase(hwPref) && NativeRustBridge.isNativeAvailable()) {
                this.performanceConfig.setExecutionEngineMode(SimulationPerformanceConfig.ExecutionEngineMode.RUST_NATIVE);
            } else if ("CPU_JIT".equalsIgnoreCase(hwPref) || "GPU_OFF".equalsIgnoreCase(hwPref)) {
                this.performanceConfig.setExecutionEngineMode(SimulationPerformanceConfig.ExecutionEngineMode.JAVA_STANDARD);
            } else {
                this.performanceConfig.setExecutionEngineMode(SimulationPerformanceConfig.ExecutionEngineMode.JAVA_VECTOR_SIMD);
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
        H3SpatialPartitioner.sortCellsByHilbertCurve(cells);

        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(cells);

        int populatedCount = 0;
        for (H3Cell c : cells) {
            if (c != null && c.getPopulation() != null && c.getPopulation() > 0) {
                populatedCount++;
            }
        }
        H3BufferAndEventCoordinator.initializePoliticalSeeding(cells, nations, populatedCount);

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
        this.worldBuffer = new WorldBuffer(cells.size());

        int cDims = currentScenario != null ? currentScenario.getCultureVectorDimensions() : 9;
        int agentCapacity = Math.min(10_000_000, Math.max(8192, Math.max(cells.size() * 24, estimatedAgents * 8)));
        this.agentBuffer = new AgentBuffer(agentCapacity, cDims);

        DODDataGenerator.populateWorldBuffer(cells, worldBuffer);
        long sSeed = currentScenario != null ? currentScenario.getSeed() : 42L;
        DODDataGenerator.initializeAgentBuffer(worldBuffer, agentBuffer, cohortSize, sSeed);

        if (clusterManager != null) {
            clusterManager.setWorldBuffer(worldBuffer);
        }
        if (historyManager != null) {
            historyManager.reset();
            historyManager.captureSnapshot(this);
            historyManager.captureWorldSnapshot(this);
        }
    }

    public void setCurrentScenario(Scenario currentScenario) { this.currentScenario = currentScenario; }
    public Scenario getCurrentScenario() { return currentScenario; }

    public SimulationPerformanceConfig getPerformanceConfig() { return performanceConfig; }
    public void setPerformanceConfig(SimulationPerformanceConfig performanceConfig) { this.performanceConfig = performanceConfig; }

    @Override
    public void start() {
        if (running.getAndSet(true)) return;
        startGameLoop();
    }

    @Override
    public void pause() {
        if (!running.getAndSet(false)) return;
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void reset() {
        pause();
        SimulationPipeline.resetGlobalSimulationState();
        firedScenarioEventKeys.clear();
        timeManager.reset(config.simulation().startYear());
        historyManager.reset();
        initialize();
    }

    @Override
    public void setSpeed(int multiplier) { setSpeed((double) multiplier); }

    @Override
    public void setSpeed(double multiplier) {
        this.speedMultiplier = Math.max(0.01, multiplier);
        if (running.get()) {
            pause();
            start();
        }
    }

    @Override
    public int getSpeed() { return (int) Math.round(speedMultiplier); }

    @Override
    public double getSpeedMultiplier() { return speedMultiplier; }

    @Override
    public TimeManager getTimeManager() { return timeManager; }

    public long getCurrentYear() { return timeManager != null ? timeManager.getCurrentYear() : 0; }

    @Override
    public boolean isRunning() { return running.get(); }

    @Override
    public void setPauseAtNextEvent(boolean pause) { this.pauseAtNextEvent.set(pause); }

    @Override
    public boolean isPauseAtNextEvent() { return this.pauseAtNextEvent.get(); }

    public List<H3Cell> getCells() { return cells; }

    /**
     * Replaces the active H3 cell list and re-initializes the DOD buffers accordingly.
     *
     * @param newCells new list of H3 cells
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
                H3BufferAndEventCoordinator.initializePopulation(newCells, currentScenario, nations);
            }
            int cohortSize = currentScenario != null && currentScenario.getTargetCohortSize() > 0 ? currentScenario.getTargetCohortSize() : 150;
            if (demographicKernel != null) {
                demographicKernel.setTargetCohortSize(cohortSize);
            }
            long initialPop = currentScenario != null ? currentScenario.getInitialHumanCount() : 1_000_000L;
            int estimatedAgents = (int) Math.max(cells.size(), Math.min(10_000_000L, initialPop / Math.max(1, cohortSize)));
            this.worldBuffer = new WorldBuffer(cells.size());
            int cDims = currentScenario != null ? currentScenario.getCultureVectorDimensions() : 9;
            int agentCapacity = Math.min(10_000_000, Math.max(8192, Math.max(cells.size() * 24, estimatedAgents * 8)));
            this.agentBuffer = new AgentBuffer(agentCapacity, cDims);
            DODDataGenerator.populateWorldBuffer(cells, worldBuffer);
            long sSeed = currentScenario != null ? currentScenario.getSeed() : 42L;
            DODDataGenerator.initializeAgentBuffer(worldBuffer, agentBuffer, cohortSize, sSeed);
        } else {
            this.worldBuffer = new WorldBuffer(0);
            this.agentBuffer = new AgentBuffer(0);
        }
    }

    public void saveSimulation(String saveName) { saveSimulation(null, saveName); }

    public void saveSimulation(String saveId, String saveName) {
        boolean wasRunning = running.get();
        if (wasRunning) pause();
        simulationSaveManager.saveSimulation(this, saveId, saveName);
        if (wasRunning) start();
    }

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
            executorService.scheduleWithFixedDelay(this::tick, 0, 1, TimeUnit.MILLISECONDS);
        } else {
            long delay = Math.max(1L, Math.round(config.simulation().tickRateMs() / Math.max(0.01, speedMultiplier)));
            executorService.scheduleWithFixedDelay(this::tick, 0, delay, TimeUnit.MILLISECONDS);
        }
    }

    public void shutdown() {
        running.set(false);
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (saveExecutor != null && !saveExecutor.isShutdown()) {
            saveExecutor.shutdownNow();
        }
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
        if (this.clusterManager != null && this.worldBuffer != null) {
            this.clusterManager.setWorldBuffer(this.worldBuffer);
        }
    }

    private final List<SimulationTickListener> tickListeners = new CopyOnWriteArrayList<>();

    public ClusterManager getClusterManager() { return clusterManager; }
    public void setOnTickCallback(Runnable callback) { this.onTickCallback = callback; }

    public void addTickListener(SimulationTickListener listener) {
        if (listener != null) {
            tickListeners.add(listener);
        }
    }

    public void removeTickListener(SimulationTickListener listener) {
        if (listener != null) {
            tickListeners.remove(listener);
        }
    }

    public List<SimulationTickListener> getTickListeners() {
        return java.util.Collections.unmodifiableList(tickListeners);
    }
    public SimulationProfiler getProfiler() { return profiler; }
    public long getTickCounter() { return tickCounter; }

    public TemporalScale getTemporalScale() { return temporalScale; }
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
            final float DT_TICK = stepDays * 86400f;

            // 1. FAST SCALE DYNAMICS
            int yearBeforeTick = timeManager.getCurrentYear();
            timeManager.advanceDays(stepDays);
            boolean yearChanged = (timeManager.getCurrentYear() != yearBeforeTick);
            profiler.endPhase("1_FastScaleFlux");

            // 2. SLOW SCALE PHYSICALIST DYNAMICS
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
                H3BufferAndEventCoordinator.checkScenarioClimateEvents(
                        currentScenario, timeManager, tickCounter, firedScenarioEventKeys,
                        eventSystem, cells, worldBuffer, agentBuffer);
                eventSystem.checkEvents(timeManager.getCurrentYear(), timeManager.getCurrentMonth(), getTotalPopulation(), getTotalFood(), cells);
                eventSystem.checkCellEvents(timeManager.getCurrentYear(), timeManager.getCurrentMonth(), cells);
                int eventCountAfter = eventSystem.peekEvents().size();

                if (pauseAtNextEvent.get() && eventCountAfter > eventCountBefore) {
                    logger.info("[Step {}] Automatic simulation pause on event (pauseAtNextEvent=true)", tickCounter);
                    pause();
                }
                profiler.endPhase("5_StatisticsAndHistory");

                profiler.beginPhase("6_BufferSync");
                profiler.endPhase("6_BufferSync");

                if (tickCounter % Math.max(1, 12 / stepDays) == 0) {
                    logger.info("[Step {}] Year {} Month {} | Pop: {} | TPS: {} it/s",
                            tickCounter, timeManager.getCurrentYear(),
                            String.format("%02d", timeManager.getCurrentMonth() + 1),
                            String.format("%,d", getTotalPopulation()),
                            String.format("%.1f", currentTPS));
                }
            }

            if (tickCounter > 0 && tickCounter % 60 == 0) {
                autoSaveCheckpoint();
            }
            tickCounter++;

            SimulationTickEvent tickEvent = new SimulationTickEvent(
                    tickCounter,
                    timeManager.getCurrentYear(),
                    timeManager.getCurrentMonth(),
                    getTotalPopulation(),
                    currentTPS,
                    currentGDP,
                    currentGini,
                    yearChanged
            );

            for (SimulationTickListener listener : tickListeners) {
                try {
                    listener.onTick(tickEvent);
                } catch (Exception ex) {
                    logger.error("Error notifying SimulationTickListener: {}", ex.getMessage(), ex);
                }
            }
            DomainEventBus.getInstance().publishNow(tickEvent);

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

    @Override
    public void stepForward(int ticks) {
        pause();
        for (int i = 0; i < Math.max(1, ticks); i++) {
            if (currentScenario != null && timeManager.getCurrentYear() > currentScenario.getEndDateYear()) {
                logger.info("stepForward terminating early: scenario target end date (Year {}) reached.", currentScenario.getEndDateYear());
                break;
            }
            running.set(true);
            try {
                tick();
            } finally {
                if (!running.get()) {
                    // tick() triggered pause() (e.g. target end date reached or event trigger)
                    break;
                }
                running.set(false);
            }
            if (currentScenario != null && timeManager.getCurrentYear() > currentScenario.getEndDateYear()) {
                logger.info("stepForward terminating: scenario target end date (Year {}) reached after tick.", currentScenario.getEndDateYear());
                break;
            }
        }
        running.set(false);
        if (historyManager != null) {
            historyManager.captureWorldSnapshot(this);
            historyManager.flushSnapshots();
        }
    }

    @Override
    public void stepBackward(int ticks) {
        pause();
        if (historyManager == null || cells == null || cells.isEmpty()) return;
        long currentTicks = timeManager.getTotalTicks();
        long targetTicks = Math.max(0, currentTicks - ticks);
        seekToTick(targetTicks);
    }

    @Override
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

    /**
     * Seeks simulation state and world snapshots to the specified tick index.
     *
     * @param targetTicks target tick index
     */
    public void seekToTick(long targetTicks) {
        pause();
        if (historyManager == null || cells == null || cells.isEmpty()) return;
        H3BufferAndEventCoordinator.restoreWorldSnapshotAtTick(historyManager, cells, worldBuffer, targetTicks);

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

    /**
     * Synchronizes DOD {@link WorldBuffer} arrays into {@link H3Cell} entity instances.
     */
    public void syncBufferToCells() {
        H3BufferAndEventCoordinator.syncBufferToCells(worldBuffer, cells, performanceConfig);
    }

    /**
     * Synchronizes domain states from individual {@link H3Cell} objects back into {@link WorldBuffer} and {@link AgentBuffer}.
     */
    public void syncCellsToBuffer() {
        H3BufferAndEventCoordinator.syncCellsToBuffer(worldBuffer, agentBuffer, cells, performanceConfig);
    }

    /**
     * Updates cached scalar aggregates in a single fused $O(N)$ cache-aligned pass over {@link WorldBuffer}.
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

    public long getPopulatedCellCount() {
        if (worldBuffer == null) return 0;
        long count = 0;
        float[] pop = worldBuffer.getBiomassHuman();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) {
            if (pop[i] > 0.1f) count++;
        }
        return count;
    }

    public List<Polity> getNations() { return nations; }
    public HistoryManager getHistoryManager() { return historyManager; }
    public double getCurrentTPS() { return currentTPS; }

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
        if (worldBuffer == null || worldBuffer.getCapacity() == 0) return 0;
        float total = 0;
        float[] tech = worldBuffer.getTechnologyLevel();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) total += tech[i];
        return total / worldBuffer.getCapacity();
    }

    public float getCurrentGini() { return currentGini; }
    public int[] getDensityDistribution() { return densityDistribution; }
    public float getCurrentGDP() { return currentGDP; }
    public float getCurrentLifeExpectancy() { return currentLifeExpectancy; }
    public float getCurrentFertility() { return currentFertility; }

    // --- EXTENDED CLIODYNAMIC & PHYSICAL METRICS (Delegated to SimulationMetricsAggregator) ---

    public double getEnergyCaptured() { return metricsAggregator.getEnergyCaptured(); }
    public double getResourceDepletionRate() { return metricsAggregator.getResourceDepletionRate(); }
    public double getEnergyPerCapita() { return metricsAggregator.getEnergyPerCapita(); }
    public double getFoodPerCapita() { return metricsAggregator.getFoodPerCapita(); }
    public double getBiomassDomesticated() { return metricsAggregator.getBiomassDomesticated(); }
    public double getPotableWaterTotal() { return metricsAggregator.getPotableWaterTotal(); }
    public double getRemainingResourcesRatio() { return metricsAggregator.getRemainingResourcesRatio(); }
    public double getSystemicEntropy() { return metricsAggregator.getSystemicEntropy(); }
    public double getPollutionLevel() { return metricsAggregator.getPollutionLevel(); }
    public double getOccupiedTerritoryArea() { return metricsAggregator.getOccupiedTerritoryArea(); }
    public double getOffspringPercentage() { return metricsAggregator.getOffspringPercentage(); }
    public double getAgeAtFirstChild() { return metricsAggregator.getAgeAtFirstChild(); }
    public double getImmigrationRate() { return metricsAggregator.getImmigrationRate(); }
    public double getEducationLevel() { return metricsAggregator.getEducationLevel(); }
    public double getHappinessIndex() { return metricsAggregator.getHappinessIndex(); }
    public double getConflictLevel() { return metricsAggregator.getConflictLevel(); }
    public int getCityStatesCount() { return metricsAggregator.getCityStatesCount(); }
    public double getInstitutionalMaturity() { return metricsAggregator.getInstitutionalMaturity(); }
    public double getDivisionOfLaborIndex() { return metricsAggregator.getDivisionOfLaborIndex(); }
    public double getEroiAlimentaire() { return metricsAggregator.getEroiAlimentaire(); }
    public double getNetSurplusFraction() { return metricsAggregator.getNetSurplusFraction(); }
    public double getTrophicMultiplier() { return metricsAggregator.getTrophicMultiplier(); }
    public double getBiomassMobilizedPerCapitaKg() { return metricsAggregator.getBiomassMobilizedPerCapitaKg(); }
    public int getMaxHierarchyLevel() { return metricsAggregator.getMaxHierarchyLevel(); }
    public long getLargestCulturalUnitSize() { return metricsAggregator.getLargestCulturalUnitSize(); }
    public double getLargestOrganizationComplexity() { return metricsAggregator.getLargestOrganizationComplexity(); }
    public double getLargestOrganizationEntropy() { return metricsAggregator.getLargestOrganizationEntropy(); }
    public double getKardashevScale() { return metricsAggregator.getKardashevScale(); }
    public double getBuiltCapitalTotal() { return metricsAggregator.getBuiltCapitalTotal(); }
    public double getEliteFormationRatio() { return metricsAggregator.getEliteFormationRatio(); }
    public double getElderCapitalShare() { return metricsAggregator.getElderCapitalShare(); }
    public double getLandRentIndex() { return metricsAggregator.getLandRentIndex(); }
    public long getToolsCount() { return metricsAggregator.getToolsCount(); }
    public long getProductsCount() { return metricsAggregator.getProductsCount(); }
    public double getSystemComplexityIndex() { return metricsAggregator.getSystemComplexityIndex(); }
    public double getReconstructionCapabilityIndex() { return metricsAggregator.getReconstructionCapabilityIndex(); }
    public double getSystemInterdependenceIndex() { return metricsAggregator.getSystemInterdependenceIndex(); }
    public int[] getAgePyramid() { return metricsAggregator.getAgePyramid(); }
    public double getShannonBandwidth() { return metricsAggregator.getShannonBandwidth(); }
    public double getCollectiveMemoryStock() { return metricsAggregator.getCollectiveMemoryStock(); }
    public double getInnovationDiffusionSpeed() { return metricsAggregator.getInnovationDiffusionSpeed(); }
    public double getKnowledgeDecayRate() { return metricsAggregator.getKnowledgeDecayRate(); }
    public double getSoilNPKQuality() { return metricsAggregator.getSoilNPKQuality(); }
    public double getCarbonFootprint() { return metricsAggregator.getCarbonFootprint(); }
    public double getWildBiodiversityIndex() { return metricsAggregator.getWildBiodiversityIndex(); }
    public double getWetBulbSafetyMargin() { return metricsAggregator.getWetBulbSafetyMargin(); }
    public double getEliteOverproductionIndex() { return metricsAggregator.getEliteOverproductionIndex(); }
    public double getFiscalStressIndex() { return metricsAggregator.getFiscalStressIndex(); }
    public double getGeopoliticalTension() { return metricsAggregator.getGeopoliticalTension(); }
    public double getCollapseVulnerability() { return metricsAggregator.getCollapseVulnerability(); }
    public double getAverageAsabiyyah() { return metricsAggregator.getAverageAsabiyyah(); }
    public double getPopulationSurvivalRate() { return metricsAggregator.getPopulationSurvivalRate(); }

    public WorldBuffer getWorldBuffer() { return worldBuffer; }
    public void setWorldBuffer(WorldBuffer worldBuffer) { this.worldBuffer = worldBuffer; }
    public AgentBuffer getAgentBuffer() { return agentBuffer; }
    public SimulationSaveManager getSimulationSaveManager() { return simulationSaveManager; }
    public FluxKernel getFluxKernel() { return fluxKernel; }

    long getLastCacheTick() { return lastCacheTick; }
    double getCachedPotableWater() { return cachedPotableWater; }
    double getCachedResourceCapital() { return cachedResourceCapital; }
    public SimulationMetricsAggregator getMetricsAggregator() { return metricsAggregator; }
}
