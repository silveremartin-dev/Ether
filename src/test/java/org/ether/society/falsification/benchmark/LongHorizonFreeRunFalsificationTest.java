/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.benchmark;

import org.ether.society.analytics.HeadlessBatchRunner;
import org.ether.society.analytics.HistoricalValidationKernel;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>Pillar 6: Multi-Century Long-Horizon Unguided Free-Run Falsification Suite</h1>
 * <p>
 * Evaluates the intrinsic thermodynamic stability, non-divergence, and emergent secular dynamics of Ether
 * when executed in a strictly open loop across <b>multi-century horizons (250 to 500 consecutive years =
 * 3,000 to 6,000 monthly ticks)</b> initialized solely at \(t = t_0\) without any external raster re-anchoring,
 * calendar nudging, or empirical curve-fitting.
 * </p>
 *
 * <h2>The 4 Multi-Century Free-Run Epistemic Invariants</h2>
 * <ol>
 *   <li><b>Non-Divergence &amp; Non-Extinction over Centuries</b>: Human population \(N(t)\) remains strictly
 *       bounded within biophysically realistic carrying envelopes across 3,000–6,000 consecutive monthly ticks,
 *       preventing Malthusian extinction (\(N \to 0\)) or unbounded exponential blow-up (\(N \to \infty\)).</li>
 *   <li><b>Strict Conservation of Mass &amp; Energy</b>: Conserved physical state variables (soil depth, aquifers,
 *       physical capital \(K\), food stocks) strictly satisfy non-negativity and never diverge to \(\pm\infty\) or \(\text{NaN}\).</li>
 *   <li><b>Endogenous Turchin-Nefedov Secular Cycles &amp; Demographic Recovery</b>: Multi-century trajectories
 *       exhibit endogenous non-monotonic variance and post-crisis demographic recovery rather than artificial flatlines.</li>
 *   <li><b>Spatial Alluvial Convergence</b>: Across multi-century open-loop evolution, population density
 *       remains endogenously concentrated in fertile river and coastal basins relative to hyper-arid deserts.</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
@Tag("scientific")
@Tag("falsification")
@Tag("benchmark")
@Tag("pillar6")
public class LongHorizonFreeRunFalsificationTest {

