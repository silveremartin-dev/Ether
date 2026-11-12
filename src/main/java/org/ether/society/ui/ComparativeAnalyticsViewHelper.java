/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.ether.society.analytics.HistoricalValidationKernel;
import org.ether.society.analytics.MetricDescriptor;
import org.ether.society.analytics.MetricRegistry;
import org.ether.society.i18n.I18n;
import org.ether.society.i18n.Language;
import org.ether.society.ui.analytics.ComparativeTimeSeriesChartCoordinator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Package-private UI helper for {@link ComparativeAnalyticsPanel}.
 *
 * <p>Configures interactive chronological chart axes, mouse pan/zoom handlers,
 * clipboard export feedback, execution context badges, and 5-language localization updates.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see ComparativeAnalyticsPanel
 */
final class ComparativeAnalyticsViewHelper {

    private ComparativeAnalyticsViewHelper() {
        // Stateless package-private UI helper
    }

    /**
     * Builds and configures the interactive chronological {@link LineChart} with pan/zoom gestures.
     *
     * @param p host comparative analytics panel
     */
    static void configureInteractiveChart(ComparativeAnalyticsPanel p) {
        p.xAxis = new NumberAxis();
        p.xAxis.setMinorTickVisible(false);
        p.xAxis.setAutoRanging(false);
        p.xAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                if (object == null) return "";
                long y = Math.round(object.doubleValue());
                long absY = Math.abs(y);
                boolean isFr = I18n.getCurrentLanguage() == Language.FRENCH;
                if (y < 0) return isFr ? String.format("%,d av. J.-C.", absY) : String.format("%,d BC", absY);
                if (y > 0) return isFr ? String.format("%,d ap. J.-C.", y) : String.format("%,d AD", y);
                return isFr ? "An 0" : "0 AD";
            }

