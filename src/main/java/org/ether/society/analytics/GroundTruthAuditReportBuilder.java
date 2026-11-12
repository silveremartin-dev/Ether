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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    private static final Logger logger = LoggerFactory.getLogger(GroundTruthAuditReportBuilder.class);

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

    private record ComparableTrajectory(
            String name,
            String runId,
            boolean isGroundTruth,
            boolean isPredictive,
            SimulationRunRecord run,
            int startYear,
            int endYear
    ) {}

    /**
     * Unified comparative report across any combination of 2+ items (or 1 simulated item paired
     * implicitly with historical ground truth): empirical reality, predictive SSP baselines,
     * and/or simulated scenario runs, strictly aligned over their date intersection and covering
     * all available variables plus Tier 2 engine partial contributions.
     */
    static <T extends ScenarioBenchmarkItem> AuditResult generateUnifiedComparativeReport(
            List<T> selectedItems,
            HistoricalValidationKernel.InterpolationMethod method,
            Function<? super T, SimulationRunRecord> runResolver,
            RootCauseAnalyzer analyzer,
            SimulationRunRepository runRepository) {

        if (selectedItems == null || selectedItems.isEmpty()) {
            return new AuditResult(
                    I18n.getOrDefault("analytics.error.no_telemetry_history",
                            "⚠️ No execution telemetry history found for selected scenarios."),
                    -1, "");
        }

        if (method == null) {
            method = HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC;
        }

        List<ComparableTrajectory> trajectories = new ArrayList<>();
        ComparableTrajectory explicitVirtualBaseline = null;

        for (T item : selectedItems) {
            String rid = item.getRunId();
            if ("HISTORICAL_GROUND_TRUTH".equals(rid)) {
                ComparableTrajectory gt = new ComparableTrajectory(
                        item.getName() != null ? item.getName() : "Historical Reality (Cliodynamic Ground Truth)",
                        rid, true, false, null, -3_000_000, HistoricalValidationKernel.HYDE_MAX_OBSERVATION_YEAR);
                trajectories.add(gt);
                if (explicitVirtualBaseline == null) explicitVirtualBaseline = gt;
            } else if (rid != null && rid.startsWith("PREDICTIVE_")) {
                ComparableTrajectory pred = new ComparableTrajectory(
                        item.getName() != null ? item.getName() : rid,
                        rid, false, true, null, HistoricalValidationKernel.HYDE_MAX_OBSERVATION_YEAR, HistoricalValidationKernel.PROJECTION_MAX_YEAR);
                trajectories.add(pred);
                if (explicitVirtualBaseline == null) explicitVirtualBaseline = pred;
            } else {
                SimulationRunRecord run = resolveRun(item, runResolver, runRepository);
                if (run != null && run.getTimeSeriesData() != null && !run.getTimeSeriesData().isEmpty()) {
                    trajectories.add(new ComparableTrajectory(
                            item.getName() != null ? item.getName() : run.getScenarioName(),
                            run.getRunId(), false, false, run, run.getStartYear(), run.getEndYear()));
                }
            }
        }

        if (trajectories.isEmpty()) {
            return new AuditResult(
                    I18n.getOrDefault("analytics.error.no_telemetry_history",
                            "⚠️ No execution telemetry history found for selected simulated scenarios. Please execute or load scenarios first."),
                    -1, "");
        }

        // Single simulated item -> implicitly compare against Historical Reality
        if (trajectories.size() == 1 && !trajectories.get(0).isGroundTruth() && !trajectories.get(0).isPredictive()) {
            ComparableTrajectory gt = new ComparableTrajectory(
                    "Empirical Cliodynamic Reality (-100,000 BP ➔ 2025 CE)",
                    "HISTORICAL_GROUND_TRUTH", true, false, null,
                    -3_000_000, HistoricalValidationKernel.HYDE_MAX_OBSERVATION_YEAR);
            trajectories.add(0, gt);
            explicitVirtualBaseline = gt;
        }

        if (trajectories.size() < 2) {
            return new AuditResult(
                    I18n.getOrDefault("analytics.divergence.prompt_report",
                            "## Please select at least two executed scenarios to generate a unified comparative report."),
                    -1, "");
        }

        // Compute exact temporal intersection across all selected trajectories
        int intersectionStart = Integer.MIN_VALUE;
        int intersectionEnd = Integer.MAX_VALUE;
        for (ComparableTrajectory t : trajectories) {
            intersectionStart = Math.max(intersectionStart, t.startYear());
            intersectionEnd = Math.min(intersectionEnd, t.endYear());
        }

        if (intersectionStart > intersectionEnd) {
            StringBuilder disjointMsg = new StringBuilder();
            disjointMsg.append("# ⚠️ Disjoint Time Intervals Across Selected Scenarios\n\n");
            disjointMsg.append("The selected trajectories have no overlapping calendar year intersection:\n\n");
            for (ComparableTrajectory t : trajectories) {
                disjointMsg.append(String.format(Locale.US, "- **%s** (`%s`): `[%d .. %d]`\n",
                        t.name(), t.runId(), t.startYear(), t.endYear()));
            }
            return new AuditResult(disjointMsg.toString(), -1,
                    "Disjoint time ranges: no overlapping calendar years between selected scenarios.");
        }

        // Choose reference baseline (virtual empirical/predictive baseline if selected, otherwise first selected run)
        ComparableTrajectory baseline = (explicitVirtualBaseline != null) ? explicitVirtualBaseline : trajectories.get(0);
        List<ComparableTrajectory> targets = new ArrayList<>();
        for (ComparableTrajectory t : trajectories) {
            if (t != baseline) {
                targets.add(t);
            }
        }

        // Build common evaluation timeline strictly inside [intersectionStart, intersectionEnd]
        List<Integer> intersectionYears = buildIntersectionSampleYears(trajectories, intersectionStart, intersectionEnd);
        if (intersectionYears.isEmpty()) {
            return new AuditResult(
                    "⚠️ No common sample years found within intersection [" + intersectionStart + " .. " + intersectionEnd + "].",
                    -1, "");
        }

        // Build unified variable catalog (27 canonical variables + extra common metrics when comparing simulated runs)
        Map<String, String> variableMap = new LinkedHashMap<>(buildCanonicalBenchmarkKeyMap());
        if (!baseline.isGroundTruth() && !baseline.isPredictive()) {
            for (ComparableTrajectory tgt : targets) {
                if (!tgt.isGroundTruth() && !tgt.isPredictive()) {
                    addExtraSimulatedVariables(variableMap, baseline.run(), tgt.run(), intersectionYears);
                }
            }
        }

        StringBuilder fullReport = new StringBuilder();
        fullReport.append("# 📊 UNIFIED COMPARATIVE & CLIODYNAMIC AUDIT REPORT\n\n");
        fullReport.append(String.format(Locale.US, "**Reference Baseline**: `%s` (`%s`)\n\n", baseline.name(), baseline.runId()));
        fullReport.append(String.format(Locale.US, "**Date Intersection Window**: `[%d .. %d]` (%d aligned sample points)\n\n",
                intersectionStart, intersectionEnd, intersectionYears.size()));
        if (baseline.isGroundTruth()) {
            fullReport.append("**Verification Standard**: Continuous Temporal Confidence Tolerance Envelope $\\tau(t)$ [AGENTS.md §11] (Deep Time: $\\pm 50\\%$, Antiquity: $\\pm 25\\text{-}35\\%$, Modern: $\\pm 5\\%$)\n\n");
        }

        record TargetAudit(
                ComparableTrajectory target,
                List<VariableDriftAnalysis> drifts,
                double avgMape,
                double avgTier1Mape,
                double avgPearsonR,
                double avgTheilUC,
                int earliestDivergence,
                List<String> paramDifferences
        ) {}

        List<TargetAudit> audits = new ArrayList<>();
        int globalEarliestDiv = -1;
        StringBuilder explanationSummary = new StringBuilder();
        final HistoricalValidationKernel.InterpolationMethod finalMethod = method;

        for (ComparableTrajectory tgt : targets) {
            double scaleFactor = 1.0;
            double[] windowBounds = null;
            if (baseline.isGroundTruth() && !tgt.isGroundTruth() && !tgt.isPredictive() && tgt.run() != null) {
                scaleFactor = computeRegionalScaleFactor(tgt.run(), tgt.run().getTimeSeriesData(), finalMethod);
                windowBounds = RegionalRasterBenchmarkExtractor.parseWindowBounds(tgt.run());
            }

            List<VariableDriftAnalysis> drifts = new ArrayList<>();
            double totalMape = 0.0;
            int validVars = 0;
            double totalTier1Mape = 0.0;
            int validTier1 = 0;
            double totalR = 0.0;
            double totalUC = 0.0;
            int validAdv = 0;
            int tgtDiv = -1;

            for (Map.Entry<String, String> varEntry : variableMap.entrySet()) {
                String label = varEntry.getKey();
                String varKey = varEntry.getValue();

                if (scaleFactor < 0.0) {
                    drifts.add(new VariableDriftAnalysis(
                            label, 0.0, "⚪ Non-Earth Realm", "—", "—", "—", "—",
                            0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "ns",
                            0.0, 0.0, 0.0, 1.0, 0.0, 0.0, GroundTruthAuditGenerator.isTier1BiophysicalVariable(varKey)));
                    continue;
                }
                if (baseline.isGroundTruth() && windowBounds != null
                        && RegionalRasterBenchmarkExtractor.isUnrasteredExtensiveGlobalMetric(varKey)) {
                    drifts.add(new VariableDriftAnalysis(
                            label, 0.0, "⚪ Regional (No 2D Ground-Truth Raster)", "—", "—", "—", "—",
                            0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "ns",
                            0.0, 0.0, 0.0, 1.0, 0.0, 0.0, GroundTruthAuditGenerator.isTier1BiophysicalVariable(varKey)));
                    continue;
                }

                Map<Integer, Double> regionalRasterSeries = (baseline.isGroundTruth() && windowBounds != null
                        && RegionalRasterBenchmarkExtractor.supportsRegionalRaster(varKey))
                        ? RegionalRasterBenchmarkExtractor.extractRegionalSeries(varKey, windowBounds, intersectionYears)
                        : Collections.emptyMap();

                final double[] wbFinal = windowBounds;
                VariableDriftAnalysis drift = GroundTruthAuditGenerator.computePairwiseDrift(
                        label,
                        varKey,
                        intersectionYears,
                        yr -> extractFromTrajectory(baseline, varKey, yr, finalMethod, regionalRasterSeries, wbFinal != null),
                        yr -> extractFromTrajectory(tgt, varKey, yr, finalMethod, Collections.emptyMap(), false)
                );
                drifts.add(drift);

                if (drift.sampleCount > 0) {
                    totalMape += drift.mape;
                    validVars++;
                    if (drift.isTier1Biophysical) {
                        totalTier1Mape += drift.mape;
                        validTier1++;
                    }
                    totalR += drift.pearsonR;
                    totalUC += drift.theilUC;
                    validAdv++;
                    if (!"—".equals(drift.driftOnsetYear)) {
                        try {
                            int yr = parseDisplayYear(drift.driftOnsetYear);
                            if (tgtDiv == -1 || yr < tgtDiv) tgtDiv = yr;
                        } catch (Exception ex) {
                            logger.debug("Failed to parse drift onset year '{}': {}", drift.driftOnsetYear, ex.getMessage());
                        }
                    }
                }
            }

            double avgMape = validVars > 0 ? totalMape / validVars : 0.0;
            double avgTier1Mape = validTier1 > 0 ? totalTier1Mape / validTier1 : avgMape;
            double avgR = validAdv > 0 ? totalR / validAdv : 0.0;
            double avgUC = validAdv > 0 ? totalUC / validAdv : 1.0;

            List<String> paramDiffs = (!baseline.isGroundTruth() && !baseline.isPredictive() && !tgt.isGroundTruth() && !tgt.isPredictive())
                    ? computeParameterDiffs(baseline.run(), tgt.run())
                    : Collections.emptyList();

            audits.add(new TargetAudit(tgt, drifts, avgMape, avgTier1Mape, avgR, avgUC, tgtDiv, paramDiffs));
            if (tgtDiv != -1 && (globalEarliestDiv == -1 || tgtDiv < globalEarliestDiv)) {
                globalEarliestDiv = tgtDiv;
            }
        }

        // Section 1: Ranking & Summary
        fullReport.append("## 🏆 1. Comparative Ranking & Alignment Summary (Date Intersection)\n\n");
        fullReport.append("| # | Compared Scenario | Tier 1 Fidelity (1-MAPE_T1) | Trajectory Alignment (Pearson r̄) | Theil Covariance (U^C) | Mean Delta / MAPE | Diagnostic Status |\n");
        fullReport.append("|---|---|---|---|---|---|---|\n");

        audits.sort(Comparator.comparingDouble(a -> a.avgMape));
        int rank = 1;
        for (TargetAudit a : audits) {
            double fidelity = Math.max(0.0, 100.0 - a.avgTier1Mape);
            String medal = rank == 1 ? "🥇 " : rank == 2 ? "🥈 " : rank == 3 ? "🥉 " : "• ";
            String diagStatus = (a.avgTier1Mape <= 12.0 && a.avgPearsonR >= 0.80) ? "🟢 Calibrated / Aligned"
                    : (a.avgTier1Mape <= 25.0 || a.avgPearsonR >= 0.65) ? "🟡 Trajectory Aligned" : "🔴 Divergent";
            fullReport.append(String.format(Locale.US, "| %s%d | **%s** | **%.1f%%** | **%+.3f** | %.1f%% | %.2f%% | %s |\n",
                    medal, rank, a.target().name(), fidelity, a.avgPearsonR, a.avgTheilUC * 100.0, a.avgMape, diagStatus));
            rank++;
        }
        fullReport.append("\n");

        // Section 2: Unified Comparative Matrix across all variables on the date intersection
        if (!audits.isEmpty()) {
            fullReport.append("## 🔬 2. Multi-Variable Comparative Matrix (Intersection `")
                    .append(intersectionStart).append(" .. ").append(intersectionEnd).append("`)\n\n");
            fullReport.append("| Variable |");
            for (TargetAudit a : audits) {
                fullReport.append(" ").append(a.target().name()).append(" |");
            }
            fullReport.append("\n|---|");
            for (int i = 0; i < audits.size(); i++) fullReport.append("---|");
            fullReport.append("\n");

            for (Map.Entry<String, String> entry : variableMap.entrySet()) {
                String varLabel = entry.getKey();
                fullReport.append("| **").append(varLabel).append("** |");
                for (TargetAudit a : audits) {
                    VariableDriftAnalysis d = a.drifts().stream()
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

        // Section 3: Deep Diagnostic Analysis per target
        fullReport.append("## 📊 3. Deep Diagnostic Analysis by Scenario\n\n");
        for (TargetAudit a : audits) {
            fullReport.append("### 🔎 Scenario: `").append(a.target().name()).append("` (`").append(a.target().runId()).append("`)\n\n");

            if (!a.paramDifferences().isEmpty()) {
                fullReport.append("#### 🔧 Parameter & Active Tier 2 Engine Differences vs Reference\n\n");
                for (String diff : a.paramDifferences()) {
                    fullReport.append("- ").append(diff).append("\n");
                }
                fullReport.append("\n");
            }

            fullReport.append("#### 🌿 3.1 Tier 1 Direct Biophysical Observables (Conserved Mass, Energy & Population)\n\n");
            fullReport.append("| Audited Variable | Mean Delta / MAPE (%) | Pearson r | Status | Drift Onset | Peak Discrepancy | Responsible Engine |\n");
            fullReport.append("|---|---|---|---|---|---|---|\n");
            for (VariableDriftAnalysis d : a.drifts()) {
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
            for (VariableDriftAnalysis d : a.drifts()) {
                if (!d.isTier1Biophysical) {
                    fullReport.append(String.format(Locale.US, "| %s | **%+.3f** | %.1f%% | %.1f%% | %+.3f | %.1f%% | %.1f%% | %s | `%s` |\n",
                            d.variableName, d.pearsonR, d.theilUM * 100.0, d.theilUC * 100.0, d.logGrowthCorrelation, d.initialLevelOffsetPct, d.mape, d.status, d.suspectEngine));
                }
            }
            fullReport.append("\n");

            fullReport.append("#### 📐 3.3 Descriptive Statistics & Hypothesis Tests ($H_0$: Target = Reference)\n\n");
            fullReport.append("| Variable | Samples (N) | Reference Mean (μ_ref) | Target Mean (μ_tgt ± σ) | 95% CI Target | Median [IQR] | Cohen's d | Significance (p-val) |\n");
            fullReport.append("|---|---|---|---|---|---|---|---|\n");
            for (VariableDriftAnalysis d : a.drifts()) {
                String obsStr = String.format(Locale.US, "%,.2f", d.meanObs);
                String simStr = String.format(Locale.US, "%,.2f \u00B1 %,.2f", d.meanSim, d.stdSim);
                String iqrStr = String.format(Locale.US, "%,.2f [%,.2f - %,.2f]", d.medianSim, d.q1Sim, d.q3Sim);
                fullReport.append(String.format(Locale.US, "| %s | %d | %s | %s | %s | %s | %.3f | %s |\n",
                        d.variableName, d.sampleCount, obsStr, simStr, d.getCi95Display(), iqrStr, d.cohenD, d.significance));
            }
            fullReport.append("\n---\n\n");

            explanationSummary.append(String.format(Locale.US, "• **%s**: Fidelity %.1f%% (Delta/MAPE %.1f%%, r̄=%+.2f) — %s\n",
                    a.target().name(), Math.max(0.0, 100.0 - a.avgMape), a.avgMape, a.avgPearsonR,
                    a.earliestDivergence() != -1 ? String.format("Divergence in %d", a.earliestDivergence()) : "Aligned"));
        }

        // Section 4: Partial Tier 2 Engine Contributions (if available on any simulated run)
        List<ComparableTrajectory> runsWithContributions = new ArrayList<>();
        for (ComparableTrajectory t : trajectories) {
            if (t.run() != null && t.run().getEngineContributionReport() != null) {
                runsWithContributions.add(t);
            }
        }
        if (!runsWithContributions.isEmpty()) {
            fullReport.append("## ⚙️ 4. Partial Contribution of Tier 2 Engines (Variance Attribution)\n\n");
            for (ComparableTrajectory t : runsWithContributions) {
                fullReport.append(t.run().getEngineContributionReport().toMarkdownSection());
            }
            if (runsWithContributions.size() >= 2) {
                appendCrossScenarioEngineContributionMatrix(fullReport, runsWithContributions.stream()
                        .map(t -> t.run().getEngineContributionReport()).toList());
            }
        }

        return new AuditResult(fullReport.toString(), globalEarliestDiv, explanationSummary.toString());
    }

    private static <T extends ScenarioBenchmarkItem> SimulationRunRecord resolveRun(
            T item,
            Function<? super T, SimulationRunRecord> runResolver,
            SimulationRunRepository runRepository) {
        SimulationRunRecord r = runResolver != null ? runResolver.apply(item) : null;
        if (r == null && runRepository != null) {
            r = runRepository.getRun(item.getRunId());
            if (r == null) {
                r = runRepository.getRunByScenarioName(item.getName()).orElse(null);
            }
        }
        return r;
    }

    private static List<Integer> buildIntersectionSampleYears(
            List<ComparableTrajectory> trajectories,
            int intersectionStart,
            int intersectionEnd) {
        java.util.TreeSet<Integer> unionYears = new java.util.TreeSet<>();
        for (ComparableTrajectory t : trajectories) {
            SimulationRunRecord run = t.run();
            if (run != null && run.getTimeSeriesData() != null) {
                for (Integer yr : run.getTimeSeriesData().keySet()) {
                    if (yr >= intersectionStart && yr <= intersectionEnd) {
                        unionYears.add(yr);
                    }
                }
            }
        }

        if (unionYears.isEmpty()) {
            int span = Math.max(1, intersectionEnd - intersectionStart);
            int step = Math.max(1, span / 80);
            for (int y = intersectionStart; y <= intersectionEnd; y += step) {
                unionYears.add(y);
            }
            unionYears.add(intersectionEnd);
        }

        List<Integer> sorted = new ArrayList<>(unionYears);
        if (sorted.size() <= 80) {
            return sorted;
        }
        List<Integer> sampled = new ArrayList<>();
        int step = Math.max(1, sorted.size() / 80);
        for (int i = 0; i < sorted.size(); i += step) {
            sampled.add(sorted.get(i));
        }
        if (!sampled.contains(sorted.get(sorted.size() - 1))) {
            sampled.add(sorted.get(sorted.size() - 1));
        }
        return sampled;
    }

    private static void addExtraSimulatedVariables(
            Map<String, String> variableMap,
            SimulationRunRecord runA,
            SimulationRunRecord runB,
            List<Integer> intersectionYears) {
        if (runA == null || runB == null || intersectionYears.isEmpty()) return;
        java.util.Set<String> existingKeys = new java.util.HashSet<>(variableMap.values());
        java.util.Set<String> candidateKeys = new java.util.LinkedHashSet<>();
        for (int yr : intersectionYears) {
            SimulationRunRecord.MetricSnapshot sA = runA.getSnapshotAt(yr);
            SimulationRunRecord.MetricSnapshot sB = runB.getSnapshotAt(yr);
            if (sA != null && sA.getMetricsMap() != null) candidateKeys.addAll(sA.getMetricsMap().keySet());
            if (sB != null && sB.getMetricsMap() != null) candidateKeys.addAll(sB.getMetricsMap().keySet());
        }
        for (String key : candidateKeys) {
            if (existingKeys.contains(key)) continue;
            // Skip redundant internal aliases already represented in the 27 canonical keys
            if (Set.of("population", "regionalPopulation", "gdp", "gdpTotal", "regionalGdp", "energyCaptured",
                    "regionalEnergy", "gini", "giniIndex", "asabiyyah", "psiStressIndex", "conflict",
                    "lifeExpectancy", "educationLevel", "carbonFootprint", "systemInterdependence").contains(key)) {
                continue;
            }
            MetricDescriptor desc = MetricRegistry.getInstance().getDescriptor(key);
            String label = (desc != null && desc.getDisplayName() != null)
                    ? desc.getDisplayName() + " (" + key + ")"
                    : key;
            variableMap.put(label, key);
            existingKeys.add(key);
        }
    }

    private static double extractFromTrajectory(
            ComparableTrajectory traj,
            String varKey,
            int year,
            HistoricalValidationKernel.InterpolationMethod method,
            Map<Integer, Double> regionalRasterSeries,
            boolean hasWindowBounds) {
        if (traj == null) return Double.NaN;
        if (traj.isGroundTruth()) {
            return (regionalRasterSeries != null && !regionalRasterSeries.isEmpty())
                    ? regionalRasterSeries.getOrDefault(year, Double.NaN)
                    : HistoricalValidationKernel.getInterpolatedBenchmarkValue(varKey, year, method);
        }
        if (traj.isPredictive()) {
            return HistoricalValidationKernel.getPredictiveBenchmarkValue(traj.runId(), varKey, year, method);
        }
        SimulationRunRecord run = traj.run();
        if (run == null || run.getTimeSeriesData().isEmpty()) return Double.NaN;

        SimulationRunRecord.MetricSnapshot snap = interpolateOrFloorSnapshot(run, year);
        if (snap == null) return Double.NaN;
        double v = GroundTruthAuditGenerator.extractValue(snap, varKey, year);
        if (Double.isNaN(v) || Double.isInfinite(v)) return Double.NaN;
        if ("milankovitchInsolation".equals(varKey) && v <= 0.0) return Double.NaN;
        return GroundTruthAuditGenerator.normalizeSimulatedUnits(varKey, v);
    }

    private static SimulationRunRecord.MetricSnapshot interpolateOrFloorSnapshot(SimulationRunRecord run, int year) {
        var ts = run.getTimeSeriesData();
        if (ts.containsKey(year)) return ts.get(year);
        Map.Entry<Integer, SimulationRunRecord.MetricSnapshot> floor = ts.floorEntry(year);
        Map.Entry<Integer, SimulationRunRecord.MetricSnapshot> ceil = ts.ceilingEntry(year);
        if (floor == null) return ceil != null ? ceil.getValue() : null;
        if (ceil == null) return floor.getValue();
        return Math.abs(year - floor.getKey()) <= Math.abs(ceil.getKey() - year) ? floor.getValue() : ceil.getValue();
    }

    private static List<String> computeParameterDiffs(SimulationRunRecord baseline, SimulationRunRecord target) {
        if (baseline == null || target == null) return Collections.emptyList();
        List<String> diffs = new ArrayList<>();
        java.util.Set<String> allKeys = new java.util.LinkedHashSet<>(baseline.getParameterMatrix().keySet());
        allKeys.addAll(target.getParameterMatrix().keySet());
        for (String key : allKeys) {
            String vA = baseline.getParameterMatrix().getOrDefault(key, "OFF / N/A");
            String vB = target.getParameterMatrix().getOrDefault(key, "OFF / N/A");
            if (!vA.equalsIgnoreCase(vB)) {
                diffs.add(String.format("`%s` : `%s` ➔ `%s`", key, vA, vB));
            }
        }
        return diffs;
    }

    private static int parseDisplayYear(String formatted) {
        String digits = formatted.replaceAll("[^0-9]", "");
        int val = Integer.parseInt(digits);
        return formatted.contains("BCE") ? -val : val;
    }

    private static void appendCrossScenarioEngineContributionMatrix(
            StringBuilder sb,
            List<EngineContributionReport> reports) {
        sb.append("### 🔀 Cross-Scenario Tier 2 Engine Contribution Matrix (%)\n\n");
        java.util.Set<String> allEngines = new java.util.TreeSet<>();
        for (EngineContributionReport r : reports) {
            for (EngineContributionReport.EngineEntry e : r.getEntries()) {
                allEngines.add(e.engineId());
            }
        }
        if (allEngines.isEmpty()) return;

        sb.append("| Tier 2 Engine |");
        for (EngineContributionReport r : reports) {
            sb.append(" ").append(r.getScenarioName() != null ? r.getScenarioName() : r.getRunId()).append(" |");
        }
        sb.append("\n|---|");
        for (int i = 0; i < reports.size(); i++) sb.append("---:|");
        sb.append("\n");

        List<Map<String, EngineContributionReport.EngineEntry>> maps = reports.stream()
                .map(EngineContributionReport::toEngineMap).toList();
        for (String engineId : allEngines) {
            sb.append("| `").append(engineId).append("` |");
            for (Map<String, EngineContributionReport.EngineEntry> m : maps) {
                EngineContributionReport.EngineEntry entry = m.get(engineId);
                if (entry == null) {
                    sb.append(" OFF |");
                } else {
                    sb.append(String.format(Locale.US, " %.2f%% |", entry.globalVarianceSharePct()));
                }
            }
            sb.append("\n");
        }
        sb.append("\n");
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
        if (simulatedSelected == null || simulatedSelected.isEmpty()) {
            return new AuditResult(
                    I18n.getOrDefault("analytics.error.no_telemetry_history",
                            "⚠️ No execution telemetry history found for selected simulated scenarios. Please execute or load scenarios first."),
                    -1, "");
        }
        // Build selection with explicit Ground Truth reference first, delegating to unified report engine
        List<ScenarioBenchmarkItem> unifiedList = new ArrayList<>();
        unifiedList.add(new ScenarioBenchmarkItem() {
            @Override public String getName() { return "Empirical Cliodynamic Reality (-100,000 BP ➔ 2025 CE)"; }
            @Override public String getRunId() { return "HISTORICAL_GROUND_TRUTH"; }
        });
        Map<ScenarioBenchmarkItem, T> typedLookup = new java.util.IdentityHashMap<>();
        for (T item : simulatedSelected) {
            unifiedList.add(item);
            typedLookup.put(item, item);
        }
        return generateUnifiedComparativeReport(
                unifiedList,
                method,
                item -> {
                    T typed = typedLookup.get(item);
                    return (typed != null && runResolver != null) ? runResolver.apply(typed) : null;
                },
                null,
                SimulationRunRepository.getInstance()
        );
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
        return 1.0;
    }

    /**
     * Generates a comparative report across pairs of simulated scenarios using the unified
     * date-intersection and multi-variable engine.
     */
    static <T extends ScenarioBenchmarkItem> AuditResult generateInterScenarioComparativeReport(
            List<T> simulatedSelected,
            RootCauseAnalyzer analyzer,
            Function<? super T, SimulationRunRecord> runResolver,
            SimulationRunRepository runRepository) {
        return generateUnifiedComparativeReport(
                simulatedSelected,
                HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                runResolver,
                analyzer,
                runRepository
        );
    }
}
