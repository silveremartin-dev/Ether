/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.scenario.ScenarioConfigPaneBuilder;
import org.ether.society.ui.scenario.ScenarioEngineConfigSectionBuilder;
import org.ether.society.ui.scenario.ScenarioEventsSectionBuilder;
import org.ether.society.ui.util.ScenarioCellPopulationDistributor;

import java.util.List;

/**
 * Package-private layout assembler and start-button/preview coordinator for {@link ScenarioSetupPanel}.
 *
 * <p>Extracts UI pane construction, section builder wiring, snapshot start-button styling,
 * and initial population distribution so {@link ScenarioSetupPanel} stays focused on its
 * public coordination contract.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see ScenarioSetupPanel
 */
final class ScenarioSetupLayoutAssembler {

    private ScenarioSetupLayoutAssembler() {
        // Stateless package-private UI helper
    }

    /**
     * Initializes the two-column {@link BorderPane} layout of {@link ScenarioSetupPanel}.
     *
     * @param p host scenario setup panel
     */
    static void initUI(ScenarioSetupPanel p) {
        p.setPadding(new Insets(20));
        p.getStyleClass().add("glass-panel");
        p.setStyle("-fx-background-color: transparent;");

        VBox configPane = createConfigPane(p);
        configPane.setPrefWidth(480);
        configPane.setMinWidth(480);

        p.configScroll = new ScrollPane(configPane);
        p.configScroll.setFitToWidth(true);
        p.configScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        p.configScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        p.configScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        BorderPane leftSidebar = new BorderPane();
        leftSidebar.setPrefWidth(480);
        leftSidebar.setMinWidth(480);
        leftSidebar.setCenter(p.configScroll);

        if (p.bottomActionBox != null) {
            p.bottomActionBox.setPadding(new Insets(10, 15, 5, 0));
            p.bottomActionBox.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(56, 189, 248, 0.2); -fx-border-width: 1 0 0 0;");
            leftSidebar.setBottom(p.bottomActionBox);
        }

        VBox previewPane = createPreviewPane(p);
        p.setLeft(leftSidebar);
        p.setCenter(previewPane);
    }

