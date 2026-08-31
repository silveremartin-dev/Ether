package org.ether.society.ui;

import org.ether.society.core.dod.PluggableStatEngine;
import org.ether.society.core.dod.StatisticsKernel;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Dedicated Variance & Individual Distribution Analytics Panel.
 * Computes and displays variance between individuals/cells on a selected variable
 * (e.g. Wealth / Capital, Food, Population Density, Age), standard deviation bounds (μ ± σ),
 * Gini coefficient, and a 10-bin histogram distribution curve.
 *
 * @author Silvere Martin-Michiellot
 */
public class VarianceDistributionPanel extends VBox {

    private final PluggableStatEngine pluggableStatEngine;
    private final StatisticsKernel statisticsKernel = new StatisticsKernel();

    private final Label headerTitle;
    private final Label subtitle;
    private final Label comboPrompt;
    private final ComboBox<String> variableCombo;

    private final Label kpiMeanTitle = new Label();
    private final Label kpiVarTitle = new Label();
    private final Label kpiStdTitle = new Label();
    private final Label kpiGiniTitle = new Label();
    private final Label kpiMinMaxTitle = new Label();

    private final HBox meanKpiBox;
    private final HBox varKpiBox;
    private final HBox stdKpiBox;
    private final HBox giniKpiBox;
    private final HBox minMaxKpiBox;

    private final Label lblMean = new Label("--");
    private final Label lblVariance = new Label("--");
    private final Label lblStdDev = new Label("--");
    private final Label lblGini = new Label("--");
    private final Label lblMinMax = new Label("-- / --");
    private final Label lblSpreadDesc = new Label();

    private final CategoryAxis xAxis;
    private final NumberAxis yAxis;
    private final BarChart<String, Number> histogramChart;
    private final XYChart.Series<String, Number> histogramSeries = new XYChart.Series<>();

    private List<H3Cell> currentCells;

    public VarianceDistributionPanel(PluggableStatEngine pluggableStatEngine) {
        this.pluggableStatEngine = pluggableStatEngine;

        getStyleClass().add("card-section");
        setPadding(new Insets(10));
        setSpacing(10);

        // Header
        headerTitle = new Label();
        headerTitle.getStyleClass().add("label-title");

        subtitle = new Label();
        subtitle.getStyleClass().add("hint-label");
        subtitle.setWrapText(true);

        VBox headerBox = new VBox(3, headerTitle, subtitle);

        // Variable Selector
        comboPrompt = new Label();
        comboPrompt.getStyleClass().add("control-label");

        variableCombo = new ComboBox<>();
        variableCombo.setMaxWidth(Double.MAX_VALUE);
        variableCombo.setOnAction(e -> updateData(currentCells));

        // KPI Summary Cards
        GridPane kpiGrid = new GridPane();
        kpiGrid.setHgap(10);
        kpiGrid.setVgap(6);

        meanKpiBox = createMiniKpi(kpiMeanTitle, lblMean);
        varKpiBox = createMiniKpi(kpiVarTitle, lblVariance);
        stdKpiBox = createMiniKpi(kpiStdTitle, lblStdDev);
        giniKpiBox = createMiniKpi(kpiGiniTitle, lblGini);
        minMaxKpiBox = createMiniKpi(kpiMinMaxTitle, lblMinMax);

        kpiGrid.add(meanKpiBox, 0, 0);
        kpiGrid.add(varKpiBox, 1, 0);
        kpiGrid.add(stdKpiBox, 2, 0);
        kpiGrid.add(giniKpiBox, 0, 1);
        kpiGrid.add(minMaxKpiBox, 1, 1, 2, 1);

        lblSpreadDesc.getStyleClass().add("hint-label");
        VBox descCard = new VBox(lblSpreadDesc);
        descCard.getStyleClass().add("hint-card");

        // Histogram BarChart
        xAxis = new CategoryAxis();
        yAxis = new NumberAxis();

        histogramChart = new BarChart<>(xAxis, yAxis);
        histogramChart.setAnimated(false);
        histogramChart.setLegendVisible(false);
        histogramChart.setPrefHeight(180);
        histogramChart.getData().add(histogramSeries);

        getChildren().addAll(headerBox, comboPrompt, variableCombo, kpiGrid, descCard, histogramChart);

        updateTexts();
        I18n.languageProperty().addListener((obs, oldL, newL) -> updateTexts());
    }

