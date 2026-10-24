/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.planet;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.data.OnlineMapService;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.WindowUtils;
import org.ether.society.ui.util.DataSourceUIHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;

/**
 * Autonomous UI Controller and Section Builder for Atmosphere, Greenhouse Gases,
 * Macroclimate Regimes, Thermal Equator-to-Pole Gradients, and Tri-Map Climate Raster Ingestion.
 *
 * <p>Extracted from {@code PlanetGeneratorPanel} to satisfy the Single Responsibility Principle (SRP).
 * Manages temperature, precipitation, and seasonality sub-blocks with independent procedural seeds and WMS feeds.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PlanetClimateSectionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PlanetClimateSectionBuilder.class);

    public static final String CLIMATE_SRC_TEMP_EARTH = "🌍 Terre — WorldClim v2.1 Bio1 & ERA5 (Composite) [Global, -100 000 BP à +2100 AD]";
    public static final String CLIMATE_SRC_TEMP_MARS = "🔴 Mars — MGS TES Thermal Radiometry [Planétaire (Mars), -4.1 Ga à Actuel]";
    public static final String CLIMATE_SRC_TEMP_VENUS = "🟡 Vénus — Magellan SAR & Hypsometric Model [Planétaire (Vénus), -500 Ma à Actuel]";
    public static final String CLIMATE_SRC_TEMP_MOON = "⚪ Lune — LRO Diviner Thermal Radiometer [Planétaire (Lune), -4.5 Ga à Actuel]";
    public static final String CLIMATE_SRC_TEMP_MERCURY = "⚪ Mercure — MESSENGER MLA Extreme Thermal Model [Planétaire (Mercure), -4.0 Ga à Actuel]";
    public static final String CLIMATE_SRC_TEMP_WMS = "🌐 NASA MERRA-2 (WMS Satellite) [Global, 1980 AD à Actuel]";

    public static final String CLIMATE_SRC_PRECIP_EARTH = "🌍 Terre — WorldClim v2.1 & GPCP v2.3 (Composite) [Global, -100 000 BP à +2100 AD]";
    public static final String CLIMATE_SRC_PRECIP_MARS = "🔴 Mars — Frost & Sublimation Model [Planétaire (Mars), -4.1 Ga à Actuel]";
    public static final String CLIMATE_SRC_PRECIP_VENUS = "🟡 Vénus — H2SO4 Virga Cycle Model [Planétaire (Vénus), -500 Ma à Actuel]";
    public static final String CLIMATE_SRC_PRECIP_MOON = "⚪ Lune — LRO LEND Vacuum Exosphere [Planétaire (Lune), -4.5 Ga à Actuel]";
    public static final String CLIMATE_SRC_PRECIP_MERCURY = "⚪ Mercure — MESSENGER Exospheric Vacuum Model [Planétaire (Mercure), -4.0 Ga à Actuel]";
    public static final String CLIMATE_SRC_PRECIP_WMS = "🌐 NASA GPM IMERG (WMS Satellite) [Global, 2000 AD à Actuel]";

    public static final String CLIMATE_SRC_SEASON_EARTH = "🌍 Terre — WorldClim v2.1 Bio4 & ERA5 (Composite) [Global, -100 000 BP à +2100 AD]";
    public static final String CLIMATE_SRC_SEASON_MARS = "🔴 Mars — Orbital Eccentricity Insolation Model [Planétaire (Mars), Cycles Milankovitch Martiens]";
    public static final String CLIMATE_SRC_SEASON_VENUS = "🟡 Vénus — Super-Rotation Low Variance Model [Planétaire (Vénus), -500 Ma à Actuel]";
    public static final String CLIMATE_SRC_SEASON_MOON = "⚪ Lune — Diurnal Insolation Amplitude Model [Planétaire (Lune), Cycle Synodique 29.5j]";
    public static final String CLIMATE_SRC_SEASON_MERCURY = "⚪ Mercure — 3:2 Spin-Orbit Thermal Variance Model [Planétaire (Mercure), Résonance 3:2]";
    public static final String CLIMATE_SRC_SEASON_WMS = "🌐 NASA MODIS LST Amplitude (WMS Satellite) [Global, 2002 AD à Actuel]";

    /** Callback interface defining interaction with the parent panel */
    public interface HostContext {
        void notifyParamChange();
        void updatePreview();
        boolean isUpdatingFromPreset();
        void calculateStellarIrradiance();
        void selectViewMode(int modeIndex);
        void exportProceduralClimateMap(String type);
        void fetchOnlineClimateData();
        Window getWindow();
    }

    private final HostContext context;

    // Sliders
    private Slider tempGradSlider;
    private Slider oxygenSlider;
    private Slider co2Slider;
    private Slider albedoSlider;
    private Slider atmoPressureSlider;

    // Sub-block 1: Temperature
    private RadioButton radioTempProc;
    private RadioButton radioTempImport;
    private TextField tempSeedField;
    private ComboBox<String> tempSourceCombo;
    private Label climateFileLabel;
    private Button loadClimateBtn;
    private Button clearClimateBtn;

    // Sub-block 2: Precipitation
    private RadioButton radioPrecipProc;
    private RadioButton radioPrecipImport;
    private TextField precipSeedField;
    private ComboBox<String> precipSourceCombo;
    private Label rainfallFileLabel;
    private Button loadRainfallBtn;
    private Button clearRainfallBtn;

    // Sub-block 3: Seasonality
    private RadioButton radioSeasonProc;
    private RadioButton radioSeasonImport;
    private TextField seasonSeedField;
    private ComboBox<String> seasonSourceCombo;
    private Label seasonalityFileLabel;
    private Button loadSeasonalityBtn;
    private Button clearSeasonalityBtn;

    // Custom Map Images & Base64 Caches
    private Image customClimateImage;
    private Image customRainfallImage;
    private Image customSeasonalityImage;
    private String cachedClimateB64;
    private String cachedRainfallB64;
    private String cachedSeasonalityB64;

    // Labels
    private Label climateSecHeader;
    private Label gradRowLabel;
    private Label oxygenRowLabel;
    private Label co2RowLabel;
    private Label albedoRowLabel;
    private Label atmoPressureRowLabel;
    private Label tempSubHeader;
    private Label precipSubHeader;
    private Label seasonSubHeader;
    private Label earthPaleoNoticeLabel;
    private Label tempSeedLabel;
    private Label precipSeedLabel;
    private Label seasonSeedLabel;
    private Label tempHintLabel;
    private Label precipHintLabel;
    private Label seasonHintLabel;
    private Label tempSourceLabel;
    private Label precipSourceLabel;
    private Label seasonSourceLabel;
    private Label tempFormatHintLabel;
    private Label precipFormatHintLabel;
    private Label seasonFormatHintLabel;

    public PlanetClimateSectionBuilder(HostContext context) {
        this.context = context;
    }

    public VBox buildSection() {
        tempGradSlider = createSlider(0, 100, 40);
        oxygenSlider = createSlider(0, 50, 21);
        co2Slider = createSlider(0, 1_000_000, 420);
        albedoSlider = createSlider(0.0, 1.0, 0.30);
        atmoPressureSlider = createSlider(0.0, 10.0, 1.0);

        albedoSlider.valueProperty().addListener((obs, old, val) -> context.calculateStellarIrradiance());
        atmoPressureSlider.valueProperty().addListener((obs, old, val) -> context.calculateStellarIrradiance());
        co2Slider.valueProperty().addListener((obs, old, val) -> context.calculateStellarIrradiance());
        tempGradSlider.valueProperty().addListener((obs, old, val) -> context.updatePreview());

        gradRowLabel = new Label();
        oxygenRowLabel = new Label();
        co2RowLabel = new Label();
        albedoRowLabel = new Label();
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

        // Sub-block 1: Temperature
        ToggleGroup tempToggle = new ToggleGroup();
        radioTempProc = new RadioButton(I18n.getOrDefault("planet.radio.procedural", "▶ Procedural Generation (Perlin Noise)"));
        radioTempImport = new RadioButton(I18n.getOrDefault("planet.radio.import_wms", "📂 External Source (PNG / WMS)"));
        radioTempProc.setToggleGroup(tempToggle); radioTempProc.setSelected(true);
        radioTempProc.getStyleClass().add("radio-proc");
        radioTempImport.getStyleClass().add("radio-import");
        radioTempImport.setToggleGroup(tempToggle);

        tempSeedField = new TextField("54321");
        tempSeedField.textProperty().addListener((obs, old, val) -> {
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            context.updatePreview();
        });
        Button tempRandBtn = new Button("🎲");
        tempRandBtn.getStyleClass().add("button-secondary");
        tempRandBtn.setOnAction(e -> {
            tempSeedField.setText(String.valueOf(new Random().nextLong(1_000_000)));
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            context.updatePreview();
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
        exportTempBtn.setOnAction(e -> context.exportProceduralClimateMap("temp"));

        VBox tempProcPanel = new VBox(6, tempSeedLabel, tempSeedBox, tempHintLabel, exportTempBtn);
        tempProcPanel.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(168, 85, 247, 0.3); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");

        tempSourceCombo = buildClimateSourceCombo("temp");
        climateFileLabel = new Label("—"); climateFileLabel.getStyleClass().add("value-label");
        loadClimateBtn = new Button(I18n.get("planet.map.btn_load")); loadClimateBtn.getStyleClass().add("button-secondary");
        loadClimateBtn.setOnAction(e -> chooseClimateMapFile());
        clearClimateBtn = new Button("❌"); clearClimateBtn.getStyleClass().add("button-secondary");
        clearClimateBtn.setOnAction(e -> {
            customClimateImage = null;
            cachedClimateB64 = null;
            climateFileLabel.setText("—");
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            context.updatePreview();
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
            if (!context.isUpdatingFromPreset()) {
                context.selectViewMode(1);
                context.notifyParamChange();
                context.updatePreview();
            }
        });
        radioTempProc.setOnAction(e -> {
            context.selectViewMode(1);
            context.updatePreview();
        });
        radioTempImport.setOnAction(e -> {
            context.selectViewMode(1);
            context.updatePreview();
        });
        tempSubHeader = new Label(I18n.getOrDefault("planet.climate.temp.header", "🌡  Temperature & Thermal:"));
        tempSubHeader.getStyleClass().add("control-label");
        VBox tempSubBlock = new VBox(6, tempSubHeader, radioTempProc, tempProcPanel, radioTempImport, tempImportPanel);
        tempSubBlock.setStyle("-fx-padding: 10 10 10 10; -fx-background-color: rgba(168, 85, 247, 0.08); -fx-background-radius: 6; -fx-border-color: rgba(168, 85, 247, 0.25); -fx-border-radius: 6;");

        // Sub-block 2: Precipitation
        ToggleGroup precipToggle = new ToggleGroup();
        radioPrecipProc = new RadioButton(I18n.getOrDefault("planet.radio.procedural", "▶ Procedural Generation (Perlin Noise)"));
        radioPrecipImport = new RadioButton(I18n.getOrDefault("planet.radio.import_wms", "📂 External Source (PNG / WMS)"));
        radioPrecipProc.setToggleGroup(precipToggle); radioPrecipProc.setSelected(true);
        radioPrecipProc.getStyleClass().add("radio-proc");
        radioPrecipImport.getStyleClass().add("radio-import");
        radioPrecipImport.setToggleGroup(precipToggle);

        precipSeedField = new TextField("11111");
        precipSeedField.textProperty().addListener((obs, old, val) -> {
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            context.updatePreview();
        });
        Button precipRandBtn = new Button("🎲");
        precipRandBtn.getStyleClass().add("button-secondary");
        precipRandBtn.setOnAction(e -> {
            precipSeedField.setText(String.valueOf(new Random().nextLong(1_000_000)));
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            context.updatePreview();
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
        exportPrecipBtn.setOnAction(e -> context.exportProceduralClimateMap("precip"));

        VBox precipProcPanel = new VBox(6, precipSeedLabel, precipSeedBox, precipHintLabel, exportPrecipBtn);
        precipProcPanel.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(2, 132, 199, 0.3); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");

        precipSourceCombo = buildClimateSourceCombo("precip");
        rainfallFileLabel = new Label("—"); rainfallFileLabel.getStyleClass().add("value-label");
        loadRainfallBtn = new Button(I18n.get("planet.map.btn_load")); loadRainfallBtn.getStyleClass().add("button-secondary");
        loadRainfallBtn.setOnAction(e -> chooseRainfallMapFile());
        clearRainfallBtn = new Button("❌"); clearRainfallBtn.getStyleClass().add("button-secondary");
        clearRainfallBtn.setOnAction(e -> {
            customRainfallImage = null;
            cachedRainfallB64 = null;
            rainfallFileLabel.setText("—");
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            context.updatePreview();
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
            if (!context.isUpdatingFromPreset()) {
                context.selectViewMode(2);
                context.notifyParamChange();
                context.updatePreview();
            }
        });
        radioPrecipProc.setOnAction(e -> {
            context.selectViewMode(2);
            context.updatePreview();
        });
        radioPrecipImport.setOnAction(e -> {
            context.selectViewMode(2);
            context.updatePreview();
        });
        precipSubHeader = new Label(I18n.getOrDefault("planet.climate.precip.header", "🌧  Precipitation / Humidity:"));
        precipSubHeader.getStyleClass().add("control-label");
        VBox precipSubBlock = new VBox(6, precipSubHeader, radioPrecipProc, precipProcPanel, radioPrecipImport, precipImportPanel);
        precipSubBlock.setStyle("-fx-padding: 10 10 10 10; -fx-background-color: rgba(2, 132, 199, 0.08); -fx-background-radius: 6; -fx-border-color: rgba(2, 132, 199, 0.25); -fx-border-radius: 6;");

        // Sub-block 3: Seasonality
        ToggleGroup seasonToggle = new ToggleGroup();
        radioSeasonProc = new RadioButton(I18n.getOrDefault("planet.radio.procedural", "▶ Procedural Generation (Perlin Noise)"));
        radioSeasonImport = new RadioButton(I18n.getOrDefault("planet.radio.import_wms", "📂 External Source (PNG / WMS)"));
        radioSeasonProc.setToggleGroup(seasonToggle); radioSeasonProc.setSelected(true);
        radioSeasonProc.getStyleClass().add("radio-proc");
        radioSeasonImport.getStyleClass().add("radio-import");
        radioSeasonImport.setToggleGroup(seasonToggle);

        seasonSeedField = new TextField("99999");
        seasonSeedField.textProperty().addListener((obs, old, val) -> {
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            context.updatePreview();
        });
        Button seasonRandBtn = new Button("🎲");
        seasonRandBtn.getStyleClass().add("button-secondary");
        seasonRandBtn.setOnAction(e -> {
            seasonSeedField.setText(String.valueOf(new Random().nextLong(1_000_000)));
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            context.updatePreview();
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
        exportSeasonBtn.setOnAction(e -> context.exportProceduralClimateMap("season"));

        VBox seasonProcPanel = new VBox(6, seasonSeedLabel, seasonSeedBox, seasonHintLabel, exportSeasonBtn);
        seasonProcPanel.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(245, 158, 11, 0.3); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");

        seasonSourceCombo = buildClimateSourceCombo("season");
        seasonalityFileLabel = new Label("—"); seasonalityFileLabel.getStyleClass().add("value-label");
        loadSeasonalityBtn = new Button(I18n.get("planet.map.btn_load")); loadSeasonalityBtn.getStyleClass().add("button-secondary");
        loadSeasonalityBtn.setOnAction(e -> chooseSeasonalityMapFile());
        clearSeasonalityBtn = new Button("❌"); clearSeasonalityBtn.getStyleClass().add("button-secondary");
        clearSeasonalityBtn.setOnAction(e -> {
            customSeasonalityImage = null;
            cachedSeasonalityB64 = null;
            seasonalityFileLabel.setText("—");
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            context.updatePreview();
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
            if (!context.isUpdatingFromPreset()) {
                context.selectViewMode(3);
                context.notifyParamChange();
                context.updatePreview();
            }
        });
        radioSeasonProc.setOnAction(e -> {
            context.selectViewMode(3);
            context.updatePreview();
        });
        radioSeasonImport.setOnAction(e -> {
            context.selectViewMode(3);
            context.updatePreview();
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
        climateSecHeader.getStyleClass().add("label-section-header");
        VBox section = new VBox(8, climateSecHeader, climateControls);
        section.getStyleClass().add("card-section");
        return section;
    }

    private ComboBox<String> buildClimateSourceCombo(String mapType) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getItems().add("");
        switch (mapType) {
            case "temp" -> combo.getItems().addAll(
                    CLIMATE_SRC_TEMP_EARTH, CLIMATE_SRC_TEMP_MARS, CLIMATE_SRC_TEMP_VENUS,
                    CLIMATE_SRC_TEMP_MOON, CLIMATE_SRC_TEMP_MERCURY, CLIMATE_SRC_TEMP_WMS
            );
            case "precip" -> combo.getItems().addAll(
                    CLIMATE_SRC_PRECIP_EARTH, CLIMATE_SRC_PRECIP_MARS, CLIMATE_SRC_PRECIP_VENUS,
                    CLIMATE_SRC_PRECIP_MOON, CLIMATE_SRC_PRECIP_MERCURY, CLIMATE_SRC_PRECIP_WMS
            );
            default -> combo.getItems().addAll(
                    CLIMATE_SRC_SEASON_EARTH, CLIMATE_SRC_SEASON_MARS, CLIMATE_SRC_SEASON_VENUS,
                    CLIMATE_SRC_SEASON_MOON, CLIMATE_SRC_SEASON_MERCURY, CLIMATE_SRC_SEASON_WMS
            );
        }
        DataSourceUIHelper.setupDetailedSourceCombo(combo, "common.combo.prompt_source", "planet.tooltip.map_source_hint");
        combo.setValue("");
        combo.setOnAction(e -> {
            if (context.isUpdatingFromPreset()) return;
            String val = combo.getValue();
            if (val != null && !val.isEmpty()) {
                if (val.contains("WMS")) {
                    context.fetchOnlineClimateData();
                } else {
                    applyClimateSourceSelection(mapType, val);
                }
            }
        });
        return combo;
    }

    private void applyClimateSourceSelection(String mapType, String selectedSource) {
        if (selectedSource == null || selectedSource.isBlank() || context.isUpdatingFromPreset()) return;
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
                cachedClimateB64 = null;
                if (radioTempImport != null) radioTempImport.setSelected(true);
                if (climateFileLabel != null) climateFileLabel.setText("🌡 " + selectedSource);
                context.selectViewMode(1);
            }
            case "precip" -> {
                customRainfallImage = img;
                cachedRainfallB64 = null;
                if (radioPrecipImport != null) radioPrecipImport.setSelected(true);
                if (rainfallFileLabel != null) rainfallFileLabel.setText("🌧 " + selectedSource);
                context.selectViewMode(2);
            }
            default -> {
                customSeasonalityImage = img;
                cachedSeasonalityB64 = null;
                if (radioSeasonImport != null) radioSeasonImport.setSelected(true);
                if (seasonalityFileLabel != null) seasonalityFileLabel.setText("🍂 " + selectedSource);
                context.selectViewMode(3);
            }
        }
        context.notifyParamChange();
        context.updatePreview();
    }

    public void setLocalFileInCombo(ComboBox<String> combo, String fileName) {
        String prefix = "📌 Fichier local : ";
        combo.getItems().removeIf(item -> item != null && item.startsWith(prefix));
        String entry = prefix + fileName;
        combo.getItems().add(entry);
        combo.setValue(entry);
    }

    private void chooseClimateMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.chooser.climate", "Load climate / temperature map image"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(context.getWindow());
        if (file != null) {
            try {
                customClimateImage = new Image(new FileInputStream(file));
                cachedClimateB64 = null;
                climateFileLabel.setText("🌡 " + file.getName());
                setLocalFileInCombo(tempSourceCombo, file.getName());
                context.selectViewMode(1);
                context.notifyParamChange();
                context.updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load climate map image", ex);
            }
        }
    }

    private void chooseRainfallMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.chooser.rainfall", "Load rainfall map image"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(context.getWindow());
        if (file != null) {
            try {
                customRainfallImage = new Image(new FileInputStream(file));
                cachedRainfallB64 = null;
                rainfallFileLabel.setText("🌧 " + file.getName());
                setLocalFileInCombo(precipSourceCombo, file.getName());
                context.selectViewMode(2);
                context.notifyParamChange();
                context.updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load rainfall map image", ex);
            }
        }
    }

    private void chooseSeasonalityMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.chooser.seasonality", "Load seasonality map image"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(context.getWindow());
        if (file != null) {
            try {
                customSeasonalityImage = new Image(new FileInputStream(file));
                cachedSeasonalityB64 = null;
                seasonalityFileLabel.setText("🍂 " + file.getName());
                setLocalFileInCombo(seasonSourceCombo, file.getName());
                context.selectViewMode(3);
                context.notifyParamChange();
                context.updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load seasonality map image", ex);
            }
        }
    }

    public void applyPreset(PlanetPreset p) {
        if (p == null) return;
        tempGradSlider.setValue(p.temperatureGradient());
        oxygenSlider.setValue(p.oxygenPercentage());
        co2Slider.setValue(p.co2Ppm());
        albedoSlider.setValue(p.albedo());
        atmoPressureSlider.setValue(p.atmospherePressureAtm());

        if (p.customClimateBase64() != null) {
            customClimateImage = ImageMapLoader.base64PngToImage(p.customClimateBase64());
            cachedClimateB64 = p.customClimateBase64();
            if (climateFileLabel != null) climateFileLabel.setText(I18n.getOrDefault("planet.preset.climate", "🌡 Preset Climate"));
        }
        if (p.customRainfallBase64() != null) {
            customRainfallImage = ImageMapLoader.base64PngToImage(p.customRainfallBase64());
            cachedRainfallB64 = p.customRainfallBase64();
            if (rainfallFileLabel != null) rainfallFileLabel.setText(I18n.getOrDefault("planet.preset.rainfall", "🌧 Preset Rainfall"));
        }
        if (p.customSeasonalityBase64() != null) {
            customSeasonalityImage = ImageMapLoader.base64PngToImage(p.customSeasonalityBase64());
            cachedSeasonalityB64 = p.customSeasonalityBase64();
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText(I18n.getOrDefault("planet.preset.seasonality", "🍂 Preset Seasonality"));
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
    }

    public void updateTexts() {
        if (climateSecHeader != null) climateSecHeader.setText(I18n.getOrDefault("planet.section.climate", "ATMOSPHERE & PLANETARY CLIMATE DOMAIN"));
        if (gradRowLabel != null) gradRowLabel.setText(I18n.get("planet.param.temp_grad"));
        if (oxygenRowLabel != null) oxygenRowLabel.setText(I18n.getOrDefault("planet.param.oxygen", "Oxygen Level O₂ (%)"));
        if (co2RowLabel != null) co2RowLabel.setText(I18n.getOrDefault("planet.param.co2", "Dioxyde de Carbone CO₂ (ppm)"));
        if (albedoRowLabel != null) albedoRowLabel.setText(I18n.getOrDefault("planet.param.albedo", "Planetary Albedo"));
        if (atmoPressureRowLabel != null) atmoPressureRowLabel.setText(I18n.getOrDefault("planet.param.atmo_pressure", "Atmospheric Pressure (atm)"));

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

        if (loadClimateBtn != null) loadClimateBtn.setText(I18n.get("planet.map.btn_load"));
        if (loadRainfallBtn != null) loadRainfallBtn.setText(I18n.get("planet.map.btn_load"));
        if (loadSeasonalityBtn != null) loadSeasonalityBtn.setText(I18n.get("planet.map.btn_load"));

        if (tempSourceCombo != null && tempSourceCombo.getCellFactory() != null) tempSourceCombo.setButtonCell(tempSourceCombo.getCellFactory().call(null));
        if (precipSourceCombo != null && precipSourceCombo.getCellFactory() != null) precipSourceCombo.setButtonCell(precipSourceCombo.getCellFactory().call(null));
        if (seasonSourceCombo != null && seasonSourceCombo.getCellFactory() != null) seasonSourceCombo.setButtonCell(seasonSourceCombo.getCellFactory().call(null));
    }

    private Slider createSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.valueProperty().addListener((obs, old, val) -> {
            if (context.isUpdatingFromPreset()) return;
            context.notifyParamChange();
            if (!slider.isValueChanging()) {
                context.updatePreview();
            }
        });
        slider.setOnMouseReleased(e -> {
            if (context.isUpdatingFromPreset()) return;
            context.notifyParamChange();
            context.updatePreview();
        });
        return slider;
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

    // Getters for building preset and diagnostics
    public double getTemperatureGradient() { return tempGradSlider.getValue(); }
    public double getOxygenPercentage() { return oxygenSlider.getValue(); }
    public double getCo2Ppm() { return co2Slider.getValue(); }
    public double getAlbedo() { return albedoSlider.getValue(); }
    public double getAtmospherePressureAtm() { return atmoPressureSlider.getValue(); }

    public boolean isTempImportActive() { return radioTempImport != null && radioTempImport.isSelected(); }
    public String getTempSource() { return tempSourceCombo != null && tempSourceCombo.getValue() != null ? tempSourceCombo.getValue() : ""; }
    public long getTempSeed(long defaultSeed) {
        try {
            if (tempSeedField != null && tempSeedField.getText() != null && !tempSeedField.getText().isEmpty()) {
                return Long.parseLong(tempSeedField.getText());
            }
        } catch (NumberFormatException ignored) {}
        return defaultSeed + 100L;
    }

    public boolean isPrecipImportActive() { return radioPrecipImport != null && radioPrecipImport.isSelected(); }
    public String getPrecipSource() { return precipSourceCombo != null && precipSourceCombo.getValue() != null ? precipSourceCombo.getValue() : ""; }
    public long getPrecipSeed(long defaultSeed) {
        try {
            if (precipSeedField != null && precipSeedField.getText() != null && !precipSeedField.getText().isEmpty()) {
                return Long.parseLong(precipSeedField.getText());
            }
        } catch (NumberFormatException ignored) {}
        return defaultSeed + 1000L;
    }

    public boolean isSeasonImportActive() { return radioSeasonImport != null && radioSeasonImport.isSelected(); }
    public String getSeasonSource() { return seasonSourceCombo != null && seasonSourceCombo.getValue() != null ? seasonSourceCombo.getValue() : ""; }
    public long getSeasonSeed(long defaultSeed) {
        try {
            if (seasonSeedField != null && seasonSeedField.getText() != null && !seasonSeedField.getText().isEmpty()) {
                return Long.parseLong(seasonSeedField.getText());
            }
        } catch (NumberFormatException ignored) {}
        return defaultSeed + 2000L;
    }

    public Image getCustomClimateImage() { return customClimateImage; }
    public void setCustomClimateImage(Image img) { this.customClimateImage = img; }
    public String getCachedClimateBase64() { return cachedClimateB64; }
    public void setCachedClimateBase64(String b64) { this.cachedClimateB64 = b64; }

    public Image getCustomRainfallImage() { return customRainfallImage; }
    public void setCustomRainfallImage(Image img) { this.customRainfallImage = img; }
    public String getCachedRainfallBase64() { return cachedRainfallB64; }
    public void setCachedRainfallBase64(String b64) { this.cachedRainfallB64 = b64; }

    public Image getCustomSeasonalityImage() { return customSeasonalityImage; }
    public void setCustomSeasonalityImage(Image img) { this.customSeasonalityImage = img; }
    public String getCachedSeasonalityBase64() { return cachedSeasonalityB64; }
    public void setCachedSeasonalityBase64(String b64) { this.cachedSeasonalityB64 = b64; }

    public RadioButton getRadioTempImport() { return radioTempImport; }
    public RadioButton getRadioPrecipImport() { return radioPrecipImport; }
    public RadioButton getRadioSeasonImport() { return radioSeasonImport; }
    public Button getLoadClimateBtn() { return loadClimateBtn; }
    public Button getLoadRainfallBtn() { return loadRainfallBtn; }
    public Button getLoadSeasonalityBtn() { return loadSeasonalityBtn; }
    public Label getClimateFileLabel() { return climateFileLabel; }
    public Label getRainfallFileLabel() { return rainfallFileLabel; }
    public Label getSeasonalityFileLabel() { return seasonalityFileLabel; }
    public ComboBox<String> getTempSourceCombo() { return tempSourceCombo; }
    public ComboBox<String> getPrecipSourceCombo() { return precipSourceCombo; }
    public ComboBox<String> getSeasonSourceCombo() { return seasonSourceCombo; }
}
