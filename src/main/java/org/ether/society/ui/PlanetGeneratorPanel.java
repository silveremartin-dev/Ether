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
    private Image customBiomeImage;
    private Image customResourceImage;
    private Image customClimateImage;     // Temperature Map / Combined RGB Map
    private Image customRainfallImage;    // Separate Moisture / Precipitation Map
    private Image customSeasonalityImage; // Separate Seasonality / Temp Amplitude Map

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
    private Slider albedoSlider;
    private Slider atmoPressureSlider;

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
    private Button climateHelpBtn;

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
    private Button generateBtn;
    private ProgressBar progressBar;

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

    private Canvas previewCanvas;
    private boolean isUpdatingFromPreset = false;

    public PlanetGeneratorPanel(Consumer<List<H3Cell>> onPlanetGeneratedCallback) {
        this.generator = new ProceduralGenerator();
        this.mapLoader = new ImageMapLoader();
        this.onlineMapService = new OnlineMapService();
        this.onPlanetGeneratedCallback = onPlanetGeneratedCallback;

        getStyleClass().add("glass-panel");
        setPadding(new Insets(20));

        initUI();
        updateTexts();

        // Bind to i18n language changes
        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    private void initUI() {
        VBox controlsBox = new VBox(15);
        controlsBox.setPrefWidth(460);
        controlsBox.setPadding(new Insets(10));

        headerLabel = new Label();
        headerLabel.getStyleClass().add("label-title");

        // --- 1. Global Presets Control Bar ---
        PresetControlBar<PlanetPreset> topPresetBar = new PresetControlBar<>(I18n.getOrDefault("planet.preset", "Préréglage Global"));
        presetCombo = topPresetBar.getPresetCombo();
        presetCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item.name());
                }
            }
        });
        presetCombo.setButtonCell(presetCombo.getCellFactory().call(null));

        topPresetBar.setPresets(PlanetPreset.getPresets(), PlanetPreset.EARTH_LIKE);
        topPresetBar.setListener(new PresetControlBar.PresetActionsListener<PlanetPreset>() {
            @Override
            public void onPresetSelected(PlanetPreset preset) {
                if (!isUpdatingFromPreset) {
                    applyPreset(preset);
                }
            }

            @Override
            public void onSavePreset(String name) {
                PlanetPreset current = buildPresetFromUI();
                PlanetPreset custom = new PlanetPreset(
                        name, current.resolution(), current.radiusKm(), current.dayLengthHours(),
                        current.axialTiltDegrees(), current.yearLengthDays(), current.distanceToSunAU(),
                        current.solarLuminosity(), current.minAltitudeMeters(), current.maxAltitudeMeters(),
                        current.averageTempC(), current.seed(), current.noiseFrequency(),
                        current.noiseScale(), current.waterLevel(), current.temperatureGradient(),
                        current.oxygenPercentage(), current.albedo(), current.atmospherePressureAtm(),
                        current.isSatellite(), current.parentPlanetMassEarthMasses(),
                        current.orbitalDistanceToParentKm(), current.co2Ppm()
                );
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

        presetsSecHeader = new Label();
        VBox presetSection = createSection(presetsSecHeader, topPresetBar);

        // --- 2. General Parameters (Resolution & Shared Seed) ---
        VBox generalControls = new VBox(8);
        generalSecHeader = new Label(I18n.getOrDefault("planet.section.general", "PARAMÈTRES GÉNÉRAUX & RÉSOLUTION"));

        resolutionCombo = new ComboBox<>();
        resolutionCombo.getItems().addAll(5, 6, 7, 8);
        resolutionCombo.setValue(6);
        resolutionCombo.setMaxWidth(Double.MAX_VALUE);
        resolutionCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(I18n.get("planet.param.resolution.res" + item));
                }
            }
        });
        resolutionCombo.setButtonCell(resolutionCombo.getCellFactory().call(null));
        resolutionCombo.setOnAction(e -> updatePreview());

        seedField = new TextField("12345");
        seedField.textProperty().addListener((obs, old, val) -> updatePreview());
        randSeedBtn = new Button("🎲");
        randSeedBtn.getStyleClass().add("button-secondary");
        randSeedBtn.setOnAction(e -> {
            seedField.setText(String.valueOf(new Random().nextLong(1000000)));
            updatePreview();
        });
        HBox seedBox = new HBox(5, seedField, randSeedBtn);
        HBox.setHgrow(seedField, Priority.ALWAYS);

        resRowLabel = new Label();
        seedRowLabel = new Label();

        generalControls.getChildren().addAll(
                createControlRow(resRowLabel, resolutionCombo, I18n.getOrDefault("planet.tooltip.resolution", "Résolution de la grille hexagonale H3")),
                createControlRow(seedRowLabel, seedBox, I18n.getOrDefault("planet.tooltip.seed", "Graine aléatoire partagée pour la génération déterministe"))
        );
        VBox generalSection = createSection(generalSecHeader, generalControls);

        // --- 3. Astronomical & Physical Section (Planets & Natural Satellites / Moons) ---
        VBox astroControls = new VBox(8);

        bodyTypeRowLabel = new Label(I18n.getOrDefault("planet.param.body_type", "Type de corps céleste :"));
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
                            I18n.getOrDefault("planet.body_type.satellite", "🌕 Satellite Naturel / Lune (Orbite Planétaire)") :
                            I18n.getOrDefault("planet.body_type.planet", "🪐 Planète Indépendante (Orbite Stellaire Directe)"));
                }
            }
        });
        bodyTypeCombo.setButtonCell(bodyTypeCombo.getCellFactory().call(null));
        bodyTypeCombo.setMaxWidth(Double.MAX_VALUE);
        bodyTypeCombo.setOnAction(e -> toggleSatelliteControls());

        parentMassSlider = createSlider(0.1, 500.0, 1.0);
        orbitDistanceParentSlider = createSlider(5000, 2000000, 384400);

        parentMassRowLabel = new Label();
        orbitDistanceParentRowLabel = new Label();

        satelliteRowBox = new VBox(6,
                createControlRow(parentMassRowLabel, parentMassSlider, "%.1f M⊕", I18n.getOrDefault("planet.tooltip.parent_mass", "Masse de la planète hôte autour de laquelle gravite le satellite")),
                createControlRow(orbitDistanceParentRowLabel, orbitDistanceParentSlider, "%.0f km", I18n.getOrDefault("planet.tooltip.parent_dist", "Distance orbitale moyenne autour de la planète hôte"))
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
                createControlRow(bodyTypeRowLabel, bodyTypeCombo, I18n.getOrDefault("planet.tooltip.body_type", "Basculer entre une planète indépendante et une lune/satellite naturel")),
                satelliteRowBox,
                createControlRow(radiusRowLabel, radiusSlider, "%.0f km", I18n.getOrDefault("planet.tooltip.radius", "Rayon moyen de la planète en km")),
                createControlRow(dayRowLabel, dayLengthSlider, "%.1f h", I18n.getOrDefault("planet.tooltip.day_length", "Durée de rotation planétaire en heures")),
                createControlRow(tiltRowLabel, axialTiltSlider, "%.1f°", I18n.getOrDefault("planet.tooltip.axial_tilt", "Inclinaison de l'axe de rotation")),
                createControlRow(yearRowLabel, yearLengthSlider, "%.0f d", I18n.getOrDefault("planet.tooltip.year_length", "Durée de révolution en jours")),
                createControlRow(distRowLabel, distanceSunSlider, "%.2f AU", I18n.getOrDefault("planet.tooltip.distance_sun", "Distance à l'étoile centrale en UA")),
                createControlRow(lumRowLabel, solarLumSlider, "%.2f L☉", I18n.getOrDefault("planet.tooltip.solar_lum", "Luminosité de l'étoile centrale")),
                irradianceLabel,
                createControlRow(tempRowLabel, avgTempSlider, "%.1f °C", I18n.getOrDefault("planet.tooltip.avg_temp", "Température moyenne globale à la surface"))
        );

        astroSecHeader = new Label();
        VBox astroSection = createSection(astroSecHeader, astroControls);

        // --- 4. Topography & Relief Section (Elevation Noise & Heightmap Imports) ---
        VBox topoControls = new VBox(8);

        minAltSlider = createSlider(-15000, -500, -11000);
        maxAltSlider = createSlider(500, 25000, 8848);
        minAltSlider.valueProperty().addListener((obs, old, val) -> updateAltRangeDisplay());
        maxAltSlider.valueProperty().addListener((obs, old, val) -> updateAltRangeDisplay());

        waterSlider = createSlider(-0.5, 1.0, 0.0);
        noiseFreqSlider = createSlider(0.1, 2.0, 1.0);
        noiseScaleSlider = createSlider(0.5, 3.0, 1.0);

        minAltRowLabel = new Label();
        maxAltRowLabel = new Label();
        waterRowLabel = new Label();
        freqRowLabel = new Label();
        scaleRowLabel = new Label();

        altRangeLabel = new Label();
        altRangeLabel.getStyleClass().add("value-label");
        updateAltRangeDisplay();

        // Custom Heightmap & Preset Body Selector
        mapSourceRowLabel = new Label();
        mapSourceCombo = new ComboBox<>();
        mapSourceCombo.getItems().addAll("none", "earth", "mars", "venus", "moon");
        mapSourceCombo.setValue("none");
        mapSourceCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(I18n.get("planet.map." + item));
                }
            }
        });
        mapSourceCombo.setButtonCell(mapSourceCombo.getCellFactory().call(null));
        mapSourceCombo.setMaxWidth(Double.MAX_VALUE);
        mapSourceCombo.setOnAction(e -> applyMapSourcePreset(mapSourceCombo.getValue()));

        elevMapRowLabel = new Label();
        elevFileLabel = new Label(I18n.get("planet.map.none"));
        elevFileLabel.getStyleClass().add("value-label");
        loadElevBtn = new Button(I18n.get("planet.map.btn_load"));
        loadElevBtn.getStyleClass().add("button-secondary");
        loadElevBtn.setOnAction(e -> chooseElevMapFile());
        clearElevBtn = new Button("❌");
        clearElevBtn.getStyleClass().add("button-secondary");
        clearElevBtn.setOnAction(e -> {
            customElevImage = null;
            elevFileLabel.setText(I18n.get("planet.map.none"));
            updatePreview();
        });
        HBox elevBox = new HBox(5, loadElevBtn, clearElevBtn);

        biomeMapRowLabel = new Label();
        biomeFileLabel = new Label(I18n.get("planet.map.none"));
        biomeFileLabel.getStyleClass().add("value-label");
        loadBiomeBtn = new Button(I18n.get("planet.map.btn_load"));
        loadBiomeBtn.getStyleClass().add("button-secondary");
        loadBiomeBtn.setOnAction(e -> chooseBiomeMapFile());
        clearBiomeBtn = new Button("❌");
        clearBiomeBtn.getStyleClass().add("button-secondary");
        clearBiomeBtn.setOnAction(e -> {
            customBiomeImage = null;
            biomeFileLabel.setText(I18n.get("planet.map.none"));
            updatePreview();
        });
        HBox biomeBox = new HBox(5, loadBiomeBtn, clearBiomeBtn);

        resourceMapRowLabel = new Label();
        resourceFileLabel = new Label(I18n.get("planet.map.none"));
        resourceFileLabel.getStyleClass().add("value-label");
        loadResourceBtn = new Button(I18n.get("planet.map.btn_load"));
        loadResourceBtn.getStyleClass().add("button-secondary");
        loadResourceBtn.setOnAction(e -> chooseResourceMapFile());
        clearResourceBtn = new Button("❌");
        clearResourceBtn.getStyleClass().add("button-secondary");
        clearResourceBtn.setOnAction(e -> {
            customResourceImage = null;
            resourceFileLabel.setText(I18n.get("planet.map.none"));
            updatePreview();
        });
        HBox resourceBox = new HBox(5, loadResourceBtn, clearResourceBtn);

        fetchOnlineBtn = new Button(I18n.get("planet.map.btn_fetch_online"));
        fetchOnlineBtn.setMaxWidth(Double.MAX_VALUE);
        fetchOnlineBtn.getStyleClass().add("button-secondary");
        fetchOnlineBtn.setOnAction(e -> fetchOnlineSatelliteData());

        exportMapsBtn = new Button(I18n.get("planet.map.btn_export"));
        exportMapsBtn.setMaxWidth(Double.MAX_VALUE);
        exportMapsBtn.getStyleClass().add("button-secondary");
        exportMapsBtn.setOnAction(e -> exportMapsWithWorldFiles());

        mapStatusLabel = new Label();
        mapStatusLabel.getStyleClass().add("value-label");
        mapStatusLabel.setWrapText(true);

        topoControls.getChildren().addAll(
                createControlRow(minAltRowLabel, minAltSlider, "%.0f m", I18n.getOrDefault("planet.tooltip.min_alt", "Altitude minimale absolue")),
                createControlRow(maxAltRowLabel, maxAltSlider, "%.0f m", I18n.getOrDefault("planet.tooltip.max_alt", "Altitude maximale absolue")),
                altRangeLabel,
                createControlRow(waterRowLabel, waterSlider, "%.2f", I18n.getOrDefault("planet.tooltip.water_level", "Seuil d'eau des océans")),
                createControlRow(freqRowLabel, noiseFreqSlider, "%.2f", I18n.getOrDefault("planet.tooltip.noise_freq", "Fréquence du bruit altimétrique")),
                createControlRow(scaleRowLabel, noiseScaleSlider, "%.2f", I18n.getOrDefault("planet.tooltip.noise_scale", "Échelle d'amplitude des reliefs")),
                createControlRow(mapSourceRowLabel, mapSourceCombo, I18n.getOrDefault("planet.tooltip.map_source", "Choix du modèle de corps céleste (USGS / NASA WMS)")),
                createControlRow(elevMapRowLabel, new VBox(3, elevBox, elevFileLabel), I18n.getOrDefault("planet.tooltip.elev_map", "Import d'une carte d'élévation heightmap")),
                createControlRow(biomeMapRowLabel, new VBox(3, biomeBox, biomeFileLabel), "Import d'une carte de biomes"),
                createControlRow(resourceMapRowLabel, new VBox(3, resourceBox, resourceFileLabel), "Import d'une carte géologique"),
                fetchOnlineBtn,
                exportMapsBtn,
                mapStatusLabel
        );

        topoSecHeader = new Label();
        VBox topoSection = createSection(topoSecHeader, topoControls);

        // --- 5. Climate & Ecosystem Section (Macroclimates, Online Fetch, & 3-Map Custom Imports) ---
        VBox climateControls = new VBox(8);

        tempGradSlider = createSlider(0, 100, 40);
        oxygenSlider = createSlider(0, 50, 21);
        co2Slider = createSlider(0, 1000000, 420);
        albedoSlider = createSlider(0.0, 1.0, 0.30);
        atmoPressureSlider = createSlider(0.0, 10.0, 1.0);

        gradRowLabel = new Label();
        oxygenRowLabel = new Label();
        co2RowLabel = new Label();
        albedoRowLabel = new Label();
        atmoPressureRowLabel = new Label();

        climateMapRowLabel = new Label();
        climateFileLabel = new Label(I18n.get("planet.map.none"));
        climateFileLabel.getStyleClass().add("value-label");
        loadClimateBtn = new Button(I18n.get("planet.map.btn_load"));
        loadClimateBtn.getStyleClass().add("button-secondary");
        loadClimateBtn.setOnAction(e -> chooseClimateMapFile());
        clearClimateBtn = new Button("❌");
        clearClimateBtn.getStyleClass().add("button-secondary");
        clearClimateBtn.setOnAction(e -> {
            customClimateImage = null;
            climateFileLabel.setText(I18n.get("planet.map.none"));
            updatePreview();
        });
        HBox climateBox = new HBox(5, loadClimateBtn, clearClimateBtn);

        rainfallMapRowLabel = new Label();
        rainfallFileLabel = new Label(I18n.get("planet.map.none"));
        rainfallFileLabel.getStyleClass().add("value-label");
        loadRainfallBtn = new Button(I18n.get("planet.map.btn_load"));
        loadRainfallBtn.getStyleClass().add("button-secondary");
        loadRainfallBtn.setOnAction(e -> chooseRainfallMapFile());
        clearRainfallBtn = new Button("❌");
        clearRainfallBtn.getStyleClass().add("button-secondary");
        clearRainfallBtn.setOnAction(e -> {
            customRainfallImage = null;
            rainfallFileLabel.setText(I18n.get("planet.map.none"));
            updatePreview();
        });
        HBox rainfallBox = new HBox(5, loadRainfallBtn, clearRainfallBtn);

        seasonalityMapRowLabel = new Label(I18n.getOrDefault("planet.map.seasonality", "Carte de Saisonnalité / Variance :"));
        seasonalityFileLabel = new Label(I18n.get("planet.map.none"));
        seasonalityFileLabel.getStyleClass().add("value-label");
        loadSeasonalityBtn = new Button(I18n.get("planet.map.btn_load"));
        loadSeasonalityBtn.getStyleClass().add("button-secondary");
        loadSeasonalityBtn.setOnAction(e -> chooseSeasonalityMapFile());
        clearSeasonalityBtn = new Button("❌");
        clearSeasonalityBtn.getStyleClass().add("button-secondary");
        clearSeasonalityBtn.setOnAction(e -> {
            customSeasonalityImage = null;
            seasonalityFileLabel.setText(I18n.get("planet.map.none"));
            updatePreview();
        });
        HBox seasonalityBox = new HBox(5, loadSeasonalityBtn, clearSeasonalityBtn);

        fetchOnlineClimateBtn = new Button(I18n.getOrDefault("planet.map.btn_fetch_online_climate", "🌐 Télécharger Climat Satellite NASA/USGS (WMS)"));
        fetchOnlineClimateBtn.setMaxWidth(Double.MAX_VALUE);
        fetchOnlineClimateBtn.getStyleClass().add("button-secondary");
        fetchOnlineClimateBtn.setOnAction(e -> fetchOnlineClimateData());

        climateHelpBtn = new Button(I18n.getOrDefault("planet.map.btn_climate_help", "ℹ️ Spécifications des cartes climatiques"));
        climateHelpBtn.setMaxWidth(Double.MAX_VALUE);
        climateHelpBtn.getStyleClass().add("button-secondary");
        climateHelpBtn.setOnAction(e -> showClimateImportFormatHelp());

        climateControls.getChildren().addAll(
                createControlRow(gradRowLabel, tempGradSlider, "%.1f °C", I18n.getOrDefault("planet.tooltip.temp_grad", "Gradient thermique équateur-pôles")),
                createControlRow(oxygenRowLabel, oxygenSlider, "%.1f %%", I18n.getOrDefault("planet.tooltip.oxygen", "Taux d'oxygène atmosphérique O₂")),
                createControlRow(co2RowLabel, co2Slider, "%.0f ppm", I18n.getOrDefault("planet.tooltip.co2", "Concentration en dioxyde de carbone CO₂ (effet de serre)")),
                createControlRow(albedoRowLabel, albedoSlider, "%.2f", I18n.getOrDefault("planet.tooltip.albedo", "Albédo surfacique de la planète")),
                createControlRow(atmoPressureRowLabel, atmoPressureSlider, "%.2f atm", I18n.getOrDefault("planet.tooltip.atmo_pressure", "Pression de l'atmosphère au sol")),
                createControlRow(climateMapRowLabel, new VBox(3, climateBox, climateFileLabel), I18n.getOrDefault("planet.tooltip.climate_map", "Import d'une carte thermique ou combinée RGB")),
                createControlRow(rainfallMapRowLabel, new VBox(3, rainfallBox, rainfallFileLabel), I18n.getOrDefault("planet.tooltip.rainfall_map", "Import d'une carte de précipitations / humidité")),
                createControlRow(seasonalityMapRowLabel, new VBox(3, seasonalityBox, seasonalityFileLabel), I18n.getOrDefault("planet.tooltip.seasonality_map", "Import d'une carte de variance / saisonnalité thermique")),
                fetchOnlineClimateBtn,
                climateHelpBtn
        );

        climateSecHeader = new Label();
        VBox climateSection = createSection(climateSecHeader, climateControls);

        controlsBox.getChildren().addAll(
                headerLabel,
                presetSection,
                generalSection,
                astroSection,
                topoSection,
                climateSection
        );

        ScrollPane scrollControls = new ScrollPane(controlsBox);
        scrollControls.setFitToWidth(true);
        scrollControls.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // --- Center: Map Preview & Generation ---
        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(10));

        previewTitle = new Label();
        previewTitle.getStyleClass().add("label-header");

        previewCanvas = new Canvas(640, 320);
        previewCanvas.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 0);");
        Tooltip.install(previewCanvas, new Tooltip(I18n.getOrDefault("planet.tooltip.preview", "Aperçu 2D équirectangulaire dynamique")));

        statsLabel = new Label();
        statsLabel.getStyleClass().add("label-stats");

        astroLabel = new Label();
        astroLabel.getStyleClass().add("control-label");

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        generateBtn = new Button();
        generateBtn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-color: #0284c7; -fx-text-fill: white; -fx-background-radius: 6;");
        generateBtn.setOnAction(e -> generatePlanet());
        generateBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.generate", "Générer la planète et la charger dans la simulation")));

        centerBox.getChildren().addAll(previewTitle, previewCanvas, statsLabel, astroLabel, progressBar, generateBtn);

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
        slider.valueProperty().addListener((obs, old, val) -> {
            if (!slider.isValueChanging()) {
                updatePreview();
            }
        });
        slider.setOnMouseReleased(e -> updatePreview());
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
        chooser.setTitle("Load Elevation Heightmap Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customElevImage = new Image(new FileInputStream(file));
                elevFileLabel.setText("📷 " + file.getName());
                updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load elevation map image", ex);
            }
        }
    }

    private void chooseBiomeMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Biome / Ecology Map Image");
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
        chooser.setTitle("Load Geology / Resource Map Image");
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
        chooser.setTitle("Load Climate / Temperature Map Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customClimateImage = new Image(new FileInputStream(file));
                climateFileLabel.setText("🌡️ " + file.getName());
                updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load climate map image", ex);
            }
        }
    }

    private void chooseRainfallMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Precipitation / Moisture Map Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customRainfallImage = new Image(new FileInputStream(file));
                rainfallFileLabel.setText("🌧️ " + file.getName());
                updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load rainfall map image", ex);
            }
        }
    }

    private void chooseSeasonalityMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Seasonality / Temperature Amplitude Map Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customSeasonalityImage = new Image(new FileInputStream(file));
                seasonalityFileLabel.setText("🍂 " + file.getName());
                updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load seasonality map image", ex);
            }
        }
    }

    private void showClimateImportFormatHelp() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Spécifications des Cartes Climatiques");
        dialog.setHeaderText("Formats d'images supportés pour l'importation du Climat");
        dialog.setContentText(
                "Vous pouvez importer des cartes climatiques sous forme d'images PNG/JPEG au ratio 2:1 (ex: 2048x1024 pixels en projection équirectangulaire) :\n\n" +
                "1. MÉTHODE À TROIS CARTES SÉPARÉES (Parité intégrale) :\n" +
                "   • Carte de Températures (Thermique) : Niveau de gris où Noir (0) = -50°C et Blanc (255) = +50°C.\n" +
                "   • Carte de Précipitations (Humidité) : Niveau de gris où Noir (0) = 0 mm/an et Blanc (255) = 3000 mm/an.\n" +
                "   • Carte de Saisonnalité (Variance) : Niveau de gris où Noir (0) = 0°C et Blanc (255) = 50°C d'amplitude annuelle.\n\n" +
                "2. MÉTHODE À CARTE UNIQUE COMBINÉE (RGB) :\n" +
                "   • Canal Rouge (R) = Température (-50°C à +50°C)\n" +
                "   • Canal Vert (V) = Précipitations (0 à 3000 mm/an)\n" +
                "   • Canal Bleu (B) = Saisonnalité / Variance (0 à 50°C)\n\n" +
                "3. TÉLÉCHARGEMENT SATELLITE EN LIGNE (WMS) :\n" +
                "   • Le bouton '🌐 Télécharger Climat Satellite' permet de récupérer directement les cartes thermiques MODIS et pluviométriques GPM de la NASA via leurs services WMS officiels !"
        );
        dialog.showAndWait();
    }

    private void applyMapSourcePreset(String sourceKey) {
        if ("none".equals(sourceKey)) {
            customElevImage = null;
            customBiomeImage = null;
            customResourceImage = null;
            customClimateImage = null;
            customRainfallImage = null;
            customSeasonalityImage = null;
            elevFileLabel.setText(I18n.get("planet.map.none"));
            biomeFileLabel.setText(I18n.get("planet.map.none"));
            resourceFileLabel.setText(I18n.get("planet.map.none"));
            climateFileLabel.setText(I18n.get("planet.map.none"));
            rainfallFileLabel.setText(I18n.get("planet.map.none"));
            seasonalityFileLabel.setText(I18n.get("planet.map.none"));
            updatePreview();
            return;
        }

        if ("earth".equals(sourceKey)) {
            try (var elevStream = getClass().getResourceAsStream("/maps/earth_elevation.png");
                 var biomeStream = getClass().getResourceAsStream("/maps/earth_biomes.png")) {
                if (elevStream != null) customElevImage = new Image(elevStream);
                if (biomeStream != null) customBiomeImage = new Image(biomeStream);
                elevFileLabel.setText("📷 Earth Elevation Map");
                biomeFileLabel.setText("🌿 Earth Biome Map");
                resourceFileLabel.setText(I18n.get("planet.map.none"));
                climateFileLabel.setText(I18n.get("planet.map.none"));
                rainfallFileLabel.setText(I18n.get("planet.map.none"));
                seasonalityFileLabel.setText(I18n.get("planet.map.none"));
            } catch (Exception e) {
                logger.warn("Could not load internal Earth maps", e);
            }
            applyPreset(PlanetPreset.EARTH_LIKE);
            return;
        }

        if ("mars".equals(sourceKey)) {
            customElevImage = null;
            customBiomeImage = null;
            customResourceImage = null;
            customClimateImage = null;
            customRainfallImage = null;
            customSeasonalityImage = null;
            bodyTypeCombo.setValue("planet");
            radiusSlider.setValue(3389);
            dayLengthSlider.setValue(24.6);
            axialTiltSlider.setValue(25.2);
            yearLengthSlider.setValue(687);
            distanceSunSlider.setValue(1.52);
            solarLumSlider.setValue(1.0);
            avgTempSlider.setValue(-63);
            minAltSlider.setValue(-8000);
            maxAltSlider.setValue(21229); // Olympus Mons
            waterSlider.setValue(-0.5); // No ocean
            elevFileLabel.setText("📷 Mars MOLA Heightmap (USGS WMS)");
            biomeFileLabel.setText(I18n.get("planet.map.none"));
            resourceFileLabel.setText(I18n.get("planet.map.none"));
            climateFileLabel.setText(I18n.get("planet.map.none"));
            rainfallFileLabel.setText(I18n.get("planet.map.none"));
            seasonalityFileLabel.setText(I18n.get("planet.map.none"));
            updatePreview();
            return;
        }

        if ("venus".equals(sourceKey)) {
            customElevImage = null;
            customBiomeImage = null;
            customResourceImage = null;
            customClimateImage = null;
            customRainfallImage = null;
            customSeasonalityImage = null;
            bodyTypeCombo.setValue("planet");
            radiusSlider.setValue(6051);
            dayLengthSlider.setValue(2802);
            axialTiltSlider.setValue(177.3);
            yearLengthSlider.setValue(225);
            distanceSunSlider.setValue(0.72);
            solarLumSlider.setValue(1.0);
            avgTempSlider.setValue(464);
            minAltSlider.setValue(-3000);
            maxAltSlider.setValue(11000); // Maxwell Montes
            waterSlider.setValue(-0.5);
            elevFileLabel.setText("📷 Venus Magellan Topography (USGS WMS)");
            biomeFileLabel.setText(I18n.get("planet.map.none"));
            resourceFileLabel.setText(I18n.get("planet.map.none"));
            climateFileLabel.setText(I18n.get("planet.map.none"));
            rainfallFileLabel.setText(I18n.get("planet.map.none"));
            seasonalityFileLabel.setText(I18n.get("planet.map.none"));
            updatePreview();
            return;
        }

        if ("moon".equals(sourceKey)) {
            customElevImage = null;
            customBiomeImage = null;
            customResourceImage = null;
            customClimateImage = null;
            customRainfallImage = null;
            customSeasonalityImage = null;
            bodyTypeCombo.setValue("satellite");
            radiusSlider.setValue(1737);
            dayLengthSlider.setValue(708);
            axialTiltSlider.setValue(1.5);
            yearLengthSlider.setValue(365);
            distanceSunSlider.setValue(1.0);
            solarLumSlider.setValue(1.0);
            avgTempSlider.setValue(-20);
            minAltSlider.setValue(-9000);
            maxAltSlider.setValue(10700);
            waterSlider.setValue(-0.5);
            elevFileLabel.setText("📷 Moon LRO Topography (USGS WMS)");
            biomeFileLabel.setText(I18n.get("planet.map.none"));
            resourceFileLabel.setText(I18n.get("planet.map.none"));
            climateFileLabel.setText(I18n.get("planet.map.none"));
            rainfallFileLabel.setText(I18n.get("planet.map.none"));
            seasonalityFileLabel.setText(I18n.get("planet.map.none"));
            updatePreview();
        }
    }

    private void fetchOnlineSatelliteData() {
        String sourceKey = mapSourceCombo.getValue();
        OnlineMapService.CelestialBody body = switch (sourceKey) {
            case "mars" -> OnlineMapService.CelestialBody.MARS;
            case "moon" -> OnlineMapService.CelestialBody.MOON;
            case "venus" -> OnlineMapService.CelestialBody.VENUS;
            default -> OnlineMapService.CelestialBody.EARTH;
        };

        mapStatusLabel.setText(I18n.get("planet.map.status_fetching"));

        onlineMapService.fetchElevationMapAsync(body).thenAccept(img -> {
            javafx.application.Platform.runLater(() -> {
                if (img != null) {
                    customElevImage = img;
                    elevFileLabel.setText("🌐 " + body.getName() + " WMS Elevation");
                    mapStatusLabel.setText(I18n.get("planet.map.status_success"));
                    updatePreview();
                } else {
                    mapStatusLabel.setText(I18n.get("planet.map.status_error"));
                }
            });
        });

        if (body.getBiomeWmsUrl() != null) {
            onlineMapService.fetchBiomeMapAsync(body).thenAccept(img -> {
                javafx.application.Platform.runLater(() -> {
                    if (img != null) {
                        customBiomeImage = img;
                        biomeFileLabel.setText("🌐 " + body.getName() + " WMS Biomes");
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

        mapStatusLabel.setText("🌐 Téléchargement des cartes climatiques WMS (NASA MODIS & GPM)...");

        onlineMapService.fetchClimateMapAsync(body).thenAccept(img -> {
            javafx.application.Platform.runLater(() -> {
                if (img != null) {
                    customClimateImage = img;
                    climateFileLabel.setText("🌐 " + body.getName() + " MODIS Thermal WMS");
                    mapStatusLabel.setText(I18n.get("planet.map.status_success"));
                    updatePreview();
                }
            });
        });

        onlineMapService.fetchRainfallMapAsync(body).thenAccept(img -> {
            javafx.application.Platform.runLater(() -> {
                if (img != null) {
                    customRainfallImage = img;
                    rainfallFileLabel.setText("🌐 " + body.getName() + " GPM Rainfall WMS");
                    updatePreview();
                }
            });
        });
    }

    private void exportMapsWithWorldFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Map Image with ESRI World File (.tfw)");
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

        // Adjust average temperature dynamically if not manually locking
        if (!isUpdatingFromPreset) {
            double alb = albedoSlider != null ? albedoSlider.getValue() : 0.30;
            double atmoP = atmoPressureSlider != null ? atmoPressureSlider.getValue() : 1.0;
            double co2 = co2Slider != null ? co2Slider.getValue() : 420.0;
            double greenhouseBoost = 15.0 * Math.sqrt(Math.max(0.1, atmoP)) + (co2 / 1000.0);
            double calcTempC = 278.5 * Math.pow((l * (1.0 - alb)) / (d * d), 0.25) - 273.15 + greenhouseBoost;
            calcTempC = Math.max(-200.0, Math.min(500.0, calcTempC));
            avgTempSlider.setValue(calcTempC);
        }
    }

    private void updateAltRangeDisplay() {
        double min = minAltSlider.getValue();
        double max = maxAltSlider.getValue();
        altRangeLabel.setText(String.format("%s: %.0f m", I18n.get("planet.param.alt_range"), max - min));
    }

    private void applyPreset(PlanetPreset p) {
        if (p == null) return;
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

        isUpdatingFromPreset = false;
        updatePreview();
    }

    public PlanetPreset buildPresetFromUI() {
        long seed = 12345;
        try {
            seed = Long.parseLong(seedField.getText());
        } catch (NumberFormatException ignored) {}

        String presetName = presetCombo.getValue() != null ? presetCombo.getValue().name() : "Custom Planet";
        boolean isSat = "satellite".equals(bodyTypeCombo.getValue());

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
                co2Slider.getValue()
        );
    }

    private void updatePreview() {
        if (previewCanvas == null) return;

        PlanetPreset preset = buildPresetFromUI();
        GraphicsContext gc = previewCanvas.getGraphicsContext2D();
        PixelWriter pw = gc.getPixelWriter();

        int w = (int) previewCanvas.getWidth();
        int h = (int) previewCanvas.getHeight();

        PixelReader elevReader = customElevImage != null ? customElevImage.getPixelReader() : null;
        PixelReader biomeReader = customBiomeImage != null ? customBiomeImage.getPixelReader() : null;
        PixelReader climateReader = customClimateImage != null ? customClimateImage.getPixelReader() : null;
        PixelReader rainfallReader = customRainfallImage != null ? customRainfallImage.getPixelReader() : null;
        PixelReader seasonalityReader = customSeasonalityImage != null ? customSeasonalityImage.getPixelReader() : null;

        double wElev = customElevImage != null ? customElevImage.getWidth() : 0;
        double hElev = customElevImage != null ? customElevImage.getHeight() : 0;
        double wBiome = customBiomeImage != null ? customBiomeImage.getWidth() : 0;
        double hBiome = customBiomeImage != null ? customBiomeImage.getHeight() : 0;
        double wClimate = customClimateImage != null ? customClimateImage.getWidth() : 0;
        double hClimate = customClimateImage != null ? customClimateImage.getHeight() : 0;
        double wRain = customRainfallImage != null ? customRainfallImage.getWidth() : 0;
        double hRain = customRainfallImage != null ? customRainfallImage.getHeight() : 0;
        double wSeason = customSeasonalityImage != null ? customSeasonalityImage.getWidth() : 0;
        double hSeason = customSeasonalityImage != null ? customSeasonalityImage.getHeight() : 0;

        int oceanCount = 0;

        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y / (double) h) * 180.0;
            for (int x = 0; x < w; x++) {
                double lng = (x / (double) w) * 360.0 - 180.0;

                PlanetPoint p = generator.getPlanetPoint(lat, lng, preset);
                Biome cellBiome = p.biome();

                // Override with custom climate/biome if available
                if (biomeReader != null) {
                    double u = (lng + 180.0) / 360.0;
                    double v = (90.0 - lat) / 180.0;
                    int bx = (int) Math.min(u * wBiome, wBiome - 1);
                    int by = (int) Math.min(v * hBiome, hBiome - 1);
                    cellBiome = mapLoader.matchBiomeColor(biomeReader.getColor(bx, by));
                } else if (climateReader != null) {
                    double u = (lng + 180.0) / 360.0;
                    double v = (90.0 - lat) / 180.0;
                    int cx = (int) Math.min(u * wClimate, wClimate - 1);
                    int cy = (int) Math.min(v * hClimate, hClimate - 1);
                    Color c = climateReader.getColor(cx, cy);
                    double customTemp = -50.0 + c.getRed() * 100.0;
                    cellBiome = (p.elevation() < preset.waterLevel()) ? Biome.OCEAN : (customTemp < 0 ? Biome.SNOW : (customTemp > 30 ? Biome.DESERT : Biome.PLAINS));
                } else if (elevReader != null) {
                    double u = (lng + 180.0) / 360.0;
                    double v = (90.0 - lat) / 180.0;
                    int ex = (int) Math.min(u * wElev, wElev - 1);
                    int ey = (int) Math.min(v * hElev, hElev - 1);
                    double brightness = elevReader.getColor(ex, ey).getBrightness();
                    double altMeters = preset.minAltitudeMeters() + brightness * (preset.maxAltitudeMeters() - preset.minAltitudeMeters());
                    double normAlt = (altMeters - preset.minAltitudeMeters()) / (preset.maxAltitudeMeters() - preset.minAltitudeMeters()) * 2.0 - 1.0;
                    cellBiome = (altMeters < 0) ? Biome.OCEAN : (normAlt > 0.6 ? Biome.MOUNTAINS : Biome.PLAINS);
                }

                if (cellBiome == Biome.OCEAN || cellBiome == Biome.DEEP_OCEAN) {
                    oceanCount++;
                }

                pw.setColor(x, y, getBiomeColor(cellBiome));
            }
        }

        int total = w * h;
        int oceanPct = (oceanCount * 100) / total;
        statsLabel.setText(I18n.get("planet.stats.ocean_land", oceanPct, 100 - oceanPct));
        astroLabel.setText(String.format("%s: %.0f km | %s: %.1fh | %s: %.1f° | %s: %.0f d | %s: %.2f AU | %s: %.1f°C",
                preset.isSatellite() ? "🌕 Lune Rayon" : "📐 Rayon",
                preset.radiusKm(),
                I18n.getOrDefault("planet.short.day", "Jour"),
                preset.dayLengthHours(),
                I18n.getOrDefault("planet.short.tilt", "Tilt"),
                preset.axialTiltDegrees(),
                I18n.getOrDefault("planet.short.year", "Année"),
                preset.yearLengthDays(),
                I18n.getOrDefault("planet.short.dist", "Dist"),
                preset.distanceToSunAU(),
                I18n.getOrDefault("planet.short.temp", "Temp"),
                preset.averageTempC()));
    }

    private Color getBiomeColor(Biome biome) {
        return switch (biome) {
            case OCEAN -> Color.rgb(25, 60, 160);
            case DEEP_OCEAN -> Color.rgb(10, 30, 110);
            case BEACH -> Color.rgb(238, 214, 175);
            case DESERT -> Color.rgb(220, 170, 120);
            case PLAINS -> Color.rgb(110, 210, 80);
            case FOREST -> Color.rgb(34, 139, 34);
            case JUNGLE -> Color.rgb(0, 100, 40);
            case MOUNTAINS -> Color.rgb(140, 130, 130);
            case HILLS -> Color.rgb(160, 160, 110);
            case TUNDRA -> Color.rgb(200, 200, 170);
            case SNOW -> Color.rgb(245, 245, 250);
            default -> Color.BLACK;
        };
    }

    public void updateTexts() {
        headerLabel.setText(I18n.get("planet.section.header"));
        presetsSecHeader.setText(I18n.get("planet.section.presets"));
        generalSecHeader.setText(I18n.getOrDefault("planet.section.general", "PARAMÈTRES GÉNÉRAUX & RÉSOLUTION"));
        astroSecHeader.setText(I18n.get("planet.section.astro"));
        topoSecHeader.setText(I18n.get("planet.section.topo"));
        climateSecHeader.setText(I18n.get("planet.section.climate"));
        previewTitle.setText(I18n.get("planet.preview.title"));
        generateBtn.setText(I18n.get("planet.btn.generate"));

        if (presetRowLabel != null) presetRowLabel.setText(I18n.get("planet.preset"));
        if (bodyTypeRowLabel != null) bodyTypeRowLabel.setText(I18n.getOrDefault("planet.param.body_type", "Type de corps céleste :"));
        if (parentMassRowLabel != null) parentMassRowLabel.setText(I18n.getOrDefault("planet.param.parent_mass", "Masse planète hôte (M⊕)"));
        if (orbitDistanceParentRowLabel != null) orbitDistanceParentRowLabel.setText(I18n.getOrDefault("planet.param.parent_dist", "Distance orbitale hôte (km)"));

        if (mapSourceRowLabel != null) mapSourceRowLabel.setText(I18n.get("planet.map.preset_body"));
        if (elevMapRowLabel != null) elevMapRowLabel.setText(I18n.get("planet.map.elevation"));
        if (biomeMapRowLabel != null) biomeMapRowLabel.setText(I18n.get("planet.map.biomes"));
        if (resourceMapRowLabel != null) resourceMapRowLabel.setText(I18n.get("planet.map.resources"));
        if (climateMapRowLabel != null) climateMapRowLabel.setText(I18n.getOrDefault("planet.map.climate", "Carte Climatique (Thermique / RGB) :"));
        if (rainfallMapRowLabel != null) rainfallMapRowLabel.setText(I18n.getOrDefault("planet.map.rainfall", "Carte de Précipitations (Humidité) :"));
        if (seasonalityMapRowLabel != null) seasonalityMapRowLabel.setText(I18n.getOrDefault("planet.map.seasonality", "Carte de Saisonnalité / Variance :"));

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

        if (oxygenRowLabel != null) oxygenRowLabel.setText(I18n.getOrDefault("planet.param.oxygen", "Taux d'Oxygène O₂ (%)"));
        if (co2RowLabel != null) co2RowLabel.setText(I18n.getOrDefault("planet.param.co2", "Dioxyde de Carbone CO₂ (ppm)"));
        if (albedoRowLabel != null) albedoRowLabel.setText(I18n.getOrDefault("planet.param.albedo", "Albédo Planétaire"));
        if (atmoPressureRowLabel != null) atmoPressureRowLabel.setText(I18n.getOrDefault("planet.param.atmo_pressure", "Pression Atmosphérique (atm)"));

        calculateStellarIrradiance();
        updateAltRangeDisplay();
        updateTooltips();
        updatePreview();
    }

    private void updateTooltips() {
        if (fetchOnlineBtn != null) fetchOnlineBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.fetch_online", "Télécharger cartes satellites NASA/USGS WMS")));
        if (fetchOnlineClimateBtn != null) fetchOnlineClimateBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.fetch_online_climate", "Télécharger cartes thermiques MODIS et pluviométriques GPM NASA (WMS)")));
        if (exportMapsBtn != null) exportMapsBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.export_map", "Exporter les cartes en PNG avec fichier de calage ESRI World File (.tfw)")));
        if (loadElevBtn != null) loadElevBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.elev_load", "Charger une carte d'élévation heightmap externe")));
        if (clearElevBtn != null) clearElevBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.elev_clear", "Effacer l'image d'élévation")));
        if (loadClimateBtn != null) loadClimateBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.climate_load", "Charger une carte thermique/climatique externe")));
        if (clearClimateBtn != null) clearClimateBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.climate_clear", "Effacer la carte climatique")));
        if (loadRainfallBtn != null) loadRainfallBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.rainfall_load", "Charger une carte de précipitations/humidité externe")));
        if (clearRainfallBtn != null) clearRainfallBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.rainfall_clear", "Effacer la carte de précipitations")));
        if (loadSeasonalityBtn != null) loadSeasonalityBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.seasonality_load", "Charger une carte de variabilité saisonnière externe")));
        if (clearSeasonalityBtn != null) clearSeasonalityBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.seasonality_clear", "Effacer la carte de variabilité saisonnière")));
        if (randSeedBtn != null) randSeedBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.seed_rand", "Générer une nouvelle graine aléatoire")));
    }

    private void generatePlanet() {
        progressBar.setVisible(true);
        progressBar.setProgress(-1);

        new Thread(() -> {
            try {
                PlanetPreset preset = buildPresetFromUI();
                logger.info("Generating full planetary grid for preset: {}", preset.name());
                List<H3Cell> cells = generator.generatePlanet(preset);

                // Apply custom imported maps if provided
                if (customElevImage != null || customBiomeImage != null || customResourceImage != null || customClimateImage != null || customRainfallImage != null || customSeasonalityImage != null) {
                    mapLoader.mapImagesToCells(cells, customElevImage, customBiomeImage, customResourceImage, customClimateImage, customRainfallImage, customSeasonalityImage, preset.minAltitudeMeters(), preset.maxAltitudeMeters());
                }

                javafx.application.Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    if (onPlanetGeneratedCallback != null) {
                        onPlanetGeneratedCallback.accept(cells);
                    }
                });
            } catch (Exception e) {
                logger.error("Error generating planet", e);
                javafx.application.Platform.runLater(() -> progressBar.setVisible(false));
            }
        }).start();
    }
}
