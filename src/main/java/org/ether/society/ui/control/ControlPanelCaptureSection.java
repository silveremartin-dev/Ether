/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.control;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.ISimulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.H3MapCanvas;
import org.ether.society.ui.NotificationOverlay;
import org.ether.society.ui.export.VideoExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Capture and telemetry card for {@link org.ether.society.ui.ControlPanel}.
 *
 * <p>Extracted from {@code ControlPanel} to satisfy the Single Responsibility Principle (SRP).
 * Provides high-definition raster snapshot export (PNG) and background MP4 video timelapse generation.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ControlPanelCaptureSection {

    private static final Logger logger = LoggerFactory.getLogger(ControlPanelCaptureSection.class);

    private final VBox card;
    private final Label exportTitle;
    private final Button hdScreenshotBtn;
    private final Button recordVideoBtn;
    private final CheckBox autoRecordCheck;
    private final VideoExportService videoExportService = new VideoExportService();

    private boolean isRecordingVideo = false;

    /**
     * Initializes the telemetry and capture controls card.
     *
     * @param onScreenshot callback for screenshot trigger
     * @param onVideoToggle callback for video recording toggle
     */
    public ControlPanelCaptureSection(Runnable onScreenshot, Runnable onVideoToggle) {
        exportTitle = createCardTitle(I18n.getOrDefault("sim.card.telemetry", "📊 4. TELEMETRY, SCREENSHOTS & EXPORTS"));

        hdScreenshotBtn = new Button("📸 " + I18n.getOrDefault("sim.btn.screenshot", "HD Screenshot"));
        hdScreenshotBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.screenshot", "Exports HD PNG screenshot to saves/screenshots/")));
        hdScreenshotBtn.setMaxWidth(Double.MAX_VALUE);
        hdScreenshotBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 10; -fx-background-radius: 6;");
        hdScreenshotBtn.setOnAction(e -> {
            if (onScreenshot != null) onScreenshot.run();
        });

        recordVideoBtn = new Button("🎥 " + I18n.getOrDefault("sim.btn.video", "Record MP4 Video"));
        recordVideoBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.video", "Starts MP4 video capture (1:1 step) in saves/timelapse/")));
        recordVideoBtn.setMaxWidth(Double.MAX_VALUE);
        recordVideoBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 10; -fx-background-radius: 6;");
        recordVideoBtn.setOnAction(e -> {
            if (onVideoToggle != null) onVideoToggle.run();
        });

        autoRecordCheck = new CheckBox(I18n.getOrDefault("sim.option.auto_record", "🎬 Auto Sync Video (Start & Pause)"));
        autoRecordCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.auto_record", "Automatically starts/stops 1:1 video recording in sync with scenario start/pause")));
        autoRecordCheck.getStyleClass().add("opt-sub-checkbox");
        autoRecordCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");

        card = new VBox(8, exportTitle, hdScreenshotBtn, recordVideoBtn, autoRecordCheck);
        card.getStyleClass().add("card-section");
    }

    /**
     * Gets the JavaFX card container node for Section 4.
     *
     * @return capture card VBox
     */
    public VBox getCard() {
        return card;
    }

    /**
     * Checks if auto video sync with scenario state is selected.
     *
     * @return true if auto record is active
     */
    public boolean isAutoRecordSelected() {
        return autoRecordCheck != null && autoRecordCheck.isSelected();
    }

    /**
     * Checks if video recording is currently in progress.
     *
     * @return true if recording video
     */
    public boolean isRecordingVideo() {
        return isRecordingVideo;
    }

    /**
     * Sets the internal video recording flag.
     *
     * @param recording true if recording
     */
    public void setRecordingVideo(boolean recording) {
        this.isRecordingVideo = recording;
    }

    /**
     * Takes an HD raster screenshot of the current map canvas view.
     *
     * @param mapCanvas active H3 map canvas
     * @param notificationOverlay optional notification overlay
     */
    public void takeHDScreenshot(H3MapCanvas mapCanvas, NotificationOverlay notificationOverlay) {
        if (mapCanvas == null) {
            logger.warn("Cannot capture screenshot: mapCanvas is null");
            return;
        }
        try {
            WritableImage writableImage = mapCanvas.snapshot(new SnapshotParameters(), null);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);

            File dir = new File("saves/screenshots");
            if (!dir.exists()) dir.mkdirs();

            String filename = "Ether_Capture_HD_" + System.currentTimeMillis() + ".png";
            File outputFile = new File(dir, filename);
            ImageIO.write(bufferedImage, "png", outputFile);

            logger.info("Saved HD screenshot to {}", outputFile.getAbsolutePath());
            if (notificationOverlay != null) {
                notificationOverlay.showNotification("📸 Capture Photo HD Sauvegardée !\n" + outputFile.getName(), "#38bdf8");
            }
        } catch (Exception ex) {
            logger.error("Failed to take HD screenshot", ex);
            if (notificationOverlay != null) {
                notificationOverlay.showNotification("❌ Erreur de Capture Photo HD", "#ef4444");
            }
        }
    }

    /**
     * Toggles asynchronous video recording and MP4 pipeline export.
     *
     * @param engine simulation engine
     * @param mapCanvas active H3 map canvas
     * @param notificationOverlay optional notification overlay
     * @param onTimelapseRecord optional timelapse recording callback
     */
    public void toggleVideoRecording(ISimulationEngine engine, H3MapCanvas mapCanvas,
                                     NotificationOverlay notificationOverlay, Runnable onTimelapseRecord) {
        if (onTimelapseRecord != null) {
            onTimelapseRecord.run();
        }

        if (engine instanceof H3SimulationEngine h3Engine) {
            if (notificationOverlay != null) {
                notificationOverlay.showNotification("🎬 Lancement de l'Export Vidéo MP4 (Arrière-plan)...", "#38bdf8");
            }

            String scenarioName = (mapCanvas != null && mapCanvas.getScenarioName() != null) ? mapCanvas.getScenarioName() : "Simulation";
            videoExportService.exportSimulationVideoAsync(h3Engine, scenarioName)
                    .thenAccept(outputDir -> {
                        javafx.application.Platform.runLater(() -> {
                            if (notificationOverlay != null) {
                                notificationOverlay.showNotification("✅ Export Vidéo Réussi !\nFichiers dans saves/exports/", "#10b981");
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        javafx.application.Platform.runLater(() -> {
                            if (notificationOverlay != null) {
                                notificationOverlay.showNotification("❌ Erreur Export Vidéo: " + ex.getMessage(), "#ef4444");
                            }
                        });
                        return null;
                    });
        }
    }

    /**
     * Updates all localized UI texts across the capture card.
     */
    public void updateTexts() {
        if (exportTitle != null) {
            exportTitle.setText(I18n.getOrDefault("sim.card.telemetry", "📊 4. TELEMETRY, SCREENSHOTS & EXPORTS"));
        }
        if (hdScreenshotBtn != null) {
            hdScreenshotBtn.setText("📸 " + I18n.getOrDefault("sim.btn.screenshot", "HD Screenshot"));
            hdScreenshotBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.screenshot", "Exports HD PNG screenshot to saves/screenshots/")));
        }
        if (recordVideoBtn != null) {
            recordVideoBtn.setText("🎥 " + I18n.getOrDefault("sim.btn.video", "Record MP4 Video"));
            recordVideoBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.video", "Starts MP4 video capture (1:1 step) in saves/timelapse/")));
        }
        if (autoRecordCheck != null) {
            autoRecordCheck.setText(I18n.getOrDefault("sim.option.auto_record", "🎬 Auto Sync Video (Start & Pause)"));
            autoRecordCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.auto_record", "Automatically starts/stops 1:1 video recording in sync with scenario start/pause")));
        }
    }

    private static Label createCardTitle(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        return label;
    }
}
