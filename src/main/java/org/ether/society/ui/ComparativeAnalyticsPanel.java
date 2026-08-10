/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.analytics.*;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.persistence.ScenarioRepository;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * 6th Tab UI Panel: Comparative Analytics & Sensitivity Benchmarks.
 * Supports multi-scenario selection, execution status validation, automated headless
 * batch execution of missing scenarios, multi-curve visualization, and root cause analysis.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.5.0
 */
public class ComparativeAnalyticsPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ComparativeAnalyticsPanel.class);

    public static class ScenarioSelectableItem {
        private final Scenario scenario;
        private boolean selected;
        private boolean executed;
        private String runId;

        public ScenarioSelectableItem(Scenario scenario, boolean selected, boolean executed, String runId) {
            this.scenario = scenario;
            this.selected = selected;
            this.executed = executed;
            this.runId = runId;
        }

        public Scenario getScenario() { return scenario; }
        public String getName() { return scenario.getName(); }
        public String getYearRange() { return "An " + scenario.getStartDateYear() + " ➔ " + scenario.getEndDateYear(); }
        public boolean isSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
        public boolean isExecuted() { return executed; }
        public void setExecuted(boolean executed) { this.executed = executed; }
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        public String getStatusDisplay() { return executed ? "🟢 Exécuté (" + runId + ")" : "🔴 Non exécuté (À lancer)"; }
    }

    private final SimulationRunRepository runRepository;
    private final ScenarioRepository scenarioRepository;
    private final RootCauseAnalyzer analyzer;

    private TableView<ScenarioSelectableItem> scenarioTable;
    private ObservableList<ScenarioSelectableItem> scenarioList;

    private ComboBox<String> metricSelectorCombo;
    private LineChart<Number, Number> chart;
    private Label warningLabel;
    private Button executeMissingBtn;

    private Label divergenceLabel;
    private Label explanationLabel;
    private TextArea reportPreviewArea;

    public ComparativeAnalyticsPanel() {
        this.runRepository = SimulationRunRepository.getInstance();
        this.scenarioRepository = new ScenarioRepository();
        this.analyzer = new RootCauseAnalyzer();

        getStyleClass().add("glass-panel");
        setPadding(new Insets(15));

        initUI();
        updateTexts();

        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    private void initUI() {
        VBox topBox = new VBox(10);
        topBox.setPadding(new Insets(0, 0, 10, 0));

        // Header Title
        Label headerLabel = new Label("📊 ANALYSE COMPARATIVE & BATAILLE DE SCÉNARIOS (DEEP ANALYTICS)");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #38bdf8;");

        // Action Toolbar
        metricSelectorCombo = new ComboBox<>();
        metricSelectorCombo.getItems().addAll("Population Totale", "Nourriture / Subsistance", "Niveau Technologique", "Indice de Stabilité");
        metricSelectorCombo.setValue("Population Totale");
        metricSelectorCombo.setOnAction(e -> updateChartAndAnalysis());

        Button analyzeBtn = new Button("⚡ Calculer les Écarts & Cause Racinaire");
        analyzeBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 6 14;");
        analyzeBtn.setOnAction(e -> runAnalysis());

        executeMissingBtn = new Button("🚀 Exécuter les Scénarios Manquants (Headless)");
        executeMissingBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 6 14;");
        executeMissingBtn.setOnAction(e -> executeMissingScenarios());
        executeMissingBtn.setVisible(false);
        executeMissingBtn.setManaged(false);

        Button exportMdBtn = new Button("📝 Exporter Rapport (.md)");
        exportMdBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 6 14;");
        exportMdBtn.setOnAction(e -> exportMarkdownReport());

        Button exportCsvBtn = new Button("📥 Exporter Données (.csv)");
        exportCsvBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-padding: 6 14;");
        exportCsvBtn.setOnAction(e -> exportCsvData());

        HBox actionsBox = new HBox(10, new Label("Indicateur Visualisé :"), metricSelectorCombo, analyzeBtn, executeMissingBtn, exportMdBtn, exportCsvBtn);
        actionsBox.setAlignment(Pos.CENTER_LEFT);

        // Status warning bar for missing runs
        warningLabel = new Label("");
        warningLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f59e0b; -fx-padding: 4 8; -fx-background-color: rgba(245, 158, 11, 0.1); -fx-background-radius: 4;");
        warningLabel.setVisible(false);
        warningLabel.setManaged(false);

        topBox.getChildren().addAll(headerLabel, actionsBox, warningLabel);
        setTop(topBox);

        // Center SplitPane: Left (Scenario Table + Chart), Right (Diagnostics & Report Preview)
        SplitPane mainSplit = new SplitPane();
        mainSplit.setStyle("-fx-background-color: transparent;");

        VBox leftPane = new VBox(10);
        
        // Scenario Selection Table
        Label tableTitle = new Label("📋 Sélection des Scénarios de la Base de Données à Comparer :");
        tableTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        scenarioTable = new TableView<>();
        scenarioTable.setPrefHeight(150);
        scenarioTable.setEditable(true);

        TableColumn<ScenarioSelectableItem, Boolean> selectCol = new TableColumn<>("Comparer");
        selectCol.setCellValueFactory(p -> {
            var item = p.getValue();
            var prop = javafx.beans.binding.Bindings.createBooleanBinding(item::isSelected);
            return prop;
        });
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setOnEditCommit(e -> {
            e.getRowValue().setSelected(e.getNewValue());
            checkExecutionStatus();
            updateChartAndAnalysis();
        });
        selectCol.setPrefWidth(80);

        TableColumn<ScenarioSelectableItem, String> nameCol = new TableColumn<>("Nom du Scénario");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(220);

        TableColumn<ScenarioSelectableItem, String> yearsCol = new TableColumn<>("Plage Chronologique");
        yearsCol.setCellValueFactory(new PropertyValueFactory<>("yearRange"));
        yearsCol.setPrefWidth(140);

        TableColumn<ScenarioSelectableItem, String> statusCol = new TableColumn<>("Statut Exécution BD");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("statusDisplay"));
        statusCol.setPrefWidth(220);

        scenarioTable.getColumns().addAll(selectCol, nameCol, yearsCol, statusCol);

        scenarioList = FXCollections.observableArrayList();
        scenarioTable.setItems(scenarioList);

        // Multi-curve Line Chart
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Années de Simulation (Ticks)");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Valeur de l'Indicateur");

        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Superposition Chronologique Multi-Scénarios");
        chart.setAnimated(false);
        chart.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); -fx-background-radius: 8;");
        VBox.setVgrow(chart, Priority.ALWAYS);

        leftPane.getChildren().addAll(tableTitle, scenarioTable, chart);

        // Right Diagnostics & Report Area
        VBox diagBox = new VBox(10);
        diagBox.setPadding(new Insets(12));
        diagBox.setStyle("-fx-background-color: rgba(15, 23, 42, 0.85); -fx-background-radius: 8; -fx-border-color: #38bdf8; -fx-border-radius: 8;");

        Label diagHeader = new Label("🔍 ANALYSE DE DIVERGENCE & ANATOMIE DES ÉCARTS");
        diagHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #38bdf8;");

        divergenceLabel = new Label("Point de rupture : Sélectionnez au moins 2 scénarios");
        divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f59e0b;");

        explanationLabel = new Label("Cochez les scénarios dans la liste ci-dessus pour lancer la comparaison.");
        explanationLabel.setWrapText(true);
        explanationLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        reportPreviewArea = new TextArea();
        reportPreviewArea.setEditable(false);
        reportPreviewArea.setStyle("-fx-control-inner-background: #090d16; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        VBox.setVgrow(reportPreviewArea, Priority.ALWAYS);

        diagBox.getChildren().addAll(diagHeader, divergenceLabel, explanationLabel, new Label("📄 Synthèse Comparative Auto-Générée :"), reportPreviewArea);

        mainSplit.getItems().addAll(leftPane, diagBox);
        mainSplit.setDividerPositions(0.60);

        setCenter(mainSplit);

        refreshRunList();
    }

    public void refreshRunList() {
        scenarioList.clear();
        List<Scenario> allScenarios = scenarioRepository.findAll();

        // Ensure built-in standard benchmark scenarios exist if DB is empty
        if (allScenarios.isEmpty()) {
            Scenario defaultScen = Scenario.createDefaultScenario();
            defaultScen.setName("Empire Romain An 0 (Standard)");
            defaultScen.setStartDateYear(0);
            defaultScen.setEndDateYear(100);
            scenarioRepository.saveOrUpdate(defaultScen);
            allScenarios = scenarioRepository.findAll();
        }

        int count = 0;
        for (Scenario sc : allScenarios) {
            Optional<SimulationRunRecord> existingRun = runRepository.getRunByScenarioName(sc.getName());
            boolean isExec = existingRun.isPresent();
            String rId = existingRun.map(SimulationRunRecord::getRunId).orElse("N/A");

            // Select first 2 or 3 scenarios by default
            boolean isSel = (count < 3);
            scenarioList.add(new ScenarioSelectableItem(sc, isSel, isExec, rId));
            count++;
        }

        checkExecutionStatus();
        updateChartAndAnalysis();
    }

    private void checkExecutionStatus() {
        List<ScenarioSelectableItem> selectedItems = scenarioList.stream().filter(ScenarioSelectableItem::isSelected).toList();
        List<ScenarioSelectableItem> missingExec = selectedItems.stream().filter(i -> !i.isExecuted()).toList();

        if (!missingExec.isEmpty()) {
            warningLabel.setText(String.format("⚠️ %d scénario(s) sélectionné(s) n'a/ont pas encore de données d'exécution enregistrées dans la base de données !", missingExec.size()));
            warningLabel.setVisible(true);
            warningLabel.setManaged(true);

            executeMissingBtn.setText(String.format("🚀 Exécuter %d Scénario(s) Manquant(s) (Headless)", missingExec.size()));
            executeMissingBtn.setVisible(true);
            executeMissingBtn.setManaged(true);
        } else {
            warningLabel.setVisible(false);
            warningLabel.setManaged(false);
            executeMissingBtn.setVisible(false);
            executeMissingBtn.setManaged(false);
        }
    }

    private void executeMissingScenarios() {
        List<ScenarioSelectableItem> unexecuted = scenarioList.stream()
            .filter(i -> i.isSelected() && !i.isExecuted())
            .toList();

        for (ScenarioSelectableItem item : unexecuted) {
            logger.info("Executing missing scenario headlessly: {}", item.getName());
            SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(item.getScenario());
            item.setExecuted(true);
            item.setRunId(record.getRunId());
        }

        scenarioTable.refresh();
        checkExecutionStatus();
        runAnalysis();
    }

    private void runAnalysis() {
        List<ScenarioSelectableItem> selectedExecuted = scenarioList.stream()
            .filter(i -> i.isSelected() && i.isExecuted())
            .toList();

        if (selectedExecuted.size() < 2) {
            divergenceLabel.setText("⚠️ Sélectionnez au moins 2 scénarios exécutés pour la comparaison");
            divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f59e0b;");
            explanationLabel.setText("Veuillez cocher au moins 2 scénarios déjà exécutés ou lancer leur exécution.");
            return;
        }

        SimulationRunRecord baseline = runRepository.getRun(selectedExecuted.get(0).getRunId());
        SimulationRunRecord target = runRepository.getRun(selectedExecuted.get(1).getRunId());

        if (baseline != null && target != null) {
            RootCauseAnalyzer.ComparisonResult result = analyzer.compareRuns(baseline, target);

            if (result.getDivergenceYear() != -1) {
                divergenceLabel.setText(String.format("⚠️ Point de rupture majeur (T_divergence) : ANNEE %d", result.getDivergenceYear()));
                divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #ef4444; -fx-font-size: 13px;");
            } else {
                divergenceLabel.setText("✅ Trajectoires parallèles (Aucune rupture majeure > 5%)");
                divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #10b981; -fx-font-size: 13px;");
            }

            explanationLabel.setText(result.getPrimaryRootCauseExplanation());
            String markdownReport = ComparativeReportGenerator.generateMarkdownReport(result);
            reportPreviewArea.setText(markdownReport);
        }

        updateChartAndAnalysis();
    }

    private void updateChartAndAnalysis() {
        chart.getData().clear();

        List<ScenarioSelectableItem> selectedExecuted = scenarioList.stream()
            .filter(i -> i.isSelected() && i.isExecuted())
            .toList();

        String metric = metricSelectorCombo.getValue();
        if (metric == null || selectedExecuted.isEmpty()) return;

        for (ScenarioSelectableItem item : selectedExecuted) {
            SimulationRunRecord record = runRepository.getRun(item.getRunId());
            if (record == null) {
                record = runRepository.getRunByScenarioName(item.getName()).orElse(null);
            }
            if (record != null) {
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName(item.getName());

                for (var entry : record.getTimeSeriesData().entrySet()) {
                    series.getData().add(new XYChart.Data<>(entry.getKey(), extractValue(entry.getValue(), metric)));
                }
                chart.getData().add(series);
            }
        }
    }

    private double extractValue(SimulationRunRecord.MetricSnapshot snap, String metric) {
        if (metric == null) return 0.0;
        return switch (metric) {
            case "Nourriture / Subsistance" -> snap.getFood();
            case "Niveau Technologique" -> snap.getAvgTech();
            case "Indice de Stabilité" -> snap.getStability();
            default -> (double) snap.getPopulation();
        };
    }

    private void exportMarkdownReport() {
        List<ScenarioSelectableItem> selectedExecuted = scenarioList.stream()
            .filter(i -> i.isSelected() && i.isExecuted())
            .toList();

        if (selectedExecuted.size() < 2) return;

        SimulationRunRecord baseline = runRepository.getRun(selectedExecuted.get(0).getRunId());
        SimulationRunRecord target = runRepository.getRun(selectedExecuted.get(1).getRunId());
        if (baseline == null || target == null) return;

        RootCauseAnalyzer.ComparisonResult result = analyzer.compareRuns(baseline, target);
        String md = ComparativeReportGenerator.generateMarkdownReport(result);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le Rapport d'Analyse Comparative");
        fileChooser.setInitialFileName("Rapport_Analyse_Comparative.md");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Markdown (*.md)", "*.md"));

        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(md);
                logger.info("Exported comparative analysis report to {}", file.getAbsolutePath());
            } catch (IOException ex) {
                logger.error("Error writing markdown report to file", ex);
            }
        }
    }

    private void exportCsvData() {
        String csv = ComparativeReportGenerator.generateCsvExport(runRepository.getAllRuns());

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les Données Téléométriques (CSV)");
        fileChooser.setInitialFileName("Export_Donnees_Simulation.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(csv);
                logger.info("Exported CSV telemetry data to {}", file.getAbsolutePath());
            } catch (IOException ex) {
                logger.error("Error writing CSV export to file", ex);
            }
        }
    }

    public void updateTexts() {
        // i18n dynamic updates
    }
}
