/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Historical Telemetry Validation & Multi-Metric Empirical Benchmark Kernel.
 *
 * <p>Loads and validates socio-economic, cliodynamic, ecological, and technological variables
 * from standardized benchmark datasets ({@code historical_cliodynamic_benchmarks.json}) and
 * prospective 1D trajectories via {@link HistoricalBenchmarkDatasetRepository}, evaluating
 * goodness-of-fit statistics via {@link BenchmarkStatisticalEvaluator}.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see HistoricalBenchmarkDatasetRepository
 * @see BenchmarkInterpolator
 * @see BenchmarkStatisticalEvaluator
 */
public class HistoricalValidationKernel {

    /** Final calendar year of observational HYDE 3.4 / Maddison / ERA5 historical datasets. */
    public static final int HYDE_MAX_OBSERVATION_YEAR = 2025;

    /** Earliest Pleistocene hominin baseline year (-3,000,000 BP). */
    public static final int PALEO_MIN_OBSERVATION_YEAR = -3000000;

    /** Upper boundary of deep Paleolithic pre-agricultural baseline (-10,000 BP). */
    public static final int PALEO_MAX_OBSERVATION_YEAR = -10000;

    /** Standard CMIP6 / SSP prospective horizon year (2100 CE). */
    public static final int PROJECTION_MAX_YEAR = 2100;

    /**
     * Resolves the canonical reference model key for a given start year.
     *
     * @param startYear the epoch start year
     * @return {@code "PALEO_SOURCES"} if before -10000 BP, {@code "HYDE_MADDISON"} if between -10000 BP and 2024 CE, or {@code "SSP2"} for future
     */
    public static String resolveDefaultReferenceModel(int startYear) {
        if (startYear < -10000) return "PALEO_SOURCES";
        if (startYear < 2025) return "HYDE_MADDISON";
        return "SSP2";
    }

    /**
     * Resolves human-readable display label for reference models.
     *
     * @param modelKey model key
     * @return human-readable description
     */
    public static String getReferenceModelDisplayLabel(String modelKey) {
        if (modelKey == null) return "HYDE 3.4 & Maddison Project";
        return switch (modelKey.toUpperCase(Locale.ROOT)) {
            case "PALEO_SOURCES" -> "Paléo Sources (EPICA Dome C, PaleoClim v1.2, Biraben)";
            case "HYDE_MADDISON" -> "HYDE 3.4 & Maddison Project (10k BCE - 2024 CE)";
            case "SSP1", "PREDICTIVE_SSP1" -> "SSP1 - Durabilité & Décarbonation Rapide (IIASA/IEA)";
            case "SSP2", "PREDICTIVE_SSP2" -> "SSP2 - Tendances Actuelles / Milieu de la Route (UN WPP / IIASA)";
            case "SSP3", "PREDICTIVE_SSP3_WORLD3" -> "SSP3 - Rivalités Régionales & Limites Ressources (Club de Rome)";
            case "SSP4", "PREDICTIVE_SSP4" -> "SSP4 - Inégalités & Stratification Sociale (GCAM4)";
            case "SSP5", "PREDICTIVE_SSP5" -> "SSP5 - Énergies Fossiles & Croissance Forcée (PIK/IIASA)";
            case "ETHER_CALIBRATED" -> "Ether Biophysique Calibré (ERA5 / Continuité Endogène 2025-2200)";
            default -> modelKey;
        };
    }

    /**
     * Canonical macro-historical epoch windows for windowed goodness-of-fit evaluation.
     */
    public enum EpochWindow {
        PALEOLITHIC_PLEISTOCENE(-3000000, -10000, "Deep Paleolithic & Pleistocene Era (-3 Ma - 10k BCE)"),
        DEEP_HORIZON(-10000, 2025, "Deep Macro-Historical Horizon (-10k BCE - 2025 CE)"),
        CLASSICAL_MEDIEVAL(-3000, 1500, "Classical Antiquity & Medieval Era (-3000 BCE - 1500 CE)"),
        EARLY_MODERN_500YR(1500, 2025, "Early Modern & Industrial Era (1500 CE - 2025 CE, 500-Year Window)"),
        MODERN_INDUSTRIAL(1900, 2025, "Modern Industrial & Digital Era (1900 CE - 2025 CE, 125-Year Window)"),
        FUTURE_PROSPECTIVE_2100(2025, 2100, "Future Prospective CMIP6/SSP Scenarios (2025 CE - 2100 CE)"),
        FUTURE_PROSPECTIVE_2200(2025, 2200, "Long-term Anthropocene Prospective (2025 CE - 2200 CE)");

