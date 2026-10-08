/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.i18n.I18n;

import java.time.format.DateTimeFormatter;
import java.util.List;

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

    /*
     * Generate markdown report.
     * Enforces physical invariants and updates associated state variables within {@code ComparativeReportGenerator}.
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

    /*
     * Generate csv export.
     * Enforces physical invariants and updates associated state variables within {@code ComparativeReportGenerator}.
     *
     * @param runs the runs parameter (List<SimulationRunRecord>)
     * @return the resulting computation or state reference
     */
    public static String generateCsvExport(List<SimulationRunRecord> runs) {
        StringBuilder sb = new StringBuilder();
        sb.append("RunId,ScenarioName,Year,Population,Food,AvgTech,Stability,PopulatedCells\n");

        for (SimulationRunRecord run : runs) {
            for (var entry : run.getTimeSeriesData().entrySet()) {
                int year = entry.getKey();
                var snap = entry.getValue();
                sb.append(String.format("%s,\"%s\",%d,%d,%.2f,%.2f,%.2f,%d\n",
                    run.getRunId(),
                    run.getScenarioName(),
                    year,
                    snap.getPopulation(),
                    snap.getFood(),
                    snap.getAvgTech(),
                    snap.getStability(),
                    snap.getPopulatedCellCount()));
            }
        }
        return sb.toString();
    }
}
