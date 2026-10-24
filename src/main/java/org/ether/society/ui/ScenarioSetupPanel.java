/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.ui.scenario.ScenarioConfigPaneBuilder;
import org.ether.society.ui.scenario.ScenarioSanityValidator;
import org.ether.society.ui.scenario.ScenarioDefaultValueManager;
import org.ether.society.ui.scenario.ScenarioLiveDiagnosticSection;
import org.ether.society.ui.scenario.ScenarioPlanetSyncManager;
import org.ether.society.ui.scenario.ScenarioSpatiotemporalSectionBuilder;
import org.ether.society.ui.scenario.ScenarioClimateEventModel;
import org.ether.society.ui.scenario.ScenarioBundleManager;
import org.ether.society.ui.scenario.ScenarioEpochCatalog;
import org.ether.society.ui.scenario.ScenarioPresetManager;
import org.ether.society.ui.scenario.ScenarioSimulationLauncher;
import org.ether.society.ui.scenario.ScenarioSpatialBoundsSectionBuilder;
import org.ether.society.ui.scenario.ScenarioThermodynamicAutocalc;
import org.ether.society.ui.scenario.ScenarioValidationHelper;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.EcologyPreset;
import org.ether.society.model.Scenario;
import org.ether.society.model.StartDatePreset;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.util.LaTeXFormatter;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.engines.tier2.theories.ProceduralPopulationEngine;
import org.ether.society.ui.scenario.ScenarioCulturalSectionBuilder;
import org.ether.society.ui.scenario.ScenarioDemographicSectionBuilder;
import org.ether.society.ui.scenario.ScenarioPreviewPaneBuilder;
import javafx.util.StringConverter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Panel for setting up civilization, initial population distribution, historical scenarios,
 * and civilizational start parameters.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioSetupPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioSetupPanel.class);
    private final java.util.Map<String, WritableImage> proceduralImageCache = new java.util.HashMap<>();

    // Inherited Presets Header
    private Label planetSectionHeader;
    private ComboBox<PlanetPreset> planetPresetCombo;
    private ComboBox<EcologyPreset> ecologyPresetCombo;
    private Label inheritedContextLabel;

    // Standardized Preset Bar for Scenarios
    private Label scenarioPresetHeader;
    private Label snapshotHeader;
    private Label bundleHeader;
    private PresetControlBar<Scenario> scenarioPresetBar;
    private Button btnAutoEpochScenario;
    private VBox bottomActionBox;

    // Form Controls
    private Label scenarioDescLabel;
    private TextArea scenarioDescriptionArea;
    private Spinner<Integer> startYearSpinner;
    private Spinner<Integer> endYearSpinner;
    private ComboBox<Integer> h3ResolutionCombo;
    private Label h3ResolutionLabel;
    private Label h3ResolutionHintLabel;
    private Spinner<Integer> targetCohortSizeSpinner;
    private Label cohortSizeLabel;
    private Label cohortSizeHintLabel;
    private ComboBox<Double> temporalResolutionCombo;
    private Label temporalResolutionLabel;
    private Label temporalResolutionHintLabel;
    private Label snapshotAutoPruningNotice;

    // State & Change Listener Tracking
    /** Internal state: is updating from preset (boolean). */
    private boolean isUpdatingFromPreset = false;

    private void notifyParamChange() {
        if (!isUpdatingFromPreset && scenarioPresetBar != null) {
            scenarioPresetBar.notifyParametersChanged();
        }
        updateLiveDiagnosticBlock();
        updateDefaultValueIndicators();
        updateStartButtonLabel();
        validateScenarioSetup();
    }

    /**
     * Attaches default value handling to a JavaFX control:
     * 1. Appends "[DEFAULT VALUE: X]" to the tooltip.
     * 2. Adds a right-click Context Menu ("Reset to default value (X)").
     */
    private <T> void attachDefaultValueHandling(Control control, T defaultValue, Runnable resetAction) {
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

    private void highlightControlIfModified(Control control, boolean isModified) {
        ScenarioDefaultValueManager.highlightControlIfModified(control, isModified);
    }

    public void resetAllToDefaults() {
        boolean oldUpdating = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            Scenario def = new Scenario();
            def.setName(org.ether.society.i18n.I18n.getOrDefault("scenario.default_name", "New Baseline Scenario"));
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

    // Delegated Section Builders and Managers
    private final ScenarioPlanetSyncManager planetSyncManager = new ScenarioPlanetSyncManager(new ScenarioPlanetSyncManager.HostContext() {
        @Override public void notifyParamChange() { ScenarioSetupPanel.this.notifyParamChange(); }
        @Override public void generatePreview() { ScenarioSetupPanel.this.generatePreview(); }
        @Override public void drawPreview() { ScenarioSetupPanel.this.drawPreview(); }
        @Override public boolean hasPreviewCells() { return currentPreviewCells != null && !currentPreviewCells.isEmpty(); }
    });

    private final ScenarioSpatiotemporalSectionBuilder spatiotemporalSectionBuilder = new ScenarioSpatiotemporalSectionBuilder(new ScenarioSpatiotemporalSectionBuilder.HostContext() {
        @Override public void notifyParamChange() { ScenarioSetupPanel.this.notifyParamChange(); }
        @Override public void loadEarthHistoricalEvents() { ScenarioSetupPanel.this.loadEarthHistoricalEvents(); }
        @Override public <T> void attachDefaultValueHandling(Control control, T defaultValue, Runnable resetAction) {
            ScenarioSetupPanel.this.attachDefaultValueHandling(control, defaultValue, resetAction);
        }
    });

    private final ScenarioSpatialBoundsSectionBuilder spatialBoundsBuilder = new ScenarioSpatialBoundsSectionBuilder(new ScenarioSpatialBoundsSectionBuilder.HostContext() {
        @Override public void notifyParamChange() { ScenarioSetupPanel.this.notifyParamChange(); }
        @Override public void drawPreview() { ScenarioSetupPanel.this.drawPreview(); }
        @Override public void resetPreviewCells() { ScenarioSetupPanel.this.currentPreviewCells = null; }
        @Override public <T> void attachDefaultValueHandling(Control control, T defaultValue, Runnable resetAction) {
            ScenarioSetupPanel.this.attachDefaultValueHandling(control, defaultValue, resetAction);
        }
    });

    private final ScenarioBundleManager bundleManager = new ScenarioBundleManager(new ScenarioBundleManager.HostContext() {
        @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
        @Override public PlanetPreset getActivePlanetPreset() { return ScenarioSetupPanel.this.activePlanetPreset != null ? ScenarioSetupPanel.this.activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : null); }
        @Override public EcologyPreset getActiveEcologyPreset() { return ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null; }
        @Override public Scenario getScenario() { return ScenarioSetupPanel.this.getScenario(); }
        @Override public void applyScenarioToUI(Scenario scenario) { ScenarioSetupPanel.this.applyScenarioToUI(scenario); }
        @Override public void onBundleLoaded(PlanetPreset planet, EcologyPreset ecology) {
            if (planet != null) {
                ScenarioSetupPanel.this.activePlanetPreset = planet;
                if (planetPresetCombo != null) {
                    if (!planetPresetCombo.getItems().contains(planet)) {
                        planetPresetCombo.getItems().add(planet);
                    }
                    planetPresetCombo.setValue(planet);
                }
            }
            if (ecology != null && ecologyPresetCombo != null) {
                if (!ecologyPresetCombo.getItems().contains(ecology)) {
                    ecologyPresetCombo.getItems().add(ecology);
                }
                ecologyPresetCombo.setValue(ecology);
            }
            if (onScenarioLoadedCallback != null) {
                onScenarioLoadedCallback.accept(ScenarioSetupPanel.this.activePlanetPreset, ecology != null ? ecology : EcologyPreset.EARTH_STANDARD);
            }
        }
        @Override public void generatePreview() { ScenarioSetupPanel.this.generatePreview(); }
        @Override public void showIntegrityWarning(String message) {
            if (validationErrorLabel != null && validationErrorBanner != null) {
                validationErrorLabel.setText(message);
                validationErrorBanner.setVisible(true);
                validationErrorBanner.setManaged(true);
            }
        }
    });

    private final ScenarioSimulationLauncher simulationLauncher;

    private ScenarioSimulationLauncher createSimulationLauncher() {
        return new ScenarioSimulationLauncher(
            new ScenarioSimulationLauncher.HostContext() {
                @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
                @Override public javafx.scene.Node getContainerNode() { return ScenarioSetupPanel.this; }
                @Override public boolean validateScenarioSetup() { return ScenarioSetupPanel.this.validateScenarioSetup(); }
                @Override public Scenario getScenario() { return ScenarioSetupPanel.this.getScenario(); }
                @Override public PlanetPreset getActivePlanetPreset() { return ScenarioSetupPanel.this.activePlanetPreset != null ? ScenarioSetupPanel.this.activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : null); }
                @Override public EcologyPreset getActiveEcologyPreset() { return ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null; }
                @Override public Integer getH3Resolution() { return h3ResolutionCombo != null ? h3ResolutionCombo.getValue() : 3; }
                @Override public boolean isClippingActive() { return spatialBoundsBuilder.isClippingEnabled(); }
                @Override public double getClippingMinLat() { return spatialBoundsBuilder.getMinLat(); }
                @Override public double getClippingMaxLat() { return spatialBoundsBuilder.getMaxLat(); }
                @Override public double getClippingMinLng() { return spatialBoundsBuilder.getMinLng(); }
                @Override public double getClippingMaxLng() { return spatialBoundsBuilder.getMaxLng(); }
                @Override public boolean isResumeFromSnapshotSelected() { return ScenarioSetupPanel.this.isResumeFromSnapshotSelected(); }
                @Override public org.ether.society.persistence.SaveMetadata getSelectedSnapshotMetadata() { return ScenarioSetupPanel.this.getSelectedSnapshotMetadata(); }
                @Override public boolean isSimulationCurrentlyRunning() { return isSimulationRunningSupplier != null && Boolean.TRUE.equals(isSimulationRunningSupplier.get()); }
                @Override public boolean isDirty() { return ScenarioSetupPanel.this.isDirty(); }
                @Override public boolean promptSaveIfDirty(javafx.stage.Window owner) { return ScenarioSetupPanel.this.promptSaveIfDirty(owner); }
                @Override public void distributeInitialPopulation(List<H3Cell> cells) { ScenarioSetupPanel.this.distributeInitialPopulation(cells); }
                @Override public void onCellsGenerated(List<H3Cell> cells) { ScenarioSetupPanel.this.currentPreviewCells = cells; }
                @Override public void markClean() { if (scenarioPresetBar != null) scenarioPresetBar.markClean(); }
                @Override public void drawPreview() { ScenarioSetupPanel.this.drawPreview(); }
                @Override public void setPreviewStatusText(String text) { if (previewStatusLabel != null) previewStatusLabel.setText(text); }
            },
            scenarioRepo,
            scenario -> { if (onStartSimulation != null) onStartSimulation.accept(scenario); }
        );
    }

    // Demographic Section Builder & Controller
    private final ScenarioDemographicSectionBuilder demographicSectionBuilder = new ScenarioDemographicSectionBuilder(new ScenarioDemographicSectionBuilder.HostContext() {
        @Override public void notifyParamChange() { ScenarioSetupPanel.this.notifyParamChange(); }
        @Override public void drawPreview() { ScenarioSetupPanel.this.drawPreview(); }
        @Override public void selectPreviewMode(int modeIndex) {
            if (previewModeCombo != null && previewModeCombo.getSelectionModel().getSelectedIndex() != modeIndex) {
                previewModeCombo.getSelectionModel().select(modeIndex);
            }
        }
        @Override public List<H3Cell> getPreviewCells() { return ScenarioSetupPanel.this.getCells(); }
        @Override public void distributeInitialPopulation(List<H3Cell> cells) { ScenarioSetupPanel.this.distributeInitialPopulation(cells); }
        @Override public Scenario getScenario() { return ScenarioSetupPanel.this.getScenario(); }
        @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : null); }
        @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
        @Override public boolean isUpdatingFromPreset() { return isUpdatingFromPreset; }
        @Override public long getStartYear() { return startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue().longValue() : -8000L; }
        @Override public double computeAutoCapitalFromYear(long year) { return ScenarioSetupPanel.this.computeAutoCapitalFromYear(year); }
        @Override public void generatePreview() { ScenarioSetupPanel.this.generatePreview(); }
        @Override public void updatePreviewTitleText() { ScenarioSetupPanel.this.updatePreviewTitleText(); }
        @Override public void updateBottomLegend() { ScenarioSetupPanel.this.updateBottomLegend(); }
    });

    // Cultural Vector & Multi-Field Layer Controls (Tab 3)
    private final ScenarioCulturalSectionBuilder culturalSectionBuilder = new ScenarioCulturalSectionBuilder(new ScenarioCulturalSectionBuilder.HostContext() {
        @Override public void notifyParamChange() { ScenarioSetupPanel.this.notifyParamChange(); }
        @Override public void drawPreview() { ScenarioSetupPanel.this.drawPreview(); }
        @Override public void updatePreviewModesCombo() { ScenarioSetupPanel.this.updatePreviewModesCombo(); }
        @Override public Scenario getScenario() { return ScenarioSetupPanel.this.getScenario(); }
        @Override public long getStartYear() { return startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -100000L; }
        @Override public PlanetPreset getActivePlanetPreset() { return ScenarioSetupPanel.this.activePlanetPreset; }
        @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
        @Override public boolean isUpdatingFromPreset() { return isUpdatingFromPreset; }
        @Override public void selectPreviewMode(int modeIndex) {
            if (previewModeCombo != null && previewModeCombo.getSelectionModel().getSelectedIndex() != modeIndex) {
                previewModeCombo.getSelectionModel().select(modeIndex);
            }
        }
        @Override public String getScenarioDescription() {
            return scenarioDescriptionArea != null ? scenarioDescriptionArea.getText() : "Ether Scenario";
        }
    });

