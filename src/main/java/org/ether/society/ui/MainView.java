/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;

import org.ether.society.model.Scenario;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javafx.animation.AnimationTimer;

/**
 * Main View Controller using a Tab-based layout.
 * Tabs:
 * 1. Setup (Scenario &amp; World Config)
 * 2. Simulation (Map &amp; Controls)
 */
public class MainView extends StackPane {
    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

    // Core Components
    private final H3SimulationEngine engine;
    private final ControlPanel controlPanel;
    private final H3MapCanvas mapCanvas;
    private final MiniMap miniMap;
    private final PerformanceHUD hud;
    private final StatsPanel statsPanel;
    private final org.ether.society.model.ScenarioTimeline timeline;
    private GodModePanel godModePanel;

    // UI Structure
    private TabPane tabPane;
    private Tab planetTab;
    private Tab resourcesTab;
    private Tab setupTab;
    private Tab executionContextTab;
    private Tab simulationTab;
    private Tab comparativeAnalyticsTab;
    private Tab preferencesTab;
    private PlanetGeneratorPanel planetGeneratorPanel;
    private ResourceDistributionPanel resourcePanel;
    private ScenarioSetupPanel setupPanel;
    private ExecutionContextPanel executionContextPanel;
    private ComparativeAnalyticsPanel comparativeAnalyticsPanel;
    private PreferencesPanel preferencesPanel;
    private NotificationOverlay notificationOverlay;

    public MainView(H3SimulationEngine engine, ControlPanel controlPanel, H3MapCanvas mapCanvas, MiniMap miniMap,
            PerformanceHUD hud) {
        this.engine = engine;
        this.controlPanel = controlPanel;
        this.mapCanvas = mapCanvas;
        this.miniMap = miniMap;
        this.hud = hud;
        this.statsPanel = new StatsPanel(engine);
        this.timeline = new org.ether.society.model.ScenarioTimeline();
        this.godModePanel = new GodModePanel(engine, timeline);

        initUI();
        updateTabTitles();

        I18n.languageProperty().addListener((obs, old, val) -> updateTabTitles());
    }

