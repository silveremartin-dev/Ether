/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.time.LocalDateTime;
import java.util.Collections;
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

        public MetricSnapshot(long population, double food, double avgTech, double stability, int populatedCellCount) {
            this.population = population;
            this.food = food;
            this.avgTech = avgTech;
            this.stability = stability;
            this.populatedCellCount = populatedCellCount;
        }

        public long getPopulation() { return population; }
        public double getFood() { return food; }
        public double getAvgTech() { return avgTech; }
        public double getStability() { return stability; }
        public int getPopulatedCellCount() { return populatedCellCount; }
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
        timeSeriesData.put(year, new MetricSnapshot(population, food, avgTech, stability, populatedCells));
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
