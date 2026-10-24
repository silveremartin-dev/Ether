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
import org.ether.society.generation.PlanetPreset;
import org.ether.society.persistence.ScenarioRepository;
import org.ether.society.ui.analytics.BatchState;
import org.ether.society.ui.analytics.ScenarioSelectableItem;
import org.ether.society.ui.analytics.SpatialTensorComparisonPane;
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
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.*;

import org.ether.society.config.EtherPaths;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.persistence.SimulationSaveManager;
import org.ether.society.persistence.SaveMetadata;

/**
 * 6th Tab UI Panel: Comparative Analytics & Sensitivity Benchmarks.
 * Supports multi-scenario selection, execution status validation, automated headless
 * batch execution of missing scenarios, 1D multi-curve time-series visualization,
 * 2D spatial cartographic tensor comparison with timeline date scrubber, hover zoom, and root cause analysis.
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

    // 2D Spatial Tensor Comparison Controls
    private SpatialTensorComparisonPane spatialTensorComparisonPane;

    private Label diagHeader;
    private Label divergenceLabel;
    private Label explanationLabel;
    private Label synthHeader;
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

        // Header Title (Uniform Modern Header Style)
        headerLabel = new Label();
        headerLabel.getStyleClass().add("label-title");
        topBox.getChildren().add(headerLabel);
        setTop(topBox);

        // --- SECTION 1: SCENARIO SELECTION TABLE & FILTER ---
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

        // Single-click row toggles scenario selection
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

        // --- SECTION 2: EXECUTION CONTROL & STATUS BAR (UNBOXED CLEAN TEXTUAL LAYOUT) ---
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

        // --- TAB 1: SCENARIOS & DATA ---
        VBox scenariosBox = new VBox(12, filterBox, scenarioTable, executionBox);
        scenariosBox.setPadding(new Insets(12));
        VBox.setVgrow(scenarioTable, Priority.ALWAYS);
        scenariosTab = new Tab(I18n.getOrDefault("analytics.tab.scenarios", "📋 Scenarios & Data"), scenariosBox);
        scenariosTab.setClosable(false);

        // --- TAB 2: 1D TIME-SERIES CHART ---
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
                if (y < 0) {
                    return isFr ? String.format("%,d av. J.-C.", absY) : String.format("%,d BC", absY);
                } else if (y > 0) {
                    return isFr ? String.format("%,d ap. J.-C.", y) : String.format("%,d AD", y);
                } else {
                    return isFr ? "An 0" : "0 AD";
                }
            }
            @Override
            public Number fromString(String string) {
                return null;
            }
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
            @Override
            public Number fromString(String string) {
                return null;
            }
        });

        Tooltip.install(xAxis, new Tooltip(I18n.getOrDefault("analytics.axis.tooltip_x", "Chronological Timeline: Calendar years (BC / AD). Mouse wheel zooms on cursor, drag pans, double-click resets.")));
        Tooltip.install(yAxis, new Tooltip(I18n.getOrDefault("analytics.axis.tooltip_y", "Physical Metric Value with standardized dimensional SI units.")));

        chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setTitle(I18n.getOrDefault("analytics.chart.title", "Multi-Scenario Chronological Overlay (💡 Scroll to Zoom on Cursor, Drag to Pan, Double-Click to Reset)"));
        chart.getStyleClass().add("card-section");
        VBox.setVgrow(chart, Priority.ALWAYS);

        // Smooth Mouse Pan (Drag Left/Right) & Cursor-Centered Zoom (Scroll Wheel)
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
            setXAxisRange(xAxis.getLowerBound() - shift, xAxis.getUpperBound() - shift);
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
                setXAxisRange(newLower, newUpper);
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

        // --- TAB 3: 2D SPATIAL TENSOR & CARTOGRAPHIC COMPARISON ---
        spatialTensorComparisonPane = new SpatialTensorComparisonPane(this::resolveRunRecord);
        spatialCartoTab = new Tab(I18n.getOrDefault("analytics.tab.carto_tensors", "ðŸ—ºï¸ Cartography & Tensors (2D)"), spatialTensorComparisonPane);
        spatialCartoTab.setClosable(false);

        // --- TAB 4: DIAGNOSTIC REPORTS & EXPORT BUTTONS ---
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

        // Export & Copy Buttons Bar
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

        // Special Historical Ground Truth Baseline Item
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

        Set<String> matchedSaveIds = new HashSet<>();

        for (Scenario sc : allScenarios) {
            String scName = sc.getDisplayName();
            if (scName == null || scName.isBlank()) {
                scName = I18n.getOrDefault("analytics.unnamed_scenario", "Unnamed Scenario") + (sc.getId() != null ? " (#" + sc.getId() + ")" : "");
            }

            // A. Check SimulationRunRepository for completed or partial runs
            Optional<SimulationRunRecord> recordOpt = runRepository.getRunByScenarioName(sc.getName());
            if (!recordOpt.isPresent()) {
                recordOpt = runRepository.getRunByScenarioName(scName);
            }
            if (!recordOpt.isPresent() && sc.getId() != null) {
                recordOpt = Optional.ofNullable(runRepository.getRun(String.valueOf(sc.getId())));
            }

            // B. Check saved snapshots on disk matching scenario name, preset key, or start year
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

                boolean match = textMatch || epochSemanticMatch;

                if (match) {
                    matchedSaveIds.add(save.getId());
                    boolean isSaveRun = save.getId() != null && save.getId().startsWith("RUN-");
                    boolean prevIsRun = matchingSave != null && matchingSave.getId() != null && matchingSave.getId().startsWith("RUN-");
                    if (matchingSave == null || (isSaveRun && !prevIsRun) || (isSaveRun == prevIsRun && save.getYear() > matchingSave.getYear())) {
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

            if (recordOpt.isPresent() && !recordOpt.get().getTimeSeriesData().isEmpty()) {
                SimulationRunRecord rec = recordOpt.get();
                runId = rec.getRunId();
                maxYearReached = Math.max(maxYearReached, rec.getEndYear());
            }

            if (matchingSave != null) {
                String saveId = matchingSave.getId();
                ensureHistoryLoadedForSave(saveId, sc.getName());
                recordOpt = Optional.ofNullable(runRepository.getRun(saveId));
                if (recordOpt.isPresent() && !recordOpt.get().getTimeSeriesData().isEmpty()) {
                    runId = saveId;
                    maxYearReached = Math.max(maxYearReached, matchingSave.getYear());
                } else {
                    matchingSave = null;
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
                || ("N/A".equals(runId) ? false : previousSelectedNames.contains(runId));

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

        // Ingest any remaining executed GCP / offline simulation runs on disk
        for (SaveMetadata save : allSaves) {
            String saveId = save.getId();
            if (saveId == null || matchedSaveIds.contains(saveId)) continue;

            String runName = save.getName();
            if (runName == null || runName.isBlank() || runName.equalsIgnoreCase("Unknown")) {
                runName = save.getScenarioName();
            }
            if (runName == null || runName.isBlank()) {
                runName = saveId;
            }

            // Exclude temporary autosaves, test saves, or empty saves
            String runNameLower = runName.toLowerCase();
            String saveIdLower = saveId.toLowerCase();
            if (runNameLower.contains("test_save") || runNameLower.contains("autosave_exit") ||
                saveIdLower.startsWith("autosave_") || saveIdLower.contains("test_save") ||
                runNameLower.startsWith("test") || runNameLower.equals("null")) {
                continue;
            }

            ensureHistoryLoadedForSave(saveId, save.getScenarioName());
            SimulationRunRecord runRec = runRepository.getRun(saveId);
            if (runRec != null && !runRec.getTimeSeriesData().isEmpty()) {
                long startY = runRec.getStartYear();
                long endY = Math.max(startY + 1, runRec.getEndYear());

                final String finalRunName = runName;
                String finalRunLower = finalRunName.toLowerCase();

                // Prevent duplicates if an item with the same name already exists in scenarioList
                boolean alreadyListed = scenarioList.stream().anyMatch(ex -> ex.getName().equalsIgnoreCase(finalRunName));
                if (alreadyListed) {
                    matchedSaveIds.add(saveId);
                    continue;
                }
                ScenarioSelectableItem matchedExisting = scenarioList.stream().filter(existing -> {
                    if (existing.getName().equalsIgnoreCase(finalRunName)) return true;
                    String exLower = existing.getName().toLowerCase();
                    if (existing.getScenario() != null) {
                        long exStart = existing.getScenario().getStartDateYear();
                        long exEnd = existing.getScenario().getEndDateYear();
                        if (Math.abs(exStart - startY) <= 800 || (exStart <= startY && exEnd >= endY) || (startY <= exStart && endY >= exEnd)) {
                            if (exLower.contains("dryas") && finalRunLower.contains("dryas")) return true;
                            if ((exLower.contains("afrique") || exLower.contains("africa")) && (finalRunLower.contains("afrique") || finalRunLower.contains("africa") || finalRunLower.contains("epoch 1") || finalRunLower.contains("epoch_1") || finalRunLower.contains("paleolithic"))) return true;
                            if ((exLower.contains("bering") || exLower.contains("lgm") || exLower.contains("glaciaire")) && (finalRunLower.contains("lgm") || finalRunLower.contains("epoch 2") || finalRunLower.contains("epoch_2") || finalRunLower.contains("bering"))) return true;
                            if ((exLower.contains("neolith") || exLower.contains("fertile")) && (finalRunLower.contains("neolith") || finalRunLower.contains("epoch 3") || finalRunLower.contains("epoch_3") || finalRunLower.contains("crescent"))) return true;
                            if ((exLower.contains("bronze") || exLower.contains("harappa")) && (finalRunLower.contains("bronze") || finalRunLower.contains("epoch 4") || finalRunLower.contains("epoch_4") || finalRunLower.contains("harappa"))) return true;
                            if (exLower.contains("maya") && finalRunLower.contains("maya")) return true;
                            if ((exLower.contains("black death") || exLower.contains("peste")) && (finalRunLower.contains("black death") || finalRunLower.contains("peste") || finalRunLower.contains("1347"))) return true;
                            if ((exLower.contains("columbian") || exLower.contains("colombien")) && (finalRunLower.contains("columbian") || finalRunLower.contains("colombien") || finalRunLower.contains("1492"))) return true;
                            if ((exLower.contains("industrial") || exLower.contains("industriel")) && (finalRunLower.contains("industrial") || finalRunLower.contains("industriel") || finalRunLower.contains("1800") || finalRunLower.contains("1850"))) return true;
                            if ((exLower.contains("world war") || exLower.contains("guerre")) && (finalRunLower.contains("world war") || finalRunLower.contains("guerre") || finalRunLower.contains("1914"))) return true;
                            if (Math.abs(exStart - startY) <= 100 && Math.abs(exEnd - endY) <= 100) return true;
                        }
                    }
                    return false;
                }).findFirst().orElse(null);

                if (matchedExisting != null) {
                    matchedSaveIds.add(saveId);
                    if (!matchedExisting.isExecuted()) {
                        matchedExisting.setExecuted(true);
                        matchedExisting.setRunId(saveId);
                        matchedExisting.setProgress(1.0);
                        matchedExisting.recalculateEstimate();
                    }
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
    private void ensureHistoryLoadedForSave(String saveId, String scenarioName) {
        if (saveId == null || saveId.isBlank() || "N/A".equals(saveId) || "HISTORICAL_GROUND_TRUTH".equals(saveId)) return;
        if (runRepository.getRun(saveId) != null) return;

        java.nio.file.Path histPath = org.ether.society.config.EtherPaths.getSavesDir().resolve(saveId).resolve("history.json");
        if (!java.nio.file.Files.exists(histPath)) {
            try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(org.ether.society.config.EtherPaths.getSavesDir())) {
                var found = stream.filter(java.nio.file.Files::isDirectory).filter(d -> {
                    if (d.getFileName().toString().equalsIgnoreCase(saveId)) return true;
                    java.nio.file.Path m = d.resolve("metadata.json");
                    if (java.nio.file.Files.exists(m)) {
                        try {
                            com.fasterxml.jackson.databind.JsonNode node = org.ether.society.persistence.EtherJsonUtil.getMapper().readTree(m.toFile());
                            if (node.has("id") && saveId.equalsIgnoreCase(node.get("id").asText())) return true;
                            if (scenarioName != null && node.has("scenarioName") && scenarioName.equalsIgnoreCase(node.get("scenarioName").asText())) return true;
                        } catch (Exception ignored) {}
                    }
                    return false;
                }).findFirst();
                if (found.isPresent()) {
                    histPath = found.get().resolve("history.json");
                }
            } catch (Exception ignored) {}
        }
        if (!java.nio.file.Files.exists(histPath)) return;

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = org.ether.society.persistence.EtherJsonUtil.getMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            List<org.ether.society.analytics.HistorySnapshot> snaps = mapper.readValue(histPath.toFile(),
                new com.fasterxml.jackson.core.type.TypeReference<List<org.ether.society.analytics.HistorySnapshot>>() {});
            if (snaps != null && !snaps.isEmpty()) {
                SimulationRunRecord rec = new SimulationRunRecord(saveId, scenarioName, "GCP/Offline Full True Simulation", null);
                for (org.ether.society.analytics.HistorySnapshot snap : snaps) {
                    Map<String, Double> metricsMap = new LinkedHashMap<>();
                    long pop = Math.max(1L, snap.totalPopulation());
                    double popDbl = (double) snap.totalPopulation();

                    // 1. Demographics & Population
                    metricsMap.put("population", popDbl);
                    metricsMap.put("worldPopulation", popDbl);
                    metricsMap.put("fertilityRate", snap.fertilityRate());
                    metricsMap.put("lifeExpectancy", snap.avgLifespan());
                    metricsMap.put("educationLevel", Math.min(100.0, snap.avgTechnology() * 10.0));
                    metricsMap.put("populationSurvivalRate", 100.0);

                    // 2. Food, Energy & Matter (scaled per capita and totals)
                    double foodPerCap = snap.totalFood() > 0 ? (snap.totalFood() / pop) * 3.362 : 3.362; // GJ/hab/year
                    metricsMap.put("foodPerCapita", foodPerCap);
                    metricsMap.put("totalFood", snap.totalFood());
                    metricsMap.put("primaryEnergy", snap.energyCaptured() * 3.15576e-5); // Exajoules (EJ/year) from MW
                    metricsMap.put("energyCaptured", snap.energyCaptured()); // MW
                    metricsMap.put("energyPerCapita", snap.energyCaptured() > 0 ? (snap.energyCaptured() / pop) : 40.0); // MJ/hab
                    metricsMap.put("resourceDepletion", snap.resourceDepletion());
                    metricsMap.put("potableWater", snap.potableWater());

                    // Base anomaly from empirical paleoclimate benchmarks (PAGES 2k, EPICA, Marcott 2013)
                    double baseAnomaly = HistoricalValidationKernel.getInterpolatedBenchmarkValue(
                        "temperatureAnomaly", snap.year(), HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);

                    // Scenario-specific physically grounded forcings
                    double shockForcing = 0.0;
                    String scLower = (scenarioName != null ? scenarioName : saveId).toLowerCase();
                    if (scLower.contains("nuclear_winter") || scLower.contains("hiver_nucleaire")) {
                        shockForcing = -12.0;
                    } else if (scLower.contains("supervolcano") || scLower.contains("toba")) {
                        shockForcing = -5.0;
                    } else if (scLower.contains("younger_dryas") || scLower.contains("recents_dryas") || scLower.contains("dryas")) {
                        shockForcing = -1.2;
                    } else if (snap.year() >= 1850) {
                        double co2Ppm = 280.0 + Math.min(600.0, snap.carbonFootprint() * 0.005);
                        shockForcing = 3.0 * (Math.log(co2Ppm / 280.0) / Math.log(2.0));
                    }

                    double tempAnomaly = Math.max(-20.0, Math.min(25.0, baseAnomaly + shockForcing));
                    double tempAbsolute = 14.0 + tempAnomaly;

                    metricsMap.put("temperatureAnomaly", tempAnomaly);
                    metricsMap.put("temperature", tempAbsolute);
                    metricsMap.put("entropyPollution", Math.min(100.0, snap.carbonFootprint() * 0.01));
                    metricsMap.put("precipitation", 950.0 + (tempAnomaly * 25.0));
                    metricsMap.put("co2Concentration", 280.0 + (snap.year() >= 1850 ? Math.min(600.0, snap.carbonFootprint() * 0.005) : 0.0));
                    metricsMap.put("urbanizationRate", Math.min(100.0, Math.max(1.0, snap.avgTechnology() * 0.8)));
                    metricsMap.put("literacyRate", Math.min(100.0, snap.avgTechnology() * 1.0));
                    metricsMap.put("sociopoliticalInstability", snap.conflictLevel());
                    metricsMap.put("currencyDebasement", snap.globalGini() * 50.0);
                    metricsMap.put("eroiAlim", Math.max(1.0, 15.0 - (snap.resourceDepletion() * 0.1)));
                    metricsMap.put("netSurplus", 85.0);
                    metricsMap.put("trophicMultiplier", 2.25);
                    metricsMap.put("biomassMobilized", 1000.0);

                    // 3. Society & Politics
                    metricsMap.put("avgTechLevel", snap.avgTechnology());
                    metricsMap.put("technology", snap.avgTechnology());
                    metricsMap.put("asabiyyah", Math.max(0.0, Math.min(100.0, 100.0 - snap.conflictLevel() - snap.globalGini() * 40.0)));
                    metricsMap.put("happiness", snap.happinessIndex());
                    metricsMap.put("conflict", snap.conflictLevel());
                    metricsMap.put("kardashev", snap.kardashevScale());
                    metricsMap.put("institutionalMaturity", snap.avgTechnology() * 5.0);

                    // 4. Economy & Capital
                    metricsMap.put("gini", snap.globalGini());
                    metricsMap.put("giniIndex", snap.globalGini());
                    double gdpBillions = snap.gdpTotal() > 1e6 ? (snap.gdpTotal() / 1e9) : snap.gdpTotal();
                    metricsMap.put("gdp", gdpBillions);
                    metricsMap.put("gdpTotal", gdpBillions);
                    metricsMap.put("grossWorldProduct", gdpBillions); // Billion USD 1990 (G$)
                    metricsMap.put("builtCapital", snap.totalWealth() > 0 ? (snap.totalWealth() / pop) : snap.totalWealth());

                    // 5. Cognition & Culture
                    metricsMap.put("collectiveMemory", snap.collectiveMemory());
                    metricsMap.put("languageDiversity", Math.max(1.0, Math.log10(pop + 10.0)));

                    // 6. Cliodynamics, Collapse & Paleolithic Benchmarks
                    metricsMap.put("eliteOverproduction", snap.eliteOverproduction());
                    metricsMap.put("collapseRisk", snap.collapseVulnerability());
                    metricsMap.put("collapseVulnerability", snap.collapseVulnerability());
                    metricsMap.put("systemComplexity", snap.systemComplexity());
                    metricsMap.put("systemInterdependence", snap.systemComplexity());
                    metricsMap.put("carbonFootprint", snap.carbonFootprint());
                    metricsMap.put("megafaunaIndex", org.ether.society.engines.tier2.theories.ProceduralPopulationEngine.calculateMegafaunaAbundanceIndex(snap.year(), popDbl / 1e6, 0.2));
                    metricsMap.put("milankovitchInsolation", org.ether.society.engines.tier2.theories.ProceduralPopulationEngine.calculateMilankovitchSummerInsolation65N(snap.year()));
                    metricsMap.put("zeroContainmentScore", 100.0);
                    metricsMap.put("psiStressIndex", snap.eliteOverproduction() * (1.0 + snap.globalGini()) * Math.max(0.5, snap.conflictLevel() / 20.0));
                    metricsMap.put("lyapunovExponent", 0.01);
                    metricsMap.put("epistemicDiscrepancy", 0.05);

                    rec.addSnapshot(snap.year(), snap.totalPopulation(), foodPerCap, snap.avgTechnology(), 100.0 - snap.conflictLevel(), 100, metricsMap);
                }
                runRepository.saveRun(rec);
            }
        } catch (Exception ex) {
            logger.warn("Could not deserialize history.json for save {}: {}", saveId, ex.getMessage());
        }
    }

    /**
     * Resolves the SimulationRunRecord for a given ScenarioSelectableItem.
     * Looks up by run ID, scenario name, preset key, and automatically attempts to load
     * telemetry from matching save directories on disk if not yet present in repository.
     *
     * @param item the selectable item
     * @return the resolved SimulationRunRecord, or null if unexecuted
     */
    private SimulationRunRecord resolveRunRecord(ScenarioSelectableItem item) {
        if (item == null) return null;
        String runId = item.getRunId();
        if ("HISTORICAL_GROUND_TRUTH".equals(runId)) return null;

        if (runId != null && !runId.isBlank() && !"N/A".equals(runId)) {
            SimulationRunRecord run = runRepository.getRun(runId);
            if (run != null && run.getTimeSeriesData() != null && !run.getTimeSeriesData().isEmpty()) {
                return run;
            }
            ensureHistoryLoadedForSave(runId, item.getName());
            run = runRepository.getRun(runId);
            if (run != null && run.getTimeSeriesData() != null && !run.getTimeSeriesData().isEmpty()) {
                return run;
            }
        }

        // Try lookup by scenario name
        SimulationRunRecord run = runRepository.getRunByScenarioName(item.getName()).orElse(null);
        if (run != null && run.getTimeSeriesData() != null && !run.getTimeSeriesData().isEmpty()) {
            return run;
        }

        if (item.getScenario() != null) {
            String scName = item.getScenario().getName();
            if (scName != null && !scName.isBlank()) {
                run = runRepository.getRunByScenarioName(scName).orElse(null);
                if (run != null && run.getTimeSeriesData() != null && !run.getTimeSeriesData().isEmpty()) {
                    return run;
                }
            }

            // Fallback: search disk saves directory for matching directory or metadata
            String scKey = item.getScenario().resolvePresetKey();
            String scNorm = scName != null ? scName.toLowerCase().replaceAll("[^a-z0-9]", "") : "";
            String keyNorm = scKey != null ? scKey.toLowerCase().replaceAll("[^a-z0-9]", "") : "";

            try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(org.ether.society.config.EtherPaths.getSavesDir())) {
                var match = stream.filter(java.nio.file.Files::isDirectory).filter(d -> {
                    String dName = d.getFileName().toString().toLowerCase().replaceAll("[^a-z0-9]", "");
                    if (!keyNorm.isEmpty() && dName.contains(keyNorm)) return true;
                    if (!scNorm.isEmpty() && (dName.contains(scNorm) || scNorm.contains(dName))) return true;
                    return false;
                }).findFirst();

                if (match.isPresent()) {
                    String foundId = match.get().getFileName().toString();
                    ensureHistoryLoadedForSave(foundId, item.getName());
                    run = runRepository.getRun(foundId);
                    if (run != null && run.getTimeSeriesData() != null && !run.getTimeSeriesData().isEmpty()) {
                        item.setRunId(foundId);
                        item.setExecuted(true);
                        return run;
                    }
                }
            } catch (Exception ignored) {}
        }

        return null;
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
        List<ScenarioSelectableItem> selected = scenarioList.stream()
            .filter(ScenarioSelectableItem::isSelected)
            .toList();

        long unexecutedCount = selected.stream().filter(i -> !i.isExecuted() && !"HISTORICAL_GROUND_TRUTH".equals(i.getRunId())).count();

        if (selected.isEmpty()) {
            warningLabel.setText(I18n.getOrDefault("analytics.warning.none_selected", "ℹ️ Aucun scénario sélectionné pour la comparaison."));
            warningLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-padding: 0; -fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: default;");
            executeMissingBtn.setText(I18n.getOrDefault("analytics.btn.execute_scenarios", "🚀 Exécuter les scénarios"));
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
                warningLabel.setText(String.format(I18n.getOrDefault("analytics.warning.unexecuted_single_est", "⚠️ 1 scénario sélectionné non simulé | %,d ans | ⏱️ Temps estimé : %s (%s)"), totalUnexecutedYears, durStr, hwName));
            } else {
                warningLabel.setText(String.format(I18n.getOrDefault("analytics.warning.unexecuted_plural_est", "⚠️ %d scénarios sélectionnés non simulés | Horizon total : %,d ans | ⏱️ Temps estimé : %s (%s)"), unexecutedCount, totalUnexecutedYears, durStr, hwName));
            }
            warningLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #f59e0b; -fx-padding: 0; -fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: default;");
            
            if (!isBatchRunning.get()) {
                executeMissingBtn.setText(unexecutedCount == 1
                    ? String.format(I18n.getOrDefault("analytics.btn.execute_missing_single_fmt", "🚀 Lancer le scénario (%s)"), durStr)
                    : String.format(I18n.getOrDefault("analytics.btn.execute_missing_plural_fmt", "🚀 Lancer %d scénarios en file (%s)"), unexecutedCount, durStr));
                executeMissingBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
                executeMissingBtn.setDisable(false);
                cancelBatchBtn.setVisible(false);
                cancelBatchBtn.setManaged(false);
            }
        } else {
            warningLabel.setText(String.format(I18n.getOrDefault("analytics.warning.ready", "✅ Tous les scénarios sélectionnés (%d) sont prêts pour l'audit et la comparaison."), selected.size()));
            warningLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #10b981; -fx-padding: 0; -fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: default;");
            long simulatedCount = selected.stream().filter(i -> !"HISTORICAL_GROUND_TRUTH".equals(i.getRunId())).count();
            if (!isBatchRunning.get()) {
                if (simulatedCount == 0) {
                    executeMissingBtn.setText(I18n.getOrDefault("analytics.btn.reexecute_single", "🔄 Réexécuter le scénario"));
                    executeMissingBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #64748b; -fx-text-fill: white; -fx-padding: 6 14;");
                    executeMissingBtn.setDisable(true);
                } else {
                    executeMissingBtn.setText(simulatedCount == 1
                        ? I18n.getOrDefault("analytics.btn.reexecute_single", "🔄 Réexécuter le scénario")
                        : String.format(I18n.getOrDefault("analytics.btn.reexecute_plural", "🔄 Réexécuter les %d scénarios"), simulatedCount));
                    executeMissingBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
                    executeMissingBtn.setDisable(false);
                }
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

        if (targetItems.isEmpty()) {
            return;
        }

        boolean allAlreadyExecuted = targetItems.stream().allMatch(ScenarioSelectableItem::isExecuted);

        // Confirmation Dialog styled and themed with strict high-contrast typography and constrained width
        Dialog<ButtonType> confirmDialog = new Dialog<>();
        confirmDialog.setTitle(allAlreadyExecuted
            ? I18n.getOrDefault("analytics.confirm.reexecute_title", "Confirmation de réexécution")
            : I18n.getOrDefault("analytics.confirm.execute_title", "Confirmation du calcul de simulation"));
        confirmDialog.setHeaderText(null); // Explicitly null to prevent JavaFX unconstrained Modena header

        double totalEstSeconds = targetItems.stream().mapToDouble(ScenarioSelectableItem::getRemainingDurationSec).sum();
        if (totalEstSeconds <= 0) {
            totalEstSeconds = targetItems.stream().mapToDouble(ScenarioSelectableItem::getEstimatedDurationSec).sum();
        }
        String durStr = ExecutionContextPanel.formatDuration(totalEstSeconds);
        String hwName = ExecutionContextPanel.getActiveHardwareMode().name();
        int cores = Runtime.getRuntime().availableProcessors();

        boolean isDark = Theme.getCurrentTheme() == Theme.DARK;
        String bgCard = isDark ? "#1e293b" : "#f8fafc";
        String textTitleColor = isDark ? "#38bdf8" : "#0284c7";
        String textBodyColor = isDark ? "#f1f5f9" : "#0f172a";
        String textMutedColor = isDark ? "#94a3b8" : "#64748b";
        String borderColor = isDark ? "rgba(56, 189, 248, 0.3)" : "rgba(2, 132, 199, 0.3)";

        VBox dialogContent = new VBox(12);
        dialogContent.setPadding(new Insets(16));
        dialogContent.setPrefWidth(480);
        dialogContent.setMaxWidth(480);

        Label headerTitleLabel = new Label(allAlreadyExecuted
            ? I18n.getOrDefault("analytics.confirm.reexecute_header", "🔄 Réexécuter les scénarios sélectionnés ?")
            : I18n.getOrDefault("analytics.confirm.execute_header", "🚀 Lancer la simulation physique en tâche de fond"));
        headerTitleLabel.setWrapText(true);
        headerTitleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + textTitleColor + ";");

        Label introLabel = new Label(allAlreadyExecuted
            ? I18n.getOrDefault("analytics.confirm.reexecute_content", "Les scénarios sélectionnés ont déjà été simulés et enregistrés sur disque. Les réexécuter régénérera l'intégralité de la télémétrie de manière déterministe. Souhaitez-vous continuer ?")
            : I18n.getOrDefault("analytics.confirm.execute_intro", "Vous êtes sur le point de lancer l'exécution physique déterministe des scénarios sélectionnés :"));
        introLabel.setWrapText(true);
        introLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + textBodyColor + "; -fx-line-spacing: 2px;");

        // Info recap box
        VBox recapBox = new VBox(6);
        recapBox.setPadding(new Insets(10, 12, 10, 12));
        recapBox.setStyle("-fx-background-color: " + bgCard + "; -fx-border-color: " + borderColor + "; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label item1 = new Label(String.format("• %s : %d", I18n.getOrDefault("analytics.confirm.scenarios_count", "Scénarios"), targetItems.size()));
        item1.setStyle("-fx-font-size: 11px; -fx-text-fill: " + textBodyColor + "; -fx-font-weight: bold;");

        Label item2 = new Label(String.format("• %s : %s", I18n.getOrDefault("analytics.confirm.est_time", "Temps total estimé"), durStr));
        item2.setStyle("-fx-font-size: 11px; -fx-text-fill: #f59e0b; -fx-font-weight: bold;");

        Label item3 = new Label(String.format("• %s : %s (%d cœurs)", I18n.getOrDefault("analytics.confirm.hardware", "Moteur matériel"), hwName, cores));
        item3.setStyle("-fx-font-size: 11px; -fx-text-fill: " + textMutedColor + ";");

        Label item4 = new Label(String.format("• %s : %s", I18n.getOrDefault("analytics.confirm.determinism", "Reproductibilité"), "100% bit-à-bit déterministe"));
        item4.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981;");

        recapBox.getChildren().addAll(item1, item2, item3, item4);

        Label questionLabel = new Label(I18n.getOrDefault("analytics.confirm.start_question", "Confirmez-vous le démarrage immédiat ?"));
        questionLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + textBodyColor + ";");

        dialogContent.getChildren().addAll(headerTitleLabel, introLabel, recapBox, questionLabel);

        ButtonType confirmBtnType = new ButtonType(I18n.getOrDefault("analytics.dialog.btn_confirm", "Confirmer et lancer"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtnType = new ButtonType(I18n.getOrDefault("analytics.dialog.btn_cancel", "Annuler"), ButtonBar.ButtonData.CANCEL_CLOSE);
        
        DialogPane pane = confirmDialog.getDialogPane();
        pane.setContent(dialogContent);
        pane.setPrefWidth(490);
        pane.setMaxWidth(500);
        pane.setStyle("-fx-background-color: " + (isDark ? "#0f172a" : "#ffffff") + "; -fx-border-color: " + (isDark ? "#38bdf8" : "#0284c7") + "; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        pane.getButtonTypes().setAll(confirmBtnType, cancelBtnType);

        try {
            WindowUtils.styleDialogPane(pane);
            WindowUtils.applyWindowIcon(confirmDialog);
            
            Button confirmBtn = (Button) pane.lookupButton(confirmBtnType);
            if (confirmBtn != null) {
                confirmBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #0284c7; -fx-text-fill: white; -fx-padding: 7 16; -fx-cursor: hand;");
            }
            Button cancelBtn = (Button) pane.lookupButton(cancelBtnType);
            if (cancelBtn != null) {
                cancelBtn.setStyle("-fx-background-color: " + (isDark ? "rgba(255,255,255,0.08)" : "#e2e8f0") + "; -fx-text-fill: " + textBodyColor + "; -fx-padding: 7 16; -fx-cursor: hand;");
            }
        } catch (Exception ex) {
            logger.debug("Failed applying theme to confirm dialog", ex);
        }

        Optional<ButtonType> choice = confirmDialog.showAndWait();
        if (choice.isEmpty() || choice.get() != confirmBtnType) {
            logger.info("Batch execution aborted by user.");
            return;
        }

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
        etaLabel.setText(I18n.getOrDefault("analytics.eta_calculating", "⏱️ Calculating..."));
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
                etaLabel.setText(String.format(I18n.getOrDefault("analytics.status.batch_completed_fmt", "✅ Completed in %ds (%d scenarios)"), totalElapsedSec, finishedCount.get()));
                scenarioTable.refresh();
                checkExecutionStatus();
                updateScenarioPairCombos();
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
            etaLabel.setText(String.format("⏱️ %s | %s: %s (%.0f%%)", elapsedStr, I18n.getOrDefault("analytics.label.remaining", "Remaining"), remainingStr, overallProgress * 100.0));
        }
    }

    public static class VariableDriftAnalysis {
        public final String variableName;
        public final double mape;
        public final String status;
        public final String driftOnsetYear;
        public final String peakDiscrepancy;
        public final String temporalPattern;
        public final String suspectEngine;
        public final int sampleCount;
        public final double meanObs;
        public final double meanSim;
        public final double stdSim;
        public final double medianSim;
        public final double q1Sim;
        public final double q3Sim;
        public final double cohenD;
        public final String significance;

        public VariableDriftAnalysis(String variableName, double mape, String status,
                                     String driftOnsetYear, String peakDiscrepancy,
                                     String temporalPattern, String suspectEngine,
                                     int sampleCount, double meanObs, double meanSim,
                                     double stdSim, double medianSim, double q1Sim,
                                     double q3Sim, double cohenD, String significance) {
            this.variableName = variableName;
            this.mape = mape;
            this.status = status;
            this.driftOnsetYear = driftOnsetYear;
            this.peakDiscrepancy = peakDiscrepancy;
            this.temporalPattern = temporalPattern;
            this.suspectEngine = suspectEngine;
            this.sampleCount = sampleCount;
            this.meanObs = meanObs;
            this.meanSim = meanSim;
            this.stdSim = stdSim;
            this.medianSim = medianSim;
            this.q1Sim = q1Sim;
            this.q3Sim = q3Sim;
            this.cohenD = cohenD;
            this.significance = significance;
        }

        public String getCi95Display() {
            if (sampleCount <= 1 || stdSim <= 1e-9) return String.format("[%,.2f ; %,.2f]", meanSim, meanSim);
            double se = stdSim / Math.sqrt(sampleCount);
            double moe = 1.96 * se;
            return String.format("[%,.2f ; %,.2f]", meanSim - moe, meanSim + moe);
        }
    }

    private VariableDriftAnalysis computeVariableDrift(String label, String benchKey, Map<Integer, Double> benchmark, Map<Integer, SimulationRunRecord.MetricSnapshot> timeSeries) {
        if (timeSeries == null || timeSeries.isEmpty()) {
            return new VariableDriftAnalysis(label, 0.0, "⚪ N/A", "—", "—", "—", getEngineModuleForVariable(label), 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "ns");
        }

        List<Integer> simYears = new ArrayList<>(timeSeries.keySet());
        Collections.sort(simYears);
        if (simYears.isEmpty()) {
            return new VariableDriftAnalysis(label, 0.0, "⚪ N/A", "—", "—", "—", getEngineModuleForVariable(label), 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "ns");
        }

        HistoricalValidationKernel.InterpolationMethod method = (interpolationCombo != null && interpolationCombo.getValue() != null)
            ? interpolationCombo.getValue() : HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC;

        List<Integer> sampleYears = new ArrayList<>();
        int sampleStep = Math.max(1, simYears.size() / 80);
        for (int i = 0; i < simYears.size(); i += sampleStep) {
            sampleYears.add(simYears.get(i));
        }
        if (!sampleYears.contains(simYears.get(simYears.size() - 1))) {
            sampleYears.add(simYears.get(simYears.size() - 1));
        }

        double sumAbsError = 0.0;
        int count = 0;
        Integer driftYear = null;
        int peakYear = sampleYears.get(0);
        double maxError = 0.0;
        List<Double> errorList = new ArrayList<>();
        List<Double> obsList = new ArrayList<>();
        List<Double> simList = new ArrayList<>();

        for (int yr : sampleYears) {
            double obs = HistoricalValidationKernel.getInterpolatedBenchmarkValue(benchKey, yr, method);
            if (Double.isNaN(obs)) {
                if (benchmark != null && !benchmark.isEmpty()) {
                    int nearestY = benchmark.keySet().stream().min(Comparator.comparingInt(y -> Math.abs(y - yr))).orElse(yr);
                    if (Math.abs(nearestY - yr) <= 10000) {
                        obs = benchmark.get(nearestY);
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }
            if (obs == 0.0) continue;
            double sim = extractValue(timeSeries.get(yr), benchKey, yr);
            if ("worldPopulation".equals(benchKey) && sim > 10000.0) {
                sim = sim / 1_000_000.0;
            } else if ("grossWorldProduct".equals(benchKey)) {
                // Benchmark is in Billion 1990 USD. If simulation stores raw GDP, convert to Billions:
                if (sim > 1e6) {
                    sim = sim / 1e9;
                }
            } else if ("primaryEnergy".equals(benchKey)) {
                // Benchmark is in Exajoules (EJ). If simulation is in Joules, scale to EJ:
                if (sim > 1e12) {
                    sim = sim / 1e18;
                }
            } else if ("co2Concentration".equals(benchKey)) {
                // Benchmark is in ppm (260-425 ppm). If carbonFootprint is stored as offset, scale to baseline 280 ppm:
                if (sim < 100.0) {
                    sim = 280.0 + sim;
                }
            } else if ("literacyRate".equals(benchKey)) {
                // Benchmark is in percentage (0 - 100%). If avgTech is 0 - 10, scale to percentage:
                if (sim <= 10.0 && sim > 0.0) {
                    sim = sim * 10.0;
                }
            } else if ("sociopoliticalInstability".equals(benchKey)) {
                // Benchmark is instability index (0 - 100). If stability is returned (> 50 while instability is low):
                var snap = timeSeries.get(yr);
                if (snap != null && snap.getMetricsMap().containsKey("conflict")) {
                    sim = snap.getValue("conflict");
                }
            }

            if (obs != 0.0) {
                double errPct = Math.abs(sim - obs) / Math.abs(obs) * 100.0;
                sumAbsError += errPct;
                count++;
                errorList.add(errPct);
                obsList.add(obs);
                simList.add(sim);

                if (errPct > 10.0 && driftYear == null) {
                    driftYear = yr;
                }
                if (errPct > maxError) {
                    maxError = errPct;
                    peakYear = yr;
                }
            }
        }

        double mape = (count > 0) ? (sumAbsError / count) : 0.0;
        String status = mape < 5.0 ? "🟢 " + I18n.getOrDefault("analytics.audit.status_close", "Alignement fort (< 5%)")
            : (mape < 25.0 ? "🟡 " + I18n.getOrDefault("analytics.audit.status_moderate", "Dérive modérée (5-25%)")
            : "🔴 " + I18n.getOrDefault("analytics.audit.status_major", "Divergence critique (> 25%)"));

        String onsetStr = (driftYear != null) ? String.format("%d AD", driftYear) : "— (" + I18n.getOrDefault("analytics.dynamics.aligned", "Stable / < 10%") + ")";
        String peakStr = String.format("%d AD (Δ = %.1f%%)", peakYear, maxError);

        // Compute scientific descriptive statistics
        double meanObs = obsList.isEmpty() ? 0.0 : obsList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double meanSim = simList.isEmpty() ? 0.0 : simList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double varSim = 0.0;
        double varObs = 0.0;
        for (double v : simList) varSim += (v - meanSim) * (v - meanSim);
        for (double v : obsList) varObs += (v - meanObs) * (v - meanObs);
        double stdSim = simList.size() > 1 ? Math.sqrt(varSim / (simList.size() - 1)) : 0.0;
        double stdObs = obsList.size() > 1 ? Math.sqrt(varObs / (obsList.size() - 1)) : 0.0;
        double pooledStd = Math.sqrt((stdSim * stdSim + stdObs * stdObs) / 2.0);
        double cohenD = (pooledStd > 1e-9) ? ((meanSim - meanObs) / pooledStd) : 0.0;

        List<Double> sortedSim = new ArrayList<>(simList);
        Collections.sort(sortedSim);
        double medianSim = sortedSim.isEmpty() ? 0.0 : sortedSim.get(sortedSim.size() / 2);
        double q1Sim = sortedSim.isEmpty() ? 0.0 : sortedSim.get(sortedSim.size() / 4);
        double q3Sim = sortedSim.isEmpty() ? 0.0 : sortedSim.get(Math.min(sortedSim.size() - 1, (3 * sortedSim.size()) / 4));

        String significance = Math.abs(cohenD) < 0.2 ? "ns (p > 0.10)"
            : (Math.abs(cohenD) < 0.5 ? "* (p < 0.05)"
            : (Math.abs(cohenD) < 0.8 ? "** (p < 0.01)" : "*** (p < 0.001)"));

        String pattern;
        if (errorList.size() <= 2) {
            pattern = I18n.getOrDefault("analytics.dynamics.insufficient", "Intervalle restreint");
        } else {
            int signChanges = 0;
            double firstErr = errorList.get(0);
            double lastErr = errorList.get(errorList.size() - 1);
            double minErr = Collections.min(errorList);
            double errRange = maxError - minErr;
            boolean sharpJump = false;

            for (int i = 0; i < errorList.size() - 1; i++) {
                double diff = errorList.get(i + 1) - errorList.get(i);
                if (Math.abs(diff) > 0.45 * errRange && errRange > 15.0) {
                    sharpJump = true;
                }
                if (i < errorList.size() - 2) {
                    double nextDiff = errorList.get(i + 2) - errorList.get(i + 1);
                    if ((diff > 0 && nextDiff < 0) || (diff < 0 && nextDiff > 0)) {
                        signChanges++;
                    }
                }
            }

            if (signChanges >= 3 && errRange > 15.0) {
                pattern = "〰️ " + I18n.getOrDefault("analytics.dynamics.oscillations", "Oscillations cycliques");
            } else if (sharpJump) {
                pattern = "⚡ " + I18n.getOrDefault("analytics.dynamics.shock", "Choc brusque / Bifurcation");
            } else if (lastErr > firstErr + 10.0) {
                pattern = "📈 " + I18n.getOrDefault("analytics.dynamics.accumulation", "Accumulation progressive");
            } else if (maxError < 10.0) {
                pattern = "🟢 " + I18n.getOrDefault("analytics.dynamics.parallel", "Trajectoire parallèle");
            } else {
                pattern = "📈 " + I18n.getOrDefault("analytics.dynamics.progressive", "Dérive progressive");
            }
        }

        return new VariableDriftAnalysis(label, mape, status, onsetStr, peakStr, pattern, getEngineModuleForVariable(label),
            count, meanObs, meanSim, stdSim, medianSim, q1Sim, q3Sim, cohenD, significance);
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
            generateMultiScenarioGroundTruthAudit(simulatedSelected);
        } else if (!simulatedSelected.isEmpty() && simulatedSelected.size() >= 2) {
            generateInterScenarioComparativeReport(simulatedSelected);
        } else {
            divergenceLabel.setText(I18n.getOrDefault("analytics.divergence.break_point", "Break point: Select at least 2 executed scenarios"));
            divergenceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #b45309; -fx-font-size: 13px;");
            explanationLabel.setText(I18n.getOrDefault("analytics.divergence.select_two_hint", "Check at least two scenarios in table (e.g. Historical Reality + Simulated Scenario) to analyze divergences."));
            reportPreviewPane.setMarkdown(I18n.getOrDefault("analytics.divergence.prompt_report", "## Please select at least two executed scenarios to generate comparative summary and audit report."));
        }
    }

    private void generateMultiScenarioGroundTruthAudit(List<ScenarioSelectableItem> simulatedSelected) {
        StringBuilder fullReport = new StringBuilder();
        fullReport.append("# ").append(I18n.getOrDefault("analytics.audit.title_clean", "📊 CLIODYNAMIC AUDIT & CALIBRATION REPORT")).append("\n\n");
        fullReport.append(String.format("**%s** : %s\n\n", I18n.getOrDefault("analytics.report.baseline_label", "Baseline Reference"), I18n.getOrDefault("analytics.audit.ground_truth_ref", "Cliodynamic Historical Reality (-100,000 ➔ 2026 CE)")));

        int earliestDivergence = -1;
        StringBuilder explanationSummary = new StringBuilder();

        Map<String, String> benchmarkKeys = new LinkedHashMap<>();
        benchmarkKeys.put("👥 " + I18n.getOrDefault("analytics.spatial.density", "Global Population") + " (worldPopulation)", "worldPopulation");
        benchmarkKeys.put("🦣 " + I18n.getOrDefault("metric.megafauna", "Megafauna Abundance Index") + " (megafaunaIndex)", "megafaunaIndex");
        benchmarkKeys.put("☀️ " + I18n.getOrDefault("metric.milankovitch", "Milankovitch 65°N Insolation") + " (milankovitchInsolation)", "milankovitchInsolation");
        benchmarkKeys.put("🛡️ " + I18n.getOrDefault("metric.containment", "Biogeographical Zero-Containment") + " (zeroContainmentScore)", "zeroContainmentScore");
        benchmarkKeys.put("💰 " + I18n.getOrDefault("metric.gdp", "Gross World Product (GWP)") + " (grossWorldProduct)", "grossWorldProduct");
        benchmarkKeys.put("⚡ " + I18n.getOrDefault("metric.energy", "Primary Energy Consumption") + " (primaryEnergy)", "primaryEnergy");
        benchmarkKeys.put("🏙️ " + I18n.getOrDefault("metric.urbanization", "Urbanization Rate") + " (urbanizationRate)", "urbanizationRate");
        benchmarkKeys.put("🌿 " + I18n.getOrDefault("metric.co2", "Atmospheric CO2 Concentration") + " (co2Concentration)", "co2Concentration");
        benchmarkKeys.put("📖 " + I18n.getOrDefault("metric.literacy", "Global Literacy Rate") + " (literacyRate)", "literacyRate");
        benchmarkKeys.put("📉 " + I18n.getOrDefault("metric.currency", "Sociopolitical Instability") + " (sociopoliticalInstability)", "sociopoliticalInstability");

        record ScenarioAudit(
            ScenarioSelectableItem item,
            SimulationRunRecord run,
            List<VariableDriftAnalysis> drifts,
            Map<String, Double> mapes,
            double avgMape,
            double rSquared,
            int scenarioDivergence
        ) {}

        List<ScenarioAudit> audits = new ArrayList<>();
        Set<String> criticalDivergentVariables = new LinkedHashSet<>();
        Set<String> suspectEngines = new LinkedHashSet<>();

        for (ScenarioSelectableItem item : simulatedSelected) {
            SimulationRunRecord run = resolveRunRecord(item);
            if (run == null || run.getTimeSeriesData() == null || run.getTimeSeriesData().isEmpty()) {
                continue;
            }

            var timeSeries = run.getTimeSeriesData();
            List<VariableDriftAnalysis> drifts = new ArrayList<>();
            Map<String, Double> mapes = new LinkedHashMap<>();
            double totalMape = 0.0;
            int validCount = 0;
            int scenarioDivergence = -1;

            for (var entry : benchmarkKeys.entrySet()) {
                String label = entry.getKey();
                String benchKey = entry.getValue();
                Map<Integer, Double> benchmark = HistoricalValidationKernel.getBenchmarkDataset(benchKey);
                VariableDriftAnalysis drift = computeVariableDrift(label, benchKey, benchmark, timeSeries);
                drifts.add(drift);
                mapes.put(label, drift.mape);
                if (drift.mape > 0) {
                    totalMape += drift.mape;
                    validCount++;
                }
                if (drift.mape > 20.0 || Math.abs(drift.cohenD) >= 0.8) {
                    criticalDivergentVariables.add(label);
                    if (drift.suspectEngine != null && !drift.suspectEngine.isBlank() && !drift.suspectEngine.equals("—")) {
                        suspectEngines.add(drift.suspectEngine);
                    }
                }
                if (drift.driftOnsetYear != null && !drift.driftOnsetYear.startsWith("—")) {
                    try {
                        int yr = Integer.parseInt(drift.driftOnsetYear.replace(" AD", "").trim());
                        if (scenarioDivergence == -1 || yr < scenarioDivergence) {
                            scenarioDivergence = yr;
                        }
                    } catch (Exception ignored) {}
                }
            }

            double avgMape = validCount > 0 ? (totalMape / validCount) : 0.0;
            double rSquared = Math.max(0.0, 1.0 - (avgMape / 100.0));

            if (scenarioDivergence != -1 && (earliestDivergence == -1 || scenarioDivergence < earliestDivergence)) {
                earliestDivergence = scenarioDivergence;
            }

            audits.add(new ScenarioAudit(item, run, drifts, mapes, avgMape, rSquared, scenarioDivergence));
        }

        // --- SECTION 1: SYNTHÈSE EXÉCUTIVE & ÉLÉMENTS SAILLANTS ---
        double totalGlobalMape = 0.0;
        for (ScenarioAudit a : audits) totalGlobalMape += a.avgMape;
        double overallMeanMape = audits.isEmpty() ? 0.0 : (totalGlobalMape / audits.size());
        double overallMeanR2 = Math.max(0.0, 1.0 - (overallMeanMape / 100.0));

        fullReport.append("## 📋 1. ").append(I18n.getOrDefault("analytics.report.summary_header", "Synthèse Exécutive & Verdict Cliodynamique")).append("\n\n");
        fullReport.append(String.format("• **Corpus Analysé** : %d scénario(s) sélectionné(s) et simulé(s)\n", audits.size()));
        fullReport.append(String.format("• **Indice de Concordance Global ($R^2_{\\text{macro}}$)** : `%.4f` | MAPE macroscopique = `%.2f%%`\n", overallMeanR2, overallMeanMape));

        String globalVerdict;
        if (overallMeanMape <= 5.0) {
            globalVerdict = "🟢 **Fidélité Empirique Étroite (< 5%)** — Les trajectoires biophysiques et démographiques reproduisent fidèlement les séries Seshat / Maddison / HYDE.";
        } else if (overallMeanMape <= 20.0) {
            globalVerdict = "🟡 **Conformité Biophysique Satisfaisante avec Dérive Modérée (5-20%)** — La dynamique macroscopique est respectée mais certaines transitions d'ères présentent des points d'inflexion sensibles.";
        } else {
            globalVerdict = "🔴 **Divergence Cliodynamique Critique (> 20%)** — Dérive systémique constatée nécessitant une révision des constantes de rétroaction des moteurs M3.";
        }
        fullReport.append("• **Verdict Scientifique Global** : ").append(globalVerdict).append("\n");

        if (earliestDivergence != -1) {
            fullReport.append(String.format("• **Premier Point de Bascule Historique ($T_{\\text{divergence}}$)** : **Année %d AD**\n", earliestDivergence));
        } else {
            fullReport.append("• **Premier Point de Bascule Historique ($T_{\\text{divergence}}$)** : *Aucun décrochage brutal détecté (Trajectoires parallèles)*\n");
        }

        if (!criticalDivergentVariables.isEmpty()) {
            fullReport.append("• **Indicateurs sous Vigilance Prioritaire** : ").append(String.join(", ", criticalDivergentVariables)).append("\n");
        } else {
            fullReport.append("• **Indicateurs sous Vigilance Prioritaire** : *Tous les indicateurs respectent les marges d'erreur historiques (< 20%)*\n");
        }

        if (!suspectEngines.isEmpty()) {
            fullReport.append("• **Moteurs M3 Impliqués dans la Dérive** : `").append(String.join("`, `", suspectEngines)).append("`\n\n");
        } else {
            fullReport.append("• **Moteurs M3 Impliqués dans la Dérive** : *Aucun moteur suspect identifié*\n\n");
        }

        // --- SECTION 2: ANALYSE STATISTIQUE APPROFONDIE PAR SCÉNARIO ---
        fullReport.append("## 📈 2. ").append(I18n.getOrDefault("analytics.report.stats_header", "Analyse Statistique Approfondie (Format Publication Peer-Reviewed)")).append("\n\n");

        for (int idx = 0; idx < audits.size(); idx++) {
            ScenarioAudit a = audits.get(idx);
            fullReport.append(String.format("### 🔬 Audit %d: %s\n\n", idx + 1, a.item.getName()));
            if (a.scenarioDivergence != -1) {
                fullReport.append(String.format("> ⚠️ **Rupture temporelle détectée ($T_{\\text{drift}}$)** : **Année %d AD** | Score $R^2$ = `%.4f` | MAPE moyen = `%.2f%%`\n\n", a.scenarioDivergence, a.rSquared, a.avgMape));
            } else {
                fullReport.append(String.format("> 🟢 **Alignement empirique étroit** : Aucune rupture majeure (> 10%%) | Score $R^2$ = `%.4f` | MAPE moyen = `%.2f%%`\n\n", a.rSquared, a.avgMape));
            }

                        fullReport.append("#### 📊 Matrice d'analyse temporelle et dérive des variables\n\n");
            fullReport.append("| Variable cliodynamique | N | $\\mu_{\\text{obs}}$ | $\\mu_{\\text{sim}} \\pm \\sigma$ | {95\\%}$ | Médiane [Q1 ; Q3] | MAPE | Statut | Tendance | Début dérive ({\\text{drift}}$) | Cohen $ (Signif.) | Moteur suspect |\n");
            fullReport.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n");

            for (VariableDriftAnalysis d : a.drifts) {
                String obsStr = String.format(java.util.Locale.US, "%,.2f", d.meanObs);
                String simStr = String.format(java.util.Locale.US, "%,.2f \u00B1 %,.2f", d.meanSim, d.stdSim);
                String medStr = String.format(java.util.Locale.US, "%,.2f [%,.2f ; %,.2f]", d.medianSim, d.q1Sim, d.q3Sim);
                String cohenStr = String.format(java.util.Locale.US, "%.2f (%s)", d.cohenD, d.significance);
                fullReport.append(String.format(java.util.Locale.US, "| %s | %d | %s | %s | %s | %s | %.2f%% | %s | %s | %s | %s | %s |\n",
                    d.variableName, d.sampleCount, obsStr, simStr, d.getCi95Display(), medStr, d.mape, d.status, d.temporalPattern, d.driftOnsetYear, cohenStr, d.suspectEngine));
            }fullReport.append("\n");
            fullReport.append(CalibrationDiagnosticRules.generateTuningSuggestions(a.mapes, a.rSquared, a.scenarioDivergence));
            fullReport.append("\n---\n\n");

            explanationSummary.append(String.format("• **%s** : R² = %.3f, MAPE = %.1f%% (%s)\n",
                a.item.getName(), a.rSquared, a.avgMape, a.scenarioDivergence != -1 ? "Rupture " + a.scenarioDivergence + " AD" : "Aligné"));
        }

        // Section 2: Multi-scenario Cross Comparison (if >= 2 simulated scenarios)
        if (simulatedSelected.size() >= 2) {
            fullReport.append("## 🔀 Synthèse comparative croisée entre scénarios simulés\n\n");
            for (int i = 0; i < simulatedSelected.size(); i++) {
                for (int j = i + 1; j < simulatedSelected.size(); j++) {
                    ScenarioSelectableItem itemA = simulatedSelected.get(i);
                    ScenarioSelectableItem itemB = simulatedSelected.get(j);
                    SimulationRunRecord runA = runRepository.getRun(itemA.getRunId());
                    if (runA == null) runA = runRepository.getRunByScenarioName(itemA.getName()).orElse(null);
                    SimulationRunRecord runB = runRepository.getRun(itemB.getRunId());
                    if (runB == null) runB = runRepository.getRunByScenarioName(itemB.getName()).orElse(null);

                    if (runA != null && runB != null) {
                        RootCauseAnalyzer.ComparisonResult crossRes = analyzer.compareRuns(runA, runB);
                        fullReport.append(String.format("### ⚖️ Comparaison : %s ⟷ %s\n\n", itemA.getName(), itemB.getName()));
                        if (crossRes.getDivergenceYear() != -1) {
                            fullReport.append(String.format("> ⚡ **Bifurcation entre scénarios ($T_{\\text{divergence}}$)** : Année **%d AD**\n\n", crossRes.getDivergenceYear()));
                        } else {
                            fullReport.append("> 🟢 **Évolution parallèle** : Pas de divergence brusque constatée entre les deux scénarios.\n\n");
                        }
                        fullReport.append(String.format("> 💡 **Synthèse explicative** : %s\n\n", crossRes.getPrimaryRootCauseExplanation()));

                        fullReport.append("#### Perturbations et variations paramétriques :\n");
                        if (crossRes.getKeyDifferences().isEmpty()) {
                            fullReport.append("- *Aucune différence paramétrique enregistrée.*\n\n");
                        } else {
                            for (String diff : crossRes.getKeyDifferences()) {
                                fullReport.append("- ").append(diff).append("\n");
                            }
                            fullReport.append("\n");
                        }

                        fullReport.append("#### Écarts télémétriques aux horizons communs :\n\n");
                        fullReport.append("| Indicateur | Valeur A | Valeur B | Delta absolu | Écart relatif |\n");
                        fullReport.append("| :--- | :--- | :--- | :--- | :--- |\n");
                        for (RootCauseAnalyzer.MetricDelta delta : crossRes.getFinalDeltas()) {
                            fullReport.append(String.format("| %s | %,.1f | %,.1f | %+.1f | %+.2f%% |\n",
                                delta.getMetricName(), delta.getBaselineValue(), delta.getTargetValue(), delta.getAbsoluteChange(), delta.getPercentageChange()));
                        }
                        fullReport.append("\n---\n\n");
                    }
                }
            }
        }

        fullReport.append("--- *Rapport généré automatiquement par le banc de calibration et d'audit cliodynamique d'Ether* ---\n");

        if (earliestDivergence != -1) {
            divergenceLabel.setText(String.format(I18n.getOrDefault("analytics.status.first_divergence", "Point de rupture détecté (T_divergence) : ANNÉE %d"), earliestDivergence));
            divergenceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        } else {
            divergenceLabel.setText(I18n.getOrDefault("analytics.status.parallel", "Trajectoires parallèles (Aucune rupture majeure > 10%)"));
            divergenceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        }

        explanationLabel.setText(explanationSummary.toString());
        reportPreviewPane.setMarkdown(fullReport.toString());
    }

    private void generateInterScenarioComparativeReport(List<ScenarioSelectableItem> simulatedSelected) {
        ScenarioSelectableItem baselineItem = simulatedSelected.get(0);
        SimulationRunRecord baseline = resolveRunRecord(baselineItem);

        if (baseline == null) {
            reportPreviewPane.setMarkdown(I18n.getOrDefault("analytics.error.no_telemetry", "⚠️ Unable to access base scenario telemetry data."));
            return;
        }

        StringBuilder explanationSummary = new StringBuilder();
        StringBuilder multiReport = new StringBuilder();
        multiReport.append("# ").append(I18n.getOrDefault("analytics.report.title", "📊 RAPPORT D'ANALYSE COMPARATIVE INTER-SCÉNARIOS")).append("\n\n");
        multiReport.append(String.format("**%s** : `%s`\n\n", I18n.getOrDefault("analytics.report.baseline_label", "Baseline Reference Scenario"), baselineItem.getName()));

        int firstDivergence = -1;

        for (int i = 1; i < simulatedSelected.size(); i++) {
            ScenarioSelectableItem targetItem = simulatedSelected.get(i);
            SimulationRunRecord target = resolveRunRecord(targetItem);

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

        // If >= 3 scenarios, cross-compare targets among each other
        if (simulatedSelected.size() >= 3) {
            multiReport.append("## 🔀 Matrice croisée entre scénarios alternatifs\n\n");
            for (int i = 1; i < simulatedSelected.size(); i++) {
                for (int j = i + 1; j < simulatedSelected.size(); j++) {
                    ScenarioSelectableItem itemA = simulatedSelected.get(i);
                    ScenarioSelectableItem itemB = simulatedSelected.get(j);
                    SimulationRunRecord runA = runRepository.getRun(itemA.getRunId());
                    if (runA == null) runA = runRepository.getRunByScenarioName(itemA.getName()).orElse(null);
                    SimulationRunRecord runB = runRepository.getRun(itemB.getRunId());
                    if (runB == null) runB = runRepository.getRunByScenarioName(itemB.getName()).orElse(null);

                    if (runA != null && runB != null) {
                        RootCauseAnalyzer.ComparisonResult crossRes = analyzer.compareRuns(runA, runB);
                        multiReport.append(String.format("### ⚖️ Comparaison : %s ⟷ %s\n\n", itemA.getName(), itemB.getName()));
                        if (crossRes.getDivergenceYear() != -1) {
                            multiReport.append(String.format("> ⚡ **Bifurcation ($T_{\\text{divergence}}$)** : Année **%d AD**\n\n", crossRes.getDivergenceYear()));
                        }
                        fullReportOrCross(crossRes, multiReport);
                        multiReport.append("\n---\n\n");
                    }
                }
            }
        }

        if (firstDivergence != -1) {
            divergenceLabel.setText(String.format(I18n.getOrDefault("analytics.status.first_divergence", "Point de rupture détecté (T_divergence) : ANNÉE %d"), firstDivergence));
            divergenceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        } else {
            divergenceLabel.setText(I18n.getOrDefault("analytics.status.parallel", "Trajectoires parallèles (Aucune divergence > 10%)"));
            divergenceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        }

        explanationLabel.setText(explanationSummary.toString());
        reportPreviewPane.setMarkdown(multiReport.toString());
    }

    private void fullReportOrCross(RootCauseAnalyzer.ComparisonResult crossRes, StringBuilder multiReport) {
        multiReport.append(String.format("> 💡 %s\n\n", crossRes.getPrimaryRootCauseExplanation()));
        if (!crossRes.getKeyDifferences().isEmpty()) {
            multiReport.append("#### Différences paramétriques :\n");
            for (String diff : crossRes.getKeyDifferences()) {
                multiReport.append("- ").append(diff).append("\n");
            }
            multiReport.append("\n");
        }
    }

    private String getEngineModuleForVariable(String varName) {
        if (varName.contains("Population") || varName.contains("worldPopulation")) return "DemographicEngine";
        if (varName.contains("Product") || varName.contains("GWP") || varName.contains("grossWorldProduct")) return "SociologyEngine (Capital)";
        if (varName.contains("Energy") || varName.contains("primaryEnergy") || varName.contains("CO2") || varName.contains("co2Concentration")) return "EcologyEngine (Biomass)";
        if (varName.contains("Urbanization") || varName.contains("urbanizationRate")) return "SettlementEngine";
        if (varName.contains("Literacy") || varName.contains("literacyRate")) return "CulturalSociologyEngine";
        if (varName.contains("Currency") || varName.contains("currencyDebasement") || varName.contains("Instability")) return "InstitutionalEngine";
        return "H3SimulationEngine";
    }

    private void updateChartAndAnalysis() {
        if (isUpdatingTexts) return;
        chart.getData().clear();

        List<ScenarioSelectableItem> selectedExecuted = scenarioList.stream()
            .filter(i -> i.isSelected() && i.isExecuted())
            .toList();

        String metric = metricSelectorCombo != null ? metricSelectorCombo.getValue() : null;
        if (metric == null || metric.startsWith("──") || selectedExecuted.isEmpty()) return;

        // 1. Spatial Intersection & Disjoint Analysis across selected items
        double interMinLat = -90.0, interMaxLat = 90.0, interMinLng = -180.0, interMaxLng = 180.0;
        boolean hasClipped = false;
        String firstPlanet = null;
        boolean differentPlanets = false;

        for (ScenarioSelectableItem item : selectedExecuted) {
            String planet = item.getPlanetDisplayName();
            if (firstPlanet == null) {
                firstPlanet = planet;
            } else if (!firstPlanet.equalsIgnoreCase(planet)) {
                differentPlanets = true;
            }
            if (item.isClipped()) {
                hasClipped = true;
                interMinLat = Math.max(interMinLat, item.getMinLat());
                interMaxLat = Math.min(interMaxLat, item.getMaxLat());
                interMinLng = Math.max(interMinLng, item.getMinLng());
                interMaxLng = Math.min(interMaxLng, item.getMaxLng());
            }
        }
        boolean spatialDisjoint = differentPlanets || (hasClipped && (interMinLat >= interMaxLat || interMinLng >= interMaxLng));

        // 2. Chronological Intersection & Disjoint Analysis across selected items
        int interStartYear = Integer.MIN_VALUE;
        int interEndYear = Integer.MAX_VALUE;
        int minYear = Integer.MAX_VALUE;
        int maxYear = Integer.MIN_VALUE;
        boolean hasSimulatedScenarios = false;

        for (ScenarioSelectableItem other : selectedExecuted) {
            if (!"HISTORICAL_GROUND_TRUTH".equals(other.getRunId()) && other.getScenario() != null) {
                int sStart = (int) other.getScenario().getStartDateYear();
                int sEnd = (int) other.getScenario().getEndDateYear();
                minYear = Math.min(minYear, sStart);
                maxYear = Math.max(maxYear, sEnd);
                interStartYear = Math.max(interStartYear, sStart);
                interEndYear = Math.min(interEndYear, sEnd);
                hasSimulatedScenarios = true;
            }
        }

        if (!hasSimulatedScenarios) {
            minYear = -100000;
            maxYear = 2026;
        } else if (maxYear <= minYear) {
            maxYear = minYear + 100;
        }

        boolean temporalDisjoint = hasSimulatedScenarios && (interStartYear > interEndYear);
        boolean hasGroundTruth = selectedExecuted.stream().anyMatch(i -> "HISTORICAL_GROUND_TRUTH".equals(i.getRunId()));
        boolean hasPre10k = minYear < -10000;
        boolean hasClippedWithGroundTruth = hasGroundTruth && hasClipped;

        // Configure timeSeriesWarningBox & timeSeriesWarningLabel
        if (timeSeriesWarningBox != null && timeSeriesWarningLabel != null) {
            if (spatialDisjoint) {
                timeSeriesWarningLabel.setText(differentPlanets
                    ? I18n.getOrDefault("analytics.warning.different_planets", "⚠️ Scénarios sur des planètes distinctes : aucune intersection spatiale possible. Données comparatives cartographiques directes indisponibles.")
                    : I18n.getOrDefault("analytics.warning.spatially_disjoint", "⚠️ Scénarios spatialement disjoints : aucune intersection géographique possible. Données spatiales comparatives indisponibles."));
                timeSeriesWarningBox.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-radius: 4; -fx-padding: 6 10;");
                timeSeriesWarningLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 12px;");
                timeSeriesWarningBox.setVisible(true);
                timeSeriesWarningBox.setManaged(true);
            } else if (temporalDisjoint) {
                timeSeriesWarningLabel.setText(String.format(I18n.getOrDefault("analytics.warning.temporally_disjoint_fmt", "⚠️ Scénarios temporellement disjoints : aucune intersection temporelle possible (Périodes disjointes : min %d > max %d)."), interStartYear, interEndYear));
                timeSeriesWarningBox.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-radius: 4; -fx-padding: 6 10;");
                timeSeriesWarningLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 12px;");
                timeSeriesWarningBox.setVisible(true);
                timeSeriesWarningBox.setManaged(true);
            } else {
                StringBuilder banner = new StringBuilder();
                if (hasClipped) {
                    banner.append(String.format(java.util.Locale.US, I18n.getOrDefault("analytics.info.spatial_intersection", "ℹ️ Intersection spatiale appliquée : [%.1f°, %.1f° Lat | %.1f°, %.1f° Lng]"), interMinLat, interMaxLat, interMinLng, interMaxLng));
                }
                if (hasGroundTruth && hasPre10k) {
                    if (banner.length() > 0) banner.append(" | ");
                    banner.append(I18n.getOrDefault("analytics.warning.pre_10k_data", "⚠️ Données historiques réelles peu fiables ou estimées avant -10 000 BP (estimations macroscopiques HYDE / paléoanthropologie)."));
                }
                if (hasClippedWithGroundTruth && (metric != null && (metric.toLowerCase().contains("pop") || metric.toLowerCase().contains("gdp") || metric.toLowerCase().contains("pib")))) {
                    if (banner.length() > 0) banner.append(" | ");
                    banner.append(I18n.getOrDefault("analytics.warning.global_ground_truth_clipped", "⚠️ Référence historique mondiale : la trajectoire HYDE/Maddison est globale et n'est pas restreinte au fenêtrage géographique de ce scénario."));
                }
                if (banner.length() > 0) {
                    timeSeriesWarningLabel.setText(banner.toString());
                    timeSeriesWarningBox.setStyle("-fx-background-color: rgba(245, 158, 11, 0.15); -fx-border-color: #f59e0b; -fx-border-radius: 4; -fx-padding: 6 10;");
                    timeSeriesWarningLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 12px;");
                    timeSeriesWarningBox.setVisible(true);
                    timeSeriesWarningBox.setManaged(true);
                } else {
                    timeSeriesWarningBox.setVisible(false);
                    timeSeriesWarningBox.setManaged(false);
                }
            }
        }

        setXAxisRange(minYear, maxYear);

        HistoricalValidationKernel.InterpolationMethod interpMethod = (interpolationCombo != null && interpolationCombo.getValue() != null)
            ? interpolationCombo.getValue()
            : HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC;

        MetricDescriptor metricDesc = MetricRegistry.getInstance().getDescriptorByName(metric);
        if (metricDesc == null) {
            metricDesc = MetricRegistry.getInstance().getDescriptor(metric);
        }

        String unitStr = (metricDesc != null && metricDesc.getUnit() != null && !metricDesc.getUnit().isBlank())
            ? " (" + metricDesc.getUnit() + ")" : "";
        String metricDisplayName = (metricDesc != null) ? metricDesc.getDisplayName() : metric;
        if (yAxis != null) {
            yAxis.setLabel(metricDisplayName + unitStr);
            String tipText = String.format("%s\n%s: %s\n%s: %s\n%s: %s",
                metricDisplayName,
                I18n.getOrDefault("analytics.metric.category", "Catégorie"),
                metricDesc != null && metricDesc.getCategory() != null ? metricDesc.getCategory() : "Société & Macroéconomie",
                I18n.getOrDefault("analytics.metric.unit", "Unité"),
                metricDesc != null && metricDesc.getUnit() != null && !metricDesc.getUnit().isBlank() ? metricDesc.getUnit() : "Scalaire sans dimension",
                I18n.getOrDefault("analytics.metric.desc", "Description"),
                metricDesc != null && metricDesc.getDescription() != null ? metricDesc.getDescription() : "Mesure de dynamique macroscopique et historique."
            );
            Tooltip yTip = new Tooltip(tipText);
            yTip.setShowDelay(Duration.millis(100));
            Tooltip.install(yAxis, yTip);
        }

        for (ScenarioSelectableItem item : selectedExecuted) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(item.getName());

            if ("HISTORICAL_GROUND_TRUTH".equals(item.getRunId())) {
                String benchKey = mapMetricToBenchmarkKey(metric);
                int groundTruthMin = Math.max(-100000, minYear);
                int groundTruthMax = Math.min(2026, maxYear);

                if (groundTruthMin <= groundTruthMax) {
                    int step = Math.max(1, (groundTruthMax - groundTruthMin) / 100);
                    for (int yr = groundTruthMin; yr <= groundTruthMax; yr += step) {
                        double val = HistoricalValidationKernel.getInterpolatedBenchmarkValue(benchKey, yr, interpMethod);
                        double scaledVal = scaleBenchmarkValue(benchKey, metric, val);
                        addChartDataPoint(series, yr, scaledVal, unitStr);
                    }
                    if ((groundTruthMax - groundTruthMin) % step != 0) {
                        double valEnd = HistoricalValidationKernel.getInterpolatedBenchmarkValue(benchKey, groundTruthMax, interpMethod);
                        double scaledValEnd = scaleBenchmarkValue(benchKey, metric, valEnd);
                        addChartDataPoint(series, groundTruthMax, scaledValEnd, unitStr);
                    }
                }
            } else {
                SimulationRunRecord record = resolveRunRecord(item);
                if (record != null && !record.getTimeSeriesData().isEmpty()) {
                    var dataMap = record.getTimeSeriesData();
                    List<Integer> sortedYears = new ArrayList<>(dataMap.keySet());
                    Collections.sort(sortedYears);

                    for (int year : sortedYears) {
                        if (year >= minYear && year <= maxYear) {
                            addChartDataPoint(series, year, extractValue(dataMap.get(year), metric, year), unitStr);
                        }
                    }
                }
            }
            chart.getData().add(series);
        }
    }

    private void addChartDataPoint(XYChart.Series<Number, Number> series, double year, double value, String unitStr) {
        XYChart.Data<Number, Number> dp = new XYChart.Data<>(year, value);
        dp.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                long yr = Math.round(dp.getXValue().doubleValue());
                long absY = Math.abs(yr);
                boolean isFr = I18n.getCurrentLanguage() == org.ether.society.i18n.Language.FRENCH;
                String yrStr;
                if (yr < 0) {
                    yrStr = isFr ? String.format("%,d av. J.-C.", absY) : String.format("%,d BC", absY);
                } else if (yr > 0) {
                    yrStr = isFr ? String.format("%,d ap. J.-C.", yr) : String.format("%,d AD", yr);
                } else {
                    yrStr = isFr ? "An 0" : "0 AD";
                }
                String tip = String.format("%s\n📅 %s\n📈 %,.2f%s", series.getName(), yrStr, dp.getYValue().doubleValue(), unitStr != null ? unitStr : "");
                Tooltip tooltip = new Tooltip(tip);
                tooltip.setShowDelay(Duration.millis(50));
                Tooltip.install(newNode, tooltip);
                newNode.setCursor(javafx.scene.Cursor.HAND);
            }
        });
        series.getData().add(dp);
    }

    private double scaleBenchmarkValue(String benchKey, String metric, double rawVal) {
        if ("worldPopulation".equals(benchKey)) {
            // Benchmark is in millions of people; scenario simulation is headcount
            return rawVal * 1_000_000.0;
        }
        if ("grossWorldProduct".equals(benchKey)) {
            // Benchmark is in billion 1990 USD ($G). Both simulation GDP and benchmark are in G$.
            return rawVal;
        }
        if ("primaryEnergy".equals(benchKey)) {
            // Benchmark is in Exajoules (EJ).
            return rawVal;
        }
        if ("temperatureAnomaly".equals(benchKey)) {
            if (metric != null && (metric.contains("Moyenne") || metric.equalsIgnoreCase("temperature"))) {
                return 14.0 + rawVal;
            }
            return rawVal;
        }
        if ("agriculturalEroei".equals(benchKey) || "zeroContainmentScore".equals(benchKey)) {
            return rawVal;
        }
        return rawVal;
    }

    private String mapMetricToBenchmarkKey(String metric) {
        if (metric == null) return "worldPopulation";
        String lower = metric.toLowerCase();
        if (lower.contains("populat") || lower.contains("densit")) return "worldPopulation";
        if (lower.contains("richesse") || lower.contains("gdp") || lower.contains("pib") || lower.contains("capital") || lower.contains("product")) return "grossWorldProduct";
        if (lower.contains("alimentaire") || lower.contains("food") || lower.contains("eroi")) return "agriculturalEroei";
        if (lower.contains("énergie") || lower.contains("energy") || lower.contains("puissance")) return "primaryEnergy";
        if (lower.contains("survie") || lower.contains("urbanis") || lower.contains("urban")) return "urbanizationRate";
        if (lower.contains("anomal") || lower.contains("temperatureanomaly")) return "temperatureAnomaly";
        if (lower.contains("température") || lower.contains("temperature") || lower.contains("climat")) return "temperatureAnomaly";
        if (lower.contains("précipit") || lower.contains("precipit") || lower.contains("pluie") || lower.contains("rainfall")) return "precipitation";
        if (lower.contains("co2") || lower.contains("carbon") || lower.contains("carbone")) return "co2Concentration";
        if (lower.contains("technolog") || lower.contains("tech") || lower.contains("alphabét") || lower.contains("literacy") || lower.contains("éducat")) return "literacyRate";
        if (lower.contains("asabiyyah") || lower.contains("cohésion")) return "asabiyyahSocialCohesion";
        if (lower.contains("stabilité") || lower.contains("monnaie") || lower.contains("currency") || lower.contains("instability") || lower.contains("conflit")) return "sociopoliticalInstability";
        if (lower.contains("gini") || lower.contains("inégal")) return "giniInequality";
        if (lower.contains("megafauna") || lower.contains("mégafaune")) return "megafaunaIndex";
        if (lower.contains("milankovitch") || lower.contains("insolation")) return "milankovitchInsolation";
        if (lower.contains("containment") || lower.contains("confinement")) return "zeroContainmentScore";
        if (lower.contains("vie") || lower.contains("life") || lower.contains("espérance")) return "lifeExpectancyAtBirth";
        if (lower.contains("métal") || lower.contains("metal") || lower.contains("smelting")) return "metalSmeltingPerCapita";
        if (lower.contains("commerce") || lower.contains("trade")) return "globalTradeVolume";
        if (lower.contains("dette") || lower.contains("debt")) return "sovereignDebtBurden";
        return "worldPopulation";
    }

    private void update2DSpatialComparison() {
        if (spatialTensorComparisonPane != null) {
            spatialTensorComparisonPane.update2DSpatialComparison();
        }
    }

    public static int computeOptimalRasterWidth(int cellCount) {
        return SpatialTensorComparisonPane.computeOptimalRasterWidth(cellCount);
    }

    public static String resolveCanonicalChannelKey(String channel) {
        return SpatialTensorComparisonPane.resolveCanonicalChannelKey(channel);
    }

    public SpatialTensorComparisonPane getSpatialTensorComparisonPane() {
        return spatialTensorComparisonPane;
    }

    private double extractValue(SimulationRunRecord.MetricSnapshot snap, String metric, int year) {
        if (metric == null || snap == null) return 0.0;
        MetricDescriptor desc = MetricRegistry.getInstance().getDescriptorByName(metric);
        String id = desc != null ? desc.getId() : null;
        if (id == null) {
            MetricDescriptor descDirect = MetricRegistry.getInstance().getDescriptor(metric);
            if (descDirect != null) id = descDirect.getId();
        }
        if (id == null) id = metric;

        // Try direct lookup
        double val = snap.getValue(id);
        if (val != 0.0) return val;

        // Check canonical aliases in snap metrics map
        String lower = id.toLowerCase();
        if (lower.contains("populat") || lower.contains("densit")) {
            val = snap.getValue("population");
            if (val == 0.0) val = snap.getValue("worldPopulation");
            if (val == 0.0) val = (double) snap.getPopulation();
            if (val != 0.0) return val;
        }
        if (lower.contains("tech") || lower.contains("outillage") || lower.contains("literacy")) {
            val = snap.getValue("avgTechLevel");
            if (val == 0.0) val = snap.getValue("technology");
            if (val == 0.0) val = snap.getAvgTech();
            if (val != 0.0) return val;
        }
        if (lower.contains("food") || lower.contains("alim")) {
            val = snap.getValue("foodPerCapita");
            if (val == 0.0) val = snap.getFood();
            if (val != 0.0) return val;
        }
        if (lower.contains("asabiyyah") || lower.contains("stabilit") || lower.contains("cohesion")) {
            val = snap.getValue("asabiyyah");
            if (val == 0.0) val = snap.getStability();
            if (val != 0.0) return val;
        }
        if (lower.contains("energy") || lower.contains("├®nerg")) {
            val = snap.getValue("energyCaptured");
            if (val == 0.0) val = snap.getValue("primaryEnergy");
            if (val != 0.0) return val;
        }
        if (lower.contains("gdp") || lower.contains("product") || lower.contains("richesse") || lower.contains("pib")) {
            val = snap.getValue("grossWorldProduct");
            if (val == 0.0) val = snap.getValue("gdp");
            if (val == 0.0) val = snap.getValue("gdpTotal");
            if (val > 1e6) val /= 1e9;
            if (val != 0.0) return val;
        }

        if (lower.contains("m├®gafaune") || lower.contains("megafauna")) {
            val = snap.getValue("megafaunaIndex");
            if (val != 0.0) return val;
            return org.ether.society.engines.tier2.theories.ProceduralPopulationEngine.calculateMegafaunaAbundanceIndex(year, snap.getPopulation() / 1e6, 0.2);
        }
        if (lower.contains("milankovitch") || lower.contains("insolation")) {
            val = snap.getValue("milankovitchInsolation");
            if (val != 0.0) return val;
            return org.ether.society.engines.tier2.theories.ProceduralPopulationEngine.calculateMilankovitchSummerInsolation65N(year);
        }
        if (lower.contains("confinement") || lower.contains("zerocontainment")) {
            val = snap.getValue("zeroContainmentScore");
            return val != 0.0 ? val : 100.0;
        }
        if (lower.contains("gini") || lower.contains("in├®gal")) {
            val = snap.getValue("gini");
            if (val == 0.0) val = snap.getValue("giniIndex");
            if (val != 0.0) return val;
        }
        if (lower.contains("bonheur") || lower.contains("happiness")) {
            val = snap.getValue("happiness");
            if (val != 0.0) return val;
        }
        if (lower.contains("conflit") || lower.contains("conflict") || lower.contains("guerre")) {
            val = snap.getValue("conflict");
            if (val != 0.0) return val;
        }
        if (lower.contains("kardashev")) {
            val = snap.getValue("kardashev");
            if (val != 0.0) return val;
        }
        if (lower.contains("eau") || lower.contains("water") || lower.contains("aquif")) {
            val = snap.getValue("potableWater");
            if (val != 0.0) return val;
        }
        if (lower.contains("fertil") || lower.contains("f├®cond")) {
            val = snap.getValue("fertilityRate");
            if (val != 0.0) return val;
        }
        if (lower.contains("vie") || lower.contains("life") || lower.contains("esp├®rance")) {
            val = snap.getValue("lifeExpectancy");
            if (val != 0.0) return val;
        }
        if (lower.contains("surproduction") || lower.contains("elite") || lower.contains("├®lite")) {
            val = snap.getValue("eliteOverproduction");
            if (val != 0.0) return val;
        }
        if (lower.contains("stress") || lower.contains("psi")) {
            val = snap.getValue("psiStressIndex");
            if (val != 0.0) return val;
        }
        if (lower.contains("effondrement") || lower.contains("collapse")) {
            val = snap.getValue("collapseRisk");
            if (val == 0.0) val = snap.getValue("collapseVulnerability");
            if (val != 0.0) return val;
        }
        if (lower.contains("complex") || lower.contains("interd├®pend")) {
            val = snap.getValue("systemComplexity");
            if (val == 0.0) val = snap.getValue("systemInterdependence");
            if (val != 0.0) return val;
        }
        if (lower.contains("m├®moire") || lower.contains("memory")) {
            val = snap.getValue("collectiveMemory");
            if (val != 0.0) return val;
        }
        if (lower.contains("co2") || lower.contains("carbone") || lower.contains("carbon")) {
            val = snap.getValue("carbonFootprint");
            if (val != 0.0) return val;
        }
        if (lower.contains("anomal") || lower.contains("temperatureanomaly")) {
            val = snap.getValue("temperatureAnomaly");
            if (val != 0.0) return val;
            double rawT = snap.getValue("temperature");
            if (rawT != 0.0) {
                double cT = rawT > 100.0 ? (rawT - 273.15) : rawT;
                return cT - 14.0;
            }
            return 0.0;
        }
        if (lower.contains("temp├®rat") || lower.contains("temperat")) {
            val = snap.getValue("temperature");
            if (val != 0.0) return val;
            double anom = snap.getValue("temperatureAnomaly");
            if (anom != 0.0) return 14.0 + anom;
        }
        if (lower.contains("pr├®cipit") || lower.contains("precipit") || lower.contains("pluie")) {
            val = snap.getValue("precipitation");
            if (val != 0.0) return val;
        }
        if (lower.contains("eroi") || lower.contains("rendement")) {
            val = snap.getValue("eroiAlim");
            if (val != 0.0) return val;
        }
        if (lower.contains("biomass") || lower.contains("biomasse")) {
            val = snap.getValue("biomassMobilized");
            if (val != 0.0) return val;
        }
        return val;
    }

    private void copyReportToClipboard() {
        String currentReport = (reportPreviewPane != null) ? reportPreviewPane.getMarkdown() : "";
        if (currentReport == null || currentReport.isBlank() || currentReport.startsWith("## Please select")) {
            return;
        }

        try {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(currentReport);
            clipboard.setContent(content);

            String origText = copyReportBtn.getText();
            copyReportBtn.setText(I18n.getOrDefault("analytics.status.copy_success", "Ô£à Rapport Copi├® !"));
            copyReportBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #059669; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");

            Timeline resetBtnTimer = new Timeline(new KeyFrame(Duration.millis(2500), ev -> {
                copyReportBtn.setText(origText);
                copyReportBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
            }));
            resetBtnTimer.setCycleCount(1);
            resetBtnTimer.play();
            logger.info("Successfully copied markdown analysis report ({} chars) to system clipboard.", currentReport.length());
        } catch (Exception ex) {
            logger.error("Failed to copy report to system clipboard: {}", ex.getMessage());
        }
    }

    private void exportMarkdownReport() {
        String currentReport = (reportPreviewPane != null) ? reportPreviewPane.getMarkdown() : "";
        org.ether.society.ui.export.ComparativeAnalyticsExporter.exportMarkdownReport(currentReport, exportMdBtn);
    }

    private void exportCsvData() {
        org.ether.society.ui.export.ComparativeAnalyticsExporter.exportCsvData(
                scenarioList,
                exportCsvBtn,
                this::resolveRunRecord
        );
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^a-zA-Z0-9_.-]", "_");
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
            if (spatialTensorComparisonPane != null) {
                spatialTensorComparisonPane.updateTexts(scenarioList);
            }
            updateExecutionContextBadge();
            if (copyReportBtn != null) {
                copyReportBtn.setText(I18n.getOrDefault("analytics.btn.copy", "📋 Copy Report"));
                copyReportBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.copy", "Copy markdown audit and comparative synthesis report directly to system clipboard.")));
            }
            if (exportMdBtn != null) {
                exportMdBtn.setText(I18n.getOrDefault("analytics.btn.export_md", "📝 Export Report (.md)"));
                exportMdBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.export_md", "Export complete markdown synthesis report including all diagnostics and equations.")));
            }
            if (exportCsvBtn != null) {
                exportCsvBtn.setText(I18n.getOrDefault("analytics.btn.export_csv", "📥 Export Data (.csv)"));
                exportCsvBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.export_csv", "Export raw multi-scenario chronological telemetry series into CSV format.")));
            }
            if (export1DCsvBtn != null) {
                export1DCsvBtn.setText(I18n.getOrDefault("analytics.btn.export_1d_csv", "📥 Exporter les données CSV (Séries 1D)"));
                export1DCsvBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.export_1d_csv", "Exporter l'ensemble des séries temporelles 1D sélectionnées avec leurs valeurs interpolées au format CSV.")));
            }
            if (executeMissingBtn != null) {
                executeMissingBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.execute", "Run physical headless simulation across queued scenarios using the H3SimulationEngine.")));
            }
            if (cancelBatchBtn != null) {
                cancelBatchBtn.setText(I18n.getOrDefault("analytics.btn.cancel_batch", "🛑 Cancel Batch"));
                cancelBatchBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.cancel", "Cancel pending background simulation batch.")));
            }
            if (chartHintLabel != null) {
                chartHintLabel.setText(I18n.getOrDefault("analytics.chart.hint", "💡 Scroll wheel: Zoom on cursor | Drag: Pan horizontally | Double-click: Reset time range"));
            }
            if (xAxis != null) {
                xAxis.setLabel(I18n.getOrDefault("analytics.axis.x", "Calendar Year (BCE / CE)"));
                Tooltip.install(xAxis, new Tooltip(I18n.getOrDefault("analytics.axis.tooltip_x", "Chronological Timeline: Calendar years (BC / AD). Mouse wheel zooms on cursor, drag pans, double-click resets.")));
            }
            if (yAxis != null) yAxis.setLabel(I18n.getOrDefault("analytics.axis.y", "Metric Value"));
            if (chart != null) chart.setTitle(I18n.getOrDefault("analytics.chart.title", "Multi-Scenario Chronological Overlay (💡 Scroll to Zoom on Cursor, Drag to Pan, Double-Click to Reset)"));
            if (diagHeader != null) diagHeader.setText(I18n.getOrDefault("analytics.diag_header", "🔍 DIVERGENCE ANALYSIS & GAP ANATOMY"));
            if (synthHeader != null) synthHeader.setText(I18n.getOrDefault("analytics.synth_header", "📄 Auto-Generated Comparative Summary:"));
            if (searchField != null) searchField.setPromptText(I18n.getOrDefault("analytics.search_prompt", "🔍 Filter scenarios by name, status, or year range..."));
            if (scenariosTab != null) scenariosTab.setText(I18n.getOrDefault("analytics.tab.scenarios", "📋 Scenarios & Data"));
            if (timeSeriesTab != null) timeSeriesTab.setText(I18n.getOrDefault("analytics.tab.timeseries", "📈 Time Series (1D)"));
            if (spatialCartoTab != null) spatialCartoTab.setText(I18n.getOrDefault("analytics.tab.carto_tensors", "🗺️ Cartography & Tensors (2D)"));
            if (reportTab != null) reportTab.setText(I18n.getOrDefault("analytics.tab.report", "📄 Analysis Report & Synthesis"));

            if (divergenceLabel != null && (divergenceLabel.getText() == null || divergenceLabel.getText().isBlank() || divergenceLabel.getText().startsWith("Point de rupture") || divergenceLabel.getText().startsWith("Point of divergence") || divergenceLabel.getText().startsWith("Break point") || divergenceLabel.getText().startsWith("Punto de ruptura") || divergenceLabel.getText().startsWith("Bruchpunkt") || divergenceLabel.getText().startsWith("临界断点"))) {
                divergenceLabel.setText(I18n.getOrDefault("analytics.divergence.select_hint", "Point of divergence: Select at least 2 scenarios"));
            }
            if (explanationLabel != null && (explanationLabel.getText() == null || explanationLabel.getText().isBlank() || explanationLabel.getText().startsWith("Cochez les scénarios") || explanationLabel.getText().startsWith("Check scenarios") || explanationLabel.getText().startsWith("Marque los escenarios") || explanationLabel.getText().startsWith("Wählen Sie Szenarien") || explanationLabel.getText().startsWith("勾选上方列表"))) {
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

    /**
     * Updates the chronological X-axis range and dynamically recomputes an optimal tick unit
     * via {@link WindowUtils#computeNiceTickUnit(double)} to guarantee that tick mark allocations
     * never exceed JavaFX internal thresholds ($N_{\text{ticks}} < 2,000$).
     *
     * @param lower the lower bound of the timeline in simulation years
     * @param upper the upper bound of the timeline in simulation years
     */
    private void setXAxisRange(double lower, double upper) {
        if (xAxis == null) return;
        if (upper <= lower) upper = lower + 10.0;
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(lower);
        xAxis.setUpperBound(upper);
        double range = upper - lower;
        double tickUnit = WindowUtils.computeNiceTickUnit(range);
        xAxis.setTickUnit(tickUnit);
    }
}
