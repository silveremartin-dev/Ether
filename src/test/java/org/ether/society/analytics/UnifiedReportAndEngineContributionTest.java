/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies unified date-intersection comparative reporting (ground truth, inter-scenario,
 * and cross-scenario) and deterministic Leave-One-Out Tier 2 engine variance attribution.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
class UnifiedReportAndEngineContributionTest {

    private record TestItem(String name, String runId) implements ScenarioBenchmarkItem {
        @Override public String getName() { return name; }
        @Override public String getRunId() { return runId; }
    }

    @Test
    @DisplayName("Should unify Ground Truth and Inter-Scenario reports over exact date intersection and all variables")
    void testUnifiedComparativeReportDateIntersectionAndAllVariables() {
        SimulationRunRecord runA = new SimulationRunRecord("RUN-A", "Scenario A", "Full Tier 2",
                Map.of("Engine: KummelAyresExergyEngine", "✅ Activé"));
        SimulationRunRecord runB = new SimulationRunRecord("RUN-B", "Scenario B", "No Kummel",
                Map.of("Engine: TurchinGoldstoneSDTEngine", "✅ Activé"));

        // Run A spans [1700 .. 1900], Run B spans [1750 .. 1950] -> intersection is [1750 .. 1900]
        for (int yr = 1700; yr <= 1900; yr += 25) {
            double pop = 600_000_000.0 * Math.pow(1.005, yr - 1700);
            Map<String, Double> m = new LinkedHashMap<>();
            m.put("worldPopulation", pop);
            m.put("grossWorldProduct", pop * 600.0 / 1e9);
            m.put("primaryEnergy", 12.0 + (yr - 1700) * 0.15);
            m.put("giniInequality", 0.42);
            m.put("asabiyyahSocialCohesion", 0.72);
            m.put("co2Concentration", 280.0 + Math.max(0, yr - 1800) * 0.2);
            m.put("customSensor", (double) yr);
            runA.addSnapshot(yr, Math.round(pop), 3.4, 2.5, 72.0, 120, m);
        }
        for (int yr = 1750; yr <= 1950; yr += 25) {
            double pop = 650_000_000.0 * Math.pow(1.007, yr - 1750);
            Map<String, Double> m = new LinkedHashMap<>();
            m.put("worldPopulation", pop);
            m.put("grossWorldProduct", pop * 850.0 / 1e9);
            m.put("primaryEnergy", 15.0 + (yr - 1750) * 0.25);
            m.put("giniInequality", 0.48);
            m.put("asabiyyahSocialCohesion", 0.65);
            m.put("co2Concentration", 282.0 + Math.max(0, yr - 1800) * 0.35);
            m.put("customSensor", yr * 1.1);
            runB.addSnapshot(yr, Math.round(pop), 3.6, 3.1, 65.0, 135, m);
        }

        EngineContributionReport contribA = new EngineContributionReport(
                "Scenario A", "RUN-A", 42L, 1700, 1900, 1, 2, 2,
                List.of(
                        new EngineContributionReport.EngineEntry(
                                "KummelAyresExergyEngine", "SimulationEngineModule (DoD)", true,
                                80.0, 1775, 2.4, "🔥 Dominant",
                                List.of(new EngineContributionReport.MetricContribution("grossWorldProduct", 80.0, 12.0, -5.0))),
                        new EngineContributionReport.EngineEntry(
                                "BoserupAgriculturalIntensificationEngine", "Unwired (Inactive in Tick Loop)", false,
                                0.0, Integer.MIN_VALUE, 0.0, "⚪ Inert (Unwired)", List.of())
                ));
        EngineContributionReport contribB = new EngineContributionReport(
                "Scenario B", "RUN-B", 42L, 1750, 1950, 1, 1, 1,
                List.of(
                        new EngineContributionReport.EngineEntry(
                                "TurchinGoldstoneSDTEngine", "SimulationEngineModule (DoD)", true,
                                100.0, 1800, 1.1, "🔥 Dominant",
                                List.of(new EngineContributionReport.MetricContribution("giniInequality", 100.0, 8.0, 4.0)))
                ));
        runA.setEngineContributionReport(contribA);
        runB.setEngineContributionReport(contribB);

        Map<String, SimulationRunRecord> store = Map.of("RUN-A", runA, "RUN-B", runB);

        // 1. Inter-scenario comparison (no Ground Truth)
        GroundTruthAuditGenerator.AuditResult interRes = GroundTruthAuditGenerator.generateUnifiedComparativeReport(
                List.of(new TestItem("Scenario A", "RUN-A"), new TestItem("Scenario B", "RUN-B")),
                HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                it -> store.get(it.getRunId()),
                new RootCauseAnalyzer(),
                null
        );
        String mdInter = interRes.reportMarkdown();
        assertTrue(mdInter.contains("[1750 .. 1900]"), "Report must evaluate over exact date intersection [1750 .. 1900]");
        assertTrue(mdInter.contains("customSensor"), "Report must include extra common simulated variables");
        assertTrue(mdInter.contains("KummelAyresExergyEngine"), "Report must include engine diffs and contributions");
        assertTrue(mdInter.contains("Cross-Scenario Tier 2 Engine Contribution Matrix"),
                "Report must include cross-scenario engine contribution matrix when 2+ runs carry attribution");

        // 2. Mixed selection: Ground Truth + Run A + Run B
        GroundTruthAuditGenerator.AuditResult gtRes = GroundTruthAuditGenerator.generateUnifiedComparativeReport(
                List.of(
                        new TestItem("Historical Reality", "HISTORICAL_GROUND_TRUTH"),
                        new TestItem("Scenario A", "RUN-A"),
                        new TestItem("Scenario B", "RUN-B")
                ),
                HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                it -> store.get(it.getRunId()),
                new RootCauseAnalyzer(),
                null
        );
        String mdGt = gtRes.reportMarkdown();
        assertTrue(mdGt.contains("[1750 .. 1900]"), "Mixed selection must intersect all selected ranges [1750 .. 1900]");
        assertTrue(mdGt.contains("Partial Contribution of Tier 2 Engines"), "Ground truth report must embed Tier 2 contributions");
    }

