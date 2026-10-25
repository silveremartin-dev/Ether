/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Handles CSV and JSON export routines for the Cultural Affinity Matrix and registry.
 */
public final class CulturalAffinityMatrixExporter {

    private static final Logger logger = LoggerFactory.getLogger(CulturalAffinityMatrixExporter.class);
    private static final ObjectMapper MAPPER = org.ether.society.persistence.EtherJsonUtil.getMapper();

    private CulturalAffinityMatrixExporter() {
        // Utility class
    }

    public static void exportMatrixCsv(Window owner, long currentEpoch,
                                       List<CulturalAffinityMatrixDialog.CulturalEntity> activeEntities,
                                       BiFunction<CulturalAffinityMatrixDialog.CulturalEntity, CulturalAffinityMatrixDialog.CulturalEntity, Double> affinityProvider) {
        if (activeEntities == null || activeEntities.isEmpty()) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("cultural.matrix.export_title", "Exporter la Matrice d'Affinité Culturelle (CSV)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        chooser.setInitialFileName("cultural_affinity_matrix_" + Math.abs(currentEpoch) + ".csv");
        File file = chooser.showSaveDialog(owner);
        if (file != null) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.print("EntityID");
                for (CulturalAffinityMatrixDialog.CulturalEntity col : activeEntities) {
                    pw.print("," + col.id());
                }
                pw.println();

                for (CulturalAffinityMatrixDialog.CulturalEntity row : activeEntities) {
                    pw.print(row.id());
                    for (CulturalAffinityMatrixDialog.CulturalEntity col : activeEntities) {
                        pw.printf(Locale.ROOT, ",%.4f", affinityProvider.apply(row, col));
                    }
                    pw.println();
                }
                logger.info("Exported cultural affinity matrix to {}", file.getAbsolutePath());
            } catch (Exception ex) {
                logger.error("Failed to export matrix to CSV: {}", ex.getMessage());
            }
        }
    }

    public static void exportMatrixJson(Window owner, long currentEpoch, long scenarioStartYear,
                                        List<CulturalAffinityMatrixDialog.CulturalEntity> activeEntities,
                                        BiFunction<CulturalAffinityMatrixDialog.CulturalEntity, CulturalAffinityMatrixDialog.CulturalEntity, Double> affinityProvider) {
        if (activeEntities == null || activeEntities.isEmpty()) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("cultural.matrix.export_json_title", "Exporter la Matrice d'Affinité Culturelle (JSON)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON (*.json)", "*.json"));
        chooser.setInitialFileName("cultural_affinity_matrix_" + Math.abs(currentEpoch) + ".json");
        File file = chooser.showSaveDialog(owner);
        if (file != null) {
            try {
                Map<String, Object> root = new LinkedHashMap<>();
                root.put("epoch", currentEpoch);
                root.put("scenarioEpoch", scenarioStartYear);
                root.put("entitiesCount", activeEntities.size());
                root.put("entities", activeEntities);

                Map<String, Map<String, Double>> matrix = new LinkedHashMap<>();
                for (CulturalAffinityMatrixDialog.CulturalEntity row : activeEntities) {
                    Map<String, Double> rowMap = new LinkedHashMap<>();
                    for (CulturalAffinityMatrixDialog.CulturalEntity col : activeEntities) {
                        rowMap.put(col.id(), affinityProvider.apply(row, col));
                    }
                    matrix.put(row.id(), rowMap);
                }
                root.put("affinityMatrix", matrix);

                MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, root);
                logger.info("Exported cultural affinity matrix to JSON: {}", file.getAbsolutePath());
            } catch (Exception ex) {
                logger.error("Failed to export matrix to JSON: {}", ex.getMessage());
            }
        }
    }
}
