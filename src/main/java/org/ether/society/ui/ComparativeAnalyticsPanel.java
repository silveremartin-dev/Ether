/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.ether.society.analytics.*;
import org.ether.society.analytics.GroundTruthAuditGenerator.AuditResult;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.persistence.ScenarioRepository;
import org.ether.society.persistence.SimulationPersistenceService;
import org.ether.society.ui.analytics.*;
import org.ether.society.ui.export.ComparativeAnalyticsExporter;
import org.ether.society.ui.util.MarkdownViewerPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 6th Tab UI Panel: Comparative Analytics &amp; Sensitivity Benchmarks.
 *
 * <p>Supports multi-scenario selection, execution status validation, automated headless
 * batch execution of missing scenarios, 1D multi-curve time-series visualization,
 * 2D spatial cartographic tensor comparison, and empirical ground truth audit reports.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ComparativeAnalyticsPanel extends BorderPane {
    static final Logger logger = LoggerFactory.getLogger(ComparativeAnalyticsPanel.class);

    private final SimulationRunRepository runRepository;
    private final ScenarioRepository scenarioRepository;
    private final SimulationPersistenceService saveManager = new SimulationPersistenceService();
    private final RootCauseAnalyzer analyzer;
    private java.util.function.Supplier<H3SimulationEngine> engineSupplier;

    public void setSimulationEngineSupplier(java.util.function.Supplier<H3SimulationEngine> engineSupplier) {
        this.engineSupplier = engineSupplier;
    }

    private final AtomicBoolean isBatchRunning = new AtomicBoolean(false);
    private final AtomicBoolean isBatchCancelled = new AtomicBoolean(false);
    private final AtomicBoolean isBatchPaused = new AtomicBoolean(false);
    private Runnable onPauseInteractiveSimulationCallback = null;
    private java.util.function.BooleanSupplier isInteractiveSimulationRunningSupplier = null;

    TableView<ScenarioSelectableItem> scenarioTable;
    ObservableList<ScenarioSelectableItem> scenarioList;
    FilteredList<ScenarioSelectableItem> filteredScenarioList;
    ComparativeScenarioTableCoordinator.TableColumns tableColumns;

    Label headerLabel;
    Label metricLabel;
    ComboBox<String> metricSelectorCombo;
    Label interpolationLabel;
    ComboBox<HistoricalValidationKernel.InterpolationMethod> interpolationCombo;
    Button copyReportBtn;
    Button exportMdBtn;
    Button exportCsvBtn;
    Button export1DCsvBtn;
    Label tableTitle;
    TextField searchField;

    private boolean isUpdatingTexts = false;

    HBox timeSeriesWarningBox;
    Label timeSeriesWarningLabel;

    TabPane mainTabPane;
    Tab scenariosTab;
    Tab timeSeriesTab;
    Tab spatialCartoTab;
    Tab culturalMatrixTab;
    Tab reportTab;

    NumberAxis xAxis;
    NumberAxis yAxis;
    LineChart<Number, Number> chart;
    Label warningLabel;
    Label executionContextBadge;
    ProgressBar batchProgressBar;
    Label etaLabel;
    Button executeMissingBtn;
    Button cancelBatchBtn;

    SpatialTensorComparisonPane spatialTensorComparisonPane;
    CulturalMatrixComparisonPane culturalMatrixComparisonPane;
    Label divergenceLabel;
    Label explanationLabel;
    MarkdownViewerPane reportPreviewPane;
    Label chartHintLabel;
    private javafx.animation.PauseTransition debouncedAnalyticsTransition;

    private void scheduleDebouncedAnalyticsUpdate() {
        if (debouncedAnalyticsTransition == null) {
            debouncedAnalyticsTransition = new javafx.animation.PauseTransition(Duration.millis(80));
            debouncedAnalyticsTransition.setOnFinished(e -> {
                updateChartAndAnalysis();
                updateReportIfActive();
                update2DSpatialComparison();
                updateCulturalMatrixComparison();
            });
        }
        debouncedAnalyticsTransition.playFromStart();
    }

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

        headerLabel = new Label();
        headerLabel.getStyleClass().add("label-title");
        topBox.getChildren().add(headerLabel);
        setTop(topBox);

        tableTitle = new Label();
        tableTitle.getStyleClass().add("label-section-header");

        searchField = new TextField();
        searchField.setPromptText(I18n.getOrDefault("analytics.search_prompt", "🔍 Filter scenarios by name, status, or year range..."));
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox filterBox = new HBox(10, tableTitle, searchField);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        scenarioTable = new TableView<>();
        VBox.setVgrow(scenarioTable, Priority.ALWAYS);

        scenarioList = FXCollections.observableArrayList();
        filteredScenarioList = new FilteredList<>(scenarioList, p -> true);
        tableColumns = ComparativeScenarioTableCoordinator.configureTable(scenarioTable, scenarioList, filteredScenarioList);

        searchField.textProperty().addListener((obs, oldVal, newVal) ->
                filteredScenarioList.setPredicate(item -> {
                    if (newVal == null || newVal.isBlank()) return true;
                    String lower = newVal.toLowerCase(Locale.ROOT);
                    return item.getName().toLowerCase(Locale.ROOT).contains(lower)
                            || item.getPlanetDisplayName().toLowerCase(Locale.ROOT).contains(lower)
                            || item.getClippingDisplay().toLowerCase(Locale.ROOT).contains(lower)
                            || item.getYearRange().toLowerCase(Locale.ROOT).contains(lower)
                            || item.getProgressPercentDisplay().toLowerCase(Locale.ROOT).contains(lower)
                            || item.getStatusDisplay().toLowerCase(Locale.ROOT).contains(lower);
                }));

        warningLabel = new Label("");
        warningLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");

        executionContextBadge = new Label();
        ComparativeAnalyticsViewHelper.updateExecutionContextBadge(this);

        VBox executionInfoBox = new VBox(3, warningLabel, executionContextBadge);
        executionInfoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(executionInfoBox, Priority.ALWAYS);

        batchProgressBar = new ProgressBar(0.0);
        batchProgressBar.setPrefWidth(140);
        batchProgressBar.setVisible(false);
        batchProgressBar.setManaged(false);

        etaLabel = new Label();
        etaLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #3b82f6;");

        executeMissingBtn = new Button();
        executeMissingBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
        executeMissingBtn.setOnAction(e -> executeMissingScenarios());

        cancelBatchBtn = new Button(I18n.getOrDefault("analytics.btn.cancel_batch", "🛑 Cancel Batch"));
        cancelBatchBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #64748b; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
        cancelBatchBtn.setVisible(false);
        cancelBatchBtn.setManaged(false);
        cancelBatchBtn.setOnAction(e -> cancelBatchExecution());

        HBox executionActionsBox = new HBox(8, batchProgressBar, etaLabel, executeMissingBtn, cancelBatchBtn);
        executionActionsBox.setAlignment(Pos.CENTER_RIGHT);

        HBox executionBox = new HBox(16, executionInfoBox, executionActionsBox);
        executionBox.setAlignment(Pos.CENTER_LEFT);
        executionBox.setPadding(new Insets(4, 0, 4, 0));

        VBox scenariosBox = new VBox(12, filterBox, scenarioTable, executionBox);
        scenariosBox.setPadding(new Insets(12));
        VBox.setVgrow(scenarioTable, Priority.ALWAYS);
        scenariosTab = new Tab(I18n.getOrDefault("analytics.tab.scenarios", "📋 Scenarios & Data"), scenariosBox);
        scenariosTab.setClosable(false);

        metricLabel = new Label();
        metricLabel.getStyleClass().add("control-label");

        metricSelectorCombo = new ComboBox<>();
        metricSelectorCombo.setPrefWidth(280);
        metricSelectorCombo.setOnAction(e -> updateChartAndAnalysis());
        metricSelectorCombo.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.metric_selector", "Select telemetry variable to plot over time across scenarios.")));

        interpolationLabel = new Label();
        interpolationLabel.getStyleClass().add("control-label");

        interpolationCombo = new ComboBox<>();
        interpolationCombo.setPrefWidth(220);
        interpolationCombo.getItems().setAll(HistoricalValidationKernel.InterpolationMethod.values());
        ComparativeAnalyticsViewHelper.setupInterpolationComboRendering(interpolationCombo);
        interpolationCombo.setValue(HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
        interpolationCombo.setOnAction(e -> updateChartAndAnalysis());
        interpolationCombo.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.interp_combo", "Reconstruction kernel used to interpolate discrete historical benchmark datasets into continuous trajectories.")));

        HBox chartControlBox = new HBox(12, metricLabel, metricSelectorCombo, interpolationLabel, interpolationCombo);
        chartControlBox.setAlignment(Pos.CENTER_LEFT);

        ComparativeAnalyticsViewHelper.configureInteractiveChart(this);
        VBox.setVgrow(chart, Priority.ALWAYS);

        chartHintLabel = new Label(I18n.getOrDefault("analytics.chart.hint", "💡 Scroll wheel: Zoom on cursor | Drag: Pan horizontally | Double-click: Reset time range"));
        chartHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-padding: 2 6;");

        export1DCsvBtn = new Button(I18n.getOrDefault("analytics.btn.export_1d_csv", "📥 Exporter les données CSV (Séries 1D)"));
        export1DCsvBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
        export1DCsvBtn.setOnAction(e -> exportCsvData());
        export1DCsvBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.export_1d_csv", "Exporter l'ensemble des séries temporelles 1D sélectionnées avec leurs valeurs interpolées au format CSV.")));

        HBox timeSeriesBottomBox = new HBox(16, chartHintLabel, export1DCsvBtn);
        timeSeriesBottomBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(chartHintLabel, Priority.ALWAYS);

        timeSeriesWarningLabel = new Label();
        timeSeriesWarningLabel.setWrapText(true);
        timeSeriesWarningBox = new HBox(8, timeSeriesWarningLabel);
        timeSeriesWarningBox.setAlignment(Pos.CENTER_LEFT);
        timeSeriesWarningBox.setVisible(false);
        timeSeriesWarningBox.setManaged(false);

        VBox timeSeriesBox = new VBox(8, chartControlBox, timeSeriesWarningBox, chart, timeSeriesBottomBox);
        timeSeriesBox.setPadding(new Insets(12));
        VBox.setVgrow(chart, Priority.ALWAYS);
        timeSeriesTab = new Tab(I18n.getOrDefault("analytics.tab.timeseries", "📈 Time Series (1D)"), timeSeriesBox);
        timeSeriesTab.setClosable(false);

        spatialTensorComparisonPane = new SpatialTensorComparisonPane(this::resolveRunRecord);
        spatialCartoTab = new Tab(I18n.getOrDefault("analytics.tab.carto_tensors", "🗺️ Cartography & Tensors (2D)"), spatialTensorComparisonPane);
        spatialCartoTab.setClosable(false);

        culturalMatrixComparisonPane = new CulturalMatrixComparisonPane();
        culturalMatrixTab = new Tab(I18n.getOrDefault("analytics.tab.cultural_matrix", "🧩 Cultural Matrices & Affinities"), culturalMatrixComparisonPane);
        culturalMatrixTab.setClosable(false);

        VBox diagHeaderBox = new VBox(6);
        diagHeaderBox.setPadding(new Insets(10));
        diagHeaderBox.getStyleClass().add("card-section");

        divergenceLabel = new Label();
        divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f59e0b; -fx-font-size: 13px;");
        explanationLabel = new Label();
        explanationLabel.setWrapText(true);
        explanationLabel.getStyleClass().add("hint-label");
        diagHeaderBox.getChildren().addAll(divergenceLabel, explanationLabel);

        reportPreviewPane = new MarkdownViewerPane();
        reportPreviewPane.setTopToolbarVisible(false);
        VBox.setVgrow(reportPreviewPane, Priority.ALWAYS);

        copyReportBtn = new Button();
        copyReportBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
        copyReportBtn.setOnAction(e -> ComparativeAnalyticsViewHelper.copyReportToClipboard(this));

        exportMdBtn = new Button();
        exportMdBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
        exportMdBtn.setOnAction(e -> exportMarkdownReport());

        exportCsvBtn = new Button();
        exportCsvBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
        exportCsvBtn.setOnAction(e -> exportCsvData());

        HBox exportBox = new HBox(8, copyReportBtn, exportMdBtn, exportCsvBtn);
        exportBox.setAlignment(Pos.CENTER_LEFT);

        VBox reportBox = new VBox(10, diagHeaderBox, reportPreviewPane, exportBox);
        reportBox.setPadding(new Insets(12));
        reportTab = new Tab(I18n.getOrDefault("analytics.tab.report", "📄 Analysis Report & Synthesis"), reportBox);
        reportTab.setClosable(false);

        mainTabPane = new TabPane(scenariosTab, timeSeriesTab, spatialCartoTab, culturalMatrixTab, reportTab);
        mainTabPane.getStyleClass().add("primary-tab-pane");
        VBox.setVgrow(mainTabPane, Priority.ALWAYS);
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == reportTab) {
                runAnalysis();
            } else if (newTab == spatialCartoTab) {
                update2DSpatialComparison();
            } else if (newTab == culturalMatrixTab) {
                updateCulturalMatrixComparison();
            } else if (spatialTensorComparisonPane != null) {
                spatialTensorComparisonPane.stopAnimation();
            }
        });

        setCenter(mainTabPane);
        refreshRunList();
    }

    public void refreshRunList() {
        ComparativeRunListLoader.refreshRunList(
                scenarioList, scenarioRepository, saveManager, runRepository, engineSupplier,
                item -> {
                    updateScenarioPairCombos();
                    checkExecutionStatus();
                    scheduleDebouncedAnalyticsUpdate();
                });
        updateScenarioPairCombos();
        checkExecutionStatus();
        updateChartAndAnalysis();
        updateReportIfActive();
        update2DSpatialComparison();
        updateCulturalMatrixComparison();
    }

    private void updateReportIfActive() {
        runAnalysis();
    }

    private void updateScenarioPairCombos() {
        if (spatialTensorComparisonPane != null) {
            spatialTensorComparisonPane.updateScenarioPairCombos(scenarioList);
        }
        if (culturalMatrixComparisonPane != null) {
            culturalMatrixComparisonPane.updateScenarioPool(scenarioList);
        }
    }

    private void updateCulturalMatrixComparison() {
        if (culturalMatrixComparisonPane != null) {
            culturalMatrixComparisonPane.updateComparison();
        }
    }

    public CulturalMatrixComparisonPane getCulturalMatrixComparisonPane() {
        return culturalMatrixComparisonPane;
    }

    public SimulationRunRecord resolveRunRecord(ScenarioSelectableItem item) {
        return ComparativeTelemetryDataLoader.resolveRunRecord(item, runRepository);
    }

    public void recalculateAllEstimates() {
        for (ScenarioSelectableItem item : scenarioList) {
            item.recalculateEstimate();
        }
        if (scenarioTable != null) scenarioTable.refresh();
        ComparativeAnalyticsViewHelper.updateExecutionContextBadge(this);
        checkExecutionStatus();
    }

    public void setInteractiveSimulationControllers(java.util.function.BooleanSupplier isRunningSupplier, Runnable pauseCallback) {
        this.isInteractiveSimulationRunningSupplier = isRunningSupplier;
        this.onPauseInteractiveSimulationCallback = pauseCallback;
    }

    public boolean isBatchRunning() {
        return isBatchRunning.get();
    }

    public void pauseOrCancelBatchForInteractiveSimulation() {
        if (isBatchRunning.get()) {
            logger.info("Pausing background comparative batch execution for interactive simulation");
            isBatchPaused.set(true);
            isBatchCancelled.set(true);
            javafx.application.Platform.runLater(() -> {
                for (ScenarioSelectableItem item : scenarioList) {
                    if (item.getBatchState() == BatchState.RUNNING || item.getBatchState() == BatchState.QUEUED) {
                        item.setBatchState(BatchState.PAUSED);
                    }
                }
                scenarioTable.refresh();
                checkExecutionStatus();
            });
        }
    }

    private void cancelBatchExecution() {
        if (isBatchRunning.get()) {
            logger.info("User requested cancellation of batch execution queue");
            isBatchCancelled.set(true);
            cancelBatchBtn.setDisable(true);
            cancelBatchBtn.setText(I18n.getOrDefault("analytics.btn.cancelling", "⏳ Stopping..."));
        }
    }

    private void checkExecutionStatus() {
        ComparativeBatchExecutionCoordinator.checkExecutionStatus(scenarioList, warningLabel, executeMissingBtn, cancelBatchBtn, isBatchRunning);
    }

    private void executeMissingScenarios() {
        ComparativeBatchExecutionCoordinator.executeMissingScenarios(
                scenarioList, scenarioTable, executeMissingBtn, cancelBatchBtn,
                batchProgressBar, etaLabel, isBatchRunning, isBatchCancelled, isBatchPaused,
                runRepository, isInteractiveSimulationRunningSupplier, onPauseInteractiveSimulationCallback,
                () -> {
                    checkExecutionStatus();
                    updateScenarioPairCombos();
                    runAnalysis();
                });
    }

    private void runAnalysis() {
        List<ScenarioSelectableItem> selectedExecuted = scenarioList.stream()
                .filter(i -> i.isSelected() && i.isExecuted())
                .toList();

        if (selectedExecuted.size() < 2) {
            divergenceLabel.setText(I18n.getOrDefault("analytics.divergence.break_point", "Break point: Select at least 2 executed scenarios"));
            divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #b45309; -fx-font-size: 13px;");
            explanationLabel.setText(I18n.getOrDefault("analytics.divergence.select_two_hint", "Check at least two scenarios in table (e.g. Historical Reality + Simulated Scenario) to analyze divergences."));
            reportPreviewPane.setMarkdown(I18n.getOrDefault("analytics.divergence.prompt_report", "## Please select at least two executed scenarios to generate comparative summary and audit report."));
            return;
        }

        HistoricalValidationKernel.InterpolationMethod method = (interpolationCombo != null && interpolationCombo.getValue() != null)
                ? interpolationCombo.getValue() : HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC;
        updateAuditResultLabels(GroundTruthAuditGenerator.generateUnifiedComparativeReport(
                selectedExecuted, method, this::resolveRunRecord, analyzer, runRepository));
    }

    private void updateAuditResultLabels(AuditResult result) {
        if (result.divergenceYear() != -1) {
            divergenceLabel.setText(String.format(I18n.getOrDefault("analytics.status.first_divergence", "Point de rupture détecté (T_divergence) : ANNÉE %d"), result.divergenceYear()));
        } else {
            divergenceLabel.setText(I18n.getOrDefault("analytics.status.parallel", "Trajectoires parallèles (Aucune divergence > 10%)"));
        }
        divergenceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        explanationLabel.setText(result.explanationSummary());
        reportPreviewPane.setMarkdown(result.reportMarkdown());
    }

    void updateChartAndAnalysis() {
        ComparativeTimeSeriesChartCoordinator.updateChartAndAnalysis(
                chart, xAxis, yAxis, scenarioList, metricSelectorCombo, interpolationCombo,
                timeSeriesWarningBox, timeSeriesWarningLabel, this::resolveRunRecord, isUpdatingTexts);
    }

    private void update2DSpatialComparison() {
        if (spatialTensorComparisonPane != null) {
            spatialTensorComparisonPane.update2DSpatialComparison();
        }
    }

    public SpatialTensorComparisonPane getSpatialTensorComparisonPane() {
        return spatialTensorComparisonPane;
    }

    private void exportMarkdownReport() {
        String currentReport = (reportPreviewPane != null) ? reportPreviewPane.getMarkdown() : "";
        ComparativeAnalyticsExporter.exportMarkdownReport(currentReport, exportMdBtn);
    }

    private void exportCsvData() {
        ComparativeAnalyticsExporter.exportCsvData(scenarioList, exportCsvBtn, this::resolveRunRecord);
    }

    public void updateTexts() {
        isUpdatingTexts = true;
        try {
            ComparativeAnalyticsViewHelper.updateTexts(this);
            if (scenarioTable != null) scenarioTable.refresh();
            checkExecutionStatus();
            updateScenarioPairCombos();
        } finally {
            isUpdatingTexts = false;
        }
    }
}
