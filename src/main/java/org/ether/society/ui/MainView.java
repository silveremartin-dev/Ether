/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.model.ScenarioTimeline;
import org.ether.society.ui.mainview.MainViewLifecycleCoordinator;
import org.ether.society.ui.mainview.MainViewSimulationTabBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Main View Controller using a Tab-based layout.
 *
 * <p>Coordinates the 7-tab planetary simulation workflow (Planet Generation, Resources &amp; Ecology,
 * Scenario Setup, Interactive Simulation, Comparative Analytics, Execution Context, and Preferences),
 * delegating simulation tab construction to {@link MainViewSimulationTabBuilder} and simulation
 * lifecycle transitions and event polling to {@link MainViewLifecycleCoordinator}.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
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
    private final ScenarioTimeline timeline;
    private final GodModePanel godModePanel;

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
    private final SimulationTimelineCoordinator timelineCoordinator;
    private FullScreenMapCoordinator fullScreenCoordinator;

    // Full-Screen & Simulation Layout Nodes
    private BorderPane simulationRoot;
    private StackPane mapStack;

    /**
     * Constructs the primary tabbed application container.
     *
     * @param engine active H3 simulation engine
     * @param controlPanel interactive simulation control panel
     * @param mapCanvas cartographic H3 map canvas
     * @param miniMap global overview minimap
     * @param hud technical performance HUD
     */
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
        this.timeline = new ScenarioTimeline();
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
                            notificationOverlay.showWarning(I18n.getOrDefault("mainview.interactive_paused_for_batch",
                                    "⏸️ Simulation interactive mise en pause pour prioriser les calculs comparatifs"));
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
                    case NATIVE_RUST -> engine.getPerformanceConfig().setExecutionEngineMode(SimulationPerformanceConfig.ExecutionEngineMode.RUST_NATIVE);
                    case JAVA_VECTOR_SIMD -> engine.getPerformanceConfig().setExecutionEngineMode(SimulationPerformanceConfig.ExecutionEngineMode.JAVA_VECTOR_SIMD);
                    case CPU_JIT, GPU_OFF, GPU_SHADERS -> engine.getPerformanceConfig().setExecutionEngineMode(SimulationPerformanceConfig.ExecutionEngineMode.JAVA_STANDARD);
                }
            }
            if (comparativeAnalyticsPanel != null) {
                comparativeAnalyticsPanel.recalculateAllEstimates();
            }
            if (notificationOverlay != null) {
                notificationOverlay.showInfo("⚡ " + String.format(I18n.getOrDefault("exec.live_applied_notice",
                        "Moteur de calcul actualisé en direct : %s"), mode.name()));
            }
        });
        executionContextPanel.setOnLiveRenderingModeChangedCallback(this::applyRenderingMode);
        executionContextTab = new Tab();
        executionContextTab.setContent(executionContextPanel);
        executionContextTab.setClosable(false);

        // 4. Simulation Tab (built after executionContextPanel & setupPanel so references are bound)
        simulationTab = new Tab();
        simulationTab.setContent(createSimulationView());
        simulationTab.setDisable(true);

        // 7. Preferences Tab
        preferencesPanel = new PreferencesPanel();
        preferencesTab = new Tab();
        preferencesTab.setContent(preferencesPanel);
        preferencesTab.setClosable(false);

        // Tab selection change listener
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> handleTabSelectionChange(oldTab, newTab));

        tabPane.getTabs().addAll(planetTab, resourcesTab, setupTab, simulationTab, comparativeAnalyticsTab, executionContextTab, preferencesTab);
        getChildren().add(tabPane);

        fullScreenCoordinator = new FullScreenMapCoordinator(this, tabPane, simulationRoot, mapStack, mapCanvas);
    }

    private void handleTabSelectionChange(Tab oldTab, Tab newTab) {
        if (isSwitchingTabs) return;

        if (oldTab != null && oldTab != newTab) {
            boolean cancelled = false;
            Window window = getScene() != null ? getScene().getWindow() : null;
            if (oldTab == planetTab && planetGeneratorPanel != null && planetGeneratorPanel.isDirty()) {
                cancelled = !planetGeneratorPanel.promptSaveIfDirty(window);
            } else if (oldTab == resourcesTab && resourcePanel != null && resourcePanel.isDirty()) {
                cancelled = !resourcePanel.promptSaveIfDirty(window);
            } else if (oldTab == setupTab && setupPanel != null && setupPanel.isDirty()) {
                cancelled = !setupPanel.promptSaveIfDirty(window);
            }

            if (cancelled) {
                Platform.runLater(() -> {
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
            if (isSim) {
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
    }

    /**
     * Updates all localized tab titles across the main tab bar and simulation sidebar.
     */
    public void updateTabTitles() {
        planetTab.setText("1. " + I18n.get("tab.planet_generator"));
        resourcesTab.setText("2. " + I18n.get("tab.resources"));
        setupTab.setText("3. " + I18n.get("tab.scenario"));
        simulationTab.setText("4. " + I18n.get("tab.simulation"));
        comparativeAnalyticsTab.setText("5. " + I18n.getOrDefault("tab.comparative_analytics", "Comparative Analytics"));
        executionContextTab.setText("6. " + I18n.getOrDefault("tab.execution_context", "⚡ Execution Context"));
        preferencesTab.setText("7. " + I18n.get("tab.preferences"));
        if (controlTab != null) {
            controlTab.setText(I18n.getOrDefault("sim.tab.controls", "🎛️ 3D Render & Controls"));
        }
        if (statsTab != null) {
            statsTab.setText(I18n.getOrDefault("sim.tab.stats", "📊 Stats"));
        }
        if (godModeTab != null) {
            godModeTab.setText(I18n.getOrDefault("sim.tab.godmode", "⚡ God Mode"));
        }
        if (headlessDashboard != null) {
            headlessDashboard.updateHeadlessTexts();
        }
    }

    private BorderPane createSimulationView() {
        MainViewSimulationTabBuilder.SimulationTabComponents comps = MainViewSimulationTabBuilder.buildSimulationView(
                engine, controlPanel, mapCanvas, statsPanel, godModePanel, setupPanel,
                executionContextPanel, timelineCoordinator, this::saveSimulation, this::loadSimulation, this::toggleFullScreen
        );
        this.simulationRoot = comps.simulationRoot();
        this.mapStack = comps.mapStack();
        this.headlessDashboard = comps.headlessDashboard();
        this.notificationOverlay = comps.notificationOverlay();
        this.colorLegend = comps.colorLegend();
        this.controlTab = comps.controlTab();
        this.statsTab = comps.statsTab();
        this.godModeTab = comps.godModeTab();

        MainViewLifecycleCoordinator.startEventPolling(
                engine, mapCanvas, controlPanel, statsPanel, executionContextPanel,
                headlessDashboard, notificationOverlay, colorLegend, hud
        );

        if (executionContextPanel != null) {
            applyRenderingMode(executionContextPanel.getRenderingMode());
        }
        return this.simulationRoot;
    }

    private void onPlanetGenerated(List<H3Cell> cells) {
        MainViewLifecycleCoordinator.onPlanetGenerated(
                cells, engine, mapCanvas, miniMap, controlPanel,
                planetGeneratorPanel, resourcePanel, setupPanel, tabPane, resourcesTab
        );
    }

    private void onResourcesApplied(List<H3Cell> cells) {
        MainViewLifecycleCoordinator.onResourcesApplied(
                cells, engine, mapCanvas, miniMap, setupPanel, tabPane, setupTab
        );
    }

    private void applyRenderingMode(ExecutionContextPanel.RenderingMode mode) {
        boolean isHeadless = (mode == ExecutionContextPanel.RenderingMode.HEADLESS);
        if (mapCanvas != null) mapCanvas.setVisible(!isHeadless);
        if (colorLegend != null) colorLegend.setVisible(!isHeadless);
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
        MainViewLifecycleCoordinator.onStartSimulation(
                scenario, engine, timeline, setupPanel, comparativeAnalyticsPanel,
                godModePanel, statsPanel, controlPanel, mapCanvas, miniMap,
                notificationOverlay, tabPane, simulationTab
        );
    }

    private void launchSimulationFromContext() {
        MainViewLifecycleCoordinator.launchSimulationFromContext(
                engine, setupPanel, statsPanel, controlPanel, mapCanvas, miniMap, tabPane, simulationTab
        );
    }

    /**
     * Toggles distraction-free full-screen map mode.
     */
    public void toggleFullScreen() {
        if (fullScreenCoordinator != null) {
            fullScreenCoordinator.toggleFullScreen();
        }
    }

    /**
     * Enters distraction-free full-screen map mode.
     */
    public void enterFullScreen() {
        if (fullScreenCoordinator != null) {
            fullScreenCoordinator.enterFullScreen();
        }
    }

    /**
     * Exits distraction-free full-screen map mode.
     */
    public void exitFullScreen() {
        if (fullScreenCoordinator != null) {
            fullScreenCoordinator.exitFullScreen();
        }
    }

    /**
     * Checks whether full-screen map mode is currently active.
     *
     * @return {@code true} if full-screen mode is active
     */
    public boolean isFullScreen() {
        return fullScreenCoordinator != null && fullScreenCoordinator.isFullScreen();
    }

    /**
     * Appends an auxiliary legend node onto the bottom-right corner of the map stack.
     *
     * @param legend JavaFX legend node
     */
    public void addLegend(Node legend) {
        if (mapStack != null) {
            StackPane.setAlignment(legend, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(legend, new Insets(0, 20, 50, 0));
            if (!mapStack.getChildren().contains(legend)) {
                mapStack.getChildren().add(legend);
            }
        }
    }

    /**
     * Opens interactive dialog to save the active simulation snapshot.
     */
    public void saveSimulation() {
        MainViewLifecycleCoordinator.saveSimulation(engine, notificationOverlay);
    }

    /**
     * Loads the latest simulation snapshot from persistence storage.
     */
    public void loadSimulation() {
        MainViewLifecycleCoordinator.loadSimulation(engine, mapCanvas, miniMap, statsPanel, controlPanel, notificationOverlay);
    }
}
