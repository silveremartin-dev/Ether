package org.ether.society.ui;

import org.ether.society.core.H3SimulationEngine;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Panneau de statistiques avancées incluant Gini et Distribution.
 */
public class StatsPanel extends VBox {
    private final H3SimulationEngine engine;
    private final Label populationLabel;
    private final Label biomassLabel;
    private final Label resourcesLabel;
    private final Label techLabel;
    private final Label giniLabel;
    private final Label gdpLabel;
    private final Label fertilityLabel;
    private final Label lifeExpLabel;
    private final Label tempLabel;

    private final LineChart<Number, Number> populationChart;
    private final XYChart.Series<Number, Number> populationSeries;
    
    private final BarChart<String, Number> distributionChart;
    private final XYChart.Series<String, Number> distributionSeries;

    public StatsPanel(H3SimulationEngine engine) {
        this.engine = engine;
        setPadding(new Insets(15));
        setSpacing(12);
        setPrefWidth(300);
        
        // Dark premium style
        setStyle("-fx-background-color: rgba(30, 30, 30, 0.9); " +
                "-fx-border-color: #444; -fx-border-width: 0 0 0 1; " +
                "-fx-text-fill: white;");

        Label title = new Label("SOCIETAL ANALYTICS");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ffd700; -fx-letter-spacing: 2;");

        populationLabel = createStyledLabel("HUMAN POP:   0");
        biomassLabel = createStyledLabel("NATURAL BIO: 0");
        resourcesLabel = createStyledLabel("FOOD STOCK:   0");
        techLabel = createStyledLabel("CIV AGE:      STONE");
        giniLabel = createStyledLabel("GINI COEFF:   0.00");
        gdpLabel = createStyledLabel("GDP (PIB):    0");
        fertilityLabel = createStyledLabel("FERTILITY:    0.0");
        lifeExpLabel = createStyledLabel("LIFE EXPECT:  0.0");
        tempLabel = createStyledLabel("AVG TEMP:     0.0°C");

        // 1. Population Growth Chart
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Year");
        xAxis.setTickLabelFill(javafx.scene.paint.Color.GRAY);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelFill(javafx.scene.paint.Color.GRAY);

        populationChart = new LineChart<>(xAxis, yAxis);
        populationChart.setTitle("Growth Dynamics");
        populationChart.setCreateSymbols(false);
        populationChart.setAnimated(false);
        populationChart.setLegendVisible(false);
        populationChart.setPrefHeight(180);

        populationSeries = new XYChart.Series<>();
        populationChart.getData().add(populationSeries);

        // 2. Density Distribution Chart
        CategoryAxis distX = new CategoryAxis();
        distX.setLabel("Density Range");
        distX.setTickLabelFill(javafx.scene.paint.Color.GRAY);
        NumberAxis distY = new NumberAxis();
        distY.setTickLabelFill(javafx.scene.paint.Color.GRAY);
        
        distributionChart = new BarChart<>(distX, distY);
        distributionChart.setTitle("Density Distribution");
        distributionChart.setAnimated(false);
        distributionChart.setLegendVisible(false);
        distributionChart.setPrefHeight(180);
        distributionChart.setCategoryGap(2);
        
        distributionSeries = new XYChart.Series<>();
        distributionChart.getData().add(distributionSeries);

        getChildren().addAll(title, populationLabel, biomassLabel, resourcesLabel, techLabel, giniLabel, gdpLabel, fertilityLabel, lifeExpLabel, tempLabel, populationChart, distributionChart);
    }

    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #ccc; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        return label;
    }

    public void update() {
        if (engine == null || engine.getWorldBuffer() == null)
            return;

        long totalPop = engine.getTotalPopulation();
        float totalBio = engine.getTotalBiomassNatural();
        double totalFood = engine.getTotalFood();
        float avgTech = engine.getAverageTechnology();
        float gini = engine.getCurrentGini();
        float gdp = engine.getCurrentGDP();
        float fertility = engine.getCurrentFertility();
        float lifeExp = engine.getCurrentLifeExpectancy();
        int[] distribution = engine.getDensityDistribution();
        int year = engine.getTimeManager().getCurrentYear();
        int month = engine.getTimeManager().getCurrentMonth();

        Platform.runLater(() -> {
            populationLabel.setText(String.format("HUMAN POP:   %,d", totalPop));
            biomassLabel.setText(String.format("NATURAL BIO: %,.0f", totalBio));
            resourcesLabel.setText(String.format("FOOD STOCK:  %,.0f", totalFood));
            techLabel.setText(String.format("CIV AGE:     %s (%.1f)", getAgeName(avgTech), avgTech));
            giniLabel.setText(String.format("GINI COEFF:  %.2f", gini));
            gdpLabel.setText(String.format("GDP (PIB):   %,.0f", gdp));
            fertilityLabel.setText(String.format("FERTILITY:   %.1f", fertility));
            lifeExpLabel.setText(String.format("LIFE EXPECT: %.1f", lifeExp));
            
            // Average temp
            float avgTemp = 0;
            float[] temps = engine.getWorldBuffer().getTemperature();
            for(float t : temps) avgTemp += t;
            avgTemp /= temps.length;
            tempLabel.setText(String.format("AVG TEMP:    %.1f°C", avgTemp));

            // Update Charts once per year or every few months
            if (month == 0) {
                // Population Chart
                if (populationSeries.getData().isEmpty() ||
                        populationSeries.getData().get(populationSeries.getData().size() - 1).getXValue()
                                .intValue() != year) {
                    populationSeries.getData().add(new XYChart.Data<>(year, totalPop));
                    if (populationSeries.getData().size() > 50) populationSeries.getData().remove(0);
                }

                // Distribution Chart
                distributionSeries.getData().clear();
                for (int i = 0; i < distribution.length; i++) {
                    distributionSeries.getData().add(new XYChart.Data<>(String.valueOf(i), distribution[i]));
                }
            }
        });
    }

    private String getAgeName(float techLevel) {
        if (techLevel < 10) return "STONE";
        if (techLevel < 30) return "BRONZE";
        if (techLevel < 60) return "IRON";
        if (techLevel < 100) return "CLASSICAL";
        if (techLevel < 200) return "MEDIEVAL";
        return "RENAISSANCE";
    }
}
