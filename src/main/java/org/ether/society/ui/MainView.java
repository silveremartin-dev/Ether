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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
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

    // UI Structure
    private TabPane tabPane;
    private Tab planetTab;
    private Tab resourcesTab;
    private Tab setupTab;
    private Tab simulationTab;
    private Tab preferencesTab;
    private PlanetGeneratorPanel planetGeneratorPanel;
    private ResourceDistributionPanel resourcePanel;
    private ScenarioSetupPanel setupPanel;
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
        resourcesTab = new Tab();
        resourcesTab.setContent(resourcePanel);
        resourcesTab.setClosable(false);

        // 3. Setup Tab
        setupPanel = new ScenarioSetupPanel(this::onStartSimulation);
        setupTab = new Tab();
        setupTab.setContent(setupPanel);
        setupTab.setClosable(false);

        // 4. Simulation Tab
        simulationTab = new Tab();
        simulationTab.setContent(createSimulationView());
        simulationTab.setDisable(true); // Disabled until started

        // 5. Preferences Tab
        preferencesPanel = new PreferencesPanel();
        preferencesTab = new Tab();
        preferencesTab.setContent(preferencesPanel);
        preferencesTab.setClosable(false);

        tabPane.getTabs().addAll(planetTab, resourcesTab, setupTab, simulationTab, preferencesTab);

        // Auto-pause when leaving simulation tab
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (oldTab == simulationTab && newTab != simulationTab) {
                logger.info("Auto-pausing simulation due to tab switch");
                engine.pause();
            }
        });

        getChildren().add(tabPane);
    }

    public void updateTabTitles() {
        planetTab.setText(org.ether.society.i18n.I18n.get("tab.planet_generator"));
        resourcesTab.setText(org.ether.society.i18n.I18n.get("tab.resources"));
        setupTab.setText(org.ether.society.i18n.I18n.get("tab.scenario"));
        simulationTab.setText(org.ether.society.i18n.I18n.get("tab.simulation"));
        preferencesTab.setText(org.ether.society.i18n.I18n.get("tab.preferences"));
    }

    private void onPlanetGenerated(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;
        logger.info("Planet generated with {} cells", cells.size());
        engine.setCells(cells);
        mapCanvas.setCells(cells);
        miniMap.setCells(cells);
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
        miniMap.setCells(cells);
        mapCanvas.draw();

        setupPanel.setGeneratedCells(cells);
        tabPane.getSelectionModel().select(setupTab);
    }

    private BorderPane createSimulationView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("glass-panel"); // Apply glass effect base

        // Map Container (Layered)
        StackPane mapStack = new StackPane();

        // 1. Scrollable Map
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(mapCanvas);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        mapStack.getChildren().add(scroll);

        // 2. Overlays
        StackPane.setAlignment(hud, Pos.TOP_RIGHT);
        StackPane.setMargin(hud, new javafx.geometry.Insets(10));
        mapStack.getChildren().add(hud);

        StackPane.setAlignment(miniMap, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(miniMap, new javafx.geometry.Insets(10));
        mapStack.getChildren().add(miniMap);

        // 3. Notification Overlay
        notificationOverlay = new NotificationOverlay();
        // Make sure it doesn't block mouse
        notificationOverlay.setPickOnBounds(false);
        mapStack.getChildren().add(notificationOverlay);

        startEventPolling();

        // Legend (New compact version will be added here? Or passed in App.java?)
        // Assuming App.java wires legend into a container, but here we construct the
        // layout.
        // We need to add ColorLegend here too if we want it shown.
        // Let's rely on caller or reconstruct. For now, let's assume valid.

        // NOTE: In App.java, mapCanvas.setTooltipContainer was called on the
        // 'mapContainer'.
        // We should ensure tooltip container is set correctly.
        mapCanvas.setTooltipContainer(mapStack);

        // Inject Visualization Engines
        if (engine.getWorldBuffer() != null) {
            // New DOD-aware injection would go here
        }

        // Connect Control Panel callbacks
        controlPanel.setOnAnalytics(this::showAnalytics);
        controlPanel.setOnSave(this::saveGame);
        controlPanel.setOnLoad(this::loadGame);
        controlPanel.setOnContourToggle(show -> mapCanvas.toggleContours(show));
        controlPanel.setOnStatsToggle(show -> {
            boolean visible = statsPanel.isVisible();
            statsPanel.setVisible(!visible);
            statsPanel.setManaged(!visible);
        });
        controlPanel.setOnTimelapseRecord(this::toggleTimelapseRecording);
        controlPanel.setOnTimelapseSeek(this::seekTimelapse);

        root.setCenter(mapStack);
        root.setRight(statsPanel);
        root.setBottom(controlPanel);

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
                int minYear = snapshots.firstKey();
                int maxYear = snapshots.lastKey();
                controlPanel.updateTimelapseSlider(minYear, maxYear, maxYear);
            }
        }
    }

    private void seekTimelapse(int year) {
        var snapshot = engine.getHistoryManager().getWorldSnapshot(year);
        if (snapshot != null) {
            mapCanvas.setCells(snapshot);
            miniMap.setCells(snapshot);
            controlPanel.updateYear(String.valueOf(year));
            logger.info("Timelapse seek to year: {}", year);
        }
    }

    private void onStartSimulation(Scenario scenario) {
        // Retrieve generated cells from setup
        List<H3Cell> newCells = setupPanel.getCells();

        if (newCells == null || newCells.isEmpty()) {
            logger.warn("Cannot start simulation: no cells generated");
            return;
        }

        logger.info("Starting simulation with scenario: {}", scenario.getName());

        // Initialize engine with scenario (runs PreComputePhase)
        engine.initializeFromScenario(scenario, newCells);

        // Update UI components
        mapCanvas.setCells(newCells);
        miniMap.setCells(newCells);

        // Update control panel with scenario info
        controlPanel.updateYear(String.valueOf(scenario.getStartDateYear()));

        // Switch Tab
        simulationTab.setDisable(false);
        tabPane.getSelectionModel().select(simulationTab);

        logger.info("Simulation tab activated with {} cells", newCells.size());
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
        AnimationTimer eventLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 500_000_000) { // Check every 500ms
                    List<String> events = engine.getEventSystem().flushEvents();
                    for (String event : events) {
                        notificationOverlay.showEvent(event);
                        logger.info("Event triggered: {}", event);
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

    private void showAnalytics() {
        if (engine == null)
            return;

        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Simulation Analytics");
        dialog.setHeaderText("Historical Data");

        AnalyticsDashboard dashboard = new AnalyticsDashboard(engine.getHistoryManager().getHistory());
        // Refresh initially
        dashboard.refresh();

        dialog.getDialogPane().setContent(dashboard);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);

        // Auto-refresh when open
        javafx.animation.Timeline updater = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> dashboard.refresh()));
        updater.setCycleCount(javafx.animation.Animation.INDEFINITE);
        updater.play();

        dialog.setOnHidden(e -> updater.stop());

        dialog.show();
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
        miniMap.setCells(engine.getCells());
    }
}
