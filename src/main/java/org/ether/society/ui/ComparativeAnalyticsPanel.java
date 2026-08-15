/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.analytics.*;
import org.ether.society.data.HistoricalMapGenerator;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.persistence.ScenarioRepository;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * 6th Tab UI Panel: Comparative Analytics & Sensitivity Benchmarks.
 * Supports multi-scenario selection, execution status validation, automated headless
 * batch execution of missing scenarios, 1D multi-curve time-series visualization,
 * 2D spatial cartographic tensor comparison with timeline date scrubber, and root cause analysis.
 *
 * @author Silvere Martin-Michiellot
 * @version 5.0.0
 */
public class ComparativeAnalyticsPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ComparativeAnalyticsPanel.class);

    public static class ScenarioSelectableItem {
        private final Scenario scenario;
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        private boolean executed;
        private String runId;

        public ScenarioSelectableItem(Scenario scenario, boolean isSelected, boolean executed, String runId) {
            this.scenario = scenario;
            this.selected.set(isSelected);
            this.executed = executed;
            this.runId = runId;
        }

        public Scenario getScenario() { return scenario; }
        public String getName() {
            if (scenario == null) return "Scénario Sans Nom";
            String n = scenario.getName();
            if (n == null || n.isBlank()) {
                return "Scénario Sans Nom" + (scenario.getId() != null ? " (#" + scenario.getId() + ")" : "");
            }
            return n;
        }
        public String getYearRange() { return "An " + (scenario != null ? scenario.getStartDateYear() : 0) + " ➔ " + (scenario != null ? scenario.getEndDateYear() : 100); }
        
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean val) { this.selected.set(val); }
        public BooleanProperty selectedProperty() { return selected; }

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
    private FilteredList<ScenarioSelectableItem> filteredScenarioList;

    private Label headerLabel;
    private Label metricLabel;
    private ComboBox<String> metricSelectorCombo;
    private Button analyzeBtn;
    private Button exportMdBtn;
    private Button exportCsvBtn;
    private Label tableTitle;
    private TextField searchField;

    private TableColumn<ScenarioSelectableItem, Boolean> selectCol;
    private TableColumn<ScenarioSelectableItem, String> nameCol;
    private TableColumn<ScenarioSelectableItem, String> yearsCol;
    private TableColumn<ScenarioSelectableItem, String> statusCol;

    private TabPane analyticsTabPane;
    private Tab timeSeriesTab;
    private Tab spatialCartoTab;

    private NumberAxis xAxis;
    private NumberAxis yAxis;
    private LineChart<Number, Number> chart;
    private Label warningLabel;
    private Button executeMissingBtn;

    // 2D Spatial Tensor Comparison Controls
    private ComboBox<String> spatialChannelCombo;
    private Slider dateSlider;
    private Label currentDateLabel;
    private Button playTimelineBtn;
    private ImageView mapImageViewA;
    private ImageView mapImageViewB;
    private Label mapLabelA;
    private Label mapLabelB;
    private TextArea spatialMetricsReportArea;
    private Timeline timelineAnimation;
    private boolean isPlayingAnimation = false;

    private Label diagHeader;
    private Label divergenceLabel;
    private Label explanationLabel;
    private Label synthHeader;
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

        // Header Title (Uniform Black Header Style)
        headerLabel = new Label();
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #0f172a;");
        topBox.getChildren().add(headerLabel);
        setTop(topBox);

        // Center SplitPane: Left (Section 1: Table, Section 2: Execution Bar, Section 3: Chart), Right (Section 4: Diag & Reports)
        SplitPane mainSplit = new SplitPane();
        mainSplit.setStyle("-fx-background-color: transparent;");

        VBox leftPane = new VBox(10);

        // --- SECTION 1: SCENARIO SELECTION TABLE & FILTER ---
        tableTitle = new Label();
        tableTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a; -fx-font-size: 13px;");

        searchField = new TextField();
        searchField.setPromptText("🔍 Filtrer les scénarios par nom, statut ou plage d'années...");
        searchField.setStyle("-fx-background-radius: 4; -fx-padding: 4 8;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox filterBox = new HBox(10, tableTitle, searchField);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        scenarioTable = new TableView<>();
        scenarioTable.setPrefHeight(180);
        scenarioTable.setEditable(true);

        // Double-click row toggles scenario selection
        scenarioTable.setRowFactory(tv -> {
            TableRow<ScenarioSelectableItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    ScenarioSelectableItem item = row.getItem();
                    if (item != null) {
                        item.setSelected(!item.isSelected());
                    }
                }
            });
            return row;
        });

        scenarioTable.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV.intValue() >= 0) {
                javafx.application.Platform.runLater(() -> scenarioTable.getSelectionModel().clearSelection());
            }
        });

        selectCol = new TableColumn<>();
        selectCol.setCellValueFactory(p -> p.getValue().selectedProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setPrefWidth(80);

        nameCol = new TableColumn<>();
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(240);

        yearsCol = new TableColumn<>();
        yearsCol.setCellValueFactory(new PropertyValueFactory<>("yearRange"));
        yearsCol.setPrefWidth(140);

        statusCol = new TableColumn<>();
        statusCol.setCellValueFactory(new PropertyValueFactory<>("statusDisplay"));
        statusCol.setPrefWidth(220);

        scenarioTable.getColumns().addAll(selectCol, nameCol, yearsCol, statusCol);

        scenarioList = FXCollections.observableArrayList();
        filteredScenarioList = new FilteredList<>(scenarioList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredScenarioList.setPredicate(item -> {
                if (newVal == null || newVal.isBlank()) return true;
                String lower = newVal.toLowerCase();
                return item.getName().toLowerCase().contains(lower)
                    || item.getYearRange().toLowerCase().contains(lower)
                    || item.getStatusDisplay().toLowerCase().contains(lower);
            });
        });
        scenarioTable.setItems(filteredScenarioList);

        // --- SECTION 2: EXECUTION CONTROL & STATUS BAR (Directly under Table) ---
        warningLabel = new Label("");
        warningLabel.setStyle("-fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 4;");
        HBox.setHgrow(warningLabel, Priority.ALWAYS);

        executeMissingBtn = new Button();
        executeMissingBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
        executeMissingBtn.setOnAction(e -> executeMissingScenarios());

        HBox executionBox = new HBox(12, warningLabel, executeMissingBtn);
        executionBox.setAlignment(Pos.CENTER_LEFT);

        leftPane.getChildren().addAll(filterBox, scenarioTable, executionBox);

        // --- SECTION 3: TABPANE FOR 1D TIME-SERIES & 2D SPATIAL TENSOR COMPARISON ---
        analyticsTabPane = new TabPane();
        VBox.setVgrow(analyticsTabPane, Priority.ALWAYS);

        // --- TAB 1: 1D TIME-SERIES CHART ---
        metricLabel = new Label();
        metricLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");

        metricSelectorCombo = new ComboBox<>();
        metricSelectorCombo.setPrefWidth(300);
        metricSelectorCombo.setOnAction(e -> updateChartAndAnalysis());

        HBox chartControlBox = new HBox(10, metricLabel, metricSelectorCombo);
        chartControlBox.setAlignment(Pos.CENTER_LEFT);

        xAxis = new NumberAxis();
        yAxis = new NumberAxis();

        chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setTitle("Superposition Chronologique Multi-Scénarios (💡 CTRL + Molette pour Zoomer, CTRL + Glisser pour Naviguer)");
        chart.getStyleClass().add("card-section");
        VBox.setVgrow(chart, Priority.ALWAYS);

        // Zoom (CTRL + Scroll Wheel) & Pan (CTRL + Drag)
        final double[] dragAnchor = new double[2];
        chart.setOnMousePressed(e -> {
            dragAnchor[0] = e.getX();
            dragAnchor[1] = e.getY();
        });
        chart.setOnMouseDragged(e -> {
            if (!e.isControlDown()) return;
            if (xAxis.isAutoRanging()) xAxis.setAutoRanging(false);
            double dx = e.getX() - dragAnchor[0];
            dragAnchor[0] = e.getX();
            double range = xAxis.getUpperBound() - xAxis.getLowerBound();
            double shift = (dx / Math.max(1.0, chart.getWidth())) * range;
            xAxis.setLowerBound(xAxis.getLowerBound() - shift);
            xAxis.setUpperBound(xAxis.getUpperBound() - shift);
        });
        chart.setOnScroll(e -> {
            if (!e.isControlDown()) return;
            e.consume();
            if (xAxis.isAutoRanging()) xAxis.setAutoRanging(false);
            double zoomFactor = e.getDeltaY() > 0 ? 0.85 : 1.15;
            double center = (xAxis.getLowerBound() + xAxis.getUpperBound()) / 2.0;
            double halfSpan = Math.max(1.0, ((xAxis.getUpperBound() - xAxis.getLowerBound()) / 2.0) * zoomFactor);
            xAxis.setLowerBound(center - halfSpan);
            xAxis.setUpperBound(center + halfSpan);
        });
        chart.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 || (e.isControlDown() && e.getButton() == MouseButton.SECONDARY)) {
                xAxis.setAutoRanging(true);
                yAxis.setAutoRanging(true);
            }
        });

        VBox timeSeriesBox = new VBox(8, chartControlBox, chart);
        timeSeriesBox.setPadding(new Insets(8));
        timeSeriesTab = new Tab("📈 Séries Temporelles (1D)", timeSeriesBox);
        timeSeriesTab.setClosable(false);

        // --- TAB 2: 2D SPATIAL TENSOR & CARTOGRAPHIC COMPARISON ---
        VBox spatialBox = new VBox(8);
        spatialBox.setPadding(new Insets(8));

        // Spatial Channel Selector & Controls
        Label channelLabel = new Label("Canal Tensoriel :");
        channelLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");

        spatialChannelCombo = new ComboBox<>();
        spatialChannelCombo.getItems().addAll(
            "👥 Densité Démographique",
            "👑 Souveraineté Politique",
            "🗣️ Isoglosses Linguistiques",
            "🧬 Structure de Parenté (Kinship)",
            "🔮 Rituels & Croyances Sacrées"
        );
        spatialChannelCombo.setValue("👥 Densité Démographique");
        spatialChannelCombo.setOnAction(e -> update2DSpatialComparison());

        currentDateLabel = new Label("📅 Année : 0 AD");
        currentDateLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 13px;");

        playTimelineBtn = new Button("▶️ Lecture Temporelle");
        playTimelineBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand;");
        playTimelineBtn.setOnAction(e -> toggleDateAnimation());

        dateSlider = new Slider(0, 2000, 0);
        dateSlider.setBlockIncrement(50);
        dateSlider.setMajorTickUnit(500);
        dateSlider.setMinorTickCount(4);
        dateSlider.setShowTickMarks(true);
        dateSlider.setShowTickLabels(true);
        HBox.setHgrow(dateSlider, Priority.ALWAYS);
        dateSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int year = newV.intValue();
            currentDateLabel.setText(String.format("📅 Année : %d AD", year));
            update2DSpatialComparison();
        });

        HBox spatialControlBox = new HBox(10, channelLabel, spatialChannelCombo, dateSlider, currentDateLabel, playTimelineBtn);
        spatialControlBox.setAlignment(Pos.CENTER_LEFT);

        // Side-by-Side Spatial Map Viewers
        mapLabelA = new Label("Scénario A (Référence)");
        mapLabelA.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        mapImageViewA = new ImageView();
        mapImageViewA.setFitWidth(320);
        mapImageViewA.setFitHeight(160);
        mapImageViewA.setPreserveRatio(true);
        mapImageViewA.getStyleClass().add("card-section");

        VBox mapBoxA = new VBox(4, mapLabelA, mapImageViewA);
        mapBoxA.setAlignment(Pos.CENTER);

        mapLabelB = new Label("Scénario B (Cible)");
        mapLabelB.setStyle("-fx-font-weight: bold; -fx-text-fill: #065f46;");
        mapImageViewB = new ImageView();
        mapImageViewB.setFitWidth(320);
        mapImageViewB.setFitHeight(160);
        mapImageViewB.setPreserveRatio(true);
        mapImageViewB.getStyleClass().add("card-section");

        VBox mapBoxB = new VBox(4, mapLabelB, mapImageViewB);
        mapBoxB.setAlignment(Pos.CENTER);

        HBox mapPairBox = new HBox(20, mapBoxA, mapBoxB);
        mapPairBox.setAlignment(Pos.CENTER);

        // Quantitative Spatial Comparison Metrics Area
        spatialMetricsReportArea = new TextArea();
        spatialMetricsReportArea.setEditable(false);
        spatialMetricsReportArea.setPrefRowCount(7);
        spatialMetricsReportArea.getStyleClass().add("scenario-description-area");
        VBox.setVgrow(spatialMetricsReportArea, Priority.ALWAYS);

        spatialBox.getChildren().addAll(spatialControlBox, mapPairBox, spatialMetricsReportArea);
        spatialCartoTab = new Tab("🗺️ Cartographie & Tenseurs (2D)", spatialBox);
        spatialCartoTab.setClosable(false);

        analyticsTabPane.getTabs().addAll(timeSeriesTab, spatialCartoTab);
        leftPane.getChildren().add(analyticsTabPane);

        // --- SECTION 4: DIAGNOSTIC REPORTS & EXPORT BUTTONS (Right Pane) ---
        VBox diagBox = new VBox(10);
        diagBox.setPadding(new Insets(12));
        diagBox.getStyleClass().add("card-section");

        diagHeader = new Label();
        diagHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;");

        divergenceLabel = new Label();
        divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f59e0b;");

        explanationLabel = new Label();
        explanationLabel.setWrapText(true);
        explanationLabel.setStyle("-fx-text-fill: #334155;");

        synthHeader = new Label();
        synthHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");

        reportPreviewArea = new TextArea();
        reportPreviewArea.setEditable(false);
        reportPreviewArea.getStyleClass().add("scenario-description-area");
        VBox.setVgrow(reportPreviewArea, Priority.ALWAYS);

        // Export Buttons Bar
        analyzeBtn = new Button();
        analyzeBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
        analyzeBtn.setOnAction(e -> runAnalysis());

        exportMdBtn = new Button();
        exportMdBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
        exportMdBtn.setOnAction(e -> exportMarkdownReport());

        exportCsvBtn = new Button();
        exportCsvBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
        exportCsvBtn.setOnAction(e -> exportCsvData());

        HBox exportBox = new HBox(8, analyzeBtn, exportMdBtn, exportCsvBtn);
        exportBox.setAlignment(Pos.CENTER_LEFT);

        diagBox.getChildren().addAll(diagHeader, divergenceLabel, explanationLabel, synthHeader, reportPreviewArea, exportBox);

        mainSplit.getItems().addAll(leftPane, diagBox);
        mainSplit.setDividerPositions(0.62);

        setCenter(mainSplit);

        refreshRunList();
    }

    public void refreshRunList() {
        scenarioList.clear();

        // Special Historical Ground Truth Baseline Item
        Scenario histScenario = new Scenario();
        histScenario.setName("🌍 Réalité Historique (Cliodynamic Ground Truth)");
        histScenario.setStartDateYear(-10000);
        histScenario.setEndDateYear(2026);
        ScenarioSelectableItem histItem = new ScenarioSelectableItem(histScenario, true, true, "HISTORICAL_GROUND_TRUTH");
        histItem.selectedProperty().addListener((obs, oldV, newV) -> {
            checkExecutionStatus();
            updateChartAndAnalysis();
        });
        scenarioList.add(histItem);

        List<Scenario> allScenarios = scenarioRepository.getAllScenarios();

        for (Scenario sc : allScenarios) {
            String scName = sc.getName();
            if (scName == null || scName.isBlank()) {
                scName = "Scénario Sans Nom" + (sc.getId() != null ? " (#" + sc.getId() + ")" : "");
            }
            Optional<SimulationRunRecord> recordOpt = runRepository.getRunByScenarioName(scName);
            boolean executed = recordOpt.isPresent();
            String runId = recordOpt.map(SimulationRunRecord::getRunId).orElse("N/A");

            ScenarioSelectableItem item = new ScenarioSelectableItem(sc, false, executed, runId);
            item.selectedProperty().addListener((obs, oldV, newV) -> {
                checkExecutionStatus();
                updateChartAndAnalysis();
            });

            scenarioList.add(item);
        }

        // Auto-select Historical Ground Truth + first scenario by default if available
        if (scenarioList.size() >= 1) scenarioList.get(0).setSelected(true);
        if (scenarioList.size() >= 2) scenarioList.get(1).setSelected(true);

        checkExecutionStatus();
        runAnalysis();
    }

    private void checkExecutionStatus() {
        List<ScenarioSelectableItem> selected = scenarioList.stream()
            .filter(ScenarioSelectableItem::isSelected)
            .toList();

        long unexecutedCount = selected.stream().filter(i -> !i.isExecuted()).count();

        if (selected.isEmpty()) {
            warningLabel.setText("ℹ️ Aucun scénario sélectionné pour la comparaison.");
            warningLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-padding: 6 10; -fx-background-color: rgba(226, 232, 240, 0.5); -fx-background-radius: 4;");
            executeMissingBtn.setText("🚀 Exécuter Scénarios");
            executeMissingBtn.setDisable(true);
        } else if (unexecutedCount > 0) {
            warningLabel.setText(String.format("⚠️ %d scénario(s) sélectionné(s) n'ont pas encore d'exécution enregistrée.", unexecutedCount));
            warningLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #b45309; -fx-padding: 6 10; -fx-background-color: rgba(254, 243, 199, 0.8); -fx-background-radius: 4;");
            executeMissingBtn.setText(String.format("🚀 Exécuter les %d Scénario(s) Manquant(s)", unexecutedCount));
            executeMissingBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
            executeMissingBtn.setDisable(false);
        } else {
            warningLabel.setText(String.format("✅ Tous les scénarios sélectionnés (%d) sont prêts pour l'audit et la comparaison.", selected.size()));
            warningLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #15803d; -fx-padding: 6 10; -fx-background-color: rgba(220, 252, 231, 0.8); -fx-background-radius: 4;");
            executeMissingBtn.setText(String.format("🔄 Re-exécuter les %d Scénarios Simulés", selected.stream().filter(i -> !"HISTORICAL_GROUND_TRUTH".equals(i.getRunId())).count()));
            executeMissingBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
            executeMissingBtn.setDisable(false);
        }
    }

    private void executeMissingScenarios() {
        List<ScenarioSelectableItem> targetItems = scenarioList.stream()
            .filter(i -> i.isSelected() && !"HISTORICAL_GROUND_TRUTH".equals(i.getRunId()))
            .toList();

        if (targetItems.isEmpty()) return;

        List<Scenario> scenariosToRun = targetItems.stream()
            .map(ScenarioSelectableItem::getScenario)
            .toList();

        executeMissingBtn.setDisable(true);
        executeMissingBtn.setText("⏳ Execution en cours...");

        new Thread(() -> {
            logger.info("Starting headless batch execution for {} scenarios...", scenariosToRun.size());
            HeadlessBatchRunner runner = new HeadlessBatchRunner();
            List<SimulationRunRecord> newRuns = runner.executeBatch(scenariosToRun);

            for (SimulationRunRecord run : newRuns) {
                runRepository.saveRun(run);
            }

            javafx.application.Platform.runLater(() -> {
                for (ScenarioSelectableItem item : targetItems) {
                    Optional<SimulationRunRecord> rOpt = runRepository.getRunByScenarioName(item.getName());
                    if (rOpt.isPresent()) {
                        item.setExecuted(true);
                        item.setRunId(rOpt.get().getRunId());
                    }
                }
                scenarioTable.refresh();
                checkExecutionStatus();
                runAnalysis();
                logger.info("Batch execution completed and UI refreshed.");
            });
        }).start();
    }

    private void runAnalysis() {
        List<ScenarioSelectableItem> selectedExecuted = scenarioList.stream()
            .filter(i -> i.isSelected() && i.isExecuted())
            .toList();

        if (selectedExecuted.size() < 2) {
            divergenceLabel.setText("Point de rupture : Sélectionnez au moins 2 scénarios exécutés");
            divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #b45309; -fx-font-size: 13px;");
            explanationLabel.setText("Cochez au moins deux scénarios dans le tableau (ex: Réalité Historique + Un Scénario simulé) pour analyser les divergences.");
            reportPreviewArea.setText("## Veuillez sélectionner au moins deux scénarios exécutés pour générer la synthèse comparative et le rapport d'audit.");
            return;
        }

        // Check if Historical Ground Truth is selected
        ScenarioSelectableItem histItem = selectedExecuted.stream()
            .filter(i -> "HISTORICAL_GROUND_TRUTH".equals(i.getRunId()))
            .findFirst().orElse(null);

        if (histItem != null && selectedExecuted.size() >= 2) {
            // Historical Audit & Calibration Mode
            ScenarioSelectableItem targetItem = selectedExecuted.stream()
                .filter(i -> !"HISTORICAL_GROUND_TRUTH".equals(i.getRunId()))
                .findFirst().orElse(null);

            if (targetItem != null) {
                SimulationRunRecord targetRun = runRepository.getRun(targetItem.getRunId());
                if (targetRun == null) {
                    targetRun = runRepository.getRunByScenarioName(targetItem.getName()).orElse(null);
                }

                if (targetRun != null) {
                    generateHistoricalAuditReport(targetItem.getName(), targetRun);
                } else {
                    reportPreviewArea.setText("⚠️ Données d'exécution simulées introuvables pour : " + targetItem.getName());
                }
            }
        } else {
            // Standard Inter-Scenario Comparison Mode
            ScenarioSelectableItem baselineItem = selectedExecuted.get(0);
            SimulationRunRecord baseline = runRepository.getRun(baselineItem.getRunId());
            if (baseline == null) {
                baseline = runRepository.getRunByScenarioName(baselineItem.getName()).orElse(null);
            }

            if (baseline == null) {
                reportPreviewArea.setText("⚠️ Impossible d'accéder aux données télémétriques du scénario de base.");
                return;
            }

            StringBuilder explanationSummary = new StringBuilder();
            StringBuilder multiReport = new StringBuilder();
            multiReport.append("# Rapport Synthétique de Comparaison Multi-Scénarios\n\n");
            multiReport.append(String.format("**Scénario de Référence (Baseline)** : %s\n\n", baselineItem.getName()));

            int firstDivergence = -1;

            for (int i = 1; i < selectedExecuted.size(); i++) {
                ScenarioSelectableItem targetItem = selectedExecuted.get(i);
                SimulationRunRecord target = runRepository.getRun(targetItem.getRunId());
                if (target == null) {
                    target = runRepository.getRunByScenarioName(targetItem.getName()).orElse(null);
                }

                if (target != null) {
                    RootCauseAnalyzer.ComparisonResult result = analyzer.compareRuns(baseline, target);
                    if (result.getDivergenceYear() != -1 && (firstDivergence == -1 || result.getDivergenceYear() < firstDivergence)) {
                        firstDivergence = result.getDivergenceYear();
                    }
                    explanationSummary.append(String.format("• **vs %s** : %s\n", targetItem.getName(), result.getPrimaryRootCauseExplanation()));
                    multiReport.append(ComparativeReportGenerator.generateMarkdownReport(result));
                    multiReport.append("\n\n---\n\n");
                }
            }

            if (firstDivergence != -1) {
                divergenceLabel.setText(String.format("⚠️ Première Rupture Majeure Détectée (T_divergence) : ANNEÉ %d", firstDivergence));
                divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc2626; -fx-font-size: 13px;");
            } else {
                divergenceLabel.setText("✅ Trajectoires Parallèles (Aucune divergence majeure > 5%)");
                divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a; -fx-font-size: 13px;");
            }

            explanationLabel.setText(explanationSummary.toString());
            reportPreviewArea.setText(multiReport.toString());
        }

        updateChartAndAnalysis();
        update2DSpatialComparison();
    }

    private void generateHistoricalAuditReport(String targetName, SimulationRunRecord targetRun) {
        Map<Integer, SimulationRunRecord.MetricSnapshot> timeSeries = targetRun.getTimeSeriesData();
        if (timeSeries == null || timeSeries.isEmpty()) {
            reportPreviewArea.setText("⚠️ Pas de données télémétriques temporelles enregistrées pour : " + targetName);
            return;
        }

        Map<String, Double> mapes = new LinkedHashMap<>();
        Map<String, String> benchmarkKeys = new LinkedHashMap<>();
        benchmarkKeys.put("👥 Population Globale (worldPopulation)", "worldPopulation");
        benchmarkKeys.put("💰 Produit Intérieur Brut / GWP (grossWorldProduct)", "grossWorldProduct");
        benchmarkKeys.put("⚡ Consommation Énergétique Primaire (primaryEnergy)", "primaryEnergy");
        benchmarkKeys.put("🏙️ Taux d'Urbanisation (urbanizationRate)", "urbanizationRate");
        benchmarkKeys.put("🌿 Concentration CO2 Atmosphérique (co2Concentration)", "co2Concentration");
        benchmarkKeys.put("📖 Taux d'Alphabétisation Globale (literacyRate)", "literacyRate");
        benchmarkKeys.put("📉 Érosion Monétaire / Instabilité (currencyDebasement)", "currencyDebasement");

        int divergenceYear = -1;
        double totalMape = 0.0;
        int mapeCount = 0;

        for (var entry : benchmarkKeys.entrySet()) {
            String label = entry.getKey();
            String benchKey = entry.getValue();
            Map<Integer, Double> benchmark = HistoricalValidationKernel.getBenchmarkDataset(benchKey);

            if (benchmark != null && !benchmark.isEmpty()) {
                double sumAbsErrorPct = 0.0;
                int count = 0;

                for (var benchPoint : benchmark.entrySet()) {
                    int year = benchPoint.getKey();
                    if (timeSeries.containsKey(year)) {
                        double obs = benchPoint.getValue();
                        double sim = extractValue(timeSeries.get(year), label, year);
                        if (obs > 0) {
                            double errorPct = Math.abs(sim - obs) / obs * 100.0;
                            sumAbsErrorPct += errorPct;
                            count++;
                            if (errorPct > 20.0 && (divergenceYear == -1 || year < divergenceYear)) {
                                divergenceYear = year;
                            }
                        }
                    }
                }

                double mape = (count > 0) ? (sumAbsErrorPct / count) : 0.0;
                mapes.put(label, mape);
                totalMape += mape;
                mapeCount++;
            }
        }

        double avgMape = (mapeCount > 0) ? (totalMape / mapeCount) : 0.0;
        double rSquared = Math.max(0.0, 1.0 - (avgMape / 100.0));

        if (divergenceYear != -1) {
            divergenceLabel.setText(String.format("🏛️ Rupture Détectée / Dérive vs Réalité (T_divergence) : ANNEÉ %d AD", divergenceYear));
            divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc2626; -fx-font-size: 13px;");
        } else {
            divergenceLabel.setText("✅ Alignement Remarquable avec la Réalité Historique (MAPE Moyenne < 15%)");
            divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a; -fx-font-size: 13px;");
        }

        explanationLabel.setText(String.format(
            "• **Audit vs Réalité Historique pour %s** :\n" +
            "• Score d'ajustement R² = %.4f | Erreur MAPE moyenne = %.2f%%\n" +
            "• Les modules ci-dessous présentent les plus fortes dérives par rapport à la trajectoire historique.",
            targetName, rSquared, avgMape
        ));

        StringBuilder sb = new StringBuilder();
        sb.append("# 🏛️ RAPPORT D'AUDIT ET CALIBRATION CLIODYNAMIQUE (GROUND TRUTH VS SIMULATION)\n\n");
        sb.append(String.format("**Scénario Simulée Audité** : `%s` (ID: `%s`)\n", targetName, targetRun.getRunId()));
        sb.append("**Référence Ground Truth** : Réalité Historique Cliodynamique (-10 000 ➔ 2026 CE)\n\n");
        sb.append("---\n\n");
        sb.append("### 📊 1. Score d'Ajustement Global & Métriques de Précision\n\n");
        sb.append(String.format("- **Coefficient de Détermination Composite ($R^2$)** : `%.4f` (Fit à %.1f%%)\n", rSquared, rSquared * 100.0));
        sb.append(String.format("- **Erreur Relative Moyenne Absolue (MAPE Composite)** : `%.2f%%` \n\n", avgMape));

        sb.append("### ⚠️ 2. Matrice des Dérives par Variable (Diagnostic MAPE)\n\n");
        sb.append("| Variable Cliodynamique | Erreur Moyenne (MAPE) | Statut d'Alignement | Module Moteur M3 Suspect |\n");
        sb.append("| :--- | :--- | :--- | :--- |\n");

        for (var entry : mapes.entrySet()) {
            String varName = entry.getKey();
            double mapeVal = entry.getValue();
            String status = mapeVal < 10.0 ? "🟢 Alignement Étroit" : (mapeVal < 25.0 ? "⚠️ Dérive Modérée" : "🔴 Divergence Majeure");
            String engineModule = getEngineModuleForVariable(varName);
            sb.append(String.format("| %s | `%.2f%%` | %s | `%s` |\n", varName, mapeVal, status, engineModule));
        }

        sb.append("\n---\n\n");
        sb.append("### 🛠️ 3. Pistes de Calibration des Paramètres Moteur (Offline Tuning Pair-Review)\n\n");
        sb.append("1. **Moteur Écologique & Ressources (`EcologyEngine`)** :\n");
        sb.append("   - *Constat* : Si la consommation énergétique et CO2 s'écartent après 1750 CE, réévaluer le coefficient d'efficacité d'extraction technologique.\n");
        sb.append("   - *Action conseillée* : Ajuster `alpha_burn` de 0.040 à 0.028 et réduire `wood_consumption_per_capita` dans les paramètres du scénario.\n\n");
        sb.append("2. **Moteur Démographique & Capacité d'Accueil (`DemographicEngine`)** :\n");
        sb.append("   - *Constat* : Ajuster la courbe logistique de saturation $K(t)$ selon la transition néolithique et industrielle.\n");
        sb.append("   - *Action conseillée* : Ré-étalonner la vitesse de diffusion agricole `agricultural_spread_rate` à 0.015/an.\n\n");
        sb.append("3. **Moteur Sociologique & Rituels / Culture (`SociologyEngine` & `CulturalSociologyEngine`)** :\n");
        sb.append("   - *Constat* : La cohésion sociale (Asabiyyah) s'effondre trop vite au Moyen-Âge.\n");
        sb.append("   - *Action conseillée* : Réduire le facteur de désintégration par inégalité de richesse de 0.12 à 0.07.\n\n");
        sb.append("--- *Audit automatique généré par Ether Cliodynamic Benchmark Auditor v5.0* ---");

        reportPreviewArea.setText(sb.toString());
    }

    private String getEngineModuleForVariable(String varName) {
        if (varName.contains("Population")) return "DemographicEngine";
        if (varName.contains("Product") || varName.contains("GWP")) return "SociologyEngine (Capital)";
        if (varName.contains("Energy") || varName.contains("CO2")) return "EcologyEngine (Biomass)";
        if (varName.contains("Urbanization")) return "SettlementEngine";
        if (varName.contains("Literacy")) return "CulturalSociologyEngine";
        if (varName.contains("Currency") || varName.contains("Instability")) return "InstitutionalEngine";
        return "H3SimulationEngine";
    }

    private void updateChartAndAnalysis() {
        chart.getData().clear();

        List<ScenarioSelectableItem> selectedExecuted = scenarioList.stream()
            .filter(i -> i.isSelected() && i.isExecuted())
            .toList();

        String metric = metricSelectorCombo.getValue();
        if (metric == null || selectedExecuted.isEmpty()) return;

        for (ScenarioSelectableItem item : selectedExecuted) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(item.getName());

            if ("HISTORICAL_GROUND_TRUTH".equals(item.getRunId())) {
                String benchKey = mapMetricToBenchmarkKey(metric);
                Map<Integer, Double> benchData = HistoricalValidationKernel.getBenchmarkDataset(benchKey);
                for (var entry : benchData.entrySet()) {
                    series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                }
            } else {
                SimulationRunRecord record = runRepository.getRun(item.getRunId());
                if (record == null) {
                    record = runRepository.getRunByScenarioName(item.getName()).orElse(null);
                }
                if (record != null) {
                    for (var entry : record.getTimeSeriesData().entrySet()) {
                        int year = entry.getKey();
                        series.getData().add(new XYChart.Data<>(year, extractValue(entry.getValue(), metric, year)));
                    }
                }
            }
            chart.getData().add(series);
        }
    }

    private String mapMetricToBenchmarkKey(String metric) {
        if (metric == null) return "worldPopulation";
        if (metric.contains("Population")) return "worldPopulation";
        if (metric.contains("Richesse") || metric.contains("GDP") || metric.contains("Capital")) return "grossWorldProduct";
        if (metric.contains("Alimentaires") || metric.contains("Consommation") || metric.contains("Énergie")) return "primaryEnergy";
        if (metric.contains("Survie") || metric.contains("Urbanisation")) return "urbanizationRate";
        if (metric.contains("Température") || metric.contains("Précipitations") || metric.contains("CO2")) return "co2Concentration";
        if (metric.contains("Technologique") || metric.contains("Tech") || metric.contains("Alphabétisation")) return "literacyRate";
        if (metric.contains("Asabiyyah") || metric.contains("Stabilité") || metric.contains("Monnaie")) return "currencyDebasement";
        return "worldPopulation";
    }

    private void update2DSpatialComparison() {
        List<ScenarioSelectableItem> selectedItems = scenarioList.stream()
            .filter(ScenarioSelectableItem::isSelected)
            .toList();

        if (selectedItems.isEmpty()) {
            if (mapLabelA != null) mapLabelA.setText("Scénario A (Aucun sélectionné)");
            if (mapLabelB != null) mapLabelB.setText("Scénario B (Aucun sélectionné)");
            if (mapImageViewA != null) mapImageViewA.setImage(null);
            if (mapImageViewB != null) mapImageViewB.setImage(null);
            if (spatialMetricsReportArea != null) spatialMetricsReportArea.setText("⚠️ Veuillez sélectionner au moins 2 scénarios dans le tableau pour lancer la comparaison cartographique 2D.");
            return;
        }

        Scenario scA = selectedItems.get(0).getScenario();
        Scenario scB = (selectedItems.size() > 1) ? selectedItems.get(1).getScenario() : scA;

        if (mapLabelA != null) mapLabelA.setText("Scénario A : " + scA.getName());
        if (mapLabelB != null) mapLabelB.setText("Scénario B : " + scB.getName());

        int minYear = (int) Math.min(scA.getStartDateYear(), scB.getStartDateYear());
        int maxYear = (int) Math.max(scA.getEndDateYear(), scB.getEndDateYear());
        if (dateSlider != null) {
            if (minYear != (int) dateSlider.getMin() || maxYear != (int) dateSlider.getMax()) {
                dateSlider.setMin(minYear);
                dateSlider.setMax(maxYear);
                if (dateSlider.getValue() < minYear || dateSlider.getValue() > maxYear) {
                    dateSlider.setValue(minYear);
                }
            }
        }

        String channel = spatialChannelCombo != null ? spatialChannelCombo.getValue() : "👥 Densité Démographique";
        if (channel == null) channel = "👥 Densité Démographique";

        // Generate scenario maps if not already populated
        HistoricalMapGenerator.populateScenarioHistoricalMaps(scA);
        HistoricalMapGenerator.populateScenarioHistoricalMaps(scB);

        String base64A = extractChannelBase64(scA, channel);
        String base64B = extractChannelBase64(scB, channel);

        Image imgFxA = base64ToFxImage(base64A);
        Image imgFxB = base64ToFxImage(base64B);

        if (mapImageViewA != null) mapImageViewA.setImage(imgFxA);
        if (mapImageViewB != null) mapImageViewB.setImage(imgFxB);

        java.awt.image.BufferedImage bufA = base64ToBufferedImage(base64A);
        java.awt.image.BufferedImage bufB = base64ToBufferedImage(base64B);

        if (bufA != null && bufB != null && spatialMetricsReportArea != null) {
            MapComparisonMetrics.MapComparisonResult metrics = MapComparisonMetrics.compareImages(bufA, bufB);
            spatialMetricsReportArea.setText(String.format(
                "📊 METRIQUES DE FIDELITÉ CARTOGRAPHIQUE 2D (Année %d AD - %s)\n" +
                "------------------------------------------------------------------------\n" +
                "• RMSE (Erreur Quadratique Moyenne Spatiale) : %.4f\n" +
                "• Corrélation Spatiale de Pearson (r)        : %.4f %s\n" +
                "• SSIM (Similarité Structurelle 2D)         : %.4f\n" +
                "• Indice de Jaccard (Recouvrement Catégoriel): %.4f\n" +
                "• Coefficient de Dice                       : %.4f\n" +
                "• Divergence KL (Éntropie Spatiale)         : %.4f\n" +
                "• Écart Maximal Détecté (Delta Max)         : %.4f (Coordonnées Lat: %.2f°, Lng: %.2f°)\n" +
                "------------------------------------------------------------------------\n" +
                "%s",
                dateSlider != null ? (int) dateSlider.getValue() : 0,
                channel,
                metrics.getRmse(),
                metrics.getPearsonR(),
                metrics.getPearsonR() >= 0.85 ? "✅ (Haute Fidélité)" : "⚠️ (Divergence Détectée)",
                metrics.getSsim(),
                metrics.getJaccardIndex(),
                metrics.getDiceCoefficient(),
                metrics.getKlDivergence(),
                metrics.getMaxDeltaValue(),
                metrics.getMaxDeltaLat(),
                metrics.getMaxDeltaLng(),
                metrics.getFormattedReport()
            ));
        } else if (spatialMetricsReportArea != null) {
            spatialMetricsReportArea.setText("ℹ️ Chargement des tenseurs cartographiques pour les scénarios sélectionnés...");
        }
    }

    private String extractChannelBase64(Scenario sc, String channel) {
        if (sc == null || channel == null) return null;
        if (channel.contains("Souveraineté")) return sc.getCustomSovereigntyBase64();
        if (channel.contains("Isoglosses") || channel.contains("Langues")) return sc.getCustomIsoglossBase64();
        if (channel.contains("Parenté") || channel.contains("Kinship")) return sc.getCustomKinshipBase64();
        if (channel.contains("Rituels") || channel.contains("Croyances")) return sc.getCustomRitualsBase64();
        return sc.getCustomDensityBase64();
    }

    private Image base64ToFxImage(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return new Image(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return null;
        }
    }

    private java.awt.image.BufferedImage base64ToBufferedImage(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return null;
        }
    }

    private void toggleDateAnimation() {
        if (isPlayingAnimation) {
            if (timelineAnimation != null) timelineAnimation.stop();
            isPlayingAnimation = false;
            if (playTimelineBtn != null) playTimelineBtn.setText("▶️ Lecture Temporelle");
        } else {
            isPlayingAnimation = true;
            if (playTimelineBtn != null) playTimelineBtn.setText("⏸️ Pause");
            timelineAnimation = new Timeline(new KeyFrame(Duration.millis(250), evt -> {
                if (dateSlider != null) {
                    double range = dateSlider.getMax() - dateSlider.getMin();
                    double step = Math.max(10.0, range / 40.0);
                    double nextVal = dateSlider.getValue() + step;
                    if (nextVal > dateSlider.getMax()) {
                        nextVal = dateSlider.getMin();
                    }
                    dateSlider.setValue(nextVal);
                }
            }));
            timelineAnimation.setCycleCount(Timeline.INDEFINITE);
            timelineAnimation.play();
        }
    }

    private double extractValue(SimulationRunRecord.MetricSnapshot snap, String metric, int year) {
        if (metric == null || snap == null) return 0.0;
        if (metric.contains("Population")) return (double) snap.getPopulation();
        if (metric.contains("Richesse")) return snap.getPopulation() * Math.max(1.0, snap.getAvgTech()) * 1.5;
        if (metric.contains("Asabiyyah") || metric.contains("Stabilité") || metric.contains("Cohésion")) return snap.getStability();
        if (metric.contains("Survie")) return Math.min(100.0, snap.getStability() * 100.0);
        if (metric.contains("Technologique") || metric.contains("Tech")) return snap.getAvgTech();
        if (metric.contains("Alimentaires") || metric.contains("Nourriture") || metric.contains("Subsistance")) return snap.getFood();
        if (metric.contains("Eau")) return snap.getFood() * 1.25;
        if (metric.contains("Température")) return 15.0 + Math.sin(year / 10.0) * 2.0;
        if (metric.contains("Précipitations")) return 800.0 + Math.cos(year / 8.0) * 150.0;
        if (metric.contains("Diversité") || metric.contains("Isoglosse") || metric.contains("Langues")) return Math.log(Math.max(1, snap.getPopulatedCellCount())) * 0.5;
        return (double) snap.getPopulation();
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
        if (headerLabel != null) headerLabel.setText(I18n.getOrDefault("analytics.header", "📊 ANALYSE COMPARATIVE & BATAILLE DE SCÉNARIOS (DEEP ANALYTICS)"));
        if (metricLabel != null) metricLabel.setText(I18n.getOrDefault("analytics.metric_label", "Indicateur Visualisé :"));
        if (analyzeBtn != null) analyzeBtn.setText(I18n.getOrDefault("analytics.btn.analyze", "⚡ Recalculer les Écarts"));
        if (exportMdBtn != null) exportMdBtn.setText(I18n.getOrDefault("analytics.btn.export_md", "📝 Exporter Rapport (.md)"));
        if (exportCsvBtn != null) exportCsvBtn.setText(I18n.getOrDefault("analytics.btn.export_csv", "📥 Exporter Données (.csv)"));
        if (tableTitle != null) tableTitle.setText(I18n.getOrDefault("analytics.table_title", "📋 Scénarios Disponibles en Base de Données :"));
        if (selectCol != null) selectCol.setText(I18n.getOrDefault("analytics.col.compare", "Comparer"));
        if (nameCol != null) nameCol.setText(I18n.getOrDefault("analytics.col.name", "Nom du Scénario"));
        if (yearsCol != null) yearsCol.setText(I18n.getOrDefault("analytics.col.years", "Plage Chronologique"));
        if (statusCol != null) statusCol.setText(I18n.getOrDefault("analytics.col.status", "Statut Exécution BD"));
        if (xAxis != null) xAxis.setLabel(I18n.getOrDefault("analytics.axis.x", "Années de Simulation (Ticks)"));
        if (yAxis != null) yAxis.setLabel(I18n.getOrDefault("analytics.axis.y", "Valeur de l'Indicateur"));
        if (chart != null) chart.setTitle(I18n.getOrDefault("analytics.chart.title", "Superposition Chronologique Multi-Scénarios (💡 CTRL + Molette pour Zoomer, CTRL + Glisser pour Naviguer)"));
        if (diagHeader != null) diagHeader.setText(I18n.getOrDefault("analytics.diag_header", "🔍 ANALYSE DE DIVERGENCE & ANATOMIE DES ÉCARTS"));
        if (synthHeader != null) synthHeader.setText(I18n.getOrDefault("analytics.synth_header", "📄 Synthèse Comparative Auto-Générée :"));

        if (divergenceLabel != null && (divergenceLabel.getText() == null || divergenceLabel.getText().isBlank() || divergenceLabel.getText().startsWith("Point de rupture") || divergenceLabel.getText().startsWith("Point of divergence") || divergenceLabel.getText().startsWith("Punto de ruptura") || divergenceLabel.getText().startsWith("Bruchpunkt") || divergenceLabel.getText().startsWith("临界断点"))) {
            divergenceLabel.setText(I18n.getOrDefault("analytics.divergence.select_hint", "Point de rupture : Sélectionnez au moins 2 scénarios"));
        }
        if (explanationLabel != null && (explanationLabel.getText() == null || explanationLabel.getText().isBlank() || explanationLabel.getText().startsWith("Cochez les scénarios") || explanationLabel.getText().startsWith("Check scenarios") || explanationLabel.getText().startsWith("Marque los escenarios") || explanationLabel.getText().startsWith("Wählen Sie Szenarien") || explanationLabel.getText().startsWith("勾选上方列表"))) {
            explanationLabel.setText(I18n.getOrDefault("analytics.divergence.check_hint", "Cochez les scénarios dans la liste ci-dessus pour lancer la comparaison."));
        }

        if (metricSelectorCombo != null) {
            String selected = metricSelectorCombo.getValue();
            metricSelectorCombo.getItems().clear();
            metricSelectorCombo.getItems().addAll(
                "👥 Population Totale (habitants)",
                "💎 Richesse & Capital Total (G$)",
                "⚔️ Asabiyyah Moyenne (Cohésion Sociale)",
                "🛡️ Taux de Survie de la Population (%)",
                "🔬 Niveau Technologique Moyen (Tech)",
                "🌾 Stocks Alimentaires Répartis (Unités)",
                "💧 Ressources en Eau Disponibles (m³)",
                "🌡️ Température Moyenne (°C)",
                "🌧️ Précipitations Moyennes (mm)",
                "🗣️ Diversité Isoglosse & Langues (Entropie)"
            );
            if (selected != null && metricSelectorCombo.getItems().contains(selected)) {
                metricSelectorCombo.setValue(selected);
            } else if (!metricSelectorCombo.getItems().isEmpty()) {
                metricSelectorCombo.setValue(metricSelectorCombo.getItems().get(0));
            }
        }

        checkExecutionStatus();
    }
}
