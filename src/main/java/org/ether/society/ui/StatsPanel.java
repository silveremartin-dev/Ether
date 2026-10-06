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
 * @version 1.0.0-beta.2
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
    /* Internal state variable for is live collection active (boolean). */
    private boolean isLiveCollectionActive = true;
    /* Internal state variable for sampling interval ticks (int). */
    private int samplingIntervalTicks = 1;
    /* Internal state variable for tick counter (long). */
    private long tickCounter = 0;
    private java.util.function.Consumer<DisplayMode> onDisplayModeRequested;

    private final Label metricsHeaderLabel;
    private final Label metricInspectorTitle;
    private final Label metricInspectorText;

    // Time window in years (-1 for all history)
    /* Internal state variable for current window years (double). */
    private double currentWindowYears = 10.0;
    /* Internal state variable for is updating texts (boolean). */
    private boolean isUpdatingTexts = false;

    /*
     * Set on display mode requested.
     * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
     *
     * @param listener the listener parameter (java.util.function.Consumer&lt;DisplayMode&gt;)
     */
    public void setOnDisplayModeRequested(java.util.function.Consumer<DisplayMode> listener) {
        this.onDisplayModeRequested = listener;
    }

    /*
     * Set selected metric by display mode.
     * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
     *
     * @param mode the mode parameter (DisplayMode)
     */
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

    /*
     * Minimalist financial sparkline chart canvas (70x20 px) showing recent trend (green for up, red for down, cyan for stable).
     */
    public static class SparklineCanvas extends javafx.scene.canvas.Canvas {
        /* Internal state variable for history (double[]). */
        private final double[] history = new double[24];
        /* Internal state variable for count (int). */
        private int count = 0;
        /* Internal state variable for head (int). */
        private int head = 0;

        /*
         * Sparkline canvas.
         * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
         *
         * @return the resulting computation or state reference
         */
        public SparklineCanvas() {
            super(70, 20);
            drawEmpty();
        }

        /*
         * Add value.
         * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
         *
         * @param val the val parameter (double)
         */
        public void addValue(double val) {
            if (Double.isNaN(val) || Double.isInfinite(val)) return;
            history[head] = val;
            head = (head + 1) % history.length;
            if (count < history.length) count++;
            redraw();
        }

        // Helper subroutine: draw empty - internal state computation & bounds checking
        private void drawEmpty() {
            var gc = getGraphicsContext2D();
            gc.clearRect(0, 0, getWidth(), getHeight());
            gc.setStroke(Color.rgb(100, 116, 139, 0.4));
            gc.setLineWidth(1.0);
            gc.strokeLine(2, getHeight() / 2, getWidth() - 2, getHeight() / 2);
        }

        // Helper subroutine: redraw - internal state computation & bounds checking
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

    /* Class to hold UI elements for a single metric card */
    private static class MetricCard extends HBox {
        /* Internal state variable for key (String). */
        private final String key;
        /* Internal state variable for title (String). */
        private String title;
        /* Internal state variable for category (String). */
        private String category;
        private final Label titleLabel;
        private final Label valueLabel;
        private final SparklineCanvas sparkline;
        /* Internal state variable for unit (String). */
        private String unit;
        /* Internal state variable for tooltip text (String). */
        private String tooltipText;
        /* Internal state variable for last val (double). */
        private double lastVal = 0.0;

        /*
         * Metric card.
         * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
         *
         * @param key the key parameter (String)
         * @param title the title parameter (String)
         * @param category the category parameter (String)
         * @param unit the unit parameter (String)
         * @param tooltipText the tooltip text parameter (String)
         * @return the resulting computation or state reference
         */
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

        /*
         * Update texts.
         * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
         *
         * @param title the title parameter (String)
         * @param category the category parameter (String)
         * @param unit the unit parameter (String)
         * @param tooltipText the tooltip text parameter (String)
         */
        public void updateTexts(String title, String category, String unit, String tooltipText) {
            // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
            this.title = title;
            this.category = category;
            this.unit = unit;
            this.tooltipText = tooltipText;
            titleLabel.setText(title + ":");
            if (tooltipText != null && !tooltipText.isBlank()) {
                Tooltip.install(this, new Tooltip(tooltipText));
            }
        }

        /*
         * Update value.
         * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
         *
         * @param displayValue the display value parameter (String)
         * @param rawNumericValue the raw numeric value parameter (double)
         */
        public void updateValue(String displayValue, double rawNumericValue) {
            // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
            this.lastVal = rawNumericValue;
            valueLabel.setText(displayValue + (unit.isEmpty() ? "" : " " + unit));
            sparkline.addValue(rawNumericValue);
        }

        /*
         * Get key.
         * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
         *
         * @return the resulting computation or state reference
         */
        public String getKey() { return key; }
        /*
         * Get category.
         * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
         *
         * @return the resulting computation or state reference
         */
        public String getCategory() { return category; }
        /*
         * Get title.
         * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
         *
         * @return the resulting computation or state reference
         */
        public String getTitle() { return title; }
        /*
         * Get tooltip text.
         * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
         *
         * @return the resulting computation or state reference
         */
        public String getTooltipText() { return tooltipText; }
    }

    /*
     * Stats panel.
     * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
     *
     * @param engine the engine parameter (H3SimulationEngine)
     */
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
            /*
             * Update item.
             * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
             *
             * @param item the item parameter (String)
             * @param empty the empty parameter (boolean)
             */
            protected void updateItem(String item, boolean empty) {
                // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
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
            /*
             * Update item.
             * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
             *
             * @param item the item parameter (String)
             * @param empty the empty parameter (boolean)
             */
            protected void updateItem(String item, boolean empty) {
                // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
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
            /*
             * To string.
             * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
             *
             * @param object the object parameter (Number)
             * @return the resulting computation or state reference
             */
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
            /*
             * From string.
             * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
             *
             * @param string the string parameter (String)
             * @return the resulting computation or state reference
             */
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

        // Navigation guidance banner
        Label chartNavHelpLabel = new Label(I18n.getOrDefault("stats.chart.nav_help", "💡 Navigation : Molette pour zoomer sur le curseur | Glisser (clic) pour défiler temporellement | Double-clic pour réinitialiser"));
        chartNavHelpLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8; -fx-padding: 3 6; -fx-background-color: rgba(56, 189, 248, 0.08); -fx-background-radius: 4;");
        chartNavHelpLabel.setWrapText(true);

        // Clamped Interactive Mouse Zoom (centered at mouse cursor) & Pan (Direct Drag)
        final double[] dragAnchor = new double[2];
        lineChart.setOnMouseEntered(e -> lineChart.setCursor(javafx.scene.Cursor.HAND));
        lineChart.setOnMouseExited(e -> lineChart.setCursor(javafx.scene.Cursor.DEFAULT));
        lineChart.setOnMousePressed(e -> {
            dragAnchor[0] = e.getX();
            dragAnchor[1] = e.getY();
            lineChart.setCursor(javafx.scene.Cursor.CLOSED_HAND);
        });
        lineChart.setOnMouseReleased(e -> lineChart.setCursor(javafx.scene.Cursor.HAND));
        lineChart.setOnMouseDragged(e -> {
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
            e.consume();
            if (xAxis.isAutoRanging()) xAxis.setAutoRanging(false);
            double minYear = getMinAllowedYear();
            double maxYear = getMaxAllowedYear();
            double zoomFactor = e.getDeltaY() > 0 ? 0.85 : 1.15;

            double curLower = xAxis.getLowerBound();
            double curUpper = xAxis.getUpperBound();
            double curSpan = Math.max(0.1, curUpper - curLower);

            // Compute value under cursor
            javafx.geometry.Point2D localPoint = xAxis.sceneToLocal(e.getSceneX(), e.getSceneY());
            double mouseVal;
            if (localPoint != null && xAxis.getWidth() > 0) {
                double fractionOnAxis = Math.clamp(localPoint.getX() / xAxis.getWidth(), 0.0, 1.0);
                mouseVal = curLower + fractionOnAxis * curSpan;
            } else {
                mouseVal = (curLower + curUpper) / 2.0;
            }

            double newSpan = Math.max(0.5, curSpan * zoomFactor);
            double fraction = Math.clamp((mouseVal - curLower) / curSpan, 0.0, 1.0);

            double newLower = mouseVal - fraction * newSpan;
            double newUpper = mouseVal + (1.0 - fraction) * newSpan;

            if (newLower < minYear) {
                newUpper += (minYear - newLower);
                newLower = minYear;
            }
            if (newUpper > maxYear) {
                newLower -= (newUpper - maxYear);
                newUpper = maxYear;
            }
            if (newLower < minYear) newLower = minYear;
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

        VBox chartBox = new VBox(6, chartHeaderLabel, comboLabel, chartMetricCombo, windowBox, chartNavHelpLabel, lineChart);
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

    // Helper subroutine: get min allowed year - internal state computation & bounds checking
    private long getMinAllowedYear() {
        if (engine != null && engine.getCurrentScenario() != null) {
            return engine.getCurrentScenario().getStartDateYear();
        }
        if (engine != null && engine.getTimeManager() != null) {
            return engine.getTimeManager().getCurrentYear();
        }
        return 0L;
    }

    // Helper subroutine: get max allowed year - internal state computation & bounds checking
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

    // Helper subroutine: set time window - internal state computation & bounds checking
    private void setTimeWindow(double windowYears) {
        this.currentWindowYears = windowYears;
        reloadChartData();
    }

    public record MetricCardMeta(String key, String defaultTitle, String defaultCategory, String defaultUnit, String defaultTooltip) {}

    private static final List<MetricCardMeta> METRIC_CARD_METAS = List.of(
        // Category 1: Energy & Matter
        new MetricCardMeta("energyCaptured", "Captured Energy (Global Power)", "⚡ Energy & Matter", "MW",
            "P_tot = ∑ (P_solar + P_biomass + P_geothermal). Gross primary power extracted from the physical environment by society."),
        new MetricCardMeta("resourceDepletion", "Resource Depletion Rate", "⚡ Energy & Matter", "%",
            "D = (Stock_initial - Stock_actual) / Stock_initial. Cumulative percentage of non-renewable mineral reserve consumption."),
        new MetricCardMeta("energyPerCapita", "Energy / Capita (Power)", "⚡ Energy & Matter", "W/cap",
            "P_cap = P_tot / N_pop. Continuous per-capita power availability (Leslie White law: Culture = E × T)."),
        new MetricCardMeta("foodPerCapita", "Food Stocks / Capita", "⚡ Energy & Matter", "GJ/cap",
            "F_cap = Food_Stocks / N_pop in Gigajoules. Trophic energy stock available per individual."),
        new MetricCardMeta("eroiAlim", "Food EROI (Net Yield)", "⚡ Energy & Matter", "Ratio",
            "EROI = E_out / E_in. Ratio of energy acquired compared to the energetic cost of subsistence."),
        new MetricCardMeta("netSurplus", "Net Energy Surplus", "⚡ Energy & Matter", "%",
            "Phi = 1 - 1/EROI. Fraction of energy available for non-agricultural structures, crafts, cities, and complex institutions."),
        new MetricCardMeta("trophicMultiplier", "Trophic Multiplier", "⚡ Energy & Matter", "x",
            "Multiplier of gross biomass mobilized relative to metabolic ingestion."),
        new MetricCardMeta("biomassMobilized", "Mobilized Biomass / Capita", "⚡ Energy & Matter", "kg/yr",
            "Gross annual biomass mobilized per individual (direct food, draft/pasture livestock feed, and losses)."),
        new MetricCardMeta("pibMaterialFlow", "Material Metabolic Flow", "⚡ Energy & Matter", "Mt/yr",
            "Total volume of biomass and ores displaced by societal metabolism."),
        new MetricCardMeta("biomassNatural", "Natural Biomass", "⚡ Energy & Matter", "GtC",
            "Total stock of preserved wild plant and faunal carbon."),
        new MetricCardMeta("biomassDomesticated", "Domesticated Biomass", "⚡ Energy & Matter", "GtC",
            "Total biomass of agricultural crops and livestock."),
        new MetricCardMeta("potableWater", "Freshwater & Aquifers", "⚡ Energy & Matter", "10³ km³",
            "Global potable freshwater and continental aquifer reserves."),
        new MetricCardMeta("remainingResources", "Remaining Geological Resources", "⚡ Energy & Matter", "%",
            "Unextracted mineral and geological capital remaining in the ground."),
        new MetricCardMeta("entropyPollution", "Thermodynamic Entropy & Pollution", "⚡ Energy & Matter", "Idx",
            "Thermodynamic entropy generation and pollutant emissions."),
        new MetricCardMeta("energyEroi", "Global Energy EROI", "⚡ Energy & Matter", "Ratio",
            "Average Energy Return On Investment across all exploited primary energy sources."),
        new MetricCardMeta("occupiedTerritory", "Subsistence Territory & Footprint", "⚡ Energy & Matter", "km²",
            "Ecological foraging / production surface area (Home Range of Binford, Kelly, Hassan)."),

        // Category 2: Demography & Health
        new MetricCardMeta("population", "Human Population", "👥 Demography & Health", "cap",
            "Total living human population on the planet."),
        new MetricCardMeta("fertilityRate", "Total Fertility Rate", "👥 Demography & Health", "ch/woman",
            "Average number of children born to a woman during her reproductive years."),
        new MetricCardMeta("offspringPct", "Adults with Offspring", "👥 Demography & Health", "%",
            "Proportion of adult population with at least one surviving descendant."),
        new MetricCardMeta("ageFirstChild", "Age at First Childbirth", "👥 Demography & Health", "yrs",
            "Average maternal age at the birth of the first child."),
        new MetricCardMeta("immigrationRate", "Net Migration Rate", "👥 Demography & Health", "‰",
            "Net inter-regional demographic flow per thousand inhabitants."),
        new MetricCardMeta("lifeExpectancy", "Life Expectancy at Birth", "👥 Demography & Health", "yrs",
            "Mean projected lifespan at birth based on current age-specific mortality."),
        new MetricCardMeta("healthIndex", "Global Health & Immunity Index", "👥 Demography & Health", "%",
            "Composite index of epidemiological resistance, nutrition, and sanitary resilience."),
        new MetricCardMeta("educationLevel", "Education & Knowledge Capital", "👥 Demography & Health", "%",
            "Literacy rate, apprenticeship level, and cumulative knowledge transmission."),

        // Category 3: Society & Institutions
        new MetricCardMeta("happinessIndex", "Subjective Well-Being / Happiness", "🏛️ Society & Institutions", "%",
            "Aggregated index of societal life satisfaction, caloric security, and social cohesion."),
        new MetricCardMeta("conflictLevel", "Societal Conflict & Warfare Intensity", "🏛️ Society & Institutions", "%",
            "Friction level, internal civil strife, rebellion, and external warfare intensity."),
        new MetricCardMeta("cityStates", "Independent Polities & City-States", "🏛️ Society & Institutions", "polities",
            "Number of autonomous administrative centers, poleis, and state institutions."),
        new MetricCardMeta("institutionalMaturity", "Institutional & Bureaucratic Maturity", "🏛️ Society & Institutions", "Idx",
            "Legal codification, fiscal administration, and state capacity index."),
        new MetricCardMeta("divisionLabor", "Division of Labor & Specialization", "🏛️ Society & Institutions", "Idx",
            "Structural diversity of specialized occupational niches (Durkheimian differentiation)."),
        new MetricCardMeta("maxHierarchy", "Maximum Jurisdictional Hierarchy", "🏛️ Society & Institutions", "Lvl",
            "Peak institutional layering depth (1 = Tribe/Band to 6 = Transnational Empire)."),
        new MetricCardMeta("largestCulture", "Largest Cultural Entity Size", "🏛️ Society & Institutions", "cap",
            "Population of the single largest unified cultural/political entity."),
        new MetricCardMeta("largestOrgComplexity", "Largest Polity Structural Complexity", "🏛️ Society & Institutions", "Idx",
            "Structural complexity of the largest organization (Population × Tech × State × Hierarchy)."),
        new MetricCardMeta("largestOrgEntropy", "Largest Polity Thermodynamic Entropy", "⚡ Energy & Matter", "J/K",
            "Rate of thermodynamic entropy and waste heat production of the largest polity."),
        new MetricCardMeta("avgTechLevel", "Average Technology Level", "🏛️ Society & Institutions", "Lvl",
            "Global population-weighted average of scientific knowledge and technological tier."),
        new MetricCardMeta("kardashevScale", "Kardashev Energy Mastery Scale", "🏛️ Society & Institutions", "Type K",
            "K = (log10(P_watts) - 6) / 10. Planetary civilizational energy mastery index (Type 0.0 to 1.0+)."),

        // Category 4: Economy & Wealth
        new MetricCardMeta("giniIndex", "Gini Income Inequality", "💎 Economy & Wealth", "Coeff",
            "G = A / (A + B). Measure of wealth/income dispersion (0 = total equality, 1 = maximum inequality)."),
        new MetricCardMeta("landGini", "Land Tenure Inequality (Land Gini)", "💎 Economy & Wealth", "Coeff",
            "Gini coefficient of agricultural land ownership and natural resource tenure."),
        new MetricCardMeta("gdpTotal", "Gross Domestic Product (GDP)", "💎 Economy & Wealth", "G$",
            "Total gross economic output converted to constant purchasing power units."),
        new MetricCardMeta("builtCapital", "Built Capital & Tooling Stock", "💎 Economy & Wealth", "kg/cap",
            "Total physical infrastructure, building mass, and machinery stock per inhabitant."),
        new MetricCardMeta("eliteFormation", "Elite Aspirant Ratio", "💎 Economy & Wealth", "%",
            "Fraction of population holding leadership, administrative, and command positions."),
        new MetricCardMeta("elderCapitalShare", "Senior Wealth Concentration", "💎 Economy & Wealth", "%",
            "Share of accumulated landed and financial wealth controlled by senior cohorts."),
        new MetricCardMeta("landRent", "Land & Urban Economic Rent", "💎 Economy & Wealth", "Idx",
            "Ricardian differential rent value driven by spatial demographic density and infrastructure."),
        new MetricCardMeta("toolsCount", "Operational Tooling Units", "💎 Economy & Wealth", "units",
            "Total quantity of productive tools, machinery, and craft instruments."),
        new MetricCardMeta("productsCount", "Product Catalog Diversity", "💎 Economy & Wealth", "types",
            "Distinct technical variety count of manufactured goods in the civilizational catalogue."),

        // Category 5: Cognition & Information
        new MetricCardMeta("shannonBandwidth", "Shannon Informational Bandwidth", "🧠 Cognition & Information", "Gbps",
            "Maximum informational transmission channel capacity across the inter-regional network."),
        new MetricCardMeta("collectiveMemory", "Cumulative Collective Knowledge Stock", "🧠 Cognition & Information", "TB",
            "Aggregated volume of preserved written records, scientific treatises, and cultural data."),
        new MetricCardMeta("innovationDiffusion", "Innovation Diffusion Wavefront Speed", "🧠 Cognition & Information", "km/yr",
            "Spatial propagation velocity of newly invented techniques across trade routes."),
        new MetricCardMeta("knowledgeDecay", "Historical Amnesia / Knowledge Decay", "🧠 Cognition & Information", "%/dec",
            "Loss rate of accumulated technical and institutional knowledge during systemic disruptions."),

        // Category 6: Ecology & Planetary Boundaries
        new MetricCardMeta("soilNPK", "Arable Soil N-P-K Fertility", "🌍 Ecology & Planetary Boundaries", "%",
            "Macro-nutrient stoichiometric fertility index (Nitrogen, Phosphorus, Potassium) in arable soils."),
        new MetricCardMeta("carryingCapacitySat", "Carrying Capacity Saturation (N/K)", "🌍 Ecology & Planetary Boundaries", "Ratio",
            "Ratio of human population to sustainable local carrying capacity (N/K). Critical threshold at 1.0."),
        new MetricCardMeta("planetaryOvershoot", "Planetary Boundary Overshoot", "🌍 Ecology & Planetary Boundaries", "x",
            "Overshoot factor across the 9 planetary boundaries (Stockholm Resilience Centre)."),
        new MetricCardMeta("carbonFootprint", "Atmospheric Carbon Footprint", "🌍 Ecology & Planetary Boundaries", "GtCO₂",
            "Annual global greenhouse gas and fossil carbon emissions to the atmosphere."),
        new MetricCardMeta("wildBiodiversity", "Wild Biodiversity Intactness", "🌍 Ecology & Planetary Boundaries", "%",
            "Proportion of preserved wild flora and fauna biomass relative to pre-anthropic baseline."),
        new MetricCardMeta("wetBulbSafety", "Wet-Bulb Temperature Safety Margin", "🌍 Ecology & Planetary Boundaries", "°C",
            "Thermal buffer margin below the human physiological lethal wet-bulb threshold (Tw = 35°C)."),

        // Category 7: Cliodynamics & Systemic Risks
        new MetricCardMeta("turchinPsi", "Political Stress Index (PSI)", "⏳ Cliodynamics & Systemic Risks", "Idx",
            "PSI = W × E × S. Turchin Structural-Demographic stress index (Popular immiseration × Elite overproduction × State fiscal distress)."),
        new MetricCardMeta("asabiyyah", "Ibn Khaldun Asabiyyah Cohesion", "⏳ Cliodynamics & Systemic Risks", "%",
            "Social group solidarity, mutual trust, and collective action capacity (Ibn Khaldoun 1377)."),
        new MetricCardMeta("eliteOverproduction", "Elite Overproduction Ratio", "⏳ Cliodynamics & Systemic Risks", "Idx",
            "Ratio of elite position contenders relative to available institutional offices (Turchin SDT)."),
        new MetricCardMeta("fiscalStress", "Fiscal & Public Treasury Distress", "⏳ Cliodynamics & Systemic Risks", "%",
            "Public debt burden and treasury extraction stress required to sustain administrative hierarchy."),
        new MetricCardMeta("geopoliticalTension", "Geopolitical Friction & War Tension", "⏳ Cliodynamics & Systemic Risks", "%",
            "Inter-polity balance of power friction and systemic escalation probability."),
        new MetricCardMeta("collapseVulnerability", "Systemic Collapse Vulnerability", "⏳ Cliodynamics & Systemic Risks", "%",
            "Instantaneous probability of institutional unraveling or cascade feedback failure."),

        // Category 8: System Complexity
        new MetricCardMeta("systemComplexity", "Systemic Structural Complexity", "⚙️ Systemic Complexity", "Idx",
            "Tainter-Joseph civilizational complexity index: number of interrelated social and economic parts."),
        new MetricCardMeta("reconstructionCapability", "Civilizational Reboot / Recovery Potential", "⚙️ Systemic Complexity", "%",
            "Capacity to reconstruct complex infrastructure and institutional memory following catastrophic shock."),
        new MetricCardMeta("systemInterdependence", "Supply Chain Interdependence Fragility", "⚙️ Systemic Complexity", "%",
            "Systemic vulnerability stemming from deep supply-chain and division-of-labor hyper-specialization."),

        // Category 9: Engine Performance
        new MetricCardMeta("engineTPS", "Engine Simulation Rate", "💻 Technical Performance", "steps/s",
            "Actual real-time compute frequency of the simulation engine in simulation steps per second."),
        new MetricCardMeta("ramMemory", "Heap RAM Utilization", "💻 Technical Performance", "MB",
            "Live JVM heap memory consumption of the H3 simulation state."),
        new MetricCardMeta("cellCount", "Loaded H3 Hexagonal Cells", "💻 Technical Performance", "hex",
            "Total count of active hexagonal spatial cells loaded in memory.")
    );

    /*
     * Update texts.
     * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
     *
     */
    public void updateTexts() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        isUpdatingTexts = true;
        try {
            headerTitle.setText(I18n.getOrDefault("stats.header", "📊 CLIODYNAMIC & PHYSICALIST STATISTICS DASHBOARD"));
            cpuNoticeLabel.setText(I18n.getOrDefault("stats.notice.cpu",
                "⚠️ CPU PERFORMANCE NOTICE: Real-time statistical aggregation (Gini indices, thermodynamic entropy, " +
                "Turchin structural-demographic analysis, variance and H3 spatial meshes) runs intensive per-cell compute cycles. " +
                "If simulation rate drops, decrease sampling frequency below (e.g. 5 or 20 ticks) or pause live collection."));

            btnLiveCollection.setText(isLiveCollectionActive
                    ? I18n.getOrDefault("stats.btn.live_collection_active", "⚡ Live Stats: ACTIVE")
                    : I18n.getOrDefault("stats.btn.live_collection_paused", "⏸️ Live Stats: PAUSED"));
            btnLiveCollection.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.live_collection", "Toggle dynamic statistics computation to conserve CPU resources.")));

            int selectedSampling = samplingCombo.getSelectionModel().getSelectedIndex();
            samplingCombo.getItems().clear();
            samplingCombo.getItems().addAll(
                I18n.getOrDefault("stats.sampling.1tick", "1 Tick (Every Cycle)"),
                I18n.getOrDefault("stats.sampling.5ticks", "5 Ticks"),
                I18n.getOrDefault("stats.sampling.20ticks", "20 Ticks"),
                I18n.getOrDefault("stats.sampling.100ticks", "100 Ticks")
            );
            samplingCombo.getSelectionModel().select(selectedSampling >= 0 ? selectedSampling : 0);
            samplingCombo.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.sampling", "Sampling and refresh rate for all spatial metrics.")));

            btnFormulaEditor.setText(I18n.getOrDefault("stats.btn.formula_editor", "🧮 Custom Formulas & Variables"));
            btnFormulaEditor.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.formula_editor", "Open pluggable formula editor (SUM, AVG, GINI, etc.).")));

            chartHeaderLabel.setText(I18n.getOrDefault("stats.section.time_evolution", "📈 Indicator Temporal Evolution"));
            comboLabel.setText(I18n.getOrDefault("stats.label.traced_stat", "Traced indicator:"));
            windowLabel.setText(I18n.getOrDefault("stats.label.window", "Time Window:"));

            btn1Yr.setText(I18n.getOrDefault("stats.window.1yr", "1 Yr"));
            btn10Yr.setText(I18n.getOrDefault("stats.window.10yr", "10 Yrs"));
            btn100Yr.setText(I18n.getOrDefault("stats.window.100yr", "100 Yrs"));
            btn1000Yr.setText(I18n.getOrDefault("stats.window.1000yr", "1000 Yrs"));
            btnAll.setText(I18n.getOrDefault("stats.window.all", "All"));

            xAxis.setLabel(I18n.getOrDefault("stats.chart.time_axis", "Time (Years)"));
            lineChart.setTitle(I18n.getOrDefault("stats.chart.title", "Temporal Trend Curve (💡 [CTRL] + Scroll to Zoom | [CTRL] + Drag to Pan | Double-Click to Reset)"));

            barHeaderLabel.setText(I18n.getOrDefault("stats.section.age_pyramid", "📊 DEMOGRAPHIC DISTRIBUTION (Age Pyramid)"));
            barChart.setTitle(I18n.getOrDefault("stats.chart.age_pyramid_title", "Age Pyramid"));

            metricsHeaderLabel.setText(I18n.getOrDefault("stats.section.detailed_metrics", "📋 CLIODYNAMIC INDICATORS & DETAILED METRICS"));
            metricInspectorTitle.setText(I18n.getOrDefault("stats.label.metric_explanation", "🔎 Cliodynamic Metric Explanation:"));
            metricInspectorText.setText(I18n.getOrDefault("stats.desc.hover_metric", "Hover or click on any statistic card below to view its mathematical formula, underlying parameters, and sociological interpretation."));

            int catIdx = categoryFilterCombo.getSelectionModel().getSelectedIndex();
            categoryFilterCombo.getItems().clear();
            categoryFilterCombo.getItems().addAll(
                I18n.getOrDefault("stats.cat.all", "All Categories"),
                I18n.getOrDefault("stats.cat.energy", "⚡ Energy & Matter"),
                I18n.getOrDefault("stats.cat.demography", "👥 Demography & Health"),
                I18n.getOrDefault("stats.cat.society", "🏛️ Society & Institutions"),
                I18n.getOrDefault("stats.cat.economy", "💎 Economy & Wealth"),
                I18n.getOrDefault("stats.cat.cognition", "🧠 Cognition & Information"),
                I18n.getOrDefault("stats.cat.ecology", "🌍 Ecology & Planetary Boundaries"),
                I18n.getOrDefault("stats.cat.cliodynamics", "⏳ Cliodynamics & Systemic Risks"),
                I18n.getOrDefault("stats.cat.complexity", "⚙️ Systemic Complexity"),
                I18n.getOrDefault("stats.cat.performance", "💻 Technical Performance")
            );
            categoryFilterCombo.getSelectionModel().select(catIdx >= 0 ? catIdx : 0);

            searchField.setPromptText(I18n.getOrDefault("stats.prompt.filter", "🔍 Filter metric (e.g. Kardashev, Gini, Collapse, NPK)..."));

            exportBtn.setText(I18n.getOrDefault("stats.btn.export_csv", "📥 Export Dataset (CSV)"));
            exportBtn.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.export_csv", "Export complete history of societal, energetic, and economic metrics as CSV.")));

            // Update all metric cards with localized texts
            for (MetricCardMeta meta : METRIC_CARD_METAS) {
                MetricCard card = metricCards.get(meta.key());
                if (card != null) {
                    String title = I18n.getOrDefault("stat.metric." + meta.key() + ".title", meta.defaultTitle());
                    String cat = I18n.getOrDefault("stat.metric." + meta.key() + ".cat", meta.defaultCategory());
                    String unit = I18n.getOrDefault("stat.metric." + meta.key() + ".unit", meta.defaultUnit());
                    String tooltip = I18n.getOrDefault("stat.metric." + meta.key() + ".tooltip", meta.defaultTooltip());
                    card.updateTexts(title, cat, unit, tooltip);
                }
            }

            // Refresh Metric Combo: Grouped by category with section headers
            String prevSelected = chartMetricCombo.getValue();
            chartMetricCombo.getItems().clear();

            Map<String, List<MetricCard>> cardsByCategory = new LinkedHashMap<>();
            for (MetricCard card : metricCards.values()) {
                cardsByCategory.computeIfAbsent(card.getCategory(), k -> new ArrayList<>()).add(card);
            }

            // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
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
                // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
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

    // Helper subroutine: register metric cards - internal state computation & bounds checking
    private void registerMetricCards(Label inspectorTitle, Label inspectorText) {
        metricCards.clear();
        for (MetricCardMeta meta : METRIC_CARD_METAS) {
            String title = I18n.getOrDefault("stat.metric." + meta.key() + ".title", meta.defaultTitle());
            String cat = I18n.getOrDefault("stat.metric." + meta.key() + ".cat", meta.defaultCategory());
            String unit = I18n.getOrDefault("stat.metric." + meta.key() + ".unit", meta.defaultUnit());
            String tooltip = I18n.getOrDefault("stat.metric." + meta.key() + ".tooltip", meta.defaultTooltip());
            addCard(meta.key(), title, cat, unit, tooltip, inspectorTitle, inspectorText);
        }
    }

    // Helper subroutine: add card - internal state computation & bounds checking
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

    // Helper subroutine: filter metrics - internal state computation & bounds checking
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

    /*
     * Reset.
     * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
     *
     */
    public void reset() {
        chartSeries.getData().clear();
        barSeries.getData().clear();
        tickCounter = 0;
        if (xAxis != null) {
            xAxis.setAutoRanging(true);
        }
    }

    /*
     * Reset chart series.
     * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
     *
     */
    public void resetChartSeries() {
        reloadChartData();
    }

    // Helper subroutine: reload chart data - internal state computation & bounds checking
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
            // Temporal integration loop: Advance simulation timeline step-by-step
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

    /*
     * Update.
     * Enforces physical invariants and updates associated state variables within {@code StatsPanel}.
     *
     */
    public void update() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
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
                        barSeries.getData().add(new XYChart.Data<>(I18n.getOrDefault("stats.pyramid.cohort.0_14", "0-14 yrs"), pyramid[0]));
                        barSeries.getData().add(new XYChart.Data<>(I18n.getOrDefault("stats.pyramid.cohort.15_24", "15-24 yrs"), pyramid[1]));
                        barSeries.getData().add(new XYChart.Data<>(I18n.getOrDefault("stats.pyramid.cohort.25_39", "25-39 yrs"), pyramid[2]));
                        barSeries.getData().add(new XYChart.Data<>(I18n.getOrDefault("stats.pyramid.cohort.40_54", "40-54 yrs"), pyramid[3]));
                        barSeries.getData().add(new XYChart.Data<>(I18n.getOrDefault("stats.pyramid.cohort.55_69", "55-69 yrs"), pyramid[4]));
                        barSeries.getData().add(new XYChart.Data<>(I18n.getOrDefault("stats.pyramid.cohort.70_84", "70-84 yrs"), pyramid[5]));
                        barSeries.getData().add(new XYChart.Data<>(I18n.getOrDefault("stats.pyramid.cohort.85_plus", "85+ yrs"), pyramid[6]));
                    } else if (pyramid != null && pyramid.length >= 3) {
                        barSeries.getData().add(new XYChart.Data<>(I18n.getOrDefault("stats.pyramid.cohort.young", "Youth (<15 yrs)"), pyramid[0]));
                        barSeries.getData().add(new XYChart.Data<>(I18n.getOrDefault("stats.pyramid.cohort.adult", "Adults (15-60 yrs)"), pyramid[1]));
                        barSeries.getData().add(new XYChart.Data<>(I18n.getOrDefault("stats.pyramid.cohort.elder", "Elders (>60 yrs)"), pyramid[2]));
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

        String key = null;
        for (MetricCard card : metricCards.values()) {
            if (card.getTitle().equalsIgnoreCase(selectedMetric)) {
                key = card.getKey();
                break;
            }
        }

        if (key != null) {
            return switch (key) {
                case "energyCaptured" -> energyCap;
                case "resourceDepletion" -> resDep;
                case "energyPerCapita" -> energyPerCap;
                case "foodPerCapita" -> foodPerCap;
                case "eroiAlim" -> engine != null ? engine.getEroiAlimentaire() : 1.0;
                case "netSurplus" -> engine != null ? engine.getNetSurplusFraction() * 100.0 : 0.0;
                case "trophicMultiplier" -> engine != null ? engine.getTrophicMultiplier() : 1.0;
                case "biomassMobilized" -> engine != null ? engine.getBiomassMobilizedPerCapitaKg() : 0.0;
                case "pibMaterialFlow" -> resDep * 5.2;
                case "biomassNatural" -> bio;
                case "biomassDomesticated" -> bioDom;
                case "potableWater" -> water;
                case "remainingResources" -> remRes;
                case "entropyPollution" -> entropy;
                case "occupiedTerritory" -> territory;
                case "population" -> pop;
                case "fertilityRate" -> fert;
                case "offspringPct" -> offspring;
                case "ageFirstChild" -> ageFirstChild;
                case "immigrationRate" -> immigration;
                case "lifeExpectancy" -> life;
                case "healthIndex" -> Math.min(100.0, life * 1.1);
                case "educationLevel" -> education;
                case "happinessIndex" -> happiness;
                case "conflictLevel" -> conflict;
                case "cityStates" -> cityStates;
                case "institutionalMaturity" -> instMaturity;
                case "divisionLabor" -> divLabor;
                case "maxHierarchy" -> maxHier;
                case "largestCulture" -> largestCult;
                case "largestOrgComplexity" -> largestOrgComp;
                case "largestOrgEntropy" -> largestOrgEnt;
                case "avgTechLevel" -> tech;
                case "kardashevScale" -> kardashev;
                case "giniIndex", "landGini" -> gini;
                case "gdpTotal" -> gdp;
                case "builtCapital" -> builtCap / Math.max(1, pop);
                case "eliteFormation" -> eliteForm;
                case "elderCapitalShare" -> elderCap;
                case "landRent" -> landRent;
                case "toolsCount" -> tools;
                case "productsCount" -> products;
                case "shannonBandwidth" -> shannonBw;
                case "collectiveMemory" -> memoryStock;
                case "innovationDiffusion" -> innovSpeed;
                case "knowledgeDecay" -> knowDecay;
                case "soilNPK" -> soilNPK;
                case "carryingCapacitySat" -> Math.min(2.0, (double) pop / Math.max(1.0, bio * 1e6));
                case "planetaryOvershoot" -> Math.max(1.0, (double) pop / 5e9);
                case "carbonFootprint" -> carbonFp;
                case "wildBiodiversity" -> wildBio;
                case "wetBulbSafety" -> wetBulb;
                case "turchinPsi" -> eliteOver * fiscalStress / 100.0;
                case "asabiyyah" -> engine != null ? engine.getAverageAsabiyyah() : 50.0;
                case "eliteOverproduction" -> eliteOver;
                case "fiscalStress" -> fiscalStress;
                case "geopoliticalTension" -> geoTension;
                case "collapseVulnerability" -> collapseVuln;
                case "systemComplexity" -> sysComp;
                case "reconstructionCapability" -> reconCap;
                case "systemInterdependence" -> sysInter;
                case "engineTPS" -> tps;
                case "ramMemory" -> usedMem;
                case "cellCount" -> totalCells;
                default -> pop;
            };
        }

        return pop;
    }

    // Helper subroutine: set card val - internal state computation & bounds checking
    private void setCardVal(String key, String val, double ratio) {
        MetricCard card = metricCards.get(key);
        if (card != null) {
            card.updateValue(val, ratio);
        }
    }

    // Helper subroutine: export csv - internal state computation & bounds checking
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


