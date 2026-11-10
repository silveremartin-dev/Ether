/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.analytics.GroundTruthAuditGenerator.AuditResult;
import org.ether.society.analytics.GroundTruthAuditGenerator.VariableDriftAnalysis;
import org.ether.society.i18n.I18n;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Markdown Audit & Inter-Scenario Comparative Report Builder for {@link GroundTruthAuditGenerator}.
 *
 * <p>Synthesizes multi-scenario ground-truth rankings, comparative gap matrices across all 27
 * canonical cliodynamic benchmark variables, hypothesis-testing summary tables (Cohen's \(d\),
 * 95% confidence intervals, interquartile ranges), and pairwise root-cause divergence analyses.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see GroundTruthAuditGenerator
 * @see ComparativeReportGenerator
 */
final class GroundTruthAuditReportBuilder {

    private GroundTruthAuditReportBuilder() {
        // Static report builder delegate
    }

    /**
     * Builds the ordered map of display labels to canonical 27-variable benchmark keys.
     */
    public static Map<String, String> buildCanonicalBenchmarkKeyMap() {
        Map<String, String> benchmarkKeys = new LinkedHashMap<>();
        benchmarkKeys.put("👥 Global Demographic Population (worldPopulation)", "worldPopulation");
        benchmarkKeys.put("💰 Gross World Product (grossWorldProduct)", "grossWorldProduct");
        benchmarkKeys.put("⚡ Primary Energy Consumption (primaryEnergy)", "primaryEnergy");
        benchmarkKeys.put("🏙️ Urbanization Rate (urbanizationRate)", "urbanizationRate");
        benchmarkKeys.put("🌿 Atmospheric CO2 Concentration (co2Concentration)", "co2Concentration");
        benchmarkKeys.put("📖 Global Literacy Rate (literacyRate)", "literacyRate");
        benchmarkKeys.put("🪙 Currency Debasement / Coinage Purity (currencyDebasement)", "currencyDebasement");
        benchmarkKeys.put("👑 Elite Overproduction Index (eliteOverproductionIndex)", "eliteOverproductionIndex");
        benchmarkKeys.put("🍞 Real Unskilled Wage (realUnskilledWage)", "realUnskilledWage");
        benchmarkKeys.put("🏛️ Political Stress Index (politicalStressIndex)", "politicalStressIndex");
        benchmarkKeys.put("🤝 Asabiyyah Social Cohesion (asabiyyahSocialCohesion)", "asabiyyahSocialCohesion");
        benchmarkKeys.put("📉 Sociopolitical Instability (sociopoliticalInstability)", "sociopoliticalInstability");
        benchmarkKeys.put("⚖️ Gini Inequality (giniInequality)", "giniInequality");
        benchmarkKeys.put("📜 Sovereign Debt Burden (sovereignDebtBurden)", "sovereignDebtBurden");
        benchmarkKeys.put("🚢 Global Trade Volume (globalTradeVolume)", "globalTradeVolume");
        benchmarkKeys.put("🌡️ Temperature Anomaly (temperatureAnomaly)", "temperatureAnomaly");
        benchmarkKeys.put("🌱 Topsoil Erosion Loss (soilErosionRate)", "soilErosionRate");
        benchmarkKeys.put("🌾 Agricultural EROEI (agriculturalEroei)", "agriculturalEroei");
        benchmarkKeys.put("🌲 Forest Cover Remaining (deforestationRate)", "deforestationRate");
        benchmarkKeys.put("⚡ Information Travel Speed (informationSpeed)", "informationSpeed");
        benchmarkKeys.put("🦣 Megafauna Abundance Index (megafaunaIndex)", "megafaunaIndex");
        benchmarkKeys.put("☀️ Milankovitch 65°N Insolation (milankovitchInsolation)", "milankovitchInsolation");
        benchmarkKeys.put("🛡️ Biogeographical Zero-Containment (zeroContainmentScore)", "zeroContainmentScore");
        benchmarkKeys.put("👶 Life Expectancy at Birth (lifeExpectancyAtBirth)", "lifeExpectancyAtBirth");
        benchmarkKeys.put("⚒️ Metal Smelting Per Capita (metalSmeltingPerCapita)", "metalSmeltingPerCapita");
        benchmarkKeys.put("⛏️ Fossil Share Primary Energy (fossilSharePrimaryEnergy)", "fossilSharePrimaryEnergy");
        benchmarkKeys.put("⚓ Shipping Freight Cost Index (shippingFreightCostIndex)", "shippingFreightCostIndex");
        return benchmarkKeys;
    }

