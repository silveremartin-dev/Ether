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
import javafx.scene.control.Tooltip;
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
    private final XYChart.Series<Number, Number> populationSeries = new XYChart.Series<>();
    
    private final BarChart<String, Number> distributionChart;
    private final XYChart.Series<String, Number> distributionSeries = new XYChart.Series<>();

    private final Label ipsLabel;
    private final Label memoryLabel;
    private final Label cellsCountLabel;
    private final Label cameraInfoLabel;

    public StatsPanel(H3SimulationEngine engine) {
        this.engine = engine;
        setPadding(new Insets(15));
        setSpacing(12);
        setPrefWidth(320);
        
        // Dark premium style
        setStyle("-fx-background-color: rgba(15, 23, 42, 0.95); " +
                "-fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-width: 0 0 0 1; " +
                "-fx-text-fill: white;");

        Label title = new Label("📊 STATISTIQUES SOCIÉTALES");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #ffd700; -fx-letter-spacing: 1;");

        populationLabel = createStyledLabel("POPULATION HUMAINE:  0");
        biomassLabel = createStyledLabel("BIOMASSE NATURELLE:  0");
        resourcesLabel = createStyledLabel("STOCKS ALIMENTAIRES: 0");
        techLabel = createStyledLabel("ÈRE CIVILISATION:    ÂGE DE LA PIERRE");
        giniLabel = createStyledLabel("INDICE DE GINI:      0.00");
        gdpLabel = createStyledLabel("PIB GLOBAL (GDP):    0");
        fertilityLabel = createStyledLabel("TAUX DE FÉCONDITÉ:   0.0");
        lifeExpLabel = createStyledLabel("ESPÉRANCE DE VIE:    0.0 ans");
        tempLabel = createStyledLabel("TEMPÉRATURE MOYENNE: 0.0°C");

        // Technical Stats Section
        Label techTitle = new Label("⚙️ STATISTIQUES TECHNIQUES");
        techTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-letter-spacing: 1; -fx-padding: 8 0 0 0;");

        ipsLabel = createStyledLabel("IPS (Rendu 3D):     -- FPS");
        memoryLabel = createStyledLabel("MÉMOIRE RAM:        -- MB");
        cellsCountLabel = createStyledLabel("CELLULES H3 CARTE:   0");
        cameraInfoLabel = createStyledLabel("ZOOM & POSITION:     1.0x (0°N, 0°E)");

        // 1. Population Growth Chart
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Année");
        xAxis.setTickLabelFill(javafx.scene.paint.Color.GRAY);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Population");
        yAxis.setTickLabelFill(javafx.scene.paint.Color.GRAY);

        populationChart = new LineChart<>(xAxis, yAxis);
        populationChart.setTitle("Dynamique de Croissance");
        populationChart.setCreateSymbols(false);
        populationChart.setAnimated(false);
        populationChart.setLegendVisible(false);
        populationChart.setPrefHeight(160);

        populationChart.getData().add(populationSeries);

        // 2. Density Distribution Chart
        CategoryAxis distX = new CategoryAxis();
        distX.setLabel("Tranche de Densité");
        distX.setTickLabelFill(javafx.scene.paint.Color.GRAY);
        NumberAxis distY = new NumberAxis();
        distY.setLabel("Nombre de Cellules");
        distY.setTickLabelFill(javafx.scene.paint.Color.GRAY);
        
        distributionChart = new BarChart<>(distX, distY);
        distributionChart.setTitle("Distribution de Densité");
        distributionChart.setAnimated(false);
        distributionChart.setLegendVisible(false);
        distributionChart.setPrefHeight(160);
        distributionChart.setCategoryGap(2);
        
        distributionChart.getData().add(distributionSeries);

        Tooltip.install(populationChart, new Tooltip("Graphique de l'évolution temporelle de la population humaine globale au fil des ans."));
        Tooltip.install(distributionChart, new Tooltip("Histogramme de répartition du nombre de cellules hexagonales H3 selon la tranche de densité démographique."));

        javafx.scene.control.Button exportBtn = new javafx.scene.control.Button("📥 Exporter Données (CSV)");
        exportBtn.setTooltip(new Tooltip("Exporter l'intégralité des métriques historiques démographiques, économiques et climatiques au format CSV / Excel."));
        exportBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 6;");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        exportBtn.setOnAction(e -> exportCsv());

        getChildren().addAll(
            title, exportBtn, populationLabel, biomassLabel, resourcesLabel, techLabel, giniLabel, gdpLabel, fertilityLabel, lifeExpLabel, tempLabel,
            techTitle, ipsLabel, memoryLabel, cellsCountLabel, cameraInfoLabel,
            populationChart, distributionChart
        );
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
            populationLabel.setText(String.format("POPULATION HUMAINE:  %,d", totalPop));
            biomassLabel.setText(String.format("BIOMASSE NATURELLE:  %,.0f", totalBio));
            resourcesLabel.setText(String.format("STOCKS ALIMENTAIRES: %,.0f", totalFood));
            techLabel.setText(String.format("ÈRE CIVILISATION:    %s (%.1f)", getAgeName(avgTech), avgTech));
            giniLabel.setText(String.format("INDICE DE GINI:      %.2f", gini));
            gdpLabel.setText(String.format("PIB GLOBAL (GDP):    %,.0f", gdp));
            fertilityLabel.setText(String.format("TAUX DE FÉCONDITÉ:   %.1f enfants/femme", fertility));
            lifeExpLabel.setText(String.format("ESPÉRANCE DE VIE:    %.1f ans", lifeExp));
            
            // Average temp
            float avgTemp = 0;
            float[] temps = engine.getWorldBuffer().getTemperature();
            if (temps != null && temps.length > 0) {
                for(float t : temps) avgTemp += t;
                avgTemp /= temps.length;
            }
            tempLabel.setText(String.format("TEMPÉRATURE MOYENNE: %.1f°C", avgTemp));

            // Technical RAM stats
            Runtime runtime = Runtime.getRuntime();
            long usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            long totalMem = runtime.totalMemory() / (1024 * 1024);
            double tps = engine.getCurrentTPS();
            int totalCells = engine.getCells() != null ? engine.getCells().size() : 0;
            
            ipsLabel.setText(String.format("IPS (Images/TPS):   %.1f", tps));
            memoryLabel.setText(String.format("MÉMOIRE RAM:        %d MB / %d MB", usedMem, totalMem));
            cellsCountLabel.setText(String.format("CELLULES H3 CARTE:   %,d hexagones", totalCells));

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

    public void updateCameraInfo(double zoom, double centerLat, double centerLng) {
        Platform.runLater(() -> {
            cameraInfoLabel.setText(String.format("ZOOM & POSITION:     %.1fx (%.2f°N, %.2f°E)", zoom, centerLat, centerLng));
        });
    }

    private String getAgeName(float techLevel) {
        if (techLevel < 10) return "ÂGE DE LA PIERRE";
        if (techLevel < 30) return "ÂGE DU BRONZE";
        if (techLevel < 60) return "ÂGE DU FER";
        if (techLevel < 100) return "ÈRE CLASSIQUE";
        if (techLevel < 200) return "ÈRE MÉDIÉVALE";
        return "RENAISSANCE";
    }

    private void exportCsv() {
        if (engine == null) return;
        
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Exporter les Statistiques Simulation (CSV / Excel)");
        fileChooser.setInitialFileName("ether_statistiques_simulation.csv");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichiers CSV (*.csv)", "*.csv"));
        
        java.io.File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file == null) return;
        
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            writer.println("Annee;Mois;Population_Humaine;Biomasse_Naturelle;Stocks_Alimentaires;PIB_Global;Indice_Gini;Fecondite;Esperance_Vie;Niveau_Technologique");
            
            var history = engine.getHistoryManager() != null ? engine.getHistoryManager().getHistory() : null;
            var snapshots = history != null ? history.getSnapshots() : null;
            
            if (snapshots != null && !snapshots.isEmpty()) {
                for (var s : snapshots) {
                    writer.println(String.format(java.util.Locale.US, "%d;%d;%d;0.00;%.2f;%.2f;%.4f;0.00;%.2f;%.2f",
                        s.year(), s.month(), s.totalPopulation(), s.totalFood(), s.totalWealth(), s.globalGini(), s.avgLifespan(), s.avgTechnology()));
                }
            } else {
                // Fallback to current live snapshot
                int y = engine.getTimeManager().getCurrentYear();
                int m = engine.getTimeManager().getCurrentMonth();
                long pop = engine.getTotalPopulation();
                float bio = engine.getTotalBiomassNatural();
                double food = engine.getTotalFood();
                float gdp = engine.getCurrentGDP();
                float gini = engine.getCurrentGini();
                float fert = engine.getCurrentFertility();
                float life = engine.getCurrentLifeExpectancy();
                float tech = engine.getAverageTechnology();
                writer.println(String.format(java.util.Locale.US, "%d;%d;%d;%.2f;%.2f;%.2f;%.4f;%.2f;%.2f;%.2f",
                    y, m, pop, bio, food, gdp, gini, fert, life, tech));
            }
            
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Exportation Réussie");
            alert.setHeaderText(null);
            alert.setContentText("Les données statistiques ont été exportées avec succès dans :\n" + file.getAbsolutePath());
            alert.showAndWait();
        } catch (Exception ex) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erreur d'Exportation");
            alert.setContentText("Impossible d'exporter les statistiques : " + ex.getMessage());
            alert.showAndWait();
        }
    }
}
