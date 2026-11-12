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
        return BenchmarkCanonicalKeyRegistry.getCanonicalBenchmarkKeyMap();
    }

    static record ComparableTrajectory(
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
                    GroundTruthTrajectoryHelper.addExtraSimulatedVariables(variableMap, baseline.run(), tgt.run(), intersectionYears);
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
                        yr -> GroundTruthTrajectoryHelper.extractFromTrajectory(baseline, varKey, yr, finalMethod, regionalRasterSeries, wbFinal != null),
                        yr -> GroundTruthTrajectoryHelper.extractFromTrajectory(tgt, varKey, yr, finalMethod, Collections.emptyMap(), false)
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
                    ? GroundTruthTrajectoryHelper.computeParameterDiffs(baseline.run(), tgt.run())
                    : Collections.emptyList();

            audits.add(new TargetAudit(tgt, drifts, avgMape, avgTier1Mape, avgR, avgUC, tgtDiv, paramDiffs));
            if (tgtDiv != -1 && (globalEarliestDiv == -1 || tgtDiv < globalEarliestDiv)) {
                globalEarliestDiv = tgtDiv;
            }
        }

        // Sections 1, 2, 3: Formatted via GroundTruthReportSectionsFormatter
        List<GroundTruthReportSectionsFormatter.TargetAudit> formattedAudits = audits.stream()
                .map(a -> new GroundTruthReportSectionsFormatter.TargetAudit(a.target(), a.drifts(), a.paramDifferences(), a.avgMape(), a.avgPearsonR(), a.earliestDivergence()))
                .toList();

        GroundTruthReportSectionsFormatter.appendSummaryRankingTable(fullReport, formattedAudits, baseline);
        GroundTruthReportSectionsFormatter.appendComparativeCrossMatrix(fullReport, formattedAudits, variableMap);
        GroundTruthReportSectionsFormatter.appendDeepDiagnosticAnalysis(fullReport, formattedAudits, explanationSummary);

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
                GroundTruthEngineContributionsFormatter.appendCrossScenarioEngineContributionMatrix(fullReport, runsWithContributions.stream()
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
        return GroundTruthTrajectoryHelper.sampleYearsUniformly(unionYears);
    }

    private static int parseDisplayYear(String formatted) {
        String digits = formatted.replaceAll("[^0-9]", "");
        int val = Integer.parseInt(digits);
        return formatted.contains("BCE") ? -val : val;
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
