/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository for offline simulation telemetry runs and historical benchmarks.
 */
public class SimulationRunRepository {
    private static final Logger logger = LoggerFactory.getLogger(SimulationRunRepository.class);
    private static SimulationRunRepository instance;

    private final Map<String, SimulationRunRecord> repository = new ConcurrentHashMap<>();

    private SimulationRunRepository() {
        seedPreRecordedHistoricalBenchmarks();
    }

    public static synchronized SimulationRunRepository getInstance() {
        if (instance == null) {
            instance = new SimulationRunRepository();
        }
        return instance;
    }

    public void registerRun(SimulationRunRecord record) {
        repository.put(record.getRunId(), record);
        logger.info("Registered simulation run in repository: {} ({})", record.getScenarioName(), record.getRunId());
    }

    public SimulationRunRecord getRun(String runId) {
        return repository.get(runId);
    }

    public List<SimulationRunRecord> getAllRuns() {
        return new ArrayList<>(repository.values());
    }

    public Optional<SimulationRunRecord> getRunByScenarioName(String scenarioName) {
        if (scenarioName == null) return Optional.empty();
        return repository.values().stream()
            .filter(r -> r.getScenarioName().equalsIgnoreCase(scenarioName))
            .findFirst();
    }

    public boolean hasRunForScenario(String scenarioName) {
        return getRunByScenarioName(scenarioName).isPresent();
    }

    /**
     * Seeds the repository with rich pre-recorded historical benchmark runs (e.g., Empire Romain variations)
     * so that offline comparative analysis can be tested immediately out-of-the-box.
     */
    private void seedPreRecordedHistoricalBenchmarks() {
        // Run A: Empire Romain An 0 (Baseline Reference)
        Map<String, String> paramsA = new LinkedHashMap<>();
        paramsA.put("Capitale", "Rome (Lat 41.9°, Lng 12.5°)");
        paramsA.put("GpuHydroEngine", "Désactivé");
        paramsA.put("EpidemicModule", "Désactivé");
        paramsA.put("DensitéInitiale", "1.0x (Standard)");

        SimulationRunRecord runA = new SimulationRunRecord(
            "RUN-ROMAN-BASELINE-01",
            "Empire Romain An 0 (Référence Standard)",
            "Configuration canonique sans modules optionnels",
            paramsA
        );

        // Run B: Empire Romain An 0 (Module Hydro GPU & Irrigation)
        Map<String, String> paramsB = new LinkedHashMap<>();
        paramsB.put("Capitale", "Rome (Lat 41.9°, Lng 12.5°)");
        paramsB.put("GpuHydroEngine", "✅ Activé (Débit d'irrigation +40%)");
        paramsB.put("EpidemicModule", "Désactivé");
        paramsB.put("DensitéInitiale", "1.0x (Standard)");

        SimulationRunRecord runB = new SimulationRunRecord(
            "RUN-ROMAN-GPUHYDRO-02",
            "Empire Romain An 0 (Alternative Hydro GPU)",
            "Activation de l'irrigation accélérée GPU sur le bassin méditerranéen",
            paramsB
        );

        // Run C: Empire Romain An 0 (Relocalisation & Épidémie)
        Map<String, String> paramsC = new LinkedHashMap<>();
        paramsC.put("Capitale", "Alexandrie (Lat 31.2°, Lng 29.9°)");
        paramsC.put("GpuHydroEngine", "Désactivé");
        paramsC.put("EpidemicModule", "⚠️ Activé (Vague Peste de Galien à T=35)");
        paramsC.put("DensitéInitiale", "1.25x (+25% sud)");

        SimulationRunRecord runC = new SimulationRunRecord(
            "RUN-ROMAN-RELOC-EPIDEMIC-03",
            "Empire Romain An 0 (Capitale Alexandrie & Épidémie)",
            "Déplacement de la capitale et déclenchement d'un choc épidémique",
            paramsC
        );

        // Populate time series data (Years 0 to 100)
        for (int year = 0; year <= 100; year += 5) {
            // Run A Baseline
            long popA = (long) (1_000_000 * Math.pow(1.008, year));
            double foodA = 800_000 + year * 1500;
            double techA = 15.0 + year * 0.35;
            double stabA = 85.0 - year * 0.15;
            runA.addSnapshot(year, popA, foodA, techA, stabA, 120 + year * 2);

            // Run B (GPU Hydro: +agricultural yield & faster pop growth)
            long popB = (long) (1_000_000 * Math.pow(1.014, year));
            double foodB = 1_200_000 + year * 4500;
            double techB = 15.0 + year * 0.42;
            double stabB = 90.0 - year * 0.05;
            runB.addSnapshot(year, popB, foodB, techB, stabB, 140 + year * 3);

            // Run C (Epidemic at T=35)
            double dip = (year >= 35 && year <= 55) ? 0.70 : 0.95;
            long popC = (long) (1_100_000 * Math.pow(1.006, year) * dip);
            double foodC = 750_000 + year * 1100;
            double techC = 15.0 + year * 0.28;
            double stabC = (year >= 35 && year <= 55) ? 45.0 : 70.0;
            runC.addSnapshot(year, popC, foodC, techC, stabC, 110 + year);
        }

        registerRun(runA);
        registerRun(runB);
        registerRun(runC);
    }
}
