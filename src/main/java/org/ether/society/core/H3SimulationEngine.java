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
import org.ether.society.core.dod.StatisticsKernel;
import org.ether.society.core.dod.UrbanKernel;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.profiling.SimulationProfiler;
import org.ether.society.core.telemetry.CompiledTelemetryPlan;
import org.ether.society.core.telemetry.TelemetryAstCompiler;
import org.ether.society.core.telemetry.TelemetryEvaluationResult;
import org.ether.society.core.telemetry.TelemetrySubscription;
import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
import org.ether.society.events.EventSystem;
import org.ether.society.events.SimulationTickListener;
import org.ether.society.model.Scenario;
import org.ether.society.model.ScenarioTimeline;
import org.ether.society.network.ClusterManager;
import org.ether.society.persistence.SimulationSaveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <h1>H3SimulationEngine</h1>
 * <p>
 * Core operational simulation engine for the Ether planetary and civilizational framework.<br>
 * Orchestrates the multi-scale simulation loop, Data-Oriented Design (DOD) kernels, Tier 1/2
 * physical and cliodynamic pipelines, event triggers, and historical snapshot recording.
 * </p>
 * <p>
 * Lifecycle and tick execution are coordinated via {@link H3EngineLifecycleAndTickCoordinator},
 * buffer synchronization and scenario shocks via {@link H3BufferAndEventCoordinator}, and
 * macro-social/thermodynamic telemetry via {@link SimulationMetricsAggregator}.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see H3EngineLifecycleAndTickCoordinator
 * @see H3BufferAndEventCoordinator
 * @see SimulationPipeline
 * @see SimulationMetricsAggregator
 */