    public void updateTexts() {
        headerTitle.setText(I18n.getOrDefault("variance.title", "\uD83D\uDCCA STATISTIQUE DE VARIANCE ENTRE INDIVIDUS & DISTRIBUTION"));
        subtitle.setText(I18n.getOrDefault("variance.subtitle", "\u00C9value \u00E0 quel point les individus / mailles s'\u00E9loignent du sch\u00E9ma standard (Moyenne \u03BC \u00B1 \u00C9cart-type \u03C3)"));
        comboPrompt.setText(I18n.getOrDefault("variance.prompt.variable", "Studied Variable:"));

        int selectedIdx = variableCombo.getSelectionModel().getSelectedIndex();
        variableCombo.getItems().clear();
        variableCombo.getItems().addAll(
                I18n.getOrDefault("variance.var.wealth", "Wealth & Capital (wealth)"),
                I18n.getOrDefault("variance.var.food", "Nourriture disponible (food)"),
                I18n.getOrDefault("variance.var.population", "Population Density (population)"),
                I18n.getOrDefault("variance.var.water", "Water Resources (water)"),
                I18n.getOrDefault("variance.var.rainfall", "Precipitation (rainfall)"),
                I18n.getOrDefault("variance.var.temperature", "Temperature (temperature)"),
                I18n.getOrDefault("variance.var.tech", "Niveau Technologique (tech)"),
                I18n.getOrDefault("variance.var.age", "Approximate Age (age)")
        );
        variableCombo.getSelectionModel().select(selectedIdx >= 0 ? selectedIdx : 0);

        kpiMeanTitle.setText(I18n.getOrDefault("variance.kpi.mean", "Moyenne (μ) :"));
        kpiVarTitle.setText(I18n.getOrDefault("variance.kpi.variance", "Variance (σ²) :"));
        kpiStdTitle.setText(I18n.getOrDefault("variance.kpi.stddev", "Standard Deviation (σ):"));
        kpiGiniTitle.setText(I18n.getOrDefault("variance.kpi.gini", "Indice Gini :"));
        kpiMinMaxTitle.setText(I18n.getOrDefault("variance.kpi.minmax", "Min / Max :"));

        Tooltip.install(meanKpiBox, new Tooltip(I18n.getOrDefault("variance.tooltip.mean", "Valeur moyenne standard de la population")));
        Tooltip.install(varKpiBox, new Tooltip(I18n.getOrDefault("variance.tooltip.variance", "Measure of squared dispersion of individuals relative to mean")));
        Tooltip.install(stdKpiBox, new Tooltip(I18n.getOrDefault("variance.tooltip.stddev", "Average deviation from standard schema (μ ± σ)")));
        Tooltip.install(giniKpiBox, new Tooltip(I18n.getOrDefault("variance.tooltip.gini", "Inequality measure (0 = equal distribution, 1 = total concentration)")));
        Tooltip.install(minMaxKpiBox, new Tooltip(I18n.getOrDefault("variance.tooltip.minmax", "Minimum and maximum extreme values observed")));

        xAxis.setLabel(I18n.getOrDefault("variance.chart.xaxis", "Standard Deviation Bracket Bins (10 Bin Histogram)"));
        yAxis.setLabel(I18n.getOrDefault("variance.chart.yaxis", "Nombre d'Individus / Cellules"));
        histogramChart.setTitle(I18n.getOrDefault("variance.chart.title", "Individual Distribution & Variance Curve"));

        if (currentCells != null) {
            updateData(currentCells);
        } else {
            lblSpreadDesc.setText(I18n.getOrDefault("variance.desc.analyzing", "Analyzing deviation pattern from standard..."));
        }
    }

    private HBox createMiniKpi(Label titleLabel, Label valLabel) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("info-badge");

        titleLabel.getStyleClass().add("control-label");
        valLabel.getStyleClass().add("value-label");

