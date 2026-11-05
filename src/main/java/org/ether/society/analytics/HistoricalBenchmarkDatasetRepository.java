/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ether.society.analytics.HistoricalValidationKernel.BenchmarkVariableMeta;
import org.ether.society.analytics.HistoricalValidationKernel.InterpolationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Empirical Benchmark Dataset Repository & Deep-Time Boundary Interpolator
 * for {@link HistoricalValidationKernel}.
 *
 * <p>Loads standardized historical benchmark series from {@code historical_cliodynamic_benchmarks.json}
 * and prospective projection pathways (SSP1–SSP5, World3, {@code ETHER_CALIBRATED}) from planetary
 * variable catalogs ({@code data/ether/earth/variables/projections/}). Enforces deep-time
 * Paleolithic boundary invariants (\(-3\,000\,000\text{ BP}\) to \(-10\,000\text{ BP}\)) and
 * Zero Double Transition continuity across the 2025 CE observational boundary.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see HistoricalValidationKernel
 * @see BenchmarkInterpolator
 */
final class HistoricalBenchmarkDatasetRepository {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalBenchmarkDatasetRepository.class);

    /** Multi-Metric Historical Benchmark Time Series (Metric Key -> Year -> Value). */
    private static final Map<String, Map<Integer, Double>> BENCHMARK_DATASETS = new HashMap<>();

    /** Metadata catalog for all canonical benchmark variables. */
    private static final Map<String, BenchmarkVariableMeta> BENCHMARK_METADATA = new LinkedHashMap<>();

    /** Prospective Multi-Model Benchmark Time Series (Model ID -> Metric Key -> Year -> Value). */
    private static final Map<String, Map<String, Map<Integer, Double>>> PREDICTIVE_BENCHMARK_DATASETS = new ConcurrentHashMap<>();

    static {
        loadJsonBenchmarksResource();
        loadPredictiveBenchmarksResource();
    }

    private HistoricalBenchmarkDatasetRepository() {
        // Static repository delegate
    }

    /**
     * Loads prospective SSP1–SSP5 and Ether Calibrated 1D projection trajectories from disk.
     */
    private static void loadPredictiveBenchmarksResource() {
        File predFile = org.ether.society.config.EtherPaths.getEarthVariablesProjectionsDir().resolve("predictive_socioeconomic_projections_2026_2100.json").toFile();
        if (!predFile.exists()) {
            predFile = new File("data/maps/ether/earth/variables/projections/predictive_socioeconomic_projections_2026_2100.json");
        }
        if (!predFile.exists()) {
            predFile = new File("data/variables/projections/predictive_socioeconomic_projections_2026_2100.json");
        }
        if (!predFile.exists()) {
            predFile = new File("data/sources/predictive/predictive_socioeconomic_projections_2026_2100.json");
        }
        if (!predFile.exists()) return;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(predFile);
            JsonNode datasetsNode = root.get("datasets");
            if (datasetsNode != null && datasetsNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> models = datasetsNode.fields();
                while (models.hasNext()) {
                    Map.Entry<String, JsonNode> modelEntry = models.next();
                    String modelId = modelEntry.getKey();
                    Map<String, Map<Integer, Double>> metricMap = new ConcurrentHashMap<>();
                    Iterator<Map.Entry<String, JsonNode>> metrics = modelEntry.getValue().fields();
                    while (metrics.hasNext()) {
                        Map.Entry<String, JsonNode> metricEntry = metrics.next();
                        String metricKey = metricEntry.getKey();
                        Map<Integer, Double> ts = new TreeMap<>();
                        Iterator<Map.Entry<String, JsonNode>> pts = metricEntry.getValue().fields();
                        while (pts.hasNext()) {
                            Map.Entry<String, JsonNode> pt = pts.next();
                            ts.put(Integer.parseInt(pt.getKey()), pt.getValue().asDouble());
                        }
                        metricMap.put(metricKey, Collections.unmodifiableMap(ts));
                    }
                    PREDICTIVE_BENCHMARK_DATASETS.put(modelId, metricMap);
                    registerPredictiveModelAliases(modelId, metricMap);
                }
                loadEtherCalibratedProjection(mapper);
                logger.info("✅ Successfully loaded Predictive Projections ({} models) from data/variables/projections/",
                        PREDICTIVE_BENCHMARK_DATASETS.size());
            }
        } catch (Exception e) {
            logger.error("❌ Failed to parse predictive projections JSON", e);
        }
    }

    /**
     * Registers canonical SSP short-name aliases for predictive datasets.
     */
    private static void registerPredictiveModelAliases(String modelId, Map<String, Map<Integer, Double>> metricMap) {
        if ("PREDICTIVE_SSP1".equalsIgnoreCase(modelId)) PREDICTIVE_BENCHMARK_DATASETS.put("SSP1", metricMap);
        else if ("PREDICTIVE_SSP2".equalsIgnoreCase(modelId)) PREDICTIVE_BENCHMARK_DATASETS.put("SSP2", metricMap);
        else if ("PREDICTIVE_SSP3_WORLD3".equalsIgnoreCase(modelId)) {
            PREDICTIVE_BENCHMARK_DATASETS.put("SSP3", metricMap);
            PREDICTIVE_BENCHMARK_DATASETS.put("PREDICTIVE_SSP3", metricMap);
        } else if ("PREDICTIVE_SSP4".equalsIgnoreCase(modelId)) PREDICTIVE_BENCHMARK_DATASETS.put("SSP4", metricMap);
        else if ("PREDICTIVE_SSP5".equalsIgnoreCase(modelId)) PREDICTIVE_BENCHMARK_DATASETS.put("SSP5", metricMap);
    }

    /**
     * Loads the endogenous {@code ETHER_CALIBRATED} prospective 1D variable series if present.
     */
    private static void loadEtherCalibratedProjection(ObjectMapper mapper) throws Exception {
        File etherCalibFile = org.ether.society.config.EtherPaths.getEarthVariablesProjectionsDir().resolve("ether_calibrated_projection_1d.json").toFile();
        if (!etherCalibFile.exists()) {
            etherCalibFile = new File("data/maps/ether/earth/variables/projections/ether_calibrated_projection_1d.json");
        }
        if (!etherCalibFile.exists()) {
            etherCalibFile = new File("data/variables/projections/ether_calibrated_projection_1d.json");
        }
        if (etherCalibFile.exists()) {
            JsonNode calibRoot = mapper.readTree(etherCalibFile);
            JsonNode varsNode = calibRoot.get("variables");
            if (varsNode != null && varsNode.isObject()) {
                Map<String, Map<Integer, Double>> etherMetricMap = new ConcurrentHashMap<>();
                Iterator<Map.Entry<String, JsonNode>> vFields = varsNode.fields();
                while (vFields.hasNext()) {
                    Map.Entry<String, JsonNode> ve = vFields.next();
                    Map<Integer, Double> ts = new TreeMap<>();
                    Iterator<Map.Entry<String, JsonNode>> pts = ve.getValue().fields();
                    while (pts.hasNext()) {
                        Map.Entry<String, JsonNode> pt = pts.next();
                        try {
                            ts.put(Integer.parseInt(pt.getKey()), pt.getValue().asDouble());
                        } catch (NumberFormatException ignored) {
                            // Skip non-numeric metadata keys
                        }
                    }
                    etherMetricMap.put(ve.getKey(), Collections.unmodifiableMap(ts));
                }
                PREDICTIVE_BENCHMARK_DATASETS.put("ETHER_CALIBRATED", etherMetricMap);
            }
        }
    }

    /**
     * Loads historical empirical benchmark datasets from classpath JSON resource.
     */
    private static void loadJsonBenchmarksResource() {
        try (InputStream is = HistoricalValidationKernel.class.getClassLoader()
                .getResourceAsStream("historical_cliodynamic_benchmarks.json")) {
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

                        TreeMap<Integer, Double> timeSeries = new TreeMap<>();
                        if (dataNode != null && dataNode.isObject()) {
                            Iterator<Map.Entry<String, JsonNode>> points = dataNode.fields();
                            while (points.hasNext()) {
                                Map.Entry<String, JsonNode> point = points.next();
                                timeSeries.put(Integer.parseInt(point.getKey()), point.getValue().asDouble());
                            }
                        }
                        BENCHMARK_DATASETS.put(varKey, Collections.unmodifiableMap(timeSeries));

                        String name = entry.getValue().path("name").asText(varKey);
                        String unit = entry.getValue().path("unit").asText("");
                        String window = entry.getValue().path("window").asText("");
                        String desc = entry.getValue().path("description").asText("");
                        String source = entry.getValue().path("source").asText("");
                        int minYear = timeSeries.isEmpty() ? 0 : timeSeries.firstKey();
                        int maxYear = timeSeries.isEmpty() ? 2026 : timeSeries.lastKey();
                        BENCHMARK_METADATA.put(varKey, new BenchmarkVariableMeta(
                                varKey, name, unit, window, desc, source, minYear, maxYear));
                    }
                    logger.info("✅ Successfully loaded Historical Benchmarks ({} variables) from JSON resource.",
                            BENCHMARK_DATASETS.size());
                }
            } else {
                logger.warn("⚠️ historical_cliodynamic_benchmarks.json resource not found, initializing fallback datasets.");
            }
        } catch (Exception e) {
            logger.error("❌ Failed to parse historical_cliodynamic_benchmarks.json", e);
        }
    }

    /**
     * Resolves a metric key or UI alias into its canonical benchmark dataset key.
     *
     * @param key variable or alias key
     * @return canonical key, or original key if already canonical
     */
    static String resolveCanonicalKey(String key) {
        if (key == null) return null;
        if (BENCHMARK_DATASETS.containsKey(key)) return key;
        String lower = key.toLowerCase(Locale.ROOT);
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

    static Map<Integer, Double> getBenchmarkDataset(String key) {
        String canon = resolveCanonicalKey(key);
        return BENCHMARK_DATASETS.getOrDefault(canon != null ? canon : key, Collections.emptyMap());
    }

    static Map<String, Map<Integer, Double>> getAllBenchmarkDatasets() {
        return Collections.unmodifiableMap(BENCHMARK_DATASETS);
    }

    static Set<String> getAllBenchmarkKeys() {
        return Collections.unmodifiableSet(BENCHMARK_DATASETS.keySet());
    }

    static Map<String, BenchmarkVariableMeta> getAllBenchmarkMetadata() {
        return Collections.unmodifiableMap(BENCHMARK_METADATA);
    }

    static BenchmarkVariableMeta getBenchmarkMetadata(String key) {
        String canon = resolveCanonicalKey(key);
        return BENCHMARK_METADATA.get(canon != null ? canon : key);
    }

    /**
     * Evaluates prospective benchmark projections across 2026–2100, connecting smoothly to
     * historical ground truth for years \(\le 2026\).
     */
    static double getPredictiveBenchmarkValue(String modelId, String key, double year, InterpolationMethod method) {
        if (modelId != null) {
            String norm = modelId.toUpperCase(Locale.ROOT);
            if (!PREDICTIVE_BENCHMARK_DATASETS.containsKey(norm)) {
                norm = switch (norm) {
                    case "SSP1" -> "PREDICTIVE_SSP1";
                    case "SSP2" -> "PREDICTIVE_SSP2";
                    case "SSP3" -> "PREDICTIVE_SSP3_WORLD3";
                    case "SSP4" -> "PREDICTIVE_SSP4";
                    case "SSP5" -> "PREDICTIVE_SSP5";
                    default -> norm;
                };
            }
            if (PREDICTIVE_BENCHMARK_DATASETS.containsKey(norm)) {
                modelId = norm;
            }
        }
        if (modelId != null && PREDICTIVE_BENCHMARK_DATASETS.containsKey(modelId)) {
            Map<String, Map<Integer, Double>> modelData = PREDICTIVE_BENCHMARK_DATASETS.get(modelId);
            if (modelData != null && modelData.containsKey(key)) {
                Map<Integer, Double> dataset = modelData.get(key);
                List<Integer> years = new ArrayList<>(dataset.keySet());
                Collections.sort(years);
                if (!years.isEmpty()) {
                    if (year <= years.get(0)) {
                        return getInterpolatedBenchmarkValue(key, year, method);
                    }
                    if (year >= years.get(years.size() - 1)) {
                        return dataset.get(years.get(years.size() - 1));
                    }
                    return dispatchInterpolation(dataset, years, year, method);
                }
            }
        }
        return getInterpolatedBenchmarkValue(key, year, method);
    }

    /**
     * Evaluates historical benchmark value using PCHIP, Catmull-Rom, or linear interpolation,
     * strictly enforcing deep-time Paleolithic invariants prior to earliest empirical observations.
     */
    static double getInterpolatedBenchmarkValue(String key, double year, InterpolationMethod method) {
        Map<Integer, Double> dataset = getBenchmarkDataset(key);
        if (dataset == null || dataset.isEmpty()) return Double.NaN;

        List<Integer> years = new ArrayList<>(dataset.keySet());
        Collections.sort(years);

        int n = years.size();
        if (n == 0) return Double.NaN;
        if (n == 1) return dataset.get(years.get(0));

        int firstYear = years.get(0);
        int lastYear = years.get(n - 1);

        if (year < firstYear) {
            return resolveDeepTimeInvariant(key);
        }
        if (year > lastYear) {
            if (lastYear <= HistoricalValidationKernel.HYDE_MAX_OBSERVATION_YEAR
                    && year > HistoricalValidationKernel.HYDE_MAX_OBSERVATION_YEAR) {
                return Double.NaN;
            }
            return dataset.get(lastYear);
        }
        return dispatchInterpolation(dataset, years, year, method);
    }

    /**
     * Enforces deep-time Pleistocene / pre-civilizational invariant baselines (-3,000,000 BP to -10,000 BP).
     */
    private static double resolveDeepTimeInvariant(String key) {
        return switch (key) {
            case "literacyRate", "urbanizationRate", "currencyDebasement", "eliteOverproductionIndex",
                 "sovereignDebtBurden", "fossilSharePrimaryEnergy", "metalSmeltingPerCapita",
                 "politicalStressIndex", "agriculturalEroei", "globalTradeVolume" -> 0.0;
            case "deforestationRate" -> 99.0;       // Pristine post-glacial forest baseline (99% cover remaining)
            case "soilErosionRate" -> 0.03;         // Geomorphic natural weathering (Montgomery 2007 PNAS)
            case "megafaunaIndex" -> 100.0;         // Pre-Quaternary megafauna baseline
            case "zeroContainmentScore" -> 1.0;     // Uncontained open biosphere
            case "giniInequality" -> 0.17;          // Egalitarian nomadic band baseline (Kohler et al. 2017 Nature)
            case "asabiyyahSocialCohesion" -> 0.95; // Foraging band in-group solidarity (Turchin 2003)
            case "informationSpeed" -> 4.0;         // Pedestrian human walking speed (4 km/h)
            case "sociopoliticalInstability" -> 5.0;// Inter-band skirmish baseline without state collapse
            case "lifeExpectancyAtBirth" -> 24.0;   // Pleistocene hominin life expectancy (Gurven & Kaplan 2007)
            default -> Double.NaN;
        };
    }

    private static double dispatchInterpolation(Map<Integer, Double> dataset, List<Integer> years,
                                                double year, InterpolationMethod method) {
        if (method == InterpolationMethod.LINEAR) {
            return BenchmarkInterpolator.linearInterpolate(dataset, years, year);
        } else if (method == InterpolationMethod.CATMULL_ROM_SPLINE) {
            return BenchmarkInterpolator.catmullRomInterpolate(dataset, years, year);
        }
        return BenchmarkInterpolator.pchipInterpolate(dataset, years, year);
    }
}
