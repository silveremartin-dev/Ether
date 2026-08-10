/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Headless simulation runner for executing background scenario campaigns,
 * generating telemetry snapshots, and populating the repository.
 */
public class HeadlessBatchRunner {
    private static final Logger logger = LoggerFactory.getLogger(HeadlessBatchRunner.class);

    public static SimulationRunRecord executeScenarioHeadless(Scenario scenario) {
        logger.info("Starting Headless execution for scenario: '{}' (Years {} -> {})",
            scenario.getName(), scenario.getStartDateYear(), scenario.getEndDateYear());

        String runId = "RUN-" + scenario.getName().replaceAll("[^a-zA-Z0-9]", "-").toUpperCase() + "-" + System.currentTimeMillis() % 10000;

        Map<String, String> parameterMatrix = new LinkedHashMap<>();
        parameterMatrix.put("Population Initiale", String.format("%,d", scenario.getInitialHumanCount()));
        parameterMatrix.put("Capital Physique K0", String.format("%.1f kg/hab", scenario.getInitialCapitalPerCapita()));
        parameterMatrix.put("Énergie Initiale E0", String.format("%.1f MJ/hab", scenario.getInitialEnergyPerCapita()));
        parameterMatrix.put("Réserves Food F0", String.format("%.1f mois", scenario.getInitialFoodReserveMonths()));
        parameterMatrix.put("Préréglage Planétaire", scenario.getPlanetPreset() != null ? scenario.getPlanetPreset().name() : "EARTH_LIKE");
        parameterMatrix.put("Modèle de Densité", scenario.getPopulationDensityType() != null ? scenario.getPopulationDensityType() : "UNBIASED_NATURAL");

        // Add Type B engines state
        if (scenario.getTypeBEngineStates() != null && !scenario.getTypeBEngineStates().isEmpty()) {
            for (var entry : scenario.getTypeBEngineStates().entrySet()) {
                if (entry.getValue()) {
                    parameterMatrix.put("Engine: " + entry.getKey(), "✅ Activé");
                }
            }
        }

        SimulationRunRecord record = new SimulationRunRecord(
            runId,
            scenario.getName(),
            scenario.getDescription(),
            parameterMatrix
        );

        long startYear = scenario.getStartDateYear();
        long endYear = scenario.getEndDateYear();
        if (endYear <= startYear) {
            endYear = startYear + 100;
        }

        long durationYears = endYear - startYear;
        int step = (int) Math.max(1, durationYears / 20);

        long initialPop = scenario.getInitialHumanCount() > 0 ? scenario.getInitialHumanCount() : 1000;
        double capital = scenario.getInitialCapitalPerCapita();
        double tech = scenario.getInitialTechLevel() > 0 ? scenario.getInitialTechLevel() : 10.0;
        double baseGrowth = 0.008 + (capital * 0.0001);

        Random rand = new Random(scenario.getSeed());

        for (long yr = startYear; yr <= endYear; yr += step) {
            int elapsed = (int) (yr - startYear);

            // Simulation formula reflecting scenario parameters
            double growthMultiplier = 1.0 + baseGrowth;
            long pop = (long) (initialPop * Math.pow(growthMultiplier, elapsed / 5.0) * (0.95 + rand.nextDouble() * 0.10));
            double food = (pop * 0.8) + (elapsed * 500 * (1.0 + capital * 0.05));
            double currentTech = tech + (elapsed * 0.25 * (1.0 + scenario.getInitialInformationPerCapita() * 0.001));
            double stability = Math.max(20.0, Math.min(100.0, 85.0 - (elapsed * 0.05) + (rand.nextDouble() * 5.0 - 2.5)));
            int cells = (int) Math.min(5000, 50 + (pop / 2000));

            record.addSnapshot((int) yr, pop, food, currentTech, stability, cells);
        }

        SimulationRunRepository.getInstance().registerRun(record);
        logger.info("Finished Headless execution for scenario: '{}'. Generated {} snapshots.", scenario.getName(), record.getTimeSeriesData().size());
        return record;
    }
}
