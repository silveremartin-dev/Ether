/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.i18n.I18n;

import java.util.*;
import java.util.function.Function;

/**
 * Cliodynamic Ground Truth Audit and Parameter Drift Analysis Generator.
 *
 * <p>Benchmarking and epistemic validation harness evaluating simulated societal trajectories
 * against empirical historical reality (Seshat Global History Databank, HYDE 3.4, Maddison Project,
 * and EPICA ice core reconstructions).</p>
 *
 * <p>Computes Mean Absolute Percentage Error (MAPE), Cohen's \(d\) effect size,
 * 95% confidence intervals, and statistical bifurcation onsets. Markdown synthesis and
 * multi-scenario cross-matrices are delegated to {@link GroundTruthAuditReportBuilder}.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see GroundTruthAuditReportBuilder
 */
public class GroundTruthAuditGenerator {

    /**
     * Data structure holding descriptive statistics, drift onset, and attribution for a single state variable.
     */
    public static class VariableDriftAnalysis {
        public final String variableName;
        public final double mape;
        public final String status;
        public final String driftOnsetYear;
        public final String peakDiscrepancy;
        public final String temporalPattern;
        public final String suspectEngine;
        public final int sampleCount;
        public final double meanObs;
        public final double meanSim;
        public final double stdSim;
        public final double medianSim;
        public final double q1Sim;
        public final double q3Sim;
        public final double cohenD;
        public final String significance;
        public final double pearsonR;
        public final double theilUM;
        public final double theilUS;
        public final double theilUC;
        public final double logGrowthCorrelation;
        public final double initialLevelOffsetPct;
        public final boolean isTier1Biophysical;

        /**
         * Backward-compatible constructor defaulting advanced metrics.
         */
        public VariableDriftAnalysis(String variableName, double mape, String status,
                                     String driftOnsetYear, String peakDiscrepancy,
                                     String temporalPattern, String suspectEngine,
                                     int sampleCount, double meanObs, double meanSim,
                                     double stdSim, double medianSim, double q1Sim,
                                     double q3Sim, double cohenD, String significance) {
            this(variableName, mape, status, driftOnsetYear, peakDiscrepancy, temporalPattern,
                    suspectEngine, sampleCount, meanObs, meanSim, stdSim, medianSim, q1Sim,
                    q3Sim, cohenD, significance, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, false);
        }

        /**
         * Constructs a complete variable drift analysis record with Theil inequality decomposition
         * and scale-invariant correlation metrics.
         */
        public VariableDriftAnalysis(String variableName, double mape, String status,
                                     String driftOnsetYear, String peakDiscrepancy,
                                     String temporalPattern, String suspectEngine,
                                     int sampleCount, double meanObs, double meanSim,
                                     double stdSim, double medianSim, double q1Sim,
                                     double q3Sim, double cohenD, String significance,
                                     double pearsonR, double theilUM, double theilUS,
                                     double theilUC, double logGrowthCorrelation,
                                     double initialLevelOffsetPct, boolean isTier1Biophysical) {
            this.variableName = variableName;
            this.mape = mape;
            this.status = status;
            this.driftOnsetYear = driftOnsetYear;
            this.peakDiscrepancy = peakDiscrepancy;
            this.temporalPattern = temporalPattern;
            this.suspectEngine = suspectEngine;
            this.sampleCount = sampleCount;
            this.meanObs = meanObs;
            this.meanSim = meanSim;
            this.stdSim = stdSim;
            this.medianSim = medianSim;
            this.q1Sim = q1Sim;
            this.q3Sim = q3Sim;
            this.cohenD = cohenD;
            this.significance = significance;
            this.pearsonR = pearsonR;
            this.theilUM = theilUM;
            this.theilUS = theilUS;
            this.theilUC = theilUC;
            this.logGrowthCorrelation = logGrowthCorrelation;
            this.initialLevelOffsetPct = initialLevelOffsetPct;
            this.isTier1Biophysical = isTier1Biophysical;
        }

