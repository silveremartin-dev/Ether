/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.analytics.GroundTruthAuditGenerator.VariableDriftAnalysis;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Markdown statistical tables and diagnostic sections formatter for
 * {@link GroundTruthAuditReportBuilder}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
final class GroundTruthReportSectionsFormatter {

    private GroundTruthReportSectionsFormatter() {
        // Utility formatter
    }

    record TargetAudit(
            GroundTruthAuditReportBuilder.ComparableTrajectory target,
            List<VariableDriftAnalysis> drifts,
            List<String> paramDifferences,
            double avgMape,
            double avgPearsonR,
            int earliestDivergence
    ) {}

    static void appendSummaryRankingTable(
            StringBuilder fullReport,
            List<TargetAudit> audits,
            GroundTruthAuditReportBuilder.ComparableTrajectory reference) {

        fullReport.append("## 🏆 1. Executive Summary & Fidelity Ranking\n\n");
        fullReport.append("| Rank | Scenario | Run ID | Status | Mean Delta / MAPE (%) | Mean Pearson r | Earliest Divergence |\n");
        fullReport.append("|:---:|:---|:---|:---:|:---:|:---:|:---:|\n");

        int rank = 1;
        for (TargetAudit a : audits) {
            String statusIcon = a.avgMape < 15.0 ? "🟢 Aligned" : a.avgMape < 30.0 ? "🟡 Drift" : "🔴 Divergent";
            String divYear = a.earliestDivergence != -1 ? String.valueOf(a.earliestDivergence) : "✅ None";
            fullReport.append(String.format(Locale.US, "| #%d | **%s** | `%s` | %s | **%.2f%%** | **%+.3f** | %s |\n",
                    rank++, a.target.name(), a.target.runId(), statusIcon, a.avgMape, a.avgPearsonR, divYear));
        }
        fullReport.append("\n---\n\n");
    }

    static void appendComparativeCrossMatrix(
            StringBuilder fullReport,
            List<TargetAudit> audits,
            Map<String, String> variableMap) {

        fullReport.append("## 📑 2. Comparative Cross-Matrix Across All Observable Variables\n\n");
        fullReport.append("| Canonical State Variable |");
        for (TargetAudit a : audits) {
            fullReport.append(" ").append(a.target.name()).append(" |");
        }
        fullReport.append("\n|---|");
        for (int i = 0; i < audits.size(); i++) fullReport.append("---|");
        fullReport.append("\n");

        for (Map.Entry<String, String> entry : variableMap.entrySet()) {
            String varLabel = entry.getKey();
            fullReport.append("| **").append(varLabel).append("** |");
            for (TargetAudit a : audits) {
                VariableDriftAnalysis d = a.drifts.stream()
                        .filter(drift -> drift.variableName.equals(varLabel)).findFirst().orElse(null);
                if (d == null || d.sampleCount == 0) {
                    fullReport.append(" ⚪ N/A |");
                } else {
                    String icon = d.status.startsWith("🟢") ? "🟢" : d.status.startsWith("🟡") ? "🟡" : "🔴";
                    fullReport.append(String.format(Locale.US, " %s r=%+.2f (%.1f%%) |", icon, d.pearsonR, d.mape));
                }
            }
            fullReport.append("\n");
        }
        fullReport.append("\n");
    }

