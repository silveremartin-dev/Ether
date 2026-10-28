package org.ether.society.analytics;

import org.ether.society.ui.analytics.ComparativeTelemetryDataLoader;
import org.ether.society.config.EtherPaths;
import org.ether.society.persistence.EtherJsonUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Automated batch harness to evaluate all 45 scenario runs against Historical Ground Truth benchmarks.
 */
public class CampaignBatchEvaluationHarness {

    public record SimpleBenchmarkItem(String name, String runId) implements ScenarioBenchmarkItem {
        @Override
        public String getName() { return name; }
        @Override
        public String getRunId() { return runId; }
    }

    @Test
    public void evaluateAllCampaignRuns() throws Exception {
        SimulationRunRepository repo = SimulationRunRepository.getInstance();
        repo.clear();

        // 1. Scan saves directory for all runs with history.json
        Path savesDir = EtherPaths.getSavesDir();
        List<Path> runDirs = new ArrayList<>();
        if (Files.exists(savesDir)) {
            try (var stream = Files.list(savesDir)) {
                runDirs = stream.filter(Files::isDirectory)
                               .filter(d -> Files.exists(d.resolve("history.json")) || Files.exists(d.resolve("snapshots")))
                               .sorted(Comparator.comparing(d -> d.getFileName().toString()))
                               .collect(Collectors.toList());
            }
        }

        System.out.println("================================================================================");
        System.out.println("Found " + runDirs.size() + " run directories with history/snapshots in saves/");
        System.out.println("================================================================================");

        List<SimpleBenchmarkItem> items = new ArrayList<>();

        for (Path d : runDirs) {
            String dirName = d.getFileName().toString();
            String scName = dirName;
            Path metaPath = d.resolve("metadata.json");
            if (Files.exists(metaPath)) {
                try {
                    var node = EtherJsonUtil.getMapper().readTree(metaPath.toFile());
                    if (node.has("scenarioName")) scName = node.get("scenarioName").asText();
                } catch (Exception ignored) {}
            }
            ComparativeTelemetryDataLoader.ensureHistoryLoadedForSave(dirName, scName, repo);
            SimulationRunRecord run = repo.getRun(dirName);
            if (run != null && run.getTimeSeriesData() != null && !run.getTimeSeriesData().isEmpty()) {
                items.add(new SimpleBenchmarkItem(scName, dirName));
            }
        }

        System.out.println("Loaded " + items.size() + " runnable scenarios with telemetry into repository.");

        if (items.isEmpty()) {
            System.out.println("No runs with time series found.");
            return;
        }

        // Run multi-scenario Ground Truth Audit
        GroundTruthAuditGenerator.AuditResult audit = GroundTruthAuditGenerator.generateMultiScenarioGroundTruthAudit(
                items,
                HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                item -> repo.getRun(item.getRunId())
        );

        // Save raw audit markdown report to scratch
        File outReport = new File("scratch/auto_audit_report.md");
        try (PrintWriter pw = new PrintWriter(new FileWriter(outReport))) {
            pw.println(audit.reportMarkdown());
        }

        System.out.println("Audit Report written to: " + outReport.getAbsolutePath());
        System.out.println("Earliest Divergence Year: " + audit.divergenceYear());
        System.out.println("Summary:\n" + audit.explanationSummary());
    }
}