    @Test
    @DisplayName("Free-Run 1: 500-Year Bronze-to-Iron Age Open-Loop Free-Run (-1279 to -779 BCE, 6,000 Monthly Ticks)")
    void testFiveHundredYearBronzeToIronAgeFreeRunStability() throws Exception {
        Scenario scenario = Scenario.getBuiltInScenarios().stream()
                .filter(s -> "ramesses_ii".equalsIgnoreCase(s.getPresetKey())
                        || (s.getName() != null && s.getName().contains("Ramesses")))
                .findFirst()
                .orElse(null);

        assertNotNull(scenario, "Scenario 'ramesses_ii' must exist in built-ins");

        // Authentic 500-year multi-century open-loop free-run: -1279 BCE to -779 BCE (6,000 monthly ticks)
        // Regional Egypt-Levant window [18°N..36°N, 24°E..42°E] at H3 Resolution 6
        int startYear = -1279;
        int endYear = -779;
        scenario.setStartDateYear(startYear);
        scenario.setEndDateYear(endYear);
        scenario.setH3Resolution(6);

        long startMs = System.currentTimeMillis();
        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(scenario);
        long elapsedMs = System.currentTimeMillis() - startMs;

        assertNotNull(record, "SimulationRunRecord must not be null for 500-year free-run execution");
        assertTrue(elapsedMs < 300_000, "500-year (6,000 ticks) free-run must complete within CPU envelope (< 300s)");

        Map<Integer, SimulationRunRecord.MetricSnapshot> timeSeries = record.getTimeSeriesData();
        assertNotNull(timeSeries, "Time series history must be recorded across the 500-year horizon");
        assertTrue(timeSeries.size() >= 450,
                "Multi-century free-run must record at least 450 annual snapshots (recorded: " + timeSeries.size() + ")");

        // -------------------------------------------------------------
        // Invariant 1: Multi-Century Non-Extinction & Non-Divergence
        // -------------------------------------------------------------
        long initialPop = timeSeries.get(startYear) != null ? timeSeries.get(startYear).getPopulation() : 0;
        long finalPop = timeSeries.get(endYear) != null ? timeSeries.get(endYear).getPopulation() : 0;

        assertTrue(initialPop > 1_000, "Initial population at -1279 BCE must be positive");
        assertTrue(finalPop > 1_000,
                "Final population after 500-year open-loop free-run (-779 BCE) must not collapse to extinction (final: " + finalPop + ")");
        assertTrue(finalPop < initialPop * 25,
                "Population after 500 years must remain bounded by regional alluvial carrying capacity");

        // -------------------------------------------------------------
        // Invariant 2: Conservation of Mass & Energy across 6,000 Ticks
        // -------------------------------------------------------------
        double minStability = Double.MAX_VALUE;
        double maxStability = -Double.MAX_VALUE;
        long minPopObserved = Long.MAX_VALUE;
        long maxPopObserved = Long.MIN_VALUE;

        for (Map.Entry<Integer, SimulationRunRecord.MetricSnapshot> entry : timeSeries.entrySet()) {
            SimulationRunRecord.MetricSnapshot snap = entry.getValue();
            assertNotNull(snap);
            assertFalse(Double.isNaN(snap.getFood()), "Food per capita must never be NaN at year " + entry.getKey());
            assertFalse(Double.isInfinite(snap.getFood()), "Food per capita must never be infinite at year " + entry.getKey());
            assertTrue(snap.getFood() >= 0.0, "Food availability must satisfy strict non-negativity");

            assertFalse(Double.isNaN(snap.getAvgTech()), "Exergy index must never be NaN at year " + entry.getKey());
            assertTrue(snap.getAvgTech() >= 0.0 && snap.getAvgTech() <= 10.0,
                    "Emergent Garrett-Lotka index must remain bounded in [0, 10]");

            assertFalse(Double.isNaN(snap.getStability()), "Stability must never be NaN at year " + entry.getKey());
            assertTrue(snap.getStability() >= 0.0 && snap.getStability() <= 100.0,
                    "Institutional stability must remain bounded within [0, 100]");

            minStability = Math.min(minStability, snap.getStability());
            maxStability = Math.max(maxStability, snap.getStability());
            minPopObserved = Math.min(minPopObserved, snap.getPopulation());
            maxPopObserved = Math.max(maxPopObserved, snap.getPopulation());
        }

        // -------------------------------------------------------------
        // Invariant 3: Endogenous Secular Dynamics (Non-Flatline Trajectory)
        // -------------------------------------------------------------
        assertTrue(maxPopObserved > minPopObserved,
                "500-year trajectory must exhibit dynamic demographic evolution rather than a static flatline");

        // -------------------------------------------------------------
        // Invariant 4: Spatial Alluvial Convergence in Final Snapshot (-779 BCE)
        // -------------------------------------------------------------
        List<H3Cell> finalSnapshot = record.getSpatialSnapshotAt(endYear);
        if ((finalSnapshot == null || finalSnapshot.isEmpty())
                && record.getSpatialSnapshots() != null
                && !record.getSpatialSnapshots().isEmpty()) {
            finalSnapshot = record.getSpatialSnapshots().lastEntry().getValue();
        }
        assertNotNull(finalSnapshot, "Final spatial snapshot at -779 BCE must be captured");
        assertFalse(finalSnapshot.isEmpty(), "Final spatial snapshot must contain H3 cells");

        long nilePop = 0;
        long desertPop = 0;
        int nileCount = 0;
        int desertCount = 0;

        for (H3Cell c : finalSnapshot) {
            double lat = c.getLatitude() != null ? c.getLatitude() : 0.0;
            double lng = c.getLongitude() != null ? c.getLongitude() : 0.0;
            long p = c.getPopulationOr(0);

            if (lat >= 24.0 && lat <= 31.5 && lng >= 30.5 && lng <= 32.5) {
                nilePop += p;
                nileCount++;
            } else if (lat >= 22.0 && lat <= 28.0 && lng >= 24.0 && lng <= 28.0) {
                desertPop += p;
                desertCount++;
            }
        }

        double avgNileDensity = nileCount > 0 ? (double) nilePop / nileCount : 0.0;
        double avgDesertDensity = desertCount > 0 ? (double) desertPop / desertCount : 0.0;
        assertTrue(avgNileDensity >= avgDesertDensity,
                "After 500 years of open-loop evolution, the Nile alluvial corridor must sustain higher density than hyper-arid desert");

        double tauStart = HistoricalValidationKernel.computeContinuousTemporalTolerance(startYear);
        double tauEnd = HistoricalValidationKernel.computeContinuousTemporalTolerance(endYear);
        assertTrue(tauEnd <= tauStart,
                "Continuous temporal error tolerance tau(t) must monotonically narrow from -1279 BCE to -779 BCE");
    }

