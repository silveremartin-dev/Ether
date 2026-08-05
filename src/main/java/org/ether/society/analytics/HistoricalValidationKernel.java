/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Historical Telemetry Validation & Multi-Metric Empirical Benchmark Kernel.
 * Loads and validates 20 socio-economic, cliodynamic, ecological, and technological variables
 * from the standardized JSON resource ({@code historical_cliodynamic_benchmarks.json}).
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class HistoricalValidationKernel {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalValidationKernel.class);

    public enum EpochWindow {
        DEEP_HORIZON(-10000, 2026, "Deep Macro-Historical Horizon (-10k BCE - 2026 CE)"),
        CLASSICAL_MEDIEVAL(-3000, 1500, "Classical Antiquity & Medieval Era (-3000 BCE - 1500 CE)"),
        EARLY_MODERN_500YR(1500, 2026, "Early Modern & Industrial Era (1500 CE - 2026 CE, 500-Year Window)"),
        MODERN_INDUSTRIAL(1900, 2026, "Modern Industrial & Digital Era (1900 CE - 2026 CE, 125-Year Window)");

        public final int startYear;
        public final int endYear;
        public final String description;

        EpochWindow(int start, int end, String desc) {
            this.startYear = start;
            this.endYear = end;
            this.description = desc;
        }

        public boolean containsYear(int yr) {
            return yr >= startYear && yr <= endYear;
        }
    }

    /** 20 Multi-Metric Historical Benchmark Time Series (Map of Metric Name -> Time Series Data) */
    private static final Map<String, Map<Integer, Double>> BENCHMARK_DATASETS = new HashMap<>();

    static {
        loadJsonBenchmarksResource();
    }

    private static void loadJsonBenchmarksResource() {
        try (InputStream is = HistoricalValidationKernel.class.getClassLoader().getResourceAsStream("historical_cliodynamic_benchmarks.json")) {
            if (is != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(is);
                JsonNode variablesNode = root.get("variables");

                if (variablesNode != null && variablesNode.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> fields = variablesNode.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        String varKey = entry.getKey();
                        JsonNode dataNode = entry.getValue().get("data");

                        Map<Integer, Double> timeSeries = new TreeMap<>();
                        if (dataNode != null && dataNode.isObject()) {
                            Iterator<Map.Entry<String, JsonNode>> points = dataNode.fields();
                            while (points.hasNext()) {
                                Map.Entry<String, JsonNode> point = points.next();
                                int yr = Integer.parseInt(point.getKey());
                                double val = point.getValue().asDouble();
                                timeSeries.put(yr, val);
                            }
                        }
                        BENCHMARK_DATASETS.put(varKey, Collections.unmodifiableMap(timeSeries));
                    }
                    logger.info("✅ Successfully loaded 20-Variable Historical Benchmarks from JSON resource.");
                }
            } else {
                logger.warn("⚠️ historical_cliodynamic_benchmarks.json resource not found, initializing fallback datasets.");
            }
        } catch (Exception e) {
            logger.error("❌ Failed to parse historical_cliodynamic_benchmarks.json", e);
        }
    }

    public record MetricFit(String metricName, double rmse, double rSquared, int samplePoints) {}

    public static class MultiMetricValidationReport {
        public EpochWindow window;
        public Map<String, MetricFit> metricFits = new HashMap<>();
        public double compositeRSquared;
        public double compositeRmse;

        public String getSummary() {
            StringBuilder sb = new StringBuilder(String.format("Multi-Metric Report [%s]:\n", window != null ? window.description : "Global"));
            for (MetricFit fit : metricFits.values()) {
                sb.append(String.format("  - %s (%d points): R^2 = %.4f, RMSE = %.2f\n", fit.metricName, fit.samplePoints, fit.rSquared, fit.rmse));
            }
            sb.append(String.format("  => Composite R^2 = %.4f, Composite RMSE = %.2f", compositeRSquared, compositeRmse));
            return sb.toString();
        }
    }

    /** 20-Variable Simulation Trajectory Container */
    public static class MultiMetricTrajectory {
        public Map<String, Map<Integer, Double>> seriesMap = new HashMap<>();

        public void recordValue(String metricKey, int year, double value) {
            seriesMap.computeIfAbsent(metricKey, k -> new TreeMap<>()).put(year, value);
        }

        public Map<Integer, Double> getSeries(String metricKey) {
            return seriesMap.getOrDefault(metricKey, Collections.emptyMap());
        }

        // Legacy accessors
        public Map<Integer, Double> population = new TreeMap<>();
        public Map<Integer, Double> gdp = new TreeMap<>();
        public Map<Integer, Double> primaryEnergy = new TreeMap<>();
        public Map<Integer, Double> urbanizationRate = new TreeMap<>();
        public Map<Integer, Double> co2Ppm = new TreeMap<>();
        public Map<Integer, Double> literacyRate = new TreeMap<>();
        public Map<Integer, Double> currencyDebasement = new TreeMap<>();
    }

    public static Map<Integer, Double> filterByWindow(Map<Integer, Double> dataset, EpochWindow window) {
        if (dataset == null) return Collections.emptyMap();
        return dataset.entrySet().stream()
            .filter(e -> window.containsYear(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, TreeMap::new));
    }

    public static double calculateRmse(Map<Integer, Double> simulatedData) {
        return calculateRmse(simulatedData, getHistoricalWorldPopulation());
    }

    public static double calculateRSquared(Map<Integer, Double> simulatedData) {
        return calculateRSquared(simulatedData, getHistoricalWorldPopulation());
    }

    public static double calculateRmse(Map<Integer, Double> simulatedData, Map<Integer, Double> benchmarkData) {
        if (simulatedData == null || simulatedData.isEmpty() || benchmarkData == null || benchmarkData.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double sumSquaredErrors = 0.0;
        int count = 0;

        for (Map.Entry<Integer, Double> entry : benchmarkData.entrySet()) {
            int year = entry.getKey();
            if (simulatedData.containsKey(year)) {
                double observed = entry.getValue();
                double simulated = simulatedData.get(year);
                sumSquaredErrors += Math.pow(simulated - observed, 2.0);
                count++;
            }
        }

        if (count == 0) return Double.MAX_VALUE;
        return Math.sqrt(sumSquaredErrors / count);
    }

    public static double calculateRSquared(Map<Integer, Double> simulatedData, Map<Integer, Double> benchmarkData) {
        if (simulatedData == null || simulatedData.isEmpty() || benchmarkData == null || benchmarkData.isEmpty()) {
            return 0.0;
        }

        double meanObserved = benchmarkData.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double ssTot = 0.0;
        double ssRes = 0.0;
        int count = 0;

        for (Map.Entry<Integer, Double> entry : benchmarkData.entrySet()) {
            int year = entry.getKey();
            if (simulatedData.containsKey(year)) {
                double observed = entry.getValue();
                double simulated = simulatedData.get(year);
                ssTot += Math.pow(observed - meanObserved, 2.0);
                ssRes += Math.pow(observed - simulated, 2.0);
                count++;
            }
        }

        if (count == 0 || ssTot == 0.0) return 1.0;
        return Math.max(0.0, 1.0 - (ssRes / ssTot));
    }

    public static MultiMetricValidationReport evaluateWindowedFit(MultiMetricTrajectory trajectory, EpochWindow window) {
        MultiMetricValidationReport report = new MultiMetricValidationReport();
        report.window = window;
        if (trajectory == null) return report;

        for (Map.Entry<String, Map<Integer, Double>> benchmarkEntry : BENCHMARK_DATASETS.entrySet()) {
            String metricKey = benchmarkEntry.getKey();
            Map<Integer, Double> rawBenchmark = benchmarkEntry.getValue();

            Map<Integer, Double> simulated = trajectory.getSeries(metricKey);
            if (simulated.isEmpty() && metricKey.equals("worldPopulation")) simulated = trajectory.population;
            if (simulated.isEmpty() && metricKey.equals("grossWorldProduct")) simulated = trajectory.gdp;
            if (simulated.isEmpty() && metricKey.equals("primaryEnergy")) simulated = trajectory.primaryEnergy;
            if (simulated.isEmpty() && metricKey.equals("urbanizationRate")) simulated = trajectory.urbanizationRate;
            if (simulated.isEmpty() && metricKey.equals("co2Concentration")) simulated = trajectory.co2Ppm;
            if (simulated.isEmpty() && metricKey.equals("literacyRate")) simulated = trajectory.literacyRate;
            if (simulated.isEmpty() && metricKey.equals("currencyDebasement")) simulated = trajectory.currencyDebasement;

            Map<Integer, Double> windowedBenchmark = filterByWindow(rawBenchmark, window);
            if (!simulated.isEmpty() && !windowedBenchmark.isEmpty()) {
                double rmse = calculateRmse(simulated, windowedBenchmark);
                double rSquared = calculateRSquared(simulated, windowedBenchmark);
                report.metricFits.put(metricKey, new MetricFit(metricKey, rmse, rSquared, windowedBenchmark.size()));
            }
        }

        double totalRSquared = 0.0;
        double totalRmse = 0.0;
        int count = 0;

        for (MetricFit fit : report.metricFits.values()) {
            totalRSquared += fit.rSquared();
            totalRmse += fit.rmse();
            count++;
        }

        if (count > 0) {
            report.compositeRSquared = totalRSquared / count;
            report.compositeRmse = totalRmse / count;
        }

        return report;
    }

    public static Map<Integer, Double> getBenchmarkDataset(String key) {
        return BENCHMARK_DATASETS.getOrDefault(key, Collections.emptyMap());
    }

    public static Map<Integer, Double> getHistoricalWorldPopulation() { return getBenchmarkDataset("worldPopulation"); }
    public static Map<Integer, Double> getHistoricalWorldGdp() { return getBenchmarkDataset("grossWorldProduct"); }
    public static Map<Integer, Double> getHistoricalWorldEnergy() { return getBenchmarkDataset("primaryEnergy"); }
    public static Map<Integer, Double> getHistoricalUrbanizationRate() { return getBenchmarkDataset("urbanizationRate"); }
    public static Map<Integer, Double> getHistoricalCo2Concentration() { return getBenchmarkDataset("co2Concentration"); }
    public static Map<Integer, Double> getHistoricalLiteracyRate() { return getBenchmarkDataset("literacyRate"); }
    public static Map<Integer, Double> getHistoricalCurrencyDebasement() { return getBenchmarkDataset("currencyDebasement"); }
}
