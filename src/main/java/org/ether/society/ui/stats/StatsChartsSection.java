/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.stats;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.ether.society.analytics.HistorySnapshot;
import org.ether.society.analytics.MetricDescriptor;
import org.ether.society.analytics.MetricRegistry;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.i18n.Language;
import org.ether.society.ui.DisplayMode;
import org.ether.society.ui.WindowUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Historical time-series LineChart and demographic age-pyramid BarChart section for {@link org.ether.society.ui.StatsPanel}.
 *
 * <p>Extracted from {@code StatsPanel} to satisfy the Single Responsibility Principle (SRP).
 * Features variable sliding time-window ranges (1 yr, 10 yr, 100 yr, 1000 yr, All),
 * clamped mouse drag-panning, cursor-centered scroll zooming, and downsampled telemetry rendering.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class StatsChartsSection {

    private final VBox chartBox;
    private final VBox barBox;

    private final Label chartHeaderLabel;
    private final Label comboLabel;
    private final ComboBox<String> chartMetricCombo;

    private final Label windowLabel;
    private final ToggleButton btn1Yr;
    private final ToggleButton btn10Yr;
    private final ToggleButton btn100Yr;
    private final ToggleButton btn1000Yr;
    private final ToggleButton btnAll;

    private final LineChart<Number, Number> lineChart;
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    private final XYChart.Series<Number, Number> chartSeries = new XYChart.Series<>();

    private final Label barHeaderLabel;
    private final CategoryAxis barX;
    private final NumberAxis barY;
    private final BarChart<String, Number> barChart;
    private final XYChart.Series<String, Number> barSeries = new XYChart.Series<>();

    private double currentWindowYears = 10.0;
    private boolean isUpdatingTexts = false;

    private Supplier<H3SimulationEngine> engineSupplier;
    private Consumer<DisplayMode> onDisplayModeRequested;

    /**
     * Constructs the charts section with interactive LineChart and age-pyramid BarChart.
     */
    public StatsChartsSection() {
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
                    MetricDescriptor desc = MetricRegistry.getInstance().getDescriptorByName(title);
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
        xAxis.setMinorTickVisible(false);
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
                boolean isFr = I18n.getCurrentLanguage() == Language.FRENCH;
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
        yAxis.setMinorTickVisible(false);
        yAxis.setTickLabelFill(Color.GRAY);
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(false);

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(false);
        lineChart.setAnimated(false);
        lineChart.setLegendVisible(false);
        lineChart.setPrefHeight(160);
        lineChart.getData().add(chartSeries);

        Label chartNavHelpLabel = new Label(I18n.getOrDefault("stats.chart.nav_help", "💡 Navigation : Molette pour zoomer sur le curseur | Glisser (clic) pour défiler temporellement | Double-clic pour réinitialiser"));
        chartNavHelpLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8; -fx-padding: 3 6; -fx-background-color: rgba(56, 189, 248, 0.08); -fx-background-radius: 4;");
        chartNavHelpLabel.setWrapText(true);

        final double[] dragAnchor = new double[2];
        lineChart.setOnMouseEntered(e -> lineChart.setCursor(Cursor.HAND));
        lineChart.setOnMouseExited(e -> lineChart.setCursor(Cursor.DEFAULT));
        lineChart.setOnMousePressed(e -> {
            dragAnchor[0] = e.getX();
            dragAnchor[1] = e.getY();
            lineChart.setCursor(Cursor.CLOSED_HAND);
        });
        lineChart.setOnMouseReleased(e -> lineChart.setCursor(Cursor.HAND));
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
            xAxis.setTickUnit(WindowUtils.computeNiceTickUnit(newUpper - newLower));
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

            Point2D localPoint = xAxis.sceneToLocal(e.getSceneX(), e.getSceneY());
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
            xAxis.setTickUnit(WindowUtils.computeNiceTickUnit(newUpper - newLower));
        });
        lineChart.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 || (e.isControlDown() && e.getButton() == MouseButton.SECONDARY)) {
                if (currentWindowYears > 0) {
                    setTimeWindow(currentWindowYears);
                } else {
                    xAxis.setAutoRanging(true);
                    yAxis.setAutoRanging(true);
                }
            }
        });

        chartBox = new VBox(6, chartHeaderLabel, comboLabel, chartMetricCombo, windowBox, chartNavHelpLabel, lineChart);
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

        barBox = new VBox(6, barHeaderLabel, barChart);
        barBox.getStyleClass().add("card-section");
    }

    public void setEngineSupplier(Supplier<H3SimulationEngine> engineSupplier) {
        this.engineSupplier = engineSupplier;
    }

    public void setOnDisplayModeRequested(Consumer<DisplayMode> onDisplayModeRequested) {
        this.onDisplayModeRequested = onDisplayModeRequested;
    }

    public VBox getChartBox() { return chartBox; }
    public VBox getBarBox() { return barBox; }
    public ComboBox<String> getChartMetricCombo() { return chartMetricCombo; }
    public NumberAxis getXAxis() { return xAxis; }
    public XYChart.Series<Number, Number> getChartSeries() { return chartSeries; }
    public XYChart.Series<String, Number> getBarSeries() { return barSeries; }

    public void reset() {
        chartSeries.getData().clear();
        barSeries.getData().clear();
        if (xAxis != null) xAxis.setAutoRanging(true);
    }

    public void resetChartSeries() {
        reloadChartData();
    }

    public void appendTimeSeriesPoint(double currentTime, double yVal, double minAllowedYear) {
        if (!chartSeries.getData().isEmpty()) {
            chartSeries.getData().removeIf(data -> data.getXValue().doubleValue() > currentTime + 0.0001);
        }

        if (chartSeries.getData().isEmpty() || Math.abs(chartSeries.getData().get(chartSeries.getData().size() - 1).getXValue().doubleValue() - currentTime) >= 0.001) {
            chartSeries.getData().add(new XYChart.Data<>(currentTime, yVal));

            if (currentWindowYears > 0) {
                double minTime = Math.max(minAllowedYear, currentTime - currentWindowYears);
                chartSeries.getData().removeIf(data -> data.getXValue().doubleValue() < minTime - 0.0001);
                xAxis.setAutoRanging(false);
                xAxis.setLowerBound(minTime);
                double maxTime = Math.max(minTime + 1.0, currentTime);
                xAxis.setUpperBound(maxTime);
                xAxis.setTickUnit(WindowUtils.computeNiceTickUnit(maxTime - minTime));
            }
        }
    }

    public void updateAgePyramid(int[] pyramid) {
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
    }

    public void setTimeWindow(double windowYears) {
        this.currentWindowYears = windowYears;
        reloadChartData();
    }

    public void reloadChartData() {
        chartSeries.getData().clear();
        H3SimulationEngine engine = engineSupplier != null ? engineSupplier.get() : null;
        if (engine == null || engine.getCells() == null || engine.getCells().isEmpty()) return;
        if (engine.getHistoryManager() == null || engine.getHistoryManager().getHistory() == null) return;

        List<HistorySnapshot> snapshots = engine.getHistoryManager().getHistory().getSnapshots();
        if (snapshots.isEmpty()) return;

        String selectedMetric = chartMetricCombo != null ? chartMetricCombo.getValue() : "Population Humaine";
        if (selectedMetric == null || selectedMetric.startsWith("─── ")) {
            selectedMetric = "Population Humaine";
        }
        double currentTime = engine.getTimeManager().getCurrentYear() + (engine.getTimeManager().getCurrentMonth() / 12.0);
        double minAllowed = getMinAllowedYear();

        double minTime = (currentWindowYears > 0) ? Math.max(minAllowed, currentTime - currentWindowYears) : minAllowed;

        List<XYChart.Data<Number, Number>> points = new ArrayList<>();
        for (HistorySnapshot snap : snapshots) {
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
            double maxTime = Math.max(minTime + 1.0, currentTime);
            xAxis.setUpperBound(maxTime);
            xAxis.setTickUnit(WindowUtils.computeNiceTickUnit(maxTime - minTime));
        } else {
            xAxis.setAutoRanging(true);
        }
    }

    public void updateTexts(boolean isUpdating) {
        this.isUpdatingTexts = isUpdating;
        chartHeaderLabel.setText("📈 " + I18n.getOrDefault("stats.header.chart", "HISTORIQUE DYNAMIQUE"));
        comboLabel.setText(I18n.getOrDefault("stats.label.choose_metric", "Métrique représentée :"));
        windowLabel.setText(I18n.getOrDefault("stats.label.window", "Fenêtre :"));
        btn1Yr.setText(I18n.getOrDefault("stats.btn.1yr", "1 an"));
        btn10Yr.setText(I18n.getOrDefault("stats.btn.10yr", "10 ans"));
        btn100Yr.setText(I18n.getOrDefault("stats.btn.100yr", "100 ans"));
        btn1000Yr.setText(I18n.getOrDefault("stats.btn.1000yr", "1 000 ans"));
        btnAll.setText(I18n.getOrDefault("stats.btn.all", "Tout"));
        barHeaderLabel.setText("👥 " + I18n.getOrDefault("stats.header.pyramid", "PYRAMIDE DES ÂGES (POPULATION MONDIALE)"));
    }

    private long getMinAllowedYear() {
        H3SimulationEngine engine = engineSupplier != null ? engineSupplier.get() : null;
        if (engine != null && engine.getCurrentScenario() != null) {
            return engine.getCurrentScenario().getStartDateYear();
        }
        if (engine != null && engine.getTimeManager() != null) {
            return engine.getTimeManager().getCurrentYear();
        }
        return 0L;
    }

    private long getMaxAllowedYear() {
        H3SimulationEngine engine = engineSupplier != null ? engineSupplier.get() : null;
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
}
