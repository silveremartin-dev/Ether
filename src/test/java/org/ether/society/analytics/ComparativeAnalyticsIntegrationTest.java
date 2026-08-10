/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.model.Scenario;
import org.ether.society.persistence.ScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComparativeAnalyticsIntegrationTest {

    private SimulationRunRepository runRepository;
    private ScenarioRepository scenarioRepository;
    private RootCauseAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        runRepository = SimulationRunRepository.getInstance();
        scenarioRepository = new ScenarioRepository();
        analyzer = new RootCauseAnalyzer();
    }

    @Test
    @DisplayName("Should detect missing scenario execution and run it headlessly")
    void testHeadlessExecutionForMissingScenario() {
        Scenario scenario = new Scenario();
        scenario.setName("Test Scenario Alpha");
        scenario.setStartDateYear(-500);
        scenario.setEndDateYear(200);
        scenario.setInitialHumanCount(5000);
        scenario.setInitialCapitalPerCapita(25.0);

        scenarioRepository.saveOrUpdate(scenario);

        // Verify status before run
        assertFalse(runRepository.hasRunForScenario("Test Scenario Alpha"), "Scenario should not have a run recorded initially");

        // Trigger Headless batch execution
        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(scenario);

        // Verify run registered
        assertNotNull(record, "HeadlessBatchRunner should produce a non-null record");
        assertTrue(runRepository.hasRunForScenario("Test Scenario Alpha"), "Repository should now contain run for Test Scenario Alpha");
        assertEquals(700, scenario.getEndDateYear() - scenario.getStartDateYear());
        assertFalse(record.getTimeSeriesData().isEmpty(), "Time series data should contain snapshots");
    }

    @Test
    @DisplayName("Should compute divergence T_divergence between baseline and target runs")
    void testRootCauseAnalyzerDivergence() {
        List<SimulationRunRecord> allRuns = runRepository.getAllRuns();
        assertTrue(allRuns.size() >= 2, "Repository should have at least 2 seeded benchmarks");

        SimulationRunRecord baseline = allRuns.get(0);
        SimulationRunRecord target = allRuns.get(1);

        RootCauseAnalyzer.ComparisonResult result = analyzer.compareRuns(baseline, target);
        assertNotNull(result, "ComparisonResult should not be null");
        assertNotNull(result.getPrimaryRootCauseExplanation(), "Explanation should not be null");

        String md = ComparativeReportGenerator.generateMarkdownReport(result);
        assertTrue(md.contains("Rapport d'Analyse Comparative"), "Report should contain title header");
    }
}
