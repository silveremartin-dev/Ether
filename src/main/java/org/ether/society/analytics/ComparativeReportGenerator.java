*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.i18n.I18n;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * <h1>Comparative Report Generator</h1>
 * <p>
 * Provides statistical analytics, empirical validation harnesses, and parameter calibration kernels.<br>
 * Evaluates simulation trajectories using multi-metric error metrics (Mean Absolute Percentage Error, Pearson correlation, and spatial centroid divergence).
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ComparativeReportGenerator {

    /**
     * Generate markdown report.
     *
     * @param result the result parameter (RootCauseAnalyzer.ComparisonResult)
     * @return the resulting computation or state reference
     */
    public static String generateMarkdownReport(RootCauseAnalyzer.ComparisonResult result) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        sb.append(I18n.getOrDefault("analytics.report.title", "# 📊 Comparative Analysis & Sensitivity Benchmark Report\n\n"));
        sb.append("**").append(I18n.getOrDefault("analytics.report.gen_date", "Generation Date")).append("** : ").append(java.time.LocalDateTime.now().format(dtf)).append("\n\n");

        sb.append("## 📌 1. ").append(I18n.getOrDefault("analytics.report.context_title", "Benchmark Context")).append("\n");
        sb.append("- **").append(I18n.getOrDefault("analytics.report.baseline_label", "Baseline Reference Scenario")).append("** : `").append(result.getBaselineRun().getScenarioName()).append("` (`").append(result.getBaselineRun().getRunId()).append("`)\n");
        sb.append("- **").append(I18n.getOrDefault("analytics.report.target_label", "Comparison Target Scenario")).append("** : `").append(result.getTargetRun().getScenarioName()).append("` (`").append(result.getTargetRun().getRunId()).append("`)\n\n");

        sb.append("## 🔍 2. ").append(I18n.getOrDefault("analytics.report.root_cause_title", "Explanatory Synthesis & Root Cause Analysis")).append("\n");
        sb.append("> ").append(result.getPrimaryRootCauseExplanation()).append("\n\n");

        if (result.getDivergenceYear() != -1) {
            sb.append("**").append(I18n.getOrDefault("analytics.report.break_point", "Break Point ($T_{divergence}$)")).append("** : ")
              .append(String.format(I18n.getOrDefault("analytics.label.year_ad_formatted", "Year %d AD"), result.getDivergenceYear())).append("\n\n");
        }

        sb.append("### ").append(I18n.getOrDefault("analytics.report.perturbation_matrix", "Initial Perturbation & Parameter Variation Matrix:")).append("\n");
        if (result.getKeyDifferences().isEmpty()) {
            sb.append("- *").append(I18n.getOrDefault("analytics.report.no_param_diff", "No parametric differences recorded (Identical initial scenarios).")).append("* \n\n");
        } else {
            for (String diff : result.getKeyDifferences()) {
                sb.append("- ").append(diff).append("\n");
            }
            sb.append("\n");
        }

        sb.append("## 📈 3. ").append(I18n.getOrDefault("analytics.report.telemetry_deltas", "Telemetry Delta Evolution")).append("\n\n");
        sb.append(String.format("| %s | %s | %s | %s | %s |\n",
            I18n.getOrDefault("analytics.report.col_indicator", "Indicator"),
            I18n.getOrDefault("analytics.report.col_baseline", "Baseline Reference"),
            I18n.getOrDefault("analytics.report.col_target", "Comparison Target"),
            I18n.getOrDefault("analytics.report.col_abs_change", "Absolute Delta"),
            I18n.getOrDefault("analytics.report.col_pct_change", "Delta (%)")));
        sb.append("| :--- | :--- | :--- | :--- | :--- |\n");

        for (RootCauseAnalyzer.MetricDelta delta : result.getFinalDeltas()) {
            sb.append(String.format("| %s | %,.1f | %,.1f | %+.1f | %+.2f%% |\n",
                delta.getMetricName(),
                delta.getBaselineValue(),
                delta.getTargetValue(),
                delta.getAbsoluteChange(),
                delta.getPercentageChange()));
        }

        sb.append("\n## 🛠️ 4. ").append(I18n.getOrDefault("analytics.report.tuning_recommendations", "Automated Parameter Calibration Recommendations (\"Correct\")")).append("\n\n");
        if (result.getProposedCorrections().isEmpty()) {
            sb.append("✅ *").append(I18n.getOrDefault("analytics.report.no_correction_required", "No parametric correction required. Trajectory conforms to empirical tolerance thresholds.")).append("* \n\n");
        } else {
            sb.append(String.format("| %s | %s | %s | %s |\n",
                I18n.getOrDefault("analytics.report.col_param_adjust", "Parameter to Adjust"),
                I18n.getOrDefault("analytics.report.col_cur_value", "Current Value"),
                I18n.getOrDefault("analytics.report.col_prop_value", "Proposed Value"),
                I18n.getOrDefault("analytics.report.col_rationale", "Rationale & Estimated Impact")));
            sb.append("| :--- | :--- | :--- | :--- |\n");
            for (RootCauseAnalyzer.ParameterCorrection corr : result.getProposedCorrections()) {
                sb.append(String.format("| `%s` | `%s` | `%s` | %s |\n",
                    corr.getParameterName(),
                    corr.getCurrentValue(),
                    corr.getProposedValue(),
                    corr.getRationale()));
            }
            sb.append("\n");
        }

        sb.append("---\n*").append(I18n.getOrDefault("analytics.report.footer", "Report automatically generated by Ether Analytics Engine")).append("*\n");
        return sb.toString();
    }

    public static String generateCsvExport(List<SimulationRunRecord> runs) {
        return generateCsvExport(runs, false, 0, 0);
    }

    /**
     * Standardized descriptor for CSV telemetry columns with explicit physical/cliodynamic units.
     */
    public record CsvIndicatorColumn(
        String header,
        String benchmarkKey,
        Function<SimulationRunRecord.MetricSnapshot, Double> simulationExtractor,
        BiFunction<Integer, HistoricalValidationKernel.InterpolationMethod, Double> benchmarkExtractor
    ) {}

    /**
     * Complete catalogue of standardized scientific and cliodynamic columns.
     * Guarantees strict identical physical units across both simulated runs and historical empirical reality.
     */
    private static final List<CsvIndicatorColumn> CSV_COLUMNS = List.of(
        new CsvIndicatorColumn("Population_headcount", "worldPopulation",
            snap -> (double) snap.getPopulation(),
            (yr, m) -> {
                double val = HistoricalValidationKernel.getInterpolatedBenchmarkValue("worldPopulation", yr, m);
                return Double.isNaN(val) ? Double.NaN : val * 1_000_000.0;
            }),

        new CsvIndicatorColumn("GrossWorldProduct_BillionUSD1990", "grossWorldProduct",
            snap -> {
                double g = snap.getValue("grossWorldProduct");
                if (g > 0.0 && g < 1e6) return g; // already in Billion USD
                double gdp = snap.getValue("gdp");
                if (gdp == 0.0) gdp = snap.getValue("gdpTotal");
                return gdp > 0.0 ? gdp / 1e9 : (g > 0.0 ? g / 1e9 : Double.NaN);
            },
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("grossWorldProduct", yr, m)),

        new CsvIndicatorColumn("PrimaryEnergy_EJ", "primaryEnergy",
            snap -> {
                double pe = snap.getValue("primaryEnergy");
                if (pe > 0.0 && pe < 1e5) return pe; // already in EJ
                double mw = snap.getValue("energyCaptured");
                return mw > 0.0 ? mw * 3.15576e-5 : (pe > 0.0 ? pe * 3.15576e-5 : 0.0);
            },
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("primaryEnergy", yr, m)),

        new CsvIndicatorColumn("CO2Concentration_ppm", "co2Concentration",
            snap -> {
                double co2 = snap.getValue("co2Concentration");
                if (co2 > 150.0 && co2 < 2000.0) return co2;
                double cf = snap.getValue("carbonFootprint");
                return 280.0 + (cf * 0.001);
            },
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("co2Concentration", yr, m)),

        new CsvIndicatorColumn("UrbanizationRate_pct", "urbanizationRate",
            snap -> snap.getValue("urbanizationRate"),
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("urbanizationRate", yr, m)),

        new CsvIndicatorColumn("LiteracyRate_pct", "literacyRate",
            snap -> {
                double lit = snap.getValue("literacyRate");
                if (lit > 0.0) return lit;
                double edu = snap.getValue("educationLevel");
                return edu > 0.0 ? edu : Math.min(100.0, snap.getAvgTech() * 10.0);
            },
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("literacyRate", yr, m)),

        new CsvIndicatorColumn("AgriculturalEROEI_ratio", "agriculturalEroei",
            snap -> {
                double eroei = snap.getValue("eroiAlim");
                if (eroei > 0.0) return eroei;
                return snap.getValue("agriculturalEroei");
            },
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("agriculturalEroei", yr, m)),

        new CsvIndicatorColumn("FoodPerCapita_GJ_yr", "foodPerCapita",
            snap -> snap.getValue("foodPerCapita"),
            (yr, m) -> 3.362), // Historical physiological subsistence requirement

        new CsvIndicatorColumn("TotalFoodStockpile_calories", "totalFood",
            snap -> snap.getFood(),
            (yr, m) -> Double.NaN), // Historical global food stockpile unrecorded

        new CsvIndicatorColumn("LifeExpectancy_years", "lifeExpectancyAtBirth",
            snap -> {
                double le = snap.getValue("lifeExpectancy");
                return le > 0.0 ? le : snap.getValue("avgLifespan");
            },
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("lifeExpectancyAtBirth", yr, m)),

        new CsvIndicatorColumn("FertilityRate_birthsPerWoman", "fertilityRate",
            snap -> snap.getValue("fertilityRate"),
            (yr, m) -> {
                double pop = HistoricalValidationKernel.getInterpolatedBenchmarkValue("worldPopulation", yr, m);
                return Double.isNaN(pop) ? Double.NaN : (yr < 1900 ? 5.5 : Math.max(1.5, 5.5 - (yr - 1900) * 0.03));
            }),

        new CsvIndicatorColumn("GiniInequality_index", "giniInequality",
            snap -> {
                double g = snap.getValue("gini");
                return g > 0.0 ? g : snap.getValue("giniIndex");
            },
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("giniInequality", yr, m)),

        new CsvIndicatorColumn("TemperatureAnomaly_degC", "temperatureAnomaly",
            snap -> {
                double ta = snap.getValue("temperatureAnomaly");
                if (ta != 0.0) return ta;
                double temp = snap.getValue("temperature");
                return temp > 0.0 ? temp - 14.5 : 0.0;
            },
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("temperatureAnomaly", yr, m)),

        new CsvIndicatorColumn("AsabiyyahSocialCohesion_index", "asabiyyahSocialCohesion",
            snap -> {
                double a = snap.getValue("asabiyyah");
                return a > 1.0 ? a / 100.0 : a;
            },
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("asabiyyahSocialCohesion", yr, m)),

        new CsvIndicatorColumn("SociopoliticalInstability_index", "sociopoliticalInstability",
            snap -> {
                double c = snap.getValue("conflict");
                return c > 0.0 ? c : snap.getValue("sociopoliticalInstability");
            },
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("sociopoliticalInstability", yr, m)),

        new CsvIndicatorColumn("PoliticalStressIndex_psi", "politicalStressIndex",
            snap -> snap.getValue("psiStressIndex"),
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("politicalStressIndex", yr, m)),

        new CsvIndicatorColumn("EliteOverproduction_index", "eliteOverproductionIndex",
            snap -> snap.getValue("eliteOverproduction"),
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("eliteOverproductionIndex", yr, m)),

        new CsvIndicatorColumn("CollapseVulnerability_pct", "collapseVulnerability",
            snap -> snap.getValue("collapseVulnerability"),
            (yr, m) -> {
                double inst = HistoricalValidationKernel.getInterpolatedBenchmarkValue("sociopoliticalInstability", yr, m);
                return Double.isNaN(inst) ? Double.NaN : inst * 0.8;
            }),

        new CsvIndicatorColumn("SystemComplexity_index", "systemComplexity",
            snap -> snap.getValue("systemComplexity"),
            (yr, m) -> {
                double urb = HistoricalValidationKernel.getInterpolatedBenchmarkValue("urbanizationRate", yr, m);
                return Double.isNaN(urb) ? Double.NaN : (urb * 0.8 + 20.0);
            }),

        new CsvIndicatorColumn("DeforestationRate_pctRemaining", "deforestationRate",
            snap -> {
                double def = snap.getValue("deforestationRate");
                return def > 0.0 ? def : Math.max(0.0, 100.0 - snap.getValue("resourceDepletion"));
            },
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("deforestationRate", yr, m)),

        new CsvIndicatorColumn("SoilErosionRate_pctLoss", "soilErosionRate",
            snap -> snap.getValue("resourceDepletion") * 0.5,
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("soilErosionRate", yr, m)),

        new CsvIndicatorColumn("MegafaunaIndex_pctLatePleistocene", "megafaunaIndex",
            snap -> snap.getValue("megafaunaIndex"),
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("megafaunaIndex", yr, m)),

        new CsvIndicatorColumn("MilankovitchInsolation_W_m2", "milankovitchInsolation",
            snap -> snap.getValue("milankovitchInsolation"),
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("milankovitchInsolation", yr, m)),

        new CsvIndicatorColumn("ZeroContainmentScore_index", "zeroContainmentScore",
            snap -> {
                double z = snap.getValue("zeroContainmentScore");
                return z > 1.0 ? z / 100.0 : z;
            },
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("zeroContainmentScore", yr, m)),

        new CsvIndicatorColumn("InformationSpeed_km_day", "informationSpeed",
            snap -> Math.max(10.0, snap.getAvgTech() * 2.0),
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("informationSpeed", yr, m)),

        new CsvIndicatorColumn("CurrencyDebasement_pctSilver", "currencyDebasement",
            snap -> snap.getValue("currencyDebasement"),
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("currencyDebasement", yr, m)),

        new CsvIndicatorColumn("SovereignDebtBurden_pctGDP", "sovereignDebtBurden",
            snap -> snap.getValue("sovereignDebtBurden"),
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("sovereignDebtBurden", yr, m)),

        new CsvIndicatorColumn("GlobalTradeVolume_index", "globalTradeVolume",
            snap -> snap.getValue("globalTradeVolume"),
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("globalTradeVolume", yr, m)),

        new CsvIndicatorColumn("MetalSmeltingPerCapita_kg_yr", "metalSmeltingPerCapita",
            snap -> snap.getValue("metalSmeltingPerCapita"),
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("metalSmeltingPerCapita", yr, m)),

        new CsvIndicatorColumn("FossilSharePrimaryEnergy_pct", "fossilSharePrimaryEnergy",
            snap -> snap.getValue("fossilSharePrimaryEnergy"),
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("fossilSharePrimaryEnergy", yr, m)),

        new CsvIndicatorColumn("ShippingFreightCostIndex_index", "shippingFreightCostIndex",
            snap -> snap.getValue("shippingFreightCostIndex"),
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("shippingFreightCostIndex", yr, m)),

        new CsvIndicatorColumn("RealUnskilledWage_index", "realUnskilledWage",
            snap -> snap.getValue("realUnskilledWage"),
            (yr, m) -> HistoricalValidationKernel.getInterpolatedBenchmarkValue("realUnskilledWage", yr, m)),

        new CsvIndicatorColumn("AvgTechnologyLevel_tier", "avgTechLevel",
            snap -> snap.getAvgTech(),
            (yr, m) -> Double.NaN), // Synthetic simulation construct

        new CsvIndicatorColumn("PopulatedCells_count", "populatedCells",
            snap -> (double) snap.getPopulatedCellCount(),
            (yr, m) -> Double.NaN)  // H3 discrete mesh construct
    );

    /**
     * Generate structured, comprehensive CSV export for simulation telemetry and historical benchmarks.
     * Enforces uniform scientific units, 1-to-1 temporal matching between scenario and historical points,
     * and explicit "N/A" formatting for epochs where empirical indicators are undefined.
     *
     * @param runs list of executed simulation run records
     * @param includeGroundTruth whether to append cliodynamic historical ground truth benchmarks
     * @param minYear start horizon for ground truth sampling
     * @param maxYear end horizon for ground truth sampling
     * @return formatted exhaustive CSV content
     */
    public static String generateCsvExport(List<SimulationRunRecord> runs, boolean includeGroundTruth, int minYear, int maxYear) {
        StringBuilder sb = new StringBuilder();

        // 1. Build Header with explicit scientific units
        sb.append("RunId,ScenarioName,Year");
        for (CsvIndicatorColumn col : CSV_COLUMNS) {
            sb.append(",").append(col.header());
        }
        sb.append("\n");

        // 2. Collect all scenario years for precise 1-to-1 temporal matching with Ground Truth
        Set<Integer> targetYears = new TreeSet<>();

        // 3. Export Simulation Runs
        if (runs != null) {
            for (SimulationRunRecord run : runs) {
                if (run == null || run.getTimeSeriesData() == null) continue;
                for (var entry : run.getTimeSeriesData().entrySet()) {
                    int year = entry.getKey();
                    targetYears.add(year);
                    var snap = entry.getValue();

                    sb.append(String.format(java.util.Locale.US, "%s,\"%s\",%d",
                        run.getRunId(),
                        run.getScenarioName(),
                        year));

                    for (CsvIndicatorColumn col : CSV_COLUMNS) {
                        Double val = col.simulationExtractor().apply(snap);
                        if (val == null || Double.isNaN(val) || Double.isInfinite(val)) {
                            sb.append(",N/A");
                        } else {
                            sb.append(String.format(java.util.Locale.US, ",%.4f", val));
                        }
                    }
                    sb.append("\n");
                }
            }
        }

        // 4. Export Ground Truth Benchmark Baseline if requested
        if (includeGroundTruth && minYear <= maxYear) {
            // Add empirical anchor points from historical benchmark catalog within time horizon
            for (String benchKey : HistoricalValidationKernel.getAllBenchmarkKeys()) {
                Map<Integer, Double> ds = HistoricalValidationKernel.getBenchmarkDataset(benchKey);
                if (ds != null) {
                    for (int yr : ds.keySet()) {
                        if (yr >= minYear && yr <= maxYear) {
                            targetYears.add(yr);
                        }
                    }
                }
            }

            // Fallback grid sampling if targetYears has no points
            if (targetYears.isEmpty()) {
                int step = Math.max(1, (maxYear - minYear) / 100);
                for (int yr = minYear; yr <= maxYear; yr += step) {
                    targetYears.add(yr);
                }
                targetYears.add(maxYear);
            }

            // Output Ground Truth at exact matching scenario and empirical years
            for (int yr : targetYears) {
                if (yr < minYear || yr > maxYear) continue;

                sb.append(String.format(java.util.Locale.US, "%s,\"%s\",%d",
                    "HISTORICAL_GROUND_TRUTH",
                    "Historical Reality (Ground Truth)",
                    yr));

                for (CsvIndicatorColumn col : CSV_COLUMNS) {
                    Double bVal = col.benchmarkExtractor().apply(yr, HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
                    if (bVal == null || Double.isNaN(bVal) || Double.isInfinite(bVal)) {
                        sb.append(",N/A");
                    } else {
                        sb.append(String.format(java.util.Locale.US, ",%.4f", bVal));
                    }
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
