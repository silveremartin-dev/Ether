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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
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
    private final Label tpsLabel;
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
    private final Label eventLabel;
    private final List<String> eventHistory = new ArrayList<>();

    // Controls
    private final Button startBtn;
    private final Button pauseBtn;
    private final Button stopBtn;
    private final Button rewindBtn;
    private final Button fastRewindBtn;
    private final Button stepBackBtn;
    private final Button stepForwardBtn;
    private final Button fastForwardBtn;

    private final Button speedMax;
    private final Slider speedSlider;

    private final Button hdScreenshotBtn;
    private final Button recordVideoBtn;
    private final CheckBox autoRecordCheck;
    private boolean isRecordingVideo = false;

    private final CheckBox mode3dCheck;
    private final CheckBox autoRotateCheck;
    private final Label reliefLabel;
    private final Slider reliefSlider;
    private final CheckBox contourCheck;
    private final CheckBox fluxVectorCheck;
    private final CheckBox resourceOverlayCheck;
    private final CheckBox hexGridCheck;
    private final ComboBox<DisplayMode> displayModeCombo;

    // Callbacks
    private Runnable onSave;
    private Runnable onLoad;
    private Consumer<Boolean> onContourToggle;
    private Runnable onTimelapseRecord;
    private Consumer<Integer> onTimelapseSeek;
    private Consumer<Boolean> onStatsToggle;

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

        dateHeaderLabel = new Label("📅 " + I18n.getOrDefault("sim.header.date", "Date & Heure : ") + "An -20000, Mois 1, Jour 1");
        dateHeaderLabel.getStyleClass().add("sidebar-title");
        dateHeaderLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        dateHeaderLabel.setTooltip(new Tooltip(
            I18n.getOrDefault("sim.tooltip.date_header",
            "⏱️ Horloge et Pas de Simulation (Temporal Resolution) :\n" +
            "• An, Mois, Jour : Horloge courante de la simulation.\n" +
            "• Échelle Rapide (Daily Tick) : Avance d'1 jour à chaque itération (dt = 86,400s).\n" +
            "• Échelle Lente (Monthly Cycle) : Réconciliation physique & économique tous les 30 jours.")
        ));

        tpsLabel = new Label("⏱️ " + I18n.getOrDefault("sim.header.tps", "Speed: 0.0 iter/sec (Fast scale 1d / slow 30d)"));
        tpsLabel.getStyleClass().add("card-description-muted");
        tpsLabel.setStyle("-fx-font-size: 11px;");
        tpsLabel.setTooltip(new Tooltip(
            I18n.getOrDefault("sim.tooltip.tps_header",
            "⚙️ Comparatif des Échelles Temporelles (Fast vs Slow Scale) :\n\n" +
            "• Échelle Rapide (1 tick = 1 jour) :\n" +
            "  Calcule les flux matériels, transports physiques et extensions politiques (O(1)).\n\n" +
            "• Échelle Lente (1 cycle = 30 jours) :\n" +
            "  Calcule le climat, la démographie, les nutriments NPK, l'inertie d'infrastructures et l'entropie.\n\n" +
            "💡 Passage de l'Échelle Lente à 1 Jour (Équivalence Rapide/Lente) :\n" +
            "  - Avantage : Précision numérique maximale, élimination des dérives d'intégration (comme l'overflow de capital à 88%).\n" +
            "  - Inconvénient : Charge CPU multipliée par 30.")
        ));

        VBox dateHeaderBox = new VBox(4, scenarioHeaderLabel, dateHeaderLabel, tpsLabel);
        styleCard(dateHeaderBox);

        // --- 2. TEMPORAL & PLAYBACK CONTROLS CARD ---
        Label timeTitle = createCardTitle("⏱️ " + I18n.getOrDefault("sim.card.time", "TIME & PLAYBACK CONTROLS"));

        rewindBtn = new Button("|<<");
        rewindBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.rewind", "Reset T=0")));
        rewindBtn.setOnAction(e -> {
            engine.pause();
            if (onTimelapseSeek != null) onTimelapseSeek.accept(0);
        });

        fastRewindBtn = new Button("<<");
        fastRewindBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastrewind", "Reculer d'un an (-12 mois)")));
        fastRewindBtn.setOnAction(e -> engine.stepBackward(12));

        stepBackBtn = new Button("|<");
        stepBackBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stepback", "Reculer d'une frame / tick (-1 mois)")));
        stepBackBtn.setOnAction(e -> engine.stepBackward(1));

        // Auto Record Checkbox (initialized early for button handlers)
        autoRecordCheck = new CheckBox(I18n.getOrDefault("sim.option.auto_record", "🎬 Auto Sync Video (Start & Pause)"));
        autoRecordCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.auto_record", "Automatically starts/stops 1:1 video recording in sync with scenario start/pause")));
        autoRecordCheck.getStyleClass().add("opt-sub-checkbox");
        autoRecordCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");

        startBtn = new Button("▶");
        startBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.start", "Lancer / Reprendre")));
        startBtn.setOnAction(e -> {
            if (autoRecordCheck.isSelected() && !isRecordingVideo) {
                toggleVideoRecording();
            }
            engine.start();
            updatePlayPauseVisuals(true);
        });

        pauseBtn = new Button(I18n.getOrDefault("sim.btn.paused", "⏸ EN PAUSE"));
        pauseBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.pause", "Mettre en pause")));
        pauseBtn.setOnAction(e -> {
            engine.pause();
            if (autoRecordCheck != null && autoRecordCheck.isSelected() && isRecordingVideo) {
                toggleVideoRecording();
            }
            updatePlayPauseVisuals(false);
        });

        stopBtn = new Button("⏹");
        stopBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stop", "Stop")));
        stopBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;");
        stopBtn.setOnAction(e -> {
            engine.pause();
            if (autoRecordCheck != null && autoRecordCheck.isSelected() && isRecordingVideo) {
                toggleVideoRecording();
            }
            updatePlayPauseVisuals(false);
        });

        updatePlayPauseVisuals(engine.isRunning());

        stepForwardBtn = new Button(">|");
        stepForwardBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stepforward", "Avancer d'une frame / tick (+1 mois)")));
        stepForwardBtn.setOnAction(e -> engine.stepForward(1));

        fastForwardBtn = new Button(">>");
        fastForwardBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastforward", "Avancer d'un an (+12 mois)")));
        fastForwardBtn.setOnAction(e -> engine.stepForward(12));

        CheckBox pauseOnEventCheck = new CheckBox(I18n.getOrDefault("sim.option.pause_on_event", "⏸️ Auto-pause on event"));
        pauseOnEventCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.pause_on_event", "Automatically pauses simulation when a new planetary or regional event occurs")));
        pauseOnEventCheck.getStyleClass().add("opt-sub-checkbox");
        pauseOnEventCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        pauseOnEventCheck.setOnAction(e -> engine.setPauseAtNextEvent(pauseOnEventCheck.isSelected()));

        HBox playBar = new HBox(4, rewindBtn, fastRewindBtn, stepBackBtn, startBtn, pauseBtn, stopBtn, stepForwardBtn, fastForwardBtn);
        playBar.setAlignment(Pos.CENTER);

        ComboBox<org.ether.society.core.H3SimulationEngine.TemporalScale> temporalScaleCombo = new ComboBox<>();
        temporalScaleCombo.getItems().addAll(org.ether.society.core.H3SimulationEngine.TemporalScale.values());
        temporalScaleCombo.setValue(engine.getTemporalScale());
        temporalScaleCombo.setMaxWidth(Double.MAX_VALUE);
        temporalScaleCombo.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.temporal_scale", "Select temporal resolution: 1 tick/day (flux accuracy) vs 1 tick/month (macro speed x30)")));
        temporalScaleCombo.setOnAction(e -> {
            if (temporalScaleCombo.getValue() != null) {
                engine.setTemporalScale(temporalScaleCombo.getValue());
            }
        });

        speedSlider = new Slider(1, 100, 1);
        speedSlider.setBlockIncrement(5);
        speedSlider.setMajorTickUnit(25);
        speedSlider.setMinorTickCount(4);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setSnapToTicks(true);
        speedSlider.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.speed_slider", "Vitesse de simulation CPU (Gauche = 1 tick/sec soit 1 jour/sec | Droite = Mode Rapide)")));
        HBox.setHgrow(speedSlider, Priority.ALWAYS);

        Label speedValueLabel = new Label("⏱️ " + I18n.getOrDefault("sim.speed.label", "Vitesse : 1 tick/sec (1 jour/sec)"));
        speedValueLabel.getStyleClass().add("value-label");
        speedValueLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        speedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int spd = newV.intValue();
            engine.setSpeed(spd);
            speedValueLabel.setText(String.format("⏱️ " + I18n.getOrDefault("sim.speed.target_fmt", "Vitesse Cible : %d ticks/sec (%d jours/sec)"), spd, spd));
        });

        speedMax = new Button("MAX 🚀");
        speedMax.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.speed_max", "Calculates ticks at maximum CPU speed without limits (Uncapped CPU ticks/sec)")));
        speedMax.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4;");
        speedMax.setOnAction(e -> {
            engine.setSpeed(999);
            speedValueLabel.setText("⏱️ " + I18n.getOrDefault("sim.speed.max_label", "Target Speed: MAX 🚀 (Unlimited - As many CPU ticks/sec as possible)"));
        });

        HBox sliderRow = new HBox(8, speedSlider, speedMax);
        // --- 1. HORLOGE & CONTRÔLE TEMPOREL CARD ---
        VBox timeCard = new VBox(8, scenarioHeaderLabel, dateHeaderLabel, tpsLabel, timeTitle, playBar, temporalScaleCombo, pauseOnEventCheck, speedValueLabel, sliderRow);
        styleCard(timeCard);

        // --- 3. MEDIA & EXPORT MP4 CARD ---
        Label mediaTitle = createCardTitle("📸 " + I18n.getOrDefault("sim.card.media", "SCREENSHOTS & VIDEO"));

        hdScreenshotBtn = new Button("📸 " + I18n.getOrDefault("sim.btn.screenshot", "Capture Photo HD"));
        hdScreenshotBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.screenshot", "Exports HD PNG screenshot to saves/screenshots/")));
        hdScreenshotBtn.setMaxWidth(Double.MAX_VALUE);
        hdScreenshotBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 10; -fx-background-radius: 6;");
        hdScreenshotBtn.setOnAction(e -> takeHDScreenshot());

        recordVideoBtn = new Button("🎥 " + I18n.getOrDefault("sim.btn.video", "Record MP4 Video"));
        recordVideoBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.video", "Starts MP4 video capture (1:1 tick) in saves/timelapse/")));
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

        Label layerComboLabel = new Label(I18n.getOrDefault("sim.layer.datacategory", "Active Data Layer:"));
        layerComboLabel.getStyleClass().add("card-description-muted");
        layerComboLabel.setStyle("-fx-font-size: 11px;");

        displayModeCombo = new ComboBox<>();
        List<DisplayMode> sortedModes = java.util.Arrays.stream(DisplayMode.values())
                .sorted(java.util.Comparator.comparing(DisplayMode::getCategory).thenComparing(DisplayMode::ordinal))
                .toList();
        displayModeCombo.getItems().addAll(sortedModes);
        displayModeCombo.setValue(DisplayMode.BIOME);
        displayModeCombo.setMaxWidth(Double.MAX_VALUE);
        displayModeCombo.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.datacategory", "Toggles main color layer on map")));

        displayModeCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(DisplayMode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String catName = item.getCategory().getCategoryName();
                    String catPrefix = catName.contains(" ") ? catName.split(" ")[1] : catName;
                    setText("[" + catPrefix + "] " + item.getDisplayName());
                }
            }
        });
        if (displayModeCombo.getCellFactory() != null) {
            displayModeCombo.setButtonCell(displayModeCombo.getCellFactory().call(null));
        }

        displayModeCombo.setOnAction(e -> {
            if (mapCanvas != null && displayModeCombo.getValue() != null) {
                mapCanvas.setDisplayMode(displayModeCombo.getValue());
                if (colorLegend != null) colorLegend.updateFromCanvas(mapCanvas);
            }
        });

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

        Label stackTitleLabel = new Label(I18n.getOrDefault("sim.layer.overlaystack", "Layer Stack (Multi-Select):"));
        stackTitleLabel.getStyleClass().add("opt-subheader");
        stackTitleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        VBox overlayStackBox = new VBox(4, stackTitleLabel, contourCheck, fluxVectorCheck, resourceOverlayCheck);
        overlayStackBox.getStyleClass().add("subcard-section");

        VBox layersCard = new VBox(8, layersTitle, presetBox, layerComboLabel, displayModeCombo, overlayStackBox);
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

        mode3dCheck.setOnAction(e -> {
            boolean is3D = mode3dCheck.isSelected();
            if (mapCanvas != null) {
                mapCanvas.setViewMode(is3D ? ViewMode.VIEW_3D : ViewMode.VIEW_2D);
            }
            reliefSlider.setDisable(!is3D);
            reliefLabel.setDisable(!is3D);
            autoRotateCheck.setDisable(!is3D);
        });

        hexGridCheck = new CheckBox(I18n.getOrDefault("sim.layer.hexgrid", "⬡ H3 Hexagon Borders"));
        hexGridCheck.setSelected(true);
        hexGridCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.hexgrid", "Shows or hides H3 hexagon grid (smooth view without borders vs grid view)")));
        hexGridCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        hexGridCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setShowHexGrid(hexGridCheck.isSelected());
            }
        });

        CheckBox smoothMapCheck = new CheckBox(I18n.getOrDefault("sim.layer.smoothmap", "🎨 Smooth Map (Continuous Heatmap)"));
        smoothMapCheck.setSelected(true);
        smoothMapCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.smoothmap", "Displays continuous map with smooth gradients instead of individual hexagons")));
        smoothMapCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        smoothMapCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setSmoothMap(smoothMapCheck.isSelected());
            }
        });

        Label paletteLabel = new Label(I18n.getOrDefault("sim.render.palette", "🎨 Palette Scientifique :"));
        paletteLabel.getStyleClass().add("control-label");

        ComboBox<ScientificColorMap> paletteCombo = new ComboBox<>();
        paletteCombo.getItems().addAll(ScientificColorMap.values());
        paletteCombo.setValue(ScientificColorMap.TURBO);
        paletteCombo.setMaxWidth(Double.MAX_VALUE);
        paletteCombo.setStyle("-fx-font-size: 11px;");
        paletteCombo.setOnAction(e -> {
            if (mapCanvas != null && paletteCombo.getValue() != null) {
                mapCanvas.setScientificColorMap(paletteCombo.getValue());
            }
        });

        CheckBox hillshadingCheck = new CheckBox(I18n.getOrDefault("sim.layer.hillshading", "⛰️ Hillshading Relief Topographique"));
        hillshadingCheck.setSelected(false);
        hillshadingCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.hillshading", "Applique un ombrage topographique lambertien selon la pente du relief")));
        hillshadingCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        hillshadingCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setShowHillshading(hillshadingCheck.isSelected());
            }
        });

        CheckBox solarTerminatorCheck = new CheckBox(I18n.getOrDefault("sim.layer.solarterminator", "☀️ Terminateur Solaire Jour/Nuit"));
        solarTerminatorCheck.setSelected(false);
        solarTerminatorCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.solarterminator", "Displays night and twilight shadow overlay")));
        solarTerminatorCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        solarTerminatorCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setShowSolarTerminator(solarTerminatorCheck.isSelected());
            }
        });

        CheckBox lodCheck = new CheckBox(I18n.getOrDefault("sim.layer.lod", "📐 H3 LOD Pyramid Aggregation"));
        lodCheck.setSelected(true);
        lodCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.lod", "Automatically aggregates cells to H3 parents when zooming out")));
        lodCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        lodCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setEnableHierarchicalLOD(lodCheck.isSelected());
            }
        });

        VBox renderCard = new VBox(8, renderTitle, mode3dCheck, reliefLabel, reliefSlider, autoRotateCheck, smoothMapCheck, hexGridCheck, lodCheck, paletteLabel, paletteCombo, hillshadingCheck, solarTerminatorCheck);
        styleCard(renderCard);

        // --- 4. TÉLÉMÉTRIE, CAPTURES & EXPORT CARD ---
        Label exportTitle = createCardTitle(I18n.getOrDefault("sim.card.telemetry", "📊 4. TELEMETRY, SCREENSHOTS & EXPORTS"));

        dbStatusLabel = new Label(I18n.getOrDefault("sim.status.dbcheck", "DB: Checking..."));
        dbStatusLabel.getStyleClass().add("control-label");

        eventLabel = new Label("");
        eventLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 11px;");
        eventLabel.setOnMouseClicked(e -> parseAndCenterEvent(eventLabel.getText()));

        VBox exportCard = new VBox(8, exportTitle, hdScreenshotBtn, recordVideoBtn, autoRecordCheck, dbStatusLabel, eventLabel);
        styleCard(exportCard);

        // Keep age and season labels initialized for dateHeaderBox updates
        popStatValue = new Label();
        foodStatValue = new Label();
        cellStatValue = new Label();
        ageLabel = new Label(I18n.getOrDefault("sim.age.stone_age", "Age: Stone Age"));
        ageLabel.getStyleClass().add("control-label");
        seasonLabel = new Label(I18n.getOrDefault("sim.season.spring", "Season: Spring"));
        seasonLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");

        // Combine cards cleanly into 4 modular accordion-style sections
        getChildren().addAll(timeCard, layersCard, renderCard, exportCard);

        updateTexts();
        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    public void updatePlayPauseVisuals(boolean isRunning) {
        if (startBtn == null || pauseBtn == null) return;
        if (isRunning) {
            startBtn.setText(I18n.getOrDefault("sim.btn.running", "▶ EN COURS"));
            startBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-border-color: #34d399; -fx-border-width: 1.5px; -fx-background-radius: 6; -fx-border-radius: 6; -fx-effect: dropshadow(three-pass-box, rgba(16,185,129,0.7), 8, 0, 0, 0);");
            pauseBtn.setText("⏸");
            pauseBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-background-radius: 6; -fx-effect: none;");
        } else {
            startBtn.setText("▶");
            startBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-background-radius: 6; -fx-effect: none;");
            pauseBtn.setText(I18n.getOrDefault("sim.btn.paused", "⏸ EN PAUSE"));
            pauseBtn.setStyle("-fx-background-color: #d97706; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-border-color: #fbbf24; -fx-border-width: 1.5px; -fx-background-radius: 6; -fx-border-radius: 6; -fx-effect: dropshadow(three-pass-box, rgba(245,158,11,0.7), 8, 0, 0, 0);");
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
        displayModeCombo.setValue(DisplayMode.BIOME);
        contourCheck.setSelected(true);
        fluxVectorCheck.setSelected(false);
        resourceOverlayCheck.setSelected(false);

        if (mapCanvas != null) {
            mapCanvas.setViewMode(ViewMode.VIEW_3D);
            mapCanvas.setDisplayMode(DisplayMode.BIOME);
            mapCanvas.setShowContours(true);
            mapCanvas.setShowFlowVectors(false);
            mapCanvas.setShowResourceOverlay(false);
            mapCanvas.setSmoothMap(true);
            if (colorLegend != null) colorLegend.updateFromCanvas(mapCanvas);
        }
    }

    private void applyPresetEcon() {
        mode3dCheck.setSelected(false);
        reliefSlider.setDisable(true); reliefLabel.setDisable(true); autoRotateCheck.setDisable(true);
        displayModeCombo.setValue(DisplayMode.FLUX);
        contourCheck.setSelected(false);
        fluxVectorCheck.setSelected(true);
        resourceOverlayCheck.setSelected(true);

        if (mapCanvas != null) {
            mapCanvas.setViewMode(ViewMode.VIEW_2D);
            mapCanvas.setDisplayMode(DisplayMode.FLUX);
            mapCanvas.setShowContours(false);
            mapCanvas.setShowFlowVectors(true);
            mapCanvas.setShowResourceOverlay(true);
            if (colorLegend != null) colorLegend.updateFromCanvas(mapCanvas);
        }
    }

    private void applyPresetClimate() {
        mode3dCheck.setSelected(false);
        reliefSlider.setDisable(true); reliefLabel.setDisable(true); autoRotateCheck.setDisable(true);
        displayModeCombo.setValue(DisplayMode.TEMPERATURE);
        contourCheck.setSelected(true);
        fluxVectorCheck.setSelected(false);
        resourceOverlayCheck.setSelected(true);

        if (mapCanvas != null) {
            mapCanvas.setViewMode(ViewMode.VIEW_2D);
            mapCanvas.setDisplayMode(DisplayMode.TEMPERATURE);
            mapCanvas.setShowContours(true);
            mapCanvas.setShowFlowVectors(false);
            mapCanvas.setShowResourceOverlay(true);
            if (colorLegend != null) colorLegend.updateFromCanvas(mapCanvas);
        }
    }

    private void applyPresetCliodynamics(CheckBox pauseOnEventCheck) {
        mode3dCheck.setSelected(false);
        reliefSlider.setDisable(true); reliefLabel.setDisable(true); autoRotateCheck.setDisable(true);
        displayModeCombo.setValue(DisplayMode.POPULATION);
        contourCheck.setSelected(false);
        fluxVectorCheck.setSelected(false);
        resourceOverlayCheck.setSelected(false);
        hexGridCheck.setSelected(true);
        if (pauseOnEventCheck != null) pauseOnEventCheck.setSelected(true);
        engine.setPauseAtNextEvent(true);

        if (mapCanvas != null) {
            mapCanvas.setViewMode(ViewMode.VIEW_2D);
            mapCanvas.setDisplayMode(DisplayMode.POPULATION);
            mapCanvas.setShowContours(false);
            mapCanvas.setShowFlowVectors(false);
            mapCanvas.setShowResourceOverlay(false);
            mapCanvas.setShowHexGrid(true);
            if (colorLegend != null) colorLegend.updateFromCanvas(mapCanvas);
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
    public void setOnStatsToggle(Consumer<Boolean> onStatsToggle) { this.onStatsToggle = onStatsToggle; }

    public void updateTimelapseSlider(int minYear, int maxYear, int currentYear) {
        dateHeaderLabel.setText(String.format("📅 Date & Heure : An %d", currentYear));
    }

    public void setMapCanvas(H3MapCanvas canvas) {
        this.mapCanvas = canvas;
        updateViewToggleButton();
        updateDisplayToggleButton();
        if (mapCanvas != null) {
            if (contourCheck != null) contourCheck.setSelected(mapCanvas.isShowContours());
            if (fluxVectorCheck != null) fluxVectorCheck.setSelected(mapCanvas.isShowFlowVectors());
            if (resourceOverlayCheck != null) resourceOverlayCheck.setSelected(mapCanvas.isShowResourceOverlay());
        }
    }

    public void setMiniMap(MiniMap miniMap) { this.miniMap = miniMap; }
    public void setColorLegend(ColorLegend legend) { this.colorLegend = legend; }

    public void updateScenarioName(String scenarioName) {
        if (scenarioName != null && !scenarioName.isBlank()) {
            scenarioHeaderLabel.setText(I18n.getOrDefault("sim.header.scenario", "🎬 Scenario: ") + scenarioName);
        }
    }

    public void updateYear(String year) {
        dateHeaderLabel.setText(I18n.getOrDefault("sim.status.date_time", "📅 Date & Heure : ") + year);
    }

    public void updateStats(long population, double food, long populatedCells, double tps) {
        popStatValue.setText(String.format(I18n.getOrDefault("sim.status.pop_total", "Pop. Totale : %s"), formatNumber(population)));
        foodStatValue.setText(String.format(I18n.getOrDefault("sim.status.food_stocks", "Stocks Alim. : %s"), formatNumber((long) food)));
        cellStatValue.setText(String.format(I18n.getOrDefault("sim.status.populated_cells", "Populated Cells: %,d"), populatedCells));
        double monthsPerSec = tps / 30.0;
        tpsLabel.setText(String.format(java.util.Locale.FRANCE, I18n.getOrDefault("sim.status.tps_detail", "⏱️ Real Speed: %.1f iter/sec (%.1f months/sec | 30 ticks = 1 month)"), tps, monthsPerSec));
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
        String lastEvent = events.get(events.size() - 1);
        eventLabel.setText(lastEvent);
        eventLabel.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.center_event", "🎯 Cliquer pour centrer la vue sur les coordonnées de cet événement.\n\n") + lastEvent));
        eventLabel.setCursor(javafx.scene.Cursor.HAND);
    }

    private void parseAndCenterEvent(String eventText) {
        if (eventText == null || mapCanvas == null) return;
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Lat:\\s*(-?\\d+(?:\\.\\d+)?)(?:°)?([NS])?,\\s*Lng:\\s*(-?\\d+(?:\\.\\d+)?)(?:°)?([EW])?");
            java.util.regex.Matcher matcher = pattern.matcher(eventText);
            if (matcher.find()) {
                double lat = Double.parseDouble(matcher.group(1));
                if ("S".equalsIgnoreCase(matcher.group(2))) lat = -Math.abs(lat);
                double lng = Double.parseDouble(matcher.group(3));
                if ("W".equalsIgnoreCase(matcher.group(4))) lng = -Math.abs(lng);
                mapCanvas.centerOnCoordinates(lat, lng);
            }
        } catch (Exception ex) {
            logger.debug("Could not parse coordinates from event text: {}", eventText);
        }
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
        startBtn.setText("▶");
        pauseBtn.setText("⏸");
        stopBtn.setText("⏹");
        mode3dCheck.setText(I18n.getOrDefault("sim.layer.mode3d", "🌐 Globe 3D H3"));
        mode3dCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.mode3d", "Toggles between 3D spherical globe and 2D flat map")));
        reliefLabel.setText(String.format(java.util.Locale.ROOT, "%s : %.0fx", I18n.getOrDefault("sim.layer.relief3d", "⛰️ Relief 3D"), reliefSlider != null ? reliefSlider.getValue() : 25.0));
        reliefSlider.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.relief3d", "Ajuste la hauteur du relief topographique en mode Globe 3D")));
        autoRotateCheck.setText(I18n.getOrDefault("sim.layer.autorotate", "🔄 Auto-rotation Globe"));
        autoRotateCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.autorotate", "Auto-rotates 3D spherical globe")));
        contourCheck.setText(I18n.getOrDefault("sim.layer.contours", "📈 Courbes de Niveau (Isolines)"));
        contourCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.contours", "Displays elevation contour lines on H3 cells")));
        fluxVectorCheck.setText(I18n.getOrDefault("sim.layer.fluxvectors", "🌊 Flux & Transports (Vecteurs)"));
        fluxVectorCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fluxvectors", "Overlays material flux and population transport vectors")));
        resourceOverlayCheck.setText(I18n.getOrDefault("sim.layer.resources", "💎 Deposits & Capital (Overlays)"));
        resourceOverlayCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.resources", "Displays metal, aquifer, and infrastructure markers")));
        hexGridCheck.setText(I18n.getOrDefault("sim.layer.hexgrid", "⬡ H3 Hexagon Borders"));
        hexGridCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.hexgrid", "Shows or hides H3 hexagon grid (smooth view without borders vs grid view)")));
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
            if (hexGridCheck != null) hexGridCheck.setSelected(mapCanvas.isShowHexGrid());
        }
    }

    private void updateDisplayToggleButton() {
        if (mapCanvas != null && displayModeCombo != null) {
            displayModeCombo.setValue(mapCanvas.getDisplayMode());
        }
    }
}
