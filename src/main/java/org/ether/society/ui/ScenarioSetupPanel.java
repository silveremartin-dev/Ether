/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

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
        @Override public boolean isGraphicSelectActive() { return graphicSelectBtn != null && graphicSelectBtn.isSelected(); }
        @Override public void setGraphicSelectActive(boolean active) { if (graphicSelectBtn != null) graphicSelectBtn.setSelected(active); }
        @Override public double getClippingMinLat() { return minLatSpinner != null && minLatSpinner.getValue() != null ? minLatSpinner.getValue() : -90.0; }
        @Override public double getClippingMaxLat() { return maxLatSpinner != null && maxLatSpinner.getValue() != null ? maxLatSpinner.getValue() : 90.0; }
        @Override public double getClippingMinLng() { return minLngSpinner != null && minLngSpinner.getValue() != null ? minLngSpinner.getValue() : -180.0; }
        @Override public double getClippingMaxLng() { return maxLngSpinner != null && maxLngSpinner.getValue() != null ? maxLngSpinner.getValue() : 180.0; }
        @Override public boolean isClippingEnabled() { return clippingCheckBox != null && clippingCheckBox.isSelected(); }
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
    private Label engineInspectorTitle;
    private Label engineInspectorText;
    private Label engineInspectorEquationsTitle;
    private Label engineInspectorEquations;
    private Label engineInspectorRef;
    private Button exportSelectedEngineBtn;
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

    private void updateEngineInspector(String className, String title, String description, String reference, String equations) {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        this.selectedEngineClassName = className;
        if (engineInspectorTitle != null) engineInspectorTitle.setText("🔎 " + className + " — " + title);
        if (engineInspectorText != null) engineInspectorText.setText(LaTeXFormatter.formatLaTeX(description));
        if (engineInspectorEquations != null) engineInspectorEquations.setText(LaTeXFormatter.formatLaTeX(equations));
        if (engineInspectorRef != null) engineInspectorRef.setText("📚 " + reference);
        if (exportSelectedEngineBtn != null) exportSelectedEngineBtn.setText(I18n.getOrDefault("scenario.btn.export_prefix", "📤 Export ") + className + ".java");
    }

    private javafx.scene.Node createEngineParameterBox(String engineKey) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(4);
        grid.setPadding(new Insets(4, 8, 8, 24));
        grid.getStyleClass().add("subcard-section");

        Map<String, Spinner<Double>> spinners = new HashMap<>();

        if ("MaritimeHighwayEngine".equals(engineKey)) {
            Label l1 = new Label(I18n.getOrDefault("scenario.label.capital_boost_rate", "Boost Capital/Tick (α):"));
            l1.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            Spinner<Double> s1 = new Spinner<>(0.0, 0.50, 0.05, 0.01);
            s1.setEditable(true);
            s1.setPrefWidth(90);
            s1.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s1, 0.05, () -> s1.getValueFactory().setValue(0.05));
            grid.add(l1, 0, 0);
            grid.add(s1, 1, 0);
            spinners.put("capitalBoostRate", s1);

            Label l2 = new Label(I18n.getOrDefault("scenario.label.friction_multiplier", "Mult. Friction Eau (μ):"));
            l2.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            Spinner<Double> s2 = new Spinner<>(0.05, 1.00, 0.20, 0.05);
            s2.setEditable(true);
            s2.setPrefWidth(90);
            s2.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s2, 0.20, () -> s2.getValueFactory().setValue(0.20));
            grid.add(l2, 2, 0);
            grid.add(s2, 3, 0);
            spinners.put("frictionMultiplier", s2);

        } else if ("HydrologicalEngineeringEngine".equals(engineKey)) {
            Label l1 = new Label(I18n.getOrDefault("scenario.label.tenochtitlan_tech", "Tenochtitlan Tech Threshold:"));
            l1.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            Spinner<Double> s1 = new Spinner<>(1.0, 10.0, 3.5, 0.5);
            s1.setEditable(true);
            s1.setPrefWidth(90);
            s1.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s1, 3.5, () -> s1.getValueFactory().setValue(3.5));
            grid.add(l1, 0, 0);
            grid.add(s1, 1, 0);
            spinners.put("tenochtitlanTech", s1);

            Label l2 = new Label(I18n.getOrDefault("scenario.label.tenochtitlan_capital", "Tenochtitlan Capital Threshold:"));
            l2.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            Spinner<Double> s2 = new Spinner<>(10.0, 1000.0, 150.0, 10.0);
            s2.setEditable(true);
            s2.setPrefWidth(90);
            s2.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s2, 150.0, () -> s2.getValueFactory().setValue(150.0));
            grid.add(l2, 2, 0);
            grid.add(s2, 3, 0);
            spinners.put("tenochtitlanCapital", s2);

            Label l3 = new Label(I18n.getOrDefault("scenario.label.aral_rain", "Aral Rain Threshold (mm):"));
            l3.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            Spinner<Double> s3 = new Spinner<>(50.0, 1000.0, 300.0, 25.0);
            s3.setEditable(true);
            s3.setPrefWidth(90);
            s3.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s3, 300.0, () -> s3.getValueFactory().setValue(300.0));
            grid.add(l3, 0, 1);
            grid.add(s3, 1, 1);
            spinners.put("aralRainfallThreshold", s3);

            Label l4 = new Label(I18n.getOrDefault("scenario.label.dam_min_elevation", "Altitude Min Barrage (m):"));
            l4.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            Spinner<Double> s4 = new Spinner<>(50.0, 2000.0, 300.0, 50.0);
            s4.setEditable(true);
            s4.setPrefWidth(90);
            s4.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s4, 300.0, () -> s4.getValueFactory().setValue(300.0));
            grid.add(l4, 2, 1);
            grid.add(s4, 3, 1);
            spinners.put("damMinElevation", s4);

        } else if ("FertileCrescentSalinizationEngine".equals(engineKey)) {
            Label l1 = new Label(I18n.getOrDefault("scenario.label.salinization_rate", "Taux Salinisation Sol (k_salt):"));
            l1.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            Spinner<Double> s1 = new Spinner<>(0.001, 0.20, 0.02, 0.005);
            s1.setEditable(true);
            s1.setPrefWidth(90);
            s1.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s1, 0.02, () -> s1.getValueFactory().setValue(0.02));
            grid.add(l1, 0, 0);
            grid.add(s1, 1, 0);
            spinners.put("salinizationRate", s1);

        } else if ("LandReclamationEngine".equals(engineKey)) {
            Label l1 = new Label(I18n.getOrDefault("scenario.label.polder_tech", "Polderization Tech Threshold:"));
            l1.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            Spinner<Double> s1 = new Spinner<>(1.0, 10.0, 4.0, 0.5);
            s1.setEditable(true);
            s1.setPrefWidth(90);
            s1.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s1, 4.0, () -> s1.getValueFactory().setValue(4.0));
            grid.add(l1, 0, 0);
            grid.add(s1, 1, 0);
            spinners.put("polderTechThreshold", s1);

            Label l2 = new Label(I18n.getOrDefault("scenario.label.polder_capital", "Polderization Capital Threshold:"));
            l2.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            Spinner<Double> s2 = new Spinner<>(10.0, 1000.0, 100.0, 10.0);
            s2.setEditable(true);
            s2.setPrefWidth(90);
            s2.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s2, 100.0, () -> s2.getValueFactory().setValue(100.0));
            grid.add(l2, 2, 0);
            grid.add(s2, 3, 0);
            spinners.put("polderCapitalThreshold", s2);
        }

        if (!spinners.isEmpty()) {
            typeBParamSpinnersMap.put(engineKey, spinners);
            return grid;
        }
        return null;
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
    public static class ClimateEvent {
        private final StringProperty type;
        private final StringProperty name;
        private final IntegerProperty year;
        private final DoubleProperty latitude;
        private final DoubleProperty longitude;
        private final DoubleProperty depth;
        private final DoubleProperty magnitude;

        /**
         * Climate event.
         *
         * @param type the type parameter (String)
         * @param name the name parameter (String)
         * @param year the year parameter (int)
         * @param lat the lat parameter (double)
         * @param lon the lon parameter (double)
         * @param depth the depth parameter (double)
         * @param mag the mag parameter (double)
         * @return the resulting computation or state reference
         */
        public ClimateEvent(String type, String name, int year, double lat, double lon, double depth, double mag) {
            this.type = new SimpleStringProperty(type);
            this.name = new SimpleStringProperty(name);
            this.year = new SimpleIntegerProperty(year);
            this.latitude = new SimpleDoubleProperty(lat);
            this.longitude = new SimpleDoubleProperty(lon);
            this.depth = new SimpleDoubleProperty(depth);
            this.magnitude = new SimpleDoubleProperty(mag);
        }

        /**
         * Type property.
         *
         * @return the resulting computation or state reference
         */
        public StringProperty typeProperty() { return type; }
        /**
         * Name property.
         *
         * @return the resulting computation or state reference
         */
        public StringProperty nameProperty() { return name; }
        /**
         * Year property.
         *
         * @return the resulting computation or state reference
         */
        public IntegerProperty yearProperty() { return year; }
        /**
         * Latitude property.
         *
         * @return the resulting computation or state reference
         */
        public DoubleProperty latitudeProperty() { return latitude; }
        /**
         * Longitude property.
         *
         * @return the resulting computation or state reference
         */
        public DoubleProperty longitudeProperty() { return longitude; }
        /**
         * Depth property.
         *
         * @return the resulting computation or state reference
         */
        public DoubleProperty depthProperty() { return depth; }
        /**
         * Magnitude property.
         *
         * @return the resulting computation or state reference
         */
        public DoubleProperty magnitudeProperty() { return magnitude; }
        /**
         * Get type.
         *
         * @return the resulting computation or state reference
         */
        public String getType() { return type.get(); }
        /**
         * Set type.
         *
         * @param v the v parameter (String)
         */
        public void setType(String v) { type.set(v); }
        /**
         * Get name.
         *
         * @return the resulting computation or state reference
         */
        public String getName() { return name.get(); }
        /**
         * Set name.
         *
         * @param v the v parameter (String)
         */
        public void setName(String v) { name.set(v); }
        /**
         * Get year.
         *
         * @return the resulting computation or state reference
         */
        public int getYear() { return year.get(); }
        /**
         * Set year.
         *
         * @param v the v parameter (int)
         */
        public void setYear(int v) { year.set(v); }
        /**
         * Get latitude.
         *
         * @return the resulting computation or state reference
         */
        public double getLatitude() { return latitude.get(); }
        /**
         * Set latitude.
         *
         * @param v the v parameter (double)
         */
        public void setLatitude(double v) { latitude.set(v); }
        /**
         * Get longitude.
         *
         * @return the resulting computation or state reference
         */
        public double getLongitude() { return longitude.get(); }
        /**
         * Set longitude.
         *
         * @param v the v parameter (double)
         */
        public void setLongitude(double v) { longitude.set(v); }
        /**
         * Get depth.
         *
         * @return the resulting computation or state reference
         */
        public double getDepth() { return depth.get(); }
        /**
         * Set depth.
         *
         * @param v the v parameter (double)
         */
        public void setDepth(double v) { depth.set(v); }
        /**
         * Get magnitude.
         *
         * @return the resulting computation or state reference
         */
        public double getMagnitude() { return magnitude.get(); }
        /**
         * Set magnitude.
         *
         * @param v the v parameter (double)
         */
        public void setMagnitude(double v) { magnitude.set(v); }
    }

    /**
     * Scenario setup panel.
     *
     * @param onStartSimulation the on start simulation parameter (Consumer&lt;Scenario&gt;)
     */
    public ScenarioSetupPanel(Consumer<Scenario> onStartSimulation) {
        this.onStartSimulation = onStartSimulation;
        this.scenarioRepo = new org.ether.society.persistence.ScenarioRepository();
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
            boolean presetChanged = false;
            if (planetPreset != null) {
                if (!planetPreset.equals(this.activePlanetPreset)) {
                    this.activePlanetPreset = planetPreset;
                    presetChanged = true;
                }
            } else {
                EcologyPreset activeEco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null;
                if (activeEco != null) {
                    PlanetPreset found = findPlanetPresetByName(activeEco.planetPresetName());
                    if (!Objects.equals(found, this.activePlanetPreset)) {
                        this.activePlanetPreset = found;
                        presetChanged = true;
                    }
                }
            }
            if (this.activePlanetPreset == null) {
                this.activePlanetPreset = PlanetPreset.EARTH_LIKE;
                presetChanged = true;
            }

            if (planetPresetCombo != null) {
                planetPresetCombo.setValue(this.activePlanetPreset);
            }
            if (ecologyName != null && ecologyPresetCombo != null) {
                for (EcologyPreset eco : ecologyPresetCombo.getItems()) {
                    if (eco.name().equalsIgnoreCase(ecologyName)) {
                        ecologyPresetCombo.setValue(eco);
                        break;
                    }
                }
            }
            updateInheritedContextDisplay(ecologyName);
            if (presetChanged || currentPreviewCells == null || currentPreviewCells.isEmpty()) {
                generatePreview();
            } else {
                drawPreview();
            }
        } finally {
            isUpdatingFromPreset = oldUpdating;
        }
    }

    /**
     * Find planet preset by name.
     *
     * @param name the name parameter (String)
     * @return the resulting computation or state reference
     */
    public PlanetPreset findPlanetPresetByName(String name) {
        if (name == null || name.isBlank()) return PlanetPreset.EARTH_LIKE;
        for (PlanetPreset p : PlanetPreset.getPresets()) {
            if (p.name().equalsIgnoreCase(name)) return p;
        }
        String lower = name.toLowerCase();
        if (lower.contains("-100") || lower.contains("lig") || lower.contains("interglaciaire") || lower.contains("eemian")) return PlanetPreset.EARTH_LIG_100000BP;
        if (lower.contains("-50") || lower.contains("sahul") || lower.contains("mis3") || lower.contains("mis 3")) return PlanetPreset.EARTH_MIS3_50000BP;
        if (lower.contains("-25") || lower.contains("beringia") || lower.contains("béringie")) return PlanetPreset.EARTH_LGM_ONSET_25000BP;
        if (lower.contains("-20") || lower.contains("lgm") || lower.contains("glaciaire") || lower.contains("solutrean") || lower.contains("solutréen")) return PlanetPreset.EARTH_LGM_20000BP;
        if (lower.contains("-10") || lower.contains("eh") || lower.contains("précoce") || lower.contains("early holocene") || lower.contains("dryas")) return PlanetPreset.EARTH_EH_10000BP;
        if (lower.contains("-6") || lower.contains("mh") || lower.contains("sahara") || lower.contains("mid holocene")) return PlanetPreset.EARTH_MH_6000BP;
        if (lower.contains("-3") || lower.contains("lh") || lower.contains("tardif") || lower.contains("late holocene")) return PlanetPreset.EARTH_LH_3000BP;
        if (lower.contains("-1900") || lower.contains("bronze")) return PlanetPreset.EARTH_BRONZE_1900BP;
        if (lower.contains("-1000") || lower.contains("iron") || lower.contains("fer")) return PlanetPreset.EARTH_IRON_1000BP;
        for (PlanetPreset p : PlanetPreset.getPresets()) {
            if (p.name().toLowerCase().contains(lower) || lower.contains(p.name().toLowerCase())) {
                return p;
            }
        }
        if (lower.contains("earth") || lower.contains("terre") || lower.contains("terran")) {
            return PlanetPreset.EARTH_LIKE;
        }
        return PlanetPreset.EARTH_LIKE;
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

        // --- Inherited Presets (Tab 1 Planet + Tab 2 Ecology) Header ---
        planetSectionHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.section.inherited", "ðŸª INHERITED CONTEXT (TABS 1 & 2)"));

        planetPresetCombo = new ComboBox<>();
        planetPresetCombo.getItems().setAll(PlanetPreset.getPresets());
        planetPresetCombo.setCellFactory(p -> new ListCell<PlanetPreset>() {
            /**
             * Update item.
             *
             * @param item the item parameter (PlanetPreset)
             * @param empty the empty parameter (boolean)
             */
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
                // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(org.ether.society.i18n.I18n.getPlanetPresetDisplayName(item.name()));
                }
            }
        });
        planetPresetCombo.setButtonCell(new ListCell<PlanetPreset>() {
            /**
             * Update item.
             *
             * @param item the item parameter (PlanetPreset)
             * @param empty the empty parameter (boolean)
             */
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
                // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
                super.updateItem(item, empty);
                if (empty || item == null) {
                    PlanetPreset current = planetPresetCombo != null ? planetPresetCombo.getValue() : null;
                    setText(current != null ? org.ether.society.i18n.I18n.getPlanetPresetDisplayName(current.name()) : org.ether.society.i18n.I18n.getPlanetPresetDisplayName("Terre (Terran)"));
                } else {
                    setText(org.ether.society.i18n.I18n.getPlanetPresetDisplayName(item.name()));
                }
            }
        });
        planetPresetCombo.setValue(PlanetPreset.EARTH_LIKE);
        planetPresetCombo.setMaxWidth(Double.MAX_VALUE);
        planetPresetCombo.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-opacity: 1.0; -fx-border-color: rgba(56, 189, 248, 0.4); -fx-border-radius: 4px;");
        planetPresetCombo.setConverter(new javafx.util.StringConverter<PlanetPreset>() {
            /**
             * To string.
             *
             * @param item the item parameter (PlanetPreset)
             * @return the resulting computation or state reference
             */
            @Override
            public String toString(PlanetPreset item) {
                return item == null ? org.ether.society.i18n.I18n.getPlanetPresetDisplayName("Terre (Terran)") : org.ether.society.i18n.I18n.getPlanetPresetDisplayName(item.name());
            }
            /**
             * From string.
             *
             * @param string the string parameter (String)
             * @return the resulting computation or state reference
             */
            @Override
            public PlanetPreset fromString(String string) {
                return null;
            }
        });
        planetPresetCombo.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.planet_preset_disabled", "Inherited planetary preset automatically derived from chosen ecology (Tab 2).")));
        planetPresetCombo.setDisable(true);

        ecologyPresetCombo = new ComboBox<>();
        ecologyPresetCombo.getItems().setAll(EcologyPreset.getBuiltInPresets());
        ecologyPresetCombo.setValue(EcologyPreset.getBuiltInPresets().get(0));
        ecologyPresetCombo.setMaxWidth(Double.MAX_VALUE);
        ecologyPresetCombo.setConverter(new javafx.util.StringConverter<EcologyPreset>() {
            /**
             * To string.
             *
             * @param item the item parameter (EcologyPreset)
             * @return the resulting computation or state reference
             */
            @Override
            public String toString(EcologyPreset item) {
                return item == null ? "" : org.ether.society.i18n.I18n.getPlanetPresetDisplayName(item.name());
            }
            /**
             * From string.
             *
             * @param string the string parameter (String)
             * @return the resulting computation or state reference
             */
            @Override
            public EcologyPreset fromString(String string) {
                return null;
            }
        });
        ecologyPresetCombo.setCellFactory(p -> new ListCell<EcologyPreset>() {
            /**
             * Update item.
             *
             * @param item the item parameter (EcologyPreset)
             * @param empty the empty parameter (boolean)
             */
            @Override
            protected void updateItem(EcologyPreset item, boolean empty) {
                // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(org.ether.society.i18n.I18n.getPlanetPresetDisplayName(item.name()));
                    String desc = item.getPresetDescription();
                    if (desc != null && !desc.isBlank()) {
                        Tooltip tip = new Tooltip(desc);
                        tip.setWrapText(true);
                        tip.setMaxWidth(450);
                        setTooltip(tip);
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });
        ecologyPresetCombo.setButtonCell(new ListCell<EcologyPreset>() {
            /**
             * Update item.
             *
             * @param item the item parameter (EcologyPreset)
             * @param empty the empty parameter (boolean)
             */
            @Override
            protected void updateItem(EcologyPreset item, boolean empty) {
                // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
                super.updateItem(item, empty);
                if (empty || item == null) {
                    EcologyPreset current = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null;
                    setText(current != null ? org.ether.society.i18n.I18n.getPlanetPresetDisplayName(current.name()) : "");
                } else {
                    setText(org.ether.society.i18n.I18n.getPlanetPresetDisplayName(item.name()));
                }
            }
        });
        ecologyPresetCombo.setOnAction(e -> {
            EcologyPreset selected = ecologyPresetCombo.getValue();
            if (selected != null) {
                PlanetPreset cascadedPlanet = findPlanetPresetByName(selected.planetPresetName());
                if (cascadedPlanet != null) {
                    this.activePlanetPreset = cascadedPlanet;
                    planetPresetCombo.setValue(cascadedPlanet);
                }
                updateInheritedContextDisplay(selected.name());
                notifyParamChange();
            }
        });

        inheritedContextLabel = new Label(I18n.getOrDefault("scenario.info.earth_derived", "ðŸŒ¿ Ecology: Earth Standard Baseline  âž”  ðŸª Planet (derived): Earth (Terran)"));
        inheritedContextLabel.setWrapText(true);
        inheritedContextLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #0284c7; -fx-padding: 6 10; -fx-background-color: rgba(56, 189, 248, 0.12); -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 6;");

        ecoPromptLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.label.ecology_preset", "1. Ecological Preset (Tab 2):"));
        planetPromptLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.label.planet_preset", "2. Planetary Preset (Tab 1 — Cascaded from Ecology):"));

        VBox inheritedSection = createSection(planetSectionHeader, new VBox(8,
                ecoPromptLabel,
                ecologyPresetCombo,
                planetPromptLabel,
                planetPresetCombo,
                inheritedContextLabel
        ));

        // --- Scenario General Info Section ---
        title1 = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.section.spatiotemporal", "ðŸŒ EPOCH & SPATIOTEMPORAL DEFINITION"));
        GridPane grid1 = new GridPane();
        grid1.setHgap(10);
        grid1.setVgap(10);

        startYearSpinner = new Spinner<>(-100000, 5000, -8000, 100);
        startYearSpinner.setEditable(true);
        startYearSpinner.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.start_year", "Chronological marker at T=0 to set the simulation on a standard calendar reference. This value is for reference and does not directly affect physics equations.")));

        endYearSpinner = new Spinner<>(-100000, 5000, 100, 100);
        endYearSpinner.setEditable(true);
        endYearSpinner.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.end_year", "Target end year of simulation. Determines total campaign duration for headless runs and comparative analytics.")));
        endYearSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        startYearLabel = new Label();
        endYearLabel = new Label();
        h3ResolutionLabel = new Label();

        // H3 Resolution (Row 0)
        h3ResolutionCombo = new ComboBox<>();
        h3ResolutionCombo.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8);
        h3ResolutionCombo.setValue(3);
        h3ResolutionCombo.setMaxWidth(Double.MAX_VALUE);
        h3ResolutionCombo.setConverter(new javafx.util.StringConverter<Integer>() {
            /**
             * To string.
             *
             * @param item the item parameter (Integer)
             * @return the resulting computation or state reference
             */
            @Override
            public String toString(Integer item) {
                return item == null ? "" : org.ether.society.i18n.I18n.getOrDefault("planet.param.resolution.res" + item, "Résolution " + item);
            }
            /**
             * From string.
             *
             * @param string the string parameter (String)
             * @return the resulting computation or state reference
             */
            @Override
            public Integer fromString(String string) {
                return null;
            }
        });
        Tooltip.install(h3ResolutionCombo, new Tooltip(org.ether.society.i18n.I18n.getOrDefault("planet.tooltip.resolution", "H3 Grid Resolution")));
        Tooltip.install(h3ResolutionLabel, h3ResolutionCombo.getTooltip());
        h3ResolutionHintLabel = new Label(I18n.getOrDefault("scenario.h3.hint",
                "• Res 2–3 : Planétaire global multi-millénaire (~41k cel.) | Res 4–5 : Régional (~2.8M cel.) | Res 6+ : Micro-bassin."));
        h3ResolutionHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        h3ResolutionHintLabel.setWrapText(true);
        VBox h3Box = new VBox(2, h3ResolutionCombo, h3ResolutionHintLabel);

        // Target Cohort Size (Row 1)
        cohortSizeLabel = new Label();
        targetCohortSizeSpinner = new Spinner<>(1, 100000, 150, 25);
        targetCohortSizeSpinner.setEditable(true);
        targetCohortSizeSpinner.setMaxWidth(Double.MAX_VALUE);
        targetCohortSizeSpinner.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.cohort_size", 
                "👥 Taille Cible des Cohortes Démographiques (Nœuds Agents DOD) [hab/cohorte] :\n" +
                "Détermine la taille moyenne des groupes d'habitants représentés par un même nœud agent.\n" +
                "• 1 à 30 hab/cohorte : Bandes nomades & chasseurs-cueilleurs\n" +
                "• 150 hab/cohorte : Nombre de Dunbar (Villages sédentaires & communautés de base - Recommandé)\n" +
                "• 10 000 à 50 000 hab/cohorte : Macro-cohortes, métropoles & calcul accéléré")));
        Tooltip.install(cohortSizeLabel, targetCohortSizeSpinner.getTooltip());
        targetCohortSizeSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        cohortSizeHintLabel = new Label(I18n.getOrDefault("scenario.cohort.hint",
                "• 1–30 : Micro-tribus nomades | 150 : Recommandé par défaut (Nombre de Dunbar) | 10k–50k : Macro-cohortes."));
        cohortSizeHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        cohortSizeHintLabel.setWrapText(true);
        VBox cohortBox = new VBox(2, targetCohortSizeSpinner, cohortSizeHintLabel);

        // Temporal Resolution (Row 2)
        temporalResolutionLabel = new Label();
        temporalResolutionCombo = new ComboBox<>();
        temporalResolutionCombo.getItems().addAll(1.0, 7.0, 15.0, 30.0, 60.0, 90.0, 180.0, 365.0);
        temporalResolutionCombo.setValue(30.0);
        temporalResolutionCombo.setMaxWidth(Double.MAX_VALUE);
        temporalResolutionCombo.setConverter(new javafx.util.StringConverter<Double>() {
            /**
             * To string.
             *
             * @param item the item parameter (Double)
             * @return the resulting computation or state reference
             */
            @Override
            public String toString(Double item) {
                if (item == null) return "";
                if (item == 1.0) return org.ether.society.i18n.I18n.getOrDefault("scenario.temporal.1d", "1 day (High Precision Seasons & Epidemics)");
                if (item == 7.0) return org.ether.society.i18n.I18n.getOrDefault("scenario.temporal.7d", "1 semaine (7 jours)");
                if (item == 15.0) return org.ether.society.i18n.I18n.getOrDefault("scenario.temporal.15d", "15 jours");
                if (item == 30.0) return org.ether.society.i18n.I18n.getOrDefault("scenario.temporal.30d", "1 month (~30 days) [Default - Balanced]");
                if (item == 60.0) return org.ether.society.i18n.I18n.getOrDefault("scenario.temporal.60d", "2 mois");
                if (item == 90.0) return org.ether.society.i18n.I18n.getOrDefault("scenario.temporal.90d", "1 trimestre (~3 mois)");
                if (item == 180.0) return org.ether.society.i18n.I18n.getOrDefault("scenario.temporal.180d", "1 semestre (~6 mois)");
                if (item == 365.0) return org.ether.society.i18n.I18n.getOrDefault("scenario.temporal.365d", "1 year (365 days) [Ultra-Fast Multi-Millennia]");
                return String.format(java.util.Locale.US, "%.0f %s", item, org.ether.society.i18n.I18n.getOrDefault("scenario.temporal.days", "jours"));
            }
            /**
             * From string.
             *
             * @param string the string parameter (String)
             * @return the resulting computation or state reference
             */
            @Override
            public Double fromString(String string) {
                return null;
            }
        });
        Tooltip temporalTooltip = new Tooltip("""
            ⏱️ Résolution Temporelle de la Simulation (Pas de Temps Δt) :
            Détermine la granularité temporelle de chaque pas de calcul de la simulation.
            • Pas de temps court (< 1 mois, ex: 1 jour, 1 semaine) : Haute précision dynamique pour les épidémies, le climat saisonnier et la mobilité rapide.
            • Pas de temps standard (1 mois - par défaut) : Équilibre optimal entre la précision physique et la vitesse d'exécution.
            • Pas de temps long (> 1 mois, ex: 3 mois, 1 an) : Accélération majeure pour les simulations à très grande échelle sur plusieurs millénaires.
            """);
        temporalResolutionCombo.setTooltip(temporalTooltip);
        Tooltip.install(temporalResolutionLabel, temporalTooltip);
        temporalResolutionCombo.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        temporalResolutionHintLabel = new Label(I18n.getOrDefault("scenario.temporal.hint",
                "• 1 Mois : Recommandé par défaut (équilibre saisons/vitesse) | 1 Jour/Sem. : Épidémies & météo | 1 An : Multi-millénaire ultra-rapide."));
        temporalResolutionHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        temporalResolutionHintLabel.setWrapText(true);
        VBox tempBox = new VBox(2, temporalResolutionCombo, temporalResolutionHintLabel);

        // Start Year (Row 3)
        startYearSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            notifyParamChange();
            if (newV != null) {
                loadEarthHistoricalEvents();
            }
        });
        Tooltip.install(startYearLabel, startYearSpinner.getTooltip());
        Tooltip.install(endYearLabel, endYearSpinner.getTooltip());

        // Add to grid1: Row 0 = H3 Res, Row 1 = Pas de temps, Row 2 = Cohort Size, Row 3 = Start Year, Row 4 = End Year
        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(190);
        col0.setPrefWidth(220);
        col0.setHgrow(Priority.NEVER);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        col1.setFillWidth(true);
        grid1.getColumnConstraints().addAll(col0, col1);

        h3ResolutionLabel.setWrapText(true);
        temporalResolutionLabel.setWrapText(true);
        cohortSizeLabel.setWrapText(true);
        startYearLabel.setWrapText(true);
        endYearLabel.setWrapText(true);

        grid1.addRow(0, h3ResolutionLabel, h3Box);
        grid1.addRow(1, temporalResolutionLabel, tempBox);
        grid1.addRow(2, cohortSizeLabel, cohortBox);
        grid1.addRow(3, startYearLabel, startYearSpinner);
        grid1.addRow(4, endYearLabel, endYearSpinner);

        VBox section1Content = new VBox(10, grid1);
        VBox section1 = createSection(title1, section1Content);

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
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        bundleHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.bundle.header", "📦 7. UNIFIED BUNDLE MULTI-SCENARIO IMPORT/EXPORT (.ETHER)"));
        bundleHeader.getStyleClass().add("label-section-header");

        bundleSubtitle = new Label(I18n.getOrDefault("scenario.desc.bundle", "Export or import the complete scenario (planetary context, ecology, active engines, cultural layers, and demographic grid) in unified .ether format for archiving or sharing."));
        bundleSubtitle.setWrapText(true);
        bundleSubtitle.getStyleClass().add("card-description-muted");

        btnExportBundle = new Button(I18n.getOrDefault("scenario.btn.export_bundle", "📦 Export Bundle (.ether)"));
        btnExportBundle.getStyleClass().add("button-secondary");
        btnExportBundle.setMaxWidth(Double.MAX_VALUE);
        btnExportBundle.getStyleClass().add("button-accent-blue");
        btnExportBundle.setOnAction(e -> exportUnifiedBundle());
        btnExportBundle.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_bundle", "Export complete scenario (physics, ecology, engines, layers, and demographics) to a unified .ether bundle file.")));

        btnImportBundle = new Button(I18n.getOrDefault("scenario.btn.import_bundle", "📂 Import Bundle (.ether)"));
        btnImportBundle.getStyleClass().add("button-secondary");
        btnImportBundle.setMaxWidth(Double.MAX_VALUE);
        btnImportBundle.setOnAction(e -> importUnifiedBundle());
        btnImportBundle.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.import_bundle", "Import and apply a unified .ether bundle file to instantly restore full scenario state.")));

        HBox bundleBox = new HBox(8, btnExportBundle, btnImportBundle);
        HBox.setHgrow(btnExportBundle, Priority.ALWAYS);
        HBox.setHgrow(btnImportBundle, Priority.ALWAYS);

        section.getChildren().addAll(bundleHeader, bundleSubtitle, bundleBox);
        return section;
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


    private void exportUnifiedBundle() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("scenario.title.export_bundle_dialog", "Export Unified Scenario Bundle (.ether)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Ether (*.ether, *.json)", "*.ether", "*.json"));
        chooser.setInitialFileName("mon-scenario-ether.ether");
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                PlanetPreset planet = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
                if (planet == null) planet = PlanetPreset.EARTH_LIKE;
                EcologyPreset eco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : EcologyPreset.EARTH_STANDARD;
                Scenario scenario = getScenario();

                org.ether.society.model.EtherScenarioBundle rawBundle =
                        new org.ether.society.model.EtherScenarioBundle("1.0.0-beta.2", planet, eco, scenario);
                org.ether.society.model.EtherScenarioBundle bundle = 
                        org.ether.society.security.EtherBundleSigner.signBundle(rawBundle, "Ether Lead Planner");

                ObjectMapper mapper = org.ether.society.persistence.EtherJsonUtil.getMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(file, bundle);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(I18n.getOrDefault("scenario.title.export_success", "Export Successful"));
                alert.setHeaderText(I18n.getOrDefault("scenario.header.export_success", "Simulation Bundle Exported Successfully"));
                alert.setContentText(I18n.getOrDefault("scenario.alert.export_bundle_desc", "Unified simulation bundle signed & saved to:\n") + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception ex) {
                logger.error("Failed to export unified bundle", ex);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18n.getOrDefault("scenario.title.export_error", "Export Error"));
                alert.setContentText(I18n.getOrDefault("scenario.alert.export_failed", "Cannot save bundle: ") + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void importUnifiedBundle() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("scenario.title.import_bundle_dialog", "Import Unified Scenario Bundle (.ether)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ether Files (*.ether, *.json)", "*.ether", "*.json"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                ObjectMapper mapper = org.ether.society.persistence.EtherJsonUtil.getMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                org.ether.society.model.EtherScenarioBundle bundle =
                        mapper.readValue(file, org.ether.society.model.EtherScenarioBundle.class);

                if (bundle != null) {
                    boolean isValid = org.ether.society.security.EtherBundleSigner.verifyBundle(bundle);
                    if (!isValid) {
                        if (validationErrorLabel != null && validationErrorBanner != null) {
                            validationErrorLabel.setText("⚠️ï¸ Avertissement d'intégrité : Le bundle importé a été modifié ou sa signature ne concorde pas.");
                            validationErrorBanner.setVisible(true);
                            validationErrorBanner.setManaged(true);
                        }
                    }

                    if (bundle.planetPreset() != null) {
                        this.activePlanetPreset = bundle.planetPreset();
                        if (planetPresetCombo != null) {
                            if (!planetPresetCombo.getItems().contains(bundle.planetPreset())) {
                                planetPresetCombo.getItems().add(bundle.planetPreset());
                            }
                            planetPresetCombo.setValue(bundle.planetPreset());
                        }
                    }
                    if (bundle.ecologyPreset() != null && ecologyPresetCombo != null) {
                        if (!ecologyPresetCombo.getItems().contains(bundle.ecologyPreset())) {
                            ecologyPresetCombo.getItems().add(bundle.ecologyPreset());
                        }
                        ecologyPresetCombo.setValue(bundle.ecologyPreset());
                    }
                    if (bundle.scenario() != null) {
                        applyScenarioToUI(bundle.scenario());
                    }
                    if (onScenarioLoadedCallback != null) {
                        onScenarioLoadedCallback.accept(this.activePlanetPreset, bundle.ecologyPreset() != null ? bundle.ecologyPreset() : EcologyPreset.EARTH_STANDARD);
                    }
                    generatePreview();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle(I18n.getOrDefault("scenario.title.import_success", "Import Successful"));
                    alert.setHeaderText(I18n.getOrDefault("scenario.header.import_success", "Unified Bundle Applied"));
                    alert.setContentText(I18n.getOrDefault("scenario.alert.import_bundle_success", "Physical, ecological, and demographic parameters loaded successfully from bundle."));
                    alert.showAndWait();
                }
            } catch (Exception ex) {
                logger.error("Failed to import unified bundle", ex);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18n.getOrDefault("scenario.title.import_error", "Import Error"));
                alert.setContentText(I18n.getOrDefault("scenario.alert.import_bundle_invalid", "Invalid bundle file: ") + ex.getMessage());
                alert.showAndWait();
            }
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

            if (startYearSpinner != null && startYearSpinner.getValueFactory() != null) {
                startYearSpinner.getValueFactory().setValue((int) s.getStartDateYear());
            }
            if (endYearSpinner != null && endYearSpinner.getValueFactory() != null) {
                endYearSpinner.getValueFactory().setValue((int) s.getEndDateYear());
            }
            if (targetCohortSizeSpinner != null && targetCohortSizeSpinner.getValueFactory() != null) {
                targetCohortSizeSpinner.getValueFactory().setValue(s.getTargetCohortSize() > 0 ? s.getTargetCohortSize() : 150);
            }
            if (temporalResolutionCombo != null) {
                temporalResolutionCombo.setValue(s.getTemporalResolutionDays() > 0 ? s.getTemporalResolutionDays() : 30.0);
            }
            if (h3ResolutionCombo != null) {
                h3ResolutionCombo.setValue(s.getH3Resolution() > 0 ? s.getH3Resolution() : 3);
            }
            if (initialHumanCountSpinner != null && initialHumanCountSpinner.getValueFactory() != null) {
                initialHumanCountSpinner.getValueFactory().setValue(s.getInitialHumanCount());
            }

            if (s.getPopulationDensityType() != null && densityPatternCombo != null && densityPatternCombo.getItems().contains(s.getPopulationDensityType())) {
                densityPatternCombo.setValue(s.getPopulationDensityType());
            }
            if (techPresetCombo != null && s.getTechPreset() != null) {
                techPresetCombo.setValue(s.getTechPreset());
            }
            if (customCapitalSpinner != null && customCapitalSpinner.getValueFactory() != null) {
                customCapitalSpinner.getValueFactory().setValue(s.getInitialCapitalPerCapita());
            }
            if (customEnergySpinner != null && customEnergySpinner.getValueFactory() != null) {
                customEnergySpinner.getValueFactory().setValue(s.getInitialEnergyPerCapita());
            }
            if (customFoodSpinner != null && customFoodSpinner.getValueFactory() != null) {
                customFoodSpinner.getValueFactory().setValue(s.getInitialFoodReserveMonths());
            }
            if (customInfoSpinner != null && customInfoSpinner.getValueFactory() != null) {
                customInfoSpinner.getValueFactory().setValue(s.getInitialInformationPerCapita());
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
            if (clippingCheckBox != null) {
                boolean active = s.isClippingEnabled();
                clippingCheckBox.setSelected(active);
                if (minLatSpinner != null && minLatSpinner.getValueFactory() != null) minLatSpinner.getValueFactory().setValue(s.getMinLat());
                if (maxLatSpinner != null && maxLatSpinner.getValueFactory() != null) maxLatSpinner.getValueFactory().setValue(s.getMaxLat());
                if (minLngSpinner != null && minLngSpinner.getValueFactory() != null) minLngSpinner.getValueFactory().setValue(s.getMinLng());
                if (maxLngSpinner != null && maxLngSpinner.getValueFactory() != null) maxLngSpinner.getValueFactory().setValue(s.getMaxLng());
                if (s.getBoundaryMode() != null && boundaryModeCombo != null) boundaryModeCombo.setValue(s.getBoundaryMode());
            }
            if (strictDeterminismCheckBox != null) {
                strictDeterminismCheckBox.setSelected(s.isStrictDeterminism());
            }
            if (sparseCellSkippingCheckBox != null) {
                sparseCellSkippingCheckBox.setSelected(s.isSparseCellSkippingEnabled());
            }
            if (oceanMacroAggregationCheckBox != null) {
                oceanMacroAggregationCheckBox.setSelected(s.isOceanMacroAggregationEnabled());
            }
            if (coastalNavigationOnlyCheckBox != null) {
                coastalNavigationOnlyCheckBox.setSelected(s.isCoastalNavigationOnlyEnabled());
            }
            if (oceanMultiRateTickingCheckBox != null) {
                oceanMultiRateTickingCheckBox.setSelected(s.isOceanMultiRateTickingEnabled());
            }
            if (climateTickFreqSlider != null) {
                climateTickFreqSlider.setValue(s.getClimateTickFrequency());
            }
            if (parallelExecutionCheckBox != null) {
                parallelExecutionCheckBox.setSelected(s.isParallelExecutionEnabled());
            }
            if (threadCountSlider != null) {
                threadCountSlider.setValue(s.getParallelThreadCount());
            }
            if (spatialRangeTruncationCheckBox != null) {
                spatialRangeTruncationCheckBox.setSelected(s.isSpatialRangeTruncationEnabled());
            }

            // Restore Type B engine checkbox states
            java.util.Map<String, Boolean> typeBStates = s.getTypeBEngineStates();
            // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
            for (java.util.Map.Entry<String, CheckBox> entry : typeBCheckBoxMap.entrySet()) {
                boolean active = typeBStates != null && typeBStates.getOrDefault(entry.getKey(), false);
                entry.getValue().setSelected(active);
            }

            // Restore Type B engine fine-grained parameter values
            java.util.Map<String, java.util.Map<String, Double>> typeBParams = s.getTypeBEngineParameters();
            if (typeBParams != null) {
                // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
                for (java.util.Map.Entry<String, java.util.Map<String, Spinner<Double>>> engEntry : typeBParamSpinnersMap.entrySet()) {
                    String engKey = engEntry.getKey();
                    java.util.Map<String, Double> savedEngParams = typeBParams.get(engKey);
                    if (savedEngParams != null) {
                        // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
                        for (java.util.Map.Entry<String, Spinner<Double>> paramEntry : engEntry.getValue().entrySet()) {
                            Double val = savedEngParams.get(paramEntry.getKey());
                            if (val != null && paramEntry.getValue() != null && paramEntry.getValue().getValueFactory() != null) {
                                paramEntry.getValue().getValueFactory().setValue(val);
                            }
                        }
                    }
                }
            }
            if (s.getCustomDensityBase64() != null && !s.getCustomDensityBase64().isBlank()) {
                customDensityImage = org.ether.society.data.ImageMapLoader.base64PngToImage(s.getCustomDensityBase64());
                String yrStr = s.getStartDateYear() != 0 ? (s.getStartDateYear() < 0 ? Math.abs(s.getStartDateYear()) + " BC" : s.getStartDateYear() + " AD") : "";
                if (densityMapFileLabel != null) densityMapFileLabel.setText(I18n.getOrDefault("scenario.preset.density_map", "📷 Carte de Densité Précalculée") + (yrStr.isEmpty() ? "" : " (" + yrStr + ")"));
                if (radioImportDemo != null) radioImportDemo.setSelected(true);
                updateDemoCompatibilityDisplay();
            } else {
                customDensityImage = null;
                if (densityMapFileLabel != null) densityMapFileLabel.setText(org.ether.society.i18n.I18n.get("planet.map.none"));
                if (radioProcDemo != null) radioProcDemo.setSelected(true);
                updateDemoCompatibilityDisplay();
            }

            // Auto-select the demographic reference source combo based on the scenario epoch/planet
            if (demoSourceCombo != null) {
                String autoSrc = pickDemoSourceForEpoch(s.getStartDateYear(), this.activePlanetPreset);
                selectBestSource(demoSourceCombo, autoSrc, "Terre");
            }


            if (s.getCultureVectorDimensions() > 0 && cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValueFactory() != null) {
                cultureVectorDimSpinner.getValueFactory().setValue(s.getCultureVectorDimensions());
            }
            culturalSectionBuilder.applyScenario(s);

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
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        clippingHeader = new Label(I18n.getOrDefault("scenario.clipping.header", "✂️ï¸ 4. BORDERS & HISTORICAL SPATIAL CLIPPING"));
        clippingHeader.getStyleClass().add("label-section-header");

        clippingCheckBox = new CheckBox(I18n.getOrDefault("scenario.clipping.enable", "Enable Partial Simulation (Truncated Zone)"));
        clippingCheckBox.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        clippingCheckBox.setOnAction(e -> {
            notifyParamChange();
            drawPreview();
        });

        graphicSelectBtn = new ToggleButton(I18n.getOrDefault("scenario.clipping.select_mode", "ðŸ–±ï¸ Graphic Map Selection Mode"));
        graphicSelectBtn.setMaxWidth(Double.MAX_VALUE);
        graphicSelectBtn.getStyleClass().add("button-secondary");
        graphicSelectBtn.setStyle("-fx-font-size: 12px;");
        graphicSelectBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.graphic_select", "Activate this button or hold SHIFT to draw a selection rectangle on preview map.")));

        GridPane boundsGrid = new GridPane();
        boundsGrid.setHgap(8);
        boundsGrid.setVgap(6);

        maxLatSpinner = new Spinner<>(-90.0, 90.0, 90.0, 1.0);
        minLatSpinner = new Spinner<>(-90.0, 90.0, -90.0, 1.0);
        minLngSpinner = new Spinner<>(-180.0, 180.0, -180.0, 1.0);
        maxLngSpinner = new Spinner<>(-180.0, 180.0, 180.0, 1.0);

        attachDefaultValueHandling(clippingCheckBox, false, () -> clippingCheckBox.setSelected(false));
        attachDefaultValueHandling(maxLatSpinner, 90.0, () -> maxLatSpinner.getValueFactory().setValue(90.0));
        attachDefaultValueHandling(minLatSpinner, -90.0, () -> minLatSpinner.getValueFactory().setValue(-90.0));
        attachDefaultValueHandling(minLngSpinner, -180.0, () -> minLngSpinner.getValueFactory().setValue(-180.0));
        attachDefaultValueHandling(maxLngSpinner, 180.0, () -> maxLngSpinner.getValueFactory().setValue(180.0));

        for (Spinner<Double> sp : List.of(maxLatSpinner, minLatSpinner, minLngSpinner, maxLngSpinner)) {
            sp.setEditable(true);
            sp.setPrefWidth(75);
            sp.valueProperty().addListener((obs, old, val) -> {
                notifyParamChange();
                drawPreview();
            });
        }

        latMaxLabel = new Label(I18n.getOrDefault("scenario.clipping.lat_max", "Lat Max (Haut) :"));
        latMinLabel = new Label(I18n.getOrDefault("scenario.clipping.lat_min", "Lat Min (Bas) :"));
        lngMinLabel = new Label(I18n.getOrDefault("scenario.clipping.lng_min", "Lng Min (Gau.) :"));
        lngMaxLabel = new Label(I18n.getOrDefault("scenario.clipping.lng_max", "Lng Max (Dro.) :"));
        for (Label lbl : List.of(latMaxLabel, latMinLabel, lngMinLabel, lngMaxLabel)) {
            lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        }
        boundsGrid.addRow(0, latMaxLabel, maxLatSpinner, latMinLabel, minLatSpinner);
        boundsGrid.addRow(1, lngMinLabel, minLngSpinner, lngMaxLabel, maxLngSpinner);

        boundaryLabel = new Label(I18n.getOrDefault("scenario.clipping.boundary_label", "Scientific Border Modeling:"));
        boundaryLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        boundaryModeCombo = new ComboBox<>();
        boundaryModeCombo.getItems().addAll(
            "DYNAMIC_RESERVOIR",
            "CLOSED_BARRIER",
            "PERIODIC_WRAP"
        );
        boundaryModeCombo.setValue("DYNAMIC_RESERVOIR");
        boundaryModeCombo.setMaxWidth(Double.MAX_VALUE);
        boundaryModeCombo.setOnAction(e -> notifyParamChange());
        attachDefaultValueHandling(boundaryModeCombo, "DYNAMIC_RESERVOIR", () -> boundaryModeCombo.setValue("DYNAMIC_RESERVOIR"));
        boundaryModeCombo.setConverter(new javafx.util.StringConverter<String>() {
            /**
             * To string.
             *
             * @param item the item parameter (String)
             * @return the resulting computation or state reference
             */
            @Override
            public String toString(String item) {
                if (item == null) return "";
                switch (item) {
                    case "DYNAMIC_RESERVOIR" -> { return I18n.getOrDefault("scenario.boundary.reservoir", "🌊 External Virtual Reservoir (Free Flow)"); }
                    case "CLOSED_BARRIER" -> { return I18n.getOrDefault("scenario.boundary.barrier", "🧱 Sealed Barrier / Isolated (Closed Edges)"); }
                    case "PERIODIC_WRAP" -> { return I18n.getOrDefault("scenario.boundary.wrap", "ðŸŒ Periodic Boundary (Toroidal)"); }
                    default -> { return item; }
                }
            }
            /**
             * From string.
             *
             * @param string the string parameter (String)
             * @return the resulting computation or state reference
             */
            @Override
            public String fromString(String string) {
                return null;
            }
        });

        resetClippingBtn = new Button(I18n.getOrDefault("scenario.clipping.reset", "🔄 Reset Area (Full Planet)"));
        resetClippingBtn.setMaxWidth(Double.MAX_VALUE);
        resetClippingBtn.getStyleClass().add("button-secondary");
        resetClippingBtn.setStyle("-fx-font-size: 12px;");

        clippingSubPanel = new VBox(10, graphicSelectBtn, boundsGrid, boundaryLabel, boundaryModeCombo, resetClippingBtn);
        clippingSubPanel.setStyle("-fx-padding: 10px; -fx-background-color: rgba(56, 189, 248, 0.04); -fx-background-radius: 6px; -fx-border-color: rgba(56, 189, 248, 0.2); -fx-border-radius: 6px; -fx-border-width: 1px;");
        clippingSubPanel.setVisible(false);
        clippingSubPanel.setManaged(false);

        resetClippingBtn.setOnAction(e -> {
            if (minLatSpinner != null && minLatSpinner.getValueFactory() != null) minLatSpinner.getValueFactory().setValue(-90.0);
            if (maxLatSpinner != null && maxLatSpinner.getValueFactory() != null) maxLatSpinner.getValueFactory().setValue(90.0);
            if (minLngSpinner != null && minLngSpinner.getValueFactory() != null) minLngSpinner.getValueFactory().setValue(-180.0);
            if (maxLngSpinner != null && maxLngSpinner.getValueFactory() != null) maxLngSpinner.getValueFactory().setValue(180.0);
            if (clippingCheckBox != null) clippingCheckBox.setSelected(false);
            if (graphicSelectBtn != null) graphicSelectBtn.setSelected(false);
            clippingSubPanel.setVisible(false);
            clippingSubPanel.setManaged(false);
            isSelectionDrag = false;
            currentPreviewCells = null;
            notifyParamChange();
            drawPreview();
        });

        clippingCheckBox.setOnAction(e -> {
            boolean active = clippingCheckBox.isSelected();
            clippingSubPanel.setVisible(active);
            clippingSubPanel.setManaged(active);
            if (!active) {
                if (minLatSpinner != null && minLatSpinner.getValueFactory() != null) minLatSpinner.getValueFactory().setValue(-90.0);
                if (maxLatSpinner != null && maxLatSpinner.getValueFactory() != null) maxLatSpinner.getValueFactory().setValue(90.0);
                if (minLngSpinner != null && minLngSpinner.getValueFactory() != null) minLngSpinner.getValueFactory().setValue(-180.0);
                if (maxLngSpinner != null && maxLngSpinner.getValueFactory() != null) maxLngSpinner.getValueFactory().setValue(180.0);
                if (graphicSelectBtn != null) graphicSelectBtn.setSelected(false);
                isSelectionDrag = false;
                currentPreviewCells = null;
            }
            notifyParamChange();
            drawPreview();
        });

        section.getChildren().addAll(clippingHeader, clippingCheckBox, clippingSubPanel);
        return section;
    }

    private void applySpatialClippingBounds(double minLat, double maxLat, double minLon, double maxLon) {
        if (minLatSpinner != null && minLatSpinner.getValueFactory() != null) minLatSpinner.getValueFactory().setValue(Math.round(minLat * 10.0) / 10.0);
        if (maxLatSpinner != null && maxLatSpinner.getValueFactory() != null) maxLatSpinner.getValueFactory().setValue(Math.round(maxLat * 10.0) / 10.0);
        if (minLngSpinner != null && minLngSpinner.getValueFactory() != null) minLngSpinner.getValueFactory().setValue(Math.round(minLon * 10.0) / 10.0);
        if (maxLngSpinner != null && maxLngSpinner.getValueFactory() != null) maxLngSpinner.getValueFactory().setValue(Math.round(maxLon * 10.0) / 10.0);
        if (clippingCheckBox != null) clippingCheckBox.setSelected(true);
        if (clippingSubPanel != null) {
            clippingSubPanel.setVisible(true);
            clippingSubPanel.setManaged(true);
        }
        if (graphicSelectBtn != null) graphicSelectBtn.setSelected(false);
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
        if (combo == null || combo.getItems() == null || combo.getItems().isEmpty()) return;
        if (target != null && combo.getItems().contains(target)) {
            combo.setValue(target);
            return;
        }
        if (target != null && !target.isBlank()) {
            String cleanTarget = target.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
            // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
            for (String it : combo.getItems()) {
                if (it != null && !it.isEmpty()) {
                    String cleanIt = it.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
                    if (cleanIt.contains(cleanTarget) || cleanTarget.contains(cleanIt)) {
                        combo.setValue(it);
                        return;
                    }
                }
            }
        }
        if (fallbackKeyword != null && !fallbackKeyword.isBlank()) {
            String kw = fallbackKeyword.toLowerCase();
            // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
            for (String it : combo.getItems()) {
                if (it != null && it.toLowerCase().contains(kw)) {
                    combo.setValue(it);
                    return;
                }
            }
        }
        for (String it : combo.getItems()) {
            if (it != null && !it.isEmpty()) {
                combo.setValue(it);
                return;
            }
        }
    }

    /**
     * Given the tensor index, scenario start year, and active planet preset, returns the best-matching
     * entry string for the cultural source combo at that tensor index, or null if the default should be kept.
     */
    private String pickCulturalSourceForEpoch(int tensorIdx, long startYear, org.ether.society.generation.PlanetPreset planet) {
        String pName = planet != null ? planet.name().toLowerCase() : "";
        if (pName.contains("mars") || pName.contains("ares")) {
            return switch (tensorIdx) {
                case 0 -> "🔴 Mars — Cartographie Linguistique Coloniale Martienne [Planétaire (Mars), +2060 AD à Futur]";
                case 1 -> "🔴 Mars — Structures de Parenté & Cohortes Pionnières [Planétaire (Mars), +2050 AD à Futur]";
                case 2 -> "🔴 Mars — Mythologie Martienne & Cultes de la Frontière [Planétaire (Mars), +2060 AD à Futur]";
                case 3 -> "🔴 Mars — Juridictions Consulaires & Traités Martiens [Planétaire (Mars), +2060 AD à Futur]";
                case 4 -> "🔴 Mars — Niveau Technologique Industriel & Robotique ISRU [Planétaire (Mars), +2050 AD à Futur]";
                case 5 -> "🔴 Mars — Réseau Ferroviaire Maglev Sub-Surface [Planétaire (Mars), +2070 AD à Futur]";
                case 6 -> "🔴 Mars — Conseil Spatial & Chartes Constitutionnelles [Planétaire (Mars), +2060 AD à Futur]";
                case 7 -> "🔴 Mars — Bioregenerative Life Support (BLSS) & Dégradation [Planétaire (Mars), +2050 AD à Futur]";
                case 8 -> "🔴 Mars — Microbiome Artificiel Confiné & Résistance [Planétaire (Mars), +2050 AD à Futur]";
                default -> null;
            };
        }
        if (pName.contains("vénus") || pName.contains("venus") || pName.contains("hesperos")) {
            return switch (tensorIdx) {
                case 0 -> "🟡 Vénus — Réseau Isogloss des Cités Aérostatiques [Planétaire (Vénus), +2120 AD à Futur]";
                case 1 -> "🟡 Vénus — Guildes & Lignages Technologiques Flottants [Planétaire (Vénus), +2100 AD à Futur]";
                case 2 -> "🟡 Vénus — Rituels Solaires & Cérémonies de Nuages [Planétaire (Vénus), +2120 AD à Futur]";
                case 3 -> "🟡 Vénus — Fédération des Stations Stratosphériques [Planétaire (Vénus), +2110 AD à Futur]";
                case 4 -> "🟡 Vénus — Synthèse Aérostatique & Ingénierie Acide [Planétaire (Vénus), +2100 AD à Futur]";
                case 5 -> "🟡 Vénus — Navettes Stratosphériques Inter-Stations [Planétaire (Vénus), +2110 AD à Futur]";
                case 6 -> "🟡 Vénus — Syndicats Flottants & Corporations Aérostats [Planétaire (Vénus), +2110 AD à Futur]";
                case 7 -> "🟡 Vénus — Érosion Chimique & Recyclage Fermé [Planétaire (Vénus), +2100 AD à Futur]";
                case 8 -> "🟡 Vénus — Immunologie en Atmosphère Confinée [Planétaire (Vénus), +2110 AD à Futur]";
                default -> null;
            };
        }
        if (pName.contains("lune") || pName.contains("moon") || pName.contains("selene")) {
            return switch (tensorIdx) {
                case 0 -> "⚪ Lune — Dialectes Sélénites des Stations Cratériques [Planétaire (Lune), +2050 AD à Futur]";
                case 1 -> "⚪ Lune — Associations d'Équipages & Clans Sélénites [Planétaire (Lune), +2045 AD à Futur]";
                case 2 -> "⚪ Lune — Philosophie Cosmique & Rituels du Clair de Terre [Planétaire (Lune), +2050 AD à Futur]";
                case 3 -> "⚪ Lune — Secteurs Traité de l'Espace & Bases Nationales [Planétaire (Lune), +2050 AD à Futur]";
                case 4 -> "⚪ Lune — Fonderies Régolithes & Extraction Sélénite [Planétaire (Lune), +2045 AD à Futur]";
                case 5 -> "⚪ Lune — Tunnels de Transport Magnétique Sélénite [Planétaire (Lune), +2050 AD à Futur]";
                case 6 -> "⚪ Lune — Protocoles Légaux des Habitats Sélénites [Planétaire (Lune), +2050 AD à Futur]";
                case 7 -> "⚪ Lune — Épuisement des Volatils & Poussière Régolithe [Planétaire (Lune), +2045 AD à Futur]";
                case 8 -> "⚪ Lune — Pathogènes d'Isolement & Régime Stérile [Planétaire (Lune), +2045 AD à Futur]";
                default -> null;
            };
        }
        if (pName.contains("mercure") || pName.contains("mercury") || pName.contains("hermes")) {
            return switch (tensorIdx) {
                case 0 -> "⚪ Mercure — Protocoles Herméens & Terminologie d'Ombre [Planétaire (Mercure), +2160 AD à Futur]";
                case 1 -> "⚪ Mercure — Confréries de Maintenance & Lignages Thermiques [Planétaire (Mercure), +2150 AD à Futur]";
                case 2 -> "⚪ Mercure — Ordres d'Énergie & Croyances de Haute Radiation [Planétaire (Mercure), +2170 AD à Futur]";
                case 3 -> "⚪ Mercure — Domaines Miniers & Enclaves Polaires [Planétaire (Mercure), +2150 AD à Futur]";
                case 4 -> "⚪ Mercure — Collecteurs Haute Énergie & Fours Directs [Planétaire (Mercure), +2150 AD à Futur]";
                case 5 -> "⚪ Mercure — Réseau de Convois Électromagnétiques [Planétaire (Mercure), +2160 AD à Futur]";
                case 6 -> "⚪ Mercure — Administration Thermique & Urgences [Planétaire (Mercure), +2160 AD à Futur]";
                case 7 -> "⚪ Mercure — Usure Thermique & Contraintes Matérielles [Planétaire (Mercure), +2150 AD à Futur]";
                case 8 -> "⚪ Mercure — Filtrage Radiatif & Microbiote Synthétique [Planétaire (Mercure), +2160 AD à Futur]";
                default -> null;
            };
        }
        // Earth — pick based on epoch
        if (startYear < -10000) {
            return switch (tensorIdx) {
                case 0 -> "ðŸŒ Terre — Automated Phonological Distance Model (ASJP) [Global, -10 000 BC à Actuel (Macro-Familles)]";
                case 1 -> "ðŸŒ Terre — Standard Cross-Cultural Sample (SCCS) [Global, -4000 BC à Actuel (186 Cultures)]";
                case 2 -> "ðŸŒ Terre — Turchin Asabiyyah Cohesion Metric (Cliodynamics) [Global, -3000 BC à +2000 AD]";
                case 3 -> "ðŸŒ Terre — GADM Administrative Sovereign Centers [Global, 1950 AD à Actuel]";
                case 4 -> "ðŸŒ Terre — Lithic-to-Metallurgy Technology Frontier Model [Global, -100 000 BC à +2000 AD]";
                case 5 -> "ðŸŒ Terre — Old World Overland Caravan Network [Sahara & Asie Centrale, -1000 BC à +1800 AD]";
                case 6 -> "ðŸŒ Terre — Historical Jurisprudence & Administration Matrix [Europe & Asie, -2000 BC à +1800 AD]";
                case 7 -> "ðŸŒ Terre — Malthusian Carrying Capacity Model [Global, -100 000 BC à +2100 AD]";
                case 8 -> "ðŸŒ Terre — Host-Pathogen Coevolution & Immunity Model [Global, -100 000 BC à +2100 AD]";
                default -> null;
            };
        }
        // Default Earth modern/historical: first real item per tensor
        return switch (tensorIdx) {
            case 0 -> "ðŸŒ Terre — Glottolog 4.8 / WALS Language Families [Global, -10 000 BC à Actuel (8 500+ Langues)]";
            case 1 -> "ðŸŒ Terre — Murdock Ethnographic Atlas (Kinship Systems) [Global, -4000 BC à Actuel (1 267 Sociétés)]";
            case 2 -> "ðŸŒ Terre — Seshat Global History Databank (Rituals & Sacred) [Global, -5000 BC à +1900 AD]";
            case 3 -> "ðŸŒ Terre — Centennia Historical Atlas (Sovereignty Boundaries) [Eurasie / Afrique / Amériques, -1000 BC à +2000 AD]";
            case 4 -> "ðŸŒ Terre — ArchaeoGLOBE Project (Land Use & Material Tools) [Global, -10 000 BC à +1850 AD]";
            case 5 -> "ðŸŒ Terre — ORBIS Stanford Geospatial Network (Trade Routes) [Bassin Méditerranéen & Proche-Orient, -300 BC à +500 AD]";
            case 6 -> "ðŸŒ Terre — Seshat Databank (Institutional Complexity & Law) [Global, -4000 BC à +1900 AD]";
            case 7 -> "ðŸŒ Terre — HYDE 3.4 Historical Land Use & Anthropogenic Stress [Global, -10 000 BC à +2023 AD]";
            case 8 -> "ðŸŒ Terre — GADM / Historical Pathogen Memory & Epidemics [Global, -3000 BC à +2023 AD]";
            default -> null;
        };
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
        if (planet == null) planet = activePlanetPreset != null ? activePlanetPreset : PlanetPreset.EARTH_LIKE;
        if (planet.customElevBase64() != null && !planet.customElevBase64().isBlank()) {
            return org.ether.society.data.ImageMapLoader.base64PngToImage(planet.customElevBase64());
        }
        String pSource = planet.elevationMapSource();
        if (pSource == null || pSource.isBlank() || "none".equalsIgnoreCase(pSource)) {
            pSource = planet.getCanonicalPlanet();
        }
        long yr = planet.getAssociatedEpochYear();
        Image img = org.ether.society.data.ImageMapLoader.loadMapImage(pSource, yr, "elevation.png");
        if (img == null && "earth".equalsIgnoreCase(pSource)) {
            img = org.ether.society.data.ImageMapLoader.loadMapImage("earth", 2026L, "elevation.png");
        }
        return img;
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
        this.externalProgressBar = bar;
        this.externalStatusLabel = label;
        this.externalOverlayContainer = overlayContainer;
    }

    private void updateProgress(double progressVal, String msg) {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        javafx.application.Platform.runLater(() -> {
            if (progressBar != null) {
                progressBar.setProgress(progressVal);
                progressBar.setVisible(true);
                progressBar.setManaged(true);
            }
            if (progressStatusLabel != null) {
                progressStatusLabel.setText(msg);
                progressStatusLabel.setVisible(true);
                progressStatusLabel.setManaged(true);
            }
            if (externalProgressBar != null) {
                externalProgressBar.setProgress(progressVal);
            }
            if (externalStatusLabel != null) {
                externalStatusLabel.setText(msg);
            }
            if (externalOverlayContainer != null) {
                boolean active = progressVal < 1.0;
                externalOverlayContainer.setVisible(active);
                externalOverlayContainer.setManaged(active);
            }
        });
    }

    /**
     * Invalidate generated state.
     *
     */
    public void invalidateGeneratedState() {
        resetStartButtonState();
    }

    private void resetStartButtonState() {
        isCalculationRunning = false;
        WindowUtils.setBusyCursor(this, false);
        javafx.application.Platform.runLater(() -> {
            if (startBtn != null) {
                startBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.start_btn", "🚀 VALIDER ET LANCER LA SIMULATION"));
                startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
                startBtn.setDisable(false);
            }
        });
    }

    private void setStartButtonCompletedState() {
        isCalculationRunning = false;
        WindowUtils.setBusyCursor(this, false);
        javafx.application.Platform.runLater(() -> {
            if (startBtn != null) {
                startBtn.setText(I18n.getOrDefault("scenario.btn.valid_ready", "⚡ VALID & READY — PROCEED TO EXECUTION CONTEXT (TAB 4)"));
                startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #0284c7; -fx-text-fill: white; -fx-background-radius: 6;");
                startBtn.setDisable(false);
            }
        });
    }

    private void setStartButtonCancelState() {
        isCalculationRunning = true;
        javafx.application.Platform.runLater(() -> {
            if (startBtn != null) {
                startBtn.setText(I18n.getOrDefault("scenario.btn.cancel_gen", "⏹ CANCEL / STOP GENERATION"));
                startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6;");
                startBtn.setDisable(false);
            }
        });
    }

    private void handleStartOrCancel() {
        if (isCalculationRunning) {
            cancelCalculation();
        } else {
            if (isSimulationRunningSupplier != null && Boolean.TRUE.equals(isSimulationRunningSupplier.get())) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle(I18n.getOrDefault("scenario.confirm_restart.title", "Simulation en cours"));
                alert.setHeaderText(I18n.getOrDefault("scenario.confirm_restart.header", "Une simulation est actuellement active"));
                alert.setContentText(I18n.getOrDefault("scenario.confirm_restart.content", "Lancer cette nouvelle configuration réinitialisera la simulation en cours. Souhaitez-vous continuer ?"));
                java.util.Optional<ButtonType> res = alert.showAndWait();
                if (res.isEmpty() || res.get() != ButtonType.OK) {
                    return;
                }
            }
            if (isDirty()) {
                javafx.stage.Window window = getScene() != null ? getScene().getWindow() : null;
                if (!promptSaveIfDirty(window)) {
                    return;
                }
            }
            startSimulationDeferred();
        }
    }

    private void cancelCalculation() {
        cancelRequested = true;
        updateProgress(0.0, I18n.getOrDefault("scenario.progress.cancelling", "🛑 Cancelling computation at user request..."));
        if (generationThread != null && generationThread.isAlive()) {
            generationThread.interrupt();
        }
        WindowUtils.setBusyCursor(this, false);
        resetStartButtonState();
    }

    private void startSimulationDeferred() {
        if (!validateScenarioSetup()) {
            resetStartButtonState();
            return;
        }

        // Capture all parameters safely on the JavaFX UI Thread before background execution
        final Scenario scenarioToSave = getScenario();
        PlanetPreset baseCfg = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        if (baseCfg == null) baseCfg = PlanetPreset.EARTH_LIKE;

        Integer res = h3ResolutionCombo != null ? h3ResolutionCombo.getValue() : null;
        if (res != null) {
            baseCfg = new PlanetPreset(
                    baseCfg.name(), res, baseCfg.radiusKm(), baseCfg.dayLengthHours(),
                    baseCfg.axialTiltDegrees(), baseCfg.yearLengthDays(), baseCfg.distanceToSunAU(),
                    baseCfg.solarLuminosity(), baseCfg.minAltitudeMeters(), baseCfg.maxAltitudeMeters(),
                    baseCfg.averageTempC(), baseCfg.seed(), baseCfg.noiseFrequency(),
                    baseCfg.noiseScale(), baseCfg.waterLevel(), baseCfg.temperatureGradient(),
                    baseCfg.oxygenPercentage(), baseCfg.albedo(), baseCfg.atmospherePressureAtm(),
                    baseCfg.isSatellite(), baseCfg.parentPlanetMassEarthMasses(),
                    baseCfg.orbitalDistanceToParentKm(), baseCfg.co2Ppm(),
                    baseCfg.seismicActivityLevel(), baseCfg.volcanicActivityLevel(),
                    baseCfg.customElevBase64(), baseCfg.customBiomeBase64(), baseCfg.customResourceBase64(),
                    baseCfg.customClimateBase64(), baseCfg.customRainfallBase64(), baseCfg.customSeasonalityBase64(),
                    baseCfg.elevationUseImport(), baseCfg.elevationMapSource(),
                    baseCfg.tempUseImport(), baseCfg.tempSource(), baseCfg.tempSeed(),
                    baseCfg.precipUseImport(), baseCfg.precipSource(), baseCfg.precipSeed(),
                    baseCfg.seasonUseImport(), baseCfg.seasonSource(), baseCfg.seasonSeed()
            );
        }
        final PlanetPreset cfg = baseCfg;

        final boolean isClippingActive = (clippingCheckBox != null && clippingCheckBox.isSelected());
        final double cMinLat = minLatSpinner != null ? minLatSpinner.getValue() : -90.0;
        final double cMaxLat = maxLatSpinner != null ? maxLatSpinner.getValue() : 90.0;
        final double cMinLng = minLngSpinner != null ? minLngSpinner.getValue() : -180.0;
        final double cMaxLng = maxLngSpinner != null ? maxLngSpinner.getValue() : 180.0;

        if (isResumeFromSnapshotSelected()) {
            final var selectedMeta = getSelectedSnapshotMetadata();
            logger.info("Directly resuming simulation from snapshot: {}", selectedMeta != null ? selectedMeta.getName() : "latest");
            javafx.application.Platform.runLater(() -> {
                updateProgress(1.0, I18n.getOrDefault("scenario.progress.instant_resume", "⚡ Instant restoration from selected snapshot..."));
                setStartButtonCompletedState();
                if (onStartSimulation != null) {
                    onStartSimulation.accept(scenarioToSave);
                }
            });
            return;
        }

        cancelRequested = false;
        setStartButtonCancelState();
        WindowUtils.setBusyCursor(this, true);

        // Immediately display the progress bar and status on UI thread
        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(0.02);
        }
        if (progressStatusLabel != null) {
            progressStatusLabel.setVisible(true);
            progressStatusLabel.setManaged(true);
            progressStatusLabel.setText(I18n.getOrDefault("scenario.progress.init_prep", "⚡ Scenario initialization & setup..."));
        }
        if (externalOverlayContainer != null) {
            externalOverlayContainer.setVisible(true);
            externalOverlayContainer.setManaged(true);
        }

        generationThread = new Thread(() -> {
            try {
                updateProgress(0.02, I18n.getOrDefault("scenario.progress.saving_db", "⚡ Saving scenario & preparing H3 grid..."));

                // 1. Cascade Planet and Ecology presets & save Scenario to DB (only when clean/saved)
                if (!isDirty()) {
                    try {
                        scenarioToSave.setPlanetPreset(cfg);
                        scenarioToSave.setEcologyPreset(ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null);
                        if (scenarioToSave.getName() == null || scenarioToSave.getName().isBlank()) {
                            scenarioToSave.setName("Auto_Scenario_" + System.currentTimeMillis());
                        }
                        scenarioRepo.saveOrUpdate(scenarioToSave);
                        logger.info("Scenario '{}' automatically saved to database repository with full cascade before simulation launch.", scenarioToSave.getName());
                    } catch (Exception ex) {
                        logger.warn("Could not save scenario to DB prior to simulation launch", ex);
                    }
                }

                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    throw new java.util.concurrent.CancellationException("Generation cancelled by user");
                }

                updateProgress(0.05, String.format(I18n.getOrDefault("scenario.progress.ready_h3", "✅ Scenario '%s' ready! ⚡ Generating H3 grid..."), scenarioToSave.getName()));

                logger.info("Generating H3 cells for scenario '{}' at resolution {}", scenarioToSave.getName(), cfg.resolution());

                // Progress-tracked procedural planet generation (5% -> 70%)
                List<H3Cell> cells = new ProceduralGenerator().generatePlanet(cfg, (done, total) -> {
                    double progressVal = 0.05 + 0.65 * ((double) done / total);
                    int percent = (int) (progressVal * 100);
                    updateProgress(progressVal, String.format(I18n.getOrDefault("scenario.progress.relief_biomes", "ðŸŒ Generating relief & H3 biomes: %d%% (%d / %d cells)"), percent, done, total));
                }, () -> cancelRequested || Thread.currentThread().isInterrupted());

                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    throw new java.util.concurrent.CancellationException("Generation cancelled by user");
                }

                // Map elevation and biome images corresponding to the selected planet preset
                applyPresetMapToCells(cells, cfg);

                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    throw new java.util.concurrent.CancellationException("Generation cancelled by user");
                }

                // Hydrography and river basin accumulation (70% -> 78%)
                updateProgress(0.72, I18n.getOrDefault("scenario.progress.hydrography", "🌊 Computing hydrographic network & river basins (72%)..."));

                // Apply Geographical Clipping if enabled (78% -> 85%)
                if (isClippingActive) {
                    updateProgress(0.80, I18n.getOrDefault("scenario.progress.clipping", "✂️ï¸ Applying geographical clipping & boundary conditions (80%)..."));

                    double marginLat = Math.max(1.0, (cMaxLat - cMinLat) * 0.08);
                    double marginLng = Math.max(1.0, (cMaxLng - cMinLng) * 0.08);

                    List<H3Cell> clippedCells = new ArrayList<>();
                    // Iterate over spatial cell domains and apply localized cellular state transformations
                    for (H3Cell c : cells) {
                        if (cancelRequested || Thread.currentThread().isInterrupted()) {
                            throw new java.util.concurrent.CancellationException("Generation cancelled by user");
                        }
                        if (c.getLatitude() >= cMinLat && c.getLatitude() <= cMaxLat &&
                            c.getLongitude() >= cMinLng && c.getLongitude() <= cMaxLng) {
                            
                            // Tag boundary cells for realistic edge condition modeling
                            if (c.getLatitude() <= cMinLat + marginLat || c.getLatitude() >= cMaxLat - marginLat ||
                                c.getLongitude() <= cMinLng + marginLng || c.getLongitude() >= cMaxLng - marginLng) {
                                c.setBoundaryCell(true);
                            }
                            clippedCells.add(c);
                        }
                    }
                    cells = clippedCells;
                    logger.info("Clipping applied: {} cells retained out of global planet grid.", cells.size());
                }

                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    throw new java.util.concurrent.CancellationException("Generation cancelled by user");
                }

                int totalCellCount = cells.size();

                // Distribute Initial Population (88% -> 94%)
                updateProgress(0.88, String.format(I18n.getOrDefault("scenario.progress.pop_capital", "👥 Distributing population, ecological footprint & capital K₀: 88%% (%d / %d cells)"), totalCellCount, totalCellCount));

                distributeInitialPopulation(cells);
                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    throw new java.util.concurrent.CancellationException("Generation cancelled by user");
                }

                updateProgress(0.94, String.format(I18n.getOrDefault("scenario.progress.thermo_sync", "⚡ Synchronizing simulation buffers & thermodynamic pre-computation: 94%% (%d / %d cells)"), totalCellCount, totalCellCount));

                currentPreviewCells = cells;

                final List<H3Cell> finalCells = cells;
                javafx.application.Platform.runLater(() -> {
                    updateProgress(1.0, String.format(I18n.getOrDefault("scenario.progress.launching", "✅ %,d H3 cells generated! Launching scenario '%s'..."), finalCells.size(), scenarioToSave.getName()));
                    drawPreview();
                    previewStatusLabel.setText(I18n.getOrDefault("scenario.status.generated_prefix", "Generated: ") + finalCells.size() + I18n.getOrDefault("scenario.status.generated_h3_suffix", " H3 cells."));

                    setStartButtonCompletedState();
                    if (scenarioPresetBar != null) {
                        scenarioPresetBar.markClean();
                    }

                    if (onStartSimulation != null) {
                        onStartSimulation.accept(scenarioToSave);
                    }
                });
            } catch (Exception ex) {
                boolean isCancel = cancelRequested || ex instanceof java.util.concurrent.CancellationException
                        || ex instanceof InterruptedException
                        || (ex.getCause() instanceof java.util.concurrent.CancellationException);
                if (isCancel) {
                    logger.info("Deferred H3 calculation cancelled by user.");
                    updateProgress(0.0, I18n.getOrDefault("scenario.progress.cancelled", "🛑 Computation cancelled by user."));
                } else {
                    logger.error("Error during deferred H3 cell calculation", ex);
                    updateProgress(1.0, String.format(I18n.getOrDefault("scenario.progress.error", "❌ Error during computation: %s"), ex.getMessage()));
                }
                resetStartButtonState();
            } finally {
                generationThread = null;
                WindowUtils.setBusyCursor(ScenarioSetupPanel.this, false);
            }
        }, "H3-Scenario-Generator-Thread");
        generationThread.setDaemon(true);
        generationThread.start();
    }

    private void applyPresetMapToCells(List<H3Cell> cells, PlanetPreset cfg) {
        if (cfg == null || cells == null || cells.isEmpty()) return;
        
        // If not using imported maps and no custom base64 image provided, keep procedural terrain
        boolean isImportMode = cfg.elevationUseImport() || (cfg.customElevBase64() != null && !cfg.customElevBase64().isBlank());
        if (!isImportMode) {
            return;
        }

        String mapSource = cfg.elevationMapSource() != null ? cfg.elevationMapSource().toLowerCase() : "";
        String presetName = cfg.name() != null ? cfg.name().toLowerCase() : "";
        
        if (cfg.customElevBase64() != null && !cfg.customElevBase64().isBlank()) {
            try {
                Image customElev = org.ether.society.data.ImageMapLoader.base64PngToImage(cfg.customElevBase64());
                Image customBiome = org.ether.society.data.ImageMapLoader.base64PngToImage(cfg.customBiomeBase64());
                if (customElev != null) {
                    new org.ether.society.data.ImageMapLoader().mapImagesToCells(cells, customElev, customBiome, null, cfg.minAltitudeMeters(), cfg.maxAltitudeMeters());
                }
            } catch (Exception e) {
                logger.warn("Could not decode custom Base64 elevation map image for preset {}", cfg.name(), e);
            }
        } else {
            String pKey = "earth";
            if (mapSource.equals("mars") || presetName.contains("mars") || presetName.contains("ares")) pKey = "mars";
            else if (mapSource.equals("venus") || presetName.contains("venus") || presetName.contains("vénus") || presetName.contains("hesperos")) pKey = "venus";
            else if (mapSource.equals("moon") || presetName.contains("lune") || presetName.contains("moon") || presetName.contains("selene")) pKey = "moon";
            else if (mapSource.equals("mercury") || presetName.contains("mercure") || presetName.contains("mercury") || presetName.contains("hermes")) pKey = "mercury";

            long targetYear = (scenarioPresetBar != null && scenarioPresetBar.getPresetCombo() != null && scenarioPresetBar.getPresetCombo().getValue() != null) 
                    ? scenarioPresetBar.getPresetCombo().getValue().getStartDateYear() : 2026L;
            Image elevImg = org.ether.society.data.ImageMapLoader.loadMapImage(pKey, targetYear, "elevation.png");
            Image biomeImg = org.ether.society.data.ImageMapLoader.loadMapImage(pKey, targetYear, "biomes.png");

            if (elevImg != null || biomeImg != null) {
                new org.ether.society.data.ImageMapLoader().mapImagesToCells(cells, elevImg, biomeImg, null, cfg.minAltitudeMeters(), cfg.maxAltitudeMeters());
            } else {
                logger.debug("No precalculated map found for preset {}, using physical terrain generator", pKey);
            }
        }
    }

    // =========================================================================
    // EVENTS SECTION — Historical Planetary Events
    // =========================================================================

    @SuppressWarnings("unchecked")
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
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (year <= -50000) return 1.0;   // Early Paleolithic / Out of Africa (~1 kg/hab)
        if (year <= -10000) return 5.0;   // Late Paleolithic / Early Neolithic (~5 kg/hab)
        if (year <= -3000)  return 25.0;  // Bronze Age (~25 kg/hab)
        if (year <= -500)   return 100.0; // Antiquity (~100 kg/hab)
        if (year <= 1000)   return 200.0; // Early Middle Ages (~200 kg/hab)
        if (year <= 1500)   return 500.0; // Renaissance / Age of Discovery (~500 kg/hab)
        if (year <= 1800)   return 2500.0; // Industrial Revolution (~2500 kg/hab)
        if (year <= 1950)   return 8000.0; // 20th Century (~8000 kg/hab)
        return 15000.0;                   // Modern / Post-Industrial (>15000 kg/hab)
    }

    /**
     * Compute auto energy from year.
     *
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static double computeAutoEnergyFromYear(long year) {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (year <= -10000) return 10.0;    // Firewood (~10 MJ/hab)
        if (year <= -3000)  return 30.0;    // Wood & fodder (~30 MJ/hab)
        if (year <= -500)   return 60.0;    // Oil & firewood (~60 MJ/hab)
        if (year <= 1000)   return 150.0;   // Hydraulic & firewood (~150 MJ/hab)
        if (year <= 1500)   return 300.0;   // Peat & timber (~300 MJ/hab)
        if (year <= 1800)   return 2500.0;  // Coal mines (~2500 MJ/hab)
        if (year <= 1950)   return 10000.0; // Hydrocarbons (~10000 MJ/hab)
        return 50000.0;                     // Modern/Future (>50000 MJ/hab)
    }

    /**
     * Compute auto food from year.
     *
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static double computeAutoFoodFromYear(long year) {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (year <= -10000) return 2.0;  // Foraging (2 months)
        if (year <= -3000)  return 4.0;  // Early granaries (4 months)
        if (year <= -500)   return 6.0;  // Roman/Han silos (6 months)
        if (year <= 1000)   return 6.0;  // Feudal granaries (6 months)
        if (year <= 1500)   return 8.0;  // Early modern storage (8 months)
        if (year <= 1800)   return 10.0; // Agricultural revolution (10 months)
        if (year <= 1950)   return 12.0; // Modern global logistics (12 months)
        return 18.0;                     // Future/Post-scarcity (18+ months)
    }

    /**
     * Compute auto info from year.
     *
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static double computeAutoInfoFromYear(long year) {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (year <= -10000) return 5.0;        // Oral tradition (~5 bits/hab)
        if (year <= -3000)  return 40.0;       // Cuneiform/Hieroglyphs (~40 bits/hab)
        if (year <= -500)   return 200.0;      // Classical manuscripts (~200 bits/hab)
        if (year <= 1000)   return 500.0;      // Scriptoriums & block printing (~500 bits/hab)
        if (year <= 1500)   return 2000.0;     // Printing press (~2000 bits/hab)
        if (year <= 1800)   return 15000.0;    // Encyclopedias & press (~15000 bits/hab)
        if (year <= 1950)   return 500000.0;   // Telecommunications & radio (~500000 bits/hab)
        return 5000000.0;                      // Internet & AI (>5000000 bits/hab)
    }

    /**
     * Apply earth preset.
     *
     */
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
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        boolean oldUpdating = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            if (headerLabel != null) headerLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.panel.title", "📜 Scenario Configuration & Setup"));
            if (scenarioPresetHeader != null) scenarioPresetHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.section.presets", "ðŸŽ›ï¸ GLOBAL PRESETS & SCENARIO SAVE"));
            if (btnAutoEpochScenario != null) {
                btnAutoEpochScenario.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.auto_epoch_wizard", "✨ Créer un Scénario Automatique par Date..."));
                btnAutoEpochScenario.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.auto_epoch_wizard", "Ouvre l'assistant de scénario temporel pour configurer en un clic les cartes d'élévation, biomes, ressources, démographie et moteurs de Type B compatibles pour n'importe quelle date.")));
            }
            if (scenarioPresetBar != null) {
                scenarioPresetBar.updateTexts();
            }
            if (scenarioDescLabel != null) scenarioDescLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.section.description", "📖 Detailed Description, Initial Conditions & Key Observables:"));
            if (planetSectionHeader != null) planetSectionHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.section.inherited", "ðŸª INHERITED CONTEXT (TABS 1 & 2)"));
            if (ecoPromptLabel != null) ecoPromptLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.label.ecology_preset", "1. Ecological Preset (Tab 2):"));
            if (planetPromptLabel != null) planetPromptLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.label.planet_preset", "2. Planetary Preset (Tab 1 — Cascaded from Ecology):"));
            if (planetPresetCombo != null && planetPresetCombo.getCellFactory() != null) {
                planetPresetCombo.setButtonCell(planetPresetCombo.getCellFactory().call(null));
            }
            if (ecologyPresetCombo != null && ecologyPresetCombo.getCellFactory() != null) {
                ecologyPresetCombo.setButtonCell(ecologyPresetCombo.getCellFactory().call(null));
            }
            updateInheritedContextDisplay(null);
            updateEngineTexts();
            if (title1 != null) title1.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.section.spatiotemporal", "ðŸŒ EPOCH & SPATIOTEMPORAL DEFINITION"));
            if (cultureHeader != null) cultureHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.culture_section", "🧠 CULTURAL VECTOR DIMENSION & MULTI-FIELD LAYERS"));
            if (clippingHeader != null) clippingHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.header", "✂️ï¸ BORDERS & HISTORICAL SPATIAL CLIPPING"));
            if (oceanOptHeader != null) oceanOptHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.ocean_opt.header", "⚙️ï¸ ENGINE ARCHITECTURE & OPTIMIZATIONS (ETHER CORE & OPTIONAL)"));
            if (title3Events != null) title3Events.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events_section", "ðŸŒªï¸ HISTORICAL PLANETARY EVENTS & CLIMATE DRIFTS"));
            if (snapshotHeader != null) snapshotHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.snapshot.header", "📸 7. RESUME FROM EXISTING SNAPSHOT (PREVIOUS SESSION)"));
            if (radioNewSimulation != null) {
                radioNewSimulation.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.start_fresh", "🌱 Start a new simulation from scratch (Year T₀)"));
                radioNewSimulation.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.start_fresh.tooltip", "Initialize a brand-new historical simulation from canonical T₀ initial conditions.")));
            }
            if (radioResumeSnapshot != null) {
                radioResumeSnapshot.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.start_snapshot", "📸 Resume from an existing Snapshot (Previous Session / Checkpoint)"));
                radioResumeSnapshot.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.start_snapshot.tooltip", "Load a previously saved physical state or periodic checkpoint directly into memory.")));
            }
            if (snapshotExplainBtn != null) {
                snapshotExplainBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.snapshot_explain", "ℹ️ï¸ What is a Snapshot? (Explanations & Mechanics)"));
                snapshotExplainBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.snapshot_explain", "Open detailed technical guide explaining physical state capture, rolling checkpoints, and multiverse branching.")));
            }
            if (snapshotRefreshBtn != null) {
                snapshotRefreshBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.refresh", "🔄 Refresh Snapshots"));
                snapshotRefreshBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.snapshot_refresh", "Rescans the disk saves directory (saves/) to synchronize the list of real simulation snapshots and checkpoints.")));
            }
            if (snapshotCombo != null) {
                snapshotCombo.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.snapshot.combo.tooltip", "Select an existing checkpoint or full simulation snapshot from the disk to resume execution.")));
                updateSnapshotDetailsDisplay(snapshotCombo.getValue());
            }
            if (bundleHeader != null) bundleHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.bundle.header", "📦 UNIFIED BUNDLE MULTI-SCENARIO IMPORT/EXPORT (.ETHER)"));
            if (bundleSubtitle != null) bundleSubtitle.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.desc.bundle", "Export or import the complete scenario (planetary context, ecology, active engines, cultural layers, and demographic grid) in unified .ether format for archiving or sharing."));
            if (btnExportBundle != null) {
                btnExportBundle.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.export_bundle", "📦 Export Bundle (.ether)"));
                btnExportBundle.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.export_bundle", "Export complete scenario (physics, ecology, engines, layers, and demographics) to a unified .ether bundle file.")));
            }
            if (btnImportBundle != null) {
                btnImportBundle.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.import_bundle", "📂 Import Bundle (.ether)"));
                btnImportBundle.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.import_bundle", "Import and apply a unified .ether bundle file to instantly restore full scenario state.")));
            }
            if (liveDiagnosticHeader != null) liveDiagnosticHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.diagnostic.header", "📋 CIVILIZATIONAL VIABILITY DIAGNOSTIC (REAL TIME)"));
            if (previewTitleLabel != null) previewTitleLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.title.right_view", "ðŸ—ºï¸ Resource Cartography & Display"));
            if (startYearLabel != null) startYearLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.start_year", "Start Year (Chronological Reference):"));
            if (endYearLabel != null) endYearLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.end_year", "End / Target Year (Chronological Reference):"));
            if (popCountLabel != null) popCountLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_count", "Initial Population (1,000 to 10,000,000,000):"));
            if (densityPatternLabel != null) densityPatternLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.density_pattern", "Distribution Pattern:"));
            if (h3ResolutionLabel != null) h3ResolutionLabel.setText(org.ether.society.i18n.I18n.getOrDefault("planet.param.resolution", "H3 Resolution:"));
            if (h3ResolutionHintLabel != null) h3ResolutionHintLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.h3.hint", "• Res 2–3 : Planétaire global (~41k cel.) | Res 4–5 : Régional (~2.8M cel.) | Res 6+ : Micro-bassin."));
            if (temporalResolutionLabel != null) temporalResolutionLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.label.temporal_resolution", "Time Step Δt (Temporal Resolution):"));
            if (temporalResolutionHintLabel != null) temporalResolutionHintLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.temporal.hint", "• 1 Mois : Recommandé par défaut (équilibre saisons/vitesse) | 1 Jour/Sem. : Épidémies & météo | 1 An : Multi-millénaire."));
            if (cohortSizeLabel != null) cohortSizeLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.label.cohort_size", "Cohort Size:"));
            if (cohortSizeHintLabel != null) cohortSizeHintLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.cohort.hint", "• 1–30 : Micro-tribus nomades | 150 : Recommandé par défaut (Nombre de Dunbar) | 10k–50k : Macro-cohortes."));
            if (snapshotAutoPruningNotice != null) snapshotAutoPruningNotice.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.snapshot.auto_pruning_notice", "ℹ️ Gestion Automatique : Les snapshots sont générés automatiquement par le moteur à intervalles réguliers et élagués logarithmiquement dans le temps. L'utilisateur n'a aucune action manuelle de suppression à effectuer : l'historique s'étale de façon optimale sur des millénaires sans saturer la mémoire ni le disque."));
            if (startBtn != null) startBtn.setText(org.ether.society.i18n.I18n.get("scenario.start_btn"));
            if (generateBtn != null) generateBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.preview_btn", "🔄 Preview Distribution"));
            if (addEventBtn != null) addEventBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.add", "➕ Ajouter"));
            if (removeEventBtn != null) removeEventBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.remove", "🗑️ Supprimer"));
            if (clearEventsBtn != null) clearEventsBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.clear", "🧹 Vider le Calendrier"));
            if (loadEarthEventsBtn != null) loadEarthEventsBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.load_biophysical", "🌋 Injecter Catastrophes Biophysiques"));
            if (loadEarthLeadersBtn != null) loadEarthLeadersBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.load_leaders", "👑 Injecter Figures & Personnages Historiques"));
            if (randomBiophysicalEventsCheckBox != null) {
                randomBiophysicalEventsCheckBox.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.random_biophysical", "🎲 Événements aléatoires biophysiques (Séismes, Volcans, Famines, Sécheresses, Épidémies)"));
                randomBiophysicalEventsCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.random_biophysical",
                    "🎲 ÉVÉNEMENTS ALÉATOIRES BIOPHYSIQUES & CATASTROPHES\n" +
                    "• Si activé : Le moteur simule des crises biophysiques spontanées (éruptions, tremblements de terre, pestes, famines) générées selon la graine (Seed) et la température globale.\n" +
                    "• Si désactivé : Aucune crise biophysique aléatoire ne survient.")));
            }
            if (randomPoliticalEventsCheckBox != null) {
                randomPoliticalEventsCheckBox.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.random_political", "👑 Événements aléatoires politiques & historiques (Émergence d'outliers, Réformateurs, Crises dynastiques)"));
                randomPoliticalEventsCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.random_political",
                    "👑 ÉVÉNEMENTS ALÉATOIRES POLITIQUES & SOCIÉTAUX\n" +
                    "• Si activé : Le moteur fait émerger stochastiquement des figures historiques et leaders de rupture (bâtisseurs, conquérants, prophètes) selon la complexité et les tensions sociales.\n" +
                    "• Si désactivé : Trajectoire sociologique structurelle pure sans émergence spontanée de leaders.")));
            }

            if (colType != null) colType.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.type", "Event Type"));
            if (colName != null) colName.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.name", "Event Name"));
            if (colYear != null) colYear.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.year", "Year (Yr)"));
            if (colLat != null) colLat.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.lat", "Lat (°)"));
            if (colLon != null) colLon.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.lon", "Lng (°)"));
            if (colDepth != null) colDepth.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.depth", "Depth (km)"));
            if (colMag != null) colMag.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.mag", "Magnitude / Intensity"));

            if (clippingCheckBox != null) clippingCheckBox.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.enable", "Enable Partial Simulation (Truncated Zone)"));
            if (graphicSelectBtn != null) graphicSelectBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.select_mode", "ðŸ–±ï¸ Graphic Map Selection Mode"));
            if (resetClippingBtn != null) resetClippingBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.reset", "🔄 Reset Area (Full Planet)"));
            if (latMaxLabel != null) latMaxLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lat_max", "Lat Max (Top):"));
            if (latMinLabel != null) latMinLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lat_min", "Lat Min (Bottom):"));
            if (lngMinLabel != null) lngMinLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lng_min", "Lng Min (Left):"));
            if (lngMaxLabel != null) lngMaxLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lng_max", "Lng Max (Right):"));
            if (boundaryLabel != null) boundaryLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.boundary_label", "Scientific Border Modeling:"));
            if (culturalHelpBtn != null) culturalHelpBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.cultural_format_help", "â“ Format des Calques"));
            if (btnGenerateProceduralTensorsSection != null) {
                btnGenerateProceduralTensorsSection.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.regen_tensors", "🪄 Régénérer les Tenseurs"));
                btnGenerateProceduralTensorsSection.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.regen_tensors", "Bascule tous les tenseurs en mode procédural et régénère les cartes selon les paramètres et la graine stochastique.")));
            }
            if (btnReliefOverlay != null) {
                btnReliefOverlay.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.relief_overlay", "⛏️ï¸ Relief"));
                btnReliefOverlay.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.relief_overlay", "Superposer l'ombrage du relief topographique et des pentes avec délimitation du trait de côte.")));
            }
            updatePreviewModesCombo();
            updatePreviewTitleText();

            if (radioProcDemo != null) radioProcDemo.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.procedural", "▶ Procedural Generation (Perlin / Simplex Noise)"));
            if (radioImportDemo != null) radioImportDemo.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.import", "📂 External Source / Import (PNG / WMS / GeoTIFF)"));
            if (loadDensityMapBtn != null) loadDensityMapBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.demo.btn_load", "📥 Import External Density Map (PNG)"));
            if (exportDensityMapBtn != null) exportDensityMapBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.demo.btn_export", "📤 Export Generated Density Map (PNG)"));

            if (boundaryModeCombo != null && boundaryModeCombo.getCellFactory() != null) {
                boundaryModeCombo.setButtonCell(boundaryModeCombo.getCellFactory().call(null));
            }

            if (densityPatternCombo != null) {
                if (densityPatternCombo.getCellFactory() != null) {
                    densityPatternCombo.setButtonCell(densityPatternCombo.getCellFactory().call(null));
                }
                String val = densityPatternCombo.getValue();
                if (val != null) {
                    densityPatternCombo.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.density.desc." + val, DENSITY_DESCRIPTIONS.getOrDefault(val, ""))));
                }
            }

            if (temporalResolutionCombo != null && temporalResolutionCombo.getCellFactory() != null) {
                temporalResolutionCombo.setButtonCell(temporalResolutionCombo.getCellFactory().call(null));
            }

            if (techPresetCombo != null && techPresetCombo.getCellFactory() != null) {
                techPresetCombo.setButtonCell(techPresetCombo.getCellFactory().call(null));
            }

            if (h3ResolutionCombo != null && h3ResolutionCombo.getCellFactory() != null) {
                h3ResolutionCombo.setButtonCell(h3ResolutionCombo.getCellFactory().call(null));
            }

            if (tensorSourceCombos != null) {
                // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
                for (ComboBox<String> sc : tensorSourceCombos.values()) {
                    if (sc != null && sc.getCellFactory() != null) {
                        sc.setButtonCell(sc.getCellFactory().call(null));
                    }
                }
            }

            updatePreviewModesCombo();

            if (eventsTable != null && eventsTable.getUserData() instanceof TableColumn<?, ?>[] cols && cols.length == 7) {
                cols[0].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.type", "Type"));
                cols[1].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.name", "Nom / Description"));
                cols[2].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.year", "Year"));
                cols[3].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.lat", "Latitude"));
                cols[4].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.lon", "Longitude"));
                cols[5].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.depth", "Profondeur (km)"));
                cols[6].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.magnitude", "Magnitude"));
            }
            if (popHeader != null) {
                popHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_section", "👥 2. INITIAL POPULATION IDENTITY CARD"));
            }
            if (engineSortLabel != null) {
                engineSortLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.sort.label", "🔀 Engine Sorting:"));
            }
            if (engineSortCombo != null) {
                int selIdx = engineSortCombo.getSelectionModel().getSelectedIndex();
                engineSortCombo.getItems().setAll(
                    org.ether.society.i18n.I18n.getOrDefault("scenario.sort.default", "⚙️ï¸ System Order (By Category)"),
                    org.ether.society.i18n.I18n.getOrDefault("scenario.sort.date_asc", "📅 Chronological Sort (Oldest → Newest)"),
                    org.ether.society.i18n.I18n.getOrDefault("scenario.sort.date_desc", "📅 Reverse Chronological Sort (Newest → Oldest)"),
                    org.ether.society.i18n.I18n.getOrDefault("scenario.sort.alpha_asc", "🔤 Alphabetical Sort (A - Z)")
                );
                if (selIdx >= 0 && selIdx < engineSortCombo.getItems().size()) {
                    engineSortCombo.getSelectionModel().select(selIdx);
                } else {
                    engineSortCombo.getSelectionModel().select(0);
                }
            }

            updateCulturalTensorSubBlockTexts();
            updateDemoCompatibilityDisplay();
        } finally {
            isUpdatingFromPreset = oldUpdating;
        }
    }


    /**
     * Get scenario.
     *
     * @return the resulting computation or state reference
     */
    public Scenario getScenario() {
        Scenario s = new Scenario();
        s.setName(scenarioPresetBar != null ? scenarioPresetBar.getCurrentName() : "New Scenario");
        if (scenarioDescriptionArea != null) {
            s.setDescription(scenarioDescriptionArea.getText());
        }
        s.setStartDateYear(startYearSpinner.getValue());
        s.setEndDateYear(endYearSpinner != null && endYearSpinner.getValue() != null ? endYearSpinner.getValue() : 100);
        s.setTargetCohortSize(targetCohortSizeSpinner != null ? targetCohortSizeSpinner.getValue() : 150);
        s.setTemporalResolutionDays(temporalResolutionCombo != null && temporalResolutionCombo.getValue() != null ? temporalResolutionCombo.getValue() : 30.0);
        s.setH3Resolution(h3ResolutionCombo != null && h3ResolutionCombo.getValue() != null ? h3ResolutionCombo.getValue() : 3);
        s.setInitialHumanCount(initialHumanCountSpinner.getValue());
        Scenario.TechPreset preset = techPresetCombo != null && techPresetCombo.getValue() != null ? techPresetCombo.getValue() : Scenario.TechPreset.AUTO_FROM_YEAR;
        s.setTechPreset(preset);

        if (preset == Scenario.TechPreset.AUTO_FROM_YEAR) {
            long startY = startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000;
            s.setInitialCapitalPerCapita(computeAutoCapitalFromYear(startY));
            s.setInitialEnergyPerCapita(computeAutoEnergyFromYear(startY));
            s.setInitialFoodReserveMonths(computeAutoFoodFromYear(startY));
            s.setInitialInformationPerCapita(computeAutoInfoFromYear(startY));
        } else if (preset == Scenario.TechPreset.CUSTOM) {
            s.setInitialCapitalPerCapita(customCapitalSpinner != null && customCapitalSpinner.getValue() != null ? customCapitalSpinner.getValue() : 100.0);
            s.setInitialEnergyPerCapita(customEnergySpinner != null && customEnergySpinner.getValue() != null ? customEnergySpinner.getValue() : 300.0);
            s.setInitialFoodReserveMonths(customFoodSpinner != null && customFoodSpinner.getValue() != null ? customFoodSpinner.getValue() : 6.0);
            s.setInitialInformationPerCapita(customInfoSpinner != null && customInfoSpinner.getValue() != null ? customInfoSpinner.getValue() : 2000.0);
        } else {
            s.setInitialCapitalPerCapita(preset.getCapital());
            s.setInitialEnergyPerCapita(preset.getEnergy());
            s.setInitialFoodReserveMonths(preset.getFoodMonths());
            s.setInitialInformationPerCapita(preset.getInfo());
        }
        s.setPopulationDensityType(densityPatternCombo.getValue());
        s.setEcologyPreset(ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null);
        if (ecologyPresetCombo != null && ecologyPresetCombo.getValue() != null) {
            s.setEcologyPresetName(ecologyPresetCombo.getValue().name());
        }
        s.setPlanetPreset(activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue());

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
        if (clippingCheckBox != null) {
            s.setClippingEnabled(clippingCheckBox.isSelected());
            s.setMinLat(minLatSpinner.getValue());
            s.setMaxLat(maxLatSpinner.getValue());
            s.setMinLng(minLngSpinner.getValue());
            s.setMaxLng(maxLngSpinner.getValue());
            s.setBoundaryMode(boundaryModeCombo.getValue());
        }
        if (strictDeterminismCheckBox != null) {
            s.setStrictDeterminism(strictDeterminismCheckBox.isSelected());
        }
        if (sparseCellSkippingCheckBox != null) {
            s.setSparseCellSkippingEnabled(sparseCellSkippingCheckBox.isSelected());
        }
        if (oceanMacroAggregationCheckBox != null) {
            s.setOceanMacroAggregationEnabled(oceanMacroAggregationCheckBox.isSelected());
        }
        if (coastalNavigationOnlyCheckBox != null) {
            s.setCoastalNavigationOnlyEnabled(coastalNavigationOnlyCheckBox.isSelected());
        }
        if (oceanMultiRateTickingCheckBox != null) {
            s.setOceanMultiRateTickingEnabled(oceanMultiRateTickingCheckBox.isSelected());
        }
        if (climateTickFreqSlider != null) {
            s.setClimateTickFrequency((int) climateTickFreqSlider.getValue());
        }
        if (parallelExecutionCheckBox != null) {
            s.setParallelExecutionEnabled(parallelExecutionCheckBox.isSelected());
        }
        if (threadCountSlider != null) {
            s.setParallelThreadCount((int) threadCountSlider.getValue());
        }
        if (spatialRangeTruncationCheckBox != null) {
            s.setSpatialRangeTruncationEnabled(spatialRangeTruncationCheckBox.isSelected());
        }
        if (customDensityImage != null) {
            s.setCustomDensityBase64(org.ether.society.data.ImageMapLoader.imageToBase64Png(customDensityImage));
        }

        // Save Cultural Vector & Multi-Field Layers
        culturalSectionBuilder.exportToScenario(s);

        // Save Type B engine checkbox states
        java.util.Map<String, Boolean> typeBStates = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, CheckBox> entry : typeBCheckBoxMap.entrySet()) {
            typeBStates.put(entry.getKey(), entry.getValue().isSelected());
        }
        s.setTypeBEngineStates(typeBStates);

        // Save Type B engine fine-grained parameter values
        java.util.Map<String, java.util.Map<String, Double>> typeBParams = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, java.util.Map<String, Spinner<Double>>> engEntry : typeBParamSpinnersMap.entrySet()) {
            java.util.Map<String, Double> engParams = new java.util.HashMap<>();
            // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
            for (java.util.Map.Entry<String, Spinner<Double>> paramEntry : engEntry.getValue().entrySet()) {
                if (paramEntry.getValue() != null && paramEntry.getValue().getValue() != null) {
                    engParams.put(paramEntry.getKey(), paramEntry.getValue().getValue());
                }
            }
            if (!engParams.isEmpty()) {
                typeBParams.put(engEntry.getKey(), engParams);
            }
        }
        s.setTypeBEngineParameters(typeBParams);

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
     * Set on scenario loaded callback.
     *
     * @param callback the callback parameter (EcologyPreset&gt;)
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

    private void exportPhysicalLawEngineTemplate(String className) {
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle(I18n.getOrDefault("scenario.dialog.export_physics_law_title", "Export Physical Law (") + className + ") (.java)");
            fileChooser.setInitialFileName(className + ".java");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichiers Source Java (*.java)", "*.java"));
            java.io.File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                String template = """
                    package org.ether.society.engines.tier1;

                    import org.ether.society.core.H3SimulationEngine;
                    import org.ether.society.database.H3Cell;

                    /**
                     * Loi Physique Cœur Ether : %s
                     */
                    public class %s {
                        /**
                         * Update.
                         *
                         * @param engine the engine parameter (H3SimulationEngine)
                         * @param cell the cell parameter (H3Cell)
                         * @param deltaTime the delta time parameter (double)
                         */
                        public void update(H3SimulationEngine engine, H3Cell cell, double deltaTime) {
                            // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
                            // Conservation thermodynamique & lois de bilans physiques
                        }
                    }
                    """.formatted(className, className);
                java.nio.file.Files.writeString(file.toPath(), template);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Physical Law Engine exporté avec succès :\n" + file.getAbsolutePath());
                alert.show();
            }
        } catch (Exception ex) {
            logger.error("Failed to export physical law engine template", ex);
        }
    }

    private void exportCustomEngineTemplate() {
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle(I18n.getOrDefault("scenario.title.export_engine_dialog", "Export Custom Ether Engine Template (.java)"));
            fileChooser.setInitialFileName("MyCustomOptionalEngine.java");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Java Source (*.java)", "*.java"));
            java.io.File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                String template = org.ether.society.engines.compiler.DynamicEngineCompiler.generateEngineTemplateCode("MyCustomOptionalEngine");
                java.nio.file.Files.writeString(file.toPath(), template);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                        I18n.getOrDefault("scenario.alert.export_template_success", "Custom engine template exported successfully:\n") + file.getAbsolutePath());
                alert.show();
            }
        } catch (Exception ex) {
            logger.error("Error exporting custom engine template", ex);
        }
    }

    private void importCustomEngineFile() {
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle(I18n.getOrDefault("scenario.title.import_engine_dialog", "Import Custom Engine (.java)"));
            fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Java Source (*.java)", "*.java")
            );
            java.io.File file = fileChooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                org.ether.society.engines.compiler.DynamicEngineCompiler.CompilationResult result =
                        org.ether.society.engines.compiler.DynamicEngineCompiler.compileAndLoadEngine(file);

                if (result.success()) {
                    String engineKey = result.engineName();
                    addCustomEngineCheckBoxToUI(engineKey, "🔌 " + engineKey + " (" + I18n.getOrDefault("scenario.engine.custom_badge", "Custom Engine") + ")", 
                            result.message(), true);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, result.message());
                    alert.show();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, result.message());
                    alert.show();
                }
            }
        } catch (Exception ex) {
            logger.error("Error importing custom engine file", ex);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Import error: " + ex.getMessage());
            alert.show();
        }
    }

    private void updatePerformanceControlsState() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        boolean strict = strictDeterminismCheckBox != null && strictDeterminismCheckBox.isSelected();
        List<CheckBox> subOpts = List.of(
            sparseCellSkippingCheckBox,
            oceanMacroAggregationCheckBox,
            coastalNavigationOnlyCheckBox,
            oceanMultiRateTickingCheckBox,
            parallelExecutionCheckBox,
            spatialRangeTruncationCheckBox
        );
        if (strict) {
            for (CheckBox cb : subOpts) {
                if (cb != null) {
                    cb.setSelected(false);
                    cb.setDisable(true);
                }
            }
            if (climateTickFreqSlider != null) climateTickFreqSlider.setDisable(true);
            if (threadCountSlider != null) threadCountSlider.setDisable(true);
        } else {
            for (CheckBox cb : subOpts) {
                if (cb != null) cb.setDisable(false);
            }
            if (climateTickFreqSlider != null && oceanMultiRateTickingCheckBox != null) {
                climateTickFreqSlider.setDisable(!oceanMultiRateTickingCheckBox.isSelected());
            }
            if (threadCountSlider != null && parallelExecutionCheckBox != null) {
                threadCountSlider.setDisable(!parallelExecutionCheckBox.isSelected());
            }
        }
    }

    /**
     * Custom SpinnerValueFactory for Long values to avoid ClassCastException.
     */
    public static class LongSpinnerValueFactory extends SpinnerValueFactory<Long> {
        /** Internal state: min (long). */
        private final long min;
        /** Internal state: max (long). */
        private final long max;
        /** Internal state: step (long). */
        private final long step;

        /**
         * Long spinner value factory.
         *
         * @param min the min parameter (long)
         * @param max the max parameter (long)
         * @param initialValue the initial value parameter (long)
         * @param step the step parameter (long)
         * @return the resulting computation or state reference
         */
        public LongSpinnerValueFactory(long min, long max, long initialValue, long step) {
            this.min = min;
            this.max = max;
            this.step = step;
            setConverter(new StringConverter<Long>() {
                /**
                 * To string.
                 *
                 * @param object the object parameter (Long)
                 * @return the resulting computation or state reference
                 */
                @Override
                public String toString(Long object) {
                    return object == null ? "" : String.valueOf(object);
                }
                /**
                 * From string.
                 *
                 * @param string the string parameter (String)
                 * @return the resulting computation or state reference
                 */
                @Override
                public Long fromString(String string) {
                    if (string == null || string.isBlank()) return initialValue;
                    try {
                        String clean = string.replaceAll("[^0-9\\-]", "");
                        return Long.parseLong(clean);
                    } catch (Exception e) {
                        return initialValue;
                    }
                }
            });
            setValue(initialValue);
        }

        /**
         * Decrement.
         *
         * @param steps the steps parameter (int)
         */
        @Override
        public void decrement(int steps) {
            long current = getValue() != null ? getValue() : min;
            long newValue = Math.max(min, current - (long) steps * step);
            setValue(newValue);
        }

        /**
         * Increment.
         *
         * @param steps the steps parameter (int)
         */
        @Override
        public void increment(int steps) {
            long current = getValue() != null ? getValue() : min;
            long newValue = Math.min(max, current + (long) steps * step);
            setValue(newValue);
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
        List<String> errors = new ArrayList<>();
        if (planetPanelSupplier != null && planetPanelSupplier.get() != null) {
            errors.addAll(planetPanelSupplier.get().getValidationErrors());
        }
        if (resourcePanelSupplier != null && resourcePanelSupplier.get() != null) {
            errors.addAll(resourcePanelSupplier.get().getValidationErrors());
        }
        if (startYearSpinner != null && endYearSpinner != null) {
            int startYr = startYearSpinner.getValue();
            int endYr = endYearSpinner.getValue();
            if (startYr >= endYr) {
                errors.add(I18n.getOrDefault("scenario.validation.invalid_years", "Start year must be strictly less than end year (Tab 3)."));
            }
        }
        if (initialHumanCountSpinner != null) {
            long count = initialHumanCountSpinner.getValue();
            if (count <= 0) {
                errors.add(I18n.getOrDefault("scenario.validation.invalid_pop", "Initial human population must be greater than 0 (Tab 3)."));
            }
        }
        if (radioImportDemo != null && radioImportDemo.isSelected()) {
            if (customDensityImage == null) {
                errors.add(I18n.getOrDefault("scenario.validation.missing_density_map", "Missing demographic density map in import mode (Tab 3)."));
            } else {
                org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(customDensityImage);
                if (!val.valid()) {
                    errors.add(String.format(I18n.getOrDefault("scenario.validation.invalid_density_map", "Incompatible demographic density map (Tab 3): %s"), val.message()));
                }
            }
        }
        if (cultureVectorDimSpinner != null) {
            int dims = cultureVectorDimSpinner.getValue();
            if (dims < 0 || dims > 32) {
                errors.add(I18n.getOrDefault("scenario.validation.invalid_tensor_dim", "Cultural tensor dimension must be between 0 and 32 (Tab 3)."));
            }
        }
        int dimsCount = cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null ? cultureVectorDimSpinner.getValue() : 9;
        for (int i = 0; i < dimsCount; i++) {
            RadioButton importRadio = tensorImportRadios.get(i);
            if (importRadio != null && importRadio.isSelected()) {
                Image img = customTensorImages.get(i);
                if (img == null) {
                    String tensorName = getCulturalTensorTitle(i);
                    errors.add(String.format(I18n.getOrDefault("scenario.validation.missing_tensor_map", "Missing external map for %s in import mode (Tab 3)."), tensorName));
                } else {
                    org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(img);
                    if (!val.valid()) {
                        String tensorName = getCulturalTensorTitle(i);
                        errors.add(String.format(I18n.getOrDefault("scenario.validation.invalid_tensor_map", "Incompatible or unreadable map for %s (Tab 3): %s"), tensorName, val.message()));
                    }
                }
            }
        }
        return errors;
    }

    /**
     * Validate scenario setup.
     *
     * @return the resulting computation or state reference
     */
    public boolean validateScenarioSetup() {
        List<String> errors = getScenarioValidationErrors();
        boolean isValid = errors.isEmpty();

        // Control border highlights
        if (startYearSpinner != null && endYearSpinner != null) {
            int startYr = startYearSpinner.getValue();
            int endYr = endYearSpinner.getValue();
            if (startYr >= endYr) {
                startYearSpinner.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
                endYearSpinner.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            } else {
                startYearSpinner.setStyle("");
                endYearSpinner.setStyle("");
            }
        }
        if (initialHumanCountSpinner != null) {
            long count = initialHumanCountSpinner.getValue();
            if (count <= 0) {
                initialHumanCountSpinner.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            } else {
                initialHumanCountSpinner.setStyle("");
            }
        }
        if (radioImportDemo != null && radioImportDemo.isSelected()) {
            if (customDensityImage == null || !org.ether.society.data.ImageMapLoader.validateMapImage(customDensityImage).valid()) {
                if (loadDensityMapBtn != null) loadDensityMapBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            } else if (loadDensityMapBtn != null) {
                loadDensityMapBtn.setStyle("");
            }
        } else if (loadDensityMapBtn != null) {
            loadDensityMapBtn.setStyle("");
        }
        if (cultureVectorDimSpinner != null) {
            int dims = cultureVectorDimSpinner.getValue();
            if (dims < 0 || dims > 32) {
                cultureVectorDimSpinner.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            } else {
                cultureVectorDimSpinner.setStyle("");
            }
        }
        int dimsCount = cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null ? cultureVectorDimSpinner.getValue() : 9;
        for (int i = 0; i < dimsCount; i++) {
            RadioButton importRadio = tensorImportRadios.get(i);
            Button loadBtn = tensorLoadBtns.get(i);
            if (importRadio != null && importRadio.isSelected()) {
                Image img = customTensorImages.get(i);
                if (img == null || !org.ether.society.data.ImageMapLoader.validateMapImage(img).valid()) {
                    if (loadBtn != null) loadBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
                } else if (loadBtn != null) {
                    loadBtn.setStyle("");
                }
            } else if (loadBtn != null) {
                loadBtn.setStyle("");
            }
        }

        if (!isValid) {
            StringBuilder sb = new StringBuilder();
            // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
            for (String err : errors) {
                sb.append("• ").append(err).append("\n");
            }
            if (validationErrorLabel != null && validationErrorBanner != null) {
                if (validationBannerHeaderLabel != null) {
                    validationBannerHeaderLabel.setText("🛑 " + I18n.getOrDefault("scenario.validation.header_errors", "ERREURS DE CONFIGURATION DU SCÉNARIO (ONGLET 3) :"));
                    validationBannerHeaderLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 13px;");
                }
                validationErrorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
                validationErrorBanner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10;");
                validationErrorLabel.setText(sb.toString().trim());
                validationErrorBanner.setVisible(true);
                validationErrorBanner.setManaged(true);
            }
            if (configScroll != null) {
                configScroll.setVvalue(0.0);
            }
        } else {
            if (validationErrorBanner != null) {
                validationErrorBanner.setVisible(false);
                validationErrorBanner.setManaged(false);
            }
        }

        updateLiveDiagnosticBlock();
        return isValid;
    }

    private static class EngineRowItem {
        final String engineId;
        final String title;
        final long yearBP;
        final int defaultIndex;
        final VBox containerNode;

        EngineRowItem(String engineId, String title, long yearBP, int defaultIndex, VBox containerNode) {
            this.engineId = engineId;
            this.title = title;
            this.yearBP = yearBP;
            this.defaultIndex = defaultIndex;
            this.containerNode = containerNode;
        }
    }

    private static long parseYearBPFromTitle(String title) {
        if (title == null) return Long.MAX_VALUE;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\((-?\\d+[\\d\\s]*)\\s*(BP|CE|BC|av\\. J\\.-C\\.)?\\)").matcher(title);
        if (matcher.find()) {
            String numStr = matcher.group(1).replaceAll("\\s+", "");
            try {
                long val = Long.parseLong(numStr);
                String unit = matcher.group(2);
                if ("BP".equalsIgnoreCase(unit)) {
                    return val < 0 ? val : -val;
                } else if ("CE".equalsIgnoreCase(unit)) {
                    return val;
                }
                return val;
            } catch (NumberFormatException ignored) {}
        }
        return Long.MAX_VALUE;
    }

    /**
     * Opens the interactive Time-Travel & Automatic Epoch Scenario generation dialog.
     */
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
