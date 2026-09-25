/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.ether.society.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive Dialog & Heatmap Inspector for Cliodynamic Cultural Affinity & Distance Matrices.
 * Displays discrete 24-bit RGB Entity traits, technocomplexes, and pairwise cultural affinities (N x N).
 * Supports live dynamic theming (Dark, Light, Presentation) and full localization across 5 languages.
 *
 * @author Silvere Martin-Michiellot
 */
public class CulturalAffinityMatrixDialog extends Stage {
    private static final Logger logger = LoggerFactory.getLogger(CulturalAffinityMatrixDialog.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record CulturalEntity(
            String id,
            String colorHex,
            int[] colorRgb,
            double[] traits,
            String nameEn,
            String nameFr,
            String nameDe,
            String nameEs,
            String nameZh,
            String kinshipType,
            String lithicTechnocomplex
    ) {
        public String getLocalizedName() {
            String lang = I18n.getCurrentLanguage() != null ? I18n.getCurrentLanguage().getCode() : "en";
            if ("fr".equalsIgnoreCase(lang)) return nameFr != null ? nameFr : nameEn;
            if ("de".equalsIgnoreCase(lang)) return nameDe != null ? nameDe : nameEn;
            if ("es".equalsIgnoreCase(lang)) return nameEs != null ? nameEs : nameEn;
            if ("zh".equalsIgnoreCase(lang)) return nameZh != null ? nameZh : nameEn;
            return nameEn != null ? nameEn : id;
        }

        public double distanceTo(CulturalEntity other) {
            if (other == null || this.traits == null || other.traits == null) return 1.0;
            double sumSq = 0.0;
            int n = Math.min(this.traits.length, other.traits.length);
            for (int i = 0; i < n; i++) {
                double diff = this.traits[i] - other.traits[i];
                sumSq += diff * diff;
            }
            return Math.sqrt(sumSq);
        }

        public double affinityWith(CulturalEntity other) {
            double dist = distanceTo(other);
            return Math.exp(-3.5 * dist);
        }
    }

    private final ComboBox<Long> epochSelector = new ComboBox<>();
    private final Label titleLabel = new Label();
    private final Label subtitleLabel = new Label();
    private final VBox entitiesBox = new VBox(8);
    private final GridPane matrixGrid = new GridPane();
    private final ScrollPane matrixScroll = new ScrollPane(matrixGrid);
    private final Button btnExportCsv = new Button();
    private final Button btnClose = new Button();

    private List<CulturalEntity> activeEntities = new ArrayList<>();
    private long currentEpoch = -100000L;

    public CulturalAffinityMatrixDialog(long initialEpoch) {
        this.currentEpoch = initialEpoch;
        initModality(Modality.APPLICATION_MODAL);
        setTitle(I18n.getOrDefault("cultural.matrix.window_title", "🧩 Pairwise Cultural Affinity & Distance Matrix"));

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("glass-panel");

        // Header Section
        titleLabel.setText(I18n.getOrDefault("cultural.matrix.header_title", "🏛️ Cliodynamic Cultural & Trait Affinity Matrix (N × N)"));
        titleLabel.getStyleClass().add("panel-header");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        subtitleLabel.setText(I18n.getOrDefault("cultural.matrix.header_desc", "Inspect pairwise cultural compatibility, linguistic drift, kinship structures, and technological contagion rates."));
        subtitleLabel.getStyleClass().add("card-description-muted");
        subtitleLabel.setStyle("-fx-font-size: 12px;");

        // Epoch Selector Row
        HBox epochRow = new HBox(10);
        epochRow.setAlignment(Pos.CENTER_LEFT);
        Label lblEpoch = new Label(I18n.getOrDefault("cultural.matrix.select_epoch", "Historical Epoch:"));
        lblEpoch.getStyleClass().add("control-label");
        lblEpoch.setStyle("-fx-font-weight: bold;");

        epochSelector.getItems().addAll(-100000L, -50000L, -25000L, -20000L, -10900L, -10000L, -8000L, -6000L, -3000L, -1900L, -1000L, 0L, 1000L, 2026L);
        epochSelector.setValue(closestSupportedEpoch(initialEpoch));
        epochSelector.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(formatEpochName(item));
                }
            }
        });
        epochSelector.setButtonCell(epochSelector.getCellFactory().call(null));
        epochSelector.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.epoch", "Select the historical epoch to inspect reconstructed ethnolinguistic entities and affinity structures.")));
        epochSelector.setOnAction(e -> {
            Long val = epochSelector.getValue();
            if (val != null) {
                loadEpochRegistry(val);
            }
        });
        epochRow.getChildren().addAll(lblEpoch, epochSelector);

        // TabPane: Tab 1 = Affinity Heatmap Matrix, Tab 2 = Cultural Entities Registry
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Tab 1: Matrix Heatmap
        matrixGrid.setHgap(6);
        matrixGrid.setVgap(6);
        matrixGrid.setPadding(new Insets(10));
        matrixScroll.setFitToWidth(true);
        matrixScroll.setStyle("-fx-background-color: transparent;");

        Tab tabMatrix = new Tab(I18n.getOrDefault("cultural.matrix.tab_heatmap", "📊 Affinity Heatmap (N × N)"), matrixScroll);
        tabMatrix.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.tab_heatmap", "Pairwise affinity percentage matrix calculated via exponential Euclidean distance in multidimensional trait space.")));

        // Tab 2: Entities List
        ScrollPane entitiesScroll = new ScrollPane(entitiesBox);
        entitiesScroll.setFitToWidth(true);
        entitiesBox.setPadding(new Insets(10));
        Tab tabEntities = new Tab(I18n.getOrDefault("cultural.matrix.tab_entities", "📜 Cultural Registry & Technocomplexes"), entitiesScroll);
        tabEntities.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.tab_entities", "Discrete ethnolinguistic entities with 24-bit color coding, kinship regimes, and lithic technocomplexes.")));

        tabPane.getTabs().addAll(tabMatrix, tabEntities);

        // Footer Actions
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);

        btnExportCsv.setText(I18n.getOrDefault("cultural.matrix.btn_export", "💾 Export Matrix (CSV)"));
        btnExportCsv.getStyleClass().add("button-secondary");
        btnExportCsv.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.export", "Export the complete N×N cultural affinity matrix as a standard CSV file.")));
        btnExportCsv.setOnAction(e -> exportMatrixCsv());

        btnClose.setText(I18n.getOrDefault("common.btn.close", "Close"));
        btnClose.getStyleClass().add("button-secondary");
        btnClose.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.close", "Close the cultural affinity inspector.")));
        btnClose.setOnAction(e -> close());

        footer.getChildren().addAll(btnExportCsv, btnClose);

        root.getChildren().addAll(titleLabel, subtitleLabel, epochRow, tabPane, footer);

        Scene scene = new Scene(root, 840, 640);
        setScene(scene);
        Theme.applyCurrentTheme(scene);
        Theme.themeProperty().addListener((obs, oldV, newV) -> Theme.applyCurrentTheme(scene));
        WindowUtils.applyWindowIcon(this);

        loadEpochRegistry(epochSelector.getValue());
    }

    private static long closestSupportedEpoch(long yr) {
        long[] supported = {-100000L, -50000L, -25000L, -20000L, -1900L, -1000L};
        long best = supported[0];
        long minDiff = Math.abs(yr - best);
        for (long s : supported) {
            long d = Math.abs(yr - s);
            if (d < minDiff) {
                minDiff = d;
                best = s;
            }
        }
        return best;
    }

    private static String formatEpochName(long yr) {
        if (yr == -100000L) return I18n.getOrDefault("cultural.epoch.eemian", "–100 000 BP (Out-of-Africa / Eemian)");
        if (yr == -50000L)  return I18n.getOrDefault("cultural.epoch.sahul", "–50 000 BP (Sahul Maritime Settlement)");
        if (yr == -25000L)  return I18n.getOrDefault("cultural.epoch.beringia", "–25 000 BP (Beringian Standstill & Gravettian)");
        if (yr == -20000L)  return I18n.getOrDefault("cultural.epoch.lgm", "–20 000 BP (Last Glacial Maximum & Solutrean)");
        if (yr == -1900L)   return I18n.getOrDefault("cultural.epoch.bronze", "–1 900 BCE (Middle Bronze Age / Babylon & Shang)");
        if (yr == -1000L)   return I18n.getOrDefault("cultural.epoch.iron", "–1 000 BCE (Early Iron Age / Phoenicians & Zhou)");
        return (yr < 0 ? Math.abs(yr) + " BCE" : yr + " CE");
    }

    private void loadEpochRegistry(long epoch) {
        this.currentEpoch = epoch;
        activeEntities.clear();
        Path p = Paths.get("data", "maps", "ether", "earth", String.valueOf(epoch), "cultural_registry.json");
        if (!Files.exists(p)) {
            // Try fallback to closest available registry
            long closest = closestSupportedEpoch(epoch);
            p = Paths.get("data", "maps", "ether", "earth", String.valueOf(closest), "cultural_registry.json");
        }

        if (Files.exists(p)) {
            try {
                JsonNode root = MAPPER.readTree(p.toFile());
                JsonNode entitiesNode = root.get("entities");
                if (entitiesNode != null && entitiesNode.isArray()) {
                    for (JsonNode eNode : entitiesNode) {
                        String id = eNode.path("id").asText("unknown");
                        String colorHex = eNode.path("colorHex").asText("#FFFFFF");
                        int[] colorRgb = new int[]{255, 255, 255};
                        if (eNode.has("colorRgb") && eNode.get("colorRgb").isArray()) {
                            colorRgb[0] = eNode.get("colorRgb").get(0).asInt(255);
                            colorRgb[1] = eNode.get("colorRgb").get(1).asInt(255);
                            colorRgb[2] = eNode.get("colorRgb").get(2).asInt(255);
                        }
                        double[] traits = new double[4];
                        if (eNode.has("traits") && eNode.get("traits").isArray()) {
                            for (int i = 0; i < 4 && i < eNode.get("traits").size(); i++) {
                                traits[i] = eNode.get("traits").get(i).asDouble(0.0);
                            }
                        }
                        JsonNode nameNode = eNode.path("name");
                        String nameEn = nameNode.path("en").asText(id);
                        String nameFr = nameNode.path("fr").asText(nameEn);
                        String nameDe = nameNode.path("de").asText(nameEn);
                        String nameEs = nameNode.path("es").asText(nameEn);
                        String nameZh = nameNode.path("zh").asText(nameEn);
                        String kinship = eNode.path("kinshipType").asText("Bilateral Foragers");
                        String lithic = eNode.path("lithicTechnocomplex").asText("Middle Paleolithic");

                        activeEntities.add(new CulturalEntity(id, colorHex, colorRgb, traits, nameEn, nameFr, nameDe, nameEs, nameZh, kinship, lithic));
                    }
                }
            } catch (Exception ex) {
                logger.warn("Failed to parse cultural_registry.json for epoch {}: {}", epoch, ex.getMessage());
            }
        }

        renderEntitiesTab();
        renderMatrixHeatmap();
    }

    private void renderEntitiesTab() {
        entitiesBox.getChildren().clear();
        if (activeEntities.isEmpty()) {
            Label empty = new Label(I18n.getOrDefault("cultural.matrix.no_data", "No cultural entities found for this epoch."));
            empty.getStyleClass().add("card-description-muted");
            entitiesBox.getChildren().add(empty);
            return;
        }

        for (CulturalEntity entity : activeEntities) {
            VBox card = new VBox(4);
            card.setPadding(new Insets(8, 12, 8, 12));
            card.getStyleClass().add("card-section");

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);

            Rectangle colorBadge = new Rectangle(16, 16);
            try {
                colorBadge.setFill(Color.web(entity.colorHex()));
            } catch (Exception ignored) {
                colorBadge.setFill(Color.WHITE);
            }
            colorBadge.setArcWidth(4);
            colorBadge.setArcHeight(4);
            colorBadge.setStroke(Color.rgb(100, 116, 139, 0.6));

            Label nameLbl = new Label(entity.getLocalizedName());
            nameLbl.getStyleClass().add("card-title");
            nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

            Label idLbl = new Label("[" + entity.id() + "]");
            idLbl.getStyleClass().add("card-description-muted");
            idLbl.setStyle("-fx-font-size: 11px;");

            header.getChildren().addAll(colorBadge, nameLbl, idLbl);

            Label kinshipLbl = new Label("• " + I18n.getOrDefault("cultural.entity.kinship", "Kinship & Social Structure: ") + entity.kinshipType());
            kinshipLbl.getStyleClass().add("control-label");
            kinshipLbl.setStyle("-fx-font-size: 11px;");

            Label lithicLbl = new Label("• " + I18n.getOrDefault("cultural.entity.techno", "Lithic / Metallurgy Technocomplex: ") + entity.lithicTechnocomplex());
            lithicLbl.getStyleClass().add("card-description-muted");
            lithicLbl.setStyle("-fx-font-size: 11px;");

            StringBuilder traitsSb = new StringBuilder("• Traits: [");
            for (int i = 0; i < entity.traits().length; i++) {
                if (i > 0) traitsSb.append(", ");
                traitsSb.append(String.format("%.2f", entity.traits()[i]));
            }
            traitsSb.append("]");
            Label traitsLbl = new Label(traitsSb.toString());
            traitsLbl.getStyleClass().add("card-description-muted");
            traitsLbl.setStyle("-fx-font-size: 10px; -fx-font-family: monospace;");

            card.getChildren().addAll(header, kinshipLbl, lithicLbl, traitsLbl);
            entitiesBox.getChildren().add(card);
        }
    }

    private void renderMatrixHeatmap() {
        matrixGrid.getChildren().clear();
        int n = activeEntities.size();
        if (n == 0) return;

        // Top-Left empty corner
        Label corner = new Label(I18n.getOrDefault("cultural.matrix.entities_label", "Entities \\ Entities"));
        corner.getStyleClass().add("control-label");
        corner.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        matrixGrid.add(corner, 0, 0);

        // Column headers
        for (int j = 0; j < n; j++) {
            CulturalEntity colEnt = activeEntities.get(j);
            VBox colHeader = new VBox(2);
            colHeader.setAlignment(Pos.CENTER);
            colHeader.setPrefWidth(90);

            Rectangle badge = new Rectangle(12, 12);
            try { badge.setFill(Color.web(colEnt.colorHex())); } catch (Exception ignored) { badge.setFill(Color.WHITE); }
            badge.setArcWidth(3); badge.setArcHeight(3);
            badge.setStroke(Color.rgb(100, 116, 139, 0.6));

            Label lbl = new Label(shortenName(colEnt.getLocalizedName()));
            lbl.getStyleClass().add("control-label");
            lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            lbl.setTooltip(new Tooltip(colEnt.getLocalizedName()));

            colHeader.getChildren().addAll(badge, lbl);
            matrixGrid.add(colHeader, j + 1, 0);
        }

        // Rows
        for (int i = 0; i < n; i++) {
            CulturalEntity rowEnt = activeEntities.get(i);
            HBox rowHeader = new HBox(6);
            rowHeader.setAlignment(Pos.CENTER_LEFT);
            rowHeader.setPrefWidth(160);

            Rectangle badge = new Rectangle(12, 12);
            try { badge.setFill(Color.web(rowEnt.colorHex())); } catch (Exception ignored) { badge.setFill(Color.WHITE); }
            badge.setArcWidth(3); badge.setArcHeight(3);
            badge.setStroke(Color.rgb(100, 116, 139, 0.6));

            Label lbl = new Label(rowEnt.getLocalizedName());
            lbl.getStyleClass().add("control-label");
            lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            lbl.setTooltip(new Tooltip(rowEnt.getLocalizedName()));

            rowHeader.getChildren().addAll(badge, lbl);
            matrixGrid.add(rowHeader, 0, i + 1);

            // Cells
            for (int j = 0; j < n; j++) {
                CulturalEntity colEnt = activeEntities.get(j);
                double affinity = rowEnt.affinityWith(colEnt);
                double dist = rowEnt.distanceTo(colEnt);

                StackPane cell = new StackPane();
                cell.setPrefSize(90, 36);

                Color cellColor = computeHeatmapColor(affinity);
                cell.setStyle(String.format("-fx-background-color: #%02X%02X%02X; -fx-background-radius: 4; -fx-border-color: #334155; -fx-border-radius: 4;",
                        (int)(cellColor.getRed() * 255), (int)(cellColor.getGreen() * 255), (int)(cellColor.getBlue() * 255)));

                Label valLbl = new Label(String.format("%.1f%%", affinity * 100.0));
                valLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: " + (affinity > 0.4 ? "#ffffff" : "#f1f5f9") + ";");
                cell.getChildren().add(valLbl);

                String tooltipText = String.format(
                        "🔍 %s ↔ %s\n" +
                        "• %s : %.1f%%\n" +
                        "• %s : %.3f\n" +
                        "• %s : %.2f\n" +
                        "• %s : %.2f",
                        rowEnt.getLocalizedName(), colEnt.getLocalizedName(),
                        I18n.getOrDefault("cultural.tooltip.affinity", "Cultural Affinity"), affinity * 100.0,
                        I18n.getOrDefault("cultural.tooltip.distance", "Euclidean Trait Distance"), dist,
                        I18n.getOrDefault("cultural.tooltip.trade_flow", "Trade Contagion Factor"), affinity * 0.85,
                        I18n.getOrDefault("cultural.tooltip.intermarriage", "Intermarriage Probability"), Math.pow(affinity, 1.5)
                );
                Tooltip.install(cell, new Tooltip(tooltipText));

                matrixGrid.add(cell, j + 1, i + 1);
            }
        }
    }

    private static Color computeHeatmapColor(double val) {
        val = Math.clamp(val, 0.0, 1.0);
        if (val < 0.5) {
            double t = val / 0.5;
            // From dark indigo (20, 20, 55) to dark cyan (15, 100, 110)
            return Color.rgb((int)(20 + t * (15 - 20)), (int)(20 + t * (100 - 20)), (int)(55 + t * (110 - 55)));
        } else {
            double t = (val - 0.5) / 0.5;
            // From dark cyan (15, 100, 110) to lush emerald/gold (16, 160, 95)
            return Color.rgb((int)(15 + t * (16 - 15)), (int)(100 + t * (160 - 100)), (int)(110 + t * (95 - 110)));
        }
    }

    private static String shortenName(String name) {
        if (name == null) return "";
        if (name.length() <= 12) return name;
        return name.substring(0, 10) + "…";
    }

    private void exportMatrixCsv() {
        if (activeEntities.isEmpty()) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("cultural.matrix.export_title", "Export Cultural Affinity Matrix (CSV)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        chooser.setInitialFileName("cultural_affinity_matrix_" + Math.abs(currentEpoch) + ".csv");
        File file = chooser.showSaveDialog(this);
        if (file != null) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.print("EntityID");
                for (CulturalEntity col : activeEntities) {
                    pw.print("," + col.id());
                }
                pw.println();

                for (CulturalEntity row : activeEntities) {
                    pw.print(row.id());
                    for (CulturalEntity col : activeEntities) {
                        pw.printf(",%.4f", row.affinityWith(col));
                    }
                    pw.println();
                }
                logger.info("Exported cultural affinity matrix to {}", file.getAbsolutePath());
            } catch (Exception ex) {
                logger.error("Failed to export matrix to CSV: {}", ex.getMessage());
            }
        }
    }
}
