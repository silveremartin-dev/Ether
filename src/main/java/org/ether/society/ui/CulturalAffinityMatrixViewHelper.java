/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.model.CulturalEntity;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.ether.society.i18n.I18n;
import org.ether.society.model.CulturalEntity;
import org.ether.society.util.DeterministicPRNG;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;

/**
 * Stateless heatmap renderer, entity registry card builder, quick-editor card builder,
 * and deterministic procedural matrix generator for {@link CulturalAffinityMatrixDialog}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see CulturalAffinityMatrixDialog
 */
final class CulturalAffinityMatrixViewHelper {

    private CulturalAffinityMatrixViewHelper() {
        // Stateless UI helper
    }

    /**
     * Configures the bottom symmetric bilateral quick-editor panel, slider/spinner bindings,
     * and canonical assimilation preset chips.
     *
     * @param quickEditorCard      root VBox container for the quick editor
     * @param quickEditorTitle     header label
     * @param quickEditorPairLabel selected entity-pair label
     * @param quickAffinitySlider  0..100 percentage slider
     * @param quickAffinitySpinner 0..100 percentage spinner
     * @param presetChipsBox       horizontal box for preset buttons
     * @param canEdit              predicate indicating whether user edits should propagate
     * @param onApplyAffinity01    callback receiving normalized affinity in {@code [0, 1]}
     */
    static void configureQuickEditorCard(VBox quickEditorCard,
                                         Label quickEditorTitle,
                                         Label quickEditorPairLabel,
                                         Slider quickAffinitySlider,
                                         Spinner<Double> quickAffinitySpinner,
                                         HBox presetChipsBox,
                                         BooleanSupplier canEdit,
                                         DoubleConsumer onApplyAffinity01) {
        quickEditorCard.setPadding(new Insets(10, 14, 10, 14));
        quickEditorCard.setStyle("-fx-background-color: rgba(30, 41, 59, 0.7); -fx-background-radius: 8; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 8;");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        quickEditorTitle.setText(I18n.getOrDefault("cultural.matrix.quick_editor_title", "✏️ Éditeur d'Affinité Symétrique Directe :"));
        quickEditorTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");
        quickEditorPairLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11.5px; -fx-text-fill: #f8fafc;");
        topRow.getChildren().addAll(quickEditorTitle, quickEditorPairLabel);

        HBox sliderRow = new HBox(10);
        sliderRow.setAlignment(Pos.CENTER_LEFT);

        Label minLbl = new Label("0%");
        minLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-font-weight: bold;");
        minLbl.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.min", "Affinité minimale (0% - Hostilité absolue / Barrière infranchissable)")));

        quickAffinitySlider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(quickAffinitySlider, Priority.ALWAYS);
        quickAffinitySlider.setShowTickMarks(true);
        quickAffinitySlider.setMajorTickUnit(25);
        quickAffinitySlider.setBlockIncrement(5);
        quickAffinitySlider.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.slider", "Ajuster le coefficient d'affinité symétrique bilatérale (0% à 100%).")));

        Label maxLbl = new Label("100%");
        maxLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-font-weight: bold;");
        maxLbl.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.max", "Affinité maximale (100% - Fusion & assimilation culturelle totale)")));

        quickAffinitySpinner.setEditable(true);
        quickAffinitySpinner.setPrefWidth(95);
        quickAffinitySpinner.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.spinner", "Valeur numérique exacte du pourcentage d'affinité (0.0% à 100.0%).")));
        if (quickAffinitySpinner.getEditor() != null) {
            quickAffinitySpinner.getEditor().setStyle("-fx-text-fill: #f8fafc; -fx-background-color: rgba(15, 23, 42, 0.8); -fx-font-weight: bold;");
        }

        boolean[] localSync = {false};
        quickAffinitySlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (!localSync[0] && canEdit.getAsBoolean() && newV != null) {
                localSync[0] = true;
                try {
                    quickAffinitySpinner.getValueFactory().setValue(Math.round(newV.doubleValue() * 10.0) / 10.0);
                    onApplyAffinity01.accept(newV.doubleValue() / 100.0);
                } finally {
                    localSync[0] = false;
                }
            }
        });

        quickAffinitySpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (!localSync[0] && canEdit.getAsBoolean() && newV != null) {
                localSync[0] = true;
                try {
                    quickAffinitySlider.setValue(newV);
                    onApplyAffinity01.accept(newV / 100.0);
                } finally {
                    localSync[0] = false;
                }
            }
        });

        sliderRow.getChildren().addAll(minLbl, quickAffinitySlider, maxLbl, quickAffinitySpinner);

        HBox presetRow = new HBox(10);
        presetRow.setAlignment(Pos.CENTER_LEFT);

        Label presetsLabel = new Label(I18n.getOrDefault("cultural.matrix.presets_label", "⚡ Préréglages d'assimilation :"));
        presetsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
        presetsLabel.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.presets_bar", "Appliquer instantanément des régimes canoniques d'affinité bilatérale.")));

        presetChipsBox.setAlignment(Pos.CENTER_LEFT);
        presetChipsBox.getChildren().clear();
        DoubleConsumer presetApply = valPct -> {
            if (!localSync[0] && canEdit.getAsBoolean()) {
                localSync[0] = true;
                try {
                    quickAffinitySlider.setValue(valPct);
                    quickAffinitySpinner.getValueFactory().setValue(valPct);
                    onApplyAffinity01.accept(valPct / 100.0);
                } finally {
                    localSync[0] = false;
                }
            }
        };
        addPresetChip(presetChipsBox, I18n.getOrDefault("cultural.matrix.preset_hostile", "⚔️ 0% Hostile"), 0.0,
                I18n.getOrDefault("cultural.matrix.preset_hostile_tt", "Hostilité irréconciliable (0%) : Conflits armés endémiques, barrières douanières absolues, refus total d'intermariage."), presetApply);
        addPresetChip(presetChipsBox, I18n.getOrDefault("cultural.matrix.preset_refractory", "🛡️ 25% Réfractaire"), 25.0,
                I18n.getOrDefault("cultural.matrix.preset_refractory_tt", "Réfractaire & Méfiant (25%) : Frottements culturels, frictions frontalières, échanges marginaux."), presetApply);
        addPresetChip(presetChipsBox, I18n.getOrDefault("cultural.matrix.preset_peaceful", "🤝 50% Neutre"), 50.0,
                I18n.getOrDefault("cultural.matrix.preset_peaceful_tt", "Neutre & Pacifique (50%) : Coexistence stable, commerce régulier sans intégration linguistique."), presetApply);
        addPresetChip(presetChipsBox, I18n.getOrDefault("cultural.matrix.preset_allied", "🏛️ 75% Allié"), 75.0,
                I18n.getOrDefault("cultural.matrix.preset_allied_tt", "Allié & Partenaire (75%) : Forte proximité culturelle, traités militaires, flux marchands intensifs."), presetApply);
        addPresetChip(presetChipsBox, I18n.getOrDefault("cultural.matrix.preset_assimilated", "💍 100% Assimilé"), 100.0,
                I18n.getOrDefault("cultural.matrix.preset_assimilated_tt", "Assimilation Totale (100%) : Fusion culturelle, continuité dialectale, intermariages illimités."), presetApply);

        presetRow.getChildren().addAll(presetsLabel, presetChipsBox);
        quickEditorCard.getChildren().setAll(topRow, sliderRow, presetRow);
    }

    private static void addPresetChip(HBox presetChipsBox, String label, double valPct, String tooltipText, DoubleConsumer onSelect) {
        Button btn = new Button(label);
        btn.getStyleClass().add("button-secondary");
        btn.setStyle("-fx-font-size: 10.5px; -fx-padding: 3 8; -fx-font-weight: bold;");
        if (tooltipText != null) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
        btn.setOnAction(e -> onSelect.accept(valPct));
        presetChipsBox.getChildren().add(btn);
    }

    /**
     * Populates the ethnographic entity cards tab.
     *
     * @param entitiesBox    target VBox container
     * @param activeEntities active cultural entities for the selected epoch
     */
    static void renderEntitiesTab(VBox entitiesBox, List<CulturalEntity> activeEntities) {
        entitiesBox.getChildren().clear();
        if (activeEntities.isEmpty()) {
            Label empty = new Label(I18n.getOrDefault("cultural.matrix.no_data", "Aucune entité culturelle trouvée pour cette époque."));
            empty.getStyleClass().add("card-description-muted");
            empty.setStyle("-fx-text-fill: #94a3b8;");
            entitiesBox.getChildren().add(empty);
            return;
        }

        for (CulturalEntity entity : activeEntities) {
            VBox card = new VBox(4);
            card.setPadding(new Insets(8, 12, 8, 12));
            card.getStyleClass().add("card-section");
            card.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 6;");

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);

            Rectangle colorBadge = createColorBadge(entity.colorHex(), 16, 4);

            Label nameLbl = new Label(entity.getLocalizedName());
            nameLbl.getStyleClass().add("card-title");
            nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #f8fafc;");

            Label idLbl = new Label("[" + entity.id() + "]");
            idLbl.getStyleClass().add("card-description-muted");
            idLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

            header.getChildren().addAll(colorBadge, nameLbl, idLbl);

            Label kinshipLbl = new Label("• " + I18n.getOrDefault("cultural.entity.kinship", "Structure Sociale & Parenté : ") + entity.kinshipType());
            kinshipLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");

            Label lithicLbl = new Label("• " + I18n.getOrDefault("cultural.entity.techno", "Technocomplexe Lithique / Métallurgique : ") + entity.lithicTechnocomplex());
            lithicLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

            StringBuilder traitsSb = new StringBuilder("• Traits: [");
            for (int i = 0; i < entity.traits().length; i++) {
                if (i > 0) traitsSb.append(", ");
                traitsSb.append(String.format("%.2f", entity.traits()[i]));
            }
            traitsSb.append("]");
            Label traitsLbl = new Label(traitsSb.toString());
            traitsLbl.setStyle("-fx-font-size: 10.5px; -fx-font-family: monospace; -fx-text-fill: #38bdf8;");

            card.getChildren().addAll(header, kinshipLbl, lithicLbl, traitsLbl);
            entitiesBox.getChildren().add(card);
        }
    }

    /**
     * Builds the interactive N x N cultural affinity heatmap grid.
     */
    static void buildMatrixGrid(GridPane matrixGrid,
                                List<CulturalEntity> activeEntities,
                                StackPane[][] cellPanes,
                                Label[][] cellLabels,
                                BiFunction<CulturalEntity, CulturalEntity, Double> affinityFn,
                                int selectedI,
                                int selectedJ,
                                BiConsumer<Integer, Integer> onSelectCell) {
        matrixGrid.getChildren().clear();
        int n = activeEntities.size();
        if (n == 0) return;

        Label corner = new Label(I18n.getOrDefault("cultural.matrix.entities_label", "Entités \\ Entités"));
        corner.getStyleClass().add("control-label");
        corner.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-padding: 4;");
        matrixGrid.add(corner, 0, 0);

        for (int j = 0; j < n; j++) {
            CulturalEntity colEnt = activeEntities.get(j);
            VBox colHeader = new VBox(3);
            colHeader.setAlignment(Pos.CENTER);
            colHeader.setPrefWidth(115);
            colHeader.setMinWidth(110);
            colHeader.setPadding(new Insets(4, 2, 4, 2));

            Rectangle badge = createColorBadge(colEnt.colorHex(), 12, 3);
            Label lbl = new Label(colEnt.getLocalizedName());
            lbl.getStyleClass().add("control-label");
            lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #f8fafc; -fx-text-alignment: center;");
            lbl.setWrapText(true);
            lbl.setAlignment(Pos.CENTER);
            lbl.setTooltip(new Tooltip(colEnt.getLocalizedName() + " [" + colEnt.id() + "]"));

            colHeader.getChildren().addAll(badge, lbl);
            matrixGrid.add(colHeader, j + 1, 0);
        }

        for (int i = 0; i < n; i++) {
            CulturalEntity rowEnt = activeEntities.get(i);
            HBox rowHeader = new HBox(6);
            rowHeader.setAlignment(Pos.CENTER_LEFT);
            rowHeader.setPrefWidth(190);
            rowHeader.setMinWidth(180);
            rowHeader.setPadding(new Insets(2, 6, 2, 6));

            Rectangle badge = createColorBadge(rowEnt.colorHex(), 12, 3);
            Label lbl = new Label(rowEnt.getLocalizedName());
            lbl.getStyleClass().add("control-label");
            lbl.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
            lbl.setWrapText(true);
            lbl.setTooltip(new Tooltip(rowEnt.getLocalizedName() + " [" + rowEnt.id() + "]"));

            rowHeader.getChildren().addAll(badge, lbl);
            matrixGrid.add(rowHeader, 0, i + 1);

            for (int j = 0; j < n; j++) {
                final int finalI = i;
                final int finalJ = j;

                StackPane cell = new StackPane();
                cell.setPrefSize(115, 36);
                cell.setMinSize(75, 28);
                cell.setCursor(Cursor.HAND);

                Label valLbl = new Label();
                valLbl.setMinWidth(Region.USE_PREF_SIZE);
                cell.getChildren().add(valLbl);

                cellPanes[i][j] = cell;
                cellLabels[i][j] = valLbl;

                updateCellDisplay(activeEntities, cellPanes, cellLabels, affinityFn, i, j, selectedI, selectedJ);
                cell.setOnMouseClicked(e -> onSelectCell.accept(finalI, finalJ));
                matrixGrid.add(cell, j + 1, i + 1);
            }
        }
    }

    private static Rectangle createColorBadge(String colorHex, double size, double arc) {
        Rectangle badge = new Rectangle(size, size);
        try {
            badge.setFill(Color.web(colorHex));
        } catch (Exception ignored) {
            badge.setFill(Color.WHITE);
        }
        badge.setArcWidth(arc);
        badge.setArcHeight(arc);
        badge.setStroke(Color.rgb(100, 116, 139, 0.8));
        return badge;
    }

    /**
     * Updates a single matrix cell's heatmap color, label text, border highlight, and localized tooltip.
     */
    static void updateCellDisplay(List<CulturalEntity> activeEntities,
                                  StackPane[][] cellPanes,
                                  Label[][] cellLabels,
                                  BiFunction<CulturalEntity, CulturalEntity, Double> affinityFn,
                                  int i, int j,
                                  int selectedI, int selectedJ) {
        if (i < 0 || j < 0 || i >= cellPanes.length || j >= cellPanes[0].length) return;
        StackPane cell = cellPanes[i][j];
        Label lbl = cellLabels[i][j];
        if (cell == null || lbl == null) return;

        CulturalEntity rowEnt = activeEntities.get(i);
        CulturalEntity colEnt = activeEntities.get(j);
        double aff = affinityFn.apply(rowEnt, colEnt);
        double dist = rowEnt.distanceTo(colEnt);

        Color bg = computeHeatmapColor(aff);
        boolean isSel = (i == selectedI && j == selectedJ) || (i == selectedJ && j == selectedI);

        String borderStyle = isSel
                ? "-fx-border-color: #38bdf8; -fx-border-width: 2px; -fx-border-radius: 4;"
                : (i == j
                        ? "-fx-border-color: #64748b; -fx-border-width: 1px; -fx-border-radius: 4;"
                        : "-fx-border-color: #334155; -fx-border-width: 1px; -fx-border-radius: 4;");

        cell.setStyle(String.format("-fx-background-color: #%02X%02X%02X; -fx-background-radius: 4; %s",
                (int) (bg.getRed() * 255), (int) (bg.getGreen() * 255), (int) (bg.getBlue() * 255), borderStyle));

        lbl.setText(String.format("%.1f%%", aff * 100.0));
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #ffffff;");

        String tooltipText = String.format(
                "🔍 %s ↔ %s\n• %s : %.1f%%\n• %s : %.3f\n• %s : %.2f\n• %s : %.2f\n\n👉 %s",
                rowEnt.getLocalizedName(), colEnt.getLocalizedName(),
                I18n.getOrDefault("cultural.tooltip.affinity", "Affinité Culturelle"), aff * 100.0,
                I18n.getOrDefault("cultural.tooltip.distance", "Distance Euclidienne"), dist,
                I18n.getOrDefault("cultural.tooltip.trade_flow", "Facteur Contagion Commerciale"), aff * 0.85,
                I18n.getOrDefault("cultural.tooltip.intermarriage", "Probabilité d'Intermariage"), Math.pow(aff, 1.5),
                (i == j
                        ? I18n.getOrDefault("cultural.matrix.diagonal_locked", "🔒 Auto-identité (100% invariant)")
                        : I18n.getOrDefault("cultural.tooltip.click_edit", "Cliquez pour modifier directement dans l'éditeur rapide."))
        );
        Tooltip.install(cell, new Tooltip(tooltipText));
    }

    static void populateByGeography(List<CulturalEntity> entities, Map<String, Double> overrides) {
        int n = entities.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                CulturalEntity e1 = entities.get(i);
                CulturalEntity e2 = entities.get(j);
                double aff = Math.exp(-2.2 * (Math.abs(i - j) / (double) n));
                overrides.put(e1.id() + ":" + e2.id(), aff);
                overrides.put(e2.id() + ":" + e1.id(), aff);
            }
        }
    }

    static void populateByTraits(List<CulturalEntity> entities, Map<String, Double> overrides) {
        int n = entities.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                CulturalEntity e1 = entities.get(i);
                CulturalEntity e2 = entities.get(j);
                double aff = e1.affinityWith(e2);
                overrides.put(e1.id() + ":" + e2.id(), aff);
                overrides.put(e2.id() + ":" + e1.id(), aff);
            }
        }
    }

    static void populateStochastic(List<CulturalEntity> entities,
                                   Map<String, Double> overrides,
                                   BiFunction<CulturalEntity, CulturalEntity, Double> affinityFn,
                                   long seed) {
        int n = entities.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                CulturalEntity e1 = entities.get(i);
                CulturalEntity e2 = entities.get(j);
                double base = affinityFn.apply(e1, e2);
                double u1 = Math.max(1e-9, DeterministicPRNG.nextDouble(seed, i, j, 9101L));
                double u2 = DeterministicPRNG.nextDouble(seed, i, j, 9102L);
                double z = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
                double aff = Math.clamp(base + z * 0.12, 0.02, 0.98);
                overrides.put(e1.id() + ":" + e2.id(), aff);
                overrides.put(e2.id() + ":" + e1.id(), aff);
            }
        }
    }

    static Color computeHeatmapColor(double val) {
        val = Math.clamp(val, 0.0, 1.0);
        if (val < 0.5) {
            double t = val / 0.5;
            return Color.rgb((int) (20 + t * (15 - 20)), (int) (30 + t * (110 - 30)), (int) (65 + t * (140 - 65)));
        } else {
            double t = (val - 0.5) / 0.5;
            return Color.rgb((int) (15 + t * (30 - 15)), (int) (110 + t * (175 - 110)), (int) (140 + t * (110 - 140)));
        }
    }
}
