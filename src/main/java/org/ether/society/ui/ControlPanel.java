/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.ether.society.core.ISimulationEngine;
import org.ether.society.events.ActiveEvent;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.control.ControlPanelCaptureSection;
import org.ether.society.ui.control.ControlPanelEventsSection;
import org.ether.society.ui.control.ControlPanelLayersSection;
import org.ether.society.ui.control.ControlPanelRenderingSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern simulation control and rendering panel for Tab 4 (Simulation View).
 *
 * <p>Refactored according to the Single Responsibility Principle (SRP). Acts as the primary
 * coordinator for simulation clock playback, temporal resolution scaling, and delegates modular cards to:
 * <ul>
 *   <li>{@link ControlPanelLayersSection} for cartographic layer stack multi-selection and 1-click scientific presets</li>
 *   <li>{@link ControlPanelRenderingSection} for 3D spherical globe projections, relief exaggeration, and uniform palettes</li>
 *   <li>{@link ControlPanelCaptureSection} for HD screenshots and MP4 video timelapse generation</li>
 *   <li>{@link ControlPanelEventsSection} for chronological planetary event feeds and click-to-ping navigation</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
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

    // Section 1: Temporal Controls
    private Label timeTitle;
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
    private final CheckBox pauseOnEventCheck;

    // Sub-Sections
    private final ControlPanelLayersSection layersSection;
    private final ControlPanelRenderingSection renderingSection;
    private final ControlPanelCaptureSection captureSection;
    private final ControlPanelEventsSection eventsSection;

    // Cluster & DB state
    private boolean isDbOnline = false;
    private String clusterNodeState = "💻 Nœud Local Standalone";

    // Callbacks
    private Runnable onSave;
    private Runnable onLoad;
    private Runnable onTimelapseRecord;
    private Consumer<Integer> onTimelapseSeek;
    private Runnable onTimelapseSeekToEnd;
    private Consumer<Boolean> onStatsToggle;
    private Runnable onRewindStart;
    private Runnable onFastRewind;
    private Runnable onStepBack;
    private Runnable onStepForward;
    private Runnable onFastForward;
    private Runnable onSeekToEnd;

    /**
     * Constructs the simulation control panel.
     *
     * @param engine active simulation engine instance
     */
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
            I18n.getOrDefault("sim.tooltip.date_header", "Current simulation clock according to configured time step.")
        ));

        dbStatusLabel = new Label(I18n.getOrDefault("sim.status.dbcheck", "DB: Checking..."));
        dbStatusLabel.getStyleClass().add("control-label");
        dbStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-padding: 2 0 0 0;");

        pauseOnEventCheck = new CheckBox(I18n.getOrDefault("sim.option.pause_on_event", "⏸️ Auto-pause on event"));
        pauseOnEventCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.pause_on_event", "Automatically pauses simulation when a new planetary or regional event occurs")));
        pauseOnEventCheck.getStyleClass().add("opt-sub-checkbox");
        pauseOnEventCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        pauseOnEventCheck.setOnAction(e -> engine.setPauseAtNextEvent(pauseOnEventCheck.isSelected()));

        // --- SUB-SECTIONS INITIALIZATION ---
        layersSection = new ControlPanelLayersSection();
        layersSection.setMapCanvasSupplier(() -> this.mapCanvas);
        layersSection.setColorLegendSupplier(() -> this.colorLegend);
        layersSection.setEngineSupplier(() -> this.engine);
        layersSection.setPauseOnEventCheckSupplier(() -> this.pauseOnEventCheck);

        renderingSection = new ControlPanelRenderingSection();
        renderingSection.setMapCanvasSupplier(() -> this.mapCanvas);
        renderingSection.setColorLegendSupplier(() -> this.colorLegend);
        layersSection.setRenderingSectionSupplier(() -> this.renderingSection);

        captureSection = new ControlPanelCaptureSection(this::takeHDScreenshot, this::toggleVideoRecording);

        eventsSection = new ControlPanelEventsSection();
        eventsSection.setOnEventSelected(evt -> {
            if (mapCanvas != null) {
                mapCanvas.flyTo(evt.getLatitude(), evt.getLongitude());
                mapCanvas.pingLocation(evt.getLatitude(), evt.getLongitude(), evt.getTitle(), evt.getType(), evt.getMagnitude());
            }
        });

        // --- 2. TEMPORAL & PLAYBACK CONTROLS CARD ---
        timeTitle = createCardTitle("⏱️ " + I18n.getOrDefault("sim.card.time", "TIME & PLAYBACK CONTROLS"));

        rewindBtn = new Button("⏮");
        rewindBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.rewind", "Reset to T=0 — Pauses simulation")));
        rewindBtn.setOnAction(e -> {
            engine.pause();
            updatePlayPauseVisuals(false);
            if (onRewindStart != null) {
                onRewindStart.run();
            } else if (onTimelapseSeek != null) {
                int startYear = engine.getCurrentScenario() != null ? (int) engine.getCurrentScenario().getStartDateYear() : -20000;
                onTimelapseSeek.accept(startYear);
            }
            updatePlayPauseVisuals(false);
        });

        fastRewindBtn = new Button("⏪");
        fastRewindBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastrewind", "Previous snapshot / Fast rewind — Pauses simulation")));
        fastRewindBtn.setOnAction(e -> {
            engine.pause();
            updatePlayPauseVisuals(false);
            if (onFastRewind != null) {
                onFastRewind.run();
            } else {
                engine.stepBackward(100);
            }
            updatePlayPauseVisuals(false);
        });

        stepBackBtn = new Button("⏴");
        stepBackBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stepback", "Step backward / previous snapshot — Pauses simulation")));
        stepBackBtn.setOnAction(e -> {
            engine.pause();
            updatePlayPauseVisuals(false);
            if (onStepBack != null) {
                onStepBack.run();
            } else {
                engine.stepBackward(10);
            }
            updatePlayPauseVisuals(false);
        });

        playPauseBtn = new Button("▶");
        playPauseBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.start", "Start / Resume")));
        playPauseBtn.setOnAction(e -> {
            if (engine.isRunning()) {
                engine.pause();
                if (captureSection != null && captureSection.isAutoRecordSelected() && captureSection.isRecordingVideo()) {
                    toggleVideoRecording();
                }
                updatePlayPauseVisuals(false);
            } else {
                if (captureSection != null && captureSection.isAutoRecordSelected() && !captureSection.isRecordingVideo()) {
                    toggleVideoRecording();
                }
                engine.start();
                updatePlayPauseVisuals(true);
            }
        });

        stepForwardBtn = new Button("⏵");
        stepForwardBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stepforward", "Step forward / next snapshot — Pauses simulation")));
        stepForwardBtn.setOnAction(e -> {
            engine.pause();
            updatePlayPauseVisuals(false);
            if (onStepForward != null) {
                onStepForward.run();
            } else {
                engine.stepForward(10);
            }
            updatePlayPauseVisuals(false);
        });

        fastForwardBtn = new Button("⏩");
        fastForwardBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastforward", "Fast forward / Next snapshot — Pauses simulation")));
        fastForwardBtn.setOnAction(e -> {
            engine.pause();
            updatePlayPauseVisuals(false);
            if (onFastForward != null) {
                onFastForward.run();
            } else {
                engine.stepForward(100);
            }
            updatePlayPauseVisuals(false);
        });

        endBtn = new Button("⏭");
        endBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastforward_end", "Jump to latest snapshot / End — Pauses simulation")));
        endBtn.setOnAction(e -> {
            engine.pause();
            updatePlayPauseVisuals(false);
            if (onSeekToEnd != null) {
                onSeekToEnd.run();
            } else if (onTimelapseSeekToEnd != null) {
                onTimelapseSeekToEnd.run();
            } else {
                engine.seekToEnd();
            }
            updatePlayPauseVisuals(false);
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

        HBox playBar = new HBox(4, rewindBtn, fastRewindBtn, stepBackBtn, playPauseBtn, stepForwardBtn, fastForwardBtn, endBtn);
        playBar.setAlignment(Pos.CENTER);

        speedSlider = new Slider(0.1, 20.0, 1.0);
        speedSlider.setBlockIncrement(0.5);
        speedSlider.setMajorTickUnit(5.0);
        speedSlider.setMinorTickCount(4);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setSnapToTicks(false);
        speedSlider.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.speed_slider", "CPU simulation speed (0.1 to 20 steps/sec or MAX)")));
        HBox.setHgrow(speedSlider, Priority.ALWAYS);

        speedValueLabel = new Label("⏱️ " + I18n.getOrDefault("sim.speed.label", "Speed : 1 step/sec"));
        speedValueLabel.getStyleClass().add("value-label");
        speedValueLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        speedMax = new ToggleButton("MAX 🚀");
        speedMax.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.speed_max", "Runs iterations at maximum processor capacity")));
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

        VBox timeCard = new VBox(8, scenarioHeaderLabel, dateHeaderLabel, dbStatusLabel, timeTitle, playBar, pauseOnEventCheck, speedValueLabel, sliderRow);
        timeCard.getStyleClass().add("card-section");

        popStatValue = new Label();
        foodStatValue = new Label();
        cellStatValue = new Label();
        ageLabel = new Label(I18n.getOrDefault("sim.age.stone_age", "Age: Stone Age"));
        ageLabel.getStyleClass().add("control-label");
        seasonLabel = new Label(I18n.getOrDefault("sim.season.spring", "Season: Spring"));
        seasonLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");

        // Combine cards into 5 modular accordion-style sections
        getChildren().addAll(timeCard, layersSection.getCard(), renderingSection.getCard(), captureSection.getCard(), eventsSection.getCard());

        updateTexts();
        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    /**
     * Updates play/pause button styling and tooltips.
     *
     * @param isRunning true if engine is running
     */
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

    private static Label createCardTitle(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        return label;
    }

    /**
     * Sets notification overlay.
     *
     * @param overlay notification overlay
     */
    public void setNotificationOverlay(NotificationOverlay overlay) {
        this.notificationOverlay = overlay;
    }

    /**
     * Takes HD screenshot of active map canvas.
     */
    public void takeHDScreenshot() {
        if (captureSection != null) {
            captureSection.takeHDScreenshot(mapCanvas, notificationOverlay);
        }
    }

    /**
     * Toggles video recording.
     */
    public void toggleVideoRecording() {
        if (captureSection != null) {
            captureSection.toggleVideoRecording(engine, mapCanvas, notificationOverlay, onTimelapseRecord);
        }
    }

    public void setOnSave(Runnable onSave) { this.onSave = onSave; }
    public void setOnLoad(Runnable onLoad) { this.onLoad = onLoad; }
    public void setOnContourToggle(Consumer<Boolean> onContourToggle) {
        if (layersSection != null) layersSection.setOnContourToggle(onContourToggle);
    }
    public void setOnTimelapseRecord(Runnable onTimelapseRecord) { this.onTimelapseRecord = onTimelapseRecord; }
    public void setOnTimelapseSeek(Consumer<Integer> onTimelapseSeek) { this.onTimelapseSeek = onTimelapseSeek; }
    public void setOnTimelapseSeekToEnd(Runnable onTimelapseSeekToEnd) { this.onTimelapseSeekToEnd = onTimelapseSeekToEnd; }
    public void setOnRewindStart(Runnable onRewindStart) { this.onRewindStart = onRewindStart; }
    public void setOnFastRewind(Runnable onFastRewind) { this.onFastRewind = onFastRewind; }
    public void setOnStepBack(Runnable onStepBack) { this.onStepBack = onStepBack; }
    public void setOnStepForward(Runnable onStepForward) { this.onStepForward = onStepForward; }
    public void setOnFastForward(Runnable onFastForward) { this.onFastForward = onFastForward; }
    public void setOnSeekToEnd(Runnable onSeekToEnd) { this.onSeekToEnd = onSeekToEnd; }
    public void setOnStatsToggle(Consumer<Boolean> onStatsToggle) { this.onStatsToggle = onStatsToggle; }
    public void setOnFullScreen(Runnable onFullScreen) {
        if (renderingSection != null) renderingSection.setOnFullScreen(onFullScreen);
    }

    /**
     * Updates date header for timelapse slider.
     *
     * @param minYear minimum year
     * @param maxYear maximum year
     * @param currentYear current simulation year
     */
    public void updateTimelapseSlider(int minYear, int maxYear, int currentYear) {
        dateHeaderLabel.setText("📅 " + I18n.getOrDefault("sim.header.date", "Date : ") + String.format(java.util.Locale.ROOT, "An %d", currentYear));
    }

    /**
     * Sets active H3 map canvas.
     *
     * @param canvas map canvas
     */
    public void setMapCanvas(H3MapCanvas canvas) {
        this.mapCanvas = canvas;
        updateViewToggleButton();
        updateDisplayToggleButton();
    }

    public void setMiniMap(MiniMap miniMap) { this.miniMap = miniMap; }

    public void setColorLegend(ColorLegend legend) {
        this.colorLegend = legend;
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
        if (eventsSection != null) {
            eventsSection.updateRecentEvents(events);
        }
    }

    public void updateAge(String ageName) {
        ageLabel.setText(I18n.getOrDefault("sim.status.age_prefix", "Age: ") + ageName);
    }

    public void updateDatabaseStatus(boolean online) {
        this.isDbOnline = online;
        refreshStatusBadge();
    }

    public void rebuildLayerMenuItems() {
        if (layersSection != null) {
            layersSection.rebuildLayerMenuItems();
        }
    }

    public void updateActiveLayerUI() {
        if (layersSection != null) {
            layersSection.updateActiveLayerUI();
        }
    }

    public void updateClusterStatus(String nodeRole, String hostPort, boolean isConnected) {
        if (isConnected) {
            this.clusterNodeState = String.format("🌐 %s (%s)", nodeRole, hostPort);
        } else {
            this.clusterNodeState = I18n.getOrDefault("sim.status.local_node", "💻 Standalone Local Node");
        }
        refreshStatusBadge();
    }

    private void refreshStatusBadge() {
        if (dbStatusLabel == null) return;
        String dbOnlineStr = I18n.getOrDefault("sim.status.db_online", "🟢 Supabase DB: Online");
        String dbOfflineStr = I18n.getOrDefault("sim.status.db_offline", "⚪ DB: Offline Mode (Local Files)");
        String dbStr = isDbOnline ? dbOnlineStr : dbOfflineStr;
        dbStatusLabel.setText(dbStr + " | " + clusterNodeState);
        dbStatusLabel.setStyle(isDbOnline ? "-fx-font-size: 11px; -fx-text-fill: #4ade80; -fx-font-weight: bold;" : "-fx-font-size: 11px; -fx-text-fill: #38bdf8;");
    }

    private void updateTexts() {
        if (playPauseBtn != null) {
            boolean running = engine != null && engine.isRunning();
            playPauseBtn.setText(running ? "⏸" : "▶");
            playPauseBtn.setTooltip(new Tooltip(running
                    ? I18n.getOrDefault("sim.tooltip.pause", "Pause")
                    : I18n.getOrDefault("sim.tooltip.start", "Start / Resume")));
        }
        if (timeTitle != null) {
            timeTitle.setText("⏱️ " + I18n.getOrDefault("sim.card.time", "TIME & PLAYBACK CONTROLS"));
        }
        if (pauseOnEventCheck != null) {
            pauseOnEventCheck.setText(I18n.getOrDefault("sim.option.pause_on_event", "⏸️ Auto-pause on event"));
            pauseOnEventCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.pause_on_event", "Automatically pauses simulation when a new planetary or regional event occurs")));
        }

        rewindBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.rewind", "Reset to T=0 — Pauses simulation")));
        fastRewindBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastrewind", "Previous snapshot / Fast rewind — Pauses simulation")));
        stepBackBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stepback", "Step backward / previous snapshot — Pauses simulation")));
        stepForwardBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stepforward", "Step forward / next snapshot — Pauses simulation")));
        fastForwardBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastforward", "Fast forward / Next snapshot — Pauses simulation")));
        endBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastforward_end", "Jump to latest snapshot / End — Pauses simulation")));
        speedMax.setText("MAX 🚀");
        speedMax.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.speed_max", "Runs iterations at maximum processor capacity")));
        speedSlider.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.speed_slider", "CPU simulation speed (0.1 to 20 steps/sec or MAX)")));
        updateSpeedLabel(speedMax != null && speedMax.isSelected() ? 999.0 : (speedSlider != null ? speedSlider.getValue() : 1.0));

        if (layersSection != null) layersSection.updateTexts();
        if (renderingSection != null) renderingSection.updateTexts();
        if (captureSection != null) captureSection.updateTexts();
        if (eventsSection != null) eventsSection.updateTexts();

        updateViewToggleButton();
        updateDisplayToggleButton();
    }

    private void updateViewToggleButton() {
        if (renderingSection != null && mapCanvas != null) {
            renderingSection.updateViewToggleButton(mapCanvas);
        }
    }

    private void updateDisplayToggleButton() {
        updateActiveLayerUI();
    }
}
