/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.SimulationMetricsAggregator;
import org.ether.society.core.telemetry.CompiledTelemetryPlan;
import org.ether.society.core.telemetry.TelemetryAstCompiler;
import org.ether.society.core.telemetry.TelemetryEvaluationResult;
import org.ether.society.core.telemetry.TelemetrySubscription;
import org.ether.society.database.H3Cell;

import java.util.*;

/**
 * Central Metric Catalog & Registry for the Ether Simulation Platform.
 * Uniformly registers all 2D spatial metrics, 1D time-series aggregations,
 * and 0D scenario indicators across Map Canvas, Stats Panel, and Comparative Analytics.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class MetricRegistry {

    private static final MetricRegistry INSTANCE = new MetricRegistry();
    private final Map<String, MetricDescriptor> metricsById = new LinkedHashMap<>();
    private final Map<String, MetricDescriptor> metricsByName = new LinkedHashMap<>();

    private MetricRegistry() {
        registerDefaultMetrics();
    }

    /**
     * Get instance.
     *
     * @return the resulting computation or state reference
     */
    public static MetricRegistry getInstance() {
        return INSTANCE;
    }

    private void register(MetricDescriptor descriptor) {
        metricsById.put(descriptor.getId(), descriptor);
        metricsByName.put(descriptor.getDisplayName(), descriptor);
    }

    private void registerDefaultMetrics() {
        MetricCatalogHelper.registerAll(this::register);
    }

    /**
     * Get descriptor.
     *
     * @param id the id parameter (String)
     * @return the resulting computation or state reference
     */
    public MetricDescriptor getDescriptor(String id) {
        if (id == null) return null;
        if (metricsById.containsKey(id)) return metricsById.get(id);
        for (MetricDescriptor d : metricsById.values()) {
            if (d.getDisplayName().equalsIgnoreCase(id)) return d;
        }
        return metricsByName.get(id);
    }

    /**
     * Get descriptor by name.
     *
     * @param displayName the display name parameter (String)
     * @return the resulting computation or state reference
     */
    public MetricDescriptor getDescriptorByName(String displayName) {
        if (displayName == null) return null;
        if (metricsByName.containsKey(displayName)) return metricsByName.get(displayName);
        for (MetricDescriptor d : metricsById.values()) {
            if (d.getDisplayName().equalsIgnoreCase(displayName)) return d;
        }
        return null;
    }

    /**
     * Get all metrics.
     *
     * @return the resulting computation or state reference
     */
    public Collection<MetricDescriptor> getAllMetrics() {
        return Collections.unmodifiableCollection(metricsById.values());
    }

    /**
     * Get all metric names.
     *
     * @return the resulting computation or state reference
     */
    public List<String> getAllMetricNames() {
        List<String> list = new ArrayList<>();
        for (MetricDescriptor d : metricsById.values()) {
            list.add(d.getDisplayName());
        }
        return list;
    }

    /**
     * Computes a full map of metric snapshot values for the given simulation engine state.
     * Records 100% of physical, environmental, macroeconomic, and cliodynamic variables
     * calculated directly by the engine and its metrics aggregator.
     */
    public Map<String, Double> computeMetricsMap(H3SimulationEngine engine) {
        Map<String, Double> map = new LinkedHashMap<>();
        if (engine == null) return map;

        // 0. Fast-path reuse of memoized single-pass AST/DAG telemetry for this tick
        TelemetryEvaluationResult cached = engine.getCachedTelemetryResult();
        if (cached != null && !cached.isEmpty() && engine.getLastTelemetryTick() == engine.getTickCounter()) {
            map.putAll(cached.getMetrics());
            return map;
        }

        // If the engine has an active subscription, evaluate its minimal compiled DAG once
        if (!engine.getTelemetrySubscription().isEmpty()) {
            TelemetryEvaluationResult eval = engine.evaluateTelemetryNow();
            map.putAll(eval.getMetrics());
            return map;
        }

        // Explicit ad-hoc audit call on an unsubscribed engine: compile & run canonical benchmark DAG in one pass
        CompiledTelemetryPlan adhocPlan = TelemetryAstCompiler.compile(TelemetrySubscription.allCanonicalBenchmarks(1));
        TelemetryEvaluationResult eval = adhocPlan.execute(
                engine.getWorldBuffer(),
                engine.getAgentBuffer(),
                engine.getCells(),
                engine.getNations(),
                null,
                engine.getTickCounter(),
                engine.getCurrentYear(),
                -1L,
                280.0,
                0.0,
                engine.getCurrentTPS());
        map.putAll(eval.getMetrics());
        List<H3Cell> cells = engine.getCells();
        if (cells != null && !cells.isEmpty()) {
            for (MetricDescriptor desc : metricsById.values()) {
                if (!map.containsKey(desc.getId())) {
                    map.put(desc.getId(), desc.aggregate(cells));
                }
            }
        }
        return map;
    }
}


