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
    private final Button pauseResumeBtn;
    private final Label statusLabel;

    public GodModePanel(H3SimulationEngine engine, ScenarioTimeline timeline) {
        this.engine = engine;
        this.timeline = timeline != null ? timeline : new ScenarioTimeline();

        setPadding(new Insets(15));
        setSpacing(12);
        getStyleClass().add("glass-panel");
        setStyle("-fx-background-color: rgba(15, 23, 42, 0.92); -fx-border-color: #38bdf8; -fx-border-radius: 8; -fx-background-radius: 8;");

        // Title Header
        Label header = new Label("⚡ MODE DIEU & CHRONOLOGIE DU SCÉNARIO (GOD MODE & TIMELINE)");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #38bdf8;");

        // Pause / Resume Control Bar
        pauseResumeBtn = new Button("⏸ Interrompre la Simulation (Pause)");
        pauseResumeBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #ef4444; -fx-text-fill: white;");
        pauseResumeBtn.setOnAction(e -> togglePause());

        statusLabel = new Label("Statut : En cours d'exécution");
        statusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

        HBox controlBar = new HBox(12, pauseResumeBtn, statusLabel);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        // Section 1: Event Injector (Direct Physical Forcing)
        VBox injectorBox = createInjectorSection();

        // Section 2: Scenario Timeline Audit Log
        Label timelineHeader = new Label("📜 CHRONOLOGIE DU SCÉNARIO & MODIFICATIONS EN DIRECT :");
        timelineHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #a78bfa;");

        timelineListView = new ListView<>();
        timelineListView.setPrefHeight(180);
        timelineListView.setStyle("-fx-control-inner-background: #090d16; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        refreshTimelineView();

        getChildren().addAll(header, controlBar, new Separator(), injectorBox, new Separator(), timelineHeader, timelineListView);
    }

    private void togglePause() {
        if (engine == null) return;
        if (engine.isRunning()) {
            engine.pause();
            pauseResumeBtn.setText("▶ Reprendre la Simulation (Play)");
            pauseResumeBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #10b981; -fx-text-fill: white;");
            statusLabel.setText("Statut : EN PAUSE (Modifications autorisées)");
            statusLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
        } else {
            engine.start();
            pauseResumeBtn.setText("⏸ Interrompre la Simulation (Pause)");
            pauseResumeBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #ef4444; -fx-text-fill: white;");
            statusLabel.setText("Statut : En cours d'exécution");
            statusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        }
    }

    private VBox createInjectorSection() {
        VBox box = new VBox(8);
        Label title = new Label("🌍 INJECTION D'ÉVÉNEMENTS PHYSIQUES EN DIRECT :");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        // Volcano SO2 Injection
        Button volcanoBtn = new Button("🌋 Éruption Stratosphérique SO₂");
        volcanoBtn.setOnAction(e -> {
            NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(1.2);
            recordIntervention("VOLCANO", "Éruption Volcanique Majeure", "Injection d'aérosols stratosphériques (τ = 1.20)");
        });

        // Heatwave / Radiative Forcing
        Button heatwaveBtn = new Button("🔥 Canicule Globale (+3°C)");
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
        solarEmpBtn.setOnAction(e -> {
            recordIntervention("SOLAR_EMP", "Tempête Géomagnétique Solaire", "Perturbation EMP et effondrement temporaire du réseau électrique");
        });

        // Pandémie Pathogène
        Button pandemicBtn = new Button("🦠 Épidémie Zoonotique");
        pandemicBtn.setOnAction(e -> {
            recordIntervention("PANDEMIC", "Outbreak Épidémique Bio-Moléculaire", "Choc immunitaire et hausse de la mortalité de Gompertz");
        });

        grid.addRow(0, volcanoBtn, heatwaveBtn);
        grid.addRow(1, solarEmpBtn, pandemicBtn);

        box.getChildren().addAll(title, grid);
        return box;
    }

    public void recordIntervention(String type, String title, String details) {
        long currentYear = engine != null ? engine.getTimeManager().getCurrentYear() : 2026;
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
