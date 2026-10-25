/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.model.EcologyPreset;
import org.ether.society.model.Scenario;
import org.ether.society.ui.scenario.ScenarioBundleManager;
import org.ether.society.ui.scenario.ScenarioClimateEventModel;
import org.ether.society.ui.scenario.ScenarioConfigPaneBuilder;
import org.ether.society.ui.scenario.ScenarioCulturalSectionBuilder;
import org.ether.society.ui.scenario.ScenarioDefaultValueManager;
import org.ether.society.ui.scenario.ScenarioDemographicSectionBuilder;
import org.ether.society.ui.scenario.ScenarioEngineConfigSectionBuilder;
import org.ether.society.ui.scenario.ScenarioEpochCatalog;
import org.ether.society.ui.scenario.ScenarioEventsSectionBuilder;
import org.ether.society.ui.scenario.ScenarioLiveDiagnosticSection;
import org.ether.society.ui.scenario.ScenarioPlanetSyncManager;
import org.ether.society.ui.scenario.ScenarioPresetManager;
import org.ether.society.ui.scenario.ScenarioPreviewPaneBuilder;
import org.ether.society.ui.scenario.ScenarioSanityValidator;
import org.ether.society.ui.scenario.ScenarioSimulationLauncher;
import org.ether.society.ui.scenario.ScenarioSnapshotInspector;
import org.ether.society.ui.scenario.ScenarioSpatialBoundsSectionBuilder;
import org.ether.society.ui.scenario.ScenarioSpatiotemporalSectionBuilder;
import org.ether.society.ui.scenario.ScenarioThermodynamicAutocalc;
import org.ether.society.ui.scenario.ScenarioValidationHelper;
import org.ether.society.ui.util.ScenarioCellPopulationDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Panel for setting up civilization, initial population distribution, historical scenarios,
 * and civilizational start parameters.
 *
 * <p>Coordinates delegated autonomous section builders for planetary synchronization, spatiotemporal
 * boundaries, population distribution, cultural multi-tensors, physics engine configurations, and scheduled
 * historical perturbations.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioSetupPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioSetupPanel.class);

    // Inherited Presets Controls
    ComboBox<PlanetPreset> planetPresetCombo;
    ComboBox<EcologyPreset> ecologyPresetCombo;
    private Label inheritedContextLabel;

    // Standardized Preset Bar for Scenarios
    private Label scenarioPresetHeader;
    PresetControlBar<Scenario> scenarioPresetBar;
    private Button btnAutoEpochScenario;
    private VBox bottomActionBox;

    // Form Controls
    private Label scenarioDescLabel;
    TextArea scenarioDescriptionArea;
    Spinner<Integer> startYearSpinner;
    Spinner<Integer> endYearSpinner;
    ComboBox<Integer> h3ResolutionCombo;
    private Spinner<Integer> targetCohortSizeSpinner;
    private ComboBox<Double> temporalResolutionCombo;

    // State & Change Listener Tracking
    boolean isUpdatingFromPreset = false;

    // Delegated Section Builders and Managers
    final ScenarioPlanetSyncManager planetSyncManager = new ScenarioPlanetSyncManager(ScenarioHostContextFactory.createPlanetSyncHostContext(this));
    final ScenarioSpatiotemporalSectionBuilder spatiotemporalSectionBuilder = new ScenarioSpatiotemporalSectionBuilder(ScenarioHostContextFactory.createSpatiotemporalHostContext(this));
    final ScenarioSpatialBoundsSectionBuilder spatialBoundsBuilder = new ScenarioSpatialBoundsSectionBuilder(ScenarioHostContextFactory.createSpatialBoundsHostContext(this));
    final ScenarioBundleManager bundleManager = new ScenarioBundleManager(ScenarioHostContextFactory.createBundleHostContext(this));
    final ScenarioDemographicSectionBuilder demographicSectionBuilder = new ScenarioDemographicSectionBuilder(ScenarioHostContextFactory.createDemographicHostContext(this));
    final ScenarioCulturalSectionBuilder culturalSectionBuilder = new ScenarioCulturalSectionBuilder(ScenarioHostContextFactory.createCulturalHostContext(this));
    final ScenarioPreviewPaneBuilder previewPaneBuilder = new ScenarioPreviewPaneBuilder(ScenarioHostContextFactory.createPreviewPaneHostContext(this));
    ScenarioEngineConfigSectionBuilder engineConfigSectionBuilder;
    final ScenarioSimulationLauncher simulationLauncher;
    final ScenarioPresetManager presetManager;
    final ScenarioConfigSyncHandler configSyncHandler = new ScenarioConfigSyncHandler(this);
    private final ScenarioLiveDiagnosticSection liveDiagnosticSection = new ScenarioLiveDiagnosticSection();
    private final ScenarioSnapshotInspector snapshotInspector = new ScenarioSnapshotInspector(meta -> {
        updateStartButtonLabel();
        notifyParamChange();
    });

    // Map Preview Controls
    ComboBox<String> previewModeCombo;
    private ToggleButton btnReliefOverlay;
    Label previewStatusLabel;

    // Execution & Calculation Controls
    private ProgressBar progressBar;
    private Label progressStatusLabel;
    private Button startBtn;
    private Button generateBtn;

    // State
    List<H3Cell> currentPreviewCells;
    PlanetPreset activePlanetPreset;
    private final Consumer<Scenario> onStartSimulation;
    private final org.ether.society.persistence.ScenarioRepository scenarioRepo;
    BiConsumer<PlanetPreset, EcologyPreset> onScenarioLoadedCallback;
    Supplier<Boolean> isSimulationRunningSupplier;

    // UI Titles & Labels
    private Label headerLabel;
    private ScrollPane configScroll;
    VBox validationErrorBanner;
    private Label validationBannerHeaderLabel;
    Label validationErrorLabel;

    // Seed & Random Events Controls
    TextField demoSeedField;
    TextField cultSeedField;
    CheckBox randomEventsCheckBox;
    CheckBox earthLeadersCheckBox;
    CheckBox proceduralLeadersCheckBox;

    // Events section
    private ScenarioEventsSectionBuilder eventsSectionBuilder;
    ObservableList<ClimateEvent> eventsList;
    private RadioButton radioNewSimulation;

    private Supplier<PlanetGeneratorPanel> planetPanelSupplier;
    private Supplier<ResourceDistributionPanel> resourcePanelSupplier;

    public void setIsSimulationRunningSupplier(Supplier<Boolean> isSimulationRunningSupplier) {
        this.isSimulationRunningSupplier = isSimulationRunningSupplier;
    }

    /** Internal model for a scheduled planetary event */
    public static class ClimateEvent extends ScenarioClimateEventModel {
        public ClimateEvent(String type, String name, int year, double lat, double lon, double depth, double mag) {
            super(type, name, year, lat, lon, depth, mag);
        }
    }

    public boolean isDirty() {
        return presetManager != null && presetManager.isDirty();
    }

    public boolean promptSaveIfDirty(javafx.stage.Window owner) {
        return presetManager == null || presetManager.promptSaveIfDirty(owner);
    }

    public PresetControlBar<Scenario> getPresetBar() {
        return presetManager != null ? presetManager.getPresetBar() : scenarioPresetBar;
    }

    public ScenarioSetupPanel(Consumer<Scenario> onStartSimulation) {
        this.onStartSimulation = onStartSimulation;
        this.scenarioRepo = new org.ether.society.persistence.ScenarioRepository();
        this.presetManager = ScenarioHostContextFactory.createPresetManager(this, this.scenarioRepo);
        this.simulationLauncher = ScenarioHostContextFactory.createSimulationLauncher(this, this.scenarioRepo, onStartSimulation);
        initUI();
        updateTexts();

        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
        
        org.ether.society.data.sync.GitHubContentSyncService.addListener(new org.ether.society.data.sync.GitHubContentSyncService.SyncEventListener() {
            @Override
            public void onSyncCompleted(int downloaded, int checked) {
                if (downloaded > 0) refreshScenarioPresets();
            }
            @Override
            public void onScenarioDiscovered(String name) {
                refreshScenarioPresets();
            }
        });

        javafx.application.Platform.runLater(this::loadEarthHistoricalEvents);
    }

    void notifyParamChange() {
        if (!isUpdatingFromPreset && scenarioPresetBar != null) {
            scenarioPresetBar.notifyParametersChanged();
        }
        updateLiveDiagnosticBlock();
        updateDefaultValueIndicators();
        updateStartButtonLabel();
        validateScenarioSetup();
    }

    <T> void attachDefaultValueHandling(Control control, T defaultValue, Runnable resetAction) {
        ScenarioDefaultValueManager.attachDefaultValueHandling(control, defaultValue, resetAction, this::notifyParamChange);
    }

    private void updateDefaultValueIndicators() {
        if (isUpdatingFromPreset) return;
        ScenarioDefaultValueManager.highlightControlIfModified(startYearSpinner, false);
        ScenarioDefaultValueManager.highlightControlIfModified(endYearSpinner, false);
        ScenarioDefaultValueManager.highlightControlIfModified(targetCohortSizeSpinner, false);
        ScenarioDefaultValueManager.highlightControlIfModified(temporalResolutionCombo, false);
        if (demographicSectionBuilder != null) {
            ScenarioDefaultValueManager.highlightControlIfModified(demographicSectionBuilder.getInitialHumanCountSpinner(), false);
        }
        if (culturalSectionBuilder != null) {
            var dimSp = culturalSectionBuilder.getCultureVectorDimSpinner();
            ScenarioDefaultValueManager.highlightControlIfModified(dimSp, dimSp != null && dimSp.getValue() != null && dimSp.getValue() != 9);
            var diffSp = culturalSectionBuilder.getCulturalDiffusionRateSpinner();
            ScenarioDefaultValueManager.highlightControlIfModified(diffSp, diffSp != null && diffSp.getValue() != null && Math.abs(diffSp.getValue() - 0.05) > 0.001);
            var mutSp = culturalSectionBuilder.getCulturalMutationRateSpinner();
            ScenarioDefaultValueManager.highlightControlIfModified(mutSp, mutSp != null && mutSp.getValue() != null && Math.abs(mutSp.getValue() - 0.01) > 0.001);
        }
    }

    public void resetAllToDefaults() {
        boolean oldUpdating = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            Scenario def = new Scenario();
            def.setName(I18n.getOrDefault("scenario.default_name", "New Baseline Scenario"));
            applyScenarioToUI(def);
        } finally {
            isUpdatingFromPreset = oldUpdating;
        }
        notifyParamChange();
        updateDefaultValueIndicators();
        if (scenarioDescriptionArea != null) {
            scenarioDescriptionArea.setText(I18n.getOrDefault("scenario.msg.reset_default", "Scenario reset to canonical simulation default values."));
        }
    }

    public void setInheritedContext(PlanetPreset planetPreset, String ecologyName) {
        boolean oldUpdating = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            planetSyncManager.setInheritedContext(planetPreset, ecologyName);
            this.activePlanetPreset = planetSyncManager.getActivePlanetPreset();
        } finally {
            isUpdatingFromPreset = oldUpdating;
        }
    }

    public PlanetPreset findPlanetPresetByName(String name) {
        return planetSyncManager.findPlanetPresetByName(name);
    }

    private void initUI() {
        setPadding(new Insets(20));
        getStyleClass().add("glass-panel");
        setStyle("-fx-background-color: transparent;");

        VBox configPane = createConfigPane();
        configPane.setPrefWidth(480);
        configPane.setMinWidth(480);

        this.configScroll = new ScrollPane(configPane);
        configScroll.setFitToWidth(true);
        configScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        configScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        configScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        BorderPane leftSidebar = new BorderPane();
        leftSidebar.setPrefWidth(480);
        leftSidebar.setMinWidth(480);
        leftSidebar.setCenter(configScroll);

        if (bottomActionBox != null) {
            bottomActionBox.setPadding(new Insets(10, 15, 5, 0));
            bottomActionBox.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(56, 189, 248, 0.2); -fx-border-width: 1 0 0 0;");
            leftSidebar.setBottom(bottomActionBox);
        }

        VBox previewPane = createPreviewPane();
        setLeft(leftSidebar);
        setCenter(previewPane);
    }

    private VBox createConfigPane() {
        headerLabel = new Label(I18n.getOrDefault("scenario.panel.title", "📜 Scenario Configuration & Setup"));
        headerLabel.getStyleClass().add("label-title");
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setMaxWidth(Double.MAX_VALUE);

        validationBannerHeaderLabel = new Label(I18n.getOrDefault("scenario.validation.header_errors", "⚠️ VALIDATION ERRORS DETECTED — PLEASE CORRECT THE FOLLOWING POINTS:"));
        validationBannerHeaderLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 13px;");
        validationErrorLabel = new Label();
        validationErrorLabel.setWrapText(true);
        validationErrorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 12px;");
        validationErrorBanner = ScenarioConfigPaneBuilder.createValidationErrorBanner(validationBannerHeaderLabel, validationErrorLabel);

        this.scenarioPresetBar = presetManager.getPresetBar();
        List<Scenario> builtInScenarios = presetManager.getBuiltInScenarios();
        Scenario defaultScenario = builtInScenarios.isEmpty() ? null : builtInScenarios.get(0);

        VBox inheritedSection = planetSyncManager.buildSection();
        planetPresetCombo = planetSyncManager.getPlanetPresetCombo();
        ecologyPresetCombo = planetSyncManager.getEcologyPresetCombo();
        inheritedContextLabel = planetSyncManager.getInheritedContextLabel();

        VBox section1 = spatiotemporalSectionBuilder.buildSection();
        startYearSpinner = spatiotemporalSectionBuilder.getStartYearSpinner();
        endYearSpinner = spatiotemporalSectionBuilder.getEndYearSpinner();
        h3ResolutionCombo = spatiotemporalSectionBuilder.getH3ResolutionCombo();
        targetCohortSizeSpinner = spatiotemporalSectionBuilder.getTargetCohortSizeSpinner();
        temporalResolutionCombo = spatiotemporalSectionBuilder.getTemporalResolutionCombo();

        VBox popSection = demographicSectionBuilder.buildSection();
        VBox cultureSection = createCulturalVectorAndLayersSection();
        VBox clippingSection = spatialBoundsBuilder.buildSection();
        VBox oceanOptSection = createOceanOptimizationSection();
        VBox eventsSection = createEventsSection();

        generateBtn = new Button();
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        generateBtn.getStyleClass().add("button-secondary");
        generateBtn.setOnAction(e -> generatePreview());
        generateBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.preview", "Quick preview of demographic distribution.")));

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        progressStatusLabel = new Label();
        progressStatusLabel.getStyleClass().add("control-label");
        progressStatusLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        progressStatusLabel.setVisible(false);
        progressStatusLabel.setManaged(false);

        VBox liveDiagnosticCard = liveDiagnosticSection.buildSection();
        updateLiveDiagnosticBlock();

        startBtn = new Button();
        startBtn.setPrefHeight(50);
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
        startBtn.setOnAction(e -> simulationLauncher.handleStartOrCancel());
        startBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.start", "Calculate H3 cells and launch simulation.")));
        simulationLauncher.bindControls(startBtn, progressBar, progressStatusLabel);

        scenarioDescLabel = new Label(I18n.getOrDefault("scenario.section.description", "📖 Detailed Description, Initial Conditions & Key Observables:"));
        scenarioDescLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 6 0 2 0;");

        scenarioDescriptionArea = new TextArea();
        scenarioDescriptionArea.setPrefRowCount(9);
        scenarioDescriptionArea.setWrapText(true);
        scenarioDescriptionArea.getStyleClass().add("scenario-description-area");
        scenarioDescriptionArea.setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0; -fx-background-color: rgba(15, 23, 42, 0.6); -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 4; -fx-background-radius: 4;");
        scenarioDescriptionArea.textProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        btnAutoEpochScenario = new Button(I18n.getOrDefault("scenario.btn.auto_epoch_wizard", "✨ Créer un Scénario Automatique par Date..."));
        btnAutoEpochScenario.setMaxWidth(Double.MAX_VALUE);
        btnAutoEpochScenario.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        btnAutoEpochScenario.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.auto_epoch_wizard", "Ouvre l'assistant de scénario temporel pour configurer en un clic les cartes d'élévation, biomes, ressources, démographie et moteurs de Type B compatibles pour n'importe quelle date.")));
        btnAutoEpochScenario.setOnAction(e -> openAutoEpochScenarioDialog());

        scenarioPresetHeader = new Label(I18n.getOrDefault("scenario.section.presets", "🎛️ GLOBAL PRESETS & SCENARIO SAVE"));
        VBox scenarioPresetSection = ScenarioConfigPaneBuilder.buildScenarioPresetSection(scenarioPresetHeader, scenarioPresetBar, btnAutoEpochScenario, scenarioDescLabel, scenarioDescriptionArea);
        VBox snapshotSection = createSnapshotSection();
        VBox bundleSection = bundleManager.buildSection();
        bottomActionBox = ScenarioConfigPaneBuilder.buildBottomActionBox(progressBar, progressStatusLabel, startBtn);

        VBox root = ScenarioConfigPaneBuilder.assembleConfigPane(
                headerLabel, validationErrorBanner, scenarioPresetSection, inheritedSection, section1,
                popSection, cultureSection, clippingSection, oceanOptSection, eventsSection,
                snapshotSection, bundleSection, liveDiagnosticCard
        );

        scenarioPresetBar.setPresets(builtInScenarios, defaultScenario);
        if (defaultScenario != null) {
            applyScenarioToUI(defaultScenario);
        }
        return root;
    }

    private VBox createSnapshotSection() {
        ToggleGroup modeGroup = new ToggleGroup();
        radioNewSimulation = new RadioButton(I18n.getOrDefault("scenario.radio.start_fresh", "🌱 Start a new simulation from scratch (Year T₀)"));
        radioNewSimulation.setStyle("-fx-font-weight: bold;");
        radioNewSimulation.setSelected(true);
        radioNewSimulation.setToggleGroup(modeGroup);
        return snapshotInspector.buildSnapshotSection(modeGroup, radioNewSimulation);
    }

    public void refreshScenarioPresets() {
        if (presetManager != null) {
            presetManager.refreshScenarioPresets();
        }
    }

    public void refreshSnapshotList() {
        snapshotInspector.refreshSnapshotListForScenario(getScenario());
        updateStartButtonLabel();
    }

    public void refreshSnapshotListForScenario(Scenario s) {
        snapshotInspector.refreshSnapshotListForScenario(s);
        updateStartButtonLabel();
    }

    public boolean isResumeFromSnapshotSelected() {
        return snapshotInspector.isResumeFromSnapshotSelected();
    }

    public org.ether.society.persistence.SaveMetadata getSelectedSnapshotMetadata() {
        return snapshotInspector.getSelectedSnapshotMetadata();
    }

    public String getSelectedSnapshotId() {
        return snapshotInspector.getSelectedSnapshotId();
    }

    void updateStartButtonLabel() {
        if (startBtn == null) return;
        boolean dirty = isDirty();
        Scenario s = getScenario();
        long startYr = s != null ? s.getStartDateYear() : -100000L;
        long endYr = s != null ? s.getEndDateYear() : 2026L;

        if (dirty) {
            snapshotInspector.setResumeDisabled(true);
            startBtn.setText(I18n.getOrDefault("scenario.btn.start_modified", "🌱 CALCULER & DÉMARRER LA SIMULATION (Paramètres Modifiés)"));
            startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #d97706; -fx-text-fill: white; -fx-background-radius: 6;");
            startBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.start_modified",
                "Lance une simulation avec vos paramètres personnalisés sans écraser les snapshots du scénario d'origine.")));
            return;
        }

        snapshotInspector.setResumeDisabled(!snapshotInspector.hasSnapshots());
        snapshotInspector.updateStartButtonContext(startBtn, startYr, endYr);
    }

    private void updateLiveDiagnosticBlock() {
        if (liveDiagnosticSection == null) return;
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        if (p == null) p = PlanetPreset.EARTH_LIKE;
        EcologyPreset eco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : EcologyPreset.EARTH_STANDARD;
        String demoSrc = demographicSectionBuilder != null && demographicSectionBuilder.getDemoSourceCombo() != null ? demographicSectionBuilder.getDemoSourceCombo().getValue() : null;
        Image elevImg = getElevationImageForPreset(p);
        int dimsCount = culturalSectionBuilder != null && culturalSectionBuilder.getCultureVectorDimSpinner() != null && culturalSectionBuilder.getCultureVectorDimSpinner().getValue() != null ? culturalSectionBuilder.getCultureVectorDimSpinner().getValue() : 9;
        long startY = startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000L;
        long initPop = demographicSectionBuilder != null ? demographicSectionBuilder.getInitialHumanCount() : 1_000_000L;

        liveDiagnosticSection.updateDiagnostics(
                p,
                eco,
                demoSrc,
                elevImg,
                demographicSectionBuilder != null ? demographicSectionBuilder.getCustomDensityImage() : null,
                dimsCount,
                culturalSectionBuilder != null ? culturalSectionBuilder.getCustomTensorImages() : Collections.emptyMap(),
                culturalSectionBuilder != null ? culturalSectionBuilder::getCulturalTensorTitle : i -> "Cultural Tensor #" + (i + 1),
                startY,
                initPop,
                getScenarioValidationErrors(),
                ScenarioSanityValidator.getOrCreateJitReport()
        );
    }

    public void applyScenarioToUI(Scenario s) {
        configSyncHandler.applyScenarioToUI(s);
    }

    private VBox createOceanOptimizationSection() {
        if (engineConfigSectionBuilder == null) {
            engineConfigSectionBuilder = new ScenarioEngineConfigSectionBuilder(
                this::notifyParamChange,
                className -> {}
            );
        }
        return engineConfigSectionBuilder.buildSection();
    }

    private VBox createCulturalVectorAndLayersSection() {
        VBox section = culturalSectionBuilder.buildSection();
        cultSeedField = culturalSectionBuilder.getCultSeedField();
        return section;
    }

    private VBox createPreviewPane() {
        VBox root = previewPaneBuilder.buildPreviewPane();
        previewModeCombo = previewPaneBuilder.getPreviewModeCombo();
        previewStatusLabel = previewPaneBuilder.getPreviewStatusLabel();
        btnReliefOverlay = previewPaneBuilder.getBtnReliefOverlay();
        return root;
    }

    public void generatePreview() {
        if (previewPaneBuilder != null) {
            previewPaneBuilder.generatePreview();
        }
    }

    void distributeInitialPopulation(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;
        long totalPop = demographicSectionBuilder != null ? demographicSectionBuilder.getInitialHumanCount() : 1_000_000L;
        double capitalK0 = computeAutoCapitalFromYear(startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000);
        String pattern = demographicSectionBuilder != null ? demographicSectionBuilder.getDensityPattern() : "UNBIASED_NATURAL";
        boolean isEarthPreset = activePlanetPreset != null && activePlanetPreset.elevationUseImport()
                && "earth".equalsIgnoreCase(activePlanetPreset.elevationMapSource());
        long startYear = startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000;

        ScenarioCellPopulationDistributor.distributeInitialPopulation(
                cells, totalPop, capitalK0, pattern, isEarthPreset, startYear,
                demographicSectionBuilder != null ? demographicSectionBuilder.getCustomDensityImage() : null, getScenario()
        );
    }

    public void drawPreview() {
        if (previewPaneBuilder != null) {
            previewPaneBuilder.drawPreview();
        }
    }

    public void updatePreviewModesCombo() {
        if (previewPaneBuilder != null) {
            previewPaneBuilder.updatePreviewModesCombo();
        }
    }

    public void updatePreviewTitleText() {
        if (previewPaneBuilder != null) {
            previewPaneBuilder.updatePreviewTitleText();
        }
    }

    public void updateBottomLegend() {
        if (previewPaneBuilder != null) {
            previewPaneBuilder.updateBottomLegend();
        }
    }

    Image getElevationImageForPreset(PlanetPreset planet) {
        return planetSyncManager.getElevationImageForPreset(planet);
    }

    public void setProgressControls(ProgressBar bar, Label label, javafx.scene.Node overlayContainer) {
        simulationLauncher.setProgressControls(bar, label, overlayContainer);
    }

    public void invalidateGeneratedState() {
        simulationLauncher.resetStartButtonState();
    }

    public void startSimulationDeferred() {
        simulationLauncher.startSimulationDeferred();
    }

    private VBox createEventsSection() {
        if (eventsSectionBuilder == null) {
            eventsSectionBuilder = new ScenarioEventsSectionBuilder(eventsList, this::notifyParamChange);
        }
        return eventsSectionBuilder.buildEventsSection(null, null,
                () -> startYearSpinner != null ? startYearSpinner.getValue() : -8000);
    }

    void loadEarthHistoricalEvents() {
        if (eventsSectionBuilder != null) {
            eventsSectionBuilder.loadEarthBiophysicalEvents(startYearSpinner != null ? startYearSpinner.getValue() : -8000, true);
        }
    }

    public static double computeAutoCapitalFromYear(long year) {
        return ScenarioThermodynamicAutocalc.computeAutoCapitalFromYear(year);
    }

    public void applyEarthPreset() {
        if (planetPresetCombo != null) {
            planetPresetCombo.setValue(PlanetPreset.EARTH_LIKE);
        }
    }

    public void updateTexts() {
        boolean oldUpdating = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            if (headerLabel != null) headerLabel.setText(I18n.getOrDefault("scenario.panel.title", "📜 Scenario Configuration & Setup"));
            if (btnAutoEpochScenario != null) {
                btnAutoEpochScenario.setText(I18n.getOrDefault("scenario.btn.auto_epoch_wizard", "✨ Créer un Scénario Automatique par Date..."));
                btnAutoEpochScenario.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.auto_epoch_wizard", "Ouvre l'assistant de scénario temporel pour configurer en un clic les cartes d'élévation, biomes, ressources, démographie et moteurs de Type B compatibles pour n'importe quelle date.")));
            }
            if (scenarioPresetBar != null) scenarioPresetBar.updateTexts();
            if (scenarioDescLabel != null) scenarioDescLabel.setText(I18n.getOrDefault("scenario.section.description", "📖 Detailed Description, Initial Conditions & Key Observables:"));
            if (startBtn != null) startBtn.setText(I18n.get("scenario.start_btn"));
            if (generateBtn != null) generateBtn.setText(I18n.getOrDefault("scenario.preview_btn", "🔄 Preview Distribution"));
            if (btnReliefOverlay != null) {
                btnReliefOverlay.setText(I18n.getOrDefault("scenario.btn.relief_overlay", "⛏️ Relief"));
                btnReliefOverlay.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.relief_overlay", "Superposer l'ombrage du relief topographique et des pentes avec délimitation du trait de côte.")));
            }

            planetSyncManager.updateTexts();
            liveDiagnosticSection.updateTexts();
            spatiotemporalSectionBuilder.updateTexts();
            spatialBoundsBuilder.updateTexts();
            bundleManager.updateTexts();
            if (engineConfigSectionBuilder != null) engineConfigSectionBuilder.updateTexts();
            if (demographicSectionBuilder != null) demographicSectionBuilder.updateTexts();
            if (culturalSectionBuilder != null) culturalSectionBuilder.updateTexts();

            updatePreviewModesCombo();
            updatePreviewTitleText();
        } finally {
            isUpdatingFromPreset = oldUpdating;
        }
    }

    public Scenario getScenario() {
        return configSyncHandler.exportScenario();
    }

    public void setOnScenarioLoadedCallback(BiConsumer<PlanetPreset, EcologyPreset> callback) {
        this.onScenarioLoadedCallback = callback;
    }

    public List<H3Cell> getCells() { return currentPreviewCells; }

    public void setGeneratedCells(List<H3Cell> cells) {
        this.currentPreviewCells = cells;
        if (cells != null && !cells.isEmpty()) {
            distributeInitialPopulation(cells);
        }
        drawPreview();
    }

    public void ensurePreviewGeneratedIfNeeded() {
        if (currentPreviewCells == null || currentPreviewCells.isEmpty()) {
            generatePreview();
        } else {
            drawPreview();
        }
    }

    public List<ClimateEvent> getScheduledEvents() {
        return eventsList != null ? new ArrayList<>(eventsList) : new ArrayList<>();
    }

    void updatePerformanceControlsState() {
        if (engineConfigSectionBuilder != null) {
            engineConfigSectionBuilder.updatePerformanceControlsState();
        }
    }

    /**
     * Custom SpinnerValueFactory for Long values to avoid ClassCastException.
     */
    public static class LongSpinnerValueFactory extends org.ether.society.ui.util.LongSpinnerValueFactory {
        public LongSpinnerValueFactory(long min, long max, long initialValue, long step) {
            super(min, max, initialValue, step);
        }
    }

    public void setPlanetPanelSupplier(Supplier<PlanetGeneratorPanel> supplier) {
        this.planetPanelSupplier = supplier;
    }

    public void setResourcePanelSupplier(Supplier<ResourceDistributionPanel> supplier) {
        this.resourcePanelSupplier = supplier;
    }

    public List<String> getScenarioValidationErrors() {
        return ScenarioValidationHelper.getValidationErrors(
                planetPanelSupplier,
                resourcePanelSupplier,
                startYearSpinner,
                endYearSpinner,
                demographicSectionBuilder != null ? demographicSectionBuilder.getInitialHumanCountSpinner() : null,
                demographicSectionBuilder != null ? demographicSectionBuilder.getRadioImportDemo() : null,
                demographicSectionBuilder != null ? demographicSectionBuilder.getCustomDensityImage() : null,
                culturalSectionBuilder != null ? culturalSectionBuilder.getCultureVectorDimSpinner() : null,
                culturalSectionBuilder != null ? culturalSectionBuilder.getTensorImportRadios() : null,
                culturalSectionBuilder != null ? culturalSectionBuilder.getCustomTensorImages() : null,
                culturalSectionBuilder != null ? culturalSectionBuilder::getCulturalTensorTitle : i -> ""
        );
    }

    public boolean validateScenarioSetup() {
        return ScenarioValidationHelper.validateAndHighlight(
                getScenarioValidationErrors(),
                startYearSpinner,
                endYearSpinner,
                demographicSectionBuilder != null ? demographicSectionBuilder.getInitialHumanCountSpinner() : null,
                demographicSectionBuilder != null ? demographicSectionBuilder.getRadioImportDemo() : null,
                demographicSectionBuilder != null ? demographicSectionBuilder.getCustomDensityImage() : null,
                demographicSectionBuilder != null ? demographicSectionBuilder.getLoadDensityMapBtn() : null,
                culturalSectionBuilder != null ? culturalSectionBuilder.getCultureVectorDimSpinner() : null,
                culturalSectionBuilder != null ? culturalSectionBuilder.getTensorImportRadios() : null,
                culturalSectionBuilder != null ? culturalSectionBuilder.getCustomTensorImages() : null,
                culturalSectionBuilder != null ? culturalSectionBuilder.getTensorLoadBtns() : null,
                validationErrorBanner,
                validationBannerHeaderLabel,
                validationErrorLabel,
                configScroll,
                this::updateLiveDiagnosticBlock
        );
    }

    public void openAutoEpochScenarioDialog() {
        try {
            String currentPlanet = activePlanetPreset != null ? activePlanetPreset.getCanonicalPlanet() : "earth";
            long currentYear = startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue().longValue() : -8000L;

            AutoEpochScenarioDialog dialog = new AutoEpochScenarioDialog(currentPlanet, currentYear, generatedScenario -> {
                applyScenarioToUI(generatedScenario);
                if (scenarioPresetBar != null) {
                    scenarioPresetBar.getPresetCombo().setValue(null);
                    scenarioPresetBar.setNameText(generatedScenario.getName());
                    scenarioPresetBar.markClean(generatedScenario);
                }
                notifyParamChange();
            });
            if (getScene() != null && getScene().getWindow() != null) {
                dialog.initOwner(getScene().getWindow());
            }
            dialog.show();
        } catch (Exception ex) {
            logger.error("Failed to open AutoEpochScenarioDialog", ex);
        }
    }
}
