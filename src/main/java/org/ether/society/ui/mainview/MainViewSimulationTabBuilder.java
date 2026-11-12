/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.mainview;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.ColorLegend;
import org.ether.society.ui.ControlPanel;
import org.ether.society.ui.ExecutionContextPanel;
import org.ether.society.ui.GodModePanel;
import org.ether.society.ui.H3MapCanvas;
import org.ether.society.ui.HeadlessDashboardOverlay;
import org.ether.society.ui.NotificationOverlay;
import org.ether.society.ui.ScenarioSetupPanel;
import org.ether.society.ui.SimulationTimelineCoordinator;
import org.ether.society.ui.StatsPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constructs the interactive Simulation Tab layout (Tab 4) for {@link org.ether.society.ui.MainView}.
 *
 * <p>Assembles the layered cartographic viewport ({@link H3MapCanvas}, {@link HeadlessDashboardOverlay},
 * {@link NotificationOverlay}, calculation progress overlay, and {@link ColorLegend}) alongside the
 * three-tab left sidebar ({@link ControlPanel}, {@link StatsPanel}, and {@link GodModePanel}).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class MainViewSimulationTabBuilder {

    /** Diagnostic logger for simulation tab assembly events. */
    private static final Logger logger = LoggerFactory.getLogger(MainViewSimulationTabBuilder.class);

    /**
     * Private constructor preventing instantiation of utility builder class.
     */
    private MainViewSimulationTabBuilder() {
        // Utility builder class
    }

    /**
     * Encapsulates the assembled JavaFX scene graph nodes produced for the Simulation Tab.
     *
     * @param simulationRoot root {@link BorderPane} hosting sidebar and map stack
     * @param mapStack layered {@link StackPane} hosting map canvas and HUD overlays
     * @param headlessDashboard telemetry overlay shown during headless execution
     * @param notificationOverlay floating toast notification container
     * @param colorLegend GIS statistical color legend overlay
     * @param controlTab sidebar tab hosting 2D/3D render and playback controls
     * @param statsTab sidebar tab hosting live cliodynamic statistics
     * @param godModeTab sidebar tab hosting physical perturbation injectors
     */
    public record SimulationTabComponents(
            BorderPane simulationRoot,
            StackPane mapStack,
            HeadlessDashboardOverlay headlessDashboard,
            NotificationOverlay notificationOverlay,
            ColorLegend colorLegend,
            Tab controlTab,
            Tab statsTab,
            Tab godModeTab
    ) {}

    /**
     * Builds and wires the complete Simulation Tab view hierarchy.
     *
     * @param engine active H3 simulation engine
     * @param controlPanel interactive control panel
     * @param mapCanvas cartographic H3 canvas
     * @param statsPanel live statistics dashboard panel
     * @param godModePanel God Mode &amp; timeline audit panel
     * @param setupPanel scenario setup panel receiving deferred progress bar bindings
     * @param executionContextPanel execution context panel providing headless target horizon
     * @param timelineCoordinator timeline playback and timelapse coordinator
     * @param onSave action callback triggered when saving simulation state
     * @param onLoad action callback triggered when loading simulation state
     * @param onFullScreen action callback triggered when toggling full-screen mode
     * @return populated {@link SimulationTabComponents} record
     */
    public static SimulationTabComponents buildSimulationView(
            H3SimulationEngine engine,
            ControlPanel controlPanel,
            H3MapCanvas mapCanvas,
            StatsPanel statsPanel,
            GodModePanel godModePanel,
            ScenarioSetupPanel setupPanel,
            ExecutionContextPanel executionContextPanel,
            SimulationTimelineCoordinator timelineCoordinator,
            Runnable onSave,
            Runnable onLoad,
            Runnable onFullScreen) {

        BorderPane root = new BorderPane();
        root.getStyleClass().add("glass-panel");

        StackPane mapStack = new StackPane();

        // 1. Direct Map Canvas
        mapCanvas.widthProperty().bind(mapStack.widthProperty());
        mapCanvas.heightProperty().bind(mapStack.heightProperty());
        mapStack.getChildren().add(mapCanvas);

        // 1.b Headless Dashboard
        HeadlessDashboardOverlay headlessDashboard = new HeadlessDashboardOverlay(mode -> {
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
        NotificationOverlay notificationOverlay = new NotificationOverlay();
        notificationOverlay.setPickOnBounds(false);
        notificationOverlay.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(notificationOverlay, Pos.BOTTOM_LEFT);
        mapStack.getChildren().add(notificationOverlay);

        if (controlPanel != null) {
            controlPanel.setNotificationOverlay(notificationOverlay);
        }

        // 3. Deferred Calculation Progress Overlay for Tab 4
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

        // 4. Color Legend Component
        ColorLegend colorLegend = new ColorLegend();
        colorLegend.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        colorLegend.updateFromCanvas(mapCanvas);
        if (controlPanel != null) {
            controlPanel.setColorLegend(colorLegend);
            controlPanel.setMapCanvas(mapCanvas);
        }
        StackPane.setAlignment(colorLegend, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(colorLegend, new Insets(0, 16, 40, 0));
        mapStack.getChildren().add(colorLegend);

        mapCanvas.setTooltipContainer(mapStack);

        // Connect Control Panel callbacks to timeline coordinator
        if (controlPanel != null && timelineCoordinator != null) {
            controlPanel.setOnSave(onSave);
            controlPanel.setOnLoad(onLoad);
            controlPanel.setOnContourToggle(mapCanvas::toggleContours);
            controlPanel.setOnTimelapseRecord(timelineCoordinator::toggleTimelapseRecording);
            controlPanel.setOnTimelapseSeek(timelineCoordinator::seekTimelapse);
            controlPanel.setOnTimelapseSeekToEnd(timelineCoordinator::seekToEndSafe);
            controlPanel.setOnRewindStart(timelineCoordinator::seekToStartSafe);
            controlPanel.setOnFastRewind(() -> timelineCoordinator.stepBackwardSafe(true));
            controlPanel.setOnStepBack(() -> timelineCoordinator.stepBackwardSafe(false));
            controlPanel.setOnStepForward(() -> timelineCoordinator.stepForwardSafe(false));
            controlPanel.setOnFastForward(() -> timelineCoordinator.stepForwardSafe(true));
            controlPanel.setOnSeekToEnd(timelineCoordinator::seekToEndSafe);
            controlPanel.setOnFullScreen(onFullScreen);
        }

        if (godModePanel != null) {
            godModePanel.setMapCanvas(mapCanvas);
            godModePanel.setNotificationOverlay(notificationOverlay);
        }

        TabPane leftSidebar = new TabPane();
        leftSidebar.setPrefWidth(480);
        leftSidebar.setMinWidth(480);
        leftSidebar.setStyle("-fx-background-color: transparent;");
        leftSidebar.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        ScrollPane controlScroll = createTransparentScrollPane(controlPanel);
        ScrollPane statsScroll = createTransparentScrollPane(statsPanel);
        ScrollPane godScroll = createTransparentScrollPane(godModePanel);

        Tab controlTab = new Tab(I18n.getOrDefault("sim.tab.controls", "🎛️ 3D Render & Controls"), controlScroll);
        Tab statsTab = new Tab(I18n.getOrDefault("sim.tab.stats", "📊 Stats"), statsScroll);
        Tab godModeTab = new Tab(I18n.getOrDefault("sim.tab.godmode", "⚡ Mode Dieu"), godScroll);
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

        return new SimulationTabComponents(
                root, mapStack, headlessDashboard, notificationOverlay, colorLegend,
                controlTab, statsTab, godModeTab
        );
    }

    /**
     * Wraps a sidebar panel in a vertically scrollable, transparent {@link ScrollPane}.
     *
     * @param content child node to embed inside the scroll viewport
     * @return configured {@link ScrollPane}
     */
    private static ScrollPane createTransparentScrollPane(javafx.scene.Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }
}
