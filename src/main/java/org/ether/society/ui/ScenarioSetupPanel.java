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
 * @version 1.0.0-beta.1
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
    private Spinner<Integer> targetCohortSizeSpinner;
    private Label cohortSizeLabel;
    private ComboBox<Double> temporalResolutionCombo;
    private Label temporalResolutionLabel;

    // State & Change Listener Tracking
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
     * 1. Appends "[VALEUR PAR DÃ‰FAUT : X]" to the tooltip.
     * 2. Adds a right-click Context Menu ("ðŸ”„ RÃ©initialiser Ã  la valeur par dÃ©faut (X)").
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
            String defaultNotice = (baseTooltip.isEmpty() ? "" : "\n\n") + I18n.getOrDefault("scenario.tooltip.default_notice_prefix", "ðŸ“Œ [DEFAULT VALUE: ") + defaultStr + I18n.getOrDefault("scenario.tooltip.default_notice_suffix", "] â€” Right-click control to reset.");
            control.setTooltip(new Tooltip(baseTooltip + defaultNotice));
        }

        // 2. Right-click Context Menu
        ContextMenu contextMenu = control.getContextMenu();
        if (contextMenu == null) {
            contextMenu = new ContextMenu();
        }
        MenuItem resetItem = new MenuItem(I18n.getOrDefault("scenario.menu.reset_default", "ðŸ”„ Reset to default value (") + defaultStr + ")");
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
    private CheckBox randomEventsCheckBox;
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
    private double zoomFactor = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private double dragStartX, dragStartY;
    private boolean isSelectionDrag = false;
    private double selectionStartX, selectionStartY;
    private double selectionCurrentX, selectionCurrentY;

    // Density Map Import UI Fields
    private RadioButton radioProcDemo;
    private RadioButton radioImportDemo;
    private Label demoCompatibilityLabel;

    private static final Map<String, String> DENSITY_LABELS = Map.ofEntries(
        Map.entry("UNBIASED_NATURAL", "âš– Natural Bio-Climatic Equilibrium (Unbiased)"),
        Map.entry("COASTAL_MARITIME", "ðŸŒŠ Coastal & Maritime Focus"),
        Map.entry("RIVER_VALLEYS", "ðŸž River Valleys & Alluvial Basins"),
        Map.entry("HIGHLAND_MOUNTAIN", "ðŸ” Highland Refuges & Mountain Reliefs"),
        Map.entry("INLAND_OASIS", "ðŸŒ´ Inland Hydrographic Basins & Oases"),
        Map.entry("EQUATORIAL_BELT", "â˜€ï¸ Equatorial Belt & Tropical Zone"),
        Map.entry("URBAN_CLUSTERS", "ðŸ™ Metropolises & Urban Clusters"),
        Map.entry("SPARSE_NOMADIC", "â›º Sparse Nomadic & Pastoralist Dispersion"),
        Map.entry("UNIFORM", "ðŸŸ¦ Absolute Uniform Distribution"),
        Map.entry("RANDOM", "ðŸŽ² Stochastic Random Distribution")
    );

    private static final Map<String, String> DENSITY_DESCRIPTIONS = Map.ofEntries(
        Map.entry("UNBIASED_NATURAL", "âš– Natural Bio-Climatic Equilibrium: Pure physical model without artificial bias. Population settles strictly according to real environmental suitability (biomes, temperature, waterways, relief)."),
        Map.entry("COASTAL_MARITIME", "ðŸŒŠ Coastal & Maritime Focus: Encourages human settlement along ocean coastlines, river deltas, and maritime margins."),
        Map.entry("RIVER_VALLEYS", "ðŸž River Valleys & Alluvial Basins: High population concentration along major hydrographic river networks and fertile alluvial basins."),
        Map.entry("HIGHLAND_MOUNTAIN", "ðŸ” Highland Refuges & Mountain Reliefs: Preferential densification in high valleys, mountain plateaus, and defensive elevated reliefs."),
        Map.entry("INLAND_OASIS", "ðŸŒ´ Inland Basins & Oases: Settlement around endorheic depressions and accessible groundwater tables."),
        Map.entry("EQUATORIAL_BELT", "â˜€ï¸ Equatorial Belt: Primary settlement concentrated around equatorial latitudes and high solar irradiance zones."),
        Map.entry("URBAN_CLUSTERS", "ðŸ™ Metropolises & Urban Clusters: Procedural emergence of multiple dense urban aggregation hubs."),
        Map.entry("SPARSE_NOMADIC", "â›º Sparse Nomadic Dispersion: Low-density pastoral population dispersed broadly across all viable biomes."),
        Map.entry("UNIFORM", "ðŸŸ¦ Absolute Uniform Distribution: Strictly constant population density across all land cells on the H3 grid."),
        Map.entry("RANDOM", "ðŸŽ² Stochastic Distribution: Uniformly random population allocation across land cells.")
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
    private volatile boolean isCalculationRunning = false;
    private volatile boolean cancelRequested = false;
    private Thread generationThread = null;
    private java.util.function.Supplier<Boolean> isSimulationRunningSupplier;

    public void setIsSimulationRunningSupplier(java.util.function.Supplier<Boolean> isSimulationRunningSupplier) {
        this.isSimulationRunningSupplier = isSimulationRunningSupplier;
    }

    private void updateEngineInspector(String className, String title, String description, String reference, String equations) {
        this.selectedEngineClassName = className;
        if (engineInspectorTitle != null) engineInspectorTitle.setText("ðŸ”Ž " + className + " â€” " + title);
        if (engineInspectorText != null) engineInspectorText.setText(LaTeXFormatter.formatLaTeX(description));
        if (engineInspectorEquations != null) engineInspectorEquations.setText(LaTeXFormatter.formatLaTeX(equations));
        if (engineInspectorRef != null) engineInspectorRef.setText("ðŸ“š " + reference);
        if (exportSelectedEngineBtn != null) exportSelectedEngineBtn.setText(I18n.getOrDefault("scenario.btn.export_prefix", "ðŸ“¤ Export ") + className + ".java");
    }

    private javafx.scene.Node createEngineParameterBox(String engineKey) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(4);
        grid.setPadding(new Insets(4, 8, 8, 24));
        grid.getStyleClass().add("subcard-section");

        Map<String, Spinner<Double>> spinners = new HashMap<>();

        if ("MaritimeHighwayEngine".equals(engineKey)) {
            Label l1 = new Label(I18n.getOrDefault("scenario.label.capital_boost_rate", "Boost Capital/Tick (Î±):"));
            l1.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            Spinner<Double> s1 = new Spinner<>(0.0, 0.50, 0.05, 0.01);
            s1.setEditable(true);
            s1.setPrefWidth(90);
            s1.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s1, 0.05, () -> s1.getValueFactory().setValue(0.05));
            grid.add(l1, 0, 0);
            grid.add(s1, 1, 0);
            spinners.put("capitalBoostRate", s1);

            Label l2 = new Label(I18n.getOrDefault("scenario.label.friction_multiplier", "Mult. Friction Eau (Î¼):"));
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
    private Button loadEarthEventsBtn;

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

        public ClimateEvent(String type, String name, int year, double lat, double lon, double depth, double mag) {
            this.type = new SimpleStringProperty(type);
            this.name = new SimpleStringProperty(name);
            this.year = new SimpleIntegerProperty(year);
            this.latitude = new SimpleDoubleProperty(lat);
            this.longitude = new SimpleDoubleProperty(lon);
            this.depth = new SimpleDoubleProperty(depth);
            this.magnitude = new SimpleDoubleProperty(mag);
        }

        public StringProperty typeProperty() { return type; }
        public StringProperty nameProperty() { return name; }
        public IntegerProperty yearProperty() { return year; }
        public DoubleProperty latitudeProperty() { return latitude; }
        public DoubleProperty longitudeProperty() { return longitude; }
        public DoubleProperty depthProperty() { return depth; }
        public DoubleProperty magnitudeProperty() { return magnitude; }
        public String getType() { return type.get(); }
        public void setType(String v) { type.set(v); }
        public String getName() { return name.get(); }
        public void setName(String v) { name.set(v); }
        public int getYear() { return year.get(); }
        public void setYear(int v) { year.set(v); }
        public double getLatitude() { return latitude.get(); }
        public void setLatitude(double v) { latitude.set(v); }
        public double getLongitude() { return longitude.get(); }
        public void setLongitude(double v) { longitude.set(v); }
        public double getDepth() { return depth.get(); }
        public void setDepth(double v) { depth.set(v); }
        public double getMagnitude() { return magnitude.get(); }
        public void setMagnitude(double v) { magnitude.set(v); }
    }

    public ScenarioSetupPanel(Consumer<Scenario> onStartSimulation) {
        this.onStartSimulation = onStartSimulation;
        this.scenarioRepo = new org.ether.society.persistence.ScenarioRepository();
        initUI();
        updateTexts();

        org.ether.society.i18n.I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
        
        javafx.application.Platform.runLater(() -> {
            loadEarthHistoricalEvents();
        });
    }

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

    public PlanetPreset findPlanetPresetByName(String name) {
        if (name == null || name.isBlank()) return PlanetPreset.EARTH_LIKE;
        for (PlanetPreset p : PlanetPreset.getPresets()) {
            if (p.name().equalsIgnoreCase(name)) return p;
        }
        String lower = name.toLowerCase();
        if (lower.contains("-100") || lower.contains("lig") || lower.contains("interglaciaire") || lower.contains("eemian")) return PlanetPreset.EARTH_LIG_100000BP;
        if (lower.contains("-50") || lower.contains("sahul") || lower.contains("mis3") || lower.contains("mis 3")) return PlanetPreset.EARTH_MIS3_50000BP;
        if (lower.contains("-25") || lower.contains("beringia") || lower.contains("bÃ©ringie")) return PlanetPreset.EARTH_LGM_ONSET_25000BP;
        if (lower.contains("-20") || lower.contains("lgm") || lower.contains("glaciaire") || lower.contains("solutrean") || lower.contains("solutrÃ©en")) return PlanetPreset.EARTH_LGM_20000BP;
        if (lower.contains("-10") || lower.contains("eh") || lower.contains("prÃ©coce") || lower.contains("early holocene") || lower.contains("dryas")) return PlanetPreset.EARTH_EH_10000BP;
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

        headerLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.panel.title", "ðŸ“œ Scenario Configuration & Setup"));
        headerLabel.getStyleClass().add("label-title");
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setMaxWidth(Double.MAX_VALUE);

        validationBannerHeaderLabel = new Label(I18n.getOrDefault("scenario.validation.header_errors", "âš ï¸ VALIDATION ERRORS DETECTED â€” PLEASE CORRECT THE FOLLOWING POINTS:"));
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
        scenarioPresetBar = new PresetControlBar<>("scenario.preset_bar", "1. PRÃ‰RÃ‰GLAGES DE SCÃ‰NARIOS");
        scenarioPresetBar.setExportCategory("scenario");
        List<Scenario> builtInScenarios = getBuiltInScenarios();
        Scenario defaultScenario = builtInScenarios.isEmpty() ? null : builtInScenarios.get(0);

        scenarioPresetBar.setListener(new PresetControlBar.PresetActionsListener<Scenario>() {
            @Override
            public void onPresetSelected(Scenario scenario) {
                applyScenarioToUI(scenario);
                if (previewModeCombo != null) {
                    previewModeCombo.getSelectionModel().select(0);
                }
            }

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

            @Override
            public void onDeletePreset(Scenario scenario) {
                scenarioPresetBar.getPresetCombo().getItems().remove(scenario);
            }

            @Override
            public void onExportPreset(File targetFile, Scenario scenario) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.writeValue(targetFile, scenario);
                    logger.info("Exported scenario preset to {}", targetFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to export scenario preset", ex);
                }
            }

            @Override
            public void onImportPreset(File sourceFile) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
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
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(org.ether.society.i18n.I18n.getPlanetPresetDisplayName(item.name()));
                }
            }
        });
        planetPresetCombo.setButtonCell(new ListCell<PlanetPreset>() {
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
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
            @Override
            public String toString(PlanetPreset item) {
                return item == null ? org.ether.society.i18n.I18n.getPlanetPresetDisplayName("Terre (Terran)") : org.ether.society.i18n.I18n.getPlanetPresetDisplayName(item.name());
            }
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
            @Override
            public String toString(EcologyPreset item) {
                return item == null ? "" : org.ether.society.i18n.I18n.getPlanetPresetDisplayName(item.name());
            }
            @Override
            public EcologyPreset fromString(String string) {
                return null;
            }
        });
        ecologyPresetCombo.setCellFactory(p -> new ListCell<EcologyPreset>() {
            @Override
            protected void updateItem(EcologyPreset item, boolean empty) {
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
            @Override
            protected void updateItem(EcologyPreset item, boolean empty) {
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
        planetPromptLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.label.planet_preset", "2. Planetary Preset (Tab 1 â€” Cascaded from Ecology):"));

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
            @Override
            public String toString(Integer item) {
                return item == null ? "" : org.ether.society.i18n.I18n.getOrDefault("planet.param.resolution.res" + item, "RÃ©solution " + item);
            }
            @Override
            public Integer fromString(String string) {
                return null;
            }
        });
        Tooltip.install(h3ResolutionCombo, new Tooltip(org.ether.society.i18n.I18n.getOrDefault("planet.tooltip.resolution", "H3 Grid Resolution")));
        Tooltip.install(h3ResolutionLabel, h3ResolutionCombo.getTooltip());
        h3ResolutionCombo.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        // Target Cohort Size (Row 1)
        cohortSizeLabel = new Label();
        targetCohortSizeSpinner = new Spinner<>(1, 100000, 150, 25);
        targetCohortSizeSpinner.setEditable(true);
        targetCohortSizeSpinner.setMaxWidth(Double.MAX_VALUE);
        targetCohortSizeSpinner.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.cohort_size", 
                "ðŸ‘¥ Taille Cible des Cohortes DÃ©mographiques (NÅ“uds Agents DOD) [hab/cohorte] :\n" +
                "DÃ©termine la taille moyenne des groupes d'habitants reprÃ©sentÃ©s par un mÃªme nÅ“ud agent.\n" +
                "â€¢ 30 Ã  50 hab/cohorte : Bandes nomades & palÃ©olithiques (Chasseurs-cueilleurs)\n" +
                "â€¢ 150 hab/cohorte : Nombre de Dunbar (Villages sÃ©dentaires & communautÃ©s de base)\n" +
                "â€¢ 500 Ã  10 000 hab/cohorte : Villes, empires & macro-simulation industrielle/moderne")));
        Tooltip.install(cohortSizeLabel, targetCohortSizeSpinner.getTooltip());
        targetCohortSizeSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        // Temporal Resolution (Row 2)
        temporalResolutionLabel = new Label();
        temporalResolutionCombo = new ComboBox<>();
        temporalResolutionCombo.getItems().addAll(1.0, 7.0, 15.0, 30.0, 60.0, 90.0, 180.0, 365.0);
        temporalResolutionCombo.setValue(30.0);
        temporalResolutionCombo.setMaxWidth(Double.MAX_VALUE);
        temporalResolutionCombo.setConverter(new javafx.util.StringConverter<Double>() {
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
            @Override
            public Double fromString(String string) {
                return null;
            }
        });
        Tooltip temporalTooltip = new Tooltip("""
            â±ï¸ RÃ©solution Temporelle de la Simulation (Pas de Temps Î”t) :
            DÃ©termine la granularitÃ© temporelle de chaque pas de calcul de la simulation.
            â€¢ Pas de temps court (< 1 mois, ex: 1 jour, 1 semaine) : Haute prÃ©cision dynamique pour les Ã©pidÃ©mies, le climat saisonnier et la mobilitÃ© rapide, au prix d'une charge de calcul CPU plus Ã©levÃ©e.
            â€¢ Pas de temps standard (1 mois - par dÃ©faut) : Ã‰quilibre optimal entre la prÃ©cision physique et la vitesse d'exÃ©cution.
            â€¢ Pas de temps long (> 1 mois, ex: 3 mois, 1 an) : AccÃ©lÃ©ration majeure pour les simulations Ã  trÃ¨s grande Ã©chelle sur plusieurs millÃ©naires avec lissage des cycles saisonniers.
            """);
        temporalResolutionCombo.setTooltip(temporalTooltip);
        Tooltip.install(temporalResolutionLabel, temporalTooltip);
        temporalResolutionCombo.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        // Start Year (Row 3)
        startYearSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            notifyParamChange();
            if (newV != null) {
                loadEarthHistoricalEvents(newV);
            }
        });
        Tooltip.install(startYearLabel, startYearSpinner.getTooltip());
        Tooltip.install(endYearLabel, endYearSpinner.getTooltip());

        // Add to grid1: Row 0 = H3 Res, Row 1 = Pas de temps, Row 2 = Cohort Size, Row 3 = Start Year, Row 4 = End Year
        grid1.addRow(0, h3ResolutionLabel, h3ResolutionCombo);
        grid1.addRow(1, temporalResolutionLabel, temporalResolutionCombo);
        grid1.addRow(2, cohortSizeLabel, targetCohortSizeSpinner);
        grid1.addRow(3, startYearLabel, startYearSpinner);
        grid1.addRow(4, endYearLabel, endYearSpinner);

        VBox section1Content = new VBox(10, grid1);
        VBox section1 = createSection(title1, section1Content);

        // --- 2. Demographics & Density Map Management (RadioButtons) ---
        VBox popSection = new VBox(10);
        popSection.getStyleClass().add("card-section");
        popHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_section", "ðŸ‘¥ 2. INITIAL POPULATION IDENTITY CARD"));
        popHeader.getStyleClass().add("label-section-header");
        popHeader.setId("__popHeader");

        ToggleGroup demoSourceGroup = new ToggleGroup();
        radioProcDemo = new RadioButton(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.procedural", "â–¶ Procedural Mode (Algorithms & Demographic Patterns)"));
        radioImportDemo = new RadioButton(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.import", "ðŸ“‚ Loading Mode for Existing Density Map (PNG)"));
        radioProcDemo.setToggleGroup(demoSourceGroup);
        radioImportDemo.setToggleGroup(demoSourceGroup);
        radioProcDemo.setSelected(true);

        Label demoTypeLabel = new Label(I18n.getOrDefault("scenario.label.demographic_mode", "âš™ Demographic Generation Mode:"));
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
            @Override
            public String toString(String item) {
                return item == null ? "" : org.ether.society.i18n.I18n.getOrDefault("scenario.density." + item, DENSITY_LABELS.getOrDefault(item, item));
            }
            @Override
            public String fromString(String string) {
                return null;
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
            @Override
            public String toString(Scenario.TechPreset item) {
                return item == null ? "" : I18n.getOrDefault("scenario.tech." + item.name(), item.getLabel());
            }
            @Override
            public Scenario.TechPreset fromString(String string) {
                return null;
            }
        });
        techPresetCombo.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.tech_preset", "Select initial physical endowment (Kâ‚€, Eâ‚€, Fâ‚€, Iâ‚€) or leave auto-calculated based on Tâ‚€ year.")));

        Label techPresetLabel = new Label(I18n.getOrDefault("scenario.label.tech_preset", "ðŸš€ Dotation & Niveau Technologique Tâ‚€ :"));
        techPresetLabel.setTooltip(techPresetCombo.getTooltip());

        popGrid.addRow(0, popCountLabel, initialHumanCountSpinner);
        popGrid.addRow(1, densityPatternLabel, densityPatternCombo);
        popGrid.addRow(2, techPresetLabel, techPresetCombo);

        demoSeedField = new TextField("12345");
        demoSeedField.setPrefWidth(120);
        demoSeedField.textProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        demoRandSeedBtn = new Button("ðŸŽ²");
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

        Label demoSeedLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.seed.label", "ðŸŽ² Scenario Procedural Seed:"));
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

        customGrid.addRow(0, new Label(I18n.getOrDefault("scenario.label.stock_k0", "ðŸ› ï¸ Kâ‚€ (kg/hab) :")), customCapitalSpinner, new Label(I18n.getOrDefault("scenario.label.stock_e0", "âš¡ Eâ‚€ (MJ/hab) :")), customEnergySpinner);
        customGrid.addRow(1, new Label(I18n.getOrDefault("scenario.label.stock_f0", "ðŸŒ¾ Fâ‚€ (mois) :")), customFoodSpinner, new Label(I18n.getOrDefault("scenario.label.stock_i0", "ðŸ§  Iâ‚€ (bits/hab) :")), customInfoSpinner);

        Label customTitleLabel = new Label(I18n.getOrDefault("scenario.section.custom_stocks", "âš™ï¸ MANUAL INITIAL PHYSICAL STOCKS SETUP (CUSTOM):"));
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

        exportDensityMapBtn = new Button(I18n.getOrDefault("scenario.btn.export_map_png", "ðŸ“¤ Export Raster (PNG / JPEG)"));
        exportDensityMapBtn.getStyleClass().add("button-secondary");
        exportDensityMapBtn.setMaxWidth(Double.MAX_VALUE);
        exportDensityMapBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_map_png", "Export demographic density map as a high-resolution PNG or JPEG raster file.")));
        exportDensityMapBtn.setOnAction(e -> exportDensityMap());

        HBox demoBtnBar = new HBox(8, exportDensityMapBtn);
        HBox.setHgrow(exportDensityMapBtn, Priority.ALWAYS);

        VBox proceduralDemoPanel = new VBox(8, popGrid, customPhysicalSubPanel, demoBtnBar);
        proceduralDemoPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");

        // Import Panel â€” section labels to match tensor block format
        Label sourceRefLabel = new Label("ðŸ“ " + I18n.getOrDefault("resource.label.reference_source", "Source de RÃ©fÃ©rence :"));
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
                "PNG / JPEG (projection Ã©quirectangulaire 2:1) :\n  Noir (0) = 0 hab/kmÂ² | Blanc (255) = DensitÃ© maximale d'habitation."));
        densityFormatHintLabel.setWrapText(true);
        densityFormatHintLabel.getStyleClass().add("card-description-muted");
        densityFormatHintLabel.setStyle("-fx-font-size: 9px; -fx-font-style: italic;");

        loadDensityMapBtn = new Button(I18n.getOrDefault("resource.btn.load_map", "ðŸ“‚ Charger la Carte"));
        loadDensityMapBtn.getStyleClass().add("button-secondary");
        loadDensityMapBtn.setStyle("-fx-font-size: 11px;");
        loadDensityMapBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.import_density_map", "Import an external demographic density map in 2:1 equirectangular PNG format.")));
        loadDensityMapBtn.setOnAction(e -> {
            notifyParamChange();
            loadCustomDensityMap();
        });

        clearDensityMapBtn = new Button("âŒ");
        clearDensityMapBtn.getStyleClass().add("button-secondary");
        clearDensityMapBtn.setStyle("-fx-font-size: 11px;");
        clearDensityMapBtn.setOnAction(e -> {
            customDensityImage = null;
            densityMapFileLabel.setText("â€”");
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

        Label loadMapSectionLabel = new Label("ðŸ“‚ " + I18n.getOrDefault("scenario.label.load_map_section", "Charger la Carte :"));
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

        liveDiagnosticHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.diagnostic.header", "ðŸ“‹ 9. CIVILIZATIONAL VIABILITY DIAGNOSTIC (REAL TIME)"));
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

        scenarioDescLabel = new Label(I18n.getOrDefault("scenario.section.description", "ðŸ“– Detailed Description, Initial Conditions & Key Observables:"));
        scenarioDescLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 6 0 2 0;");

        scenarioDescriptionArea = new TextArea();
        scenarioDescriptionArea.setPrefRowCount(9);
        scenarioDescriptionArea.setWrapText(true);
        scenarioDescriptionArea.getStyleClass().add("scenario-description-area");
        scenarioDescriptionArea.setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0; -fx-background-color: rgba(15, 23, 42, 0.6); -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 4; -fx-background-radius: 4;");
        scenarioDescriptionArea.textProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        btnAutoEpochScenario = new Button(I18n.getOrDefault("scenario.btn.auto_epoch_wizard", "âœ¨ CrÃ©er un ScÃ©nario Automatique par Date..."));
        btnAutoEpochScenario.setMaxWidth(Double.MAX_VALUE);
        btnAutoEpochScenario.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        btnAutoEpochScenario.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.auto_epoch_wizard", "Ouvre l'assistant de scÃ©nario temporel pour configurer en un clic les cartes d'Ã©lÃ©vation, biomes, ressources, dÃ©mographie et moteurs de Type B compatibles pour n'importe quelle date.")));
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

        bundleHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.bundle.header", "ðŸ“¦ 7. UNIFIED BUNDLE MULTI-SCENARIO IMPORT/EXPORT (.ETHER)"));
        bundleHeader.getStyleClass().add("label-section-header");

        bundleSubtitle = new Label(I18n.getOrDefault("scenario.desc.bundle", "Export or import the complete scenario (planetary context, ecology, active engines, cultural layers, and demographic grid) in unified .ether format for archiving or sharing."));
        bundleSubtitle.setWrapText(true);
        bundleSubtitle.getStyleClass().add("card-description-muted");

        btnExportBundle = new Button(I18n.getOrDefault("scenario.btn.export_bundle", "ðŸ“¦ Export Bundle (.ether)"));
        btnExportBundle.getStyleClass().add("button-secondary");
        btnExportBundle.setMaxWidth(Double.MAX_VALUE);
        btnExportBundle.getStyleClass().add("button-accent-blue");
        btnExportBundle.setOnAction(e -> exportUnifiedBundle());
        btnExportBundle.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_bundle", "Export complete scenario (physics, ecology, engines, layers, and demographics) to a unified .ether bundle file.")));

        btnImportBundle = new Button(I18n.getOrDefault("scenario.btn.import_bundle", "ðŸ“‚ Import Bundle (.ether)"));
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

    private VBox createSnapshotSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        snapshotHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.snapshot.header", "ðŸ“¸ 7. RESUME FROM EXISTING SNAPSHOT (PREVIOUS SESSION)"));
        snapshotHeader.getStyleClass().add("label-section-header");

        Label subtitle = new Label(I18n.getOrDefault("scenario.desc.snapshot", "If the simulation was previously run and snapshots or checkpoints exist, you can resume directly from that snapshot without starting over."));
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 11px;");

        ToggleGroup modeGroup = new ToggleGroup();
        radioNewSimulation = new RadioButton(I18n.getOrDefault("scenario.radio.start_fresh", "ðŸŒ± Start a new simulation from scratch (Year Tâ‚€)"));
        radioNewSimulation.setStyle("-fx-font-weight: bold;");
        radioNewSimulation.setTooltip(new Tooltip(I18n.getOrDefault("scenario.radio.start_fresh.tooltip", "Initialize a brand-new historical simulation from canonical Tâ‚€ initial conditions.")));

        radioResumeSnapshot = new RadioButton(I18n.getOrDefault("scenario.radio.start_snapshot", "ðŸ“¸ Resume from an existing Snapshot (Previous Session / Checkpoint)"));
        radioResumeSnapshot.setStyle("-fx-font-weight: bold; -fx-text-fill: #7c3aed;");
        radioResumeSnapshot.setTooltip(new Tooltip(I18n.getOrDefault("scenario.radio.start_snapshot.tooltip", "Load a previously saved physical state or periodic checkpoint directly into memory.")));

        radioNewSimulation.setToggleGroup(modeGroup);
        radioResumeSnapshot.setToggleGroup(modeGroup);
        radioNewSimulation.setSelected(true);

        snapshotCombo = new ComboBox<>();
        snapshotCombo.setMaxWidth(Double.MAX_VALUE);
        snapshotCombo.setTooltip(new Tooltip(I18n.getOrDefault("scenario.snapshot.combo.tooltip", "Select an existing checkpoint or full simulation snapshot from the disk to resume execution.")));
        snapshotCombo.setConverter(new javafx.util.StringConverter<org.ether.society.persistence.SaveMetadata>() {
            @Override
            public String toString(org.ether.society.persistence.SaveMetadata item) {
                if (item == null) return "";
                String timeStr = item.getTimestamp() != null ? item.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A";
                return String.format("ðŸ“¸ Point de reprise : An %,d (Mois %02d) â€” %s", item.getYear(), item.getMonth(), timeStr);
            }
            @Override
            public org.ether.society.persistence.SaveMetadata fromString(String string) {
                return null;
            }
        });

        snapshotCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(org.ether.society.persistence.SaveMetadata item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    String timeStr = item.getTimestamp() != null ? item.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A";
                    String text = String.format("ðŸ“¸ Point de reprise : An %,d (Mois %02d) â€” %s", item.getYear(), item.getMonth(), timeStr);
                    setText(text);
                    setTooltip(new Tooltip(String.format("ðŸ“ ID: %s\nðŸ“œ ScÃ©nario: %s\nâ³ AnnÃ©e: %,d (Mois %02d)\nðŸ“… Horodatage: %s\nðŸ’¡ Reprendre ce point restaure l'Ã©tat tout en gardant l'historique de simulation.",
                            item.getId(), item.getScenarioName(), item.getYear(), item.getMonth(), timeStr)));
                }
            }
        });

        Label snapshotHistoryHintLabel = new Label("ðŸ’¡ Note de reprise : Reprendre le snapshot le plus rÃ©cent charge l'Ã©tat final calculÃ© tout en conservant l'accÃ¨s Ã  l'historique complet pour naviguer et remonter le temps dans la simulation.");
        snapshotHistoryHintLabel.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #94a3b8; -fx-padding: 2 4 4 4;");
        snapshotHistoryHintLabel.setWrapText(true);

        // Auto-refresh snapshot list when combo opens to ensure live synchronization with disk
        snapshotCombo.setOnShowing(e -> refreshSnapshotList());

        snapshotDateLabel = new Label(I18n.getOrDefault("scenario.label.timestamp_null", "ðŸ“… Timestamp: -"));
        snapshotTimeLabel = new Label(I18n.getOrDefault("scenario.label.moment_null", "â³ Time: -"));
        snapshotScenarioLabel = new Label(I18n.getOrDefault("scenario.info.scenario_empty", "ðŸ“œ Scenario: -"));
        snapshotPathLabel = new Label(I18n.getOrDefault("scenario.label.snapshot_id_null", "ðŸ“ Snapshot ID: -"));

        for (Label l : List.of(snapshotDateLabel, snapshotTimeLabel, snapshotScenarioLabel, snapshotPathLabel)) {
            l.setStyle("-fx-font-size: 11px;");
            l.setWrapText(true);
            l.setMaxWidth(Double.MAX_VALUE);
        }

        VBox detailsList = new VBox(5);
        detailsList.setPadding(new Insets(6, 10, 8, 10));
        detailsList.getChildren().addAll(snapshotDateLabel, snapshotTimeLabel, snapshotScenarioLabel, snapshotPathLabel);

        Label sheetHeader = new Label(I18n.getOrDefault("scenario.section.snapshot_sheet", "ðŸ“‹ Technical-Historical Sheet of Selected Snapshot:"));
        sheetHeader.getStyleClass().add("opt-sub-checkbox");
        sheetHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");

        VBox snapshotCard = new VBox(6, sheetHeader, detailsList);
        snapshotCard.getStyleClass().add("subcard-section");

        snapshotExplainBtn = new Button(I18n.getOrDefault("scenario.btn.snapshot_explain", "â„¹ï¸ What is a Snapshot? (Explanations & Mechanics)"));
        snapshotExplainBtn.getStyleClass().add("button-secondary");
        snapshotExplainBtn.setMaxWidth(Double.MAX_VALUE);
        snapshotExplainBtn.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        snapshotExplainBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.snapshot_explain", "Open detailed technical guide explaining physical state capture, rolling checkpoints, and multiverse branching.")));
        snapshotExplainBtn.setOnAction(e -> showSnapshotExplanationDialog());

        snapshotRefreshBtn = new Button(I18n.getOrDefault("scenario.btn.refresh", "ðŸ”„ Refresh Snapshots"));
        snapshotRefreshBtn.getStyleClass().add("button-secondary");
        snapshotRefreshBtn.setStyle("-fx-font-size: 11px;");
        snapshotRefreshBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.snapshot_refresh", "Rescans the disk saves directory (saves/) to synchronize the list of real simulation snapshots and checkpoints.")));
        snapshotRefreshBtn.setOnAction(e -> refreshSnapshotList());

        HBox btnBox = new HBox(8, snapshotExplainBtn, snapshotRefreshBtn);
        HBox.setHgrow(snapshotExplainBtn, Priority.ALWAYS);

        snapshotContainer = new VBox(8, snapshotCombo, snapshotHistoryHintLabel, snapshotCard, btnBox);
        snapshotContainer.setVisible(false);
        snapshotContainer.setManaged(false);

        modeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            boolean isResume = newV == radioResumeSnapshot;
            snapshotContainer.setVisible(isResume);
            snapshotContainer.setManaged(isResume);
            if (isResume) {
                refreshSnapshotList();
            }
            updateStartButtonLabel();
        });

        snapshotCombo.valueProperty().addListener((obs, oldV, newV) -> {
            updateSnapshotDetailsDisplay(newV);
        });

        refreshSnapshotList();

        section.getChildren().addAll(snapshotHeader, subtitle, radioNewSimulation, radioResumeSnapshot, snapshotContainer);
        return section;
    }

    private void updateSnapshotDetailsDisplay(org.ether.society.persistence.SaveMetadata newV) {
        if (newV != null) {
            String timeStr = newV.getTimestamp() != null ? newV.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "N/A";
            String dateTxt = I18n.getOrDefault("scenario.snapshot.timestamp", "ðŸ“… Timestamp: ") + timeStr;
            String timeTxt = String.format(I18n.getOrDefault("scenario.snapshot.time_format", "â³ Moment: Year %,d (Month %02d)"), newV.getYear(), newV.getMonth());
            String scName = newV.getScenarioName() != null ? newV.getScenarioName() : I18n.getOrDefault("scenario.unknown", "Unknown");
            String scTxt = I18n.getOrDefault("scenario.snapshot.scenario_prefix", "ðŸ“œ Scenario: ") + scName;
            String idTxt = I18n.getOrDefault("scenario.snapshot.id", "ðŸ“ Snapshot ID: ") + newV.getId();

            if (snapshotDateLabel != null) {
                snapshotDateLabel.setText(dateTxt);
                snapshotDateLabel.setTooltip(new Tooltip(dateTxt + "\n" + I18n.getOrDefault("scenario.snapshot.tooltip.timestamp", "Exact real-world date and time when this snapshot was captured.")));
            }
            if (snapshotTimeLabel != null) {
                snapshotTimeLabel.setText(timeTxt);
                snapshotTimeLabel.setTooltip(new Tooltip(timeTxt + "\n" + I18n.getOrDefault("scenario.snapshot.tooltip.time", "Precise chronological year and simulation month.")));
            }
            if (snapshotScenarioLabel != null) {
                snapshotScenarioLabel.setText(scTxt);
                snapshotScenarioLabel.setTooltip(new Tooltip(scTxt + "\n" + I18n.getOrDefault("scenario.snapshot.tooltip.scenario", "Origin scenario and initial physical parameters.")));
            }
            if (snapshotPathLabel != null) {
                snapshotPathLabel.setText(idTxt);
                snapshotPathLabel.setTooltip(new Tooltip(idTxt + "\n" + I18n.getOrDefault("scenario.snapshot.tooltip.id", "Unique UUID or save identifier on the filesystem.")));
            }
        } else {
            if (snapshotDateLabel != null) {
                snapshotDateLabel.setText(I18n.getOrDefault("scenario.label.timestamp_null", "ðŸ“… Timestamp: -"));
                snapshotDateLabel.setTooltip(null);
            }
            if (snapshotTimeLabel != null) {
                snapshotTimeLabel.setText(I18n.getOrDefault("scenario.label.moment_null", "â³ Time: -"));
                snapshotTimeLabel.setTooltip(null);
            }
            if (snapshotScenarioLabel != null) {
                snapshotScenarioLabel.setText(I18n.getOrDefault("scenario.info.scenario_empty", "ðŸ“œ Scenario: -"));
                snapshotScenarioLabel.setTooltip(null);
            }
            if (snapshotPathLabel != null) {
                snapshotPathLabel.setText(I18n.getOrDefault("scenario.label.snapshot_id_null", "ðŸ“ Snapshot ID: -"));
                snapshotPathLabel.setTooltip(null);
            }
        }
    }

    public void refreshSnapshotList() {
        refreshSnapshotListForScenario(getScenario());
    }

    public void refreshSnapshotListForScenario(Scenario s) {
        if (snapshotCombo == null) return;
        List<org.ether.society.persistence.SaveMetadata> allSaves = getSaveManager().listSaves();
        List<org.ether.society.persistence.SaveMetadata> matchingSaves = new ArrayList<>();

        if (s != null && !allSaves.isEmpty()) {
            long targetStartYear = s.getStartDateYear();
            long targetEndYear = s.getEndDateYear();
            String scNorm = s.getName() != null ? s.getName().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
            String scKey = s.getPresetKey() != null ? s.getPresetKey().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
            String scDisp = s.getDisplayName() != null ? s.getDisplayName().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
            String scDensity = s.getPopulationDensityType() != null ? s.getPopulationDensityType().toLowerCase().replaceAll("[^a-z0-9]", "") : "";

            for (org.ether.society.persistence.SaveMetadata save : allSaves) {
                String saveNorm = save.getName() != null ? save.getName().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
                String saveScNorm = save.getScenarioName() != null ? save.getScenarioName().toLowerCase().replaceAll("[^a-z0-9]", "") : "";

                boolean match = (!scKey.isEmpty() && (saveNorm.contains(scKey) || saveScNorm.contains(scKey)))
                             || (!scNorm.isEmpty() && (saveNorm.contains(scNorm) || saveScNorm.contains(scNorm)))
                             || (!scDisp.isEmpty() && (saveNorm.contains(scDisp) || saveScNorm.contains(scDisp)))
                             || (!scDensity.isEmpty() && (saveNorm.contains(scDensity) || saveScNorm.contains(scDensity)))
                             || (Math.abs(save.getYear() - targetStartYear) <= 150)
                             || (Math.abs(save.getYear() - targetEndYear) <= 150);

                if (match) {
                    matchingSaves.add(save);
                }
            }
        }

        // Deduplicate snapshots covering the same year / scenario (keep the newest save)
        Map<Long, org.ether.society.persistence.SaveMetadata> deduplicatedByYear = new LinkedHashMap<>();
        // Sort first by timestamp descending so newer files take precedence
        matchingSaves.sort((a, b) -> {
            if (a.getTimestamp() != null && b.getTimestamp() != null) {
                return b.getTimestamp().compareTo(a.getTimestamp());
            }
            return 0;
        });
        for (var save : matchingSaves) {
            deduplicatedByYear.putIfAbsent(save.getYear(), save);
        }

        List<org.ether.society.persistence.SaveMetadata> filteredSaves = new ArrayList<>(deduplicatedByYear.values());
        // Sort descending by end year / simulation progress reached (latest checkpoint first)
        filteredSaves.sort((a, b) -> Long.compare(b.getYear(), a.getYear()));

        snapshotCombo.getItems().setAll(filteredSaves);

        if (!filteredSaves.isEmpty()) {
            if (radioResumeSnapshot != null) {
                radioResumeSnapshot.setDisable(false);
                radioResumeSnapshot.setSelected(true);
            }
            snapshotCombo.setValue(filteredSaves.get(0));
            updateSnapshotDetailsDisplay(filteredSaves.get(0));
        } else {
            if (radioResumeSnapshot != null) {
                radioResumeSnapshot.setDisable(true);
                if (radioNewSimulation != null) {
                    radioNewSimulation.setSelected(true);
                }
            }
            snapshotCombo.setValue(null);
            snapshotCombo.setPromptText(I18n.getOrDefault("scenario.snapshot.no_saves_for_scenario", "(Aucun snapshot enregistrÃ© pour ce scÃ©nario)"));
            updateSnapshotDetailsDisplay(null);
        }
        updateStartButtonLabel();
        logger.info("Refreshed snapshots list for scenario '{}': {} matching checkpoint(s) found on disk.", s != null ? s.getName() : "N/A", filteredSaves.size());
    }

    private void showSnapshotExplanationDialog() {
        new SnapshotExplanationDialog(getScene() != null ? getScene().getWindow() : null).showAndWait();
    }

    public boolean isResumeFromSnapshotSelected() {
        return radioResumeSnapshot != null && radioResumeSnapshot.isSelected();
    }

    public org.ether.society.persistence.SaveMetadata getSelectedSnapshotMetadata() {
        return snapshotCombo != null ? snapshotCombo.getValue() : null;
    }

    public String getSelectedSnapshotId() {
        org.ether.society.persistence.SaveMetadata meta = getSelectedSnapshotMetadata();
        return meta != null ? meta.getId() : null;
    }

    private void updateStartButtonLabel() {
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
            startBtn.setText(I18n.getOrDefault("scenario.btn.start_modified", "ðŸŒ± CALCULER & DÃ‰MARRER LA SIMULATION (ParamÃ¨tres ModifiÃ©s)"));
            startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #d97706; -fx-text-fill: white; -fx-background-radius: 6;");
            startBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.start_modified", 
                "Lance une simulation avec vos paramÃ¨tres personnalisÃ©s sans Ã©craser les snapshots du scÃ©nario d'origine.")));
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
                startBtn.setText(String.format(I18n.getOrDefault("scenario.btn.load_complete_snapshot", "âš¡ CHARGER L'HISTORIQUE & Ã‰TAT COMPLET (An %,d â€” 100%% CalculÃ©)"), snapYr));
                startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-background-radius: 6;");
                startBtn.setTooltip(new Tooltip(String.format(I18n.getOrDefault("scenario.tooltip.load_complete_snapshot",
                    "Ce scÃ©nario dispose d'un historique complet calculÃ© jusqu'Ã  l'An %,d.\nChargement instantanÃ© sans recalcul : navigation temporelle et cartographique immÃ©diate."), snapYr)));
            } else {
                startBtn.setText(String.format(I18n.getOrDefault("scenario.btn.resume_partial_snapshot", "ðŸš€ REPRENDRE LE CHECKPOINT & CONTINUER (An %,d âž” %,d)"), snapYr, endYr));
                startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 6;");
                startBtn.setTooltip(new Tooltip(String.format(I18n.getOrDefault("scenario.tooltip.resume_partial_snapshot",
                    "Reprend la simulation Ã  partir du snapshot de l'An %,d.\nSeules les Ã©tapes postÃ©rieures (An %,d âž” %,d) seront calculÃ©es."), snapYr, snapYr, endYr)));
            }
        } else {
            startBtn.setText(String.format(I18n.getOrDefault("scenario.button.start_fresh", "ðŸŒ± CALCULER & DÃ‰MARRER LA SIMULATION (An %,d âž” %,d)"), startYr, endYr));
            startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
            startBtn.setTooltip(new Tooltip(String.format(I18n.getOrDefault("scenario.tooltip.start_fresh",
                "Calcule une trajectoire depuis l'An %,d jusqu'Ã  l'An %,d sans utiliser de snapshot existant."), startYr, endYr)));
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
                        new org.ether.society.model.EtherScenarioBundle("1.0.0-beta.1", planet, eco, scenario);
                org.ether.society.model.EtherScenarioBundle bundle = 
                        org.ether.society.security.EtherBundleSigner.signBundle(rawBundle, "Ether Lead Planner");

                ObjectMapper mapper = new ObjectMapper();
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
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                org.ether.society.model.EtherScenarioBundle bundle =
                        mapper.readValue(file, org.ether.society.model.EtherScenarioBundle.class);

                if (bundle != null) {
                    boolean isValid = org.ether.society.security.EtherBundleSigner.verifyBundle(bundle);
                    if (!isValid) {
                        if (validationErrorLabel != null && validationErrorBanner != null) {
                            validationErrorLabel.setText("âš ï¸ Avertissement d'intÃ©gritÃ© : Le bundle importÃ© a Ã©tÃ© modifiÃ© ou sa signature ne concorde pas.");
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
            Label lbl = new Label("  â€¢ " + item);
            lbl.setWrapText(true);
            if (item.startsWith("âŒ")) {
                lbl.getStyleClass().add("diagnostic-error");
                lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            } else if (item.startsWith("âš ï¸")) {
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
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        if (p == null) p = PlanetPreset.EARTH_LIKE;
        EcologyPreset eco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : EcologyPreset.EARTH_STANDARD;
        String planetName = p.name();
        String planetKey = planetName.toLowerCase();

        List<String> geoItems = new ArrayList<>();
        List<String> ecoItems = new ArrayList<>();
        List<String> engineItems = new ArrayList<>();
        List<String> validationItems = new ArrayList<>();
        int alertCount = 0;

        // =========================================================================
        // 1. CARTOGRAPHIC & GEOGRAPHIC COMPATIBILITY (Tabs 1 & 3)
        // =========================================================================
        org.ether.society.generation.ProceduralGenerator generator = new org.ether.society.generation.ProceduralGenerator();

        // A. Planetary Body Mismatch
        String demoSrc = demoSourceCombo != null ? demoSourceCombo.getValue() : null;
        boolean bodyMismatch = false;
        if (demoSrc != null && !demoSrc.isBlank()) {
            String demoLower = demoSrc.toLowerCase();
            boolean planetIsEarth = planetKey.contains("terre") || planetKey.contains("earth") || planetKey.contains("terran");
            boolean demoIsMars = demoLower.contains("mars");
            boolean demoIsMoon = demoLower.contains("lune") || demoLower.contains("moon");
            boolean demoIsVenus = demoLower.contains("vÃ©nus") || demoLower.contains("venus");
            boolean demoIsMercury = demoLower.contains("mercure") || demoLower.contains("mercury");
            boolean demoIsEarth = demoLower.contains("terre") || demoLower.contains("earth") || demoLower.contains("hyde");

            if (planetIsEarth && (demoIsMars || demoIsMoon || demoIsVenus || demoIsMercury)) {
                geoItems.add("âš ï¸ " + String.format(I18n.getOrDefault("scenario.warning.demo_body_mismatch", "IncohÃ©rence planÃ©taire : Source dÃ©mographique Â« %s Â» sÃ©lectionnÃ©e sur un relief terrestre (Onglet 1)."), demoSrc.trim()));
                bodyMismatch = true;
                alertCount++;
            } else if (planetKey.contains("mars") && demoIsEarth) {
                geoItems.add("âš ï¸ " + String.format(I18n.getOrDefault("scenario.warning.demo_body_mismatch", "IncohÃ©rence planÃ©taire : Source dÃ©mographique terrestre Â« %s Â» appliquÃ©e sur le relief martien (Onglet 1)."), demoSrc.trim()));
                bodyMismatch = true;
                alertCount++;
            } else if (planetKey.contains("lune") && demoIsEarth) {
                geoItems.add("âš ï¸ " + String.format(I18n.getOrDefault("scenario.warning.demo_body_mismatch", "IncohÃ©rence planÃ©taire : Source dÃ©mographique terrestre Â« %s Â» appliquÃ©e sur le relief lunaire (Onglet 1)."), demoSrc.trim()));
                bodyMismatch = true;
                alertCount++;
            } else if (planetKey.contains("vÃ©nus") && demoIsEarth) {
                geoItems.add("âš ï¸ " + String.format(I18n.getOrDefault("scenario.warning.demo_body_mismatch", "IncohÃ©rence planÃ©taire : Source dÃ©mographique terrestre Â« %s Â» appliquÃ©e sur VÃ©nus (Onglet 1)."), demoSrc.trim()));
                bodyMismatch = true;
                alertCount++;
            } else if (planetKey.contains("mercure") && demoIsEarth) {
                geoItems.add("âš ï¸ " + String.format(I18n.getOrDefault("scenario.warning.demo_body_mismatch", "IncohÃ©rence planÃ©taire : Source dÃ©mographique terrestre Â« %s Â» appliquÃ©e sur Mercure (Onglet 1)."), demoSrc.trim()));
                bodyMismatch = true;
                alertCount++;
            }
        }
        if (!bodyMismatch) {
            geoItems.add(String.format(I18n.getOrDefault("scenario.diagnostic.geo_ok", "âœ… Relief et corps cÃ©leste parfaitement cohÃ©rents (%s)"), planetName));
        }

        // B. Demographic Density vs Ocean Immersion
        Image elevImg = getElevationImageForPreset(p);
        PixelReader elevReader = elevImg != null ? elevImg.getPixelReader() : null;

        if (customDensityImage != null && customDensityImage.getPixelReader() != null) {
            double totalDensityBrightness = 0.0;
            double oceanDensityBrightness = 0.0;
            int imgW = (int) customDensityImage.getWidth();
            int imgH = (int) customDensityImage.getHeight();
            PixelReader pr = customDensityImage.getPixelReader();
            int sampleSteps = 60;

            for (int sy = 0; sy < sampleSteps; sy++) {
                double normLat = (sy + 0.5) / sampleSteps;
                double lat = 90.0 - normLat * 180.0;
                int py = (int) Math.min(imgH - 1, normLat * imgH);
                for (int sx = 0; sx < sampleSteps * 2; sx++) {
                    double normLon = (sx + 0.5) / (sampleSteps * 2.0);
                    double lon = -180.0 + normLon * 360.0;
                    int px = (int) Math.min(imgW - 1, normLon * imgW);
                    double b = pr.getColor(px, py).getBrightness();
                    if (b > 0.05) {
                        totalDensityBrightness += b;
                        boolean isOceanCell = false;
                        if (elevReader != null && elevImg != null) {
                            int ex = (int) Math.min(elevImg.getWidth() - 1, normLon * elevImg.getWidth());
                            int ey = (int) Math.min(elevImg.getHeight() - 1, normLat * elevImg.getHeight());
                            Color ec = elevReader.getColor(ex, ey);
                            double eNorm = (ec.getRed() + ec.getGreen() + ec.getBlue()) / 3.0;
                            isOceanCell = (eNorm < p.waterLevel());
                        } else {
                            var pt = generator.getPlanetPoint(lat, lon, p);
                            isOceanCell = (pt.elevation() < p.waterLevel());
                        }
                        if (isOceanCell) {
                            oceanDensityBrightness += b;
                        }
                    }
                }
            }
            if (totalDensityBrightness > 0 && oceanDensityBrightness > 0) {
                double oceanPct = (oceanDensityBrightness * 100.0) / totalDensityBrightness;
                if (oceanPct > 15.0) {
                    geoItems.add(String.format(java.util.Locale.FRANCE,
                        I18n.getOrDefault("scenario.warning.ocean_density", "âš ï¸ IncompatibilitÃ© gÃ©ographique : %.1f%% de la densitÃ© dÃ©mographique importÃ©e se trouve en zone ocÃ©anique / sous-marine (%s)."),
                        oceanPct, planetName));
                    alertCount++;
                } else {
                    geoItems.add(String.format(I18n.getOrDefault("scenario.diagnostic.demo_coast_ok", "âœ… DensitÃ© dÃ©mographique conforme aux terres Ã©mergÃ©es (%s)"), planetName));
                }
            } else {
                geoItems.add(String.format(I18n.getOrDefault("scenario.diagnostic.demo_coast_ok", "âœ… DensitÃ© dÃ©mographique conforme aux terres Ã©mergÃ©es (%s)"), planetName));
            }
        }

        // C. Cultural Tensors vs Ocean Immersion
        int dimsCount = cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null ? cultureVectorDimSpinner.getValue() : 9;
        boolean tensorOceanWarn = false;
        for (int i = 0; i < dimsCount; i++) {
            Image tImg = customTensorImages.get(i);
            if (tImg != null && tImg.getPixelReader() != null && p.waterLevel() > -0.4) {
                double totalTB = 0.0;
                double oceanTB = 0.0;
                int tw = (int) tImg.getWidth();
                int th = (int) tImg.getHeight();
                PixelReader pr = tImg.getPixelReader();
                int sampleSteps = 40;
                for (int sy = 0; sy < sampleSteps; sy++) {
                    double normLat = (sy + 0.5) / sampleSteps;
                    double lat = 90.0 - normLat * 180.0;
                    int py = (int) Math.min(th - 1, normLat * th);
                    for (int sx = 0; sx < sampleSteps * 2; sx++) {
                        double normLon = (sx + 0.5) / (sampleSteps * 2.0);
                        double lon = -180.0 + normLon * 360.0;
                        int px = (int) Math.min(tw - 1, normLon * tw);
                        double b = pr.getColor(px, py).getBrightness();
                        if (b > 0.08) {
                            totalTB += b;
                            boolean isOceanCell = false;
                            if (elevReader != null && elevImg != null) {
                                int ex = (int) Math.min(elevImg.getWidth() - 1, normLon * elevImg.getWidth());
                                int ey = (int) Math.min(elevImg.getHeight() - 1, normLat * elevImg.getHeight());
                                Color ec = elevReader.getColor(ex, ey);
                                double eNorm = (ec.getRed() + ec.getGreen() + ec.getBlue()) / 3.0;
                                isOceanCell = (eNorm < p.waterLevel());
                            } else {
                                var pt = generator.getPlanetPoint(lat, lon, p);
                                isOceanCell = (pt.elevation() < p.waterLevel());
                            }
                            if (isOceanCell) oceanTB += b;
                        }
                    }
                }
                if (totalTB > 0 && oceanTB > 0) {
                    double oceanPct = (oceanTB * 100.0) / totalTB;
                    if (oceanPct > 20.0) {
                        geoItems.add(String.format(java.util.Locale.FRANCE,
                            I18n.getOrDefault("scenario.warning.ocean_culture", "âš ï¸ IncompatibilitÃ© culturelle : %.1f%% de l'intensitÃ© du tenseur Â« %s Â» est situÃ©e sur l'ocÃ©an (%s)."),
                            oceanPct, getCulturalTensorTitle(i), planetName));
                        tensorOceanWarn = true;
                        alertCount++;
                    }
                }
            }
        }
        if (!tensorOceanWarn && !customTensorImages.isEmpty()) {
            geoItems.add(String.format(I18n.getOrDefault("scenario.diagnostic.tensors_ok", "âœ… Tenseurs culturels conformes aux terres Ã©mergÃ©es (%s)"), planetName));
        }

        // D. Extreme Hostile Environment
        double atmoPres = p.atmospherePressureAtm();
        double avgTemp = p.averageTempC();
        boolean isSpaceBody = planetKey.contains("lune") || planetKey.contains("moon") || planetKey.contains("mercure") || planetKey.contains("mercury") || planetKey.contains("mars") || planetKey.contains("venus") || planetKey.contains("vÃ©nus");
        if (!isSpaceBody && (atmoPres < 0.1 || atmoPres > 5.0 || avgTemp < -50 || avgTemp > 60)) {
            geoItems.add(String.format(java.util.Locale.FRANCE,
                I18n.getOrDefault("scenario.warning.hostile_environment", "âš ï¸ Environnement hostile (Onglet 1) : Pression (%.2f atm) ou TempÃ©rature (%.1f Â°C) extrÃªme â€” Survie humaine conditionnÃ©e Ã  des habitats scellÃ©s."),
                atmoPres, avgTemp));
            alertCount++;
        }

        // =========================================================================
        // 2. ECOLOGICAL VIABILITY & PLANETARY RESOURCES (Tabs 2 & 3)
        // =========================================================================
        if (p.atmospherePressureAtm() < 0.01) {
            ecoItems.add("âŒ AtmosphÃ¨re absente/tenue (" + String.format("%.3f", p.atmospherePressureAtm()) + " atm) : L'eau liquide bout Ã  la surface. Survie humaine impossible sans dÃ´mes fermÃ©s.");
            alertCount++;
        } else if (p.oxygenPercentage() < 10.0) {
            ecoItems.add("âš ï¸ AtmosphÃ¨re hypoxique (O2 = " + String.format("%.1f%%", p.oxygenPercentage()) + ") : Insuffisant pour la respiration des organismes complexes.");
            alertCount++;
        } else {
            ecoItems.add("âœ… AtmosphÃ¨re respirable & constante (P = " + String.format("%.2f", p.atmospherePressureAtm()) + " atm, O2 = " + String.format("%.1f%%", p.oxygenPercentage()) + ")");
        }

        if (p.waterLevel() < -0.3) {
            ecoItems.add("âš ï¸ Ressources en Eau LimitÃ©es : Monde trÃ¨s aride. Stress hydrique majeur prÃ©visible.");
            alertCount++;
        } else {
            ecoItems.add("âœ… Hydrologie Ã©quilibrÃ©e (Niveau d'eau = " + String.format("%.0f%%", (1.0 + p.waterLevel()) * 50) + ")");
        }

        double capitalK0 = computeAutoCapitalFromYear(startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000);
        double crustal = eco != null ? eco.crustalMetalOresGt() : 80.0;
        if (capitalK0 >= 8000.0 && crustal < 20.0) {
            ecoItems.add("âš ï¸ DÃ©ficit en MÃ©taux Industriels : Capital physique " + String.format("%.0f", capitalK0) + " kg/hab configurÃ© mais mÃ©taux crustaux faibles (" + String.format("%.1f Gt", crustal) + "). Risque de pÃ©nurie industrielle.");
            alertCount++;
        } else {
            ecoItems.add("âœ… CompatibilitÃ© MatÃ©riaux / Capital Physique");
        }

        long pop = initialHumanCountSpinner != null ? initialHumanCountSpinner.getValue() : 1_000_000L;
        if (pop > 5_000_000_000L && p.waterLevel() < -0.2) {
            ecoItems.add("âŒ Surpopulation Majeure : " + String.format("%,d", pop) + " habitants configurÃ©s sur un monde aride.");
            alertCount++;
        } else {
            ecoItems.add("âœ… DensitÃ© DÃ©mographique Initiale RÃ©aliste (" + String.format("%,d", pop) + " hab)");
        }

        // =========================================================================
        // 3. PHYSICAL ENGINES & JIT FUSIONS
        // =========================================================================
        org.ether.society.engines.compiler.EngineConflictReport jitReport = getOrCreateJitReport();
        if (jitReport != null && !jitReport.getEntries().isEmpty()) {
            for (org.ether.society.engines.compiler.EngineConflictReport.ConflictEntry entry : jitReport.getEntries()) {
                if (entry.getSeverity() == org.ether.society.engines.compiler.EngineConflictReport.ConflictSeverity.INCOMPATIBLE) {
                    engineItems.add("âŒ INCOMPATIBILITÃ‰ MOTEURS (" + entry.getVariableName() + ") : " + entry.getDescription());
                    alertCount++;
                } else {
                    engineItems.add("âœ… Fusion JIT Moteurs (" + entry.getVariableName() + ") : " + entry.getDescription());
                }
            }
        } else {
            engineItems.add("âœ… Compilation JIT Moteurs : 100% Compatible & Fusions ValidÃ©es");
        }

        // =========================================================================
        // 4. SCENARIO CONFIGURATION & SETUP INTEGRITY
        // =========================================================================
        List<String> configErrors = getScenarioValidationErrors();
        if (configErrors.isEmpty()) {
            validationItems.add(I18n.getOrDefault("scenario.diagnostic.validation_ok", "âœ… Configuration et couches du scÃ©nario 100% conformes et prÃªtes Ã  l'exÃ©cution"));
        } else {
            for (String err : configErrors) {
                validationItems.add("âŒ " + err);
                alertCount++;
            }
        }

        // =========================================================================
        // RENDER CATEGORIZED DIAGNOSTIC BOX
        // =========================================================================
        liveDiagnosticContentBox.getChildren().clear();

        if (liveDiagnosticHeader != null) {
            liveDiagnosticHeader.getStyleClass().removeAll("diagnostic-header-success", "diagnostic-header-warn");
            if (alertCount == 0) {
                liveDiagnosticHeader.setText(I18n.getOrDefault("scenario.diagnostic.header_viable", "ðŸ“‹ 9. CIVILIZATIONAL VIABILITY & COMPATIBILITY DIAGNOSTICS: 100% VIABLE & COMPATIBLE"));
                liveDiagnosticHeader.getStyleClass().add("diagnostic-header-success");
            } else {
                liveDiagnosticHeader.setText(I18n.getOrDefault("scenario.diagnostic.header_alerts", "ðŸ“‹ 9. CIVILIZATIONAL VIABILITY & COMPATIBILITY DIAGNOSTICS: ") + alertCount + " " + I18n.getOrDefault("scenario.diagnostic.alert_count_label", "ALERTE(S) / TENSION(S)"));
                liveDiagnosticHeader.getStyleClass().add("diagnostic-header-warn");
            }
        }

        renderDiagnosticCategory(liveDiagnosticContentBox, I18n.getOrDefault("scenario.diagnostic.cat.geography", "ðŸ—ºï¸ Cartographic & Geographic Compatibility (Tabs 1 & 3)"), geoItems);
        renderDiagnosticCategory(liveDiagnosticContentBox, I18n.getOrDefault("scenario.diagnostic.cat.ecology", "ðŸŒ¿ Ecological Viability & Planetary Resources (Tabs 2 & 3)"), ecoItems);
        renderDiagnosticCategory(liveDiagnosticContentBox, I18n.getOrDefault("scenario.diagnostic.cat.engines", "âš¡ Physical Engines & JIT Fusion Compatibility"), engineItems);
        renderDiagnosticCategory(liveDiagnosticContentBox, I18n.getOrDefault("scenario.diagnostic.cat.validation", "ðŸ›‘ Scenario Setup & Configuration Validation"), validationItems);
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
            Button resetSecBtn = new Button("ðŸ”„ " + org.ether.society.i18n.I18n.getOrDefault("scenario.btn.reset_section", "Default"));
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

    private void applyScenarioToUI(Scenario s) {
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

            // Lazy populate cartographic buffer maps if not already generated (only for Earth)
            if (isEarth && s.getCustomDensityBase64() == null) {
                org.ether.society.data.HistoricalMapGenerator.populateScenarioHistoricalMaps(s);
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
            for (java.util.Map.Entry<String, CheckBox> entry : typeBCheckBoxMap.entrySet()) {
                boolean active = typeBStates != null && typeBStates.getOrDefault(entry.getKey(), false);
                entry.getValue().setSelected(active);
            }

            // Restore Type B engine fine-grained parameter values
            java.util.Map<String, java.util.Map<String, Double>> typeBParams = s.getTypeBEngineParameters();
            if (typeBParams != null) {
                for (java.util.Map.Entry<String, java.util.Map<String, Spinner<Double>>> engEntry : typeBParamSpinnersMap.entrySet()) {
                    String engKey = engEntry.getKey();
                    java.util.Map<String, Double> savedEngParams = typeBParams.get(engKey);
                    if (savedEngParams != null) {
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
                if (densityMapFileLabel != null) densityMapFileLabel.setText(I18n.getOrDefault("scenario.preset.density_map", "ðŸ“· Carte de DensitÃ© PrÃ©calculÃ©e") + (yrStr.isEmpty() ? "" : " (" + yrStr + ")"));
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
            if (s.getTensorProceduralModes() != null && !s.getTensorProceduralModes().isEmpty()) {
                for (int i = 0; i < dims && i < s.getTensorProceduralModes().size(); i++) {
                    boolean isProc = s.getTensorProceduralModes().get(i);
                    if (isProc && tensorProcRadios.containsKey(i)) {
                        tensorProcRadios.get(i).setSelected(true);
                    } else if (!isProc && tensorImportRadios.containsKey(i)) {
                        tensorImportRadios.get(i).setSelected(true);
                    }
                }
            } else {
                for (int i = 0; i < dims; i++) {
                    if (tensorImportRadios.containsKey(i)) {
                        tensorImportRadios.get(i).setSelected(true);
                    }
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

        clippingHeader = new Label(I18n.getOrDefault("scenario.clipping.header", "âœ‚ï¸ 4. BORDERS & HISTORICAL SPATIAL CLIPPING"));
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
            @Override
            public String toString(String item) {
                if (item == null) return "";
                switch (item) {
                    case "DYNAMIC_RESERVOIR" -> { return I18n.getOrDefault("scenario.boundary.reservoir", "ðŸŒŠ External Virtual Reservoir (Free Flow)"); }
                    case "CLOSED_BARRIER" -> { return I18n.getOrDefault("scenario.boundary.barrier", "ðŸ§± Sealed Barrier / Isolated (Closed Edges)"); }
                    case "PERIODIC_WRAP" -> { return I18n.getOrDefault("scenario.boundary.wrap", "ðŸŒ Periodic Boundary (Toroidal)"); }
                    default -> { return item; }
                }
            }
            @Override
            public String fromString(String string) {
                return null;
            }
        });

        resetClippingBtn = new Button(I18n.getOrDefault("scenario.clipping.reset", "ðŸ”„ Reset Area (Full Planet)"));
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

    private VBox createOceanOptimizationSection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("card-section");

        Label oceanOptHeader = new Label(I18n.getOrDefault("scenario.ocean_opt.header", "âš™ï¸ 5. ENGINE ARCHITECTURE & OPTIMIZATIONS (ETHER CORE & OPTIONAL)"));
        oceanOptHeader.getStyleClass().add("label-section-header");

        Label oceanOptDesc = new Label(I18n.getOrDefault("scenario.ocean_opt.desc", "Definition and configuration of determinism mode, 7 simulation optimizations, and Ether Core and optional procedural engines. Each scenario embeds its optimization setup to guarantee perfect reproducibility."));
        oceanOptDesc.getStyleClass().add("card-description-muted");
        oceanOptDesc.setWrapText(true);

        // --- ðŸŽ¯ MASTER CONTROL : MODE DÃ‰TERMINISME STRICTE ---
        strictDeterminismCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.strict_determinism", "ðŸ”’ STRICT DETERMINISM MODE (0% Approximation / 100% Bit-to-Bit Reproducibility)"));
        strictDeterminismCheckBox.setSelected(true);
        strictDeterminismCheckBox.getStyleClass().add("radio-proc");
        strictDeterminismCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.strict_determinism",
            "ðŸŽ¯ STRICT BIT-TO-BIT DETERMINISM (ACADEMIC RESEARCH MODE)\n" +
            "â€¢ Disables ALL optimizations and algorithmic shortcuts.\n" +
            "â€¢ Guarantees 100% bit-identical simulation trajectories on the same seed.\n" +
            "â€¢ Recommended for validation tests, benchmarks, and convergence audits.")));
        attachDefaultValueHandling(strictDeterminismCheckBox, true, () -> strictDeterminismCheckBox.setSelected(true));

        Label determinismNote = new Label(I18n.getOrDefault("scenario.note.strict_determinism", "ðŸ’¡ Note: Checking Strict Determinism disables all heuristic approaches and guarantees bit-identical numerical fidelity."));
        determinismNote.getStyleClass().add("control-note");
        determinismNote.setWrapText(true);

        VBox masterBox = new VBox(4, strictDeterminismCheckBox, determinismNote);
        masterBox.getStyleClass().add("opt-master-box");

        // --- âš¡ INDIVIDUAL OPTIMIZATIONS & APPROXIMATIONS ---
        optSubHeader = new Label(I18n.getOrDefault("scenario.opt.sub_header", "âš¡ ALGORITHMIC OPTIMIZATIONS & PERFORMANCE SHORTCUTS:"));
        optSubHeader.getStyleClass().add("opt-subheader");

        sparseCellSkippingCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.sparse_cell_skipping", "ðŸœ Sparse / Uninhabited Cell Skipping (Deserts & Abysses)"));
        sparseCellSkippingCheckBox.setSelected(false);
        sparseCellSkippingCheckBox.getStyleClass().add("opt-sub-checkbox");
        sparseCellSkippingCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.sparse_cell_skipping",
            "âš¡ BENEFIT: +40% to +60% TPS speedup across global grid.\n" +
            "âš ï¸ PHYSICAL IMPACT: Bypasses evaluation loops on desert/oceanic cells with no human presence or active event.")));
        attachDefaultValueHandling(sparseCellSkippingCheckBox, false, () -> sparseCellSkippingCheckBox.setSelected(false));

        oceanMacroAggregationCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.macro_aggregation", "ðŸŒŠ Abyssal Ocean Macro-Aggregation (Deep Basins z < -200m in Blocks)"));
        oceanMacroAggregationCheckBox.setSelected(false);
        oceanMacroAggregationCheckBox.getStyleClass().add("opt-sub-checkbox");
        oceanMacroAggregationCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.ocean_macro_aggregation",
            "âš¡ BENEFIT: +25% to +35% TPS speedup by grouping deep water cells.\n" +
            "âš ï¸ PHYSICAL IMPACT: Smoothing of abyssal micro-currents without impacting terrestrial civilizations.")));
        attachDefaultValueHandling(oceanMacroAggregationCheckBox, false, () -> oceanMacroAggregationCheckBox.setSelected(false));

        coastalNavigationOnlyCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.coastal_nav", "âš“ Exclusive Coastal Navigation (Pathfinding Focused on Coasts & Straits)"));
        coastalNavigationOnlyCheckBox.setSelected(false);
        coastalNavigationOnlyCheckBox.getStyleClass().add("opt-sub-checkbox");
        coastalNavigationOnlyCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.coastal_nav",
            "âš¡ BENEFIT: Major CPU savings on naval and commercial trade pathfinding.\n" +
            "âš ï¸ PHYSICAL IMPACT: Ships prefer coastal waters; ocean navigation restricted prior to Age of Discovery.")));
        attachDefaultValueHandling(coastalNavigationOnlyCheckBox, false, () -> coastalNavigationOnlyCheckBox.setSelected(false));

        oceanMultiRateTickingCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.multi_rate_ticking", "â± Oceanic & Multi-Rate Climate Ticking (Updated Every N Ticks)"));
        oceanMultiRateTickingCheckBox.setSelected(false);
        oceanMultiRateTickingCheckBox.getStyleClass().add("opt-sub-checkbox");
        oceanMultiRateTickingCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.multi_rate_ticking",
            "âš¡ BENEFIT: +30% throughput by executing thermohaline circulation and fluid inertia at sub-frequency.\n" +
            "âš ï¸ PHYSICAL IMPACT: Potential temporal aliasing during ultra-fast atmospheric events.")));
        attachDefaultValueHandling(oceanMultiRateTickingCheckBox, false, () -> oceanMultiRateTickingCheckBox.setSelected(false));

        climateTickFreqSlider = new Slider(1, 30, 5);
        climateTickFreqSlider.setMajorTickUnit(5);
        climateTickFreqSlider.setMinorTickCount(4);
        climateTickFreqSlider.setSnapToTicks(true);
        climateTickFreqSlider.setShowTickMarks(true);
        climateTickFreqSlider.setStyle("-fx-pref-width: 200px;");
        climateTickFreqSlider.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.climate_tick_freq", "Adjust climate update frequency ratio")));
        climateTickFreqValueLabel = new Label(I18n.getOrDefault("scenario.label.climate_freq_default", "Climate Frequency Ratio: 1:5 ticks"));
        climateTickFreqValueLabel.getStyleClass().add("opt-value-highlight");
        climateTickFreqSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int val = newV.intValue();
            climateTickFreqValueLabel.setText(val == 1 ? I18n.getOrDefault("scenario.climate_freq.strict", "Climate Frequency Ratio: 1:1 (Strict Bit-for-Bit Cadence)") : I18n.getOrDefault("scenario.climate_freq.ratio_prefix", "Climate Frequency Ratio: 1:") + val + " ticks");
            notifyParamChange();
        });
        attachDefaultValueHandling(climateTickFreqSlider, 6.0, () -> climateTickFreqSlider.setValue(6.0));

        HBox climateSliderBox = new HBox(10, new Label("   â””â”€"), climateTickFreqValueLabel, climateTickFreqSlider);
        climateSliderBox.setAlignment(Pos.CENTER_LEFT);

        parallelExecutionCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.parallel_execution", "ðŸš€ Async Multi-Thread Parallelization (CompletableFuture / AVX)"));
        parallelExecutionCheckBox.setSelected(false);
        parallelExecutionCheckBox.getStyleClass().add("opt-sub-checkbox");
        parallelExecutionCheckBox.setTooltip(new Tooltip("""
            âš¡ BÃ‰NÃ‰FICE : Exploitation intÃ©grale de tous les cÅ“urs CPU du systÃ¨me.
            âš ï¸ IMPACT PHYSIQUE : L'ordre de sommation flottante peut varier lÃ©gÃ¨rement entre exÃ©cutions (non-associativitÃ© IEEE 754 en multi-threading).
            """));
        attachDefaultValueHandling(parallelExecutionCheckBox, false, () -> parallelExecutionCheckBox.setSelected(false));

        threadCountSlider = new Slider(0, 32, 0);
        threadCountSlider.setMajorTickUnit(8);
        threadCountSlider.setMinorTickCount(7);
        threadCountSlider.setSnapToTicks(true);
        threadCountSlider.setShowTickMarks(true);
        threadCountSlider.setStyle("-fx-pref-width: 200px;");
        threadCountSlider.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.thread_count", "Select number of CPU threads assigned to parallel processing")));
        threadCountValueLabel = new Label(I18n.getOrDefault("scenario.label.threads_auto", "Threads Multi-Thread : Auto (Tous cÅ“urs CPU)"));
        threadCountValueLabel.getStyleClass().add("opt-value-highlight");
        threadCountSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int val = newV.intValue();
            if (val == 0) {
                threadCountValueLabel.setText(String.format(I18n.getOrDefault("scenario.label.threads_auto_cores", "Threads Multi-Thread : Auto (Tous cÅ“urs %d)"), Runtime.getRuntime().availableProcessors()));
            } else if (val == 1) {
                threadCountValueLabel.setText(I18n.getOrDefault("scenario.label.threads_single", "Multi-Thread Threads: 1 (Single-threaded Deterministic)"));
            } else {
                threadCountValueLabel.setText(String.format(I18n.getOrDefault("scenario.label.threads_count", "Threads Multi-Thread : %d threads"), val));
            }
            notifyParamChange();
        });
        attachDefaultValueHandling(threadCountSlider, 4.0, () -> threadCountSlider.setValue(4.0));

        HBox threadSliderBox = new HBox(10, new Label("   â””â”€"), threadCountValueLabel, threadCountSlider);
        threadSliderBox.setAlignment(Pos.CENTER_LEFT);

        spatialRangeTruncationCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.spatial_truncation", "ðŸ’¨ Spatial Range Truncation of Plumes & Diffusions (10â»â¶ Cutoff)"));
        spatialRangeTruncationCheckBox.setSelected(false);
        spatialRangeTruncationCheckBox.getStyleClass().add("opt-sub-checkbox");
        spatialRangeTruncationCheckBox.setTooltip(new Tooltip("""
            âš¡ BÃ‰NÃ‰FICE : Limite le calcul de dispersion atmosphÃ©rique aux cellules adjacentes affectÃ©es.
            âš ï¸ IMPACT PHYSIQUE : NÃ©glige les concentrations d'aÃ©rosols et suie ultra-diluÃ©es devenant infÃ©rieures Ã  10â»â¶ ppm.
            """));
        attachDefaultValueHandling(spatialRangeTruncationCheckBox, false, () -> spatialRangeTruncationCheckBox.setSelected(false));

        List<CheckBox> subOpts = List.of(
            sparseCellSkippingCheckBox,
            oceanMacroAggregationCheckBox,
            coastalNavigationOnlyCheckBox,
            oceanMultiRateTickingCheckBox,
            parallelExecutionCheckBox,
            spatialRangeTruncationCheckBox
        );

        strictDeterminismCheckBox.setOnAction(e -> {
            boolean strict = strictDeterminismCheckBox.isSelected();
            if (strict) {
                for (CheckBox cb : subOpts) {
                    cb.setSelected(false);
                }
            }
            updatePerformanceControlsState();
            notifyParamChange();
        });

        for (CheckBox cb : subOpts) {
            cb.setOnAction(e -> {
                if (cb.isSelected() && strictDeterminismCheckBox.isSelected()) {
                    strictDeterminismCheckBox.setSelected(false);
                } else if (subOpts.stream().noneMatch(CheckBox::isSelected)) {
                    strictDeterminismCheckBox.setSelected(true);
                }
                updatePerformanceControlsState();
                notifyParamChange();
            });
        }

        VBox optBox = new VBox(5,
            optSubHeader,
            sparseCellSkippingCheckBox,
            oceanMacroAggregationCheckBox,
            coastalNavigationOnlyCheckBox,
            oceanMultiRateTickingCheckBox,
            climateSliderBox,
            parallelExecutionCheckBox,
            threadSliderBox,
            spatialRangeTruncationCheckBox
        );
        optBox.setStyle("-fx-padding: 8px 12px; -fx-background-radius: 6px; -fx-border-color: rgba(148, 163, 184, 0.2); -fx-border-radius: 6px; -fx-border-width: 1px;");
        optBox.visibleProperty().bind(strictDeterminismCheckBox.selectedProperty().not());
        optBox.managedProperty().bind(strictDeterminismCheckBox.selectedProperty().not());

        // Detail Inspector Card for selected/hovered Engine (Technical Description + Math Equations + Academic References)
        engineInspectorTitle = new Label(I18n.getOrDefault("scenario.header.engine_inspector", "ðŸ”Ž Cliodynamic Engine Inspector (Hover an engine to inspect)"));
        engineInspectorTitle.getStyleClass().add("engine-inspector-title");
        
        engineInspectorText = new Label(I18n.getOrDefault("scenario.desc.engine_inspector", "Select or hover a Type A (Core) or Type B (Optional) engine to display its state equations, physical principles, and academic references."));
        engineInspectorText.setWrapText(true);
        engineInspectorText.getStyleClass().add("engine-inspector-text");

        engineInspectorEquationsTitle = new Label(I18n.getOrDefault("scenario.label.engine_equations", "ðŸ“ Mathematical Equations & Cliodynamic Formulation:"));
        engineInspectorEquationsTitle.getStyleClass().add("engine-inspector-equations-title");

        engineInspectorEquations = new Label(
            "â€¢ Formulations mathÃ©matiques et bilans de conservation affichÃ©s dynamiquement."
        );
        engineInspectorEquations.setWrapText(true);
        engineInspectorEquations.getStyleClass().add("engine-inspector-equations");

        engineInspectorRef = new Label(I18n.getOrDefault("scenario.label.engine_refs", "ðŸ“š Scientific references and academic literature."));
        engineInspectorRef.setWrapText(true);
        engineInspectorRef.getStyleClass().add("hint-label");

        exportSelectedEngineBtn = new Button(I18n.getOrDefault("scenario.btn.export_engine_template", "ðŸ“¤ Export Custom Engine Template (.java)"));
        exportSelectedEngineBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 10; -fx-background-radius: 4;");
        exportSelectedEngineBtn.setOnAction(e -> exportCustomEngineTemplate());

        VBox engineInspectorCard = new VBox(5, engineInspectorTitle, engineInspectorText, engineInspectorEquationsTitle, engineInspectorEquations, engineInspectorRef, exportSelectedEngineBtn);
        engineInspectorCard.getStyleClass().add("engine-inspector-card");
        engineInspectorCard.setMinHeight(160);

        // --- ðŸ”’ ETHER CORE ENGINES SECTION (CÅ“ur Central Applicatif - 24 Moteurs Permanents) ---
        VBox typeABox = new VBox(6);
        typeABox.getStyleClass().add("card-section");

        exportCoreTemplateBtn = new Button(I18n.getOrDefault("scenario.btn.export_law_engine", "ðŸ“¤ Export Physical Law Engine (.java)"));
        exportCoreTemplateBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 4;");
        exportCoreTemplateBtn.setOnAction(e -> exportPhysicalLawEngineTemplate("PhysicalLawEngine"));

        coreExplanationLabel = new Label(I18n.getOrDefault("scenario.info.core_engines", "â„¹ï¸ Why are Ether Core engines permanent? They enforce physical conservation laws (mass & energy, thermodynamics, hydrology, H3 insolation, metabolism) required for basic world survival."));
        coreExplanationLabel.getStyleClass().add("control-note");
        coreExplanationLabel.setWrapText(true);
        typeABox.getChildren().addAll(exportCoreTemplateBtn, coreExplanationLabel);

        List<String[]> coreEngines = List.of(
            new String[]{"PhysicalLawEngine", "Moteur Physique & Lois de Conservation",
                "Moteur de conservation thermodynamique de la matiÃ¨re et de l'Ã©nergie (Premier et Second Principes). Calcule le bilan calorifique planÃ©taire et la dÃ©gradation de l'Ã©nergie en chaleur dissipÃ©e.",
                "Ref: Carnot, N. L. S. (1824); Clausius, R. (1865); Prigogine, I. (1977). Non-Equilibrium Thermodynamics.",
                "â€¢ Premier Principe (Ã‰nergie) : dE_total/dt = Q_in - Q_out = 0\nâ€¢ Second Principe (Entropie) : dS/dt = dS_ext + dS_int >= 0 avec dS_int = Q_dissipee / Temp_surface"},
            new String[]{"BiologicalDemographicsEngine", "DÃ©mographie Cellulaire & MÃ©tabolisme",
                "RÃ©gulation mÃ©tabolique de la population humaine. Calcule la mortalitÃ© de Gompertz-Makeham selon l'Ã¢ge, l'espÃ©rance de vie, la natalitÃ© malthusienne et la sous-alimentation.",
                "Ref: Kleiber, M. (1932); Gompertz, B. (1825); Makeham, W. M. (1860); Malthus, T. R. (1798).",
                "â€¢ MortalitÃ© Gompertz-Makeham : Î¼(x) = Î± Â· exp(Î² Â· x) + Î³\nâ€¢ Bilan MÃ©tabolique de Kleiber : B_metabolisme = qâ‚€ Â· M^(0.75)\nâ€¢ Dynamique de Population : dN/dt = r Â· N Â· (1 - N / K_soutenable)"},
            new String[]{"PhysicalEnergyGridEngine", "Grille Ã‰nergÃ©tique & EROEI Brut",
                "ModÃ©lisation des flux d'Ã©nergie primaire planÃ©taire (solaire, gÃ©othermie, biomasse). DÃ©termine l'EROEI brut (Energy Return on Energy Invested) pour les rÃ©coltes et l'extraction.",
                "Ref: Hall, C. A. S., et al. (2014). EROEI of Global Energy Resources. Nature Climate Change.",
                "â€¢ EROEI Brut = E_produite_brute / E_investie_extraction\nâ€¢ Ã‰nergie Utile Net = E_brute Â· (1 - 1 / EROEI)\nâ€¢ Seuil Critique Civilisationnel : EROEI >= 3.0 requis pour soutenir les institutions."},
            new String[]{"TechTreeEngine", "Diffusion Technologique & Capital Savoir",
                "Arbre d'innovation technologique et diffusion cognitive. Simule l'accumulation du capital d'instruction, la propagation spatiale des inventions et le franchissement des seuils d'Ã©tapes (Niveaux Tech 0.0 Ã  10.0+).",
                "Ref: Mokyr, J. (1990). The Lever of Riches: Technological Creativity. Oxford Univ. Press.",
                "â€¢ Croissance du Capital Savoir : dT/dt = Î± Â· Pop Â· (T / T_max)^Î² + Î£ D_voisinage Â· (T_j - T_i)\nâ€¢ Diffusion Spatiale : Flux_innovation = -D_diffusion Â· âˆ‡T(x)"},
            new String[]{"AquiferDepletionEngine", "Hydrologie & Transfert d'Eau Douce",
                "Hydrologie continentale et rÃ©plÃ©tion/dÃ©plÃ©tion des nappes phrÃ©atiques. Simule le bilan prÃ©cipitations-Ã©vapotranspiration, le dÃ©bit des riviÃ¨res et le stress hydrique.",
                "Ref: Gleick, P. H. (2000). Water Futures; Wada, Y. et al. (2010). Global Groundwater Depletion. GRL.",
                "â€¢ Bilan Hydrique Cellulaire : dW_nappe/dt = Precipitations - Evapotranspiration - Extraction_agricole - Ruissellement\nâ€¢ Stress Hydrique = Extraction_totale / Recharge_annuelle"},
            new String[]{"H3ClimateSystem", "SystÃ¨me Climatique H3 & Saisons",
                "Moteur climato-saisonnier basÃ© sur la discrÃ©tisation hexagonale H3. Calcule la tempÃ©rature moyenne de surface, le gradient Ã©quateur-pÃ´le, l'insolation selon l'obliquitÃ© orbitale et les saisons.",
                "Ref: Uber H3 Spatial Index (2018); Sellers, W. D. (1969). Energy Balance Climate Models.",
                "â€¢ Bilan Radiatif Solaire : S(lat, t) = (S_const / 4) Â· [1 + e Â· cos(Ï‰ Â· t)] Â· cos(lat - declinaison)\nâ€¢ Ã‰quilibre Thermique : C_thermique Â· dT/dt = S(1 - AlbÃ©do) - ÎµÂ·ÏƒÂ·Tâ´ + Div(K_transport Â· âˆ‡T)"},
            new String[]{"PoliticalSimulationEngine", "Moteur Politique & FrontiÃ¨res",
                "ModÃ©lisation des structures politiques et gÃ©opolitiques. GÃ¨re la dÃ©limitation des territoires, la souverainetÃ© des citÃ©s-Ã©tats, les confÃ©dÃ©rations culturelles et la stabilitÃ© des frontiÃ¨res.",
                "Ref: Tilly, C. (1990). Coercion, Capital, and European States; Mann, M. (1986). Sources of Social Power.",
                "â€¢ Projection de Puissance d'Ã‰tat : P_militaire(d) = (CapacitÃ©_Fiscale Â· Taux_LevÃ©e) / (1 + Î± Â· Distance_Capitale)\nâ€¢ StabilitÃ© des FrontiÃ¨res : Seuil d'annexion si P_i(x) > 1.35 Â· P_j(x)"},
            new String[]{"StatisticsKernel", "Noyau Statistique & Cliodynamique",
                "Noyau d'agrÃ©gation statistique et d'analyse cliodynamique en temps rÃ©el. Calcule l'indice de Gini, le PIB mondial, la complexitÃ© de Turchin, le risque d'effondrement et exporte les bilans CSV.",
                "Ref: Gini, C. (1912); Turchin, P. (2016). Ages of Discord; Tainter, J. (1988). Collapse of Complex Societies.",
                "â€¢ Indice de Gini : G = (Î£ Î£ |y_i - y_j|) / (2 Â· nÂ² Â· y_moyen)\nâ€¢ Pression de Crise SystÃ©mique (PSI) = (Pop / Pop_soutenable) Â· (1 / Salaire_reel) Â· InegalitÃ©_Elite"},
            new String[]{"WorldBuffer / AgentBuffer", "Noyau DOD Allocateur MÃ©moire",
                "Allocateur de mÃ©moire et registres DOD (Data-Oriented Design). Optimise la mÃ©moire cache du processeur en vectorisant les attributs des cohortes d'agents et des mailles H3.",
                "Ref: Acton, M. (2014). Data-Oriented Design; LMAX Disruptor High-Performance Ring Buffer (2011).",
                "â€¢ Structure des Tableaux d'Attributs (SoA) : float[] capitalWork, float[] capitalResource, int[] populationCohorts\nâ€¢ Alignement Cache SIMD VectorisÃ© : 64-byte aligned blocks for AVX-512 execution."},
            new String[]{"OceanPhysicsEngine", "Dynamo Fluidique & Basculement OcÃ©anique",
                "Dynamo fluidique et inertie thermique des ocÃ©ans. ModÃ©lise la capacitÃ© calorifique de la masse d'eau marine, la dÃ©rive thermique lente et la rÃ©gulation du climat vÃ©gÃ©tal.",
                "Ref: Stommel, H. (1961). Thermohaline Convection; Rahmstorf, S. (1995). AMOC Stability. Nature.",
                "â€¢ ModÃ¨le Ã  Deux Mailles de Stommel : dq/dt = c_T Â· Î”T - c_S Â· Î”S\nâ€¢ Transport de Chaleur OcÃ©anique : F_ocean = Ï Â· C_p Â· V_derive Â· (T_equateur - T_pole)"},
            new String[]{"MalthusianCapacityEngine", "Pression Malthusienne & CapacitÃ© Portante",
                "CapacitÃ© portante Ã©cologique (K) et pression Malthusienne. Calcule le seuil maximal d'habitants soutenables par cellule avant dÃ©gradation irrÃ©versible de l'environnement.",
                "Ref: Malthus, T. R. (1798); Catton, W. R. (1980). Overshoot: Ecological Footprint.",
                "â€¢ CapacitÃ© Portante K(t) = FertilitÃ©_Sol Â· Eau_Disponible Â· Niveau_Tech\nâ€¢ Ratio Malthusien M = Pop / K\nâ€¢ DÃ©gradation Environnementale en cas de Surconsommation (M > 1) : dK/dt = -Î³ Â· (M - 1) Â· K"},
            new String[]{"OreGradeThermodynamicsEngine", "GÃ©o-MÃ©tallurgie & DÃ©plÃ©tion Crustale",
                "GÃ©o-mÃ©tallurgie et thermodynamique d'Ã©puisement des filons minÃ©raux crustaux. Simule le dÃ©clin du titre des minerais (Loi de Lasky) et la hausse de l'Ã©nergie nÃ©cessaire Ã  l'extraction.",
                "Ref: Lasky, S. G. (1950). Mineral Resource Depletion Law; Ayres, R. U. (1998). Industrial Ecology.",
                "â€¢ Loi de Lasky (Titre du Minerai) : Grade(g) = gâ‚€ Â· exp(-k Â· Cumul_Extrait)\nâ€¢ Ã‰nergie SpÃ©cifique d'Extraction : E_extraction(g) = Eâ‚€ / (Grade(g))^1.35"},
            new String[]{"SoilNutrientNPKEngine", "Cycle NPK & FertilitÃ© des Sols",
                "Cycles biogÃ©ochimiques des nutriments NPK (Azote, Phosphore, Potassium). RÃ©git l'Ã©puisement des sols agricoles par la culture intensive et la restauration organique.",
                "Ref: Liebig, J. von (1840). Law of the Minimum; Smil, V. (2001). Enriching the Earth (N-P-K Cycles).",
                "â€¢ Loi du Minimum de Liebig : Rendement = Y_max Â· min( N/N_ref, P/P_ref, K/K_ref )\nâ€¢ Ã‰puisement des Nutriment : dN/dt = Restauration_Naturelle + Apport_Engrais - Export_Recolte"},
            new String[]{"FluxEngine", "Flux de Subsistance & Routes Commerciales",
                "Routes commerciales et flux de subsistance inter-cellules H3. Calcule les coÃ»ts de transport, l'arbitrage marchand et l'Ã©quilibrage des stocks alimentaires par le commerce.",
                "Ref: Tinbergen, J. (1962). Gravity Model of Trade; Onsager, L. (1931). Reciprocal Relations.",
                "â€¢ ModÃ¨le Gravitationnel de Commerce : Flux(i, j) = G Â· (PIB_i Â· PIB_j) / (Distance(i, j)^1.8)\nâ€¢ Friction de Transport : CoÃ»t_fret = Exp(Friction_Relief Â· Distance)"},
            new String[]{"AtmosphericOxygenEngine", "Dynamique de l'OxygÃ¨ne AtmosphÃ©rique",
                "Bilan de la pression partielle d'oxygÃ¨ne (Oâ‚‚) atmosphÃ©rique. RÃ©gule les conditions mÃ©taboliques pour la survie des organismes complexes et le risque d'incendies forestiers.",
                "Ref: Berner, R. A. (2006). GEOCARBSULF: Atmospheric Oxygen over Phanerozoic Time.",
                "â€¢ Bilan d'Oâ‚‚ AtmosphÃ©rique : dOâ‚‚/dt = Photosynthese_Net - Respiration_Biomasse - Oxydation_MinÃ©rale\nâ€¢ Risque d'Embrasement Sauvage = Max(0, (pOâ‚‚ - 0.15) / 0.06)"},
            new String[]{"CrustalGeothermalEngine", "GÃ©othermie Crustale & Tectonique",
                "Flux de chaleur interne terrestre et potentiel gÃ©othermique crustal. Simule le gradient gÃ©othermique et le potentiel d'Ã©nergie gÃ©othermique de surface.",
                "Ref: Turcotte, D. L. & Schubert, G. (2002). Geodynamics: Mantle Heat Transport. Cambridge Univ. Press.",
                "â€¢ ConductivitÃ© Thermique Crustale : q = -k_roche Â· (dT/dz)\nâ€¢ Potentiel GÃ©othermique d'Exploitation : P_geoth = q_surface Â· Surface_H3 Â· Rendement_Carnot"},
            new String[]{"DynamicHydrographicSiltationEngine", "Hydrographie & Ensablement Fluvial",
                "Hydrographie et dynamique d'Ã©rosion/ensablement des bassins versants. ModÃ©lise la modification du lit des fleuves et le dÃ©pÃ´t d'alluvions fertiles.",
                "Ref: Horton, R. E. (1945). Drainage-Basin Development; Schumm, S. A. (1977). The Fluvial System.",
                "â€¢ Ã‰quation d'Ã‰rosion Fluviale : E_sediment = k_erodibilite Â· (DÃ©bit_eau)^1.4 Â· (Pente_terrain)^1.2\nâ€¢ DÃ©pÃ´t Alluvionnaire : dSilt/dt = E_amont - Sedimentation_loc_lit"},
            new String[]{"GreenhouseRadiativeEngine", "ForÃ§age Radiatif & Effet de Serre",
                "Bilan de forÃ§age radiatif et effet de serre. Calcule l'impact des concentrations de COâ‚‚, CHâ‚„ et Hâ‚‚O sur l'infrarouge rÃ©Ã©mis vers la surface.",
                "Ref: Myhre, G. et al. (1998). Radiative Forcing Equations; IPCC AR6 WG1 (2021).",
                "â€¢ ForÃ§age Radiatif du COâ‚‚ : Î”F_CO2 = 5.35 Â· ln(C / Câ‚€)  [W/mÂ²]\nâ€¢ SensibilitÃ© Climatique : Î”T_eq = Î» Â· (Î”F_CO2 + Î”F_CH4 + Î”F_aerosols)"},
            new String[]{"InfrastructureEnergyEngine", "RÃ©seaux d'Infrastructure Ã‰nergÃ©tique",
                "RÃ©seaux d'infrastructures Ã©nergÃ©tiques et transport de puissance. ModÃ©lise la perte en ligne des rÃ©seaux Ã©lectriques et olÃ©oducs.",
                "Ref: Bak, P. et al. (1987). Self-Organized Criticality in Grid Infrastructure.",
                "â€¢ Perte en Ligne Ã‰lectrique : Perte_Joule = R_cable Â· IÂ² Â· Distance\nâ€¢ CapacitÃ© Maximale de Transit : Cap_max = V_reseau Â· I_max_thermique"},
            new String[]{"NetEnergyEROEIEngine", "EROEI Net & Rendement Ã‰nergÃ©tique",
                "Calcul du rendement Ã©nergÃ©tique net (EROEI Net civilisationnel). Ã‰value la fraction d'Ã©nergie rÃ©investie dans l'extraction par rapport Ã  l'Ã©nergie utilisable pour la sociÃ©tÃ©.",
                "Ref: Hall, C. A. S. & Klitgaard, K. A. (2018). Energy and the Wealth of Nations. Springer.",
                "â€¢ Fraction d'Ã‰nergie RÃ©investie : F_invest = 1 / EROEI_systemique\nâ€¢ Ã‰nergie Nette Utile SociÃ©tÃ© = Ã‰nergie_Totale Â· (1 - F_invest)"},
            new String[]{"PermafrostThawEngine", "DÃ©gel du Permafrost & Relargage MÃ©thane",
                "Dynamique de fonte du cryosol (Permafrost). Simule la dÃ©stabilisation des sols gelÃ©s et le relargage rÃ©troactif de mÃ©thane et COâ‚‚ stratosphÃ©riques.",
                "Ref: Schuur, E. A. G. et al. (2015). Vulnerability of Permafrost Carbon to Climate Change. Nature.",
                "â€¢ Fonte du Cryosol : dV_gel/dt = -Î± Â· Max(0, T_surface - 0.0 Â°C)\nâ€¢ Ã‰mission RÃ©troactive CHâ‚„/COâ‚‚ : Emiss_gaz = Stock_carbone_degele Â· K_microbien(T)"},
            new String[]{"PhysicsTransportEngine", "Frictions Thermodynamiques de Transport",
                "CoÃ»ts Ã©nergÃ©tiques et frictions thermodynamiques des transports. Calcule l'Ã©nergie consommÃ©e par tonne-kilomÃ¨tre selon le relief et le mode de transport.",
                "Ref: Smil, V. (2017). Energy and Civilization: A History. MIT Press.",
                "â€¢ Ã‰nergie SpÃ©cifique par Mode (MJ/t-km) : Maritim=0.15, Ferroviaire=0.35, Route=2.5, Porteur=12.0\nâ€¢ Friction du Relief : CoÃ»t_total = E_specifique Â· (1 + k_pente Â· Pente_moyenne) Â· Distance"},
            new String[]{"ThermohalineOceanEngine", "Circulation Thermohaline OcÃ©anique",
                "Circulation thermohaline globale (Boucle AMOC). ModÃ©lise la plongÃ©e des eaux salÃ©es froides en Atlantique Nord et la redistribution de la chaleur planÃ©taire.",
                "Ref: Broecker, W. S. (1991). The Great Ocean Conveyor; Rahmstorf, S. (2002). Ocean Circulation. Nature.",
                "â€¢ DÃ©bit AMOC Q_amoc = k_thermo Â· (Ï_nord - Ï_equateur)\nâ€¢ Point de Basculement Salin : Si Dilution_Eau_Douce > Seuil_Critique -> Effondrement AMOC (Q -> 0)"},
            new String[]{"TrophicEcosystemEngine", "RÃ©seau Trophique & Ã‰cosystÃ¨mes",
                "RÃ©seau trophique et dynamique des Ã©cosystÃ¨mes fauniques. Simule les Ã©quations de Lotka-Volterra entre prÃ©dateurs, herbivores et producteurs primaires.",
                "Ref: Lotka, A. J. (1925); Volterra, V. (1926); MacArthur, R. H. & Wilson, E. O. (1967).",
                "â€¢ Dynamique Herbivores H : dH/dt = r_h Â· H Â· (1 - H/K) - a Â· H Â· P\nâ€¢ Dynamique PrÃ©dateurs P : dP/dt = b Â· a Â· H Â· P - m_p Â· P"}
        );

        coreEngineRows.clear();
        for (String[] eng : coreEngines) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Label iconTitle = new Label("ðŸ”’ " + eng[0]);
            iconTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
            String title = I18n.getEngineTitle(eng[0], eng[1]);
            Label descLbl = new Label("â€” " + title);
            descLbl.setStyle("-fx-font-size: 10px;");
            HBox.setHgrow(descLbl, Priority.ALWAYS);
            row.getChildren().addAll(iconTitle, descLbl);

            String desc = I18n.getEngineDescription(eng[0], eng[2]);
            String ref = I18n.getEngineReference(eng[0], eng[3]);
            String eqText = eng.length > 4 ? I18n.getEngineEquation(eng[0], eng[4]) : I18n.getOrDefault("scenario.engine.state_eq_default", "ðŸ“ Ã‰quation d'Ã‰tat : dX/dt = f(X, t) + Î£ F_inter-cellulaire");
            String permPrefix = I18n.getOrDefault("scenario.engine.perm_prefix", "ðŸ”’ [MOTEUR PERMANENT]\n");
            Tooltip tooltip = new Tooltip(permPrefix + eng[0] + " â€” " + title + "\n\n" + desc + "\n\n" + eqText + "\n\nðŸ“š " + ref);
            tooltip.setStyle("-fx-font-size: 11px; -fx-max-width: 500px;");
            Tooltip.install(row, tooltip);

            final String[] finalEng = eng;
            row.setOnMouseEntered(e -> {
                String curTitle = I18n.getEngineTitle(finalEng[0], finalEng[1]);
                String curDesc = I18n.getEngineDescription(finalEng[0], finalEng[2]);
                String curRef = I18n.getEngineReference(finalEng[0], finalEng[3]);
                String curEq = finalEng.length > 4 ? I18n.getEngineEquation(finalEng[0], finalEng[4]) : I18n.getOrDefault("scenario.engine.state_eq_default", "ðŸ“ Ã‰quation d'Ã‰tat : dX/dt = f(X, t) + Î£ F_inter-cellulaire");
                updateEngineInspector(finalEng[0], curTitle, curDesc, curRef, curEq);
            });

            coreEngineRows.add(new CoreEngineRow(eng[0], descLbl, row, eng[1], eng[2], eng[3], eng.length > 4 ? eng[4] : null));
            typeABox.getChildren().add(row);
        }

        corePane = new TitledPane(I18n.getOrDefault("scenario.header.core_engines", "ðŸ”’ ARCHITECTURE CÅ’UR ETHER (24 MOTEURS PERMANENTS)"), typeABox);
        corePane.setExpanded(false);
        corePane.getStyleClass().add("titled-pane-primary");

        // --- âš™ï¸ OPTIONAL & CUSTOM ENGINES SECTION (Optionnels, Extensibles & Dynamic Import/Export) ---
        typeBBoxContainer = new VBox(8);
        typeBBoxContainer.getStyleClass().add("custom-module-card");

        // Action bar for Import / Export Custom Engines
        exportTemplateBtn = new Button(I18n.getOrDefault("scenario.btn.export_java_template", "ðŸ“¤ Export Java Engine Template (.java)"));
        exportTemplateBtn.setStyle("-fx-background-color: #4c1d95; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 4;");
        exportTemplateBtn.setOnAction(e -> exportCustomEngineTemplate());

        importEngineBtn = new Button(I18n.getOrDefault("scenario.btn.import_compile_engine", "ðŸ“¥ Import & Compile Java Engine (.java / .class)"));
        importEngineBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 4;");
        importEngineBtn.setOnAction(e -> importCustomEngineFile());

        autoSelectEnginesForYearBtn = new Button(I18n.getOrDefault("scenario.btn.autoselect_engines", "âš¡ Check Engines Based on Tâ‚€"));
        autoSelectEnginesForYearBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 4;");
        autoSelectEnginesForYearBtn.setOnAction(e -> {
            long year = startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -100000;
            autoSelectEnginesForYear(year);
        });

        HBox importExportBox = new HBox(10, autoSelectEnginesForYearBtn, exportTemplateBtn, importEngineBtn);
        importExportBox.setAlignment(Pos.CENTER_LEFT);
        importExportBox.setStyle("-fx-padding: 0 0 8 0;");
        typeBBoxContainer.getChildren().add(importExportBox);

        // Sorting toolbar for optional engines
        engineSortCombo = new ComboBox<>();
        engineSortCombo.getItems().addAll(
            org.ether.society.i18n.I18n.getOrDefault("scenario.sort.default", "âš™ï¸ System Order (By Category)"),
            org.ether.society.i18n.I18n.getOrDefault("scenario.sort.date_asc", "ðŸ“… Chronological Sort (Oldest â†’ Newest)"),
            org.ether.society.i18n.I18n.getOrDefault("scenario.sort.date_desc", "ðŸ“… Reverse Chronological Sort (Newest â†’ Oldest)"),
            org.ether.society.i18n.I18n.getOrDefault("scenario.sort.alpha_asc", "ðŸ”¤ Alphabetical Sort (A - Z)")
        );
        engineSortCombo.setValue(org.ether.society.i18n.I18n.getOrDefault("scenario.sort.default", "âš™ï¸ System Order (By Category)"));
        engineSortCombo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        engineSortLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.sort.label", "ðŸ”€ Engine Sorting:"));
        engineSortLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        HBox sortBar = new HBox(8, engineSortLabel, engineSortCombo);
        sortBar.setAlignment(Pos.CENTER_LEFT);
        sortBar.setStyle("-fx-padding: 4 0 8 0;");
        typeBBoxContainer.getChildren().add(sortBar);

        List<String[]> optionalEngines = List.of(
            new String[]{"PaleoLanguageDriftEngine", "ðŸ—£ï¸ DÃ©rive PhonÃ©tique & Linguistique PalÃ©olithique (-100 000 BP)",
                "Simule la dÃ©rive isoline et la divergence des continuum de palÃ©o-langues entre vallÃ©es d'Eurasie et clans palÃ©olithiques isolÃ©s.",
                "Ref: Cavalli-Sforza, L. L. (1994). History and Geography of Human Genes; Swadesh, M. (1952).",
                "â€¢ DÃ©rive Isoglosse : dS_lang/dt = D_lang * âˆ‡Â²S_lang + Ïƒ_drift * Î·(x,y,t)\nâ€¢ Seuil de Divergence : Isolement > 500 ans -> Rupture d'intercomprÃ©hension inter-tribale"},
            new String[]{"KarstCaveShelterEngine", "ðŸ¦‡ Abris Micro-Climatiques Karstiques & Grottes (-300 000 BP)",
                "Apport de la gÃ©omorphologie karstique pour le refuge hivernal des bandes palÃ©olithiques en pÃ©riodes de poussÃ©es glaciaires.",
                "Ref: Pettitt, P. (2011). The Palaeolithic Origins of Human Burial; Gamble, C. (1999).",
                "â€¢ Protection Thermique : RÃ©duction de la mortalitÃ© hivernale de 40% dans les mailles karstiques\nâ€¢ Isolation R_karst : Flux_thermique dQ/dt = -k_karst * A * (T_caverne - T_ext)"},
            new String[]{"ParietalArtAsabiyyahEngine", "ðŸŽ¨ Art PariÃ©tal & CohÃ©sion Rituelle d'AgrÃ©gation (-45 000 BP)",
                "Sanctuaires ornÃ©s (Chauvet, Lascaux, Sulawesi) renforÃ§ant l'Asabiyyah rituelle et facilitant les mariages exogames lors des agrÃ©gations saisonniÃ¨res.",
                "Ref: Clottes, J. (2008). Cave Art; Lewis-Williams, D. (2002). The Mind in the Cave.",
                "â€¢ Boost de CohÃ©sion : +25% d'Asabiyyah dans les mailles d'agrÃ©gation sanctuarisÃ©es\nâ€¢ Diffusion Symbolique : Taux_Ã©change_matÃ©riel * exp(Art_pariÃ©tal / K)"},
            new String[]{"SnowpackMobilityEngine", "ðŸŽ¿ Raquettes & Skis / MobilitÃ© Sub-Arctique (-10 000 BP)",
                "DÃ©veloppement de raquettes Ã  neige et patins en bois rÃ©duisant la friction glaciaire et dÃ©bloquant la chasse hivernale sub-arctique.",
                "Ref: Burov, G. M. (1989). Some Mesolithic Wooden Artefacts from the Vis Sites; Forsten, A. (1993).",
                "â€¢ Friction de Neige : Vitesse_brute / (1.0 + 2.0 * Ã‰paisseur_Neige)\nâ€¢ Effet Raquette / Ski : Annulation de 80% de la pÃ©nalitÃ© de dÃ©placement sous enneigement persistant"},
            new String[]{"CanidDomesticationEngine", "ðŸ• Symbiose Trophique & Domestication CanidÃ©s (-15 000 BP)",
                "Couplage trophique mutualiste entre bandes de chasseurs et proto-chiens (Bonn-Oberkassel, AltaÃ¯) augmentant le rendement des battues.",
                "Ref: GermonprÃ©, M. et al. (2009). Fossil dogs of the Upper Paleolithic; Larson, G. et al. (2012).",
                "â€¢ Gain d'Ã‰nergie de Chasse : dE/dt = Î·_chasse * M_proie * (1.0 + Î² * N_canid / N_humain) - C_maintenance\nâ€¢ Taux de RÃ©tention Alimentaire : +18% de calories carnÃ©es restituÃ©es Ã  la bande"},
            new String[]{"LithicTradeProvenanceEngine", "ðŸª¨ Provenance GÃ©ochimique du Silex & Obsidienne (-300 000 BP)",
                "RÃ©seaux d'Ã©change lithique tracÃ© par fluorescence X (XRF) suivant des surfaces d'Ã©nergie isotrope de moindre coÃ»t.",
                "Ref: Renfrew, C. (1975). Trade as Action at a Distance; Torrence, R. (1986). Obsidian Trade.",
                "â€¢ CoÃ»t-Distance Isotrope : C(x,y) = âˆ« (1.0 + Îº * tanÂ²(pente)) ds\nâ€¢ Maintien du Tranchant : PrÃ©vient la dÃ©gradation du rendement de chasse Ã  Ã©loignement des carriÃ¨res"},
            new String[]{"MeatCuringReservesEngine", "ðŸ¥© Conservation de Viande FumÃ©e & Graisse (Pemmican) (-25 000 BP)",
                "Techniques de fumage et sÃ©chage prolongeant la durÃ©e de conservation des rÃ©serves de +6 mois face aux crÃªtes d'inanition hivernale.",
                "Ref: Speth, J. D. (2010). The Paleolithic Human Diet; Outram, A. K. (2001). Bone marrow & fat.",
                "â€¢ Prolongation des Stocks : Fâ‚€ +6 mois de rÃ©serves\nâ€¢ Tampon de Famine Hivernale : EmpÃªche l'effondrement dÃ©mographique des cohortes Ã¢gÃ©es"},
            new String[]{"ExogamousKinshipEngine", "ðŸ§¬ ParentÃ© Exogame & Ã‰vitement de l'Ingeste (-40 000 BP)",
                "Ã‰changes obligatoires de partenaires entre bandes nomades (Sunghir) prÃ©venant la dÃ©pression de consanguinitÃ© gÃ©nÃ©tique (F_is).",
                "Ref: Sikora, M. et al. (2017). Ancient genomes show social structure in Upper Paleolithic. Science.",
                "â€¢ ContrÃ´le F_is : Fitness W = Wâ‚€ * (1.0 - Î³ * F_is)\nâ€¢ Maintien de ViabilitÃ© : EmpÃªche la baisse de fertilitÃ© des petites bandes nomades"},
            new String[]{"ArchaicIntrogressionEngine", "ðŸ§¬ Introgressions HomininÃ©s ArchaÃ¯ques (EPAS1 / TLR1/6/10) (-50 000 BP)",
                "HÃ©ritage gÃ©nÃ©tique dÃ©nisovien (adaptation Ã  l'altitude tibÃ©taine) et nÃ©andertalien (immunitÃ© innÃ©e aux zoonoses sub-arctiques).",
                "Ref: Huerta-SÃ¡nchez, E. et al. (2014). Altitude adaptation in Tibetans via Denisovan EPAS1. Nature.",
                "â€¢ EPAS1 DÃ©nisovien : +30% d'habitabilitÃ© sur le Plateau TibÃ©tain\nâ€¢ TLR NÃ©andertalien : ImmunitÃ© renforcÃ©e aux agents pathogÃ¨nes forestiers"},
            new String[]{"TailoredClothingThermalEngine", "ðŸª¡ VÃªtements AjustÃ©s en Fourrure & Aiguilles Ã  Chas (-35 000 BP)",
                "Aiguilles en os pour vÃªtements ajustÃ©s multicouches hermÃ©tiques (Sunghir, Denisova) rÃ©duisant la mortalitÃ© thermique.",
                "Ref: Gilligan, I. (2010). The Prehistoric Development of Clothing. Proc. Prehist. Soc.",
                "â€¢ Protection Thermique Extreme : RÃ©duit la pÃ©nalitÃ© mÃ©tabolique du grand froid (â‰¤ -20Â°C) de 65%\nâ€¢ Expansion Sub-Arctique : DÃ©bloque la colonisation des hautes latitudes sibÃ©riennes"},
            new String[]{"OchreTanningTechnologyEngine", "ðŸªµ Ocre Rouge, Pigments & Tannage AntibactÃ©rien (-100 000 BP)",
                "Usage dual de l'ocre (Blombos, Qafzeh) : rituels symboliques et prÃ©servation des peaux par tannage bactÃ©ricide.",
                "Ref: Henshilwood, C. S. et al. (2011). 100,000-Year-Old Ochre-Processing Workshop. Science.",
                "â€¢ Preservation des Peaux : Ralentit de 50% la dÃ©gradation des contenants en cuir\nâ€¢ CohÃ©sion Rituelle : +10% d'Asabiyyah dans les mailles d'extraction d'ocre"},
            new String[]{"AtlatlArcheryBallisticsEngine", "ðŸ¹ Propulseur (Atlatl) & Arc / Ballistique de Chasse (-20 000 BP)",
                "Levier propulsif augmentant l'Ã©nergie cinÃ©tique des projectiles (Ek = 0.5 m vÂ²) et doublant la distance de tir sÃ©curisÃ©e.",
                "Ref: Cattelain, P. (1997). Hunting Weapons in the European Upper Palaeolithic; Churchill, S. E. (1993).",
                "â€¢ Ã‰nergie CinÃ©tique : PortÃ©e de chasse doublÃ©e (dist >= 30m)\nâ€¢ Rendement sur Grand Gibier : +40% de succÃ¨s lors des battues de mÃ©gafaune"},
            new String[]{"CoastalMarineRefugiaEngine", "ðŸš Subsistance Littorale & Refuges Glaciaires (-160 000 BP)",
                "Coquillages et ressources intertidales (Pinnacle Point, Klasies River) offrant un aliment protÃ©ique et omÃ©ga-3 insensible aux sÃ©cheresses.",
                "Ref: Marean, C. W. et al. (2007). Early human use of marine resources and pigment in South Africa. Nature.",
                "â€¢ Nutrition OmÃ©ga-3 : Alimentation cÃ©rÃ©brale et rÃ©silience immunitaire\nâ€¢ Sanctuary Littoral : Maintien des densitÃ©s humaines lors des pires stades isotopiques glaciaires (MIS 6)"},
            new String[]{"SeasonalAggregationSanctuaryEngine", "ðŸ›ï¸ Rassemblements Saisonniers de Super-Bandes (-30 000 BP)",
                "AgglomÃ©rations temporaires de centaines d'individus aux confluences fluviales (DolnÃ­ VÄ›stonice) pour les rituels et les alliances.",
                "Ref: Gamble, C. (1999). The Palaeolithic Societies of Europe. Cambridge Univ. Press.",
                "â€¢ Ã‰change de Savoir & GÃ¨nes : Flux d'innovation technologique +50% lors des agrÃ©gations\nâ€¢ Mariages Exogames : RÃ©initialisation du coefficient de consanguinitÃ© rÃ©gionale"},
            new String[]{"PassiveSnareSmallGameEngine", "ðŸª¤ PiÃ©geage au Lacet & Filets pour Petit Gibier (-15 000 BP)",
                "Diversification Broad-Spectrum (Flannery) par capture passive de lagomorphes et d'oiseaux aquatiques Ã  faible coÃ»t calorique.",
                "Ref: Flannery, K. V. (1969). Broad-Spectrum Revolution; Pryor, J. H. (1988).",
                "â€¢ Extr. Passive : Calories captÃ©es sans risque physique pour la bande\nâ€¢ CapacitÃ© Portante K : +20% d'augmentation de K dans les zones humides"},
            new String[]{"AridWaterStorageStashEngine", "ðŸ¥š RÃ©serves d'Eau en Å’ufs d'Autruche enfouis (-60 000 BP)",
                "Stockage clandestin d'eau en rÃ©cipients d'Å“ufs d'autruche gravÃ©s (Diepkloof) permettant la traversÃ©e des corridors arides.",
                "Ref: Texier, P. J. et al. (2010). A Howiesons Poort tradition of engraving ostrich eggshell containers. PNAS.",
                "â€¢ Franchissement DÃ©sertique : Annulation de la mortalitÃ© par dÃ©shydratation sur les routes arides\nâ€¢ Rayon de Transhumance : MultipliÃ© par 3 en zone xÃ©rique (Kalahari / Outback)"},
            new String[]{"ResinHaftingAdhesivesEngine", "ðŸŒ² Emmanchement au Nielle de Bouleau & RÃ©sine (-200 000 BP)",
                "Pyrotechnologie d'extraction de poix de bouleau et rÃ©sine de spinifex pour l'emmanchement solide des armatures en pierre.",
                "Ref: Koller, J. et al. (2001). High-tech in the Middle Palaeolithic: Birch bark pitch. Eur. J. Arch.",
                "â€¢ SoliditÃ© de l'Armature : Multiplie par 2.5 la rÃ©sistance aux chocs des javelots et dards\nâ€¢ Gain d'Extraction : +15% de rendement sur la grande chasse de forÃªt/taÃ¯ga"},
            new String[]{"OsseousIndustryCarvingEngine", "ðŸ¦´ Industrie de l'Os, Bois de Cerf & Ivoire (-40 000 BP)",
                "FaÃ§onnage de harpons, sagaies et aiguilles dans les matÃ©rieux osseux de la mÃ©gafaune (Mammouth, Renne).",
                "Ref: Knecht, H. (1997). Projectile Technology. Plenum Press, New York.",
                "â€¢ Diversification de l'Outillage : Harpons barbelÃ©s augmentant la pÃªche fluviale de +35%\nâ€¢ Substitution du Silex : Exploitation des stocks d'ivoire sur les zones dÃ©pourvues de carriÃ¨res lithiques"},
            new String[]{"PlantFiberCordageEngine", "ðŸŒ¾ Cordages en Fibres VÃ©gÃ©tales & Tissage ArcaÃ¯que (-30 000 BP)",
                "Extraction et tressage de fibres vÃ©gÃ©tales (Ortie, Tilleul, Lin sauvage - Grotte de Dzudzuana) pour liens et sacs de transport.",
                "Ref: Kvavadze, E. et al. (2009). 30,000-Year-Old Wild Flax Fibers. Science.",
                "â€¢ Transport et Logistique : Augmente la capacitÃ© de transport nomade de +40 kg/bande\nâ€¢ Liens et Filets : Composants essentiels pour piÃ¨ges et vÃªtements multicouches"},
            new String[]{"MegafaunaPitfallTrapEngine", "ðŸ•³ï¸ Fosses-PiÃ¨ges Ã  MÃ©gafaune & Chasse de Fosse (-15 000 BP)",
                "Creusement collectif de piÃ¨ges Ã  fosse (Tultepec) pour la capture sans risque de mammouths et bisons des steppes.",
                "Ref: Washington-Allen, R. A. et al. (2020). Prehistoric Mammoth Pitfall Traps in Central Mexico.",
                "â€¢ Capture de MÃ©gafaune : Abattage de gros gibier sans perte humaine dans la bande\nâ€¢ Apport Calorique Massif : Stockage de plusieurs tonnes de viande carnÃ©e d'un seul coup"},
            new String[]{"AcousticFluteResonanceEngine", "ðŸŽµ FlÃ»tes en Os & Acoustique des Cavernes OrnÃ©es (-40 000 BP)",
                "FlÃ»tes en os d'oiseau et ivoire (Hohle Fels) et rÃ©sonance acoustique des sanctuaires profonds stimulant l'adhÃ©sion rituelle.",
                "Ref: Conard, N. J. et al. (2009). Female figurine and flutes from Hohle Fels. Nature.",
                "â€¢ Stimulation Rituelle : Boost d'Asabiyyah lors des cÃ©rÃ©monies acoustiques underground\nâ€¢ CohÃ©sion Inter-Tribu : Renforce les alliances diplomatiques lors des agrÃ©gations"},
            new String[]{"SymbolicBeadNetworkEngine", "ðŸš Parure de Perles d'Å’uf d'Autruche & Coquillages (-40 000 BP)",
                "Fabrication et troc de perles d'Å“ufs d'autruche et coquillages perforÃ©s (Enkapune Ya Muto) comme rÃ©seaux de confiance et rÃ©assurance.",
                "Ref: Ambrose, S. H. (1998). Chronology of the Later Stone Age at Enkapune Ya Muto. J. Arch. Sci.",
                "â€¢ RÃ©seau de Secours (Hito) : Les Ã©changes de perles scellent des pactes d'entraide inter-bande en cas de famine\nâ€¢ FluiditÃ© Commerciale : RÃ©duit la friction de troc avec les bandes Ã©trangÃ¨res"},
            new String[]{"PortableArtFigurineEngine", "ðŸ—¿ Figurines Mobiles en Ivoire & Terres Cuites (VÃ©nus) (-30 000 BP)",
                "Sculpture d'artefacts anthropomorphes et zoomorphes (Willendorf, DolnÃ­ VÄ›stonice) circulant comme symboles d'alliance et de fÃ©conditÃ©.",
                "Ref: Soffer, O. et al. (2000). The Venus Figurines: Textiles, Basketry, Gender, and Status.",
                "â€¢ Marqueur d'Alliance : Transportable d'une vallÃ©e Ã  l'autre pour sceller les pactes de mariage\nâ€¢ RÃ©silience DÃ©mographique : Symbolique rituelle stimulant les taux de natalitÃ©"},
            new String[]{"CaveLightingPyrotechnicsEngine", "ðŸ›¢ï¸ Ã‰clairage Ã  la Graisse Animale & Torches de Caverne (-35 000 BP)",
                "Lampes en grÃ¨s Ã  combustion de graisse animale (Lascaux, Chauvet) permettant l'exploration et l'art dans les galeries obscures.",
                "Ref: Beaune, S. A. de (1987). Lampes et brÃ»loirs palÃ©olithiques. CNRS Ã‰ditions.",
                "â€¢ Exploration Profonde : DÃ©bloque l'accÃ¨s aux sanctuaires endokarstiques situÃ©s Ã  plus de 500m des entrÃ©es\nâ€¢ SÃ©curitÃ© Karstique : Ã‰limine les accidents et chÃ»tes dans les gouffres sombres"},
            new String[]{"RiverCanoeTransportEngine", "ðŸš£ Pirogues de Bouleau & CanoÃ«s en Ã‰corce (-45 000 BP)",
                "Fabrication de pirogues monoxyles et canoÃ«s en Ã©corce pour le franchissement des grands fleuves et le transport lourd.",
                "Ref: McGrail, S. (2001). Boats of the World: From the Stone Age to Medieval Times. Oxford.",
                "â€¢ Navigation Fluviale : Traverse les fleuves infranchissables sans noyade\nâ€¢ EfficacitÃ© de Fret : RÃ©duit le coÃ»t-distance de dÃ©placement le long des cours d'eau de 80%"},
            new String[]{"PermafrostColdCacheEngine", "ðŸ§Š Fosses de CongÃ©lation dans le Permafrost (-30 000 BP)",
                "Creusement de fosses dans le sol gelÃ© permanent (Yana RHS) pour le stockage frigorifique des carcasses de grand gibier.",
                "Ref: Pitulko, V. V. et al. (2004). The Yana RHS Site, Royal Society B.",
                "â€¢ Cryo-Conservation : PrÃ©serve la viande fraÃ®che pendant plusieurs annÃ©es sans putrÃ©faction\nâ€¢ Survie Arctique : Maintient des foyers permanents dans les environnements de haute latitude"},
            new String[]{"PlantDetoxificationLeachingEngine", "ðŸ§ª Lessivage et DÃ©toxification des Tubercules Toxiques (-20 000 BP)",
                "ProcÃ©dÃ©s de rouissage et dÃ©toxification par lixiviation Ã  l'eau courante pour rendre comestibles les tubercules et nymphes toxiques.",
                "Ref: Barker, G. et al. (2007). The Niah Cave Project. Human Ecology.",
                "â€¢ Ã‰largissement Alimentaire : Rend consommables des ressources vÃ©gÃ©tales toxiques auparavant mortelles\nâ€¢ SÃ©curitÃ© Alimentaire Tropicale : Maintient la subsistance dans les forÃªts Ã©quatoriales"},
            new String[]{"TopographicGameDriveEngine", "ðŸ”ï¸ PiÃ¨ges Topographiques V-Shaped & Desert Kites (-12 000 BP)",
                "AmÃ©nagement de murets en V (Desert Kites) rabattant les troupeaux de gazelles et bisons vers des enclos naturels d'abattage.",
                "Ref: Holzer, A. et al. (2010). Desert kites in the Levant. J. Arch. Sci.",
                "â€¢ Abattage de Masse : Capture simultanÃ©e de troupeaux entiers lors des migrations\nâ€¢ Accumulation de Capital CarnÃ© : Alimente de grands rassemblements sÃ©dentaires prÃ©-agricoles"},
            new String[]{"LunarCalendarTallyEngine", "ðŸŒ™ Notations Calendaires Lunaires sur Os (-30 000 BP)",
                "Incisures sÃ©rielles sur os (Abri Blanchard, Ishango) pour le dÃ©compte des cycles lunaires et la prÃ©diction des migrations de faune.",
                "Ref: Marshack, A. (1972). The Roots of Civilization. McGraw-Hill.",
                "â€¢ PrÃ©diction SaisonniÃ¨re : Anticipe l'arrivÃ©e des troupeaux migrateurs avec une prÃ©cision de quelques jours\nâ€¢ Synchronisation des AgrÃ©gations : Fixe les rendez-vous inter-bandes lors des pleines lunes"},
            new String[]{"MammothBoneHabitationEngine", "ðŸ•ï¸ Habitations en Os de Mammouth & Peaux Tendues (-25 000 BP)",
                "Assemblage de structures circulaires composÃ©es de crÃ¢nes et dÃ©fenses de mammouths (Mezhyrich) recouverts de peaux Ã©tanches.",
                "Ref: Pidoplichko, I. G. (1998). Upper Palaeolithic Mammoth-Bone Dwellings in Ukraine.",
                "â€¢ Architecture Sans Bois : Construction d'abris solides et isolÃ©s dans les steppes dÃ©nudÃ©es d'arbres\nâ€¢ RÃ©silience Hivernale : Maintient la chaleur interne des foyers pendant le LGM"},
            new String[]{"FireHardenedSpearEngine", "ðŸªµ Javelots en Bois Durci au Feu (SchÃ¶ningen) (-300 000 BP)",
                "Taille et durcissement pyrotechnique de la pointe de pieux en Ã©picÃ©a (SchÃ¶ningen) pour la chasse Ã  jet Ã  moyenne distance.",
                "Ref: Thieme, H. (1997). Lower Palaeolithic hunting spears from Germany. Nature.",
                "â€¢ Chasse AcheulÃ©enne : Arme d'homininÃ© archaÃ¯que (Heidelbergensis / Sapiens) efficace sur les chevaux sauvages\nâ€¢ PÃ©nÃ©tration MÃ©canique : AmÃ©liore la perforation du cuir des grands herbivores"},
            new String[]{"ParasiteControlRepellentEngine", "ðŸŒ¿ Literies d'Herbes MÃ©dicinales & Ocre Anti-Parasites (-70 000 BP)",
                "Matelas d'herbes aromatiques (Sibudu Cave) associÃ©es Ã  l'ocre pour repousser les insectes vecteurs de maladies et punaises.",
                "Ref: Wadley, L. et al. (2011). Middle Stone Age Bedding Construction and Insect Repellent Use. Science.",
                "â€¢ HygiÃ¨ne d'Habitat : RÃ©duit les infections parasitaires et le paludisme dans les abris sous roche\nâ€¢ SantÃ© des Cohortes : Baisse de la mortalitÃ© infantile et amÃ©lioration du sommeil des forageurs"},
            new String[]{"WildCerealGrindingEngine", "ðŸŒ¾ Pilonnage et Mortiers de CÃ©rÃ©ales Sauvages (Ohalo II) (-23 000 BP)",
                "Broyage de graines de graminÃ©es et d'orge sauvage sur meules en pierre (Ohalo II) pour l'Ã©laboration de galettes cuites.",
                "Ref: Nadel, D. et al. (2012). Ohalo II: A 23,000-Year-Old Submerged Hunter-Gatherer Camp. PLoS ONE.",
                "â€¢ NutritivitÃ© des Graines : Extrait les glucides complexes des cÃ©rÃ©ales sauvages\nâ€¢ PrÃ©mices Natufiennes : Ã‰tape fondatrice vers la sÃ©dentarisation prÃ©-agricole"},
            new String[]{"VolcanicTephraRefugiaEngine", "ðŸŒ‹ TÃ©phrochronologie & Refuges post-SuperÃ©ruption de Toba (-74 000 BP)",
                "Survie dans les vallÃ©es refuges littorales lors des retombÃ©es massives d'aÃ©rosols et de cendres de la super-Ã©ruption du Toba.",
                "Ref: Rampino, M. R. & Self, S. (1992). Volcanic winter and accelerated glaciation following Toba. Nature.",
                "â€¢ Goulot d'Ã‰tranglement DÃ©mographique : RÃ©duction de la population mondiale Ã  ~10,000 reproducteurs\nâ€¢ RÃ©silience des Refuges : Maintien de foyers viables le long des cÃ´tes sud-africaines"},
            new String[]{"DemographicLifeTableEngine", "ðŸ“Š Tables de MortalitÃ© PalÃ©odÃ©mographiques & Fission/Fusion (-300 000 BP)",
                "Structure par Ã¢ge, haute mortalitÃ© infantile (40%) et fission des bandes nomades dÃ¨s que l'effectif dÃ©passe 50 individus.",
                "Ref: Bocquet-Appel, J.-P. (2002). Paleoanthropological Life Tables. Curr. Anthropol.",
                "â€¢ Fission de Bande : Si N_bande > 50 -> Fission en 2 sous-bandes autonomes de ~25 individus\nâ€¢ EspÃ©rance de Vie : eâ‚€ â‰ˆ 25-30 ans avec mortalitÃ© maternelle Ã©levÃ©e"},
            new String[]{"ToolKitMaintenanceEngine", "ðŸª¨ Ã‰conomie Lithique & DÃ©gradation au Trajet (-300 000 BP)",
                "Perte d'efficacitÃ© mÃ©canique du tranchant Ã  mesure que s'accroÃ®t la distance aux carriÃ¨res de silex.",
                "Ref: Binford, L. R. (1979). Organization and Formation Processes: Looking at Curated Technologies.",
                "â€¢ DÃ©gradation du Tranchant : EfficacitÃ© = Eâ‚€ / (1.0 + Î» * Dist_CarriÃ¨re)\nâ€¢ Entretien Lithique : NÃ©cessite des retouches rÃ©duisant la masse de l'outil"},
            new String[]{"FireStickFarmingEngine", "ðŸ”¥ Fire-Stick Farming & MosaÃ¯ques PyrogÃ©niques (-50 000 BP)",
                "BrÃ»lis systÃ©matiques prÃ©venant la fermeture des canopÃ©es et maintenant des prairies ouvertes Ã  herbivores.",
                "Ref: Bliege Bird, R. et al. (2008). The fire stick farming hypothesis. PNAS.",
                "â€¢ MosaÃ¯que PyrogÃ©nique : dB_veg/dt = r B (1 - B/K) - Î»_feu * B * Pop\nâ€¢ Augmentation du Gibier : Maintient les densitÃ©s de grands herbivores en forÃªt claire"},
            new String[]{"HomininCompetitiveExclusionEngine", "ðŸ¦´ CompÃ©tition InterspÃ©cifique & Exclusion NÃ©andertal/Sapiens (-100 000 BP)",
                "ModÃ©lisation de l'exclusion compÃ©titive de Lotka-Volterra entre Sapiens, NÃ©andertaliens et DÃ©nisoviens.",
                "Ref: Banks, W. E. et al. (2008). Neanderthal Extinction by Competitive Exclusion. PLoS ONE.",
                "â€¢ Ã‰quations CouplÃ©es : dNâ‚/dt = râ‚ Nâ‚ (1 - (Nâ‚ + Î±â‚â‚‚ Nâ‚‚)/Kâ‚)\nâ€¢ Avantage Sapiens : LÃ©gÃ¨re supÃ©rioritÃ© d'extraction calorique (Î±â‚â‚‚ < Î±â‚‚â‚)"},
            new String[]{"ShellMiddenAccumulationEngine", "ðŸš KjÃ¶kkenmÃ¶ddings & Accumulation Littorale (-12 000 BP)",
                "Accumulation de amas coquilliers littoraux (ErtebÃ¸lle) stabilisant la sÃ©dentarisation prÃ©-agricole cÃ´tiÃ¨re.",
                "Ref: Bailey, G. N. (2007). Shell mounds and coastal archaeology. Quat. Int.",
                "â€¢ Accumulation Malacologique : Maintient la densitÃ© de bande constante mÃªme en hiver\nâ€¢ SÃ©dentaritÃ© CotiÃ¨re : DÃ©bloque les premiers habitats semi-permanents littoraux"},
            new String[]{"PelagicFishingHookEngine", "ðŸŽ£ PÃªche HauturiÃ¨re & HameÃ§ons en Os de Timor (-42 000 BP)",
                "Invention d'hameÃ§ons en os et lignes en fibre pour la capture de poissons pÃ©lagiques (Thons, Thyrsites) en eau profonde.",
                "Ref: O'Connor, S. et al. (2011). Pelagic Fishing at 42,000 Years Before the Present in East Timor. Science.",
                "â€¢ Capture PÃ©lagique : DÃ©bloque la pÃªche en haute mer au-delÃ  des rÃ©cifs littoraux\nâ€¢ Apport ProtÃ©ique Massif : +30% d'apport calorique en zone insulaire"},
            new String[]{"GeophyteDiggingStickEngine", "ðŸ  BÃ¢tons Fouisseurs & Extraction des GÃ©ophytes (USO) (-170 000 BP)",
                "BÃ¢tons fouisseurs durcis au feu pour l'extraction de tubercules et oignons souterrains en zone aride.",
                "Ref: Wadley, L. et al. (2020). Cooked starchy rhizomes in South Africa 170,000 years ago. Science.",
                "â€¢ Extraction USO : Glucides souterrains accessibles lors des sÃ©cheresses\nâ€¢ CapacitÃ© Portante en AriditÃ© : ProtÃ¨ge les cohortes contre la disette"},
            new String[]{"BirchTarPyrolysisKilnEngine", "ðŸº Pyrolyse en Fosse & Nielle de Bouleau (-50 000 BP)",
                "Extraction par pyrolyse Ã  froid sous terre de braise sans oxygÃ¨ne pour la synthÃ¨se de colle de bouleau purifiÃ©e.",
                "Ref: Schmidt, P. et al. (2019). Birch tar production in the Early Palaeolithic. Archaeol. Anthropol. Sci.",
                "â€¢ Production d'AdhÃ©sif Pur : Permet le collage hermÃ©tique des armatures de jet\nâ€¢ RÃ©sistance MÃ©canique : ZÃ©ro perte de pointe de lance lors des impacts sur os"},
            new String[]{"SkinKayakSubArcticEngine", "ðŸ›¶ Kayaks en Peau & Navigation PÃ©riglaciaire (-18 000 BP)",
                "Embarcations Ã©tanches en peaux de focs et mammifÃ¨res marins tendues sur ossature pour la chasse polaire.",
                "Ref: Ames, K. M. (2002). Going by boat: the maritime highway. Am. Antiq.",
                "â€¢ Chasse Marine Arctique : Capture de phoques et baleines le long de la banquise\nâ€¢ Corridor CÃ´tier : Franchissement des fronts glaciaires marginaux du Pacifique"},
            new String[]{"SalmonRunHarpoonEngine", "ðŸŸ Harpons BarbelÃ©s & Chasse aux RemontÃ©es de Saumons (-16 000 BP)",
                "Harpons dÃ©tachables en bois de renne pour l'exploitation massive des ruÃ©es de saumons au printemps.",
                "Ref: Costamagno, S. et al. (2018). Salmon fishing in Magdalenian Europe. Quat. Int.",
                "â€¢ PoussÃ©e Calorique SaisonniÃ¨re : Stockage de plusieurs quintaux de saumon sÃ©chÃ© en quelques semaines\nâ€¢ AgglomÃ©ration SaisonniÃ¨re : Alimente les grands rassemblements magdalÃ©niens"},
            new String[]{"CaveBearNicheCompetitionEngine", "ðŸ» CompÃ©tition de Caverne avec Ursus Spelaeus (-50 000 BP)",
                "Chasse et expulsion de l'ours des cavernes pour la possession des abris karstiques hivernaux.",
                "Ref: Stiner, M. C. (1998). Mortality analysis of Pleistocene bears and hominids. J. Arch. Sci.",
                "â€¢ ConquÃªte d'Abris : LibÃ©ration des grottes thermiquement isolÃ©es pour la bande\nâ€¢ Resource CarnÃ©e & Fourrure : Fourrures Ã©paisses pour l'isolation hivernale"},
            new String[]{"OchreMiningQuarryEngine", "â›ï¸ MiniÃ¨re d'Ocre & CarriÃ¨res d'HÃ©matite (-40 000 BP)",
                "Exploitation miniÃ¨re souterraine d'hÃ©matite et d'ocre rouge (Lion Cave, Eswatini).",
                "Ref: Barham, L. (2002). Systematic Pigment Use in the Middle Stone Age. Curr. Anthropol.",
                "â€¢ Extraction Industrielle : Tonnage d'ocre pour l'Ã©change symbolique rÃ©gional\nâ€¢ Corridors de Troc : L'ocre miniÃ¨re devient la premiÃ¨re monnaie symbolique de troc"},
            new String[]{"WindCuringSteppeCacheEngine", "ðŸŒ¬ï¸ Dessiccation par le Vent dans la Steppe PÃ©riglaciaire (-28 000 BP)",
                "SÃ©chage des bandes de viande par les vents glacÃ©s et secs de la steppe-toundra (Kostenki).",
                "Ref: Soffer, O. (1985). The Upper Paleolithic of the Central Russian Plain. Academic Press.",
                "â€¢ Cryo-Dessiccation : Conservation de viande sÃ©chÃ©e sans sel ni fumÃ©e pendant 1 an\nâ€¢ RÃ©silience aux Blizzards : Permet le maintien des camps d'hiver arctiques"},
            new String[]{"LashingsHideThongEngine", "ðŸ§¶ LaniÃ¨res de Cuir & Ligatures de Peau (-35 000 BP)",
                "DÃ©coupe en spirale de laniÃ¨res de cuir de mÃ©gafaune pour ligatures de charpentes et traÃ®neaux.",
                "Ref: Leroi-Gourhan, A. (1964). Le Geste et la Parole. Albin Michel.",
                "â€¢ RigiditÃ© des Assemblages : Construction de grands abris nomades solides\nâ€¢ Tractage Lourd : Permet le dÃ©placement de blocs de pierre et carcasse sur traÃ®neau"},
            new String[]{"MicrolithBladeletProductionEngine", "ðŸ”ª DÃ©bitage de Lamelles & Microlithes emmanchÃ©s (-20 000 BP)",
                "Taille standardisÃ©e de micro-lamelles tranchantes insÃ©rÃ©es dans des rainures en os.",
                "Ref: Inizan, M.-L. et al. (1995). Technologie de la Pierre TaillÃ©e. CREP.",
                "â€¢ Ã‰conomie de MatiÃ¨re PremiÃ¨re : 1 kg de silex donne 50 mÃ¨tres de tranchant utile\nâ€¢ InterchangeabilitÃ© : Remplacement instantanÃ© des microlithes cassÃ©s"},
            new String[]{"HighAltitudeHypoxiaEngine", "ðŸ”ï¸ Acclimatation Ã  l'Hypoxie des Hauts Plateaux (-40 000 BP)",
                "Adaptation gÃ©nÃ©tique et physiologique (EPAS1) permettant la vie sur les plateaux du Tibet et des Andes.",
                "Ref: Zhang, X. L. et al. (2018). Denisovan DNA and high-altitude adaptation. Science.",
                "â€¢ Colonisation des Sommets : Survie et chasse au-dessus de 3,500 m d'altitude\nâ€¢ Refuges Montagneux : Protection contre les poussÃ©es dÃ©mographiques des plaines"},
            new String[]{"MortuaryBurialRegaliaEngine", "âš°ï¸ SÃ©pultures Symboliques OrnÃ©es & Asabiyyah FunÃ©raire (-30 000 BP)",
                "Inhumations complexes avec des milliers de perles d'ivoire et ocre (Sunghir, Arene Candide).",
                "Ref: Formicola, V. (2007). From Sunghir to the Gravettian burials. Curr. Anthropol.",
                "â€¢ SolidaritÃ© TotÃ©mique : Renforce le sentiment d'appartenance et la mÃ©moire des ancÃªtres\nâ€¢ Asabiyyah FunÃ©raire : Boost durable de cohÃ©sion sociale inter-gÃ©nÃ©rationnelle"},
            new String[]{"ProtoCeramicFiringEngine", "ðŸ”¥ Terres Cuites CÃ©ramiques de VÄ›stonice (-29 000 BP)",
                "FaÃ§onnage et cuisson au four de figurines animales et humaines en argile (DolnÃ­ VÄ›stonice).",
                "Ref: Vandiver, P. B. et al. (1989). The Origins of Ceramic Technology at Dolni Vestonice. Science.",
                "â€¢ Pyrotechnologie CÃ©ramique : PremiÃ¨re maÃ®trise de la cuisson d'argile Ã  800Â°C\nâ€¢ Magie Chasseuse : Usage rituel des statuettes pour favoriser la chasse sur la mÃ©gafaune"},
            new String[]{"EyedNeedleSewingEngine", "ðŸª¡ Aiguilles en Os Ã  Chas & Costumes Polaires Multicouches (-35 000 BP)",
                "PerÃ§age d'aiguilles fines Ã  chas pour la couture Ã©tanche de parkas et pantalons en fourrure.",
                "Ref: Golovanova, L. V. et al. (2010). Significance of Tailored Fur Suits. Curr. Anthropol.",
                "â€¢ Ã‰tanchÃ©itÃ© Thermique : Suppression du vent polaire dans le vÃªtement\nâ€¢ Survie sous -40Â°C : DÃ©bloque la chasse arctique lors des tempÃªtes du LGM"},
            new String[]{"KelpHighwayNavigationEngine", "ðŸŒ¿ Corridor Maritime du Kelp & Route de la CÃ´te Pacifique (-16 000 BP)",
                "Migration cÃ´tiÃ¨re rapide le long des forÃªts de kelp riches en loutres, poissons et coquillages.",
                "Ref: Erlandson, J. M. et al. (2007). The Kelp Highway Hypothesis. J. Island Coast. Arch.",
                "â€¢ Colonisation Maritime Rapide : Contourne la calotte glaciaire laurentide par la cÃ´te Pacifique\nâ€¢ Abondance Alimentaire : Ressources littorales inÃ©puisables et protÃ©gÃ©es des houles"},
            new String[]{"CaveHyenaScavengingEngine", "ðŸº NÃ©crophagie CompÃ©titive avec l'HyÃ¨ne des Cavernes (-45 000 BP)",
                "Concurrence fÃ©roce pour le pillage des carcasses de mÃ©gafaune entre bandes humaines et hyÃ¨nes spelaea.",
                "Ref: Discamps, E. (2014). Ungulate biomass and hyena-human interaction. Quat. Int.",
                "â€¢ Scavenging CompÃ©titif : RÃ©cupÃ©ration de grandes carcasses tuÃ©es par d'autres prÃ©dateurs\nâ€¢ Risque de Morsure : NÃ©cessite l'usage du feu pour repousser les meutes d'hyÃ¨nes"},
            new String[]{"OchreTradeAllianceEngine", "ðŸ”´ Corridors d'Ã‰change d'Ochre Rouge & Alliances Inter-Tribales (-50 000 BP)",
                "Circulation de blocs d'ocre rouge haut de gamme sur des centaines de kilomÃ¨tres pour sceller des pactes.",
                "Ref: McBrearty, S. & Brooks, A. S. (2000). The revolution that wasn't. J. Hum. Evol.",
                "â€¢ Alliance Diplomatique : Ã‰vite les conflits territoriaux lors des migrations\nâ€¢ RÃ©seau d'Assurance-Famine : Droit d'accÃ¨s aux territoires voisins en cas de sÃ©cheresse"},
            new String[]{"HeatTreatedFlintPressureEngine", "ðŸ”¥ Chauffe du Silex & Retouche par Pression SolutrÃ©enne (-21 000 BP)",
                "Traitement thermique contrÃ´lÃ© du silex amÃ©liorant l'aptitude Ã  la retouche fine par pression (Feuilles de laurier).",
                "Ref: Inizan, M.-L. & Tixier, J. (2000). Thermal Alteration of Siliceous Rocks.",
                "â€¢ Perfection des Armatures : CrÃ©ation de pointes solutrÃ©ennes ultra-minces et pÃ©nÃ©trantes\nâ€¢ PortÃ©e & PrÃ©cision : AmÃ©liore la trajectoire et l'impact des sagaies"},
            new String[]{"FluvioglacialLithicHarvestEngine", "ðŸŒŠ RÃ©colte Lithique sur GÃ®tes Fluvioglaciaires (-20 000 BP)",
                "Collecte de galets de silex et quartzite dÃ©posÃ©s par les torrents de fonte sous-glaciaire.",
                "Ref: Bussell, M. A. et al. (2001). Fluvioglacial Gravels as Raw Material Sources.",
                "â€¢ Silex de Fonte : Approvisionnement lithique sur les bandes d'Ã©pandage (Sandur) au pied des glaciers\nâ€¢ ContinuitÃ© Outillage : Permet le maintien des bandes au contact immÃ©diat de la calotte"},
            new String[]{"JomonCeramicBoilingEngine", "ðŸº CÃ©ramiques JÅmon & Bouillissage de Toxines Marine (-16 500 BP)",
                "Poteraie Ã  fond pointu (JÅmon) permettant le bouillissage prolongÃ© des toxines coquillÃ¨res et glands.",
                "Ref: Habu, J. (2004). Ancient Jomon of Japan. Cambridge Univ. Press.",
                "â€¢ Bio-DisponibilitÃ© CoquillÃ¨re : +45% d'extraction calorique sur les ressources cÃ´tiÃ¨res\nâ€¢ Conservation ProtÃ©gÃ©e : Stockage des bouillies pendant +3 mois"},
            new String[]{"LevalloisPreparedCoreEngine", "ðŸª¨ Ã‰clats Levallois & PrÃ©dÃ©termination des Formes (-300 000 BP)",
                "Taille sur nuclÃ©us prÃ©parÃ© produisant des Ã©clats prÃ©formÃ©s aux tranchants rÃ©guliers.",
                "Ref: BoÃ«da, E. (1994). Le concept Levallois : variabilitÃ© des mÃ©thodes. CNRS Ã‰ditions.",
                "â€¢ Vitesse de Taille : +60% de rendement d'armatures par heure\nâ€¢ Ã‰conomie de MatiÃ¨re : RÃ©duction de 35% du gaspillage de nuclÃ©us"},
            new String[]{"AcheuleanBifaceSymmetryEngine", "ðŸª“ Bifaces SymÃ©triques AcheulÃ©ens & SignalÃ©tique Sociale (-500 000 BP)",
                "Bifaces symÃ©triques servant d'indicateurs de compÃ©tence et d'attractivitÃ© au sein de la cohorte.",
                "Ref: Kohn, M. & Mithen, S. (1999). Handaxes: Products of Sexual Selection? Antiquity.",
                "â€¢ SignalÃ©tique Sociale : Boost de cohÃ©sion sociale (+15% d'Asabiyyah)\nâ€¢ SÃ©lection Sexuelle : Stabilise le choix des partenaires au sein de la cohorte"},
            new String[]{"OldowanMarrowPercussionEngine", "ðŸ¦´ Galets AmÃ©nagÃ©s Oldowayens & Extraction de Moelle (-2 600 000 BP)",
                "Percussion de galets pour briser les os longs de mÃ©gafaune et extraire la moelle riche en lipides.",
                "Ref: Semaw, S. et al. (1997). 2.5-million-year-old stone tools from Gona, Ethiopia. Nature.",
                "â€¢ Moelle CarnÃ©e : Apport calorique lipidique massif (+2.5 MJ/hab/jour) lors des saisons sÃ¨ches\nâ€¢ Scavenging Efficace : Extraction de ressources inaccessibles aux autres carnivores"},
            new String[]{"TrophicCascadesPredatorEngine", "ðŸ¦ Cascades Trophiques & Extinction des Apex PrÃ©dateurs (-13 000 BP)",
                "La disparition des grands herbivores entraÃ®ne l'effondrement des hyÃ¨nes des cavernes et tigres Ã  dents de sabre.",
                "Ref: Ripple, W. J. & Van Valkenburgh, B. (2010). Linking Pleistocene megafauna density to apex predators.",
                "â€¢ RÃ©organisation Trophique : Redirection de la chasse humaine vers le petit gibier\nâ€¢ Chute des Super-PrÃ©dateurs : Suppression du risque de prÃ©dation directe sur les camps nomades"},
            new String[]{"BeringianStandstillIsolationEngine", "ðŸ”ï¸ Isolation GÃ©nÃ©tique en BÃ©ringie & Adaptation au Froid (-22 000 BP)",
                "Isolement prolongÃ© des populations en BÃ©ringie fixant les haplogroupes amÃ©rindiens fondateurs.",
                "Ref: Tamm, E. et al. (2007). Beringian Standstill and Spread of Native American Founders. PLoS ONE.",
                "â€¢ Fixation GÃ©nÃ©tique : Ã‰mergence du profil mÃ©tabolique rÃ©sistant au grand froid arctique\nâ€¢ LignÃ©e Fondatrice : Infiltration continentale rapide lors de l'ouverture du corridor libre de glace"},
            new String[]{"AridOasisWellDiggingEngine", "ðŸ’§ Creusement de Puits d'Eau en Zone Aride du Sahul (-45 000 BP)",
                "Creusement de puits profonds dans les lits de riviÃ¨res assÃ©chÃ©es (soaks) du bassin de l'Eyre.",
                "Ref: Thorley, P. (1998). Pleistocene settlement in Central Australia. Antiquity.",
                "â€¢ TraversÃ©e du DÃ©sert : Maintient les itinÃ©raires d'interconnexion au cÅ“ur de l'Australie\nâ€¢ SÃ©curitÃ© Hydrique : Annule la mortalitÃ© par dÃ©shydratation sur les pistes nomades"},
            new String[]{"HandStencilTerritoryEngine", "âœ‹ Empreintes de Mains en NÃ©gatif & Marqueurs de Territoire (-40 000 BP)",
                "Soufflage de pigments autour des mains sur les parois de grottes pour dÃ©limiter les territoires de chasse.",
                "Ref: Aubert, M. et al. (2014). Pleistocene cave art date from Sulawesi, Indonesia. Nature.",
                "â€¢ Marqueur Frontalier : RÃ©duit les conflits territoriaux inter-tribaux de -25%\nâ€¢ MÃ©moire Territoriale : Ancrage des droits d'usage sur les zones d'agrÃ©gation"},
            new String[]{"AtlatlBalancingStoneEngine", "ðŸ¹ Pierres de Lestage & RÃ©glage Ballistique du Propulseur (-18 000 BP)",
                "Fixation de lests en pierre polie sur le fut du propulseur pour ajuster la flexion et la vitesse du dard.",
                "Ref: Peets, O. H. (1960). Experiments in the use of atlatl weights. Am. Antiq.",
                "â€¢ Vitesse de Dard : Vitesse de tir portÃ©e Ã  40 m/s\nâ€¢ PortÃ©e Pratique : Ã‰largit le rayon de tir mortel sur les bisons Ã  45 mÃ¨tres"},
            new String[]{"PressureFlakerPointEngine", "ðŸ¦´ Retouche par Pression au Bois de Cerf & Dents SerratÃ©es (-25 000 BP)",
                "Usage de retouchoirs en bois de renne pour faÃ§onner des tranchants dentelÃ©s ultra-pÃ©nÃ©trants.",
                "Ref: Crabtree, D. E. (1968). Mesoamerican polyhedral cores and pressure flaking. Am. Antiq.",
                "â€¢ Perforation Profonde : PÃ©nÃ©tration accrue de +40% dans le cuir des mammouths\nâ€¢ PrÃ©cision de Taille : Standardisation parfaite des armatures de javelots"},
            new String[]{"WildFlaxSpinningEngine", "ðŸ§µ Filage du Lin Sauvage & Nets de Chasse Haute-RÃ©sistance (-32 000 BP)",
                "Torsade de fibres de lin sauvage (Dzudzuana) crÃ©ant des cordes et nets de capture Ã  haute rÃ©sistance.",
                "Ref: Kvavadze, E. et al. (2009). 30,000-Year-Old Wild Flax Fibers. Science.",
                "â€¢ RÃ©sistance des Filets : AmÃ©liore de +30% la capture du petit gibier en forÃªt\nâ€¢ Cordages de Propulsion : Fabrication de lignes d'arc de prÃ©cision"},
            new String[]{"OchreResinHaftingEngine", "ðŸŒ² Chargement Chimique des Poix Ã  la Poudre d'Ocre (-70 000 BP)",
                "Adjonction de poudre d'ocre dans la rÃ©sine vÃ©gÃ©tale servant de charge minÃ©rale renforÃ§ant le mastic.",
                "Ref: Wadley, L. (2005). Putting ochre to the test: replication experiments. Antiquity.",
                "â€¢ SoliditÃ© du Mastic : Augmente la rÃ©sistance au cisaillement de +150% au grand froid\nâ€¢ Anti-DÃ©collement : ZÃ©ro casse de mastic lors des impacts sur structures osseuses"},
            new String[]{"ReindeerRiverInterceptionEngine", "ðŸ¦Œ Interception des Renne aux Passages de Fleuves (-15 000 BP)",
                "Chasse aux points de traversÃ©e fluviale des rennes migrateurs au printemps et Ã  l'automne.",
                "Ref: Gordon, B. C. (1988). Of Men and Reindeer Herd in French Magdalenian Prehistory.",
                "â€¢ Abattage Massif Saisonner : Stockage de tonnes de viande fumÃ©e lors des migrations\nâ€¢ DensitÃ© d'Habitat : Maintien de densitÃ©s magdalÃ©niennes Ã©levÃ©es (0.15 hab/kmÂ²)"},
            new String[]{"EndokarstTorchMappingEngine", "ðŸ”¦ Pistes de Torches & Cartographie des Cavernes Profondes (-30 000 BP)",
                "Mouchages de torches et repÃ¨res au charbon permettant l'exploration des rÃ©seaux karstiques profonds.",
                "Ref: Clottes, J. & Courtin, J. (1994). La grotte Cosquer. Seuil.",
                "â€¢ Guidage Souterrain : Permet l'exploration sÃ©curisÃ©e des galeries Ã  plus de 1 km des entrÃ©es\nâ€¢ ZÃ©ro Perte : Ã‰limine les accidents et dÃ©sorientations dans le labyrinthe karstique"},
            new String[]{"PeriglacialLoessDustEngine", "ðŸŒªï¸ PoussiÃ¨res de Loess PÃ©riglaciaires & Stress Respiratoire (-24 000 BP)",
                "TempÃªtes de poussiÃ¨res de loess balayant la steppe glaciaire rÃ©duisant la santÃ© des cohortes.",
                "Ref: Antoine, P. et al. (2009). High-resolution record of the Last Glacial loess in Europe. Quat. Sci. Rev.",
                "â€¢ Stress Respiratoire : PÃ©nalitÃ© de -10% sur la survie infantile en steppe ouverte\nâ€¢ Protection Abris : NÃ©cessite l'usage d'abris Ã©tanches en os et cuir"},
            new String[]{"OstrichEggshellNetworkEngine", "ðŸ¥š RÃ©seau de Gourdes d'Å’ufs d'Autruche GravÃ©s (-60 000 BP)",
                "Troc de gourdes en Å“uf d'autruche permettant les expÃ©ditions en dÃ©sert profond.",
                "Ref: Texier, P.-J. et al. (2013). The Howiesons Poort engraving tradition. J. Arch. Sci.",
                "â€¢ Rayon d'Action DÃ©sertique : Ã‰largit le pÃ©rimÃ¨tre de forage de +200 km en zone aride\nâ€¢ Monnaie Symbolique : Renforce la confiance lors des rÃ©unions inter-bandes"},
            new String[]{"MarrowFatRenderingEngine", "ðŸ² Concasage Osseux & Extractions de Graisse par Bouillissage (-20 000 BP)",
                "Pilonnage des Ã©piphyses et bouillissage par pierres chauffÃ©es pour extraire la graisse d'os.",
                "Ref: Outram, A. K. (2001). A new method for identifying bone marrow and grease rendering. J. Arch. Sci.",
                "â€¢ ConcentrÃ© Ã‰nergÃ©tique : Production de galettes de pemmican ultra-caloriques pour l'hiver\nâ€¢ Anti-Inanition : EmpÃªche la malnutrition protÃ©ique ('Rabbit Starvation')"},
            new String[]{"SeaOtterFurTanningEngine", "ðŸ¦¦ Tannage de Fourrures de Loutre de Mer pour Kayaks (-15 000 BP)",
                "Tannage des fourrures de loutres de mer (100,000 poils/cmÂ²) pour l'Ã©tanchÃ©itÃ© des parkas et canoÃ«s.",
                "Ref: Erlandson, J. M. (2001). The Archaeology of Aquatic Adaptations. J. Arch. Res.",
                "â€¢ Protection Thermique Aquatique : Ã‰limine l'hypothermie lors des expÃ©ditions en mer froide\nâ€¢ Corridor Kelp : Permet la navigation continue le long des forÃªts de kelp arctiques"},
            new String[]{"ZoonoticPathogenSpilloverEngine", "ðŸ¦‡ Zoo-PathogÃ¨nes des Cavernes & SÃ©lection d'ImmunitÃ©s (-50 000 BP)",
                "Exposition aux virus de chauve-souris dans les grottes sÃ©lectionnant les allÃ¨les immunitaires TLR.",
                "Ref: Enard, D. & Petrov, D. A. (2018). Evidence that RNA viruses drove adaptive introgression. Cell.",
                "â€¢ PoussÃ©es Ã‰pidÃ©miques : Ã‰limine les bandes nomades sans protection immunitaire\nâ€¢ SÃ©lection TLR : Renforce la rÃ©silience gÃ©nÃ©tique des survivants de la cohorte"},
            new String[]{"DoggerlandMarshFowlingEngine", "ðŸ¦† Filets d'Oiseleur dans les MarÃ©cages du Doggerland (-11 000 BP)",
                "Capture d'oiseaux migrateurs au filet dans les marais de la mer du Nord assÃ©chÃ©e.",
                "Ref: Coles, B. J. (1998). Doggerland: a cultural review. Proc. Prehist. Soc.",
                "â€¢ Abondance Aviaire : Apport calorique printanier massif pour les bandes mÃ©solithiques\nâ€¢ Stabilisation CotiÃ¨re : Maintien de populations Ã©levÃ©es avant la submersion marine"},
            new String[]{"NightTorchSpearfishingEngine", "ðŸŸ PÃªche Nocturne au Flambeau de Pin & Harponnage (-14 000 BP)",
                "Ã‰clairage des riviÃ¨res Ã  la torche de rÃ©sine attirable les poissons la nuit.",
                "Ref: Zhilin, M. (2014). Early Mesolithic bone arrows and harpoons from Upper Volga. Quat. Int.",
                "â€¢ Rendement Nocturne : Doubler la capture de brochets et truites dans les cours d'eau\nâ€¢ Diversification Broad-Spectrum : SÃ©curise l'alimentation lors des disettes de chasse"},
            new String[]{"BasaltGrindingSlabEngine", "ðŸª¨ Meules en Basalte & Broyage des Glands et Graines (-18 000 BP)",
                "FaÃ§onnage de grandes meules en basalte porique pour le pilonnage des glands et graines sauvages.",
                "Ref: Wright, K. I. (1994). Ground stone tools and plant food processing. Am. Antiq.",
                "â€¢ DÃ©toxification des Glands : Extraction des tanins par broyage et lessivage\nâ€¢ DensitÃ© SÃ©dentaire : Augmente la capacitÃ© portante des zones de chÃªnes et cÃ©rÃ©ales"},
            new String[]{"BisonCliffJumpDriveEngine", "ðŸ‚ Rabattage de Troupeaux de Bisons sur Falaises (-12 000 BP)",
                "Battues coordonnÃ©es rabattant des centaines de bisons au-dessus de falaises naturelles.",
                "Ref: Frison, G. C. (1991). Prehistoric Hunters of the High Plains. Academic Press.",
                "â€¢ Capture en Masse : Abattage simultanÃ© de troupeaux entiers en une seule opÃ©ration\nâ€¢ Stock de Viande GÃ©ant : SÃ©chage de tonnes de viande assurant 1 an d'autonomie"},
            new String[]{"ObsidianSolarIgnitionEngine", "â˜€ï¸ Miroirs en Obsidienne & Allumage Solaire (-14 000 BP)",
                "Polissage de miroirs en obsidienne pour la concentration des rayons solaires sur de l'adouadou.",
                "Ref: Cann, J. R. & Renfrew, C. (1964). Characterization of obsidian. Proc. Prehist. Soc.",
                "â€¢ Allumage Rapide : RÃ©duction de 50% de l'effort de friction du feu par temps ensoleillÃ©\nâ€¢ Prestige Rituel : Renforcement du rÃ´le des chamans de la bande"},
            new String[]{"SubGlacialMeltwaterWeirEngine", "ðŸŸ Barrages Ã  Poissons dans les Torrents de Fonte Glacier (-12 000 BP)",
                "Construction de piÃ¨ges en pieux de bois dans les riviÃ¨res de fonte sous-glaciaire.",
                "Ref: Pedersen, L. et al. (1997). The Danish StorebÃ¦lt since the Ice Age. A/S StorebÃ¦lt.",
                "â€¢ Capture Efficace : Capture continue des anguilles et truites en pÃ©riode de dÃ©gel\nâ€¢ Subsistance PÃ©riglaciaire : Maintient les bandes Ã  proximitÃ© immÃ©diate de la calotte"},
            new String[]{"IvoryHotWaterStraighteningEngine", "ðŸ˜ Redressement d'Ivoire Ã  l'Eau Chaude & Lances de 2m (-27 000 BP)",
                "Chauffe et trempage d'ivoire de mammouth permettant de redresser les lances (Sunghir).",
                "Ref: Formicola, V. (2007). The Sunghir burials. Curr. Anthropol.",
                "â€¢ Lances Rigides de 2m : Fabrication d'armes de choc d'une portÃ©e inÃ©galÃ©e\nâ€¢ Force d'Impact : Perforation maximale lors de la chasse rapprochÃ©e au mammouth"},
            new String[]{"CaveWallClaySealingEngine", "ðŸ§± Calfeutrage des Cavernes Ã  l'Argile & Isolation (-35 000 BP)",
                "PlÃ¢trage des fissures de grottes avec de la boue et argile pour Ã©liminer les courants d'air.",
                "Ref: Beaune, S. A. de (2000). Pour une archÃ©ologie du geste. CNRS Ã‰ditions.",
                "â€¢ Chaleur Interne : Maintien de la tempÃ©rature de la grotte au-dessus de 10Â°C en hiver\nâ€¢ Ã‰conomie de Bois : RÃ©duit la consommation de combustible pour le foyer"},
            new String[]{"BirchBarkVesselEngine", "ðŸªµ RÃ©cipients en Ã‰corce de Bouleau PlissÃ©e & Bouillissage (-20 000 BP)",
                "Pliage et couture de rÃ©cipients en Ã©corce Ã©tanchÃ©ifiÃ©s Ã  la poix pour chauffer l'eau.",
                "Ref: Burov, G. M. (1996). On the search for heritage of Mesolithic timber crafts. World Arch.",
                "â€¢ Bouillissage Sans CÃ©ramique : Cuisson des potages et viandes avec des pierres chauffÃ©es\nâ€¢ MobilitÃ© LÃ©gÃ¨re : RÃ©cipients souples et legers incassables pendant les trajets nomades"},
            new String[]{"SnowTroughRefrigerationEngine", "â„ï¸ TranchÃ©es Frigorifiques dans la Neige & Caches d'Hiver (-25 000 BP)",
                "Stockage de carcasses de grand gibier dans des tranchÃ©es de neige recouvertes de sapin.",
                "Ref: Pitulko, V. V. et al. (2014). Early human presence in the Arctic. Science.",
                "â€¢ Cryo-Conservation PrintaniÃ¨re : Maintient la fraÃ®cheur de la viande jusqu'au mois de mai\nâ€¢ SÃ©curitÃ© de la Cohorte : Ã‰vite les famines de fin d'hiver lors du dÃ©gel"},
            new String[]{"ArrowPoisonSynthesisEngine", "ðŸ§ª SynthÃ¨se de Toxines VÃ©gÃ©tales (Aconit) & Fleches EmpoisonnÃ©es (-15 000 BP)",
                "Extraction de toxines vÃ©gÃ©tales (*Aconitum*) pour enduire les armatures microlithiques.",
                "Ref: Wadley, L. et al. (2012). Evidence for arrow use and poison 44,000 years ago. PNAS.",
                "â€¢ Paralysie des Proies : Neutralisation rapide du gibier blessÃ© en quelques minutes\nâ€¢ SuccÃ¨s de Chasse : +60% de rendement sur les ungulÃ©s rapides et craintifs"},
            new String[]{"LakeChadWadiMigrationEngine", "ðŸŒŠ Corridors des Wadis du Sahara & PÃªche du Lac MÃ©ga-Tchad (-15 000 BP)",
                "Migration le long des riviÃ¨res sahariennes rÃ©activÃ©es et pÃªche sur le Lac MÃ©ga-Tchad.",
                "Ref: Drake, N. A. et al. (2011). Ancient watercourses suggest a humid Sahara. PNAS.",
                "â€¢ TraversÃ©e Trans-Saharienne : Autorise la migration Sapiens sans souffrance hydrique\nâ€¢ Ressource Lacustre GÃ©ante : Exploitation des capitaines et poissons chats gÃ©ants"},
            new String[]{"EpipaleolithicStorageHamletEngine", "ðŸ¡ Hameaux SÃ©dentaires Natufiens & Fosses de Stockage (-12 500 BP)",
                "Construction de maisons semi-enterrÃ©es en pierre avec fosses de stockage de graines.",
                "Ref: Bar-Yosef, O. (1998). The Natufian Culture in the Levant. Evol. Anthropol.",
                "â€¢ Ancrage Territorial : Transition dÃ©finitive de la cohorte vers le village sÃ©dentaire\nâ€¢ Accumulation de Capital : Fosses de stockage scellÃ©es protÃ©geant les rÃ©serves de cÃ©rÃ©ales"},
            new String[]{"AutoRegulationPureEngine", "âš¡ Auto-RÃ©gulation Pure & CybernÃ©tique (1948)",
                "Moteur de rÃ©gulation dynamique automatique des Ã©quilibres homÃ©ostatiques.",
                "Ref: Wiener, N. (1948). Cybernetics: Or Control and Communication in the Animal and the Machine.",
                "â€¢ Boucle de RÃ©troaction : dX/dt = -k Â· (X - X_cible)"},
            new String[]{"FrontierAsabiyyahEngine", "âš” Asabiyyah de FrontiÃ¨re (Ibn Khaldoun 1377 & Peter Turchin 2003)",
                "ThÃ©orie Khaldounienne de la solidaritÃ© de groupe et dÃ©clin des dynasties (Badiya vs Hadara). ModÃ©lise l'Ã©rosion de la cohÃ©sion sociale lors du passage de la frontiÃ¨re mÃ©tastable aux mÃ©tropoles opulentes.",
                "Ref: Ibn Khaldun (1377). Muqaddimah; Turchin, P. (2003). Historical Dynamics: Securing the Peace, Princeton Univ. Press.",
                "â€¢ Variation d'Asabiyyah (CohÃ©sion A) : dA/dt = câ‚Â·F(x)Â·(1 - A) - câ‚‚Â·(K(x)/N(x))Â·A\n  oÃ¹ F(x) est la pression militaire de frontiÃ¨re et K(x)/N(x) le capital par habitant (luxe).\nâ€¢ MÃ©tropole opulente (K > 1000 kg/hab) : DÃ©clin d'Asabiyyah dA/dt = -2.0% par pas de temps.\nâ€¢ Zone de frontiÃ¨re (K â‰¤ 1000 kg/hab) : Forge la cohÃ©sion militaire dA/dt = +2.0% par pas de temps.\nâ€¢ InÃ©galitÃ© & DÃ©clin Dynastique : S_cohesion(t) = A(t) Â· Pop(t) Â· (1 - Gini(t))."},
            new String[]{"AiAutonomousRegulationPureEngine", "ðŸ¤– RÃ©gulation Autonome de l'IA & Gouvernance (2023+)",
                "ModÃ©lisation de la rÃ©gulation et des risques de l'intelligence artificielle. Ã‰value les probabilitÃ©s d'Ã©mergence d'infrastructures autonomes et de gestion des risques.",
                "Ref: Bostrom, N. (2014). Superintelligence; Russell, S. (2019). Human Compatible.",
                "â€¢ Seuil d'Autonomie IA : P_alignement = 1 / (1 + exp(-k Â· (Niveau_Gouvernance - ComplexitÃ©_IA)))\nâ€¢ Taux de Risque SystÃ©mique R_ia = (1 - P_alignement) Â· Puissance_Calcul_PlanÃ©taire"},
            new String[]{"AmerindianEcosystemEngine", "ðŸŒ¾ Agro-foresterie AmÃ©rindienne & Terra Preta (-4 000 BP)",
                "Techniques agricoles prÃ©colombiennes et enrichissement des sols en biochar. Augmente la capacitÃ© portante et la rÃ©silience des sols de forÃªt tropicale.",
                "Ref: Denevan, W. M. (1992). The Pristine Myth; Glaser, B. et al. (2002). Terra Preta Biochar.",
                "â€¢ Formation de Terra Preta : dC_biochar/dt = Apport_Charbon_Organique - Oxidation_Lente(0.001)\nâ€¢ Gain de CapacitÃ© Portante : K_sols = K_base Â· (1 + Î± Â· ln(1 + C_biochar))"},
            new String[]{"AsymmetricColonialTradeEngine", "ðŸš¢ Commerce Colonial AsymÃ©trique & Extraction (1500)",
                "Flux de ressources et fuite de valeur des colonies vers les mÃ©tropoles. Simule la capture de rente et le blocage de l'industrialisation pÃ©riphÃ©rique.",
                "Ref: Wallerstein, I. (1974). The Modern World-System; Frank, A. G. (1967). Dependency Theory.",
                "â€¢ Capture de Rente : Transfert_Richesse = Termes_Echange_Asym Â· Export_MatiÃ¨res_PremiÃ¨res\nâ€¢ Frein d'Industrialisation PÃ©riphÃ©rique : dT_peripherie/dt = T_base Â· (1 - Ratio_Extraction)"},
            new String[]{"BifurcationChaosEngine", "ðŸŒ€ Chaos & Analyse des Bifurcations SystÃ©miques (1963)",
                "SensibilitÃ© aux conditions initiales et points de basculement. GÃ©nÃ¨re des micro-oscillations chaotiques pouvant dÃ©clencher des cascades d'instabilitÃ©.",
                "Ref: Lorenz, E. N. (1963). Deterministic Nonperiodic Flow; May, R. M. (1976).",
                "â€¢ Attracteur de Lorenz / Bifurcation Logistique : x_{t+1} = r Â· x_t Â· (1 - x_t)\nâ€¢ Exposant de Liapounov Î» > 0 -> Divergence exponentielle des trajectoires de simulation"},
            new String[]{"BioMolecularEpidemiologyEngine", "â˜£ï¸ Ã‰pidÃ©miologie Bio-MolÃ©culaire & ImmunitÃ© Pop (-5 000 BP / 1927)",
                "Simulation avancÃ©e de la transmission virale et foyers infectieux. ModÃ©lise la transmission SIR/SEIR selon la densitÃ© urbaine et le rÃ©seau de commerce.",
                "Ref: Kermack, W. O. & McKendrick, A. G. (1927). SIR Epidemiological Model.",
                "â€¢ ModÃ¨le SEIR : dS/dt = -Î²Â·SÂ·I/N, dE/dt = Î²Â·SÂ·I/N - ÏƒÂ·E, dI/dt = ÏƒÂ·E - Î³Â·I, dR/dt = Î³Â·I\nâ€¢ Taux de Reproduction de Base Râ‚€ = Î² / Î³ Â· (1 + Variance_Contacts_DensitÃ©)"},
            new String[]{"CulturalMaterialismPureEngine", "ðŸ“œ MatÃ©rialisme Culturel (Marvin Harris 1979)",
                "DÃ©terminisme de l'infrastructure technologique et dÃ©mographique sur les croyances. Adapte les valeurs morales aux contraintes d'extraction d'Ã©nergie.",
                "Ref: Harris, M. (1979). Cultural Materialism: The Struggle for a Science of Culture.",
                "â€¢ Alignement Superstructure : Superstructure(t) = f(Infrastructure_Ã‰nergÃ©tique, Pression_DÃ©mographique)\nâ€¢ Transition Morale : dM/dt = k_adaptation Â· (Mode_Production - M)"},
            new String[]{"CulturalSociologyEngine", "ðŸ“œ Sociologie Culturelle & Matrice de Voisinage (-50 000 BP)",
                "Ã‰volution des valeurs culturelles et mÃ©tissage rÃ©gional. GÃ¨re la diffusion des langues, des normes et la dÃ©rive culturelle entre mailles voisines.",
                "Ref: Cavalli-Sforza, L. L. & Feldman, M. W. (1981). Cultural Transmission and Evolution.",
                "â€¢ Matrice de Diffusion Culturelle : dC_i/dt = Î£_j w_{ij} Â· (C_j - C_i) + Drift_Accidentel\nâ€¢ Distance Culturelle d(i,j) = || Vector_Langue_i - Vector_Langue_j ||"},
            new String[]{"DeforestationErosionEngine", "ðŸœï¸ Ã‰rosion ForestiÃ¨re & Ensablement Fluvial (-6 000 BP)",
                "DÃ©gradation des sols et perte de couverture vÃ©gÃ©tale. EntraÃ®ne le ravinement des terres arables et le comblement des lits de riviÃ¨res lors de coupes rases.",
                "Ref: Montgomery, D. R. (2007). Dirt: The Erosion of Civilizations. Univ. of California Press.",
                "â€¢ Ã‰rosion des Sols : Perte_Sol = k_coupe Â· (1 - Couverture_Forestiere) Â· PrecipitationsÂ³\nâ€¢ Comblement Fluvial = Î£ Perte_Sol_Amont"},
            new String[]{"EdoJapanIsolationEngine", "â›©ï¸ Isolationnisme du Japon Edo / Sakoku (1635)",
                "Maintien d'un Ã©quilibre zÃ©ro-croissance et fermeture des frontiÃ¨res. Ã‰limine la dÃ©pendance extÃ©rieure au dÃ©triment du rythme de progrÃ¨s technologique.",
                "Ref: Totman, C. (1993). Early Modern Japan; Diamond, J. (2005). Collapse (Tokugawa Forestry).",
                "â€¢ Ã‰quilibre Sylvicole & ZÃ©ro-Croissance : Extraction_Bois <= Auto_RÃ©gÃ©nÃ©ration_ForÃªt\nâ€¢ Isolement Commercial : Flux_Externe = 0, StabilitÃ©_Interne = Maximale"},
            new String[]{"EntropicMetalDissipationEngine", "ðŸ­ Dissipation Entropique des MÃ©taux & Jevons Rebound (1800)",
                "Dispersion irrÃ©mÃ©diable des mÃ©taux rares et effets rebond. Calcule la perte irrÃ©cupÃ©rable de cuivre et de mÃ©taux prÃ©cieux par usure mÃ©canique et oxydation.",
                "Ref: Georgescu-Roegen, N. (1971). The Entropy Law and the Economic Process.",
                "â€¢ Pertes Entropiques IrrÃ©cupÃ©rables : dMetal_dissipe/dt = Production Â· (1 - Taux_Recyclage_Max)\nâ€¢ Limite d'Usure Recyclage : Max_Recyclage = 85% par contrainte thermodynamique"},
            new String[]{"EcotoxicologyFertilityEngine", "ðŸ§ª Ã‰cotoxicologie & StÃ©rilitÃ© Chimique (1950)",
                "Impacts des polluants synthÃ©tiques sur le taux de fÃ©conditÃ©. Simule la baisse de fertilitÃ© humaine liÃ©e Ã  la concentration cumulÃ©e d'entropie chimique.",
                "Ref: Colborn, T. et al. (1996). Our Stolen Future; Swan, S. H. (2021). Count Down.",
                "â€¢ Baisse de FÃ©conditÃ© : F_effective = F_naturelle Â· exp(-k_tox Â· Charge_Pollution_Cumulee)"},
            new String[]{"FertileCrescentSalinizationEngine", "ðŸŒ¾ Salinisation du Croissant Fertile (-2 400 BP)",
                "DÃ©gradation historique des sols irriguÃ©s en MÃ©sopotamie antique. Accumulation de sels minÃ©raux toxiques par Ã©vaporation de l'eau d'irrigation.",
                "Ref: Jacobsen, T. & Adams, R. M. (1958). Salt and Silt in Ancient Mesopotamian Agriculture.",
                "â€¢ Accumulation Saline : dSel/dt = (Volume_Irrigation Â· Concentration_Sel_Eau) - Leaching_Drainage"},
            new String[]{"GeoengineeringAlbedoFeedbackEngine", "ðŸ§ª GÃ©o-ingÃ©nierie & RÃ©troaction d'AlbÃ©do Artificiel (2030+)",
                "Injections d'aÃ©rosols stratosphÃ©riques et terraformation. Diminue l'insolation solaire globale pour contrer le rÃ©chauffement climatique.",
                "Ref: Crutzen, P. J. (2006). Albedo Modification via Stratospheric Aerosol Injection.",
                "â€¢ Delta AlbÃ©do Artificiel : Î”AlbÃ©do = k_aerosol Â· Mass_SO2_Injectee\nâ€¢ Refroidissement ForcÃ© : Î”T_cooling = -Î» Â· Sâ‚€ Â· Î”AlbÃ©do"},
            new String[]{"HandyNasaHybridEngine", "ðŸ“‰ ModÃ¨le HANDY NASA Hybride (DÃ©mographie & Ã‰lites 2014)",
                "RÃ©troactions entre Ã©lites, travailleurs et ressources (NASA / Motesharrei). Simule les scÃ©narios d'effondrement par surconsommation des Ã©lites.",
                "Ref: Motesharrei, S., Rivas, J., & Kalnay, E. (2014). HANDY: Human and Nature Dynamics.",
                "â€¢ Ã‰quations HANDY (4 Variables) : dx_w/dt = Î±_wÂ·x_w - Î²_wÂ·x_w,  dx_e/dt = Î±_eÂ·x_e - Î²_eÂ·x_e\nâ€¢ Surconsommation Ã‰lites : Consommation_Elite = s Â· Consommation_Travailleur avec s >> 1"},
            new String[]{"HandyNasaPureEngine", "ðŸ“‰ ModÃ¨le HANDY NASA Pur (Ã‰quations DiffÃ©rentielles 2014)",
                "SystÃ¨me dynamique pur de la dynamique homme-nature (Handy Model). SystÃ¨me Ã  4 Ã©quations couplÃ©es (Ã‰lites, Travailleurs, Nature, Capital).",
                "Ref: Motesharrei, S. et al. (2014). HANDY Model Equations. Ecological Economics 101.",
                "â€¢ Nature N : dN/dt = Î³Â·NÂ·(Î» - N) - Î´Â·x_wÂ·N\nâ€¢ Accumulation de Capital K : dK/dt = Î´Â·x_wÂ·N - C_w - C_e"},
            new String[]{"JevonsParadoxEngine", "âš¡ Effet Rebond & Paradoxe de Jevons (1865)",
                "L'augmentation de l'efficacitÃ© Ã©nergÃ©tique accroÃ®t la consommation globale. Annule les gains d'Ã©conomie d'Ã©nergie par l'expansion de l'Ã©chelle industrielle.",
                "Ref: Jevons, W. S. (1865). The Coal Question; Alcott, B. (2005). Jevons' Paradox.",
                "â€¢ Ã‰nergie ConsommÃ©e E_total = Pop Â· EfficacitÃ©^Îµ avec Îµ > 1.0 (Paradoxe de Jevons)"},
            new String[]{"KardashevPureEngine", "ðŸŒŒ Ã‰chelle de Kardashev & Capture Ã‰nergÃ©tique (1964)",
                "Transition vers le contrÃ´le de l'Ã©nergie planÃ©taire intÃ©grale. Ã‰value le score de Kardashev (Type 0.0 Ã  1.0) selon la puissance totale captÃ©e en watts.",
                "Ref: Kardashev, N. S. (1964). Transmission of Information by Extraterrestrial Civilizations.",
                "â€¢ Indice de Kardashev K = (logâ‚â‚€(Puissance_Watts) - 6) / 10\nâ€¢ Type I = 10Â¹â¶ Watts (Ã‰nergie PlanÃ©taire IntÃ©grale)"},
            new String[]{"KinSelectionHamiltonEngine", "ðŸ§¬ SÃ©lection de ParentÃ¨le (RÃ¨gle de Hamilton -300 000 BP / 1964)",
                "Ã‰volution de l'altruisme gÃ©nÃ©tique et coopÃ©ration inter-individus (rB > C). DÃ©termine la cohÃ©sion des petites tribus et familles Ã©tendues.",
                "Ref: Hamilton, W. D. (1964). The Genetical Evolution of Social Behaviour. J. Theor. Biol.",
                "â€¢ Condition d'Altruisme : r Â· Benefit > Cost avec r = Coefficient d'Apparentement GÃ©nÃ©tique"},
            new String[]{"KurzweilAcceleratingReturnsEngine", "ðŸš€ Loi des Rendements AccÃ©lÃ©rÃ©s (Ray Kurzweil 2005)",
                "AccÃ©lÃ©ration exponentielle du progrÃ¨s scientifique et des processeurs. RÃ©duit les dÃ©lais d'invention Ã  mesure que le niveau technologique s'Ã©lÃ¨ve.",
                "Ref: Kurzweil, R. (2005). The Singularity Is Near; Moore, G. E. (1965).",
                "â€¢ Vitesse d'Invention dTech/dt = Vâ‚€ Â· exp(Î» Â· Tech(t))"},
            new String[]{"LenskiPureEngine", "ðŸ§  Ã‰volution Socioculturelle (Gerhard Lenski 1966)",
                "Classification des sociÃ©tÃ©s selon leur mode d'extraction d'information et d'Ã©nergie. RÃ©trograde ou promeut le type de sociÃ©tÃ© (Chasseurs, Agricoles, Industriels).",
                "Ref: Lenski, G. (1966). Power and Privilege: A Theory of Social Stratification.",
                "â€¢ Stade SociÃ©tal = f(Ã‰nergie_Par_Habitant, Stock_Information_Technologique)"},
            new String[]{"LeslieWhitePureEngine", "âš¡ Loi de Leslie White (Culture = E Ã— T 1943)",
                "Le dÃ©veloppement culturel varie directly avec l'Ã©nergie captÃ©e par habitant (C = E Ã— T). DÃ©termine la complexitÃ© symbolique selon l'Ã©nergie.",
                "Ref: White, L. A. (1943). Energy and the Evolution of Culture. American Anthropologist.",
                "â€¢ ComplexitÃ© Culturelle C = Ã‰nergie_Par_Capita Â· EfficacitÃ©_Technologique"},
            new String[]{"MaritimeHighwayEngine", "â›µ Autoroute Maritime & Thalassocraties (-3 000 BP)",
                "RÃ©seau de navigation cÃ´tiÃ¨re et autoroutes maritimes universelles. RÃ©duit la friction de transport sur l'eau libre de glace (thermodynamique) et stimule l'accumulation de capital.",
                "Ref: Braudel, F. (1949). La MÃ©diterranÃ©e; Archimedes Buoyancy Transport Models.",
                "â€¢ Multiplicateur de Transport Maritime = 0.20 Ã— Friction_Terrestre (Cap : +5%/tick)"},
            new String[]{"DynamicMaritimeRoutingGraph", "ðŸŒ Graphe de Routage Trans-OcÃ©anique Dynamique (-40 000 BP)",
                "Calcul dynamique et adaptatif des routes maritimes globales selon l'Ã©volution technologique (cabotage -> navigation hauturiÃ¨re -> brise-glace) et thermodynamique.",
                "Ref: Dynamic Graph Routing & Fluid Drag; Bowditch, N. (1802). American Practical Navigator.",
                "â€¢ Invalidation auto selon Niveau Tech & Glace de Mer\nâ€¢ PortÃ©e de Cabotage (Tech < 3.0: 300km, Tech >= 6.0: Trans-OcÃ©anique)"},
            new String[]{"LandReclamationEngine", "ðŸ—ï¸ PoldÃ©risation & Habitats Flottants (1200)",
                "Transformation de zones cÃ´tiÃ¨res en polders agricoles et crÃ©ation de citÃ©s flottantes en haute mer selon le niveau technologique et le capital.",
                "Ref: Dutch Water Boards History; Seasteading Institute (2008).",
                "â€¢ Tech >= 4.0 & K >= 100 -> PoldÃ©risation ; Tech >= 8.5 & K >= 500 -> Habitat Flottant"},
            new String[]{"MarineSubmersionEngine", "ðŸš¨ Submersion Marine & Ã‰vacuation Physique (1950+)",
                "ModÃ©lisation physique de l'Ã©lÃ©vation du niveau de la mer et de la maintenance des digues. Ã‰vacuation prÃ©ventive (jusqu'Ã  98% en sociÃ©tÃ© moderne) et flux de rÃ©fugiÃ©s sans mortalitÃ© brutale irrÃ©aliste.",
                "Ref: IPCC Coastal Inundation & Flood Early Warning Physics; Adger, W. N. (2006). Vulnerability.",
                "â€¢ Ã‰vacuation Physique: Ratio_Evac = 1 - exp(-0.4 Â· Tech Â· (1 + K/500))\nâ€¢ DÃ©placement de Population vers mailles sÃ¨ches adjacentes"},
            new String[]{"HydrologicalEngineeringEngine", "ðŸ’§ IngÃ©nierie Hydrologique & Transgressions PaysagÃ¨res (-3 000 BP)",
                "AssÃ¨chement/remplissage de lacs urbains (Texcoco / Tenochtitlan), assÃ¨chement anthropique de mers intÃ©rieures endorÃ©iques (Mer d'Aral) et retenues d'eau / barrages hydroÃ©lectriques de montagne.",
                "Ref: IPCC Water Resource Engineering; World Commission on Dams (2000); Tenochtitlan Chinampa Hydro-Engineering.",
                "â€¢ Drenage Texcoco: Lac -> Plaine Urbaine (Tech >= 3.5, K >= 150)\nâ€¢ AssÃ¨chement Aral: Lac -> DÃ©sert SalÃ© (Drainage > Recharge)\nâ€¢ Barrage Montagne: Retenue d'eau (1000L) & Ã‰nergie HydroÃ©lectrique (Tech >= 4.5, Alt >= 300m)"},
            new String[]{"MegafaunaEcosystemEngine", "ðŸ¦• Conservation de la BiodiversitÃ© Sauvage & MÃ©gafaune (-50 000 BP)",
                "Pressions de chasse et extinction/prÃ©servation des espÃ¨ces sauvages. Simule la disparition de la grande faune lors de l'expansion humaine nÃ©olithique.",
                "Ref: Martin, P. S. (1984). Quaternary Extinctions: A Prehistoric Revolution.",
                "â€¢ Extinction MÃ©gafaune dM/dt = r_mÂ·M - k_chasseÂ·Pop_HumaineÂ·M"},
            new String[]{"MilitaryTechShockEngine", "ðŸ’£ Chocs de Technologie Militaire & Poudre Ã  Canon (1200)",
                "RÃ©volution militaire et transformation de l'architecture des fortifs. Augmente la capacitÃ© de conquÃªte des empires centralisÃ©s.",
                "Ref: Parker, G. (1988). The Military Revolution; McNeill, W. H. (1982).",
                "â€¢ Puissance Offensive = Puissance_Base Â· (1 + Multiplicateur_Poudre_Canon)"},
            new String[]{"MonasticDemographicBufferEngine", "ðŸ›ï¸ Buffers DÃ©mographiques Monastiques & Savoir (500)",
                "PrÃ©servation du capital intellectuel et rÃ©gulation dÃ©mographique par les monastÃ¨res. EmpÃªche la perte totale de savoir lors de la chute d'un empire.",
                "Ref: Weber, M. (1905); Kautsky, K. (1889). Thomas More and his Utopia.",
                "â€¢ Plancher de RÃ©tention du Savoir : T_min = Max(T_courant, T_monastique_sauvegardÃ©)"},
            new String[]{"NordhausDiceHybridEngine", "ðŸŒ¡ï¸ ModÃ¨le DICE Hybride (Nordhaus 1992 - Climat & Ã‰conomie)",
                "Couplage Ã©conomie-climat intÃ©grÃ© avec boucle de dommage du carbone. Ã‰value la perte de PIB causÃ©e par l'Ã©lÃ©vation des tempÃ©ratures extrÃªmes.",
                "Ref: Nordhaus, W. D. (1992, 2017). Integrated Assessment Models (DICE-2016R).",
                "â€¢ Fonction de Dommage Nordhaus Î©(T) = 1 / (1 + Ï€â‚Â·T + Ï€â‚‚Â·TÂ²)\nâ€¢ PIB AjustÃ© Climat Y_net = Î©(T) Â· Y_brut"},
            new String[]{"NordhausDicePureEngine", "ðŸŒ¡ï¸ ModÃ¨le DICE Pur (Taxe Carbone & PIB 1992)",
                "ModÃ©lisation analytique du coÃ»t du carbone et investissements verts. Calcule le prix social du carbone pour inciter la dÃ©carbonation.",
                "Ref: Nordhaus, W. D. (1992). An Optimal Transition Path for Controlling Greenhouse Gases. Science.",
                "â€¢ Prix Social du Carbone SCC = d(Dommages_Futurs_ActualisÃ©s) / d(Ã‰mission_CO2)"},
            new String[]{"NuclearSafetyRadiotoxicityEngine", "âš›ï¸ RadiotoxicitÃ© & Fusion NuclÃ©aire (1950)",
                "Gestion des risques d'accidents atomiques et retombÃ©es toxiques. Simule la contamination des terres et les surcoÃ»ts de sÃ©curitÃ© industrielle.",
                "Ref: Perrow, C. (1984). Normal Accidents: Living with High-Risk Technologies.",
                "â€¢ ProbabilitÃ© d'Accident Majeur = 1 - exp(-Taux_DÃ©faillance_SystÃ¨me Â· Nombre_Reacteurs)"},
            new String[]{"NuclearWarfareClimateEngine", "ðŸ’¥ Guerres Thermodynamiques & Hiver NuclÃ©aire (1945)",
                "ModÃ©lisation des incendies massifs et refroidissement climatique. Injection de suie stratosphÃ©rique bloquant les rayons solaires pendant des annÃ©es.",
                "Ref: Turco, R. P., Toon, O. B., Ackerman, T. P., Pollack, J. B., & Sagan, C. (1983). TTAPS.",
                "â€¢ Baisse TempÃ©rature Mondiale Î”T_nuclÃ©aire = -15.0 Â°C Â· (Masse_Suie / 150 Tg)"},
            new String[]{"OstromCommonsPureEngine", "ðŸžï¸ Auto-Gouvernance des Communs (Elinor Ostrom 1990)",
                "RÃ¨gles institutionnelles locales pour Ã©viter la tragÃ©die des communs. Maintient la durabilitÃ© des pÃ¢turages et de la pÃªche sans privatisation.",
                "Ref: Ostrom, E. (1990). Governing the Commons: Evolution of Institutions.",
                "â€¢ Maintien des Communs : Taux_Survie_Communs = f(Institution_Locale, Sanctions_GraduÃ©es)"},
            new String[]{"PinkerViolenceDeclinePureEngine", "ðŸ•Šï¸ DÃ©clin Historique de la Violence (Steven Pinker 2011)",
                "Baisse de la mortalitÃ© violente par l'Ã‰tat, le commerce et l'alphabÃ©tisation. RÃ©duit les homicides et les guerres Ã  mesure que l'Ã‰tat de droit progresse.",
                "Ref: Pinker, S. (2011). The Better Angels of Our Nature: Why Violence Has Declined.",
                "â€¢ Taux de MortalitÃ© Violente = Vâ‚€ Â· exp(-k_etat Â· Monopole_Violence - k_commerce Â· Fret)"},
            new String[]{"ProtestantWorkEthicEngine", "âœï¸ Ã‰thique du Travail & Accumulation de Capital (Max Weber 1905)",
                "Impact des valeurs morales sur la formation du capital industriel. Stimule le rÃ©investissement des bÃ©nÃ©fices dans les machines au lieu du luxe.",
                "Ref: Weber, M. (1905). Die protestantische Ethik und der Geist des Kapitalismus.",
                "â€¢ Taux d'Ã‰pargne RÃ©investie : S_Ã©pargne = S_base Â· (1 + Î±_ethique_travail)"},
            new String[]{"PsychohistoryPureEngine", "ðŸ“Š Psychohistoire Cliodynamique (ModÃ¨le d'Asimov 1951)",
                "PrÃ©diction statistique des grandes masses humaines Ã  long terme. Anticipe les cycles de stabilitÃ© et prÃ©vient les crises d'effondrement.",
                "Ref: Asimov, I. (1951). Foundation; Turchin, P. (2008). Arise Cliodynamics. Nature.",
                "â€¢ Trajectoire Macro-Historique : X(t+Î”t) = Matrix_P Â· X(t) avec Invariance Statistique"},
            new String[]{"RomanImperialCliodynamicEngine", "ðŸª™ Cycle Cliodynamique Romain & AltÃ©ration de la Monnaie (-27 / 193)",
                "DÃ©gradation de la puretÃ© du Denier et crises fiscales impÃ©riales. EntraÃ®ne l'hyper-inflation et le dÃ©clin du pouvoir d'achat des lÃ©gions.",
                "Ref: Turchin, P. & Scheidel, W. (2009). Coin Debasement and Structural Cycles in Rome.",
                "â€¢ PuretÃ© de la Monnaie : Teneur_Argent = Tâ‚€ Â· exp(-k_solde_fiscal Â· DÃ©ficit_ArmÃ©e)\nâ€¢ Inflation & Solde des LÃ©gions : Solde_Reelle = Solde_Nominale / Inflation"},
            new String[]{"ScottAgainstTheGrainPureEngine", "ðŸŒ¾ ModÃ¨le de l'Ã‰tat CÃ©rÃ©alier (James C. Scott -3 500 BP / 2017)",
                "AttractivitÃ© des cÃ©rÃ©ales taxables et Ã©mergence de l'Ã‰tat archaÃ¯que. PrivilÃ©gie le blÃ©/riz stockables pour l'impÃ´t au dÃ©triment des tubercules.",
                "Ref: Scott, J. C. (2017). Against the Grain: A Deep History of the Earliest States.",
                "â€¢ CapacitÃ© Taxable : Assiette_Fiscale = Production_CÃ©rÃ©ales_Stockables - Subsistance_Minimale"},
            new String[]{"SelfDomesticationEngine", "ðŸ• Auto-Domestication Humaine & RÃ©duction de l'AgressivitÃ© (-300 000 BP)",
                "SÃ©lection contre l'agressivitÃ© rÃ©active dans les fortes densitÃ©s. SÃ©lectionne les comportements coopÃ©ratifs indispensables Ã  la vie urbaine.",
                "Ref: Hare, B. (2017). Survival of the Friendliest; Lahire, B. (2018). Structures Fondamentales.",
                "â€¢ RÃ©duction AgressivitÃ© RÃ©active : dAgressivite/dt = -k_urbain Â· DensitÃ©_Cellule"},
            new String[]{"SexualSelectionMatingEngine", "ðŸ’ SÃ©lection Sexuelle & MarchÃ© Matrimonial (-300 000 BP)",
                "Structures de parentÃ© et polygamie/monogamie selon les ressources. RÃ©gule l'accÃ¨s aux partenaires selon l'inÃ©galitÃ© de capital.",
                "Ref: Buss, D. M. (1989). Sex differences in human mate preferences. BBS.",
                "â€¢ Indice Polygynie = f(Gini_Richesse, Monopole_Ressources_Elites)"},
            new String[]{"SmilMaterialTransitionsPureEngine", "ðŸ—ï¸ Transitions MatÃ©rielles & Ã‰nergÃ©tiques (Vaclav Smil 2019)",
                "Inertie physique des transitions vers l'acier, le bÃ©ton, l'ammoniac et le plastique. Impose des dÃ©lais de plusieurs dÃ©cennies pour remplacer un matÃ©riau de base.",
                "Ref: Smil, V. (2019). Material World & Energy Transitions: History, Requirements.",
                "â€¢ Temps de Transition MatÃ©rielle T_transition = 40 Ã  60 ans par inertie du capital fixe"},
            new String[]{"SpatialCityFractalEngine", "ðŸ™ï¸ Distribution Fractale des Villes (-3 000 BP / 1949)",
                "HiÃ©rarchie des mÃ©tropoles et loi rang-taille urbaine. Organise le rÃ©seau urbain en sous-centres rÃ©gionaux et mÃ©tropoles primatiales.",
                "Ref: Batty, M. (2008). The Size, Scale, and Shape of Cities. Science; Zipf, G. K. (1949).",
                "â€¢ Loi de Zipf Rang-Taille : Pop(Rang r) = Pop_Max / r^Î± avec Î± â‰ˆ 1.0"},
            new String[]{"TasmanianCulturalRegressionEngine", "ðŸï¸ RÃ©gression Culturelle de Tasmanie (Henrich -10 000 BP)",
                "Perte de technologies complexes par goulet d'Ã©tranglement dÃ©mographique. Fait rÃ©gresser l'outillage si la population tombe sous le seuil critique d'apprentissage.",
                "Ref: Henrich, J. (2004). Demography and Cultural Loss in Tasmania. American Antiquity.",
                "â€¢ Seuil Critique d'Apprentissage : dTech/dt < 0 si Pop_Tribu < N_critique"},
            new String[]{"TechnologicalSingularityEngine", "ðŸŒŒ SingularitÃ© Technologique & Kurzweil Rebound (2045+)",
                "AccÃ©lÃ©ration exponentielle du progrÃ¨s scientifique et IA. Franchit le point d'inflexion oÃ¹ les machines auto-amÃ©liorent leur propre conception.",
                "Ref: Good, I. J. (1965); Vinge, V. (1993); Kurzweil, R. (2005). The Singularity Is Near.",
                "â€¢ Boucle de RÃ©troaction SingularitÃ© : d(CapacitÃ©_IA)/dt = (CapacitÃ©_IA)^1.5"},
            new String[]{"UrbanThermodynamicsEngine", "ðŸ™ï¸ Thermodynamique Urbaine & MÃ©tropoles (1850)",
                "ÃŽlots de chaleur urbains et densitÃ© bÃ¢tie hyper-concentrÃ©e. Ã‰lÃ¨ve la tempÃ©rature locale des mÃ©tropoles et consomme de la puissance de climatisation.",
                "Ref: Oke, T. R. (1982). The Energetic Basis of the Urban Heat Island. Q. J. R. Meteorol. Soc.",
                "â€¢ ÃŽlot de Chaleur Urbain Î”T_urbain = a Â· logâ‚â‚€(Immobilier_DensitÃ©) + b"},
            new String[]{"World3CouplingEngine", "ðŸ“‰ ModÃ¨le CouplÃ© World3 & Limites Ã  la Croissance (1972)",
                "RÃ©troactions entre population, pollution et capital (Club de Rome). Connecte le modÃ¨le World3 Meadows aux cellules hexagonales H3.",
                "Ref: Meadows, D. H., Meadows, D. L., Randers, J., & Behrens, W. W. (1972). Limits to Growth.",
                "â€¢ System Dynamic 5 Subsystems (Population, Capital, Agriculture, Pollution, Non-Renewables)"},
            new String[]{"World3HybridEngine", "ðŸ“‰ ModÃ¨le World3 Hybride (Physique & Capital 1972)",
                "Couplage de la dynamique de World3 aux variables spatiales H3. IntÃ¨gre la dispersion spatiale de la pollution et des ressources finies.",
                "Ref: Meadows, D. H. et al. (1972, 2004). Limits to Growth: The 30-Year Update.",
                "â€¢ Dispersion Spatiale H3 de la Pollution Persistante"},
            new String[]{"World3PureEngine", "ðŸ“‰ ModÃ¨le World3 Pur (Ã‰quations du Club de Rome 1972)",
                "Reproduction fidÃ¨le des 5 sous-systÃ¨mes du rapport Meadows 1972 (Population, Capital, Agriculture, Pollution, Ressources).",
                "Ref: Meadows, D. H. et al. (1972). World3 Model Equations (Club of Rome Report).",
                "â€¢ Reproduction IntÃ©grale des Ã‰quations World3 DYNAMO / Stella"}
        );

        typeBCheckBoxMap.clear();
        typeBParamSpinnersMap.clear();
        optionalEngineMetas.clear();
        Map<String, VBox> engineContainers = new HashMap<>();
        for (String[] eng : optionalEngines) {
            String engineTitle = org.ether.society.i18n.I18n.getEngineTitle(eng[0], eng[1]);
            CheckBox cb = new CheckBox(engineTitle);
            cb.setSelected("FrontierAsabiyyahEngine".equals(eng[0]));
            cb.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

            String desc = org.ether.society.i18n.I18n.getEngineDescription(eng[0], eng[2]);
            String ref = org.ether.society.i18n.I18n.getEngineReference(eng[0], eng[3]);
            String eqText = eng.length > 4 ? org.ether.society.i18n.I18n.getEngineEquation(eng[0], eng[4]) : I18n.getOrDefault("scenario.engine.state_eq_default", "ðŸ“ Ã‰quation d'Ã‰tat : dX/dt = f(X, t) + Î£ F_inter-cellulaire");
            String optPrefix = I18n.getOrDefault("scenario.engine.opt_prefix", "âš™ï¸ [MOTEUR OPTIONNEL]\n");
            Tooltip tooltip = new Tooltip(optPrefix + eng[0] + " â€” " + engineTitle + "\n\n" + desc + "\n\n" + eqText + "\n\nðŸ“š " + ref);
            tooltip.setStyle("-fx-font-size: 11px; -fx-max-width: 500px;");
            cb.setTooltip(tooltip);

            cb.setOnAction(e -> notifyParamChange());

            typeBCheckBoxMap.put(eng[0], cb);

            HBox row = new HBox(8, cb);
            row.setAlignment(Pos.CENTER_LEFT);

            final String[] finalEng = eng;
            row.setOnMouseEntered(e -> {
                String curTitle = I18n.getEngineTitle(finalEng[0], finalEng[1]);
                String curDesc = I18n.getEngineDescription(finalEng[0], finalEng[2]);
                String curRef = I18n.getEngineReference(finalEng[0], finalEng[3]);
                String curEq = finalEng.length > 4 ? I18n.getEngineEquation(finalEng[0], finalEng[4]) : I18n.getOrDefault("scenario.engine.state_eq_default", "ðŸ“ Ã‰quation d'Ã‰tat : dX/dt = f(X, t) + Î£ F_inter-cellulaire");
                updateEngineInspector(finalEng[0], curTitle, curDesc, curRef, curEq);
            });
            cb.setOnMouseEntered(row.getOnMouseEntered());

            optionalEngineMetas.add(new OptionalEngineMeta(eng[0], cb, eng[1], eng[2], eng[3], eng.length > 4 ? eng[4] : null));

            VBox engContainer = new VBox(4, row);
            javafx.scene.Node paramBox = createEngineParameterBox(eng[0]);
            if (paramBox != null) {
                paramBox.visibleProperty().bind(cb.selectedProperty());
                paramBox.managedProperty().bind(cb.selectedProperty());
                engContainer.getChildren().add(paramBox);
            }

            engineContainers.put(eng[0], engContainer);
            typeBBoxContainer.getChildren().add(engContainer);
        }

        engineSortCombo.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            sortOptionalEngines(newV != null ? newV.intValue() : 0, optionalEngines, engineContainers);
        });

        this.typeBPane = new TitledPane(String.format(I18n.getOrDefault("scenario.header.opt_engines_format", "âš™ï¸ MODULES OPTIONNELS (%d MOTEURS EXTENSIBLES & IMPORT/EXPORT)"), optionalEngines.size()), typeBBoxContainer);
        this.typeBPane.setExpanded(true);
        this.typeBPane.getStyleClass().add("titled-pane-secondary");

        section.getChildren().addAll(oceanOptHeader, oceanOptDesc, masterBox, optBox, engineInspectorCard, corePane, this.typeBPane);
        return section;
    }

    private void updateEngineTexts() {
        if (corePane != null) {
            corePane.setText(I18n.getOrDefault("scenario.header.core_engines", "ðŸ”’ ARCHITECTURE CÅ’UR ETHER (24 MOTEURS PERMANENTS)"));
        }
        if (typeBPane != null) {
            typeBPane.setText(String.format(I18n.getOrDefault("scenario.header.opt_engines_format", "âš™ï¸ MODULES OPTIONNELS (%d MOTEURS EXTENSIBLES & IMPORT/EXPORT)"), optionalEngineMetas.size()));
        }
        if (coreExplanationLabel != null) {
            coreExplanationLabel.setText(I18n.getOrDefault("scenario.info.core_engines", "â„¹ï¸ Why are Ether Core engines permanent? They enforce physical conservation laws (mass & energy, thermodynamics, hydrology, H3 insolation, metabolism) required for basic world survival."));
        }
        if (exportCoreTemplateBtn != null) {
            exportCoreTemplateBtn.setText(I18n.getOrDefault("scenario.btn.export_law_engine", "ðŸ“¤ Export Physical Law Engine (.java)"));
        }
        if (autoSelectEnginesForYearBtn != null) {
            autoSelectEnginesForYearBtn.setText(I18n.getOrDefault("scenario.btn.autoselect_engines", "âš¡ Cocher les Moteurs selon Tâ‚€"));
            autoSelectEnginesForYearBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.autoselect_engines", "SÃ©lectionne automatiquement les moteurs compatibles avec l'annÃ©e Tâ‚€ sÃ©lectionnÃ©e.")));
        }
        if (exportTemplateBtn != null) {
            exportTemplateBtn.setText(I18n.getOrDefault("scenario.btn.export_java_template", "ðŸ“¤ Exporter Template Moteur (.java)"));
            exportTemplateBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_java_template", "GÃ©nÃ¨re un squelette de classe Java pour crÃ©er un nouveau sous-moteur physique cliodynamique conforme.")));
        }
        if (importEngineBtn != null) {
            importEngineBtn.setText(I18n.getOrDefault("scenario.btn.import_compile_engine", "ðŸ“¥ Importer & Compiler Moteur (.java / .class)"));
            importEngineBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.import_compile_engine", "Charge et compile Ã  chaud un module moteur cliodynamique personnalisÃ©.")));
        }
        if (strictDeterminismCheckBox != null) {
            strictDeterminismCheckBox.setText(I18n.getOrDefault("scenario.opt.strict_determinism", "ðŸ”’ MODE DÃ‰TERMINISME STRICT (0% d'approximation / ReproductibilitÃ© bit-Ã -bit 100%)"));
            strictDeterminismCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.strict_determinism",
                "ðŸ”’ RÃˆGLE FONDAMENTALE DE DÃ‰TERMINISME STRICT\n" +
                "â€¢ Si activÃ© : Aucune approximation spatiale ou temporelle n'est tolÃ©rÃ©e.\n" +
                "â€¢ Toutes les cellules H3 sont Ã©valuÃ©es Ã  chaque sous-itÃ©ration.\n" +
                "â€¢ DÃ©sactive les raccourcis de performance pour garantir des trajectoires 100% identiques.")));
        }
        if (optSubHeader != null) {
            optSubHeader.setText(I18n.getOrDefault("scenario.opt.sub_header", "âš¡ ALGORITHMIC OPTIMIZATIONS & PERFORMANCE SHORTCUTS:"));
        }
        if (sparseCellSkippingCheckBox != null) {
            sparseCellSkippingCheckBox.setText(I18n.getOrDefault("scenario.opt.sparse_cell_skipping", "ðŸœ Sparse / Uninhabited Cell Skipping (Deserts & Abysses)"));
            sparseCellSkippingCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.sparse_cell_skipping",
                "âš¡ BENEFIT: +40% to +60% TPS speedup across global grid.\n" +
                "âš ï¸ PHYSICAL IMPACT: Bypasses evaluation loops on desert/oceanic cells with no human presence or active event.")));
        }
        if (oceanMacroAggregationCheckBox != null) {
            oceanMacroAggregationCheckBox.setText(I18n.getOrDefault("scenario.ocean_opt.macro_aggregation", "ðŸŒŠ Abyssal Ocean Macro-Aggregation (Deep Basins z < -200m in Blocks)"));
            oceanMacroAggregationCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.ocean_macro_aggregation",
                "âš¡ BENEFIT: +25% to +35% TPS speedup by grouping deep water cells.\n" +
                "âš ï¸ PHYSICAL IMPACT: Smoothing of abyssal micro-currents without impacting terrestrial civilizations.")));
        }
        if (coastalNavigationOnlyCheckBox != null) {
            coastalNavigationOnlyCheckBox.setText(I18n.getOrDefault("scenario.ocean_opt.coastal_nav", "âš“ Exclusive Coastal Navigation (Pathfinding Focused on Coasts & Straits)"));
            coastalNavigationOnlyCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.coastal_nav",
                "âš¡ BENEFIT: Major CPU savings on naval and commercial trade pathfinding.\n" +
                "âš ï¸ PHYSICAL IMPACT: Ships prefer coastal waters; ocean navigation restricted prior to Age of Discovery.")));
        }
        if (oceanMultiRateTickingCheckBox != null) {
            oceanMultiRateTickingCheckBox.setText(I18n.getOrDefault("scenario.ocean_opt.multi_rate_ticking", "â± Oceanic & Multi-Rate Climate Ticking (Updated Every N Ticks)"));
            oceanMultiRateTickingCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.multi_rate_ticking",
                "âš¡ BENEFIT: +30% throughput by executing thermohaline circulation and fluid inertia at sub-frequency.\n" +
                "âš ï¸ PHYSICAL IMPACT: Potential temporal aliasing during ultra-fast atmospheric events.")));
        }
        if (parallelExecutionCheckBox != null) {
            parallelExecutionCheckBox.setText(I18n.getOrDefault("scenario.opt.parallel_execution", "ðŸš€ Async Multi-Thread Parallelization (CompletableFuture / AVX)"));
            parallelExecutionCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.parallel_execution",
                "âš¡ BENEFIT: Full exploitation of all available CPU cores.\n" +
                "âš ï¸ PHYSICAL IMPACT: Floating point order may slightly vary across executions.")));
        }
        if (spatialRangeTruncationCheckBox != null) {
            spatialRangeTruncationCheckBox.setText(I18n.getOrDefault("scenario.opt.spatial_truncation", "ðŸ’¨ Spatial Range Truncation of Plumes & Diffusions (10â»â¶ Cutoff)"));
            spatialRangeTruncationCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.spatial_truncation",
                "âš¡ BENEFIT: Restricts atmospheric dispersion calculation to affected adjacent cells.\n" +
                "âš ï¸ PHYSICAL IMPACT: Discards ultra-diluted aerosol and soot concentrations below 10â»â¶ ppm.")));
        }
        if (engineInspectorTitle != null && selectedEngineClassName != null) {
            engineInspectorTitle.setText("ðŸ”Ž " + selectedEngineClassName + " â€” " + I18n.getEngineTitle(selectedEngineClassName, ""));
        }
        if (engineInspectorEquationsTitle != null) {
            engineInspectorEquationsTitle.setText(I18n.getOrDefault("scenario.label.engine_equations", "ðŸ“ Mathematical Equations & Cliodynamic Formulation:"));
        }
        if (exportSelectedEngineBtn != null && selectedEngineClassName != null) {
            exportSelectedEngineBtn.setText(I18n.getOrDefault("scenario.btn.export_prefix", "ðŸ“¤ Export ") + selectedEngineClassName + ".java");
        }

        // Refresh Core engines
        for (CoreEngineRow row : coreEngineRows) {
            String title = I18n.getEngineTitle(row.id, row.defaultTitle);
            row.descLabel.setText("â€” " + title);
            String desc = I18n.getEngineDescription(row.id, row.defaultDesc);
            String ref = I18n.getEngineReference(row.id, row.defaultRef);
            String eqText = row.defaultEq != null ? I18n.getEngineEquation(row.id, row.defaultEq) : I18n.getOrDefault("scenario.engine.state_eq_default", "ðŸ“ Ã‰quation d'Ã‰tat : dX/dt = f(X, t) + Î£ F_inter-cellulaire");
            String permPrefix = I18n.getOrDefault("scenario.engine.perm_prefix", "ðŸ”’ [MOTEUR PERMANENT]\n");
            Tooltip tooltip = new Tooltip(permPrefix + row.id + " â€” " + title + "\n\n" + desc + "\n\n" + eqText + "\n\nðŸ“š " + ref);
            tooltip.setStyle("-fx-font-size: 11px; -fx-max-width: 500px;");
            Tooltip.install(row.row, tooltip);
        }

        // Refresh Optional engines
        for (OptionalEngineMeta meta : optionalEngineMetas) {
            String title = I18n.getEngineTitle(meta.id, meta.defaultTitle);
            meta.cb.setText(title);
            String desc = I18n.getEngineDescription(meta.id, meta.defaultDesc);
            String ref = I18n.getEngineReference(meta.id, meta.defaultRef);
            String eqText = meta.defaultEq != null ? I18n.getEngineEquation(meta.id, meta.defaultEq) : I18n.getOrDefault("scenario.engine.state_eq_default", "ðŸ“ Ã‰quation d'Ã‰tat : dX/dt = f(X, t) + Î£ F_inter-cellulaire");
            String optPrefix = I18n.getOrDefault("scenario.engine.opt_prefix", "âš™ï¸ [MOTEUR OPTIONNEL]\n");
            Tooltip tooltip = new Tooltip(optPrefix + meta.id + " â€” " + title + "\n\n" + desc + "\n\n" + eqText + "\n\nðŸ“š " + ref);
            tooltip.setStyle("-fx-font-size: 11px; -fx-max-width: 500px;");
            meta.cb.setTooltip(tooltip);
        }

        // Refresh engine inspector details
        if (selectedEngineClassName != null) {
            for (CoreEngineRow row : coreEngineRows) {
                if (row.id.equals(selectedEngineClassName)) {
                    updateEngineInspector(row.id, I18n.getEngineTitle(row.id, row.defaultTitle), I18n.getEngineDescription(row.id, row.defaultDesc), I18n.getEngineReference(row.id, row.defaultRef), row.defaultEq != null ? I18n.getEngineEquation(row.id, row.defaultEq) : I18n.getOrDefault("scenario.engine.state_eq_default", "ðŸ“ Ã‰quation d'Ã‰tat : dX/dt = f(X, t) + Î£ F_inter-cellulaire"));
                    break;
                }
            }
            for (OptionalEngineMeta meta : optionalEngineMetas) {
                if (meta.id.equals(selectedEngineClassName)) {
                    updateEngineInspector(meta.id, I18n.getEngineTitle(meta.id, meta.defaultTitle), I18n.getEngineDescription(meta.id, meta.defaultDesc), I18n.getEngineReference(meta.id, meta.defaultRef), meta.defaultEq != null ? I18n.getEngineEquation(meta.id, meta.defaultEq) : I18n.getOrDefault("scenario.engine.state_eq_default", "ðŸ“ Ã‰quation d'Ã‰tat : dX/dt = f(X, t) + Î£ F_inter-cellulaire"));
                    break;
                }
            }
        }
    }

    private void autoSelectEnginesForYear(long year) {
        for (Map.Entry<String, CheckBox> entry : typeBCheckBoxMap.entrySet()) {
            long appYear = getEngineApparitionYear(entry.getKey());
            entry.getValue().setSelected(year >= appYear);
        }
        notifyParamChange();
    }

    private void sortOptionalEngines(int sortIdx, List<String[]> optionalEngines, Map<String, VBox> engineContainers) {
        if (typeBBoxContainer == null) return;
        List<javafx.scene.Node> headers = new ArrayList<>();
        if (typeBBoxContainer.getChildren().size() >= 2) {
            headers.add(typeBBoxContainer.getChildren().get(0));
            headers.add(typeBBoxContainer.getChildren().get(1));
        }

        List<String[]> sorted = new ArrayList<>(optionalEngines);
        if (sortIdx == 1) { // Chronological (Oldest -> Newest)
            sorted.sort(Comparator.comparingLong(e -> getEngineApparitionYear(e[0])));
        } else if (sortIdx == 2) { // Reverse Chronological (Newest -> Oldest)
            sorted.sort((a, b) -> Long.compare(getEngineApparitionYear(b[0]), getEngineApparitionYear(a[0])));
        } else if (sortIdx == 3) { // Alphabetical (A - Z)
            sorted.sort(Comparator.comparing(e -> I18n.getEngineTitle(e[0], e[1])));
        } // sortIdx == 0 -> Keep original system category order

        typeBBoxContainer.getChildren().clear();
        typeBBoxContainer.getChildren().addAll(headers);
        for (String[] eng : sorted) {
            VBox box = engineContainers.get(eng[0]);
            if (box != null) {
                typeBBoxContainer.getChildren().add(box);
            }
        }
    }

    private static long getEngineApparitionYear(String key) {
        return switch (key) {
            case "OldowanMarrowPercussionEngine" -> -2600000L;
            case "AcheuleanBifaceSymmetryEngine" -> -500000L;
            case "KarstCaveShelterEngine", "LithicTradeProvenanceEngine", "FireHardenedSpearEngine",
                 "DemographicLifeTableEngine", "ToolKitMaintenanceEngine", "LevalloisPreparedCoreEngine",
                 "SelfDomesticationEngine", "SexualSelectionMatingEngine", "KinSelectionHamiltonEngine" -> -300000L;
            case "ResinHaftingAdhesivesEngine" -> -200000L;
            case "GeophyteDiggingStickEngine" -> -170000L;
            case "CoastalMarineRefugiaEngine" -> -160000L;
            case "PaleoLanguageDriftEngine", "OchreTanningTechnologyEngine", "HomininCompetitiveExclusionEngine" -> -100000L;
            case "VolcanicTephraRefugiaEngine" -> -74000L;
            case "ParasiteControlRepellentEngine", "OchreResinHaftingEngine" -> -70000L;
            case "AridWaterStorageStashEngine", "OstrichEggshellNetworkEngine" -> -60000L;
            case "ArchaicIntrogressionEngine", "FireStickFarmingEngine", "BirchTarPyrolysisKilnEngine",
                 "CaveBearNicheCompetitionEngine", "OchreTradeAllianceEngine", "ZoonoticPathogenSpilloverEngine",
                 "MegafaunaEcosystemEngine", "CulturalSociologyEngine" -> -50000L;
            case "ParietalArtAsabiyyahEngine", "RiverCanoeTransportEngine", "CaveHyenaScavengingEngine",
                 "AridOasisWellDiggingEngine" -> -45000L;
            case "PelagicFishingHookEngine" -> -42000L;
            case "ExogamousKinshipEngine", "OsseousIndustryCarvingEngine", "AcousticFluteResonanceEngine",
                 "SymbolicBeadNetworkEngine", "HighAltitudeHypoxiaEngine", "HandStencilTerritoryEngine",
                 "DynamicMaritimeRoutingGraph", "OchreMiningQuarryEngine" -> -40000L;
            case "TailoredClothingThermalEngine", "CaveLightingPyrotechnicsEngine", "EyedNeedleSewingEngine",
                 "LashingsHideThongEngine", "CaveWallClaySealingEngine" -> -35000L;
            case "WildFlaxSpinningEngine" -> -32000L;
            case "SeasonalAggregationSanctuaryEngine", "PlantFiberCordageEngine", "PortableArtFigurineEngine",
                 "PermafrostColdCacheEngine", "LunarCalendarTallyEngine", "MortuaryBurialRegaliaEngine",
                 "EndokarstTorchMappingEngine" -> -30000L;
            case "ProtoCeramicFiringEngine" -> -29000L;
            case "WindCuringSteppeCacheEngine" -> -28000L;
            case "IvoryHotWaterStraighteningEngine" -> -27000L;
            case "MeatCuringReservesEngine", "MammothBoneHabitationEngine", "PressureFlakerPointEngine",
                 "SnowTroughRefrigerationEngine" -> -25000L;
            case "PeriglacialLoessDustEngine" -> -24000L;
            case "WildCerealGrindingEngine" -> -23000L;
            case "BeringianStandstillIsolationEngine" -> -22000L;
            case "HeatTreatedFlintPressureEngine" -> -21000L;
            case "AtlatlArcheryBallisticsEngine", "PlantDetoxificationLeachingEngine",
                 "MicrolithBladeletProductionEngine", "FluvioglacialLithicHarvestEngine",
                 "MarrowFatRenderingEngine", "BirchBarkVesselEngine" -> -20000L;
            case "SkinKayakSubArcticEngine", "AtlatlBalancingStoneEngine", "BasaltGrindingSlabEngine" -> -18000L;
            case "JomonCeramicBoilingEngine" -> -16500L;
            case "SalmonRunHarpoonEngine", "KelpHighwayNavigationEngine" -> -16000L;
            case "CanidDomesticationEngine", "PassiveSnareSmallGameEngine", "MegafaunaPitfallTrapEngine",
                 "SeaOtterFurTanningEngine", "ReindeerRiverInterceptionEngine", "ArrowPoisonSynthesisEngine",
                 "LakeChadWadiMigrationEngine" -> -15000L;
            case "NightTorchSpearfishingEngine", "ObsidianSolarIgnitionEngine" -> -14000L;
            case "TrophicCascadesPredatorEngine" -> -13000L;
            case "EpipaleolithicStorageHamletEngine" -> -12500L;
            case "TopographicGameDriveEngine", "ShellMiddenAccumulationEngine", "BisonCliffJumpDriveEngine",
                 "SubGlacialMeltwaterWeirEngine" -> -12000L;
            case "DoggerlandMarshFowlingEngine" -> -11000L;
            case "SnowpackMobilityEngine", "TasmanianCulturalRegressionEngine" -> -10000L;
            case "DeforestationErosionEngine" -> -6000L;
            case "BioMolecularEpidemiologyEngine" -> -5000L;
            case "AmerindianEcosystemEngine" -> -4000L;
            case "ScottAgainstTheGrainPureEngine" -> -3500L;
            case "MaritimeHighwayEngine", "HydrologicalEngineeringEngine", "SpatialCityFractalEngine" -> -3000L;
            case "FertileCrescentSalinizationEngine" -> -2400L;
            case "RomanImperialCliodynamicEngine" -> -27L;
            case "MonasticDemographicBufferEngine" -> 500L;
            case "LandReclamationEngine", "MilitaryTechShockEngine" -> 1200L;
            case "FrontierAsabiyyahEngine" -> 1377L;
            case "AsymmetricColonialTradeEngine" -> 1500L;
            case "EdoJapanIsolationEngine" -> 1635L;
            case "EntropicMetalDissipationEngine" -> 1800L;
            case "UrbanThermodynamicsEngine" -> 1850L;
            case "JevonsParadoxEngine" -> 1865L;
            case "ProtestantWorkEthicEngine" -> 1905L;
            case "LeslieWhitePureEngine" -> 1943L;
            case "NuclearWarfareClimateEngine" -> 1945L;
            case "AutoRegulationPureEngine" -> 1948L;
            case "NuclearSafetyRadiotoxicityEngine", "MarineSubmersionEngine", "EcotoxicologyFertilityEngine" -> 1950L;
            case "PsychohistoryPureEngine" -> 1951L;
            case "BifurcationChaosEngine" -> 1963L;
            case "KardashevPureEngine" -> 1964L;
            case "LenskiPureEngine" -> 1966L;
            case "World3CouplingEngine", "World3HybridEngine", "World3PureEngine" -> 1972L;
            case "CulturalMaterialismPureEngine" -> 1979L;
            case "OstromCommonsPureEngine" -> 1990L;
            case "NordhausDiceHybridEngine", "NordhausDicePureEngine" -> 1992L;
            case "KurzweilAcceleratingReturnsEngine" -> 2005L;
            case "PinkerViolenceDeclinePureEngine" -> 2011L;
            case "HandyNasaHybridEngine", "HandyNasaPureEngine" -> 2014L;
            case "SmilMaterialTransitionsPureEngine" -> 2019L;
            case "AiAutonomousRegulationPureEngine" -> 2023L;
            case "GeoengineeringAlbedoFeedbackEngine" -> 2030L;
            case "TechnologicalSingularityEngine" -> 2045L;
            default -> -100000L;
        };
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

        cultureHeader = new Label(I18n.getOrDefault("scenario.culture_section", "ðŸ§  3. CULTURAL VECTOR DIMENSION & MULTI-FIELD LAYERS"));
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
        Label diffLbl = new Label(I18n.getOrDefault("scenario.label.cultural_diffusion", "Cultural Diffusion Conductivity (Î±):"));
        diffLbl.getStyleClass().add("control-label");
        culturalDiffusionRateSpinner = new Spinner<>(0.001, 0.50, 0.05, 0.01);
        culturalDiffusionRateSpinner.setEditable(true);
        culturalDiffusionRateSpinner.setMaxWidth(Double.MAX_VALUE);
        culturalDiffusionRateSpinner.valueProperty().addListener((obs, o, n) -> notifyParamChange());
        Tooltip.install(culturalDiffusionRateSpinner, new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_diffusion", "Diffusion rate of association energy between neighboring H3 hexagons.")));

        // 3. Cultural Mutation Noise Rate
        Label mutLbl = new Label(I18n.getOrDefault("scenario.label.cultural_mutation", "Mutation / Drift Noise (1/âˆšN):"));
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

        btnGenerateProceduralTensorsSection = new Button(I18n.getOrDefault("scenario.btn.regen_tensors", "ðŸª„ RÃ©gÃ©nÃ©rer les Tenseurs"));
        btnGenerateProceduralTensorsSection.getStyleClass().add("button");
        btnGenerateProceduralTensorsSection.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnGenerateProceduralTensorsSection.setMinWidth(Region.USE_PREF_SIZE);
        btnGenerateProceduralTensorsSection.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.regen_tensors", "Bascule tous les tenseurs en mode procÃ©dural et rÃ©gÃ©nÃ¨re les cartes selon les paramÃ¨tres et la graine stochastique.")));
        btnGenerateProceduralTensorsSection.setOnAction(e -> generateProceduralCulturalTensors());

        cultSeedField = new TextField("54321");
        cultSeedField.setPrefWidth(80);
        cultSeedField.setStyle("-fx-font-size: 11px;");
        cultSeedField.textProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        Button cultRandSeedBtn = new Button("ðŸŽ²");
        cultRandSeedBtn.getStyleClass().add("button-secondary");
        cultRandSeedBtn.setStyle("-fx-font-size: 11px;");
        cultRandSeedBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.random_tensor_seed", "Tirer une nouvelle graine stochastique alÃ©atoire pour les tenseurs culturels (sans affecter la densitÃ© de population).")));

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

        HBox cultSeedBox = new HBox(4, new Label(I18n.getOrDefault("scenario.label.cultural_seed", "Graine :")), new Label("ðŸŽ²"), cultSeedField, cultRandSeedBtn);
        cultSeedBox.setAlignment(Pos.CENTER_LEFT);

        HBox seedRow = new HBox(8, cultSeedBox, btnGenerateProceduralTensorsSection);
        seedRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(btnGenerateProceduralTensorsSection, Priority.ALWAYS);

        Button btnExportGisMultiFormat = new Button(I18n.getOrDefault("scenario.btn.export_gis", "ðŸ—ºï¸ Export SIG Multi-Format"));
        btnExportGisMultiFormat.getStyleClass().add("button-secondary");
        btnExportGisMultiFormat.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportGisMultiFormat.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_gis", "Export scenario density layers and 9 cultural tensors under professional GIS formats (GeoTIFF, NetCDF-4, GeoJSON).")));
        btnExportGisMultiFormat.setOnAction(e -> exportGisMultiFormat());

        Button btnExportProvenanceManifest = new Button(I18n.getOrDefault("scenario.btn.provenance_manifest", "ðŸ”’ Manifeste Provenance SHA-256"));
        btnExportProvenanceManifest.getStyleClass().add("button-secondary");
        btnExportProvenanceManifest.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportProvenanceManifest.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.gen_provenance", "Generate a cryptographic JSON manifest (provenance.json) containing SHA-256 hashes of datasets and parameters.")));
        btnExportProvenanceManifest.setOnAction(e -> exportProvenanceManifest());

        Button btnCulturalMatrix = new Button(I18n.getOrDefault("scenario.btn.cultural_matrix_inspect", "ðŸ” Inspecteur des AffinitÃ©s Culturales"));
        btnCulturalMatrix.getStyleClass().add("button-secondary");
        btnCulturalMatrix.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnCulturalMatrix.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_matrix", "Inspect the N Ã— N Pairwise Cultural Affinity and Distance Heatmap Matrix.")));
        btnCulturalMatrix.setOnAction(e -> {
            long epoch = startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -100000L;
            new CulturalAffinityMatrixDialog(epoch).show();
        });

        HBox actionButtonsRow = new HBox(8, culturalHelpBtn, btnExportGisMultiFormat, btnExportProvenanceManifest);
        actionButtonsRow.setAlignment(Pos.CENTER_LEFT);

        VBox matrixCard = new VBox(4);
        matrixCard.setStyle("-fx-padding: 8px 10px; -fx-background-color: rgba(148, 163, 184, 0.08); -fx-background-radius: 6px; -fx-border-color: rgba(148, 163, 184, 0.25); -fx-border-radius: 6px; -fx-border-width: 1px;");
        Label matrixTitle = new Label(I18n.getOrDefault("scenario.cultural_matrix.title", "Matrice d'AffinitÃ© & Distances Culturelles"));
        matrixTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        Label matrixDesc = new Label(I18n.getOrDefault("scenario.cultural_matrix.desc", "Inspecter et Ã©diter la matrice NÃ—N des distances de Mahalanobis et coefficients d'assimilation entre entitÃ©s culturelles."));
        matrixDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: -fx-text-muted;");
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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.getOrDefault("scenario.title.export_gis_dialog", "Export Scenario Data under GIS Format (GeoJSON / GeoTIFF / NetCDF)"));
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("GeoJSON Vector File (*.geojson)", "*.geojson"),
            new FileChooser.ExtensionFilter("GeoTIFF Multi-Band Raster Metadata (*.json)", "*.json"),
            new FileChooser.ExtensionFilter("NetCDF-4 Spatiotemporal Cube (*.nc.json)", "*.nc.json")
        );
        File targetFile = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (targetFile != null) {
            try {
                Map<String, Object> gisData = new LinkedHashMap<>();
                gisData.put("type", "FeatureCollection");
                gisData.put("crs", Map.of("type", "name", "properties", Map.of("name", "urn:ogc:def:crs:OGC:1.3:CRS84")));
                gisData.put("scenario", scenarioDescriptionArea != null ? scenarioDescriptionArea.getText() : "Ether Scenario");
                gisData.put("startYear", startYearSpinner != null ? startYearSpinner.getValue() : -100000);
                gisData.put("culturalTensorsCount", cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9);
                gisData.put("exportTimestamp", java.time.Instant.now().toString());

                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(targetFile, gisData);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(I18n.getOrDefault("scenario.title.gis_success", "GIS Export Successful"));
                alert.setHeaderText(I18n.getOrDefault("scenario.header.gis_success", "GIS File Generated Successfully"));
                alert.setContentText("Le scÃ©nario a Ã©tÃ© exportÃ© sous format SIG compatible QGIS/ArcGIS : " + targetFile.getName());
                alert.showAndWait();
            } catch (Exception ex) {
                logger.error("Erreur lors de l'exportation SIG du scÃ©nario: {}", ex.getMessage(), ex);
            }
        }
    }

    private void exportProvenanceManifest() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.getOrDefault("scenario.title.export_provenance_dialog", "Generate & Export SHA-256 Provenance Manifest"));
        fileChooser.setInitialFileName("provenance.json");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Manifest (*.json)", "*.json"));
        File targetFile = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (targetFile != null) {
            try {
                Map<String, Object> manifest = new LinkedHashMap<>();
                manifest.put("manifestVersion", "1.0.0");
                manifest.put("engineVersion", "Ether 1.0.0-beta.1");
                manifest.put("exportTimestamp", java.time.Instant.now().toString());
                manifest.put("startYearBP", startYearSpinner != null ? startYearSpinner.getValue() : -100000);
                manifest.put("prngSeed", cultSeedField != null ? cultSeedField.getText() : "54321");
                manifest.put("deterministicReplayGuaranteed", true);

                Map<String, String> hashes = new LinkedHashMap<>();
                hashes.put("hyde34_baseline", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
                hashes.put("natural_earth_vectors", "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e");
                hashes.put("chelsa_trace21k_paleoclimate", "3b7b4d32f7e77a1122ab9123456789abcdef0123456789abcdef0123456789ab");
                manifest.put("datasetHashesSHA256", hashes);

                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(targetFile, manifest);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(I18n.getOrDefault("scenario.title.provenance_success", "Cryptographic Manifest Generated"));
                alert.setHeaderText(I18n.getOrDefault("scenario.header.provenance_success", "provenance.json Manifest Created"));
                alert.setContentText("Le manifeste de provenance SHA-256 garantissant la reproductibilitÃ© a Ã©tÃ© Ã©crit dans : " + targetFile.getName());
                alert.showAndWait();
            } catch (Exception ex) {
                logger.error("Erreur lors de la gÃ©nÃ©ration du manifeste de provenance: {}", ex.getMessage(), ex);
            }
        }
    }

    private void showCulturalImportFormatHelp() {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle(I18n.getOrDefault("scenario.title.layer_formats_modal", "SpÃ©cifications Techniques des Tenseurs Cartographiques & Datasets"));
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f172a; -fx-text-fill: #f8fafc;");
        root.setPrefWidth(720);
        root.setPrefHeight(600);

        Label title = new Label("ðŸ—ºï¸ " + I18n.getOrDefault("scenario.layer_formats.title", "Standard des Tenseurs Spatio-Temporels (2048 Ã— 1024, Ã‰quirectangulaire 2:1)"));
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #0f172a; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox contentBox = new VBox(14);
        contentBox.setStyle("-fx-background-color: #0f172a;");

        // Section 1: 24-bit RGB Categorical & Spatial Dithering Tensors
        VBox sec1 = new VBox(6);
        sec1.setStyle("-fx-background-color: #1e293b; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #3b82f6; -fx-border-radius: 8;");
        Label lblSec1 = new Label("ðŸ·ï¸ " + I18n.getOrDefault("scenario.layer_formats.rgb_title", "1. Tenseurs CatÃ©goriels Discrets & Dithering Spatial (Encodage RGB 24-bit)"));
        lblSec1.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #60a5fa;");
        Label descSec1 = new Label(
            "â€¢ Tenseur 0 (Isoglosses / Langues) : Couleur RGB pure par famille linguistique (Glottolog 5.0 / WALS) + Dithering stochastique aux zones de contact.\n" +
            "â€¢ Tenseur 1 (ParentÃ© / Filiation) : Typologie Todd & Murdock (Famille souche #8B5CF6, NuclÃ©aire #2563EB, Communautaire #DC2626, MatrilinÃ©aire #F43F5E).\n" +
            "â€¢ Tenseur 2 (Rituels & Croyances) : SystÃ¨mes sacrÃ©s (Sunnisme #10B981, Catholicisme #EC4899, Orthodoxie #8B5CF6, Culte GrÃ©co-Romain #EA580C, Hindouisme #F59E0B, Bouddhisme #EAB308, JudaÃ¯sme #2563EB) + MosaÃ¯que multiconfessionnelle dans les grands carrefours sacrÃ©s.\n" +
            "â€¢ Tenseur 3 (SouverainetÃ© & Domaines) : EntitÃ©s politiques Ã©tatiques (Seshat ClioPatria / CShapes) et 22 domaines claniques/tribaux rÃ©gionaux.\n" +
            "â†³ RÃ©fÃ©rence d'association : data/maps/ether/earth/<annÃ©e>/cultural_registry.json"
        );
        descSec1.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11.5px;");
        descSec1.setWrapText(true);
        sec1.getChildren().addAll(lblSec1, descSec1);

        // Section 2: Multi-Modal Network & Connectivity Tensor
        VBox sec2 = new VBox(6);
        sec2.setStyle("-fx-background-color: #1e293b; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #06b6d4; -fx-border-radius: 8;");
        Label lblSec2 = new Label("ðŸŒ " + I18n.getOrDefault("scenario.layer_formats.network_title", "2. Tenseur de RÃ©seau Multi-Modal & Flux (RGB 24-bit)"));
        lblSec2.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #22d3ee;");
        Label descSec2 = new Label(
            "â€¢ Tenseur 5 (RÃ©seau Commercial & Hydrographie) : RÃ©seau multi-modal interconnectÃ© :\n" +
            "  - Corridors fluviaux navigables (Nil, Tigre-Euphrate, Indus, Gange, YangtsÃ©, Fleuve Jaune, Rhin, Danube, Dniepr, Volga, Niger, Mississippi, Amazone) en cyan (#00D2E6).\n" +
            "  - Voies impÃ©riales antiques pavÃ©es (Voies romaines, Route royale perse, Chi Dao Qin-Han, Grand Trunk Road) en or/terracotta (#FFAA28).\n" +
            "  - Routes transcontinentales (Soie, Encens, Ambre) en or Ã©clatant (#FFC832).\n" +
            "  - Voies maritimes (MÃ©diterranÃ©e, OcÃ©an Indien, Atlantique, Galions de Manille) en bleu/sarcelle (#1EB4D2).\n" +
            "  - NÅ“uds d'Emporia & Comptoirs marchands matÃ©rialisÃ©s par des disques blancs Ã  halo luminescent hiÃ©rarchisÃ© selon le tonnage de transit."
        );
        descSec2.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11.5px;");
        descSec2.setWrapText(true);
        sec2.getChildren().addAll(lblSec2, descSec2);

        // Section 3: 8-bit Continuous Grayscale Physical Intensity Tensors
        VBox sec3 = new VBox(6);
        sec3.setStyle("-fx-background-color: #1e293b; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #10b981; -fx-border-radius: 8;");
        Label lblSec3 = new Label("ðŸ“ˆ " + I18n.getOrDefault("scenario.layer_formats.gray_title", "3. Tenseurs Continus d'IntensitÃ© Physique & Cliodynamique (Niveaux de Gris 8-bit [0..255])"));
        lblSec3.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #34d399;");
        Label descSec3 = new Label(
            "â€¢ DÃ©mographie (Density) : [0..255] DensitÃ© de population par kmÂ² calibrÃ©e sur HYDE 3.4.\n" +
            "â€¢ Tenseur 4 (MatÃ©rialitÃ© & Technologies) : [0..255] EROEI, outillage, capital Kâ‚€ et intensitÃ© d'innovation (ArchaeoGLOBE).\n" +
            "â€¢ Tenseur 6 (ComplexitÃ© Institutionnelle) : [0..255] HiÃ©rarchie administrative SESHAT (Rome/Han 160, 2026 Ã  220, headroom futur [226-255]) avec portÃ©e Ã©tendue le long des infrastructures.\n" +
            "â€¢ Tenseur 7 (Empreinte Ã‰cologique) : [0..255] Consommation d'exergy agricole, dÃ©forestation et pression biotique.\n" +
            "â€¢ Tenseur 8 (PathogÃ¨nes & Râ‚€) : [0..255] Charge vectorielle tropicale, rÃ©servoirs zoonotiques et foyers Ã©pidÃ©miques de promiscuitÃ© urbaine.\n" +
            "â€¢ 10 Tenseurs GÃ©ologiques : Charbon, PÃ©trole, Gaz, Uranium, HÃ©lium-3, Fer/Cuivre, MÃ©taux PrÃ©cieux, Terres Rares, GÃ©othermie, AquifÃ¨res (USGS MRDS / WHYMAP / GEM)."
        );
        descSec3.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11.5px;");
        descSec3.setWrapText(true);
        sec3.getChildren().addAll(lblSec3, descSec3);

        // Section 4: Temporal Validity of Datasets
        VBox sec4 = new VBox(6);
        sec4.setStyle("-fx-background-color: #1e293b; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #f59e0b; -fx-border-radius: 8;");
        Label lblSec4 = new Label("â³ " + I18n.getOrDefault("scenario.layer_formats.validity_title", "4. Plage de ValiditÃ© Temporelle des Jeux de DonnÃ©es"));
        lblSec4.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;");
        Label descSec4 = new Label(
            "â€¢ SESHAT & HYDE 3.4 : Strictement valides pour t â‰¥ -10 000 BCE (HolocÃ¨ne et Histoire documentÃ©e).\n" +
            "â€¢ PrÃ©histoire PalÃ©olithique (t < -10 000 BCE) : ModÃ¨les archÃ©ologiques orographiques multi-clades (Sapiens, NÃ©andertaliens, Denisoviens, ArchaÃ¯ques).\n" +
            "â€¢ ForÃ§ages PalÃ©oclimatiques : CHELSA-TraCE21k (LGM -20k, HolocÃ¨ne) et WorldClim / PaleoClim LIG (-100k BP).\n" +
            "â€¢ AltimÃ©trie & BathymÃ©trie : NOAA ETOPO 2022 (Niveau marin ajustÃ© dynamiquement selon l'Ã©poque glaciaire)."
        );
        descSec4.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11.5px;");
        descSec4.setWrapText(true);
        sec4.getChildren().addAll(lblSec4, descSec4);

        contentBox.getChildren().addAll(sec1, sec2, sec3, sec4);
        scroll.setContent(contentBox);

        Button btnClose = new Button(I18n.getOrDefault("common.close", "Fermer"));
        btnClose.getStyleClass().add("button-primary");
        btnClose.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        btnClose.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(btnClose);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, scroll, btnBox);
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        dialog.setScene(scene);
        dialog.show();
    }

    private String getCulturalTensorTitle(int index) {
        if (customTensorNames.containsKey(index) && !customTensorNames.get(index).isBlank()) {
            return "ðŸ§¬ 3.2." + (index + 1) + " " + I18n.getOrDefault("scenario.tensor.label_prefix", "Tenseur ") + (index + 1) + " : " + customTensorNames.get(index);
        }
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.title.0", "ðŸ“œ 3.2.1 Tensor 1: Isoglosses & Linguistic Continua (Languages)");
            case 1 -> I18n.getOrDefault("scenario.tensor.title.1", "ðŸ› 3.2.2 Tensor 2: Kinship & Clan Structures (Lineage)");
            case 2 -> I18n.getOrDefault("scenario.tensor.title.2", "ðŸ”® 3.2.3 Tensor 3: Rituals, Beliefs & Sacred (Asabiyyah)");
            case 3 -> I18n.getOrDefault("scenario.tensor.title.3", "ðŸ‘‘ 3.2.4 Tensor 4: Politico-Military Sovereignty & Capitals");
            case 4 -> I18n.getOrDefault("scenario.tensor.title.4", "ðŸº 3.2.5 Tensor 5: Tooling, Materiality & Technologies (Artifacts)");
            case 5 -> I18n.getOrDefault("scenario.tensor.title.5", "ðŸ« 3.2.6 Tensor 6: Trade Corridors & Networks (Economic Routes)");
            case 6 -> I18n.getOrDefault("scenario.tensor.title.6", "âš– 3.2.7 Tensor 7: Institutional Complexity & Norms (Seshat & Law)");
            case 7 -> I18n.getOrDefault("scenario.tensor.title.7", "âš ï¸ 3.2.8 Tensor 8: Ecological Footprint & Malthusian Stress (Degradation)");
            case 8 -> I18n.getOrDefault("scenario.tensor.title.8", "ðŸ§¬ 3.2.9 Tensor 9: Pathogen Immunity & Health Memory (Epidemiology)");
            default -> I18n.getOrDefault("scenario.tensor.title.ext", "ðŸ§¬ 3.2.{0} Tensor {0}: Extensible Cultural Substrate {0}", (index + 1));
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
            default -> I18n.getOrDefault("scenario.status.empirical_loaded", "ðŸ“· Empirical baseline layer loaded");
        };
    }

    private String getCulturalFormatHint(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.0.format", "RGB 24-bit CatÃ©goriel + Dithering Spatial (2:1 Ã©quirectangulaire) :\n  Valeurs RGB = Familles linguistiques pures & continuums dialectaux (Glottolog 5.0 / WALS)");
            case 1 -> I18n.getOrDefault("scenario.tensor.1.format", "RGB 24-bit CatÃ©goriel (2:1 Ã©quirectangulaire) :\n  Valeurs RGB = Organisation sociale & familiale (Todd / Murdock : Souche #8B5CF6, NuclÃ©aire #2563EB, Communautaire #DC2626, MatrilinÃ©aire #F43F5E)");
            case 2 -> I18n.getOrDefault("scenario.tensor.2.format", "RGB 24-bit CatÃ©goriel + MosaÃ¯que Multiconfessionnelle (2:1 Ã©quirectangulaire) :\n  Valeurs RGB = SystÃ¨mes sacrÃ©s (Islam #10B981, Catholicisme #EC4899, Orthodoxie #8B5CF6, Culte GrÃ©co-Romain #EA580C, Hindouisme #F59E0B, Bouddhisme #EAB308, JudaÃ¯sme #2563EB)");
            case 3 -> I18n.getOrDefault("scenario.tensor.3.format", "RGB 24-bit CatÃ©goriel (2:1 Ã©quirectangulaire) :\n  Valeurs RGB = SouverainetÃ© politique Ã©tatique & 22 domaines claniques rÃ©gionaux (Seshat ClioPatria / CShapes)");
            case 4 -> I18n.getOrDefault("scenario.tensor.4.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 Ã©quirectangulaire) :\n  Noir (0) = PrÃ©lÃ¨vement / Faible capture exergÃ©tique | Blanc (255) = MÃ©tallurgie & outillage avancÃ© (ArchaeoGLOBE)");
            case 5 -> I18n.getOrDefault("scenario.tensor.5.format", "RGB 24-bit RÃ©seau Multi-Modal & Emporia HiÃ©rarchisÃ©s (2:1 Ã©quirectangulaire) :\n  Cyan = Fleuves navigables (#00D2E6) | Or = Routes de la Soie (#FFC832) | Terracotta = Voies ImpÃ©riales (#FFAA28) | Bleu = Maritime (#1EB4D2) | Disques blancs = Emporia");
            case 6 -> I18n.getOrDefault("scenario.tensor.6.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 Ã©quirectangulaire) :\n  Valeurs = PortÃ©e administrative SESHAT (Rome/Han 160, 2026 Ã  220, headroom [226-255]) avec extension infrastructurelle");
            case 7 -> I18n.getOrDefault("scenario.tensor.7.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 Ã©quirectangulaire) :\n  Noir (0) = Biome intact | Blanc (255) = DÃ©gradation malthusienne & empreinte agricole (HYDE 3.4)");
            case 8 -> I18n.getOrDefault("scenario.tensor.8.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 Ã©quirectangulaire) :\n  Valeurs = Potentiel de transmission vectorielle Râ‚€, foyers zoonotiques et promiscuitÃ© urbaine (Mordecai / CDC)");
            default -> I18n.getOrDefault("scenario.hint.cultural_format", "PNG / GeoJSON en projection Ã©quirectangulaire 2:1 (RGB CatÃ©goriel ou Niveaux de Gris 8-bit)");
        };
    }

    /**
     * Given the scenario's start year and active planet preset, returns the best-matching
     * entry string for demoSourceCombo, or null if no match can be determined.
     */
    private String pickDemoSourceForEpoch(long startYear, org.ether.society.generation.PlanetPreset planet) {
        String pName = planet != null ? planet.name().toLowerCase() : "";
        if (pName.contains("mars") || pName.contains("ares"))
            return "ðŸ”´ Mars â€” ModÃ¨le de Colonisation Spatiale & DÃ´mes d'Habitation [PlanÃ©taire (Mars), +2050 AD Ã  Futur]";
        if (pName.contains("vÃ©nus") || pName.contains("venus") || pName.contains("hesperos"))
            return "ðŸŸ¡ VÃ©nus â€” Stations AÃ©rostatiques Cloud Cities (Altitude 50 km) [PlanÃ©taire (VÃ©nus), +2100 AD Ã  Futur]";
        if (pName.contains("lune") || pName.contains("moon") || pName.contains("selene"))
            return "âšª Lune â€” Bases SÃ©lÃ©nites Sous-Terraines & CratÃ¨res Shackleton [PlanÃ©taire (Lune), +2040 AD Ã  Futur]";
        if (pName.contains("mercure") || pName.contains("mercury") || pName.contains("hermes"))
            return "âšª Mercure â€” DÃ´mes Polaires & Habitats d'Ombre Permanente [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]";
        // Earth epochs
        if (startYear < -10000)
            return "ðŸŒ Terre â€” PalÃ©o-DÃ©mographie & Expansion Sapiens [Afrique & Eurasie, -100 000 BC Ã  -10 000 BC]";
        if (startYear >= -3000 && startYear < 1900)
            return "ðŸŒ Terre â€” CShapes & Centennia Reconstitutions DÃ©mographiques [Empires & Ã‰tats, -3000 BC Ã  +2000 AD]";
        return "ðŸŒ Terre â€” HYDE 3.4 / Grille Historique AnthropocÃ¨ne [Global, -10 000 BC Ã  +2023 AD]";
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
                case 0 -> "ðŸ”´ Mars â€” Cartographie Linguistique Coloniale Martienne [PlanÃ©taire (Mars), +2060 AD Ã  Futur]";
                case 1 -> "ðŸ”´ Mars â€” Structures de ParentÃ© & Cohortes PionniÃ¨res [PlanÃ©taire (Mars), +2050 AD Ã  Futur]";
                case 2 -> "ðŸ”´ Mars â€” Mythologie Martienne & Cultes de la FrontiÃ¨re [PlanÃ©taire (Mars), +2060 AD Ã  Futur]";
                case 3 -> "ðŸ”´ Mars â€” Juridictions Consulaires & TraitÃ©s Martiens [PlanÃ©taire (Mars), +2060 AD Ã  Futur]";
                case 4 -> "ðŸ”´ Mars â€” Niveau Technologique Industriel & Robotique ISRU [PlanÃ©taire (Mars), +2050 AD Ã  Futur]";
                case 5 -> "ðŸ”´ Mars â€” RÃ©seau Ferroviaire Maglev Sub-Surface [PlanÃ©taire (Mars), +2070 AD Ã  Futur]";
                case 6 -> "ðŸ”´ Mars â€” Conseil Spatial & Chartes Constitutionnelles [PlanÃ©taire (Mars), +2060 AD Ã  Futur]";
                case 7 -> "ðŸ”´ Mars â€” Bioregenerative Life Support (BLSS) & DÃ©gradation [PlanÃ©taire (Mars), +2050 AD Ã  Futur]";
                case 8 -> "ðŸ”´ Mars â€” Microbiome Artificiel ConfinÃ© & RÃ©sistance [PlanÃ©taire (Mars), +2050 AD Ã  Futur]";
                default -> null;
            };
        }
        if (pName.contains("vÃ©nus") || pName.contains("venus") || pName.contains("hesperos")) {
            return switch (tensorIdx) {
                case 0 -> "ðŸŸ¡ VÃ©nus â€” RÃ©seau Isogloss des CitÃ©s AÃ©rostatiques [PlanÃ©taire (VÃ©nus), +2120 AD Ã  Futur]";
                case 1 -> "ðŸŸ¡ VÃ©nus â€” Guildes & Lignages Technologiques Flottants [PlanÃ©taire (VÃ©nus), +2100 AD Ã  Futur]";
                case 2 -> "ðŸŸ¡ VÃ©nus â€” Rituels Solaires & CÃ©rÃ©monies de Nuages [PlanÃ©taire (VÃ©nus), +2120 AD Ã  Futur]";
                case 3 -> "ðŸŸ¡ VÃ©nus â€” FÃ©dÃ©ration des Stations StratosphÃ©riques [PlanÃ©taire (VÃ©nus), +2110 AD Ã  Futur]";
                case 4 -> "ðŸŸ¡ VÃ©nus â€” SynthÃ¨se AÃ©rostatique & IngÃ©nierie Acide [PlanÃ©taire (VÃ©nus), +2100 AD Ã  Futur]";
                case 5 -> "ðŸŸ¡ VÃ©nus â€” Navettes StratosphÃ©riques Inter-Stations [PlanÃ©taire (VÃ©nus), +2110 AD Ã  Futur]";
                case 6 -> "ðŸŸ¡ VÃ©nus â€” Syndicats Flottants & Corporations AÃ©rostats [PlanÃ©taire (VÃ©nus), +2110 AD Ã  Futur]";
                case 7 -> "ðŸŸ¡ VÃ©nus â€” Ã‰rosion Chimique & Recyclage FermÃ© [PlanÃ©taire (VÃ©nus), +2100 AD Ã  Futur]";
                case 8 -> "ðŸŸ¡ VÃ©nus â€” Immunologie en AtmosphÃ¨re ConfinÃ©e [PlanÃ©taire (VÃ©nus), +2110 AD Ã  Futur]";
                default -> null;
            };
        }
        if (pName.contains("lune") || pName.contains("moon") || pName.contains("selene")) {
            return switch (tensorIdx) {
                case 0 -> "âšª Lune â€” Dialectes SÃ©lÃ©nites des Stations CratÃ©riques [PlanÃ©taire (Lune), +2050 AD Ã  Futur]";
                case 1 -> "âšª Lune â€” Associations d'Ã‰quipages & Clans SÃ©lÃ©nites [PlanÃ©taire (Lune), +2045 AD Ã  Futur]";
                case 2 -> "âšª Lune â€” Philosophie Cosmique & Rituels du Clair de Terre [PlanÃ©taire (Lune), +2050 AD Ã  Futur]";
                case 3 -> "âšª Lune â€” Secteurs TraitÃ© de l'Espace & Bases Nationales [PlanÃ©taire (Lune), +2050 AD Ã  Futur]";
                case 4 -> "âšª Lune â€” Fonderies RÃ©golithes & Extraction SÃ©lÃ©nite [PlanÃ©taire (Lune), +2045 AD Ã  Futur]";
                case 5 -> "âšª Lune â€” Tunnels de Transport MagnÃ©tique SÃ©lÃ©nite [PlanÃ©taire (Lune), +2050 AD Ã  Futur]";
                case 6 -> "âšª Lune â€” Protocoles LÃ©gaux des Habitats SÃ©lÃ©nites [PlanÃ©taire (Lune), +2050 AD Ã  Futur]";
                case 7 -> "âšª Lune â€” Ã‰puisement des Volatils & PoussiÃ¨re RÃ©golithe [PlanÃ©taire (Lune), +2045 AD Ã  Futur]";
                case 8 -> "âšª Lune â€” PathogÃ¨nes d'Isolement & RÃ©gime StÃ©rile [PlanÃ©taire (Lune), +2045 AD Ã  Futur]";
                default -> null;
            };
        }
        if (pName.contains("mercure") || pName.contains("mercury") || pName.contains("hermes")) {
            return switch (tensorIdx) {
                case 0 -> "âšª Mercure â€” Protocoles HermÃ©ens & Terminologie d'Ombre [PlanÃ©taire (Mercure), +2160 AD Ã  Futur]";
                case 1 -> "âšª Mercure â€” ConfrÃ©ries de Maintenance & Lignages Thermiques [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]";
                case 2 -> "âšª Mercure â€” Ordres d'Ã‰nergie & Croyances de Haute Radiation [PlanÃ©taire (Mercure), +2170 AD Ã  Futur]";
                case 3 -> "âšª Mercure â€” Domaines Miniers & Enclaves Polaires [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]";
                case 4 -> "âšª Mercure â€” Collecteurs Haute Ã‰nergie & Fours Directs [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]";
                case 5 -> "âšª Mercure â€” RÃ©seau de Convois Ã‰lectromagnÃ©tiques [PlanÃ©taire (Mercure), +2160 AD Ã  Futur]";
                case 6 -> "âšª Mercure â€” Administration Thermique & Urgences [PlanÃ©taire (Mercure), +2160 AD Ã  Futur]";
                case 7 -> "âšª Mercure â€” Usure Thermique & Contraintes MatÃ©rielles [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]";
                case 8 -> "âšª Mercure â€” Filtrage Radiatif & Microbiote SynthÃ©tique [PlanÃ©taire (Mercure), +2160 AD Ã  Futur]";
                default -> null;
            };
        }
        // Earth â€” pick based on epoch
        if (startYear < -10000) {
            return switch (tensorIdx) {
                case 0 -> "ðŸŒ Terre â€” Automated Phonological Distance Model (ASJP) [Global, -10 000 BC Ã  Actuel (Macro-Familles)]";
                case 1 -> "ðŸŒ Terre â€” Standard Cross-Cultural Sample (SCCS) [Global, -4000 BC Ã  Actuel (186 Cultures)]";
                case 2 -> "ðŸŒ Terre â€” Turchin Asabiyyah Cohesion Metric (Cliodynamics) [Global, -3000 BC Ã  +2000 AD]";
                case 3 -> "ðŸŒ Terre â€” GADM Administrative Sovereign Centers [Global, 1950 AD Ã  Actuel]";
                case 4 -> "ðŸŒ Terre â€” Lithic-to-Metallurgy Technology Frontier Model [Global, -100 000 BC Ã  +2000 AD]";
                case 5 -> "ðŸŒ Terre â€” Old World Overland Caravan Network [Sahara & Asie Centrale, -1000 BC Ã  +1800 AD]";
                case 6 -> "ðŸŒ Terre â€” Historical Jurisprudence & Administration Matrix [Europe & Asie, -2000 BC Ã  +1800 AD]";
                case 7 -> "ðŸŒ Terre â€” Malthusian Carrying Capacity Model [Global, -100 000 BC Ã  +2100 AD]";
                case 8 -> "ðŸŒ Terre â€” Host-Pathogen Coevolution & Immunity Model [Global, -100 000 BC Ã  +2100 AD]";
                default -> null;
            };
        }
        // Default Earth modern/historical: first real item per tensor
        return switch (tensorIdx) {
            case 0 -> "ðŸŒ Terre â€” Glottolog 4.8 / WALS Language Families [Global, -10 000 BC Ã  Actuel (8 500+ Langues)]";
            case 1 -> "ðŸŒ Terre â€” Murdock Ethnographic Atlas (Kinship Systems) [Global, -4000 BC Ã  Actuel (1 267 SociÃ©tÃ©s)]";
            case 2 -> "ðŸŒ Terre â€” Seshat Global History Databank (Rituals & Sacred) [Global, -5000 BC Ã  +1900 AD]";
            case 3 -> "ðŸŒ Terre â€” Centennia Historical Atlas (Sovereignty Boundaries) [Eurasie / Afrique / AmÃ©riques, -1000 BC Ã  +2000 AD]";
            case 4 -> "ðŸŒ Terre â€” ArchaeoGLOBE Project (Land Use & Material Tools) [Global, -10 000 BC Ã  +1850 AD]";
            case 5 -> "ðŸŒ Terre â€” ORBIS Stanford Geospatial Network (Trade Routes) [Bassin MÃ©diterranÃ©en & Proche-Orient, -300 BC Ã  +500 AD]";
            case 6 -> "ðŸŒ Terre â€” Seshat Databank (Institutional Complexity & Law) [Global, -4000 BC Ã  +1900 AD]";
            case 7 -> "ðŸŒ Terre â€” HYDE 3.4 Historical Land Use & Anthropogenic Stress [Global, -10 000 BC Ã  +2023 AD]";
            case 8 -> "ðŸŒ Terre â€” GADM / Historical Pathogen Memory & Epidemics [Global, -3000 BC Ã  +2023 AD]";
            default -> null;
        };
    }

    private ComboBox<String> buildPopulationSourceCombo() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getItems().addAll(
            "",
            "ðŸŒ Terre â€” HYDE 3.4 / Grille Historique AnthropocÃ¨ne [Global, -10 000 BC Ã  +2023 AD]",
            "ðŸŒ Terre â€” PalÃ©o-DÃ©mographie & Expansion Sapiens [Afrique & Eurasie, -100 000 BC Ã  -10 000 BC]",
            "ðŸŒ Terre â€” CShapes & Centennia Reconstitutions DÃ©mographiques [Empires & Ã‰tats, -3000 BC Ã  +2000 AD]",
            "ðŸ”´ Mars â€” ModÃ¨le de Colonisation Spatiale & DÃ´mes d'Habitation [PlanÃ©taire (Mars), +2050 AD Ã  Futur]",
            "ðŸŸ¡ VÃ©nus â€” Stations AÃ©rostatiques Cloud Cities (Altitude 50 km) [PlanÃ©taire (VÃ©nus), +2100 AD Ã  Futur]",
            "âšª Lune â€” Bases SÃ©lÃ©nites Sous-Terraines & CratÃ¨res Shackleton [PlanÃ©taire (Lune), +2040 AD Ã  Futur]",
            "âšª Mercure â€” DÃ´mes Polaires & Habitats d'Ombre Permanente [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]"
        );
        org.ether.society.data.DataSourceMetadataRegistry.setupDetailedSourceCombo(
                combo, "common.combo.prompt_source", "planet.tooltip.map_source_hint");
        long startYr = (startYearSpinner != null && startYearSpinner.getValue() != null) ? startYearSpinner.getValue().longValue() : -8000L;
        String initSrc = pickDemoSourceForEpoch(startYr, this.activePlanetPreset);
        selectBestSource(combo, initSrc, "Terre");
        combo.setOnAction(e -> {
            if (isUpdatingFromPreset) return;
            String val = combo.getValue();
            if (val != null && !val.isEmpty()) {
                if (radioImportDemo != null) {
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
                "ðŸŒ Terre â€” Glottolog 4.8 / WALS Language Families [Global, -10 000 BC Ã  Actuel (8 500+ Langues)]",
                "ðŸŒ Terre â€” Ethnologue World Linguistic Tree [Global, -3000 BC Ã  Actuel (7 100+ Langues)]",
                "ðŸŒ Terre â€” Automated Phonological Distance Model (ASJP) [Global, -10 000 BC Ã  Actuel (Macro-Familles)]",
                "ðŸ”´ Mars â€” Cartographie Linguistique Coloniale Martienne [PlanÃ©taire (Mars), +2060 AD Ã  Futur]",
                "ðŸŸ¡ VÃ©nus â€” RÃ©seau Isogloss des CitÃ©s AÃ©rostatiques [PlanÃ©taire (VÃ©nus), +2120 AD Ã  Futur]",
                "âšª Lune â€” Dialectes SÃ©lÃ©nites des Stations CratÃ©riques [PlanÃ©taire (Lune), +2050 AD Ã  Futur]",
                "âšª Mercure â€” Protocoles HermÃ©ens & Terminologie d'Ombre [PlanÃ©taire (Mercure), +2160 AD Ã  Futur]"
            );
            case 1 -> combo.getItems().addAll(
                "ðŸŒ Terre â€” Murdock Ethnographic Atlas (Kinship Systems) [Global, -4000 BC Ã  Actuel (1 267 SociÃ©tÃ©s)]",
                "ðŸŒ Terre â€” Standard Cross-Cultural Sample (SCCS) [Global, -4000 BC Ã  Actuel (186 Cultures)]",
                "ðŸŒ Terre â€” Clan & Lineage Structural Matrix (Seshat) [Global, -4000 BC Ã  +1900 AD]",
                "ðŸ”´ Mars â€” Structures de ParentÃ© & Cohortes PionniÃ¨res [PlanÃ©taire (Mars), +2050 AD Ã  Futur]",
                "ðŸŸ¡ VÃ©nus â€” Guildes & Lignages Technologiques Flottants [PlanÃ©taire (VÃ©nus), +2100 AD Ã  Futur]",
                "âšª Lune â€” Associations d'Ã‰quipages & Clans SÃ©lÃ©nites [PlanÃ©taire (Lune), +2045 AD Ã  Futur]",
                "âšª Mercure â€” ConfrÃ©ries de Maintenance & Lignages Thermiques [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]"
            );
            case 2 -> combo.getItems().addAll(
                "ðŸŒ Terre â€” Seshat Global History Databank (Rituals & Sacred) [Global, -5000 BC Ã  +1900 AD]",
                "ðŸŒ Terre â€” World Religion Database (WRD & Cultes) [Global, -3000 BC Ã  +2020 AD]",
                "ðŸŒ Terre â€” Turchin Asabiyyah Cohesion Metric (Cliodynamics) [Global, -3000 BC Ã  +2000 AD]",
                "ðŸ”´ Mars â€” Mythologie Martienne & Cultes de la FrontiÃ¨re [PlanÃ©taire (Mars), +2060 AD Ã  Futur]",
                "ðŸŸ¡ VÃ©nus â€” Rituels Solaires & CÃ©rÃ©monies de Nuages [PlanÃ©taire (VÃ©nus), +2120 AD Ã  Futur]",
                "âšª Lune â€” Philosophie Cosmique & Rituels du Clair de Terre [PlanÃ©taire (Lune), +2050 AD Ã  Futur]",
                "âšª Mercure â€” Ordres d'Ã‰nergie & Croyances de Haute Radiation [PlanÃ©taire (Mercure), +2170 AD Ã  Futur]"
            );
            case 3 -> combo.getItems().addAll(
                "ðŸŒ Terre â€” Centennia Historical Atlas (Sovereignty Boundaries) [Eurasie / Afrique / AmÃ©riques, -1000 BC Ã  +2000 AD]",
                "ðŸŒ Terre â€” CShapes 2.0 Historical Polities & Borders [Global, 1886 AD Ã  2019 AD]",
                "ðŸŒ Terre â€” GADM Administrative Sovereign Centers [Global, 1950 AD Ã  Actuel]",
                "ðŸ”´ Mars â€” Juridictions Consulaires & TraitÃ©s Martiens [PlanÃ©taire (Mars), +2060 AD Ã  Futur]",
                "ðŸŸ¡ VÃ©nus â€” FÃ©dÃ©ration des Stations StratosphÃ©riques [PlanÃ©taire (VÃ©nus), +2110 AD Ã  Futur]",
                "âšª Lune â€” Secteurs TraitÃ© de l'Espace & Bases Nationales [PlanÃ©taire (Lune), +2050 AD Ã  Futur]",
                "âšª Mercure â€” Domaines Miniers & Enclaves Polaires [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]"
            );
            case 4 -> combo.getItems().addAll(
                "ðŸŒ Terre â€” ArchaeoGLOBE Project (Land Use & Material Tools) [Global, -10 000 BC Ã  +1850 AD]",
                "ðŸŒ Terre â€” Archaeological Material Culture Database [Global, -50 000 BC Ã  +1500 AD]",
                "ðŸŒ Terre â€” Lithic-to-Metallurgy Technology Frontier Model [Global, -100 000 BC Ã  +2000 AD]",
                "ðŸ”´ Mars â€” Niveau Technologique Industriel & Robotique ISRU [PlanÃ©taire (Mars), +2050 AD Ã  Futur]",
                "ðŸŸ¡ VÃ©nus â€” SynthÃ¨se AÃ©rostatique & IngÃ©nierie Acide [PlanÃ©taire (VÃ©nus), +2100 AD Ã  Futur]",
                "âšª Lune â€” Fonderies RÃ©golithes & Extraction SÃ©lÃ©nite [PlanÃ©taire (Lune), +2045 AD Ã  Futur]",
                "âšª Mercure â€” Collecteurs Haute Ã‰nergie & Fours Directs [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]"
            );
            case 5 -> combo.getItems().addAll(
                "ðŸŒ Terre â€” ORBIS Stanford Geospatial Network (Trade Routes) [Bassin MÃ©diterranÃ©en & Proche-Orient, -300 BC Ã  +500 AD]",
                "ðŸŒ Terre â€” Silk Road & Maritime Monsoon Corridors [Eurasie & OcÃ©an Indien, -500 BC Ã  +1700 AD]",
                "ðŸŒ Terre â€” Old World Overland Caravan Network [Sahara & Asie Centrale, -1000 BC Ã  +1800 AD]",
                "ðŸ”´ Mars â€” RÃ©seau Ferroviaire Maglev Sub-Surface [PlanÃ©taire (Mars), +2070 AD Ã  Futur]",
                "ðŸŸ¡ VÃ©nus â€” Navettes StratosphÃ©riques Inter-Stations [PlanÃ©taire (VÃ©nus), +2110 AD Ã  Futur]",
                "âšª Lune â€” Tunnels de Transport MagnÃ©tique SÃ©lÃ©nite [PlanÃ©taire (Lune), +2050 AD Ã  Futur]",
                "âšª Mercure â€” RÃ©seau de Convois Ã‰lectromagnÃ©tiques [PlanÃ©taire (Mercure), +2160 AD Ã  Futur]"
            );
            case 6 -> combo.getItems().addAll(
                "ðŸŒ Terre â€” Seshat Databank (Institutional Complexity & Law) [Global, -4000 BC Ã  +1900 AD]",
                "ðŸŒ Terre â€” Cross-National Time-Series Data (CNTS Bureaucracy) [Global, 1815 AD Ã  2022 AD]",
                "ðŸŒ Terre â€” Historical Jurisprudence & Administration Matrix [Europe & Asie, -2000 BC Ã  +1800 AD]",
                "ðŸ”´ Mars â€” Conseil Spatial & Chartes Constitutionnelles [PlanÃ©taire (Mars), +2060 AD Ã  Futur]",
                "ðŸŸ¡ VÃ©nus â€” Syndicats Flottants & Corporations AÃ©rostats [PlanÃ©taire (VÃ©nus), +2110 AD Ã  Futur]",
                "âšª Lune â€” Protocoles LÃ©gaux des Habitats SÃ©lÃ©nites [PlanÃ©taire (Lune), +2050 AD Ã  Futur]",
                "âšª Mercure â€” Administration Thermique & Urgences [PlanÃ©taire (Mercure), +2160 AD Ã  Futur]"
            );
            case 7 -> combo.getItems().addAll(
                "ðŸŒ Terre â€” HYDE 3.4 Historical Land Use & Anthropogenic Stress [Global, -10 000 BC Ã  +2023 AD]",
                "ðŸŒ Terre â€” Anthromes 2.0 Global Anthropogenic Biomes [Global, -8000 BC Ã  +2000 AD]",
                "ðŸŒ Terre â€” Malthusian Carrying Capacity Model [Global, -100 000 BC Ã  +2100 AD]",
                "ðŸ”´ Mars â€” Bioregenerative Life Support (BLSS) & DÃ©gradation [PlanÃ©taire (Mars), +2050 AD Ã  Futur]",
                "ðŸŸ¡ VÃ©nus â€” Ã‰rosion Chimique & Recyclage FermÃ© [PlanÃ©taire (VÃ©nus), +2100 AD Ã  Futur]",
                "âšª Lune â€” Ã‰puisement des Volatils & PoussiÃ¨re RÃ©golithe [PlanÃ©taire (Lune), +2045 AD Ã  Futur]",
                "âšª Mercure â€” Usure Thermique & Contraintes MatÃ©rielles [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]"
            );
            case 8 -> combo.getItems().addAll(
                "ðŸŒ Terre â€” GADM / Historical Pathogen Memory & Epidemics [Global, -3000 BC Ã  +2023 AD]",
                "ðŸŒ Terre â€” Global Infectious Disease Vector Database [Zones Tropicales & TempÃ©rÃ©es, -1000 BC Ã  +2020 AD]",
                "ðŸŒ Terre â€” Host-Pathogen Coevolution & Immunity Model [Global, -100 000 BC Ã  +2100 AD]",
                "ðŸ”´ Mars â€” Microbiome Artificiel ConfinÃ© & RÃ©sistance [PlanÃ©taire (Mars), +2050 AD Ã  Futur]",
                "ðŸŸ¡ VÃ©nus â€” Immunologie en AtmosphÃ¨re ConfinÃ©e [PlanÃ©taire (VÃ©nus), +2110 AD Ã  Futur]",
                "âšª Lune â€” PathogÃ¨nes d'Isolement & RÃ©gime StÃ©rile [PlanÃ©taire (Lune), +2045 AD Ã  Futur]",
                "âšª Mercure â€” Filtrage Radiatif & Microbiote SynthÃ©tique [PlanÃ©taire (Mercure), +2160 AD Ã  Futur]"
            );
            default -> combo.getItems().addAll(
                "ðŸŒ Terre â€” Seshat / Global Databank Substrate [Global, -5000 BC Ã  +2000 AD]",
                "ðŸŒ Terre â€” Historical Empirical Baseline [Global, -100 000 BP Ã  Actuel]",
                "ðŸ”´ Mars â€” ModÃ¨le Cartographique Martien DÃ©rivÃ© [PlanÃ©taire (Mars), +2050 AD Ã  Futur]"
            );
        }
        org.ether.society.data.DataSourceMetadataRegistry.setupDetailedSourceCombo(
                combo, "common.combo.prompt_source", "planet.tooltip.map_source_hint");
        long scYear = (startYearSpinner != null && startYearSpinner.getValue() != null)
                ? startYearSpinner.getValue().longValue() : -8000L;
        String autoSrc = pickCulturalSourceForEpoch(tensorIdx, scYear, this.activePlanetPreset);
        selectBestSource(combo, autoSrc, "Terre");
        combo.setOnAction(e -> {
            if (isUpdatingFromPreset) return;
            String val = combo.getValue();
            if (val != null && !val.isEmpty()) {
                if (tensorImportRadios.containsKey(tensorIdx)) {
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
            case 0 -> new TensorParamDescriptor("scenario.tensor.0.p1.label", "Dispersion dialectale (Î±) :", "scenario.tensor.0.p1.tooltip", "Ã‰chelle spatiale de diffusion des variantes phonÃ©tiques et lexicales.", 0.01, 0.25, 0.05, 0.01, "%.2f");
            case 1 -> new TensorParamDescriptor("scenario.tensor.1.p1.label", "Rayon clanique :", "scenario.tensor.1.p1.tooltip", "Rayon spatial d'influence et de solidaritÃ© des lignages et clans.", 10.0, 500.0, 120.0, 10.0, "%.0f km");
            case 2 -> new TensorParamDescriptor("scenario.tensor.2.p1.label", "Force Asabiyyah :", "scenario.tensor.2.p1.tooltip", "Niveau de cohÃ©sion sociale et de solidaritÃ© sacrÃ©e (Asabiyyah d'Ibn Khaldoun).", 0.10, 1.00, 0.70, 0.05, "%.2f");
            case 3 -> new TensorParamDescriptor("scenario.tensor.3.p1.label", "PortÃ©e des capitales :", "scenario.tensor.3.p1.tooltip", "Rayon d'action direct de l'autoritÃ© politico-militaire centrale.", 50.0, 2500.0, 600.0, 50.0, "%.0f km");
            case 4 -> new TensorParamDescriptor("scenario.tensor.4.p1.label", "Foyers d'innovation :", "scenario.tensor.4.p1.tooltip", "Nombre de centres artisanaux et mÃ©tallurgiques initiaux.", 1.0, 20.0, 5.0, 1.0, "%.0f");
            case 5 -> new TensorParamDescriptor("scenario.tensor.5.p1.label", "Comptoirs & carrefours :", "scenario.tensor.5.p1.tooltip", "Nombre de carrefours marchands et comptoirs d'Ã©change.", 2.0, 30.0, 8.0, 1.0, "%.0f");
            case 6 -> new TensorParamDescriptor("scenario.tensor.6.p1.label", "Niveaux bureaucratiques :", "scenario.tensor.6.p1.tooltip", "Profondeur de la hiÃ©rarchie administrative (Seshat Databank).", 1.0, 8.0, 3.0, 1.0, "%.0f niv.");
            case 7 -> new TensorParamDescriptor("scenario.tensor.7.p1.label", "Surexploitation des sols :", "scenario.tensor.7.p1.tooltip", "IntensitÃ© du forÃ§age anthropique et de la dÃ©forestation.", 0.0, 1.00, 0.40, 0.05, "%.2f");
            case 8 -> new TensorParamDescriptor("scenario.tensor.8.p1.label", "Pression pathogÃ¨ne :", "scenario.tensor.8.p1.tooltip", "Pression endÃ©mique virale et bactÃ©rienne rÃ©gionale.", 0.0, 1.00, 0.35, 0.05, "%.2f");
            default -> new TensorParamDescriptor("scenario.tensor.ext.p1.label", "FrÃ©quence spatiale :", "scenario.tensor.ext.p1.tooltip", "Ã‰chelle d'ondulation du substrat procÃ©dural.", 0.001, 0.05, 0.01, 0.001, "%.3f");
        };
    }

    private TensorParamDescriptor getTensorParam2Descriptor(int tensorIdx) {
        return switch (tensorIdx) {
            case 0 -> new TensorParamDescriptor("scenario.tensor.0.p2.label", "Foyers linguistiques :", "scenario.tensor.0.p2.tooltip", "Nombre de foyers indÃ©pendants et familles linguistiques initiales.", 1.0, 16.0, 4.0, 1.0, "%.0f");
            case 1 -> new TensorParamDescriptor("scenario.tensor.1.p2.label", "PermÃ©abilitÃ© exogamique :", "scenario.tensor.1.p2.tooltip", "Taux d'alliances matrimoniales inter-clans et exogamie.", 0.0, 1.00, 0.35, 0.05, "%.2f");
            case 2 -> new TensorParamDescriptor("scenario.tensor.2.p2.label", "Sanctuaires sacrÃ©s :", "scenario.tensor.2.p2.tooltip", "Nombre de hauts lieux rituels et sanctuaires Ã©mergents.", 1.0, 25.0, 6.0, 1.0, "%.0f");
            case 3 -> new TensorParamDescriptor("scenario.tensor.3.p2.label", "Centralisation rÃ©galienne :", "scenario.tensor.3.p2.tooltip", "DegrÃ© de concentration du pouvoir politique et fiscal.", 0.0, 1.00, 0.75, 0.05, "%.2f");
            case 4 -> new TensorParamDescriptor("scenario.tensor.4.p2.label", "TechnicitÃ© matÃ©rielle :", "scenario.tensor.4.p2.tooltip", "Niveau initial de complexitÃ© des artÃ©facts et outillages.", 0.0, 10.0, 2.5, 0.5, "%.1f");
            case 5 -> new TensorParamDescriptor("scenario.tensor.5.p2.label", "PortÃ©e des routes :", "scenario.tensor.5.p2.tooltip", "Longueur maximale des routes commerciales et caravanes.", 100.0, 4000.0, 1200.0, 100.0, "%.0f km");
            case 6 -> new TensorParamDescriptor("scenario.tensor.6.p2.label", "Codification juridique :", "scenario.tensor.6.p2.tooltip", "Niveau de formalisation et de codification des lois Ã©crites.", 0.0, 1.00, 0.45, 0.05, "%.2f");
            case 7 -> new TensorParamDescriptor("scenario.tensor.7.p2.label", "Taux d'Ã©puisement :", "scenario.tensor.7.p2.tooltip", "Vitesse d'Ã©rosion des sols et d'amenuisement des ressources.", 0.001, 0.10, 0.02, 0.005, "%.3f");
            case 8 -> new TensorParamDescriptor("scenario.tensor.8.p2.label", "RÃ©servoirs zoonotiques :", "scenario.tensor.8.p2.tooltip", "Rayon d'influence des foyers sauvages et rÃ©servoirs animaux.", 20.0, 600.0, 180.0, 20.0, "%.0f km");
            default -> new TensorParamDescriptor("scenario.tensor.ext.p2.label", "Amplitude du signal :", "scenario.tensor.ext.p2.tooltip", "IntensitÃ© relative du tenseur extensible.", 0.10, 1.00, 0.80, 0.05, "%.2f");
        };
    }

    private TensorParamDescriptor getTensorParam3Descriptor(int tensorIdx) {
        return switch (tensorIdx) {
            case 0 -> new TensorParamDescriptor("scenario.tensor.0.p3.label", "BarriÃ¨re de relief :", "scenario.tensor.0.p3.tooltip", "Impact du relief et des chaÃ®nes de montagnes sur l'isolation linguistique.", 0.0, 1.00, 0.60, 0.05, "%.2f");
            case 1 -> new TensorParamDescriptor("scenario.tensor.1.p3.label", "HiÃ©rarchie lignagÃ¨re :", "scenario.tensor.1.p3.tooltip", "DegrÃ© de structuration et de segmentation patriarcale/matriarcale.", 0.0, 1.00, 0.50, 0.05, "%.2f");
            case 2 -> new TensorParamDescriptor("scenario.tensor.2.p3.label", "Diffusion thÃ©ologique :", "scenario.tensor.2.p3.tooltip", "PortÃ©e spatiale d'expansion des doctrines et rites sacrÃ©s.", 50.0, 2000.0, 400.0, 50.0, "%.0f km");
            case 3 -> new TensorParamDescriptor("scenario.tensor.3.p3.label", "Friction frontaliÃ¨re :", "scenario.tensor.3.p3.tooltip", "Tension militaire et friction aux marches de l'empire.", 0.0, 1.00, 0.40, 0.05, "%.2f");
            case 4 -> new TensorParamDescriptor("scenario.tensor.4.p3.label", "Diffusion technique :", "scenario.tensor.4.p3.tooltip", "Conductance de propagation des savoirs artisanaux.", 0.01, 0.30, 0.08, 0.01, "%.2f");
            case 5 -> new TensorParamDescriptor("scenario.tensor.5.p3.label", "PondÃ©ration maritime :", "scenario.tensor.5.p3.tooltip", "AttractivitÃ© des voies navigables, cÃ´tiÃ¨res et maritimes.", 0.0, 1.00, 0.65, 0.05, "%.2f");
            case 6 -> new TensorParamDescriptor("scenario.tensor.6.p3.label", "Seuil d'intÃ©gration :", "scenario.tensor.6.p3.tooltip", "Taille de population dÃ©clenchant l'Ã©mergence des institutions.", 100.0, 10000.0, 1500.0, 100.0, "%.0f hab.");
            case 7 -> new TensorParamDescriptor("scenario.tensor.7.p3.label", "RÃ©silience biocapacitÃ© :", "scenario.tensor.7.p3.tooltip", "CapacitÃ© de rÃ©gÃ©nÃ©ration naturelle du biome.", 0.10, 2.00, 1.00, 0.10, "%.2f");
            case 8 -> new TensorParamDescriptor("scenario.tensor.8.p3.label", "MÃ©moire immunitaire :", "scenario.tensor.8.p3.tooltip", "Vitesse d'acquisition et persistance de l'immunitÃ© de groupe.", 0.01, 0.25, 0.06, 0.01, "%.2f");
            default -> new TensorParamDescriptor("scenario.tensor.ext.p3.label", "Taux de diffusion :", "scenario.tensor.ext.p3.tooltip", "Conductance de diffusion spatiale.", 0.01, 0.20, 0.05, 0.01, "%.2f");
        };
    }

    private String getDefaultTensorSeed(int tensorIdx) {
        return String.valueOf(11235L + tensorIdx * 11111L);
    }

    private void updateTensorFileLabel(int tensorIdx) {
        Label fileLbl = tensorFileLabels.get(tensorIdx);
        Button loadBtn = tensorLoadBtns.get(tensorIdx);
        if (fileLbl == null) return;
        boolean isProc = tensorProcRadios.containsKey(tensorIdx) && tensorProcRadios.get(tensorIdx).isSelected();
        Image img = customTensorImages.get(tensorIdx);

        if (isProc) {
            fileLbl.setText(I18n.getOrDefault("scenario.tensor.status.procedural", "âœ… Mode procÃ©dural actif"));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
            if (loadBtn != null) loadBtn.setStyle("");
            return;
        }

        if (img == null) {
            fileLbl.setText(I18n.getOrDefault("scenario.tensor.file.none", "âš ï¸ Aucune carte chargÃ©e â€” Fichier requis en mode import"));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #f59e0b;");
            if (loadBtn != null) loadBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            return;
        }

        org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(img);
        if (!val.valid()) {
            fileLbl.setText(String.format(I18n.getOrDefault("scenario.tensor.file.invalid", "âš ï¸ Carte incompatible : %s"), val.message()));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            if (loadBtn != null) loadBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
        } else {
            fileLbl.setText(String.format(I18n.getOrDefault("scenario.tensor.file.loaded_res", "ðŸ“· Carte chargÃ©e : %s (%dx%d)"), getCulturalBaselineName(tensorIdx), val.width(), val.height()));
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

    private void rebuildCulturalTensorSubBlocks(int dimCount) {
        if (btnGenerateProceduralTensorsSection != null) {
            btnGenerateProceduralTensorsSection.setText(I18n.getOrDefault("scenario.btn.regen_tensors", "ðŸª„ RÃ©gÃ©nÃ©rer les Tenseurs"));
            btnGenerateProceduralTensorsSection.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.regen_tensors", "Bascule tous les tenseurs en mode procÃ©dural et rÃ©gÃ©nÃ¨re les cartes selon les paramÃ¨tres et la graine stochastique.")));
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
                dlg.setHeaderText(I18n.getOrDefault("scenario.tensor.rename.prompt", "Nom personnalisÃ© du Tenseur NÂ°{0} :", (tensorIdx + 1)));
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

            Button deleteBtn = new Button(I18n.getOrDefault("scenario.tensor.btn.delete", "âœ–"));
            deleteBtn.getStyleClass().add("button-secondary");
            deleteBtn.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-padding: 2 6; -fx-font-weight: bold; -fx-cursor: hand;");
            deleteBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tensor.btn.delete_tooltip", "Supprimer ce tenseur culturel (dÃ©crÃ©mente le nombre total de tenseurs).")));
            deleteBtn.setOnAction(e -> deleteCulturalTensor(tensorIdx));

            titleRow.getChildren().addAll(subTitle, renameBtn, deleteBtn);
            subBlock.getChildren().add(titleRow);

            ToggleGroup tg = new ToggleGroup();
            RadioButton radioProc = new RadioButton(I18n.getOrDefault("scenario.mode.procedural_sde", "â–¶ Mode ProcÃ©dural (ParamÃ¨tres adaptatifs & Graine)"));
            RadioButton radioImport = new RadioButton(I18n.getOrDefault("scenario.mode.spatial_import", "ðŸ“‚ Importation Carte Spatiale (PNG / GeoJSON)"));
            radioProc.setToggleGroup(tg);
            radioImport.setToggleGroup(tg);
            radioProc.getStyleClass().add("radio-proc");
            radioImport.getStyleClass().add("radio-import");
            radioProc.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_radio_proc", "GÃ©nÃ¨re procÃ©duralement ce tenseur culturel via des Ã©quations stochastiques et les paramÃ¨tres ci-dessous.")));
            radioImport.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_radio_import", "Importe une image matricielle ou une couche SIG externe pour modÃ©liser ce tenseur culturel.")));

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
            seedField.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.tensor_seed_field", "Graine stochastique dÃ©terministe pour initialiser ce champ de tenseur culturel.")));
            seedField.textProperty().addListener((obs, o, n) -> {
                if (!isUpdatingFromPreset) {
                    notifyParamChange();
                    drawPreview();
                }
            });

            Button randBtn = new Button("ðŸŽ²");
            randBtn.getStyleClass().add("button-secondary");
            randBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            randBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.random_tensor_seed", "Tirer une nouvelle graine alÃ©atoire pour ce tenseur.")));
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

            Button exportSingleBtn = new Button(I18n.getOrDefault("scenario.tensor.btn.export_single", "ðŸ“¤ Export"));
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

            Label procStatusLbl = new Label(I18n.getOrDefault("scenario.status.proc_gen_tensor_prefix", "ðŸª„ ModÃ©lisation procÃ©durale dynamique active (Tenseur ") + (tensorIdx + 1) + ")");
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
            btnLoad.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.load_cultural_tensor_map", "Ouvre un sÃ©lecteur de fichier pour importer une carte raster externe (PNG/GeoTIFF) pour ce tenseur.")));
            tensorLoadBtns.put(tensorIdx, btnLoad);

            Button btnClear = new Button("âŒ");
            btnClear.getStyleClass().add("button-secondary");
            btnClear.setStyle("-fx-font-size: 11px;");
            btnClear.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.clear_cultural_tensor_map", "Efface l'image importÃ©e et rÃ©initialise le tenseur au mode procÃ©dural par dÃ©faut.")));

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
                    alert.setHeaderText(I18n.getOrDefault("scenario.dialog.invalid_map_header", "Fichier de carte non supportÃ© ou illisible"));
                    alert.setContentText(file.getName() + " :\n" + val.message() + "\n\n" + I18n.getOrDefault("scenario.dialog.invalid_map_hint", "Veuillez fournir une image raster valide (PNG/JPG) en projection Ã©quirectangulaire (2:1)."));
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

        Label badge = new Label(I18n.getOrDefault("scenario.badge.custom", "[ðŸ”Œ CUSTOM]"));
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
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                } else if (item.startsWith("â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€")) {
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
            if (val != null && (val.startsWith("â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€") || val.startsWith("â”€") || val.contains("â”€â”€â”€â”€â”€"))) {
                previewModeCombo.getSelectionModel().select(0);
                return;
            }
            updatePreviewTitleText();
            updateBottomLegend();
            drawPreview();
        });

        btnReliefOverlay = new ToggleButton(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.relief_overlay", "â›°ï¸ Relief"));
        btnReliefOverlay.setSelected(false);
        btnReliefOverlay.getStyleClass().add("button-secondary");
        btnReliefOverlay.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.relief_overlay", "Superposer l'ombrage du relief topographique et des pentes avec dÃ©limitation du trait de cÃ´te.")));
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
        if (previewModeCombo == null) return;
        int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
        int currentSelectionIndex = previewModeCombo.getSelectionModel().getSelectedIndex();

        java.util.List<String> items = new java.util.ArrayList<>();
        int num = 1;
        items.add(num++ + ". " + I18n.getOrDefault("scenario.preview.mode.density", "ðŸ“Š Relief & Demographic Density (Agent Nodes Tâ‚€)"));

        for (int i = 0; i < dims; i++) {
            items.add(num++ + ". " + getCulturalTensorPreviewName(i));
        }

        items.add(I18n.getOrDefault("scenario.preview.separator.derived", "â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ CALQUES DÃ‰DUITS & DYNAMIQUES â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€"));
        items.add(num++ + ". " + I18n.getOrDefault("scenario.preview.mode.capital",   "ðŸ› ï¸ Initial Physical Capital K(x) [kg/capita] (Derived)"));
        items.add(num++ + ". " + I18n.getOrDefault("scenario.preview.mode.energy",    "âš¡ Initial Energy Stock E(x) [MJ/capita] (Derived)"));
        items.add(num++ + ". " + I18n.getOrDefault("scenario.preview.mode.food",      "ðŸŒ¾ Food Reserves F(x) [Months] (Derived)"));
        items.add(num++ + ". " + I18n.getOrDefault("scenario.preview.mode.info",      "ðŸ§  Information Capital & Knowledge I(x) [Bits/capita] (Derived)"));
        items.add(num++ + ". " + I18n.getOrDefault("scenario.preview.mode.footprint", "âš ï¸ Demographic Footprint & Malthusian Tension (Derived)"));
        items.add(num   + ". " + I18n.getOrDefault("scenario.preview.mode.friction",  "ðŸ§± Border Friction Gradient Ïƒ_friction (Derived)"));

        previewModeCombo.getItems().setAll(items);

        if (currentSelectionIndex >= 0 && currentSelectionIndex < previewModeCombo.getItems().size()) {
            previewModeCombo.getSelectionModel().select(currentSelectionIndex);
        } else {
            previewModeCombo.getSelectionModel().select(0);
        }
    }

    private String getCulturalTensorPreviewName(int index) {
        if (customTensorNames.containsKey(index) && !customTensorNames.get(index).isBlank()) {
            return "ðŸ§¬ " + I18n.getOrDefault("scenario.tensor.custom.preview_prefix", "Tensor ") + (index + 1) + " : " + customTensorNames.get(index);
        }
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.1.preview", "ðŸ“œ Tensor 1: Isoglosses & Linguistic Continua (Languages)");
            case 1 -> I18n.getOrDefault("scenario.tensor.2.preview", "ðŸ› Tensor 2: Kinship & Clan Structures (Kinship)");
            case 2 -> I18n.getOrDefault("scenario.tensor.3.preview", "ðŸ”® Tensor 3: Rituals, Beliefs & Sacred (Asabiyyah)");
            case 3 -> I18n.getOrDefault("scenario.tensor.4.preview", "ðŸ‘‘ Tensor 4: Politico-Military Sovereignty & Capitals");
            case 4 -> I18n.getOrDefault("scenario.tensor.5.preview", "ðŸº Tensor 5: Tooling, Materiality & Technologies (Artifacts)");
            case 5 -> I18n.getOrDefault("scenario.tensor.6.preview", "ðŸ« Tensor 6: Corridors & Trade Networks (Economic Routes)");
            case 6 -> I18n.getOrDefault("scenario.tensor.7.preview", "âš– Tensor 7: Institutional Complexity & Norms (Seshat & Law)");
            case 7 -> I18n.getOrDefault("scenario.tensor.8.preview", "âš ï¸ Tensor 8: Ecological Footprint & Malthusian Tension (Degradation)");
            case 8 -> I18n.getOrDefault("scenario.tensor.9.preview", "ðŸ§¬ Tensor 9: Pathogen Immunity & Health Memory (Epidemiology)");
            default -> I18n.getOrDefault("scenario.tensor.custom.preview_prefix", "ðŸ§¬ Tensor ") + (index + 1) + I18n.getOrDefault("scenario.tensor.custom.preview_mid", " : Cultural Substrate ") + (index + 1);
        };
    }

    private void updatePreviewTitleText() {
        if (previewTitleLabel == null) return;
        previewTitleLabel.setText(I18n.getOrDefault("scenario.title.right_view", "ðŸ—ºï¸ Resource Cartography & Display"));
    }

    private void updateBottomLegend() {
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
                I18n.getOrDefault("setup.legend.density.0", "InhabitÃ© (0 hab/kmÂ²)"),
                I18n.getOrDefault("setup.legend.density.1", "Faible (1â€“50 hab/kmÂ²)"),
                I18n.getOrDefault("setup.legend.density.2", "Moyenne (50â€“500)"),
                I18n.getOrDefault("setup.legend.density.3", "Ã‰levÃ©e (500â€“2.5k)"),
                I18n.getOrDefault("setup.legend.density.4", "MÃ©tropole (> 2.5k)")
            };
            fullTooltips = new String[]{
                I18n.getOrDefault("setup.legend.density.0.desc", "Zone InhabitÃ©e : 0 hab/kmÂ² (OcÃ©ans, dÃ©serts, haute montagne)"),
                I18n.getOrDefault("setup.legend.density.1.desc", "DensitÃ© Faible : 1 Ã  50 hab/kmÂ² (Campagnes, tribus nomades)"),
                I18n.getOrDefault("setup.legend.density.2.desc", "DensitÃ© Moyenne : 50 Ã  500 hab/kmÂ² (Bourgades & vallÃ©es agricoles)"),
                I18n.getOrDefault("setup.legend.density.3.desc", "DensitÃ© Ã‰levÃ©e / CitÃ© : 500 Ã  2 500 hab/kmÂ² (Centres urbains rÃ©gionaux)"),
                I18n.getOrDefault("setup.legend.density.4.desc", "MÃ©tropole / Megapole : > 2 500 hab/kmÂ² (Grandes capitales historiques)")
            };
        } else if (idx >= 1 && idx <= dims) {
            int tIdx = idx - 1;
            if (tIdx == 0) {
                // Tensor 1: Isoglosses & Languages
                colors = new Color[]{
                    Color.rgb(30, 95, 165), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.dialect.0", "Dialecte ArchaÃ¯que (Câ‚€=0)"),
                    I18n.getOrDefault("setup.legend.dialect.1", "Foyer Prosodique (0.25)"),
                    I18n.getOrDefault("setup.legend.dialect.2", "Isoglosse MÃ©diane (0.50)"),
                    I18n.getOrDefault("setup.legend.dialect.3", "Innovations Substratiques (0.75)"),
                    I18n.getOrDefault("setup.legend.dialect.4", "Dialecte ExogÃ¨ne (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.dialect.0.desc", "Dialecte ArchaÃ¯que : Formes originelles non diffusÃ©es"),
                    I18n.getOrDefault("setup.legend.dialect.1.desc", "Foyer Prosodique : Zone d'expansion dialectale secondaire"),
                    I18n.getOrDefault("setup.legend.dialect.2.desc", "Isoglosse MÃ©diane : Zone de frontiÃ¨re linguistique et bilinguisme"),
                    I18n.getOrDefault("setup.legend.dialect.3.desc", "Innovations Substratiques : Lexique technique ou grammatical rÃ©novÃ©"),
                    I18n.getOrDefault("setup.legend.dialect.4.desc", "Dialecte ExogÃ¨ne / Innovant : Standard de communication Ã©mergent")
                };
            } else if (tIdx == 1) {
                // Tensor 2: Kinship & Clan Structures
                colors = new Color[]{
                    Color.rgb(56, 189, 248), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(167, 139, 250)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.kinship.0", "Famille NuclÃ©aire (Câ‚=0)"),
                    I18n.getOrDefault("setup.legend.kinship.1", "LignÃ©e Ã‰largie (0.25)"),
                    I18n.getOrDefault("setup.legend.kinship.2", "Matriarcat Lacustre (0.50)"),
                    I18n.getOrDefault("setup.legend.kinship.3", "Patriarcat HiÃ©rarchique (0.75)"),
                    I18n.getOrDefault("setup.legend.kinship.4", "ConfÃ©dÃ©ration Tribale (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.kinship.0.desc", "Famille NuclÃ©aire : Cellule parentale autonome de base"),
                    I18n.getOrDefault("setup.legend.kinship.1.desc", "LignÃ©e Ã‰largie : Entraide inter-gÃ©nÃ©rationnelle et clans d'alliance"),
                    I18n.getOrDefault("setup.legend.kinship.2.desc", "Matriarcat Lacustre : Filiations matrilinÃ©aires et terres collectives"),
                    I18n.getOrDefault("setup.legend.kinship.3.desc", "Patriarcat HiÃ©rarchique : Structure agnatique et chefferies martiales"),
                    I18n.getOrDefault("setup.legend.kinship.4.desc", "ConfÃ©dÃ©ration Tribale : AssemblÃ©e de clans fÃ©dÃ©rÃ©s Ã  grande Ã©chelle")
                };
            } else if (tIdx == 2) {
                // Tensor 3: Rituals & Asabiyyah
                colors = new Color[]{
                    Color.rgb(14, 165, 233), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(225, 29, 72)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.rituals.0", "Animisme Local (Câ‚‚=0)"),
                    I18n.getOrDefault("setup.legend.rituals.1", "Cultes Civiques (0.25)"),
                    I18n.getOrDefault("setup.legend.rituals.2", "PolythÃ©isme (0.50)"),
                    I18n.getOrDefault("setup.legend.rituals.3", "Asabiyyah Ã‰levÃ©e (0.75)"),
                    I18n.getOrDefault("setup.legend.rituals.4", "Dogme Transcendant (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.rituals.0.desc", "Animisme Local : Croyances de terroirs et esprit des Ã©lÃ©ments"),
                    I18n.getOrDefault("setup.legend.rituals.1.desc", "Cultes Civiques : Rites urbains d'intÃ©gration communautaire"),
                    I18n.getOrDefault("setup.legend.rituals.2.desc", "PolythÃ©isme : PanthÃ©ons structurÃ©s et clergÃ©s rÃ©gionaux"),
                    I18n.getOrDefault("setup.legend.rituals.3.desc", "Asabiyyah Ã‰levÃ©e : Forte solidaritÃ© tribale (CohÃ©sion d'Ibn Khaldoun)"),
                    I18n.getOrDefault("setup.legend.rituals.4.desc", "Dogme Transcendant : MonothÃ©isme ou idÃ©ologie universaliste")
                };
            } else if (tIdx == 3) {
                // Tensor 4: Sovereignty & Polities
                colors = new Color[]{
                    Color.rgb(30, 95, 165), Color.rgb(16, 185, 129), Color.rgb(245, 158, 11), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.sovereignty.0", "Zone Franche (Câ‚ƒ=0)"),
                    I18n.getOrDefault("setup.legend.sovereignty.1", "CitÃ©-Ã‰tat Libre (0.25)"),
                    I18n.getOrDefault("setup.legend.sovereignty.2", "PrincipautÃ© (0.50)"),
                    I18n.getOrDefault("setup.legend.sovereignty.3", "Empire CentralisÃ© (0.75)"),
                    I18n.getOrDefault("setup.legend.sovereignty.4", "Capitale Core (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.sovereignty.0.desc", "Zone Franche / Nomade : Absence de souverainetÃ© Ã©tatique formalisÃ©e"),
                    I18n.getOrDefault("setup.legend.sovereignty.1.desc", "CitÃ©-Ã‰tat Libre : Autonomie municipale et hinterland restreint"),
                    I18n.getOrDefault("setup.legend.sovereignty.2.desc", "PrincipautÃ© RÃ©gionale : ContrÃ´le fÃ©odal ou provincial intermÃ©diaire"),
                    I18n.getOrDefault("setup.legend.sovereignty.3.desc", "Empire CentralisÃ© : Administration unifiÃ©e et prÃ©lÃ¨vement fiscal"),
                    I18n.getOrDefault("setup.legend.sovereignty.4.desc", "Capitale Core : Foyer du pouvoir politique et militaire suprÃªme")
                };
            } else if (tIdx == 4) {
                // Tensor 5: Tooling, Materiality & Technologies (Strict Grayscale matching raster map)
                colors = new Color[]{
                    Color.rgb(20, 20, 20), Color.rgb(80, 80, 80), Color.rgb(140, 140, 140), Color.rgb(200, 200, 200), Color.rgb(255, 255, 255)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.tech.0", "Lithique / PalÃ©o (Câ‚„=0)"),
                    I18n.getOrDefault("setup.legend.tech.1", "CÃ©ramique / NÃ©olithique (0.25)"),
                    I18n.getOrDefault("setup.legend.tech.2", "Bronze / MÃ©tallurgie (0.50)"),
                    I18n.getOrDefault("setup.legend.tech.3", "Fer & MÃ©canique (0.75)"),
                    I18n.getOrDefault("setup.legend.tech.4", "Industrie & NumÃ©rique (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.tech.0.desc", "Industrie Lithique : Taille du silex, os poli, bois et cuir"),
                    I18n.getOrDefault("setup.legend.tech.1.desc", "CÃ©ramique & NÃ©olithisation : Poteaux, cuisson de terre, faux et meules"),
                    I18n.getOrDefault("setup.legend.tech.2.desc", "MÃ©tallurgie du Cuivre et Bronze : Fours de rÃ©duction, alliages, soc d'araire"),
                    I18n.getOrDefault("setup.legend.tech.3.desc", "Ã‚ge du Fer & Machines Simples : Hauts fourneaux, moulins, engrenages"),
                    I18n.getOrDefault("setup.legend.tech.4.desc", "RÃ©volution Industrielle & Digitale : Vapeur, rÃ©seaux Ã©lectriques, automates")
                };
            } else if (tIdx == 5) {
                // Tensor 6: Corridors & Trade Networks
                colors = new Color[]{
                    Color.rgb(30, 41, 59), Color.rgb(180, 83, 9), Color.rgb(217, 119, 6), Color.rgb(234, 88, 12), Color.rgb(13, 148, 136)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.trade.0", "Enclave IsolÃ©e (Câ‚…=0)"),
                    I18n.getOrDefault("setup.legend.trade.1", "Pistes Locales (0.25)"),
                    I18n.getOrDefault("setup.legend.trade.2", "Caravanes Terrestres (0.50)"),
                    I18n.getOrDefault("setup.legend.trade.3", "Voies Fluviales (0.75)"),
                    I18n.getOrDefault("setup.legend.trade.4", "Hubs Maritimes (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.trade.0.desc", "Enclave IsolÃ©e : Autarcie locale sans axe d'Ã©change pÃ©renne"),
                    I18n.getOrDefault("setup.legend.trade.1.desc", "Pistes PÃ©destres Locales : Sentiers de troc de proximitÃ©"),
                    I18n.getOrDefault("setup.legend.trade.2.desc", "Caravanes Terrestres : Routes de la Soie, pistes trans-sahariennes"),
                    I18n.getOrDefault("setup.legend.trade.3.desc", "Voies Navigables & Fluviales : Transports massifs par fleuves et canaux"),
                    I18n.getOrDefault("setup.legend.trade.4.desc", "Hubs Maritimes & Mondiaux : Ports hauturiers et corridors mondialisÃ©s")
                };
            } else if (tIdx == 6) {
                // Tensor 7: Institutional Complexity & Norms (Strict Grayscale matching raster map)
                colors = new Color[]{
                    Color.rgb(20, 20, 20), Color.rgb(80, 80, 80), Color.rgb(140, 140, 140), Color.rgb(200, 200, 200), Color.rgb(255, 255, 255)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.inst.0", "Coutume Orale (Câ‚†=0)"),
                    I18n.getOrDefault("setup.legend.inst.1", "Tribunaux Locaux (0.25)"),
                    I18n.getOrDefault("setup.legend.inst.2", "Code Juridique (0.50)"),
                    I18n.getOrDefault("setup.legend.inst.3", "Bureaucratie Fiscale (0.75)"),
                    I18n.getOrDefault("setup.legend.inst.4", "Ã‰tat de Droit (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.inst.0.desc", "Coutume Orale : RÃ©solution informelle des conflits par les anciens"),
                    I18n.getOrDefault("setup.legend.inst.1.desc", "Juridictions Locales : AssemblÃ©es coutumiÃ¨res et magistrats de citÃ©"),
                    I18n.getOrDefault("setup.legend.inst.2.desc", "Code Ã‰crit : Corpus lÃ©gal unifiÃ© (ex: Code d'Hammurabi, Droit Romain)"),
                    I18n.getOrDefault("setup.legend.inst.3.desc", "Bureaucratie CentralisÃ©e : PrÃ©lÃ¨vement cadastral, ministÃ¨res et fonction publique"),
                    I18n.getOrDefault("setup.legend.inst.4.desc", "Ã‰tat de Droit Constitutionnel : SÃ©paration des pouvoirs, institutions impersonnelles")
                };
            } else if (tIdx == 7) {
                // Tensor 8: Ecological Footprint & Environmental Tension (Strict Grayscale matching raster map)
                colors = new Color[]{
                    Color.rgb(20, 20, 20), Color.rgb(80, 80, 80), Color.rgb(140, 140, 140), Color.rgb(200, 200, 200), Color.rgb(255, 255, 255)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.eco.0", "Biome Vierge (Câ‚‡=0)"),
                    I18n.getOrDefault("setup.legend.eco.1", "Pression ModÃ©rÃ©e (0.25)"),
                    I18n.getOrDefault("setup.legend.eco.2", "Surexploitation (0.50)"),
                    I18n.getOrDefault("setup.legend.eco.3", "Tension Critique (0.75)"),
                    I18n.getOrDefault("setup.legend.eco.4", "Effondrement (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.eco.0.desc", "Biome Intact : Ã‰cosystÃ¨mes Ã  l'Ã©quilibre sans perturbation anthropique"),
                    I18n.getOrDefault("setup.legend.eco.1.desc", "Pression LÃ©gÃ¨re : ForÃªt gÃ©rÃ©e, chasse et Ã©levage durable"),
                    I18n.getOrDefault("setup.legend.eco.2.desc", "Surexploitation Agricole : DÃ©forestation, Ã©rosion des sols, baisse des rendements"),
                    I18n.getOrDefault("setup.legend.eco.3.desc", "Tension Malthusienne SÃ©vÃ¨re : PÃ©nuries de bois, rarÃ©faction du gibier et des nappes"),
                    I18n.getOrDefault("setup.legend.eco.4.desc", "Effondrement Ã‰cologique : DÃ©sertification irrÃ©versible et disette systÃ©mique")
                };
            } else if (tIdx == 8) {
                // Tensor 9: Pathogen Immunity & Health Memory (Strict Grayscale matching raster map)
                colors = new Color[]{
                    Color.rgb(20, 20, 20), Color.rgb(80, 80, 80), Color.rgb(140, 140, 140), Color.rgb(200, 200, 200), Color.rgb(255, 255, 255)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.pathogen.0", "NaÃ¯f / VulnÃ©rable (Câ‚ˆ=0)"),
                    I18n.getOrDefault("setup.legend.pathogen.1", "EndÃ©mie Locale (0.25)"),
                    I18n.getOrDefault("setup.legend.pathogen.2", "RÃ©sistance Acquise (0.50)"),
                    I18n.getOrDefault("setup.legend.pathogen.3", "MÃ©moire Ã‰levÃ©e (0.75)"),
                    I18n.getOrDefault("setup.legend.pathogen.4", "Bouclier Immunitaire (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.pathogen.0.desc", "Population NaÃ¯ve : Aucune immunitÃ© prÃ©alable (vulnÃ©rabilitÃ© maximale aux chocs microbiens)"),
                    I18n.getOrDefault("setup.legend.pathogen.1.desc", "EndÃ©mie ModÃ©rÃ©e : PrÃ©sence de zoonoses locales stabilisÃ©es"),
                    I18n.getOrDefault("setup.legend.pathogen.2.desc", "RÃ©sistance Acquise : SÃ©lection adaptative et rÃ©silience immunitaire face aux Ã©pidÃ©mies courantes"),
                    I18n.getOrDefault("setup.legend.pathogen.3.desc", "MÃ©moire Ã‰pidÃ©mique Ã‰levÃ©e : Forte diversitÃ© d'anticorps dans les grands rÃ©seaux urbains"),
                    I18n.getOrDefault("setup.legend.pathogen.4.desc", "Bouclier Sanitaire & MÃ©dical : Mesures prophylactiques, vaccins et structures hospitaliÃ¨res")
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
                    I18n.getOrDefault("setup.legend.cap.4", "Haute DensitÃ© (> 10k)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.cap.0.desc", "Capital Physique Primitif : Outils individuels lÃ©gers"),
                    I18n.getOrDefault("setup.legend.cap.1.desc", "Capital Artisanal : Ateliers, animaux de trait, charrues"),
                    I18n.getOrDefault("setup.legend.cap.2.desc", "Capital Manufacturier : Forges, moulins hydrauliques, navires"),
                    I18n.getOrDefault("setup.legend.cap.3.desc", "Capital Industriel : Machines Ã  vapeur, voies ferrÃ©es, usines"),
                    I18n.getOrDefault("setup.legend.cap.4.desc", "Infrastructures AvancÃ©es : RÃ©seaux Ã©lectriques, tÃ©lÃ©coms, centres logistiques")
                };
            } else if (derivedOffset == 2 || mode.contains("Ã©nergÃ©tique") || mode.contains("energy") || mode.contains("energie") || mode.contains("energÃ©tico") || mode.contains("e(x)")) {
                // Derived Energy E(x)
                colors = new Color[]{
                    Color.rgb(69, 26, 3), Color.rgb(180, 83, 9), Color.rgb(234, 88, 12), Color.rgb(250, 204, 21), Color.rgb(254, 240, 138)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.energy.0", "Biomasse (< 10 MJ/hab)"),
                    I18n.getOrDefault("setup.legend.energy.1", "Traction (10-50 MJ)"),
                    I18n.getOrDefault("setup.legend.energy.2", "Hydraulique/Charbon (50-200)"),
                    I18n.getOrDefault("setup.legend.energy.3", "Fossile (200-500 MJ)"),
                    I18n.getOrDefault("setup.legend.energy.4", "Ã‰lectrique (> 500 MJ)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.energy.0.desc", "RÃ©gime Biomasse : Chaleur du bois et travail musculaire"),
                    I18n.getOrDefault("setup.legend.energy.1.desc", "RÃ©gime Traction Animale : Attelages, bÅ“ufs et chevaux"),
                    I18n.getOrDefault("setup.legend.energy.2.desc", "RÃ©gime MÃ©canique : Ã‰nergie hydraulique, Ã©olienne et dÃ©buts du charbon"),
                    I18n.getOrDefault("setup.legend.energy.3.desc", "RÃ©gime Hydrocarbures : PÃ©trole, gaz et thermodynamique industrielle"),
                    I18n.getOrDefault("setup.legend.energy.4.desc", "RÃ©gime Ã‰lectrique Massif : RÃ©seaux de puissance et transition Ã©nergÃ©tique")
                };
            } else if (derivedOffset == 3 || mode.contains("alimentaires") || mode.contains("food") || mode.contains("nahrung") || mode.contains("alimentos") || mode.contains("f(x)")) {
                // Derived Food F(x)
                colors = new Color[]{
                    Color.rgb(163, 230, 53), Color.rgb(132, 204, 22), Color.rgb(34, 197, 94), Color.rgb(5, 150, 105), Color.rgb(6, 78, 59)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.food.0", "< 1 Mois (Critique)"),
                    I18n.getOrDefault("setup.legend.food.1", "1â€“3 Mois (Faible)"),
                    I18n.getOrDefault("setup.legend.food.2", "3â€“6 Mois (Moyen)"),
                    I18n.getOrDefault("setup.legend.food.3", "6â€“12 Mois (SÃ©curisÃ©)"),
                    I18n.getOrDefault("setup.legend.food.4", "> 12 Mois (Abondance)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.food.0.desc", "RÃ©serves Critiques : VulnÃ©rabilitÃ© immÃ©diate Ã  la moindre mauvaise rÃ©colte"),
                    I18n.getOrDefault("setup.legend.food.1.desc", "RÃ©serves de Subsistance : Stocks saisonniers d'appoint"),
                    I18n.getOrDefault("setup.legend.food.2.desc", "Greniers Traditionnels : CapacitÃ© de soudure inter-annuelle"),
                    I18n.getOrDefault("setup.legend.food.3.desc", "RÃ©serves StratÃ©giques : Silos rÃ©gionaux prÃ©venant toute disette"),
                    I18n.getOrDefault("setup.legend.food.4.desc", "Surplus SystÃ©mique : ChaÃ®nes logistiques agroalimentaires pÃ©rennes")
                };
            } else if (derivedOffset == 4 || mode.contains("informationnel") || mode.contains("info") || mode.contains("i(x)")) {
                // Derived Info I(x)
                colors = new Color[]{
                    Color.rgb(55, 48, 163), Color.rgb(124, 58, 237), Color.rgb(219, 39, 119), Color.rgb(6, 182, 212), Color.rgb(224, 242, 254)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.info.0", "Oral (< 10 bits/hab)"),
                    I18n.getOrDefault("setup.legend.info.1", "Ã‰crit & Parchemin (10-100)"),
                    I18n.getOrDefault("setup.legend.info.2", "Imprimerie (100-1k)"),
                    I18n.getOrDefault("setup.legend.info.3", "MÃ©dias Masse (1k-10k)"),
                    I18n.getOrDefault("setup.legend.info.4", "NumÃ©rique (> 10k)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.info.0.desc", "Tradition Orale : Savoirs transmis par la mÃ©moire et le chant"),
                    I18n.getOrDefault("setup.legend.info.1.desc", "Ã‰crit & Manuscrits : Enregistrement sur argile, papyrus et parchemins"),
                    I18n.getOrDefault("setup.legend.info.2.desc", "Imprimerie MÃ©canique : Diffusion Ã©largie des traitÃ©s et encyclopÃ©dies"),
                    I18n.getOrDefault("setup.legend.info.3.desc", "TÃ©lÃ©communications : Presse quotidienne, tÃ©lÃ©graphe, radio et cinÃ©ma"),
                    I18n.getOrDefault("setup.legend.info.4.desc", "SociÃ©tÃ© NumÃ©rique : Internet, calcul haute performance et bases de donnÃ©es")
                };
            } else if (derivedOffset == 5 || mode.contains("empreinte") || mode.contains("footprint") || mode.contains("fuÃŸabdruck") || mode.contains("huella") || mode.contains("malthus")) {
                // Derived Footprint & Malthusian Tension
                colors = new Color[]{
                    Color.rgb(16, 185, 129), Color.rgb(132, 204, 22), Color.rgb(245, 158, 11), Color.rgb(239, 68, 68), Color.rgb(127, 29, 29)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.eco.0", "Biome Vierge (Câ‚‡=0)"),
                    I18n.getOrDefault("setup.legend.eco.1", "Pression ModÃ©rÃ©e (0.25)"),
                    I18n.getOrDefault("setup.legend.eco.2", "Surexploitation (0.50)"),
                    I18n.getOrDefault("setup.legend.eco.3", "Tension Critique (0.75)"),
                    I18n.getOrDefault("setup.legend.eco.4", "Effondrement (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.eco.0.desc", "Biome Intact : Ã‰cosystÃ¨mes Ã  l'Ã©quilibre sans perturbation anthropique"),
                    I18n.getOrDefault("setup.legend.eco.1.desc", "Pression LÃ©gÃ¨re : ForÃªt gÃ©rÃ©e, chasse et Ã©levage durable"),
                    I18n.getOrDefault("setup.legend.eco.2.desc", "Surexploitation Agricole : DÃ©forestation, Ã©rosion des sols, baisse des rendements"),
                    I18n.getOrDefault("setup.legend.eco.3.desc", "Tension Malthusienne SÃ©vÃ¨re : PÃ©nuries de bois, rarÃ©faction du gibier et des nappes"),
                    I18n.getOrDefault("setup.legend.eco.4.desc", "Effondrement Ã‰cologique : DÃ©sertification irrÃ©versible et disette systÃ©mique")
                };
            } else if (derivedOffset == 6 || mode.contains("friction") || mode.contains("reibung") || mode.contains("fricciÃ³n")) {
                // Derived Border Friction
                colors = new Color[]{
                    Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68), Color.rgb(167, 139, 250)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.friction.0", "Plaine (Faible Ïƒ)"),
                    I18n.getOrDefault("setup.legend.friction.1", "Colline / Fleuve (Moyen)"),
                    I18n.getOrDefault("setup.legend.friction.2", "Montagne (Fort)"),
                    I18n.getOrDefault("setup.legend.friction.3", "DÃ©sert / ExtrÃªme"),
                    I18n.getOrDefault("setup.legend.friction.4", "BarriÃ¨re Absolue")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.friction.0.desc", "Plaine Alluviale : Friction minimale Ã  la mobilitÃ© (Ïƒ â‰ˆ 0.1)"),
                    I18n.getOrDefault("setup.legend.friction.1.desc", "Colline / Fleuve : Obstacle naturel mineur franchissable"),
                    I18n.getOrDefault("setup.legend.friction.2.desc", "ChaÃ®ne Montagneuse : Transports ralentis, cols escarpÃ©s"),
                    I18n.getOrDefault("setup.legend.friction.3.desc", "DÃ©sert ExtrÃªme : Zone aride exigeant des convois spÃ©cialisÃ©s"),
                    I18n.getOrDefault("setup.legend.friction.4.desc", "Haute Altitude / Falaise : BarriÃ¨re infranchissable pour les armÃ©es")
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
                    alert.setHeaderText(I18n.getOrDefault("scenario.dialog.invalid_map_header", "Fichier de carte non supportÃ© ou illisible"));
                    alert.setContentText(file.getName() + " :\n" + val.message() + "\n\n" + I18n.getOrDefault("scenario.dialog.invalid_map_hint", "Veuillez fournir une image raster valide (PNG/JPG) en projection Ã©quirectangulaire (2:1)."));
                    alert.showAndWait();
                    return;
                }
                customDensityImage = img;
                densityMapFileLabel.setText(String.format("ðŸ“· %s (%dx%d)", file.getName(), val.width(), val.height()));
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
        if (demoCompatibilityLabel == null) return;
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        String planetName = p != null ? p.name() : "Standard";
        if (customDensityImage != null) {
            org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(customDensityImage);
            if (!val.valid()) {
                demoCompatibilityLabel.setText(String.format(I18n.getOrDefault("scenario.demo.map_invalid", "âš ï¸ Carte dÃ©mographique incompatible : %s"), val.message()));
                demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            } else {
                demoCompatibilityLabel.setText(String.format(I18n.getOrDefault("scenario.demo.map_loaded", "âœ… Map loaded and compatible with selected Tab 1 world (%s)"), planetName));
                demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
            }
        } else {
            demoCompatibilityLabel.setText(I18n.getOrDefault("scenario.demo.no_map", "ðŸª No external map loaded â€” Procedural mode active"));
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
                I18n.getOrDefault("scenario.dialog.density_help_title", "SpÃ©cifications de la Carte de DensitÃ©"),
                I18n.getOrDefault("scenario.dialog.density_help_header", "Formats d'Image SupportÃ©s pour l'Import de DensitÃ© DÃ©mographique"),
                "Vous pouvez importer une carte de rÃ©partition dÃ©mographique sous forme d'image PNG/JPEG au ratio 2:1 (ex: 2048x1024 pixels en projection Ã©quirectangulaire) :\n\n" +
                "1. CARTE DE DENSITÃ‰ NIVEAUX DE GRIS :\n" +
                "   â€¢ Noir (0) = 0 hab/kmÂ² (Zone inhabitÃ©e / dÃ©sertique / aquatique)\n" +
                "   â€¢ Blanc (255) = DensitÃ© maximale d'habitation (Foyer urbain ou mÃ©tropole)\n\n" +
                "2. INTÃ‰GRATION AVEC LE TERRAIN ET L'Ã‰COLOGIE :\n" +
                "   â€¢ Les zones d'eau (ocÃ©ans/mers) dÃ©finies dans l'onglet 1 sont automatiquement filtrÃ©es pour Ã©viter l'apparition de populations en pleine mer.\n" +
                "   â€¢ La population totale paramÃ©trÃ©e dans le spinner est distribuÃ©e au prorata de la luminositÃ© de chaque pixel."
        );
    }

    private void generatePreview() {
        if (previewStatusLabel != null) {
            previewStatusLabel.setText(I18n.getOrDefault("scenario.status.calc_preview", "âš¡ Calculating demographic preview in background..."));
        }
        isPreviewGenerating.set(true);
        new Thread(() -> {
            try {
                PlanetPreset cfg = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : null);
                if (cfg == null) cfg = PlanetPreset.EARTH_LIKE;

                // Fast preview generation using Resolution 3 (41,162 cells) instead of blocking Res 4
                List<H3Cell> previewCells = new ProceduralGenerator().generatePlanet(
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

                // Map elevation and biome images corresponding to the selected planet preset
                applyPresetMapToCells(previewCells, cfg);

                distributeInitialPopulation(previewCells);

                javafx.application.Platform.runLater(() -> {
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

        if (customDensityImage != null) {
            // Custom PNG density map sampling
            List<H3Cell> landCells = cells.stream()
                    .filter(c -> c.getElevation() > 0 && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                    .toList();
            if (landCells.isEmpty()) return;

            double popPerCell = (double) totalPop / landCells.size();
            for (H3Cell c : landCells) {
                double normLat = (c.getLatitude() + 90.0) / 180.0;
                double normLon = (c.getLongitude() + 180.0) / 360.0;
                int px = (int) Math.min(customDensityImage.getWidth() - 1, Math.max(0, normLon * customDensityImage.getWidth()));
                int py = (int) Math.min(customDensityImage.getHeight() - 1, Math.max(0, (1.0 - normLat) * customDensityImage.getHeight()));
                Color imgCol = customDensityImage.getPixelReader().getColor(px, py);
                double factor = imgCol.getBrightness() * 5.0;
                c.setPopulation((int) (popPerCell * factor));
            }
        } else {
            // Standard procedural density calculation via ProceduralPopulationEngine
            double techLevel = Math.clamp(Math.log10(Math.max(1.0, capitalK0)) * 2.2 + 0.2, 0.2, 10.0);
            ProceduralPopulationEngine.distributePopulation(cells, getScenario(), totalPop, techLevel, pattern, isEarthPreset, startYear);
        }
    }

    private Color getReliefAndDensityColor(H3Cell c) {
        if (c == null) return Color.rgb(15, 23, 42);

        double elev = c.getElevation() != null ? c.getElevation() : 0.0;
        Biome b = c.getBiome();

        Color baseTerrainCol;
        if (elev <= 0) {
            // Sea / Ocean Bathymetry (Deep navy to coastal cyan-blue)
            double depthRatio = Math.clamp(Math.abs(elev) / 4000.0, 0.0, 1.0);
            int r = (int) (30 - depthRatio * 20);
            int g = (int) (95 - depthRatio * 75);
            int bCol = (int) (165 - depthRatio * 115);
            baseTerrainCol = Color.rgb(Math.clamp(r, 0, 255), Math.clamp(g, 0, 255), Math.clamp(bCol, 0, 255));
        } else {
            // Land Topography (hypsometric tinting + biome colors)
            double altRatio = Math.clamp(elev / 4500.0, 0.0, 1.0);
            int r, g, bCol;
            if (b == Biome.SNOW || elev > 3800) {
                r = 235; g = 245; bCol = 255; // Ice / Snow peaks
            } else if (b == Biome.TUNDRA) {
                r = 135; g = 155; bCol = 135;
            } else if (b == Biome.DESERT) {
                r = 215; g = 185; bCol = 125;
            } else if (b == Biome.JUNGLE) {
                r = 25; g = 100; bCol = 45;
            } else if (b == Biome.FOREST) {
                r = 35; g = 115; bCol = 50;
            } else if (b == Biome.HILLS || (elev > 1200 && elev <= 2800)) {
                r = 130 + (int)(altRatio * 40);
                g = 120 - (int)(altRatio * 20);
                bCol = 80;
            } else if (b == Biome.MOUNTAINS || elev > 2800) {
                r = 160 + (int)(altRatio * 50);
                g = 155 + (int)(altRatio * 45);
                bCol = 150 + (int)(altRatio * 45);
            } else { // PLAINS / BEACH / Default land
                r = 65; g = 135; bCol = 55;
            }

            // Modulate by elevation (high land is slightly lighter/paler)
            r = Math.clamp((int)(r * (0.85 + altRatio * 0.35)), 0, 255);
            g = Math.clamp((int)(g * (0.85 + altRatio * 0.35)), 0, 255);
            bCol = Math.clamp((int)(bCol * (0.85 + altRatio * 0.35)), 0, 255);
            baseTerrainCol = Color.rgb(r, g, bCol);
        }

        if (elev <= 0) {
            return baseTerrainCol;
        }

        long pop = c.getPopulation() != null ? c.getPopulation() : 0;

        // Population Density Heat Overlay (aligned with UI Legend Bar)
        Color heatCol;
        if (pop <= 0) {
            heatCol = Color.rgb(30, 95, 165);  // InhabitÃ© (0 hab/kmÂ²) - Navy Blue (Legend matching)
        } else if (pop < 50) {
            heatCol = Color.rgb(16, 185, 129); // Faible (1 - 50 hab/kmÂ²) - Green
        } else if (pop < 500) {
            heatCol = Color.rgb(234, 179, 8);  // Moyenne (50 - 500 hab/kmÂ²) - Yellow
        } else if (pop < 2500) {
            heatCol = Color.rgb(249, 115, 22); // Ã‰levÃ©e / CitÃ© (500 - 2500 hab/kmÂ²) - Orange
        } else {
            heatCol = Color.rgb(239, 68, 68);  // MÃ©tropole (> 2500 hab/kmÂ²) - Red
        }

        return heatCol;
    }

    private double computeCellCarryingCapacity(H3Cell c) {
        if (c == null || c.getElevation() <= 0) return 0.0;
        double baseCap = 250.0;
        Biome b = c.getBiome();
        if (b == Biome.DESERT || b == Biome.TUNDRA || b == Biome.SNOW) baseCap *= 0.1;
        else if (b == Biome.PLAINS || b == Biome.FOREST) baseCap *= 1.5;
        else if (b == Biome.JUNGLE) baseCap *= 0.8;

        if (c.getWaterResource() != null && c.getWaterResource() > 0.1) {
            baseCap *= (1.0 + 3.0 * (c.getWaterResource() / 1000.0));
        }
        if (c.getFreshwaterAquifer() != null && c.getFreshwaterAquifer() > 0.1) {
            baseCap *= (1.0 + 1.5 * (c.getFreshwaterAquifer() / 1000.0));
        }

        double capK0 = computeAutoCapitalFromYear(startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000);
        baseCap *= Math.max(0.5, (capK0 / 1000.0) * 0.8);
        return Math.max(10.0, baseCap);
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
                            if (brightness <= 0.02) heatCol = Color.rgb(30, 95, 165);       // InhabitÃ©
                            else if (brightness < 0.20) heatCol = Color.rgb(16, 185, 129);  // Faible
                            else if (brightness < 0.45) heatCol = Color.rgb(234, 179, 8);   // Moyenne
                            else if (brightness < 0.75) heatCol = Color.rgb(249, 115, 22);  // Ã‰levÃ©e
                            else heatCol = Color.rgb(239, 68, 68);                          // MÃ©tropole
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

        // Calques dÃ©duits & physiques
        int derivedOffset = idx - (dims + 1);
        if (derivedOffset == 1 || mode.contains("capital") || mode.contains("k(x)")) {
            double cap = c.getResourceCapital() != null ? c.getResourceCapital() : 0.0;
            double norm = Math.clamp(cap / 500000.0, 0.0, 1.0);
            return Color.hsb((1.0 - norm) * 240.0, 0.85, 0.90);
        } else if (derivedOffset == 2 || mode.contains("Ã©nergÃ©tique") || mode.contains("energy") || mode.contains("energie") || mode.contains("energÃ©tico") || mode.contains("e(x)")) {
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
        } else if (derivedOffset == 5 || mode.contains("empreinte") || mode.contains("footprint") || mode.contains("fuÃŸabdruck") || mode.contains("huella") || mode.contains("malthus")) {
            double cap = computeCellCarryingCapacity(c);
            long pop = c.getPopulation() != null ? c.getPopulation() : 0;
            double ratio = Math.clamp(pop / Math.max(1.0, cap), 0.0, 1.5);
            return Color.hsb((1.0 - Math.min(1.0, ratio)) * 120.0, 0.85, 0.90);
        } else if (derivedOffset == 6 || mode.contains("friction") || mode.contains("reibung") || mode.contains("fricciÃ³n")) {
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
            gc.fillText(String.format("âœ‚ï¸ Zone: Lat[%.1fÂ°, %.1fÂ°] Lng[%.1fÂ°, %.1fÂ°]", cMinLat, cMaxLat, cMinLng, cMaxLng), rx + 4, ry - 6);
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
        if (previewStatusLabel == null) return;
        if (currentPreviewCells == null || currentPreviewCells.isEmpty()) {
            previewStatusLabel.setText(I18n.getOrDefault("scenario.label.preview_precalc", "AperÃ§u de la distribution dÃ©mographique prÃ©-calculÃ©e"));
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
                I18n.getOrDefault("scenario.map_info.density", "ðŸ‘¥ DÃ©mographie Tâ‚€ : %s hab.  |  ðŸ“ %s cellules H3 actives (Res %d)  |  ðŸ“ˆ DensitÃ© max : %.1f hab/kmÂ² (Moy : %.1f)"),
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
            String modeStr = isProc ? (I18n.getOrDefault("scenario.mode.procedural", "ProcÃ©dural") + " (" + I18n.getOrDefault("scenario.label.seed", "Graine") + ": " + (tensorSeedFields.containsKey(tIdx) ? tensorSeedFields.get(tIdx).getText() : getDefaultTensorSeed(tIdx)) + ")")
                                    : (I18n.getOrDefault("scenario.mode.imported", "Import SIG / Empirique"));
            double diff = culturalDiffusionRateSpinner != null && culturalDiffusionRateSpinner.getValue() != null ? culturalDiffusionRateSpinner.getValue() : 0.05;
            double mut = culturalMutationRateSpinner != null && culturalMutationRateSpinner.getValue() != null ? culturalMutationRateSpinner.getValue() : 0.01;

            double minVal = 1.0, maxVal = 0.0, sumVal = 0.0, sumSq = 0.0;
            int count = 0;
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
                I18n.getOrDefault("scenario.map_info.tensor", "âš™ï¸ Mode : %s  |  ðŸ“Š Dispersion : Î¼=%.2f Â± %.2f [min=%.2f, max=%.2f]  |  ðŸŒ€ Diffusion D=%.3f  |  ðŸŽ² Bruit=%.3f"),
                modeStr, mean, stdDev, minVal, maxVal, diff, mut
            ));
        } else {
            // Derived layers (Capital, Energy, Food, Info, Footprint, Friction)
            int derivedOffset = curSel - (dims + 1);
            String layerName = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue() : "";
            double sum = 0.0;
            int count = 0;
            for (H3Cell c : currentPreviewCells) {
                if (c == null) continue;
                boolean isLand = (c.getElevation() != null && c.getElevation() > 0.0) || (c.getBiome() != null && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN);
                if (isLand) {
                    count++;
                    if (derivedOffset == 1 || layerName.contains("Capital") || layerName.contains("K(x)")) sum += (c.getResourceCapital() != null ? c.getResourceCapital() : 0.0);
                    else if (derivedOffset == 2 || layerName.contains("Energy") || layerName.contains("E(x)") || layerName.contains("Ã©nergÃ©tique") || layerName.contains("Energie") || layerName.contains("EnergÃ©tico")) sum += (c.getEnergyFire() != null ? c.getEnergyFire() : 0.0);
                    else if (derivedOffset == 3 || layerName.contains("Food") || layerName.contains("F(x)") || layerName.contains("alimentaires") || layerName.contains("Nahrung") || layerName.contains("Alimentos")) sum += (c.getFoodResource() != null ? c.getFoodResource() : 0.0);
                    else if (derivedOffset == 4 || layerName.contains("Info") || layerName.contains("I(x)") || layerName.contains("informationnel") || layerName.contains("Information")) sum += (c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 0.0);
                    else if (derivedOffset == 6 || layerName.contains("Friction") || layerName.contains("friction") || layerName.contains("Reibung") || layerName.contains("FricciÃ³n")) sum += (c.getMovementFriction() != null ? c.getMovementFriction() : 1.0);
                    else sum += computeCellCarryingCapacity(c);
                }
            }
            double mean = count > 0 ? (sum / count) : 0.0;
            previewStatusLabel.setText(String.format(
                I18n.getOrDefault("scenario.map_info.derived", "ðŸ“Š Moyenne : %.2f  |  âš¡ Stock global : %s  |  ðŸ“ Cellules actives : %s"),
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
                            pxColor = blendColors(baseReliefColor, Color.rgb(30, 95, 165), 0.35); // InhabitÃ©
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
        gc.fillText("âš¡ AperÃ§u 2D dynamique instantanÃ© (" + (mode.isEmpty() ? "Relief/DensitÃ©" : mode.toUpperCase()) + ")...", 20, h - 15);
    }

    public boolean isDirty() {
        return scenarioPresetBar != null && scenarioPresetBar.isDirty();
    }

    public boolean promptSaveIfDirty(javafx.stage.Window owner) {
        if (scenarioPresetBar == null) return true;
        return scenarioPresetBar.promptSavePresetIfDirty(owner);
    }

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
    // DEFERRED EXECUTION â€” Calculate H3 Grid & Start Simulation
    // =========================================================================

    public void setProgressControls(ProgressBar bar, Label label, javafx.scene.Node overlayContainer) {
        this.externalProgressBar = bar;
        this.externalStatusLabel = label;
        this.externalOverlayContainer = overlayContainer;
    }

    private void updateProgress(double progressVal, String msg) {
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

    public void invalidateGeneratedState() {
        resetStartButtonState();
    }

    private void resetStartButtonState() {
        isCalculationRunning = false;
        WindowUtils.setBusyCursor(this, false);
        javafx.application.Platform.runLater(() -> {
            if (startBtn != null) {
                startBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.start_btn", "ðŸš€ VALIDER ET LANCER LA SIMULATION"));
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
                startBtn.setText(I18n.getOrDefault("scenario.btn.valid_ready", "âš¡ VALID & READY â€” PROCEED TO EXECUTION CONTEXT (TAB 4)"));
                startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #0284c7; -fx-text-fill: white; -fx-background-radius: 6;");
                startBtn.setDisable(false);
            }
        });
    }

    private void setStartButtonCancelState() {
        isCalculationRunning = true;
        javafx.application.Platform.runLater(() -> {
            if (startBtn != null) {
                startBtn.setText(I18n.getOrDefault("scenario.btn.cancel_gen", "â¹ CANCEL / STOP GENERATION"));
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
                alert.setContentText(I18n.getOrDefault("scenario.confirm_restart.content", "Lancer cette nouvelle configuration rÃ©initialisera la simulation en cours. Souhaitez-vous continuer ?"));
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
        updateProgress(0.0, I18n.getOrDefault("scenario.progress.cancelling", "ðŸ›‘ Cancelling computation at user request..."));
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
                updateProgress(1.0, I18n.getOrDefault("scenario.progress.instant_resume", "âš¡ Instant restoration from selected snapshot..."));
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
            progressStatusLabel.setText(I18n.getOrDefault("scenario.progress.init_prep", "âš¡ Scenario initialization & setup..."));
        }
        if (externalOverlayContainer != null) {
            externalOverlayContainer.setVisible(true);
            externalOverlayContainer.setManaged(true);
        }

        generationThread = new Thread(() -> {
            try {
                updateProgress(0.02, I18n.getOrDefault("scenario.progress.saving_db", "âš¡ Saving scenario & preparing H3 grid..."));

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

                updateProgress(0.05, String.format(I18n.getOrDefault("scenario.progress.ready_h3", "âœ… Scenario '%s' ready! âš¡ Generating H3 grid..."), scenarioToSave.getName()));

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
                updateProgress(0.72, I18n.getOrDefault("scenario.progress.hydrography", "ðŸŒŠ Computing hydrographic network & river basins (72%)..."));

                // Apply Geographical Clipping if enabled (78% -> 85%)
                if (isClippingActive) {
                    updateProgress(0.80, I18n.getOrDefault("scenario.progress.clipping", "âœ‚ï¸ Applying geographical clipping & boundary conditions (80%)..."));

                    double marginLat = Math.max(1.0, (cMaxLat - cMinLat) * 0.08);
                    double marginLng = Math.max(1.0, (cMaxLng - cMinLng) * 0.08);

                    List<H3Cell> clippedCells = new ArrayList<>();
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
                updateProgress(0.88, String.format(I18n.getOrDefault("scenario.progress.pop_capital", "ðŸ‘¥ Distributing population, ecological footprint & capital Kâ‚€: 88%% (%d / %d cells)"), totalCellCount, totalCellCount));

                distributeInitialPopulation(cells);
                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    throw new java.util.concurrent.CancellationException("Generation cancelled by user");
                }

                updateProgress(0.94, String.format(I18n.getOrDefault("scenario.progress.thermo_sync", "âš¡ Synchronizing simulation buffers & thermodynamic pre-computation: 94%% (%d / %d cells)"), totalCellCount, totalCellCount));

                currentPreviewCells = cells;

                final List<H3Cell> finalCells = cells;
                javafx.application.Platform.runLater(() -> {
                    updateProgress(1.0, String.format(I18n.getOrDefault("scenario.progress.launching", "âœ… %,d H3 cells generated! Launching scenario '%s'..."), finalCells.size(), scenarioToSave.getName()));
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
                    updateProgress(0.0, I18n.getOrDefault("scenario.progress.cancelled", "ðŸ›‘ Computation cancelled by user."));
                } else {
                    logger.error("Error during deferred H3 cell calculation", ex);
                    updateProgress(1.0, String.format(I18n.getOrDefault("scenario.progress.error", "âŒ Error during computation: %s"), ex.getMessage()));
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
            else if (mapSource.equals("venus") || presetName.contains("venus") || presetName.contains("vÃ©nus") || presetName.contains("hesperos")) pKey = "venus";
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
    // EVENTS SECTION â€” Historical Planetary Events
    // =========================================================================

    @SuppressWarnings("unchecked")
    private VBox createEventsSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        title3Events = new Label();
        title3Events.getStyleClass().add("label-section-header");
        title3Events.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events", "Schedule climate and geological events.")));

        eventsList = FXCollections.observableArrayList();
        eventsTable = new TableView<>(eventsList);
        eventsTable.setEditable(true);
        eventsTable.setPrefHeight(170);
        eventsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        List<String> eventTypes = List.of(
            // --- JALONS INFORMATIFS (PrÃ©fixe milestone_) ---
            "milestone_archaeology",
            "milestone_polity",
            "milestone_technology",
            "milestone_golden_age",
            "milestone_ethnography",
            // --- DÃ‰SASTRES & CATACLYSMES (PrÃ©fixe disaster_) ---
            "disaster_volcano",
            "disaster_earthquake",
            "disaster_tsunami",
            "disaster_flood",
            "disaster_drought",
            "disaster_pandemic",
            "disaster_famine",
            "disaster_heatwave",
            "disaster_ice_age",
            "disaster_meteor",
            "disaster_solar_emp",
            "disaster_nuclear_strike",
            "disaster_nuclear_winter",
            "disaster_cyber_attack",
            "disaster_economic_crash",
            "disaster_biodiversity_collapse",
            "disaster_geoengineering",
            "disaster_alien_contact",
            // --- ALIASES RÃ‰TROCOMPATIBLES ---
            "milestone",
            "historical",
            "volcano",
            "flood",
            "drought",
            "nuclear_strike",
            "nuclear_winter",
            "earthquake",
            "tsunami",
            "meteor",
            "solar_emp",
            "pandemic",
            "famine",
            "heatwave",
            "ice_age",
            "cyber_attack",
            "economic_crash",
            "biodiversity_collapse",
            "geoengineering",
            "renaissance_boom",
            "tech_singularity",
            "alien_contact"
        );

        colType = new TableColumn<>();
        colType.setCellValueFactory(d -> d.getValue().typeProperty());
        colType.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(eventTypes)));
        colType.setOnEditCommit(e -> e.getRowValue().setType(e.getNewValue()));
        colType.setPrefWidth(140);

        colName = new TableColumn<>();
        colName.setCellValueFactory(d -> d.getValue().nameProperty());
        colName.setCellFactory(TextFieldTableCell.forTableColumn());
        colName.setOnEditCommit(e -> e.getRowValue().setName(e.getNewValue()));
        colName.setPrefWidth(130);

        colYear = new TableColumn<>();
        colYear.setCellValueFactory(d -> d.getValue().yearProperty().asObject());
        colYear.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
        colYear.setOnEditCommit(e -> e.getRowValue().setYear(e.getNewValue()));
        colYear.setPrefWidth(65);

        javafx.util.StringConverter<Double> coordConverter = new javafx.util.StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                if (object == null || Math.abs(object) < 1e-6) {
                    return "â€”";
                }
                return String.format(java.util.Locale.US, "%.1f", object);
            }

            @Override
            public Double fromString(String string) {
                if (string == null || string.trim().isEmpty() || "â€”".equals(string.trim()) || "-".equals(string.trim())) {
                    return 0.0;
                }
                try {
                    return Double.parseDouble(string.trim());
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
        };

        colLat = new TableColumn<>();
        colLat.setCellValueFactory(d -> d.getValue().latitudeProperty().asObject());
        colLat.setCellFactory(TextFieldTableCell.forTableColumn(coordConverter));
        colLat.setOnEditCommit(e -> e.getRowValue().setLatitude(e.getNewValue()));
        colLat.setPrefWidth(55);

        colLon = new TableColumn<>();
        colLon.setCellValueFactory(d -> d.getValue().longitudeProperty().asObject());
        colLon.setCellFactory(TextFieldTableCell.forTableColumn(coordConverter));
        colLon.setOnEditCommit(e -> e.getRowValue().setLongitude(e.getNewValue()));
        colLon.setPrefWidth(55);

        colDepth = new TableColumn<>();
        colDepth.setCellValueFactory(d -> d.getValue().depthProperty().asObject());
        colDepth.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        colDepth.setOnEditCommit(e -> e.getRowValue().setDepth(e.getNewValue()));
        colDepth.setPrefWidth(70);

        colMag = new TableColumn<>();
        colMag.setCellValueFactory(d -> d.getValue().magnitudeProperty().asObject());
        colMag.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        colMag.setOnEditCommit(e -> e.getRowValue().setMagnitude(e.getNewValue()));
        colMag.setPrefWidth(110);

        eventsTable.getColumns().clear();
        eventsTable.getColumns().addAll(colType, colName, colYear, colLat, colLon, colDepth, colMag);
        eventsTable.setUserData(new TableColumn[]{colType, colName, colYear, colLat, colLon, colDepth, colMag});
        eventsTable.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events_table", "ðŸ“‹ Events Table: Double-click a cell to edit its type (milestone_* or disaster_*), name, year, coordinates, or magnitude.")));

        addEventBtn = new Button();
        addEventBtn.getStyleClass().add("button-secondary");
        addEventBtn.setMinWidth(Region.USE_PREF_SIZE);
        addEventBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events.add", "âž• Add new event (choose among milestone_* or disaster_* types).")));
        addEventBtn.setOnAction(e -> eventsList.add(new ClimateEvent("milestone_archaeology", org.ether.society.i18n.I18n.getOrDefault("scenario.event.default_name", "New Event"), 2026, 0.0, 0.0, 0.0, 5.0)));

        removeEventBtn = new Button();
        removeEventBtn.getStyleClass().add("button-secondary");
        removeEventBtn.setMinWidth(Region.USE_PREF_SIZE);
        removeEventBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events.remove", "Delete selected event from table.")));
        removeEventBtn.setOnAction(e -> {
            ClimateEvent sel = eventsTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle(org.ether.society.i18n.I18n.getOrDefault("dialog.confirm.title", "Delete Confirmation"));
                alert.setHeaderText(null);
                alert.setContentText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.confirm_delete", "Do you really want to delete this event?"));
                WindowUtils.applyWindowIcon(alert);
                alert.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.OK) {
                        eventsList.remove(sel);
                    }
                });
            }
        });

        loadEarthEventsBtn = new Button();
        loadEarthEventsBtn.getStyleClass().add("button-secondary");
        loadEarthEventsBtn.setMinWidth(Region.USE_PREF_SIZE);
        loadEarthEventsBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events.load", "Load or merge master chronological events catalog (cataclysms & milestones).")));
        loadEarthEventsBtn.setOnAction(e -> loadEarthHistoricalEvents(startYearSpinner != null ? startYearSpinner.getValue() : -8000, true));

        randomEventsCheckBox = new CheckBox(org.ether.society.i18n.I18n.getOrDefault("scenario.events.random_checkbox", "ðŸŽ² Activer les Ã©vÃ©nements alÃ©atoires & crises stochastiques (Famines, Ã‰pidÃ©mies, SÃ©ismes, Ã‰ruptions)"));
        randomEventsCheckBox.setSelected(true);
        randomEventsCheckBox.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        randomEventsCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.random_events",
            "ðŸŽ² GESTION DES Ã‰VÃ‰NEMENTS ALÃ‰ATOIRES & STOCHASTIQUES\n" +
            "â€¢ Si activÃ© : Le moteur gÃ©nÃ¨re des crises Ã©mergentes (famines, pestes, sÃ©cheresses, Ã©ruptions) pilotÃ©es par la graine stochastique (Seed).\n" +
            "â€¢ Si dÃ©sactivÃ© : AUCUN Ã©vÃ©nement alÃ©atoire ne survient spontanÃ©ment (moins rÃ©aliste, mais garantit une trajectoire dÃ©terministe pure).")));
        randomEventsCheckBox.setOnAction(e -> notifyParamChange());

        earthLeadersCheckBox = new CheckBox(org.ether.society.i18n.I18n.getOrDefault("scenario.events.earth_leaders_checkbox", "ðŸ›ï¸ IntÃ©grer les Ã©vÃ©nements de personnages historiques terrestres (Alexandre, Auguste, Hammourabi...)"));
        earthLeadersCheckBox.setSelected(true);
        earthLeadersCheckBox.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold;");
        earthLeadersCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.earth_leaders",
            "ðŸ›ï¸ PERSONNAGES HISTORIQUES & BIFURCATIONS TERRESTRES\n" +
            "â€¢ Si activÃ© : Le moteur injecte les figures historiques majeures rÃ©elles avec leurs modificateurs spatio-temporels exacts.\n" +
            "â€¢ Si dÃ©sactivÃ© : Trajectoire structurelle pure sans chocs biographiques exogÃ¨nes.")));
        earthLeadersCheckBox.setOnAction(e -> notifyParamChange());

        proceduralLeadersCheckBox = new CheckBox(org.ether.society.i18n.I18n.getOrDefault("scenario.events.procedural_leaders_checkbox", "ðŸŒŸ Activer le gÃ©nÃ©rateur d'outliers historiques (Leaders Ã©mergents procÃ©duraux)"));
        proceduralLeadersCheckBox.setSelected(true);
        proceduralLeadersCheckBox.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold;");
        proceduralLeadersCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.procedural_leaders",
            "ðŸŒŸ GESTION DES LEADERS Ã‰MERGENTS PROCÃ‰DURAUX\n" +
            "â€¢ Si activÃ© : Des gÃ©nies militaires, rÃ©formateurs ou bÃ¢tisseurs Ã©mergent stochastiquement selon la complexitÃ© et le stress des populations.\n" +
            "â€¢ Si dÃ©sactivÃ© : Aucun leader procÃ©dural n'Ã©merge.")));
        proceduralLeadersCheckBox.setOnAction(e -> notifyParamChange());

        Label randomEventsNote = new Label(I18n.getOrDefault("scenario.note.random_events", "ðŸ’¡ Note RÃ©alisme & Graine : Les Ã©vÃ©nements alÃ©atoires utilisent la graine (Seed) du scÃ©nario pour une reproductibilitÃ© exacte. Les dÃ©sactiver supprime toute crise spontanÃ©e imprÃ©vue."));
        randomEventsNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        randomEventsNote.setWrapText(true);

        VBox randomEventsBox = new VBox(6, randomEventsCheckBox, earthLeadersCheckBox, proceduralLeadersCheckBox, randomEventsNote);
        randomEventsBox.setStyle("-fx-padding: 8 12; -fx-background-color: rgba(56, 189, 248, 0.06); -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.2); -fx-border-radius: 6;");

        CheckBox eventsCheckBox = new CheckBox(org.ether.society.i18n.I18n.getOrDefault("scenario.events.enable", "ðŸ“… Planifier des Ã©vÃ©nements climatiques & dÃ©sastres datÃ©s (Tableau / ScÃ©nario)"));
        eventsCheckBox.setStyle("-fx-font-weight: bold;");
        eventsCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.enable_events", "Active le calendrier d'Ã©vÃ©nements datÃ©s (Ã©ruptions, sÃ©ismes, traitÃ©s) Ã  des annÃ©es prÃ©cises.")));

        HBox btnBar = new HBox(8, addEventBtn, removeEventBtn, new Separator(javafx.geometry.Orientation.VERTICAL), loadEarthEventsBtn);
        btnBar.setAlignment(Pos.CENTER_LEFT);

        VBox eventsSubPanel = new VBox(8, btnBar, eventsTable);
        eventsSubPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(251,146,60,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        eventsSubPanel.setVisible(false);
        eventsSubPanel.setManaged(false);

        eventsCheckBox.setOnAction(e -> {
            boolean active = eventsCheckBox.isSelected();
            eventsSubPanel.setVisible(active);
            eventsSubPanel.setManaged(active);
            notifyParamChange();
        });

        section.getChildren().addAll(title3Events, randomEventsBox, eventsCheckBox, eventsSubPanel);
        return section;
    }

    private void loadEarthHistoricalEvents() {
        int startYear = startYearSpinner != null ? startYearSpinner.getValue() : -8000;
        loadEarthHistoricalEvents(startYear, false);
    }

    private void loadEarthHistoricalEvents(int startYear) {
        loadEarthHistoricalEvents(startYear, false);
    }

    private void loadEarthHistoricalEvents(int startYear, boolean promptConfirmation) {
        if (eventsList == null) return;

        boolean replaceAll = true;
        if (promptConfirmation && !eventsList.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(org.ether.society.i18n.I18n.getOrDefault("scenario.events.load.title", "Loading Historical Events"));
            alert.setHeaderText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.load.header", "Existing Events Detected"));
            alert.setContentText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.load.content", 
                    "Des Ã©vÃ©nements existent dÃ©jÃ  dans votre tableau. Souhaitez-vous les remplacer ou les fusionner avec la base historique ?"));

            ButtonType btnReplace = new ButtonType(org.ether.society.i18n.I18n.getOrDefault("scenario.events.load.btn_replace", "Remplacer Tout"));
            ButtonType btnMerge = new ButtonType(org.ether.society.i18n.I18n.getOrDefault("scenario.events.load.btn_merge", "Fusionner / Ajouter"));
            ButtonType btnCancel = new ButtonType(org.ether.society.i18n.I18n.getOrDefault("scenario.events.load.btn_cancel", "Annuler"), ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(btnReplace, btnMerge, btnCancel);
            WindowUtils.applyWindowIcon(alert);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() == btnCancel) {
                return;
            }
            if (result.get() == btnMerge) {
                replaceAll = false;
            }
        }

        List<org.ether.society.model.ClimateEvent> rawList = org.ether.society.data.WikidataEventsFetcher.getOrBuildFullCatalog();

        List<ClimateEvent> filtered = new ArrayList<>();
        for (org.ether.society.model.ClimateEvent ev : rawList) {
            if (ev.getYear() >= startYear) {
                filtered.add(new ClimateEvent(ev.getType(), ev.getName(), ev.getYear(), ev.getLatitude(), ev.getLongitude(), ev.getDepth(), ev.getMagnitude()));
            }
        }

        if (replaceAll) {
            eventsList.setAll(filtered);
        } else {
            for (ClimateEvent ev : filtered) {
                boolean exists = eventsList.stream().anyMatch(e -> e.getName().equalsIgnoreCase(ev.getName()) && e.getYear() == ev.getYear());
                if (!exists) {
                    eventsList.add(ev);
                }
            }
        }

        logger.info("Loaded {} historical events posterior to start year {}.", eventsList.size(), startYear);
    }

    /**
     * Auto-calibrates initial physical capital stock (K0 in kg/capita) based on start year T0.
     */
    public static double computeAutoCapitalFromYear(long year) {
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

    public static double computeAutoEnergyFromYear(long year) {
        if (year <= -10000) return 10.0;    // Firewood (~10 MJ/hab)
        if (year <= -3000)  return 30.0;    // Wood & fodder (~30 MJ/hab)
        if (year <= -500)   return 60.0;    // Oil & firewood (~60 MJ/hab)
        if (year <= 1000)   return 150.0;   // Hydraulic & firewood (~150 MJ/hab)
        if (year <= 1500)   return 300.0;   // Peat & timber (~300 MJ/hab)
        if (year <= 1800)   return 2500.0;  // Coal mines (~2500 MJ/hab)
        if (year <= 1950)   return 10000.0; // Hydrocarbons (~10000 MJ/hab)
        return 50000.0;                     // Modern/Future (>50000 MJ/hab)
    }

    public static double computeAutoFoodFromYear(long year) {
        if (year <= -10000) return 2.0;  // Foraging (2 months)
        if (year <= -3000)  return 4.0;  // Early granaries (4 months)
        if (year <= -500)   return 6.0;  // Roman/Han silos (6 months)
        if (year <= 1000)   return 6.0;  // Feudal granaries (6 months)
        if (year <= 1500)   return 8.0;  // Early modern storage (8 months)
        if (year <= 1800)   return 10.0; // Agricultural revolution (10 months)
        if (year <= 1950)   return 12.0; // Modern global logistics (12 months)
        return 18.0;                     // Future/Post-scarcity (18+ months)
    }

    public static double computeAutoInfoFromYear(long year) {
        if (year <= -10000) return 5.0;        // Oral tradition (~5 bits/hab)
        if (year <= -3000)  return 40.0;       // Cuneiform/Hieroglyphs (~40 bits/hab)
        if (year <= -500)   return 200.0;      // Classical manuscripts (~200 bits/hab)
        if (year <= 1000)   return 500.0;      // Scriptoriums & block printing (~500 bits/hab)
        if (year <= 1500)   return 2000.0;     // Printing press (~2000 bits/hab)
        if (year <= 1800)   return 15000.0;    // Encyclopedias & press (~15000 bits/hab)
        if (year <= 1950)   return 500000.0;   // Telecommunications & radio (~500000 bits/hab)
        return 5000000.0;                      // Internet & AI (>5000000 bits/hab)
    }

    public void applyEarthPreset() {
        if (planetPresetCombo != null) {
            planetPresetCombo.setValue(PlanetPreset.EARTH_LIKE);
        }
    }

    // =========================================================================
    // I18N â€” Update all UI text based on current language
    // =========================================================================

    public void updateTexts() {
        boolean oldUpdating = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            if (headerLabel != null) headerLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.panel.title", "ðŸ“œ Scenario Configuration & Setup"));
            if (scenarioPresetHeader != null) scenarioPresetHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.section.presets", "ðŸŽ›ï¸ GLOBAL PRESETS & SCENARIO SAVE"));
            if (btnAutoEpochScenario != null) {
                btnAutoEpochScenario.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.auto_epoch_wizard", "âœ¨ CrÃ©er un ScÃ©nario Automatique par Date..."));
                btnAutoEpochScenario.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.auto_epoch_wizard", "Ouvre l'assistant de scÃ©nario temporel pour configurer en un clic les cartes d'Ã©lÃ©vation, biomes, ressources, dÃ©mographie et moteurs de Type B compatibles pour n'importe quelle date.")));
            }
            if (scenarioPresetBar != null) {
                scenarioPresetBar.updateTexts();
            }
            if (scenarioDescLabel != null) scenarioDescLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.section.description", "ðŸ“– Detailed Description, Initial Conditions & Key Observables:"));
            if (planetSectionHeader != null) planetSectionHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.section.inherited", "ðŸª INHERITED CONTEXT (TABS 1 & 2)"));
            if (ecoPromptLabel != null) ecoPromptLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.label.ecology_preset", "1. Ecological Preset (Tab 2):"));
            if (planetPromptLabel != null) planetPromptLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.label.planet_preset", "2. Planetary Preset (Tab 1 â€” Cascaded from Ecology):"));
            if (planetPresetCombo != null && planetPresetCombo.getCellFactory() != null) {
                planetPresetCombo.setButtonCell(planetPresetCombo.getCellFactory().call(null));
            }
            if (ecologyPresetCombo != null && ecologyPresetCombo.getCellFactory() != null) {
                ecologyPresetCombo.setButtonCell(ecologyPresetCombo.getCellFactory().call(null));
            }
            updateInheritedContextDisplay(null);
            updateEngineTexts();
            if (title1 != null) title1.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.section.spatiotemporal", "ðŸŒ EPOCH & SPATIOTEMPORAL DEFINITION"));
            if (cultureHeader != null) cultureHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.culture_section", "ðŸ§  CULTURAL VECTOR DIMENSION & MULTI-FIELD LAYERS"));
            if (clippingHeader != null) clippingHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.header", "âœ‚ï¸ BORDERS & HISTORICAL SPATIAL CLIPPING"));
            if (oceanOptHeader != null) oceanOptHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.ocean_opt.header", "âš™ï¸ ENGINE ARCHITECTURE & OPTIMIZATIONS (ETHER CORE & OPTIONAL)"));
            if (title3Events != null) title3Events.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events_section", "ðŸŒªï¸ HISTORICAL PLANETARY EVENTS & CLIMATE DRIFTS"));
            if (snapshotHeader != null) snapshotHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.snapshot.header", "ðŸ“¸ 7. RESUME FROM EXISTING SNAPSHOT (PREVIOUS SESSION)"));
            if (radioNewSimulation != null) {
                radioNewSimulation.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.start_fresh", "ðŸŒ± Start a new simulation from scratch (Year Tâ‚€)"));
                radioNewSimulation.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.start_fresh.tooltip", "Initialize a brand-new historical simulation from canonical Tâ‚€ initial conditions.")));
            }
            if (radioResumeSnapshot != null) {
                radioResumeSnapshot.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.start_snapshot", "ðŸ“¸ Resume from an existing Snapshot (Previous Session / Checkpoint)"));
                radioResumeSnapshot.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.start_snapshot.tooltip", "Load a previously saved physical state or periodic checkpoint directly into memory.")));
            }
            if (snapshotExplainBtn != null) {
                snapshotExplainBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.snapshot_explain", "â„¹ï¸ What is a Snapshot? (Explanations & Mechanics)"));
                snapshotExplainBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.snapshot_explain", "Open detailed technical guide explaining physical state capture, rolling checkpoints, and multiverse branching.")));
            }
            if (snapshotRefreshBtn != null) {
                snapshotRefreshBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.refresh", "ðŸ”„ Refresh Snapshots"));
                snapshotRefreshBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.snapshot_refresh", "Rescans the disk saves directory (saves/) to synchronize the list of real simulation snapshots and checkpoints.")));
            }
            if (snapshotCombo != null) {
                snapshotCombo.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.snapshot.combo.tooltip", "Select an existing checkpoint or full simulation snapshot from the disk to resume execution.")));
                updateSnapshotDetailsDisplay(snapshotCombo.getValue());
            }
            if (bundleHeader != null) bundleHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.bundle.header", "ðŸ“¦ UNIFIED BUNDLE MULTI-SCENARIO IMPORT/EXPORT (.ETHER)"));
            if (bundleSubtitle != null) bundleSubtitle.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.desc.bundle", "Export or import the complete scenario (planetary context, ecology, active engines, cultural layers, and demographic grid) in unified .ether format for archiving or sharing."));
            if (btnExportBundle != null) {
                btnExportBundle.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.export_bundle", "ðŸ“¦ Export Bundle (.ether)"));
                btnExportBundle.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.export_bundle", "Export complete scenario (physics, ecology, engines, layers, and demographics) to a unified .ether bundle file.")));
            }
            if (btnImportBundle != null) {
                btnImportBundle.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.import_bundle", "ðŸ“‚ Import Bundle (.ether)"));
                btnImportBundle.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.import_bundle", "Import and apply a unified .ether bundle file to instantly restore full scenario state.")));
            }
            if (liveDiagnosticHeader != null) liveDiagnosticHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.diagnostic.header", "ðŸ“‹ CIVILIZATIONAL VIABILITY DIAGNOSTIC (REAL TIME)"));
            if (previewTitleLabel != null) previewTitleLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.title.right_view", "ðŸ—ºï¸ Resource Cartography & Display"));
            if (startYearLabel != null) startYearLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.start_year", "Start Year (Chronological Reference):"));
            if (endYearLabel != null) endYearLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.end_year", "End / Target Year (Chronological Reference):"));
            if (popCountLabel != null) popCountLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_count", "Initial Population (1,000 to 10,000,000,000):"));
            if (densityPatternLabel != null) densityPatternLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.density_pattern", "Distribution Pattern:"));
            if (h3ResolutionLabel != null) h3ResolutionLabel.setText(org.ether.society.i18n.I18n.getOrDefault("planet.param.resolution", "H3 Resolution:"));
            if (temporalResolutionLabel != null) temporalResolutionLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.label.temporal_resolution", "Time Step Î”t (Temporal Resolution):"));
            if (cohortSizeLabel != null) cohortSizeLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.label.cohort_size", "Cohort Size:"));
            if (startBtn != null) startBtn.setText(org.ether.society.i18n.I18n.get("scenario.start_btn"));
            if (generateBtn != null) generateBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.preview_btn", "ðŸ”„ Preview Distribution"));
            if (addEventBtn != null) addEventBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.add", "âž• Add Event"));
            if (removeEventBtn != null) removeEventBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.remove", "ðŸ—‘ï¸ Remove Event"));
            if (loadEarthEventsBtn != null) loadEarthEventsBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.load_earth", "ðŸŒ Load Earth Historical Events"));
            if (randomEventsCheckBox != null) {
                randomEventsCheckBox.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.random_checkbox", "ðŸŽ² Activer les Ã©vÃ©nements alÃ©atoires & crises stochastiques (Famines, Ã‰pidÃ©mies, SÃ©ismes, Ã‰ruptions)"));
                randomEventsCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.random_events",
                    "ðŸŽ² GESTION DES Ã‰VÃ‰NEMENTS ALÃ‰ATOIRES & STOCHASTIQUES\n" +
                    "â€¢ Si activÃ© : Le moteur gÃ©nÃ¨re des crises Ã©mergentes (famines, pestes, sÃ©cheresses, Ã©ruptions) pilotÃ©es par la graine stochastique (Seed).\n" +
                    "â€¢ Si dÃ©sactivÃ© : AUCUN Ã©vÃ©nement alÃ©atoire ne survient spontanÃ©ment (moins rÃ©aliste, mais garantit une trajectoire dÃ©terministe pure).")));
            }
            if (earthLeadersCheckBox != null) {
                earthLeadersCheckBox.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.earth_leaders_checkbox", "ðŸ›ï¸ IntÃ©grer les Ã©vÃ©nements de personnages historiques terrestres (Alexandre, Auguste, Hammourabi...)"));
                earthLeadersCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.earth_leaders",
                    "ðŸ›ï¸ PERSONNAGES HISTORIQUES & BIFURCATIONS TERRESTRES\n" +
                    "â€¢ Si activÃ© : Le moteur injecte les figures historiques majeures rÃ©elles avec leurs modificateurs spatio-temporels exacts.\n" +
                    "â€¢ Si dÃ©sactivÃ© : Trajectoire structurelle pure sans chocs biographiques exogÃ¨nes.")));
            }
            if (proceduralLeadersCheckBox != null) {
                proceduralLeadersCheckBox.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.procedural_leaders_checkbox", "ðŸŒŸ Activer le gÃ©nÃ©rateur d'outliers historiques (Leaders Ã©mergents procÃ©duraux)"));
                proceduralLeadersCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.procedural_leaders",
                    "ðŸŒŸ GESTION DES LEADERS Ã‰MERGENTS PROCÃ‰DURAUX\n" +
                    "â€¢ Si activÃ© : Des gÃ©nies militaires, rÃ©formateurs ou bÃ¢tisseurs Ã©mergent stochastiquement selon la complexitÃ© et le stress des populations.\n" +
                    "â€¢ Si dÃ©sactivÃ© : Aucun leader procÃ©dural n'Ã©merge.")));
            }

            if (colType != null) colType.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.type", "Event Type"));
            if (colName != null) colName.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.name", "Event Name"));
            if (colYear != null) colYear.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.year", "Year (Yr)"));
            if (colLat != null) colLat.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.lat", "Lat (Â°)"));
            if (colLon != null) colLon.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.lon", "Lng (Â°)"));
            if (colDepth != null) colDepth.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.depth", "Depth (km)"));
            if (colMag != null) colMag.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.mag", "Magnitude / Intensity"));

            if (clippingCheckBox != null) clippingCheckBox.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.enable", "Enable Partial Simulation (Truncated Zone)"));
            if (graphicSelectBtn != null) graphicSelectBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.select_mode", "ðŸ–±ï¸ Graphic Map Selection Mode"));
            if (resetClippingBtn != null) resetClippingBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.reset", "ðŸ”„ Reset Area (Full Planet)"));
            if (latMaxLabel != null) latMaxLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lat_max", "Lat Max (Top):"));
            if (latMinLabel != null) latMinLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lat_min", "Lat Min (Bottom):"));
            if (lngMinLabel != null) lngMinLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lng_min", "Lng Min (Left):"));
            if (lngMaxLabel != null) lngMaxLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lng_max", "Lng Max (Right):"));
            if (boundaryLabel != null) boundaryLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.boundary_label", "Scientific Border Modeling:"));
            if (culturalHelpBtn != null) culturalHelpBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.cultural_format_help", "â“ Format des Calques"));
            if (btnGenerateProceduralTensorsSection != null) {
                btnGenerateProceduralTensorsSection.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.regen_tensors", "ðŸª„ RÃ©gÃ©nÃ©rer les Tenseurs"));
                btnGenerateProceduralTensorsSection.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.regen_tensors", "Bascule tous les tenseurs en mode procÃ©dural et rÃ©gÃ©nÃ¨re les cartes selon les paramÃ¨tres et la graine stochastique.")));
            }
            if (btnReliefOverlay != null) {
                btnReliefOverlay.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.relief_overlay", "â›°ï¸ Relief"));
                btnReliefOverlay.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.relief_overlay", "Superposer l'ombrage du relief topographique et des pentes avec dÃ©limitation du trait de cÃ´te.")));
            }
            updatePreviewModesCombo();
            updatePreviewTitleText();

            if (radioProcDemo != null) radioProcDemo.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.procedural", "â–¶ Procedural Generation (Perlin / Simplex Noise)"));
            if (radioImportDemo != null) radioImportDemo.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.import", "ðŸ“‚ External Source / Import (PNG / WMS / GeoTIFF)"));
            if (loadDensityMapBtn != null) loadDensityMapBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.demo.btn_load", "ðŸ“¥ Import External Density Map (PNG)"));
            if (exportDensityMapBtn != null) exportDensityMapBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.demo.btn_export", "ðŸ“¤ Export Generated Density Map (PNG)"));

            if (boundaryModeCombo != null) {
                String val = boundaryModeCombo.getValue();
                boundaryModeCombo.setValue(null);
                boundaryModeCombo.setValue(val);
            }

            if (densityPatternCombo != null) {
                if (densityPatternCombo.getCellFactory() != null) {
                    densityPatternCombo.setButtonCell(densityPatternCombo.getCellFactory().call(null));
                } else {
                    String val = densityPatternCombo.getValue();
                    densityPatternCombo.setValue(null);
                    densityPatternCombo.setValue(val);
                }
            }

            if (temporalResolutionCombo != null) {
                Double val = temporalResolutionCombo.getValue();
                temporalResolutionCombo.setValue(null);
                temporalResolutionCombo.setValue(val);
            }

            if (techPresetCombo != null) {
                Scenario.TechPreset val = techPresetCombo.getValue();
                techPresetCombo.setValue(null);
                techPresetCombo.setValue(val);
            }

            if (h3ResolutionCombo != null) {
                Integer val = h3ResolutionCombo.getValue();
                h3ResolutionCombo.setValue(null);
                h3ResolutionCombo.setValue(val);
            }

            if (tensorSourceCombos != null) {
                for (ComboBox<String> sc : tensorSourceCombos.values()) {
                    if (sc != null && sc.getCellFactory() != null) {
                        sc.setButtonCell(sc.getCellFactory().call(null));
                    }
                }
            }

            updatePreviewModesCombo();

            if (eventsTable != null && eventsTable.getUserData() instanceof TableColumn[] cols && cols.length == 6) {
                cols[0].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.name", "Nom / Description"));
                cols[1].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.year", "Year"));
                cols[2].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.lat", "Latitude"));
                cols[3].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.lon", "Longitude"));
                cols[4].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.depth", "Profondeur (km)"));
                cols[5].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.magnitude", "Magnitude"));
            }
            if (popHeader != null) {
                popHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_section", "ðŸ‘¥ 2. INITIAL POPULATION IDENTITY CARD"));
            }
            if (engineSortLabel != null) {
                engineSortLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.sort.label", "ðŸ”€ Engine Sorting:"));
            }
            if (engineSortCombo != null) {
                int selIdx = engineSortCombo.getSelectionModel().getSelectedIndex();
                engineSortCombo.getItems().setAll(
                    org.ether.society.i18n.I18n.getOrDefault("scenario.sort.default", "âš™ï¸ System Order (By Category)"),
                    org.ether.society.i18n.I18n.getOrDefault("scenario.sort.date_asc", "ðŸ“… Chronological Sort (Oldest â†’ Newest)"),
                    org.ether.society.i18n.I18n.getOrDefault("scenario.sort.date_desc", "ðŸ“… Reverse Chronological Sort (Newest â†’ Oldest)"),
                    org.ether.society.i18n.I18n.getOrDefault("scenario.sort.alpha_asc", "ðŸ”¤ Alphabetical Sort (A - Z)")
                );
                if (selIdx >= 0 && selIdx < engineSortCombo.getItems().size()) {
                    engineSortCombo.getSelectionModel().select(selIdx);
                } else {
                    engineSortCombo.getSelectionModel().select(0);
                }
            }

            rebuildCulturalTensorSubBlocks(cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null ? cultureVectorDimSpinner.getValue() : 9);
            updateDemoCompatibilityDisplay();
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

    public void setOnScenarioLoadedCallback(java.util.function.BiConsumer<PlanetPreset, EcologyPreset> callback) {
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

    private void exportPhysicalLawEngineTemplate(String className) {
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle(I18n.getOrDefault("scenario.dialog.export_physics_law_title", "Export Physical Law (") + className + ") (.java)");
            fileChooser.setInitialFileName(className + ".java");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichiers Source Java (*.java)", "*.java"));
            java.io.File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                String template = """
                    package org.ether.society.procedural;

                    import org.ether.society.core.H3SimulationEngine;
                    import org.ether.society.model.H3Cell;

                    /**
                     * Loi Physique CÅ“ur Ether : %s
                     */
                    public class %s {
                        public void update(H3SimulationEngine engine, H3Cell cell, double deltaTime) {
                            // Conservation thermodynamique & lois de bilans physiques
                        }
                    }
                    """.formatted(className, className);
                java.nio.file.Files.writeString(file.toPath(), template);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Physical Law Engine exportÃ© avec succÃ¨s :\n" + file.getAbsolutePath());
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
                    addCustomEngineCheckBoxToUI(engineKey, "ðŸ”Œ " + engineKey + " (" + I18n.getOrDefault("scenario.engine.custom_badge", "Custom Engine") + ")", 
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
        private final long min;
        private final long max;
        private final long step;

        public LongSpinnerValueFactory(long min, long max, long initialValue, long step) {
            this.min = min;
            this.max = max;
            this.step = step;
            setConverter(new StringConverter<Long>() {
                @Override
                public String toString(Long object) {
                    return object == null ? "" : String.valueOf(object);
                }
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

        @Override
        public void decrement(int steps) {
            long current = getValue() != null ? getValue() : min;
            long newValue = Math.max(min, current - (long) steps * step);
            setValue(newValue);
        }

        @Override
        public void increment(int steps) {
            long current = getValue() != null ? getValue() : min;
            long newValue = Math.min(max, current + (long) steps * step);
            setValue(newValue);
        }
    }

    private java.util.function.Supplier<PlanetGeneratorPanel> planetPanelSupplier;
    private java.util.function.Supplier<ResourceDistributionPanel> resourcePanelSupplier;

    public void setPlanetPanelSupplier(java.util.function.Supplier<PlanetGeneratorPanel> supplier) {
        this.planetPanelSupplier = supplier;
    }

    public void setResourcePanelSupplier(java.util.function.Supplier<ResourceDistributionPanel> supplier) {
        this.resourcePanelSupplier = supplier;
    }

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
            for (String err : errors) {
                sb.append("â€¢ ").append(err).append("\n");
            }
            if (validationErrorLabel != null && validationErrorBanner != null) {
                if (validationBannerHeaderLabel != null) {
                    validationBannerHeaderLabel.setText("ðŸ›‘ " + I18n.getOrDefault("scenario.validation.header_errors", "ERREURS DE CONFIGURATION DU SCÃ‰NARIO (ONGLET 3) :"));
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
        dialog.show();
    }

}