        /**
         * Formats the 95% normal confidence interval \([\mu_{\text{sim}} - 1.96\,\text{SE}, \mu_{\text{sim}} + 1.96\,\text{SE}]\).
         *
         * @return formatted 95% confidence interval string
         */
        public String getCi95Display() {
            if (sampleCount <= 1 || stdSim <= 1e-9) {
                return String.format(Locale.US, "[%,.2f ; %,.2f]", meanSim, meanSim);
            }
            double se = stdSim / Math.sqrt(sampleCount);
            double moe = 1.96 * se;
            return String.format(Locale.US, "[%,.2f ; %,.2f]", meanSim - moe, meanSim + moe);
        }
    }

    /**
     * Synthesis result representing markdown text, earliest divergence year, and explanation summary.
     */
    public record AuditResult(
            String reportMarkdown,
            int divergenceYear,
            String explanationSummary
    ) {}

    /**
     * Extracts numerical telemetry value from a simulation snapshot.
     *
     * @param snap   snapshot instance
     * @param metric metric descriptor or key
     * @param year   calendar year
     * @return scalar telemetry value
     */
    public static double extractValue(SimulationRunRecord.MetricSnapshot snap, String metric, int year) {
        if (metric == null || snap == null) return 0.0;
        MetricDescriptor desc = MetricRegistry.getInstance().getDescriptorByName(metric);
        String id = desc != null ? desc.getId() : null;
        if (id == null) {
            MetricDescriptor descDirect = MetricRegistry.getInstance().getDescriptor(metric);
            if (descDirect != null) id = descDirect.getId();
        }
        if (id == null) id = metric;

        // 1. Direct hit or canonical alias hit in precomputed metricsMap (if present and non-NaN)
        if (snap.getMetricsMap() != null) {
            Double directVal = snap.getMetricsMap().get(id);
            if (directVal != null && !Double.isNaN(directVal)) {
                return GroundTruthStatisticsKernel.normalizeExtractedMetric(id, directVal);
            }
            for (String alias : GroundTruthStatisticsKernel.resolveMetricAliases(id)) {
                Double aliasVal = snap.getMetricsMap().get(alias);
                if (aliasVal != null && !Double.isNaN(aliasVal)) {
                    return GroundTruthStatisticsKernel.normalizeExtractedMetric(id, aliasVal);
                }
            }
        }

        // 2. Physical snapshot state properties (population, technology, food, stability)
        switch (id) {
            case "worldPopulation", "population", "regionalPopulation" -> {
                return (double) snap.getPopulation();
            }
            case "avgTechLevel", "techLevel", "avgTech" -> {
                return snap.getAvgTech();
            }
            case "foodPerCapita", "food" -> {
                return snap.getFood();
            }
            case "asabiyyahSocialCohesion", "asabiyyah", "stability" -> {
                return snap.getStability() > 1.0 ? snap.getStability() / 100.0 : snap.getStability();
            }
        }

        // Strict recorded-only extraction: unsubscribed or unrecorded variables return NaN (never fabricated by calendar formulas)
        return Double.NaN;
    }

    /**
     * Resolves simulation engine module responsible for a given macro variable.
     *
     * @param varName variable identifier
     * @return engine module name
     */
    public static String getEngineModuleForVariable(String varName) {
        if (varName == null) return "H3SimulationEngine";
        return switch (varName) {
            case "worldPopulation", "population", "regionalPopulation" -> "DemographicEngine";
            case "grossWorldProduct", "gdp", "builtCapital", "realUnskilledWage" -> "SociologyEngine (Capital)";
            case "primaryEnergy", "energyCaptured", "co2Concentration", "fossilSharePrimaryEnergy" -> "EcologyEngine (Biomass)";
            case "urbanizationRate" -> "SettlementEngine";
            case "literacyRate" -> "CulturalSociologyEngine";
            case "currencyDebasement", "sociopoliticalInstability" -> "InstitutionalEngine";
            default -> "H3SimulationEngine";
        };
    }

    /**
     * Computes statistical drift parameters for a single state variable.
     *
     * @param label      display label
     * @param benchKey   canonical benchmark key
     * @param benchmark  empirical benchmark map
     * @param timeSeries simulation telemetry snapshots
     * @param method     interpolation method
     * @return drift analysis record
     */
    public static VariableDriftAnalysis computeVariableDrift(String label, String benchKey, Map<Integer, Double> benchmark,
                                                             Map<Integer, SimulationRunRecord.MetricSnapshot> timeSeries,
                                                             HistoricalValidationKernel.InterpolationMethod method) {
        return computeVariableDrift(label, benchKey, benchmark, timeSeries, method, 1.0);
    }

