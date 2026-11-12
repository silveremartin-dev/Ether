/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.core.dod.StatisticsKernel;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.telemetry.CompiledTelemetryPlan;
import org.ether.society.core.telemetry.TelemetryAstCompiler;
import org.ether.society.core.telemetry.TelemetryEvaluationResult;
import org.ether.society.core.telemetry.TelemetrySubscription;
import org.ether.society.data.DODDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.network.codec.CellTopologyWireCodec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Post-hoc telemetry extractor operating directly on serialized world snapshots
 * ({@code snapshot_tick_*.bin.gz}) or in-memory {@link H3Cell} state frames.
 *
 * <p>Uses the exact same {@link TelemetryAstCompiler} / {@link CompiledTelemetryPlan} DAG engine
 * as live simulation ticks, guaranteeing 100% mathematical equivalence without re-executing
 * simulation physics or maintaining duplicate formula logic.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see TelemetryAstCompiler
 * @see SimulationHistoryCoordinator
 */
public final class SnapshotTelemetryExtractor {

    private SnapshotTelemetryExtractor() {
        // Pure static utility class
    }

    /**
     * Evaluates an explicit {@link TelemetrySubscription} directly over a cellular snapshot.
     *
     * @param cells        cellular world snapshot
     * @param subscription desired 1D metrics and/or 2D spatial raster queries
     * @param tick         snapshot tick index
     * @param year         snapshot calendar year
     * @return immutable evaluated telemetry result ({@link TelemetryEvaluationResult#EMPTY} if subscription is empty)
     */
    public static TelemetryEvaluationResult extractFromCells(
            List<H3Cell> cells,
            TelemetrySubscription subscription,
            long tick,
            long year) {
        if (subscription == null || subscription.isEmpty() || cells == null || cells.isEmpty()) {
            return TelemetryEvaluationResult.EMPTY;
        }
        CompiledTelemetryPlan plan = TelemetryAstCompiler.compile(subscription);
        return extractFromCells(cells, plan, tick, year, -1L, 280.0, 0.0);
    }

    /**
     * Evaluates a pre-compiled {@link CompiledTelemetryPlan} directly over a cellular snapshot,
     * bypassing H3 ring-1 neighbor graph allocation for maximum throughput.
     *
     * @param cells             cellular world snapshot
     * @param plan              pre-compiled DAG telemetry plan
     * @param tick              snapshot tick index
     * @param year              snapshot calendar year
     * @param initialPopulation optional initial scenario population $N_0$ ($\le 0$ falls back gracefully)
     * @param t0Co2Ppm          initial atmospheric $\text{CO}_2$ baseline in ppm
     * @param elapsedYears      elapsed years since scenario start
     * @return immutable evaluated telemetry result
     */
    public static TelemetryEvaluationResult extractFromCells(
            List<H3Cell> cells,
            CompiledTelemetryPlan plan,
            long tick,
            long year,
            long initialPopulation,
            double t0Co2Ppm,
            double elapsedYears) {
        if (plan == null || plan.isNoOp() || cells == null || cells.isEmpty()) {
            return TelemetryEvaluationResult.EMPTY;
        }
        WorldBuffer wb = new WorldBuffer(cells.size());
        DODDataGenerator.populateWorldBuffer(cells, wb, false);
        return plan.execute(
                wb,
                null,
                cells,
                List.of(),
                new StatisticsKernel(),
                tick,
                year,
                initialPopulation,
                t0Co2Ppm,
                elapsedYears,
                0.0);
    }

    /**
     * Loads a persisted compressed cellular snapshot from disk and extracts all subscribed
     * 1D scalar metrics and 2D spatial raster slices/statistics.
     *
     * @param snapshotFile path to {@code snapshot_tick_*.bin.gz}
     * @param subscription desired metrics and spatial queries
     * @param tick         snapshot tick index
     * @param year         snapshot calendar year
     * @return immutable telemetry evaluation result
     * @throws IOException if the snapshot archive cannot be read or decoded
     */
    public static TelemetryEvaluationResult extractFromSnapshotFile(
            Path snapshotFile,
            TelemetrySubscription subscription,
            long tick,
            long year) throws IOException {
        if (snapshotFile == null || !Files.exists(snapshotFile) || subscription == null || subscription.isEmpty()) {
            return TelemetryEvaluationResult.EMPTY;
        }
        List<H3Cell> cells = CellTopologyWireCodec.loadFromFile(snapshotFile);
        return extractFromCells(cells, subscription, tick, year);
    }

    /**
     * Streams every chronological world snapshot stored in {@link SimulationHistoryCoordinator} through a single
     * compiled {@link CompiledTelemetryPlan}, returning an ordered timeline of extracted results
     * without re-running the simulation engine.
     *
     * @param historyManager snapshot repository containing persisted {@link H3Cell} frames
     * @param subscription   explicit telemetry and spatial subscription
     * @param startYear      scenario start year
     * @param ticksPerYear   ticks elapsed per simulated calendar year ($\ge 1$)
     * @return chronologically ordered map of {@code tick -> TelemetryEvaluationResult}
     */
    public static NavigableMap<Long, TelemetryEvaluationResult> extractTimeline(
            SimulationHistoryCoordinator historyManager,
            TelemetrySubscription subscription,
            long startYear,
            double ticksPerYear) {
        NavigableMap<Long, TelemetryEvaluationResult> timeline = new TreeMap<>();
        if (historyManager == null || subscription == null || subscription.isEmpty()) {
            return timeline;
        }
        CompiledTelemetryPlan plan = TelemetryAstCompiler.compile(subscription);
        if (plan.isNoOp()) {
            return timeline;
        }

        double tpy = Math.max(1.0, ticksPerYear);
        int count = historyManager.getSnapshotCount();
        long initialPop = -1L;

        for (int idx = 0; idx < count; idx++) {
            Long tickObj = historyManager.getTickByIndex(idx);
            if (tickObj == null) continue;
            long tick = tickObj;
            if (!subscription.shouldEvaluateAtTick(tick)) {
                continue;
            }
            List<H3Cell> cells = historyManager.getWorldSnapshot(tick);
            if (cells == null || cells.isEmpty()) continue;

            double elapsedYears = tick / tpy;
            long year = startYear + Math.round(elapsedYears);
            if (initialPop <= 0L) {
                long sum = 0L;
                for (H3Cell c : cells) {
                    sum += c.getPopulationOr(0);
                }
                initialPop = Math.max(1L, sum);
            }

            TelemetryEvaluationResult res = extractFromCells(cells, plan, tick, year, initialPop, 280.0, elapsedYears);
            timeline.put(tick, res);
        }
        return timeline;
    }
}
