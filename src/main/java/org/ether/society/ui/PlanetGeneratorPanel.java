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
import org.ether.society.procedural.ElevationPreset;
import org.ether.society.procedural.ClimatePreset;
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
 * climate customization, and custom map image import (Elevation, Biomes, Geology/Resources).
 * 
 * @author Silvere Martin-Michiellot
 * @version 2.1.0
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

    // Online & Export Buttons & Labels
    private Button fetchOnlineBtn;
    private Button exportMapsBtn;
    private Label mapStatusLabel;

    // Controls
    private ComboBox<PlanetPreset> presetCombo;
    private ComboBox<ElevationPreset> elevationPresetCombo;
    private ComboBox<ClimatePreset> climatePresetCombo;
    private ComboBox<String> mapSourceCombo;
    private TextField seedField;
    private Slider radiusSlider;
    private Slider dayLengthSlider;
    private Slider axialTiltSlider;
    private Slider yearLengthSlider;
    private Slider distanceSunSlider;
    private Slider solarLumSlider;
    private Slider minAltSlider;
    private Slider maxAltSlider;
    private Slider avgTempSlider;
    private Slider noiseFreqSlider;
    private Slider noiseScaleSlider;
    private Slider waterSlider;
    private Slider tempGradSlider;
    private ComboBox<Integer> resolutionCombo;

    // Map Load Buttons & Labels
    private Label elevFileLabel;
    private Label biomeFileLabel;
    private Label resourceFileLabel;
    private Button loadElevBtn;
    private Button clearElevBtn;
    private Button loadBiomeBtn;
    private Button clearBiomeBtn;
    private Button loadResourceBtn;
    private Button clearResourceBtn;

    // UI Labels for i18n
    private Label headerLabel;
    private Label presetsSecHeader;
    private Label customMapsSecHeader;
    private Label astroSecHeader;
    private Label topoSecHeader;
    private Label previewTitle;
    private Label statsLabel;
    private Label astroLabel;
    private Label irradianceLabel;
    private Label altRangeLabel;
    private Button generateBtn;
    private Button saveJsonBtn;
    private Button loadJsonBtn;
    private ProgressBar progressBar;

    // Labels for control rows
    private Label presetRowLabel;
    private Label elevationRowLabel;
    private Label climateRowLabel;
    private Label mapSourceRowLabel;
    private Label elevMapRowLabel;
    private Label biomeMapRowLabel;
    private Label resourceMapRowLabel;
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
        controlsBox.setPrefWidth(350);
        controlsBox.setPadding(new Insets(10));

        headerLabel = new Label();
        headerLabel.getStyleClass().add("label-title");

        // 1. Standardized Preset Control Bar
        PresetControlBar<PlanetPreset> topPresetBar = new PresetControlBar<>("Preset");
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
                        current.noiseScale(), current.waterLevel(), current.temperatureGradient()
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

        elevationPresetCombo = new ComboBox<>();
        elevationPresetCombo.getItems().addAll(ElevationPreset.values());
        elevationPresetCombo.setMaxWidth(Double.MAX_VALUE);
        elevationPresetCombo.setOnAction(e -> {
            ElevationPreset ep = elevationPresetCombo.getValue();
            if (ep != null && !isUpdatingFromPreset) {
                waterSlider.setValue(ep.getWaterCoverage() - 0.5);
                noiseScaleSlider.setValue(ep.getRoughness() * 2.0);
                maxAltSlider.setValue(ep.getMaxElevationMeters());
                updatePreview();
            }
        });

        climatePresetCombo = new ComboBox<>();
        climatePresetCombo.getItems().addAll(ClimatePreset.values());
        climatePresetCombo.setMaxWidth(Double.MAX_VALUE);
        climatePresetCombo.setOnAction(e -> {
            ClimatePreset cp = climatePresetCombo.getValue();
            if (cp != null && !isUpdatingFromPreset) {
                avgTempSlider.setValue(cp.getAvgTemperatureCelsius());
                updatePreview();
            }
        });

        elevationRowLabel = new Label();
        climateRowLabel = new Label();

        presetsSecHeader = new Label();
        VBox presetSection = createSection(presetsSecHeader, new VBox(6,
                topPresetBar,
                createControlRow(elevationRowLabel, elevationPresetCombo),
                createControlRow(climateRowLabel, climatePresetCombo)
        ));

        // 2. Custom Maps Import Section (Elevation, Biomes, Geology & Resources)
        VBox customMapsControls = new VBox(8);
        customMapsSecHeader = new Label();

        mapSourceRowLabel = new Label();
        mapSourceCombo = new ComboBox<>();
        mapSourceCombo.getItems().addAll(
                "none",
                "earth",
                "mars",
                "venus",
                "moon"
        );
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

        customMapsControls.getChildren().addAll(
                createControlRow(mapSourceRowLabel, mapSourceCombo),
                createControlRow(elevMapRowLabel, new VBox(3, elevBox, elevFileLabel)),
                createControlRow(biomeMapRowLabel, new VBox(3, biomeBox, biomeFileLabel)),
                createControlRow(resourceMapRowLabel, new VBox(3, resourceBox, resourceFileLabel)),
                fetchOnlineBtn,
                exportMapsBtn,
                mapStatusLabel
        );

        VBox customMapsSection = createSection(customMapsSecHeader, customMapsControls);

        // 3. Astronomical & Physical Controls
        VBox astroControls = new VBox(8);

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

        resolutionCombo = new ComboBox<>();
        resolutionCombo.getItems().addAll(5, 6, 7, 8);
        resolutionCombo.setValue(6);
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
        resolutionCombo.setMaxWidth(Double.MAX_VALUE);

        radiusRowLabel = new Label();
        resRowLabel = new Label();
        dayRowLabel = new Label();
        tiltRowLabel = new Label();
        yearRowLabel = new Label();
        distRowLabel = new Label();
        lumRowLabel = new Label();
        tempRowLabel = new Label();

        irradianceLabel = new Label();
        irradianceLabel.getStyleClass().add("value-label");

        astroControls.getChildren().addAll(
                createControlRow(radiusRowLabel, radiusSlider, "%.0f km"),
                createControlRow(resRowLabel, resolutionCombo),
                createControlRow(dayRowLabel, dayLengthSlider, "%.1f h"),
                createControlRow(tiltRowLabel, axialTiltSlider, "%.1f°"),
                createControlRow(yearRowLabel, yearLengthSlider, "%.0f d"),
                createControlRow(distRowLabel, distanceSunSlider, "%.2f AU"),
                createControlRow(lumRowLabel, solarLumSlider, "%.2f L☉"),
                irradianceLabel,
                createControlRow(tempRowLabel, avgTempSlider, "%.1f °C")
        );

        astroSecHeader = new Label();
        VBox astroSection = createSection(astroSecHeader, astroControls);

        // 4. Topography & Altitudes Section
        VBox topoControls = new VBox(8);

        seedField = new TextField("12345");
        seedField.textProperty().addListener((obs, old, val) -> updatePreview());
        Button randSeedBtn = new Button("🎲");
        randSeedBtn.setOnAction(e -> {
            seedField.setText(String.valueOf(new Random().nextLong(1000000)));
            updatePreview();
        });
        HBox seedBox = new HBox(5, seedField, randSeedBtn);
        HBox.setHgrow(seedField, Priority.ALWAYS);

        minAltSlider = createSlider(-15000, -500, -11000);
        maxAltSlider = createSlider(500, 25000, 8848);
        minAltSlider.valueProperty().addListener((obs, old, val) -> updateAltRangeDisplay());
        maxAltSlider.valueProperty().addListener((obs, old, val) -> updateAltRangeDisplay());

        waterSlider = createSlider(-0.5, 1.0, 0.0);
        noiseFreqSlider = createSlider(0.1, 2.0, 1.0);
        noiseScaleSlider = createSlider(0.5, 3.0, 1.0);
        tempGradSlider = createSlider(0, 100, 40);

        seedRowLabel = new Label();
        minAltRowLabel = new Label();
        maxAltRowLabel = new Label();
        waterRowLabel = new Label();
        freqRowLabel = new Label();
        scaleRowLabel = new Label();
        gradRowLabel = new Label();

        altRangeLabel = new Label();
        altRangeLabel.getStyleClass().add("value-label");
        updateAltRangeDisplay();

        topoControls.getChildren().addAll(
                createControlRow(seedRowLabel, seedBox),
                createControlRow(minAltRowLabel, minAltSlider, "%.0f m"),
                createControlRow(maxAltRowLabel, maxAltSlider, "%.0f m"),
                altRangeLabel,
                createControlRow(waterRowLabel, waterSlider, "%.2f"),
                createControlRow(freqRowLabel, noiseFreqSlider, "%.2f"),
                createControlRow(scaleRowLabel, noiseScaleSlider, "%.2f"),
                createControlRow(gradRowLabel, tempGradSlider, "%.1f °C")
        );

        topoSecHeader = new Label();
        VBox topoSection = createSection(topoSecHeader, topoControls);

        controlsBox.getChildren().addAll(headerLabel, presetSection, customMapsSection, astroSection, topoSection);

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

        centerBox.getChildren().addAll(previewTitle, previewCanvas, statsLabel, astroLabel, progressBar, generateBtn);

        setLeft(scrollControls);
        setCenter(centerBox);

        // Apply Earth as default preset
        presetCombo.getSelectionModel().select(PlanetPreset.EARTH_LIKE);
        applyPreset(PlanetPreset.EARTH_LIKE);
    }

    private VBox createSection(Label header, VBox content) {
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

    private VBox createControlRow(Label label, javafx.scene.Node control) {
        label.getStyleClass().add("control-label");
        return new VBox(4, label, control);
    }

    private VBox createControlRow(Label label, Slider slider, String formatPattern) {
        label.getStyleClass().add("control-label");
        Label valLabel = new Label(String.format(formatPattern, slider.getValue()));
        valLabel.getStyleClass().add("value-label");
        
        slider.valueProperty().addListener((obs, old, val) -> 
            valLabel.setText(String.format(formatPattern, val.doubleValue()))
        );

        HBox header = new HBox(label, new Pane(), valLabel);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        return new VBox(3, header, slider);
    }

    private void chooseElevMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Elevation Map Image");
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

    private void applyMapSourcePreset(String sourceKey) {
        if ("none".equals(sourceKey)) {
            customElevImage = null;
            customBiomeImage = null;
            customResourceImage = null;
            elevFileLabel.setText(I18n.get("planet.map.none"));
            biomeFileLabel.setText(I18n.get("planet.map.none"));
            resourceFileLabel.setText(I18n.get("planet.map.none"));
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
            elevFileLabel.setText("📷 Mars MOLA Heightmap");
            biomeFileLabel.setText(I18n.get("planet.map.none"));
            resourceFileLabel.setText(I18n.get("planet.map.none"));
            updatePreview();
            return;
        }

        if ("venus".equals(sourceKey)) {
            customElevImage = null;
            customBiomeImage = null;
            customResourceImage = null;
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
            elevFileLabel.setText("📷 Venus Magellan Topography");
            biomeFileLabel.setText(I18n.get("planet.map.none"));
            resourceFileLabel.setText(I18n.get("planet.map.none"));
            updatePreview();
            return;
        }

        if ("moon".equals(sourceKey)) {
            customElevImage = null;
            customBiomeImage = null;
            customResourceImage = null;
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
            elevFileLabel.setText("📷 Moon LRO Topography");
            biomeFileLabel.setText(I18n.get("planet.map.none"));
            resourceFileLabel.setText(I18n.get("planet.map.none"));
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
            double calcTempC = 278.5 * Math.pow(l / (d * d), 0.25) - 273.15 + 15.0; // base greenhouse +15°C
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

        calculateStellarIrradiance();
        updateAltRangeDisplay();

        isUpdatingFromPreset = false;
        updatePreview();
    }

    private PlanetPreset buildPresetFromUI() {
        long seed = 12345;
        try {
            seed = Long.parseLong(seedField.getText());
        } catch (NumberFormatException ignored) {}

        String presetName = presetCombo.getValue() != null ? presetCombo.getValue().name() : "Custom Planet";

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
                tempGradSlider.getValue()
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

        double wElev = customElevImage != null ? customElevImage.getWidth() : 0;
        double hElev = customElevImage != null ? customElevImage.getHeight() : 0;
        double wBiome = customBiomeImage != null ? customBiomeImage.getWidth() : 0;
        double hBiome = customBiomeImage != null ? customBiomeImage.getHeight() : 0;

        int oceanCount = 0;

        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y / (double) h) * 180.0;
            for (int x = 0; x < w; x++) {
                double lng = (x / (double) w) * 360.0 - 180.0;

                PlanetPoint p = generator.getPlanetPoint(lat, lng, preset);
                Biome cellBiome = p.biome();

                // Override with custom biome image if available
                if (biomeReader != null) {
                    double u = (lng + 180.0) / 360.0;
                    double v = (90.0 - lat) / 180.0;
                    int bx = (int) Math.min(u * wBiome, wBiome - 1);
                    int by = (int) Math.min(v * hBiome, hBiome - 1);
                    cellBiome = mapLoader.matchBiomeColor(biomeReader.getColor(bx, by));
                } else if (elevReader != null) {
                    // Sample heightmap
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
        astroLabel.setText(String.format("📐 Radius: %.0f km | ⏱️ Day: %.1fh | 🔄 Tilt: %.1f° | 📅 Year: %.0f d | ☀️ Dist: %.2f AU | 🌡️ Temp: %.1f°C",
                preset.radiusKm(), preset.dayLengthHours(), preset.axialTiltDegrees(), preset.yearLengthDays(), preset.distanceToSunAU(), preset.averageTempC()));
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
        customMapsSecHeader.setText(I18n.get("planet.section.custom_maps"));
        astroSecHeader.setText(I18n.get("planet.section.astro"));
        topoSecHeader.setText(I18n.get("planet.section.topo"));
        previewTitle.setText(I18n.get("planet.preview.title"));
        generateBtn.setText(I18n.get("planet.btn.generate"));

        if (presetRowLabel != null) presetRowLabel.setText(I18n.get("planet.preset"));
        if (elevationRowLabel != null) elevationRowLabel.setText(I18n.get("planet.preset.elevation"));
        if (climateRowLabel != null) climateRowLabel.setText(I18n.get("planet.preset.climate"));
        if (saveJsonBtn != null) saveJsonBtn.setText(I18n.get("planet.btn.save_json"));
        if (loadJsonBtn != null) loadJsonBtn.setText(I18n.get("planet.btn.load_json"));

        if (mapSourceRowLabel != null) mapSourceRowLabel.setText(I18n.get("planet.map.preset_body"));
        if (elevMapRowLabel != null) elevMapRowLabel.setText(I18n.get("planet.map.elevation"));
        if (biomeMapRowLabel != null) biomeMapRowLabel.setText(I18n.get("planet.map.biomes"));
        if (resourceMapRowLabel != null) resourceMapRowLabel.setText(I18n.get("planet.map.resources"));
        if (loadElevBtn != null) loadElevBtn.setText(I18n.get("planet.map.btn_load"));
        if (loadBiomeBtn != null) loadBiomeBtn.setText(I18n.get("planet.map.btn_load"));
        if (loadResourceBtn != null) loadResourceBtn.setText(I18n.get("planet.map.btn_load"));

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

        calculateStellarIrradiance();
        updateAltRangeDisplay();
        updatePreview();
    }

    private void savePresetJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Planet Preset");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Preset Files", "*.json"));
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                PlanetPreset preset = buildPresetFromUI();
                mapper.writeValue(file, preset);
                logger.info("Saved planet preset JSON to {}", file.getAbsolutePath());
            } catch (IOException ex) {
                logger.error("Failed to save preset JSON", ex);
            }
        }
    }

    private void loadPresetJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Planet Preset");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Preset Files", "*.json"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null && file.exists()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                PlanetPreset preset = mapper.readValue(file, PlanetPreset.class);
                applyPreset(preset);
                logger.info("Loaded planet preset JSON from {}", file.getAbsolutePath());
            } catch (IOException ex) {
                logger.error("Failed to load preset JSON", ex);
            }
        }
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
                if (customElevImage != null || customBiomeImage != null || customResourceImage != null) {
                    mapLoader.mapImagesToCells(cells, customElevImage, customBiomeImage, customResourceImage, preset.minAltitudeMeters(), preset.maxAltitudeMeters());
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