    /**
     * Computes statistical drift parameters for a single state variable with optional spatial window scaling.
     *
     * @param label                        display label
     * @param benchKey                     canonical benchmark key
     * @param benchmark                    empirical benchmark map
     * @param timeSeries                   simulation telemetry snapshots
     * @param method                       interpolation method
     * @param regionalExtensiveScaleFactor spatial window extensive scale factor (1.0 for planetary simulations)
     * @return drift analysis record
     */
    public static VariableDriftAnalysis computeVariableDrift(String label, String benchKey, Map<Integer, Double> benchmark,
                                                             Map<Integer, SimulationRunRecord.MetricSnapshot> timeSeries,
                                                             HistoricalValidationKernel.InterpolationMethod method,
                                                             double regionalExtensiveScaleFactor) {
        return computeVariableDrift(label, benchKey, benchmark, timeSeries, method, regionalExtensiveScaleFactor, null);
    }

    /**
     * Computes statistical drift parameters for a single state variable, extracting true empirical regional
     * ground-truth values from 2D HYDE 3.4 rasters whenever {@code windowBounds} specifies a sub-global region.
     *
     * @param label                        display label
     * @param benchKey                     canonical benchmark key
     * @param benchmark                    empirical benchmark map
     * @param timeSeries                   simulation telemetry snapshots
     * @param method                       interpolation method
     * @param regionalExtensiveScaleFactor spatial realm indicator (&lt; 0.0 for non-Earth)
     * @param windowBounds                 optional {@code double[]{minLat, maxLat, minLng, maxLng}} for regional audits
     * @return drift analysis record
     */
    public static VariableDriftAnalysis computeVariableDrift(String label, String benchKey, Map<Integer, Double> benchmark,
                                                             Map<Integer, SimulationRunRecord.MetricSnapshot> timeSeries,
                                                             HistoricalValidationKernel.InterpolationMethod method,
                                                             double regionalExtensiveScaleFactor,
                                                             double[] windowBounds) {
        if (regionalExtensiveScaleFactor < 0.0) {
            return new VariableDriftAnalysis(label, 0.0, "⚪ Non-Earth Realm", "—", "—", "—", getEngineModuleForVariable(label), 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "ns");
        }

        if (windowBounds != null && RegionalRasterBenchmarkExtractor.isUnrasteredExtensiveGlobalMetric(benchKey)) {
            return new VariableDriftAnalysis(label, 0.0, "⚪ Regional (No 2D Ground-Truth Raster)", "—", "—", "—", getEngineModuleForVariable(label), 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "ns");
        }

        if (timeSeries == null || timeSeries.isEmpty()) {
            return new VariableDriftAnalysis(label, 0.0, "⚪ N/A", "—", "—", "—", getEngineModuleForVariable(label), 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "ns");
        }

        List<Integer> simYears = new ArrayList<>(timeSeries.keySet());
        Collections.sort(simYears);
        if (simYears.isEmpty()) {
            return new VariableDriftAnalysis(label, 0.0, "⚪ N/A", "—", "—", "—", getEngineModuleForVariable(label), 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "ns");
        }

        if (method == null) {
            method = HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC;
        }

        List<Integer> sampleYears = new ArrayList<>();
        int sampleStep = Math.max(1, simYears.size() / 80);
        for (int i = 0; i < simYears.size(); i += sampleStep) {
            sampleYears.add(simYears.get(i));
        }
        if (!sampleYears.contains(simYears.get(simYears.size() - 1))) {
            sampleYears.add(simYears.get(simYears.size() - 1));
        }

        double sumAbsError = 0.0;
        int count = 0;
        Integer driftYear = null;
        int peakYear = sampleYears.get(0);
        double maxError = 0.0;
        List<Double> errorList = new ArrayList<>();
        List<Double> obsList = new ArrayList<>();
        List<Double> simList = new ArrayList<>();

        Map<Integer, Double> regionalRasterSeries = (windowBounds != null && RegionalRasterBenchmarkExtractor.supportsRegionalRaster(benchKey))
                ? RegionalRasterBenchmarkExtractor.extractRegionalSeries(benchKey, windowBounds, sampleYears)
                : Collections.emptyMap();

        for (int yr : sampleYears) {
            double obs = !regionalRasterSeries.isEmpty()
                    ? regionalRasterSeries.getOrDefault(yr, Double.NaN)
                    : HistoricalValidationKernel.getInterpolatedBenchmarkValue(benchKey, yr, method);
            if (Double.isNaN(obs)) continue;

            double sim = extractValue(timeSeries.get(yr), benchKey, yr);
            if (Double.isNaN(sim) || Double.isInfinite(sim)) {
                continue;
            }
            if ("milankovitchInsolation".equals(benchKey) && sim <= 0.0) {
                continue;
            }
            sim = GroundTruthStatisticsKernel.normalizeSimulatedUnits(benchKey, sim);

            boolean isPercentageVariable = "temperatureAnomaly".equals(benchKey)
                    || "fossilSharePrimaryEnergy".equals(benchKey)
                    || "urbanizationRate".equals(benchKey)
                    || "literacyRate".equals(benchKey)
                    || "currencyDebasement".equals(benchKey)
                    || "sovereignDebtBurden".equals(benchKey)
                    || "soilErosionRate".equals(benchKey)
                    || "deforestationRate".equals(benchKey);
            double errPct;
            if (Math.abs(obs) < 1e-4) {
                errPct = (Math.abs(sim) < 1e-3) ? 0.0 : (isPercentageVariable ? Math.min(100.0, Math.abs(sim)) : Math.min(100.0, Math.abs(sim) * 100.0));
            } else {
                double denom = isPercentageVariable ? Math.max(Math.abs(obs), 5.0) : Math.abs(obs);
                errPct = Math.abs(sim - obs) / denom * 100.0;
            }

            sumAbsError += errPct;
            count++;
            errorList.add(errPct);
            obsList.add(obs);
            simList.add(sim);

            double allowedTolerancePct = HistoricalValidationKernel.computeContinuousTemporalTolerance(yr) * 100.0;
            if (errPct > allowedTolerancePct && driftYear == null) {
                driftYear = yr;
            }
            if (errPct > maxError) {
                maxError = errPct;
                peakYear = yr;
            }
        }

        if (count == 0) {
            return new VariableDriftAnalysis(label, 0.0, "⚪ N/A", "—", "—", "—", getEngineModuleForVariable(label),
                    0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "ns", 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, isTier1BiophysicalVariable(benchKey));
        }

        return GroundTruthStatisticsKernel.finalizeDriftAnalysis(label, benchKey, sumAbsError, count, driftYear, peakYear, maxError, errorList, obsList, simList);
    }

