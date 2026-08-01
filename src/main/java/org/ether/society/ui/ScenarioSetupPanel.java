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
    private TextField scenarioNameField;
    private Spinner<Integer> startYearSpinner;
    private ComboBox<StartDatePreset> eraCombo;
    private ComboBox<Integer> h3ResolutionCombo;
    private Label h3ResolutionLabel;

    // Population & Density Controls
    private Spinner<Long> initialHumanCountSpinner;
    private Slider initialTechLevelSlider;
    private ComboBox<String> densityPatternCombo;
    private Spinner<Integer> urbanCentersSpinner;

    // Density Map Import/Export Controls
    private Image customDensityImage;
    private Label densityMapFileLabel;
    private Button loadDensityMapBtn;
    private Button clearDensityMapBtn;
    private Button exportDensityMapBtn;

    // Map Preview
    private Canvas previewCanvas;
    private Label previewStatusLabel;

    // Execution & Calculation Controls
    private ProgressBar progressBar;
    private Label progressStatusLabel;

    // State
    private List<H3Cell> currentPreviewCells;
    private PlanetPreset activePlanetPreset;
    private final Consumer<Scenario> onStartSimulation;
    private final org.ether.society.persistence.ScenarioRepository scenarioRepo;

    // Labels for i18n
    private Label title1;
    private Label title3Events;
    private Label nameLabel;
    private Label eraLabel;
    private Label startYearLabel;
    private Label popCountLabel;
    private Label techLevelLabel;
    private Label densityPatternLabel;
    private Label urbanCentersLabel;
    private Button generateBtn;
    private Button startBtn;

    // Seed & Random Events Controls for Tab 3
    private TextField demoSeedField;
    private Button demoRandSeedBtn;
    private Button demoRegenBtn;
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
    }

    public void setInheritedContext(PlanetPreset planetPreset, String ecologyName) {
        this.activePlanetPreset = planetPreset;
        if (planetPreset != null && planetPresetCombo != null) {
            planetPresetCombo.setValue(planetPreset);
        }
        updateInheritedContextDisplay(ecologyName);
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

        // --- 1. Inherited Presets (Tab 1 Planet + Tab 2 Ecology) Header ---
        planetSectionHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.section.inherited", "CONTEXTE HÉRITÉ (ONGLETS 1 & 2)"));

        planetPresetCombo = new ComboBox<>();
        planetPresetCombo.getItems().setAll(PlanetPreset.getPresets());
        planetPresetCombo.setValue(PlanetPreset.EARTH_LIKE);
        planetPresetCombo.setMaxWidth(Double.MAX_VALUE);
        planetPresetCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        planetPresetCombo.setButtonCell(planetPresetCombo.getCellFactory().call(null));
        planetPresetCombo.setOnAction(e -> {
            PlanetPreset selected = planetPresetCombo.getValue();
            if (selected != null) {
                this.activePlanetPreset = selected;
                updateInheritedContextDisplay(null);
            }
        });

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
                updateInheritedContextDisplay(selected.name());
            }
        });

        inheritedContextLabel = new Label("🪐 Planète : Terrestre Standard | 🌿 Écologie : Équilibrée");
        inheritedContextLabel.setWrapText(true);
        inheritedContextLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 6 10; -fx-background-color: rgba(56, 189, 248, 0.12); -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 6;");

        VBox inheritedSection = createSection(planetSectionHeader, new VBox(8,
                new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.label.planet_preset", "Préréglage Planétaire (Onglet 1) :")),
                planetPresetCombo,
                new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.label.ecology_preset", "Préréglage Écologique (Onglet 2) :")),
                ecologyPresetCombo,
                inheritedContextLabel
        ));

        // --- 2. Standardized Preset Control Bar for Scenarios ---
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
                scenarioNameField.setText(name);
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

        // --- 3. Scenario General Info Section (Inline Editable Name) ---
        VBox section1 = new VBox(10);
        title1 = new Label();
        title1.getStyleClass().add("label-header");
        GridPane grid1 = new GridPane();
        grid1.setHgap(10);
        grid1.setVgap(10);

        scenarioNameField = new TextField("Croissant Fertile & Néolithique (-8000)");
        scenarioNameField.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.name", "Nom du scénario éditable.")));

        eraCombo = new ComboBox<>();
        eraCombo.getItems().addAll(StartDatePreset.values());
        eraCombo.setValue(StartDatePreset.NEOLITHIZATION);
        eraCombo.setMaxWidth(Double.MAX_VALUE);
        eraCombo.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.era", "Époque historique prédéfinie.")));
        eraCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(StartDatePreset p) {
                return p != null ? p.getDisplayName() + " (" + p.getYear() + ")" : "";
            }

            @Override
            public StartDatePreset fromString(String s) {
                return null;
            }
        });

        startYearSpinner = new Spinner<>(-100000, 2100, -8000, 100);
        startYearSpinner.setEditable(true);
        startYearSpinner.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.start_year", "Marqueur temporel de départ (repère visuel sans influence sur les calculs).")));

        eraCombo.setOnAction(e -> {
            StartDatePreset p = eraCombo.getValue();
            if (p != null) {
                startYearSpinner.getValueFactory().setValue((int) p.getYear());
                initialHumanCountSpinner.getValueFactory().setValue(p.getEstimatedPopulation());
                initialTechLevelSlider.setValue((double) p.getEstimatedTechLevel());
                scenarioNameField.setText(p.getDisplayName() + " Civilization");
                if (currentPreviewCells != null) {
                    distributeInitialPopulation(currentPreviewCells);
                    drawPreview();
                }
            }
        });

        nameLabel = new Label();
        eraLabel = new Label();
        startYearLabel = new Label();
        Tooltip.install(nameLabel, scenarioNameField.getTooltip());
        Tooltip.install(eraLabel, eraCombo.getTooltip());
        Tooltip.install(startYearLabel, startYearSpinner.getTooltip());

        grid1.addRow(0, nameLabel, scenarioNameField);
        grid1.addRow(1, eraLabel, eraCombo);
        grid1.addRow(2, startYearLabel, startYearSpinner);

        // H3 Resolution
        h3ResolutionLabel = new Label();
        h3ResolutionCombo = new ComboBox<>();
        h3ResolutionCombo.getItems().addAll(5, 6, 7, 8);
        h3ResolutionCombo.setValue(6);
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
        grid1.addRow(3, h3ResolutionLabel, h3ResolutionCombo);

        section1.getChildren().addAll(title1, grid1);

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
        radioProcDemo.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        radioImportDemo.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold;");

        GridPane popGrid = new GridPane();
        popGrid.setHgap(10);
        popGrid.setVgap(10);

        initialHumanCountSpinner = new Spinner<>(100L, 50000000L, 50000L, 5000L);
        initialHumanCountSpinner.setEditable(true);
        initialHumanCountSpinner.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.pop_count", "Population globale initiale.")));

        initialTechLevelSlider = new Slider(0.0, 10.0, 1.0);
        initialTechLevelSlider.setShowTickLabels(true);
        initialTechLevelSlider.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.tech_level", "Niveau technologique initial.")));

        Map<String, String> densityDescriptions = Map.of(
            "FERTILE_CRESCENT", "🌾 Croissant Fertile : Implantation le long des plaines alluviales et littoraux tempérés (Plaines: ×4.5).",
            "MESOAMERICA", "🌴 Mésoamérique : Dispersion adaptée aux jungles tropicales et vallées d'altitude (Jungles/Collines: ×3.5).",
            "MESOPOTAMIA_ASSYRIA", "🏛️ Mésopotamie & Assyrie : Hyper-concentration le long des réseaux hydrographiques majeurs (Plaines alluviales: ×5.0).",
            "RIVER_VALLEYS", "🌊 Vallées Fluviales : Colonisation linéaire le long du tracé des fleuves et deltas (Fleuves/Littoraux: ×4.5).",
            "URBAN_CLUSTERS", "🏙️ Nœuds Urbains : Émergence de métropoles hyper-concentrées avec grappes urbaines (Booster Cités: ×15.0).",
            "SPARSE_NOMADIC", "⛺ Dispersion Nomade : Population pastorale dispersée à faible densité sur de grands espaces (Déserts/Tundras: ×1.5)."
        );

        densityPatternCombo = new ComboBox<>();
        densityPatternCombo.getItems().addAll("FERTILE_CRESCENT", "MESOAMERICA", "MESOPOTAMIA_ASSYRIA", "RIVER_VALLEYS", "URBAN_CLUSTERS", "SPARSE_NOMADIC");
        densityPatternCombo.setValue("FERTILE_CRESCENT");
        densityPatternCombo.setMaxWidth(Double.MAX_VALUE);
        densityPatternCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    setTooltip(new Tooltip(densityDescriptions.getOrDefault(item, item)));
                }
            }
        });
        densityPatternCombo.setButtonCell(densityPatternCombo.getCellFactory().call(null));
        densityPatternCombo.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.density", "Algorithme de répartition démographique sur la grille H3.")));

        urbanCentersSpinner = new Spinner<>(1, 30, 5, 1);
        urbanCentersSpinner.setEditable(true);
        urbanCentersSpinner.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.urban", "Nombre de foyers ou cités d'origine.")));

        popCountLabel = new Label();
        techLevelLabel = new Label();
        densityPatternLabel = new Label();
        urbanCentersLabel = new Label();
        Tooltip.install(popCountLabel, initialHumanCountSpinner.getTooltip());
        Tooltip.install(techLevelLabel, initialTechLevelSlider.getTooltip());
        Tooltip.install(densityPatternLabel, densityPatternCombo.getTooltip());
        Tooltip.install(urbanCentersLabel, urbanCentersSpinner.getTooltip());

        demoSeedField = new TextField("12345");
        demoSeedField.setPrefWidth(120);
        demoRandSeedBtn = new Button("🎲");
        demoRandSeedBtn.getStyleClass().add("button-secondary");
        demoRandSeedBtn.setOnAction(e -> {
            demoSeedField.setText(String.valueOf(new Random().nextLong(1000000)));
            if (currentPreviewCells != null) {
                distributeInitialPopulation(currentPreviewCells);
                drawPreview();
            }
        });
        HBox demoSeedBox = new HBox(5, demoSeedField, demoRandSeedBtn);
        HBox.setHgrow(demoSeedField, Priority.ALWAYS);

        demoRegenBtn = new Button(org.ether.society.i18n.I18n.getOrDefault("scenario.btn.regenerate", "🔄 Regénérer la démographie (nouvelle graine)"));
        demoRegenBtn.setMaxWidth(Double.MAX_VALUE);
        demoRegenBtn.getStyleClass().add("button-secondary");
        demoRegenBtn.setStyle("-fx-text-fill: #38bdf8;");
        demoRegenBtn.setOnAction(e -> {
            demoSeedField.setText(String.valueOf(new Random().nextLong(1000000)));
            if (currentPreviewCells != null) {
                distributeInitialPopulation(currentPreviewCells);
                drawPreview();
            }
        });

        Label demoSeedLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.seed.label", "Graine Aléatoire Démographique :"));
        popGrid.addRow(4, demoSeedLabel, demoSeedBox);

        exportDensityMapBtn = new Button("📤 Exporter Carte de Densité Générée (PNG)");
        exportDensityMapBtn.getStyleClass().add("button-secondary");
        exportDensityMapBtn.setMaxWidth(Double.MAX_VALUE);
        exportDensityMapBtn.setOnAction(e -> exportDensityMap());

        VBox proceduralDemoPanel = new VBox(8, popGrid, demoRegenBtn, exportDensityMapBtn);
        proceduralDemoPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");

        // Import Panel
        densityMapFileLabel = new Label(org.ether.society.i18n.I18n.get("planet.map.none"));
        densityMapFileLabel.getStyleClass().add("value-label");

        demoCompatibilityLabel = new Label("🪐 Validation terrain : Aucune carte externe chargée");
        demoCompatibilityLabel.setWrapText(true);
        demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        loadDensityMapBtn = new Button("📥 Importer Carte de Densité Externe (PNG)");
        loadDensityMapBtn.getStyleClass().add("button-secondary");
        loadDensityMapBtn.setMaxWidth(Double.MAX_VALUE);
        loadDensityMapBtn.setOnAction(e -> loadCustomDensityMap());

        clearDensityMapBtn = new Button("❌ Annuler Import");
        clearDensityMapBtn.getStyleClass().add("button-secondary");
        clearDensityMapBtn.setOnAction(e -> {
            customDensityImage = null;
            densityMapFileLabel.setText(org.ether.society.i18n.I18n.get("planet.map.none"));
            updateDemoCompatibilityDisplay();
            if (currentPreviewCells != null) drawPreview();
        });

        HBox mapBtnBox = new HBox(6, loadDensityMapBtn, clearDensityMapBtn);
        VBox importDemoPanel = new VBox(8, mapBtnBox, densityMapFileLabel, demoCompatibilityLabel);
        importDemoPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(167,139,250,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        importDemoPanel.setVisible(false);
        importDemoPanel.setManaged(false);

        // Toggle listener
        demoSourceGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
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
        initialHumanCountSpinner.valueProperty().addListener((obs, oldV, newV) -> { if (currentPreviewCells != null) { distributeInitialPopulation(currentPreviewCells); drawPreview(); } });
        initialTechLevelSlider.valueProperty().addListener((obs, oldV, newV) -> { if (currentPreviewCells != null) { distributeInitialPopulation(currentPreviewCells); drawPreview(); } });
        densityPatternCombo.valueProperty().addListener((obs, oldV, newV) -> { if (currentPreviewCells != null) { distributeInitialPopulation(currentPreviewCells); drawPreview(); } });

        popSection.getChildren().addAll(popHeader, radioProcDemo, proceduralDemoPanel, radioImportDemo, importDemoPanel);

        // --- 5. Spatial Clipping & Boundary Condition Section ---
        VBox clippingSection = createClippingSection();

        // --- 6. Events Section ---
        VBox eventsSection = createEventsSection();

        // --- 7. Actions & Progress Bar (Deferred Execution) ---
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

        startBtn = new Button();
        startBtn.setPrefHeight(50);
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
        startBtn.setOnAction(e -> startSimulationDeferred());
        startBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.start", "Calculer les cellules H3 et lancer la simulation.")));

        VBox bottom = new VBox(10, generateBtn, progressBar, progressStatusLabel, startBtn);
        VBox.setVgrow(bottom, Priority.ALWAYS);
        bottom.setAlignment(Pos.BOTTOM_CENTER);

        root.getChildren().addAll(inheritedSection, scenarioPresetBar, section1, popSection, clippingSection, eventsSection, bottom);
        return root;
    }

    private VBox createSection(Label header, javafx.scene.Node content) {
        header.getStyleClass().add("label-section-header");
        VBox box = new VBox(8, header, content);
        box.getStyleClass().add("card-section");
        return box;
    }

    private List<Scenario> getBuiltInScenarios() {
        List<Scenario> list = new ArrayList<>();

        Scenario s1 = new Scenario();
        s1.setName("Croissant Fertile & Néolithique (-8000)");
        s1.setStartDateYear(-8000);
        s1.setInitialHumanCount(25000);
        s1.setInitialTechLevel(1.2);
        s1.setPopulationDensityType("FERTILE_CRESCENT");
        list.add(s1);

        Scenario s2 = new Scenario();
        s2.setName("Empire Assyrien & Mésopotamie (-2000)");
        s2.setStartDateYear(-2000);
        s2.setInitialHumanCount(500000);
        s2.setInitialTechLevel(3.5);
        s2.setPopulationDensityType("MESOPOTAMIA_ASSYRIA");
        list.add(s2);

        Scenario s3 = new Scenario();
        s3.setName("Mésoamérique : Cités Mayas & Vallée (1000)");
        s3.setStartDateYear(1000);
        s3.setInitialHumanCount(1200000);
        s3.setInitialTechLevel(4.0);
        s3.setPopulationDensityType("MESOAMERICA");
        list.add(s3);

        Scenario s4 = new Scenario();
        s4.setName("Antiquité Classique Méditerranéenne (-500)");
        s4.setStartDateYear(-500);
        s4.setInitialHumanCount(2500000);
        s4.setInitialTechLevel(4.8);
        s4.setPopulationDensityType("URBAN_CLUSTERS");
        list.add(s4);

        return list;
    }

    private void updateInheritedContextDisplay(String ecologyName) {
        if (inheritedContextLabel == null) return;
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        if (p == null) p = PlanetPreset.EARTH_LIKE;
        EcologyPreset eco = ecologyPresetCombo.getValue();
        String ecoName = ecologyName != null ? ecologyName : (eco != null ? eco.name() : "Standard");

        String text = String.format("🪐 Planète : %s (Rayon: %,.0f km) | 🌿 Écologie : %s", p.name(), p.radiusKm(), ecoName);
        inheritedContextLabel.setText(text);
    }

    private void applyScenarioToUI(Scenario s) {
        if (s == null) return;
        scenarioNameField.setText(s.getName());
        startYearSpinner.getValueFactory().setValue((int) s.getStartDateYear());
        initialHumanCountSpinner.getValueFactory().setValue(s.getInitialHumanCount());
        initialTechLevelSlider.setValue(s.getInitialTechLevel());

        if (s.getPopulationDensityType() != null && densityPatternCombo.getItems().contains(s.getPopulationDensityType())) {
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
            minLatSpinner.getValueFactory().setValue(s.getMinLat());
            maxLatSpinner.getValueFactory().setValue(s.getMaxLat());
            minLngSpinner.getValueFactory().setValue(s.getMinLng());
            maxLngSpinner.getValueFactory().setValue(s.getMaxLng());
            if (s.getBoundaryMode() != null) boundaryModeCombo.setValue(s.getBoundaryMode());
        }
        if (s.getCustomDensityBase64() != null) {
            customDensityImage = org.ether.society.data.ImageMapLoader.base64PngToImage(s.getCustomDensityBase64());
            if (densityMapFileLabel != null) densityMapFileLabel.setText("📷 Preset Density Map");
            updateDemoCompatibilityDisplay();
        }
        drawPreview();
    }

    private VBox createClippingSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        clippingHeader = new Label(I18n.getOrDefault("scenario.clipping.header", "✂️ SIMULATION LOCALE & FRONTIÈRES (CLIPPING)"));
        clippingHeader.getStyleClass().add("label-header");
        clippingHeader.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        clippingCheckBox = new CheckBox(I18n.getOrDefault("scenario.clipping.enable", "Activer la simulation partielle (Zone Tronquée)"));
        clippingCheckBox.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold;");
        clippingCheckBox.setOnAction(e -> drawPreview());

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
            sp.valueProperty().addListener((obs, old, val) -> drawPreview());
        }

        boundsGrid.addRow(0, new Label("Lat Max (Haut) :"), maxLatSpinner, new Label("Lat Min (Bas) :"), minLatSpinner);
        boundsGrid.addRow(1, new Label("Lng Min (Gau.) :"), minLngSpinner, new Label("Lng Max (Dro.) :"), maxLngSpinner);

        Label boundaryLabel = new Label("Modélisation Scientifique des Frontières :");
        boundaryLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        boundaryModeCombo = new ComboBox<>();
        boundaryModeCombo.getItems().addAll(
            "DYNAMIC_RESERVOIR",
            "CLOSED_BARRIER",
            "PERIODIC_WRAP"
        );
        boundaryModeCombo.setValue("DYNAMIC_RESERVOIR");
        boundaryModeCombo.setMaxWidth(Double.MAX_VALUE);
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
        resetClippingBtn.getStyleClass().add("button-secondary");
        resetClippingBtn.setMaxWidth(Double.MAX_VALUE);
        resetClippingBtn.setOnAction(e -> {
            clippingCheckBox.setSelected(false);
            maxLatSpinner.getValueFactory().setValue(90.0);
            minLatSpinner.getValueFactory().setValue(-90.0);
            minLngSpinner.getValueFactory().setValue(-180.0);
            maxLngSpinner.getValueFactory().setValue(180.0);
            drawPreview();
        });

        section.getChildren().addAll(clippingHeader, clippingCheckBox, graphicSelectBtn, boundsGrid, boundaryLabel, boundaryModeCombo, resetClippingBtn);
        return section;
    }

    private VBox createPreviewPane() {
        VBox root = new VBox(10);
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("CARTE DE DENSITÉ DE POPULATION INITIALE");
        title.getStyleClass().add("label-header");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer);

        Pane canvasContainer = new StackPane();
        canvasContainer.setStyle("-fx-background-color: black; -fx-border-color: #475569;");
        previewCanvas = new Canvas(600, 400);

        // Interactive Zoom, Pan, Selection & Double Click Handlers
        previewCanvas.setOnScroll(e -> {
            double delta = e.getDeltaY();
            double factor = delta > 0 ? 1.15 : 0.85;
            zoomFactor = Math.max(0.5, Math.min(20.0, zoomFactor * factor));
            drawPreview();
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
        HBox legend = new HBox(8);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(6));
        legend.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-background-radius: 5;");

        String[] labels = { "Inhabité / Eau", "Faible", "Moyenne", "Élevée (Cité)", "Métropole" };
        Color[] colors = {
                Color.rgb(30, 41, 59),
                Color.rgb(16, 185, 129),
                Color.rgb(234, 179, 8),
                Color.rgb(249, 115, 22),
                Color.rgb(239, 68, 68)
        };

        for (int i = 0; i < labels.length; i++) {
            javafx.scene.shape.Rectangle colorBox = new javafx.scene.shape.Rectangle(18, 10);
            colorBox.setFill(colors[i]);
            colorBox.setStroke(Color.GRAY);

            Label lbl = new Label(labels[i]);
            lbl.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 10px;");

            HBox item = new HBox(3, colorBox, lbl);
            item.setAlignment(Pos.CENTER);
            legend.getChildren().add(item);
        }
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
            demoCompatibilityLabel.setText(String.format("✅ Carte chargée et compatible avec le monde choisi à l'onglet 1 (%s)", planetName));
            demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
        } else {
            demoCompatibilityLabel.setText("🪐 Aucune carte externe chargée — Mode procédural actif");
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

    private void generatePreview() {
        try {
            PlanetPreset cfg = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
            if (cfg == null) cfg = PlanetPreset.EARTH_LIKE;

            // Generate low-res cells for fast preview (Resolution 3 or 4)
            List<H3Cell> previewCells = new ProceduralGenerator().generatePlanet(
                    new PlanetPreset(
                            cfg.name(), 4, cfg.radiusKm(), cfg.dayLengthHours(),
                            cfg.axialTiltDegrees(), cfg.yearLengthDays(), cfg.distanceToSunAU(),
                            cfg.solarLuminosity(), cfg.minAltitudeMeters(), cfg.maxAltitudeMeters(),
                            cfg.averageTempC(), cfg.seed(), cfg.noiseFrequency(),
                            cfg.noiseScale(), cfg.waterLevel(), cfg.temperatureGradient(),
                            cfg.oxygenPercentage(), cfg.albedo(), cfg.atmospherePressureAtm(),
                            cfg.isSatellite(), cfg.parentPlanetMassEarthMasses(),
                            cfg.orbitalDistanceToParentKm(), cfg.co2Ppm()
                    )
            );

            distributeInitialPopulation(previewCells);
            currentPreviewCells = previewCells;
            drawPreview();
            previewStatusLabel.setText("Aperçu généré : " + previewCells.size() + " cellules.");
        } catch (Exception ex) {
            logger.error("Failed to generate preview", ex);
            previewStatusLabel.setText("Erreur aperçu : " + ex.getMessage());
        }
    }

    private void distributeInitialPopulation(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        long totalPop = initialHumanCountSpinner.getValue();
        double techLevel = initialTechLevelSlider.getValue();
        String pattern = densityPatternCombo.getValue();
        boolean isEarthPreset = activePlanetPreset != null && activePlanetPreset.name() != null && activePlanetPreset.name().toLowerCase().contains("earth");
        long startYear = startYearSpinner.getValue();

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
            org.ether.society.procedural.ProceduralPopulationEngine.distributePopulation(
                    cells, getScenario(), totalPop, techLevel, pattern, isEarthPreset, startYear
            );
        }
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

                Color col;
                if (c.getElevation() <= 0) {
                    col = Color.rgb(15, 23, 42); // Sea
                } else {
                    long pop = c.getPopulation();
                    if (pop <= 0) col = Color.rgb(30, 41, 59);
                    else if (pop < 100) col = Color.rgb(16, 185, 129); // Low
                    else if (pop < 1000) col = Color.rgb(234, 179, 8); // Medium
                    else if (pop < 5000) col = Color.rgb(249, 115, 22); // City
                    else col = Color.rgb(239, 68, 68); // Metropolis
                }

                gc.setFill(col);
                gc.fillRect(x, y, 2, 2);
            }
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

    // =========================================================================
    // DEFERRED EXECUTION — Calculate H3 Grid & Start Simulation
    // =========================================================================

    private void startSimulationDeferred() {
        startBtn.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(-1); // Indeterminate progress
        progressStatusLabel.setVisible(true);
        progressStatusLabel.setManaged(true);
        progressStatusLabel.setText("⚡ Generation of H3 grid cells in progress...");

        new Thread(() -> {
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
                            cfg.orbitalDistanceToParentKm(), cfg.co2Ppm()
                    );
                }

                logger.info("Generating H3 cells for scenario '{}' at resolution {}", scenarioNameField.getText(), cfg.resolution());
                List<H3Cell> cells = new ProceduralGenerator().generatePlanet(cfg);

                // Apply Geographical Clipping if enabled
                if (clippingCheckBox != null && clippingCheckBox.isSelected()) {
                    double minLat = minLatSpinner.getValue();
                    double maxLat = maxLatSpinner.getValue();
                    double minLng = minLngSpinner.getValue();
                    double maxLng = maxLngSpinner.getValue();

                    double marginLat = Math.max(1.0, (maxLat - minLat) * 0.08);
                    double marginLng = Math.max(1.0, (maxLng - minLng) * 0.08);

                    List<H3Cell> clippedCells = new ArrayList<>();
                    for (H3Cell c : cells) {
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

                distributeInitialPopulation(cells);
                currentPreviewCells = cells;

                final List<H3Cell> finalCells = cells;
                javafx.application.Platform.runLater(() -> {
                    progressBar.setProgress(1.0);
                    progressStatusLabel.setText("✅ " + finalCells.size() + " H3 cells calculated!");
                    drawPreview();
                    previewStatusLabel.setText("Généré : " + finalCells.size() + " cellules H3.");

                    Scenario s = getScenario();
                    if (onStartSimulation != null) {
                        onStartSimulation.accept(s);
                    }

                    startBtn.setDisable(false);
                });
            } catch (Exception ex) {
                logger.error("Error during deferred H3 cell calculation", ex);
                javafx.application.Platform.runLater(() -> {
                    progressStatusLabel.setText("❌ Error during calculation: " + ex.getMessage());
                    startBtn.setDisable(false);
                });
            }
        }).start();
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

        randomEventsCheckBox = new CheckBox(org.ether.society.i18n.I18n.getOrDefault("scenario.events.random_checkbox", "☑️ Générer dynamiquement les événements géologiques & dérives climatiques (séismes, volcanisme, élévation mer, Sahara vert)"));
        randomEventsCheckBox.setSelected(true);
        randomEventsCheckBox.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        HBox btnBar = new HBox(8, addEventBtn, removeEventBtn, new Separator(javafx.geometry.Orientation.VERTICAL), loadEarthEventsBtn);
        btnBar.setAlignment(Pos.CENTER_LEFT);

        section.getChildren().addAll(title3Events, randomEventsCheckBox, btnBar, eventsTable);
        return section;
    }

    private void loadEarthHistoricalEvents() {
        eventsList.clear();
        eventsList.add(new ClimateEvent("volcano", "Éruption Tambora (Indonesia)", 1815, -8.25, 117.98, 0.0, 7.0));
        eventsList.add(new ClimateEvent("volcano", "Éruption Krakatoa (Indonesia)", 1883, -6.10, 105.42, 0.0, 6.0));
        eventsList.add(new ClimateEvent("volcano", "Éruption Pinatubo (Philippines)", 1991, 15.13, 120.35, 0.0, 6.0));
        eventsList.add(new ClimateEvent("volcano", "Éruption Vésuve (Pompéi)", 79, 40.82, 14.43, 0.0, 5.0));
        eventsList.add(new ClimateEvent("volcano", "Éruption Santorini / Thera (Grèce)", -1640, 36.40, 25.40, 0.0, 7.0));
        eventsList.add(new ClimateEvent("earthquake", "Séisme Valdivia (Chili, M9.5)", 1960, -38.14, -73.41, 25.0, 9.5));
        eventsList.add(new ClimateEvent("earthquake", "Séisme Alaska (M9.2)", 1964, 61.02, -147.65, 25.0, 9.2));
        eventsList.add(new ClimateEvent("earthquake", "Séisme Sumatra-Andaman (M9.1)", 2004, 3.30, 95.98, 30.0, 9.1));
        eventsList.add(new ClimateEvent("earthquake", "Séisme Tohoku Japon (M9.0)", 2011, 38.30, 142.37, 29.0, 9.0));
        eventsList.add(new ClimateEvent("earthquake", "Séisme Lisbonne (Portugal)", 1755, 36.00, -10.50, 30.0, 8.7));
        eventsList.add(new ClimateEvent("tsunami", "Tsunami Sumatra (270 000 victimes)", 2004, 3.30, 95.98, 30.0, 9.1));
        eventsList.add(new ClimateEvent("tsunami", "Tsunami Tohoku Japon", 2011, 38.30, 142.37, 29.0, 9.0));
        eventsList.add(new ClimateEvent("tsunami", "Tsunami Krakatoa (36 000 victimes)", 1883, -6.10, 105.42, 0.0, 6.0));
        eventsList.add(new ClimateEvent("climate_drift", "Holocène Vert (Sahara Humide & Fertile)", -6000, 20.0, 10.0, 0.0, 3.0));
        eventsList.add(new ClimateEvent("climate_drift", "Optimum Climatique Médiéval (+1.2°C)", 1000, 45.0, 15.0, 0.0, 1.2));
        eventsList.add(new ClimateEvent("climate_drift", "Petit Âge Glaciaire (-1.5°C Maunder)", 1650, 50.0, 10.0, 0.0, -1.5));
        eventsList.add(new ClimateEvent("sea_level", "Élévation Littorale Moderne (+2.5m Submersion)", 2050, 0.0, 0.0, 0.0, 2.5));
        eventsList.add(new ClimateEvent("impact", "Impact Tunguska (Sibérie)", 1908, 60.89, 101.89, 0.0, 5.0));
        eventsList.add(new ClimateEvent("impact", "Impact Chicxulub (Extinction K-Pg)", -66000000, 21.40, -89.50, 0.0, 10.0));
        eventsList.add(new ClimateEvent("pandemic", "Peste Noire (Europe, 1/3 pop.)", 1347, 44.00, 10.00, 0.0, 9.0));
        eventsList.add(new ClimateEvent("pandemic", "Grippe Espagnole (50M victimes)", 1918, 40.00, 0.00, 0.0, 8.0));
        eventsList.add(new ClimateEvent("flood", "Grande Inondation Jaune (Chine)", 1931, 32.00, 118.00, 0.0, 8.0));
        logger.info("Loaded {} Earth historical events into events table.", eventsList.size());
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
        if (title1 != null) title1.setText(org.ether.society.i18n.I18n.get("scenario.title"));
        if (title3Events != null) title3Events.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events_section", "4. ÉVÉNEMENTS PLANÉTAIRES HISTORIQUES"));
        if (nameLabel != null) nameLabel.setText(org.ether.society.i18n.I18n.get("scenario.name"));
        if (eraLabel != null) eraLabel.setText(org.ether.society.i18n.I18n.get("scenario.era"));
        if (startYearLabel != null) startYearLabel.setText(org.ether.society.i18n.I18n.get("scenario.start_year"));
        if (popCountLabel != null) popCountLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_count", "Population Initiale :"));
        if (techLevelLabel != null) techLevelLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.tech_level", "Niveau Technologique :"));
        if (densityPatternLabel != null) densityPatternLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.density_pattern", "Motif de Répartition :"));
        if (urbanCentersLabel != null) urbanCentersLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.urban_centers", "Nœuds Urbains / Cités :"));
        if (h3ResolutionLabel != null) h3ResolutionLabel.setText(org.ether.society.i18n.I18n.getOrDefault("planet.param.resolution", "Résolution H3 :"));
        if (startBtn != null) startBtn.setText(org.ether.society.i18n.I18n.get("scenario.start_btn"));
        if (generateBtn != null) generateBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.preview_btn", "🔄 Prévisualiser la Répartition"));
        if (addEventBtn != null) addEventBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.add", "➕ Ajouter Événement"));
        if (removeEventBtn != null) removeEventBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.remove", "🗑️ Supprimer"));
        if (loadEarthEventsBtn != null) loadEarthEventsBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.load_earth", "🌍 Charger Événements Historiques Terre"));

        if (clippingHeader != null) clippingHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.header", "✂️ SIMULATION LOCALE & FRONTIÈRES (CLIPPING)"));
        if (clippingCheckBox != null) clippingCheckBox.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.enable", "Activer la simulation partielle (Zone Tronquée)"));
        if (graphicSelectBtn != null) graphicSelectBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.select_mode", "🖱️ Mode Sélection Graphique sur Carte"));
        if (resetClippingBtn != null) resetClippingBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.reset", "🔄 Réinitialiser la Zone (Pleine Planète)"));
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
    }

    public Scenario getScenario() {
        Scenario s = new Scenario();
        s.setName(scenarioNameField.getText());
        s.setStartDateYear(startYearSpinner.getValue());
        s.setInitialHumanCount(initialHumanCountSpinner.getValue());
        s.setInitialTechLevel(initialTechLevelSlider.getValue());
        s.setPopulationDensityType(densityPatternCombo.getValue());
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
}
