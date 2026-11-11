/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.config.EtherPaths;
import org.ether.society.persistence.EtherJsonUtil;
import org.ether.society.ui.analytics.ComparativeTelemetryDataLoader;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * CLI Runner to generate standardized Cliodynamic Ground Truth Audit & Falsification Reports
 * directly from saved simulation runs in {@code saves/}.
 *
 * <p>Usage:
 * {@code java -cp ... org.ether.society.analytics.ScenarioFalsificationReportRunner [runDirectoryName ...]}
 * If no directories are provided, the runner evaluates the latest historical simulation runs.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public class ScenarioFalsificationReportRunner {

    public record SimpleBenchmarkItem(String name, String runId) implements ScenarioBenchmarkItem {
        @Override
        public String getName() { return name; }
        @Override
        public String getRunId() { return runId; }
    }

    public static void main(String[] args) {
        try {
            SimulationRunRepository repo = SimulationRunRepository.getInstance();
            repo.clear();

            List<String> targetDirNames = new ArrayList<>();
            if (args != null && args.length > 0) {
                for (String arg : args) {
                    if (arg != null && !arg.isBlank()) {
                        File f = new File(arg);
                        targetDirNames.add(f.isDirectory() ? f.getName() : arg.trim());
                    }
                }
            }

            Path savesDir = EtherPaths.getSavesDir();
            if (targetDirNames.isEmpty()) {
                // Find most recent runs in saves/
                if (Files.exists(savesDir)) {
                    try (var stream = Files.list(savesDir)) {
                        List<Path> latestRuns = stream.filter(Files::isDirectory)
                                .filter(d -> d.getFileName().toString().startsWith("RUN-"))
                                .filter(d -> Files.exists(d.resolve("history.json")) || Files.exists(d.resolve("snapshots")))
                                .sorted((a, b) -> {
                                    try {
                                        return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                                    } catch (Exception e) {
                                        return 0;
                                    }
                                })
                                .limit(5)
                                .toList();
                        for (Path p : latestRuns) {
                            targetDirNames.add(p.getFileName().toString());
                        }
                    }
                }
            }

            List<SimpleBenchmarkItem> items = new ArrayList<>();
            for (String dirName : targetDirNames) {
                Path d = savesDir.resolve(dirName);
                if (!Files.exists(d)) {
                    System.err.println("Warning: Run directory not found: " + d);
                    continue;
                }
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
                    if (run.getEngineContributionReport() == null && !Boolean.getBoolean("skipEngineContributions")) {
                        Path scenarioPath = d.resolve("scenario.json");
                        if (Files.exists(scenarioPath)) {
                            try {
                                org.ether.society.model.Scenario sc = EtherJsonUtil.getMapper()
                                        .readValue(scenarioPath.toFile(), org.ether.society.model.Scenario.class);
                                EngineContributionReport contrib = Tier2EngineContributionAnalyzer.analyze(sc, true, run);
                                if (contrib != null) {
                                    run.setEngineContributionReport(contrib);
                                    Tier2EngineContributionAnalyzer.saveReport(d, contrib);
                                }
                            } catch (Exception ex) {
                                System.err.println("Warning: Could not compute Tier 2 engine contributions for " + dirName + ": " + ex.getMessage());
                            }
                        }
                    }
                    items.add(new SimpleBenchmarkItem(scName, dirName));
                }
            }

            if (items.isEmpty()) {
                System.err.println("No valid simulation runs with time-series telemetry found.");
                System.exit(1);
            }

            GroundTruthAuditGenerator.AuditResult audit = GroundTruthAuditGenerator.generateMultiScenarioGroundTruthAudit(
                    items,
                    HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                    item -> repo.getRun(item.getRunId())
            );

            // Print report directly to stdout
            System.out.println(audit.reportMarkdown());

            // Also persist to docs/reports and scratch
            File reportsDir = new File("docs/reports");
            reportsDir.mkdirs();
            File outReport = new File(reportsDir, "scenario_falsification_report.md");
            try (PrintWriter pw = new PrintWriter(outReport)) {
                pw.println(audit.reportMarkdown());
            }
            File scratchReport = new File("scratch/scenario_falsification_report.md");
            scratchReport.getParentFile().mkdirs();
            try (PrintWriter pw = new PrintWriter(scratchReport)) {
                pw.println(audit.reportMarkdown());
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
