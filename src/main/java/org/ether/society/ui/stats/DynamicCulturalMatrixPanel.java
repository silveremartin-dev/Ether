/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.stats;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.model.CulturalEntity;
import org.ether.society.model.Scenario;
import org.ether.society.ui.CulturalAffinityMatrixDialog;
import org.ether.society.ui.CulturalEpochRegistryLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * <h1>Dynamic Cultural Matrix Evolution Component</h1>
 *
 * <p>Live animated cultural affinity matrix \(N \times N\) component integrated into
 * {@link org.ether.society.ui.StatsPanel}. Tracks directional ethnolinguistic affinities,
 * assimilation trajectories, and cultural drift \(\Delta(t - t_0)\) in real-time as the
 * simulation advances or when scrubbing historical snapshots.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 * @see org.ether.society.ui.StatsPanel
 * @see org.ether.society.ui.CulturalAffinityMatrixDialog
 */
public class DynamicCulturalMatrixPanel extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(DynamicCulturalMatrixPanel.class);

    private final Label titleLabel;
    private final Label descLabel;
    private final Label epochBadge;
    private final Label entitiesBadge;
    private final Label meanAffinityBadge;
    private final Label polarizationBadge;

    private final ToggleGroup modeGroup;
    private final RadioButton rbCurrentMatrix;
    private final RadioButton rbDeltaFromT0;
    private final Button btnInspectDialog;

    private final GridPane matrixGrid;
    private final ScrollPane matrixScroll;
    private final Label statusHintLabel;

    private int lastRenderedYear = Integer.MIN_VALUE;
    private boolean isDeltaMode = false;
    private boolean isUpdatingTexts = false;
    private H3SimulationEngine cachedEngine;

    /**
     * Constructs the dynamic cultural matrix panel and initializes UI controls.
     */
    public DynamicCulturalMatrixPanel() {
        setPadding(new Insets(10));
        setSpacing(8);
        getStyleClass().add("card-section");

        // 1. Header with Title and Description
        titleLabel = new Label(I18n.getOrDefault("stats.cultural.title", "🧩 Matrice Culturelle Évolutive (N × N)"));
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        descLabel = new Label(I18n.getOrDefault("stats.cultural.desc",
                "Suivi en temps réel des affinités ethnolinguistiques, distances culturelles et assimilation sociale au fil des siècles."));
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        // 2. Badges Row
        epochBadge = new Label(String.format(I18n.getOrDefault("stats.cultural.epoch_fmt", "Époque : %s"), "0 AD"));
        epochBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #f59e0b; -fx-background-color: rgba(245, 158, 11, 0.15); -fx-padding: 2 6; -fx-background-radius: 4;");

        entitiesBadge = new Label(String.format(I18n.getOrDefault("stats.cultural.active_entities_fmt", "%d Entités Actives"), 0));
        entitiesBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-background-color: rgba(56, 189, 248, 0.15); -fx-padding: 2 6; -fx-background-radius: 4;");

        meanAffinityBadge = new Label(String.format(I18n.getOrDefault("stats.cultural.mean_affinity_fmt", "Affinité Moyenne : %.1f%%"), 50.0));
        meanAffinityBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #10b981; -fx-background-color: rgba(16, 185, 129, 0.15); -fx-padding: 2 6; -fx-background-radius: 4;");

        polarizationBadge = new Label(String.format(I18n.getOrDefault("stats.cultural.polarization_fmt", "Indice de Polarisation : %.2f"), 0.0));
        polarizationBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #ec4899; -fx-background-color: rgba(236, 72, 153, 0.15); -fx-padding: 2 6; -fx-background-radius: 4;");

        HBox badgesBox = new HBox(8, epochBadge, entitiesBadge, meanAffinityBadge, polarizationBadge);
        badgesBox.setAlignment(Pos.CENTER_LEFT);

        // 3. Mode Toggles & Inspect Button
        modeGroup = new ToggleGroup();
        rbCurrentMatrix = new RadioButton(I18n.getOrDefault("stats.cultural.mode_current", "🏛️ Matrice Instantanée (t)"));
        rbCurrentMatrix.setToggleGroup(modeGroup);
        rbCurrentMatrix.setSelected(true);
        rbCurrentMatrix.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        rbCurrentMatrix.setTooltip(new Tooltip(I18n.getOrDefault("stats.cultural.tooltip_mode_current", "Afficher la matrice d'affinité culturelle active à l'année actuelle.")));

        rbDeltaFromT0 = new RadioButton(I18n.getOrDefault("stats.cultural.mode_delta", "⚖️ Évolution depuis t₀ (Δt)"));
        rbDeltaFromT0.setToggleGroup(modeGroup);
        rbDeltaFromT0.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
        rbDeltaFromT0.setTooltip(new Tooltip(I18n.getOrDefault("stats.cultural.tooltip_mode_delta", "Afficher les dérives culturelles et variations d'affinité depuis l'année initiale t₀ du scénario.")));

        modeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            isDeltaMode = rbDeltaFromT0.isSelected();
            forceReRender();
        });

        btnInspectDialog = new Button(I18n.getOrDefault("stats.cultural.btn_inspect", "🔍 Inspecteur"));
        btnInspectDialog.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
        btnInspectDialog.setTooltip(new Tooltip(I18n.getOrDefault("stats.cultural.tooltip_inspect", "Ouvrir l'inspecteur interactif complet de la matrice culturelle avec les paramètres actuels.")));
        btnInspectDialog.setOnAction(e -> openDetailedInspector());

        HBox controlsBar = new HBox(12, rbCurrentMatrix, rbDeltaFromT0, new Region(), btnInspectDialog);
        HBox.setHgrow(controlsBar.getChildren().get(2), Priority.ALWAYS);
        controlsBar.setAlignment(Pos.CENTER_LEFT);

        // 4. Matrix Grid & ScrollPane
        matrixGrid = new GridPane();
        matrixGrid.setHgap(3);
        matrixGrid.setVgap(3);
        matrixGrid.setPadding(new Insets(4));
        matrixGrid.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); -fx-background-radius: 4;");

        matrixScroll = new ScrollPane(matrixGrid);
        matrixScroll.setPrefHeight(150);
        matrixScroll.setMaxHeight(220);
        matrixScroll.setFitToWidth(false);
        matrixScroll.setFitToHeight(false);
        matrixScroll.setPannable(true);
        matrixScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        matrixScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        matrixScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        statusHintLabel = new Label(I18n.getOrDefault("analytics.cultural.cell_hint", "💡 Cliquez sur une cellule pour inspecter l'affinité orientée entre entités."));
        statusHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        statusHintLabel.setWrapText(true);

        getChildren().addAll(titleLabel, descLabel, badgesBox, controlsBar, matrixScroll, statusHintLabel);
    }

    /**
     * Updates the dynamic cultural affinity matrix display according to the active simulation engine and current year.
     *
     * @param engine      active simulation engine
     * @param currentYear current simulation calendar year
     */
    public void updateState(H3SimulationEngine engine, int currentYear) {
        if (engine == null) return;
        this.cachedEngine = engine;

        // Debounce: update only when calendar year changes or on explicit re-render
        if (currentYear == lastRenderedYear) return;
        lastRenderedYear = currentYear;

        String planet = (engine.getCurrentScenario() != null && engine.getCurrentScenario().getPlanetPreset() != null)
                ? engine.getCurrentScenario().getPlanetPreset().getCanonicalPlanet() : "earth";
        Scenario scenario = engine.getCurrentScenario();
        long startYear = scenario != null ? (long) scenario.getStartDateYear() : 0L;

        List<CulturalEntity> entities = CulturalEpochRegistryLoader.loadEpochRegistry(planet, (long) currentYear);
        if (entities.isEmpty()) entities = CulturalEpochRegistryLoader.loadEpochRegistry((long) currentYear);

        final int entityCount = entities.size();
        final List<CulturalEntity> finalEntities = entities;

        Platform.runLater(() -> {
            epochBadge.setText(String.format(I18n.getOrDefault("stats.cultural.epoch_fmt", "Époque : %s"),
                    CulturalEpochRegistryLoader.formatEpochName((long) currentYear)));
            entitiesBadge.setText(String.format(I18n.getOrDefault("stats.cultural.active_entities_fmt", "%d Entités Actives"), entityCount));

            if (finalEntities.isEmpty()) {
                matrixGrid.getChildren().clear();
                Label noData = new Label("⚠️ " + I18n.getOrDefault("cultural.matrix.no_data", "Aucune entité culturelle enregistrée pour cette époque."));
                noData.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10.5px;");
                matrixGrid.add(noData, 0, 0);
                meanAffinityBadge.setText(String.format(I18n.getOrDefault("stats.cultural.mean_affinity_fmt", "Affinité Moyenne : %.1f%%"), 0.0));
                polarizationBadge.setText(String.format(I18n.getOrDefault("stats.cultural.polarization_fmt", "Indice de Polarisation : %.2f"), 0.0));
                return;
            }

            renderGrid(finalEntities, scenario, startYear, currentYear);
        });
    }

    private void forceReRender() {
        int yr = lastRenderedYear;
        lastRenderedYear = Integer.MIN_VALUE;
        if (cachedEngine != null && yr != Integer.MIN_VALUE) {
            updateState(cachedEngine, yr);
        }
    }

    private void renderGrid(List<CulturalEntity> entities, Scenario scenario, long startYear, int currentYear) {
        matrixGrid.getChildren().clear();
        int n = entities.size();
        if (n == 0) return;

        // Headers
        Label corner = new Label("↓ \\ →");
        corner.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-padding: 2;");
        matrixGrid.add(corner, 0, 0);

        for (int j = 0; j < n; j++) {
            CulturalEntity e = entities.get(j);
            VBox colBox = new VBox(2);
            colBox.setAlignment(Pos.CENTER);
            Rectangle badge = createBadge(e.colorHex(), 7);
            Label lbl = new Label(truncate(e.getLocalizedName(), 7));
            lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");
            Tooltip.install(lbl, new Tooltip(e.getLocalizedName()));
            colBox.getChildren().addAll(badge, lbl);
            matrixGrid.add(colBox, j + 1, 0);
        }

        for (int i = 0; i < n; i++) {
            CulturalEntity e = entities.get(i);
            HBox rowBox = new HBox(4);
            rowBox.setAlignment(Pos.CENTER_LEFT);
            Rectangle badge = createBadge(e.colorHex(), 7);
            Label lbl = new Label(truncate(e.getLocalizedName(), 7));
            lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");
            Tooltip.install(lbl, new Tooltip(e.getLocalizedName()));
            rowBox.getChildren().addAll(badge, lbl);
            matrixGrid.add(rowBox, 0, i + 1);
        }

        // Baseline entities at t0 for delta evaluation
        String planetT0 = (cachedEngine != null && cachedEngine.getCurrentScenario() != null && cachedEngine.getCurrentScenario().getPlanetPreset() != null)
                ? cachedEngine.getCurrentScenario().getPlanetPreset().getCanonicalPlanet() : "earth";
        List<CulturalEntity> entitiesT0 = CulturalEpochRegistryLoader.loadEpochRegistry(planetT0, startYear);
        if (entitiesT0.isEmpty()) entitiesT0 = entities;

        double sumAffinity = 0.0;
        double sumSqDiff = 0.0;
        int count = n * n;

        for (int i = 0; i < n; i++) {
            CulturalEntity eOrig = entities.get(i);
            for (int j = 0; j < n; j++) {
                CulturalEntity eTarg = entities.get(j);

                double affCurrent = getAffinity(scenario, eOrig, eTarg);
                double affT0 = getAffinity(scenario, findOrFallback(entitiesT0, eOrig.id()), findOrFallback(entitiesT0, eTarg.id()));
                double delta = affCurrent - affT0;

                sumAffinity += affCurrent;
                sumSqDiff += (affCurrent - 0.5) * (affCurrent - 0.5);

                StackPane cell = new StackPane();
                cell.setPrefSize(58, 24);
                cell.setMinSize(50, 20);
                cell.setCursor(Cursor.HAND);

                Label valLbl = new Label();
                valLbl.setStyle("-fx-font-size: 9.5px; -fx-font-weight: bold; -fx-text-fill: white;");

                if (isDeltaMode) {
                    valLbl.setText(String.format(Locale.ROOT, "%+.0f%%", delta * 100.0));
                    cell.setStyle(computeDeltaStyle(delta));
                } else {
                    valLbl.setText(String.format(Locale.ROOT, "%.0f%%", affCurrent * 100.0));
                    Color bg = computeAffinityColor(affCurrent);
                    cell.setStyle(String.format(Locale.ROOT, "-fx-background-color: rgba(%d, %d, %d, 0.85); -fx-background-radius: 3;",
                            (int) (bg.getRed() * 255), (int) (bg.getGreen() * 255), (int) (bg.getBlue() * 255)));
                }

                cell.getChildren().add(valLbl);

                String tooltip = String.format("%s → %s\nAffinity (t): %.1f%%\nAffinity (t₀): %.1f%%\nDelta Δ: %+.1f%%\nKinship: %s",
                        eOrig.getLocalizedName(), eTarg.getLocalizedName(), affCurrent * 100.0, affT0 * 100.0, delta * 100.0, eOrig.kinshipType());
                Tooltip.install(cell, new Tooltip(tooltip));

                final double finalDelta = delta;
                cell.setOnMouseClicked(ev -> statusHintLabel.setText(String.format(
                        "🔍 %s → %s | Affinité : %.1f%% | Évolution depuis t₀ (%d AD) : %+.1f%% | Parenté : %s",
                        eOrig.getLocalizedName(), eTarg.getLocalizedName(), affCurrent * 100.0, startYear, finalDelta * 100.0, eOrig.kinshipType())));

                matrixGrid.add(cell, j + 1, i + 1);
            }
        }

        double mean = count > 0 ? sumAffinity / count : 0.5;
        double variance = count > 0 ? sumSqDiff / count : 0.0;
        double polarization = Math.sqrt(variance) * 2.0;

        meanAffinityBadge.setText(String.format(I18n.getOrDefault("stats.cultural.mean_affinity_fmt", "Affinité Moyenne : %.1f%%"), mean * 100.0));
        polarizationBadge.setText(String.format(I18n.getOrDefault("stats.cultural.polarization_fmt", "Indice de Polarisation : %.2f"), polarization));
    }

    private CulturalEntity findOrFallback(List<CulturalEntity> list, String id) {
        if (id == null) return null;
        for (CulturalEntity e : list) {
            if (id.equals(e.id())) return e;
        }
        return null;
    }

    private static double getAffinity(Scenario s, CulturalEntity e1, CulturalEntity e2) {
        if (e1 == null || e2 == null) return 0.5;
        if (e1.id().equals(e2.id())) return 1.0;
        if (s != null && s.getCulturalAffinityMatrix() != null) {
            String k1 = e1.id() + ":" + e2.id();
            String k2 = e2.id() + ":" + e1.id();
            Double v = s.getCulturalAffinityMatrix().get(k1);
            if (v != null) return v;
            v = s.getCulturalAffinityMatrix().get(k2);
            if (v != null) return v;
        }
        return e1.affinityTowards(e2);
    }

    private static Color computeAffinityColor(double val) {
        val = Math.clamp(val, 0.0, 1.0);
        if (val < 0.5) {
            double t = val / 0.5;
            return Color.rgb((int) (239 * (1.0 - t) + 30 * t), (int) (68 * (1.0 - t) + 41 * t), (int) (68 * (1.0 - t) + 59 * t));
        } else {
            double t = (val - 0.5) / 0.5;
            return Color.rgb((int) (30 * (1.0 - t) + 16 * t), (int) (41 * (1.0 - t) + 185 * t), (int) (59 * (1.0 - t) + 129 * t));
        }
    }

    private static String computeDeltaStyle(double delta) {
        if (Math.abs(delta) < 0.005) {
            return "-fx-background-color: rgba(30, 41, 59, 0.7); -fx-background-radius: 3;";
        } else if (delta > 0) {
            double alpha = Math.min(0.9, 0.25 + delta * 0.65);
            return String.format(Locale.ROOT, "-fx-background-color: rgba(16, 185, 129, %.2f); -fx-background-radius: 3;", alpha);
        } else {
            double alpha = Math.min(0.9, 0.25 + Math.abs(delta) * 0.65);
            return String.format(Locale.ROOT, "-fx-background-color: rgba(239, 68, 68, %.2f); -fx-background-radius: 3;", alpha);
        }
    }

    private static Rectangle createBadge(String colorHex, double size) {
        Rectangle r = new Rectangle(size, size);
        r.setArcWidth(2);
        r.setArcHeight(2);
        try {
            r.setFill(Color.web(colorHex != null ? colorHex : "#94a3b8"));
        } catch (Exception ex) {
            r.setFill(Color.GRAY);
        }
        return r;
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max - 1) + "…" : text;
    }

    private void openDetailedInspector() {
        long epoch = lastRenderedYear != Integer.MIN_VALUE ? (long) lastRenderedYear : 0L;
        Scenario sc = cachedEngine != null ? cachedEngine.getCurrentScenario() : null;
        CulturalAffinityMatrixDialog dlg = new CulturalAffinityMatrixDialog(epoch, sc);
        dlg.show();
    }

    /**
     * Updates localized UI strings on language change.
     */
    public void updateTexts() {
        isUpdatingTexts = true;
        try {
            titleLabel.setText(I18n.getOrDefault("stats.cultural.title", "🧩 Matrice Culturelle Évolutive (N × N)"));
            descLabel.setText(I18n.getOrDefault("stats.cultural.desc",
                    "Suivi en temps réel des affinités ethnolinguistiques, distances culturelles et assimilation sociale au fil des siècles."));
            rbCurrentMatrix.setText(I18n.getOrDefault("stats.cultural.mode_current", "🏛️ Matrice Instantanée (t)"));
            rbDeltaFromT0.setText(I18n.getOrDefault("stats.cultural.mode_delta", "⚖️ Évolution depuis t₀ (Δt)"));
            btnInspectDialog.setText(I18n.getOrDefault("stats.cultural.btn_inspect", "🔍 Inspecteur"));
        } finally {
            isUpdatingTexts = false;
        }
        forceReRender();
    }
}
