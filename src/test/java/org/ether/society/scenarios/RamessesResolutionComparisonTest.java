package org.ether.society.scenarios;

import org.ether.society.analytics.HeadlessBatchRunner;
import org.ether.society.analytics.HistoricalValidationKernel;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Benchmark test executing Ramsès II specifically in Resolution 6 and Resolution 7.
 */
public class RamessesResolutionComparisonTest {

    @Test
    @DisplayName("Execute Ramses II at H3 Resolution 6")
    public void testRamessesResolution6() {
        runRamessesAtResolution(6);
    }

    @Test
    @DisplayName("Execute Ramses II at H3 Resolution 7")
    public void testRamessesResolution7() {
        runRamessesAtResolution(7);
    }

    private void runRamessesAtResolution(int resolution) {
        Scenario s = Scenario.getBuiltInScenarios().stream()
                .filter(sc -> "ramesses_ii".equalsIgnoreCase(sc.getPresetKey()))
                .findFirst()
                .orElse(null);

        assertNotNull(s, "Scenario ramesses_ii must exist");

        s.setStartDateYear(-1279);
        s.setEndDateYear(-1259); // 20 years
        s.setH3Resolution(resolution);
        s.setTargetCohortSize(50000);

        System.out.println("================================================================================");
        System.out.println(" 🏺 EXECUTING RAMSES II AT H3 RESOLUTION " + resolution + " (Regional Nile Window)");
        System.out.println("    Period: " + s.getStartDateYear() + " -> " + s.getEndDateYear() + " (20 years, monthly)");
        System.out.println("================================================================================");

        long startMs = System.currentTimeMillis();
        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(s);
        long elapsedMs = System.currentTimeMillis() - startMs;

        assertNotNull(record, "Simulation run record must be generated");
        var timeSeries = record.getTimeSeriesData();
        assertNotNull(timeSeries, "Time series must exist");
        assertFalse(timeSeries.isEmpty(), "Time series must contain snapshots");

        System.out.printf("  Completed in %,d ms across %d snapshots recorded.%n", elapsedMs, timeSeries.size());

        List<String> variablesToAudit = List.of(
                "worldPopulation",
                "grossWorldProduct",
                "primaryEnergy",
                "urbanizationRate",
                "sociopoliticalInstability"
        );

        System.out.println("  📊 VARIABLE TELEMETRY & STABILITY (RES " + resolution + "):");
        for (String varName : variablesToAudit) {
            List<Double> values = new ArrayList<>();
            List<Double> benchErrors = new ArrayList<>();

            for (var entry : timeSeries.entrySet()) {
                int yr = entry.getKey();
                var snap = entry.getValue();
                double val = snap.getValue(varName);
                if (!Double.isNaN(val)) {
                    values.add(val);
                    double bench = HistoricalValidationKernel.getInterpolatedBenchmarkValue(varName, yr);
                    if (!Double.isNaN(bench) && bench > 0.0) {
                        double simVal = ("worldPopulation".equals(varName) && val > 10000.0) ? (val / 1_000_000.0) : val;
                        double mape = Math.abs(simVal - bench) / bench * 100.0;
                        benchErrors.add(mape);
                    }
                }
            }

            if (values.size() < 3) continue;

            double first = values.get(0);
            double last = values.get(values.size() - 1);
            double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

            int signFlips = 0;
            double prevDelta = 0.0;
            for (int i = 1; i < values.size(); i++) {
                double delta = values.get(i) - values.get(i - 1);
                if (Math.abs(delta) > 1e-6) {
                    if (prevDelta != 0.0 && ((delta > 0 && prevDelta < 0) || (delta < 0 && prevDelta > 0))) {
                        signFlips++;
                    }
                    prevDelta = delta;
                }
            }

            double flipRatio = (double) signFlips / Math.max(1, values.size() - 2);
            String oscillationStatus = (flipRatio > 0.40) ? String.format("⚠️ HIGH OSCILLATION (%d flips)", signFlips)
                    : (flipRatio > 0.15) ? String.format("⚡ MODERATE FLUCTUATION (%d flips)", signFlips)
                    : String.format("✅ STABLE (%d flips)", signFlips);

            String mapeStr = benchErrors.isEmpty() ? "N/A" : String.format("%.2f%%", benchErrors.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

            System.out.printf("   - %-26s : Start=%,.1f | End=%,.1f | Range=[%,.1f, %,.1f] | Mean MAPE=%-7s | %s%n",
                    varName, first, last, min, max, mapeStr, oscillationStatus);

            assertFalse(Double.isNaN(last), "Value must not be NaN: " + varName);
            assertFalse(Double.isInfinite(last), "Value must not be Infinite: " + varName);
            assertTrue(min >= 0.0, "Physical value must not be negative: " + varName);
        }
    }
}
