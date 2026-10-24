/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.analytics.*;
import org.ether.society.analytics.GroundTruthAuditGenerator.AuditResult;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.persistence.SaveMetadata;
import org.ether.society.persistence.ScenarioRepository;
import org.ether.society.persistence.SimulationSaveManager;
import org.ether.society.ui.analytics.*;
import org.ether.society.ui.export.ComparativeAnalyticsExporter;
import org.ether.society.ui.util.MarkdownViewerPane;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 6th Tab UI Panel: Comparative Analytics & Sensitivity Benchmarks.
 * 
 * <p>Supports multi-scenario selection, execution status validation, automated headless
 * batch execution of missing scenarios, 1D multi-curve time-series visualization,
 * 2D spatial cartographic tensor comparison, and empirical ground truth audit reports.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ComparativeAnalyticsPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ComparativeAnalyticsPanel.class);

    private final SimulationRunRepository runRepository;
    private final ScenarioRepository scenarioRepository;
    private final SimulationSaveManager saveManager = new SimulationSaveManager();
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

    private TableView<ScenarioSelectableItem> scenarioTable;
    private ObservableList<ScenarioSelectableItem> scenarioList;
    private FilteredList<ScenarioSelectableItem> filteredScenarioList;

    private Label headerLabel;
    private Label metricLabel;
    private ComboBox<String> metricSelectorCombo;
    private Label interpolationLabel;
    private ComboBox<HistoricalValidationKernel.InterpolationMethod> interpolationCombo;
    private Button copyReportBtn;
    private Button exportMdBtn;
    private Button exportCsvBtn;
    private Button export1DCsvBtn;
    private Label tableTitle;
    private TextField searchField;

    private TableColumn<ScenarioSelectableItem, Boolean> selectCol;
    private boolean isUpdatingTexts = false;
    private TableColumn<ScenarioSelectableItem, String> nameCol;
    private TableColumn<ScenarioSelectableItem, String> planetCol;
    private TableColumn<ScenarioSelectableItem, String> clippingCol;
    private TableColumn<ScenarioSelectableItem, Long> yearsCol;
    private TableColumn<ScenarioSelectableItem, Double> progressCol;
    private TableColumn<ScenarioSelectableItem, Double> estDurationCol;
    private TableColumn<ScenarioSelectableItem, String> statusCol;

    private HBox timeSeriesWarningBox;
    private Label timeSeriesWarningLabel;

    private TabPane mainTabPane;
    private Tab scenariosTab;
    private Tab timeSeriesTab;
    private Tab spatialCartoTab;
    private Tab reportTab;

    private NumberAxis xAxis;
    private NumberAxis yAxis;
    private LineChart<Number, Number> chart;
    private Label warningLabel;
    private Label executionContextBadge;
    private ProgressBar batchProgressBar;
    private Label etaLabel;
    private Button executeMissingBtn;
    private Button cancelBatchBtn;

    private SpatialTensorComparisonPane spatialTensorComparisonPane;
    private Label divergenceLabel;
    private Label explanationLabel;
    private MarkdownViewerPane reportPreviewPane;
    private Label chartHintLabel;
    private javafx.animation.PauseTransition debouncedAnalyticsTransition;

    private void scheduleDebouncedAnalyticsUpdate() {
        if (debouncedAnalyticsTransition == null) {
            debouncedAnalyticsTransition = new javafx.animation.PauseTransition(Duration.millis(80));
            debouncedAnalyticsTransition.setOnFinished(e -> {
                updateChartAndAnalysis();
                updateReportIfActive();
                update2DSpatialComparison();
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
        scenarioTable.setEditable(true);

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
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    ScenarioSelectableItem item = row.getItem();
                    if (item != null) {
                        boolean isCheckBoxClick = false;
                        if (event.getTarget() instanceof javafx.scene.Node target) {
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
                    if (item != null) item.setSelected(checkBox.isSelected());
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

        planetCol = new TableColumn<>();
        planetCol.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().getPlanetDisplayName()));
        planetCol.setPrefWidth(125);

        clippingCol = new TableColumn<>();
        clippingCol.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().getClippingDisplay()));
        clippingCol.setPrefWidth(210);

        yearsCol = new TableColumn<>();
        yearsCol.setCellValueFactory(p -> new javafx.beans.property.SimpleLongProperty(p.getValue().getStartDateYear()).asObject());
        yearsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    setText(getTableRow().getItem().getYearRange());
                }
            }
        });
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
        estDurationCol.setCellValueFactory(p -> new javafx.beans.property.SimpleDoubleProperty(p.getValue().getRemainingDurationSec()).asObject());
        estDurationCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    setText(getTableRow().getItem().getEstimatedDurationDisplay());
                }
            }
        });
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
                            label.setTooltip(new Tooltip(scItem.getRunId() != null && !scItem.getRunId().equals("N/A") ? "Run ID: " + scItem.getRunId() : ""));
                            setGraphic(label);
                            setText(null);
                        }
                        default -> {
                            label.setText(scItem.getStatusDisplay());
                            label.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                            label.setTooltip(new Tooltip(scItem.getRunId() != null && !scItem.getRunId().equals("N/A") ? "Run ID: " + scItem.getRunId() : ""));
                            setGraphic(label);
                            setText(null);
                        }
                    }
                }
            }
        });

        scenarioTable.getColumns().addAll(List.of(selectCol, nameCol, planetCol, clippingCol, yearsCol, progressCol, estDurationCol, statusCol));

        selectCol.setComparator(Boolean::compare);
        nameCol.setComparator(String.CASE_INSENSITIVE_ORDER);
        planetCol.setComparator(String.CASE_INSENSITIVE_ORDER);
        clippingCol.setComparator(String.CASE_INSENSITIVE_ORDER);
        yearsCol.setComparator(Long::compare);
        progressCol.setComparator(Double::compare);
        estDurationCol.setComparator(Double::compare);
        statusCol.setComparator(String.CASE_INSENSITIVE_ORDER);

        scenarioList = FXCollections.observableArrayList();
        filteredScenarioList = new FilteredList<>(scenarioList, p -> true);
        SortedList<ScenarioSelectableItem> sortedScenarioList = new SortedList<>(filteredScenarioList);
        sortedScenarioList.comparatorProperty().bind(Bindings.createObjectBinding(() -> {
            Comparator<ScenarioSelectableItem> baseComp = scenarioTable.getComparator();
            return (a, b) -> {
                if (a == b) return 0;
                if (a == null) return 1;
                if (b == null) return -1;
                boolean aGround = "HISTORICAL_GROUND_TRUTH".equals(a.getRunId());
                boolean bGround = "HISTORICAL_GROUND_TRUTH".equals(b.getRunId());
                if (aGround && bGround) return 0;
                if (aGround) return -1;
                if (bGround) return 1;
                if (baseComp != null) {
                    return baseComp.compare(a, b);
                }
                return 0;
            };
        }, scenarioTable.comparatorProperty()));

        scenarioTable.setSortPolicy(tv -> TableView.DEFAULT_SORT_POLICY.call(tv));

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredScenarioList.setPredicate(item -> {
                if (newVal == null || newVal.isBlank()) return true;
                String lower = newVal.toLowerCase();
                return item.getName().toLowerCase().contains(lower)
                    || item.getPlanetDisplayName().toLowerCase().contains(lower)
                    || item.getClippingDisplay().toLowerCase().contains(lower)
                    || item.getYearRange().toLowerCase().contains(lower)
                    || item.getProgressPercentDisplay().toLowerCase().contains(lower)
                    || item.getStatusDisplay().toLowerCase().contains(lower);
            });
        });

        scenarioTable.setItems(sortedScenarioList);

        warningLabel = new Label("");
        warningLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");

        executionContextBadge = new Label();
        executionContextBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");
        updateExecutionContextBadge();

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
        setupInterpolationComboRendering(interpolationCombo);
        interpolationCombo.setValue(HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
        interpolationCombo.setOnAction(e -> updateChartAndAnalysis());
        interpolationCombo.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.interp_combo", "Reconstruction kernel used to interpolate discrete historical benchmark datasets into continuous trajectories.")));

        HBox chartControlBox = new HBox(12, metricLabel, metricSelectorCombo, interpolationLabel, interpolationCombo);
        chartControlBox.setAlignment(Pos.CENTER_LEFT);

        xAxis = new NumberAxis();
        xAxis.setMinorTickVisible(false);
        xAxis.setAutoRanging(false);
        xAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                if (object == null) return "";
                double val = object.doubleValue();
                long y = Math.round(val);
                long absY = Math.abs(y);
                boolean isFr = I18n.getCurrentLanguage() == org.ether.society.i18n.Language.FRENCH;
                if (y < 0) return isFr ? String.format("%,d av. J.-C.", absY) : String.format("%,d BC", absY);
                if (y > 0) return isFr ? String.format("%,d ap. J.-C.", y) : String.format("%,d AD", y);
                return isFr ? "An 0" : "0 AD";
            }
            @Override public Number fromString(String string) { return null; }
        });

        yAxis = new NumberAxis();
        yAxis.setMinorTickVisible(false);
        yAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                if (object == null) return "";
                double val = object.doubleValue();
                double absVal = Math.abs(val);
                if (absVal >= 1e12) return String.format("%.1fT", val / 1e12);
                if (absVal >= 1e9) return String.format("%.1fB", val / 1e9);
                if (absVal >= 1e6) return String.format("%.1fM", val / 1e6);
                if (absVal >= 1e3) return String.format("%.1fk", val / 1e3);
                if (absVal < 0.01 && absVal > 0) return String.format("%.3f", val);
                return String.format("%.1f", val);
            }
            @Override public Number fromString(String string) { return null; }
        });

        Tooltip.install(xAxis, new Tooltip(I18n.getOrDefault("analytics.axis.tooltip_x", "Chronological Timeline: Calendar years (BC / AD). Mouse wheel zooms on cursor, drag pans, double-click resets.")));
        Tooltip.install(yAxis, new Tooltip(I18n.getOrDefault("analytics.axis.tooltip_y", "Physical Metric Value with standardized dimensional SI units.")));

        chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setTitle(I18n.getOrDefault("analytics.chart.title", "Multi-Scenario Chronological Overlay (💡 Scroll to Zoom on Cursor, Drag to Pan, Double-Click to Reset)"));
        chart.getStyleClass().add("card-section");
        VBox.setVgrow(chart, Priority.ALWAYS);

        final double[] dragAnchor = new double[2];
        chart.setOnMousePressed(e -> {
            dragAnchor[0] = e.getX();
            dragAnchor[1] = e.getY();
        });
        chart.setOnMouseDragged(e -> {
            double dx = e.getX() - dragAnchor[0];
            dragAnchor[0] = e.getX();
            double range = xAxis.getUpperBound() - xAxis.getLowerBound();
            double plotWidth = Math.max(10.0, chart.getWidth() - 80.0);
            double shift = (dx / plotWidth) * range;
            ComparativeTimeSeriesChartCoordinator.setXAxisRange(xAxis, xAxis.getLowerBound() - shift, xAxis.getUpperBound() - shift);
            chart.setCursor(javafx.scene.Cursor.CLOSED_HAND);
        });
        chart.setOnMouseReleased(e -> chart.setCursor(javafx.scene.Cursor.DEFAULT));
        chart.setOnScroll(e -> {
            e.consume();
            double zoomFactor = e.getDeltaY() > 0 ? 0.82 : 1.22;
            double currentLower = xAxis.getLowerBound();
            double currentUpper = xAxis.getUpperBound();
            double range = currentUpper - currentLower;

            double plotX = Math.max(0.0, Math.min(chart.getWidth(), e.getX() - 50.0));
            double plotWidth = Math.max(10.0, chart.getWidth() - 80.0);
            double mouseRatio = Math.max(0.0, Math.min(1.0, plotX / plotWidth));
            double mouseDataX = currentLower + mouseRatio * range;

            double newLower = mouseDataX - (mouseDataX - currentLower) * zoomFactor;
            double newUpper = mouseDataX + (currentUpper - mouseDataX) * zoomFactor;
            if (newUpper - newLower > 0.5) {
                ComparativeTimeSeriesChartCoordinator.setXAxisRange(xAxis, newLower, newUpper);
            }
        });
        chart.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 || e.getButton() == MouseButton.SECONDARY) {
                updateChartAndAnalysis();
            }
        });

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
        copyReportBtn.setOnAction(e -> copyReportToClipboard());

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

        mainTabPane = new TabPane(scenariosTab, timeSeriesTab, spatialCartoTab, reportTab);
        mainTabPane.getStyleClass().add("primary-tab-pane");
        VBox.setVgrow(mainTabPane, Priority.ALWAYS);
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == reportTab) {
                runAnalysis();
            } else if (newTab == spatialCartoTab) {
                update2DSpatialComparison();
            } else {
                if (spatialTensorComparisonPane != null) {
                    spatialTensorComparisonPane.stopAnimation();
                }
            }
        });

        setCenter(mainTabPane);
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

        Scenario histScenario = new Scenario();
        histScenario.setName(I18n.getOrDefault("analytics.scenario.ground_truth", "🌍 Historical Reality (Cliodynamic Ground Truth)"));
        histScenario.setStartDateYear(-100000);
        histScenario.setEndDateYear(2026);
        boolean wasHistSelected = previousSelectedNames.isEmpty() || previousSelectedNames.contains("HISTORICAL_GROUND_TRUTH") || previousSelectedNames.contains(histScenario.getName());
        ScenarioSelectableItem histItem = new ScenarioSelectableItem(histScenario, wasHistSelected, true, "HISTORICAL_GROUND_TRUTH");
        histItem.selectedProperty().addListener((obs, oldV, newV) -> {
            updateScenarioPairCombos();
            checkExecutionStatus();
            updateChartAndAnalysis();
            updateReportIfActive();
            update2DSpatialComparison();
        });
        scenarioList.add(histItem);

        List<Scenario> allScenarios = scenarioRepository.getAllScenarios();
        List<SaveMetadata> allSaves = Collections.emptyList();
        try {
            allSaves = saveManager.listSaves();
        } catch (Exception e) {
            logger.warn("Failed to list saves for analytics revalidation: {}", e.getMessage());
        }

        H3SimulationEngine activeEngine = (engineSupplier != null) ? engineSupplier.get() : null;
        Set<String> matchedSaveIds = new HashSet<>();

        for (Scenario sc : allScenarios) {
            String scName = sc.getDisplayName();
            if (scName == null || scName.isBlank()) {
                scName = I18n.getOrDefault("analytics.unnamed_scenario", "Unnamed Scenario") + (sc.getId() != null ? " (#" + sc.getId() + ")" : "");
            }

            Optional<SimulationRunRecord> recordOpt = runRepository.getRunByScenarioName(sc.getName());
            if (!recordOpt.isPresent()) recordOpt = runRepository.getRunByScenarioName(scName);
            if (!recordOpt.isPresent() && sc.getId() != null) recordOpt = Optional.ofNullable(runRepository.getRun(String.valueOf(sc.getId())));

            SaveMetadata matchingSave = null;
            String scNorm = sc.getName() != null ? sc.getName().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
            String scKey = sc.getPresetKey() != null ? sc.getPresetKey().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
            String scDisp = sc.getDisplayName() != null ? sc.getDisplayName().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
            String scDensity = sc.getPopulationDensityType() != null ? sc.getPopulationDensityType().toLowerCase().replaceAll("[^a-z0-9]", "") : "";

            for (SaveMetadata save : allSaves) {
                String saveId = save.getId() != null ? save.getId().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
                String saveNorm = save.getName() != null ? save.getName().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
                String saveScNorm = save.getScenarioName() != null ? save.getScenarioName().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
                
                boolean textMatch = (!scKey.isEmpty() && (saveNorm.contains(scKey) || saveScNorm.contains(scKey) || saveId.contains(scKey)))
                             || (!scNorm.isEmpty() && (saveNorm.contains(scNorm) || saveScNorm.contains(scNorm) || saveId.contains(scNorm)))
                             || (!scDisp.isEmpty() && (saveNorm.contains(scDisp) || saveScNorm.contains(scDisp) || saveId.contains(scDisp)))
                             || (!scDensity.isEmpty() && (saveNorm.contains(scDensity) || saveScNorm.contains(scDensity)));

                boolean epochSemanticMatch = false;
                if (!textMatch) {
                    if ((scNorm.contains("afrique") || scNorm.contains("africa") || scNorm.contains("toba") || scNorm.contains("sapiens")) && (saveId.contains("epoch1") || saveId.contains("paleolithic") || saveNorm.contains("africa") || saveScNorm.contains("afrique"))) {
                        epochSemanticMatch = true;
                    } else if ((scNorm.contains("dryas") || scNorm.contains("natufien")) && (saveId.contains("dryas") || saveNorm.contains("dryas") || saveScNorm.contains("dryas"))) {
                        epochSemanticMatch = true;
                    } else if ((scNorm.contains("bering") || scNorm.contains("solutre") || scNorm.contains("lgm") || scNorm.contains("glaciaire")) && (saveId.contains("epoch2") || saveId.contains("lgm") || saveNorm.contains("beringia"))) {
                        epochSemanticMatch = true;
                    } else if ((scNorm.contains("fertile") || scNorm.contains("crescent") || scNorm.contains("sahara") || scNorm.contains("neolith")) && (saveId.contains("epoch3") || saveId.contains("neolithic") || saveNorm.contains("sahara"))) {
                        epochSemanticMatch = true;
                    } else if ((scNorm.contains("bronze") || scNorm.contains("mesopotam") || scNorm.contains("assyri") || scNorm.contains("harappa") || scNorm.contains("egypt")) && (saveId.contains("epoch4") || saveId.contains("bronze") || saveId.contains("harappa") || saveNorm.contains("egypt"))) {
                        epochSemanticMatch = true;
                    }
                }

                if (textMatch || epochSemanticMatch) {
                    matchedSaveIds.add(save.getId());
                    boolean isSaveRun = save.getId() != null && save.getId().startsWith("RUN-");
                    boolean prevIsRun = matchingSave != null && matchingSave.getId() != null && matchingSave.getId().startsWith("RUN-");
                    if (matchingSave == null || (isSaveRun && !prevIsRun) || (isSaveRun == prevIsRun && save.getYear() > matchingSave.getYear())) {
                        matchingSave = save;
                    }
                }
            }

            boolean isInteractiveMatch = (activeEngine != null 
                && activeEngine.getCurrentScenario() != null 
                && (activeEngine.getCurrentScenario().getName().equalsIgnoreCase(sc.getName())
                    || (sc.getId() != null && sc.getId().equals(activeEngine.getCurrentScenario().getId()))));

            long startYear = sc.getStartDateYear();
            long endYear = sc.getEndDateYear();
            long totalYears = Math.max(1, endYear - startYear);
            long maxYearReached = startYear;
            String runId = "N/A";

            if (recordOpt.isPresent() && !recordOpt.get().getTimeSeriesData().isEmpty()) {
                SimulationRunRecord rec = recordOpt.get();
                runId = rec.getRunId();
                maxYearReached = Math.max(maxYearReached, rec.getEndYear());
            }

            if (matchingSave != null) {
                String saveId = matchingSave.getId();
                ComparativeTelemetryDataLoader.ensureHistoryLoadedForSave(saveId, sc.getName(), runRepository);
                recordOpt = Optional.ofNullable(runRepository.getRun(saveId));
                if (recordOpt.isPresent() && !recordOpt.get().getTimeSeriesData().isEmpty()) {
                    runId = saveId;
                    maxYearReached = Math.max(maxYearReached, matchingSave.getYear());
                }
            }

            if (isInteractiveMatch) {
                maxYearReached = Math.max(maxYearReached, activeEngine.getCurrentYear());
            }

            boolean hasTelemetry = recordOpt.isPresent() && !recordOpt.get().getTimeSeriesData().isEmpty();
            double progress = Math.min(1.0, Math.max(0.0, (double) (maxYearReached - startYear) / totalYears));
            if (hasTelemetry && (matchingSave != null || recordOpt.get().getEndYear() >= endYear)) {
                progress = 1.0;
            }
            boolean executed = hasTelemetry && progress >= 1.0;
            if (!hasTelemetry) {
                executed = false;
                runId = "N/A";
            }

            boolean isSelected = previousSelectedNames.contains(sc.getName())
                || previousSelectedNames.contains(scName)
                || (sc.getId() != null && previousSelectedNames.contains(String.valueOf(sc.getId())))
                || (!"N/A".equals(runId) && previousSelectedNames.contains(runId));

            ScenarioSelectableItem item = new ScenarioSelectableItem(sc, isSelected, executed, runId);
            item.setProgress(progress);
            item.recalculateEstimate();

            item.selectedProperty().addListener((obs, oldV, newV) -> {
                updateScenarioPairCombos();
                checkExecutionStatus();
                scheduleDebouncedAnalyticsUpdate();
            });

            scenarioList.add(item);
            if (item.isExecuted() && item.getRunId() != null && !item.getRunId().equals("N/A")) {
                matchedSaveIds.add(item.getRunId());
            }
        }

        for (SaveMetadata save : allSaves) {
            String saveId = save.getId();
            if (saveId == null || matchedSaveIds.contains(saveId)) continue;

            String runName = save.getName();
            if (runName == null || runName.isBlank() || runName.equalsIgnoreCase("Unknown")) {
                runName = save.getScenarioName();
            }
            if (runName == null || runName.isBlank()) runName = saveId;

            String runNameLower = runName.toLowerCase();
            String saveIdLower = saveId.toLowerCase();
            if (runNameLower.contains("test_save") || runNameLower.contains("autosave_exit") ||
                saveIdLower.startsWith("autosave_") || saveIdLower.contains("test_save") ||
                runNameLower.startsWith("test") || runNameLower.equals("null")) {
                continue;
            }

            ComparativeTelemetryDataLoader.ensureHistoryLoadedForSave(saveId, save.getScenarioName(), runRepository);
            SimulationRunRecord runRec = runRepository.getRun(saveId);
            if (runRec != null && !runRec.getTimeSeriesData().isEmpty()) {
                long startY = runRec.getStartYear();
                long endY = Math.max(startY + 1, runRec.getEndYear());

                final String finalRunName = runName;
                boolean alreadyListed = scenarioList.stream().anyMatch(ex -> ex.getName().equalsIgnoreCase(finalRunName));
                if (alreadyListed) {
                    matchedSaveIds.add(saveId);
                    continue;
                }

                Scenario synthScenario = new Scenario();
                synthScenario.setName(runName);
                synthScenario.setStartDateYear(startY);
                synthScenario.setEndDateYear(endY);
                synthScenario.setH3Resolution(save.getH3Resolution() > 0 ? save.getH3Resolution() : 3);

                boolean isSelected = previousSelectedNames.contains(runName) || previousSelectedNames.contains(saveId);
                ScenarioSelectableItem extraItem = new ScenarioSelectableItem(synthScenario, isSelected, true, saveId);
                extraItem.setProgress(1.0);
                extraItem.recalculateEstimate();

                extraItem.selectedProperty().addListener((obs, oldV, newV) -> {
                    updateScenarioPairCombos();
                    checkExecutionStatus();
                    scheduleDebouncedAnalyticsUpdate();
                });

                scenarioList.add(extraItem);
                matchedSaveIds.add(saveId);
            }
        }

        updateScenarioPairCombos();
        checkExecutionStatus();
        updateChartAndAnalysis();
        updateReportIfActive();
        update2DSpatialComparison();
    }

    private void updateReportIfActive() {
        runAnalysis();
    }

    private void updateScenarioPairCombos() {
        if (spatialTensorComparisonPane != null) {
            spatialTensorComparisonPane.updateScenarioPairCombos(scenarioList);
        }
    }

    public SimulationRunRecord resolveRunRecord(ScenarioSelectableItem item) {
        return ComparativeTelemetryDataLoader.resolveRunRecord(item, runRepository);
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
        int cores = Runtime.getRuntime().availableProcessors();
        executionContextBadge.setText(String.format(I18n.getOrDefault("analytics.badge.context_gpu_off_fmt", "💻 Contexte : CPU Multi-cœurs (%d cœurs | Sans GPU)"), cores));
        executionContextBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-padding: 0; -fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: default;");
        executionContextBadge.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.context_details",
            "Contexte d'exécution de la simulation : calcul multithreadé sur les cœurs CPU de la machine locale avec vectorisation SIMD native. Le GPU est inactif pour garantir une stricte reproductibilité déterministe bit-à-bit.")));
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
            }
        );
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

        boolean hasGroundTruth = selectedExecuted.stream().anyMatch(i -> "HISTORICAL_GROUND_TRUTH".equals(i.getRunId()));
        List<ScenarioSelectableItem> simulatedSelected = selectedExecuted.stream()
            .filter(i -> !"HISTORICAL_GROUND_TRUTH".equals(i.getRunId()))
            .toList();

        if (hasGroundTruth && !simulatedSelected.isEmpty()) {
            HistoricalValidationKernel.InterpolationMethod method = (interpolationCombo != null && interpolationCombo.getValue() != null)
                    ? interpolationCombo.getValue() : HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC;
            AuditResult result = GroundTruthAuditGenerator.generateMultiScenarioGroundTruthAudit(simulatedSelected, method, this::resolveRunRecord);
            updateAuditResultLabels(result);
        } else if (!simulatedSelected.isEmpty() && simulatedSelected.size() >= 2) {
            AuditResult result = GroundTruthAuditGenerator.generateInterScenarioComparativeReport(simulatedSelected, analyzer, this::resolveRunRecord, runRepository);
            updateAuditResultLabels(result);
        }
    }

    private void updateAuditResultLabels(AuditResult result) {
        if (result.divergenceYear() != -1) {
            divergenceLabel.setText(String.format(I18n.getOrDefault("analytics.status.first_divergence", "Point de rupture détecté (T_divergence) : ANNÉE %d"), result.divergenceYear()));
            divergenceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        } else {
            divergenceLabel.setText(I18n.getOrDefault("analytics.status.parallel", "Trajectoires parallèles (Aucune divergence > 10%)"));
            divergenceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        }
        explanationLabel.setText(result.explanationSummary());
        reportPreviewPane.setMarkdown(result.reportMarkdown());
    }

    private void updateChartAndAnalysis() {
        ComparativeTimeSeriesChartCoordinator.updateChartAndAnalysis(
            chart, xAxis, yAxis, scenarioList, metricSelectorCombo, interpolationCombo,
            timeSeriesWarningBox, timeSeriesWarningLabel, this::resolveRunRecord, isUpdatingTexts
        );
    }

    private void update2DSpatialComparison() {
        if (spatialTensorComparisonPane != null) {
            spatialTensorComparisonPane.update2DSpatialComparison();
        }
    }

    public SpatialTensorComparisonPane getSpatialTensorComparisonPane() {
        return spatialTensorComparisonPane;
    }

    private void copyReportToClipboard() {
        String currentReport = (reportPreviewPane != null) ? reportPreviewPane.getMarkdown() : "";
        if (currentReport == null || currentReport.isBlank() || currentReport.startsWith("## Please select")) return;

        try {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(currentReport);
            clipboard.setContent(content);

            String origText = copyReportBtn.getText();
            copyReportBtn.setText(I18n.getOrDefault("analytics.status.copy_success", "✅ Rapport Copié !"));
            copyReportBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #059669; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");

            Timeline resetBtnTimer = new Timeline(new KeyFrame(Duration.millis(2500), ev -> {
                copyReportBtn.setText(origText);
                copyReportBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
            }));
            resetBtnTimer.setCycleCount(1);
            resetBtnTimer.play();
            logger.info("Successfully copied markdown analysis report to clipboard.");
        } catch (Exception ex) {
            logger.error("Failed to copy report to system clipboard: {}", ex.getMessage());
        }
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
            if (headerLabel != null) headerLabel.setText(I18n.getOrDefault("analytics.header", "📊 COMPARATIVE ANALYTICS & SCENARIO BENCHMARKS (DEEP ANALYTICS)"));
            if (tableTitle != null) tableTitle.setText(I18n.getOrDefault("analytics.table_title", "📋 Historical Baseline & Simulated Scenarios"));
            if (selectCol != null) selectCol.setText(I18n.getOrDefault("analytics.col.compare", "Compare"));
            if (nameCol != null) nameCol.setText(I18n.getOrDefault("analytics.col.name", "Scenario Name"));
            if (planetCol != null) planetCol.setText(I18n.getOrDefault("analytics.col.planet", "Planète"));
            if (clippingCol != null) clippingCol.setText(I18n.getOrDefault("analytics.col.clipping", "Fenêtrage"));
            if (yearsCol != null) yearsCol.setText(I18n.getOrDefault("analytics.col.years", "Time Range"));
            if (progressCol != null) progressCol.setText(I18n.getOrDefault("analytics.col.progress", "Progress"));
            if (estDurationCol != null) estDurationCol.setText(I18n.getOrDefault("analytics.col.est_duration", "Remaining Time"));
            if (statusCol != null) statusCol.setText(I18n.getOrDefault("analytics.col.status", "Database Execution Status"));
            if (metricLabel != null) metricLabel.setText(I18n.getOrDefault("analytics.metric_label", "Visualized Metric:"));
            if (interpolationLabel != null) interpolationLabel.setText(I18n.getOrDefault("analytics.interp_label", "Interpolation:"));
            if (spatialTensorComparisonPane != null) spatialTensorComparisonPane.updateTexts(scenarioList);
            updateExecutionContextBadge();
            if (copyReportBtn != null) {
                copyReportBtn.setText(I18n.getOrDefault("analytics.btn.copy", "📋 Copy Report"));
                copyReportBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.copy", "Copy markdown audit report.")));
            }
            if (exportMdBtn != null) {
                exportMdBtn.setText(I18n.getOrDefault("analytics.btn.export_md", "📝 Export Report (.md)"));
                exportMdBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.export_md", "Export complete markdown report.")));
            }
            if (exportCsvBtn != null) {
                exportCsvBtn.setText(I18n.getOrDefault("analytics.btn.export_csv", "📥 Export Data (.csv)"));
                exportCsvBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.export_csv", "Export raw multi-scenario telemetry into CSV.")));
            }
            if (export1DCsvBtn != null) {
                export1DCsvBtn.setText(I18n.getOrDefault("analytics.btn.export_1d_csv", "📥 Exporter les données CSV (Séries 1D)"));
                export1DCsvBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.export_1d_csv", "Exporter les séries temporelles 1D sélectionnées en CSV.")));
            }
            if (executeMissingBtn != null) {
                executeMissingBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.execute", "Run physical headless simulation across queued scenarios.")));
            }
            if (cancelBatchBtn != null) {
                cancelBatchBtn.setText(I18n.getOrDefault("analytics.btn.cancel_batch", "🛑 Cancel Batch"));
                cancelBatchBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.cancel", "Cancel pending background batch.")));
            }
            if (chartHintLabel != null) {
                chartHintLabel.setText(I18n.getOrDefault("analytics.chart.hint", "💡 Scroll wheel: Zoom on cursor | Drag: Pan horizontally | Double-click: Reset time range"));
            }
            if (xAxis != null) {
                xAxis.setLabel(I18n.getOrDefault("analytics.axis.x", "Calendar Year (BCE / CE)"));
            }
            if (yAxis != null) yAxis.setLabel(I18n.getOrDefault("analytics.axis.y", "Metric Value"));
            if (chart != null) chart.setTitle(I18n.getOrDefault("analytics.chart.title", "Multi-Scenario Chronological Overlay"));
            if (searchField != null) searchField.setPromptText(I18n.getOrDefault("analytics.search_prompt", "🔍 Filter scenarios by name, status, or year range..."));
            if (scenariosTab != null) scenariosTab.setText(I18n.getOrDefault("analytics.tab.scenarios", "📋 Scenarios & Data"));
            if (timeSeriesTab != null) timeSeriesTab.setText(I18n.getOrDefault("analytics.tab.timeseries", "📈 Time Series (1D)"));
            if (spatialCartoTab != null) spatialCartoTab.setText(I18n.getOrDefault("analytics.tab.carto_tensors", "🗺️ Cartography & Tensors (2D)"));
            if (reportTab != null) reportTab.setText(I18n.getOrDefault("analytics.tab.report", "📄 Analysis Report & Synthesis"));

            if (divergenceLabel != null && (divergenceLabel.getText() == null || divergenceLabel.getText().isBlank() || divergenceLabel.getText().startsWith("Point de rupture") || divergenceLabel.getText().startsWith("Point of divergence") || divergenceLabel.getText().startsWith("Break point"))) {
                divergenceLabel.setText(I18n.getOrDefault("analytics.divergence.select_hint", "Point of divergence: Select at least 2 scenarios"));
            }
            if (explanationLabel != null && (explanationLabel.getText() == null || explanationLabel.getText().isBlank() || explanationLabel.getText().startsWith("Cochez les scénarios") || explanationLabel.getText().startsWith("Check scenarios"))) {
                explanationLabel.setText(I18n.getOrDefault("analytics.divergence.check_hint", "Check scenarios in the list above to start comparison."));
            }

            if (interpolationCombo != null) {
                HistoricalValidationKernel.InterpolationMethod currentInterp = interpolationCombo.getValue();
                setupInterpolationComboRendering(interpolationCombo);
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
            updateScenarioPairCombos();
        } finally {
            isUpdatingTexts = false;
        }
    }

    private void setupInterpolationComboRendering(ComboBox<HistoricalValidationKernel.InterpolationMethod> combo) {
        if (combo == null) return;
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(HistoricalValidationKernel.InterpolationMethod item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : I18n.getOrDefault("interpolation." + item.name().toLowerCase(), item.name()));
            }
        });
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(HistoricalValidationKernel.InterpolationMethod item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : I18n.getOrDefault("interpolation." + item.name().toLowerCase(), item.name()));
            }
        });
        combo.setConverter(new javafx.util.StringConverter<>() {
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
    }
}