// Map Preview Subsystem Builder & Controller
private final ScenarioPreviewPaneBuilder previewPaneBuilder = new ScenarioPreviewPaneBuilder(new ScenarioPreviewPaneBuilder.HostContext() {
    @Override public Scenario getScenario() { return ScenarioSetupPanel.this.getScenario(); }
    @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : null); }
    @Override public long getStartYear() { return startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000L; }
    @Override public int getCultureVectorDimensions() { return culturalSectionBuilder != null ? culturalSectionBuilder.getCultureVectorDimensions() : 9; }
    @Override public double getCulturalDiffusionRate() { return culturalSectionBuilder != null ? culturalSectionBuilder.getCulturalDiffusionRate() : 0.05; }
    @Override public double getCulturalMutationRate() { return culturalSectionBuilder != null ? culturalSectionBuilder.getCulturalMutationRate() : 0.02; }
    @Override public Image getCustomTensorImage(int index) { return culturalSectionBuilder != null ? culturalSectionBuilder.getCustomTensorImage(index) : null; }
    @Override public boolean isTensorImportActive(int index) { return culturalSectionBuilder != null && culturalSectionBuilder.isTensorImportActive(index); }
    @Override public double getTensorParam1(int index) { return culturalSectionBuilder != null ? culturalSectionBuilder.getTensorParam1(index) : 1.0; }
    @Override public double getTensorParam2(int index) { return culturalSectionBuilder != null ? culturalSectionBuilder.getTensorParam2(index) : 1.0; }
    @Override public double getTensorParam3(int index) { return culturalSectionBuilder != null ? culturalSectionBuilder.getTensorParam3(index) : 1.0; }
    @Override public long getTensorSeed(int index) { return culturalSectionBuilder != null ? culturalSectionBuilder.getTensorSeed(index) : (11235L + index * 11111L); }
    @Override public Color evaluateProceduralTensorColor(int tIdx, double lat, double lon, double elev, double p1, double p2, double p3, long seed) {
        return culturalSectionBuilder != null ? culturalSectionBuilder.evaluateProceduralTensorColor(tIdx, lat, lon, elev, p1, p2, p3, seed) : Color.GRAY;
    }
    @Override public String getCulturalTensorTitle(int index) { return culturalSectionBuilder != null ? culturalSectionBuilder.getCulturalTensorTitle(index) : ""; }
    @Override public String getCulturalTensorPreviewName(int index) { return culturalSectionBuilder != null ? culturalSectionBuilder.getCulturalTensorPreviewName(index) : ""; }
    @Override public Image getCustomDensityImage() { return demographicSectionBuilder != null ? demographicSectionBuilder.getCustomDensityImage() : null; }
    @Override public boolean isDensityImportActive() { return demographicSectionBuilder != null && demographicSectionBuilder.isDensityImportActive(); }
    @Override public long getInitialHumanCount() { return demographicSectionBuilder != null ? demographicSectionBuilder.getInitialHumanCount() : 1_000_000L; }
    @Override public String getDensityPattern() { return demographicSectionBuilder != null ? demographicSectionBuilder.getDensityPattern() : "UNBIASED_NATURAL"; }
    @Override public String getDemoSeed() { return demoSeedField != null ? demoSeedField.getText() : ""; }
    @Override public String getCultSeed() { return cultSeedField != null ? cultSeedField.getText() : ""; }
    @Override public int getH3Resolution() { return h3ResolutionCombo != null && h3ResolutionCombo.getValue() != null ? h3ResolutionCombo.getValue() : 3; }
    @Override public boolean isUpdatingFromPreset() { return isUpdatingFromPreset; }
    @Override public double computeAutoCapitalFromYear(long year) { return ScenarioSetupPanel.this.computeAutoCapitalFromYear(year); }
    @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
    @Override public void notifyParamChange() { ScenarioSetupPanel.this.notifyParamChange(); }
    @Override public void applySpatialClippingBounds(double minLat, double maxLat, double minLon, double maxLon) {
        spatialBoundsBuilder.applySpatialClippingBounds(minLat, maxLat, minLon, maxLon);
    }
    @Override public boolean isGraphicSelectActive() { return spatialBoundsBuilder.isGraphicSelectActive(); }
    @Override public void setGraphicSelectActive(boolean active) { spatialBoundsBuilder.setGraphicSelectActive(active); }
    @Override public double getClippingMinLat() { return spatialBoundsBuilder.getMinLat(); }
    @Override public double getClippingMaxLat() { return spatialBoundsBuilder.getMaxLat(); }
    @Override public double getClippingMinLng() { return spatialBoundsBuilder.getMinLng(); }
    @Override public double getClippingMaxLng() { return spatialBoundsBuilder.getMaxLng(); }
    @Override public boolean isClippingEnabled() { return spatialBoundsBuilder.isClippingEnabled(); }
    @Override public Image getElevationImageForPreset(PlanetPreset preset) { return ScenarioSetupPanel.this.getElevationImageForPreset(preset); }
    @Override public void applyPresetMapToCells(List<H3Cell> cells, PlanetPreset cfg) { simulationLauncher.applyPresetMapToCells(cells, cfg, getScenario()); }
    @Override public void onPreviewCellsGenerated(List<H3Cell> cells) { ScenarioSetupPanel.this.currentPreviewCells = cells; }
});

