/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Historical Steady-Regime Calibration & Multi-Resolution Sensitivity Harness.
 *
 * <p>Calibrates the core physical, demographic, and cliodynamic simulation engines against
 * canonical historical epochs devoid of major systemic bifurcations/tipping points, enabling
 * precise baseline parameter calibration before assessing non-linear disruptions.</p>
 *
 * <p>Supports:
 * <ul>
 *   <li><b>Intermediate Checkpoints Verification</b>: Evaluates raster maps and socio-economic variables
 *       at multiple intermediate time horizons \(t_k \in (t_0, t_1)\) to locate where and when deviations occur.</li>
 *   <li><b>Empire & Polity Spatial Localization</b>: Verifies geographic centroids \((\text{Lat}, \text{Lng})\),
 *       territory coverage (Jaccard/Dice overlap), and demographic mass of historical empires.</li>
 *   <li><b>Cartographic Tensor Comparison</b>: SSIM, Pearson \(r\), RMSE, and KL Divergence on Density, Sovereignty,
 *       and Technology layers.</li>
 *   <li><b>Root Cause Drift Decomposition & Automated Remediation</b>: Differentiates historical stochastic shocks
 *       from unidirectional systematic engine biases, calculating exact correction multipliers.</li>
 * </ul>
 * </p>
 *
 * <p>Decomposed to adhere strictly to the Single Responsibility Principle (SRP):
 * <ul>
 *   <li>Scenario catalogs in {@link HistoricalCalibrationScenarioRegistry}.</li>
 *   <li>Forward DoD simulation execution in {@link HistoricalCalibrationSimulationExecutor}.</li>
 *   <li>Sensitivity & engine ablation audits in {@link HistoricalCalibrationSensitivityHelper}.</li>
 *   <li>Atomicity & save persistence in {@link CalibrationPersistenceService}.</li>
 *   <li>Spatial maps & checkpoint evaluations in {@link HistoricalCalibrationSpatialHelper}.</li>
 *   <li>Academic report synthesis in {@link HistoricalCalibrationReportGenerator}.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 */
