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
import org.ether.society.ui.util.MarkdownViewerPane;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import java.util.Comparator;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.persistence.SimulationSaveManager;
import org.ether.society.persistence.SaveMetadata;

/**
 * 6th Tab UI Panel: Comparative Analytics & Sensitivity Benchmarks.
 * Supports multi-scenario selection, execution status validation, automated headless
 * batch execution of missing scenarios, 1D multi-curve time-series visualization,
 * 2D spatial cartographic tensor comparison with timeline date scrubber, and root cause analysis.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class ComparativeAnalyticsPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ComparativeAnalyticsPanel.class);

    public enum BatchState {
        NOT_EXECUTED,
        QUEUED,
        RUNNING,
        EXECUTED,
        PAUSED
    }

    public static class ScenarioSelectableItem {
        private final Scenario scenario;
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        private boolean executed;
        private String runId;
        private BatchState batchState = BatchState.NOT_EXECUTED;
        private double progress = 0.0;
        private int queueIndex = 0;
        private double estimatedDurationSec = 0.0;
        private double remainingDurationSec = 0.0;

        public ScenarioSelectableItem(Scenario scenario, boolean isSelected, boolean executed, String runId) {
            this.scenario = scenario;
            this.selected.set(isSelected);
            this.executed = executed;
            this.runId = runId;
            if ("HISTORICAL_GROUND_TRUTH".equals(runId)) {
                this.progress = 1.0;
                this.executed = true;
                this.batchState = BatchState.EXECUTED;
                this.estimatedDurationSec = 0.0;
                this.remainingDurationSec = 0.0;
            } else if (executed) {
                this.progress = 1.0;
                this.batchState = BatchState.EXECUTED;
                this.estimatedDurationSec = 0.0;
                this.remainingDurationSec = 0.0;
            } else {
                this.batchState = BatchState.NOT_EXECUTED;
                recalculateEstimate();
            }
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

        public boolean isExecuted() { return executed || progress >= 1.0; }
        public void setExecuted(boolean executed) { 
            this.executed = executed; 
            if (executed) {
                this.batchState = BatchState.EXECUTED;
                this.progress = 1.0;
                this.remainingDurationSec = 0.0;
            }
        }
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }

        public BatchState getBatchState() { return batchState; }
        public void setBatchState(BatchState batchState) { this.batchState = batchState; }

        public double getProgress() { return progress; }
        public void setProgress(double progress) { 
            this.progress = Math.max(0.0, Math.min(1.0, progress));
            if (this.progress >= 1.0) {
                this.executed = true;
                this.remainingDurationSec = 0.0;
                if (this.batchState != BatchState.RUNNING) {
                    this.batchState = BatchState.EXECUTED;
                }
            } else {
                this.executed = false;
                this.remainingDurationSec = Math.max(0.0, this.estimatedDurationSec * (1.0 - this.progress));
            }
        }

        public String getProgressPercentDisplay() {
            return String.format("%d%%", (int) Math.round(progress * 100.0));
        }

        public int getQueueIndex() { return queueIndex; }
        public void setQueueIndex(int queueIndex) { this.queueIndex = queueIndex; }

        public double getEstimatedDurationSec() { return estimatedDurationSec; }
        public double getRemainingDurationSec() { return remainingDurationSec; }

        public void recalculateEstimate() {
            if ("HISTORICAL_GROUND_TRUTH".equals(runId)) {
                this.estimatedDurationSec = 0.0;
                this.remainingDurationSec = 0.0;
                this.progress = 1.0;
                this.executed = true;
                return;
            }
            if (scenario != null) {
                int cellCount = scenario.getInitialHumanCount() > 0 ? (int) Math.min(5000, 50 + scenario.getInitialHumanCount() / 2000) : 4096;
                this.estimatedDurationSec = ExecutionContextPanel.estimateExecutionTimeSeconds(
                    scenario.getStartDateYear(),
                    scenario.getEndDateYear(),
                    cellCount,
                    ExecutionContextPanel.getActiveHardwareMode()
                );
                if (progress >= 1.0 || executed) {
                    this.remainingDurationSec = 0.0;
                } else {
                    this.remainingDurationSec = Math.max(0.0, this.estimatedDurationSec * (1.0 - this.progress));
                }
            }
        }

        public String getEstimatedDurationDisplay() {
            if ("HISTORICAL_GROUND_TRUTH".equals(runId) || (progress >= 1.0 && executed)) return "-";
            return ExecutionContextPanel.formatDuration(remainingDurationSec);
        }

        public String getStatusDisplay() { 
            if ("HISTORICAL_GROUND_TRUTH".equals(runId)) {
                return I18n.getOrDefault("analytics.status.ground_truth_ready", "🟢 Historical Ground Truth (HYDE / Maddison / Seshat)");
            }
            if (progress >= 1.0 || executed) {
                return String.format(I18n.getOrDefault("analytics.status.executed", "🟢 Executed (%s)"), runId != null && !runId.equals("N/A") ? runId : "OK");
            }
            if (progress > 0.0) {
                return String.format(I18n.getOrDefault("analytics.status.partial_fmt", "🟡 Partial (%d%%)"), (int) Math.round(progress * 100.0));
            }
            return I18n.getOrDefault("analytics.status.not_executed", "🔴 Not executed (Pending)"); 
        }
    }

    private final SimulationRunRepository runRepository;
    private final ScenarioRepository scenarioRepository;
    private final SimulationSaveManager saveManager = new SimulationSaveManager();
    private final RootCauseAnalyzer analyzer;
    private java.util.function.Supplier<H3SimulationEngine> engineSupplier;

    public void setSimulationEngineSupplier(java.util.function.Supplier<H3SimulationEngine> engineSupplier) {
        this.engineSupplier = engineSupplier;
    }

    private final java.util.concurrent.atomic.AtomicBoolean isBatchRunning = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean isBatchCancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean isBatchPaused = new java.util.concurrent.atomic.AtomicBoolean(false);
    private Thread batchWorkerThread = null;
    private Runnable onPauseInteractiveSimulationCallback = null;
    private java.util.function.BooleanSupplier isInteractiveSimulationRunningSupplier = null;

    private TableView<ScenarioSelectableItem> scenarioTable;
    private ObservableList<ScenarioSelectableItem> scenarioList;
    private FilteredList<ScenarioSelectableItem> filteredScenarioList;

    private Label headerLabel;
    private Label metricLabel;
    private ComboBox<String> metricSelectorCombo;
    private Label interpolationLabel;
    private ComboBox<HistoricalValidationKernel.InterpolationMethod> interpolationCombo;
    private Button analyzeBtn;
    private Button exportMdBtn;
    private Button exportCsvBtn;
    private Label tableTitle;
    private TextField searchField;

    private TableColumn<ScenarioSelectableItem, Boolean> selectCol;
    private boolean isUpdatingTexts = false;
    private TableColumn<ScenarioSelectableItem, String> nameCol;
    private TableColumn<ScenarioSelectableItem, String> yearsCol;
    private TableColumn<ScenarioSelectableItem, Double> progressCol;
    private TableColumn<ScenarioSelectableItem, String> estDurationCol;
    private TableColumn<ScenarioSelectableItem, String> statusCol;

    private TabPane analyticsTabPane;
    private Tab timeSeriesTab;
    private Tab spatialCartoTab;

    private NumberAxis xAxis;
    private NumberAxis yAxis;
    private LineChart<Number, Number> chart;
    private Label warningLabel;
    private Label executionContextBadge;
    private ProgressBar batchProgressBar;
    private Label etaLabel;
    private Button executeMissingBtn;
    private Button cancelBatchBtn;

    // 2D Spatial Tensor Comparison Controls
    private ComboBox<String> spatialChannelCombo;
    private Label channelLabel;
    private Slider dateSlider;
    private Label currentDateLabel;
    private Button playTimelineBtn;
    private ImageView mapImageViewA;
    private ImageView mapImageViewB;
    private ImageView mapImageViewDiff;
    private Label mapLabelA;
    private Label mapLabelB;
    private Label mapLabelDiff;
    private TextArea spatialMetricsReportArea;
    private Timeline timelineAnimation;
    private boolean isPlayingAnimation = false;

    private Label diagHeader;
    private Label divergenceLabel;
    private Label explanationLabel;
    private Label synthHeader;
    private MarkdownViewerPane reportPreviewPane;

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
        headerLabel.getStyleClass().add("label-title");
        topBox.getChildren().add(headerLabel);
        setTop(topBox);

        // Center SplitPane: Left (Section 1: Table, Section 2: Execution Bar, Section 3: Chart), Right (Section 4: Diag & Reports)
        SplitPane mainSplit = new SplitPane();
        mainSplit.setStyle("-fx-background-color: transparent;");

        VBox leftPane = new VBox(10);

        // --- SECTION 1: SCENARIO SELECTION TABLE & FILTER ---
        tableTitle = new Label();
        tableTitle.getStyleClass().add("label-section-header");

        searchField = new TextField();
        searchField.setPromptText(I18n.getOrDefault("analytics.search_prompt", "🔍 Filter scenarios by name, status, or year range..."));
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox filterBox = new HBox(10, tableTitle, searchField);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        scenarioTable = new TableView<>();
        scenarioTable.setPrefHeight(180);
        scenarioTable.setEditable(true);

        // Single-click or Double-click row toggles scenario selection
        scenarioTable.setRowFactory(tv -> {
            TableRow<ScenarioSelectableItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    ScenarioSelectableItem item = row.getItem();
                    if (item != null) {
                        // Check if click target was directly on the CheckBox widget inside the cell
                        boolean isCheckBoxClick = false;
                        if (event.getTarget() instanceof javafx.scene.Node) {
                            javafx.scene.Node target = (javafx.scene.Node) event.getTarget();
                            while (target != null && target != row) {
                                if (target instanceof CheckBox) {
                                    isCheckBoxClick = true;
                                    break;
                                }
                                target = target.getParent();
                            }
                        }
                        if (!isCheckBoxClick) {
                            item.setSelected(!item.isSelected());
                        }
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
        selectCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                setAlignment(Pos.CENTER);
                checkBox.setMnemonicParsing(false);
                checkBox.setOnAction(e -> {
                    ScenarioSelectableItem item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setSelected(checkBox.isSelected());
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item);
                    setGraphic(checkBox);
                }
            }
        });
        selectCol.setPrefWidth(80);

        nameCol = new TableColumn<>();
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(220);

        yearsCol = new TableColumn<>();
        yearsCol.setCellValueFactory(new PropertyValueFactory<>("yearRange"));
        yearsCol.setPrefWidth(130);

        progressCol = new TableColumn<>();
        progressCol.setCellValueFactory(p -> new javafx.beans.property.SimpleDoubleProperty(p.getValue().getProgress()).asObject());
        progressCol.setPrefWidth(130);
        progressCol.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar progressBar = new ProgressBar(0.0);
            private final Label percentLabel = new Label("0%");
            private final HBox container = new HBox(6, progressBar, percentLabel);
            {
                progressBar.setPrefWidth(65);
                progressBar.setMaxWidth(65);
                progressBar.setPrefHeight(10);
                container.setAlignment(Pos.CENTER_LEFT);
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    ScenarioSelectableItem scItem = getTableRow().getItem();
                    double prog = scItem.getProgress();
                    progressBar.setProgress(prog);
                    percentLabel.setText(scItem.getProgressPercentDisplay());
                    if (prog >= 1.0) {
                        progressBar.setStyle("-fx-accent: #10b981;");
                        percentLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else if (prog > 0.0) {
                        progressBar.setStyle("-fx-accent: #f59e0b;");
                        percentLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else {
                        progressBar.setStyle("-fx-accent: #64748b;");
                        percentLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                    }
                    setGraphic(container);
                    setText(null);
                }
            }
        });

        estDurationCol = new TableColumn<>();
        estDurationCol.setCellValueFactory(new PropertyValueFactory<>("estimatedDurationDisplay"));
        estDurationCol.setPrefWidth(110);
        estDurationCol.setStyle("-fx-alignment: CENTER;");

        statusCol = new TableColumn<>();
        statusCol.setCellValueFactory(new PropertyValueFactory<>("statusDisplay"));
        statusCol.setPrefWidth(240);
        statusCol.setCellFactory(col -> new TableCell<>() {
            private final ProgressIndicator spinner = new ProgressIndicator();
            private final Label label = new Label();
            private final HBox container = new HBox(6, spinner, label);
            {
                spinner.setMaxSize(16, 16);
                spinner.setPrefSize(16, 16);
                container.setAlignment(Pos.CENTER_LEFT);
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    ScenarioSelectableItem scItem = getTableRow().getItem();
                    switch (scItem.getBatchState()) {
                        case RUNNING -> {
                            spinner.setProgress(scItem.getProgress() > 0 ? scItem.getProgress() : -1);
                            label.setText(String.format(I18n.getOrDefault("analytics.status.running_fmt", "🔄 Running (%d%%)"), (int) (scItem.getProgress() * 100)));
                            label.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                            setGraphic(container);
                            setText(null);
                        }
                        case QUEUED -> {
                            label.setText(String.format(I18n.getOrDefault("analytics.status.queued_fmt", "⏳ Queued (#%d)"), scItem.getQueueIndex()));
                            label.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                            setGraphic(label);
                            setText(null);
                        }
                        case PAUSED -> {
                            label.setText(I18n.getOrDefault("analytics.status.paused", "⏸️ Paused"));
                            label.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;");
                            setGraphic(label);
                            setText(null);
                        }
                        case EXECUTED -> {
                            label.setText(scItem.getStatusDisplay());
                            label.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                            setGraphic(label);
                            setText(null);
                        }
                        default -> {
                            label.setText(scItem.getStatusDisplay());
                            label.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                            setGraphic(label);
                            setText(null);
                        }
                    }
                }
            }
        });

        scenarioTable.getColumns().addAll(selectCol, nameCol, yearsCol, progressCol, estDurationCol, statusCol);

        selectCol.setComparator((a, b) -> Boolean.compare(a, b));
        nameCol.setComparator(String.CASE_INSENSITIVE_ORDER);
        yearsCol.setComparator((a, b) -> a.compareTo(b));
        progressCol.setComparator(Double::compare);
        estDurationCol.setComparator((a, b) -> a.compareTo(b));
        statusCol.setComparator(String.CASE_INSENSITIVE_ORDER);

        scenarioList = FXCollections.observableArrayList();
        filteredScenarioList = new FilteredList<>(scenarioList, p -> true);
        SortedList<ScenarioSelectableItem> sortedScenarioList = new SortedList<>(filteredScenarioList);
        sortedScenarioList.comparatorProperty().bind(Bindings.createObjectBinding(() -> {
            Comparator<ScenarioSelectableItem> baseComp = scenarioTable.getComparator();
            return (a, b) -> {
                if (a == null && b == null) return 0;
                if (a == null) return 1;
                if (b == null) return -1;
                if ("HISTORICAL_GROUND_TRUTH".equals(a.getRunId())) return -1;
                if ("HISTORICAL_GROUND_TRUTH".equals(b.getRunId())) return 1;
                if (baseComp != null) {
                    return baseComp.compare(a, b);
                }
                return 0;
            };
        }, scenarioTable.comparatorProperty()));

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredScenarioList.setPredicate(item -> {
                if (newVal == null || newVal.isBlank()) return true;
                String lower = newVal.toLowerCase();
                return item.getName().toLowerCase().contains(lower)
                    || item.getYearRange().toLowerCase().contains(lower)
                    || item.getProgressPercentDisplay().toLowerCase().contains(lower)
                    || item.getStatusDisplay().toLowerCase().contains(lower);
            });
        });

        scenarioTable.setRowFactory(tv -> {
            TableRow<ScenarioSelectableItem> row = new TableRow<>() {
                @Override
                protected void updateItem(ScenarioSelectableItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else if ("HISTORICAL_GROUND_TRUTH".equals(item.getRunId())) {
                        setStyle("-fx-border-color: transparent transparent #38bdf8 transparent; -fx-border-width: 0 0 3px 0; -fx-border-style: solid; -fx-background-color: rgba(56, 189, 248, 0.12); -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            };
            return row;
        });

        scenarioTable.setItems(sortedScenarioList);

        // --- SECTION 2: EXECUTION CONTROL & STATUS BAR (Directly under Table) ---
        warningLabel = new Label("");
        warningLabel.setStyle("-fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 4;");
        HBox.setHgrow(warningLabel, Priority.ALWAYS);

        executionContextBadge = new Label();
        executionContextBadge.setStyle("-fx-font-weight: bold; -fx-text-fill: #3b82f6; -fx-padding: 4 8; -fx-background-color: rgba(59, 130, 246, 0.12); -fx-background-radius: 4; -fx-border-color: #3b82f6; -fx-border-radius: 4;");
        updateExecutionContextBadge();

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

        HBox executionBox = new HBox(8, warningLabel, executionContextBadge, batchProgressBar, etaLabel, executeMissingBtn, cancelBatchBtn);
        executionBox.setAlignment(Pos.CENTER_LEFT);

        leftPane.getChildren().addAll(filterBox, scenarioTable, executionBox);

        // --- SECTION 3: TABPANE FOR 1D TIME-SERIES & 2D SPATIAL TENSOR COMPARISON ---
        analyticsTabPane = new TabPane();
        VBox.setVgrow(analyticsTabPane, Priority.ALWAYS);

        // --- TAB 1: 1D TIME-SERIES CHART ---
        metricLabel = new Label();
        metricLabel.getStyleClass().add("control-label");

        metricSelectorCombo = new ComboBox<>();
        metricSelectorCombo.setPrefWidth(280);
        metricSelectorCombo.setOnAction(e -> updateChartAndAnalysis());

        interpolationLabel = new Label();
        interpolationLabel.getStyleClass().add("control-label");

        interpolationCombo = new ComboBox<>();
        interpolationCombo.setPrefWidth(220);
        interpolationCombo.getItems().setAll(HistoricalValidationKernel.InterpolationMethod.values());
        interpolationCombo.setValue(HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
        interpolationCombo.setOnAction(e -> updateChartAndAnalysis());

        HBox chartControlBox = new HBox(12, metricLabel, metricSelectorCombo, interpolationLabel, interpolationCombo);
        chartControlBox.setAlignment(Pos.CENTER_LEFT);

        xAxis = new NumberAxis();
        yAxis = new NumberAxis();

        chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setTitle(I18n.getOrDefault("analytics.chart.title", "Multi-Scenario Chronological Overlay (💡 CTRL + Scroll to Zoom, CTRL + Drag to Pan)"));
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
        timeSeriesTab = new Tab(I18n.getOrDefault("analytics.tab.timeseries", "📈 Time Series (1D)"), timeSeriesBox);
        timeSeriesTab.setClosable(false);

        // --- TAB 2: 2D SPATIAL TENSOR & CARTOGRAPHIC COMPARISON ---
        VBox spatialBox = new VBox(8);
        spatialBox.setPadding(new Insets(8));

        // Spatial Channel Selector & Controls
        channelLabel = new Label(I18n.getOrDefault("analytics.label.channel", "Canal Tensoriel :"));
        channelLabel.getStyleClass().add("control-label");

        spatialChannelCombo = new ComboBox<>();
        spatialChannelCombo.getItems().addAll(
            I18n.getOrDefault("analytics.spatial.density", "👥 Demographic Density"),
            I18n.getOrDefault("analytics.spatial.technology", "🔬 Niveau Technologique (Outillage)"),
            I18n.getOrDefault("analytics.spatial.temperature", "🌡️ Température & Climat"),
            I18n.getOrDefault("analytics.spatial.aquifers", "💧 Aquifères & Eau Douce"),
            I18n.getOrDefault("analytics.spatial.agriculture", "🌾 Biomasse & Terres Agricoles"),
            I18n.getOrDefault("analytics.spatial.sovereignty", "👑 Political Sovereignty"),
            I18n.getOrDefault("analytics.spatial.linguistic", "🗣️ Isoglosses Linguistiques (Langues)"),
            I18n.getOrDefault("analytics.spatial.kinship", "🧬 Kinship Structure (Parenté)"),
            I18n.getOrDefault("analytics.spatial.rituals", "🔮 Sacred Rituals & Beliefs (Rituels)"),
            I18n.getOrDefault("analytics.spatial.trade", "🐫 Trade Corridors & Exchange (Commerce)"),
            I18n.getOrDefault("analytics.spatial.institutional", "⚖️ Institutional Complexity (Seshat)"),
            I18n.getOrDefault("analytics.spatial.ecological", "⚠️ Ecological Footprint (Empreinte)"),
            I18n.getOrDefault("analytics.spatial.pathogen", "🧬 Pathogen & Zoonotic Immunity (Santé)")
        );
        spatialChannelCombo.setValue(I18n.getOrDefault("analytics.spatial.density", "👥 Demographic Density"));
        spatialChannelCombo.setOnAction(e -> update2DSpatialComparison());

        currentDateLabel = new Label(I18n.getOrDefault("analytics.label.year_ad", "📅 Year: 0 AD"));
        currentDateLabel.getStyleClass().add("value-label");

        playTimelineBtn = new Button(I18n.getOrDefault("analytics.btn.play_timeline", "▶️ Lecture Temporelle"));
        playTimelineBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand;");
        playTimelineBtn.setOnAction(e -> toggleDateAnimation());

        dateSlider = new Slider(0, 2000, 0);
        dateSlider.setBlockIncrement(50);
        dateSlider.setMajorTickUnit(500);
        dateSlider.setMinorTickCount(4);
        dateSlider.setShowTickMarks(true);
        dateSlider.setShowTickLabels(true);
        dateSlider.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.date_slider", "Drag to navigate historical chronological timeline of comparative maps.")));
        HBox.setHgrow(dateSlider, Priority.ALWAYS);
        dateSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int year = newV.intValue();
            currentDateLabel.setText(String.format(I18n.getOrDefault("analytics.label.year_ad_formatted", "📅 Year: %d AD"), year));
            update2DSpatialComparison();
        });

        HBox spatialControlBox = new HBox(10, channelLabel, spatialChannelCombo, dateSlider, currentDateLabel, playTimelineBtn);
        spatialControlBox.setAlignment(Pos.CENTER_LEFT);

        // 3-Map Side-by-Side Spatial Map Viewers (A, B, and Difference Heatmap)
        mapLabelA = new Label(I18n.getOrDefault("analytics.label.scenario_a_ref", "Scenario A (Reference)"));
        mapLabelA.getStyleClass().add("label-section-header");
        mapImageViewA = new ImageView();
        mapImageViewA.setFitWidth(230);
        mapImageViewA.setFitHeight(125);
        mapImageViewA.setPreserveRatio(true);
        mapImageViewA.getStyleClass().add("card-section");

        VBox mapBoxA = new VBox(4, mapLabelA, mapImageViewA);
        mapBoxA.setAlignment(Pos.CENTER);

        mapLabelB = new Label(I18n.getOrDefault("analytics.label.scenario_b_target", "Scenario B (Target)"));
        mapLabelB.getStyleClass().add("label-section-header");
        mapImageViewB = new ImageView();
        mapImageViewB.setFitWidth(230);
        mapImageViewB.setFitHeight(125);
        mapImageViewB.setPreserveRatio(true);
        mapImageViewB.getStyleClass().add("card-section");

        VBox mapBoxB = new VBox(4, mapLabelB, mapImageViewB);
        mapBoxB.setAlignment(Pos.CENTER);

        mapLabelDiff = new Label(I18n.getOrDefault("analytics.label.scenario_diff", "Discrepancy Heatmap Δ(A - B)"));
        mapLabelDiff.getStyleClass().add("label-section-header");
        mapLabelDiff.setStyle("-fx-text-fill: #f59e0b;");
        mapImageViewDiff = new ImageView();
        mapImageViewDiff.setFitWidth(230);
        mapImageViewDiff.setFitHeight(125);
        mapImageViewDiff.setPreserveRatio(true);
        mapImageViewDiff.getStyleClass().add("card-section");

        VBox mapBoxDiff = new VBox(4, mapLabelDiff, mapImageViewDiff);
        mapBoxDiff.setAlignment(Pos.CENTER);

        HBox mapPairBox = new HBox(12, mapBoxA, mapBoxB, mapBoxDiff);
        mapPairBox.setAlignment(Pos.CENTER);

        // Quantitative Spatial Comparison Metrics Area
        spatialMetricsReportArea = new TextArea();
        spatialMetricsReportArea.setEditable(false);
        spatialMetricsReportArea.setPrefRowCount(7);
        spatialMetricsReportArea.getStyleClass().add("scenario-description-area");
        VBox.setVgrow(spatialMetricsReportArea, Priority.ALWAYS);

        spatialBox.getChildren().addAll(spatialControlBox, mapPairBox, spatialMetricsReportArea);
        spatialCartoTab = new Tab(I18n.getOrDefault("analytics.tab.carto_tensors", "🗺️ Cartographie & Tenseurs (2D)"), spatialBox);
        spatialCartoTab.setClosable(false);

        analyticsTabPane.getTabs().addAll(timeSeriesTab, spatialCartoTab);
        leftPane.getChildren().add(analyticsTabPane);

        // --- SECTION 4: DIAGNOSTIC REPORTS & EXPORT BUTTONS (Right Pane) ---
        VBox diagBox = new VBox(10);
        diagBox.setPadding(new Insets(12));
        diagBox.getStyleClass().add("card-section");

        diagHeader = new Label();
        diagHeader.getStyleClass().add("label-title");

        divergenceLabel = new Label();
        divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f59e0b;");

        explanationLabel = new Label();
        explanationLabel.setWrapText(true);
        explanationLabel.getStyleClass().add("hint-label");

        synthHeader = new Label();
        synthHeader.getStyleClass().add("label-section-header");

        reportPreviewPane = new MarkdownViewerPane();
        VBox.setVgrow(reportPreviewPane, Priority.ALWAYS);

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

        diagBox.getChildren().addAll(diagHeader, divergenceLabel, explanationLabel, synthHeader, reportPreviewPane, exportBox);

        mainSplit.getItems().addAll(leftPane, diagBox);
        mainSplit.setDividerPositions(0.62);

        setCenter(mainSplit);

        refreshRunList();
    }

    public void refreshRunList() {
        Set<String> previousSelectedNames = new HashSet<>();
        for (ScenarioSelectableItem it : scenarioList) {
            if (it.isSelected()) {
                previousSelectedNames.add(it.getName());
                if (it.getScenario() != null && it.getScenario().getId() != null) {
                    previousSelectedNames.add(String.valueOf(it.getScenario().getId()));
                }
                if (it.getRunId() != null) {
                    previousSelectedNames.add(it.getRunId());
                }
            }
        }

        scenarioList.clear();

        // Special Historical Ground Truth Baseline Item
        Scenario histScenario = new Scenario();
        histScenario.setName(I18n.getOrDefault("analytics.scenario.ground_truth", "🌍 Historical Reality (Cliodynamic Ground Truth)"));
        histScenario.setStartDateYear(-100000);
        histScenario.setEndDateYear(2026);
        boolean wasHistSelected = previousSelectedNames.isEmpty() || previousSelectedNames.contains("HISTORICAL_GROUND_TRUTH") || previousSelectedNames.contains(histScenario.getName());
        ScenarioSelectableItem histItem = new ScenarioSelectableItem(histScenario, wasHistSelected, true, "HISTORICAL_GROUND_TRUTH");
        histItem.selectedProperty().addListener((obs, oldV, newV) -> {
            checkExecutionStatus();
            updateChartAndAnalysis();
        });
        scenarioList.add(histItem);

        // 1. Available scenarios from repository
        List<Scenario> allScenarios = scenarioRepository.getAllScenarios();

        // 2. Available simulation saves on disk
        List<SaveMetadata> allSaves = Collections.emptyList();
        try {
            allSaves = saveManager.listSaves();
        } catch (Exception e) {
            logger.warn("Failed to list saves for analytics revalidation: {}", e.getMessage());
        }

        // 3. Active interactive simulation engine
        H3SimulationEngine activeEngine = (engineSupplier != null) ? engineSupplier.get() : null;

        for (Scenario sc : allScenarios) {
            String scName = sc.getDisplayName();
            if (scName == null || scName.isBlank()) {
                scName = "Scénario Sans Nom" + (sc.getId() != null ? " (#" + sc.getId() + ")" : "");
            }

            // A. Check SimulationRunRepository for completed or partial runs
            Optional<SimulationRunRecord> recordOpt = runRepository.getRunByScenarioName(sc.getName());
            if (!recordOpt.isPresent()) {
                recordOpt = runRepository.getRunByScenarioName(scName);
            }
            if (!recordOpt.isPresent() && sc.getId() != null) {
                recordOpt = Optional.ofNullable(runRepository.getRun(String.valueOf(sc.getId())));
            }

            // B. Check saved snapshots on disk matching scenario name
            SaveMetadata matchingSave = null;
            for (SaveMetadata save : allSaves) {
                if (save.getScenarioName() != null && (save.getScenarioName().equalsIgnoreCase(sc.getName()) || save.getScenarioName().equalsIgnoreCase(scName))) {
                    if (matchingSave == null || save.getYear() > matchingSave.getYear()) {
                        matchingSave = save;
                    }
                }
            }

            // C. Check if currently active interactive simulation matches this scenario
            boolean isInteractiveMatch = (activeEngine != null 
                && activeEngine.getCurrentScenario() != null 
                && (activeEngine.getCurrentScenario().getName().equalsIgnoreCase(sc.getName())
                    || (sc.getId() != null && sc.getId().equals(activeEngine.getCurrentScenario().getId()))));

            long startYear = sc.getStartDateYear();
            long endYear = sc.getEndDateYear();
            long totalYears = Math.max(1, endYear - startYear);

            long maxYearReached = startYear;
            String runId = "N/A";

            if (recordOpt.isPresent()) {
                SimulationRunRecord rec = recordOpt.get();
                runId = rec.getRunId();
                if (!rec.getTimeSeriesData().isEmpty()) {
                    maxYearReached = Math.max(maxYearReached, rec.getEndYear());
                }
            }

            if (matchingSave != null) {
                maxYearReached = Math.max(maxYearReached, matchingSave.getYear());
                if ("N/A".equals(runId)) {
                    runId = "SAVE-" + matchingSave.getId().substring(0, Math.min(8, matchingSave.getId().length())).toUpperCase();
                }
            }

            if (isInteractiveMatch) {
                maxYearReached = Math.max(maxYearReached, activeEngine.getCurrentYear());
            }

            double progress = Math.min(1.0, Math.max(0.0, (double) (maxYearReached - startYear) / totalYears));
            if (recordOpt.isPresent() && recordOpt.get().getEndYear() >= endYear) {
                progress = 1.0;
            }
            boolean executed = (progress >= 1.0);

            boolean isSelected = previousSelectedNames.contains(sc.getName())
                || previousSelectedNames.contains(scName)
                || (sc.getId() != null && previousSelectedNames.contains(String.valueOf(sc.getId())))
                || ("N/A".equals(runId) ? false : previousSelectedNames.contains(runId));

            ScenarioSelectableItem item = new ScenarioSelectableItem(sc, isSelected, executed, runId);
            item.setProgress(progress);
            item.recalculateEstimate();

            item.selectedProperty().addListener((obs, oldV, newV) -> {
                checkExecutionStatus();
                updateChartAndAnalysis();
            });

            scenarioList.add(item);
        }

        checkExecutionStatus();
        updateChartAndAnalysis();
    }

    public void recalculateAllEstimates() {
        for (ScenarioSelectableItem item : scenarioList) {
            item.recalculateEstimate();
        }
        if (scenarioTable != null) {
            scenarioTable.refresh();
        }
        updateExecutionContextBadge();
        checkExecutionStatus();
    }

    private void updateExecutionContextBadge() {
        if (executionContextBadge == null) return;
        ExecutionContextPanel.HardwareMode mode = ExecutionContextPanel.getActiveHardwareMode();
        int cores = Runtime.getRuntime().availableProcessors();
        String modeName = mode != null ? mode.name() : "CPU_JIT";
        executionContextBadge.setText(String.format("⚙️ Contexte : %s (%d cœurs, Headless)", modeName, cores));
        executionContextBadge.setTooltip(new Tooltip(String.format(
            "Exécution batch headless pilotée par le Contexte d'Exécution :\n• Moteur matériel actif : %s\n• Parallélisme alloué : %d cœurs\n• Débit estimé : %,.0f cellules/sec",
            modeName, cores, ExecutionContextPanel.getEstimatedCellTicksThroughput(mode)
        )));
    }

    public void setInteractiveSimulationControllers(
        java.util.function.BooleanSupplier isRunningSupplier,
        Runnable pauseCallback
    ) {
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
        List<ScenarioSelectableItem> selected = scenarioList.stream()
            .filter(ScenarioSelectableItem::isSelected)
            .toList();

        long unexecutedCount = selected.stream().filter(i -> !i.isExecuted() && !"HISTORICAL_GROUND_TRUTH".equals(i.getRunId())).count();

        if (selected.isEmpty()) {
            warningLabel.setText(I18n.getOrDefault("analytics.warning.none_selected", "ℹ️ No scenario selected for comparison."));
            warningLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-padding: 6 10; -fx-background-color: rgba(226, 232, 240, 0.5); -fx-background-radius: 4;");
            executeMissingBtn.setText(I18n.getOrDefault("analytics.btn.execute_scenarios", "🚀 Execute Scenarios"));
            executeMissingBtn.setDisable(true);
            cancelBatchBtn.setVisible(false);
            cancelBatchBtn.setManaged(false);
        } else if (unexecutedCount > 0) {
            long totalUnexecutedYears = selected.stream()
                .filter(i -> !i.isExecuted() && !"HISTORICAL_GROUND_TRUTH".equals(i.getRunId()))
                .mapToLong(i -> Math.max(1, (long)((1.0 - i.getProgress()) * (i.getScenario().getEndDateYear() - i.getScenario().getStartDateYear()))))
                .sum();
            double totalEstSeconds = selected.stream()
                .filter(i -> !i.isExecuted() && !"HISTORICAL_GROUND_TRUTH".equals(i.getRunId()))
                .mapToDouble(ScenarioSelectableItem::getRemainingDurationSec)
                .sum();
            String durStr = ExecutionContextPanel.formatDuration(totalEstSeconds);
            String hwName = ExecutionContextPanel.getActiveHardwareMode().name();

            if (unexecutedCount == 1) {
                warningLabel.setText(String.format(I18n.getOrDefault("analytics.warning.unexecuted_single_est", "⚠️ 1 selected scenario uncalculated | %,d yrs | Est. Time: %s (%s)"), totalUnexecutedYears, durStr, hwName));
            } else {
                warningLabel.setText(String.format(I18n.getOrDefault("analytics.warning.unexecuted_plural_est", "⚠️ %d selected scenarios uncalculated | Total Horizon: %,d yrs | ⏱️ Total Est. Time: %s (%s)"), unexecutedCount, totalUnexecutedYears, durStr, hwName));
            }
            warningLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #b45309; -fx-padding: 6 10; -fx-background-color: rgba(254, 243, 199, 0.8); -fx-background-radius: 4;");
            
            if (!isBatchRunning.get()) {
                executeMissingBtn.setText(unexecutedCount == 1
                    ? String.format(I18n.getOrDefault("analytics.btn.execute_missing_single_fmt", "🚀 Execute Missing Scenario (%s)"), durStr)
                    : String.format(I18n.getOrDefault("analytics.btn.execute_missing_plural_fmt", "🚀 Execute %d Scenarios in Queue (%s)"), unexecutedCount, durStr));
                executeMissingBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
                executeMissingBtn.setDisable(false);
                cancelBatchBtn.setVisible(false);
                cancelBatchBtn.setManaged(false);
            }
        } else {
            warningLabel.setText(String.format(I18n.getOrDefault("analytics.warning.ready", "✅ All selected scenarios (%d) are ready for audit and comparison."), selected.size()));
            warningLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #15803d; -fx-padding: 6 10; -fx-background-color: rgba(220, 252, 231, 0.8); -fx-background-radius: 4;");
            long simulatedCount = selected.stream().filter(i -> !"HISTORICAL_GROUND_TRUTH".equals(i.getRunId())).count();
            if (!isBatchRunning.get()) {
                executeMissingBtn.setText(simulatedCount == 1
                    ? I18n.getOrDefault("analytics.btn.reexecute_single", "🔄 Re-execute Simulated Scenario")
                    : String.format(I18n.getOrDefault("analytics.btn.reexecute_plural", "🔄 Re-execute %d Simulated Scenarios"), simulatedCount));
                executeMissingBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
                executeMissingBtn.setDisable(false);
                cancelBatchBtn.setVisible(false);
                cancelBatchBtn.setManaged(false);
            }
        }
    }

    private void executeMissingScenarios() {
        if (isBatchRunning.get()) return;

        List<ScenarioSelectableItem> targetItems = scenarioList.stream()
            .filter(i -> i.isSelected() && !"HISTORICAL_GROUND_TRUTH".equals(i.getRunId()))
            .toList();

        if (targetItems.isEmpty()) return;

        // Auto-pause interactive simulation if running to prevent compute conflict
        if (isInteractiveSimulationRunningSupplier != null && isInteractiveSimulationRunningSupplier.getAsBoolean()) {
            if (onPauseInteractiveSimulationCallback != null) {
                logger.info("Auto-pausing interactive simulation to prioritize comparative batch execution");
                onPauseInteractiveSimulationCallback.run();
            }
        }

        isBatchRunning.set(true);
        isBatchCancelled.set(false);
        isBatchPaused.set(false);

        ExecutionContextPanel.HardwareMode activeHw = ExecutionContextPanel.getActiveHardwareMode();
        int availableCores = Runtime.getRuntime().availableProcessors();
        int threadCount = switch (activeHw) {
            case NATIVE_RUST, JAVA_VECTOR_SIMD -> Math.max(1, availableCores);
            case GPU_SHADERS -> 2;
            case CPU_JIT -> Math.max(1, availableCores);
            case GPU_OFF -> 1;
        };

        executeMissingBtn.setDisable(true);
        executeMissingBtn.setText(I18n.getOrDefault("analytics.btn.executing", "⏳ Processing Batch Queue..."));
        cancelBatchBtn.setVisible(true);
        cancelBatchBtn.setManaged(true);
        cancelBatchBtn.setDisable(false);
        cancelBatchBtn.setText(I18n.getOrDefault("analytics.btn.cancel_batch", "🛑 Cancel Batch"));

        batchProgressBar.setProgress(0.0);
        batchProgressBar.setVisible(true);
        batchProgressBar.setManaged(true);
        etaLabel.setText("⏱️ Calcul...");
        etaLabel.setVisible(true);
        etaLabel.setManaged(true);

        int queuePos = 1;
        for (ScenarioSelectableItem item : targetItems) {
            item.setBatchState(BatchState.QUEUED);
            item.setQueueIndex(queuePos++);
            item.setProgress(0.0);
        }
        scenarioTable.refresh();

        long startTimeMs = System.currentTimeMillis();

        batchWorkerThread = new Thread(() -> {
            logger.info("Starting parallel batch execution with {} threads for {} scenarios...", threadCount, targetItems.size());
            
            java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(Math.max(1, threadCount));
            java.util.concurrent.atomic.AtomicInteger finishedCount = new java.util.concurrent.atomic.AtomicInteger(0);
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>();

            for (ScenarioSelectableItem item : targetItems) {
                futures.add(pool.submit(() -> {
                    if (isBatchCancelled.get()) {
                        javafx.application.Platform.runLater(() -> {
                            item.setBatchState(BatchState.NOT_EXECUTED);
                        });
                        return;
                    }

                    javafx.application.Platform.runLater(() -> {
                        item.setBatchState(BatchState.RUNNING);
                        scenarioTable.refresh();
                    });

                    SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(
                        item.getScenario(),
                        (sc, prog, yr, endYr) -> {
                            javafx.application.Platform.runLater(() -> {
                                item.setProgress(prog);
                                scenarioTable.refresh();
                                updateLiveBatchProgressAndEta(targetItems, startTimeMs);
                            });
                        },
                        isBatchCancelled::get
                    );

                    if (record != null && !isBatchCancelled.get()) {
                        runRepository.saveRun(record);
                        finishedCount.incrementAndGet();
                        javafx.application.Platform.runLater(() -> {
                            item.setExecuted(true);
                            item.setRunId(record.getRunId());
                            item.setBatchState(BatchState.EXECUTED);
                            item.setProgress(1.0);
                            scenarioTable.refresh();
                            updateLiveBatchProgressAndEta(targetItems, startTimeMs);
                        });
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            if (!item.isExecuted()) {
                                item.setBatchState(BatchState.NOT_EXECUTED);
                            }
                            scenarioTable.refresh();
                        });
                    }
                }));
            }

            pool.shutdown();
            try {
                for (var f : futures) {
                    f.get();
                }
            } catch (Exception ex) {
                logger.warn("Batch thread pool interrupted: {}", ex.getMessage());
            }

            long totalElapsedSec = Math.max(1, (System.currentTimeMillis() - startTimeMs) / 1000);

            javafx.application.Platform.runLater(() -> {
                isBatchRunning.set(false);
                batchProgressBar.setVisible(false);
                batchProgressBar.setManaged(false);
                etaLabel.setText(String.format(I18n.getOrDefault("analytics.status.batch_completed_fmt", "✅ Terminé en %ds (%d scénarios)"), totalElapsedSec, finishedCount.get()));
                scenarioTable.refresh();
                checkExecutionStatus();
                runAnalysis();
                logger.info("Batch execution sequence completed in {}s for {} scenarios.", totalElapsedSec, finishedCount.get());
            });
        }, "ComparativeAnalyticsBatchParallelWorker");

        batchWorkerThread.setDaemon(true);
        batchWorkerThread.start();
    }

    private void updateLiveBatchProgressAndEta(List<ScenarioSelectableItem> items, long startTimeMs) {
        if (items == null || items.isEmpty()) return;
        double sumProgress = 0.0;
        for (ScenarioSelectableItem it : items) {
            sumProgress += it.getProgress();
        }
        double overallProgress = sumProgress / items.size();
        batchProgressBar.setProgress(overallProgress);

        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        if (overallProgress > 0.02 && elapsedMs > 400) {
            long totalEstMs = (long) (elapsedMs / overallProgress);
            long remainingMs = Math.max(0, totalEstMs - elapsedMs);
            String elapsedStr = ExecutionContextPanel.formatDuration(elapsedMs / 1000.0);
            String remainingStr = ExecutionContextPanel.formatDuration(remainingMs / 1000.0);
            etaLabel.setText(String.format("⏱️ %s | %s: %s (%.0f%%)", elapsedStr, I18n.getOrDefault("analytics.label.remaining", "Restant"), remainingStr, overallProgress * 100.0));
        }
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
                    reportPreviewPane.setMarkdown(I18n.getOrDefault("analytics.error.sim_data_not_found", "⚠️ Simulated execution data not found for: ") + targetItem.getName());
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
                reportPreviewPane.setMarkdown(I18n.getOrDefault("analytics.error.no_telemetry", "⚠️ Unable to access base scenario telemetry data."));
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
                divergenceLabel.setText(String.format(I18n.getOrDefault("analytics.status.first_divergence", "⚠️ First Major Break Detected (T_divergence): YEAR %d"), firstDivergence));
                divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc2626; -fx-font-size: 13px;");
            } else {
                divergenceLabel.setText(I18n.getOrDefault("analytics.status.parallel", "✅ Parallel Trajectories (No major divergence > 5%)"));
                divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a; -fx-font-size: 13px;");
            }

            explanationLabel.setText(explanationSummary.toString());
            reportPreviewPane.setMarkdown(multiReport.toString());
        }

        updateChartAndAnalysis();
        update2DSpatialComparison();
    }

    private void generateHistoricalAuditReport(String targetName, SimulationRunRecord targetRun) {
        Map<Integer, SimulationRunRecord.MetricSnapshot> timeSeries = targetRun.getTimeSeriesData();
        if (timeSeries == null || timeSeries.isEmpty()) {
            reportPreviewPane.setMarkdown(I18n.getOrDefault("analytics.error.no_telemetry_for", "⚠️ No time telemetry data recorded for: ") + targetName);
            return;
        }

        Map<String, Double> mapes = new LinkedHashMap<>();
        Map<String, String> benchmarkKeys = new LinkedHashMap<>();
        benchmarkKeys.put("👥 Population Globale (worldPopulation)", "worldPopulation");
        benchmarkKeys.put("🦣 Abondance Mégafaune (megafaunaIndex)", "megafaunaIndex");
        benchmarkKeys.put("☀️ Insolation Milankovitch 65°N (milankovitchInsolation)", "milankovitchInsolation");
        benchmarkKeys.put("🛡️ Respect Confinement Biogéographique (zeroContainmentScore)", "zeroContainmentScore");
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
            divergenceLabel.setText(String.format(I18n.getOrDefault("analytics.status.divergence_vs_reality", "🏛️ Break Detected / Drift vs Reality (T_divergence): YEAR %d AD"), divergenceYear));
            divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc2626; -fx-font-size: 13px;");
        } else {
            divergenceLabel.setText(I18n.getOrDefault("analytics.status.historical_align", "✅ Remarkable Alignment with Historical Reality (Mean MAPE < 15%)"));
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
        sb.append("**Référence Ground Truth** : Réalité Historique Cliodynamique (-100 000 ➔ 2026 CE)\n\n");
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
        sb.append(CalibrationDiagnosticRules.generateTuningSuggestions(mapes, rSquared, divergenceYear));
        sb.append("--- *Audit automatique généré par Ether Cliodynamic Benchmark Auditor v5.0* ---");

        reportPreviewPane.setMarkdown(sb.toString());
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
        if (isUpdatingTexts) return;
        chart.getData().clear();

        List<ScenarioSelectableItem> selectedExecuted = scenarioList.stream()
            .filter(i -> i.isSelected() && i.isExecuted())
            .toList();

        String metric = metricSelectorCombo.getValue();
        if (metric == null || metric.startsWith("──") || selectedExecuted.isEmpty()) return;

        // Determine timeline range across target scenarios for interpolated ground truth alignment
        int minYear = 0;
        int maxYear = 2026;
        boolean hasTargetScenarios = false;
        for (ScenarioSelectableItem other : selectedExecuted) {
            if (!"HISTORICAL_GROUND_TRUTH".equals(other.getRunId()) && other.getScenario() != null) {
                if (!hasTargetScenarios) {
                    minYear = (int) other.getScenario().getStartDateYear();
                    maxYear = (int) other.getScenario().getEndDateYear();
                    hasTargetScenarios = true;
                } else {
                    minYear = (int) Math.min(minYear, other.getScenario().getStartDateYear());
                    maxYear = (int) Math.max(maxYear, other.getScenario().getEndDateYear());
                }
            }
        }
        if (!hasTargetScenarios) {
            minYear = -100000;
            maxYear = 2026;
        }

        HistoricalValidationKernel.InterpolationMethod interpMethod = (interpolationCombo != null && interpolationCombo.getValue() != null)
            ? interpolationCombo.getValue()
            : HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC;

        for (ScenarioSelectableItem item : selectedExecuted) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(item.getName());

            if ("HISTORICAL_GROUND_TRUTH".equals(item.getRunId())) {
                String benchKey = mapMetricToBenchmarkKey(metric);
                int step = Math.max(1, (maxYear - minYear) / 60);
                for (int yr = minYear; yr <= maxYear; yr += step) {
                    double val = HistoricalValidationKernel.getInterpolatedBenchmarkValue(benchKey, yr, interpMethod);
                    series.getData().add(new XYChart.Data<>(yr, val));
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
        if (isUpdatingTexts) return;
        List<ScenarioSelectableItem> selectedItems = scenarioList.stream()
            .filter(ScenarioSelectableItem::isSelected)
            .toList();

        if (selectedItems.isEmpty()) {
            if (mapLabelA != null) mapLabelA.setText(I18n.getOrDefault("analytics.label.scenario_a_none", "Scenario A (None selected)"));
            if (mapLabelB != null) mapLabelB.setText(I18n.getOrDefault("analytics.label.scenario_b_none", "Scenario B (None selected)"));
            if (mapLabelDiff != null) mapLabelDiff.setText(I18n.getOrDefault("analytics.label.scenario_diff", "Discrepancy Heatmap Δ(A - B)"));
            if (mapImageViewA != null) mapImageViewA.setImage(null);
            if (mapImageViewB != null) mapImageViewB.setImage(null);
            if (mapImageViewDiff != null) mapImageViewDiff.setImage(null);
            if (spatialMetricsReportArea != null) spatialMetricsReportArea.setText(I18n.getOrDefault("analytics.prompt.select_2d", "⚠️ Please select at least 2 scenarios in table to launch 2D map comparison."));
            return;
        }

        Scenario scA = selectedItems.get(0).getScenario();
        Scenario scB = (selectedItems.size() > 1) ? selectedItems.get(1).getScenario() : scA;

        if (scA == null) return;
        if (scB == null) scB = scA;

        if (mapLabelA != null) mapLabelA.setText(I18n.getOrDefault("analytics.label.scenario_a_prefix", "Scenario A: ") + scA.getName());
        if (mapLabelB != null) mapLabelB.setText(I18n.getOrDefault("analytics.label.scenario_b_prefix", "Scenario B: ") + scB.getName());
        if (mapLabelDiff != null) mapLabelDiff.setText(I18n.getOrDefault("analytics.label.scenario_diff", "Discrepancy Heatmap Δ(A - B)"));

        int startA = (int) scA.getStartDateYear();
        int endA = (int) scA.getEndDateYear();
        int startB = (int) scB.getStartDateYear();
        int endB = (int) scB.getEndDateYear();

        int minYear = Math.max(-10000, Math.min(startA, startB));
        int maxYear = Math.min(2026, Math.max(endA, endB));
        if (maxYear <= minYear) maxYear = minYear + 100;

        if (dateSlider != null) {
            if (minYear != (int) dateSlider.getMin() || maxYear != (int) dateSlider.getMax()) {
                dateSlider.setMin(minYear);
                dateSlider.setMax(maxYear);
                double tickUnit = Math.max(10.0, (maxYear - minYear) / 5.0);
                dateSlider.setMajorTickUnit(tickUnit);
                if (dateSlider.getValue() < minYear || dateSlider.getValue() > maxYear) {
                    dateSlider.setValue(minYear);
                }
            }
        }

        String channel = spatialChannelCombo != null ? spatialChannelCombo.getValue() : I18n.getOrDefault("analytics.spatial.density", "👥 Demographic Density");
        if (channel == null) channel = I18n.getOrDefault("analytics.spatial.density", "👥 Demographic Density");

        int targetYear = dateSlider != null ? (int) dateSlider.getValue() : 0;

        java.awt.image.BufferedImage bufA = null;
        java.awt.image.BufferedImage bufB = null;
        java.awt.image.BufferedImage bufDiff = null;

        try {
            // Check if simulated spatial snapshots are available from execution records
            SimulationRunRecord runA = (selectedItems.size() > 0 && selectedItems.get(0).getRunId() != null)
                ? runRepository.getRun(selectedItems.get(0).getRunId()) : null;
            SimulationRunRecord runB = (selectedItems.size() > 1 && selectedItems.get(1).getRunId() != null)
                ? runRepository.getRun(selectedItems.get(1).getRunId()) : null;

            List<org.ether.society.database.H3Cell> snapshotCellsA = runA != null ? runA.getSpatialSnapshotAt(targetYear) : null;
            List<org.ether.society.database.H3Cell> snapshotCellsB = runB != null ? runB.getSpatialSnapshotAt(targetYear) : null;

            // 1. Channel A Raster Extraction
            File diskRasterA = getDiskRasterForChannel(targetYear, channel);
            if ("HISTORICAL_GROUND_TRUTH".equals(selectedItems.get(0).getRunId()) && diskRasterA != null && diskRasterA.exists()) {
                bufA = ImageIO.read(diskRasterA);
            } else if (snapshotCellsA != null && !snapshotCellsA.isEmpty()) {
                bufA = rasterizeCellsToImage(snapshotCellsA, channel, 512, 256);
            } else if (diskRasterA != null && diskRasterA.exists()) {
                bufA = ImageIO.read(diskRasterA);
            } else if (channel.contains("Densité") || channel.contains("Demographic") || channel.contains("Density")) {
                bufA = HistoricalMapGenerator.rasterizeDensityMapForYear(scA.getPopulationDensityType(), scA, targetYear);
            } else {
                HistoricalMapGenerator.populateScenarioHistoricalMaps(scA);
                bufA = base64ToBufferedImage(extractChannelBase64(scA, channel));
            }

            // 2. Channel B Raster Extraction
            File diskRasterB = getDiskRasterForChannel(targetYear, channel);
            if (selectedItems.size() > 1 && "HISTORICAL_GROUND_TRUTH".equals(selectedItems.get(1).getRunId()) && diskRasterB != null && diskRasterB.exists()) {
                bufB = ImageIO.read(diskRasterB);
            } else if (snapshotCellsB != null && !snapshotCellsB.isEmpty()) {
                bufB = rasterizeCellsToImage(snapshotCellsB, channel, 512, 256);
            } else if (channel.contains("Densité") || channel.contains("Demographic") || channel.contains("Density")) {
                bufB = HistoricalMapGenerator.rasterizeDensityMapForYear(scB.getPopulationDensityType(), scB, targetYear);
            } else {
                HistoricalMapGenerator.populateScenarioHistoricalMaps(scB);
                bufB = base64ToBufferedImage(extractChannelBase64(scB, channel));
            }

            if (bufA != null && bufB != null) {
                bufDiff = MapComparisonMetrics.generateDiscrepancyHeatmap(bufA, bufB);
            }
        } catch (Exception ex) {
            logger.warn("Could not generate 2D comparison maps for target year {}: {}", targetYear, ex.getMessage());
        }

        Image imgFxA = bufferedImageToFxImage(bufA);
        Image imgFxB = bufferedImageToFxImage(bufB);
        Image imgFxDiff = bufferedImageToFxImage(bufDiff);

        if (mapImageViewA != null) mapImageViewA.setImage(imgFxA);
        if (mapImageViewB != null) mapImageViewB.setImage(imgFxB);
        if (mapImageViewDiff != null) mapImageViewDiff.setImage(imgFxDiff);

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
            spatialMetricsReportArea.setText(I18n.getOrDefault("analytics.status.loading_tensors", "ℹ️ Loading cartographic tensors for selected scenarios..."));
        }
    }

    private java.awt.image.BufferedImage rasterizeCellsToImage(List<org.ether.society.database.H3Cell> cells, String channel, int width, int height) {
        if (cells == null || cells.isEmpty()) return null;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // Background ocean
        g2.setColor(new java.awt.Color(15, 23, 42));
        g2.fillRect(0, 0, width, height);

        for (org.ether.society.database.H3Cell c : cells) {
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            int x = (int) Math.round(((lng + 180.0) / 360.0) * width);
            int y = (int) Math.round(((90.0 - lat) / 180.0) * height);
            int r = Math.max(3, width / 200);

            java.awt.Color col;
            if (channel.contains("Densité") || channel.contains("Demographic") || channel.contains("Density")) {
                long pop = c.getPopulation() != null ? c.getPopulation() : 0;
                if (pop > 0) {
                    double norm = Math.min(1.0, Math.log10(pop + 1.0) / 6.0);
                    col = getDensityColor(norm);
                } else {
                    col = new java.awt.Color(30, 41, 59);
                }
            } else if (channel.contains("Technologie") || channel.contains("Technology") || channel.contains("Outillage")) {
                double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
                double norm = Math.min(1.0, tech / 100.0);
                col = getTechColor(norm);
            } else if (channel.contains("Température") || channel.contains("Temperature") || channel.contains("Climat")) {
                double temp = c.getTemperature() != null ? c.getTemperature() : 15.0;
                double norm = Math.max(0.0, Math.min(1.0, (temp + 20.0) / 60.0));
                col = getTemperatureColor(norm);
            } else if (channel.contains("Aquifère") || channel.contains("Aquifer") || channel.contains("Eau")) {
                double aqua = c.getFreshwaterAquifer() != null ? c.getFreshwaterAquifer() : 50.0;
                double norm = Math.max(0.0, Math.min(1.0, aqua / 100.0));
                col = getAquiferColor(norm);
            } else if (channel.contains("Biomasse") || channel.contains("Biomass") || channel.contains("Agricole") || channel.contains("Agriculture")) {
                double bio = c.getBiomassAgriculture() != null ? c.getBiomassAgriculture() : 0.0;
                double norm = Math.max(0.0, Math.min(1.0, bio / 1000.0));
                col = getAgricultureColor(norm);
            } else if (channel.contains("Souveraineté") || channel.contains("Sovereignty")) {
                long polity = (c.getLanguageGroup() != null) ? (long) Math.abs(c.getLanguageGroup().hashCode()) : (c.getId() != null ? (long) Math.abs(c.getId().hashCode()) : 0L);
                col = getPolityColor(polity);
            } else {
                long lang = (c.getLanguageGroup() != null) ? (long) Math.abs(c.getLanguageGroup().hashCode()) : 0L;
                col = getPolityColor(lang);
            }

            g2.setColor(col);
            g2.fillOval(x - r, y - r, r * 2, r * 2);
        }
        g2.dispose();
        return img;
    }

    private java.awt.Color getTemperatureColor(double norm) {
        int r = (int) (norm * 255);
        int g = (int) ((1.0 - Math.abs(norm - 0.5) * 2.0) * 200);
        int b = (int) ((1.0 - norm) * 255);
        return new java.awt.Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
    }

    private java.awt.Color getAquiferColor(double norm) {
        int r = (int) (20 + norm * 30);
        int g = (int) (100 + norm * 140);
        int b = (int) (200 + norm * 55);
        return new java.awt.Color(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    private java.awt.Color getAgricultureColor(double norm) {
        int r = (int) (120 - norm * 80);
        int g = (int) (140 + norm * 100);
        int b = (int) (30 + norm * 20);
        return new java.awt.Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
    }

    private java.awt.Color getDensityColor(double norm) {
        if (norm < 0.33) {
            return new java.awt.Color(16, 185, 129);
        } else if (norm < 0.66) {
            return new java.awt.Color(245, 158, 11);
        } else {
            return new java.awt.Color(239, 68, 68);
        }
    }

    private java.awt.Color getTechColor(double norm) {
        int r = (int) (56 + norm * (245 - 56));
        int g = (int) (189 - norm * 80);
        int b = (int) (248 - norm * 150);
        return new java.awt.Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
    }

    private java.awt.Color getPolityColor(long id) {
        if (id <= 0) return new java.awt.Color(100, 116, 139);
        return java.awt.Color.getHSBColor((float) ((id * 0.618033988749895) % 1.0), 0.75f, 0.85f);
    }

    private File getDiskRasterForChannel(int year, String channel) {
        String baseMapDir = "data/maps/ether/earth/" + year + "/";
        String channelKey = "density";
        if (channel != null) {
            if (channel.contains("Technologie") || channel.contains("Technology")) channelKey = "technology";
            else if (channel.contains("Température") || channel.contains("Temperature") || channel.contains("Climat")) channelKey = "temperature";
            else if (channel.contains("Aquifère") || channel.contains("Aquifer") || channel.contains("Eau")) channelKey = "aquifers";
            else if (channel.contains("Biomasse") || channel.contains("Agriculture")) channelKey = "biomes";
            else if (channel.contains("Souveraineté") || channel.contains("Sovereignty")) channelKey = "sovereignty";
            else if (channel.contains("Isoglosses") || channel.contains("Linguistique") || channel.contains("Linguistic")) channelKey = "isogloss";
            else if (channel.contains("Parenté") || channel.contains("Kinship")) channelKey = "kinship";
            else if (channel.contains("Rituels") || channel.contains("Rituals")) channelKey = "rituals";
            else if (channel.contains("Commerce") || channel.contains("Trade")) channelKey = "tradenetwork";
            else if (channel.contains("Institution") || channel.contains("Institutional")) channelKey = "institutional";
            else if (channel.contains("Écologique") || channel.contains("Ecological")) channelKey = "ecological";
            else if (channel.contains("Pathogène") || channel.contains("Pathogen")) channelKey = "pathogen";
        }
        File rasterFile = new File(baseMapDir, "earth_" + year + "_" + channelKey + ".png");
        if (rasterFile.exists()) return rasterFile;
        // Check nearest available epoch
        File earthDir = new File("data/maps/ether/earth/");
        if (earthDir.exists() && earthDir.isDirectory()) {
            File[] dirs = earthDir.listFiles(File::isDirectory);
            if (dirs != null) {
                int closestYear = -1;
                int minDiff = Integer.MAX_VALUE;
                for (File d : dirs) {
                    try {
                        int y = Integer.parseInt(d.getName());
                        int diff = Math.abs(y - year);
                        if (diff < minDiff && diff <= 30) {
                            minDiff = diff;
                            closestYear = y;
                        }
                    } catch (NumberFormatException ignored) {}
                }
                if (closestYear != -1) {
                    File candidate = new File("data/maps/ether/earth/" + closestYear + "/earth_" + closestYear + "_" + channelKey + ".png");
                    if (candidate.exists()) return candidate;
                }
            }
        }
        return null;
    }

    private String extractChannelBase64(Scenario sc, String channel) {
        if (sc == null || channel == null) return null;
        if (channel.contains("Souveraineté") || channel.contains("Sovereignty")) return sc.getCustomTensorMapBase64(3);
        if (channel.contains("Isoglosses") || channel.contains("Linguistique") || channel.contains("Linguistic") || channel.contains("Langues")) return sc.getCustomTensorMapBase64(0);
        if (channel.contains("Parenté") || channel.contains("Kinship")) return sc.getCustomTensorMapBase64(1);
        if (channel.contains("Rituels") || channel.contains("Rituals") || channel.contains("Croyances")) return sc.getCustomTensorMapBase64(2);
        if (channel.contains("Technologie") || channel.contains("Technology") || channel.contains("Artefacts") || channel.contains("Outillage")) return sc.getCustomTensorMapBase64(4);
        if (channel.contains("Commerce") || channel.contains("Trade") || channel.contains("Corridors")) return sc.getCustomTensorMapBase64(5);
        if (channel.contains("Institution") || channel.contains("Institutional") || channel.contains("Seshat")) return sc.getCustomTensorMapBase64(6);
        if (channel.contains("Écologique") || channel.contains("Ecological") || channel.contains("Empreinte")) return sc.getCustomTensorMapBase64(7);
        if (channel.contains("Pathogène") || channel.contains("Pathogen") || channel.contains("Immunité") || channel.contains("Santé")) return sc.getCustomTensorMapBase64(8);
        return sc.getCustomDensityBase64();
    }

    private Image bufferedImageToFxImage(java.awt.image.BufferedImage buf) {
        if (buf == null) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buf, "PNG", baos);
            return new Image(new ByteArrayInputStream(baos.toByteArray()));
        } catch (Exception e) {
            return null;
        }
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
            if (playTimelineBtn != null) playTimelineBtn.setText(I18n.getOrDefault("analytics.btn.play_timeline", "▶️ Lecture Temporelle"));
        } else {
            isPlayingAnimation = true;
            if (playTimelineBtn != null) playTimelineBtn.setText(I18n.getOrDefault("analytics.btn.pause_timeline", "⏸️ Pause"));
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
        MetricDescriptor desc = MetricRegistry.getInstance().getDescriptorByName(metric);
        if (desc != null) {
            double val = snap.getValue(desc.getId());
            if (val != 0.0) return val;
        }
        if (metric.contains("Mégafaune") || metric.contains("megafauna")) {
            return org.ether.society.procedural.ProceduralPopulationEngine.calculateMegafaunaAbundanceIndex(year, snap.getPopulation() / 1e6, 0.2);
        }
        if (metric.contains("Milankovitch") || metric.contains("Insolation")) {
            return org.ether.society.procedural.ProceduralPopulationEngine.calculateMilankovitchSummerInsolation65N(year);
        }
        if (metric.contains("Confinement") || metric.contains("zeroContainment")) {
            return 100.0;
        }
        return snap.getValue(metric);
    }

    private void exportMarkdownReport() {
        String currentReport = (reportPreviewPane != null) ? reportPreviewPane.getMarkdown() : "";

        if (currentReport == null || currentReport.isBlank() || currentReport.startsWith("## Please select")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(I18n.getOrDefault("analytics.title.export_notice", "Export Report Notice"));
            alert.setHeaderText(I18n.getOrDefault("analytics.header.export_notice", "Aucun rapport généré à exporter"));
            alert.setContentText(I18n.getOrDefault("analytics.content.export_notice", "Veuillez d'abord sélectionner au moins deux scénarios dans le tableau (ou la Réalité Historique + un scénario simulé) et cliquer sur 'Recalculer les écarts' avant d'exporter."));
            alert.showAndWait();
            return;
        }

        String initialName = currentReport.contains("RAPPORT D'AUDIT ET CALIBRATION")
            ? "Rapport_Audit_Historique_Cliodynamique.md"
            : "Rapport_Analyse_Comparative.md";

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.getOrDefault("analytics.title.export_report_dialog", "Export Comparative Analysis Report"));
        fileChooser.setInitialFileName(initialName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Markdown (*.md)", "*.md"));

        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(currentReport);
                logger.info("Exported comparative analysis report to {}", file.getAbsolutePath());

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle(I18n.getOrDefault("analytics.title.export_success", "Export Successful"));
                successAlert.setHeaderText(null);
                successAlert.setContentText(I18n.getOrDefault("analytics.content.export_success", "Le rapport Markdown a été exporté avec succès vers :\n") + file.getAbsolutePath());
                successAlert.showAndWait();
            } catch (IOException ex) {
                logger.error("Error writing markdown report to file", ex);
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur d'export");
                errorAlert.setHeaderText("Échec de l'écriture du fichier");
                errorAlert.setContentText(ex.getMessage());
                errorAlert.showAndWait();
            }
        }
    }

    private void exportCsvData() {
        String csv = ComparativeReportGenerator.generateCsvExport(runRepository.getAllRuns());

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.getOrDefault("analytics.title.export_csv_dialog", "Export Telemetry Data (CSV)"));
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
        isUpdatingTexts = true;
        try {
            if (headerLabel != null) headerLabel.setText(I18n.getOrDefault("analytics.header", "📊 COMPARATIVE ANALYTICS & SCENARIO BATTLE (DEEP ANALYTICS)"));
            if (selectCol != null) selectCol.setText(I18n.getOrDefault("analytics.col.compare", "Comparer"));
            if (nameCol != null) nameCol.setText(I18n.getOrDefault("analytics.col.name", "Nom du Scénario"));
            if (yearsCol != null) yearsCol.setText(I18n.getOrDefault("analytics.col.years", "Plage Chronologique"));
            if (progressCol != null) progressCol.setText(I18n.getOrDefault("analytics.col.progress", "Progression"));
            if (estDurationCol != null) estDurationCol.setText(I18n.getOrDefault("analytics.col.est_duration", "Temps Restant"));
            if (statusCol != null) statusCol.setText(I18n.getOrDefault("analytics.col.status", "Statut Exécution BD"));
            if (metricLabel != null) metricLabel.setText(I18n.getOrDefault("analytics.metric_label", "Visualized Metric:"));
            if (interpolationLabel != null) interpolationLabel.setText(I18n.getOrDefault("analytics.interp_label", "Interpolation :"));
            if (channelLabel != null) channelLabel.setText(I18n.getOrDefault("analytics.channel_label", "Canal Tensoriel :"));
            updateExecutionContextBadge();
            if (mapLabelDiff != null) mapLabelDiff.setText(I18n.getOrDefault("analytics.label.scenario_diff", "Discrepancy Heatmap Δ(A - B)"));
            if (playTimelineBtn != null) {
                playTimelineBtn.setText(I18n.getOrDefault("analytics.btn.play_timeline", "▶️ Lecture Temporelle"));
                playTimelineBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.play_timeline", "Animate chronological evolution of 2D cartographic tensors over historical centuries.")));
            }
            if (analyzeBtn != null) {
                analyzeBtn.setText(I18n.getOrDefault("analytics.btn.analyze", "⚡ Recalculate Divergences"));
                analyzeBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.analyze", "Calculate divergences across trajectories (T_divergence > 5%) and compute MAPE/R² vs Ground Truth.")));
            }
            if (exportMdBtn != null) {
                exportMdBtn.setText(I18n.getOrDefault("analytics.btn.export_md", "📝 Export Report (.md)"));
                exportMdBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.export_md", "Export complete markdown synthesis report including all diagnostics and equations.")));
            }
            if (exportCsvBtn != null) {
                exportCsvBtn.setText(I18n.getOrDefault("analytics.btn.export_csv", "📥 Export Data (.csv)"));
                exportCsvBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.export_csv", "Export raw multi-scenario chronological telemetry series into CSV format.")));
            }
            if (executeMissingBtn != null) {
                executeMissingBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.execute", "Run physical headless simulation across queued scenarios using the H3SimulationEngine.")));
            }
            if (cancelBatchBtn != null) {
                cancelBatchBtn.setText(I18n.getOrDefault("analytics.btn.cancel_batch", "🛑 Cancel Batch"));
                cancelBatchBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.cancel", "Cancel pending background simulation batch.")));
            }
            if (xAxis != null) xAxis.setLabel(I18n.getOrDefault("analytics.axis.x", "Simulation Years (Ticks)"));
            if (yAxis != null) yAxis.setLabel(I18n.getOrDefault("analytics.axis.y", "Valeur de l'Indicateur"));
            if (chart != null) chart.setTitle(I18n.getOrDefault("analytics.chart.title", "Multi-Scenario Chronological Overlay (💡 CTRL + Scroll to Zoom, CTRL + Drag to Pan)"));
            if (diagHeader != null) diagHeader.setText(I18n.getOrDefault("analytics.diag_header", "🔍 DIVERGENCE ANALYSIS & GAP ANATOMY"));
            if (synthHeader != null) synthHeader.setText(I18n.getOrDefault("analytics.synth_header", "📄 Auto-Generated Comparative Summary:"));
            if (searchField != null) searchField.setPromptText(I18n.getOrDefault("analytics.search_prompt", "🔍 Filter scenarios by name, status, or year range..."));
            if (timeSeriesTab != null) timeSeriesTab.setText(I18n.getOrDefault("analytics.tab.timeseries", "📈 Time Series (1D)"));
            if (spatialCartoTab != null) spatialCartoTab.setText(I18n.getOrDefault("analytics.tab.spatial", "🗺️ Cartographie & Tenseurs (2D)"));

            if (divergenceLabel != null && (divergenceLabel.getText() == null || divergenceLabel.getText().isBlank() || divergenceLabel.getText().startsWith("Point de rupture") || divergenceLabel.getText().startsWith("Point of divergence") || divergenceLabel.getText().startsWith("Punto de ruptura") || divergenceLabel.getText().startsWith("Bruchpunkt") || divergenceLabel.getText().startsWith("临界断点"))) {
                divergenceLabel.setText(I18n.getOrDefault("analytics.divergence.select_hint", "Break point: Select at least 2 scenarios"));
            }
            if (explanationLabel != null && (explanationLabel.getText() == null || explanationLabel.getText().isBlank() || explanationLabel.getText().startsWith("Cochez les scénarios") || explanationLabel.getText().startsWith("Check scenarios") || explanationLabel.getText().startsWith("Marque los escenarios") || explanationLabel.getText().startsWith("Wählen Sie Szenarien") || explanationLabel.getText().startsWith("勾选上方列表"))) {
                explanationLabel.setText(I18n.getOrDefault("analytics.divergence.check_hint", "Check scenarios in the list above to start comparison."));
            }

            if (spatialChannelCombo != null) {
                int selectedIdx = spatialChannelCombo.getSelectionModel().getSelectedIndex();
                if (selectedIdx < 0) selectedIdx = 0;
                spatialChannelCombo.getItems().clear();
                spatialChannelCombo.getItems().addAll(
                    I18n.getOrDefault("analytics.spatial.density", "👥 Demographic Density"),
                    I18n.getOrDefault("analytics.spatial.technology", "🔬 Niveau Technologique (Outillage)"),
                    I18n.getOrDefault("analytics.spatial.temperature", "🌡️ Température & Climat"),
                    I18n.getOrDefault("analytics.spatial.aquifers", "💧 Aquifères & Eau Douce"),
                    I18n.getOrDefault("analytics.spatial.agriculture", "🌾 Biomasse & Terres Agricoles"),
                    I18n.getOrDefault("analytics.spatial.sovereignty", "👑 Political Sovereignty"),
                    I18n.getOrDefault("analytics.spatial.linguistic", "🗣️ Isoglosses Linguistiques (Langues)"),
                    I18n.getOrDefault("analytics.spatial.kinship", "🧬 Kinship Structure (Parenté)"),
                    I18n.getOrDefault("analytics.spatial.rituals", "🔮 Sacred Rituals & Beliefs (Rituels)"),
                    I18n.getOrDefault("analytics.spatial.trade", "🐫 Trade Corridors & Exchange (Commerce)"),
                    I18n.getOrDefault("analytics.spatial.institutional", "⚖️ Institutional Complexity (Seshat)"),
                    I18n.getOrDefault("analytics.spatial.ecological", "⚠️ Ecological Footprint (Empreinte)"),
                    I18n.getOrDefault("analytics.spatial.pathogen", "🧬 Pathogen & Zoonotic Immunity (Santé)")
                );
                spatialChannelCombo.getSelectionModel().select(selectedIdx);
            }

            if (interpolationCombo != null) {
                HistoricalValidationKernel.InterpolationMethod currentInterp = interpolationCombo.getValue();
                interpolationCombo.setConverter(new javafx.util.StringConverter<HistoricalValidationKernel.InterpolationMethod>() {
                    @Override
                    public String toString(HistoricalValidationKernel.InterpolationMethod object) {
                        if (object == null) return "";
                        return I18n.getOrDefault("interpolation." + object.name().toLowerCase(), object.name());
                    }
                    @Override
                    public HistoricalValidationKernel.InterpolationMethod fromString(String string) {
                        return null;
                    }
                });
                if (currentInterp != null) interpolationCombo.setValue(currentInterp);
            }

            if (metricSelectorCombo != null) {
                String selected = metricSelectorCombo.getValue();
                metricSelectorCombo.getItems().clear();

                Map<MetricDescriptor.Category, List<MetricDescriptor>> grouped = new TreeMap<>();
                for (MetricDescriptor d : MetricRegistry.getInstance().getAllMetrics()) {
                    grouped.computeIfAbsent(d.getCategory(), k -> new ArrayList<>()).add(d);
                }

                for (var entry : grouped.entrySet()) {
                    String categoryHeader = "── " + entry.getKey().getDisplayName() + " ──";
                    metricSelectorCombo.getItems().add(categoryHeader);
                    List<MetricDescriptor> metrics = entry.getValue();
                    metrics.sort(Comparator.comparing(MetricDescriptor::getDisplayName, String.CASE_INSENSITIVE_ORDER));
                    for (MetricDescriptor d : metrics) {
                        metricSelectorCombo.getItems().add(d.getDisplayName());
                    }
                }

                metricSelectorCombo.setCellFactory(p -> new ListCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setDisable(false);
                        } else if (item.startsWith("──")) {
                            setText(item);
                            setDisable(true);
                            setStyle("-fx-font-weight: bold; -fx-opacity: 0.7; -fx-padding: 4 8; -fx-text-fill: #38bdf8;");
                        } else {
                            setText(item);
                            setDisable(false);
                            setStyle("-fx-font-weight: normal; -fx-padding: 2 12;");
                        }
                    }
                });

                if (selected != null && metricSelectorCombo.getItems().contains(selected) && !selected.startsWith("──")) {
                    metricSelectorCombo.setValue(selected);
                } else {
                    for (String item : metricSelectorCombo.getItems()) {
                        if (!item.startsWith("──")) {
                            metricSelectorCombo.setValue(item);
                            break;
                        }
                    }
                }
            }

            if (scenarioTable != null) scenarioTable.refresh();
            checkExecutionStatus();
        } finally {
            isUpdatingTexts = false;
        }
    }
}

