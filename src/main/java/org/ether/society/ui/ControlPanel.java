/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.core.ISimulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.i18n.Language;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.SnapshotParameters;
import javafx.embed.swing.SwingFXUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.ether.society.events.ActiveEvent;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern simulation control and rendering panel for Tab 4 (Simulation View).
 * Provides video recording, HD screenshots, temporal controls, display modes, and live metrics.
 */
public class ControlPanel extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(ControlPanel.class);

    private final ISimulationEngine engine;
    private NotificationOverlay notificationOverlay;

    // Temporal Labels
    private final Label scenarioHeaderLabel;
    private final Label dateHeaderLabel;
    private final Label ageLabel;
    private final Label seasonLabel;

    // Statistics Labels
    private final Label popStatValue;
    private final Label foodStatValue;
    private final Label cellStatValue;

    // References to UI components
    private H3MapCanvas mapCanvas;
    private MiniMap miniMap;
    private ColorLegend colorLegend;
    private final Label dbStatusLabel;
    private final List<String> eventHistory = new ArrayList<>();

    // Section 5: Recent Events UI
    private final Label eventsTitleLabel;
    private final Label eventsHintLabel;
    private final Label noEventsLabel;
    private final VBox eventsListBox;

    // Controls
    private final Button playPauseBtn;
    private final Button rewindBtn;
    private final Button fastRewindBtn;
    private final Button stepBackBtn;
    private final Button stepForwardBtn;
    private final Button fastForwardBtn;
    private final Button endBtn;

    private final ToggleButton speedMax;
    private final Slider speedSlider;
    private final Label speedValueLabel;

    private final Button hdScreenshotBtn;
    private final Button recordVideoBtn;
    private final CheckBox autoRecordCheck;
    private boolean isRecordingVideo = false;

    private final CheckBox mode3dCheck;
    private final CheckBox autoRotateCheck;
    private final Label reliefLabel;
    private final Slider reliefSlider;
    private final CheckBox hillshadingCheck;
    private final CheckBox solarTerminatorCheck;
    private final CheckBox smoothMapCheck;
    private final CheckBox contourCheck;
    private final CheckBox fluxVectorCheck;
    private final CheckBox resourceOverlayCheck;
    private final CheckBox floatingLayerCheck;
    private final CheckBox legendCheck;
    private final CheckBox dateOverlayCheck;
    private final CheckBox cellInfoCheck;
    private final CheckBox hexGridCheck;
    private final MenuButton activeLayersMenuBtn;
    private final java.util.Map<DisplayMode, CheckBox> layerCheckBoxMap = new java.util.EnumMap<>(DisplayMode.class);
    private final FlowPane activeLayersChipsBox;
    private final Button fullScreenBtn;

    // Callbacks
    private Runnable onSave;
    private Runnable onLoad;
    private Consumer<Boolean> onContourToggle;
    private Runnable onTimelapseRecord;
    private Consumer<Integer> onTimelapseSeek;
    private Runnable onTimelapseSeekToEnd;
    private Consumer<Boolean> onStatsToggle;
    private Runnable onFullScreen;

    public ControlPanel(ISimulationEngine engine) {
        this.engine = engine;

        setSpacing(10);
        setPadding(new Insets(12));
        getStyleClass().add("glass-panel");

        // --- 1. DATE & TIME HEADER CARD ---
        scenarioHeaderLabel = new Label("🎬 " + I18n.getOrDefault("sim.header.scenario", "Scenario: ") + "Out of Africa");
        scenarioHeaderLabel.getStyleClass().add("sidebar-recap-highlight");
        scenarioHeaderLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        scenarioHeaderLabel.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.scenario_name", "Name of current historical or procedural scenario")));

        dateHeaderLabel = new Label("📅 " + I18n.getOrDefault("sim.header.date", "Date : ") + "An -100000");
        dateHeaderLabel.getStyleClass().add("sidebar-title");
        dateHeaderLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        dateHeaderLabel.setTooltip(new Tooltip(
            I18n.getOrDefault("sim.tooltip.date_header", "⏱️ Horloge courante de la simulation selon le pas temporel configuré.")
        ));

        dbStatusLabel = new Label(I18n.getOrDefault("sim.status.dbcheck", "DB: Checking..."));
        dbStatusLabel.getStyleClass().add("control-label");
        dbStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-padding: 2 0 0 0;");

        // --- 2. TEMPORAL & PLAYBACK CONTROLS CARD ---
        Label timeTitle = createCardTitle("⏱️ " + I18n.getOrDefault("sim.card.time", "TIME & PLAYBACK CONTROLS"));

        rewindBtn = new Button("⏮");
        rewindBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.rewind", "Réinitialiser T=0")));
        rewindBtn.setOnAction(e -> {
            engine.pause();
            updatePlayPauseVisuals(false);
            if (onTimelapseSeek != null) {
                int startYear = engine.getCurrentScenario() != null ? (int) engine.getCurrentScenario().getStartDateYear() : -20000;
                onTimelapseSeek.accept(startYear);
            }
        });

        fastRewindBtn = new Button("⏪");
        fastRewindBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastrewind", "Reculer de 1000 pas [Maintenir appuyé]")));
        setupRepeatAction(fastRewindBtn, () -> engine.stepBackward(1000));

        stepBackBtn = new Button("⏴");
        stepBackBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stepback", "Reculer de 10 pas [Maintenir appuyé]")));
        setupRepeatAction(stepBackBtn, () -> engine.stepBackward(10));

        // Auto Record Checkbox (initialized early for button handlers)
        autoRecordCheck = new CheckBox(I18n.getOrDefault("sim.option.auto_record", "🎬 Auto Sync Video (Start & Pause)"));
        autoRecordCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.auto_record", "Automatically starts/stops 1:1 video recording in sync with scenario start/pause")));
        autoRecordCheck.getStyleClass().add("opt-sub-checkbox");
        autoRecordCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");

        playPauseBtn = new Button("▶");
        playPauseBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.start", "Lancer / Reprendre")));
        playPauseBtn.setOnAction(e -> {
            if (engine.isRunning()) {
                engine.pause();
                if (autoRecordCheck != null && autoRecordCheck.isSelected() && isRecordingVideo) {
                    toggleVideoRecording();
                }
                updatePlayPauseVisuals(false);
            } else {
                if (autoRecordCheck.isSelected() && !isRecordingVideo) {
                    toggleVideoRecording();
                }
                engine.start();
                updatePlayPauseVisuals(true);
            }
        });

        stepForwardBtn = new Button("⏵");
        stepForwardBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stepforward", "Avancer de 10 pas [Maintenir appuyé]")));
        setupRepeatAction(stepForwardBtn, () -> engine.stepForward(10));

        fastForwardBtn = new Button("⏩");
        fastForwardBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastforward", "Avancer de 1000 pas [Maintenir appuyé]")));
        setupRepeatAction(fastForwardBtn, () -> engine.stepForward(1000));

        endBtn = new Button("⏭");
        endBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastforward_end", "Aller à la fin de la simulation (Dernier checkpoint / Fin)")));
        endBtn.setOnAction(e -> {
            engine.pause();
            updatePlayPauseVisuals(false);
            if (onTimelapseSeekToEnd != null) {
                onTimelapseSeekToEnd.run();
            } else {
                engine.seekToEnd();
            }
        });

        Button[] playButtons = { rewindBtn, fastRewindBtn, stepBackBtn, playPauseBtn, stepForwardBtn, fastForwardBtn, endBtn };
        for (Button btn : playButtons) {
            btn.setMinWidth(36);
            btn.setPrefWidth(38);
            btn.setMaxWidth(44);
            btn.setMinHeight(30);
            btn.setPrefHeight(30);
            btn.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 4; -fx-background-radius: 6; -fx-cursor: hand;");
        }

        updatePlayPauseVisuals(engine.isRunning());

        CheckBox pauseOnEventCheck = new CheckBox(I18n.getOrDefault("sim.option.pause_on_event", "⏸️ Auto-pause on event"));
        pauseOnEventCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.pause_on_event", "Automatically pauses simulation when a new planetary or regional event occurs")));
        pauseOnEventCheck.getStyleClass().add("opt-sub-checkbox");
        pauseOnEventCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        pauseOnEventCheck.setOnAction(e -> engine.setPauseAtNextEvent(pauseOnEventCheck.isSelected()));

        HBox playBar = new HBox(4, rewindBtn, fastRewindBtn, stepBackBtn, playPauseBtn, stepForwardBtn, fastForwardBtn, endBtn);
        playBar.setAlignment(Pos.CENTER);

        speedSlider = new Slider(0.1, 20.0, 1.0);
        speedSlider.setBlockIncrement(0.5);
        speedSlider.setMajorTickUnit(5.0);
        speedSlider.setMinorTickCount(4);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setSnapToTicks(false);
        speedSlider.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.speed_slider", "Vitesse de simulation CPU (0.1 à 20 pas/sec ou MAX)")));
        HBox.setHgrow(speedSlider, Priority.ALWAYS);

        speedValueLabel = new Label("⏱️ " + I18n.getOrDefault("sim.speed.label", "Vitesse : 1 pas/sec"));
        speedValueLabel.getStyleClass().add("value-label");
        speedValueLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        speedMax = new ToggleButton("MAX 🚀");
        speedMax.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.speed_max", "Calcule les itérations à la vitesse maximale du processeur")));
        updateSpeedMaxStyle(false);

        speedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double raw = newV.doubleValue();
            double spd;
            if (raw < 1.0) {
                spd = Math.max(0.1, Math.round(raw * 10.0) / 10.0);
            } else if (raw <= 5.0) {
                spd = Math.round(raw * 2.0) / 2.0;
            } else {
                spd = Math.round(raw);
            }

            if (spd < speedSlider.getMax() && speedMax.isSelected()) {
                updateSpeedMaxStyle(false);
            }
            if (!speedMax.isSelected()) {
                engine.setSpeed(spd);
                updateSpeedLabel(spd);
            }
        });

        speedMax.setOnAction(e -> {
            if (speedMax.isSelected()) {
                speedSlider.setValue(speedSlider.getMax());
                updateSpeedMaxStyle(true);
                engine.setSpeed(999.0);
                updateSpeedLabel(999.0);
            } else {
                updateSpeedMaxStyle(false);
                double spd = speedSlider.getValue();
                if (spd < 1.0) {
                    spd = Math.max(0.1, Math.round(spd * 10.0) / 10.0);
                } else if (spd <= 5.0) {
                    spd = Math.round(spd * 2.0) / 2.0;
                } else {
                    spd = Math.round(spd);
                }
                engine.setSpeed(spd);
                updateSpeedLabel(spd);
            }
        });

        updateSpeedLabel(1.0);

        HBox sliderRow = new HBox(8, speedSlider, speedMax);
        sliderRow.setAlignment(Pos.TOP_LEFT);
        HBox.setMargin(speedMax, new Insets(2, 0, 0, 0));

        // --- 1. HORLOGE & CONTRÔLE TEMPOREL CARD ---
        VBox timeCard = new VBox(8, scenarioHeaderLabel, dateHeaderLabel, dbStatusLabel, timeTitle, playBar, pauseOnEventCheck, speedValueLabel, sliderRow);
        styleCard(timeCard);

        // --- 3. MEDIA & EXPORT MP4 CARD ---
        Label mediaTitle = createCardTitle("📸 " + I18n.getOrDefault("sim.card.media", "SCREENSHOTS & VIDEO"));

        hdScreenshotBtn = new Button("📸 " + I18n.getOrDefault("sim.btn.screenshot", "Capture Photo HD"));
        hdScreenshotBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.screenshot", "Exports HD PNG screenshot to saves/screenshots/")));
        hdScreenshotBtn.setMaxWidth(Double.MAX_VALUE);
        hdScreenshotBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 10; -fx-background-radius: 6;");
        hdScreenshotBtn.setOnAction(e -> takeHDScreenshot());

        recordVideoBtn = new Button("🎥 " + I18n.getOrDefault("sim.btn.video", "Record MP4 Video"));
        recordVideoBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.video", "Starts MP4 video capture (1:1 step) in saves/timelapse/")));
        recordVideoBtn.setMaxWidth(Double.MAX_VALUE);
        recordVideoBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 10; -fx-background-radius: 6;");
        recordVideoBtn.setOnAction(e -> toggleVideoRecording());

        // --- 2. PILE DE COUCHES & PRÉRÉGLAGES SCIENTIFIQUES CARD ---
        Label layersTitle = createCardTitle(I18n.getOrDefault("sim.card.layers_presets", "🗺️ 2. LAYER STACK & SCIENTIFIC PRESETS"));

        // 1-Click Scientific Presets
        Label presetsTitle = new Label(I18n.getOrDefault("sim.presets.1click", "⚡ 1-Click Scientific Presets:"));
        presetsTitle.getStyleClass().add("opt-subheader");
        presetsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        Button presetSynth = createPresetBtn(I18n.getOrDefault("sim.preset.synthesis", "🌍 Synthesis"), I18n.getOrDefault("sim.preset.synthesis.desc", "3D Globe Mode + Biomes + Relief + Contour Lines"), () -> applyPresetSynthesis(pauseOnEventCheck));
        Button presetEcon = createPresetBtn(I18n.getOrDefault("sim.preset.economy", "📈 Economy"), I18n.getOrDefault("sim.preset.economy.desc", "Flux / GDP Map + Transport Vectors + Ore Deposits"), () -> applyPresetEcon());
        Button presetClim = createPresetBtn(I18n.getOrDefault("sim.preset.climate", "🌡️ Climate"), I18n.getOrDefault("sim.preset.climate.desc", "Temperature Map + Aquifers + Relief Contour Lines"), () -> applyPresetClimate());
        Button presetCliodyn = createPresetBtn(I18n.getOrDefault("sim.preset.cliodynamics", "🏛️ Cliodynamics"), I18n.getOrDefault("sim.preset.cliodynamics.desc", "Demographic Map + H3 Mesh + Event Pause"), () -> applyPresetCliodynamics(pauseOnEventCheck));

        GridPane presetGrid = new GridPane();
        presetGrid.setHgap(4);
        presetGrid.setVgap(4);
        presetGrid.add(presetSynth, 0, 0);
        presetGrid.add(presetEcon, 1, 0);
        presetGrid.add(presetClim, 0, 1);
        presetGrid.add(presetCliodyn, 1, 1);
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50);
        presetGrid.getColumnConstraints().addAll(col1, col2);

        VBox presetBox = new VBox(4, presetsTitle, presetGrid);
        presetBox.getStyleClass().add("subcard-section");

        Label layerComboLabel = new Label(I18n.getOrDefault("sim.layer.datacategory", "Active Data Layers (Multi-Select):"));
        layerComboLabel.getStyleClass().add("card-description-muted");
        layerComboLabel.setStyle("-fx-font-size: 11px;");

        activeLayersMenuBtn = new MenuButton();
        activeLayersMenuBtn.setMaxWidth(Double.MAX_VALUE);
        activeLayersMenuBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.datacategory", "Cochez une ou plusieurs couches pour les superposer sur la carte")));
        activeLayersMenuBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: #38bdf8; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");

        activeLayersChipsBox = new FlowPane(4, 4);
        activeLayersChipsBox.setStyle("-fx-padding: 2 0;");

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
                    cb.setSelected(dm == DisplayMode.BIOME || dm == DisplayMode.POPULATION);
                    cb.setOnAction(e -> onLayerToggled(dm, cb.isSelected()));
                    layerCheckBoxMap.put(dm, cb);
                    CustomMenuItem mi = new CustomMenuItem(cb, false);
                    activeLayersMenuBtn.getItems().add(mi);
                }
            }
            activeLayersMenuBtn.getItems().add(new SeparatorMenuItem());
        }

        updateActiveLayerUI();

        contourCheck = new CheckBox(I18n.getOrDefault("sim.layer.contours", "📈 Courbes de Niveau (Isolines)"));
        contourCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.contours", "Displays elevation contour lines on H3 cells")));
        contourCheck.getStyleClass().add("opt-sub-checkbox");
        contourCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        contourCheck.setOnAction(e -> {
            if (mapCanvas != null) mapCanvas.setShowContours(contourCheck.isSelected());
            if (onContourToggle != null) onContourToggle.accept(contourCheck.isSelected());
        });

        fluxVectorCheck = new CheckBox(I18n.getOrDefault("sim.layer.fluxvectors", "🌊 Flux & Transports (Vecteurs)"));
        fluxVectorCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fluxvectors", "Overlays material flux and population transport vectors")));
        fluxVectorCheck.getStyleClass().add("opt-sub-checkbox");
        fluxVectorCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        fluxVectorCheck.setOnAction(e -> {
            if (mapCanvas != null) mapCanvas.setShowFlowVectors(fluxVectorCheck.isSelected());
        });

        resourceOverlayCheck = new CheckBox(I18n.getOrDefault("sim.layer.resources", "💎 Deposits & Capital (Overlays)"));
        resourceOverlayCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.resources", "Displays metal, aquifer, and infrastructure markers")));
        resourceOverlayCheck.getStyleClass().add("opt-sub-checkbox");
        resourceOverlayCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        resourceOverlayCheck.setOnAction(e -> {
            if (mapCanvas != null) mapCanvas.setShowResourceOverlay(resourceOverlayCheck.isSelected());
        });

        floatingLayerCheck = new CheckBox(I18n.getOrDefault("sim.layer.floating", "☁️ Calques Flottants 2.5D (Altitude)"));
        floatingLayerCheck.setSelected(true);
        floatingLayerCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.floating", "Projette les couches thématiques en élévation 2.5D flottant au-dessus du relief des biomes")));
        floatingLayerCheck.getStyleClass().add("opt-sub-checkbox");
        floatingLayerCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        floatingLayerCheck.setOnAction(e -> {
            if (mapCanvas != null) mapCanvas.setShowFloatingLayers(floatingLayerCheck.isSelected());
        });

        legendCheck = new CheckBox(I18n.getOrDefault("sim.layer.legend", "🗺️ Légende des Couleurs (Overlay)"));
        legendCheck.setSelected(true);
        legendCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.legend", "Affiche ou masque le panneau de légende des couleurs")));
        legendCheck.getStyleClass().add("opt-sub-checkbox");
        legendCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        legendCheck.setOnAction(e -> {
            if (colorLegend != null) {
                colorLegend.setVisible(legendCheck.isSelected());
                colorLegend.setManaged(legendCheck.isSelected());
            }
        });

        dateOverlayCheck = new CheckBox(I18n.getOrDefault("sim.layer.date_overlay", "📅 Incrustation Date & Scénario"));
        dateOverlayCheck.setSelected(true);
        dateOverlayCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.date_overlay", "Affiche ou masque les badges d'incrustation temporelle sur la carte")));
        dateOverlayCheck.getStyleClass().add("opt-sub-checkbox");
        dateOverlayCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        dateOverlayCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setShowCornerOverlays(dateOverlayCheck.isSelected());
            }
        });

        cellInfoCheck = new CheckBox(I18n.getOrDefault("sim.layer.cell_info", "ℹ️ Infos Cellule au Survol (Infobulle)"));
        cellInfoCheck.setSelected(true);
        cellInfoCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.cell_info", "Active ou désactive l'infobulle d'inspection au survol des hexagones")));
        cellInfoCheck.getStyleClass().add("opt-sub-checkbox");
        cellInfoCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        cellInfoCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setShowMouseOverInfo(cellInfoCheck.isSelected());
            }
        });

        Label dataLayersTitle = new Label(I18n.getOrDefault("sim.layer.datalayers", "📊 Calques de Données & Vecteurs :"));
        dataLayersTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");

        VBox dataLayersBox = new VBox(4, dataLayersTitle, contourCheck, fluxVectorCheck, resourceOverlayCheck, floatingLayerCheck);
        dataLayersBox.getStyleClass().add("subcard-section");

        Label uiOverlaysTitle = new Label(I18n.getOrDefault("sim.layer.uioverlays", "🖥️ Affichage & Surimpressions UI :"));
        uiOverlaysTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");

        VBox uiOverlaysBox = new VBox(4, uiOverlaysTitle, legendCheck, dateOverlayCheck, cellInfoCheck);
        uiOverlaysBox.getStyleClass().add("subcard-section");

        VBox layersCard = new VBox(8, layersTitle, presetBox, layerComboLabel, activeLayersMenuBtn, activeLayersChipsBox, dataLayersBox, uiOverlaysBox);
        styleCard(layersCard);

        // --- 3. PROJECTION & PARAMÈTRES DE RENDU CARD ---
        Label renderTitle = createCardTitle(I18n.getOrDefault("sim.card.rendering", "🌐 3. PROJECTION & RENDERING PARAMETERS"));

        mode3dCheck = new CheckBox(I18n.getOrDefault("sim.layer.mode3d", "🌐 Globe 3D H3"));
        mode3dCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.mode3d", "Toggles between 3D spherical globe and 2D flat map")));
        mode3dCheck.getStyleClass().add("opt-sub-checkbox");
        mode3dCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");

        reliefLabel = new Label(I18n.getOrDefault("sim.layer.relief3d", "⛰️ Relief 3D") + " : 25x");
        reliefLabel.getStyleClass().add("control-label");
        reliefLabel.setDisable(true);

        reliefSlider = new Slider(0.0, 50.0, 25.0);
        reliefSlider.setBlockIncrement(5.0);
        reliefSlider.setMajorTickUnit(15.0);
        reliefSlider.setMinorTickCount(2);
        reliefSlider.setShowTickMarks(true);
        reliefSlider.setShowTickLabels(false);
        reliefSlider.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.relief3d", "Adjusts topographic elevation height in 3D Globe mode (0.0x to 50x)")));
        reliefSlider.setDisable(true);
        reliefSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double val = newV.doubleValue();
            if (mapCanvas != null) {
                mapCanvas.setVerticalExaggeration(val);
            }
            reliefLabel.setText(String.format(java.util.Locale.ROOT, "%s : %.0fx", I18n.getOrDefault("sim.layer.relief3d", "⛰️ Relief 3D"), val));
        });

        autoRotateCheck = new CheckBox(I18n.getOrDefault("sim.layer.autorotate", "🔄 Auto-rotation Globe"));
        autoRotateCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.autorotate", "Auto-rotates 3D spherical globe")));
        autoRotateCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        autoRotateCheck.setDisable(true);
        autoRotateCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setAutoRotating(autoRotateCheck.isSelected());
            }
        });

        hillshadingCheck = new CheckBox(I18n.getOrDefault("sim.layer.hillshading", "⛰️ Ombrage Relief Topographique"));
        hillshadingCheck.setSelected(false);
        hillshadingCheck.setDisable(true);
        hillshadingCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.hillshading", "Applique un ombrage topographique lambertien selon la pente du relief")));
        hillshadingCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        hillshadingCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setShowHillshading(hillshadingCheck.isSelected());
            }
        });

        solarTerminatorCheck = new CheckBox(I18n.getOrDefault("sim.layer.solarterminator", "☀️ Terminateur Solaire Jour/Nuit"));
        solarTerminatorCheck.setSelected(false);
        solarTerminatorCheck.setDisable(true);
        solarTerminatorCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.solarterminator", "Displays night and twilight shadow overlay")));
        solarTerminatorCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        solarTerminatorCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setShowSolarTerminator(solarTerminatorCheck.isSelected());
            }
        });

        mode3dCheck.setOnAction(e -> {
            boolean is3D = mode3dCheck.isSelected();
            if (mapCanvas != null) {
                mapCanvas.setViewMode(is3D ? ViewMode.VIEW_3D : ViewMode.VIEW_2D);
            }
            reliefSlider.setDisable(!is3D);
            reliefLabel.setDisable(!is3D);
            autoRotateCheck.setDisable(!is3D);
            hillshadingCheck.setDisable(!is3D);
            solarTerminatorCheck.setDisable(!is3D);
        });

        VBox globe3dSubBox = new VBox(6, reliefLabel, reliefSlider, autoRotateCheck, hillshadingCheck, solarTerminatorCheck);
        globe3dSubBox.setStyle("-fx-padding: 4 0 4 12; -fx-border-color: rgba(56, 189, 248, 0.2); -fx-border-width: 0 0 0 2;");

        // 2D & General Map Parameters
        smoothMapCheck = new CheckBox(I18n.getOrDefault("sim.layer.smoothmap", "🎨 Smooth Map (Continuous Heatmap)"));
        smoothMapCheck.setSelected(true);
        smoothMapCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.smoothmap", "Displays continuous map with smooth gradients instead of individual hexagons")));
        smoothMapCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        smoothMapCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setSmoothMap(smoothMapCheck.isSelected());
            }
        });

        hexGridCheck = new CheckBox(I18n.getOrDefault("sim.layer.hexgrid", "⬡ H3 Hexagon Borders"));
        hexGridCheck.setSelected(false);
        hexGridCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.hexgrid", "Shows or hides H3 hexagon grid (smooth view without borders vs grid view)")));
        hexGridCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        hexGridCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setShowHexGrid(hexGridCheck.isSelected());
            }
        });

        Label paletteLabel = new Label(I18n.getOrDefault("sim.render.palette", "🎨 Palette Scientifique :"));
        paletteLabel.getStyleClass().add("control-label");

        ComboBox<ScientificColorMap> paletteCombo = new ComboBox<>();
        paletteCombo.getItems().addAll(ScientificColorMap.values());
        paletteCombo.setValue(ScientificColorMap.TURBO);
        paletteCombo.setMaxWidth(Double.MAX_VALUE);
        paletteCombo.setStyle("-fx-font-size: 11px;");
        paletteCombo.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.palette", "Choisit la palette de couleurs perceptuellement uniforme")));
        paletteCombo.setOnAction(e -> {
            if (mapCanvas != null && paletteCombo.getValue() != null) {
                mapCanvas.setScientificColorMap(paletteCombo.getValue());
                if (colorLegend != null) colorLegend.updateFromCanvas(mapCanvas);
            }
        });

        fullScreenBtn = new Button(I18n.getOrDefault("sim.btn.fullscreen", "🖥️ Plein Écran (Carte)"));
        fullScreenBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fullscreen", "Affiche la carte en plein écran sans aucun autre élément (Touche Échap pour quitter)")));
        fullScreenBtn.setMaxWidth(Double.MAX_VALUE);
        fullScreenBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 7 12; -fx-background-radius: 6; -fx-cursor: hand;");
        fullScreenBtn.setOnAction(e -> {
            if (onFullScreen != null) {
                onFullScreen.run();
            }
        });

        VBox renderCard = new VBox(8, renderTitle, mode3dCheck, globe3dSubBox, paletteLabel, paletteCombo, smoothMapCheck, hexGridCheck, fullScreenBtn);
        styleCard(renderCard);

        // --- 4. TÉLÉMÉTRIE, CAPTURES & EXPORT CARD ---
        Label exportTitle = createCardTitle(I18n.getOrDefault("sim.card.telemetry", "📊 4. TELEMETRY, SCREENSHOTS & EXPORTS"));
        VBox exportCard = new VBox(8, exportTitle, hdScreenshotBtn, recordVideoBtn, autoRecordCheck);
        styleCard(exportCard);

        // --- 5. ÉVÉNEMENTS RÉCENTS (CHRONOLOGIE) CARD ---
        eventsTitleLabel = createCardTitle(I18n.getOrDefault("sim.card.recent_events", "📜 5. ÉVÉNEMENTS RÉCENTS (CHRONOLOGIE)"));
        eventsHintLabel = new Label(I18n.getOrDefault("sim.events.double_click_hint", "💡 Double-cliquer sur un événement pour voler vers sa position"));
        eventsHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
        eventsHintLabel.setWrapText(true);

        noEventsLabel = new Label(I18n.getOrDefault("sim.events.no_events", "Aucun événement enregistré pour le moment."));
        noEventsLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-style: italic; -fx-padding: 4 0;");

        eventsListBox = new VBox(6);
        eventsListBox.getChildren().add(noEventsLabel);

        VBox eventsCard = new VBox(8, eventsTitleLabel, eventsHintLabel, eventsListBox);
        styleCard(eventsCard);

        // Keep age and season labels initialized for dateHeaderBox updates
        popStatValue = new Label();
        foodStatValue = new Label();
        cellStatValue = new Label();
        ageLabel = new Label(I18n.getOrDefault("sim.age.stone_age", "Age: Stone Age"));
        ageLabel.getStyleClass().add("control-label");
        seasonLabel = new Label(I18n.getOrDefault("sim.season.spring", "Season: Spring"));
        seasonLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");

        // Combine cards cleanly into 5 modular accordion-style sections
        getChildren().addAll(timeCard, layersCard, renderCard, exportCard, eventsCard);

        updateTexts();
        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    public void updatePlayPauseVisuals(boolean isRunning) {
        if (playPauseBtn == null) return;
        if (isRunning) {
            playPauseBtn.setText("⏸");
            playPauseBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.pause", "Mettre en pause")));
            playPauseBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 2 4; -fx-border-color: #34d399; -fx-border-width: 1.5px; -fx-background-radius: 6; -fx-border-radius: 6; -fx-effect: dropshadow(three-pass-box, rgba(16,185,129,0.7), 6, 0, 0, 0); -fx-cursor: hand;");
        } else {
            playPauseBtn.setText("▶");
            playPauseBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.start", "Lancer / Reprendre")));
            playPauseBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 2 4; -fx-border-color: #38bdf8; -fx-border-width: 1.5px; -fx-background-radius: 6; -fx-border-radius: 6; -fx-effect: dropshadow(three-pass-box, rgba(2,132,199,0.5), 6, 0, 0, 0); -fx-cursor: hand;");
        }
    }

    private void updateSpeedMaxStyle(boolean isMax) {
        if (speedMax == null) return;
        speedMax.setSelected(isMax);
        if (isMax) {
            speedMax.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4; -fx-cursor: hand; -fx-border-color: #c4b5fd; -fx-border-width: 1.5; -fx-border-radius: 4; -fx-effect: dropshadow(three-pass-box, rgba(139,92,246,0.8), 6, 0, 0, 0);");
        } else {
            speedMax.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4; -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 1.5; -fx-border-radius: 4; -fx-effect: none;");
        }
    }

    private void updateSpeedLabel(double spd) {
        if (speedValueLabel == null) return;
        if (speedMax != null && speedMax.isSelected()) {
            speedValueLabel.setText("⏱️ " + I18n.getOrDefault("sim.speed.max_label", "Vitesse Cible : MAX 🚀 (Calcul CPU sans limite de fréquence)"));
            return;
        }
        double stepDays = engine != null && engine.getCurrentScenario() != null && engine.getCurrentScenario().getTemporalResolutionDays() > 0
                ? engine.getCurrentScenario().getTemporalResolutionDays() : 1.0;
        double simDaysPerSec = spd * stepDays;

        String spdStr = (Math.abs(spd - Math.round(spd)) < 0.001)
                ? String.valueOf(Math.round(spd))
                : String.format(java.util.Locale.US, "%.1f", spd);

        String timeUnit;
        if (stepDays >= 360.0) {
            double yearsPerSec = simDaysPerSec / 365.0;
            if (Math.abs(yearsPerSec - 1.0) < 0.001) {
                timeUnit = I18n.getOrDefault("sim.speed.unit.year_singular", "1 an/sec");
            } else {
                String yStr = (Math.abs(yearsPerSec - Math.round(yearsPerSec)) < 0.001)
                        ? String.valueOf(Math.round(yearsPerSec))
                        : String.format(java.util.Locale.US, "%.1f", yearsPerSec);
                timeUnit = String.format(I18n.getOrDefault("sim.speed.unit.year_plural", "%s ans/sec"), yStr);
            }
        } else if (stepDays >= 28.0) {
            double monthsPerSec = simDaysPerSec / 30.0;
            if (Math.abs(monthsPerSec - 1.0) < 0.001) {
                timeUnit = I18n.getOrDefault("sim.speed.unit.month_singular", "1 mois/sec");
            } else {
                String mStr = (Math.abs(monthsPerSec - Math.round(monthsPerSec)) < 0.001)
                        ? String.valueOf(Math.round(monthsPerSec))
                        : String.format(java.util.Locale.US, "%.1f", monthsPerSec);
                timeUnit = String.format(I18n.getOrDefault("sim.speed.unit.month_plural", "%s mois/sec"), mStr);
            }
        } else {
            double daysPerSec = simDaysPerSec;
            if (Math.abs(daysPerSec - 1.0) < 0.001) {
                timeUnit = I18n.getOrDefault("sim.speed.unit.day_singular", "1 jour/sec");
            } else {
                String dStr = (Math.abs(daysPerSec - Math.round(daysPerSec)) < 0.001)
                        ? String.valueOf(Math.round(daysPerSec))
                        : String.format(java.util.Locale.US, "%.1f", daysPerSec);
                timeUnit = String.format(I18n.getOrDefault("sim.speed.unit.day_plural", "%s jours/sec"), dStr);
            }
        }

        String fmt = I18n.getOrDefault("sim.speed.target_custom_fmt", "Vitesse Cible : %s pas/sec (%s)");
        speedValueLabel.setText("⏱️ " + String.format(fmt, spdStr, timeUnit));
    }

    private void onLayerToggled(DisplayMode mode, boolean selected) {
        if (mapCanvas != null) {
            mapCanvas.setDisplayModeActive(mode, selected);
            if (colorLegend != null) colorLegend.updateFromCanvas(mapCanvas);
            updateActiveLayerUI();
        }
    }

    public void updateActiveLayerUI() {
        if (activeLayersMenuBtn == null) return;

        if (mapCanvas == null) {
            for (java.util.Map.Entry<DisplayMode, CheckBox> entry : layerCheckBoxMap.entrySet()) {
                DisplayMode dm = entry.getKey();
                entry.getValue().setSelected(dm == DisplayMode.BIOME || dm == DisplayMode.POPULATION);
            }
            activeLayersMenuBtn.setText("📊 " + I18n.getOrDefault("sim.layer.datacategory", "Couches Actives") + " (2)");
            return;
        }

        java.util.Set<DisplayMode> active = mapCanvas.getActiveDisplayModes();
        for (java.util.Map.Entry<DisplayMode, CheckBox> entry : layerCheckBoxMap.entrySet()) {
            entry.getValue().setSelected(active.contains(entry.getKey()));
        }

        int count = active.size();
        if (count == 0) {
            activeLayersMenuBtn.setText("📊 " + I18n.getOrDefault("sim.layer.none", "Aucun calque"));
        } else if (count == 1) {
            activeLayersMenuBtn.setText("📊 " + active.iterator().next().getDisplayName());
        } else {
            activeLayersMenuBtn.setText(String.format(java.util.Locale.ROOT, "📊 %s (%d %s)",
                    I18n.getOrDefault("sim.layer.datacategory", "Couches Actives"), count, I18n.getOrDefault("sim.layer.checked", "cochées")));
        }

        if (activeLayersChipsBox != null) {
            activeLayersChipsBox.getChildren().clear();
            for (DisplayMode dm : active) {
                Button chip = new Button(dm.getDisplayName() + " ✕");
                chip.setStyle("-fx-background-color: rgba(14, 165, 233, 0.18); -fx-text-fill: #38bdf8; -fx-border-color: #38bdf8; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;");
                chip.setOnAction(e -> {
                    mapCanvas.setDisplayModeActive(dm, false);
                    if (colorLegend != null) colorLegend.updateFromCanvas(mapCanvas);
                    updateActiveLayerUI();
                });
                activeLayersChipsBox.getChildren().add(chip);
            }
        }
    }

    private Button createPresetBtn(String label, String tooltip, Runnable action) {
        Button btn = new Button(label);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("button-secondary");
        btn.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 5; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void applyPresetSynthesis(CheckBox pauseOnEventCheck) {
        mode3dCheck.setSelected(true);
        reliefSlider.setDisable(false); reliefLabel.setDisable(false); autoRotateCheck.setDisable(false);
        contourCheck.setSelected(true);
        fluxVectorCheck.setSelected(false);
        resourceOverlayCheck.setSelected(false);

        if (mapCanvas != null) {
            mapCanvas.setViewMode(ViewMode.VIEW_3D);
            mapCanvas.setActiveDisplayModes(List.of(DisplayMode.BIOME));
            mapCanvas.setShowContours(true);
            mapCanvas.setShowFlowVectors(false);
            mapCanvas.setShowResourceOverlay(false);
            mapCanvas.setSmoothMap(true);
            if (colorLegend != null) colorLegend.updateFromCanvas(mapCanvas);
            updateActiveLayerUI();
        }
    }

    private void applyPresetEcon() {
        mode3dCheck.setSelected(false);
        reliefSlider.setDisable(true); reliefLabel.setDisable(true); autoRotateCheck.setDisable(true);
        contourCheck.setSelected(false);
        fluxVectorCheck.setSelected(true);
        resourceOverlayCheck.setSelected(true);

        if (mapCanvas != null) {
            mapCanvas.setViewMode(ViewMode.VIEW_2D);
            mapCanvas.setActiveDisplayModes(List.of(DisplayMode.BIOME, DisplayMode.FLUX));
            mapCanvas.setShowContours(false);
            mapCanvas.setShowFlowVectors(true);
            mapCanvas.setShowResourceOverlay(true);
            if (colorLegend != null) colorLegend.updateFromCanvas(mapCanvas);
            updateActiveLayerUI();
        }
    }

    private void applyPresetClimate() {
        mode3dCheck.setSelected(false);
        reliefSlider.setDisable(true); reliefLabel.setDisable(true); autoRotateCheck.setDisable(true);
        contourCheck.setSelected(true);
        fluxVectorCheck.setSelected(false);
        resourceOverlayCheck.setSelected(true);

        if (mapCanvas != null) {
            mapCanvas.setViewMode(ViewMode.VIEW_2D);
            mapCanvas.setActiveDisplayModes(List.of(DisplayMode.BIOME, DisplayMode.TEMPERATURE));
            mapCanvas.setShowContours(true);
            mapCanvas.setShowFlowVectors(false);
            mapCanvas.setShowResourceOverlay(true);
            if (colorLegend != null) colorLegend.updateFromCanvas(mapCanvas);
            updateActiveLayerUI();
        }
    }

    private void applyPresetCliodynamics(CheckBox pauseOnEventCheck) {
        mode3dCheck.setSelected(false);
        reliefSlider.setDisable(true); reliefLabel.setDisable(true); autoRotateCheck.setDisable(true);
        contourCheck.setSelected(false);
        fluxVectorCheck.setSelected(false);
        resourceOverlayCheck.setSelected(false);
        hexGridCheck.setSelected(true);
        if (pauseOnEventCheck != null) pauseOnEventCheck.setSelected(true);
        engine.setPauseAtNextEvent(true);

        if (mapCanvas != null) {
            mapCanvas.setViewMode(ViewMode.VIEW_2D);
            mapCanvas.setActiveDisplayModes(List.of(DisplayMode.BIOME, DisplayMode.POPULATION));
            mapCanvas.setShowContours(false);
            mapCanvas.setShowFlowVectors(false);
            mapCanvas.setShowResourceOverlay(false);
            mapCanvas.setShowHexGrid(true);
            if (colorLegend != null) colorLegend.updateFromCanvas(mapCanvas);
            updateActiveLayerUI();
        }
    }

    private Label createCardTitle(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        return label;
    }

    private void styleCard(VBox card) {
        card.getStyleClass().add("card-section");
    }

    public void setNotificationOverlay(NotificationOverlay overlay) {
        this.notificationOverlay = overlay;
    }

    public void takeHDScreenshot() {
        if (mapCanvas == null) {
            logger.warn("Cannot capture screenshot: mapCanvas is null");
            return;
        }
        try {
            WritableImage writableImage = mapCanvas.snapshot(new SnapshotParameters(), null);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);

            File dir = new File("saves/screenshots");
            if (!dir.exists()) dir.mkdirs();

            String filename = "Ether_Capture_HD_" + System.currentTimeMillis() + ".png";
            File outputFile = new File(dir, filename);
            ImageIO.write(bufferedImage, "png", outputFile);

            logger.info("Saved HD screenshot to {}", outputFile.getAbsolutePath());
            if (notificationOverlay != null) {
                notificationOverlay.showNotification("📸 Capture Photo HD Sauvegardée !\n" + outputFile.getName(), "#38bdf8");
            }
        } catch (Exception ex) {
            logger.error("Failed to take HD screenshot", ex);
            if (notificationOverlay != null) {
                notificationOverlay.showNotification("❌ Erreur de Capture Photo HD", "#ef4444");
            }
        }
    }

    private final org.ether.society.persistence.VideoExportService videoExportService = new org.ether.society.persistence.VideoExportService();

    public void toggleVideoRecording() {
        if (onTimelapseRecord != null) {
            onTimelapseRecord.run();
        }

        if (engine instanceof org.ether.society.core.H3SimulationEngine h3Engine) {
            if (notificationOverlay != null) {
                notificationOverlay.showNotification("🎬 Lancement de l'Export Vidéo MP4 (Arrière-plan)...", "#38bdf8");
            }

            String scenarioName = (mapCanvas != null && mapCanvas.getScenarioName() != null) ? mapCanvas.getScenarioName() : "Simulation";
            videoExportService.exportSimulationVideoAsync(h3Engine, scenarioName)
                    .thenAccept(outputDir -> {
                        javafx.application.Platform.runLater(() -> {
                            if (notificationOverlay != null) {
                                notificationOverlay.showNotification("✅ Export Vidéo Réussi !\nFichiers dans saves/exports/", "#10b981");
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        javafx.application.Platform.runLater(() -> {
                            if (notificationOverlay != null) {
                                notificationOverlay.showNotification("❌ Erreur Export Vidéo: " + ex.getMessage(), "#ef4444");
                            }
                        });
                        return null;
                    });
        }
    }

    public void setOnSave(Runnable onSave) { this.onSave = onSave; }
    public void setOnLoad(Runnable onLoad) { this.onLoad = onLoad; }
    public void setOnContourToggle(Consumer<Boolean> onContourToggle) { this.onContourToggle = onContourToggle; }
    public void setOnTimelapseRecord(Runnable onTimelapseRecord) { this.onTimelapseRecord = onTimelapseRecord; }
    public void setOnTimelapseSeek(Consumer<Integer> onTimelapseSeek) { this.onTimelapseSeek = onTimelapseSeek; }
    public void setOnTimelapseSeekToEnd(Runnable onTimelapseSeekToEnd) { this.onTimelapseSeekToEnd = onTimelapseSeekToEnd; }
    public void setOnStatsToggle(Consumer<Boolean> onStatsToggle) { this.onStatsToggle = onStatsToggle; }
    public void setOnFullScreen(Runnable onFullScreen) { this.onFullScreen = onFullScreen; }

    public void updateTimelapseSlider(int minYear, int maxYear, int currentYear) {
        dateHeaderLabel.setText("📅 " + I18n.getOrDefault("sim.header.date", "Date : ") + String.format(java.util.Locale.ROOT, "An %d", currentYear));
    }

    public void setMapCanvas(H3MapCanvas canvas) {
        this.mapCanvas = canvas;
        updateViewToggleButton();
        updateDisplayToggleButton();
        if (mapCanvas != null) {
            if (contourCheck != null) contourCheck.setSelected(mapCanvas.isShowContours());
            if (fluxVectorCheck != null) fluxVectorCheck.setSelected(mapCanvas.isShowFlowVectors());
            if (resourceOverlayCheck != null) resourceOverlayCheck.setSelected(mapCanvas.isShowResourceOverlay());
            if (floatingLayerCheck != null) floatingLayerCheck.setSelected(mapCanvas.isShowFloatingLayers());
            if (dateOverlayCheck != null) dateOverlayCheck.setSelected(mapCanvas.isShowCornerOverlays());
            if (cellInfoCheck != null) cellInfoCheck.setSelected(mapCanvas.isShowMouseOverInfo());
        }
    }

    public void setMiniMap(MiniMap miniMap) { this.miniMap = miniMap; }
    public void setColorLegend(ColorLegend legend) { 
        this.colorLegend = legend; 
        if (legendCheck != null && colorLegend != null) {
            legendCheck.setSelected(colorLegend.isVisible());
        }
    }

    private void setupRepeatAction(Button btn, Runnable action) {
        javafx.animation.Timeline repeatTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(80), e -> action.run())
        );
        repeatTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);

        javafx.animation.PauseTransition initialDelay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(350));
        initialDelay.setOnFinished(e -> repeatTimeline.playFromStart());

        btn.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                action.run();
                initialDelay.playFromStart();
            }
        });

        Runnable stopRepeat = () -> {
            initialDelay.stop();
            repeatTimeline.stop();
        };

        btn.setOnMouseReleased(e -> stopRepeat.run());
        btn.setOnMouseExited(e -> stopRepeat.run());
    }

    public void updateScenarioName(String scenarioName) {
        if (scenarioName != null && !scenarioName.isBlank()) {
            scenarioHeaderLabel.setText(I18n.getOrDefault("sim.header.scenario", "🎬 Scenario: ") + scenarioName);
        }
    }

    public void updateYear(String year) {
        dateHeaderLabel.setText("📅 " + I18n.getOrDefault("sim.header.date", "Date : ") + year);
    }

    public void updateStats(long population, double food, long populatedCells, double tps) {
        popStatValue.setText(String.format(I18n.getOrDefault("sim.status.pop_total", "Pop. Totale : %s"), formatNumber(population)));
        foodStatValue.setText(String.format(I18n.getOrDefault("sim.status.food_stocks", "Stocks Alim. : %s"), formatNumber((long) food)));
        cellStatValue.setText(String.format(I18n.getOrDefault("sim.status.populated_cells", "Populated Cells: %,d"), populatedCells));
    }

    private String formatNumber(long num) {
        if (num >= 1_000_000_000L) {
            return String.format("%.2f Md", num / 1_000_000_000.0);
        } else if (num >= 1_000_000L) {
            return String.format("%.2f M", num / 1_000_000.0);
        } else if (num >= 1_000L) {
            return String.format("%.1f k", num / 1_000.0);
        }
        return String.valueOf(num);
    }

    public void updateSeason(int month) {
        String[] seasonNames = {
            I18n.getOrDefault("sim.season.winter", "Hiver ❄️"),
            I18n.getOrDefault("sim.season.spring", "Printemps 🌿"),
            I18n.getOrDefault("sim.season.summer", "Été ☀️"),
            I18n.getOrDefault("sim.season.autumn", "Automne 🍂")
        };
        String[] seasonColors = { "#64b5f6", "#4ade80", "#facc15", "#fb923c" };

        int seasonIndex;
        if (month == 11 || month == 0 || month == 1) seasonIndex = 0;
        else if (month >= 2 && month <= 4) seasonIndex = 1;
        else if (month >= 5 && month <= 7) seasonIndex = 2;
        else seasonIndex = 3;

        seasonLabel.setText(I18n.getOrDefault("sim.status.season_prefix", "Season: ") + seasonNames[seasonIndex]);
        seasonLabel.setStyle("-fx-text-fill: " + seasonColors[seasonIndex] + "; -fx-font-weight: bold;");
    }

    public void logEvents(List<String> events) {
        if (events == null || events.isEmpty()) return;
        eventHistory.addAll(events);
    }

    public void updateRecentEvents(List<ActiveEvent> events) {
        if (events == null || eventsListBox == null) return;

        // Filter and get last 10 events sorted chronologically (by year, then month, then day)
        List<ActiveEvent> sortedList = new ArrayList<>(events);
        sortedList.sort(Comparator.comparingInt(ActiveEvent::getYear)
                .thenComparingInt(ActiveEvent::getMonth)
                .thenComparingInt(ActiveEvent::getDay));

        if (sortedList.size() > 10) {
            sortedList = sortedList.subList(sortedList.size() - 10, sortedList.size());
        }

        eventsListBox.getChildren().clear();
        if (sortedList.isEmpty()) {
            eventsListBox.getChildren().add(noEventsLabel);
            return;
        }

        for (ActiveEvent evt : sortedList) {
            VBox itemCard = createEventItemNode(evt);
            eventsListBox.getChildren().add(itemCard);
        }
    }

    private VBox createEventItemNode(ActiveEvent evt) {
        VBox card = new VBox(4);
        String normalStyle = "-fx-background-color: rgba(30, 41, 59, 0.75); -fx-border-color: rgba(56, 189, 248, 0.25); -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 6 8; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: rgba(51, 65, 85, 0.90); -fx-border-color: #38bdf8; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 6 8; -fx-cursor: hand;";
        card.setStyle(normalStyle);

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(normalStyle));

        // Header row: Title (left) + Date (right)
        Label titleLabel = new Label(evt.getTitle());
        titleLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 11px;");
        titleLabel.setMaxWidth(260);
        titleLabel.setEllipsisString("...");

        Label dateLabel = new Label(evt.getFormattedDate());
        dateLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 10px;");

        Region spacerTop = new Region();
        HBox.setHgrow(spacerTop, Priority.ALWAYS);
        HBox topRow = new HBox(6, titleLabel, spacerTop, dateLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Details row: Magnitude/Intensity (left) + Coordinates (right)
        String magColor = evt.getIntensityBadgeColor();
        Label magLabel = new Label(String.format(java.util.Locale.ROOT, "⚡ Mag: %.1f (%s)", evt.getMagnitude(), evt.getIntensityLabel()));
        magLabel.setStyle("-fx-text-fill: " + magColor + "; -fx-font-weight: bold; -fx-font-size: 10px;");

        Label coordLabel = new Label("📍 " + evt.getFormattedCoordinates());
        coordLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");

        Region spacerBottom = new Region();
        HBox.setHgrow(spacerBottom, Priority.ALWAYS);
        HBox bottomRow = new HBox(6, magLabel, spacerBottom, coordLabel);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(topRow, bottomRow);

        // Tooltip with complete event details
        Tooltip tooltip = new Tooltip(String.format(
            "%s\n\n📅 Date : %s\n⚡ Magnitude : %.1f (%s)\n📍 Coordonnées : %s\n\n💡 Double-cliquer pour centrer la vue 3D / 2D sur cet événement.",
            evt.getTitle(), evt.getFormattedDate(), evt.getMagnitude(), evt.getIntensityLabel(), evt.getFormattedCoordinates()
        ));
        tooltip.setShowDelay(javafx.util.Duration.millis(150));
        Tooltip.install(card, tooltip);

        // Double click navigates / flies camera to this event's coordinates and pings the location
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                if (mapCanvas != null) {
                    mapCanvas.flyTo(evt.getLatitude(), evt.getLongitude());
                    mapCanvas.pingLocation(evt.getLatitude(), evt.getLongitude(), evt.getTitle(), evt.getType(), evt.getMagnitude());
                    if (notificationOverlay != null) {
                        notificationOverlay.showNotification("🎯 Centrage sur l'événement :\n" + evt.getTitle(), "#38bdf8");
                    }
                }
            }
        });

        return card;
    }

    public void updateAge(String ageName) {
        ageLabel.setText(I18n.getOrDefault("sim.status.age_prefix", "Age: ") + ageName);
    }

    private boolean isDbOnline = false;
    private String clusterNodeState = "💻 Nœud Local Standalone";

    public void updateDatabaseStatus(boolean online) {
        this.isDbOnline = online;
        refreshStatusBadge();
    }

    public void updateClusterStatus(String nodeRole, String hostPort, boolean isConnected) {
        if (isConnected) {
            this.clusterNodeState = String.format("🌐 %s (%s)", nodeRole, hostPort);
        } else {
            this.clusterNodeState = "💻 Nœud Local Standalone";
        }
        refreshStatusBadge();
    }

    private void refreshStatusBadge() {
        if (dbStatusLabel == null) return;
        String dbStr = isDbOnline ? "🟢 BDD Supabase : En Ligne" : "⚪ BDD : Mode Hors Ligne (Fichiers Locaux)";
        dbStatusLabel.setText(dbStr + " | " + clusterNodeState);
        dbStatusLabel.setStyle(isDbOnline ? "-fx-font-size: 11px; -fx-text-fill: #4ade80; -fx-font-weight: bold;" : "-fx-font-size: 11px; -fx-text-fill: #38bdf8;");
    }

    private void updateTexts() {
        if (playPauseBtn != null) {
            boolean running = engine != null && engine.isRunning();
            playPauseBtn.setText(running ? "⏸" : "▶");
            playPauseBtn.setTooltip(new Tooltip(running
                    ? I18n.getOrDefault("sim.tooltip.pause", "Mettre en pause")
                    : I18n.getOrDefault("sim.tooltip.start", "Lancer / Reprendre")));
        }
        rewindBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.rewind", "Réinitialiser T=0")));
        fastRewindBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastrewind", "Reculer de 1000 pas [Maintenir appuyé]")));
        stepBackBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stepback", "Reculer de 10 pas [Maintenir appuyé]")));
        stepForwardBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stepforward", "Avancer de 10 pas [Maintenir appuyé]")));
        fastForwardBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastforward", "Avancer de 1000 pas [Maintenir appuyé]")));
        endBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastforward_end", "Aller à la fin de la simulation (Dernier checkpoint / Fin)")));
        speedMax.setText("MAX 🚀");
        speedMax.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.speed_max", "Calcule les itérations à la vitesse maximale du processeur")));
        speedSlider.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.speed_slider", "Vitesse de simulation CPU (0.1 à 20 pas/sec ou MAX)")));
        updateSpeedLabel(speedMax != null && speedMax.isSelected() ? 999.0 : (speedSlider != null ? speedSlider.getValue() : 1.0));
        mode3dCheck.setText(I18n.getOrDefault("sim.layer.mode3d", "🌐 Globe 3D H3"));
        mode3dCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.mode3d", "Toggles between 3D spherical globe and 2D flat map")));
        reliefLabel.setText(String.format(java.util.Locale.ROOT, "%s : %.0fx", I18n.getOrDefault("sim.layer.relief3d", "⛰️ Relief 3D"), reliefSlider != null ? reliefSlider.getValue() : 25.0));
        reliefSlider.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.relief3d", "Ajuste la hauteur du relief topographique en mode Globe 3D")));
        autoRotateCheck.setText(I18n.getOrDefault("sim.layer.autorotate", "🔄 Auto-rotation Globe"));
        autoRotateCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.autorotate", "Auto-rotates 3D spherical globe")));
        if (hillshadingCheck != null) {
            hillshadingCheck.setText(I18n.getOrDefault("sim.layer.hillshading", "⛰️ Ombrage Relief Topographique"));
            hillshadingCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.hillshading", "Applique un ombrage topographique lambertien selon la pente du relief")));
        }
        if (solarTerminatorCheck != null) {
            solarTerminatorCheck.setText(I18n.getOrDefault("sim.layer.solarterminator", "☀️ Terminateur Solaire Jour/Nuit"));
            solarTerminatorCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.solarterminator", "Displays night and twilight shadow overlay")));
        }
        if (smoothMapCheck != null) {
            smoothMapCheck.setText(I18n.getOrDefault("sim.layer.smoothmap", "🎨 Smooth Map (Continu / Sans Pavage)"));
            smoothMapCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.smoothmap", "Affiche une carte continue fondue sans démarcations hexagonales")));
        }
        contourCheck.setText(I18n.getOrDefault("sim.layer.contours", "📈 Courbes de Niveau (Isolines)"));
        contourCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.contours", "Displays elevation contour lines on H3 cells")));
        fluxVectorCheck.setText(I18n.getOrDefault("sim.layer.fluxvectors", "🌊 Flux & Transports (Vecteurs)"));
        fluxVectorCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fluxvectors", "Overlays material flux and population transport vectors")));
        resourceOverlayCheck.setText(I18n.getOrDefault("sim.layer.resources", "💎 Deposits & Capital (Overlays)"));
        resourceOverlayCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.resources", "Displays metal, aquifer, and infrastructure markers")));
        if (floatingLayerCheck != null) {
            floatingLayerCheck.setText(I18n.getOrDefault("sim.layer.floating", "☁️ Calques Flottants 2.5D (Altitude)"));
            floatingLayerCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.floating", "Projette les couches thématiques en élévation 2.5D flottant au-dessus du relief naturel des biomes")));
        }
        legendCheck.setText(I18n.getOrDefault("sim.layer.legend", "🗺️ Légende des Couleurs (Overlay)"));
        legendCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.legend", "Affiche ou masque le panneau de légende des couleurs")));
        dateOverlayCheck.setText(I18n.getOrDefault("sim.layer.date_overlay", "📅 Incrustation Date & Scénario"));
        dateOverlayCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.date_overlay", "Affiche ou masque les badges d'incrustation temporelle sur la carte")));
        cellInfoCheck.setText(I18n.getOrDefault("sim.layer.cell_info", "ℹ️ Infos Cellule au Survol (Infobulle)"));
        cellInfoCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.cell_info", "Active ou désactive l'infobulle d'inspection au survol des hexagones")));
        hexGridCheck.setText(I18n.getOrDefault("sim.layer.hexgrid", "⬡ H3 Hexagon Borders"));
        hexGridCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.hexgrid", "Shows or hides H3 hexagon grid (smooth view without borders vs grid view)")));
        if (fullScreenBtn != null) {
            fullScreenBtn.setText(I18n.getOrDefault("sim.btn.fullscreen", "🖥️ Plein Écran (Carte)"));
            fullScreenBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fullscreen", "Affiche la carte en plein écran sans aucun autre élément (Touche Échap pour quitter)")));
        }
        if (hdScreenshotBtn != null) {
            hdScreenshotBtn.setText("📸 " + I18n.getOrDefault("sim.btn.screenshot", "Capture Photo HD"));
            hdScreenshotBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.screenshot", "Exporte un instantané PNG haute définition dans saves/screenshots/")));
        }
        if (recordVideoBtn != null) {
            recordVideoBtn.setText("🎥 " + I18n.getOrDefault("sim.btn.video", "Enregistrer Vidéo MP4"));
            recordVideoBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.video", "Démarre l'export vidéo MP4 (1:1 pas) dans saves/timelapse/")));
        }
        if (autoRecordCheck != null) {
            autoRecordCheck.setText(I18n.getOrDefault("sim.option.auto_record", "🎬 Synchronisation Vidéo Auto (Start & Pause)"));
            autoRecordCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.auto_record", "Démarre et suspend automatiquement la capture vidéo en synchronisation avec la simulation")));
        }
        if (eventsTitleLabel != null) {
            eventsTitleLabel.setText(I18n.getOrDefault("sim.card.recent_events", "📜 5. ÉVÉNEMENTS RÉCENTS (CHRONOLOGIE)"));
        }
        if (eventsHintLabel != null) {
            eventsHintLabel.setText(I18n.getOrDefault("sim.events.double_click_hint", "💡 Double-cliquer sur un événement pour voler vers sa position"));
        }
        if (noEventsLabel != null) {
            noEventsLabel.setText(I18n.getOrDefault("sim.events.no_events", "Aucun événement enregistré pour le moment."));
        }
        updateViewToggleButton();
        updateDisplayToggleButton();
    }

    private void updateViewToggleButton() {
        if (mapCanvas != null && mode3dCheck != null) {
            boolean is3D = mapCanvas.getViewMode() == ViewMode.VIEW_3D;
            mode3dCheck.setSelected(is3D);
            if (reliefSlider != null) reliefSlider.setDisable(!is3D);
            if (reliefLabel != null) reliefLabel.setDisable(!is3D);
            if (autoRotateCheck != null) autoRotateCheck.setDisable(!is3D);
            if (hillshadingCheck != null) hillshadingCheck.setDisable(!is3D);
            if (solarTerminatorCheck != null) solarTerminatorCheck.setDisable(!is3D);
            if (hexGridCheck != null) hexGridCheck.setSelected(mapCanvas.isShowHexGrid());
            if (smoothMapCheck != null) smoothMapCheck.setSelected(mapCanvas.isSmoothMap());
        }
    }

    private void updateDisplayToggleButton() {
        updateActiveLayerUI();
    }
}
