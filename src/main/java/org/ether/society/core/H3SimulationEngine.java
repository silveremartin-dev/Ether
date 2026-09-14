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

    // Density-based simulation systems
    private final H3ClimateSystem climateSystem;
    private final org.ether.society.agents.AgentManager agentManager;
    private final org.ether.society.gpu.GPUManager gpuManager;

    private final org.ether.society.analytics.HistoryManager historyManager;
    private final org.ether.society.diplomacy.DiplomacyManager diplomacyManager;
    private final org.ether.society.diplomacy.PoliticalSimulationEngine politicalEngine;
    private final SimulationSaveManager simulationSaveManager;

    // DOD Layer
    private final org.ether.society.core.profiling.SimulationProfiler profiler = new org.ether.society.core.profiling.SimulationProfiler();
    private org.ether.society.core.dod.WorldBuffer worldBuffer;
    private org.ether.society.core.dod.AgentBuffer agentBuffer;
    private org.ether.society.flux.FluxEngine fluxEngine;
    private org.ether.society.core.dod.DemographicKernel demographicKernel;
    private org.ether.society.core.dod.UrbanKernel urbanKernel;
    private org.ether.society.core.dod.CultureKernel cultureKernel;
    private final org.ether.society.core.dod.EnvironmentalKernel environmentalKernel;
    private final org.ether.society.core.dod.StatisticsKernel statisticsKernel;
    private final SimulationPipeline simulationPipeline;

    private long lastTickTime = 0;
    private double currentTPS = 0;
    private float currentGini = 0;
    private float currentGDP = 0;
    private float currentLifeExpectancy = 0;
    private float currentFertility = 0;
    private int[] densityDistribution = new int[20]; // 20 bins

    private List<H3Cell> cells;
    private Scenario currentScenario;
    private final java.util.Set<String> firedScenarioEventKeys = new java.util.HashSet<>();
    private org.ether.society.procedural.SimulationPerformanceConfig performanceConfig = new org.ether.society.procedural.SimulationPerformanceConfig(true);

    private org.ether.society.network.ClusterManager clusterManager;

    private ScheduledExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean pauseAtNextEvent = new java.util.concurrent.atomic.AtomicBoolean(false);
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
        this.simulationSaveManager = new SimulationSaveManager();

        this.fluxEngine = new org.ether.society.flux.FluxEngine();
        this.demographicKernel = new org.ether.society.core.dod.DemographicKernel();
        this.urbanKernel = new org.ether.society.core.dod.UrbanKernel();
        this.cultureKernel = new org.ether.society.core.dod.CultureKernel();
        this.environmentalKernel = new org.ether.society.core.dod.EnvironmentalKernel();
        this.statisticsKernel = new org.ether.society.core.dod.StatisticsKernel();
        this.simulationPipeline = new SimulationPipeline(demographicKernel, urbanKernel, cultureKernel, environmentalKernel, statisticsKernel);

        initialize();
    }

    public org.ether.society.events.EventSystem getEventSystem() {
        return eventSystem;
    }

    private void initialize() {
        logger.info("Initializing H3 simulation engine (idle state)...");
        this.cells = new java.util.ArrayList<>();
        if (this.diplomacyManager != null) {
            this.diplomacyManager.clear();
        }
        this.worldBuffer = new org.ether.society.core.dod.WorldBuffer(0);
        this.agentBuffer = new org.ether.society.core.dod.AgentBuffer(0);
    }

    public void initializeFromScenario(Scenario scenario, List<H3Cell> cells) {
        logger.info("Initializing from scenario: {}", scenario.getName());

        boolean wasRunning = running.get();
        if (wasRunning) {
            pause();
        }

        this.currentScenario = scenario;
        this.performanceConfig = scenario != null ? scenario.toPerformanceConfig() : new org.ether.society.procedural.SimulationPerformanceConfig(true);
        this.cells = cells;

        timeManager.reset((int) scenario.getStartDateYear());
        firedScenarioEventKeys.clear();
        if (eventSystem != null) {
            eventSystem.reset();
        }

        if (diplomacyManager != null) {
            diplomacyManager.clear();
        }

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
        }
    }

    public Scenario getCurrentScenario() {
        return currentScenario;
    }

    public org.ether.society.procedural.SimulationPerformanceConfig getPerformanceConfig() {
        return performanceConfig;
    }

    public void setPerformanceConfig(org.ether.society.procedural.SimulationPerformanceConfig performanceConfig) {
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
    public void start() {
        if (running.getAndSet(true)) return;
        startGameLoop();
    }

    @Override
    public void pause() {
        if (!running.getAndSet(false)) return;
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Override
    public void reset() {
        pause();
        firedScenarioEventKeys.clear();
        timeManager.reset(config.simulation().startYear());
        historyManager.reset();
        initialize();
    }

    @Override
    public void setSpeed(int multiplier) {
        this.speedMultiplier = multiplier;
        if (running.get()) {
            pause();
            start();
        }
    }

    @Override
    public int getSpeed() {
        return speedMultiplier;
    }

    @Override
    public TimeManager getTimeManager() {
        return timeManager;
    }

    public long getCurrentYear() {
        return timeManager != null ? timeManager.getCurrentYear() : 0;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void setPauseAtNextEvent(boolean pause) {
        this.pauseAtNextEvent.set(pause);
    }

    @Override
    public boolean isPauseAtNextEvent() {
        return this.pauseAtNextEvent.get();
    }

    public List<H3Cell> getCells() {
        return cells;
    }

    public void setCells(List<H3Cell> newCells) {
        boolean wasRunning = running.get();
        if (wasRunning) pause();

        this.cells = newCells;
        if (diplomacyManager != null) diplomacyManager.clear();

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

    public void saveSimulation(String saveName) {
        boolean wasRunning = running.get();
        if (wasRunning) pause();
        simulationSaveManager.saveSimulation(this, saveName);
        if (wasRunning) start();
    }

    public void loadSimulation(String saveId) {
        boolean wasRunning = running.get();
        if (wasRunning) pause();
        simulationSaveManager.loadSimulation(saveId, this);
    }


    public H3ClimateSystem getClimateSystem() {
        return climateSystem;
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
        if (speedMultiplier >= 999) {
            // MAX speed: continuous non-accumulating loop with minimum fixed delay (1 ms)
            executorService.scheduleWithFixedDelay(this::tick, 0, 1, TimeUnit.MILLISECONDS);
        } else {
            long delay = Math.max(1, config.simulation().tickRateMs() / Math.max(1, speedMultiplier));
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

    private Runnable onTickCallback;

    public void setClusterManager(org.ether.society.network.ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
        if (this.clusterManager != null && this.worldBuffer != null) {
            this.clusterManager.setWorldBuffer(this.worldBuffer);
        }
    }

    public org.ether.society.network.ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setOnTickCallback(Runnable callback) {
        this.onTickCallback = callback;
    }

    public org.ether.society.core.profiling.SimulationProfiler getProfiler() {
        return profiler;
    }

    private int tickCounter = 0;

    public long getTickCounter() {
        return tickCounter;
    }

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
        public String toString() {
            return label;
        }
    }

    private TemporalScale temporalScale = TemporalScale.DAILY;

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

            final float DT_TICK = stepDays * 86400f; // Delta time for this tick in seconds

            // 1. FAST SCALE DYNAMICS (Scaled to stepDays)
            profiler.beginPhase("1_FastScaleFlux");
            if (clusterManager != null && clusterManager.getNodeRegistry().size() > 1) {
                clusterManager.executeDistributedTick(tickCounter, worldBuffer, DT_TICK, buf -> fluxEngine.tick(buf, DT_TICK));
            } else {
                fluxEngine.tick(worldBuffer, DT_TICK);
            }
            politicalEngine.tick(cells, stepDays);
            timeManager.advanceDays(stepDays);
            profiler.endPhase("1_FastScaleFlux");

            // 2. SLOW SCALE PHYSICALIST DYNAMICS
            // Triggered every tick if stepDays >= 30, or every (30 / stepDays) ticks if stepDays < 30
            int slowModulo = Math.max(1, 30 / stepDays);
            boolean runSlowScale = (stepDays >= 30) || (tickCounter % slowModulo == 0);

            if (runSlowScale) {
                if (currentScenario != null && timeManager.getCurrentYear() >= currentScenario.getEndDateYear()) {
                    logger.info("🏁 Simulation reached scenario target end date (Year {}). Auto-pausing.", currentScenario.getEndDateYear());
                    pause();
                    return;
                }
                int month = timeManager.getCurrentMonth();
                float dtSlow = Math.max(DT_TICK, 30.0f * 86400f);

                profiler.beginPhase("2_ClimateAndEnvironment");
                int climateFreq = (performanceConfig != null && performanceConfig.isEnableMultiFreqClimateTicks()) 
                        ? performanceConfig.getClimateTickFrequency() : 1;
                if (climateFreq <= 1 || (tickCounter / slowModulo) % climateFreq == 0) {
                    climateSystem.updateClimate(cells, month);
                    syncClimateToBuffer();
                }
                environmentalKernel.tick(worldBuffer, month, dtSlow);
                profiler.endPhase("2_ClimateAndEnvironment");

                simulationPipeline.executeTick(
                        this, cells, worldBuffer, agentBuffer, climateSystem,
                        profiler, performanceConfig, (long) dtSlow, getAverageTechnology()
                );

                // Statistics & Snapshots
                profiler.beginPhase("5_StatisticsAndHistory");
                currentGini = statisticsKernel.calculateGini(worldBuffer.getResourceCapital());
                densityDistribution = statisticsKernel.calculateDistribution(worldBuffer.getBiomassHuman(), 20, 1000.0f);
                currentGDP = statisticsKernel.calculateGDP(worldBuffer.getResourceCapital());
                currentLifeExpectancy = statisticsKernel.calculateLifeExpectancy(agentBuffer.getAge(), agentBuffer.getHexIds());
                currentFertility = statisticsKernel.calculateFertilityRate(agentBuffer.getBirths(), agentBuffer.getMass());

                historyManager.captureSnapshot(this);
                historyManager.captureWorldSnapshot(this);
                
                int eventCountBefore = eventSystem.peekEvents().size();
                checkScenarioClimateEvents();
                eventSystem.checkEvents(timeManager.getCurrentYear(), timeManager.getCurrentMonth(), getTotalPopulation(), getTotalFood(), cells);
                eventSystem.checkCellEvents(timeManager.getCurrentYear(), timeManager.getCurrentMonth(), cells);
                int eventCountAfter = eventSystem.peekEvents().size();

                if (pauseAtNextEvent.get() && eventCountAfter > eventCountBefore) {
                    logger.info("Auto-pausing simulation tick due to event trigger (pauseAtNextEvent=true)");
                    pause();
                }
                profiler.endPhase("5_StatisticsAndHistory");

                profiler.beginPhase("6_BufferSync");
                syncBufferToCells();
                profiler.endPhase("6_BufferSync");
            }

            // Every 60 Ticks: trigger rolling checkpoint auto-save
            if (tickCounter > 0 && tickCounter % 60 == 0) {
                autoSaveCheckpoint();
            }

            agentManager.update();
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

                    org.ether.society.events.ActiveEvent ae = new org.ether.society.events.ActiveEvent(
                        "SCENARIO_EVT_" + System.currentTimeMillis(),
                        "🌋 SCÉNARIO : " + evt.getName() + " (" + evt.getType() + " - Mag: " + evt.getMagnitude() + ")",
                        evt.getType().toUpperCase(),
                        evt.getLatitude(), evt.getLongitude(),
                        currentYear, timeManager.getCurrentMonth(), 1, 25.0
                    );
                    eventSystem.recordSpatialEvent(ae);
                    logger.info("Triggered scheduled scenario climate event: {} at year {}", evt.getName(), currentYear);

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

                if (type.contains("volcano") || type.contains("nuclear") || type.contains("ice")) {
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
    public void stepBackward(int ticks) {
        pause();
        if (historyManager == null || cells == null || cells.isEmpty()) return;
        long currentTicks = timeManager.getTotalTicks();
        long targetTicks = Math.max(0, currentTicks - ticks);

        java.util.NavigableMap<Long, List<H3Cell>> snapshots = historyManager.getWorldSnapshots();
        if (snapshots.isEmpty()) return;

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
                }
            }
            if (worldBuffer != null) {
                org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, worldBuffer);
            }
        }
    }

    private final java.util.concurrent.ExecutorService saveExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Ether-AutoSave-Thread");
        t.setDaemon(true);
        return t;
    });

    private void autoSaveCheckpoint() {
        if (cells == null || cells.isEmpty()) return;
        final int currentTick = tickCounter;
        saveExecutor.submit(() -> {
            try {
                simulationSaveManager.saveCheckpoint(this, currentTick);
            } catch (Exception ex) {
                logger.error("Failed to save 60-tick checkpoint", ex);
            }
        });
    }

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
        }
    }

    public long getTotalPopulation() {
        if (worldBuffer == null) return 0;
        long total = 0;
        float[] pop = worldBuffer.getBiomassHuman();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) {
            total += (long)pop[i];
        }
        return total;
    }

    public double getTotalFood() {
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

    private void initializePoliticalSeeding(int populatedCount) {
        if (populatedCount < 3 || cells == null || cells.isEmpty()) return;

        if (diplomacyManager != null) {
            diplomacyManager.clear();
        }

        cells.stream()
                .filter(c -> c.getPopulation() != null && c.getPopulation() > 0)
                .sorted(java.util.Comparator.comparingInt(H3Cell::getPopulation).reversed())
                .limit(3)
                .forEach(c -> {
                    if (c.getOwner() == null) {
                        String name = "Realm of Hex " + Long.toHexString(c.getH3Index()).toUpperCase();
                        javafx.scene.paint.Color color = javafx.scene.paint.Color.hsb(Math.random() * 360, 0.8, 0.9);
                        org.ether.society.model.Nation nation = new org.ether.society.model.Nation(name, color, c);
                        diplomacyManager.registerNation(nation);
                    }
                });
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

    // --- EXTENDED CLIODYNAMIC & PHYSICAL METRICS ---

    public double getEnergyCaptured() {
        if (worldBuffer == null) return 0;
        double energy = 0;
        float[] tech = worldBuffer.getTechnologyLevel();
        float[] pop = worldBuffer.getBiomassHuman();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) {
            energy += (100.0 + tech[i] * 15.0) * (pop[i] > 0 ? 1 : 0);
        }
        return energy;
    }

    public double getResourceDepletionRate() {
        if (worldBuffer == null || worldBuffer.getCapacity() == 0) return 0;
        double maxRes = worldBuffer.getCapacity() * 1000.0;
        double currentRes = 0;
        float[] res = worldBuffer.getResourceCapital();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) currentRes += res[i];
        return Math.max(0, Math.min(100.0, (1.0 - currentRes / Math.max(1, maxRes)) * 100.0));
    }

    public double getEnergyPerCapita() {
        long pop = getTotalPopulation();
        return pop > 0 ? getEnergyCaptured() / pop : 0;
    }

    public double getFoodPerCapita() {
        long pop = getTotalPopulation();
        return pop > 0 ? getTotalFood() / pop : 0;
    }

    public double getBiomassDomesticated() {
        return getTotalPopulation() * 0.15 + getTotalFood() * 0.4;
    }

    public double getPotableWaterTotal() {
        if (worldBuffer == null) return 0;
        double water = 0;
        float[] w = worldBuffer.getWaterResource();
        for (int i = 0; i < worldBuffer.getCapacity(); i++) water += w[i];
        return water;
    }

    public double getRemainingResourcesRatio() {
        return 100.0 - getResourceDepletionRate();
    }

    public double getSystemicEntropy() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        return (pop * 0.05 + tech * 2.5) % 1000.0;
    }

    public double getPollutionLevel() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        return Math.max(0, (tech > 50 ? (tech - 50) * 1.5 * (pop / 100000.0) : 0));
    }

    public double getOccupiedTerritoryArea() {
        return getPopulatedCellCount() * 1250.0;
    }

    public double getOffspringPercentage() {
        float fert = getCurrentFertility();
        return Math.min(95.0, Math.max(20.0, 40.0 + fert * 12.0));
    }

    public double getAgeAtFirstChild() {
        float tech = getAverageTechnology();
        return Math.min(32.0, Math.max(16.0, 18.0 + (tech / 200.0) * 10.0));
    }

    public double getImmigrationRate() {
        long pop = getTotalPopulation();
        return pop > 0 ? (pop % 1000) / 10.0 : 0;
    }

    public double getEducationLevel() {
        float tech = getAverageTechnology();
        return Math.min(100.0, (tech / 250.0) * 100.0);
    }

    public double getHappinessIndex() {
        float gini = getCurrentGini();
        float life = getCurrentLifeExpectancy();
        double foodPerCap = getFoodPerCapita();
        double base = (life / 80.0) * 50.0 + Math.min(50.0, foodPerCap * 10.0) - (gini * 30.0);
        return Math.max(0.0, Math.min(100.0, base));
    }

    public double getConflictLevel() {
        float gini = getCurrentGini();
        double happiness = getHappinessIndex();
        return Math.max(0.0, Math.min(100.0, (gini * 60.0) + (100.0 - happiness) * 0.4));
    }

    public int getCityStatesCount() {
        long popCells = getPopulatedCellCount();
        return (int) Math.max(1, popCells / 5);
    }

    public double getInstitutionalMaturity() {
        float tech = getAverageTechnology();
        return Math.min(100.0, tech * 0.45);
    }

    public double getDivisionOfLaborIndex() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        return Math.min(100.0, (tech * 0.5) + Math.log10(Math.max(1, pop)) * 5.0);
    }

    public int getMaxHierarchyLevel() {
        float tech = getAverageTechnology();
        if (tech < 10) return 1;
        if (tech < 30) return 2;
        if (tech < 60) return 3;
        if (tech < 100) return 4;
        if (tech < 200) return 5;
        return 6;
    }

    public long getLargestCulturalUnitSize() {
        if (diplomacyManager != null && !diplomacyManager.getNations().isEmpty()) {
            return diplomacyManager.getNations().stream()
                    .mapToLong(org.ether.society.model.Nation::getTotalPopulation)
                    .max()
                    .orElse((long) (getTotalPopulation() * Math.min(0.85, 0.2 + (getAverageTechnology() / 300.0))));
        }
        long pop = getTotalPopulation();
        return (long) (pop * Math.min(0.85, 0.2 + (getAverageTechnology() / 300.0)));
    }

    public double getLargestOrganizationComplexity() {
        long largestPop = getLargestCulturalUnitSize();
        if (largestPop <= 0) return 0.0;
        float avgTech = getAverageTechnology();
        int hierarchy = getMaxHierarchyLevel();
        double stateCap = 0.5;
        if (diplomacyManager != null && !diplomacyManager.getNations().isEmpty()) {
            org.ether.society.model.Nation largest = diplomacyManager.getNations().stream()
                    .max(java.util.Comparator.comparingLong(org.ether.society.model.Nation::getTotalPopulation))
                    .orElse(null);
            if (largest != null) {
                stateCap = largest.getStateCapacity();
            }
        }
        return largestPop * (1.0 + avgTech * 0.4) * stateCap * (Math.log(1.0 + hierarchy) / Math.log(2.0));
    }

    public double getLargestOrganizationEntropy() {
        double complexity = getLargestOrganizationComplexity();
        double pollution = getPollutionLevel();
        float tech = getAverageTechnology();
        return (complexity * 0.05 + tech * 1.2) * (1.0 + pollution / 100.0);
    }

    public double getKardashevScale() {
        double energy = getEnergyCaptured();
        if (energy <= 0) return 0.0;
        double watts = energy * 1e6;
        double k = (Math.log10(Math.max(1.0, watts)) - 6.0) / 10.0;
        return Math.max(0.0, Math.min(3.0, k));
    }

    public double getBuiltCapitalTotal() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        return pop * (5.0 + tech * 12.0);
    }

    public double getEliteFormationRatio() {
        float gini = getCurrentGini();
        return Math.min(25.0, Math.max(0.5, 1.0 + gini * 15.0));
    }

    public double getElderCapitalShare() {
        float gini = getCurrentGini();
        float life = getCurrentLifeExpectancy();
        return Math.min(90.0, Math.max(30.0, 40.0 + (life / 80.0) * 30.0 + gini * 20.0));
    }

    public double getLandRentIndex() {
        long pop = getTotalPopulation();
        long cells = getPopulatedCellCount();
        double density = cells > 0 ? (double) pop / cells : 0;
        return density * 1.5 + getAverageTechnology() * 0.8;
    }

    public long getToolsCount() {
        float tech = getAverageTechnology();
        return (long) (getTotalPopulation() * (1.2 + tech * 0.5));
    }

    public long getProductsCount() {
        float tech = getAverageTechnology();
        return (long) (10.0 + Math.pow(tech, 1.8));
    }

    public double getSystemComplexityIndex() {
        float tech = getAverageTechnology();
        double divLabor = getDivisionOfLaborIndex();
        return Math.min(100.0, (tech * 0.4 + divLabor * 0.6));
    }

    public double getReconstructionCapabilityIndex() {
        double edu = getEducationLevel();
        float tech = getAverageTechnology();
        return Math.min(100.0, (edu * 0.7 + tech * 0.3));
    }

    public double getSystemInterdependenceIndex() {
        double complexity = getSystemComplexityIndex();
        return Math.min(100.0, complexity * 0.95);
    }

    public int[] getAgePyramid() {
        int[] cohorts = new int[7];
        if (agentBuffer != null && agentBuffer.getCapacity() > 0) {
            float[] ages = agentBuffer.getAge();
            int[] hexIds = agentBuffer.getHexIds();
            for (int i = 0; i < agentBuffer.getCapacity(); i++) {
                if (hexIds != null && hexIds[i] == -1) continue;
                float age = ages != null ? ages[i] : 25.0f;
                if (age < 15) cohorts[0]++;
                else if (age < 25) cohorts[1]++;
                else if (age < 40) cohorts[2]++;
                else if (age < 55) cohorts[3]++;
                else if (age < 70) cohorts[4]++;
                else if (age < 85) cohorts[5]++;
                else cohorts[6]++;
            }
            // Scale by cohort weight if agents represent demographic cohorts
            int cohortSize = currentScenario != null && currentScenario.getTargetCohortSize() > 0 ? currentScenario.getTargetCohortSize() : 150;
            long realPop = getTotalPopulation();
            long agentSum = 0; for (int c : cohorts) agentSum += c;
            if (agentSum > 0 && realPop > agentSum) {
                double scale = (double) realPop / agentSum;
                for (int i = 0; i < 7; i++) cohorts[i] = (int) (cohorts[i] * scale);
            }
        } else {
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

    // --- 🧠 COGNITION & INFORMATION ---
    public double getShannonBandwidth() {
        float tech = getAverageTechnology();
        return 1.0 + Math.pow(tech, 1.4) * 0.8;
    }

    public double getCollectiveMemoryStock() {
        float tech = getAverageTechnology();
        long pop = getTotalPopulation();
        return (pop * 0.05 + Math.pow(tech, 2.1));
    }

    public double getInnovationDiffusionSpeed() {
        float tech = getAverageTechnology();
        double divLabor = getDivisionOfLaborIndex();
        return Math.min(100.0, (tech * 0.4 + divLabor * 0.6));
    }

    public double getKnowledgeDecayRate() {
        float gini = getCurrentGini();
        double conflict = getConflictLevel();
        return Math.min(100.0, (conflict * 0.7 + gini * 30.0));
    }

    // --- 🌍 ÉCOLOGIE & FRONTIÈRES PLANÉTAIRES ---
    public double getSoilNPKQuality() {
        float tech = getAverageTechnology();
        double resDep = getResourceDepletionRate();
        return Math.max(5.0, 100.0 - (resDep * 0.6) + Math.min(15.0, tech * 0.1));
    }

    public double getCarbonFootprint() {
        double energy = getEnergyCaptured();
        float tech = getAverageTechnology();
        return (energy * (tech > 40 && tech < 180 ? 0.85 : 0.2)) / 1000.0;
    }

    public double getWildBiodiversityIndex() {
        float bioNat = getTotalBiomassNatural();
        double bioDom = getBiomassDomesticated();
        double total = bioNat + bioDom;
        return total > 0 ? Math.min(100.0, (bioNat / total) * 100.0) : 100.0;
    }

    public double getWetBulbSafetyMargin() {
        float temp = 15.0f;
        float[] temps = worldBuffer != null ? worldBuffer.getTemperature() : null;
        if (temps != null && temps.length > 0) {
            float sum = 0; for(float t : temps) sum += t;
            temp = sum / temps.length;
        }
        return Math.max(0.0, 35.0 - (temp + 3.5));
    }

    // --- ⏳ CLIODYNAMIQUE & RISQUES SYSTÉMIQUES ---
    public double getEliteOverproductionIndex() {
        double eliteForm = getEliteFormationRatio();
        float gini = getCurrentGini();
        return Math.min(10.0, (eliteForm / 5.0) * (1.0 + gini * 2.0));
    }

    public double getFiscalStressIndex() {
        double landRent = getLandRentIndex();
        float gini = getCurrentGini();
        return Math.min(100.0, (gini * 50.0) + (landRent * 0.3));
    }

    public double getGeopoliticalTension() {
        int cityStates = getCityStatesCount();
        double conflict = getConflictLevel();
        return Math.min(100.0, (cityStates * 2.5) + (conflict * 0.7));
    }

    public double getCollapseVulnerability() {
        double resDep = getResourceDepletionRate();
        double psi = getEliteOverproductionIndex();
        double entropy = getSystemicEntropy();
        return Math.min(100.0, (resDep * 0.3) + (psi * 4.0) + (entropy / 20.0));
    }

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

    public org.ether.society.core.dod.WorldBuffer getWorldBuffer() {
        return worldBuffer;
    }

    public org.ether.society.core.dod.AgentBuffer getAgentBuffer() {
        return agentBuffer;
    }
}
