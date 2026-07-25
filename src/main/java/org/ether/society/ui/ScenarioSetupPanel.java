/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.ether.society.model.StartDatePreset;
import org.ether.society.procedural.ClimatePreset;
import org.ether.society.procedural.ElevationPreset;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Unified Panel for setting up a simulation.
 * Combines Scenario parameters, World Generation (Procedural/Real/Load), and
 * Preview.
 */
public class ScenarioSetupPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioSetupPanel.class);

    // Components
    private TextField scenarioNameField;
    private Spinner<Integer> startYearSpinner;
    private ComboBox<StartDatePreset> eraCombo;

    private ToggleGroup sourceGroup;
    private RadioButton sourceProcedural;
    private RadioButton sourceLoad;
    private RadioButton sourceReal;

    // Sub-panels for sources
    private VBox proceduralPanel;
    private VBox loadPanel;
    private VBox realDataPanel;

    // Procedural Controls
    private ComboBox<PlanetPreset> presetCombo;
    private ComboBox<ElevationPreset> elevationPresetCombo;
    private ComboBox<ClimatePreset> climatePresetCombo;
    private TextField seedField;
    private Slider waterSlider;
    private Slider tempSlider;

    // Planet Parameters
    private Slider elevationRangeSlider;
    private Slider seaLevelSlider;
    private Spinner<Double> planetRadiusSpinner;
    private Spinner<Double> cellRadiusSpinner;
    private Spinner<Double> axialTiltSpinner;
    private Spinner<Double> rotationPeriodSpinner;
    private Spinner<Double> revolutionPeriodSpinner;
    private Spinner<Double> solarIrradianceSpinner;

    // Load Controls
    private ComboBox<String> existingMapsCombo;

    // Preview
    private Canvas previewCanvas;
    private ToggleButton viewBiomeBtn;
    private ToggleButton viewElevationBtn;
    private Label previewStatusLabel;

    // Data State
    private List<H3Cell> currentPreviewCells;
    private Consumer<Scenario> onStartSimulation; // Callback

    private org.ether.society.persistence.ScenarioRepository scenarioRepo;
    private ComboBox<Scenario> savedScenariosCombo;

    // i18n Labels
    private Label title1;
    private Label title2;
    private Label nameLabel;
    private Label eraLabel;
    private Label startYearLabel;
    private Button loadBtn;
    private Button saveBtn;
    private Button generateBtn;
    private Button startBtn;

    public ScenarioSetupPanel(Consumer<Scenario> onStartSimulation) {
        this.onStartSimulation = onStartSimulation;
        this.scenarioRepo = new org.ether.society.persistence.ScenarioRepository();
        initUI();
        updateTexts();

        org.ether.society.i18n.I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    private void initUI() {
        setPadding(new Insets(20));
        getStyleClass().add("glass-panel");
        setStyle("-fx-background-color: transparent;");

        // Left: Configuration
        VBox configPane = createConfigPane();
        configPane.setPrefWidth(400);

        // Right: Preview
        VBox previewPane = createPreviewPane();

        setLeft(configPane);
        setCenter(previewPane);

        // Initial State
        updateSourceVisibility();
    }

    private VBox createConfigPane() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(0, 20, 0, 0));

        // 1. Scenario Details
        VBox section1 = new VBox(10);
        title1 = new Label();
        title1.getStyleClass().add("label-header");

        // Load Saved Scenario
        HBox loadBox = new HBox(10);
        savedScenariosCombo = new ComboBox<>();
        savedScenariosCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(savedScenariosCombo, Priority.ALWAYS);
        refreshScenarioList();

        loadBtn = new Button();
        loadBtn.setOnAction(e -> loadSelectedScenario());

        loadBox.getChildren().addAll(savedScenariosCombo, loadBtn);

        GridPane grid1 = new GridPane();
        grid1.setHgap(10);
        grid1.setVgap(10);

        scenarioNameField = new TextField("New Civilization");
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

        // When era selected, update year and name
        eraCombo.setOnAction(e -> {
            StartDatePreset p = eraCombo.getValue();
            if (p != null) {
                startYearSpinner.getValueFactory().setValue((int) p.getYear());
                scenarioNameField.setText(p.getDisplayName() + " Civilization");
            }
        });

        nameLabel = new Label();
        eraLabel = new Label();
        startYearLabel = new Label();

        grid1.addRow(0, nameLabel, scenarioNameField);
        grid1.addRow(1, eraLabel, eraCombo);
        grid1.addRow(2, startYearLabel, startYearSpinner);

        // Save Button
        saveBtn = new Button();
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> saveCurrentScenario());

        section1.getChildren().addAll(title1, loadBox, grid1, saveBtn);

        // 2. World Source
        VBox section2 = new VBox(10);
        title2 = new Label();
        title2.getStyleClass().add("label-header");

        sourceGroup = new ToggleGroup();
        sourceProcedural = new RadioButton();
        sourceLoad = new RadioButton();
        sourceReal = new RadioButton();

        sourceProcedural.setToggleGroup(sourceGroup);
        sourceLoad.setToggleGroup(sourceGroup);
        sourceReal.setToggleGroup(sourceGroup);
        sourceProcedural.setSelected(true);

        sourceGroup.selectedToggleProperty().addListener((o) -> updateSourceVisibility());

        HBox sourceBox = new HBox(15, sourceProcedural, sourceLoad, sourceReal);

        // Sub-panels
        proceduralPanel = createProceduralPanel();
        loadPanel = createLoadPanel();
        realDataPanel = createRealDataPanel(); // Placeholder

        StackPane subPanels = new StackPane(proceduralPanel, loadPanel, realDataPanel);

        section2.getChildren().addAll(title2, sourceBox, new Separator(), subPanels);

        // 3. Action
        generateBtn = new Button("Generate Preview");
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        generateBtn.setStyle("-fx-font-weight: bold; -fx-base: #3498db;");
        generateBtn.setOnAction(e -> generatePreview());

        // Start Button
        startBtn = new Button();
        startBtn.setPrefHeight(50);
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-base: #2ecc71;");
        startBtn.setOnAction(e -> startSimulation());

        VBox bottom = new VBox(15, generateBtn, new Separator(), startBtn);
        VBox.setVgrow(bottom, Priority.ALWAYS);
        bottom.setAlignment(Pos.BOTTOM_CENTER);

        root.getChildren().addAll(section1, new Separator(), section2, bottom);
        return root;
    }

    public void updateTexts() {
        if (title1 != null) title1.setText(org.ether.society.i18n.I18n.get("scenario.title"));
        if (title2 != null) title2.setText(org.ether.society.i18n.I18n.get("scenario.world_title"));
        if (nameLabel != null) nameLabel.setText(org.ether.society.i18n.I18n.get("scenario.name"));
        if (eraLabel != null) eraLabel.setText(org.ether.society.i18n.I18n.get("scenario.era"));
        if (startYearLabel != null) startYearLabel.setText(org.ether.society.i18n.I18n.get("scenario.start_year"));
        if (loadBtn != null) loadBtn.setText(org.ether.society.i18n.I18n.get("scenario.load_btn"));
        if (saveBtn != null) saveBtn.setText(org.ether.society.i18n.I18n.get("scenario.save_btn"));
        if (startBtn != null) startBtn.setText(org.ether.society.i18n.I18n.get("scenario.start_btn"));
        if (sourceProcedural != null) sourceProcedural.setText(org.ether.society.i18n.I18n.get("scenario.world_planet_gen"));
        if (sourceLoad != null) sourceLoad.setText(org.ether.society.i18n.I18n.get("scenario.world_load_map"));
        if (sourceReal != null) sourceReal.setText(org.ether.society.i18n.I18n.get("scenario.world_real_earth"));
    }

    private VBox createProceduralPanel() {
        VBox p = new VBox(12);
        p.setPadding(new Insets(15));
        p.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 8;");

        Label infoLabel = new Label("🪐 Planetary geography, topography, and physics are configured in Tab 1 (PLANET GENERATOR).\n\n" +
                                    "🌿 Ecological and resource distributions (Fauna, Flora, Ores) are tuned in Tab 2 (RESOURCES & ECOLOGY).\n\n" +
                                    "The generated world is automatically linked to this scenario.");
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");

        p.getChildren().add(infoLabel);
        return p;
    }

    private VBox createLoadPanel() {
        VBox p = new VBox(10);
        existingMapsCombo = new ComboBox<>();
        // Populate (handled on show/init normally, here simple refresh)
        refreshMapList();

        Button refreshBtn = new Button("Refresh List");
        refreshBtn.setOnAction(e -> refreshMapList());

        p.getChildren().addAll(new Label("Select Map:"), new HBox(5, existingMapsCombo, refreshBtn));
        return p;
    }

    private VBox createRealDataPanel() {
        VBox p = new VBox(10);
        p.getChildren().add(new Label("Loads approximate Earth data for now."));
        return p;
    }

    private VBox createPreviewPane() {
        VBox root = new VBox(10);
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("MAP PREVIEW");
        title.getStyleClass().add("label-header");

        ToggleGroup viewGroup = new ToggleGroup();
        viewBiomeBtn = new ToggleButton("Biomes");
        viewElevationBtn = new ToggleButton("Elevation");
        viewBiomeBtn.setToggleGroup(viewGroup);
        viewElevationBtn.setToggleGroup(viewGroup);
        viewElevationBtn.setSelected(true); // Default to elevation view
        viewBiomeBtn.setTooltip(new Tooltip("Show biome/vegetation colors"));
        viewElevationBtn.setTooltip(new Tooltip("Show elevation gradient"));

        viewGroup.selectedToggleProperty().addListener(o -> drawPreview());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, viewBiomeBtn, viewElevationBtn);

        // Canvas Container
        Pane canvasContainer = new StackPane();
        canvasContainer.setStyle("-fx-background-color: black; -fx-border-color: #7f8c8d;");
        previewCanvas = new Canvas(600, 400); // Fixed size for now, loop constrained

        // Resize logic not strictly needed if in StackPane, but beneficial
        canvasContainer.getChildren().add(previewCanvas);
        VBox.setVgrow(canvasContainer, Priority.ALWAYS);

        previewStatusLabel = new Label("No preview generated");

        // Elevation Legend
        HBox legendBox = createLegend();

        root.getChildren().addAll(header, canvasContainer, legendBox, previewStatusLabel);
        return root;
    }

    /**
     * Create a color legend for the preview.
     */
    private HBox createLegend() {
        HBox legend = new HBox(5);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(5));
        legend.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-background-radius: 5;");

        // Elevation gradient: ocean floor (-11000) through sea level (0) to peaks
        // (+9000)
        String[] labels = { "-11km", "-5km", "0 (sea)", "+3km", "+9km" };
        Color[] colors = {
                Color.rgb(15, 30, 60), // Deep ocean
                Color.rgb(30, 80, 150), // Shallow ocean
                Color.rgb(60, 130, 60), // Coastal/lowland
                Color.rgb(139, 119, 101), // Mountains
                Color.rgb(255, 255, 255) // Snow peaks
        };

        for (int i = 0; i < labels.length; i++) {
            javafx.scene.shape.Rectangle colorBox = new javafx.scene.shape.Rectangle(20, 12);
            colorBox.setFill(colors[i]);
            colorBox.setStroke(Color.GRAY);

            Label lbl = new Label(labels[i]);
            lbl.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 10px;");

            HBox item = new HBox(3, colorBox, lbl);
            item.setAlignment(Pos.CENTER);
            legend.getChildren().add(item);

            if (i < labels.length - 1) {
                Label arrow = new Label("->");
                arrow.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 10px;");
                legend.getChildren().add(arrow);
            }
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

    private void applyPreset(PlanetPreset p) {
        if (p == null)
            return;
        waterSlider.setValue(p.waterLevel());
        tempSlider.setValue(p.temperatureGradient());
        seedField.setText(String.valueOf(p.seed()));
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

    private void refreshScenarioList() {
        savedScenariosCombo.getItems().setAll(scenarioRepo.findAll());
        savedScenariosCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Scenario s) {
                return s != null ? s.getName() : "";
            }

            @Override
            public Scenario fromString(String string) {
                return null; // Not needed
            }
        });
    }

    private void saveCurrentScenario() {
        Scenario s = getScenario();
        scenarioRepo.saveOrUpdate(s);
        refreshScenarioList();

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Scenario '" + s.getName() + "' saved!");
        alert.show();
    }

    private void loadSelectedScenario() {
        Scenario s = savedScenariosCombo.getValue();
        if (s == null)
            return;

        scenarioNameField.setText(s.getName());
        startYearSpinner.getValueFactory().setValue((int) s.getStartDateYear());
        // Climate harshness loaded from scenario (no separate slider)

        // Load Planet Config
        if (s.getPlanetPreset() != null) {
            PlanetPreset pc = s.getPlanetPreset();
            sourceProcedural.setSelected(true);
            seedField.setText(String.valueOf(pc.seed()));
            waterSlider.setValue(pc.waterLevel());
            tempSlider.setValue(pc.temperatureGradient());
        }

        if (s.isUseRealEarthData()) {
            sourceReal.setSelected(true);
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Scenario Loaded!");
        alert.show();
    }

    private void generatePreview() {
        previewStatusLabel.setText("Generating...");

        // Run in thread
        new Thread(() -> {
            try {
                List<H3Cell> cells = null;
                if (sourceProcedural.isSelected()) {
                    PlanetPreset cfg = getPlanetPresetFromUI();
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
                    List<H3Cell> finalCells = cells;
                    javafx.application.Platform.runLater(() -> {
                        currentPreviewCells = finalCells;
                        drawPreview();
                        previewStatusLabel.setText("Generated " + finalCells.size() + " cells");
                    });
                }
            } catch (Exception e) {
                logger.error("Error generating preview", e);
                javafx.application.Platform.runLater(() -> previewStatusLabel.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private PlanetPreset getPlanetPresetFromUI() {
        long seed = 42;
        try {
            seed = Long.parseLong(seedField.getText());
        } catch (Exception ignored) {
        }

        return new PlanetPreset(
                "Custom",
                6, // Resolution
                seed,
                0.5, // Freq
                2.0, // Scale
                waterSlider.getValue(),
                tempSlider.getValue());
    }

    private void drawPreview() {
        if (currentPreviewCells == null)
            return;

        GraphicsContext gc = previewCanvas.getGraphicsContext2D();
        double w = previewCanvas.getWidth();
        double h = previewCanvas.getHeight();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        // Find bounds - FORCE FULL GLOBE for correct wrapping
        double minLat = -90;
        double maxLat = 90;
        double minLng = -180;
        double maxLng = 180;

        boolean showElev = viewElevationBtn.isSelected();

        double scaleX = w / (maxLng - minLng);
        double scaleY = h / (maxLat - minLat);
        double scale = Math.min(scaleX, scaleY) * 0.9;

        double offX = (w - (maxLng - minLng) * scale) / 2;
        double offY = (h - (maxLat - minLat) * scale) / 2;

        for (H3Cell c : currentPreviewCells) {
            double x = (c.getLongitude() - minLng) * scale + offX;
            double y = (maxLat - c.getLatitude()) * scale + offY;

            Color col;
            if (showElev) {
                double norm = Math.max(0, Math.min(1, c.getElevation() / 6000.0));
                if (c.getElevation() <= 0)
                    col = Color.DARKBLUE;
                else
                    col = Color.gray(0.2 + 0.8 * norm);
            } else {
                col = getBiomeColor(c.getBiome());
            }

            gc.setFill(col);
            gc.fillRect(x, y, 2, 2);
        }
    }

    private Color getBiomeColor(Biome b) {
        if (b == null)
            return Color.BLACK;
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
            Alert a = new Alert(Alert.AlertType.WARNING, "Please generate a preview first!");
            a.showAndWait();
            return;
        }

        Scenario s = getScenario(); // Use helper
        if (onStartSimulation != null) {
            onStartSimulation.accept(s);
        }
    }

    public Scenario getScenario() {
        Scenario s = new Scenario();
        s.setName(scenarioNameField.getText());
        s.setStartDateYear(startYearSpinner.getValue());
        s.setClimateHarshness(tempSlider.getValue()); // Use temperature slider as proxy for harshness

        if (sourceProcedural.isSelected()) {
            s.setPlanetPreset(getPlanetPresetFromUI());
            s.setUseRealEarthData(false);
        } else if (sourceReal.isSelected()) {
            s.setUseRealEarthData(true);
            s.setPlanetPreset(null);
        } else {
            // Load existing map - we don't save the map config back to scenario strictly in
            // this model
            // unless we loaded a scenario first.
            // For now, leave empty.
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