    private void initUI() {
        tabPane = new TabPane();
        // Transparent tab pane for glass effect
        tabPane.setStyle("-fx-tab-min-height: 40px; -fx-tab-max-height: 40px; -fx-background-color: transparent;");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // 1. Planet Generator Tab
        planetGeneratorPanel = new PlanetGeneratorPanel(this::onPlanetGenerated);
        planetTab = new Tab();
        planetTab.setContent(planetGeneratorPanel);
        planetTab.setClosable(false);

        // 2. Resources & Ecology Tab
        resourcePanel = new ResourceDistributionPanel(this::onResourcesApplied);
        if (planetGeneratorPanel != null) {
            resourcePanel.setPlanetPresetSupplier(planetGeneratorPanel::buildPresetFromUI);
            resourcePanel.setPlanetPresetApplyCallback(planetGeneratorPanel::applyPreset);
        }
        resourcesTab = new Tab();
        resourcesTab.setContent(resourcePanel);
        resourcesTab.setClosable(false);

        // 3. Setup Tab
        setupPanel = new ScenarioSetupPanel(this::onStartSimulation);
        setupPanel.setOnScenarioLoadedCallback((planet, eco) -> {
            if (planet != null && planetGeneratorPanel != null) {
                planetGeneratorPanel.applyPreset(planet);
            }
            if (eco != null && resourcePanel != null) {
                resourcePanel.applyEcologyPreset(eco);
            }
        });
        setupTab = new Tab();
        setupTab.setContent(setupPanel);
        setupTab.setClosable(false);

        // 4. Execution Context Tab (between Setup and Simulation)
        executionContextPanel = new ExecutionContextPanel(this::launchSimulationFromContext);
        executionContextTab = new Tab();
        executionContextTab.setContent(executionContextPanel);
        executionContextTab.setDisable(true); // Disabled until scenario setup is completed
        executionContextTab.setClosable(false);

        // 5. Simulation Tab
        simulationTab = new Tab();
        simulationTab.setContent(createSimulationView());
        simulationTab.setDisable(true); // Disabled until started

        // 6. Comparative Analytics Tab (Offline Benchmark & Sensitivity Analytics)
        comparativeAnalyticsPanel = new ComparativeAnalyticsPanel();
        comparativeAnalyticsTab = new Tab();
        comparativeAnalyticsTab.setContent(comparativeAnalyticsPanel);
        comparativeAnalyticsTab.setClosable(false);

        // 7. Preferences Tab
        preferencesPanel = new PreferencesPanel();
        preferencesTab = new Tab();
        preferencesTab.setContent(preferencesPanel);
        preferencesTab.setClosable(false);

        tabPane.getTabs().addAll(planetTab, resourcesTab, setupTab, executionContextTab, simulationTab, comparativeAnalyticsTab, preferencesTab);

        // Tab selection change listener
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            boolean isSim = (newTab == simulationTab);
            if (mapCanvas != null) {
                mapCanvas.setTabVisible(isSim);
            }
            if (oldTab == simulationTab && !isSim) {
                logger.info("Auto-pausing simulation and halting 2D/3D map rendering due to tab switch");
                engine.pause();
            }
            if (isSim) {
                if (mapCanvas != null) {
                    mapCanvas.resetView();
                }
            }
            if (newTab == comparativeAnalyticsTab && comparativeAnalyticsPanel != null) {
                comparativeAnalyticsPanel.refreshRunList();
            }
            if (newTab == resourcesTab && planetGeneratorPanel != null) {
                resourcePanel.setActivePlanetPreset(planetGeneratorPanel.buildPresetFromUI());
            }
            if (newTab == setupTab && setupPanel != null) {
                if (resourcePanel != null) {
                    org.ether.society.procedural.PlanetPreset activePlanet = resourcePanel.getActivePlanetPreset();
                    org.ether.society.model.EcologyPreset activeEco = resourcePanel.getSelectedEcologyPreset();
                    setupPanel.setInheritedContext(activePlanet, activeEco != null ? activeEco.name() : null);
                }
                setupPanel.ensurePreviewGeneratedIfNeeded();
            }
        });

        getChildren().add(tabPane);
    }

    public void updateTabTitles() {
        planetTab.setText(org.ether.society.i18n.I18n.get("tab.planet_generator"));
        resourcesTab.setText(org.ether.society.i18n.I18n.get("tab.resources"));
        setupTab.setText(org.ether.society.i18n.I18n.get("tab.scenario"));
        executionContextTab.setText(org.ether.society.i18n.I18n.getOrDefault("tab.execution_context", "⚡ Contexte d'Exécution"));
        simulationTab.setText(org.ether.society.i18n.I18n.get("tab.simulation"));
        comparativeAnalyticsTab.setText(org.ether.society.i18n.I18n.getOrDefault("tab.comparative_analytics", "📊 Analyse Comparative"));
        preferencesTab.setText(org.ether.society.i18n.I18n.get("tab.preferences"));
    }

    private void onPlanetGenerated(List<H3Cell> cells) {
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

    private void onResourcesApplied(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;
        logger.info("Resource distribution applied to {} cells", cells.size());
        engine.setCells(cells);
        mapCanvas.setCells(cells);
        if (miniMap != null) miniMap.setCells(cells);
        mapCanvas.draw();

        setupPanel.setGeneratedCells(cells);
        tabPane.getSelectionModel().select(setupTab);
    }

    private BorderPane createSimulationView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("glass-panel"); // Apply glass effect base

        // Map Container (Layered)
        StackPane mapStack = new StackPane();

        // 1. Direct Map Canvas (Bound to container dimensions to eliminate scrollbars and fill 100% of space)
        mapCanvas.widthProperty().bind(mapStack.widthProperty());
        mapCanvas.heightProperty().bind(mapStack.heightProperty());
        mapStack.getChildren().add(mapCanvas);

        // 2. Notification Overlay
        notificationOverlay = new NotificationOverlay();
        notificationOverlay.setPickOnBounds(false);
        StackPane.setAlignment(notificationOverlay, Pos.BOTTOM_CENTER);
        mapStack.getChildren().add(notificationOverlay);

        if (controlPanel != null) {
            controlPanel.setNotificationOverlay(notificationOverlay);
        }

        // 4. Deferred Calculation Progress Overlay for Tab 4
        ProgressBar simProgressBar = new ProgressBar(0);
        simProgressBar.setMaxWidth(400);
        simProgressBar.setPrefHeight(18);

        Label simStatusLabel = new Label("⚡ Initialisation de la simulation...");
        simStatusLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 14px;");

        VBox simProgressOverlay = new VBox(12, simStatusLabel, simProgressBar);
        simProgressOverlay.setAlignment(Pos.CENTER);
        simProgressOverlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.88); -fx-padding: 24; -fx-background-radius: 12; -fx-border-color: #38bdf8; -fx-border-radius: 12; -fx-border-width: 1.5;");
        simProgressOverlay.setMaxSize(500, 130);
        simProgressOverlay.setVisible(false);
        simProgressOverlay.setManaged(false);

        StackPane.setAlignment(simProgressOverlay, Pos.CENTER);
        mapStack.getChildren().add(simProgressOverlay);

        if (setupPanel != null) {
            setupPanel.setProgressControls(simProgressBar, simStatusLabel, simProgressOverlay);
        }

        startEventPolling();

        mapCanvas.setTooltipContainer(mapStack);

        // Connect Control Panel callbacks
        controlPanel.setOnSave(this::saveGame);
        controlPanel.setOnLoad(this::loadGame);
        controlPanel.setOnContourToggle(show -> mapCanvas.toggleContours(show));
        controlPanel.setOnTimelapseRecord(this::toggleTimelapseRecording);
        controlPanel.setOnTimelapseSeek(this::seekTimelapse);

        TabPane leftSidebar = new TabPane();
        leftSidebar.setPrefWidth(480);
        leftSidebar.setMinWidth(480);
        leftSidebar.setStyle("-fx-background-color: transparent;");
        leftSidebar.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        javafx.scene.control.ScrollPane controlScroll = new javafx.scene.control.ScrollPane(controlPanel);
        controlScroll.setFitToWidth(true);
        controlScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        javafx.scene.control.ScrollPane statsScroll = new javafx.scene.control.ScrollPane(statsPanel);
        statsScroll.setFitToWidth(true);
        statsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        javafx.scene.control.ScrollPane godScroll = new javafx.scene.control.ScrollPane(godModePanel);
        godScroll.setFitToWidth(true);
        godScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Tab controlTab = new Tab(I18n.getOrDefault("sim.tab.controls", "🎛️ Rendu 3D & Contrôles"), controlScroll);
        Tab statsTab = new Tab(I18n.getOrDefault("sim.tab.stats", "📊 Stats"), statsScroll);
        Tab godModeTab = new Tab(I18n.getOrDefault("sim.tab.godmode", "⚡ Mode Dieu"), godScroll);
        leftSidebar.getTabs().addAll(controlTab, statsTab, godModeTab);

        root.setLeft(leftSidebar);
        root.setCenter(mapStack);

        return root;
    }

    // --- Timelapse Recording State ---
    private boolean isRecording = false;

    private void toggleTimelapseRecording() {
        isRecording = !isRecording;
        if (isRecording) {
            logger.info("Timelapse recording STARTED");
            // Initial snapshot
            engine.getHistoryManager().captureWorldSnapshot(engine);
        } else {
            logger.info("Timelapse recording STOPPED");
            // Update slider range
            var snapshots = engine.getHistoryManager().getWorldSnapshots();
            if (!snapshots.isEmpty()) {
                int minYear = (int) (long) snapshots.firstKey();
                int maxYear = (int) (long) snapshots.lastKey();
                controlPanel.updateTimelapseSlider(minYear, maxYear, maxYear);
            }
        }
    }

    private void seekTimelapse(int year) {
        var snapshot = engine.getHistoryManager().getWorldSnapshot(year);
        if (snapshot != null) {
            mapCanvas.setCells(snapshot);
            if (miniMap != null) miniMap.setCells(snapshot);
            controlPanel.updateYear(String.valueOf(year));
            logger.info("Timelapse seek to year: {}", year);
        }
    }

    private void onStartSimulation(Scenario scenario) {
        if (setupPanel != null && setupPanel.isResumeFromSnapshotSelected()) {
            var meta = setupPanel.getSelectedSnapshotMetadata();
            if (meta != null) {
                logger.info("Resuming simulation from snapshot: {} (Year {}, Month {})", meta.getName(), meta.getYear(), meta.getMonth());
                engine.loadGame(meta.getId());
                if (meta.getYear() != 0) {
                    engine.getTimeManager().reset((int) meta.getYear());
                }

                timeline.clear();
                timeline.addEntry((int) meta.getYear(), "REPRISE_SNAPSHOT", "Reprise depuis Snapshot : " + meta.getName(),
                    String.format("Restauré à l'An %,d (Mois %d) - Scénario %s", meta.getYear(), meta.getMonth(), meta.getScenarioName()), false);

                if (godModePanel != null) {
                    godModePanel.refreshTimelineView();
                }

                List<H3Cell> restoredCells = engine.getCells();
                if (restoredCells != null && !restoredCells.isEmpty()) {
                    mapCanvas.setWorldBuffer(engine.getWorldBuffer());
                    mapCanvas.setCells(restoredCells);
                    if (miniMap != null) miniMap.setCells(restoredCells);
                }

                controlPanel.updateScenarioName(meta.getScenarioName() + " (Snapshot Restauré)");
                controlPanel.updateYear(String.valueOf(meta.getYear()));

                simulationTab.setDisable(false);
                tabPane.getSelectionModel().select(simulationTab);
                logger.info("Simulation tab activated via snapshot restore");
                return;
            }
        }

        // Retrieve generated cells from setup
        List<H3Cell> newCells = setupPanel.getCells();


        if (newCells == null || newCells.isEmpty()) {
            logger.warn("Cannot start simulation: no cells generated");
            return;
        }

        logger.info("Starting simulation with scenario: {}", scenario.getName());

        // Initialize engine with scenario (runs PreComputePhase)
        engine.initializeFromScenario(scenario, newCells);

        // Record T_0 setup and events in Timeline
        timeline.clear();
        timeline.addEntry(scenario.getStartDateYear(), "SETUP", "Scénario Initial : " + scenario.getName(),
            String.format("Pop: %,d | Tech: %.1f | Motif: %s", scenario.getInitialHumanCount(), scenario.getInitialTechLevel(), scenario.getPopulationDensityType()), false);

        for (var evt : setupPanel.getScheduledEvents()) {
            timeline.addEntry(evt.getYear(), evt.getType().toUpperCase(), evt.getName(),
                String.format("Lat: %.2f°, Lng: %.2f°, Mag: %.1f", evt.getLatitude(), evt.getLongitude(), evt.getMagnitude()), false);
        }

        if (godModePanel != null) {
            godModePanel.refreshTimelineView();
        }

        // Update UI components
        mapCanvas.setWorldBuffer(engine.getWorldBuffer());
        mapCanvas.setCells(newCells);
        if (miniMap != null) miniMap.setCells(newCells);

        // Update control panel with scenario info
        controlPanel.updateScenarioName(scenario.getName());
        controlPanel.updateYear(String.valueOf(scenario.getStartDateYear()));
        if (mapCanvas != null) {
            mapCanvas.setScenarioName(scenario.getName());
        }

        // Enable Execution Context tab (Tab 4) and switch to it after Scenario Setup (Tab 3) validation
        executionContextTab.setDisable(false);
        if (tabPane.getSelectionModel().getSelectedItem() == setupTab) {
            tabPane.getSelectionModel().select(executionContextTab);
            logger.info("Execution Context tab enabled and selected after scenario setup validation");
        } else {
            simulationTab.setDisable(false);
            tabPane.getSelectionModel().select(simulationTab);
            logger.info("Simulation tab activated with {} cells", newCells.size());
        }
    }

    private void launchSimulationFromContext() {
        if (setupPanel != null) {
            org.ether.society.model.Scenario currentScenario = setupPanel.getScenario();
            List<H3Cell> cells = setupPanel.getCells();
            if (currentScenario != null && cells != null && !cells.isEmpty()) {
                if (engine.getCells() == null || engine.getCells().isEmpty()) {
                    engine.initializeFromScenario(currentScenario, cells);
                    mapCanvas.setWorldBuffer(engine.getWorldBuffer());
                    mapCanvas.setCells(cells);
                    if (miniMap != null) miniMap.setCells(cells);
                    controlPanel.updateScenarioName(currentScenario.getName());
                    controlPanel.updateYear(String.valueOf(currentScenario.getStartDateYear()));
                }
            }
        }
        simulationTab.setDisable(false);
        tabPane.getSelectionModel().select(simulationTab);
        if (mapCanvas != null) {
            mapCanvas.resetView();
        }
        logger.info("Simulation tab enabled and activated from Execution Context Panel");
    }

    // Add ColorLegend helper
    public void addLegend(javafx.scene.Node legend) {
        if (simulationTab.getContent() instanceof BorderPane bp) {
            if (bp.getCenter() instanceof StackPane sp) {
                StackPane.setAlignment(legend, Pos.BOTTOM_LEFT);
                StackPane.setMargin(legend, new javafx.geometry.Insets(10));
                sp.getChildren().add(legend);
            }
        }
    }

    private void startEventPolling() {
        if (mapCanvas != null && engine != null) {
            mapCanvas.setEventSystem(engine.getEventSystem());
        }

        if (engine instanceof org.ether.society.core.H3SimulationEngine h3Engine) {
            final java.util.concurrent.atomic.AtomicLong lastUiUpdateNanos = new java.util.concurrent.atomic.AtomicLong(0);
            h3Engine.setOnTickCallback(() -> {
                long now = System.nanoTime();
                boolean isRecording = mapCanvas != null && mapCanvas.isRecordingVideo();
                // Throttle UI update calls to ~30 FPS unless video frame capture is requested
                if (isRecording || (now - lastUiUpdateNanos.get() >= 33_000_000L)) {
                    lastUiUpdateNanos.set(now);
                    int year = engine.getTimeManager().getCurrentYear();
                    int month = engine.getTimeManager().getCurrentMonth();
                    int day = engine.getTimeManager().getCurrentDay();
                    String dateStr = String.format("An %d - M.%02d D.%02d", year, month + 1, day);

                    long currentTick = h3Engine.getTickCounter();
                    javafx.application.Platform.runLater(() -> {
                        if (mapCanvas != null) {
                            mapCanvas.setCurrentDateStr(dateStr);
                            if (mapCanvas.isRecordingVideo()) {
                                mapCanvas.captureTickFrame(currentTick);
                            }
                        }
                    });
                }
            });
        }

        AnimationTimer eventLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 250_000_000) { // Check every 250ms for smooth beacon animation & logs
                    List<String> events = engine.getEventSystem().flushEvents();
                    if (!events.isEmpty()) {
                        for (String event : events) {
                            notificationOverlay.showEvent(event);
                            logger.info("Event triggered: {}", event);
                        }
                        if (controlPanel != null) {
                            controlPanel.logEvents(events);
                        }
                    }

                    // Redraw map for event beacons if active events exist
                    if (mapCanvas != null && mapCanvas.getEventSystem() != null && !mapCanvas.getEventSystem().getActiveEvents().isEmpty()) {
                        mapCanvas.draw();
                    }

                    // Update Global Age Display
                    if (controlPanel != null) {
                        float avgTech = ((org.ether.society.core.H3SimulationEngine)engine).getAverageTechnology();
                        String currentAge = getAgeName(avgTech);
                        controlPanel.updateAge(currentAge);
                        
                        // Update Stats
                        controlPanel.updateStats(
                            ((org.ether.society.core.H3SimulationEngine)engine).getTotalPopulation(),
                            ((org.ether.society.core.H3SimulationEngine)engine).getTotalFood(),
                            ((org.ether.society.core.H3SimulationEngine)engine).getPopulatedCellCount(),
                            ((org.ether.society.core.H3SimulationEngine)engine).getCurrentTPS()
                        );
                        
                        statsPanel.update();
                        
                        // Update Season
                        controlPanel.updateSeason(engine.getTimeManager().getCurrentMonth());
                    }

                    lastUpdate = now;
                }
            }
        };
        eventLoop.start();
    }

    private String getAgeName(float techLevel) {
        if (techLevel < 10) return "STONE AGE";
        if (techLevel < 30) return "BRONZE AGE";
        if (techLevel < 60) return "IRON AGE";
        if (techLevel < 100) return "CLASSICAL AGE";
        if (techLevel < 200) return "MEDIEVAL AGE";
        return "RENAISSANCE";
    }



    public void saveGame() {
        // Prompt for save name
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog("My Save");
        dialog.setTitle("Save Game");
        dialog.setHeaderText("Enter name for this save:");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(name -> {
            engine.saveGame(name);
            notificationOverlay.showEvent("Game Saved: " + name);
        });
    }

    public void loadGame() {
        // Simple Load (MVP: just load from DB)
        // In future: Show list of saves
        engine.loadGame(null);
        notificationOverlay.showEvent("Game Loaded from Database");
        
        // Refresh UI
        mapCanvas.setCells(engine.getCells());
        if (miniMap != null) miniMap.setCells(engine.getCells());
    }
}
