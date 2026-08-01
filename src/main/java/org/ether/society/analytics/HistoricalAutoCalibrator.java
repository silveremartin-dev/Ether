/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.database.H3Cell;
import org.ether.society.procedural.ProceduralEngineRegistry;
import org.ether.society.procedural.typeb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Historical Auto-Calibration & Self-Correcting Empirical Validation Kernel.
 * Systematically tests Ether baseline vs various Type B procedural engine configurations
 * against historical reference telemetry (10,000 BCE - 2026 CE).
 *
 * Computes:
 * 1. <b>RMSE</b> (Root Mean Square Error)
 * 2. <b>R^2</b> (Coefficient of Determination)
 * 3. <b>Drift Analysis</b>: Identifies historical epochs where simulation strays from reality.
 * 4. <b>Auto-Correction</b>: Automatically ranks and selects the optimal Type B plugin set.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class HistoricalAutoCalibrator {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalAutoCalibrator.class);

    public static class CalibrationResult {
        public String configurationName;
        public List<String> activePlugins;
        public double rmse;
        public double rSquared;
        public double maxEpochDriftPercent;
        public int worstDriftEpochYear;
        public Map<Integer, Double> simulatedTrajectory;

        public CalibrationResult(String name, List<String> plugins) {
            this.configurationName = name;
            this.activePlugins = new ArrayList<>(plugins);
            this.simulatedTrajectory = new HashMap<>();
        }

        @Override
        public String toString() {
            return String.format(
                "Calibration Result [%s]: RMSE=%.2f, R^2=%.4f, Max Drift=%.1f%% (in Year %d), Active Plugins=%d",
                configurationName, rmse, rSquared, maxEpochDriftPercent, worstDriftEpochYear, activePlugins.size()
            );
        }
    }

    /**
     * Executes auto-calibration across candidate configurations and returns the optimal best-fit result.
     */
    public static CalibrationResult evaluateAndAutoCalibrate() {
        Map<Integer, Double> benchmarkData = HistoricalValidationKernel.getHistoricalWorldPopulation();
        List<CalibrationResult> candidateResults = new ArrayList<>();

        // Candidate 1: Pure Ether Baseline (No Type B plugins)
        CalibrationResult baselineResult = runTrajectoryEvaluation("Ether Core Baseline", List.of(), benchmarkData);
        candidateResults.add(baselineResult);

        // Candidate 2: Basic Cliodynamic Type B (Turchin Asabiyyah + Monastic Buffer + Lenski)
        CalibrationResult basicCliodynamic = runTrajectoryEvaluation("Basic Cliodynamic Hybrid",
            List.of("FrontierAsabiyyah", "MonasticBuffer", "Lenski"), benchmarkData);
        candidateResults.add(basicCliodynamic);

        // Candidate 3: Industrial & Modern Type B (World3 + Nordhaus DICE + Protestant Ethic + Smil)
        CalibrationResult modernHybrid = runTrajectoryEvaluation("Modern Thermodynamic Hybrid",
            List.of("World3", "NordhausDICE", "ProtestantWorkEthic", "SmilMaterial"), benchmarkData);
        candidateResults.add(modernHybrid);

        // Candidate 4: Full Historical Type B Suite (All empirical historical engines)
        CalibrationResult fullSuite = runTrajectoryEvaluation("Full Empirical Type B Suite",
            List.of("FrontierAsabiyyah", "MonasticBuffer", "ProtestantWorkEthic", "FertileCrescent", "AmerindianEcosystem", "RomanCliodynamics", "EdoJapan"), benchmarkData);
        candidateResults.add(fullSuite);

        // Rank configurations by R^2 (descending) and RMSE (ascending)
        candidateResults.sort((a, b) -> Double.compare(b.rSquared, a.rSquared));

        CalibrationResult bestFit = candidateResults.get(0);
        logger.info("🏆 Optimal Auto-Calibrated Configuration: '{}' with R^2 = {}, RMSE = {}",
            bestFit.configurationName, bestFit.rSquared, bestFit.rmse);

        return bestFit;
    }

    /**
     * Runs simulation trajectory evaluation for a specific configuration of plugins against benchmark data.
     */
    public static CalibrationResult runTrajectoryEvaluation(String configName, List<String> pluginNames, Map<Integer, Double> benchmarkData) {
        CalibrationResult result = new CalibrationResult(configName, pluginNames);

        // Prepare test H3 cell representing world aggregate population density
        H3Cell cell = new H3Cell(613503380827930701L, 30.0, 30.0);
        cell.setPopulation(4); // 4 Million at -10,000 BCE
        cell.setResourceCapital(10.0);
        cell.setTechnologyLevel(1.0);
        cell.setFoodResource(100.0);

        List<H3Cell> cells = List.of(cell);

        // Clear and register requested plugins
        ProceduralEngineRegistry.clearPlugins();
        for (String pName : pluginNames) {
            registerByName(pName);
        }

        // Simulate step-by-step across historical benchmark years (-10,000 BCE to 2026 CE)
        List<Integer> benchmarkYears = new ArrayList<>(benchmarkData.keySet());
        Collections.sort(benchmarkYears);

        int currentYear = benchmarkYears.get(0);
        result.simulatedTrajectory.put(currentYear, (double) cell.getPopulation());

        for (int i = 1; i < benchmarkYears.size(); i++) {
            int nextYear = benchmarkYears.get(i);
            int deltaYears = nextYear - currentYear;

            // Execute Ether core tick + pluggable engines
            simulateEpochStep(cells, currentYear, deltaYears);

            result.simulatedTrajectory.put(nextYear, (double) cell.getPopulation());
            currentYear = nextYear;
        }

        // Calculate statistical fit metrics
        result.rmse = HistoricalValidationKernel.calculateRmse(result.simulatedTrajectory);
        result.rSquared = HistoricalValidationKernel.calculateRSquared(result.simulatedTrajectory);

        // Analyze drift per epoch ("Où sont les dérives")
        analyzeEpochDrift(result, benchmarkData);

        // Reset registry after test
        ProceduralEngineRegistry.clearPlugins();

        return result;
    }

    private static void simulateEpochStep(List<H3Cell> cells, int startYear, int deltaYears) {
        H3Cell cell = cells.get(0);
        double pop = cell.getPopulation();

        // Baseline logistic growth with technology/era factor
        double growthRate = 0.0003 + 0.00005 * (cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0);

        // Industrial acceleration after 1800 CE
        if (startYear >= 1800) {
            growthRate += 0.008;
            cell.setTechnologyLevel((cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0) + 0.05 * (deltaYears / 10.0));
        }

        double newPop = pop * Math.exp(growthRate * Math.min(100, deltaYears));
        cell.setPopulation((int) newPop);

        // Execute active registered plugins
        ProceduralEngineRegistry.processPlugins(cells, deltaYears);
    }

    private static void registerByName(String name) {
        switch (name) {
            case "FrontierAsabiyyah" -> ProceduralEngineRegistry.registerPlugin("FrontierAsabiyyah", FrontierAsabiyyahEngine::processHybrid);
            case "MonasticBuffer" -> ProceduralEngineRegistry.registerPlugin("MonasticBuffer", MonasticDemographicBufferEngine::processHybrid);
            case "Lenski" -> ProceduralEngineRegistry.registerPlugin("Lenski", LenskiPureEngine::processHybrid);
            case "World3" -> ProceduralEngineRegistry.registerPlugin("World3", World3HybridEngine::processPlugin);
            case "NordhausDICE" -> ProceduralEngineRegistry.registerPlugin("NordhausDICE", NordhausDiceHybridEngine::processPlugin);
            case "ProtestantWorkEthic" -> ProceduralEngineRegistry.registerPlugin("ProtestantWorkEthic", ProtestantWorkEthicEngine::processHybrid);
            case "SmilMaterial" -> ProceduralEngineRegistry.registerPlugin("SmilMaterial", SmilMaterialTransitionsPureEngine::processHybrid);
            case "FertileCrescent" -> ProceduralEngineRegistry.registerPlugin("FertileCrescent", FertileCrescentSalinizationEngine::processHybrid);
            case "AmerindianEcosystem" -> ProceduralEngineRegistry.registerPlugin("AmerindianEcosystem", AmerindianEcosystemEngine::processHybrid);
            case "RomanCliodynamics" -> ProceduralEngineRegistry.registerPlugin("RomanCliodynamics", RomanImperialCliodynamicEngine::processHybrid);
            case "EdoJapan" -> ProceduralEngineRegistry.registerPlugin("EdoJapan", EdoJapanIsolationEngine::processHybrid);
        }
    }

    private static void analyzeEpochDrift(CalibrationResult result, Map<Integer, Double> benchmarkData) {
        double maxDrift = 0.0;
        int worstYear = 2026;

        for (Map.Entry<Integer, Double> entry : benchmarkData.entrySet()) {
            int year = entry.getKey();
            double observed = entry.getValue();
            Double simulated = result.simulatedTrajectory.get(year);

            if (simulated != null && observed > 0) {
                double driftPercent = (Math.abs(simulated - observed) / observed) * 100.0;
                if (driftPercent > maxDrift) {
                    maxDrift = driftPercent;
                    worstYear = year;
                }
            }
        }

        result.maxEpochDriftPercent = maxDrift;
        result.worstDriftEpochYear = worstYear;
    }
}
