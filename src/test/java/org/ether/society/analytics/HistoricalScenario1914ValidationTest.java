package org.ether.society.analytics;

import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>Historical Scenario 1914-1960 Execution & Divergence Test</h1>
 * <p>
 * Evaluates the 20th-century historical scenario (World Wars, Totalitarian Crisis & Nuclear Dawn: 1914-1960)
 * against real empirical ground truth (Maddison Project, UN WPP, Law Dome Ice Cores, Vaclav Smil, UNESCO).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class HistoricalScenario1914ValidationTest {

    @Test
    @DisplayName("Verify Historical 1914-1960 Simulation Fidelity & Empirical Ground Truth Invariance")
    public void test1914WorldWarsHistoricalFidelity() {
        System.out.println("================================================================================");
        System.out.println(" ⚔️ EXECUTING HISTORICAL SCENARIO: 1914-1960 WORLD WARS & NUCLEAR DAWN");
        System.out.println("================================================================================");

        List<Scenario> allScenarios = Scenario.getBuiltInScenarios();
        Scenario s1914 = allScenarios.stream()
                .filter(s -> s.getPresetKey() != null && s.getPresetKey().contains("1914"))
                .findFirst()
                .orElse(null);

        assertNotNull(s1914, "Historical 1914 scenario must be present in built-in scenarios catalog");
        System.out.println("Selected Scenario : " + s1914.getName() + " (" + s1914.getPresetKey() + ")");
        System.out.println("Epoch Horizon     : " + s1914.getStartDateYear() + " -> " + s1914.getEndDateYear() + " (" + (s1914.getEndDateYear() - s1914.getStartDateYear()) + " years)");

        long startTimeNs = System.nanoTime();
        long startWallTimeMs = System.currentTimeMillis();

        SimulationRunRecord runRecord = HeadlessBatchRunner.executeScenarioHeadless(s1914);
        assertNotNull(runRecord, "Simulation run record must be generated successfully");

        long durationMs = System.currentTimeMillis() - startWallTimeMs;
        double durationSeconds = durationMs / 1000.0;
        int snapshotCount = runRecord.getTimeSeriesData() != null ? runRecord.getTimeSeriesData().size() : 0;
        assertTrue(snapshotCount > 0, "Simulation must generate telemetry snapshots");

        double dtDays = s1914.getTemporalResolutionDays() > 0 ? s1914.getTemporalResolutionDays() : 30.0;
        long totalTicks = (long) Math.ceil(((s1914.getEndDateYear() - s1914.getStartDateYear()) * 365.25) / dtDays);
        double tps = totalTicks / Math.max(0.001, durationSeconds);
        double latencyMsPerTick = durationMs / (double) totalTicks;

        System.out.println("\n--- ⏱️ 1914-1960 HISTORICAL PERFORMANCE PROFILE ---");
        System.out.println(String.format(Locale.US, "Duration           : %,.2f seconds (%d ms)", durationSeconds, durationMs));
        System.out.println(String.format(Locale.US, "Simulated Ticks    : %,d ticks (dt = %.1f days)", totalTicks, dtDays));
        System.out.println(String.format(Locale.US, "Throughput (TPS)   : %,.2f ticks/sec", tps));
        System.out.println(String.format(Locale.US, "Mean Tick Latency  : %,.3f ms/tick", latencyMsPerTick));
        System.out.println(String.format(Locale.US, "Snapshots Recorded : %d", snapshotCount));

        for (var snapEntry : runRecord.getTimeSeriesData().entrySet()) {
            System.out.println(String.format(Locale.US, "  Snapshot Year %d: Pop = %,.0f | GDP = %,.1f | Energy = %,.1f | CO2 = %.1f",
                    snapEntry.getKey(), snapEntry.getValue().getValue("worldPopulation"),
                    snapEntry.getValue().getValue("grossWorldProduct"), snapEntry.getValue().getValue("primaryEnergy"),
                    snapEntry.getValue().getValue("co2Concentration")));
        }

        // Multi-Variate Historical Divergence Audit against Empirical Ground Truth
        System.out.println("\n--- 📊 HISTORICAL EMPIRICAL DIVERGENCE AUDIT (1914-1960) ---");
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

        System.out.println("\n--- 🏁 1914-1960 HISTORICAL CALIBRATION VERDICT ---");
        System.out.println(String.format(Locale.US, "Evaluated Metrics  : %d", evaluatedCount));
        System.out.println(String.format(Locale.US, "Overall Mean MAPE  : %.2f%%", meanMape));
        System.out.println(String.format(Locale.US, "Fidelity Score     : %.2f%%", fidelityScore));

        // Invariant: Simulation must execute stably with positive demographic rebound (> 1.4 billion)
        assertTrue(runRecord.getTimeSeriesData().get(1960).getPopulation() > 1_400_000_000L,
                "1960 population must remain vital and above 1.4 billion, was: " + runRecord.getTimeSeriesData().get(1960).getPopulation());
        assertTrue(fidelityScore >= 70.0, "Historical fidelity score must be >= 70%, was: " + fidelityScore);
    }
}
