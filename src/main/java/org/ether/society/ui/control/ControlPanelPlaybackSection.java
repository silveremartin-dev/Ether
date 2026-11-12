/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.control;

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
import org.ether.society.i18n.I18n;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Temporal clock, scenario/date status header, DB/cluster telemetry badge, and playback speed
 * card for {@link org.ether.society.ui.ControlPanel}.
 *
 * <p>Extracted from {@code ControlPanel} in accordance with the Single Responsibility Principle (SRP)
 * to isolate transport buttons (rewind, step, play/pause, fast-forward, seek-to-end), logarithmic/piecewise
 * speed scaling, and database/cluster connection status rendering.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class ControlPanelPlaybackSection {

    private final ISimulationEngine engine;
    private final VBox card;

    // Header & Telemetry Labels
    private final Label scenarioHeaderLabel;
    private final Label dateHeaderLabel;
    private final Label dbStatusLabel;
    private final Label ageLabel;
    private final Label seasonLabel;
    private final Label popStatValue;
    private final Label foodStatValue;
    private final Label cellStatValue;

    // Playback Controls
    private final Label timeTitle;
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

    // Cluster & DB state
    private boolean isDbOnline = false;
    private String clusterNodeState = "💻 Nœud Local Standalone";

    // Callbacks
    private ControlPanelCaptureSection captureSection;
    private Runnable toggleVideoRecordingAction;
    private Consumer<Integer> onTimelapseSeek;
    private Runnable onTimelapseSeekToEnd;
    private Runnable onRewindStart;
    private Runnable onFastRewind;
    private Runnable onStepBack;
    private Runnable onStepForward;
    private Runnable onFastForward;
    private Runnable onSeekToEnd;

    /**
     * Constructs the playback, temporal resolution, and telemetry header card.
     *
     * @param engine active simulation engine instance
     */
    public ControlPanelPlaybackSection(ISimulationEngine engine) {
        this.engine = engine;

        scenarioHeaderLabel = new Label("🎬 " + I18n.getOrDefault("sim.header.scenario", "Scenario: ") + "Out of Africa");
        scenarioHeaderLabel.getStyleClass().add("sidebar-recap-highlight");
        scenarioHeaderLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        scenarioHeaderLabel.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.scenario_name", "Name of current historical or procedural scenario")));

        dateHeaderLabel = new Label("📅 " + I18n.getOrDefault("sim.header.date", "Date : ") + "An -100000");
        dateHeaderLabel.getStyleClass().add("sidebar-title");
        dateHeaderLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        dateHeaderLabel.setTooltip(new Tooltip(
                I18n.getOrDefault("sim.tooltip.date_header", "Current simulation clock according to configured time step.")));

        dbStatusLabel = new Label(I18n.getOrDefault("sim.status.dbcheck", "DB: Checking..."));
        dbStatusLabel.getStyleClass().add("control-label");
        dbStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-padding: 2 0 0 0;");

        pauseOnEventCheck = new CheckBox(I18n.getOrDefault("sim.option.pause_on_event", "⏸️ Auto-pause on event"));
        pauseOnEventCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.pause_on_event", "Automatically pauses simulation when a new planetary or regional event occurs")));
        pauseOnEventCheck.getStyleClass().add("opt-sub-checkbox");
        pauseOnEventCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        pauseOnEventCheck.setOnAction(e -> engine.setPauseAtNextEvent(pauseOnEventCheck.isSelected()));

        timeTitle = new Label("⏱️ " + I18n.getOrDefault("sim.card.time", "TIME & PLAYBACK CONTROLS"));
        timeTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        rewindBtn = new Button("⏮");
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
        fastRewindBtn.setOnAction(e -> {
            engine.pause();
            updatePlayPauseVisuals(false);
            if (onFastRewind != null) onFastRewind.run();
            else engine.stepBackward(100);
            updatePlayPauseVisuals(false);
        });

        stepBackBtn = new Button("⏴");
        stepBackBtn.setOnAction(e -> {
            engine.pause();
            updatePlayPauseVisuals(false);
            if (onStepBack != null) onStepBack.run();
            else engine.stepBackward(10);
            updatePlayPauseVisuals(false);
        });

        playPauseBtn = new Button("▶");
        playPauseBtn.setOnAction(e -> {
            if (engine.isRunning()) {
                engine.pause();
                if (captureSection != null && captureSection.isAutoRecordSelected()
                        && captureSection.isRecordingVideo() && toggleVideoRecordingAction != null) {
                    toggleVideoRecordingAction.run();
                }
                updatePlayPauseVisuals(false);
            } else {
                if (captureSection != null && captureSection.isAutoRecordSelected()
                        && !captureSection.isRecordingVideo() && toggleVideoRecordingAction != null) {
                    toggleVideoRecordingAction.run();
                }
                engine.start();
                updatePlayPauseVisuals(true);
            }
        });

        stepForwardBtn = new Button("⏵");
        stepForwardBtn.setOnAction(e -> {
            engine.pause();
            updatePlayPauseVisuals(false);
            if (onStepForward != null) onStepForward.run();
            else engine.stepForward(10);
            updatePlayPauseVisuals(false);
        });

        fastForwardBtn = new Button("⏩");
        fastForwardBtn.setOnAction(e -> {
            engine.pause();
            updatePlayPauseVisuals(false);
            if (onFastForward != null) onFastForward.run();
            else engine.stepForward(100);
            updatePlayPauseVisuals(false);
        });

        endBtn = new Button("⏭");
        endBtn.setOnAction(e -> {
            engine.pause();
            updatePlayPauseVisuals(false);
            if (onSeekToEnd != null) onSeekToEnd.run();
            else if (onTimelapseSeekToEnd != null) onTimelapseSeekToEnd.run();
            else engine.seekToEnd();
            updatePlayPauseVisuals(false);
        });

        Button[] playButtons = {rewindBtn, fastRewindBtn, stepBackBtn, playPauseBtn, stepForwardBtn, fastForwardBtn, endBtn};
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
        HBox.setHgrow(speedSlider, Priority.ALWAYS);

        speedValueLabel = new Label("⏱️ " + I18n.getOrDefault("sim.speed.label", "Speed : 1 step/sec"));
        speedValueLabel.getStyleClass().add("value-label");
        speedValueLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        speedMax = new ToggleButton("MAX 🚀");
        updateSpeedMaxStyle(false);

        speedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double spd = quantizeSpeed(newV.doubleValue());
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
                double spd = quantizeSpeed(speedSlider.getValue());
                engine.setSpeed(spd);
                updateSpeedLabel(spd);
            }
        });

        updateSpeedLabel(1.0);

        HBox sliderRow = new HBox(8, speedSlider, speedMax);
        sliderRow.setAlignment(Pos.TOP_LEFT);
        HBox.setMargin(speedMax, new Insets(2, 0, 0, 0));

        card = new VBox(8, scenarioHeaderLabel, dateHeaderLabel, dbStatusLabel, timeTitle, playBar, pauseOnEventCheck, speedValueLabel, sliderRow);
        card.getStyleClass().add("card-section");

        popStatValue = new Label();
        foodStatValue = new Label();
        cellStatValue = new Label();
        ageLabel = new Label(I18n.getOrDefault("sim.age.stone_age", "Age: Stone Age"));
        ageLabel.getStyleClass().add("control-label");
        seasonLabel = new Label(I18n.getOrDefault("sim.season.spring", "Season: Spring"));
        seasonLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
    }

    private static double quantizeSpeed(double raw) {
        if (raw < 1.0) return Math.max(0.1, Math.round(raw * 10.0) / 10.0);
        if (raw <= 5.0) return Math.round(raw * 2.0) / 2.0;
        return Math.round(raw);
    }

    /** Returns the assembled JavaFX playback card. */
    public VBox getCard() { return card; }

    /** Returns the event auto-pause checkbox. */
    public CheckBox getPauseOnEventCheck() { return pauseOnEventCheck; }

    /** Binds the capture section used for synchronized auto-recording on play/pause. */
    public void bindCaptureControls(ControlPanelCaptureSection section, Runnable toggleVideoAction) {
        this.captureSection = section;
        this.toggleVideoRecordingAction = toggleVideoAction;
    }

    public void setOnTimelapseSeek(Consumer<Integer> cb) { this.onTimelapseSeek = cb; }
    public void setOnTimelapseSeekToEnd(Runnable cb) { this.onTimelapseSeekToEnd = cb; }
    public void setOnRewindStart(Runnable cb) { this.onRewindStart = cb; }
    public void setOnFastRewind(Runnable cb) { this.onFastRewind = cb; }
    public void setOnStepBack(Runnable cb) { this.onStepBack = cb; }
    public void setOnStepForward(Runnable cb) { this.onStepForward = cb; }
    public void setOnFastForward(Runnable cb) { this.onFastForward = cb; }
    public void setOnSeekToEnd(Runnable cb) { this.onSeekToEnd = cb; }

    /**
     * Updates play/pause button icon, high-contrast styling, and localized tooltip.
     *
     * @param isRunning {@code true} if the simulation engine is actively ticking
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
        String spdStr = formatCompact(spd);

        String timeUnit;
        if (stepDays >= 360.0) {
            double yearsPerSec = simDaysPerSec / 365.0;
            timeUnit = Math.abs(yearsPerSec - 1.0) < 0.001
                    ? I18n.getOrDefault("sim.speed.unit.year_singular", "1 an/sec")
                    : String.format(I18n.getOrDefault("sim.speed.unit.year_plural", "%s ans/sec"), formatCompact(yearsPerSec));
        } else if (stepDays >= 28.0) {
            double monthsPerSec = simDaysPerSec / 30.0;
            timeUnit = Math.abs(monthsPerSec - 1.0) < 0.001
                    ? I18n.getOrDefault("sim.speed.unit.month_singular", "1 mois/sec")
                    : String.format(I18n.getOrDefault("sim.speed.unit.month_plural", "%s mois/sec"), formatCompact(monthsPerSec));
        } else {
            timeUnit = Math.abs(simDaysPerSec - 1.0) < 0.001
                    ? I18n.getOrDefault("sim.speed.unit.day_singular", "1 jour/sec")
                    : String.format(I18n.getOrDefault("sim.speed.unit.day_plural", "%s jours/sec"), formatCompact(simDaysPerSec));
        }
        String fmt = I18n.getOrDefault("sim.speed.target_custom_fmt", "Vitesse Cible : %s pas/sec (%s)");
        speedValueLabel.setText("⏱️ " + String.format(fmt, spdStr, timeUnit));
    }

    private static String formatCompact(double v) {
        return (Math.abs(v - Math.round(v)) < 0.001) ? String.valueOf(Math.round(v)) : String.format(Locale.US, "%.1f", v);
    }

    public void updateTimelapseSlider(int currentYear) {
        dateHeaderLabel.setText("📅 " + I18n.getOrDefault("sim.header.date", "Date : ") + String.format(Locale.ROOT, "An %d", currentYear));
    }

    public void updateScenarioName(String scenarioName) {
        if (scenarioName != null && !scenarioName.isBlank()) {
            scenarioHeaderLabel.setText(I18n.getOrDefault("sim.header.scenario", "🎬 Scenario: ") + scenarioName);
        }
    }

    public void updateYear(String year) {
        dateHeaderLabel.setText("📅 " + I18n.getOrDefault("sim.header.date", "Date : ") + year);
    }

    public void updateStats(long population, double food, long populatedCells) {
        popStatValue.setText(String.format(I18n.getOrDefault("sim.status.pop_total", "Pop. Totale : %s"), formatNumber(population)));
        foodStatValue.setText(String.format(I18n.getOrDefault("sim.status.food_stocks", "Stocks Alim. : %s"), formatNumber((long) food)));
        cellStatValue.setText(String.format(I18n.getOrDefault("sim.status.populated_cells", "Populated Cells: %,d"), populatedCells));
    }

    private static String formatNumber(long num) {
        if (num >= 1_000_000_000L) return String.format("%.2f Md", num / 1_000_000_000.0);
        if (num >= 1_000_000L) return String.format("%.2f M", num / 1_000_000.0);
        if (num >= 1_000L) return String.format("%.1f k", num / 1_000.0);
        return String.valueOf(num);
    }

    public void updateSeason(int month) {
        String[] seasonNames = {
                I18n.getOrDefault("sim.season.winter", "Hiver ❄️"),
                I18n.getOrDefault("sim.season.spring", "Printemps 🌿"),
                I18n.getOrDefault("sim.season.summer", "Été ☀️"),
                I18n.getOrDefault("sim.season.autumn", "Automne 🍂")
        };
        String[] seasonColors = {"#64b5f6", "#4ade80", "#facc15", "#fb923c"};
        int idx = (month == 11 || month == 0 || month == 1) ? 0 : (month <= 4 ? 1 : (month <= 7 ? 2 : 3));
        seasonLabel.setText(I18n.getOrDefault("sim.status.season_prefix", "Season: ") + seasonNames[idx]);
        seasonLabel.setStyle("-fx-text-fill: " + seasonColors[idx] + "; -fx-font-weight: bold;");
    }

    public void updateAge(String ageName) {
        ageLabel.setText(I18n.getOrDefault("sim.status.age_prefix", "Age: ") + ageName);
    }

    public void updateDatabaseStatus(boolean online) {
        this.isDbOnline = online;
        refreshStatusBadge();
    }

    public void updateClusterStatus(String nodeRole, String hostPort, boolean isConnected) {
        this.clusterNodeState = isConnected
                ? String.format("🌐 %s (%s)", nodeRole, hostPort)
                : I18n.getOrDefault("sim.status.local_node", "💻 Standalone Local Node");
        refreshStatusBadge();
    }

    private void refreshStatusBadge() {
        String dbOnlineStr = I18n.getOrDefault("sim.status.db_online", "🟢 Supabase DB: Online");
        String dbOfflineStr = I18n.getOrDefault("sim.status.db_offline", "⚪ DB: Offline Mode (Local Files)");
        dbStatusLabel.setText((isDbOnline ? dbOnlineStr : dbOfflineStr) + " | " + clusterNodeState);
        dbStatusLabel.setStyle(isDbOnline ? "-fx-font-size: 11px; -fx-text-fill: #4ade80; -fx-font-weight: bold;" : "-fx-font-size: 11px; -fx-text-fill: #38bdf8;");
    }

    /** Refreshes localized captions and tooltips across all playback controls. */
    public void updateTexts() {
        boolean running = engine != null && engine.isRunning();
        playPauseBtn.setText(running ? "⏸" : "▶");
        playPauseBtn.setTooltip(new Tooltip(running
                ? I18n.getOrDefault("sim.tooltip.pause", "Pause")
                : I18n.getOrDefault("sim.tooltip.start", "Start / Resume")));
        timeTitle.setText("⏱️ " + I18n.getOrDefault("sim.card.time", "TIME & PLAYBACK CONTROLS"));
        pauseOnEventCheck.setText(I18n.getOrDefault("sim.option.pause_on_event", "⏸️ Auto-pause on event"));
        pauseOnEventCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.pause_on_event", "Automatically pauses simulation when a new planetary or regional event occurs")));

        rewindBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.rewind", "Reset to T=0 — Pauses simulation")));
        fastRewindBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastrewind", "Previous snapshot / Fast rewind — Pauses simulation")));
        stepBackBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stepback", "Step backward / previous snapshot — Pauses simulation")));
        stepForwardBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.stepforward", "Step forward / next snapshot — Pauses simulation")));
        fastForwardBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastforward", "Fast forward / Next snapshot — Pauses simulation")));
        endBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fastforward_end", "Jump to latest snapshot / End — Pauses simulation")));
        speedMax.setText("MAX 🚀");
        speedMax.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.speed_max", "Runs iterations at maximum processor capacity")));
        speedSlider.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.speed_slider", "CPU simulation speed (0.1 to 20 steps/sec or MAX)")));
        updateSpeedLabel(speedMax.isSelected() ? 999.0 : speedSlider.getValue());
    }
}