            @Override
            public Number fromString(String string) {
                return null;
            }
        });

        p.yAxis = new NumberAxis();
        p.yAxis.setMinorTickVisible(false);
        p.yAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                if (object == null) return "";
                double val = object.doubleValue();
                double absVal = Math.abs(val);
                if (absVal >= 1e12) return String.format("%.1fT", val / 1e12);
                if (absVal >= 1e9) return String.format("%.1fB", val / 1e9);
                if (absVal >= 1e6) return String.format("%.1fM", val / 1e6);
                if (absVal >= 1e3) return String.format("%.1fk", val / 1e3);
                if (absVal < 0.01 && absVal > 0) return String.format("%.3f", val);
                return String.format("%.1f", val);
            }

            @Override
            public Number fromString(String string) {
                return null;
            }
        });

        Tooltip.install(p.xAxis, new Tooltip(I18n.getOrDefault("analytics.axis.tooltip_x", "Chronological Timeline: Calendar years (BC / AD). Mouse wheel zooms on cursor, drag pans, double-click resets.")));
        Tooltip.install(p.yAxis, new Tooltip(I18n.getOrDefault("analytics.axis.tooltip_y", "Physical Metric Value with standardized dimensional SI units.")));

        p.chart = new LineChart<>(p.xAxis, p.yAxis);
        p.chart.setAnimated(false);
        p.chart.setTitle(I18n.getOrDefault("analytics.chart.title", "Multi-Scenario Chronological Overlay (💡 Scroll to Zoom on Cursor, Drag to Pan, Double-Click to Reset)"));
        p.chart.getStyleClass().add("card-section");

        final double[] dragAnchor = new double[2];
        p.chart.setOnMousePressed(e -> {
            dragAnchor[0] = e.getX();
            dragAnchor[1] = e.getY();
        });
        p.chart.setOnMouseDragged(e -> {
            double dx = e.getX() - dragAnchor[0];
            dragAnchor[0] = e.getX();
            double range = p.xAxis.getUpperBound() - p.xAxis.getLowerBound();
            double plotWidth = Math.max(10.0, p.chart.getWidth() - 80.0);
            double shift = (dx / plotWidth) * range;
            ComparativeTimeSeriesChartCoordinator.setXAxisRange(p.xAxis, p.xAxis.getLowerBound() - shift, p.xAxis.getUpperBound() - shift);
            p.chart.setCursor(Cursor.CLOSED_HAND);
        });
        p.chart.setOnMouseReleased(e -> p.chart.setCursor(Cursor.DEFAULT));
        p.chart.setOnScroll(e -> {
            e.consume();
            double zoomFactor = e.getDeltaY() > 0 ? 0.82 : 1.22;
            double currentLower = p.xAxis.getLowerBound();
            double currentUpper = p.xAxis.getUpperBound();
            double range = currentUpper - currentLower;

            double plotX = Math.max(0.0, Math.min(p.chart.getWidth(), e.getX() - 50.0));
            double plotWidth = Math.max(10.0, p.chart.getWidth() - 80.0);
            double mouseRatio = Math.max(0.0, Math.min(1.0, plotX / plotWidth));
            double mouseDataX = currentLower + mouseRatio * range;

            double newLower = mouseDataX - (mouseDataX - currentLower) * zoomFactor;
            double newUpper = mouseDataX + (currentUpper - mouseDataX) * zoomFactor;
            if (newUpper - newLower > 0.5) {
                ComparativeTimeSeriesChartCoordinator.setXAxisRange(p.xAxis, newLower, newUpper);
            }
        });
        p.chart.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 || e.getButton() == MouseButton.SECONDARY) {
                p.updateChartAndAnalysis();
            }
        });
    }

    static void updateExecutionContextBadge(ComparativeAnalyticsPanel p) {
        if (p.executionContextBadge == null) return;
        int cores = Runtime.getRuntime().availableProcessors();
        p.executionContextBadge.setText(String.format(I18n.getOrDefault("analytics.badge.context_gpu_off_fmt", "💻 Contexte : CPU Multi-cœurs (%d cœurs | Sans GPU)"), cores));
        p.executionContextBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-padding: 0; -fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: default;");
        p.executionContextBadge.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.context_details",
                "Contexte d'exécution de la simulation : calcul multithreadé sur les cœurs CPU de la machine locale avec vectorisation SIMD native. Le GPU est inactif pour garantir une stricte reproductibilité déterministe bit-à-bit.")));
    }

    static void copyReportToClipboard(ComparativeAnalyticsPanel p) {
        String currentReport = (p.reportPreviewPane != null) ? p.reportPreviewPane.getMarkdown() : "";
        if (currentReport == null || currentReport.isBlank() || currentReport.startsWith("## Please select")) return;

        try {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(currentReport);
            clipboard.setContent(content);

            String origText = p.copyReportBtn.getText();
            p.copyReportBtn.setText(I18n.getOrDefault("analytics.status.copy_success", "✅ Rapport Copié !"));
            p.copyReportBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #059669; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");

            Timeline resetBtnTimer = new Timeline(new KeyFrame(Duration.millis(2500), ev -> {
                p.copyReportBtn.setText(origText);
                p.copyReportBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
            }));
            resetBtnTimer.setCycleCount(1);
            resetBtnTimer.play();
            ComparativeAnalyticsPanel.logger.info("Successfully copied markdown analysis report to clipboard.");
        } catch (Exception ex) {
            ComparativeAnalyticsPanel.logger.error("Failed to copy report to system clipboard: {}", ex.getMessage());
        }
    }

    static void setupInterpolationComboRendering(ComboBox<HistoricalValidationKernel.InterpolationMethod> combo) {
        if (combo == null) return;
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(HistoricalValidationKernel.InterpolationMethod item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : I18n.getOrDefault("interpolation." + item.name().toLowerCase(Locale.ROOT), item.name()));
            }
        });
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(HistoricalValidationKernel.InterpolationMethod item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : I18n.getOrDefault("interpolation." + item.name().toLowerCase(Locale.ROOT), item.name()));
            }
        });
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(HistoricalValidationKernel.InterpolationMethod object) {
                return object == null ? "" : I18n.getOrDefault("interpolation." + object.name().toLowerCase(Locale.ROOT), object.name());
            }

            @Override
            public HistoricalValidationKernel.InterpolationMethod fromString(String string) {
                return null;
            }
        });
    }

    /**
     * Refreshes all localized text strings, tooltips, and metric selector categories.
     *
     * @param p host comparative analytics panel
     */
    static void updateTexts(ComparativeAnalyticsPanel p) {
        if (p.headerLabel != null) p.headerLabel.setText(I18n.getOrDefault("analytics.header", "📊 COMPARATIVE ANALYTICS & SCENARIO BENCHMARKS (DEEP ANALYTICS)"));
        if (p.tableTitle != null) p.tableTitle.setText(I18n.getOrDefault("analytics.table_title", "📋 Historical Baseline & Simulated Scenarios"));
        if (p.tableColumns != null) {
            if (p.tableColumns.selectCol() != null) p.tableColumns.selectCol().setText(I18n.getOrDefault("analytics.col.compare", "Compare"));
            if (p.tableColumns.nameCol() != null) p.tableColumns.nameCol().setText(I18n.getOrDefault("analytics.col.name", "Scenario Name"));
            if (p.tableColumns.planetCol() != null) p.tableColumns.planetCol().setText(I18n.getOrDefault("analytics.col.planet", "Planète"));
            if (p.tableColumns.clippingCol() != null) p.tableColumns.clippingCol().setText(I18n.getOrDefault("analytics.col.clipping", "Fenêtrage"));
            if (p.tableColumns.yearsCol() != null) p.tableColumns.yearsCol().setText(I18n.getOrDefault("analytics.col.years", "Time Range"));
            if (p.tableColumns.progressCol() != null) p.tableColumns.progressCol().setText(I18n.getOrDefault("analytics.col.progress", "Progress"));
            if (p.tableColumns.estDurationCol() != null) p.tableColumns.estDurationCol().setText(I18n.getOrDefault("analytics.col.est_duration", "Remaining Time"));
            if (p.tableColumns.statusCol() != null) p.tableColumns.statusCol().setText(I18n.getOrDefault("analytics.col.status", "Database Execution Status"));
        }
        if (p.metricLabel != null) p.metricLabel.setText(I18n.getOrDefault("analytics.metric_label", "Visualized Metric:"));
        if (p.interpolationLabel != null) p.interpolationLabel.setText(I18n.getOrDefault("analytics.interp_label", "Interpolation:"));
        if (p.spatialTensorComparisonPane != null) p.spatialTensorComparisonPane.updateTexts(p.scenarioList);
        updateExecutionContextBadge(p);

        if (p.copyReportBtn != null) {
            p.copyReportBtn.setText(I18n.getOrDefault("analytics.btn.copy", "📋 Copy Report"));
            p.copyReportBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.copy", "Copy markdown audit report.")));
        }
        if (p.exportMdBtn != null) {
            p.exportMdBtn.setText(I18n.getOrDefault("analytics.btn.export_md", "📝 Export Report (.md)"));
            p.exportMdBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.export_md", "Export complete markdown report.")));
        }
        if (p.exportCsvBtn != null) {
            p.exportCsvBtn.setText(I18n.getOrDefault("analytics.btn.export_csv", "📥 Export Data (.csv)"));
            p.exportCsvBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.export_csv", "Export raw multi-scenario telemetry into CSV.")));
        }
        if (p.export1DCsvBtn != null) {
            p.export1DCsvBtn.setText(I18n.getOrDefault("analytics.btn.export_1d_csv", "📥 Exporter les données CSV (Séries 1D)"));
            p.export1DCsvBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.export_1d_csv", "Exporter les séries temporelles 1D sélectionnées en CSV.")));
        }
        if (p.executeMissingBtn != null) {
            p.executeMissingBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.execute", "Run physical headless simulation across queued scenarios.")));
        }
        if (p.cancelBatchBtn != null) {
            p.cancelBatchBtn.setText(I18n.getOrDefault("analytics.btn.cancel_batch", "🛑 Cancel Batch"));
            p.cancelBatchBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.cancel", "Cancel pending background batch.")));
        }
        if (p.chartHintLabel != null) {
            p.chartHintLabel.setText(I18n.getOrDefault("analytics.chart.hint", "💡 Scroll wheel: Zoom on cursor | Drag: Pan horizontally | Double-click: Reset time range"));
        }
        if (p.xAxis != null) p.xAxis.setLabel(I18n.getOrDefault("analytics.axis.x", "Calendar Year (BCE / CE)"));
        if (p.yAxis != null) p.yAxis.setLabel(I18n.getOrDefault("analytics.axis.y", "Metric Value"));
        if (p.chart != null) p.chart.setTitle(I18n.getOrDefault("analytics.chart.title", "Multi-Scenario Chronological Overlay"));
        if (p.searchField != null) p.searchField.setPromptText(I18n.getOrDefault("analytics.search_prompt", "🔍 Filter scenarios by name, status, or year range..."));
        if (p.scenariosTab != null) p.scenariosTab.setText(I18n.getOrDefault("analytics.tab.scenarios", "📋 Scenarios & Data"));
        if (p.timeSeriesTab != null) p.timeSeriesTab.setText(I18n.getOrDefault("analytics.tab.timeseries", "📈 Time Series (1D)"));
        if (p.spatialCartoTab != null) p.spatialCartoTab.setText(I18n.getOrDefault("analytics.tab.carto_tensors", "🗺️ Cartography & Tensors (2D)"));
        if (p.culturalMatrixTab != null) p.culturalMatrixTab.setText(I18n.getOrDefault("analytics.tab.cultural_matrix", "🧩 Cultural Matrices & Affinities"));
        if (p.culturalMatrixComparisonPane != null) p.culturalMatrixComparisonPane.updateTexts();
        if (p.reportTab != null) p.reportTab.setText(I18n.getOrDefault("analytics.tab.report", "📄 Analysis Report & Synthesis"));

        if (p.divergenceLabel != null && (p.divergenceLabel.getText() == null || p.divergenceLabel.getText().isBlank()
                || p.divergenceLabel.getText().startsWith("Point de rupture")
                || p.divergenceLabel.getText().startsWith("Point of divergence")
                || p.divergenceLabel.getText().startsWith("Break point"))) {
            p.divergenceLabel.setText(I18n.getOrDefault("analytics.divergence.select_hint", "Point of divergence: Select at least 2 scenarios"));
        }
        if (p.explanationLabel != null && (p.explanationLabel.getText() == null || p.explanationLabel.getText().isBlank()
                || p.explanationLabel.getText().startsWith("Cochez les scénarios")
                || p.explanationLabel.getText().startsWith("Check scenarios"))) {
            p.explanationLabel.setText(I18n.getOrDefault("analytics.divergence.check_hint", "Check scenarios in the list above to start comparison."));
        }

        if (p.interpolationCombo != null) {
            HistoricalValidationKernel.InterpolationMethod currentInterp = p.interpolationCombo.getValue();
            setupInterpolationComboRendering(p.interpolationCombo);
            if (currentInterp != null) p.interpolationCombo.setValue(currentInterp);
        }

        populateMetricSelectorCombo(p.metricSelectorCombo);
    }

    private static void populateMetricSelectorCombo(ComboBox<String> metricSelectorCombo) {
        if (metricSelectorCombo == null) return;
        String selected = metricSelectorCombo.getValue();
        metricSelectorCombo.getItems().clear();

        Map<MetricDescriptor.Category, List<MetricDescriptor>> grouped = new TreeMap<>();
        for (MetricDescriptor d : MetricRegistry.getInstance().getAllMetrics()) {
            grouped.computeIfAbsent(d.getCategory(), k -> new ArrayList<>()).add(d);
        }

        for (var entry : grouped.entrySet()) {
            metricSelectorCombo.getItems().add("── " + entry.getKey().getDisplayName() + " ──");
            List<MetricDescriptor> metrics = entry.getValue();
            metrics.sort(Comparator.comparing(MetricDescriptor::getDisplayName, String.CASE_INSENSITIVE_ORDER));
            for (MetricDescriptor d : metrics) {
                metricSelectorCombo.getItems().add(d.getDisplayName());
            }
        }

        metricSelectorCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                } else if (item.startsWith("──")) {
                    setText(item);
                    setDisable(true);
                    setStyle("-fx-font-weight: bold; -fx-opacity: 0.7; -fx-padding: 4 8; -fx-text-fill: #38bdf8;");
                } else {
                    setText(item);
                    setDisable(false);
                    setStyle("-fx-font-weight: normal; -fx-padding: 2 12;");
                }
            }
        });

        if (selected != null && metricSelectorCombo.getItems().contains(selected) && !selected.startsWith("──")) {
            metricSelectorCombo.setValue(selected);
        } else {
            for (String item : metricSelectorCombo.getItems()) {
                if (!item.startsWith("──")) {
                    metricSelectorCombo.setValue(item);
                    break;
                }
            }
        }
    }
}
