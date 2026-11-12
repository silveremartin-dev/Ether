/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.ether.society.i18n.I18n;
import org.ether.society.model.CulturalEntity;
import org.ether.society.model.Scenario;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Renders cultural affinity grids and differential delta tables on JavaFX {@link GridPane} layouts.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public final class CulturalMatrixGridRenderer {

    private CulturalMatrixGridRenderer() {
        // Utility renderer
    }

    /**
     * Creates a standardized styled matrix GridPane.
     */
    public static GridPane createMatrixGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(3);
        grid.setVgap(3);
        grid.setStyle("-fx-background-color: rgba(15, 23, 42, 0.4); -fx-background-radius: 6; -fx-padding: 8;");
        return grid;
    }

    /**
     * Populates an absolute cultural affinity matrix grid for a scenario.
     */
    public static void populateAffinityGrid(
            GridPane grid, Scenario s, List<CulturalEntity> entities,
            String scenarioName, Consumer<String> detailCallback) {
        grid.getChildren().clear();
        int n = entities.size();
        if (n == 0) return;

        addHeaders(grid, entities);

        for (int i = 0; i < n; i++) {
            CulturalEntity eOrig = entities.get(i);
            for (int j = 0; j < n; j++) {
                CulturalEntity eTarg = entities.get(j);
                double val = CulturalMatrixComparisonHelper.getAffinity(s, eOrig, eTarg);

                StackPane cell = new StackPane();
                cell.setPrefSize(72, 28);
                cell.setMinSize(64, 24);

                Color bg = CulturalMatrixComparisonHelper.computeAffinityColor(val);
                cell.setStyle(String.format(Locale.ROOT, "-fx-background-color: rgba(%d, %d, %d, 0.85); -fx-background-radius: 4;",
                        (int) (bg.getRed() * 255), (int) (bg.getGreen() * 255), (int) (bg.getBlue() * 255)));

                Label lbl = new Label(String.format(Locale.ROOT, "%.0f%%", val * 100.0));
                lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;");
                cell.getChildren().add(lbl);

                String tooltip = String.format(I18n.getOrDefault("analytics.cultural.cell_tooltip_affinity_fmt",
                                "%s\nOrigin: %s\nTarget: %s\nAffinity: %.1f%%"),
                        scenarioName, eOrig.getLocalizedName(), eTarg.getLocalizedName(), val * 100.0);
                Tooltip.install(cell, new Tooltip(tooltip));

                cell.setCursor(Cursor.HAND);
                cell.setOnMouseClicked(e -> {
                    if (detailCallback != null) {
                        detailCallback.accept(String.format("[%s] %s → %s: %.1f%% affinity",
                                scenarioName, eOrig.getLocalizedName(), eTarg.getLocalizedName(), val * 100.0));
                    }
                });

                grid.add(cell, j + 1, i + 1);
            }
        }
    }

    /**
     * Populates a differential affinity matrix grid \(\Delta(B - A)\) and returns summary statistics.
     */
    public static CulturalMatrixComparisonHelper.MatrixStats populateDeltaGrid(
            GridPane grid, Scenario sA, Scenario sB, List<CulturalEntity> entities,
            String nameA, String nameB, Consumer<String> detailCallback) {
        grid.getChildren().clear();
        int n = entities.size();
        if (n == 0) return new CulturalMatrixComparisonHelper.MatrixStats(0, 0, 0, 0, 0, "-", "-");

        addHeaders(grid, entities);

        double sumA = 0, sumB = 0, sumSqDelta = 0, sumAbsDelta = 0;
        double maxDelta = 0;
        String maxOrig = "-", maxTarg = "-";
        int count = n * n;

        for (int i = 0; i < n; i++) {
            CulturalEntity eOrig = entities.get(i);
            for (int j = 0; j < n; j++) {
                CulturalEntity eTarg = entities.get(j);
                double aA = CulturalMatrixComparisonHelper.getAffinity(sA, eOrig, eTarg);
                double aB = CulturalMatrixComparisonHelper.getAffinity(sB, eOrig, eTarg);
                double delta = aB - aA;

                sumA += aA;
                sumB += aB;
                sumSqDelta += delta * delta;
                sumAbsDelta += Math.abs(delta);

                if (Math.abs(delta) > Math.abs(maxDelta)) {
                    maxDelta = delta;
                    maxOrig = eOrig.getLocalizedName();
                    maxTarg = eTarg.getLocalizedName();
                }

                StackPane cell = new StackPane();
                cell.setPrefSize(72, 28);
                cell.setMinSize(64, 24);
                cell.setStyle(CulturalMatrixComparisonHelper.computeDeltaCellStyle(delta));

                Label lbl = new Label(String.format(Locale.ROOT, "%+.0f%%", delta * 100.0));
                lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;");
                cell.getChildren().add(lbl);

                String tooltip = String.format(I18n.getOrDefault("analytics.cultural.cell_tooltip_fmt",
                                "Origin: %s\nTarget: %s\nBaseline (A): %.1f%%\nTarget (B): %.1f%%\nDelta Δ: %+.1f%%"),
                        eOrig.getLocalizedName(), eTarg.getLocalizedName(), aA * 100.0, aB * 100.0, delta * 100.0);
                Tooltip.install(cell, new Tooltip(tooltip));

                cell.setCursor(Cursor.HAND);
                final double finalDelta = delta;
                cell.setOnMouseClicked(e -> {
                    if (detailCallback != null) {
                        detailCallback.accept(String.format("Δ = %+.1f%% | %s → %s | %s: %.1f%% vs %s: %.1f%%",
                                finalDelta * 100.0, eOrig.getLocalizedName(), eTarg.getLocalizedName(), nameA, aA * 100.0, nameB, aB * 100.0));
                    }
                });

                grid.add(cell, j + 1, i + 1);
            }
        }

        double meanA = sumA / count;
        double meanB = sumB / count;
        double frob = Math.sqrt(sumSqDelta);
        double mad = sumAbsDelta / count;

        return new CulturalMatrixComparisonHelper.MatrixStats(meanA, meanB, frob, mad, maxDelta, maxOrig, maxTarg);
    }

    /**
     * Adds row and column labels and color badges to the grid.
     */
    public static void addHeaders(GridPane grid, List<CulturalEntity> entities) {
        int n = entities.size();
        Label corner = new Label("Orig \\ Targ");
        corner.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-padding: 2;");
        grid.add(corner, 0, 0);

        for (int j = 0; j < n; j++) {
            CulturalEntity e = entities.get(j);
            VBox colBox = new VBox(2);
            colBox.setAlignment(Pos.CENTER);
            Rectangle badge = CulturalMatrixComparisonHelper.createBadge(e.colorHex(), 8);
            Label lbl = new Label(CulturalMatrixComparisonHelper.truncateName(e.getLocalizedName(), 8));
            lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");
            Tooltip.install(lbl, new Tooltip(e.getLocalizedName()));
            colBox.getChildren().addAll(badge, lbl);
            grid.add(colBox, j + 1, 0);
        }

        for (int i = 0; i < n; i++) {
            CulturalEntity e = entities.get(i);
            HBox rowBox = new HBox(4);
            rowBox.setAlignment(Pos.CENTER_LEFT);
            Rectangle badge = CulturalMatrixComparisonHelper.createBadge(e.colorHex(), 8);
            Label lbl = new Label(CulturalMatrixComparisonHelper.truncateName(e.getLocalizedName(), 8));
            lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");
            Tooltip.install(lbl, new Tooltip(e.getLocalizedName()));
            rowBox.getChildren().addAll(badge, lbl);
            grid.add(rowBox, 0, i + 1);
        }
    }
}