    @Test
    @DisplayName("Should report disjoint time windows clearly when scenarios do not overlap")
    void testDisjointDateWindows() {
        SimulationRunRecord ancient = new SimulationRunRecord("RUN-OLD", "Ancient", "desc", Map.of());
        ancient.addSnapshot(-500, 1000, 3.0, 1.0, 70.0, 10);
        ancient.addSnapshot(-400, 1200, 3.0, 1.0, 70.0, 10);

        SimulationRunRecord modern = new SimulationRunRecord("RUN-MOD", "Modern", "desc", Map.of());
        modern.addSnapshot(1900, 1_600_000, 3.5, 5.0, 60.0, 50);
        modern.addSnapshot(2000, 6_000_000, 3.8, 8.0, 65.0, 80);

        Map<String, SimulationRunRecord> store = Map.of("RUN-OLD", ancient, "RUN-MOD", modern);
        GroundTruthAuditGenerator.AuditResult res = GroundTruthAuditGenerator.generateUnifiedComparativeReport(
                List.of(new TestItem("Ancient", "RUN-OLD"), new TestItem("Modern", "RUN-MOD")),
                HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                it -> store.get(it.getRunId()),
                new RootCauseAnalyzer(),
                null
        );
        assertTrue(res.reportMarkdown().contains("Disjoint Time Intervals"),
                "Disjoint date ranges must produce an explicit disjoint-window report");
    }

    @Test
    @DisplayName("Should execute deterministic Leave-One-Out Tier 2 engine attribution and persist JSON")
    void testDeterministicTier2EngineContributionAnalyzer(@TempDir Path tempDir) {
        Scenario sc = Scenario.createDefaultScenario();
        sc.setName("Test LOO Attribution");
        sc.setH3Resolution(1);
        sc.setStartDateYear(1850);
        sc.setEndDateYear(1870);
        sc.setTemporalResolutionDays(90.0);
        sc.setStrictDeterminism(true);
        sc.setSeed(12345L);
        sc.getTypeBEngineStates().clear();
        sc.getTypeBEngineStates().put("KummelAyresExergyEngine", true);
        sc.getTypeBEngineStates().put("JevonsParadoxEngine", true);
        sc.getTypeBEngineStates().put("TurchinGoldstoneSDTEngine", true);
        sc.getTypeBEngineStates().put("UnwiredHypotheticalEngine", true);

        EngineContributionReport report = Tier2EngineContributionAnalyzer.analyze(sc, true, null);
        assertNotNull(report, "Contribution report should not be null");
        assertEquals(4, report.getTotalEnabledEngines(), "Should list all 4 enabled Tier 2 engines");
        assertEquals(3, report.getTestedEngineCount(), "Should ablate the 3 wired engines and skip the unwired one");

        Map<String, EngineContributionReport.EngineEntry> byId = report.toEngineMap();
        assertTrue(byId.containsKey("UnwiredHypotheticalEngine"));
        assertFalse(byId.get("UnwiredHypotheticalEngine").wiredInLoop());
        assertEquals(0.0, byId.get("UnwiredHypotheticalEngine").globalVarianceSharePct(), 1e-9);

        double totalShare = report.getEntries().stream()
                .mapToDouble(EngineContributionReport.EngineEntry::globalVarianceSharePct)
                .sum();
        assertTrue(totalShare <= 100.01 && totalShare >= 0.0, "Normalized shares must lie in [0, 100]");

        Tier2EngineContributionAnalyzer.saveReport(tempDir, report);
        EngineContributionReport reloaded = Tier2EngineContributionAnalyzer.loadReport(tempDir);
        assertNotNull(reloaded, "Persisted engine_contributions.json should reload cleanly");
        assertEquals(report.getEntries().size(), reloaded.getEntries().size());
        assertTrue(report.toMarkdownSection().contains("KummelAyresExergyEngine"));
    }
}
