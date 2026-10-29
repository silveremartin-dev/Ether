/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import org.ether.society.i18n.I18n;

import java.util.Comparator;
import java.util.List;

/**
 * Coordinator for the multi-scenario comparison TableView.
 *
 * <p>Configures responsive cell rendering, interactive checkbox toggling,
 * progress bars, status badges, and comparator bindings.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ComparativeScenarioTableCoordinator {

    private ComparativeScenarioTableCoordinator() {
        // Utility class
    }

    /**
     * Encapsulates references to the table columns for localization updates.
     */
    public record TableColumns(
        TableColumn<ScenarioSelectableItem, Boolean> selectCol,
        TableColumn<ScenarioSelectableItem, String> nameCol,
        TableColumn<ScenarioSelectableItem, String> planetCol,
        TableColumn<ScenarioSelectableItem, String> clippingCol,
        TableColumn<ScenarioSelectableItem, Long> yearsCol,
        TableColumn<ScenarioSelectableItem, Double> progressCol,
        TableColumn<ScenarioSelectableItem, Double> estDurationCol,
        TableColumn<ScenarioSelectableItem, String> statusCol
    ) {}

    /**
     * Initializes and configures the scenario table view with custom cells and sorting.
     */
    public static TableColumns configureTable(
            TableView<ScenarioSelectableItem> scenarioTable,
            ObservableList<ScenarioSelectableItem> scenarioList,
            FilteredList<ScenarioSelectableItem> filteredScenarioList
    ) {
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
                    } else if (item.getRunId() != null && item.getRunId().startsWith("PREDICTIVE_")) {
                        setStyle("-fx-border-color: transparent transparent #a855f7 transparent; -fx-border-width: 0 0 2px 0; -fx-border-style: solid; -fx-background-color: rgba(168, 85, 247, 0.10); -fx-font-weight: bold;");
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

        TableColumn<ScenarioSelectableItem, Boolean> selectCol = new TableColumn<>();
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

        TableColumn<ScenarioSelectableItem, String> nameCol = new TableColumn<>();
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(220);

        TableColumn<ScenarioSelectableItem, String> planetCol = new TableColumn<>();
        planetCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getPlanetDisplayName()));
        planetCol.setPrefWidth(125);

        TableColumn<ScenarioSelectableItem, String> clippingCol = new TableColumn<>();
        clippingCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getClippingDisplay()));
        clippingCol.setPrefWidth(210);

        TableColumn<ScenarioSelectableItem, Long> yearsCol = new TableColumn<>();
        yearsCol.setCellValueFactory(p -> new SimpleLongProperty(p.getValue().getStartDateYear()).asObject());
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

        TableColumn<ScenarioSelectableItem, Double> progressCol = new TableColumn<>();
        progressCol.setCellValueFactory(p -> new SimpleDoubleProperty(p.getValue().getProgress()).asObject());
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

        TableColumn<ScenarioSelectableItem, Double> estDurationCol = new TableColumn<>();
        estDurationCol.setCellValueFactory(p -> new SimpleDoubleProperty(p.getValue().getRemainingDurationSec()).asObject());
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

        TableColumn<ScenarioSelectableItem, String> statusCol = new TableColumn<>();
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

        SortedList<ScenarioSelectableItem> sortedScenarioList = new SortedList<>(filteredScenarioList);
        sortedScenarioList.comparatorProperty().bind(Bindings.createObjectBinding(() -> {
            Comparator<ScenarioSelectableItem> baseComp = scenarioTable.getComparator();
            return (a, b) -> {
                if (a == b) return 0;
                if (a == null) return 1;
                if (b == null) return -1;
                int pA = "HISTORICAL_GROUND_TRUTH".equals(a.getRunId()) ? 0 : (a.getRunId() != null && a.getRunId().startsWith("PREDICTIVE_") ? 1 : 2);
                int pB = "HISTORICAL_GROUND_TRUTH".equals(b.getRunId()) ? 0 : (b.getRunId() != null && b.getRunId().startsWith("PREDICTIVE_") ? 1 : 2);
                if (pA != pB) return Integer.compare(pA, pB);
                if (pA == 1) {
                    // Stable ordering among predictive baselines
                    return String.valueOf(a.getRunId()).compareTo(String.valueOf(b.getRunId()));
                }
                if (baseComp != null) {
                    return baseComp.compare(a, b);
                }
                return 0;
            };
        }, scenarioTable.comparatorProperty()));

        scenarioTable.setSortPolicy(tv -> TableView.DEFAULT_SORT_POLICY.call(tv));
        scenarioTable.setItems(sortedScenarioList);

        return new TableColumns(selectCol, nameCol, planetCol, clippingCol, yearsCol, progressCol, estDurationCol, statusCol);
    }
}
