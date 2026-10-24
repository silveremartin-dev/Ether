/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

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
        if (control == null) return;

        String defaultStr;
        if (defaultValue instanceof Double d) {
            defaultStr = String.format(java.util.Locale.US, "%.2f", d);
        } else {
            defaultStr = String.valueOf(defaultValue);
        }

        // 1. Tooltip enhancement
        Tooltip currentTooltip = control.getTooltip();
        String baseTooltip = currentTooltip != null ? currentTooltip.getText() : "";
        if (!baseTooltip.contains("[DEFAULT VALUE")) {
            String defaultNotice = (baseTooltip.isEmpty() ? "" : "\n\n") + I18n.getOrDefault("scenario.tooltip.default_notice_prefix", "📌 [DEFAULT VALUE: ") + defaultStr + I18n.getOrDefault("scenario.tooltip.default_notice_suffix", "] — Right-click control to reset.");
            control.setTooltip(new Tooltip(baseTooltip + defaultNotice));
        }

        // 2. Right-click Context Menu
        ContextMenu contextMenu = control.getContextMenu();
        if (contextMenu == null) {
            contextMenu = new ContextMenu();
        }
        MenuItem resetItem = new MenuItem(I18n.getOrDefault("scenario.menu.reset_default", "🔄 Reset to default value (") + defaultStr + ")");
        resetItem.setOnAction(e -> {
            if (resetAction != null) {
                resetAction.run();
                notifyParamChange();
            }
        });
        contextMenu.getItems().add(resetItem);
        control.setContextMenu(contextMenu);
    }

    private void updateDefaultValueIndicators() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (isUpdatingFromPreset) return;

        highlightControlIfModified(startYearSpinner, false);
        highlightControlIfModified(endYearSpinner, false);
        highlightControlIfModified(targetCohortSizeSpinner, false);
        highlightControlIfModified(temporalResolutionCombo, false);
        highlightControlIfModified(initialHumanCountSpinner, false);

        highlightControlIfModified(cultureVectorDimSpinner, cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null && cultureVectorDimSpinner.getValue() != 9);
        highlightControlIfModified(culturalDiffusionRateSpinner, culturalDiffusionRateSpinner != null && culturalDiffusionRateSpinner.getValue() != null && Math.abs(culturalDiffusionRateSpinner.getValue() - 0.05) > 0.001);
        highlightControlIfModified(culturalMutationRateSpinner, culturalMutationRateSpinner != null && culturalMutationRateSpinner.getValue() != null && Math.abs(culturalMutationRateSpinner.getValue() - 0.01) > 0.001);
        highlightControlIfModified(strictDeterminismCheckBox, strictDeterminismCheckBox != null && strictDeterminismCheckBox.isSelected());
        highlightControlIfModified(climateTickFreqSlider, climateTickFreqSlider != null && Math.abs(climateTickFreqSlider.getValue() - 6.0) > 0.001);
        highlightControlIfModified(threadCountSlider, threadCountSlider != null && Math.abs(threadCountSlider.getValue() - 4.0) > 0.001);

        for (Map.Entry<String, Map<String, Spinner<Double>>> engEntry : typeBParamSpinnersMap.entrySet()) {
            // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
            for (Map.Entry<String, Spinner<Double>> pEntry : engEntry.getValue().entrySet()) {
                Spinner<Double> sp = pEntry.getValue();
                if (sp != null && sp.getValue() != null) {
                    double defVal = getTypeBParamDefault(engEntry.getKey(), pEntry.getKey());
                    highlightControlIfModified(sp, Math.abs(sp.getValue() - defVal) > 0.0001);
                }
            }
        }
    }

    private double getTypeBParamDefault(String engineKey, String paramKey) {
        if ("MaritimeHighwayEngine".equals(engineKey)) {
            if ("capitalBoostRate".equals(paramKey)) return 0.05;
            if ("frictionMultiplier".equals(paramKey)) return 0.20;
        } else if ("HydrologicalEngineeringEngine".equals(engineKey)) {
            if ("tenochtitlanTech".equals(paramKey)) return 3.5;
            if ("tenochtitlanCapital".equals(paramKey)) return 150.0;
            if ("aralRainfallThreshold".equals(paramKey)) return 300.0;
            if ("damMinElevation".equals(paramKey)) return 300.0;
        } else if ("FertileCrescentSalinizationEngine".equals(engineKey)) {
            if ("salinizationRate".equals(paramKey)) return 0.02;
        } else if ("LandReclamationEngine".equals(engineKey)) {
            if ("polderTechThreshold".equals(paramKey)) return 4.0;
            if ("polderCapitalThreshold".equals(paramKey)) return 100.0;
        }
        return 0.0;
    }

    private void highlightControlIfModified(Control control, boolean isModified) {
        if (control == null) return;
        if (isModified) {
            control.setStyle("-fx-border-color: #f59e0b; -fx-border-width: 1.5px; -fx-border-radius: 4px;");
        } else {
            control.setStyle("");
        }
    }

    /**
     * Reset all to defaults.
     *
     */
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

    // Population & Density Controls
    private Spinner<Long> initialHumanCountSpinner;
    private ComboBox<String> densityPatternCombo;
    private ComboBox<Scenario.TechPreset> techPresetCombo;
    private Spinner<Double> customCapitalSpinner;
    private Spinner<Double> customEnergySpinner;
    private Spinner<Double> customFoodSpinner;
    private Spinner<Double> customInfoSpinner;
    private VBox customPhysicalSubPanel;

    // Density Map Import/Export Controls
    private ComboBox<String> demoSourceCombo;
    private Label demoSourceLabel;
    private Image customDensityImage;
    private Label densityMapFileLabel;
    private Button loadDensityMapBtn;
    private Button clearDensityMapBtn;
    private Button demoHelpBtn;
    private Label densityFormatHintLabel;
    private Button exportDensityMapBtn;

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

    private Spinner<Integer> cultureVectorDimSpinner;
    private Spinner<Double> culturalDiffusionRateSpinner;
    private Spinner<Double> culturalMutationRateSpinner;
    private Image customIsoglossImage;
    private Image customKinshipImage;
    private Image customRitualsImage;
    private Image customSovereigntyImage;
    private Label isoglossFileLabel;
    private Label kinshipFileLabel;
    private Label ritualsFileLabel;
    private final java.util.Map<Integer, Image> customTensorImages = culturalSectionBuilder.getCustomTensorImages();
    private final java.util.Map<Integer, String> customTensorNames = culturalSectionBuilder.getCustomTensorNames();
    private final java.util.Map<Integer, RadioButton> tensorProcRadios = culturalSectionBuilder.getTensorProcRadios();
    private final java.util.Map<Integer, RadioButton> tensorImportRadios = culturalSectionBuilder.getTensorImportRadios();
    private final java.util.Map<Integer, Slider> tensorParam1Sliders = culturalSectionBuilder.getTensorParam1Sliders();
    private final java.util.Map<Integer, Slider> tensorParam2Sliders = culturalSectionBuilder.getTensorParam2Sliders();
    private final java.util.Map<Integer, Slider> tensorParam3Sliders = culturalSectionBuilder.getTensorParam3Sliders();
    private final java.util.Map<Integer, TextField> tensorSeedFields = culturalSectionBuilder.getTensorSeedFields();
    private final java.util.Map<Integer, ComboBox<String>> tensorSourceCombos = culturalSectionBuilder.getTensorSourceCombos();
    private final java.util.Map<Integer, Button> tensorLoadBtns = culturalSectionBuilder.getTensorLoadBtns();
    private VBox layersDynamicContainer;
    private Button culturalHelpBtn;

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
        @Override public Image getCustomDensityImage() { return customDensityImage; }
        @Override public boolean isDensityImportActive() { return radioImportDemo != null && radioImportDemo.isSelected(); }
        @Override public long getInitialHumanCount() { return initialHumanCountSpinner != null && initialHumanCountSpinner.getValue() != null ? initialHumanCountSpinner.getValue() : 1_000_000L; }
        @Override public String getDensityPattern() { return densityPatternCombo != null && densityPatternCombo.getValue() != null ? densityPatternCombo.getValue() : "UNBIASED_NATURAL"; }
        @Override public String getDemoSeed() { return demoSeedField != null ? demoSeedField.getText() : ""; }
        @Override public String getCultSeed() { return cultSeedField != null ? cultSeedField.getText() : ""; }
        @Override public int getH3Resolution() { return h3ResolutionCombo != null && h3ResolutionCombo.getValue() != null ? h3ResolutionCombo.getValue() : 3; }
        @Override public boolean isUpdatingFromPreset() { return isUpdatingFromPreset; }
        @Override public double computeAutoCapitalFromYear(long year) { return ScenarioSetupPanel.this.computeAutoCapitalFromYear(year); }
        @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
        @Override public void notifyParamChange() { ScenarioSetupPanel.this.notifyParamChange(); }
        @Override public void applySpatialClippingBounds(double minLat, double maxLat, double minLon, double maxLon) {
            ScenarioSetupPanel.this.applySpatialClippingBounds(minLat, maxLat, minLon, maxLon);
        }
        @Override public boolean isGraphicSelectActive() { return spatialBoundsBuilder.isGraphicSelectActive(); }
        @Override public void setGraphicSelectActive(boolean active) { spatialBoundsBuilder.setGraphicSelectActive(active); }
        @Override public double getClippingMinLat() { return spatialBoundsBuilder.getMinLat(); }
        @Override public double getClippingMaxLat() { return spatialBoundsBuilder.getMaxLat(); }
        @Override public double getClippingMinLng() { return spatialBoundsBuilder.getMinLng(); }
        @Override public double getClippingMaxLng() { return spatialBoundsBuilder.getMaxLng(); }
        @Override public boolean isClippingEnabled() { return spatialBoundsBuilder.isClippingEnabled(); }
        @Override public Image getElevationImageForPreset(PlanetPreset preset) { return ScenarioSetupPanel.this.getElevationImageForPreset(preset); }
        @Override public void applyPresetMapToCells(List<H3Cell> cells, PlanetPreset cfg) { ScenarioSetupPanel.this.applyPresetMapToCells(cells, cfg); }
        @Override public void onPreviewCellsGenerated(List<H3Cell> cells) { ScenarioSetupPanel.this.currentPreviewCells = cells; }
    });

    // Map Preview
    private Canvas previewCanvas;
    private ComboBox<String> previewModeCombo;
    private ToggleButton btnReliefOverlay;
    private CheckBox reliefOverlayCheckBox;
    private Label previewStatusLabel;

    // Execution & Calculation Controls
    private ProgressBar progressBar;
    private Label progressStatusLabel;
    private ProgressBar externalProgressBar;
    private Label externalStatusLabel;
    private javafx.scene.Node externalOverlayContainer;

    // State
    /** Internal state: current preview cells (List&lt;H3Cell&gt;). */
    private List<H3Cell> currentPreviewCells;
    private PlanetPreset activePlanetPreset;
    private final java.util.concurrent.atomic.AtomicBoolean isPreviewGenerating = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final Consumer<Scenario> onStartSimulation;
    private final org.ether.society.persistence.ScenarioRepository scenarioRepo;

    // Labels for i18n
    private Label headerLabel;
    private Label title1;
    private Label popHeader;
    private Label title3Events;
    private Label nameLabel;
    private Label startYearLabel;
    private Label endYearLabel;
    private Label popCountLabel;
    private Label densityPatternLabel;
    private Button generateBtn;
    private Button startBtn;

    // Seed & Random Events Controls for Tab 3
    private TextField demoSeedField;
    private TextField cultSeedField;
    private Button demoRandSeedBtn;
    private CheckBox randomEventsCheckBox; // legacy alias for randomBiophysicalEventsCheckBox
    private CheckBox randomBiophysicalEventsCheckBox;
    private CheckBox randomPoliticalEventsCheckBox;
    private CheckBox earthLeadersCheckBox;
    private CheckBox proceduralLeadersCheckBox;

    // Spatial Clipping & Boundary Condition Controls
    private Label clippingHeader;
    private Label oceanOptHeader;
    private Label cultureHeader;
    private CheckBox clippingCheckBox;
    private ToggleButton graphicSelectBtn;
    private Spinner<Double> minLatSpinner;
    private Spinner<Double> maxLatSpinner;
    private Spinner<Double> minLngSpinner;
    private Spinner<Double> maxLngSpinner;
    private ComboBox<String> boundaryModeCombo;
    private Button resetClippingBtn;
    private VBox clippingSubPanel;
    private Button btnGenerateProceduralTensorsSection;
    private ComboBox<String> engineSortCombo;
    private Label engineSortLabel;

    // Navigation & Selection State
    /** Internal state: zoom factor (double). */
    private double zoomFactor = 1.0;
    /** Internal state: pan x (double). */
    private double panX = 0.0;
    /** Internal state: pan y (double). */
    private double panY = 0.0;
    /** Internal state: drag start x (double). */
    private double dragStartX, dragStartY;
    /** Internal state: is selection drag (boolean). */
    private boolean isSelectionDrag = false;
    /** Internal state: selection start x (double). */
    private double selectionStartX, selectionStartY;
    /** Internal state: selection current x (double). */
    private double selectionCurrentX, selectionCurrentY;

    // Density Map Import UI Fields
    private RadioButton radioProcDemo;
    private RadioButton radioImportDemo;
    private Label demoCompatibilityLabel;

    private static final Map<String, String> DENSITY_LABELS = Map.ofEntries(
        Map.entry("UNBIASED_NATURAL", "⚖ Natural Bio-Climatic Equilibrium (Unbiased)"),
        Map.entry("COASTAL_MARITIME", "🌊 Coastal & Maritime Focus"),
        Map.entry("RIVER_VALLEYS", "ðŸž River Valleys & Alluvial Basins"),
        Map.entry("HIGHLAND_MOUNTAIN", "ðŸ” Highland Refuges & Mountain Reliefs"),
        Map.entry("INLAND_OASIS", "🌴 Inland Hydrographic Basins & Oases"),
        Map.entry("EQUATORIAL_BELT", "â˜€ï¸ Equatorial Belt & Tropical Zone"),
        Map.entry("URBAN_CLUSTERS", "ðŸ™ Metropolises & Urban Clusters"),
        Map.entry("SPARSE_NOMADIC", "⛺ Sparse Nomadic & Pastoralist Dispersion"),
        Map.entry("UNIFORM", "🟦 Absolute Uniform Distribution"),
        Map.entry("RANDOM", "🎲 Stochastic Random Distribution")
    );

    private static final Map<String, String> DENSITY_DESCRIPTIONS = Map.ofEntries(
        Map.entry("UNBIASED_NATURAL", "⚖ Natural Bio-Climatic Equilibrium: Pure physical model without artificial bias. Population settles strictly according to real environmental suitability (biomes, temperature, waterways, relief)."),
        Map.entry("COASTAL_MARITIME", "🌊 Coastal & Maritime Focus: Encourages human settlement along ocean coastlines, river deltas, and maritime margins."),
        Map.entry("RIVER_VALLEYS", "ðŸž River Valleys & Alluvial Basins: High population concentration along major hydrographic river networks and fertile alluvial basins."),
        Map.entry("HIGHLAND_MOUNTAIN", "ðŸ” Highland Refuges & Mountain Reliefs: Preferential densification in high valleys, mountain plateaus, and defensive elevated reliefs."),
        Map.entry("INLAND_OASIS", "🌴 Inland Basins & Oases: Settlement around endorheic depressions and accessible groundwater tables."),
        Map.entry("EQUATORIAL_BELT", "â˜€ï¸ Equatorial Belt: Primary settlement concentrated around equatorial latitudes and high solar irradiance zones."),
        Map.entry("URBAN_CLUSTERS", "ðŸ™ Metropolises & Urban Clusters: Procedural emergence of multiple dense urban aggregation hubs."),
        Map.entry("SPARSE_NOMADIC", "⛺ Sparse Nomadic Dispersion: Low-density pastoral population dispersed broadly across all viable biomes."),
        Map.entry("UNIFORM", "🟦 Absolute Uniform Distribution: Strictly constant population density across all land cells on the H3 grid."),
        Map.entry("RANDOM", "🎲 Stochastic Distribution: Uniformly random population allocation across land cells.")
    );

    // Clipping & Boundary Label Fields for live i18n
    private Label latMaxLabel;
    private Label latMinLabel;
    private Label lngMinLabel;
    private Label lngMaxLabel;
    private Label boundaryLabel;
    private Label previewTitleLabel;

    // Ocean & Engine Optimization Architecture Checkboxes (Persisted at Scenario Level for Physical Conformance)
    private CheckBox strictDeterminismCheckBox;
    private CheckBox sparseCellSkippingCheckBox;
    private CheckBox oceanMacroAggregationCheckBox;
    private CheckBox coastalNavigationOnlyCheckBox;
    private CheckBox oceanMultiRateTickingCheckBox;
    private Slider climateTickFreqSlider;
    private Label climateTickFreqValueLabel;
    private CheckBox parallelExecutionCheckBox;
    private Slider threadCountSlider;
    private Label threadCountValueLabel;
    private Label ecoPromptLabel;
    private Label planetPromptLabel;
    private Label optSubHeader;

    private static class CoreEngineRow {
        final String id;
        final Label descLabel;
        final HBox row;
        final String defaultTitle;
        final String defaultDesc;
        final String defaultRef;
        final String defaultEq;

        CoreEngineRow(String id, Label descLabel, HBox row, String defaultTitle, String defaultDesc, String defaultRef, String defaultEq) {
            this.id = id;
            this.descLabel = descLabel;
            this.row = row;
            this.defaultTitle = defaultTitle;
            this.defaultDesc = defaultDesc;
            this.defaultRef = defaultRef;
            this.defaultEq = defaultEq;
        }
    }

    private static class OptionalEngineMeta {
        final String id;
        final CheckBox cb;
        final String defaultTitle;
        final String defaultDesc;
        final String defaultRef;
        final String defaultEq;

        OptionalEngineMeta(String id, CheckBox cb, String defaultTitle, String defaultDesc, String defaultRef, String defaultEq) {
            this.id = id;
            this.cb = cb;
            this.defaultTitle = defaultTitle;
            this.defaultDesc = defaultDesc;
            this.defaultRef = defaultRef;
            this.defaultEq = defaultEq;
        }
    }

    private final java.util.List<CoreEngineRow> coreEngineRows = new java.util.ArrayList<>();
    private final java.util.List<OptionalEngineMeta> optionalEngineMetas = new java.util.ArrayList<>();
    private TitledPane corePane;
    private TitledPane typeBPane;
    private Label coreExplanationLabel;
    private Button exportCoreTemplateBtn;
    private Button exportTemplateBtn;
    private Button importEngineBtn;
    private Button autoSelectEnginesForYearBtn;

    private CheckBox spatialRangeTruncationCheckBox;
    private final java.util.Map<String, CheckBox> typeBCheckBoxMap = new java.util.HashMap<>();
    private final java.util.Map<String, java.util.Map<String, Spinner<Double>>> typeBParamSpinnersMap = new java.util.HashMap<>();
    private VBox typeBBoxContainer;
    private VBox liveDiagnosticCard;
    private Label liveDiagnosticHeader;
    private VBox liveDiagnosticContentBox;
    /** Internal state: selected engine class name (String). */
    private String selectedEngineClassName = "FrontierAsabiyyahEngine";
    private Button btnExportBundle;
    private Button btnImportBundle;
    private Label bundleSubtitle;

    // Async Calculation & Thread Control Fields
    /** Internal state: boolean (volatile). */
    private volatile boolean isCalculationRunning = false;
    /** Internal state: boolean (volatile). */
    private volatile boolean cancelRequested = false;
    private Thread generationThread = null;
    private java.util.function.Supplier<Boolean> isSimulationRunningSupplier;

    /**
     * Set is simulation running supplier.
     *
     * @param isSimulationRunningSupplier the is simulation running supplier parameter (java.util.function.Supplier&lt;Boolean&gt;)
     */
    public void setIsSimulationRunningSupplier(java.util.function.Supplier<Boolean> isSimulationRunningSupplier) {
        this.isSimulationRunningSupplier = isSimulationRunningSupplier;
    }

    // Events section
    private TableView<ClimateEvent> eventsTable;
    private org.ether.society.ui.scenario.ScenarioEventsSectionBuilder eventsSectionBuilder;
    private ObservableList<ClimateEvent> eventsList;
    private TableColumn<ClimateEvent, String> colType;
    private TableColumn<ClimateEvent, String> colName;
    private TableColumn<ClimateEvent, Integer> colYear;
    private TableColumn<ClimateEvent, Double> colLat;
    private TableColumn<ClimateEvent, Double> colLon;
    private TableColumn<ClimateEvent, Double> colDepth;
    private TableColumn<ClimateEvent, Double> colMag;
    private Button addEventBtn;
    private Button removeEventBtn;
    private Button clearEventsBtn;
    private Button loadEarthEventsBtn;
    private Button loadEarthLeadersBtn;

    // Snapshot Management UI Fields
    private RadioButton radioNewSimulation;
    private RadioButton radioResumeSnapshot;
    private ComboBox<org.ether.society.persistence.SaveMetadata> snapshotCombo;
    private VBox snapshotContainer;
    private Label snapshotDateLabel;
    private Label snapshotTimeLabel;
    private Label snapshotScenarioLabel;
    private Label snapshotPathLabel;
    private Button snapshotExplainBtn;
    private Button snapshotRefreshBtn;
    private ScrollPane configScroll;
    private VBox validationErrorBanner;
    private Label validationBannerHeaderLabel;
    private Label validationErrorLabel;
    private org.ether.society.persistence.SimulationSaveManager saveManagerForUI = new org.ether.society.persistence.SimulationSaveManager();

    private org.ether.society.persistence.SimulationSaveManager getSaveManager() {
        if (saveManagerForUI == null) {
            saveManagerForUI = new org.ether.society.persistence.SimulationSaveManager();
        }
        return saveManagerForUI;
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
    public ScenarioSetupPanel(Consumer<Scenario> onStartSimulation) {
        this.onStartSimulation = onStartSimulation;
        this.scenarioRepo = new org.ether.society.persistence.ScenarioRepository();
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
        VBox root = new VBox(15);
        root.setPadding(new Insets(0, 20, 0, 0));

        headerLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.panel.title", "📜 Scenario Configuration & Setup"));
        headerLabel.getStyleClass().add("label-title");
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setMaxWidth(Double.MAX_VALUE);

        validationBannerHeaderLabel = new Label(I18n.getOrDefault("scenario.validation.header_errors", "⚠️ï¸ VALIDATION ERRORS DETECTED — PLEASE CORRECT THE FOLLOWING POINTS:"));
        validationBannerHeaderLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 13px;");

        validationErrorLabel = new Label();
        validationErrorLabel.setWrapText(true);
        validationErrorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 12px;");

        validationErrorBanner = new VBox(6,
                validationBannerHeaderLabel,
                validationErrorLabel
        );
        validationErrorBanner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10;");
        validationErrorBanner.setVisible(false);
        validationErrorBanner.setManaged(false);

        // --- 1. Standardized Preset Control Bar for Scenarios ---
        scenarioPresetBar = new PresetControlBar<>("scenario.preset_bar", "1. PRÉRÉGLAGES DE SCÉNARIOS");
        scenarioPresetBar.setExportCategory("scenario");
        List<Scenario> builtInScenarios = getBuiltInScenarios();
        Scenario defaultScenario = builtInScenarios.isEmpty() ? null : builtInScenarios.get(0);

        scenarioPresetBar.setListener(new PresetControlBar.PresetActionsListener<Scenario>() {
            /**
             * On preset selected.
             *
             * @param scenario the scenario parameter (Scenario)
             */
            @Override
            public void onPresetSelected(Scenario scenario) {
                applyScenarioToUI(scenario);
                if (previewModeCombo != null) {
                    previewModeCombo.getSelectionModel().select(0);
                }
            }

            /**
             * On save preset.
             *
             * @param name the name parameter (String)
             */
            @Override
            public void onSavePreset(String name) {
                if (!validateScenarioSetup()) {
                    return;
                }
                Scenario custom = getScenario();
                custom.setName(name);
                scenarioRepo.saveOrUpdate(custom);
                scenarioPresetBar.getPresetCombo().getItems().removeIf(s -> s != null && name.equalsIgnoreCase(s.getName()));
                scenarioPresetBar.getPresetCombo().getItems().add(custom);
                scenarioPresetBar.getPresetCombo().setValue(custom);
                scenarioPresetBar.setNameText(name);
            }

            /**
             * On delete preset.
             *
             * @param scenario the scenario parameter (Scenario)
             */
            @Override
            public void onDeletePreset(Scenario scenario) {
                scenarioPresetBar.getPresetCombo().getItems().remove(scenario);
            }

            /**
             * On export preset.
             *
             * @param targetFile the target file parameter (File)
             * @param scenario the scenario parameter (Scenario)
             */
            @Override
            public void onExportPreset(File targetFile, Scenario scenario) {
                try {
                    ObjectMapper mapper = org.ether.society.persistence.EtherJsonUtil.getMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.writeValue(targetFile, scenario);
                    logger.info("Exported scenario preset to {}", targetFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to export scenario preset", ex);
                }
            }

            /**
             * On import preset.
             *
             * @param sourceFile the source file parameter (File)
             */
            @Override
            public void onImportPreset(File sourceFile) {
                try {
                    ObjectMapper mapper = org.ether.society.persistence.EtherJsonUtil.getMapper();
                    Scenario scenario = mapper.readValue(sourceFile, Scenario.class);
                    scenarioPresetBar.getPresetCombo().getItems().add(scenario);
                    scenarioPresetBar.getPresetCombo().setValue(scenario);
                    applyScenarioToUI(scenario);
                    logger.info("Imported scenario preset from {}", sourceFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to import scenario preset", ex);
                }
            }
        });

        // --- Inherited Presets (Tab 1 Planet + Tab 2 Ecology) ---
        VBox inheritedSection = planetSyncManager.buildSection();
        planetPresetCombo = planetSyncManager.getPlanetPresetCombo();
        ecologyPresetCombo = planetSyncManager.getEcologyPresetCombo();
        inheritedContextLabel = planetSyncManager.getInheritedContextLabel();

        // --- Scenario General Info Section ---
        VBox section1 = spatiotemporalSectionBuilder.buildSection();
        startYearSpinner = spatiotemporalSectionBuilder.getStartYearSpinner();
        endYearSpinner = spatiotemporalSectionBuilder.getEndYearSpinner();
        h3ResolutionCombo = spatiotemporalSectionBuilder.getH3ResolutionCombo();
        targetCohortSizeSpinner = spatiotemporalSectionBuilder.getTargetCohortSizeSpinner();
        temporalResolutionCombo = spatiotemporalSectionBuilder.getTemporalResolutionCombo();

        // --- 2. Demographics & Density Map Management ---
        VBox popSection = createPopulationDensitySection();

        // --- 4. Cultural Vector & Multi-Field Layers Section (Tab 3) ---
        VBox cultureSection = createCulturalVectorAndLayersSection();

        // --- 5. Spatial Clipping & Boundary Condition Section ---
        VBox clippingSection = createClippingSection();

        // --- 6. Ocean Optimizations Section (Persisted in Scenario for Determinism) ---
        VBox oceanOptSection = createOceanOptimizationSection();

        // --- 7. Events Section ---
        VBox eventsSection = createEventsSection();

        // --- 8. Actions & Progress Bar (Deferred Execution) ---
        generateBtn = new Button();
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        generateBtn.getStyleClass().add("button-secondary");
        generateBtn.setOnAction(e -> generatePreview());
        generateBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.preview", "Quick preview of demographic distribution.")));

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        progressStatusLabel = new Label();
        progressStatusLabel.getStyleClass().add("control-label");
        progressStatusLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        progressStatusLabel.setVisible(false);
        progressStatusLabel.setManaged(false);

        // Real-Time Integrated Diagnostic Block
        liveDiagnosticCard = new VBox(8);
        liveDiagnosticCard.getStyleClass().add("card-section");

        liveDiagnosticHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.diagnostic.header", "📋 9. CIVILIZATIONAL VIABILITY DIAGNOSTIC (REAL TIME)"));
        liveDiagnosticHeader.getStyleClass().add("label-section-header");

        liveDiagnosticContentBox = new VBox(4);
        liveDiagnosticCard.getChildren().addAll(liveDiagnosticHeader, liveDiagnosticContentBox);
        updateLiveDiagnosticBlock();

        startBtn = new Button();
        startBtn.setPrefHeight(50);
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
        startBtn.setOnAction(e -> handleStartOrCancel());
        startBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.start", "Calculate H3 cells and launch simulation.")));
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

        scenarioPresetHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.section.presets", "ðŸŽ›ï¸ GLOBAL PRESETS & SCENARIO SAVE"));
        VBox scenarioPresetSection = createSection(scenarioPresetHeader, new VBox(8, scenarioPresetBar, btnAutoEpochScenario, scenarioDescLabel, scenarioDescriptionArea));

        VBox snapshotSection = createSnapshotSection();
        VBox bundleSection = createBundleSection();

        bottomActionBox = new VBox(8, progressBar, progressStatusLabel, startBtn);
        bottomActionBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(headerLabel, validationErrorBanner, scenarioPresetSection, inheritedSection, section1, popSection, cultureSection, clippingSection, oceanOptSection, eventsSection, snapshotSection, bundleSection, liveDiagnosticCard);

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
        javafx.application.Platform.runLater(() -> {
            try {
                List<Scenario> all = getBuiltInScenarios();
                Scenario current = scenarioPresetBar != null && scenarioPresetBar.getPresetCombo() != null
                        ? scenarioPresetBar.getPresetCombo().getValue() : null;
                if (scenarioPresetBar != null) {
                    scenarioPresetBar.setPresets(all, current != null ? current : (all.isEmpty() ? null : all.get(0)));
                }
                refreshSnapshotList();
                logger.info("Successfully refreshed {} scenario preset(s) into UI.", all.size());
            } catch (Exception e) {
                logger.warn("Could not refresh scenario presets: {}", e.getMessage());
            }
        });
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
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (startBtn == null) return;
        boolean dirty = isDirty();
        Scenario s = getScenario();
        long startYr = s != null ? s.getStartDateYear() : -100000L;
        long endYr = s != null ? s.getEndDateYear() : 2026L;

        if (dirty) {
            if (radioResumeSnapshot != null) {
                radioResumeSnapshot.setDisable(true);
                if (radioResumeSnapshot.isSelected() && radioNewSimulation != null) {
                    radioNewSimulation.setSelected(true);
                }
            }
            startBtn.setText(I18n.getOrDefault("scenario.btn.start_modified", "🌱 CALCULER & DÉMARRER LA SIMULATION (Paramètres Modifiés)"));
            startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #d97706; -fx-text-fill: white; -fx-background-radius: 6;");
            startBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.start_modified", 
                "Lance une simulation avec vos paramètres personnalisés sans écraser les snapshots du scénario d'origine.")));
            return;
        }

        var selectedSnapshot = getSelectedSnapshotMetadata();
        boolean hasSnapshots = snapshotCombo != null && snapshotCombo.getItems() != null && !snapshotCombo.getItems().isEmpty();

        if (radioResumeSnapshot != null) {
            radioResumeSnapshot.setDisable(!hasSnapshots);
        }

        if (isResumeFromSnapshotSelected() && selectedSnapshot != null) {
            long snapYr = selectedSnapshot.getYear();
            if (snapYr >= endYr) {
                startBtn.setText(String.format(I18n.getOrDefault("scenario.btn.load_complete_snapshot", "⚡ CHARGER L'HISTORIQUE & ÉTAT COMPLET (An %,d — 100%% Calculé)"), snapYr));
                startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-background-radius: 6;");
                startBtn.setTooltip(new Tooltip(String.format(I18n.getOrDefault("scenario.tooltip.load_complete_snapshot",
                    "Ce scénario dispose d'un historique complet calculé jusqu'à l'An %,d.\nChargement instantané sans recalcul : navigation temporelle et cartographique immédiate."), snapYr)));
            } else {
                startBtn.setText(String.format(I18n.getOrDefault("scenario.btn.resume_partial_snapshot", "🚀 REPRENDRE LE CHECKPOINT & CONTINUER (An %,d ➔ %,d)"), snapYr, endYr));
                startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 6;");
                startBtn.setTooltip(new Tooltip(String.format(I18n.getOrDefault("scenario.tooltip.resume_partial_snapshot",
                    "Reprend la simulation à partir du snapshot de l'An %,d.\nSeules les étapes postérieures (An %,d ➔ %,d) seront calculées."), snapYr, snapYr, endYr)));
            }
        } else {
            startBtn.setText(String.format(I18n.getOrDefault("scenario.button.start_fresh", "🌱 CALCULER & DÉMARRER LA SIMULATION (An %,d ➔ %,d)"), startYr, endYr));
            startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
            startBtn.setTooltip(new Tooltip(String.format(I18n.getOrDefault("scenario.tooltip.start_fresh",
                "Calcule une trajectoire depuis l'An %,d jusqu'à l'An %,d sans utiliser de snapshot existant."), startYr, endYr)));
        }
    }


    private void runPreFlightSanityCheck() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        updateLiveDiagnosticBlock();
    }

    private static org.ether.society.engines.compiler.EngineConflictReport getOrCreateJitReport() {
        return org.ether.society.ui.scenario.ScenarioSanityValidator.getOrCreateJitReport();
    }

    private void renderDiagnosticCategory(VBox container, String title, List<String> items) {
        org.ether.society.ui.scenario.ScenarioSanityValidator.renderDiagnosticCategory(container, title, items);
    }

    private void updateLiveDiagnosticBlock() {
        if (liveDiagnosticContentBox == null) return;
        org.ether.society.generation.PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : org.ether.society.generation.PlanetPreset.EARTH_LIKE);
        if (p == null) p = org.ether.society.generation.PlanetPreset.EARTH_LIKE;
        org.ether.society.model.EcologyPreset eco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : org.ether.society.model.EcologyPreset.EARTH_STANDARD;
        String demoSrc = demoSourceCombo != null ? demoSourceCombo.getValue() : null;
        javafx.scene.image.Image elevImg = getElevationImageForPreset(p);
        int dimsCount = cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null ? cultureVectorDimSpinner.getValue() : 9;
        long startY = startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000L;
        long initPop = initialHumanCountSpinner != null && initialHumanCountSpinner.getValue() != null ? initialHumanCountSpinner.getValue() : 1_000_000L;

        org.ether.society.ui.scenario.ScenarioSanityValidator.updateLiveDiagnostics(
            liveDiagnosticContentBox,
            liveDiagnosticHeader,
            p,
            eco,
            demoSrc,
            elevImg,
            customDensityImage,
            dimsCount,
            customTensorImages,
            this::getCulturalTensorTitle,
            startY,
            initPop,
            getScenarioValidationErrors(),
            getOrCreateJitReport()
        );
    }

    private VBox createSection(Label header, javafx.scene.Node content) {
        return createSection(header, content, null);
    }

    private VBox createSection(Label header, javafx.scene.Node content, Runnable sectionResetAction) {
        header.getStyleClass().add("label-section-header");
        HBox headerRow = new HBox(8, header);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        if (sectionResetAction != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button resetSecBtn = new Button("🔄 " + org.ether.society.i18n.I18n.getOrDefault("scenario.btn.reset_section", "Default"));
            resetSecBtn.getStyleClass().add("button-secondary");
            resetSecBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            resetSecBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.reset_section", "Reset section parameters to canonical default values.")));
            resetSecBtn.setOnAction(e -> {
                sectionResetAction.run();
                notifyParamChange();
            });
            headerRow.getChildren().addAll(spacer, resetSecBtn);
        }
        VBox box = new VBox(8, headerRow, content);
        box.getStyleClass().add("card-section");
        return box;
    }

    private List<Scenario> getBuiltInScenarios() {
        return scenarioRepo != null ? scenarioRepo.getAllScenarios() : org.ether.society.persistence.PresetStorageService.loadAllScenarios();
    }

    private void updateInheritedContextDisplay(String ecologyName) {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (inheritedContextLabel == null) return;
        EcologyPreset eco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null;
        String ecoDisplay = eco != null ? org.ether.society.i18n.I18n.getPlanetPresetDisplayName(eco.name()) : (ecologyName != null ? org.ether.society.i18n.I18n.getPlanetPresetDisplayName(ecologyName) : "Earth Standard Baseline");
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : null);
        if (p == null && eco != null) {
            p = findPlanetPresetByName(eco.planetPresetName());
        }
        if (p == null) p = PlanetPreset.EARTH_LIKE;
        String planetDisplay = org.ether.society.i18n.I18n.getPlanetPresetDisplayName(p.name());

        String fmt = org.ether.society.i18n.I18n.getOrDefault("scenario.info.inherited_context_format", "ðŸŒ¿ Ecology (Tab 2): %s  âž”  ðŸª Planet (cascaded): %s (Radius: %,.0f km)");
        inheritedContextLabel.setText(String.format(fmt, ecoDisplay, planetDisplay, p.radiusKm()));
    }

    /**
     * Apply scenario to UI.
     *
     * @param s the scenario to apply
     */
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
            if (demoSourceCombo != null) {
                String autoSrc = pickDemoSourceForEpoch(s.getStartDateYear(), this.activePlanetPreset);
                selectBestSource(demoSourceCombo, autoSrc, "Terre");
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
                className -> selectedEngineClassName = className
            );
        }
        VBox section = engineConfigSectionBuilder.buildSection();
        this.strictDeterminismCheckBox = engineConfigSectionBuilder.getStrictDeterminismCheckBox();
        this.climateTickFreqSlider = engineConfigSectionBuilder.getClimateTickFreqSlider();
        this.threadCountSlider = engineConfigSectionBuilder.getThreadCountSlider();
        this.autoSelectEnginesForYearBtn = engineConfigSectionBuilder.getAutoSelectEnginesForYearBtn();
        this.typeBCheckBoxMap.clear();
        this.typeBCheckBoxMap.putAll(engineConfigSectionBuilder.getTypeBCheckBoxMap());
        this.typeBParamSpinnersMap.clear();
        this.typeBParamSpinnersMap.putAll(engineConfigSectionBuilder.getTypeBParamSpinnersMap());
        return section;
    }

    private void updateEngineTexts() {
        if (engineConfigSectionBuilder != null) {
            engineConfigSectionBuilder.updateTexts();
        }
    }

    private void autoSelectEnginesForYear(long year) {
        if (engineConfigSectionBuilder != null) {
            engineConfigSectionBuilder.autoSelectEnginesForYear(year);
        }
    }

    private VBox createCulturalVectorAndLayersSection() {
        VBox section = culturalSectionBuilder.buildSection();
        cultureVectorDimSpinner = culturalSectionBuilder.getCultureVectorDimSpinner();
        culturalDiffusionRateSpinner = culturalSectionBuilder.getCulturalDiffusionRateSpinner();
        culturalMutationRateSpinner = culturalSectionBuilder.getCulturalMutationRateSpinner();
        cultSeedField = culturalSectionBuilder.getCultSeedField();
        return section;
    }

    private void exportGisMultiFormat() {
        org.ether.society.ui.scenario.ScenarioGisExporter.exportGisMultiFormat(
                getScene() != null ? getScene().getWindow() : null,
                scenarioDescriptionArea != null ? scenarioDescriptionArea.getText() : "Ether Scenario",
                startYearSpinner != null ? startYearSpinner.getValue() : -100000,
                cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9
        );
    }

    private void exportProvenanceManifest() {
        org.ether.society.ui.scenario.ScenarioGisExporter.exportProvenanceManifest(
                getScene() != null ? getScene().getWindow() : null,
                startYearSpinner != null ? startYearSpinner.getValue() : -100000,
                cultSeedField != null ? cultSeedField.getText() : "54321"
        );
    }

    private void showCulturalImportFormatHelp() {
        org.ether.society.ui.scenario.ScenarioGisExporter.showCulturalImportFormatHelp(getScene() != null ? getScene().getWindow() : null);
    }

    private String getDefaultTensorName(int index) {
        return culturalSectionBuilder.getDefaultTensorName(index);
    }

    private String getCulturalTensorTitle(int index) {
        return culturalSectionBuilder.getCulturalTensorTitle(index);
    }

    private String getCulturalTensorTooltip(int index) {
        return culturalSectionBuilder.getCulturalTensorTooltip(index);
    }

    private String getCulturalBaselineName(int index) {
        return culturalSectionBuilder.getCulturalBaselineName(index);
    }

    private String getCulturalFormatHint(int index) {
        return culturalSectionBuilder.getCulturalFormatHint(index);
    }

