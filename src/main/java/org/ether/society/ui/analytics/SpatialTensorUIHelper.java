/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.ether.society.i18n.I18n;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Package-private UI helper for {@link SpatialTensorComparisonPane}.
 *
 * <p>Manages the floating hover magnifier popup, full-screen cartographic modal dialog,
 * and 5-language localization / dynamic custom cultural tensor channel registration.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see SpatialTensorComparisonPane
 */
final class SpatialTensorUIHelper {

    private final Popup hoverMapPopup = new Popup();
    private final ImageView popupImageView = new ImageView();
    private final Label popupTitleLabel = new Label();
    private Timeline popupHideTimer;

    SpatialTensorUIHelper() {
        popupImageView.setFitWidth(560);
        popupImageView.setFitHeight(280);
        popupImageView.setPreserveRatio(true);
        popupTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 4 8; -fx-font-size: 13px;");

        VBox popupBox = new VBox(6, popupTitleLabel, popupImageView);
        popupBox.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.96);" +
                "-fx-padding: 10px;" +
                "-fx-border-color: #38bdf8;" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 12, 0, 0, 4);"
        );
        hoverMapPopup.getContent().add(popupBox);
        hoverMapPopup.setAutoHide(false);
    }

    /**
     * Attaches hover-zoom magnifier and primary-click modal inspection to a map {@link ImageView}.
     *
     * @param view          target map image view
     * @param titleSupplier supplier for localized map title
     */
    void attachHoverZoom(ImageView view, Supplier<String> titleSupplier) {
        view.setCursor(Cursor.HAND);
        view.setOnMouseEntered(e -> {
            if (view.getImage() != null) {
                cancelHideTimer();
                popupImageView.setImage(view.getImage());
                popupTitleLabel.setText(titleSupplier.get());
                Bounds bounds = view.localToScreen(view.getBoundsInLocal());
                if (bounds != null) {
                    double popupX = bounds.getMinX();
                    double popupY = bounds.getMaxY() + 8;
                    Rectangle2D screen = Screen.getPrimary().getVisualBounds();
                    if (popupY + 300 > screen.getMaxY()) popupY = bounds.getMinY() - 310;
                    if (popupX + 580 > screen.getMaxX()) popupX = screen.getMaxX() - 590;
                    hoverMapPopup.show(view, Math.max(10, popupX), Math.max(10, popupY));
                }
            }
        });
        view.setOnMouseExited(e -> scheduleHidePopup());
        view.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && view.getImage() != null) {
                showFullMapModal(titleSupplier.get(), view.getImage());
            }
        });
    }

    private void scheduleHidePopup() {
        cancelHideTimer();
        popupHideTimer = new Timeline(new KeyFrame(Duration.millis(200), ev -> hoverMapPopup.hide()));
        popupHideTimer.setCycleCount(1);
        popupHideTimer.play();
    }

    private void cancelHideTimer() {
        if (popupHideTimer != null) {
            popupHideTimer.stop();
            popupHideTimer = null;
        }
    }

    static void showFullMapModal(String title, Image img) {
        if (img == null) return;
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(title != null ? title : I18n.getOrDefault("analytics.preview.modal_title", "2D Cartographic Tensor High-Resolution View"));

        ImageView largeView = new ImageView(img);
        largeView.setFitWidth(800);
        largeView.setFitHeight(400);
        largeView.setPreserveRatio(true);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-size: 15px;");

        Button closeBtn = new Button(I18n.getOrDefault("analytics.preview.close", "Close"));
        closeBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 6 16; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> modal.close());
        closeBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.close_modal", "Close detailed view dialog.")));

        VBox layout = new VBox(12, titleLbl, largeView, closeBtn);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(16));
        layout.setStyle("-fx-background-color: #0f172a;");

        modal.setScene(new Scene(layout, 840, 500));
        modal.show();
    }

    /**
     * Updates localized labels, legend badges, and dynamic custom tensor channels for {@link SpatialTensorComparisonPane}.
     *
     * @param p            host spatial tensor comparison pane
     * @param scenarioList active scenarios pool
     */
    static void updateLocalizedControls(SpatialTensorComparisonPane p, List<ScenarioSelectableItem> scenarioList) {
        if (p.channelLabel != null) p.channelLabel.setText(I18n.getOrDefault("analytics.channel_label", "Tensor Channel:"));
        if (p.scenarioSelectLabelA != null) p.scenarioSelectLabelA.setText(I18n.getOrDefault("analytics.label.select_a", "Baseline (A):"));
        if (p.scenarioSelectLabelB != null) p.scenarioSelectLabelB.setText(I18n.getOrDefault("analytics.label.select_b", "Target (B):"));

        if (p.playTimelineBtn != null) {
            p.playTimelineBtn.setText(p.timelineAnimator.isPlayingAnimation()
                    ? I18n.getOrDefault("analytics.btn.pause_timeline", "⏸️ Pause")
                    : I18n.getOrDefault("analytics.btn.play_timeline", "▶️ Play Timeline"));
        }

        if (p.export2DZipBtn != null) {
            p.export2DZipBtn.setText(I18n.getOrDefault("analytics.btn.export_2d_zip", "📦 Export 2D Tensors (.zip)"));
            p.export2DZipBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.export_2d_zip", "Export all 2D comparative maps and spatial metrics report into a timestamped ZIP archive.")));
        }

        if (p.legendBlue != null) p.legendBlue.setText("🟦 " + I18n.getOrDefault("analytics.legend.under", "A < B (Underestimation)"));
        if (p.legendSlate != null) p.legendSlate.setText("⬛ " + I18n.getOrDefault("analytics.legend.zero", "Δ = 0 (Identical / Zero divergence)"));
        if (p.legendGreen != null) p.legendGreen.setText("🟩 " + I18n.getOrDefault("analytics.legend.close", "|Δ| ≤ 5% (Close agreement)"));
        if (p.legendRed != null) p.legendRed.setText("🟥 " + I18n.getOrDefault("analytics.legend.over", "A > B (Overestimation)"));

        populateSpatialChannelCombo(p.spatialChannelCombo, scenarioList);
    }

    private static void populateSpatialChannelCombo(ComboBox<String> spatialChannelCombo, List<ScenarioSelectableItem> scenarioList) {
        if (spatialChannelCombo == null) return;
        int selectedIdx = Math.max(0, spatialChannelCombo.getSelectionModel().getSelectedIndex());
        var oldAction = spatialChannelCombo.getOnAction();
        spatialChannelCombo.setOnAction(null);
        spatialChannelCombo.getItems().setAll(
                I18n.getOrDefault("analytics.spatial.density", "👥 Demographic Density"),
                I18n.getOrDefault("analytics.spatial.technology", "🔬 Technology & Tooling Level"),
                I18n.getOrDefault("analytics.spatial.temperature", "🌡️ Surface Temperature & Climate"),
                I18n.getOrDefault("analytics.spatial.aquifers", "💧 Aquifers & Freshwater Tables"),
                I18n.getOrDefault("analytics.spatial.agriculture", "🌾 Agricultural Biomass & Soils"),
                I18n.getOrDefault("analytics.spatial.sovereignty", "👑 Political Sovereignty & Borders"),
                I18n.getOrDefault("analytics.spatial.linguistic", "🗣️ Linguistic Isoglosses (Languages)"),
                I18n.getOrDefault("analytics.spatial.kinship", "🧬 Kinship & Family Structures"),
                I18n.getOrDefault("analytics.spatial.rituals", "🔮 Sacred Beliefs & Ritual Practices"),
                I18n.getOrDefault("analytics.spatial.trade", "🛣️ Trade Corridors & Exchange Routes"),
                I18n.getOrDefault("analytics.spatial.institutional", "⚖️ Institutional Complexity (Seshat)"),
                I18n.getOrDefault("analytics.spatial.ecological", "⚠️ Ecological Footprint & Overshoot"),
                I18n.getOrDefault("analytics.spatial.pathogen", "🧬 Pathogen Burden & Zoonotic Risk")
        );

        Set<String> customChannels = new LinkedHashSet<>();
        if (scenarioList != null) {
            for (ScenarioSelectableItem item : scenarioList) {
                if (item != null && item.getScenario() != null) {
                    int dims = item.getScenario().getCultureVectorDimensions();
                    var customNames = item.getScenario().getCustomTensorNames();
                    int maxDim = Math.max(dims, (customNames != null && !customNames.isEmpty())
                            ? customNames.keySet().stream().mapToInt(k -> k + 1).max().orElse(0) : 0);
                    for (int tIdx = 9; tIdx < maxDim; tIdx++) {
                        int oneBased = tIdx + 1;
                        String rawName = customNames != null ? customNames.get(tIdx) : null;
                        boolean hasCustomName = rawName != null && !rawName.isBlank();
                        String displayName = hasCustomName ? rawName.trim() : (I18n.getOrDefault("scenario.tensor.custom.fallback", "Custom Layer") + " " + oneBased);
                        String sanitized = displayName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_").replaceAll("^_+|_+$", "");
                        String slug = "custom_tensor_" + String.format("%02d", oneBased) + (sanitized.isBlank() ? "" : "_" + sanitized);
                        customChannels.add("🧬 " + I18n.getOrDefault("scenario.tensor.label_prefix", "Tensor ") + "[" + oneBased + "]: " + displayName + " (" + slug + ")");
                    }
                }
            }
        }
        for (String customCh : customChannels) {
            if (!spatialChannelCombo.getItems().contains(customCh)) {
                spatialChannelCombo.getItems().add(customCh);
            }
        }

        if (selectedIdx >= spatialChannelCombo.getItems().size()) selectedIdx = 0;
        spatialChannelCombo.getSelectionModel().select(selectedIdx);
        spatialChannelCombo.setOnAction(oldAction);
    }
}
