/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.model.CulturalEntity;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.ether.society.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive Dialog & Heatmap Inspector for Cliodynamic Cultural Affinity & Distance Matrices.
 * Displays discrete 24-bit RGB Entity traits, technocomplexes, and pairwise cultural affinities (N x N).
 * Supports in-place symmetric cell editing, procedural auto-generation, and full 5-language localization.
 *
 * @author Silvere Martin-Michiellot
 */
public class CulturalAffinityMatrixDialog extends Stage {
    private static final Logger logger = LoggerFactory.getLogger(CulturalAffinityMatrixDialog.class);

    /**
     * Immutable representation of a discrete cultural entity at a historical epoch.
     *
     * @param id                  canonical entity identifier
     * @param colorHex            24-bit hex badge color
     * @param colorRgb            RGB components
     * @param traits              normalized ethnographic feature vector
     * @param nameEn              English display name
     * @param nameFr              French display name
     * @param nameDe              German display name
     * @param nameEs              Spanish display name
     * @param nameZh              Chinese display name
     * @param kinshipType         kinship & social structure classification
     * @param lithicTechnocomplex lithic or metallurgical technocomplex classification
     */
    

    private final ComboBox<Long> epochSelector = new ComboBox<>();
    private final Label titleLabel = new Label();
    private final Label subtitleLabel = new Label();
    private final Label scenarioEpochBadge = new Label();
    private final VBox entitiesBox = new VBox(8);
    private final GridPane matrixGrid = new GridPane();
    private final ScrollPane matrixScroll = new ScrollPane(matrixGrid);

    private final Button btnGenGeo = new Button();
    private final Button btnGenTraits = new Button();
    private final Button btnGenStochastic = new Button();
    private final Button btnResetDefaults = new Button();

    private final VBox quickEditorCard = new VBox(8);
    private final Label quickEditorTitle = new Label();
    private final Label quickEditorPairLabel = new Label();
    private final Slider quickAffinitySlider = new Slider(0.0, 100.0, 50.0);
    private final Spinner<Double> quickAffinitySpinner = new Spinner<>(0.0, 100.0, 50.0, 1.0);
    private final HBox presetChipsBox = new HBox(6);

    private final Button btnExportCsv = new Button();
    private final Button btnExportJson = new Button();
    private final Button btnCancel = new Button();
    private final Button btnClose = new Button();

    private List<CulturalEntity> activeEntities = new ArrayList<>();
    private final java.util.Map<String, Double> customAffinityOverrides = new java.util.HashMap<>();
    private final java.util.Map<String, Double> initialAffinityOverridesSnapshot = new java.util.HashMap<>();
    private StackPane[][] cellPanes = new StackPane[0][0];
    private Label[][] cellLabels = new Label[0][0];

    private long currentEpoch = -100000L;
    private final long scenarioStartYear;
    private int selectedI = -1;
    private int selectedJ = -1;
    private boolean isUpdatingEditor = false;

    private final org.ether.society.model.Scenario targetScenario;

    public CulturalAffinityMatrixDialog(long initialEpoch) {
        this(initialEpoch, null);
    }

