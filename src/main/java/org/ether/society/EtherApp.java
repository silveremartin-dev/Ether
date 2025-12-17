/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society;

import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.ControlPanel;
import org.ether.society.ui.DisplayMode;
import org.ether.society.ui.ColorLegend;
import org.ether.society.ui.H3MapCanvas;
import org.ether.society.ui.MiniMap;
import org.ether.society.ui.PerformanceHUD;

import java.io.IOException;
import java.util.List;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX Application entry point.
 * Initializes and displays the simulation UI.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 1.0.0
 */
public class EtherApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(EtherApp.class);

    private Stage primaryStage;
    private Label infoLabel;
    private H3SimulationEngine h3Engine;
    private MiniMap miniMap;
    private H3MapCanvas mapCanvas;
    private ControlPanel controlPanel;
    private PerformanceHUD hud;
    private ColorLegend colorLegend;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        logger.info("Starting Human Society Simulation...");

        // Load configuration
        Configuration config;
        try {
            config = ConfigurationLoader.loadDefault();
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
            return;
        }

        // Use H3 Engine
        h3Engine = new H3SimulationEngine(config);

        // Components
        hud = new PerformanceHUD();
        colorLegend = new ColorLegend();
        miniMap = new MiniMap();

        // Create H3 map canvas
        mapCanvas = new H3MapCanvas(1200, 700);
        mapCanvas.setCells(h3Engine.getCells());

        // Create scroll pane for canvas
        ScrollPane scrollPane = new ScrollPane(mapCanvas);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);

        // Control panel
        controlPanel = new ControlPanel(h3Engine);
        controlPanel.setMapCanvas(mapCanvas);
        controlPanel.setOnSave(this::saveSimulation);
        controlPanel.setOnLoad(this::loadSimulation);

        // Initialize Main View (Tabbed Interface)
        org.ether.society.ui.MainView mainView = new org.ether.society.ui.MainView(h3Engine, controlPanel, mapCanvas,
                miniMap, hud);
        mainView.addLegend(colorLegend);

        // Wire mini-map and legend
        miniMap.setCells(h3Engine.getCells());
        miniMap.setMainCanvas(mapCanvas);
        mapCanvas.setMiniMap(miniMap);
        controlPanel.setMiniMap(miniMap);
        controlPanel.setColorLegend(colorLegend);

        // Menu Bar
        javafx.scene.control.MenuBar menuBar = new javafx.scene.control.MenuBar();
        javafx.scene.control.Menu toolsMenu = new javafx.scene.control.Menu("Tools");

        javafx.scene.control.MenuItem planetGenItem = new javafx.scene.control.MenuItem("Planet Generator...");
        planetGenItem.setOnAction(e -> openPlanetGenerator());

        toolsMenu.getItems().add(planetGenItem);
        menuBar.getMenus().add(toolsMenu);

        StackPane contentStack = new StackPane(mainView);

        // Info label overlay
        infoLabel = new Label();
        infoLabel.setPadding(new javafx.geometry.Insets(5));
        infoLabel.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 3;");
        StackPane.setAlignment(infoLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(infoLabel, new javafx.geometry.Insets(5, 50, 0, 0));
        contentStack.getChildren().add(infoLabel);

        BorderPane mainRoot = new BorderPane();
        mainRoot.setTop(menuBar);
        mainRoot.setCenter(contentStack);

        Scene scene = new Scene(mainRoot, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();

        // Initial text update
        updateTexts();

        // Listen for language changes
        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());

        // Render loop
        AnimationTimer timer = new AnimationTimer() {
            private String lastYear = "";
            private long lastMapRedraw = 0;
            private static final long REDRAW_INTERVAL_NS = 500_000_000L; // 500ms

            @Override
            public void handle(long now) {
                // Update Simulation Time
                String currentYear = h3Engine.getTimeManager().getFormattedDate();
                if (!currentYear.equals(lastYear)) {
                    lastYear = currentYear;
                    controlPanel.updateYear(currentYear);
                }

                // Update HUD
                hud.registerFrame(now);
                hud.updateSimulationInfo(
                        h3Engine.getCells().size(),
                        mapCanvas.getZoomFactor(),
                        mapCanvas.getCenterLat(),
                        mapCanvas.getCenterLng());

                // Update population stats periodically
                controlPanel.updateStats(
                        h3Engine.getTotalPopulation(),
                        h3Engine.getTotalFood(),
                        h3Engine.getPopulatedCellCount());

                // Update season display
                controlPanel.updateSeason(h3Engine.getTimeManager().getCurrentMonth());

                // Poll events
                controlPanel.logEvents(h3Engine.getEventSystem().flushEvents());

                // Redraw map periodically when in data visualization mode
                if (now - lastMapRedraw > REDRAW_INTERVAL_NS) {
                    DisplayMode mode = mapCanvas.getDisplayMode();
                    if (mode != DisplayMode.BIOME) {
                        mapCanvas.draw();
                    }
                    lastMapRedraw = now;
                }
            }
        };
        timer.start();

        logger.info("Application started successfully");
    }

    private void updateTexts() {
        primaryStage.setTitle(I18n.get("app.title"));
        String year = h3Engine.getTimeManager().getFormattedDate();
        infoLabel.setText(I18n.get("app.info", year));
    }

    private void saveSimulation() {
        logger.info("Saving simulation...");
        logger.warn("Save disabled for offline verification due to DB connection issues");
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.WARNING);
        alert.setContentText("Database unavailable. Feature disabled.");
        alert.showAndWait();
    }

    private void loadSimulation() {
        logger.info("Loading simulation...");
        logger.warn("Load disabled for offline verification due to DB connection issues");
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.WARNING);
        alert.setContentText("Database unavailable. Feature disabled.");
        alert.showAndWait();
    }

    private void openPlanetGenerator() {
        org.ether.society.ui.PlanetGeneratorDialog dialog = new org.ether.society.ui.PlanetGeneratorDialog();
        java.util.Optional<List<org.ether.society.database.H3Cell>> result = dialog.showAndWaitForCells();

        result.ifPresent(cells -> {
            logger.info("Generated {} cells procedurally", cells.size());

            // Update Engine
            h3Engine.setCells(cells);

            // Update UI
            mapCanvas.setCells(cells);
            miniMap.setCells(cells);
            controlPanel.updateSeason(h3Engine.getTimeManager().getCurrentMonth());

            if (hud != null) {
                hud.updateSimulationInfo(cells.size(), mapCanvas.getZoomFactor(), mapCanvas.getCenterLat(),
                        mapCanvas.getCenterLng());
            }

            // Force redraw
            mapCanvas.draw();

            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setContentText("Generated world with " + cells.size() + " cells");
            alert.showAndWait();
        });
    }
}
