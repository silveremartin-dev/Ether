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
    private final java.util.Map<Integer, Image> customTensorImages = new java.util.HashMap<>();
    private final java.util.Map<Integer, String> customTensorNames = new java.util.HashMap<>();
    private VBox layersDynamicContainer;
    private Button culturalHelpBtn;

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

        // --- 2. Demographics & Density Map Management (RadioButtons) ---
        VBox popSection = new VBox(10);
        popSection.getStyleClass().add("card-section");
        popHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_section", "👥 2. INITIAL POPULATION IDENTITY CARD"));
        popHeader.getStyleClass().add("label-section-header");
        popHeader.setId("__popHeader");

        ToggleGroup demoSourceGroup = new ToggleGroup();
        radioProcDemo = new RadioButton(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.procedural", "▶ Procedural Mode (Algorithms & Demographic Patterns)"));
        radioImportDemo = new RadioButton(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.import", "📂 Loading Mode for Existing Density Map (PNG)"));
        radioProcDemo.setToggleGroup(demoSourceGroup);
        radioImportDemo.setToggleGroup(demoSourceGroup);
        radioProcDemo.setSelected(true);

        Label demoTypeLabel = new Label(I18n.getOrDefault("scenario.label.demographic_mode", "⚙ Demographic Generation Mode:"));
        demoTypeLabel.setStyle("-fx-font-weight: bold;");

        VBox demoProcBox = new VBox(8);
        demoProcBox.setStyle("-fx-padding: 8; -fx-background-color: rgba(56, 189, 248, 0.04); -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.15); -fx-border-radius: 6;");

        initialHumanCountSpinner = new Spinner<>(new LongSpinnerValueFactory(1_000L, 10_000_000_000L, 1_000_000L, 100_000L));
        initialHumanCountSpinner.setEditable(true);
        initialHumanCountSpinner.setMaxWidth(Double.MAX_VALUE);
        initialHumanCountSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            notifyParamChange();
            if (!isUpdatingFromPreset && newV != null && currentPreviewCells != null && !currentPreviewCells.isEmpty()) {
                distributeInitialPopulation(currentPreviewCells);
                drawPreview();
            }
        });

        densityPatternCombo = new ComboBox<>();
        densityPatternCombo.getItems().addAll("UNBIASED_NATURAL", "COASTAL_MARITIME", "RIVER_VALLEYS", "HIGHLAND_MOUNTAIN", "INLAND_OASIS", "EQUATORIAL_BELT", "URBAN_CLUSTERS", "SPARSE_NOMADIC", "UNIFORM", "RANDOM");
        densityPatternCombo.setValue("UNBIASED_NATURAL");
        densityPatternCombo.setMaxWidth(Double.MAX_VALUE);
        densityPatternCombo.setConverter(new javafx.util.StringConverter<String>() {
            /**
             * To string.
             *
             * @param item the item parameter (String)
             * @return the resulting computation or state reference
             */
            @Override
            public String toString(String item) {
                return item == null ? "" : org.ether.society.i18n.I18n.getOrDefault("scenario.density." + item, DENSITY_LABELS.getOrDefault(item, item));
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
        densityPatternCombo.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(org.ether.society.i18n.I18n.getOrDefault("scenario.density." + item, DENSITY_LABELS.getOrDefault(item, item)));
                    String desc = org.ether.society.i18n.I18n.getOrDefault("scenario.density.desc." + item, DENSITY_DESCRIPTIONS.getOrDefault(item, ""));
                    if (!desc.isBlank()) {
                        setTooltip(new Tooltip(desc));
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });
        densityPatternCombo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(org.ether.society.i18n.I18n.getOrDefault("scenario.density." + item, DENSITY_LABELS.getOrDefault(item, item)));
                }
            }
        });
        densityPatternCombo.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.density.desc.UNBIASED_NATURAL", DENSITY_DESCRIPTIONS.get("UNBIASED_NATURAL"))));
        densityPatternCombo.valueProperty().addListener((obs, oldV, newV) -> {
            notifyParamChange();
            if (newV != null) {
                densityPatternCombo.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.density.desc." + newV, DENSITY_DESCRIPTIONS.getOrDefault(newV, ""))));
            }
        });

        popCountLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_count", "Initial Population (1,000 to 10,000,000,000):"));
        densityPatternLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.density_pattern", "Distribution Pattern:"));

        GridPane popGrid = new GridPane();
        popGrid.setHgap(10);
        popGrid.setVgap(10);
        ColumnConstraints pCol1 = new ColumnConstraints();
        pCol1.setPercentWidth(45);
        ColumnConstraints pCol2 = new ColumnConstraints();
        pCol2.setPercentWidth(55);
        popGrid.getColumnConstraints().setAll(pCol1, pCol2);

        techPresetCombo = new ComboBox<>();
        techPresetCombo.getItems().setAll(Scenario.TechPreset.values());
        techPresetCombo.setValue(Scenario.TechPreset.AUTO_FROM_YEAR);
        techPresetCombo.setMaxWidth(Double.MAX_VALUE);
        techPresetCombo.setConverter(new javafx.util.StringConverter<Scenario.TechPreset>() {
            /**
             * To string.
             *
             * @param item the item parameter (Scenario.TechPreset)
             * @return the resulting computation or state reference
             */
            @Override
            public String toString(Scenario.TechPreset item) {
                return item == null ? "" : I18n.getOrDefault("scenario.tech." + item.name(), item.getLabel());
            }
            /**
             * From string.
             *
             * @param string the string parameter (String)
             * @return the resulting computation or state reference
             */
            @Override
            public Scenario.TechPreset fromString(String string) {
                return null;
            }
        });
        techPresetCombo.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.tech_preset", "Select initial physical endowment (K₀, E₀, F₀, I₀) or leave auto-calculated based on T₀ year.")));

        Label techPresetLabel = new Label(I18n.getOrDefault("scenario.label.tech_preset", "🚀 Dotation & Niveau Technologique T₀ :"));
        techPresetLabel.setTooltip(techPresetCombo.getTooltip());

        popGrid.addRow(0, popCountLabel, initialHumanCountSpinner);
        popGrid.addRow(1, densityPatternLabel, densityPatternCombo);
        popGrid.addRow(2, techPresetLabel, techPresetCombo);

        demoSeedField = new TextField("12345");
        demoSeedField.setPrefWidth(120);
        demoSeedField.textProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        demoRandSeedBtn = new Button("🎲");
        demoRandSeedBtn.getStyleClass().add("button-secondary");
        demoRandSeedBtn.setOnAction(e -> {
            notifyParamChange();
            demoSeedField.setText(String.valueOf(new Random().nextLong(1000000)));
            if (currentPreviewCells != null) {
                distributeInitialPopulation(currentPreviewCells);
                drawPreview();
            }
        });
        demoRandSeedBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.seed.tooltip", "Generate new random seed for demographics.")));
        HBox demoSeedBox = new HBox(5, demoSeedField, demoRandSeedBtn);
        HBox.setHgrow(demoSeedField, Priority.ALWAYS);

        Label demoSeedLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.seed.label", "🎲 Scenario Procedural Seed:"));
        popGrid.addRow(3, demoSeedLabel, demoSeedBox);

        // Custom Physical Stocks Sub-Panel (Visible only when CUSTOM tech preset selected)
        customCapitalSpinner = new Spinner<>(0.0, 1_000_000.0, 100.0, 10.0);
        customCapitalSpinner.setEditable(true);
        customCapitalSpinner.setMaxWidth(Double.MAX_VALUE);
        customCapitalSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        customEnergySpinner = new Spinner<>(0.0, 10_000_000.0, 300.0, 50.0);
        customEnergySpinner.setEditable(true);
        customEnergySpinner.setMaxWidth(Double.MAX_VALUE);
        customEnergySpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        customFoodSpinner = new Spinner<>(0.0, 120.0, 6.0, 1.0);
        customFoodSpinner.setEditable(true);
        customFoodSpinner.setMaxWidth(Double.MAX_VALUE);
        customFoodSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        customInfoSpinner = new Spinner<>(0.0, 100_000_000_000.0, 2000.0, 100.0);
        customInfoSpinner.setEditable(true);
        customInfoSpinner.setMaxWidth(Double.MAX_VALUE);
        customInfoSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        GridPane customGrid = new GridPane();
        customGrid.setHgap(8);
        customGrid.setVgap(8);
        ColumnConstraints cgCol1 = new ColumnConstraints();
        cgCol1.setPercentWidth(25);
        ColumnConstraints cgCol2 = new ColumnConstraints();
        cgCol2.setPercentWidth(25);
        ColumnConstraints cgCol3 = new ColumnConstraints();
        cgCol3.setPercentWidth(25);
        ColumnConstraints cgCol4 = new ColumnConstraints();
        cgCol4.setPercentWidth(25);
        customGrid.getColumnConstraints().setAll(cgCol1, cgCol2, cgCol3, cgCol4);

        customGrid.addRow(0, new Label(I18n.getOrDefault("scenario.label.stock_k0", "ðŸ› ï¸ Kâ‚€ (kg/hab) :")), customCapitalSpinner, new Label(I18n.getOrDefault("scenario.label.stock_e0", "⚡ Eâ‚€ (MJ/hab) :")), customEnergySpinner);
        customGrid.addRow(1, new Label(I18n.getOrDefault("scenario.label.stock_f0", "🌾 F₀ (mois) :")), customFoodSpinner, new Label(I18n.getOrDefault("scenario.label.stock_i0", "🧠 I₀ (bits/hab) :")), customInfoSpinner);

        Label customTitleLabel = new Label(I18n.getOrDefault("scenario.section.custom_stocks", "⚙️ï¸ MANUAL INITIAL PHYSICAL STOCKS SETUP (CUSTOM):"));
        customTitleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");

        customPhysicalSubPanel = new VBox(6, customTitleLabel, customGrid);
        customPhysicalSubPanel.setStyle("-fx-padding: 8px; -fx-background-color: rgba(245, 158, 11, 0.05); -fx-background-radius: 6px; -fx-border-color: rgba(245, 158, 11, 0.25); -fx-border-radius: 6px; -fx-border-width: 1px;");
        customPhysicalSubPanel.setVisible(false);
        customPhysicalSubPanel.setManaged(false);

        techPresetCombo.valueProperty().addListener((obs, oldV, newV) -> {
            notifyParamChange();
            boolean isCustom = (newV == Scenario.TechPreset.CUSTOM);
            customPhysicalSubPanel.setVisible(isCustom);
            customPhysicalSubPanel.setManaged(isCustom);
        });

        exportDensityMapBtn = new Button(I18n.getOrDefault("scenario.btn.export_map_png", "📤 Export Raster (PNG / JPEG)"));
        exportDensityMapBtn.getStyleClass().add("button-secondary");
        exportDensityMapBtn.setMaxWidth(Double.MAX_VALUE);
        exportDensityMapBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_map_png", "Export demographic density map as a high-resolution PNG or JPEG raster file.")));
        exportDensityMapBtn.setOnAction(e -> exportDensityMap());

        HBox demoBtnBar = new HBox(8, exportDensityMapBtn);
        HBox.setHgrow(exportDensityMapBtn, Priority.ALWAYS);

        VBox proceduralDemoPanel = new VBox(8, popGrid, customPhysicalSubPanel, demoBtnBar);
        proceduralDemoPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");

        // Import Panel — section labels to match tensor block format
        Label sourceRefLabel = new Label("ðŸ“ " + I18n.getOrDefault("resource.label.reference_source", "Source de Référence :"));
        sourceRefLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");
        demoSourceLabel = sourceRefLabel;
        demoSourceCombo = buildPopulationSourceCombo();

        densityMapFileLabel = new Label(org.ether.society.i18n.I18n.get("planet.map.none"));
        densityMapFileLabel.getStyleClass().add("value-label");
        densityMapFileLabel.setStyle("-fx-font-size: 10px;");

        demoCompatibilityLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.status.terrain_no_map", "ðŸª Terrain validation: No external map loaded"));
        demoCompatibilityLabel.getStyleClass().add("subcard-status-muted");
        demoCompatibilityLabel.setWrapText(true);
        demoCompatibilityLabel.setStyle("-fx-font-size: 11px;");

        densityFormatHintLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.format.density_hint",
                "PNG / JPEG (projection équirectangulaire 2:1) :\n  Noir (0) = 0 hab/km² | Blanc (255) = Densité maximale d'habitation."));
        densityFormatHintLabel.setWrapText(true);
        densityFormatHintLabel.getStyleClass().add("card-description-muted");
        densityFormatHintLabel.setStyle("-fx-font-size: 9px; -fx-font-style: italic;");

        loadDensityMapBtn = new Button(I18n.getOrDefault("resource.btn.load_map", "📂 Charger la Carte"));
        loadDensityMapBtn.getStyleClass().add("button-secondary");
        loadDensityMapBtn.setStyle("-fx-font-size: 11px;");
        loadDensityMapBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.import_density_map", "Import an external demographic density map in 2:1 equirectangular PNG format.")));
        loadDensityMapBtn.setOnAction(e -> {
            notifyParamChange();
            loadCustomDensityMap();
        });

        clearDensityMapBtn = new Button("❌");
        clearDensityMapBtn.getStyleClass().add("button-secondary");
        clearDensityMapBtn.setStyle("-fx-font-size: 11px;");
        clearDensityMapBtn.setOnAction(e -> {
            customDensityImage = null;
            densityMapFileLabel.setText("—");
            if (radioProcDemo != null) {
                radioProcDemo.setSelected(true);
            }
            notifyParamChange();
            drawPreview();
        });

        demoHelpBtn = new Button(I18n.getOrDefault("scenario.btn.format_specs", "â“ Format"));
        demoHelpBtn.getStyleClass().add("button-secondary");
        demoHelpBtn.setStyle("-fx-font-size: 11px;");
        demoHelpBtn.setMinWidth(Region.USE_PREF_SIZE);
        demoHelpBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.density_specs", "Display instructions and image format specifications.")));
        demoHelpBtn.setOnAction(e -> showDensityImportFormatHelp());

        Label loadMapSectionLabel = new Label("📂 " + I18n.getOrDefault("scenario.label.load_map_section", "Charger la Carte :"));
        loadMapSectionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");
        Label formatSectionLabel = new Label("ðŸ“ " + I18n.getOrDefault("scenario.label.format_section", "Format de Carte :"));
        formatSectionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");

        HBox mapBtnBox = new HBox(6, loadDensityMapBtn, clearDensityMapBtn, demoHelpBtn);
        mapBtnBox.setAlignment(Pos.CENTER_LEFT);

        VBox importDemoPanel = new VBox(6, sourceRefLabel, demoSourceCombo, loadMapSectionLabel, mapBtnBox, densityMapFileLabel, demoCompatibilityLabel, formatSectionLabel, densityFormatHintLabel);
        importDemoPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(167,139,250,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        importDemoPanel.setVisible(false);
        importDemoPanel.setManaged(false);

        // Toggle listener
        demoSourceGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            notifyParamChange();
            boolean isProc = sel == radioProcDemo;
            proceduralDemoPanel.setVisible(isProc);
            proceduralDemoPanel.setManaged(isProc);
            importDemoPanel.setVisible(!isProc);
            importDemoPanel.setManaged(!isProc);
            if (previewModeCombo != null && previewModeCombo.getSelectionModel().getSelectedIndex() != 0) {
                previewModeCombo.getSelectionModel().select(0);
            }
            if (currentPreviewCells != null) {
                distributeInitialPopulation(currentPreviewCells);
            }
            drawPreview();
        });

        // Live preview listeners
        initialHumanCountSpinner.valueProperty().addListener((obs, oldV, newV) -> { notifyParamChange(); if (currentPreviewCells != null) { distributeInitialPopulation(currentPreviewCells); drawPreview(); } });
        densityPatternCombo.valueProperty().addListener((obs, oldV, newV) -> { notifyParamChange(); if (currentPreviewCells != null) { distributeInitialPopulation(currentPreviewCells); drawPreview(); } });

        popSection.getChildren().addAll(popHeader, radioProcDemo, proceduralDemoPanel, radioImportDemo, importDemoPanel);

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

    private static org.ether.society.engines.compiler.EngineConflictReport cachedJitReport = null;
    private static synchronized org.ether.society.engines.compiler.EngineConflictReport getOrCreateJitReport() {
        if (cachedJitReport == null) {
            org.ether.society.engines.compiler.ScenarioEngineJITCompiler jit = new org.ether.society.engines.compiler.ScenarioEngineJITCompiler();
            jit.registerEngineStep("BiologicalDemographics", "biomassHuman", new org.ether.society.engines.compiler.SymbolicExpression("biomassHuman", 1.01, 0.0), 10000.0);
            jit.registerEngineStep("EcologicalDegradation", "biomassHuman", new org.ether.society.engines.compiler.SymbolicExpression("biomassHuman", 0.995, 0.0), 10000.0);
            jit.registerEngineStep("AtmosphericSolar", "temperature", new org.ether.society.engines.compiler.SymbolicExpression("temperature", 1.0, 0.02), 25.0);
            jit.compile();
            cachedJitReport = jit.getConflictReport();
        }
        return cachedJitReport;
    }

    private void renderDiagnosticCategory(VBox container, String title, List<String> items) {
        if (items == null || items.isEmpty()) return;
        Label catLabel = new Label(title);
        catLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #38bdf8; -fx-padding: 6 0 2 0;");
        container.getChildren().add(catLabel);
        for (String item : items) {
            Label lbl = new Label("  • " + item);
            lbl.setWrapText(true);
            if (item.startsWith("❌")) {
                lbl.getStyleClass().add("diagnostic-error");
                lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            } else if (item.startsWith("⚠️ï¸")) {
                lbl.getStyleClass().add("diagnostic-warn");
                lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #f59e0b;");
            } else {
                lbl.getStyleClass().add("diagnostic-pass");
                lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981;");
            }
            container.getChildren().add(lbl);
        }
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
            if (s.getCulturalDiffusionRate() > 0 && culturalDiffusionRateSpinner != null && culturalDiffusionRateSpinner.getValueFactory() != null) {
                culturalDiffusionRateSpinner.getValueFactory().setValue(s.getCulturalDiffusionRate());
            }
            if (s.getCulturalMutationRate() > 0 && culturalMutationRateSpinner != null && culturalMutationRateSpinner.getValueFactory() != null) {
                culturalMutationRateSpinner.getValueFactory().setValue(s.getCulturalMutationRate());
            }
            int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
            customTensorImages.clear();
            customTensorNames.clear();
            if (s.getCustomTensorNames() != null) {
                customTensorNames.putAll(s.getCustomTensorNames());
            }
            customIsoglossImage = null;
            customKinshipImage = null;
            customRitualsImage = null;
            customSovereigntyImage = null;

            for (int i = 0; i < dims; i++) {
                String b64 = s.getCustomTensorMapBase64(i);
                if (b64 != null && !b64.isBlank()) {
                    Image img = org.ether.society.data.ImageMapLoader.base64PngToImage(b64);
                    if (img != null) {
                        customTensorImages.put(i, img);
                        if (i == 0) customIsoglossImage = img;
                        if (i == 1) customKinshipImage = img;
                        if (i == 2) customRitualsImage = img;
                        if (i == 3) customSovereigntyImage = img;
                    }
                }
            }
            rebuildCulturalTensorSubBlocks(dims);

            if (s.getTensorSeeds() != null) {
                for (java.util.Map.Entry<Integer, Long> entry : s.getTensorSeeds().entrySet()) {
                    int tIdx = entry.getKey();
                    if (tensorSeedFields.containsKey(tIdx) && tensorSeedFields.get(tIdx) != null) {
                        tensorSeedFields.get(tIdx).setText(String.valueOf(entry.getValue()));
                    }
                }
            }
            if (s.getTensorProceduralParameters() != null) {
                // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
                for (java.util.Map.Entry<Integer, java.util.Map<String, Double>> entry : s.getTensorProceduralParameters().entrySet()) {
                    int tIdx = entry.getKey();
                    java.util.Map<String, Double> pMap = entry.getValue();
                    if (pMap != null) {
                        TensorParamDescriptor d1 = getTensorParam1Descriptor(tIdx);
                        TensorParamDescriptor d2 = getTensorParam2Descriptor(tIdx);
                        TensorParamDescriptor d3 = getTensorParam3Descriptor(tIdx);
                        if (pMap.containsKey(d1.labelKey) && tensorParam1Sliders.containsKey(tIdx) && tensorParam1Sliders.get(tIdx) != null) {
                            tensorParam1Sliders.get(tIdx).setValue(pMap.get(d1.labelKey));
                        }
                        if (pMap.containsKey(d2.labelKey) && tensorParam2Sliders.containsKey(tIdx) && tensorParam2Sliders.get(tIdx) != null) {
                            tensorParam2Sliders.get(tIdx).setValue(pMap.get(d2.labelKey));
                        }
                        if (pMap.containsKey(d3.labelKey) && tensorParam3Sliders.containsKey(tIdx) && tensorParam3Sliders.get(tIdx) != null) {
                            tensorParam3Sliders.get(tIdx).setValue(pMap.get(d3.labelKey));
                        }
                    }
                }
            }
            for (int i = 0; i < dims; i++) {
                boolean hasCustomImage = customTensorImages.containsKey(i) && customTensorImages.get(i) != null;
                boolean isProc = true;
                if (s.getTensorProceduralModes() != null && i < s.getTensorProceduralModes().size()) {
                    isProc = s.getTensorProceduralModes().get(i);
                    if (!isProc && !hasCustomImage) {
                        isProc = true;
                    }
                } else {
                    isProc = !hasCustomImage;
                }
                if (isProc && tensorProcRadios.containsKey(i)) {
                    tensorProcRadios.get(i).setSelected(true);
                } else if (!isProc && tensorImportRadios.containsKey(i)) {
                    tensorImportRadios.get(i).setSelected(true);
                }
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

    private final java.util.Map<Integer, RadioButton> tensorProcRadios = new java.util.HashMap<>();
    private final java.util.Map<Integer, RadioButton> tensorImportRadios = new java.util.HashMap<>();
    private final java.util.Map<Integer, Label> tensorSubTitles = new java.util.HashMap<>();
    private final java.util.Map<Integer, Label> tensorFileLabels = new java.util.HashMap<>();
    private final java.util.Map<Integer, Label> tensorSourceLabels = new java.util.HashMap<>();
    private final java.util.Map<Integer, ComboBox<String>> tensorSourceCombos = new java.util.HashMap<>();
    private final java.util.Map<Integer, Button> tensorLoadBtns = new java.util.HashMap<>();
    private final java.util.Map<Integer, Label> tensorFormatLabels = new java.util.HashMap<>();
    private final java.util.Map<Integer, TextField> tensorSeedFields = new java.util.HashMap<>();
    private final java.util.Map<Integer, Slider> tensorParam1Sliders = new java.util.HashMap<>();
    private final java.util.Map<Integer, Slider> tensorParam2Sliders = new java.util.HashMap<>();
    private final java.util.Map<Integer, Slider> tensorParam3Sliders = new java.util.HashMap<>();
    private final java.util.Map<Integer, Label> tensorParam1ValueLabels = new java.util.HashMap<>();
    private final java.util.Map<Integer, Label> tensorParam2ValueLabels = new java.util.HashMap<>();
    private final java.util.Map<Integer, Label> tensorParam3ValueLabels = new java.util.HashMap<>();
    private final java.util.Map<Integer, Button> tensorGenSingleBtns = new java.util.HashMap<>();

    private VBox createCulturalVectorAndLayersSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        cultureHeader = new Label(I18n.getOrDefault("scenario.culture_section", "🧠 3. CULTURAL VECTOR DIMENSION & MULTI-FIELD LAYERS"));
        cultureHeader.getStyleClass().add("label-section-header");

        Label desc = new Label(I18n.getOrDefault("scenario.header.cultural_tensor", "3.1 Cultural Tensor Kernel & Langevin-SDE (Diffusion & Mutation):\nConfiguration of N-dimensional information tensor and import/generation of cartographic layers across all cultural dimensions."));
        desc.setStyle("-fx-font-size: 11px; -fx-font-weight: normal;");
        desc.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        ColumnConstraints cg1 = new ColumnConstraints();
        cg1.setPercentWidth(55);
        ColumnConstraints cg2 = new ColumnConstraints();
        cg2.setPercentWidth(45);
        grid.getColumnConstraints().setAll(cg1, cg2);

        // 1. Vector Dimension (M)
        Label dimLbl = new Label(I18n.getOrDefault("scenario.label.cultural_dim", "Taille Tenseur Culturel (M dims) :"));
        dimLbl.getStyleClass().add("control-label");
        cultureVectorDimSpinner = new Spinner<>(0, 32, 9, 1);
        cultureVectorDimSpinner.setEditable(true);
        cultureVectorDimSpinner.setMaxWidth(Double.MAX_VALUE);
        cultureVectorDimSpinner.valueProperty().addListener((obs, o, n) -> {
            notifyParamChange();
            if (n != null) {
                rebuildCulturalTensorSubBlocks(n);
                updatePreviewModesCombo();
                drawPreview();
            }
        });
        Tooltip.install(cultureVectorDimSpinner, new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_dim", "Nombre de composantes N-dimensionnelles du vecteur d'information culturelle (ex: 9D, 16D, 32D).")));

        // 2. Cultural Diffusion Rate
        Label diffLbl = new Label(I18n.getOrDefault("scenario.label.cultural_diffusion", "Cultural Diffusion Conductivity (α):"));
        diffLbl.getStyleClass().add("control-label");
        culturalDiffusionRateSpinner = new Spinner<>(0.001, 0.50, 0.05, 0.01);
        culturalDiffusionRateSpinner.setEditable(true);
        culturalDiffusionRateSpinner.setMaxWidth(Double.MAX_VALUE);
        culturalDiffusionRateSpinner.valueProperty().addListener((obs, o, n) -> notifyParamChange());
        Tooltip.install(culturalDiffusionRateSpinner, new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_diffusion", "Diffusion rate of association energy between neighboring H3 hexagons.")));

        // 3. Cultural Mutation Noise Rate
        Label mutLbl = new Label(I18n.getOrDefault("scenario.label.cultural_mutation", "Mutation / Drift Noise (1/√N):"));
        mutLbl.getStyleClass().add("control-label");
        culturalMutationRateSpinner = new Spinner<>(0.001, 0.10, 0.01, 0.005);
        culturalMutationRateSpinner.setEditable(true);
        culturalMutationRateSpinner.setMaxWidth(Double.MAX_VALUE);
        culturalMutationRateSpinner.valueProperty().addListener((obs, o, n) -> notifyParamChange());
        Tooltip.install(culturalMutationRateSpinner, new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_mutation", "Langevin drift factor (stochastic innovation background noise).")));

        grid.add(dimLbl, 0, 0); grid.add(cultureVectorDimSpinner, 1, 0);
        grid.add(diffLbl, 0, 1); grid.add(culturalDiffusionRateSpinner, 1, 1);
        grid.add(mutLbl, 0, 2); grid.add(culturalMutationRateSpinner, 1, 2);

        // Layer Import Panel
        VBox layersPanel = new VBox(8);
        layersPanel.getStyleClass().add("layers-panel-card");

        Label layerTitle = new Label(I18n.getOrDefault("scenario.header.tensor_subblocks", "ðŸ—ºï¸ 3.2 Cartographic Sub-Blocks per Tensor (Procedural Generation / Imported Maps)"));
        layerTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        layerTitle.setWrapText(true);

        btnGenerateProceduralTensorsSection = new Button(I18n.getOrDefault("scenario.btn.regen_tensors", "🪄 Régénérer les Tenseurs"));
        btnGenerateProceduralTensorsSection.getStyleClass().add("button");
        btnGenerateProceduralTensorsSection.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnGenerateProceduralTensorsSection.setMinWidth(Region.USE_PREF_SIZE);
        btnGenerateProceduralTensorsSection.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.regen_tensors", "Bascule tous les tenseurs en mode procédural et régénère les cartes selon les paramètres et la graine stochastique.")));
        btnGenerateProceduralTensorsSection.setOnAction(e -> generateProceduralCulturalTensors());

        cultSeedField = new TextField("54321");
        cultSeedField.setPrefWidth(80);
        cultSeedField.setStyle("-fx-font-size: 11px;");
        cultSeedField.textProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        Button cultRandSeedBtn = new Button("🎲");
        cultRandSeedBtn.getStyleClass().add("button-secondary");
        cultRandSeedBtn.setStyle("-fx-font-size: 11px;");
        cultRandSeedBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.random_tensor_seed", "Tirer une nouvelle graine stochastique aléatoire pour les tenseurs culturels (sans affecter la densité de population).")));

        cultRandSeedBtn.setOnAction(e -> {
            notifyParamChange();
            String newSeed = String.valueOf(new java.util.Random().nextLong(1000000));
            cultSeedField.setText(newSeed);
            generateProceduralCulturalTensors();
        });

        culturalHelpBtn = new Button(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.cultural_format_help", "â“ Format Calques"));
        culturalHelpBtn.getStyleClass().add("button-secondary");
        culturalHelpBtn.setStyle("-fx-font-size: 11px;");
        culturalHelpBtn.setMinWidth(Region.USE_PREF_SIZE);
        culturalHelpBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_specs", "Specifications of image formats and cartographic data for cultural and geopolitical layer import.")));
        culturalHelpBtn.setOnAction(e -> showCulturalImportFormatHelp());

        HBox cultSeedBox = new HBox(4, new Label(I18n.getOrDefault("scenario.label.cultural_seed", "Graine :")), new Label("🎲"), cultSeedField, cultRandSeedBtn);
        cultSeedBox.setAlignment(Pos.CENTER_LEFT);

        HBox seedRow = new HBox(8, cultSeedBox, btnGenerateProceduralTensorsSection);
        seedRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(btnGenerateProceduralTensorsSection, Priority.ALWAYS);

        Button btnExportGisMultiFormat = new Button(I18n.getOrDefault("scenario.btn.export_gis", "ðŸ—ºï¸ Export SIG Multi-Format"));
        btnExportGisMultiFormat.getStyleClass().add("button-secondary");
        btnExportGisMultiFormat.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportGisMultiFormat.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_gis", "Export scenario density layers and 9 cultural tensors under professional GIS formats (GeoTIFF, NetCDF-4, GeoJSON).")));
        btnExportGisMultiFormat.setOnAction(e -> exportGisMultiFormat());

        Button btnExportProvenanceManifest = new Button(I18n.getOrDefault("scenario.btn.provenance_manifest", "🔒 Manifeste Provenance SHA-256"));
        btnExportProvenanceManifest.getStyleClass().add("button-secondary");
        btnExportProvenanceManifest.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportProvenanceManifest.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.gen_provenance", "Generate a cryptographic JSON manifest (provenance.json) containing SHA-256 hashes of datasets and parameters.")));
        btnExportProvenanceManifest.setOnAction(e -> exportProvenanceManifest());

        Button btnCulturalMatrix = new Button(I18n.getOrDefault("scenario.btn.cultural_matrix_inspect", "🧩 Inspecteur des Affinités Culturelles"));
        btnCulturalMatrix.getStyleClass().add("button-secondary");
        btnCulturalMatrix.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnCulturalMatrix.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_matrix", "Inspect the N × N Pairwise Cultural Affinity and Distance Heatmap Matrix.")));
        btnCulturalMatrix.setOnAction(e -> {
            long epoch = startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -100000L;
            new CulturalAffinityMatrixDialog(epoch, getScenario()).show();
        });

        HBox actionButtonsRow = new HBox(8, culturalHelpBtn, btnExportGisMultiFormat, btnExportProvenanceManifest);
        actionButtonsRow.setAlignment(Pos.CENTER_LEFT);

        VBox matrixCard = new VBox(4);
        matrixCard.setStyle("-fx-padding: 8px 10px; -fx-background-color: rgba(30, 41, 59, 0.65); -fx-background-radius: 6px; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 6px; -fx-border-width: 1px;");
        Label matrixTitle = new Label(I18n.getOrDefault("scenario.cultural_matrix.title", "Matrice d'Affinité & Distances Culturelles"));
        matrixTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #f8fafc;");
        Label matrixDesc = new Label(I18n.getOrDefault("scenario.cultural_matrix.desc", "Inspecter et éditer la matrice N×N des distances de Mahalanobis et coefficients d'assimilation entre entités culturelles."));
        matrixDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");
        matrixDesc.setWrapText(true);
        matrixCard.getChildren().addAll(matrixTitle, matrixDesc, btnCulturalMatrix);

        VBox seedAndActionBox = new VBox(8, seedRow, actionButtonsRow, matrixCard);

        VBox layerHeaderBox = new VBox(6, layerTitle, seedAndActionBox);

        Label culturalFormatHintLabel = new Label(I18n.getOrDefault("scenario.desc.tensor_formats", "PNG / JPEG (2:1 equirectangular projection):\n  Each sub-block below allows individual choice of parameterized procedural generation or map import. In map import mode, a valid map file must be loaded."));
        culturalFormatHintLabel.setWrapText(true);
        culturalFormatHintLabel.getStyleClass().add("hint-label");

        layersDynamicContainer = new VBox(10);
        rebuildCulturalTensorSubBlocks(cultureVectorDimSpinner.getValue());

        layersPanel.getChildren().addAll(layerHeaderBox, culturalFormatHintLabel, layersDynamicContainer);
        section.getChildren().addAll(cultureHeader, desc, grid, layersPanel);
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

    /**
 * Gets the default name of a cultural tensor layer at the given index.
 *
 * @param index zero-based cultural tensor index
 * @return standard default tensor name
 */
private String getDefaultTensorName(int index) {
    return switch (index) {
        case 0 -> I18n.getOrDefault("scenario.tensor.name.0", "Isoglosses & Continua Linguistiques");
        case 1 -> I18n.getOrDefault("scenario.tensor.name.1", "Structures de Parenté & Lignages");
        case 2 -> I18n.getOrDefault("scenario.tensor.name.2", "Rituels, Croyances & Sacré");
        case 3 -> I18n.getOrDefault("scenario.tensor.name.3", "Souveraineté Politico-Militaire & Capitales");
        case 4 -> I18n.getOrDefault("scenario.tensor.name.4", "Outillage, Matérialité & Technologies");
        case 5 -> I18n.getOrDefault("scenario.tensor.name.5", "Corridors Commerciaux & Réseaux d'Échange");
        case 6 -> I18n.getOrDefault("scenario.tensor.name.6", "Complexité Institutionnelle & Normes");
        case 7 -> I18n.getOrDefault("scenario.tensor.name.7", "Empreinte Écologique & Pression Malthusienne");
        case 8 -> I18n.getOrDefault("scenario.tensor.name.8", "Immunité aux Pathogènes & Mémoire Sanitaire");
        default -> I18n.getOrDefault("scenario.tensor.name.ext", "Substrat Culturel Extensible {0}", (index + 1));
    };
}

    private String getCulturalTensorTitle(int index) {
        if (customTensorNames.containsKey(index) && !customTensorNames.get(index).isBlank()) {
            return "🧬 3.2." + (index + 1) + " " + I18n.getOrDefault("scenario.tensor.label_prefix", "Tenseur ") + (index + 1) + " : " + customTensorNames.get(index);
        }
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.title.0", "📜 3.2.1 Tensor 1: Isoglosses & Linguistic Continua (Languages)");
            case 1 -> I18n.getOrDefault("scenario.tensor.title.1", "ðŸ› 3.2.2 Tensor 2: Kinship & Clan Structures (Lineage)");
            case 2 -> I18n.getOrDefault("scenario.tensor.title.2", "🔮 3.2.3 Tensor 3: Rituals, Beliefs & Sacred (Asabiyyah)");
            case 3 -> I18n.getOrDefault("scenario.tensor.title.3", "👑 3.2.4 Tensor 4: Politico-Military Sovereignty & Capitals");
            case 4 -> I18n.getOrDefault("scenario.tensor.title.4", "ðŸº 3.2.5 Tensor 5: Tooling, Materiality & Technologies (Artifacts)");
            case 5 -> I18n.getOrDefault("scenario.tensor.title.5", "ðŸ« 3.2.6 Tensor 6: Trade Corridors & Networks (Economic Routes)");
            case 6 -> I18n.getOrDefault("scenario.tensor.title.6", "⚖ 3.2.7 Tensor 7: Institutional Complexity & Norms (Seshat & Law)");
            case 7 -> I18n.getOrDefault("scenario.tensor.title.7", "⚠️ï¸ 3.2.8 Tensor 8: Ecological Footprint & Malthusian Stress (Degradation)");
            case 8 -> I18n.getOrDefault("scenario.tensor.title.8", "🧬 3.2.9 Tensor 9: Pathogen Immunity & Health Memory (Epidemiology)");
            default -> I18n.getOrDefault("scenario.tensor.title.ext", "🧬 3.2.{0} Tensor {0}: Extensible Cultural Substrate {0}", (index + 1));
        };
    }

    private String getCulturalTensorTooltip(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.tooltip.0", "Linguistic component: Dialect continua, mutual intelligibility, and phonetic barriers.");
            case 1 -> I18n.getOrDefault("scenario.tensor.tooltip.1", "Organizational component: Lineage structures, exogamy, clan networks, and alliances.");
            case 2 -> I18n.getOrDefault("scenario.tensor.tooltip.2", "Sacred component: Religious norms, integration rites, taboos, and Asabiyyah social cohesion.");
            case 3 -> I18n.getOrDefault("scenario.tensor.tooltip.3", "Geopolitical component: Administrative centers, allegiance to capitals, and spheres of influence.");
            case 4 -> I18n.getOrDefault("scenario.tensor.tooltip.4", "Material component: Craft traditions, domestic tools, pottery, metallurgy, and architecture.");
            case 5 -> I18n.getOrDefault("scenario.tensor.tooltip.5", "Economic component: Trade corridors (Silk Road, Trans-Sahara, Ocean), trade hubs, and fairs.");
            case 6 -> I18n.getOrDefault("scenario.tensor.tooltip.6", "Institutional component (Seshat): Administrative complexity, codification of customary law, and bureaucracy.");
            case 7 -> I18n.getOrDefault("scenario.tensor.tooltip.7", "Ecological component: Soil degradation, deforestation, salinization, and Malthusian pressure.");
            case 8 -> I18n.getOrDefault("scenario.tensor.tooltip.8", "Sanitary component: Acquired immunity barriers, zoonotic reservoirs, and epidemic vulnerability.");
            default -> String.format(I18n.getOrDefault("scenario.tensor.tooltip.ext", "Composante culturelle multichamp extensible %dD."), (index + 1));
        };
    }

    private String getCulturalBaselineName(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.0.source.baseline", "Glottolog 4.8 / WALS Language Families (Earth Baseline)");
            case 1 -> I18n.getOrDefault("scenario.tensor.1.source.baseline", "Murdock Ethnographic Atlas / Kinship (Earth Baseline)");
            case 2 -> I18n.getOrDefault("scenario.tensor.2.source.baseline", "Seshat Global History Databank / Rituals (Earth Baseline)");
            case 3 -> I18n.getOrDefault("scenario.tensor.3.source.baseline", "Centennia Historical Atlas / Sovereignty (Earth Baseline)");
            case 4 -> I18n.getOrDefault("scenario.tensor.4.source.baseline", "ArchaeoGLOBE Project / Material Culture (Earth Baseline)");
            case 5 -> I18n.getOrDefault("scenario.tensor.5.source.baseline", "ORBIS / Old World Trade Networks (Earth Baseline)");
            case 6 -> I18n.getOrDefault("scenario.tensor.6.source.baseline", "Seshat Historical Databank / Law & Norms (Earth Baseline)");
            case 7 -> I18n.getOrDefault("scenario.tensor.7.source.baseline", "HYDE 3.4 / Historical Land Use & Degradation (Earth Baseline)");
            case 8 -> I18n.getOrDefault("scenario.tensor.8.source.baseline", "GADM / Historical Pathogen Memory (Earth Baseline)");
            default -> I18n.getOrDefault("scenario.status.empirical_loaded", "📷 Empirical baseline layer loaded");
        };
    }

    private String getCulturalFormatHint(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.0.format", "RGB 24-bit Catégoriel + Dithering Spatial (2:1 équirectangulaire) :\n  Valeurs RGB = Familles linguistiques pures & continuums dialectaux (Glottolog 5.0 / WALS)");
            case 1 -> I18n.getOrDefault("scenario.tensor.1.format", "RGB 24-bit Catégoriel (2:1 équirectangulaire) :\n  Valeurs RGB = Organisation sociale & familiale (Todd / Murdock : Souche #8B5CF6, Nucléaire #2563EB, Communautaire #DC2626, Matrilinéaire #F43F5E)");
            case 2 -> I18n.getOrDefault("scenario.tensor.2.format", "RGB 24-bit Catégoriel + Mosaïque Multiconfessionnelle (2:1 équirectangulaire) :\n  Valeurs RGB = Systèmes sacrés (Islam #10B981, Catholicisme #EC4899, Orthodoxie #8B5CF6, Culte Gréco-Romain #EA580C, Hindouisme #F59E0B, Bouddhisme #EAB308, Judaïsme #2563EB)");
            case 3 -> I18n.getOrDefault("scenario.tensor.3.format", "RGB 24-bit Catégoriel (2:1 équirectangulaire) :\n  Valeurs RGB = Souveraineté politique étatique & 22 domaines claniques régionaux (Seshat ClioPatria / CShapes)");
            case 4 -> I18n.getOrDefault("scenario.tensor.4.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 équirectangulaire) :\n  Noir (0) = Prélèvement / Faible capture exergétique | Blanc (255) = Métallurgie & outillage avancé (ArchaeoGLOBE)");
            case 5 -> I18n.getOrDefault("scenario.tensor.5.format", "RGB 24-bit Réseau Multi-Modal & Emporia Hiérarchisés (2:1 équirectangulaire) :\n  Cyan = Fleuves navigables (#00D2E6) | Or = Routes de la Soie (#FFC832) | Terracotta = Voies Impériales (#FFAA28) | Bleu = Maritime (#1EB4D2) | Disques blancs = Emporia");
            case 6 -> I18n.getOrDefault("scenario.tensor.6.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 équirectangulaire) :\n  Valeurs = Portée administrative SESHAT (Rome/Han 160, 2026 à 220, headroom [226-255]) avec extension infrastructurelle");
            case 7 -> I18n.getOrDefault("scenario.tensor.7.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 équirectangulaire) :\n  Noir (0) = Biome intact | Blanc (255) = Dégradation malthusienne & empreinte agricole (HYDE 3.4)");
            case 8 -> I18n.getOrDefault("scenario.tensor.8.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 équirectangulaire) :\n  Valeurs = Potentiel de transmission vectorielle R₀, foyers zoonotiques et promiscuité urbaine (Mordecai / CDC)");
            default -> I18n.getOrDefault("scenario.hint.cultural_format", "PNG / GeoJSON en projection équirectangulaire 2:1 (RGB Catégoriel ou Niveaux de Gris 8-bit)");
        };
    }

    /**
     * Given the scenario's start year and active planet preset, returns the best-matching
     * entry string for demoSourceCombo, or null if no match can be determined.
     */
    private String pickDemoSourceForEpoch(long startYear, org.ether.society.generation.PlanetPreset planet) {
        String pName = planet != null ? planet.name().toLowerCase() : "";
        if (pName.contains("mars") || pName.contains("ares"))
            return "🔴 Mars — Modèle de Colonisation Spatiale & Dômes d'Habitation [Planétaire (Mars), +2050 AD à Futur]";
        if (pName.contains("vénus") || pName.contains("venus") || pName.contains("hesperos"))
            return "🟡 Vénus — Stations Aérostatiques Cloud Cities (Altitude 50 km) [Planétaire (Vénus), +2100 AD à Futur]";
        if (pName.contains("lune") || pName.contains("moon") || pName.contains("selene"))
            return "⚪ Lune — Bases Sélénites Sous-Terraines & Cratères Shackleton [Planétaire (Lune), +2040 AD à Futur]";
        if (pName.contains("mercure") || pName.contains("mercury") || pName.contains("hermes"))
            return "⚪ Mercure — Dômes Polaires & Habitats d'Ombre Permanente [Planétaire (Mercure), +2150 AD à Futur]";
        // Earth epochs
        if (startYear < -10000)
            return "ðŸŒ Terre — Paléo-Démographie & Expansion Sapiens [Afrique & Eurasie, -100 000 BC à -10 000 BC]";
        if (startYear >= -3000 && startYear < 1900)
            return "ðŸŒ Terre — CShapes & Centennia Reconstitutions Démographiques [Empires & États, -3000 BC à +2000 AD]";
        return "ðŸŒ Terre — HYDE 3.4 / Grille Historique Anthropocène [Global, -10 000 BC à +2023 AD]";
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
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getItems().addAll(
            "",
            "ðŸŒ Terre — HYDE 3.4 / Grille Historique Anthropocène [Global, -10 000 BC à +2023 AD]",
            "ðŸŒ Terre — Paléo-Démographie & Expansion Sapiens [Afrique & Eurasie, -100 000 BC à -10 000 BC]",
            "ðŸŒ Terre — CShapes & Centennia Reconstitutions Démographiques [Empires & États, -3000 BC à +2000 AD]",
            "🔴 Mars — Modèle de Colonisation Spatiale & Dômes d'Habitation [Planétaire (Mars), +2050 AD à Futur]",
            "🟡 Vénus — Stations Aérostatiques Cloud Cities (Altitude 50 km) [Planétaire (Vénus), +2100 AD à Futur]",
            "⚪ Lune — Bases Sélénites Sous-Terraines & Cratères Shackleton [Planétaire (Lune), +2040 AD à Futur]",
            "⚪ Mercure — Dômes Polaires & Habitats d'Ombre Permanente [Planétaire (Mercure), +2150 AD à Futur]"
        );
        org.ether.society.ui.util.DataSourceUIHelper.setupDetailedSourceCombo(
                combo, "common.combo.prompt_source", "planet.tooltip.map_source_hint");
        long startYr = (startYearSpinner != null && startYearSpinner.getValue() != null) ? startYearSpinner.getValue().longValue() : -8000L;
        String initSrc = pickDemoSourceForEpoch(startYr, this.activePlanetPreset);
        selectBestSource(combo, initSrc, "Terre");
        combo.setOnAction(e -> {
            if (isUpdatingFromPreset) return;
            String val = combo.getValue();
            if (val != null && !val.isEmpty()) {
                if (customDensityImage != null && radioImportDemo != null) {
                    radioImportDemo.setSelected(true);
                }
                notifyParamChange();
                drawPreview();
            }
        });
        return combo;
    }

    private ComboBox<String> buildCulturalSourceCombo(int tensorIdx) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getItems().add("");
        switch (tensorIdx) {
            case 0 -> combo.getItems().addAll(
                "ðŸŒ Terre — Glottolog 4.8 / WALS Language Families [Global, -10 000 BC à Actuel (8 500+ Langues)]",
                "ðŸŒ Terre — Ethnologue World Linguistic Tree [Global, -3000 BC à Actuel (7 100+ Langues)]",
                "ðŸŒ Terre — Automated Phonological Distance Model (ASJP) [Global, -10 000 BC à Actuel (Macro-Familles)]",
                "🔴 Mars — Cartographie Linguistique Coloniale Martienne [Planétaire (Mars), +2060 AD à Futur]",
                "🟡 Vénus — Réseau Isogloss des Cités Aérostatiques [Planétaire (Vénus), +2120 AD à Futur]",
                "⚪ Lune — Dialectes Sélénites des Stations Cratériques [Planétaire (Lune), +2050 AD à Futur]",
                "⚪ Mercure — Protocoles Herméens & Terminologie d'Ombre [Planétaire (Mercure), +2160 AD à Futur]"
            );
            case 1 -> combo.getItems().addAll(
                "ðŸŒ Terre — Murdock Ethnographic Atlas (Kinship Systems) [Global, -4000 BC à Actuel (1 267 Sociétés)]",
                "ðŸŒ Terre — Standard Cross-Cultural Sample (SCCS) [Global, -4000 BC à Actuel (186 Cultures)]",
                "ðŸŒ Terre — Clan & Lineage Structural Matrix (Seshat) [Global, -4000 BC à +1900 AD]",
                "🔴 Mars — Structures de Parenté & Cohortes Pionnières [Planétaire (Mars), +2050 AD à Futur]",
                "🟡 Vénus — Guildes & Lignages Technologiques Flottants [Planétaire (Vénus), +2100 AD à Futur]",
                "⚪ Lune — Associations d'Équipages & Clans Sélénites [Planétaire (Lune), +2045 AD à Futur]",
                "⚪ Mercure — Confréries de Maintenance & Lignages Thermiques [Planétaire (Mercure), +2150 AD à Futur]"
            );
            case 2 -> combo.getItems().addAll(
                "ðŸŒ Terre — Seshat Global History Databank (Rituals & Sacred) [Global, -5000 BC à +1900 AD]",
                "ðŸŒ Terre — World Religion Database (WRD & Cultes) [Global, -3000 BC à +2020 AD]",
                "ðŸŒ Terre — Turchin Asabiyyah Cohesion Metric (Cliodynamics) [Global, -3000 BC à +2000 AD]",
                "🔴 Mars — Mythologie Martienne & Cultes de la Frontière [Planétaire (Mars), +2060 AD à Futur]",
                "🟡 Vénus — Rituels Solaires & Cérémonies de Nuages [Planétaire (Vénus), +2120 AD à Futur]",
                "⚪ Lune — Philosophie Cosmique & Rituels du Clair de Terre [Planétaire (Lune), +2050 AD à Futur]",
                "⚪ Mercure — Ordres d'Énergie & Croyances de Haute Radiation [Planétaire (Mercure), +2170 AD à Futur]"
            );
            case 3 -> combo.getItems().addAll(
                "ðŸŒ Terre — Centennia Historical Atlas (Sovereignty Boundaries) [Eurasie / Afrique / Amériques, -1000 BC à +2000 AD]",
                "ðŸŒ Terre — CShapes 2.0 Historical Polities & Borders [Global, 1886 AD à 2019 AD]",
                "ðŸŒ Terre — GADM Administrative Sovereign Centers [Global, 1950 AD à Actuel]",
                "🔴 Mars — Juridictions Consulaires & Traités Martiens [Planétaire (Mars), +2060 AD à Futur]",
                "🟡 Vénus — Fédération des Stations Stratosphériques [Planétaire (Vénus), +2110 AD à Futur]",
                "⚪ Lune — Secteurs Traité de l'Espace & Bases Nationales [Planétaire (Lune), +2050 AD à Futur]",
                "⚪ Mercure — Domaines Miniers & Enclaves Polaires [Planétaire (Mercure), +2150 AD à Futur]"
            );
            case 4 -> combo.getItems().addAll(
                "ðŸŒ Terre — ArchaeoGLOBE Project (Land Use & Material Tools) [Global, -10 000 BC à +1850 AD]",
                "ðŸŒ Terre — Archaeological Material Culture Database [Global, -50 000 BC à +1500 AD]",
                "ðŸŒ Terre — Lithic-to-Metallurgy Technology Frontier Model [Global, -100 000 BC à +2000 AD]",
                "🔴 Mars — Niveau Technologique Industriel & Robotique ISRU [Planétaire (Mars), +2050 AD à Futur]",
                "🟡 Vénus — Synthèse Aérostatique & Ingénierie Acide [Planétaire (Vénus), +2100 AD à Futur]",
                "⚪ Lune — Fonderies Régolithes & Extraction Sélénite [Planétaire (Lune), +2045 AD à Futur]",
                "⚪ Mercure — Collecteurs Haute Énergie & Fours Directs [Planétaire (Mercure), +2150 AD à Futur]"
            );
            case 5 -> combo.getItems().addAll(
                "ðŸŒ Terre — ORBIS Stanford Geospatial Network (Trade Routes) [Bassin Méditerranéen & Proche-Orient, -300 BC à +500 AD]",
                "ðŸŒ Terre — Silk Road & Maritime Monsoon Corridors [Eurasie & Océan Indien, -500 BC à +1700 AD]",
                "ðŸŒ Terre — Old World Overland Caravan Network [Sahara & Asie Centrale, -1000 BC à +1800 AD]",
                "🔴 Mars — Réseau Ferroviaire Maglev Sub-Surface [Planétaire (Mars), +2070 AD à Futur]",
                "🟡 Vénus — Navettes Stratosphériques Inter-Stations [Planétaire (Vénus), +2110 AD à Futur]",
                "⚪ Lune — Tunnels de Transport Magnétique Sélénite [Planétaire (Lune), +2050 AD à Futur]",
                "⚪ Mercure — Réseau de Convois Électromagnétiques [Planétaire (Mercure), +2160 AD à Futur]"
            );
            case 6 -> combo.getItems().addAll(
                "ðŸŒ Terre — Seshat Databank (Institutional Complexity & Law) [Global, -4000 BC à +1900 AD]",
                "ðŸŒ Terre — Cross-National Time-Series Data (CNTS Bureaucracy) [Global, 1815 AD à 2022 AD]",
                "ðŸŒ Terre — Historical Jurisprudence & Administration Matrix [Europe & Asie, -2000 BC à +1800 AD]",
                "🔴 Mars — Conseil Spatial & Chartes Constitutionnelles [Planétaire (Mars), +2060 AD à Futur]",
                "🟡 Vénus — Syndicats Flottants & Corporations Aérostats [Planétaire (Vénus), +2110 AD à Futur]",
                "⚪ Lune — Protocoles Légaux des Habitats Sélénites [Planétaire (Lune), +2050 AD à Futur]",
                "⚪ Mercure — Administration Thermique & Urgences [Planétaire (Mercure), +2160 AD à Futur]"
            );
            case 7 -> combo.getItems().addAll(
                "ðŸŒ Terre — HYDE 3.4 Historical Land Use & Anthropogenic Stress [Global, -10 000 BC à +2023 AD]",
                "ðŸŒ Terre — Anthromes 2.0 Global Anthropogenic Biomes [Global, -8000 BC à +2000 AD]",
                "ðŸŒ Terre — Malthusian Carrying Capacity Model [Global, -100 000 BC à +2100 AD]",
                "🔴 Mars — Bioregenerative Life Support (BLSS) & Dégradation [Planétaire (Mars), +2050 AD à Futur]",
                "🟡 Vénus — Érosion Chimique & Recyclage Fermé [Planétaire (Vénus), +2100 AD à Futur]",
                "⚪ Lune — Épuisement des Volatils & Poussière Régolithe [Planétaire (Lune), +2045 AD à Futur]",
                "⚪ Mercure — Usure Thermique & Contraintes Matérielles [Planétaire (Mercure), +2150 AD à Futur]"
            );
            case 8 -> combo.getItems().addAll(
                "ðŸŒ Terre — GADM / Historical Pathogen Memory & Epidemics [Global, -3000 BC à +2023 AD]",
                "ðŸŒ Terre — Global Infectious Disease Vector Database [Zones Tropicales & Tempérées, -1000 BC à +2020 AD]",
                "ðŸŒ Terre — Host-Pathogen Coevolution & Immunity Model [Global, -100 000 BC à +2100 AD]",
                "🔴 Mars — Microbiome Artificiel Confiné & Résistance [Planétaire (Mars), +2050 AD à Futur]",
                "🟡 Vénus — Immunologie en Atmosphère Confinée [Planétaire (Vénus), +2110 AD à Futur]",
                "⚪ Lune — Pathogènes d'Isolement & Régime Stérile [Planétaire (Lune), +2045 AD à Futur]",
                "⚪ Mercure — Filtrage Radiatif & Microbiote Synthétique [Planétaire (Mercure), +2160 AD à Futur]"
            );
            default -> combo.getItems().addAll(
                "ðŸŒ Terre — Seshat / Global Databank Substrate [Global, -5000 BC à +2000 AD]",
                "ðŸŒ Terre — Historical Empirical Baseline [Global, -100 000 BP à Actuel]",
                "🔴 Mars — Modèle Cartographique Martien Dérivé [Planétaire (Mars), +2050 AD à Futur]"
            );
        }
        org.ether.society.ui.util.DataSourceUIHelper.setupDetailedSourceCombo(
                combo, "common.combo.prompt_source", "planet.tooltip.map_source_hint");
        long scYear = (startYearSpinner != null && startYearSpinner.getValue() != null)
                ? startYearSpinner.getValue().longValue() : -8000L;
        String autoSrc = pickCulturalSourceForEpoch(tensorIdx, scYear, this.activePlanetPreset);
        selectBestSource(combo, autoSrc, "Terre");
        combo.setOnAction(e -> {
            if (isUpdatingFromPreset) return;
            String val = combo.getValue();
            if (val != null && !val.isEmpty()) {
                if (customTensorImages.containsKey(tensorIdx) && customTensorImages.get(tensorIdx) != null && tensorImportRadios.containsKey(tensorIdx)) {
                    tensorImportRadios.get(tensorIdx).setSelected(true);
                }
                notifyParamChange();
                drawPreview();
            }
        });
        return combo;
    }

    private static class TensorParamDescriptor {
        final String labelKey;
        final String defaultLabel;
        final String tooltipKey;
        final String defaultTooltip;
        final double min;
        final double max;
        final double defVal;
        final double step;
        final String format;

        TensorParamDescriptor(String labelKey, String defaultLabel, String tooltipKey, String defaultTooltip,
                              double min, double max, double defVal, double step, String format) {
            this.labelKey = labelKey;
            this.defaultLabel = defaultLabel;
            this.tooltipKey = tooltipKey;
            this.defaultTooltip = defaultTooltip;
            this.min = min;
            this.max = max;
            this.defVal = defVal;
            this.step = step;
            this.format = format;
        }
    }

    private TensorParamDescriptor getTensorParam1Descriptor(int tensorIdx) {
        return switch (tensorIdx) {
            case 0 -> new TensorParamDescriptor("scenario.tensor.0.p1.label", "Dispersion dialectale (α) :", "scenario.tensor.0.p1.tooltip", "Échelle spatiale de diffusion des variantes phonétiques et lexicales.", 0.01, 0.25, 0.05, 0.01, "%.2f");
            case 1 -> new TensorParamDescriptor("scenario.tensor.1.p1.label", "Rayon clanique :", "scenario.tensor.1.p1.tooltip", "Rayon spatial d'influence et de solidarité des lignages et clans.", 10.0, 500.0, 120.0, 10.0, "%.0f km");
            case 2 -> new TensorParamDescriptor("scenario.tensor.2.p1.label", "Force Asabiyyah :", "scenario.tensor.2.p1.tooltip", "Niveau de cohésion sociale et de solidarité sacrée (Asabiyyah d'Ibn Khaldoun).", 0.10, 1.00, 0.70, 0.05, "%.2f");
            case 3 -> new TensorParamDescriptor("scenario.tensor.3.p1.label", "Portée des capitales :", "scenario.tensor.3.p1.tooltip", "Rayon d'action direct de l'autorité politico-militaire centrale.", 50.0, 2500.0, 600.0, 50.0, "%.0f km");
            case 4 -> new TensorParamDescriptor("scenario.tensor.4.p1.label", "Foyers d'innovation :", "scenario.tensor.4.p1.tooltip", "Nombre de centres artisanaux et métallurgiques initiaux.", 1.0, 20.0, 5.0, 1.0, "%.0f");
            case 5 -> new TensorParamDescriptor("scenario.tensor.5.p1.label", "Comptoirs & carrefours :", "scenario.tensor.5.p1.tooltip", "Nombre de carrefours marchands et comptoirs d'échange.", 2.0, 30.0, 8.0, 1.0, "%.0f");
            case 6 -> new TensorParamDescriptor("scenario.tensor.6.p1.label", "Niveaux bureaucratiques :", "scenario.tensor.6.p1.tooltip", "Profondeur de la hiérarchie administrative (Seshat Databank).", 1.0, 8.0, 3.0, 1.0, "%.0f niv.");
            case 7 -> new TensorParamDescriptor("scenario.tensor.7.p1.label", "Surexploitation des sols :", "scenario.tensor.7.p1.tooltip", "Intensité du forçage anthropique et de la déforestation.", 0.0, 1.00, 0.40, 0.05, "%.2f");
            case 8 -> new TensorParamDescriptor("scenario.tensor.8.p1.label", "Pression pathogène :", "scenario.tensor.8.p1.tooltip", "Pression endémique virale et bactérienne régionale.", 0.0, 1.00, 0.35, 0.05, "%.2f");
            default -> new TensorParamDescriptor("scenario.tensor.ext.p1.label", "Fréquence spatiale :", "scenario.tensor.ext.p1.tooltip", "Échelle d'ondulation du substrat procédural.", 0.001, 0.05, 0.01, 0.001, "%.3f");
        };
    }

    private TensorParamDescriptor getTensorParam2Descriptor(int tensorIdx) {
        return switch (tensorIdx) {
            case 0 -> new TensorParamDescriptor("scenario.tensor.0.p2.label", "Foyers linguistiques :", "scenario.tensor.0.p2.tooltip", "Nombre de foyers indépendants et familles linguistiques initiales.", 1.0, 16.0, 4.0, 1.0, "%.0f");
            case 1 -> new TensorParamDescriptor("scenario.tensor.1.p2.label", "Perméabilité exogamique :", "scenario.tensor.1.p2.tooltip", "Taux d'alliances matrimoniales inter-clans et exogamie.", 0.0, 1.00, 0.35, 0.05, "%.2f");
            case 2 -> new TensorParamDescriptor("scenario.tensor.2.p2.label", "Sanctuaires sacrés :", "scenario.tensor.2.p2.tooltip", "Nombre de hauts lieux rituels et sanctuaires émergents.", 1.0, 25.0, 6.0, 1.0, "%.0f");
            case 3 -> new TensorParamDescriptor("scenario.tensor.3.p2.label", "Centralisation régalienne :", "scenario.tensor.3.p2.tooltip", "Degré de concentration du pouvoir politique et fiscal.", 0.0, 1.00, 0.75, 0.05, "%.2f");
            case 4 -> new TensorParamDescriptor("scenario.tensor.4.p2.label", "Technicité matérielle :", "scenario.tensor.4.p2.tooltip", "Niveau initial de complexité des artéfacts et outillages.", 0.0, 10.0, 2.5, 0.5, "%.1f");
            case 5 -> new TensorParamDescriptor("scenario.tensor.5.p2.label", "Portée des routes :", "scenario.tensor.5.p2.tooltip", "Longueur maximale des routes commerciales et caravanes.", 100.0, 4000.0, 1200.0, 100.0, "%.0f km");
            case 6 -> new TensorParamDescriptor("scenario.tensor.6.p2.label", "Codification juridique :", "scenario.tensor.6.p2.tooltip", "Niveau de formalisation et de codification des lois écrites.", 0.0, 1.00, 0.45, 0.05, "%.2f");
            case 7 -> new TensorParamDescriptor("scenario.tensor.7.p2.label", "Taux d'épuisement :", "scenario.tensor.7.p2.tooltip", "Vitesse d'érosion des sols et d'amenuisement des ressources.", 0.001, 0.10, 0.02, 0.005, "%.3f");
            case 8 -> new TensorParamDescriptor("scenario.tensor.8.p2.label", "Réservoirs zoonotiques :", "scenario.tensor.8.p2.tooltip", "Rayon d'influence des foyers sauvages et réservoirs animaux.", 20.0, 600.0, 180.0, 20.0, "%.0f km");
            default -> new TensorParamDescriptor("scenario.tensor.ext.p2.label", "Amplitude du signal :", "scenario.tensor.ext.p2.tooltip", "Intensité relative du tenseur extensible.", 0.10, 1.00, 0.80, 0.05, "%.2f");
        };
    }

    private TensorParamDescriptor getTensorParam3Descriptor(int tensorIdx) {
        return switch (tensorIdx) {
            case 0 -> new TensorParamDescriptor("scenario.tensor.0.p3.label", "Barrière de relief :", "scenario.tensor.0.p3.tooltip", "Impact du relief et des chaînes de montagnes sur l'isolation linguistique.", 0.0, 1.00, 0.60, 0.05, "%.2f");
            case 1 -> new TensorParamDescriptor("scenario.tensor.1.p3.label", "Hiérarchie lignagère :", "scenario.tensor.1.p3.tooltip", "Degré de structuration et de segmentation patriarcale/matriarcale.", 0.0, 1.00, 0.50, 0.05, "%.2f");
            case 2 -> new TensorParamDescriptor("scenario.tensor.2.p3.label", "Diffusion théologique :", "scenario.tensor.2.p3.tooltip", "Portée spatiale d'expansion des doctrines et rites sacrés.", 50.0, 2000.0, 400.0, 50.0, "%.0f km");
            case 3 -> new TensorParamDescriptor("scenario.tensor.3.p3.label", "Friction frontalière :", "scenario.tensor.3.p3.tooltip", "Tension militaire et friction aux marches de l'empire.", 0.0, 1.00, 0.40, 0.05, "%.2f");
            case 4 -> new TensorParamDescriptor("scenario.tensor.4.p3.label", "Diffusion technique :", "scenario.tensor.4.p3.tooltip", "Conductance de propagation des savoirs artisanaux.", 0.01, 0.30, 0.08, 0.01, "%.2f");
            case 5 -> new TensorParamDescriptor("scenario.tensor.5.p3.label", "Pondération maritime :", "scenario.tensor.5.p3.tooltip", "Attractivité des voies navigables, côtières et maritimes.", 0.0, 1.00, 0.65, 0.05, "%.2f");
            case 6 -> new TensorParamDescriptor("scenario.tensor.6.p3.label", "Seuil d'intégration :", "scenario.tensor.6.p3.tooltip", "Taille de population déclenchant l'émergence des institutions.", 100.0, 10000.0, 1500.0, 100.0, "%.0f hab.");
            case 7 -> new TensorParamDescriptor("scenario.tensor.7.p3.label", "Résilience biocapacité :", "scenario.tensor.7.p3.tooltip", "Capacité de régénération naturelle du biome.", 0.10, 2.00, 1.00, 0.10, "%.2f");
            case 8 -> new TensorParamDescriptor("scenario.tensor.8.p3.label", "Mémoire immunitaire :", "scenario.tensor.8.p3.tooltip", "Vitesse d'acquisition et persistance de l'immunité de groupe.", 0.01, 0.25, 0.06, 0.01, "%.2f");
            default -> new TensorParamDescriptor("scenario.tensor.ext.p3.label", "Taux de diffusion :", "scenario.tensor.ext.p3.tooltip", "Conductance de diffusion spatiale.", 0.01, 0.20, 0.05, 0.01, "%.2f");
        };
    }

    private String getDefaultTensorSeed(int tensorIdx) {
        return String.valueOf(11235L + tensorIdx * 11111L);
    }

    private void updateTensorFileLabel(int tensorIdx) {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        Label fileLbl = tensorFileLabels.get(tensorIdx);
        Button loadBtn = tensorLoadBtns.get(tensorIdx);
        if (fileLbl == null) return;
        boolean isProc = tensorProcRadios.containsKey(tensorIdx) && tensorProcRadios.get(tensorIdx).isSelected();
        Image img = customTensorImages.get(tensorIdx);

        if (isProc) {
            fileLbl.setText(I18n.getOrDefault("scenario.tensor.status.procedural", "✅ Mode procédural actif"));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
            if (loadBtn != null) loadBtn.setStyle("");
            return;
        }

        if (img == null) {
            fileLbl.setText(I18n.getOrDefault("scenario.tensor.file.none", "⚠️ï¸ Aucune carte chargée — Fichier requis en mode import"));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #f59e0b;");
            if (loadBtn != null) loadBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            return;
        }

        org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(img);
        if (!val.valid()) {
            fileLbl.setText(String.format(I18n.getOrDefault("scenario.tensor.file.invalid", "⚠️ï¸ Carte incompatible : %s"), val.message()));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            if (loadBtn != null) loadBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
        } else {
            fileLbl.setText(String.format(I18n.getOrDefault("scenario.tensor.file.loaded_res", "📷 Carte chargée : %s (%dx%d)"), getCulturalBaselineName(tensorIdx), val.width(), val.height()));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8;");
            if (loadBtn != null) loadBtn.setStyle("");
        }
    }

    private void exportCulturalTensor(int tensorIdx) {
        Image img = customTensorImages.get(tensorIdx);
        if (img == null) {
            img = generateProceduralTensorRasterImage(tensorIdx, 2048, 1024);
        }
        String defaultName = "cultural_tensor_" + (tensorIdx + 1) + ".png";
        String title = I18n.getOrDefault("scenario.title.export_tensor_dialog", "Export Cultural Tensor " + (tensorIdx + 1));
        WindowUtils.exportImageWithChooser(getScene() != null ? getScene().getWindow() : null, img, defaultName, title);
    }

    private Image generateProceduralTensorRasterImage(int tensorIdx, int w, int h) {
        WritableImage img = new WritableImage(w, h);
        PixelWriter pw = img.getPixelWriter();
        double p1 = tensorParam1Sliders.containsKey(tensorIdx) ? tensorParam1Sliders.get(tensorIdx).getValue() : 1.0;
        double p2 = tensorParam2Sliders.containsKey(tensorIdx) ? tensorParam2Sliders.get(tensorIdx).getValue() : 1.0;
        double p3 = tensorParam3Sliders.containsKey(tensorIdx) ? tensorParam3Sliders.get(tensorIdx).getValue() : 1.0;
        long seed = 11235L + tensorIdx * 11111L;
        if (tensorSeedFields.containsKey(tensorIdx)) {
            try { seed = Long.parseLong(tensorSeedFields.get(tensorIdx).getText().trim()); } catch (Exception ignored) {}
        }
        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y / (double) h) * 180.0;
            for (int x = 0; x < w; x++) {
                double lon = -180.0 + (x / (double) w) * 360.0;
                Color c = evaluateProceduralTensorColor(tensorIdx, lat, lon, 500.0, p1, p2, p3, seed);
                pw.setColor(x, y, c);
            }
        }
        return img;
    }

    private Color evaluateProceduralTensorColor(int tIndex, double lat, double lon, double elevation, double p1, double p2, double p3, long seed) {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        double noiseX = (lon + 180.0) / 360.0;
        double noiseY = (lat + 90.0) / 180.0;
        double pseudoNoise = Math.sin(noiseX * 12.0 + seed % 100) * Math.cos(noiseY * 12.0 + (seed / 100) % 100);
        double pseudoNoise2 = Math.sin(noiseX * 24.0 + (seed / 10) % 100) * Math.cos(noiseY * 24.0 + seed % 50);
        double combinedNoise = Math.clamp(0.5 + 0.35 * pseudoNoise + 0.15 * pseudoNoise2, 0.0, 1.0);

        return switch (tIndex % 9) {
            case 0 -> {
                double reliefFactor = Math.clamp(elevation / 3000.0, 0.0, 1.0) * p3;
                double val = Math.clamp((noiseY * 0.7 + noiseX * 0.3 + combinedNoise * 0.3 * p1 + reliefFactor * 0.2) * (p2 / 4.0), 0.0, 1.0);
                yield Color.hsb(val * 300.0, 0.75, 0.90);
            }
            case 1 -> {
                double val = Math.clamp(Math.abs(Math.sin((lat + p1 * 0.01) * 0.1) * Math.cos((lon + p2 * 10.0) * 0.1) + combinedNoise * p3 * 0.3), 0.0, 1.0);
                yield Color.hsb(180.0 + val * 120.0, 0.80, 0.85);
            }
            case 2 -> {
                double altNorm = Math.clamp(elevation / 3000.0, 0.0, 1.0);
                double val = Math.clamp(altNorm * 0.5 + combinedNoise * 0.5 * p1, 0.0, 1.0);
                yield Color.hsb(40.0 + val * 200.0, 0.85, 0.95);
            }
            case 3 -> {
                double val = Math.clamp(combinedNoise * p2, 0.0, 1.0);
                yield Color.hsb((val * 360.0 + seed % 360) % 360.0, 0.75, 0.85);
            }
            case 4 -> {
                double val = Math.clamp(combinedNoise * (p2 / 5.0) + p1 * 0.05, 0.0, 1.0);
                yield Color.gray(val);
            }
            case 5 -> {
                double val = Math.clamp(combinedNoise * p3 + (p1 / 30.0), 0.0, 1.0);
                yield Color.hsb(35.0 + val * 30.0, 0.85, 0.95);
            }
            case 6 -> {
                double val = Math.clamp(combinedNoise * p2 + (p1 / 8.0) * 0.5, 0.0, 1.0);
                yield Color.gray(val);
            }
            case 7 -> {
                double val = Math.clamp(combinedNoise * p1 + p2 * 5.0, 0.0, 1.0);
                yield Color.gray(val);
            }
            case 8 -> {
                double val = Math.clamp(combinedNoise * p1 + p3 * 2.0, 0.0, 1.0);
                yield Color.gray(val);
            }
            default -> {
                double val = Math.clamp(combinedNoise, 0.0, 1.0);
                yield Color.gray(val);
            }
        };
    }

    private HBox createTensorParamRow(TensorParamDescriptor desc, Slider slider, Label valueLabel, Runnable onChange) {
        Label label = new Label(I18n.getOrDefault(desc.labelKey, desc.defaultLabel));
        label.getStyleClass().add("control-label");
        label.setStyle("-fx-font-size: 10px;");
        label.setMinWidth(140);
        label.setMaxWidth(140);
        label.setWrapText(true);

        slider.setMin(desc.min);
        slider.setMax(desc.max);
        slider.setValue(desc.defVal);
        slider.setBlockIncrement(desc.step);
        slider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(slider, Priority.ALWAYS);

        valueLabel.setText(String.format(java.util.Locale.ROOT, desc.format, desc.defVal));
        valueLabel.getStyleClass().add("value-label");
        valueLabel.setStyle("-fx-font-size: 10px;");
        valueLabel.setMinWidth(65);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            valueLabel.setText(String.format(java.util.Locale.ROOT, desc.format, newVal.doubleValue()));
            if (!isUpdatingFromPreset) {
                onChange.run();
            }
        });

        String tip = I18n.getOrDefault(desc.tooltipKey, desc.defaultTooltip);
        if (tip != null && !tip.isEmpty()) {
            Tooltip.install(label, new Tooltip(tip));
            Tooltip.install(slider, new Tooltip(tip));
        }

        attachDefaultValueHandling(slider, desc.defVal, () -> slider.setValue(desc.defVal));

        HBox row = new HBox(6, label, slider, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void deleteCulturalTensor(int indexToDelete) {
        int curDims = cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null ? cultureVectorDimSpinner.getValue() : 9;
        if (indexToDelete < 0 || indexToDelete >= curDims) return;

        // Shift all configurations from indexToDelete up to curDims - 2
        for (int j = indexToDelete; j < curDims - 1; j++) {
            // Names
            if (customTensorNames.containsKey(j + 1)) {
                customTensorNames.put(j, customTensorNames.get(j + 1));
            } else {
                customTensorNames.remove(j);
            }
            // Images
            if (customTensorImages.containsKey(j + 1)) {
                customTensorImages.put(j, customTensorImages.get(j + 1));
            } else {
                customTensorImages.remove(j);
            }
            // Seeds
            if (tensorSeedFields.containsKey(j + 1)) {
                String s = tensorSeedFields.get(j + 1).getText();
                if (tensorSeedFields.containsKey(j)) tensorSeedFields.get(j).setText(s);
                else tensorSeedFields.put(j, new TextField(s));
            }
            // Sliders & Values
            if (tensorParam1Sliders.containsKey(j + 1) && tensorParam1Sliders.containsKey(j)) {
                tensorParam1Sliders.get(j).setValue(tensorParam1Sliders.get(j + 1).getValue());
            }
            if (tensorParam2Sliders.containsKey(j + 1) && tensorParam2Sliders.containsKey(j)) {
                tensorParam2Sliders.get(j).setValue(tensorParam2Sliders.get(j + 1).getValue());
            }
            if (tensorParam3Sliders.containsKey(j + 1) && tensorParam3Sliders.containsKey(j)) {
                tensorParam3Sliders.get(j).setValue(tensorParam3Sliders.get(j + 1).getValue());
            }
            // Radio proc vs import
            if (tensorImportRadios.containsKey(j + 1) && tensorImportRadios.get(j + 1).isSelected()) {
                if (tensorImportRadios.containsKey(j)) tensorImportRadios.get(j).setSelected(true);
            } else {
                if (tensorProcRadios.containsKey(j)) tensorProcRadios.get(j).setSelected(true);
            }
        }

        // Clean up the last index
        int lastIdx = curDims - 1;
        customTensorNames.remove(lastIdx);
        customTensorImages.remove(lastIdx);
        tensorSeedFields.remove(lastIdx);
        tensorParam1Sliders.remove(lastIdx);
        tensorParam2Sliders.remove(lastIdx);
        tensorParam3Sliders.remove(lastIdx);
        tensorParam1ValueLabels.remove(lastIdx);
        tensorParam2ValueLabels.remove(lastIdx);
        tensorParam3ValueLabels.remove(lastIdx);
        tensorProcRadios.remove(lastIdx);
        tensorImportRadios.remove(lastIdx);
        tensorSourceCombos.remove(lastIdx);
        tensorSourceLabels.remove(lastIdx);
        tensorFileLabels.remove(lastIdx);
        tensorLoadBtns.remove(lastIdx);
        tensorFormatLabels.remove(lastIdx);
        tensorGenSingleBtns.remove(lastIdx);
        tensorSubTitles.remove(lastIdx);

        int newDims = Math.max(0, curDims - 1);
        if (cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValueFactory() != null) {
            cultureVectorDimSpinner.getValueFactory().setValue(newDims);
        } else {
            rebuildCulturalTensorSubBlocks(newDims);
            updatePreviewModesCombo();
            notifyParamChange();
            drawPreview();
        }
    }

    private void updateCulturalTensorSubBlockTexts() {
        if (btnGenerateProceduralTensorsSection != null) {
            btnGenerateProceduralTensorsSection.setText(I18n.getOrDefault("scenario.btn.regen_tensors", "ðŸª„ Régénérer les Tenseurs"));
            btnGenerateProceduralTensorsSection.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.regen_tensors", "Bascule tous les tenseurs en mode procédural et régénère les cartes selon les paramètres et la graine stochastique.")));
        }
        int dims = cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null ? cultureVectorDimSpinner.getValue() : 9;
        if (layersDynamicContainer == null || layersDynamicContainer.getChildren().size() != dims) {
            rebuildCulturalTensorSubBlocks(dims);
            return;
        }
        for (int i = 0; i < dims; i++) {
            Label subTitle = tensorSubTitles.get(i);
            if (subTitle != null) {
                subTitle.setText(getCulturalTensorTitle(i));
                Tooltip.install(subTitle, new Tooltip(getCulturalTensorTooltip(i)));
            }
            RadioButton radioProc = tensorProcRadios.get(i);
            if (radioProc != null) {
                radioProc.setText(I18n.getOrDefault("scenario.mode.procedural_sde", "▶️ Mode Procédural (Paramètres adaptatifs & Graine)"));
                radioProc.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_radio_proc", "Génère procéduralement ce tenseur culturel via des équations stochastiques et les paramètres ci-dessous.")));
            }
            RadioButton radioImport = tensorImportRadios.get(i);
            if (radioImport != null) {
                radioImport.setText(I18n.getOrDefault("scenario.mode.spatial_import", "ðŸ“‚ Importation Carte Spatiale (PNG / GeoJSON)"));
                radioImport.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_radio_import", "Importe une image matricielle ou une couche SIG externe pour modéliser ce tenseur culturel.")));
            }
            Label srcLbl = tensorSourceLabels.get(i);
            if (srcLbl != null) {
                srcLbl.setText(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
            }
            Button loadBtn = tensorLoadBtns.get(i);
            if (loadBtn != null) {
                loadBtn.setText(I18n.getOrDefault("resource.btn.load_map", "Load Map"));
            }
            Button genBtn = tensorGenSingleBtns.get(i);
            if (genBtn != null) {
                genBtn.setText(I18n.getOrDefault("scenario.tensor.btn.export_single", "ðŸ“¤ Export"));
            }
            Label fmtLbl = tensorFormatLabels.get(i);
            if (fmtLbl != null) {
                fmtLbl.setText(getCulturalFormatHint(i));
            }
            ComboBox<String> sc = tensorSourceCombos.get(i);
            if (sc != null && sc.getCellFactory() != null) {
                sc.setButtonCell(sc.getCellFactory().call(null));
            }
        }
    }

    private void rebuildCulturalTensorSubBlocks(int dimCount) {
        if (btnGenerateProceduralTensorsSection != null) {
            btnGenerateProceduralTensorsSection.setText(I18n.getOrDefault("scenario.btn.regen_tensors", "🪄 Régénérer les Tenseurs"));
            btnGenerateProceduralTensorsSection.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.regen_tensors", "Bascule tous les tenseurs en mode procédural et régénère les cartes selon les paramètres et la graine stochastique.")));
        }
        if (layersDynamicContainer == null) return;
        layersDynamicContainer.getChildren().clear();

        for (int i = 0; i < dimCount; i++) {
            final int tensorIdx = i;

            VBox subBlock = new VBox(6);
            subBlock.setStyle("-fx-padding: 10; -fx-background-color: rgba(167,139,250,0.04); -fx-background-radius: 6; -fx-border-color: rgba(167,139,250,0.15); -fx-border-radius: 6;");

            HBox titleRow = new HBox(8);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            Label subTitle = new Label(getCulturalTensorTitle(i));
            subTitle.getStyleClass().add("control-label");
            subTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            subTitle.setWrapText(true);
            HBox.setHgrow(subTitle, Priority.ALWAYS);
            Tooltip.install(subTitle, new Tooltip(getCulturalTensorTooltip(i)));
            tensorSubTitles.put(tensorIdx, subTitle);

            Button renameBtn = new Button(I18n.getOrDefault("scenario.tensor.btn.rename", "âœï¸ Renommer"));
            renameBtn.getStyleClass().add("button-secondary");
            renameBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            renameBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tensor.btn.rename_tooltip", "Cliquez pour personnaliser le nom de ce calque de tenseur culturel.")));
            renameBtn.setOnAction(e -> {
                String cur = customTensorNames.getOrDefault(tensorIdx, "");
                TextInputDialog dlg = new TextInputDialog(cur);
                dlg.setTitle(I18n.getOrDefault("scenario.tensor.rename.title", "Personnaliser le Nom du Tenseur"));
                dlg.setHeaderText(I18n.getOrDefault("scenario.tensor.rename.prompt", "Nom personnalisé du Tenseur N°{0} :", (tensorIdx + 1)));
                WindowUtils.applyWindowIcon(dlg);
                dlg.showAndWait().ifPresent(strVal -> {
                    if (strVal.isBlank()) {
                        customTensorNames.remove(tensorIdx);
                    } else {
                        customTensorNames.put(tensorIdx, strVal.trim());
                    }
                    subTitle.setText(getCulturalTensorTitle(tensorIdx));
                    updatePreviewModesCombo();
                    notifyParamChange();
                });
            });

            Button deleteBtn = new Button(I18n.getOrDefault("scenario.tensor.btn.delete", "✖"));
            deleteBtn.getStyleClass().add("button-secondary");
            deleteBtn.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-padding: 2 6; -fx-font-weight: bold; -fx-cursor: hand;");
            deleteBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tensor.btn.delete_tooltip", "Supprimer ce tenseur culturel (décrémente le nombre total de tenseurs).")));
            deleteBtn.setOnAction(e -> deleteCulturalTensor(tensorIdx));

            titleRow.getChildren().addAll(subTitle, renameBtn, deleteBtn);
            subBlock.getChildren().add(titleRow);

            ToggleGroup tg = new ToggleGroup();
            RadioButton radioProc = new RadioButton(I18n.getOrDefault("scenario.mode.procedural_sde", "▶ Mode Procédural (Paramètres adaptatifs & Graine)"));
            RadioButton radioImport = new RadioButton(I18n.getOrDefault("scenario.mode.spatial_import", "📂 Importation Carte Spatiale (PNG / GeoJSON)"));
            radioProc.setToggleGroup(tg);
            radioImport.setToggleGroup(tg);
            radioProc.getStyleClass().add("radio-proc");
            radioImport.getStyleClass().add("radio-import");
            radioProc.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_radio_proc", "Génère procéduralement ce tenseur culturel via des équations stochastiques et les paramètres ci-dessous.")));
            radioImport.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_radio_import", "Importe une image matricielle ou une couche SIG externe pour modéliser ce tenseur culturel.")));

            boolean hasImage = customTensorImages.containsKey(tensorIdx) && customTensorImages.get(tensorIdx) != null;
            if (hasImage) {
                radioImport.setSelected(true);
            } else {
                radioProc.setSelected(true);
            }

            tensorProcRadios.put(tensorIdx, radioProc);
            tensorImportRadios.put(tensorIdx, radioImport);

            // --- 1. Procedural Configuration Sub-Box ---
            Label seedLbl = new Label(I18n.getOrDefault("scenario.tensor.seed.label", "Graine :"));
            seedLbl.getStyleClass().add("control-label");
            seedLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            seedLbl.setMinWidth(60);

            TextField seedField = tensorSeedFields.computeIfAbsent(tensorIdx, k -> new TextField(getDefaultTensorSeed(tensorIdx)));
            seedField.setStyle("-fx-font-size: 10px; -fx-pref-width: 80px;");
            seedField.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.tensor_seed_field", "Graine stochastique déterministe pour initialiser ce champ de tenseur culturel.")));
            seedField.textProperty().addListener((obs, o, n) -> {
                if (!isUpdatingFromPreset) {
                    notifyParamChange();
                    drawPreview();
                }
            });

            Button randBtn = new Button("🎲");
            randBtn.getStyleClass().add("button-secondary");
            randBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            randBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.random_tensor_seed", "Tirer une nouvelle graine aléatoire pour ce tenseur.")));
            randBtn.setOnAction(e -> {
                seedField.setText(String.valueOf(new java.util.Random().nextLong(1000000)));
                if (radioProc != null) {
                    radioProc.setSelected(true);
                }
                if (previewModeCombo != null && previewModeCombo.getSelectionModel().getSelectedIndex() != (tensorIdx + 1)) {
                    previewModeCombo.getSelectionModel().select(tensorIdx + 1);
                }
                notifyParamChange();
                drawPreview();
            });

            Button exportSingleBtn = new Button(I18n.getOrDefault("scenario.tensor.btn.export_single", "📤 Export"));
            exportSingleBtn.getStyleClass().add("button-secondary");
            exportSingleBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 8; -fx-font-weight: bold;");
            exportSingleBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tensor.btn.export_single_tooltip", "Export this cultural tensor raster as a high-resolution PNG or JPEG file.")));
            tensorGenSingleBtns.put(tensorIdx, exportSingleBtn);
            exportSingleBtn.setOnAction(e -> exportCulturalTensor(tensorIdx));

            HBox seedRow = new HBox(6, seedLbl, seedField, randBtn, exportSingleBtn);
            seedRow.setAlignment(Pos.CENTER_LEFT);

            Slider p1Slider = tensorParam1Sliders.computeIfAbsent(tensorIdx, k -> new Slider());
            Label p1Val = tensorParam1ValueLabels.computeIfAbsent(tensorIdx, k -> new Label());
            HBox p1Row = createTensorParamRow(getTensorParam1Descriptor(tensorIdx), p1Slider, p1Val, () -> { notifyParamChange(); drawPreview(); });

            Slider p2Slider = tensorParam2Sliders.computeIfAbsent(tensorIdx, k -> new Slider());
            Label p2Val = tensorParam2ValueLabels.computeIfAbsent(tensorIdx, k -> new Label());
            HBox p2Row = createTensorParamRow(getTensorParam2Descriptor(tensorIdx), p2Slider, p2Val, () -> { notifyParamChange(); drawPreview(); });

            Slider p3Slider = tensorParam3Sliders.computeIfAbsent(tensorIdx, k -> new Slider());
            Label p3Val = tensorParam3ValueLabels.computeIfAbsent(tensorIdx, k -> new Label());
            HBox p3Row = createTensorParamRow(getTensorParam3Descriptor(tensorIdx), p3Slider, p3Val, () -> { notifyParamChange(); drawPreview(); });

            Label procStatusLbl = new Label(I18n.getOrDefault("scenario.status.proc_gen_tensor_prefix", "🪄 Modélisation procédurale dynamique active (Tenseur ") + (tensorIdx + 1) + ")");
            procStatusLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #10b981;");

            VBox procBox = new VBox(5, seedRow, p1Row, p2Row, p3Row, procStatusLbl);
            procBox.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
            procBox.setVisible(!hasImage);
            procBox.setManaged(!hasImage);

            // --- 2. Import Configuration Sub-Box ---
            Label sourceLbl = new Label(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
            tensorSourceLabels.put(tensorIdx, sourceLbl);

            ComboBox<String> sourceCombo = buildCulturalSourceCombo(tensorIdx);
            {
                // Auto-select the culturally appropriate source based on the scenario epoch and planet
                long scYear = (startYearSpinner != null && startYearSpinner.getValue() != null)
                        ? startYearSpinner.getValue().longValue() : 0L;
                String autoSrc = pickCulturalSourceForEpoch(tensorIdx, scYear, this.activePlanetPreset);
                selectBestSource(sourceCombo, autoSrc, "Terre");
            }
            tensorSourceCombos.put(tensorIdx, sourceCombo);

            Button btnLoad = new Button(I18n.getOrDefault("resource.btn.load_map", "Load Map"));
            btnLoad.getStyleClass().add("button-secondary");
            btnLoad.setStyle("-fx-font-size: 11px;");
            btnLoad.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.load_cultural_tensor_map", "Ouvre un sélecteur de fichier pour importer une carte raster externe (PNG/GeoTIFF) pour ce tenseur.")));
            tensorLoadBtns.put(tensorIdx, btnLoad);

            Button btnClear = new Button("❌");
            btnClear.getStyleClass().add("button-secondary");
            btnClear.setStyle("-fx-font-size: 11px;");
            btnClear.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.clear_cultural_tensor_map", "Efface l'image importée et réinitialise le tenseur au mode procédural par défaut.")));

            HBox btnBox = new HBox(6, btnLoad, btnClear);
            btnBox.setAlignment(Pos.CENTER_LEFT);

            Label fileLbl = new Label();
            fileLbl.getStyleClass().add("value-label");
            fileLbl.setStyle("-fx-font-size: 10px;");
            tensorFileLabels.put(tensorIdx, fileLbl);
            updateTensorFileLabel(tensorIdx);

            Label formatHintLbl = new Label(getCulturalFormatHint(tensorIdx));
            formatHintLbl.getStyleClass().add("card-description-muted");
            formatHintLbl.setStyle("-fx-font-size: 9px; -fx-font-style: italic;");
            formatHintLbl.setWrapText(true);
            tensorFormatLabels.put(tensorIdx, formatHintLbl);

            VBox importBox = new VBox(6, sourceLbl, sourceCombo, btnBox, fileLbl, formatHintLbl);
            importBox.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(167,139,250,0.25); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
            importBox.setVisible(hasImage);
            importBox.setManaged(hasImage);

            tg.selectedToggleProperty().addListener((obs, oldV, sel) -> {
                boolean isProc = sel == radioProc;
                procBox.setVisible(isProc);
                procBox.setManaged(isProc);
                importBox.setVisible(!isProc);
                importBox.setManaged(!isProc);
                updateTensorFileLabel(tensorIdx);
                if (previewModeCombo != null && previewModeCombo.getSelectionModel().getSelectedIndex() != (tensorIdx + 1)) {
                    previewModeCombo.getSelectionModel().select(tensorIdx + 1);
                }
                if (!isUpdatingFromPreset) {
                    notifyParamChange();
                    drawPreview();
                }
            });

            btnLoad.setOnAction(e -> loadCustomCultureLayerForTensor(tensorIdx, img -> {
                customTensorImages.put(tensorIdx, img);
                if (tensorIdx == 0) customIsoglossImage = img;
                if (tensorIdx == 1) customKinshipImage = img;
                if (tensorIdx == 2) customRitualsImage = img;
                if (tensorIdx == 3) customSovereigntyImage = img;

                updateTensorFileLabel(tensorIdx);
                if (previewModeCombo != null && previewModeCombo.getSelectionModel().getSelectedIndex() != (tensorIdx + 1)) {
                    previewModeCombo.getSelectionModel().select(tensorIdx + 1);
                }
                radioImport.setSelected(true);
                notifyParamChange();
                drawPreview();
            }));

            btnClear.setOnAction(e -> {
                customTensorImages.remove(tensorIdx);
                if (tensorIdx == 0) customIsoglossImage = null;
                if (tensorIdx == 1) customKinshipImage = null;
                if (tensorIdx == 2) customRitualsImage = null;
                if (tensorIdx == 3) customSovereigntyImage = null;

                updateTensorFileLabel(tensorIdx);
                if (previewModeCombo != null && previewModeCombo.getSelectionModel().getSelectedIndex() != (tensorIdx + 1)) {
                    previewModeCombo.getSelectionModel().select(tensorIdx + 1);
                }
                radioProc.setSelected(true);
                notifyParamChange();
                drawPreview();
            });

            subBlock.getChildren().addAll(subTitle, radioProc, procBox, radioImport, importBox);
            layersDynamicContainer.getChildren().add(subBlock);
        }
    }

    private void loadCustomCultureLayerForTensor(int tensorIdx, Consumer<Image> onLoaded) {
        String name = getCulturalTensorTitle(tensorIdx);
        loadCustomCultureLayer(name, onLoaded);
    }

    private void loadCustomCultureLayer(String layerName, Consumer<Image> onLoaded) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("scenario.dialog.import_layer_title", "Import layer ") + layerName + " (PNG/GeoJSON/JPG)");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images & Cartes", "*.png", "*.jpg", "*.jpeg", "*.geojson", "*.json")
        );
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                Image img = new Image(new FileInputStream(file));
                org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(img);
                if (!val.valid()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(I18n.getOrDefault("scenario.dialog.invalid_map_title", "Carte Incompatible ou Invalide"));
                    alert.setHeaderText(I18n.getOrDefault("scenario.dialog.invalid_map_header", "Fichier de carte non supporté ou illisible"));
                    alert.setContentText(file.getName() + " :\n" + val.message() + "\n\n" + I18n.getOrDefault("scenario.dialog.invalid_map_hint", "Veuillez fournir une image raster valide (PNG/JPG) en projection équirectangulaire (2:1)."));
                    alert.showAndWait();
                    return;
                }
                onLoaded.accept(img);
            } catch (Exception ex) {
                logger.error("Failed to load culture layer {}", layerName, ex);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur de chargement: " + ex.getMessage());
                alert.showAndWait();
            }
        }
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
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10));
        root.getStyleClass().add("subcard-section");

        previewTitleLabel = new Label(I18n.getOrDefault("scenario.title.right_view", "ðŸ—ºï¸ Resource Cartography & Display"));
        previewTitleLabel.getStyleClass().add("label-header");
        previewTitleLabel.setMaxWidth(Double.MAX_VALUE);
        previewTitleLabel.setAlignment(Pos.CENTER);
        previewTitleLabel.setStyle("-fx-alignment: center; -fx-text-alignment: center; -fx-font-weight: bold;");

        HBox titleBox = new HBox(previewTitleLabel);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setMaxWidth(Double.MAX_VALUE);

        previewModeCombo = new ComboBox<>();
        updatePreviewModesCombo();
        previewModeCombo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        previewModeCombo.setMaxWidth(380);
        previewModeCombo.setCellFactory(p -> new ListCell<>() {
            /**
             * Update item.
             *
             * @param item the item parameter (String)
             * @param empty the empty parameter (boolean)
             */
            @Override
            protected void updateItem(String item, boolean empty) {
                // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                } else if (item.startsWith("──────────")) {
                    setText(item);
                    setDisable(true);
                    setStyle("-fx-font-weight: bold; -fx-opacity: 0.6; -fx-padding: 4 8;");
                } else {
                    setText(item);
                    setDisable(false);
                    setStyle("-fx-font-weight: normal;");
                }
            }
        });
        previewModeCombo.setOnAction(e -> {
            String val = previewModeCombo.getValue();
            if (val != null && (val.startsWith("──────────") || val.startsWith("─") || val.contains("─────"))) {
                previewModeCombo.getSelectionModel().select(0);
                return;
            }
            updatePreviewTitleText();
            updateBottomLegend();
            drawPreview();
        });

        btnReliefOverlay = new ToggleButton(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.relief_overlay", "⛏️ï¸ Relief"));
        btnReliefOverlay.setSelected(false);
        btnReliefOverlay.getStyleClass().add("button-secondary");
        btnReliefOverlay.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.relief_overlay", "Superposer l'ombrage du relief topographique et des pentes avec délimitation du trait de côte.")));
        btnReliefOverlay.setOnAction(e -> drawPreview());

        HBox controlBar = new HBox(8, previewModeCombo, btnReliefOverlay);
        controlBar.setAlignment(Pos.CENTER);

        StackPane canvasContainer = new StackPane();
        canvasContainer.setStyle("-fx-background-color: black; -fx-border-color: #475569; -fx-border-radius: 6; -fx-background-radius: 6;");
        previewCanvas = new Canvas(640, 360);

        // Clip container (not canvas directly) to prevent JavaFX Prism NGCanvas renderForClip 0x0 NPE
        javafx.scene.shape.Rectangle containerClip = new javafx.scene.shape.Rectangle();
        containerClip.widthProperty().bind(canvasContainer.widthProperty());
        containerClip.heightProperty().bind(canvasContainer.heightProperty());
        canvasContainer.setClip(containerClip);

        canvasContainer.widthProperty().addListener((obs, oldV, newV) -> {
            double w = Math.floor(newV.doubleValue());
            if (w >= 10.0 && Math.abs(w - previewCanvas.getWidth()) >= 4.0) {
                previewCanvas.setWidth(w);
                drawPreview();
            }
        });
        canvasContainer.heightProperty().addListener((obs, oldV, newV) -> {
            double h = Math.floor(newV.doubleValue());
            if (h >= 10.0 && Math.abs(h - previewCanvas.getHeight()) >= 4.0) {
                previewCanvas.setHeight(h);
                drawPreview();
            }
        });

        // Interactive Zoom, Pan, Selection & Double Click Handlers
        previewCanvas.setOnScroll(e -> {
            double delta = e.getDeltaY();
            double factor = delta > 0 ? 1.15 : 0.85;
            double oldZoom = zoomFactor;
            double newZoom = Math.max(0.5, Math.min(20.0, oldZoom * factor));

            if (newZoom != oldZoom) {
                double mouseX = e.getX();
                double mouseY = e.getY();
                double w = previewCanvas.getWidth();
                double h = previewCanvas.getHeight();
                double baseScale = Math.min(w / 360.0, h / 180.0);
                double oldScale = baseScale * oldZoom;
                double newScale = baseScale * newZoom;

                if (oldScale > 0 && newScale > 0) {
                    double mouseLng = (mouseX - w / 2.0 - panX) / oldScale;
                    double mouseLat = (h / 2.0 + panY - mouseY) / oldScale;

                    zoomFactor = newZoom;
                    panX = mouseX - w / 2.0 - mouseLng * newScale;
                    panY = mouseY - h / 2.0 + mouseLat * newScale;
                } else {
                    zoomFactor = newZoom;
                }
                drawPreview();
            }
        });

        previewCanvas.setOnMousePressed(e -> {
            if (e.isShiftDown() || (graphicSelectBtn != null && graphicSelectBtn.isSelected())) {
                isSelectionDrag = true;
                selectionStartX = e.getX();
                selectionStartY = e.getY();
                selectionCurrentX = e.getX();
                selectionCurrentY = e.getY();
            } else {
                isSelectionDrag = false;
                dragStartX = e.getX();
                dragStartY = e.getY();
            }
        });

        previewCanvas.setOnMouseDragged(e -> {
            if (isSelectionDrag) {
                selectionCurrentX = e.getX();
                selectionCurrentY = e.getY();
            } else {
                double dx = e.getX() - dragStartX;
                double dy = e.getY() - dragStartY;
                panX += dx;
                panY += dy;
                dragStartX = e.getX();
                dragStartY = e.getY();
            }
            drawPreview();
        });

        previewCanvas.setOnMouseReleased(e -> {
            if (isSelectionDrag) {
                isSelectionDrag = false;
                double w = previewCanvas.getWidth();
                double h = previewCanvas.getHeight();
                double baseScale = Math.min(w / 360.0, h / 180.0);
                double scale = baseScale * zoomFactor;
                double offX = (w / 2.0) - (180.0) * scale + panX;
                double offY = (h / 2.0) - (90.0) * scale + panY;

                double x1 = Math.min(selectionStartX, selectionCurrentX);
                double x2 = Math.max(selectionStartX, selectionCurrentX);
                double y1 = Math.min(selectionStartY, selectionCurrentY);
                double y2 = Math.max(selectionStartY, selectionCurrentY);

                if (Math.abs(x2 - x1) > 5 && Math.abs(y2 - y1) > 5) {
                    double lon1 = Math.max(-180.0, Math.min(180.0, ((x1 - offX) / scale) - 180.0));
                    double lon2 = Math.max(-180.0, Math.min(180.0, ((x2 - offX) / scale) - 180.0));
                    double lat2 = Math.max(-90.0, Math.min(90.0, 90.0 - ((y1 - offY) / scale)));
                    double lat1 = Math.max(-90.0, Math.min(90.0, 90.0 - ((y2 - offY) / scale)));

                    if (minLatSpinner != null) minLatSpinner.getValueFactory().setValue(Math.round(lat1 * 10.0) / 10.0);
                    if (maxLatSpinner != null) maxLatSpinner.getValueFactory().setValue(Math.round(lat2 * 10.0) / 10.0);
                    if (minLngSpinner != null) minLngSpinner.getValueFactory().setValue(Math.round(lon1 * 10.0) / 10.0);
                    if (maxLngSpinner != null) maxLngSpinner.getValueFactory().setValue(Math.round(lon2 * 10.0) / 10.0);
                    if (clippingCheckBox != null) clippingCheckBox.setSelected(true);
                    if (graphicSelectBtn != null) graphicSelectBtn.setSelected(false);
                }
                drawPreview();
            }
        });

        previewCanvas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                zoomFactor = 1.0;
                panX = 0.0;
                panY = 0.0;
                drawPreview();
            }
        });

        canvasContainer.getChildren().add(previewCanvas);
        VBox.setVgrow(canvasContainer, Priority.ALWAYS);

        previewStatusLabel = new Label(I18n.getOrDefault("scenario.label.preview_precalc", "Pre-calculated initial distribution preview"));
        previewStatusLabel.getStyleClass().add("control-label");

        HBox legendBox = createLegend();
        root.getChildren().addAll(titleBox, controlBar, canvasContainer, legendBox, previewStatusLabel);
        return root;
    }

    private HBox legendItemsContainer;

    private HBox createLegend() {
        HBox legend = new HBox(12);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(6, 12, 6, 12));
        legend.getStyleClass().add("card-section");

        legendItemsContainer = new HBox(12);
        legendItemsContainer.setAlignment(Pos.CENTER);

        legend.getChildren().add(legendItemsContainer);
        updateBottomLegend();
        return legend;
    }

    private void generateProceduralPopulationDensity() {
        customDensityImage = null;
        if (radioProcDemo != null) radioProcDemo.setSelected(true);
        if (previewModeCombo != null) {
            previewModeCombo.getSelectionModel().select(0);
            updatePreviewTitleText();
            updateBottomLegend();
        }
        Scenario s = getScenario();
        if (s != null) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                org.ether.society.data.HistoricalMapGenerator.generateProceduralMapsForScenario(s);
            }).thenRun(() -> javafx.application.Platform.runLater(() -> {
                String b64 = s.getCustomDensityBase64();
                if (b64 != null && !b64.isBlank()) {
                    customDensityImage = org.ether.society.data.ImageMapLoader.base64PngToImage(b64);
                }
                if (currentPreviewCells != null && !currentPreviewCells.isEmpty()) {
                    distributeInitialPopulation(currentPreviewCells);
                }
                notifyParamChange();
                drawPreview();
            }));
        } else if (currentPreviewCells == null || currentPreviewCells.isEmpty()) {
            generatePreview();
        } else {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                distributeInitialPopulation(currentPreviewCells);
            }).thenRun(() -> javafx.application.Platform.runLater(this::drawPreview));
        }
    }

    private void generateProceduralCulturalTensors() {
        if (tensorProcRadios != null) {
            for (var entry : tensorProcRadios.entrySet()) {
                if (entry.getValue() != null) {
                    entry.getValue().setSelected(true);
                }
            }
        }
        customTensorImages.clear();
        customIsoglossImage = null;
        customKinshipImage = null;
        customRitualsImage = null;
        customSovereigntyImage = null;

        if (previewModeCombo != null) {
            int curSel = previewModeCombo.getSelectionModel().getSelectedIndex();
            int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
            if (curSel <= 0 || curSel > dims) {
                previewModeCombo.getSelectionModel().select(1); // Select first cultural tensor (isogloss)
            }
            updatePreviewTitleText();
            updateBottomLegend();
        }

        if (tensorFileLabels != null) {
            for (Integer idx : tensorFileLabels.keySet()) {
                updateTensorFileLabel(idx);
            }
        }
        notifyParamChange();

        Scenario s = getScenario();
        if (s == null) return;
        if (btnGenerateProceduralTensorsSection != null) btnGenerateProceduralTensorsSection.setDisable(true);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            // Persist maps to disk for the exact scenario year (on-demand epoch generation)
            org.ether.society.data.HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(s);
            // Also generate base64 tensors for the UI preview
            org.ether.society.data.HistoricalMapGenerator.generateProceduralMapsForScenario(s);
        }).thenRun(() -> javafx.application.Platform.runLater(() -> {
            if (btnGenerateProceduralTensorsSection != null) btnGenerateProceduralTensorsSection.setDisable(false);
            int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
            for (int i = 0; i < dims; i++) {
                String b64 = s.getCustomTensorMapBase64(i);
                if (b64 != null && !b64.isBlank()) {
                    Image img = org.ether.society.data.ImageMapLoader.base64PngToImage(b64);
                    if (img != null) {
                        customTensorImages.put(i, img);
                        if (i == 0) customIsoglossImage = img;
                        if (i == 1) customKinshipImage = img;
                        if (i == 2) customRitualsImage = img;
                        if (i == 3) customSovereigntyImage = img;
                    }
                }
                updateTensorFileLabel(i);
            }
            notifyParamChange();
            drawPreview();
        }));
    }

    private void updatePreviewModesCombo() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (previewModeCombo == null) return;
        int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
        int currentSelectionIndex = previewModeCombo.getSelectionModel().getSelectedIndex();

        java.util.List<String> items = new java.util.ArrayList<>();
        int num = 1;
        items.add(num++ + ". " + I18n.getOrDefault("scenario.preview.mode.density", "📊 Relief & Demographic Density (Agent Nodes T₀)"));

        for (int i = 0; i < dims; i++) {
            items.add(num++ + ". " + getCulturalTensorPreviewName(i));
        }

        items.add(I18n.getOrDefault("scenario.preview.separator.derived", "────────── CALQUES DÉDUITS & DYNAMIQUES ──────────"));
        items.add(num++ + ". " + I18n.getOrDefault("scenario.preview.mode.capital",   "ðŸ› ï¸ Initial Physical Capital K(x) [kg/capita] (Derived)"));
        items.add(num++ + ". " + I18n.getOrDefault("scenario.preview.mode.energy",    "⚡ Initial Energy Stock E(x) [MJ/capita] (Derived)"));
        items.add(num++ + ". " + I18n.getOrDefault("scenario.preview.mode.food",      "🌾 Food Reserves F(x) [Months] (Derived)"));
        items.add(num++ + ". " + I18n.getOrDefault("scenario.preview.mode.info",      "🧠 Information Capital & Knowledge I(x) [Bits/capita] (Derived)"));
        items.add(num++ + ". " + I18n.getOrDefault("scenario.preview.mode.footprint", "⚠️ï¸ Demographic Footprint & Malthusian Tension (Derived)"));
        items.add(num   + ". " + I18n.getOrDefault("scenario.preview.mode.friction",  "🧱 Border Friction Gradient σ_friction (Derived)"));

        previewModeCombo.getItems().setAll(items);

        if (currentSelectionIndex >= 0 && currentSelectionIndex < previewModeCombo.getItems().size()) {
            previewModeCombo.getSelectionModel().select(currentSelectionIndex);
        } else {
            previewModeCombo.getSelectionModel().select(0);
        }
    }

    private String getCulturalTensorPreviewName(int index) {
        if (customTensorNames.containsKey(index) && !customTensorNames.get(index).isBlank()) {
            return "🧬 " + I18n.getOrDefault("scenario.tensor.custom.preview_prefix", "Tensor ") + (index + 1) + " : " + customTensorNames.get(index);
        }
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.1.preview", "📜 Tensor 1: Isoglosses & Linguistic Continua (Languages)");
            case 1 -> I18n.getOrDefault("scenario.tensor.2.preview", "ðŸ› Tensor 2: Kinship & Clan Structures (Kinship)");
            case 2 -> I18n.getOrDefault("scenario.tensor.3.preview", "🔮 Tensor 3: Rituals, Beliefs & Sacred (Asabiyyah)");
            case 3 -> I18n.getOrDefault("scenario.tensor.4.preview", "👑 Tensor 4: Politico-Military Sovereignty & Capitals");
            case 4 -> I18n.getOrDefault("scenario.tensor.5.preview", "ðŸº Tensor 5: Tooling, Materiality & Technologies (Artifacts)");
            case 5 -> I18n.getOrDefault("scenario.tensor.6.preview", "ðŸ« Tensor 6: Corridors & Trade Networks (Economic Routes)");
            case 6 -> I18n.getOrDefault("scenario.tensor.7.preview", "⚖ Tensor 7: Institutional Complexity & Norms (Seshat & Law)");
            case 7 -> I18n.getOrDefault("scenario.tensor.8.preview", "⚠️ï¸ Tensor 8: Ecological Footprint & Malthusian Tension (Degradation)");
            case 8 -> I18n.getOrDefault("scenario.tensor.9.preview", "🧬 Tensor 9: Pathogen Immunity & Health Memory (Epidemiology)");
            default -> I18n.getOrDefault("scenario.tensor.custom.preview_prefix", "🧬 Tensor ") + (index + 1) + I18n.getOrDefault("scenario.tensor.custom.preview_mid", " : Cultural Substrate ") + (index + 1);
        };
    }

    private void updatePreviewTitleText() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (previewTitleLabel == null) return;
        previewTitleLabel.setText(I18n.getOrDefault("scenario.title.right_view", "ðŸ—ºï¸ Resource Cartography & Display"));
    }

    private void updateBottomLegend() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (legendItemsContainer == null) return;
        legendItemsContainer.getChildren().clear();

        int idx = previewModeCombo != null ? previewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        String mode = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue().toLowerCase() : "";
        int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;

        Color[] colors;
        String[] labels;
        String[] fullTooltips;

        if (idx == 0) {
            // Mode 0: Relief & Demographic Density
            colors = new Color[]{
                Color.rgb(30, 95, 165), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
            };
            labels = new String[]{
                I18n.getOrDefault("setup.legend.density.0", "Inhabité (0 hab/km²)"),
                I18n.getOrDefault("setup.legend.density.1", "Faible (1–50 hab/km²)"),
                I18n.getOrDefault("setup.legend.density.2", "Moyenne (50–500)"),
                I18n.getOrDefault("setup.legend.density.3", "Élevée (500–2.5k)"),
                I18n.getOrDefault("setup.legend.density.4", "Métropole (> 2.5k)")
            };
            fullTooltips = new String[]{
                I18n.getOrDefault("setup.legend.density.0.desc", "Zone Inhabitée : 0 hab/km² (Océans, déserts, haute montagne)"),
                I18n.getOrDefault("setup.legend.density.1.desc", "Densité Faible : 1 à 50 hab/km² (Campagnes, tribus nomades)"),
                I18n.getOrDefault("setup.legend.density.2.desc", "Densité Moyenne : 50 à 500 hab/km² (Bourgades & vallées agricoles)"),
                I18n.getOrDefault("setup.legend.density.3.desc", "Densité Élevée / Cité : 500 à 2 500 hab/km² (Centres urbains régionaux)"),
                I18n.getOrDefault("setup.legend.density.4.desc", "Métropole / Megapole : > 2 500 hab/km² (Grandes capitales historiques)")
            };
        } else if (idx >= 1 && idx <= dims) {
            int tIdx = idx - 1;
            if (tIdx == 0) {
                // Tensor 1: Isoglosses & Languages
                colors = new Color[]{
                    Color.rgb(30, 95, 165), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.dialect.0", "Dialecte Archaïque (C₀=0)"),
                    I18n.getOrDefault("setup.legend.dialect.1", "Foyer Prosodique (0.25)"),
                    I18n.getOrDefault("setup.legend.dialect.2", "Isoglosse Médiane (0.50)"),
                    I18n.getOrDefault("setup.legend.dialect.3", "Innovations Substratiques (0.75)"),
                    I18n.getOrDefault("setup.legend.dialect.4", "Dialecte Exogène (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.dialect.0.desc", "Dialecte Archaïque : Formes originelles non diffusées"),
                    I18n.getOrDefault("setup.legend.dialect.1.desc", "Foyer Prosodique : Zone d'expansion dialectale secondaire"),
                    I18n.getOrDefault("setup.legend.dialect.2.desc", "Isoglosse Médiane : Zone de frontière linguistique et bilinguisme"),
                    I18n.getOrDefault("setup.legend.dialect.3.desc", "Innovations Substratiques : Lexique technique ou grammatical rénové"),
                    I18n.getOrDefault("setup.legend.dialect.4.desc", "Dialecte Exogène / Innovant : Standard de communication émergent")
                };
            } else if (tIdx == 1) {
                // Tensor 2: Kinship & Clan Structures
                colors = new Color[]{
                    Color.rgb(56, 189, 248), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(167, 139, 250)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.kinship.0", "Famille Nucléaire (Câ‚=0)"),
                    I18n.getOrDefault("setup.legend.kinship.1", "Lignée Élargie (0.25)"),
                    I18n.getOrDefault("setup.legend.kinship.2", "Matriarcat Lacustre (0.50)"),
                    I18n.getOrDefault("setup.legend.kinship.3", "Patriarcat Hiérarchique (0.75)"),
                    I18n.getOrDefault("setup.legend.kinship.4", "Confédération Tribale (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.kinship.0.desc", "Famille Nucléaire : Cellule parentale autonome de base"),
                    I18n.getOrDefault("setup.legend.kinship.1.desc", "Lignée Élargie : Entraide inter-générationnelle et clans d'alliance"),
                    I18n.getOrDefault("setup.legend.kinship.2.desc", "Matriarcat Lacustre : Filiations matrilinéaires et terres collectives"),
                    I18n.getOrDefault("setup.legend.kinship.3.desc", "Patriarcat Hiérarchique : Structure agnatique et chefferies martiales"),
                    I18n.getOrDefault("setup.legend.kinship.4.desc", "Confédération Tribale : Assemblée de clans fédérés à grande échelle")
                };
            } else if (tIdx == 2) {
                // Tensor 3: Rituals & Asabiyyah
                colors = new Color[]{
                    Color.rgb(14, 165, 233), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(225, 29, 72)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.rituals.0", "Animisme Local (C₂=0)"),
                    I18n.getOrDefault("setup.legend.rituals.1", "Cultes Civiques (0.25)"),
                    I18n.getOrDefault("setup.legend.rituals.2", "Polythéisme (0.50)"),
                    I18n.getOrDefault("setup.legend.rituals.3", "Asabiyyah Élevée (0.75)"),
                    I18n.getOrDefault("setup.legend.rituals.4", "Dogme Transcendant (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.rituals.0.desc", "Animisme Local : Croyances de terroirs et esprit des éléments"),
                    I18n.getOrDefault("setup.legend.rituals.1.desc", "Cultes Civiques : Rites urbains d'intégration communautaire"),
                    I18n.getOrDefault("setup.legend.rituals.2.desc", "Polythéisme : Panthéons structurés et clergés régionaux"),
                    I18n.getOrDefault("setup.legend.rituals.3.desc", "Asabiyyah Élevée : Forte solidarité tribale (Cohésion d'Ibn Khaldoun)"),
                    I18n.getOrDefault("setup.legend.rituals.4.desc", "Dogme Transcendant : Monothéisme ou idéologie universaliste")
                };
            } else if (tIdx == 3) {
                // Tensor 4: Sovereignty & Polities
                colors = new Color[]{
                    Color.rgb(30, 95, 165), Color.rgb(16, 185, 129), Color.rgb(245, 158, 11), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.sovereignty.0", "Zone Franche (C₃=0)"),
                    I18n.getOrDefault("setup.legend.sovereignty.1", "Cité-État Libre (0.25)"),
                    I18n.getOrDefault("setup.legend.sovereignty.2", "Principauté (0.50)"),
                    I18n.getOrDefault("setup.legend.sovereignty.3", "Empire Centralisé (0.75)"),
                    I18n.getOrDefault("setup.legend.sovereignty.4", "Capitale Core (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.sovereignty.0.desc", "Zone Franche / Nomade : Absence de souveraineté étatique formalisée"),
                    I18n.getOrDefault("setup.legend.sovereignty.1.desc", "Cité-État Libre : Autonomie municipale et hinterland restreint"),
                    I18n.getOrDefault("setup.legend.sovereignty.2.desc", "Principauté Régionale : Contrôle féodal ou provincial intermédiaire"),
                    I18n.getOrDefault("setup.legend.sovereignty.3.desc", "Empire Centralisé : Administration unifiée et prélèvement fiscal"),
                    I18n.getOrDefault("setup.legend.sovereignty.4.desc", "Capitale Core : Foyer du pouvoir politique et militaire suprême")
                };
            } else if (tIdx == 4) {
                // Tensor 5: Tooling, Materiality & Technologies (Strict Grayscale matching raster map)
                colors = new Color[]{
                    Color.rgb(20, 20, 20), Color.rgb(80, 80, 80), Color.rgb(140, 140, 140), Color.rgb(200, 200, 200), Color.rgb(255, 255, 255)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.tech.0", "Lithique / Paléo (C₄=0)"),
                    I18n.getOrDefault("setup.legend.tech.1", "Céramique / Néolithique (0.25)"),
                    I18n.getOrDefault("setup.legend.tech.2", "Bronze / Métallurgie (0.50)"),
                    I18n.getOrDefault("setup.legend.tech.3", "Fer & Mécanique (0.75)"),
                    I18n.getOrDefault("setup.legend.tech.4", "Industrie & Numérique (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.tech.0.desc", "Industrie Lithique : Taille du silex, os poli, bois et cuir"),
                    I18n.getOrDefault("setup.legend.tech.1.desc", "Céramique & Néolithisation : Poteaux, cuisson de terre, faux et meules"),
                    I18n.getOrDefault("setup.legend.tech.2.desc", "Métallurgie du Cuivre et Bronze : Fours de réduction, alliages, soc d'araire"),
                    I18n.getOrDefault("setup.legend.tech.3.desc", "Âge du Fer & Machines Simples : Hauts fourneaux, moulins, engrenages"),
                    I18n.getOrDefault("setup.legend.tech.4.desc", "Révolution Industrielle & Digitale : Vapeur, réseaux électriques, automates")
                };
            } else if (tIdx == 5) {
                // Tensor 6: Corridors & Trade Networks
                colors = new Color[]{
                    Color.rgb(30, 41, 59), Color.rgb(180, 83, 9), Color.rgb(217, 119, 6), Color.rgb(234, 88, 12), Color.rgb(13, 148, 136)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.trade.0", "Enclave Isolée (C₅=0)"),
                    I18n.getOrDefault("setup.legend.trade.1", "Pistes Locales (0.25)"),
                    I18n.getOrDefault("setup.legend.trade.2", "Caravanes Terrestres (0.50)"),
                    I18n.getOrDefault("setup.legend.trade.3", "Voies Fluviales (0.75)"),
                    I18n.getOrDefault("setup.legend.trade.4", "Hubs Maritimes (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.trade.0.desc", "Enclave Isolée : Autarcie locale sans axe d'échange pérenne"),
                    I18n.getOrDefault("setup.legend.trade.1.desc", "Pistes Pédestres Locales : Sentiers de troc de proximité"),
                    I18n.getOrDefault("setup.legend.trade.2.desc", "Caravanes Terrestres : Routes de la Soie, pistes trans-sahariennes"),
                    I18n.getOrDefault("setup.legend.trade.3.desc", "Voies Navigables & Fluviales : Transports massifs par fleuves et canaux"),
                    I18n.getOrDefault("setup.legend.trade.4.desc", "Hubs Maritimes & Mondiaux : Ports hauturiers et corridors mondialisés")
                };
            } else if (tIdx == 6) {
                // Tensor 7: Institutional Complexity & Norms (Strict Grayscale matching raster map)
                colors = new Color[]{
                    Color.rgb(20, 20, 20), Color.rgb(80, 80, 80), Color.rgb(140, 140, 140), Color.rgb(200, 200, 200), Color.rgb(255, 255, 255)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.inst.0", "Coutume Orale (C₆=0)"),
                    I18n.getOrDefault("setup.legend.inst.1", "Tribunaux Locaux (0.25)"),
                    I18n.getOrDefault("setup.legend.inst.2", "Code Juridique (0.50)"),
                    I18n.getOrDefault("setup.legend.inst.3", "Bureaucratie Fiscale (0.75)"),
                    I18n.getOrDefault("setup.legend.inst.4", "État de Droit (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.inst.0.desc", "Coutume Orale : Résolution informelle des conflits par les anciens"),
                    I18n.getOrDefault("setup.legend.inst.1.desc", "Juridictions Locales : Assemblées coutumières et magistrats de cité"),
                    I18n.getOrDefault("setup.legend.inst.2.desc", "Code Écrit : Corpus légal unifié (ex: Code d'Hammurabi, Droit Romain)"),
                    I18n.getOrDefault("setup.legend.inst.3.desc", "Bureaucratie Centralisée : Prélèvement cadastral, ministères et fonction publique"),
                    I18n.getOrDefault("setup.legend.inst.4.desc", "État de Droit Constitutionnel : Séparation des pouvoirs, institutions impersonnelles")
                };
            } else if (tIdx == 7) {
                // Tensor 8: Ecological Footprint & Environmental Tension (Strict Grayscale matching raster map)
                colors = new Color[]{
                    Color.rgb(20, 20, 20), Color.rgb(80, 80, 80), Color.rgb(140, 140, 140), Color.rgb(200, 200, 200), Color.rgb(255, 255, 255)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.eco.0", "Biome Vierge (C₇=0)"),
                    I18n.getOrDefault("setup.legend.eco.1", "Pression Modérée (0.25)"),
                    I18n.getOrDefault("setup.legend.eco.2", "Surexploitation (0.50)"),
                    I18n.getOrDefault("setup.legend.eco.3", "Tension Critique (0.75)"),
                    I18n.getOrDefault("setup.legend.eco.4", "Effondrement (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.eco.0.desc", "Biome Intact : Écosystèmes à l'équilibre sans perturbation anthropique"),
                    I18n.getOrDefault("setup.legend.eco.1.desc", "Pression Légère : Forêt gérée, chasse et élevage durable"),
                    I18n.getOrDefault("setup.legend.eco.2.desc", "Surexploitation Agricole : Déforestation, érosion des sols, baisse des rendements"),
                    I18n.getOrDefault("setup.legend.eco.3.desc", "Tension Malthusienne Sévère : Pénuries de bois, raréfaction du gibier et des nappes"),
                    I18n.getOrDefault("setup.legend.eco.4.desc", "Effondrement Écologique : Désertification irréversible et disette systémique")
                };
            } else if (tIdx == 8) {
                // Tensor 9: Pathogen Immunity & Health Memory (Strict Grayscale matching raster map)
                colors = new Color[]{
                    Color.rgb(20, 20, 20), Color.rgb(80, 80, 80), Color.rgb(140, 140, 140), Color.rgb(200, 200, 200), Color.rgb(255, 255, 255)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.pathogen.0", "Naïf / Vulnérable (C₈=0)"),
                    I18n.getOrDefault("setup.legend.pathogen.1", "Endémie Locale (0.25)"),
                    I18n.getOrDefault("setup.legend.pathogen.2", "Résistance Acquise (0.50)"),
                    I18n.getOrDefault("setup.legend.pathogen.3", "Mémoire Élevée (0.75)"),
                    I18n.getOrDefault("setup.legend.pathogen.4", "Bouclier Immunitaire (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.pathogen.0.desc", "Population Naïve : Aucune immunité préalable (vulnérabilité maximale aux chocs microbiens)"),
                    I18n.getOrDefault("setup.legend.pathogen.1.desc", "Endémie Modérée : Présence de zoonoses locales stabilisées"),
                    I18n.getOrDefault("setup.legend.pathogen.2.desc", "Résistance Acquise : Sélection adaptative et résilience immunitaire face aux épidémies courantes"),
                    I18n.getOrDefault("setup.legend.pathogen.3.desc", "Mémoire Épidémique Élevée : Forte diversité d'anticorps dans les grands réseaux urbains"),
                    I18n.getOrDefault("setup.legend.pathogen.4.desc", "Bouclier Sanitaire & Médical : Mesures prophylactiques, vaccins et structures hospitalières")
                };
            } else {
                // Extensible tensor >= 9 (Strict Grayscale matching raster map)
                colors = new Color[]{
                    Color.rgb(20, 20, 20), Color.rgb(80, 80, 80), Color.rgb(140, 140, 140), Color.rgb(200, 200, 200), Color.rgb(255, 255, 255)
                };
                labels = new String[]{"0.0", "0.25", "0.50", "0.75", "1.0"};
                fullTooltips = new String[]{"C = 0.0", "C = 0.25", "C = 0.50", "C = 0.75", "C = 1.0"};
            }
        } else {
            // Derived layers
            int derivedOffset = idx - (dims + 1);
            if (derivedOffset == 1 || mode.contains("capital") || mode.contains("k(x)")) {
                // Derived Capital K(x)
                colors = new Color[]{
                    Color.rgb(30, 58, 138), Color.rgb(6, 182, 212), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(239, 68, 68)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.cap.0", "Lithique (< 10 kg/hab)"),
                    I18n.getOrDefault("setup.legend.cap.1", "Artisanal (10-100 kg)"),
                    I18n.getOrDefault("setup.legend.cap.2", "Manufacturier (100-1k)"),
                    I18n.getOrDefault("setup.legend.cap.3", "Industriel (1k-10k)"),
                    I18n.getOrDefault("setup.legend.cap.4", "Haute Densité (> 10k)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.cap.0.desc", "Capital Physique Primitif : Outils individuels légers"),
                    I18n.getOrDefault("setup.legend.cap.1.desc", "Capital Artisanal : Ateliers, animaux de trait, charrues"),
                    I18n.getOrDefault("setup.legend.cap.2.desc", "Capital Manufacturier : Forges, moulins hydrauliques, navires"),
                    I18n.getOrDefault("setup.legend.cap.3.desc", "Capital Industriel : Machines à vapeur, voies ferrées, usines"),
                    I18n.getOrDefault("setup.legend.cap.4.desc", "Infrastructures Avancées : Réseaux électriques, télécoms, centres logistiques")
                };
            } else if (derivedOffset == 2 || mode.contains("énergétique") || mode.contains("energy") || mode.contains("energie") || mode.contains("energético") || mode.contains("e(x)")) {
                // Derived Energy E(x)
                colors = new Color[]{
                    Color.rgb(69, 26, 3), Color.rgb(180, 83, 9), Color.rgb(234, 88, 12), Color.rgb(250, 204, 21), Color.rgb(254, 240, 138)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.energy.0", "Biomasse (< 10 MJ/hab)"),
                    I18n.getOrDefault("setup.legend.energy.1", "Traction (10-50 MJ)"),
                    I18n.getOrDefault("setup.legend.energy.2", "Hydraulique/Charbon (50-200)"),
                    I18n.getOrDefault("setup.legend.energy.3", "Fossile (200-500 MJ)"),
                    I18n.getOrDefault("setup.legend.energy.4", "Électrique (> 500 MJ)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.energy.0.desc", "Régime Biomasse : Chaleur du bois et travail musculaire"),
                    I18n.getOrDefault("setup.legend.energy.1.desc", "Régime Traction Animale : Attelages, bœufs et chevaux"),
                    I18n.getOrDefault("setup.legend.energy.2.desc", "Régime Mécanique : Énergie hydraulique, éolienne et débuts du charbon"),
                    I18n.getOrDefault("setup.legend.energy.3.desc", "Régime Hydrocarbures : Pétrole, gaz et thermodynamique industrielle"),
                    I18n.getOrDefault("setup.legend.energy.4.desc", "Régime Électrique Massif : Réseaux de puissance et transition énergétique")
                };
            } else if (derivedOffset == 3 || mode.contains("alimentaires") || mode.contains("food") || mode.contains("nahrung") || mode.contains("alimentos") || mode.contains("f(x)")) {
                // Derived Food F(x)
                colors = new Color[]{
                    Color.rgb(163, 230, 53), Color.rgb(132, 204, 22), Color.rgb(34, 197, 94), Color.rgb(5, 150, 105), Color.rgb(6, 78, 59)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.food.0", "< 1 Mois (Critique)"),
                    I18n.getOrDefault("setup.legend.food.1", "1–3 Mois (Faible)"),
                    I18n.getOrDefault("setup.legend.food.2", "3–6 Mois (Moyen)"),
                    I18n.getOrDefault("setup.legend.food.3", "6–12 Mois (Sécurisé)"),
                    I18n.getOrDefault("setup.legend.food.4", "> 12 Mois (Abondance)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.food.0.desc", "Réserves Critiques : Vulnérabilité immédiate à la moindre mauvaise récolte"),
                    I18n.getOrDefault("setup.legend.food.1.desc", "Réserves de Subsistance : Stocks saisonniers d'appoint"),
                    I18n.getOrDefault("setup.legend.food.2.desc", "Greniers Traditionnels : Capacité de soudure inter-annuelle"),
                    I18n.getOrDefault("setup.legend.food.3.desc", "Réserves Stratégiques : Silos régionaux prévenant toute disette"),
                    I18n.getOrDefault("setup.legend.food.4.desc", "Surplus Systémique : Chaînes logistiques agroalimentaires pérennes")
                };
            } else if (derivedOffset == 4 || mode.contains("informationnel") || mode.contains("info") || mode.contains("i(x)")) {
                // Derived Info I(x)
                colors = new Color[]{
                    Color.rgb(55, 48, 163), Color.rgb(124, 58, 237), Color.rgb(219, 39, 119), Color.rgb(6, 182, 212), Color.rgb(224, 242, 254)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.info.0", "Oral (< 10 bits/hab)"),
                    I18n.getOrDefault("setup.legend.info.1", "Écrit & Parchemin (10-100)"),
                    I18n.getOrDefault("setup.legend.info.2", "Imprimerie (100-1k)"),
                    I18n.getOrDefault("setup.legend.info.3", "Médias Masse (1k-10k)"),
                    I18n.getOrDefault("setup.legend.info.4", "Numérique (> 10k)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.info.0.desc", "Tradition Orale : Savoirs transmis par la mémoire et le chant"),
                    I18n.getOrDefault("setup.legend.info.1.desc", "Écrit & Manuscrits : Enregistrement sur argile, papyrus et parchemins"),
                    I18n.getOrDefault("setup.legend.info.2.desc", "Imprimerie Mécanique : Diffusion élargie des traités et encyclopédies"),
                    I18n.getOrDefault("setup.legend.info.3.desc", "Télécommunications : Presse quotidienne, télégraphe, radio et cinéma"),
                    I18n.getOrDefault("setup.legend.info.4.desc", "Société Numérique : Internet, calcul haute performance et bases de données")
                };
            } else if (derivedOffset == 5 || mode.contains("empreinte") || mode.contains("footprint") || mode.contains("fußabdruck") || mode.contains("huella") || mode.contains("malthus")) {
                // Derived Footprint & Malthusian Tension
                colors = new Color[]{
                    Color.rgb(16, 185, 129), Color.rgb(132, 204, 22), Color.rgb(245, 158, 11), Color.rgb(239, 68, 68), Color.rgb(127, 29, 29)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.eco.0", "Biome Vierge (C₇=0)"),
                    I18n.getOrDefault("setup.legend.eco.1", "Pression Modérée (0.25)"),
                    I18n.getOrDefault("setup.legend.eco.2", "Surexploitation (0.50)"),
                    I18n.getOrDefault("setup.legend.eco.3", "Tension Critique (0.75)"),
                    I18n.getOrDefault("setup.legend.eco.4", "Effondrement (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.eco.0.desc", "Biome Intact : Écosystèmes à l'équilibre sans perturbation anthropique"),
                    I18n.getOrDefault("setup.legend.eco.1.desc", "Pression Légère : Forêt gérée, chasse et élevage durable"),
                    I18n.getOrDefault("setup.legend.eco.2.desc", "Surexploitation Agricole : Déforestation, érosion des sols, baisse des rendements"),
                    I18n.getOrDefault("setup.legend.eco.3.desc", "Tension Malthusienne Sévère : Pénuries de bois, raréfaction du gibier et des nappes"),
                    I18n.getOrDefault("setup.legend.eco.4.desc", "Effondrement Écologique : Désertification irréversible et disette systémique")
                };
            } else if (derivedOffset == 6 || mode.contains("friction") || mode.contains("reibung") || mode.contains("fricción")) {
                // Derived Border Friction
                colors = new Color[]{
                    Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68), Color.rgb(167, 139, 250)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.friction.0", "Plaine (Faible σ)"),
                    I18n.getOrDefault("setup.legend.friction.1", "Colline / Fleuve (Moyen)"),
                    I18n.getOrDefault("setup.legend.friction.2", "Montagne (Fort)"),
                    I18n.getOrDefault("setup.legend.friction.3", "Désert / Extrême"),
                    I18n.getOrDefault("setup.legend.friction.4", "Barrière Absolue")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.friction.0.desc", "Plaine Alluviale : Friction minimale à la mobilité (σ ≈ 0.1)"),
                    I18n.getOrDefault("setup.legend.friction.1.desc", "Colline / Fleuve : Obstacle naturel mineur franchissable"),
                    I18n.getOrDefault("setup.legend.friction.2.desc", "Chaîne Montagneuse : Transports ralentis, cols escarpés"),
                    I18n.getOrDefault("setup.legend.friction.3.desc", "Désert Extrême : Zone aride exigeant des convois spécialisés"),
                    I18n.getOrDefault("setup.legend.friction.4.desc", "Haute Altitude / Falaise : Barrière infranchissable pour les armées")
                };
            } else {
                // Fallback default
                colors = new Color[]{
                    Color.rgb(30, 95, 165), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
                };
                labels = new String[]{"0.0", "0.25", "0.50", "0.75", "1.0"};
                fullTooltips = new String[]{"0.0", "0.25", "0.50", "0.75", "1.0"};
            }
        }

        for (int i = 0; i < labels.length; i++) {
            javafx.scene.shape.Rectangle colorBox = new javafx.scene.shape.Rectangle(14, 10);
            colorBox.setFill(colors[i]);
            colorBox.setStroke(Color.GRAY);
            colorBox.setArcWidth(3);
            colorBox.setArcHeight(3);

            Label lbl = new Label(labels[i]);
            lbl.getStyleClass().add("control-label");
            lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");

            HBox item = new HBox(4, colorBox, lbl);
            item.setAlignment(Pos.CENTER);
            Tooltip.install(item, new Tooltip(fullTooltips[i]));
            legendItemsContainer.getChildren().add(item);
        }
    }

    private void loadCustomDensityMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("scenario.title.load_density_dialog", "Load Population Density Map (PNG/JPEG)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                Image img = new Image(new FileInputStream(file));
                org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(img);
                if (!val.valid()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(I18n.getOrDefault("scenario.dialog.invalid_map_title", "Carte Incompatible ou Invalide"));
                    alert.setHeaderText(I18n.getOrDefault("scenario.dialog.invalid_map_header", "Fichier de carte non supporté ou illisible"));
                    alert.setContentText(file.getName() + " :\n" + val.message() + "\n\n" + I18n.getOrDefault("scenario.dialog.invalid_map_hint", "Veuillez fournir une image raster valide (PNG/JPG) en projection équirectangulaire (2:1)."));
                    alert.showAndWait();
                    return;
                }
                customDensityImage = img;
                densityMapFileLabel.setText(String.format("📷 %s (%dx%d)", file.getName(), val.width(), val.height()));
                if (radioImportDemo != null) radioImportDemo.setSelected(true);
                updateDemoCompatibilityDisplay();
                if (currentPreviewCells != null) {
                    distributeInitialPopulation(currentPreviewCells);
                    drawPreview();
                }
            } catch (Exception ex) {
                logger.error("Failed to load custom density map", ex);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur de chargement: " + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void updateDemoCompatibilityDisplay() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (demoCompatibilityLabel == null) return;
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        String planetName = p != null ? p.name() : "Standard";
        if (customDensityImage != null) {
            org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(customDensityImage);
            if (!val.valid()) {
                demoCompatibilityLabel.setText(String.format(I18n.getOrDefault("scenario.demo.map_invalid", "⚠️ï¸ Carte démographique incompatible : %s"), val.message()));
                demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            } else {
                demoCompatibilityLabel.setText(String.format(I18n.getOrDefault("scenario.demo.map_loaded", "✅ Map loaded and compatible with selected Tab 1 world (%s)"), planetName));
                demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
            }
        } else {
            demoCompatibilityLabel.setText(I18n.getOrDefault("scenario.demo.no_map", "ðŸª No external map loaded — Procedural mode active"));
            demoCompatibilityLabel.getStyleClass().add("subcard-status-muted");
            demoCompatibilityLabel.setStyle("-fx-font-size: 11px;");
        }
    }

    private void exportDensityMap() {
        if (previewCanvas == null) return;
        int w = 2048;
        int h = 1024;
        WritableImage image = new WritableImage(w, h);
        PixelWriter pw = image.getPixelWriter();

        if (customDensityImage != null && customDensityImage.getWidth() > 0) {
            PixelReader pr = customDensityImage.getPixelReader();
            double srcW = customDensityImage.getWidth();
            double srcH = customDensityImage.getHeight();
            for (int y = 0; y < h; y++) {
                int sy = (int) Math.clamp((y / (double) h) * srcH, 0, srcH - 1);
                for (int x = 0; x < w; x++) {
                    int sx = (int) Math.clamp((x / (double) w) * srcW, 0, srcW - 1);
                    pw.setColor(x, y, pr.getColor(sx, sy));
                }
            }
        } else if (currentPreviewCells != null && !currentPreviewCells.isEmpty()) {
            double minLat = -90, maxLat = 90;
            double minLng = -180, maxLng = 180;
            double scaleX = w / (maxLng - minLng);
            double scaleY = h / (maxLat - minLat);

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    pw.setColor(x, y, Color.rgb(15, 23, 42));
                }
            }

            // Iterate over spatial cell domains and apply localized cellular state transformations
            for (H3Cell c : currentPreviewCells) {
                int px = (int) ((c.getLongitude() - minLng) * scaleX);
                int py = (int) ((maxLat - c.getLatitude()) * scaleY);
                if (px >= 0 && px < w && py >= 0 && py < h) {
                    pw.setColor(px, py, getReliefAndDensityColor(c));
                }
            }
        } else {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    pw.setColor(x, y, Color.rgb(15, 23, 42));
                }
            }
        }
        WindowUtils.exportImageWithChooser(getScene() != null ? getScene().getWindow() : null, image,
                "ether-population-density.png",
                I18n.getOrDefault("scenario.title.export_density_dialog", "Export Population Density Map (PNG / JPEG)"));
    }

    private void showDensityImportFormatHelp() {
        WindowUtils.showScrollableInfoDialog(
                I18n.getOrDefault("scenario.dialog.density_help_title", "Spécifications de la Carte de Densité"),
                I18n.getOrDefault("scenario.dialog.density_help_header", "Formats d'Image Supportés pour l'Import de Densité Démographique"),
                "Vous pouvez importer une carte de répartition démographique sous forme d'image PNG/JPEG au ratio 2:1 (ex: 2048x1024 pixels en projection équirectangulaire) :\n\n" +
                "1. CARTE DE DENSITÉ NIVEAUX DE GRIS :\n" +
                "   • Noir (0) = 0 hab/km² (Zone inhabitée / désertique / aquatique)\n" +
                "   • Blanc (255) = Densité maximale d'habitation (Foyer urbain ou métropole)\n\n" +
                "2. INTÉGRATION AVEC LE TERRAIN ET L'ÉCOLOGIE :\n" +
                "   • Les zones d'eau (océans/mers) définies dans l'onglet 1 sont automatiquement filtrées pour éviter l'apparition de populations en pleine mer.\n" +
                "   • La population totale paramétrée dans le spinner est distribuée au prorata de la luminosité de chaque pixel."
        );
    }

    private static final java.util.Map<String, List<H3Cell>> previewMeshCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong previewSeq = new java.util.concurrent.atomic.AtomicLong(0);

    private void generatePreview() {
        if (previewStatusLabel != null) {
            previewStatusLabel.setText(I18n.getOrDefault("scenario.status.calc_preview", "⚡ Calculating demographic preview in background..."));
        }
        isPreviewGenerating.set(true);
        final long mySeq = previewSeq.incrementAndGet();

        new Thread(() -> {
            try {
                PlanetPreset cfg = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : null);
                if (cfg == null) cfg = PlanetPreset.EARTH_LIKE;

                String meshKey = cfg.name() + "_res3_" + cfg.seed();
                List<H3Cell> baseCells = previewMeshCache.get(meshKey);
                List<H3Cell> previewCells;

                if (baseCells == null) {
                    baseCells = new ProceduralGenerator().generatePlanet(
                            new PlanetPreset(
                                    cfg.name(), 3, cfg.radiusKm(), cfg.dayLengthHours(),
                                    cfg.axialTiltDegrees(), cfg.yearLengthDays(), cfg.distanceToSunAU(),
                                    cfg.solarLuminosity(), cfg.minAltitudeMeters(), cfg.maxAltitudeMeters(),
                                    cfg.averageTempC(), cfg.seed(), cfg.noiseFrequency(),
                                    cfg.noiseScale(), cfg.waterLevel(), cfg.temperatureGradient(),
                                    cfg.oxygenPercentage(), cfg.albedo(), cfg.atmospherePressureAtm(),
                                    cfg.isSatellite(), cfg.parentPlanetMassEarthMasses(),
                                    cfg.orbitalDistanceToParentKm(), cfg.co2Ppm(),
                                    cfg.seismicActivityLevel(), cfg.volcanicActivityLevel(),
                                    cfg.customElevBase64(), cfg.customBiomeBase64(), cfg.customResourceBase64(),
                                    cfg.customClimateBase64(), cfg.customRainfallBase64(), cfg.customSeasonalityBase64(),
                                    cfg.elevationUseImport(), cfg.elevationMapSource(),
                                    cfg.tempUseImport(), cfg.tempSource(), cfg.tempSeed(),
                                    cfg.precipUseImport(), cfg.precipSource(), cfg.precipSeed(),
                                    cfg.seasonUseImport(), cfg.seasonSource(), cfg.seasonSeed()
                            )
                    );
                    applyPresetMapToCells(baseCells, cfg);
                    previewMeshCache.put(meshKey, baseCells);
                }

                if (mySeq != previewSeq.get()) return;

                // Create working copy of preview cells to distribute population
                previewCells = new ArrayList<>(baseCells.size());
                for (H3Cell c : baseCells) {
                    H3Cell copy = new H3Cell(c.getH3Index(), c.getLatitude(), c.getLongitude());
                    copy.setElevation(c.getElevation());
                    copy.setBiome(c.getBiome());
                    copy.setTemperature(c.getTemperature());
                    copy.setRainfall(c.getRainfall());
                    previewCells.add(copy);
                }

                distributeInitialPopulation(previewCells);

                if (mySeq != previewSeq.get()) return;

                javafx.application.Platform.runLater(() -> {
                    if (mySeq != previewSeq.get()) return;
                    currentPreviewCells = previewCells;
                    isPreviewGenerating.set(false);
                    drawPreview();
                    if (previewStatusLabel != null) {
                        previewStatusLabel.setText(I18n.getOrDefault("scenario.status.preview_generated", "Preview generated: ") + previewCells.size() + I18n.getOrDefault("scenario.status.cells_suffix", " cells."));
                    }
                });
            } catch (Exception ex) {
                logger.error("Failed to generate preview", ex);
                javafx.application.Platform.runLater(() -> {
                    if (mySeq != previewSeq.get()) return;
                    isPreviewGenerating.set(false);
                    if (previewStatusLabel != null) {
                        previewStatusLabel.setText(I18n.getOrDefault("scenario.status.preview_error", "Preview error: ") + ex.getMessage());
                    }
                    drawPreview();
                });
            }
        }, "H3-Preview-Async-Thread").start();
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


    private Color getPreviewColorForCell(H3Cell c) {
        if (c == null) return Color.BLACK;

        int idx = previewModeCombo != null ? previewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        String mode = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue().toLowerCase() : "";

        double lat = c.getLatitude() != null ? c.getLatitude() : 0.0;
        double lon = c.getLongitude() != null ? c.getLongitude() : 0.0;

        double elev = c.getElevation() != null ? c.getElevation() : 0.0;
        boolean isLand = elev > 0.0 || (c.getBiome() != null && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN);

        if (idx == 0) {
            // Default Density & Relief Mode
            if (radioImportDemo != null && radioImportDemo.isSelected()) {
                if (customDensityImage != null && customDensityImage.getWidth() > 0) {
                    if (!isLand) return getReliefAndDensityColor(c);
                    Color customCol = sampleImageColorAtLatLon(customDensityImage, lat, lon);
                    if (customCol != null) {
                        double brightness = (customCol.getRed() + customCol.getGreen() + customCol.getBlue()) / 3.0;
                        if (Math.abs(customCol.getRed() - customCol.getGreen()) < 0.02 && Math.abs(customCol.getGreen() - customCol.getBlue()) < 0.02) {
                            Color heatCol;
                            if (brightness <= 0.02) heatCol = Color.rgb(30, 95, 165);       // Uninhabited
                            else if (brightness < 0.20) heatCol = Color.rgb(16, 185, 129);  // Low
                            else if (brightness < 0.45) heatCol = Color.rgb(234, 179, 8);   // Medium
                            else if (brightness < 0.75) heatCol = Color.rgb(249, 115, 22);  // High
                            else heatCol = Color.rgb(239, 68, 68);                          // Metropolis
                            return blendColors(getReliefAndDensityColor(c), heatCol, Math.max(0.65, brightness));
                        }
                        return customCol;
                    }
                }
                return getReliefAndDensityColor(c);
            }
            return getReliefAndDensityColor(c);
        }

        if (!isLand) {
            return getReliefAndDensityColor(c); // Strict Ocean Masking for all cultural and derived layers
        }

        int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
        if (idx >= 1 && idx <= dims) {
            int tIndex = idx - 1;
            boolean isImport = tensorImportRadios.containsKey(tIndex) && tensorImportRadios.get(tIndex).isSelected();
            if (isImport) {
                Image img = customTensorImages.get(tIndex);
                if (img != null && img.getWidth() > 0) {
                    Color customCol = sampleImageColorAtLatLon(img, lat, lon);
                    if (customCol != null) return customCol;
                }
                return getReliefAndDensityColor(c);
            }

            double p1 = tensorParam1Sliders.containsKey(tIndex) ? tensorParam1Sliders.get(tIndex).getValue() : 1.0;
            double p2 = tensorParam2Sliders.containsKey(tIndex) ? tensorParam2Sliders.get(tIndex).getValue() : 1.0;
            double p3 = tensorParam3Sliders.containsKey(tIndex) ? tensorParam3Sliders.get(tIndex).getValue() : 1.0;
            long seed = 11235L + tIndex * 11111L;
            if (tensorSeedFields.containsKey(tIndex)) {
                try { seed = Long.parseLong(tensorSeedFields.get(tIndex).getText().trim()); } catch (Exception ignored) {}
            }
            return evaluateProceduralTensorColor(tIndex, lat, lon, elev, p1, p2, p3, seed);
        }

        // Calques déduits & physiques
        int derivedOffset = idx - (dims + 1);
        if (derivedOffset == 1 || mode.contains("capital") || mode.contains("k(x)")) {
            double cap = c.getResourceCapital() != null ? c.getResourceCapital() : 0.0;
            double norm = Math.clamp(cap / 500000.0, 0.0, 1.0);
            return Color.hsb((1.0 - norm) * 240.0, 0.85, 0.90);
        } else if (derivedOffset == 2 || mode.contains("énergétique") || mode.contains("energy") || mode.contains("energie") || mode.contains("energético") || mode.contains("e(x)")) {
            double energy = c.getEnergyFire() != null ? c.getEnergyFire() : 0.0;
            double norm = Math.clamp(energy / 1000000.0, 0.0, 1.0);
            return Color.hsb(30.0 + norm * 30.0, 0.90, 0.95);
        } else if (derivedOffset == 3 || mode.contains("alimentaires") || mode.contains("food") || mode.contains("nahrung") || mode.contains("alimentos") || mode.contains("f(x)")) {
            double food = c.getFoodResource() != null ? c.getFoodResource() : 0.0;
            double norm = Math.clamp(food / 100000.0, 0.0, 1.0);
            return Color.hsb(120.0, 0.70 + norm * 0.30, 0.60 + norm * 0.35);
        } else if (derivedOffset == 4 || mode.contains("informationnel") || mode.contains("info") || mode.contains("i(x)")) {
            double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
            double norm = Math.clamp(tech / 10.0, 0.0, 1.0);
            return Color.hsb(270.0 + norm * 60.0, 0.85, 0.90);
        } else if (derivedOffset == 5 || mode.contains("empreinte") || mode.contains("footprint") || mode.contains("fußabdruck") || mode.contains("huella") || mode.contains("malthus")) {
            double cap = computeCellCarryingCapacity(c);
            long pop = c.getPopulation() != null ? c.getPopulation() : 0;
            double ratio = Math.clamp(pop / Math.max(1.0, cap), 0.0, 1.5);
            return Color.hsb((1.0 - Math.min(1.0, ratio)) * 120.0, 0.85, 0.90);
        } else if (derivedOffset == 6 || mode.contains("friction") || mode.contains("reibung") || mode.contains("fricción")) {
            double fric = c.getMovementFriction() != null ? c.getMovementFriction() : 1.0;
            double norm = Math.clamp((fric - 1.0) / 4.0, 0.0, 1.0);
            return Color.rgb((int)(norm * 255), (int)((1.0 - norm) * 200), 50);
        }

        return getReliefAndDensityColor(c);
    }

    private Color sampleImageColorAtLatLon(Image img, double lat, double lon) {
        if (img == null || img.getWidth() <= 1 || img.getHeight() <= 1) return null;
        PixelReader reader = img.getPixelReader();
        if (reader == null) return null;

        int px = (int) Math.clamp(((lon + 180.0) / 360.0) * img.getWidth(), 0, img.getWidth() - 1);
        int py = (int) Math.clamp(((90.0 - lat) / 180.0) * img.getHeight(), 0, img.getHeight() - 1);
        return reader.getColor(px, py);
    }

    private void drawPreview() {
        if (isUpdatingFromPreset || previewCanvas == null) return;
        double w = previewCanvas.getWidth();
        double h = previewCanvas.getHeight();
        if (w < 1.0 || h < 1.0) return;
        GraphicsContext gc = previewCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        double minLat = -90, maxLat = 90;
        double minLng = -180, maxLng = 180;

        double scaleX = w / (maxLng - minLng);
        double scaleY = h / (maxLat - minLat);
        double baseScale = Math.min(scaleX, scaleY);
        double scale = baseScale * zoomFactor;

        double offX = (w / 2.0) - (0.0 - minLng) * scale + panX;
        double offY = (h / 2.0) - (maxLat - 0.0) * scale + panY;

        if (currentPreviewCells != null && !currentPreviewCells.isEmpty()) {
            double cellCount = currentPreviewCells.size();
            double areaSqDeg = (maxLat - minLat) * (maxLng - minLng);
            double avgAreaPerCell = areaSqDeg / Math.max(1.0, cellCount);
            double dynamicCellDeg = Math.sqrt(avgAreaPerCell) * 1.08;
            double cellSize = Math.max(1.5, dynamicCellDeg * scale);

            PlanetPreset activePlanet = activePlanetPreset != null ? activePlanetPreset : PlanetPreset.EARTH_LIKE;
            double wLvl = activePlanet.waterLevel();
            boolean hasOcean = wLvl > -0.4;
            boolean isReliefActive = (btnReliefOverlay != null && btnReliefOverlay.isSelected());

            // Iterate over spatial cell domains and apply localized cellular state transformations
            for (H3Cell c : currentPreviewCells) {
                double x = (c.getLongitude() - minLng) * scale + offX;
                double y = (maxLat - c.getLatitude()) * scale + offY;

                if (x < -cellSize || x > w + cellSize || y < -cellSize || y > h + cellSize) continue;

                Color col = getPreviewColorForCell(c);
                boolean isCoastalCell = Boolean.TRUE.equals(c.getIsCoastal());
                double elev = c.getElevation() != null ? c.getElevation() : 0.0;
                boolean isDatumCell = !hasOcean && Math.abs(elev) < 350.0;

                if (isReliefActive) {
                    if (hasOcean && elev <= 0) {
                        double depthRatio = Math.clamp(Math.abs(elev) / 4000.0, 0.0, 1.0);
                        double mult = 0.85 + 0.15 * (1.0 - depthRatio);
                        col = Color.color(
                            Math.clamp(col.getRed() * mult, 0.0, 1.0),
                            Math.clamp(col.getGreen() * mult, 0.0, 1.0),
                            Math.clamp(col.getBlue() * mult, 0.0, 1.0)
                        );
                    } else {
                        double decl = c.getMovementFriction() != null ? Math.max(0.0, c.getMovementFriction() - 1.0) : 0.0;
                        double normElev = Math.clamp(elev / 4000.0, 0.0, 1.0);
                        double mult = 0.75 + 0.30 * normElev + 0.15 * Math.min(1.0, decl);
                        col = Color.color(
                            Math.clamp(col.getRed() * mult, 0.0, 1.0),
                            Math.clamp(col.getGreen() * mult, 0.0, 1.0),
                            Math.clamp(col.getBlue() * mult, 0.0, 1.0)
                        );
                    }
                }
                gc.setFill(col);
                double absLat = Math.abs(c.getLatitude());
                double cosLat = Math.cos(Math.toRadians(Math.min(88.0, absLat)));
                double cellW = Math.max(cellSize, cellSize / Math.max(0.12, cosLat));
                double cellH = cellSize;

                if (cellW >= 7.0 && cellH >= 7.0) {
                    gc.fillRoundRect(x - cellW / 2.0, y - cellH / 2.0, cellW, cellH, 3.0, 3.0);
                    if (isReliefActive && hasOcean && isCoastalCell) {
                        gc.setStroke(Color.rgb(240, 249, 255)); // Crisp white-cyan coastline outline
                        gc.setLineWidth(1.5);
                        gc.strokeRoundRect(x - cellW / 2.0, y - cellH / 2.0, cellW, cellH, 3.0, 3.0);
                    } else if (isReliefActive && isDatumCell) {
                        gc.setStroke(Color.rgb(251, 191, 36)); // Crisp amber-gold Datum Z=0 line
                        gc.setLineWidth(1.5);
                        gc.strokeRoundRect(x - cellW / 2.0, y - cellH / 2.0, cellW, cellH, 3.0, 3.0);
                    } else if (cellW >= 12.0) {
                        gc.setStroke(Color.rgb(0, 0, 0, 0.25));
                        gc.setLineWidth(0.75);
                        gc.strokeRoundRect(x - cellW / 2.0, y - cellH / 2.0, cellW, cellH, 3.0, 3.0);
                    }
                } else {
                    gc.fillRect(x - cellW / 2.0, y - cellH / 2.0, cellW, cellH);
                    if (isReliefActive && hasOcean && isCoastalCell) {
                        gc.setStroke(Color.rgb(240, 249, 255));
                        gc.setLineWidth(1.0);
                        gc.strokeRect(x - cellW / 2.0, y - cellH / 2.0, cellW, cellH);
                    } else if (isReliefActive && isDatumCell) {
                        gc.setStroke(Color.rgb(251, 191, 36));
                        gc.setLineWidth(1.0);
                        gc.strokeRect(x - cellW / 2.0, y - cellH / 2.0, cellW, cellH);
                    }
                }
            }
        } else {
            drawInstant2DDensityPreview(gc, w, h, scale, offX, offY);
        }

        // Draw Clipping bounding box & dimmed overlay
        boolean isClippingActive = (clippingCheckBox != null && clippingCheckBox.isSelected()) || isSelectionDrag;
        if (isClippingActive) {
            double cMinLat = minLatSpinner != null ? minLatSpinner.getValue() : -90.0;
            double cMaxLat = maxLatSpinner != null ? maxLatSpinner.getValue() : 90.0;
            double cMinLng = minLngSpinner != null ? minLngSpinner.getValue() : -180.0;
            double cMaxLng = maxLngSpinner != null ? maxLngSpinner.getValue() : 180.0;

            if (isSelectionDrag) {
                double x1 = Math.min(selectionStartX, selectionCurrentX);
                double x2 = Math.max(selectionStartX, selectionCurrentX);
                double y1 = Math.min(selectionStartY, selectionCurrentY);
                double y2 = Math.max(selectionStartY, selectionCurrentY);

                cMinLng = Math.max(-180.0, Math.min(180.0, ((x1 - offX) / scale) + minLng));
                cMaxLng = Math.max(-180.0, Math.min(180.0, ((x2 - offX) / scale) + minLng));
                cMaxLat = Math.max(-90.0, Math.min(90.0, maxLat - ((y1 - offY) / scale)));
                cMinLat = Math.max(-90.0, Math.min(90.0, maxLat - ((y2 - offY) / scale)));
            }

            double x1 = (cMinLng - minLng) * scale + offX;
            double x2 = (cMaxLng - minLng) * scale + offX;
            double y1 = (maxLat - cMaxLat) * scale + offY;
            double y2 = (maxLat - cMinLat) * scale + offY;

            double rx = Math.min(x1, x2);
            double ry = Math.min(y1, y2);
            double rw = Math.abs(x2 - x1);
            double rh = Math.abs(y2 - y1);

            // Dim exterior
            gc.setFill(Color.rgb(0, 0, 0, 0.45));
            gc.fillRect(0, 0, w, Math.max(0, ry));
            gc.fillRect(0, ry + rh, w, Math.max(0, h - (ry + rh)));
            gc.fillRect(0, Math.max(0, ry), Math.max(0, rx), rh);
            gc.fillRect(rx + rw, Math.max(0, ry), Math.max(0, w - (rx + rw)), rh);

            // Bright Cyan dashed rectangle
            gc.setStroke(Color.rgb(56, 189, 248, 0.95));
            gc.setLineWidth(2.0);
            gc.setLineDashes(6.0, 4.0);
            gc.strokeRect(rx, ry, rw, rh);
            gc.setLineDashes((double[]) null);
            // Text tag
            gc.setFill(Color.rgb(56, 189, 248));
            gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 11));
            gc.fillText(String.format("✂️ï¸ Zone: Lat[%.1f°, %.1f°] Lng[%.1f°, %.1f°]", cMinLat, cMaxLat, cMinLng, cMaxLng), rx + 4, ry - 6);
        }
        updateMapInfoSummary();
    }

    private double sampleCulturalTensorValue(int tIdx, H3Cell cell) {
        if (cell == null) return 0.5;
        Image img = customTensorImages.get(tIdx);
        if (img != null && img.getPixelReader() != null) {
            double normLat = (cell.getLatitude() + 90.0) / 180.0;
            double normLon = (cell.getLongitude() + 180.0) / 360.0;
            int px = (int) Math.min(img.getWidth() - 1, Math.max(0, normLon * img.getWidth()));
            int py = (int) Math.min(img.getHeight() - 1, Math.max(0, (1.0 - normLat) * img.getHeight()));
            return img.getPixelReader().getColor(px, py).getBrightness();
        }
        double p1 = tensorParam1Sliders.containsKey(tIdx) ? tensorParam1Sliders.get(tIdx).getValue() : 1.0;
        double p2 = tensorParam2Sliders.containsKey(tIdx) ? tensorParam2Sliders.get(tIdx).getValue() : 1.0;
        double p3 = tensorParam3Sliders.containsKey(tIdx) ? tensorParam3Sliders.get(tIdx).getValue() : 1.0;
        long seed = 11235L + tIdx * 11111L;
        if (tensorSeedFields.containsKey(tIdx)) {
            try { seed = Long.parseLong(tensorSeedFields.get(tIdx).getText().trim()); } catch (Exception ignored) {}
        }
        double elev = cell.getElevation() != null ? cell.getElevation() : 500.0;
        Color col = evaluateProceduralTensorColor(tIdx, cell.getLatitude(), cell.getLongitude(), elev, p1, p2, p3, seed);
        return col.getBrightness();
    }

    private void updateMapInfoSummary() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (previewStatusLabel == null) return;
        if (currentPreviewCells == null || currentPreviewCells.isEmpty()) {
            previewStatusLabel.setText(I18n.getOrDefault("scenario.label.preview_precalc", "Aperçu de la distribution démographique pré-calculée"));
            return;
        }

        int curSel = previewModeCombo != null ? previewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;

        if (curSel <= 0) {
            // Mode 0: Demography & Density
            long totalPop = initialHumanCountSpinner != null && initialHumanCountSpinner.getValue() != null ? initialHumanCountSpinner.getValue() : 1_000_000L;
            int activeCells = 0;
            double maxDensity = 0.0;
            double sumDensity = 0.0;
            // Iterate over spatial cell domains and apply localized cellular state transformations
            for (H3Cell c : currentPreviewCells) {
                if (c == null) continue;
                boolean isLand = (c.getElevation() != null && c.getElevation() > 0.0) || (c.getBiome() != null && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN);
                if (isLand) {
                    activeCells++;
                    double dens = c.getPopulation() != null ? c.getPopulation().doubleValue() : 0.0;
                    if (dens > maxDensity) maxDensity = dens;
                    sumDensity += dens;
                }
            }
            double avgDensity = activeCells > 0 ? (sumDensity / activeCells) : 0.0;
            int res = h3ResolutionCombo != null && h3ResolutionCombo.getValue() != null ? h3ResolutionCombo.getValue() : 3;
            previewStatusLabel.setText(String.format(
                I18n.getOrDefault("scenario.map_info.density", "👥 Démographie T₀ : %s hab.  |  ðŸ“ %s cellules H3 actives (Res %d)  |  📈 Densité max : %.1f hab/km² (Moy : %.1f)"),
                String.format(java.util.Locale.FRANCE, "%,d", totalPop),
                String.format(java.util.Locale.FRANCE, "%,d", activeCells),
                res,
                maxDensity,
                avgDensity
            ));
        } else if (curSel >= 1 && curSel <= dims) {
            // Mode 1..dims: Cultural Tensor (curSel - 1)
            int tIdx = curSel - 1;
            String title = getCulturalTensorTitle(tIdx);
            boolean isProc = tensorProcRadios.containsKey(tIdx) && tensorProcRadios.get(tIdx).isSelected();
            String modeStr = isProc ? (I18n.getOrDefault("scenario.mode.procedural", "Procédural") + " (" + I18n.getOrDefault("scenario.label.seed", "Graine") + ": " + (tensorSeedFields.containsKey(tIdx) ? tensorSeedFields.get(tIdx).getText() : getDefaultTensorSeed(tIdx)) + ")")
                                    : (I18n.getOrDefault("scenario.mode.imported", "Import SIG / Empirique"));
            double diff = culturalDiffusionRateSpinner != null && culturalDiffusionRateSpinner.getValue() != null ? culturalDiffusionRateSpinner.getValue() : 0.05;
            double mut = culturalMutationRateSpinner != null && culturalMutationRateSpinner.getValue() != null ? culturalMutationRateSpinner.getValue() : 0.01;

            double minVal = 1.0, maxVal = 0.0, sumVal = 0.0, sumSq = 0.0;
            int count = 0;
            // Iterate over spatial cell domains and apply localized cellular state transformations
            for (H3Cell c : currentPreviewCells) {
                if (c == null) continue;
                boolean isLand = (c.getElevation() != null && c.getElevation() > 0.0) || (c.getBiome() != null && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN);
                if (isLand) {
                    double val = sampleCulturalTensorValue(tIdx, c);
                    if (val < minVal) minVal = val;
                    if (val > maxVal) maxVal = val;
                    sumVal += val;
                    sumSq += val * val;
                    count++;
                }
            }
            double mean = count > 0 ? (sumVal / count) : 0.5;
            double variance = count > 1 ? Math.max(0.0, (sumSq - (sumVal * sumVal) / count) / (count - 1)) : 0.0;
            double stdDev = Math.sqrt(variance);
            if (count == 0) { minVal = 0.0; maxVal = 1.0; }

            previewStatusLabel.setText(String.format(
                I18n.getOrDefault("scenario.map_info.tensor", "⚙️ï¸ Mode : %s  |  📊 Dispersion : μ=%.2f ± %.2f [min=%.2f, max=%.2f]  |  🌀 Diffusion D=%.3f  |  🎲 Bruit=%.3f"),
                modeStr, mean, stdDev, minVal, maxVal, diff, mut
            ));
        } else {
            // Derived layers (Capital, Energy, Food, Info, Footprint, Friction)
            int derivedOffset = curSel - (dims + 1);
            String layerName = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue() : "";
            double sum = 0.0;
            int count = 0;
            // Iterate over spatial cell domains and apply localized cellular state transformations
            for (H3Cell c : currentPreviewCells) {
                if (c == null) continue;
                boolean isLand = (c.getElevation() != null && c.getElevation() > 0.0) || (c.getBiome() != null && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN);
                if (isLand) {
                    count++;
                    if (derivedOffset == 1 || layerName.contains("Capital") || layerName.contains("K(x)")) sum += (c.getResourceCapital() != null ? c.getResourceCapital() : 0.0);
                    else if (derivedOffset == 2 || layerName.contains("Energy") || layerName.contains("E(x)") || layerName.contains("énergétique") || layerName.contains("Energie") || layerName.contains("Energético")) sum += (c.getEnergyFire() != null ? c.getEnergyFire() : 0.0);
                    else if (derivedOffset == 3 || layerName.contains("Food") || layerName.contains("F(x)") || layerName.contains("alimentaires") || layerName.contains("Nahrung") || layerName.contains("Alimentos")) sum += (c.getFoodResource() != null ? c.getFoodResource() : 0.0);
                    else if (derivedOffset == 4 || layerName.contains("Info") || layerName.contains("I(x)") || layerName.contains("informationnel") || layerName.contains("Information")) sum += (c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 0.0);
                    else if (derivedOffset == 6 || layerName.contains("Friction") || layerName.contains("friction") || layerName.contains("Reibung") || layerName.contains("Fricción")) sum += (c.getMovementFriction() != null ? c.getMovementFriction() : 1.0);
                    else sum += computeCellCarryingCapacity(c);
                }
            }
            double mean = count > 0 ? (sum / count) : 0.0;
            previewStatusLabel.setText(String.format(
                I18n.getOrDefault("scenario.map_info.derived", "📊 Moyenne : %.2f  |  ⚡ Stock global : %s  |  ðŸ“ Cellules actives : %s"),
                mean,
                String.format(java.util.Locale.FRANCE, "%,.0f", sum),
                String.format(java.util.Locale.FRANCE, "%,d", count)
            ));
        }
    }

    private Color blendColors(Color base, Color overlay, double opacity) {
        if (base == null) return overlay;
        if (overlay == null) return base;
        double r = base.getRed() * (1.0 - opacity) + overlay.getRed() * opacity;
        double g = base.getGreen() * (1.0 - opacity) + overlay.getGreen() * opacity;
        double b = base.getBlue() * (1.0 - opacity) + overlay.getBlue() * opacity;
        return Color.color(Math.clamp(r, 0.0, 1.0), Math.clamp(g, 0.0, 1.0), Math.clamp(b, 0.0, 1.0));
    }

    private Color getReliefShadeColor(double elevation, double waterLevel, double declivity) {
        if (elevation < waterLevel) {
            double depthNorm = Math.clamp((waterLevel - elevation) / 2000.0, 0.0, 1.0);
            double val = 0.05 + 0.15 * (1.0 - depthNorm);
            return Color.color(val * 0.5, val * 0.7, val);
        } else {
            double hNorm = Math.clamp((elevation - waterLevel) / 3000.0, 0.0, 1.0);
            double shade = 0.2 + 0.6 * hNorm + 0.2 * Math.min(1.0, declivity * 2.0);
            return Color.color(shade, shade, shade);
        }
    }

    private void drawLegendDot(GraphicsContext gc, double x, double y, Color c, String label) {
        gc.setFill(c);
        gc.fillOval(x, y - 7, 8, 8);
        gc.setFill(Color.rgb(226, 232, 240));
        gc.fillText(label, x + 12, y);
    }

    private void drawInstant2DDensityPreview(GraphicsContext gc, double w, double h, double scale, double offX, double offY) {
        int pwWidth = 320;
        int pwHeight = 160;

        PlanetPreset planet = activePlanetPreset != null ? activePlanetPreset : PlanetPreset.EARTH_LIKE;
        Image bgImage = null;
        if (planet.customElevBase64() != null && !planet.customElevBase64().isBlank()) {
            bgImage = org.ether.society.data.ImageMapLoader.base64PngToImage(planet.customElevBase64());
        } else if (planet.elevationUseImport()) {
            bgImage = org.ether.society.data.ImageMapLoader.loadMapImage(planet.elevationMapSource(), planet.getAssociatedEpochYear(), "elevation.png");
        }
        if (bgImage == null && "earth".equalsIgnoreCase(planet.elevationMapSource())) {
            bgImage = getCachedEarthElevationImage();
        }
        PixelReader bgReader = bgImage != null ? bgImage.getPixelReader() : null;

        int idx = previewModeCombo != null ? previewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        String mode = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue().toLowerCase() : "";
        int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
        Image activeCustomImage = null;
        boolean isImportActive = false;
        if (idx >= 1 && idx <= dims) {
            int tIndex = idx - 1;
            isImportActive = tensorImportRadios.containsKey(tIndex) && tensorImportRadios.get(tIndex).isSelected();
            activeCustomImage = customTensorImages.get(tIndex);
        } else if (idx == 0) {
            isImportActive = radioImportDemo != null && radioImportDemo.isSelected();
            activeCustomImage = customDensityImage;
        }

        PixelReader customReader = activeCustomImage != null ? activeCustomImage.getPixelReader() : null;
        boolean isFootprintMode = idx == 6 || mode.contains("empreinte");
        boolean isReliefOverlay = btnReliefOverlay != null && btnReliefOverlay.isSelected();

        String cacheKey = "sc_mode=" + idx + "_w=" + pwWidth + "_h=" + pwHeight + "_relief=" + isReliefOverlay
                + "_import=" + isImportActive
                + "_pName=" + (planet != null ? planet.name() : "")
                + "_startYear=" + (startYearSpinner != null ? startYearSpinner.getValue() : -8000)
                + "_pop=" + (initialHumanCountSpinner != null ? initialHumanCountSpinner.getValue() : 1000000)
                + "_pattern=" + (densityPatternCombo != null ? densityPatternCombo.getValue() : "")
                + "_demoSeed=" + (demoSeedField != null ? demoSeedField.getText() : "")
                + "_cultSeed=" + (cultSeedField != null ? cultSeedField.getText() : "")
                + "_hasCustomDensity=" + (customDensityImage != null)
                + "_tensorCount=" + customTensorImages.size();

        WritableImage img = proceduralImageCache.get(cacheKey);
        if (img == null) {
            img = new WritableImage(pwWidth, pwHeight);
            PixelWriter writer = img.getPixelWriter();
            ProceduralGenerator generator = new ProceduralGenerator();

            double minAlt = planet != null ? planet.minAltitudeMeters() : -11000.0;
            double maxAlt = planet != null ? planet.maxAltitudeMeters() : 8848.0;
            double wLevel = planet != null ? planet.waterLevel() : 0.478;
            boolean hasOcean = wLevel > -0.4;

            double cutThreshold;
            if (hasOcean) {
                cutThreshold = Math.clamp(wLevel, 0.01, 0.99);
            } else {
                double altRange = Math.max(100.0, maxAlt - minAlt);
                cutThreshold = Math.clamp((-minAlt) / altRange, 0.05, 0.95);
            }

            for (int py = 0; py < pwHeight; py++) {
                for (int px = 0; px < pwWidth; px++) {
                    double lat = 90.0 - (py / (double) pwHeight) * 180.0;
                    double lon = -180.0 + (px / (double) pwWidth) * 360.0;

                    // 1) Accurate elevation, water level and coastlines / Datum Z = 0
                    double elevVal;
                    boolean isLand;
                    boolean isCoast;
                    double hillshade;

                    if (bgReader != null && bgImage != null) {
                        double bgW = bgImage.getWidth();
                        double bgH = bgImage.getHeight();
                        int bx = (int) Math.clamp(((px / (double) pwWidth) * bgW), 0, bgW - 1);
                        int by = (int) Math.clamp(((py / (double) pwHeight) * bgH), 0, bgH - 1);
                        elevVal = bgReader.getColor(bx, by).getRed();
                        boolean isAboveCut = elevVal >= cutThreshold;
                        isLand = hasOcean ? isAboveCut : true;

                        int bxE = (bx + 1) % (int) bgW;
                        int bxW = (bx - 1 + (int) bgW) % (int) bgW;
                        int byN = Math.max(0, by - 1);
                        int byS = Math.min((int) bgH - 1, by + 1);

                        double eE = bgReader.getColor(bxE, by).getRed();
                        double eW = bgReader.getColor(bxW, by).getRed();
                        double eN = bgReader.getColor(bx, byN).getRed();
                        double eS = bgReader.getColor(bx, byS).getRed();

                        isCoast = (isAboveCut != (eE >= cutThreshold)) || (isAboveCut != (eW >= cutThreshold))
                                || (isAboveCut != (eN >= cutThreshold)) || (isAboveCut != (eS >= cutThreshold));

                        double dLng = (eE - eW) * 16.0;
                        double dLat = (eN - eS) * 16.0;
                        hillshade = (0.5 * dLng + 0.5 * dLat + 0.707) / Math.sqrt(dLng * dLng + dLat * dLat + 1.0);
                    } else {
                        var pt = generator.getPlanetPoint(lat, lon, planet);
                        elevVal = pt.elevation();

                        double dDeg = 0.5;
                        var ptE = generator.getPlanetPoint(lat, lon + dDeg, planet);
                        var ptW = generator.getPlanetPoint(lat, lon - dDeg, planet);
                        var ptN = generator.getPlanetPoint(lat + dDeg, lon, planet);
                        var ptS = generator.getPlanetPoint(lat - dDeg, lon, planet);

                        if (hasOcean) {
                            isLand = elevVal >= wLevel;
                            isCoast = (isLand != (ptE.elevation() >= wLevel))
                                    || (isLand != (ptW.elevation() >= wLevel))
                                    || (isLand != (ptN.elevation() >= wLevel))
                                    || (isLand != (ptS.elevation() >= wLevel));
                        } else {
                            isLand = true;
                            double datumCut = 0.0;
                            boolean isAboveDatum = elevVal >= datumCut;
                            isCoast = (isAboveDatum != (ptE.elevation() >= datumCut))
                                    || (isAboveDatum != (ptW.elevation() >= datumCut))
                                    || (isAboveDatum != (ptN.elevation() >= datumCut))
                                    || (isAboveDatum != (ptS.elevation() >= datumCut));
                        }

                        double dLng = ((ptE.elevation() - ptW.elevation()) / 1200.0);
                        double dLat = ((ptN.elevation() - ptS.elevation()) / 1200.0);
                        hillshade = (0.5 * dLng + 0.5 * dLat + 0.707) / Math.sqrt(dLng * dLng + dLat * dLat + 1.0);
                    }

                    Color baseReliefColor;
                    if (!isLand) {
                        baseReliefColor = Color.rgb(15, 23, 42); // Sea / Deep ocean navy
                    } else {
                        double landNorm = bgReader != null
                                ? Math.clamp((elevVal - cutThreshold) / Math.max(0.01, 1.0 - cutThreshold), 0.0, 1.0)
                                : Math.clamp((elevVal - wLevel) / Math.max(1000.0, planet.maxAltitudeMeters() - wLevel), 0.0, 1.0);
                        if (landNorm < 0.20) {
                            double t = landNorm / 0.20;
                            baseReliefColor = Color.rgb((int)(34 + 60 * t), (int)(139 + 30 * t), (int)(34 + 20 * t));
                        } else if (landNorm < 0.40) {
                            double t = (landNorm - 0.20) / 0.20;
                            baseReliefColor = Color.rgb((int)(94 + 86 * t), (int)(169 + 16 * t), (int)(54 + 21 * t));
                        } else if (landNorm < 0.65) {
                            double t = (landNorm - 0.40) / 0.25;
                            baseReliefColor = Color.rgb((int)(180 - 10 * t), (int)(185 - 65 * t), (int)(75 - 15 * t));
                        } else if (landNorm < 0.85) {
                            double t = (landNorm - 0.65) / 0.20;
                            baseReliefColor = Color.rgb((int)(170 - 20 * t), (int)(120 + 40 * t), (int)(60 + 80 * t));
                        } else {
                            double t = (landNorm - 0.85) / 0.15;
                            baseReliefColor = Color.rgb((int)(150 + 95 * t), (int)(160 + 90 * t), (int)(140 + 115 * t));
                        }
                    }

                    // 2) Main layer color
                    Color pxColor;
                    if (idx >= 1 && idx <= dims) {
                        int tIndex = idx - 1;
                        if (isImportActive) {
                            if (customReader != null && activeCustomImage.getWidth() > 0 && activeCustomImage.getHeight() > 0) {
                                int imgX = (int) Math.clamp(((px / (double) pwWidth) * activeCustomImage.getWidth()), 0, activeCustomImage.getWidth() - 1);
                                int imgY = (int) Math.clamp(((py / (double) pwHeight) * activeCustomImage.getHeight()), 0, activeCustomImage.getHeight() - 1);
                                pxColor = customReader.getColor(imgX, imgY);
                            } else {
                                pxColor = Color.BLACK;
                            }
                        } else {
                            double p1 = tensorParam1Sliders.containsKey(tIndex) ? tensorParam1Sliders.get(tIndex).getValue() : 1.0;
                            double p2 = tensorParam2Sliders.containsKey(tIndex) ? tensorParam2Sliders.get(tIndex).getValue() : 1.0;
                            double p3 = tensorParam3Sliders.containsKey(tIndex) ? tensorParam3Sliders.get(tIndex).getValue() : 1.0;
                            long seed = 11235L + tIndex * 11111L;
                            if (tensorSeedFields.containsKey(tIndex)) {
                                try { seed = Long.parseLong(tensorSeedFields.get(tIndex).getText().trim()); } catch (Exception ignored) {}
                            }
                            pxColor = evaluateProceduralTensorColor(tIndex, lat, lon, elevVal, p1, p2, p3, seed);
                        }
                    } else if (isImportActive) {
                        if (customReader != null && activeCustomImage.getWidth() > 0 && activeCustomImage.getHeight() > 0) {
                            int imgX = (int) Math.clamp(((px / (double) pwWidth) * activeCustomImage.getWidth()), 0, activeCustomImage.getWidth() - 1);
                            int imgY = (int) Math.clamp(((py / (double) pwHeight) * activeCustomImage.getHeight()), 0, activeCustomImage.getHeight() - 1);
                            pxColor = customReader.getColor(imgX, imgY);
                            if (!isLand) {
                                pxColor = baseReliefColor;
                            }
                        } else {
                            pxColor = Color.BLACK;
                        }
                    } else if (!isLand) {
                        pxColor = baseReliefColor; // Ocean Navy
                    } else {
                        // Density mode default preview on land: blend population heat on top of natural hypsometric base
                        long startYear = startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000;
                        boolean isAmericas = lon < -25.0;
                        boolean isSahul = (lat < 10.0 && lon > 95.0) || (lat < -10.0 && lon > 110.0);

                        boolean isHumanSettled = true;
                        if (startYear <= -50000 && (isAmericas || isSahul)) {
                            isHumanSettled = false;
                        } else if (startYear <= -25000 && isAmericas) {
                            isHumanSettled = false;
                        }

                        if (!isHumanSettled) {
                            pxColor = blendColors(baseReliefColor, Color.rgb(30, 95, 165), 0.35); // Inhabité
                        } else {
                            boolean isEastAfrica = (lat >= -15 && lat <= 15) && (lon >= 25 && lon <= 45);
                            boolean isFertileCrescent = (lat >= 20 && lat <= 38) && (lon >= 25 && lon <= 90);
                            boolean isChinaIndus = (lat >= 10 && lat <= 42) && (lon >= 65 && lon <= 125);

                            if (isEastAfrica || isFertileCrescent || isChinaIndus) {
                                Color heatCol = isFootprintMode ? Color.rgb(239, 68, 68) : Color.rgb(249, 115, 22);
                                pxColor = blendColors(baseReliefColor, heatCol, 0.70);
                            } else {
                                pxColor = blendColors(baseReliefColor, Color.rgb(16, 185, 129), 0.50);
                            }
                        }
                    }

                    // 3) Relief overlay and coastline / Datum outlines
                    if (isReliefOverlay && pxColor != Color.BLACK) {
                        if (isCoast) {
                            pxColor = hasOcean ? Color.rgb(240, 249, 255) : Color.rgb(251, 191, 36); // Crisp coastline outline at z = Z_sea or Datum Z = 0
                        } else {
                            double mult = 0.68 + 0.45 * hillshade;
                            pxColor = Color.color(
                                Math.clamp(pxColor.getRed() * mult, 0.0, 1.0),
                                Math.clamp(pxColor.getGreen() * mult, 0.0, 1.0),
                                Math.clamp(pxColor.getBlue() * mult, 0.0, 1.0)
                            );
                        }
                    }

                    writer.setColor(px, py, pxColor != null ? pxColor : Color.BLACK);
                }
            }
            proceduralImageCache.put(cacheKey, img);
        }

        double drawW = 360.0 * scale;
        double drawH = 180.0 * scale;
        gc.drawImage(img, offX, offY, drawW, drawH);

        gc.setFill(Color.rgb(56, 189, 248, 0.95));
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
        gc.fillText("⚡ Aperçu 2D dynamique instantané (" + (mode.isEmpty() ? "Relief/Densité" : mode.toUpperCase()) + ")...", 20, h - 15);
    }

    /**
     * Is dirty.
     *
     * @return the resulting computation or state reference
     */
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
        int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
        s.setCultureVectorDimensions(dims);
        s.setCustomTensorNames(new java.util.HashMap<>(customTensorNames));
        if (culturalDiffusionRateSpinner != null) {
            s.setCulturalDiffusionRate(culturalDiffusionRateSpinner.getValue());
        }
        if (culturalMutationRateSpinner != null) {
            s.setCulturalMutationRate(culturalMutationRateSpinner.getValue());
        }
        java.util.Map<Integer, Long> tSeeds = new java.util.HashMap<>();
        java.util.Map<Integer, java.util.Map<String, Double>> tProcParams = new java.util.HashMap<>();

        for (int i = 0; i < dims; i++) {
            if (customTensorImages.get(i) != null) {
                s.setCustomTensorMapBase64(i, org.ether.society.data.ImageMapLoader.imageToBase64Png(customTensorImages.get(i)));
            }
            boolean isProc = tensorProcRadios.containsKey(i) && tensorProcRadios.get(i).isSelected();
            while (s.getTensorProceduralModes().size() <= i) {
                s.getTensorProceduralModes().add(true);
            }
            s.getTensorProceduralModes().set(i, isProc);

            // Tensor Seeds
            if (tensorSeedFields.containsKey(i) && tensorSeedFields.get(i) != null) {
                try {
                    tSeeds.put(i, Long.parseLong(tensorSeedFields.get(i).getText().trim()));
                } catch (NumberFormatException ignored) {
                    try {
                        tSeeds.put(i, Long.parseLong(getDefaultTensorSeed(i)));
                    } catch (Exception e) {
                        tSeeds.put(i, 12345L);
                    }
                }
            } else {
                try {
                    tSeeds.put(i, Long.parseLong(getDefaultTensorSeed(i)));
                } catch (Exception e) {
                    tSeeds.put(i, 12345L);
                }
            }

            // Tensor Procedural Parameters
            java.util.Map<String, Double> pMap = new java.util.HashMap<>();
            TensorParamDescriptor d1 = getTensorParam1Descriptor(i);
            TensorParamDescriptor d2 = getTensorParam2Descriptor(i);
            TensorParamDescriptor d3 = getTensorParam3Descriptor(i);
            if (tensorParam1Sliders.containsKey(i) && tensorParam1Sliders.get(i) != null) {
                pMap.put(d1.labelKey, tensorParam1Sliders.get(i).getValue());
            }
            if (tensorParam2Sliders.containsKey(i) && tensorParam2Sliders.get(i) != null) {
                pMap.put(d2.labelKey, tensorParam2Sliders.get(i).getValue());
            }
            if (tensorParam3Sliders.containsKey(i) && tensorParam3Sliders.get(i) != null) {
                pMap.put(d3.labelKey, tensorParam3Sliders.get(i).getValue());
            }
            tProcParams.put(i, pMap);
        }
        s.setTensorSeeds(tSeeds);
        s.setTensorProceduralParameters(tProcParams);

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