    public CulturalAffinityMatrixDialog(long initialEpoch, org.ether.society.model.Scenario scenario) {
        this.scenarioStartYear = initialEpoch;
        this.currentEpoch = initialEpoch;
        this.targetScenario = scenario;
        initModality(Modality.APPLICATION_MODAL);
        setTitle(I18n.getOrDefault("cultural.matrix.window_title", "ðŸ§© Matrice d'AffinitÃ© & Distances Culturelles"));

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("glass-panel");

        titleLabel.setText(I18n.getOrDefault("cultural.matrix.header_title", "ðŸ›ï¸ Matrice d'AffinitÃ© Culturelle Cliodynamique (N Ã— N)"));
        titleLabel.getStyleClass().add("panel-header");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        subtitleLabel.setText(I18n.getOrDefault("cultural.matrix.header_desc", "Inspecter et Ã©diter la matrice NÃ—N des distances de Mahalanobis, continuitÃ©s linguistiques et coefficients d'assimilation."));
        subtitleLabel.getStyleClass().add("card-description-muted");
        subtitleLabel.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #94a3b8;");

        HBox epochRow = new HBox(12);
        epochRow.setAlignment(Pos.CENTER_LEFT);

        scenarioEpochBadge.setText(String.format(I18n.getOrDefault("cultural.matrix.current_scenario_epoch", "ðŸŽ¯ Ã‰poque du ScÃ©nario : %s"), CulturalEpochRegistryLoader.formatEpochName(initialEpoch)));
        scenarioEpochBadge.setStyle("-fx-font-weight: bold; -fx-font-size: 11.5px; -fx-text-fill: #f59e0b; -fx-background-color: rgba(245, 158, 11, 0.12); -fx-padding: 4 10; -fx-background-radius: 6; -fx-border-color: rgba(245, 158, 11, 0.35); -fx-border-radius: 6;");

        Label lblEpoch = new Label(I18n.getOrDefault("cultural.matrix.select_epoch", "Ã‰poque Historique :"));
        lblEpoch.getStyleClass().add("control-label");
        lblEpoch.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        epochSelector.getItems().setAll(CulturalEpochRegistryLoader.discoverAllEpochs());
        epochSelector.setValue(CulturalEpochRegistryLoader.closestSupportedEpoch(initialEpoch));
        epochSelector.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : CulturalEpochRegistryLoader.formatEpochName(item));
            }
        });
        epochSelector.setButtonCell(epochSelector.getCellFactory().call(null));
        epochSelector.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.epoch", "SÃ©lectionner l'Ã©poque historique pour inspecter les entitÃ©s ethnolinguistiques et matrices d'affinitÃ©.")));
        epochSelector.setOnAction(e -> {
            Long val = epochSelector.getValue();
            if (val != null) loadEpochRegistry(val);
        });
        epochRow.getChildren().addAll(scenarioEpochBadge, new Region(), lblEpoch, epochSelector);
        HBox.setHgrow(epochRow.getChildren().get(1), Priority.ALWAYS);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        VBox matrixTabRoot = new VBox(10);
        matrixTabRoot.setPadding(new Insets(8));

        HBox proceduralToolbar = buildProceduralToolbar();

        matrixGrid.setHgap(4);
        matrixGrid.setVgap(4);
        matrixGrid.setPadding(new Insets(8));
        matrixScroll.setFitToWidth(false);
        matrixScroll.setFitToHeight(false);
        matrixScroll.setPannable(true);
        matrixScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        matrixScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        matrixScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(matrixScroll, Priority.ALWAYS);

        setupQuickEditorPanel();
        matrixTabRoot.getChildren().addAll(proceduralToolbar, matrixScroll, quickEditorCard);

        Tab tabMatrix = new Tab(I18n.getOrDefault("cultural.matrix.tab_heatmap", "ðŸ“Š Carte Thermique d'AffinitÃ© (N Ã— N)"), matrixTabRoot);
        tabMatrix.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.tab_heatmap", "Matrice NÃ—N des coefficients d'affinitÃ© symÃ©triques Ã©ditables en direct.")));

        ScrollPane entitiesScroll = new ScrollPane(entitiesBox);
        entitiesScroll.setFitToWidth(true);
        entitiesScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        entitiesBox.setPadding(new Insets(10));
        Tab tabEntities = new Tab(I18n.getOrDefault("cultural.matrix.tab_entities", "ðŸ“œ Registre Culturel & Technocomplexes"), entitiesScroll);
        tabEntities.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.tab_entities", "EntitÃ©s ethnolinguistiques discrÃ¨tes avec codage couleur 24-bit, rÃ©gimes de parentÃ© et technocomplexes lithiques.")));

        tabPane.getTabs().addAll(tabMatrix, tabEntities);
        HBox footer = buildFooterBar();
        root.getChildren().addAll(titleLabel, subtitleLabel, epochRow, tabPane, footer);

        Scene scene = new Scene(root, 960, 720);
        setScene(scene);
        Theme.applyCurrentTheme(scene);
        Theme.themeProperty().addListener((obs, oldV, newV) -> Theme.applyCurrentTheme(scene));
        WindowHelper.applyWindowIcon(this);

        loadEpochRegistry(epochSelector.getValue());
        if (targetScenario != null && !targetScenario.getCulturalAffinityMatrix().isEmpty()) {
            customAffinityOverrides.putAll(targetScenario.getCulturalAffinityMatrix());
            renderMatrixHeatmap();
        }
        initialAffinityOverridesSnapshot.clear();
        initialAffinityOverridesSnapshot.putAll(customAffinityOverrides);
    }

    private HBox buildProceduralToolbar() {
        HBox proceduralToolbar = new HBox(8);
        proceduralToolbar.setAlignment(Pos.CENTER_LEFT);
        proceduralToolbar.setPadding(new Insets(6, 10, 6, 10));
        proceduralToolbar.setStyle("-fx-background-color: rgba(15, 23, 42, 0.4); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 6;");

        Label procLabel = new Label(I18n.getOrDefault("cultural.matrix.procedural_header", "ðŸª„ GÃ©nÃ©ration ProcÃ©durale & Heuristiques :"));
        procLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");

        configureToolbarBtn(btnGenGeo, "cultural.matrix.btn_gen_geo", "ðŸŒ Distance GÃ©o & Isoglosses",
                "cultural.matrix.btn_gen_geo_tt", "Calculer les affinitÃ©s par dÃ©croissance exponentielle de distance spatiale et linguistique.",
                this::generateAffinitiesByGeography);
        configureToolbarBtn(btnGenTraits, "cultural.matrix.btn_gen_traits", "ðŸ§¬ SimilaritÃ© des Traits (Mahalanobis)",
                "cultural.matrix.btn_gen_traits_tt", "Calculer les affinitÃ©s basÃ©es sur la distance euclidienne des traits ethnographiques multidimensionnels.",
                this::generateAffinitiesByTraits);
        configureToolbarBtn(btnGenStochastic, "cultural.matrix.btn_gen_stochastic", "ðŸŽ² Bruit Stochastique",
                "cultural.matrix.btn_gen_stochastic_tt", "Appliquer une variation alÃ©atoire rÃ©aliste gaussienne symÃ©trique.",
                this::generateAffinitiesStochastic);
        configureToolbarBtn(btnResetDefaults, "cultural.matrix.btn_reset_defaults", "ðŸ”„ DÃ©fauts",
                "cultural.matrix.btn_reset_defaults_tt", "Restaurer les affinitÃ©s historiques canoniques de cette Ã©poque.",
                () -> {
                    customAffinityOverrides.clear();
                    renderMatrixHeatmap();
                    updateQuickEditor();
                });

        proceduralToolbar.getChildren().addAll(procLabel, btnGenGeo, btnGenTraits, btnGenStochastic, btnResetDefaults);
        return proceduralToolbar;
    }

    private static void configureToolbarBtn(Button btn, String textKey, String defText, String ttKey, String defTt, Runnable action) {
        btn.setText(I18n.getOrDefault(textKey, defText));
        btn.getStyleClass().add("button-secondary");
        btn.setStyle("-fx-font-size: 10.5px;");
        btn.setTooltip(new Tooltip(I18n.getOrDefault(ttKey, defTt)));
        btn.setOnAction(e -> action.run());
    }

    private HBox buildFooterBar() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);

        btnExportCsv.setText(I18n.getOrDefault("cultural.matrix.btn_export_csv", "ðŸ’¾ Exporter Matrice (CSV)"));
        btnExportCsv.getStyleClass().add("button-secondary");
        btnExportCsv.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.export_csv", "Exporter la matrice complÃ¨te d'affinitÃ© culturelle NÃ—N sous format CSV standard.")));
        btnExportCsv.setOnAction(e -> CulturalAffinityMatrixExporter.exportMatrixCsv(this, currentEpoch, activeEntities, this::getAffinity));

        btnExportJson.setText(I18n.getOrDefault("cultural.matrix.btn_export_json", "ðŸ“„ Exporter Matrice (JSON)"));
        btnExportJson.getStyleClass().add("button-secondary");
        btnExportJson.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.export_json", "Exporter le registre culturel complet et la matrice d'affinitÃ© sous format JSON.")));
        btnExportJson.setOnAction(e -> CulturalAffinityMatrixExporter.exportMatrixJson(this, currentEpoch, scenarioStartYear, activeEntities, this::getAffinity));

        btnCancel.setText(I18n.getOrDefault("common.btn.cancel", "Annuler"));
        btnCancel.getStyleClass().add("button-secondary");
        btnCancel.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.cancel", "Annuler les modifications et restaurer l'Ã©tat initial des affinitÃ©s.")));
        btnCancel.setOnAction(e -> {
            customAffinityOverrides.clear();
            customAffinityOverrides.putAll(initialAffinityOverridesSnapshot);
            close();
        });

        btnClose.setText(I18n.getOrDefault("common.btn.close", "Fermer"));
        btnClose.getStyleClass().add("button-primary");
        btnClose.setStyle("-fx-font-weight: bold;");
        btnClose.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.close", "Valider et fermer l'inspecteur des affinitÃ©s culturelles.")));
        btnClose.setOnAction(e -> {
            if (targetScenario != null) {
                targetScenario.setCulturalAffinityMatrix(customAffinityOverrides);
                logger.info("Persisted {} custom cultural affinity overrides into scenario '{}'.",
                        customAffinityOverrides.size(), targetScenario.getName());
            }
            close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footer.getChildren().addAll(btnExportCsv, btnExportJson, spacer, btnCancel, btnClose);
        return footer;
    }

    private void setupQuickEditorPanel() {
        CulturalAffinityMatrixViewHelper.configureQuickEditorCard(
                quickEditorCard, quickEditorTitle, quickEditorPairLabel,
                quickAffinitySlider, quickAffinitySpinner, presetChipsBox,
                () -> !isUpdatingEditor && selectedI >= 0 && selectedJ >= 0,
                aff01 -> {
                    isUpdatingEditor = true;
                    try {
                        applyQuickEdit(aff01);
                    } finally {
                        isUpdatingEditor = false;
                    }
                });
    }

    private void applyQuickEdit(double affinity) {
        if (selectedI < 0 || selectedJ < 0 || selectedI >= activeEntities.size() || selectedJ >= activeEntities.size()) return;
        CulturalEntity e1 = activeEntities.get(selectedI);
        CulturalEntity e2 = activeEntities.get(selectedJ);
        double finalAff = (selectedI == selectedJ) ? 1.0 : Math.clamp(affinity, 0.0, 1.0);
        // Directed square matrix: modify only the directional link e1 -> e2
        customAffinityOverrides.put(e1.id() + ":" + e2.id(), finalAff);
        updateCellDisplay(selectedI, selectedJ);
    }

    private void updateCellDisplay(int i, int j) {
        CulturalAffinityMatrixViewHelper.updateCellDisplay(
                activeEntities, cellPanes, cellLabels, this::getAffinity, i, j, selectedI, selectedJ);
    }

    private void loadEpochRegistry(long epoch) {
        this.currentEpoch = epoch;
        activeEntities = CulturalEpochRegistryLoader.loadEpochRegistry(epoch);
        customAffinityOverrides.clear();
        selectedI = -1;
        selectedJ = -1;
        renderEntitiesTab();
        renderMatrixHeatmap();
        if (!activeEntities.isEmpty()) {
            selectCell(0, 1 < activeEntities.size() ? 1 : 0);
        } else {
            updateQuickEditor();
        }
    }

    private double getAffinity(CulturalEntity e1, CulturalEntity e2) {
        if (e1 == null || e2 == null) return 0.0;
        if (e1.id().equals(e2.id())) return 1.0;
        String key = e1.id() + ":" + e2.id();
        if (customAffinityOverrides.containsKey(key)) return customAffinityOverrides.get(key);
        return e1.affinityTowards(e2);
    }

    private void renderEntitiesTab() {
        CulturalAffinityMatrixViewHelper.renderEntitiesTab(entitiesBox, activeEntities);
    }

    private void renderMatrixHeatmap() {
        int n = activeEntities.size();
        if (n == 0) {
            matrixGrid.getChildren().clear();
            return;
        }
        cellPanes = new StackPane[n][n];
        cellLabels = new Label[n][n];
        CulturalAffinityMatrixViewHelper.buildMatrixGrid(
                matrixGrid, activeEntities, cellPanes, cellLabels,
                this::getAffinity, selectedI, selectedJ, this::selectCell);
    }

    private void selectCell(int i, int j) {
        int oldI = selectedI, oldJ = selectedJ;
        selectedI = i;
        selectedJ = j;
        if (oldI >= 0 && oldJ >= 0) {
            updateCellDisplay(oldI, oldJ);
            updateCellDisplay(oldJ, oldI);
        }
        if (selectedI >= 0 && selectedJ >= 0) {
            updateCellDisplay(selectedI, selectedJ);
            updateCellDisplay(selectedJ, selectedI);
        }
        updateQuickEditor();
    }

    private void updateQuickEditor() {
        if (selectedI < 0 || selectedJ < 0 || selectedI >= activeEntities.size() || selectedJ >= activeEntities.size()) {
            quickEditorPairLabel.setText("â€” " + I18n.getOrDefault("cultural.matrix.select_cell_hint", "SÃ©lectionnez une cellule de la matrice ci-dessus pour l'Ã©diter."));
            quickAffinitySlider.setDisable(true);
            quickAffinitySpinner.setDisable(true);
            presetChipsBox.setDisable(true);
            return;
        }
        CulturalEntity e1 = activeEntities.get(selectedI);
        CulturalEntity e2 = activeEntities.get(selectedJ);
        double aff = getAffinity(e1, e2);
        boolean isDiagonal = (selectedI == selectedJ);
        quickEditorPairLabel.setText(String.format("ðŸ·ï¸ %s â†’ %s %s", e1.getLocalizedName(), e2.getLocalizedName(), isDiagonal ? "(ðŸ”’ Auto-identitÃ©)" : ""));

        isUpdatingEditor = true;
        quickAffinitySlider.setValue(aff * 100.0);
        quickAffinitySpinner.getValueFactory().setValue(Math.round(aff * 1000.0) / 10.0);
        quickAffinitySlider.setDisable(isDiagonal);
        quickAffinitySpinner.setDisable(isDiagonal);
        presetChipsBox.setDisable(isDiagonal);
        isUpdatingEditor = false;
    }

    private void generateAffinitiesByGeography() {
        CulturalAffinityMatrixViewHelper.populateByGeography(activeEntities, customAffinityOverrides);
        renderMatrixHeatmap();
        updateQuickEditor();
    }

    private void generateAffinitiesByTraits() {
        CulturalAffinityMatrixViewHelper.populateByTraits(activeEntities, customAffinityOverrides);
        renderMatrixHeatmap();
        updateQuickEditor();
    }

    private void generateAffinitiesStochastic() {
        long seed = (targetScenario != null && targetScenario.getCulturalSeed() != 0L)
                ? targetScenario.getCulturalSeed()
                : ((targetScenario != null && targetScenario.getSeed() != 0L) ? targetScenario.getSeed() : 42L);
        CulturalAffinityMatrixViewHelper.populateStochastic(activeEntities, customAffinityOverrides, this::getAffinity, seed);
        renderMatrixHeatmap();
        updateQuickEditor();
    }
}

