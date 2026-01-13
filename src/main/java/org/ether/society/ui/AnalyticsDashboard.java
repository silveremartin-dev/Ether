package org.ether.society.ui;


import javafx.geometry.Insets;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.ether.society.analytics.HistorySnapshot;
import org.ether.society.analytics.SimulationHistory;

import java.util.List;

/**
 * Dashboard for visualizing simulation history.
 */
public class AnalyticsDashboard extends VBox {

    private final SimulationHistory history;

    // Charts
    private LineChart<String, Number> popChart;
    private LineChart<String, Number> econChart;
    private LineChart<String, Number> qualityChart;

    // Data Series
    private XYChart.Series<String, Number> popSeries;
    private XYChart.Series<String, Number> foodSeries;
    private XYChart.Series<String, Number> capitalSeries;
    private XYChart.Series<String, Number> lifespanSeries;
    private XYChart.Series<String, Number> techSeries;

    public AnalyticsDashboard(SimulationHistory history) {
        this.history = history;
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        createCharts();

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab tab1 = new Tab("Demographics", popChart);
        Tab tab2 = new Tab("Economy", econChart);
        Tab tab3 = new Tab("Quality of Life", qualityChart);

        tabPane.getTabs().addAll(tab1, tab2, tab3);

        this.getChildren().add(tabPane);

        // Initial setup
        refresh();
    }

    @SuppressWarnings("unchecked")
    private void createCharts() {
        // 1. Population Chart
        CategoryAxis xAxis1 = new CategoryAxis();
        xAxis1.setLabel("Year");
        NumberAxis yAxis1 = new NumberAxis();
        yAxis1.setLabel("Population");

        popChart = new LineChart<>(xAxis1, yAxis1);
        popChart.setTitle("Global Population");
        popChart.setAnimated(false); // Disable animation for performance with many points

        popSeries = new XYChart.Series<>();
        popSeries.setName("Total Population");
        popChart.getData().add(popSeries);

        // 2. Economy Chart
        CategoryAxis xAxis2 = new CategoryAxis();
        NumberAxis yAxis2 = new NumberAxis();
        econChart = new LineChart<>(xAxis2, yAxis2);
        econChart.setTitle("Global Economy");
        econChart.setAnimated(false);

        foodSeries = new XYChart.Series<>();
        foodSeries.setName("Food Reserves");
        capitalSeries = new XYChart.Series<>();
        capitalSeries.setName("Total Capital");

        econChart.getData().addAll(foodSeries, capitalSeries);

        // 3. Quality of Life
        CategoryAxis xAxis3 = new CategoryAxis();
        NumberAxis yAxis3 = new NumberAxis();
        qualityChart = new LineChart<>(xAxis3, yAxis3);
        qualityChart.setTitle("Quality of Life");
        qualityChart.setAnimated(false);

        lifespanSeries = new XYChart.Series<>();
        lifespanSeries.setName("Avg Lifespan");
        techSeries = new XYChart.Series<>();
        techSeries.setName("Global Tech Level");

        qualityChart.getData().addAll(lifespanSeries, techSeries);
    }

    /**
     * Refresh data from history.
     * This should be called periodically by the UI loop, not every tick.
     */
    public void refresh() {
        List<HistorySnapshot> snapshots = history.getSnapshots();

        // Performance optimization: If too many points, sample them.
        // For MVP, simplistic redraw or append.
        // JavaFX Charts get slow with >1000 points.

        // Simple strategy: Clear and Rebuild (safest for sync) or selective update.
        // Let's rebuild for correctness first.

        popSeries.getData().clear();
        foodSeries.getData().clear();
        capitalSeries.getData().clear();
        lifespanSeries.getData().clear();
        techSeries.getData().clear();

        // Decimation Factor
        int step = Math.max(1, snapshots.size() / 200); // Max 200 points

        for (int i = 0; i < snapshots.size(); i += step) {
            HistorySnapshot s = snapshots.get(i);
            String label = String.valueOf(s.year());

            popSeries.getData().add(new XYChart.Data<>(label, s.totalPopulation()));
            foodSeries.getData().add(new XYChart.Data<>(label, s.totalFood()));
            capitalSeries.getData().add(new XYChart.Data<>(label, s.totalWealth()));
            lifespanSeries.getData().add(new XYChart.Data<>(label, s.avgLifespan()));
            techSeries.getData().add(new XYChart.Data<>(label, s.avgTechnology()));
        }
    }
}
