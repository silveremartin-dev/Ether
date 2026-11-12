/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.ether.society.i18n.I18n;
import org.ether.society.model.CulturalEntity;
import org.ether.society.model.Scenario;
import org.ether.society.ui.CulturalEpochRegistryLoader;

import java.util.*;

/**
 * <h1>Cultural Matrix &amp; Affinity Discrepancy Comparison Pane</h1>
 *
 * <p>Provides multi-scenario ethnographic tensor and affinity matrix comparison \(\Delta(A - B)\)
 * within the comparative analytics suite. Supports pairwise differential heatmaps, multi-scenario
 * divergence galleries against an empirical baseline, Frobenius norm matrix discrepancy,
 * mean absolute delta (MAD), and asynchronous evaluation.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 * @see org.ether.society.model.CulturalEntity
 * @see org.ether.society.ui.ComparativeAnalyticsPanel
 */
public class CulturalMatrixComparisonPane extends VBox {

    private Label baselineLabel;
    private ComboBox<ScenarioSelectableItem> comboBaseline;
    private Label targetLabel;
    private ComboBox<ScenarioSelectableItem> comboTarget;
    private Label epochLabel;
    private ComboBox<Long> comboEpoch;

    private ToggleGroup modeGroup;
    private RadioButton rbPairwiseView;
    private RadioButton rbMultiView;

    private HBox pairwiseContainer;
    private ScrollPane pairwiseScrollPane;

    private ScrollPane multiScenarioScrollPane;
    private HBox multiScenarioContainer;

    private Label matrixLabelA;
    private Label matrixLabelB;
    private Label matrixLabelDelta;

    private GridPane gridA;
    private GridPane gridB;
    private GridPane gridDelta;

    private Label lblMeanA;
    private Label lblMeanB;
    private Label lblFrobenius;
    private Label lblMad;
    private Label lblMaxShift;
    private Label cellDetailLabel;

    private Button btnExportCsv;
    private Button btnExportJson;
    private Button btnCopyReport;

    private List<ScenarioSelectableItem> allScenarios = new ArrayList<>();
    private List<ScenarioSelectableItem> selectedScenarios = new ArrayList<>();
    private boolean isUpdatingTexts = false;

    /**
     * Constructs the cultural matrix comparison pane and initializes controls.
     */
    public CulturalMatrixComparisonPane() {
        super(8);
        setPadding(new Insets(12));
        initUI();
    }

