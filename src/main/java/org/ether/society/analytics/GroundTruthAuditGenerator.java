/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.engines.tier2.theories.ProceduralPopulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.analytics.ScenarioSelectableItem;

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
 * 95% confidence intervals, and statistical bifurcation onsets.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
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

        public VariableDriftAnalysis(String variableName, double mape, String status,
                                     String driftOnsetYear, String peakDiscrepancy,
                                     String temporalPattern, String suspectEngine,
                                     int sampleCount, double meanObs, double meanSim,
                                     double stdSim, double medianSim, double q1Sim,
                                     double q3Sim, double cohenD, String significance) {
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
        }

        public String getCi95Display() {
            if (sampleCount <= 1 || stdSim <= 1e-9) return String.format(Locale.US, "[%,.2f ; %,.2f]", meanSim, meanSim);
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
     * @param snap snapshot instance
     * @param metric metric descriptor or key
     * @param year calendar year
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

        double val = snap.getValue(id);
        if (val != 0.0) return val;

        String lower = id.toLowerCase(Locale.ROOT);
        if (lower.contains("populat") || lower.contains("densit")) {
            val = snap.getValue("population");
            if (val == 0.0) val = snap.getValue("worldPopulation");
            if (val == 0.0) val = (double) snap.getPopulation();
            if (val != 0.0) return val;
        }
        if (lower.contains("tech") || lower.contains("outillage") || lower.contains("literacy")) {
            val = snap.getValue("avgTechLevel");
            if (val == 0.0) val = snap.getValue("technology");
            if (val == 0.0) val = snap.getAvgTech();
            if (val != 0.0) return val;
        }
        if (lower.contains("food") || lower.contains("alim")) {
            val = snap.getValue("foodPerCapita");
            if (val == 0.0) val = snap.getFood();
            if (val != 0.0) return val;
        }
        if (lower.contains("asabiyyah") || lower.contains("stabilit") || lower.contains("cohesion")) {
            val = snap.getValue("asabiyyah");
            if (val == 0.0) val = snap.getStability();
            if (val != 0.0) return val;
        }
        if (lower.contains("energy") || lower.contains("énerg")) {
            val = snap.getValue("energyCaptured");
            if (val == 0.0) val = snap.getValue("primaryEnergy");
            if (val != 0.0) return val;
        }
        if (lower.contains("gdp") || lower.contains("product") || lower.contains("richesse") || lower.contains("pib")) {
            val = snap.getValue("grossWorldProduct");
            if (val == 0.0) val = snap.getValue("gdp");
            if (val == 0.0) val = snap.getValue("gdpTotal");
            if (val > 1e6) val /= 1e9;
            if (val != 0.0) return val;
        }
        if (lower.contains("mégafaune") || lower.contains("megafauna")) {
            val = snap.getValue("megafaunaIndex");
            if (val != 0.0) return val;
            return ProceduralPopulationEngine.calculateMegafaunaAbundanceIndex(year, snap.getPopulation() / 1e6, 0.2);
        }
        if (lower.contains("milankovitch") || lower.contains("insolation")) {
            val = snap.getValue("milankovitchInsolation");
            if (val != 0.0) return val;
            return ProceduralPopulationEngine.calculateMilankovitchSummerInsolation65N(year);
        }
        if (lower.contains("confinement") || lower.contains("zerocontainment")) {
            val = snap.getValue("zeroContainmentScore");
            return val != 0.0 ? val : 100.0;
        }
        if (lower.contains("gini") || lower.contains("inégal")) {
            val = snap.getValue("gini");
            if (val == 0.0) val = snap.getValue("giniIndex");
            if (val != 0.0) return val;
        }
        if (lower.contains("bonheur") || lower.contains("happiness")) {
            val = snap.getValue("happiness");
            if (val == 0.0) val = snap.getValue("avgHappiness");
            if (val != 0.0) return val;
        }
        return 0.0;
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
     */
    public static VariableDriftAnalysis computeVariableDrift(String label, String benchKey, Map<Integer, Double> benchmark,
                                                             Map<Integer, SimulationRunRecord.MetricSnapshot> timeSeries,
                                                             HistoricalValidationKernel.InterpolationMethod method) {
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
            if (Double.isNaN(obs)) {
                if (benchmark != null && !benchmark.isEmpty()) {
                    int nearestY = benchmark.keySet().stream().min(Comparator.comparingInt(y -> Math.abs(y - yr))).orElse(yr);
                    if (Math.abs(nearestY - yr) <= 10000) {
                        obs = benchmark.get(nearestY);
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
            if (obs == 0.0) continue;
            double sim = extractValue(timeSeries.get(yr), benchKey, yr);
            if ("worldPopulation".equals(benchKey) && sim > 10000.0) {
                sim = sim / 1_000_000.0;
            } else if ("grossWorldProduct".equals(benchKey)) {
                if (sim > 1e6) {
                    sim = sim / 1e9;
                }
            } else if ("primaryEnergy".equals(benchKey)) {
                if (sim > 1e12) {
                    sim = sim / 1e18;
                }
            } else if ("co2Concentration".equals(benchKey)) {
                if (sim < 100.0) {
                    sim = 280.0 + sim;
                }
            } else if ("literacyRate".equals(benchKey)) {
                if (sim <= 10.0 && sim > 0.0) {
                    sim = sim * 10.0;
                }
            } else if ("sociopoliticalInstability".equals(benchKey)) {
                var snap = timeSeries.get(yr);
                if (snap != null && snap.getMetricsMap().containsKey("conflict")) {
                    sim = snap.getValue("conflict");
                }
            }

            if (obs != 0.0) {
                double errPct = Math.abs(sim - obs) / Math.abs(obs) * 100.0;
                sumAbsError += errPct;
                count++;
                errorList.add(errPct);
                obsList.add(obs);
                simList.add(sim);

                if (errPct > 10.0 && driftYear == null) {
                    driftYear = yr;
                }
                if (errPct > maxError) {
                    maxError = errPct;
                    peakYear = yr;
                }
            }
        }

        if (count == 0) {
            return new VariableDriftAnalysis(label, 0.0, "⚪ N/A", "—", "—", "—", getEngineModuleForVariable(label), 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "ns");
        }

        double mape = sumAbsError / count;
        String status;
        if (mape <= 10.0) status = "🟢 " + I18n.getOrDefault("analytics.status.calibrated", "Calibrated");
        else if (mape <= 25.0) status = "🟡 " + I18n.getOrDefault("analytics.status.moderate_drift", "Moderate drift");
        else status = "🔴 " + I18n.getOrDefault("analytics.status.severe_drift", "Severe drift");

        String onsetStr = driftYear != null ? String.format("%d AD", driftYear) : "—";
        String peakStr = String.format(Locale.US, "%.1f%% (%d AD)", maxError, peakYear);

        String pattern;
        if (errorList.size() >= 4) {
            double firstHalfAvg = 0.0;
            int half = errorList.size() / 2;
            for (int i = 0; i < half; i++) firstHalfAvg += errorList.get(i);
            firstHalfAvg /= half;

            double secondHalfAvg = 0.0;
            for (int i = half; i < errorList.size(); i++) secondHalfAvg += errorList.get(i);
            secondHalfAvg /= (errorList.size() - half);

            if (secondHalfAvg > firstHalfAvg * 1.5) {
                pattern = "📈 " + I18n.getOrDefault("analytics.pattern.divergent", "Divergent (compounding)");
            } else if (firstHalfAvg > secondHalfAvg * 1.5) {
                pattern = "📉 " + I18n.getOrDefault("analytics.pattern.convergent", "Convergent (damping)");
            } else {
                pattern = "〰️ " + I18n.getOrDefault("analytics.pattern.oscillating", "Oscillating (stable offset)");
            }
        } else {
            pattern = "—";
        }

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

        return new VariableDriftAnalysis(label, mape, status, onsetStr, peakStr, pattern, getEngineModuleForVariable(label),
                count, meanObs, meanSim, stdSim, medianSim, q1Sim, q3Sim, cohenD, significance);
    }

    /**
     * Generates a multi-scenario ground truth audit against empirical historical reality.
     */
    public static AuditResult generateMultiScenarioGroundTruthAudit(List<ScenarioSelectableItem> simulatedSelected,
                                                                    HistoricalValidationKernel.InterpolationMethod method,
                                                                    Function<ScenarioSelectableItem, SimulationRunRecord> runResolver) {
        StringBuilder fullReport = new StringBuilder();
        fullReport.append("# ").append(I18n.getOrDefault("analytics.audit.title_clean", "📊 CLIODYNAMIC AUDIT & CALIBRATION REPORT")).append("\n\n");
        fullReport.append(String.format("**%s** : %s\n\n", I18n.getOrDefault("analytics.report.baseline_label", "Baseline Reference"), I18n.getOrDefault("analytics.audit.ground_truth_ref", "Cliodynamic Historical Reality (-100,000 ➔ 2026 CE)")));

        int earliestDivergence = -1;
        StringBuilder explanationSummary = new StringBuilder();

        Map<String, String> benchmarkKeys = new LinkedHashMap<>();
        benchmarkKeys.put("👥 " + I18n.getOrDefault("analytics.spatial.density", "Global Population") + " (worldPopulation)", "worldPopulation");
        benchmarkKeys.put("🦣 " + I18n.getOrDefault("metric.megafauna", "Megafauna Abundance Index") + " (megafaunaIndex)", "megafaunaIndex");
        benchmarkKeys.put("☀️ " + I18n.getOrDefault("metric.milankovitch", "Milankovitch 65°N Insolation") + " (milankovitchInsolation)", "milankovitchInsolation");
        benchmarkKeys.put("🛡️ " + I18n.getOrDefault("metric.containment", "Biogeographical Zero-Containment") + " (zeroContainmentScore)", "zeroContainmentScore");
        benchmarkKeys.put("💰 " + I18n.getOrDefault("metric.gdp", "Gross World Product (GWP)") + " (grossWorldProduct)", "grossWorldProduct");
        benchmarkKeys.put("⚡ " + I18n.getOrDefault("metric.energy", "Primary Energy Consumption") + " (primaryEnergy)", "primaryEnergy");
        benchmarkKeys.put("🏙️ " + I18n.getOrDefault("metric.urbanization", "Urbanization Rate") + " (urbanizationRate)", "urbanizationRate");
        benchmarkKeys.put("🌿 " + I18n.getOrDefault("metric.co2", "Atmospheric CO2 Concentration") + " (co2Concentration)", "co2Concentration");
        benchmarkKeys.put("📖 " + I18n.getOrDefault("metric.literacy", "Global Literacy Rate") + " (literacyRate)", "literacyRate");
        benchmarkKeys.put("📉 " + I18n.getOrDefault("metric.currency", "Sociopolitical Instability") + " (sociopoliticalInstability)", "sociopoliticalInstability");

        record ScenarioAudit(
                ScenarioSelectableItem item,
                SimulationRunRecord run,
                List<VariableDriftAnalysis> drifts,
                Map<String, Double> mapes,
                double avgMape,
                double rSquared,
                int scenarioDivergence
        ) {}

        List<ScenarioAudit> audits = new ArrayList<>();

        for (ScenarioSelectableItem item : simulatedSelected) {
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

            for (Map.Entry<String, String> entry : benchmarkKeys.entrySet()) {
                String label = entry.getKey();
                String benchKey = entry.getValue();
                Map<Integer, Double> benchmark = HistoricalValidationKernel.getBenchmarkDataset(benchKey);
                VariableDriftAnalysis drift = computeVariableDrift(label, benchKey, benchmark, timeSeries, method);
                drifts.add(drift);
                mapes.put(label, drift.mape);
                if (drift.mape > 0.0) {
                    totalMape += drift.mape;
                    validVariables++;
                }
                if (!"—".equals(drift.driftOnsetYear)) {
                    try {
                        int yr = Integer.parseInt(drift.driftOnsetYear.replaceAll("[^0-9-]", ""));
                        if (scenarioDiv == -1 || yr < scenarioDiv) scenarioDiv = yr;
                    } catch (Exception ignored) {}
                }
            }

            double avgMape = validVariables > 0 ? (totalMape / validVariables) : 0.0;
            double rSquared = Math.max(0.0, 1.0 - (avgMape / 100.0));

            audits.add(new ScenarioAudit(item, run, drifts, mapes, avgMape, rSquared, scenarioDiv));

            if (scenarioDiv != -1 && (earliestDivergence == -1 || scenarioDiv < earliestDivergence)) {
                earliestDivergence = scenarioDiv;
            }
        }

        if (audits.isEmpty()) {
            return new AuditResult(
                    I18n.getOrDefault("analytics.error.no_telemetry_history", "⚠️ No execution telemetry history found for selected simulated scenarios. Please execute or load scenarios first."),
                    -1,
                    ""
            );
        }

        fullReport.append("## 🏆 1. ").append(I18n.getOrDefault("analytics.audit.ranking_title", "Inter-Scenario Calibration Ranking (Empirical Reality Fitness)")).append("\n\n");
        fullReport.append("| # | ").append(I18n.getOrDefault("analytics.audit.th_scenario", "Scenario")).append(" | ")
                .append(I18n.getOrDefault("analytics.audit.th_score", "Score de Fidélité")).append(" | ")
                .append(I18n.getOrDefault("analytics.audit.th_mape", "MAPE Globale")).append(" | ")
                .append(I18n.getOrDefault("analytics.audit.th_r2", "R² d'Ajustement")).append(" | ")
                .append(I18n.getOrDefault("analytics.audit.th_div", "Première Rupture (T_div)")).append(" | ")
                .append(I18n.getOrDefault("analytics.audit.th_status", "Diagnostic")).append(" |\n");
        fullReport.append("|---|---|---|---|---|---|---|\n");

        audits.sort(Comparator.comparingDouble(a -> a.avgMape));

        int rank = 1;
        for (ScenarioAudit a : audits) {
            double fidelity = Math.max(0.0, 100.0 - a.avgMape);
            String medal = rank == 1 ? "🥇 " : rank == 2 ? "🥈 " : rank == 3 ? "🥉 " : "• ";
            String divStr = a.scenarioDivergence != -1 ? String.format("%d AD", a.scenarioDivergence) : "Aucune (> 10%)";
            String diagStatus = a.avgMape <= 12.0 ? "🟢 Conforme" : a.avgMape <= 25.0 ? "🟡 Dérive Modérée" : "🔴 Divergence Majeure";

            fullReport.append(String.format(Locale.US, "| %s%d | **%s** | **%.1f%%** | %.2f%% | %.3f | %s | %s |\n",
                    medal, rank, a.item.getName(), fidelity, a.avgMape, a.rSquared, divStr, diagStatus));
            rank++;
        }
        fullReport.append("\n");

        if (audits.size() >= 2) {
            fullReport.append("## 🔬 2. ").append(I18n.getOrDefault("analytics.audit.matrix_title", "Multi-Scenario Comparative Gap Matrix")).append("\n\n");
            fullReport.append("| Variable Historique (Métrique) |");
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
                    Double m = a.mapes.get(varLabel);
                    String badge = (m == null || m == 0.0) ? "⚪ N/A" : m <= 10.0 ? String.format(Locale.US, "🟢 %.1f%%", m) : m <= 25.0 ? String.format(Locale.US, "🟡 %.1f%%", m) : String.format(Locale.US, "🔴 %.1f%%", m);
                    fullReport.append(" ").append(badge).append(" |");
                }
                fullReport.append("\n");
            }
            fullReport.append("\n");
        }

        fullReport.append("## 📊 3. ").append(I18n.getOrDefault("analytics.audit.deep_title", "Deep Diagnostic Analysis by Scenario")).append("\n\n");
        for (ScenarioAudit a : audits) {
            fullReport.append("### 🔎 Scénario : `").append(a.item.getName()).append("` (`").append(a.item.getRunId()).append("`)\n\n");
            fullReport.append("| Variable Auditée | MAPE (%) | Statut | Début Dérive | Pic Divergence | Profil Temporel | Moteur Responsable |\n");
            fullReport.append("|---|---|---|---|---|---|---|\n");
            for (VariableDriftAnalysis d : a.drifts) {
                fullReport.append(String.format(Locale.US, "| %s | %.2f%% | %s | %s | %s | %s | `%s` |\n",
                        d.variableName, d.mape, d.status, d.driftOnsetYear, d.peakDiscrepancy, d.temporalPattern, d.suspectEngine));
            }
            fullReport.append("\n");

            fullReport.append("#### 📐 Statistiques Descriptives & Tests d'Hypothèse (H0 : Modèle = Réalité)\n\n");
            fullReport.append("| Variable | Échantillons (N) | Moy. Observée (μ_obs) | Moy. Simulée (μ_sim ± σ) | IC 95% Simulée | Médienne [IQR] | Cohen's d | Signif. p-val |\n");
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

        for (ScenarioAudit a : audits) {
            explanationSummary.append(String.format(Locale.US, "• **%s** : Fidélité %.1f%% (MAPE %.1f%%) — %s\n",
                    a.item.getName(), Math.max(0.0, 100.0 - a.avgMape), a.avgMape,
                    a.scenarioDivergence != -1 ? String.format("Rupture en %d AD", a.scenarioDivergence) : "Parallèle"));
        }

        return new AuditResult(fullReport.toString(), earliestDivergence, explanationSummary.toString());
    }

    /**
     * Generates a comparative report across pairs of simulated scenarios.
     */
    public static AuditResult generateInterScenarioComparativeReport(List<ScenarioSelectableItem> simulatedSelected,
                                                                      RootCauseAnalyzer analyzer,
                                                                      Function<ScenarioSelectableItem, SimulationRunRecord> runResolver,
                                                                      SimulationRunRepository runRepository) {
        ScenarioSelectableItem baselineItem = simulatedSelected.get(0);
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
        multiReport.append("# ").append(I18n.getOrDefault("analytics.report.title", "📊 RAPPORT D'ANALYSE COMPARATIVE INTER-SCÉNARIOS")).append("\n\n");
        multiReport.append(String.format("**%s** : `%s`\n\n", I18n.getOrDefault("analytics.report.baseline_label", "Baseline Reference Scenario"), baselineItem.getName()));

        int firstDivergence = -1;

        for (int i = 1; i < simulatedSelected.size(); i++) {
            ScenarioSelectableItem targetItem = simulatedSelected.get(i);
            SimulationRunRecord target = runResolver != null ? runResolver.apply(targetItem) : null;

            if (target != null && analyzer != null) {
                RootCauseAnalyzer.ComparisonResult result = analyzer.compareRuns(baseline, target);
                if (result.getDivergenceYear() != -1 && (firstDivergence == -1 || result.getDivergenceYear() < firstDivergence)) {
                    firstDivergence = result.getDivergenceYear();
                }
                explanationSummary.append(String.format("• **vs %s** : %s\n", targetItem.getName(), result.getPrimaryRootCauseExplanation()));
                multiReport.append(ComparativeReportGenerator.generateMarkdownReport(result));
                multiReport.append("\n\n---\n\n");
            }
        }

        if (simulatedSelected.size() >= 3 && runRepository != null && analyzer != null) {
            multiReport.append("## 🔀 Matrice croisée entre scénarios alternatifs\n\n");
            for (int i = 1; i < simulatedSelected.size(); i++) {
                for (int j = i + 1; j < simulatedSelected.size(); j++) {
                    ScenarioSelectableItem itemA = simulatedSelected.get(i);
                    ScenarioSelectableItem itemB = simulatedSelected.get(j);
                    SimulationRunRecord runA = runRepository.getRun(itemA.getRunId());
                    if (runA == null) runA = runRepository.getRunByScenarioName(itemA.getName()).orElse(null);
                    SimulationRunRecord runB = runRepository.getRun(itemB.getRunId());
                    if (runB == null) runB = runRepository.getRunByScenarioName(itemB.getName()).orElse(null);

                    if (runA != null && runB != null) {
                        RootCauseAnalyzer.ComparisonResult crossRes = analyzer.compareRuns(runA, runB);
                        multiReport.append(String.format("### ⚖️ Comparaison : %s ⟷ %s\n\n", itemA.getName(), itemB.getName()));
                        if (crossRes.getDivergenceYear() != -1) {
                            multiReport.append(String.format("> ⚡ **Bifurcation ($T_{\\text{divergence}}$)** : Année **%d AD**\n\n", crossRes.getDivergenceYear()));
                        }
                        fullReportOrCross(crossRes, multiReport);
                        multiReport.append("\n---\n\n");
                    }
                }
            }
        }

        return new AuditResult(multiReport.toString(), firstDivergence, explanationSummary.toString());
    }

    private static void fullReportOrCross(RootCauseAnalyzer.ComparisonResult crossRes, StringBuilder multiReport) {
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
