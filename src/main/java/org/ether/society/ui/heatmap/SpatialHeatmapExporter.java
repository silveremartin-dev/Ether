/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.heatmap;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.analytics.HistoryManager;
import org.ether.society.ui.export.AtlasVideoExporter;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.DisplayMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <h2>Spatial Heatmap Exporter</h2>
 * <p>
 * Handles asynchronous encoding and export of dynamic temporal cartography (GIF / Video)
 * from historical simulation snapshots.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public final class SpatialHeatmapExporter {

    private static final Logger logger = LoggerFactory.getLogger(SpatialHeatmapExporter.class);

    private SpatialHeatmapExporter() {
        // Utility class
    }

    /**
     * Launches interactive file dialog and background video export of historical simulation frames.
     *
     * @param window owner window
     * @param historyManager simulation history manager
     * @param activeLayers active display modes
     * @param startIdx start snapshot index
     * @param endIdx end snapshot index
     * @param playbackDelayMs delay between frames in milliseconds
     * @param onStart export start callback (e.g. disable UI buttons)
     * @param onComplete export completion callback (e.g. re-enable UI buttons)
     */
    public static void exportVideo(
            Window window,
            HistoryManager historyManager,
            Set<DisplayMode> activeLayers,
            int startIdx,
            int endIdx,
            int playbackDelayMs,
            Runnable onStart,
            Runnable onComplete) {

        if (historyManager == null || historyManager.getWorldSnapshots().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(I18n.getOrDefault("heatmap.export.warn_title", "Aucune Donnée Historique"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.getOrDefault("heatmap.export.no_snapshots", "Aucun instantané n'est disponible pour l'exportation vidéo. Lancez la simulation d'abord."));
            alert.showAndWait();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("heatmap.export.dialog_title", "Exporter l'Atlas Dynamique en Vidéo / GIF Animé"));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GIF Vidéo Animée (*.gif)", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les Fichiers (*.*)", "*.*")
        );
        chooser.setInitialFileName("Atlas_Territoires_" + System.currentTimeMillis() + ".gif");
        File targetFile = chooser.showSaveDialog(window);
        if (targetFile == null) return;

        if (onStart != null) onStart.run();

        AtlasVideoExporter.exportVideoAsync(
                historyManager,
                activeLayers,
                startIdx,
                endIdx,
                playbackDelayMs,
                targetFile,
                "Atlas",
                progress -> {}
        ).thenAccept(outFile -> Platform.runLater(() -> {
            if (onComplete != null) onComplete.run();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(I18n.getOrDefault("heatmap.export.success_title", "Export Vidéo Réussi"));
            alert.setHeaderText(I18n.getOrDefault("heatmap.export.success_header", "Atlas Vidéo Exporté avec Succès"));
            alert.setContentText(String.format(Locale.ROOT, I18n.getOrDefault("heatmap.export.success_body", "L'animation cartographique a été générée dans :\n%s\n(%d instantanés encodés)"),
                    outFile.getAbsolutePath(), (endIdx - startIdx + 1)));

            ButtonType openFileBtn = new ButtonType(I18n.getOrDefault("heatmap.export.open_file", "Ouvrir le Fichier"), ButtonBar.ButtonData.YES);
            ButtonType closeBtn = new ButtonType(I18n.getOrDefault("heatmap.export.close", "Fermer"), ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(openFileBtn, closeBtn);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == openFileBtn) {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(outFile);
                    }
                } catch (Exception ex) {
                    logger.warn("Could not open exported video file directly: {}", ex.getMessage());
                }
            }
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (onComplete != null) onComplete.run();

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18n.getOrDefault("heatmap.export.error_title", "Erreur d'Exportation"));
                alert.setHeaderText(null);
                alert.setContentText(I18n.getOrDefault("heatmap.export.error_body", "Impossible d'exporter l'atlas vidéo : ") + ex.getMessage());
                alert.showAndWait();
            });
            return null;
        });
    }
}
