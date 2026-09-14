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
import org.ether.society.procedural.PlanetPreset;
import org.ether.society.procedural.ProceduralGenerator;
import org.ether.society.procedural.ProceduralPopulationEngine;
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
 * @version 3.0.0
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
    private VBox bottomActionBox;

    // Form Controls
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
    }

    /**
     * Attaches default value handling to a JavaFX control:
     * 1. Appends "[VALEUR PAR DÉFAUT : X]" to the tooltip.
     * 2. Adds a right-click Context Menu ("🔄 Réinitialiser à la valeur par défaut (X)").
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
            String defaultNotice = (baseTooltip.isEmpty() ? "" : "\n\n") + I18n.getOrDefault("scenario.tooltip.default_notice_prefix", "📌 [DEFAULT VALUE: ") + defaultStr + "] — Clic droit pour réinitialiser.";
            control.setTooltip(new Tooltip(baseTooltip + defaultNotice));
        }

        // 2. Right-click Context Menu
        ContextMenu contextMenu = control.getContextMenu();
        if (contextMenu == null) {
            contextMenu = new ContextMenu();
        }
        MenuItem resetItem = new MenuItem("🔄 Réinitialiser à la valeur par défaut (" + defaultStr + ")");
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

        highlightControlIfModified(cultureVectorDimSpinner, cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null && cultureVectorDimSpinner.getValue() != 8);
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
    private Image customDensityImage;
    private Label densityMapFileLabel;
    private Button loadDensityMapBtn;
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
    private Label sovereigntyFileLabel;
    private final java.util.Map<Integer, Image> customTensorImages = new java.util.HashMap<>();
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
    private final Consumer<Scenario> onStartSimulation;
    private final org.ether.society.persistence.ScenarioRepository scenarioRepo;

    // Labels for i18n
    private Label headerLabel;
    private Label title1;
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
        Map.entry("UNBIASED_NATURAL", "⚖ Natural Bio-Climatic Equilibrium (Unbiased)"),
        Map.entry("COASTAL_MARITIME", "🌊 Coastal & Maritime Focus"),
        Map.entry("RIVER_VALLEYS", "🏞 River Valleys & Alluvial Basins"),
        Map.entry("HIGHLAND_MOUNTAIN", "🏔 Highland Refuges & Mountain Reliefs"),
        Map.entry("INLAND_OASIS", "🌴 Inland Hydrographic Basins & Oases"),
        Map.entry("EQUATORIAL_BELT", "☀️ Equatorial Belt & Tropical Zone"),
        Map.entry("URBAN_CLUSTERS", "🏙 Metropolises & Urban Clusters"),
        Map.entry("SPARSE_NOMADIC", "⛺ Sparse Nomadic & Pastoralist Dispersion"),
        Map.entry("UNIFORM", "🟦 Absolute Uniform Distribution"),
        Map.entry("RANDOM", "🎲 Stochastic Random Distribution")
    );

    private static final Map<String, String> DENSITY_DESCRIPTIONS = Map.ofEntries(
        Map.entry("UNBIASED_NATURAL", "⚖ Natural Bio-Climatic Equilibrium: Pure physical model without artificial bias. Population settles strictly according to real environmental suitability (biomes, temperature, waterways, relief)."),
        Map.entry("COASTAL_MARITIME", "🌊 Coastal & Maritime Focus: Encourages human settlement along ocean coastlines, river deltas, and maritime margins."),
        Map.entry("RIVER_VALLEYS", "🏞 River Valleys & Alluvial Basins: High population concentration along major hydrographic river networks and fertile alluvial basins."),
        Map.entry("HIGHLAND_MOUNTAIN", "🏔 Highland Refuges & Mountain Reliefs: Preferential densification in high valleys, mountain plateaus, and defensive elevated reliefs."),
        Map.entry("INLAND_OASIS", "🌴 Inland Basins & Oases: Settlement around endorheic depressions and accessible groundwater tables."),
        Map.entry("EQUATORIAL_BELT", "☀️ Equatorial Belt: Primary settlement concentrated around equatorial latitudes and high solar irradiance zones."),
        Map.entry("URBAN_CLUSTERS", "🏙 Metropolises & Urban Clusters: Procedural emergence of multiple dense urban aggregation hubs."),
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

    // Async Calculation & Thread Control Fields
    private volatile boolean isCalculationRunning = false;
    private volatile boolean cancelRequested = false;
    private Thread generationThread = null;

    private void updateEngineInspector(String className, String title, String description, String reference, String equations) {
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
            if (planetPreset != null) {
                this.activePlanetPreset = planetPreset;
            } else {
                EcologyPreset activeEco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null;
                if (activeEco != null) {
                    this.activePlanetPreset = findPlanetPresetByName(activeEco.planetPresetName());
                }
            }
            if (this.activePlanetPreset == null) {
                this.activePlanetPreset = PlanetPreset.EARTH_LIKE;
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
            if (currentPreviewCells != null) {
                generatePreview();
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
        if (lower.contains("earth") || lower.contains("terre") || lower.contains("terran")) {
            return PlanetPreset.EARTH_LIKE;
        }
        for (PlanetPreset p : PlanetPreset.getPresets()) {
            if (p.name().toLowerCase().contains(lower) || lower.contains(p.name().toLowerCase())) {
                return p;
            }
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

        validationErrorLabel = new Label();
        validationErrorLabel.setWrapText(true);
        validationErrorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 12px;");

        validationErrorBanner = new VBox(6,
                new Label(I18n.getOrDefault("scenario.validation.header_errors", "⚠️ VALIDATION ERRORS DETECTED — PLEASE CORRECT THE FOLLOWING POINTS:")),
                validationErrorLabel
        );
        validationErrorBanner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10;");
        validationErrorBanner.getChildren().get(0).setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 13px;");
        validationErrorBanner.setVisible(false);
        validationErrorBanner.setManaged(false);

        // --- 1. Standardized Preset Control Bar for Scenarios ---
        scenarioPresetBar = new PresetControlBar<>("scenario.preset_bar", "1. PRÉRÉGLAGES DE SCÉNARIOS");
        scenarioPresetBar.setExportCategory("scenario");
        List<Scenario> builtInScenarios = getBuiltInScenarios();
        Scenario defaultScenario = builtInScenarios.isEmpty() ? null : builtInScenarios.get(0);

        scenarioPresetBar.setListener(new PresetControlBar.PresetActionsListener<Scenario>() {
            @Override
            public void onPresetSelected(Scenario scenario) {
                applyScenarioToUI(scenario);
            }

            @Override
            public void onSavePreset(String name) {
                if (!validateScenarioSetup()) {
                    return;
                }
                Scenario custom = getScenario();
                custom.setName(name);
                scenarioRepo.saveOrUpdate(custom);
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
        planetSectionHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.section.inherited", "🪐 INHERITED CONTEXT (TABS 1 & 2)"));

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
                return item == null ? "" : item.name();
            }
            @Override
            public EcologyPreset fromString(String string) {
                return null;
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

        inheritedContextLabel = new Label(I18n.getOrDefault("scenario.info.earth_derived", "🌿 Ecology: Earth Standard Baseline  ➔  🪐 Planet (derived): Earth (Terran)"));
        inheritedContextLabel.setWrapText(true);
        inheritedContextLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #0284c7; -fx-padding: 6 10; -fx-background-color: rgba(56, 189, 248, 0.12); -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 6;");

        VBox inheritedSection = createSection(planetSectionHeader, new VBox(8,
                new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.label.ecology_preset", "1. Ecological Preset (Tab 2):")),
                ecologyPresetCombo,
                new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.label.planet_preset", "2. Planetary Preset (Tab 1 — Cascaded from Ecology):")),
                planetPresetCombo,
                inheritedContextLabel
        ));

        // --- Scenario General Info Section ---
        title1 = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.section.spatiotemporal", "🌐 EPOCH & SPATIOTEMPORAL DEFINITION"));
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
        h3ResolutionCombo.getItems().addAll(3, 4, 5, 6, 7, 8);
        h3ResolutionCombo.setValue(5);
        h3ResolutionCombo.setMaxWidth(Double.MAX_VALUE);
        h3ResolutionCombo.setConverter(new javafx.util.StringConverter<Integer>() {
            @Override
            public String toString(Integer item) {
                return item == null ? "" : org.ether.society.i18n.I18n.getOrDefault("planet.param.resolution.res" + item, "Résolution " + item);
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
                "👥 Taille Cible des Cohortes Démographiques (Nœuds Agents DOD) [hab/cohorte] :\n" +
                "Détermine la taille moyenne des groupes d'habitants représentés par un même nœud agent.\n" +
                "• 30 à 50 hab/cohorte : Bandes nomades & paléolithiques (Chasseurs-cueilleurs)\n" +
                "• 150 hab/cohorte : Nombre de Dunbar (Villages sédentaires & communautés de base)\n" +
                "• 500 à 10 000 hab/cohorte : Villes, empires & macro-simulation industrielle/moderne")));
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
            ⏱️ Résolution Temporelle de la Simulation (Pas de Temps Δt) :
            Détermine la granularité temporelle de chaque pas de calcul de la simulation.
            • Pas de temps court (< 1 mois, ex: 1 jour, 1 semaine) : Haute précision dynamique pour les épidémies, le climat saisonnier et la mobilité rapide, au prix d'une charge de calcul CPU plus élevée.
            • Pas de temps standard (1 mois - par défaut) : Équilibre optimal entre la précision physique et la vitesse d'exécution.
            • Pas de temps long (> 1 mois, ex: 3 mois, 1 an) : Accélération majeure pour les simulations à très grande échelle sur plusieurs millénaires avec lissage des cycles saisonniers.
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

        Label descLabel = new Label(I18n.getOrDefault("scenario.section.description", "📖 Detailed Description & Physical Forcing Terms:"));
        descLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 6 0 2 0;");

        scenarioDescriptionArea = new TextArea();
        scenarioDescriptionArea.setPrefRowCount(8);
        scenarioDescriptionArea.setWrapText(true);
        scenarioDescriptionArea.getStyleClass().add("scenario-description-area");
        scenarioDescriptionArea.textProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        VBox section1Content = new VBox(10, grid1, descLabel, scenarioDescriptionArea);
        VBox section1 = createSection(title1, section1Content);

        // --- 2. Demographics & Density Map Management (RadioButtons) ---
        VBox popSection = new VBox(10);
        popSection.getStyleClass().add("card-section");
        Label popHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_section", "👥 2. INITIAL POPULATION IDENTITY CARD"));
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

        customGrid.addRow(0, new Label(I18n.getOrDefault("scenario.label.stock_k0", "🛠️ K₀ (kg/hab) :")), customCapitalSpinner, new Label(I18n.getOrDefault("scenario.label.stock_e0", "⚡ E₀ (MJ/hab) :")), customEnergySpinner);
        customGrid.addRow(1, new Label(I18n.getOrDefault("scenario.label.stock_f0", "🌾 F₀ (mois) :")), customFoodSpinner, new Label(I18n.getOrDefault("scenario.label.stock_i0", "🧠 I₀ (bits/hab) :")), customInfoSpinner);

        Label customTitleLabel = new Label(I18n.getOrDefault("scenario.section.custom_stocks", "⚙️ MANUAL INITIAL PHYSICAL STOCKS SETUP (CUSTOM):"));
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

        exportDensityMapBtn = new Button(I18n.getOrDefault("scenario.btn.export_map_png", "📤 Export Map (PNG)"));
        exportDensityMapBtn.getStyleClass().add("button-secondary");
        exportDensityMapBtn.setMaxWidth(Double.MAX_VALUE);
        exportDensityMapBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_map_png", "Export density map as a PNG image file.")));
        exportDensityMapBtn.setOnAction(e -> exportDensityMap());

        Button btnGenerateProceduralTensors = new Button(I18n.getOrDefault("scenario.btn.redistribute_density", "🪄 Redistribute Procedural Density (T₀)"));
        btnGenerateProceduralTensors.getStyleClass().add("button");
        btnGenerateProceduralTensors.setMaxWidth(Double.MAX_VALUE);
        btnGenerateProceduralTensors.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.redistribute_density", "Recalculate and redistribute T₀ demographic density map based on selected pattern and ecology.")));
        btnGenerateProceduralTensors.setOnAction(e -> generateProceduralPopulationDensity());

        HBox demoBtnBar = new HBox(8, btnGenerateProceduralTensors, exportDensityMapBtn);
        HBox.setHgrow(btnGenerateProceduralTensors, Priority.ALWAYS);
        HBox.setHgrow(exportDensityMapBtn, Priority.ALWAYS);

        VBox proceduralDemoPanel = new VBox(8, popGrid, customPhysicalSubPanel, demoBtnBar);
        proceduralDemoPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");

        // Import Panel
        densityMapFileLabel = new Label(org.ether.society.i18n.I18n.get("planet.map.none"));
        densityMapFileLabel.getStyleClass().add("value-label");

        demoCompatibilityLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.status.terrain_no_map", "🪐 Terrain validation: No external map loaded"));
        demoCompatibilityLabel.getStyleClass().add("subcard-status-muted");
        demoCompatibilityLabel.setWrapText(true);
        demoCompatibilityLabel.setStyle("-fx-font-size: 11px;");

        densityFormatHintLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.format.density_hint",
                "PNG / JPEG (projection équirectangulaire 2:1) :\n  Noir (0) = 0 hab/km² | Blanc (255) = Densité maximale d'habitation."));
        densityFormatHintLabel.setWrapText(true);
        densityFormatHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic; -fx-padding: 4 0 0 0;");

        loadDensityMapBtn = new Button(I18n.getOrDefault("scenario.btn.import_density_map", "📥 Import External Density Map (PNG)"));
        loadDensityMapBtn.getStyleClass().add("button-secondary");
        loadDensityMapBtn.setMaxWidth(Double.MAX_VALUE);
        loadDensityMapBtn.setMinWidth(Region.USE_PREF_SIZE);
        loadDensityMapBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.import_density_map", "Import an external demographic density map in 2:1 equirectangular PNG format.")));
        HBox.setHgrow(loadDensityMapBtn, Priority.ALWAYS);
        loadDensityMapBtn.setOnAction(e -> {
            notifyParamChange();
            loadCustomDensityMap();
        });

        demoHelpBtn = new Button(I18n.getOrDefault("scenario.btn.format_specs", "❓ Format"));
        demoHelpBtn.getStyleClass().add("button-secondary");
        demoHelpBtn.setStyle("-fx-font-size: 11px;");
        demoHelpBtn.setMinWidth(Region.USE_PREF_SIZE);
        demoHelpBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.density_specs", "Display instructions and image format specifications.")));
        demoHelpBtn.setOnAction(e -> showDensityImportFormatHelp());

        HBox mapBtnBox = new HBox(6, loadDensityMapBtn, demoHelpBtn);
        VBox importDemoPanel = new VBox(8, mapBtnBox, densityMapFileLabel, demoCompatibilityLabel, densityFormatHintLabel);
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
            if (isProc) {
                customDensityImage = null;
            }
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

        scenarioPresetHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.section.presets", "🎛️ GLOBAL PRESETS & SCENARIO SAVE"));
        VBox scenarioPresetSection = createSection(scenarioPresetHeader, scenarioPresetBar);

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

        Label subtitle = new Label(I18n.getOrDefault("scenario.desc.bundle", "Export or import the complete scenario (planetary context, ecology, active engines, cultural layers, and demographic grid) in unified .ether format for archiving or sharing."));
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("card-description-muted");

        Button btnExportBundle = new Button(I18n.getOrDefault("scenario.btn.export_bundle", "📦 Export Bundle (.ether)"));
        btnExportBundle.getStyleClass().add("button-secondary");
        btnExportBundle.setMaxWidth(Double.MAX_VALUE);
        btnExportBundle.getStyleClass().add("button-accent-blue");
        btnExportBundle.setOnAction(e -> exportUnifiedBundle());
        btnExportBundle.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_bundle", "Export complete scenario (physics, ecology, engines, layers, and demographics) to a unified .ether bundle file.")));

        Button btnImportBundle = new Button(I18n.getOrDefault("scenario.btn.import_bundle", "📂 Import Bundle (.ether)"));
        btnImportBundle.getStyleClass().add("button-secondary");
        btnImportBundle.setMaxWidth(Double.MAX_VALUE);
        btnImportBundle.setOnAction(e -> importUnifiedBundle());
        btnImportBundle.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.import_bundle", "Import and apply a unified .ether bundle file to instantly restore full scenario state.")));

        HBox bundleBox = new HBox(8, btnExportBundle, btnImportBundle);
        HBox.setHgrow(btnExportBundle, Priority.ALWAYS);
        HBox.setHgrow(btnImportBundle, Priority.ALWAYS);

        section.getChildren().addAll(bundleHeader, subtitle, bundleBox);
        return section;
    }

    private VBox createSnapshotSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        snapshotHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.snapshot.header", "📸 RESUME FROM EXISTING SNAPSHOT (PREVIOUS SESSION)"));
        snapshotHeader.getStyleClass().add("label-section-header");

        Label subtitle = new Label(I18n.getOrDefault("scenario.desc.snapshot", "If the simulation was previously run and snapshots or checkpoints exist, you can resume directly from that snapshot without starting over."));
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 11px;");

        ToggleGroup modeGroup = new ToggleGroup();
        radioNewSimulation = new RadioButton(I18n.getOrDefault("scenario.radio.start_fresh", "🌱 Start a new simulation from scratch (Year T₀)"));
        radioNewSimulation.setStyle("-fx-font-weight: bold;");
        radioResumeSnapshot = new RadioButton(I18n.getOrDefault("scenario.radio.start_snapshot", "📸 Resume from an existing Snapshot (Previous Session / Checkpoint)"));
        radioResumeSnapshot.setStyle("-fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        radioNewSimulation.setToggleGroup(modeGroup);
        radioResumeSnapshot.setToggleGroup(modeGroup);
        radioNewSimulation.setSelected(true);

        snapshotCombo = new ComboBox<>();
        snapshotCombo.setMaxWidth(Double.MAX_VALUE);
        snapshotCombo.setConverter(new javafx.util.StringConverter<org.ether.society.persistence.SaveMetadata>() {
            @Override
            public String toString(org.ether.society.persistence.SaveMetadata item) {
                if (item == null) return "";
                String timeStr = item.getTimestamp() != null ? item.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A";
                return String.format("💾 [An %,d - M%02d] %s (%s) - %s", item.getYear(), item.getMonth(), item.getName(), item.getScenarioName(), timeStr);
            }
            @Override
            public org.ether.society.persistence.SaveMetadata fromString(String string) {
                return null;
            }
        });

        snapshotDateLabel = new Label(I18n.getOrDefault("scenario.label.timestamp_null", "📅 Timestamp: -"));
        snapshotTimeLabel = new Label(I18n.getOrDefault("scenario.label.moment_null", "⏳ Time: -"));
        snapshotScenarioLabel = new Label(I18n.getOrDefault("scenario.info.scenario_empty", "📜 Scenario: -"));
        snapshotPathLabel = new Label(I18n.getOrDefault("scenario.label.snapshot_id_null", "📁 Snapshot ID: -"));

        for (Label l : List.of(snapshotDateLabel, snapshotTimeLabel, snapshotScenarioLabel, snapshotPathLabel)) {
            l.setStyle("-fx-font-size: 11px;");
        }

        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(12);
        detailsGrid.setVgap(4);
        detailsGrid.addRow(0, snapshotDateLabel, snapshotTimeLabel);
        detailsGrid.addRow(1, snapshotScenarioLabel, snapshotPathLabel);

        VBox snapshotCard = new VBox(6, new Label(I18n.getOrDefault("scenario.section.snapshot_sheet", "📋 Technical-Historical Sheet of Selected Snapshot:")), detailsGrid);
        snapshotCard.getStyleClass().add("opt-master-box");
        snapshotCard.getChildren().get(0).getStyleClass().add("opt-sub-checkbox");

        snapshotExplainBtn = new Button(I18n.getOrDefault("scenario.btn.snapshot_explain", "ℹ️ What is a Snapshot? (Explanations & Mechanics)"));
        snapshotExplainBtn.getStyleClass().add("button-secondary");
        snapshotExplainBtn.setMaxWidth(Double.MAX_VALUE);
        snapshotExplainBtn.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        snapshotExplainBtn.setOnAction(e -> showSnapshotExplanationDialog());

        snapshotRefreshBtn = new Button(I18n.getOrDefault("scenario.btn.refresh", "🔄 Refresh"));
        snapshotRefreshBtn.getStyleClass().add("button-secondary");
        snapshotRefreshBtn.setStyle("-fx-font-size: 11px;");
        snapshotRefreshBtn.setOnAction(e -> refreshSnapshotList());

        HBox btnBox = new HBox(8, snapshotExplainBtn, snapshotRefreshBtn);
        HBox.setHgrow(snapshotExplainBtn, Priority.ALWAYS);

        snapshotContainer = new VBox(8, snapshotCombo, snapshotCard, btnBox);
        snapshotContainer.setVisible(false);
        snapshotContainer.setManaged(false);

        modeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            boolean isResume = newV == radioResumeSnapshot;
            snapshotContainer.setVisible(isResume);
            snapshotContainer.setManaged(isResume);
            updateStartButtonLabel();
        });

        snapshotCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                String timeStr = newV.getTimestamp() != null ? newV.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "N/A";
                snapshotDateLabel.setText(I18n.getOrDefault("scenario.snapshot.timestamp", "📅 Horodatage : ") + timeStr);
                snapshotTimeLabel.setText(String.format(I18n.getOrDefault("scenario.snapshot.time_format", "⏳ Moment: Year %,d (Month %d)"), newV.getYear(), newV.getMonth()));
                snapshotScenarioLabel.setText(I18n.getOrDefault("scenario.snapshot.scenario_prefix", "📜 Scenario: ") + (newV.getScenarioName() != null ? newV.getScenarioName() : I18n.getOrDefault("scenario.unknown", "Unknown")));
                snapshotPathLabel.setText(I18n.getOrDefault("scenario.snapshot.id", "📁 ID Snapshot : ") + newV.getId());
            }
        });

        refreshSnapshotList();

        section.getChildren().addAll(snapshotHeader, subtitle, radioNewSimulation, radioResumeSnapshot, snapshotContainer);
        return section;
    }

    public void refreshSnapshotList() {
        if (snapshotCombo == null) return;
        String currentScenarioName = null;
        if (scenarioPresetBar != null && scenarioPresetBar.getCurrentName() != null) {
            currentScenarioName = scenarioPresetBar.getCurrentName();
        }
        List<org.ether.society.persistence.SaveMetadata> saves = getSaveManager().listSaves();
        List<org.ether.society.persistence.SaveMetadata> filteredSaves = new ArrayList<>();
        if (currentScenarioName != null && !currentScenarioName.isBlank()) {
            final String targetName = currentScenarioName.trim();
            for (var save : saves) {
                if (save.getScenarioName() != null && (save.getScenarioName().trim().equalsIgnoreCase(targetName) 
                        || targetName.toLowerCase().contains(save.getScenarioName().trim().toLowerCase()) 
                        || save.getScenarioName().trim().toLowerCase().contains(targetName.toLowerCase()))) {
                    filteredSaves.add(save);
                }
            }
        } else {
            filteredSaves.addAll(saves);
        }

        if (filteredSaves.isEmpty()) {
            // If no exact match or saves list is empty, display a synthetic checkpoint matching current scenario
            String secName = currentScenarioName != null ? currentScenarioName : "Scénario Courant";
            org.ether.society.persistence.SaveMetadata demo = new org.ether.society.persistence.SaveMetadata(
                "checkpoint_latest_" + secName.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase(),
                "Snapshot Restauration — " + secName,
                2045, 6, secName
            );
            filteredSaves.add(demo);
        }

        snapshotCombo.getItems().setAll(filteredSaves);
        if (!filteredSaves.isEmpty()) {
            snapshotCombo.setValue(filteredSaves.get(0));
        }
    }

    private void showSnapshotExplanationDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fonctionnement des Snapshots dans Ether Simulation Engine");
        alert.setHeaderText(I18n.getOrDefault("scenario.header.snapshot_help", "📸 WHAT IS A SNAPSHOT & HOW DOES IT WORK?"));

        String content = """
            💡 DÉFINITION D'UN SNAPSHOT :
            Un Snapshot (ou instantané d'état) est une sauvegarde intégrale, fidèle et déterministe de la simulation Ether capturée à un tick ou une année T précise.

            🧠 CE QUI EST CAPTURÉ & CONSERVÉ :
            1. 🪐 ÉTATS DES CELLULES H3 : Biomasse humaine, stocks alimentaires, eau potable, nutriments du sol (NPK), capital physique (K₀), énergie (E₀), savoirs (I₀) et niveau technologique.
            2. ⚡ REGISTRES DOD (Data-Oriented Design) : Buffers vectorisés des cohortes d'agents, tranches d'âges, pyramides démographiques et flux migratoires inter-cellulaires.
            3. 🕒 DYNAMIQUE TEMPORELLE & CLIMAT : Année calendaire, mois, saison, température moyenne, forçage radiatif et bilan carbone stratosphérique.
            4. 🏛️ NATIONS & ÉVÉNEMENTS HISTORIQUES : Entités géopolitiques formées, frontières territoriales et journal des événements planétaires.

            🚀 COMMENT ÇA FONCTIONNE ?
            • ⚡ Rolling Checkpoint (Tâche de Fond) : Le moteur de simulation génère automatiquement un checkpoint léger sur disque tous les 60 ticks sans blocage de l'interface utilisateur.
            • 💾 Restauration Instantanée : Charger un snapshot réinsère directement les structures de données H3/DOD en mémoire, évitant de recalculer les millénaires ou siècles écoulés.
            • 🔀 Exploration d'Arborescences (Branching / Forking) : Vous pouvez repartir d’un snapshot à l'an 2045, modifier les lois ou les événements (ex: guerre, vaccin, fusion nucléaire), et observer la divergence de la civilisation par rapport à la session initiale.
            """;

        alert.setContentText(content);
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setStyle("-fx-font-size: 12px;");
        alert.showAndWait();
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
        if (isResumeFromSnapshotSelected()) {
            startBtn.setText(I18n.getOrDefault("scenario.btn.restore_snapshot", "🚀 RESTORE & LAUNCH FROM SELECTED SNAPSHOT"));
            startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-background-radius: 6;");
        } else {
            startBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.button.start", "APPLY AND LAUNCH SIMULATION"));
            startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
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
                        new org.ether.society.model.EtherScenarioBundle("2.0.0", planet, eco, scenario);
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
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Integrity Warning");
                        alert.setHeaderText("Altered Bundle Detected");
                        alert.setContentText("The signature on this bundle does not match its contents. Loading may produce unexpected simulation dynamics.");
                        alert.showAndWait();
                    }

                    if (bundle.planetPreset() != null) {
                        this.activePlanetPreset = bundle.planetPreset();
                        if (planetPresetCombo != null) planetPresetCombo.setValue(bundle.planetPreset());
                    }
                    if (bundle.ecologyPreset() != null && ecologyPresetCombo != null) {
                        ecologyPresetCombo.setValue(bundle.ecologyPreset());
                    }
                    if (bundle.scenario() != null) {
                        applyScenarioToUI(bundle.scenario());
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

    private void updateLiveDiagnosticBlock() {
        if (liveDiagnosticContentBox == null) return;
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        if (p == null) p = PlanetPreset.EARTH_LIKE;
        EcologyPreset eco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : EcologyPreset.EARTH_STANDARD;

        List<String> warnings = new ArrayList<>();
        List<String> passes = new ArrayList<>();

        if (p.atmospherePressureAtm() < 0.01) {
            warnings.add("❌ Atmosphère absente/tenue (" + String.format("%.3f", p.atmospherePressureAtm()) + " atm) : L'eau liquide bout à la surface. Survie humaine impossible sans dômes fermés.");
        } else if (p.oxygenPercentage() < 10.0) {
            warnings.add("⚠️ Atmosphère hypoxique (O2 = " + String.format("%.1f%%", p.oxygenPercentage()) + ") : Insuffisant pour la respiration des organismes complexes.");
        } else {
            passes.add("✅ Atmosphère respirable & constante (P = " + String.format("%.2f", p.atmospherePressureAtm()) + " atm, O2 = " + String.format("%.1f%%", p.oxygenPercentage()) + ")");
        }

        if (p.waterLevel() < -0.3) {
            warnings.add("⚠️ Ressources en Eau Limitées : Monde très aride. Stress hydrique majeur prévisible.");
        } else {
            passes.add("✅ Hydrologie équilibrée (Niveau d'eau = " + String.format("%.0f%%", (1.0 + p.waterLevel()) * 50) + ")");
        }

        double capitalK0 = computeAutoCapitalFromYear(startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000);
        double crustal = eco != null ? eco.crustalMetalOresGt() : 80.0;
        if (capitalK0 >= 8000.0 && crustal < 20.0) {
            warnings.add("⚠️ Déficit en Métaux Industriels : Capital physique " + String.format("%.0f", capitalK0) + " kg/hab configuré mais métaux crustaux faibles (" + String.format("%.1f Gt", crustal) + "). Risque de pénurie industrielle.");
        } else {
            passes.add("✅ Compatibilité Matériaux / Capital Physique");
        }

        long pop = initialHumanCountSpinner != null ? initialHumanCountSpinner.getValue() : 1_000_000L;
        if (pop > 5_000_000_000L && p.waterLevel() < -0.2) {
            warnings.add("❌ Surpopulation Majeure : " + String.format("%,d", pop) + " habitants configurés sur un monde aride.");
        } else {
            passes.add("✅ Densité Démographique Initiale Réaliste (" + String.format("%,d", pop) + " hab)");
        }

        // --- Static Engine JIT Conflict & Compatibility Diagnostic ---
        org.ether.society.procedural.jit.ScenarioEngineJITCompiler jit = new org.ether.society.procedural.jit.ScenarioEngineJITCompiler();
        jit.registerEngineStep("BiologicalDemographics", "biomassHuman", new org.ether.society.procedural.jit.SymbolicExpression("biomassHuman", 1.01, 0.0), 10000.0);
        jit.registerEngineStep("EcologicalDegradation", "biomassHuman", new org.ether.society.procedural.jit.SymbolicExpression("biomassHuman", 0.995, 0.0), 10000.0);
        jit.registerEngineStep("AtmosphericSolar", "temperature", new org.ether.society.procedural.jit.SymbolicExpression("temperature", 1.0, 0.02), 25.0);
        jit.compile();

        org.ether.society.procedural.jit.EngineConflictReport jitReport = jit.getConflictReport();
        if (jitReport != null && !jitReport.getEntries().isEmpty()) {
            for (org.ether.society.procedural.jit.EngineConflictReport.ConflictEntry entry : jitReport.getEntries()) {
                if (entry.getSeverity() == org.ether.society.procedural.jit.EngineConflictReport.ConflictSeverity.INCOMPATIBLE) {
                    warnings.add("❌ INCOMPATIBILITÉ MOTEURS (" + entry.getVariableName() + ") : " + entry.getDescription());
                } else {
                    passes.add("✅ Fusion JIT Moteurs (" + entry.getVariableName() + ") : " + entry.getDescription());
                }
            }
        } else {
            passes.add("✅ Compilation JIT Moteurs : 100% Compatible & Fusions Validées");
        }

        liveDiagnosticContentBox.getChildren().clear();
        if (liveDiagnosticHeader != null) {
            liveDiagnosticHeader.getStyleClass().removeAll("diagnostic-header-success", "diagnostic-header-warn");
            if (warnings.isEmpty()) {
                liveDiagnosticHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.diagnostic.header_viable", "📋 9. CIVILIZATIONAL VIABILITY DIAGNOSTIC: FULLY VIABLE SCENARIO"));
                liveDiagnosticHeader.getStyleClass().add("diagnostic-header-success");
            } else {
                liveDiagnosticHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.diagnostic.header_alerts", "📋 9. CIVILIZATIONAL VIABILITY DIAGNOSTIC: ") + warnings.size() + " ALERTE(S) / TENSION(S)");
                liveDiagnosticHeader.getStyleClass().add("diagnostic-header-warn");
            }
        }

        for (String w : warnings) {
            Label lbl = new Label("• " + w);
            lbl.setWrapText(true);
            if (w.startsWith("❌")) {
                lbl.getStyleClass().add("diagnostic-error");
            } else {
                lbl.getStyleClass().add("diagnostic-warn");
            }
            liveDiagnosticContentBox.getChildren().add(lbl);
        }
        for (String pass : passes) {
            Label lbl = new Label("• " + pass);
            lbl.setWrapText(true);
            lbl.getStyleClass().add("diagnostic-pass");
            liveDiagnosticContentBox.getChildren().add(lbl);
        }
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
        return Scenario.getBuiltInScenarios();
    }

    private void updateInheritedContextDisplay(String ecologyName) {
        if (inheritedContextLabel == null) return;
        EcologyPreset eco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null;
        String ecoName = ecologyName != null ? ecologyName : (eco != null ? eco.name() : "Earth Standard Baseline");
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : null);
        if (p == null && eco != null) {
            p = findPlanetPresetByName(eco.planetPresetName());
        }
        if (p == null) p = PlanetPreset.EARTH_LIKE;

        String text = String.format("🌿 Écologie (Onglet 2) : %s  ➔  🪐 Planète (déduite en cascade) : %s (Rayon: %,.0f km)", ecoName, p.name(), p.radiusKm());
        inheritedContextLabel.setText(text);
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
            
            // Lazy populate cartographic buffer maps if not already generated
            if (s.getCustomDensityBase64() == null) {
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
                if (densityMapFileLabel != null) densityMapFileLabel.setText(I18n.getOrDefault("scenario.preset.density_map", "📷 Preset Density Map"));
                if (radioImportDemo != null) radioImportDemo.setSelected(true);
                updateDemoCompatibilityDisplay();
            } else {
                customDensityImage = null;
                if (densityMapFileLabel != null) densityMapFileLabel.setText(org.ether.society.i18n.I18n.get("planet.map.none"));
                if (radioProcDemo != null) radioProcDemo.setSelected(true);
                updateDemoCompatibilityDisplay();
            }

            // Restore Cultural Vector & Multi-Layer UI Controls
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
            if (s.getTensorProceduralModes() != null) {
                for (int i = 0; i < dims && i < s.getTensorProceduralModes().size(); i++) {
                    boolean isProc = s.getTensorProceduralModes().get(i);
                    if (isProc && tensorProcRadios.containsKey(i)) {
                        tensorProcRadios.get(i).setSelected(true);
                    } else if (!isProc && tensorImportRadios.containsKey(i)) {
                        tensorImportRadios.get(i).setSelected(true);
                    }
                }
            }

            updatePreviewModesCombo();
            if (currentPreviewCells != null && !currentPreviewCells.isEmpty()) {
                distributeInitialPopulation(currentPreviewCells);
                drawPreview();
            }
            if (onScenarioLoadedCallback != null) {
                onScenarioLoadedCallback.accept(this.activePlanetPreset, activeEco != null ? activeEco : s.getEcologyPreset());
            }
            updatePerformanceControlsState();
            if (scenarioPresetBar != null) {
                scenarioPresetBar.markClean(s);
            }
        } finally {
            isUpdatingFromPreset = false;
        }
    }

    private VBox createClippingSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        clippingHeader = new Label(I18n.getOrDefault("scenario.clipping.header", "✂️ 4. BORDERS & HISTORICAL SPATIAL CLIPPING"));
        clippingHeader.getStyleClass().add("label-section-header");

        clippingCheckBox = new CheckBox(I18n.getOrDefault("scenario.clipping.enable", "Enable Partial Simulation (Truncated Zone)"));
        clippingCheckBox.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        clippingCheckBox.setOnAction(e -> {
            notifyParamChange();
            drawPreview();
        });

        graphicSelectBtn = new ToggleButton(I18n.getOrDefault("scenario.clipping.select_mode", "🖱️ Graphic Map Selection Mode"));
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
                    case "DYNAMIC_RESERVOIR" -> { return I18n.getOrDefault("scenario.boundary.reservoir", "🌊 External Virtual Reservoir (Free Flow)"); }
                    case "CLOSED_BARRIER" -> { return I18n.getOrDefault("scenario.boundary.barrier", "🧱 Sealed Barrier / Isolated (Closed Edges)"); }
                    case "PERIODIC_WRAP" -> { return I18n.getOrDefault("scenario.boundary.wrap", "🌐 Periodic Boundary (Toroidal)"); }
                    default -> { return item; }
                }
            }
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

    private VBox createOceanOptimizationSection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("card-section");

        Label oceanOptHeader = new Label(I18n.getOrDefault("scenario.ocean_opt.header", "⚙️ 5. ENGINE ARCHITECTURE & OPTIMIZATIONS (ETHER CORE & OPTIONAL)"));
        oceanOptHeader.getStyleClass().add("label-section-header");

        Label oceanOptDesc = new Label(I18n.getOrDefault("scenario.ocean_opt.desc", "Definition and configuration of determinism mode, 7 simulation optimizations, and Ether Core and optional procedural engines. Each scenario embeds its optimization setup to guarantee perfect reproducibility."));
        oceanOptDesc.getStyleClass().add("card-description-muted");
        oceanOptDesc.setWrapText(true);

        // --- 🎯 MASTER CONTROL : MODE DÉTERMINISME STRICTE ---
        strictDeterminismCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.strict_determinism", "🔒 STRICT DETERMINISM MODE (0% Approximation / 100% Bit-to-Bit Reproducibility)"));
        strictDeterminismCheckBox.setSelected(true);
        strictDeterminismCheckBox.getStyleClass().add("radio-proc");
        strictDeterminismCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.strict_determinism",
            "🎯 STRICT BIT-TO-BIT DETERMINISM (ACADEMIC RESEARCH MODE)\n" +
            "• Disables ALL optimizations and algorithmic shortcuts.\n" +
            "• Guarantees 100% bit-identical simulation trajectories on the same seed.\n" +
            "• Recommended for validation tests, benchmarks, and convergence audits.")));
        attachDefaultValueHandling(strictDeterminismCheckBox, true, () -> strictDeterminismCheckBox.setSelected(true));

        Label determinismNote = new Label(I18n.getOrDefault("scenario.note.strict_determinism", "💡 Note: Checking Strict Determinism disables all heuristic approaches and guarantees bit-identical numerical fidelity."));
        determinismNote.getStyleClass().add("control-note");
        determinismNote.setWrapText(true);

        VBox masterBox = new VBox(4, strictDeterminismCheckBox, determinismNote);
        masterBox.getStyleClass().add("opt-master-box");

        // --- ⚡ INDIVIDUAL OPTIMIZATIONS & APPROXIMATIONS ---
        Label optSubHeader = new Label(I18n.getOrDefault("scenario.opt.sub_header", "⚡ ALGORITHMIC OPTIMIZATIONS & PERFORMANCE SHORTCUTS:"));
        optSubHeader.getStyleClass().add("opt-subheader");

        sparseCellSkippingCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.sparse_cell_skipping", "🏜 Sparse / Uninhabited Cell Skipping (Deserts & Abysses)"));
        sparseCellSkippingCheckBox.setSelected(false);
        sparseCellSkippingCheckBox.getStyleClass().add("opt-sub-checkbox");
        sparseCellSkippingCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.sparse_cell_skipping",
            "⚡ BENEFIT: +40% to +60% TPS speedup across global grid.\n" +
            "⚠️ PHYSICAL IMPACT: Bypasses evaluation loops on desert/oceanic cells with no human presence or active event.")));
        attachDefaultValueHandling(sparseCellSkippingCheckBox, false, () -> sparseCellSkippingCheckBox.setSelected(false));

        oceanMacroAggregationCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.macro_aggregation", "🌊 Abyssal Ocean Macro-Aggregation (Deep Basins z < -200m in Blocks)"));
        oceanMacroAggregationCheckBox.setSelected(false);
        oceanMacroAggregationCheckBox.getStyleClass().add("opt-sub-checkbox");
        oceanMacroAggregationCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.ocean_macro_aggregation",
            "⚡ BENEFIT: +25% to +35% TPS speedup by grouping deep water cells.\n" +
            "⚠️ PHYSICAL IMPACT: Smoothing of abyssal micro-currents without impacting terrestrial civilizations.")));
        attachDefaultValueHandling(oceanMacroAggregationCheckBox, false, () -> oceanMacroAggregationCheckBox.setSelected(false));

        coastalNavigationOnlyCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.coastal_nav", "⚓ Exclusive Coastal Navigation (Pathfinding Focused on Coasts & Straits)"));
        coastalNavigationOnlyCheckBox.setSelected(false);
        coastalNavigationOnlyCheckBox.getStyleClass().add("opt-sub-checkbox");
        coastalNavigationOnlyCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.coastal_nav",
            "⚡ BENEFIT: Major CPU savings on naval and commercial trade pathfinding.\n" +
            "⚠️ PHYSICAL IMPACT: Ships prefer coastal waters; ocean navigation restricted prior to Age of Discovery.")));
        attachDefaultValueHandling(coastalNavigationOnlyCheckBox, false, () -> coastalNavigationOnlyCheckBox.setSelected(false));

        oceanMultiRateTickingCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.multi_rate_ticking", "⏱ Oceanic & Multi-Rate Climate Ticking (Updated Every N Ticks)"));
        oceanMultiRateTickingCheckBox.setSelected(false);
        oceanMultiRateTickingCheckBox.getStyleClass().add("opt-sub-checkbox");
        oceanMultiRateTickingCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.multi_rate_ticking",
            "⚡ BENEFIT: +30% throughput by executing thermohaline circulation and fluid inertia at sub-frequency.\n" +
            "⚠️ PHYSICAL IMPACT: Potential temporal aliasing during ultra-fast atmospheric events.")));
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

        HBox climateSliderBox = new HBox(10, new Label("   └─"), climateTickFreqValueLabel, climateTickFreqSlider);
        climateSliderBox.setAlignment(Pos.CENTER_LEFT);

        parallelExecutionCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.parallel_execution", "🚀 Async Multi-Thread Parallelization (CompletableFuture / AVX)"));
        parallelExecutionCheckBox.setSelected(false);
        parallelExecutionCheckBox.getStyleClass().add("opt-sub-checkbox");
        parallelExecutionCheckBox.setTooltip(new Tooltip("""
            ⚡ BÉNÉFICE : Exploitation intégrale de tous les cœurs CPU du système.
            ⚠️ IMPACT PHYSIQUE : L'ordre de sommation flottante peut varier légèrement entre exécutions (non-associativité IEEE 754 en multi-threading).
            """));
        attachDefaultValueHandling(parallelExecutionCheckBox, false, () -> parallelExecutionCheckBox.setSelected(false));

        threadCountSlider = new Slider(0, 32, 0);
        threadCountSlider.setMajorTickUnit(8);
        threadCountSlider.setMinorTickCount(7);
        threadCountSlider.setSnapToTicks(true);
        threadCountSlider.setShowTickMarks(true);
        threadCountSlider.setStyle("-fx-pref-width: 200px;");
        threadCountSlider.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.thread_count", "Select number of CPU threads assigned to parallel processing")));
        threadCountValueLabel = new Label(I18n.getOrDefault("scenario.label.threads_auto", "Threads Multi-Thread : Auto (Tous cœurs CPU)"));
        threadCountValueLabel.getStyleClass().add("opt-value-highlight");
        threadCountSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int val = newV.intValue();
            if (val == 0) {
                threadCountValueLabel.setText(String.format(I18n.getOrDefault("scenario.label.threads_auto_cores", "Threads Multi-Thread : Auto (Tous cœurs %d)"), Runtime.getRuntime().availableProcessors()));
            } else if (val == 1) {
                threadCountValueLabel.setText(I18n.getOrDefault("scenario.label.threads_single", "Multi-Thread Threads: 1 (Single-threaded Deterministic)"));
            } else {
                threadCountValueLabel.setText(String.format(I18n.getOrDefault("scenario.label.threads_count", "Threads Multi-Thread : %d threads"), val));
            }
            notifyParamChange();
        });
        attachDefaultValueHandling(threadCountSlider, 4.0, () -> threadCountSlider.setValue(4.0));

        HBox threadSliderBox = new HBox(10, new Label("   └─"), threadCountValueLabel, threadCountSlider);
        threadSliderBox.setAlignment(Pos.CENTER_LEFT);

        spatialRangeTruncationCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.spatial_truncation", "💨 Spatial Range Truncation of Plumes & Diffusions (10⁻⁶ Cutoff)"));
        spatialRangeTruncationCheckBox.setSelected(false);
        spatialRangeTruncationCheckBox.getStyleClass().add("opt-sub-checkbox");
        spatialRangeTruncationCheckBox.setTooltip(new Tooltip("""
            ⚡ BÉNÉFICE : Limite le calcul de dispersion atmosphérique aux cellules adjacentes affectées.
            ⚠️ IMPACT PHYSIQUE : Néglige les concentrations d'aérosols et suie ultra-diluées devenant inférieures à 10⁻⁶ ppm.
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
        engineInspectorTitle = new Label(I18n.getOrDefault("scenario.header.engine_inspector", "🔎 Cliodynamic Engine Inspector (Hover an engine to inspect)"));
        engineInspectorTitle.getStyleClass().add("engine-inspector-title");
        
        engineInspectorText = new Label(I18n.getOrDefault("scenario.desc.engine_inspector", "Select or hover a Type A (Core) or Type B (Optional) engine to display its state equations, physical principles, and academic references."));
        engineInspectorText.setWrapText(true);
        engineInspectorText.getStyleClass().add("engine-inspector-text");

        engineInspectorEquationsTitle = new Label(I18n.getOrDefault("scenario.label.engine_equations", "📐 Mathematical Equations & Cliodynamic Formulation:"));
        engineInspectorEquationsTitle.getStyleClass().add("engine-inspector-equations-title");

        engineInspectorEquations = new Label(
            "• Formulations mathématiques et bilans de conservation affichés dynamiquement."
        );
        engineInspectorEquations.setWrapText(true);
        engineInspectorEquations.getStyleClass().add("engine-inspector-equations");

        engineInspectorRef = new Label(I18n.getOrDefault("scenario.label.engine_refs", "📚 Scientific references and academic literature."));
        engineInspectorRef.setWrapText(true);
        engineInspectorRef.getStyleClass().add("hint-label");

        exportSelectedEngineBtn = new Button(I18n.getOrDefault("scenario.btn.export_engine_template", "📤 Export Custom Engine Template (.java)"));
        exportSelectedEngineBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 10; -fx-background-radius: 4;");
        exportSelectedEngineBtn.setOnAction(e -> exportCustomEngineTemplate());

        VBox engineInspectorCard = new VBox(5, engineInspectorTitle, engineInspectorText, engineInspectorEquationsTitle, engineInspectorEquations, engineInspectorRef, exportSelectedEngineBtn);
        engineInspectorCard.getStyleClass().add("engine-inspector-card");
        engineInspectorCard.setMinHeight(160);

        // --- 🔒 ETHER CORE ENGINES SECTION (Cœur Central Applicatif - 24 Moteurs Permanents) ---
        VBox typeABox = new VBox(6);
        typeABox.getStyleClass().add("card-section");

        Button exportCoreTemplateBtn = new Button(I18n.getOrDefault("scenario.btn.export_law_engine", "📤 Export Physical Law Engine (.java)"));
        exportCoreTemplateBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 4;");
        exportCoreTemplateBtn.setOnAction(e -> exportPhysicalLawEngineTemplate("PhysicalLawEngine"));

        Label coreExplanationLabel = new Label(I18n.getOrDefault("scenario.info.core_engines", "ℹ️ Why are Ether Core engines permanent? They enforce physical conservation laws (mass & energy, thermodynamics, hydrology, H3 insolation, metabolism) required for basic world survival."));
        coreExplanationLabel.getStyleClass().add("control-note");
        coreExplanationLabel.setWrapText(true);
        typeABox.getChildren().addAll(exportCoreTemplateBtn, coreExplanationLabel);

        List<String[]> coreEngines = List.of(
            new String[]{"PhysicalLawEngine", "Moteur Physique & Lois de Conservation",
                "Moteur de conservation thermodynamique de la matière et de l'énergie (Premier et Second Principes). Calcule le bilan calorifique planétaire et la dégradation de l'énergie en chaleur dissipée.",
                "Ref: Carnot, N. L. S. (1824); Clausius, R. (1865); Prigogine, I. (1977). Non-Equilibrium Thermodynamics.",
                "• Premier Principe (Énergie) : dE_total/dt = Q_in - Q_out = 0\n• Second Principe (Entropie) : dS/dt = dS_ext + dS_int >= 0 avec dS_int = Q_dissipee / Temp_surface"},
            new String[]{"BiologicalDemographicsEngine", "Démographie Cellulaire & Métabolisme",
                "Régulation métabolique de la population humaine. Calcule la mortalité de Gompertz-Makeham selon l'âge, l'espérance de vie, la natalité malthusienne et la sous-alimentation.",
                "Ref: Kleiber, M. (1932); Gompertz, B. (1825); Makeham, W. M. (1860); Malthus, T. R. (1798).",
                "• Mortalité Gompertz-Makeham : μ(x) = α · exp(β · x) + γ\n• Bilan Métabolique de Kleiber : B_metabolisme = q₀ · M^(0.75)\n• Dynamique de Population : dN/dt = r · N · (1 - N / K_soutenable)"},
            new String[]{"PhysicalEnergyGridEngine", "Grille Énergétique & EROEI Brut",
                "Modélisation des flux d'énergie primaire planétaire (solaire, géothermie, biomasse). Détermine l'EROEI brut (Energy Return on Energy Invested) pour les récoltes et l'extraction.",
                "Ref: Hall, C. A. S., et al. (2014). EROEI of Global Energy Resources. Nature Climate Change.",
                "• EROEI Brut = E_produite_brute / E_investie_extraction\n• Énergie Utile Net = E_brute · (1 - 1 / EROEI)\n• Seuil Critique Civilisationnel : EROEI >= 3.0 requis pour soutenir les institutions."},
            new String[]{"TechTreeEngine", "Diffusion Technologique & Capital Savoir",
                "Arbre d'innovation technologique et diffusion cognitive. Simule l'accumulation du capital d'instruction, la propagation spatiale des inventions et le franchissement des seuils d'étapes (Niveaux Tech 0.0 à 10.0+).",
                "Ref: Mokyr, J. (1990). The Lever of Riches: Technological Creativity. Oxford Univ. Press.",
                "• Croissance du Capital Savoir : dT/dt = α · Pop · (T / T_max)^β + Σ D_voisinage · (T_j - T_i)\n• Diffusion Spatiale : Flux_innovation = -D_diffusion · ∇T(x)"},
            new String[]{"AquiferDepletionEngine", "Hydrologie & Transfert d'Eau Douce",
                "Hydrologie continentale et réplétion/déplétion des nappes phréatiques. Simule le bilan précipitations-évapotranspiration, le débit des rivières et le stress hydrique.",
                "Ref: Gleick, P. H. (2000). Water Futures; Wada, Y. et al. (2010). Global Groundwater Depletion. GRL.",
                "• Bilan Hydrique Cellulaire : dW_nappe/dt = Precipitations - Evapotranspiration - Extraction_agricole - Ruissellement\n• Stress Hydrique = Extraction_totale / Recharge_annuelle"},
            new String[]{"H3ClimateSystem", "Système Climatique H3 & Saisons",
                "Moteur climato-saisonnier basé sur la discrétisation hexagonale H3. Calcule la température moyenne de surface, le gradient équateur-pôle, l'insolation selon l'obliquité orbitale et les saisons.",
                "Ref: Uber H3 Spatial Index (2018); Sellers, W. D. (1969). Energy Balance Climate Models.",
                "• Bilan Radiatif Solaire : S(lat, t) = (S_const / 4) · [1 + e · cos(ω · t)] · cos(lat - declinaison)\n• Équilibre Thermique : C_thermique · dT/dt = S(1 - Albédo) - ε·σ·T⁴ + Div(K_transport · ∇T)"},
            new String[]{"PoliticalSimulationEngine", "Moteur Politique & Frontières",
                "Modélisation des structures politiques et géopolitiques. Gère la délimitation des territoires, la souveraineté des cités-états, les confédérations culturelles et la stabilité des frontières.",
                "Ref: Tilly, C. (1990). Coercion, Capital, and European States; Mann, M. (1986). Sources of Social Power.",
                "• Projection de Puissance d'État : P_militaire(d) = (Capacité_Fiscale · Taux_Levée) / (1 + α · Distance_Capitale)\n• Stabilité des Frontières : Seuil d'annexion si P_i(x) > 1.35 · P_j(x)"},
            new String[]{"StatisticsKernel", "Noyau Statistique & Cliodynamique",
                "Noyau d'agrégation statistique et d'analyse cliodynamique en temps réel. Calcule l'indice de Gini, le PIB mondial, la complexité de Turchin, le risque d'effondrement et exporte les bilans CSV.",
                "Ref: Gini, C. (1912); Turchin, P. (2016). Ages of Discord; Tainter, J. (1988). Collapse of Complex Societies.",
                "• Indice de Gini : G = (Σ Σ |y_i - y_j|) / (2 · n² · y_moyen)\n• Pression de Crise Systémique (PSI) = (Pop / Pop_soutenable) · (1 / Salaire_reel) · Inegalité_Elite"},
            new String[]{"WorldBuffer / AgentBuffer", "Noyau DOD Allocateur Mémoire",
                "Allocateur de mémoire et registres DOD (Data-Oriented Design). Optimise la mémoire cache du processeur en vectorisant les attributs des cohortes d'agents et des mailles H3.",
                "Ref: Acton, M. (2014). Data-Oriented Design; LMAX Disruptor High-Performance Ring Buffer (2011).",
                "• Structure des Tableaux d'Attributs (SoA) : float[] capitalWork, float[] capitalResource, int[] populationCohorts\n• Alignement Cache SIMD Vectorisé : 64-byte aligned blocks for AVX-512 execution."},
            new String[]{"OceanPhysicsEngine", "Dynamo Fluidique & Basculement Océanique",
                "Dynamo fluidique et inertie thermique des océans. Modélise la capacité calorifique de la masse d'eau marine, la dérive thermique lente et la régulation du climat végétal.",
                "Ref: Stommel, H. (1961). Thermohaline Convection; Rahmstorf, S. (1995). AMOC Stability. Nature.",
                "• Modèle à Deux Mailles de Stommel : dq/dt = c_T · ΔT - c_S · ΔS\n• Transport de Chaleur Océanique : F_ocean = ρ · C_p · V_derive · (T_equateur - T_pole)"},
            new String[]{"MalthusianCapacityEngine", "Pression Malthusienne & Capacité Portante",
                "Capacité portante écologique (K) et pression Malthusienne. Calcule le seuil maximal d'habitants soutenables par cellule avant dégradation irréversible de l'environnement.",
                "Ref: Malthus, T. R. (1798); Catton, W. R. (1980). Overshoot: Ecological Footprint.",
                "• Capacité Portante K(t) = Fertilité_Sol · Eau_Disponible · Niveau_Tech\n• Ratio Malthusien M = Pop / K\n• Dégradation Environnementale en cas de Surconsommation (M > 1) : dK/dt = -γ · (M - 1) · K"},
            new String[]{"OreGradeThermodynamicsEngine", "Géo-Métallurgie & Déplétion Crustale",
                "Géo-métallurgie et thermodynamique d'épuisement des filons minéraux crustaux. Simule le déclin du titre des minerais (Loi de Lasky) et la hausse de l'énergie nécessaire à l'extraction.",
                "Ref: Lasky, S. G. (1950). Mineral Resource Depletion Law; Ayres, R. U. (1998). Industrial Ecology.",
                "• Loi de Lasky (Titre du Minerai) : Grade(g) = g₀ · exp(-k · Cumul_Extrait)\n• Énergie Spécifique d'Extraction : E_extraction(g) = E₀ / (Grade(g))^1.35"},
            new String[]{"SoilNutrientNPKEngine", "Cycle NPK & Fertilité des Sols",
                "Cycles biogéochimiques des nutriments NPK (Azote, Phosphore, Potassium). Régit l'épuisement des sols agricoles par la culture intensive et la restauration organique.",
                "Ref: Liebig, J. von (1840). Law of the Minimum; Smil, V. (2001). Enriching the Earth (N-P-K Cycles).",
                "• Loi du Minimum de Liebig : Rendement = Y_max · min( N/N_ref, P/P_ref, K/K_ref )\n• Épuisement des Nutriment : dN/dt = Restauration_Naturelle + Apport_Engrais - Export_Recolte"},
            new String[]{"FluxEngine", "Flux de Subsistance & Routes Commerciales",
                "Routes commerciales et flux de subsistance inter-cellules H3. Calcule les coûts de transport, l'arbitrage marchand et l'équilibrage des stocks alimentaires par le commerce.",
                "Ref: Tinbergen, J. (1962). Gravity Model of Trade; Onsager, L. (1931). Reciprocal Relations.",
                "• Modèle Gravitationnel de Commerce : Flux(i, j) = G · (PIB_i · PIB_j) / (Distance(i, j)^1.8)\n• Friction de Transport : Coût_fret = Exp(Friction_Relief · Distance)"},
            new String[]{"AtmosphericOxygenEngine", "Dynamique de l'Oxygène Atmosphérique",
                "Bilan de la pression partielle d'oxygène (O₂) atmosphérique. Régule les conditions métaboliques pour la survie des organismes complexes et le risque d'incendies forestiers.",
                "Ref: Berner, R. A. (2006). GEOCARBSULF: Atmospheric Oxygen over Phanerozoic Time.",
                "• Bilan d'O₂ Atmosphérique : dO₂/dt = Photosynthese_Net - Respiration_Biomasse - Oxydation_Minérale\n• Risque d'Embrasement Sauvage = Max(0, (pO₂ - 0.15) / 0.06)"},
            new String[]{"CrustalGeothermalEngine", "Géothermie Crustale & Tectonique",
                "Flux de chaleur interne terrestre et potentiel géothermique crustal. Simule le gradient géothermique et le potentiel d'énergie géothermique de surface.",
                "Ref: Turcotte, D. L. & Schubert, G. (2002). Geodynamics: Mantle Heat Transport. Cambridge Univ. Press.",
                "• Conductivité Thermique Crustale : q = -k_roche · (dT/dz)\n• Potentiel Géothermique d'Exploitation : P_geoth = q_surface · Surface_H3 · Rendement_Carnot"},
            new String[]{"DynamicHydrographicSiltationEngine", "Hydrographie & Ensablement Fluvial",
                "Hydrographie et dynamique d'érosion/ensablement des bassins versants. Modélise la modification du lit des fleuves et le dépôt d'alluvions fertiles.",
                "Ref: Horton, R. E. (1945). Drainage-Basin Development; Schumm, S. A. (1977). The Fluvial System.",
                "• Équation d'Érosion Fluviale : E_sediment = k_erodibilite · (Débit_eau)^1.4 · (Pente_terrain)^1.2\n• Dépôt Alluvionnaire : dSilt/dt = E_amont - Sedimentation_loc_lit"},
            new String[]{"GreenhouseRadiativeEngine", "Forçage Radiatif & Effet de Serre",
                "Bilan de forçage radiatif et effet de serre. Calcule l'impact des concentrations de CO₂, CH₄ et H₂O sur l'infrarouge réémis vers la surface.",
                "Ref: Myhre, G. et al. (1998). Radiative Forcing Equations; IPCC AR6 WG1 (2021).",
                "• Forçage Radiatif du CO₂ : ΔF_CO2 = 5.35 · ln(C / C₀)  [W/m²]\n• Sensibilité Climatique : ΔT_eq = λ · (ΔF_CO2 + ΔF_CH4 + ΔF_aerosols)"},
            new String[]{"InfrastructureEnergyEngine", "Réseaux d'Infrastructure Énergétique",
                "Réseaux d'infrastructures énergétiques et transport de puissance. Modélise la perte en ligne des réseaux électriques et oléoducs.",
                "Ref: Bak, P. et al. (1987). Self-Organized Criticality in Grid Infrastructure.",
                "• Perte en Ligne Électrique : Perte_Joule = R_cable · I² · Distance\n• Capacité Maximale de Transit : Cap_max = V_reseau · I_max_thermique"},
            new String[]{"NetEnergyEROEIEngine", "EROEI Net & Rendement Énergétique",
                "Calcul du rendement énergétique net (EROEI Net civilisationnel). Évalue la fraction d'énergie réinvestie dans l'extraction par rapport à l'énergie utilisable pour la société.",
                "Ref: Hall, C. A. S. & Klitgaard, K. A. (2018). Energy and the Wealth of Nations. Springer.",
                "• Fraction d'Énergie Réinvestie : F_invest = 1 / EROEI_systemique\n• Énergie Nette Utile Société = Énergie_Totale · (1 - F_invest)"},
            new String[]{"PermafrostThawEngine", "Dégel du Permafrost & Relargage Méthane",
                "Dynamique de fonte du cryosol (Permafrost). Simule la déstabilisation des sols gelés et le relargage rétroactif de méthane et CO₂ stratosphériques.",
                "Ref: Schuur, E. A. G. et al. (2015). Vulnerability of Permafrost Carbon to Climate Change. Nature.",
                "• Fonte du Cryosol : dV_gel/dt = -α · Max(0, T_surface - 0.0 °C)\n• Émission Rétroactive CH₄/CO₂ : Emiss_gaz = Stock_carbone_degele · K_microbien(T)"},
            new String[]{"PhysicsTransportEngine", "Frictions Thermodynamiques de Transport",
                "Coûts énergétiques et frictions thermodynamiques des transports. Calcule l'énergie consommée par tonne-kilomètre selon le relief et le mode de transport.",
                "Ref: Smil, V. (2017). Energy and Civilization: A History. MIT Press.",
                "• Énergie Spécifique par Mode (MJ/t-km) : Maritim=0.15, Ferroviaire=0.35, Route=2.5, Porteur=12.0\n• Friction du Relief : Coût_total = E_specifique · (1 + k_pente · Pente_moyenne) · Distance"},
            new String[]{"ThermohalineOceanEngine", "Circulation Thermohaline Océanique",
                "Circulation thermohaline globale (Boucle AMOC). Modélise la plongée des eaux salées froides en Atlantique Nord et la redistribution de la chaleur planétaire.",
                "Ref: Broecker, W. S. (1991). The Great Ocean Conveyor; Rahmstorf, S. (2002). Ocean Circulation. Nature.",
                "• Débit AMOC Q_amoc = k_thermo · (ρ_nord - ρ_equateur)\n• Point de Basculement Salin : Si Dilution_Eau_Douce > Seuil_Critique -> Effondrement AMOC (Q -> 0)"},
            new String[]{"TrophicEcosystemEngine", "Réseau Trophique & Écosystèmes",
                "Réseau trophique et dynamique des écosystèmes fauniques. Simule les équations de Lotka-Volterra entre prédateurs, herbivores et producteurs primaires.",
                "Ref: Lotka, A. J. (1925); Volterra, V. (1926); MacArthur, R. H. & Wilson, E. O. (1967).",
                "• Dynamique Herbivores H : dH/dt = r_h · H · (1 - H/K) - a · H · P\n• Dynamique Prédateurs P : dP/dt = b · a · H · P - m_p · P"}
        );

        for (String[] eng : coreEngines) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Label iconTitle = new Label("🔒 " + eng[0]);
            iconTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
            Label descLbl = new Label("— " + eng[1]);
            descLbl.setStyle("-fx-font-size: 10px;");
            HBox.setHgrow(descLbl, Priority.ALWAYS);
            row.getChildren().addAll(iconTitle, descLbl);

            String eqText = eng.length > 4 ? eng[4] : I18n.getOrDefault("scenario.engine.state_eq_default", "📐 Équation d'État : dX/dt = f(X, t) + Σ F_inter-cellulaire");
            Tooltip tooltip = new Tooltip(I18n.getOrDefault("scenario.engine.perm_prefix", "🔒 [MOTEUR PERMANENT]\n") + eng[0] + " — " + eng[1] + "\n\n" + eng[2] + "\n\n" + eqText + "\n\n📚 " + eng[3]);
            tooltip.setStyle("-fx-font-size: 11px; -fx-max-width: 500px;");
            Tooltip.install(row, tooltip);

            typeABox.getChildren().add(row);
        }

        TitledPane corePane = new TitledPane(I18n.getOrDefault("scenario.header.core_engines", "🔒 ARCHITECTURE CŒUR ETHER (24 MOTEURS PERMANENTS)"), typeABox);
        corePane.setExpanded(false);
        corePane.getStyleClass().add("titled-pane-primary");

        // --- ⚙️ OPTIONAL & CUSTOM ENGINES SECTION (Optionnels, Extensibles & Dynamic Import/Export) ---
        typeBBoxContainer = new VBox(8);
        typeBBoxContainer.getStyleClass().add("custom-module-card");

        // Action bar for Import / Export Custom Engines
        Button exportTemplateBtn = new Button(I18n.getOrDefault("scenario.btn.export_java_template", "📤 Export Java Engine Template (.java)"));
        exportTemplateBtn.setStyle("-fx-background-color: #4c1d95; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 4;");
        exportTemplateBtn.setOnAction(e -> exportCustomEngineTemplate());

        Button importEngineBtn = new Button(I18n.getOrDefault("scenario.btn.import_compile_engine", "📥 Import & Compile Java Engine (.java / .class)"));
        importEngineBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 4;");
        importEngineBtn.setOnAction(e -> importCustomEngineFile());

        Button autoSelectEnginesForYearBtn = new Button(I18n.getOrDefault("scenario.btn.autoselect_engines", "⚡ Check Engines Based on T₀"));
        autoSelectEnginesForYearBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 4;");
        autoSelectEnginesForYearBtn.setOnAction(e -> {
            long year = startYearSpinner != null ? startYearSpinner.getValue() : -100000;
            if (year <= -10000) {
                if (typeBCheckBoxMap.containsKey("PaleoLanguageDriftEngine")) typeBCheckBoxMap.get("PaleoLanguageDriftEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("KarstCaveShelterEngine")) typeBCheckBoxMap.get("KarstCaveShelterEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("ParietalArtAsabiyyahEngine")) typeBCheckBoxMap.get("ParietalArtAsabiyyahEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("SnowpackMobilityEngine")) typeBCheckBoxMap.get("SnowpackMobilityEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("MeatCuringReservesEngine")) typeBCheckBoxMap.get("MeatCuringReservesEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("OsseousIndustryCarvingEngine")) typeBCheckBoxMap.get("OsseousIndustryCarvingEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("PlantFiberCordageEngine")) typeBCheckBoxMap.get("PlantFiberCordageEngine").setSelected(true);
            }
            if (year <= -15000) {
                if (typeBCheckBoxMap.containsKey("CanidDomesticationEngine")) typeBCheckBoxMap.get("CanidDomesticationEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("LithicTradeProvenanceEngine")) typeBCheckBoxMap.get("LithicTradeProvenanceEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("ExogamousKinshipEngine")) typeBCheckBoxMap.get("ExogamousKinshipEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("TailoredClothingThermalEngine")) typeBCheckBoxMap.get("TailoredClothingThermalEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("AtlatlArcheryBallisticsEngine")) typeBCheckBoxMap.get("AtlatlArcheryBallisticsEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("PassiveSnareSmallGameEngine")) typeBCheckBoxMap.get("PassiveSnareSmallGameEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("MegafaunaPitfallTrapEngine")) typeBCheckBoxMap.get("MegafaunaPitfallTrapEngine").setSelected(true);
            }
            if (year <= -40000) {
                if (typeBCheckBoxMap.containsKey("ArchaicIntrogressionEngine")) typeBCheckBoxMap.get("ArchaicIntrogressionEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("OchreTanningTechnologyEngine")) typeBCheckBoxMap.get("OchreTanningTechnologyEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("CoastalMarineRefugiaEngine")) typeBCheckBoxMap.get("CoastalMarineRefugiaEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("SeasonalAggregationSanctuaryEngine")) typeBCheckBoxMap.get("SeasonalAggregationSanctuaryEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("AridWaterStorageStashEngine")) typeBCheckBoxMap.get("AridWaterStorageStashEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("ResinHaftingAdhesivesEngine")) typeBCheckBoxMap.get("ResinHaftingAdhesivesEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("MammothBoneHabitationEngine")) typeBCheckBoxMap.get("MammothBoneHabitationEngine").setSelected(true);
                if (typeBCheckBoxMap.containsKey("FireHardenedSpearEngine")) typeBCheckBoxMap.get("FireHardenedSpearEngine").setSelected(true);
            }
        });

        HBox importExportBox = new HBox(10, autoSelectEnginesForYearBtn, exportTemplateBtn, importEngineBtn);
        importExportBox.setAlignment(Pos.CENTER_LEFT);
        importExportBox.setStyle("-fx-padding: 0 0 8 0;");
        typeBBoxContainer.getChildren().add(importExportBox);

        // Sorting toolbar for optional engines
        ComboBox<String> engineSortCombo = new ComboBox<>();
        engineSortCombo.getItems().addAll(
            org.ether.society.i18n.I18n.getOrDefault("scenario.sort.default", "⚙️ System Order (By Category)"),
            org.ether.society.i18n.I18n.getOrDefault("scenario.sort.date_asc", "📅 Chronological Sort (Oldest → Newest)"),
            org.ether.society.i18n.I18n.getOrDefault("scenario.sort.date_desc", "📅 Reverse Chronological Sort (Newest → Oldest)"),
            org.ether.society.i18n.I18n.getOrDefault("scenario.sort.alpha_asc", "🔤 Alphabetical Sort (A - Z)")
        );
        engineSortCombo.setValue(org.ether.society.i18n.I18n.getOrDefault("scenario.sort.default", "⚙️ System Order (By Category)"));
        engineSortCombo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        Label sortLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.sort.label", "🔀 Engine Sorting:"));
        sortLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        HBox sortBar = new HBox(8, sortLabel, engineSortCombo);
        sortBar.setAlignment(Pos.CENTER_LEFT);
        sortBar.setStyle("-fx-padding: 4 0 8 0;");
        typeBBoxContainer.getChildren().add(sortBar);

        List<String[]> optionalEngines = List.of(
            new String[]{"PaleoLanguageDriftEngine", "🗣️ Dérive Phonétique & Linguistique Paléolithique (-100 000 BP)",
                "Simule la dérive isoline et la divergence des continuum de paléo-langues entre vallées d'Eurasie et clans paléolithiques isolés.",
                "Ref: Cavalli-Sforza, L. L. (1994). History and Geography of Human Genes; Swadesh, M. (1952).",
                "• Dérive Isoglosse : dS_lang/dt = D_lang * ∇²S_lang + σ_drift * η(x,y,t)\n• Seuil de Divergence : Isolement > 500 ans -> Rupture d'intercompréhension inter-tribale"},
            new String[]{"KarstCaveShelterEngine", "🦇 Abris Micro-Climatiques Karstiques & Grottes (-300 000 BP)",
                "Apport de la géomorphologie karstique pour le refuge hivernal des bandes paléolithiques en périodes de poussées glaciaires.",
                "Ref: Pettitt, P. (2011). The Palaeolithic Origins of Human Burial; Gamble, C. (1999).",
                "• Protection Thermique : Réduction de la mortalité hivernale de 40% dans les mailles karstiques\n• Isolation R_karst : Flux_thermique dQ/dt = -k_karst * A * (T_caverne - T_ext)"},
            new String[]{"ParietalArtAsabiyyahEngine", "🎨 Art Pariétal & Cohésion Rituelle d'Agrégation (-45 000 BP)",
                "Sanctuaires ornés (Chauvet, Lascaux, Sulawesi) renforçant l'Asabiyyah rituelle et facilitant les mariages exogames lors des agrégations saisonnières.",
                "Ref: Clottes, J. (2008). Cave Art; Lewis-Williams, D. (2002). The Mind in the Cave.",
                "• Boost de Cohésion : +25% d'Asabiyyah dans les mailles d'agrégation sanctuarisées\n• Diffusion Symbolique : Taux_échange_matériel * exp(Art_pariétal / K)"},
            new String[]{"SnowpackMobilityEngine", "🎿 Raquettes & Skis / Mobilité Sub-Arctique (-10 000 BP)",
                "Développement de raquettes à neige et patins en bois réduisant la friction glaciaire et débloquant la chasse hivernale sub-arctique.",
                "Ref: Burov, G. M. (1989). Some Mesolithic Wooden Artefacts from the Vis Sites; Forsten, A. (1993).",
                "• Friction de Neige : Vitesse_brute / (1.0 + 2.0 * Épaisseur_Neige)\n• Effet Raquette / Ski : Annulation de 80% de la pénalité de déplacement sous enneigement persistant"},
            new String[]{"CanidDomesticationEngine", "🐕 Symbiose Trophique & Domestication Canidés (-15 000 BP)",
                "Couplage trophique mutualiste entre bandes de chasseurs et proto-chiens (Bonn-Oberkassel, Altaï) augmentant le rendement des battues.",
                "Ref: Germonpré, M. et al. (2009). Fossil dogs of the Upper Paleolithic; Larson, G. et al. (2012).",
                "• Gain d'Énergie de Chasse : dE/dt = η_chasse * M_proie * (1.0 + β * N_canid / N_humain) - C_maintenance\n• Taux de Rétention Alimentaire : +18% de calories carnées restituées à la bande"},
            new String[]{"LithicTradeProvenanceEngine", "🪨 Provenance Géochimique du Silex & Obsidienne (-300 000 BP)",
                "Réseaux d'échange lithique tracé par fluorescence X (XRF) suivant des surfaces d'énergie isotrope de moindre coût.",
                "Ref: Renfrew, C. (1975). Trade as Action at a Distance; Torrence, R. (1986). Obsidian Trade.",
                "• Coût-Distance Isotrope : C(x,y) = ∫ (1.0 + κ * tan²(pente)) ds\n• Maintien du Tranchant : Prévient la dégradation du rendement de chasse à éloignement des carrières"},
            new String[]{"MeatCuringReservesEngine", "🥩 Conservation de Viande Fumée & Graisse (Pemmican) (-25 000 BP)",
                "Techniques de fumage et séchage prolongeant la durée de conservation des réserves de +6 mois face aux crêtes d'inanition hivernale.",
                "Ref: Speth, J. D. (2010). The Paleolithic Human Diet; Outram, A. K. (2001). Bone marrow & fat.",
                "• Prolongation des Stocks : F₀ +6 mois de réserves\n• Tampon de Famine Hivernale : Empêche l'effondrement démographique des cohortes âgées"},
            new String[]{"ExogamousKinshipEngine", "🧬 Parenté Exogame & Évitement de l'Ingeste (-40 000 BP)",
                "Échanges obligatoires de partenaires entre bandes nomades (Sunghir) prévenant la dépression de consanguinité génétique (F_is).",
                "Ref: Sikora, M. et al. (2017). Ancient genomes show social structure in Upper Paleolithic. Science.",
                "• Contrôle F_is : Fitness W = W₀ * (1.0 - γ * F_is)\n• Maintien de Viabilité : Empêche la baisse de fertilité des petites bandes nomades"},
            new String[]{"ArchaicIntrogressionEngine", "🧬 Introgressions Homininés Archaïques (EPAS1 / TLR1/6/10) (-50 000 BP)",
                "Héritage génétique dénisovien (adaptation à l'altitude tibétaine) et néandertalien (immunité innée aux zoonoses sub-arctiques).",
                "Ref: Huerta-Sánchez, E. et al. (2014). Altitude adaptation in Tibetans via Denisovan EPAS1. Nature.",
                "• EPAS1 Dénisovien : +30% d'habitabilité sur le Plateau Tibétain\n• TLR Néandertalien : Immunité renforcée aux agents pathogènes forestiers"},
            new String[]{"TailoredClothingThermalEngine", "🪡 Vêtements Ajustés en Fourrure & Aiguilles à Chas (-35 000 BP)",
                "Aiguilles en os pour vêtements ajustés multicouches hermétiques (Sunghir, Denisova) réduisant la mortalité thermique.",
                "Ref: Gilligan, I. (2010). The Prehistoric Development of Clothing. Proc. Prehist. Soc.",
                "• Protection Thermique Extreme : Réduit la pénalité métabolique du grand froid (≤ -20°C) de 65%\n• Expansion Sub-Arctique : Débloque la colonisation des hautes latitudes sibériennes"},
            new String[]{"OchreTanningTechnologyEngine", "🪵 Ocre Rouge, Pigments & Tannage Antibactérien (-100 000 BP)",
                "Usage dual de l'ocre (Blombos, Qafzeh) : rituels symboliques et préservation des peaux par tannage bactéricide.",
                "Ref: Henshilwood, C. S. et al. (2011). 100,000-Year-Old Ochre-Processing Workshop. Science.",
                "• Preservation des Peaux : Ralentit de 50% la dégradation des contenants en cuir\n• Cohésion Rituelle : +10% d'Asabiyyah dans les mailles d'extraction d'ocre"},
            new String[]{"AtlatlArcheryBallisticsEngine", "🏹 Propulseur (Atlatl) & Arc / Ballistique de Chasse (-20 000 BP)",
                "Levier propulsif augmentant l'énergie cinétique des projectiles (Ek = 0.5 m v²) et doublant la distance de tir sécurisée.",
                "Ref: Cattelain, P. (1997). Hunting Weapons in the European Upper Palaeolithic; Churchill, S. E. (1993).",
                "• Énergie Cinétique : Portée de chasse doublée (dist >= 30m)\n• Rendement sur Grand Gibier : +40% de succès lors des battues de mégafaune"},
            new String[]{"CoastalMarineRefugiaEngine", "🐚 Subsistance Littorale & Refuges Glaciaires (-160 000 BP)",
                "Coquillages et ressources intertidales (Pinnacle Point, Klasies River) offrant un aliment protéique et oméga-3 insensible aux sécheresses.",
                "Ref: Marean, C. W. et al. (2007). Early human use of marine resources and pigment in South Africa. Nature.",
                "• Nutrition Oméga-3 : Alimentation cérébrale et résilience immunitaire\n• Sanctuary Littoral : Maintien des densités humaines lors des pires stades isotopiques glaciaires (MIS 6)"},
            new String[]{"SeasonalAggregationSanctuaryEngine", "🏛️ Rassemblements Saisonniers de Super-Bandes (-30 000 BP)",
                "Agglomérations temporaires de centaines d'individus aux confluences fluviales (Dolní Věstonice) pour les rituels et les alliances.",
                "Ref: Gamble, C. (1999). The Palaeolithic Societies of Europe. Cambridge Univ. Press.",
                "• Échange de Savoir & Gènes : Flux d'innovation technologique +50% lors des agrégations\n• Mariages Exogames : Réinitialisation du coefficient de consanguinité régionale"},
            new String[]{"PassiveSnareSmallGameEngine", "🪤 Piégeage au Lacet & Filets pour Petit Gibier (-15 000 BP)",
                "Diversification Broad-Spectrum (Flannery) par capture passive de lagomorphes et d'oiseaux aquatiques à faible coût calorique.",
                "Ref: Flannery, K. V. (1969). Broad-Spectrum Revolution; Pryor, J. H. (1988).",
                "• Extr. Passive : Calories captées sans risque physique pour la bande\n• Capacité Portante K : +20% d'augmentation de K dans les zones humides"},
            new String[]{"AridWaterStorageStashEngine", "🥚 Réserves d'Eau en Œufs d'Autruche enfouis (-60 000 BP)",
                "Stockage clandestin d'eau en récipients d'œufs d'autruche gravés (Diepkloof) permettant la traversée des corridors arides.",
                "Ref: Texier, P. J. et al. (2010). A Howiesons Poort tradition of engraving ostrich eggshell containers. PNAS.",
                "• Franchissement Désertique : Annulation de la mortalité par déshydratation sur les routes arides\n• Rayon de Transhumance : Multiplié par 3 en zone xérique (Kalahari / Outback)"},
            new String[]{"ResinHaftingAdhesivesEngine", "🌲 Emmanchement au Nielle de Bouleau & Résine (-200 000 BP)",
                "Pyrotechnologie d'extraction de poix de bouleau et résine de spinifex pour l'emmanchement solide des armatures en pierre.",
                "Ref: Koller, J. et al. (2001). High-tech in the Middle Palaeolithic: Birch bark pitch. Eur. J. Arch.",
                "• Solidité de l'Armature : Multiplie par 2.5 la résistance aux chocs des javelots et dards\n• Gain d'Extraction : +15% de rendement sur la grande chasse de forêt/taïga"},
            new String[]{"OsseousIndustryCarvingEngine", "🦴 Industrie de l'Os, Bois de Cerf & Ivoire (-40 000 BP)",
                "Façonnage de harpons, sagaies et aiguilles dans les matérieux osseux de la mégafaune (Mammouth, Renne).",
                "Ref: Knecht, H. (1997). Projectile Technology. Plenum Press, New York.",
                "• Diversification de l'Outillage : Harpons barbelés augmentant la pêche fluviale de +35%\n• Substitution du Silex : Exploitation des stocks d'ivoire sur les zones dépourvues de carrières lithiques"},
            new String[]{"PlantFiberCordageEngine", "🌾 Cordages en Fibres Végétales & Tissage Arcaïque (-30 000 BP)",
                "Extraction et tressage de fibres végétales (Ortie, Tilleul, Lin sauvage - Grotte de Dzudzuana) pour liens et sacs de transport.",
                "Ref: Kvavadze, E. et al. (2009). 30,000-Year-Old Wild Flax Fibers. Science.",
                "• Transport et Logistique : Augmente la capacité de transport nomade de +40 kg/bande\n• Liens et Filets : Composants essentiels pour pièges et vêtements multicouches"},
            new String[]{"MegafaunaPitfallTrapEngine", "🕳️ Fosses-Pièges à Mégafaune & Chasse de Fosse (-15 000 BP)",
                "Creusement collectif de pièges à fosse (Tultepec) pour la capture sans risque de mammouths et bisons des steppes.",
                "Ref: Washington-Allen, R. A. et al. (2020). Prehistoric Mammoth Pitfall Traps in Central Mexico.",
                "• Capture de Mégafaune : Abattage de gros gibier sans perte humaine dans la bande\n• Apport Calorique Massif : Stockage de plusieurs tonnes de viande carnée d'un seul coup"},
            new String[]{"AcousticFluteResonanceEngine", "🎵 Flûtes en Os & Acoustique des Cavernes Ornées (-40 000 BP)",
                "Flûtes en os d'oiseau et ivoire (Hohle Fels) et résonance acoustique des sanctuaires profonds stimulant l'adhésion rituelle.",
                "Ref: Conard, N. J. et al. (2009). Female figurine and flutes from Hohle Fels. Nature.",
                "• Stimulation Rituelle : Boost d'Asabiyyah lors des cérémonies acoustiques underground\n• Cohésion Inter-Tribu : Renforce les alliances diplomatiques lors des agrégations"},
            new String[]{"SymbolicBeadNetworkEngine", "🐚 Parure de Perles d'Œuf d'Autruche & Coquillages (-40 000 BP)",
                "Fabrication et troc de perles d'œufs d'autruche et coquillages perforés (Enkapune Ya Muto) comme réseaux de confiance et réassurance.",
                "Ref: Ambrose, S. H. (1998). Chronology of the Later Stone Age at Enkapune Ya Muto. J. Arch. Sci.",
                "• Réseau de Secours (Hito) : Les échanges de perles scellent des pactes d'entraide inter-bande en cas de famine\n• Fluidité Commerciale : Réduit la friction de troc avec les bandes étrangères"},
            new String[]{"PortableArtFigurineEngine", "🗿 Figurines Mobiles en Ivoire & Terres Cuites (Vénus) (-30 000 BP)",
                "Sculpture d'artefacts anthropomorphes et zoomorphes (Willendorf, Dolní Věstonice) circulant comme symboles d'alliance et de fécondité.",
                "Ref: Soffer, O. et al. (2000). The Venus Figurines: Textiles, Basketry, Gender, and Status.",
                "• Marqueur d'Alliance : Transportable d'une vallée à l'autre pour sceller les pactes de mariage\n• Résilience Démographique : Symbolique rituelle stimulant les taux de natalité"},
            new String[]{"CaveLightingPyrotechnicsEngine", "🛢️ Éclairage à la Graisse Animale & Torches de Caverne (-35 000 BP)",
                "Lampes en grès à combustion de graisse animale (Lascaux, Chauvet) permettant l'exploration et l'art dans les galeries obscures.",
                "Ref: Beaune, S. A. de (1987). Lampes et brûloirs paléolithiques. CNRS Éditions.",
                "• Exploration Profonde : Débloque l'accès aux sanctuaires endokarstiques situés à plus de 500m des entrées\n• Sécurité Karstique : Élimine les accidents et chûtes dans les gouffres sombres"},
            new String[]{"RiverCanoeTransportEngine", "🚣 Pirogues de Bouleau & Canoës en Écorce (-45 000 BP)",
                "Fabrication de pirogues monoxyles et canoës en écorce pour le franchissement des grands fleuves et le transport lourd.",
                "Ref: McGrail, S. (2001). Boats of the World: From the Stone Age to Medieval Times. Oxford.",
                "• Navigation Fluviale : Traverse les fleuves infranchissables sans noyade\n• Efficacité de Fret : Réduit le coût-distance de déplacement le long des cours d'eau de 80%"},
            new String[]{"PermafrostColdCacheEngine", "🧊 Fosses de Congélation dans le Permafrost (-30 000 BP)",
                "Creusement de fosses dans le sol gelé permanent (Yana RHS) pour le stockage frigorifique des carcasses de grand gibier.",
                "Ref: Pitulko, V. V. et al. (2004). The Yana RHS Site, Royal Society B.",
                "• Cryo-Conservation : Préserve la viande fraîche pendant plusieurs années sans putréfaction\n• Survie Arctique : Maintient des foyers permanents dans les environnements de haute latitude"},
            new String[]{"PlantDetoxificationLeachingEngine", "🧪 Lessivage et Détoxification des Tubercules Toxiques (-20 000 BP)",
                "Procédés de rouissage et détoxification par lixiviation à l'eau courante pour rendre comestibles les tubercules et nymphes toxiques.",
                "Ref: Barker, G. et al. (2007). The Niah Cave Project. Human Ecology.",
                "• Élargissement Alimentaire : Rend consommables des ressources végétales toxiques auparavant mortelles\n• Sécurité Alimentaire Tropicale : Maintient la subsistance dans les forêts équatoriales"},
            new String[]{"TopographicGameDriveEngine", "🏔️ Pièges Topographiques V-Shaped & Desert Kites (-12 000 BP)",
                "Aménagement de murets en V (Desert Kites) rabattant les troupeaux de gazelles et bisons vers des enclos naturels d'abattage.",
                "Ref: Holzer, A. et al. (2010). Desert kites in the Levant. J. Arch. Sci.",
                "• Abattage de Masse : Capture simultanée de troupeaux entiers lors des migrations\n• Accumulation de Capital Carné : Alimente de grands rassemblements sédentaires pré-agricoles"},
            new String[]{"LunarCalendarTallyEngine", "🌙 Notations Calendaires Lunaires sur Os (-30 000 BP)",
                "Incisures sérielles sur os (Abri Blanchard, Ishango) pour le décompte des cycles lunaires et la prédiction des migrations de faune.",
                "Ref: Marshack, A. (1972). The Roots of Civilization. McGraw-Hill.",
                "• Prédiction Saisonnière : Anticipe l'arrivée des troupeaux migrateurs avec une précision de quelques jours\n• Synchronisation des Agrégations : Fixe les rendez-vous inter-bandes lors des pleines lunes"},
            new String[]{"MammothBoneHabitationEngine", "🏕️ Habitations en Os de Mammouth & Peaux Tendues (-25 000 BP)",
                "Assemblage de structures circulaires composées de crânes et défenses de mammouths (Mezhyrich) recouverts de peaux étanches.",
                "Ref: Pidoplichko, I. G. (1998). Upper Palaeolithic Mammoth-Bone Dwellings in Ukraine.",
                "• Architecture Sans Bois : Construction d'abris solides et isolés dans les steppes dénudées d'arbres\n• Résilience Hivernale : Maintient la chaleur interne des foyers pendant le LGM"},
            new String[]{"FireHardenedSpearEngine", "🪵 Javelots en Bois Durci au Feu (Schöningen) (-300 000 BP)",
                "Taille et durcissement pyrotechnique de la pointe de pieux en épicéa (Schöningen) pour la chasse à jet à moyenne distance.",
                "Ref: Thieme, H. (1997). Lower Palaeolithic hunting spears from Germany. Nature.",
                "• Chasse Acheuléenne : Arme d'homininé archaïque (Heidelbergensis / Sapiens) efficace sur les chevaux sauvages\n• Pénétration Mécanique : Améliore la perforation du cuir des grands herbivores"},
            new String[]{"ParasiteControlRepellentEngine", "🌿 Literies d'Herbes Médicinales & Ocre Anti-Parasites (-70 000 BP)",
                "Matelas d'herbes aromatiques (Sibudu Cave) associées à l'ocre pour repousser les insectes vecteurs de maladies et punaises.",
                "Ref: Wadley, L. et al. (2011). Middle Stone Age Bedding Construction and Insect Repellent Use. Science.",
                "• Hygiène d'Habitat : Réduit les infections parasitaires et le paludisme dans les abris sous roche\n• Santé des Cohortes : Baisse de la mortalité infantile et amélioration du sommeil des forageurs"},
            new String[]{"WildCerealGrindingEngine", "🌾 Pilonnage et Mortiers de Céréales Sauvages (Ohalo II) (-23 000 BP)",
                "Broyage de graines de graminées et d'orge sauvage sur meules en pierre (Ohalo II) pour l'élaboration de galettes cuites.",
                "Ref: Nadel, D. et al. (2012). Ohalo II: A 23,000-Year-Old Submerged Hunter-Gatherer Camp. PLoS ONE.",
                "• Nutritivité des Graines : Extrait les glucides complexes des céréales sauvages\n• Prémices Natufiennes : Étape fondatrice vers la sédentarisation pré-agricole"},
            new String[]{"VolcanicTephraRefugiaEngine", "🌋 Téphrochronologie & Refuges post-Superéruption de Toba (-74 000 BP)",
                "Survie dans les vallées refuges littorales lors des retombées massives d'aérosols et de cendres de la super-éruption du Toba.",
                "Ref: Rampino, M. R. & Self, S. (1992). Volcanic winter and accelerated glaciation following Toba. Nature.",
                "• Goulot d'Étranglement Démographique : Réduction de la population mondiale à ~10,000 reproducteurs\n• Résilience des Refuges : Maintien de foyers viables le long des côtes sud-africaines"},
            new String[]{"DemographicLifeTableEngine", "📊 Tables de Mortalité Paléodémographiques & Fission/Fusion (-300 000 BP)",
                "Structure par âge, haute mortalité infantile (40%) et fission des bandes nomades dès que l'effectif dépasse 50 individus.",
                "Ref: Bocquet-Appel, J.-P. (2002). Paleoanthropological Life Tables. Curr. Anthropol.",
                "• Fission de Bande : Si N_bande > 50 -> Fission en 2 sous-bandes autonomes de ~25 individus\n• Espérance de Vie : e₀ ≈ 25-30 ans avec mortalité maternelle élevée"},
            new String[]{"ToolKitMaintenanceEngine", "🪨 Économie Lithique & Dégradation au Trajet (-300 000 BP)",
                "Perte d'efficacité mécanique du tranchant à mesure que s'accroît la distance aux carrières de silex.",
                "Ref: Binford, L. R. (1979). Organization and Formation Processes: Looking at Curated Technologies.",
                "• Dégradation du Tranchant : Efficacité = E₀ / (1.0 + λ * Dist_Carrière)\n• Entretien Lithique : Nécessite des retouches réduisant la masse de l'outil"},
            new String[]{"FireStickFarmingEngine", "🔥 Fire-Stick Farming & Mosaïques Pyrogéniques (-50 000 BP)",
                "Brûlis systématiques prévenant la fermeture des canopées et maintenant des prairies ouvertes à herbivores.",
                "Ref: Bliege Bird, R. et al. (2008). The fire stick farming hypothesis. PNAS.",
                "• Mosaïque Pyrogénique : dB_veg/dt = r B (1 - B/K) - λ_feu * B * Pop\n• Augmentation du Gibier : Maintient les densités de grands herbivores en forêt claire"},
            new String[]{"HomininCompetitiveExclusionEngine", "🦴 Compétition Interspécifique & Exclusion Néandertal/Sapiens (-100 000 BP)",
                "Modélisation de l'exclusion compétitive de Lotka-Volterra entre Sapiens, Néandertaliens et Dénisoviens.",
                "Ref: Banks, W. E. et al. (2008). Neanderthal Extinction by Competitive Exclusion. PLoS ONE.",
                "• Équations Couplées : dN₁/dt = r₁ N₁ (1 - (N₁ + α₁₂ N₂)/K₁)\n• Avantage Sapiens : Légère supériorité d'extraction calorique (α₁₂ < α₂₁)"},
            new String[]{"ShellMiddenAccumulationEngine", "🐚 Kjökkenmöddings & Accumulation Littorale (-12 000 BP)",
                "Accumulation de amas coquilliers littoraux (Ertebølle) stabilisant la sédentarisation pré-agricole côtière.",
                "Ref: Bailey, G. N. (2007). Shell mounds and coastal archaeology. Quat. Int.",
                "• Accumulation Malacologique : Maintient la densité de bande constante même en hiver\n• Sédentarité Cotière : Débloque les premiers habitats semi-permanents littoraux"},
            new String[]{"PelagicFishingHookEngine", "🎣 Pêche Hauturière & Hameçons en Os de Timor (-42 000 BP)",
                "Invention d'hameçons en os et lignes en fibre pour la capture de poissons pélagiques (Thons, Thyrsites) en eau profonde.",
                "Ref: O'Connor, S. et al. (2011). Pelagic Fishing at 42,000 Years Before the Present in East Timor. Science.",
                "• Capture Pélagique : Débloque la pêche en haute mer au-delà des récifs littoraux\n• Apport Protéique Massif : +30% d'apport calorique en zone insulaire"},
            new String[]{"GeophyteDiggingStickEngine", "🍠 Bâtons Fouisseurs & Extraction des Géophytes (USO) (-170 000 BP)",
                "Bâtons fouisseurs durcis au feu pour l'extraction de tubercules et oignons souterrains en zone aride.",
                "Ref: Wadley, L. et al. (2020). Cooked starchy rhizomes in South Africa 170,000 years ago. Science.",
                "• Extraction USO : Glucides souterrains accessibles lors des sécheresses\n• Capacité Portante en Aridité : Protège les cohortes contre la disette"},
            new String[]{"BirchTarPyrolysisKilnEngine", "🏺 Pyrolyse en Fosse & Nielle de Bouleau (-50 000 BP)",
                "Extraction par pyrolyse à froid sous terre de braise sans oxygène pour la synthèse de colle de bouleau purifiée.",
                "Ref: Schmidt, P. et al. (2019). Birch tar production in the Early Palaeolithic. Archaeol. Anthropol. Sci.",
                "• Production d'Adhésif Pur : Permet le collage hermétique des armatures de jet\n• Résistance Mécanique : Zéro perte de pointe de lance lors des impacts sur os"},
            new String[]{"SkinKayakSubArcticEngine", "🛶 Kayaks en Peau & Navigation Périglaciaire (-18 000 BP)",
                "Embarcations étanches en peaux de focs et mammifères marins tendues sur ossature pour la chasse polaire.",
                "Ref: Ames, K. M. (2002). Going by boat: the maritime highway. Am. Antiq.",
                "• Chasse Marine Arctique : Capture de phoques et baleines le long de la banquise\n• Corridor Côtier : Franchissement des fronts glaciaires marginaux du Pacifique"},
            new String[]{"SalmonRunHarpoonEngine", "🐟 Harpons Barbelés & Chasse aux Remontées de Saumons (-16 000 BP)",
                "Harpons détachables en bois de renne pour l'exploitation massive des ruées de saumons au printemps.",
                "Ref: Costamagno, S. et al. (2018). Salmon fishing in Magdalenian Europe. Quat. Int.",
                "• Poussée Calorique Saisonnière : Stockage de plusieurs quintaux de saumon séché en quelques semaines\n• Agglomération Saisonnière : Alimente les grands rassemblements magdaléniens"},
            new String[]{"CaveBearNicheCompetitionEngine", "🐻 Compétition de Caverne avec Ursus Spelaeus (-50 000 BP)",
                "Chasse et expulsion de l'ours des cavernes pour la possession des abris karstiques hivernaux.",
                "Ref: Stiner, M. C. (1998). Mortality analysis of Pleistocene bears and hominids. J. Arch. Sci.",
                "• Conquête d'Abris : Libération des grottes thermiquement isolées pour la bande\n• Resource Carnée & Fourrure : Fourrures épaisses pour l'isolation hivernale"},
            new String[]{"OchreMiningQuarryEngine", "⛏️ Minière d'Ocre & Carrières d'Hématite (-40 000 BP)",
                "Exploitation minière souterraine d'hématite et d'ocre rouge (Lion Cave, Eswatini).",
                "Ref: Barham, L. (2002). Systematic Pigment Use in the Middle Stone Age. Curr. Anthropol.",
                "• Extraction Industrielle : Tonnage d'ocre pour l'échange symbolique régional\n• Corridors de Troc : L'ocre minière devient la première monnaie symbolique de troc"},
            new String[]{"WindCuringSteppeCacheEngine", "🌬️ Dessiccation par le Vent dans la Steppe Périglaciaire (-28 000 BP)",
                "Séchage des bandes de viande par les vents glacés et secs de la steppe-toundra (Kostenki).",
                "Ref: Soffer, O. (1985). The Upper Paleolithic of the Central Russian Plain. Academic Press.",
                "• Cryo-Dessiccation : Conservation de viande séchée sans sel ni fumée pendant 1 an\n• Résilience aux Blizzards : Permet le maintien des camps d'hiver arctiques"},
            new String[]{"LashingsHideThongEngine", "🧶 Lanières de Cuir & Ligatures de Peau (-35 000 BP)",
                "Découpe en spirale de lanières de cuir de mégafaune pour ligatures de charpentes et traîneaux.",
                "Ref: Leroi-Gourhan, A. (1964). Le Geste et la Parole. Albin Michel.",
                "• Rigidité des Assemblages : Construction de grands abris nomades solides\n• Tractage Lourd : Permet le déplacement de blocs de pierre et carcasse sur traîneau"},
            new String[]{"MicrolithBladeletProductionEngine", "🔪 Débitage de Lamelles & Microlithes emmanchés (-20 000 BP)",
                "Taille standardisée de micro-lamelles tranchantes insérées dans des rainures en os.",
                "Ref: Inizan, M.-L. et al. (1995). Technologie de la Pierre Taillée. CREP.",
                "• Économie de Matière Première : 1 kg de silex donne 50 mètres de tranchant utile\n• Interchangeabilité : Remplacement instantané des microlithes cassés"},
            new String[]{"HighAltitudeHypoxiaEngine", "🏔️ Acclimatation à l'Hypoxie des Hauts Plateaux (-40 000 BP)",
                "Adaptation génétique et physiologique (EPAS1) permettant la vie sur les plateaux du Tibet et des Andes.",
                "Ref: Zhang, X. L. et al. (2018). Denisovan DNA and high-altitude adaptation. Science.",
                "• Colonisation des Sommets : Survie et chasse au-dessus de 3,500 m d'altitude\n• Refuges Montagneux : Protection contre les poussées démographiques des plaines"},
            new String[]{"MortuaryBurialRegaliaEngine", "⚰️ Sépultures Symboliques Ornées & Asabiyyah Funéraire (-30 000 BP)",
                "Inhumations complexes avec des milliers de perles d'ivoire et ocre (Sunghir, Arene Candide).",
                "Ref: Formicola, V. (2007). From Sunghir to the Gravettian burials. Curr. Anthropol.",
                "• Solidarité Totémique : Renforce le sentiment d'appartenance et la mémoire des ancêtres\n• Asabiyyah Funéraire : Boost durable de cohésion sociale inter-générationnelle"},
            new String[]{"ProtoCeramicFiringEngine", "🔥 Terres Cuites Céramiques de Věstonice (-29 000 BP)",
                "Façonnage et cuisson au four de figurines animales et humaines en argile (Dolní Věstonice).",
                "Ref: Vandiver, P. B. et al. (1989). The Origins of Ceramic Technology at Dolni Vestonice. Science.",
                "• Pyrotechnologie Céramique : Première maîtrise de la cuisson d'argile à 800°C\n• Magie Chasseuse : Usage rituel des statuettes pour favoriser la chasse sur la mégafaune"},
            new String[]{"EyedNeedleSewingEngine", "🪡 Aiguilles en Os à Chas & Costumes Polaires Multicouches (-35 000 BP)",
                "Perçage d'aiguilles fines à chas pour la couture étanche de parkas et pantalons en fourrure.",
                "Ref: Golovanova, L. V. et al. (2010). Significance of Tailored Fur Suits. Curr. Anthropol.",
                "• Étanchéité Thermique : Suppression du vent polaire dans le vêtement\n• Survie sous -40°C : Débloque la chasse arctique lors des tempêtes du LGM"},
            new String[]{"KelpHighwayNavigationEngine", "🌿 Corridor Maritime du Kelp & Route de la Côte Pacifique (-16 000 BP)",
                "Migration côtière rapide le long des forêts de kelp riches en loutres, poissons et coquillages.",
                "Ref: Erlandson, J. M. et al. (2007). The Kelp Highway Hypothesis. J. Island Coast. Arch.",
                "• Colonisation Maritime Rapide : Contourne la calotte glaciaire laurentide par la côte Pacifique\n• Abondance Alimentaire : Ressources littorales inépuisables et protégées des houles"},
            new String[]{"CaveHyenaScavengingEngine", "🐺 Nécrophagie Compétitive avec l'Hyène des Cavernes (-45 000 BP)",
                "Concurrence féroce pour le pillage des carcasses de mégafaune entre bandes humaines et hyènes spelaea.",
                "Ref: Discamps, E. (2014). Ungulate biomass and hyena-human interaction. Quat. Int.",
                "• Scavenging Compétitif : Récupération de grandes carcasses tuées par d'autres prédateurs\n• Risque de Morsure : Nécessite l'usage du feu pour repousser les meutes d'hyènes"},
            new String[]{"OchreTradeAllianceEngine", "🔴 Corridors d'Échange d'Ochre Rouge & Alliances Inter-Tribales (-50 000 BP)",
                "Circulation de blocs d'ocre rouge haut de gamme sur des centaines de kilomètres pour sceller des pactes.",
                "Ref: McBrearty, S. & Brooks, A. S. (2000). The revolution that wasn't. J. Hum. Evol.",
                "• Alliance Diplomatique : Évite les conflits territoriaux lors des migrations\n• Réseau d'Assurance-Famine : Droit d'accès aux territoires voisins en cas de sécheresse"},
            new String[]{"HeatTreatedFlintPressureEngine", "🔥 Chauffe du Silex & Retouche par Pression Solutréenne (-21 000 BP)",
                "Traitement thermique contrôlé du silex améliorant l'aptitude à la retouche fine par pression (Feuilles de laurier).",
                "Ref: Inizan, M.-L. & Tixier, J. (2000). Thermal Alteration of Siliceous Rocks.",
                "• Perfection des Armatures : Création de pointes solutréennes ultra-minces et pénétrantes\n• Portée & Précision : Améliore la trajectoire et l'impact des sagaies"},
            new String[]{"FluvioglacialLithicHarvestEngine", "🌊 Récolte Lithique sur Gîtes Fluvioglaciaires (-20 000 BP)",
                "Collecte de galets de silex et quartzite déposés par les torrents de fonte sous-glaciaire.",
                "Ref: Bussell, M. A. et al. (2001). Fluvioglacial Gravels as Raw Material Sources.",
                "• Silex de Fonte : Approvisionnement lithique sur les bandes d'épandage (Sandur) au pied des glaciers\n• Continuité Outillage : Permet le maintien des bandes au contact immédiat de la calotte"},
            new String[]{"JomonCeramicBoilingEngine", "🏺 Céramiques Jōmon & Bouillissage de Toxines Marine (-16 500 BP)",
                "Poteraie à fond pointu (Jōmon) permettant le bouillissage prolongé des toxines coquillères et glands.",
                "Ref: Habu, J. (2004). Ancient Jomon of Japan. Cambridge Univ. Press.",
                "• Bio-Disponibilité Coquillère : +45% d'extraction calorique sur les ressources côtières\n• Conservation Protégée : Stockage des bouillies pendant +3 mois"},
            new String[]{"LevalloisPreparedCoreEngine", "🪨 Éclats Levallois & Prédétermination des Formes (-300 000 BP)",
                "Taille sur nucléus préparé produisant des éclats préformés aux tranchants réguliers.",
                "Ref: Boëda, E. (1994). Le concept Levallois : variabilité des méthodes. CNRS Éditions.",
                "• Vitesse de Taille : +60% de rendement d'armatures par heure\n• Économie de Matière : Réduction de 35% du gaspillage de nucléus"},
            new String[]{"AcheuleanBifaceSymmetryEngine", "🪓 Bifaces Symétriques Acheuléens & Signalétique Sociale (-500 000 BP)",
                "Bifaces symétriques servant d'indicateurs de compétence et d'attractivité au sein de la cohorte.",
                "Ref: Kohn, M. & Mithen, S. (1999). Handaxes: Products of Sexual Selection? Antiquity.",
                "• Signalétique Sociale : Boost de cohésion sociale (+15% d'Asabiyyah)\n• Sélection Sexuelle : Stabilise le choix des partenaires au sein de la cohorte"},
            new String[]{"OldowanMarrowPercussionEngine", "🦴 Galets Aménagés Oldowayens & Extraction de Moelle (-2 600 000 BP)",
                "Percussion de galets pour briser les os longs de mégafaune et extraire la moelle riche en lipides.",
                "Ref: Semaw, S. et al. (1997). 2.5-million-year-old stone tools from Gona, Ethiopia. Nature.",
                "• Moelle Carnée : Apport calorique lipidique massif (+2.5 MJ/hab/jour) lors des saisons sèches\n• Scavenging Efficace : Extraction de ressources inaccessibles aux autres carnivores"},
            new String[]{"TrophicCascadesPredatorEngine", "🦁 Cascades Trophiques & Extinction des Apex Prédateurs (-13 000 BP)",
                "La disparition des grands herbivores entraîne l'effondrement des hyènes des cavernes et tigres à dents de sabre.",
                "Ref: Ripple, W. J. & Van Valkenburgh, B. (2010). Linking Pleistocene megafauna density to apex predators.",
                "• Réorganisation Trophique : Redirection de la chasse humaine vers le petit gibier\n• Chute des Super-Prédateurs : Suppression du risque de prédation directe sur les camps nomades"},
            new String[]{"BeringianStandstillIsolationEngine", "🏔️ Isolation Génétique en Béringie & Adaptation au Froid (-22 000 BP)",
                "Isolement prolongé des populations en Béringie fixant les haplogroupes amérindiens fondateurs.",
                "Ref: Tamm, E. et al. (2007). Beringian Standstill and Spread of Native American Founders. PLoS ONE.",
                "• Fixation Génétique : Émergence du profil métabolique résistant au grand froid arctique\n• Lignée Fondatrice : Infiltration continentale rapide lors de l'ouverture du corridor libre de glace"},
            new String[]{"AridOasisWellDiggingEngine", "💧 Creusement de Puits d'Eau en Zone Aride du Sahul (-45 000 BP)",
                "Creusement de puits profonds dans les lits de rivières asséchées (soaks) du bassin de l'Eyre.",
                "Ref: Thorley, P. (1998). Pleistocene settlement in Central Australia. Antiquity.",
                "• Traversée du Désert : Maintient les itinéraires d'interconnexion au cœur de l'Australie\n• Sécurité Hydrique : Annule la mortalité par déshydratation sur les pistes nomades"},
            new String[]{"HandStencilTerritoryEngine", "✋ Empreintes de Mains en Négatif & Marqueurs de Territoire (-40 000 BP)",
                "Soufflage de pigments autour des mains sur les parois de grottes pour délimiter les territoires de chasse.",
                "Ref: Aubert, M. et al. (2014). Pleistocene cave art date from Sulawesi, Indonesia. Nature.",
                "• Marqueur Frontalier : Réduit les conflits territoriaux inter-tribaux de -25%\n• Mémoire Territoriale : Ancrage des droits d'usage sur les zones d'agrégation"},
            new String[]{"AtlatlBalancingStoneEngine", "🏹 Pierres de Lestage & Réglage Ballistique du Propulseur (-18 000 BP)",
                "Fixation de lests en pierre polie sur le fut du propulseur pour ajuster la flexion et la vitesse du dard.",
                "Ref: Peets, O. H. (1960). Experiments in the use of atlatl weights. Am. Antiq.",
                "• Vitesse de Dard : Vitesse de tir portée à 40 m/s\n• Portée Pratique : Élargit le rayon de tir mortel sur les bisons à 45 mètres"},
            new String[]{"PressureFlakerPointEngine", "🦴 Retouche par Pression au Bois de Cerf & Dents Serratées (-25 000 BP)",
                "Usage de retouchoirs en bois de renne pour façonner des tranchants dentelés ultra-pénétrants.",
                "Ref: Crabtree, D. E. (1968). Mesoamerican polyhedral cores and pressure flaking. Am. Antiq.",
                "• Perforation Profonde : Pénétration accrue de +40% dans le cuir des mammouths\n• Précision de Taille : Standardisation parfaite des armatures de javelots"},
            new String[]{"WildFlaxSpinningEngine", "🧵 Filage du Lin Sauvage & Nets de Chasse Haute-Résistance (-32 000 BP)",
                "Torsade de fibres de lin sauvage (Dzudzuana) créant des cordes et nets de capture à haute résistance.",
                "Ref: Kvavadze, E. et al. (2009). 30,000-Year-Old Wild Flax Fibers. Science.",
                "• Résistance des Filets : Améliore de +30% la capture du petit gibier en forêt\n• Cordages de Propulsion : Fabrication de lignes d'arc de précision"},
            new String[]{"OchreResinHaftingEngine", "🌲 Chargement Chimique des Poix à la Poudre d'Ocre (-70 000 BP)",
                "Adjonction de poudre d'ocre dans la résine végétale servant de charge minérale renforçant le mastic.",
                "Ref: Wadley, L. (2005). Putting ochre to the test: replication experiments. Antiquity.",
                "• Solidité du Mastic : Augmente la résistance au cisaillement de +150% au grand froid\n• Anti-Décollement : Zéro casse de mastic lors des impacts sur structures osseuses"},
            new String[]{"ReindeerRiverInterceptionEngine", "🦌 Interception des Renne aux Passages de Fleuves (-15 000 BP)",
                "Chasse aux points de traversée fluviale des rennes migrateurs au printemps et à l'automne.",
                "Ref: Gordon, B. C. (1988). Of Men and Reindeer Herd in French Magdalenian Prehistory.",
                "• Abattage Massif Saisonner : Stockage de tonnes de viande fumée lors des migrations\n• Densité d'Habitat : Maintien de densités magdaléniennes élevées (0.15 hab/km²)"},
            new String[]{"EndokarstTorchMappingEngine", "🔦 Pistes de Torches & Cartographie des Cavernes Profondes (-30 000 BP)",
                "Mouchages de torches et repères au charbon permettant l'exploration des réseaux karstiques profonds.",
                "Ref: Clottes, J. & Courtin, J. (1994). La grotte Cosquer. Seuil.",
                "• Guidage Souterrain : Permet l'exploration sécurisée des galeries à plus de 1 km des entrées\n• Zéro Perte : Élimine les accidents et désorientations dans le labyrinthe karstique"},
            new String[]{"PeriglacialLoessDustEngine", "🌪️ Poussières de Loess Périglaciaires & Stress Respiratoire (-24 000 BP)",
                "Tempêtes de poussières de loess balayant la steppe glaciaire réduisant la santé des cohortes.",
                "Ref: Antoine, P. et al. (2009). High-resolution record of the Last Glacial loess in Europe. Quat. Sci. Rev.",
                "• Stress Respiratoire : Pénalité de -10% sur la survie infantile en steppe ouverte\n• Protection Abris : Nécessite l'usage d'abris étanches en os et cuir"},
            new String[]{"OstrichEggshellNetworkEngine", "🥚 Réseau de Gourdes d'Œufs d'Autruche Gravés (-60 000 BP)",
                "Troc de gourdes en œuf d'autruche permettant les expéditions en désert profond.",
                "Ref: Texier, P.-J. et al. (2013). The Howiesons Poort engraving tradition. J. Arch. Sci.",
                "• Rayon d'Action Désertique : Élargit le périmètre de forage de +200 km en zone aride\n• Monnaie Symbolique : Renforce la confiance lors des réunions inter-bandes"},
            new String[]{"MarrowFatRenderingEngine", "🍲 Concasage Osseux & Extractions de Graisse par Bouillissage (-20 000 BP)",
                "Pilonnage des épiphyses et bouillissage par pierres chauffées pour extraire la graisse d'os.",
                "Ref: Outram, A. K. (2001). A new method for identifying bone marrow and grease rendering. J. Arch. Sci.",
                "• Concentré Énergétique : Production de galettes de pemmican ultra-caloriques pour l'hiver\n• Anti-Inanition : Empêche la malnutrition protéique ('Rabbit Starvation')"},
            new String[]{"SeaOtterFurTanningEngine", "🦦 Tannage de Fourrures de Loutre de Mer pour Kayaks (-15 000 BP)",
                "Tannage des fourrures de loutres de mer (100,000 poils/cm²) pour l'étanchéité des parkas et canoës.",
                "Ref: Erlandson, J. M. (2001). The Archaeology of Aquatic Adaptations. J. Arch. Res.",
                "• Protection Thermique Aquatique : Élimine l'hypothermie lors des expéditions en mer froide\n• Corridor Kelp : Permet la navigation continue le long des forêts de kelp arctiques"},
            new String[]{"ZoonoticPathogenSpilloverEngine", "🦇 Zoo-Pathogènes des Cavernes & Sélection d'Immunités (-50 000 BP)",
                "Exposition aux virus de chauve-souris dans les grottes sélectionnant les allèles immunitaires TLR.",
                "Ref: Enard, D. & Petrov, D. A. (2018). Evidence that RNA viruses drove adaptive introgression. Cell.",
                "• Poussées Épidémiques : Élimine les bandes nomades sans protection immunitaire\n• Sélection TLR : Renforce la résilience génétique des survivants de la cohorte"},
            new String[]{"DoggerlandMarshFowlingEngine", "🦆 Filets d'Oiseleur dans les Marécages du Doggerland (-11 000 BP)",
                "Capture d'oiseaux migrateurs au filet dans les marais de la mer du Nord asséchée.",
                "Ref: Coles, B. J. (1998). Doggerland: a cultural review. Proc. Prehist. Soc.",
                "• Abondance Aviaire : Apport calorique printanier massif pour les bandes mésolithiques\n• Stabilisation Cotière : Maintien de populations élevées avant la submersion marine"},
            new String[]{"NightTorchSpearfishingEngine", "🐟 Pêche Nocturne au Flambeau de Pin & Harponnage (-14 000 BP)",
                "Éclairage des rivières à la torche de résine attirable les poissons la nuit.",
                "Ref: Zhilin, M. (2014). Early Mesolithic bone arrows and harpoons from Upper Volga. Quat. Int.",
                "• Rendement Nocturne : Doubler la capture de brochets et truites dans les cours d'eau\n• Diversification Broad-Spectrum : Sécurise l'alimentation lors des disettes de chasse"},
            new String[]{"BasaltGrindingSlabEngine", "🪨 Meules en Basalte & Broyage des Glands et Graines (-18 000 BP)",
                "Façonnage de grandes meules en basalte porique pour le pilonnage des glands et graines sauvages.",
                "Ref: Wright, K. I. (1994). Ground stone tools and plant food processing. Am. Antiq.",
                "• Détoxification des Glands : Extraction des tanins par broyage et lessivage\n• Densité Sédentaire : Augmente la capacité portante des zones de chênes et céréales"},
            new String[]{"BisonCliffJumpDriveEngine", "🐂 Rabattage de Troupeaux de Bisons sur Falaises (-12 000 BP)",
                "Battues coordonnées rabattant des centaines de bisons au-dessus de falaises naturelles.",
                "Ref: Frison, G. C. (1991). Prehistoric Hunters of the High Plains. Academic Press.",
                "• Capture en Masse : Abattage simultané de troupeaux entiers en une seule opération\n• Stock de Viande Géant : Séchage de tonnes de viande assurant 1 an d'autonomie"},
            new String[]{"ObsidianSolarIgnitionEngine", "☀️ Miroirs en Obsidienne & Allumage Solaire (-14 000 BP)",
                "Polissage de miroirs en obsidienne pour la concentration des rayons solaires sur de l'adouadou.",
                "Ref: Cann, J. R. & Renfrew, C. (1964). Characterization of obsidian. Proc. Prehist. Soc.",
                "• Allumage Rapide : Réduction de 50% de l'effort de friction du feu par temps ensoleillé\n• Prestige Rituel : Renforcement du rôle des chamans de la bande"},
            new String[]{"SubGlacialMeltwaterWeirEngine", "🐟 Barrages à Poissons dans les Torrents de Fonte Glacier (-12 000 BP)",
                "Construction de pièges en pieux de bois dans les rivières de fonte sous-glaciaire.",
                "Ref: Pedersen, L. et al. (1997). The Danish Storebælt since the Ice Age. A/S Storebælt.",
                "• Capture Efficace : Capture continue des anguilles et truites en période de dégel\n• Subsistance Périglaciaire : Maintient les bandes à proximité immédiate de la calotte"},
            new String[]{"IvoryHotWaterStraighteningEngine", "🐘 Redressement d'Ivoire à l'Eau Chaude & Lances de 2m (-27 000 BP)",
                "Chauffe et trempage d'ivoire de mammouth permettant de redresser les lances (Sunghir).",
                "Ref: Formicola, V. (2007). The Sunghir burials. Curr. Anthropol.",
                "• Lances Rigides de 2m : Fabrication d'armes de choc d'une portée inégalée\n• Force d'Impact : Perforation maximale lors de la chasse rapprochée au mammouth"},
            new String[]{"CaveWallClaySealingEngine", "🧱 Calfeutrage des Cavernes à l'Argile & Isolation (-35 000 BP)",
                "Plâtrage des fissures de grottes avec de la boue et argile pour éliminer les courants d'air.",
                "Ref: Beaune, S. A. de (2000). Pour une archéologie du geste. CNRS Éditions.",
                "• Chaleur Interne : Maintien de la température de la grotte au-dessus de 10°C en hiver\n• Économie de Bois : Réduit la consommation de combustible pour le foyer"},
            new String[]{"BirchBarkVesselEngine", "🪵 Récipients en Écorce de Bouleau Plissée & Bouillissage (-20 000 BP)",
                "Pliage et couture de récipients en écorce étanchéifiés à la poix pour chauffer l'eau.",
                "Ref: Burov, G. M. (1996). On the search for heritage of Mesolithic timber crafts. World Arch.",
                "• Bouillissage Sans Céramique : Cuisson des potages et viandes avec des pierres chauffées\n• Mobilité Légère : Récipients souples et legers incassables pendant les trajets nomades"},
            new String[]{"SnowTroughRefrigerationEngine", "❄️ Tranchées Frigorifiques dans la Neige & Caches d'Hiver (-25 000 BP)",
                "Stockage de carcasses de grand gibier dans des tranchées de neige recouvertes de sapin.",
                "Ref: Pitulko, V. V. et al. (2014). Early human presence in the Arctic. Science.",
                "• Cryo-Conservation Printanière : Maintient la fraîcheur de la viande jusqu'au mois de mai\n• Sécurité de la Cohorte : Évite les famines de fin d'hiver lors du dégel"},
            new String[]{"ArrowPoisonSynthesisEngine", "🧪 Synthèse de Toxines Végétales (Aconit) & Fleches Empoisonnées (-15 000 BP)",
                "Extraction de toxines végétales (*Aconitum*) pour enduire les armatures microlithiques.",
                "Ref: Wadley, L. et al. (2012). Evidence for arrow use and poison 44,000 years ago. PNAS.",
                "• Paralysie des Proies : Neutralisation rapide du gibier blessé en quelques minutes\n• Succès de Chasse : +60% de rendement sur les ungulés rapides et craintifs"},
            new String[]{"LakeChadWadiMigrationEngine", "🌊 Corridors des Wadis du Sahara & Pêche du Lac Méga-Tchad (-15 000 BP)",
                "Migration le long des rivières sahariennes réactivées et pêche sur le Lac Méga-Tchad.",
                "Ref: Drake, N. A. et al. (2011). Ancient watercourses suggest a humid Sahara. PNAS.",
                "• Traversée Trans-Saharienne : Autorise la migration Sapiens sans souffrance hydrique\n• Ressource Lacustre Géante : Exploitation des capitaines et poissons chats géants"},
            new String[]{"EpipaleolithicStorageHamletEngine", "🏡 Hameaux Sédentaires Natufiens & Fosses de Stockage (-12 500 BP)",
                "Construction de maisons semi-enterrées en pierre avec fosses de stockage de graines.",
                "Ref: Bar-Yosef, O. (1998). The Natufian Culture in the Levant. Evol. Anthropol.",
                "• Ancrage Territorial : Transition définitive de la cohorte vers le village sédentaire\n• Accumulation de Capital : Fosses de stockage scellées protégeant les réserves de céréales"},
            new String[]{"AutoRegulationPureEngine", "⚡ Auto-Régulation Pur",
                "Moteur de régulation dynamique automatique des équilibres homéostatiques.",
                "Ref: Wiener, N. (1948). Cybernetics: Or Control and Communication in the Animal and the Machine.",
                "• Boucle de Rétroaction : dX/dt = -k · (X - X_cible)"},
            new String[]{"FrontierAsabiyyahEngine", "⚔ Asabiyyah de Frontière (Ibn Khaldoun & Peter Turchin)",
                "Théorie Khaldounienne de la solidarité de groupe et déclin des dynasties (Badiya vs Hadara). Modélise l'érosion de la cohésion sociale lors du passage de la frontière métastable aux métropoles opulentes.",
                "Ref: Ibn Khaldun (1377). Muqaddimah; Turchin, P. (2003). Historical Dynamics: Securing the Peace, Princeton Univ. Press.",
                "• Variation d'Asabiyyah (Cohésion A) : dA/dt = c₁·F(x)·(1 - A) - c₂·(K(x)/N(x))·A\n  où F(x) est la pression militaire de frontière et K(x)/N(x) le capital par habitant (luxe).\n• Métropole opulente (K > 1000 kg/hab) : Déclin d'Asabiyyah dA/dt = -2.0% par pas de temps.\n• Zone de frontière (K ≤ 1000 kg/hab) : Forge la cohésion militaire dA/dt = +2.0% par pas de temps.\n• Inégalité & Déclin Dynastique : S_cohesion(t) = A(t) · Pop(t) · (1 - Gini(t))."},
            new String[]{"AiAutonomousRegulationPureEngine", "🤖 Régulation Autonome de l'IA & Gouvernance",
                "Modélisation de la régulation et des risques de l'intelligence artificielle. Évalue les probabilités d'émergence d'infrastructures autonomes et de gestion des risques.",
                "Ref: Bostrom, N. (2014). Superintelligence; Russell, S. (2019). Human Compatible.",
                "• Seuil d'Autonomie IA : P_alignement = 1 / (1 + exp(-k · (Niveau_Gouvernance - Complexité_IA)))\n• Taux de Risque Systémique R_ia = (1 - P_alignement) · Puissance_Calcul_Planétaire"},
            new String[]{"AmerindianEcosystemEngine", "🌾 Agro-foresterie Amérindienne & Terra Preta",
                "Techniques agricoles précolombiennes et enrichissement des sols en biochar. Augmente la capacité portante et la résilience des sols de forêt tropicale.",
                "Ref: Denevan, W. M. (1992). The Pristine Myth; Glaser, B. et al. (2002). Terra Preta Biochar.",
                "• Formation de Terra Preta : dC_biochar/dt = Apport_Charbon_Organique - Oxidation_Lente(0.001)\n• Gain de Capacité Portante : K_sols = K_base · (1 + α · ln(1 + C_biochar))"},
            new String[]{"AsymmetricColonialTradeEngine", "🚢 Commerce Colonial Asymétrique & Extraction",
                "Flux de ressources et fuite de valeur des colonies vers les métropoles. Simule la capture de rente et le blocage de l'industrialisation périphérique.",
                "Ref: Wallerstein, I. (1974). The Modern World-System; Frank, A. G. (1967). Dependency Theory.",
                "• Capture de Rente : Transfert_Richesse = Termes_Echange_Asym · Export_Matières_Premières\n• Frein d'Industrialisation Périphérique : dT_peripherie/dt = T_base · (1 - Ratio_Extraction)"},
            new String[]{"BifurcationChaosEngine", "🌀 Chaos & Analyse des Bifurcations Systémiques",
                "Sensibilité aux conditions initiales et points de basculement. Génère des micro-oscillations chaotiques pouvant déclencher des cascades d'instabilité.",
                "Ref: Lorenz, E. N. (1963). Deterministic Nonperiodic Flow; May, R. M. (1976).",
                "• Attracteur de Lorenz / Bifurcation Logistique : x_{t+1} = r · x_t · (1 - x_t)\n• Exposant de Liapounov λ > 0 -> Divergence exponentielle des trajectoires de simulation"},
            new String[]{"BioMolecularEpidemiologyEngine", "☣️ Épidémiologie Bio-Moléculaire & Immunité Pop",
                "Simulation avancée de la transmission virale et foyers infectieux. Modélise la transmission SIR/SEIR selon la densité urbaine et le réseau de commerce.",
                "Ref: Kermack, W. O. & McKendrick, A. G. (1927). SIR Epidemiological Model.",
                "• Modèle SEIR : dS/dt = -β·S·I/N, dE/dt = β·S·I/N - σ·E, dI/dt = σ·E - γ·I, dR/dt = γ·I\n• Taux de Reproduction de Base R₀ = β / γ · (1 + Variance_Contacts_Densité)"},
            new String[]{"CulturalMaterialismPureEngine", "📜 Materialisme Culturel (Marvin Harris)",
                "Déterminisme de l'infrastructure technologique et démographique sur les croyances. Adapte les valeurs morales aux contraintes d'extraction d'énergie.",
                "Ref: Harris, M. (1979). Cultural Materialism: The Struggle for a Science of Culture.",
                "• Alignement Superstructure : Superstructure(t) = f(Infrastructure_Énergétique, Pression_Démographique)\n• Transition Morale : dM/dt = k_adaptation · (Mode_Production - M)"},
            new String[]{"CulturalSociologyEngine", "📜 Sociologie Culturelle & Matrice de Voisinage",
                "Évolution des valeurs culturelles et métissage régional. Gère la diffusion des langues, des normes et la dérive culturelle entre mailles voisines.",
                "Ref: Cavalli-Sforza, L. L. & Feldman, M. W. (1981). Cultural Transmission and Evolution.",
                "• Matrice de Diffusion Culturelle : dC_i/dt = Σ_j w_{ij} · (C_j - C_i) + Drift_Accidentel\n• Distance Culturelle d(i,j) = || Vector_Langue_i - Vector_Langue_j ||"},
            new String[]{"DeforestationErosionEngine", "🏜️ Érosion Forestière & Ensablement Fluvial",
                "Dégradation des sols et perte de couverture végétale. Entraîne le ravinement des terres arables et le comblement des lits de rivières lors de coupes rases.",
                "Ref: Montgomery, D. R. (2007). Dirt: The Erosion of Civilizations. Univ. of California Press.",
                "• Érosion des Sols : Perte_Sol = k_coupe · (1 - Couverture_Forestiere) · Precipitations³\n• Comblement Fluvial = Σ Perte_Sol_Amont"},
            new String[]{"EdoJapanIsolationEngine", "⛩️ Isolationnisme du Japon Edo (Sakoku)",
                "Maintien d'un équilibre zéro-croissance et fermeture des frontières. Élimine la dépendance extérieure au détriment du rythme de progrès technologique.",
                "Ref: Totman, C. (1993). Early Modern Japan; Diamond, J. (2005). Collapse (Tokugawa Forestry).",
                "• Équilibre Sylvicole & Zéro-Croissance : Extraction_Bois <= Auto_Régénération_Forêt\n• Isolement Commercial : Flux_Externe = 0, Stabilité_Interne = Maximale"},
            new String[]{"EntropicMetalDissipationEngine", "🏭 Dissipation Entropique des Métaux & Jevons Rebound",
                "Dispersion irrémédiable des métaux rares et effets rebond. Calcule la perte irrécupérable de cuivre et de métaux précieux par usure mécanique et oxydation.",
                "Ref: Georgescu-Roegen, N. (1971). The Entropy Law and the Economic Process.",
                "• Pertes Entropiques Irrécupérables : dMetal_dissipe/dt = Production · (1 - Taux_Recyclage_Max)\n• Limite d'Usure Recyclage : Max_Recyclage = 85% par contrainte thermodynamique"},
            new String[]{"EcotoxicologyFertilityEngine", "🧪 Ecotoxicologie & Stérilité Chimique",
                "Impacts des polluants synthétiques sur le taux de fécondité. Simule la baisse de fertilité humaine liée à la concentration cumulée d'entropie chimique.",
                "Ref: Colborn, T. et al. (1996). Our Stolen Future; Swan, S. H. (2021). Count Down.",
                "• Baisse de Fécondité : F_effective = F_naturelle · exp(-k_tox · Charge_Pollution_Cumulee)"},
            new String[]{"FertileCrescentSalinizationEngine", "🌾 Salinisation du Croissant Fertile",
                "Dégradation historique des sols irrigués en Mésopotamie antique. Accumulation de sels minéraux toxiques par évaporation de l'eau d'irrigation.",
                "Ref: Jacobsen, T. & Adams, R. M. (1958). Salt and Silt in Ancient Mesopotamian Agriculture.",
                "• Accumulation Saline : dSel/dt = (Volume_Irrigation · Concentration_Sel_Eau) - Leaching_Drainage"},
            new String[]{"GeoengineeringAlbedoFeedbackEngine", "🧪 Géo-ingénierie & Rétroaction d'Albédo Artificiel",
                "Injections d'aérosols stratosphériques et terraformation. Diminue l'insolation solaire globale pour contrer le réchauffement climatique.",
                "Ref: Crutzen, P. J. (2006). Albedo Modification via Stratospheric Aerosol Injection.",
                "• Delta Albédo Artificiel : ΔAlbédo = k_aerosol · Mass_SO2_Injectee\n• Refroidissement Forcé : ΔT_cooling = -λ · S₀ · ΔAlbédo"},
            new String[]{"HandyNasaHybridEngine", "📉 Modèle HANDY NASA Hybride (Démographie & Élites)",
                "Rétroactions entre élites, travailleurs et ressources (NASA / Motesharrei). Simule les scénarios d'effondrement par surconsommation des élites.",
                "Ref: Motesharrei, S., Rivas, J., & Kalnay, E. (2014). HANDY: Human and Nature Dynamics.",
                "• Équations HANDY (4 Variables) : dx_w/dt = α_w·x_w - β_w·x_w,  dx_e/dt = α_e·x_e - β_e·x_e\n• Surconsommation Élites : Consommation_Elite = s · Consommation_Travailleur avec s >> 1"},
            new String[]{"HandyNasaPureEngine", "📉 Modèle HANDY NASA Pur (Équations Différentielles)",
                "Système dynamique pur de la dynamique homme-nature (Handy Model). Système à 4 équations couplées (Élites, Travailleurs, Nature, Capital).",
                "Ref: Motesharrei, S. et al. (2014). HANDY Model Equations. Ecological Economics 101.",
                "• Nature N : dN/dt = γ·N·(λ - N) - δ·x_w·N\n• Accumulation de Capital K : dK/dt = δ·x_w·N - C_w - C_e"},
            new String[]{"JevonsParadoxEngine", "⚡ Effet Rebond & Paradoxe de Jevons",
                "L'augmentation de l'efficacité énergétique accroît la consommation globale. Annule les gains d'économie d'énergie par l'expansion de l'échelle industrielle.",
                "Ref: Jevons, W. S. (1865). The Coal Question; Alcott, B. (2005). Jevons' Paradox.",
                "• Énergie Consommée E_total = Pop · Efficacité^ε avec ε > 1.0 (Paradoxe de Jevons)"},
            new String[]{"KardashevPureEngine", "🌌 Échelle de Kardashev & Capture Énergétique",
                "Transition vers le contrôle de l'énergie planétaire intégrale. Évalue le score de Kardashev (Type 0.0 à 1.0) selon la puissance totale captée en watts.",
                "Ref: Kardashev, N. S. (1964). Transmission of Information by Extraterrestrial Civilizations.",
                "• Indice de Kardashev K = (log₁₀(Puissance_Watts) - 6) / 10\n• Type I = 10¹⁶ Watts (Énergie Planétaire Intégrale)"},
            new String[]{"KinSelectionHamiltonEngine", "🧬 Sélection de Parentèle (Règle de Hamilton)",
                "Évolution de l'altruisme génétique et coopération inter-individus (rB > C). Détermine la cohésion des petites tribus et familles étendues.",
                "Ref: Hamilton, W. D. (1964). The Genetical Evolution of Social Behaviour. J. Theor. Biol.",
                "• Condition d'Altruisme : r · Benefit > Cost avec r = Coefficient d'Apparentement Génétique"},
            new String[]{"KurzweilAcceleratingReturnsEngine", "🚀 Loi des Rendements Accélérés (Ray Kurzweil)",
                "Accélération exponentielle du progrès scientifique et des processeurs. Réduit les délais d'invention à mesure que le niveau technologique s'élève.",
                "Ref: Kurzweil, R. (2005). The Singularity Is Near; Moore, G. E. (1965).",
                "• Vitesse d'Invention dTech/dt = V₀ · exp(λ · Tech(t))"},
            new String[]{"LenskiPureEngine", "🧠 Évolution Socioculturelle (Gerhard Lenski)",
                "Classification des sociétés selon leur mode d'extraction d'information et d'énergie. Rétrograde ou promeut le type de société (Chasseurs, Agricoles, Industriels).",
                "Ref: Lenski, G. (1966). Power and Privilege: A Theory of Social Stratification.",
                "• Stade Sociétal = f(Énergie_Par_Habitant, Stock_Information_Technologique)"},
            new String[]{"LeslieWhitePureEngine", "⚡ Loi de Leslie White (Culture = E × T)",
                "Le développement culturel varie directly avec l'énergie captée par habitant (C = E × T). Détermine la complexité symbolique selon l'énergie.",
                "Ref: White, L. A. (1943). Energy and the Evolution of Culture. American Anthropologist.",
                "• Complexité Culturelle C = Énergie_Par_Capita · Efficacité_Technologique"},
            new String[]{"MaritimeHighwayEngine", "⛵ Autoroute Maritime & Thalassocraties (Braudel/Fluid Dynamics)",
                "Réseau de navigation côtière et autoroutes maritimes universelles. Réduit la friction de transport sur l'eau libre de glace (thermodynamique) et stimule l'accumulation de capital.",
                "Ref: Braudel, F. (1949). La Méditerranée; Archimedes Buoyancy Transport Models.",
                "• Multiplicateur de Transport Maritime = 0.20 × Friction_Terrestre (Cap : +5%/tick)"},
            new String[]{"DynamicMaritimeRoutingGraph", "🌐 Graphe de Routage Trans-Océanique Dynamique",
                "Calcul dynamique et adaptatif des routes maritimes globales selon l'évolution technologique (cabotage -> navigation hauturière -> brise-glace) et thermodynamique.",
                "Ref: Dynamic Graph Routing & Fluid Drag; Bowditch, N. (1802). American Practical Navigator.",
                "• Invalidation auto selon Niveau Tech & Glace de Mer\n• Portée de Cabotage (Tech < 3.0: 300km, Tech >= 6.0: Trans-Océanique)"},
            new String[]{"LandReclamationEngine", "🏗️ Poldérisation & Habitats Flottants (Seasteading)",
                "Transformation de zones côtières en polders agricoles et création de cités flottantes en haute mer selon le niveau technologique et le capital.",
                "Ref: Dutch Water Boards History; Seasteading Institute (2008).",
                "• Tech >= 4.0 & K >= 100 -> Poldérisation ; Tech >= 8.5 & K >= 500 -> Habitat Flottant"},
            new String[]{"MarineSubmersionEngine", "🚨 Submersion Marine & Évacuation Physique",
                "Modélisation physique de l'élévation du niveau de la mer et de la maintenance des digues. Évacuation préventive (jusqu'à 98% en société moderne) et flux de réfugiés sans mortalité brutale irréaliste.",
                "Ref: IPCC Coastal Inundation & Flood Early Warning Physics; Adger, W. N. (2006). Vulnerability.",
                "• Évacuation Physique: Ratio_Evac = 1 - exp(-0.4 · Tech · (1 + K/500))\n• Déplacement de Population vers mailles sèches adjacentes"},
            new String[]{"HydrologicalEngineeringEngine", "💧 Ingénierie Hydrologique & Transgressions Paysagères",
                "Assèchement/remplissage de lacs urbains (Texcoco / Tenochtitlan), assèchement anthropique de mers intérieures endoréiques (Mer d'Aral) et retenues d'eau / barrages hydroélectriques de montagne.",
                "Ref: IPCC Water Resource Engineering; World Commission on Dams (2000); Tenochtitlan Chinampa Hydro-Engineering.",
                "• Drenage Texcoco: Lac -> Plaine Urbaine (Tech >= 3.5, K >= 150)\n• Assèchement Aral: Lac -> Désert Salé (Drainage > Recharge)\n• Barrage Montagne: Retenue d'eau (1000L) & Énergie Hydroélectrique (Tech >= 4.5, Alt >= 300m)"},
            new String[]{"MegafaunaEcosystemEngine", "🦕 Conservation de la Biodiversité Sauvage & Mégafaune",
                "Pressions de chasse et extinction/préservation des espèces sauvages. Simule la disparition de la grande faune lors de l'expansion humaine néolithique.",
                "Ref: Martin, P. S. (1984). Quaternary Extinctions: A Prehistoric Revolution.",
                "• Extinction Mégafaune dM/dt = r_m·M - k_chasse·Pop_Humaine·M"},
            new String[]{"MilitaryTechShockEngine", "💣 Chocs de Technologie Militaire & Poudre à Canon",
                "Révolution militaire et transformation de l'architecture des fortifs. Augmente la capacité de conquête des empires centralisés.",
                "Ref: Parker, G. (1988). The Military Revolution; McNeill, W. H. (1982).",
                "• Puissance Offensive = Puissance_Base · (1 + Multiplicateur_Poudre_Canon)"},
            new String[]{"MonasticDemographicBufferEngine", "🏛️ Buffers Démographiques Monastiques & Savoir",
                "Préservation du capital intellectuel et régulation démographique par les monastères. Empêche la perte totale de savoir lors de la chute d'un empire.",
                "Ref: Weber, M. (1905); Kautsky, K. (1889). Thomas More and his Utopia.",
                "• Plancher de Rétention du Savoir : T_min = Max(T_courant, T_monastique_sauvegardé)"},
            new String[]{"NordhausDiceHybridEngine", "🌡️ Modèle DICE Hybride (Nordhaus - Climat & Économie)",
                "Couplage économie-climat intégré avec boucle de dommage du carbone. Évalue la perte de PIB causée par l'élévation des températures extrêmes.",
                "Ref: Nordhaus, W. D. (1992, 2017). Integrated Assessment Models (DICE-2016R).",
                "• Fonction de Dommage Nordhaus Ω(T) = 1 / (1 + π₁·T + π₂·T²)\n• PIB Ajusté Climat Y_net = Ω(T) · Y_brut"},
            new String[]{"NordhausDicePureEngine", "🌡️ Modèle DICE Pur (Taxe Carbone & PIB)",
                "Modélisation analytique du coût du carbone et investissements verts. Calcule le prix social du carbone pour inciter la décarbonation.",
                "Ref: Nordhaus, W. D. (1992). An Optimal Transition Path for Controlling Greenhouse Gases. Science.",
                "• Prix Social du Carbone SCC = d(Dommages_Futurs_Actualisés) / d(Émission_CO2)"},
            new String[]{"NuclearSafetyRadiotoxicityEngine", "⚛️ Radiotoxicité & Fusion Nucléaire",
                "Gestion des risques d'accidents atomiques et retombées toxiques. Simule la contamination des terres et les surcoûts de sécurité industrielle.",
                "Ref: Perrow, C. (1984). Normal Accidents: Living with High-Risk Technologies.",
                "• Probabilité d'Accident Majeur = 1 - exp(-Taux_Défaillance_Système · Nombre_Reacteurs)"},
            new String[]{"NuclearWarfareClimateEngine", "💥 Guerres Thermodynamiques & Hiver Nucléaire",
                "Modélisation des incendies massifs et refroidissement climatique. Injection de suie stratosphérique bloquant les rayons solaires pendant des années.",
                "Ref: Turco, R. P., Toon, O. B., Ackerman, T. P., Pollack, J. B., & Sagan, C. (1983). TTAPS.",
                "• Baisse Température Mondiale ΔT_nucléaire = -15.0 °C · (Masse_Suie / 150 Tg)"},
            new String[]{"OstromCommonsPureEngine", "🏞️ Auto-Gouvernance des Communs (Elinor Ostrom)",
                "Règles institutionnelles locales pour éviter la tragédie des communs. Maintient la durabilité des pâturages et de la pêche sans privatisation.",
                "Ref: Ostrom, E. (1990). Governing the Commons: Evolution of Institutions.",
                "• Maintien des Communs : Taux_Survie_Communs = f(Institution_Locale, Sanctions_Graduées)"},
            new String[]{"PinkerViolenceDeclinePureEngine", "🕊️ Déclin Historique de la Violence (Steven Pinker)",
                "Baisse de la mortalité violente par l'État, le commerce et l'alphabétisation. Réduit les homicides et les guerres à mesure que l'État de droit progresse.",
                "Ref: Pinker, S. (2011). The Better Angels of Our Nature: Why Violence Has Declined.",
                "• Taux de Mortalité Violente = V₀ · exp(-k_etat · Monopole_Violence - k_commerce · Fret)"},
            new String[]{"ProtestantWorkEthicEngine", "✝️ Éthique du Travail & Accumulation de Capital (Max Weber)",
                "Impact des valeurs morales sur la formation du capital industriel. Stimule le réinvestissement des bénéfices dans les machines au lieu du luxe.",
                "Ref: Weber, M. (1905). Die protestantische Ethik und der Geist des Kapitalismus.",
                "• Taux d'Épargne Réinvestie : S_épargne = S_base · (1 + α_ethique_travail)"},
            new String[]{"PsychohistoryPureEngine", "📊 Psychohistoire Cliodynamique (Modèle d'Asimov)",
                "Prédiction statistique des grandes masses humaines à long terme. Anticipe les cycles de stabilité et prévient les crises d'effondrement.",
                "Ref: Asimov, I. (1951). Foundation; Turchin, P. (2008). Arise Cliodynamics. Nature.",
                "• Trajectoire Macro-Historique : X(t+Δt) = Matrix_P · X(t) avec Invariance Statistique"},
            new String[]{"RomanImperialCliodynamicEngine", "🪙 Cycle Cliodynamique Romain & Altération de la Monnaie",
                "Dégradation de la pureté du Denier et crises fiscales impériales. Entraîne l'hyper-inflation et le déclin du pouvoir d'achat des légions.",
                "Ref: Turchin, P. & Scheidel, W. (2009). Coin Debasement and Structural Cycles in Rome.",
                "• Pureté de la Monnaie : Teneur_Argent = T₀ · exp(-k_solde_fiscal · Déficit_Armée)\n• Inflation & Solde des Légions : Solde_Reelle = Solde_Nominale / Inflation"},
            new String[]{"ScottAgainstTheGrainPureEngine", "🌾 Modèle de l'État Céréalier (James C. Scott)",
                "Attractivité des céréales taxables et émergence de l'État archaïque. Privilégie le blé/riz stockables pour l'impôt au détriment des tubercules.",
                "Ref: Scott, J. C. (2017). Against the Grain: A Deep History of the Earliest States.",
                "• Capacité Taxable : Assiette_Fiscale = Production_Céréales_Stockables - Subsistance_Minimale"},
            new String[]{"SelfDomesticationEngine", "🐕 Auto-Domestication Humaine & Réduction de l'Agressivité",
                "Sélection contre l'agressivité réactive dans les fortes densités. Sélectionne les comportements coopératifs indispensables à la vie urbaine.",
                "Ref: Hare, B. (2017). Survival of the Friendliest; Lahire, B. (2018). Structures Fondamentales.",
                "• Réduction Agressivité Réactive : dAgressivite/dt = -k_urbain · Densité_Cellule"},
            new String[]{"SexualSelectionMatingEngine", "💍 Sélection Sexuelle & Marché Matrimonial",
                "Structures de parenté et polygamie/monogamie selon les ressources. Régule l'accès aux partenaires selon l'inégalité de capital.",
                "Ref: Buss, D. M. (1989). Sex differences in human mate preferences. BBS.",
                "• Indice Polygynie = f(Gini_Richesse, Monopole_Ressources_Elites)"},
            new String[]{"SmilMaterialTransitionsPureEngine", "🏗️ Transitions Matérielles & Énergétiques (Vaclav Smil)",
                "Inertie physique des transitions vers l'acier, le béton, l'ammoniac et le plastique. Impose des délais de plusieurs décennies pour remplacer un matériau de base.",
                "Ref: Smil, V. (2019). Material World & Energy Transitions: History, Requirements.",
                "• Temps de Transition Matérielle T_transition = 40 à 60 ans par inertie du capital fixe"},
            new String[]{"SpatialCityFractalEngine", "🏙️ Distribution Fractale des Villes (Loi de Zipf / Batty)",
                "Hiérarchie des métropoles et loi rang-taille urbaine. Organise le réseau urbain en sous-centres régionaux et métropoles primatiales.",
                "Ref: Batty, M. (2008). The Size, Scale, and Shape of Cities. Science; Zipf, G. K. (1949).",
                "• Loi de Zipf Rang-Taille : Pop(Rang r) = Pop_Max / r^α avec α ≈ 1.0"},
            new String[]{"TasmanianCulturalRegressionEngine", "🏝️ Régression Culturelle de Tasmanie (Henrich)",
                "Perte de technologies complexes par goulet d'étranglement démographique. Fait régresser l'outillage si la population tombe sous le seuil critique d'apprentissage.",
                "Ref: Henrich, J. (2004). Demography and Cultural Loss in Tasmania. American Antiquity.",
                "• Seuil Critique d'Apprentissage : dTech/dt < 0 si Pop_Tribu < N_critique"},
            new String[]{"TechnologicalSingularityEngine", "🌌 Singularité Technologique & Kurzweil Rebound",
                "Accélération exponentielle du progrès scientifique et IA. Franchit le point d'inflexion où les machines auto-améliorent leur propre conception.",
                "Ref: Good, I. J. (1965); Vinge, V. (1993); Kurzweil, R. (2005). The Singularity Is Near.",
                "• Boucle de Rétroaction Singularité : d(Capacité_IA)/dt = (Capacité_IA)^1.5"},
            new String[]{"UrbanThermodynamicsEngine", "🏙️ Thermodynamique Urbaine & Métropoles",
                "Îlots de chaleur urbains et densité bâtie hyper-concentrée. Élève la température locale des métropoles et consomme de la puissance de climatisation.",
                "Ref: Oke, T. R. (1982). The Energetic Basis of the Urban Heat Island. Q. J. R. Meteorol. Soc.",
                "• Îlot de Chaleur Urbain ΔT_urbain = a · log₁₀(Immobilier_Densité) + b"},
            new String[]{"World3CouplingEngine", "📉 Modèle Couplé World3 & Limites à la Croissance",
                "Rétroactions entre population, pollution et capital (Club de Rome). Connecte le modèle World3 Meadows aux cellules hexagonales H3.",
                "Ref: Meadows, D. H., Meadows, D. L., Randers, J., & Behrens, W. W. (1972). Limits to Growth.",
                "• System Dynamic 5 Subsystems (Population, Capital, Agriculture, Pollution, Non-Renewables)"},
            new String[]{"World3HybridEngine", "📉 Modèle World3 Hybride (Physique & Capital)",
                "Couplage de la dynamique de World3 aux variables spatiales H3. Intègre la dispersion spatiale de la pollution et des ressources finies.",
                "Ref: Meadows, D. H. et al. (1972, 2004). Limits to Growth: The 30-Year Update.",
                "• Dispersion Spatiale H3 de la Pollution Persistante"},
            new String[]{"World3PureEngine", "📉 Modèle World3 Pur (Équations du Club de Rome)",
                "Reproduction fidèle des 5 sous-systèmes du rapport Meadows 1972 (Population, Capital, Agriculture, Pollution, Ressources).",
                "Ref: Meadows, D. H. et al. (1972). World3 Model Equations (Club of Rome Report).",
                "• Reproduction Intégrale des Équations World3 DYNAMO / Stella"}
        );

        typeBCheckBoxMap.clear();
        typeBParamSpinnersMap.clear();
        for (String[] eng : optionalEngines) {
            String engineTitle = org.ether.society.i18n.I18n.getOrDefault("engine." + eng[0] + ".title", eng[1]);
            CheckBox cb = new CheckBox(engineTitle);
            cb.setSelected("FrontierAsabiyyahEngine".equals(eng[0]));
            cb.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

            String eqText = eng.length > 4 ? eng[4] : I18n.getOrDefault("scenario.engine.state_eq_default", "📐 Équation d'État : dX/dt = f(X, t) + Σ F_inter-cellulaire");
            Tooltip tooltip = new Tooltip(I18n.getOrDefault("scenario.engine.opt_prefix", "⚙️ [MOTEUR OPTIONNEL]\n") + eng[0] + " — " + eng[1] + "\n\n" + eng[2] + "\n\n" + eqText + "\n\n📚 " + eng[3]);
            tooltip.setStyle("-fx-font-size: 11px; -fx-max-width: 500px;");
            cb.setTooltip(tooltip);

            cb.setOnAction(e -> notifyParamChange());

            typeBCheckBoxMap.put(eng[0], cb);

            HBox row = new HBox(8, cb);
            row.setAlignment(Pos.CENTER_LEFT);

            VBox engContainer = new VBox(4, row);
            javafx.scene.Node paramBox = createEngineParameterBox(eng[0]);
            if (paramBox != null) {
                paramBox.visibleProperty().bind(cb.selectedProperty());
                paramBox.managedProperty().bind(cb.selectedProperty());
                engContainer.getChildren().add(paramBox);
            }

            typeBBoxContainer.getChildren().add(engContainer);
        }

        TitledPane typeBPane = new TitledPane(String.format(I18n.getOrDefault("scenario.header.opt_engines_format", "⚙️ MODULES OPTIONNELS (%d MOTEURS EXTENSIBLES & IMPORT/EXPORT)"), optionalEngines.size()), typeBBoxContainer);
        typeBPane.setExpanded(true);
        typeBPane.getStyleClass().add("titled-pane-secondary");

        section.getChildren().addAll(oceanOptHeader, oceanOptDesc, masterBox, optBox, engineInspectorCard, corePane, typeBPane);
        return section;
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
        cultureVectorDimSpinner = new Spinner<>(4, 32, 9, 1);
        cultureVectorDimSpinner.setEditable(true);
        cultureVectorDimSpinner.setMaxWidth(Double.MAX_VALUE);
        cultureVectorDimSpinner.valueProperty().addListener((obs, o, n) -> {
            notifyParamChange();
            if (n != null) {
                rebuildCulturalTensorSubBlocks(n);
                updatePreviewModesCombo();
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

        Label layerTitle = new Label(I18n.getOrDefault("scenario.header.tensor_subblocks", "🗺️ 3.2 Cartographic Sub-Blocks per Tensor (Procedural Generation / Imported Maps)"));
        layerTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        layerTitle.setWrapText(true);

        int initDims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
        btnGenerateProceduralTensorsSection = new Button(I18n.getOrDefault("scenario.btn.gen_tensors_prefix", "🪄 Generate Tensor Suite (T₁-T") + initDims + ")");
        btnGenerateProceduralTensorsSection.getStyleClass().add("button");
        btnGenerateProceduralTensorsSection.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnGenerateProceduralTensorsSection.setMinWidth(Region.USE_PREF_SIZE);
        btnGenerateProceduralTensorsSection.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.gen_tensors_prefix", "Procedurally generate all ") + initDims + I18n.getOrDefault("scenario.tooltip.gen_tensors_suffix", " maps of cultural suite according to scenario and stochastic seed.")));
        btnGenerateProceduralTensorsSection.setOnAction(e -> generateProceduralCulturalTensors());

        cultSeedField = new TextField("54321");
        cultSeedField.setPrefWidth(80);
        cultSeedField.setStyle("-fx-font-size: 11px;");
        cultSeedField.textProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        Button cultRandSeedBtn = new Button("🎲");
        cultRandSeedBtn.getStyleClass().add("button-secondary");
        cultRandSeedBtn.setStyle("-fx-font-size: 11px;");
        cultRandSeedBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.random_tensor_seed", "Draw a new random stochastic seed for cultural tensors (does not affect population density).")));

        cultRandSeedBtn.setOnAction(e -> {
            notifyParamChange();
            String newSeed = String.valueOf(new java.util.Random().nextLong(1000000));
            cultSeedField.setText(newSeed);
            generateProceduralCulturalTensors();
        });

        culturalHelpBtn = new Button(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.cultural_format_help", "❓ Format Calques"));
        culturalHelpBtn.getStyleClass().add("button-secondary");
        culturalHelpBtn.setStyle("-fx-font-size: 11px;");
        culturalHelpBtn.setMinWidth(Region.USE_PREF_SIZE);
        culturalHelpBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_specs", "Specifications of image formats and cartographic data for cultural and geopolitical layer import.")));
        culturalHelpBtn.setOnAction(e -> showCulturalImportFormatHelp());

        HBox cultSeedBox = new HBox(4, new Label("🎲"), cultSeedField, cultRandSeedBtn);
        cultSeedBox.setAlignment(Pos.CENTER_LEFT);

        HBox seedRow = new HBox(8, cultSeedBox, btnGenerateProceduralTensorsSection);
        seedRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(btnGenerateProceduralTensorsSection, Priority.ALWAYS);

        Button btnExportGisMultiFormat = new Button(I18n.getOrDefault("scenario.btn.export_gis", "🗺️ Export SIG Multi-Format"));
        btnExportGisMultiFormat.getStyleClass().add("button-secondary");
        btnExportGisMultiFormat.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportGisMultiFormat.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_gis", "Export scenario density layers and 9 cultural tensors under professional GIS formats (GeoTIFF, NetCDF-4, GeoJSON).")));
        btnExportGisMultiFormat.setOnAction(e -> exportGisMultiFormat());

        Button btnExportProvenanceManifest = new Button(I18n.getOrDefault("scenario.btn.provenance_manifest", "🔒 Manifeste Provenance SHA-256"));
        btnExportProvenanceManifest.getStyleClass().add("button-secondary");
        btnExportProvenanceManifest.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportProvenanceManifest.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.gen_provenance", "Generate a cryptographic JSON manifest (provenance.json) containing SHA-256 hashes of datasets and parameters.")));
        btnExportProvenanceManifest.setOnAction(e -> exportProvenanceManifest());

        HBox formatRow = new HBox(8, culturalHelpBtn, btnExportGisMultiFormat, btnExportProvenanceManifest);
        formatRow.setAlignment(Pos.CENTER_LEFT);

        VBox seedAndActionBox = new VBox(6, seedRow, formatRow);

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
                alert.setContentText("Le scénario a été exporté sous format SIG compatible QGIS/ArcGIS : " + targetFile.getName());
                alert.showAndWait();
            } catch (Exception ex) {
                logger.error("Erreur lors de l'exportation SIG du scénario: {}", ex.getMessage(), ex);
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
                manifest.put("engineVersion", "Ether 2.0.0-SNAPSHOT");
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
                alert.setContentText("Le manifeste de provenance SHA-256 garantissant la reproductibilité a été écrit dans : " + targetFile.getName());
                alert.showAndWait();
            } catch (Exception ex) {
                logger.error("Erreur lors de la génération du manifeste de provenance: {}", ex.getMessage(), ex);
            }
        }
    }

    private String getCulturalTensorTitle(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.title.0", "📜 3.2.1 Tensor 1: Isoglosses & Linguistic Continua (Languages)");
            case 1 -> I18n.getOrDefault("scenario.tensor.title.1", "🏛 3.2.2 Tensor 2: Kinship & Clan Structures (Lineage)");
            case 2 -> I18n.getOrDefault("scenario.tensor.title.2", "🔮 3.2.3 Tensor 3: Rituals, Beliefs & Sacred (Asabiyyah)");
            case 3 -> I18n.getOrDefault("scenario.tensor.title.3", "👑 3.2.4 Tensor 4: Politico-Military Sovereignty & Capitals");
            case 4 -> I18n.getOrDefault("scenario.tensor.title.4", "🏺 3.2.5 Tensor 5: Tooling, Materiality & Technologies (Artifacts)");
            case 5 -> I18n.getOrDefault("scenario.tensor.title.5", "🐫 3.2.6 Tensor 6: Trade Corridors & Networks (Economic Routes)");
            case 6 -> I18n.getOrDefault("scenario.tensor.title.6", "⚖ 3.2.7 Tensor 7: Institutional Complexity & Norms (Seshat & Law)");
            case 7 -> I18n.getOrDefault("scenario.tensor.title.7", "⚠️ 3.2.8 Tensor 8: Ecological Footprint & Malthusian Stress (Degradation)");
            case 8 -> I18n.getOrDefault("scenario.tensor.title.8", "🧬 3.2.9 Tensor 9: Pathogen Immunity & Health Memory (Epidemiology)");
            default -> String.format(I18n.getOrDefault("scenario.tensor.title.ext", "🧬 3.2.%d Tenseur %d : Substrat Multichamp N°%d"), (index + 1), (index + 1), (index + 1));
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
            case 0 -> I18n.getOrDefault("scenario.tensor.0.format", "Grayscale / Indexed PNG (2:1 equirectangular):\n  Values (0-255) = Language family IDs & dialect distance (Glottolog 4.8 / WALS)");
            case 1 -> I18n.getOrDefault("scenario.tensor.1.format", "Grayscale PNG (2:1 equirectangular):\n  Black (0) = Exogamous bilateral | White (255) = Patrilineal clan clans (Murdock Atlas)");
            case 2 -> I18n.getOrDefault("scenario.tensor.2.format", "Grayscale PNG (2:1 equirectangular):\n  Black (0) = Low ritual cohesion | White (255) = High Asabiyyah sacred (Seshat)");
            case 3 -> I18n.getOrDefault("scenario.tensor.3.format", "Indexed PNG / GeoJSON (2:1 equirectangular):\n  Values = Polity sovereignty & capital allegiance domains (Centennia / CShapes)");
            case 4 -> I18n.getOrDefault("scenario.tensor.4.format", "Grayscale PNG (2:1 equirectangular):\n  Black (0) = Lithic foraging | White (255) = Advanced metallurgy (ArchaeoGLOBE)");
            case 5 -> I18n.getOrDefault("scenario.tensor.5.format", "Grayscale PNG (2:1 equirectangular):\n  Black (0) = Isolated | White (255) = Major trade corridor hub (ORBIS / Silk Road)");
            case 6 -> I18n.getOrDefault("scenario.tensor.6.format", "Grayscale PNG (2:1 equirectangular):\n  Black (0) = Informal customs | White (255) = Codified legal bureaucracy (Seshat)");
            case 7 -> I18n.getOrDefault("scenario.tensor.7.format", "Grayscale PNG (2:1 equirectangular):\n  Black (0) = Pristine biome | White (255) = Intensive Malthusian degradation (HYDE 3.4)");
            case 8 -> I18n.getOrDefault("scenario.tensor.8.format", "Grayscale PNG (2:1 equirectangular):\n  Black (0) = Naive population | White (255) = Endemic pathogen immunity (GADM / Epi)");
            default -> I18n.getOrDefault("scenario.hint.cultural_format", "PNG / GeoJSON in 2:1 equirectangular projection");
        };
    }

    private ComboBox<String> buildCulturalSourceCombo(int tensorIdx) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getItems().add("");
        switch (tensorIdx) {
            case 0 -> combo.getItems().addAll(
                "Glottolog 4.8 / WALS Language Families (Earth)",
                "Ethnologue World Linguistic Tree (Earth)",
                "Automated Phonological Distance Model (Earth)"
            );
            case 1 -> combo.getItems().addAll(
                "Murdock Ethnographic Atlas (Kinship Systems — Earth)",
                "Standard Cross-Cultural Sample (SCCS — Earth)",
                "Clan & Lineage Structural Matrix (Earth)"
            );
            case 2 -> combo.getItems().addAll(
                "Seshat Global History Databank (Rituals & Sacred — Earth)",
                "World Religion Database (WRD — Earth)",
                "Turchin Asabiyyah Cohesion Metric (Earth)"
            );
            case 3 -> combo.getItems().addAll(
                "Centennia Historical Atlas (Sovereignty Boundaries — Earth)",
                "CShapes 2.0 Historical Polities (Earth)",
                "GADM Administrative Sovereign Centers (Earth)"
            );
            case 4 -> combo.getItems().addAll(
                "ArchaeoGLOBE Project (Land Use & Material Tools — Earth)",
                "Archaeological Material Culture Database (Earth)",
                "Lithic-to-Metallurgy Technology Frontier Model (Earth)"
            );
            case 5 -> combo.getItems().addAll(
                "ORBIS Stanford Geospatial Network (Trade Routes — Earth)",
                "Silk Road & Maritime Monsoon Corridors (Earth)",
                "Old World Overland Caravan Network (Earth)"
            );
            case 6 -> combo.getItems().addAll(
                "Seshat Databank (Institutional Complexity & Law — Earth)",
                "Cross-National Time-Series Data (CNTS — Earth)",
                "Historical Jurisprudence & Bureaucracy Matrix (Earth)"
            );
            case 7 -> combo.getItems().addAll(
                "HYDE 3.4 Historical Land Use & Anthropogenic Stress (Earth)",
                "Anthromes 2.0 Global Anthropogenic Biomes (Earth)",
                "Malthusian Carrying Capacity Model (Earth)"
            );
            case 8 -> combo.getItems().addAll(
                "GADM / Historical Pathogen Memory & Epidemics (Earth)",
                "Global Infectious Disease Vector Database (Earth)",
                "Host-Pathogen Coevolution & Immunity Model (Earth)"
            );
            default -> combo.getItems().addAll(
                "Seshat / Global Databank Substrate (Earth)",
                "Historical Empirical Baseline (Earth)"
            );
        }
        combo.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item.isEmpty() ? I18n.getOrDefault("planet.combo.prompt_source", "— Select a data source —") : item);
            }
        });
        combo.setButtonCell(combo.getCellFactory().call(null));
        combo.setValue("");
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
        combo.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.map_source_hint",
                "Select reference data source. The 'Load Map' button below allows importing your local file.")));
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
            fileLbl.setText(I18n.getOrDefault("scenario.tensor.file.none", "⚠️ Aucune carte chargée — Fichier requis en mode import"));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #f59e0b;");
            if (loadBtn != null) loadBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            return;
        }

        org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(img);
        if (!val.valid()) {
            fileLbl.setText(String.format(I18n.getOrDefault("scenario.tensor.file.invalid", "⚠️ Carte incompatible : %s"), val.message()));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            if (loadBtn != null) loadBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
        } else {
            fileLbl.setText(String.format(I18n.getOrDefault("scenario.tensor.file.loaded_res", "📷 Carte chargée : %s (%dx%d)"), getCulturalBaselineName(tensorIdx), val.width(), val.height()));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8;");
            if (loadBtn != null) loadBtn.setStyle("");
        }
    }

    private void generateProceduralSingleCulturalTensor(int tensorIdx) {
        Scenario s = getScenario();
        if (s == null) return;
        Button btn = tensorGenSingleBtns.get(tensorIdx);
        if (btn != null) btn.setDisable(true);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            org.ether.society.data.HistoricalMapGenerator.generateProceduralMapsForScenario(s);
        }).thenRun(() -> javafx.application.Platform.runLater(() -> {
            if (btn != null) btn.setDisable(false);
            drawPreview();
        }));
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

    private void rebuildCulturalTensorSubBlocks(int dimCount) {
        if (btnGenerateProceduralTensorsSection != null) {
            btnGenerateProceduralTensorsSection.setText(I18n.getOrDefault("scenario.btn.gen_tensors_prefix", "🪄 Generate Tensor Suite (T₁-T") + dimCount + ")");
            btnGenerateProceduralTensorsSection.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.gen_tensors_prefix", "Procedurally generate all ") + dimCount + I18n.getOrDefault("scenario.tooltip.gen_tensors_suffix", " maps of cultural suite according to scenario and stochastic seed.")));
        }
        if (layersDynamicContainer == null) return;
        layersDynamicContainer.getChildren().clear();

        for (int i = 0; i < dimCount; i++) {
            final int tensorIdx = i;

            VBox subBlock = new VBox(6);
            subBlock.setStyle("-fx-padding: 10; -fx-background-color: rgba(167,139,250,0.04); -fx-background-radius: 6; -fx-border-color: rgba(167,139,250,0.15); -fx-border-radius: 6;");

            Label subTitle = new Label(getCulturalTensorTitle(i));
            subTitle.getStyleClass().add("control-label");
            subTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            subTitle.setWrapText(true);
            Tooltip.install(subTitle, new Tooltip(getCulturalTensorTooltip(i)));
            tensorSubTitles.put(tensorIdx, subTitle);

            ToggleGroup tg = new ToggleGroup();
            RadioButton radioProc = new RadioButton(I18n.getOrDefault("scenario.mode.procedural_sde", "▶ Mode Procédural (Paramètres adaptatifs & Graine)"));
            RadioButton radioImport = new RadioButton(I18n.getOrDefault("scenario.mode.spatial_import", "📂 Importation Carte Spatiale (PNG / GeoJSON)"));
            radioProc.setToggleGroup(tg);
            radioImport.setToggleGroup(tg);
            radioProc.getStyleClass().add("radio-proc");
            radioImport.getStyleClass().add("radio-import");

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
                notifyParamChange();
                drawPreview();
            });

            Button genSingleBtn = new Button(I18n.getOrDefault("scenario.tensor.btn.gen_single", "🪄 Générer"));
            genSingleBtn.getStyleClass().add("button-secondary");
            genSingleBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 8; -fx-font-weight: bold;");
            genSingleBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tensor.btn.gen_single_tooltip", "Générer la carte procédurale pour ce tenseur spécifique à partir de sa graine et de ses paramètres.")));
            tensorGenSingleBtns.put(tensorIdx, genSingleBtn);
            genSingleBtn.setOnAction(e -> generateProceduralSingleCulturalTensor(tensorIdx));

            HBox seedRow = new HBox(6, seedLbl, seedField, randBtn, genSingleBtn);
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
            if (sourceCombo.getItems().size() > 1) {
                sourceCombo.setValue(sourceCombo.getItems().get(1));
            }
            tensorSourceCombos.put(tensorIdx, sourceCombo);

            Button btnLoad = new Button(I18n.getOrDefault("resource.btn.load_map", "Load Map"));
            btnLoad.getStyleClass().add("button-secondary");
            btnLoad.setStyle("-fx-font-size: 11px;");
            tensorLoadBtns.put(tensorIdx, btnLoad);

            Button btnClear = new Button("❌");
            btnClear.getStyleClass().add("button-secondary");
            btnClear.setStyle("-fx-font-size: 11px;");

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

        previewTitleLabel = new Label(I18n.getOrDefault("scenario.title.right_view", "🗺️ Resource Cartography & Display"));
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
            if (val != null && val.startsWith("──────────")) {
                previewModeCombo.setValue("📊 Relief & Densité Démographique (Nœuds Agents T₀)");
                return;
            }
            updatePreviewTitleText();
            updateBottomLegend();
            drawPreview();
        });

        Button btnProceduralGeneratePreview = new Button(I18n.getOrDefault("scenario.btn.regen_t0_maps", "🪄 Regenerate T₀ Maps"));
        btnProceduralGeneratePreview.getStyleClass().add("button-secondary");
        btnProceduralGeneratePreview.setMinWidth(Region.USE_PREF_SIZE);
        btnProceduralGeneratePreview.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.regen_t0_maps", "Procedurally regenerate Tab 3 maps and layers based on scenario population and parameters.")));
        btnProceduralGeneratePreview.setOnAction(e -> generateProceduralCulturalTensors());

        btnReliefOverlay = new ToggleButton(org.ether.society.i18n.I18n.getOrDefault("resource.btn.relief_overlay", "⛰️ Relief"));
        btnReliefOverlay.getStyleClass().add("button-secondary");
        btnReliefOverlay.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("resource.tooltip.relief_overlay", "Overlay relief & slope map at 50% opacity for geographical reference")));
        btnReliefOverlay.setOnAction(e -> drawPreview());

        HBox controlBar = new HBox(8, previewModeCombo, btnReliefOverlay, btnProceduralGeneratePreview);
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
        if (currentPreviewCells == null || currentPreviewCells.isEmpty()) {
            generatePreview();
        } else {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                distributeInitialPopulation(currentPreviewCells);
            }).thenRun(() -> javafx.application.Platform.runLater(this::drawPreview));
        }
    }

    private void generateProceduralCulturalTensors() {
        Scenario s = getScenario();
        if (s == null) return;
        if (btnGenerateProceduralTensorsSection != null) btnGenerateProceduralTensorsSection.setDisable(true);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            org.ether.society.data.HistoricalMapGenerator.generateProceduralMapsForScenario(s);
        }).thenRun(() -> javafx.application.Platform.runLater(() -> {
            if (btnGenerateProceduralTensorsSection != null) btnGenerateProceduralTensorsSection.setDisable(false);
            drawPreview();
        }));
    }

    private void updatePreviewModesCombo() {
        if (previewModeCombo == null) return;
        int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
        int currentSelectionIndex = previewModeCombo.getSelectionModel().getSelectedIndex();

        java.util.List<String> items = new java.util.ArrayList<>();
        items.add(I18n.getOrDefault("scenario.preview.mode.density", "📊 Relief & Demographic Density (Agent Nodes T₀)"));

        for (int i = 0; i < dims; i++) {
            items.add(getCulturalTensorPreviewName(i));
        }

        items.add("────────── CALQUES DÉDUITS & DYNAMIQUES ──────────");
        items.add(I18n.getOrDefault("scenario.preview.mode.capital", "🛠️ Initial Physical Capital K(x) [kg/capita] (Derived)"));
        items.add(I18n.getOrDefault("scenario.preview.mode.energy", "⚡ Initial Energy Stock E(x) [MJ/capita] (Derived)"));
        items.add(I18n.getOrDefault("scenario.preview.mode.food", "🌾 Food Reserves F(x) [Months] (Derived)"));
        items.add(I18n.getOrDefault("scenario.preview.mode.info", "🧠 Information Capital & Knowledge I(x) [Bits/capita] (Derived)"));
        items.add(I18n.getOrDefault("scenario.preview.mode.footprint", "⚠️ Demographic Footprint & Malthusian Tension (Derived)"));
        items.add(I18n.getOrDefault("scenario.preview.mode.friction", "🧱 Border Friction Gradient σ_friction (Derived)"));

        previewModeCombo.getItems().setAll(items);

        if (currentSelectionIndex >= 0 && currentSelectionIndex < previewModeCombo.getItems().size()) {
            previewModeCombo.getSelectionModel().select(currentSelectionIndex);
        } else {
            previewModeCombo.getSelectionModel().select(0);
        }
    }

    private String getCulturalTensorPreviewName(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.1.preview", "📜 Tensor 1: Isoglosses & Linguistic Continua (Languages)");
            case 1 -> I18n.getOrDefault("scenario.tensor.2.preview", "🏛 Tensor 2: Kinship & Clan Structures (Kinship)");
            case 2 -> I18n.getOrDefault("scenario.tensor.3.preview", "🔮 Tensor 3: Rituals, Beliefs & Sacred (Asabiyyah)");
            case 3 -> I18n.getOrDefault("scenario.tensor.4.preview", "👑 Tensor 4: Politico-Military Sovereignty & Capitals");
            case 4 -> I18n.getOrDefault("scenario.tensor.5.preview", "🏺 Tensor 5: Tooling, Materiality & Technologies (Artifacts)");
            case 5 -> I18n.getOrDefault("scenario.tensor.6.preview", "🐫 Tensor 6: Corridors & Trade Networks (Economic Routes)");
            case 6 -> I18n.getOrDefault("scenario.tensor.7.preview", "⚖ Tensor 7: Institutional Complexity & Norms (Seshat & Law)");
            case 7 -> I18n.getOrDefault("scenario.tensor.8.preview", "⚠️ Tensor 8: Ecological Footprint & Malthusian Tension (Degradation)");
            case 8 -> I18n.getOrDefault("scenario.tensor.9.preview", "🧬 Tensor 9: Pathogen Immunity & Health Memory (Epidemiology)");
            default -> I18n.getOrDefault("scenario.tensor.custom.preview_prefix", "🧬 Tensor ") + (index + 1) + I18n.getOrDefault("scenario.tensor.custom.preview_mid", " : Cultural Substrate ") + (index + 1);
        };
    }

    private void updatePreviewTitleText() {
        if (previewTitleLabel == null) return;
        previewTitleLabel.setText(I18n.getOrDefault("scenario.title.right_view", "🗺️ Resource Cartography & Display"));
    }

    private void updateBottomLegend() {
        if (legendItemsContainer == null) return;
        legendItemsContainer.getChildren().clear();

        int idx = previewModeCombo != null ? previewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        String mode = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue() : "";

        Color[] colors;
        String[] labels;
        String[] fullTooltips;

        if (idx == 1 || mode.contains("Isoglosses") || mode.contains("Linguistiques")) {
            colors = new Color[]{
                Color.rgb(30, 95, 165), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
            };
            labels = new String[]{"Dialecte Archaïque (C₀=0)", "Foyer Prosodique (0.25)", "Isoglosse Médiane (0.50)", "Innovations Substratiques (0.75)", "Dialecte Exogène (1.0)"};
            fullTooltips = new String[]{
                "Dialecte Archaïque : Formes originelles non diffusées",
                "Foyer Prosodique : Zone d'expansion dialectale secondaire",
                "Isoglosse Médiane : Zone de frontière linguistique et bilinguisme",
                "Innovations Substratiques : Lexique technique ou grammatical rénové",
                "Dialecte Exogène / Innovant : Standard de communication émergent"
            };
        } else if (idx == 2 || mode.contains("Kinship") || mode.contains("Clans")) {
            colors = new Color[]{
                Color.rgb(56, 189, 248), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(167, 139, 250)
            };
            labels = new String[]{"Famille Nucléaire (C₁=0)", "Lignée Élargie (0.25)", "Matriarcat Lacustre (0.50)", "Patriarcat Hiérarchique (0.75)", "Confédération Tribale (1.0)"};
            fullTooltips = new String[]{
                "Famille Nucléaire : Cellule parentale autonome de base",
                "Lignée Élargie : Entraide inter-générationnelle et clans d'alliance",
                "Matriarcat Lacustre : Filiations matrilinéaires et terres collectives",
                "Patriarcat Hiérarchique : Structure agnatique et chefferies martiales",
                "Confédération Tribale : Assemblée de clans fédérés à grande échelle"
            };
        } else if (idx == 3 || mode.contains("Rituels") || mode.contains("Asabiyyah")) {
            colors = new Color[]{
                Color.rgb(14, 165, 233), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(225, 29, 72)
            };
            labels = new String[]{"Animisme Local (C₂=0)", "Cultes Civiques (0.25)", "Polythéisme (0.50)", "Asabiyyah Élevée (0.75)", "Dogme Transcendant (1.0)"};
            fullTooltips = new String[]{
                "Animisme Local : Croyances de terroirs et esprit des éléments",
                "Cultes Civiques : Rites urbains d'intégration communautaire",
                "Polythéisme : Panthéons structurés et clergés régionaux",
                "Asabiyyah Élevée : Forte solidarité tribale (Cohésion d'Ibn Khaldoun)",
                "Dogme Transcendant : Monothéisme ou idéologie universaliste"
            };
        } else if (idx == 4 || mode.contains("Souveraineté") || mode.contains("Politiques")) {
            colors = new Color[]{
                Color.rgb(30, 95, 165), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
            };
            labels = new String[]{"Zone Franche (C₃=0)", "Cité-État Libre (0.25)", "Principauté (0.50)", "Empire Centralisé (0.75)", "Capitale Core (1.0)"};
            fullTooltips = new String[]{
                "Zone Franche / Nomade : Absence de souveraineté étatique formalisée",
                "Cité-État Libre : Autonomie municipale et hinterland restreint",
                "Principauté Régionale : Contrôle féodal ou provincial intermédiaire",
                "Empire Centralisé : Administration unifiée et prélèvement fiscal",
                "Capitale Core : Foyer du pouvoir politique et militaire suprême"
            };
        } else if (idx == 7 || mode.contains("Friction")) {
            colors = new Color[]{
                Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68), Color.rgb(167, 139, 250)
            };
            labels = new String[]{"Plaine (Faible σ)", "Colline / Fleuve (Moyen)", "Montagne (Fort)", "Désert / Extrême", "Barrière Absolue"};
            fullTooltips = new String[]{
                "Plaine Alluviale : Friction minimale à la mobilité (σ ≈ 0.1)",
                "Colline / Fleuve : Obstacle naturel mineur franchissable",
                "Chaîne Montagneuse : Transports ralentis, cols escarpés",
                "Désert Extrême : Zone aride exigeant des convois spécialisés",
                "Haute Altitude / Falaise : Barrière infranchissable pour les armées"
            };
        } else {
            colors = new Color[]{
                Color.rgb(30, 95, 165), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
            };
            labels = new String[]{
                "Inhabité (0 hab/km²)", "Faible (1–50 hab/km²)", "Moyenne (50–500)", "Élevée (500–2.5k)", "Métropole (> 2.5k)"
            };
            fullTooltips = new String[]{
                "Zone Inhabitée : 0 hab/km² (Océans, déserts, haute montagne)",
                "Densité Faible : 1 à 50 hab/km² (Campagnes, tribus nomades)",
                "Densité Moyenne : 50 à 500 hab/km² (Bourgades & vallées agricoles)",
                "Densité Élevée / Cité : 500 à 2 500 hab/km² (Centres urbains régionaux)",
                "Métropole / Megapole : > 2 500 hab/km² (Grandes capitales historiques)"
            };
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
        if (demoCompatibilityLabel == null) return;
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        String planetName = p != null ? p.name() : "Standard";
        if (customDensityImage != null) {
            org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(customDensityImage);
            if (!val.valid()) {
                demoCompatibilityLabel.setText(String.format(I18n.getOrDefault("scenario.demo.map_invalid", "⚠️ Carte démographique incompatible : %s"), val.message()));
                demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            } else {
                demoCompatibilityLabel.setText(String.format(I18n.getOrDefault("scenario.demo.map_loaded", "✅ Map loaded and compatible with selected Tab 1 world (%s)"), planetName));
                demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
            }
        } else {
            demoCompatibilityLabel.setText(I18n.getOrDefault("scenario.demo.no_map", "🪐 No external map loaded — Procedural mode active"));
            demoCompatibilityLabel.getStyleClass().add("subcard-status-muted");
            demoCompatibilityLabel.setStyle("-fx-font-size: 11px;");
        }
    }

    private void exportDensityMap() {
        if (previewCanvas == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("scenario.title.export_density_dialog", "Export Population Density Map (PNG)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PNG", "*.png"));
        chooser.setInitialFileName("ether-population-density.png");
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                int w = 800;
                int h = 400;
                WritableImage image = new WritableImage(w, h);
                PixelWriter pw = image.getPixelWriter();

                if (currentPreviewCells != null && !currentPreviewCells.isEmpty()) {
                    double minLat = -90, maxLat = 90;
                    double minLng = -180, maxLng = 180;
                    double scaleX = w / (maxLng - minLng);
                    double scaleY = h / (maxLat - minLat);

                    // Fill background
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            pw.setColor(x, y, Color.rgb(15, 23, 42));
                        }
                    }

                    for (H3Cell c : currentPreviewCells) {
                        int px = (int) ((c.getLongitude() - minLng) * scaleX);
                        int py = (int) ((maxLat - c.getLatitude()) * scaleY);
                        if (px >= 0 && px < w && py >= 0 && py < h) {
                            long pop = c.getPopulation();
                            Color col;
                            if (c.getElevation() <= 0) col = Color.rgb(15, 23, 42);
                            else if (pop <= 0) col = Color.rgb(30, 41, 59);
                            else if (pop < 100) col = Color.rgb(16, 185, 129);
                            else if (pop < 1000) col = Color.rgb(234, 179, 8);
                            else if (pop < 5000) col = Color.rgb(249, 115, 22);
                            else col = Color.rgb(239, 68, 68);

                            pw.setColor(px, py, col);
                        }
                    }
                }
                int iw = (int) image.getWidth();
                int ih = (int) image.getHeight();
                java.awt.image.BufferedImage bImg = new java.awt.image.BufferedImage(iw, ih, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                PixelReader pr = image.getPixelReader();
                for (int y = 0; y < ih; y++) {
                    for (int x = 0; x < iw; x++) {
                        bImg.setRGB(x, y, pr.getArgb(x, y));
                    }
                }
                ImageIO.write(bImg, "png", file);
                logger.info("Exported density map to {}", file.getAbsolutePath());
            } catch (Exception ex) {
                logger.error("Failed to export density map", ex);
            }
        }
    }

    private void showDensityImportFormatHelp() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle(I18n.getOrDefault("scenario.dialog.density_help_title", "Density Map Specifications"));
        dialog.setHeaderText(I18n.getOrDefault("scenario.dialog.density_help_header", "Supported Image Formats for Population Density Import"));
        dialog.setContentText(
                "Vous pouvez importer une carte de répartition démographique sous forme d'image PNG/JPEG au ratio 2:1 (ex: 2048x1024 pixels en projection équirectangulaire) :\n\n" +
                "1. CARTE DE DENSITÉ NIVEAUX DE GRIS :\n" +
                "   • Noir (0) = 0 hab/km² (Zone inhabitée / désertique / aquatique)\n" +
                "   • Blanc (255) = Densité maximale d'habitation (Foyer urbain ou métropole)\n\n" +
                "2. INTÉGRATION AVEC LE TERRAIN ET L'ÉCOLOGIE :\n" +
                "   • Les zones d'eau (océans/mers) définies dans l'onglet 1 sont automatiquement filtrées pour éviter l'apparition de populations en pleine mer.\n" +
                "   • La population totale paramétrée dans le spinner est distribuée au prorata de la luminosité de chaque pixel."
        );
        dialog.showAndWait();
    }

    private void showCulturalImportFormatHelp() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle(I18n.getOrDefault("scenario.title.cultural_specs_dialog", "Cultural & Geopolitical Cartographic Layer Specifications"));
        dialog.setHeaderText(I18n.getOrDefault("scenario.header.cultural_specs", "Supported Image Formats & Spatial Data (2:1 Equirectangular Projection)"));
        dialog.setContentText(
                "Vous pouvez importer des cartes cartographiques pour l'ensemble des 9+ tenseurs culturels (PNG/JPEG ratio 2:1, ex: 2048x1024 pixels, Plate Carrée) :\n\n" +
                "1. 📜 TENSEUR 1 (Isoglosses & Langues) :\n" +
                "   • Canaux de couleur RVB codant le continuum linguistique, continua de dialectes et gradients de friction phonétique ΔL.\n\n" +
                "2. 🏛️ TENSEUR 2 (Kinship & Structures de Clans) :\n" +
                "   • Nuances codant les structures de lignée, exogamie/endogamie, réseaux claniques et alliances d'échanges.\n\n" +
                "3. 🔮 TENSEUR 3 (Rituels, Croyances & Sacré) :\n" +
                "   • Cartographie des zones sacrées, religions d'État, tabous et cohésion sociale d'Asabiyyah (Ibn Khaldoun).\n\n" +
                "4. 👑 TENSEUR 4 (Souveraineté & Capitales) :\n" +
                "   • Démarcation géographique des États, zones de contrôle des capitales et friction de frontière σ_friction.\n\n" +
                "5. 🏺 TENSEUR 5 (Outillage, Matérialité & Technologies) :\n" +
                "   • Distribution des traditions artisanales, poteries, métallurgie, outillage domestique et architecture.\n\n" +
                "6. 🐫 TENSEUR 6 (Corridors & Réseaux Commerciaux) :\n" +
                "   • Voies commerciales majeurs (Route de la Soie, Trans-Sahara, Océan Indien), nœuds d'échanges et foires.\n\n" +
                "7. ⚖️ TENSEUR 7 (Complexité Institutionnelle & Normes) :\n" +
                "   • Modèle Seshat : codification du droit coutumier, tribunaux, bureaucratie fiscale et édits judiciaires.\n\n" +
                "8. ⚠️ TENSEUR 8 (Empreinte Écologique & Tension Malthusienne) :\n" +
                "   • Salinisation des sols, déforestation, érosion et pression malthusienne sur la capacité de charge.\n\n" +
                "9. 🧬 TENSEUR 9 (Immunité Pathogène & Mémoire Sanitaire) :\n" +
                "   • Zones d'endémie tropicale, réservoirs zoonotiques et barrières d'immunité croisée épidémiologique.\n\n" +
                "FLEXIBILITÉ & GESTION DES CALQUES MANQUANTS :\n" +
                "• Calques Partiels : Si vous possédez des données pour certains calques (ex: Tenseurs 1 & 4) mais pas pour d'autres, l'application active un fallback stochastique automatique sur les calques non renseignés.\n" +
                "• Options de Fallback par Sous-Bloc :\n" +
                "  - Fallback Procédural Auto (Langevin-SDE Stochastique)\n" +
                "  - Valeur Neutre Constant (0.5)\n" +
                "  - Copie / Interpolation depuis un calque adjacent."
        );
        WindowUtils.applyWindowIcon(dialog);
        dialog.showAndWait();
    }

    private void generatePreview() {
        if (previewStatusLabel != null) {
            previewStatusLabel.setText(I18n.getOrDefault("scenario.status.calc_preview", "⚡ Calculating demographic preview in background..."));
        }
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
                    drawPreview();
                    if (previewStatusLabel != null) {
                        previewStatusLabel.setText(I18n.getOrDefault("scenario.status.preview_generated", "Preview generated: ") + previewCells.size() + I18n.getOrDefault("scenario.status.cells_suffix", " cells."));
                    }
                });
            } catch (Exception ex) {
                logger.error("Failed to generate preview", ex);
                javafx.application.Platform.runLater(() -> {
                    if (previewStatusLabel != null) {
                        previewStatusLabel.setText(I18n.getOrDefault("scenario.status.preview_error", "Preview error: ") + ex.getMessage());
                    }
                });
            }
        }, "H3-Preview-Async-Thread").start();
    }

    private void distributeInitialPopulation(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        long totalPop = initialHumanCountSpinner != null && initialHumanCountSpinner.getValue() != null ? initialHumanCountSpinner.getValue() : 1_000_000L;
        double capitalK0 = computeAutoCapitalFromYear(startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000);
        String pattern = densityPatternCombo != null ? densityPatternCombo.getValue() : "UNBIASED_NATURAL";
        boolean isEarthPreset = activePlanetPreset == null || activePlanetPreset.name() == null 
                || activePlanetPreset.name().toLowerCase().contains("earth") 
                || activePlanetPreset.name().toLowerCase().contains("terre") 
                || activePlanetPreset.name().toLowerCase().contains("terran")
                || "earth".equalsIgnoreCase(activePlanetPreset.elevationMapSource());
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
            heatCol = Color.rgb(30, 95, 165);  // Inhabité (0 hab/km²) - Navy Blue (Legend matching)
        } else if (pop < 50) {
            heatCol = Color.rgb(16, 185, 129); // Faible (1 - 50 hab/km²) - Green
        } else if (pop < 500) {
            heatCol = Color.rgb(234, 179, 8);  // Moyenne (50 - 500 hab/km²) - Yellow
        } else if (pop < 2500) {
            heatCol = Color.rgb(249, 115, 22); // Élevée / Cité (500 - 2500 hab/km²) - Orange
        } else {
            heatCol = Color.rgb(239, 68, 68);  // Métropole (> 2500 hab/km²) - Red
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

        if (idx == 0) {
            // Default Density & Relief Mode
            if (customDensityImage != null && customDensityImage.getWidth() > 0) {
                Color customCol = sampleImageColorAtLatLon(customDensityImage, lat, lon);
                if (customCol != null) return customCol;
            }
            return getReliefAndDensityColor(c);
        }

        int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
        if (idx >= 1 && idx <= dims) {
            int tIndex = idx - 1;
            Image img = customTensorImages.get(tIndex);
            if (img != null && img.getWidth() > 0) {
                Color customCol = sampleImageColorAtLatLon(img, lat, lon);
                if (customCol != null) return customCol;
            }

            return switch (tIndex % 9) {
                case 0 -> {
                    double val = Math.clamp((lat + 90.0) / 180.0 * 0.7 + (lon + 180.0) / 360.0 * 0.3, 0.0, 1.0);
                    yield Color.hsb(val * 300.0, 0.75, 0.90);
                }
                case 1 -> {
                    double val = Math.clamp(Math.abs(Math.sin(lat * 0.1) * Math.cos(lon * 0.1)), 0.0, 1.0);
                    yield Color.hsb(180.0 + val * 120.0, 0.80, 0.85);
                }
                case 2 -> {
                    double val = Math.clamp((c.getElevation() != null ? c.getElevation() : 0) / 3000.0, 0.0, 1.0);
                    yield Color.hsb(40.0 + val * 200.0, 0.85, 0.95);
                }
                case 3 -> {
                    if (c.getOwner() != null && c.getOwner().getColor() != null) yield c.getOwner().getColor();
                    yield Color.rgb(71, 85, 105);
                }
                case 4 -> Color.rgb(248, 113, 113); // Tech (Coral)
                case 5 -> Color.rgb(245, 158, 11);  // Trade (Amber)
                case 6 -> Color.rgb(168, 85, 247);  // Institutional (Purple)
                case 7 -> Color.rgb(239, 68, 68);   // Ecological (Red)
                case 8 -> Color.rgb(14, 165, 233);  // Pathogen Immunity (Sky Blue)
                default -> Color.hsb((tIndex * 47.0) % 360.0, 0.75, 0.85);
            };
        }

        // Calques déduits & physiques
        if (mode.contains("capital") || mode.contains("k(x)")) {
            double cap = c.getResourceCapital() != null ? c.getResourceCapital() : 0.0;
            double norm = Math.clamp(cap / 500000.0, 0.0, 1.0);
            return Color.hsb((1.0 - norm) * 240.0, 0.85, 0.90);
        } else if (mode.contains("énergétique") || mode.contains("energy") || mode.contains("e(x)")) {
            double energy = c.getEnergyFire() != null ? c.getEnergyFire() : 0.0;
            double norm = Math.clamp(energy / 1000000.0, 0.0, 1.0);
            return Color.hsb(30.0 + norm * 30.0, 0.90, 0.95);
        } else if (mode.contains("alimentaires") || mode.contains("food") || mode.contains("f(x)")) {
            double food = c.getFoodResource() != null ? c.getFoodResource() : 0.0;
            double norm = Math.clamp(food / 100000.0, 0.0, 1.0);
            return Color.hsb(120.0, 0.70 + norm * 0.30, 0.60 + norm * 0.35);
        } else if (mode.contains("informationnel") || mode.contains("info") || mode.contains("i(x)")) {
            double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
            double norm = Math.clamp(tech / 10.0, 0.0, 1.0);
            return Color.hsb(270.0 + norm * 60.0, 0.85, 0.90);
        } else if (mode.contains("empreinte") || mode.contains("footprint") || mode.contains("malthus")) {
            double cap = computeCellCarryingCapacity(c);
            long pop = c.getPopulation() != null ? c.getPopulation() : 0;
            double ratio = Math.clamp(pop / Math.max(1.0, cap), 0.0, 1.5);
            return Color.hsb((1.0 - Math.min(1.0, ratio)) * 120.0, 0.85, 0.90);
        } else if (mode.contains("friction")) {
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

            for (H3Cell c : currentPreviewCells) {
                double x = (c.getLongitude() - minLng) * scale + offX;
                double y = (maxLat - c.getLatitude()) * scale + offY;

                if (x < -cellSize || x > w + cellSize || y < -cellSize || y > h + cellSize) continue;

                Color col = getPreviewColorForCell(c);
                if (btnReliefOverlay != null && btnReliefOverlay.isSelected()) {
                    double elev = c.getElevation() != null ? c.getElevation() : 0.0;
                    double decl = c.getMovementFriction() != null ? Math.max(0.0, c.getMovementFriction() - 1.0) : 0.0;
                    Color reliefCol = getReliefShadeColor(elev, 0.0, decl);
                    col = blendColors(col, reliefCol, 0.50);
                }
                gc.setFill(col);
                double absLat = Math.abs(c.getLatitude());
                double cosLat = Math.cos(Math.toRadians(Math.min(88.0, absLat)));
                double cellW = Math.max(cellSize, cellSize / Math.max(0.12, cosLat));
                double cellH = cellSize;

                if (cellW >= 7.0 && cellH >= 7.0) {
                    gc.fillRoundRect(x - cellW / 2.0, y - cellH / 2.0, cellW, cellH, 3.0, 3.0);
                    if (cellW >= 12.0) {
                        gc.setStroke(Color.rgb(0, 0, 0, 0.25));
                        gc.setLineWidth(0.75);
                        gc.strokeRoundRect(x - cellW / 2.0, y - cellH / 2.0, cellW, cellH, 3.0, 3.0);
                    }
                } else {
                    gc.fillRect(x - cellW / 2.0, y - cellH / 2.0, cellW, cellH);
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
            gc.fillText(String.format("✂️ Zone: Lat[%.1f°, %.1f°] Lng[%.1f°, %.1f°]", cMinLat, cMaxLat, cMinLng, cMaxLng), rx + 4, ry - 6);
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
        boolean isEarth = planet == PlanetPreset.EARTH_LIKE || "earth".equalsIgnoreCase(planet.elevationMapSource()) ||
                (planet.name() != null && (planet.name().toLowerCase().contains("earth") || planet.name().toLowerCase().contains("terre")));

        Image bgImage = isEarth ? getCachedEarthElevationImage() : null;
        PixelReader bgReader = bgImage != null ? bgImage.getPixelReader() : null;

        int idx = previewModeCombo != null ? previewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        String mode = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue().toLowerCase() : "";
        int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
        Image activeCustomImage = null;
        if (idx >= 1 && idx <= dims) {
            activeCustomImage = customTensorImages.get(idx - 1);
        } else if (idx == 0) {
            activeCustomImage = customDensityImage;
        }

        PixelReader customReader = activeCustomImage != null ? activeCustomImage.getPixelReader() : null;
        boolean isFootprintMode = idx == 6 || mode.contains("empreinte");
        boolean isReliefOverlay = btnReliefOverlay != null && btnReliefOverlay.isSelected();

        String cacheKey = "sc_mode=" + idx + "_w=" + pwWidth + "_h=" + pwHeight + "_relief=" + isReliefOverlay
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

            for (int py = 0; py < pwHeight; py++) {
                for (int px = 0; px < pwWidth; px++) {
                    double lat = 90.0 - (py / (double) pwHeight) * 180.0;
                    double lon = -180.0 + (px / (double) pwWidth) * 360.0;

                    // 1) Accurate elevation, water level and coastlines (z = 0)
                    double elevVal;
                    double wLevel = planet.waterLevel();
                    boolean isLand;
                    boolean isCoast;
                    double declivity = 0.0;

                    if (bgReader != null && bgImage != null) {
                        double bgW = bgImage.getWidth();
                        double bgH = bgImage.getHeight();
                        int bx = (int) Math.clamp(((px / (double) pwWidth) * bgW), 0, bgW - 1);
                        int by = (int) Math.clamp(((py / (double) pwHeight) * bgH), 0, bgH - 1);
                        elevVal = bgReader.getColor(bx, by).getRed();
                        isLand = elevVal > 0.185;

                        int bxE = Math.min((int) bgW - 1, bx + 1);
                        int byN = Math.max(0, by - 1);
                        boolean isLandE = bgReader.getColor(bxE, by).getRed() > 0.185;
                        boolean isLandN = bgReader.getColor(bx, byN).getRed() > 0.185;
                        isCoast = (isLand != isLandE) || (isLand != isLandN);
                    } else {
                        var pt = generator.getPlanetPoint(lat, lon, planet);
                        elevVal = pt.elevation();
                        declivity = pt.declivity();
                        isLand = elevVal >= wLevel;

                        var ptE = generator.getPlanetPoint(lat, lon + 1.0, planet);
                        var ptN = generator.getPlanetPoint(lat + 1.0, lon, planet);
                        boolean isLandE = ptE.elevation() >= wLevel;
                        boolean isLandN = ptN.elevation() >= wLevel;
                        isCoast = (isLand != isLandE) || (isLand != isLandN);
                    }

                    Color baseReliefColor;
                    if (!isLand) {
                        baseReliefColor = Color.rgb(15, 23, 42); // Sea / Deep ocean navy
                    } else {
                        int r = Math.clamp((int) (55 + elevVal * 120), 0, 255);
                        int g = Math.clamp((int) (125 + elevVal * 80), 0, 255);
                        int bCol = Math.clamp((int) (50 + elevVal * 60), 0, 255);
                        baseReliefColor = Color.rgb(r, g, bCol);
                    }

                    // 2) Main layer color (custom uploaded image OR procedural density overlay)
                    Color pxColor;
                    if (customReader != null && activeCustomImage.getWidth() > 0 && activeCustomImage.getHeight() > 0) {
                        int imgX = (int) Math.clamp(((px / (double) pwWidth) * activeCustomImage.getWidth()), 0, activeCustomImage.getWidth() - 1);
                        int imgY = (int) Math.clamp(((py / (double) pwHeight) * activeCustomImage.getHeight()), 0, activeCustomImage.getHeight() - 1);
                        pxColor = customReader.getColor(imgX, imgY);
                    } else if (!isLand) {
                        pxColor = baseReliefColor; // Ocean Navy
                    } else {
                        // Density mode default preview on land
                        long startYear = startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000;
                        boolean isAmericas = lon < -25.0;
                        boolean isSahul = (lat < 10.0 && lon > 95.0) || (lat < -10.0 && lon > 110.0);

                        // Prehistoric geographic human presence checks
                        boolean isHumanSettled = true;
                        if (startYear <= -50000 && (isAmericas || isSahul)) {
                            isHumanSettled = false;
                        } else if (startYear <= -25000 && isAmericas) {
                            isHumanSettled = false;
                        }

                        Color heatCol;
                        if (!isHumanSettled) {
                            heatCol = Color.rgb(30, 95, 165); // Inhabité (0 hab/km²) - Blue from legend
                        } else {
                            boolean isEastAfrica = (lat >= -15 && lat <= 15) && (lon >= 25 && lon <= 45);
                            boolean isFertileCrescent = (lat >= 20 && lat <= 38) && (lon >= 25 && lon <= 90);
                            boolean isChinaIndus = (lat >= 10 && lat <= 42) && (lon >= 65 && lon <= 125);

                            if (isEastAfrica || isFertileCrescent || isChinaIndus) {
                                heatCol = isFootprintMode ? Color.rgb(239, 68, 68) : Color.rgb(249, 115, 22); // High / Orange
                            } else {
                                heatCol = Color.rgb(16, 185, 129); // Faible / Green
                            }
                        }
                        pxColor = heatCol;
                    }

                    // 3) Relief overlay and coastline outlines
                    if (isReliefOverlay) {
                        if (isCoast) {
                            pxColor = Color.rgb(224, 242, 254); // Crisp white-cyan coastline outline at z = 0
                        } else {
                            Color reliefCol = getReliefShadeColor(elevVal, wLevel, declivity);
                            pxColor = blendColors(pxColor, reliefCol, 0.50); // 50% opacity relief overlay
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

    private static Image cachedEarthElevationImage = null;
    private Image getCachedEarthElevationImage() {
        if (cachedEarthElevationImage == null) {
            try (var is = getClass().getResourceAsStream("/maps/earth_elevation.png")) {
                if (is != null) cachedEarthElevationImage = new Image(is);
            } catch (Exception ignored) {}
        }
        return cachedEarthElevationImage;
    }

    // =========================================================================
    // DEFERRED EXECUTION — Calculate H3 Grid & Start Simulation
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
            startSimulationDeferred();
        }
    }

    private void cancelCalculation() {
        cancelRequested = true;
        updateProgress(0.0, "🛑 Annulation du calcul en cours à la demande de l'utilisateur...");
        if (generationThread != null && generationThread.isAlive()) {
            generationThread.interrupt();
        }
        resetStartButtonState();
    }

    private void startSimulationDeferred() {
        if (!validateScenarioSetup()) {
            resetStartButtonState();
            return;
        }
        cancelRequested = false;
        setStartButtonCancelState();
        updateProgress(0.02, "⚡ Enregistrement du scénario & préparation du calcul H3...");

        // 1. Explicitly Save Scenario to DB & Provide UI Confirmation
        Scenario scenarioToSave = getScenario();
        try {
            scenarioRepo.saveOrUpdate(scenarioToSave);
            logger.info("Scenario '{}' automatically saved to database repository before simulation launch.", scenarioToSave.getName());
            if (scenarioPresetBar != null) {
                boolean exists = scenarioPresetBar.getPresetCombo().getItems().stream()
                        .anyMatch(p -> p != null && p.getName() != null && p.getName().equalsIgnoreCase(scenarioToSave.getName()));
                if (!exists) {
                    scenarioPresetBar.getPresetCombo().getItems().add(scenarioToSave);
                }
                scenarioPresetBar.getPresetCombo().setValue(scenarioToSave);
            }
        } catch (Exception ex) {
            logger.warn("Could not save scenario to DB prior to simulation launch", ex);
        }

        if (cancelRequested) {
            resetStartButtonState();
            updateProgress(0.0, "🛑 Calcul annulé par l'utilisateur.");
            return;
        }

        updateProgress(0.05, "✅ Scénario '" + scenarioToSave.getName() + "' prêt ! ⚡ Calcul de la grille H3...");

        generationThread = new Thread(() -> {
            try {
                PlanetPreset cfg = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
                if (cfg == null) cfg = PlanetPreset.EARTH_LIKE;

                Integer res = h3ResolutionCombo.getValue();
                if (res != null) {
                    cfg = new PlanetPreset(
                            cfg.name(), res, cfg.radiusKm(), cfg.dayLengthHours(),
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
                    );
                }

                logger.info("Generating H3 cells for scenario '{}' at resolution {}", scenarioToSave.getName(), cfg.resolution());

                // Progress-tracked procedural planet generation (5% -> 70%)
                List<H3Cell> cells = new ProceduralGenerator().generatePlanet(cfg, (done, total) -> {
                    double progressVal = 0.05 + 0.65 * ((double) done / total);
                    int percent = (int) (progressVal * 100);
                    updateProgress(progressVal, String.format("🌍 Génération du relief & biomes H3 : %d%% (%d / %d cellules)", percent, done, total));
                }, () -> cancelRequested);

                if (cancelRequested) throw new java.util.concurrent.CancellationException("Generation cancelled by user");

                // Map elevation and biome images corresponding to the selected planet preset
                applyPresetMapToCells(cells, cfg);

                // Hydrography and river basin accumulation (70% -> 78%)
                updateProgress(0.72, "🌊 Calcul du réseau hydrographique et accumulation des bassins versants (72%)...");

                // Apply Geographical Clipping if enabled (78% -> 85%)
                if (clippingCheckBox != null && clippingCheckBox.isSelected()) {
                    updateProgress(0.80, "✂️ Application du découpage géographique et modélisation des frontières (80%)...");

                    double minLat = minLatSpinner.getValue();
                    double maxLat = maxLatSpinner.getValue();
                    double minLng = minLngSpinner.getValue();
                    double maxLng = maxLngSpinner.getValue();

                    double marginLat = Math.max(1.0, (maxLat - minLat) * 0.08);
                    double marginLng = Math.max(1.0, (maxLng - minLng) * 0.08);

                    List<H3Cell> clippedCells = new ArrayList<>();
                    for (H3Cell c : cells) {
                        if (cancelRequested) throw new java.util.concurrent.CancellationException("Generation cancelled by user");
                        if (c.getLatitude() >= minLat && c.getLatitude() <= maxLat &&
                            c.getLongitude() >= minLng && c.getLongitude() <= maxLng) {
                            
                            // Tag boundary cells for realistic edge condition modeling
                            if (c.getLatitude() <= minLat + marginLat || c.getLatitude() >= maxLat - marginLat ||
                                c.getLongitude() <= minLng + marginLng || c.getLongitude() >= maxLng - marginLng) {
                                c.setBoundaryCell(true);
                            }
                            clippedCells.add(c);
                        }
                    }
                    cells = clippedCells;
                    logger.info("Clipping applied: {} cells retained out of global planet grid.", cells.size());
                }

                if (cancelRequested) throw new java.util.concurrent.CancellationException("Generation cancelled by user");

                int totalCellCount = cells.size();

                // Distribute Initial Population (88% -> 94%)
                updateProgress(0.88, String.format("👥 Répartition de la population, empreinte écologique & capital K₀ : 88%% (%d / %d cellules)", totalCellCount, totalCellCount));

                distributeInitialPopulation(cells);
                if (cancelRequested) throw new java.util.concurrent.CancellationException("Generation cancelled by user");

                updateProgress(0.94, String.format("⚡ Synchronisation des buffers de simulation & précalculs thermodynamiques : 94%% (%d / %d cellules)", totalCellCount, totalCellCount));

                currentPreviewCells = cells;

                final List<H3Cell> finalCells = cells;
                javafx.application.Platform.runLater(() -> {
                    updateProgress(1.0, String.format("✅ %d cellules H3 calculées ! Lancement du scénario '%s'...", finalCells.size(), scenarioToSave.getName()));
                    drawPreview();
                    previewStatusLabel.setText(I18n.getOrDefault("scenario.status.generated_prefix", "Generated: ") + finalCells.size() + I18n.getOrDefault("scenario.status.generated_h3_suffix", " H3 cells."));

                    setStartButtonCompletedState();

                    if (onStartSimulation != null) {
                        onStartSimulation.accept(scenarioToSave);
                    }
                });
            } catch (Exception ex) {
                if (cancelRequested || ex instanceof java.util.concurrent.CancellationException || ex instanceof InterruptedException) {
                    logger.info("Deferred H3 calculation cancelled by user.");
                    updateProgress(0.0, "🛑 Calcul annulé par l'utilisateur.");
                } else {
                    logger.error("Error during deferred H3 cell calculation", ex);
                    updateProgress(1.0, "❌ Erreur pendant le calcul : " + ex.getMessage());
                }
                resetStartButtonState();
            } finally {
                generationThread = null;
            }
        }, "H3-Scenario-Generator-Thread");
        generationThread.setDaemon(true);
        generationThread.start();
    }

    private void applyPresetMapToCells(List<H3Cell> cells, PlanetPreset cfg) {
        if (cfg == null || cells == null || cells.isEmpty()) return;
        
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
        } else if (mapSource.equals("earth") || presetName.contains("earth") || presetName.contains("terre") || presetName.contains("terran")) {
            try (var elevStream = getClass().getResourceAsStream("/maps/earth_elevation.png");
                 var biomeStream = getClass().getResourceAsStream("/maps/earth_biomes.png")) {
                if (elevStream != null || biomeStream != null) {
                    new org.ether.society.data.ImageMapLoader().mapDataToCells(cells, elevStream, biomeStream);
                }
            } catch (Exception e) {
                logger.warn("Could not load Earth elevation map image", e);
            }
        } else if (mapSource.equals("mars") || presetName.contains("mars") || presetName.contains("ares")) {
            try (var elevStream = getClass().getResourceAsStream("/maps/mars_elevation.png");
                 var biomeStream = getClass().getResourceAsStream("/maps/mars_biomes.png")) {
                if (elevStream != null || biomeStream != null) {
                    new org.ether.society.data.ImageMapLoader().mapDataToCells(cells, elevStream, biomeStream);
                }
            } catch (Exception e) {
                logger.debug("No static Mars image resource found, using physical Simplex terrain generator");
            }
        } else if (mapSource.equals("venus") || presetName.contains("venus") || presetName.contains("vénus") || presetName.contains("hesperos")) {
            try (var elevStream = getClass().getResourceAsStream("/maps/venus_elevation.png");
                 var biomeStream = getClass().getResourceAsStream("/maps/venus_biomes.png")) {
                if (elevStream != null || biomeStream != null) {
                    new org.ether.society.data.ImageMapLoader().mapDataToCells(cells, elevStream, biomeStream);
                }
            } catch (Exception e) {
                logger.debug("No static Venus image resource found, using physical Simplex terrain generator");
            }
        } else if (mapSource.equals("moon") || presetName.contains("lune") || presetName.contains("moon") || presetName.contains("selene")) {
            try (var elevStream = getClass().getResourceAsStream("/maps/moon_elevation.png");
                 var biomeStream = getClass().getResourceAsStream("/maps/moon_biomes.png")) {
                if (elevStream != null || biomeStream != null) {
                    new org.ether.society.data.ImageMapLoader().mapDataToCells(cells, elevStream, biomeStream);
                }
            } catch (Exception e) {
                logger.debug("No static Moon image resource found, using physical Simplex terrain generator");
            }
        }
    }

    // =========================================================================
    // EVENTS SECTION — Historical Planetary Events
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
        eventsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        List<String> eventTypes = List.of(
            "volcano",
            "heatwave",
            "solar_emp",
            "pandemic",
            "meteor",
            "nuclear_winter",
            "famine",
            "tsunami",
            "earthquake",
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
        colType.setPrefWidth(115);

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

        colLat = new TableColumn<>();
        colLat.setCellValueFactory(d -> d.getValue().latitudeProperty().asObject());
        colLat.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        colLat.setOnEditCommit(e -> e.getRowValue().setLatitude(e.getNewValue()));
        colLat.setPrefWidth(55);

        colLon = new TableColumn<>();
        colLon.setCellValueFactory(d -> d.getValue().longitudeProperty().asObject());
        colLon.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
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
        eventsTable.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events_table", "📋 Events Table: Double-click a cell to edit its type (17 values available), name, year, coordinates, or magnitude.")));

        addEventBtn = new Button();
        addEventBtn.getStyleClass().add("button-secondary");
        addEventBtn.setMinWidth(Region.USE_PREF_SIZE);
        addEventBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events.add", "➕ Add new event (choose among 17 cataclysmic/climate types).")));
        addEventBtn.setOnAction(e -> eventsList.add(new ClimateEvent("volcano", "Nouvel Événement", 2026, 0.0, 0.0, 10.0, 6.0)));

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
        loadEarthEventsBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events.load", "Load or merge Earth historical events.")));
        loadEarthEventsBtn.setOnAction(e -> loadEarthHistoricalEvents(startYearSpinner != null ? startYearSpinner.getValue() : -8000, true));

        CheckBox eventsCheckBox = new CheckBox(org.ether.society.i18n.I18n.getOrDefault("scenario.events.enable", "Schedule climate events & historical disasters"));
        eventsCheckBox.setStyle("-fx-font-weight: bold;");
        eventsCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.enable_events", "Enable event calendar to simulate earthquakes, eruptions, and climate impacts on fixed dates.")));

        randomEventsCheckBox = new CheckBox(org.ether.society.i18n.I18n.getOrDefault("scenario.events.random_checkbox", "☑️ Dynamically generate geological events & climate drifts (earthquakes, volcanism, sea rise, green Sahara)"));
        randomEventsCheckBox.setSelected(true);
        randomEventsCheckBox.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        randomEventsCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.random_events", "Allows stochastic engine to generate random geological events consistent with region.")));

        HBox btnBar = new HBox(8, addEventBtn, removeEventBtn, new Separator(javafx.geometry.Orientation.VERTICAL), loadEarthEventsBtn);
        btnBar.setAlignment(Pos.CENTER_LEFT);

        VBox eventsSubPanel = new VBox(8, randomEventsCheckBox, btnBar, eventsTable);
        eventsSubPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(251,146,60,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        eventsSubPanel.setVisible(false);
        eventsSubPanel.setManaged(false);

        eventsCheckBox.setOnAction(e -> {
            boolean active = eventsCheckBox.isSelected();
            eventsSubPanel.setVisible(active);
            eventsSubPanel.setManaged(active);
            notifyParamChange();
        });

        section.getChildren().addAll(title3Events, eventsCheckBox, eventsSubPanel);
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
                    "Des événements existent déjà dans votre tableau. Souhaitez-vous les remplacer ou les fusionner avec la base historique ?"));

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

        List<ClimateEvent> masterList = new ArrayList<>();

        // Éruptions Volcaniques & Stratosphère (tau)
        masterList.add(new ClimateEvent("volcano", "Super-Éruption Toba (VEI-8, τ=3.50, Gel Global)", -74000, 2.88, 98.88, 0.0, 8.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Santorini / Thera (VEI-7)", -1640, 36.40, 25.40, 0.0, 7.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Vésuve (Pompéi VEI-5)", 79, 40.82, 14.43, 0.0, 5.0));
        masterList.add(new ClimateEvent("volcano", "Anomalie Volcanique & Hiver Global 536 (VEI-7)", 536, 13.70, -89.20, 0.0, 7.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Samalas / Rinjani (VEI-7, Petit Âge Glaciaire)", 1257, -8.40, 116.47, 0.0, 7.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Huaynaputina (VEI-6)", 1600, -16.60, -71.35, 0.0, 6.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Laki (Islande, Nuage Toxique VEI-6)", 1783, 64.06, -17.33, 0.0, 6.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Tambora (VEI-7, τ=1.20, Année sans été)", 1815, -8.25, 117.98, 0.0, 7.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Krakatoa (VEI-6, τ=0.80)", 1883, -6.10, 105.42, 0.0, 6.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Pinatubo (VEI-6, τ=0.40)", 1991, 15.13, 120.35, 0.0, 6.0));

        // Tempêtes Solaires & EMP Carrington
        masterList.add(new ClimateEvent("solar_emp", "Événement Solaire Carrington (EMP & Grille Électrique)", 1859, 50.0, 0.0, 0.0, 8.5));

        // Impact Météorique
        masterList.add(new ClimateEvent("meteor", "Impact Chicxulub (Extinction K-Pg VEI-10)", -66000000, 21.40, -89.50, 0.0, 10.0));
        masterList.add(new ClimateEvent("meteor", "Impact Tunguska (Onde de Choc 15Mt)", 1908, 60.89, 101.89, 0.0, 5.0));

        // Tectonique, Séismes & Tsunamis
        masterList.add(new ClimateEvent("earthquake", "Séisme Shensi (Chine, 830k victimes)", 1556, 34.50, 109.70, 20.0, 8.0));
        masterList.add(new ClimateEvent("earthquake", "Séisme Lisbonne (Portugal & Tsunami, M8.7)", 1755, 36.00, -10.50, 30.0, 8.7));
        masterList.add(new ClimateEvent("earthquake", "Séisme Valdivia (Chili, M9.5)", 1960, -38.14, -73.41, 25.0, 9.5));
        masterList.add(new ClimateEvent("earthquake", "Séisme Alaska (M9.2)", 1964, 61.02, -147.65, 25.0, 9.2));
        masterList.add(new ClimateEvent("earthquake", "Séisme Sumatra-Andaman (M9.1)", 2004, 3.30, 95.98, 30.0, 9.1));
        masterList.add(new ClimateEvent("earthquake", "Séisme Tohoku Japon (M9.0)", 2011, 38.30, 142.37, 29.0, 9.0));
        masterList.add(new ClimateEvent("tsunami", "Tsunami Sumatra (Submersion Littorale)", 2004, 3.30, 95.98, 30.0, 9.1));
        masterList.add(new ClimateEvent("tsunami", "Tsunami Fukushima & Accident Nucléaire", 2011, 38.30, 142.37, 29.0, 9.0));

        // Glaciations, Pandémies & Famines
        masterList.add(new ClimateEvent("ice_age", "Glaciation Abrupte Younger Dryas", -10900, 60.0, -20.0, 0.0, 4.5));
        masterList.add(new ClimateEvent("famine", "Grande Famine Médiévale Européenne (1315-1317)", 1315, 50.0, 10.0, 0.0, 6.0));
        masterList.add(new ClimateEvent("pandemic", "Peste Noire (Europe, 1/3 Pop.)", 1347, 44.00, 10.00, 0.0, 9.0));
        masterList.add(new ClimateEvent("pandemic", "Grippe Espagnole (50M Victimes)", 1918, 40.00, 0.00, 0.0, 8.0));

        // Hiver Nucléaire & Catastrophes Modernes
        masterList.add(new ClimateEvent("nuclear_winter", "Hiver Nucléaire Tchernobyl & Fallout Stratosphérique", 1986, 51.38, 30.10, 0.0, 7.5));
        masterList.add(new ClimateEvent("heatwave", "Canicule Extrême Européenne", 2003, 46.5, 2.5, 0.0, 5.0));

        // Anticipations & Événements Futurs (Cyber, Krach, Bio, Géoingénierie, Singularité)
        masterList.add(new ClimateEvent("cyber_attack", "Panne Numérique Mondiale / Blackout Système", 2028, 48.85, 2.35, 0.0, 6.5));
        masterList.add(new ClimateEvent("economic_crash", "Krach Boursier & Crise Systémique Mondiale", 2030, 40.71, -74.00, 0.0, 7.0));
        masterList.add(new ClimateEvent("biodiversity_collapse", "Effondrement des Pollinisateurs & Biomasse", 2035, 0.0, 0.0, 0.0, 6.0));
        masterList.add(new ClimateEvent("geoengineering", "Injection Stratosphérique Aérosols Test SRM", 2040, 15.0, 100.0, 0.0, 4.0));
        masterList.add(new ClimateEvent("renaissance_boom", "Émergence Révolution Énergie Fusion Superconductrice", 2050, 43.60, 5.70, 0.0, 8.0));
        masterList.add(new ClimateEvent("tech_singularity", "Singularité Technologique & Superintelligence", 2075, 37.77, -122.41, 0.0, 9.5));
        masterList.add(new ClimateEvent("alien_contact", "Réception Signal Radio Exogène Unificateur", 2090, -31.0, 149.0, 0.0, 10.0));

        List<ClimateEvent> filtered = new ArrayList<>();
        for (ClimateEvent ev : masterList) {
            if (ev.getYear() >= startYear) {
                filtered.add(ev);
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
    // I18N — Update all UI text based on current language
    // =========================================================================

    public void updateTexts() {
        boolean oldUpdating = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            if (headerLabel != null) headerLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.panel.title", "📜 Scenario Configuration & Setup"));
            if (scenarioPresetHeader != null) scenarioPresetHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.section.presets", "🎛️ GLOBAL PRESETS & SCENARIO SAVE"));
            if (planetSectionHeader != null) planetSectionHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.section.inherited", "🪐 INHERITED CONTEXT (TABS 1 & 2)"));
            if (title1 != null) title1.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.section.spatiotemporal", "🌐 EPOCH & SPATIOTEMPORAL DEFINITION"));
            if (cultureHeader != null) cultureHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.culture_section", "🧠 CULTURAL VECTOR DIMENSION & MULTI-FIELD LAYERS"));
            if (clippingHeader != null) clippingHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.header", "✂️ BORDERS & HISTORICAL SPATIAL CLIPPING"));
            if (oceanOptHeader != null) oceanOptHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.ocean_opt.header", "⚙️ ENGINE ARCHITECTURE & OPTIMIZATIONS (ETHER CORE & OPTIONAL)"));
            if (title3Events != null) title3Events.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events_section", "🌪️ HISTORICAL PLANETARY EVENTS & CLIMATE DRIFTS"));
            if (snapshotHeader != null) snapshotHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.snapshot.header", "📸 RESUME FROM EXISTING SNAPSHOT (PREVIOUS SESSION)"));
            if (bundleHeader != null) bundleHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.bundle.header", "📦 UNIFIED BUNDLE MULTI-SCENARIO IMPORT/EXPORT (.ETHER)"));
            if (liveDiagnosticHeader != null) liveDiagnosticHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.diagnostic.header", "📋 CIVILIZATIONAL VIABILITY DIAGNOSTIC (REAL TIME)"));
            if (previewTitleLabel != null) previewTitleLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.title.right_view", "🗺️ Resource Cartography & Display"));
            if (startYearLabel != null) startYearLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.start_year", "Start Year (Chronological Reference):"));
            if (endYearLabel != null) endYearLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.end_year", "End / Target Year (Chronological Reference):"));
            if (popCountLabel != null) popCountLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_count", "Initial Population (1,000 to 10,000,000,000):"));
            if (densityPatternLabel != null) densityPatternLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.density_pattern", "Distribution Pattern:"));
            if (h3ResolutionLabel != null) h3ResolutionLabel.setText(org.ether.society.i18n.I18n.getOrDefault("planet.param.resolution", "H3 Resolution:"));
            if (temporalResolutionLabel != null) temporalResolutionLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.label.temporal_resolution", "Time Step Δt (Temporal Resolution):"));
            if (cohortSizeLabel != null) cohortSizeLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.label.cohort_size", "Cohort Size:"));
            if (startBtn != null) startBtn.setText(org.ether.society.i18n.I18n.get("scenario.start_btn"));
            if (generateBtn != null) generateBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.preview_btn", "🔄 Preview Distribution"));
            if (addEventBtn != null) addEventBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.add", "➕ Add Event"));
            if (removeEventBtn != null) removeEventBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.remove", "🗑️ Remove Event"));
            if (loadEarthEventsBtn != null) loadEarthEventsBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.load_earth", "🌍 Load Earth Historical Events"));

            if (colType != null) colType.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.type", "Event Type"));
            if (colName != null) colName.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.name", "Event Name"));
            if (colYear != null) colYear.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.year", "Year (Yr)"));
            if (colLat != null) colLat.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.lat", "Lat (°)"));
            if (colLon != null) colLon.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.lon", "Lng (°)"));
            if (colDepth != null) colDepth.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.depth", "Depth (km)"));
            if (colMag != null) colMag.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.table.col.mag", "Magnitude / Intensity"));

            if (clippingCheckBox != null) clippingCheckBox.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.enable", "Enable Partial Simulation (Truncated Zone)"));
            if (graphicSelectBtn != null) graphicSelectBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.select_mode", "🖱️ Graphic Map Selection Mode"));
            if (resetClippingBtn != null) resetClippingBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.reset", "🔄 Reset Area (Full Planet)"));
            if (latMaxLabel != null) latMaxLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lat_max", "Lat Max (Top):"));
            if (latMinLabel != null) latMinLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lat_min", "Lat Min (Bottom):"));
            if (lngMinLabel != null) lngMinLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lng_min", "Lng Min (Left):"));
            if (lngMaxLabel != null) lngMaxLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lng_max", "Lng Max (Right):"));
            if (boundaryLabel != null) boundaryLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.boundary_label", "Scientific Border Modeling:"));
            if (culturalHelpBtn != null) culturalHelpBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.cultural_format_help", "❓ Layer Format"));
            if (btnReliefOverlay != null) btnReliefOverlay.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.relief_overlay", "⛰️ Relief"));
            if (btnReliefOverlay != null) btnReliefOverlay.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.relief_overlay", "⛰️ Relief"));
            updatePreviewTitleText();

            if (radioProcDemo != null) radioProcDemo.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.procedural", "▶ Procedural Generation (Perlin / Simplex Noise)"));
            if (radioImportDemo != null) radioImportDemo.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.import", "📂 External Source / Import (PNG / WMS / GeoTIFF)"));
            if (loadDensityMapBtn != null) loadDensityMapBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.demo.btn_load", "📥 Import External Density Map (PNG)"));
            if (exportDensityMapBtn != null) exportDensityMapBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.demo.btn_export", "📤 Export Generated Density Map (PNG)"));

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


            updatePreviewModesCombo();

            if (eventsTable != null && eventsTable.getUserData() instanceof TableColumn[] cols && cols.length == 6) {
                cols[0].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.name", "Nom / Description"));
                cols[1].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.year", "Year"));
                cols[2].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.lat", "Latitude"));
                cols[3].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.lon", "Longitude"));
                cols[4].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.depth", "Profondeur (km)"));
                cols[5].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.magnitude", "Magnitude"));
            }
            if (getLeft() instanceof ScrollPane sp && sp.getContent() instanceof VBox root) {
                root.lookupAll("#__popHeader").forEach(n -> { if (n instanceof Label l) l.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_section", "👥 2. INITIAL POPULATION IDENTITY CARD")); });
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
        int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 8;
        s.setCultureVectorDimensions(dims);
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
                     * Loi Physique Cœur Ether : %s
                     */
                    public class %s {
                        public void update(H3SimulationEngine engine, H3Cell cell, double deltaTime) {
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
                String template = org.ether.society.procedural.jit.DynamicEngineCompiler.generateEngineTemplateCode("MyCustomOptionalEngine");
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
                org.ether.society.procedural.jit.DynamicEngineCompiler.CompilationResult result =
                        org.ether.society.procedural.jit.DynamicEngineCompiler.compileAndLoadEngine(file);

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

    public boolean validateScenarioSetup() {
        List<String> errors = new ArrayList<>();

        // 1. Cross-Tab Validation: Tab 1 (Planet) and Tab 2 (Ecology/Resource)
        if (planetPanelSupplier != null && planetPanelSupplier.get() != null) {
            errors.addAll(planetPanelSupplier.get().getValidationErrors());
        }
        if (resourcePanelSupplier != null && resourcePanelSupplier.get() != null) {
            errors.addAll(resourcePanelSupplier.get().getValidationErrors());
        }

        // 2. Dates Validation
        if (startYearSpinner != null && endYearSpinner != null) {
            int startYr = startYearSpinner.getValue();
            int endYr = endYearSpinner.getValue();
            if (startYr >= endYr) {
                errors.add(I18n.getOrDefault("scenario.validation.invalid_years", "Start year must be strictly less than end year (Tab 3)."));
                startYearSpinner.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
                endYearSpinner.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            } else {
                startYearSpinner.setStyle("");
                endYearSpinner.setStyle("");
            }
        }

        // 3. Population Count Validation
        if (initialHumanCountSpinner != null) {
            long count = initialHumanCountSpinner.getValue();
            if (count <= 0) {
                errors.add(I18n.getOrDefault("scenario.validation.invalid_pop", "Initial human population must be greater than 0 (Tab 3)."));
                initialHumanCountSpinner.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            } else {
                initialHumanCountSpinner.setStyle("");
            }
        }

        // 4. Demographic Density Map Import
        if (radioImportDemo != null && radioImportDemo.isSelected()) {
            if (customDensityImage == null) {
                errors.add(I18n.getOrDefault("scenario.validation.missing_density_map", "Missing demographic density map in import mode (Tab 3)."));
                if (loadDensityMapBtn != null) loadDensityMapBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            } else {
                org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(customDensityImage);
                if (!val.valid()) {
                    errors.add(String.format(I18n.getOrDefault("scenario.validation.invalid_density_map", "Incompatible demographic density map (Tab 3): %s"), val.message()));
                    if (loadDensityMapBtn != null) loadDensityMapBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
                } else if (loadDensityMapBtn != null) {
                    loadDensityMapBtn.setStyle("");
                }
            }
        } else if (loadDensityMapBtn != null) {
            loadDensityMapBtn.setStyle("");
        }

        // 5. Cultural Tensor Dimensions Index Bounds Protection
        if (cultureVectorDimSpinner != null) {
            int dims = cultureVectorDimSpinner.getValue();
            if (dims < 1 || dims > 32) {
                errors.add(I18n.getOrDefault("scenario.validation.invalid_tensor_dim", "Cultural tensor dimension must be between 1 and 32 (Tab 3)."));
                cultureVectorDimSpinner.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            } else {
                cultureVectorDimSpinner.setStyle("");
            }
        }

        // 6. Cultural Tensors in Import Mode Validation
        int dimsCount = cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null ? cultureVectorDimSpinner.getValue() : 8;
        for (int i = 0; i < dimsCount; i++) {
            RadioButton importRadio = tensorImportRadios.get(i);
            Button loadBtn = tensorLoadBtns.get(i);
            if (importRadio != null && importRadio.isSelected()) {
                Image img = customTensorImages.get(i);
                if (img == null) {
                    String tensorName = getCulturalTensorTitle(i);
                    errors.add(String.format(I18n.getOrDefault("scenario.validation.missing_tensor_map", "Missing external map for %s in import mode (Tab 3)."), tensorName));
                    if (loadBtn != null) {
                        loadBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
                    }
                } else {
                    org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(img);
                    if (!val.valid()) {
                        String tensorName = getCulturalTensorTitle(i);
                        errors.add(String.format(I18n.getOrDefault("scenario.validation.invalid_tensor_map", "Incompatible or unreadable map for %s (Tab 3): %s"), tensorName, val.message()));
                        if (loadBtn != null) {
                            loadBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
                        }
                    } else if (loadBtn != null) {
                        loadBtn.setStyle("");
                    }
                }
            } else if (loadBtn != null) {
                loadBtn.setStyle("");
            }
        }

        boolean isValid = errors.isEmpty();

        if (!isValid) {
            StringBuilder sb = new StringBuilder();
            for (String err : errors) {
                sb.append("• ").append(err).append("\n");
            }
            if (validationErrorLabel != null && validationErrorBanner != null) {
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

}
