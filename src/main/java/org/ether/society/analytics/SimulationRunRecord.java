/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data structure storing telemetry and checkpoint data for an offline simulation run.
 */
public class SimulationRunRecord {

    public static class MetricSnapshot {
        /* Internal state variable for population (long). */
        private final long population;
        /* Internal state variable for food (double). */
        private final double food;
        /* Internal state variable for avg tech (double). */
        private final double avgTech;
        /* Internal state variable for stability (double). */
        private final double stability;
        /* Internal state variable for populated cell count (int). */
        private final int populatedCellCount;
        /* Internal state variable for map (final). */
        private final Map<String, Double> metricsMap;

        /*
         * Metric snapshot.
         * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
         *
         * @param population the population parameter (long)
         * @param food the food parameter (double)
         * @param avgTech the avg tech parameter (double)
         * @param stability the stability parameter (double)
         * @param populatedCellCount the populated cell count parameter (int)
         * @return the resulting computation or state reference
         */
        public MetricSnapshot(long population, double food, double avgTech, double stability, int populatedCellCount) {
            this(population, food, avgTech, stability, populatedCellCount, null);
        }

        /*
         * Metric snapshot.
         * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
         *
         * @param population the population parameter (long)
         * @param food the food parameter (double)
         * @param avgTech the avg tech parameter (double)
         * @param stability the stability parameter (double)
         * @param populatedCellCount the populated cell count parameter (int)
         * @param metricsMap the metrics map parameter (Double&gt;)
         * @return the resulting computation or state reference
         */
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

        /*
         * Get population.
         * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
         *
         * @return the resulting computation or state reference
         */
        public long getPopulation() { return population; }
        /*
         * Get food.
         * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
         *
         * @return the resulting computation or state reference
         */
        public double getFood() { return food; }
        /*
         * Get avg tech.
         * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
         *
         * @return the resulting computation or state reference
         */
        public double getAvgTech() { return avgTech; }
        /*
         * Get stability.
         * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
         *
         * @return the resulting computation or state reference
         */
        public double getStability() { return stability; }
        /*
         * Get populated cell count.
         * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
         *
         * @return the resulting computation or state reference
         */
        public int getPopulatedCellCount() { return populatedCellCount; }
        /*
         * Get metrics map.
         * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
         *
         * @return the resulting computation or state reference
         */
        public Map<String, Double> getMetricsMap() { return Collections.unmodifiableMap(metricsMap); }

        /*
         * Get value.
         * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
         *
         * @param key the key parameter (String)
         * @return the resulting computation or state reference
         */
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

    /* Internal state variable for run id (String). */
    private final String runId;
    /* Internal state variable for scenario name (String). */
    private final String scenarioName;
    /* Internal state variable for variant description (String). */
    private final String variantDescription;
    private final LocalDateTime executionTime;
    /* Internal state variable for map (final). */
    private final Map<String, String> parameterMatrix;
    private final TreeMap<Integer, MetricSnapshot> timeSeriesData;

    private final TreeMap<Integer, List<org.ether.society.database.H3Cell>> spatialSnapshots = new TreeMap<>();

    public SimulationRunRecord(String runId, String scenarioName, String variantDescription,
                               Map<String, String> parameterMatrix) {
        this.runId = runId;
        this.scenarioName = scenarioName;
        this.variantDescription = variantDescription;
        this.executionTime = LocalDateTime.now();
        this.parameterMatrix = parameterMatrix != null ? parameterMatrix : Collections.emptyMap();
        this.timeSeriesData = new TreeMap<>();
    }

    /*
     * Add snapshot.
     * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
     *
     * @param year the year parameter (int)
     * @param population the population parameter (long)
     * @param food the food parameter (double)
     * @param avgTech the avg tech parameter (double)
     * @param stability the stability parameter (double)
     * @param populatedCells the populated cells parameter (int)
     */
    public void addSnapshot(int year, long population, double food, double avgTech, double stability, int populatedCells) {
        addSnapshot(year, population, food, avgTech, stability, populatedCells, null);
    }

