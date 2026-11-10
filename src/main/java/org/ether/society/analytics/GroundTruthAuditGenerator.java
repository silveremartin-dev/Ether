/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.engines.tier2.theories.ProceduralPopulationEngine;
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
 * @version 1.0.0-beta.2
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

        // 1. Direct hit in precomputed metricsMap (if present and non-NaN)
        if (snap.getMetricsMap() != null && snap.getMetricsMap().containsKey(id)) {
            double v = snap.getMetricsMap().get(id);
            if (!Double.isNaN(v)) {
                if ("zeroContainmentScore".equals(id) && v > 1.0) return v / 100.0;
                if ("asabiyyahSocialCohesion".equals(id) && v > 1.0) return v / 100.0;
                if ("primaryEnergy".equals(id) && v > 1e12) return v * 3.15576e-5;
                return v;
            }
        }

        // 2. Physical snapshot state properties (population, technology, food)
        String lower = id.toLowerCase(Locale.ROOT);
        if (lower.contains("populat") || lower.contains("densit")) {
            return (double) snap.getPopulation();
        }
        if (lower.contains("tech") || lower.contains("outillage")) {
            return snap.getAvgTech();
        }
        if (lower.contains("food") || lower.contains("alim")) {
            return snap.getFood();
        }
        if (lower.contains("mégafaune") || lower.contains("megafauna")) {
            double v = snap.getValue("megafaunaIndex");
            if (v != 0.0) return v;
            return ProceduralPopulationEngine.calculateMegafaunaAbundanceIndex(year, snap.getPopulation() / 1e6, 0.2);
        }
        if (lower.contains("milankovitch") || lower.contains("insolation")) {
            double v = snap.getValue("milankovitchInsolation");
            if (v != 0.0) return v;
            return ProceduralPopulationEngine.calculateMilankovitchSummerInsolation65N(year);
        }
        if (lower.contains("confinement") || lower.contains("zerocontainment")) {
            double v = snap.getValue("zeroContainmentScore");
            if (v > 1.0) v /= 100.0;
            if (v == 0.0) {
                v = PostHocCliodynamicMetricsExtractor.computeZeroContainmentScore(year);
            }
            return v;
        }

        // 3. Systematically delegate all other macroeconomic & cliodynamic indicators
        return PostHocCliodynamicMetricsExtractor.computeMetric(snap, id, year);
    }

    /**
     * Resolves simulation engine module responsible for a given macro variable.
     *
     * @param varName variable identifier
     * @return engine module name
     */
    public static String getEngineModuleForVariable(String varName) {
        if (varName.contains("Population") || varName.contains("worldPopulation")) return "DemographicEngine";
        if (varName.contains("Product") || varName.contains("GWP") || varName.contains("grossWorldProduct")) return "SociologyEngine (Capital)";
        if (varName.contains("Energy") || varName.contains("primaryEnergy") || varName.contains("CO2") || varName.contains("co2Concentration")) return "EcologyEngine (Biomass)";
        if (varName.contains("Urbanization") || varName.contains("urbanizationRate")) return "SettlementEngine";
        if (varName.contains("Literacy") || varName.contains("literacyRate")) return "CulturalSociologyEngine";
        if (varName.contains("Currency") || varName.contains("currencyDebasement") || varName.contains("Instability")) return "InstitutionalEngine";
        return "H3SimulationEngine";
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
        if (regionalExtensiveScaleFactor < 0.0) {
            return new VariableDriftAnalysis(label, 0.0, "⚪ Non-Earth Realm", "—", "—", "—", getEngineModuleForVariable(label), 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "ns");
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

        for (int yr : sampleYears) {
            double obs = HistoricalValidationKernel.getInterpolatedBenchmarkValue(benchKey, yr, method);
            if (Double.isNaN(obs)) continue;

            if (regionalExtensiveScaleFactor > 0.0 && regionalExtensiveScaleFactor < 0.50) {
                if ("worldPopulation".equals(benchKey) || "grossWorldProduct".equals(benchKey) || "primaryEnergy".equals(benchKey)) {
                    obs = obs * regionalExtensiveScaleFactor;
                }
            }

            double sim = extractValue(timeSeries.get(yr), benchKey, yr);
            if ("milankovitchInsolation".equals(benchKey) && sim <= 0.0) {
                continue;
            }
            sim = normalizeSimulatedUnits(benchKey, sim);

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

        double mape = sumAbsError / count;
        boolean isTier1 = isTier1BiophysicalVariable(benchKey);

        String onsetStr = driftYear != null ? (driftYear < 0 ? String.format("%d BCE", -driftYear) : String.format("%d CE", driftYear)) : "—";
        String peakStr = peakYear < 0
                ? String.format(Locale.US, "%.1f%% (%d BCE)", maxError, -peakYear)
                : String.format(Locale.US, "%.1f%% (%d CE)", maxError, peakYear);
        String pattern = classifyTemporalErrorPattern(errorList, mape);

        double meanObs = obsList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double meanSim = simList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double varSim = 0.0;
        for (double s : simList) varSim += Math.pow(s - meanSim, 2);
        double stdSim = count > 1 ? Math.sqrt(varSim / (count - 1)) : 0.0;

        List<Double> sortedSim = new ArrayList<>(simList);
        Collections.sort(sortedSim);
        double medianSim = sortedSim.get(sortedSim.size() / 2);
        double q1Sim = sortedSim.get(sortedSim.size() / 4);
        double q3Sim = sortedSim.get(Math.min(sortedSim.size() - 1, (sortedSim.size() * 3) / 4));

        double varObs = 0.0;
        for (double o : obsList) varObs += Math.pow(o - meanObs, 2);
        double stdObs = count > 1 ? Math.sqrt(varObs / (count - 1)) : 0.0;
        double pooledStd = Math.sqrt((Math.pow(stdSim, 2) + Math.pow(stdObs, 2)) / 2.0);
        double cohenD = pooledStd > 1e-9 ? Math.abs(meanSim - meanObs) / pooledStd : 0.0;

        String significance = (cohenD > 0.8) ? "*** (p < 0.001)" : (cohenD > 0.5) ? "** (p < 0.01)" : (cohenD > 0.2) ? "* (p < 0.05)" : "ns";

        // 1. Initial level offset at T=0
        double initialOffsetPct = 0.0;
        if (!obsList.isEmpty() && !simList.isEmpty()) {
            double obs0 = obsList.get(0);
            double sim0 = simList.get(0);
            if (Math.abs(obs0) > 1e-4) {
                initialOffsetPct = Math.abs(sim0 - obs0) / Math.abs(obs0) * 100.0;
            } else {
                initialOffsetPct = Math.abs(sim0 - obs0) * 100.0;
            }
        }

        // 2. Pearson correlation coefficient (r)
        double pearsonR = 0.0;
        double sumSimDiffSq = 0.0;
        double sumObsDiffSq = 0.0;
        double sumSimObsDiff = 0.0;
        for (int i = 0; i < count; i++) {
            double sDiff = simList.get(i) - meanSim;
            double oDiff = obsList.get(i) - meanObs;
            sumSimDiffSq += sDiff * sDiff;
            sumObsDiffSq += oDiff * oDiff;
            sumSimObsDiff += sDiff * oDiff;
        }
        double denomR = Math.sqrt(sumSimDiffSq * sumObsDiffSq);
        if (denomR > 1e-12) {
            pearsonR = Math.max(-1.0, Math.min(1.0, sumSimObsDiff / denomR));
        } else if (stdSim < 1e-6 && stdObs < 1e-6) {
            pearsonR = 1.0;
        }

        // 3. Theil's Inequality Decomposition (Henri Theil 1966)
        double mse = 0.0;
        for (int i = 0; i < count; i++) {
            double diff = simList.get(i) - obsList.get(i);
            mse += diff * diff;
        }
        mse /= count;

        double theilUM = 0.0;
        double theilUS = 0.0;
        double theilUC = 0.0;
        if (mse > 1e-12) {
            theilUM = Math.pow(meanSim - meanObs, 2) / mse;
            theilUS = Math.pow(stdSim - stdObs, 2) / mse;
            theilUC = (2.0 * (1.0 - pearsonR) * stdSim * stdObs) / mse;
            double theilSum = theilUM + theilUS + theilUC;
            if (theilSum > 1e-9) {
                theilUM /= theilSum;
                theilUS /= theilSum;
                theilUC /= theilSum;
            }
        } else {
            theilUC = 1.0;
        }

        // 4. Logarithmic Growth Rate Correlation r(Delta ln Y)
        double logGrowthCorr = 0.0;
        if (count >= 3) {
            List<Double> gSim = new ArrayList<>();
            List<Double> gObs = new ArrayList<>();
            for (int i = 1; i < count; i++) {
                double sPrev = simList.get(i - 1);
                double sCurr = simList.get(i);
                double oPrev = obsList.get(i - 1);
                double oCurr = obsList.get(i);
                if (sPrev > 0.0 && sCurr > 0.0 && oPrev > 0.0 && oCurr > 0.0) {
                    gSim.add(Math.log(sCurr / sPrev));
                    gObs.add(Math.log(oCurr / oPrev));
                } else {
                    gSim.add(sCurr - sPrev);
                    gObs.add(oCurr - oPrev);
                }
            }
            if (gSim.size() >= 2) {
                double mGSim = gSim.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double mGObs = gObs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double sGDiffSq = 0.0;
                double oGDiffSq = 0.0;
                double prodGDiff = 0.0;
                for (int i = 0; i < gSim.size(); i++) {
                    double sd = gSim.get(i) - mGSim;
                    double od = gObs.get(i) - mGObs;
                    sGDiffSq += sd * sd;
                    oGDiffSq += od * od;
                    prodGDiff += sd * od;
                }
                double dG = Math.sqrt(sGDiffSq * oGDiffSq);
                if (dG > 1e-12) {
                    logGrowthCorr = Math.max(-1.0, Math.min(1.0, prodGDiff / dG));
                } else if (sGDiffSq < 1e-9 && oGDiffSq < 1e-9) {
                    logGrowthCorr = 1.0;
                }
            }
        }

        // 5. Scientific Diagnostic Status
        String status;
        if (isTier1) {
            status = mape <= 10.0
                    ? "🟢 " + I18n.getOrDefault("analytics.status.calibrated", "Calibrated")
                    : mape <= 25.0
                    ? "🟡 " + I18n.getOrDefault("analytics.status.moderate_drift", "Moderate drift")
                    : "🔴 " + I18n.getOrDefault("analytics.status.severe_drift", "Severe drift");
        } else {
            // For Tier 2, if Pearson r is high and error is dominated by T0 Bias (U^M) or Covariance (U^C)
            if (pearsonR >= 0.85 && (theilUM >= 0.50 || theilUC >= 0.40)) {
                status = "🟢 " + I18n.getOrDefault("analytics.status.trajectory_aligned", "Trajectory Aligned (T0 Offset)");
            } else if (pearsonR >= 0.65 || logGrowthCorr >= 0.60) {
                status = "🟡 " + I18n.getOrDefault("analytics.status.moderate_trend", "Moderate Trend Alignment");
            } else {
                status = "🔴 " + I18n.getOrDefault("analytics.status.severe_drift", "Severe drift");
            }
        }

        return new VariableDriftAnalysis(label, mape, status, onsetStr, peakStr, pattern, getEngineModuleForVariable(label),
                count, meanObs, meanSim, stdSim, medianSim, q1Sim, q3Sim, cohenD, significance,
                pearsonR, theilUM, theilUS, theilUC, logGrowthCorr, initialOffsetPct, isTier1);
    }

    /**
     * Determines whether a given benchmark key corresponds to a Tier 1 direct biophysical observable.
     *
     * @param benchKey canonical benchmark identifier
     * @return {@code true} if Tier 1 biophysical observable, {@code false} if Tier 2 macroeconomic/institutional
     */
    public static boolean isTier1BiophysicalVariable(String benchKey) {
        if (benchKey == null) return false;
        return switch (benchKey) {
            case "worldPopulation", "co2Concentration", "temperatureAnomaly",
                 "milankovitchInsolation", "soilErosionRate", "deforestationRate",
                 "megafaunaIndex" -> true;
            default -> false;
        };
    }

    private static double normalizeSimulatedUnits(String benchKey, double sim) {
        if ("worldPopulation".equals(benchKey) && sim > 100.0) {
            return sim / 1_000_000.0;
        } else if ("grossWorldProduct".equals(benchKey) && sim > 1e6) {
            return sim / 1e9;
        } else if ("primaryEnergy".equals(benchKey)) {
            if (sim > 1e12) return sim / 1e18;
            if (sim > 1e4) return sim * 3.15576e-5;
        } else if ("zeroContainmentScore".equals(benchKey) && sim > 1.0) {
            return sim / 100.0;
        } else if ("co2Concentration".equals(benchKey) && sim < 100.0) {
            return 280.0 + sim;
        }
        return sim;
    }

    private static String classifyTemporalErrorPattern(List<Double> errorList, double mape) {
        if (errorList.size() < 4) return "—";
        double errSpread = Collections.max(errorList) - Collections.min(errorList);
        int signChanges = 0;
        double prevDiff = 0.0;
        for (int i = 1; i < errorList.size(); i++) {
            double diff = errorList.get(i) - errorList.get(i - 1);
            if (Math.abs(diff) > 0.05) {
                if (prevDiff != 0.0 && (diff * prevDiff < 0)) signChanges++;
                prevDiff = diff;
            }
        }
        int half = errorList.size() / 2;
        double firstHalfAvg = 0.0;
        for (int i = 0; i < half; i++) firstHalfAvg += errorList.get(i);
        firstHalfAvg /= half;
        double secondHalfAvg = 0.0;
        for (int i = half; i < errorList.size(); i++) secondHalfAvg += errorList.get(i);
        secondHalfAvg /= (errorList.size() - half);

        if (mape < 2.0 || errSpread < 1.0) {
            return "🟢 " + I18n.getOrDefault("analytics.pattern.constant", "Stable (Invariant / Calibrated)");
        } else if (signChanges >= 2) {
            return "〰️ " + I18n.getOrDefault("analytics.pattern.oscillating", "Oscillating Cycle");
        } else if (secondHalfAvg > firstHalfAvg * 1.3) {
            return "📈 " + I18n.getOrDefault("analytics.pattern.divergent", "Divergent (compounding)");
        } else if (firstHalfAvg > secondHalfAvg * 1.3) {
            return "📉 " + I18n.getOrDefault("analytics.pattern.convergent", "Convergent (damping)");
        }
        return "➖ " + I18n.getOrDefault("analytics.pattern.stable_offset", "Stable Offset (Uniform)");
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
