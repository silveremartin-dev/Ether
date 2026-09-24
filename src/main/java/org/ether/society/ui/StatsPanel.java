/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.PluggableStatEngine;
import org.ether.society.i18n.I18n;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

import java.util.*;

/**
 * Enhanced Cliodynamic & Physicalist Statistics Dashboard.
 * Features categorized metrics, dynamic historical time-series graphing,
 * search filtering, age pyramid visualization, and CSV export.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class StatsPanel extends VBox {
    private final H3SimulationEngine engine;

    // Controls Header
    private final Label headerTitle;
    private final Label cpuNoticeLabel;
    private final ToggleButton btnLiveCollection;
    private final ComboBox<String> samplingCombo;
    private final Button btnFormulaEditor;

    private final Label chartHeaderLabel;
    private final Label comboLabel;
    private final Label windowLabel;
    private final ToggleButton btn1Yr;
    private final ToggleButton btn10Yr;
    private final ToggleButton btn100Yr;
    private final ToggleButton btn1000Yr;
    private final ToggleButton btnAll;

    private final ComboBox<String> categoryFilterCombo;
    private final TextField searchField;
    private final ComboBox<String> chartMetricCombo;
    private final Button exportBtn;

    // Line Chart for selected metric
    private final LineChart<Number, Number> lineChart;
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    private final XYChart.Series<Number, Number> chartSeries = new XYChart.Series<>();

    // Bar Chart for Age Pyramid / Distribution
    private final Label barHeaderLabel;
    private final CategoryAxis barX;
    private final NumberAxis barY;
    private final BarChart<String, Number> barChart;
    private final XYChart.Series<String, Number> barSeries = new XYChart.Series<>();

    // Metric Labels Registry
    private final Map<String, MetricCard> metricCards = new LinkedHashMap<>();

    // Pluggable Formula & Variance Analytics Engine
    private final PluggableStatEngine pluggableStatEngine = new PluggableStatEngine();
    private final VarianceDistributionPanel variancePanel;
    private final SpatialHeatmapPanel spatialHeatmapPanel = new SpatialHeatmapPanel();
    private boolean isLiveCollectionActive = true;
    private int samplingIntervalTicks = 1;
    private long tickCounter = 0;
    private java.util.function.Consumer<DisplayMode> onDisplayModeRequested;

    private final Label metricsHeaderLabel;
    private final Label metricInspectorTitle;
    private final Label metricInspectorText;

    // Time window in years (-1 for all history)
    private double currentWindowYears = 10.0;
    private boolean isUpdatingTexts = false;

    public void setOnDisplayModeRequested(java.util.function.Consumer<DisplayMode> listener) {
        this.onDisplayModeRequested = listener;
    }

    public void setSelectedMetricByDisplayMode(DisplayMode mode) {
        if (mode == null) return;
        String metricId = mode.getMetricId();
        org.ether.society.analytics.MetricDescriptor desc = org.ether.society.analytics.MetricRegistry.getInstance().getDescriptor(metricId);
        if (desc != null) {
            String title = desc.getDisplayName();
            if (chartMetricCombo != null && chartMetricCombo.getItems().contains(title)) {
                chartMetricCombo.setValue(title);
                resetChartSeries();
            }
        }
    }

    // Container for metric sections
    private final VBox metricsContainer;

    /**
     * Minimalist financial sparkline chart canvas (70x20 px) showing recent trend (green for up, red for down, cyan for stable).
     */
    public static class SparklineCanvas extends javafx.scene.canvas.Canvas {
        private final double[] history = new double[24];
        private int count = 0;
        private int head = 0;

        public SparklineCanvas() {
            super(70, 20);
            drawEmpty();
        }

        public void addValue(double val) {
            if (Double.isNaN(val) || Double.isInfinite(val)) return;
            history[head] = val;
            head = (head + 1) % history.length;
            if (count < history.length) count++;
            redraw();
        }

        private void drawEmpty() {
            var gc = getGraphicsContext2D();
            gc.clearRect(0, 0, getWidth(), getHeight());
            gc.setStroke(Color.rgb(100, 116, 139, 0.4));
            gc.setLineWidth(1.0);
            gc.strokeLine(2, getHeight() / 2, getWidth() - 2, getHeight() / 2);
        }

        private void redraw() {
            var gc = getGraphicsContext2D();
            double w = getWidth();
            double h = getHeight();
            gc.clearRect(0, 0, w, h);

            if (count < 2) {
                drawEmpty();
                return;
            }

            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;

            double[] ordered = new double[count];
            for (int i = 0; i < count; i++) {
                int idx = (head - count + i + history.length) % history.length;
                ordered[i] = history[idx];
                if (ordered[i] < min) min = ordered[i];
                if (ordered[i] > max) max = ordered[i];
            }

            double range = max - min;
            if (range <= 0.00001) {
                gc.setStroke(Color.rgb(148, 163, 184, 0.8));
                gc.setLineWidth(1.5);
                gc.strokeLine(2, h / 2, w - 2, h / 2);
                return;
            }

            double firstVal = ordered[0];
            double lastVal = ordered[count - 1];
            Color lineColor;
            Color fillColor;
            if (lastVal > firstVal * 1.002) {
                lineColor = Color.rgb(34, 197, 94); // Up-trend Green
                fillColor = Color.rgb(34, 197, 94, 0.15);
            } else if (lastVal < firstVal * 0.998) {
                lineColor = Color.rgb(239, 68, 68); // Down-trend Red
                fillColor = Color.rgb(239, 68, 68, 0.15);
            } else {
                lineColor = Color.rgb(56, 189, 248); // Stable Cyan
                fillColor = Color.rgb(56, 189, 248, 0.12);
            }

            double padY = 3.0;
            double usableH = h - (padY * 2);
            double stepX = (w - 6.0) / (count - 1);

            double[] xPoints = new double[count + 2];
            double[] yPoints = new double[count + 2];

            for (int i = 0; i < count; i++) {
                xPoints[i] = 3.0 + i * stepX;
                double norm = (ordered[i] - min) / range;
                yPoints[i] = h - padY - (norm * usableH);
            }

            xPoints[count] = xPoints[count - 1];
            yPoints[count] = h;
            xPoints[count + 1] = xPoints[0];
            yPoints[count + 1] = h;

            gc.setFill(fillColor);
            gc.fillPolygon(xPoints, yPoints, count + 2);

            gc.setStroke(lineColor);
            gc.setLineWidth(1.5);
            for (int i = 0; i < count - 1; i++) {
                gc.strokeLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
            }

            gc.setFill(lineColor);
            double lastX = xPoints[count - 1];
            double lastY = yPoints[count - 1];
            gc.fillOval(lastX - 2.0, lastY - 2.0, 4.0, 4.0);
        }
    }

    /** Class to hold UI elements for a single metric card */
    private static class MetricCard extends HBox {
        private final String key;
        private String title;
        private String category;
        private final Label titleLabel;
        private final Label valueLabel;
        private final SparklineCanvas sparkline;
        private String unit;
        private String tooltipText;
        private double lastVal = 0.0;

        public MetricCard(String key, String title, String category, String unit, String tooltipText) {
            this.key = key;
            this.title = title;
            this.category = category;
            this.unit = unit;
            this.tooltipText = tooltipText;

            setPadding(new Insets(5, 8, 5, 8));
            setSpacing(8);
            setAlignment(Pos.CENTER_LEFT);
            getStyleClass().add("card-section");

            titleLabel = new Label(title + ":");
            titleLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
            titleLabel.setMinWidth(160);
            titleLabel.setMaxWidth(180);

            valueLabel = new Label("-- " + unit);
            valueLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold;");
            valueLabel.setMinWidth(95);

            sparkline = new SparklineCanvas();

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            getChildren().addAll(titleLabel, valueLabel, spacer, sparkline);

            if (tooltipText != null && !tooltipText.isBlank()) {
                Tooltip.install(this, new Tooltip(tooltipText));
            }
        }

        public void updateTexts(String title, String category, String unit, String tooltipText) {
            this.title = title;
            this.category = category;
            this.unit = unit;
            this.tooltipText = tooltipText;
            titleLabel.setText(title + ":");
            if (tooltipText != null && !tooltipText.isBlank()) {
                Tooltip.install(this, new Tooltip(tooltipText));
            }
        }

        public void updateValue(String displayValue, double rawNumericValue) {
            this.lastVal = rawNumericValue;
            valueLabel.setText(displayValue + (unit.isEmpty() ? "" : " " + unit));
            sparkline.addValue(rawNumericValue);
        }

        public String getKey() { return key; }
        public String getCategory() { return category; }
        public String getTitle() { return title; }
        public String getTooltipText() { return tooltipText; }
    }

    public StatsPanel(H3SimulationEngine engine) {
        this.engine = engine;
        this.variancePanel = new VarianceDistributionPanel(pluggableStatEngine);

        setPadding(new Insets(12));
        setSpacing(12);
        getStyleClass().add("glass-panel");

        // --- TOP HEADER TOOLBAR ---
        headerTitle = new Label();
        headerTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        cpuNoticeLabel = new Label();
        cpuNoticeLabel.setWrapText(true);
        cpuNoticeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #93c5fd; -fx-padding: 4 8; -fx-background-color: rgba(30, 58, 138, 0.4); -fx-background-radius: 4; -fx-border-color: rgba(59, 130, 246, 0.4); -fx-border-radius: 4;");

        btnLiveCollection = new ToggleButton();
        btnLiveCollection.setSelected(true);
        btnLiveCollection.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 8;");
        btnLiveCollection.setOnAction(e -> {
            isLiveCollectionActive = btnLiveCollection.isSelected();
            btnLiveCollection.setText(isLiveCollectionActive
                    ? I18n.getOrDefault("stats.btn.live_collection_active", "⚡ Collecte Stats : ACTIF")
                    : I18n.getOrDefault("stats.btn.live_collection_paused", "⏸️ Collecte Stats : EN PAUSE"));
            btnLiveCollection.setStyle(isLiveCollectionActive
                    ? "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 8;"
                    : "-fx-background-color: #64748b; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 8;");
        });

        samplingCombo = new ComboBox<>();
        samplingCombo.setStyle("-fx-font-size: 10px;");
        samplingCombo.setOnAction(e -> {
            int idx = samplingCombo.getSelectionModel().getSelectedIndex();
            samplingIntervalTicks = switch (idx) {
                case 1 -> 5;
                case 2 -> 20;
                case 3 -> 100;
                default -> 1;
            };
        });

        btnFormulaEditor = new Button();
        btnFormulaEditor.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 8;");
        btnFormulaEditor.setOnAction(e -> {
            PluggableFormulaEditorDialog dlg = new PluggableFormulaEditorDialog(pluggableStatEngine, engine != null ? engine.getCells() : null);
            dlg.showAndWait();
            update();
        });

        HBox perfToolbar = new HBox(8, btnLiveCollection, samplingCombo, btnFormulaEditor);
        perfToolbar.setAlignment(Pos.CENTER_LEFT);

        VBox topControlsBox = new VBox(6, headerTitle, cpuNoticeLabel, perfToolbar);
        topControlsBox.getStyleClass().add("card-section");

        // --- SECTION 1: GRAPH SELECTION & TIME SERIES ---
        chartHeaderLabel = new Label();
        chartHeaderLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        comboLabel = new Label();
        comboLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-weight: bold;");

        chartMetricCombo = new ComboBox<>();
        chartMetricCombo.setMaxWidth(Double.MAX_VALUE);
        chartMetricCombo.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else if (item.startsWith("─── ") && item.endsWith(" ───")) {
                    setDisable(true);
                    setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 6; -fx-opacity: 1.0; -fx-background-color: rgba(30, 41, 59, 0.5);");
                    setText(item);
                } else {
                    setDisable(false);
                    setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0; -fx-padding: 3 12;");
                    setText("  " + item);
                }
            }
        });
        chartMetricCombo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
                }
            }
        });
        chartMetricCombo.setOnAction(e -> {
            if (isUpdatingTexts) return;
            String title = chartMetricCombo.getValue();
            if (title != null && !title.startsWith("─── ")) {
                resetChartSeries();
                if (onDisplayModeRequested != null) {
                    org.ether.society.analytics.MetricDescriptor desc = org.ether.society.analytics.MetricRegistry.getInstance().getDescriptorByName(title);
                    if (desc != null) {
                        onDisplayModeRequested.accept(DisplayMode.fromMetricId(desc.getId()));
                    }
                }
            }
        });

        // Sliding Time Window Toggle Buttons
        windowLabel = new Label();
        windowLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-weight: bold;");

        ToggleGroup windowGroup = new ToggleGroup();
        btn1Yr = new ToggleButton();
        btn10Yr = new ToggleButton();
        btn100Yr = new ToggleButton();
        btn1000Yr = new ToggleButton();
        btnAll = new ToggleButton();

        for (ToggleButton b : new ToggleButton[]{btn1Yr, btn10Yr, btn100Yr, btn1000Yr, btnAll}) {
            b.setToggleGroup(windowGroup);
            b.setStyle("-fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 4;");
        }
        btn10Yr.setSelected(true);
        this.currentWindowYears = 10.0;

        btn1Yr.setOnAction(e -> setTimeWindow(1.0));
        btn10Yr.setOnAction(e -> setTimeWindow(10.0));
        btn100Yr.setOnAction(e -> setTimeWindow(100.0));
        btn1000Yr.setOnAction(e -> setTimeWindow(1000.0));
        btnAll.setOnAction(e -> setTimeWindow(-1.0));

        HBox windowBox = new HBox(6, windowLabel, btn1Yr, btn10Yr, btn100Yr, btn1000Yr, btnAll);
        windowBox.setAlignment(Pos.CENTER_LEFT);

        xAxis = new NumberAxis();
        xAxis.setTickLabelFill(Color.GRAY);
        xAxis.setAutoRanging(true);
        xAxis.setForceZeroInRange(false);
        xAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                if (object == null) return "";
                double val = object.doubleValue();
                long y = (long) Math.floor(val);
                long absY = Math.abs(y);
                boolean isFr = I18n.getCurrentLanguage() == org.ether.society.i18n.Language.FRENCH;
                if (y < 0) {
                    return isFr ? String.format("%,d av. J.-C.", absY) : String.format("%,d BC", absY);
                } else if (y > 0) {
                    return isFr ? String.format("%,d ap. J.-C.", y) : String.format("%,d AD", y);
                } else {
                    return isFr ? "An 0" : "0 AD";
                }
            }
            @Override
            public Number fromString(String string) { return 0; }
        });

        yAxis = new NumberAxis();
        yAxis.setTickLabelFill(Color.GRAY);
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(false);

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(false);
        lineChart.setAnimated(false);
        lineChart.setLegendVisible(false);
        lineChart.setPrefHeight(160);
        lineChart.getData().add(chartSeries);

        // Clamped Interactive Mouse Zoom (CTRL + Scroll Wheel) & Pan (CTRL + Drag)
        final double[] dragAnchor = new double[2];
        lineChart.setOnMousePressed(e -> {
            dragAnchor[0] = e.getX();
            dragAnchor[1] = e.getY();
        });
        lineChart.setOnMouseDragged(e -> {
            if (!e.isControlDown()) return;
            if (xAxis.isAutoRanging()) xAxis.setAutoRanging(false);
            double minYear = getMinAllowedYear();
            double maxYear = getMaxAllowedYear();
            double dx = e.getX() - dragAnchor[0];
            dragAnchor[0] = e.getX();
            double range = xAxis.getUpperBound() - xAxis.getLowerBound();
            double shift = (dx / Math.max(1.0, lineChart.getWidth())) * range;
            double newLower = xAxis.getLowerBound() - shift;
            double newUpper = xAxis.getUpperBound() - shift;
            if (newLower < minYear) {
                newUpper += (minYear - newLower);
                newLower = minYear;
            }
            if (newUpper > maxYear) {
                newLower -= (newUpper - maxYear);
                newUpper = maxYear;
            }
            if (newLower < minYear) newLower = minYear;
            xAxis.setLowerBound(newLower);
            xAxis.setUpperBound(newUpper);
        });
        lineChart.setOnScroll(e -> {
            if (!e.isControlDown()) return;
            e.consume();
            if (xAxis.isAutoRanging()) xAxis.setAutoRanging(false);
            double minYear = getMinAllowedYear();
            double maxYear = getMaxAllowedYear();
            double zoomFactor = e.getDeltaY() > 0 ? 0.85 : 1.15;
            double center = (xAxis.getLowerBound() + xAxis.getUpperBound()) / 2.0;
            double halfSpan = Math.max(0.5, ((xAxis.getUpperBound() - xAxis.getLowerBound()) / 2.0) * zoomFactor);
            double newLower = Math.max(minYear, center - halfSpan);
            double newUpper = Math.min(maxYear, center + halfSpan);
            if (newUpper <= newLower) newUpper = newLower + 1.0;
            xAxis.setLowerBound(newLower);
            xAxis.setUpperBound(newUpper);
        });
        lineChart.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 || (e.isControlDown() && e.getButton() == javafx.scene.input.MouseButton.SECONDARY)) {
                if (currentWindowYears > 0) {
                    setTimeWindow(currentWindowYears);
                } else {
                    xAxis.setAutoRanging(true);
                    yAxis.setAutoRanging(true);
                }
            }
        });

        VBox chartBox = new VBox(6, chartHeaderLabel, comboLabel, chartMetricCombo, windowBox, lineChart);
        chartBox.getStyleClass().add("card-section");

        // --- SECTION 2: DEMOGRAPHICS (AGE PYRAMID) ---
        barHeaderLabel = new Label();
        barHeaderLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        barX = new CategoryAxis();
        barX.setTickLabelFill(Color.GRAY);
        barY = new NumberAxis();
        barY.setTickLabelFill(Color.GRAY);
        barChart = new BarChart<>(barX, barY);
        barChart.setAnimated(false);
        barChart.setLegendVisible(false);
        barChart.setPrefHeight(130);
        barChart.getData().add(barSeries);

        VBox barBox = new VBox(6, barHeaderLabel, barChart);
        barBox.getStyleClass().add("card-section");

        // --- SECTION 3: METRIC CARDS & CATEGORY FILTER ---
        metricsHeaderLabel = new Label();
        metricsHeaderLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        // Interactive Metric Inspector Panel
        metricInspectorTitle = new Label();
        metricInspectorTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #ffd700;");
        metricInspectorText = new Label();
        metricInspectorText.setWrapText(true);
        metricInspectorText.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");

        VBox metricInspectorCard = new VBox(4, metricInspectorTitle, metricInspectorText);
        metricInspectorCard.setStyle("-fx-padding: 8 10; -fx-background-color: rgba(255, 215, 0, 0.08); -fx-background-radius: 6; -fx-border-color: rgba(255, 215, 0, 0.25); -fx-border-radius: 6;");

        // Category Filter
        categoryFilterCombo = new ComboBox<>();
        categoryFilterCombo.setMaxWidth(Double.MAX_VALUE);
        categoryFilterCombo.setOnAction(e -> filterMetrics());

        // Search Field
        searchField = new TextField();
        searchField.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); -fx-text-fill: white; -fx-prompt-text-fill: #64748b;");
        searchField.textProperty().addListener((obs, oldV, newV) -> filterMetrics());

        // Export Button
        exportBtn = new Button();
        exportBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6;");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        exportBtn.setOnAction(e -> exportCsv());

        VBox cardsControlBox = new VBox(6, metricsHeaderLabel, metricInspectorCard, categoryFilterCombo, searchField, exportBtn);
        cardsControlBox.getStyleClass().add("card-section");

        // Container for metric sections
        metricsContainer = new VBox(6);

        registerMetricCards(metricInspectorTitle, metricInspectorText);

        getChildren().addAll(topControlsBox, chartBox, barBox, spatialHeatmapPanel, variancePanel, cardsControlBox, metricsContainer);

        updateTexts();
        I18n.languageProperty().addListener((obs, oldL, newL) -> updateTexts());
    }

    private long getMinAllowedYear() {
        if (engine != null && engine.getCurrentScenario() != null) {
            return engine.getCurrentScenario().getStartDateYear();
        }
        if (engine != null && engine.getTimeManager() != null) {
            return engine.getTimeManager().getCurrentYear();
        }
        return 0L;
    }

    private long getMaxAllowedYear() {
        if (engine != null && engine.getCurrentScenario() != null) {
            long endYr = engine.getCurrentScenario().getEndDateYear();
            long currentYr = engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : endYr;
            return Math.max(endYr, currentYr);
        }
        if (engine != null && engine.getTimeManager() != null) {
            return engine.getTimeManager().getCurrentYear() + 100L;
        }
        return 2100L;
    }

    private void setTimeWindow(double windowYears) {
        this.currentWindowYears = windowYears;
        reloadChartData();
    }

    public void updateTexts() {
        isUpdatingTexts = true;
        try {
            headerTitle.setText(I18n.getOrDefault("stats.header", "📊 TABLEAU DE BORD STATISTIQUE & CLIODYNAMIQUE"));
            cpuNoticeLabel.setText(I18n.getOrDefault("stats.notice.cpu",
                "⚠️ EXPLICATION PERFORMANCE CPU : L'agrégation statistique en temps réel (indices Gini, entropie thermodynamique, " +
                "analyse Turchin, variances et maillages H3) effectue des calculs intensifs sur chaque cellule à chaque cycle. " +
                "Si la simulation ralentit, réduisez la fréquence d'échantillonnage ci-dessous (ex: 5 ou 20 ticks) ou mettez la collecte en pause."));

            btnLiveCollection.setText(isLiveCollectionActive
                    ? I18n.getOrDefault("stats.btn.live_collection_active", "⚡ Collecte Stats : ACTIF")
                    : I18n.getOrDefault("stats.btn.live_collection_paused", "⏸️ Collecte Stats : EN PAUSE"));
            btnLiveCollection.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.live_collection", "Activer/Désactiver le calcul des statistiques dynamiques pour économiser le CPU.")));

            int selectedSampling = samplingCombo.getSelectionModel().getSelectedIndex();
            samplingCombo.getItems().clear();
            samplingCombo.getItems().addAll(
                I18n.getOrDefault("stats.sampling.1tick", "1 Tick (Chaque Cycle)"),
                I18n.getOrDefault("stats.sampling.5ticks", "5 Ticks"),
                I18n.getOrDefault("stats.sampling.20ticks", "20 Ticks"),
                I18n.getOrDefault("stats.sampling.100ticks", "100 Ticks")
            );
            samplingCombo.getSelectionModel().select(selectedSampling >= 0 ? selectedSampling : 0);
            samplingCombo.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.sampling", "Fréquence d'échantillonnage et de rafraîchissement des métriques.")));

            btnFormulaEditor.setText(I18n.getOrDefault("stats.btn.formula_editor", "🧮 Éditeur de Formules & Variables"));
            btnFormulaEditor.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.formula_editor", "Ouvrir l'éditeur de formules cliodynamiques personnalisées (SUM, AVG, GINI, etc.).")));

            chartHeaderLabel.setText(I18n.getOrDefault("stats.section.time_evolution", "📈 Évolution temporelle des indicateurs"));
            comboLabel.setText(I18n.getOrDefault("stats.label.traced_stat", "Indicateur tracé :"));
            windowLabel.setText(I18n.getOrDefault("stats.label.window", "Fenêtre :"));

            btn1Yr.setText(I18n.getOrDefault("stats.window.1yr", "1 An"));
            btn10Yr.setText(I18n.getOrDefault("stats.window.10yr", "10 Ans"));
            btn100Yr.setText(I18n.getOrDefault("stats.window.100yr", "100 Ans"));
            btn1000Yr.setText(I18n.getOrDefault("stats.window.1000yr", "1000 Ans"));
            btnAll.setText(I18n.getOrDefault("stats.window.all", "Tout"));

            xAxis.setLabel(I18n.getOrDefault("stats.chart.time_axis", "Temps (Années)"));
            lineChart.setTitle(I18n.getOrDefault("stats.chart.title", "Courbe d'Évolution Temporelle (💡 [CTRL] + Molette pour Zoomer | [CTRL] + Glisser pour Déplacer | Double-Clic pour Réinitialiser)"));

            barHeaderLabel.setText(I18n.getOrDefault("stats.section.age_pyramid", "📊 RÉPARTITION DÉMOGRAPHIQUE (Pyramide des Âges)"));
            barChart.setTitle(I18n.getOrDefault("stats.chart.age_pyramid_title", "Pyramide des Âges"));

            metricsHeaderLabel.setText(I18n.getOrDefault("stats.section.detailed_metrics", "📋 INDICATEURS CLIODYNAMIQUES & MÉTRIQUES DÉTAILLÉES"));
            metricInspectorTitle.setText(I18n.getOrDefault("stats.label.metric_explanation", "🔎 Explication de la Métrique Cliodynamique :"));
            metricInspectorText.setText(I18n.getOrDefault("stats.desc.hover_metric", "Survolez ou cliquez sur une carte statistique ci-dessous pour afficher sa formule mathématique, son mode de calcul et sa signification sociétale."));

            int catIdx = categoryFilterCombo.getSelectionModel().getSelectedIndex();
            categoryFilterCombo.getItems().clear();
            categoryFilterCombo.getItems().addAll(
                I18n.getOrDefault("stats.cat.all", "Toutes les Catégories"),
                I18n.getOrDefault("stats.cat.energy", "⚡ Énergie & Matière"),
                I18n.getOrDefault("stats.cat.demography", "👥 Démographie & Santé"),
                I18n.getOrDefault("stats.cat.society", "🏛️ Société & Institutions"),
                I18n.getOrDefault("stats.cat.economy", "💎 Économie & Richesse"),
                I18n.getOrDefault("stats.cat.cognition", "🧠 Cognition & Information"),
                I18n.getOrDefault("stats.cat.ecology", "🌍 Écologie & Frontières Planétaires"),
                I18n.getOrDefault("stats.cat.cliodynamics", "⏳ Cliodynamique & Risques Systémiques"),
                I18n.getOrDefault("stats.cat.complexity", "⚙️ Complexité Systémique"),
                I18n.getOrDefault("stats.cat.performance", "💻 Performances Techniques")
            );
            categoryFilterCombo.getSelectionModel().select(catIdx >= 0 ? catIdx : 0);

            searchField.setPromptText(I18n.getOrDefault("stats.prompt.filter", "🔍 Filtrer une statistique (ex: Kardashev, Gini, Effondrement, NPK)..."));

            exportBtn.setText(I18n.getOrDefault("stats.btn.export_csv", "📥 Exporter les Données (CSV)"));
            exportBtn.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.export_csv", "Exporter l'historique complet des métriques sociétales, énergétiques et économiques au format CSV.")));

            // Refresh Metric Combo: Grouped by category with section headers
            String prevSelected = chartMetricCombo.getValue();
            chartMetricCombo.getItems().clear();

            Map<String, List<MetricCard>> cardsByCategory = new LinkedHashMap<>();
            for (MetricCard card : metricCards.values()) {
                cardsByCategory.computeIfAbsent(card.getCategory(), k -> new ArrayList<>()).add(card);
            }

            for (Map.Entry<String, List<MetricCard>> entry : cardsByCategory.entrySet()) {
                String catName = entry.getKey();
                List<MetricCard> cardsInCat = entry.getValue();
                cardsInCat.sort(Comparator.comparing(MetricCard::getTitle, String.CASE_INSENSITIVE_ORDER));

                chartMetricCombo.getItems().add("─── " + catName + " ───");
                for (MetricCard card : cardsInCat) {
                    chartMetricCombo.getItems().add(card.getTitle());
                }
            }

            if (prevSelected != null && chartMetricCombo.getItems().contains(prevSelected)) {
                chartMetricCombo.setValue(prevSelected);
            } else {
                for (String it : chartMetricCombo.getItems()) {
                    if (!it.startsWith("─── ")) {
                        chartMetricCombo.setValue(it);
                        break;
                    }
                }
            }

            filterMetrics();
            reloadChartData();
        } finally {
            isUpdatingTexts = false;
        }
    }

    private void registerMetricCards(Label inspectorTitle, Label inspectorText) {
        // Category 1: Énergie & Matière
        addCard("energyCaptured", "Énergie Captée (Puissance Globale)", "⚡ Énergie & Matière", "MW",
            "P_tot = ∑ (P_solaire + P_biomasse + P_géothermie). Puissance primaire brute extraite du milieu physique par la société (métabolisme, feu, biomasse, traction, hydraulique, fossiles).", inspectorTitle, inspectorText);
        addCard("resourceDepletion", "Déplétion des Ressources", "⚡ Énergie & Matière", "%",
            "D = (Stock_initial - Stock_actuel) / Stock_initial. Pourcentage cumulé de consommation des réserves minérales non-renouvelables.", inspectorTitle, inspectorText);
        addCard("energyPerCapita", "Énergie / Individu (Puissance)", "⚡ Énergie & Matière", "W/hab",
            "P_cap = P_tot / N_pop. Puissance énergétique continue disponible par habitant selon la loi de Leslie White (Culture = E × T. ~300 W au Paléolithique, ~10 kW en société industrielle).", inspectorTitle, inspectorText);
        addCard("foodPerCapita", "Stock Alimentaire / Habitant", "⚡ Énergie & Matière", "GJ/hab",
            "F_cap = Stock_Alimentaire / N_pop en Gigajoules. Stock d'énergie trophique disponible par individu (1 hab = 9 205 kJ/jour = 3,362 GJ/an).", inspectorTitle, inspectorText);
        addCard("eroiAlim", "EROI Alimentaire (Rendement Net)", "⚡ Énergie & Matière", "Ratio",
            "EROI = E_sortie / E_entrée. Ratio d'énergie acquise par rapport au coût énergétique de subsistance (3:1 à 15:1 au Paléolithique/Néolithique, < 1.0 en régime thermo-industriel inversé).", inspectorTitle, inspectorText);
        addCard("netSurplus", "Surplus Énergétique Net", "⚡ Énergie & Matière", "%",
            "Phi = 1 - 1/EROI. Fraction d'énergie disponible pour les structures non-agricoles, l'artisanat, les cités et les institutions complexes.", inspectorTitle, inspectorText);
        addCard("trophicMultiplier", "Empreinte Trophique", "⚡ Énergie & Matière", "x",
            "Multiplicateur de biomasse brute mobilisée par rapport à l'ingestion métabolique (2.25x en chasse-cueillette, 12.5x en pastoralisme, 20x en système mondialisé).", inspectorTitle, inspectorText);
        addCard("biomassMobilized", "Biomasse Mobilisée / Habitant", "⚡ Énergie & Matière", "kg/an",
            "Masse brute annuelle de biomasse mobilisée par individu (nourriture directe, alimentation du bétail de trait/pâturage et pertes).", inspectorTitle, inspectorText);
        addCard("pibMaterialFlow", "Flux Métabolique de Matière", "⚡ Énergie & Matière", "Mt/an", "Volume total de biomasse et de minerais déplacé par le métabolisme industriel.", inspectorTitle, inspectorText);
        addCard("biomassNatural", "Biomasse Naturelle", "⚡ Énergie & Matière", "GtC", "Stock total de carbone végétal et faunique sauvage préservé.", inspectorTitle, inspectorText);
        addCard("biomassDomesticated", "Biomasse Domestiquée", "⚡ Énergie & Matière", "GtC", "Biomasse totale des cultures agricoles et du bétail domestique.", inspectorTitle, inspectorText);
        addCard("potableWater", "Eau Douce & Aquifères", "⚡ Énergie & Matière", "10³ km³", "Réserves globales d'eau potable et nappe phréatique continentale.", inspectorTitle, inspectorText);
        addCard("remainingResources", "Ressources Restantes", "⚡ Énergie & Matière", "%", "Capital minéral et géologique non-extrait restant au sol.", inspectorTitle, inspectorText);
        addCard("entropyPollution", "Entropie & Pollution", "⚡ Énergie & Matière", "Idx", "Génération d'entropie thermodynamique et rejets polluants.", inspectorTitle, inspectorText);
        addCard("energyEroi", "EROI Énergétique Global", "⚡ Énergie & Matière", "Ratio", "Energy Return On Investment : Ratio moyen de rendement énergétique de l'ensemble des sources d'énergie exploitées.", inspectorTitle, inspectorText);
        addCard("occupiedTerritory", "Territoire de Subsistance & Emprise", "⚡ Énergie & Matière", "km²",
            "Surface écologique d'exploitation (Home Range de Binford, Kelly, Hassan). Modélise l'emprise diffuse des chasseurs-cueilleurs (10 à 100 km²/hab, soit 250 à 10 000 km² par bande de 25 personnes selon le biome) jusqu'à la concentration sédentaire agricole et urbaine.", inspectorTitle, inspectorText);

        // Category 2: Démographie & Santé
        addCard("population", "Population Humaine", "👥 Démographie & Santé", "hab", "Population totale d'habitants sur la planète.", inspectorTitle, inspectorText);
        addCard("fertilityRate", "Taux de Fertilité", "👥 Démographie & Santé", "enf/femme", "Nombre moyen d'enfants par femme en âge de procréer.", inspectorTitle, inspectorText);
        addCard("offspringPct", "Taux avec Descendance", "👥 Démographie & Santé", "%", "Proportion d'adultes ayant au moins un descendant.", inspectorTitle, inspectorText);
        addCard("ageFirstChild", "Âge au 1er Enfant", "👥 Démographie & Santé", "ans", "Âge moyen de la mère à la naissance du premier enfant.", inspectorTitle, inspectorText);
        addCard("immigrationRate", "Taux d'Immigration", "👥 Démographie & Santé", "‰", "Flux migratoires nets inter-régionaux.", inspectorTitle, inspectorText);
        addCard("lifeExpectancy", "Espérance de Vie", "👥 Démographie & Santé", "ans", "Espérance de vie moyenne à la naissance.", inspectorTitle, inspectorText);
        addCard("healthIndex", "Niveau de Santé Global", "👥 Démographie & Santé", "%", "Indice global de résistance sanitaire et d'immunité.", inspectorTitle, inspectorText);
        addCard("educationLevel", "Niveau d'Éducation", "👥 Démographie & Santé", "%", "Taux d'instruction et capital de savoir accumulé.", inspectorTitle, inspectorText);

        // Category 3: Société & Institutions
        addCard("happinessIndex", "Indice de Bonheur", "🏛️ Société & Institutions", "%", "Niveau de satisfaction globale et de bien-être mesuré.", inspectorTitle, inspectorText);
        addCard("conflictLevel", "Taux de Conflits", "🏛️ Société & Institutions", "%", "Intensité des frictions sociales, guerres et violence.", inspectorTitle, inspectorText);
        addCard("cityStates", "Nombre de Cités-États", "🏛️ Société & Institutions", "cités", "Pôles autonomes d'administration et institutions urbaines.", inspectorTitle, inspectorText);
        addCard("institutionalMaturity", "Naissance des Institutions", "🏛️ Société & Institutions", "Idx", "Maturité juridique, administrative et étatiste.", inspectorTitle, inspectorText);
        addCard("divisionLabor", "Division du Travail", "🏛️ Société & Institutions", "Idx", "Niveau de spécialisation des métiers et de différenciation sociale.", inspectorTitle, inspectorText);
        addCard("maxHierarchy", "Niveau Max Hiérarchique", "🏛️ Société & Institutions", "Niv", "Niveau d'empilement institutionnel (Tribu 1 ➔ Empire/Réseau 6).", inspectorTitle, inspectorText);
        addCard("largestCulture", "Plus Grande Unité Culturelle", "🏛️ Société & Institutions", "hab", "Taille de la plus vaste confédération culturelle/politique.", inspectorTitle, inspectorText);
        addCard("largestOrgComplexity", "Complexité Max Organisation", "🏛️ Société & Institutions", "Idx", "Complexité sociétale de la plus grande organisation/empire (Population × Tech × Capacités d'État × Hiérarchie).", inspectorTitle, inspectorText);
        addCard("largestOrgEntropy", "Entropie Max Civilisation", "⚡ Énergie & Matière", "J/K", "Génération d'entropie thermodynamique et rejet de chaleur résiduelle de la plus grande civilisation.", inspectorTitle, inspectorText);
        addCard("avgTechLevel", "Niveau Technologique Moyen", "🏛️ Société & Institutions", "Niv", "Moyenne globale du niveau d'avancement scientifique et technologique.", inspectorTitle, inspectorText);
        addCard("kardashevScale", "Échelle de Kardashev", "🏛️ Société & Institutions", "Type K", "K = (log10(P_watts) - 6) / 10. Niveau de maîtrise énergétique globale (Type 0.0 à 1.0+).", inspectorTitle, inspectorText);

        // Category 4: Économie & Richesse
        addCard("giniIndex", "Indice de Gini (Inégalité)", "💎 Économie & Richesse", "Coeff", "G = A / (A + B). Mesure de concentration des richesses (0 = égalité, 1 = inégalité absolue).", inspectorTitle, inspectorText);
        addCard("landGini", "Inégalité Foncière (Gini Sol)", "💎 Économie & Richesse", "Coeff", "Coefficient de concentration de la propriété des terres agricoles et des ressources du sol.", inspectorTitle, inspectorText);
        addCard("gdpTotal", "PIB Global (GDP)", "💎 Économie & Richesse", "G$", "Produit Intérieur Brut total converti en monnaie constante.", inspectorTitle, inspectorText);
        addCard("builtCapital", "Capital Bâti & Outillage", "💎 Économie & Richesse", "kg/hab", "Stock total d'infrastructures physiques et de machines.", inspectorTitle, inspectorText);
        addCard("eliteFormation", "Formation d'Élite", "💎 Économie & Richesse", "%", "Proportion de la population détenant les fonctions de commandement.", inspectorTitle, inspectorText);
        addCard("elderCapitalShare", "Possession Capital (Aînés)", "💎 Économie & Richesse", "%", "Part de la richesse foncière détenue par la tranche d'âge senior.", inspectorTitle, inspectorText);
        addCard("landRent", "Rente Foncière & Immobilière", "💎 Économie & Richesse", "Idx", "Valorisation de la rente du sol liée à la densité et aux infrastructures.", inspectorTitle, inspectorText);
        addCard("toolsCount", "Nombre d'Outils en Service", "💎 Économie & Richesse", "unités", "Quantité totale d'outils et équipements de production.", inspectorTitle, inspectorText);
        addCard("productsCount", "Variété de Produits", "💎 Économie & Richesse", "types", "Diversité des produits manufacturés au catalogue technique.", inspectorTitle, inspectorText);

        // Category 5: Cognition & Information
        addCard("shannonBandwidth", "Bande Passante Shannon", "🧠 Cognition & Information", "Gbps", "Débit maximal de transmission d'information à travers le réseau civilisationnel.", inspectorTitle, inspectorText);
        addCard("collectiveMemory", "Stock Mémoire Collective", "🧠 Cognition & Information", "TB", "Volume cumulé des connaissances, données et patrimoines écrits.", inspectorTitle, inspectorText);
        addCard("innovationDiffusion", "Vitesse de Diffusion Tech", "🧠 Cognition & Information", "km/an", "Vitesse de propagation spatiale des nouvelles technologies.", inspectorTitle, inspectorText);
        addCard("knowledgeDecay", "Taux d'Amnésie Historique", "🧠 Cognition & Information", "%/décade", "Vitesse de déperdition ou d'oubli du savoir lors des crises.", inspectorTitle, inspectorText);

        // Category 6: Écologie & Frontières Planétaires
        addCard("soilNPK", "Qualité NPK des Sols", "🌍 Écologie & Frontières Planétaires", "%", "Indice de fertilité et teneur en nutriments organiques des sols cultivés.", inspectorTitle, inspectorText);
        addCard("carryingCapacitySat", "Saturation Capacité Portante (N/K)", "🌍 Écologie & Frontières Planétaires", "Ratio", "Ratio démographique global rapporté à la biocapacité soutenable (N/K). Seuil critique à 1.0 (Overshoot malthusien).", inspectorTitle, inspectorText);
        addCard("planetaryOvershoot", "Dépassement Planétaire (Overshoot)", "🌍 Écologie & Frontières Planétaires", "x", "Facteur de dépassement des 9 frontières planétaires du Stockholm Resilience Centre (Rockström et al.).", inspectorTitle, inspectorText);
        addCard("carbonFootprint", "Empreinte Carbone", "🌍 Écologie & Frontières Planétaires", "GtCO₂", "Émissions annuelles de gaz à effet de serre et carbone fossile.", inspectorTitle, inspectorText);
        addCard("wildBiodiversity", "Biodiversité Sauvage", "🌍 Écologie & Frontières Planétaires", "%", "Part de la biomasse faunique et florale sauvage préservée.", inspectorTitle, inspectorText);
        addCard("wetBulbSafety", "Marge Sécurité Bulbe Humide", "🌍 Écologie & Frontières Planétaires", "°C", "Écart de température avec le seuil létal de bulbe humide (35°C).", inspectorTitle, inspectorText);

        // Category 7: Cliodynamique & Risques Systémiques
        addCard("turchinPsi", "Indice de Stress Politique (PSI)", "⏳ Cliodynamique & Risques Systémiques", "Idx", "PSI = W × E × S. Indice synthétique de Peter Turchin modélisant la détresse populaire (W), la surproduction des élites (E) et la faiblesse de l'État (S).", inspectorTitle, inspectorText);
        addCard("asabiyyah", "Cohésion Asabiyyah", "⏳ Cliodynamique & Risques Systémiques", "%", "Indice de solidarité de groupe et de capacité d'action collective d'Ibn Khaldoun (1377).", inspectorTitle, inspectorText);
        addCard("eliteOverproduction", "Surproduction Élitaire (Turchin)", "⏳ Cliodynamique & Risques Systémiques", "Idx", "PSI = (Élites_aspirantes / Postes_disponibles) × Inégalité. Ratio de compétition pour le pouvoir (Indice PSI de Turchin).", inspectorTitle, inspectorText);
        addCard("fiscalStress", "Pression & Stress Fiscal", "⏳ Cliodynamique & Risques Systémiques", "%", "Stress financier et charge de maintien des institutions publiques.", inspectorTitle, inspectorText);
        addCard("geopoliticalTension", "Tension Géopolitique", "⏳ Cliodynamique & Risques Systémiques", "%", "Friction diplomatique et risque d'escalade guerrière multipolaire.", inspectorTitle, inspectorText);
        addCard("collapseVulnerability", "Risque d'Effondrement", "⏳ Cliodynamique & Risques Systémiques", "%", "Probabilité d'effondrement systémique ou de boucle d'entropie.", inspectorTitle, inspectorText);

        // Category 8: Complexité Systémique
        addCard("systemComplexity", "Complexité Systémique", "⚙️ Complexité Systémique", "Idx", "Indice d'interconnexion des rouages économiques et sociaux.", inspectorTitle, inspectorText);
        addCard("reconstructionCapability", "Capacité à Reconstruire", "⚙️ Complexité Systémique", "%", "Résilience et capacité à rebâtir la civilisation à partir de zéro.", inspectorTitle, inspectorText);
        addCard("systemInterdependence", "Interdépendance (Rouages)", "⚙️ Complexité Systémique", "%", "Fragilité systémique liée à l'interdépendance des chaînes logistiques.", inspectorTitle, inspectorText);

        // Category 9: Performances Engine
        addCard("engineTPS", "Fréquence de Calcul (Pas/s)", "💻 Performances Techniques", "pas/s", "Fréquence réelle de calcul du moteur de simulation (pas par seconde).", inspectorTitle, inspectorText);
        addCard("ramMemory", "Utilisation Mémoire RAM", "💻 Performances Techniques", "MB", "Consommation mémoire vive du moteur.", inspectorTitle, inspectorText);
        addCard("cellCount", "Cellules Hexagonales H3", "💻 Performances Techniques", "hex", "Nombre total de mailles hexagonales chargées en mémoire.", inspectorTitle, inspectorText);
    }

    private void addCard(String key, String title, String category, String unit, String tooltip, Label inspectorTitle, Label inspectorText) {
        MetricCard card = new MetricCard(key, title, category, unit, tooltip);
        card.setOnMouseEntered(e -> {
            inspectorTitle.setText("🔎 " + card.getTitle() + " [" + card.getCategory() + "]");
            inspectorText.setText(card.getTooltipText());
        });
        card.setOnMouseClicked(e -> {
            inspectorTitle.setText("🔎 " + card.getTitle() + " [" + card.getCategory() + "]");
            inspectorText.setText(card.getTooltipText());
            if (chartMetricCombo != null) {
                chartMetricCombo.setValue(card.getTitle());
                resetChartSeries();
            }
            if (onDisplayModeRequested != null) {
                onDisplayModeRequested.accept(DisplayMode.fromMetricId(key));
            }
        });
        metricCards.put(key, card);
    }

    private void filterMetrics() {
        String catFilter = categoryFilterCombo.getValue();
        String searchText = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";

        metricsContainer.getChildren().clear();
        String currentCat = "";

        for (MetricCard card : metricCards.values()) {
            boolean matchesCat = catFilter == null || catFilter.contains("Toutes") || catFilter.contains("All") || card.getCategory().equalsIgnoreCase(catFilter);
            boolean matchesSearch = searchText.isEmpty() || card.getTitle().toLowerCase().contains(searchText) || card.getCategory().toLowerCase().contains(searchText);

            if (matchesCat && matchesSearch) {
                if (!card.getCategory().equals(currentCat)) {
                    currentCat = card.getCategory();
                    Label catHeader = new Label(currentCat.toUpperCase());
                    catHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #ffd700; -fx-padding: 8 0 2 0;");
                    metricsContainer.getChildren().add(catHeader);
                }
                metricsContainer.getChildren().add(card);
            }
        }
    }

    public void reset() {
        chartSeries.getData().clear();
        barSeries.getData().clear();
        tickCounter = 0;
        if (xAxis != null) {
            xAxis.setAutoRanging(true);
        }
    }

    public void resetChartSeries() {
        reloadChartData();
    }

    private void reloadChartData() {
        chartSeries.getData().clear();
        if (engine == null || engine.getCells() == null || engine.getCells().isEmpty()) {
            return;
        }
        if (engine.getHistoryManager() == null || engine.getHistoryManager().getHistory() == null) {
            return;
        }
        List<org.ether.society.analytics.HistorySnapshot> snapshots = engine.getHistoryManager().getHistory().getSnapshots();
        if (snapshots.isEmpty()) {
            return;
        }

        String selectedMetric = chartMetricCombo != null ? chartMetricCombo.getValue() : "Population Humaine";
        if (selectedMetric == null || selectedMetric.startsWith("─── ")) {
            selectedMetric = "Population Humaine";
        }
        double currentTime = engine.getTimeManager().getCurrentYear() + (engine.getTimeManager().getCurrentMonth() / 12.0);
        double minAllowed = getMinAllowedYear();

        double minTime = (currentWindowYears > 0) ? Math.max(minAllowed, currentTime - currentWindowYears) : minAllowed;

        List<XYChart.Data<Number, Number>> points = new ArrayList<>();
        for (org.ether.society.analytics.HistorySnapshot snap : snapshots) {
            double snapTime = snap.year() + (snap.month() / 12.0);
            if (snapTime >= minTime - 0.0001 && snapTime <= currentTime + 0.0001) {
                double val = snap.getMetricValue(selectedMetric);
                points.add(new XYChart.Data<>(snapTime, val));
            }
        }

        if (points.size() > 600) {
            int step = (int) Math.ceil(points.size() / 500.0);
            List<XYChart.Data<Number, Number>> downsampled = new ArrayList<>();
            for (int i = 0; i < points.size(); i += step) {
                downsampled.add(points.get(i));
            }
            if (!downsampled.contains(points.get(points.size() - 1))) {
                downsampled.add(points.get(points.size() - 1));
            }
            chartSeries.getData().addAll(downsampled);
        } else {
            chartSeries.getData().addAll(points);
        }

        if (currentWindowYears > 0) {
            xAxis.setAutoRanging(false);
            xAxis.setLowerBound(minTime);
            xAxis.setUpperBound(Math.max(minTime + 1.0, currentTime));
        } else {
            xAxis.setAutoRanging(true);
        }
    }

    private final java.util.concurrent.atomic.AtomicLong lastStatsUiUpdateNanos = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicBoolean statsUpdatePending = new java.util.concurrent.atomic.AtomicBoolean(false);

    public void update() {
        if (engine == null || !isLiveCollectionActive) return;
        if (engine.getCells() == null || engine.getCells().isEmpty()) return;

        tickCounter++;
        if (tickCounter % samplingIntervalTicks != 0) return;

        long nowNanos = System.nanoTime();
        if (nowNanos - lastStatsUiUpdateNanos.get() < 100_000_000L) { // Throttle to max 10 UI updates per second
            return;
        }
        if (!statsUpdatePending.compareAndSet(false, true)) {
            return;
        }
        lastStatsUiUpdateNanos.set(nowNanos);

        // Extract values from engine
        long pop = engine.getTotalPopulation();
        float bio = engine.getTotalBiomassNatural();
        double food = engine.getTotalFood();
        float gini = engine.getCurrentGini();
        float gdp = engine.getCurrentGDP();
        float fert = engine.getCurrentFertility();
        float life = engine.getCurrentLifeExpectancy();
        float tech = engine.getAverageTechnology();

        double energyCap = engine.getEnergyCaptured();
        double resDep = engine.getResourceDepletionRate();
        double energyPerCap = engine.getEnergyPerCapita();
        double foodPerCap = engine.getFoodPerCapita();
        double eroiAlim = engine.getEroiAlimentaire();
        double netSurplus = engine.getNetSurplusFraction() * 100.0;
        double trophicMul = engine.getTrophicMultiplier();
        double bioMobilized = engine.getBiomassMobilizedPerCapitaKg();
        double bioDom = engine.getBiomassDomesticated();
        double water = engine.getPotableWaterTotal();
        double remRes = engine.getRemainingResourcesRatio();
        double entropy = engine.getSystemicEntropy();
        double territory = engine.getOccupiedTerritoryArea();

        double offspring = engine.getOffspringPercentage();
        double ageFirstChild = engine.getAgeAtFirstChild();
        double immigration = engine.getImmigrationRate();
        double education = engine.getEducationLevel();

        double happiness = engine.getHappinessIndex();
        double conflict = engine.getConflictLevel();
        int cityStates = engine.getCityStatesCount();
        double instMaturity = engine.getInstitutionalMaturity();
        double divLabor = engine.getDivisionOfLaborIndex();
        int maxHier = engine.getMaxHierarchyLevel();
        long largestCult = engine.getLargestCulturalUnitSize();
        double kardashev = engine.getKardashevScale();

        double builtCap = engine.getBuiltCapitalTotal();
        double eliteForm = engine.getEliteFormationRatio();
        double elderCap = engine.getElderCapitalShare();
        double landRent = engine.getLandRentIndex();
        long tools = engine.getToolsCount();
        long products = engine.getProductsCount();

        double sysComp = engine.getSystemComplexityIndex();
        double reconCap = engine.getReconstructionCapabilityIndex();
        double sysInter = engine.getSystemInterdependenceIndex();

        double shannonBw = engine.getShannonBandwidth();
        double memoryStock = engine.getCollectiveMemoryStock();
        double innovSpeed = engine.getInnovationDiffusionSpeed();
        double knowDecay = engine.getKnowledgeDecayRate();

        double soilNPK = engine.getSoilNPKQuality();
        double carbonFp = engine.getCarbonFootprint();
        double wildBio = engine.getWildBiodiversityIndex();
        double wetBulb = engine.getWetBulbSafetyMargin();

        double eliteOver = engine.getEliteOverproductionIndex();
        double fiscalStress = engine.getFiscalStressIndex();
        double geoTension = engine.getGeopoliticalTension();
        double collapseVuln = engine.getCollapseVulnerability();

        Runtime rt = Runtime.getRuntime();
        long usedMem = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        double tps = engine.getCurrentTPS();
        int totalCells = engine.getCells() != null ? engine.getCells().size() : 0;
        int year = engine.getTimeManager().getCurrentYear();

        Platform.runLater(() -> {
            try {
                spatialHeatmapPanel.setHistoryManager(engine.getHistoryManager());
                spatialHeatmapPanel.updateCells(engine.getCells());
                spatialHeatmapPanel.setDateLabel(String.format("📅 An %d", year));

                // Update cards with raw numerical values for sparkline trend analysis
                setCardVal("energyCaptured", String.format("%,.1f", energyCap), energyCap);
                setCardVal("resourceDepletion", String.format("%.1f", resDep), resDep);
                setCardVal("energyPerCapita", String.format("%.1f", energyPerCap), energyPerCap);
                setCardVal("foodPerCapita", String.format("%.2f", foodPerCap), foodPerCap);
                setCardVal("eroiAlim", String.format("%.2f : 1", eroiAlim), eroiAlim);
                setCardVal("netSurplus", String.format("%.1f%%", netSurplus), netSurplus);
                setCardVal("trophicMultiplier", String.format("%.2fx", trophicMul), trophicMul);
                setCardVal("biomassMobilized", String.format("%,.0f", bioMobilized), bioMobilized);
                setCardVal("pibMaterialFlow", String.format("%.1f", resDep * 5.2), resDep * 5.2);
                setCardVal("biomassNatural", String.format("%,.0f", bio), bio);
                setCardVal("biomassDomesticated", String.format("%,.1f", bioDom), bioDom);
                setCardVal("potableWater", String.format("%,.0f", water), water);
                setCardVal("remainingResources", String.format("%.1f", remRes), remRes);
                setCardVal("entropyPollution", String.format("%.1f", entropy), entropy);
                setCardVal("occupiedTerritory", String.format("%,.0f", territory), territory);

                setCardVal("population", String.format("%,d", pop), (double) pop);
                setCardVal("fertilityRate", String.format("%.1f", fert), (double) fert);
                setCardVal("offspringPct", String.format("%.1f", offspring), offspring);
                setCardVal("ageFirstChild", String.format("%.1f", ageFirstChild), ageFirstChild);
                setCardVal("immigrationRate", String.format("%.1f", immigration), immigration);
                setCardVal("lifeExpectancy", String.format("%.1f", life), (double) life);
                setCardVal("healthIndex", String.format("%.1f", Math.min(100.0, life * 1.1)), Math.min(100.0, life * 1.1));
                setCardVal("educationLevel", String.format("%.1f", education), education);

                double largestOrgComp = engine.getLargestOrganizationComplexity();
                double largestOrgEnt = engine.getLargestOrganizationEntropy();

                setCardVal("happinessIndex", String.format("%.1f", happiness), happiness);
                setCardVal("conflictLevel", String.format("%.1f", conflict), conflict);
                setCardVal("cityStates", String.format("%d", cityStates), (double) cityStates);
                setCardVal("institutionalMaturity", String.format("%.1f", instMaturity), instMaturity);
                setCardVal("divisionLabor", String.format("%.1f", divLabor), divLabor);
                setCardVal("maxHierarchy", String.format("Niv %d", maxHier), (double) maxHier);
                setCardVal("largestCulture", String.format("%,d", largestCult), (double) largestCult);
                setCardVal("largestOrgComplexity", String.format("%,.0f", largestOrgComp), largestOrgComp);
                setCardVal("largestOrgEntropy", String.format("%,.1f", largestOrgEnt), largestOrgEnt);
                setCardVal("avgTechLevel", String.format("%.2f", tech), (double) tech);
                setCardVal("kardashevScale", String.format("%.2f", kardashev), kardashev);

                setCardVal("giniIndex", String.format("%.2f", gini), (double) gini);
                setCardVal("gdpTotal", String.format("%,.0f", gdp), (double) gdp);
                setCardVal("builtCapital", String.format("%,.0f", builtCap / Math.max(1, pop)), builtCap / Math.max(1, pop));
                setCardVal("eliteFormation", String.format("%.1f", eliteForm), eliteForm);
                setCardVal("elderCapitalShare", String.format("%.1f", elderCap), elderCap);
                setCardVal("landRent", String.format("%.1f", landRent), landRent);
                setCardVal("toolsCount", String.format("%,d", tools), (double) tools);
                setCardVal("productsCount", String.format("%,d", products), (double) products);

                setCardVal("shannonBandwidth", String.format("%.1f", shannonBw), shannonBw);
                setCardVal("collectiveMemory", String.format("%,.0f", memoryStock), memoryStock);
                setCardVal("innovationDiffusion", String.format("%.1f", innovSpeed), innovSpeed);
                setCardVal("knowledgeDecay", String.format("%.1f", knowDecay), knowDecay);

                setCardVal("soilNPK", String.format("%.1f", soilNPK), soilNPK);
                setCardVal("carbonFootprint", String.format("%.2f", carbonFp), carbonFp);
                setCardVal("wildBiodiversity", String.format("%.1f", wildBio), wildBio);
                setCardVal("wetBulbSafety", String.format("%.1f", wetBulb), wetBulb);

                setCardVal("eliteOverproduction", String.format("%.2f", eliteOver), eliteOver);
                setCardVal("fiscalStress", String.format("%.1f", fiscalStress), fiscalStress);
                setCardVal("geopoliticalTension", String.format("%.1f", geoTension), geoTension);
                setCardVal("collapseVulnerability", String.format("%.1f", collapseVuln), collapseVuln);

                setCardVal("systemComplexity", String.format("%.1f", sysComp), sysComp);
                setCardVal("reconstructionCapability", String.format("%.1f", reconCap), reconCap);
                setCardVal("systemInterdependence", String.format("%.1f", sysInter), sysInter);

                setCardVal("engineTPS", String.format("%.1f", tps), tps);
                setCardVal("ramMemory", String.format("%d", usedMem), (double) usedMem);
                setCardVal("cellCount", String.format("%,d", totalCells), (double) totalCells);

                // Update Time Series Chart
                String selectedMetric = chartMetricCombo.getValue();
                if (selectedMetric == null || selectedMetric.startsWith("─── ")) {
                    selectedMetric = "Population Humaine";
                }
                double yVal = extractMetricValue(selectedMetric, energyCap, resDep, energyPerCap, foodPerCap, bio, bioDom,
                        water, remRes, entropy, territory, pop, fert, offspring, ageFirstChild, immigration, life,
                        education, happiness, conflict, cityStates, instMaturity, divLabor, maxHier, largestCult,
                        largestOrgComp, largestOrgEnt, tech, kardashev, gini, gdp, builtCap, eliteForm, elderCap,
                        landRent, tools, products, shannonBw, memoryStock, innovSpeed, knowDecay, soilNPK, carbonFp,
                        wildBio, wetBulb, eliteOver, fiscalStress, geoTension, collapseVuln, sysComp, reconCap,
                        sysInter, tps, usedMem, totalCells);

                double currentTime = year + (engine.getTimeManager().getCurrentMonth() / 12.0);

                // If user rewound/stepped back in time, prune future data points from the active chart
                if (!chartSeries.getData().isEmpty()) {
                    chartSeries.getData().removeIf(data -> data.getXValue().doubleValue() > currentTime + 0.0001);
                }

                if (chartSeries.getData().isEmpty() || Math.abs(chartSeries.getData().get(chartSeries.getData().size() - 1).getXValue().doubleValue() - currentTime) >= 0.001) {
                    chartSeries.getData().add(new XYChart.Data<>(currentTime, yVal));

                    if (currentWindowYears > 0) {
                        double minTime = Math.max(getMinAllowedYear(), currentTime - currentWindowYears);
                        chartSeries.getData().removeIf(data -> data.getXValue().doubleValue() < minTime - 0.0001);
                        xAxis.setAutoRanging(false);
                        xAxis.setLowerBound(minTime);
                        xAxis.setUpperBound(Math.max(minTime + 1.0, currentTime));
                    }

                    // Update Age Pyramid Bar Chart (7 Cohorts)
                    int[] pyramid = engine.getAgePyramid();
                    barSeries.getData().clear();
                    if (pyramid != null && pyramid.length >= 7) {
                        barSeries.getData().add(new XYChart.Data<>("0-14 ans", pyramid[0]));
                        barSeries.getData().add(new XYChart.Data<>("15-24 ans", pyramid[1]));
                        barSeries.getData().add(new XYChart.Data<>("25-39 ans", pyramid[2]));
                        barSeries.getData().add(new XYChart.Data<>("40-54 ans", pyramid[3]));
                        barSeries.getData().add(new XYChart.Data<>("55-69 ans", pyramid[4]));
                        barSeries.getData().add(new XYChart.Data<>("70-84 ans", pyramid[5]));
                        barSeries.getData().add(new XYChart.Data<>("85+ ans", pyramid[6]));
                    } else if (pyramid != null && pyramid.length >= 3) {
                        barSeries.getData().add(new XYChart.Data<>("Jeunes (<15ans)", pyramid[0]));
                        barSeries.getData().add(new XYChart.Data<>("Adultes (15-60ans)", pyramid[1]));
                        barSeries.getData().add(new XYChart.Data<>("Aînés (>60ans)", pyramid[2]));
                    }

                    // Update Variance & Distribution Panel
                    variancePanel.updateData(engine.getCells());
                }
            } finally {
                statsUpdatePending.set(false);
            }
        });
    }

    private double extractMetricValue(String selectedMetric, double energyCap, double resDep, double energyPerCap,
            double foodPerCap, float bio, double bioDom, double water, double remRes, double entropy, double territory,
            long pop, float fert, double offspring, double ageFirstChild, double immigration, float life,
            double education, double happiness, double conflict, int cityStates, double instMaturity, double divLabor,
            int maxHier, long largestCult, double largestOrgComp, double largestOrgEnt, float tech, double kardashev,
            float gini, float gdp, double builtCap, double eliteForm, double elderCap, double landRent, long tools,
            long products, double shannonBw, double memoryStock, double innovSpeed, double knowDecay, double soilNPK,
            double carbonFp, double wildBio, double wetBulb, double eliteOver, double fiscalStress, double geoTension,
            double collapseVuln, double sysComp, double reconCap, double sysInter, double tps, long usedMem, int totalCells) {
        if (selectedMetric == null) return pop;
        String m = selectedMetric.toLowerCase();

        if (m.contains("captée") || m.contains("energy captured")) return energyCap;
        if (m.contains("déplétion") || m.contains("depletion")) return resDep;
        if (m.contains("énergie /") || m.contains("energy /")) return energyPerCap;
        if (m.contains("nourriture /") || m.contains("food /")) return foodPerCap;
        if (m.contains("matière") || m.contains("material")) return resDep * 5.2;
        if (m.contains("biomasse naturelle") || m.contains("natural biomass")) return bio;
        if (m.contains("biomasse domestiquée") || m.contains("domesticated")) return bioDom;
        if (m.contains("eau douce") || m.contains("water")) return water;
        if (m.contains("ressources restantes") || m.contains("remaining resources")) return remRes;
        if (m.contains("entropie") || m.contains("pollution") || m.contains("entropy")) return entropy;
        if (m.contains("territoire") || m.contains("territory")) return territory;

        if (m.contains("survie") || m.contains("survival")) return engine.getPopulationSurvivalRate();
        if (m.contains("cohésion") || m.contains("asabiyyah")) return engine.getAverageAsabiyyah();
        if (m.contains("fertilité") || m.contains("fertility")) return fert;
        if (m.contains("descendance") || m.contains("offspring")) return offspring;
        if (m.contains("1er enfant") || m.contains("first child")) return ageFirstChild;
        if (m.contains("immigration")) return immigration;
        if (m.contains("espérance de vie") || m.contains("life expectancy")) return life;
        if (m.contains("santé") || m.contains("health")) return Math.min(100.0, life * 1.1);
        if (m.contains("éducation") || m.contains("education")) return education;

        if (m.contains("bonheur") || m.contains("happiness")) return happiness;
        if (m.contains("conflit") || m.contains("conflict")) return conflict;
        if (m.contains("cités-états") || m.contains("city-states")) return cityStates;
        if (m.contains("naissance des institutions") || m.contains("maturity")) return instMaturity;
        if (m.contains("division du travail") || m.contains("division of labor")) return divLabor;
        if (m.contains("hiérarchique") || m.contains("hierarchy")) return maxHier;
        if (m.contains("unité culturelle") || m.contains("cultural unit")) return largestCult;
        if (m.contains("complexité max organisation") || m.contains("organization complexity")) return largestOrgComp;
        if (m.contains("entropie max civilisation") || m.contains("civilization entropy")) return largestOrgEnt;
        if (m.contains("technologique") || m.contains("technology")) return tech;
        if (m.contains("kardashev")) return kardashev;

        if (m.contains("gini")) return gini;
        if (m.contains("pib") || m.contains("gdp")) return gdp;
        if (m.contains("capital bâti") || m.contains("built capital")) return builtCap / Math.max(1, pop);
        if (m.contains("élite") && !m.contains("surproduction")) return eliteForm;
        if (m.contains("aînés") || m.contains("elder")) return elderCap;
        if (m.contains("rente") || m.contains("rent")) return landRent;
        if (m.contains("outils") || m.contains("tools")) return tools;
        if (m.contains("produits") || m.contains("products")) return products;

        if (m.contains("shannon")) return shannonBw;
        if (m.contains("mémoire") || m.contains("memory")) return memoryStock;
        if (m.contains("diffusion")) return innovSpeed;
        if (m.contains("amnésie") || m.contains("decay")) return knowDecay;

        if (m.contains("npk") || m.contains("sol")) return soilNPK;
        if (m.contains("carbone") || m.contains("carbon")) return carbonFp;
        if (m.contains("biodiversité") || m.contains("biodiversity")) return wildBio;
        if (m.contains("bulbe humide") || m.contains("wet bulb")) return wetBulb;

        if (m.contains("surproduction") || m.contains("turchin")) return eliteOver;
        if (m.contains("fiscal")) return fiscalStress;
        if (m.contains("tension") || m.contains("geopolitical")) return geoTension;
        if (m.contains("effondrement") || m.contains("collapse")) return collapseVuln;

        if (m.contains("complexité systémique") || m.contains("system complexity")) return sysComp;
        if (m.contains("reconstruire") || m.contains("reconstruction")) return reconCap;
        if (m.contains("interdépendance") || m.contains("interdependence")) return sysInter;

        if (m.contains("tps") || m.contains("fréquence") || m.contains("pas/s") || m.contains("ticks/s")) return tps;
        if (m.contains("ram") || m.contains("mémoire")) return usedMem;
        if (m.contains("cellules") || m.contains("cells") || m.contains("hex")) return totalCells;

        return pop;
    }

    private void setCardVal(String key, String val, double ratio) {
        MetricCard card = metricCards.get(key);
        if (card != null) {
            card.updateValue(val, ratio);
        }
    }

    private void exportCsv() {
        if (engine == null) return;

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle(I18n.getOrDefault("stats.title.export_dialog", "Exporter les Statistiques Cliodynamiques (CSV / Excel)"));
        fileChooser.setInitialFileName("ether_statistiques_cliodynamiques.csv");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichiers CSV (*.csv)", "*.csv"));

        java.io.File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file == null) return;

        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            writer.println("Annee;Mois;Population;Biomasse;Energie_Captee;Kardashev;Gini;PIB;Bonheur;Conflits;Esperance_Vie;Fecondite;Division_Travail;Complexite");

            int y = engine.getTimeManager().getCurrentYear();
            int m = engine.getTimeManager().getCurrentMonth();
            writer.println(String.format(java.util.Locale.US, "%d;%d;%d;%.2f;%.2f;%.3f;%.4f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f",
                y, m, engine.getTotalPopulation(), engine.getTotalBiomassNatural(), engine.getEnergyCaptured(),
                engine.getKardashevScale(), engine.getCurrentGini(), engine.getCurrentGDP(), engine.getHappinessIndex(),
                engine.getConflictLevel(), engine.getCurrentLifeExpectancy(), engine.getCurrentFertility(),
                engine.getDivisionOfLaborIndex(), engine.getSystemComplexityIndex()));

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(I18n.getOrDefault("stats.title.export_success", "Exportation Réussie"));
            alert.setContentText("Données statistiques exportées avec succès dans :\n" + file.getAbsolutePath());
            alert.showAndWait();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur d'Exportation");
            alert.setContentText("Impossible d'exporter les statistiques : " + ex.getMessage());
            alert.showAndWait();
        }
    }
}