    /*
     * Add snapshot.
     * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
     *
     * @param year the year parameter (int)
     * @param population the population parameter (long)
     * @param food the food parameter (double)
     * @param avgTech the avg tech parameter (double)
     * @param stability the stability parameter (double)
     * @param populatedCells the populated cells parameter (int)
     * @param metricsMap the metrics map parameter (Double&gt;)
     */
    public void addSnapshot(int year, long population, double food, double avgTech, double stability, int populatedCells, Map<String, Double> metricsMap) {
        timeSeriesData.put(year, new MetricSnapshot(population, food, avgTech, stability, populatedCells, metricsMap));
    }

    /*
     * Add spatial snapshot.
     * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
     *
     * @param year the year parameter (int)
     * @param cells the cells parameter (List&lt;org.ether.society.database.H3Cell&gt;)
     */
    public void addSpatialSnapshot(int year, List<org.ether.society.database.H3Cell> cells) {
        if (cells != null && !cells.isEmpty()) {
            List<org.ether.society.database.H3Cell> snapshot = cells.stream()
                .map(org.ether.society.database.H3Cell::snapshot)
                .toList();
            spatialSnapshots.put(year, snapshot);
        }
    }

    /*
     * Get spatial snapshot at.
     * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
     *
     * @param year the year parameter (int)
     * @return the resulting computation or state reference
     */
    public List<org.ether.society.database.H3Cell> getSpatialSnapshotAt(int year) {
        if (spatialSnapshots.containsKey(year)) {
            return spatialSnapshots.get(year);
        }
        Map.Entry<Integer, List<org.ether.society.database.H3Cell>> entry = spatialSnapshots.floorEntry(year);
        if (entry != null) return entry.getValue();
        entry = spatialSnapshots.ceilingEntry(year);
        return entry != null ? entry.getValue() : null;
    }

    /*
     * Get spatial snapshots.
     * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
     *
     * @return the resulting computation or state reference
     */
    public TreeMap<Integer, List<org.ether.society.database.H3Cell>> getSpatialSnapshots() {
        return spatialSnapshots;
    }

    /*
     * Get run id.
     * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
     *
     * @return the resulting computation or state reference
     */
    public String getRunId() { return runId; }
    /*
     * Get scenario name.
     * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
     *
     * @return the resulting computation or state reference
     */
    public String getScenarioName() { return scenarioName; }
    /*
     * Get variant description.
     * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
     *
     * @return the resulting computation or state reference
     */
    public String getVariantDescription() { return variantDescription; }
    /*
     * Get execution time.
     * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
     *
     * @return the resulting computation or state reference
     */
    public LocalDateTime getExecutionTime() { return executionTime; }
    /*
     * Get parameter matrix.
     * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
     *
     * @return the resulting computation or state reference
     */
    public Map<String, String> getParameterMatrix() { return parameterMatrix; }
    /*
     * Get time series data.
     * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
     *
     * @return the resulting computation or state reference
     */
    public TreeMap<Integer, MetricSnapshot> getTimeSeriesData() { return timeSeriesData; }

    /*
     * Get snapshot at.
     * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
     *
     * @param year the year parameter (int)
     * @return the resulting computation or state reference
     */
    public MetricSnapshot getSnapshotAt(int year) {
        if (timeSeriesData.containsKey(year)) {
            return timeSeriesData.get(year);
        }
        Map.Entry<Integer, MetricSnapshot> entry = timeSeriesData.floorEntry(year);
        return entry != null ? entry.getValue() : null;
    }

    /*
     * Get start year.
     * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
     *
     * @return the resulting computation or state reference
     */
    public int getStartYear() {
        return timeSeriesData.isEmpty() ? 0 : timeSeriesData.firstKey();
    }

    /*
     * Get end year.
     * Enforces physical invariants and updates associated state variables within {@code SimulationRunRecord}.
     *
     * @return the resulting computation or state reference
     */
    public int getEndYear() {
        return timeSeriesData.isEmpty() ? 0 : timeSeriesData.lastKey();
    }
}
