/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;
import org.ether.society.analytics.HistoryManager;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.heatmap.SpatialHeatmapCanvasRenderer;
import org.ether.society.ui.heatmap.SpatialHeatmapExporter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 2D &amp; 2D+1D Spatial Dynamic Territory Atlas &amp; Animated Replay Component for {@link StatsPanel}.
 *
 * <p>Features multi-layer compositing across all simulation categories (Physical, Climate, Demographics,
 * Ecology, Minerals, Society &amp; Cliodynamics), full historical timeline scrubbing, interval range selection,
 * precision playback transport controls, variable animation speeds, and live spatial autocorrelation
 * (Moran's \(I\)), delegating canvas rendering to {@link SpatialHeatmapCanvasRenderer} and video export
 * to {@link SpatialHeatmapExporter}.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 */
public class SpatialHeatmapPanel extends VBox {

    private final Label titleLabel;
    private final Label dateLabel;
    private final Canvas mapCanvas;
    private final Label lblMoranI;

    // Multi-Layer Overlay Controls
    private final Set<DisplayMode> activeLayers = new LinkedHashSet<>(List.of(DisplayMode.BIOME, DisplayMode.POPULATION));
    private final MenuButton activeLayersMenuBtn;
    private final Map<DisplayMode, CheckBox> layerCheckBoxMap = new EnumMap<>(DisplayMode.class);
    private final FlowPane activeLayersChipsBox;

    // Timeline Scrubbing & Transport Controls
    private final Slider timeSlider;
    private final Label timePositionLabel;
    private final Button btnJumpStart;
    private final Button btnStepBack;
    private final Button playPauseBtn;
    private final Button btnStepForward;
    private final Button btnJumpEnd;
    private final ComboBox<String> speedCombo;
    private final CheckBox loopCheckBox;

    // Range / Interval Selection Controls & Video Export
    private final Button btnSetStart;
    private final Button btnSetEnd;
    private final Button btnClearRange;
    private final Button btnExportVideo;
    private final Label rangeInfoLabel;
    private Integer rangeStartIndex = null;
    private Integer rangeEndIndex = null;

    // Hover popup – enlarged map (560×280)
    private final Popup hoverPopup = new Popup();
    private final Canvas popupCanvas = new Canvas(560, 280);

    private List<H3Cell> currentCells = new ArrayList<>();
    private HistoryManager historyManager;
    private Timeline animationTimeline;
    /** Internal state: is playing animation (boolean). */
    private boolean isPlayingAnimation = false;
    /** Internal state: current snapshot index (int). */
    private int currentSnapshotIndex = 0;
    /** Internal state: playback delay ms (int). */
    private int playbackDelayMs = 300;
    /** Internal state: is scrubbing (boolean). */
    private boolean isScrubbing = false;

    /**
     * Constructs the spatial heatmap panel and initializes transport controls and layer selectors.
     */
    public SpatialHeatmapPanel() {
        setPadding(new Insets(10));
        setSpacing(8);
        getStyleClass().add("card-section");

        // 1. Header with Title & Date Badge
        titleLabel = new Label();
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        dateLabel = new Label("");
        dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        HBox titleBox = new HBox(8, titleLabel, dateLabel);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // 2. Multi-Layer Stack Selector
        activeLayersMenuBtn = new MenuButton();
        activeLayersMenuBtn.setMaxWidth(Double.MAX_VALUE);
        activeLayersMenuBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: #38bdf8; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");

        activeLayersChipsBox = new FlowPane(4, 4);
        activeLayersChipsBox.setStyle("-fx-padding: 2 0;");

        buildLayerMenu();
        VBox layerControlBox = new VBox(4, activeLayersMenuBtn, activeLayersChipsBox);

        // 3. Map Canvas (320x150) & Moran's I
        mapCanvas = new Canvas(320, 150);
        lblMoranI = new Label();
        lblMoranI.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        // 4. Timeline Scrubber Slider
        timeSlider = new Slider(0, 0, 0);
        timeSlider.setMaxWidth(Double.MAX_VALUE);
        timeSlider.setShowTickMarks(true);
        timeSlider.setShowTickLabels(false);
        timeSlider.setMajorTickUnit(10);
        timeSlider.setBlockIncrement(1);
        timeSlider.setStyle("-fx-cursor: hand;");
        timeSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (isScrubbing) {
                jumpToSnapshotIndex(newV.intValue(), false);
            }
        });
        timeSlider.setOnMousePressed(e -> isScrubbing = true);
        timeSlider.setOnMouseReleased(e -> {
            isScrubbing = false;
            jumpToSnapshotIndex((int) Math.round(timeSlider.getValue()), true);
        });

        timePositionLabel = new Label("--");
        timePositionLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        HBox sliderBox = new HBox(8, timeSlider, timePositionLabel);
        HBox.setHgrow(timeSlider, Priority.ALWAYS);
        sliderBox.setAlignment(Pos.CENTER_LEFT);

        // 5. Transport Toolbar
        btnJumpStart = createTransportBtn("⏮", "heatmap.tooltip.jump_start", "Aller au premier instant (ou début de plage)");
        btnJumpStart.setOnAction(e -> {
            pauseAnimation();
            jumpToSnapshotIndex(getEffectiveStartIndex(), true);
        });

        btnStepBack = createTransportBtn("⏴", "heatmap.tooltip.step_back", "Reculer d'un instant");
        btnStepBack.setOnAction(e -> {
            pauseAnimation();
            stepSnapshot(-1);
        });

        playPauseBtn = new Button("▶");
        playPauseBtn.setMinSize(36, 28);
        playPauseBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 6; -fx-cursor: hand;");
        playPauseBtn.setOnAction(e -> toggleAnimation());

        btnStepForward = createTransportBtn("⏵", "heatmap.tooltip.step_forward", "Avancer d'un instant");
        btnStepForward.setOnAction(e -> {
            pauseAnimation();
            stepSnapshot(1);
        });

        btnJumpEnd = createTransportBtn("⏭", "heatmap.tooltip.jump_end", "Aller au dernier instant (ou fin de plage)");
        btnJumpEnd.setOnAction(e -> {
            pauseAnimation();
            jumpToSnapshotIndex(getEffectiveEndIndex(), true);
        });

        speedCombo = new ComboBox<>();
        speedCombo.getItems().addAll("0.5x", "1.0x", "2.0x", "5.0x", "10.0x");
        speedCombo.setValue("1.0x");
        speedCombo.setStyle("-fx-font-size: 10px; -fx-background-radius: 4;");
        speedCombo.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.speed", "Vitesse de relecture temporelle")));
        speedCombo.setOnAction(e -> updatePlaybackSpeed());

        loopCheckBox = new CheckBox(I18n.getOrDefault("heatmap.loop", "🔁"));
        loopCheckBox.setSelected(true);
        loopCheckBox.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        loopCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.loop", "Relecture en boucle")));

        HBox transportBar = new HBox(4, btnJumpStart, btnStepBack, playPauseBtn, btnStepForward, btnJumpEnd, speedCombo, loopCheckBox);
        transportBar.setAlignment(Pos.CENTER_LEFT);

        // 6. Interval / Range Selection & Video Export Bar
        btnSetStart = createMiniRangeBtn("📌 Début A", () -> setRangeStart(currentSnapshotIndex));
        btnSetStart.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.set_start", "Définir l'instant actuel comme borne de départ (A)")));

        btnSetEnd = createMiniRangeBtn("📌 Fin B", () -> setRangeEnd(currentSnapshotIndex));
        btnSetEnd.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.set_end", "Définir l'instant actuel comme borne de fin (B)")));

        btnClearRange = createMiniRangeBtn("✖ Bornes", this::clearRange);
        btnClearRange.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.clear_range", "Réinitialiser les bornes pour rejouer tout l'historique")));

        btnExportVideo = new Button("🎥 " + I18n.getOrDefault("heatmap.btn.export_video", "Export"));
        btnExportVideo.setStyle("-fx-background-color: #0369a1; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
        btnExportVideo.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.export_video", "Exporte l'atlas dynamique et ses couches multicouches au format vidéo / GIF animé haute fidélité.")));
        btnExportVideo.setOnAction(e -> exportVideo());

        rangeInfoLabel = new Label(I18n.getOrDefault("heatmap.range.all", "Plage : Tout l'historique"));
        rangeInfoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        rangeInfoLabel.setWrapText(true);

        FlowPane rangeBtnsBox = new FlowPane(4, 4, btnSetStart, btnSetEnd, btnClearRange, btnExportVideo);
        VBox rangeBar = new VBox(4, rangeBtnsBox, rangeInfoLabel);

        getChildren().addAll(titleBox, layerControlBox, mapCanvas, lblMoranI, sliderBox, transportBar, rangeBar);

        SpatialHeatmapCanvasRenderer.setupHoverPopup(
                mapCanvas, hoverPopup, popupCanvas,
                () -> SpatialHeatmapCanvasRenderer.renderMapOnCanvas(popupCanvas, currentCells, activeLayers, lblMoranI, false)
        );
        setupAnimation();
        updateTexts();
        I18n.languageProperty().addListener((obs, oldL, newL) -> updateTexts());
    }

    private Button createTransportBtn(String text, String tooltipKey, String fallbackTooltip) {
        Button btn = new Button(text);
        btn.setMinSize(28, 26);
        btn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
        btn.setTooltip(new Tooltip(I18n.getOrDefault(tooltipKey, fallbackTooltip)));
        return btn;
    }

    private Button createMiniRangeBtn(String label, Runnable action) {
        Button btn = new Button(label);
        btn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 6; -fx-background-radius: 4; -fx-cursor: hand; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 4;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void buildLayerMenu() {
        SpatialHeatmapCanvasRenderer.buildLayerMenu(activeLayersMenuBtn, layerCheckBoxMap, activeLayers, this::onLayerToggled);
        updateActiveLayerChips();
    }

    private void onLayerToggled(DisplayMode dm, boolean selected) {
        if (selected) {
            activeLayers.add(dm);
        } else {
            activeLayers.remove(dm);
            if (activeLayers.isEmpty()) {
                activeLayers.add(DisplayMode.BIOME);
                CheckBox cb = layerCheckBoxMap.get(DisplayMode.BIOME);
                if (cb != null) cb.setSelected(true);
            }
        }
        updateActiveLayerChips();
        renderMap();
    }

    private void updateActiveLayerChips() {
        SpatialHeatmapCanvasRenderer.updateActiveLayerChips(
                activeLayersChipsBox, activeLayersMenuBtn, activeLayers, layerCheckBoxMap,
                () -> {
                    updateActiveLayerChips();
                    renderMap();
                }
        );
    }

    /**
     * Updates all localized UI texts and layer menus.
     */
    public void updateTexts() {
        titleLabel.setText(I18n.getOrDefault("heatmap.title", "🗺️ Atlas Dynamique des Territoires (Toutes Couches)"));
        buildLayerMenu();

        if (isPlayingAnimation) {
            playPauseBtn.setText("⏸");
            playPauseBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 6;");
            playPauseBtn.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.pause", "Mettre en pause")));
        } else {
            playPauseBtn.setText("▶");
            playPauseBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 6;");
            playPauseBtn.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.play", "Lancer la séquence temporelle (2D+1D)")));
        }

        btnExportVideo.setText("🎥 " + I18n.getOrDefault("heatmap.btn.export_video", "Export"));
        btnExportVideo.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.export_video", "Exporte l'atlas dynamique et ses couches multicouches au format vidéo / GIF animé haute fidélité.")));

        updateRangeLabel();
    }

    /**
     * Binds the simulation history manager for temporal snapshot scrubbing.
     *
     * @param historyManager simulation history manager
     */
    public void setHistoryManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
        updateSliderRange();
    }

    /**
     * Updates displayed cells when interactive replay animation is paused.
     *
     * @param cells active H3 spatial cells
     */
    public void updateCells(List<H3Cell> cells) {
        if (!isPlayingAnimation && cells != null) {
            this.currentCells = cells;
            updateSliderRange();
            renderMap();
        }
    }

    /**
     * Updates the current calendar year/date badge text.
     *
     * @param text formatted date label
     */
    public void setDateLabel(String text) {
        if (dateLabel != null) {
            dateLabel.setText(text != null ? text : "");
        }
    }

    private void updateSliderRange() {
        if (historyManager == null || historyManager.getWorldSnapshots().isEmpty()) {
            timeSlider.setMin(0);
            timeSlider.setMax(0);
            timeSlider.setValue(0);
            timePositionLabel.setText("0 / 0");
            return;
        }

        int count = historyManager.getWorldSnapshots().size();
        timeSlider.setMin(0);
        timeSlider.setMax(Math.max(0, count - 1));
        if (!isScrubbing && !isPlayingAnimation) {
            timeSlider.setValue(currentSnapshotIndex);
        }
        timePositionLabel.setText(String.format(Locale.ROOT, "%d / %d", currentSnapshotIndex + 1, count));
    }

    private void jumpToSnapshotIndex(int index, boolean render) {
        if (historyManager == null || historyManager.getWorldSnapshots().isEmpty()) return;
        List<Long> keys = new ArrayList<>(historyManager.getWorldSnapshots().keySet());
        if (keys.isEmpty()) return;

        currentSnapshotIndex = Math.clamp(index, 0, keys.size() - 1);
        long stepKey = keys.get(currentSnapshotIndex);
        this.currentCells = historyManager.getWorldSnapshots().get(stepKey);

        if (!isScrubbing) {
            timeSlider.setValue(currentSnapshotIndex);
        }
        timePositionLabel.setText(String.format(Locale.ROOT, "%d / %d", currentSnapshotIndex + 1, keys.size()));

        if (dateLabel != null) {
            dateLabel.setText(String.format(Locale.ROOT, "Pas : %,d (%d/%d)", stepKey, currentSnapshotIndex + 1, keys.size()));
        }

        if (render) {
            renderMap();
        }

        if (historyManager != null && historyManager.getHistory() != null && currentSnapshotIndex < historyManager.getHistory().getSnapshots().size()) {
            int snapYear = historyManager.getHistory().getSnapshots().get(currentSnapshotIndex).year();
            if (onSnapshotYearChanged != null) {
                onSnapshotYearChanged.accept(snapYear);
            }
        }
    }

    private Consumer<Integer> onSnapshotYearChanged;

    /**
     * Registers a listener notified when the chronological snapshot scrubbing changes year.
     *
     * @param listener year consumer
     */
    public void setOnSnapshotYearChanged(Consumer<Integer> listener) {
        this.onSnapshotYearChanged = listener;
    }

    private void exportVideo() {
        SpatialHeatmapExporter.exportVideo(
                getScene() != null ? getScene().getWindow() : null,
                historyManager,
                activeLayers,
                getEffectiveStartIndex(),
                getEffectiveEndIndex(),
                playbackDelayMs,
                () -> {
                    btnExportVideo.setDisable(true);
                    btnExportVideo.setText("⏳ " + I18n.getOrDefault("heatmap.export.exporting", "Export en cours..."));
                },
                () -> {
                    btnExportVideo.setDisable(false);
                    btnExportVideo.setText("🎥 " + I18n.getOrDefault("heatmap.btn.export_video", "Exporter Vidéo"));
                }
        );
    }

    private void stepSnapshot(int delta) {
        int target = currentSnapshotIndex + delta;
        int minIdx = getEffectiveStartIndex();
        int maxIdx = getEffectiveEndIndex();
        if (target < minIdx) target = maxIdx;
        if (target > maxIdx) target = minIdx;
        jumpToSnapshotIndex(target, true);
    }

    private int getEffectiveStartIndex() { return (rangeStartIndex != null) ? Math.max(0, rangeStartIndex) : 0; }
    private int getEffectiveEndIndex() {
        if (historyManager == null || historyManager.getWorldSnapshots().isEmpty()) return 0;
        int max = historyManager.getWorldSnapshots().size() - 1;
        return (rangeEndIndex != null) ? Math.min(max, rangeEndIndex) : max;
    }
    private void setRangeStart(int index) { this.rangeStartIndex = index; if (rangeEndIndex != null && rangeEndIndex < rangeStartIndex) rangeEndIndex = rangeStartIndex; updateRangeLabel(); }
    private void setRangeEnd(int index) { this.rangeEndIndex = index; if (rangeStartIndex != null && rangeStartIndex > rangeEndIndex) rangeStartIndex = rangeEndIndex; updateRangeLabel(); }
    private void clearRange() { this.rangeStartIndex = null; this.rangeEndIndex = null; updateRangeLabel(); }

    private void updateRangeLabel() {
        if (rangeStartIndex == null && rangeEndIndex == null) {
            rangeInfoLabel.setText(I18n.getOrDefault("heatmap.range.all", "Plage : Tout l'historique"));
        } else {
            int start = getEffectiveStartIndex() + 1;
            int end = getEffectiveEndIndex() + 1;
            rangeInfoLabel.setText(String.format(Locale.ROOT, "Plage [%d ➔ %d]", start, end));
        }
    }

    private void updatePlaybackSpeed() {
        String val = speedCombo.getValue();
        playbackDelayMs = switch (val != null ? val : "") {
            case "0.5x" -> 600; case "2.0x" -> 150; case "5.0x" -> 60; case "10.0x" -> 30; default -> 300;
        };
        if (isPlayingAnimation) { animationTimeline.stop(); setupAnimation(); animationTimeline.play(); }
    }

    private void renderMap() {
        SpatialHeatmapCanvasRenderer.renderMapOnCanvas(mapCanvas, currentCells, activeLayers, lblMoranI, true);
        if (hoverPopup.isShowing()) {
            SpatialHeatmapCanvasRenderer.renderMapOnCanvas(popupCanvas, currentCells, activeLayers, lblMoranI, false);
        }
    }

    private void setupAnimation() {
        animationTimeline = new Timeline(new KeyFrame(Duration.millis(playbackDelayMs), e -> {
            if (historyManager == null || historyManager.getWorldSnapshots().isEmpty()) return;
            int startIdx = getEffectiveStartIndex();
            int endIdx = getEffectiveEndIndex();

            if (currentSnapshotIndex < startIdx || currentSnapshotIndex >= endIdx) {
                if (loopCheckBox.isSelected()) {
                    currentSnapshotIndex = startIdx;
                } else {
                    toggleAnimation();
                    return;
                }
            } else {
                currentSnapshotIndex++;
            }

            jumpToSnapshotIndex(currentSnapshotIndex, true);
        }));
        animationTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void pauseAnimation() {
        if (isPlayingAnimation) {
            animationTimeline.pause();
            isPlayingAnimation = false;
            playPauseBtn.setText("▶");
            playPauseBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 6;");
            playPauseBtn.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.play", "Lancer la séquence temporelle (2D+1D)")));
        }
    }

    private void toggleAnimation() {
        if (isPlayingAnimation) {
            pauseAnimation();
        } else {
            if (historyManager != null && !historyManager.getWorldSnapshots().isEmpty()) {
                if (currentSnapshotIndex >= getEffectiveEndIndex()) {
                    currentSnapshotIndex = getEffectiveStartIndex();
                }
                animationTimeline.play();
                isPlayingAnimation = true;
                playPauseBtn.setText("⏸");
                playPauseBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 6;");
                playPauseBtn.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.pause", "Mettre en pause")));
            } else {
                lblMoranI.setText(I18n.getOrDefault("heatmap.no_snapshot", "Veuillez lancer la simulation pour animer la séquence temporelle."));
            }
        }
    }
}
