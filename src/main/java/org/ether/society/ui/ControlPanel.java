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
        scenarioHeaderLabel = new Label("🎬 " + I18n.getOrDefault("sim.header.scenario", "Scénario : ") + "Out of Africa");
        scenarioHeaderLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #a78bfa;");
        scenarioHeaderLabel.setTooltip(new Tooltip("Nom du scénario historique ou procédural en cours de simulation"));

        dateHeaderLabel = new Label("📅 " + I18n.getOrDefault("sim.header.date", "Date & Heure : ") + "An -20000, Mois 1, Jour 1");
        dateHeaderLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        dateHeaderLabel.setTooltip(new Tooltip(
            "⏱️ Horloge et Pas de Simulation (Temporal Resolution) :\n" +
            "• An, Mois, Jour : Horloge courante de la simulation.\n" +
            "• Échelle Rapide (Daily Tick) : Avance d'1 jour à chaque itération (dt = 86,400s).\n" +
            "• Échelle Lente (Monthly Cycle) : Réconciliation physique & économique tous les 30 jours."
        ));

        tpsLabel = new Label("⏱️ " + I18n.getOrDefault("sim.header.tps", "Cadence : 0.0 itér/sec (Échelle rapide 1d / lente 30d)"));
        tpsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        tpsLabel.setTooltip(new Tooltip(
            "⚙️ Comparatif des Échelles Temporelles (Fast vs Slow Scale) :\n\n" +
            "• Échelle Rapide (1 tick = 1 jour) :\n" +
            "  Calcule les flux matériels, transports physiques et extensions politiques (O(1)).\n\n" +
            "• Échelle Lente (1 cycle = 30 jours) :\n" +
            "  Calcule le climat, la démographie, les nutriments NPK, l'inertie d'infrastructures et l'entropie.\n\n" +
            "💡 Passage de l'Échelle Lente à 1 Jour (Équivalence Rapide/Lente) :\n" +
            "  - Avantage : Précision numérique maximale, élimination des dérives d'intégration (comme l'overflow de capital à 88%).\n" +
            "  - Inconvénient : Charge CPU multipliée par 30."
        ));

        VBox dateHeaderBox = new VBox(4, scenarioHeaderLabel, dateHeaderLabel, tpsLabel);
        styleCard(dateHeaderBox);

        // --- 2. TEMPORAL & PLAYBACK CONTROLS CARD ---
        Label timeTitle = createCardTitle("⏱️ " + I18n.getOrDefault("sim.card.time", "CONTRÔLES TEMPS & LECTURE"));

        rewindBtn = new Button("|<<");
        rewindBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.rewind", "Réinitialiser T=0")));
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
        autoRecordCheck = new CheckBox("🎬 Auto Sync Vidéo (Lancement & Pause)");
        autoRecordCheck.setTooltip(new Tooltip("Démarre/Arrête la vidéo 1:1 automatiquement en synchronisation avec le lancement et la pause du scénario"));
        autoRecordCheck.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");

        startBtn = new Button("▶");
        startBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.start", "Lancer / Reprendre")));
        startBtn.setOnAction(e -> {
            if (autoRecordCheck.isSelected() && !isRecordingVideo) {
                toggleVideoRecording();
            }
            engine.start();
            updatePlayPauseVisuals(true);
        });

        pauseBtn = new Button("⏸ EN PAUSE");
        pauseBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.pause", "Mettre en pause")));
        pauseBtn.setOnAction(e -> {
            engine.pause();
            if (autoRecordCheck != null && autoRecordCheck.isSelected() && isRecordingVideo) {
                toggleVideoRecording();
            }
            updatePlayPauseVisuals(false);
        });

        stopBtn = new Button("⏹");
        stopBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stop", "Arrêter")));
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

        CheckBox pauseOnEventCheck = new CheckBox("⏸️ Pause auto sur évènement");
        pauseOnEventCheck.setTooltip(new Tooltip("Met automatiquement la simulation en pause dès qu'un nouvel événement planétaire ou régional se produit"));
        pauseOnEventCheck.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        pauseOnEventCheck.setOnAction(e -> engine.setPauseAtNextEvent(pauseOnEventCheck.isSelected()));

        HBox playBar = new HBox(4, rewindBtn, fastRewindBtn, stepBackBtn, startBtn, pauseBtn, stopBtn, stepForwardBtn, fastForwardBtn);
        playBar.setAlignment(Pos.CENTER);

        ComboBox<org.ether.society.core.H3SimulationEngine.TemporalScale> temporalScaleCombo = new ComboBox<>();
        temporalScaleCombo.getItems().addAll(org.ether.society.core.H3SimulationEngine.TemporalScale.values());
        temporalScaleCombo.setValue(engine.getTemporalScale());
        temporalScaleCombo.setMaxWidth(Double.MAX_VALUE);
        temporalScaleCombo.setTooltip(new Tooltip("Choisis la résolution temporelle : 1 tick/jour (précision flux) vs 1 tick/mois (vitesse macro x30)"));
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
        speedSlider.setTooltip(new Tooltip("Vitesse de simulation CPU (Gauche = 1 tick/sec soit 1 jour/sec | Droite = Mode Rapide)"));
        HBox.setHgrow(speedSlider, Priority.ALWAYS);

        Label speedValueLabel = new Label("⏱️ Vitesse : 1 tick/sec (1 jour/sec)");
        speedValueLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        speedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int spd = newV.intValue();
            engine.setSpeed(spd);
            speedValueLabel.setText(String.format("⏱️ Vitesse Cible : %d ticks/sec (%d jours/sec)", spd, spd));
        });

        speedMax = new Button("MAX 🚀");
        speedMax.setTooltip(new Tooltip("Calcule les ticks à la vitesse maximale permise par le processeur sans aucune limite (Uncapped CPU ticks/sec)"));
        speedMax.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4;");
        speedMax.setOnAction(e -> {
            engine.setSpeed(999);
            speedValueLabel.setText("⏱️ Vitesse Cible : MAX 🚀 (Illimité - Autant de ticks CPU/sec que possible)");
        });

        HBox sliderRow = new HBox(8, speedSlider, speedMax);
        // --- 1. HORLOGE & CONTRÔLE TEMPOREL CARD ---
        VBox timeCard = new VBox(8, scenarioHeaderLabel, dateHeaderLabel, tpsLabel, timeTitle, playBar, temporalScaleCombo, pauseOnEventCheck, speedValueLabel, sliderRow);
        styleCard(timeCard);

        // --- 3. MEDIA & EXPORT MP4 CARD ---
        Label mediaTitle = createCardTitle("📸 " + I18n.getOrDefault("sim.card.media", "CAPTURES & VIDÉO"));

        hdScreenshotBtn = new Button("📸 " + I18n.getOrDefault("sim.btn.screenshot", "Capture Photo HD"));
        hdScreenshotBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.screenshot", "Exporte un instantané PNG HD dans saves/screenshots/")));
        hdScreenshotBtn.setMaxWidth(Double.MAX_VALUE);
        hdScreenshotBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 10; -fx-background-radius: 6;");
        hdScreenshotBtn.setOnAction(e -> takeHDScreenshot());

        recordVideoBtn = new Button("🎥 " + I18n.getOrDefault("sim.btn.video", "Enregistrer Vidéo MP4"));
        recordVideoBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.video", "Démarre la capture vidéo MP4 (1:1 tick) dans saves/timelapse/")));
        recordVideoBtn.setMaxWidth(Double.MAX_VALUE);
        recordVideoBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 10; -fx-background-radius: 6;");
        recordVideoBtn.setOnAction(e -> toggleVideoRecording());

        // --- 2. PILE DE COUCHES & PRÉRÉGLAGES SCIENTIFIQUES CARD ---
        Label layersTitle = createCardTitle("🗺️ 2. PILE DE COUCHES & PRÉRÉGLAGES SCIENTIFIQUES");

        // 1-Click Scientific Presets
        Label presetsTitle = new Label("⚡ Préréglages Scientifiques en 1-Clic :");
        presetsTitle.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px;");

        Button presetSynth = createPresetBtn("🌍 Synthèse", "Mode 3D Globe + Biomes + Relief + Isolines", () -> applyPresetSynthesis(pauseOnEventCheck));
        Button presetEcon = createPresetBtn("📈 Économie", "Carte Flux / PIB + Vecteurs Transport + Gisements", () -> applyPresetEcon());
        Button presetClim = createPresetBtn("🌡️ Climat", "Carte Température + Aquifères + Isolines Dénivelé", () -> applyPresetClimate());
        Button presetCliodyn = createPresetBtn("🏛️ Cliodynamique", "Carte Démographique + Maillage H3 + Pause Événements", () -> applyPresetCliodynamics(pauseOnEventCheck));

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
        presetBox.setStyle("-fx-background-color: rgba(15, 23, 42, 0.5); -fx-padding: 6; -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.25); -fx-border-radius: 6;");

        Label layerComboLabel = new Label(I18n.getOrDefault("sim.layer.datacategory", "Couche Donnée Active :"));
        layerComboLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        displayModeCombo = new ComboBox<>();
        List<DisplayMode> sortedModes = java.util.Arrays.stream(DisplayMode.values())
                .sorted(java.util.Comparator.comparing(DisplayMode::getCategory).thenComparing(DisplayMode::ordinal))
                .toList();
        displayModeCombo.getItems().addAll(sortedModes);
        displayModeCombo.setValue(DisplayMode.BIOME);
        displayModeCombo.setMaxWidth(Double.MAX_VALUE);
        displayModeCombo.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.datacategory", "Alterne la couche de couleur principale sur la carte")));

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
        contourCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.contours", "Affiche le dénivelé d'altitude et isolines sur les cellules H3")));
        contourCheck.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        contourCheck.setOnAction(e -> {
            if (mapCanvas != null) mapCanvas.setShowContours(contourCheck.isSelected());
            if (onContourToggle != null) onContourToggle.accept(contourCheck.isSelected());
        });

        fluxVectorCheck = new CheckBox(I18n.getOrDefault("sim.layer.fluxvectors", "🌊 Flux & Transports (Vecteurs)"));
        fluxVectorCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fluxvectors", "Surimpose les vecteurs de flux matériels et transports de population")));
        fluxVectorCheck.setStyle("-fx-text-fill: #fb923c; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        fluxVectorCheck.setOnAction(e -> {
            if (mapCanvas != null) mapCanvas.setShowFlowVectors(fluxVectorCheck.isSelected());
        });

        resourceOverlayCheck = new CheckBox(I18n.getOrDefault("sim.layer.resources", "💎 Gisements & Capital (Overlays)"));
        resourceOverlayCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.resources", "Affiche les marqueurs de métaux, nappe phréatique et infrastructure")));
        resourceOverlayCheck.setStyle("-fx-text-fill: #eab308; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        resourceOverlayCheck.setOnAction(e -> {
            if (mapCanvas != null) mapCanvas.setShowResourceOverlay(resourceOverlayCheck.isSelected());
        });

        Label stackTitleLabel = new Label(I18n.getOrDefault("sim.layer.overlaystack", "Pile de Couches (Multi-Sélection) :"));
        stackTitleLabel.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold; -fx-font-size: 11px;");

        VBox overlayStackBox = new VBox(4, stackTitleLabel, contourCheck, fluxVectorCheck, resourceOverlayCheck);
        overlayStackBox.setStyle("-fx-background-color: rgba(15, 23, 42, 0.4); -fx-padding: 6; -fx-background-radius: 6; -fx-border-color: rgba(167, 139, 250, 0.2); -fx-border-radius: 6;");

        VBox layersCard = new VBox(8, layersTitle, presetBox, layerComboLabel, displayModeCombo, overlayStackBox);
        styleCard(layersCard);

        // --- 3. PROJECTION & PARAMÈTRES DE RENDU CARD ---
        Label renderTitle = createCardTitle("🌐 3. PROJECTION & PARAMÈTRES DE RENDU");

        mode3dCheck = new CheckBox(I18n.getOrDefault("sim.layer.mode3d", "🌐 Globe 3D H3"));
        mode3dCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.mode3d", "Bascule entre globe sphérique 3D et carte plate 2D")));
        mode3dCheck.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");

        reliefLabel = new Label(I18n.getOrDefault("sim.layer.relief3d", "⛰️ Relief 3D") + " : 25x");
        reliefLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        reliefLabel.setDisable(true);

        reliefSlider = new Slider(0.0, 50.0, 25.0);
        reliefSlider.setBlockIncrement(5.0);
        reliefSlider.setMajorTickUnit(15.0);
        reliefSlider.setMinorTickCount(2);
        reliefSlider.setShowTickMarks(true);
        reliefSlider.setShowTickLabels(false);
        reliefSlider.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.relief3d", "Ajuste la hauteur du relief topographique en mode Globe 3D (0.0x à 50x)")));
        reliefSlider.setDisable(true);
        reliefSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double val = newV.doubleValue();
            if (mapCanvas != null) {
                mapCanvas.setVerticalExaggeration(val);
            }
            reliefLabel.setText(String.format(java.util.Locale.ROOT, "%s : %.0fx", I18n.getOrDefault("sim.layer.relief3d", "⛰️ Relief 3D"), val));
        });

        autoRotateCheck = new CheckBox(I18n.getOrDefault("sim.layer.autorotate", "🔄 Auto-rotation Globe"));
        autoRotateCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.autorotate", "Fait pivoter automatiquement le globe sphérique 3D")));
        autoRotateCheck.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
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

        hexGridCheck = new CheckBox(I18n.getOrDefault("sim.layer.hexgrid", "⬡ Bordures Hexagones H3"));
        hexGridCheck.setSelected(true);
        hexGridCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.hexgrid", "Affiche ou masque le maillage hexagonale H3 (vue lissée sans bordures vs vue maillée)")));
        hexGridCheck.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        hexGridCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setShowHexGrid(hexGridCheck.isSelected());
            }
        });

        CheckBox smoothMapCheck = new CheckBox(I18n.getOrDefault("sim.layer.smoothmap", "🎨 Carte Lissée (Continuous Heatmap)"));
        smoothMapCheck.setSelected(true);
        smoothMapCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.smoothmap", "Affiche une carte continue avec dégradés fluides au lieu d'hexagones individuels")));
        smoothMapCheck.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        smoothMapCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setSmoothMap(smoothMapCheck.isSelected());
            }
        });

        Label paletteLabel = new Label("🎨 Palette Scientifique :");
        paletteLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-font-weight: bold;");

        ComboBox<ScientificColorMap> paletteCombo = new ComboBox<>();
        paletteCombo.getItems().addAll(ScientificColorMap.values());
        paletteCombo.setValue(ScientificColorMap.TURBO);
        paletteCombo.setMaxWidth(Double.MAX_VALUE);
        paletteCombo.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #e2e8f0; -fx-font-size: 11px;");
        paletteCombo.setOnAction(e -> {
            if (mapCanvas != null && paletteCombo.getValue() != null) {
                mapCanvas.setScientificColorMap(paletteCombo.getValue());
            }
        });

        CheckBox hillshadingCheck = new CheckBox("⛰️ Hillshading Relief Topographique");
        hillshadingCheck.setSelected(false);
        hillshadingCheck.setTooltip(new Tooltip("Applique un ombrage topographique lambertien selon la pente du relief"));
        hillshadingCheck.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        hillshadingCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setShowHillshading(hillshadingCheck.isSelected());
            }
        });

        CheckBox solarTerminatorCheck = new CheckBox("☀️ Terminateur Solaire Jour/Nuit");
        solarTerminatorCheck.setSelected(false);
        solarTerminatorCheck.setTooltip(new Tooltip("Affiche l'ombre portée de la nuit et de la pénombre crépusculaire"));
        solarTerminatorCheck.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        solarTerminatorCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setShowSolarTerminator(solarTerminatorCheck.isSelected());
            }
        });

        CheckBox lodCheck = new CheckBox("📐 Agrégation Pyramide LOD H3");
        lodCheck.setSelected(true);
        lodCheck.setTooltip(new Tooltip("Agrège automatiquement les cellules vers leurs parents H3 lors du dézoom"));
        lodCheck.setStyle("-fx-text-fill: #a855f7; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        lodCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setEnableHierarchicalLOD(lodCheck.isSelected());
            }
        });

        VBox renderCard = new VBox(8, renderTitle, mode3dCheck, reliefLabel, reliefSlider, autoRotateCheck, smoothMapCheck, hexGridCheck, lodCheck, paletteLabel, paletteCombo, hillshadingCheck, solarTerminatorCheck);
        styleCard(renderCard);

        // --- 4. TÉLÉMÉTRIE, CAPTURES & EXPORT CARD ---
        Label exportTitle = createCardTitle("📊 4. TÉLÉMÉTRIE, CAPTURES & EXPORTS");

        dbStatusLabel = new Label(I18n.getOrDefault("sim.status.dbcheck", "BDD: Vérification..."));
        dbStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        eventLabel = new Label("");
        eventLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 11px;");
        eventLabel.setOnMouseClicked(e -> parseAndCenterEvent(eventLabel.getText()));

        VBox exportCard = new VBox(8, exportTitle, hdScreenshotBtn, recordVideoBtn, autoRecordCheck, dbStatusLabel, eventLabel);
        styleCard(exportCard);

        // Keep age and season labels initialized for dateHeaderBox updates
        popStatValue = new Label();
        foodStatValue = new Label();
        cellStatValue = new Label();
        ageLabel = new Label("Âge : Âge de la Pierre");
        ageLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold;");
        seasonLabel = new Label("Saison : Printemps");
        seasonLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");

        // Combine cards cleanly into 4 modular accordion-style sections
        getChildren().addAll(timeCard, layersCard, renderCard, exportCard);

        updateTexts();
        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    public void updatePlayPauseVisuals(boolean isRunning) {
        if (startBtn == null || pauseBtn == null) return;
        if (isRunning) {
            startBtn.setText("▶ EN COURS");
            startBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-border-color: #34d399; -fx-border-width: 1.5px; -fx-background-radius: 6; -fx-border-radius: 6; -fx-effect: dropshadow(three-pass-box, rgba(16,185,129,0.7), 8, 0, 0, 0);");
            pauseBtn.setText("⏸");
            pauseBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-background-radius: 6; -fx-effect: none;");
        } else {
            startBtn.setText("▶");
            startBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-background-radius: 6; -fx-effect: none;");
            pauseBtn.setText("⏸ EN PAUSE");
            pauseBtn.setStyle("-fx-background-color: #d97706; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-border-color: #fbbf24; -fx-border-width: 1.5px; -fx-background-radius: 6; -fx-border-radius: 6; -fx-effect: dropshadow(three-pass-box, rgba(245,158,11,0.7), 8, 0, 0, 0);");
        }
    }

    private Button createPresetBtn(String label, String tooltip, Runnable action) {
        Button btn = new Button(label);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: rgba(30, 41, 59, 0.85); -fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 10px; -fx-border-color: #38bdf8; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5; -fx-cursor: hand;");
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
            scenarioHeaderLabel.setText("🎬 Scénario : " + scenarioName);
        }
    }

    public void updateYear(String year) {
        dateHeaderLabel.setText("📅 Date & Heure : " + year);
    }

    public void updateStats(long population, double food, long populatedCells, double tps) {
        popStatValue.setText(String.format("Pop. Totale : %s", formatNumber(population)));
        foodStatValue.setText(String.format("Stocks Alim. : %s", formatNumber((long) food)));
        cellStatValue.setText(String.format("Cellules Habitées : %,d", populatedCells));
        double monthsPerSec = tps / 30.0;
        tpsLabel.setText(String.format(java.util.Locale.FRANCE, "⏱️ Cadence Réelle : %.1f itér/sec (%.1f mois/sec | 30 ticks = 1 mois)", tps, monthsPerSec));
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
        String[] seasonNames = { "Hiver ❄️", "Printemps 🌿", "Été ☀️", "Automne 🍂" };
        String[] seasonColors = { "#64b5f6", "#4ade80", "#facc15", "#fb923c" };

        int seasonIndex;
        if (month == 11 || month == 0 || month == 1) seasonIndex = 0;
        else if (month >= 2 && month <= 4) seasonIndex = 1;
        else if (month >= 5 && month <= 7) seasonIndex = 2;
        else seasonIndex = 3;

        seasonLabel.setText("Saison : " + seasonNames[seasonIndex]);
        seasonLabel.setStyle("-fx-text-fill: " + seasonColors[seasonIndex] + "; -fx-font-weight: bold;");
    }

    public void logEvents(List<String> events) {
        if (events == null || events.isEmpty()) return;
        eventHistory.addAll(events);
        String lastEvent = events.get(events.size() - 1);
        eventLabel.setText(lastEvent);
        eventLabel.setTooltip(new Tooltip("🎯 Cliquer pour centrer la vue sur les coordonnées de cet événement.\n\n" + lastEvent));
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
        ageLabel.setText("Âge : " + ageName);
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
        mode3dCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.mode3d", "Bascule entre globe sphérique 3D et carte plate 2D")));
        reliefLabel.setText(String.format(java.util.Locale.ROOT, "%s : %.0fx", I18n.getOrDefault("sim.layer.relief3d", "⛰️ Relief 3D"), reliefSlider != null ? reliefSlider.getValue() : 25.0));
        reliefSlider.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.relief3d", "Ajuste la hauteur du relief topographique en mode Globe 3D")));
        autoRotateCheck.setText(I18n.getOrDefault("sim.layer.autorotate", "🔄 Auto-rotation Globe"));
        autoRotateCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.autorotate", "Fait pivoter automatiquement le globe sphérique 3D")));
        contourCheck.setText(I18n.getOrDefault("sim.layer.contours", "📈 Courbes de Niveau (Isolines)"));
        contourCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.contours", "Affiche le dénivelé d'altitude et isolines sur les cellules H3")));
        fluxVectorCheck.setText(I18n.getOrDefault("sim.layer.fluxvectors", "🌊 Flux & Transports (Vecteurs)"));
        fluxVectorCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fluxvectors", "Surimpose les vecteurs de flux matériels et transports de population")));
        resourceOverlayCheck.setText(I18n.getOrDefault("sim.layer.resources", "💎 Gisements & Capital (Overlays)"));
        resourceOverlayCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.resources", "Affiche les marqueurs de métaux, nappe phréatique et infrastructure")));
        hexGridCheck.setText(I18n.getOrDefault("sim.layer.hexgrid", "⬡ Bordures Hexagones H3"));
        hexGridCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.hexgrid", "Affiche ou masque le maillage hexagonale H3 (vue lissée sans bordures vs vue maillée)")));
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
