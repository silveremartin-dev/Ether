/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.model.Scenario;
import org.ether.society.persistence.EtherJsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * <h1>EtherFutureCalibrationRunner</h1>
 * <p>
 * Operational execution runner for the flagship 2025-2200 CE Earth biophysical
 * calibration simulation ({@link EtherFutureCalibrationScenarioHarness}). Executes
 * the scenario headlessly on the H3 planetary mesh, persists spatial snapshots to
 * {@code saves/}, and exports the calibrated 1D projection time series to
 * {@code data/variables/projections/ether_calibrated_projection_1d.json}.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 * @see EtherFutureCalibrationScenarioHarness
 * @see HeadlessBatchRunner
 */
public final class EtherFutureCalibrationRunner {

    private static final Logger logger = LoggerFactory.getLogger(EtherFutureCalibrationRunner.class);

    private EtherFutureCalibrationRunner() {
        // Utility runner class
    }

    /**
     * Executes the full 2025-2200 monthly calibration simulation, persists snapshots in {@code saves/},
     * and exports continuous 1D telemetry to {@code targetOutputFile}.
     *
     * @param targetOutputFile destination JSON file for the 1D calibrated time series
     * @return recorded {@link SimulationRunRecord} produced by the headless run
     * @throws Exception if execution or persistence encounters an error
     */
    public static SimulationRunRecord executeCalibrationRun(File targetOutputFile) throws Exception {
        logger.info("Initializing Ether Planetary Calibration Run (2025 -> 2200 CE, H3 Res 4, Monthly)...");
        Scenario scenario = EtherFutureCalibrationScenarioHarness.createCalibrationScenario();

        String endYearOverride = System.getProperty("future.calibration.endYear");
        if (endYearOverride != null && !endYearOverride.isBlank()) {
            scenario.setEndDateYear(Integer.parseInt(endYearOverride.trim()));
        }
        String resOverride = System.getProperty("future.calibration.resolution");
        if (resOverride != null && !resOverride.isBlank()) {
            scenario.setH3Resolution(Integer.parseInt(resOverride.trim()));
        }

        int totalTicks = scenario.calculateScenarioTicks();
        logger.info("Executing {} planned monthly ticks ({} -> {} CE, H3 Res {})...",
                totalTicks, scenario.getStartDateYear(), scenario.getEndDateYear(), scenario.getH3Resolution());

        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(scenario);
        if (record == null || record.getTimeSeriesData() == null || record.getTimeSeriesData().isEmpty()) {
            throw new IllegalStateException("Future calibration run returned empty telemetry record.");
        }

        Map<String, Map<String, Double>> variables = new LinkedHashMap<>();
        String[] exportedKeys = {
                "worldPopulation", "grossWorldProduct", "primaryEnergy",
                "co2Concentration", "temperatureAnomaly", "urbanizationRate",
                "fossilSharePrimaryEnergy", "literacyRate", "lifeExpectancyAtBirth"
        };
        for (String key : exportedKeys) {
            variables.put(key, new TreeMap<>());
        }

        for (Map.Entry<Integer, SimulationRunRecord.MetricSnapshot> entry : record.getTimeSeriesData().entrySet()) {
            String yrKey = String.valueOf(entry.getKey());
            SimulationRunRecord.MetricSnapshot snap = entry.getValue();
            Map<String, Double> m = snap.getMetricsMap();

            double popMillions = snap.getPopulation() / 1_000_000.0;
            variables.get("worldPopulation").put(yrKey, Math.round(popMillions * 100.0) / 100.0);

            if (m != null) {
                putRoundedIfPresent(variables.get("grossWorldProduct"), yrKey, m, "grossWorldProduct", "gdpTotal");
                putRoundedIfPresent(variables.get("primaryEnergy"), yrKey, m, "primaryEnergy", "energyCaptured");
                putRoundedIfPresent(variables.get("co2Concentration"), yrKey, m, "co2Concentration", "co2Ppm");
                putRoundedIfPresent(variables.get("temperatureAnomaly"), yrKey, m, "temperatureAnomaly", "tempAnomaly");
                putRoundedIfPresent(variables.get("urbanizationRate"), yrKey, m, "urbanizationRate", "urbanShare");
                putRoundedIfPresent(variables.get("fossilSharePrimaryEnergy"), yrKey, m, "fossilSharePrimaryEnergy", "fossilShare");
                putRoundedIfPresent(variables.get("literacyRate"), yrKey, m, "literacyRate", "literacy");
                putRoundedIfPresent(variables.get("lifeExpectancyAtBirth"), yrKey, m, "lifeExpectancyAtBirth", "lifeExpectancy");
            }
        }

        variables.entrySet().removeIf(e -> e.getValue().isEmpty());

        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("modelId", "ETHER_CALIBRATED");
        metadata.put("name", "Ether Biophysical Coupled Earth Simulation (Calibrated 2025-2200)");
        metadata.put("agency", "Ether Planetary Epistemic Laboratory");
        metadata.put("scenario", "Coupled Thermodynamic-Cliodynamic Continuity Run");
        metadata.put("description", "Endogenous simulation trajectory calibrated on ERA5, HYDE 3.4, UN WPP 2024 and Maddison historical data.");
        metadata.put("dimension", String.format("1D (Scalar Time-Series Projections %d-%d)",
                scenario.getStartDateYear(), scenario.getEndDateYear()));
        payload.put("metadata", metadata);
        payload.put("variables", variables);

        if (targetOutputFile.getParentFile() != null) {
            targetOutputFile.getParentFile().mkdirs();
        }
        EtherJsonHelper.writePrettyAtomic(targetOutputFile.toPath(), payload);
        logger.info("Calibrated 1D projection successfully written to: {} (runId={})",
                targetOutputFile.getAbsolutePath(), record.getRunId());
        return record;
    }

    private static void putRoundedIfPresent(Map<String, Double> target, String yrKey,
                                            Map<String, Double> metrics, String primaryKey, String fallbackKey) {
        Double val = metrics.get(primaryKey);
        if (val == null) {
            val = metrics.get(fallbackKey);
        }
        if (val != null && Double.isFinite(val)) {
            target.put(yrKey, Math.round(val * 1000.0) / 1000.0);
        }
    }

    /**
     * CLI entry point executing the 2025-2200 future calibration run.
     *
     * @param args optional first argument specifying target output JSON path
     */
    public static void main(String[] args) {
        try {
            File out = args.length > 0 ? new File(args[0]) : EtherFutureCalibrationScenarioHarness.getTelemetryOutputFile();
            executeCalibrationRun(out);
        } catch (Exception e) {
            logger.error("Failed executing calibration run", e);
            System.exit(1);
        }
    }
}