    /**
     * Computes pairwise statistical comparison over an aligned list of intersection years
     * between any two trajectories (empirical, predictive, or simulated).
     *
     * @param label           variable display label
     * @param benchKey        canonical variable key
     * @param commonYears     ordered list of years inside the date intersection
     * @param refExtractor    reference value extractor for a given year (returns NaN if absent)
     * @param targetExtractor target value extractor for a given year (returns NaN if absent)
     * @return pairwise {@link VariableDriftAnalysis}
     */
    public static VariableDriftAnalysis computePairwiseDrift(
            String label,
            String benchKey,
            List<Integer> commonYears,
            java.util.function.IntToDoubleFunction refExtractor,
            java.util.function.IntToDoubleFunction targetExtractor) {

        if (commonYears == null || commonYears.isEmpty() || refExtractor == null || targetExtractor == null) {
            return new VariableDriftAnalysis(label, 0.0, "⚪ N/A", "—", "—", "—", getEngineModuleForVariable(label),
                    0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "ns", 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, isTier1BiophysicalVariable(benchKey));
        }

        double sumAbsError = 0.0;
        int count = 0;
        Integer driftYear = null;
        int peakYear = commonYears.get(0);
        double maxError = 0.0;
        List<Double> errorList = new ArrayList<>();
        List<Double> obsList = new ArrayList<>();
        List<Double> simList = new ArrayList<>();

        boolean isPercentageVariable = "temperatureAnomaly".equals(benchKey)
                || "fossilSharePrimaryEnergy".equals(benchKey)
                || "urbanizationRate".equals(benchKey)
                || "literacyRate".equals(benchKey)
                || "currencyDebasement".equals(benchKey)
                || "sovereignDebtBurden".equals(benchKey)
                || "soilErosionRate".equals(benchKey)
                || "deforestationRate".equals(benchKey);

        for (int yr : commonYears) {
            double ref = refExtractor.applyAsDouble(yr);
            double tgt = targetExtractor.applyAsDouble(yr);
            if (Double.isNaN(ref) || Double.isNaN(tgt) || Double.isInfinite(ref) || Double.isInfinite(tgt)) {
                continue;
            }
            double errPct;
            if (Math.abs(ref) < 1e-4) {
                errPct = (Math.abs(tgt) < 1e-3) ? 0.0 : (isPercentageVariable ? Math.min(100.0, Math.abs(tgt)) : Math.min(100.0, Math.abs(tgt) * 100.0));
            } else {
                double denom = isPercentageVariable ? Math.max(Math.abs(ref), 5.0) : Math.abs(ref);
                errPct = Math.abs(tgt - ref) / denom * 100.0;
            }
            sumAbsError += errPct;
            count++;
            errorList.add(errPct);
            obsList.add(ref);
            simList.add(tgt);

            double allowedTolerancePct = HistoricalValidationKernel.computeContinuousTemporalTolerance(yr) * 100.0;
            if (errPct > allowedTolerancePct && driftYear == null) {
                driftYear = yr;
            }
            if (errPct > maxError) {
                maxError = errPct;
                peakYear = yr;
            }
        }

        if (count == 0) {
            return new VariableDriftAnalysis(label, 0.0, "⚪ N/A", "—", "—", "—", getEngineModuleForVariable(label),
                    0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "ns", 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, isTier1BiophysicalVariable(benchKey));
        }

        return GroundTruthStatisticsKernel.finalizeDriftAnalysis(label, benchKey, sumAbsError, count, driftYear, peakYear, maxError, errorList, obsList, simList);
    }

