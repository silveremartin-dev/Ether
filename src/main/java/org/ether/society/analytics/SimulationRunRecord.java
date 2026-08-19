/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data structure storing telemetry and checkpoint data for an offline simulation run.
 */
public class SimulationRunRecord {

    public static class MetricSnapshot {
        private final long population;
        private final double food;
        private final double avgTech;
        private final double stability;
        private final int populatedCellCount;
        private final Map<String, Double> metricsMap;

        public MetricSnapshot(long population, double food, double avgTech, double stability, int populatedCellCount) {
            this(population, food, avgTech, stability, populatedCellCount, null);
        }

        public MetricSnapshot(long population, double food, double avgTech, double stability, int populatedCellCount, Map<String, Double> metricsMap) {
            this.population = population;
            this.food = food;
            this.avgTech = avgTech;
            this.stability = stability;
            this.populatedCellCount = populatedCellCount;
            this.metricsMap = metricsMap != null ? new LinkedHashMap<>(metricsMap) : new LinkedHashMap<>();

            // Guarantee primary keys are present in metricsMap
            this.metricsMap.putIfAbsent("population", (double) population);
            this.metricsMap.putIfAbsent("foodPerCapita", food);
            this.metricsMap.putIfAbsent("avgTechLevel", avgTech);
            this.metricsMap.putIfAbsent("asabiyyah", stability);
            this.metricsMap.putIfAbsent("populatedCellCount", (double) populatedCellCount);
        }

        public long getPopulation() { return population; }
        public double getFood() { return food; }
        public double getAvgTech() { return avgTech; }
        public double getStability() { return stability; }
        public int getPopulatedCellCount() { return populatedCellCount; }
        public Map<String, Double> getMetricsMap() { return Collections.unmodifiableMap(metricsMap); }

        public double getValue(String key) {
            if (key == null) return (double) population;
            MetricDescriptor desc = MetricRegistry.getInstance().getDescriptor(key);
            String id = desc != null ? desc.getId() : key;
            if (metricsMap.containsKey(id)) {
                return metricsMap.get(id);
            }
            if (metricsMap.containsKey(key)) {
                return metricsMap.get(key);
            }
            // Backward compatibility fallbacks
            if (key.contains("Population")) return (double) population;
            if (key.contains("Tech")) return avgTech;
            if (key.contains("Asabiyyah") || key.contains("Stabilité")) return stability;
            if (key.contains("Food") || key.contains("Alimentaire")) return food;
            return 0.0;
        }
    }

    private final String runId;
    private final String scenarioName;
    private final String variantDescription;
    private final LocalDateTime executionTime;
    private final Map<String, String> parameterMatrix;
    private final TreeMap<Integer, MetricSnapshot> timeSeriesData;

    public SimulationRunRecord(String runId, String scenarioName, String variantDescription,
                               Map<String, String> parameterMatrix) {
        this.runId = runId;
        this.scenarioName = scenarioName;
        this.variantDescription = variantDescription;
        this.executionTime = LocalDateTime.now();
        this.parameterMatrix = parameterMatrix != null ? parameterMatrix : Collections.emptyMap();
        this.timeSeriesData = new TreeMap<>();
    }

    public void addSnapshot(int year, long population, double food, double avgTech, double stability, int populatedCells) {
        addSnapshot(year, population, food, avgTech, stability, populatedCells, null);
    }

    public void addSnapshot(int year, long population, double food, double avgTech, double stability, int populatedCells, Map<String, Double> metricsMap) {
        timeSeriesData.put(year, new MetricSnapshot(population, food, avgTech, stability, populatedCells, metricsMap));
    }

    public String getRunId() { return runId; }
    public String getScenarioName() { return scenarioName; }
    public String getVariantDescription() { return variantDescription; }
    public LocalDateTime getExecutionTime() { return executionTime; }
    public Map<String, String> getParameterMatrix() { return parameterMatrix; }
    public TreeMap<Integer, MetricSnapshot> getTimeSeriesData() { return timeSeriesData; }

    public MetricSnapshot getSnapshotAt(int year) {
        if (timeSeriesData.containsKey(year)) {
            return timeSeriesData.get(year);
        }
        Map.Entry<Integer, MetricSnapshot> entry = timeSeriesData.floorEntry(year);
        return entry != null ? entry.getValue() : null;
    }

    public int getStartYear() {
        return timeSeriesData.isEmpty() ? 0 : timeSeriesData.firstKey();
    }

    public int getEndYear() {
        return timeSeriesData.isEmpty() ? 0 : timeSeriesData.lastKey();
    }
}
