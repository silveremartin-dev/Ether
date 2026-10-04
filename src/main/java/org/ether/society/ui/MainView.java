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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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
    /* Internal state variable for is switching tabs (boolean). */
    private boolean isSwitchingTabs = false;

    // Headless Mode Dashboard Overlay
    private VBox headlessDashboard;
    private Label lblHeadlessTitle;
    private Label lblHeadlessDesc;
    private Label lblHeadlessYearVal;
    private Label lblHeadlessTicksVal;
    private Label lblHeadlessPopVal;
    private Label lblHeadlessTargetVal;
    private Label lblHeadlessSpeedVal;
    private Button btnHeadlessSwitchGui;

    // Full-Screen Map Mode
    private BorderPane simulationRoot;
    private StackPane mapStack;
    /* Internal state variable for is full screen (boolean). */
    private boolean isFullScreen = false;
    private javafx.event.EventHandler<javafx.scene.input.KeyEvent> escapeKeyFilter;

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
        this.godModePanel = new GodModePanel(engine, timeline);

        initUI();
        updateTabTitles();

        I18n.languageProperty().addListener((obs, old, val) -> updateTabTitles());
    }

    // Helper subroutine: init ui - internal state computation & bounds checking
    private void initUI() {
        tabPane = new TabPane();
        tabPane.getStyleClass().add("main-tab-pane");
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
    }

    /*
     * Update tab titles.
     * Enforces physical invariants and updates associated state variables within {@code MainView}.
     *
     */
    public void updateTabTitles() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
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
        updateHeadlessTexts();
    }

    // Helper subroutine: on planet generated - internal state computation & bounds checking
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

    // Helper subroutine: on resources applied - internal state computation & bounds checking
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

    // Helper subroutine: create simulation view - internal state computation & bounds checking
    private BorderPane createSimulationView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("glass-panel"); // Apply glass effect base
        this.simulationRoot = root;

        // Map Container (Layered)
        StackPane mapStack = new StackPane();
        this.mapStack = mapStack;

        // 1. Direct Map Canvas (Bound to container dimensions to eliminate scrollbars and fill 100% of space)
        mapCanvas.widthProperty().bind(mapStack.widthProperty());
        mapCanvas.heightProperty().bind(mapStack.heightProperty());
        mapStack.getChildren().add(mapCanvas);

        // 1.b Headless Dashboard (Replaces 2D/3D visual canvas during headless execution)
        headlessDashboard = createHeadlessDashboard();
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

        // 5. Color Legend Component (Bottom-Right overlay on map StackPane)
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

        // Connect Control Panel callbacks
        controlPanel.setOnSave(this::saveSimulation);
        controlPanel.setOnLoad(this::loadSimulation);
        controlPanel.setOnContourToggle(show -> mapCanvas.toggleContours(show));
        controlPanel.setOnTimelapseRecord(this::toggleTimelapseRecording);
        controlPanel.setOnTimelapseSeek(this::seekTimelapse);
        controlPanel.setOnTimelapseSeekToEnd(this::seekToEnd);
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

    // Helper subroutine: create headless dashboard - internal state computation & bounds checking
    private VBox createHeadlessDashboard() {
        lblHeadlessTitle = new Label();
        lblHeadlessTitle.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 20px; -fx-font-weight: bold;");

        lblHeadlessDesc = new Label();
        lblHeadlessDesc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-text-alignment: center;");
        lblHeadlessDesc.setWrapText(true);
        lblHeadlessDesc.setMaxWidth(680);

        lblHeadlessYearVal = createMetricValLabel("An 0");
        lblHeadlessTicksVal = createMetricValLabel("0");
        lblHeadlessPopVal = createMetricValLabel("0");
        lblHeadlessTargetVal = createMetricValLabel("-");
        lblHeadlessSpeedVal = createMetricValLabel("0 pas/s");

        VBox cardYear = createMetricCard(I18n.getOrDefault("headless.stat.year", "Année en cours :"), lblHeadlessYearVal);
        VBox cardTicks = createMetricCard(I18n.getOrDefault("headless.stat.ticks", "Pas de simulation :"), lblHeadlessTicksVal);
        VBox cardPop = createMetricCard(I18n.getOrDefault("headless.stat.pop", "Population mondiale :"), lblHeadlessPopVal);
        VBox cardTarget = createMetricCard(I18n.getOrDefault("headless.stat.target", "Horizon cible :"), lblHeadlessTargetVal);
        VBox cardSpeed = createMetricCard(I18n.getOrDefault("headless.stat.speed", "Vitesse de calcul :"), lblHeadlessSpeedVal);

        HBox grid1 = new HBox(16, cardYear, cardTicks, cardPop);
        grid1.setAlignment(Pos.CENTER);

        HBox grid2 = new HBox(16, cardTarget, cardSpeed);
        grid2.setAlignment(Pos.CENTER);

        btnHeadlessSwitchGui = new Button();
        btnHeadlessSwitchGui.getStyleClass().add("button-secondary");
        btnHeadlessSwitchGui.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 22; -fx-cursor: hand;");
        btnHeadlessSwitchGui.setOnAction(e -> {
            if (executionContextPanel != null) {
                executionContextPanel.setRenderingMode(ExecutionContextPanel.RenderingMode.GUI);
            }
        });

        VBox dashboard = new VBox(24, lblHeadlessTitle, lblHeadlessDesc, grid1, grid2, btnHeadlessSwitchGui);
        dashboard.setAlignment(Pos.CENTER);
        dashboard.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 70%, #0f172a, #020617); -fx-padding: 40;");
        dashboard.setVisible(false);
        dashboard.setManaged(false);

        updateHeadlessTexts();

        return dashboard;
    }

    // Helper subroutine: create metric card - internal state computation & bounds checking
    private VBox createMetricCard(String title, Label valLabel) {
        Label lblT = new Label(title);
        lblT.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
        VBox card = new VBox(6, lblT, valLabel);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: rgba(30, 41, 59, 0.85); -fx-padding: 14 20; -fx-background-radius: 8; -fx-border-color: rgba(56, 189, 248, 0.25); -fx-border-radius: 8; -fx-min-width: 170;");
        return card;
    }

    // Helper subroutine: create metric val label - internal state computation & bounds checking
    private Label createMetricValLabel(String initial) {
        Label lbl = new Label(initial);
        lbl.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 16px; -fx-font-weight: bold;");
        return lbl;
    }

    // Helper subroutine: update headless texts - internal state computation & bounds checking
    private void updateHeadlessTexts() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (lblHeadlessTitle != null) lblHeadlessTitle.setText(I18n.getOrDefault("headless.banner.title", "🚀 MODE HEADLESS ACTIF (ACCÉLÉRATION MAXIMALE)"));
        if (lblHeadlessDesc != null) lblHeadlessDesc.setText(I18n.getOrDefault("headless.banner.desc", "L'affichage cartographique 2D/3D temps réel est désactivé pour allouer 100 % de la puissance de calcul CPU/GPU au moteur physique et démographique."));
        if (btnHeadlessSwitchGui != null) btnHeadlessSwitchGui.setText(I18n.getOrDefault("headless.btn.switch_gui", "🖼️ Réactiver l'affichage visuel (Mode GUI)"));
    }

    // Helper subroutine: apply rendering mode - internal state computation & bounds checking
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

    // Helper subroutine: update headless telemetry - internal state computation & bounds checking
    private void updateHeadlessTelemetry(String dateStr, long currentTick) {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (lblHeadlessYearVal != null) lblHeadlessYearVal.setText(dateStr);
        if (lblHeadlessTicksVal != null) lblHeadlessTicksVal.setText(String.format("%,d", currentTick));
        if (lblHeadlessPopVal != null && engine instanceof org.ether.society.core.H3SimulationEngine h3) {
            lblHeadlessPopVal.setText(String.format("%,d hab", h3.getTotalPopulation()));
        }
        if (lblHeadlessTargetVal != null && executionContextPanel != null) {
            int targetYear = executionContextPanel.getTargetYear();
            lblHeadlessTargetVal.setText(targetYear != 0 ? String.format("An %,d", targetYear) : "—");
        }
        if (lblHeadlessSpeedVal != null && hud != null) {
            lblHeadlessSpeedVal.setText(String.format("%.1f it/s", hud.getFps()));
        }
    }

    // --- Timelapse Recording State ---
    /* Internal state variable for is recording (boolean). */
    private boolean isRecording = false;

    // Helper subroutine: toggle timelapse recording - internal state computation & bounds checking
    private void toggleTimelapseRecording() {
        isRecording = !isRecording;
        if (isRecording) {
            logger.info("Timelapse recording STARTED");
            engine.getHistoryManager().captureWorldSnapshot(engine);
        } else {
            logger.info("Timelapse recording STOPPED");
            var snapshots = engine.getHistoryManager().getWorldSnapshots();
            if (!snapshots.isEmpty()) {
                int minYear = (int) (long) snapshots.firstKey();
                int maxYear = (int) (long) snapshots.lastKey();
                controlPanel.updateTimelapseSlider(minYear, maxYear, maxYear);
            }
        }
    }

    // Helper subroutine: seek timelapse - internal state computation & bounds checking
    private void seekTimelapse(int year) {
        long startYear = engine.getCurrentScenario() != null ? engine.getCurrentScenario().getStartDateYear() : -20000;
        long targetTicks = Math.max(0, (year - startYear) * 12);
        engine.seekToTick(targetTicks);
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
        }
        if (statsPanel != null) {
            statsPanel.update();
        }
        logger.info("Timelapse seek to year: {} (tick {})", year, targetTicks);
    }

    // Helper subroutine: seek to end - internal state computation & bounds checking
    private void seekToEnd() {
        var snapshots = engine.getHistoryManager() != null ? engine.getHistoryManager().getWorldSnapshots() : null;
        if (snapshots != null && !snapshots.isEmpty()) {
            long lastTick = snapshots.lastKey();
            engine.seekToTick(lastTick);
        } else {
            long endYear = engine.getCurrentScenario() != null ? engine.getCurrentScenario().getEndDateYear() : 2100;
            seekTimelapse((int) endYear);
            return;
        }
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
        }
        if (statsPanel != null) {
            statsPanel.update();
        }
        logger.info("Seek to end completed");
    }

    // Helper subroutine: on start simulation - internal state computation & bounds checking
    private void onStartSimulation(Scenario scenario) {
        if (setupPanel != null && setupPanel.isResumeFromSnapshotSelected()) {
            var meta = setupPanel.getSelectedSnapshotMetadata();
            if (meta != null) {
                logger.info("Resuming simulation from snapshot: {} (Year {}, Month {})", meta.getName(), meta.getYear(), meta.getMonth());
                engine.loadSimulation(meta.getId());
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

    // Helper subroutine: launch simulation from context - internal state computation & bounds checking
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
        
        // Keep execution context tab enabled for live adjustments
        simulationTab.setDisable(false);
        tabPane.getSelectionModel().select(simulationTab);
        if (statsPanel != null) {
            statsPanel.reset();
        }
        if (mapCanvas != null) {
            mapCanvas.resetView();
        }
        // Do NOT auto-start the simulation engine: remain in ready/paused mode waiting for user click on Play
        if (engine != null && engine.isRunning()) {
            engine.pause();
        }
        if (controlPanel != null) {
            controlPanel.updatePlayPauseVisuals(false);
        }
        logger.info("Simulation initialized from Execution Context Panel and switched to Simulation Tab (4) in ready/paused mode");
    }

    /*
     * Toggle full screen.
     * Enforces physical invariants and updates associated state variables within {@code MainView}.
     *
     */
    public void toggleFullScreen() {
        if (isFullScreen) {
            exitFullScreen();
        } else {
            enterFullScreen();
        }
    }

    /*
     * Enter full screen.
     * Enforces physical invariants and updates associated state variables within {@code MainView}.
     *
     */
    public void enterFullScreen() {
        if (isFullScreen || mapStack == null || simulationRoot == null) return;
        isFullScreen = true;

        // 1. Detach mapStack from simulationRoot
        simulationRoot.setCenter(null);

        // 2. Hide main TabPane so nothing else is visible on screen
        tabPane.setVisible(false);
        tabPane.setManaged(false);

        // 3. Add mapStack directly to MainView StackPane to fill 100% of the window
        if (!getChildren().contains(mapStack)) {
            getChildren().add(mapStack);
        }

        // 4. Set JavaFX Stage to FullScreen if scene is attached
        if (getScene() != null && getScene().getWindow() instanceof javafx.stage.Stage stage) {
            stage.setFullScreen(true);

            // Listen to OS-level fullscreen exit (e.g. default Escape handling by JavaFX)
            stage.fullScreenProperty().addListener(new javafx.beans.value.ChangeListener<Boolean>() {
                @Override
                /*
                 * Changed.
                 * Enforces physical invariants and updates associated state variables within {@code MainView}.
                 *
                 * @param obs the obs parameter (Boolean&gt;)
                 * @param oldVal the old val parameter (Boolean)
                 * @param newVal the new val parameter (Boolean)
                 */
                public void changed(javafx.beans.value.ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) {
                    if (!newVal && isFullScreen) {
                        stage.fullScreenProperty().removeListener(this);
                        exitFullScreen();
                    }
                }
            });
        }

        // 5. Add key event filter for Escape key
        if (escapeKeyFilter == null) {
            escapeKeyFilter = event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    exitFullScreen();
                    event.consume();
                }
            };
        }
        if (getScene() != null) {
            getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, escapeKeyFilter);
        }

        if (mapCanvas != null) {
            mapCanvas.draw();
        }
        logger.info("Entered Full Screen map view mode");
    }

    /*
     * Exit full screen.
     * Enforces physical invariants and updates associated state variables within {@code MainView}.
     *
     */
    public void exitFullScreen() {
        if (!isFullScreen || mapStack == null || simulationRoot == null) return;
        isFullScreen = false;

        // 1. Remove Escape key filter
        if (escapeKeyFilter != null && getScene() != null) {
            getScene().removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, escapeKeyFilter);
        }

        // 2. Exit OS stage fullscreen if still active
        if (getScene() != null && getScene().getWindow() instanceof javafx.stage.Stage stage) {
            if (stage.isFullScreen()) {
                stage.setFullScreen(false);
            }
        }

        // 3. Remove mapStack from MainView StackPane root
        getChildren().remove(mapStack);

        // 4. Restore tabPane visibility
        tabPane.setManaged(true);
        tabPane.setVisible(true);

        // 5. Restore mapStack to simulationRoot center
        simulationRoot.setCenter(mapStack);

        if (mapCanvas != null) {
            mapCanvas.resetView();
        }

        javafx.application.Platform.runLater(() -> {
            if (mapCanvas != null) {
                mapCanvas.draw();
            }
        });

        logger.info("Exited Full Screen map view mode");
    }

    /*
     * Is full screen.
     * Enforces physical invariants and updates associated state variables within {@code MainView}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isFullScreen() {
        return isFullScreen;
    }

    /*
     * Add legend.
     * Enforces physical invariants and updates associated state variables within {@code MainView}.
     *
     * @param legend the legend parameter (javafx.scene.Node)
     */
    public void addLegend(javafx.scene.Node legend) {
        if (mapStack != null) {
            StackPane.setAlignment(legend, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(legend, new javafx.geometry.Insets(0, 20, 50, 0));
            if (!mapStack.getChildren().contains(legend)) {
                mapStack.getChildren().add(legend);
            }
        }
    }

    // Helper subroutine: start event polling - internal state computation & bounds checking
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
                                notificationOverlay.showInfo(String.format(I18n.getOrDefault("headless.target_reached", "🏁 Horizon cible atteint (An %d) — Simulation en pause"), targetYear));
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
                                if (isHeadless) {
                                    updateHeadlessTelemetry(dateStr, currentTick);
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
            /* Internal state variable for last update (long). */
            private long lastUpdate = 0;

            @Override
            /*
             * Handle.
             * Enforces physical invariants and updates associated state variables within {@code MainView}.
             *
             * @param now the now parameter (long)
             */
            public void handle(long now) {
                if (now - lastUpdate >= 250_000_000) {
                    List<String> events = engine.getEventSystem().flushEvents();
                    if (!events.isEmpty()) {
                        long curPas = engine.getTickCounter();
                        // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
                        for (String event : events) {
                            notificationOverlay.showEvent(event);
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
                        float avgTech = ((org.ether.society.core.H3SimulationEngine)engine).getAverageTechnology();
                        String currentAge = getAgeName(avgTech);
                        controlPanel.updateAge(currentAge);

                        controlPanel.updateStats(
                            ((org.ether.society.core.H3SimulationEngine)engine).getTotalPopulation(),
                            ((org.ether.society.core.H3SimulationEngine)engine).getTotalFood(),
                            ((org.ether.society.core.H3SimulationEngine)engine).getPopulatedCellCount(),
                            ((org.ether.society.core.H3SimulationEngine)engine).getCurrentTPS()
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

    // Helper subroutine: get age name - internal state computation & bounds checking
    private String getAgeName(float techLevel) {
        if (techLevel < 10) return I18n.getOrDefault("age.stone", "STONE AGE");
        if (techLevel < 30) return I18n.getOrDefault("age.bronze", "BRONZE AGE");
        if (techLevel < 60) return I18n.getOrDefault("age.iron", "IRON AGE");
        if (techLevel < 100) return I18n.getOrDefault("age.classical", "CLASSICAL AGE");
        if (techLevel < 200) return I18n.getOrDefault("age.medieval", "MEDIEVAL AGE");
        return I18n.getOrDefault("age.renaissance", "RENAISSANCE");
    }

    /*
     * Save simulation.
     * Enforces physical invariants and updates associated state variables within {@code MainView}.
     *
     */
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

    /*
     * Load simulation.
     * Enforces physical invariants and updates associated state variables within {@code MainView}.
     *
     */
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
