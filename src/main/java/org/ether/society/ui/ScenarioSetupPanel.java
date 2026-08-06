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

    // Inherited Presets Header
    private Label planetSectionHeader;
    private ComboBox<PlanetPreset> planetPresetCombo;
    private ComboBox<EcologyPreset> ecologyPresetCombo;
    private Label inheritedContextLabel;

    // Standardized Preset Bar for Scenarios
    private PresetControlBar<Scenario> scenarioPresetBar;

    // Form Controls
    private TextArea scenarioDescriptionArea;
    private Spinner<Integer> startYearSpinner;
    private ComboBox<Integer> h3ResolutionCombo;
    private Label h3ResolutionLabel;
    private Spinner<Integer> targetCohortSizeSpinner;
    private Label cohortSizeLabel;

    // State & Change Listener Tracking
    private boolean isUpdatingFromPreset = false;

    private void notifyParamChange() {
        if (!isUpdatingFromPreset && scenarioPresetBar != null) {
            scenarioPresetBar.notifyParametersChanged();
        }
    }

    // Population & Density Controls
    private Spinner<Long> initialHumanCountSpinner;
    private Spinner<Double> initialCapitalSpinner;
    private Spinner<Double> initialEnergySpinner;
    private Spinner<Double> initialFoodSpinner;
    private Spinner<Double> initialInfoSpinner;
    private ComboBox<String> densityPatternCombo;
    private Spinner<Integer> urbanCentersSpinner;

    // Density Map Import/Export Controls
    private Image customDensityImage;
    private Label densityMapFileLabel;
    private Button loadDensityMapBtn;
    private Button demoHelpBtn;
    private Label densityFormatHintLabel;
    private Button exportDensityMapBtn;

    // Map Preview
    private Canvas previewCanvas;
    private ComboBox<String> previewModeCombo;
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
    private Label title1;
    private Label title3Events;
    private Label nameLabel;
    private Label startYearLabel;
    private Label popCountLabel;
    private Label capitalLabel;
    private Label energyLabel;
    private Label foodLabel;
    private Label infoLabel;
    private Label densityPatternLabel;
    private Label urbanCentersLabel;
    private Button generateBtn;
    private Button startBtn;

    // Seed & Random Events Controls for Tab 3
    private TextField demoSeedField;
    private Button demoRandSeedBtn;
    private CheckBox randomEventsCheckBox;

    // Spatial Clipping & Boundary Condition Controls
    private Label clippingHeader;
    private CheckBox clippingCheckBox;
    private ToggleButton graphicSelectBtn;
    private Spinner<Double> minLatSpinner;
    private Spinner<Double> maxLatSpinner;
    private Spinner<Double> minLngSpinner;
    private Spinner<Double> maxLngSpinner;
    private ComboBox<String> boundaryModeCombo;
    private Button resetClippingBtn;

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

    private static final Map<String, String> DENSITY_LABELS = Map.of(
        "UNBIASED_NATURAL", "⚖️ Équilibre Naturel Pur (Sans Favoritisme / Par Défaut)",
        "FERTILE_CRESCENT", "🌾 Plaines Alluviales & Littoraux (Croissant Fertile)",
        "MESOAMERICA", "🌴 Jungles Tropicales & Collines (Mésoamérique)",
        "MESOPOTAMIA_ASSYRIA", "🏛️ Deltas & Bassins Fluviaux (Mésopotamie)",
        "RIVER_VALLEYS", "🌊 Axes Fluviaux & Deltas",
        "URBAN_CLUSTERS", "🏙️ Nœuds Urbains & Métropoles Concentrées",
        "SPARSE_NOMADIC", "⛺ Dispersion Pastoraliste Nomade (Déserts/Tundras)"
    );

    private static final Map<String, String> DENSITY_DESCRIPTIONS = Map.of(
        "UNBIASED_NATURAL", "⚖️ Équilibre Naturel Pur : Aucun favoritisme régional ni biais artificiel. La population s'établit strictement selon la viabilité environnementale réelle (température, eau, altitude, biomes).",
        "FERTILE_CRESCENT", "🌾 Plaines Alluviales & Littoraux : Implantation le long des plaines alluviales et littoraux tempérés (Plaines/Littoraux: ×4.5, autres: ×0.3). Archetype: Croissant Fertile.",
        "MESOAMERICA", "🌴 Valées Tropicales & Collines : Dispersion adaptée aux jungles tropicales et vallées d'altitude (Jungles/Collines: ×3.5, autres: ×0.5). Archetype: Mésoamérique.",
        "MESOPOTAMIA_ASSYRIA", "🏛️ Deltas & Bassins Fluviaux : Hyper-concentration le long des réseaux hydrographiques majeurs et deltas (Plaines: ×5.0, autres: ×0.2). Archetype: Mésopotamie.",
        "RIVER_VALLEYS", "🌊 Axes Fluviaux : Colonisation linéaire le long du tracé des fleuves et deltas (Fleuves/Littoraux: ×4.5, autres: ×0.3).",
        "URBAN_CLUSTERS", "🏙️ Nœuds Urbains : Émergence de métropoles hyper-concentrées avec grappes urbaines (Booster Cités: ×15.0).",
        "SPARSE_NOMADIC", "⛺ Dispersion Nomade : Population pastorale dispersée à faible densité sur de grands espaces (Déserts/Tundras: ×1.5, autres: ×0.8)."
    );

    // Clipping & Boundary Label Fields for live i18n
    private Label latMaxLabel;
    private Label latMinLabel;
    private Label lngMinLabel;
    private Label lngMaxLabel;
    private Label boundaryLabel;
    private Label previewTitleLabel;

    // Ocean Optimization Checkboxes (Persisted at Scenario Level)
    private CheckBox oceanMacroAggregationCheckBox;
    private CheckBox coastalNavigationOnlyCheckBox;
    private CheckBox oceanMultiRateTickingCheckBox;

    // Async Calculation & Thread Control Fields
    private volatile boolean isCalculationRunning = false;
    private volatile boolean cancelRequested = false;
    private Thread generationThread = null;

    // Events section
    private TableView<ClimateEvent> eventsTable;
    private ObservableList<ClimateEvent> eventsList;
    private Button addEventBtn;
    private Button removeEventBtn;
    private Button loadEarthEventsBtn;

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
            generatePreview();
        });
    }

    public void setInheritedContext(PlanetPreset planetPreset, String ecologyName) {
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
        notifyParamChange();
        if (currentPreviewCells != null) {
            generatePreview();
        }
    }

    public PlanetPreset findPlanetPresetByName(String name) {
        if (name == null || name.isBlank()) return PlanetPreset.EARTH_LIKE;
        for (PlanetPreset p : PlanetPreset.getPresets()) {
            if (p.name().equalsIgnoreCase(name)) return p;
        }
        String lower = name.toLowerCase();
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

        // Left: Configuration Controls
        VBox configPane = createConfigPane();
        configPane.setPrefWidth(460);

        ScrollPane configScroll = new ScrollPane(configPane);
        configScroll.setFitToWidth(true);
        configScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        configScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        configScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Right: Preview Area (Density map only)
        VBox previewPane = createPreviewPane();

        setLeft(configScroll);
        setCenter(previewPane);
    }

    private VBox createConfigPane() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(0, 20, 0, 0));

        // --- 1. Standardized Preset Control Bar for Scenarios ---
        scenarioPresetBar = new PresetControlBar<>(org.ether.society.i18n.I18n.getOrDefault("scenario.preset_bar", "Préréglage de Scénario"));
        scenarioPresetBar.setExportCategory("scenario");
        List<Scenario> builtInScenarios = getBuiltInScenarios();
        Scenario defaultScenario = builtInScenarios.isEmpty() ? null : builtInScenarios.get(0);
        scenarioPresetBar.setPresets(builtInScenarios, defaultScenario);

        scenarioPresetBar.setListener(new PresetControlBar.PresetActionsListener<Scenario>() {
            @Override
            public void onPresetSelected(Scenario scenario) {
                applyScenarioToUI(scenario);
            }

            @Override
            public void onSavePreset(String name) {
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

        // --- 2. Inherited Presets (Tab 1 Planet + Tab 2 Ecology) Header ---
        planetSectionHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.section.inherited", "CONTEXTE HÉRITÉ (ONGLETS 1 & 2)"));

        planetPresetCombo = new ComboBox<>();
        planetPresetCombo.getItems().setAll(PlanetPreset.getPresets());
        planetPresetCombo.setValue(PlanetPreset.EARTH_LIKE);
        planetPresetCombo.setMaxWidth(Double.MAX_VALUE);
        planetPresetCombo.setDisable(true); // Greyed out: cascade-derived from Tab 2 Ecology
        planetPresetCombo.setStyle("-fx-opacity: 0.75;");
        planetPresetCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        planetPresetCombo.setButtonCell(planetPresetCombo.getCellFactory().call(null));
        planetPresetCombo.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.planet_preset_disabled", "Préréglage planétaire hérité et déduit automatiquement de l'écologie choisie (Onglet 2).")));

        ecologyPresetCombo = new ComboBox<>();
        ecologyPresetCombo.getItems().setAll(EcologyPreset.getBuiltInPresets());
        ecologyPresetCombo.setValue(EcologyPreset.getBuiltInPresets().get(0));
        ecologyPresetCombo.setMaxWidth(Double.MAX_VALUE);
        ecologyPresetCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(EcologyPreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        ecologyPresetCombo.setButtonCell(ecologyPresetCombo.getCellFactory().call(null));
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

        inheritedContextLabel = new Label("🌿 Écologie : Earth Standard Baseline  ➔  🪐 Planète (déduite) : Terre (Terran)");
        inheritedContextLabel.setWrapText(true);
        inheritedContextLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 6 10; -fx-background-color: rgba(56, 189, 248, 0.12); -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 6;");

        VBox inheritedSection = createSection(planetSectionHeader, new VBox(8,
                new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.label.ecology_preset", "1️⃣ Préréglage Écologique (Onglet 2) :")),
                ecologyPresetCombo,
                new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.label.planet_preset", "2️⃣ Préréglage Planétaire (Onglet 1 — Déduit en cascade de l'Écologie) :")),
                planetPresetCombo,
                inheritedContextLabel
        ));

        // --- 3. Scenario General Info Section ---
        VBox section1 = new VBox(10);
        title1 = new Label();
        title1.getStyleClass().add("label-header");
        GridPane grid1 = new GridPane();
        grid1.setHgap(10);
        grid1.setVgap(10);

        startYearSpinner = new Spinner<>(-100000, 2100, -8000, 100);
        startYearSpinner.setEditable(true);
        startYearSpinner.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.start_year", "Marqueur chronologique à T=0 pour caler la simulation sur un repère calendaire standard. Ce chiffre est à titre indicatif et n'influence pas directement les équations de la simulation.")));

        startYearLabel = new Label();
        h3ResolutionLabel = new Label();

        // H3 Resolution (Row 0)
        h3ResolutionCombo = new ComboBox<>();
        h3ResolutionCombo.getItems().addAll(3, 4, 5, 6, 7, 8);
        h3ResolutionCombo.setValue(5);
        h3ResolutionCombo.setMaxWidth(Double.MAX_VALUE);
        h3ResolutionCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : org.ether.society.i18n.I18n.get("planet.param.resolution.res" + item));
            }
        });
        h3ResolutionCombo.setButtonCell(h3ResolutionCombo.getCellFactory().call(null));
        Tooltip.install(h3ResolutionCombo, new Tooltip(org.ether.society.i18n.I18n.getOrDefault("planet.tooltip.resolution", "Résolution de la grille H3")));
        Tooltip.install(h3ResolutionLabel, h3ResolutionCombo.getTooltip());
        h3ResolutionCombo.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        // Target Cohort Size (Row 1)
        cohortSizeLabel = new Label();
        targetCohortSizeSpinner = new Spinner<>(1, 100000, 500, 100);
        targetCohortSizeSpinner.setEditable(true);
        targetCohortSizeSpinner.setMaxWidth(Double.MAX_VALUE);
        targetCohortSizeSpinner.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.cohort_size", 
                "👥 Taille Cible des Cohortes Démographiques (Nœuds Agents DOD) [hab/cohorte] :\n" +
                "Détermine la taille moyenne des groupes d'habitants représentés par un même nœud agent.\n" +
                "• 1 000 à 10 000 hab/cohorte : Macro-simulation haute performance (par défaut)\n" +
                "• 100 à 500 hab/cohorte : Démographie fine et micro-groupes\n" +
                "• 1 hab/cohorte : Agent 1:1 (Modèle Individu par Individu - Mode Expérimental)")));
        Tooltip.install(cohortSizeLabel, targetCohortSizeSpinner.getTooltip());
        targetCohortSizeSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        // Start Year (Row 2)
        startYearSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            notifyParamChange();
            if (newV != null) {
                loadEarthHistoricalEvents(newV);
                if (!isUpdatingFromPreset) {
                    if (initialCapitalSpinner != null && initialCapitalSpinner.getValueFactory() != null)
                        initialCapitalSpinner.getValueFactory().setValue(computeAutoCapitalFromYear(newV));
                    if (initialEnergySpinner != null && initialEnergySpinner.getValueFactory() != null)
                        initialEnergySpinner.getValueFactory().setValue(computeAutoEnergyFromYear(newV));
                    if (initialFoodSpinner != null && initialFoodSpinner.getValueFactory() != null)
                        initialFoodSpinner.getValueFactory().setValue(computeAutoFoodFromYear(newV));
                    if (initialInfoSpinner != null && initialInfoSpinner.getValueFactory() != null)
                        initialInfoSpinner.getValueFactory().setValue(computeAutoInfoFromYear(newV));
                }
            }
        });
        Tooltip.install(startYearLabel, startYearSpinner.getTooltip());

        // Add to grid1: Row 0 = H3 Res, Row 1 = Cohort Size, Row 2 = Start Year
        grid1.addRow(0, h3ResolutionLabel, h3ResolutionCombo);
        grid1.addRow(1, cohortSizeLabel, targetCohortSizeSpinner);
        grid1.addRow(2, startYearLabel, startYearSpinner);

        Label descLabel = new Label("📖 Description Détaillée & Termes de Forçage Physiques :");
        descLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 6 0 2 0;");

        scenarioDescriptionArea = new TextArea();
        scenarioDescriptionArea.setPrefRowCount(8);
        scenarioDescriptionArea.setWrapText(true);
        scenarioDescriptionArea.getStyleClass().add("scenario-description-area");
        scenarioDescriptionArea.textProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        section1.getChildren().addAll(title1, grid1, descLabel, scenarioDescriptionArea);

        // --- 4. Demographics & Density Map Management (RadioButtons) ---
        VBox popSection = new VBox(10);
        Label popHeader = new Label();
        popHeader.getStyleClass().add("label-header");
        popHeader.setId("__popHeader");

        ToggleGroup demoSourceGroup = new ToggleGroup();
        radioProcDemo = new RadioButton(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.procedural", "▶ Mode Procédural (Algorithmes & Motifs Démographiques)"));
        radioImportDemo = new RadioButton(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.import", "📂 Mode Chargement d'une Carte de Densité Existante (PNG)"));
        radioProcDemo.setToggleGroup(demoSourceGroup);
        radioImportDemo.setToggleGroup(demoSourceGroup);
        radioProcDemo.setSelected(true);

        Label demoTypeLabel = new Label("⚙ Mode de Génération Démographique :");
        demoTypeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        VBox demoProcBox = new VBox(8);
        demoProcBox.setStyle("-fx-padding: 8; -fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 6;");

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

        initialCapitalSpinner = new Spinner<>(0.0, 50000.0, 10.0, 50.0);
        initialCapitalSpinner.setEditable(true);
        initialCapitalSpinner.setMaxWidth(Double.MAX_VALUE);
        initialCapitalSpinner.setTooltip(new Tooltip("Stock d'infrastructures physiques, outillage et machines de départ par habitant (kg/hab). Auto-calibré selon l'année T0 (ex: Néolithique ≈ 5 kg/hab, An 1500 ≈ 500 kg/hab, An 1800 ≈ 2500 kg/hab)."));
        initialCapitalSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        densityPatternCombo = new ComboBox<>();
        densityPatternCombo.getItems().addAll("UNBIASED_NATURAL", "FERTILE_CRESCENT", "MESOAMERICA", "MESOPOTAMIA_ASSYRIA", "RIVER_VALLEYS", "URBAN_CLUSTERS", "SPARSE_NOMADIC");
        densityPatternCombo.setValue("UNBIASED_NATURAL");
        densityPatternCombo.setMaxWidth(Double.MAX_VALUE);
        densityPatternCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(DENSITY_LABELS.getOrDefault(item, item));
                    setTooltip(new Tooltip(DENSITY_DESCRIPTIONS.getOrDefault(item, item)));
                }
            }
        });
        densityPatternCombo.setButtonCell(densityPatternCombo.getCellFactory().call(null));
        densityPatternCombo.setTooltip(new Tooltip(DENSITY_DESCRIPTIONS.get("UNBIASED_NATURAL")));
        densityPatternCombo.valueProperty().addListener((obs, oldV, newV) -> {
            notifyParamChange();
            if (newV != null) {
                densityPatternCombo.setTooltip(new Tooltip(DENSITY_DESCRIPTIONS.getOrDefault(newV, "")));
            }
        });

        urbanCentersSpinner = new Spinner<>(1, 30, 5, 1);
        urbanCentersSpinner.setEditable(true);
        urbanCentersSpinner.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.urban", "Nombre de foyers ou cités d'origine.")));

        popCountLabel = new Label();
        capitalLabel = new Label();
        densityPatternLabel = new Label();
        urbanCentersLabel = new Label();
        Tooltip.install(popCountLabel, initialHumanCountSpinner.getTooltip());
        Tooltip.install(capitalLabel, initialCapitalSpinner.getTooltip());
        Tooltip.install(densityPatternLabel, densityPatternCombo.getTooltip());
        Tooltip.install(urbanCentersLabel, urbanCentersSpinner.getTooltip());

        GridPane popGrid = new GridPane();
        popGrid.setHgap(10);
        popGrid.setVgap(10);
        popGrid.addRow(0, popCountLabel, initialHumanCountSpinner);
        popGrid.addRow(1, capitalLabel, initialCapitalSpinner);
        popGrid.addRow(2, densityPatternLabel, densityPatternCombo);
        popGrid.addRow(3, urbanCentersLabel, urbanCentersSpinner);

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
        demoRandSeedBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.seed.tooltip", "Générer une nouvelle graine aléatoire pour la démographie.")));
        HBox demoSeedBox = new HBox(5, demoSeedField, demoRandSeedBtn);
        HBox.setHgrow(demoSeedField, Priority.ALWAYS);

        Label demoSeedLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.seed.label", "Graine Aléatoire Démographique :"));
        popGrid.addRow(4, demoSeedLabel, demoSeedBox);

        // Secondary initial physical state spinners
        initialEnergySpinner = new Spinner<>(0.0, 1_000_000.0, 50.0, 50.0);
        initialEnergySpinner.setEditable(true);
        initialEnergySpinner.setMaxWidth(Double.MAX_VALUE);
        initialEnergySpinner.setTooltip(new Tooltip("⚡ Stock Énergétique Initial (E₀) [MJ/habitant] :\nCombustibles et vecteurs énergétiques pré-extraits (bois, charbon, pétrole, batteries) disponibles au cycle 0. Permet de faire fonctionner les machines et transports immédiatement sans attendre l'extraction brute."));
        initialEnergySpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        initialFoodSpinner = new Spinner<>(0.0, 120.0, 6.0, 1.0);
        initialFoodSpinner.setEditable(true);
        initialFoodSpinner.setMaxWidth(Double.MAX_VALUE);
        initialFoodSpinner.setTooltip(new Tooltip("🌾 Réserves Alimentaires Initiales (F₀) [mois de subsistance] :\nVivres et céréales stockées dans les greniers et silos au démarrage. Sert de tampon métabolique pour protéger les populations contre les famines et chocs climatiques initiaux."));
        initialFoodSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        initialInfoSpinner = new Spinner<>(0.0, 1_000_000_000.0, 100.0, 100.0);
        initialInfoSpinner.setEditable(true);
        initialInfoSpinner.setMaxWidth(Double.MAX_VALUE);
        initialInfoSpinner.setTooltip(new Tooltip("🧠 Capital d'Information & Savoir Archivé (I₀) [bits/habitant] :\nSavoirs techniques, écritures, brevets et archives numérisées. Détermine l'efficacité d'apprentissage, l'absorption des innovations et la résilience en cas de destruction du capital physique."));
        initialInfoSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        energyLabel = new Label("⚡ Stock Énergétique (E₀) (MJ/hab) :");
        foodLabel = new Label("🌾 Réserves Alimentaires (F₀) (mois) :");
        infoLabel = new Label("🧠 Capital Informationnel (I₀) (bits/hab) :");
        Tooltip.install(energyLabel, initialEnergySpinner.getTooltip());
        Tooltip.install(foodLabel, initialFoodSpinner.getTooltip());
        Tooltip.install(infoLabel, initialInfoSpinner.getTooltip());

        GridPane secondaryGrid = new GridPane();
        secondaryGrid.setHgap(10);
        secondaryGrid.setVgap(8);
        secondaryGrid.addRow(0, energyLabel, initialEnergySpinner);
        secondaryGrid.addRow(1, foodLabel, initialFoodSpinner);
        secondaryGrid.addRow(2, infoLabel, initialInfoSpinner);

        TitledPane secondaryPane = new TitledPane("🔋 PARAMÈTRES PHYSIQUES SECONDAIRES & RÉSERVES DE DÉPART (OPTIONNELS)", secondaryGrid);
        secondaryPane.setExpanded(false);
        secondaryPane.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold;");

        exportDensityMapBtn = new Button("📤 Exporter Carte de Densité Générée (PNG)");
        exportDensityMapBtn.getStyleClass().add("button-secondary");
        exportDensityMapBtn.setMaxWidth(Double.MAX_VALUE);
        exportDensityMapBtn.setOnAction(e -> exportDensityMap());

        VBox proceduralDemoPanel = new VBox(8, popGrid, secondaryPane, exportDensityMapBtn);
        proceduralDemoPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");

        // Import Panel
        densityMapFileLabel = new Label(org.ether.society.i18n.I18n.get("planet.map.none"));
        densityMapFileLabel.getStyleClass().add("value-label");

        demoCompatibilityLabel = new Label("🪐 Validation terrain : Aucune carte externe chargée");
        demoCompatibilityLabel.setWrapText(true);
        demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        densityFormatHintLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.format.density_hint",
                "PNG / JPEG (projection équirectangulaire 2:1) :\n  Noir (0) = 0 hab/km² | Blanc (255) = Densité maximale d'habitation."));
        densityFormatHintLabel.setWrapText(true);
        densityFormatHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic; -fx-padding: 4 0 0 0;");

        loadDensityMapBtn = new Button("📥 Importer Carte de Densité Externe (PNG)");
        loadDensityMapBtn.getStyleClass().add("button-secondary");
        loadDensityMapBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(loadDensityMapBtn, Priority.ALWAYS);
        loadDensityMapBtn.setOnAction(e -> {
            notifyParamChange();
            loadCustomDensityMap();
        });

        demoHelpBtn = new Button("❓ Format");
        demoHelpBtn.getStyleClass().add("button-secondary");
        demoHelpBtn.setStyle("-fx-font-size: 11px;");
        demoHelpBtn.setTooltip(new Tooltip("Afficher le mode d'emploi et les spécifications de format d'image."));
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
            if (currentPreviewCells != null) {
                distributeInitialPopulation(currentPreviewCells);
                drawPreview();
            }
        });

        // Live preview listeners
        initialHumanCountSpinner.valueProperty().addListener((obs, oldV, newV) -> { notifyParamChange(); if (currentPreviewCells != null) { distributeInitialPopulation(currentPreviewCells); drawPreview(); } });
        densityPatternCombo.valueProperty().addListener((obs, oldV, newV) -> { notifyParamChange(); if (currentPreviewCells != null) { distributeInitialPopulation(currentPreviewCells); drawPreview(); } });
        urbanCentersSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        popSection.getChildren().addAll(popHeader, radioProcDemo, proceduralDemoPanel, radioImportDemo, importDemoPanel);

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
        generateBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.preview", "Aperçu rapide de la répartition démographique.")));

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        progressStatusLabel = new Label();
        progressStatusLabel.getStyleClass().add("control-label");
        progressStatusLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        progressStatusLabel.setVisible(false);
        progressStatusLabel.setManaged(false);

        Button btnPreFlight = new Button("📋 Diagnostic de Viabilité Civilisationnelle");
        btnPreFlight.getStyleClass().add("button-secondary");
        btnPreFlight.setMaxWidth(Double.MAX_VALUE);
        btnPreFlight.setStyle("-fx-font-weight: bold; -fx-text-fill: #eab308;");
        btnPreFlight.setOnAction(e -> runPreFlightSanityCheck());

        Button btnExportBundle = new Button("📦 Exporter Bundle (.ether)");
        btnExportBundle.getStyleClass().add("button-secondary");
        btnExportBundle.setMaxWidth(Double.MAX_VALUE);
        btnExportBundle.setStyle("-fx-text-fill: #38bdf8;");
        btnExportBundle.setOnAction(e -> exportUnifiedBundle());

        Button btnImportBundle = new Button("📂 Importer Bundle (.ether)");
        btnImportBundle.getStyleClass().add("button-secondary");
        btnImportBundle.setMaxWidth(Double.MAX_VALUE);
        btnImportBundle.setStyle("-fx-text-fill: #a78bfa;");
        btnImportBundle.setOnAction(e -> importUnifiedBundle());

        HBox bundleBox = new HBox(8, btnExportBundle, btnImportBundle);
        HBox.setHgrow(btnExportBundle, Priority.ALWAYS);
        HBox.setHgrow(btnImportBundle, Priority.ALWAYS);

        startBtn = new Button();
        startBtn.setPrefHeight(50);
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
        startBtn.setOnAction(e -> handleStartOrCancel());
        startBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.start", "Calculer les cellules H3 et lancer la simulation.")));

        VBox bottom = new VBox(10, btnPreFlight, bundleBox, generateBtn, progressBar, progressStatusLabel, startBtn);
        VBox.setVgrow(bottom, Priority.ALWAYS);
        bottom.setAlignment(Pos.BOTTOM_CENTER);

        root.getChildren().addAll(scenarioPresetBar, inheritedSection, section1, popSection, clippingSection, oceanOptSection, eventsSection, bottom);
        return root;
    }

    private void exportUnifiedBundle() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter le Bundle de Scénario Unifié (.ether)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Ether (*.ether, *.json)", "*.ether", "*.json"));
        chooser.setInitialFileName("mon-scenario-ether.ether");
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                PlanetPreset planet = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
                if (planet == null) planet = PlanetPreset.EARTH_LIKE;
                EcologyPreset eco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : EcologyPreset.EARTH_STANDARD;
                Scenario scenario = getScenario();

                org.ether.society.model.EtherScenarioBundle bundle =
                        new org.ether.society.model.EtherScenarioBundle("2.0.0", planet, eco, scenario);

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(file, bundle);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Exportation Réussie");
                alert.setHeaderText("Bundle de Simulation Exporté avec Succès");
                alert.setContentText("Le bundle unifié contenant la physique, l'écologie et la démographie a été enregistré sous :\n" + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception ex) {
                logger.error("Failed to export unified bundle", ex);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur d'Exportation");
                alert.setContentText("Impossible d'enregistrer le bundle : " + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void importUnifiedBundle() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importer un Bundle de Scénario Unifié (.ether)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Ether (*.ether, *.json)", "*.ether", "*.json"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                org.ether.society.model.EtherScenarioBundle bundle =
                        mapper.readValue(file, org.ether.society.model.EtherScenarioBundle.class);

                if (bundle != null) {
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
                    alert.setTitle("Importation Réussie");
                    alert.setHeaderText("Bundle Unifié Appliqué");
                    alert.setContentText("Les paramètres physiques, écologiques et démographiques du bundle ont été rechargés avec succès.");
                    alert.showAndWait();
                }
            } catch (Exception ex) {
                logger.error("Failed to import unified bundle", ex);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur d'Importation");
                alert.setContentText("Le fichier bundle sélectionné n'est pas valide : " + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void runPreFlightSanityCheck() {
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
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
            warnings.add("⚠️ Ressources en Eau Limités : Monde très aride. Stress hydrique majeur prévisible.");
        } else {
            passes.add("✅ Hydrologie équilibrée (Niveau d'eau = " + String.format("%.0f%%", (1.0 + p.waterLevel()) * 50) + ")");
        }

        double capitalK0 = initialCapitalSpinner != null ? initialCapitalSpinner.getValue() : 1000.0;
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
                    passes.add("⚡ Fusion JIT Moteurs (" + entry.getVariableName() + ") : " + entry.getDescription());
                }
            }
        } else {
            passes.add("⚡ Compilation JIT Moteurs : 100% Compatible & Fusions Validées");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📋 Diagnostic de Viabilité Civilisationnelle (Scénario '").append(scenarioPresetBar != null ? scenarioPresetBar.getCurrentName() : "Custom").append("') :\n\n");

        if (!warnings.isEmpty()) {
            sb.append("⚠️ ALERTES DE VIABILITÉ & TENSIONS :\n");
            for (String w : warnings) sb.append("• ").append(w).append("\n");
            sb.append("\n");
        }

        sb.append("✔️ POINTS DE STABILITÉ CONFIRMÉS :\n");
        for (String pass : passes) sb.append("• ").append(pass).append("\n");

        Alert alert = new Alert(warnings.isEmpty() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        alert.setTitle("Diagnostic de Viabilité Civilisationnelle");
        alert.setHeaderText(warnings.isEmpty() ? "✅ Scénario Viable et Équilibré" : "⚠️ Tension(s) ou Risque(s) Détecté(s)");
        alert.setContentText(sb.toString());
        alert.getDialogPane().setPrefWidth(520);
        alert.showAndWait();
    }

    private VBox createSection(Label header, javafx.scene.Node content) {
        header.getStyleClass().add("label-section-header");
        VBox box = new VBox(8, header, content);
        box.getStyleClass().add("card-section");
        return box;
    }

    private List<Scenario> getBuiltInScenarios() {
        List<Scenario> list = new ArrayList<>();

        // --- SCÉNARIOS DU PASSÉ ---
        Scenario s0 = new Scenario();
        s0.setName("Sortie d'Afrique & Expansion Homo Sapiens (-100000)");
        s0.setStartDateYear(-100000);
        s0.setInitialHumanCount(50000);
        s0.setInitialCapitalPerCapita(2.0);
        s0.setInitialEnergyPerCapita(5.0);
        s0.setInitialFoodReserveMonths(2.0);
        s0.setInitialInformationPerCapita(2.0);
        s0.setPopulationDensityType("ONE_CONTINENT");
        s0.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s0.setDescription("""
            🌍 SCÉNARIO PALÉOLITHIQUE : Berceau Africain, Traversée des Continents & Out of Africa (-100 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise la dynamique démographique et l'expansion spatiale des premières populations d'Homo Sapiens depuis l'Afrique de l'Est à travers le Moyen-Orient, l'Eurasie, l'Océanie et les Amériques.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Population Initiale : 50 000 individus (Capacité nomade pré-agricole).
            • Stock Capital Physique (K₀) : 2 kg/habitant (bifaces en pierre, javelots, bifaces).
            • Énergie Stockée (E₀) : 5 MJ/habitant (maîtrise du feu et combustible bois).
            • Réserves Alimentaires (F₀) : 2 mois de subsistance en chasse-cueillette.
            • Savoir Archivé (I₀) : 2 bits/habitant (traditions orales paléolithiques & langage).
            """);
        list.add(s0);

        Scenario s1 = new Scenario();
        s1.setName("Croissant Fertile & Néolithique (-8000)");
        s1.setStartDateYear(-8000);
        s1.setInitialHumanCount(25000);
        s1.setInitialCapitalPerCapita(5.0);
        s1.setInitialEnergyPerCapita(10.0);
        s1.setInitialFoodReserveMonths(3.0);
        s1.setInitialInformationPerCapita(5.0);
        s1.setPopulationDensityType("FERTILE_CRESCENT");
        s1.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s1.setClippingEnabled(true);
        s1.setMinLat(25.0); s1.setMaxLat(42.0); s1.setMinLng(25.0); s1.setMaxLng(55.0);
        s1.setBoundaryMode("DYNAMIC_RESERVOIR");
        s1.setDescription("""
            🌾 SCÉNARIO HISTORIQUE : L'Aube de l'Agriculture au Croissant Fertile (-8000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Ce scénario modélise la transition majeure du Néolithique entre l'économie de subsistance des chasseurs-cueilleurs et l'émergence des premières communautés agricoles sédentaires le long du Tigre, de l'Euphrate, du Nil et de la côte Levantine.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Stock Capital Physique (K₀) : 5 kg/habitant (outillage en silex, bois, vannerie).
            • Énergie Stockée (E₀) : 10 MJ/habitant (combustible bois & biomosse).
            • Réserves Alimentaires (F₀) : 3 mois de subsistance en baies, graines et viande séchée.
            • Savoir Archivé (I₀) : 5 bits/habitant (tradition orale & transmission du savoir-faire).
            • Température Moyenne du Globe : ~14.0°C (Fin de la glaciation du Würm, Climat Holocène doux).
            """);
        list.add(s1);

        // --- SCÉNARIO : SAHARA VERT (PÉRIODE HUMIDE AFRICAINE -6000) ---
        Scenario sGreenSahara = new Scenario();
        sGreenSahara.setName("Le Sahara Vert & Période Humide Africaine (-6000)");
        sGreenSahara.setStartDateYear(-6000);
        sGreenSahara.setInitialHumanCount(60000);
        sGreenSahara.setInitialCapitalPerCapita(6.0);
        sGreenSahara.setInitialEnergyPerCapita(12.0);
        sGreenSahara.setInitialFoodReserveMonths(4.0);
        sGreenSahara.setInitialInformationPerCapita(10.0);
        sGreenSahara.setPopulationDensityType("GREEN_SAHARA");
        sGreenSahara.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sGreenSahara.setDescription("""
            🌴 SCÉNARIO PALÉOCLIMATIQUE : Le Sahara Vert & Période Humide Africaine (-6000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise la Période Humide Africaine (AHP) où l'insolation printanière amplifiée par l'orbite terrestre a intensifié la mousson africaine. Le désert du Sahara était alors une savane verdoyante parsemée de lac majeurs (Lac Méga-Tchad), peuplée d'éleveurs néolithiques et de chasseurs-cueilleurs.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Précipitations Sahariennes : 800 à 1200 mm/an (Savane arborée & lacs).
            • Stock Capital Physique (K₀) : 6 kg/habitant (poterie pastorale, harpons en os).
            • Biomasse Halieutique & Lacustre (B_fish) : Abondance maximale le long des berges lacustres.
            """);
        list.add(sGreenSahara);

        // --- SCÉNARIO : RÉCENTS DRYAS (-10900) ---
        Scenario sYoungerDryas = new Scenario();
        sYoungerDryas.setName("Le Récents Dryas & Choc Climatique Natufien (-10900)");
        sYoungerDryas.setStartDateYear(-10900);
        sYoungerDryas.setInitialHumanCount(40000);
        sYoungerDryas.setInitialCapitalPerCapita(4.0);
        sYoungerDryas.setInitialEnergyPerCapita(8.0);
        sYoungerDryas.setInitialFoodReserveMonths(2.5);
        sYoungerDryas.setInitialInformationPerCapita(8.0);
        sYoungerDryas.setPopulationDensityType("YOUNGER_DRYAS");
        sYoungerDryas.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sYoungerDryas.setDescription("""
            ❄️ SCÉNARIO PALÉOCLIMATIQUE : Le Récents Dryas & Pression Foragère Au Levant (-10 900 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Refroidissement brutal de 5 à 8°C de l'Atlantique Nord déclenché par le déversement d'eau douce du Lac Agassiz. Au Levant, la sécheresse aiguë réduit les céréales sauvages, contraignant les populations Natufiennes à la sédentarisation pré-agricole et au contrôle des graines.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Refroidissement Stratosphérique : -5.5°C au-dessus de l'Eurasie.
            • Sécheresse Levantine : Précipitations chutant sous 250 mm/an.
            • Capital Lithique (K₀) : 4 kg/habitant (faucilles en silex, mortiers en pierre).
            """);
        list.add(sYoungerDryas);

        Scenario s2 = new Scenario();
        s2.setName("Le Petit Âge Glaciaire de l'Antiquité Tardive & Peste de Justinien (536)");
        s2.setStartDateYear(536);
        s2.setInitialHumanCount(180000000);
        s2.setInitialCapitalPerCapita(250.0);
        s2.setInitialEnergyPerCapita(100.0);
        s2.setInitialFoodReserveMonths(1.5);
        s2.setInitialInformationPerCapita(300.0);
        s2.setPopulationDensityType("URBAN_CLUSTERS");
        s2.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s2.setDescription("""
            🌋 SCÉNARIO HISTORIQUE : L'Anomalie Climatique Volcanique de 536 & Choc Sanitaire
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            L'année 536 est considérée par les historiens du climat comme "la pire année de l'histoire humaine". Deux éruptions volcaniques super-massives consécutives (Ilopango et Krakatoa) ont injecté un voile d'aérosols stratosphériques occultant le Soleil pendant 18 mois.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Stock Capital Physique (K₀) : 250 kg/habitant (outils fer, fermes, infrastructures romaines).
            • Énergie Stockée (E₀) : 100 MJ/habitant (réserves de bois de chauffe).
            • Réserves Alimentaires (F₀) : 1.5 mois (famine aiguë post-éruption volcanique).
            • Savoir Archivé (I₀) : 300 bits/habitant (manuscrits, parchemins, administration impériale).
            """);
        list.add(s2);

        Scenario s3 = new Scenario();
        s3.setName("Empire Assyrien & Irrigation Mésopotamienne (-2000)");
        s3.setStartDateYear(-2000);
        s3.setInitialHumanCount(500000);
        s3.setInitialCapitalPerCapita(80.0);
        s3.setInitialEnergyPerCapita(50.0);
        s3.setInitialFoodReserveMonths(6.0);
        s3.setInitialInformationPerCapita(50.0);
        s3.setPopulationDensityType("MESOPOTAMIA_ASSYRIA");
        s3.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s3.setClippingEnabled(true);
        s3.setMinLat(28.0); s3.setMaxLat(40.0); s3.setMinLng(38.0); s3.setMaxLng(52.0);
        s3.setBoundaryMode("DYNAMIC_RESERVOIR");
        s3.setDescription("""
            🏛️ SCÉNARIO HISTORIQUE : Hydraulique, Salinisation & Guerre Cinétique Assyrienne (-2000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'apogée et les vulnérabilités de la civilisation mésopotamienne et de l'Empire Assyrien basés sur l'irrigation intensive à partir du Tigre et de l'Euphrate.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Stock Capital Physique (K₀) : 80 kg/habitant (outillage bronze, canaux, chars).
            • Énergie Stockée (E₀) : 50 MJ/habitant (bois, réserves d'huile & fourrage).
            • Réserves Alimentaires (F₀) : 6 mois (silos à grain urbains Mésopotamiens).
            • Savoir Archivé (I₀) : 50 bits/habitant (cunéiforme & comptabilité argile).
            """);
        list.add(s3);

        Scenario s4 = new Scenario();
        s4.setName("Dynastie Song & Pré-Industrialisation Hydraulique (1000)");
        s4.setStartDateYear(1000);
        s4.setInitialHumanCount(100000000);
        s4.setInitialCapitalPerCapita(600.0);
        s4.setInitialEnergyPerCapita(500.0);
        s4.setInitialFoodReserveMonths(8.0);
        s4.setInitialInformationPerCapita(1200.0);
        s4.setPopulationDensityType("RIVER_VALLEYS");
        s4.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s4.setDescription("""
            🏮 SCÉNARIO HISTORIQUE : Le Siècle d'Or de la Dynastie Song (1000 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            La Chine des Song a connu la première pré-industrialisation de l'histoire, avec une utilisation massive du charbon de terre pour la fonte du fer et des réseaux de transport fluviaux ultra-efficaces.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Stock Capital Physique (K₀) : 600 kg/habitant (moulins hydrauliques, hauts fourneaux charbon, jonques).
            • Énergie Stockée (E₀) : 500 MJ/habitant (stocks de charbon de terre & bois).
            • Réserves Alimentaires (F₀) : 8 mois (greniers d'État Song & riziculture Champa).
            • Savoir Archivé (I₀) : 1200 bits/habitant (imprimerie typographique, papier monnaie).
            """);
        list.add(s4);

        // --- SCÉNARIOS DU FUTUR ---
        Scenario s5 = new Scenario();
        s5.setName("Business As Usual : Fossil Fuel Reliance & Warming (SSP5-8.5)");
        s5.setStartDateYear(2026);
        s5.setInitialHumanCount(8200000000L);
        s5.setInitialCapitalPerCapita(15000.0);
        s5.setInitialEnergyPerCapita(25000.0);
        s5.setInitialFoodReserveMonths(9.0);
        s5.setInitialInformationPerCapita(5000000.0);
        s5.setPopulationDensityType("URBAN_CLUSTERS");
        s5.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s5.setDescription("""
            📉 SCÉNARIO FUTUR : Business As Usual (Trajectoire GIEC SSP5-8.5)
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Poursuite de l'extraction des combustibles fossiles traditionnels sans déploiement massif de la fusion ni captage du carbone.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Stock Capital Physique (K₀) : 15 000 kg/habitant (infrastructures lourdes, réseaux, machines).
            • Énergie Stockée (E₀) : 25 000 MJ/habitant (stocks pétroliers, gaziers & charbon).
            • Réserves Alimentaires (F₀) : 9 mois (logistique agro-alimentaire mondiale).
            • Savoir Archivé (I₀) : 5 000 000 bits/habitant (Internet, bibliothèques numériques, brevets).
            """);
        list.add(s5);

        Scenario s6 = new Scenario();
        s6.setName("Singularité Technologique, ASI & Fusion D-T (2045)");
        s6.setStartDateYear(2045);
        s6.setInitialHumanCount(9000000000L);
        s6.setInitialCapitalPerCapita(50000.0);
        s6.setInitialEnergyPerCapita(100000.0);
        s6.setInitialFoodReserveMonths(24.0);
        s6.setInitialInformationPerCapita(100000000.0);
        s6.setPopulationDensityType("URBAN_CLUSTERS");
        s6.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s6.setDescription("""
            🤖 SCÉNARIO FUTUR : Singularité Technologique & Énergie de Fusion D-T
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Franchissement du seuil d'émergence d'une Super-Intelligence Artificielle (ASI) et maîtrise industrielle de la fusion nucléaire deutérium-tritium.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Stock Capital Physique (K₀) : 50 000 kg/habitant (robotique moléculaire, fonderies orbitales).
            • Énergie Stockée (E₀) : 100 000 MJ/habitant (fusion D-T & super-batteries).
            • Réserves Alimentaires (F₀) : 24 mois (synthèse protéique fermée & hydroponie géante).
            • Savoir Archivé (I₀) : 100 000 000 bits/habitant (Super-Intelligence Artificielle).
            """);
        list.add(s6);

        Scenario s7 = new Scenario();
        s7.setName("Hiver Nucléaire & Ombre Stratosphérique (2035)");
        s7.setStartDateYear(2035);
        s7.setInitialHumanCount(8500000000L);
        s7.setInitialCapitalPerCapita(18000.0);
        s7.setInitialEnergyPerCapita(1500.0);
        s7.setInitialFoodReserveMonths(1.5);
        s7.setInitialInformationPerCapita(500000.0);
        s7.setPopulationDensityType("URBAN_CLUSTERS");
        s7.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s7.setDescription("""
            ☢️ SCÉNARIO FUTUR : Catastrophe de la Guerre Nucléaire & Hiver Stratosphérique
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Conflit nucléaire à haute intensité déclenchant d'immenses tempêtes de feu urbaines et l'injection massive de carbone suie dans la stratosphère.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Stock Capital Physique (K₀) : 18 000 kg/habitant (infrastructures modernes dégradées).
            • Énergie Stockée (E₀) : 1 500 MJ/habitant (réseaux électriques et stocks pétroliers fragmentés).
            • Réserves Alimentaires (F₀) : 1.5 mois (effondrement logistique et gel des cultures).
            • Savoir Archivé (I₀) : 500 000 bits/habitant (serveurs et réseaux isolés).
            """);
        list.add(s7);

        Scenario s8 = new Scenario();
        s8.setName("Falaise du Phosphate Minéral & Crise N-P-K (2050)");
        s8.setStartDateYear(2050);
        s8.setInitialHumanCount(9500000000L);
        s8.setInitialCapitalPerCapita(22000.0);
        s8.setInitialEnergyPerCapita(12000.0);
        s8.setInitialFoodReserveMonths(4.0);
        s8.setInitialInformationPerCapita(2000000.0);
        s8.setPopulationDensityType("URBAN_CLUSTERS");
        s8.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s8.setDescription("""
            ⛏️ SCÉNARIO FUTUR : Épuisement du Phosphate de Roche (Peak P 2050)
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Épuisement géologique complet des gisements de phosphate de roche bon marché sans transition vers un recyclage circulaire intégral.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Stock Capital Physique (K₀) : 22 000 kg/habitant (infrastructure technologique poussée).
            • Énergie Stockée (E₀) : 12 000 MJ/habitant (transition renouvelable/nucléaire partielle).
            • Réserves Alimentaires (F₀) : 4.0 mois (crise d'engrais NPK réduisant les récoltes de 50%).
            • Savoir Archivé (I₀) : 2 000 000 bits/habitant (mémoire numérique mondiale).
            """);
        list.add(s8);

        Scenario s9 = new Scenario();
        s9.setName("Super-Éruption Volcanique Toba/Yellowstone (2060)");
        s9.setStartDateYear(2060);
        s9.setInitialHumanCount(9800000000L);
        s9.setInitialCapitalPerCapita(25000.0);
        s9.setInitialEnergyPerCapita(20000.0);
        s9.setInitialFoodReserveMonths(3.0);
        s9.setInitialInformationPerCapita(5000000.0);
        s9.setPopulationDensityType("URBAN_CLUSTERS");
        s9.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s9.setDescription("""
            🌋 SCÉNARIO FUTUR : Super-Volcan VEI-8 & Refroidissement Vulcanologique
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Éruption super-volcanique de degré VEI-8 éjectant plus de 1000 km³ de cendres et de dioxyde de soufre (SO2) dans la haute atmosphère.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Stock Capital Physique (K₀) : 25 000 kg/habitant (infrastructures avancées et serres automatisées).
            • Énergie Stockée (E₀) : 20 000 MJ/habitant (centrales nucléaires et géothermiques).
            • Réserves Alimentaires (F₀) : 3.0 mois (destructions agricoles par cendres).
            • Savoir Archivé (I₀) : 5 000 000 bits/habitant (savoir automatisé & archives).
            """);
        list.add(s9);

        return list;
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
            if (targetCohortSizeSpinner != null && targetCohortSizeSpinner.getValueFactory() != null) {
                targetCohortSizeSpinner.getValueFactory().setValue(s.getTargetCohortSize() > 0 ? s.getTargetCohortSize() : 500);
            }
            if (initialHumanCountSpinner != null && initialHumanCountSpinner.getValueFactory() != null) {
                initialHumanCountSpinner.getValueFactory().setValue(s.getInitialHumanCount());
            }
            if (initialCapitalSpinner != null && initialCapitalSpinner.getValueFactory() != null) {
                initialCapitalSpinner.getValueFactory().setValue(s.getInitialCapitalPerCapita());
            }
            if (initialEnergySpinner != null && initialEnergySpinner.getValueFactory() != null) {
                initialEnergySpinner.getValueFactory().setValue(s.getInitialEnergyPerCapita());
            }
            if (initialFoodSpinner != null && initialFoodSpinner.getValueFactory() != null) {
                initialFoodSpinner.getValueFactory().setValue(s.getInitialFoodReserveMonths());
            }
            if (initialInfoSpinner != null && initialInfoSpinner.getValueFactory() != null) {
                initialInfoSpinner.getValueFactory().setValue(s.getInitialInformationPerCapita());
            }

            if (s.getPopulationDensityType() != null && densityPatternCombo != null && densityPatternCombo.getItems().contains(s.getPopulationDensityType())) {
                densityPatternCombo.setValue(s.getPopulationDensityType());
            }
            if (s.getSeed() != 0 && demoSeedField != null) {
                demoSeedField.setText(String.valueOf(s.getSeed()));
            }
            if (randomEventsCheckBox != null) {
                randomEventsCheckBox.setSelected(s.isRandomEventsEnabled());
            }
            if (clippingCheckBox != null) {
                clippingCheckBox.setSelected(s.isClippingEnabled());
                if (minLatSpinner != null && minLatSpinner.getValueFactory() != null) minLatSpinner.getValueFactory().setValue(s.getMinLat());
                if (maxLatSpinner != null && maxLatSpinner.getValueFactory() != null) maxLatSpinner.getValueFactory().setValue(s.getMaxLat());
                if (minLngSpinner != null && minLngSpinner.getValueFactory() != null) minLngSpinner.getValueFactory().setValue(s.getMinLng());
                if (maxLngSpinner != null && maxLngSpinner.getValueFactory() != null) maxLngSpinner.getValueFactory().setValue(s.getMaxLng());
                if (s.getBoundaryMode() != null && boundaryModeCombo != null) boundaryModeCombo.setValue(s.getBoundaryMode());
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
            if (s.getCustomDensityBase64() != null) {
                customDensityImage = org.ether.society.data.ImageMapLoader.base64PngToImage(s.getCustomDensityBase64());
                if (densityMapFileLabel != null) densityMapFileLabel.setText("📷 Preset Density Map");
                updateDemoCompatibilityDisplay();
            } else {
                customDensityImage = null;
                if (densityMapFileLabel != null) densityMapFileLabel.setText(org.ether.society.i18n.I18n.get("planet.map.none"));
                updateDemoCompatibilityDisplay();
            }
            if (currentPreviewCells != null && !currentPreviewCells.isEmpty()) {
                distributeInitialPopulation(currentPreviewCells);
                drawPreview();
            } else if (previewCanvas != null) {
                generatePreview();
            }
        } finally {
            isUpdatingFromPreset = false;
        }
    }

    private VBox createClippingSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        clippingHeader = new Label(I18n.getOrDefault("scenario.clipping.header", "✂️ SIMULATION LOCALE & FRONTIÈRES (CLIPPING)"));
        clippingHeader.getStyleClass().add("label-header");
        clippingHeader.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        clippingCheckBox = new CheckBox(I18n.getOrDefault("scenario.clipping.enable", "Activer la simulation partielle (Zone Tronquée)"));
        clippingCheckBox.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold;");
        clippingCheckBox.setOnAction(e -> {
            notifyParamChange();
            drawPreview();
        });

        graphicSelectBtn = new ToggleButton(I18n.getOrDefault("scenario.clipping.select_mode", "🖱️ Mode Sélection Graphique sur Carte"));
        graphicSelectBtn.setMaxWidth(Double.MAX_VALUE);
        graphicSelectBtn.setStyle("-fx-font-size: 11px;");
        graphicSelectBtn.setTooltip(new Tooltip("Activez ce bouton ou maintenez SHIFT enfoncé pour dessiner un rectangle de sélection sur la carte preview."));

        GridPane boundsGrid = new GridPane();
        boundsGrid.setHgap(8);
        boundsGrid.setVgap(6);

        maxLatSpinner = new Spinner<>(-90.0, 90.0, 90.0, 1.0);
        minLatSpinner = new Spinner<>(-90.0, 90.0, -90.0, 1.0);
        minLngSpinner = new Spinner<>(-180.0, 180.0, -180.0, 1.0);
        maxLngSpinner = new Spinner<>(-180.0, 180.0, 180.0, 1.0);

        for (Spinner<Double> sp : List.of(maxLatSpinner, minLatSpinner, minLngSpinner, maxLngSpinner)) {
            sp.setEditable(true);
            sp.setPrefWidth(90);
            sp.valueProperty().addListener((obs, old, val) -> {
                notifyParamChange();
                drawPreview();
            });
        }

        latMaxLabel = new Label(I18n.getOrDefault("scenario.clipping.lat_max", "Lat Max (Haut) :"));
        latMinLabel = new Label(I18n.getOrDefault("scenario.clipping.lat_min", "Lat Min (Bas) :"));
        lngMinLabel = new Label(I18n.getOrDefault("scenario.clipping.lng_min", "Lng Min (Gau.) :"));
        lngMaxLabel = new Label(I18n.getOrDefault("scenario.clipping.lng_max", "Lng Max (Dro.) :"));
        boundsGrid.addRow(0, latMaxLabel, maxLatSpinner, latMinLabel, minLatSpinner);
        boundsGrid.addRow(1, lngMinLabel, minLngSpinner, lngMaxLabel, maxLngSpinner);

        boundaryLabel = new Label(I18n.getOrDefault("scenario.clipping.boundary_label", "Modélisation Scientifique des Frontières :"));
        boundaryLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        boundaryModeCombo = new ComboBox<>();
        boundaryModeCombo.getItems().addAll(
            "DYNAMIC_RESERVOIR",
            "CLOSED_BARRIER",
            "PERIODIC_WRAP"
        );
        boundaryModeCombo.setValue("DYNAMIC_RESERVOIR");
        boundaryModeCombo.setMaxWidth(Double.MAX_VALUE);
        boundaryModeCombo.setOnAction(e -> notifyParamChange());
        boundaryModeCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case "DYNAMIC_RESERVOIR" -> setText(I18n.getOrDefault("scenario.boundary.reservoir", "🌊 Réservoir Virtuel Extérieur (Flux Libres)"));
                        case "CLOSED_BARRIER" -> setText(I18n.getOrDefault("scenario.boundary.barrier", "🧱 Frontière Étanche / Isolée (Bords Fermés)"));
                        case "PERIODIC_WRAP" -> setText(I18n.getOrDefault("scenario.boundary.wrap", "🌐 Raccordement Périodique (Torique)"));
                        default -> setText(item);
                    }
                }
            }
        });
        boundaryModeCombo.setButtonCell(boundaryModeCombo.getCellFactory().call(null));

        resetClippingBtn = new Button(I18n.getOrDefault("scenario.clipping.reset", "🔄 Réinitialiser la Zone (Pleine Planète)"));
        resetClippingBtn.setMaxWidth(Double.MAX_VALUE);
        resetClippingBtn.setStyle("-fx-font-size: 11px;");
        resetClippingBtn.setOnAction(e -> {
            if (minLatSpinner != null && minLatSpinner.getValueFactory() != null) minLatSpinner.getValueFactory().setValue(-90.0);
            if (maxLatSpinner != null && maxLatSpinner.getValueFactory() != null) maxLatSpinner.getValueFactory().setValue(90.0);
            if (minLngSpinner != null && minLngSpinner.getValueFactory() != null) minLngSpinner.getValueFactory().setValue(-180.0);
            if (maxLngSpinner != null && maxLngSpinner.getValueFactory() != null) maxLngSpinner.getValueFactory().setValue(180.0);
            notifyParamChange();
            drawPreview();
        });

        VBox clippingSubPanel = new VBox(8, graphicSelectBtn, boundsGrid, boundaryLabel, boundaryModeCombo, resetClippingBtn);
        clippingSubPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        clippingSubPanel.setVisible(false);
        clippingSubPanel.setManaged(false);

        clippingCheckBox.setOnAction(e -> {
            boolean active = clippingCheckBox.isSelected();
            clippingSubPanel.setVisible(active);
            clippingSubPanel.setManaged(active);
            notifyParamChange();
            drawPreview();
        });

        section.getChildren().addAll(clippingHeader, clippingCheckBox, clippingSubPanel);
        return section;
    }

    private VBox createOceanOptimizationSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        Label oceanOptHeader = new Label(I18n.getOrDefault("scenario.ocean_opt.header", "🌊 OPTIMISATIONS ET PERFORMANCES OCÉANIQUES"));
        oceanOptHeader.getStyleClass().add("label-header");
        oceanOptHeader.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        Label oceanOptDesc = new Label(I18n.getOrDefault("scenario.ocean_opt.desc", "Ajuster les simplifications algorithmiques pour les cellules d'eau profonde afin de maximiser le nombre de TPS (Ticks Par Seconde)."));
        oceanOptDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        oceanOptDesc.setWrapText(true);

        oceanMacroAggregationCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.macro_aggregation", "🌊 Macro-agrégation Océanique (Traiter les bassins profonds en blocs virtuels)"));
        oceanMacroAggregationCheckBox.setSelected(true);
        oceanMacroAggregationCheckBox.setStyle("-fx-text-fill: #e2e8f0;");
        oceanMacroAggregationCheckBox.setTooltip(new Tooltip("Regroupe les cellules d'eau profonde pour éviter le calcul individuel de micro-courants inutiles."));
        oceanMacroAggregationCheckBox.setOnAction(e -> notifyParamChange());

        coastalNavigationOnlyCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.coastal_nav", "⚓ Navigation Maritime Littorale Uniquement (Simplifier les routes hauturières)"));
        coastalNavigationOnlyCheckBox.setSelected(false);
        coastalNavigationOnlyCheckBox.setStyle("-fx-text-fill: #e2e8f0;");
        coastalNavigationOnlyCheckBox.setTooltip(new Tooltip("Restreint le pathfinding naval détaillé aux côtes et détroits commercialement actifs."));
        coastalNavigationOnlyCheckBox.setOnAction(e -> notifyParamChange());

        oceanMultiRateTickingCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.multirate", "⏱ Ticking Océanique Asynchrone / Multi-Cadence (Cadence réduite ×5)"));
        oceanMultiRateTickingCheckBox.setSelected(true);
        oceanMultiRateTickingCheckBox.setStyle("-fx-text-fill: #e2e8f0;");
        oceanMultiRateTickingCheckBox.setTooltip(new Tooltip("Exécute la mise à jour des dynamiques océaniques 1 tick sur 5 pour libérer du CPU aux sociétés terrestres."));
        oceanMultiRateTickingCheckBox.setOnAction(e -> notifyParamChange());

        VBox box = new VBox(6, oceanMacroAggregationCheckBox, coastalNavigationOnlyCheckBox, oceanMultiRateTickingCheckBox);
        box.setStyle("-fx-padding: 6 0 0 12;");

        section.getChildren().addAll(oceanOptHeader, oceanOptDesc, box);
        return section;
    }

    private VBox createPreviewPane() {
        VBox root = new VBox(10);
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER);

        previewTitleLabel = new Label(I18n.getOrDefault("scenario.preview_title", "🌍 DISTRIBUTION DÉMOGRAPHIQUE, EMPREINTE & PRESSION MALTHUSIENNE (T₀)"));
        previewTitleLabel.getStyleClass().add("label-header");

        previewModeCombo = new ComboBox<>();
        previewModeCombo.getItems().addAll(
                "🗺️ Densité Démographique (hab/km²)",
                "🌱 Empreinte Écologique & Pression Malthusienne"
        );
        previewModeCombo.setValue("🗺️ Densité Démographique (hab/km²)");
        previewModeCombo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        previewModeCombo.setOnAction(e -> drawPreview());

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        header.getChildren().addAll(previewTitleLabel, spacer1, previewModeCombo, spacer2);

        Pane canvasContainer = new StackPane();
        canvasContainer.setStyle("-fx-background-color: black; -fx-border-color: #475569;");
        previewCanvas = new Canvas(600, 400);

        canvasContainer.widthProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() > 0) {
                previewCanvas.setWidth(newV.doubleValue());
                drawPreview();
            }
        });
        canvasContainer.heightProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() > 0) {
                previewCanvas.setHeight(newV.doubleValue());
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
                double baseScale = Math.min(w / 360.0, h / 180.0) * 0.9;
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
                double baseScale = Math.min(w / 360.0, h / 180.0) * 0.9;
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

        previewStatusLabel = new Label("Cliquez sur « Démarrer la Simulation » pour calculer la grille H3");
        previewStatusLabel.getStyleClass().add("control-label");

        HBox legendBox = createLegend();
        root.getChildren().addAll(header, canvasContainer, legendBox, previewStatusLabel);
        return root;
    }

    private HBox createLegend() {
        HBox legend = new HBox(12);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(6, 12, 6, 12));
        legend.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-background-radius: 6;");

        HBox legendItems = new HBox(8);
        legendItems.setAlignment(Pos.CENTER);

        String[] defaultLabels = {
                "Eau / Inhabité (0 hab/km²)",
                "Faible (1 – 50 hab/km²)",
                "Moyenne (50 – 500 hab/km²)",
                "Élevée / Cité (500 – 2 500 hab/km²)",
                "Métropole (> 2 500 hab/km²)"
        };
        Color[] colors = {
                Color.rgb(30, 95, 165),
                Color.rgb(16, 185, 129),
                Color.rgb(234, 179, 8),
                Color.rgb(249, 115, 22),
                Color.rgb(239, 68, 68)
        };

        for (int i = 0; i < defaultLabels.length; i++) {
            javafx.scene.shape.Rectangle colorBox = new javafx.scene.shape.Rectangle(18, 10);
            colorBox.setFill(colors[i]);
            colorBox.setStroke(Color.GRAY);

            String key = switch (i) {
                case 0 -> "scenario.legend.water_uninhabited";
                case 1 -> "scenario.legend.low";
                case 2 -> "scenario.legend.medium";
                case 3 -> "scenario.legend.high";
                case 4 -> "scenario.legend.metropolis";
                default -> "";
            };

            Label lbl = new Label(org.ether.society.i18n.I18n.getOrDefault(key, defaultLabels[i]));
            lbl.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 10px;");

            HBox item = new HBox(3, colorBox, lbl);
            item.setAlignment(Pos.CENTER);
            legendItems.getChildren().add(item);
        }

        legend.getChildren().add(legendItems);
        return legend;
    }

    private void loadCustomDensityMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Charger une carte de densité de population (PNG/JPEG)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customDensityImage = new Image(new FileInputStream(file));
                densityMapFileLabel.setText("📷 " + file.getName());
                updateDemoCompatibilityDisplay();
                if (currentPreviewCells != null) {
                    distributeInitialPopulation(currentPreviewCells);
                    drawPreview();
                }
            } catch (Exception ex) {
                logger.error("Failed to load custom density map", ex);
            }
        }
    }

    private void updateDemoCompatibilityDisplay() {
        if (demoCompatibilityLabel == null) return;
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        String planetName = p != null ? p.name() : "Standard";
        if (customDensityImage != null) {
            demoCompatibilityLabel.setText(String.format(I18n.getOrDefault("scenario.demo.map_loaded", "✅ Carte chargée et compatible avec le monde choisi à l'onglet 1 (%s)"), planetName));
            demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
        } else {
            demoCompatibilityLabel.setText(I18n.getOrDefault("scenario.demo.no_map", "🪐 Aucune carte externe chargée — Mode procédural actif"));
            demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        }
    }

    private void exportDensityMap() {
        if (previewCanvas == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter la carte de densité de population (PNG)");
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
        dialog.setTitle(I18n.getOrDefault("scenario.dialog.density_help_title", "Spécifications de la Carte de Densité"));
        dialog.setHeaderText(I18n.getOrDefault("scenario.dialog.density_help_header", "Formats d'images supportés pour l'importation de la Densité de Population"));
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

    private void generatePreview() {
        if (previewStatusLabel != null) {
            previewStatusLabel.setText("⚡ Calcul de l'aperçu démographique en arrière-plan...");
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

                // Map elevation and biome images if preset is Earth or has custom Base64 map
                boolean isEarth = cfg == PlanetPreset.EARTH_LIKE || "earth".equalsIgnoreCase(cfg.elevationMapSource()) ||
                        (cfg.name() != null && (cfg.name().toLowerCase().contains("earth") || cfg.name().toLowerCase().contains("terre") || cfg.name().toLowerCase().contains("terran")));
                if (isEarth) {
                    try (var elevStream = getClass().getResourceAsStream("/maps/earth_elevation.png");
                         var biomeStream = getClass().getResourceAsStream("/maps/earth_biomes.png")) {
                        if (elevStream != null || biomeStream != null) {
                            new org.ether.society.data.ImageMapLoader().mapDataToCells(previewCells, elevStream, biomeStream);
                        }
                    } catch (Exception e) {
                        logger.warn("Could not load Earth elevation map image for preview", e);
                    }
                } else if (cfg.customElevBase64() != null && !cfg.customElevBase64().isBlank()) {
                    try {
                        Image customElev = org.ether.society.data.ImageMapLoader.base64PngToImage(cfg.customElevBase64());
                        Image customBiome = org.ether.society.data.ImageMapLoader.base64PngToImage(cfg.customBiomeBase64());
                        if (customElev != null) {
                            new org.ether.society.data.ImageMapLoader().mapImagesToCells(previewCells, customElev, customBiome, null, cfg.minAltitudeMeters(), cfg.maxAltitudeMeters());
                        }
                    } catch (Exception e) {
                        logger.warn("Could not decode custom Base64 elevation map image for preview", e);
                    }
                }

                distributeInitialPopulation(previewCells);

                javafx.application.Platform.runLater(() -> {
                    currentPreviewCells = previewCells;
                    drawPreview();
                    if (previewStatusLabel != null) {
                        previewStatusLabel.setText("Aperçu généré : " + previewCells.size() + " cellules.");
                    }
                });
            } catch (Exception ex) {
                logger.error("Failed to generate preview", ex);
                javafx.application.Platform.runLater(() -> {
                    if (previewStatusLabel != null) {
                        previewStatusLabel.setText("Erreur aperçu : " + ex.getMessage());
                    }
                });
            }
        }, "H3-Preview-Async-Thread").start();
    }

    private void distributeInitialPopulation(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        long totalPop = initialHumanCountSpinner != null && initialHumanCountSpinner.getValue() != null ? initialHumanCountSpinner.getValue() : 1_000_000L;
        double capitalK0 = initialCapitalSpinner != null && initialCapitalSpinner.getValue() != null ? initialCapitalSpinner.getValue() : 1000.0;
        String pattern = densityPatternCombo != null ? densityPatternCombo.getValue() : "UNBIASED_NATURAL";
        boolean isEarthPreset = activePlanetPreset != null && activePlanetPreset.name() != null && activePlanetPreset.name().toLowerCase().contains("earth");
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

        long pop = c.getPopulation() != null ? c.getPopulation() : 0;
        if (pop <= 0 || elev <= 0) {
            return baseTerrainCol;
        }

        boolean isFootprintMode = previewModeCombo != null && previewModeCombo.getValue() != null && previewModeCombo.getValue().contains("Empreinte");

        Color heatCol;
        if (isFootprintMode) {
            double capacity = computeCellCarryingCapacity(c);
            double ratio = capacity > 0 ? (double) pop / capacity : 2.0;

            if (ratio < 0.5) {
                heatCol = Color.rgb(16, 185, 129); // Vert / Sous la capacité portante (< 50%)
            } else if (ratio < 1.0) {
                heatCol = Color.rgb(234, 179, 8);  // Jaune / Charge Modérée (50% - 100%)
            } else if (ratio < 1.5) {
                heatCol = Color.rgb(249, 115, 22); // Orange / Tension Écologique (100% - 150%)
            } else {
                heatCol = Color.rgb(239, 68, 68);  // Rouge / Surpopulation Critique (> 150%)
            }
        } else {
            // Population Density Heat Overlay
            if (pop < 50) {
                heatCol = Color.rgb(16, 185, 129); // Faible (1 - 50 hab/km²)
            } else if (pop < 500) {
                heatCol = Color.rgb(234, 179, 8);  // Moyenne (50 - 500 hab/km²)
            } else if (pop < 2500) {
                heatCol = Color.rgb(249, 115, 22); // Élevée / Cité (500 - 2500 hab/km²)
            } else {
                heatCol = Color.rgb(239, 68, 68);  // Métropole (> 2500 hab/km²)
            }
        }

        // Blend heat with terrain relief (75% heat, 25% relief) so physical land topography shines through!
        double alpha = 0.75;
        int blendedR = (int) (baseTerrainCol.getRed() * 255 * (1.0 - alpha) + heatCol.getRed() * 255 * alpha);
        int blendedG = (int) (baseTerrainCol.getGreen() * 255 * (1.0 - alpha) + heatCol.getGreen() * 255 * alpha);
        int blendedB = (int) (baseTerrainCol.getBlue() * 255 * (1.0 - alpha) + heatCol.getBlue() * 255 * alpha);
        return Color.rgb(Math.clamp(blendedR, 0, 255), Math.clamp(blendedG, 0, 255), Math.clamp(blendedB, 0, 255));
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

        double capK0 = initialCapitalSpinner != null ? initialCapitalSpinner.getValue() : 1000.0;
        baseCap *= Math.max(0.5, (capK0 / 1000.0) * 0.8);
        return Math.max(10.0, baseCap);
    }

    private void drawPreview() {
        GraphicsContext gc = previewCanvas.getGraphicsContext2D();
        double w = previewCanvas.getWidth();
        double h = previewCanvas.getHeight();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.rgb(15, 23, 42));
        gc.fillRect(0, 0, w, h);

        double minLat = -90, maxLat = 90;
        double minLng = -180, maxLng = 180;

        double scaleX = w / (maxLng - minLng);
        double scaleY = h / (maxLat - minLat);
        double baseScale = Math.min(scaleX, scaleY) * 0.9;
        double scale = baseScale * zoomFactor;

        double offX = (w / 2.0) - (0.0 - minLng) * scale + panX;
        double offY = (h / 2.0) - (maxLat - 0.0) * scale + panY;

        if (currentPreviewCells != null) {
            for (H3Cell c : currentPreviewCells) {
                double x = (c.getLongitude() - minLng) * scale + offX;
                double y = (maxLat - c.getLatitude()) * scale + offY;

                if (x < -5 || x > w + 5 || y < -5 || y > h + 5) continue;

                Color col = getReliefAndDensityColor(c);
                gc.setFill(col);
                gc.fillRect(x, y, 2, 2);
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
            gc.setLineDashes(null);

            // Text tag
            gc.setFill(Color.rgb(56, 189, 248));
            gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 11));
            gc.fillText(String.format("✂️ Zone: Lat[%.1f°, %.1f°] Lng[%.1f°, %.1f°]", cMinLat, cMaxLat, cMinLng, cMaxLng), rx + 4, ry - 6);
        }
    }

    private void drawInstant2DDensityPreview(GraphicsContext gc, double w, double h, double scale, double offX, double offY) {
        int pwWidth = 320;
        int pwHeight = 160;
        WritableImage img = new WritableImage(pwWidth, pwHeight);
        PixelWriter writer = img.getPixelWriter();

        PlanetPreset planet = activePlanetPreset != null ? activePlanetPreset : PlanetPreset.EARTH_LIKE;
        boolean isEarth = planet == PlanetPreset.EARTH_LIKE || "earth".equalsIgnoreCase(planet.elevationMapSource()) ||
                (planet.name() != null && (planet.name().toLowerCase().contains("earth") || planet.name().toLowerCase().contains("terre")));

        Image bgImage = null;
        if (isEarth) {
            try (var is = getClass().getResourceAsStream("/maps/earth_elevation.png")) {
                if (is != null) bgImage = new Image(is);
            } catch (Exception ignored) {}
        }
        PixelReader bgReader = bgImage != null ? bgImage.getPixelReader() : null;

        for (int py = 0; py < pwHeight; py++) {
            for (int px = 0; px < pwWidth; px++) {
                Color pxColor;
                if (bgReader != null) {
                    pxColor = bgReader.getColor(px, py);
                } else {
                    double lat = 90.0 - (py / (double) pwHeight) * 180.0;
                    double lon = -180.0 + (px / (double) pwWidth) * 360.0;
                    double alt = Math.sin(lat * Math.PI / 180.0) * Math.cos(lon * Math.PI / 180.0);
                    pxColor = alt < 0 ? Color.rgb(20, 60, 140) : Color.rgb(40, 140, 60);
                }
                writer.setColor(px, py, pxColor);
            }
        }

        double drawW = 360.0 * scale;
        double drawH = 180.0 * scale;
        gc.drawImage(img, offX, offY, drawW, drawH);

        gc.setFill(Color.rgb(56, 189, 248, 0.95));
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
        gc.fillText("⚡ Génération de la grille H3 démographique en arrière-plan...", 20, h - 15);
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

    private void setStartButtonCancelState() {
        isCalculationRunning = true;
        javafx.application.Platform.runLater(() -> {
            if (startBtn != null) {
                startBtn.setText("⏹ ANNULER / STOPPER LA GÉNÉRATION");
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

                // Distribute Initial Population (85% -> 95%)
                updateProgress(0.88, "👥 Répartition de la population, empreinte écologique & capital K₀ (88%)...");

                distributeInitialPopulation(cells);
                if (cancelRequested) throw new java.util.concurrent.CancellationException("Generation cancelled by user");

                currentPreviewCells = cells;

                final List<H3Cell> finalCells = cells;
                javafx.application.Platform.runLater(() -> {
                    updateProgress(1.0, "✅ " + finalCells.size() + " cellules H3 calculées ! Lancement du scénario '" + scenarioToSave.getName() + "'...");
                    drawPreview();
                    previewStatusLabel.setText("Généré : " + finalCells.size() + " cellules H3.");

                    resetStartButtonState();

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

    // =========================================================================
    // EVENTS SECTION — Historical Planetary Events
    // =========================================================================

    @SuppressWarnings("unchecked")
    private VBox createEventsSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        title3Events = new Label();
        title3Events.getStyleClass().add("label-header");
        title3Events.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events", "Planifier des événements climatiques et géologiques.")));

        eventsList = FXCollections.observableArrayList();
        eventsTable = new TableView<>(eventsList);
        eventsTable.setEditable(true);
        eventsTable.setPrefHeight(200);

        List<String> eventTypes = List.of("earthquake","volcano","tsunami","tornado","wildfire","flood","drought","pandemic","impact");

        TableColumn<ClimateEvent, String> colType = new TableColumn<>();
        colType.setCellValueFactory(d -> d.getValue().typeProperty());
        colType.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(eventTypes)));
        colType.setOnEditCommit(e -> e.getRowValue().setType(e.getNewValue()));
        colType.setPrefWidth(120);

        TableColumn<ClimateEvent, String> colName = new TableColumn<>();
        colName.setCellValueFactory(d -> d.getValue().nameProperty());
        colName.setCellFactory(TextFieldTableCell.forTableColumn());
        colName.setOnEditCommit(e -> e.getRowValue().setName(e.getNewValue()));
        colName.setPrefWidth(160);

        TableColumn<ClimateEvent, Integer> colYear = new TableColumn<>();
        colYear.setCellValueFactory(d -> d.getValue().yearProperty().asObject());
        colYear.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
        colYear.setOnEditCommit(e -> e.getRowValue().setYear(e.getNewValue()));
        colYear.setPrefWidth(70);

        TableColumn<ClimateEvent, Double> colLat = new TableColumn<>();
        colLat.setCellValueFactory(d -> d.getValue().latitudeProperty().asObject());
        colLat.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        colLat.setOnEditCommit(e -> e.getRowValue().setLatitude(e.getNewValue()));
        colLat.setPrefWidth(75);

        TableColumn<ClimateEvent, Double> colLon = new TableColumn<>();
        colLon.setCellValueFactory(d -> d.getValue().longitudeProperty().asObject());
        colLon.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        colLon.setOnEditCommit(e -> e.getRowValue().setLongitude(e.getNewValue()));
        colLon.setPrefWidth(75);

        TableColumn<ClimateEvent, Double> colDepth = new TableColumn<>();
        colDepth.setCellValueFactory(d -> d.getValue().depthProperty().asObject());
        colDepth.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        colDepth.setOnEditCommit(e -> e.getRowValue().setDepth(e.getNewValue()));
        colDepth.setPrefWidth(80);

        TableColumn<ClimateEvent, Double> colMag = new TableColumn<>();
        colMag.setCellValueFactory(d -> d.getValue().magnitudeProperty().asObject());
        colMag.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        colMag.setOnEditCommit(e -> e.getRowValue().setMagnitude(e.getNewValue()));
        colMag.setPrefWidth(80);

        eventsTable.getColumns().addAll(colType, colName, colYear, colLat, colLon, colDepth, colMag);
        eventsTable.setUserData(new TableColumn[]{colType, colName, colYear, colLat, colLon, colDepth, colMag});

        addEventBtn = new Button();
        addEventBtn.getStyleClass().add("button-secondary");
        addEventBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events.add", "Ajouter un événement.")));
        addEventBtn.setOnAction(e -> eventsList.add(new ClimateEvent("earthquake", "Nouvel Événement", 0, 0.0, 0.0, 10.0, 6.0)));

        removeEventBtn = new Button();
        removeEventBtn.getStyleClass().add("button-secondary");
        removeEventBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events.remove", "Supprimer l'événement sélectionné.")));
        removeEventBtn.setOnAction(e -> {
            ClimateEvent sel = eventsTable.getSelectionModel().getSelectedItem();
            if (sel != null) eventsList.remove(sel);
        });

        loadEarthEventsBtn = new Button();
        loadEarthEventsBtn.getStyleClass().add("button-secondary");
        loadEarthEventsBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events.load", "Charger les événements historiques de la Terre.")));
        loadEarthEventsBtn.setOnAction(e -> loadEarthHistoricalEvents());

        CheckBox eventsCheckBox = new CheckBox(org.ether.society.i18n.I18n.getOrDefault("scenario.events.enable", "Planifier des événements climatiques & catastrophes historiques"));
        eventsCheckBox.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold;");

        randomEventsCheckBox = new CheckBox(org.ether.society.i18n.I18n.getOrDefault("scenario.events.random_checkbox", "☑️ Générer dynamiquement les événements géologiques & dérives climatiques (séismes, volcanisme, élévation mer, Sahara vert)"));
        randomEventsCheckBox.setSelected(true);
        randomEventsCheckBox.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");

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
        loadEarthHistoricalEvents(startYear);
    }

    private void loadEarthHistoricalEvents(int startYear) {
        if (eventsList == null) return;
        eventsList.clear();

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

        // Tempêtes Solaire & Éruptions EMP Carrington
        masterList.add(new ClimateEvent("impact", "Événement Solaire Carrington (EMP & Grille Électrique)", 1859, 50.0, 0.0, 0.0, 8.5));

        // Impact Météorique
        masterList.add(new ClimateEvent("impact", "Impact Chicxulub (Extinction K-Pg VEI-10)", -66000000, 21.40, -89.50, 0.0, 10.0));
        masterList.add(new ClimateEvent("impact", "Impact Tunguska (Onde de Choc 15Mt)", 1908, 60.89, 101.89, 0.0, 5.0));

        // Tectonique, Séismes & Tsunamis
        masterList.add(new ClimateEvent("earthquake", "Séisme Shensi (Chine, 830k victimes)", 1556, 34.50, 109.70, 20.0, 8.0));
        masterList.add(new ClimateEvent("earthquake", "Séisme Lisbonne (Portugal & Tsunami, M8.7)", 1755, 36.00, -10.50, 30.0, 8.7));
        masterList.add(new ClimateEvent("earthquake", "Séisme Valdivia (Chili, M9.5)", 1960, -38.14, -73.41, 25.0, 9.5));
        masterList.add(new ClimateEvent("earthquake", "Séisme Alaska (M9.2)", 1964, 61.02, -147.65, 25.0, 9.2));
        masterList.add(new ClimateEvent("earthquake", "Séisme Sumatra-Andaman (M9.1)", 2004, 3.30, 95.98, 30.0, 9.1));
        masterList.add(new ClimateEvent("earthquake", "Séisme Tohoku Japon (M9.0)", 2011, 38.30, 142.37, 29.0, 9.0));
        masterList.add(new ClimateEvent("tsunami", "Tsunami Sumatra (Submersion Littorale)", 2004, 3.30, 95.98, 30.0, 9.1));
        masterList.add(new ClimateEvent("tsunami", "Tsunami Fukushima & Accident Nucléaire", 2011, 38.30, 142.37, 29.0, 9.0));

        // Radiations & Accidents Technologiques
        masterList.add(new ClimateEvent("impact", "Accident Nucléaire de Tchernobyl (Fallout Bq/m²)", 1986, 51.38, 30.10, 0.0, 7.5));

        // Ozone CFC & Dérive Climatique
        masterList.add(new ClimateEvent("climate_drift", "Holocène Vert (Sahara Humide & Fertile)", -6000, 20.0, 10.0, 0.0, 3.0));
        masterList.add(new ClimateEvent("climate_drift", "Optimum Climatique Médiéval (+1.2°C)", 1000, 45.0, 15.0, 0.0, 1.2));
        masterList.add(new ClimateEvent("climate_drift", "Peste Noire (Europe, 1/3 Pop.)", 1347, 44.00, 10.00, 0.0, 9.0));
        masterList.add(new ClimateEvent("climate_drift", "Petit Âge Glaciaire (-1.5°C Maunder)", 1650, 50.0, 10.0, 0.0, -1.5));
        masterList.add(new ClimateEvent("climate_drift", "Trou dans la Couche d'Ozone CFC (Flux UV)", 1985, -80.0, 0.0, 0.0, 4.0));
        masterList.add(new ClimateEvent("flood", "Grande Inondation Jaune (Chine)", 1931, 32.00, 118.00, 0.0, 8.0));
        masterList.add(new ClimateEvent("pandemic", "Grippe Espagnole (50M Victimes)", 1918, 40.00, 0.00, 0.0, 8.0));
        masterList.add(new ClimateEvent("sea_level", "Élévation Littorale Moderne (+2.5m Submersion)", 2050, 0.0, 0.0, 0.0, 2.5));

        // Filter events: Keep all historical events taking place AFTER or IN the starting year
        for (ClimateEvent ev : masterList) {
            if (ev.getYear() >= startYear) {
                eventsList.add(ev);
            }
        }

        logger.info("Loaded {} historical events posterior to start year {}.", eventsList.size(), startYear);
    }

    /**
     * Auto-calibrates initial physical capital stock (K0 in kg/capita) based on start year T0.
     */
    public static double computeAutoCapitalFromYear(long year) {
        if (year <= -10000) return 5.0;   // Paleolithic / Early Neolithic (~5 kg/hab)
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
        if (title1 != null) title1.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.general_params", "1. PARAMÈTRES GÉNÉRAUX"));
        if (title3Events != null) title3Events.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events_section", "4. ÉVÉNEMENTS PLANÉTAIRES HISTORIQUES"));
        if (startYearLabel != null) startYearLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.start_year", "Année de départ (Repère chronologique) :"));
        if (popCountLabel != null) popCountLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_count", "Population Initiale (1 000 à 10 000 000 000) :"));
        if (capitalLabel != null) capitalLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.capital_per_capita", "🛠️ Capital Physique Initial (K₀) (kg/hab) :"));
        if (densityPatternLabel != null) densityPatternLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.density_pattern", "Motif de Répartition :"));
        if (urbanCentersLabel != null) urbanCentersLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.urban_centers", "Nœuds Urbains / Cités :"));
        if (h3ResolutionLabel != null) h3ResolutionLabel.setText(org.ether.society.i18n.I18n.getOrDefault("planet.param.resolution", "Résolution H3 :"));
        if (cohortSizeLabel != null) cohortSizeLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.label.cohort_size", "Taille des Cohortes :"));
        if (startBtn != null) startBtn.setText(org.ether.society.i18n.I18n.get("scenario.start_btn"));
        if (generateBtn != null) generateBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.preview_btn", "🔄 Prévisualiser la Répartition"));
        if (addEventBtn != null) addEventBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.add", "➕ Ajouter Événement"));
        if (removeEventBtn != null) removeEventBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.remove", "🗑️ Supprimer"));
        if (loadEarthEventsBtn != null) loadEarthEventsBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.load_earth", "🌍 Charger Événements Historiques Terre"));

        if (clippingHeader != null) clippingHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.header", "✂️ SIMULATION LOCALE & FRONTIÈRES (CLIPPING)"));
        if (clippingCheckBox != null) clippingCheckBox.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.enable", "Activer la simulation partielle (Zone Tronquée)"));
        if (graphicSelectBtn != null) graphicSelectBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.select_mode", "🖱️ Mode Sélection Graphique sur Carte"));
        if (resetClippingBtn != null) resetClippingBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.reset", "🔄 Réinitialiser la Zone (Pleine Planète)"));
        if (latMaxLabel != null) latMaxLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lat_max", "Lat Max (Haut) :"));
        if (latMinLabel != null) latMinLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lat_min", "Lat Min (Bas) :"));
        if (lngMinLabel != null) lngMinLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lng_min", "Lng Min (Gau.) :"));
        if (lngMaxLabel != null) lngMaxLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lng_max", "Lng Max (Dro.) :"));
        if (boundaryLabel != null) boundaryLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.boundary_label", "Modélisation Scientifique des Frontières :"));
        if (previewTitleLabel != null) previewTitleLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.preview_title", "CARTE DE DENSITÉ DE POPULATION INITIALE"));

        if (radioProcDemo != null) radioProcDemo.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.demo.procedural", "▶ Génération Procédurale"));
        if (radioImportDemo != null) radioImportDemo.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.demo.import", "📂 Import Carte Externe (PNG)"));
        if (loadDensityMapBtn != null) loadDensityMapBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.demo.btn_load", "📥 Importer Carte de Densité Externe (PNG)"));
        if (exportDensityMapBtn != null) exportDensityMapBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.demo.btn_export", "📤 Exporter Carte de Densité Générée (PNG)"));

        if (boundaryModeCombo != null) {
            boundaryModeCombo.setButtonCell(boundaryModeCombo.getCellFactory().call(null));
        }

        if (eventsTable != null && eventsTable.getUserData() instanceof TableColumn[] cols && cols.length == 7) {
            cols[0].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.type", "Type"));
            cols[1].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.name", "Nom / Description"));
            cols[2].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.year", "Année"));
            cols[3].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.lat", "Latitude"));
            cols[4].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.lon", "Longitude"));
            cols[5].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.depth", "Profondeur (km)"));
            cols[6].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.magnitude", "Magnitude"));
        }
        if (getLeft() instanceof ScrollPane sp && sp.getContent() instanceof VBox root) {
            root.lookupAll("#__popHeader").forEach(n -> { if (n instanceof Label l) l.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_section", "2. DÉMOGRAPHIE & RÉPARTITION DE POPULATION")); });
        }
        updateDemoCompatibilityDisplay();
    }

    public Scenario getScenario() {
        Scenario s = new Scenario();
        s.setName(scenarioPresetBar != null ? scenarioPresetBar.getCurrentName() : "New Scenario");
        if (scenarioDescriptionArea != null) {
            s.setDescription(scenarioDescriptionArea.getText());
        }
        s.setStartDateYear(startYearSpinner.getValue());
        s.setTargetCohortSize(targetCohortSizeSpinner != null ? targetCohortSizeSpinner.getValue() : 500);
        s.setInitialHumanCount(initialHumanCountSpinner.getValue());
        s.setInitialCapitalPerCapita(initialCapitalSpinner != null ? initialCapitalSpinner.getValue() : 10.0);
        s.setInitialEnergyPerCapita(initialEnergySpinner != null ? initialEnergySpinner.getValue() : 50.0);
        s.setInitialFoodReserveMonths(initialFoodSpinner != null ? initialFoodSpinner.getValue() : 6.0);
        s.setInitialInformationPerCapita(initialInfoSpinner != null ? initialInfoSpinner.getValue() : 100.0);
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
        s.setRandomEventsEnabled(randomEventsCheckBox == null || randomEventsCheckBox.isSelected());
        if (clippingCheckBox != null) {
            s.setClippingEnabled(clippingCheckBox.isSelected());
            s.setMinLat(minLatSpinner.getValue());
            s.setMaxLat(maxLatSpinner.getValue());
            s.setMinLng(minLngSpinner.getValue());
            s.setMaxLng(maxLngSpinner.getValue());
            s.setBoundaryMode(boundaryModeCombo.getValue());
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
        if (customDensityImage != null) {
            s.setCustomDensityBase64(org.ether.society.data.ImageMapLoader.imageToBase64Png(customDensityImage));
        }
        return s;
    }

    public List<H3Cell> getCells() { return currentPreviewCells; }

    public void setGeneratedCells(List<H3Cell> cells) {
        this.currentPreviewCells = cells;
        drawPreview();
    }

    public List<ClimateEvent> getScheduledEvents() {
        return eventsList != null ? new ArrayList<>(eventsList) : new ArrayList<>();
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
}
