/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.StatisticsKernel;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.telemetry.CompiledTelemetryPlan;
import org.ether.society.core.telemetry.TelemetryAstCompiler;
import org.ether.society.core.telemetry.TelemetryEvaluationResult;
import org.ether.society.core.telemetry.TelemetrySubscription;
import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
import org.ether.society.model.Scenario;

import java.util.List;

/**
 * Encapsulates live telemetry subscription, plan compilation, and on-demand DAG execution
 * for {@link H3SimulationEngine}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
final class H3SimulationTelemetryCoordinator {

    private TelemetrySubscription telemetrySubscription = TelemetrySubscription.NONE;
    private CompiledTelemetryPlan compiledTelemetryPlan = CompiledTelemetryPlan.NO_OP;
    private TelemetryEvaluationResult cachedTelemetryResult = TelemetryEvaluationResult.EMPTY;
    private long lastTelemetryTick = -1L;
    private long lastStatsSyncTick = -1L;
    private boolean headlessTelemetrySubscribed = false;

    H3SimulationTelemetryCoordinator() {
        // Package-private coordinator
    }

    TelemetrySubscription getTelemetrySubscription() {
        return telemetrySubscription;
    }

    CompiledTelemetryPlan getCompiledTelemetryPlan() {
        return compiledTelemetryPlan;
    }

    TelemetryEvaluationResult getCachedTelemetryResult() {
        return cachedTelemetryResult;
    }

    long getLastTelemetryTick() {
        return lastTelemetryTick;
    }

    boolean isHeadlessTelemetrySubscribed() {
        return headlessTelemetrySubscribed;
    }

    void setHeadlessTelemetrySubscribed(boolean subscribed) {
        if (subscribed) {
            if (this.telemetrySubscription.isEmpty()) {
                setTelemetrySubscription(TelemetrySubscription.allCanonicalBenchmarks(1));
            }
        } else {
            setTelemetrySubscription(TelemetrySubscription.NONE);
        }
    }

    void setTelemetrySubscription(TelemetrySubscription subscription) {
        this.telemetrySubscription = (subscription != null) ? subscription : TelemetrySubscription.NONE;
        this.compiledTelemetryPlan = TelemetryAstCompiler.compile(this.telemetrySubscription);
        this.headlessTelemetrySubscribed = !this.telemetrySubscription.isEmpty();
        this.lastTelemetryTick = -1L;
        if (this.telemetrySubscription.isEmpty()) {
            this.cachedTelemetryResult = TelemetryEvaluationResult.EMPTY;
        }
    }

    TelemetryEvaluationResult evaluateTelemetryNow(
            WorldBuffer worldBuffer,
            AgentBuffer agentBuffer,
            List<H3Cell> cells,
            List<Polity> nations,
            StatisticsKernel statisticsKernel,
            long tickCounter,
            Scenario currentScenario,
            SimulationTimeCoordinator timeManager,
            long initialPopulationBaseline,
            double initialCo2Baseline,
            double currentTPS) {

        if (compiledTelemetryPlan.isNoOp() || (worldBuffer == null && compiledTelemetryPlan.getInputMask() != 0)) {
            return TelemetryEvaluationResult.EMPTY;
        }
        long startYr = currentScenario != null ? currentScenario.getStartDateYear() : timeManager.getCurrentYear();
        long curYr = timeManager.getCurrentYear();
        double elapsedYrs = Math.max(0.0, (double) (curYr - startYr));

        double baseInfo = currentScenario != null ? currentScenario.getInitialInformationPerCapita() : 0.0;
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
                currentTPS,
                baseInfo);

        this.cachedTelemetryResult = res;
        this.lastTelemetryTick = tickCounter;
        return res;
    }

    /**
     * Refreshes cached scalar statistics directly from the DOD buffers when the engine
     * advances without an active telemetry subscription.
     */
    void refreshUnbufferedMetricsIfStale(H3SimulationEngine engine) {
        if (engine.worldBuffer == null || engine.statisticsKernel == null) return;
        if (this.lastStatsSyncTick == engine.tickCounter && engine.currentFertility > 0.0f && engine.currentLifeExpectancy > 0.0f) {
            return;
        }
        engine.currentGini = engine.statisticsKernel.calculateGini(engine.worldBuffer.getResourceCapital());
        engine.currentGDP = engine.statisticsKernel.calculateGDP(
                engine.worldBuffer.getResourceCapital(), engine.worldBuffer.getHumanPopulation());
        double popTotal = Math.max(0.0, (double) engine.getTotalPopulation());
        double edibleCaloricTotal = 0.0;
        float[] popArr = engine.worldBuffer.getHumanPopulation();
        for (int i = 0; i < engine.worldBuffer.getCapacity(); i++) {
            if (popArr != null && popArr[i] > 0.0f) {
                edibleCaloricTotal += org.ether.society.core.dod.WorldBufferComponentAdapter
                        .computeAvailableEdibleCaloricGJ(engine.worldBuffer, i);
            }
        }
        double ediblePerCap = popTotal > 0.0 ? Math.max(engine.getFoodPerCapita(), edibleCaloricTotal / popTotal) : 0.0;
        float foodSatVal = (float) Math.min(1.0, ediblePerCap > 0.0
                ? ediblePerCap / org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ
                : 1.0);
        engine.currentLifeExpectancy = engine.statisticsKernel.calculateLifeExpectancy(engine.worldBuffer, foodSatVal);
        if (engine.agentBuffer != null) {
            engine.currentFertility = engine.statisticsKernel.calculateFertilityRate(
                    engine.agentBuffer.getBirths(), engine.agentBuffer.getMass());
        }
        this.lastStatsSyncTick = engine.tickCounter;
    }
}
