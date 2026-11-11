/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.i18n.I18n;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.ether.society.ui.analytics.SpatialTensorDiskCatalog.RASTER_IMAGE_CACHE;

/**
 * <h1>Spatial Tensor Multi-Scenario Renderer</h1>
 *
 * <p>Renders horizontal gallery cards for multi-scenario 2D cartographic comparisons,
 * computing parallel spatial discrepancy heatmaps and updating markdown metrics tables.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 * @see SpatialTensorComparisonPane
 */
final class SpatialTensorMultiScenarioRenderer {

    private SpatialTensorMultiScenarioRenderer() {
        // Pure stateless renderer
    }

    static void render(
            ScenarioSelectableItem baselineItem,
            int targetYear,
            String channelKey,
            long currentReqId,
            List<ScenarioSelectableItem> activeSelectedScenarios,
            HBox multiScenarioBox,
            AtomicLong spatialComparisonRequestId,
            Function<ScenarioSelectableItem, SimulationRunRecord> runResolver,
            TextArea spatialMetricsReportArea,
            SpatialTensorUIHelper uiHelper) {

        if (multiScenarioBox == null) return;
        multiScenarioBox.getChildren().clear();

        // 1. Baseline Reference Card
        ImageView baseView = new ImageView();
        baseView.setFitWidth(280);
        baseView.setFitHeight(140);
        baseView.setPreserveRatio(true);
        baseView.getStyleClass().add("card-section");

        Label baseTitle = new Label("👑 " + I18n.getOrDefault("analytics.cultural.baseline", "Baseline: ") + baselineItem.getName());
        baseTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-size: 11px;");

        VBox baseCard = new VBox(4, baseTitle, baseView);
        baseCard.setAlignment(Pos.CENTER);
        baseCard.setPadding(new Insets(6));
        baseCard.setStyle("-fx-background-color: rgba(30, 41, 59, 0.7); -fx-background-radius: 6; -fx-border-color: #38bdf8; -fx-border-radius: 6;");

        uiHelper.attachHoverZoom(baseView, () -> "Baseline: " + baselineItem.getName());
        multiScenarioBox.getChildren().add(baseCard);

        StringBuilder reportSb = new StringBuilder();
        reportSb.append(String.format(Locale.ROOT, "# Multi-Scenario Cartographic Comparison (Year %d)\n\n", targetYear));
        reportSb.append(String.format(Locale.ROOT, "- **Channel**: %s\n", channelKey));
        reportSb.append(String.format(Locale.ROOT, "- **Baseline Reference**: %s\n\n", baselineItem.getName()));
        reportSb.append("| Target Scenario | Pearson r | SSIM | RMSE | Jaccard IoU | KL Divergence | Fidelity Status |\n");
        reportSb.append("|---|---|---|---|---|---|---|\n");

        for (ScenarioSelectableItem targetItem : activeSelectedScenarios) {
            if (targetItem == baselineItem) continue;

            ImageView targetView = new ImageView();
            targetView.setFitWidth(210);
            targetView.setFitHeight(105);
            targetView.setPreserveRatio(true);
            targetView.getStyleClass().add("card-section");

            ImageView diffView = new ImageView();
            diffView.setFitWidth(210);
            diffView.setFitHeight(105);
            diffView.setPreserveRatio(true);
            diffView.getStyleClass().add("card-section");

            Label targetTitle = new Label("📊 " + targetItem.getName());
            targetTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc; -fx-font-size: 11px;");

            Label metricsLabel = new Label("⏳ Evaluating...");
            metricsLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

            Label lblTargetMap = new Label(targetItem.getName());
            lblTargetMap.setStyle("-fx-font-size: 9px; -fx-text-fill: #cbd5e1;");
            Label lblDiffMap = new Label("Δ vs Baseline");
            lblDiffMap.setStyle("-fx-font-size: 9px; -fx-text-fill: #f59e0b; -fx-font-weight: bold;");

            VBox boxTarget = new VBox(2, lblTargetMap, targetView);
            boxTarget.setAlignment(Pos.CENTER);
            VBox boxDiff = new VBox(2, lblDiffMap, diffView);
            boxDiff.setAlignment(Pos.CENTER);

            HBox dualMaps = new HBox(8, boxTarget, boxDiff);
            dualMaps.setAlignment(Pos.CENTER);

            VBox card = new VBox(4, targetTitle, dualMaps, metricsLabel);
            card.setAlignment(Pos.CENTER);
            card.setPadding(new Insets(6));
            card.setStyle("-fx-background-color: rgba(15, 23, 42, 0.7); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 6;");

            uiHelper.attachHoverZoom(targetView, targetItem::getName);
            uiHelper.attachHoverZoom(diffView, () -> "Δ(" + baselineItem.getName() + " - " + targetItem.getName() + ")");
            multiScenarioBox.getChildren().add(card);

            SpatialTensorDiffEngine.compute2DComparisonAsync(
                    baselineItem, targetItem, targetYear, channelKey, runResolver, RASTER_IMAGE_CACHE, total -> {}
            ).thenAccept(res -> Platform.runLater(() -> {
                if (currentReqId != spatialComparisonRequestId.get()) return;
                if (baseView.getImage() == null && res.imgA() != null) {
                    baseView.setImage(res.imgA());
                }
                targetView.setImage(res.imgB());
                diffView.setImage(res.imgDiff());
                if (res.metrics() != null) {
                    var m = res.metrics();
                    metricsLabel.setText(String.format(Locale.ROOT, "SSIM: %.3f | RMSE: %.1f | r: %.3f | IoU: %.1f%%",
                            m.getSsim(), m.getRmse(), m.getPearsonR(), m.getJaccardIndex() * 100.0));
                    reportSb.append(String.format(Locale.ROOT, "| %s | %.4f | %.4f | %.2f | %.2f%% | %.4f | %s |\n",
                            targetItem.getName(), m.getPearsonR(), m.getSsim(), m.getRmse(), m.getJaccardIndex() * 100.0, m.getKlDivergence(), m.getDiagnosticSummary()));
                    if (spatialMetricsReportArea != null) {
                        spatialMetricsReportArea.setText(reportSb.toString());
                    }
                }
            }));
        }
    }
}
