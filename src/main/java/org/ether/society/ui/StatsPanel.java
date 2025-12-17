package org.ether.society.ui;

import org.ether.society.core.H3SimulationEngine;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class StatsPanel extends VBox {
    private final H3SimulationEngine engine;
    private final Label populationLabel;
    private final Label tempLabel;
    private final LineChart<Number, Number> populationChart;
    private final XYChart.Series<Number, Number> populationSeries;

    public StatsPanel(H3SimulationEngine engine) {
        this.engine = engine;
        setPadding(new Insets(10));
        setSpacing(10);
        setPrefWidth(250);
        setStyle("-fx-background-color: #37474f;");

        Label title = new Label("Statistics");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        populationLabel = new Label("Population: 0");
        tempLabel = new Label("Global Temp Offset: 0.0°C");

        // Chart
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Year");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Population");

        populationChart = new LineChart<>(xAxis, yAxis);
        populationChart.setTitle("Population Growth");
        populationChart.setCreateSymbols(false);
        populationChart.setAnimated(false);

        populationSeries = new XYChart.Series<>();
        populationSeries.setName("Humans");
        populationChart.getData().add(populationSeries);

        getChildren().addAll(title, populationLabel, tempLabel, populationChart);
    }

    public void update() {
        if (engine == null)
            return;

        long totalPop = engine.getTotalPopulation();
        int year = engine.getTimeManager().getCurrentYear();

        Platform.runLater(() -> {
            populationLabel.setText("Population: " + totalPop);
            // Temperature offset not exposed directly in engine, omitting for now or
            // calculating average?
            // Simple placeholder for now.

            // Update chart once per year (checking month is 0)
            if (engine.getTimeManager().getCurrentMonth() == 0) {
                // Avoid duplicate points if called frequently
                if (populationSeries.getData().isEmpty() ||
                        populationSeries.getData().get(populationSeries.getData().size() - 1).getXValue()
                                .intValue() != year) {
                    populationSeries.getData().add(new XYChart.Data<>(year, totalPop));
                }
            }
        });
    }
}
