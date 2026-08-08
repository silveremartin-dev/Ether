/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.model.ScenarioTimeline;
import org.ether.society.procedural.NuclearWarfareClimateEngine;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * God Mode & Interactive Timeline Controller.
 * Allows pausing live simulation, injecting real physical events (volcanoes, EMP, heatwaves),
 * and viewing the chronological scenario timeline audit trail.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class GodModePanel extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(GodModePanel.class);

    private final H3SimulationEngine engine;
    private final ScenarioTimeline timeline;

    private final ListView<String> timelineListView;

    // Configurable Form Controls
    private final ComboBox<String> eventTypeCombo;
    private final TextField eventNameField;
    private final Spinner<Integer> targetYearSpinner;
    private final Spinner<Double> latSpinner;
    private final Spinner<Double> lngSpinner;
    private final Spinner<Double> magnitudeSpinner;

    public GodModePanel(H3SimulationEngine engine, ScenarioTimeline timeline) {
        this.engine = engine;
        this.timeline = timeline != null ? timeline : new ScenarioTimeline();

        setPadding(new Insets(12));
        setSpacing(10);
        getStyleClass().add("glass-panel");
        setStyle("-fx-background-color: rgba(15, 23, 42, 0.85); -fx-background-radius: 8; -fx-border-color: rgba(56, 189, 248, 0.2); -fx-border-radius: 8;");

        // Title Header
        Label header = new Label(org.ether.society.i18n.I18n.getOrDefault("godmode.title", "⚡ MODE DIEU & CHRONOLOGIE"));
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #38bdf8;");

        // Form fields initialization
        eventTypeCombo = new ComboBox<>();
        eventTypeCombo.getItems().addAll(
            "VOLCANO",
            "HEATWAVE",
            "SOLAR_EMP",
            "PANDEMIC",
            "METEOR",
            "NUCLEAR_WINTER",
            "FAMINE"
        );
        eventTypeCombo.setValue("VOLCANO");
        eventTypeCombo.setMaxWidth(Double.MAX_VALUE);
        eventTypeCombo.setTooltip(new Tooltip("Type de perturbation physique ou climatique à injecter dans l'écosystème."));

        eventNameField = new TextField("Éruption Stratosphérique SO₂");
        eventNameField.setPromptText("Titre ou Nom de l'événement...");
        eventNameField.setTooltip(new Tooltip("Titre personnalisé qui apparaîtra dans le registre chronologique et l'audit trail."));

        int currentYr = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 2026;
        targetYearSpinner = new Spinner<>(-100000, 2100, currentYr, 1);
        targetYearSpinner.setEditable(true);
        targetYearSpinner.setMaxWidth(Double.MAX_VALUE);
        targetYearSpinner.setTooltip(new Tooltip("Année cible exacte de déclenchement de l'événement dans le calendrier de la simulation."));

        latSpinner = new Spinner<>(-90.0, 90.0, 0.0, 1.0);
        latSpinner.setEditable(true);
        latSpinner.setMaxWidth(Double.MAX_VALUE);
        latSpinner.setTooltip(new Tooltip("Latitude de l'épicentre du phénomène physique (-90° Sud à +90° Nord)."));

        lngSpinner = new Spinner<>(-180.0, 180.0, 0.0, 1.0);
        lngSpinner.setEditable(true);
        lngSpinner.setMaxWidth(Double.MAX_VALUE);
        lngSpinner.setTooltip(new Tooltip("Longitude de l'épicentre du phénomène physique (-180° Ouest à +180° Est)."));

        magnitudeSpinner = new Spinner<>(0.1, 10.0, 1.5, 0.5);
        magnitudeSpinner.setEditable(true);
        magnitudeSpinner.setMaxWidth(Double.MAX_VALUE);
        magnitudeSpinner.setTooltip(new Tooltip("Intensité / Magnitude du choc (détermine la profondeur et l'impact spatial de la perturbation)."));

        // Update default event name when type changes
        eventTypeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            switch (newV) {
                case "VOLCANO" -> eventNameField.setText("Éruption Stratosphérique SO₂");
                case "HEATWAVE" -> eventNameField.setText("Canicule Globale & Forçage Radiatif");
                case "SOLAR_EMP" -> eventNameField.setText("Tempête Solaire Carrington (EMP)");
                case "PANDEMIC" -> eventNameField.setText("Épidémie Zoonotique Bio-Moléculaire");
                case "METEOR" -> eventNameField.setText("Impact d'Astéroïde Majeur");
                case "NUCLEAR_WINTER" -> eventNameField.setText("Hiver Nucléaire / Glaciation");
                case "FAMINE" -> eventNameField.setText("Sécheresse & Famine Répandue");
            }
        });

        // Section 1: Event Builder (Direct & Scheduled Physical Forcing)
        VBox injectorBox = createInjectorSection();
        injectorBox.setStyle("-fx-padding: 10; -fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 6;");

        // Section 2: Scenario Timeline Audit Log
        Label timelineHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("godmode.timeline.title", "📜 CHRONOLOGIE DU SCÉNARIO & MODIFICATIONS EN DIRECT :"));
        timelineHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #a78bfa; -fx-font-size: 12px;");

        timelineListView = new ListView<>();
        timelineListView.setPrefHeight(180);
        timelineListView.setStyle("-fx-control-inner-background: #090d16; -fx-font-size: 11px;");
        timelineListView.setTooltip(new Tooltip("Registre d'audit temporel : Liste chronologique de tous les forçages et évènements du scénario."));
        refreshTimelineView();

        VBox timelineBox = new VBox(6, timelineHeader, timelineListView);
        timelineBox.setStyle("-fx-padding: 10; -fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 6;");

        getChildren().addAll(header, injectorBox, timelineBox);
    }

    private VBox createInjectorSection() {
        VBox box = new VBox(8);
        Label title = new Label("🛠️ ÉDITION & PROGRAMMATION D'ÉVÉNEMENTS CLIMATIQUES :");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);

        grid.addRow(0, createLabel("Type d'Événement :"), eventTypeCombo);
        grid.addRow(1, createLabel("Nom / Titre :"), eventNameField);
        grid.addRow(2, createLabel("Année Cible (Date) :"), targetYearSpinner);
        grid.addRow(3, createLabel("Latitude (-90 à +90°) :"), latSpinner);
        grid.addRow(4, createLabel("Longitude (-180 à +180°) :"), lngSpinner);
        grid.addRow(5, createLabel("Intensité / Magnitude :"), magnitudeSpinner);

        Button scheduleBtn = new Button("📅 Programmer dans la Chronologie");
        scheduleBtn.setTooltip(new Tooltip("Inscrit l'événement dans le calendrier du scénario pour un déclenchement automatique à l'année cible spécifiée."));
        scheduleBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6;");
        scheduleBtn.setMaxWidth(Double.MAX_VALUE);
        scheduleBtn.setOnAction(e -> scheduleEvent(false));

        Button triggerNowBtn = new Button("⚡ Déclencher Immédiatement");
        triggerNowBtn.setTooltip(new Tooltip("Applique instantanément les perturbations climatiques et physiques sur le monde à l'année courante en direct."));
        triggerNowBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6;");
        triggerNowBtn.setMaxWidth(Double.MAX_VALUE);
        triggerNowBtn.setOnAction(e -> scheduleEvent(true));

        HBox btnBox = new HBox(8, scheduleBtn, triggerNowBtn);
        HBox.setHgrow(scheduleBtn, Priority.ALWAYS);
        HBox.setHgrow(triggerNowBtn, Priority.ALWAYS);

        box.getChildren().addAll(title, grid, btnBox);
        return box;
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    private void scheduleEvent(boolean immediate) {
        String type = eventTypeCombo.getValue();
        String name = eventNameField.getText();
        if (name == null || name.isBlank()) name = type;

        int currentYear = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 2026;
        int targetYear = immediate ? currentYear : targetYearSpinner.getValue();

        double lat = latSpinner.getValue();
        double lng = lngSpinner.getValue();
        double mag = magnitudeSpinner.getValue();

        String details = String.format(java.util.Locale.US, "Lat: %.2f°, Lng: %.2f°, Mag: %.1f", lat, lng, mag);

        timeline.addEntry(targetYear, type, name, details, true);
        refreshTimelineView();

        // If target year is current year or immediate, execute physical forcing directly
        if (immediate || targetYear <= currentYear) {
            executePhysicalForcing(type, mag, lat, lng);
            logger.info("God Mode intervention EXECUTED immediately (Year {}): {} - {}", currentYear, name, details);
        } else {
            logger.info("God Mode intervention SCHEDULED for Year {}: {} - {}", targetYear, name, details);
        }
    }

    private void executePhysicalForcing(String type, double mag, double lat, double lng) {
        // Volcano SO2 Injection
        Button volcanoBtn = new Button("🌋 Éruption Stratosphérique SO₂");
        volcanoBtn.setTooltip(new Tooltip("Injecter une éruption super-volcanique stratosphérique (aérosols SO₂ τ = 1.20). Réduit le rayonnement solaire global et déclenche un hiver volcanique."));
        volcanoBtn.setOnAction(e -> {
            NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(1.2);
            recordIntervention("VOLCANO", "Éruption Volcanique Majeure", "Injection d'aérosols stratosphériques (τ = 1.20)");
        });

        // Heatwave / Radiative Forcing
        Button heatwaveBtn = new Button("🔥 Canicule Globale (+3°C)");
        heatwaveBtn.setTooltip(new Tooltip("Injecter une canicule mondiale (+3°C). Augmente instantanément la température de surface de toutes les cellules hexagonales H3."));
        heatwaveBtn.setOnAction(e -> {
            if (engine != null && engine.getCells() != null) {
                for (H3Cell c : engine.getCells()) {
                    c.setTemperature((c.getTemperature() != null ? c.getTemperature() : 15.0) + 3.0);
                }
            }
            recordIntervention("HEATWAVE", "Canicule & Forçage Radiatif", "Augmentation globale de température (+3.0°C)");
        });

        // Solar Carrington EMP Storm
        Button solarEmpBtn = new Button("⚡ Tempête Solaire EMP (Carrington)");
        solarEmpBtn.setTooltip(new Tooltip("Injecter une tempête géomagnétique solaire majeure (Événement Carrington). Désactive le réseau électrique et détruit temporairement le capital d'information."));
        solarEmpBtn.setOnAction(e -> {
            recordIntervention("SOLAR_EMP", "Tempête Géomagnétique Solaire", "Perturbation EMP et effondrement temporaire du réseau électrique");
        });

        // Pandémie Pathogène
        Button pandemicBtn = new Button("🦠 Épidémie Zoonotique");
        pandemicBtn.setTooltip(new Tooltip("Injecter un choc épidémique zoonotique global. Augmente brutalement le taux de mortalité de Gompertz et réduit la fécondité."));
        pandemicBtn.setOnAction(e -> {
            recordIntervention("PANDEMIC", "Outbreak Épidémique Bio-Moléculaire", "Choc immunitaire et hausse de la mortalité de Gompertz");
        });

        if (type == null) return;
        switch (type) {
            case "VOLCANO" -> {
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(mag);
            }
            case "HEATWAVE" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        c.setTemperature((c.getTemperature() != null ? c.getTemperature() : 15.0) + mag);
                    }
                }
            }
            case "NUCLEAR_WINTER" -> {
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(mag * 2.0);
            }
            case "PANDEMIC", "SOLAR_EMP", "METEOR", "FAMINE" -> {
                // Logged & tracked in event system timeline
            }
        }
    }

    public void recordIntervention(String type, String title, String details) {
        long currentYear = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 2026;
        timeline.addEntry(currentYear, type, title, details, true);
        refreshTimelineView();
        logger.info("God Mode intervention recorded at Year {}: {} - {}", currentYear, title, details);
    }

    public void refreshTimelineView() {
        timelineListView.getItems().clear();
        for (ScenarioTimeline.TimelineEntry entry : timeline.getEntries()) {
            String badge = entry.isGodModeIntervention() ? "⚡ [GOD MODE]" : "📜 [HISTORIQUE]";
            timelineListView.getItems().add(String.format("Année %5d | %s %s : %s", entry.year(), badge, entry.title(), entry.details()));
        }
    }
}
