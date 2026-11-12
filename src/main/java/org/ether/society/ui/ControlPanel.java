/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import org.ether.society.core.ISimulationEngine;
import org.ether.society.events.ActiveEvent;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.control.ControlPanelCaptureSection;
import org.ether.society.ui.control.ControlPanelEventsSection;
import org.ether.society.ui.control.ControlPanelLayersSection;
import org.ether.society.ui.control.ControlPanelPlaybackSection;
import org.ether.society.ui.control.ControlPanelRenderingSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern simulation control and rendering panel for Tab 4 (Simulation View).
 *
 * <p>Refactored according to the Single Responsibility Principle (SRP). Acts as the primary
 * coordinator delegating modular cards to:
 * <ul>
 *   <li>{@link ControlPanelPlaybackSection} for simulation clock playback, temporal resolution scaling, and DB/cluster status</li>
 *   <li>{@link ControlPanelLayersSection} for cartographic layer stack multi-selection and 1-click scientific presets</li>
 *   <li>{@link ControlPanelRenderingSection} for 3D spherical globe projections, relief exaggeration, and uniform palettes</li>
 *   <li>{@link ControlPanelCaptureSection} for HD screenshots and MP4 video timelapse generation</li>
 *   <li>{@link ControlPanelEventsSection} for chronological planetary event feeds and click-to-ping navigation</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class ControlPanel extends VBox {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(ControlPanel.class);

    private final ISimulationEngine engine;
    private NotificationOverlay notificationOverlay;

    // References to UI components
    private H3MapCanvas mapCanvas;
    @SuppressWarnings("unused")
    private MiniMap miniMap;
    private ColorLegend colorLegend;
    private final List<String> eventHistory = new ArrayList<>();

    // Sub-Sections
    private final ControlPanelPlaybackSection playbackSection;
    private final ControlPanelLayersSection layersSection;
    private final ControlPanelRenderingSection renderingSection;
    private final ControlPanelCaptureSection captureSection;
    private final ControlPanelEventsSection eventsSection;

    // Callbacks
    @SuppressWarnings("unused")
    private Runnable onSave;
    @SuppressWarnings("unused")
    private Runnable onLoad;
    private Runnable onTimelapseRecord;
    @SuppressWarnings("unused")
    private Consumer<Boolean> onStatsToggle;

    /**
     * Constructs the simulation control panel.
     *
     * @param engine active simulation engine instance
     */
    public ControlPanel(ISimulationEngine engine) {
        this.engine = engine;

        setSpacing(10);
        setPadding(new Insets(12));
        getStyleClass().add("glass-panel");

        playbackSection = new ControlPanelPlaybackSection(engine);

        layersSection = new ControlPanelLayersSection();
        layersSection.setMapCanvasSupplier(() -> this.mapCanvas);
        layersSection.setColorLegendSupplier(() -> this.colorLegend);
        layersSection.setEngineSupplier(() -> this.engine);
        layersSection.setPauseOnEventCheckSupplier(playbackSection::getPauseOnEventCheck);

        renderingSection = new ControlPanelRenderingSection();
        renderingSection.setMapCanvasSupplier(() -> this.mapCanvas);
        renderingSection.setColorLegendSupplier(() -> this.colorLegend);
        layersSection.setRenderingSectionSupplier(() -> this.renderingSection);

        captureSection = new ControlPanelCaptureSection(this::takeHDScreenshot, this::toggleVideoRecording);
        playbackSection.bindCaptureControls(captureSection, this::toggleVideoRecording);

        eventsSection = new ControlPanelEventsSection();
        eventsSection.setOnEventSelected(evt -> {
            if (mapCanvas != null) {
                mapCanvas.flyTo(evt.getLatitude(), evt.getLongitude());
                mapCanvas.pingLocation(evt.getLatitude(), evt.getLongitude(), evt.getTitle(), evt.getType(), evt.getMagnitude());
            }
        });

        getChildren().addAll(
                playbackSection.getCard(),
                layersSection.getCard(),
                renderingSection.getCard(),
                captureSection.getCard(),
                eventsSection.getCard()
        );

        updateTexts();
        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    /**
     * Updates play/pause button styling and tooltips.
     *
     * @param isRunning true if engine is running
     */
    public void updatePlayPauseVisuals(boolean isRunning) {
        if (playbackSection != null) {
            playbackSection.updatePlayPauseVisuals(isRunning);
        }
    }

    /**
     * Sets notification overlay.
     *
     * @param overlay notification overlay
     */
    public void setNotificationOverlay(NotificationOverlay overlay) {
        this.notificationOverlay = overlay;
    }

    /**
     * Takes HD screenshot of active map canvas.
     */
    public void takeHDScreenshot() {
        if (captureSection != null) {
            captureSection.takeHDScreenshot(mapCanvas, notificationOverlay);
        }
    }

    /**
     * Toggles video recording.
     */
    public void toggleVideoRecording() {
        if (captureSection != null) {
            captureSection.toggleVideoRecording(engine, mapCanvas, notificationOverlay, onTimelapseRecord);
        }
    }

    public void setOnSave(Runnable onSave) { this.onSave = onSave; }
    public void setOnLoad(Runnable onLoad) { this.onLoad = onLoad; }
    public void setOnContourToggle(Consumer<Boolean> onContourToggle) {
        if (layersSection != null) layersSection.setOnContourToggle(onContourToggle);
    }
    public void setOnTimelapseRecord(Runnable onTimelapseRecord) { this.onTimelapseRecord = onTimelapseRecord; }
    public void setOnTimelapseSeek(Consumer<Integer> onTimelapseSeek) {
        if (playbackSection != null) playbackSection.setOnTimelapseSeek(onTimelapseSeek);
    }
    public void setOnTimelapseSeekToEnd(Runnable onTimelapseSeekToEnd) {
        if (playbackSection != null) playbackSection.setOnTimelapseSeekToEnd(onTimelapseSeekToEnd);
    }
    public void setOnRewindStart(Runnable onRewindStart) {
        if (playbackSection != null) playbackSection.setOnRewindStart(onRewindStart);
    }
    public void setOnFastRewind(Runnable onFastRewind) {
        if (playbackSection != null) playbackSection.setOnFastRewind(onFastRewind);
    }
    public void setOnStepBack(Runnable onStepBack) {
        if (playbackSection != null) playbackSection.setOnStepBack(onStepBack);
    }
    public void setOnStepForward(Runnable onStepForward) {
        if (playbackSection != null) playbackSection.setOnStepForward(onStepForward);
    }
    public void setOnFastForward(Runnable onFastForward) {
        if (playbackSection != null) playbackSection.setOnFastForward(onFastForward);
    }
    public void setOnSeekToEnd(Runnable onSeekToEnd) {
        if (playbackSection != null) playbackSection.setOnSeekToEnd(onSeekToEnd);
    }
    public void setOnStatsToggle(Consumer<Boolean> onStatsToggle) { this.onStatsToggle = onStatsToggle; }
    public void setOnFullScreen(Runnable onFullScreen) {
        if (renderingSection != null) renderingSection.setOnFullScreen(onFullScreen);
    }

    /**
     * Updates date header for timelapse slider.
     *
     * @param minYear minimum year
     * @param maxYear maximum year
     * @param currentYear current simulation year
     */
    public void updateTimelapseSlider(int minYear, int maxYear, int currentYear) {
        if (playbackSection != null) {
            playbackSection.updateTimelapseSlider(currentYear);
        }
    }

    /**
     * Sets active H3 map canvas.
     *
     * @param canvas map canvas
     */
    public void setMapCanvas(H3MapCanvas canvas) {
        this.mapCanvas = canvas;
        updateViewToggleButton();
        updateDisplayToggleButton();
    }

    public void setMiniMap(MiniMap miniMap) { this.miniMap = miniMap; }

    public void setColorLegend(ColorLegend legend) {
        this.colorLegend = legend;
    }

    public void updateScenarioName(String scenarioName) {
        if (playbackSection != null) {
            playbackSection.updateScenarioName(scenarioName);
        }
    }

    public void updateYear(String year) {
        if (playbackSection != null) {
            playbackSection.updateYear(year);
        }
    }

    public void updateStats(long population, double food, long populatedCells, double tps) {
        if (playbackSection != null) {
            playbackSection.updateStats(population, food, populatedCells);
        }
    }

    public void updateSeason(int month) {
        if (playbackSection != null) {
            playbackSection.updateSeason(month);
        }
    }

    public void logEvents(List<String> events) {
        if (events == null || events.isEmpty()) return;
        eventHistory.addAll(events);
    }

    public void updateRecentEvents(List<ActiveEvent> events) {
        if (eventsSection != null) {
            eventsSection.updateRecentEvents(events);
        }
    }

    public void updateAge(String ageName) {
        if (playbackSection != null) {
            playbackSection.updateAge(ageName);
        }
    }

    public void updateDatabaseStatus(boolean online) {
        if (playbackSection != null) {
            playbackSection.updateDatabaseStatus(online);
        }
    }

    public void rebuildLayerMenuItems() {
        if (layersSection != null) {
            layersSection.rebuildLayerMenuItems();
        }
    }

    public void updateActiveLayerUI() {
        if (layersSection != null) {
            layersSection.updateActiveLayerUI();
        }
    }

    public void updateClusterStatus(String nodeRole, String hostPort, boolean isConnected) {
        if (playbackSection != null) {
            playbackSection.updateClusterStatus(nodeRole, hostPort, isConnected);
        }
    }

    private void updateTexts() {
        if (playbackSection != null) playbackSection.updateTexts();
        if (layersSection != null) layersSection.updateTexts();
        if (renderingSection != null) renderingSection.updateTexts();
        if (captureSection != null) captureSection.updateTexts();
        if (eventsSection != null) eventsSection.updateTexts();

        updateViewToggleButton();
        updateDisplayToggleButton();
    }

    private void updateViewToggleButton() {
        if (renderingSection != null && mapCanvas != null) {
            renderingSection.updateViewToggleButton(mapCanvas);
        }
    }

    private void updateDisplayToggleButton() {
        updateActiveLayerUI();
    }
}