    /**
     * Generates a multi-scenario ground truth audit against empirical historical reality.
     *
     * @param simulatedSelected selected scenario benchmark items
     * @param method            benchmark interpolation method
     * @param runResolver       function resolving each scenario item to its {@link SimulationRunRecord}
     * @param <T>               scenario benchmark item subtype
     * @return synthesized {@link AuditResult}
     */
    static <T extends ScenarioBenchmarkItem> AuditResult generateMultiScenarioGroundTruthAudit(
            List<T> simulatedSelected,
            HistoricalValidationKernel.InterpolationMethod method,
            Function<? super T, SimulationRunRecord> runResolver) {

        StringBuilder fullReport = new StringBuilder();
        fullReport.append("# 📊 CLIODYNAMIC AUDIT & HISTORICAL CALIBRATION REPORT\n\n");
        fullReport.append("**Reference Baseline**: Empirical Cliodynamic Reality (-100,000 BP ➔ 2026 CE)\n");
        fullReport.append("**Verification Standard**: Continuous Temporal Confidence Tolerance Envelope $\\tau(t)$ [AGENTS.md §11] (Deep Time: $\\pm 50\\%$, Antiquity: $\\pm 25\\text{-}35\\%$, Modern: $\\pm 5\\%$)\n\n");

        int earliestDivergence = -1;
        StringBuilder explanationSummary = new StringBuilder();
        Map<String, String> benchmarkKeys = buildCanonicalBenchmarkKeyMap();

        record ScenarioAudit(
                ScenarioBenchmarkItem item,
                SimulationRunRecord run,
                List<VariableDriftAnalysis> drifts,
                Map<String, Double> mapes,
                double avgMape,
                double rSquared,
                int scenarioDivergence,
                double avgTier1Mape,
                double avgPearsonR,
                double avgTheilUC
        ) {}

        List<ScenarioAudit> audits = new ArrayList<>();

        for (T item : simulatedSelected) {
            SimulationRunRecord run = runResolver != null ? runResolver.apply(item) : null;
            if (run == null || run.getTimeSeriesData() == null || run.getTimeSeriesData().isEmpty()) {
                continue;
            }

            var timeSeries = run.getTimeSeriesData();
            List<VariableDriftAnalysis> drifts = new ArrayList<>();
            Map<String, Double> mapes = new LinkedHashMap<>();
            double totalMape = 0.0;
            int validVariables = 0;
            int scenarioDiv = -1;
            double totalTier1Mape = 0.0;
            int validTier1Count = 0;
            double totalPearsonR = 0.0;
            double totalTheilUC = 0.0;
            int validAdvancedCount = 0;

            double regionalExtensiveScaleFactor = computeRegionalScaleFactor(run, timeSeries, method);

            if (regionalExtensiveScaleFactor < 0.0) {
                // Non-Earth scenario: Terrestrial historical datasets (HYDE/Maddison) do NOT apply
                for (Map.Entry<String, String> entry : benchmarkKeys.entrySet()) {
                    String label = entry.getKey();
                    VariableDriftAnalysis drift = new VariableDriftAnalysis(
                            label, 0.0, "⚪ Non-Earth Realm", "—", "—", "—", "—",
                            0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "ns",
                            0.0, 0.0, 0.0, 1.0, 0.0, 0.0, false
                    );
                    drifts.add(drift);
                    mapes.put(label, 0.0);
                }
            } else {
                for (Map.Entry<String, String> entry : benchmarkKeys.entrySet()) {
                    String label = entry.getKey();
                    String benchKey = entry.getValue();
                    Map<Integer, Double> benchmark = HistoricalValidationKernel.getBenchmarkDataset(benchKey);
                    VariableDriftAnalysis drift = GroundTruthAuditGenerator.computeVariableDrift(
                            label, benchKey, benchmark, timeSeries, method, regionalExtensiveScaleFactor);
                    drifts.add(drift);
                    mapes.put(label, drift.mape);
                    if (drift.mape > 0.0) {
                        totalMape += drift.mape;
                        validVariables++;
                        if (drift.isTier1Biophysical) {
                            totalTier1Mape += drift.mape;
                            validTier1Count++;
                        }
                        totalPearsonR += drift.pearsonR;
                        totalTheilUC += drift.theilUC;
                        validAdvancedCount++;
                    }
                    if (!"—".equals(drift.driftOnsetYear)) {
                        try {
                            int yr = Integer.parseInt(drift.driftOnsetYear.replaceAll("[^0-9-]", ""));
                            if (scenarioDiv == -1 || yr < scenarioDiv) scenarioDiv = yr;
                        } catch (Exception ignored) {}
                    }
                }
            }

            double avgMape = validVariables > 0 ? (totalMape / validVariables) : 0.0;
            double rSquared = Math.max(0.0, 1.0 - (avgMape / 100.0));
            double avgTier1Mape = validTier1Count > 0 ? (totalTier1Mape / validTier1Count) : avgMape;
            double avgPearsonR = validAdvancedCount > 0 ? (totalPearsonR / validAdvancedCount) : 0.0;
            double avgTheilUC = validAdvancedCount > 0 ? (totalTheilUC / validAdvancedCount) : 1.0;

            audits.add(new ScenarioAudit(item, run, drifts, mapes, avgMape, rSquared, scenarioDiv, avgTier1Mape, avgPearsonR, avgTheilUC));

            if (scenarioDiv != -1 && (earliestDivergence == -1 || scenarioDiv < earliestDivergence)) {
                earliestDivergence = scenarioDiv;
            }
        }

        if (audits.isEmpty()) {
            return new AuditResult(
                    I18n.getOrDefault("analytics.error.no_telemetry_history",
                            "⚠️ No execution telemetry history found for selected simulated scenarios. Please execute or load scenarios first."),
                    -1,
                    ""
            );
        }

        fullReport.append("## 🏆 1. Inter-Scenario Calibration Ranking (Empirical Reality Fitness)\n\n");
        fullReport.append("| # | Scenario | Biophysical Fidelity (1-MAPE_T1) | Trajectory Alignment (Pearson r̄) | Theil Covariance (U^C) | Global MAPE | Diagnostic Status |\n");
        fullReport.append("|---|---|---|---|---|---|---|\n");

        audits.sort(Comparator.comparingDouble(a -> a.avgMape));

        int rank = 1;
        for (ScenarioAudit a : audits) {
            double biophysicalFidelity = Math.max(0.0, 100.0 - a.avgTier1Mape);
            String medal = rank == 1 ? "🥇 " : rank == 2 ? "🥈 " : rank == 3 ? "🥉 " : "• ";
            String diagStatus = (a.avgTier1Mape <= 12.0 && a.avgPearsonR >= 0.80) ? "🟢 Calibrated"
                    : (a.avgTier1Mape <= 25.0 || a.avgPearsonR >= 0.65) ? "🟡 Trajectory Aligned" : "🔴 Divergent";

            fullReport.append(String.format(Locale.US, "| %s%d | **%s** | **%.1f%%** | **%+.3f** | %.1f%% | %.2f%% | %s |\n",
                    medal, rank, a.item.getName(), biophysicalFidelity, a.avgPearsonR, a.avgTheilUC * 100.0, a.avgMape, diagStatus));
            rank++;
        }
        fullReport.append("\n");

        if (audits.size() >= 2) {
            fullReport.append("## 🔬 2. Multi-Scenario Comparative Gap Matrix\n\n");
            fullReport.append("| Historical Benchmark Variable |");
            for (ScenarioAudit a : audits) {
                fullReport.append(" ").append(a.item.getName()).append(" |");
            }
            fullReport.append("\n|---|");
            for (int i = 0; i < audits.size(); i++) fullReport.append("---|");
            fullReport.append("\n");

            for (Map.Entry<String, String> entry : benchmarkKeys.entrySet()) {
                String varLabel = entry.getKey();
                fullReport.append("| **").append(varLabel).append("** |");
                for (ScenarioAudit a : audits) {
                    VariableDriftAnalysis d = a.drifts.stream().filter(drift -> drift.variableName.equals(varLabel)).findFirst().orElse(null);
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

        fullReport.append("## 📊 3. Deep Diagnostic Analysis by Scenario\n\n");
        for (ScenarioAudit a : audits) {
            fullReport.append("### 🔎 Scenario: `").append(a.item.getName()).append("` (`").append(a.item.getRunId()).append("`)\n\n");

            // --- 3.1: Tier 1 Direct Biophysical Observables ---
            fullReport.append("#### 🌿 3.1 Tier 1 Direct Biophysical Observables (Conserved Mass, Energy & Population)\n\n");
            fullReport.append("| Audited Variable | MAPE (%) | Pearson r | Status | Drift Onset | Peak Discrepancy | Responsible Engine |\n");
            fullReport.append("|---|---|---|---|---|---|---|\n");
            for (VariableDriftAnalysis d : a.drifts) {
                if (d.isTier1Biophysical) {
                    fullReport.append(String.format(Locale.US, "| %s | %.2f%% | %+.3f | %s | %s | %s | `%s` |\n",
                            d.variableName, d.mape, d.pearsonR, d.status, d.driftOnsetYear, d.peakDiscrepancy, d.suspectEngine));
                }
            }
            fullReport.append("\n");

            // --- 3.2: Tier 2 Macroeconomic & Institutional Indices ---
            fullReport.append("#### 🏛️ 3.2 Tier 2 Macroeconomic & Institutional Indices (Theil Inequality Decomposition)\n\n");
            fullReport.append("> [!NOTE]\n");
            fullReport.append("> **Henri Theil Inequality Decomposition ($MSE = U^M + U^S + U^C = 1.0$)**:\n");
            fullReport.append("> - **$U^M$ (Mean/Bias Proportion)**: Static level translation offset ($T_0$ initial calibration difference between simulation physical units and historical retrospective index definitions).\n");
            fullReport.append("> - **$U^S$ (Variance Proportion)**: Scale and volatility difference.\n");
            fullReport.append("> - **$U^C$ (Covariance Proportion)**: Dynamic co-movement. High $U^C$ and high Pearson $r \\ge 0.85$ prove structural causal alignment ($dY/dt$) despite static $T_0$ level bias.\n\n");

            fullReport.append("| Macroeconomic / Institutional Variable | Pearson r | Theil U^M (T0 Bias) | Theil U^C (Covar) | Log Growth r(Δln Y) | T0 Level Offset | Raw MAPE | Diagnostic Status | Responsible Theory |\n");
            fullReport.append("|---|---|---|---|---|---|---|---|---|\n");
            for (VariableDriftAnalysis d : a.drifts) {
                if (!d.isTier1Biophysical) {
                    fullReport.append(String.format(Locale.US, "| %s | **%+.3f** | %.1f%% | %.1f%% | %+.3f | %.1f%% | %.1f%% | %s | `%s` |\n",
                            d.variableName, d.pearsonR, d.theilUM * 100.0, d.theilUC * 100.0, d.logGrowthCorrelation, d.initialLevelOffsetPct, d.mape, d.status, d.suspectEngine));
                }
            }
            fullReport.append("\n");

            // --- 3.3: Descriptive Statistics & Hypothesis Tests ---
            fullReport.append("#### 📐 3.3 Descriptive Statistics & Hypothesis Tests (H0: Model = Reality)\n\n");
            fullReport.append("| Variable | Samples (N) | Observed Mean (μ_obs) | Simulated Mean (μ_sim ± σ) | 95% CI Simulated | Median [IQR] | Cohen's d | Significance (p-val) |\n");
            fullReport.append("|---|---|---|---|---|---|---|---|\n");
            for (VariableDriftAnalysis d : a.drifts) {
                String obsStr = String.format(Locale.US, "%,.2f", d.meanObs);
                String simStr = String.format(Locale.US, "%,.2f \u00B1 %,.2f", d.meanSim, d.stdSim);
                String iqrStr = String.format(Locale.US, "%,.2f [%,.2f - %,.2f]", d.medianSim, d.q1Sim, d.q3Sim);
                fullReport.append(String.format(Locale.US, "| %s | %d | %s | %s | %s | %s | %.3f | %s |\n",
                        d.variableName, d.sampleCount, obsStr, simStr, d.getCi95Display(), iqrStr, d.cohenD, d.significance));
            }
            fullReport.append("\n---\n\n");
        }

        fullReport.append("## 🌍 4. Orthogonal 9-Dimensional Geocultural Tensor Spatial Fidelity\n\n");
        fullReport.append("> Empirical 9D geocultural tensor metrics ($r$, $\\text{SSIM}$, $\\text{IoU}$) are computed directly against ground-truth epoch rasters per run and reported dynamically in the spatial geocultural synthesis section.\n\n");

        for (ScenarioAudit a : audits) {
            explanationSummary.append(String.format(Locale.US, "• **%s**: Fidelity %.1f%% (MAPE %.1f%%) — %s\n",
                    a.item.getName(), Math.max(0.0, 100.0 - a.avgMape), a.avgMape,
                    a.scenarioDivergence != -1 ? String.format("Bifurcation in %d CE", a.scenarioDivergence) : "Calibrated"));
        }

        return new AuditResult(fullReport.toString(), earliestDivergence, explanationSummary.toString());
    }

    public static double computeRegionalScaleFactor(SimulationRunRecord run,
                                                     Map<Integer, SimulationRunRecord.MetricSnapshot> timeSeries,
                                                     HistoricalValidationKernel.InterpolationMethod method) {
        if (run != null && run.getParameterMatrix() != null) {
            String planet = run.getParameterMatrix().get("Préréglage Planétaire");
            if (planet == null) planet = run.getParameterMatrix().get("Planet");
            if (planet != null) {
                String pLower = planet.toLowerCase(Locale.ROOT);
                if (!pLower.contains("terre") && !pLower.contains("earth")) {
                    return -1.0; // Marker: Non-Earth Realm (Terrestrial benchmarks do not apply)
                }
            }
            String domain = run.getParameterMatrix().get("Spatial Domain");
            if ("PLANETARY_MESH".equals(domain)) {
                return 1.0;
            }
        }
        List<Integer> sortedYears = new ArrayList<>(timeSeries.keySet());
        Collections.sort(sortedYears);
        if (!sortedYears.isEmpty()) {
            int firstYear = sortedYears.get(0);
            long initialSimPop = timeSeries.get(firstYear).getPopulation();
            double globalPopM = HistoricalValidationKernel.getInterpolatedBenchmarkValue("worldPopulation", firstYear, method);
            if (!Double.isNaN(globalPopM) && globalPopM > 0.0) {
                double globalPop = globalPopM * 1_000_000.0;
                // For regional windows (or when initial simulation population is a strict subset of global ecumene)
                if (initialSimPop > 0 && initialSimPop < globalPop * 0.95) {
                    return (double) initialSimPop / globalPop;
                }
            }
        }
        return 1.0;
    }

    /**
     * Generates a comparative report across pairs of simulated scenarios.
     */
    static <T extends ScenarioBenchmarkItem> AuditResult generateInterScenarioComparativeReport(
            List<T> simulatedSelected,
            RootCauseAnalyzer analyzer,
            Function<? super T, SimulationRunRecord> runResolver,
            SimulationRunRepository runRepository) {

        T baselineItem = simulatedSelected.get(0);
        SimulationRunRecord baseline = runResolver != null ? runResolver.apply(baselineItem) : null;

        if (baseline == null) {
            return new AuditResult(
                    I18n.getOrDefault("analytics.error.no_telemetry", "⚠️ Unable to access base scenario telemetry data."),
                    -1,
                    ""
            );
        }

        StringBuilder explanationSummary = new StringBuilder();
        StringBuilder multiReport = new StringBuilder();
        multiReport.append("# ").append(I18n.getOrDefault("analytics.report.title",
                "📊 RAPPORT D'ANALYSE COMPARATIVE INTER-SCÉNARIOS")).append("\n\n");
        multiReport.append(String.format("**%s** : `%s`\n\n",
                I18n.getOrDefault("analytics.report.baseline_label", "Baseline Reference Scenario"),
                baselineItem.getName()));

        int firstDivergence = -1;

        for (int i = 1; i < simulatedSelected.size(); i++) {
            T targetItem = simulatedSelected.get(i);
            SimulationRunRecord target = runResolver != null ? runResolver.apply(targetItem) : null;

            if (target != null && analyzer != null) {
                RootCauseAnalyzer.ComparisonResult result = analyzer.compareRuns(baseline, target);
                if (result.getDivergenceYear() != -1 && (firstDivergence == -1 || result.getDivergenceYear() < firstDivergence)) {
                    firstDivergence = result.getDivergenceYear();
                }
                explanationSummary.append(String.format("• **vs %s** : %s\n",
                        targetItem.getName(), result.getPrimaryRootCauseExplanation()));
                multiReport.append(ComparativeReportGenerator.generateMarkdownReport(result));
                multiReport.append("\n\n---\n\n");
            }
        }

        if (simulatedSelected.size() >= 3 && runRepository != null && analyzer != null) {
            multiReport.append("## 🔀 Matrice croisée entre scénarios alternatifs\n\n");
            for (int i = 1; i < simulatedSelected.size(); i++) {
                for (int j = i + 1; j < simulatedSelected.size(); j++) {
                    ScenarioBenchmarkItem itemA = simulatedSelected.get(i);
                    ScenarioBenchmarkItem itemB = simulatedSelected.get(j);
                    SimulationRunRecord runA = runRepository.getRun(itemA.getRunId());
                    if (runA == null) runA = runRepository.getRunByScenarioName(itemA.getName()).orElse(null);
                    SimulationRunRecord runB = runRepository.getRun(itemB.getRunId());
                    if (runB == null) runB = runRepository.getRunByScenarioName(itemB.getName()).orElse(null);

                    if (runA != null && runB != null) {
                        RootCauseAnalyzer.ComparisonResult crossRes = analyzer.compareRuns(runA, runB);
                        multiReport.append(String.format("### ⚖️ Comparaison : %s ⟷ %s\n\n", itemA.getName(), itemB.getName()));
                        if (crossRes.getDivergenceYear() != -1) {
                            multiReport.append(String.format("> ⚡ **Bifurcation ($T_{\\text{divergence}}$)** : Année **%d AD**\n\n",
                                    crossRes.getDivergenceYear()));
                        }
                        appendCrossDifferences(crossRes, multiReport);
                        multiReport.append("\n---\n\n");
                    }
                }
            }
        }

        return new AuditResult(multiReport.toString(), firstDivergence, explanationSummary.toString());
    }

    private static void appendCrossDifferences(RootCauseAnalyzer.ComparisonResult crossRes, StringBuilder multiReport) {
        multiReport.append(String.format("> 💡 %s\n\n", crossRes.getPrimaryRootCauseExplanation()));
        if (!crossRes.getKeyDifferences().isEmpty()) {
            multiReport.append("#### Différences paramétriques :\n");
            for (String diff : crossRes.getKeyDifferences()) {
                multiReport.append("- ").append(diff).append("\n");
            }
            multiReport.append("\n");
        }
    }
}