        public final int startYear;
        public final int endYear;
        public final String description;

        EpochWindow(int start, int end, String desc) {
            this.startYear = start;
            this.endYear = end;
            this.description = desc;
        }

        /**
         * Checks whether the given calendar year lies within this epoch window.
         *
         * @param yr chronological year
         * @return {@code true} if {@code startYear <= yr <= endYear}
         */
        public boolean containsYear(int yr) {
            return yr >= startYear && yr <= endYear;
        }
    }

    /**
     * Metadata descriptor for a benchmark state variable.
     */
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

    /**
     * Goodness-of-fit metrics for a single benchmark variable.
     */
    public record MetricFit(String metricName, double rmse, double rSquared, int samplePoints) {}

    /**
     * Composite validation report across multiple benchmark metrics within an {@link EpochWindow}.
     */
    public static class MultiMetricValidationReport {
        public EpochWindow window;
        public Map<String, MetricFit> metricFits = new HashMap<>();
        public double compositeRSquared;
        public double compositeRmse;

        /**
         * Formats a human-readable summary of all metric fits and composite \(R^2\) / RMSE.
         *
         * @return multi-line diagnostic summary string
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder(String.format("Multi-Metric Report [%s]:\n",
                    window != null ? window.description : "Global"));
            for (MetricFit fit : metricFits.values()) {
                sb.append(String.format("  - %s (%d points): R^2 = %.4f, RMSE = %.2f\n",
                        fit.metricName, fit.samplePoints, fit.rSquared, fit.rmse));
            }
            sb.append(String.format("  => Composite R^2 = %.4f, Composite RMSE = %.2f",
                    compositeRSquared, compositeRmse));
            return sb.toString();
        }
    }

    /**
     * Multi-Variable Simulation Trajectory Container.
     */
    public static class MultiMetricTrajectory {
        public Map<String, Map<Integer, Double>> seriesMap = new HashMap<>();

        /**
         * Appends a scalar observation for a metric at a given calendar year.
         *
         * @param metricKey canonical metric key
         * @param year      chronological year
         * @param value     simulated value
         */
        public void recordValue(String metricKey, int year, double value) {
            seriesMap.computeIfAbsent(metricKey, k -> new TreeMap<>()).put(year, value);
        }

        /**
         * Returns the recorded time series for a metric key.
         *
         * @param metricKey canonical metric key
         * @return map of {@code year -> value}, or empty map if absent
         */
        public Map<Integer, Double> getSeries(String metricKey) {
            return seriesMap.getOrDefault(metricKey, Collections.emptyMap());
        }
    }

    /**
     * Mathematical interpolation schemes for continuous 1D benchmark trajectories.
     */
    public enum InterpolationMethod {
        PCHIP_MONOTONE_CUBIC,
        CATMULL_ROM_SPLINE,
        LINEAR
    }

    /**
     * Filters a time-series dataset to entries falling within the specified {@link EpochWindow}.
     *
     * @param dataset input time series ({@code year -> value})
     * @param window  target epoch window
     * @return filtered {@link TreeMap}
     */
    public static Map<Integer, Double> filterByWindow(Map<Integer, Double> dataset, EpochWindow window) {
        if (dataset == null) return Collections.emptyMap();
        return dataset.entrySet().stream()
                .filter(e -> window.containsYear(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, TreeMap::new));
    }

    /**
     * Calculates Root Mean Square Error (RMSE) against empirical world population ground truth.
     *
     * @param simulatedData simulated world population trajectory
     * @return RMSE statistic
     */
    public static double calculateRmse(Map<Integer, Double> simulatedData) {
        return calculateRmse(simulatedData, getHistoricalWorldPopulation());
    }

