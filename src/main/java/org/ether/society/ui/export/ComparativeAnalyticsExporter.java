package org.ether.society.ui.export;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import org.ether.society.analytics.ComparativeReportGenerator;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.config.EtherPaths;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.ComparativeAnalyticsPanel.ScenarioSelectableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * <h2>Comparative Analytics Exporter</h2>
 * <p>
 * Handles asynchronous file export pipelines for comparative multi-scenario simulations:
 * <ul>
 *   <li><b>Markdown Reports</b>: Formatted scientific audit and comparative reports with timestamped filenames.</li>
 *   <li><b>1D CSV Telemetry</b>: Multi-scenario chronological series with interpolated metric tables.</li>
 *   <li><b>2D Cartography ZIP Archives</b>: Deflated archives containing spatial metric summaries, differential delta maps, and cached tensor raster channels.</li>
 * </ul>
 * </p>
 *
 * @author Ether Architectural Refactoring Engine
 * @since 1.0.0
 * @see org.ether.society.ui.ComparativeAnalyticsPanel
 */
public final class ComparativeAnalyticsExporter {

    private static final Logger logger = LoggerFactory.getLogger(ComparativeAnalyticsExporter.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ComparativeAnalyticsExporter() {
        // Utility class
    }

    /**
     * Exports a Markdown comparative report to the application exports directory.
     *
     * @param markdownReport report text in GitHub Flavored Markdown
     * @param exportMdBtn target button to show transient success feedback
     */
    public static void exportMarkdownReport(String markdownReport, Button exportMdBtn) {
        if (markdownReport == null || markdownReport.isBlank() || markdownReport.startsWith("## Please select")) {
            return;
        }

        String timestamp = TIMESTAMP_FORMATTER.format(LocalDateTime.now());
        String fileName = markdownReport.contains("RAPPORT D'AUDIT") || markdownReport.contains("CLIODYNAMIC AUDIT")
                ? "Report_Cliodynamic_Audit_" + timestamp + ".md"
                : "Report_Comparative_Analysis_" + timestamp + ".md";

        Path targetFile = EtherPaths.getExportsDir().resolve(fileName);
        try (FileWriter writer = new FileWriter(targetFile.toFile(), StandardCharsets.UTF_8)) {
            writer.write(markdownReport);
            logger.info("Exported comparative analysis report to {}", targetFile.toAbsolutePath());

            if (exportMdBtn != null) {
                String origText = exportMdBtn.getText();
                exportMdBtn.setText(String.format(I18n.getOrDefault("analytics.status.export_md_success", "✅ Exporté : %s"), fileName));
                exportMdBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #059669; -fx-text-fill: white; -fx-padding: 6 14;");

                Timeline resetTimer = new Timeline(new KeyFrame(Duration.seconds(3), evt -> {
                    exportMdBtn.setText(origText);
                    exportMdBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
                }));
                resetTimer.setCycleCount(1);
                resetTimer.play();
            }
        } catch (IOException ex) {
            logger.error("Error writing markdown report to file", ex);
        }
    }

    /**
     * Exports selected 1D scenario telemetry data into a chronological CSV table.
     *
     * @param scenarioList full scenario items list
     * @param exportCsvBtn target button for transient visual status feedback
     * @param runResolver resolver function converting a selectable item into its simulated run record
     */
    public static void exportCsvData(List<ScenarioSelectableItem> scenarioList,
                                    Button exportCsvBtn,
                                    Function<ScenarioSelectableItem, SimulationRunRecord> runResolver) {
        if (scenarioList == null) return;
        List<ScenarioSelectableItem> selected = scenarioList.stream()
                .filter(ScenarioSelectableItem::isSelected)
                .toList();

        if (selected.isEmpty()) return;

        List<SimulationRunRecord> runsToExport = new ArrayList<>();
        boolean includeGroundTruth = false;
        int minYear = Integer.MAX_VALUE;
        int maxYear = Integer.MIN_VALUE;

        for (ScenarioSelectableItem it : selected) {
            if ("HISTORICAL_GROUND_TRUTH".equals(it.getRunId())) {
                includeGroundTruth = true;
            } else if (it.isExecuted()) {
                SimulationRunRecord r = runResolver != null ? runResolver.apply(it) : null;
                if (r != null) {
                    runsToExport.add(r);
                    if (it.getScenario() != null) {
                        minYear = Math.min(minYear, (int) it.getScenario().getStartDateYear());
                        maxYear = Math.max(maxYear, (int) it.getScenario().getEndDateYear());
                    }
                }
            }
        }

        if (minYear == Integer.MAX_VALUE) { minYear = -100000; maxYear = 2026; }

        String csv = ComparativeReportGenerator.generateCsvExport(runsToExport, includeGroundTruth, minYear, maxYear);
        String timestamp = TIMESTAMP_FORMATTER.format(LocalDateTime.now());
        String fileName = "Data_Comparative_Analysis_" + timestamp + ".csv";
        Path targetFile = EtherPaths.getExportsDir().resolve(fileName);

        try (FileWriter writer = new FileWriter(targetFile.toFile(), StandardCharsets.UTF_8)) {
            writer.write(csv);
            logger.info("Exported CSV telemetry data to {}", targetFile.toAbsolutePath());

            if (exportCsvBtn != null) {
                String origText = exportCsvBtn.getText();
                exportCsvBtn.setText(String.format(I18n.getOrDefault("analytics.status.export_csv_success", "✅ Exporté : %s"), fileName));
                exportCsvBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #6d28d9; -fx-text-fill: white; -fx-padding: 6 14;");

                Timeline resetTimer = new Timeline(new KeyFrame(Duration.seconds(3), evt -> {
                    exportCsvBtn.setText(origText);
                    exportCsvBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
                }));
                resetTimer.setCycleCount(1);
                resetTimer.play();
            }
        } catch (IOException ex) {
            logger.error("Error writing CSV export to file", ex);
        }
    }

    /**
     * Packages active 2D cartography layers and raster tensor cache into a compressed ZIP file.
     *
     * @param export2DZipBtn export button
     * @param itemA baseline scenario A
     * @param itemB target scenario B
     * @param targetYear simulated calendar year
     * @param channelKey canonical tensor channel name
     * @param spatialReportText spatial metric summary text
     * @param imgA current image for scenario A
     * @param imgB current image for scenario B
     * @param imgDiff current delta difference image
     * @param rasterImageCache global raster image cache
     */
    public static void export2DCartographyZip(Button export2DZipBtn,
                                             ScenarioSelectableItem itemA,
                                             ScenarioSelectableItem itemB,
                                             int targetYear,
                                             String channelKey,
                                             String spatialReportText,
                                             Image imgA,
                                             Image imgB,
                                             Image imgDiff,
                                             Map<String, BufferedImage> rasterImageCache) {
        if (itemA == null) return;
        final ScenarioSelectableItem finalA = itemA;
        final ScenarioSelectableItem finalB = itemB != null ? itemB : itemA;

        String timestamp = TIMESTAMP_FORMATTER.format(LocalDateTime.now());
        String zipName = "Cartography_Tensors_Export_" + timestamp + ".zip";

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.getOrDefault("analytics.filechooser.export_tensors", "Save 2D Cartography Tensors ZIP"));
        fileChooser.setInitialFileName(zipName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Archives (*.zip)", "*.zip"));
        try {
            File defaultDir = EtherPaths.getExportsDir().toFile();
            if (defaultDir.exists()) fileChooser.setInitialDirectory(defaultDir);
        } catch (Exception ignored) {}

        Window window = (export2DZipBtn != null && export2DZipBtn.getScene() != null) ? export2DZipBtn.getScene().getWindow() : null;
        File selectedFile = (window != null) ? fileChooser.showSaveDialog(window) : EtherPaths.getExportsDir().resolve(zipName).toFile();
        if (selectedFile == null) return;
        final Path targetZip = selectedFile.toPath();
        final String savedFileName = selectedFile.getName();

        if (export2DZipBtn != null) {
            export2DZipBtn.setDisable(true);
            export2DZipBtn.setText("⏳ Compression ZIP...");
        }

        CompletableFuture.runAsync(() -> {
            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(targetZip)))) {
                zos.setLevel(Deflater.BEST_SPEED);

                // 1. Add spatial report text file
                zos.putNextEntry(new ZipEntry("spatial_metrics_report.txt"));
                zos.write((spatialReportText != null ? spatialReportText : "").getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                // 2. Export currently displayed maps
                if (imgA != null) {
                    addFxImageToZip(zos, imgA, "Map_A_" + sanitizeFileName(finalA.getName()) + "_" + targetYear + "_" + channelKey + ".png");
                }
                if (imgB != null) {
                    addFxImageToZip(zos, imgB, "Map_B_" + sanitizeFileName(finalB.getName()) + "_" + targetYear + "_" + channelKey + ".png");
                }
                if (imgDiff != null) {
                    addFxImageToZip(zos, imgDiff, "Delta_A_minus_B_" + targetYear + "_" + channelKey + ".png");
                }

                // 3. Export all available cached channels
                if (rasterImageCache != null) {
                    for (Map.Entry<String, BufferedImage> entry : rasterImageCache.entrySet()) {
                        String key = entry.getKey();
                        if (key == null || key.startsWith("FILE_") || key.contains("null")) continue;
                        BufferedImage img = entry.getValue();
                        if (img != null) {
                            addBufferedImageToZip(zos, img, "tensors_cache/" + sanitizeFileName(key) + ".png");
                        }
                    }
                }

                if (export2DZipBtn != null) {
                    Platform.runLater(() -> {
                        export2DZipBtn.setDisable(false);
                        export2DZipBtn.setText("✅ Exporté : " + savedFileName);
                        export2DZipBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #059669; -fx-text-fill: white; -fx-padding: 6 14;");
                        Timeline t = new Timeline(new KeyFrame(Duration.seconds(4), evt -> {
                            export2DZipBtn.setText(I18n.getOrDefault("analytics.btn.export_2d_zip", "📦 Export 2D Tensors (.zip)"));
                            export2DZipBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #6366f1; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
                        }));
                        t.setCycleCount(1);
                        t.play();
                    });
                }
            } catch (Exception ex) {
                logger.error("Failed to export 2D cartography ZIP", ex);
                if (export2DZipBtn != null) {
                    Platform.runLater(() -> {
                        export2DZipBtn.setDisable(false);
                        export2DZipBtn.setText(I18n.getOrDefault("analytics.btn.export_2d_zip", "📦 Export 2D Tensors (.zip)"));
                    });
                }
            }
        });
    }

    /**
     * Converts a JavaFX Image to a buffered image and writes it into a ZIP archive entry.
     *
     * @param zos output zip stream
     * @param fxImage source JavaFX Image
     * @param entryName zip entry file path
     * @throws IOException if image encoding or streaming fails
     */
    public static void addFxImageToZip(ZipOutputStream zos, Image fxImage, String entryName) throws IOException {
        if (fxImage == null) return;
        BufferedImage bImg = javafx.embed.swing.SwingFXUtils.fromFXImage(fxImage, null);
        addBufferedImageToZip(zos, bImg, entryName);
    }

    /**
     * Encodes a BufferedImage as PNG into a ZIP entry.
     *
     * @param zos output zip stream
     * @param bImg source BufferedImage
     * @param entryName zip entry path
     * @throws IOException on write failure
     */
    public static void addBufferedImageToZip(ZipOutputStream zos, BufferedImage bImg, String entryName) throws IOException {
        if (bImg == null) return;
        zos.putNextEntry(new ZipEntry(entryName));
        ImageIO.write(bImg, "PNG", zos);
        zos.closeEntry();
    }

    /**
     * Sanitizes file names by replacing non-alphanumeric characters with underscores.
     *
     * @param name input file name
     * @return clean safe filename string
     */
    public static String sanitizeFileName(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }
}