// Map Preview
private Canvas previewCanvas;
    private ComboBox<String> previewModeCombo;
    private ToggleButton btnReliefOverlay;
    private Label previewStatusLabel;

    // Execution & Calculation Controls
    private ProgressBar progressBar;
    private Label progressStatusLabel;
    private Button startBtn;
    private Button generateBtn;

    // State
    private List<H3Cell> currentPreviewCells;
    private PlanetPreset activePlanetPreset;
    private final Consumer<Scenario> onStartSimulation;
    private final org.ether.society.persistence.ScenarioRepository scenarioRepo;
    private final ScenarioPresetManager presetManager;
    private final ScenarioLiveDiagnosticSection liveDiagnosticSection = new ScenarioLiveDiagnosticSection();

    // UI Titles & Labels
    private Label headerLabel;
    private Label previewTitleLabel;
    private ScrollPane configScroll;
    private VBox validationErrorBanner;
    private Label validationBannerHeaderLabel;
    private Label validationErrorLabel;

    // Seed & Random Events Controls
    private TextField demoSeedField;
    private TextField cultSeedField;
    private CheckBox randomEventsCheckBox;
    private CheckBox randomBiophysicalEventsCheckBox;
    private CheckBox randomPoliticalEventsCheckBox;
    private CheckBox earthLeadersCheckBox;
    private CheckBox proceduralLeadersCheckBox;

    // Events section
    private org.ether.society.ui.scenario.ScenarioEventsSectionBuilder eventsSectionBuilder;
    private ObservableList<ClimateEvent> eventsList;

    // Snapshot Management
    private RadioButton radioNewSimulation;
    private java.util.function.Supplier<Boolean> isSimulationRunningSupplier;

    public void setIsSimulationRunningSupplier(java.util.function.Supplier<Boolean> isSimulationRunningSupplier) {
        this.isSimulationRunningSupplier = isSimulationRunningSupplier;
    }

    /** Internal model for a scheduled planetary event */
    public static class ClimateEvent extends ScenarioClimateEventModel {
        public ClimateEvent(String type, String name, int year, double lat, double lon, double depth, double mag) {
            super(type, name, year, lat, lon, depth, mag);
        }
    }

    /**
     * Scenario setup panel.
     *
     * @param onStartSimulation the on start simulation parameter (Consumer&lt;Scenario&gt;)
     */
    private ScenarioPresetManager createPresetManager() {
        return new ScenarioPresetManager(new ScenarioPresetManager.HostContext() {
            @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
            @Override public boolean validateScenarioSetup() { return ScenarioSetupPanel.this.validateScenarioSetup(); }
            @Override public Scenario getScenario() { return ScenarioSetupPanel.this.getScenario(); }
            @Override public void applyScenarioToUI(Scenario s) { ScenarioSetupPanel.this.applyScenarioToUI(s); }
            @Override public void selectPreviewMode(int modeIndex) {
                if (previewModeCombo != null && previewModeCombo.getSelectionModel().getSelectedIndex() != modeIndex) {
                    previewModeCombo.getSelectionModel().select(modeIndex);
                }
            }
            @Override public void refreshSnapshotList() { ScenarioSetupPanel.this.refreshSnapshotList(); }
            @Override public void refreshSnapshotListForScenario(Scenario s) { ScenarioSetupPanel.this.refreshSnapshotListForScenario(s); }
            @Override public void updateStartButtonLabel() { ScenarioSetupPanel.this.updateStartButtonLabel(); }
            @Override public void notifyParamChange() { ScenarioSetupPanel.this.notifyParamChange(); }
        }, this.scenarioRepo);
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
        this.presetManager = createPresetManager();
        this.simulationLauncher = createSimulationLauncher();
        initUI();
        updateTexts();

        org.ether.society.i18n.I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
        
        org.ether.society.data.sync.GitHubContentSyncService.addListener(new org.ether.society.data.sync.GitHubContentSyncService.SyncEventListener() {
            @Override
            public void onSyncCompleted(int downloaded, int checked) {
                if (downloaded > 0) {
                    refreshScenarioPresets();
                }
            }
            @Override
            public void onScenarioDiscovered(String name) {
                refreshScenarioPresets();
            }
        });

        javafx.application.Platform.runLater(() -> {
            loadEarthHistoricalEvents();
        });
    }

    /**
     * Set inherited context.
     *
     * @param planetPreset the planet preset parameter (PlanetPreset)
     * @param ecologyName the ecology name parameter (String)
     */
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

        // Left: Configuration Controls with pinned bottom action bar
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

        // Right: Preview Area
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

        VBox popSection = createPopulationDensitySection();
        VBox cultureSection = createCulturalVectorAndLayersSection();
        VBox clippingSection = createClippingSection();
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
        VBox bundleSection = createBundleSection();
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

    private VBox createBundleSection() {
        return bundleManager.buildSection();
    }

    private final org.ether.society.ui.scenario.ScenarioSnapshotInspector snapshotInspector =
            new org.ether.society.ui.scenario.ScenarioSnapshotInspector(meta -> {
                updateStartButtonLabel();
                notifyParamChange();
            });

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

    private void updateSnapshotDetailsDisplay(org.ether.society.persistence.SaveMetadata meta) {
        snapshotInspector.updateSnapshotDetailsDisplay(meta);
    }

    private void updateStartButtonLabel() {
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

    private void runPreFlightSanityCheck() {
        updateLiveDiagnosticBlock();
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
                culturalSectionBuilder != null ? culturalSectionBuilder.getCustomTensorImages() : java.util.Collections.emptyMap(),
                culturalSectionBuilder != null ? culturalSectionBuilder::getCulturalTensorTitle : i -> "Cultural Tensor #" + (i + 1),
                startY,
                initPop,
                getScenarioValidationErrors(),
                ScenarioSanityValidator.getOrCreateJitReport()
        );
    }

    private List<Scenario> getBuiltInScenarios() {
        return presetManager != null ? presetManager.getBuiltInScenarios() : java.util.Collections.emptyList();
    }

    private void updateInheritedContextDisplay(String ecologyName) {
        planetSyncManager.updateInheritedContextDisplay(ecologyName);
    }

    public void applyScenarioToUI(Scenario s) {
        if (s == null) return;
        isUpdatingFromPreset = true;
        try {
            if (scenarioPresetBar != null) {
                scenarioPresetBar.setNameText(s.getName());
            }
            if (scenarioDescriptionArea != null) {
                scenarioDescriptionArea.setText(s.getDescription() != null ? s.getDescription() : "");
            }

            // Determine if planet is Earth
            boolean isEarth = true;
            if (s.getPlanetPreset() != null) {
                isEarth = "earth".equalsIgnoreCase(s.getPlanetPreset().getCanonicalPlanet());
            } else if (activePlanetPreset != null) {
                isEarth = "earth".equalsIgnoreCase(activePlanetPreset.getCanonicalPlanet());
            }

            // Lazy populate cartographic buffer maps asynchronously if not already generated (only for Earth)
            if (isEarth && s.getCustomDensityBase64() == null) {
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    org.ether.society.data.HistoricalMapGenerator.populateScenarioHistoricalMaps(s);
                });
            }

            // Select Ecology Preset which cascades to Planet Preset
            if (s.getEcologyPreset() != null && ecologyPresetCombo != null) {
                ecologyPresetCombo.setValue(s.getEcologyPreset());
            } else if (s.getEcologyPresetName() != null && ecologyPresetCombo != null) {
                for (EcologyPreset eco : ecologyPresetCombo.getItems()) {
                    if (eco.name().equalsIgnoreCase(s.getEcologyPresetName())) {
                        ecologyPresetCombo.setValue(eco);
                        break;
                    }
                }
            }

            EcologyPreset activeEco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null;
            PlanetPreset cascaded = activeEco != null ? findPlanetPresetByName(activeEco.planetPresetName()) : s.getPlanetPreset();
            if (cascaded != null) {
                this.activePlanetPreset = cascaded;
                if (planetPresetCombo != null) {
                    planetPresetCombo.setValue(cascaded);
                }
            }
            updateInheritedContextDisplay(activeEco != null ? activeEco.name() : null);

            spatiotemporalSectionBuilder.applyScenario(s);
            demographicSectionBuilder.applyScenario(s);
            culturalSectionBuilder.applyScenario(s);
            spatialBoundsBuilder.applyScenario(s);
            if (engineConfigSectionBuilder != null) {
                engineConfigSectionBuilder.applyScenario(s);
            }

            if (s.getSeed() != 0 && demoSeedField != null) {
                demoSeedField.setText(String.valueOf(s.getSeed()));
            }
            if (s.getCulturalSeed() != 0 && cultSeedField != null) {
                cultSeedField.setText(String.valueOf(s.getCulturalSeed()));
            }
            if (randomEventsCheckBox != null) {
                randomEventsCheckBox.setSelected(s.isRandomEventsEnabled());
            }
            if (earthLeadersCheckBox != null) {
                earthLeadersCheckBox.setSelected(s.isEarthHistoricalLeadersEnabled());
            }
            if (proceduralLeadersCheckBox != null) {
                proceduralLeadersCheckBox.setSelected(s.isProceduralLeadersEnabled());
            }

            // Auto-select the demographic reference source combo based on the scenario epoch/planet
            if (demographicSectionBuilder != null && demographicSectionBuilder.getDemoSourceCombo() != null) {
                String autoSrc = pickDemoSourceForEpoch(s.getStartDateYear(), this.activePlanetPreset);
                selectBestSource(demographicSectionBuilder.getDemoSourceCombo(), autoSrc, "Terre");
            }

            // Restore Scheduled Events
            if (eventsList != null) {
                eventsList.clear();
                if (s.getClimateEvents() != null && !s.getClimateEvents().isEmpty()) {
                    for (org.ether.society.model.ClimateEvent ce : s.getClimateEvents()) {
                        eventsList.add(new ClimateEvent(ce.getType(), ce.getName(), ce.getYear(), ce.getLatitude(), ce.getLongitude(), ce.getDepth(), ce.getMagnitude()));
                    }
                }
            }

            updatePreviewModesCombo();
            if (previewModeCombo != null) {
                previewModeCombo.getSelectionModel().select(0);
            }
            currentPreviewCells = null;
            generatePreview();
            if (onScenarioLoadedCallback != null) {
                onScenarioLoadedCallback.accept(this.activePlanetPreset, activeEco != null ? activeEco : s.getEcologyPreset());
            }
            updatePerformanceControlsState();
            if (scenarioPresetBar != null) {
                scenarioPresetBar.markClean(s);
            }
            refreshSnapshotListForScenario(s);
        } finally {
            isUpdatingFromPreset = false;
        }
    }

    private VBox createClippingSection() {
        return spatialBoundsBuilder.buildSection();
    }

    private void applySpatialClippingBounds(double minLat, double maxLat, double minLon, double maxLon) {
        spatialBoundsBuilder.applySpatialClippingBounds(minLat, maxLat, minLon, maxLon);
    }

    private org.ether.society.ui.scenario.ScenarioEngineConfigSectionBuilder engineConfigSectionBuilder;

    private VBox createOceanOptimizationSection() {
        if (engineConfigSectionBuilder == null) {
            engineConfigSectionBuilder = new org.ether.society.ui.scenario.ScenarioEngineConfigSectionBuilder(
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

    private String pickDemoSourceForEpoch(long startYear, org.ether.society.generation.PlanetPreset planet) {
        return demographicSectionBuilder != null ? demographicSectionBuilder.pickDemoSourceForEpoch(startYear, planet) : "";
    }

    private void selectBestSource(ComboBox<String> combo, String target, String fallbackKeyword) {
        ScenarioEpochCatalog.selectBestSource(combo, target, fallbackKeyword);
    }

    private VBox createPreviewPane() {
        VBox root = previewPaneBuilder.buildPreviewPane();
        previewCanvas = previewPaneBuilder.getPreviewCanvas();
        previewModeCombo = previewPaneBuilder.getPreviewModeCombo();
        previewStatusLabel = previewPaneBuilder.getPreviewStatusLabel();
        btnReliefOverlay = previewPaneBuilder.getBtnReliefOverlay();
        return root;
    }

    private VBox createPopulationDensitySection() {
        return demographicSectionBuilder.buildSection();
    }

    public void generatePreview() {
        if (previewPaneBuilder != null) {
            previewPaneBuilder.generatePreview();
        }
    }

    private void distributeInitialPopulation(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;
        long totalPop = demographicSectionBuilder != null ? demographicSectionBuilder.getInitialHumanCount() : 1_000_000L;
        double capitalK0 = computeAutoCapitalFromYear(startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000);
        String pattern = demographicSectionBuilder != null ? demographicSectionBuilder.getDensityPattern() : "UNBIASED_NATURAL";
        boolean isEarthPreset = activePlanetPreset != null && activePlanetPreset.elevationUseImport()
                && "earth".equalsIgnoreCase(activePlanetPreset.elevationMapSource());
        long startYear = startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000;

        org.ether.society.ui.util.ScenarioCellPopulationDistributor.distributeInitialPopulation(
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

    private Image getElevationImageForPreset(PlanetPreset planet) {
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
            eventsSectionBuilder = new org.ether.society.ui.scenario.ScenarioEventsSectionBuilder(eventsList, this::notifyParamChange);
        }
        return eventsSectionBuilder.buildEventsSection(randomBiophysicalEventsCheckBox, randomPoliticalEventsCheckBox,
                () -> startYearSpinner != null ? startYearSpinner.getValue() : -8000);
    }

    private void loadEarthHistoricalEvents() {
        if (eventsSectionBuilder != null) {
            eventsSectionBuilder.loadEarthBiophysicalEvents(startYearSpinner != null ? startYearSpinner.getValue() : -8000, true);
        }
    }

    private void loadEarthBiophysicalEvents(int startYear, boolean promptConfirmation) {
        if (eventsSectionBuilder != null) {
            eventsSectionBuilder.loadEarthBiophysicalEvents(startYear, promptConfirmation);
        }
    }

    private void loadEarthLeadersEvents(int startYear, boolean promptConfirmation) {
        if (eventsSectionBuilder != null) {
            eventsSectionBuilder.loadHistoricalLeaderInterventions(startYear, promptConfirmation);
        }
    }

    public static double computeAutoCapitalFromYear(long year) {
        return ScenarioThermodynamicAutocalc.computeAutoCapitalFromYear(year);
    }

    public static double computeAutoEnergyFromYear(long year) {
        return ScenarioThermodynamicAutocalc.computeAutoEnergyFromYear(year);
    }

    public static double computeAutoFoodFromYear(long year) {
        return ScenarioThermodynamicAutocalc.computeAutoFoodFromYear(year);
    }

    public static double computeAutoInfoFromYear(long year) {
        return ScenarioThermodynamicAutocalc.computeAutoInfoFromYear(year);
    }

    public void applyEarthPreset() {
        if (planetPresetCombo != null) {
            planetPresetCombo.setValue(PlanetPreset.EARTH_LIKE);
        }
    }

    // =========================================================================
    // I18N — Update all UI text based on current language
    // =========================================================================

    /**
     * Update texts.
     *
     */
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
            
            if (previewTitleLabel != null) previewTitleLabel.setText(I18n.getOrDefault("scenario.title.right_view", "🗺️ Resource Cartography & Display"));
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
        Scenario s = new Scenario();
        s.setName(scenarioPresetBar != null ? scenarioPresetBar.getCurrentName() : "New Scenario");
        if (scenarioDescriptionArea != null) {
            s.setDescription(scenarioDescriptionArea.getText());
        }
        spatiotemporalSectionBuilder.exportToScenario(s);
        demographicSectionBuilder.exportToScenario(s);
        culturalSectionBuilder.exportToScenario(s);
        spatialBoundsBuilder.exportToScenario(s);
        if (engineConfigSectionBuilder != null) {
            engineConfigSectionBuilder.exportToScenario(s);
        }

        s.setEcologyPreset(ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null);
        if (ecologyPresetCombo != null && ecologyPresetCombo.getValue() != null) {
            s.setEcologyPresetName(ecologyPresetCombo.getValue().name());
        }
        s.setPlanetPreset(activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : null));

        long seedVal = 12345L;
        try {
            if (demoSeedField != null) seedVal = Long.parseLong(demoSeedField.getText());
        } catch (NumberFormatException ignored) {}
        s.setSeed(seedVal);

        long cultSeedVal = 54321L;
        try {
            if (cultSeedField != null) cultSeedVal = Long.parseLong(cultSeedField.getText());
        } catch (NumberFormatException ignored) {}
        s.setCulturalSeed(cultSeedVal);
        s.setRandomEventsEnabled(randomEventsCheckBox == null || randomEventsCheckBox.isSelected());
        s.setEarthHistoricalLeadersEnabled(earthLeadersCheckBox == null || earthLeadersCheckBox.isSelected());
        s.setProceduralLeadersEnabled(proceduralLeadersCheckBox == null || proceduralLeadersCheckBox.isSelected());

        // Save Scheduled Events
        if (eventsList != null) {
            java.util.List<org.ether.society.model.ClimateEvent> sEvents = new java.util.ArrayList<>();
            for (ClimateEvent ce : eventsList) {
                sEvents.add(new org.ether.society.model.ClimateEvent(ce.getType(), ce.getName(), ce.getYear(), ce.getLatitude(), ce.getLongitude(), ce.getDepth(), ce.getMagnitude()));
            }
            s.setClimateEvents(sEvents);
        }

        return s;
    }

    private java.util.function.BiConsumer<PlanetPreset, EcologyPreset> onScenarioLoadedCallback;

    /**
     * Sets the callback invoked when a scenario preset or bundle is loaded into the UI.
     *
     * @param callback consumer receiving active planet and ecology presets
     */
    public void setOnScenarioLoadedCallback(java.util.function.BiConsumer<PlanetPreset, EcologyPreset> callback) {
        this.onScenarioLoadedCallback = callback;
    }

    /**
     * Get cells.
     *
     * @return the resulting computation or state reference
     */
    public List<H3Cell> getCells() { return currentPreviewCells; }

    /**
     * Set generated cells.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     */
    public void setGeneratedCells(List<H3Cell> cells) {
        this.currentPreviewCells = cells;
        if (cells != null && !cells.isEmpty()) {
            distributeInitialPopulation(cells);
        }
        drawPreview();
    }

    /**
     * Ensure preview generated if needed.
     *
     */
    public void ensurePreviewGeneratedIfNeeded() {
        if (currentPreviewCells == null || currentPreviewCells.isEmpty()) {
            generatePreview();
        } else {
            drawPreview();
        }
    }

    /**
     * Get scheduled events.
     *
     * @return the resulting computation or state reference
     */
    public List<ClimateEvent> getScheduledEvents() {
        return eventsList != null ? new ArrayList<>(eventsList) : new ArrayList<>();
    }

    private void updatePerformanceControlsState() {
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

    private java.util.function.Supplier<PlanetGeneratorPanel> planetPanelSupplier;
    private java.util.function.Supplier<ResourceDistributionPanel> resourcePanelSupplier;

    /**
     * Set planet panel supplier.
     *
     * @param supplier the supplier parameter (java.util.function.Supplier&lt;PlanetGeneratorPanel&gt;)
     */
    public void setPlanetPanelSupplier(java.util.function.Supplier<PlanetGeneratorPanel> supplier) {
        this.planetPanelSupplier = supplier;
    }

    /**
     * Set resource panel supplier.
     *
     * @param supplier the supplier parameter (java.util.function.Supplier&lt;ResourceDistributionPanel&gt;)
     */
    public void setResourcePanelSupplier(java.util.function.Supplier<ResourceDistributionPanel> supplier) {
        this.resourcePanelSupplier = supplier;
    }

    /**
     * Get scenario validation errors.
     *
     * @return the resulting computation or state reference
     */
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
