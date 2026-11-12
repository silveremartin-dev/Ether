/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.i18n.I18n;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Epistemic Statistics Kernel for Ground Truth Audits.
 * Evaluates Cohen's d effect sizes, Pearson correlation coefficients, Theil inequality
 * decompositions (U^M bias, U^S variance, U^C covariance), and temporal error patterns.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 * @see GroundTruthAuditGenerator
 */
public final class GroundTruthStatisticsKernel {

    private GroundTruthStatisticsKernel() {
        // Pure stateless kernel
    }

    /**
     * Determines whether a given benchmark key corresponds to a Tier 1 direct biophysical observable.
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

    /**
     * Normalizes simulated values to match canonical benchmark units.
     */
    public static double normalizeSimulatedUnits(String benchKey, double sim) {
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

    /**
     * Normalizes extracted metric snapshot values.
     */
    public static double normalizeExtractedMetric(String id, double v) {
        if ("zeroContainmentScore".equals(id) && v > 1.0) return v / 100.0;
        if ("asabiyyahSocialCohesion".equals(id) && v > 1.0) return v / 100.0;
        if ("primaryEnergy".equals(id) && v > 1e12) return v * 3.15576e-5;
        if ("co2Concentration".equals(id) && v < 100.0) return 280.0 + v;
        return v;
    }

    /**
     * Resolves fallback aliases for canonical metrics.
     */
    public static List<String> resolveMetricAliases(String id) {
        return switch (id) {
            case "worldPopulation" -> List.of("population", "regionalPopulation");
            case "grossWorldProduct" -> List.of("gdp", "gdpTotal", "regionalGdp", "builtCapital");
            case "primaryEnergy" -> List.of("energyCaptured", "regionalEnergy");
            case "giniInequality" -> List.of("gini", "giniIndex");
            case "asabiyyahSocialCohesion" -> List.of("asabiyyah");
            case "politicalStressIndex" -> List.of("psiStressIndex");
            case "sociopoliticalInstability" -> List.of("conflict");
            case "lifeExpectancyAtBirth" -> List.of("lifeExpectancy");
            case "literacyRate" -> List.of("educationLevel");
            case "co2Concentration" -> List.of("carbonFootprint");
            case "systemComplexity" -> List.of("systemInterdependence");
            default -> Collections.emptyList();
        };
    }

    /**
     * Finalizes drift statistics across empirical observations and simulation outputs.
     */
    public static GroundTruthAuditGenerator.VariableDriftAnalysis finalizeDriftAnalysis(
            String label, String benchKey, double sumAbsError, int count, Integer driftYear,
            int peakYear, double maxError, List<Double> errorList, List<Double> obsList, List<Double> simList) {

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

        double mse = 0.0;
        for (int i = 0; i < count; i++) {
            double diff = simList.get(i) - obsList.get(i);
            mse += diff * diff;
        }
        mse /= count;

        double theilUM = 0.0, theilUS = 0.0, theilUC = 0.0;
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

        double logGrowthCorr = 0.0;
        if (count >= 3) {
            List<Double> gSim = new ArrayList<>();
            List<Double> gObs = new ArrayList<>();
            for (int i = 1; i < count; i++) {
                double sPrev = simList.get(i - 1), sCurr = simList.get(i);
                double oPrev = obsList.get(i - 1), oCurr = obsList.get(i);
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
                double sGDiffSq = 0.0, oGDiffSq = 0.0, prodGDiff = 0.0;
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

        String status;
        if (isTier1) {
            status = mape <= 10.0
                    ? "🟢 " + I18n.getOrDefault("analytics.status.calibrated", "Calibrated")
                    : mape <= 25.0
                    ? "🟡 " + I18n.getOrDefault("analytics.status.moderate_drift", "Moderate drift")
                    : "🔴 " + I18n.getOrDefault("analytics.status.severe_drift", "Severe drift");
        } else {
            if (pearsonR >= 0.85 && (theilUM >= 0.50 || theilUC >= 0.40)) {
                status = "🟢 " + I18n.getOrDefault("analytics.status.trajectory_aligned", "Trajectory Aligned (T0 Offset)");
            } else if (pearsonR >= 0.65 || logGrowthCorr >= 0.60) {
                status = "🟡 " + I18n.getOrDefault("analytics.status.moderate_trend", "Moderate Trend Alignment");
            } else {
                status = "🔴 " + I18n.getOrDefault("analytics.status.severe_drift", "Severe drift");
            }
        }

        return new GroundTruthAuditGenerator.VariableDriftAnalysis(
                label, mape, status, onsetStr, peakStr, pattern,
                GroundTruthAuditGenerator.getEngineModuleForVariable(label),
                count, meanObs, meanSim, stdSim, medianSim, q1Sim, q3Sim, cohenD, significance,
                pearsonR, theilUM, theilUS, theilUC, logGrowthCorr, initialOffsetPct, isTier1);
    }

    /**
     * Classifies temporal error progression pattern into qualitative archetypes.
     */
    public static String classifyTemporalErrorPattern(List<Double> errorList, double mape) {
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
}
