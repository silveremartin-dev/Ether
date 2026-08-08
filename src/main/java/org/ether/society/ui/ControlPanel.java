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
    private final Button stepBackBtn;
    private final Button stepForwardBtn;

    private final Button speed1x;
    private final Button speed2x;
    private final Button speed5x;
    private final Button speed20x;
    private final Button speedMax;
    private final Slider speedSlider;

    private final Button hdScreenshotBtn;
    private final Button recordVideoBtn;
    private boolean isRecordingVideo = false;

    private final CheckBox mode3dCheck;
    private final CheckBox contourCheck;
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
        setStyle("-fx-background-color: rgba(15, 23, 42, 0.85); -fx-background-radius: 8; -fx-border-color: rgba(56, 189, 248, 0.2); -fx-border-radius: 8;");

        // --- 1. DATE & TIME HEADER CARD ---
        scenarioHeaderLabel = new Label("🎬 " + I18n.getOrDefault("sim.header.scenario", "Scénario : ") + "Out of Africa");
        scenarioHeaderLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #a78bfa;");

        dateHeaderLabel = new Label("📅 " + I18n.getOrDefault("sim.header.date", "Date & Heure : ") + "T=0");
        dateHeaderLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        tpsLabel = new Label("⏱️ " + I18n.getOrDefault("sim.header.tps", "Cadence : 0.0 itérations/sec (1 tick = 1 mois)"));
        tpsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

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

        stepBackBtn = new Button("<<");
        stepBackBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stepback", "Ralentir / Reculer")));
        stepBackBtn.setOnAction(e -> engine.setSpeed(Math.max(1, (int)(engine.getSpeed() / 2))));

        startBtn = new Button("▶");
        startBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.start", "Lancer / Reprendre")));
        startBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold;");
        startBtn.setOnAction(e -> engine.start());

        pauseBtn = new Button("⏸");
        pauseBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.pause", "Mettre en pause")));
        pauseBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold;");
        pauseBtn.setOnAction(e -> engine.pause());

        stopBtn = new Button("⏹");
        stopBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stop", "Arrêter")));
        stopBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;");
        stopBtn.setOnAction(e -> engine.pause());

        stepForwardBtn = new Button(">>");
        stepForwardBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stepforward", "Avancer rapide")));
        stepForwardBtn.setOnAction(e -> engine.setSpeed(Math.min(20, (int)(engine.getSpeed() * 2))));

        HBox playBar = new HBox(6, rewindBtn, stepBackBtn, startBtn, pauseBtn, stopBtn, stepForwardBtn);
        playBar.setAlignment(Pos.CENTER);

        speedSlider = new Slider(1, 20, 1);
        speedSlider.setBlockIncrement(1);
        speedSlider.setMajorTickUnit(5);
        speedSlider.setMinorTickCount(4);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setSnapToTicks(true);
        speedSlider.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.slider", "Vitesse de simulation")));

        Label speedValueLabel = new Label("⏱️ Vitesse : 1x (1 mois / sec)");
        speedValueLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        speedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int spd = newV.intValue();
            engine.setSpeed(spd);
            String timeRate;
            if (spd >= 100) {
                timeRate = "Calcul CPU Max (Illimité)";
            } else if (spd >= 12) {
                timeRate = String.format(java.util.Locale.FRANCE, "%.1f ans / sec", spd / 12.0);
            } else {
                timeRate = String.format("%d mois / sec", spd);
            }
            speedValueLabel.setText(String.format("⏱️ Vitesse : %s (%s)", spd >= 100 ? "MAX 🚀" : spd + "x", timeRate));
        });

        // Speed graduation scale label line
        HBox scaleLabelsBox = new HBox();
        scaleLabelsBox.setAlignment(Pos.CENTER_LEFT);
        Label lbl1 = new Label("1x");
        Label lbl5 = new Label("5x");
        Label lbl10 = new Label("10x");
        Label lbl15 = new Label("15x");
        Label lbl20 = new Label("20x");
        Label lblMax = new Label("MAX");
        for (Label l : List.of(lbl1, lbl5, lbl10, lbl15, lbl20, lblMax)) {
            l.setStyle("-fx-font-size: 9px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        }
        Region s1 = new Region(); HBox.setHgrow(s1, Priority.ALWAYS);
        Region s2 = new Region(); HBox.setHgrow(s2, Priority.ALWAYS);
        Region s3 = new Region(); HBox.setHgrow(s3, Priority.ALWAYS);
        Region s4 = new Region(); HBox.setHgrow(s4, Priority.ALWAYS);
        Region s5 = new Region(); HBox.setHgrow(s5, Priority.ALWAYS);
        scaleLabelsBox.getChildren().addAll(lbl1, s1, lbl5, s2, lbl10, s3, lbl15, s4, lbl20, s5, lblMax);

        // Speed preset buttons
        speed1x = new Button("1x");
        speed1x.setOnAction(e -> { engine.setSpeed(1); speedSlider.setValue(1); });
        
        speed2x = new Button("2x");
        speed2x.setOnAction(e -> { engine.setSpeed(2); speedSlider.setValue(2); });
        
        speed5x = new Button("5x");
        speed5x.setOnAction(e -> { engine.setSpeed(5); speedSlider.setValue(5); });
        
        speed20x = new Button("20x");
        speed20x.setOnAction(e -> { engine.setSpeed(20); speedSlider.setValue(20); });

        speedMax = new Button("MAX 🚀");
        speedMax.setTooltip(new Tooltip("Calcule les ticks à la vitesse maximale permise par le processeur (Uncapped CPU)"));
        speedMax.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8;");
        speedMax.setOnAction(e -> {
            engine.setSpeed(999);
            speedValueLabel.setText("⏱️ Vitesse : MAX 🚀 (Calcul CPU Max - Uncapped)");
        });

        HBox speedBtnBox = new HBox(5, speed1x, speed2x, speed5x, speed20x, speedMax);
        speedBtnBox.setAlignment(Pos.CENTER);
        speedBtnBox.setAlignment(Pos.CENTER);

        VBox timeCard = new VBox(8, timeTitle, playBar, speedValueLabel, speedBtnBox, speedSlider, scaleLabelsBox);
        styleCard(timeCard);

        // --- 3. MEDIA & EXPORT MP4 CARD ---
        Label mediaTitle = createCardTitle("📸 " + I18n.getOrDefault("sim.card.media", "CAPTURES & VIDÉO"));

        hdScreenshotBtn = new Button("📸 " + I18n.getOrDefault("sim.btn.screenshot", "Capture Photo HD"));
        hdScreenshotBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.screenshot", "Exporte un instantané PNG HD dans saves/screenshots/")));
        hdScreenshotBtn.setMaxWidth(Double.MAX_VALUE);
        hdScreenshotBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 10; -fx-background-radius: 6;");
        hdScreenshotBtn.setOnAction(e -> takeHDScreenshot());

        recordVideoBtn = new Button("🎥 " + I18n.getOrDefault("sim.btn.video", "Enregistrer Vidéo MP4"));
        recordVideoBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.video", "Démarre la capture vidéo MP4 dans saves/timelapse/")));
        recordVideoBtn.setMaxWidth(Double.MAX_VALUE);
        recordVideoBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 10; -fx-background-radius: 6;");
        recordVideoBtn.setOnAction(e -> toggleVideoRecording());

        VBox mediaCard = new VBox(8, mediaTitle, hdScreenshotBtn, recordVideoBtn);
        styleCard(mediaCard);

        // --- 4. DISPLAY & VISUAL LAYERS (CASES À COCHER) ---
        Label viewTitle = createCardTitle("🎨 " + I18n.getOrDefault("sim.card.layers", "COUCHES & OVERLAYS VISUELS"));

        mode3dCheck = new CheckBox(I18n.getOrDefault("sim.layer.mode3d", "🌐 Globe 3D H3"));
        mode3dCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.mode3d", "Bascule entre globe sphérique 3D et carte plate 2D")));
        mode3dCheck.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        mode3dCheck.setOnAction(e -> {
            if (mapCanvas != null) {
                mapCanvas.setViewMode(mode3dCheck.isSelected() ? ViewMode.VIEW_3D : ViewMode.VIEW_2D);
            }
        });

        contourCheck = new CheckBox(I18n.getOrDefault("sim.layer.contours", "📈 Courbes de Niveau (Contours)"));
        contourCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.contours", "Affiche le dénivelé d'altitude sur les cellules H3")));
        contourCheck.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        contourCheck.setOnAction(e -> {
            if (onContourToggle != null) onContourToggle.accept(contourCheck.isSelected());
        });

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
        displayModeCombo.setButtonCell(displayModeCombo.getCellFactory().call(null));

        displayModeCombo.setOnAction(e -> {
            if (mapCanvas != null && displayModeCombo.getValue() != null) {
                mapCanvas.setDisplayMode(displayModeCombo.getValue());
                if (colorLegend != null) colorLegend.setDisplayMode(displayModeCombo.getValue());
            }
        });

        VBox layersVBox = new VBox(6, mode3dCheck, contourCheck, layerComboLabel, displayModeCombo);

        VBox viewCard = new VBox(8, viewTitle, layersVBox);
        styleCard(viewCard);

        // --- 5. SYSTEM STATUS CARD ---
        dbStatusLabel = new Label(I18n.getOrDefault("sim.status.dbcheck", "BDD: Vérification..."));
        dbStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        eventLabel = new Label("");
        eventLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 11px;");

        VBox systemCard = new VBox(6, dbStatusLabel, eventLabel);
        styleCard(systemCard);

        // Keep age and season labels initialized for dateHeaderBox updates
        popStatValue = new Label();
        foodStatValue = new Label();
        cellStatValue = new Label();
        ageLabel = new Label("Âge : Âge de la Pierre");
        ageLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold;");
        seasonLabel = new Label("Saison : Printemps");
        seasonLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");

        // Combine cards cleanly
        getChildren().addAll(dateHeaderBox, timeCard, mediaCard, viewCard, systemCard);

        updateTexts();
        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    private Label createCardTitle(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        return label;
    }

    private void styleCard(VBox card) {
        card.setStyle("-fx-padding: 10; -fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 8; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 8;");
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

    public void toggleVideoRecording() {
        if (onTimelapseRecord != null) {
            onTimelapseRecord.run();
        }
        this.isRecordingVideo = !this.isRecordingVideo;
        if (isRecordingVideo) {
            recordVideoBtn.setText("⏹️ Arrêter Vidéo MP4");
            recordVideoBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8;");
            if (notificationOverlay != null) {
                notificationOverlay.showNotification("🎥 Enregistrement Vidéo MP4 Démarré !", "#ef4444");
            }
        } else {
            recordVideoBtn.setText("🎥 Enregistrer Vidéo MP4");
            recordVideoBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8;");
            if (notificationOverlay != null) {
                notificationOverlay.showNotification("🎬 Enregistrement Vidéo MP4 Finalisé !\nStocké dans saves/timelapse/", "#10b981");
            }
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
        tpsLabel.setText(String.format(java.util.Locale.FRANCE, "⏱️ Cadence : %.1f itérations/sec (1 tick = 1 mois)", tps));
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
    }

    public void updateAge(String ageName) {
        ageLabel.setText("Âge : " + ageName);
    }

    public void updateDatabaseStatus(boolean online) {
        if (online) {
            dbStatusLabel.setText("🟢 BDD Supabase/PostGIS : En Ligne");
            dbStatusLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");
        } else {
            dbStatusLabel.setText("⚪ BDD : Mode Hors Ligne (Fichiers Locaux)");
            dbStatusLabel.setStyle("-fx-text-fill: #94a3b8;");
        }
    }

    private void updateTexts() {
        startBtn.setText("▶");
        pauseBtn.setText("⏸");
        stopBtn.setText("⏹");
        updateViewToggleButton();
        updateDisplayToggleButton();
    }

    private void updateViewToggleButton() {
        if (mapCanvas != null && mode3dCheck != null) {
            mode3dCheck.setSelected(mapCanvas.getViewMode() == ViewMode.VIEW_3D);
        }
    }

    private void updateDisplayToggleButton() {
        if (mapCanvas != null && displayModeCombo != null) {
            displayModeCombo.setValue(mapCanvas.getDisplayMode());
        }
    }
}
