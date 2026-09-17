/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.database.H3Cell;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.data.OnlineMapService;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Biome;
import org.ether.society.procedural.PlanetPreset;
import org.ether.society.procedural.ProceduralGenerator;
import org.ether.society.procedural.ProceduralGenerator.PlanetPoint;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Integrated UI Panel for procedural planet generation, astronomical physics, 
 * topography & heightmap imports, satellite/moon dynamics, macroclimates, and 3-map climate imports.
 * 
 * @author Silvere Martin-Michiellot
 * @version 2.4.0
 */
public class PlanetGeneratorPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(PlanetGeneratorPanel.class);

    private final ProceduralGenerator generator;
    private final ImageMapLoader mapLoader;
    private final OnlineMapService onlineMapService;
    private final Consumer<List<H3Cell>> onPlanetGeneratedCallback;

    // Custom Map Images
    private Image customElevImage;
    /** Reference to preset bar to notify when parameters change */
    private PresetControlBar<PlanetPreset> presetBar;
    private Image customBiomeImage;
    private Image customResourceImage;
    private Image customClimateImage;     // Temperature Map / Combined RGB Map
    private Image customRainfallImage;    // Separate Moisture / Precipitation Map
    private Image customSeasonalityImage; // Separate Seasonality / Temp Amplitude Map

    private String cachedElevB64;
    private String cachedBiomeB64;
    private String cachedResourceB64;
    private String cachedClimateB64;
    private String cachedRainfallB64;
    private String cachedSeasonalityB64;

    // Online & Export Buttons & Labels
    private Button fetchOnlineBtn;
    private Button fetchOnlineClimateBtn;
    private Button exportMapsBtn;
    private Label mapStatusLabel;

    // Controls
    private ComboBox<PlanetPreset> presetCombo;
    private ComboBox<String> mapSourceCombo;
    private TextField seedField;
    private Button randSeedBtn;
    private ComboBox<Integer> resolutionCombo;
    private RadioButton radioProc;
    private RadioButton radioImport;

    // Celestial Body Type & Satellite Orbit Controls
    private ComboBox<String> bodyTypeCombo;
    private Slider parentMassSlider;
    private Slider orbitDistanceParentSlider;
    private VBox satelliteRowBox;

    // Astronomical Controls
    private Slider radiusSlider;
    private Slider dayLengthSlider;
    private Slider axialTiltSlider;
    private Slider yearLengthSlider;
    private Slider distanceSunSlider;
    private Slider solarLumSlider;
    private Slider avgTempSlider;

    // Topography Controls
    private Slider minAltSlider;
    private Slider maxAltSlider;
    private Slider noiseFreqSlider;
    private Slider noiseScaleSlider;
    private Slider waterSlider;

    // Climate & Ecosystem Controls
    private Slider tempGradSlider;
    private Slider oxygenSlider;
    private Slider co2Slider;
    private Slider albedoSlider; // kept hidden — auto-calculated from biome
    private Slider atmoPressureSlider;

    // Climate sub-map radio toggles
    private RadioButton radioTempProc, radioTempImport;
    private RadioButton radioPrecipProc, radioPrecipImport;
    private RadioButton radioSeasonProc, radioSeasonImport;
    // Climate sub-map source combos (import mode)
    private ComboBox<String> tempSourceCombo, precipSourceCombo, seasonSourceCombo;
    // Climate sub-map seeds (procedural mode)
    private TextField tempSeedField, precipSeedField, seasonSeedField;
    // Climate sub-block labels (for i18n live update)
    private Label tempSubHeader, precipSubHeader, seasonSubHeader;
    private Label tempSeedLabel, precipSeedLabel, seasonSeedLabel;
    private Label tempHintLabel, precipHintLabel, seasonHintLabel;
    private Label tempSourceLabel, precipSourceLabel, seasonSourceLabel;
    private Label tempFormatHintLabel, precipFormatHintLabel, seasonFormatHintLabel;
    private Label earthPaleoNoticeLabel;

    // Validation Warning Banner (inline, non-blocking)
    private VBox validationWarningBanner;
    private Label validationWarningLabel;

    // Map Load Buttons & Labels
    private Label elevFileLabel;
    private Label biomeFileLabel;
    private Label resourceFileLabel;
    private Label climateFileLabel;
    private Label rainfallFileLabel;
    private Label seasonalityFileLabel;
    private Button loadElevBtn;
    private Button clearElevBtn;
    private Button loadBiomeBtn;
    private Button clearBiomeBtn;
    private Button loadResourceBtn;
    private Button clearResourceBtn;
    private Button loadClimateBtn;
    private Button clearClimateBtn;
    private Button loadRainfallBtn;
    private Button clearRainfallBtn;
    private Button loadSeasonalityBtn;
    private Button clearSeasonalityBtn;

    // UI Labels for i18n
    private Label headerLabel;
    private Label presetsSecHeader;
    private Label generalSecHeader;
    private Label customMapsSecHeader;
    private Label astroSecHeader;
    private Label topoSecHeader;
    private Label climateSecHeader;
    private Label previewTitle;
    private Label statsLabel;
    private Label astroLabel;
    private Label irradianceLabel;
    private Label altRangeLabel;
    private Label generateHintLabel;

    // Labels for control rows
    private Label presetRowLabel;
    private Label bodyTypeRowLabel;
    private Label parentMassRowLabel;
    private Label orbitDistanceParentRowLabel;
    private Label mapSourceRowLabel;
    private Label elevMapRowLabel;
    private Label biomeMapRowLabel;
    private Label resourceMapRowLabel;
    private Label climateMapRowLabel;
    private Label rainfallMapRowLabel;
    private Label seasonalityMapRowLabel;
    private Label radiusRowLabel;
    private Label resRowLabel;
    private Label dayRowLabel;
    private Label tiltRowLabel;
    private Label yearRowLabel;
    private Label distRowLabel;
    private Label lumRowLabel;
    private Label minAltRowLabel;
    private Label maxAltRowLabel;
    private Label tempRowLabel;
    private Label seedRowLabel;
    private Label waterRowLabel;
    private Label freqRowLabel;
    private Label scaleRowLabel;
    private Label gradRowLabel;
    private Label oxygenRowLabel;
    private Label co2RowLabel;
    private Label albedoRowLabel;
    private Label atmoPressureRowLabel;

    private ComboBox<String> viewModeCombo;
    private HBox legendBar;
    private Canvas previewCanvas;
    private boolean isUpdatingFromPreset = false;

    // Interactive Zoom/Pan State
    private double zoomFactor = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private double dragStartX, dragStartY;

    public PlanetGeneratorPanel(Consumer<List<H3Cell>> onPlanetGeneratedCallback) {
        this.generator = new ProceduralGenerator();
        this.mapLoader = new ImageMapLoader();
        this.onlineMapService = new OnlineMapService();
        this.onPlanetGeneratedCallback = onPlanetGeneratedCallback;

        getStyleClass().add("glass-panel");
        setPadding(new Insets(20));

        initUI();
        applyPreset(PlanetPreset.EARTH_LIKE);
        updateTexts();

        // Bind to i18n language changes
        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    private void initUI() {
        VBox controlsBox = new VBox(15);
        controlsBox.setPadding(new Insets(10));

        headerLabel = new Label();
        headerLabel.getStyleClass().add("label-title");

        validationWarningLabel = new Label();
        validationWarningLabel.setWrapText(true);
        validationWarningLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px;");
        validationWarningBanner = new VBox(4,
                new Label(I18n.getOrDefault("planet.validation.header", "⚠️ Paramètres ou cartes requis manquants ou invalides :")),
                validationWarningLabel
        );
        validationWarningBanner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10;");
        validationWarningBanner.getChildren().get(0).setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 13px;");
        validationWarningBanner.setVisible(false);
        validationWarningBanner.setManaged(false);

        // --- 1. Global Presets Control Bar ---
        PresetControlBar<PlanetPreset> topPresetBar = new PresetControlBar<>("planet.preset", "Préréglage Global");
        this.presetBar = topPresetBar;
        topPresetBar.setExportCategory("planetgenerator");
        presetCombo = topPresetBar.getPresetCombo();
        topPresetBar.setPresets(PlanetPreset.getPresets(), PlanetPreset.EARTH_LIKE);
        topPresetBar.setListener(new PresetControlBar.PresetActionsListener<PlanetPreset>() {
            @Override
            public void onPresetSelected(PlanetPreset preset) {
                if (!isUpdatingFromPreset) {
                    applyPreset(preset);
                    if (viewModeCombo != null) {
                        viewModeCombo.getSelectionModel().select(0);
                    }
                }
            }

            @Override
            public void onSavePreset(String name) {
                if (!validatePlanetSetup()) {
                    return;
                }
                PlanetPreset current = buildPresetFromUI();
                PlanetPreset custom = current.withName(name);
                topPresetBar.getPresetCombo().getItems().add(custom);
                topPresetBar.getPresetCombo().setValue(custom);
            }

            @Override
            public void onDeletePreset(PlanetPreset preset) {
                topPresetBar.getPresetCombo().getItems().remove(preset);
            }

            @Override
            public void onExportPreset(File targetFile, PlanetPreset preset) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.writeValue(targetFile, preset);
                    logger.info("Exported planet preset to {}", targetFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to export planet preset", ex);
                }
            }

            @Override
            public void onImportPreset(File sourceFile) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    PlanetPreset preset = mapper.readValue(sourceFile, PlanetPreset.class);
                    topPresetBar.getPresetCombo().getItems().add(preset);
                    topPresetBar.getPresetCombo().setValue(preset);
                    applyPreset(preset);
                    logger.info("Imported planet preset from {}", sourceFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to import planet preset", ex);
                }
            }
        });

        presetsSecHeader = new Label(I18n.getOrDefault("planet.section.presets", "GLOBAL PRESETS & PLANET SAVE"));
        VBox presetSection = createSection(presetsSecHeader, topPresetBar);

        // --- 2. General Parameters (seed moved to topo/procedural panel; kept for astro labels) ---
        VBox generalControls = new VBox(8);
        generalSecHeader = new Label(I18n.getOrDefault("planet.section.general", "GENERAL PARAMETERS"));
        resRowLabel = new Label();

        // Resolution combo (internal use only — displayed in Tab 3)
        resolutionCombo = new ComboBox<>();
        resolutionCombo.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8);
        resolutionCombo.setValue(3);
        resolutionCombo.setMaxWidth(Double.MAX_VALUE);
        resolutionCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : I18n.get("planet.param.resolution.res" + item));
            }
        });
        resolutionCombo.setButtonCell(resolutionCombo.getCellFactory().call(null));
        resolutionCombo.setOnAction(e -> {
            if (!isUpdatingFromPreset && presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });

        // generalControls is empty — seed is now inside the procedural panel
        VBox generalSection = createSection(generalSecHeader, generalControls);

        // --- 3. Astronomical & Physical Section (Planets & Natural Satellites / Moons) ---
        VBox astroControls = new VBox(8);

        bodyTypeRowLabel = new Label(I18n.getOrDefault("planet.param.body_type", "Celestial Body Type:"));
        bodyTypeCombo = new ComboBox<>();
        bodyTypeCombo.getItems().addAll("planet", "satellite");
        bodyTypeCombo.setValue("planet");
        bodyTypeCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText("satellite".equals(item) ? 
                            I18n.getOrDefault("planet.body_type.satellite", "🌙 Natural Satellite / Moon (Planetary Orbit)") :
                            I18n.getOrDefault("planet.body_type.planet", "🌍 Independent Planet (Direct Stellar Orbit)"));
                }
            }
        });
        bodyTypeCombo.setButtonCell(bodyTypeCombo.getCellFactory().call(null));
        bodyTypeCombo.setMaxWidth(Double.MAX_VALUE);
        bodyTypeCombo.setOnAction(e -> {
            if (!isUpdatingFromPreset && presetBar != null) presetBar.notifyParametersChanged();
            toggleSatelliteControls();
        });

        parentMassSlider = createSlider(0.1, 500.0, 1.0);
        orbitDistanceParentSlider = createSlider(5000, 2000000, 384400);

        parentMassRowLabel = new Label();
        orbitDistanceParentRowLabel = new Label();

        satelliteRowBox = new VBox(6,
                createControlRow(parentMassRowLabel, parentMassSlider, "%.1f M⊕", I18n.getOrDefault("planet.tooltip.parent_mass", "Mass of host planet around which satellite orbits")),
                createControlRow(orbitDistanceParentRowLabel, orbitDistanceParentSlider, "%.0f km", I18n.getOrDefault("planet.tooltip.parent_dist", "Average orbital distance around host planet"))
        );
        satelliteRowBox.setVisible(false);
        satelliteRowBox.setManaged(false);

        radiusSlider = createSlider(1000, 25000, 6371);
        dayLengthSlider = createSlider(1, 3000, 24);
        axialTiltSlider = createSlider(0, 180, 23.5);
        yearLengthSlider = createSlider(10, 12000, 365);
        distanceSunSlider = createSlider(0.1, 10.0, 1.0);
        solarLumSlider = createSlider(0.1, 5.0, 1.0);
        avgTempSlider = createSlider(-200, 500, 15);

        // Auto-calculate temperature from stellar irradiance when distance or luminosity changes
        distanceSunSlider.valueProperty().addListener((obs, old, val) -> calculateStellarIrradiance());
        solarLumSlider.valueProperty().addListener((obs, old, val) -> calculateStellarIrradiance());

        radiusRowLabel = new Label();
        dayRowLabel = new Label();
        tiltRowLabel = new Label();
        yearRowLabel = new Label();
        distRowLabel = new Label();
        lumRowLabel = new Label();
        tempRowLabel = new Label();

        irradianceLabel = new Label();
        irradianceLabel.getStyleClass().add("value-label");

        astroControls.getChildren().addAll(
                createControlRow(bodyTypeRowLabel, bodyTypeCombo, I18n.getOrDefault("planet.tooltip.body_type", "Toggle between independent planet and natural satellite/moon")),
                satelliteRowBox,
                createControlRow(radiusRowLabel, radiusSlider, "%.0f km", I18n.getOrDefault("planet.tooltip.radius", "Average planetary radius in km")),
                createControlRow(dayRowLabel, dayLengthSlider, "%.1f h", I18n.getOrDefault("planet.tooltip.day_length", "Planetary rotation duration in hours")),
                createControlRow(tiltRowLabel, axialTiltSlider, "%.1f°", I18n.getOrDefault("planet.tooltip.axial_tilt", "Inclinaison de l'axe de rotation")),
                createControlRow(yearRowLabel, yearLengthSlider, "%.0f d", I18n.getOrDefault("planet.tooltip.year_length", "Revolution duration in days")),
                createControlRow(distRowLabel, distanceSunSlider, "%.2f AU", I18n.getOrDefault("planet.tooltip.distance_sun", "Distance to central star in AU")),
                createControlRow(lumRowLabel, solarLumSlider, "%.2f L☉", I18n.getOrDefault("planet.tooltip.solar_lum", "Central star luminosity")),
                irradianceLabel,
                createControlRow(tempRowLabel, avgTempSlider, "%.1f °C", I18n.getOrDefault("planet.tooltip.avg_temp", "Average global surface temperature"))
        );

        astroSecHeader = new Label(I18n.getOrDefault("planet.section.astro", "1. ASTRONOMY, PHYSICS & CELESTIAL BODIES DOMAIN"));
        VBox astroSection = createSection(astroSecHeader, astroControls);

        // --- 4. Topography & Relief Section — Common Physical Scale Header + RadioButton: Procedural OR Import ---
        VBox topoControls = new VBox(10);

        minAltSlider = createSlider(-15000, -500, -11000);
        maxAltSlider = createSlider(500, 25000, 8848);
        minAltSlider.valueProperty().addListener((obs, old, val) -> updateAltRangeDisplay());
        maxAltSlider.valueProperty().addListener((obs, old, val) -> updateAltRangeDisplay());
        waterSlider = createSlider(-0.5, 1.0, 0.38);
        noiseFreqSlider = createSlider(0.1, 2.0, 1.0);
        noiseScaleSlider = createSlider(0.5, 3.0, 1.0);

        minAltRowLabel = new Label();
        maxAltRowLabel = new Label();
        waterRowLabel = new Label();
        freqRowLabel = new Label();
        scaleRowLabel = new Label();
        seedRowLabel = new Label();

        altRangeLabel = new Label();
        altRangeLabel.getStyleClass().add("value-label");
        altRangeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 2 0 4 0;");
        updateAltRangeDisplay();

        VBox commonTopoBox = new VBox(6,
                createControlRow(minAltRowLabel, minAltSlider, "%.0f m", I18n.getOrDefault("planet.tooltip.min_alt", "Absolute minimum altitude (ocean floor) [Corresponds to image level 0]")),
                createControlRow(maxAltRowLabel, maxAltSlider, "%.0f m", I18n.getOrDefault("planet.tooltip.max_alt", "Altitude maximale absolue (sommet montagneux) [Correspond au niveau 255 de l'image]")),
                altRangeLabel,
                createControlRow(waterRowLabel, waterSlider, "%.2f", I18n.getOrDefault("planet.tooltip.water_level", "Oceanic submergence threshold — 0.38 corresponds to ~71% submerged oceans on Earth"))
        );

        // Seed + random button (inside procedural panel)
        seedField = new TextField("12345");
        seedField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdatingFromPreset && presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });
        randSeedBtn = new Button("🎲");
        randSeedBtn.getStyleClass().add("button-secondary");
        randSeedBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.seed_rand", "New random seed")));
        randSeedBtn.setOnAction(e -> {
            seedField.setText(String.valueOf(new Random().nextLong(1000000)));
            if (!isUpdatingFromPreset && presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });
        HBox seedBox = new HBox(5, seedField, randSeedBtn);
        HBox.setHgrow(seedField, Priority.ALWAYS);

        // Resolution info label relative to planet radius
        Label resolutionInfoLabel = new Label();
        resolutionInfoLabel.getStyleClass().add("card-description-muted");
        resolutionInfoLabel.setWrapText(true);
        radiusSlider.valueProperty().addListener((obs, old, val) -> {
            double r = val.doubleValue();
            double circumference = 2 * Math.PI * r;
            int rec = circumference > 60000 ? 2048 : (circumference > 20000 ? 1024 : 512);
            resolutionInfoLabel.setText(String.format(
                I18n.getOrDefault("planet.hint.map_resolution",
                    "📎 Rayon %.0f km → circonférence %.0f km. Résolution recommandée : %d×%d px min."),
                r, circumference, rec, rec / 2));
        });
        double initR = radiusSlider.getValue();
        double initC = 2 * Math.PI * initR;
        int initRec = initC > 60000 ? 2048 : (initC > 20000 ? 1024 : 512);
        resolutionInfoLabel.setText(String.format(
            I18n.getOrDefault("planet.hint.map_resolution",
                "📎 Rayon %.0f km → circonférence %.0f km. Résolution recommandée : %d×%d px min."),
            initR, initC, initRec, initRec / 2));

        // Export procedural heightmap button
        Button exportProceduralBtn = new Button(I18n.getOrDefault("planet.btn.export_procedural", "📤 Export Procedural Heightmap (PNG)"));
        exportProceduralBtn.setMaxWidth(Double.MAX_VALUE);
        exportProceduralBtn.getStyleClass().add("button-secondary");
        exportProceduralBtn.setOnAction(e -> exportProceduralHeightmap());

        // --- RadioButton toggle: Procedural vs Import ---
        ToggleGroup elevSourceGroup = new ToggleGroup();
        radioProc = new RadioButton(I18n.getOrDefault("planet.radio.procedural", "▶ Procedural Generation (Perlin Noise)"));
        radioImport = new RadioButton(I18n.getOrDefault("planet.radio.import", "📂 Import Heightmap Externe (PNG/GeoTIFF)"));
        radioProc.setToggleGroup(elevSourceGroup);
        radioImport.setToggleGroup(elevSourceGroup);
        radioProc.setSelected(true);
        radioProc.getStyleClass().add("radio-proc");
        radioImport.getStyleClass().add("radio-import");

        // Procedural panel (contains seed, noise frequency, noise scale)
        VBox proceduralPanel = new VBox(8,
                createControlRow(seedRowLabel, seedBox, I18n.getOrDefault("planet.tooltip.seed", "Random seed for deterministic generation")),
                createControlRow(freqRowLabel, noiseFreqSlider, "%.2f", I18n.getOrDefault("planet.tooltip.noise_freq", "Spatial frequency of altimetric noise")),
                createControlRow(scaleRowLabel, noiseScaleSlider, "%.2f", I18n.getOrDefault("planet.tooltip.noise_scale", "Elevation amplitude scale (mountains / plains)")),
                exportProceduralBtn,
                resolutionInfoLabel
        );
        proceduralPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");

        // Import panel
        mapSourceRowLabel = new Label();
        mapSourceCombo = new ComboBox<>();
        mapSourceCombo.getItems().addAll("", "earth", "mars", "venus", "moon", "mercury");
        mapSourceCombo.setValue("earth");
        mapSourceCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText(I18n.getOrDefault("common.combo.prompt_source", "— Select a data source —"));
                } else {
                    setText(I18n.get("planet.map." + item));
                }
            }
        });
        mapSourceCombo.setButtonCell(mapSourceCombo.getCellFactory().call(null));
        mapSourceCombo.setMaxWidth(Double.MAX_VALUE);
        mapSourceCombo.setOnAction(e -> {
            if (isUpdatingFromPreset) return;
            String sel = mapSourceCombo.getValue();
            if (sel != null && !sel.isEmpty()) {
                applyMapSourcePreset(sel);
                if (presetBar != null) presetBar.notifyParametersChanged();
                updatePreview();
            }
        });

        elevMapRowLabel = new Label();
        elevFileLabel = new Label(I18n.get("planet.map.none"));
        elevFileLabel.getStyleClass().add("value-label");
        biomeFileLabel = new Label(I18n.get("planet.map.none"));
        biomeFileLabel.getStyleClass().add("value-label");
        resourceFileLabel = new Label(I18n.get("planet.map.none"));
        resourceFileLabel.getStyleClass().add("value-label");
        mapStatusLabel = new Label();
        mapStatusLabel.getStyleClass().add("value-label");
        loadElevBtn = new Button(I18n.get("planet.map.btn_load"));
        loadElevBtn.getStyleClass().add("button-secondary");
        loadElevBtn.setOnAction(e -> chooseElevMapFile());
        clearElevBtn = new Button("❌");
        clearElevBtn.getStyleClass().add("button-secondary");
        clearElevBtn.setOnAction(e -> {
            customElevImage = null;
            elevFileLabel.setText(I18n.get("planet.map.none"));
            if (presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });
        HBox elevBox = new HBox(5, loadElevBtn, clearElevBtn);

        VBox importPanel = new VBox(8,
                createControlRow(mapSourceRowLabel, mapSourceCombo,
                    I18n.getOrDefault("planet.tooltip.map_source", "Celestial body preset (Earth, Mars, Venus, Moon) — selects predefined elevation map")),
                createControlRow(elevMapRowLabel, new VBox(3, elevBox, elevFileLabel),
                    I18n.getOrDefault("planet.tooltip.elev_map", "Import d'une heightmap PNG en niveaux de gris (noir=min alt, blanc=max alt)"))
        );
        importPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(167,139,250,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        importPanel.setVisible(false);
        importPanel.setManaged(false);

        // Wire RadioButton visibility toggle & preview mode synchronization
        elevSourceGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isProc = sel == radioProc;
            proceduralPanel.setVisible(isProc);
            proceduralPanel.setManaged(isProc);
            importPanel.setVisible(!isProc);
            importPanel.setManaged(!isProc);
            if (!isProc && customElevImage == null && mapSourceCombo.getValue() != null && !mapSourceCombo.getValue().isEmpty()) {
                applyMapSourcePreset(mapSourceCombo.getValue());
            }
            if (viewModeCombo != null) {
                viewModeCombo.getSelectionModel().select(0);
            }
            if (!isUpdatingFromPreset) {
                if (presetBar != null) presetBar.notifyParametersChanged();
                updatePreview();
            }
        });
        radioProc.setOnAction(e -> {
            if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(0);
            updatePreview();
        });
        radioImport.setOnAction(e -> {
            if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(0);
            updatePreview();
        });

        topoControls.getChildren().addAll(
                commonTopoBox,
                new Separator(),
                radioProc,
                proceduralPanel,
                radioImport,
                importPanel
        );

        topoSecHeader = new Label(I18n.getOrDefault("planet.section.topo", "2. TOPOGRAPHY & PLANETARY RELIEF DOMAIN"));
        VBox topoSection = createSection(topoSecHeader, topoControls);

        // --- 5. Climate & Ecosystem Section — 3 independent sub-blocks ---
        // Global atmospheric sliders (always visible)
        tempGradSlider   = createSlider(0, 100, 40);
        oxygenSlider     = createSlider(0, 50, 21);
        co2Slider        = createSlider(0, 1_000_000, 420);
        albedoSlider     = createSlider(0.0, 1.0, 0.30); // hidden – auto-calculated
        atmoPressureSlider = createSlider(0.0, 10.0, 1.0);

        albedoSlider.valueProperty().addListener((obs, old, val) -> calculateStellarIrradiance());
        atmoPressureSlider.valueProperty().addListener((obs, old, val) -> calculateStellarIrradiance());
        co2Slider.valueProperty().addListener((obs, old, val) -> calculateStellarIrradiance());
        tempGradSlider.valueProperty().addListener((obs, old, val) -> updatePreview());

        gradRowLabel        = new Label();
        oxygenRowLabel      = new Label();
        co2RowLabel         = new Label();
        albedoRowLabel      = new Label();
        atmoPressureRowLabel = new Label();

        VBox atmosphericGlobal = new VBox(8,
                createControlRow(gradRowLabel, tempGradSlider, "%.1f °C",
                        "Gradient thermique équateur-pôles (différentiel thermique en °C entre l'équateur et les pôles)"),
                createControlRow(oxygenRowLabel, oxygenSlider, "%.1f %%",
                        "Taux d'oxygène atmosphérique O₂ — 21% pour Terre ; < 10% = hypoxie ; > 35% = risque incendie"),
                createControlRow(co2RowLabel, co2Slider, "%.0f ppm",
                        "CO₂ atmosphérique (ppm). Terre actuelle: 420 ppm. Crétacé: ~1000-2000 ppm. Vénus: ~960 000 ppm"),
                createControlRow(atmoPressureRowLabel, atmoPressureSlider, "%.2f atm",
                        "Pression atmosphérique au sol. Terre = 1.0 atm. Mars ≈ 0.006 atm. Vénus ≈ 92 atm")
        );

        // --- Helper: build a climate map sub-block ---
        // SUB-BLOCK 1: Temperature / Thermal Map
        ToggleGroup tempToggle = new ToggleGroup();
        radioTempProc   = new RadioButton(I18n.getOrDefault("planet.radio.procedural", "▶ Procedural Generation (Perlin Noise)"));
        radioTempImport = new RadioButton(I18n.getOrDefault("planet.radio.import_wms", "📂 External Source (PNG / WMS)"));
        radioTempProc.setToggleGroup(tempToggle); radioTempProc.setSelected(true);
        radioTempProc.getStyleClass().add("radio-proc");
        radioTempImport.getStyleClass().add("radio-import");
        radioTempImport.setToggleGroup(tempToggle);

        tempSeedField = new TextField("54321");
        tempSeedField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdatingFromPreset && presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });
        Button tempRandBtn = new Button("🎲");
        tempRandBtn.getStyleClass().add("button-secondary");
        tempRandBtn.setOnAction(e -> {
            tempSeedField.setText(String.valueOf(new java.util.Random().nextLong(1_000_000)));
            if (!isUpdatingFromPreset && presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });
        HBox tempSeedBox = new HBox(5, tempSeedField, tempRandBtn); HBox.setHgrow(tempSeedField, Priority.ALWAYS);

        tempSeedLabel = new Label(I18n.getOrDefault("planet.climate.seed_label", "Generation Seed:"));
        tempHintLabel = new Label(I18n.getOrDefault("planet.climate.temp.hint",
                "ℹ  The thermal map is derived from the equator-to-pole gradient and the random seed bias."));
        tempHintLabel.setWrapText(true);
        tempHintLabel.getStyleClass().add("card-description-muted");

        Button exportTempBtn = new Button(I18n.getOrDefault("planet.btn.export_climate_temp", "📤 Export Procedural Thermal Map (PNG)"));
        exportTempBtn.setMaxWidth(Double.MAX_VALUE);
        exportTempBtn.getStyleClass().add("button-secondary");
        exportTempBtn.setOnAction(e -> exportProceduralClimateMap("temp"));

        VBox tempProcPanel = new VBox(6, tempSeedLabel, tempSeedBox, tempHintLabel, exportTempBtn);
        tempProcPanel.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(168, 85, 247, 0.3); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");

        tempSourceCombo = buildClimateSourceCombo("temp");
        climateFileLabel = new Label("—"); climateFileLabel.getStyleClass().add("value-label");
        loadClimateBtn = new Button(I18n.get("planet.map.btn_load")); loadClimateBtn.getStyleClass().add("button-secondary");
        loadClimateBtn.setOnAction(e -> chooseClimateMapFile());
        clearClimateBtn = new Button("❌"); clearClimateBtn.getStyleClass().add("button-secondary");
        clearClimateBtn.setOnAction(e -> {
            customClimateImage = null;
            climateFileLabel.setText("—");
            if (!isUpdatingFromPreset && presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });
        HBox tempImportBtns = new HBox(5, loadClimateBtn, clearClimateBtn);

        tempFormatHintLabel = new Label(I18n.getOrDefault("planet.climate.temp.format",
                "Grayscale PNG (equirectangular 2:1):\n  Black (0) = −50°C | White (255) = +50°C"));
        tempFormatHintLabel.setWrapText(true);
        tempFormatHintLabel.getStyleClass().add("card-description-muted");
        tempSourceLabel = new Label(I18n.getOrDefault("planet.climate.source_label", "Reference Source:"));
        VBox tempImportPanel = new VBox(6, tempSourceLabel, tempSourceCombo, tempImportBtns, climateFileLabel, tempFormatHintLabel);
        tempImportPanel.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(168, 85, 247, 0.3); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
        tempImportPanel.setVisible(false); tempImportPanel.setManaged(false);

        tempToggle.selectedToggleProperty().addListener((obs, o, sel) -> {
            boolean isProc = sel == radioTempProc;
            tempProcPanel.setVisible(isProc);   tempProcPanel.setManaged(isProc);
            tempImportPanel.setVisible(!isProc); tempImportPanel.setManaged(!isProc);
            if (!isUpdatingFromPreset) {
                if (viewModeCombo != null) {
                    viewModeCombo.getSelectionModel().select(1);
                }
                if (presetBar != null) presetBar.notifyParametersChanged();
                updatePreview();
            }
        });
        radioTempProc.setOnAction(e -> {
            if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(1);
            updatePreview();
        });
        radioTempImport.setOnAction(e -> {
            if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(1);
            updatePreview();
        });
        tempSubHeader = new Label(I18n.getOrDefault("planet.climate.temp.header", "🌡  Temperature & Thermal:"));
        tempSubHeader.getStyleClass().add("control-label");
        VBox tempSubBlock = new VBox(6, tempSubHeader, radioTempProc, tempProcPanel, radioTempImport, tempImportPanel);
        tempSubBlock.setStyle("-fx-padding: 10 10 10 10; -fx-background-color: rgba(168, 85, 247, 0.08); -fx-background-radius: 6; -fx-border-color: rgba(168, 85, 247, 0.25); -fx-border-radius: 6;");

        // SUB-BLOCK 2: Precipitation / Rainfall Map
        ToggleGroup precipToggle = new ToggleGroup();
        radioPrecipProc   = new RadioButton(I18n.getOrDefault("planet.radio.procedural", "▶ Procedural Generation (Perlin Noise)"));
        radioPrecipImport = new RadioButton(I18n.getOrDefault("planet.radio.import_wms", "📂 External Source (PNG / WMS)"));
        radioPrecipProc.setToggleGroup(precipToggle); radioPrecipProc.setSelected(true);
        radioPrecipProc.getStyleClass().add("radio-proc");
        radioPrecipImport.getStyleClass().add("radio-import");
        radioPrecipImport.setToggleGroup(precipToggle);

        precipSeedField = new TextField("11111");
        precipSeedField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdatingFromPreset && presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });
        Button precipRandBtn = new Button("🎲");
        precipRandBtn.getStyleClass().add("button-secondary");
        precipRandBtn.setOnAction(e -> {
            precipSeedField.setText(String.valueOf(new java.util.Random().nextLong(1_000_000)));
            if (!isUpdatingFromPreset && presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });
        HBox precipSeedBox = new HBox(5, precipSeedField, precipRandBtn); HBox.setHgrow(precipSeedField, Priority.ALWAYS);

        precipSeedLabel = new Label(I18n.getOrDefault("planet.climate.seed_label", "Generation Seed:"));
        precipHintLabel = new Label(I18n.getOrDefault("planet.climate.precip.hint",
                "ℹ  The rainfall map is generated from the latitudinal hygrometric bias and seed."));
        precipHintLabel.setWrapText(true);
        precipHintLabel.getStyleClass().add("card-description-muted");

        Button exportPrecipBtn = new Button(I18n.getOrDefault("planet.btn.export_climate_precip", "📤 Export Procedural Rainfall Map (PNG)"));
        exportPrecipBtn.setMaxWidth(Double.MAX_VALUE);
        exportPrecipBtn.getStyleClass().add("button-secondary");
        exportPrecipBtn.setOnAction(e -> exportProceduralClimateMap("precip"));

        VBox precipProcPanel = new VBox(6, precipSeedLabel, precipSeedBox, precipHintLabel, exportPrecipBtn);
        precipProcPanel.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(2, 132, 199, 0.3); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");

        precipSourceCombo = buildClimateSourceCombo("precip");
        rainfallFileLabel = new Label("—"); rainfallFileLabel.getStyleClass().add("value-label");
        loadRainfallBtn = new Button(I18n.get("planet.map.btn_load")); loadRainfallBtn.getStyleClass().add("button-secondary");
        loadRainfallBtn.setOnAction(e -> chooseRainfallMapFile());
        clearRainfallBtn = new Button("❌"); clearRainfallBtn.getStyleClass().add("button-secondary");
        clearRainfallBtn.setOnAction(e -> {
            customRainfallImage = null;
            rainfallFileLabel.setText("—");
            if (!isUpdatingFromPreset && presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });
        HBox precipImportBtns = new HBox(5, loadRainfallBtn, clearRainfallBtn);

        precipFormatHintLabel = new Label(I18n.getOrDefault("planet.climate.precip.format",
                "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 0 mm/yr | White (255) = 3,000 mm/yr"));
        precipFormatHintLabel.setWrapText(true);
        precipFormatHintLabel.getStyleClass().add("card-description-muted");
        precipSourceLabel = new Label(I18n.getOrDefault("planet.climate.source_label", "Reference Source:"));
        VBox precipImportPanel = new VBox(6, precipSourceLabel, precipSourceCombo, precipImportBtns, rainfallFileLabel, precipFormatHintLabel);
        precipImportPanel.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(2, 132, 199, 0.3); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
        precipImportPanel.setVisible(false); precipImportPanel.setManaged(false);

        precipToggle.selectedToggleProperty().addListener((obs, o, sel) -> {
            boolean isProc = sel == radioPrecipProc;
            precipProcPanel.setVisible(isProc);   precipProcPanel.setManaged(isProc);
            precipImportPanel.setVisible(!isProc); precipImportPanel.setManaged(!isProc);
            if (!isUpdatingFromPreset) {
                if (viewModeCombo != null) {
                    viewModeCombo.getSelectionModel().select(2);
                }
                if (presetBar != null) presetBar.notifyParametersChanged();
                updatePreview();
            }
        });
        radioPrecipProc.setOnAction(e -> {
            if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(2);
            updatePreview();
        });
        radioPrecipImport.setOnAction(e -> {
            if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(2);
            updatePreview();
        });
        precipSubHeader = new Label(I18n.getOrDefault("planet.climate.precip.header", "🌧  Precipitation / Humidity:"));
        precipSubHeader.getStyleClass().add("control-label");
        VBox precipSubBlock = new VBox(6, precipSubHeader, radioPrecipProc, precipProcPanel, radioPrecipImport, precipImportPanel);
        precipSubBlock.setStyle("-fx-padding: 10 10 10 10; -fx-background-color: rgba(2, 132, 199, 0.08); -fx-background-radius: 6; -fx-border-color: rgba(2, 132, 199, 0.25); -fx-border-radius: 6;");

        // SUB-BLOCK 3: Seasonality / Thermal Variance Map
        ToggleGroup seasonToggle = new ToggleGroup();
        radioSeasonProc   = new RadioButton(I18n.getOrDefault("planet.radio.procedural", "▶ Procedural Generation (Perlin Noise)"));
        radioSeasonImport = new RadioButton(I18n.getOrDefault("planet.radio.import_wms", "📂 External Source (PNG / WMS)"));
        radioSeasonProc.setToggleGroup(seasonToggle); radioSeasonProc.setSelected(true);
        radioSeasonProc.getStyleClass().add("radio-proc");
        radioSeasonImport.getStyleClass().add("radio-import");
        radioSeasonImport.setToggleGroup(seasonToggle);

        seasonSeedField = new TextField("99999");
        seasonSeedField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdatingFromPreset && presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });
        Button seasonRandBtn = new Button("🎲");
        seasonRandBtn.getStyleClass().add("button-secondary");
        seasonRandBtn.setOnAction(e -> {
            seasonSeedField.setText(String.valueOf(new java.util.Random().nextLong(1_000_000)));
            if (!isUpdatingFromPreset && presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });
        HBox seasonSeedBox = new HBox(5, seasonSeedField, seasonRandBtn); HBox.setHgrow(seasonSeedField, Priority.ALWAYS);

        seasonSeedLabel = new Label(I18n.getOrDefault("planet.climate.seed_label", "Generation Seed:"));
        seasonHintLabel = new Label(I18n.getOrDefault("planet.climate.season.hint",
                "ℹ  Seasonality is derived from axial tilt and a random latitudinal bias."));
        seasonHintLabel.setWrapText(true);
        seasonHintLabel.getStyleClass().add("card-description-muted");

        Button exportSeasonBtn = new Button(I18n.getOrDefault("planet.btn.export_climate_season", "📤 Export Procedural Seasonality Map (PNG)"));
        exportSeasonBtn.setMaxWidth(Double.MAX_VALUE);
        exportSeasonBtn.getStyleClass().add("button-secondary");
        exportSeasonBtn.setOnAction(e -> exportProceduralClimateMap("season"));

        VBox seasonProcPanel = new VBox(6, seasonSeedLabel, seasonSeedBox, seasonHintLabel, exportSeasonBtn);
        seasonProcPanel.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(245, 158, 11, 0.3); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");

        seasonSourceCombo = buildClimateSourceCombo("season");
        seasonalityFileLabel = new Label("—"); seasonalityFileLabel.getStyleClass().add("value-label");
        loadSeasonalityBtn = new Button(I18n.get("planet.map.btn_load")); loadSeasonalityBtn.getStyleClass().add("button-secondary");
        loadSeasonalityBtn.setOnAction(e -> chooseSeasonalityMapFile());
        clearSeasonalityBtn = new Button("❌"); clearSeasonalityBtn.getStyleClass().add("button-secondary");
        clearSeasonalityBtn.setOnAction(e -> {
            customSeasonalityImage = null;
            seasonalityFileLabel.setText("—");
            if (!isUpdatingFromPreset && presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });
        HBox seasonImportBtns = new HBox(5, loadSeasonalityBtn, clearSeasonalityBtn);

        seasonFormatHintLabel = new Label(I18n.getOrDefault("planet.climate.season.format",
                "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 0°C amplitude | White (255) = 50°C annual amplitude"));
        seasonFormatHintLabel.setWrapText(true);
        seasonFormatHintLabel.getStyleClass().add("card-description-muted");
        seasonSourceLabel = new Label(I18n.getOrDefault("planet.climate.source_label", "Reference Source:"));
        VBox seasonImportPanel = new VBox(6, seasonSourceLabel, seasonSourceCombo, seasonImportBtns, seasonalityFileLabel, seasonFormatHintLabel);
        seasonImportPanel.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(245, 158, 11, 0.3); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
        seasonImportPanel.setVisible(false); seasonImportPanel.setManaged(false);

        seasonToggle.selectedToggleProperty().addListener((obs, o, sel) -> {
            boolean isProc = sel == radioSeasonProc;
            seasonProcPanel.setVisible(isProc);   seasonProcPanel.setManaged(isProc);
            seasonImportPanel.setVisible(!isProc); seasonImportPanel.setManaged(!isProc);
            if (!isUpdatingFromPreset) {
                if (viewModeCombo != null) {
                    viewModeCombo.getSelectionModel().select(3);
                }
                if (presetBar != null) presetBar.notifyParametersChanged();
                updatePreview();
            }
        });
        radioSeasonProc.setOnAction(e -> {
            if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(3);
            updatePreview();
        });
        radioSeasonImport.setOnAction(e -> {
            if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(3);
            updatePreview();
        });
        seasonSubHeader = new Label(I18n.getOrDefault("planet.climate.season.header", "🍂  Seasonality / Thermal Variance:"));
        seasonSubHeader.getStyleClass().add("control-label");
        VBox seasonSubBlock = new VBox(6, seasonSubHeader, radioSeasonProc, seasonProcPanel, radioSeasonImport, seasonImportPanel);
        seasonSubBlock.setStyle("-fx-padding: 10 10 10 10; -fx-background-color: rgba(245, 158, 11, 0.08); -fx-background-radius: 6; -fx-border-color: rgba(245, 158, 11, 0.25); -fx-border-radius: 6;");

        earthPaleoNoticeLabel = new Label(I18n.getOrDefault("planet.climate.earth_paleo_notice",
                "🌍 Note Paléoclimatique : Pour la Terre, le moteur intègre la dynamique temporelle (glaciations, niveau des mers LGM -120m, Sahara Vert). Les cartes ci-contre présentent la baseline climatologique actuelle (ERA5 / WorldClim)."));
        earthPaleoNoticeLabel.setWrapText(true);
        earthPaleoNoticeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-padding: 6 10; -fx-background-color: rgba(56,189,248,0.08); -fx-background-radius: 5; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 5;");

        VBox climateControls = new VBox(12,
                atmosphericGlobal,
                earthPaleoNoticeLabel,
                new Separator(),
                tempSubBlock,
                new Separator(),
                precipSubBlock,
                new Separator(),
                seasonSubBlock
        );

        climateSecHeader = new Label(I18n.getOrDefault("planet.section.climate", "3. ATMOSPHERE & PLANETARY CLIMATE DOMAIN"));
        VBox climateSection = createSection(climateSecHeader, climateControls);

        controlsBox.getChildren().addAll(
                headerLabel,
                validationWarningBanner,
                presetSection,
                astroSection,
                topoSection,
                climateSection
        );

        controlsBox.setPrefWidth(460);
        controlsBox.setMinWidth(460);

        ScrollPane scrollControls = new ScrollPane(controlsBox);
        scrollControls.setFitToWidth(true);
        scrollControls.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollControls.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollControls.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollControls.setPrefWidth(480);
        scrollControls.setMinWidth(480);
        scrollControls.setMaxWidth(480);

        // --- Center: Map Preview & Generation ---
        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(10));

        previewTitle = new Label(I18n.getOrDefault("planet.preview.title", "Dynamic 2D Equirectangular Planetary Preview"));
        previewTitle.getStyleClass().add("label-header");

        // View Mode Selector for Tab 1
        viewModeCombo = new ComboBox<>();
        viewModeCombo.getItems().addAll(
                I18n.getOrDefault("planet.view.heightmap", "MAP 1. Heightmap & Relief Map"),
                I18n.getOrDefault("planet.view.temperature", "MAP 2. Temperature Map (°C)"),
                I18n.getOrDefault("planet.view.precipitation", "MAP 3. Precipitation Map (mm/yr)"),
                I18n.getOrDefault("planet.view.seasonality", "MAP 4. Seasonality / Thermal Amplitude Map (°C)")
        );
        viewModeCombo.setValue(viewModeCombo.getItems().get(0));
        viewModeCombo.setMaxWidth(420);
        viewModeCombo.setOnAction(e -> {
            updateLegend();
            updatePreview();
        });

        StackPane canvasContainer = new StackPane();
        canvasContainer.setStyle("-fx-background-color: black; -fx-border-color: #475569; -fx-border-radius: 6; -fx-background-radius: 6;");
        previewCanvas = new Canvas(640, 360);
        Tooltip.install(previewCanvas, new Tooltip(I18n.getOrDefault("planet.tooltip.preview", "Dynamic 2D equirectangular preview")));

        // Clip container to prevent JavaFX Prism NGCanvas renderForClip 0x0 NPE
        javafx.scene.shape.Rectangle containerClip = new javafx.scene.shape.Rectangle();
        containerClip.widthProperty().bind(canvasContainer.widthProperty());
        containerClip.heightProperty().bind(canvasContainer.heightProperty());
        canvasContainer.setClip(containerClip);

        canvasContainer.widthProperty().addListener((obs, oldV, newV) -> {
            double w = Math.floor(newV.doubleValue());
            if (w >= 10.0 && Math.abs(w - previewCanvas.getWidth()) >= 4.0) {
                previewCanvas.setWidth(w);
                updatePreview();
            }
        });
        canvasContainer.heightProperty().addListener((obs, oldV, newV) -> {
            double h = Math.floor(newV.doubleValue());
            if (h >= 10.0 && Math.abs(h - previewCanvas.getHeight()) >= 4.0) {
                previewCanvas.setHeight(h);
                updatePreview();
            }
        });

        // Interactive Zoom & Pan Handlers
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
                updatePreview();
            }
        });
        previewCanvas.setOnMousePressed(e -> {
            dragStartX = e.getX();
            dragStartY = e.getY();
        });
        previewCanvas.setOnMouseDragged(e -> {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            panX += dx;
            panY += dy;
            dragStartX = e.getX();
            dragStartY = e.getY();
            updatePreview();
        });
        previewCanvas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                zoomFactor = 1.0;
                panX = 0.0;
                panY = 0.0;
                updatePreview();
            }
        });

        canvasContainer.getChildren().add(previewCanvas);
        VBox.setVgrow(canvasContainer, Priority.ALWAYS);

        legendBar = new HBox(10);
        legendBar.setAlignment(Pos.CENTER);
        legendBar.setPadding(new Insets(5, 10, 5, 10));
        legendBar.getStyleClass().add("card-section");

        statsLabel = new Label();
        statsLabel.getStyleClass().add("label-stats");

        astroLabel = new Label();
        astroLabel.getStyleClass().add("control-label");

        centerBox.getChildren().addAll(previewTitle, viewModeCombo, canvasContainer, legendBar, statsLabel, astroLabel);
        VBox.setVgrow(centerBox, Priority.ALWAYS);

        setLeft(scrollControls);
        setCenter(centerBox);

        // Apply Earth as default preset
        presetCombo.getSelectionModel().select(PlanetPreset.EARTH_LIKE);
        applyPreset(PlanetPreset.EARTH_LIKE);
    }

    private void toggleSatelliteControls() {
        boolean isSat = "satellite".equals(bodyTypeCombo.getValue());
        satelliteRowBox.setVisible(isSat);
        satelliteRowBox.setManaged(isSat);
        updatePreview();
    }

    public static final String CLIMATE_SRC_TEMP_EARTH = "🌍 Terre — WorldClim v2.1 Bio1 & ERA5 (Composite)";
    public static final String CLIMATE_SRC_TEMP_MARS = "🔴 Mars — MGS TES Thermal Radiometry";
    public static final String CLIMATE_SRC_TEMP_VENUS = "🟡 Vénus — Magellan SAR & Hypsometric Model";
    public static final String CLIMATE_SRC_TEMP_MOON = "⚪ Lune — LRO Diviner Thermal Radiometer";
    public static final String CLIMATE_SRC_TEMP_MERCURY = "⚪ Mercure — MESSENGER MLA Extreme Thermal Model";
    public static final String CLIMATE_SRC_TEMP_WMS = "🌐 NASA MERRA-2 (WMS Satellite)";

    public static final String CLIMATE_SRC_PRECIP_EARTH = "🌍 Terre — WorldClim v2.1 & GPCP v2.3 (Composite)";
    public static final String CLIMATE_SRC_PRECIP_MARS = "🔴 Mars — Frost & Sublimation Model";
    public static final String CLIMATE_SRC_PRECIP_VENUS = "🟡 Vénus — H2SO4 Virga Cycle Model";
    public static final String CLIMATE_SRC_PRECIP_MOON = "⚪ Lune — LRO LEND Vacuum Exosphere";
    public static final String CLIMATE_SRC_PRECIP_MERCURY = "⚪ Mercure — MESSENGER Exospheric Vacuum Model";
    public static final String CLIMATE_SRC_PRECIP_WMS = "🌐 NASA GPM IMERG (WMS Satellite)";

    public static final String CLIMATE_SRC_SEASON_EARTH = "🌍 Terre — WorldClim v2.1 Bio4 & ERA5 (Composite)";
    public static final String CLIMATE_SRC_SEASON_MARS = "🔴 Mars — Orbital Eccentricity Insolation Model";
    public static final String CLIMATE_SRC_SEASON_VENUS = "🟡 Vénus — Super-Rotation Low Variance Model";
    public static final String CLIMATE_SRC_SEASON_MOON = "⚪ Lune — Diurnal Insolation Amplitude Model";
    public static final String CLIMATE_SRC_SEASON_MERCURY = "⚪ Mercure — 3:2 Spin-Orbit Thermal Variance Model";
    public static final String CLIMATE_SRC_SEASON_WMS = "🌐 NASA MODIS LST Amplitude (WMS Satellite)";

    /**
     * Builds the climate source combo for a given map type (temp / precip / season).
     * Lists public scientific reference datasets the user can use as import source.
     */
    private ComboBox<String> buildClimateSourceCombo(String mapType) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getItems().add(""); // Empty default option
        switch (mapType) {
            case "temp" -> combo.getItems().addAll(
                    CLIMATE_SRC_TEMP_EARTH,
                    CLIMATE_SRC_TEMP_MARS,
                    CLIMATE_SRC_TEMP_VENUS,
                    CLIMATE_SRC_TEMP_MOON,
                    CLIMATE_SRC_TEMP_MERCURY,
                    CLIMATE_SRC_TEMP_WMS
            );
            case "precip" -> combo.getItems().addAll(
                    CLIMATE_SRC_PRECIP_EARTH,
                    CLIMATE_SRC_PRECIP_MARS,
                    CLIMATE_SRC_PRECIP_VENUS,
                    CLIMATE_SRC_PRECIP_MOON,
                    CLIMATE_SRC_PRECIP_MERCURY,
                    CLIMATE_SRC_PRECIP_WMS
            );
            default -> combo.getItems().addAll(
                    CLIMATE_SRC_SEASON_EARTH,
                    CLIMATE_SRC_SEASON_MARS,
                    CLIMATE_SRC_SEASON_VENUS,
                    CLIMATE_SRC_SEASON_MOON,
                    CLIMATE_SRC_SEASON_MERCURY,
                    CLIMATE_SRC_SEASON_WMS
            );
        }
        combo.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item.isEmpty() ? I18n.getOrDefault("common.combo.prompt_source", "— Select a data source —") : item);
            }
        });
        combo.setButtonCell(combo.getCellFactory().call(null));
        combo.setValue("");
        combo.setOnAction(e -> {
            if (isUpdatingFromPreset) return;
            String val = combo.getValue();
            if (val != null && !val.isEmpty()) {
                if (val.contains("WMS")) {
                    fetchOnlineClimateData();
                } else {
                    applyClimateSourceSelection(mapType, val);
                }
            }
        });
        combo.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.map_source_hint",
                "Sélectionnez la source de données de référence.\nLe bouton '📂 Charger…' ci-dessous permet d'importer votre fichier PNG local.")));
        return combo;
    }

    private void applyClimateSourceSelection(String mapType, String selectedSource) {
        if (selectedSource == null || selectedSource.isBlank() || isUpdatingFromPreset) return;
        String key = selectedSource.toLowerCase();
        String file;
        if (key.contains("mars") || key.contains("tes") || key.contains("mgs")) {
            file = switch (mapType) {
                case "temp" -> "mars_temperature.png";
                case "precip" -> "mars_precipitation.png";
                default -> "mars_seasonality.png";
            };
        } else if (key.contains("vénus") || key.contains("venus") || key.contains("magellan")) {
            file = switch (mapType) {
                case "temp" -> "venus_temperature.png";
                case "precip" -> "venus_precipitation.png";
                default -> "venus_seasonality.png";
            };
        } else if (key.contains("lune") || key.contains("moon") || key.contains("lro") || key.contains("diviner") || key.contains("lend")) {
            file = switch (mapType) {
                case "temp" -> "moon_temperature.png";
                case "precip" -> "moon_precipitation.png";
                default -> "moon_seasonality.png";
            };
        } else if (key.contains("mercure") || key.contains("mercury") || key.contains("messenger")) {
            file = switch (mapType) {
                case "temp" -> "mercury_temperature.png";
                case "precip" -> "mercury_precipitation.png";
                default -> "mercury_seasonality.png";
            };
        } else {
            file = switch (mapType) {
                case "temp" -> "earth_temperature.png";
                case "precip" -> "earth_precipitation.png";
                default -> "earth_seasonality.png";
            };
        }

        Image img = ImageMapLoader.loadMapImage(file);
        switch (mapType) {
            case "temp" -> {
                customClimateImage = img;
                if (radioTempImport != null) radioTempImport.setSelected(true);
                if (climateFileLabel != null) climateFileLabel.setText("🌡️ " + selectedSource);
                if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(1);
            }
            case "precip" -> {
                customRainfallImage = img;
                if (radioPrecipImport != null) radioPrecipImport.setSelected(true);
                if (rainfallFileLabel != null) rainfallFileLabel.setText("🌧️ " + selectedSource);
                if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(2);
            }
            default -> {
                customSeasonalityImage = img;
                if (radioSeasonImport != null) radioSeasonImport.setSelected(true);
                if (seasonalityFileLabel != null) seasonalityFileLabel.setText("☀️ " + selectedSource);
                if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(3);
            }
        }
        if (presetBar != null) presetBar.notifyParametersChanged();
        updatePreview();
    }

    /**
     * Injects (or replaces) a local-file entry in the given climate source combo,
     * then selects it. Keeps all scientific-source entries intact.
     */
    private void setLocalFileInCombo(ComboBox<String> combo, String fileName) {
        String prefix = "📁 Fichier local : ";
        combo.getItems().removeIf(item -> item != null && item.startsWith(prefix));
        String entry = prefix + fileName;
        combo.getItems().add(entry);
        combo.setValue(entry);
    }

    private VBox createSection(Label header, javafx.scene.Node content) {
        header.getStyleClass().add("label-section-header");
        VBox box = new VBox(8, header, content);
        box.getStyleClass().add("card-section");
        return box;
    }

    private Slider createSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        // Guard: during applyPreset(), setValue() fires this listener for every slider.
        // isUpdatingFromPreset prevents ~15 redundant full-canvas redraws per preset change.
        slider.valueProperty().addListener((obs, old, val) -> {
            if (isUpdatingFromPreset) return;
            if (presetBar != null) presetBar.notifyParametersChanged();
            if (!slider.isValueChanging()) {
                updatePreview();
            }
        });
        slider.setOnMouseReleased(e -> {
            if (isUpdatingFromPreset) return;
            if (presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });
        return slider;
    }

    private VBox createControlRow(Label label, javafx.scene.Node control, String tooltipText) {
        label.getStyleClass().add("control-label");
        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip tt = new Tooltip(tooltipText);
            label.setTooltip(tt);
            if (control instanceof Control ctrl) {
                ctrl.setTooltip(tt);
            }
        }
        return new VBox(4, label, control);
    }

    private VBox createControlRow(Label label, Slider slider, String formatPattern, String tooltipText) {
        label.getStyleClass().add("control-label");
        Label valLabel = new Label(String.format(formatPattern, slider.getValue()));
        valLabel.getStyleClass().add("value-label");
        
        slider.valueProperty().addListener((obs, old, val) -> 
            valLabel.setText(String.format(formatPattern, val.doubleValue()))
        );

        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip tt = new Tooltip(tooltipText);
            label.setTooltip(tt);
            slider.setTooltip(tt);
            valLabel.setTooltip(tt);
        }

        HBox header = new HBox(label, new Pane(), valLabel);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        return new VBox(3, header, slider);
    }

    private void chooseElevMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.chooser.elevation", "Load elevation heightmap image"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customElevImage = new Image(new FileInputStream(file));
                elevFileLabel.setText("📷 " + file.getName());
                if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(0);
                if (presetBar != null) presetBar.notifyParametersChanged();
                // Update the source combo to reflect the loaded local file
                String prefix = "📁 Fichier local : ";
                mapSourceCombo.getItems().removeIf(item -> item != null && item.startsWith(prefix));
                String localEntry = prefix + file.getName();
                mapSourceCombo.getItems().add(localEntry);
                mapSourceCombo.setValue(localEntry);
                updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load elevation map image", ex);
            }
        }
    }

    /**
     * Exports the current procedural heightmap to a PNG file.
     * Resolution is automatically recommended based on planet radius.
     */
    private void exportProceduralHeightmap() {
        PlanetPreset preset = buildPresetFromUI();
        double circumference = 2 * Math.PI * preset.radiusKm();
        int recW = circumference > 60000 ? 2048 : (circumference > 20000 ? 1024 : 512);
        int recH = recW / 2;

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.dialog.export_heightmap", "Export Procedural Heightmap (PNG)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        chooser.setInitialFileName(String.format("ether-heightmap-%s-%dx%d.png",
                preset.name().toLowerCase().replaceAll("[^a-z0-9]", "-"), recW, recH));
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file == null) return;

        new Thread(() -> {
            try {
                WritableImage img = new WritableImage(recW, recH);
                PixelWriter pw = img.getPixelWriter();
                for (int y = 0; y < recH; y++) {
                    for (int x = 0; x < recW; x++) {
                        double lon = ((double) x / recW) * 360.0 - 180.0;
                        double lat = 90.0 - ((double) y / recH) * 180.0;
                        ProceduralGenerator.PlanetPoint pt = generator.getPlanetPoint(lat, lon, preset);
                        double norm = Math.max(0, Math.min(1,
                                (pt.elevation() - preset.minAltitudeMeters()) /
                                (preset.maxAltitudeMeters() - preset.minAltitudeMeters())));
                        int v = (int) (norm * 255);
                        pw.setColor(x, y, Color.rgb(v, v, v));
                    }
                }
                // Write PNG via ImageIO
                int w = recW, h = recH;
                java.awt.image.BufferedImage bImg = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_BYTE_GRAY);
                PixelReader pr = img.getPixelReader();
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        bImg.setRGB(x, y, pr.getArgb(x, y));
                    }
                }
                javax.imageio.ImageIO.write(bImg, "png", file);
                javafx.application.Platform.runLater(() -> {
                    if (mapStatusLabel != null)
                        mapStatusLabel.setText(I18n.getOrDefault("planet.status.heightmap_exported", "✅ Heightmap exported: ") + file.getName() + " (" + recW + "×" + recH + " px)");
                });
                logger.info("Exported procedural heightmap to {} ({}x{})", file.getAbsolutePath(), recW, recH);
            } catch (Exception ex) {
                logger.error("Failed to export procedural heightmap", ex);
                javafx.application.Platform.runLater(() -> {
                    if (mapStatusLabel != null)
                        mapStatusLabel.setText(I18n.getOrDefault("planet.status.heightmap_export_error", "❌ Erreur export heightmap : ") + ex.getMessage());
                });
            }
        }).start();
    }

    private void exportProceduralClimateMap(String type) {
        PlanetPreset preset = buildPresetFromUI();
        double circumference = 2 * Math.PI * preset.radiusKm();
        int recW = circumference > 60000 ? 2048 : (circumference > 20000 ? 1024 : 512);
        int recH = recW / 2;

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.chooser.export", "Export map ") + type + " (PNG)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        chooser.setInitialFileName(String.format("ether-%s-%s-%dx%d.png",
                type, preset.name().toLowerCase().replaceAll("[^a-z0-9]", "-"), recW, recH));
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file == null) return;

        new Thread(() -> {
            try {
                WritableImage img = new WritableImage(recW, recH);
                PixelWriter pw = img.getPixelWriter();
                for (int y = 0; y < recH; y++) {
                    for (int x = 0; x < recW; x++) {
                        double lon = ((double) x / recW) * 360.0 - 180.0;
                        double lat = 90.0 - ((double) y / recH) * 180.0;
                        ProceduralGenerator.PlanetPoint pt = generator.getPlanetPoint(lat, lon, preset);
                        double norm = 0.5;
                        if ("temp".equalsIgnoreCase(type)) {
                            norm = Math.max(0, Math.min(1, (pt.temperature() + 50.0) / 100.0));
                        } else if ("precip".equalsIgnoreCase(type)) {
                            norm = Math.max(0, Math.min(1, pt.rainfall()));
                        } else {
                            norm = Math.max(0, Math.min(1, (Math.abs(lat) / 90.0) * (preset.axialTiltDegrees() / 45.0)));
                        }
                        int v = (int) (norm * 255);
                        pw.setColor(x, y, Color.rgb(v, v, v));
                    }
                }
                java.awt.image.BufferedImage bImg = new java.awt.image.BufferedImage(recW, recH, java.awt.image.BufferedImage.TYPE_BYTE_GRAY);
                PixelReader pr = img.getPixelReader();
                for (int y = 0; y < recH; y++) {
                    for (int x = 0; x < recW; x++) {
                        bImg.setRGB(x, y, pr.getArgb(x, y));
                    }
                }
                javax.imageio.ImageIO.write(bImg, "png", file);
                javafx.application.Platform.runLater(() -> {
                    if (mapStatusLabel != null)
                        mapStatusLabel.setText(I18n.getOrDefault("planet.status.map_exported_prefix", "✅ Map ") + type + I18n.getOrDefault("planet.status.map_exported_middle", " exported: ") + file.getName());
                });
            } catch (Exception ex) {
                logger.error("Failed to export procedural climate map " + type, ex);
            }
        }).start();
    }

    private void chooseBiomeMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.chooser.biome", "Load biome / ecology map image"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customBiomeImage = new Image(new FileInputStream(file));
                biomeFileLabel.setText("🌿 " + file.getName());
                updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load biome map image", ex);
            }
        }
    }

    private void chooseResourceMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.chooser.geology", "Load geological / ore map image"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customResourceImage = new Image(new FileInputStream(file));
                resourceFileLabel.setText("🪨 " + file.getName());
                updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load resource map image", ex);
            }
        }
    }

    private void chooseClimateMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.chooser.climate", "Load climate / temperature map image"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customClimateImage = new Image(new FileInputStream(file));
                climateFileLabel.setText("🌡️ " + file.getName());
                setLocalFileInCombo(tempSourceCombo, file.getName());
                if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(1);
                updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load climate map image", ex);
            }
        }
    }

    private void chooseRainfallMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.chooser.rainfall", "Load rainfall map image"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customRainfallImage = new Image(new FileInputStream(file));
                rainfallFileLabel.setText("🌧️ " + file.getName());
                setLocalFileInCombo(precipSourceCombo, file.getName());
                if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(2);
                updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load rainfall map image", ex);
            }
        }
    }

    private void chooseSeasonalityMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.chooser.seasonality", "Load seasonality map image"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customSeasonalityImage = new Image(new FileInputStream(file));
                seasonalityFileLabel.setText("🍂 " + file.getName());
                setLocalFileInCombo(seasonSourceCombo, file.getName());
                if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(3);
                updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load seasonality map image", ex);
            }
        }
    }

    private void showClimateImportFormatHelp() {
        WindowUtils.showScrollableInfoDialog(
                I18n.getOrDefault("planet.dialog.climate_title", "Spécifications des Cartes de Relief & Climat"),
                I18n.getOrDefault("planet.dialog.climate_header", "Formats d'Image Supportés (Projection Équirectangulaire 2:1)"),
                "Vous pouvez importer des cartes d'élévation et de climat sous forme d'images PNG/JPEG au ratio 2:1 (ex: 2048x1024 pixels, Plate Carrée) :\n\n" +
                "1. CARTES D'ÉLÉVATION / RELIEF (FAUSSES COULEURS HYPSOMÉTRIQUES & BATHYMÉTRIQUES) :\n" +
                "   • Dépressions / Fosses océaniques : Bleu nuit / Bleu roi\n" +
                "   • Plaines / Basses terres : Vert émeraude / Vert clair\n" +
                "   • Collines / Plateaux : Ocre / Jaune / Orange\n" +
                "   • Hautes Montagnes & Volcans : Brun / Rouge foncé\n" +
                "   • Plus hauts sommets : Blanc neigeux\n" +
                "   • Mode Niveaux de Gris : Noir (0) = Altitude minimale, Blanc (255) = Altitude maximale.\n\n" +
                "2. CARTES CLIMATIQUES SÉPARÉES :\n" +
                "   • Températures : Noir / Bleu froid = -50°C, Blanc / Rouge chaud = +50°C\n" +
                "   • Précipitations : Noir = 0 mm/an, Blanc = 3000 mm/an\n" +
                "   • Saisonnalité : Noir = 0°C, Blanc = 50°C d'amplitude annuelle\n\n" +
                "3. CARTE COMBINÉE MULTI-CANAUX RGB :\n" +
                "   • Canal Rouge (R) = Température (-50°C à +50°C)\n" +
                "   • Canal Vert (V) = Précipitations (0 à 3000 mm/an)\n" +
                "   • Canal Bleu (B) = Saisonnalité / Amplitude (0 à 50°C)\n\n" +
                "4. TÉLÉCHARGEMENT SATELLITE EN LIGNE (WMS) :\n" +
                "   • Le bouton '🌐 Télécharger Climat Satellite' permet de récupérer directement les flux officiels MODIS et GPM de la NASA !"
        );
    }

    private void loadEarthPresetMaps() {
        try {
            customElevImage        = ImageMapLoader.loadMapImage("earth_elevation.png");
            customBiomeImage       = ImageMapLoader.loadMapImage("earth_biomes.png");
            customClimateImage     = ImageMapLoader.loadMapImage("earth_temperature.png");
            customRainfallImage    = ImageMapLoader.loadMapImage("earth_precipitation.png");
            customSeasonalityImage = ImageMapLoader.loadMapImage("earth_seasonality.png");

            if (elevFileLabel != null) elevFileLabel.setText(I18n.getOrDefault("planet.status.earth_dem_active", "📷 Active preset: Earth Elevation (NOAA/NASA DEM)"));
            if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.status.earth_biome_active", "🌿 Active preset: Earth Biomes & Land Cover (MODIS)"));
            if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("planet.status.no_file_proc", "📄 No file loaded (Procedural active)"));
            if (climateFileLabel != null) climateFileLabel.setText("🌡️ " + CLIMATE_SRC_TEMP_EARTH);
            if (rainfallFileLabel != null) rainfallFileLabel.setText("🌧️ " + CLIMATE_SRC_PRECIP_EARTH);
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText("☀️ " + CLIMATE_SRC_SEASON_EARTH);
        } catch (Exception e) {
            logger.warn("Could not load internal Earth maps", e);
        }
    }

    private void clearCustomMaps() {
        customElevImage = null;
        customBiomeImage = null;
        customResourceImage = null;
        customClimateImage = null;
        customRainfallImage = null;
        customSeasonalityImage = null;
        cachedElevB64 = null;
        cachedBiomeB64 = null;
        cachedResourceB64 = null;
        cachedClimateB64 = null;
        cachedRainfallB64 = null;
        cachedSeasonalityB64 = null;
        String procDefault = I18n.getOrDefault("planet.status.no_file_proc", "📄 No external file (Procedural generation active)");
        if (elevFileLabel != null) elevFileLabel.setText(procDefault);
        if (biomeFileLabel != null) biomeFileLabel.setText(procDefault);
        if (resourceFileLabel != null) resourceFileLabel.setText(procDefault);
        if (climateFileLabel != null) climateFileLabel.setText(procDefault);
        if (rainfallFileLabel != null) rainfallFileLabel.setText(procDefault);
        if (seasonalityFileLabel != null) seasonalityFileLabel.setText(procDefault);
    }

    private void applyMapSourcePreset(String sourceKey) {
        boolean oldUpdating = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            if ("none".equals(sourceKey)) {
                clearCustomMaps();
                if (radioProc != null) radioProc.setSelected(true);
                if (radioTempProc != null) radioTempProc.setSelected(true);
                if (radioPrecipProc != null) radioPrecipProc.setSelected(true);
                if (radioSeasonProc != null) radioSeasonProc.setSelected(true);
                return;
            }

            if ("earth".equals(sourceKey)) {
                loadEarthPresetMaps();
                if (radioImport != null) radioImport.setSelected(true);
                if (radioTempImport != null) radioTempImport.setSelected(true);
                if (radioPrecipImport != null) radioPrecipImport.setSelected(true);
                if (radioSeasonImport != null) radioSeasonImport.setSelected(true);

                if (tempSourceCombo != null) {
                    tempSourceCombo.setValue(CLIMATE_SRC_TEMP_EARTH);
                }
                if (precipSourceCombo != null) {
                    precipSourceCombo.setValue(CLIMATE_SRC_PRECIP_EARTH);
                }
                if (seasonSourceCombo != null) {
                    seasonSourceCombo.setValue(CLIMATE_SRC_SEASON_EARTH);
                }

                if (!oldUpdating) {
                    applyPreset(PlanetPreset.EARTH_LIKE);
                }
                return;
            }

            if ("mars".equals(sourceKey)) {
                clearCustomMaps();
                bodyTypeCombo.setValue("planet");
                radiusSlider.setValue(3389.5);
                dayLengthSlider.setValue(24.6);
                axialTiltSlider.setValue(25.2);
                yearLengthSlider.setValue(687);
                distanceSunSlider.setValue(1.52);
                solarLumSlider.setValue(1.0);
                avgTempSlider.setValue(-60);
                minAltSlider.setValue(-8000);
                maxAltSlider.setValue(21229); // Olympus Mons
                waterSlider.setValue(-0.4);

                customElevImage = ImageMapLoader.loadMapImage("mars_elevation.png");
                customBiomeImage = ImageMapLoader.loadMapImage("mars_biomes.png");
                customClimateImage = ImageMapLoader.loadMapImage("mars_temperature.png");
                customRainfallImage = ImageMapLoader.loadMapImage("mars_precipitation.png");
                customSeasonalityImage = ImageMapLoader.loadMapImage("mars_seasonality.png");

                if (radioImport != null) radioImport.setSelected(true);
                if (radioTempImport != null) radioTempImport.setSelected(true);
                if (radioPrecipImport != null) radioPrecipImport.setSelected(true);
                if (radioSeasonImport != null) radioSeasonImport.setSelected(true);

                if (tempSourceCombo != null) tempSourceCombo.setValue(CLIMATE_SRC_TEMP_MARS);
                if (precipSourceCombo != null) precipSourceCombo.setValue(CLIMATE_SRC_PRECIP_MARS);
                if (seasonSourceCombo != null) seasonSourceCombo.setValue(CLIMATE_SRC_SEASON_MARS);

                if (elevFileLabel != null) elevFileLabel.setText(I18n.getOrDefault("planet.source.mars_dem", "📷 Mars MOLA Topography (NASA PDS)"));
                if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.source.mars_biome", "🌿 Mars Planetary Terrains & Volcanic Plains"));
                if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("resource.source.mars_geology", "🪨 Mars Multi-mineral Deposits"));
                if (climateFileLabel != null) climateFileLabel.setText("🌡️ " + CLIMATE_SRC_TEMP_MARS);
                if (rainfallFileLabel != null) rainfallFileLabel.setText("🌧️ " + CLIMATE_SRC_PRECIP_MARS);
                if (seasonalityFileLabel != null) seasonalityFileLabel.setText("☀️ " + CLIMATE_SRC_SEASON_MARS);
                return;
            }

            if ("venus".equals(sourceKey)) {
                clearCustomMaps();
                bodyTypeCombo.setValue("planet");
                radiusSlider.setValue(6051.8);
                dayLengthSlider.setValue(2802);
                axialTiltSlider.setValue(177.3);
                yearLengthSlider.setValue(225);
                distanceSunSlider.setValue(0.72);
                solarLumSlider.setValue(1.0);
                avgTempSlider.setValue(464);
                minAltSlider.setValue(-3000);
                maxAltSlider.setValue(11000); // Maxwell Montes
                waterSlider.setValue(-0.5);

                customElevImage = ImageMapLoader.loadMapImage("venus_elevation.png");
                customBiomeImage = ImageMapLoader.loadMapImage("venus_biomes.png");
                customClimateImage = ImageMapLoader.loadMapImage("venus_temperature.png");
                customRainfallImage = ImageMapLoader.loadMapImage("venus_precipitation.png");
                customSeasonalityImage = ImageMapLoader.loadMapImage("venus_seasonality.png");

                if (radioImport != null) radioImport.setSelected(true);
                if (radioTempImport != null) radioTempImport.setSelected(true);
                if (radioPrecipImport != null) radioPrecipImport.setSelected(true);
                if (radioSeasonImport != null) radioSeasonImport.setSelected(true);

                if (tempSourceCombo != null) tempSourceCombo.setValue(CLIMATE_SRC_TEMP_VENUS);
                if (precipSourceCombo != null) precipSourceCombo.setValue(CLIMATE_SRC_PRECIP_VENUS);
                if (seasonSourceCombo != null) seasonSourceCombo.setValue(CLIMATE_SRC_SEASON_VENUS);

                if (elevFileLabel != null) elevFileLabel.setText(I18n.getOrDefault("planet.source.venus_dem", "📷 Venus Magellan Topography (NASA PDS)"));
                if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.source.venus_biome", "🌿 Venus Volcanic Plains & Tesserae"));
                if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("resource.source.venus_geology", "🪨 Venus Magellan Pyrite / Radar Minerals"));
                if (climateFileLabel != null) climateFileLabel.setText("🌡️ " + CLIMATE_SRC_TEMP_VENUS);
                if (rainfallFileLabel != null) rainfallFileLabel.setText("🌧️ " + CLIMATE_SRC_PRECIP_VENUS);
                if (seasonalityFileLabel != null) seasonalityFileLabel.setText("☀️ " + CLIMATE_SRC_SEASON_VENUS);
                return;
            }

            if ("moon".equals(sourceKey)) {
                clearCustomMaps();
                bodyTypeCombo.setValue("satellite");
                radiusSlider.setValue(1737.4);
                dayLengthSlider.setValue(708);
                axialTiltSlider.setValue(1.5);
                yearLengthSlider.setValue(365);
                distanceSunSlider.setValue(1.0);
                solarLumSlider.setValue(1.0);
                avgTempSlider.setValue(-20);
                minAltSlider.setValue(-9000);
                maxAltSlider.setValue(10700);
                waterSlider.setValue(-0.5);

                customElevImage = ImageMapLoader.loadMapImage("moon_elevation.png");
                customBiomeImage = ImageMapLoader.loadMapImage("moon_biomes.png");
                customClimateImage = ImageMapLoader.loadMapImage("moon_temperature.png");
                customRainfallImage = ImageMapLoader.loadMapImage("moon_precipitation.png");
                customSeasonalityImage = ImageMapLoader.loadMapImage("moon_seasonality.png");

                if (radioImport != null) radioImport.setSelected(true);
                if (radioTempImport != null) radioTempImport.setSelected(true);
                if (radioPrecipImport != null) radioPrecipImport.setSelected(true);
                if (radioSeasonImport != null) radioSeasonImport.setSelected(true);

                if (tempSourceCombo != null) tempSourceCombo.setValue(CLIMATE_SRC_TEMP_MOON);
                if (precipSourceCombo != null) precipSourceCombo.setValue(CLIMATE_SRC_PRECIP_MOON);
                if (seasonSourceCombo != null) seasonSourceCombo.setValue(CLIMATE_SRC_SEASON_MOON);

                if (elevFileLabel != null) elevFileLabel.setText(I18n.getOrDefault("planet.source.moon_dem", "📷 Moon LRO LOLA Topography (NASA PDS)"));
                if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.source.moon_biome", "🌿 Moon Lunar Maria & Anorthosite Highlands"));
                if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("resource.source.moon_geology", "🪨 Moon KREEP & Ilmenite Deposits"));
                if (climateFileLabel != null) climateFileLabel.setText("🌡️ " + CLIMATE_SRC_TEMP_MOON);
                if (rainfallFileLabel != null) rainfallFileLabel.setText("🌧️ " + CLIMATE_SRC_PRECIP_MOON);
                if (seasonalityFileLabel != null) seasonalityFileLabel.setText("☀️ " + CLIMATE_SRC_SEASON_MOON);
                return;
            }

            if ("mercury".equals(sourceKey)) {
                clearCustomMaps();
                bodyTypeCombo.setValue("planet");
                radiusSlider.setValue(2439.7);
                dayLengthSlider.setValue(4222.6);
                axialTiltSlider.setValue(0.034);
                yearLengthSlider.setValue(87.97);
                distanceSunSlider.setValue(0.387);
                solarLumSlider.setValue(1.0);
                avgTempSlider.setValue(167);
                minAltSlider.setValue(-5000);
                maxAltSlider.setValue(4480);
                waterSlider.setValue(-0.5);

                customElevImage = ImageMapLoader.loadMapImage("mercury_elevation.png");
                customBiomeImage = ImageMapLoader.loadMapImage("mercury_biomes.png");
                customClimateImage = ImageMapLoader.loadMapImage("mercury_temperature.png");
                customRainfallImage = ImageMapLoader.loadMapImage("mercury_precipitation.png");
                customSeasonalityImage = ImageMapLoader.loadMapImage("mercury_seasonality.png");

                if (radioImport != null) radioImport.setSelected(true);
                if (radioTempImport != null) radioTempImport.setSelected(true);
                if (radioPrecipImport != null) radioPrecipImport.setSelected(true);
                if (radioSeasonImport != null) radioSeasonImport.setSelected(true);

                if (tempSourceCombo != null) tempSourceCombo.setValue(CLIMATE_SRC_TEMP_MERCURY);
                if (precipSourceCombo != null) precipSourceCombo.setValue(CLIMATE_SRC_PRECIP_MERCURY);
                if (seasonSourceCombo != null) seasonSourceCombo.setValue(CLIMATE_SRC_SEASON_MERCURY);

                if (elevFileLabel != null) elevFileLabel.setText(I18n.getOrDefault("planet.source.mercury_dem", "📷 Mercury MESSENGER MLA DEM (NASA PDS)"));
                if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.source.mercury_biome", "🌿 Mercury Smooth & Intercrater Plains"));
                if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("resource.source.mercury_geology", "🪨 Mercury High-Iron & PSR Ice Deposits"));
                if (climateFileLabel != null) climateFileLabel.setText("🌡️ " + CLIMATE_SRC_TEMP_MERCURY);
                if (rainfallFileLabel != null) rainfallFileLabel.setText("🌧️ " + CLIMATE_SRC_PRECIP_MERCURY);
                if (seasonalityFileLabel != null) seasonalityFileLabel.setText("☀️ " + CLIMATE_SRC_SEASON_MERCURY);
                return;
            }
        } finally {
            isUpdatingFromPreset = oldUpdating;
        }
        if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(0);
        updatePreview();
    }

    private void fetchOnlineSatelliteData() {
        String sourceKey = mapSourceCombo.getValue();
        OnlineMapService.CelestialBody body = switch (sourceKey) {
            case "mars" -> OnlineMapService.CelestialBody.MARS;
            case "moon" -> OnlineMapService.CelestialBody.MOON;
            case "venus" -> OnlineMapService.CelestialBody.VENUS;
            default -> OnlineMapService.CelestialBody.EARTH;
        };

        if (mapStatusLabel != null) mapStatusLabel.setText(I18n.get("planet.map.status_fetching"));

        onlineMapService.fetchElevationMapAsync(body).thenAccept(img -> {
            javafx.application.Platform.runLater(() -> {
                if (img != null) {
                    customElevImage = img;
                    if (elevFileLabel != null) elevFileLabel.setText("🌐 " + body.getName() + " WMS Elevation");
                    if (mapStatusLabel != null) mapStatusLabel.setText(I18n.get("planet.map.status_success"));
                    updatePreview();
                } else {
                    if (mapStatusLabel != null) mapStatusLabel.setText(I18n.get("planet.map.status_error"));
                }
            });
        });

        if (body.getBiomeWmsUrl() != null) {
            onlineMapService.fetchBiomeMapAsync(body).thenAccept(img -> {
                javafx.application.Platform.runLater(() -> {
                    if (img != null) {
                        customBiomeImage = img;
                        if (biomeFileLabel != null) biomeFileLabel.setText("🌐 " + body.getName() + " WMS Biomes");
                        updatePreview();
                    }
                });
            });
        }
    }

    private void fetchOnlineClimateData() {
        String sourceKey = mapSourceCombo.getValue();
        OnlineMapService.CelestialBody body = switch (sourceKey) {
            case "mars" -> OnlineMapService.CelestialBody.MARS;
            case "moon" -> OnlineMapService.CelestialBody.MOON;
            case "venus" -> OnlineMapService.CelestialBody.VENUS;
            default -> OnlineMapService.CelestialBody.EARTH;
        };

        if (mapStatusLabel != null) mapStatusLabel.setText(I18n.getOrDefault("planet.status.downloading_wms", "🌐 Downloading WMS climate maps (NASA MODIS & GPM)..."));

        onlineMapService.fetchClimateMapAsync(body).thenAccept(img -> {
            javafx.application.Platform.runLater(() -> {
                if (img != null) {
                    customClimateImage = img;
                    if (climateFileLabel != null) climateFileLabel.setText("🌐 " + body.getName() + " MODIS Thermal WMS");
                    if (mapStatusLabel != null) mapStatusLabel.setText(I18n.get("planet.map.status_success"));
                    updatePreview();
                }
            });
        });

        onlineMapService.fetchRainfallMapAsync(body).thenAccept(img -> {
            javafx.application.Platform.runLater(() -> {
                if (img != null) {
                    customRainfallImage = img;
                    if (rainfallFileLabel != null) rainfallFileLabel.setText("🌐 " + body.getName() + " GPM Rainfall WMS");
                    updatePreview();
                }
            });
        });
    }

    private void exportMapsWithWorldFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.chooser.worldfile", "Export map with ESRI World File (.tfw)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Map Image", "*.png"));
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            Image imgToExport = customElevImage;
            if (imgToExport == null) {
                WritableImage wimg = new WritableImage((int) previewCanvas.getWidth(), (int) previewCanvas.getHeight());
                previewCanvas.snapshot(null, wimg);
                imgToExport = wimg;
            }
            mapLoader.exportMapToPngAndWorldFile(imgToExport, file);
        }
    }

    private void calculateStellarIrradiance() {
        double d = distanceSunSlider.getValue();
        double l = solarLumSlider.getValue();
        double irradiance = 1361.0 * (l / (d * d));
        irradianceLabel.setText(String.format("%s: %.1f W/m²", I18n.get("planet.param.irradiance"), irradiance));

        // Adjust average surface temperature dynamically based on stellar flux & atmospheric parameters
        if (!isUpdatingFromPreset) {
            double alb = albedoSlider != null ? albedoSlider.getValue() : 0.30;
            double atmoP = atmoPressureSlider != null ? atmoPressureSlider.getValue() : 1.0;
            double co2Ppm = co2Slider != null ? co2Slider.getValue() : 420.0;

            // Blackbody equilibrium temperature (K -> °C): T_eq = 278.5 * [(L * (1 - albedo)) / d^2]^(1/4) - 273.15
            double tEquilibriumC = 278.5 * Math.pow((l * (1.0 - alb)) / (d * d), 0.25) - 273.15;

            // Greenhouse boost: airless bodies (p < 0.01 atm) have zero greenhouse warming
            double greenhouseBoost = 0.0;
            if (atmoP >= 0.01) {
                double co2PartialPressureAtm = atmoP * (co2Ppm / 1_000_000.0);
                double co2Forcing = 3.0 * (Math.log(Math.max(0.0001, co2PartialPressureAtm / 0.00028)) / Math.log(2.0));
                double pressureTerm = (atmoP > 1.0) ? 8.0 * Math.log10(atmoP) : (atmoP - 1.0) * 8.0;
                greenhouseBoost = 33.0 * Math.sqrt(Math.min(2.0, atmoP)) + co2Forcing + pressureTerm;
            }

            double calcTempC = tEquilibriumC + greenhouseBoost;
            calcTempC = Math.max(-250.0, Math.min(600.0, calcTempC));
            avgTempSlider.setValue(calcTempC);
        }
    }

    private void updateAltRangeDisplay() {
        double min = minAltSlider.getValue();
        double max = maxAltSlider.getValue();
        altRangeLabel.setText(String.format("%s: %.0f m", I18n.get("planet.param.alt_range"), max - min));
    }

    public void applyPreset(PlanetPreset p) {
        if (p == null) return;
        WindowUtils.setBusyCursor(this, true);
        try {
            isUpdatingFromPreset = true;

            bodyTypeCombo.setValue(p.isSatellite() ? "satellite" : "planet");
            toggleSatelliteControls();
        parentMassSlider.setValue(p.parentPlanetMassEarthMasses());
        orbitDistanceParentSlider.setValue(p.orbitalDistanceToParentKm());

        seedField.setText(String.valueOf(p.seed()));
        radiusSlider.setValue(p.radiusKm());
        dayLengthSlider.setValue(p.dayLengthHours());
        axialTiltSlider.setValue(p.axialTiltDegrees());
        yearLengthSlider.setValue(p.yearLengthDays());
        distanceSunSlider.setValue(p.distanceToSunAU());
        solarLumSlider.setValue(p.solarLuminosity());
        minAltSlider.setValue(p.minAltitudeMeters());
        maxAltSlider.setValue(p.maxAltitudeMeters());
        avgTempSlider.setValue(p.averageTempC());
        resolutionCombo.setValue(p.resolution());
        noiseFreqSlider.setValue(p.noiseFrequency());
        noiseScaleSlider.setValue(p.noiseScale());
        waterSlider.setValue(p.waterLevel());
        tempGradSlider.setValue(p.temperatureGradient());
        oxygenSlider.setValue(p.oxygenPercentage());
        co2Slider.setValue(p.co2Ppm());
        albedoSlider.setValue(p.albedo());
        atmoPressureSlider.setValue(p.atmospherePressureAtm());

        calculateStellarIrradiance();
        updateAltRangeDisplay();

        // Restore custom map images if saved in JSON preset Base64 strings
        if (p.customElevBase64() != null) {
            customElevImage = org.ether.society.data.ImageMapLoader.base64PngToImage(p.customElevBase64());
            cachedElevB64 = p.customElevBase64();
            if (elevFileLabel != null) elevFileLabel.setText(I18n.getOrDefault("planet.preset.heightmap", "📷 Preset Heightmap"));
        }
        if (p.customBiomeBase64() != null) {
            customBiomeImage = org.ether.society.data.ImageMapLoader.base64PngToImage(p.customBiomeBase64());
            cachedBiomeB64 = p.customBiomeBase64();
            if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.preset.biomes", "🌿 Preset Biomes"));
        }
        if (p.customResourceBase64() != null) {
            customResourceImage = org.ether.society.data.ImageMapLoader.base64PngToImage(p.customResourceBase64());
            cachedResourceB64 = p.customResourceBase64();
            if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("planet.preset.resources", "🪨 Preset Resources"));
        }
        if (p.customClimateBase64() != null) {
            customClimateImage = org.ether.society.data.ImageMapLoader.base64PngToImage(p.customClimateBase64());
            cachedClimateB64 = p.customClimateBase64();
            if (climateFileLabel != null) climateFileLabel.setText(I18n.getOrDefault("planet.preset.climate", "🌡️ Preset Climate"));
        }
        if (p.customRainfallBase64() != null) {
            customRainfallImage = org.ether.society.data.ImageMapLoader.base64PngToImage(p.customRainfallBase64());
            cachedRainfallB64 = p.customRainfallBase64();
            if (rainfallFileLabel != null) rainfallFileLabel.setText(I18n.getOrDefault("planet.preset.rainfall", "🌧️ Preset Rainfall"));
        }
        if (p.customSeasonalityBase64() != null) {
            customSeasonalityImage = org.ether.society.data.ImageMapLoader.base64PngToImage(p.customSeasonalityBase64());
            cachedSeasonalityB64 = p.customSeasonalityBase64();
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText(I18n.getOrDefault("planet.preset.seasonality", "☀️ Preset Seasonality"));
        }

        String lowerName = p.name() != null ? p.name().toLowerCase() : "";
        boolean isSuperEarth = lowerName.contains("super-terre") || lowerName.contains("super-earth") || lowerName.contains("gaia");
        String elevSrc = p.elevationMapSource();
        if (elevSrc == null || elevSrc.isEmpty()) {
            if (p.elevationUseImport()) {
                if (!isSuperEarth && (p == PlanetPreset.EARTH_LIKE || lowerName.contains("terre") || lowerName.contains("terran") || lowerName.contains("earth"))) {
                    elevSrc = "earth";
                } else if (lowerName.contains("mars") || lowerName.contains("ares")) {
                    elevSrc = "mars";
                } else if (lowerName.contains("vénus") || lowerName.contains("venus") || lowerName.contains("hesperos")) {
                    elevSrc = "venus";
                } else if (lowerName.contains("lune") || lowerName.contains("moon") || lowerName.contains("selene")) {
                    elevSrc = "moon";
                } else if (lowerName.contains("mercure") || lowerName.contains("mercury") || lowerName.contains("hermes")) {
                    elevSrc = "mercury";
                } else {
                    elevSrc = "none";
                }
            } else {
                elevSrc = "none";
            }
        }
        if (mapSourceCombo != null) {
            mapSourceCombo.setValue(elevSrc);
        }

        if (p.elevationUseImport() || p.customElevBase64() != null) {
            if ("earth".equals(elevSrc) || "mars".equals(elevSrc) || "venus".equals(elevSrc) || "moon".equals(elevSrc) || "mercury".equals(elevSrc)) {
                if (p.customElevBase64() == null) applyMapSourcePreset(elevSrc);
            }
            radioImport.setSelected(true);
        } else {
            if (p.customElevBase64() == null) clearCustomMaps();
            radioProc.setSelected(true);
        }

        // Temperature sub-block
        if (tempSourceCombo != null) {
            String tSrc = p.tempSource();
            if (tSrc != null && !tSrc.isEmpty()) {
                if (!tempSourceCombo.getItems().contains(tSrc)) {
                    tempSourceCombo.getItems().add(tSrc);
                }
                tempSourceCombo.setValue(tSrc);
            } else if (p.customClimateBase64() != null) {
                setLocalFileInCombo(tempSourceCombo, "Preset Climate Map");
            } else if (!tempSourceCombo.getItems().isEmpty()) {
                tempSourceCombo.setValue(tempSourceCombo.getItems().get(0));
            }
        }
        if (p.tempUseImport() || p.customClimateBase64() != null) {
            radioTempImport.setSelected(true);
        } else {
            radioTempProc.setSelected(true);
        }

        // Precipitation sub-block
        if (precipSourceCombo != null) {
            String prSrc = p.precipSource();
            if (prSrc != null && !prSrc.isEmpty()) {
                if (!precipSourceCombo.getItems().contains(prSrc)) {
                    precipSourceCombo.getItems().add(prSrc);
                }
                precipSourceCombo.setValue(prSrc);
            } else if (p.customRainfallBase64() != null) {
                setLocalFileInCombo(precipSourceCombo, "Preset Rainfall Map");
            } else if (!precipSourceCombo.getItems().isEmpty()) {
                precipSourceCombo.setValue(precipSourceCombo.getItems().get(0));
            }
        }
        if (p.precipUseImport() || p.customRainfallBase64() != null) {
            radioPrecipImport.setSelected(true);
        } else {
            radioPrecipProc.setSelected(true);
        }

        // Seasonality sub-block
        if (seasonSourceCombo != null) {
            String sSrc = p.seasonSource();
            if (sSrc != null && !sSrc.isEmpty()) {
                if (!seasonSourceCombo.getItems().contains(sSrc)) {
                    seasonSourceCombo.getItems().add(sSrc);
                }
                seasonSourceCombo.setValue(sSrc);
            } else if (p.customSeasonalityBase64() != null) {
                setLocalFileInCombo(seasonSourceCombo, "Preset Seasonality Map");
            } else if (!seasonSourceCombo.getItems().isEmpty()) {
                seasonSourceCombo.setValue(seasonSourceCombo.getItems().get(0));
            }
        }
        if (p.seasonUseImport() || p.customSeasonalityBase64() != null) {
            radioSeasonImport.setSelected(true);
        } else {
            radioSeasonProc.setSelected(true);
        }

        if (viewModeCombo != null) {
            viewModeCombo.getSelectionModel().select(0);
        }
        if (presetBar != null) {
            presetBar.markClean(p);
        }
        isUpdatingFromPreset = false;
        updatePreview();
        } finally {
            WindowUtils.setBusyCursor(this, false);
        }
    }

    public PlanetPreset buildPresetFromUI() {
        long seed = 12345;
        try {
            seed = Long.parseLong(seedField.getText());
        } catch (NumberFormatException ignored) {}

        long tempSeed = seed + 100L;
        try {
            if (tempSeedField != null && tempSeedField.getText() != null && !tempSeedField.getText().isEmpty()) {
                tempSeed = Long.parseLong(tempSeedField.getText());
            }
        } catch (NumberFormatException ignored) {}

        long precipSeed = seed + 1000L;
        try {
            if (precipSeedField != null && precipSeedField.getText() != null && !precipSeedField.getText().isEmpty()) {
                precipSeed = Long.parseLong(precipSeedField.getText());
            }
        } catch (NumberFormatException ignored) {}

        long seasonSeed = seed + 2000L;
        try {
            if (seasonSeedField != null && seasonSeedField.getText() != null && !seasonSeedField.getText().isEmpty()) {
                seasonSeed = Long.parseLong(seasonSeedField.getText());
            }
        } catch (NumberFormatException ignored) {}

        String presetName = presetCombo.getValue() != null ? presetCombo.getValue().name() : "Custom Planet";
        boolean isSat = "satellite".equals(bodyTypeCombo.getValue());

        if (customElevImage != null && cachedElevB64 == null) cachedElevB64 = org.ether.society.data.ImageMapLoader.imageToBase64Png(customElevImage);
        if (customBiomeImage != null && cachedBiomeB64 == null) cachedBiomeB64 = org.ether.society.data.ImageMapLoader.imageToBase64Png(customBiomeImage);
        if (customResourceImage != null && cachedResourceB64 == null) cachedResourceB64 = org.ether.society.data.ImageMapLoader.imageToBase64Png(customResourceImage);
        if (customClimateImage != null && cachedClimateB64 == null) cachedClimateB64 = org.ether.society.data.ImageMapLoader.imageToBase64Png(customClimateImage);
        if (customRainfallImage != null && cachedRainfallB64 == null) cachedRainfallB64 = org.ether.society.data.ImageMapLoader.imageToBase64Png(customRainfallImage);
        if (customSeasonalityImage != null && cachedSeasonalityB64 == null) cachedSeasonalityB64 = org.ether.society.data.ImageMapLoader.imageToBase64Png(customSeasonalityImage);

        boolean elevImport = radioImport != null && radioImport.isSelected();
        String elevSrc = mapSourceCombo != null && mapSourceCombo.getValue() != null ? mapSourceCombo.getValue() : "none";

        boolean tempImport = radioTempImport != null && radioTempImport.isSelected();
        String tempSrc = tempSourceCombo != null && tempSourceCombo.getValue() != null ? tempSourceCombo.getValue() : "";

        boolean precipImport = radioPrecipImport != null && radioPrecipImport.isSelected();
        String precipSrc = precipSourceCombo != null && precipSourceCombo.getValue() != null ? precipSourceCombo.getValue() : "";

        boolean seasonImport = radioSeasonImport != null && radioSeasonImport.isSelected();
        String seasonSrc = seasonSourceCombo != null && seasonSourceCombo.getValue() != null ? seasonSourceCombo.getValue() : "";

        String customElevB64 = (elevImport && customElevImage != null) ? cachedElevB64 : null;
        String customBiomeB64 = (elevImport && customBiomeImage != null) ? cachedBiomeB64 : null;
        String customResourceB64 = (elevImport && customResourceImage != null) ? cachedResourceB64 : null;
        String customClimateB64 = (tempImport && customClimateImage != null) ? cachedClimateB64 : null;
        String customRainfallB64 = (precipImport && customRainfallImage != null) ? cachedRainfallB64 : null;
        String customSeasonalityB64 = (seasonImport && customSeasonalityImage != null) ? cachedSeasonalityB64 : null;

        return new PlanetPreset(
                presetName,
                resolutionCombo.getValue() != null ? resolutionCombo.getValue() : 6,
                radiusSlider.getValue(),
                dayLengthSlider.getValue(),
                axialTiltSlider.getValue(),
                yearLengthSlider.getValue(),
                distanceSunSlider.getValue(),
                solarLumSlider.getValue(),
                minAltSlider.getValue(),
                maxAltSlider.getValue(),
                avgTempSlider.getValue(),
                seed,
                noiseFreqSlider.getValue(),
                noiseScaleSlider.getValue(),
                waterSlider.getValue(),
                tempGradSlider.getValue(),
                oxygenSlider.getValue(),
                albedoSlider.getValue(),
                atmoPressureSlider.getValue(),
                isSat,
                parentMassSlider.getValue(),
                orbitDistanceParentSlider.getValue(),
                co2Slider.getValue(),
                presetCombo.getValue() != null ? presetCombo.getValue().seismicActivityLevel() : 2.5,
                presetCombo.getValue() != null ? presetCombo.getValue().volcanicActivityLevel() : 1.5,
                customElevB64,
                customBiomeB64,
                customResourceB64,
                customClimateB64,
                customRainfallB64,
                customSeasonalityB64,
                elevImport,
                elevSrc,
                tempImport,
                tempSrc,
                tempSeed,
                precipImport,
                precipSrc,
                precipSeed,
                seasonImport,
                seasonSrc,
                seasonSeed
        );
    }

    private void updateLegend() {
        if (legendBar == null) return;
        legendBar.getChildren().clear();

        PlanetPreset preset = buildPresetFromUI();
        int selectedIdx = viewModeCombo != null ? viewModeCombo.getSelectionModel().getSelectedIndex() : 0;

        if (selectedIdx == 0) { // Heightmap / Relief
            double minAlt = preset.minAltitudeMeters();
            double maxAlt = preset.maxAltitudeMeters();
            Label minLabel = new Label(String.format("Min: %,.0f m", minAlt));
            minLabel.getStyleClass().add("control-label");
            minLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");

            legendBar.getChildren().add(minLabel);
            addLegendItem("DEPRESSIONS", Color.rgb(30, 30, 30), I18n.getOrDefault("planet.legend.depressions", "Dépressions / Abysses"));
            addLegendItem("LOWLANDS", Color.rgb(90, 90, 90), I18n.getOrDefault("planet.legend.lowlands", "Basses Terres"));
            addLegendItem("PLAINS", Color.rgb(150, 150, 150), I18n.getOrDefault("planet.legend.plains", "Plaines / Niveau 0"));
            addLegendItem("HIGHLANDS", Color.rgb(205, 205, 205), I18n.getOrDefault("planet.legend.highlands", "Reliefs & Plateaux"));
            addLegendItem("PEAKS", Color.rgb(255, 255, 255), I18n.getOrDefault("planet.legend.peaks", "Sommets"));

            Label maxLabel = new Label(String.format("Max: %,.0f m", maxAlt));
            maxLabel.getStyleClass().add("control-label");
            maxLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");
            legendBar.getChildren().add(maxLabel);

        } else if (selectedIdx == 1) { // Température
            double minT = preset.averageTempC() - preset.temperatureGradient() - 20.0;
            double maxT = preset.averageTempC() + preset.temperatureGradient();
            Label minLabel = new Label(String.format("Min: %.1f °C", minT));
            minLabel.getStyleClass().add("control-label");
            minLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");

            legendBar.getChildren().add(minLabel);
            addLegendItem("POLAR", Color.rgb(40, 0, 120), I18n.getOrDefault("planet.legend.polar", "Polar / Gel"));
            addLegendItem("MILD", Color.rgb(0, 200, 180), I18n.getOrDefault("planet.legend.mild", "Mild"));
            addLegendItem("WARM", Color.rgb(240, 200, 0), I18n.getOrDefault("planet.legend.warm", "Chaud"));
            addLegendItem("HOT", Color.rgb(220, 0, 40), I18n.getOrDefault("planet.legend.hot", "Canicule"));

            Label maxLabel = new Label(String.format("Max: %.1f °C", maxT));
            maxLabel.getStyleClass().add("control-label");
            maxLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #ef4444;");
            legendBar.getChildren().add(maxLabel);

        } else if (selectedIdx == 2) { // Précipitations
            Label minLabel = new Label(I18n.getOrDefault("planet.legend.min_arid", "Min: 0 mm/an (Aride)"));
            minLabel.getStyleClass().add("control-label");
            minLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #eab308;");

            legendBar.getChildren().add(minLabel);
            addLegendItem("ARID", Color.rgb(210, 170, 110), I18n.getOrDefault("planet.legend.arid", "Aride"));
            addLegendItem("MODERATE", Color.rgb(60, 180, 80), I18n.getOrDefault("planet.legend.moderate", "Moderate"));
            addLegendItem("HUMID", Color.rgb(20, 160, 180), I18n.getOrDefault("planet.legend.humid", "Humide"));
            addLegendItem("HEAVY", Color.rgb(10, 60, 200), I18n.getOrDefault("planet.legend.deluge", "Deluge"));

            Label maxLabel = new Label(I18n.getOrDefault("planet.legend.max_humid", "Max: 3 000 mm/an (Humide)"));
            maxLabel.getStyleClass().add("control-label");
            maxLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #3b82f6;");
            legendBar.getChildren().add(maxLabel);

        } else { // Saisonnalité / Amplitude Thermique
            double maxAmp = Math.min(60.0, (preset.axialTiltDegrees() / 23.5) * 35.0);
            Label minLabel = new Label(I18n.getOrDefault("planet.legend.min_stable", "Min: 0.0 °C (Stable)"));
            minLabel.getStyleClass().add("control-label");
            minLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");

            legendBar.getChildren().add(minLabel);
            addLegendItem("STABLE", Color.rgb(30, 80, 140), I18n.getOrDefault("planet.legend.low_variance", "Faible Variance"));
            addLegendItem("SEASONAL", Color.rgb(80, 180, 100), I18n.getOrDefault("planet.legend.seasonal", "Seasonal"));
            addLegendItem("HIGH", Color.rgb(240, 140, 20), I18n.getOrDefault("planet.legend.high_variance", "High"));
            addLegendItem("EXTREME", Color.rgb(220, 30, 80), I18n.getOrDefault("planet.legend.extreme_amp", "Extreme Amplitude"));

            Label maxLabel = new Label(String.format("%s: %.1f °C (%s)", I18n.getOrDefault("planet.short.max", "Max"), maxAmp, I18n.getOrDefault("planet.legend.amplitude", "Amplitude")));
            maxLabel.getStyleClass().add("control-label");
            maxLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a855f7;");
            legendBar.getChildren().add(maxLabel);
        }
    }

    private void addLegendItem(String id, Color col, String text) {
        Pane colorSwatch = new Pane();
        colorSwatch.setPrefSize(14, 14);
        colorSwatch.setStyle(String.format("-fx-background-color: rgba(%d,%d,%d,1); -fx-border-color: #ffffff; -fx-border-width: 1; -fx-background-radius: 3;",
                (int)(col.getRed()*255), (int)(col.getGreen()*255), (int)(col.getBlue()*255)));
        Label lbl = new Label(text);
        lbl.getStyleClass().add("control-label");
        HBox itemBox = new HBox(4, colorSwatch, lbl);
        itemBox.setAlignment(Pos.CENTER);
        legendBar.getChildren().add(itemBox);
    }

    private void updatePreview() {
        if (previewCanvas == null) return;
        WindowUtils.setBusyCursor(this, true);
        try {
            updateLegend();

            PlanetPreset preset = buildPresetFromUI();
            GraphicsContext gc = previewCanvas.getGraphicsContext2D();

            int canvasW = (int) previewCanvas.getWidth();
            int canvasH = (int) previewCanvas.getHeight();
            if (gc == null || canvasW < 10 || canvasH < 10) return;

            int mapMode = viewModeCombo != null ? viewModeCombo.getSelectionModel().getSelectedIndex() : 0;
            boolean isImportMode = radioImport != null && radioImport.isSelected();
            PixelReader elevReader = (isImportMode && customElevImage != null) ? customElevImage.getPixelReader() : null;

            // Parse independent climate seeds from UI fields
            long tempSeed = preset.seed() + 100L;
            long precipSeed = preset.seed() + 1000L;
            long seasonSeed = preset.seed() + 2000L;
            try { if (tempSeedField   != null) tempSeed   = Long.parseLong(tempSeedField.getText());   } catch (NumberFormatException ignored) {}
            try { if (precipSeedField != null) precipSeed = Long.parseLong(precipSeedField.getText()); } catch (NumberFormatException ignored) {}
            try { if (seasonSeedField != null) seasonSeed = Long.parseLong(seasonSeedField.getText()); } catch (NumberFormatException ignored) {}
            final long tSeed = tempSeed, pSeed = precipSeed, sSeed = seasonSeed;

            // Use fast 320x160 buffer for preview rendering
            int w = 320;
            int h = 160;

            WritableImage buffer = new WritableImage(w, h);
            PixelWriter pw = buffer.getPixelWriter();

            double wElev = customElevImage != null ? customElevImage.getWidth() : 0;
            double hElev = customElevImage != null ? customElevImage.getHeight() : 0;

            // Legend temperature range accounts for CO2 and pressure greenhouse forcing
            double co2Forcing = 3.0 * (Math.log(Math.max(1.0, preset.co2Ppm()) / 280.0) / Math.log(2.0));
            double pressureBoost = (preset.atmospherePressureAtm() - 1.0) * 8.0;
            double minTemp = preset.averageTempC() + co2Forcing + pressureBoost - preset.temperatureGradient() - 20.0;
            double maxTemp = preset.averageTempC() + co2Forcing + pressureBoost + preset.temperatureGradient();

            int oceanCount = 0;
            int bgArgb = 0xFF0F172A; // Color.rgb(15, 23, 42)

            for (int py = 0; py < h; py++) {
                double y_base = (py - h / 2.0 - panY) / zoomFactor + h / 2.0;
                if (y_base < 0 || y_base >= h) {
                    for (int px = 0; px < w; px++) pw.setArgb(px, py, bgArgb);
                    continue;
                }
                double lat = 90.0 - (y_base / (double) h) * 180.0;

                for (int px = 0; px < w; px++) {
                    double x_base = (px - w / 2.0 - panX) / zoomFactor + w / 2.0;
                    if (x_base < 0 || x_base >= w) {
                        pw.setArgb(px, py, bgArgb);
                        continue;
                    }
                    double lng = -180.0 + (x_base / (double) w) * 360.0;

                    Color pxColor;
                    boolean isOcean = false;

                    if (preset.waterLevel() > -0.4) {
                        if (radioImport.isSelected() && customElevImage != null && elevReader != null) {
                            double u = (lng + 180.0) / 360.0;
                            double v = (90.0 - lat) / 180.0;
                            int ex = (int) Math.min(u * wElev, wElev - 1);
                            int ey = (int) Math.min(v * hElev, hElev - 1);
                            Color c = elevReader.getColor(ex, ey);
                            double eNorm = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                            isOcean = (eNorm < preset.waterLevel());
                        } else {
                            var point = generator.getPlanetPoint(lat, lng, preset);
                            isOcean = (point.elevation() < preset.waterLevel());
                        }
                    }

                    if (mapMode == 0) { // Heightmap / Relief
                        if (radioImport.isSelected() && customElevImage != null && elevReader != null) {
                            double u = (lng + 180.0) / 360.0;
                            double v = (90.0 - lat) / 180.0;
                            int ex = (int) Math.min(u * wElev, wElev - 1);
                            int ey = (int) Math.min(v * hElev, hElev - 1);
                            Color c = elevReader.getColor(ex, ey);
                            double eNorm = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                            if (isOcean) {
                                pxColor = Color.rgb(15, 23, 42); // Ocean
                                oceanCount++;
                            } else {
                                pxColor = getHypsometricColor(eNorm);
                            }
                        } else {
                            if (isOcean) {
                                pxColor = Color.rgb(15, 23, 42); // Deep navy for ocean
                                oceanCount++;
                            } else {
                                var point = generator.getPlanetPoint(lat, lng, preset);
                                double norm = (point.elevation() - preset.waterLevel()) / (1.0 - preset.waterLevel() + 0.001);
                                pxColor = getHypsometricColor(norm);
                            }
                        }
                    } else if (mapMode == 1) { // Température (°C)
                        double tempC;
                        if (radioTempImport != null && radioTempImport.isSelected() && customClimateImage != null) {
                            PixelReader pr = customClimateImage.getPixelReader();
                            double u = (lng + 180.0) / 360.0;
                            double v = (90.0 - lat) / 180.0;
                            int tx = (int) Math.min(u * customClimateImage.getWidth(), customClimateImage.getWidth() - 1);
                            int ty = (int) Math.min(v * customClimateImage.getHeight(), customClimateImage.getHeight() - 1);
                            int argb = pr.getArgb(tx, ty);
                            double tNorm = (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / (3.0 * 255.0);
                            tempC = minTemp + tNorm * (maxTemp - minTemp);
                        } else {
                            tempC = generator.getPlanetPoint(lat, lng, preset, tSeed, pSeed, sSeed).temperature();
                        }
                        pxColor = getTemperatureColor(tempC, minTemp, maxTemp);
                        if (isOcean) {
                            pxColor = blendColors(pxColor, Color.rgb(10, 25, 60), 0.30);
                        }
                    } else if (mapMode == 2) { // Précipitations (mm/an)
                        double precipNorm;
                        if (radioPrecipImport != null && radioPrecipImport.isSelected() && customRainfallImage != null) {
                            PixelReader pr = customRainfallImage.getPixelReader();
                            double u = (lng + 180.0) / 360.0;
                            double v = (90.0 - lat) / 180.0;
                            int rx = (int) Math.min(u * customRainfallImage.getWidth(), customRainfallImage.getWidth() - 1);
                            int ry = (int) Math.min(v * customRainfallImage.getHeight(), customRainfallImage.getHeight() - 1);
                            int argb = pr.getArgb(rx, ry);
                            precipNorm = (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / (3.0 * 255.0);
                        } else {
                            precipNorm = generator.getPlanetPoint(lat, lng, preset, tSeed, pSeed, sSeed).rainfall();
                        }
                        pxColor = getPrecipitationColor(precipNorm);
                        if (isOcean) {
                            pxColor = blendColors(pxColor, Color.rgb(15, 23, 42), 0.35);
                        }
                    } else { // Seasonality / Thermal Amplitude (°C)
                        double seasonNorm;
                        if (radioSeasonImport != null && radioSeasonImport.isSelected() && customSeasonalityImage != null) {
                            PixelReader pr = customSeasonalityImage.getPixelReader();
                            double u = (lng + 180.0) / 360.0;
                            double v = (90.0 - lat) / 180.0;
                            int sx = (int) Math.min(u * customSeasonalityImage.getWidth(), customSeasonalityImage.getWidth() - 1);
                            int sy = (int) Math.min(v * customSeasonalityImage.getHeight(), customSeasonalityImage.getHeight() - 1);
                            int argb = pr.getArgb(sx, sy);
                            seasonNorm = (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / (3.0 * 255.0);
                        } else {
                            seasonNorm = generator.getPlanetPoint(lat, lng, preset, tSeed, pSeed, sSeed).seasonality();
                        }
                        pxColor = getSeasonalityColor(seasonNorm);
                        if (isOcean) {
                            pxColor = blendColors(pxColor, Color.rgb(10, 20, 50), 0.30);
                        }
                    }

                    pw.setColor(px, py, pxColor);
                }
            }

            // Render buffer onto preview canvas
            gc.drawImage(buffer, 0, 0, canvasW, canvasH);

            // Global planetary ocean/land area ratio calculation with cosine latitude weighting.
            // Decoupled from viewMode (temperature, precipitation, seasonality) and canvas zoom/pan.
            double totalAreaWeight = 0.0;
            double oceanAreaWeight = 0.0;
            int sampleH = 160;
            int sampleW = 320;

            for (int gy = 0; gy < sampleH; gy++) {
                double gLat = 90.0 - (gy + 0.5) * (180.0 / sampleH);
                double weight = Math.cos(Math.toRadians(gLat));
                for (int gx = 0; gx < sampleW; gx++) {
                    double gLng = -180.0 + (gx + 0.5) * (360.0 / sampleW);
                    boolean isGlobOcean = false;
                    if (preset.waterLevel() > -0.4) {
                        if (isImportMode && customElevImage != null && elevReader != null && wElev > 0 && hElev > 0) {
                            double u = (gLng + 180.0) / 360.0;
                            double v = (90.0 - gLat) / 180.0;
                            int ex = (int) Math.min(u * wElev, wElev - 1);
                            int ey = (int) Math.min(v * hElev, hElev - 1);
                            Color c = elevReader.getColor(ex, ey);
                            double eNorm = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                            isGlobOcean = (eNorm < 0.35);
                        } else {
                            var pt = generator.getPlanetPoint(gLat, gLng, preset);
                            isGlobOcean = (pt.elevation() < preset.waterLevel());
                        }
                    }
                    totalAreaWeight += weight;
                    if (isGlobOcean) {
                        oceanAreaWeight += weight;
                    }
                }
            }

            int oceanPct = (int) Math.round((oceanAreaWeight * 100.0) / Math.max(0.0001, totalAreaWeight));
            statsLabel.setText(I18n.get("planet.stats.ocean_land", oceanPct, 100 - oceanPct));
            astroLabel.setText(String.format("%s: %.0f km | %s: %.1fh | %s: %.1f° | %s: %.0f d | %s: %.2f AU | %s: %.1f°C",
                    preset.isSatellite() ? I18n.getOrDefault("planet.short.moon_radius", "🌕 Lune Rayon") : I18n.getOrDefault("planet.short.radius", "📐 Rayon"),
                    preset.radiusKm(),
                    I18n.getOrDefault("planet.short.day", "Jour"),
                    preset.dayLengthHours(),
                    I18n.getOrDefault("planet.short.tilt", "Tilt"),
                    preset.axialTiltDegrees(),
                    I18n.getOrDefault("planet.short.year", "Year"),
                    preset.yearLengthDays(),
                    I18n.getOrDefault("planet.short.dist", "Dist"),
                    preset.distanceToSunAU(),
                    I18n.getOrDefault("planet.short.temp", "Temp"),
                    preset.averageTempC()));
        } finally {
            WindowUtils.setBusyCursor(this, false);
        }
    }

    private Color blendColors(Color base, Color overlay, double opacity) {
        if (base == null) return overlay;
        double r = base.getRed() * (1.0 - opacity) + overlay.getRed() * opacity;
        double g = base.getGreen() * (1.0 - opacity) + overlay.getGreen() * opacity;
        double b = base.getBlue() * (1.0 - opacity) + overlay.getBlue() * opacity;
        return Color.color(Math.max(0.0, Math.min(1.0, r)), Math.max(0.0, Math.min(1.0, g)), Math.max(0.0, Math.min(1.0, b)));
    }

    private Color getTemperatureColor(double tempC, double minT, double maxT) {
        double norm = Math.max(0.0, Math.min(1.0, (tempC - minT) / (maxT - minT + 0.001)));
        if (norm < 0.25) {
            double t = norm / 0.25;
            return Color.rgb((int)(40 * (1 - t)), (int)(100 * t), (int)(120 + 100 * t));
        } else if (norm < 0.5) {
            double t = (norm - 0.25) / 0.25;
            return Color.rgb((int)(100 * t), (int)(100 + 100 * t), (int)(220 * (1 - t)));
        } else if (norm < 0.75) {
            double t = (norm - 0.5) / 0.25;
            return Color.rgb((int)(100 + 140 * t), (int)(200 - 40 * t), 0);
        } else {
            double t = (norm - 0.75) / 0.25;
            return Color.rgb((int)(240 - 20 * t), (int)(160 * (1 - t)), (int)(40 * t));
        }
    }

    private Color getPrecipitationColor(double norm) {
        norm = Math.max(0.0, Math.min(1.0, norm));
        if (norm < 0.25) {
            double t = norm / 0.25;
            return Color.rgb((int)(210 - 10 * t), (int)(170 + 40 * t), (int)(110 + 10 * t));
        } else if (norm < 0.5) {
            double t = (norm - 0.25) / 0.25;
            return Color.rgb((int)(200 - 140 * t), (int)(210 - 30 * t), (int)(120 - 40 * t));
        } else if (norm < 0.75) {
            double t = (norm - 0.5) / 0.25;
            return Color.rgb((int)(60 - 40 * t), (int)(180 - 20 * t), (int)(80 + 100 * t));
        } else {
            double t = (norm - 0.75) / 0.25;
            return Color.rgb((int)(20 - 10 * t), (int)(160 - 100 * t), (int)(180 + 20 * t));
        }
    }

    private Color getSeasonalityColor(double norm) {
        norm = Math.max(0.0, Math.min(1.0, norm));
        if (norm < 0.33) {
            double t = norm / 0.33;
            return Color.rgb((int)(30 + 50 * t), (int)(80 + 100 * t), (int)(140 - 40 * t));
        } else if (norm < 0.66) {
            double t = (norm - 0.33) / 0.33;
            return Color.rgb((int)(80 + 160 * t), (int)(180 - 40 * t), (int)(100 - 80 * t));
        } else {
            double t = (norm - 0.66) / 0.34;
            return Color.rgb((int)(240 - 20 * t), (int)(140 - 110 * t), (int)(20 + 60 * t));
        }
    }

    private Color getHypsometricColor(double norm) {
        norm = Math.max(0.0, Math.min(1.0, norm));
        if (norm < 0.20) {
            double t = norm / 0.20;
            return Color.rgb((int)(34 + 60 * t), (int)(139 + 30 * t), (int)(34 + 20 * t));
        } else if (norm < 0.40) {
            double t = (norm - 0.20) / 0.20;
            return Color.rgb((int)(94 + 86 * t), (int)(169 + 16 * t), (int)(54 + 21 * t));
        } else if (norm < 0.65) {
            double t = (norm - 0.40) / 0.25;
            return Color.rgb((int)(180 - 10 * t), (int)(185 - 65 * t), (int)(75 - 15 * t));
        } else if (norm < 0.85) {
            double t = (norm - 0.65) / 0.20;
            return Color.rgb((int)(170 - 20 * t), (int)(120 + 40 * t), (int)(60 + 80 * t));
        } else {
            double t = (norm - 0.85) / 0.15;
            return Color.rgb((int)(150 + 95 * t), (int)(160 + 90 * t), (int)(140 + 115 * t));
        }
    }

    private Color getBiomeColor(Biome biome) {
        return switch (biome) {
            case OCEAN -> Color.rgb(25, 60, 160);
            case DEEP_OCEAN -> Color.rgb(10, 30, 110);
            case BEACH -> Color.rgb(238, 214, 175);
            case DESERT -> Color.rgb(220, 170, 120);
            case PLAINS -> Color.rgb(110, 210, 80);
            case SAVANNAH -> Color.rgb(180, 200, 70);
            case FOREST -> Color.rgb(34, 139, 34);
            case JUNGLE -> Color.rgb(0, 100, 40);
            case MOUNTAINS -> Color.rgb(140, 130, 130);
            case HILLS -> Color.rgb(160, 160, 110);
            case TUNDRA -> Color.rgb(200, 200, 170);
            case SNOW -> Color.rgb(245, 245, 250);
            case GLACIER -> Color.rgb(220, 240, 255);
            default -> Color.BLACK;
        };
    }

    public void updateTexts() {
        boolean oldUpdating = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            headerLabel.setText(I18n.get("planet.section.header"));
            presetsSecHeader.setText(I18n.getOrDefault("planet.section.presets", "GLOBAL PRESETS & PLANET SAVE"));
            generalSecHeader.setText(I18n.getOrDefault("planet.section.general", "GENERAL PARAMETERS & RESOLUTION"));
            astroSecHeader.setText(I18n.getOrDefault("planet.section.astro", "ASTRONOMY, PHYSICS & CELESTIAL BODIES DOMAIN"));
            topoSecHeader.setText(I18n.getOrDefault("planet.section.topo", "TOPOGRAPHY & PLANETARY RELIEF DOMAIN"));
            climateSecHeader.setText(I18n.getOrDefault("planet.section.climate", "ATMOSPHERE & PLANETARY CLIMATE DOMAIN"));
            previewTitle.setText(I18n.get("planet.preview.title"));

            if (presetRowLabel != null) presetRowLabel.setText(I18n.get("planet.preset"));
            if (bodyTypeRowLabel != null) bodyTypeRowLabel.setText(I18n.getOrDefault("planet.param.body_type", "Celestial Body Type:"));
            if (parentMassRowLabel != null) parentMassRowLabel.setText(I18n.getOrDefault("planet.param.parent_mass", "Host planet mass (M⊕)"));
            if (orbitDistanceParentRowLabel != null) orbitDistanceParentRowLabel.setText(I18n.getOrDefault("planet.param.parent_dist", "Host orbital distance (km)"));

            if (mapSourceRowLabel != null) mapSourceRowLabel.setText(I18n.get("planet.map.preset_body"));
            if (elevMapRowLabel != null) elevMapRowLabel.setText(I18n.get("planet.map.elevation"));
            if (biomeMapRowLabel != null) biomeMapRowLabel.setText(I18n.get("planet.map.biomes"));
            if (resourceMapRowLabel != null) resourceMapRowLabel.setText(I18n.get("planet.map.resources"));
            if (climateMapRowLabel != null) climateMapRowLabel.setText(I18n.getOrDefault("planet.map.climate", "Climate Map (Thermal / RGB):"));
            if (rainfallMapRowLabel != null) rainfallMapRowLabel.setText(I18n.getOrDefault("planet.map.rainfall", "Precipitation Map (Humidity):"));
            if (seasonalityMapRowLabel != null) seasonalityMapRowLabel.setText(I18n.getOrDefault("planet.map.seasonality", "Seasonality / Variance Map:"));

            if (loadElevBtn != null) loadElevBtn.setText(I18n.get("planet.map.btn_load"));
            if (loadBiomeBtn != null) loadBiomeBtn.setText(I18n.get("planet.map.btn_load"));
            if (loadResourceBtn != null) loadResourceBtn.setText(I18n.get("planet.map.btn_load"));
            if (loadClimateBtn != null) loadClimateBtn.setText(I18n.get("planet.map.btn_load"));
            if (loadRainfallBtn != null) loadRainfallBtn.setText(I18n.get("planet.map.btn_load"));
            if (loadSeasonalityBtn != null) loadSeasonalityBtn.setText(I18n.get("planet.map.btn_load"));

            radiusRowLabel.setText(I18n.get("planet.param.radius"));
            resRowLabel.setText(I18n.get("planet.param.resolution"));
            dayRowLabel.setText(I18n.get("planet.param.day_length"));
            tiltRowLabel.setText(I18n.get("planet.param.axial_tilt"));
            yearRowLabel.setText(I18n.get("planet.param.year_length"));
            distRowLabel.setText(I18n.get("planet.param.distance_sun"));
            lumRowLabel.setText(I18n.get("planet.param.solar_lum"));
            tempRowLabel.setText(I18n.get("planet.param.avg_temp"));

            seedRowLabel.setText(I18n.get("planet.param.seed"));
            minAltRowLabel.setText(I18n.get("planet.param.min_alt"));
            maxAltRowLabel.setText(I18n.get("planet.param.max_alt"));
            waterRowLabel.setText(I18n.get("planet.param.water_level"));
            freqRowLabel.setText(I18n.get("planet.param.noise_freq"));
            scaleRowLabel.setText(I18n.get("planet.param.noise_scale"));
            gradRowLabel.setText(I18n.get("planet.param.temp_grad"));

            if (oxygenRowLabel != null) oxygenRowLabel.setText(I18n.getOrDefault("planet.param.oxygen", "Oxygen Level O₂ (%)"));
            if (co2RowLabel != null) co2RowLabel.setText(I18n.getOrDefault("planet.param.co2", "Dioxyde de Carbone CO₂ (ppm)"));
            if (albedoRowLabel != null) albedoRowLabel.setText(I18n.getOrDefault("planet.param.albedo", "Planetary Albedo"));
            if (atmoPressureRowLabel != null) atmoPressureRowLabel.setText(I18n.getOrDefault("planet.param.atmo_pressure", "Atmospheric Pressure (atm)"));

            if (radioProc != null) radioProc.setText(I18n.getOrDefault("planet.radio.procedural", "▶ Procedural Generation (Perlin Noise)"));
            if (radioImport != null) radioImport.setText(I18n.getOrDefault("planet.radio.import", "📂 Import Heightmap Externe (PNG/GeoTIFF)"));

            if (radioTempProc != null) radioTempProc.setText(I18n.getOrDefault("planet.radio.procedural", "▶ Procedural Generation (Perlin Noise)"));
            if (radioTempImport != null) radioTempImport.setText(I18n.getOrDefault("planet.radio.import_wms", "📂 Source Externe (PNG / WMS)"));
            if (radioPrecipProc != null) radioPrecipProc.setText(I18n.getOrDefault("planet.radio.procedural", "▶ Procedural Generation (Perlin Noise)"));
            if (radioPrecipImport != null) radioPrecipImport.setText(I18n.getOrDefault("planet.radio.import_wms", "📂 Source Externe (PNG / WMS)"));
            if (radioSeasonProc != null) radioSeasonProc.setText(I18n.getOrDefault("planet.radio.procedural", "▶ Procedural Generation (Perlin Noise)"));
            if (radioSeasonImport != null) radioSeasonImport.setText(I18n.getOrDefault("planet.radio.import_wms", "📂 Source Externe (PNG / WMS)"));

            if (tempSubHeader != null) tempSubHeader.setText(I18n.getOrDefault("planet.climate.temp.header", "🌡  Temperature & Thermal:"));
            if (precipSubHeader != null) precipSubHeader.setText(I18n.getOrDefault("planet.climate.precip.header", "🌧  Precipitation / Humidity:"));
            if (seasonSubHeader != null) seasonSubHeader.setText(I18n.getOrDefault("planet.climate.season.header", "🍂  Seasonality / Thermal Variance:"));
            if (earthPaleoNoticeLabel != null) earthPaleoNoticeLabel.setText(I18n.getOrDefault("planet.climate.earth_paleo_notice", "🌍 Note Paléoclimatique : Pour la Terre, le moteur intègre la dynamique temporelle (glaciations, niveau des mers LGM -120m, Sahara Vert). Les cartes ci-contre présentent la baseline climatologique actuelle (ERA5 / WorldClim)."));

            if (tempSeedLabel != null) tempSeedLabel.setText(I18n.getOrDefault("planet.climate.seed_label", "Generation seed:"));
            if (precipSeedLabel != null) precipSeedLabel.setText(I18n.getOrDefault("planet.climate.seed_label", "Generation seed:"));
            if (seasonSeedLabel != null) seasonSeedLabel.setText(I18n.getOrDefault("planet.climate.seed_label", "Generation seed:"));

            if (tempSourceLabel != null) tempSourceLabel.setText(I18n.getOrDefault("planet.climate.source_label", "Reference source:"));
            if (precipSourceLabel != null) precipSourceLabel.setText(I18n.getOrDefault("planet.climate.source_label", "Reference source:"));
            if (seasonSourceLabel != null) seasonSourceLabel.setText(I18n.getOrDefault("planet.climate.source_label", "Reference source:"));

            if (tempHintLabel != null) tempHintLabel.setText(I18n.getOrDefault("planet.climate.temp.hint", "ℹ  Thermal map is calculated from equator-pole gradient and random seed bias."));
            if (precipHintLabel != null) precipHintLabel.setText(I18n.getOrDefault("planet.climate.precip.hint", "ℹ  Precipitation map is generated from latitudinal hygrometric bias and seed."));
            if (seasonHintLabel != null) seasonHintLabel.setText(I18n.getOrDefault("planet.climate.season.hint", "ℹ  Seasonality is derived from axial tilt and random latitudinal bias."));

            if (tempFormatHintLabel != null) tempFormatHintLabel.setText(I18n.getOrDefault("planet.climate.temp.format", "PNG niveaux de gris (projection équirectangulaire 2:1) :\n  Black (0) = −50°C | White (255) = +50°C"));
            if (precipFormatHintLabel != null) precipFormatHintLabel.setText(I18n.getOrDefault("planet.climate.precip.format", "PNG niveaux de gris (projection équirectangulaire 2:1) :\n  Black (0) = 0 mm/yr | White (255) = 3,000 mm/yr"));
            if (seasonFormatHintLabel != null) seasonFormatHintLabel.setText(I18n.getOrDefault("planet.climate.season.format", "PNG niveaux de gris (projection équirectangulaire 2:1) :\n  Black (0) = 0°C amplitude | White (255) = 50°C annual amplitude"));

            if (bodyTypeCombo != null && bodyTypeCombo.getCellFactory() != null) {
                bodyTypeCombo.setButtonCell(bodyTypeCombo.getCellFactory().call(null));
            }
            if (mapSourceCombo != null && mapSourceCombo.getCellFactory() != null) {
                mapSourceCombo.setButtonCell(mapSourceCombo.getCellFactory().call(null));
            }
            if (tempSourceCombo != null && tempSourceCombo.getCellFactory() != null) {
                tempSourceCombo.setButtonCell(tempSourceCombo.getCellFactory().call(null));
            }
            if (precipSourceCombo != null && precipSourceCombo.getCellFactory() != null) {
                precipSourceCombo.setButtonCell(precipSourceCombo.getCellFactory().call(null));
            }
            if (seasonSourceCombo != null && seasonSourceCombo.getCellFactory() != null) {
                seasonSourceCombo.setButtonCell(seasonSourceCombo.getCellFactory().call(null));
            }

            if (viewModeCombo != null) {
                int selected = viewModeCombo.getSelectionModel().getSelectedIndex();
                viewModeCombo.getItems().setAll(
                    I18n.getOrDefault("planet.view.heightmap", "MAP 1. Heightmap & Relief Map"),
                    I18n.getOrDefault("planet.view.temperature", "MAP 2. Temperature Map (°C)"),
                    I18n.getOrDefault("planet.view.precipitation", "MAP 3. Precipitation Map (mm/yr)"),
                    I18n.getOrDefault("planet.view.seasonality", "MAP 4. Seasonality / Thermal Amplitude Map (°C)")
                );
                if (selected >= 0 && selected < viewModeCombo.getItems().size()) {
                    viewModeCombo.getSelectionModel().select(selected);
                }
            }
            updateLegend();

            calculateStellarIrradiance();
            updateAltRangeDisplay();
            updateTooltips();
            updateAstroLabelText();
        } finally {
            isUpdatingFromPreset = oldUpdating;
        }
    }

    private void updateAstroLabelText() {
        if (astroLabel == null) return;
        PlanetPreset preset = buildPresetFromUI();
        if (preset == null) return;
        astroLabel.setText(String.format("%s: %.0f km | %s: %.1fh | %s: %.1f° | %s: %.0f d | %s: %.2f AU | %s: %.1f°C",
                preset.isSatellite() ? I18n.getOrDefault("planet.short.moon_radius", "🌕 Lune Rayon") : I18n.getOrDefault("planet.short.radius", "📐 Rayon"),
                preset.radiusKm(),
                I18n.getOrDefault("planet.short.day", "Jour"),
                preset.dayLengthHours(),
                I18n.getOrDefault("planet.short.tilt", "Tilt"),
                preset.axialTiltDegrees(),
                I18n.getOrDefault("planet.short.year", "Year"),
                preset.yearLengthDays(),
                I18n.getOrDefault("planet.short.dist", "Dist"),
                preset.distanceToSunAU(),
                I18n.getOrDefault("planet.short.temp", "Temp"),
                preset.averageTempC()));
    }

    private void updateTooltips() {
        if (fetchOnlineBtn != null) fetchOnlineBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.fetch_online", "Download NASA/USGS WMS satellite maps")));
        if (fetchOnlineClimateBtn != null) fetchOnlineClimateBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.fetch_online_climate", "Download NASA MODIS thermal and GPM rainfall satellite maps (WMS)")));
        if (exportMapsBtn != null) exportMapsBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.export_map", "Export maps as PNG with ESRI World File (.tfw)")));
        if (loadElevBtn != null) loadElevBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.elev_load", "Load external heightmap elevation map")));
        if (loadClimateBtn != null) loadClimateBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.climate_load", "Load external thermal/climate map")));
        if (loadRainfallBtn != null) loadRainfallBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.rainfall_load", "Load external rainfall/humidity map")));
        if (loadSeasonalityBtn != null) loadSeasonalityBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.seasonality_load", "Load external seasonal variance map")));
    }

    public boolean validatePlanetSetup() {
        return validatePlanetSetup(true);
    }

    private boolean isCustomFileRequired(ComboBox<String> combo) {
        if (combo == null || combo.getValue() == null) return false;
        String val = combo.getValue().trim().toLowerCase();
        return "custom".equalsIgnoreCase(val) || "file".equalsIgnoreCase(val);
    }

    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();
        if (radioImport != null && radioImport.isSelected()) {
            if (customElevImage == null) {
                errors.add(I18n.getOrDefault("planet.validation.missing_elev_map", "Image heightmap d'altitude manquante en mode d'importation (Onglet 1)."));
            } else {
                org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(customElevImage);
                if (!val.valid()) {
                    errors.add(I18n.getOrDefault("planet.validation.invalid_elev_map", "Image heightmap d'altitude incompatible (Onglet 1) : ") + val.message());
                }
            }
        }
        if (radioTempImport != null && radioTempImport.isSelected()) {
            if (customClimateImage == null) {
                errors.add(I18n.getOrDefault("planet.validation.missing_temp_map", "Missing thermal map in import mode (Tab 1)."));
            } else {
                org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(customClimateImage);
                if (!val.valid()) {
                    errors.add(I18n.getOrDefault("planet.validation.invalid_temp_map", "Thermal map incompatible (Tab 1): ") + val.message());
                }
            }
        }
        if (radioPrecipImport != null && radioPrecipImport.isSelected()) {
            if (customRainfallImage == null) {
                errors.add(I18n.getOrDefault("planet.validation.missing_precip_map", "Missing rainfall map in import mode (Tab 1)."));
            } else {
                org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(customRainfallImage);
                if (!val.valid()) {
                    errors.add(I18n.getOrDefault("planet.validation.invalid_precip_map", "Rainfall map incompatible (Tab 1): ") + val.message());
                }
            }
        }
        if (radioSeasonImport != null && radioSeasonImport.isSelected()) {
            if (customSeasonalityImage == null) {
                errors.add(I18n.getOrDefault("planet.validation.missing_season_map", "Missing seasonality map in import mode (Tab 1)."));
            } else {
                org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(customSeasonalityImage);
                if (!val.valid()) {
                    errors.add(I18n.getOrDefault("planet.validation.invalid_season_map", "Seasonality map incompatible (Tab 1): ") + val.message());
                }
            }
        }
        if (minAltSlider != null && maxAltSlider != null && minAltSlider.getValue() >= maxAltSlider.getValue()) {
            errors.add(I18n.getOrDefault("planet.validation.invalid_alt_range", "Minimum altitude must be strictly lower than maximum altitude (Tab 1)."));
        }
        return errors;
    }

    public boolean validatePlanetSetup(boolean showDialog) {
        List<String> errors = getValidationErrors();
        boolean isValid = errors.isEmpty();

        boolean elevBad = radioImport != null && radioImport.isSelected() && (customElevImage == null || !org.ether.society.data.ImageMapLoader.validateMapImage(customElevImage).valid());
        if (loadElevBtn != null) loadElevBtn.setStyle(elevBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        boolean tempBad = radioTempImport != null && radioTempImport.isSelected() && (customClimateImage == null || !org.ether.society.data.ImageMapLoader.validateMapImage(customClimateImage).valid());
        if (loadClimateBtn != null) loadClimateBtn.setStyle(tempBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        boolean precipBad = radioPrecipImport != null && radioPrecipImport.isSelected() && (customRainfallImage == null || !org.ether.society.data.ImageMapLoader.validateMapImage(customRainfallImage).valid());
        if (loadRainfallBtn != null) loadRainfallBtn.setStyle(precipBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        boolean seasonBad = radioSeasonImport != null && radioSeasonImport.isSelected() && (customSeasonalityImage == null || !org.ether.society.data.ImageMapLoader.validateMapImage(customSeasonalityImage).valid());
        if (loadSeasonalityBtn != null) loadSeasonalityBtn.setStyle(seasonBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        if (!isValid) {
            if (showDialog && validationWarningLabel != null && validationWarningBanner != null) {
                StringBuilder errorMsg = new StringBuilder();
                for (String err : errors) {
                    errorMsg.append("• ").append(err).append("\n");
                }
                validationWarningLabel.setText(errorMsg.toString().trim());
                validationWarningBanner.setVisible(true);
                validationWarningBanner.setManaged(true);
            }
        } else {
            if (validationWarningBanner != null) {
                validationWarningBanner.setVisible(false);
                validationWarningBanner.setManaged(false);
            }
        }

        return isValid;
    }

    public boolean isDirty() {
        return presetBar != null && presetBar.isDirty();
    }

    public boolean promptSaveIfDirty(javafx.stage.Window owner) {
        if (presetBar == null) return true;
        return presetBar.promptSavePresetIfDirty(owner);
    }

    public PresetControlBar<PlanetPreset> getPresetBar() {
        return presetBar;
    }
}
