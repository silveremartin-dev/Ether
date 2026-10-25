package org.ether.society.ui;

import org.ether.society.analytics.HistoryManager;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Biome;
import org.ether.society.ui.heatmap.SpatialHeatmapColorPalette;
import org.ether.society.ui.heatmap.SpatialHeatmapExporter;
import org.ether.society.ui.export.AtlasVideoExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.util.*;

/**
 * 2D & 2D+1D Spatial Dynamic Territory Atlas & Animated Replay Component for StatsPanel.

 * Features multi-layer compositing across all simulation categories (Physical, Climate, Demographics,
 * Ecology, Minerals, Society & Cliodynamics), full historical timeline scrubbing, interval range selection,
 * precision playback transport controls, variable animation speeds, and live spatial autocorrelation (Moran's I).
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

    private static final Logger logger = LoggerFactory.getLogger(SpatialHeatmapPanel.class);

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
    private Timeline popupHideTimer;

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
     * Spatial heatmap panel.
     *
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

        // 2. Multi-Layer Stack Selector (Categorized with Separators matching ControlPanel)
        activeLayersMenuBtn = new MenuButton();
        activeLayersMenuBtn.setMaxWidth(Double.MAX_VALUE);
        activeLayersMenuBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: #38bdf8; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");

        activeLayersChipsBox = new FlowPane(4, 4);
        activeLayersChipsBox.setStyle("-fx-padding: 2 0;");

        buildLayerMenu();

        VBox layerControlBox = new VBox(4, activeLayersMenuBtn, activeLayersChipsBox);

        // 3. Map Canvas (320x150)
        mapCanvas = new Canvas(320, 150);

        // 4. Spatial Autocorrelation (Moran's I)
        lblMoranI = new Label();
        lblMoranI.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        // 5. Timeline Scrubber Slider
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

        // 6. Transport Toolbar
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

        // 7. Interval / Range Selection & Video Export Bar
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

        setupPopup();
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
                    cb.setOnAction(e -> onLayerToggled(dm, cb.isSelected()));
                    layerCheckBoxMap.put(dm, cb);
                    CustomMenuItem mi = new CustomMenuItem(cb, false);
                    activeLayersMenuBtn.getItems().add(mi);
                }
            }
            activeLayersMenuBtn.getItems().add(new SeparatorMenuItem());
        }

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

    private void applyPreset(DisplayMode... modes) {
        activeLayers.clear();
        activeLayers.addAll(List.of(modes));
        for (Map.Entry<DisplayMode, CheckBox> entry : layerCheckBoxMap.entrySet()) {
            entry.getValue().setSelected(activeLayers.contains(entry.getKey()));
        }
        updateActiveLayerChips();
        renderMap();
    }

    private void updateActiveLayerChips() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
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
                    updateActiveLayerChips();
                    renderMap();
                }
            });

            chip.getChildren().addAll(chipLbl, removeBtn);
            activeLayersChipsBox.getChildren().add(chip);
        }
    }

    private void setupPopup() {
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

        mapCanvas.setOnMouseEntered(e -> {
            cancelHideTimer();
            javafx.geometry.Bounds bounds = mapCanvas.localToScreen(mapCanvas.getBoundsInLocal());
            if (bounds != null) {
                double popupX = bounds.getMaxX() + 8;
                double popupY = bounds.getMinY();
                javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
                if (popupX + 560 > screen.getMaxX()) {
                    popupX = bounds.getMinX() - 560 - 8;
                }
                hoverPopup.show(mapCanvas, popupX, popupY);
                renderMapOnCanvas(popupCanvas);
            }
        });

        mapCanvas.setOnMouseExited(e -> scheduleHidePopup());
        popupCanvas.setOnMouseEntered(e -> cancelHideTimer());
        popupCanvas.setOnMouseExited(e -> scheduleHidePopup());
    }

    private void scheduleHidePopup() {
        cancelHideTimer();
        popupHideTimer = new Timeline(new KeyFrame(Duration.millis(200), ev -> hoverPopup.hide()));
        popupHideTimer.setCycleCount(1);
        popupHideTimer.play();
    }

    private void cancelHideTimer() {
        if (popupHideTimer != null) {
            popupHideTimer.stop();
            popupHideTimer = null;
        }
    }

    /**
     * Update texts.
     *
     */
    public void updateTexts() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
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
     * Set history manager.
     *
     * @param historyManager the history manager parameter (HistoryManager)
     */
    public void setHistoryManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
        updateSliderRange();
    }

    /**
     * Update cells.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     */
    public void updateCells(List<H3Cell> cells) {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (!isPlayingAnimation && cells != null) {
            this.currentCells = cells;
            updateSliderRange();
            renderMap();
        }
    }

    /**
     * Set date label.
     *
     * @param text the text parameter (String)
     */
    public void setDateLabel(String text) {
        if (dateLabel != null) {
            dateLabel.setText(text != null ? text : "");
        }
    }

    private void updateSliderRange() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
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
    }

    private void exportVideo() {
        if (historyManager == null || historyManager.getWorldSnapshots().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(I18n.getOrDefault("heatmap.export.warn_title", "Aucune Donnée Historique"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.getOrDefault("heatmap.export.no_snapshots", "Aucun instantané n'est disponible pour l'exportation vidéo. Lancez la simulation d'abord."));
            alert.showAndWait();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("heatmap.export.dialog_title", "Exporter l'Atlas Dynamique en Vidéo / GIF Animé"));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GIF Vidéo Animée (*.gif)", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les Fichiers (*.*)", "*.*")
        );
        chooser.setInitialFileName("Atlas_Territoires_" + System.currentTimeMillis() + ".gif");
        File targetFile = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (targetFile == null) return;

        btnExportVideo.setDisable(true);
        btnExportVideo.setText("⏳ " + I18n.getOrDefault("heatmap.export.exporting", "Export en cours..."));

        int startIdx = getEffectiveStartIndex();
        int endIdx = getEffectiveEndIndex();

        AtlasVideoExporter.exportVideoAsync(
                historyManager,
                activeLayers,
                startIdx,
                endIdx,
                playbackDelayMs,
                targetFile,
                "Atlas",
                progress -> {
                    // background progress callback
                }
        ).thenAccept(outFile -> javafx.application.Platform.runLater(() -> {
            btnExportVideo.setDisable(false);
            btnExportVideo.setText("🎥 " + I18n.getOrDefault("heatmap.btn.export_video", "Exporter Vidéo"));

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(I18n.getOrDefault("heatmap.export.success_title", "Export Vidéo Réussi"));
            alert.setHeaderText(I18n.getOrDefault("heatmap.export.success_header", "Atlas Vidéo Exporté avec Succès"));
            alert.setContentText(String.format(Locale.ROOT, I18n.getOrDefault("heatmap.export.success_body", "L'animation cartographique a été générée dans :\n%s\n(%d instantanés encodés)"),
                    outFile.getAbsolutePath(), (endIdx - startIdx + 1)));

            ButtonType openFileBtn = new ButtonType(I18n.getOrDefault("heatmap.export.open_file", "Ouvrir le Fichier"), ButtonBar.ButtonData.YES);
            ButtonType closeBtn = new ButtonType(I18n.getOrDefault("heatmap.export.close", "Fermer"), ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(openFileBtn, closeBtn);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == openFileBtn) {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(outFile);
                    }
                } catch (Exception ex) {
                    logger.warn("Could not open exported video file directly: {}", ex.getMessage());
                }
            }
        })).exceptionally(ex -> {
            javafx.application.Platform.runLater(() -> {
                btnExportVideo.setDisable(false);
                btnExportVideo.setText("🎥 " + I18n.getOrDefault("heatmap.btn.export_video", "Exporter Vidéo"));

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18n.getOrDefault("heatmap.export.error_title", "Erreur d'Exportation"));
                alert.setHeaderText(null);
                alert.setContentText(I18n.getOrDefault("heatmap.export.error_body", "Impossible d'exporter l'atlas vidéo : ") + ex.getMessage());
                alert.showAndWait();
            });
            return null;
        });
    }

    private void stepSnapshot(int delta) {
        int target = currentSnapshotIndex + delta;
        int minIdx = getEffectiveStartIndex();
        int maxIdx = getEffectiveEndIndex();
        if (target < minIdx) target = maxIdx;
        if (target > maxIdx) target = minIdx;
        jumpToSnapshotIndex(target, true);
    }

    private int getEffectiveStartIndex() {
        return (rangeStartIndex != null) ? Math.max(0, rangeStartIndex) : 0;
    }

    private int getEffectiveEndIndex() {
        if (historyManager == null || historyManager.getWorldSnapshots().isEmpty()) return 0;
        int max = historyManager.getWorldSnapshots().size() - 1;
        return (rangeEndIndex != null) ? Math.min(max, rangeEndIndex) : max;
    }

    private void setRangeStart(int index) {
        this.rangeStartIndex = index;
        if (rangeEndIndex != null && rangeEndIndex < rangeStartIndex) {
            rangeEndIndex = rangeStartIndex;
        }
        updateRangeLabel();
    }

    private void setRangeEnd(int index) {
        this.rangeEndIndex = index;
        if (rangeStartIndex != null && rangeStartIndex > rangeEndIndex) {
            rangeStartIndex = rangeEndIndex;
        }
        updateRangeLabel();
    }

    private void clearRange() {
        this.rangeStartIndex = null;
        this.rangeEndIndex = null;
        updateRangeLabel();
    }

    private void updateRangeLabel() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (rangeStartIndex == null && rangeEndIndex == null) {
            rangeInfoLabel.setText(I18n.getOrDefault("heatmap.range.all", "Plage : Tout l'historique"));
        } else {
            int start = getEffectiveStartIndex() + 1;
            int end = getEffectiveEndIndex() + 1;
            rangeInfoLabel.setText(String.format(Locale.ROOT, "Plage [%d ➔ %d]", start, end));
        }
    }

    private void updatePlaybackSpeed() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        String val = speedCombo.getValue();
        if ("0.5x".equals(val)) playbackDelayMs = 600;
        else if ("2.0x".equals(val)) playbackDelayMs = 150;
        else if ("5.0x".equals(val)) playbackDelayMs = 60;
        else if ("10.0x".equals(val)) playbackDelayMs = 30;
        else playbackDelayMs = 300;

        if (isPlayingAnimation) {
            animationTimeline.stop();
            setupAnimation();
            animationTimeline.play();
        }
    }

    private void renderMap() {
        renderMapOnCanvas(mapCanvas);
        if (hoverPopup.isShowing()) {
            renderMapOnCanvas(popupCanvas);
        }
    }

    private void renderMapOnCanvas(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.rgb(15, 23, 42, 0.95));
        gc.fillRoundRect(0, 0, w, h, 8, 8);

        if (currentCells == null || currentCells.isEmpty()) {
            gc.setFill(Color.web("#64748b"));
            gc.fillText(I18n.getOrDefault("heatmap.no_data", "Aucune donnée cartographique"), w / 4, h / 2);
            lblMoranI.setText(I18n.getOrDefault("heatmap.moran_none", "Indice de Moran (Autocorrélation spatiale) : --"));
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

        // 1. Base Terrain / Biome geography background (if BIOME active or as base backdrop)
        for (H3Cell c : currentCells) {
            if (c.getLatitude() == null || c.getLongitude() == null) continue;
            double x = ((c.getLongitude() - minLng) / lngRange) * (w - 16) + 8;
            double y = (1.0 - ((c.getLatitude() - minLat) / latRange)) * (h - 16) + 8;

            Color baseBiomeColor = getBiomeBaseColor(c.getBiome());
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
            // Iterate over spatial cell domains and apply localized cellular state transformations
            for (H3Cell c : currentCells) {
                double val = getCellValue(c, mode);
                minVal = Math.min(minVal, val);
                maxVal = Math.max(maxVal, val);
            }
            double valRange = Math.max(0.001, maxVal - (minVal < 0 ? minVal : 0.0));

            // Iterate over spatial cell domains and apply localized cellular state transformations
            for (H3Cell c : currentCells) {
                if (c.getLatitude() == null || c.getLongitude() == null) continue;
                double val = getCellValue(c, mode);
                if (val <= 0 && isSparseZeroSkipped(mode)) continue;

                double x = ((c.getLongitude() - minLng) / lngRange) * (w - 16) + 8;
                double y = (1.0 - ((c.getLatitude() - minLat) / latRange)) * (h - 16) + 8;

                double ratio = Math.clamp((val - (minVal < 0 ? minVal : 0.0)) / valRange, 0.0, 1.0);
                Color heatColor = getColorForDisplayMode(ratio, mode, val);
                gc.setFill(heatColor);

                double size = Math.max(3.5, 3.5 + ratio * 6.5) * dotScale;
                gc.fillOval(x - size / 2, y - size / 2, size, size);
            }
        }

        // 3. Compute Moran's I on the primary scalar layer
        if (canvas == mapCanvas && primaryScalarMode != null) {
            double moranI = computeMoranI(primaryScalarMode);
            String descKey = moranI > 0.3 ? "heatmap.moran.clusters" : (moranI < -0.1 ? "heatmap.moran.dispersed" : "heatmap.moran.random");
            String descFallback = moranI > 0.3 ? "Clusters Concentrés (Agrégation)" : (moranI < -0.1 ? "Dispersion Spatiale" : "Répartition Aléatoire");
            String desc = I18n.getOrDefault(descKey, descFallback);
            String labelPattern = I18n.getOrDefault("heatmap.moran.label", "Indice de Moran (Autocorrélation [%s]) : %.3f (%s)");
            lblMoranI.setText(String.format(Locale.US, labelPattern, primaryScalarMode.getDisplayName(), moranI, desc));
        } else if (canvas == mapCanvas) {
            lblMoranI.setText(I18n.getOrDefault("heatmap.moran_none", "Indice de Moran (Autocorrélation spatiale) : --"));
        }
    }

    private boolean isSparseZeroSkipped(DisplayMode mode) {
        return SpatialHeatmapColorPalette.isSparseZeroSkipped(mode);
    }

    private Color getBiomeBaseColor(Biome biome) {
        return SpatialHeatmapColorPalette.getBiomeBaseColor(biome);
    }

    private double getCellValue(H3Cell c, DisplayMode mode) {
        return SpatialHeatmapColorPalette.getCellValue(c, mode);
    }

    private Color getColorForDisplayMode(double ratio, DisplayMode mode, double rawVal) {
        return SpatialHeatmapColorPalette.getColorForDisplayMode(ratio, mode, rawVal);
    }

    private double computeMoranI(DisplayMode mode) {
        return SpatialHeatmapColorPalette.computeMoranI(currentCells, mode);
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
                    toggleAnimation(); // Pause at end of range
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