    /**
     * Builds the scrollable configuration sidebar containing all scenario sections.
     *
     * @param p host scenario setup panel
     * @return assembled configuration VBox
     */
    static VBox createConfigPane(ScenarioSetupPanel p) {
        p.headerLabel = new Label(I18n.getOrDefault("scenario.panel.title", "📜 Scenario Configuration & Setup"));
        p.headerLabel.getStyleClass().add("label-title");
        p.headerLabel.setAlignment(Pos.CENTER);
        p.headerLabel.setMaxWidth(Double.MAX_VALUE);

        p.validationBannerHeaderLabel = new Label(I18n.getOrDefault("scenario.validation.header_errors", "⚠️ VALIDATION ERRORS DETECTED — PLEASE CORRECT THE FOLLOWING POINTS:"));
        p.validationBannerHeaderLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 13px;");
        p.validationErrorLabel = new Label();
        p.validationErrorLabel.setWrapText(true);
        p.validationErrorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 12px;");
        p.validationErrorBanner = ScenarioConfigPaneBuilder.createValidationErrorBanner(p.validationBannerHeaderLabel, p.validationErrorLabel);

        p.scenarioPresetBar = p.presetManager.getPresetBar();
        List<Scenario> builtInScenarios = p.presetManager.getBuiltInScenarios();
        Scenario defaultScenario = builtInScenarios.isEmpty() ? null : builtInScenarios.get(0);

        VBox inheritedSection = p.planetSyncManager.buildSection();
        p.planetPresetCombo = p.planetSyncManager.getPlanetPresetCombo();
        p.ecologyPresetCombo = p.planetSyncManager.getEcologyPresetCombo();
        p.inheritedContextLabel = p.planetSyncManager.getInheritedContextLabel();

        VBox section1 = p.spatiotemporalSectionBuilder.buildSection();
        p.startYearSpinner = p.spatiotemporalSectionBuilder.getStartYearSpinner();
        p.endYearSpinner = p.spatiotemporalSectionBuilder.getEndYearSpinner();
        p.h3ResolutionCombo = p.spatiotemporalSectionBuilder.getH3ResolutionCombo();
        p.targetCohortSizeSpinner = p.spatiotemporalSectionBuilder.getTargetCohortSizeSpinner();
        p.temporalResolutionCombo = p.spatiotemporalSectionBuilder.getTemporalResolutionCombo();

        VBox popSection = p.demographicSectionBuilder.buildSection();
        VBox cultureSection = p.culturalSectionBuilder.buildSection();
        p.cultSeedField = p.culturalSectionBuilder.getCultSeedField();

        VBox clippingSection = p.spatialBoundsBuilder.buildSection();
        if (p.engineConfigSectionBuilder == null) {
            p.engineConfigSectionBuilder = new ScenarioEngineConfigSectionBuilder(p::notifyParamChange, className -> {});
        }
        VBox oceanOptSection = p.engineConfigSectionBuilder.buildSection();

        if (p.eventsSectionBuilder == null) {
            p.eventsSectionBuilder = new ScenarioEventsSectionBuilder(p.eventsList, p::notifyParamChange);
        }
        VBox eventsSection = p.eventsSectionBuilder.buildEventsSection(null, null,
                () -> p.startYearSpinner != null ? p.startYearSpinner.getValue() : -8000);

        p.generateBtn = new Button();
        p.generateBtn.setMaxWidth(Double.MAX_VALUE);
        p.generateBtn.getStyleClass().add("button-secondary");
        p.generateBtn.setOnAction(e -> p.generatePreview());
        p.generateBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.preview", "Quick preview of demographic distribution.")));

        p.progressBar = new ProgressBar(0);
        p.progressBar.setMaxWidth(Double.MAX_VALUE);
        p.progressBar.setVisible(false);
        p.progressBar.setManaged(false);

        p.progressStatusLabel = new Label();
        p.progressStatusLabel.getStyleClass().add("control-label");
        p.progressStatusLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        p.progressStatusLabel.setVisible(false);
        p.progressStatusLabel.setManaged(false);

        VBox liveDiagnosticCard = p.liveDiagnosticSection.buildSection();
        ScenarioSetupDiagnosticHelper.updateLiveDiagnosticBlock(p);

        p.startBtn = new Button();
        p.startBtn.setPrefHeight(50);
        p.startBtn.setMaxWidth(Double.MAX_VALUE);
        p.startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
        p.startBtn.setOnAction(e -> p.simulationLauncher.handleStartOrCancel());
        p.startBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.start", "Calculate H3 cells and launch simulation.")));
        p.simulationLauncher.bindControls(p.startBtn, p.progressBar, p.progressStatusLabel);

        p.scenarioDescLabel = new Label(I18n.getOrDefault("scenario.section.description", "📖 Detailed Description, Initial Conditions & Key Observables:"));
        p.scenarioDescLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 6 0 2 0;");

        p.scenarioDescriptionArea = new TextArea();
        p.scenarioDescriptionArea.setPrefRowCount(9);
        p.scenarioDescriptionArea.setWrapText(true);
        p.scenarioDescriptionArea.getStyleClass().add("scenario-description-area");
        p.scenarioDescriptionArea.setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0; -fx-background-color: rgba(15, 23, 42, 0.6); -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 4; -fx-background-radius: 4;");
        p.scenarioDescriptionArea.textProperty().addListener((obs, oldV, newV) -> p.notifyParamChange());

        p.btnAutoEpochScenario = new Button(I18n.getOrDefault("scenario.btn.auto_epoch_wizard", "✨ Créer un Scénario Automatique par Date..."));
        p.btnAutoEpochScenario.setMaxWidth(Double.MAX_VALUE);
        p.btnAutoEpochScenario.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        p.btnAutoEpochScenario.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.auto_epoch_wizard", "Ouvre l'assistant de scénario temporel pour configurer en un clic les cartes d'élévation, biomes, ressources, démographie et moteurs de Type B compatibles pour n'importe quelle date.")));
        p.btnAutoEpochScenario.setOnAction(e -> p.openAutoEpochScenarioDialog());

        p.scenarioPresetHeader = new Label(I18n.getOrDefault("scenario.section.presets", "🎛️ GLOBAL PRESETS & SCENARIO SAVE"));
        VBox scenarioPresetSection = ScenarioConfigPaneBuilder.buildScenarioPresetSection(
                p.scenarioPresetHeader, p.scenarioPresetBar, p.btnAutoEpochScenario, p.scenarioDescLabel, p.scenarioDescriptionArea);

        ToggleGroup modeGroup = new ToggleGroup();
        p.radioNewSimulation = new RadioButton(I18n.getOrDefault("scenario.radio.start_fresh", "🌱 Start a new simulation from scratch (Year T₀)"));
        p.radioNewSimulation.setStyle("-fx-font-weight: bold;");
        p.radioNewSimulation.setSelected(true);
        p.radioNewSimulation.setToggleGroup(modeGroup);
        VBox snapshotSection = p.snapshotInspector.buildSnapshotSection(modeGroup, p.radioNewSimulation);

        VBox bundleSection = p.bundleManager.buildSection();
        p.bottomActionBox = ScenarioConfigPaneBuilder.buildBottomActionBox(p.progressBar, p.progressStatusLabel, p.startBtn);

        VBox root = ScenarioConfigPaneBuilder.assembleConfigPane(
                p.headerLabel, p.validationErrorBanner, scenarioPresetSection, inheritedSection, section1,
                popSection, cultureSection, clippingSection, oceanOptSection, eventsSection,
                snapshotSection, bundleSection, liveDiagnosticCard);

        p.scenarioPresetBar.setPresets(builtInScenarios, defaultScenario);
        if (defaultScenario != null) {
            p.applyScenarioToUI(defaultScenario);
        }
        return root;
    }

