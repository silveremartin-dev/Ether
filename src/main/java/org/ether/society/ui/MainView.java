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
import javafx.scene.layout.Priority;
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
    private Tab controlTab;
    private Tab statsTab;
    private Tab godModeTab;
    private PlanetGeneratorPanel planetGeneratorPanel;
    private ResourceDistributionPanel resourcePanel;
    private ScenarioSetupPanel setupPanel;
    private ExecutionContextPanel executionContextPanel;
    private ComparativeAnalyticsPanel comparativeAnalyticsPanel;
    private PreferencesPanel preferencesPanel;
    private NotificationOverlay notificationOverlay;
    private ColorLegend colorLegend;
    /** Internal state: is switching tabs (boolean). */
    private boolean isSwitchingTabs = false;

    // Extracted Coordinators
    private HeadlessDashboardOverlay headlessDashboard;
    private SimulationTimelineCoordinator timelineCoordinator;
    private FullScreenMapCoordinator fullScreenCoordinator;

    // Full-Screen & Simulation Layout Nodes
    private BorderPane simulationRoot;
    private StackPane mapStack;

    public MainView(H3SimulationEngine engine, ControlPanel controlPanel, H3MapCanvas mapCanvas, MiniMap miniMap,
            PerformanceHUD hud) {
        this.engine = engine;
        this.controlPanel = controlPanel;
        this.mapCanvas = mapCanvas;
        this.miniMap = miniMap;
        this.hud = hud;
        this.statsPanel = new StatsPanel(engine);
        this.statsPanel.setOnDisplayModeRequested(mode -> {
            if (mapCanvas != null) {
                mapCanvas.setDisplayMode(mode);
            }
        });
        this.timeline = new org.ether.society.model.ScenarioTimeline();
        if (engine != null) {
            engine.setTimeline(this.timeline);
        }
        this.godModePanel = new GodModePanel(engine, timeline);
        this.timelineCoordinator = new SimulationTimelineCoordinator(engine, controlPanel, this::refreshViewsAfterSeek);

        initUI();
        updateTabTitles();

        I18n.languageProperty().addListener((obs, oldVal, newVal) -> updateTabTitles());
    }

    private void initUI() {
        tabPane = new TabPane();
        tabPane.getStyleClass().add("main-tab-pane");
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
        setupPanel.setIsSimulationRunningSupplier(() -> engine != null && engine.isRunning());
        if (planetGeneratorPanel != null) {
            setupPanel.setPlanetPanelSupplier(() -> planetGeneratorPanel);
        }
        if (resourcePanel != null) {
            setupPanel.setResourcePanelSupplier(() -> resourcePanel);
        }
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

        // 4. Simulation Tab
        simulationTab = new Tab();
        simulationTab.setContent(createSimulationView());
        simulationTab.setDisable(true); // Disabled until started

        // 5. Comparative Analytics Tab (Offline Benchmark & Sensitivity Analytics)
        comparativeAnalyticsPanel = new ComparativeAnalyticsPanel();
        comparativeAnalyticsPanel.setInteractiveSimulationControllers(
            () -> engine != null && engine.isRunning(),
            () -> {
                if (engine != null && engine.isRunning()) {
                    engine.pause();
                    if (controlPanel != null) {
                        controlPanel.updatePlayPauseVisuals(false);
                    }
                    if (notificationOverlay != null) {
                        notificationOverlay.showWarning(I18n.getOrDefault("mainview.interactive_paused_for_batch", "⏸️ Simulation interactive mise en pause pour prioriser les calculs comparatifs"));
                    }
                }
            }
        );
        comparativeAnalyticsPanel.setSimulationEngineSupplier(() -> engine);
        comparativeAnalyticsTab = new Tab();
        comparativeAnalyticsTab.setContent(comparativeAnalyticsPanel);
        comparativeAnalyticsTab.setClosable(false);

        // 6. Execution Context Tab (Hardware Acceleration & Infrastructure Settings)
        executionContextPanel = new ExecutionContextPanel(this::launchSimulationFromContext);
        executionContextPanel.setOnLiveConfigChangedCallback(mode -> {
            logger.info("Live execution context hardware mode changed: {}", mode);
            if (engine != null && engine.getPerformanceConfig() != null) {
                switch (mode) {
                    case NATIVE_RUST -> engine.getPerformanceConfig().setExecutionEngineMode(org.ether.society.config.SimulationPerformanceConfig.ExecutionEngineMode.RUST_NATIVE);
                    case JAVA_VECTOR_SIMD -> engine.getPerformanceConfig().setExecutionEngineMode(org.ether.society.config.SimulationPerformanceConfig.ExecutionEngineMode.JAVA_VECTOR_SIMD);
                    case CPU_JIT, GPU_OFF, GPU_SHADERS -> engine.getPerformanceConfig().setExecutionEngineMode(org.ether.society.config.SimulationPerformanceConfig.ExecutionEngineMode.JAVA_STANDARD);
                }
            }
            if (comparativeAnalyticsPanel != null) {
                comparativeAnalyticsPanel.recalculateAllEstimates();
            }
            if (notificationOverlay != null) {
                notificationOverlay.showInfo("⚡ " + String.format(I18n.getOrDefault("exec.live_applied_notice", "Moteur de calcul actualisé en direct : %s"), mode.name()));
            }
        });
        executionContextPanel.setOnLiveRenderingModeChangedCallback(this::applyRenderingMode);
        executionContextTab = new Tab();
        executionContextTab.setContent(executionContextPanel);
        executionContextTab.setClosable(false);

        // 7. Preferences Tab
        preferencesPanel = new PreferencesPanel();
        preferencesTab = new Tab();
        preferencesTab.setContent(preferencesPanel);
        preferencesTab.setClosable(false);

        // Tab selection change listener
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (isSwitchingTabs) return;

            if (oldTab != null && oldTab != newTab) {
                boolean cancelled = false;
                javafx.stage.Window window = getScene() != null ? getScene().getWindow() : null;
                if (oldTab == planetTab && planetGeneratorPanel != null && planetGeneratorPanel.isDirty()) {
                    if (!planetGeneratorPanel.promptSaveIfDirty(window)) {
                        cancelled = true;
                    }
                } else if (oldTab == resourcesTab && resourcePanel != null && resourcePanel.isDirty()) {
                    if (!resourcePanel.promptSaveIfDirty(window)) {
                        cancelled = true;
                    }
                } else if (oldTab == setupTab && setupPanel != null && setupPanel.isDirty()) {
                    if (!setupPanel.promptSaveIfDirty(window)) {
                        cancelled = true;
                    }
                }

                if (cancelled) {
                    javafx.application.Platform.runLater(() -> {
                        isSwitchingTabs = true;
                        try {
                            tabPane.getSelectionModel().select(oldTab);
                        } finally {
                            isSwitchingTabs = false;
                        }
                    });
                    return;
                }
            }

            boolean isSim = (newTab == simulationTab);
            if (mapCanvas != null) {
                mapCanvas.setTabVisible(isSim);
            }
            if (isSim) {
                if (mapCanvas != null) {
                    mapCanvas.resetView();
                }
            }
            if (newTab == comparativeAnalyticsTab && comparativeAnalyticsPanel != null) {
                comparativeAnalyticsPanel.refreshRunList();
            }
            if (newTab == setupTab && setupPanel != null) {
                setupPanel.ensurePreviewGeneratedIfNeeded();
                setupPanel.refreshSnapshotList();
            }
        });

        tabPane.getTabs().addAll(planetTab, resourcesTab, setupTab, simulationTab, comparativeAnalyticsTab, executionContextTab, preferencesTab);

        getChildren().add(tabPane);

        fullScreenCoordinator = new FullScreenMapCoordinator(this, tabPane, simulationRoot, mapStack, mapCanvas);
    }

    /**
     * Update tab titles.
     */
    public void updateTabTitles() {
        planetTab.setText("1. " + org.ether.society.i18n.I18n.get("tab.planet_generator"));
        resourcesTab.setText("2. " + org.ether.society.i18n.I18n.get("tab.resources"));
        setupTab.setText("3. " + org.ether.society.i18n.I18n.get("tab.scenario"));
        simulationTab.setText("4. " + org.ether.society.i18n.I18n.get("tab.simulation"));
        comparativeAnalyticsTab.setText("5. " + org.ether.society.i18n.I18n.getOrDefault("tab.comparative_analytics", "Comparative Analytics"));
        executionContextTab.setText("6. " + org.ether.society.i18n.I18n.getOrDefault("tab.execution_context", "⚡ Execution Context"));
        preferencesTab.setText("7. " + org.ether.society.i18n.I18n.get("tab.preferences"));
        if (controlTab != null) {
            controlTab.setText(org.ether.society.i18n.I18n.getOrDefault("sim.tab.controls", "🎛️ 3D Render & Controls"));
        }
        if (statsTab != null) {
            statsTab.setText(org.ether.society.i18n.I18n.getOrDefault("sim.tab.stats", "📊 Stats"));
        }
        if (godModeTab != null) {
            godModeTab.setText(org.ether.society.i18n.I18n.getOrDefault("sim.tab.godmode", "⚡ God Mode"));
        }
        if (headlessDashboard != null) {
            headlessDashboard.updateHeadlessTexts();
        }
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
        root.getStyleClass().add("glass-panel");
        this.simulationRoot = root;

        // Map Container (Layered)
        mapStack = new StackPane();

        // 1. Direct Map Canvas
        mapCanvas.widthProperty().bind(mapStack.widthProperty());
        mapCanvas.heightProperty().bind(mapStack.heightProperty());
        mapStack.getChildren().add(mapCanvas);

        // 1.b Headless Dashboard
        headlessDashboard = new HeadlessDashboardOverlay(mode -> {
            if (executionContextPanel != null) {
                executionContextPanel.setRenderingMode(mode);
            }
        });
        headlessDashboard.setSuppliers(
            () -> engine != null ? engine.getTotalPopulation() : 0L,
            () -> executionContextPanel != null ? executionContextPanel.getTargetYear() : 0
        );
        mapStack.getChildren().add(headlessDashboard);

        // 2. Notification Overlay
        notificationOverlay = new NotificationOverlay();
        notificationOverlay.setPickOnBounds(false);
        notificationOverlay.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
        StackPane.setAlignment(notificationOverlay, Pos.BOTTOM_LEFT);
        mapStack.getChildren().add(notificationOverlay);

        if (controlPanel != null) {
            controlPanel.setNotificationOverlay(notificationOverlay);
        }

        // 4. Deferred Calculation Progress Overlay for Tab 4
        ProgressBar simProgressBar = new ProgressBar(0);
        simProgressBar.setMaxWidth(400);
        simProgressBar.setPrefHeight(18);

        Label simStatusLabel = new Label(I18n.getOrDefault("mainview.status.init", "⚡ Initialisation de la simulation..."));
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

        // 5. Color Legend Component
        colorLegend = new ColorLegend();
        colorLegend.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
        colorLegend.updateFromCanvas(mapCanvas);
        if (controlPanel != null) {
            controlPanel.setColorLegend(colorLegend);
            controlPanel.setMapCanvas(mapCanvas);
        }
        StackPane.setAlignment(colorLegend, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(colorLegend, new javafx.geometry.Insets(0, 16, 40, 0));
        mapStack.getChildren().add(colorLegend);

        startEventPolling();

        mapCanvas.setTooltipContainer(mapStack);

        // Connect Control Panel callbacks to timeline coordinator
        controlPanel.setOnSave(this::saveSimulation);
        controlPanel.setOnLoad(this::loadSimulation);
        controlPanel.setOnContourToggle(show -> mapCanvas.toggleContours(show));
        controlPanel.setOnTimelapseRecord(timelineCoordinator::toggleTimelapseRecording);
        controlPanel.setOnTimelapseSeek(timelineCoordinator::seekTimelapse);
        controlPanel.setOnTimelapseSeekToEnd(timelineCoordinator::seekToEndSafe);
        controlPanel.setOnRewindStart(timelineCoordinator::seekToStartSafe);
        controlPanel.setOnFastRewind(() -> timelineCoordinator.stepBackwardSafe(true));
        controlPanel.setOnStepBack(() -> timelineCoordinator.stepBackwardSafe(false));
        controlPanel.setOnStepForward(() -> timelineCoordinator.stepForwardSafe(false));
        controlPanel.setOnFastForward(() -> timelineCoordinator.stepForwardSafe(true));
        controlPanel.setOnSeekToEnd(timelineCoordinator::seekToEndSafe);
        controlPanel.setOnFullScreen(this::toggleFullScreen);

        if (godModePanel != null) {
            godModePanel.setMapCanvas(mapCanvas);
            godModePanel.setNotificationOverlay(notificationOverlay);
        }

        TabPane leftSidebar = new TabPane();
        leftSidebar.setPrefWidth(480);
        leftSidebar.setMinWidth(480);
        leftSidebar.setStyle("-fx-background-color: transparent;");
        leftSidebar.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        javafx.scene.control.ScrollPane controlScroll = new javafx.scene.control.ScrollPane(controlPanel);
        controlScroll.setFitToWidth(true);
        controlScroll.setPannable(true);
        controlScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        controlScroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        controlScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        javafx.scene.control.ScrollPane statsScroll = new javafx.scene.control.ScrollPane(statsPanel);
        statsScroll.setFitToWidth(true);
        statsScroll.setPannable(true);
        statsScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        statsScroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        statsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        javafx.scene.control.ScrollPane godScroll = new javafx.scene.control.ScrollPane(godModePanel);
        godScroll.setFitToWidth(true);
        godScroll.setPannable(true);
        godScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        godScroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        godScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        this.controlTab = new Tab(I18n.getOrDefault("sim.tab.controls", "🎛️ 3D Render & Controls"), controlScroll);
        this.statsTab = new Tab(I18n.getOrDefault("sim.tab.stats", "📊 Stats"), statsScroll);
        this.godModeTab = new Tab(I18n.getOrDefault("sim.tab.godmode", "⚡ Mode Dieu"), godScroll);
        leftSidebar.getTabs().addAll(controlTab, statsTab, godModeTab);

        leftSidebar.getSelectionModel().selectedItemProperty().addListener((obs, oldSubTab, newSubTab) -> {
            if (newSubTab == godModeTab) {
                logger.info("Auto-pausing simulation due to switching to God Mode / Climate Event tab");
                if (engine != null && engine.isRunning()) {
                    engine.pause();
                    if (controlPanel != null) {
                        controlPanel.updatePlayPauseVisuals(false);
                    }
                }
            }
        });

        mapCanvas.setOnCellClickedCallback((lat, lng) -> {
            if (godModePanel != null) {
                godModePanel.updateCoordinates(lat, lng);
            }
        });

        root.setLeft(leftSidebar);
        root.setCenter(mapStack);

        if (executionContextPanel != null) {
            applyRenderingMode(executionContextPanel.getRenderingMode());
        }

        return root;
    }

    private void applyRenderingMode(ExecutionContextPanel.RenderingMode mode) {
        boolean isHeadless = (mode == ExecutionContextPanel.RenderingMode.HEADLESS);
        if (mapCanvas != null) {
            mapCanvas.setVisible(!isHeadless);
        }
        if (colorLegend != null) {
            colorLegend.setVisible(!isHeadless);
        }
        if (headlessDashboard != null) {
            headlessDashboard.setVisible(isHeadless);
            headlessDashboard.setManaged(isHeadless);
        }
        if (!isHeadless && mapCanvas != null) {
            mapCanvas.draw();
        }
        logger.info("Simulation visual rendering mode updated: {}", mode);
    }

    private void refreshViewsAfterSeek() {
        if (engine == null) return;
        if (mapCanvas != null) {
            mapCanvas.setWorldBuffer(engine.getWorldBuffer());
            mapCanvas.setCells(engine.getCells());
            mapCanvas.draw();
        }
        if (miniMap != null && engine.getCells() != null) {
            miniMap.setCells(engine.getCells());
        }
        if (controlPanel != null) {
            controlPanel.updateYear(engine.getTimeManager().getFormattedDate());
            controlPanel.updatePlayPauseVisuals(false);
        }
        if (statsPanel != null) {
            statsPanel.update();
        }
    }

    private void onStartSimulation(Scenario scenario) {
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
                timeline.addEntry((int) meta.getYear(), "REPRISE_SNAPSHOT", I18n.getOrDefault("mainview.timeline.resume_title", "Reprise depuis Snapshot : ") + meta.getName(),
                    String.format(I18n.getOrDefault("mainview.timeline.resume_details", "Restored at Year %,d (Month %d) - Scenario %s"), meta.getYear(), meta.getMonth(), meta.getScenarioName()), false);

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

                // Move directly to Tab 4 (Simulation)
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

        // Auto-pause background batch execution if running in Comparative Analytics (Tab 5)
        if (comparativeAnalyticsPanel != null && comparativeAnalyticsPanel.isBatchRunning()) {
            logger.info("Auto-pausing background comparative batch execution as a new simulation is starting");
            comparativeAnalyticsPanel.pauseOrCancelBatchForInteractiveSimulation();
            if (notificationOverlay != null) {
                notificationOverlay.showWarning(I18n.getOrDefault("mainview.batch_paused_notice", "⏸️ Calculs comparatifs en tâche de fond mis en pause pour allouer les ressources à la nouvelle simulation"));
            }
        }

        logger.info("Starting simulation with scenario: {}", scenario.getName());

        engine.initializeFromScenario(scenario, newCells);

        timeline.clear();
        timeline.addEntry(scenario.getStartDateYear(), "SETUP", I18n.getOrDefault("mainview.timeline.init_title", "Initial Scenario: ") + scenario.getName(),
            String.format("Pop: %,d | Tech: %.1f | Motif: %s", scenario.getInitialHumanCount(), scenario.getInitialTechLevel(), scenario.getPopulationDensityType()), false);

        for (var evt : setupPanel.getScheduledEvents()) {
            timeline.addEntry(evt.getYear(), evt.getType().toUpperCase(), evt.getName(),
                String.format("Lat: %.2f°, Lng: %.2f°, Mag: %.1f", evt.getLatitude(), evt.getLongitude(), evt.getMagnitude()), false);
        }

        if (godModePanel != null) {
            godModePanel.refreshTimelineView();
        }
        if (statsPanel != null) {
            statsPanel.reset();
        }

        mapCanvas.setWorldBuffer(engine.getWorldBuffer());
        mapCanvas.setCells(newCells);
        if (miniMap != null) miniMap.setCells(newCells);

        controlPanel.updateScenarioName(scenario.getName());
        controlPanel.updateYear(String.format("An %d", scenario.getStartDateYear()));
        if (mapCanvas != null) {
            mapCanvas.setScenarioName(scenario.getName());
            mapCanvas.setCurrentDateStr(String.format("An %d", scenario.getStartDateYear()));
        }

        // Move directly to Tab 4 (Simulation) in ready/paused mode
        simulationTab.setDisable(false);
        tabPane.getSelectionModel().select(simulationTab);
        logger.info("Transitioned directly to Simulation Tab (4) with {} cells in ready/paused mode", newCells.size());
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
                    controlPanel.updateYear(String.format("An %d", currentScenario.getStartDateYear()));
                    if (mapCanvas != null) {
                        mapCanvas.setCurrentDateStr(String.format("An %d", currentScenario.getStartDateYear()));
                    }
                }
            }
        }
        
        simulationTab.setDisable(false);
        tabPane.getSelectionModel().select(simulationTab);
        if (statsPanel != null) {
            statsPanel.reset();
        }
        if (mapCanvas != null) {
            mapCanvas.resetView();
        }
        if (engine != null && engine.isRunning()) {
            engine.pause();
        }
        if (controlPanel != null) {
            controlPanel.updatePlayPauseVisuals(false);
        }
        logger.info("Simulation initialized from Execution Context Panel and switched to Simulation Tab (4) in ready/paused mode");
    }

    public void toggleFullScreen() {
        if (fullScreenCoordinator != null) {
            fullScreenCoordinator.toggleFullScreen();
        }
    }

    public void enterFullScreen() {
        if (fullScreenCoordinator != null) {
            fullScreenCoordinator.enterFullScreen();
        }
    }

    public void exitFullScreen() {
        if (fullScreenCoordinator != null) {
            fullScreenCoordinator.exitFullScreen();
        }
    }

    public boolean isFullScreen() {
        return fullScreenCoordinator != null && fullScreenCoordinator.isFullScreen();
    }

    public void addLegend(javafx.scene.Node legend) {
        if (mapStack != null) {
            StackPane.setAlignment(legend, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(legend, new javafx.geometry.Insets(0, 20, 50, 0));
            if (!mapStack.getChildren().contains(legend)) {
                mapStack.getChildren().add(legend);
            }
        }
    }

    private void startEventPolling() {
        if (mapCanvas != null && engine != null) {
            mapCanvas.setEventSystem(engine.getEventSystem());
        }

        if (engine instanceof org.ether.society.core.H3SimulationEngine h3Engine) {
            final java.util.concurrent.atomic.AtomicLong lastUiUpdateNanos = new java.util.concurrent.atomic.AtomicLong(0);
            final java.util.concurrent.atomic.AtomicBoolean renderPending = new java.util.concurrent.atomic.AtomicBoolean(false);

            h3Engine.setOnTickCallback(() -> {
                long now = System.nanoTime();
                boolean isRecording = mapCanvas != null && mapCanvas.isRecordingVideo();
                long currentTick = h3Engine.getTickCounter();

                double resDays = engine.getCurrentScenario() != null ? engine.getCurrentScenario().getTemporalResolutionDays() : 1.0;
                String dateStr = engine.getTimeManager().formatContextualDate(resDays);

                boolean isHeadless = executionContextPanel != null && executionContextPanel.getRenderingMode() == ExecutionContextPanel.RenderingMode.HEADLESS;

                if (isHeadless) {
                    int targetYear = executionContextPanel.getTargetYear();
                    long curYear = engine.getTimeManager().getCurrentYear();
                    if (targetYear != 0 && curYear >= targetYear && engine.isRunning()) {
                        engine.pause();
                        javafx.application.Platform.runLater(() -> {
                            if (controlPanel != null) controlPanel.updatePlayPauseVisuals(false);
                            if (notificationOverlay != null) {
                                notificationOverlay.showInfo(I18n.getOrDefault("headless.target_reached", "🏁 Horizon cible atteint (An {0}) — Simulation en pause", targetYear));
                            }
                        });
                    }
                }

                if (isRecording && mapCanvas != null) {
                    mapCanvas.setCurrentDateStr(dateStr);
                }

                long updateInterval = isHeadless ? 100_000_000L : 33_000_000L;
                if (now - lastUiUpdateNanos.get() >= updateInterval) {
                    if (renderPending.compareAndSet(false, true)) {
                        javafx.application.Platform.runLater(() -> {
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
                }
            });
        }

        AnimationTimer eventLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 250_000_000) {
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

                    boolean isHeadless = executionContextPanel != null && executionContextPanel.getRenderingMode() == ExecutionContextPanel.RenderingMode.HEADLESS;
                    if (!isHeadless && mapCanvas != null && mapCanvas.getEventSystem() != null && !mapCanvas.getEventSystem().getActiveEvents().isEmpty()) {
                        mapCanvas.draw();
                    }

                    if (controlPanel != null) {
                        float avgTech = engine.getAverageTechnology();
                        String currentAge = getAgeName(avgTech);
                        controlPanel.updateAge(currentAge);

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

    private String getAgeName(float techLevel) {
        if (techLevel < 10) return I18n.getOrDefault("age.stone", "STONE AGE");
        if (techLevel < 30) return I18n.getOrDefault("age.bronze", "BRONZE AGE");
        if (techLevel < 60) return I18n.getOrDefault("age.iron", "IRON AGE");
        if (techLevel < 100) return I18n.getOrDefault("age.classical", "CLASSICAL AGE");
        if (techLevel < 200) return I18n.getOrDefault("age.medieval", "MEDIEVAL AGE");
        return I18n.getOrDefault("age.renaissance", "RENAISSANCE");
    }

    public void saveSimulation() {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(I18n.getOrDefault("mainview.save.default_name", "Sauvegarde Scenario"));
        dialog.setTitle(I18n.getOrDefault("mainview.save.dialog_title", "Save Simulation"));
        dialog.setHeaderText(I18n.getOrDefault("mainview.save.dialog_header", "Entrez le nom de la sauvegarde :"));
        dialog.setContentText(I18n.getOrDefault("mainview.save.dialog_label", "Nom :"));

        dialog.showAndWait().ifPresent(name -> {
            engine.saveSimulation(name);
            notificationOverlay.showEvent(I18n.getOrDefault("mainview.save.success", "Simulation Saved: ") + name);
        });
    }

    public void loadSimulation() {
        engine.loadSimulation(null);
        notificationOverlay.showEvent(I18n.getOrDefault("mainview.load.success", "Simulation loaded from database"));

        mapCanvas.setWorldBuffer(engine.getWorldBuffer());
        mapCanvas.setCells(engine.getCells());
        mapCanvas.draw();
        if (miniMap != null) miniMap.setCells(engine.getCells());
        if (statsPanel != null) {
            statsPanel.resetChartSeries();
        }
        if (controlPanel != null) {
            controlPanel.updateYear(engine.getTimeManager().getFormattedDate());
        }
    }
}
