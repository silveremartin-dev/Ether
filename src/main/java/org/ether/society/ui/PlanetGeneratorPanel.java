/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.database.H3Cell;
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
import javafx.scene.image.PixelWriter;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Integrated UI Panel for procedural planet generation, astronomical physics, and climate customization.
 * 
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 */
public class PlanetGeneratorPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(PlanetGeneratorPanel.class);

    private final ProceduralGenerator generator;
    private final Consumer<List<H3Cell>> onPlanetGeneratedCallback;

    // Controls
    private ComboBox<PlanetPreset> presetCombo;
    private ComboBox<ElevationPreset> elevationPresetCombo;
    private ComboBox<ClimatePreset> climatePresetCombo;
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

    // UI Labels for i18n
    private Label headerLabel;
    private Label presetsSecHeader;
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
        controlsBox.setPrefWidth(340);
        controlsBox.setPadding(new Insets(10));

        headerLabel = new Label();
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        // 1. Preset Selector
        presetCombo = new ComboBox<>();
        presetCombo.getItems().addAll(PlanetPreset.getPresets());
        presetCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        presetCombo.setButtonCell(presetCombo.getCellFactory().call(null));
        presetCombo.setOnAction(e -> {
            if (!isUpdatingFromPreset) {
                applyPreset(presetCombo.getValue());
            }
        });
        presetCombo.setMaxWidth(Double.MAX_VALUE);

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

        presetRowLabel = new Label();
        elevationRowLabel = new Label();
        climateRowLabel = new Label();

        saveJsonBtn = new Button();
        saveJsonBtn.setMaxWidth(Double.MAX_VALUE);
        saveJsonBtn.setStyle("-fx-font-size: 11px; -fx-base: #475569;");
        saveJsonBtn.setOnAction(e -> savePresetJson());

        loadJsonBtn = new Button();
        loadJsonBtn.setMaxWidth(Double.MAX_VALUE);
        loadJsonBtn.setStyle("-fx-font-size: 11px; -fx-base: #475569;");
        loadJsonBtn.setOnAction(e -> loadPresetJson());

        HBox jsonBox = new HBox(8, saveJsonBtn, loadJsonBtn);
        HBox.setHgrow(saveJsonBtn, Priority.ALWAYS);
        HBox.setHgrow(loadJsonBtn, Priority.ALWAYS);

        presetsSecHeader = new Label();
        VBox presetSection = createSection(presetsSecHeader, new VBox(6,
                createControlRow(presetRowLabel, presetCombo),
                createControlRow(elevationRowLabel, elevationPresetCombo),
                createControlRow(climateRowLabel, climatePresetCombo),
                jsonBox
        ));

        // 2. Astronomical & Physical Controls
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
        irradianceLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");

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

        // 3. Topography & Altitudes Section
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
        altRangeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
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

        controlsBox.getChildren().addAll(headerLabel, presetSection, astroSection, topoSection);

        ScrollPane scrollControls = new ScrollPane(controlsBox);
        scrollControls.setFitToWidth(true);
        scrollControls.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // --- Center: Map Preview & Generation ---
        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(10));

        previewTitle = new Label();
        previewTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");

        previewCanvas = new Canvas(640, 320);
        previewCanvas.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 0);");

        statsLabel = new Label();
        statsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #e2e8f0;");

        astroLabel = new Label();
        astroLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

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
        header.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        VBox box = new VBox(8, header, content);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 8; -fx-padding: 10;");
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
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1;");
        return new VBox(4, label, control);
    }

    private VBox createControlRow(Label label, Slider slider, String formatPattern) {
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1;");
        Label valLabel = new Label(String.format(formatPattern, slider.getValue()));
        valLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8;");
        
        slider.valueProperty().addListener((obs, old, val) -> 
            valLabel.setText(String.format(formatPattern, val.doubleValue()))
        );

        HBox header = new HBox(label, new Pane(), valLabel);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        return new VBox(3, header, slider);
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

        int oceanCount = 0;

        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y / (double) h) * 180.0;
            for (int x = 0; x < w; x++) {
                double lng = (x / (double) w) * 360.0 - 180.0;

                PlanetPoint p = generator.getPlanetPoint(lat, lng, preset);

                if (p.biome() == Biome.OCEAN || p.biome() == Biome.DEEP_OCEAN) {
                    oceanCount++;
                }

                pw.setColor(x, y, getBiomeColor(p.biome()));
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
        astroSecHeader.setText(I18n.get("planet.section.astro"));
        topoSecHeader.setText(I18n.get("planet.section.topo"));
        previewTitle.setText(I18n.get("planet.preview.title"));
        generateBtn.setText(I18n.get("planet.btn.generate"));

        if (presetRowLabel != null) presetRowLabel.setText(I18n.get("planet.preset"));
        if (elevationRowLabel != null) elevationRowLabel.setText(I18n.get("planet.preset.elevation"));
        if (climateRowLabel != null) climateRowLabel.setText(I18n.get("planet.preset.climate"));
        if (saveJsonBtn != null) saveJsonBtn.setText(I18n.get("planet.btn.save_json"));
        if (loadJsonBtn != null) loadJsonBtn.setText(I18n.get("planet.btn.load_json"));

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
