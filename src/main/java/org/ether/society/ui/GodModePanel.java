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

        // Section 2: Direct Resource & Demographic Spawner
        VBox spawnerBox = createSpawnerSection();
        spawnerBox.setStyle("-fx-padding: 10; -fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 6;");

        // Section 3: Spatial Terraform Brush
        VBox terraformBox = createTerraformSection();
        terraformBox.setStyle("-fx-padding: 10; -fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 6;");

        // Section 4: Disaster Reset & Climate Normalization
        VBox resetBox = createResetSection();
        resetBox.setStyle("-fx-padding: 10; -fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 6;");

        // Section 5: Scenario Timeline Audit Log & Scheduled Queue Actions
        Label timelineHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("godmode.timeline.title", "📜 CHRONOLOGIE DU SCÉNARIO & REGISTRE D'AUDIT EN DIRECT :"));
        timelineHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #a78bfa; -fx-font-size: 12px;");

        timelineListView = new ListView<>();
        timelineListView.setPrefHeight(160);
        timelineListView.setStyle("-fx-control-inner-background: #090d16; -fx-font-size: 11px;");
        timelineListView.setTooltip(new Tooltip("Registre d'audit temporel : Liste chronologique de tous les forçages et évènements du scénario."));
        refreshTimelineView();

        Button btnClearTimeline = new Button("🗑️ Effacer l'Historique");
        btnClearTimeline.getStyleClass().add("button-secondary");
        btnClearTimeline.setStyle("-fx-font-size: 10px; -fx-text-fill: #f87171;");
        btnClearTimeline.setOnAction(e -> {
            timeline.getEntries().clear();
            refreshTimelineView();
        });

        HBox timelineActionsBox = new HBox(8, btnClearTimeline);
        timelineActionsBox.setAlignment(Pos.CENTER_RIGHT);

        VBox timelineBox = new VBox(6, timelineHeader, timelineListView, timelineActionsBox);
        timelineBox.setStyle("-fx-padding: 10; -fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 6;");

        getChildren().addAll(header, injectorBox, spawnerBox, terraformBox, resetBox, timelineBox);
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

    private VBox createSpawnerSection() {
        VBox box = new VBox(8);
        Label title = new Label("🌱 INJECTION DIRECTE DE POPULATION & RESSOURCES :");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #10b981;");

        Button injectPopBtn = new Button("👥 Injecter 100 000 Habitants (Épicentre)");
        injectPopBtn.setTooltip(new Tooltip("Injecte une cohorte de 100 000 habitants à la position géographique spécifiée par les spinners Lat/Lng."));
        injectPopBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 4;");
        injectPopBtn.setMaxWidth(Double.MAX_VALUE);
        injectPopBtn.setOnAction(e -> {
            recordIntervention("POP_INJECT", "Injection Démographique", "Ajout de +100,000 habitants aux coordonnées (Lat: " + latSpinner.getValue() + ", Lng: " + lngSpinner.getValue() + ")");
        });

        Button injectFoodBtn = new Button("🌾 Injecter Stock Alimentaire (Silos)");
        injectFoodBtn.setTooltip(new Tooltip("Remplit les stocks alimentaires à 100% pour éviter les famines immédiates."));
        injectFoodBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 4;");
        injectFoodBtn.setMaxWidth(Double.MAX_VALUE);
        injectFoodBtn.setOnAction(e -> {
            recordIntervention("FOOD_INJECT", "Injection Alimentaire", "Remplissage des stocks céréaliers mondiaux (+12 mois)");
        });

        Button massExtinctionBtn = new Button("💀 Déclencher Extinction Massive (Extinction 50%)");
        massExtinctionBtn.setTooltip(new Tooltip("Réduit instantanément de 50% la population mondiale active (Choc de Cataclysme)."));
        massExtinctionBtn.setStyle("-fx-background-color: #991b1b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 4;");
        massExtinctionBtn.setMaxWidth(Double.MAX_VALUE);
        massExtinctionBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation d'Extinction Massive");
            alert.setHeaderText("⚠️ Action Destructive en Mode Dieu");
            alert.setContentText("Êtes-vous sûr de vouloir éliminer 50% de la population mondiale ? Cette intervention sera enregistrée de façon irréversible dans l'audit trail.");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    recordIntervention("MASS_EXTINCTION", "Extinction Cataclysmique", "Réduction immédiate de 50% de la biomasse humaine mondiale");
                }
            });
        });

        HBox btnGrid = new HBox(8, injectPopBtn, injectFoodBtn);
        HBox.setHgrow(injectPopBtn, Priority.ALWAYS);
        HBox.setHgrow(injectFoodBtn, Priority.ALWAYS);

        box.getChildren().addAll(title, btnGrid, massExtinctionBtn);
        return box;
    }

    private VBox createTerraformSection() {
        VBox box = new VBox(8);
        Label title = new Label("🖌️ PINCEAU SPATIAL & DYNAMIQUES LOCALES :");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        ComboBox<String> brushModeCombo = new ComboBox<>();
        brushModeCombo.getItems().addAll(
            "👥 Boost Population (+50 000 hab)",
            "🌾 Injection Agricole & Silos (+500 t)",
            "🚰 Recharge Nappe Aquifère (+2 000 m³)",
            "🔥 Vague de Chaleur Locale (+10.0°C)",
            "❄️ Refroidissement Local (-10.0°C)",
            "🧼 Dépollution Écologique Total (0.0)"
        );
        brushModeCombo.setValue("👥 Boost Population (+50 000 hab)");
        brushModeCombo.setMaxWidth(Double.MAX_VALUE);

        Button applyBrushBtn = new Button("🖌️ Appliquer aux Coordonnées Épicentre");
        applyBrushBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6;");
        applyBrushBtn.setMaxWidth(Double.MAX_VALUE);
        applyBrushBtn.setOnAction(e -> {
            if (engine == null || engine.getCells() == null) return;
            double targetLat = latSpinner.getValue();
            double targetLng = lngSpinner.getValue();
            String mode = brushModeCombo.getValue();

            // Find nearest cell
            H3Cell nearest = null;
            double minDist = Double.MAX_VALUE;
            for (H3Cell c : engine.getCells()) {
                double dist = Math.hypot(c.getLatitude() - targetLat, c.getLongitude() - targetLng);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = c;
                }
            }

            if (nearest != null) {
                if (mode.contains("Population")) nearest.setPopulation(nearest.getPopulation() + 50000);
                else if (mode.contains("Agricole")) nearest.setFoodResource((nearest.getFoodResource() != null ? nearest.getFoodResource() : 0.0) + 500.0);
                else if (mode.contains("Aquifère")) nearest.setFreshwaterAquifer((nearest.getFreshwaterAquifer() != null ? nearest.getFreshwaterAquifer() : 0.0) + 2000.0);
                else if (mode.contains("Chaleur")) nearest.setTemperature((nearest.getTemperature() != null ? nearest.getTemperature() : 15.0) + 10.0);
                else if (mode.contains("Refroidissement")) nearest.setTemperature((nearest.getTemperature() != null ? nearest.getTemperature() : 15.0) - 10.0);
                else if (mode.contains("Dépollution")) nearest.setPollutionLevel(0.0);

                recordIntervention("TERRAFORM_BRUSH", "Pinceau Spatial", "Action '" + mode + "' appliquée sur la maille Lat " + String.format("%.2f", nearest.getLatitude()) + "°, Lng " + String.format("%.2f", nearest.getLongitude()) + "°");
            }
        });

        box.getChildren().addAll(title, brushModeCombo, applyBrushBtn);
        return box;
    }

    private VBox createResetSection() {
        VBox box = new VBox(8);
        Label title = new Label("🛑 NORMALISATION & RÉINITIALISATION PHYSIQUE :");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #f59e0b;");

        Button resetDisastersBtn = new Button("🛑 Stopper Tous les Désastres & Dissiper l'Ombre Stratosphérique");
        resetDisastersBtn.setTooltip(new Tooltip("Réinitialise la profondeur optique de la suie stratosphérique (τ = 0.0) et annule les perturbations caniculaires/volcaniques actives."));
        resetDisastersBtn.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6;");
        resetDisastersBtn.setMaxWidth(Double.MAX_VALUE);
        resetDisastersBtn.setOnAction(e -> {
            NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(0.0);
            recordIntervention("RESET_CLIMATE", "Dissipation des Aérosols", "Retour à l'équilibre climatique et transparence stratosphérique standard (τ = 0.0)");
        });

        box.getChildren().addAll(title, resetDisastersBtn);
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
        int currentMonth = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentMonth() : 0;
        int currentDay = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentDay() : 1;
        int targetYear = immediate ? currentYear : targetYearSpinner.getValue();

        double lat = latSpinner.getValue();
        double lng = lngSpinner.getValue();
        double mag = magnitudeSpinner.getValue();

        String details = String.format(java.util.Locale.US, "Lat: %.2f°, Lng: %.2f°, Mag: %.1f", lat, lng, mag);

        timeline.addEntry(targetYear, type, name, details, true);
        refreshTimelineView();

        if (engine != null && engine.getEventSystem() != null) {
            org.ether.society.events.ActiveEvent ae = new org.ether.society.events.ActiveEvent(
                "GM_" + System.currentTimeMillis(),
                "⚡ GOD MODE: " + name,
                type, lat, lng, targetYear, currentMonth, currentDay, 30.0
            );
            engine.getEventSystem().recordSpatialEvent(ae);
        }

        // If target year is current year or immediate, execute physical forcing directly
        if (immediate || targetYear <= currentYear) {
            executePhysicalForcing(type, mag, lat, lng);
            logger.info("God Mode intervention EXECUTED immediately (Year {}): {} - {}", currentYear, name, details);
        } else {
            logger.info("God Mode intervention SCHEDULED for Year {}: {} - {}", targetYear, name, details);
        }
    }

    private void executePhysicalForcing(String type, double mag, double lat, double lng) {
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
        int currentMonth = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentMonth() : 0;
        int currentDay = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentDay() : 1;
        timeline.addEntry(currentYear, type, title, details, true);
        refreshTimelineView();

        if (engine != null && engine.getEventSystem() != null) {
            double lat = latSpinner != null ? latSpinner.getValue() : 0.0;
            double lng = lngSpinner != null ? lngSpinner.getValue() : 0.0;
            org.ether.society.events.ActiveEvent ae = new org.ether.society.events.ActiveEvent(
                "GM_" + System.currentTimeMillis(),
                "⚡ GOD MODE: " + title,
                "GOD_MODE", lat, lng, (int) currentYear, currentMonth, currentDay, 30.0
            );
            engine.getEventSystem().recordSpatialEvent(ae);
        }

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