public class HistoricalScenarioCalibrationHarness {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalScenarioCalibrationHarness.class);

    /**
     * Drift classification taxonomy.
     */
    public enum DriftCategory {
        HISTORICAL_STOCHASTIC_SHOCK("Exogenous Historical Shock (Unpredictable historical accident / localized climate anomaly)"),
        SYSTEMATIC_MALTHUSIAN_CAPACITY("Systematic Engine Drift: Malthusian Carrying Capacity / Agrarian Yield"),
        SYSTEMATIC_DEMOGRAPHIC_VITAL_RATES("Systematic Engine Drift: Demographic Fertility / Mortality Vital Kinetics"),
        SYSTEMATIC_INNOVATION_DIFFUSION("Systematic Engine Drift: Technological & Knowledge Diffusion Elasticity"),
        SYSTEMATIC_EXERGY_ENERGY_SCALING("Systematic Engine Drift: Thermodynamic Exergy & Energy Conversion Efficiency"),
        SYSTEMATIC_SPATIAL_FRICTION("Systematic Engine Drift: Spatial Agglomeration & Migration Dispersion Friction");

        public final String description;

        DriftCategory(String desc) {
            this.description = desc;
        }
    }

    /**
     * Historical empire / polity ground truth definition for spatial localization verification.
     */
    public record EmpireGroundTruth(
            String empireName,
            int year,
            double expectedCentroidLat,
            double expectedCentroidLng,
            double expectedPopulationMillions,
            double expectedTechLevel,
            double expectedTerritoryAreaKm2
    ) {}

    /**
     * Calibration scenario definition for non-bifurcating historical regimes.
     */
    public record CalibrationScenarioDefinition(
            String scenarioKey,
            String displayName,
            int startYear,
            int endYear,
            List<Integer> intermediateCheckpointYears,
            String historicalRegimeDescription,
            double initialWorldPopMillions,
            double targetWorldPopMillions,
            float initialTechLevel,
            float initialCapitalPerCapita,
            float agroYieldCoeff,
            List<EmpireGroundTruth> keyEmpires,
            boolean isNonBifurcatingCalibrationTarget
    ) {}

    /**
     * Empire localization validation checkpoint.
     */
    public record EmpireLocalizationResult(
            String empireName,
            int year,
            double simulatedCentroidLat,
            double simulatedCentroidLng,
            double expectedCentroidLat,
            double expectedCentroidLng,
            double centroidDisplacementKm,
            double simulatedPopulationMillions,
            double expectedPopulationMillions,
            double populationMapePercent,
            double simulatedTechLevel,
            double expectedTechLevel,
            double territorialJaccardOverlap,
            boolean isProperlyLocated
    ) {}

    /**
     * Intermediate checkpoint evaluation snapshot.
     */
    public static class IntermediateCheckpointSnapshot {
        public int year;
        public double simulatedPopulation;
        public double observedPopulation;
        public double demographicMape;
        public double simulatedGwp;
        public double observedGwp;
        public double simulatedTech;
        public double observedTech;
        public MapComparisonMetrics.MapComparisonResult densityMapComparison;
        public MapComparisonMetrics.MapComparisonResult sovereigntyMapComparison;
        public List<EmpireLocalizationResult> empireValidations = new ArrayList<>();
    }

    /**
     * Discrepancy index and spectral derivative evaluation.
     */
    public record DiscrepancySpectrum(
            int year,
            double omega,
            double omegaDot,
            String regimeClassification
    ) {}

    // --- PREDEFINED SCENARIO CATALOGS (DELEGATED FOR 100% BACKWARD COMPATIBILITY) ---
    public static final List<CalibrationScenarioDefinition> CALIBRATION_SCENARIOS = HistoricalCalibrationScenarioRegistry.CALIBRATION_SCENARIOS;
    public static final List<CalibrationScenarioDefinition> FALSIFICATION_COUNTERFACTUAL_SCENARIOS = HistoricalCalibrationScenarioRegistry.FALSIFICATION_COUNTERFACTUAL_SCENARIOS;
    public static final List<CalibrationScenarioDefinition> ARCHAEOLOGICAL_DETECTIVE_SCENARIOS = HistoricalCalibrationScenarioRegistry.ARCHAEOLOGICAL_DETECTIVE_SCENARIOS;
    public static final List<CalibrationScenarioDefinition> MASTER_NINE_EPOCH_BLOCKS = HistoricalCalibrationScenarioRegistry.MASTER_NINE_EPOCH_BLOCKS;

    /**
     * Parameter drift diagnostic and remediation item.
     */
    public record DriftDiagnosis(
            String metricName,
            DriftCategory category,
            double observedValue,
            double simulatedValue,
            double mapeDeviationPercent,
            String direction,
            String physicalRootCause,
            String parameterToTune,
            String currentValue,
            String recommendedValue,
            double tuningMultiplier
    ) {}

    /**
     * Calibration outcome for a single scenario.
     */
    public static class ScenarioCalibrationResult {
        public CalibrationScenarioDefinition scenario;
        public Map<Integer, Double> simulatedPopulationTrajectory = new TreeMap<>();
        public Map<String, Double> finalSimulatedTelemetry = new LinkedHashMap<>();
        public Map<String, Double> observedGroundTruthTelemetry = new LinkedHashMap<>();
        public Map<String, Double> metricMape = new LinkedHashMap<>();
        public double compositeRSquared;
        public double compositeRmse;
        public double meanMape;

        // Intermediate Checkpoint Snapshots
        public List<IntermediateCheckpointSnapshot> intermediateCheckpoints = new ArrayList<>();
        public List<DiscrepancySpectrum> discrepancySpectra = new ArrayList<>();

        // Spatial Map Comparison Results (if raster layers exist on disk)
        public MapComparisonMetrics.MapComparisonResult densityMapComparison;
        public MapComparisonMetrics.MapComparisonResult biomesMapComparison;
        public MapComparisonMetrics.MapComparisonResult technologyMapComparison;

        // Root Cause & Remediation
        public List<DriftDiagnosis> driftDiagnoses = new ArrayList<>();

        public ScenarioCalibrationResult(CalibrationScenarioDefinition scenario) {
            this.scenario = scenario;
        }
    }

    /**
     * Spatial resolution sensitivity entry.
     */
    public record SpatialSensitivityEntry(
            int h3Resolution,
            int cellCount,
            double spatialRmse,
            double spatialPearsonR,
            double ssim,
            double throughputTPS,
            double relativeSpeedVsBaseline
    ) {}

    /**
     * Temporal integration step sensitivity entry.
     */
    public record TemporalSensitivityEntry(
            int tickStepDays,
            double demographicDriftMape,
            double energyDriftMape,
            double integrationDriftRmse,
            double throughputTPS
    ) {}

    /**
     * Super-agent cohort size sensitivity entry.
     */
    public record CohortSensitivityEntry(
            int targetCohortSize,
            int activeCohortNodes,
            double stochasticVariancePercent,
            double memoryFootprintMB,
            double throughputTPS
    ) {}

    /**
     * Multi-scale sensitivity matrix result container.
     */
    public static class MultiScaleSensitivityMatrix {
        public List<SpatialSensitivityEntry> spatialEntries = new ArrayList<>();
        public List<TemporalSensitivityEntry> temporalEntries = new ArrayList<>();
        public List<CohortSensitivityEntry> cohortEntries = new ArrayList<>();
    }

    /**
     * Ablation audit record for specialized/hybrid engines.
     */
    public record EngineAblationAuditEntry(
            String engineName,
            String targetEpoch,
            double deltaRmseWithoutEngine,
            double cpuOverheadPercent,
            boolean isRecommendedActive,
            String justification
    ) {}

    /**
     * Runs comprehensive calibration across canonical steady-regime scenarios.
     *
     * @return list of calibration results
     */
    public static List<ScenarioCalibrationResult> runAllScenarioCalibrations() {
        List<ScenarioCalibrationResult> results = new ArrayList<>();
        for (CalibrationScenarioDefinition scenarioDef : CALIBRATION_SCENARIOS) {
            logger.info("🎯 Running Calibration Scenario: '{}' (Years {} -> {})",
                    scenarioDef.displayName(), scenarioDef.startYear(), scenarioDef.endYear());
            results.add(runScenarioCalibration(scenarioDef));
        }
        return results;
    }

    /**
     * Runs twin counterfactual falsification scenarios.
     *
     * @return list of counterfactual calibration results
     */
    public static List<ScenarioCalibrationResult> runAllFalsificationCounterfactuals() {
        List<ScenarioCalibrationResult> results = new ArrayList<>();
        for (CalibrationScenarioDefinition scenarioDef : FALSIFICATION_COUNTERFACTUAL_SCENARIOS) {
            logger.info("🔬 Running Counterfactual Falsification Twin: '{}' (Years {} -> {})",
                    scenarioDef.displayName(), scenarioDef.startYear(), scenarioDef.endYear());
            results.add(runScenarioCalibration(scenarioDef));
        }
        return results;
    }

    /**
     * Runs full scientific calibration and falsification suite.
     *
     * @return combined list of all calibration, counterfactual, detective, and master epoch results
     */
    public static List<ScenarioCalibrationResult> runFullScientificValidationSuite() {
        List<ScenarioCalibrationResult> all = new ArrayList<>(runAllScenarioCalibrations());
        all.addAll(runAllFalsificationCounterfactuals());
        all.addAll(runAllArchaeologicalDetectiveScenarios());
        all.addAll(runAllMasterNineEpochBlocks());
        return all;
    }

    /**
     * Runs Archaeological Detective scenarios to locate unrecorded historical anomalies.
     *
     * @return list of archaeological detective scenario results
     */
    public static List<ScenarioCalibrationResult> runAllArchaeologicalDetectiveScenarios() {
        List<ScenarioCalibrationResult> results = new ArrayList<>();
        for (CalibrationScenarioDefinition scenarioDef : ARCHAEOLOGICAL_DETECTIVE_SCENARIOS) {
            logger.info("🕵️ Running Archaeological Detective Scenario: '{}' (Years {} -> {})",
                    scenarioDef.displayName(), scenarioDef.startYear(), scenarioDef.endYear());
            results.add(runScenarioCalibration(scenarioDef));
        }
        return results;
    }

    /**
     * Runs master 9-epoch historical blocks across the entire -100,000 BP to 2026 CE timeline.
     *
     * @return list of master epoch calibration results
     */
    public static List<ScenarioCalibrationResult> runAllMasterNineEpochBlocks() {
        List<ScenarioCalibrationResult> results = new ArrayList<>();
        for (CalibrationScenarioDefinition scenarioDef : MASTER_NINE_EPOCH_BLOCKS) {
            logger.info("🌍 Running Master Epoch Slice: '{}' (Years {} -> {})",
                    scenarioDef.displayName(), scenarioDef.startYear(), scenarioDef.endYear());
            results.add(runScenarioCalibration(scenarioDef));
        }
        return results;
    }

    /**
     * Evaluates marginal sensitivity and CPU cost of Tier 2 & World3 hybrid engines.
     *
     * @return list of {@link EngineAblationAuditEntry} audit records
     */
    public static List<EngineAblationAuditEntry> runPluggableEngineAndWorld3AblationAudit() {
        return HistoricalCalibrationSensitivityHelper.runPluggableEngineAndWorld3AblationAudit();
    }

    /**
     * Discovers all authentic 20-year epochs present on disk intersecting {@code [startYear, endYear]}.
     *
     * @param startYear inclusive scenario start year
     * @param endYear   inclusive scenario end year
     * @return chronologically sorted list of available epoch years
     */
    public static List<Integer> discoverAvailableDiskEpochs(int startYear, int endYear) {
        return HistoricalCalibrationSimulationExecutor.discoverAvailableDiskEpochs(startYear, endYear);
    }

    /**
     * Executes single-scenario before-after calibration, intermediate checkpoint checks, and drift analysis.
     *
     * @param def calibration scenario definition
     * @return scenario calibration result
     */
    public static ScenarioCalibrationResult runScenarioCalibration(CalibrationScenarioDefinition def) {
        return HistoricalCalibrationSimulationExecutor.runScenarioCalibration(def);
    }

    /**
     * Executes multi-scale sensitivity sweep across spatial, temporal, and cohort dimensions.
     *
     * @param def calibration scenario definition
     * @return multi-scale sensitivity matrix
     */
    public static MultiScaleSensitivityMatrix runMultiScaleSensitivityMatrix(CalibrationScenarioDefinition def) {
        return HistoricalCalibrationSensitivityHelper.runMultiScaleSensitivityMatrix(def);
    }

    /**
     * Generates a comprehensive standardized academic markdown calibration and falsification report.
     *
     * @param results     executed scenario calibration results
     * @param sensitivity multi-scale sensitivity matrix
     * @return formatted Markdown report
     */
    public static String generateMarkdownCalibrationReport(List<ScenarioCalibrationResult> results, MultiScaleSensitivityMatrix sensitivity) {
        return HistoricalCalibrationReportGenerator.generateMarkdownCalibrationReport(results, sensitivity);
    }

    /**
     * CLI and Cloud execution entry point for full academic calibration and falsification pipeline.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        logger.info("===============================================================================");
        logger.info("🚀 STARTING ETHER CLIODYNAMIC ENGINE ACADEMIC CALIBRATION & FALSIFICATION SUITE");
        logger.info("===============================================================================");

        List<ScenarioCalibrationResult> combined = runFullScientificValidationSuite();
        MultiScaleSensitivityMatrix sensitivity = runMultiScaleSensitivityMatrix(CALIBRATION_SCENARIOS.get(1));
        String report = generateMarkdownCalibrationReport(combined, sensitivity);

        logger.info("\n{}", report);

        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of("docs/SCIENTIFIC_CALIBRATION_REPORT.md"),
                    report
            );
            logger.info("📄 Academic Calibration Report successfully saved to docs/SCIENTIFIC_CALIBRATION_REPORT.md");
        } catch (Exception ex) {
            logger.warn("Could not write report to docs/: {}", ex.getMessage());
        }

        logger.info("🏛 Academic Calibration & Epistemic Falsification Suite Completed Successfully.");
    }
}