    /**
     * Calculates coefficient of determination (\(R^2\)) against empirical world population ground truth.
     *
     * @param simulatedData simulated world population trajectory
     * @return \(R^2\) statistic
     */
    public static double calculateRSquared(Map<Integer, Double> simulatedData) {
        return calculateRSquared(simulatedData, getHistoricalWorldPopulation());
    }

    /**
     * Calculates Root Mean Square Error (RMSE) between simulated and benchmark series.
     *
     * @param simulatedData simulated trajectory
     * @param benchmarkData empirical benchmark series
     * @return RMSE statistic
     */
    public static double calculateRmse(Map<Integer, Double> simulatedData, Map<Integer, Double> benchmarkData) {
        return BenchmarkStatisticalEvaluator.calculateRmse(simulatedData, benchmarkData);
    }

    /**
     * Calculates coefficient of determination (\(R^2\)) between simulated and benchmark series.
     *
     * @param simulatedData simulated trajectory
     * @param benchmarkData empirical benchmark series
     * @return \(R^2\) statistic
     */
    public static double calculateRSquared(Map<Integer, Double> simulatedData, Map<Integer, Double> benchmarkData) {
        return BenchmarkStatisticalEvaluator.calculateRSquared(simulatedData, benchmarkData);
    }

    /**
     * Calculates the Pearson correlation coefficient \(r \in [-1.0, 1.0]\) between simulated trajectories
     * and empirical benchmark ground truth over intersecting time points.
     *
     * @param simulatedData simulated trajectory time series
     * @param benchmarkData empirical benchmark dataset
     * @return Pearson correlation coefficient \(r\)
     */
    public static double calculatePearsonCorrelation(Map<Integer, Double> simulatedData, Map<Integer, Double> benchmarkData) {
        return BenchmarkStatisticalEvaluator.calculatePearsonCorrelation(simulatedData, benchmarkData);
    }

