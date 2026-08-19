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

    public enum InterpolationMethod {
        PCHIP_MONOTONE_CUBIC,
        CATMULL_ROM_SPLINE,
        LINEAR
    }

    /**
     * Retrieves the C1 continuous and differentiable interpolated historical benchmark value for a given metric key and year.
     * Uses Piecewise Cubic Hermite Interpolating Polynomial (PCHIP) by default to guarantee monotonicity preservation
     * and smooth derivative continuity without non-physical oscillations.
     */
    public static double getInterpolatedBenchmarkValue(String key, double year) {
        return getInterpolatedBenchmarkValue(key, year, InterpolationMethod.PCHIP_MONOTONE_CUBIC);
    }

    public static double getInterpolatedBenchmarkValue(String key, double year, InterpolationMethod method) {
        Map<Integer, Double> dataset = getBenchmarkDataset(key);
        if (dataset == null || dataset.isEmpty()) return 0.0;

        List<Integer> years = new ArrayList<>(dataset.keySet());
        Collections.sort(years);

        int n = years.size();
        if (n == 0) return 0.0;
        if (n == 1) return dataset.get(years.get(0));
        if (year <= years.get(0)) return dataset.get(years.get(0));
        if (year >= years.get(n - 1)) return dataset.get(years.get(n - 1));

        if (method == InterpolationMethod.LINEAR) {
            return linearInterpolate(dataset, years, year);
        } else if (method == InterpolationMethod.CATMULL_ROM_SPLINE) {
            return catmullRomInterpolate(dataset, years, year);
        } else {
            return pchipInterpolate(dataset, years, year);
        }
    }

    private static double linearInterpolate(Map<Integer, Double> dataset, List<Integer> x, double targetX) {
        int intYear = (int) Math.round(targetX);
        if (dataset.containsKey(intYear)) return dataset.get(intYear);

        Integer lowerKey = null;
        Integer upperKey = null;

        for (Integer k : x) {
            if (k <= targetX) {
                lowerKey = k;
            } else if (k > targetX && upperKey == null) {
                upperKey = k;
                break;
            }
        }

        if (lowerKey == null && upperKey == null) return 0.0;
        if (lowerKey == null) return dataset.get(upperKey);
        if (upperKey == null) return dataset.get(lowerKey);

        double valLower = dataset.get(lowerKey);
        double valUpper = dataset.get(upperKey);

        double span = upperKey - lowerKey;
        if (span <= 0) return valLower;

        double fraction = (targetX - lowerKey) / span;
        return valLower + fraction * (valUpper - valLower);
    }

    private static double pchipInterpolate(Map<Integer, Double> dataset, List<Integer> x, double targetX) {
        int n = x.size();
        double[] h = new double[n - 1];
        double[] delta = new double[n - 1];
        double[] y = new double[n];

        for (int i = 0; i < n; i++) {
            y[i] = dataset.get(x.get(i));
        }

        for (int i = 0; i < n - 1; i++) {
            h[i] = x.get(i + 1) - x.get(i);
            delta[i] = (y[i + 1] - y[i]) / h[i];
        }

        // Derivatives d[i] for Monotone Hermite Cubic
        double[] d = new double[n];

        // Interior nodes (weighted harmonic mean)
        for (int i = 1; i < n - 1; i++) {
            if (delta[i - 1] * delta[i] <= 0.0) {
                d[i] = 0.0;
            } else {
                double w1 = 2.0 * h[i] + h[i - 1];
                double w2 = h[i] + 2.0 * h[i - 1];
                d[i] = (w1 + w2) / (w1 / delta[i - 1] + w2 / delta[i]);
            }
        }

        // Boundary nodes
        d[0] = pchipEndDerivative(h[0], h[1], delta[0], delta[1]);
        d[n - 1] = pchipEndDerivative(h[n - 2], (n >= 4 ? h[n - 3] : h[n - 2]), delta[n - 2], (n >= 3 ? delta[n - 3] : delta[n - 2]));

        // Interval search
        int k = 0;
        for (int i = 0; i < n - 1; i++) {
            if (targetX >= x.get(i) && targetX <= x.get(i + 1)) {
                k = i;
                break;
            }
        }

        double hk = h[k];
        double t = (targetX - x.get(k)) / hk;
        double t2 = t * t;
        double t3 = t2 * t;

        // Hermite cubic basis functions
        double h00 = 2.0 * t3 - 3.0 * t2 + 1.0;
        double h10 = t3 - 2.0 * t2 + t;
        double h01 = -2.0 * t3 + 3.0 * t2;
        double h11 = t3 - t2;

        return h00 * y[k] + h10 * hk * d[k] + h01 * y[k + 1] + h11 * hk * d[k + 1];
    }

    private static double pchipEndDerivative(double h1, double h2, double delta1, double delta2) {
        double d = ((2.0 * h1 + h2) * delta1 - h1 * delta2) / (h1 + h2);
        if (d * delta1 < 0.0) {
            d = 0.0;
        } else if ((delta1 * delta2 < 0.0) && (Math.abs(d) > Math.abs(3.0 * delta1))) {
            d = 3.0 * delta1;
        }
        return d;
    }

    private static double catmullRomInterpolate(Map<Integer, Double> dataset, List<Integer> x, double targetX) {
        int n = x.size();
        int k = 0;
        for (int i = 0; i < n - 1; i++) {
            if (targetX >= x.get(i) && targetX <= x.get(i + 1)) {
                k = i;
                break;
            }
        }

        double y0 = dataset.get(x.get(Math.max(0, k - 1)));
        double y1 = dataset.get(x.get(k));
        double y2 = dataset.get(x.get(Math.min(n - 1, k + 1)));
        double y3 = dataset.get(x.get(Math.min(n - 1, k + 2)));

        double hk = x.get(Math.min(n - 1, k + 1)) - x.get(k);
        if (hk <= 0) return y1;

        double t = (targetX - x.get(k)) / hk;
        double t2 = t * t;
        double t3 = t2 * t;

        double a0 = -0.5 * y0 + 1.5 * y1 - 1.5 * y2 + 0.5 * y3;
        double a1 = y0 - 2.5 * y1 + 2.0 * y2 - 0.5 * y3;
        double a2 = -0.5 * y0 + 0.5 * y2;
        double a3 = y1;

        return a0 * t3 + a1 * t2 + a2 * t + a3;
    }

    public static Map<Integer, Double> getHistoricalWorldPopulation() { return getBenchmarkDataset("worldPopulation"); }
    public static Map<Integer, Double> getHistoricalWorldGdp() { return getBenchmarkDataset("grossWorldProduct"); }
    public static Map<Integer, Double> getHistoricalWorldEnergy() { return getBenchmarkDataset("primaryEnergy"); }
    public static Map<Integer, Double> getHistoricalUrbanizationRate() { return getBenchmarkDataset("urbanizationRate"); }
    public static Map<Integer, Double> getHistoricalCo2Concentration() { return getBenchmarkDataset("co2Concentration"); }
    public static Map<Integer, Double> getHistoricalLiteracyRate() { return getBenchmarkDataset("literacyRate"); }
    public static Map<Integer, Double> getHistoricalCurrencyDebasement() { return getBenchmarkDataset("currencyDebasement"); }
}