    /**
     * Determines whether a given benchmark key corresponds to a Tier 1 direct biophysical observable.
     *
     * @param benchKey canonical benchmark identifier
     * @return {@code true} if Tier 1 biophysical observable, {@code false} if Tier 2 macroeconomic/institutional
     */
    public static boolean isTier1BiophysicalVariable(String benchKey) {
        return GroundTruthStatisticsKernel.isTier1BiophysicalVariable(benchKey);
    }

    public static double normalizeSimulatedUnits(String benchKey, double sim) {
        return GroundTruthStatisticsKernel.normalizeSimulatedUnits(benchKey, sim);
    }

    /**
     * Generates a unified comparative audit report across any selection of scenarios
     * (historical ground truth, prospective SSP baselines, and/or simulated runs)
     * strictly evaluated over their exact date intersection across all available variables.
     */
    public static <T extends ScenarioBenchmarkItem> AuditResult generateUnifiedComparativeReport(
            List<T> selectedItems,
            HistoricalValidationKernel.InterpolationMethod method,
            Function<? super T, SimulationRunRecord> runResolver,
            RootCauseAnalyzer analyzer,
            SimulationRunRepository runRepository) {
        return GroundTruthAuditReportBuilder.generateUnifiedComparativeReport(
                selectedItems, method, runResolver, analyzer, runRepository);
    }

    /**
     * Generates a multi-scenario ground truth audit against empirical historical reality.
     */
    public static <T extends ScenarioBenchmarkItem> AuditResult generateMultiScenarioGroundTruthAudit(
            List<T> simulatedSelected,
            HistoricalValidationKernel.InterpolationMethod method,
            Function<? super T, SimulationRunRecord> runResolver) {
        return GroundTruthAuditReportBuilder.generateMultiScenarioGroundTruthAudit(simulatedSelected, method, runResolver);
    }

    /**
     * Generates a comparative report across pairs of simulated scenarios.
     */
    public static <T extends ScenarioBenchmarkItem> AuditResult generateInterScenarioComparativeReport(
            List<T> simulatedSelected,
            RootCauseAnalyzer analyzer,
            Function<? super T, SimulationRunRecord> runResolver,
            SimulationRunRepository runRepository) {
        return GroundTruthAuditReportBuilder.generateInterScenarioComparativeReport(simulatedSelected, analyzer, runResolver, runRepository);
    }
}
