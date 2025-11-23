package com.ether.society.ui;

import com.ether.society.SimulationEngine;
import com.ether.society.model.World;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class StatsPanel extends VBox {
    private final SimulationEngine engine;
    private final Label populationLabel;
    private final Label tempLabel;
    private final LineChart<Number, Number> populationChart;
    private final XYChart.Series<Number, Number> populationSeries;

    public StatsPanel(SimulationEngine engine) {
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
        World world = engine.getWorld();
        int pop = world.getAgents().size(); // Assuming all agents are humans for now, or filter
        
        Platform.runLater(() -> {
            populationLabel.setText("Population: " + pop);
            tempLabel.setText(String.format("Global Temp Offset: %.2f°C", world.getGlobalTemperatureOffset()));
            
            int year = engine.getTimeManager().getCurrentYear();
            // Add data point every year or so to avoid clutter
            if (engine.getTimeManager().getCurrentMonth() == 0) {
                 populationSeries.getData().add(new XYChart.Data<>(year, pop));
            }
        });
    }
}
