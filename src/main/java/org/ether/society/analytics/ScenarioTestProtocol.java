/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.model.Scenario;
import org.ether.society.persistence.ScenarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Standardized Scenario Testing & Auto-Correction Protocol Engine ("Début connu, Fin à comparer, Expliquer et Corriger").
 * Provides a reproducible scaffold for running scenarios from a known initial state, comparing end state trajectories
 * against empirical benchmarks or target runs, diagnosing divergence root causes, and generating/applying automated parameter corrections.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class ScenarioTestProtocol {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioTestProtocol.class);

    private final RootCauseAnalyzer analyzer;
    private final ScenarioRepository scenarioRepository;

    public static class ProtocolReport {
        private final Scenario initialScenario;
        private final SimulationRunRecord executionRecord;
        private final SimulationRunRecord targetRecord;
        private final RootCauseAnalyzer.ComparisonResult comparisonResult;
        private final double rmse;
        private final double rSquared;
        private final boolean isCalibrated;

        private final MapComparisonMetrics.MapComparisonResult spatialMetrics;

        public ProtocolReport(Scenario initialScenario, SimulationRunRecord executionRecord,
                              SimulationRunRecord targetRecord, RootCauseAnalyzer.ComparisonResult comparisonResult,
                              double rmse, double rSquared, boolean isCalibrated,
                              MapComparisonMetrics.MapComparisonResult spatialMetrics) {
            this.initialScenario = initialScenario;
            this.executionRecord = executionRecord;
            this.targetRecord = targetRecord;
            this.comparisonResult = comparisonResult;
            this.rmse = rmse;
            this.rSquared = rSquared;
            this.isCalibrated = isCalibrated;
            this.spatialMetrics = spatialMetrics;
        }

        public Scenario getInitialScenario() { return initialScenario; }
        public SimulationRunRecord getExecutionRecord() { return executionRecord; }
        public SimulationRunRecord getTargetRecord() { return targetRecord; }
        public RootCauseAnalyzer.ComparisonResult getComparisonResult() { return comparisonResult; }
        public double getRmse() { return rmse; }
        public double getRSquared() { return rSquared; }
        public boolean isCalibrated() { return isCalibrated; }
        public MapComparisonMetrics.MapComparisonResult getSpatialMetrics() { return spatialMetrics; }

        public String getSummary() {
            String spatialSummaryStr = spatialMetrics != null ? "\n  - Spatial SSIM: " + String.format("%.4f", spatialMetrics.getSsim()) + " | Spatial Pearson r: " + String.format("%.4f", spatialMetrics.getPearsonR()) : "";
            return String.format(
                "📋 Protocol Report for [%s]:\n" +
                "  - Status: %s\n" +
                "  - Divergence Year (T_divergence): %s\n" +
                "  - RMSE: %.2f | R^2: %.4f%s\n" +
                "  - Explanation: %s\n" +
                "  - Corrections Proposed: %d",
                initialScenario != null ? initialScenario.getName() : "Unknown",
                isCalibrated ? "🟢 CALIBRATED" : "🔴 DIVERGENT (Corrections Required)",
                comparisonResult != null && comparisonResult.getDivergenceYear() != -1 ? "An " + comparisonResult.getDivergenceYear() : "Aucune",
                rmse, rSquared, spatialSummaryStr,
                comparisonResult != null ? comparisonResult.getPrimaryRootCauseExplanation() : "N/A",
                comparisonResult != null ? comparisonResult.getProposedCorrections().size() : 0
            );
        }
    }

    public ScenarioTestProtocol() {
        this.analyzer = new RootCauseAnalyzer();
        this.scenarioRepository = new ScenarioRepository();
    }

    /**
     * Executes the full testing protocol for a scenario against a known target run:
     * 1. Known Start: Executes the scenario headlessly.
     * 2. Expected End Comparison: Compares generated run against expected target run.
     * 3. Root Cause Explanation: Identifies T_divergence and driver factors.
     * 4. Parameter Corrections: Generates actionable parameter adjustment recommendations.
     */
    public ProtocolReport runProtocol(Scenario scenario, SimulationRunRecord targetRun) {
        if (scenario == null || targetRun == null) {
            throw new IllegalArgumentException("Scenario and targetRun must not be null.");
        }

        logger.info("🧪 Running Scenario Test Protocol for scenario '{}' vs target run '{}'", scenario.getName(), targetRun.getRunId());

        // 1. Début Connu: Headless simulation execution
        SimulationRunRecord executionRecord = HeadlessBatchRunner.executeScenarioHeadless(scenario);

        // 2. Fin à Comparer & Expliquer: Compute divergence and root cause analysis
        RootCauseAnalyzer.ComparisonResult comparison = analyzer.compareRuns(targetRun, executionRecord);

        // 3. Evaluate Statistical Metrics (RMSE and R^2)
        Map<Integer, Double> simulatedPopMap = extractPopulationMap(executionRecord);
        Map<Integer, Double> targetPopMap = extractPopulationMap(targetRun);

        double rmse = HistoricalValidationKernel.calculateRmse(simulatedPopMap, targetPopMap);
        double rSquared = HistoricalValidationKernel.calculateRSquared(simulatedPopMap, targetPopMap);
        boolean isCalibrated = comparison.getDivergenceYear() == -1 && rmse < 1000.0 && rSquared >= 0.90;

        ProtocolReport report = new ProtocolReport(scenario, executionRecord, targetRun, comparison, rmse, rSquared, isCalibrated, null);
        logger.info(report.getSummary());

        return report;
    }

    /**
     * Executes the scenario protocol against a 20-variable historical benchmark window.
     */
    public ProtocolReport runProtocolAgainstBenchmark(Scenario scenario, HistoricalValidationKernel.EpochWindow window) {
        Map<Integer, Double> benchmarkPop = HistoricalValidationKernel.filterByWindow(
            HistoricalValidationKernel.getHistoricalWorldPopulation(), window);

        SimulationRunRecord benchmarkRecord = createSyntheticBenchmarkRecord(scenario, benchmarkPop);
        return runProtocol(scenario, benchmarkRecord);
    }

    /**
     * Applies proposed parameter corrections ("Corriger") directly to a Scenario object.
     */
    public void applyCorrections(Scenario scenario, List<RootCauseAnalyzer.ParameterCorrection> corrections) {
        if (scenario == null || corrections == null || corrections.isEmpty()) return;

        logger.info("🛠️ Applying {} parameter corrections to scenario '{}'", corrections.size(), scenario.getName());

        for (RootCauseAnalyzer.ParameterCorrection corr : corrections) {
            String param = corr.getParameterName();
            try {
                if (param.equals("initialFoodReserveMonths")) {
                    double val = parseDouble(corr.getProposedValue(), scenario.getInitialFoodReserveMonths());
                    scenario.setInitialFoodReserveMonths(val);
                } else if (param.equals("initialCapitalPerCapita")) {
                    double val = parseDouble(corr.getProposedValue(), scenario.getInitialCapitalPerCapita());
                    scenario.setInitialCapitalPerCapita(val);
                } else if (param.equals("initialHumanCount")) {
                    long val = parseLong(corr.getProposedValue(), scenario.getInitialHumanCount());
                    scenario.setInitialHumanCount(val);
                } else if (param.equals("initialInformationPerCapita")) {
                    double val = parseDouble(corr.getProposedValue(), scenario.getInitialInformationPerCapita());
                    scenario.setInitialInformationPerCapita(val);
                } else if (param.startsWith("typeBEngineStates:")) {
                    String engineName = param.substring("typeBEngineStates:".length());
                    scenario.getTypeBEngineStates().put(engineName, true);
                }
            } catch (Exception e) {
                logger.error("Failed to apply correction for parameter {}: {}", param, e.getMessage());
            }
        }

        scenarioRepository.saveOrUpdate(scenario);
    }

    /**
     * Executes an automated iterative self-calibration loop ("Expliquer et Corriger").
     * Iteratively executes the protocol, applies proposed corrections, and re-runs until
     * calibrated or maximum iterations reached.
     */
    public ProtocolReport autoCalibrate(Scenario scenario, SimulationRunRecord targetRun, int maxIterations, double targetRmseThreshold) {
        ProtocolReport currentReport = null;

        for (int iter = 1; iter <= maxIterations; iter++) {
            logger.info("🔄 Auto-Calibration Iteration {}/{} for scenario '{}'", iter, maxIterations, scenario.getName());
            currentReport = runProtocol(scenario, targetRun);

            if (currentReport.isCalibrated() || currentReport.getRmse() <= targetRmseThreshold) {
                logger.info("✅ Auto-Calibration Converged at iteration {} with RMSE = {}", iter, currentReport.getRmse());
                break;
            }

            List<RootCauseAnalyzer.ParameterCorrection> corrections = currentReport.getComparisonResult().getProposedCorrections();
            if (corrections.isEmpty()) {
                logger.info("ℹ️ No further parameter corrections proposed. Stopping auto-calibration loop.");
                break;
            }

            applyCorrections(scenario, corrections);
        }

        return currentReport;
    }

    private Map<Integer, Double> extractPopulationMap(SimulationRunRecord record) {
        Map<Integer, Double> map = new java.util.TreeMap<>();
        if (record != null && record.getTimeSeriesData() != null) {
            for (var entry : record.getTimeSeriesData().entrySet()) {
                map.put(entry.getKey(), (double) entry.getValue().getPopulation());
            }
        }
        return map;
    }

    private SimulationRunRecord createSyntheticBenchmarkRecord(Scenario scenario, Map<Integer, Double> benchmarkPop) {
        String runId = "BENCHMARK-" + scenario.getName().replaceAll("[^a-zA-Z0-9]", "-").toUpperCase();
        SimulationRunRecord record = new SimulationRunRecord(runId, "Benchmark " + scenario.getName(), "Historical Benchmark Reference Data", Map.of());

        for (var entry : benchmarkPop.entrySet()) {
            int year = entry.getKey();
            long pop = Math.round(entry.getValue());
            record.addSnapshot(year, pop, pop * 0.8, 10.0, 85.0, 100);
        }
        return record;
    }

    private double parseDouble(String str, double fallback) {
        try {
            String cleaned = str.replaceAll("[^0-9,.-]", "").replace(',', '.');
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return fallback;
        }
    }

    private long parseLong(String str, long fallback) {
        try {
            String cleaned = str.replaceAll("[^0-9-]", "");
            return Long.parseLong(cleaned);
        } catch (Exception e) {
            return fallback;
        }
    }
}

