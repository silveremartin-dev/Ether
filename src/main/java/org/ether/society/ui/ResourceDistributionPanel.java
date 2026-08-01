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
import org.ether.society.model.EcologyPreset;
import org.ether.society.procedural.PlanetPreset;
import org.ether.society.procedural.ProceduralGenerator;

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
 * Enhanced UI Panel for editing planet-wide ecological and resource distribution 
 * using real scientific metric variables (GtC, Gt, Mt, mW/m², 10³ km³).
 * 
 * Features automated planetary derivation from Tab 1 physics, spatial dispersion algorithms,
 * custom biome/geology map imports (PNG / WMS / ESRI World Files), and real-time 2D visualization.
 * 
 * @author Silvere Martin-Michiellot
 * @version 2.5.0
 */
public class ResourceDistributionPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ResourceDistributionPanel.class);

    private List<H3Cell> activeCells;
    private PlanetPreset activePlanetPreset;
    private final Consumer<List<H3Cell>> onResourcesAppliedCallback;
    private final ImageMapLoader mapLoader = new ImageMapLoader();
    private final OnlineMapService onlineMapService = new OnlineMapService();
    private final ProceduralGenerator generator = new ProceduralGenerator();

    // Custom Ecology Image Maps
    private Image customBiomeImage;
    private Image customResourceImage;
    private Image customHydroImage;       // Hydrography / River map
    // Climate maps moved from Tab 1
    private Image customClimateImage;     // Temperature / Combined RGB
    private Image customRainfallImage;    // Precipitation
    private Image customSeasonalityImage; // Seasonality

    // Inherited Planet Preset & Context Controls
    private Label planetSectionHeader;
    private ComboBox<PlanetPreset> planetPresetCombo;
    private Label planetContextLabel;
    private Button syncPlanetBtn;
    private Button autoDeriveEcologyBtn;
    private Button autoDeriveGeologyBtn;

    private boolean isUpdatingFromPreset = false;

    // Presets Bar
    private PresetControlBar<EcologyPreset> ecologyPresetBar;

    // Scientific Resource Sliders (Metric Units)
    private Slider terrestrialBiomassSlider;  // GtC (Gigatons of Carbon)
    private Slider soilCarbonSlider;          // GtC (Soil organic carbon / Agriculture)
    private Slider faunaBiomassSlider;        // GtC (Terrestrial animal & game fauna)
    private Slider aquaticBiomassSlider;      // GtC (Marine & freshwater life)
    private Slider crustalMetalSlider;        // Gt (Industrial base metals: Fe, Cu, Al)
    private Slider preciousMetalSlider;       // Mt (Precious & rare earth ores: Au, Pt, REE)
    private Slider mantleHeatSlider;          // mW/m² (Mantle heat flow & geothermal/tectonic index)
    private Slider freshwaterAquiferSlider;   // 10³ km³ (Groundwater & deep aquifer reserves)

    // Custom Map Controls & Buttons
    private Label mapsSecHeader;
    private ComboBox<String> mapSourceCombo;
    private Label mapSourceRowLabel;
    private Label biomeFileLabel;
    private Label resourceFileLabel;
    private Label hydroFileLabel;
    private Label biomeMapRowLabel;
    private Label resourceMapRowLabel;
    private Label hydroMapRowLabel;
    private Button loadBiomeBtn;
    private Button clearBiomeBtn;
    private Button loadResourceBtn;
    private Button clearResourceBtn;
    private Button loadHydroBtn;
    private Button clearHydroBtn;
    private Button fetchOnlineHydroBtn;
    private Button proceduralHydroBtn;
    // Climate map controls (moved from Tab 1)
    private Label climateMapRowLabel;
    private Label rainfallMapRowLabel;
    private Label seasonalityMapRowLabel;
    private Label climateFileLabel;
    private Label rainfallFileLabel;
    private Label seasonalityFileLabel;
    private Button loadClimateBtn;
    private Button clearClimateBtn;
    private Button loadRainfallBtn;
    private Button clearRainfallBtn;
    private Button loadSeasonalityBtn;
    private Button clearSeasonalityBtn;
    private Button fetchOnlineClimateBtn;
    private Button climateHelpBtn;
    private Label climateStatusLabel;
    private Button fetchOnlineBtn;
    private Button exportMapsBtn;
    private Button specsHelpBtn;
    private Label mapStatusLabel;

    // Seismic & Volcanic Activity Sliders for Tab 2
    private Slider seismicActivitySlider;
    private Slider volcanicActivitySlider;
    private Label seismicRowLabel;
    private Label volcanicRowLabel;

    // Per-domain RadioButtons & Compatibility Labels for Tab 2
    private RadioButton radioProcBiome;
    private RadioButton radioImportBiome;
    private Label biomeCompatibilityLabel;

    private RadioButton radioProcHydro;
    private RadioButton radioImportHydro;
    private Label hydroCompatibilityLabel;

    private RadioButton radioProcClimate;
    private RadioButton radioImportClimate;
    private Label climateCompatibilityLabel;

    private RadioButton radioProcGeology;
    private RadioButton radioImportGeology;
    private Label geologyCompatibilityLabel;

    // Independent Domain Seeds
    private TextField biomeSeedField;
    private TextField hydroSeedField;
    private TextField climateSeedField;
    private TextField geologySeedField;

    // Domain Source ComboBoxes
    private ComboBox<String> biomeSourceCombo;
    private ComboBox<String> hydroSourceCombo;
    private ComboBox<String> climateSourceCombo;
    private ComboBox<String> geologySourceCombo;


    // Legacy single RadioButtons kept for preset compatibility
    private RadioButton radioProcEco;
    private RadioButton radioImportEco;

    // Section header labels for live i18n update
    private Label seedSecHeader;
    private Label biomeDomainSecHeader;
    private Label hydroDomainSecHeader;
    private Label climateSummaryLabel;
    private Label climateDomainSecHeader;
    private Label geologyDomainSecHeader;
    private Label ecoCompatibilityLabel;

    // Section & Row Labels for i18n
    private Label headerLabel;
    private Label floraSecHeader;
    private Label faunaSecHeader;
    private Label mineralSecHeader;
    private Label aquaticSecHeader;

    private Label terrestrialBiomassRowLabel;
    private Label soilCarbonRowLabel;
    private Label faunaBiomassRowLabel;
    private Label aquaticBiomassRowLabel;
    private Label crustalMetalRowLabel;
    private Label preciousMetalRowLabel;
    private Label mantleHeatRowLabel;
    private Label freshwaterAquiferRowLabel;

    // Seed & Random Controls
    private TextField seedField;
    private Button randSeedBtn;
    private Button regenBtn;

    // Right-Side View Visualizers & Preview
    private Label rightViewTitle;
    private ComboBox<String> viewModeCombo;
    private Canvas mapPreviewCanvas;
    private HBox legendBar;
    private Label summaryLabel;
    private Button applyBtn;

    // Interactive Zoom/Pan State
    private double zoomFactor = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private double dragStartX, dragStartY;

    public ResourceDistributionPanel(Consumer<List<H3Cell>> onResourcesAppliedCallback) {
        this.onResourcesAppliedCallback = onResourcesAppliedCallback;

        getStyleClass().add("glass-panel");
        setPadding(new Insets(20));

        initUI();
        updateTexts();

        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    public void setActivePlanetPreset(PlanetPreset planetPreset) {
        this.activePlanetPreset = planetPreset;
        if (planetPreset != null && planetPresetCombo != null) {
            planetPresetCombo.setValue(planetPreset);
        }
        updatePlanetContextDisplay();
        updatePreviewCanvas();
    }

    public void setActiveCells(List<H3Cell> cells) {
        this.activeCells = cells;
        updateSummary();
        updatePreviewCanvas();
    }

    private void initUI() {
        VBox controlsBox = new VBox(15);
        controlsBox.setPrefWidth(460);
        controlsBox.setPadding(new Insets(10));

        headerLabel = new Label();
        headerLabel.getStyleClass().add("label-title");

        // --- 1. Inherited Planet Preset & Territory Section ---
        planetSectionHeader = new Label(I18n.getOrDefault("resource.section.planet_preset", "PLANÈTE & DÉDUCTION SCIENTIFIQUE (ONGLET 1)"));

        planetPresetCombo = new ComboBox<>();
        planetPresetCombo.getItems().setAll(PlanetPreset.getPresets());
        planetPresetCombo.setValue(PlanetPreset.EARTH_LIKE);
        planetPresetCombo.setMaxWidth(Double.MAX_VALUE);
        planetPresetCombo.setCellFactory(p -> new ListCell<>() {
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
        planetPresetCombo.setButtonCell(planetPresetCombo.getCellFactory().call(null));
        planetPresetCombo.setOnAction(e -> {
            PlanetPreset selected = planetPresetCombo.getValue();
            if (selected != null) {
                this.activePlanetPreset = selected;
                adaptResourceSlidersToPlanet(selected);
                updatePlanetContextDisplay();
                updatePreviewCanvas();
            }
        });

        planetContextLabel = new Label("🪐 Territoire : Planète Terrestre (Earth-Like)");
        planetContextLabel.setWrapText(true);
        planetContextLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 6 10; -fx-background-color: rgba(56, 189, 248, 0.12); -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 6;");

        syncPlanetBtn = new Button(I18n.getOrDefault("resource.btn.sync_planet", "🔄 Synchroniser avec la Planète de l'Onglet 1"));
        syncPlanetBtn.getStyleClass().add("button-secondary");
        syncPlanetBtn.setMaxWidth(Double.MAX_VALUE);
        syncPlanetBtn.setOnAction(e -> {
            if (activePlanetPreset != null) {
                planetPresetCombo.setValue(activePlanetPreset);
                adaptResourceSlidersToPlanet(activePlanetPreset);
                updatePlanetContextDisplay();
                updatePreviewCanvas();
            }
        });

        autoDeriveEcologyBtn = new Button("⚡ Auto-déduire Écologie & Biomes depuis la Physique");
        autoDeriveEcologyBtn.getStyleClass().add("button-secondary");
        autoDeriveEcologyBtn.setMaxWidth(Double.MAX_VALUE);
        autoDeriveEcologyBtn.setOnAction(e -> autoDeriveEcologyFromPlanet());

        autoDeriveGeologyBtn = new Button("🌋 Calculer Tectonique & Métaux depuis la Manteau");
        autoDeriveGeologyBtn.getStyleClass().add("button-secondary");
        autoDeriveGeologyBtn.setMaxWidth(Double.MAX_VALUE);
        autoDeriveGeologyBtn.setOnAction(e -> autoDeriveGeologyFromPlanet());

        VBox planetSection = createSection(planetSectionHeader, new VBox(8,
                new Label(I18n.getOrDefault("resource.label.planet_preset_select", "Préréglage de corps céleste hérité :")),
                planetPresetCombo,
                planetContextLabel,
                syncPlanetBtn,
                autoDeriveEcologyBtn,
                autoDeriveGeologyBtn
        ));

        // --- 2. Standardized Preset Control Bar for Ecology ---
        ecologyPresetBar = new PresetControlBar<>(I18n.getOrDefault("resource.preset_title", "Préréglage Écologique & Ressources"));
        ecologyPresetBar.setExportCategory("ecology");
        ecologyPresetBar.setPresets(EcologyPreset.getBuiltInPresets(), EcologyPreset.EARTH_STANDARD);

        ecologyPresetBar.setListener(new PresetControlBar.PresetActionsListener<EcologyPreset>() {
            @Override
            public void onPresetSelected(EcologyPreset preset) {
                applyEcologyPreset(preset);
            }

            @Override
            public void onSavePreset(String name) {
                long seedVal = 12345L;
                try {
                    seedVal = Long.parseLong(seedField.getText());
                } catch (NumberFormatException ignored) {}

                String customBiomeB64 = customBiomeImage != null ? ImageMapLoader.imageToBase64Png(customBiomeImage) : null;
                String customResourceB64 = customResourceImage != null ? ImageMapLoader.imageToBase64Png(customResourceImage) : null;
                String customHydroB64 = customHydroImage != null ? ImageMapLoader.imageToBase64Png(customHydroImage) : null;
                String customClimateB64 = customClimateImage != null ? ImageMapLoader.imageToBase64Png(customClimateImage) : null;
                String customRainfallB64 = customRainfallImage != null ? ImageMapLoader.imageToBase64Png(customRainfallImage) : null;
                String customSeasonalityB64 = customSeasonalityImage != null ? ImageMapLoader.imageToBase64Png(customSeasonalityImage) : null;

                EcologyPreset custom = new EcologyPreset(
                        name,
                        terrestrialBiomassSlider.getValue(),
                        soilCarbonSlider.getValue(),
                        faunaBiomassSlider.getValue(),
                        aquaticBiomassSlider.getValue(),
                        crustalMetalSlider.getValue(),
                        preciousMetalSlider.getValue(),
                        mantleHeatSlider.getValue(),
                        freshwaterAquiferSlider.getValue(),
                        seedVal,
                        customBiomeB64,
                        customResourceB64,
                        customHydroB64,
                        customClimateB64,
                        customRainfallB64,
                        customSeasonalityB64
                );
                ecologyPresetBar.getPresetCombo().getItems().add(custom);
                ecologyPresetBar.getPresetCombo().setValue(custom);
            }

            @Override
            public void onDeletePreset(EcologyPreset preset) {
                ecologyPresetBar.getPresetCombo().getItems().remove(preset);
            }

            @Override
            public void onExportPreset(File targetFile, EcologyPreset preset) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.writeValue(targetFile, preset);
                    logger.info("Exported ecology preset to {}", targetFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to export ecology preset", ex);
                }
            }

            @Override
            public void onImportPreset(File sourceFile) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    EcologyPreset preset = mapper.readValue(sourceFile, EcologyPreset.class);
                    ecologyPresetBar.getPresetCombo().getItems().add(preset);
                    ecologyPresetBar.getPresetCombo().setValue(preset);
                    applyEcologyPreset(preset);
                    logger.info("Imported ecology preset from {}", sourceFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to import ecology preset", ex);
                }
            }
        });


        // --- 4. Custom Satellite & Geological Map Section (PNG / WMS / WorldFiles) ---
        mapsSecHeader = new Label(I18n.getOrDefault("resource.section.custom_maps", "CARTES SATELLITE & SITES GÉOLOGIQUES (PNG / WMS)"));

        mapSourceRowLabel = new Label(I18n.getOrDefault("resource.param.map_source", "Modèle de corps céleste / Satellite :"));
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
                    setText(I18n.getOrDefault("planet.map." + item, item));
                }
            }
        });
        mapSourceCombo.setButtonCell(mapSourceCombo.getCellFactory().call(null));
        mapSourceCombo.setMaxWidth(Double.MAX_VALUE);
        mapSourceCombo.setOnAction(e -> applyPresetMapSource(mapSourceCombo.getValue()));

        biomeMapRowLabel = new Label(I18n.getOrDefault("resource.param.biome_map", "Carte de biomes / Végétation extraterrestre (PNG) :"));
        biomeFileLabel = new Label(I18n.get("planet.map.none"));
        biomeFileLabel.getStyleClass().add("value-label");
        loadBiomeBtn = new Button(I18n.get("planet.map.btn_load"));
        loadBiomeBtn.getStyleClass().add("button-secondary");
        loadBiomeBtn.setOnAction(e -> loadCustomBiomeMap());
        clearBiomeBtn = new Button("❌");
        clearBiomeBtn.getStyleClass().add("button-secondary");
        clearBiomeBtn.setOnAction(e -> {
            customBiomeImage = null;
            biomeFileLabel.setText(I18n.get("planet.map.none"));
            updatePreviewCanvas();
            updateSummary();
        });
        HBox biomeBox = new HBox(5, loadBiomeBtn, clearBiomeBtn);

        resourceMapRowLabel = new Label(I18n.getOrDefault("resource.param.resource_map", "Carte géologique & minerais multi-canaux (PNG) :"));
        resourceFileLabel = new Label(I18n.get("planet.map.none"));
        resourceFileLabel.getStyleClass().add("value-label");
        loadResourceBtn = new Button(I18n.get("planet.map.btn_load"));
        loadResourceBtn.getStyleClass().add("button-secondary");
        loadResourceBtn.setOnAction(e -> loadCustomResourceMap());
        clearResourceBtn = new Button("❌");
        clearResourceBtn.getStyleClass().add("button-secondary");
        clearResourceBtn.setOnAction(e -> {
            customResourceImage = null;
            resourceFileLabel.setText(I18n.get("planet.map.none"));
            updatePreviewCanvas();
            updateSummary();
        });
        HBox resourceBox = new HBox(5, loadResourceBtn, clearResourceBtn);

        hydroMapRowLabel = new Label(I18n.getOrDefault("resource.param.hydro_map", "Carte Hydrographique & Fleuves (Cours d'eau PNG) :"));
        hydroFileLabel = new Label(I18n.get("planet.map.none"));
        hydroFileLabel.getStyleClass().add("value-label");
        loadHydroBtn = new Button(I18n.get("planet.map.btn_load"));
        loadHydroBtn.getStyleClass().add("button-secondary");
        loadHydroBtn.setOnAction(e -> loadCustomHydroMap());
        clearHydroBtn = new Button("❌");
        clearHydroBtn.getStyleClass().add("button-secondary");
        clearHydroBtn.setOnAction(e -> {
            customHydroImage = null;
            hydroFileLabel.setText(I18n.get("planet.map.none"));
            updatePreviewCanvas();
            updateSummary();
        });
        HBox hydroBox = new HBox(5, loadHydroBtn, clearHydroBtn);

        fetchOnlineHydroBtn = new Button(I18n.getOrDefault("resource.btn.fetch_online_hydro", "🌐 Télécharger Hydrographie Satellite (Earth WMS / Preset Terre)"));
        fetchOnlineHydroBtn.setMaxWidth(Double.MAX_VALUE);
        fetchOnlineHydroBtn.getStyleClass().add("button-secondary");
        fetchOnlineHydroBtn.setOnAction(e -> fetchOnlineHydroData());

        proceduralHydroBtn = new Button(I18n.getOrDefault("resource.btn.procedural_hydro", "⚡ Auto-générer Fleuves & Cours d'eau (Procédural par Déclivité)"));
        proceduralHydroBtn.setMaxWidth(Double.MAX_VALUE);
        proceduralHydroBtn.getStyleClass().add("button-secondary");
        proceduralHydroBtn.setOnAction(e -> generateProceduralHydrography());

        fetchOnlineBtn = new Button(I18n.getOrDefault("resource.btn.fetch_online", "🌐 Télécharger cartes satellite WMS (USGS / NASA)"));
        fetchOnlineBtn.setMaxWidth(Double.MAX_VALUE);
        fetchOnlineBtn.getStyleClass().add("button-secondary");
        fetchOnlineBtn.setOnAction(e -> fetchOnlineSatelliteData());

        exportMapsBtn = new Button(I18n.getOrDefault("resource.btn.export_maps", "📤 Exporter cartes (PNG + WorldFile .tfw)"));
        exportMapsBtn.setMaxWidth(Double.MAX_VALUE);
        exportMapsBtn.getStyleClass().add("button-secondary");
        exportMapsBtn.setOnAction(e -> exportMapsWithWorldFiles());

        specsHelpBtn = new Button(I18n.getOrDefault("resource.btn.specs_help", "ℹ️ Spécifications des cartes de biomes & géologie"));
        specsHelpBtn.setMaxWidth(Double.MAX_VALUE);
        specsHelpBtn.getStyleClass().add("button-secondary");
        specsHelpBtn.setOnAction(e -> showEcologyImportFormatHelp());

        mapStatusLabel = new Label();
        mapStatusLabel.getStyleClass().add("value-label");
        mapStatusLabel.setWrapText(true);

        VBox customMapsSection = createSection(mapsSecHeader, new VBox(8,
                createControlRow(mapSourceRowLabel, mapSourceCombo, I18n.getOrDefault("resource.tooltip.map_source", "Sélectionner une carte satellite prédéfinie")),
                createControlRow(biomeMapRowLabel, new VBox(3, biomeBox, biomeFileLabel), "Import d'une carte de biomes au format PNG/JPEG"),
                createControlRow(resourceMapRowLabel, new VBox(3, resourceBox, resourceFileLabel), "Import d'une carte de minerais & géologie multi-canaux"),
                createControlRow(hydroMapRowLabel, new VBox(3, hydroBox, hydroFileLabel), "Import d'une carte hydrographique et réseau de fleuves"),
                fetchOnlineHydroBtn,
                proceduralHydroBtn,
                fetchOnlineBtn,
                exportMapsBtn,
                specsHelpBtn,
                mapStatusLabel
        ));

        // --- Climate maps section (moved from Tab 1) ---
        Label climateMapsHeader = new Label(I18n.getOrDefault("resource.section.climate_maps", "CARTES CLIMATIQUES (TEMPÉRATURE / PRÉCIPITATIONS)"));

        climateMapRowLabel = new Label();
        climateFileLabel = new Label(I18n.get("planet.map.none"));
        climateFileLabel.getStyleClass().add("value-label");
        loadClimateBtn = new Button(I18n.get("planet.map.btn_load"));
        loadClimateBtn.getStyleClass().add("button-secondary");
        loadClimateBtn.setOnAction(e -> chooseClimateMapFile());
        clearClimateBtn = new Button("❌");
        clearClimateBtn.getStyleClass().add("button-secondary");
        clearClimateBtn.setOnAction(e -> { customClimateImage = null; climateFileLabel.setText(I18n.get("planet.map.none")); updatePreviewCanvas(); });
        HBox climateBox = new HBox(5, loadClimateBtn, clearClimateBtn);

        rainfallMapRowLabel = new Label();
        rainfallFileLabel = new Label(I18n.get("planet.map.none"));
        rainfallFileLabel.getStyleClass().add("value-label");
        loadRainfallBtn = new Button(I18n.get("planet.map.btn_load"));
        loadRainfallBtn.getStyleClass().add("button-secondary");
        loadRainfallBtn.setOnAction(e -> chooseRainfallMapFile());
        clearRainfallBtn = new Button("❌");
        clearRainfallBtn.getStyleClass().add("button-secondary");
        clearRainfallBtn.setOnAction(e -> { customRainfallImage = null; rainfallFileLabel.setText(I18n.get("planet.map.none")); updatePreviewCanvas(); });
        HBox rainfallBox = new HBox(5, loadRainfallBtn, clearRainfallBtn);

        seasonalityMapRowLabel = new Label();
        seasonalityFileLabel = new Label(I18n.get("planet.map.none"));
        seasonalityFileLabel.getStyleClass().add("value-label");
        loadSeasonalityBtn = new Button(I18n.get("planet.map.btn_load"));
        loadSeasonalityBtn.getStyleClass().add("button-secondary");
        loadSeasonalityBtn.setOnAction(e -> chooseSeasonalityMapFile());
        clearSeasonalityBtn = new Button("❌");
        clearSeasonalityBtn.getStyleClass().add("button-secondary");
        clearSeasonalityBtn.setOnAction(e -> { customSeasonalityImage = null; seasonalityFileLabel.setText(I18n.get("planet.map.none")); updatePreviewCanvas(); });
        HBox seasonalityBox = new HBox(5, loadSeasonalityBtn, clearSeasonalityBtn);

        fetchOnlineClimateBtn = new Button(I18n.getOrDefault("planet.map.btn_fetch_online_climate", "🌐 Télécharger Climat Satellite NASA/USGS (WMS)"));
        fetchOnlineClimateBtn.setMaxWidth(Double.MAX_VALUE);
        fetchOnlineClimateBtn.getStyleClass().add("button-secondary");
        fetchOnlineClimateBtn.setOnAction(e -> fetchOnlineClimateData());

        climateHelpBtn = new Button(I18n.getOrDefault("planet.map.btn_climate_help", "ℹ️ Spécifications des cartes climatiques"));
        climateHelpBtn.setMaxWidth(Double.MAX_VALUE);
        climateHelpBtn.getStyleClass().add("button-secondary");
        climateHelpBtn.setOnAction(e -> showClimateImportFormatHelp());

        climateStatusLabel = new Label();
        climateStatusLabel.getStyleClass().add("value-label");
        climateStatusLabel.setWrapText(true);

        VBox climateMapsSection = createSection(climateMapsHeader, new VBox(8,
                createControlRow(climateMapRowLabel, new VBox(3, climateBox, climateFileLabel), I18n.getOrDefault("planet.tooltip.climate_map", "Import d'une carte thermique ou combinée RGB")),
                createControlRow(rainfallMapRowLabel, new VBox(3, rainfallBox, rainfallFileLabel), I18n.getOrDefault("planet.tooltip.rainfall_map", "Import d'une carte de précipitations / humidité")),
                createControlRow(seasonalityMapRowLabel, new VBox(3, seasonalityBox, seasonalityFileLabel), I18n.getOrDefault("planet.tooltip.seasonality_map", "Import d'une carte de variance saisonnière")),
                fetchOnlineClimateBtn,
                climateHelpBtn,
                climateStatusLabel
        ));


        // --- 5. Flora & Terrestrial Plant Resources ---
        terrestrialBiomassSlider = createSlider(1.0, 5000.0, 450.0);
        soilCarbonSlider = createSlider(10.0, 10000.0, 1500.0);
        terrestrialBiomassRowLabel = new Label("Biomasse Végétale & Forêts Globale :");
        soilCarbonRowLabel = new Label("Carbone Organique des Sols & Agriculture :");
        floraSecHeader = new Label("VÉGÉTATION & SOLE (MÉTRIQUES)");

        VBox floraSection = new VBox(8,
                createControlRow(terrestrialBiomassRowLabel, terrestrialBiomassSlider, "%.0f GtC", "Stock global de biomasse végétale et forêts (Gigatonnes de Carbone)"),
                createControlRow(soilCarbonRowLabel, soilCarbonSlider, "%.0f GtC", "Stock de carbone organique dans les sols et potentiel agricole (GtC)")
        );

        // --- 6. Fauna & Animal Resources ---
        faunaBiomassSlider = createSlider(0.01, 50.0, 2.0);
        aquaticBiomassSlider = createSlider(0.1, 100.0, 6.0);
        faunaBiomassRowLabel = new Label("Biomasse Faunique Terrestre & Sauvage :");
        aquaticBiomassRowLabel = new Label("Biomasse Marine & Estuarienne :");
        faunaSecHeader = new Label("BIOMASSE ANIMALE (MÉTRIQUES)");

        VBox faunaSection = new VBox(8,
                createControlRow(faunaBiomassRowLabel, faunaBiomassSlider, "%.2f GtC", "Biomasse totale des vertébrés et vertébrés terrestres sauvages (GtC)"),
                createControlRow(aquaticBiomassRowLabel, aquaticBiomassSlider, "%.2f GtC", "Stock de biomasse marine, poissons et phytoplancton (GtC)")
        );

        // --- 7. Minerals & Underground Geology ---
        crustalMetalSlider = createSlider(1.0, 2000.0, 80.0);
        preciousMetalSlider = createSlider(10.0, 50000.0, 1200.0);
        mantleHeatSlider = createSlider(10.0, 400.0, 87.0);
        freshwaterAquiferSlider = createSlider(10.0, 100000.0, 15000.0);
        seismicActivitySlider = createSlider(0.0, 10.0, 2.5);
        volcanicActivitySlider = createSlider(0.0, 8.0, 1.5);
        seismicRowLabel = new Label("Activité Sismique & Tectonique (Mag Richter) :");
        volcanicRowLabel = new Label("Activité Volcanique Globale (VEI) :");

        crustalMetalRowLabel = new Label("Réserves Métalliques Industrielles (Fer/Cuivre) :");
        preciousMetalRowLabel = new Label("Minerais Précieux & Terres Rares (Or/Pt) :");
        mantleHeatRowLabel = new Label("Flux Thermique du Manteau & Tectonique :");
        freshwaterAquiferRowLabel = new Label("Réserves d'Eau Douce & Aquifères :");

        // ---- DOMAIN SEEDS (one per domain, replacing global seed section) ----
        // biomeSeedField is also assigned to seedField for EcologyPreset compatibility.

        // --- DOMAINE 1 : BIOMES & FLORE ---
        ToggleGroup biomeGroup = new ToggleGroup();
        radioProcBiome = new RadioButton(I18n.getOrDefault("resource.mode.procedural_biome", "▶ Biomes Procéduraux"));
        radioImportBiome = new RadioButton(I18n.getOrDefault("resource.mode.import_biome", "📂 Carte de Biomes (PNG)"));
        radioProcBiome.setToggleGroup(biomeGroup);
        radioImportBiome.setToggleGroup(biomeGroup);
        radioProcBiome.setSelected(true);
        radioProcBiome.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        radioImportBiome.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold;");

        biomeSeedField = new TextField("12345");
        randSeedBtn = new Button("🎲");
        randSeedBtn.getStyleClass().add("button-secondary");
        randSeedBtn.setOnAction(e -> { biomeSeedField.setText(String.valueOf(new Random().nextLong(1000000))); updatePreviewCanvas(); });
        HBox biomeSeedBox = new HBox(5, biomeSeedField, randSeedBtn);
        HBox.setHgrow(biomeSeedField, Priority.ALWAYS);
        seedField = biomeSeedField; // EcologyPreset compat

        Button exportBiomeBtn = new Button(I18n.getOrDefault("resource.btn.export_biome", "📤 Exporter la Carte de Biomes Procédurale (PNG)"));
        exportBiomeBtn.setMaxWidth(Double.MAX_VALUE);
        exportBiomeBtn.getStyleClass().add("button-secondary");
        exportBiomeBtn.setOnAction(e -> exportMapsWithWorldFiles());

        VBox biomeProcBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.seed.label", "Graine de génération :")), biomeSeedBox,
                        I18n.getOrDefault("resource.tooltip.seed", "Graine aléatoire pour la distribution procédurale des biomes")),
                floraSection, faunaSection, exportBiomeBtn
        );
        biomeProcBox.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");

        biomeSourceCombo = new ComboBox<>();
        biomeSourceCombo.getItems().addAll("none", "earth", "mars", "venus", "moon");
        biomeSourceCombo.setValue("none");
        biomeSourceCombo.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : I18n.getOrDefault("planet.map." + item, item));
            }
        });
        biomeSourceCombo.setButtonCell(biomeSourceCombo.getCellFactory().call(null));
        biomeSourceCombo.setMaxWidth(Double.MAX_VALUE);

        VBox biomeImportBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.param.map_source", "Corps céleste / Source :")), biomeSourceCombo,
                        "Sélectionner une source satellite prédéfinie"),
                createControlRow(biomeMapRowLabel, new VBox(3, biomeBox, biomeFileLabel), "Import d'une carte de biomes au format PNG/JPEG"),
                specsHelpBtn
        );
        biomeImportBox.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(167,139,250,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        biomeImportBox.setVisible(false);
        biomeImportBox.setManaged(false);

        biomeGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isProc = sel == radioProcBiome;
            biomeProcBox.setVisible(isProc); biomeProcBox.setManaged(isProc);
            biomeImportBox.setVisible(!isProc); biomeImportBox.setManaged(!isProc);
            updatePreviewCanvas();
        });

        biomeDomainSecHeader = new Label(I18n.getOrDefault("resource.domain.biome", "1. DOMAINE BIOMES & FLORE (VÉGÉTATION)"));
        VBox biomeDomainSection = createSection(biomeDomainSecHeader, new VBox(8,
                radioProcBiome, biomeProcBox, radioImportBiome, biomeImportBox
        ));

        // --- DOMAINE 2 : HYDROGRAPHIE & EAU DOUCE ---
        ToggleGroup hydroGroup = new ToggleGroup();
        radioProcHydro = new RadioButton(I18n.getOrDefault("resource.mode.procedural_hydro", "▶ Hydrographie Procédurale"));
        radioImportHydro = new RadioButton(I18n.getOrDefault("resource.mode.import_hydro", "📂 Carte Hydrographique (PNG)"));
        radioProcHydro.setToggleGroup(hydroGroup);
        radioImportHydro.setToggleGroup(hydroGroup);
        radioProcHydro.setSelected(true);
        radioProcHydro.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        radioImportHydro.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold;");

        hydroSeedField = new TextField("23456");
        Button hydroRandBtn = new Button("🎲");
        hydroRandBtn.getStyleClass().add("button-secondary");
        hydroRandBtn.setOnAction(e -> { hydroSeedField.setText(String.valueOf(new Random().nextLong(1000000))); updatePreviewCanvas(); });
        HBox hydroSeedBox = new HBox(5, hydroSeedField, hydroRandBtn);
        HBox.setHgrow(hydroSeedField, Priority.ALWAYS);

        VBox hydroProcBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.seed.label", "Graine de génération :")), hydroSeedBox,
                        "Graine aléatoire pour la génération procédurale des cours d'eau"),
                createControlRow(freshwaterAquiferRowLabel, freshwaterAquiferSlider, "%.0f x10³ km³", "Volume total des nappes phréatiques et aquifères continentaux"),
                proceduralHydroBtn
        );
        hydroProcBox.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");

        hydroSourceCombo = new ComboBox<>();
        hydroSourceCombo.getItems().addAll("none", "earth", "mars");
        hydroSourceCombo.setValue("none");
        hydroSourceCombo.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : I18n.getOrDefault("planet.map." + item, item));
            }
        });
        hydroSourceCombo.setButtonCell(hydroSourceCombo.getCellFactory().call(null));
        hydroSourceCombo.setMaxWidth(Double.MAX_VALUE);

        VBox hydroImportBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.param.map_source", "Corps céleste / Source :")), hydroSourceCombo,
                        "Source satellite pour la carte hydrographique"),
                createControlRow(hydroMapRowLabel, new VBox(3, hydroBox, hydroFileLabel), "Import d'une carte hydrographique et réseau de fleuves"),
                fetchOnlineHydroBtn
        );
        hydroImportBox.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(167,139,250,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        hydroImportBox.setVisible(false);
        hydroImportBox.setManaged(false);

        hydroGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isProc = sel == radioProcHydro;
            hydroProcBox.setVisible(isProc); hydroProcBox.setManaged(isProc);
            hydroImportBox.setVisible(!isProc); hydroImportBox.setManaged(!isProc);
            updatePreviewCanvas();
        });

        hydroDomainSecHeader = new Label(I18n.getOrDefault("resource.domain.hydro", "2. DOMAINE HYDROGRAPHIE & EAU DOUCE"));
        VBox hydroDomainSection = createSection(hydroDomainSecHeader, new VBox(8,
                radioProcHydro, hydroProcBox, radioImportHydro, hydroImportBox
        ));

        // --- DOMAINE 3 : CLIMAT & ATMOSPHÈRE ---
        ToggleGroup climateGroup = new ToggleGroup();
        radioProcClimate = new RadioButton(I18n.getOrDefault("resource.mode.procedural_climate", "▶ Climat Procédural"));
        radioImportClimate = new RadioButton(I18n.getOrDefault("resource.mode.import_climate", "📂 Cartes Climatiques (PNG)"));
        radioProcClimate.setToggleGroup(climateGroup);
        radioImportClimate.setToggleGroup(climateGroup);
        radioProcClimate.setSelected(true);
        radioProcClimate.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        radioImportClimate.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold;");

        climateSeedField = new TextField("34567");
        Button climateRandBtn = new Button("🎲");
        climateRandBtn.getStyleClass().add("button-secondary");
        climateRandBtn.setOnAction(e -> { climateSeedField.setText(String.valueOf(new Random().nextLong(1000000))); updatePreviewCanvas(); });
        HBox climateSeedBox = new HBox(5, climateSeedField, climateRandBtn);
        HBox.setHgrow(climateSeedField, Priority.ALWAYS);

        climateSummaryLabel = new Label(I18n.getOrDefault("resource.climate.summary", "🌡️ Modèle Climatique : Hérité des paramètres planétaires de l'Onglet 1 (Température, Gradient, Pression, O₂, CO₂, Albédo)"));
        climateSummaryLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
        climateSummaryLabel.setWrapText(true);

        VBox climateProcBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.seed.label", "Graine de génération :")), climateSeedBox,
                        "Graine aléatoire pour la variation procédurale du modèle climatique"),
                climateSummaryLabel
        );
        climateProcBox.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");

        climateSourceCombo = new ComboBox<>();
        climateSourceCombo.getItems().addAll(
                "NASA MODIS LST (WMS — températures)",
                "NASA GPM IMERG (WMS — précipitations)",
                "ERA5 Reanalysis (Copernicus)",
                "Koppen-Geiger Classification (PNG)"
        );
        climateSourceCombo.setValue(climateSourceCombo.getItems().get(0));
        climateSourceCombo.setMaxWidth(Double.MAX_VALUE);

        VBox climateImportBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("planet.climate.source_label", "Source de référence :")), climateSourceCombo,
                        "Source de données climatiques de référence"),
                createControlRow(climateMapRowLabel, new VBox(3, climateBox, climateFileLabel), I18n.getOrDefault("planet.tooltip.climate_map", "Import d'une carte thermique ou combinée RGB")),
                createControlRow(rainfallMapRowLabel, new VBox(3, rainfallBox, rainfallFileLabel), I18n.getOrDefault("planet.tooltip.rainfall_map", "Import d'une carte de précipitations / humidité")),
                createControlRow(seasonalityMapRowLabel, new VBox(3, seasonalityBox, seasonalityFileLabel), I18n.getOrDefault("planet.tooltip.seasonality_map", "Import d'une carte de variance saisonnière")),
                fetchOnlineClimateBtn,
                climateHelpBtn
        );
        climateImportBox.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(167,139,250,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        climateImportBox.setVisible(false);
        climateImportBox.setManaged(false);

        climateGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isProc = sel == radioProcClimate;
            climateProcBox.setVisible(isProc); climateProcBox.setManaged(isProc);
            climateImportBox.setVisible(!isProc); climateImportBox.setManaged(!isProc);
            updatePreviewCanvas();
        });

        climateDomainSecHeader = new Label(I18n.getOrDefault("resource.domain.climate", "3. DOMAINE CLIMAT & ATMOSPHÈRE"));
        VBox climateDomainSection = createSection(climateDomainSecHeader, new VBox(8,
                radioProcClimate, climateProcBox, radioImportClimate, climateImportBox
        ));

        // --- DOMAINE 4 : GÉOLOGIE, TECTONIQUE & MINERAIS ---
        ToggleGroup geologyGroup = new ToggleGroup();
        radioProcGeology = new RadioButton(I18n.getOrDefault("resource.mode.procedural_geology", "▶ Géologie Procédurale"));
        radioImportGeology = new RadioButton(I18n.getOrDefault("resource.mode.import_geology", "📂 Carte Géologique (PNG)"));
        radioProcGeology.setToggleGroup(geologyGroup);
        radioImportGeology.setToggleGroup(geologyGroup);
        radioProcGeology.setSelected(true);
        radioProcGeology.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        radioImportGeology.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold;");

        geologySeedField = new TextField("45678");
        Button geologyRandBtn = new Button("🎲");
        geologyRandBtn.getStyleClass().add("button-secondary");
        geologyRandBtn.setOnAction(e -> { geologySeedField.setText(String.valueOf(new Random().nextLong(1000000))); updatePreviewCanvas(); });
        HBox geologySeedBox = new HBox(5, geologySeedField, geologyRandBtn);
        HBox.setHgrow(geologySeedField, Priority.ALWAYS);

        Button exportGeologyBtn = new Button(I18n.getOrDefault("resource.btn.export_geology", "📤 Exporter la Carte Géologique (PNG + WorldFile)"));
        exportGeologyBtn.setMaxWidth(Double.MAX_VALUE);
        exportGeologyBtn.getStyleClass().add("button-secondary");
        exportGeologyBtn.setOnAction(e -> exportMapsWithWorldFiles());

        VBox geologyProcBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.seed.label", "Graine de génération :")), geologySeedBox,
                        "Graine aléatoire pour la distribution des gisements et structures tectoniques"),
                createControlRow(seismicRowLabel, seismicActivitySlider, "%.1f Mag", "Niveau de sismicité planétaire générant des séismes"),
                createControlRow(volcanicRowLabel, volcanicActivitySlider, "%.1f VEI", "Niveau d'activité volcanique générant des éruptions"),
                createControlRow(crustalMetalRowLabel, crustalMetalSlider, "%.0f Gt", "Réserves de métaux industriels dans la croûte (Gigatonnes)"),
                createControlRow(preciousMetalRowLabel, preciousMetalSlider, "%.0f Mt", "Stock d'éléments précieux et terres rares (Mégatonnes)"),
                createControlRow(mantleHeatRowLabel, mantleHeatSlider, "%.1f mW/m²", "Flux thermique du manteau, volcanisme et activité géothermique"),
                exportGeologyBtn
        );
        geologyProcBox.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");

        geologySourceCombo = new ComboBox<>();
        geologySourceCombo.getItems().addAll("none", "earth", "mars", "venus");
        geologySourceCombo.setValue("none");
        geologySourceCombo.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : I18n.getOrDefault("planet.map." + item, item));
            }
        });
        geologySourceCombo.setButtonCell(geologySourceCombo.getCellFactory().call(null));
        geologySourceCombo.setMaxWidth(Double.MAX_VALUE);

        VBox geologyImportBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.param.map_source", "Corps céleste / Source :")), geologySourceCombo,
                        "Source satellite pour la carte géologique"),
                createControlRow(resourceMapRowLabel, new VBox(3, resourceBox, resourceFileLabel), "Import d'une carte géologique & minerais multi-canaux"),
                specsHelpBtn
        );
        geologyImportBox.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(167,139,250,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        geologyImportBox.setVisible(false);
        geologyImportBox.setManaged(false);

        geologyGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isProc = sel == radioProcGeology;
            geologyProcBox.setVisible(isProc); geologyProcBox.setManaged(isProc);
            geologyImportBox.setVisible(!isProc); geologyImportBox.setManaged(!isProc);
            updatePreviewCanvas();
        });

        geologyDomainSecHeader = new Label(I18n.getOrDefault("resource.domain.geology", "4. DOMAINE GÉOLOGIE, TECTONIQUE & MINERAIS"));
        VBox geologyDomainSection = createSection(geologyDomainSecHeader, new VBox(8,
                radioProcGeology, geologyProcBox, radioImportGeology, geologyImportBox
        ));

        // Legacy compatibility
        radioProcEco = radioProcBiome;
        radioImportEco = radioImportBiome;

        controlsBox.getChildren().addAll(
                headerLabel,
                ecologyPresetBar,
                planetSection,
                biomeDomainSection,
                hydroDomainSection,
                climateDomainSection,
                geologyDomainSection,
                exportMapsBtn
        );


        ScrollPane scrollControls = new ScrollPane(controlsBox);
        scrollControls.setFitToWidth(true);
        scrollControls.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // --- Center / Right View: Interactive Visualization & Summary ---
        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(10));

        rightViewTitle = new Label(I18n.getOrDefault("resource.title.right_view", "AFFICHEUR & CARTOGRAPHIE DES RESSOURCES"));
        rightViewTitle.getStyleClass().add("label-header");

        // View Mode Selector
        viewModeCombo = new ComboBox<>();
        viewModeCombo.getItems().addAll(
                "🌿 Carte des Biomes & Végétation (GtC)",
                "🪨 Carte Géologique & Métaux (Gt Fer/Cuivre)",
                "🌋 Flux Thermique & Ceintures Tectoniques (mW/m²)",
                "💧 Aquifères & Biomasse Aquatique (10³ km³)",
                "🌊 Carte Hydrographique & Fleuves (Déclivité & Cours d'eau)"
        );
        viewModeCombo.setValue(viewModeCombo.getItems().get(0));
        viewModeCombo.setMaxWidth(380);
        viewModeCombo.setOnAction(e -> {
            updateLegend();
            updatePreviewCanvas();
        });

        // 2D Map Canvas
        mapPreviewCanvas = new Canvas(640, 360);
        mapPreviewCanvas.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 0);");
        Tooltip.install(mapPreviewCanvas, new Tooltip(I18n.getOrDefault("resource.tooltip.preview", "Aperçu dynamique de la répartition géographique des ressources")));

        // Interactive Zoom & Pan Handlers
        mapPreviewCanvas.setOnScroll(e -> {
            double delta = e.getDeltaY();
            double factor = delta > 0 ? 1.15 : 0.85;
            zoomFactor = Math.max(0.5, Math.min(20.0, zoomFactor * factor));
            updatePreviewCanvas();
        });
        mapPreviewCanvas.setOnMousePressed(e -> {
            dragStartX = e.getX();
            dragStartY = e.getY();
        });
        mapPreviewCanvas.setOnMouseDragged(e -> {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            panX += dx;
            panY += dy;
            dragStartX = e.getX();
            dragStartY = e.getY();
            updatePreviewCanvas();
        });
        mapPreviewCanvas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                zoomFactor = 1.0;
                panX = 0.0;
                panY = 0.0;
                updatePreviewCanvas();
            }
        });

        legendBar = new HBox(10);
        legendBar.setAlignment(Pos.CENTER);
        legendBar.setPadding(new Insets(5, 10, 5, 10));
        legendBar.getStyleClass().add("card-section");
        updateLegend();

        summaryLabel = new Label("🌍 Générez ou chargez des cellules planétaires pour activer la carte.");
        summaryLabel.getStyleClass().add("control-label");
        summaryLabel.setStyle("-fx-alignment: center;");
        summaryLabel.setWrapText(true);

        Label saveHintLabel = new Label(I18n.getOrDefault(
                "resource.hint.save_preset",
                "💾  Utilisez le bouton « Enregistrer » (barre de préréglage) pour nommer et sauvegarder la configuration écologique actuelle dans la liste des préréglages disponibles."));
        saveHintLabel.setWrapText(true);
        saveHintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-padding: 8 12; " +
                "-fx-background-color: rgba(16,185,129,0.07); -fx-background-radius: 6; " +
                "-fx-border-color: rgba(16,185,129,0.2); -fx-border-radius: 6;");

        centerBox.getChildren().addAll(rightViewTitle, viewModeCombo, mapPreviewCanvas, legendBar, summaryLabel, saveHintLabel);

        setLeft(scrollControls);
        setCenter(centerBox);

        updatePlanetContextDisplay();
        updatePreviewCanvas();
    }

    private void autoDeriveEcologyFromPlanet() {
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        if (p == null) p = PlanetPreset.EARTH_LIKE;

        // Calculate surface area scaling factor relative to Earth
        double radiusRatio = p.radiusKm() / 6371.0;
        double areaScale = Math.pow(radiusRatio, 2);

        // Water ratio modifier
        double waterMod = Math.max(0.05, 1.0 + p.waterLevel());
        double tempMod = Math.max(0.1, 1.0 - Math.abs(p.averageTempC() - 15.0) / 60.0);

        terrestrialBiomassSlider.setValue(450.0 * areaScale * waterMod * tempMod);
        soilCarbonSlider.setValue(1500.0 * areaScale * tempMod);
        faunaBiomassSlider.setValue(2.0 * areaScale * waterMod * tempMod);
        aquaticBiomassSlider.setValue(6.0 * areaScale * (1.0 + p.waterLevel() * 1.5));
        freshwaterAquiferSlider.setValue(15000.0 * areaScale * waterMod);

        updateSummary();
        updatePreviewCanvas();
        logger.info("Auto-derived ecological parameters for planet {}", p.name());
    }

    private void autoDeriveGeologyFromPlanet() {
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        if (p == null) p = PlanetPreset.EARTH_LIKE;

        double radiusRatio = p.radiusKm() / 6371.0;
        double volumeScale = Math.pow(radiusRatio, 3);

        crustalMetalSlider.setValue(80.0 * volumeScale);
        preciousMetalSlider.setValue(1200.0 * volumeScale);
        // Smaller bodies cool faster -> lower mantle heat flow
        mantleHeatSlider.setValue(Math.min(300.0, 87.0 * radiusRatio));

        updateSummary();
        updatePreviewCanvas();
        logger.info("Auto-derived geological parameters for planet {}", p.name());
    }

    private void updatePlanetContextDisplay() {
        if (planetContextLabel == null) return;
        PlanetPreset preset = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        if (preset == null) preset = PlanetPreset.EARTH_LIKE;

        int cellCount = activeCells != null ? activeCells.size() : 12482;
        String text = String.format(
                "🪐 Planète Héritée : %s | Rayon : %,.0f km | Temp : %.1f°C | Eau : %.0f%% | Cellules : %,d",
                preset.name(),
                preset.radiusKm(),
                preset.averageTempC(),
                preset.waterLevel() * 100.0,
                cellCount
        );
        planetContextLabel.setText(text);
    }

    private void adaptResourceSlidersToPlanet(PlanetPreset p) {
        if (p == null) return;
        if (p.isSatellite() || p.radiusKm() < 3000) { // Moon/Titan
            terrestrialBiomassSlider.setValue(10.0);
            soilCarbonSlider.setValue(50.0);
            faunaBiomassSlider.setValue(0.01);
            crustalMetalSlider.setValue(150.0);
            mantleHeatSlider.setValue(25.0);
        } else if (p.waterLevel() < -0.2) { // Arid / Mars
            terrestrialBiomassSlider.setValue(50.0);
            soilCarbonSlider.setValue(200.0);
            aquaticBiomassSlider.setValue(0.2);
            freshwaterAquiferSlider.setValue(1200.0);
        } else if (p.waterLevel() > 0.4) { // Ocean World
            terrestrialBiomassSlider.setValue(120.0);
            aquaticBiomassSlider.setValue(25.0);
            freshwaterAquiferSlider.setValue(45000.0);
        } else { // Earth-Like
            terrestrialBiomassSlider.setValue(450.0);
            soilCarbonSlider.setValue(1500.0);
            faunaBiomassSlider.setValue(2.0);
            aquaticBiomassSlider.setValue(6.0);
            crustalMetalSlider.setValue(80.0);
            preciousMetalSlider.setValue(1200.0);
            mantleHeatSlider.setValue(87.0);
            freshwaterAquiferSlider.setValue(15000.0);
        }
        updateSummary();
    }

    private void applyEcologyPreset(EcologyPreset p) {
        if (p == null) return;
        isUpdatingFromPreset = true;

        terrestrialBiomassSlider.setValue(p.terrestrialBiomassGtC());
        soilCarbonSlider.setValue(p.soilOrganicCarbonGtC());
        faunaBiomassSlider.setValue(p.faunaBiomassGtC());
        aquaticBiomassSlider.setValue(p.aquaticBiomassGtC());
        crustalMetalSlider.setValue(p.crustalMetalOresGt());
        preciousMetalSlider.setValue(p.preciousMetalOresMt());
        mantleHeatSlider.setValue(p.mantleHeatFlowMwM2());
        freshwaterAquiferSlider.setValue(p.freshwaterReserveKm3());

        if (p.seed() != 0 && seedField != null) {
            seedField.setText(String.valueOf(p.seed()));
        }

        if (p.customBiomeBase64() != null) {
            customBiomeImage = ImageMapLoader.base64PngToImage(p.customBiomeBase64());
            if (biomeFileLabel != null) biomeFileLabel.setText("🌿 Preset Biomes");
        }
        if (p.customResourceBase64() != null) {
            customResourceImage = ImageMapLoader.base64PngToImage(p.customResourceBase64());
            if (resourceFileLabel != null) resourceFileLabel.setText("🪨 Preset Resources");
        }
        if (p.customHydroBase64() != null) {
            customHydroImage = ImageMapLoader.base64PngToImage(p.customHydroBase64());
            if (hydroFileLabel != null) hydroFileLabel.setText("💧 Preset Hydro");
        }
        if (p.customClimateBase64() != null) {
            customClimateImage = ImageMapLoader.base64PngToImage(p.customClimateBase64());
            if (climateFileLabel != null) climateFileLabel.setText("🌡️ Preset Climate");
        }
        if (p.customRainfallBase64() != null) {
            customRainfallImage = ImageMapLoader.base64PngToImage(p.customRainfallBase64());
            if (rainfallFileLabel != null) rainfallFileLabel.setText("🌧️ Preset Rainfall");
        }
        if (p.customSeasonalityBase64() != null) {
            customSeasonalityImage = ImageMapLoader.base64PngToImage(p.customSeasonalityBase64());
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText("☀️ Preset Seasonality");
        }

        isUpdatingFromPreset = false;
        updateSummary();
        updatePreviewCanvas();
    }

    private void loadCustomBiomeMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Charger une carte de biomes (PNG/JPEG)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customBiomeImage = new Image(new FileInputStream(file));
                biomeFileLabel.setText("📷 " + file.getName());
                updateSummary();
                updatePreviewCanvas();
            } catch (Exception ex) {
                logger.error("Failed to load custom biome map", ex);
            }
        }
    }

    private void loadCustomResourceMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Charger une carte géologique & minerais (PNG/JPEG)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customResourceImage = new Image(new FileInputStream(file));
                resourceFileLabel.setText("🪨 " + file.getName());
                updateSummary();
                updatePreviewCanvas();
            } catch (Exception ex) {
                logger.error("Failed to load custom resource map", ex);
            }
        }
    }

    private void chooseClimateMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.dialog.climate_load", "Charger une carte climatique / thermique (PNG/JPEG)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customClimateImage = new Image(new FileInputStream(file));
                climateFileLabel.setText("🌡️ " + file.getName());
                updatePreviewCanvas();
            } catch (Exception ex) {
                logger.error("Failed to load climate map", ex);
            }
        }
    }

    private void chooseRainfallMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.dialog.rainfall_load", "Charger une carte de précipitations (PNG/JPEG)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customRainfallImage = new Image(new FileInputStream(file));
                rainfallFileLabel.setText("🌧️ " + file.getName());
                updatePreviewCanvas();
            } catch (Exception ex) {
                logger.error("Failed to load rainfall map", ex);
            }
        }
    }

    private void chooseSeasonalityMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.dialog.seasonality_load", "Charger une carte de saisonnalité (PNG/JPEG)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customSeasonalityImage = new Image(new FileInputStream(file));
                seasonalityFileLabel.setText("🍂 " + file.getName());
                updatePreviewCanvas();
            } catch (Exception ex) {
                logger.error("Failed to load seasonality map", ex);
            }
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
        if (climateStatusLabel != null)
            climateStatusLabel.setText(I18n.getOrDefault("planet.map.status_fetching", "Téléchargement WMS en cours..."));
        onlineMapService.fetchClimateMapAsync(body).thenAccept(img -> javafx.application.Platform.runLater(() -> {
            if (img != null) {
                customClimateImage = img;
                climateFileLabel.setText("🌐 " + body.getName() + " MODIS Thermal WMS");
                if (climateStatusLabel != null) climateStatusLabel.setText(I18n.get("planet.map.status_success"));
                updatePreviewCanvas();
            }
        }));
        onlineMapService.fetchRainfallMapAsync(body).thenAccept(img -> javafx.application.Platform.runLater(() -> {
            if (img != null) {
                customRainfallImage = img;
                rainfallFileLabel.setText("🌐 " + body.getName() + " GPM Rainfall WMS");
                updatePreviewCanvas();
            }
        }));
    }

    private void showClimateImportFormatHelp() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle(I18n.getOrDefault("planet.dialog.climate_help_title", "Spécifications des Cartes Climatiques"));
        dialog.setHeaderText(I18n.getOrDefault("planet.dialog.climate_help_header", "Formats d'images supportés pour l'importation du Climat"));
        dialog.setContentText(
            "Images PNG/JPEG en ratio 2:1 (ex: 2048×1024 px, projection équirectangulaire).\n\n" +
            "1. TROIS CARTES SÉPARÉES :\n" +
            "   • Températures : Noir=−50°C, Blanc=+50°C\n" +
            "   • Précipitations : Noir=0 mm/an, Blanc=3000 mm/an\n" +
            "   • Saisonnalité : Noir=0°C, Blanc=50°C d'amplitude\n\n" +
            "2. CARTE COMBINÉE RGB :\n" +
            "   • R = Température  |  G = Précipitations  |  B = Saisonnalité\n\n" +
            "3. WMS NASA : Bouton « Télécharger Climat Satellite »"
        );
        dialog.showAndWait();
    }

    private boolean validateTerrainMapCompatibility(String sourceKey) {
        if ("none".equals(sourceKey) || sourceKey == null) return true;

        PlanetPreset activePreset = activePlanetPreset;
        if (activePreset == null && planetPresetCombo != null) {
            activePreset = planetPresetCombo.getValue();
        }

        String planetName = activePreset != null ? activePreset.name().toLowerCase() : "earth";

        boolean isCompatible = true;
        if ("earth".equals(sourceKey) && !planetName.contains("earth") && !planetName.contains("terrestre") && !planetName.contains("standard")) {
            isCompatible = false;
        } else if ("mars".equals(sourceKey) && !planetName.contains("mars")) {
            isCompatible = false;
        } else if ("venus".equals(sourceKey) && !planetName.contains("venus")) {
            isCompatible = false;
        } else if ("moon".equals(sourceKey) && !planetName.contains("lune") && !planetName.contains("moon")) {
            isCompatible = false;
        }

        if (!isCompatible) {
            String activeDisplayName = activePreset != null ? activePreset.name() : "Standard";
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(I18n.getOrDefault("planet.dialog.incompatible_title", "Incompatibilité de Terrain"));
            alert.setHeaderText("Incompatibilité Terrain / Carte Externe");
            alert.setContentText(String.format(
                "La carte de ressources/biomes '%s' ne peut pas être chargée sur le terrain actuel '%s'.\n\n" +
                "Motif : Le relief, le climat et les biomes du monde sélectionné à l'onglet 1 ne correspondent pas avec cette carte externe.",
                sourceKey.toUpperCase(), activeDisplayName
            ));
            alert.showAndWait();
            if (ecoCompatibilityLabel != null) {
                ecoCompatibilityLabel.setText(String.format("⚠️ Incompatibilité : Carte '%s' rejetée sur terrain '%s'", sourceKey.toUpperCase(), activeDisplayName));
                ecoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            }
            return false;
        }

        if (ecoCompatibilityLabel != null) {
            String activeDisplayName = activePreset != null ? activePreset.name() : "Standard";
            ecoCompatibilityLabel.setText(String.format("✅ Carte '%s' vérifiée et compatible avec le terrain '%s'", sourceKey.toUpperCase(), activeDisplayName));
            ecoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
        }
        return true;
    }

    private void applyPresetMapSource(String sourceKey) {
        if ("none".equals(sourceKey)) {
            customBiomeImage = null;
            customResourceImage = null;
            customHydroImage = null;
            biomeFileLabel.setText(I18n.get("planet.map.none"));
            resourceFileLabel.setText(I18n.get("planet.map.none"));
            if (hydroFileLabel != null) hydroFileLabel.setText(I18n.get("planet.map.none"));
            if (ecoCompatibilityLabel != null) {
                ecoCompatibilityLabel.setText("🪐 Aucune carte externe chargée — Mode procédural actif");
                ecoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            }
            updatePreviewCanvas();
            updateSummary();
            return;
        }

        if (!validateTerrainMapCompatibility(sourceKey)) {
            mapSourceCombo.setValue("none");
            return;
        }

        if ("earth".equals(sourceKey)) {
            try (var biomeStream = getClass().getResourceAsStream("/maps/earth_biome.png")) {
                if (biomeStream != null) {
                    customBiomeImage = new Image(biomeStream);
                    biomeFileLabel.setText("📷 Earth MODIS Biomes");
                }
            } catch (Exception ex) {
                logger.warn("Could not load default earth biome resource map", ex);
            }
            fetchOnlineHydroData();
        } else if ("mars".equals(sourceKey)) {
            biomeFileLabel.setText("📷 Mars MOLA Geology");
        }
        updatePreviewCanvas();
        updateSummary();
    }

    private void loadCustomHydroMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Charger une carte hydrographique (PNG/JPEG)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customHydroImage = new Image(new FileInputStream(file));
                if (hydroFileLabel != null) hydroFileLabel.setText("🌊 " + file.getName());
                updateSummary();
                updatePreviewCanvas();
            } catch (Exception ex) {
                logger.error("Failed to load custom hydro map", ex);
            }
        }
    }

    private void fetchOnlineHydroData() {
        String sourceKey = mapSourceCombo != null ? mapSourceCombo.getValue() : "earth";
        OnlineMapService.CelestialBody body = switch (sourceKey) {
            case "mars" -> OnlineMapService.CelestialBody.MARS;
            case "moon" -> OnlineMapService.CelestialBody.MOON;
            case "venus" -> OnlineMapService.CelestialBody.VENUS;
            default -> OnlineMapService.CelestialBody.EARTH;
        };

        if (mapStatusLabel != null)
            mapStatusLabel.setText(I18n.getOrDefault("planet.map.status_fetching", "Téléchargement hydrographie satellite WMS en cours..."));

        onlineMapService.fetchHydroMapAsync(body).thenAccept(img -> javafx.application.Platform.runLater(() -> {
            if (img != null) {
                customHydroImage = img;
                if (hydroFileLabel != null) hydroFileLabel.setText("🌐 " + body.getName() + " NASA SWBD Water/Hydro WMS");
                if (mapStatusLabel != null) mapStatusLabel.setText(I18n.getOrDefault("planet.map.status_success", "Carte hydrographique téléchargée avec succès."));
                updatePreviewCanvas();
                updateSummary();
            } else {
                if (mapStatusLabel != null) mapStatusLabel.setText("Information : Basculement sur la carte hydrographique procédurale (déclivité).");
                generateProceduralHydrography();
            }
        }));
    }

    private void generateProceduralHydrography() {
        customHydroImage = null;
        if (hydroFileLabel != null) hydroFileLabel.setText("⚡ Hydrographie Procédurale (Fleuves/Déclivité)");
        if (mapStatusLabel != null) mapStatusLabel.setText("⚡ Carte hydrographique procédurale générée par calcul de déclivité et de bassins versants.");
        if (viewModeCombo != null && viewModeCombo.getItems().size() > 4) {
            viewModeCombo.setValue(viewModeCombo.getItems().get(4));
        }
        updateLegend();
        updatePreviewCanvas();
        updateSummary();
    }

    private void fetchOnlineSatelliteData() {
        String sourceKey = mapSourceCombo.getValue();
        OnlineMapService.CelestialBody body = switch (sourceKey) {
            case "mars" -> OnlineMapService.CelestialBody.MARS;
            case "moon" -> OnlineMapService.CelestialBody.MOON;
            case "venus" -> OnlineMapService.CelestialBody.VENUS;
            default -> OnlineMapService.CelestialBody.EARTH;
        };

        mapStatusLabel.setText(I18n.getOrDefault("planet.map.status_fetching", "Téléchargement WMS en cours..."));

        if (body.getBiomeWmsUrl() != null) {
            onlineMapService.fetchBiomeMapAsync(body).thenAccept(img -> {
                javafx.application.Platform.runLater(() -> {
                    if (img != null) {
                        customBiomeImage = img;
                        biomeFileLabel.setText("🌐 " + body.getName() + " WMS Biomes");
                        mapStatusLabel.setText(I18n.getOrDefault("planet.map.status_success", "Carte satellite téléchargée avec succès."));
                        updatePreviewCanvas();
                        updateSummary();
                    } else {
                        mapStatusLabel.setText(I18n.getOrDefault("planet.map.status_error", "Erreur lors du téléchargement."));
                    }
                });
            });
        } else {
            mapStatusLabel.setText("Information : Pas de WMS spécifique pour " + body.getName() + ", utilisation du modèle procédural.");
        }
    }

    private void exportMapsWithWorldFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter l'image de carte avec ESRI World File (.tfw)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Carte PNG", "*.png"));
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            WritableImage wimg = new WritableImage((int) mapPreviewCanvas.getWidth(), (int) mapPreviewCanvas.getHeight());
            mapPreviewCanvas.snapshot(null, wimg);
            mapLoader.exportMapToPngAndWorldFile(wimg, file);
        }
    }

    private void showEcologyImportFormatHelp() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Spécifications des Cartes de Biomes & Géologie");
        dialog.setHeaderText("Formats d'images et données géologiques attendus");
        dialog.setContentText(
                "Vous pouvez importer des cartes de biomes et de géologie sous forme d'images PNG/JPEG au ratio 2:1 (projection équirectangulaire) :\n\n" +
                "1. CARTE DE BIOMES (ÉCOSYSSTÈMES) :\n" +
                "   • Océan Profond : RGB(0, 0, 100)\n" +
                "   • Océan : RGB(0, 50, 200)\n" +
                "   • Plage / Littoral : RGB(240, 220, 150)\n" +
                "   • Plaines / Prairies : RGB(100, 200, 50)\n" +
                "   • Forêt Tempérée : RGB(20, 120, 20)\n" +
                "   • Jungle Tropicale : RGB(0, 80, 0)\n" +
                "   • Désert Aride : RGB(255, 200, 50)\n" +
                "   • Collines : RGB(150, 150, 100)\n" +
                "   • Montagnes : RGB(100, 100, 100)\n" +
                "   • Toundra : RGB(150, 200, 220)\n" +
                "   • Neige / Glaciers : RGB(255, 255, 255)\n\n" +
                "2. CARTE GÉOLOGIQUE ET MINERAIS (MULTI-CANAUX RGB) :\n" +
                "   • Canal Rouge (R) = Gisements Métalliques (Fer, Cuivre, Bronze)\n" +
                "   • Canal Vert (V) = Densité du Bois, Forêts et Biomasse Végétale\n" +
                "   • Canal Bleu (B) = Nappe Phréatique et Poissonnerie Aquatique\n\n" +
                "3. GÉORÉFÉRENCEMENT ESRI WORLD FILE (.tfw) :\n" +
                "   • L'export génère automatiquement un fichier compagnon .tfw pour l'alignement dans les logiciels GIS (QGIS, ArcGIS)."
        );
        dialog.showAndWait();
    }

    private void updateLegend() {
        if (legendBar == null) return;
        legendBar.getChildren().clear();

        int selectedIdx = viewModeCombo != null ? viewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        if (selectedIdx == 0) { // Biomes
            addLegendItem("DEEP_OCEAN", Color.rgb(0, 0, 100), "Abysses");
            addLegendItem("OCEAN", Color.rgb(0, 50, 200), "Océan");
            addLegendItem("PLAINS", Color.rgb(100, 200, 50), "Plaines");
            addLegendItem("FOREST", Color.rgb(20, 120, 20), "Forêt");
            addLegendItem("JUNGLE", Color.rgb(0, 80, 0), "Jungle");
            addLegendItem("DESERT", Color.rgb(255, 200, 50), "Désert");
            addLegendItem("MOUNTAINS", Color.rgb(100, 100, 100), "Montagnes");
        } else if (selectedIdx == 1) { // Minerals / Ores
            addLegendItem("LOW", Color.rgb(30, 40, 50), "Faible Deposit");
            addLegendItem("MED", Color.rgb(180, 100, 40), "Moyen");
            addLegendItem("HIGH", Color.rgb(240, 60, 20), "Riche Gisements");
        } else if (selectedIdx == 2) { // Mantle / Tectonic
            addLegendItem("LOW", Color.rgb(40, 40, 60), "Inerte");
            addLegendItem("MED", Color.rgb(180, 80, 30), "Tectonique Modérée");
            addLegendItem("HIGH", Color.rgb(240, 20, 20), "Magmatisme Élevé");
        } else if (selectedIdx == 3) { // Aquifer / Aquatic
            addLegendItem("LOW", Color.rgb(20, 40, 80), "Aride / Sec");
            addLegendItem("MED", Color.rgb(40, 120, 200), "Abondance Modérée");
            addLegendItem("HIGH", Color.rgb(0, 220, 255), "Aquifère / Poissonnerie");
        } else { // Hydrography & River Networks
            addLegendItem("DEEP_OCEAN", Color.rgb(15, 23, 42), "Océan / Abysses");
            addLegendItem("MAJOR_RIVER", Color.rgb(2, 132, 199), "Fleuves Majeurs");
            addLegendItem("RIVER_STREAM", Color.rgb(56, 189, 248), "Cours d'Eau & Rivières");
            addLegendItem("TRIBUTARY", Color.rgb(20, 184, 166), "Affluents & Ruisseaux");
            addLegendItem("LAND_SLOPE", Color.rgb(75, 85, 99), "Relief / Déclivité");
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

    private void updatePreviewCanvas() {
        if (mapPreviewCanvas == null) return;

        GraphicsContext gc = mapPreviewCanvas.getGraphicsContext2D();

        int canvasW = (int) mapPreviewCanvas.getWidth();
        int canvasH = (int) mapPreviewCanvas.getHeight();

        int mode = viewModeCombo != null ? viewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        PlanetPreset planet = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        if (planet == null) planet = PlanetPreset.EARTH_LIKE;

        boolean isCustomMapActive = (mode == 0 && customBiomeImage != null)
                || (mode == 1 && customResourceImage != null)
                || (mode == 4 && customHydroImage != null);

        int w = isCustomMapActive ? canvasW : 320;
        int h = isCustomMapActive ? canvasH : 160;

        WritableImage buffer = new WritableImage(w, h);
        PixelWriter pw = buffer.getPixelWriter();

        PixelReader customBiomeReader = customBiomeImage != null ? customBiomeImage.getPixelReader() : null;
        PixelReader customResReader = customResourceImage != null ? customResourceImage.getPixelReader() : null;
        PixelReader customHydroReader = customHydroImage != null ? customHydroImage.getPixelReader() : null;
        double wBio = customBiomeImage != null ? customBiomeImage.getWidth() : 0;
        double hBio = customBiomeImage != null ? customBiomeImage.getHeight() : 0;
        double wRes = customResourceImage != null ? customResourceImage.getWidth() : 0;
        double hRes = customResourceImage != null ? customResourceImage.getHeight() : 0;
        double wHydro = customHydroImage != null ? customHydroImage.getWidth() : 0;
        double hHydro = customHydroImage != null ? customHydroImage.getHeight() : 0;

        double tBiomass = terrestrialBiomassSlider != null ? terrestrialBiomassSlider.getValue() : 450.0;
        double mMetal = crustalMetalSlider != null ? crustalMetalSlider.getValue() : 80.0;
        double mHeat = mantleHeatSlider != null ? mantleHeatSlider.getValue() : 87.0;
        double fAquifer = freshwaterAquiferSlider != null ? freshwaterAquiferSlider.getValue() : 15000.0;

        for (int py = 0; py < h; py++) {
            double y_base = (py - h / 2.0 - panY) / zoomFactor + h / 2.0;
            if (y_base < 0 || y_base >= h) {
                for (int px = 0; px < w; px++) pw.setColor(px, py, Color.rgb(15, 23, 42));
                continue;
            }
            double lat = 90.0 - (y_base / (double) h) * 180.0;

            for (int px = 0; px < w; px++) {
                double x_base = (px - w / 2.0 - panX) / zoomFactor + w / 2.0;
                if (x_base < 0 || x_base >= w) {
                    pw.setColor(px, py, Color.rgb(15, 23, 42));
                    continue;
                }
                double lon = -180.0 + (x_base / (double) w) * 360.0;

                Color pxColor;

                if (mode == 0) { // Biome Map
                    if (customBiomeReader != null) {
                        int bx = (int) Math.min((x_base / (double) w) * wBio, wBio - 1);
                        int by = (int) Math.min((y_base / (double) h) * hBio, hBio - 1);
                        pxColor = customBiomeReader.getColor(bx, by);
                    } else {
                        var point = generator.getPlanetPoint(lat, lon, planet);
                        pxColor = mapLoader.getBiomeTargetColor(point.biome());
                    }
                } else if (mode == 1) { // Mineral / Geology Map
                    if (customResReader != null) {
                        int rx = (int) Math.min((x_base / (double) w) * wRes, wRes - 1);
                        int ry = (int) Math.min((y_base / (double) h) * hRes, hRes - 1);
                        Color c = customResReader.getColor(rx, ry);
                        pxColor = Color.rgb((int)(c.getRed()*255), (int)(c.getRed()*180), (int)(c.getRed()*100));
                    } else {
                        var point = generator.getPlanetPoint(lat, lon, planet);
                        if (point.elevation() < 0) {
                            pxColor = Color.rgb(15, 23, 42); // Slate ocean floor
                        } else {
                            double metalDensity = Math.min(1.0, (Math.abs(point.elevation()) / 8848.0 + 0.2) * (mMetal / 80.0));
                            int r = (int) Math.min(255, 40 + metalDensity * 215);
                            int g = (int) Math.min(255, 30 + metalDensity * 120);
                            int b = (int) Math.min(255, 20 + metalDensity * 40);
                            pxColor = Color.rgb(r, g, b);
                        }
                    }
                } else if (mode == 2) { // Mantle Heat & Tectonic Map
                    var point = generator.getPlanetPoint(lat, lon, planet);
                    double heatDensity = Math.min(1.0, (mHeat / 150.0) * (0.4 + Math.abs(point.elevation()) / 5000.0));
                    int r = (int) Math.min(255, 30 + heatDensity * 220);
                    int g = (int) Math.min(255, 20 + heatDensity * 90);
                    int b = (int) Math.min(255, 40 + (1.0 - heatDensity) * 100);
                    pxColor = Color.rgb(r, g, b);
                } else if (mode == 3) { // Aquatic & Aquifer Map
                    var point = generator.getPlanetPoint(lat, lon, planet);
                    if (point.elevation() < 0) {
                        double aquaDensity = Math.min(1.0, (Math.abs(point.elevation()) / 5000.0));
                        int r = (int) Math.min(255, 0 + aquaDensity * 40);
                        int g = (int) Math.min(255, 80 + aquaDensity * 160);
                        int b = (int) Math.min(255, 180 + aquaDensity * 75);
                        pxColor = Color.rgb(r, g, b);
                    } else {
                        double waterTable = Math.min(1.0, (point.rainfall() / 2500.0) * (fAquifer / 15000.0));
                        pxColor = Color.rgb((int)(30 + waterTable * 40), (int)(60 + waterTable * 100), (int)(90 + waterTable * 140));
                    }
                } else { // Mode 4: Hydrography & River Networks
                    if (customHydroReader != null) {
                        int hx = (int) Math.min((x_base / (double) w) * wHydro, wHydro - 1);
                        int hy = (int) Math.min((y_base / (double) h) * hHydro, hHydro - 1);
                        pxColor = customHydroReader.getColor(hx, hy);
                    } else {
                        var point = generator.getPlanetPoint(lat, lon, planet);
                        if (point.elevation() < planet.waterLevel()) {
                            pxColor = Color.rgb(15, 23, 42); // Sea/Ocean
                        } else {
                            double riverFlow = point.riverFlow();
                            double declivity = point.declivity();
                            if (riverFlow > 0.42) {
                                pxColor = Color.rgb(2, 132, 199); // Fleuve Majeur
                            } else if (riverFlow > 0.28) {
                                pxColor = Color.rgb(56, 189, 248); // Cours d'Eau
                            } else if (riverFlow > 0.16) {
                                pxColor = Color.rgb(20, 184, 166); // Affluent
                            } else {
                                int r = (int) Math.min(255, 45 + declivity * 100);
                                int g = (int) Math.min(255, 60 + declivity * 80);
                                int b = (int) Math.min(255, 55 + declivity * 50);
                                pxColor = Color.rgb(r, g, b);
                            }
                        }
                    }
                }

                pw.setColor(px, py, pxColor);
            }
        }

        gc.drawImage(buffer, 0, 0, canvasW, canvasH);
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
            if (isUpdatingFromPreset) return;
            if (!slider.isValueChanging()) {
                if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
                updateSummary();
                updatePreviewCanvas();
            }
        });
        slider.setOnMouseReleased(e -> {
            if (isUpdatingFromPreset) return;
            if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
            updateSummary();
            updatePreviewCanvas();
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

    private void updateSummary() {
        int count = activeCells != null ? activeCells.size() : 0;
        if (summaryLabel == null) return;

        summaryLabel.setText(String.format(
                "🌍 Cellules H3 Actives : %,d\n" +
                "🌲 Végétale : %.0f GtC  |  🌾 Sols : %.0f GtC  |  🦌 Faune : %.2f GtC\n" +
                "⛏️ Métaux : %.0f Gt  |  💎 Précieux : %.0f Mt  |  🌋 Manteau : %.1f mW/m²  |  💧 Aquifères : %.0f x10³ km³",
                count,
                terrestrialBiomassSlider != null ? terrestrialBiomassSlider.getValue() : 450.0,
                soilCarbonSlider != null ? soilCarbonSlider.getValue() : 1500.0,
                faunaBiomassSlider != null ? faunaBiomassSlider.getValue() : 2.0,
                crustalMetalSlider != null ? crustalMetalSlider.getValue() : 80.0,
                preciousMetalSlider != null ? preciousMetalSlider.getValue() : 1200.0,
                mantleHeatSlider != null ? mantleHeatSlider.getValue() : 87.0,
                freshwaterAquiferSlider != null ? freshwaterAquiferSlider.getValue() : 15000.0
        ));
    }

    public void updateTexts() {
        if (headerLabel != null) headerLabel.setText(I18n.getOrDefault("resource.title", "DISTRIBUTION DES RESSOURCES & ÉCOLOGIE (MÉTRIQUES SCIENTIFIQUES)"));
        if (planetSectionHeader != null) planetSectionHeader.setText(I18n.getOrDefault("resource.section.planet_preset", "PARAMÈTRES PLANÉTAIRES (ONGLET 1)"));
        if (biomeDomainSecHeader != null) biomeDomainSecHeader.setText(I18n.getOrDefault("resource.domain.biome", "1. DOMAINE BIOMES & FLORE (VÉGÉTATION)"));
        if (hydroDomainSecHeader != null) hydroDomainSecHeader.setText(I18n.getOrDefault("resource.domain.hydro", "2. DOMAINE HYDROGRAPHIE & EAU DOUCE"));
        if (climateSummaryLabel != null) climateSummaryLabel.setText(I18n.getOrDefault("resource.climate.summary", "🌡️ Modèle Climatique : Hérité des paramètres planétaires de l'Onglet 1 (Température, Gradient, Pression, O₂, CO₂, Albédo)"));
        if (climateDomainSecHeader != null) climateDomainSecHeader.setText(I18n.getOrDefault("resource.domain.climate", "3. DOMAINE CLIMAT & ATMOSPHÈRE"));
        if (geologyDomainSecHeader != null) geologyDomainSecHeader.setText(I18n.getOrDefault("resource.domain.geology", "4. DOMAINE GÉOLOGIE, TECTONIQUE & MINERAIS"));

        if (floraSecHeader != null) floraSecHeader.setText(I18n.getOrDefault("resource.section.flora", "1. ÉCOLOGIE VÉGÉTALE & SOLS (MÉTRIQUES)"));
        if (faunaSecHeader != null) faunaSecHeader.setText(I18n.getOrDefault("resource.section.fauna", "2. BIOMASSE ANIMALE & AQUATIQUE (MÉTRIQUES)"));
        if (mineralSecHeader != null) mineralSecHeader.setText(I18n.getOrDefault("resource.section.minerals", "3. GÉOLOGIE & HYDROLOGIE (MÉTRIQUES)"));

        if (syncPlanetBtn != null) syncPlanetBtn.setText(I18n.getOrDefault("resource.btn.sync_planet", "🔄 Synchroniser avec la Planète de l'Onglet 1"));
        if (autoDeriveEcologyBtn != null) autoDeriveEcologyBtn.setText(I18n.getOrDefault("resource.btn.auto_derive_eco", "⚡ Auto-déduire Écologie & Biomes depuis la Physique"));
        if (autoDeriveGeologyBtn != null) autoDeriveGeologyBtn.setText(I18n.getOrDefault("resource.btn.auto_derive_geo", "🌋 Calculer Tectonique & Métaux depuis le Manteau"));
        if (mapsSecHeader != null) mapsSecHeader.setText(I18n.getOrDefault("resource.section.imports", "IMPORTS D'ÉCOLOGIE, GÉOLOGIE & HYDROLOGIE DE L'ONGLET 1"));

        if (mapSourceRowLabel != null) mapSourceRowLabel.setText(I18n.getOrDefault("resource.param.map_source", "Modèle de corps céleste / Satellite :"));
        if (biomeMapRowLabel != null) biomeMapRowLabel.setText(I18n.getOrDefault("resource.param.biome_map", "Carte de biomes / Végétation extraterrestre (PNG) :"));
        if (resourceMapRowLabel != null) resourceMapRowLabel.setText(I18n.getOrDefault("resource.param.resource_map", "Carte géologique & minerais multi-canaux (PNG) :"));
        if (hydroMapRowLabel != null) hydroMapRowLabel.setText(I18n.getOrDefault("resource.param.hydro_map", "Carte Hydrographique & Fleuves (Cours d'eau PNG) :"));
        if (climateMapRowLabel != null) climateMapRowLabel.setText(I18n.getOrDefault("planet.map.climate", "Carte Climatique (Thermique / RGB) :"));
        if (rainfallMapRowLabel != null) rainfallMapRowLabel.setText(I18n.getOrDefault("planet.map.rainfall", "Carte de Précipitations (Humidité) :"));
        if (seasonalityMapRowLabel != null) seasonalityMapRowLabel.setText(I18n.getOrDefault("planet.map.seasonality", "Carte de Saisonnalité / Variance :"));

        if (terrestrialBiomassRowLabel != null) terrestrialBiomassRowLabel.setText(I18n.getOrDefault("resource.param.terrestrial_biomass", "Biomasse Végétale Terrestre (Gt/cell) :"));
        if (soilCarbonRowLabel != null) soilCarbonRowLabel.setText(I18n.getOrDefault("resource.param.soil_carbon", "Carbone Organique des Sols (t/ha) :"));
        if (faunaBiomassRowLabel != null) faunaBiomassRowLabel.setText(I18n.getOrDefault("resource.param.fauna_biomass", "Biomasse Faunique Terrestre (kg/km²) :"));
        if (aquaticBiomassRowLabel != null) aquaticBiomassRowLabel.setText(I18n.getOrDefault("resource.param.aquatic_biomass", "Biomasse & Faune Aquatique (t/km²) :"));
        if (crustalMetalRowLabel != null) crustalMetalRowLabel.setText(I18n.getOrDefault("resource.param.crustal_metal", "Richesse en Métaux Croûte (Fe/Cu/Al) :"));
        if (preciousMetalRowLabel != null) preciousMetalRowLabel.setText(I18n.getOrDefault("resource.param.precious_metal", "Concentration en Métaux Précieux (Au/Ag) :"));
        if (mantleHeatRowLabel != null) mantleHeatRowLabel.setText(I18n.getOrDefault("resource.param.mantle_heat", "Flux Thermique du Manteau & Géothermie :"));
        if (freshwaterAquiferRowLabel != null) freshwaterAquiferRowLabel.setText(I18n.getOrDefault("resource.param.freshwater_aquifer", "Capacité des Nappes Phréatiques (m³) :"));

        if (radioProcBiome != null) radioProcBiome.setText(I18n.getOrDefault("resource.mode.procedural_biome", "▶ Biomes Procéduraux"));
        if (radioImportBiome != null) radioImportBiome.setText(I18n.getOrDefault("resource.mode.import_biome", "📂 Carte de Biomes (PNG)"));
        if (radioProcHydro != null) radioProcHydro.setText(I18n.getOrDefault("resource.mode.procedural_hydro", "▶ Hydrographie Procédurale"));
        if (radioImportHydro != null) radioImportHydro.setText(I18n.getOrDefault("resource.mode.import_hydro", "📂 Carte Hydrographique (PNG)"));
        if (radioProcClimate != null) radioProcClimate.setText(I18n.getOrDefault("resource.mode.procedural_climate", "▶ Climat Procédural"));
        if (radioImportClimate != null) radioImportClimate.setText(I18n.getOrDefault("resource.mode.import_climate", "📂 Cartes Climatiques (PNG)"));
        if (radioProcGeology != null) radioProcGeology.setText(I18n.getOrDefault("resource.mode.procedural_geology", "▶ Géologie Procédurale"));
        if (radioImportGeology != null) radioImportGeology.setText(I18n.getOrDefault("resource.mode.import_geology", "📂 Carte Géologique (PNG)"));


        if (mapSourceCombo != null) {
            mapSourceCombo.setButtonCell(mapSourceCombo.getCellFactory().call(null));
        }

        if (loadBiomeBtn != null) loadBiomeBtn.setText(I18n.get("planet.map.btn_load"));
        if (loadResourceBtn != null) loadResourceBtn.setText(I18n.get("planet.map.btn_load"));
        if (loadHydroBtn != null) loadHydroBtn.setText(I18n.get("planet.map.btn_load"));
        if (loadClimateBtn != null) loadClimateBtn.setText(I18n.get("planet.map.btn_load"));
        if (loadRainfallBtn != null) loadRainfallBtn.setText(I18n.get("planet.map.btn_load"));
        if (loadSeasonalityBtn != null) loadSeasonalityBtn.setText(I18n.get("planet.map.btn_load"));

        if (applyBtn != null) applyBtn.setText(I18n.getOrDefault("resource.btn.apply", "✅ Appliquer la distribution scientifique à la simulation"));

        updateSummary();
        updatePlanetContextDisplay();
    }

    private void applyResourceDistribution() {
        if (activeCells == null || activeCells.isEmpty()) return;

        double totalBiomass = terrestrialBiomassSlider.getValue();
        double totalSoilCarbon = soilCarbonSlider.getValue();
        double totalFaunaBiomass = faunaBiomassSlider.getValue();
        double totalAquaticBiomass = aquaticBiomassSlider.getValue();
        double totalMetals = crustalMetalSlider.getValue();
        double totalPrecious = preciousMetalSlider.getValue();
        double heatFlow = mantleHeatSlider.getValue();
        double totalAquifer = freshwaterAquiferSlider.getValue();

        double cellScale = 1.0 / Math.max(1, activeCells.size());

        activeCells.parallelStream().forEach(cell -> {
            Biome b = cell.getBiome();
            if (b == null) return;

            // Distribute global metrics to individual cell attributes (in tonnes & kg)
            cell.setMantleHeatFlow(heatFlow);
            double baseAquifer = totalAquifer * cellScale * (cell.getRainfall() / 1000.0);
            cell.setFreshwaterAquifer(baseAquifer);

            // Compute accessible groundwater table factor
            var point = generator.getPlanetPoint(cell.getLatitude(), cell.getLongitude(), activePlanetPreset != null ? activePlanetPreset : PlanetPreset.EARTH_LIKE);
            cell.setAccessibleAquifer(baseAquifer * (0.2 + 0.8 * point.accessibleAquifer()));

            switch (b) {
                case FOREST, JUNGLE -> {
                    cell.setBiomassNatural(totalBiomass * cellScale * 3.0);
                    cell.setWoodResource(totalBiomass * cellScale * 2500.0);
                }
                case PLAINS -> {
                    cell.setSoilOrganicCarbon(totalSoilCarbon * cellScale * 2.0);
                    cell.setFoodResource(totalSoilCarbon * cellScale * 1000.0);
                    cell.setBiomassLivestock(totalFaunaBiomass * cellScale * 2.0);
                }
                case MOUNTAINS -> {
                    cell.setResourceMetal(totalMetals * cellScale * 4.0);
                    cell.setResourcePreciousMetal(totalPrecious * cellScale * 5.0);
                }
                case OCEAN, DEEP_OCEAN -> {
                    cell.setBiomassFish(totalAquaticBiomass * cellScale * 5.0);
                    cell.setFoodResource(totalAquaticBiomass * cellScale * 500.0);
                }
                default -> {
                    cell.setBiomassNatural(totalBiomass * cellScale * 0.5);
                }
            }
        });

        // Apply custom map images if loaded
        if (customBiomeImage != null || customResourceImage != null) {
            mapLoader.mapImagesToCells(activeCells, null, customBiomeImage, customResourceImage, -11000, 8848);
        }

        logger.info("Applied scientific ecological resource distribution to {} cells", activeCells.size());
        if (onResourcesAppliedCallback != null) {
            onResourcesAppliedCallback.accept(activeCells);
        }
    }
}
