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
            org.ether.society.ui.WindowUtils.applyWindowIcon(primaryStage);
            logger.info("Starting Ether Application with Splash Screen...");

            org.ether.society.ui.SplashScreen splash = new org.ether.society.ui.SplashScreen();
            splash.show();

            javafx.concurrent.Task<Void> initTask = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    splash.updateProgress(0.15,
                            I18n.getOrDefault("splash.step.config.status", "Loading system configuration..."),
                            I18n.getOrDefault("splash.step.config.substatus", "Reading execution properties and settings"));
                    config = org.ether.society.config.ConfigurationLoader.loadDefault();
                    Thread.sleep(120);

                    splash.updateProgress(0.40,
                            I18n.getOrDefault("splash.step.h3.status", "Initializing H3 spatial engine..."),
                            I18n.getOrDefault("splash.step.h3.substatus", "Allocating geodesic mesh & DOD buffers"));
                    h3Engine = new H3SimulationEngine(config);
                    Thread.sleep(150);

                    splash.updateProgress(0.70,
                            I18n.getOrDefault("splash.step.ui.status", "Building JavaFX graphical components..."),
                            I18n.getOrDefault("splash.step.ui.substatus", "Initializing 2D/3D rendering & control panels"));
                    javafx.application.Platform.runLater(() -> {
                        mapCanvas = new H3MapCanvas(1280, 800);
                        miniMap = new MiniMap();
                        hud = new PerformanceHUD();
                        controlPanel = new ControlPanel(h3Engine);
                        mainView = new MainView(h3Engine, controlPanel, mapCanvas, miniMap, hud);
                        controlPanel.setMapCanvas(mapCanvas);
                        controlPanel.setMiniMap(miniMap);
                        controlPanel.updateDatabaseStatus(org.ether.society.database.DatabaseConfig.isDatabaseAvailable());
                    });
                    Thread.sleep(200);

                    splash.updateProgress(0.95,
                            I18n.getOrDefault("splash.step.theme.status", "Finalizing display and themes..."),
                            I18n.getOrDefault("splash.step.theme.substatus", "Applying visual styles"));
                    Thread.sleep(100);
                    splash.updateProgress(1.0,
                            I18n.getOrDefault("splash.step.ready.status", "Ready!"),
                            I18n.getOrDefault("splash.step.ready.substatus", "Opening dashboard"));
                    return null;
                }
            };

            initTask.setOnSucceeded(ev -> {
                try {
                    Scene scene = new Scene(mainView, 1280, 800);
                    try {
                        String css = getClass().getResource("/styles.css").toExternalForm();
                        scene.getStylesheets().add(css);
                    } catch (Exception e) {
                        logger.warn("Could not load styles.css stylesheet: {}", e.getMessage());
                    }

                    primaryStage.setScene(scene);
                    Theme.applyCurrentTheme(scene);
                    updateTexts();
                    org.ether.society.ui.WindowUtils.applyWindowIcon(primaryStage);

                    I18n.languageProperty().addListener((obs, old, val) -> updateTexts());

                    primaryStage.setMinWidth(1024);
                    primaryStage.setMinHeight(700);
                    primaryStage.setOnCloseRequest(e -> stop());

                    splash.close();
                    primaryStage.show();

                    startAnimationTimer();
                    logger.info("Application started successfully");
                } catch (Exception ex) {
                    logger.error("Error setting up main stage after splash completion", ex);
                }
            });

            initTask.setOnFailed(ev -> {
                splash.close();
                logger.error("FATAL ERROR during splash screen initialization task", initTask.getException());
            });

            new Thread(initTask, "EtherApp-SplashInitThread").start();

        } catch (Throwable t) {
            System.err.println("!!! FATAL EXCEPTION IN ETHERAPP START !!!");
            t.printStackTrace(System.err);
            logger.error("FATAL ERROR in Application start method", t);
            if (t instanceof RuntimeException re) throw re;
            if (t instanceof Exception e) throw e;
            throw new RuntimeException(t);
        }
    }

    private void startAnimationTimer() {
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
                if (h3Engine != null && h3Engine.getWorldBuffer() != null) {
                    hud.registerFrame(now);
                    hud.updateSimulationInfo((int) h3Engine.getPopulatedCellCount(), 1.0, 0, 0);
                    if (h3Engine.getProfiler() != null) {
                        hud.updateProfilerInfo(
                            h3Engine.getProfiler().getAverageTickTimeMs(),
                            h3Engine.getProfiler().getP95TickTimeMs()
                        );
                    }
                }

                // Update control panel stats
                if (controlPanel != null && h3Engine != null) {
                    controlPanel.updateStats(
                        h3Engine.getTotalPopulation(),
                        h3Engine.getTotalFood(),
                        h3Engine.getPopulatedCellCount(),
                        h3Engine.getCurrentTPS()
                    );

                    controlPanel.updateYear(
                        String.format("An %d, Mois %d, Jour %d",
                            h3Engine.getTimeManager().getCurrentYear(),
                            h3Engine.getTimeManager().getCurrentMonth() + 1,
                            h3Engine.getTimeManager().getCurrentDay())
                    );
                }

                // Periodic map redraw when needed
                if (now - lastMapRedraw >= 500_000_000L) { // every 0.5s
                    if (mapCanvas != null) {
                        DisplayMode mode = mapCanvas.getDisplayMode();
                        if (mode != DisplayMode.BIOME) {
                            mapCanvas.draw();
                        }
                    }
                    lastMapRedraw = now;
                }
            }
        };
        timer.start();
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
                h3Engine.saveSimulation("Autosave_Exit");
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
