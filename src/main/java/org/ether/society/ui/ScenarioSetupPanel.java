/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.collections.ObservableList;
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
import org.ether.society.ui.scenario.ScenarioCulturalSectionBuilder;
import org.ether.society.ui.scenario.ScenarioDefaultValueManager;
import org.ether.society.ui.scenario.ScenarioDemographicSectionBuilder;
import org.ether.society.ui.scenario.ScenarioEngineConfigSectionBuilder;
import org.ether.society.ui.scenario.ScenarioEventsSectionBuilder;
import org.ether.society.ui.scenario.ScenarioLiveDiagnosticSection;
import org.ether.society.ui.scenario.ScenarioPlanetSyncManager;
import org.ether.society.ui.scenario.ScenarioPresetManager;
import org.ether.society.ui.scenario.ScenarioPreviewPaneBuilder;
import org.ether.society.ui.scenario.ScenarioSimulationLauncher;
import org.ether.society.ui.scenario.ScenarioSnapshotInspector;
import org.ether.society.ui.scenario.ScenarioSpatialBoundsSectionBuilder;
import org.ether.society.ui.scenario.ScenarioSpatiotemporalSectionBuilder;
import org.ether.society.ui.scenario.ScenarioThermodynamicAutocalc;

import java.util.ArrayList;
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

    // Inherited Presets Controls
    ComboBox<PlanetPreset> planetPresetCombo;
    ComboBox<EcologyPreset> ecologyPresetCombo;
    Label inheritedContextLabel;

    // Standardized Preset Bar for Scenarios
    Label scenarioPresetHeader;
    PresetControlBar<Scenario> scenarioPresetBar;
    Button btnAutoEpochScenario;
    VBox bottomActionBox;

    // Form Controls
    Label scenarioDescLabel;
    TextArea scenarioDescriptionArea;
    Spinner<Integer> startYearSpinner;
    Spinner<Integer> endYearSpinner;
    ComboBox<Integer> h3ResolutionCombo;
    Spinner<Integer> targetCohortSizeSpinner;
    ComboBox<Double> temporalResolutionCombo;

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
    final ScenarioLiveDiagnosticSection liveDiagnosticSection = new ScenarioLiveDiagnosticSection();
    final ScenarioSnapshotInspector snapshotInspector = new ScenarioSnapshotInspector(meta -> {
        updateStartButtonLabel();
        notifyParamChange();
    });

    // Map Preview Controls
    ComboBox<String> previewModeCombo;
    ToggleButton btnReliefOverlay;
    Label previewStatusLabel;

    // Execution & Calculation Controls
    ProgressBar progressBar;
    Label progressStatusLabel;
    Button startBtn;
    Button generateBtn;

    // State
    List<H3Cell> currentPreviewCells;
    PlanetPreset activePlanetPreset;
    private final org.ether.society.persistence.ScenarioRepository scenarioRepo;
    BiConsumer<PlanetPreset, EcologyPreset> onScenarioLoadedCallback;
    Supplier<Boolean> isSimulationRunningSupplier;

    // UI Titles & Labels
    Label headerLabel;
    ScrollPane configScroll;
    VBox validationErrorBanner;
    Label validationBannerHeaderLabel;
    Label validationErrorLabel;

    // Seed & Random Events Controls
    TextField demoSeedField;
    TextField cultSeedField;
    CheckBox randomEventsCheckBox;
    CheckBox earthLeadersCheckBox;
    CheckBox proceduralLeadersCheckBox;
    CheckBox chronicleObserverCheckBox;

    // Events section
    ScenarioEventsSectionBuilder eventsSectionBuilder;
    ObservableList<ClimateEvent> eventsList;
    RadioButton radioNewSimulation;

    Supplier<PlanetGeneratorPanel> planetPanelSupplier;
    Supplier<ResourceDistributionPanel> resourcePanelSupplier;

    public void setIsSimulationRunningSupplier(Supplier<Boolean> isSimulationRunningSupplier) {
        this.isSimulationRunningSupplier = isSimulationRunningSupplier;
    }

    /** Internal model for a scheduled planetary event. */
    public static class ClimateEvent extends ScenarioClimateEventModel {
        public ClimateEvent(String type, String name, int year, double lat, double lon, double depth, double mag, int durationDays) {
            super(type, name, year, lat, lon, depth, mag, durationDays);
        }

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
        this.scenarioRepo = new org.ether.society.persistence.ScenarioRepository();
        this.presetManager = ScenarioHostContextFactory.createPresetManager(this, this.scenarioRepo);
        this.simulationLauncher = ScenarioHostContextFactory.createSimulationLauncher(this, this.scenarioRepo, onStartSimulation);
        ScenarioSetupLayoutAssembler.initUI(this);
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
        ScenarioSetupDiagnosticHelper.updateDefaultValueIndicators(this);
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
        ScenarioSetupLayoutAssembler.updateStartButtonLabel(this);
    }

    private void updateLiveDiagnosticBlock() {
        ScenarioSetupDiagnosticHelper.updateLiveDiagnosticBlock(this);
    }

    public void applyScenarioToUI(Scenario s) {
        configSyncHandler.applyScenarioToUI(s);
    }

    public void generatePreview() {
        if (previewPaneBuilder != null) {
            previewPaneBuilder.generatePreview();
        }
    }

    void distributeInitialPopulation(List<H3Cell> cells) {
        ScenarioSetupLayoutAssembler.distributeInitialPopulation(this, cells);
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
        ScenarioSetupDiagnosticHelper.updatePanelTexts(this);
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
        return ScenarioSetupDiagnosticHelper.getValidationErrors(this);
    }

    public boolean validateScenarioSetup() {
        return ScenarioSetupDiagnosticHelper.validateScenarioSetup(this);
    }

    public void openAutoEpochScenarioDialog() {
        ScenarioSetupDiagnosticHelper.openAutoEpochScenarioDialog(this);
    }
}
