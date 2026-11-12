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
 * @version 1.0.0-beta.2
 */
final class H3SimulationTelemetryCoordinator {

    private TelemetrySubscription telemetrySubscription = TelemetrySubscription.NONE;
    private CompiledTelemetryPlan compiledTelemetryPlan = CompiledTelemetryPlan.NO_OP;
    private TelemetryEvaluationResult cachedTelemetryResult = TelemetryEvaluationResult.EMPTY;
    private long lastTelemetryTick = -1L;
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
        return res;
    }
}
