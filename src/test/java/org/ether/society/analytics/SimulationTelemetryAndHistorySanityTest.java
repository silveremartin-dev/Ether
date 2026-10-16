/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>Simulation Telemetry and History Sanity Test</h1>
 * <p>
 * Verifies that:
 * 1. SimulationHistory pins the initial snapshot ($t_0$) at all times, never dropping the starting step.
 * 2. Equidistant decimation is performed across intermediate steps to keep memory and disk bounded.
 * 3. HistoricalValidationKernel returns Double.NaN for out-of-epoch historical indicators (e.g. GDP before 1 CE).
 * 4. ComparativeReportGenerator produces scientifically standardized CSV exports with explicit SI/cliodynamic units,
 *    1-to-1 matching temporal points between scenario runs and historical ground truth, and explicit N/A values.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SimulationTelemetryAndHistorySanityTest {

    @Test
    @DisplayName("SimulationHistory: Initial step (t0) must never be dropped, and decimation must be equidistant")
    void testInitialSnapshotPinnedAndEquidistantDecimation() {
        SimulationHistory history = new SimulationHistory(100); // Small capacity to trigger decimation quickly

        // Step 0: Initial snapshot at start of scenario (-10900 BCE, Month 0)
        HistorySnapshot initialSnap = new HistorySnapshot(
            -10900, 0, 40000L, 1000.0, 500.0, 30.0, 0.25, 10.0,
            1600.0, 0.1, 80.0, 10.0, 100000.0, 20.0, 30.0, 5000.0,
            0.0, 15.0, 60.0, 0.0, 30000000.0, 10000.0, 5.5, 0, 1.0
        );
        history.addSnapshot(initialSnap);

        // Add 250 intermediate snapshots
        for (int i = 1; i <= 250; i++) {
            int year = -10900 + (i / 12);
            int month = i % 12;
            history.addSnapshot(new HistorySnapshot(
                year, month, 40000L + i * 10, 1000.0, 500.0, 30.0, 0.25, 10.0,
                1600.0, 0.1, 80.0, 10.0, 100000.0, 20.0, 30.0, 5000.0,
                0.0, 15.0, 60.0, 0.0, 30000000.0, 10000.0, 5.5, 0, 1.0
            ));
        }

        List<HistorySnapshot> snapshots = history.getSnapshots();

        // 1. Capacity must remain bounded <= 100
        assertTrue(snapshots.size() <= 100, "Snapshot count must remain bounded within capacity");

        // 2. The very first snapshot MUST strictly be Year -10900 Month 0 (NEVER DROPPED)
        assertEquals(-10900, snapshots.get(0).year(), "First snapshot year must strictly be initial scenario year");
        assertEquals(0, snapshots.get(0).month(), "First snapshot month must strictly be initial scenario month");

        // 3. The latest snapshot MUST be the most recent tick added
        HistorySnapshot lastSnap = snapshots.get(snapshots.size() - 1);
        int expectedLastYear = -10900 + (250 / 12);
        int expectedLastMonth = 250 % 12;
        assertEquals(expectedLastYear, lastSnap.year(), "Last snapshot year must match most recent step");
        assertEquals(expectedLastMonth, lastSnap.month(), "Last snapshot month must match most recent step");

        // 4. Chronological order must be strictly monotonically increasing
        for (int i = 1; i < snapshots.size(); i++) {
            long prevTime = ((long) snapshots.get(i - 1).year() * 12) + snapshots.get(i - 1).month();
            long currTime = ((long) snapshots.get(i).year() * 12) + snapshots.get(i).month();
            assertTrue(currTime > prevTime, "Snapshots must be strictly monotonically ordered");
        }
    }

    @Test
    @DisplayName("HistoricalValidationKernel: Out-of-bounds epochs must return Double.NaN (N/A)")
    void testHistoricalOutOfBoundsReturnsNaN() {
        // Gross World Product starts at 1 CE in Maddison dataset
        double gdpPaleolithic = HistoricalValidationKernel.getInterpolatedBenchmarkValue("grossWorldProduct", -10900);
        assertTrue(Double.isNaN(gdpPaleolithic), "GDP at -10900 BCE must be NaN (non-applicable)");

        // Currency debasement starts at -200 BCE (Roman Denarius)
        double debasementPaleo = HistoricalValidationKernel.getInterpolatedBenchmarkValue("currencyDebasement", -10000);
        assertTrue(Double.isNaN(debasementPaleo), "Silver debasement at -10000 BCE must be NaN (non-applicable)");

        // Sovereign debt starts in 1500 CE
        double debtMedieval = HistoricalValidationKernel.getInterpolatedBenchmarkValue("sovereignDebtBurden", 500);
        assertTrue(Double.isNaN(debtMedieval), "Sovereign debt in Antiquity/Medieval era must be NaN (non-applicable)");

        // Deep variables MUST be defined at -10900 BCE
        double popPaleo = HistoricalValidationKernel.getInterpolatedBenchmarkValue("worldPopulation", -10900);
        assertFalse(Double.isNaN(popPaleo), "World population at -10900 BCE must be defined");
        assertTrue(popPaleo > 0.0 && popPaleo < 10.0, "Paleolithic population must be in millions (around 2-5M)");

        double insolationPaleo = HistoricalValidationKernel.getInterpolatedBenchmarkValue("milankovitchInsolation", -10900);
        assertFalse(Double.isNaN(insolationPaleo), "Milankovitch insolation at -10900 BCE must be defined");
        assertTrue(insolationPaleo > 400.0 && insolationPaleo < 600.0, "Milankovitch 65N insolation must be between 400 and 600 W/m²");
    }

    @Test
    @DisplayName("ComparativeReportGenerator: Export must use standard SI/cliodynamic units and match scenario years")
    void testStandardizedScientificCsvExportWithMatchedYears() {
        // Create synthetic run record with 3 years: -10900, -10800, -10700
        SimulationRunRecord run = new SimulationRunRecord("RUN-TEST-YD", "Younger Dryas Test", "deterministic", null);

        Map<String, Double> m1 = new HashMap<>();
        m1.put("worldPopulation", 40000.0);
        m1.put("gdp", 40000000.0); // 40 Million dollars = 0.04 Billion USD
        m1.put("grossWorldProduct", 0.04);
        m1.put("primaryEnergy", 0.05); // 0.05 EJ
        m1.put("energyCaptured", 1600.0);
        m1.put("co2Concentration", 260.0);
        m1.put("foodPerCapita", 3.4);
        run.addSnapshot(-10900, 40000L, 3.4, 15.0, 80.0, 100, m1);

        Map<String, Double> m2 = new HashMap<>();
        m2.put("worldPopulation", 42000.0);
        m2.put("gdp", 45000000.0);
        m2.put("grossWorldProduct", 0.045);
        m2.put("primaryEnergy", 0.052);
        m2.put("energyCaptured", 1650.0);
        m2.put("co2Concentration", 261.0);
        m2.put("foodPerCapita", 3.4);
        run.addSnapshot(-10800, 42000L, 3.4, 15.2, 80.0, 100, m2);

        Map<String, Double> m3 = new HashMap<>();
        m3.put("worldPopulation", 45000.0);
        m3.put("gdp", 50000000.0);
        m3.put("grossWorldProduct", 0.05);
        m3.put("primaryEnergy", 0.055);
        m3.put("energyCaptured", 1700.0);
        m3.put("co2Concentration", 262.0);
        m3.put("foodPerCapita", 3.4);
        run.addSnapshot(-10700, 45000L, 3.4, 15.5, 80.0, 100, m3);

        String csv = ComparativeReportGenerator.generateCsvExport(List.of(run), true, -10900, -10700);

        assertNotNull(csv);
        String[] lines = csv.split("\n");
        assertTrue(lines.length >= 7, "CSV must contain header + 3 simulated rows + matching historical rows");

        // Verify header has standardized units
        String header = lines[0];
        assertTrue(header.contains("Population_headcount"), "Header must include Population_headcount");
        assertTrue(header.contains("GrossWorldProduct_BillionUSD1990"), "Header must include GrossWorldProduct_BillionUSD1990");
        assertTrue(header.contains("PrimaryEnergy_EJ"), "Header must include PrimaryEnergy_EJ");
        assertTrue(header.contains("CO2Concentration_ppm"), "Header must include CO2Concentration_ppm");

        // Verify simulated row at -10900
        String simRow1 = lines[1];
        assertTrue(simRow1.contains("RUN-TEST-YD"), "Simulated row must contain RunId");
        assertTrue(simRow1.contains("-10900"), "Simulated row must contain Year -10900");
        assertTrue(simRow1.contains("40000"), "Simulated row must contain headcount population 40000");

        // Verify historical rows exist at matching years (-10900, -10800, -10700)
        boolean hasHistAt10900 = false;
        boolean hasHistAt10800 = false;
        boolean hasHistAt10700 = false;
        for (String line : lines) {
            if (line.contains("HISTORICAL_GROUND_TRUTH")) {
                if (line.contains(",-10900,")) hasHistAt10900 = true;
                if (line.contains(",-10800,")) hasHistAt10800 = true;
                if (line.contains(",-10700,")) hasHistAt10700 = true;
            }
        }

        assertTrue(hasHistAt10900, "Historical Ground Truth must match scenario year -10900 exactly");
        assertTrue(hasHistAt10800, "Historical Ground Truth must match scenario year -10800 exactly");
        assertTrue(hasHistAt10700, "Historical Ground Truth must match scenario year -10700 exactly");

        // Verify that in historical row at -10900, out-of-bounds indicators like GDP or debt have N/A
        for (String line : lines) {
            if (line.contains("HISTORICAL_GROUND_TRUTH") && line.contains(",-10900,")) {
                assertTrue(line.contains("N/A"), "Historical row at -10900 must contain N/A for out-of-bounds indicators");
            }
        }
    }
}
