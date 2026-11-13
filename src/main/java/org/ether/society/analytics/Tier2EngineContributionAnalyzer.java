/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.analytics.EngineContributionReport.EngineEntry;
import org.ether.society.analytics.EngineContributionReport.MetricContribution;
import org.ether.society.core.ScenarioGridInitializer;
import org.ether.society.core.engine.SimulationEngineRegistry;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.ether.society.persistence.EtherJsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Deterministic Leave-One-Out (LOO) Variance Attribution Analyzer for Tier 2 Cliodynamic Engines.
 *
 * <p>Quantifies the partial contribution (%) of each enabled Tier 2 engine to trajectory variance
 * across core physical and macroeconomic observables, identifies divergence onset years
 * (\(T_{\text{divergence}}\)), and evaluates impact on historical empirical fit (\(\Delta\text{MAPE}\)).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see EngineContributionReport
 */
public final class Tier2EngineContributionAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(Tier2EngineContributionAnalyzer.class);
    public static final String CONTRIBUTIONS_FILENAME = "engine_contributions.json";

    private static final List<String> CORE_METRICS = List.of(
            "worldPopulation",
            "grossWorldProduct",
            "primaryEnergy",
            "giniInequality",
            "asabiyyahSocialCohesion",
            "co2Concentration"
    );

    private static final Set<String> JIT_SYMBOLIC_ENGINES = Set.of(
            "SmilMaterialTransitionsPureEngine",
            "JevonsParadoxEngine",
            "TasmanianCulturalRegressionEngine",
            "CoGovernanceTradeEngine",
            "KurzweilAcceleratingReturnsEngine",
            "World3CouplingEngine",
            "RomanImperialCliodynamicEngine"
    );

    private static final Set<String> AUXILIARY_TYPEB_ENGINES = Set.of(
            "EdoJapanIsolationEngine",
            "NuclearSafetyRadiotoxicityEngine",
            "NuclearWarfareClimateEngine",
            "LanguageLinguisticEngine"
    );

    private Tier2EngineContributionAnalyzer() {
        // Utility analyzer
    }

    /**
     * Execution wiring status descriptor for a Tier 2 engine identifier.
     *
     * @param wiredInLoop  {@code true} if the engine participates in the tick execution pipeline
     * @param statusLabel  human-readable wiring classification
     */
    public record WiringStatus(boolean wiredInLoop, String statusLabel) {}

    /**
     * Classifies whether a Tier 2 engine ID is currently executed by the tick pipeline.
     *
     * @param engineId Tier 2 engine identifier
     * @return wiring status descriptor
     */
    public static WiringStatus classifyEngineWiring(String engineId) {
        if (engineId == null || engineId.isBlank()) {
            return new WiringStatus(false, "Unknown");
        }
        if (SimulationEngineRegistry.getInstance().getEngine(engineId) != null
                || "TechTreeEngine".equals(engineId)) {
            return new WiringStatus(true, "SimulationEngineModule (DoD)");
        }
        if (JIT_SYMBOLIC_ENGINES.contains(engineId)) {
            return new WiringStatus(true, "JIT Symbolic (TypeB)");
        }
        if (AUXILIARY_TYPEB_ENGINES.contains(engineId)) {
            return new WiringStatus(true, "Auxiliary Hook (TypeB)");
        }
        return new WiringStatus(false, "Unwired (Inactive in Tick Loop)");
    }

    /**
     * Executes deterministic Leave-One-Out ablation across all enabled Tier 2 engines of {@code scenario}.
     *
     * @param scenario            target scenario configuration
     * @param skipKnownUnwired    if {@code true}, skips re-running engines known not to be wired into the tick loop
     * @param optionalBaselineRun optional pre-computed deterministic baseline run (if {@code null}, executed fresh)
     * @return structured attribution report, or {@code null} if scenario is invalid
     */
    public static EngineContributionReport analyze(
            Scenario scenario,
            boolean skipKnownUnwired,
            SimulationRunRecord optionalBaselineRun) {
        if (scenario == null) return null;

        Scenario detScenario = scenario.copy();
        detScenario.setStrictDeterminism(true);
        detScenario.setParallelExecutionEnabled(false);
        if (detScenario.getSeed() == 0L) {
            detScenario.setSeed(42L);
        }
        if (detScenario.getH3Resolution() <= 0) {
            detScenario.setH3Resolution(1);
        }

        Map<String, Boolean> engineStates = detScenario.getTypeBEngineStates();
        List<String> enabledEngines = new ArrayList<>();
        if (engineStates != null) {
            for (Map.Entry<String, Boolean> entry : engineStates.entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    enabledEngines.add(entry.getKey());
                }
            }
        }
        Collections.sort(enabledEngines);

        if (enabledEngines.isEmpty()) {
            return new EngineContributionReport(
                    detScenario.getName(),
                    optionalBaselineRun != null ? optionalBaselineRun.getRunId() : "DET-" + detScenario.getPresetKey(),
                    detScenario.getSeed(),
                    (int) detScenario.getStartDateYear(),
                    (int) detScenario.getEndDateYear(),
                    detScenario.getH3Resolution(),
                    0,
                    0,
                    Collections.emptyList()
            );
        }

        List<H3Cell> baseCells = ScenarioGridInitializer.initializeCellsForScenario(detScenario, null, null);
        if (baseCells == null || baseCells.isEmpty()) {
            logger.warn("Could not initialize H3 grid for Tier 2 contribution analysis on '{}'.", detScenario.getName());
            return null;
        }

        // Always execute deterministic baseline on the exact same baseCells snapshot when strict determinism matters
        SimulationRunRecord baseline = (optionalBaselineRun != null && scenario.isStrictDeterminism()
                && !optionalBaselineRun.getTimeSeriesData().isEmpty())
                ? optionalBaselineRun
                : HeadlessBatchRunner.executeScenarioWithCells(detScenario, cloneCells(baseCells), false, null, null);

        if (baseline == null || baseline.getTimeSeriesData().isEmpty()) {
            return null;
        }

        double baselineMape = computeMeanEmpiricalMape(detScenario, baseline);

        record RawEngineDiff(
                String engineId,
                WiringStatus wiring,
                Map<String, Double> rmseByMetric,
                Map<String, Double> finalDeltaByMetric,
                int divergenceYear,
                double mapeDeltaVsReality
        ) {}

        List<RawEngineDiff> rawDiffs = new ArrayList<>();
        int testedCount = 0;

        for (String engineId : enabledEngines) {
            WiringStatus wiring = classifyEngineWiring(engineId);
            if (skipKnownUnwired && !wiring.wiredInLoop()) {
                rawDiffs.add(new RawEngineDiff(
                        engineId, wiring, Collections.emptyMap(), Collections.emptyMap(),
                        Integer.MIN_VALUE, Double.isNaN(baselineMape) ? Double.NaN : 0.0));
                continue;
            }

            Scenario ablated = detScenario.copy();
            ablated.getTypeBEngineStates().put(engineId, false);
            SimulationRunRecord ablatedRun = HeadlessBatchRunner.executeScenarioWithCells(
                    ablated, cloneCells(baseCells), false, null, null);
            testedCount++;

            if (ablatedRun == null || ablatedRun.getTimeSeriesData().isEmpty()) {
                rawDiffs.add(new RawEngineDiff(
                        engineId, wiring, Collections.emptyMap(), Collections.emptyMap(),
                        Integer.MIN_VALUE, Double.NaN));
                continue;
            }

            Map<String, Double> rmseMap = new LinkedHashMap<>();
            Map<String, Double> finalDeltaMap = new LinkedHashMap<>();
            int divYear = computeTrajectoryDivergence(baseline, ablatedRun, rmseMap, finalDeltaMap);

            double ablatedMape = computeMeanEmpiricalMape(ablated, ablatedRun);
            double mapeDelta = (!Double.isNaN(baselineMape) && !Double.isNaN(ablatedMape))
                    ? (ablatedMape - baselineMape)
                    : Double.NaN;

            rawDiffs.add(new RawEngineDiff(engineId, wiring, rmseMap, finalDeltaMap, divYear, mapeDelta));
        }

        // Normalize squared trajectory RMSE per metric across all engines
        Map<String, Double> sumSqByMetric = new LinkedHashMap<>();
        for (String metric : CORE_METRICS) {
            double sumSq = 0.0;
            for (RawEngineDiff diff : rawDiffs) {
                double rmse = diff.rmseByMetric().getOrDefault(metric, 0.0);
                sumSq += rmse * rmse;
            }
            sumSqByMetric.put(metric, sumSq);
        }

        int activeMetricCount = 0;
        for (double s : sumSqByMetric.values()) {
            if (s > 1e-12) activeMetricCount++;
        }

        List<EngineEntry> entries = new ArrayList<>();
        for (RawEngineDiff diff : rawDiffs) {
            List<MetricContribution> metricContribs = new ArrayList<>();
            double shareSum = 0.0;

            for (String metric : CORE_METRICS) {
                double rmse = diff.rmseByMetric().getOrDefault(metric, 0.0);
                double finalDelta = diff.finalDeltaByMetric().getOrDefault(metric, 0.0);
                double sumSq = sumSqByMetric.getOrDefault(metric, 0.0);
                double sharePct = (sumSq > 1e-12) ? ((rmse * rmse) / sumSq) * 100.0 : 0.0;
                if (sumSq > 1e-12) {
                    shareSum += sharePct;
                }
                metricContribs.add(new MetricContribution(metric, sharePct, rmse, finalDelta));
            }

            double globalShare = (activeMetricCount > 0) ? (shareSum / activeMetricCount) : 0.0;
            String classification = classifyContribution(diff.wiring().wiredInLoop(), globalShare);

            entries.add(new EngineEntry(
                    diff.engineId(),
                    diff.wiring().statusLabel(),
                    diff.wiring().wiredInLoop(),
                    globalShare,
                    diff.divergenceYear(),
                    diff.mapeDeltaVsReality(),
                    classification,
                    metricContribs
            ));
        }

        // Sort descending by global variance share, then wired status, then alphabetical
        entries.sort((a, b) -> {
            int cmp = Double.compare(b.globalVarianceSharePct(), a.globalVarianceSharePct());
            if (cmp != 0) return cmp;
            cmp = Boolean.compare(b.wiredInLoop(), a.wiredInLoop());
            if (cmp != 0) return cmp;
            return a.engineId().compareTo(b.engineId());
        });

        String runId = optionalBaselineRun != null ? optionalBaselineRun.getRunId() : baseline.getRunId();
        return new EngineContributionReport(
                detScenario.getName(),
                runId,
                detScenario.getSeed(),
                (int) detScenario.getStartDateYear(),
                (int) detScenario.getEndDateYear(),
                detScenario.getH3Resolution(),
                testedCount,
                enabledEngines.size(),
                entries
        );
    }

    /**
     * Persists an {@link EngineContributionReport} to {@code engine_contributions.json} inside {@code saveDir}.
     *
     * @param saveDir target save directory
     * @param report  report to serialize
     */
    public static void saveReport(Path saveDir, EngineContributionReport report) {
        if (saveDir == null || report == null) return;
        try {
            Files.createDirectories(saveDir);
            EtherJsonHelper.writePrettyAtomic(saveDir.resolve(CONTRIBUTIONS_FILENAME), report);
        } catch (Exception e) {
            logger.warn("Could not persist engine contribution report to {}: {}", saveDir, e.getMessage());
        }
    }

    /**
     * Loads a persisted {@link EngineContributionReport} from {@code saveDir/engine_contributions.json} if present.
     *
     * @param saveDir run save directory
     * @return loaded report, or {@code null} if absent/unreadable
     */
    public static EngineContributionReport loadReport(Path saveDir) {
        if (saveDir == null) return null;
        Path path = saveDir.resolve(CONTRIBUTIONS_FILENAME);
        if (!Files.exists(path)) return null;
        try {
            return EtherJsonHelper.getMapper().readValue(path.toFile(), EngineContributionReport.class);
        } catch (Exception e) {
            logger.warn("Could not read engine contribution report from {}: {}", path, e.getMessage());
            return null;
        }
    }

    private static List<H3Cell> cloneCells(List<H3Cell> source) {
        List<H3Cell> cloned = new ArrayList<>(source.size());
        for (H3Cell c : source) {
            cloned.add(c.snapshot());
        }
        return cloned;
    }

    private static int computeTrajectoryDivergence(
            SimulationRunRecord baseline,
            SimulationRunRecord ablated,
            Map<String, Double> outRmsePct,
            Map<String, Double> outFinalDeltaPct) {

        TreeMap<Integer, SimulationRunRecord.MetricSnapshot> tsBase = baseline.getTimeSeriesData();
        TreeMap<Integer, SimulationRunRecord.MetricSnapshot> tsAbl = ablated.getTimeSeriesData();

        List<Integer> years = new ArrayList<>(tsBase.keySet());
        Collections.sort(years);
        if (years.isEmpty()) return Integer.MIN_VALUE;

        int firstDivYear = Integer.MIN_VALUE;

        for (String metric : CORE_METRICS) {
            double sumSqRelErr = 0.0;
            int n = 0;
            double lastDeltaPct = 0.0;

            for (int yr : years) {
                SimulationRunRecord.MetricSnapshot sBase = tsBase.get(yr);
                SimulationRunRecord.MetricSnapshot sAbl = tsAbl.get(yr);
                if (sBase == null || sAbl == null) continue;

                double vBase = GroundTruthAuditGenerator.extractValue(sBase, metric, yr);
                double vAbl = GroundTruthAuditGenerator.extractValue(sAbl, metric, yr);
                if (Double.isNaN(vBase) || Double.isNaN(vAbl) || Double.isInfinite(vBase) || Double.isInfinite(vAbl)) {
                    continue;
                }

                double denom = Math.max(Math.abs(vBase), 1e-4);
                double relDiffPct = Math.abs(vBase - vAbl) / denom * 100.0;
                sumSqRelErr += relDiffPct * relDiffPct;
                n++;
                lastDeltaPct = ((vAbl - vBase) / denom) * 100.0;

                if (relDiffPct >= 1.0 && (firstDivYear == Integer.MIN_VALUE || yr < firstDivYear)) {
                    firstDivYear = yr;
                }
            }

            double rmsePct = (n > 0) ? Math.sqrt(sumSqRelErr / n) : 0.0;
            outRmsePct.put(metric, rmsePct);
            outFinalDeltaPct.put(metric, lastDeltaPct);
        }

        return firstDivYear;
    }

    private static double computeMeanEmpiricalMape(Scenario scenario, SimulationRunRecord run) {
        if (scenario != null && scenario.getPlanetPreset() != null
                && !scenario.getPlanetPreset().getCanonicalPlanet().equalsIgnoreCase("earth")) {
            return Double.NaN;
        }
        if (run == null || run.getTimeSeriesData().isEmpty()) return Double.NaN;

        double sumMape = 0.0;
        int validVars = 0;
        double[] windowBounds = RegionalRasterBenchmarkExtractor.parseWindowBounds(run);
        double scaleFactor = GroundTruthAuditReportBuilder.computeRegionalScaleFactor(
                run, run.getTimeSeriesData(), HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
        if (scaleFactor < 0.0) return Double.NaN;

        List<String> empiricalKeys = List.of("worldPopulation", "grossWorldProduct", "primaryEnergy", "co2Concentration");
        for (String key : empiricalKeys) {
            Map<Integer, Double> bench = HistoricalValidationKernel.getBenchmarkDataset(key);
            GroundTruthAuditGenerator.VariableDriftAnalysis drift = GroundTruthAuditGenerator.computeVariableDrift(
                    key, key, bench, run.getTimeSeriesData(),
                    HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                    scaleFactor, windowBounds);
            if (drift.sampleCount > 0) {
                sumMape += drift.mape;
                validVars++;
            }
        }
        return validVars > 0 ? (sumMape / validVars) : Double.NaN;
    }

    private static String classifyContribution(boolean wiredInLoop, double globalSharePct) {
        if (!wiredInLoop) {
            return "âšª Inert (Unwired)";
        }
        if (globalSharePct >= 10.0) {
            return "🔥 Dominant";
        }
        if (globalSharePct >= 1.0) {
            return " Moderate";
        }
        if (globalSharePct > 1e-4) {
            return " Marginal";
        }
        return "âšª Inert (0%)";
    }
}

