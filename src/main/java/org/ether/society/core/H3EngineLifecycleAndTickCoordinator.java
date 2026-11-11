/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.NativeRustBridge;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.telemetry.CompiledTelemetryPlan;
import org.ether.society.core.telemetry.TelemetryEvaluationResult;
import org.ether.society.core.telemetry.TelemetrySubscription;
import org.ether.society.data.DODDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.events.DomainEventBus;
import org.ether.society.events.SimulationTickEvent;
import org.ether.society.events.SimulationTickListener;
import org.ether.society.model.Scenario;
import org.ether.society.network.spatial.H3SpatialPartitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.prefs.Preferences;

/**
 * Package-private coordinator handling scenario initialization, Data-Oriented Design (DOD) buffer
 * allocation, multi-scale tick execution, timeline seeking, and single-pass scalar reductions
 * for {@link H3SimulationEngine}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see H3SimulationEngine
 */
final class H3EngineLifecycleAndTickCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(H3EngineLifecycleAndTickCoordinator.class);

    private H3EngineLifecycleAndTickCoordinator() {
        // Static package-private helper
    }

    /**
     * Initializes the engine state, Hilbert spatial ordering, DOD buffers, and kernels from a scenario.
     *
     * @param engine   owning {@link H3SimulationEngine}
     * @param scenario target scenario configuration
     * @param cells    spatial H3 cell list
     */
    static void initializeFromScenario(H3SimulationEngine engine, Scenario scenario, List<H3Cell> cells) {
        logger.info("Initializing from scenario: {}", scenario.getName());
        if (engine.isRunning()) {
            engine.pause();
        }
        SimulationPipeline.resetGlobalSimulationState();
        engine.currentScenario = scenario;
        engine.performanceConfig = scenario != null
                ? scenario.toPerformanceConfig()
                : new SimulationPerformanceConfig(true);
        try {
            String defaultHw = NativeRustBridge.isNativeAvailable() ? "NATIVE_RUST" : "JAVA_VECTOR_SIMD";
            String hwPref = Preferences.userRoot().node("org/ether/society/ui")
                    .get("ether_hardware_mode", defaultHw);
            if ("NATIVE_RUST".equalsIgnoreCase(hwPref) && NativeRustBridge.isNativeAvailable()) {
                engine.performanceConfig.setExecutionEngineMode(SimulationPerformanceConfig.ExecutionEngineMode.RUST_NATIVE);
            } else if ("CPU_JIT".equalsIgnoreCase(hwPref) || "GPU_OFF".equalsIgnoreCase(hwPref)) {
                engine.performanceConfig.setExecutionEngineMode(SimulationPerformanceConfig.ExecutionEngineMode.JAVA_STANDARD);
            } else {
                engine.performanceConfig.setExecutionEngineMode(SimulationPerformanceConfig.ExecutionEngineMode.JAVA_VECTOR_SIMD);
            }
        } catch (Throwable ignored) {
            // Headless or restricted security context fallback
        }

        engine.cells = cells;
        engine.timeManager.reset((int) scenario.getStartDateYear());
        engine.firedScenarioEventKeys.clear();

        if (engine.eventSystem != null) {
            engine.eventSystem.reset();
            if (scenario != null) {
                engine.eventSystem.setSeed(scenario.getSeed());
                engine.eventSystem.setEnableRandomEvents(scenario.isRandomEventsEnabled());
                engine.eventSystem.setEnableHistoricalMilestones(scenario.isRandomEventsEnabled());
                engine.eventSystem.setEnableEarthHistoricalLeaders(scenario.isEarthHistoricalLeadersEnabled());
                engine.eventSystem.setEnableProceduralLeaders(scenario.isProceduralLeadersEnabled());
                engine.eventSystem.setEnableChronicleSingularityObserver(scenario.isChronicleSingularityObserverEnabled());
            } else {
                engine.eventSystem.setEnableRandomEvents(true);
                engine.eventSystem.setEnableHistoricalMilestones(true);
            }
        }

        engine.nations.clear();
        H3SpatialPartitioner.sortCellsByHilbertCurve(cells);

        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(cells);

        H3BufferAndEventCoordinator.initializePoliticalEntities(cells, engine.nations, scenario);

        int cohortSize = scenario != null && scenario.getTargetCohortSize() > 0
                ? scenario.getTargetCohortSize()
                : org.ether.society.model.AutoEpochScenarioHelper.calibrateDefaultTargetCohortSize(
                        scenario != null && scenario.getPlanetPreset() != null ? scenario.getPlanetPreset().getCanonicalPlanet() : "earth",
                        scenario != null ? scenario.getStartDateYear() : -8000L,
                        scenario != null ? scenario.getInitialHumanCount() : 0L);
        if (engine.demographicKernel != null) {
            engine.demographicKernel.setTargetCohortSize(cohortSize);
            long sSeed = scenario != null && scenario.getSeed() != 0L ? scenario.getSeed() : 42L;
            engine.demographicKernel.setBaseSeed(sSeed);
        }
        if (engine.cultureKernel != null) {
            engine.cultureKernel.resetState();
            long sSeed = scenario != null && scenario.getSeed() != 0L ? scenario.getSeed() : 42L;
            long cSeed = scenario != null && scenario.getCulturalSeed() != 0L ? scenario.getCulturalSeed() : sSeed;
            engine.cultureKernel.setBaseSeed(cSeed);
        }
        if (engine.simulationPipeline != null) {
            engine.simulationPipeline.configureScenario(scenario);
        }

        setupBuffersAndInitialStatistics(engine, cohortSize);

        if (engine.clusterManager != null) {
            engine.clusterManager.setWorldBuffer(engine.worldBuffer);
        }
        if (engine.historyManager != null) {
            engine.historyManager.reset();
            engine.historyManager.captureSnapshot(engine);
            engine.historyManager.captureWorldSnapshot(engine);
        }
    }

    /**
     * Replaces the active H3 cell list and re-initializes the DOD buffers accordingly.
     *
     * @param engine   owning {@link H3SimulationEngine}
     * @param newCells new list of H3 cells
     */
    static void setCells(H3SimulationEngine engine, List<H3Cell> newCells) {
        if (engine.isRunning()) {
            engine.pause();
        }
        engine.cells = newCells;
        engine.nations.clear();
        if (newCells != null && !newCells.isEmpty()) {
            if (engine.currentScenario != null) {
                PreComputePhase preCompute = new PreComputePhase(engine.currentScenario);
                preCompute.execute(newCells);
            } else {
                H3BufferAndEventCoordinator.initializePopulation(newCells, engine.currentScenario, engine.nations);
            }
            int cohortSize = engine.currentScenario != null && engine.currentScenario.getTargetCohortSize() > 0
                    ? engine.currentScenario.getTargetCohortSize()
                    : org.ether.society.model.AutoEpochScenarioHelper.calibrateDefaultTargetCohortSize(
                            engine.currentScenario != null && engine.currentScenario.getPlanetPreset() != null ? engine.currentScenario.getPlanetPreset().getCanonicalPlanet() : "earth",
                            engine.currentScenario != null ? engine.currentScenario.getStartDateYear() : -8000L,
                            engine.currentScenario != null ? engine.currentScenario.getInitialHumanCount() : 0L);
            if (engine.demographicKernel != null) {
                engine.demographicKernel.setTargetCohortSize(cohortSize);
            }
            setupBuffersAndInitialStatistics(engine, cohortSize);
        } else {
            engine.worldBuffer = new WorldBuffer(0);
            engine.agentBuffer = new AgentBuffer(0);
        }
    }

    /**
     * Allocates DOD {@link WorldBuffer} and {@link AgentBuffer} arenas and evaluates baseline telemetry.
     *
     * @param engine     owning {@link H3SimulationEngine}
     * @param cohortSize target Dunbar cohort mass threshold
     */
    static void setupBuffersAndInitialStatistics(H3SimulationEngine engine, int cohortSize) {
        long initialPop = engine.currentScenario != null ? engine.currentScenario.getInitialHumanCount() : 1_000_000L;
        int estimatedAgents = (int) Math.max(engine.cells.size(), Math.min(10_000_000L, initialPop / Math.max(1, cohortSize)));
        engine.worldBuffer = new WorldBuffer(engine.cells.size());

        int cDims = engine.currentScenario != null ? engine.currentScenario.getCultureVectorDimensions() : 9;
        int agentCapacity = Math.min(10_000_000, Math.max(8192, Math.max(engine.cells.size() * 24, estimatedAgents * 8)));
        engine.agentBuffer = new AgentBuffer(agentCapacity, cDims);

        DODDataGenerator.populateWorldBuffer(engine.cells, engine.worldBuffer);
        long sSeed = engine.currentScenario != null ? engine.currentScenario.getSeed() : 42L;
        DODDataGenerator.initializeAgentBuffer(engine.worldBuffer, engine.agentBuffer, cohortSize, sSeed);

        engine.initialPopulationBaseline = initialPop;
        engine.initialCo2Baseline = (engine.currentScenario != null && engine.currentScenario.getPlanetPreset() != null
                && engine.currentScenario.getPlanetPreset().co2Ppm() > 0.0)
                ? engine.currentScenario.getPlanetPreset().co2Ppm()
                : 280.0;

        // Evaluate initial statistics ONLY when telemetry subscription is non-empty
        if (!engine.getCompiledTelemetryPlan().isNoOp() && engine.worldBuffer != null) {
            engine.evaluateTelemetryNow();
        }
    }

    /**
     * Executes a single multi-scale simulation tick, coordinating fast-scale temporal progression,
     * slow-scale physical/cliodynamic pipelines, telemetry caching, event checks, and listener dispatch.
     *
     * @param engine owning {@link H3SimulationEngine}
     */
    static void executeTick(H3SimulationEngine engine) {
        if (!engine.running.get()) return;
        long tickStartNanos = System.nanoTime();
        if (engine.lastTickTime != 0) {
            double diff = (tickStartNanos - engine.lastTickTime) / 1_000_000_000.0;
            if (diff > 0) {
                double instantTPS = 1.0 / diff;
                engine.currentTPS = (engine.currentTPS <= 0) ? instantTPS : (engine.currentTPS * 0.90 + instantTPS * 0.10);
            }
        }
        engine.lastTickTime = tickStartNanos;

        try {
            int stepDays = (engine.currentScenario != null && engine.currentScenario.getTemporalResolutionDays() > 0)
                    ? (int) Math.round(engine.currentScenario.getTemporalResolutionDays()) : 1;
            stepDays = Math.max(1, stepDays);
            final float dtTick = stepDays * 86400f;

            // 1. Fast-scale temporal advancement
            int yearBeforeTick = engine.timeManager.getCurrentYear();
            engine.timeManager.advanceDays(stepDays);
            boolean yearChanged = (engine.timeManager.getCurrentYear() != yearBeforeTick);
            engine.profiler.endPhase("1_FastScaleFlux");

            // 2. Slow-scale physicalist & cliodynamic dynamics
            boolean multiFreq = engine.performanceConfig != null
                    && engine.performanceConfig.isEnableMultiFreqClimateTicks()
                    && !engine.performanceConfig.isStrictDeterminism();
            int slowModulo = multiFreq ? Math.max(1, 30 / stepDays) : 1;
            boolean runSlowScale = (stepDays >= 30) || (engine.tickCounter % slowModulo == 0);

            if (runSlowScale) {
                if (engine.currentScenario != null && engine.timeManager.getCurrentYear() > engine.currentScenario.getEndDateYear()) {
                    logger.info("Simulation reached scenario target end date (Year {}). Auto-pausing.", engine.currentScenario.getEndDateYear());
                    if (engine.historyManager != null) {
                        engine.historyManager.captureSnapshot(engine);
                        engine.historyManager.captureWorldSnapshot(engine);
                    }
                    engine.pause();
                    return;
                }

                float dtSlow = multiFreq ? Math.max(dtTick, slowModulo * dtTick) : dtTick;
                engine.simulationPipeline.executeTick(
                        engine, engine.cells, engine.worldBuffer, engine.agentBuffer,
                        engine.profiler, engine.performanceConfig, (long) dtSlow
                );

                // 5. Statistics, Telemetry & History Snapshots (Zero-cost when unsubscribed or skipped by subsampling)
                TelemetrySubscription activeSub = engine.getTelemetrySubscription();
                CompiledTelemetryPlan telemetryPlan = engine.getCompiledTelemetryPlan();
                if (!telemetryPlan.isNoOp() && activeSub.shouldEvaluateAtTick(engine.tickCounter)) {
                    engine.profiler.beginPhase("5_StatisticsAndHistory");
                    TelemetryEvaluationResult evalRes = engine.evaluateTelemetryNow();
                    if (engine.historyManager != null) {
                        engine.historyManager.captureSnapshot(engine, evalRes);
                    }
                    engine.profiler.endPhase("5_StatisticsAndHistory");
                }

                if (engine.historyManager != null && engine.historyManager.shouldCaptureWorldSnapshot(engine.tickCounter, false, yearChanged)) {
                    H3BufferSyncHelper.syncBufferToCells(engine.worldBuffer, engine.cells, engine.performanceConfig);
                    engine.historyManager.captureWorldSnapshot(engine);
                }

                int eventCountBefore = engine.eventSystem.peekEvents().size();
                H3BufferAndEventCoordinator.checkScenarioClimateEvents(
                        engine.currentScenario, engine.timeManager, engine.tickCounter, engine.firedScenarioEventKeys,
                        engine.eventSystem, engine.cells, engine.worldBuffer, engine.agentBuffer);
                engine.eventSystem.checkEvents(
                        engine.timeManager.getCurrentYear(), engine.timeManager.getCurrentMonth(), engine.timeManager.getCurrentDay(),
                        engine.getTotalPopulation(), engine.getTotalFood(), engine.cells);
                engine.eventSystem.checkCellEvents(
                        engine.timeManager.getCurrentYear(), engine.timeManager.getCurrentMonth(), engine.timeManager.getCurrentDay(),
                        engine.cells);
                engine.eventSystem.checkSingularities(
                        engine.timeManager.getCurrentYear(), engine.timeManager.getCurrentMonth(),
                        engine.cells, engine.currentScenario);
                int eventCountAfter = engine.eventSystem.peekEvents().size();

                if (engine.pauseAtNextEvent.get() && eventCountAfter > eventCountBefore) {
                    logger.info("[Step {}] Automatic simulation pause on event (pauseAtNextEvent=true)", engine.tickCounter);
                    engine.pause();
                }
                engine.profiler.endPhase("5_StatisticsAndHistory");

                engine.profiler.beginPhase("6_BufferSync");
                engine.profiler.endPhase("6_BufferSync");

                if (engine.tickCounter % Math.max(1, 12 / stepDays) == 0) {
                    logger.info("[Step {}] Year {} Month {} | Pop: {} | TPS: {} it/s",
                            engine.tickCounter, engine.timeManager.getCurrentYear(),
                            String.format("%02d", engine.timeManager.getCurrentMonth() + 1),
                            String.format("%,d", engine.getTotalPopulation()),
                            String.format("%.1f", engine.currentTPS));
                }
            }

            if (engine.tickCounter > 0 && engine.tickCounter % 60 == 0) {
                autoSaveCheckpoint(engine);
            }
            engine.tickCounter++;

            SimulationTickEvent tickEvent = new SimulationTickEvent(
                    engine.tickCounter,
                    engine.timeManager.getCurrentYear(),
                    engine.timeManager.getCurrentMonth(),
                    engine.getTotalPopulation(),
                    engine.currentTPS,
                    engine.currentGDP,
                    engine.currentGini,
                    yearChanged
            );

            for (SimulationTickListener listener : engine.tickListeners) {
                try {
                    listener.onTick(tickEvent);
                } catch (Exception ex) {
                    logger.error("Error notifying SimulationTickListener: {}", ex.getMessage(), ex);
                }
            }
            DomainEventBus.getInstance().publishNow(tickEvent);

            long tickDuration = System.nanoTime() - tickStartNanos;
            long renderStartNanos = System.nanoTime();
            if (engine.onTickCallback != null) {
                engine.onTickCallback.run();
            }
            long renderDuration = System.nanoTime() - renderStartNanos;
            engine.profiler.recordTick(tickDuration, renderDuration);
        } catch (Exception e) {
            logger.error("Error during simulation tick", e);
        }
    }

    /**
     * Advances simulation state deterministically by {@code ticks} steps while paused.
     *
     * @param engine owning {@link H3SimulationEngine}
     * @param ticks  number of steps to advance
     */
    static void stepForward(H3SimulationEngine engine, int ticks) {
        engine.pause();
        for (int i = 0; i < Math.max(1, ticks); i++) {
            if (engine.currentScenario != null && engine.timeManager.getCurrentYear() > engine.currentScenario.getEndDateYear()) {
                logger.info("stepForward terminating early: scenario target end date (Year {}) reached.", engine.currentScenario.getEndDateYear());
                break;
            }
            engine.running.set(true);
            try {
                executeTick(engine);
            } finally {
                if (!engine.running.get()) {
                    break;
                }
                engine.running.set(false);
            }
            if (engine.currentScenario != null && engine.timeManager.getCurrentYear() > engine.currentScenario.getEndDateYear()) {
                logger.info("stepForward terminating: scenario target end date (Year {}) reached after tick.", engine.currentScenario.getEndDateYear());
                break;
            }
        }
        engine.running.set(false);
        if (engine.historyManager != null) {
            engine.historyManager.captureWorldSnapshot(engine);
            engine.historyManager.flushSnapshots();
        }
    }

    /**
     * Seeks simulation state and world snapshots to the specified tick index.
     *
     * @param engine      owning {@link H3SimulationEngine}
     * @param targetTicks target tick index
     */
    static void seekToTick(H3SimulationEngine engine, long targetTicks) {
        engine.pause();
        if (engine.historyManager == null || engine.cells == null || engine.cells.isEmpty()) return;
        H3BufferAndEventCoordinator.restoreWorldSnapshotAtTick(
                engine.historyManager, engine.cells, engine.worldBuffer, targetTicks);

        long initialYear = engine.currentScenario != null ? engine.currentScenario.getStartDateYear() : -20000;
        int stepDaysVal = (engine.currentScenario != null && engine.currentScenario.getTemporalResolutionDays() > 0)
                ? (int) Math.round(engine.currentScenario.getTemporalResolutionDays()) : 1;
        long totalDays = targetTicks * stepDaysVal;
        long totalMonths = totalDays / 30;
        int targetYear = (int) (initialYear + (totalMonths / 12));
        int targetMonth = (int) (totalMonths % 12);
        int targetDay = (int) ((totalDays % 30) + 1);
        engine.timeManager.setTime(targetYear, targetMonth, targetDay, targetTicks);
        engine.tickCounter = (int) targetTicks;

        if (engine.worldBuffer != null && engine.statisticsKernel != null) {
            engine.currentGini = engine.statisticsKernel.calculateGini(engine.worldBuffer.getResourceCapital());
            engine.densityDistribution = engine.statisticsKernel.calculateDistribution(
                    engine.worldBuffer.getBiomassHuman(), 20, 1000.0f);
            engine.currentGDP = engine.statisticsKernel.calculateGDP(
                    engine.worldBuffer.getResourceCapital(), engine.worldBuffer.getBiomassHuman());
            if (engine.agentBuffer != null) {
                float foodPerCapVal = (float) engine.getFoodPerCapita();
                float foodSatVal = Math.min(1.0f, foodPerCapVal > 0 ? foodPerCapVal / 2.0f : 1.0f);
                engine.currentLifeExpectancy = engine.statisticsKernel.calculateLifeExpectancy(
                        engine.worldBuffer, foodSatVal);
                engine.currentFertility = engine.statisticsKernel.calculateFertilityRate(
                        engine.agentBuffer.getBirths(), engine.agentBuffer.getMass());
            }
        }
    }

    /**
     * Asynchronously persists a periodic simulation checkpoint at most once every 30 seconds.
     *
     * @param engine owning {@link H3SimulationEngine}
     */
    static void autoSaveCheckpoint(H3SimulationEngine engine) {
        if (engine.cells == null || engine.cells.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now - engine.lastAutoSaveTimeMs < 30_000L) {
            return;
        }
        engine.lastAutoSaveTimeMs = now;
        final int currentTick = engine.tickCounter;
        engine.saveExecutor.submit(() -> {
            try {
                engine.simulationSaveManager.saveCheckpoint(engine, currentTick);
            } catch (Exception ex) {
                logger.error("Failed to save periodic checkpoint", ex);
            }
        });
    }

    /**
     * Updates cached scalar aggregates in a single fused $O(N)$ cache-aligned pass over {@link WorldBuffer}.
     *
     * @param engine owning {@link H3SimulationEngine}
     */
    static void updateGlobalAggregatesCache(H3SimulationEngine engine) {
        WorldBuffer wb = engine.worldBuffer;
        if (wb == null) return;
        int cap = wb.getCapacity();
        float[] pop = wb.getBiomassHuman();
        float[] food = wb.getFoodResource();
        float[] water = wb.getWaterResource();
        float[] bio = wb.getBiomassNatural();
        float[] res = wb.getResourceCapital();

        double sumPop = 0.0;
        double sumFood = 0.0;
        double sumWater = 0.0;
        float sumBio = 0.0f;
        double sumRes = 0.0;

        for (int i = 0; i < cap; i++) {
            if (pop != null && pop[i] > 0.0f) sumPop += pop[i];
            if (food != null) sumFood += food[i];
            if (water != null) sumWater += water[i];
            if (bio != null) sumBio += bio[i];
            if (res != null) sumRes += res[i];
        }

        engine.cachedTotalPopulation = Math.round(sumPop);
        engine.cachedTotalFood = sumFood;
        engine.cachedPotableWater = sumWater;
        engine.cachedBiomassNatural = sumBio;
        engine.cachedAverageTechnology = (float) (engine.metricsAggregator != null ? engine.metricsAggregator.getEmergentTechnologyLevel() : 1.0);
        engine.cachedResourceCapital = sumRes;
        engine.lastCacheTick = engine.tickCounter;
    }
}
