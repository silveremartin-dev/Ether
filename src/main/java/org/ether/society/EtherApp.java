/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society;

import org.ether.society.config.Configuration;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.ControlPanel;
import org.ether.society.ui.DisplayMode;
import org.ether.society.ui.H3MapCanvas;
import org.ether.society.ui.MainView;
import org.ether.society.ui.MiniMap;
import org.ether.society.ui.PerformanceHUD;
import org.ether.society.ui.Theme;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application entry point for Ether simulation.
 */
public class EtherApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(EtherApp.class);

    private Stage primaryStage;
    private Configuration config;
    private H3SimulationEngine h3Engine;

    // UI Components
    private H3MapCanvas mapCanvas;
    private MiniMap miniMap;
    private PerformanceHUD hud;
    private ControlPanel controlPanel;
    private MainView mainView;

    private AnimationTimer timer;
    private long lastFpsUpdate = 0;
    private int frameCount = 0;
    private double currentFps = 0.0;
    private long lastMapRedraw = 0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            this.primaryStage = primaryStage;
            logger.info("Starting Ether Application...");

            // Load configuration
            config = org.ether.society.config.ConfigurationLoader.loadDefault();

            // Initialize simulation engine
            h3Engine = new H3SimulationEngine(config);

            // Initialize UI components
            mapCanvas = new H3MapCanvas(1280, 800);
            miniMap = new MiniMap();
            hud = new PerformanceHUD();
            controlPanel = new ControlPanel(h3Engine);

            // Initialize main layout
            mainView = new MainView(h3Engine, controlPanel, mapCanvas, miniMap, hud);

            // Wire canvas references to control panel
            controlPanel.setMapCanvas(mapCanvas);
            controlPanel.setMiniMap(miniMap);

            // Connect database status monitoring
            controlPanel.updateDatabaseStatus(org.ether.society.database.DatabaseConfig.isDatabaseAvailable());

            // Create scene
            Scene scene = new Scene(mainView, 1280, 800);

            // Load CSS stylesheet if available
            try {
                String css = getClass().getResource("/styles.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                logger.warn("Could not load styles.css stylesheet: {}", e.getMessage());
            }

            // Configure stage
            primaryStage.setScene(scene);
            Theme.applyCurrentTheme(scene);
            updateTexts();
            org.ether.society.ui.WindowUtils.applyWindowIcon(primaryStage);

            // Register i18n listener
            I18n.languageProperty().addListener((obs, old, val) -> updateTexts());

            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(700);

            // Stop engine on window close
            primaryStage.setOnCloseRequest(e -> stop());

            primaryStage.show();

            // Start animation timer for UI updates
            timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    frameCount++;
                    if (now - lastFpsUpdate >= 1_000_000_000L) {
                        currentFps = frameCount / ((now - lastFpsUpdate) / 1_000_000_000.0);
                        frameCount = 0;
                        lastFpsUpdate = now;
                    }

                    // Update performance HUD
                    if (h3Engine.getWorldBuffer() != null) {
                        hud.registerFrame(now);
                        hud.updateSimulationInfo((int) h3Engine.getPopulatedCellCount(), 1.0, 0, 0);
                    }

                    // Update control panel stats
                    controlPanel.updateStats(
                        h3Engine.getTotalPopulation(),
                        h3Engine.getTotalFood(),
                        h3Engine.getPopulatedCellCount(),
                        h3Engine.getCurrentTPS()
                    );

                    controlPanel.updateYear(
                        String.format("An %d, Mois %d",
                            h3Engine.getTimeManager().getCurrentYear(),
                            h3Engine.getTimeManager().getCurrentMonth() + 1)
                    );

                    // Periodic map redraw when needed
                    if (now - lastMapRedraw >= 500_000_000L) { // every 0.5s
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
        } catch (Throwable t) {
            System.err.println("!!! FATAL EXCEPTION IN ETHERAPP START !!!");
            t.printStackTrace(System.err);
            logger.error("FATAL ERROR in Application start method", t);
            if (t instanceof RuntimeException re) throw re;
            if (t instanceof Exception e) throw e;
            throw new RuntimeException(t);
        }
    }

    @Override
    public void stop() {
        logger.info("Stopping Ether Application...");
        if (timer != null) {
            timer.stop();
        }
        if (h3Engine != null) {
            try {
                logger.info("Auto-saving active simulation state on exit...");
                h3Engine.saveGame("Autosave_Exit");
            } catch (Exception ex) {
                logger.warn("Could not auto-save on exit: {}", ex.getMessage());
            }
            h3Engine.shutdown();
        }
        javafx.application.Platform.exit();
        System.exit(0);
    }

    private void updateTexts() {
        primaryStage.setTitle(I18n.get("app.title"));
    }
}
