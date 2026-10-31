package org.ether.society.scenarios;

import org.ether.society.analytics.GroundTruthAuditGenerator;
import org.ether.society.analytics.HeadlessBatchRunner;
import org.ether.society.analytics.HistoricalValidationKernel;
import org.ether.society.analytics.ScenarioBenchmarkItem;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.analytics.SimulationRunRepository;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Executes and audits the 5 target scenarios:
 * 1. Antonine Plague ("Pest" 165 CE)
 * 2. Black Death ("Noir" 1347 CE)
 * 3. Ramesses II (-1279 BCE)
 * 4. 1914 World Wars (1914 CE)
 * 5. Pax Romana Augustus (Calm Baseline Epoch, 0 CE)
 *
 * Audits:
 * - High-frequency oscillations (Sawtooth ringing index Phi_osc)
 * - Historical benchmark divergence (MAPE against HYDE/Maddison/Seshat)
 * - Boundary invariants, anomalies and bugs.
 */
public class TargetScenariosOscillationAuditTest {

    @Test
    @DisplayName("Audit Target Scenarios and Detect Numerical Oscillations")
    public void testAuditScenariosAndDetectOscillations() {
        List<Scenario> allScenarios = Scenario.getBuiltInScenarios();

        List<String> targetKeys = List.of(
                "antonine_plague_165",
                "black_death_1347",
                "ramesses_ii",
                "world_wars_totalitarian_1914",
                "pax_romana_augustus_0"
        );

        System.out.println("================================================================================");
        System.out.println(" 🔬 REAL TARGET SCENARIOS EXECUTION & OSCILLATION INTEGRITY AUDIT");
        System.out.println("================================================================================");

        for (String key : targetKeys) {
            Scenario s = allScenarios.stream()
                    .filter(sc -> key.equalsIgnoreCase(sc.getPresetKey()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(s, "Target scenario not found: " + key);

            // Configure reasonable execution horizon for regression diagnostics
            long startYear = s.getStartDateYear();
            long maxRunYears = switch (key) {
                case "antonine_plague_165" -> 15; // 165 - 180
                case "black_death_1347" -> 10;    // 1347 - 1357
                case "ramesses_ii" -> 20;         // -1279 - -1259
                case "world_wars_totalitarian_1914" -> 15; // 1914 - 1929
                case "pax_romana_augustus_0" -> 20; // 0 - 20
                default -> 15;
            };
            s.setEndDateYear(startYear + maxRunYears);
            if (!"ramesses_ii".equalsIgnoreCase(key)) {
                s.setH3Resolution(2); // Fast spatial resolution for verification
            } else {
                s.setH3Resolution(4); // Resolution 4 preserves Nile river cells
            }
            s.setTargetCohortSize(50000);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("▶ EXECUTING: " + s.getName() + " (" + s.getPresetKey() + ")");
            System.out.println("  Period: " + s.getStartDateYear() + " ➔ " + s.getEndDateYear() + " (" + maxRunYears + " years)");
            System.out.println("--------------------------------------------------------------------------------");

            long startMs = System.currentTimeMillis();
            SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(s);
            long elapsedMs = System.currentTimeMillis() - startMs;

            assertNotNull(record, "Simulation run record must be generated");
            var timeSeries = record.getTimeSeriesData();
            assertNotNull(timeSeries, "Time series must exist");
            assertFalse(timeSeries.isEmpty(), "Time series must contain snapshots");

            System.out.printf("  Completed in %,d ms across %d snapshots recorded.%n", elapsedMs, timeSeries.size());

            // Audit variables for stability and high-frequency oscillations
            List<String> variablesToAudit = List.of(
                    "worldPopulation",
                    "grossWorldProduct",
                    "primaryEnergy",
                    "urbanizationRate",
                    "sociopoliticalInstability"
            );

            System.out.println("  📊 VARIABLE TELEMETRY, OSCILLATIONS & BENCHMARK DIVERGENCE:");
            for (String varName : variablesToAudit) {
                List<Double> values = new ArrayList<>();
                List<Double> benchErrors = new ArrayList<>();

                for (var entry : timeSeries.entrySet()) {
                    long yr = entry.getKey();
                    double val = entry.getValue().getValue(varName);
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

                // 1. Check bounds and anomalies
                double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                double first = values.get(0);
                double last = values.get(values.size() - 1);

                // 2. Count direction sign-flips (sawtooth ringing)
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
                String oscillationStatus;
                if (flipRatio > 0.40) {
                    oscillationStatus = String.format("⚠️ HIGH OSCILLATION (%d flips, %.0f%%)", signFlips, flipRatio * 100);
                } else if (flipRatio > 0.15) {
                    oscillationStatus = String.format("⚡ MODERATE FLUCTUATION (%d flips)", signFlips);
                } else {
                    oscillationStatus = String.format("✅ STABLE (%d flips)", signFlips);
                }

                String mapeStr = benchErrors.isEmpty() ? "N/A" : String.format("%.2f%%", benchErrors.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

                System.out.printf("   - %-26s : Start=%,.1f | End=%,.1f | Range=[%,.1f, %,.1f] | Mean MAPE=%-7s | %s%n",
                        varName, first, last, min, max, mapeStr, oscillationStatus);

                assertFalse(Double.isNaN(last), "Value must not be NaN: " + varName);
                assertFalse(Double.isInfinite(last), "Value must not be Infinite: " + varName);
                assertTrue(min >= 0.0, "Physical value must not be negative: " + varName + " min=" + min);
            }
        }

        System.out.println("\n================================================================================");
        System.out.println(" 📊 GENERATING OFFICIAL GROUND TRUTH AUDIT & SYNTHETIC FIDELITY SCORES");
        System.out.println("================================================================================");

        List<ScenarioBenchmarkItem> benchmarkItems = new ArrayList<>();
        for (String key : targetKeys) {
            Scenario s = allScenarios.stream().filter(sc -> key.equalsIgnoreCase(sc.getPresetKey())).findFirst().orElse(null);
            if (s != null) {
                var optRun = SimulationRunRepository.getInstance().getRunByScenarioName(s.getName());
                optRun.ifPresent(r -> benchmarkItems.add(new ScenarioBenchmarkItem() {
                    @Override public String getName() { return r.getScenarioName(); }
                    @Override public String getRunId() { return r.getRunId(); }
                }));
            }
        }

        if (!benchmarkItems.isEmpty()) {
            GroundTruthAuditGenerator.AuditResult auditResult = GroundTruthAuditGenerator.generateMultiScenarioGroundTruthAudit(
                    benchmarkItems,
                    HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                    item -> SimulationRunRepository.getInstance().getRun(item.getRunId())
            );
            System.out.println(auditResult.reportMarkdown());
        }
    }
}
