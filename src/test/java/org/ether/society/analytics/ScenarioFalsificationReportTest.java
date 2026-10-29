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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Standardized Epistemic Falsification Test Harness.
 *
 * <p>Loads simulation execution snapshots directly from {@code saves/}, evaluates them
 * against historical ground truth datasets (HYDE 3.4, Maddison, EPICA, Seshat),
 * and generates the standardized cliodynamic falsification audit report.</p>
 */
public class ScenarioFalsificationReportTest {

    public record SimpleBenchmarkItem(String name, String runId) implements ScenarioBenchmarkItem {
        @Override
        public String getName() { return name; }
        @Override
        public String getRunId() { return runId; }
    }

    @Test
    public void generateFalsificationAuditReport() throws Exception {
        SimulationRunRepository repo = SimulationRunRepository.getInstance();
        repo.clear();

        Path savesDir = EtherPaths.getSavesDir();
        String runsProp = System.getProperty("runs");

        List<String> targetDirNames = new ArrayList<>();
        if (runsProp != null && !runsProp.isBlank()) {
            for (String part : runsProp.split("[,;\\s]+")) {
                if (!part.isBlank()) targetDirNames.add(part.trim());
            }
        } else {
            // Automatically find the most recent runs for each target scenario
            List<String> scenarioPrefixes = List.of(
                    "RUN-GUERRES-MONDIALES--RUPTURE-TOTALITAIRE----RE-NUCL-AIRE--1914",
                    "RUN-LA-PESTE-NOIRE---INVERSION--CONOMIQUE-POST-F-ODALE--1347",
                    "RUN-RAMS-S-II--PAX-AEGYPTIACA---B-TISSEURS-MONUMENTAUX---1279"
            );
            for (String prefix : scenarioPrefixes) {
                try (var stream = Files.list(savesDir)) {
                    stream.filter(p -> Files.isDirectory(p) && p.getFileName().toString().startsWith(prefix))
                            .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                            .ifPresent(p -> targetDirNames.add(p.getFileName().toString()));
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
                items.add(new SimpleBenchmarkItem(scName, dirName));
            }
        }

        if (items.isEmpty()) {
            System.err.println("No valid runs loaded.");
            return;
        }

        GroundTruthAuditGenerator.AuditResult audit = GroundTruthAuditGenerator.generateMultiScenarioGroundTruthAudit(
                items,
                HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                item -> repo.getRun(item.getRunId())
        );

        System.out.println("\n" + audit.reportMarkdown());

        File outReport = new File("scratch/scenario_falsification_report.md");
        outReport.getParentFile().mkdirs();
        try (PrintWriter pw = new PrintWriter(outReport)) {
            pw.println(audit.reportMarkdown());
        }
        System.out.println("\n[SUCCESS] Standardized audit report written to: " + outReport.getAbsolutePath());
    }
}
