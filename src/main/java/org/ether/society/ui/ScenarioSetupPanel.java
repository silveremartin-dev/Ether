/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.ether.society.model.StartDatePreset;
import org.ether.society.procedural.PlanetPreset;
import org.ether.society.procedural.ProceduralGenerator;
import org.ether.society.procedural.ExternalDataService;
import org.ether.society.database.DataService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel for setting up civilization, initial population distribution, historical scenarios,
 * and civilizational start parameters.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.2.0
 */
public class ScenarioSetupPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioSetupPanel.class);

    // Context Headers
    private Label planetContextLabel;
    private Label ecologyContextLabel;
    private PlanetPreset activePlanetPreset;

    // Standardized Preset Bar
    private PresetControlBar<Scenario> scenarioPresetBar;

    // Form Controls
    private TextField scenarioNameField;
    private Spinner<Integer> startYearSpinner;
    private ComboBox<StartDatePreset> eraCombo;

    // Population & Civilization Controls
    private Spinner<Long> initialHumanCountSpinner;
    private Slider initialTechLevelSlider;
    private ComboBox<String> densityPatternCombo;
    private Spinner<Integer> urbanCentersSpinner;
    private Slider urbanRatioSlider;

    private ToggleGroup sourceGroup;
    private RadioButton sourceProcedural;
    private RadioButton sourceLoad;
    private RadioButton sourceReal;

    private VBox proceduralPanel;
    private VBox loadPanel;
    private VBox realDataPanel;

    private ComboBox<String> existingMapsCombo;

    // Map Preview Controls
    private Canvas previewCanvas;
    private ToggleButton viewBiomeBtn;
    private ToggleButton viewElevationBtn;
    private ToggleButton viewPopHeatmapBtn;
    private Label previewStatusLabel;

    // State
    private List<H3Cell> currentPreviewCells;
    private final Consumer<Scenario> onStartSimulation;
    private final org.ether.society.persistence.ScenarioRepository scenarioRepo;

    // Labels for i18n
    private Label title1;
    private Label title2;
    private Label nameLabel;
    private Label eraLabel;
    private Label startYearLabel;
    private Label popCountLabel;
    private Label techLevelLabel;
    private Label densityPatternLabel;
    private Button generateBtn;
    private Button startBtn;

    public ScenarioSetupPanel(Consumer<Scenario> onStartSimulation) {
        this.onStartSimulation = onStartSimulation;
        this.scenarioRepo = new org.ether.society.persistence.ScenarioRepository();
        initUI();
        updateTexts();

        org.ether.society.i18n.I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    public void setInheritedContext(PlanetPreset planetPreset, String ecologyName) {
        this.activePlanetPreset = planetPreset;
        String pName = planetPreset != null ? planetPreset.name() : "Earth-Like Standard";
        if (planetContextLabel != null) planetContextLabel.setText("🪐 Planète : " + pName);
        if (ecologyContextLabel != null) ecologyContextLabel.setText("🌿 Écologie : " + (ecologyName != null ? ecologyName : "Equilibrée"));
    }

    private void initUI() {
        setPadding(new Insets(20));
        getStyleClass().add("glass-panel");
        setStyle("-fx-background-color: transparent;");

        // Left: Configuration
        VBox configPane = createConfigPane();
        configPane.setPrefWidth(440);

        // Right: Preview
        VBox previewPane = createPreviewPane();

        setLeft(configPane);
        setCenter(previewPane);

        updateSourceVisibility();
    }

    private VBox createConfigPane() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(0, 20, 0, 0));

        // Inherited Context Banner
        planetContextLabel = new Label("🪐 Planète : Terrestre Standard");
        planetContextLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        ecologyContextLabel = new Label("🌿 Écologie : Standard Équilibrée");
        ecologyContextLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #34d399;");
        HBox contextBox = new HBox(12, planetContextLabel, ecologyContextLabel);
        contextBox.getStyleClass().add("card-section");

        // 1. Standardized Scenario Preset Control Bar
        scenarioPresetBar = new PresetControlBar<>("Scénario Preset");
        List<Scenario> builtInScenarios = getBuiltInScenarios();
        scenarioPresetBar.setPresets(builtInScenarios, builtInScenarios.get(0));
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

        // 2. Scenario General Info Section
        VBox section1 = new VBox(10);
        title1 = new Label("1. CIVILISATION & ÉPOQUE HISTORIQUE");
        title1.getStyleClass().add("label-header");

        GridPane grid1 = new GridPane();
        grid1.setHgap(10);
        grid1.setVgap(10);

        scenarioNameField = new TextField("Berceau de la Civilisation");
        eraCombo = new ComboBox<>();
        eraCombo.getItems().addAll(StartDatePreset.values());
        eraCombo.setValue(StartDatePreset.NEOLITHIZATION);
        eraCombo.setMaxWidth(Double.MAX_VALUE);
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

        startYearSpinner = new Spinner<>(-100000, 2100, -10000, 100);

        eraCombo.setOnAction(e -> {
            StartDatePreset p = eraCombo.getValue();
            if (p != null) {
                startYearSpinner.getValueFactory().setValue((int) p.getYear());
                scenarioNameField.setText(p.getDisplayName() + " Civilization");
            }
        });

        nameLabel = new Label("Nom du Scénario :");
        eraLabel = new Label("Époque :");
        startYearLabel = new Label("Année de Départ :");

        grid1.addRow(0, nameLabel, scenarioNameField);
        grid1.addRow(1, eraLabel, eraCombo);
        grid1.addRow(2, startYearLabel, startYearSpinner);

        section1.getChildren().addAll(title1, grid1);

        // 3. Population & Density Setup Section
        VBox popSection = new VBox(10);
        Label popHeader = new Label("2. DÉMOGRAPHIE & RÉPARTITION DE POPULATION");
        popHeader.getStyleClass().add("label-header");

        GridPane popGrid = new GridPane();
        popGrid.setHgap(10);
        popGrid.setVgap(10);

        initialHumanCountSpinner = new Spinner<>(100L, 50000000L, 50000L, 5000L);
        initialTechLevelSlider = new Slider(0.0, 10.0, 1.0);
        initialTechLevelSlider.setShowTickLabels(true);

        densityPatternCombo = new ComboBox<>();
        densityPatternCombo.getItems().addAll(
                "FERTILE_CRESCENT",      // Croissant Fertile (Nile, Tigre, Euphrate)
                "MESOAMERICA",            // Mésoamérique (Vallée de Mexico, Mayas)
                "MESOPOTAMIA_ASSYRIA",    // Mésopotamie & Empire Assyrien
                "RIVER_VALLEYS",          // Vallées fluviales majeures (Indus, Fleuve Jaune)
                "URBAN_CLUSTERS",         // Cités-états & nœuds urbains
                "SPARSE_NOMADIC"          // Population nomade/pastorale dispersée
        );
        densityPatternCombo.setValue("FERTILE_CRESCENT");
        densityPatternCombo.setMaxWidth(Double.MAX_VALUE);

        urbanCentersSpinner = new Spinner<>(1, 30, 5, 1);
        urbanRatioSlider = new Slider(0.1, 0.9, 0.4);

        popCountLabel = new Label("Population Initiale :");
        techLevelLabel = new Label("Niveau Technologique :");
        densityPatternLabel = new Label("Motif de Répartition :");

        popGrid.addRow(0, popCountLabel, initialHumanCountSpinner);
        popGrid.addRow(1, techLevelLabel, initialTechLevelSlider);
        popGrid.addRow(2, densityPatternLabel, densityPatternCombo);
        popGrid.addRow(3, new Label("Nœuds Urbains / Cités :"), urbanCentersSpinner);

        popSection.getChildren().addAll(popHeader, popGrid);

        // 4. World Source Section
        VBox section2 = new VBox(10);
        title2 = new Label("3. SOURCE DU TERRITOIRE");
        title2.getStyleClass().add("label-header");

        sourceGroup = new ToggleGroup();
        sourceProcedural = new RadioButton("Planète Procédurale (Onglet 1)");
        sourceLoad = new RadioButton("Carte depuis la BDD");
        sourceReal = new RadioButton("Vraie Terre (NASA/USGS)");

        sourceProcedural.setToggleGroup(sourceGroup);
        sourceLoad.setToggleGroup(sourceGroup);
        sourceReal.setToggleGroup(sourceGroup);
        sourceProcedural.setSelected(true);

        sourceGroup.selectedToggleProperty().addListener((o) -> updateSourceVisibility());

        HBox sourceBox = new HBox(12, sourceProcedural, sourceLoad, sourceReal);

        proceduralPanel = createProceduralPanel();
        loadPanel = createLoadPanel();
        realDataPanel = createRealDataPanel();

        StackPane subPanels = new StackPane(proceduralPanel, loadPanel, realDataPanel);
        section2.getChildren().addAll(title2, sourceBox, new Separator(), subPanels);

        // 5. Actions
        generateBtn = new Button("🔄 Prévisualiser la Répartition");
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        generateBtn.setStyle("-fx-font-weight: bold; -fx-base: #3498db;");
        generateBtn.setOnAction(e -> generatePreview());

        startBtn = new Button("🚀 LANCER LA SIMULATION");
        startBtn.setPrefHeight(50);
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-base: #2ecc71;");
        startBtn.setOnAction(e -> startSimulation());

        VBox bottom = new VBox(12, generateBtn, new Separator(), startBtn);
        VBox.setVgrow(bottom, Priority.ALWAYS);
        bottom.setAlignment(Pos.BOTTOM_CENTER);

        root.getChildren().addAll(contextBox, scenarioPresetBar, section1, new Separator(), popSection, new Separator(), section2, bottom);
        return root;
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

    private void applyScenarioToUI(Scenario s) {
        if (s == null) return;
        scenarioNameField.setText(s.getName());
        startYearSpinner.getValueFactory().setValue((int) s.getStartDateYear());
        initialHumanCountSpinner.getValueFactory().setValue(s.getInitialHumanCount());
        initialTechLevelSlider.setValue(s.getInitialTechLevel());

        if (s.getPopulationDensityType() != null && densityPatternCombo.getItems().contains(s.getPopulationDensityType())) {
            densityPatternCombo.setValue(s.getPopulationDensityType());
        }
    }

    public void updateTexts() {
        if (title1 != null) title1.setText(org.ether.society.i18n.I18n.get("scenario.title"));
        if (title2 != null) title2.setText(org.ether.society.i18n.I18n.get("scenario.world_title"));
        if (nameLabel != null) nameLabel.setText(org.ether.society.i18n.I18n.get("scenario.name"));
        if (eraLabel != null) eraLabel.setText(org.ether.society.i18n.I18n.get("scenario.era"));
        if (startYearLabel != null) startYearLabel.setText(org.ether.society.i18n.I18n.get("scenario.start_year"));
        if (startBtn != null) startBtn.setText(org.ether.society.i18n.I18n.get("scenario.start_btn"));
    }

    private VBox createProceduralPanel() {
        VBox p = new VBox(8);
        p.setPadding(new Insets(10));
        p.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 8;");
        Label infoLabel = new Label("🪐 Territoire, relief et géologie issus de l'Onglet 1 (Planète).\n🌿 Écologie et minerais issus de l'Onglet 2 (Ressources).");
        infoLabel.setWrapText(true);
        infoLabel.getStyleClass().add("control-label");
        p.getChildren().add(infoLabel);
        return p;
    }

    private VBox createLoadPanel() {
        VBox p = new VBox(8);
        existingMapsCombo = new ComboBox<>();
        refreshMapList();
        Button refreshBtn = new Button("Rafraîchir");
        refreshBtn.setOnAction(e -> refreshMapList());
        p.getChildren().addAll(new Label("Choisir une carte sauvegardée :"), new HBox(5, existingMapsCombo, refreshBtn));
        return p;
    }

    private VBox createRealDataPanel() {
        VBox p = new VBox(8);
        p.getChildren().add(new Label("Charge les données satellitaires réelles de la Terre (WMS/USGS)."));
        return p;
    }

    private VBox createPreviewPane() {
        VBox root = new VBox(10);
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("PREVISUALISATION DE LA CARTE ET DENSITÉ");
        title.getStyleClass().add("label-header");

        ToggleGroup viewGroup = new ToggleGroup();
        viewBiomeBtn = new ToggleButton("Biomes");
        viewElevationBtn = new ToggleButton("Relief");
        viewPopHeatmapBtn = new ToggleButton("🔥 Population");

        viewBiomeBtn.setToggleGroup(viewGroup);
        viewElevationBtn.setToggleGroup(viewGroup);
        viewPopHeatmapBtn.setToggleGroup(viewGroup);
        viewPopHeatmapBtn.setSelected(true);

        viewGroup.selectedToggleProperty().addListener(o -> drawPreview());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, viewBiomeBtn, viewElevationBtn, viewPopHeatmapBtn);

        Pane canvasContainer = new StackPane();
        canvasContainer.setStyle("-fx-background-color: black; -fx-border-color: #475569;");
        previewCanvas = new Canvas(600, 400);

        canvasContainer.getChildren().add(previewCanvas);
        VBox.setVgrow(canvasContainer, Priority.ALWAYS);

        previewStatusLabel = new Label("Aucune prévisualisation générée");
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

        String[] labels = { "Désert/Eau", "Faible", "Moyenne", "Élevée (Ville)", "Métropole" };
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

    private void updateSourceVisibility() {
        proceduralPanel.setVisible(sourceProcedural.isSelected());
        proceduralPanel.setManaged(sourceProcedural.isSelected());

        loadPanel.setVisible(sourceLoad.isSelected());
        loadPanel.setManaged(sourceLoad.isSelected());

        realDataPanel.setVisible(sourceReal.isSelected());
        realDataPanel.setManaged(sourceReal.isSelected());
    }

    private void refreshMapList() {
        try {
            DataService ds = new DataService();
            if (ds.isDatabaseAvailable()) {
                existingMapsCombo.getItems().setAll(ds.getAvailableMaps());
                if (!existingMapsCombo.getItems().isEmpty()) {
                    existingMapsCombo.setValue(existingMapsCombo.getItems().get(0));
                }
            }
        } catch (Exception e) {
            logger.error("Failed to list maps", e);
        }
    }

    private void generatePreview() {
        previewStatusLabel.setText("Génération de la prévisualisation...");

        new Thread(() -> {
            try {
                List<H3Cell> cells = null;
                if (sourceProcedural.isSelected()) {
                    PlanetPreset cfg = activePlanetPreset != null ? activePlanetPreset : new PlanetPreset("Custom", 6, 42, 0.5, 2.0, 0.0, 40.0);
                    cells = new ProceduralGenerator().generatePlanet(cfg);
                } else if (sourceReal.isSelected()) {
                    cells = ExternalDataService.loadEarthData();
                } else if (sourceLoad.isSelected()) {
                    String map = existingMapsCombo.getValue();
                    if (map != null) {
                        DataService ds = new DataService();
                        cells = ds.loadSimulation(map);
                    }
                }

                if (cells != null) {
                    // Inject initial population distribution onto cells according to selected pattern
                    distributeInitialPopulation(cells);

                    List<H3Cell> finalCells = cells;
                    javafx.application.Platform.runLater(() -> {
                        currentPreviewCells = finalCells;
                        drawPreview();
                        previewStatusLabel.setText("Généré : " + finalCells.size() + " cellules avec répartition démographique");
                    });
                }
            } catch (Exception e) {
                logger.error("Error generating preview", e);
                javafx.application.Platform.runLater(() -> previewStatusLabel.setText("Erreur: " + e.getMessage()));
            }
        }).start();
    }

    private void distributeInitialPopulation(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        long totalPop = initialHumanCountSpinner.getValue();
        String pattern = densityPatternCombo.getValue();

        // Filter habitable land cells
        List<H3Cell> landCells = cells.stream()
                .filter(c -> c.getElevation() > 0 && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                .toList();

        if (landCells.isEmpty()) return;

        double popPerCell = (double) totalPop / landCells.size();

        for (H3Cell c : landCells) {
            double factor = 1.0;
            if ("FERTILE_CRESCENT".equals(pattern) || "RIVER_VALLEYS".equals(pattern)) {
                // Boost lowland plains & river basins
                if (c.getBiome() == Biome.PLAINS || c.getBiome() == Biome.BEACH) factor = 4.0;
                else if (c.getBiome() == Biome.DESERT || c.getBiome() == Biome.SNOW) factor = 0.05;
            } else if ("MESOAMERICA".equals(pattern)) {
                if (c.getBiome() == Biome.JUNGLE || c.getBiome() == Biome.PLAINS || c.getBiome() == Biome.HILLS) factor = 3.0;
            } else if ("URBAN_CLUSTERS".equals(pattern) || "MESOPOTAMIA_ASSYRIA".equals(pattern)) {
                if (c.getBiome() == Biome.PLAINS) factor = 5.0;
            }
            c.setPopulation((int) (popPerCell * factor));
        }
    }

    private void drawPreview() {
        if (currentPreviewCells == null) return;

        GraphicsContext gc = previewCanvas.getGraphicsContext2D();
        double w = previewCanvas.getWidth();
        double h = previewCanvas.getHeight();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        double minLat = -90, maxLat = 90;
        double minLng = -180, maxLng = 180;

        boolean showElev = viewElevationBtn.isSelected();
        boolean showPop = viewPopHeatmapBtn.isSelected();

        double scaleX = w / (maxLng - minLng);
        double scaleY = h / (maxLat - minLat);
        double scale = Math.min(scaleX, scaleY) * 0.9;

        double offX = (w - (maxLng - minLng) * scale) / 2;
        double offY = (h - (maxLat - minLat) * scale) / 2;

        for (H3Cell c : currentPreviewCells) {
            double x = (c.getLongitude() - minLng) * scale + offX;
            double y = (maxLat - c.getLatitude()) * scale + offY;

            Color col;
            if (showPop) {
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
            } else if (showElev) {
                double norm = Math.max(0, Math.min(1, c.getElevation() / 6000.0));
                if (c.getElevation() <= 0) col = Color.DARKBLUE;
                else col = Color.gray(0.2 + 0.8 * norm);
            } else {
                col = getBiomeColor(c.getBiome());
            }

            gc.setFill(col);
            gc.fillRect(x, y, 2, 2);
        }
    }

    private Color getBiomeColor(Biome b) {
        if (b == null) return Color.BLACK;
        return switch (b) {
            case OCEAN -> Color.DARKBLUE;
            case DEEP_OCEAN -> Color.NAVY;
            case BEACH -> Color.SANDYBROWN;
            case DESERT -> Color.GOLD;
            case PLAINS -> Color.YELLOWGREEN;
            case FOREST -> Color.DARKGREEN;
            case JUNGLE -> Color.DARKSEAGREEN;
            case MOUNTAINS -> Color.GRAY;
            case HILLS -> Color.DARKGRAY;
            case TUNDRA -> Color.LIGHTBLUE;
            case SNOW -> Color.WHITE;
        };
    }

    private void startSimulation() {
        if (currentPreviewCells == null || currentPreviewCells.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Veuillez générer une prévisualisation avant de démarrer !");
            a.showAndWait();
            return;
        }

        Scenario s = getScenario();
        if (onStartSimulation != null) {
            onStartSimulation.accept(s);
        }
    }

    public Scenario getScenario() {
        Scenario s = new Scenario();
        s.setName(scenarioNameField.getText());
        s.setStartDateYear(startYearSpinner.getValue());
        s.setInitialHumanCount(initialHumanCountSpinner.getValue());
        s.setInitialTechLevel(initialTechLevelSlider.getValue());
        s.setPopulationDensityType(densityPatternCombo.getValue());

        if (sourceProcedural.isSelected()) {
            s.setPlanetPreset(activePlanetPreset);
            s.setUseRealEarthData(false);
        } else if (sourceReal.isSelected()) {
            s.setUseRealEarthData(true);
            s.setPlanetPreset(null);
        }
        return s;
    }

    public List<H3Cell> getCells() {
        return currentPreviewCells;
    }

    public void setGeneratedCells(List<H3Cell> cells) {
        this.currentPreviewCells = cells;
        drawPreview();
    }
}
