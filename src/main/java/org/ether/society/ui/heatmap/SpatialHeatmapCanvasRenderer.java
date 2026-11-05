/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.heatmap;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.util.Duration;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.DisplayMode;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Handles multi-layer 2D cartographic canvas rendering, categorized layer menu construction,
 * active layer chip pills, and hover magnifier popup behavior for {@link org.ether.society.ui.SpatialHeatmapPanel}.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 */
public final class SpatialHeatmapCanvasRenderer {

    /**
     * Private constructor preventing instantiation of utility renderer class.
     */
    private SpatialHeatmapCanvasRenderer() {
        // Utility renderer class
    }

    /**
     * Renders the composite multi-layer heatmap and optional Moran's \(I\) spatial autocorrelation label.
     *
     * @param canvas target JavaFX canvas
     * @param currentCells active H3 spatial cells
     * @param activeLayers ordered set of active thematic display modes
     * @param lblMoranI label displaying Moran's \(I\) index (updated when {@code computeMoran} is true)
     * @param computeMoran whether to evaluate and display Moran's \(I\) on the primary scalar layer
     */
    public static void renderMapOnCanvas(
            Canvas canvas,
            List<H3Cell> currentCells,
            Set<DisplayMode> activeLayers,
            Label lblMoranI,
            boolean computeMoran) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.rgb(15, 23, 42, 0.95));
        gc.fillRoundRect(0, 0, w, h, 8, 8);

        if (currentCells == null || currentCells.isEmpty()) {
            gc.setFill(Color.web("#64748b"));
            gc.fillText(I18n.getOrDefault("heatmap.no_data", "Aucune donnée cartographique"), w / 4, h / 2);
            if (lblMoranI != null) {
                lblMoranI.setText(I18n.getOrDefault("heatmap.moran_none", "Indice de Moran (Autocorrélation spatiale) : --"));
            }
            return;
        }

        double minLat = 90, maxLat = -90, minLng = 180, maxLng = -180;
        for (H3Cell c : currentCells) {
            if (c.getLatitude() != null) {
                minLat = Math.min(minLat, c.getLatitude());
                maxLat = Math.max(maxLat, c.getLatitude());
            }
            if (c.getLongitude() != null) {
                minLng = Math.min(minLng, c.getLongitude());
                maxLng = Math.max(maxLng, c.getLongitude());
            }
        }

        if (minLat < -60 && maxLat > 60) {
            minLat = -90.0;
            maxLat = 90.0;
        }
        if (minLng < -150 && maxLng > 150) {
            minLng = -180.0;
            maxLng = 180.0;
        }

        double latRange = Math.max(0.01, maxLat - minLat);
        double lngRange = Math.max(0.01, maxLng - minLng);
        double dotScale = (w > 400) ? 1.8 : 1.0;

        // 1. Base Terrain / Biome geography background
        for (H3Cell c : currentCells) {
            if (c.getLatitude() == null || c.getLongitude() == null) continue;
            double x = ((c.getLongitude() - minLng) / lngRange) * (w - 16) + 8;
            double y = (1.0 - ((c.getLatitude() - minLat) / latRange)) * (h - 16) + 8;

            Color baseBiomeColor = SpatialHeatmapColorPalette.getBiomeBaseColor(c.getBiome());
            gc.setFill(baseBiomeColor);
            gc.fillOval(x - 2 * dotScale, y - 2 * dotScale, 4 * dotScale, 4 * dotScale);
        }

        // 2. Thematic Active Scalar Layers Compositing (Alpha blended)
        DisplayMode primaryScalarMode = null;
        for (DisplayMode mode : activeLayers) {
            if (mode == DisplayMode.BIOME) continue;
            if (primaryScalarMode == null && mode.getEncodingType() == DisplayMode.EncodingType.SCALAR_1D) {
                primaryScalarMode = mode;
            }

            double minVal = Double.MAX_VALUE;
            double maxVal = -Double.MAX_VALUE;
            for (H3Cell c : currentCells) {
                double val = SpatialHeatmapColorPalette.getCellValue(c, mode);
                minVal = Math.min(minVal, val);
                maxVal = Math.max(maxVal, val);
            }
            double valRange = Math.max(0.001, maxVal - (minVal < 0 ? minVal : 0.0));

            for (H3Cell c : currentCells) {
                if (c.getLatitude() == null || c.getLongitude() == null) continue;
                double val = SpatialHeatmapColorPalette.getCellValue(c, mode);
                if (val <= 0 && SpatialHeatmapColorPalette.isSparseZeroSkipped(mode)) continue;

                double x = ((c.getLongitude() - minLng) / lngRange) * (w - 16) + 8;
                double y = (1.0 - ((c.getLatitude() - minLat) / latRange)) * (h - 16) + 8;

                double ratio = Math.clamp((val - (minVal < 0 ? minVal : 0.0)) / valRange, 0.0, 1.0);
                Color heatColor = SpatialHeatmapColorPalette.getColorForDisplayMode(ratio, mode, val);
                gc.setFill(heatColor);

                double size = Math.max(3.5, 3.5 + ratio * 6.5) * dotScale;
                gc.fillOval(x - size / 2, y - size / 2, size, size);
            }
        }

        // 3. Compute Moran's I on the primary scalar layer
        if (computeMoran && lblMoranI != null) {
            if (primaryScalarMode != null) {
                double moranI = SpatialHeatmapColorPalette.computeMoranI(currentCells, primaryScalarMode);
                String descKey = moranI > 0.3 ? "heatmap.moran.clusters" : (moranI < -0.1 ? "heatmap.moran.dispersed" : "heatmap.moran.random");
                String descFallback = moranI > 0.3 ? "Clusters Concentrés (Agrégation)" : (moranI < -0.1 ? "Dispersion Spatiale" : "Répartition Aléatoire");
                String desc = I18n.getOrDefault(descKey, descFallback);
                String labelPattern = I18n.getOrDefault("heatmap.moran.label", "Indice de Moran (Autocorrélation [%s]) : %.3f (%s)");
                lblMoranI.setText(String.format(Locale.US, labelPattern, primaryScalarMode.getDisplayName(), moranI, desc));
            } else {
                lblMoranI.setText(I18n.getOrDefault("heatmap.moran_none", "Indice de Moran (Autocorrélation spatiale) : --"));
            }
        }
    }

    /**
     * Populates the categorized {@link MenuButton} with checkboxes for every {@link DisplayMode}.
     *
     * @param activeLayersMenuBtn target menu button
     * @param layerCheckBoxMap registry mapping display modes to their menu checkboxes
     * @param activeLayers currently enabled layers
     * @param onLayerToggled callback invoked when a layer checkbox changes state
     */
    public static void buildLayerMenu(
            MenuButton activeLayersMenuBtn,
            Map<DisplayMode, CheckBox> layerCheckBoxMap,
            Set<DisplayMode> activeLayers,
            BiConsumer<DisplayMode, Boolean> onLayerToggled) {
        activeLayersMenuBtn.getItems().clear();
        layerCheckBoxMap.clear();

        for (DisplayMode.Category cat : DisplayMode.Category.values()) {
            Label catHeader = new Label("─── " + cat.getCategoryName() + " ───");
            catHeader.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 8 2 8;");
            CustomMenuItem catItem = new CustomMenuItem(catHeader, false);
            activeLayersMenuBtn.getItems().add(catItem);

            for (DisplayMode dm : DisplayMode.values()) {
                if (dm.getCategory() == cat) {
                    CheckBox cb = new CheckBox(dm.getDisplayName());
                    cb.setTooltip(new Tooltip(dm.getDescription()));
                    cb.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 11px; -fx-padding: 2 8; -fx-cursor: hand;");
                    cb.setSelected(activeLayers.contains(dm));
                    cb.setOnAction(e -> onLayerToggled.accept(dm, cb.isSelected()));
                    layerCheckBoxMap.put(dm, cb);
                    CustomMenuItem mi = new CustomMenuItem(cb, false);
                    activeLayersMenuBtn.getItems().add(mi);
                }
            }
            activeLayersMenuBtn.getItems().add(new SeparatorMenuItem());
        }
    }

    /**
     * Synchronizes removable active-layer chip pills with the current {@code activeLayers} set.
     *
     * @param activeLayersChipsBox target {@link FlowPane} container for layer chips
     * @param activeLayersMenuBtn menu button whose title displays active layer count
     * @param activeLayers active display modes
     * @param layerCheckBoxMap checkbox map updated when a chip is dismissed
     * @param onChipsUpdated callback triggered after removing a layer chip
     */
    public static void updateActiveLayerChips(
            FlowPane activeLayersChipsBox,
            MenuButton activeLayersMenuBtn,
            Set<DisplayMode> activeLayers,
            Map<DisplayMode, CheckBox> layerCheckBoxMap,
            Runnable onChipsUpdated) {
        activeLayersChipsBox.getChildren().clear();
        int count = activeLayers.size();
        activeLayersMenuBtn.setText("🗺️ " + I18n.getOrDefault("heatmap.layers_btn", "Couches Actives") + " (" + count + ")");

        for (DisplayMode mode : activeLayers) {
            HBox chip = new HBox(4);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.setStyle("-fx-background-color: #0369a1; -fx-background-radius: 12; -fx-padding: 1 6 1 6;");

            Label chipLbl = new Label(mode.getDisplayName());
            chipLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: white; -fx-font-weight: bold;");

            Button removeBtn = new Button("×");
            removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #bae6fd; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 0 0 0 2; -fx-cursor: hand;");
            removeBtn.setOnAction(e -> {
                if (activeLayers.size() > 1) {
                    activeLayers.remove(mode);
                    CheckBox cb = layerCheckBoxMap.get(mode);
                    if (cb != null) cb.setSelected(false);
                    if (onChipsUpdated != null) onChipsUpdated.run();
                }
            });

            chip.getChildren().addAll(chipLbl, removeBtn);
            activeLayersChipsBox.getChildren().add(chip);
        }
    }

    /**
     * Configures the enlarged 560x280 hover magnifier popup over the compact heatmap canvas.
     *
     * @param mapCanvas primary compact canvas
     * @param hoverPopup popup window instance
     * @param popupCanvas enlarged canvas rendered inside popup
     * @param onRenderPopup callback rendering the current map onto {@code popupCanvas}
     */
    public static void setupHoverPopup(
            Canvas mapCanvas,
            Popup hoverPopup,
            Canvas popupCanvas,
            Runnable onRenderPopup) {
        VBox popupBox = new VBox(0, popupCanvas);
        popupBox.setStyle(
                "-fx-background-color: #0f172a;" +
                "-fx-border-color: #38bdf8;" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;"
        );
        hoverPopup.getContent().add(popupBox);
        hoverPopup.setAutoHide(false);

        final Timeline[] hideTimerHolder = new Timeline[1];
        Runnable cancelTimer = () -> {
            if (hideTimerHolder[0] != null) {
                hideTimerHolder[0].stop();
                hideTimerHolder[0] = null;
            }
        };
        Runnable scheduleHide = () -> {
            cancelTimer.run();
            Timeline timer = new Timeline(new KeyFrame(Duration.millis(200), ev -> hoverPopup.hide()));
            timer.setCycleCount(1);
            hideTimerHolder[0] = timer;
            timer.play();
        };

        mapCanvas.setOnMouseEntered(e -> {
            cancelTimer.run();
            Bounds bounds = mapCanvas.localToScreen(mapCanvas.getBoundsInLocal());
            if (bounds != null) {
                double popupX = bounds.getMaxX() + 8;
                double popupY = bounds.getMinY();
                Rectangle2D screen = Screen.getPrimary().getVisualBounds();
                if (popupX + 560 > screen.getMaxX()) {
                    popupX = bounds.getMinX() - 560 - 8;
                }
                hoverPopup.show(mapCanvas, popupX, popupY);
                if (onRenderPopup != null) onRenderPopup.run();
            }
        });

        mapCanvas.setOnMouseExited(e -> scheduleHide.run());
        popupCanvas.setOnMouseEntered(e -> cancelTimer.run());
        popupCanvas.setOnMouseExited(e -> scheduleHide.run());
    }
}