/**
     * Given the scenario's start year and active planet preset, returns the best-matching
     * entry string for demoSourceCombo, or null if no match can be determined.
     */
    private String pickDemoSourceForEpoch(long startYear, org.ether.society.generation.PlanetPreset planet) {
        return demographicSectionBuilder != null ? demographicSectionBuilder.pickDemoSourceForEpoch(startYear, planet) : "";
    }
    /**
     * Resiliently selects the best matching item in a source ComboBox.
     */
    private void selectBestSource(ComboBox<String> combo, String target, String fallbackKeyword) {
        ScenarioEpochCatalog.selectBestSource(combo, target, fallbackKeyword);
    }

    private String pickCulturalSourceForEpoch(int tensorIdx, long startYear, org.ether.society.generation.PlanetPreset planet) {
        return ScenarioEpochCatalog.pickCulturalSourceForEpoch(tensorIdx, startYear, planet);
    }

    private ComboBox<String> buildPopulationSourceCombo() {
        return demographicSectionBuilder != null ? demographicSectionBuilder.getDemoSourceCombo() : new ComboBox<>();
    }

    private ComboBox<String> buildCulturalSourceCombo(int tensorIdx) {
        return culturalSectionBuilder.buildCulturalSourceCombo(tensorIdx);
    }

    private ScenarioCulturalSectionBuilder.TensorParamDescriptor getTensorParam1Descriptor(int tensorIdx) {
        return culturalSectionBuilder.getTensorParam1Descriptor(tensorIdx);
    }

    private ScenarioCulturalSectionBuilder.TensorParamDescriptor getTensorParam2Descriptor(int tensorIdx) {
        return culturalSectionBuilder.getTensorParam2Descriptor(tensorIdx);
    }

    private ScenarioCulturalSectionBuilder.TensorParamDescriptor getTensorParam3Descriptor(int tensorIdx) {
        return culturalSectionBuilder.getTensorParam3Descriptor(tensorIdx);
    }

    private String getDefaultTensorSeed(int tensorIdx) {
        return culturalSectionBuilder.getDefaultTensorSeed(tensorIdx);
    }

    private void updateTensorFileLabel(int tensorIdx) {
        culturalSectionBuilder.updateTensorFileLabel(tensorIdx);
    }

    private void exportCulturalTensor(int tensorIdx) {
        culturalSectionBuilder.exportCulturalTensor(tensorIdx);
    }

    private Image generateProceduralTensorRasterImage(int tensorIdx, int w, int h) {
        return culturalSectionBuilder.generateProceduralTensorRasterImage(tensorIdx, w, h);
    }

    private Color evaluateProceduralTensorColor(int tIndex, double lat, double lon, double elevation, double p1, double p2, double p3, long seed) {
        return culturalSectionBuilder.evaluateProceduralTensorColor(tIndex, lat, lon, elevation, p1, p2, p3, seed);
    }

    private void deleteCulturalTensor(int indexToDelete) {
        culturalSectionBuilder.deleteCulturalTensor(indexToDelete);
    }

    private void updateCulturalTensorSubBlockTexts() {
        culturalSectionBuilder.updateCulturalTensorSubBlockTexts();
    }

    private void rebuildCulturalTensorSubBlocks(int dimCount) {
        culturalSectionBuilder.rebuildCulturalTensorSubBlocks(dimCount);
    }

    private void loadCustomCultureLayerForTensor(int tensorIdx, Consumer<Image> onLoaded) {
        culturalSectionBuilder.loadCustomCultureLayerForTensor(tensorIdx, onLoaded);
    }

    private void loadCustomCultureLayer(String layerName, Consumer<Image> onLoaded) {
        culturalSectionBuilder.loadCustomCultureLayer(layerName, onLoaded);
    }