    static void appendDeepDiagnosticAnalysis(
            StringBuilder fullReport,
            List<TargetAudit> audits,
            StringBuilder explanationSummary) {

        fullReport.append("## 📊 3. Deep Diagnostic Analysis by Scenario\n\n");
        for (TargetAudit a : audits) {
            fullReport.append("### 🔎 Scenario: `").append(a.target.name()).append("` (`").append(a.target.runId()).append("`)\n\n");

            if (!a.paramDifferences.isEmpty()) {
                fullReport.append("#### 🔧 Parameter & Active Tier 2 Engine Differences vs Reference\n\n");
                for (String diff : a.paramDifferences) {
                    fullReport.append("- ").append(diff).append("\n");
                }
                fullReport.append("\n");
            }

            fullReport.append("#### 🌿 3.1 Tier 1 Direct Biophysical Observables (Conserved Mass, Energy & Population)\n\n");
            fullReport.append("| Audited Variable | Mean Delta / MAPE (%) | Pearson r | Status | Drift Onset | Peak Discrepancy | Responsible Engine |\n");
            fullReport.append("|---|---|---|---|---|---|---|\n");
            for (VariableDriftAnalysis d : a.drifts) {
                if (d.isTier1Biophysical) {
                    fullReport.append(String.format(Locale.US, "| %s | %.2f%% | %+.3f | %s | %s | %s | `%s` |\n",
                            d.variableName, d.mape, d.pearsonR, d.status, d.driftOnsetYear, d.peakDiscrepancy, d.suspectEngine));
                }
            }
            fullReport.append("\n");

            fullReport.append("#### 🏛️ 3.2 Tier 2 Macroeconomic, Institutional & Extended Observables (Theil Decomposition)\n\n");
            fullReport.append("> **Henri Theil Inequality Decomposition ($MSE = U^M + U^S + U^C = 1.0$)**: "
                    + "$U^M$ = Mean/Bias offset ($T_0$), $U^S$ = Variance/Scale difference, $U^C$ = Dynamic covariance alignment.\n\n");
            fullReport.append("| Variable | Pearson r | Theil U^M | Theil U^C | Log Growth r(Δln Y) | T0 Offset | Mean Delta / MAPE | Diagnostic Status | Responsible Theory |\n");
            fullReport.append("|---|---|---|---|---|---|---|---|---|\n");
            for (VariableDriftAnalysis d : a.drifts) {
                if (!d.isTier1Biophysical) {
                    fullReport.append(String.format(Locale.US, "| %s | **%+.3f** | %.1f%% | %.1f%% | %+.3f | %.1f%% | %.1f%% | %s | `%s` |\n",
                            d.variableName, d.pearsonR, d.theilUM * 100.0, d.theilUC * 100.0, d.logGrowthCorrelation, d.initialLevelOffsetPct, d.mape, d.status, d.suspectEngine));
                }
            }
            fullReport.append("\n");

            fullReport.append("#### 📐 3.3 Descriptive Statistics & Hypothesis Tests ($H_0$: Target = Reference)\n\n");
            fullReport.append("| Variable | Samples (N) | Reference Mean (μ_ref) | Target Mean (μ_tgt ± σ) | 95% CI Target | Median [IQR] | Cohen's d | Significance (p-val) |\n");
            fullReport.append("|---|---|---|---|---|---|---|---|\n");
            for (VariableDriftAnalysis d : a.drifts) {
                String obsStr = String.format(Locale.US, "%,.2f", d.meanObs);
                String simStr = String.format(Locale.US, "%,.2f \u00B1 %,.2f", d.meanSim, d.stdSim);
                String iqrStr = String.format(Locale.US, "%,.2f [%,.2f - %,.2f]", d.medianSim, d.q1Sim, d.q3Sim);
                fullReport.append(String.format(Locale.US, "| %s | %d | %s | %s | %s | %s | %.3f | %s |\n",
                        d.variableName, d.sampleCount, obsStr, simStr, d.getCi95Display(), iqrStr, d.cohenD, d.significance));
            }
            fullReport.append("\n---\n\n");

            explanationSummary.append(String.format(Locale.US, "• **%s**: Fidelity %.1f%% (Delta/MAPE %.1f%%, r̄=%+.2f) — %s\n",
                    a.target.name(), Math.max(0.0, 100.0 - a.avgMape), a.avgMape, a.avgPearsonR,
                    a.earliestDivergence != -1 ? String.format("Divergence in %d", a.earliestDivergence) : "Aligned"));
        }
    }
}