    /**
     * Evaluates multi-metric goodness-of-fit restricted to a target {@link EpochWindow}.
     *
     * @param trajectory multi-metric simulation trajectory
     * @param window     target epoch window
     * @return populated {@link MultiMetricValidationReport}
     */
    public static MultiMetricValidationReport evaluateWindowedFit(MultiMetricTrajectory trajectory, EpochWindow window) {
        MultiMetricValidationReport report = new MultiMetricValidationReport();
        report.window = window;
        if (trajectory == null) return report;

        for (Map.Entry<String, Map<Integer, Double>> benchmarkEntry :
                HistoricalBenchmarkDatasetRepository.getAllBenchmarkDatasets().entrySet()) {
            String metricKey = benchmarkEntry.getKey();
            Map<Integer, Double> simulated = trajectory.getSeries(metricKey);
            Map<Integer, Double> windowedBenchmark = filterByWindow(benchmarkEntry.getValue(), window);
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
        return HistoricalBenchmarkDatasetRepository.resolveCanonicalKey(key);
    }

    /**
     * Gets raw benchmark time-series dataset for a metric key (or resolved alias).
     *
     * @param key metric key or alias
     * @return benchmark dataset map ({@code Year -> Value})
     */
    public static Map<Integer, Double> getBenchmarkDataset(String key) {
        return HistoricalBenchmarkDatasetRepository.getBenchmarkDataset(key);
    }

    /**
     * Gets all canonical benchmark dataset keys.
     *
     * @return unmodifiable set of benchmark variable keys
     */
    public static Set<String> getAllBenchmarkKeys() {
        return HistoricalBenchmarkDatasetRepository.getAllBenchmarkKeys();
    }

    /**
     * Gets complete metadata map for all benchmark variables.
     *
     * @return unmodifiable map of metadata objects
     */
    public static Map<String, BenchmarkVariableMeta> getAllBenchmarkMetadata() {
        return HistoricalBenchmarkDatasetRepository.getAllBenchmarkMetadata();
    }

    /**
     * Gets metadata for a specific benchmark key or alias.
     *
     * @param key variable key or alias
     * @return metadata object or {@code null}
     */
    public static BenchmarkVariableMeta getBenchmarkMetadata(String key) {
        return HistoricalBenchmarkDatasetRepository.getBenchmarkMetadata(key);
    }

    /**
     * Retrieves the interpolated predictive benchmark value for a specific predictive model suite
     * across the 2026–2100 horizon.
     *
     * @param modelId predictive model identifier
     * @param key     metric variable key
     * @param year    projection year
     * @param method  interpolation method
     * @return interpolated value, connecting smoothly to historical ground truth for years \(\le 2026\)
     */
    public static double getPredictiveBenchmarkValue(String modelId, String key, double year, InterpolationMethod method) {
        return HistoricalBenchmarkDatasetRepository.getPredictiveBenchmarkValue(modelId, key, year, method);
    }

    /**
     * Retrieves the \(C^1\) continuous interpolated historical benchmark value using PCHIP by default.
     *
     * @param key  variable key
     * @param year chronological year (negative for BCE/BP)
     * @return interpolated value, or {@code Double.NaN} if outside the valid historical domain
     */
    public static double getInterpolatedBenchmarkValue(String key, double year) {
        return getInterpolatedBenchmarkValue(key, year, InterpolationMethod.PCHIP_MONOTONE_CUBIC);
    }

    /**
     * Retrieves interpolated benchmark value using the specified mathematical interpolation method.
     *
     * @param key    variable key
     * @param year   chronological year
     * @param method interpolation method
     * @return interpolated value, deep-time invariant baseline, or {@code Double.NaN}
     */
    public static double getInterpolatedBenchmarkValue(String key, double year, InterpolationMethod method) {
        return HistoricalBenchmarkDatasetRepository.getInterpolatedBenchmarkValue(key, year, method);
    }

    /**
     * Computes the continuous, differentiable \(C^1\) temporal confidence tolerance envelope \(\tau(t)\).
     *
     * <p>Formula (AGENTS.md §11):
     * \[\tau(t) = \tau_{\text{mod}} + \frac{\tau_{\text{paleo}} - \tau_{\text{mod}}}{1 + \exp\left(\frac{t - t_{\text{trans}}}{s_{\text{trans}}}\right)}\]
     * with \(\tau_{\text{paleo}} = 0.50\) (50% relative uncertainty in prehistoric and ancient eras),
     * \(\tau_{\text{mod}} = 0.05\) (5% asymptotic relative uncertainty in the modern satellite and census era),
     * \(t_{\text{trans}} = 1200\text{ CE}\), and \(s_{\text{trans}} = 300\text{ years}\).</p>
     *
     * @param year calendar year (BCE negative, CE positive)
     * @return relative error tolerance fraction in [0.05, 0.50]
     */
    public static double computeContinuousTemporalTolerance(double year) {
        double tauPaleo = 0.50;
        double tauMod = 0.05;
        double tTrans = 1200.0;
        double sTrans = 300.0;
        return tauMod + (tauPaleo - tauMod) / (1.0 + Math.exp((year - tTrans) / sTrans));
    }

    /** @return empirical historical world population dataset (millions) */
    public static Map<Integer, Double> getHistoricalWorldPopulation() { return getBenchmarkDataset("worldPopulation"); }

    /** @return empirical historical gross world product dataset (Billion 1990 Int$) */
    public static Map<Integer, Double> getHistoricalWorldGdp() { return getBenchmarkDataset("grossWorldProduct"); }

    /** @return empirical historical primary energy consumption dataset (EJ/yr) */
    public static Map<Integer, Double> getHistoricalWorldEnergy() { return getBenchmarkDataset("primaryEnergy"); }

    /** @return empirical historical urbanization rate dataset (%) */
    public static Map<Integer, Double> getHistoricalUrbanizationRate() { return getBenchmarkDataset("urbanizationRate"); }

    /** @return empirical historical atmospheric \(\text{CO}_2\) concentration dataset (ppm) */
    public static Map<Integer, Double> getHistoricalCo2Concentration() { return getBenchmarkDataset("co2Concentration"); }

    /** @return empirical historical global literacy rate dataset (%) */
    public static Map<Integer, Double> getHistoricalLiteracyRate() { return getBenchmarkDataset("literacyRate"); }

    /** @return empirical historical coinage precious-metal purity / currency debasement dataset (%) */
    public static Map<Integer, Double> getHistoricalCurrencyDebasement() { return getBenchmarkDataset("currencyDebasement"); }
}
