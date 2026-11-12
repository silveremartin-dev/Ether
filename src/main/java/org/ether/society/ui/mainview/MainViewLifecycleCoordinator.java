/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.mainview;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.model.ScenarioTimeline;
import org.ether.society.ui.ColorLegend;
import org.ether.society.ui.ComparativeAnalyticsPanel;
import org.ether.society.ui.ControlPanel;
import org.ether.society.ui.ExecutionContextPanel;
import org.ether.society.ui.GodModePanel;
import org.ether.society.ui.H3MapCanvas;
import org.ether.society.ui.HeadlessDashboardOverlay;
import org.ether.society.ui.MiniMap;
import org.ether.society.ui.NotificationOverlay;
import org.ether.society.ui.PerformanceHUD;
import org.ether.society.ui.PlanetGeneratorPanel;
import org.ether.society.ui.ResourceDistributionPanel;
import org.ether.society.ui.ScenarioSetupPanel;
import org.ether.society.ui.StatsPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coordinates simulation lifecycle transitions, tick telemetry polling, snapshot save/load actions,
 * and cross-tab pipeline handoffs for {@link org.ether.society.ui.MainView}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class MainViewLifecycleCoordinator {

    /** Diagnostic logger for lifecycle transitions and tick events. */
    private static final Logger logger = LoggerFactory.getLogger(MainViewLifecycleCoordinator.class);

    /**
     * Private constructor preventing instantiation of utility coordinator class.
     */
    private MainViewLifecycleCoordinator() {
        // Utility coordinator class
    }

    /**
     * Handles completion of procedural/empirical planet generation in Tab 1 and transitions to Tab 2.
     *
     * @param cells generated H3 planetary cells
     * @param engine simulation engine
     * @param mapCanvas cartographic canvas
     * @param miniMap overview minimap
     * @param controlPanel control panel
     * @param planetGeneratorPanel planet generator panel
     * @param resourcePanel resource distribution panel
     * @param setupPanel scenario setup panel
     * @param tabPane root tab pane
     * @param resourcesTab target resources tab
     */
    public static void onPlanetGenerated(
            List<H3Cell> cells,
            H3SimulationEngine engine,
            H3MapCanvas mapCanvas,
            MiniMap miniMap,
            ControlPanel controlPanel,
            PlanetGeneratorPanel planetGeneratorPanel,
            ResourceDistributionPanel resourcePanel,
            ScenarioSetupPanel setupPanel,
            TabPane tabPane,
            Tab resourcesTab) {
        if (cells == null || cells.isEmpty()) return;
        logger.info("Planet generated with {} cells", cells.size());
        engine.setCells(cells);
        mapCanvas.setCells(cells);
        if (miniMap != null) miniMap.setCells(cells);
        controlPanel.updateSeason(engine.getTimeManager().getCurrentMonth());
        mapCanvas.draw();

        resourcePanel.setActiveCells(cells);
        if (planetGeneratorPanel != null) {
            resourcePanel.setActivePlanetPreset(planetGeneratorPanel.buildPresetFromUI());
        }
        setupPanel.setGeneratedCells(cells);
        tabPane.getSelectionModel().select(resourcesTab);
    }

    /**
     * Handles application of resource and ecological distributions in Tab 2 and transitions to Tab 3.
     *
     * @param cells updated H3 cells with ecological/mineral attributes
     * @param engine simulation engine
     * @param mapCanvas cartographic canvas
     * @param miniMap overview minimap
     * @param setupPanel scenario setup panel
     * @param tabPane root tab pane
     * @param setupTab target scenario setup tab
     */
    public static void onResourcesApplied(
            List<H3Cell> cells,
            H3SimulationEngine engine,
            H3MapCanvas mapCanvas,
            MiniMap miniMap,
            ScenarioSetupPanel setupPanel,
            TabPane tabPane,
            Tab setupTab) {
        if (cells == null || cells.isEmpty()) return;
        logger.info("Resource distribution applied to {} cells", cells.size());
        engine.setCells(cells);
        mapCanvas.setCells(cells);
        if (miniMap != null) miniMap.setCells(cells);
        mapCanvas.draw();

        setupPanel.setGeneratedCells(cells);
        tabPane.getSelectionModel().select(setupTab);
    }

    /**
     * Initializes or restores a simulation from {@link ScenarioSetupPanel} (either fresh or from snapshot)
     * and transitions to the interactive Simulation Tab (Tab 4).
     *
     * @param scenario configured scenario model
     * @param engine simulation engine
     * @param timeline scenario timeline audit log
     * @param setupPanel scenario setup panel
     * @param comparativeAnalyticsPanel comparative analytics panel (auto-paused if batch running)
     * @param godModePanel God Mode panel
     * @param statsPanel statistics dashboard panel
     * @param controlPanel control panel
     * @param mapCanvas cartographic canvas
     * @param miniMap overview minimap
     * @param notificationOverlay floating notification overlay
     * @param tabPane root tab pane
     * @param simulationTab target simulation tab
     */
    public static void onStartSimulation(
            Scenario scenario,
            H3SimulationEngine engine,
            ScenarioTimeline timeline,
            ScenarioSetupPanel setupPanel,
            ComparativeAnalyticsPanel comparativeAnalyticsPanel,
            GodModePanel godModePanel,
            StatsPanel statsPanel,
            ControlPanel controlPanel,
            H3MapCanvas mapCanvas,
            MiniMap miniMap,
            NotificationOverlay notificationOverlay,
            TabPane tabPane,
            Tab simulationTab) {
        if (setupPanel != null && setupPanel.isResumeFromSnapshotSelected()) {
            var meta = setupPanel.getSelectedSnapshotMetadata();
            if (meta != null) {
                logger.info("Resuming simulation from snapshot: {} (Year {}, Month {})", meta.getName(), meta.getYear(), meta.getMonth());
                engine.loadSimulation(meta.getId());
                if (scenario != null) {
                    engine.setCurrentScenario(scenario);
                }
                if (meta.getYear() != 0) {
                    engine.getTimeManager().reset((int) meta.getYear());
                }

                timeline.clear();
                timeline.addEntry((int) meta.getYear(), "REPRISE_SNAPSHOT",
                        I18n.getOrDefault("mainview.timeline.resume_title", "Reprise depuis Snapshot : ") + meta.getName(),
                        String.format(I18n.getOrDefault("mainview.timeline.resume_details", "Restored at Year %,d (Month %d) - Scenario %s"),
                                meta.getYear(), meta.getMonth(), meta.getScenarioName()), false);

                if (godModePanel != null) {
                    godModePanel.refreshTimelineView();
                }

                List<H3Cell> restoredCells = engine.getCells();
                if (restoredCells != null && !restoredCells.isEmpty()) {
                    mapCanvas.setWorldBuffer(engine.getWorldBuffer());
                    mapCanvas.setCells(restoredCells);
                    if (miniMap != null) miniMap.setCells(restoredCells);
                }

                controlPanel.updateScenarioName(meta.getScenarioName() + " (" + I18n.getOrDefault("mainview.restored_snapshot", "Restored Snapshot") + ")");
                controlPanel.updateYear(String.format("An %d", meta.getYear()));

                if (statsPanel != null) {
                    statsPanel.resetChartSeries();
                }

                simulationTab.setDisable(false);
                tabPane.getSelectionModel().select(simulationTab);
                logger.info("Transitioned directly to Simulation Tab (4) following snapshot restore in ready/paused mode");
                return;
            }
        }

        List<H3Cell> newCells = setupPanel.getCells();
        if (newCells == null || newCells.isEmpty()) {
            logger.warn("Cannot start simulation: no cells generated");
            return;
        }

        if (comparativeAnalyticsPanel != null && comparativeAnalyticsPanel.isBatchRunning()) {
            logger.info("Auto-pausing background comparative batch execution as a new simulation is starting");
            comparativeAnalyticsPanel.pauseOrCancelBatchForInteractiveSimulation();
            if (notificationOverlay != null) {
                notificationOverlay.showWarning(I18n.getOrDefault("mainview.batch_paused_notice",
                        "⏸️ Calculs comparatifs en tâche de fond mis en pause pour allouer les ressources à la nouvelle simulation"));
            }
        }

        logger.info("Starting simulation with scenario: {}", scenario.getName());
        engine.initializeFromScenario(scenario, newCells);

        timeline.clear();
        timeline.addEntry(scenario.getStartDateYear(), "SETUP",
                I18n.getOrDefault("mainview.timeline.init_title", "Initial Scenario: ") + scenario.getName(),
                String.format("Pop: %,d | Tech: %.1f | Motif: %s",
                        scenario.getInitialHumanCount(), scenario.getInitialTechLevel(), scenario.getPopulationDensityType()), false);

        for (var evt : setupPanel.getScheduledEvents()) {
            timeline.addEntry(evt.getYear(), evt.getType().toUpperCase(), evt.getName(),
                    String.format("Lat: %.2f°, Lng: %.2f°, Mag: %.1f", evt.getLatitude(), evt.getLongitude(), evt.getMagnitude()), false);
        }

        if (godModePanel != null) godModePanel.refreshTimelineView();
        if (statsPanel != null) statsPanel.reset();

        mapCanvas.setWorldBuffer(engine.getWorldBuffer());
        mapCanvas.setCells(newCells);
        if (miniMap != null) miniMap.setCells(newCells);

        controlPanel.updateScenarioName(scenario.getName());
        controlPanel.updateYear(String.format("An %d", scenario.getStartDateYear()));
        mapCanvas.setScenarioName(scenario.getName());
        mapCanvas.setCurrentDateStr(String.format("An %d", scenario.getStartDateYear()));

        simulationTab.setDisable(false);
        tabPane.getSelectionModel().select(simulationTab);
        logger.info("Transitioned directly to Simulation Tab (4) with {} cells in ready/paused mode", newCells.size());
    }

    /**
     * Launches or switches to the Simulation Tab directly from {@link ExecutionContextPanel}.
     *
     * @param engine simulation engine
     * @param setupPanel scenario setup panel
     * @param statsPanel statistics dashboard panel
     * @param controlPanel control panel
     * @param mapCanvas cartographic canvas
     * @param miniMap overview minimap
     * @param tabPane root tab pane
     * @param simulationTab target simulation tab
     */
    public static void launchSimulationFromContext(
            H3SimulationEngine engine,
            ScenarioSetupPanel setupPanel,
            StatsPanel statsPanel,
            ControlPanel controlPanel,
            H3MapCanvas mapCanvas,
            MiniMap miniMap,
            TabPane tabPane,
            Tab simulationTab) {
        if (setupPanel != null) {
            Scenario currentScenario = setupPanel.getScenario();
            List<H3Cell> cells = setupPanel.getCells();
            if (currentScenario != null && cells != null && !cells.isEmpty()) {
                if (engine.getCells() == null || engine.getCells().isEmpty()) {
                    engine.initializeFromScenario(currentScenario, cells);
                    mapCanvas.setWorldBuffer(engine.getWorldBuffer());
                    mapCanvas.setCells(cells);
                    if (miniMap != null) miniMap.setCells(cells);
                    controlPanel.updateScenarioName(currentScenario.getName());
                    controlPanel.updateYear(String.format("An %d", currentScenario.getStartDateYear()));
                    if (mapCanvas != null) {
                        mapCanvas.setCurrentDateStr(String.format("An %d", currentScenario.getStartDateYear()));
                    }
                }
            }
        }

        simulationTab.setDisable(false);
        tabPane.getSelectionModel().select(simulationTab);
        if (statsPanel != null) statsPanel.reset();
        if (mapCanvas != null) mapCanvas.resetView();
        if (engine != null && engine.isRunning()) engine.pause();
        if (controlPanel != null) controlPanel.updatePlayPauseVisuals(false);
        logger.info("Simulation initialized from Execution Context Panel and switched to Simulation Tab (4) in ready/paused mode");
    }

    /**
     * Prompts the user for a snapshot name and persists the current simulation state.
     *
     * @param engine simulation engine
     * @param notificationOverlay notification toast overlay
     */
    public static void saveSimulation(H3SimulationEngine engine, NotificationOverlay notificationOverlay) {
        TextInputDialog dialog = new TextInputDialog(I18n.getOrDefault("mainview.save.default_name", "Sauvegarde Scenario"));
        dialog.setTitle(I18n.getOrDefault("mainview.save.dialog_title", "Save Simulation"));
        dialog.setHeaderText(I18n.getOrDefault("mainview.save.dialog_header", "Entrez le nom de la sauvegarde :"));
        dialog.setContentText(I18n.getOrDefault("mainview.save.dialog_label", "Nom :"));

        dialog.showAndWait().ifPresent(name -> {
            engine.saveSimulation(name);
            if (notificationOverlay != null) {
                notificationOverlay.showEvent(I18n.getOrDefault("mainview.save.success", "Simulation Saved: ") + name);
            }
        });
    }

    /**
     * Restores the latest simulation snapshot from the database and refreshes visual panels.
     *
     * @param engine simulation engine
     * @param mapCanvas cartographic canvas
     * @param miniMap overview minimap
     * @param statsPanel statistics dashboard panel
     * @param controlPanel control panel
     * @param notificationOverlay notification toast overlay
     */
    public static void loadSimulation(
            H3SimulationEngine engine,
            H3MapCanvas mapCanvas,
            MiniMap miniMap,
            StatsPanel statsPanel,
            ControlPanel controlPanel,
            NotificationOverlay notificationOverlay) {
        engine.loadSimulation(null);
        if (notificationOverlay != null) {
            notificationOverlay.showEvent(I18n.getOrDefault("mainview.load.success", "Simulation loaded from database"));
        }

        mapCanvas.setWorldBuffer(engine.getWorldBuffer());
        mapCanvas.setCells(engine.getCells());
        mapCanvas.draw();
        if (miniMap != null) miniMap.setCells(engine.getCells());
        if (statsPanel != null) statsPanel.resetChartSeries();
        if (controlPanel != null) controlPanel.updateYear(engine.getTimeManager().getFormattedDate());
    }

    /**
     * Starts engine tick callbacks and JavaFX {@link AnimationTimer} event polling.
     *
     * @param engine simulation engine
     * @param mapCanvas cartographic canvas
     * @param controlPanel control panel
     * @param statsPanel statistics dashboard panel
     * @param executionContextPanel execution context panel
     * @param headlessDashboard headless telemetry overlay
     * @param notificationOverlay notification toast overlay
     * @param colorLegend GIS color legend
     * @param hud performance HUD
     */
    public static void startEventPolling(
            H3SimulationEngine engine,
            H3MapCanvas mapCanvas,
            ControlPanel controlPanel,
            StatsPanel statsPanel,
            ExecutionContextPanel executionContextPanel,
            HeadlessDashboardOverlay headlessDashboard,
            NotificationOverlay notificationOverlay,
            ColorLegend colorLegend,
            PerformanceHUD hud) {
        if (mapCanvas != null && engine != null) {
            mapCanvas.setEventSystem(engine.getEventSystem());
        }

        if (engine != null) {
            final AtomicLong lastUiUpdateNanos = new AtomicLong(0);
            final AtomicBoolean renderPending = new AtomicBoolean(false);

            engine.setOnTickCallback(() -> {
                long now = System.nanoTime();
                boolean isRecording = mapCanvas != null && mapCanvas.isRecordingVideo();
                long currentTick = engine.getTickCounter();

                double resDays = engine.getCurrentScenario() != null ? engine.getCurrentScenario().getTemporalResolutionDays() : 1.0;
                String dateStr = engine.getTimeManager().formatContextualDate(resDays);

                boolean isHeadless = executionContextPanel != null
                        && executionContextPanel.getRenderingMode() == ExecutionContextPanel.RenderingMode.HEADLESS;

                if (isHeadless) {
                    int targetYear = executionContextPanel.getTargetYear();
                    long curYear = engine.getTimeManager().getCurrentYear();
                    if (targetYear != 0 && curYear >= targetYear && engine.isRunning()) {
                        engine.pause();
                        Platform.runLater(() -> {
                            if (controlPanel != null) controlPanel.updatePlayPauseVisuals(false);
                            if (notificationOverlay != null) {
                                notificationOverlay.showInfo(I18n.getOrDefault("headless.target_reached",
                                        "🏁 Horizon cible atteint (An {0}) — Simulation en pause", targetYear));
                            }
                        });
                    }
                }

                if (isRecording && mapCanvas != null) {
                    mapCanvas.setCurrentDateStr(dateStr);
                }

                long updateInterval = isHeadless ? 100_000_000L : 33_000_000L;
                if (now - lastUiUpdateNanos.get() >= updateInterval && renderPending.compareAndSet(false, true)) {
                    Platform.runLater(() -> {
                        try {
                            if (isHeadless && headlessDashboard != null) {
                                headlessDashboard.updateHeadlessTelemetry(dateStr, currentTick, hud != null ? hud.getFps() : 0.0);
                            } else {
                                if (mapCanvas != null) {
                                    mapCanvas.setCurrentDateStr(dateStr);
                                    mapCanvas.invalidateSmoothCache();
                                    mapCanvas.draw();
                                }
                                if (colorLegend != null && mapCanvas != null) {
                                    colorLegend.updateFromCanvas(mapCanvas);
                                }
                            }
                            if (controlPanel != null) {
                                controlPanel.updateYear(dateStr);
                            }
                        } finally {
                            lastUiUpdateNanos.set(System.nanoTime());
                            renderPending.set(false);
                        }
                    });
                }
            });
        }

        AnimationTimer eventLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 250_000_000 && engine != null) {
                    List<String> events = engine.getEventSystem().flushEvents();
                    if (!events.isEmpty()) {
                        long curPas = engine.getTickCounter();
                        for (String event : events) {
                            logger.info("📢 [Pas {}] {}", curPas, event);
                        }
                    }

                    if (controlPanel != null) {
                        controlPanel.updateRecentEvents(engine.getEventSystem().getRecentEventsHistory());
                    }

                    boolean isHeadless = executionContextPanel != null
                            && executionContextPanel.getRenderingMode() == ExecutionContextPanel.RenderingMode.HEADLESS;
                    if (!isHeadless && mapCanvas != null && mapCanvas.getEventSystem() != null
                            && !mapCanvas.getEventSystem().getActiveEvents().isEmpty()) {
                        mapCanvas.draw();
                    }

                    if (controlPanel != null) {
                        controlPanel.updateAge(resolveAgeName(engine.getAverageTechnology()));
                        controlPanel.updateStats(
                                engine.getTotalPopulation(),
                                engine.getTotalFood(),
                                engine.getPopulatedCellCount(),
                                engine.getCurrentTPS()
                        );
                        if (statsPanel != null && statsPanel.isVisible()) {
                            statsPanel.update();
                        }
                        controlPanel.updateSeason(engine.getTimeManager().getCurrentMonth());
                    }
                    lastUpdate = now;
                }
            }
        };
        eventLoop.start();
    }

    /**
     * Resolves localized historical age designation from average civilizational technology index.
     *
     * @param techLevel mean planetary technological level
     * @return localized age title
     */
    public static String resolveAgeName(float techLevel) {
        if (techLevel < 1.5f) return I18n.getOrDefault("age.stone", "STONE AGE");
        if (techLevel < 2.0f) return I18n.getOrDefault("age.bronze", "BRONZE AGE");
        if (techLevel < 2.5f) return I18n.getOrDefault("age.iron", "IRON AGE");
        if (techLevel < 3.2f) return I18n.getOrDefault("age.classical", "CLASSICAL AGE");
        if (techLevel < 4.2f) return I18n.getOrDefault("age.medieval", "MEDIEVAL AGE");
        if (techLevel < 5.0f) return I18n.getOrDefault("age.renaissance", "RENAISSANCE");
        if (techLevel < 6.5f) return I18n.getOrDefault("age.industrial", "INDUSTRIAL AGE");
        if (techLevel < 8.0f) return I18n.getOrDefault("age.atomic", "ATOMIC & INFORMATION AGE");
        return I18n.getOrDefault("age.future", "SPACE & FUSION AGE");
    }
}
