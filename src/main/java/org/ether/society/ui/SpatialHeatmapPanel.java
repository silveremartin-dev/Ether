package org.ether.society.ui;

import org.ether.society.analytics.HistoryManager;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Biome;
import org.ether.society.persistence.AtlasVideoExporter;
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

    // Preset Buttons
    private final Button presetSynthBtn;
    private final Button presetDemoBtn;
    private final Button presetClimBtn;
    private final Button presetEconBtn;

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
    private boolean isPlayingAnimation = false;
    private int currentSnapshotIndex = 0;
    private int playbackDelayMs = 300;
    private boolean isScrubbing = false;

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

        // 1-Click Scientific Presets
        presetSynthBtn = createMiniPresetBtn("🌍 Synthèse", () -> applyPreset(DisplayMode.BIOME, DisplayMode.POPULATION));
        presetDemoBtn = createMiniPresetBtn("👥 Démographie", () -> applyPreset(DisplayMode.BIOME, DisplayMode.POPULATION, DisplayMode.MIGRATION));
        presetClimBtn = createMiniPresetBtn("🌡️ Climat", () -> applyPreset(DisplayMode.BIOME, DisplayMode.TEMPERATURE, DisplayMode.WATER));
        presetEconBtn = createMiniPresetBtn("💰 Économie", () -> applyPreset(DisplayMode.BIOME, DisplayMode.GDP_WEALTH, DisplayMode.FLUX, DisplayMode.MINERAL_RESOURCES));

        HBox presetsBox = new HBox(4, presetSynthBtn, presetDemoBtn, presetClimBtn, presetEconBtn);
        presetsBox.setAlignment(Pos.CENTER_LEFT);

        VBox layerControlBox = new VBox(4, activeLayersMenuBtn, activeLayersChipsBox, presetsBox);

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
        btnJumpStart.setOnAction(e -> jumpToSnapshotIndex(getEffectiveStartIndex(), true));

        btnStepBack = createTransportBtn("⏴", "heatmap.tooltip.step_back", "Reculer d'un instant");
        btnStepBack.setOnAction(e -> stepSnapshot(-1));

        playPauseBtn = new Button("▶");
        playPauseBtn.setMinSize(36, 28);
        playPauseBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 6; -fx-cursor: hand;");
        playPauseBtn.setOnAction(e -> toggleAnimation());

        btnStepForward = createTransportBtn("⏵", "heatmap.tooltip.step_forward", "Avancer d'un instant");
        btnStepForward.setOnAction(e -> stepSnapshot(1));

        btnJumpEnd = createTransportBtn("⏭", "heatmap.tooltip.jump_end", "Aller au dernier instant (ou fin de plage)");
        btnJumpEnd.setOnAction(e -> jumpToSnapshotIndex(getEffectiveEndIndex(), true));

        speedCombo = new ComboBox<>();
        speedCombo.getItems().addAll("0.5x", "1.0x", "2.0x", "5.0x", "10.0x");
        speedCombo.setValue("1.0x");
        speedCombo.setStyle("-fx-font-size: 10px; -fx-background-radius: 4;");
        speedCombo.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.speed", "Vitesse de relecture temporelle")));
        speedCombo.setOnAction(e -> updatePlaybackSpeed());

        loopCheckBox = new CheckBox(I18n.getOrDefault("heatmap.loop", "🔁 Boucle"));
        loopCheckBox.setSelected(true);
        loopCheckBox.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        HBox transportBar = new HBox(6, btnJumpStart, btnStepBack, playPauseBtn, btnStepForward, btnJumpEnd, speedCombo, loopCheckBox);
        transportBar.setAlignment(Pos.CENTER_LEFT);

        // 7. Interval / Range Selection & Video Export Bar
        btnSetStart = createMiniRangeBtn("📌 Début A", () -> setRangeStart(currentSnapshotIndex));
        btnSetEnd = createMiniRangeBtn("📌 Fin B", () -> setRangeEnd(currentSnapshotIndex));
        btnClearRange = createMiniRangeBtn("✖ Réinitialiser Bornes", this::clearRange);

        btnExportVideo = new Button("🎥 " + I18n.getOrDefault("heatmap.btn.export_video", "Exporter Vidéo"));
        btnExportVideo.setStyle("-fx-background-color: #0369a1; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 4; -fx-cursor: hand;");
        btnExportVideo.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.export_video", "Exporte l'atlas dynamique et ses couches multicouches au format vidéo / GIF animé haute fidélité.")));
        btnExportVideo.setOnAction(e -> exportVideo());

        rangeInfoLabel = new Label(I18n.getOrDefault("heatmap.range.all", "Plage : Tout l'historique"));
        rangeInfoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        HBox rangeBar = new HBox(6, btnSetStart, btnSetEnd, btnClearRange, btnExportVideo, rangeInfoLabel);
        rangeBar.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(titleBox, layerControlBox, mapCanvas, lblMoranI, sliderBox, transportBar, rangeBar);

        setupPopup();
        setupAnimation();
        updateTexts();
        I18n.languageProperty().addListener((obs, oldL, newL) -> updateTexts());
    }

    private Button createMiniPresetBtn(String label, Runnable action) {
        Button btn = new Button(label);
        btn.setStyle("-fx-background-color: #334155; -fx-text-fill: #e2e8f0; -fx-font-size: 9px; -fx-padding: 2 6; -fx-background-radius: 4; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private Button createTransportBtn(String text, String tooltipKey, String fallbackTooltip) {
        Button btn = new Button(text);
        btn.setMinSize(30, 28);
        btn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 6; -fx-cursor: hand;");
        btn.setTooltip(new Tooltip(I18n.getOrDefault(tooltipKey, fallbackTooltip)));
        return btn;
    }

    private Button createMiniRangeBtn(String label, Runnable action) {
        Button btn = new Button(label);
        btn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-font-size: 9px; -fx-padding: 2 6; -fx-background-radius: 4; -fx-cursor: hand;");
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

        btnExportVideo.setText("🎥 " + I18n.getOrDefault("heatmap.btn.export_video", "Exporter Vidéo"));
        btnExportVideo.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.export_video", "Exporte l'atlas dynamique et ses couches multicouches au format vidéo / GIF animé haute fidélité.")));

        updateRangeLabel();
        renderMap();
    }

    public void setHistoryManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
        updateSliderRange();
    }

    public void updateCells(List<H3Cell> cells) {
        if (!isPlayingAnimation && cells != null) {
            this.currentCells = cells;
            updateSliderRange();
            renderMap();
        }
    }

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
            for (H3Cell c : currentCells) {
                double val = getCellValue(c, mode);
                minVal = Math.min(minVal, val);
                maxVal = Math.max(maxVal, val);
            }
            double valRange = Math.max(0.001, maxVal - (minVal < 0 ? minVal : 0.0));

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
        return switch (mode) {
            case POPULATION, FLUX, MIGRATION, CONFLICT, EPIDEMIC -> true;
            default -> false;
        };
    }

    private Color getBiomeBaseColor(Biome biome) {
        if (biome == null) return Color.rgb(30, 41, 59, 0.6);
        return switch (biome) {
            case OCEAN -> Color.rgb(14, 45, 80, 0.7);
            case DEEP_OCEAN -> Color.rgb(10, 30, 60, 0.85);
            case BEACH -> Color.rgb(180, 160, 100, 0.8);
            case DESERT -> Color.rgb(160, 110, 50, 0.8);
            case PLAINS -> Color.rgb(45, 95, 45, 0.8);
            case SAVANNAH -> Color.rgb(130, 140, 50, 0.8);
            case FOREST -> Color.rgb(25, 75, 40, 0.85);
            case JUNGLE -> Color.rgb(15, 85, 30, 0.85);
            case MOUNTAINS -> Color.rgb(110, 110, 120, 0.85);
            case HILLS -> Color.rgb(90, 100, 80, 0.8);
            case TUNDRA -> Color.rgb(130, 140, 140, 0.8);
            case SNOW, GLACIER -> Color.rgb(200, 215, 225, 0.85);
            case LAKE -> Color.rgb(20, 70, 120, 0.75);
        };
    }

    private double getCellValue(H3Cell c, DisplayMode mode) {
        if (c == null || mode == null) return 0.0;
        return switch (mode) {
            case BIOME -> c.getBiome() != null ? c.getBiome().ordinal() : 0.0;
            case TEMPERATURE -> c.getTemperature() != null ? c.getTemperature() : 15.0;
            case PRECIPITATION -> c.getRainfall() != null ? c.getRainfall() : 500.0;
            case WATER -> c.getWaterResource() != null ? c.getWaterResource() : 100.0;
            case ALBEDO -> c.getDynamicAlbedo() != null ? c.getDynamicAlbedo() : 0.3;
            case FRICTION -> c.getMovementFriction() != null ? c.getMovementFriction() : 1.0;
            case ENERGY_CAPACITY -> c.getEnergySolar() != null ? c.getEnergySolar() : 0.0;
            case ENTROPY_POLLUTION -> c.getPollutionLevel() != null ? c.getPollutionLevel() : 0.0;
            case SOIL_QUALITY -> c.getSoilOrganicCarbon() != null ? c.getSoilOrganicCarbon() : (c.getFoodResource() != null ? c.getFoodResource() / 50.0 : 50.0);
            case BIODIVERSITY -> c.getBiomassNatural() != null ? c.getBiomassNatural() : 50.0;
            case OCEAN_PH -> 8.1;
            case PERMAFROST -> c.getIceSheetThicknessMeters() != null ? c.getIceSheetThicknessMeters() : 0.0;
            case POPULATION -> c.getPopulation() != null ? c.getPopulation().doubleValue() : 0.0;
            case MIGRATION -> c.getFluxPressure();
            case AGE_PYRAMID -> c.getPopElderly() != null && c.getPopulation() != null && c.getPopulation() > 0 ? ((double) c.getPopElderly() / c.getPopulation()) * 100.0 : 15.0;
            case EPIDEMIC -> c.getPollutionLevel() != null ? c.getPollutionLevel() * 0.8 : 0.0;
            case HEALTH_LIFE_EXPECTANCY -> c.getLifespan() != null ? c.getLifespan() : 45.0;
            case EDUCATION_LEVEL -> c.getTechnologyLevel() != null ? Math.min(100.0, c.getTechnologyLevel() * 12.0) : 10.0;
            case MALTHUSIAN_PRESSURE -> {
                double food = c.getFoodResource() != null ? c.getFoodResource() : 10.0;
                double pop = c.getPopulation() != null ? c.getPopulation() : 0.0;
                yield pop > 0 ? (pop * 3.362) / Math.max(1.0, food) : 0.0;
            }
            case CAPACITY -> c.getFoodResource() != null ? c.getFoodResource() / 3.362 : 1000.0;
            case FOOD -> c.getFoodResource() != null ? c.getFoodResource() : 0.0;
            case WOOD -> c.getWoodResource() != null ? c.getWoodResource() : 100.0;
            case MINERAL_RESOURCES -> c.getResourceMetal() != null ? c.getResourceMetal() : 0.0;
            case MINING_EXPLOITATION -> c.getResourceWork() != null ? c.getResourceWork() : 0.0;
            case TECHNOLOGY -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
            case CULTURE -> c.getOwner() != null ? Math.abs(c.getOwner().hashCode() % 100) : 1.0;
            case POLITICAL -> c.getOwner() != null ? 100.0 : 0.0;
            case INEQUALITY -> c.getGiniIndex() != null ? c.getGiniIndex() : 0.35;
            case ASABIYYAH -> 0.70;
            case FLUX -> c.getFluxPressure();
            case HAPPINESS -> 65.0;
            case CONFLICT -> c.getOwner() != null ? 75.0 : 5.0;
            case INSTITUTIONAL_MATURITY -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() * 10.0 : 5.0;
            case GDP_WEALTH -> {
                double pop = c.getPopulation() != null ? c.getPopulation() : 0.0;
                double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
                yield pop * tech * 15.0;
            }
            case ELITE_DENSITY -> 2.5;
            case COLLECTIVE_MEMORY -> c.getTechnologyLevel() != null ? Math.pow(c.getTechnologyLevel(), 2.0) * 100.0 : 10.0;
            case COLLAPSE_RISK -> {
                double pop = c.getPopulation() != null ? c.getPopulation() : 0.0;
                double food = c.getFoodResource() != null ? c.getFoodResource() : 1.0;
                yield pop > 0 && food < pop ? Math.min(100.0, ((pop - food) / pop) * 100.0) : 5.0;
            }
        };
    }

    private Color getColorForDisplayMode(double ratio, DisplayMode mode, double rawVal) {
        if (mode.getEncodingType() == DisplayMode.EncodingType.ID_24BIT_CATEGORICAL) {
            return Color.hsb(ratio * 360, 0.85, 0.95, 0.80);
        }
        return switch (mode.getCategory()) {
            case PHYSICAL -> {
                if (mode == DisplayMode.TEMPERATURE) {
                    yield Color.color(ratio, 0.2, 1.0 - ratio, 0.80);
                } else if (mode == DisplayMode.PRECIPITATION || mode == DisplayMode.WATER) {
                    yield Color.color(0.1, 0.4 + ratio * 0.4, Math.min(1.0, 0.6 + ratio * 0.4), 0.80);
                } else {
                    yield Color.color(Math.min(1.0, ratio * 1.2), Math.min(1.0, 0.8 - ratio * 0.4), 0.2, 0.80);
                }
            }
            case DEMOGRAPHICS -> {
                if (ratio < 0.3) {
                    yield Color.rgb((int)(255 * (ratio / 0.3)), (int)(220 * (ratio / 0.3)), 0, 0.80);
                } else if (ratio < 0.7) {
                    double t = (ratio - 0.3) / 0.4;
                    yield Color.rgb(255, (int)(220 - t * 140), 0, 0.85);
                } else {
                    double t = (ratio - 0.7) / 0.3;
                    yield Color.rgb(255, (int)(80 - t * 60), (int)(t * 40), 0.90);
                }
            }
            case ECOLOGY -> Color.color(0.2, Math.min(1.0, 0.3 + ratio * 0.7), 0.3, 0.80);
            case MINING -> Color.color(Math.min(1.0, 0.4 + ratio * 0.6), 0.3, Math.min(1.0, 0.6 + ratio * 0.4), 0.80);
            case SOCIETY_POLITICS -> {
                if (mode == DisplayMode.COLLAPSE_RISK || mode == DisplayMode.CONFLICT) {
                    yield Color.color(Math.min(1.0, 0.5 + ratio * 0.5), 0.1, 0.1, 0.85);
                } else if (mode == DisplayMode.GDP_WEALTH) {
                    yield Color.color(0.2, Math.min(1.0, 0.4 + ratio * 0.6), Math.min(1.0, ratio * 1.2), 0.85);
                } else {
                    yield Color.color(Math.min(1.0, 0.3 + ratio * 0.7), 0.4, Math.min(1.0, 0.8 - ratio * 0.4), 0.80);
                }
            }
        };
    }

    private double computeMoranI(DisplayMode mode) {
        if (currentCells.size() < 4) return 0.0;
        double sum = 0;
        for (H3Cell c : currentCells) sum += getCellValue(c, mode);
        double mean = sum / currentCells.size();

        double num = 0, denom = 0;
        int n = currentCells.size();

        for (int i = 0; i < n; i++) {
            double zi = getCellValue(currentCells.get(i), mode) - mean;
            denom += zi * zi;
            for (int j = i + 1; j < Math.min(n, i + 10); j++) {
                double zj = getCellValue(currentCells.get(j), mode) - mean;
                num += zi * zj;
            }
        }
        return denom > 0 ? Math.clamp(num / denom, -1.0, 1.0) : 0.0;
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

    private void toggleAnimation() {
        if (isPlayingAnimation) {
            animationTimeline.pause();
            isPlayingAnimation = false;
            playPauseBtn.setText("▶");
            playPauseBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 6;");
            playPauseBtn.setTooltip(new Tooltip(I18n.getOrDefault("heatmap.tooltip.play", "Lancer la séquence temporelle (2D+1D)")));
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
