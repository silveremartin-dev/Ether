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
 * <h1>Simulation Run Record</h1>
 * <p>
 * Provides statistical analytics, empirical validation harnesses, and parameter calibration kernels.<br>
 * Evaluates simulation trajectories using multi-metric error metrics (Mean Absolute Percentage Error, Pearson correlation, and spatial centroid divergence).
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SimulationRunRecord {

    public static class MetricSnapshot {
        /** Total living human population count across the active domain. */
        private final long population;
        /** Per-capita caloric food availability (\(\text{kcal/day}\) or \(\text{GJ}\)). */
        private final double food;
        /** Read-only Garrett-Lotka emergent exergy-capital diagnostic \(\Phi_{\text{exergy}}(p, k_{\text{pc}})\). */
        private final double avgTech;
        /** Mean institutional and social cohesion stability score \(\in [0, 100]\). */
        private final double stability;
        /** Number of inhabited H3 cells (\(N_i > 0\)). */
        private final int populatedCellCount;
        /** Full key-value dictionary of recorded physical and cliodynamic telemetry indicators. */
        private final Map<String, Double> metricsMap;

        /**
         * Constructs a telemetry snapshot with core physical aggregates.
         *
         * @param population         total human population \(N\)
         * @param food               food availability per capita
         * @param avgTech            emergent Garrett-Lotka exergy-capital index \(\Phi_{\text{exergy}}\)
         * @param stability          institutional stability score \(\in [0, 100]\)
         * @param populatedCellCount number of inhabited cells
         */
        public MetricSnapshot(long population, double food, double avgTech, double stability, int populatedCellCount) {
            this(population, food, avgTech, stability, populatedCellCount, null);
        }

        /**
         * Constructs a telemetry snapshot with core physical aggregates and an extended indicator map.
         *
         * @param population         total human population \(N\)
         * @param food               food availability per capita
         * @param avgTech            emergent Garrett-Lotka exergy-capital index \(\Phi_{\text{exergy}}\)
         * @param stability          institutional stability score \(\in [0, 100]\)
         * @param populatedCellCount number of inhabited cells
         * @param metricsMap         extended map of telemetry variables
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

        /** @return total living human population \(N\) */
        public long getPopulation() { return population; }
        /** @return caloric food availability */
        public double getFood() { return food; }
        /** @return read-only Garrett-Lotka emergent exergy-capital diagnostic \(\Phi_{\text{exergy}}\) */
        public double getAvgTech() { return avgTech; }
        /** @return institutional stability score \(\in [0, 100]\) */
        public double getStability() { return stability; }
        /** @return count of inhabited H3 cells */
        public int getPopulatedCellCount() { return populatedCellCount; }
        /** @return unmodifiable view of all recorded telemetry indicators */
        public Map<String, Double> getMetricsMap() { return Collections.unmodifiableMap(metricsMap); }

        /**
         * Retrieves a recorded metric value by canonical identifier or descriptor key.
         *
         * @param key canonical metric identifier or display label
         * @return recorded metric value, or {@code 0.0} if absent
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

    /** Internal state: run id (String). */
    private final String runId;
    /** Internal state: scenario name (String). */
    private final String scenarioName;
    /** Internal state: variant description (String). */
    private final String variantDescription;
    private final LocalDateTime executionTime;
    /** Internal state: map (final). */
    private final Map<String, String> parameterMatrix;
    private final TreeMap<Integer, MetricSnapshot> timeSeriesData;

    /** Maximum number of full 2D spatial cell snapshots retained in JVM heap memory. */
    public static final int MAX_SPATIAL_SNAPSHOTS_IN_MEMORY = 16;

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

    /**
     * Add snapshot.
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

    /**
     * Add snapshot.
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

    /**
     * Add spatial snapshot with bounded in-memory eviction policy to prevent JVM heap exhaustion.
     * Preserves the initial baseline ($t = t_0$) and the most recent milestones.
     *
     * @param year simulation calendar year
     * @param cells cell collection to capture
     */
    public void addSpatialSnapshot(int year, List<org.ether.society.database.H3Cell> cells) {
        if (cells != null && !cells.isEmpty()) {
            if (spatialSnapshots.size() >= MAX_SPATIAL_SNAPSHOTS_IN_MEMORY && !spatialSnapshots.containsKey(year)) {
                // Keep initial baseline (first key), evict an intermediate milestone to bound RAM consumption
                Integer firstKey = spatialSnapshots.firstKey();
                for (Integer key : spatialSnapshots.keySet()) {
                    if (!key.equals(firstKey)) {
                        spatialSnapshots.remove(key);
                        break;
                    }
                }
            }
            List<org.ether.society.database.H3Cell> snapshot = cells.stream()
                .map(org.ether.society.database.H3Cell::snapshot)
                .toList();
            spatialSnapshots.put(year, snapshot);
        }
    }

    /**
     * Explicitly frees in-memory spatial snapshots to relieve garbage collection pressure.
     */
    public void clearSpatialSnapshots() {
        spatialSnapshots.clear();
    }

    /**
     * Get spatial snapshot at.
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

    /**
     * Get spatial snapshots.
     *
     * @return the resulting computation or state reference
     */
    public TreeMap<Integer, List<org.ether.society.database.H3Cell>> getSpatialSnapshots() {
        return spatialSnapshots;
    }

    /**
     * Get run id.
     *
     * @return the resulting computation or state reference
     */
    public String getRunId() { return runId; }
    /**
     * Get scenario name.
     *
     * @return the resulting computation or state reference
     */
    public String getScenarioName() { return scenarioName; }
    /**
     * Get variant description.
     *
     * @return the resulting computation or state reference
     */
    public String getVariantDescription() { return variantDescription; }
    /**
     * Get execution time.
     *
     * @return the resulting computation or state reference
     */
    public LocalDateTime getExecutionTime() { return executionTime; }
    /**
     * Get parameter matrix.
     *
     * @return the resulting computation or state reference
     */
    public Map<String, String> getParameterMatrix() { return parameterMatrix; }
    /**
     * Get time series data.
     *
     * @return the resulting computation or state reference
     */
    public TreeMap<Integer, MetricSnapshot> getTimeSeriesData() { return timeSeriesData; }

    /**
     * Get snapshot at.
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

    /**
     * Get start year.
     *
     * @return the resulting computation or state reference
     */
    public int getStartYear() {
        return timeSeriesData.isEmpty() ? 0 : timeSeriesData.firstKey();
    }

    /**
     * Get end year.
     *
     * @return the resulting computation or state reference
     */
    public int getEndYear() {
        return timeSeriesData.isEmpty() ? 0 : timeSeriesData.lastKey();
    }

    /**
     * Retrieves the populated cell count from the final recorded metric snapshot.
     *
     * @return cell count in final snapshot, or 0 if no snapshots exist
     */
    public int getFinalCellCount() {
        if (timeSeriesData.isEmpty()) return 0;
        MetricSnapshot last = timeSeriesData.lastEntry().getValue();
        return last != null ? last.getPopulatedCellCount() : 0;
    }
}
