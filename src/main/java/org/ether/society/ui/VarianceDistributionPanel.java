/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.core.dod.PluggableStatEngine;
import org.ether.society.core.dod.StatisticsKernel;
import org.ether.society.database.H3Cell;

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

    private final ComboBox<String> variableCombo;
    private final Label lblMean = new Label("--");
    private final Label lblVariance = new Label("--");
    private final Label lblStdDev = new Label("--");
    private final Label lblGini = new Label("--");
    private final Label lblMinMax = new Label("-- / --");
    private final Label lblSpreadDesc = new Label("Analyse du schéma d'écart au standard...");

    private final BarChart<String, Number> histogramChart;
    private final XYChart.Series<String, Number> histogramSeries = new XYChart.Series<>();

    public VarianceDistributionPanel(PluggableStatEngine pluggableStatEngine) {
        this.pluggableStatEngine = pluggableStatEngine;

        setPadding(new Insets(10));
        setSpacing(10);
        getStyleClass().add("glass-panel");
        setStyle("-fx-background-color: rgba(15, 23, 42, 0.85); -fx-background-radius: 8; -fx-border-color: rgba(56, 189, 248, 0.25); -fx-border-radius: 8;");

        // Header
        Label headerTitle = new Label("📊 STATISTIQUE DE VARIANCE ENTRE INDIVIDUS & DISTRIBUTION");
        headerTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        Label subtitle = new Label("Évalue à quel point les individus / mailles s'éloignent du schéma standard (Moyenne μ ± Écart-type σ)");
        subtitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        subtitle.setWrapText(true);

        VBox headerBox = new VBox(3, headerTitle, subtitle);

        // Variable Selector
        Label comboPrompt = new Label("Variable Étudiée :");
        comboPrompt.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        variableCombo = new ComboBox<>();
        variableCombo.getItems().addAll(
                "Richesse & Capital (wealth)",
                "Nourriture disponible (food)",
                "Densité de Population (population)",
                "Ressources en Eau (water)",
                "Précipitations (rainfall)",
                "Température (temperature)",
                "Niveau Technologique (tech)",
                "Âge Approximatif (age)"
        );
        variableCombo.setValue("Richesse & Capital (wealth)");
        variableCombo.setMaxWidth(Double.MAX_VALUE);
        variableCombo.setOnAction(e -> updateData(null));

        // KPI Summary Cards
        GridPane kpiGrid = new GridPane();
        kpiGrid.setHgap(10);
        kpiGrid.setVgap(6);

        kpiGrid.add(createMiniKpi("Moyenne (μ) :", lblMean, "Valeur moyenne standard de la population"), 0, 0);
        kpiGrid.add(createMiniKpi("Variance (σ²) :", lblVariance, "Mesure de la dispersion au carré des individus par rapport à la moyenne"), 1, 0);
        kpiGrid.add(createMiniKpi("Écart-Type (σ) :", lblStdDev, "Écart moyen au schéma standard (μ ± σ)"), 2, 0);

        kpiGrid.add(createMiniKpi("Indice Gini :", lblGini, "Mesure d'inégalité (0 = répartition égale, 1 = concentration totale)"), 0, 1);
        kpiGrid.add(createMiniKpi("Min / Max :", lblMinMax, "Valeurs extrêmes minimale et maximale observées"), 1, 1, 2, 1);

        lblSpreadDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1; -fx-font-style: italic;");
        VBox descCard = new VBox(lblSpreadDesc);
        descCard.setStyle("-fx-padding: 6 10; -fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.05);");

        // Histogram BarChart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Tranches d'Écart au Schéma Standard (Histogramme 10 Bins)");
        xAxis.setStyle("-fx-tick-label-fill: #94a3b8;");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Nombre d'Individus / Cellules");
        yAxis.setStyle("-fx-tick-label-fill: #94a3b8;");

        histogramChart = new BarChart<>(xAxis, yAxis);
        histogramChart.setTitle("Courbe de Répartition et Variance des Individus");
        histogramChart.setAnimated(false);
        histogramChart.setLegendVisible(false);
        histogramChart.setPrefHeight(180);
        histogramChart.getData().add(histogramSeries);

        getChildren().addAll(headerBox, comboPrompt, variableCombo, kpiGrid, descCard, histogramChart);
    }

    private HBox createMiniKpi(String title, Label valLabel, String tooltip) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-padding: 4 8; -fx-background-radius: 4;");

        Label tLbl = new Label(title);
        tLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        valLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        box.getChildren().addAll(tLbl, valLabel);
        Tooltip.install(box, new Tooltip(tooltip));
        return box;
    }

    public void updateData(List<H3Cell> cells) {
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
            lblSpreadDesc.setText("Aucune donnée disponible pour " + varKey);
            histogramSeries.getData().clear();
            return;
        }

        // Calculate aggregates using StatisticsKernel
        float[] aggs = statisticsKernel.calculateAggregates(values); // avg, min, max, stdDev
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

        // Evaluate spread narrative description
        double cv = avg > 0 ? (stdDev / avg) * 100.0 : 0.0; // Coefficient of Variation
        String spreadText;
        if (cv > 80.0 || gini > 0.5) {
            spreadText = String.format("🚨 Forte dispersion : Les individus s'éloignent très fortement du schéma standard (Écart-Type σ = %.2f, Gini = %.3f). Inégalité très prononcée.", stdDev, gini);
        } else if (cv > 35.0 || gini > 0.25) {
            spreadText = String.format("⚠️ Dispersion modérée : Écart significatif de la population par rapport au schéma standard (68%% des individus entre %.2f et %.2f).", Math.max(0, avg - stdDev), avg + stdDev);
        } else {
            spreadText = String.format("✅ Dispersion faible : Population homogène et très proche de la moyenne standard (μ = %.2f ± %.2f).", avg, stdDev);
        }
        lblSpreadDesc.setText(spreadText);

        // Update 10-bin distribution histogram
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
        if (comboText.contains("wealth")) return "wealth";
        if (comboText.contains("food")) return "food";
        if (comboText.contains("population")) return "population";
        if (comboText.contains("water")) return "water";
        if (comboText.contains("rainfall")) return "rainfall";
        if (comboText.contains("temperature")) return "temperature";
        if (comboText.contains("tech")) return "tech";
        if (comboText.contains("age")) return "age";
        return "wealth";
    }
}