public final class H3SimulationEngine implements ISimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(H3SimulationEngine.class);

    final Configuration config;
    final TimeManager timeManager;
    final EventSystem eventSystem;
    final HistoryManager historyManager;
    final List<Polity> nations = new CopyOnWriteArrayList<>();
    final SimulationSaveManager simulationSaveManager;

    // DOD Layer
    final SimulationProfiler profiler = new SimulationProfiler();
    WorldBuffer worldBuffer;
    AgentBuffer agentBuffer;
    final DemographicKernel demographicKernel;
    final UrbanKernel urbanKernel;
    final CultureKernel cultureKernel;
    final FluxKernel fluxKernel;
    final EnvironmentalKernel environmentalKernel;
    final StatisticsKernel statisticsKernel;
    final SimulationPipeline simulationPipeline;
    final SimulationMetricsAggregator metricsAggregator;

    long lastTickTime = 0;
    double currentTPS = 0;
    float currentGini = 0;
    float currentGDP = 0;
    float currentLifeExpectancy = 0;
    float currentFertility = 0;
    int[] densityDistribution = new int[20];

    // High-performance single-pass global aggregate cache (invalidated per tick)
    long cachedTotalPopulation = -1;
    double cachedTotalFood = -1;
    double cachedPotableWater = -1;
    float cachedBiomassNatural = -1;
    float cachedAverageTechnology = -1;
    double cachedResourceCapital = -1;
    long lastCacheTick = -1;

    List<H3Cell> cells;
    Scenario currentScenario;
    final Set<String> firedScenarioEventKeys = new HashSet<>();
    SimulationPerformanceConfig performanceConfig = new SimulationPerformanceConfig(true);
    ClusterManager clusterManager;
    private ScheduledExecutorService executorService;
    final AtomicBoolean running = new AtomicBoolean(false);
    final AtomicBoolean pauseAtNextEvent = new AtomicBoolean(false);
    private double speedMultiplier = 1.0;
    Runnable onTickCallback;
    int tickCounter = 0;
    final List<SimulationTickListener> tickListeners = new CopyOnWriteArrayList<>();

    private TemporalScale temporalScale = TemporalScale.DAILY;

    final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Ether-AutoSave-Thread");
        t.setDaemon(true);
        return t;
    });
    long lastAutoSaveTimeMs = 0;

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
        H3EngineLifecycleAndTickCoordinator.initializeFromScenario(this, scenario, cells);
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
        H3EngineLifecycleAndTickCoordinator.setCells(this, newCells);
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
        long delay = (speedMultiplier >= 999.0)
                ? 1L
                : Math.max(1L, Math.round(config.simulation().tickRateMs() / Math.max(0.01, speedMultiplier)));
        executorService.scheduleWithFixedDelay(this::tick, 0, delay, TimeUnit.MILLISECONDS);
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

    public ClusterManager getClusterManager() { return clusterManager; }
    public void setOnTickCallback(Runnable callback) { this.onTickCallback = callback; }

    public void addTickListener(SimulationTickListener listener) {
        if (listener != null) tickListeners.add(listener);
    }

    public void removeTickListener(SimulationTickListener listener) {
        if (listener != null) tickListeners.remove(listener);
    }

    public List<SimulationTickListener> getTickListeners() {
        return Collections.unmodifiableList(tickListeners);
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
        H3EngineLifecycleAndTickCoordinator.executeTick(this);
    }

    @Override
    public void stepForward(int ticks) {
        H3EngineLifecycleAndTickCoordinator.stepForward(this, ticks);
    }

    @Override
    public void stepBackward(int ticks) {
        pause();
        if (historyManager == null || cells == null || cells.isEmpty()) return;
        long currentTicks = timeManager.getTotalTicks();
        seekToTick(Math.max(0, currentTicks - ticks));
    }

    @Override
    public void seekToEnd() {
        pause();
        if (historyManager != null && !historyManager.getWorldSnapshots().isEmpty()) {
            seekToTick(historyManager.getWorldSnapshots().lastKey());
        } else if (currentScenario != null) {
            long initialYear = currentScenario.getStartDateYear();
            long endYear = currentScenario.getEndDateYear();
            int stepDaysVal = (currentScenario.getTemporalResolutionDays() > 0)
                    ? (int) Math.round(currentScenario.getTemporalResolutionDays()) : 1;
            long totalDays = Math.max(0, (endYear - initialYear) * 365L);
            seekToTick(totalDays / stepDaysVal);
        }
    }

    /**
     * Seeks simulation state and world snapshots to the specified tick index.
     *
     * @param targetTicks target tick index
     */
    public void seekToTick(long targetTicks) {
        H3EngineLifecycleAndTickCoordinator.seekToTick(this, targetTicks);
    }

    /** Synchronizes DOD {@link WorldBuffer} arrays into {@link H3Cell} entity instances. */
    public void syncBufferToCells() {
        H3BufferAndEventCoordinator.syncBufferToCells(worldBuffer, cells, performanceConfig);
    }

    /** Synchronizes domain states from {@link H3Cell} objects back into {@link WorldBuffer} and {@link AgentBuffer}. */
    public void syncCellsToBuffer() {
        H3BufferAndEventCoordinator.syncCellsToBuffer(worldBuffer, agentBuffer, cells, performanceConfig);
    }

    /** Updates cached scalar aggregates in a single fused $O(N)$ cache-aligned pass over {@link WorldBuffer}. */
    public void updateGlobalAggregatesCache() {
        H3EngineLifecycleAndTickCoordinator.updateGlobalAggregatesCache(this);
    }

    public long getTotalPopulation() {
        if (lastCacheTick == tickCounter && cachedTotalPopulation >= 0) return cachedTotalPopulation;
        if (worldBuffer == null) return 0;
        double total = 0.0;
        float[] pop = worldBuffer.getBiomassHuman();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) {
            if (pop[i] > 0.0f) total += pop[i];
        }
        return Math.round(total);
    }

    public double getTotalFood() {
        if (lastCacheTick == tickCounter && cachedTotalFood >= 0) return cachedTotalFood;
        if (worldBuffer == null) return 0;
        double total = 0;
        float[] food = worldBuffer.getFoodResource();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) total += food[i];
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
        if (lastCacheTick == tickCounter && cachedBiomassNatural >= 0) return cachedBiomassNatural;
        if (worldBuffer == null) return 0;
        float total = 0;
        float[] bio = worldBuffer.getBiomassNatural();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) total += bio[i];
        return total;
    }

    public float getAverageTechnology() {
        if (metricsAggregator != null) {
            return (float) metricsAggregator.getEmergentTechnologyLevel();
        }
        return 1.0f;
    }

    public float getCurrentGini() { return currentGini; }
    public int[] getDensityDistribution() { return densityDistribution; }
    public float getCurrentGDP() { return currentGDP; }
    public float getCurrentLifeExpectancy() { return currentLifeExpectancy; }
    public float getCurrentFertility() { return currentFertility; }

    // --- EXTENDED CLIODYNAMIC & PHYSICAL METRICS (Delegated to SimulationMetricsAggregator) ---
    public double getEnergyCaptured() { return metricsAggregator.getEnergyCaptured(); } public double getResourceDepletionRate() { return metricsAggregator.getResourceDepletionRate(); }
    public double getEnergyPerCapita() { return metricsAggregator.getEnergyPerCapita(); } public double getFoodPerCapita() { return metricsAggregator.getFoodPerCapita(); }
    public double getBiomassDomesticated() { return metricsAggregator.getBiomassDomesticated(); } public double getPotableWaterTotal() { return metricsAggregator.getPotableWaterTotal(); }
    public double getRemainingResourcesRatio() { return metricsAggregator.getRemainingResourcesRatio(); } public double getSystemicEntropy() { return metricsAggregator.getSystemicEntropy(); }
    public double getPollutionLevel() { return metricsAggregator.getPollutionLevel(); } public double getOccupiedTerritoryArea() { return metricsAggregator.getOccupiedTerritoryArea(); }
    public double getOffspringPercentage() { return metricsAggregator.getOffspringPercentage(); } public double getAgeAtFirstChild() { return metricsAggregator.getAgeAtFirstChild(); }
    public double getImmigrationRate() { return metricsAggregator.getImmigrationRate(); } public double getEducationLevel() { return metricsAggregator.getEducationLevel(); }
    public double getHappinessIndex() { return metricsAggregator.getHappinessIndex(); } public double getConflictLevel() { return metricsAggregator.getConflictLevel(); }
    public int getCityStatesCount() { return metricsAggregator.getCityStatesCount(); } public double getInstitutionalMaturity() { return metricsAggregator.getInstitutionalMaturity(); }
    public double getDivisionOfLaborIndex() { return metricsAggregator.getDivisionOfLaborIndex(); } public double getEroiAlimentaire() { return metricsAggregator.getEroiAlimentaire(); }
    public double getNetSurplusFraction() { return metricsAggregator.getNetSurplusFraction(); } public double getTrophicMultiplier() { return metricsAggregator.getTrophicMultiplier(); }
    public double getBiomassMobilizedPerCapitaKg() { return metricsAggregator.getBiomassMobilizedPerCapitaKg(); } public int getMaxHierarchyLevel() { return metricsAggregator.getMaxHierarchyLevel(); }
    public long getLargestCulturalUnitSize() { return metricsAggregator.getLargestCulturalUnitSize(); } public double getLargestOrganizationComplexity() { return metricsAggregator.getLargestOrganizationComplexity(); }
    public double getLargestOrganizationEntropy() { return metricsAggregator.getLargestOrganizationEntropy(); } public double getKardashevScale() { return metricsAggregator.getKardashevScale(); }
    public double getBuiltCapitalTotal() { return metricsAggregator.getBuiltCapitalTotal(); } public double getEliteFormationRatio() { return metricsAggregator.getEliteFormationRatio(); }
    public double getElderCapitalShare() { return metricsAggregator.getElderCapitalShare(); } public double getLandRentIndex() { return metricsAggregator.getLandRentIndex(); }
    public long getToolsCount() { return metricsAggregator.getToolsCount(); } public long getProductsCount() { return metricsAggregator.getProductsCount(); }
    public double getSystemComplexityIndex() { return metricsAggregator.getSystemComplexityIndex(); } public double getReconstructionCapabilityIndex() { return metricsAggregator.getReconstructionCapabilityIndex(); }
    public double getSystemInterdependenceIndex() { return metricsAggregator.getSystemInterdependenceIndex(); } public int[] getAgePyramid() { return metricsAggregator.getAgePyramid(); }
    public double getShannonBandwidth() { return metricsAggregator.getShannonBandwidth(); } public double getCollectiveMemoryStock() { return metricsAggregator.getCollectiveMemoryStock(); }
    public double getInnovationDiffusionSpeed() { return metricsAggregator.getInnovationDiffusionSpeed(); } public double getKnowledgeDecayRate() { return metricsAggregator.getKnowledgeDecayRate(); }
    public double getSoilNPKQuality() { return metricsAggregator.getSoilNPKQuality(); } public double getCarbonFootprint() { return metricsAggregator.getCarbonFootprint(); }
    public double getWildBiodiversityIndex() { return metricsAggregator.getWildBiodiversityIndex(); } public double getWetBulbSafetyMargin() { return metricsAggregator.getWetBulbSafetyMargin(); }
    public double getEliteOverproductionIndex() { return metricsAggregator.getEliteOverproductionIndex(); } public double getFiscalStressIndex() { return metricsAggregator.getFiscalStressIndex(); }
    public double getGeopoliticalTension() { return metricsAggregator.getGeopoliticalTension(); } public double getCollapseVulnerability() { return metricsAggregator.getCollapseVulnerability(); }
    public double getAverageAsabiyyah() { return metricsAggregator.getAverageAsabiyyah(); } public double getPopulationSurvivalRate() { return metricsAggregator.getPopulationSurvivalRate(); }
    public double getUrbanizationRate() { return metricsAggregator.getUrbanizationRate(); } public double getLiteracyRate() { return metricsAggregator.getLiteracyRate(); }
    public double getAtmosphericCO2Ppm() { return metricsAggregator.getAtmosphericCO2Ppm(); } public double getTemperatureAnomaly() { return metricsAggregator.getTemperatureAnomaly(); }

    private boolean headlessTelemetrySubscribed = false;
    private TelemetrySubscription telemetrySubscription = TelemetrySubscription.NONE;
    private CompiledTelemetryPlan compiledTelemetryPlan = CompiledTelemetryPlan.NO_OP;
    private TelemetryEvaluationResult cachedTelemetryResult = TelemetryEvaluationResult.EMPTY;
    private long lastTelemetryTick = -1L;
    long initialPopulationBaseline = -1L;
    double initialCo2Baseline = 280.0;

    /** @return {@code true} if headless or GUI telemetry subscription is active */
    public boolean isHeadlessTelemetrySubscribed() { return headlessTelemetrySubscribed; }

    /**
     * Enables or disables headless telemetry using canonical benchmark variables when enabling
     * from an empty subscription, or clearing to {@link TelemetrySubscription#NONE} when disabling.
     *
     * @param subscribed enable or disable telemetry evaluation
     */
    public void setHeadlessTelemetrySubscribed(boolean subscribed) {
        if (subscribed) {
            if (this.telemetrySubscription.isEmpty()) {
                setTelemetrySubscription(TelemetrySubscription.allCanonicalBenchmarks(1));
            } else {
                this.headlessTelemetrySubscribed = true;
            }
        } else {
            setTelemetrySubscription(TelemetrySubscription.NONE);
        }
    }

    /** @return immutable active telemetry subscription */
    public TelemetrySubscription getTelemetrySubscription() {
        return telemetrySubscription;
    }

    /**
     * Replaces the active telemetry subscription and compiles a minimal DAG execution plan.
     * Passing {@code null} or an empty subscription sets zero-cost {@link TelemetrySubscription#NONE}.
     *
     * @param subscription metric and spatial query contract
     */
    public void setTelemetrySubscription(TelemetrySubscription subscription) {
        this.telemetrySubscription = (subscription != null) ? subscription : TelemetrySubscription.NONE;
        this.compiledTelemetryPlan = TelemetryAstCompiler.compile(this.telemetrySubscription);
        this.headlessTelemetrySubscribed = !this.telemetrySubscription.isEmpty();
        this.lastTelemetryTick = -1L;
        if (this.telemetrySubscription.isEmpty()) {
            this.cachedTelemetryResult = TelemetryEvaluationResult.EMPTY;
        }
    }

    /** @return pre-compiled topological DAG evaluation plan */
    public CompiledTelemetryPlan getCompiledTelemetryPlan() {
        return compiledTelemetryPlan;
    }

    /** @return most recent memoized telemetry evaluation output (or {@link TelemetryEvaluationResult#EMPTY}) */
    public TelemetryEvaluationResult getCachedTelemetryResult() {
        return cachedTelemetryResult;
    }

    /** @return simulation tick index at which {@link #cachedTelemetryResult} was last evaluated */
    public long getLastTelemetryTick() {
        return lastTelemetryTick;
    }

    /**
     * Evaluates the currently compiled {@link CompiledTelemetryPlan} once against the current engine state
     * and memoizes scalar and kernel outputs for instant reuse across UI cards and benchmark exporters.
     *
     * @return immutable evaluated telemetry result
     */
    public TelemetryEvaluationResult evaluateTelemetryNow() {
        if (compiledTelemetryPlan.isNoOp() || worldBuffer == null) {
            return TelemetryEvaluationResult.EMPTY;
        }
        long startYr = currentScenario != null ? currentScenario.getStartDateYear() : timeManager.getCurrentYear();
        long curYr = timeManager.getCurrentYear();
        double elapsedYrs = Math.max(0.0, (double) (curYr - startYr));
        TelemetryEvaluationResult res = compiledTelemetryPlan.execute(
                worldBuffer,
                agentBuffer,
                cells,
                nations,
                statisticsKernel,
                tickCounter,
                curYr,
                initialPopulationBaseline,
                initialCo2Baseline,
                elapsedYrs,
                currentTPS);

        this.cachedTelemetryResult = res;
        this.lastTelemetryTick = tickCounter;
        this.lastCacheTick = tickCounter;
        this.cachedTotalPopulation = res.getTotalPopulation();
        this.cachedTotalFood = res.getTotalFood();
        this.cachedPotableWater = res.getPotableWater();
        this.cachedBiomassNatural = (float) res.getNaturalBiomass();
        this.cachedAverageTechnology = (float) res.getAvgTech();
        this.cachedResourceCapital = res.getBuiltCapitalTotal();
        if (res.getGdp() > 0.0f) this.currentGDP = res.getGdp();
        if (res.getGini() > 0.0f) this.currentGini = res.getGini();
        if (res.getLifeExpectancy() > 0.0f) this.currentLifeExpectancy = res.getLifeExpectancy();
        if (res.getFertility() > 0.0f) this.currentFertility = res.getFertility();
        if (res.getDensityDistribution().length > 0) this.densityDistribution = res.getDensityDistribution();
        return res;
    }

    public WorldBuffer getWorldBuffer() { return worldBuffer; }
    public void setWorldBuffer(WorldBuffer worldBuffer) { this.worldBuffer = worldBuffer; }
    public AgentBuffer getAgentBuffer() { return agentBuffer; }
    public SimulationSaveManager getSimulationSaveManager() { return simulationSaveManager; }
    public FluxKernel getFluxKernel() { return fluxKernel; }

    public long getLastCacheTick() { return lastCacheTick; }
    public double getCachedPotableWater() { return cachedPotableWater; }
    public double getCachedResourceCapital() { return cachedResourceCapital; }
    public SimulationMetricsAggregator getMetricsAggregator() { return metricsAggregator; }
}
