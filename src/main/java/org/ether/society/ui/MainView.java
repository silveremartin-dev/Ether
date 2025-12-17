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

    // UI Structure
    private TabPane tabPane;
    private Tab setupTab;
    private Tab simulationTab;
    private ScenarioSetupPanel setupPanel;
    private NotificationOverlay notificationOverlay;

    public MainView(H3SimulationEngine engine, ControlPanel controlPanel, H3MapCanvas mapCanvas, MiniMap miniMap,
            PerformanceHUD hud) {
        this.engine = engine;
        this.controlPanel = controlPanel;
        this.mapCanvas = mapCanvas;
        this.miniMap = miniMap;
        this.hud = hud;

        initUI();
    }

    private void initUI() {
        tabPane = new TabPane();
        tabPane.setStyle("-fx-tab-min-height: 40px; -fx-tab-max-height: 40px;");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // 1. Setup Tab
        setupPanel = new ScenarioSetupPanel(this::onStartSimulation);
        setupTab = new Tab("SCENARIO & WORLD");
        setupTab.setContent(setupPanel);
        setupTab.setClosable(false);

        // 2. Simulation Tab
        simulationTab = new Tab("SIMULATION");
        simulationTab.setContent(createSimulationView());
        simulationTab.setDisable(true); // Disabled until started

        tabPane.getTabs().addAll(setupTab, simulationTab);

        // Auto-pause when leaving simulation tab
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (oldTab == simulationTab && newTab != simulationTab) {
                logger.info("Auto-pausing simulation due to tab switch");
                engine.pause();
            }
        });

        getChildren().add(tabPane);
    }

    private BorderPane createSimulationView() {
        BorderPane root = new BorderPane();

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
        if (engine.getDensityEngine() != null) {
            mapCanvas.setEngines(
                    engine.getDensityEngine().getFluxEngine(),
                    engine.getDensityEngine().getCultureEngine(),
                    engine.getAgentManager());
        }

        // Connect Control Panel callbacks
        controlPanel.setOnAnalytics(this::showAnalytics);

        root.setCenter(mapStack);
        root.setBottom(controlPanel);

        return root;
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
                        String currentAge = engine.getDensityEngine().getMaxAchievedAge().getDisplayName();
                        controlPanel.updateAge(currentAge);
                    }

                    lastUpdate = now;
                }
            }
        };
        eventLoop.start();
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
}