    private void initUI() {
        // --- ROW 1: Selection Controls & Modes ---
        baselineLabel = new Label(I18n.getOrDefault("analytics.cultural.baseline", "Baseline (A):"));
        baselineLabel.getStyleClass().add("control-label");
        comboBaseline = new ComboBox<>();
        comboBaseline.setPrefWidth(210);
        comboBaseline.setOnAction(e -> updateComparison());

        targetLabel = new Label(I18n.getOrDefault("analytics.cultural.target", "Target (B):"));
        targetLabel.getStyleClass().add("control-label");
        comboTarget = new ComboBox<>();
        comboTarget.setPrefWidth(210);
        comboTarget.setOnAction(e -> updateComparison());

        epochLabel = new Label(I18n.getOrDefault("analytics.cultural.epoch", "Epoch:"));
        epochLabel.getStyleClass().add("control-label");
        comboEpoch = new ComboBox<>();
        comboEpoch.setPrefWidth(180);
        comboEpoch.getItems().setAll(CulturalEpochRegistryLoader.discoverAllEpochs());
        comboEpoch.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : CulturalEpochRegistryLoader.formatEpochName(item));
            }
        });
        comboEpoch.setButtonCell(comboEpoch.getCellFactory().call(null));
        if (!comboEpoch.getItems().isEmpty()) {
            comboEpoch.setValue(comboEpoch.getItems().get(0));
        }
        comboEpoch.setOnAction(e -> updateComparison());

        HBox selectionRow = new HBox(10, baselineLabel, comboBaseline, targetLabel, comboTarget, epochLabel, comboEpoch);
        selectionRow.setAlignment(Pos.CENTER_LEFT);

        // --- ROW 2: View Mode Toggles ---
        modeGroup = new ToggleGroup();
        rbPairwiseView = new RadioButton(I18n.getOrDefault("analytics.cultural.view_pairwise", "⚖️ Detailed Differential View [A vs B & Delta]"));
        rbPairwiseView.setToggleGroup(modeGroup);
        rbPairwiseView.setSelected(true);
        rbPairwiseView.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        rbMultiView = new RadioButton(I18n.getOrDefault("analytics.cultural.view_multi", "🌐 Multi-Scenario Gallery [Deltas vs Baseline]"));
        rbMultiView.setToggleGroup(modeGroup);
        rbMultiView.setDisable(true);
        rbMultiView.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        modeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            boolean isMulti = rbMultiView.isSelected();
            pairwiseScrollPane.setVisible(!isMulti);
            pairwiseScrollPane.setManaged(!isMulti);
            multiScenarioScrollPane.setVisible(isMulti);
            multiScenarioScrollPane.setManaged(isMulti);
            targetLabel.setVisible(!isMulti);
            targetLabel.setManaged(!isMulti);
            comboTarget.setVisible(!isMulti);
            comboTarget.setManaged(!isMulti);
            updateComparison();
        });

        HBox modeRow = new HBox(16, rbPairwiseView, rbMultiView);
        modeRow.setAlignment(Pos.CENTER_LEFT);

        VBox topControlsBox = new VBox(8, selectionRow, modeRow);

        // --- PAIRWISE VIEW GRIDS ---
        matrixLabelA = new Label(I18n.getOrDefault("analytics.cultural.matrix_a", "Scenario A Cultural Matrix"));
        matrixLabelA.getStyleClass().add("label-section-header");
        gridA = createMatrixGrid();
        VBox boxA = new VBox(4, matrixLabelA, gridA);
        boxA.setAlignment(Pos.TOP_CENTER);

        matrixLabelB = new Label(I18n.getOrDefault("analytics.cultural.matrix_b", "Scenario B Cultural Matrix"));
        matrixLabelB.getStyleClass().add("label-section-header");
        gridB = createMatrixGrid();
        VBox boxB = new VBox(4, matrixLabelB, gridB);
        boxB.setAlignment(Pos.TOP_CENTER);

        matrixLabelDelta = new Label(I18n.getOrDefault("analytics.cultural.delta_matrix", "Discrepancy Matrix Δ(Target - Baseline)"));
        matrixLabelDelta.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
        gridDelta = createMatrixGrid();
        VBox boxDelta = new VBox(4, matrixLabelDelta, gridDelta);
        boxDelta.setAlignment(Pos.TOP_CENTER);

        pairwiseContainer = new HBox(16, boxA, boxB, boxDelta);
        pairwiseContainer.setAlignment(Pos.TOP_CENTER);
        pairwiseContainer.setPadding(new Insets(8));

        pairwiseScrollPane = new ScrollPane(pairwiseContainer);
        pairwiseScrollPane.setFitToWidth(false);
        pairwiseScrollPane.setFitToHeight(false);
        pairwiseScrollPane.setPannable(true);
        pairwiseScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pairwiseScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(pairwiseScrollPane, Priority.ALWAYS);

        // --- MULTI-SCENARIO SCROLLPANE ---
        multiScenarioContainer = new HBox(16);
        multiScenarioContainer.setAlignment(Pos.TOP_LEFT);
        multiScenarioContainer.setPadding(new Insets(8));

        multiScenarioScrollPane = new ScrollPane(multiScenarioContainer);
        multiScenarioScrollPane.setFitToHeight(true);
        multiScenarioScrollPane.setPannable(true);
        multiScenarioScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        multiScenarioScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        multiScenarioScrollPane.setVisible(false);
        multiScenarioScrollPane.setManaged(false);
        VBox.setVgrow(multiScenarioScrollPane, Priority.ALWAYS);

        // --- LEGEND BAR ---
        HBox legendBar = buildLegendBar();

        // --- STATISTICAL SUMMARY CARDS ---
        lblMeanA = new Label();
        lblMeanA.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        lblMeanB = new Label();
        lblMeanB.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #818cf8; -fx-font-weight: bold;");

        lblFrobenius = new Label();
        lblFrobenius.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #f59e0b; -fx-font-weight: bold;");

        lblMad = new Label();
        lblMad.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #10b981; -fx-font-weight: bold;");

        lblMaxShift = new Label();
        lblMaxShift.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #ec4899; -fx-font-weight: bold;");

        HBox statsSummaryBox = new HBox(18, lblMeanA, lblMeanB, lblFrobenius, lblMad, lblMaxShift);
        statsSummaryBox.setAlignment(Pos.CENTER_LEFT);
        statsSummaryBox.setPadding(new Insets(6, 12, 6, 12));
        statsSummaryBox.setStyle("-fx-background-color: rgba(30, 41, 59, 0.7); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 6;");

        cellDetailLabel = new Label(I18n.getOrDefault("analytics.cultural.cell_hint", "💡 Click or hover over any matrix cell for ethnographic breakdown."));
        cellDetailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1; -fx-padding: 2 4;");
        cellDetailLabel.setWrapText(true);

        // --- EXPORT TOOLBAR ---
        btnExportCsv = new Button(I18n.getOrDefault("analytics.cultural.export_csv", "📥 Export Matrices (CSV)"));
        btnExportCsv.setStyle("-fx-font-weight: bold; -fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-padding: 5 12; -fx-cursor: hand;");
        btnExportCsv.setOnAction(e -> exportCsv());

        btnExportJson = new Button(I18n.getOrDefault("analytics.cultural.export_json", "📥 Export JSON"));
        btnExportJson.setStyle("-fx-font-weight: bold; -fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 5 12; -fx-cursor: hand;");
        btnExportJson.setOnAction(e -> exportJson());

        btnCopyReport = new Button(I18n.getOrDefault("analytics.cultural.copy_report", "📋 Copy Cultural Report"));
        btnCopyReport.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 5 12; -fx-cursor: hand;");
        btnCopyReport.setOnAction(e -> copyReportToClipboard());

        HBox exportBox = new HBox(10, btnExportCsv, btnExportJson, btnCopyReport);
        exportBox.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(topControlsBox, pairwiseScrollPane, multiScenarioScrollPane,
                legendBar, statsSummaryBox, cellDetailLabel, exportBox);
    }

    private GridPane createMatrixGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(3);
        grid.setVgap(3);
        grid.setPadding(new Insets(6));
        grid.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); -fx-background-radius: 6;");
        return grid;
    }

    private HBox buildLegendBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(4, 12, 4, 12));
        bar.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); -fx-background-radius: 6;");

        Label red = new Label("🟥 " + I18n.getOrDefault("analytics.cultural.delta_legend_under", "Δ < 0 (Reduced affinity / Distance increased)"));
        red.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");

        Label slate = new Label("⬛ " + I18n.getOrDefault("analytics.cultural.delta_legend_zero", "Δ = 0 (Identical cultural affinity)"));
        slate.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label green = new Label("🟩 " + I18n.getOrDefault("analytics.cultural.delta_legend_over", "Δ > 0 (Increased affinity / Mutual assimilation)"));
        green.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px;");

        bar.getChildren().addAll(red, slate, green);
        return bar;
    }

    /**
     * Updates the selectable scenario pool from comparative table.
     *
     * @param scenarioList active scenario list
     */
    public void updateScenarioPool(List<ScenarioSelectableItem> scenarioList) {
        if (scenarioList == null) return;
        this.allScenarios = new ArrayList<>(scenarioList);
        this.selectedScenarios = scenarioList.stream().filter(ScenarioSelectableItem::isSelected).toList();

        List<ScenarioSelectableItem> pool = (selectedScenarios.size() >= 2) ? selectedScenarios : allScenarios;

        ScenarioSelectableItem prevA = comboBaseline.getValue();
        ScenarioSelectableItem prevB = comboTarget.getValue();

        comboBaseline.getItems().setAll(pool);
        comboTarget.getItems().setAll(pool);

        if (prevA != null && pool.contains(prevA)) comboBaseline.setValue(prevA);
        else if (!pool.isEmpty()) comboBaseline.setValue(pool.get(0));

        if (prevB != null && pool.contains(prevB)) comboTarget.setValue(prevB);
        else if (pool.size() > 1) comboTarget.setValue(pool.get(1));
        else if (!pool.isEmpty()) comboTarget.setValue(pool.get(0));

        if (selectedScenarios.size() >= 3) {
            rbMultiView.setDisable(false);
            rbMultiView.setText(String.format(I18n.getOrDefault("analytics.mode.multi_scenarios_count", "🌐 Multi-Scenarios (%d selected)"), selectedScenarios.size()));
        } else {
            rbMultiView.setDisable(true);
            rbPairwiseView.setSelected(true);
        }

        updateComparison();
    }

    /**
     * Recomputes cultural matrices, delta matrix, and statistical divergence metrics.
     */
    public void updateComparison() {
        if (isUpdatingTexts) return;

        ScenarioSelectableItem itemA = comboBaseline.getValue();
        ScenarioSelectableItem itemB = comboTarget.getValue();

        if (itemA == null && !comboBaseline.getItems().isEmpty()) itemA = comboBaseline.getItems().get(0);
        if (itemB == null && comboTarget.getItems().size() > 1) itemB = comboTarget.getItems().get(1);
        if (itemB == null) itemB = itemA;

        if (itemA == null || itemA.getScenario() == null) {
            cellDetailLabel.setText(I18n.getOrDefault("analytics.cultural.no_scenarios", "⚠️ Select at least 2 scenarios to compare cultural affinity matrices."));
            return;
        }

        Scenario scA = itemA.getScenario();
        Scenario scB = (itemB != null && itemB.getScenario() != null) ? itemB.getScenario() : scA;

        Long selectedEpoch = comboEpoch.getValue();
        long epoch = selectedEpoch != null ? selectedEpoch : scA.getStartDateYear();

        List<CulturalEntity> entitiesA = CulturalEpochRegistryLoader.loadEpochRegistry(itemA.getPlanetDisplayName(), epoch);
        if (entitiesA.isEmpty()) entitiesA = CulturalEpochRegistryLoader.loadEpochRegistry(epoch);

        List<CulturalEntity> entitiesB = CulturalEpochRegistryLoader.loadEpochRegistry(itemB != null ? itemB.getPlanetDisplayName() : itemA.getPlanetDisplayName(), epoch);
        if (entitiesB.isEmpty()) entitiesB = CulturalEpochRegistryLoader.loadEpochRegistry(epoch);

        if (rbMultiView.isSelected() && selectedScenarios.size() >= 3) {
            renderMultiScenarioView(itemA, epoch);
        } else {
            renderPairwiseView(itemA, itemB, scA, scB, entitiesA, entitiesB);
        }
    }

    private void renderPairwiseView(ScenarioSelectableItem itemA, ScenarioSelectableItem itemB,
                                    Scenario scA, Scenario scB,
                                    List<CulturalEntity> entitiesA, List<CulturalEntity> entitiesB) {
        matrixLabelA.setText(I18n.getOrDefault("analytics.cultural.matrix_a", "Scenario A: ") + itemA.getName());
        matrixLabelB.setText(I18n.getOrDefault("analytics.cultural.matrix_b", "Scenario B: ") + (itemB != null ? itemB.getName() : scB.getName()));
        matrixLabelDelta.setText(I18n.getOrDefault("analytics.cultural.delta_matrix", "Discrepancy Matrix Δ(B - A)"));

        CulturalMatrixGridRenderer.populateAffinityGrid(gridA, scA, entitiesA, itemA.getName(), cellDetailLabel::setText);
        CulturalMatrixGridRenderer.populateAffinityGrid(gridB, scB, entitiesB, itemB != null ? itemB.getName() : scB.getName(), cellDetailLabel::setText);

        // Delta Grid: compares entities present in both
        List<CulturalEntity> commonEntities = CulturalMatrixComparisonHelper.alignEntities(entitiesA, entitiesB);
        CulturalMatrixComparisonHelper.MatrixStats stats = CulturalMatrixGridRenderer.populateDeltaGrid(
                gridDelta, scA, scB, commonEntities, itemA.getName(), itemB != null ? itemB.getName() : scB.getName(), cellDetailLabel::setText);

        lblMeanA.setText(String.format(I18n.getOrDefault("analytics.cultural.mean_affinity_a", "Mean Affinity A: %.1f%%"), stats.meanA() * 100.0));
        lblMeanB.setText(String.format(I18n.getOrDefault("analytics.cultural.mean_affinity_b", "Mean Affinity B: %.1f%%"), stats.meanB() * 100.0));
        lblFrobenius.setText(String.format(I18n.getOrDefault("analytics.cultural.frobenius", "Frobenius Norm ||Δ||: %.3f"), stats.frobeniusNorm()));
        lblMad.setText(String.format(I18n.getOrDefault("analytics.cultural.mean_shift", "Mean Absolute Shift (MAD): %.2f%%"), stats.mad() * 100.0));
        lblMaxShift.setText(String.format(I18n.getOrDefault("analytics.cultural.max_shift", "Max Shift: %+.1f%% (%s → %s)"), stats.maxDelta() * 100.0, stats.maxOrig(), stats.maxTarg()));
    }

    private void renderMultiScenarioView(ScenarioSelectableItem baselineItem, long epoch) {
        multiScenarioContainer.getChildren().clear();
        Scenario baseSc = baselineItem.getScenario();
        List<CulturalEntity> baseEntities = CulturalEpochRegistryLoader.loadEpochRegistry(baselineItem.getPlanetDisplayName(), epoch);
        if (baseEntities.isEmpty()) baseEntities = CulturalEpochRegistryLoader.loadEpochRegistry(epoch);

        // Baseline Card
        VBox baseCard = new VBox(6);
        baseCard.setPadding(new Insets(8));
        baseCard.getStyleClass().add("card-section");
        baseCard.setStyle("-fx-background-color: rgba(30, 41, 59, 0.7); -fx-background-radius: 8; -fx-border-color: #38bdf8; -fx-border-radius: 8;");
        Label baseTitle = new Label("👑 " + I18n.getOrDefault("analytics.cultural.baseline", "Baseline: ") + baselineItem.getName());
        baseTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #38bdf8;");
        GridPane baseGrid = CulturalMatrixGridRenderer.createMatrixGrid();
        CulturalMatrixGridRenderer.populateAffinityGrid(baseGrid, baseSc, baseEntities, baselineItem.getName(), cellDetailLabel::setText);
        baseCard.getChildren().addAll(baseTitle, baseGrid);
        multiScenarioContainer.getChildren().add(baseCard);

        double totalMadSum = 0;
        int compCount = 0;

        for (ScenarioSelectableItem compItem : selectedScenarios) {
            if (compItem == baselineItem) continue;
            Scenario compSc = compItem.getScenario();
            if (compSc == null) continue;

            List<CulturalEntity> compEntities = CulturalEpochRegistryLoader.loadEpochRegistry(compItem.getPlanetDisplayName(), epoch);
            if (compEntities.isEmpty()) compEntities = CulturalEpochRegistryLoader.loadEpochRegistry(epoch);
            List<CulturalEntity> common = CulturalMatrixComparisonHelper.alignEntities(baseEntities, compEntities);

            VBox compCard = new VBox(6);
            compCard.setPadding(new Insets(8));
            compCard.getStyleClass().add("card-section");
            compCard.setStyle("-fx-background-color: rgba(15, 23, 42, 0.7); -fx-background-radius: 8; -fx-border-color: rgba(255, 255, 255, 0.12); -fx-border-radius: 8;");

            Label compTitle = new Label("📊 " + compItem.getName() + " [Δ vs Baseline]");
            compTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #f59e0b;");

            GridPane deltaGrid = CulturalMatrixGridRenderer.createMatrixGrid();
            CulturalMatrixComparisonHelper.MatrixStats st = CulturalMatrixGridRenderer.populateDeltaGrid(
                    deltaGrid, baseSc, compSc, common, baselineItem.getName(), compItem.getName(), cellDetailLabel::setText);

            Label stLabel = new Label(String.format("||Δ||_F = %.3f | MAD = %.1f%% | Max: %+.1f%%", st.frobeniusNorm(), st.mad() * 100.0, st.maxDelta() * 100.0));
            stLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

            compCard.getChildren().addAll(compTitle, deltaGrid, stLabel);
            multiScenarioContainer.getChildren().add(compCard);

            totalMadSum += st.mad();
            compCount++;
        }

        lblMeanA.setText(String.format("Baseline: %s", baselineItem.getName()));
        lblMeanB.setText(String.format("Multi-Scenarios: %d Compared", compCount));
        lblFrobenius.setText(String.format("Average MAD vs Baseline: %.2f%%", compCount > 0 ? (totalMadSum / compCount) * 100.0 : 0.0));
        lblMad.setText("");
        lblMaxShift.setText("");
    }

    private void exportCsv() {
        CulturalMatrixComparisonExporter.exportCsv(
                getScene() != null ? getScene().getWindow() : null,
                comboBaseline.getValue(),
                comboTarget.getValue(),
                comboEpoch.getValue());
    }

    private void exportJson() {
        CulturalMatrixComparisonExporter.exportJson(
                getScene() != null ? getScene().getWindow() : null,
                comboBaseline.getValue(),
                comboTarget.getValue(),
                comboEpoch.getValue());
    }

    private void copyReportToClipboard() {
        CulturalMatrixComparisonExporter.copyReportToClipboard(
                comboBaseline.getValue(),
                comboTarget.getValue(),
                comboEpoch.getValue(),
                lblFrobenius.getText(),
                lblMad.getText(),
                lblMaxShift.getText());
        cellDetailLabel.setText("✅ " + I18n.getOrDefault("analytics.copied", "Copied cultural delta report to clipboard!"));
    }

    /**
     * Updates localized strings and combo tooltips across UI components.
     */
    public void updateTexts() {
        isUpdatingTexts = true;
        try {
            if (baselineLabel != null) baselineLabel.setText(I18n.getOrDefault("analytics.cultural.baseline", "Baseline (A):"));
            if (targetLabel != null) targetLabel.setText(I18n.getOrDefault("analytics.cultural.target", "Target (B):"));
            if (epochLabel != null) epochLabel.setText(I18n.getOrDefault("analytics.cultural.epoch", "Epoch:"));
            if (rbPairwiseView != null) rbPairwiseView.setText(I18n.getOrDefault("analytics.cultural.view_pairwise", "⚖️ Detailed Differential View [A vs B & Delta]"));
            if (rbMultiView != null) rbMultiView.setText(String.format(I18n.getOrDefault("analytics.mode.multi_scenarios_count", "🌐 Multi-Scenarios (%d selected)"), selectedScenarios.size()));
            if (btnExportCsv != null) btnExportCsv.setText(I18n.getOrDefault("analytics.cultural.export_csv", "📥 Export Matrices (CSV)"));
            if (btnExportJson != null) btnExportJson.setText(I18n.getOrDefault("analytics.cultural.export_json", "📥 Export JSON"));
            if (btnCopyReport != null) btnCopyReport.setText(I18n.getOrDefault("analytics.cultural.copy_report", "📋 Copy Cultural Report"));
        } finally {
            isUpdatingTexts = false;
        }
        updateComparison();
    }
}