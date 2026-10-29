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
 * Loads and validates socio-economic, cliodynamic, ecological, and technological variables
 * from the standardized JSON resource ({@code historical_cliodynamic_benchmarks.json}).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
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

        /**
         * Contains year.
         *
         * @param yr the yr parameter (int)
         * @return the resulting computation or state reference
         */
        public boolean containsYear(int yr) {
            return yr >= startYear && yr <= endYear;
        }
    }

    public record BenchmarkVariableMeta(
        String key,
        String name,
        String unit,
        String window,
        String description,
        String source,
        int minYear,
        int maxYear
    ) {}

    /** Multi-Metric Historical Benchmark Time Series (Map of Metric Name -> Time Series Data) */
    private static final Map<String, Map<Integer, Double>> BENCHMARK_DATASETS = new HashMap<>();
    private static final Map<String, BenchmarkVariableMeta> BENCHMARK_METADATA = new LinkedHashMap<>();

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

                        String name = entry.getValue().path("name").asText(varKey);
                        String unit = entry.getValue().path("unit").asText("");
                        String window = entry.getValue().path("window").asText("");
                        String desc = entry.getValue().path("description").asText("");
                        String source = entry.getValue().path("source").asText("");
                        int minYear = timeSeries.isEmpty() ? 0 : ((TreeMap<Integer, Double>) timeSeries).firstKey();
                        int maxYear = timeSeries.isEmpty() ? 2026 : ((TreeMap<Integer, Double>) timeSeries).lastKey();
                        BENCHMARK_METADATA.put(varKey, new BenchmarkVariableMeta(varKey, name, unit, window, desc, source, minYear, maxYear));
                    }
                    logger.info("✅ Successfully loaded Historical Benchmarks ({} variables) from JSON resource.", BENCHMARK_DATASETS.size());
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

        /**
         * Get summary.
         *
         * @return the resulting computation or state reference
         */
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

        /**
         * Record value.
         *
         * @param metricKey the metric key parameter (String)
         * @param year the year parameter (int)
         * @param value the value parameter (double)
         */
        public void recordValue(String metricKey, int year, double value) {
            seriesMap.computeIfAbsent(metricKey, k -> new TreeMap<>()).put(year, value);
        }

        /**
         * Get series.
         *
         * @param metricKey the metric key parameter (String)
         * @return the resulting computation or state reference
         */
        public Map<Integer, Double> getSeries(String metricKey) {
            return seriesMap.getOrDefault(metricKey, Collections.emptyMap());
        }
    }

    /**
     * Filter by window.
     *
     * @param dataset the dataset parameter (Double&gt;)
     * @param window the window parameter (EpochWindow)
     * @return the resulting computation or state reference
     */
    public static Map<Integer, Double> filterByWindow(Map<Integer, Double> dataset, EpochWindow window) {
        if (dataset == null) return Collections.emptyMap();
        return dataset.entrySet().stream()
            .filter(e -> window.containsYear(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, TreeMap::new));
    }

    /**
     * Calculate rmse.
     *
     * @param simulatedData the simulated data parameter (Double&gt;)
     * @return the resulting computation or state reference
     */
    public static double calculateRmse(Map<Integer, Double> simulatedData) {
        // Benchmark evaluation: Record metric snapshot and calculate residual variance
        // Compare simulated trajectories against empirical historical ground truth
        return calculateRmse(simulatedData, getHistoricalWorldPopulation());
    }

    /**
     * Calculate rsquared.
     *
     * @param simulatedData the simulated data parameter (Double&gt;)
     * @return the resulting computation or state reference
     */
    public static double calculateRSquared(Map<Integer, Double> simulatedData) {
        // Benchmark evaluation: Record metric snapshot and calculate residual variance
        // Compare simulated trajectories against empirical historical ground truth
        return calculateRSquared(simulatedData, getHistoricalWorldPopulation());
    }

    /**
     * Calculate rmse.
     *
     * @param simulatedData the simulated data parameter (Double&gt;)
     * @param benchmarkData the benchmark data parameter (Double&gt;)
     * @return the resulting computation or state reference
     */
    public static double calculateRmse(Map<Integer, Double> simulatedData, Map<Integer, Double> benchmarkData) {
        // Benchmark evaluation: Record metric snapshot and calculate residual variance
        // Compare simulated trajectories against empirical historical ground truth
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

    /**
     * Calculate rsquared.
     *
     * @param simulatedData the simulated data parameter (Double&gt;)
     * @param benchmarkData the benchmark data parameter (Double&gt;)
     * @return the resulting computation or state reference
     */
    public static double calculateRSquared(Map<Integer, Double> simulatedData, Map<Integer, Double> benchmarkData) {
        // Benchmark evaluation: Record metric snapshot and calculate residual variance
        // Compare simulated trajectories against empirical historical ground truth
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

    /**
     * Calculates the Pearson correlation coefficient \(r \in [-1.0, 1.0]\) between simulated trajectories
     * and empirical benchmark ground truth over intersecting time points.
     *
     * @param simulatedData simulated trajectory time series
     * @param benchmarkData empirical benchmark dataset
     * @return Pearson correlation coefficient r
     */
    public static double calculatePearsonCorrelation(Map<Integer, Double> simulatedData, Map<Integer, Double> benchmarkData) {
        if (simulatedData == null || benchmarkData == null) return 0.0;
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : benchmarkData.entrySet()) {
            int year = entry.getKey();
            if (simulatedData.containsKey(year)) {
                x.add(simulatedData.get(year));
                y.add(entry.getValue());
            }
        }
        int n = x.size();
        if (n < 2) return 0.0;
        double meanX = x.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double meanY = y.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double num = 0.0, denX = 0.0, denY = 0.0;
        for (int i = 0; i < n; i++) {
            double dx = x.get(i) - meanX;
            double dy = y.get(i) - meanY;
            num += dx * dy;
            denX += dx * dx;
            denY += dy * dy;
        }
        if (denX <= 0.0 || denY <= 0.0) return 0.0;
        return num / Math.sqrt(denX * denY);
    }

    /**
     * Evaluate windowed fit.
     *
     * @param trajectory the trajectory parameter (MultiMetricTrajectory)
     * @param window the window parameter (EpochWindow)
     * @return the resulting computation or state reference
     */
    public static MultiMetricValidationReport evaluateWindowedFit(MultiMetricTrajectory trajectory, EpochWindow window) {
        // Benchmark evaluation: Record metric snapshot and calculate residual variance
        // Compare simulated trajectories against empirical historical ground truth
        MultiMetricValidationReport report = new MultiMetricValidationReport();
        report.window = window;
        if (trajectory == null) return report;

        for (Map.Entry<String, Map<Integer, Double>> benchmarkEntry : BENCHMARK_DATASETS.entrySet()) {
            String metricKey = benchmarkEntry.getKey();
            Map<Integer, Double> rawBenchmark = benchmarkEntry.getValue();

            Map<Integer, Double> simulated = trajectory.getSeries(metricKey);

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

    /**
     * Resolves metric key or UI alias into standard canonical benchmark dataset key.
     *
     * @param key variable or alias key
     * @return canonical key, or original key if already canonical
     */
    public static String resolveCanonicalKey(String key) {
        if (key == null) return null;
        if (BENCHMARK_DATASETS.containsKey(key)) return key;
        String lower = key.toLowerCase();
        if (lower.contains("populat")) return "worldPopulation";
        if (lower.contains("gdp") || lower.contains("product") || lower.contains("richesse")) return "grossWorldProduct";
        if (lower.contains("energy") || lower.contains("energie") || lower.contains("puissance")) return "primaryEnergy";
        if (lower.contains("urban")) return "urbanizationRate";
        if (lower.contains("co2") || lower.contains("carbon")) return "co2Concentration";
        if (lower.contains("literacy") || lower.contains("alphabet")) return "literacyRate";
        if (lower.contains("currency") || lower.contains("debase") || lower.contains("monnaie")) return "currencyDebasement";
        if (lower.contains("elite")) return "eliteOverproductionIndex";
        if (lower.contains("wage") || lower.contains("salaire")) return "realUnskilledWage";
        if (lower.contains("stress") || lower.contains("psi")) return "politicalStressIndex";
        if (lower.contains("asabiyyah") || lower.contains("cohesion")) return "asabiyyahSocialCohesion";
        if (lower.contains("instabil") || lower.contains("conflict") || lower.contains("conflit")) return "sociopoliticalInstability";
        if (lower.contains("gini") || lower.contains("inegal")) return "giniInequality";
        if (lower.contains("debt") || lower.contains("dette")) return "sovereignDebtBurden";
        if (lower.contains("trade") || lower.contains("commerce")) return "globalTradeVolume";
        if (lower.contains("temp")) return "temperatureAnomaly";
        if (lower.contains("erosion")) return "soilErosionRate";
        if (lower.contains("eroei") || lower.contains("eroi") || lower.contains("food")) return "agriculturalEroei";
        if (lower.contains("forest") || lower.contains("deforest")) return "deforestationRate";
        if (lower.contains("speed") || lower.contains("vitesse")) return "informationSpeed";
        if (lower.contains("megafauna")) return "megafaunaIndex";
        if (lower.contains("milankovitch") || lower.contains("insolation")) return "milankovitchInsolation";
        if (lower.contains("containment") || lower.contains("isole")) return "zeroContainmentScore";
        if (lower.contains("life") || lower.contains("esperance") || lower.contains("vie")) return "lifeExpectancyAtBirth";
        if (lower.contains("metal") || lower.contains("smelt")) return "metalSmeltingPerCapita";
        if (lower.contains("fossil")) return "fossilSharePrimaryEnergy";
        if (lower.contains("freight") || lower.contains("shipping")) return "shippingFreightCostIndex";
        return key;
    }

    /**
     * Gets raw benchmark time-series dataset for a metric key (or resolved alias).
     *
     * @param key the metric key parameter (String)
     * @return the benchmark dataset map (Year -> Value)
     */
    public static Map<Integer, Double> getBenchmarkDataset(String key) {
        String canon = resolveCanonicalKey(key);
        return BENCHMARK_DATASETS.getOrDefault(canon != null ? canon : key, Collections.emptyMap());
    }

    /**
     * Gets all canonical benchmark dataset keys.
     *
     * @return set of 27 benchmark variable keys
     */
    public static Set<String> getAllBenchmarkKeys() {
        return Collections.unmodifiableSet(BENCHMARK_DATASETS.keySet());
    }

    /**
     * Gets complete metadata map for all 27 benchmark variables.
     *
     * @return unmodifiable map of metadata objects
     */
    public static Map<String, BenchmarkVariableMeta> getAllBenchmarkMetadata() {
        return Collections.unmodifiableMap(BENCHMARK_METADATA);
    }

    /**
     * Gets metadata for a specific benchmark key or alias.
     *
     * @param key variable key or alias
     * @return metadata object or null
     */
    public static BenchmarkVariableMeta getBenchmarkMetadata(String key) {
        String canon = resolveCanonicalKey(key);
        return BENCHMARK_METADATA.get(canon != null ? canon : key);
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
     *
     * @param key variable key
     * @param year chronological year (negative for BCE/BP)
     * @return interpolated value, or Double.NaN if outside the valid historical epoch domain
     */
    public static double getInterpolatedBenchmarkValue(String key, double year) {
        return getInterpolatedBenchmarkValue(key, year, InterpolationMethod.PCHIP_MONOTONE_CUBIC);
    }

    /**
     * Retrieves interpolated benchmark value using specified mathematical interpolation method.
     * Returns Double.NaN when queried before the earliest historical observation point
     * (e.g. GDP before 1 CE, Roman silver debasement before -200 BCE, sovereign debt before 1500 CE).
     *
     * @param key the key parameter (String)
     * @param year the year parameter (double)
     * @param method the method parameter (InterpolationMethod)
     * @return the resulting computation or Double.NaN if not historically applicable
     */
    public static double getInterpolatedBenchmarkValue(String key, double year, InterpolationMethod method) {
        Map<Integer, Double> dataset = getBenchmarkDataset(key);
        if (dataset == null || dataset.isEmpty()) return Double.NaN;

        List<Integer> years = new ArrayList<>(dataset.keySet());
        Collections.sort(years);

        int n = years.size();
        if (n == 0) return Double.NaN;
        if (n == 1) return dataset.get(years.get(0));

        int firstYear = years.get(0);
        int lastYear = years.get(n - 1);

        // Outside historical definition range: deep time pre-civilizational invariants (-3,000,000 BP to -10,000 BP)
        if (year < firstYear) {
            switch (key) {
                // Anthropogenic civilizational & institutional variables strictly 0.0 before historical emergence
                case "literacyRate", "urbanizationRate", "currencyDebasement", "eliteOverproductionIndex",
                     "sovereignDebtBurden", "fossilSharePrimaryEnergy", "metalSmeltingPerCapita",
                     "deforestationRate", "politicalStressIndex", "agriculturalEroei", "globalTradeVolume" -> {
                    return 0.0;
                }
                // Ecological & evolutionary deep-time physical invariants
                case "megafaunaIndex" -> {
                    return 100.0; // Pre-Quaternary megafauna baseline
                }
                case "zeroContainmentScore" -> {
                    return 1.0; // Uncontained open biosphere
                }
                case "soilErosionRate" -> {
                    return 0.03; // Geomorphic natural background weathering (Montgomery 2007 PNAS)
                }
                case "giniInequality" -> {
                    return 0.17; // Egalitarian nomadic hunter-gatherer band baseline (Kohler et al. 2017 Nature)
                }
                case "asabiyyahSocialCohesion" -> {
                    return 0.95; // Foraging band in-group solidarity (Turchin 2003)
                }
                case "informationSpeed" -> {
                    return 4.0; // Pedestrian human walking speed (4 km/h)
                }
                case "sociopoliticalInstability" -> {
                    return 5.0; // Inter-band skirmish baseline without institutional state collapse
                }
                case "lifeExpectancyAtBirth" -> {
                    return 24.0; // Pleistocene hominin life expectancy at birth (Gurven & Kaplan 2007)
                }
                default -> {
                    return Double.NaN;
                }
            }
        }
        if (year > lastYear) {
            return dataset.get(lastYear);
        }

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

    /**
     * Get historical world population.
     *
     * @return the resulting computation or state reference
     */
    public static Map<Integer, Double> getHistoricalWorldPopulation() { return getBenchmarkDataset("worldPopulation"); }
    /**
     * Get historical world gdp.
     *
     * @return the resulting computation or state reference
     */
    public static Map<Integer, Double> getHistoricalWorldGdp() { return getBenchmarkDataset("grossWorldProduct"); }
    /**
     * Get historical world energy.
     *
     * @return the resulting computation or state reference
     */
    public static Map<Integer, Double> getHistoricalWorldEnergy() { return getBenchmarkDataset("primaryEnergy"); }
    /**
     * Get historical urbanization rate.
     *
     * @return the resulting computation or state reference
     */
    public static Map<Integer, Double> getHistoricalUrbanizationRate() { return getBenchmarkDataset("urbanizationRate"); }
    /**
     * Get historical co2concentration.
     *
     * @return the resulting computation or state reference
     */
    public static Map<Integer, Double> getHistoricalCo2Concentration() { return getBenchmarkDataset("co2Concentration"); }
    /**
     * Get historical literacy rate.
     *
     * @return the resulting computation or state reference
     */
    public static Map<Integer, Double> getHistoricalLiteracyRate() { return getBenchmarkDataset("literacyRate"); }
    /**
     * Get historical currency debasement.
     *
     * @return the resulting computation or state reference
     */
    public static Map<Integer, Double> getHistoricalCurrencyDebasement() { return getBenchmarkDataset("currencyDebasement"); }
}