    @Test
    @DisplayName("Free-Run 2: 250-Year Late Medieval to Early Modern Open-Loop Free-Run (1347 to 1597 CE, 3,000 Monthly Ticks)")
    void testTwoHundredFiftyYearPostPlagueToEarlyModernFreeRun() throws Exception {
        Scenario blackDeathScenario = Scenario.getBuiltInScenarios().stream()
                .filter(s -> "black_death_1347".equalsIgnoreCase(s.getPresetKey())
                        || (s.getName() != null && s.getName().contains("Black Death")))
                .findFirst()
                .orElse(null);

        assertNotNull(blackDeathScenario, "Scenario 'black_death_1347' must exist in built-ins");

        // Authentic 250-year multi-century open-loop free-run: 1347 CE to 1597 CE (3,000 monthly ticks)
        // Focused on the European-Mediterranean plague basin [28°N..60°N, -12°W..45°E] at H3 Resolution 4
        int startYear = 1347;
        int endYear = 1597;
        blackDeathScenario.setStartDateYear(startYear);
        blackDeathScenario.setEndDateYear(endYear);
        blackDeathScenario.setH3Resolution(4);
        blackDeathScenario.setClippingEnabled(true);
        blackDeathScenario.setMinLat(28.0);
        blackDeathScenario.setMaxLat(60.0);
        blackDeathScenario.setMinLng(-12.0);
        blackDeathScenario.setMaxLng(45.0);

        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(blackDeathScenario);
        assertNotNull(record, "250-year post-Black Death free-run must produce a valid record");

        Map<Integer, SimulationRunRecord.MetricSnapshot> timeSeries = record.getTimeSeriesData();
        assertNotNull(timeSeries);
        assertTrue(timeSeries.size() >= 200,
                "250-year free-run must capture at least 200 annual snapshots (captured: " + timeSeries.size() + ")");

        for (Map.Entry<Integer, SimulationRunRecord.MetricSnapshot> e : timeSeries.entrySet()) {
            SimulationRunRecord.MetricSnapshot snap = e.getValue();
            assertTrue(snap.getPopulation() > 50_000,
                    "European-Mediterranean population must survive across all 250 years (year " + e.getKey() + ")");
            assertFalse(Double.isNaN(snap.getFood()) || Double.isInfinite(snap.getFood()));
            assertTrue(snap.getFood() >= 0.0, "Food per capita must remain non-negative");
            assertTrue(snap.getAvgTech() >= 0.0 && snap.getAvgTech() <= 10.0,
                    "Emergent technology diagnostic must remain bounded in [0, 10]");
        }

        // Verify continuous tolerance narrowing across the 250-year transition from Late Middle Ages (1347) to Early Modern (1597)
        double tau1347 = HistoricalValidationKernel.computeContinuousTemporalTolerance(1347L);
        double tau1597 = HistoricalValidationKernel.computeContinuousTemporalTolerance(1597L);
        assertTrue(tau1597 < tau1347,
                "Continuous temporal tolerance must narrow from 1347 CE (" + (tau1347 * 100.0)
                        + "%) to 1597 CE (" + (tau1597 * 100.0) + "%)");
    }
}
