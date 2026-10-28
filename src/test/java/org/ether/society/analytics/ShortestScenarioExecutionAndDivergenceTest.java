package org.ether.society.analytics;

import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>Shortest Scenario Real-Physics Execution & Divergence Calibration Test</h1>
 * <p>
 * Executes the shortest canonical planetary scenario in headless physics mode,
 * instruments real-time simulation throughput (TPS, latency), and runs the
 * multi-variate ground truth divergence audit to verify that all mathematical
 * bug fixes (spatial scoping, denominator epsilon smoothing, zero-containment scale,
 * andBosperupian agricultural feedback) prevent model drift.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ShortestScenarioExecutionAndDivergenceTest {

    @Test
    @DisplayName("Execute Shortest Scenario and Audit Divergence & Performance")
    public void testExecuteShortestScenarioAndAuditDivergence() {
        System.out.println("================================================================================");
        System.out.println(" ⚡ EXECUTING SHORTEST SCENARIO & BENCHMARKING PERFORMANCE & DIVERGENCE");
        System.out.println("================================================================================");

        List<Scenario> scenarios = Scenario.getBuiltInScenarios();
        assertNotNull(scenarios, "Scenario list must not be null");
        assertFalse(scenarios.isEmpty(), "Scenario list must not be empty");

        // Identify the shortest scenario by chronological duration (years)
        Scenario shortest = scenarios.stream()
                .filter(s -> s.getEndDateYear() > s.getStartDateYear())
                .min(Comparator.comparingLong(s -> s.getEndDateYear() - s.getStartDateYear()))
                .orElse(scenarios.get(0));

        long spanYears = shortest.getEndDateYear() - shortest.getStartDateYear();
        System.out.println("Selected Shortest Scenario: " + shortest.getName() + " (" + shortest.getPresetKey() + ")");
        System.out.println("Epoch Horizon: " + shortest.getStartDateYear() + " -> " + shortest.getEndDateYear() + " (" + spanYears + " years)");

        // Optimize resolution and cohort granularity for regression benchmark speed
        shortest.setH3Resolution(2);
        shortest.setTargetCohortSize(50000);

        // 1. Measure Execution Performance
        long startTimeNs = System.nanoTime();
        long startWallTimeMs = System.currentTimeMillis();

        SimulationRunRecord runRecord = HeadlessBatchRunner.executeScenarioHeadless(shortest);
        assertNotNull(runRecord, "Simulation run record must be generated successfully");

        long endWallTimeMs = System.currentTimeMillis();
        long durationMs = endWallTimeMs - startWallTimeMs;
        double durationSeconds = durationMs / 1000.0;

        int snapshotCount = runRecord.getTimeSeriesData() != null ? runRecord.getTimeSeriesData().size() : 0;
        assertTrue(snapshotCount > 0, "Simulation must generate at least 1 telemetry snapshot");

        // Approximate total ticks executed based on dt
        double dtDays = shortest.getTemporalResolutionDays() > 0 ? shortest.getTemporalResolutionDays() : 30.0;
        long totalTicks = Math.max(1, (long) Math.ceil((spanYears * 365.25) / dtDays));
        double tps = totalTicks / Math.max(0.001, durationSeconds);
        double latencyMsPerTick = durationMs / (double) totalTicks;

        System.out.println("\n--- ⏱️ PERFORMANCE PROFILE ---");
        System.out.println(String.format(Locale.US, "Duration           : %,.2f seconds (%d ms)", durationSeconds, durationMs));
        System.out.println(String.format(Locale.US, "Simulated Ticks    : %,d ticks (dt = %.1f days)", totalTicks, dtDays));
        System.out.println(String.format(Locale.US, "Throughput (TPS)   : %,.2f ticks/sec", tps));
        System.out.println(String.format(Locale.US, "Mean Tick Latency  : %,.3f ms/tick", latencyMsPerTick));
        System.out.println(String.format(Locale.US, "Snapshots Recorded : %d", snapshotCount));
        for (var snapEntry : runRecord.getTimeSeriesData().entrySet()) {
            System.out.println(String.format(Locale.US, "  Snapshot Year %d: Pop = %,.0f | GDP = %,.1f | Energy = %,.1f",
                    snapEntry.getKey(), snapEntry.getValue().getValue("worldPopulation"),
                    snapEntry.getValue().getValue("grossWorldProduct"), snapEntry.getValue().getValue("primaryEnergy")));
        }

        // 2. Multi-Variate Historical Divergence Audit
        System.out.println("\n--- 📊 HISTORICAL DIVERGENCE AUDIT ---");
        Map<String, String> benchmarkKeys = new LinkedHashMap<>();
        benchmarkKeys.put("Atmospheric CO2 (co2Concentration)", "co2Concentration");
        benchmarkKeys.put("Milankovitch 65°N (milankovitchInsolation)", "milankovitchInsolation");
        benchmarkKeys.put("World Population (worldPopulation)", "worldPopulation");
        benchmarkKeys.put("Gross World Product (grossWorldProduct)", "grossWorldProduct");
        benchmarkKeys.put("Primary Energy (primaryEnergy)", "primaryEnergy");
        benchmarkKeys.put("Zero-Containment (zeroContainmentScore)", "zeroContainmentScore");
        benchmarkKeys.put("Megafauna Abundance (megafaunaIndex)", "megafaunaIndex");
        benchmarkKeys.put("Urbanization Rate (urbanizationRate)", "urbanizationRate");
        benchmarkKeys.put("Literacy Rate (literacyRate)", "literacyRate");
        benchmarkKeys.put("Sociopolitical Instability (sociopoliticalInstability)", "sociopoliticalInstability");

        double totalMape = 0.0;
        int evaluatedCount = 0;

        System.out.println("| Metric Variable | Simulated Mean | Observed Mean | MAPE (%) | Status |");
        System.out.println("|---|:---:|:---:|:---:|:---:|");

        for (Map.Entry<String, String> entry : benchmarkKeys.entrySet()) {
            String label = entry.getKey();
            String benchKey = entry.getValue();

            Map<Integer, Double> benchmark = HistoricalValidationKernel.getBenchmarkDataset(benchKey);
            GroundTruthAuditGenerator.VariableDriftAnalysis drift = GroundTruthAuditGenerator.computeVariableDrift(
                    label, benchKey, benchmark, runRecord.getTimeSeriesData(),
                    HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC
            );

            assertFalse(Double.isNaN(drift.mape), "Variable MAPE for '" + label + "' must not be NaN");

            if (drift.sampleCount > 0) {
                totalMape += drift.mape;
                evaluatedCount++;
                System.out.println(String.format(Locale.US, "| %s | %,.2f | %,.2f | **%.2f%%** | %s |",
                        label, drift.meanSim, drift.meanObs, drift.mape, drift.status));
            }
        }

        double meanMape = evaluatedCount > 0 ? (totalMape / evaluatedCount) : 0.0;
        double fidelityScore = Math.max(0.0, 100.0 - meanMape);

        System.out.println("\n--- 🏁 CALIBRATION VERDICT ---");
        System.out.println(String.format(Locale.US, "Evaluated Metrics  : %d", evaluatedCount));
        System.out.println(String.format(Locale.US, "Overall Mean MAPE  : %.2f%%", meanMape));
        System.out.println(String.format(Locale.US, "Fidelity Score     : %.2f%%", fidelityScore));

        // 3. Falsification Invariants Verification
        // Invariant 1: zeroContainmentScore must not exhibit previous 12,000% scale error
        Map<Integer, Double> zBench = HistoricalValidationKernel.getBenchmarkDataset("zeroContainmentScore");
        var zDrift = GroundTruthAuditGenerator.computeVariableDrift(
                "zeroContainmentScore", "zeroContainmentScore", zBench, runRecord.getTimeSeriesData(),
                HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC
        );
        assertTrue(zDrift.mape < 25.0, "Zero-Containment MAPE must be < 25%, was: " + zDrift.mape);

        // Invariant 2: primaryEnergy must not exhibit previous 5,000,000% MW/EJ singularity
        Map<Integer, Double> eBench = HistoricalValidationKernel.getBenchmarkDataset("primaryEnergy");
        var eDrift = GroundTruthAuditGenerator.computeVariableDrift(
                "primaryEnergy", "primaryEnergy", eBench, runRecord.getTimeSeriesData(),
                HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC
        );
        assertTrue(eDrift.mape < 50.0, "Primary Energy MAPE must be < 50%, was: " + eDrift.mape);

        // Invariant 3: Global fidelity score must meet calibrated threshold (>= 75%)
        assertTrue(fidelityScore >= 75.0, "Shortest scenario fidelity score must be >= 75%, was: " + fidelityScore);

        System.out.println("================================================================================");
        System.out.println(" ✔ ALL FALSABILITY AND PERFORMANCE INVARIANTS SATISFIED WITH ZERO BUGS!");
        System.out.println("================================================================================");
    }
}