        box.getChildren().addAll(titleLabel, valLabel);
        return box;
    }

    public void updateData(List<H3Cell> cells) {
        this.currentCells = cells;
        String sel = variableCombo.getValue();
        if (sel == null) return;

        String varKey = extractKey(sel);
        float[] values = pluggableStatEngine.extractVariableArray(varKey, cells, null);

        if (values == null || values.length == 0) {
            lblMean.setText("--");
            lblVariance.setText("--");
            lblStdDev.setText("--");
            lblGini.setText("--");
            lblMinMax.setText("-- / --");
            lblSpreadDesc.setText(I18n.getOrDefault("variance.desc.no_data", "No data available for ") + varKey);
            histogramSeries.getData().clear();
            return;
        }

        float[] aggs = statisticsKernel.calculateAggregates(values);
        float avg = aggs[0];
        float min = aggs[1];
        float max = aggs[2];
        float stdDev = aggs[3];
        float variance = stdDev * stdDev;
        float gini = statisticsKernel.calculateGini(values);

        lblMean.setText(String.format("%.2f", avg));
        lblVariance.setText(String.format("%.2f", variance));
        lblStdDev.setText(String.format("%.2f", stdDev));
        lblGini.setText(String.format("%.3f", gini));
        lblMinMax.setText(String.format("%.1f / %.1f", min, max));

        double cv = avg > 0 ? (stdDev / avg) * 100.0 : 0.0;
        String spreadText;
        if (cv > 80.0 || gini > 0.5) {
            spreadText = String.format(I18n.getOrDefault("variance.desc.high", "\uD83D\uDEA8 Forte dispersion : Les individus s'\u00E9loignent tr\u00E8s fortement du sch\u00E9ma standard (\u00C9cart-Type \u03C3 = %.2f, Gini = %.3f). In\u00E9galit\u00E9 tr\u00E8s prononc\u00E9e."), stdDev, gini);
        } else if (cv > 35.0 || gini > 0.25) {
            spreadText = String.format(I18n.getOrDefault("variance.desc.moderate", "\u26A0\uFE0F Dispersion mod\u00E9r\u00E9e : \u00C9cart significatif de la population par rapport au sch\u00E9ma standard (68%% des individus entre %.2f et %.2f)."), Math.max(0, avg - stdDev), avg + stdDev);
        } else {
            spreadText = String.format(I18n.getOrDefault("variance.desc.low", "\u2705 Dispersion faible : Population homog\u00E8ne et tr\u00E8s proche de la moyenne standard (\u03BC = %.2f \u00B1 %.2f)."), avg, stdDev);
        }
        lblSpreadDesc.setText(spreadText);

        histogramSeries.getData().clear();
        int bins = 10;
        int[] dist = statisticsKernel.calculateDistribution(values, bins, max);

        float binWidth = max > 0 ? max / bins : 1.0f;
        for (int i = 0; i < bins; i++) {
            float rangeStart = i * binWidth;
            float rangeEnd = (i + 1) * binWidth;
            String binLabel = String.format("%.0f-%.0f", rangeStart, rangeEnd);
            histogramSeries.getData().add(new XYChart.Data<>(binLabel, dist[i]));
        }
    }

    private String extractKey(String comboText) {
        if (comboText == null) return "wealth";
        if (comboText.contains("wealth") || comboText.contains("Richesse") || comboText.contains("Reichtum") || comboText.contains("Riqueza") || comboText.contains("财富")) return "wealth";
        if (comboText.contains("food") || comboText.contains("Nourriture") || comboText.contains("Nahrung") || comboText.contains("Alimento") || comboText.contains("食物")) return "food";
        if (comboText.contains("population") || comboText.contains("Population") || comboText.contains("Bevölkerung") || comboText.contains("Población") || comboText.contains("人口")) return "population";
        if (comboText.contains("water") || comboText.contains("Eau") || comboText.contains("Wasser") || comboText.contains("Agua") || comboText.contains("水")) return "water";
        if (comboText.contains("rainfall") || comboText.contains("Précipitations") || comboText.contains("Niederschlag") || comboText.contains("Precipitación") || comboText.contains("降水")) return "rainfall";
        if (comboText.contains("temperature") || comboText.contains("Température") || comboText.contains("Temperatur") || comboText.contains("Temperatura") || comboText.contains("温度")) return "temperature";
        if (comboText.contains("tech") || comboText.contains("Technologique") || comboText.contains("Technologie") || comboText.contains("Tecnológico") || comboText.contains("科技")) return "tech";
        if (comboText.contains("age") || comboText.contains("Âge") || comboText.contains("Alter") || comboText.contains("Edad") || comboText.contains("年龄")) return "age";
        return "wealth";
    }
}