private void addCustomEngineCheckBoxToUI(String engineKey, String labelText, String tooltipText, boolean defaultSelected) {
        if (typeBCheckBoxMap.containsKey(engineKey)) {
            typeBCheckBoxMap.get(engineKey).setSelected(defaultSelected);
            return;
        }

        CheckBox cb = new CheckBox(labelText);
        cb.setSelected(defaultSelected);
        cb.getStyleClass().add("custom-engine-checkbox");
        cb.setTooltip(new Tooltip(tooltipText));
        cb.setOnAction(e -> notifyParamChange());

        typeBCheckBoxMap.put(engineKey, cb);

        Label badge = new Label(I18n.getOrDefault("scenario.badge.custom", "[🔌 CUSTOM]"));
        badge.getStyleClass().add("custom-engine-badge");

        HBox row = new HBox(8, badge, cb);
        row.setAlignment(Pos.CENTER_LEFT);

        if (typeBBoxContainer != null) {
            typeBBoxContainer.getChildren().add(row);
        }
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
        VBox popSection = demographicSectionBuilder.buildSection();
        initialHumanCountSpinner = demographicSectionBuilder.getInitialHumanCountSpinner();
        densityPatternCombo = demographicSectionBuilder.getDensityPatternCombo();
        techPresetCombo = demographicSectionBuilder.getTechPresetCombo();
        customCapitalSpinner = demographicSectionBuilder.getCustomCapitalSpinner();
        customEnergySpinner = demographicSectionBuilder.getCustomEnergySpinner();
        customFoodSpinner = demographicSectionBuilder.getCustomFoodSpinner();
        customInfoSpinner = demographicSectionBuilder.getCustomInfoSpinner();
        radioProcDemo = demographicSectionBuilder.getRadioProcDemo();
        radioImportDemo = demographicSectionBuilder.getRadioImportDemo();
        demoSourceCombo = demographicSectionBuilder.getDemoSourceCombo();
        demoCompatibilityLabel = demographicSectionBuilder.getDemoCompatibilityLabel();
        densityMapFileLabel = demographicSectionBuilder.getDensityMapFileLabel();
        popHeader = demographicSectionBuilder.getPopHeader();
        exportDensityMapBtn = demographicSectionBuilder.getExportDensityMapBtn();
        loadDensityMapBtn = demographicSectionBuilder.getLoadDensityMapBtn();
        return popSection;
    }

    private void generateProceduralPopulationDensity() {
        demographicSectionBuilder.generateProceduralPopulationDensity();
    }

    private void loadCustomDensityMap() {
        demographicSectionBuilder.loadCustomDensityMap();
    }

    private void updateDemoCompatibilityDisplay() {
        demographicSectionBuilder.updateDemoCompatibilityDisplay();
    }

    private void exportDensityMap() {
        demographicSectionBuilder.exportDensityMap();
    }

    private void showDensityImportFormatHelp() {
        demographicSectionBuilder.showDensityImportFormatHelp();
    }

    public void generatePreview() {
        if (previewPaneBuilder != null) {
            previewPaneBuilder.generatePreview();
        }
    }
    private void distributeInitialPopulation(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        long totalPop = initialHumanCountSpinner != null && initialHumanCountSpinner.getValue() != null ? initialHumanCountSpinner.getValue() : 1_000_000L;
        double capitalK0 = computeAutoCapitalFromYear(startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000);
        String pattern = densityPatternCombo != null ? densityPatternCombo.getValue() : "UNBIASED_NATURAL";
        boolean isEarthPreset = activePlanetPreset != null && activePlanetPreset.elevationUseImport() 
                && "earth".equalsIgnoreCase(activePlanetPreset.elevationMapSource());
        long startYear = startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000;

        org.ether.society.ui.util.ScenarioCellPopulationDistributor.distributeInitialPopulation(
                cells, totalPop, capitalK0, pattern, isEarthPreset, startYear, customDensityImage, getScenario()
        );
    }

    private Color getReliefAndDensityColor(H3Cell c) {
        return org.ether.society.ui.util.ScenarioCellPopulationDistributor.getReliefAndDensityColor(c);
    }

    private double computeCellCarryingCapacity(H3Cell c) {
        double capK0 = computeAutoCapitalFromYear(startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000);
        return org.ether.society.ui.util.ScenarioCellPopulationDistributor.computeCellCarryingCapacity(c, capK0);
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
    public boolean isDirty() {
        return scenarioPresetBar != null && scenarioPresetBar.isDirty();
    }

    /**
     * Prompt save if dirty.
     *
     * @param owner the owner parameter (javafx.stage.Window)
     * @return the resulting computation or state reference
     */
    public boolean promptSaveIfDirty(javafx.stage.Window owner) {
        if (scenarioPresetBar == null) return true;
        return scenarioPresetBar.promptSavePresetIfDirty(owner);
    }

    /**
     * Get preset bar.
     *
     * @return the resulting computation or state reference
     */
    public PresetControlBar<Scenario> getPresetBar() {
        return scenarioPresetBar;
    }

    private Image getElevationImageForPreset(PlanetPreset planet) {
        return planetSyncManager.getElevationImageForPreset(planet);
    }

    private Image getCachedEarthElevationImage() {
        return getElevationImageForPreset(activePlanetPreset);
    }

    // =========================================================================
    // DEFERRED EXECUTION — Calculate H3 Grid & Start Simulation
    // =========================================================================

    /**
     * Set progress controls.
     *
     * @param bar the bar parameter (ProgressBar)
     * @param label the label parameter (Label)
     * @param overlayContainer the overlay container parameter (javafx.scene.Node)
     */
    public void setProgressControls(ProgressBar bar, Label label, javafx.scene.Node overlayContainer) {
        simulationLauncher.setProgressControls(bar, label, overlayContainer);
    }

    private void updateProgress(double progressVal, String msg) {
        simulationLauncher.updateProgress(progressVal, msg);
    }

    public void invalidateGeneratedState() {
        simulationLauncher.resetStartButtonState();
    }

    private void resetStartButtonState() {
        simulationLauncher.resetStartButtonState();
    }

    private void setStartButtonCompletedState() {
        simulationLauncher.setStartButtonCompletedState();
    }

    private void setStartButtonCancelState() {
        simulationLauncher.setStartButtonCancelState();
    }

    private void handleStartOrCancel() {
        simulationLauncher.handleStartOrCancel();
    }

    private void cancelCalculation() {
        simulationLauncher.cancelCalculation();
    }

    private void startSimulationDeferred() {
        simulationLauncher.startSimulationDeferred();
    }

    private void applyPresetMapToCells(List<H3Cell> cells, PlanetPreset cfg) {
        simulationLauncher.applyPresetMapToCells(cells, cfg, getScenario());
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
            if (liveDiagnosticHeader != null) liveDiagnosticHeader.setText(I18n.getOrDefault("scenario.diagnostic.header", "📋 CIVILIZATIONAL VIABILITY DIAGNOSTIC (REAL TIME)"));
            if (previewTitleLabel != null) previewTitleLabel.setText(I18n.getOrDefault("scenario.title.right_view", "🗺️ Resource Cartography & Display"));
            if (startBtn != null) startBtn.setText(I18n.get("scenario.start_btn"));
            if (generateBtn != null) generateBtn.setText(I18n.getOrDefault("scenario.preview_btn", "🔄 Preview Distribution"));
            if (btnReliefOverlay != null) {
                btnReliefOverlay.setText(I18n.getOrDefault("scenario.btn.relief_overlay", "⛏️ Relief"));
                btnReliefOverlay.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.relief_overlay", "Superposer l'ombrage du relief topographique et des pentes avec délimitation du trait de côte.")));
            }

            planetSyncManager.updateTexts();
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
                initialHumanCountSpinner,
                radioImportDemo,
                customDensityImage,
                cultureVectorDimSpinner,
                tensorImportRadios,
                customTensorImages,
                this::getCulturalTensorTitle
        );
    }

    public boolean validateScenarioSetup() {
        return ScenarioValidationHelper.validateAndHighlight(
                getScenarioValidationErrors(),
                startYearSpinner,
                endYearSpinner,
                initialHumanCountSpinner,
                radioImportDemo,
                customDensityImage,
                loadDensityMapBtn,
                cultureVectorDimSpinner,
                tensorImportRadios,
                customTensorImages,
                tensorLoadBtns,
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
