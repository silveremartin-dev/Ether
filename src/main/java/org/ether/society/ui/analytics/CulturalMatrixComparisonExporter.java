/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.i18n.I18n;
import org.ether.society.model.CulturalEntity;
import org.ether.society.model.Scenario;
import org.ether.society.ui.CulturalEpochRegistryLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * <h1>Cultural Matrix Comparison Exporter</h1>
 *
 * <p>Handles exporting comparative cultural affinity matrices, discrepancy metrics,
 * and JSON/CSV reports to local files or clipboard.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 * @see CulturalMatrixComparisonPane
 */
final class CulturalMatrixComparisonExporter {

    private static final Logger logger = LoggerFactory.getLogger(CulturalMatrixComparisonExporter.class);

    private CulturalMatrixComparisonExporter() {
        // Pure stateless exporter
    }

    static void exportCsv(Window window, ScenarioSelectableItem itemA, ScenarioSelectableItem itemB, Long epoch) {
        if (itemA == null || itemA.getScenario() == null) return;
        Scenario scA = itemA.getScenario();
        Scenario scB = itemB != null ? itemB.getScenario() : scA;

        long targetEpoch = epoch != null ? epoch : scA.getStartDateYear();
        List<CulturalEntity> entities = CulturalMatrixComparisonHelper.alignEntities(
                CulturalEpochRegistryLoader.loadEpochRegistry(itemA.getPlanetDisplayName(), targetEpoch),
                CulturalEpochRegistryLoader.loadEpochRegistry(itemB != null ? itemB.getPlanetDisplayName() : itemA.getPlanetDisplayName(), targetEpoch)
        );

        FileChooser fc = new FileChooser();
        fc.setTitle(I18n.getOrDefault("analytics.cultural.export_csv", "Export Cultural Matrices (CSV)"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        String idA = itemA.getScenario() != null && itemA.getScenario().getId() != null ? String.valueOf(itemA.getScenario().getId()) : (itemA.getRunId() != null ? itemA.getRunId() : "scenario_a");
        String idB = itemB != null ? (itemB.getScenario() != null && itemB.getScenario().getId() != null ? String.valueOf(itemB.getScenario().getId()) : (itemB.getRunId() != null ? itemB.getRunId() : "scenario_b")) : "self";
        fc.setInitialFileName(String.format("cultural_matrix_diff_%s_vs_%s_%d.csv", idA, idB, targetEpoch));

        File file = fc.showSaveDialog(window);
        if (file == null) return;

        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
            fw.write("Origin_Entity,Target_Entity,Affinity_Scenario_A,Affinity_Scenario_B,Delta_B_minus_A\n");
            for (CulturalEntity e1 : entities) {
                for (CulturalEntity e2 : entities) {
                    double aA = CulturalMatrixComparisonHelper.getAffinity(scA, e1, e2);
                    double aB = CulturalMatrixComparisonHelper.getAffinity(scB, e1, e2);
                    fw.write(String.format(Locale.US, "%s,%s,%.4f,%.4f,%+.4f\n",
                            e1.id(), e2.id(), aA, aB, aB - aA));
                }
            }
            logger.info("Exported cultural matrix discrepancy CSV to {}", file.getAbsolutePath());
        } catch (IOException ex) {
            logger.error("Failed to export cultural matrix CSV", ex);
        }
    }

    static void exportJson(Window window, ScenarioSelectableItem itemA, ScenarioSelectableItem itemB, Long epoch) {
        if (itemA == null || itemA.getScenario() == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle(I18n.getOrDefault("analytics.cultural.export_json", "Export JSON"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
        fc.setInitialFileName("cultural_matrix_comparison.json");

        File file = fc.showSaveDialog(window);
        if (file == null) return;

        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
            fw.write(buildJsonReport(itemA, itemB, epoch));
            logger.info("Exported cultural comparison JSON to {}", file.getAbsolutePath());
        } catch (IOException ex) {
            logger.error("Failed to export cultural comparison JSON", ex);
        }
    }

    static String buildJsonReport(ScenarioSelectableItem itemA, ScenarioSelectableItem itemB, Long epoch) {
        return String.format(Locale.US, """
                {
                  "baselineScenario": "%s",
                  "targetScenario": "%s",
                  "epoch": %d,
                  "exportTimestamp": "%s"
                }
                """, itemA.getName(), itemB != null ? itemB.getName() : itemA.getName(),
                epoch != null ? epoch : 0L, java.time.Instant.now().toString());
    }

    static void copyReportToClipboard(ScenarioSelectableItem itemA, ScenarioSelectableItem itemB,
                                      Long epoch, String frobeniusText, String madText, String maxShiftText) {
        if (itemA == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("# Cultural Affinity Matrix Comparison Report\n\n");
        sb.append(String.format("- **Baseline (A)**: %s (%s)\n", itemA.getName(), itemA.getPlanetDisplayName()));
        sb.append(String.format("- **Target (B)**: %s\n", itemB != null ? itemB.getName() : "None"));
        sb.append(String.format("- **Selected Epoch**: %s\n", epoch != null ? CulturalEpochRegistryLoader.formatEpochName(epoch) : "N/A"));
        sb.append(String.format("- %s\n", frobeniusText));
        sb.append(String.format("- %s\n", madText));
        sb.append(String.format("- %s\n", maxShiftText));

        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(sb.toString());
        clipboard.setContent(content);
    }
}