    static VBox createPreviewPane(ScenarioSetupPanel p) {
        VBox root = p.previewPaneBuilder.buildPreviewPane();
        p.previewModeCombo = p.previewPaneBuilder.getPreviewModeCombo();
        p.previewStatusLabel = p.previewPaneBuilder.getPreviewStatusLabel();
        p.btnReliefOverlay = p.previewPaneBuilder.getBtnReliefOverlay();
        return root;
    }

    /**
     * Updates the start simulation button text, color, tooltip, and snapshot resume lock state.
     *
     * @param p host scenario setup panel
     */
    static void updateStartButtonLabel(ScenarioSetupPanel p) {
        if (p.startBtn == null) return;
        boolean dirty = p.isDirty();
        Scenario s = p.getScenario();
        long startYr = s != null ? s.getStartDateYear() : -100000L;
        long endYr = s != null ? s.getEndDateYear() : 2026L;

        if (dirty) {
            p.snapshotInspector.setResumeDisabled(true);
            p.startBtn.setText(I18n.getOrDefault("scenario.btn.start_modified", "🌱 CALCULER & DÉMARRER LA SIMULATION (Paramètres Modifiés)"));
            p.startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #d97706; -fx-text-fill: white; -fx-background-radius: 6;");
            p.startBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.start_modified",
                    "Lance une simulation avec vos paramètres personnalisés sans écraser les snapshots du scénario d'origine.")));
            return;
        }

        p.snapshotInspector.setResumeDisabled(!p.snapshotInspector.hasSnapshots());
        p.snapshotInspector.updateStartButtonContext(p.startBtn, startYr, endYr);
    }

    /**
     * Distributes initial population and baseline capital stock across preview H3 cells.
     *
     * @param p     host scenario setup panel
     * @param cells preview H3 cells
     */
    static void distributeInitialPopulation(ScenarioSetupPanel p, List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;
        long totalPop = p.demographicSectionBuilder != null ? p.demographicSectionBuilder.getInitialHumanCount() : 1_000_000L;
        long startYear = p.startYearSpinner != null && p.startYearSpinner.getValue() != null ? p.startYearSpinner.getValue() : -8000;
        double capitalK0 = ScenarioSetupPanel.computeAutoCapitalFromYear(startYear);
        String pattern = p.demographicSectionBuilder != null ? p.demographicSectionBuilder.getDensityPattern() : "UNBIASED_NATURAL";
        boolean isEarthPreset = p.activePlanetPreset != null && p.activePlanetPreset.elevationUseImport()
                && "earth".equalsIgnoreCase(p.activePlanetPreset.elevationMapSource());

        ScenarioCellPopulationDistributor.distributeInitialPopulation(
                cells, totalPop, capitalK0, pattern, isEarthPreset, startYear,
                p.demographicSectionBuilder != null ? p.demographicSectionBuilder.getCustomDensityImage() : null, p.getScenario());
    }
}
