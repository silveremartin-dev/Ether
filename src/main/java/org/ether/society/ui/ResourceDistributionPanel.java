/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.database.H3Cell;
import org.ether.society.data.HistoricalMapGenerator;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.data.OnlineMapService;
import org.ether.society.data.ResourceDepositMapReader;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Biome;
import org.ether.society.model.EcologyPreset;
import org.ether.society.procedural.PlanetPreset;
import org.ether.society.procedural.ProceduralGenerator;
import org.ether.society.procedural.SimplexNoise;

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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // Geological & Energy Tensor Maps (Tab 2 Parity with Tab 1 & Tab 3)
    private final java.util.Map<Integer, Image> customGeologyLayerImages = new java.util.HashMap<>();
    private final java.util.Map<Integer, RadioButton> geologyProcRadios = new java.util.HashMap<>();
    private final java.util.Map<Integer, RadioButton> geologyImportRadios = new java.util.HashMap<>();
    private final java.util.Map<Integer, Label> geologySubTitles = new java.util.HashMap<>();
    private final java.util.Map<Integer, Label> geologyFileLabels = new java.util.HashMap<>();
    private final java.util.Map<Integer, Label> geologySourceLabels = new java.util.HashMap<>();
    private final java.util.Map<Integer, ComboBox<String>> geologySourceCombos = new java.util.HashMap<>();
    private final java.util.Map<Integer, Button> geologyLoadBtns = new java.util.HashMap<>();
    private final java.util.Map<Integer, Label> geologyFormatLabels = new java.util.HashMap<>();
    private final java.util.Map<Integer, Label> geologySeedLabels = new java.util.HashMap<>();
    private final java.util.Map<Integer, Button> geologyGenBtns = new java.util.HashMap<>();
    private final java.util.Map<Integer, List<Label>> geologySliderTitleLabels = new java.util.HashMap<>();
    private VBox geologyLayersDynamicContainer;
    private TextField geologySeedInput;
    private Button btnGenerateProceduralGeologyTensors;

    // Inherited Planet Preset & Context Controls
    private Label planetSectionHeader;
    private ComboBox<PlanetPreset> planetPresetCombo;
    private Label planetContextLabel;
    private Label thermoSynthesisBadge;
    private Button syncPlanetBtn;
    private Button autoDeriveEcologyBtn;
    private Button autoDeriveGeologyBtn;
    private Button autoDeriveHydroBtn;
    private Label biomeStatusLabel;
    private Label hydroStatusLabel;
    private Label geologyStatusLabel;

    private boolean isUpdatingFromPreset = false;
    private final Map<Long, SimplexNoise> noiseCache = new HashMap<>();

    private SimplexNoise getOrCreateNoise(long seed) {
        return noiseCache.computeIfAbsent(seed, SimplexNoise::new);
    }

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
    private Label biomeFormatHintLabel;
    private Label hydroFormatHintLabel;
    private Label geologyFormatHintLabel;
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

    // Per-tensor Geology Seed & Physical Sliders (4.1 to 4.9)
    private final Map<Integer, TextField> geologyTensorSeeds = new java.util.HashMap<>();
    private final Map<Integer, Slider> geologyAbundanceSliders = new java.util.HashMap<>();
    private final Map<Integer, Slider> geologyThresholdSliders = new java.util.HashMap<>();
    private final Map<Integer, Slider> geologyParam3Sliders = new java.util.HashMap<>();
    private final Map<Integer, Slider> geologyParam4Sliders = new java.util.HashMap<>();

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
    private ToggleButton btnReliefOverlay;
    private Canvas mapPreviewCanvas;
    private HBox legendBar;
    private Label summaryLabel;
    private Button applyBtn;

    // Interactive Zoom/Pan State
    private double zoomFactor = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private double dragStartX, dragStartY;

    private java.util.function.Supplier<PlanetPreset> planetPresetSupplier;
    private Consumer<PlanetPreset> planetPresetApplyCallback;

    public void setPlanetPresetSupplier(java.util.function.Supplier<PlanetPreset> supplier) {
        this.planetPresetSupplier = supplier;
    }

    public void setPlanetPresetApplyCallback(Consumer<PlanetPreset> callback) {
        this.planetPresetApplyCallback = callback;
    }

    public ResourceDistributionPanel(Consumer<List<H3Cell>> onResourcesAppliedCallback) {
        this.onResourcesAppliedCallback = onResourcesAppliedCallback;

        getStyleClass().add("glass-panel");
        setPadding(new Insets(20));

        initUI();
        applyEcologyPreset(EcologyPreset.EARTH_STANDARD);
        updateTexts();

        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    public void setActivePlanetPreset(PlanetPreset planetPreset) {
        this.activePlanetPreset = planetPreset;
        boolean oldState = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            if (planetPreset != null && planetPresetCombo != null) {
                PlanetPreset match = planetPresetCombo.getItems().stream()
                        .filter(p -> p.name() != null && p.name().equalsIgnoreCase(planetPreset.name()))
                        .findFirst().orElse(null);
                if (match != null) {
                    planetPresetCombo.setValue(match);
                } else {
                    planetPresetCombo.getItems().add(planetPreset);
                    planetPresetCombo.setValue(planetPreset);
                }
            }
            // Auto-load maps for known celestial bodies (mirrors Tab 1 behaviour)
            autoApplyMapsForPreset(planetPreset);
            updatePlanetContextDisplay();
            updatePreviewCanvas();
        } finally {
            isUpdatingFromPreset = oldState;
        }
    }

    public void setActiveCells(List<H3Cell> cells) {
        this.activeCells = cells;
        updateSummary();
        updatePreviewCanvas();
    }

    private void initUI() {
        VBox controlsBox = new VBox(15);
        controlsBox.setPadding(new Insets(10));

        headerLabel = new Label();
        headerLabel.getStyleClass().add("label-title");

        // --- 1. Inherited Planet Preset & Territory Section ---
        planetSectionHeader = new Label(I18n.getOrDefault("resource.section.planet_preset", "PLANET & SCIENTIFIC DEDUCTION (TAB 1)"));

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
                    setText(I18n.getPlanetPresetDisplayName(item.name()));
                }
            }
        });
        planetPresetCombo.setButtonCell(new ListCell<PlanetPreset>() {
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    PlanetPreset current = planetPresetCombo != null ? planetPresetCombo.getValue() : null;
                    setText(current != null ? I18n.getPlanetPresetDisplayName(current.name()) : I18n.getPlanetPresetDisplayName("Terre (Terran)"));
                } else {
                    setText(I18n.getPlanetPresetDisplayName(item.name()));
                }
            }
        });
        planetPresetCombo.setConverter(new javafx.util.StringConverter<PlanetPreset>() {
            @Override
            public String toString(PlanetPreset item) {
                return item == null ? I18n.getPlanetPresetDisplayName("Terre (Terran)") : I18n.getPlanetPresetDisplayName(item.name());
            }
            @Override
            public PlanetPreset fromString(String string) {
                return null;
            }
        });
        planetPresetCombo.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-opacity: 1.0; -fx-border-color: rgba(56, 189, 248, 0.4); -fx-border-radius: 4px;");
        planetPresetCombo.setOnAction(e -> {
            PlanetPreset selected = planetPresetCombo.getValue();
            if (selected != null) {
                this.activePlanetPreset = selected;
                adaptResourceSlidersToPlanet(selected);
                // Auto-load maps for known celestial bodies (mirrors Tab 1 behaviour)
                autoApplyMapsForPreset(selected);
                updatePlanetContextDisplay();
                updatePreviewCanvas();
            }
        });

        planetContextLabel = new Label(I18n.getOrDefault("resource.territory.earth", "🪐 Territory: Earth-Like Planet"));
        planetContextLabel.setWrapText(true);
        planetContextLabel.getStyleClass().add("planet-context-badge");

        syncPlanetBtn = new Button(I18n.getOrDefault("resource.btn.sync_planet", "🔄 Synchronize with Tab 1 Planet"));
        syncPlanetBtn.getStyleClass().add("button-secondary");
        syncPlanetBtn.setMaxWidth(Double.MAX_VALUE);
        syncPlanetBtn.setOnAction(e -> {
            PlanetPreset current = planetPresetSupplier != null ? planetPresetSupplier.get() : activePlanetPreset;
            if (current != null) {
                setActivePlanetPreset(current);
                adaptResourceSlidersToPlanet(current);
                updatePlanetContextDisplay();
                updatePreviewCanvas();
            }
        });

        Button autoDeriveMasterBtn = new Button(I18n.getOrDefault("resource.btn.auto_derive_all", "⚡ Auto-derive All from Physics (Biomes, Aquifers & Geology)"));
        autoDeriveMasterBtn.getStyleClass().addAll("button-secondary", "button-accent-blue");
        autoDeriveMasterBtn.setMaxWidth(Double.MAX_VALUE);
        autoDeriveMasterBtn.setOnAction(e -> {
            autoDeriveEcologyFromPlanet();
            autoDeriveHydroFromPlanet();
            autoDeriveGeologyFromPlanet();
        });

        thermoSynthesisBadge = new Label();
        thermoSynthesisBadge.setWrapText(true);
        thermoSynthesisBadge.getStyleClass().add("info-badge");

        VBox planetSection = createSection(planetSectionHeader, new VBox(8,
                new Label(I18n.getOrDefault("resource.label.planet_preset_select", "Inherited Celestial Body Preset:")),
                planetPresetCombo,
                planetContextLabel,
                thermoSynthesisBadge,
                syncPlanetBtn,
                autoDeriveMasterBtn
        ));

        // --- 2. Standardized Preset Control Bar for Ecology ---
        ecologyPresetBar = new PresetControlBar<>("resource.preset_title", "Préréglage Écologique & Ressources");
        ecologyPresetBar.setExportCategory("ecology");
        ecologyPresetBar.setPresets(EcologyPreset.getBuiltInPresets(), EcologyPreset.EARTH_STANDARD);

        ecologyPresetBar.setListener(new PresetControlBar.PresetActionsListener<EcologyPreset>() {
            @Override
            public void onPresetSelected(EcologyPreset preset) {
                applyEcologyPreset(preset);
                if (preset != null) {
                    if (preset.embeddedPlanetPreset() != null) {
                        setActivePlanetPreset(preset.embeddedPlanetPreset());
                        if (planetPresetApplyCallback != null) {
                            planetPresetApplyCallback.accept(preset.embeddedPlanetPreset());
                        }
                    } else if (preset.planetPresetName() != null) {
                        PlanetPreset p = findPlanetPresetByName(preset.planetPresetName());
                        if (p != null) {
                            setActivePlanetPreset(p);
                            if (planetPresetApplyCallback != null) {
                                planetPresetApplyCallback.accept(p);
                            }
                        }
                    }
                }
            }

            @Override
            public void onSavePreset(String name) {
                if (!validateResourceSetup()) {
                    return;
                }
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

                PlanetPreset currentPlanet = planetPresetSupplier != null ? planetPresetSupplier.get() : activePlanetPreset;
                if (currentPlanet == null) currentPlanet = PlanetPreset.EARTH_LIKE;
                String planetName = currentPlanet.name() != null ? currentPlanet.name() : PlanetPreset.EARTH_LIKE.name();

                EcologyPreset custom = new EcologyPreset(
                        name,
                        planetName,
                        currentPlanet,
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
        mapsSecHeader = new Label(I18n.getOrDefault("resource.section.custom_maps", "SATELLITE MAPS & GEOLOGICAL SITES (PNG / WMS)"));

        mapSourceRowLabel = new Label(I18n.getOrDefault("resource.param.map_source", "Celestial Body Model / Satellite:"));
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
                    setText("none".equals(item) ? I18n.getOrDefault("planet.combo.prompt_source", "— Select Data Source —") : I18n.getOrDefault("planet.map." + item, item));
                }
            }
        });
        mapSourceCombo.setButtonCell(mapSourceCombo.getCellFactory().call(null));
        mapSourceCombo.setMaxWidth(Double.MAX_VALUE);
        mapSourceCombo.setOnAction(e -> applyPresetMapSource(mapSourceCombo.getValue()));

        biomeMapRowLabel = new Label(I18n.getOrDefault("resource.param.biome_map", "Biome / Extraterrestrial Vegetation Map (PNG):"));
        biomeFileLabel = new Label(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
        biomeFileLabel.getStyleClass().add("value-label");
        loadBiomeBtn = new Button(I18n.get("planet.map.btn_load"));
        loadBiomeBtn.getStyleClass().add("button-secondary");
        loadBiomeBtn.setOnAction(e -> loadCustomBiomeMap());
        clearBiomeBtn = new Button("❌");
        clearBiomeBtn.getStyleClass().add("button-secondary");
        clearBiomeBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18n.getOrDefault("dialog.confirm.title", "Delete Confirmation"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.getOrDefault("resource.confirm.clear_biome", "Do you really want to clear the imported biome map?"));
            WindowUtils.applyWindowIcon(alert);
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    customBiomeImage = null;
                    biomeFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
                    updatePreviewCanvas();
                    updateSummary();
                }
            });
        });
        HBox biomeBox = new HBox(5, loadBiomeBtn, clearBiomeBtn);

        biomeFormatHintLabel = new Label(I18n.getOrDefault("resource.format.biome_hint",
                "PNG / JPEG (projection équirectangulaire 2:1) :\nImage de biomes ou couvert végétal (niveaux de gris ou RGB)."));
        biomeFormatHintLabel.setWrapText(true);
        biomeFormatHintLabel.getStyleClass().add("hint-label");

        resourceMapRowLabel = new Label(I18n.getOrDefault("resource.param.resource_map", "Geological & Multi-Channel Ore Map (PNG):"));
        resourceFileLabel = new Label(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
        resourceFileLabel.getStyleClass().add("value-label");
        loadResourceBtn = new Button(I18n.get("planet.map.btn_load"));
        loadResourceBtn.getStyleClass().add("button-secondary");
        loadResourceBtn.setOnAction(e -> loadCustomResourceMap());
        clearResourceBtn = new Button("❌");
        clearResourceBtn.getStyleClass().add("button-secondary");
        clearResourceBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18n.getOrDefault("dialog.confirm.title", "Delete Confirmation"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.getOrDefault("resource.confirm.clear_resource", "Do you really want to clear the imported geological map?"));
            WindowUtils.applyWindowIcon(alert);
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    customResourceImage = null;
                    resourceFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
                    updatePreviewCanvas();
                    updateSummary();
                }
            });
        });
        HBox resourceBox = new HBox(5, loadResourceBtn, clearResourceBtn);

        geologyFormatHintLabel = new Label(I18n.getOrDefault("resource.format.geology_hint",
                "PNG multi-canaux (projection équirectangulaire 2:1) :\n • Canal R (Rouge) = Métaux crustaux industriels (Fer/Cuivre)\n • Canal G (Vert) = Minerais précieux & terres rares (Or/Pt)\n • Canal B (Bleu) = Flux thermique du manteau & aquifères"));
        geologyFormatHintLabel.setWrapText(true);
        geologyFormatHintLabel.getStyleClass().add("hint-label");

        hydroMapRowLabel = new Label(I18n.getOrDefault("resource.param.hydro_map", "Hydrographic & Rivers Map (PNG Watercourses):"));
        hydroFileLabel = new Label(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
        hydroFileLabel.getStyleClass().add("value-label");
        loadHydroBtn = new Button(I18n.get("planet.map.btn_load"));
        loadHydroBtn.getStyleClass().add("button-secondary");
        loadHydroBtn.setOnAction(e -> loadCustomHydroMap());
        clearHydroBtn = new Button("❌");
        clearHydroBtn.getStyleClass().add("button-secondary");
        clearHydroBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18n.getOrDefault("dialog.confirm.title", "Delete Confirmation"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.getOrDefault("resource.confirm.clear_hydro", "Do you really want to clear the imported hydrographic map?"));
            WindowUtils.applyWindowIcon(alert);
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    customHydroImage = null;
                    hydroFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
                    updatePreviewCanvas();
                    updateSummary();
                }
            });
        });
        HBox hydroBox = new HBox(5, loadHydroBtn, clearHydroBtn);

        hydroFormatHintLabel = new Label(I18n.getOrDefault("resource.format.hydro_hint",
                "PNG / JPEG (projection équirectangulaire 2:1) :\nNoir (0) = sans eau | Blanc (255) = réseau fluvial / débit max."));
        hydroFormatHintLabel.setWrapText(true);
        hydroFormatHintLabel.getStyleClass().add("hint-label");

        fetchOnlineHydroBtn = new Button(I18n.getOrDefault("resource.btn.fetch_online_hydro", "🌐 Download Satellite Hydrography (Earth WMS / Earth Preset)"));
        fetchOnlineHydroBtn.setMaxWidth(Double.MAX_VALUE);
        fetchOnlineHydroBtn.getStyleClass().add("button-secondary");
        fetchOnlineHydroBtn.setOnAction(e -> fetchOnlineHydroData());

        proceduralHydroBtn = new Button(I18n.getOrDefault("resource.btn.procedural_hydro", "⚡ Auto-generate Rivers & Watercourses (Procedural Slope)"));
        proceduralHydroBtn.setMaxWidth(Double.MAX_VALUE);
        proceduralHydroBtn.getStyleClass().add("button-secondary");
        proceduralHydroBtn.setOnAction(e -> generateProceduralHydrography());

        fetchOnlineBtn = new Button(I18n.getOrDefault("resource.btn.fetch_online", "🌐 Download WMS Satellite Maps (USGS / NASA)"));
        fetchOnlineBtn.setMaxWidth(Double.MAX_VALUE);
        fetchOnlineBtn.getStyleClass().add("button-secondary");
        fetchOnlineBtn.setOnAction(e -> fetchOnlineSatelliteData());

        exportMapsBtn = new Button(I18n.getOrDefault("resource.btn.export_maps", "📤 Export Maps (PNG + WorldFile .tfw)"));
        exportMapsBtn.setMaxWidth(Double.MAX_VALUE);
        exportMapsBtn.getStyleClass().add("button-secondary");
        exportMapsBtn.setOnAction(e -> exportMapsWithWorldFiles());

        specsHelpBtn = new Button(I18n.getOrDefault("resource.btn.specs_help", "ℹ️ Biome & Geology Map Specifications"));
        specsHelpBtn.setMaxWidth(Double.MAX_VALUE);
        specsHelpBtn.getStyleClass().add("button-secondary");
        specsHelpBtn.setOnAction(e -> showEcologyImportFormatHelp());

        mapStatusLabel = new Label();
        mapStatusLabel.getStyleClass().add("value-label");
        mapStatusLabel.setWrapText(true);
        // Note: customMapsSection legacy wrapper removed — controls are assembled
        // inside their respective domain import boxes (biomeDomainSection, hydroDomainSection, geologyDomainSection).

        // --- Climate map controls (used inside climateDomainSection → climateImportBox) ---
        climateMapRowLabel = new Label();
        climateFileLabel = new Label(I18n.get("planet.map.none"));
        climateFileLabel.getStyleClass().add("value-label");
        loadClimateBtn = new Button(I18n.get("planet.map.btn_load"));
        loadClimateBtn.getStyleClass().add("button-secondary");
        loadClimateBtn.setOnAction(e -> chooseClimateMapFile());
        clearClimateBtn = new Button("❌");
        clearClimateBtn.getStyleClass().add("button-secondary");
        clearClimateBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18n.getOrDefault("dialog.confirm.title", "Delete Confirmation"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.getOrDefault("resource.confirm.clear_climate", "Do you really want to clear the imported climate map?"));
            WindowUtils.applyWindowIcon(alert);
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    customClimateImage = null;
                    climateFileLabel.setText(I18n.get("planet.map.none"));
                    updatePreviewCanvas();
                }
            });
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
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18n.getOrDefault("dialog.confirm.title", "Delete Confirmation"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.getOrDefault("resource.confirm.clear_rainfall", "Do you really want to clear the imported precipitation map?"));
            WindowUtils.applyWindowIcon(alert);
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    customRainfallImage = null;
                    rainfallFileLabel.setText(I18n.get("planet.map.none"));
                    updatePreviewCanvas();
                }
            });
        });
        HBox rainfallBox = new HBox(5, loadRainfallBtn, clearRainfallBtn);

        seasonalityMapRowLabel = new Label();
        seasonalityFileLabel = new Label(I18n.get("planet.map.none"));
        seasonalityFileLabel.getStyleClass().add("value-label");
        loadSeasonalityBtn = new Button(I18n.get("planet.map.btn_load"));
        loadSeasonalityBtn.getStyleClass().add("button-secondary");
        loadSeasonalityBtn.setOnAction(e -> chooseSeasonalityMapFile());
        clearSeasonalityBtn = new Button("❌");
        clearSeasonalityBtn.getStyleClass().add("button-secondary");
        clearSeasonalityBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18n.getOrDefault("dialog.confirm.title", "Delete Confirmation"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.getOrDefault("resource.confirm.clear_seasonality", "Do you really want to clear the imported seasonality map?"));
            WindowUtils.applyWindowIcon(alert);
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    customSeasonalityImage = null;
                    seasonalityFileLabel.setText(I18n.get("planet.map.none"));
                    updatePreviewCanvas();
                }
            });
        });
        HBox seasonalityBox = new HBox(5, loadSeasonalityBtn, clearSeasonalityBtn);

        fetchOnlineClimateBtn = new Button(I18n.getOrDefault("planet.map.btn_fetch_online_climate", "🌐 Download Satellite Climate NASA/USGS (WMS)"));
        fetchOnlineClimateBtn.setMaxWidth(Double.MAX_VALUE);
        fetchOnlineClimateBtn.getStyleClass().add("button-secondary");
        fetchOnlineClimateBtn.setOnAction(e -> fetchOnlineClimateData());

        climateHelpBtn = new Button(I18n.getOrDefault("planet.map.btn_climate_help", "ℹ️ Climate Map Specifications"));
        climateHelpBtn.setMaxWidth(Double.MAX_VALUE);
        climateHelpBtn.getStyleClass().add("button-secondary");
        climateHelpBtn.setOnAction(e -> showClimateImportFormatHelp());

        climateStatusLabel = new Label();
        climateStatusLabel.getStyleClass().add("value-label");
        climateStatusLabel.setWrapText(true);


        // --- 5. Flora & Terrestrial Plant Resources ---
        terrestrialBiomassSlider = createSlider(1.0, 5000.0, 450.0);
        soilCarbonSlider = createSlider(10.0, 10000.0, 1500.0);
        terrestrialBiomassRowLabel = new Label(I18n.getOrDefault("resource.label.plant_biomass", "Global Plant Biomass & Forests:"));
        soilCarbonRowLabel = new Label(I18n.getOrDefault("resource.param.soil_carbon", "Soil Organic Carbon (GtC):"));
        floraSecHeader = new Label(I18n.getOrDefault("resource.section.vegetation", "VEGETATION & SOILS (METRICS)"));

        VBox floraSection = new VBox(8,
                createControlRow(terrestrialBiomassRowLabel, terrestrialBiomassSlider, "%.0f GtC", I18n.getOrDefault("resource.desc.terrestrial_biomass", "Global stock of plant biomass and forests (Gigatons of Carbon)")),
                createControlRow(soilCarbonRowLabel, soilCarbonSlider, "%.0f GtC", I18n.getOrDefault("resource.desc.soil_carbon", "Soil organic carbon stock and agricultural potential (GtC)"))
        );

        // --- 6. Fauna & Animal Resources ---
        faunaBiomassSlider = createSlider(0.01, 50.0, 2.0);
        aquaticBiomassSlider = createSlider(0.1, 100.0, 6.0);
        faunaBiomassRowLabel = new Label(I18n.getOrDefault("resource.label.wildlife_biomass", "Terrestrial & Wild Animal Biomass:"));
        aquaticBiomassRowLabel = new Label(I18n.getOrDefault("resource.label.marine_biomass", "Marine & Estuarine Biomass:"));
        faunaSecHeader = new Label(I18n.getOrDefault("resource.section.animal_biomass", "ANIMAL BIOMASS (METRICS)"));

        VBox faunaSection = new VBox(8,
                createControlRow(faunaBiomassRowLabel, faunaBiomassSlider, "%.2f GtC", I18n.getOrDefault("resource.desc.fauna_biomass", "Total biomass of wild terrestrial vertebrates (GtC)")),
                createControlRow(aquaticBiomassRowLabel, aquaticBiomassSlider, "%.2f GtC", I18n.getOrDefault("resource.desc.aquatic_biomass", "Marine biomass stock, fish and phytoplankton (GtC)"))
        );

        // --- 7. Minerals & Underground Geology ---
        crustalMetalSlider = createSlider(1.0, 2000.0, 80.0);
        preciousMetalSlider = createSlider(10.0, 50000.0, 1200.0);
        mantleHeatSlider = createSlider(10.0, 400.0, 87.0);
        freshwaterAquiferSlider = createSlider(10.0, 100000.0, 15000.0);
        seismicActivitySlider = createSlider(0.0, 10.0, 2.5);
        volcanicActivitySlider = createSlider(0.0, 8.0, 1.5);
        seismicRowLabel = new Label(I18n.getOrDefault("resource.label.seismic", "Seismic & Tectonic Activity (Mag Richter):"));
        volcanicRowLabel = new Label(I18n.getOrDefault("resource.label.volcanic", "Global Volcanic Activity (VEI):"));

        crustalMetalRowLabel = new Label(I18n.getOrDefault("resource.label.industrial_metals", "Industrial Metal Reserves (Iron/Copper):"));
        preciousMetalRowLabel = new Label(I18n.getOrDefault("resource.label.precious_metals", "Precious Ores & REE (Gold/Pt):"));
        mantleHeatRowLabel = new Label(I18n.getOrDefault("resource.param.mantle_heat", "Mantle Heat Flux (mW/m²):"));
        freshwaterAquiferRowLabel = new Label(I18n.getOrDefault("resource.label.freshwater", "Freshwater Reserves & Aquifers:"));

        // ---- DOMAIN SEEDS (one per domain, replacing global seed section) ----
        // biomeSeedField is also assigned to seedField for EcologyPreset compatibility.

        // --- DOMAINE 1 : BIOMES & FLORE ---
        ToggleGroup biomeGroup = new ToggleGroup();
        radioProcBiome = new RadioButton(I18n.getOrDefault("resource.mode.procedural_biome", "▶ Procedural Biomes"));
        radioImportBiome = new RadioButton(I18n.getOrDefault("resource.mode.import_biome", "📂 Biome Map (PNG)"));
        radioProcBiome.setToggleGroup(biomeGroup);
        radioImportBiome.setToggleGroup(biomeGroup);
        radioProcBiome.setSelected(true);
        radioProcBiome.getStyleClass().add("radio-proc");
        radioImportBiome.getStyleClass().add("radio-import");

        biomeSeedField = new TextField("12345");
        randSeedBtn = new Button("🎲");
        randSeedBtn.getStyleClass().add("button-secondary");
        randSeedBtn.setOnAction(e -> { biomeSeedField.setText(String.valueOf(new Random().nextLong(1000000))); updatePreviewCanvas(); });
        HBox biomeSeedBox = new HBox(5, biomeSeedField, randSeedBtn);
        HBox.setHgrow(biomeSeedField, Priority.ALWAYS);
        seedField = biomeSeedField; // EcologyPreset compat

        Button exportBiomeBtn = new Button(I18n.getOrDefault("resource.btn.export_biome", "📤 Export Procedural Biome Map (PNG)"));
        exportBiomeBtn.setMaxWidth(Double.MAX_VALUE);
        exportBiomeBtn.getStyleClass().add("button-secondary");
        exportBiomeBtn.setOnAction(e -> exportMapsWithWorldFiles());

        autoDeriveEcologyBtn = new Button(I18n.getOrDefault("resource.btn.auto_derive_eco", "⚡ Auto-derive Ecology & Biomes from Physics"));
        autoDeriveEcologyBtn.getStyleClass().add("button-secondary");
        autoDeriveEcologyBtn.setMaxWidth(Double.MAX_VALUE);
        autoDeriveEcologyBtn.setOnAction(e -> autoDeriveEcologyFromPlanet());

        biomeStatusLabel = new Label();
        biomeStatusLabel.getStyleClass().add("subcard-status-label");
        biomeStatusLabel.setWrapText(true);

        VBox biomeProcBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.seed.label", "Generation Seed:")), biomeSeedBox,
                        I18n.getOrDefault("resource.tooltip.seed", "Random seed for procedural biome distribution")),
                autoDeriveEcologyBtn,
                biomeStatusLabel,
                floraSection, faunaSection, exportBiomeBtn
        );
        biomeProcBox.getStyleClass().add("subcard-procedural-box");

        biomeSourceCombo = new ComboBox<>();
        biomeSourceCombo.getItems().addAll("none", "earth", "mars", "venus", "moon");
        biomeSourceCombo.setValue("none");
        biomeSourceCombo.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || "none".equals(item) ? I18n.getOrDefault("planet.combo.prompt_source", "— Select Data Source —") : I18n.getOrDefault("planet.map." + item, item));
            }
        });
        biomeSourceCombo.setButtonCell(biomeSourceCombo.getCellFactory().call(null));
        biomeSourceCombo.setMaxWidth(Double.MAX_VALUE);
        biomeSourceCombo.setOnAction(e -> {
            if (isUpdatingFromPreset) return;
            String val = biomeSourceCombo.getValue();
            if (val != null && !"none".equals(val)) {
                radioImportBiome.setSelected(true);
                applyPresetMapSource(val);
            }
        });

        VBox biomeImportBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.param.map_source", "Celestial Body / Source:")), biomeSourceCombo,
                        "Sélectionner une source satellite prédéfinie"),
                createControlRow(biomeMapRowLabel, new VBox(3, biomeBox, biomeFileLabel, biomeFormatHintLabel), I18n.getOrDefault("resource.desc.import_biome_map", "Import a biome map in PNG/JPEG format"))
        );
        biomeImportBox.getStyleClass().add("subcard-import-box");
        biomeImportBox.setVisible(false);
        biomeImportBox.setManaged(false);

        biomeGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isProc = sel == radioProcBiome;
            biomeProcBox.setVisible(isProc); biomeProcBox.setManaged(isProc);
            biomeImportBox.setVisible(!isProc); biomeImportBox.setManaged(!isProc);
            if (!isProc && customBiomeImage == null && biomeSourceCombo.getValue() != null && !"none".equals(biomeSourceCombo.getValue())) {
                applyPresetMapSource(biomeSourceCombo.getValue());
            }
            if (!isUpdatingFromPreset) {
                if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
                updatePreviewCanvas();
            }
        });

        biomeDomainSecHeader = new Label(I18n.getOrDefault("resource.domain.biome", "1. BIOME & FLORA DOMAIN (VEGETATION)"));
        VBox biomeDomainSection = createSection(biomeDomainSecHeader, new VBox(8,
                radioProcBiome, biomeProcBox, radioImportBiome, biomeImportBox
        ));

        // --- DOMAINE 2 : HYDROGRAPHIE & EAU DOUCE ---
        ToggleGroup hydroGroup = new ToggleGroup();
        radioProcHydro = new RadioButton(I18n.getOrDefault("resource.mode.procedural_hydro", "▶ Procedural Hydrography"));
        radioImportHydro = new RadioButton(I18n.getOrDefault("resource.mode.import_hydro", "📂 Hydrographic Map (PNG)"));
        radioProcHydro.setToggleGroup(hydroGroup);
        radioImportHydro.setToggleGroup(hydroGroup);
        radioProcHydro.setSelected(true);
        radioProcHydro.getStyleClass().add("radio-proc");
        radioImportHydro.getStyleClass().add("radio-import");

        hydroSeedField = new TextField("23456");
        Button hydroRandBtn = new Button("🎲");
        hydroRandBtn.getStyleClass().add("button-secondary");
        hydroRandBtn.setOnAction(e -> { hydroSeedField.setText(String.valueOf(new Random().nextLong(1000000))); updatePreviewCanvas(); });
        HBox hydroSeedBox = new HBox(5, hydroSeedField, hydroRandBtn);
        HBox.setHgrow(hydroSeedField, Priority.ALWAYS);

        autoDeriveHydroBtn = new Button(I18n.getOrDefault("resource.btn.auto_derive_hydro", "💧 Auto-derive Aquifers & Freshwater from Physics"));
        autoDeriveHydroBtn.getStyleClass().add("button-secondary");
        autoDeriveHydroBtn.setMaxWidth(Double.MAX_VALUE);
        autoDeriveHydroBtn.setOnAction(e -> autoDeriveHydroFromPlanet());

        hydroStatusLabel = new Label();
        hydroStatusLabel.getStyleClass().add("subcard-status-label");
        hydroStatusLabel.setWrapText(true);

        VBox hydroProcBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.seed.label", "Generation Seed:")), hydroSeedBox,
                        "Graine aléatoire pour la génération procédurale des cours d'eau"),
                autoDeriveHydroBtn,
                hydroStatusLabel,
                createControlRow(freshwaterAquiferRowLabel, freshwaterAquiferSlider, "%.0f x10³ km³", I18n.getOrDefault("resource.desc.freshwater_aquifer", "Total volume of groundwater and continental aquifers")),
                proceduralHydroBtn
        );
        hydroProcBox.getStyleClass().add("subcard-procedural-box");

        hydroSourceCombo = new ComboBox<>();
        hydroSourceCombo.getItems().addAll("none", "earth", "mars");
        hydroSourceCombo.setValue("none");
        hydroSourceCombo.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || "none".equals(item) ? I18n.getOrDefault("planet.combo.prompt_source", "— Select Data Source —") : I18n.getOrDefault("planet.map." + item, item));
            }
        });
        hydroSourceCombo.setButtonCell(hydroSourceCombo.getCellFactory().call(null));
        hydroSourceCombo.setMaxWidth(Double.MAX_VALUE);
        hydroSourceCombo.setOnAction(e -> {
            if (isUpdatingFromPreset) return;
            String val = hydroSourceCombo.getValue();
            if (val != null && !"none".equals(val)) {
                radioImportHydro.setSelected(true);
                fetchOnlineHydroData();
            }
        });

        VBox hydroImportBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.param.map_source", "Celestial Body / Source:")), hydroSourceCombo,
                        "Source satellite pour la carte hydrographique"),
                createControlRow(hydroMapRowLabel, new VBox(3, hydroBox, hydroFileLabel, hydroFormatHintLabel), I18n.getOrDefault("resource.desc.import_hydro_map", "Import a hydrographic map and river network"))
        );
        hydroImportBox.getStyleClass().add("subcard-import-box");
        hydroImportBox.setVisible(false);
        hydroImportBox.setManaged(false);

        hydroGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isProc = sel == radioProcHydro;
            hydroProcBox.setVisible(isProc); hydroProcBox.setManaged(isProc);
            hydroImportBox.setVisible(!isProc); hydroImportBox.setManaged(!isProc);
            if (!isProc && customHydroImage == null && hydroSourceCombo.getValue() != null && !"none".equals(hydroSourceCombo.getValue())) {
                fetchOnlineHydroData();
            }
            if (!isUpdatingFromPreset) {
                if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
                updatePreviewCanvas();
            }
        });

        hydroDomainSecHeader = new Label(I18n.getOrDefault("resource.domain.hydro", "2. DOMAINE HYDROGRAPHIE & EAU DOUCE"));
        VBox hydroDomainSection = createSection(hydroDomainSecHeader, new VBox(8,
                radioProcHydro, hydroProcBox, radioImportHydro, hydroImportBox
        ));

        // --- DOMAINE 3 : CLIMAT & ATMOSPHÈRE ---
        ToggleGroup climateGroup = new ToggleGroup();
        radioProcClimate = new RadioButton(I18n.getOrDefault("resource.mode.procedural_climate", "▶ Procedural Climate"));
        radioImportClimate = new RadioButton(I18n.getOrDefault("resource.mode.import_climate", "📂 Climate Maps (PNG)"));
        radioProcClimate.setToggleGroup(climateGroup);
        radioImportClimate.setToggleGroup(climateGroup);
        radioProcClimate.setSelected(true);
        radioProcClimate.getStyleClass().add("radio-proc");
        radioImportClimate.getStyleClass().add("radio-import");

        climateSeedField = new TextField("34567");
        Button climateRandBtn = new Button("🎲");
        climateRandBtn.getStyleClass().add("button-secondary");
        climateRandBtn.setOnAction(e -> { climateSeedField.setText(String.valueOf(new Random().nextLong(1000000))); updatePreviewCanvas(); });
        HBox climateSeedBox = new HBox(5, climateSeedField, climateRandBtn);
        HBox.setHgrow(climateSeedField, Priority.ALWAYS);

        climateSummaryLabel = new Label(I18n.getOrDefault("resource.climate.summary", "🌡️ Climate Model: Inherited from Tab 1 planet parameters (Temperature, Gradient, Pressure, O₂, CO₂, Albedo)"));
        climateSummaryLabel.getStyleClass().add("subcard-status-muted");
        climateSummaryLabel.setWrapText(true);

        VBox climateProcBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.seed.label", "Generation Seed:")), climateSeedBox,
                        "Graine aléatoire pour la variation procédurale du modèle climatique"),
                climateSummaryLabel
        );
        climateProcBox.getStyleClass().add("subcard-procedural-box");

        climateSourceCombo = new ComboBox<>();
        climateSourceCombo.getItems().addAll(
                "none",
                "NASA MODIS LST (WMS — températures)",
                "NASA GPM IMERG (WMS — précipitations)",
                "ERA5 Reanalysis (Copernicus)",
                "Koppen-Geiger Classification (PNG)"
        );
        climateSourceCombo.setValue("none");
        climateSourceCombo.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || "none".equals(item) ? I18n.getOrDefault("planet.combo.prompt_source", "— Select Data Source —") : item);
            }
        });
        climateSourceCombo.setButtonCell(climateSourceCombo.getCellFactory().call(null));
        climateSourceCombo.setMaxWidth(Double.MAX_VALUE);
        climateSourceCombo.setOnAction(e -> {
            if (isUpdatingFromPreset) return;
            String val = climateSourceCombo.getValue();
            if (val != null && !"none".equals(val)) {
                radioImportClimate.setSelected(true);
                fetchOnlineClimateData();
            }
        });

        VBox climateImportBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("planet.climate.source_label", "Reference Source:")), climateSourceCombo,
                        "Reference climate data source"),
                createControlRow(climateMapRowLabel, new VBox(3, climateBox, climateFileLabel), I18n.getOrDefault("planet.tooltip.climate_map", "Thermal or combined RGB map import")),
                createControlRow(rainfallMapRowLabel, new VBox(3, rainfallBox, rainfallFileLabel), I18n.getOrDefault("planet.tooltip.rainfall_map", "Precipitation / humidity map import")),
                createControlRow(seasonalityMapRowLabel, new VBox(3, seasonalityBox, seasonalityFileLabel), I18n.getOrDefault("planet.tooltip.seasonality_map", "Import a seasonal variance map")),
                fetchOnlineClimateBtn,
                climateHelpBtn
        );
        climateImportBox.getStyleClass().add("subcard-import-box");
        climateImportBox.setVisible(false);
        climateImportBox.setManaged(false);

        climateGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isProc = sel == radioProcClimate;
            climateProcBox.setVisible(isProc); climateProcBox.setManaged(isProc);
            climateImportBox.setVisible(!isProc); climateImportBox.setManaged(!isProc);
            if (!isProc && customClimateImage == null && climateSourceCombo.getValue() != null && !"none".equals(climateSourceCombo.getValue())) {
                fetchOnlineClimateData();
            }
            if (!isUpdatingFromPreset) {
                if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
                updatePreviewCanvas();
            }
        });

        climateDomainSecHeader = new Label(I18n.getOrDefault("resource.domain.climate", "3. CLIMATE & ATMOSPHERE DOMAIN"));
        VBox climateDomainSection = createSection(climateDomainSecHeader, new VBox(8,
                radioProcClimate, climateProcBox, radioImportClimate, climateImportBox
        ));

        // --- DOMAINE 4 : GÉOLOGIE, TECTONIQUE & MINERAIS ---
        ToggleGroup geologyGroup = new ToggleGroup();
        radioProcGeology = new RadioButton(I18n.getOrDefault("resource.mode.procedural_geology", "▶ Procedural Geology"));
        radioImportGeology = new RadioButton(I18n.getOrDefault("resource.mode.import_geology", "📂 Geological Map (PNG)"));
        radioProcGeology.setToggleGroup(geologyGroup);
        radioImportGeology.setToggleGroup(geologyGroup);
        radioProcGeology.setSelected(true);
        radioProcGeology.getStyleClass().add("radio-proc");
        radioImportGeology.getStyleClass().add("radio-import");

        geologySeedField = new TextField("45678");
        Button geologyRandBtn = new Button("🎲");
        geologyRandBtn.getStyleClass().add("button-secondary");
        geologyRandBtn.setOnAction(e -> { geologySeedField.setText(String.valueOf(new Random().nextLong(1000000))); updatePreviewCanvas(); });
        HBox geologySeedBox = new HBox(5, geologySeedField, geologyRandBtn);
        HBox.setHgrow(geologySeedField, Priority.ALWAYS);

        Button exportGeologyBtn = new Button(I18n.getOrDefault("resource.btn.export_geology", "📤 Export Geological Map (PNG + WorldFile)"));
        exportGeologyBtn.setMaxWidth(Double.MAX_VALUE);
        exportGeologyBtn.getStyleClass().add("button-secondary");
        exportGeologyBtn.setOnAction(e -> exportMapsWithWorldFiles());

        autoDeriveGeologyBtn = new Button(I18n.getOrDefault("resource.btn.auto_derive_geo", "🌋 Calculate Tectonics & Metals from Mantle"));
        autoDeriveGeologyBtn.getStyleClass().add("button-secondary");
        autoDeriveGeologyBtn.setMaxWidth(Double.MAX_VALUE);
        autoDeriveGeologyBtn.setOnAction(e -> autoDeriveGeologyFromPlanet());

        geologyStatusLabel = new Label();
        geologyStatusLabel.getStyleClass().add("subcard-status-label");
        geologyStatusLabel.setWrapText(true);

        VBox geologyProcBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.seed.label", "Generation Seed:")), geologySeedBox,
                        "Graine aléatoire pour la distribution des gisements et structures tectoniques"),
                autoDeriveGeologyBtn,
                geologyStatusLabel,
                createControlRow(seismicRowLabel, seismicActivitySlider, "%.1f Mag", I18n.getOrDefault("resource.desc.seismic_activity", "Planetary seismicity level generating earthquakes")),
                createControlRow(volcanicRowLabel, volcanicActivitySlider, "%.1f VEI", I18n.getOrDefault("resource.desc.volcanic_activity", "Volcanic activity level generating eruptions")),
                createControlRow(crustalMetalRowLabel, crustalMetalSlider, "%.0f Gt", I18n.getOrDefault("resource.desc.crustal_metal", "Industrial metal reserves in crust (Gigatons)")),
                createControlRow(preciousMetalRowLabel, preciousMetalSlider, "%.0f Mt", I18n.getOrDefault("resource.desc.precious_metal", "Precious elements and rare earth stock (Megatons)")),
                createControlRow(mantleHeatRowLabel, mantleHeatSlider, "%.1f mW/m²", I18n.getOrDefault("resource.desc.mantle_heat", "Mantle heat flux, volcanism, and geothermal activity")),
                exportGeologyBtn
        );
        geologyProcBox.getStyleClass().add("subcard-procedural-box");

        geologySourceCombo = new ComboBox<>();
        geologySourceCombo.getItems().addAll("none", "earth", "mars", "venus");
        geologySourceCombo.setValue("none");
        geologySourceCombo.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || "none".equals(item) ? I18n.getOrDefault("planet.combo.prompt_source", "— Select Data Source —") : I18n.getOrDefault("planet.map." + item, item));
            }
        });
        geologySourceCombo.setButtonCell(geologySourceCombo.getCellFactory().call(null));
        geologySourceCombo.setMaxWidth(Double.MAX_VALUE);
        geologySourceCombo.setOnAction(e -> {
            if (isUpdatingFromPreset) return;
            String val = geologySourceCombo.getValue();
            if (val != null && !"none".equals(val)) {
                radioImportGeology.setSelected(true);
                updatePreviewCanvas();
            }
        });

        VBox geologyImportBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.param.map_source", "Celestial Body / Source:")), geologySourceCombo,
                        "Source satellite pour la carte géologique"),
                createControlRow(resourceMapRowLabel, new VBox(3, resourceBox, resourceFileLabel, geologyFormatHintLabel), I18n.getOrDefault("resource.desc.import_geology_map", "Import a multi-channel geological & ore map"))
        );
        geologyImportBox.getStyleClass().add("subcard-import-box");
        geologyImportBox.setVisible(false);
        geologyImportBox.setManaged(false);

        geologyGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isProc = sel == radioProcGeology;
            geologyProcBox.setVisible(isProc); geologyProcBox.setManaged(isProc);
            geologyImportBox.setVisible(!isProc); geologyImportBox.setManaged(!isProc);
            if (!isUpdatingFromPreset) {
                if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
                updatePreviewCanvas();
            }
        });

        VBox geologyDomainSection = createGeologyVectorAndLayersSection();

        // Legacy compatibility
        radioProcEco = radioProcBiome;
        radioImportEco = radioImportBiome;

        controlsBox.getChildren().addAll(
                headerLabel,
                ecologyPresetBar,
                planetSection,
                biomeDomainSection,
                hydroDomainSection,
                geologyDomainSection
        );


        controlsBox.setPrefWidth(460);
        controlsBox.setMinWidth(460);

        ScrollPane scrollControls = new ScrollPane(controlsBox);
        scrollControls.setFitToWidth(true);
        scrollControls.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollControls.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollControls.getStyleClass().add("transparent-scroll-pane");
        scrollControls.setPrefWidth(480);
        scrollControls.setMinWidth(480);
        scrollControls.setMaxWidth(480);

        // --- Center / Right View: Interactive Visualization & Summary ---
        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(10));
        centerBox.getStyleClass().add("subcard-section");

        rightViewTitle = new Label(I18n.getOrDefault("resource.title.right_view", "Dynamic 2D Equirectangular Planetary Preview"));
        rightViewTitle.getStyleClass().add("label-header");

        // View Mode Selector
        viewModeCombo = new ComboBox<>();
        viewModeCombo.getItems().addAll(
                I18n.getOrDefault("resource.view.section_eco", "────────── ECOLOGICAL & BIOME MAPS ──────────"),
                I18n.getOrDefault("resource.view.biomes", "🌿 Biomes & Vegetation Map (GtC)"),
                I18n.getOrDefault("resource.view.hydro", "🌊 Hydrographic & Rivers Map (Slope & Watercourses)"),
                I18n.getOrDefault("resource.view.heat", "🌋 Flux Thermique & Ceintures Tectoniques (mW/m²)"),
                I18n.getOrDefault("resource.view.section_geology", "────────── GEOLOGICAL TENSORS & ENERGIES ──────────"),
                "⛏ Gisements de Charbon (COAL)",
                "🛢 Réserves de Pétrole Brut & Fuel (CRUDE_OIL)",
                "🔥 Champs de Gaz Naturel (NATURAL_GAS)",
                "⚛ Minerais d'Uranium & Fission (URANIUM)",
                "🌌 Hélium-3 & Fusion Lunaires (HELIUM_3)",
                "⛓ Métaux Industriels Fer & Cuivre (IRON_COPPER)",
                "💎 Terres Rares & Métaux Précieux (PRECIOUS_REE)",
                "💧 Aquifères & Eau Douce (FRESHWATER_AQUIFERS)"
        );
        viewModeCombo.setValue(viewModeCombo.getItems().get(1));
        viewModeCombo.setMaxWidth(380);
        viewModeCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                } else if (item.startsWith("──────")) {
                    setText(item);
                    setDisable(true);
                    setStyle("-fx-font-weight: bold; -fx-opacity: 0.6; -fx-padding: 4 8;");
                } else {
                    setText(item);
                    setDisable(false);
                    setStyle("-fx-font-weight: normal;");
                }
            }
        });
        viewModeCombo.setOnAction(e -> {
            String val = viewModeCombo.getValue();
            if (val != null && val.startsWith("──────")) {
                viewModeCombo.setValue(viewModeCombo.getItems().get(1));
                return;
            }
            updateLegend();
            updatePreviewCanvas();
        });

        btnReliefOverlay = new ToggleButton(I18n.getOrDefault("resource.btn.relief_overlay", "⛰️ Relief"));
        btnReliefOverlay.getStyleClass().add("button-secondary");
        btnReliefOverlay.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.relief_overlay", "Overlay relief & slope map at 50% opacity for geographical reference")));
        btnReliefOverlay.setOnAction(e -> updatePreviewCanvas());

        HBox viewModeControlBar = new HBox(8, viewModeCombo, btnReliefOverlay);
        viewModeControlBar.setAlignment(Pos.CENTER);

        StackPane canvasContainer = new StackPane();
        canvasContainer.setStyle("-fx-background-color: black; -fx-border-color: #475569; -fx-border-radius: 6; -fx-background-radius: 6;");

        mapPreviewCanvas = new Canvas(640, 360);
        mapPreviewCanvas.getStyleClass().add("map-canvas-shadow");
        Tooltip.install(mapPreviewCanvas, new Tooltip(I18n.getOrDefault("resource.tooltip.preview", "Dynamic preview of geographical resource distribution")));

        // Clip container to prevent JavaFX Prism NGCanvas renderForClip 0x0 NPE
        javafx.scene.shape.Rectangle containerClip = new javafx.scene.shape.Rectangle();
        containerClip.widthProperty().bind(canvasContainer.widthProperty());
        containerClip.heightProperty().bind(canvasContainer.heightProperty());
        canvasContainer.setClip(containerClip);

        canvasContainer.widthProperty().addListener((obs, oldV, newV) -> {
            double w = Math.floor(newV.doubleValue());
            if (w >= 10.0 && Math.abs(w - mapPreviewCanvas.getWidth()) >= 4.0) {
                mapPreviewCanvas.setWidth(w);
                updatePreviewCanvas();
            }
        });
        canvasContainer.heightProperty().addListener((obs, oldV, newV) -> {
            double h = Math.floor(newV.doubleValue());
            if (h >= 10.0 && Math.abs(h - mapPreviewCanvas.getHeight()) >= 4.0) {
                mapPreviewCanvas.setHeight(h);
                updatePreviewCanvas();
            }
        });

        // Interactive Zoom & Pan Handlers
        mapPreviewCanvas.setOnScroll(e -> {
            double delta = e.getDeltaY();
            double factor = delta > 0 ? 1.15 : 0.85;
            double oldZoom = zoomFactor;
            double newZoom = Math.max(0.5, Math.min(20.0, oldZoom * factor));

            if (newZoom != oldZoom) {
                double mouseX = e.getX();
                double mouseY = e.getY();
                double w = mapPreviewCanvas.getWidth();
                double h = mapPreviewCanvas.getHeight();
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
                updatePreviewCanvas();
            }
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

        canvasContainer.getChildren().add(mapPreviewCanvas);
        VBox.setVgrow(canvasContainer, Priority.ALWAYS);

        legendBar = new HBox(10);
        legendBar.setAlignment(Pos.CENTER);
        legendBar.setPadding(new Insets(5, 10, 5, 10));
        legendBar.getStyleClass().add("card-section");
        updateLegend();

        summaryLabel = new Label(I18n.getOrDefault("resource.prompt.load_cells", "🌍 Generate or load planet cells to activate map."));
        summaryLabel.getStyleClass().addAll("control-label", "summary-centered-label");
        summaryLabel.setWrapText(true);

        centerBox.getChildren().addAll(rightViewTitle, viewModeControlBar, canvasContainer, legendBar, summaryLabel);
        VBox.setVgrow(centerBox, Priority.ALWAYS);

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

        double tBiomass = Math.round(450.0 * areaScale * waterMod * tempMod);
        double sCarbon = Math.round(1500.0 * areaScale * tempMod);
        double fBiomass = Math.round(2.0 * areaScale * waterMod * tempMod * 100.0) / 100.0;
        double aBiomass = Math.round(6.0 * areaScale * (1.0 + p.waterLevel() * 1.5) * 100.0) / 100.0;

        terrestrialBiomassSlider.setValue(tBiomass);
        soilCarbonSlider.setValue(sCarbon);
        faunaBiomassSlider.setValue(fBiomass);
        aquaticBiomassSlider.setValue(aBiomass);

        if (biomeStatusLabel != null) {
            biomeStatusLabel.setText(String.format(I18n.getOrDefault("resource.status.auto_derived_biome", "⚡ Auto-derived from %s: Plant=%.0f GtC | Soils=%.0f GtC | Fauna=%.2f GtC | Aquatic=%.2f GtC"),
                    p.name(), tBiomass, sCarbon, fBiomass, aBiomass));
        }

        if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
        updateSummary();
        updatePreviewCanvas();
        logger.info("Auto-derived ecological parameters for planet {}", p.name());
    }

    private void autoDeriveHydroFromPlanet() {
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        if (p == null) p = PlanetPreset.EARTH_LIKE;

        double radiusRatio = p.radiusKm() / 6371.0;
        double areaScale = Math.pow(radiusRatio, 2);
        double waterMod = Math.max(0.05, 1.0 + p.waterLevel());

        double aquifer = Math.round(15000.0 * areaScale * waterMod);
        freshwaterAquiferSlider.setValue(aquifer);

        if (hydroStatusLabel != null) {
            hydroStatusLabel.setText(String.format(I18n.getOrDefault("resource.status.auto_derived_hydro", "⚡ Auto-derived from %s: Groundwater / Aquifers=%.0f x10³ km³"), p.name(), aquifer));
        }

        if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
        updateSummary();
        updatePreviewCanvas();
        logger.info("Auto-derived hydrographical parameters for planet {}", p.name());
    }

    private void autoDeriveGeologyFromPlanet() {
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        if (p == null) p = PlanetPreset.EARTH_LIKE;

        double radiusRatio = p.radiusKm() / 6371.0;
        double volumeScale = Math.pow(radiusRatio, 3);

        double crustal = Math.round(80.0 * volumeScale);
        double precious = Math.round(1200.0 * volumeScale);
        // Smaller bodies cool faster -> lower mantle heat flow
        double heat = Math.round(Math.min(300.0, 87.0 * radiusRatio) * 10.0) / 10.0;
        double seismic = Math.round(Math.min(10.0, Math.max(0.0, p.seismicActivityLevel())) * 10.0) / 10.0;
        double volcanic = Math.round(Math.min(8.0, Math.max(0.0, p.volcanicActivityLevel())) * 10.0) / 10.0;

        crustalMetalSlider.setValue(crustal);
        preciousMetalSlider.setValue(precious);
        mantleHeatSlider.setValue(heat);

        if (seismicActivitySlider != null) {
            seismicActivitySlider.setValue(seismic);
        }
        if (volcanicActivitySlider != null) {
            volcanicActivitySlider.setValue(volcanic);
        }

        if (geologyStatusLabel != null) {
            geologyStatusLabel.setText(String.format(I18n.getOrDefault("resource.status.auto_derived_geo", "⚡ Auto-derived from %s: Metals=%.0f Gt | Precious=%.0f Mt | Mantle=%.1f mW/m² | Seismic=%.1f Mag | Volcanism=%.1f VEI"),
                    p.name(), crustal, precious, heat, seismic, volcanic));
        }

        if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
        updateSummary();
        updatePreviewCanvas();
        logger.info("Auto-derived geological parameters for planet {}", p.name());
    }

    private void updatePlanetContextDisplay() {
        if (planetContextLabel == null) return;
        PlanetPreset preset = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        if (preset == null) preset = PlanetPreset.EARTH_LIKE;

        int cellCount = activeCells != null ? activeCells.size() : 12482;
        String text = I18n.getOrDefault(
                "planet.context.fmt",
                "🪐 Planète Héritée : %s | Rayon : %,.0f km | Temp : %.1f°C | Eau : %.0f%% | Cellules : %,d",
                I18n.getPlanetPresetDisplayName(preset.name()),
                preset.radiusKm(),
                preset.averageTempC(),
                preset.waterLevel() * 100.0,
                cellCount
        );
        planetContextLabel.setText(text);
        updateThermodynamicSynthesisBadge(preset);
    }

    private void updateThermodynamicSynthesisBadge(PlanetPreset p) {
        if (thermoSynthesisBadge == null) return;
        if (p == null) p = PlanetPreset.EARTH_LIKE;

        StringBuilder sb = new StringBuilder();
        sb.append(I18n.getOrDefault("planet.thermo.header", "🔬 **Synthèse Thermodynamique (%s)** :\n", I18n.getPlanetPresetDisplayName(p.name())));

        if (p.atmospherePressureAtm() < 0.01) {
            sb.append(I18n.getOrDefault("planet.thermo.no_atmosphere", "• **Atmosphère absente ou extrêmement faible** (%s atm) : Eau liquide impossible à la surface (sublimation). Pluviométrie nulle, biomasse nulle.\n", String.format("%.3f", p.atmospherePressureAtm())));
        } else if (p.waterLevel() < -0.2) {
            sb.append(I18n.getOrDefault("planet.thermo.arid", "• **Monde Aride / Désertique** (Eau %s%%) : Aquifères restreints, faune concentrée dans les bassins continentaux.\n", String.format("%.0f", (1.0 + p.waterLevel()) * 50)));
        } else if (p.waterLevel() > 0.4) {
            sb.append(I18n.getOrDefault("planet.thermo.ocean", "• **Monde Océan** (Eau %s%%) : Biomasse marine hyper-dominante (phytoplancton), réserves en eau douce majeures.\n", String.format("%.0f", (1.0 + p.waterLevel()) * 50)));
        } else {
            sb.append(I18n.getOrDefault("planet.thermo.balanced", "• **Balanced Ecosystem (Earth-Like)**: Complete continental water cycle, optimal forest and wildlife biomass.\n"));
        }

        if (p.isSatellite() || p.radiusKm() < 3000) {
            sb.append(I18n.getOrDefault("planet.thermo.small_body", "• **Corps de Faible Rayon (%s km)** : Refroidissement du manteau rapide ➔ Flux thermique et métaux industriels réduits.", String.format("%.0f", p.radiusKm())));
        } else {
            sb.append(I18n.getOrDefault("planet.thermo.massive_body", "• **Corps Massif (%s km)** : Tectonique des plaques active, réserves métalliques crustales et minerais précieux abondants.", String.format("%.0f", p.radiusKm())));
        }

        thermoSynthesisBadge.setText(sb.toString());
    }

    private void adaptResourceSlidersToPlanet(PlanetPreset p) {
        if (p == null) return;
        boolean oldState = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            if (seismicActivitySlider != null) seismicActivitySlider.setValue(p.seismicActivityLevel());
            if (volcanicActivitySlider != null) volcanicActivitySlider.setValue(p.volcanicActivityLevel());

            if (p.isSatellite() || p.radiusKm() < 3000) { // Moon/Titan
                terrestrialBiomassSlider.setValue(10.0);
                soilCarbonSlider.setValue(50.0);
                faunaBiomassSlider.setValue(0.01);
                crustalMetalSlider.setValue(150.0);
                mantleHeatSlider.setValue(25.0);
                if (seismicActivitySlider != null) seismicActivitySlider.setValue(1.0);
                if (volcanicActivitySlider != null) volcanicActivitySlider.setValue(0.5);
            } else if (p.waterLevel() < -0.2) { // Arid / Mars
                terrestrialBiomassSlider.setValue(50.0);
                soilCarbonSlider.setValue(200.0);
                aquaticBiomassSlider.setValue(0.2);
                freshwaterAquiferSlider.setValue(1200.0);
                if (seismicActivitySlider != null) seismicActivitySlider.setValue(1.2);
                if (volcanicActivitySlider != null) volcanicActivitySlider.setValue(0.8);
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
        } finally {
            isUpdatingFromPreset = oldState;
        }
        updateSummary();
    }

    public EcologyPreset getSelectedEcologyPreset() {
        return ecologyPresetBar != null && ecologyPresetBar.getPresetCombo() != null ? ecologyPresetBar.getPresetCombo().getValue() : null;
    }

    public void applyEcologyPreset(EcologyPreset p) {
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

        boolean isEarthBased = p == EcologyPreset.EARTH_STANDARD
                || "Earth Standard Baseline".equalsIgnoreCase(p.name())
                || (p.planetPresetName() != null && (p.planetPresetName().toLowerCase().contains("earth") || p.planetPresetName().toLowerCase().contains("terre")));

        if (p.customBiomeBase64() != null) {
            customBiomeImage = ImageMapLoader.base64PngToImage(p.customBiomeBase64());
            if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.preset.biomes", "🌿 Preset Biomes"));
            if (radioImportBiome != null) radioImportBiome.setSelected(true);
        } else if (isEarthBased) {
            if (biomeSourceCombo != null) biomeSourceCombo.setValue("earth");
            try (var biomeStream = getClass().getResourceAsStream("/maps/earth_biomes.png")) {
                if (biomeStream != null) {
                    customBiomeImage = new Image(biomeStream);
                    if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.source.earth_modis_biomes", "🌿 Earth MODIS Biomes"));
                }
            } catch (Exception ex) {
                logger.warn("Could not load default earth biome map", ex);
            }
            if (radioImportBiome != null) radioImportBiome.setSelected(true);
        } else {
            customBiomeImage = null;
            if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
            if (radioProcBiome != null) radioProcBiome.setSelected(true);
            if (biomeSourceCombo != null) biomeSourceCombo.setValue("none");
        }

        if (p.customResourceBase64() != null) {
            customResourceImage = ImageMapLoader.base64PngToImage(p.customResourceBase64());
            if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("planet.preset.resources", "🪨 Preset Resources"));
            if (radioImportGeology != null) radioImportGeology.setSelected(true);
        } else if (isEarthBased) {
            if (geologySourceCombo != null) geologySourceCombo.setValue("earth");
            if (radioImportGeology != null) radioImportGeology.setSelected(true);
        } else {
            customResourceImage = null;
            if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
            if (radioProcGeology != null) radioProcGeology.setSelected(true);
            if (geologySourceCombo != null) geologySourceCombo.setValue("none");
        }

        if (p.customHydroBase64() != null) {
            customHydroImage = ImageMapLoader.base64PngToImage(p.customHydroBase64());
            if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("planet.preset.hydro", "💧 Preset Hydro"));
            if (radioImportHydro != null) radioImportHydro.setSelected(true);
        } else if (isEarthBased) {
            if (hydroSourceCombo != null) hydroSourceCombo.setValue("earth");
            fetchOnlineHydroData();
            if (radioImportHydro != null) radioImportHydro.setSelected(true);
        } else {
            customHydroImage = null;
            if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
            if (radioProcHydro != null) radioProcHydro.setSelected(true);
            if (hydroSourceCombo != null) hydroSourceCombo.setValue("none");
        }

        if (p.customClimateBase64() != null) {
            customClimateImage = ImageMapLoader.base64PngToImage(p.customClimateBase64());
            if (climateFileLabel != null) climateFileLabel.setText(I18n.getOrDefault("planet.preset.climate", "🌡️ Preset Climate"));
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        }
        if (p.customRainfallBase64() != null) {
            customRainfallImage = ImageMapLoader.base64PngToImage(p.customRainfallBase64());
            if (rainfallFileLabel != null) rainfallFileLabel.setText(I18n.getOrDefault("planet.preset.rainfall", "🌧️ Preset Rainfall"));
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        }
        if (p.customSeasonalityBase64() != null) {
            customSeasonalityImage = ImageMapLoader.base64PngToImage(p.customSeasonalityBase64());
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText(I18n.getOrDefault("planet.preset.seasonality", "☀️ Preset Seasonality"));
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        }

        if (ecologyPresetBar != null) {
            ecologyPresetBar.markClean(p);
        }
        isUpdatingFromPreset = false;
        updateSummary();
        updatePreviewCanvas();
    }

    private void loadCustomBiomeMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("resource.chooser.biome", "Load Biome Map (PNG/JPEG)"));
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
        chooser.setTitle(I18n.getOrDefault("resource.chooser.geology", "Load Geological & Ore Map (PNG/JPEG)"));
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
        chooser.setTitle(I18n.getOrDefault("planet.dialog.climate_load", "Load Climate / Thermal Map (PNG/JPEG)"));
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
        chooser.setTitle(I18n.getOrDefault("planet.dialog.rainfall_load", "Load Precipitation Map (PNG/JPEG)"));
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
        chooser.setTitle(I18n.getOrDefault("planet.dialog.seasonality_load", "Load Seasonality Map (PNG/JPEG)"));
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
            climateStatusLabel.setText(I18n.getOrDefault("planet.map.status_fetching", "WMS Download in Progress..."));
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
        dialog.setTitle(I18n.getOrDefault("planet.dialog.climate_help_title", "Climate Map Specifications"));
        dialog.setHeaderText(I18n.getOrDefault("planet.dialog.climate_help_header", "Supported Image Formats for Climate Import"));
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
        String elevSrc = activePreset != null ? activePreset.elevationMapSource() : null;

        boolean isCompatible = true;
        if ("earth".equalsIgnoreCase(sourceKey)) {
            if ("earth".equalsIgnoreCase(elevSrc)) {
                isCompatible = true;
            } else if (!planetName.contains("earth") && !planetName.contains("terre") && !planetName.contains("terran")
                    && !planetName.contains("terrestre") && !planetName.contains("standard") && !planetName.contains("gaia")
                    && !planetName.contains("archipel") && !planetName.contains("oceania") && !planetName.contains("océan") && !planetName.contains("ocean")) {
                isCompatible = false;
            }
        } else if ("mars".equalsIgnoreCase(sourceKey)) {
            if ("mars".equalsIgnoreCase(elevSrc)) {
                isCompatible = true;
            } else if (!planetName.contains("mars") && !planetName.contains("ares") && !planetName.contains("désert") && !planetName.contains("desert")) {
                isCompatible = false;
            }
        } else if ("venus".equalsIgnoreCase(sourceKey)) {
            if ("venus".equalsIgnoreCase(elevSrc)) {
                isCompatible = true;
            } else if (!planetName.contains("venus") && !planetName.contains("vénus") && !planetName.contains("hesperos")) {
                isCompatible = false;
            }
        } else if ("moon".equalsIgnoreCase(sourceKey)) {
            if ("moon".equalsIgnoreCase(elevSrc)) {
                isCompatible = true;
            } else if (!planetName.contains("lune") && !planetName.contains("moon") && !planetName.contains("selene") && !planetName.contains("titan") && !planetName.contains("cryo") && (activePreset == null || !activePreset.isSatellite())) {
                isCompatible = false;
            }
        }

        if (!isCompatible) {
            String activeDisplayName = activePreset != null ? activePreset.name() : "Standard";
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(I18n.getOrDefault("planet.dialog.incompatible_title", "Terrain Incompatibility"));
            alert.setHeaderText(I18n.getOrDefault("resource.dialog.incompatible_header", "Terrain / External Map Incompatibility"));
            alert.setContentText(String.format(
                "La carte de ressources/biomes '%s' ne peut pas être chargée sur le terrain actuel '%s'.\n\n" +
                "Motif : Le relief, le climat et les biomes du monde sélectionné à l'onglet 1 ne correspondent pas avec cette carte externe.",
                sourceKey.toUpperCase(), activeDisplayName
            ));
            alert.showAndWait();
            if (ecoCompatibilityLabel != null) {
                ecoCompatibilityLabel.setText(String.format(I18n.getOrDefault("resource.status.incompatible_map", "⚠️ Incompatibility: Map '%s' rejected on terrain '%s'"), sourceKey.toUpperCase(), activeDisplayName));
                ecoCompatibilityLabel.getStyleClass().setAll("compatibility-error");
            }
            return false;
        }

        if (ecoCompatibilityLabel != null) {
            String activeDisplayName = activePreset != null ? activePreset.name() : "Standard";
            ecoCompatibilityLabel.setText(String.format(I18n.getOrDefault("resource.status.compatible_map", "✅ Map '%s' verified and compatible with terrain '%s'"), sourceKey.toUpperCase(), activeDisplayName));
            ecoCompatibilityLabel.getStyleClass().setAll("compatibility-success");
        }
        return true;
    }

    /**
     * Detects the type of celestial body from the given PlanetPreset and automatically
     * selects the correct map source (earth / mars / moon / venus / none), which triggers
     * radio button switching and satellite data loading for biomes, hydro and geology.
     * This mirrors the automatic behaviour of Tab 1 when a planet preset is changed.
     */
    private void autoApplyMapsForPreset(PlanetPreset p) {
        if (p == null) return;
        String lower = p.name() != null ? p.name().toLowerCase() : "";
        String sourceKey;
        if (lower.contains("terre") || lower.contains("terran") || lower.contains("earth")) {
            sourceKey = "earth";
        } else if (lower.contains("mars") || lower.contains("ares")) {
            sourceKey = "mars";
        } else if (lower.contains("lune") || lower.contains("moon") || lower.contains("selene")) {
            sourceKey = "moon";
        } else if (lower.contains("vénus") || lower.contains("venus") || lower.contains("hesperos")) {
            sourceKey = "venus";
        } else {
            // Unknown body → stay procedural, do nothing
            return;
        }

        // Update the combo (isUpdatingFromPreset guards prevent feedback loops)
        isUpdatingFromPreset = true;
        if (mapSourceCombo != null) mapSourceCombo.setValue(sourceKey);
        if (biomeSourceCombo != null) biomeSourceCombo.setValue(sourceKey);
        if (geologySourceCombo != null) geologySourceCombo.setValue(sourceKey);
        if (hydroSourceCombo != null) hydroSourceCombo.setValue(sourceKey);
        isUpdatingFromPreset = false;

        // Now trigger the full map-load cascade
        applyPresetMapSource(sourceKey);
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
                ecoCompatibilityLabel.setText(I18n.getOrDefault("resource.status.no_map", "🪐 No external map loaded — Procedural mode active"));
                ecoCompatibilityLabel.getStyleClass().setAll("compatibility-neutral");
            }
            // Revert all domain radio buttons to procedural mode
            isUpdatingFromPreset = true;
            if (radioProcBiome != null) radioProcBiome.setSelected(true);
            if (radioProcHydro != null) radioProcHydro.setSelected(true);
            if (radioProcGeology != null) radioProcGeology.setSelected(true);
            isUpdatingFromPreset = false;
            updatePreviewCanvas();
            updateSummary();
            return;
        }

        if (!validateTerrainMapCompatibility(sourceKey)) {
            mapSourceCombo.setValue("none");
            return;
        }

        // Switch all domain radio buttons to Import mode
        isUpdatingFromPreset = true;
        if (radioImportBiome != null) radioImportBiome.setSelected(true);
        if (radioImportHydro != null) radioImportHydro.setSelected(true);
        if (radioImportGeology != null) radioImportGeology.setSelected(true);
        isUpdatingFromPreset = false;

        if ("earth".equals(sourceKey)) {
            try (var biomeStream = getClass().getResourceAsStream("/maps/earth_biomes.png")) {
                if (biomeStream != null) {
                    customBiomeImage = new Image(biomeStream);
                    if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.source.earth_modis_biomes", "🌿 Earth MODIS Biomes"));
                    if (radioImportBiome != null) radioImportBiome.setSelected(true);
                }
            } catch (Exception ex) {
                logger.warn("Could not load default earth biome resource map", ex);
            }
            if (biomeSourceCombo != null) biomeSourceCombo.setValue("earth");
            if (geologySourceCombo != null) geologySourceCombo.setValue("earth");
            if (hydroSourceCombo != null) hydroSourceCombo.setValue("earth");
            if (climateSourceCombo != null && climateSourceCombo.getItems().size() > 1) climateSourceCombo.setValue(climateSourceCombo.getItems().get(1));
            if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("resource.source.earth_geology", "🪨 Earth USGS Geology"));
            if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("resource.source.earth_hydro", "🌊 Earth SWBD Hydrography"));
            fetchOnlineHydroData();
            prepopulateEarthGeologyTensors();
            if (ecoCompatibilityLabel != null) {
                ecoCompatibilityLabel.setText(I18n.getOrDefault("resource.status.earth_loaded", "🌍 Earth maps loaded (MODIS Biomes + USGS Geology + SWBD Hydrography)"));
                ecoCompatibilityLabel.getStyleClass().setAll("compatibility-success");
            }
        } else if ("mars".equals(sourceKey)) {
            if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("resource.status.mars_biomes", "🌿 Mars Biomes (Procedural)"));
            if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("resource.source.mars_geology", "🪨 Mars MOLA Geology"));
            if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("resource.source.mars_hydro", "⚡ Hydrographie (Aride)"));
            if (geologySourceCombo != null) geologySourceCombo.setValue("mars");
            if (mapStatusLabel != null) mapStatusLabel.setText(I18n.getOrDefault("resource.status.mars_geology", "🔴 Source: Mars — MOLA Geology (procedural for hydro & biomes)"));
            if (radioProcHydro != null) radioProcHydro.setSelected(true);
        } else if ("moon".equals(sourceKey)) {
            if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("resource.source.moon_biomes", "🌿 Lune (Sans Biomes)"));
            if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("resource.source.moon_geology", "🪨 Lune Topographie & Regolithe"));
            if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("resource.source.moon_hydro", "⚡ Hydrographie (Nulle)"));
            if (mapStatusLabel != null) mapStatusLabel.setText(I18n.getOrDefault("resource.status.moon_topo", "🌕 Source: Moon — Procedural topography"));
            if (radioProcHydro != null) radioProcHydro.setSelected(true);
            if (radioProcBiome != null) radioProcBiome.setSelected(true);
        } else if ("venus".equals(sourceKey)) {
            if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("resource.status.venus_atmo", "🌿 Venus (Extreme atmosphere)"));
            if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("resource.status.venus_radar", "🪨 Venus Magellan Radar"));
            if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("resource.status.venus_hydro", "⚡ Hydrography (Sublimated)"));
            if (mapStatusLabel != null) mapStatusLabel.setText(I18n.getOrDefault("resource.status.venus_source", "♀ Source: Venus — Magellan Radar"));
            if (radioProcHydro != null) radioProcHydro.setSelected(true);
            if (radioProcBiome != null) radioProcBiome.setSelected(true);
        }
        updatePreviewCanvas();
        updateSummary();
    }

    private void loadCustomHydroMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("resource.chooser.hydro", "Load Hydrographic Map (PNG/JPEG)"));
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
            mapStatusLabel.setText(I18n.getOrDefault("planet.map.status_fetching", "Downloading satellite hydrography WMS..."));

        onlineMapService.fetchHydroMapAsync(body).thenAccept(img -> javafx.application.Platform.runLater(() -> {
            if (img != null) {
                customHydroImage = img;
                if (hydroFileLabel != null) hydroFileLabel.setText("🌐 " + body.getName() + " USGS HydroSHEDS / SWBD Hydro WMS");
                if (mapStatusLabel != null) mapStatusLabel.setText(I18n.getOrDefault("planet.map.status_success", "Hydrographic map downloaded successfully."));
                updatePreviewCanvas();
                updateSummary();
            } else {
                if ("earth".equals(sourceKey) && radioImportHydro != null && radioImportHydro.isSelected()) {
                    if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("resource.source.earth_hydro_wms", "🌊 Earth SWBD / USGS HydroSHEDS Hydrographie"));
                    if (mapStatusLabel != null) mapStatusLabel.setText(I18n.getOrDefault("resource.status.usgs_active", "🌍 Earth Hydrographic Map USGS HydroSHEDS / SWBD active."));
                    updatePreviewCanvas();
                    updateSummary();
                } else {
                    if (mapStatusLabel != null) mapStatusLabel.setText(I18n.getOrDefault("resource.map.procedural_fallback", "Info: Falling back to procedural hydrographic map (slope)."));
                    generateProceduralHydrography();
                }
            }
        }));
    }

    private void generateProceduralHydrography() {
        customHydroImage = null;
        if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("resource.mode.procedural_hydro", "⚡ Procedural Hydrography (Rivers/Slope)"));
        if (mapStatusLabel != null) mapStatusLabel.setText(I18n.getOrDefault("resource.map.procedural_generated", "⚡ Procedural hydrographic map generated via slope and watershed calculation."));
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

        mapStatusLabel.setText(I18n.getOrDefault("planet.map.status_fetching", "WMS Download in Progress..."));

        if (body.getBiomeWmsUrl() != null) {
            onlineMapService.fetchBiomeMapAsync(body).thenAccept(img -> {
                javafx.application.Platform.runLater(() -> {
                    if (img != null) {
                        customBiomeImage = img;
                        biomeFileLabel.setText("🌐 " + body.getName() + " WMS Biomes");
                        mapStatusLabel.setText(I18n.getOrDefault("planet.map.status_success", "Satellite map downloaded successfully."));
                        updatePreviewCanvas();
                        updateSummary();
                    } else {
                        mapStatusLabel.setText(I18n.getOrDefault("planet.map.status_error", "Download error."));
                    }
                });
            });
        } else {
            mapStatusLabel.setText(I18n.getOrDefault("resource.map.wms_fallback", "Info: No specific WMS for ") + body.getName() + I18n.getOrDefault("resource.map.using_procedural", ", using procedural model."));
        }
    }

    private void exportMapsWithWorldFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("resource.chooser.worldfile", "Export map image with ESRI World File (.tfw)"));
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
        dialog.setTitle(I18n.getOrDefault("resource.dialog.specs_title", "Biome & Geology Map Specifications"));
        dialog.setHeaderText(I18n.getOrDefault("resource.dialog.specs_header", "Expected Image Formats and Geological Data"));
        dialog.setContentText(
                I18n.getOrDefault("resource.dialog.specs_content",
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
                "   • L'export génère automatiquement un fichier compagnon .tfw pour l'alignement dans les logiciels GIS (QGIS, ArcGIS).")
        );
        dialog.showAndWait();
    }

    private static final double[][] COAL_SPOTS = {
        {-80.0, 40.0, 45, 1.2}, {-88.0, 39.0, 40, 1.1}, {7.0, 51.5, 30, 1.0}, {19.0, 50.0, 30, 1.0},
        {38.0, 48.0, 35, 1.1}, {86.0, 55.0, 50, 1.3}, {112.0, 37.0, 55, 1.4}, {108.0, 39.0, 45, 1.2},
        {148.0, -23.0, 45, 1.2}, {150.0, -32.0, 40, 1.1}, {86.0, 23.5, 40, 1.1}, {29.0, -26.0, 35, 1.0}
    };
    private static final double[][] OIL_SPOTS = {
        {48.0, 26.0, 65, 1.5}, {51.0, 29.0, 60, 1.4}, {47.0, 30.5, 55, 1.3}, {76.0, 61.0, 55, 1.3},
        {-102.0, 32.0, 50, 1.2}, {-90.0, 28.5, 45, 1.1}, {-65.0, 8.5, 45, 1.2}, {-71.5, 10.0, 40, 1.1},
        {6.0, 4.5, 40, 1.1}, {-41.0, -22.5, 45, 1.2}, {2.0, 57.5, 40, 1.1}, {125.0, 46.5, 35, 1.0},
        {-148.0, 70.0, 35, 1.0}, {50.0, 40.0, 40, 1.1}
    };
    private static final double[][] GAS_SPOTS = {
        {77.0, 66.0, 65, 1.5}, {73.0, 67.5, 60, 1.4}, {52.0, 26.5, 60, 1.5}, {-77.5, 41.5, 50, 1.2},
        {-95.0, 35.0, 45, 1.1}, {6.8, 53.2, 35, 1.0}, {3.3, 32.9, 45, 1.2}, {62.2, 37.3, 45, 1.2},
        {105.0, 30.5, 40, 1.1}, {80.0, 62.0, 45, 1.2}
    };
    private static final double[][] URANIUM_SPOTS = {
        {-105.5, 58.0, 50, 1.4}, {136.9, -30.4, 45, 1.4}, {68.0, 44.0, 55, 1.4}, {66.0, 43.0, 45, 1.2},
        {7.4, 18.7, 40, 1.1}, {27.5, -26.2, 40, 1.1}, {118.0, 50.0, 40, 1.1}, {-109.0, 38.0, 45, 1.1},
        {15.0, -22.5, 35, 1.0}
    };
    private static final double[][] HE3_SPOTS = {
        {23.5, 8.5, 50, 1.2}, {-43.0, 18.0, 60, 1.2}, {17.5, 28.0, 45, 1.1}
    };
    private static final double[][] IRON_COPPER_SPOTS = {
        // Banded Iron Formations (BIF)
        {118.5, -22.5, 60, 1.4}, {-50.0, -6.0, 55, 1.3}, {-43.5, -20.0, 45, 1.2}, {-66.0, 53.0, 50, 1.2},
        {36.5, 51.5, 55, 1.3}, {33.5, 48.0, 45, 1.2}, {-92.5, 47.5, 45, 1.2}, {85.0, 22.0, 50, 1.3},
        {123.0, 41.0, 50, 1.3}, {23.0, -27.5, 45, 1.1}, {20.2, 67.8, 35, 1.1},
        // Copper Belts (Andes Porphyry, N.A. Cordillera, Central Africa, Kazakhstan, PNG)
        {-69.0, -24.0, 75, 1.5}, {-76.0, -12.0, 65, 1.4}, {-70.5, -33.5, 55, 1.3},
        {-110.0, 33.0, 70, 1.4}, {-112.0, 40.5, 55, 1.2}, {-122.0, 53.0, 60, 1.2},
        {28.0, -12.5, 60, 1.4}, {75.0, 47.0, 65, 1.3}, {60.0, 56.0, 50, 1.2},
        {137.1, -4.0, 45, 1.3}, {106.8, 43.0, 55, 1.3}, {145.0, -32.0, 45, 1.1}
    };
    private static final double[][] PRECIOUS_REE_SPOTS = {
        {27.5, -25.5, 50, 1.5}, {109.9, 41.8, 55, 1.5}, {-67.5, -21.0, 65, 1.4},
        {27.0, -26.5, 45, 1.3}, {-116.0, 40.8, 40, 1.2}, {122.5, -28.7, 45, 1.3},
        {116.0, -33.8, 40, 1.2}, {64.6, 41.5, 40, 1.2}, {88.2, 69.3, 45, 1.3},
        {-115.5, 35.5, 35, 1.1}
    };
    private static final double[][] MANTLE_HEAT_SPOTS = {
        {-155.5, 19.8, 35, 1.3}, {-178.0, -29.0, 50, 1.2}, {-72.0, -15.0, 65, 1.3},
        {140.0, 36.0, 55, 1.2}, {43.0, 11.5, 45, 1.3}, {-25.0, 64.8, 50, 1.2},
        {14.0, 40.8, 40, 1.1}, {-110.5, 44.4, 40, 1.2}, {105.0, -5.0, 50, 1.2}
    };
    private static final double[][] AQUIFER_SPOTS = {
        {-54.0, -25.0, 75, 1.3}, {-60.0, -3.0, 85, 1.3}, {25.0, 22.0, 80, 1.4},
        {-100.0, 38.0, 60, 1.2}, {80.0, 27.0, 70, 1.3}, {138.0, -26.0, 75, 1.3},
        {6.0, 30.0, 65, 1.2}, {75.0, 60.0, 75, 1.3}
    };

    private static double sampleHotspotVal(double lng, double lat, double[][] spots) {
        double val = 0.0;
        for (double[] spot : spots) {
            double dlng = lng - spot[0];
            double dlat = lat - spot[1];
            double dist = Math.sqrt(dlng * dlng + dlat * dlat);
            double radDeg = spot[2] / 4.0;
            if (dist < radDeg) {
                double intensity = spot.length > 3 ? spot[3] : 1.0;
                val += Math.pow(1.0 - (dist / radDeg), 1.5) * intensity;
            }
        }
        return Math.clamp(val, 0.0, 1.0);
    }

    private Color blendColors(Color base, Color overlay, double opacity) {
        if (base == null) return overlay;
        double r = base.getRed() * (1.0 - opacity) + overlay.getRed() * opacity;
        double g = base.getGreen() * (1.0 - opacity) + overlay.getGreen() * opacity;
        double b = base.getBlue() * (1.0 - opacity) + overlay.getBlue() * opacity;
        return Color.color(Math.clamp(r, 0.0, 1.0), Math.clamp(g, 0.0, 1.0), Math.clamp(b, 0.0, 1.0));
    }

    private Color getReliefShadeColor(double elevation, double waterLevel, double declivity) {
        if (elevation < waterLevel) {
            double depthNorm = Math.clamp((waterLevel - elevation) / 2000.0, 0.0, 1.0);
            double val = 0.05 + 0.15 * (1.0 - depthNorm);
            return Color.color(val * 0.5, val * 0.7, val);
        } else {
            double hNorm = Math.clamp((elevation - waterLevel) / 3000.0, 0.0, 1.0);
            double shade = 0.2 + 0.6 * hNorm + 0.2 * Math.min(1.0, declivity * 2.0);
            return Color.color(shade, shade, shade);
        }
    }

    private int getViewModeIndex() {
        if (viewModeCombo == null) return 0;
        int selected = viewModeCombo.getSelectionModel().getSelectedIndex();
        if (selected >= 1 && selected <= 11) {
            return selected - 1;
        } else if (selected >= 13 && selected <= 15) {
            return selected - 2;
        }

        if (viewModeCombo.getValue() == null) return 0;
        String lower = viewModeCombo.getValue().toLowerCase();
        if (lower.contains("14. ") || lower.contains("aridité") || lower.contains("salinis") || lower.contains("aridity")) return 13;
        if (lower.contains("13. ") || lower.contains("sismique") || lower.contains("tectonique") || lower.contains("seismic")) return 12;
        if (lower.contains("12. ") || lower.contains("température") || lower.contains("microclimat") || lower.contains("temp")) return 11;
        if (lower.contains("11. ") || lower.contains("aquifère") || lower.contains("aquifer") || lower.contains("4.9")) return 10;
        if (lower.contains("10. ") || lower.contains("mantle") || lower.contains("thermique") || lower.contains("4.8")) return 9;
        if (lower.contains("9. ") || lower.contains("précieux") || lower.contains("precious") || lower.contains("4.7")) return 8;
        if (lower.contains("8. ") || lower.contains("fer") || lower.contains("copper") || lower.contains("iron") || lower.contains("4.6")) return 7;
        if (lower.contains("7. ") || lower.contains("hélium") || lower.contains("helium") || lower.contains("4.5")) return 6;
        if (lower.contains("6. ") || lower.contains("uranium") || lower.contains("4.4")) return 5;
        if (lower.contains("5. ") || lower.contains("gaz") || lower.contains("gas") || lower.contains("4.3")) return 4;
        if (lower.contains("4. ") || lower.contains("pétrole") || lower.contains("oil") || lower.contains("4.2")) return 3;
        if (lower.contains("3. ") || lower.contains("charbon") || lower.contains("coal") || lower.contains("4.1")) return 2;
        if (lower.contains("2. ") || lower.contains("hydrographie") || lower.contains("hydro")) return 1;
        if (lower.contains("1. ") || lower.contains("biome")) return 0;
        return 0;
    }

    private void updateLegend() {
        if (legendBar == null) return;
        legendBar.getChildren().clear();

        int selectedIdx = getViewModeIndex();
        if (selectedIdx == 0) { // Biomes
            addLegendItem("DEEP_OCEAN", Color.rgb(0, 0, 100), I18n.getOrDefault("resource.legend.deep_ocean", "Deep Ocean / Abyss"));
            addLegendItem("OCEAN", Color.rgb(0, 50, 200), I18n.getOrDefault("resource.legend.ocean", "Ocean / Continental Shelf"));
            addLegendItem("PLAINS", Color.rgb(100, 200, 50), I18n.getOrDefault("resource.legend.plains", "Grasslands & Plains"));
            addLegendItem("FOREST", Color.rgb(20, 120, 20), I18n.getOrDefault("resource.legend.forest", "Temperate & Boreal Forest"));
            addLegendItem("JUNGLE", Color.rgb(0, 80, 0), I18n.getOrDefault("resource.legend.jungle", "Tropical Rainforest / Jungle"));
            addLegendItem("DESERT", Color.rgb(255, 200, 50), I18n.getOrDefault("resource.legend.desert", "Arid Desert & Scrub"));
            addLegendItem("MOUNTAINS", Color.rgb(100, 100, 100), I18n.getOrDefault("resource.legend.mountains", "High Altitude / Mountains"));
        } else if (selectedIdx == 1) { // Hydrography & River Networks
            addLegendItem("DEEP_OCEAN", Color.rgb(15, 23, 42), I18n.getOrDefault("resource.legend.hydro_ocean", "Ocean / Abyss"));
            addLegendItem("MAJOR_RIVER", Color.rgb(2, 132, 199), I18n.getOrDefault("resource.legend.major_river", "Major Rivers"));
            addLegendItem("RIVER_STREAM", Color.rgb(56, 189, 248), I18n.getOrDefault("resource.legend.river_stream", "Streams & Watercourses"));
            addLegendItem("TRIBUTARY", Color.rgb(20, 184, 166), I18n.getOrDefault("resource.legend.tributary", "Tributaries & Drainage"));
            addLegendItem("LAND_SLOPE", Color.rgb(75, 85, 99), I18n.getOrDefault("resource.legend.land_slope", "Continental Relief"));
        } else if (selectedIdx == 2) { // COAL (Mode 2)
            addLegendItem("LOW", Color.rgb(30, 30, 30), I18n.getOrDefault("resource.legend.coal_low", "Sterile / Traces"));
            addLegendItem("MED", Color.rgb(180, 100, 20), I18n.getOrDefault("resource.legend.coal_med", "Coal Basin"));
            addLegendItem("HIGH", Color.rgb(255, 140, 0), I18n.getOrDefault("resource.legend.coal_high", "Major Anthracite Deposit"));
        } else if (selectedIdx == 3) { // CRUDE OIL (Mode 3)
            addLegendItem("LOW", Color.rgb(30, 20, 20), I18n.getOrDefault("resource.legend.oil_low", "Sterile / Traces"));
            addLegendItem("MED", Color.rgb(180, 40, 40), I18n.getOrDefault("resource.legend.oil_med", "Oil Field"));
            addLegendItem("HIGH", Color.rgb(220, 38, 38), I18n.getOrDefault("resource.legend.oil_high", "Supergiant Bituminous Basin"));
        } else if (selectedIdx == 4) { // NATURAL GAS (Mode 4)
            addLegendItem("LOW", Color.rgb(10, 30, 40), I18n.getOrDefault("resource.legend.gas_low", "Sterile / Traces"));
            addLegendItem("MED", Color.rgb(20, 140, 160), I18n.getOrDefault("resource.legend.gas_med", "Gas Field"));
            addLegendItem("HIGH", Color.rgb(6, 182, 212), I18n.getOrDefault("resource.legend.gas_high", "Natural Gas Superfield"));
        } else if (selectedIdx == 5) { // URANIUM (Mode 5)
            addLegendItem("LOW", Color.rgb(10, 40, 20), I18n.getOrDefault("resource.legend.uranium_low", "Traces"));
            addLegendItem("MED", Color.rgb(20, 140, 60), I18n.getOrDefault("resource.legend.uranium_med", "Uranium Ore Deposit"));
            addLegendItem("HIGH", Color.rgb(34, 197, 94), I18n.getOrDefault("resource.legend.uranium_high", "High-Grade Pitchblende"));
        } else if (selectedIdx == 6) { // HELIUM-3 (Mode 6)
            addLegendItem("LOW", Color.rgb(30, 10, 30), I18n.getOrDefault("resource.legend.he3_low", "Regolith Traces"));
            addLegendItem("MED", Color.rgb(140, 30, 140), I18n.getOrDefault("resource.legend.he3_med", "Enriched Regolith"));
            addLegendItem("HIGH", Color.rgb(217, 70, 239), I18n.getOrDefault("resource.legend.he3_high", "Lunar Helium-3 Concentrate"));
        } else if (selectedIdx == 7) { // IRON & COPPER (Mode 7)
            addLegendItem("LOW", Color.rgb(40, 30, 20), I18n.getOrDefault("resource.legend.iron_low", "Low Grade"));
            addLegendItem("MED", Color.rgb(180, 90, 20), I18n.getOrDefault("resource.legend.iron_med", "Banded Iron Formation"));
            addLegendItem("HIGH", Color.rgb(249, 115, 22), I18n.getOrDefault("resource.legend.iron_high", "Massive Iron & Copper Deposit"));
        } else if (selectedIdx == 8) { // PRECIOUS METALS & REE (Mode 8)
            addLegendItem("LOW", Color.rgb(40, 35, 10), I18n.getOrDefault("resource.legend.precious_low", "Gold/REE Traces"));
            addLegendItem("MED", Color.rgb(170, 130, 20), I18n.getOrDefault("resource.legend.precious_med", "Placer & Gold Vein"));
            addLegendItem("HIGH", Color.rgb(234, 179, 8), I18n.getOrDefault("resource.legend.precious_high", "Major Rare Earth Basin"));
        } else if (selectedIdx == 9) { // MANTLE HEAT FLUX (Mode 9)
            addLegendItem("LOW", Color.rgb(40, 40, 60), I18n.getOrDefault("resource.legend.heat_low", "Inert / Stable Craton"));
            addLegendItem("MED", Color.rgb(180, 80, 30), I18n.getOrDefault("resource.legend.heat_med", "Mean Geothermal Flux"));
            addLegendItem("HIGH", Color.rgb(240, 20, 20), I18n.getOrDefault("resource.legend.heat_high", "High Magmatism & Tectonic Rifts"));
        } else if (selectedIdx == 10) { // AQUIFERS & FRESHWATER (Mode 10)
            addLegendItem("LOW", Color.rgb(20, 40, 80), I18n.getOrDefault("resource.legend.aquifer_low", "Arid / Dry"));
            addLegendItem("MED", Color.rgb(40, 120, 200), I18n.getOrDefault("resource.legend.aquifer_med", "Moderate Aquifer"));
            addLegendItem("HIGH", Color.rgb(0, 220, 255), I18n.getOrDefault("resource.legend.aquifer_high", "Giant Basin Aquifer"));
        } else if (selectedIdx == 11) { // SURFACE TEMPERATURE & MICROCLIMATES
            addLegendItem("POLAR", Color.rgb(35, 120, 230), I18n.getOrDefault("resource.legend.temp_polar", "Polar / Glacial (< 0°C)"));
            addLegendItem("TEMPERATE", Color.rgb(80, 200, 120), I18n.getOrDefault("resource.legend.temp_temperate", "Temperate (10°C - 22°C)"));
            addLegendItem("TROPICAL", Color.rgb(250, 130, 20), I18n.getOrDefault("resource.legend.temp_tropical", "Tropical / Warm (22°C - 34°C)"));
            addLegendItem("TORRID", Color.rgb(245, 20, 40), I18n.getOrDefault("resource.legend.temp_torrid", "Torrid Extreme (> 34°C)"));
        } else if (selectedIdx == 12) { // SEISMIC & VOLCANIC TECTONISM
            addLegendItem("QUIET", Color.rgb(40, 45, 55), I18n.getOrDefault("resource.legend.seismic_quiet", "Stable Shield / Craton"));
            addLegendItem("MODERATE", Color.rgb(200, 100, 30), I18n.getOrDefault("resource.legend.seismic_med", "Active Fault / Orogeny"));
            addLegendItem("HIGH", Color.rgb(245, 30, 30), I18n.getOrDefault("resource.legend.seismic_high", "Subduction & Volcanic Arc"));
        } else if (selectedIdx == 13) { // ARIDITY & SOIL SALINIZATION
            addLegendItem("HUMID", Color.rgb(30, 140, 160), I18n.getOrDefault("resource.legend.aridity_humid", "Humid / Fertile Soils"));
            addLegendItem("SEMI_ARID", Color.rgb(210, 150, 50), I18n.getOrDefault("resource.legend.aridity_semi", "Semi-Arid Steppe"));
            addLegendItem("HYPER_ARID", Color.rgb(230, 60, 20), I18n.getOrDefault("resource.legend.aridity_hyper", "Hyper-Arid Salt Desert"));
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
        if (canvasW < 1 || canvasH < 1) return;

        int mode = getViewModeIndex();
        PlanetPreset planet = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        if (planet == null) planet = PlanetPreset.EARTH_LIKE;
        if (seismicActivitySlider != null || volcanicActivitySlider != null) {
            double sVal = seismicActivitySlider != null ? seismicActivitySlider.getValue() : planet.seismicActivityLevel();
            double vVal = volcanicActivitySlider != null ? volcanicActivitySlider.getValue() : planet.volcanicActivityLevel();
            planet = planet.withSeismicAndVolcanic(sVal, vVal);
        }
        int w = 960;
        int h = 480;

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
                    if (radioImportBiome != null && radioImportBiome.isSelected() && customBiomeReader != null) {
                        int bx = (int) Math.min((x_base / (double) w) * wBio, wBio - 1);
                        int by = (int) Math.min((y_base / (double) h) * hBio, hBio - 1);
                        Color rawC = customBiomeReader.getColor(bx, by);
                        pxColor = mapLoader.getBiomeTargetColor(mapLoader.matchBiomeColor(rawC));
                    } else {
                        var point = generator.getPlanetPoint(lat, lon, planet);
                        pxColor = mapLoader.getBiomeTargetColor(point.biome());
                    }
                } else if (mode == 1) { // Hydrography & River Networks Map
                    if (customHydroReader != null) {
                        int hx = (int) Math.min((x_base / (double) w) * wHydro, wHydro - 1);
                        int hy = (int) Math.min((y_base / (double) h) * hHydro, hHydro - 1);
                        Color c = customHydroReader.getColor(hx, hy);
                        double waterIntensity = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                        if (waterIntensity > 0.6) {
                            pxColor = Color.rgb(2, 132, 199);
                        } else if (waterIntensity > 0.3) {
                            pxColor = Color.rgb(56, 189, 248);
                        } else {
                            var pt = generator.getPlanetPoint(lat, lon, planet);
                            if (pt.elevation() < planet.waterLevel()) {
                                pxColor = Color.rgb(15, 23, 42);
                            } else {
                                int r = (int) Math.min(255, 45 + pt.declivity() * 100);
                                int g = (int) Math.min(255, 60 + pt.declivity() * 80);
                                int b = (int) Math.min(255, 55 + pt.declivity() * 50);
                                pxColor = Color.rgb(r, g, b);
                            }
                        }
                    } else {
                        var point = generator.getPlanetPoint(lat, lon, planet);
                        if (point.elevation() < planet.waterLevel()) {
                            pxColor = Color.rgb(15, 23, 42);
                        } else {
                            double riverFlow = point.riverFlow();
                            double declivity = point.declivity();
                            if (riverFlow > 0.42) {
                                pxColor = Color.rgb(2, 132, 199);
                            } else if (riverFlow > 0.28) {
                                pxColor = Color.rgb(56, 189, 248);
                            } else if (riverFlow > 0.16) {
                                pxColor = Color.rgb(20, 184, 166);
                            } else {
                                int r = (int) Math.min(255, 45 + declivity * 100);
                                int g = (int) Math.min(255, 60 + declivity * 80);
                                int b = (int) Math.min(255, 55 + declivity * 50);
                                pxColor = Color.rgb(r, g, b);
                            }
                        }
                    }
                } else if (mode == 2) { // COAL (Layer 0 / 4.1)
                    var point = generator.getPlanetPoint(lat, lon, planet);
                    if (point.elevation() < planet.waterLevel()) {
                        pxColor = Color.rgb(15, 23, 42);
                    } else {
                        double val;
                        boolean useImport = geologyImportRadios.get(0) != null && geologyImportRadios.get(0).isSelected();
                        Image gImg = customGeologyLayerImages.get(0);
                        if (useImport && gImg != null && gImg.getPixelReader() != null) {
                            int gx = (int) Math.min((x_base / (double) w) * gImg.getWidth(), gImg.getWidth() - 1);
                            int gy = (int) Math.min((y_base / (double) h) * gImg.getHeight(), gImg.getHeight() - 1);
                            Color c = gImg.getPixelReader().getColor(gx, gy);
                            val = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                        } else {
                            val = sampleProceduralGeologyTensor(0, lon, lat, planet);
                        }
                        if (val > 0.05) {
                            int r = (int) Math.min(255, 60 + val * 195);
                            int g = (int) Math.min(255, 30 + val * 110);
                            int b = (int) Math.min(255, 10);
                            pxColor = Color.rgb(r, g, b);
                        } else {
                            pxColor = Color.rgb(30, 41, 59);
                        }
                    }
                } else if (mode == 3) { // CRUDE OIL (Layer 1 / 4.2)
                    var point = generator.getPlanetPoint(lat, lon, planet);
                    if (point.elevation() < planet.waterLevel()) {
                        pxColor = Color.rgb(15, 23, 42);
                    } else {
                        double val;
                        boolean useImport = geologyImportRadios.get(1) != null && geologyImportRadios.get(1).isSelected();
                        Image gImg = customGeologyLayerImages.get(1);
                        if (useImport && gImg != null && gImg.getPixelReader() != null) {
                            int gx = (int) Math.min((x_base / (double) w) * gImg.getWidth(), gImg.getWidth() - 1);
                            int gy = (int) Math.min((y_base / (double) h) * gImg.getHeight(), gImg.getHeight() - 1);
                            Color c = gImg.getPixelReader().getColor(gx, gy);
                            val = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                        } else {
                            val = sampleProceduralGeologyTensor(1, lon, lat, planet);
                        }
                        if (val > 0.05) {
                            int r = (int) Math.min(255, 80 + val * 175);
                            int g = (int) Math.min(255, 15 + val * 35);
                            int b = (int) Math.min(255, 15 + val * 35);
                            pxColor = Color.rgb(r, g, b);
                        } else {
                            pxColor = Color.rgb(30, 41, 59);
                        }
                    }
                } else if (mode == 4) { // NATURAL GAS (Layer 2 / 4.3)
                    var point = generator.getPlanetPoint(lat, lon, planet);
                    if (point.elevation() < planet.waterLevel()) {
                        pxColor = Color.rgb(15, 23, 42);
                    } else {
                        double val;
                        boolean useImport = geologyImportRadios.get(2) != null && geologyImportRadios.get(2).isSelected();
                        Image gImg = customGeologyLayerImages.get(2);
                        if (useImport && gImg != null && gImg.getPixelReader() != null) {
                            int gx = (int) Math.min((x_base / (double) w) * gImg.getWidth(), gImg.getWidth() - 1);
                            int gy = (int) Math.min((y_base / (double) h) * gImg.getHeight(), gImg.getHeight() - 1);
                            Color c = gImg.getPixelReader().getColor(gx, gy);
                            val = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                        } else {
                            val = sampleProceduralGeologyTensor(2, lon, lat, planet);
                        }
                        if (val > 0.05) {
                            int r = (int) Math.min(255, 6 + val * 20);
                            int g = (int) Math.min(255, 100 + val * 120);
                            int b = (int) Math.min(255, 140 + val * 115);
                            pxColor = Color.rgb(r, g, b);
                        } else {
                            pxColor = Color.rgb(30, 41, 59);
                        }
                    }
                } else if (mode == 5) { // URANIUM (Layer 3 / 4.4)
                    var point = generator.getPlanetPoint(lat, lon, planet);
                    if (point.elevation() < planet.waterLevel()) {
                        pxColor = Color.rgb(15, 23, 42);
                    } else {
                        double val;
                        boolean useImport = geologyImportRadios.get(3) != null && geologyImportRadios.get(3).isSelected();
                        Image gImg = customGeologyLayerImages.get(3);
                        if (useImport && gImg != null && gImg.getPixelReader() != null) {
                            int gx = (int) Math.min((x_base / (double) w) * gImg.getWidth(), gImg.getWidth() - 1);
                            int gy = (int) Math.min((y_base / (double) h) * gImg.getHeight(), gImg.getHeight() - 1);
                            Color c = gImg.getPixelReader().getColor(gx, gy);
                            val = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                        } else {
                            val = sampleProceduralGeologyTensor(3, lon, lat, planet);
                        }
                        if (val > 0.05) {
                            int r = (int) Math.min(255, 10 + val * 30);
                            int g = (int) Math.min(255, 120 + val * 135);
                            int b = (int) Math.min(255, 30 + val * 70);
                            pxColor = Color.rgb(r, g, b);
                        } else {
                            pxColor = Color.rgb(30, 41, 59);
                        }
                    }
                } else if (mode == 6) { // HELIUM-3 (Layer 4 / 4.5)
                    double val;
                    boolean useImport = geologyImportRadios.get(4) != null && geologyImportRadios.get(4).isSelected();
                    Image gImg = customGeologyLayerImages.get(4);
                    if (useImport && gImg != null && gImg.getPixelReader() != null) {
                        int gx = (int) Math.min((x_base / (double) w) * gImg.getWidth(), gImg.getWidth() - 1);
                        int gy = (int) Math.min((y_base / (double) h) * gImg.getHeight(), gImg.getHeight() - 1);
                        Color c = gImg.getPixelReader().getColor(gx, gy);
                        val = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                    } else {
                        val = sampleProceduralGeologyTensor(4, lon, lat, planet);
                    }
                    if (val > 0.05) {
                        int r = (int) Math.min(255, 120 + val * 135);
                        int g = (int) Math.min(255, 20 + val * 60);
                        int b = (int) Math.min(255, 120 + val * 135);
                        pxColor = Color.rgb(r, g, b);
                    } else {
                        pxColor = Color.rgb(30, 41, 59);
                    }
                } else if (mode == 7) { // IRON & COPPER (Layer 5 / 4.6)
                    var point = generator.getPlanetPoint(lat, lon, planet);
                    if (point.elevation() < planet.waterLevel()) {
                        pxColor = Color.rgb(15, 23, 42);
                    } else {
                        double val;
                        boolean useImport = geologyImportRadios.get(5) != null && geologyImportRadios.get(5).isSelected();
                        Image gImg = customGeologyLayerImages.get(5);
                        if (useImport && gImg != null && gImg.getPixelReader() != null) {
                            int gx = (int) Math.min((x_base / (double) w) * gImg.getWidth(), gImg.getWidth() - 1);
                            int gy = (int) Math.min((y_base / (double) h) * gImg.getHeight(), gImg.getHeight() - 1);
                            Color c = gImg.getPixelReader().getColor(gx, gy);
                            val = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                        } else {
                            val = sampleProceduralGeologyTensor(5, lon, lat, planet);
                        }
                        if (val > 0.05) {
                            if (val < 0.35) {
                                pxColor = Color.rgb(139, 69, 19); // Low Grade (Rust brown)
                            } else if (val < 0.70) {
                                pxColor = Color.rgb(217, 119, 6);  // Banded Iron Formation (Amber)
                            } else {
                                pxColor = Color.rgb(249, 115, 22); // Massive Iron & Copper (Flame orange)
                            }
                        } else {
                            pxColor = Color.rgb(30, 41, 59);
                        }
                    }
                } else if (mode == 8) { // PRECIOUS METALS & REE (Layer 6 / 4.7)
                    var point = generator.getPlanetPoint(lat, lon, planet);
                    if (point.elevation() < planet.waterLevel()) {
                        pxColor = Color.rgb(15, 23, 42);
                    } else {
                        double val;
                        boolean useImport = geologyImportRadios.get(6) != null && geologyImportRadios.get(6).isSelected();
                        Image gImg = customGeologyLayerImages.get(6);
                        if (useImport && gImg != null && gImg.getPixelReader() != null) {
                            int gx = (int) Math.min((x_base / (double) w) * gImg.getWidth(), gImg.getWidth() - 1);
                            int gy = (int) Math.min((y_base / (double) h) * gImg.getHeight(), gImg.getHeight() - 1);
                            Color c = gImg.getPixelReader().getColor(gx, gy);
                            val = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                        } else {
                            val = sampleProceduralGeologyTensor(6, lon, lat, planet);
                        }
                        if (val > 0.05) {
                            int r = (int) Math.min(255, 150 + val * 105);
                            int g = (int) Math.min(255, 110 + val * 100);
                            int b = (int) Math.min(255, 10 + val * 20);
                            pxColor = Color.rgb(r, g, b);
                        } else {
                            pxColor = Color.rgb(30, 41, 59);
                        }
                    }
                } else if (mode == 9) { // MANTLE HEAT FLUX & TECTONICS (Layer 7 / 4.8)
                    double val;
                    boolean useImport = geologyImportRadios.get(7) != null && geologyImportRadios.get(7).isSelected();
                    Image gImg = customGeologyLayerImages.get(7);
                    if (useImport && gImg != null && gImg.getPixelReader() != null) {
                        int gx = (int) Math.min((x_base / (double) w) * gImg.getWidth(), gImg.getWidth() - 1);
                        int gy = (int) Math.min((y_base / (double) h) * gImg.getHeight(), gImg.getHeight() - 1);
                        Color c = gImg.getPixelReader().getColor(gx, gy);
                        val = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                    } else {
                        val = sampleProceduralGeologyTensor(7, lon, lat, planet);
                    }
                    int r = (int) Math.min(255, 30 + val * 220);
                    int g = (int) Math.min(255, 20 + val * 90);
                    int b = (int) Math.min(255, 40 + (1.0 - val) * 100);
                    pxColor = Color.rgb(r, g, b);
                } else if (mode == 10) { // AQUIFERS & FRESHWATER (Layer 8 / 4.9)
                    var point = generator.getPlanetPoint(lat, lon, planet);
                    if (point.elevation() < planet.waterLevel()) {
                        pxColor = Color.rgb(15, 23, 42);
                    } else {
                        double val;
                        boolean useImport = geologyImportRadios.get(8) != null && geologyImportRadios.get(8).isSelected();
                        Image gImg = customGeologyLayerImages.get(8);
                        if (useImport && gImg != null && gImg.getPixelReader() != null) {
                            int gx = (int) Math.min((x_base / (double) w) * gImg.getWidth(), gImg.getWidth() - 1);
                            int gy = (int) Math.min((y_base / (double) h) * gImg.getHeight(), gImg.getHeight() - 1);
                            Color c = gImg.getPixelReader().getColor(gx, gy);
                            val = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                        } else {
                            val = sampleProceduralGeologyTensor(8, lon, lat, planet);
                        }
                        if (val > 0.05) {
                            int r = (int) Math.min(255, 10 + val * 30);
                            int g = (int) Math.min(255, 100 + val * 120);
                            int b = (int) Math.min(255, 160 + val * 95);
                            pxColor = Color.rgb(r, g, b);
                        } else {
                            pxColor = Color.rgb(30, 41, 59);
                        }
                    }
                } else if (mode == 11) { // SURFACE TEMPERATURE & MICROCLIMATES
                    var point = generator.getPlanetPoint(lat, lon, planet);
                    double tempC = point.temperature();
                    double tNorm = Math.clamp((tempC + 25.0) / 70.0, 0.0, 1.0);
                    int r, g, b;
                    if (tNorm < 0.25) {
                        double n = tNorm / 0.25;
                        r = (int) (15 + n * 40); g = (int) (100 + n * 80); b = (int) (220 + n * 35);
                    } else if (tNorm < 0.5) {
                        double n = (tNorm - 0.25) / 0.25;
                        r = (int) (55 + n * 50); g = (int) (180 + n * 30); b = (int) (255 - n * 180);
                    } else if (tNorm < 0.75) {
                        double n = (tNorm - 0.5) / 0.25;
                        r = (int) (105 + n * 140); g = (int) (210 - n * 60); b = (int) (75 - n * 60);
                    } else {
                        double n = (tNorm - 0.75) / 0.25;
                        r = (int) (245 + n * 10); g = (int) (150 - n * 130); b = (int) (15 + n * 20);
                    }
                    pxColor = Color.rgb(r, g, b);
                } else if (mode == 12) { // SEISMIC & VOLCANIC TECTONISM
                    var point = generator.getPlanetPoint(lat, lon, planet);
                    double heatVal = sampleProceduralGeologyTensor(7, lon, lat, planet);
                    double decl = point.declivity();
                    double sLevel = planet.seismicActivityLevel() / 10.0;
                    double vLevel = planet.volcanicActivityLevel() / 8.0;

                    double risk = Math.clamp(heatVal * 0.5 + decl * 2.0 * sLevel + vLevel * 0.3, 0.0, 1.0);
                    if (point.elevation() < planet.waterLevel()) {
                        int r = (int) (15 + risk * 230);
                        int g = (int) (23 + risk * 40);
                        int b = (int) (42 + (1.0 - risk) * 50);
                        pxColor = Color.rgb(r, g, b);
                    } else {
                        int r = (int) (40 + risk * 215);
                        int g = (int) (45 + risk * 90);
                        int b = (int) (55 + (1.0 - risk) * 20);
                        pxColor = Color.rgb(r, g, b);
                    }
                } else { // Mode 13: ARIDITY & SOIL SALINIZATION
                    var point = generator.getPlanetPoint(lat, lon, planet);
                    if (point.elevation() < planet.waterLevel()) {
                        pxColor = Color.rgb(15, 23, 42);
                    } else {
                        double rainfall = point.rainfall();
                        double tempC = point.temperature();
                        double aridityIndex = rainfall / Math.max(1.0, tempC + 10.0);
                        double dryness = Math.clamp(1.0 - (aridityIndex / 50.0), 0.0, 1.0);

                        int r = (int) (30 + dryness * 200);
                        int g = (int) (140 - dryness * 80);
                        int b = (int) (160 - dryness * 140);
                        pxColor = Color.rgb(r, g, b);
                    }
                }

                if (btnReliefOverlay != null && btnReliefOverlay.isSelected()) {
                    var pt = generator.getPlanetPoint(lat, lon, planet);
                    double elev = pt.elevation();
                    double wLevel = planet.waterLevel();
                    boolean isLandHere = elev >= wLevel;

                    var ptE = generator.getPlanetPoint(lat, lon + 0.5, planet);
                    var ptN = generator.getPlanetPoint(lat + 0.5, lon, planet);
                    boolean isLandEast = ptE.elevation() >= wLevel;
                    boolean isLandNorth = ptN.elevation() >= wLevel;
                    boolean isCoast = (isLandHere != isLandEast) || (isLandHere != isLandNorth);

                    if (isCoast) {
                        pxColor = Color.rgb(224, 242, 254); // Crisp white-cyan coastline outline
                    } else {
                        Color reliefCol = getReliefShadeColor(elev, wLevel, pt.declivity());
                        pxColor = blendColors(pxColor, reliefCol, 0.50); // 50% opacity relief overlay
                    }
                }

                pw.setColor(px, py, pxColor);
            }
        }

        gc.setImageSmoothing(true);
        gc.drawImage(buffer, 0, 0, canvasW, canvasH);
    }

    private double sampleProceduralGeologyTensor(int layerIdx, double lon, double lat, PlanetPreset planet) {
        double p1 = geologyAbundanceSliders.containsKey(layerIdx) ? geologyAbundanceSliders.get(layerIdx).getValue() : GEOLOGY_SLIDER_SPECS[layerIdx][0].defaultValue;
        double p2 = geologyThresholdSliders.containsKey(layerIdx) ? geologyThresholdSliders.get(layerIdx).getValue() : GEOLOGY_SLIDER_SPECS[layerIdx][1].defaultValue;
        double p3 = geologyParam3Sliders.containsKey(layerIdx) ? geologyParam3Sliders.get(layerIdx).getValue() : GEOLOGY_SLIDER_SPECS[layerIdx][2].defaultValue;
        double p4 = geologyParam4Sliders.containsKey(layerIdx) ? geologyParam4Sliders.get(layerIdx).getValue() : GEOLOGY_SLIDER_SPECS[layerIdx][3].defaultValue;

        double[][] baseHotspots = switch (layerIdx) {
            case 0 -> COAL_SPOTS;
            case 1 -> OIL_SPOTS;
            case 2 -> GAS_SPOTS;
            case 3 -> URANIUM_SPOTS;
            case 4 -> HE3_SPOTS;
            case 5 -> IRON_COPPER_SPOTS;
            case 6 -> PRECIOUS_REE_SPOTS;
            case 7 -> MANTLE_HEAT_SPOTS;
            default -> AQUIFER_SPOTS;
        };

        double baseVal = sampleHotspotVal(lon, lat, baseHotspots);

        long seed = 12345L + (layerIdx + 1) * 9876543L;
        if (geologyTensorSeeds.containsKey(layerIdx) && geologyTensorSeeds.get(layerIdx) != null) {
            try {
                seed = Long.parseLong(geologyTensorSeeds.get(layerIdx).getText().trim());
            } catch (NumberFormatException ignored) {}
        }

        double latR = Math.toRadians(lat);
        double lonR = Math.toRadians(lon);
        double x = Math.cos(latR) * Math.cos(lonR);
        double y = Math.cos(latR) * Math.sin(lonR);
        double z = Math.sin(latR);

        SimplexNoise noise1 = getOrCreateNoise(seed);
        SimplexNoise noise2 = getOrCreateNoise(seed + 99999L);
        double scale1 = 2.5 + (layerIdx % 5) * 1.2;
        double scale2 = 5.5 + (layerIdx % 4) * 1.8;
        double n1 = (noise1.noise(x * scale1, y * scale1, z * scale1) + 1.0) * 0.5;
        double n2 = (noise2.noise(x * scale2, y * scale2, z * scale2) + 1.0) * 0.5;

        double combined;

        switch (layerIdx) {
            case 0 -> { // COAL
                double biomassMask = (n1 > (1.0 - p2)) ? 1.0 : 0.0;
                double rankMult = 0.5 + p3 * 0.5;
                double depthMult = Math.clamp(p4 / 5.0, 0.2, 2.0);
                combined = (baseVal * 0.5 + n1 * 0.3 + n2 * 0.2) * biomassMask * rankMult * depthMult * p1;
            }
            case 1 -> { // CRUDE_OIL
                double tocMask = (n1 > (1.0 - p2 * 0.8)) ? 1.0 : 0.2;
                double trapBonus = 0.5 + p3 * 0.8 * n2;
                double tempFactor = 1.0 - Math.abs(p4 - 120.0) / 100.0;
                tempFactor = Math.clamp(tempFactor, 0.1, 1.0);
                combined = (baseVal * 0.5 + n1 * 0.5) * tocMask * trapBonus * tempFactor * p1;
            }
            case 2 -> { // NATURAL_GAS
                double shaleBoost = 0.4 + p2 * 0.6;
                double depthPres = Math.clamp(p3 / 4.0, 0.3, 1.8);
                double sealMask = 0.3 + p4 * 0.7;
                combined = (baseVal * 0.4 + n1 * 0.4 + n2 * 0.2) * shaleBoost * depthPres * sealMask * p1;
            }
            case 3 -> { // URANIUM
                double ppmNorm = p1 / 250.0;
                double hydroBoost = 0.5 + p2 * 1.0 * n2;
                double thUFactor = 1.0 + (p3 - 3.5) * 0.1;
                double unconformityMask = 0.6 + p4 * 0.8 * (n1 > 0.6 ? 1.5 : 0.5);
                combined = (baseVal * 0.4 + n1 * 0.6) * ppmNorm * hydroBoost * thUFactor * unconformityMask;
            }
            case 4 -> { // HELIUM_3
                double ppbNorm = p1 / 15.0;
                double ilmeniteBoost = 0.4 + p2 * 0.8;
                double solarIon = Math.clamp(p3, 0.0, 5.0);
                double churnLoss = Math.clamp(1.0 - (p4 - 3.0) * 0.05, 0.5, 1.2);
                double atmShield = planet.atmospherePressureAtm() > 0.1 ? 0.001 : 1.0;
                combined = (baseVal * 0.5 + n1 * 0.5) * ppbNorm * ilmeniteBoost * solarIon * churnLoss * atmShield;
            }
            case 5 -> { // IRON_COPPER
                double bifLayer = (n1 > 0.4) ? p3 * 0.8 : 0.2;
                double magmaticPorphyry = p2 * 1.2 * n2 * (p4 / 0.8);
                combined = (baseVal * 0.4 + bifLayer * 0.3 + magmaticPorphyry * 0.3) * p1;
            }
            case 6 -> { // PRECIOUS_REE
                double salarLi = p2 * 1.2 * (n1 > 0.5 ? 1.0 : 0.1);
                double carbonatite = p3 * 1.5 * (n2 > 0.7 ? 1.5 : 0.2);
                double spodumene = (p4 / 1.5) * 0.5;
                combined = (baseVal * 0.4 + salarLi * 0.2 + carbonatite * 0.2 + spodumene * 0.2) * p1;
            }
            case 7 -> { // MANTLE_HEAT
                double heatNorm = p1 / 65.0;
                double plumeAnomalies = 1.0 + p2 * 1.5 * n1;
                double cratonInsulation = Math.clamp(1.0 - (p3 - 150.0) / 300.0, 0.3, 1.5);
                double radiogenicCrust = 0.8 + (p4 / 1.2) * 0.4 * n2;
                combined = (baseVal * 0.4 + n1 * 0.6) * heatNorm * plumeAnomalies * cratonInsulation * radiogenicCrust;
            }
            default -> { // FRESHWATER_AQUIFERS (8)
                double capNorm = p1 / 1.0;
                double hydraulicCond = 0.5 + p2 * 0.8;
                double porosityFactor = p3 / 18.0;
                double permafrostCap = Math.clamp(1.0 - (p4 / 2000.0) * 0.5, 0.3, 1.0);
                combined = (baseVal * 0.5 + n1 * 0.5) * capNorm * hydraulicCond * porosityFactor * permafrostCap;
            }
        }

        return Math.clamp(combined, 0.0, 1.0);
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
        if (summaryLabel == null) return;

        summaryLabel.setText(String.format(
                I18n.getOrDefault("resource.summary.format",
                        "🌲 Végétale : %.0f GtC  |  🌾 Sols : %.0f GtC  |  🦌 Faune : %.2f GtC\n" +
                        "⛏️ Métaux : %.0f Gt  |  💎 Précieux : %.0f Mt  |  🌋 Manteau : %.1f mW/m²  |  💧 Aquifères : %.0f x10³ km³"),
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
        boolean oldUpdating = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            if (headerLabel != null) headerLabel.setText(I18n.getOrDefault("resource.title", "RESOURCE DISTRIBUTION & ECOLOGY (SCIENTIFIC METRICS)"));
            if (headerLabel != null) headerLabel.setText(I18n.getOrDefault("resource.title", "RESOURCE DISTRIBUTION & ECOLOGY (SCIENTIFIC METRICS)"));
            if (planetSectionHeader != null) planetSectionHeader.setText(I18n.getOrDefault("resource.section.planet_preset", "PLANETARY PARAMETERS"));
            if (biomeDomainSecHeader != null) biomeDomainSecHeader.setText(I18n.getOrDefault("resource.domain.biome", "BIOME & FLORA DOMAIN (VEGETATION)"));
            if (hydroDomainSecHeader != null) hydroDomainSecHeader.setText(I18n.getOrDefault("resource.domain.hydro", "HYDROGRAPHY & FRESHWATER DOMAIN"));
            if (climateSummaryLabel != null) climateSummaryLabel.setText(I18n.getOrDefault("resource.climate.summary", "🌡️ Climate Model: Inherited from Tab 1 planet parameters (Temperature, Gradient, Pressure, O₂, CO₂, Albedo)"));
            if (climateDomainSecHeader != null) climateDomainSecHeader.setText(I18n.getOrDefault("resource.domain.climate", "CLIMATE & ATMOSPHERE DOMAIN"));
            if (geologyDomainSecHeader != null) geologyDomainSecHeader.setText(I18n.getOrDefault("resource.domain.geology", "GEOLOGY, TECTONICS & ORES DOMAIN"));

            if (floraSecHeader != null) floraSecHeader.setText(I18n.getOrDefault("resource.section.flora", "PLANT ECOLOGY & SOILS (METRICS)"));
            if (faunaSecHeader != null) faunaSecHeader.setText(I18n.getOrDefault("resource.section.fauna", "ANIMAL & AQUATIC BIOMASS (METRICS)"));
            if (mineralSecHeader != null) mineralSecHeader.setText(I18n.getOrDefault("resource.section.minerals", "GEOLOGY & HYDROLOGY (METRICS)"));

            if (headerLabel != null) headerLabel.setText(I18n.getOrDefault("resource.title", "RESOURCE DISTRIBUTION & ECOLOGY (SCIENTIFIC METRICS)"));
            if (rightViewTitle != null) rightViewTitle.setText(I18n.getOrDefault("resource.title.right_view", "Dynamic 2D Equirectangular Planetary Preview"));

            if (syncPlanetBtn != null) syncPlanetBtn.setText(I18n.getOrDefault("resource.btn.sync_planet", "🔄 Synchronize with Tab 1 Planet"));
            if (autoDeriveEcologyBtn != null) autoDeriveEcologyBtn.setText(I18n.getOrDefault("resource.btn.auto_derive_eco", "⚡ Auto-derive Ecology & Biomes from Physics"));
            if (autoDeriveHydroBtn != null) autoDeriveHydroBtn.setText(I18n.getOrDefault("resource.btn.auto_derive_hydro", "💧 Auto-derive Aquifers & Freshwater from Physics"));
            if (autoDeriveGeologyBtn != null) autoDeriveGeologyBtn.setText(I18n.getOrDefault("resource.btn.auto_derive_geo", "🌋 Calculate Tectonics & Metals from Mantle"));
            if (mapsSecHeader != null) mapsSecHeader.setText(I18n.getOrDefault("resource.section.imports", "TAB 1 ECOLOGY, GEOLOGY & HYDROLOGY IMPORTS"));

            if (mapSourceRowLabel != null) mapSourceRowLabel.setText(I18n.getOrDefault("resource.param.map_source", "Celestial Body Model / Satellite:"));
            if (biomeMapRowLabel != null) biomeMapRowLabel.setText(I18n.getOrDefault("resource.param.biome_map", "Biome / Extraterrestrial Vegetation Map (PNG):"));
            if (resourceMapRowLabel != null) resourceMapRowLabel.setText(I18n.getOrDefault("resource.param.resource_map", "Geological & Multi-Channel Ore Map (PNG):"));
            if (hydroMapRowLabel != null) hydroMapRowLabel.setText(I18n.getOrDefault("resource.param.hydro_map", "Hydrographic & Rivers Map (PNG Watercourses):"));
            if (climateMapRowLabel != null) climateMapRowLabel.setText(I18n.getOrDefault("planet.map.climate", "Climate Map (Thermal / RGB):"));
            if (rainfallMapRowLabel != null) rainfallMapRowLabel.setText(I18n.getOrDefault("planet.map.rainfall", "Precipitation Map (Humidity):"));
            if (seasonalityMapRowLabel != null) seasonalityMapRowLabel.setText(I18n.getOrDefault("planet.map.seasonality", "Seasonality / Variance Map:"));

            if (fetchOnlineHydroBtn != null) fetchOnlineHydroBtn.setText(I18n.getOrDefault("resource.btn.fetch_online_hydro", "🌐 Download Satellite Hydrography (Earth WMS / Earth Preset)"));
            if (proceduralHydroBtn != null) proceduralHydroBtn.setText(I18n.getOrDefault("resource.btn.procedural_hydro", "⚡ Auto-generate Rivers & Watercourses (Procedural Slope)"));
            if (fetchOnlineBtn != null) fetchOnlineBtn.setText(I18n.getOrDefault("resource.btn.fetch_online", "🌐 Download WMS Satellite Maps (USGS / NASA)"));
            if (fetchOnlineClimateBtn != null) fetchOnlineClimateBtn.setText(I18n.getOrDefault("planet.map.btn_fetch_online_climate", "🌐 Download Satellite Climate NASA/USGS (WMS)"));
            if (climateHelpBtn != null) climateHelpBtn.setText(I18n.getOrDefault("planet.map.btn_climate_help", "ℹ️ Climate Map Specifications"));
            if (exportMapsBtn != null) exportMapsBtn.setText(I18n.getOrDefault("resource.btn.export_maps", "📤 Export Maps (PNG + WorldFile .tfw)"));
            if (specsHelpBtn != null) specsHelpBtn.setText(I18n.getOrDefault("resource.btn.specs_help", "ℹ️ Biome & Geology Map Specifications"));

            if (terrestrialBiomassRowLabel != null) terrestrialBiomassRowLabel.setText(I18n.getOrDefault("resource.param.terrestrial_biomass", "Terrestrial Plant Biomass (GtC):"));
            if (soilCarbonRowLabel != null) soilCarbonRowLabel.setText(I18n.getOrDefault("resource.param.soil_carbon", "Soil Organic Carbon (GtC):"));
            if (faunaBiomassRowLabel != null) faunaBiomassRowLabel.setText(I18n.getOrDefault("resource.param.fauna_biomass", "Terrestrial Animal Biomass (GtC):"));
            if (aquaticBiomassRowLabel != null) aquaticBiomassRowLabel.setText(I18n.getOrDefault("resource.param.aquatic_biomass", "Aquatic Biomass & Fauna (GtC):"));
            if (crustalMetalRowLabel != null) crustalMetalRowLabel.setText(I18n.getOrDefault("resource.param.crustal_metal", "Crustal Metal Wealth (Gt):"));
            if (preciousMetalRowLabel != null) preciousMetalRowLabel.setText(I18n.getOrDefault("resource.param.precious_metal", "Precious Metal Concentration (Mt):"));
            if (mantleHeatRowLabel != null) mantleHeatRowLabel.setText(I18n.getOrDefault("resource.param.mantle_heat", "Mantle Heat Flux (mW/m²):"));
            if (freshwaterAquiferRowLabel != null) freshwaterAquiferRowLabel.setText(I18n.getOrDefault("resource.param.freshwater_aquifer", "Groundwater Aquifer Capacity (10³ km³):"));
            if (seismicRowLabel != null) seismicRowLabel.setText(I18n.getOrDefault("resource.param.seismic", "Seismic & Tectonic Activity (Mag Richter):"));
            if (volcanicRowLabel != null) volcanicRowLabel.setText(I18n.getOrDefault("resource.param.volcanic", "Global Volcanic Activity (VEI):"));

            if (radioProcBiome != null) radioProcBiome.setText(I18n.getOrDefault("resource.mode.procedural_biome", "▶ Procedural Biomes"));
            if (radioImportBiome != null) radioImportBiome.setText(I18n.getOrDefault("resource.mode.import_biome", "📂 Biome Map (PNG)"));
            if (radioProcHydro != null) radioProcHydro.setText(I18n.getOrDefault("resource.mode.procedural_hydro", "▶ Procedural Hydrography"));
            if (radioImportHydro != null) radioImportHydro.setText(I18n.getOrDefault("resource.mode.import_hydro", "📂 Hydrographic Map (PNG)"));
            if (radioProcClimate != null) radioProcClimate.setText(I18n.getOrDefault("resource.mode.procedural_climate", "▶ Procedural Climate"));
            if (radioImportClimate != null) radioImportClimate.setText(I18n.getOrDefault("resource.mode.import_climate", "📂 Climate Maps (PNG)"));
            if (radioProcGeology != null) radioProcGeology.setText(I18n.getOrDefault("resource.mode.procedural_geology", "▶ Procedural Geology"));
            if (radioImportGeology != null) radioImportGeology.setText(I18n.getOrDefault("resource.mode.import_geology", "📂 Geological Map (PNG)"));

            if (btnReliefOverlay != null) {
                btnReliefOverlay.setText(I18n.getOrDefault("resource.btn.relief_overlay", "⛰️ Relief"));
            }

            if (viewModeCombo != null) {
                int selected = viewModeCombo.getSelectionModel().getSelectedIndex();
                viewModeCombo.getItems().setAll(
                    I18n.getOrDefault("resource.view.section_base", "────────── 11 CARTES CANONIQUES DE BASE ──────────"),
                    I18n.getOrDefault("resource.view.biomes", "🌿 1. Biomes & Couverture Végétale"),
                    I18n.getOrDefault("resource.view.hydro", "🌊 2. Hydrographie & Réseau Fluvial"),
                    I18n.getOrDefault("resource.view.coal", "⛏️ 3. Gisements de Charbon"),
                    I18n.getOrDefault("resource.view.oil", "🛢️ 4. Réserves de Pétrole Brut"),
                    I18n.getOrDefault("resource.view.gas", "🔥 5. Champs de Gaz Naturel"),
                    I18n.getOrDefault("resource.view.uranium", "⚛️ 6. Minerais d'Uranium & Fission"),
                    I18n.getOrDefault("resource.view.helium3", "🌌 7. Hélium-3 & Fusion Lunaires"),
                    I18n.getOrDefault("resource.view.iron_copper", "⛓️ 8. Métaux Fer BIF & Cuivre"),
                    I18n.getOrDefault("resource.view.precious", "💎 9. Terres Rares & Métaux Précieux"),
                    I18n.getOrDefault("resource.view.heat", "🌋 10. Flux Thermique du Manteau"),
                    I18n.getOrDefault("resource.view.aquifer", "💧 11. Aquifères & Eau Douce"),
                    I18n.getOrDefault("resource.view.section_derived", "────────── CARTES DÉDUITES / ANOMALIES ──────────"),
                    I18n.getOrDefault("resource.view.temp", "🌡️ 12. Températures Surface & Microclimats"),
                    I18n.getOrDefault("resource.view.seismic", "🌋 13. Tectonique & Aléa Sismique/Volcanique"),
                    I18n.getOrDefault("resource.view.aridity", "🏜️ 14. Aridité & Salinisation des Sols")
                );
                if (selected >= 0 && selected < viewModeCombo.getItems().size()) {
                    viewModeCombo.getSelectionModel().select(selected);
                } else {
                    viewModeCombo.getSelectionModel().select(1);
                }
            }

            if (mapSourceCombo != null && mapSourceCombo.getCellFactory() != null) mapSourceCombo.setButtonCell(mapSourceCombo.getCellFactory().call(null));
            if (biomeSourceCombo != null && biomeSourceCombo.getCellFactory() != null) biomeSourceCombo.setButtonCell(biomeSourceCombo.getCellFactory().call(null));
            if (hydroSourceCombo != null && hydroSourceCombo.getCellFactory() != null) hydroSourceCombo.setButtonCell(hydroSourceCombo.getCellFactory().call(null));
            if (climateSourceCombo != null && climateSourceCombo.getCellFactory() != null) climateSourceCombo.setButtonCell(climateSourceCombo.getCellFactory().call(null));
            if (geologySourceCombo != null && geologySourceCombo.getCellFactory() != null) geologySourceCombo.setButtonCell(geologySourceCombo.getCellFactory().call(null));

            if (loadBiomeBtn != null) loadBiomeBtn.setText(I18n.get("planet.map.btn_load"));
            if (loadResourceBtn != null) loadResourceBtn.setText(I18n.get("planet.map.btn_load"));
            if (loadHydroBtn != null) loadHydroBtn.setText(I18n.get("planet.map.btn_load"));
            if (loadClimateBtn != null) loadClimateBtn.setText(I18n.get("planet.map.btn_load"));
            if (loadRainfallBtn != null) loadRainfallBtn.setText(I18n.get("planet.map.btn_load"));
            if (loadSeasonalityBtn != null) loadSeasonalityBtn.setText(I18n.get("planet.map.btn_load"));

            if (customBiomeImage == null && biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
            if (customResourceImage == null && resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
            if (customHydroImage == null && hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
            if (customClimateImage == null && climateFileLabel != null) climateFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
            if (customRainfallImage == null && rainfallFileLabel != null) rainfallFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
            if (customSeasonalityImage == null && seasonalityFileLabel != null) seasonalityFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));

            if (biomeFormatHintLabel != null) biomeFormatHintLabel.setText(I18n.getOrDefault("resource.format.biome_hint", "PNG / JPEG (2:1 equirectangular projection):\n  Biome image or vegetation cover (grayscale or RGB)."));
            if (hydroFormatHintLabel != null) hydroFormatHintLabel.setText(I18n.getOrDefault("resource.format.hydro_hint", "PNG / JPEG (2:1 equirectangular projection):\n  Black (0) = no water | White (255) = river network / max flow."));
            if (geologyFormatHintLabel != null) geologyFormatHintLabel.setText(I18n.getOrDefault("resource.format.geology_hint", "Multi-channel PNG (2:1 equirectangular projection):\n • R Channel (Red) = Industrial crustal metals (Iron/Copper)\n • G Channel (Green) = Precious ores & REE (Gold/Pt)\n • B Channel (Blue) = Mantle heat flux & aquifers"));
            if (climateSummaryLabel != null) climateSummaryLabel.setText(I18n.getOrDefault("resource.climate.summary", "🌡️ Climate Model: Inherited from Tab 1 planet parameters (Temperature, Gradient, Pressure, O₂, CO₂, Albedo)"));

            if (applyBtn != null) applyBtn.setText(I18n.getOrDefault("resource.btn.apply", "✅ Apply scientific distribution to simulation"));

            // Dynamic refresh of the 9 geology tensor cards
            for (int i = 0; i < 9; i++) {
                if (geologySubTitles.containsKey(i) && geologySubTitles.get(i) != null) {
                    geologySubTitles.get(i).setText(getGeologyTensorTitle(i));
                    Tooltip.install(geologySubTitles.get(i), new Tooltip(getGeologyTensorTooltip(i)));
                }
                if (geologyProcRadios.containsKey(i) && geologyProcRadios.get(i) != null) {
                    geologyProcRadios.get(i).setText(I18n.getOrDefault("resource.mode.procedural", "▶ Procedural Generation (Hotspots & Physics)"));
                }
                if (geologyImportRadios.containsKey(i) && geologyImportRadios.get(i) != null) {
                    geologyImportRadios.get(i).setText(I18n.getOrDefault("resource.mode.import_file", "📂 External Source (PNG / GeoTIFF)"));
                }
                if (geologySourceLabels.containsKey(i) && geologySourceLabels.get(i) != null) {
                    geologySourceLabels.get(i).setText(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
                }
                if (geologySeedLabels.containsKey(i) && geologySeedLabels.get(i) != null) {
                    geologySeedLabels.get(i).setText(I18n.getOrDefault("resource.label.tensor_seed", "Generation Seed:"));
                }
                if (geologyGenBtns.containsKey(i) && geologyGenBtns.get(i) != null) {
                    geologyGenBtns.get(i).setText(I18n.getOrDefault("resource.btn.gen_single_tensor", "🪄 Generate"));
                }
                if (geologyLoadBtns.containsKey(i) && geologyLoadBtns.get(i) != null) {
                    geologyLoadBtns.get(i).setText(I18n.getOrDefault("resource.btn.load_map", "Load Map"));
                }
                if (geologyFormatLabels.containsKey(i) && geologyFormatLabels.get(i) != null) {
                    geologyFormatLabels.get(i).setText(getGeologyFormatHint(i));
                }
                if (geologyFileLabels.containsKey(i) && geologyFileLabels.get(i) != null && customGeologyLayerImages.containsKey(i) && customGeologyLayerImages.get(i) != null) {
                    String cur = geologyFileLabels.get(i).getText();
                    if (cur != null && (cur.contains("Baseline") || cur.contains("Référence") || cur.contains("Earth") || cur.contains("Planetary") || cur.contains("📷"))) {
                        geologyFileLabels.get(i).setText(getGeologyBaselineName(i));
                    }
                }
                if (geologySourceCombos.containsKey(i) && geologySourceCombos.get(i) != null && geologySourceCombos.get(i).getCellFactory() != null) {
                    geologySourceCombos.get(i).setButtonCell(geologySourceCombos.get(i).getCellFactory().call(null));
                }
                if (geologySliderTitleLabels.containsKey(i) && geologySliderTitleLabels.get(i) != null) {
                    List<Label> titles = geologySliderTitleLabels.get(i);
                    for (int s = 0; s < Math.min(titles.size(), 4); s++) {
                        TensorSliderMeta meta = GEOLOGY_SLIDER_SPECS[i][s];
                        titles.get(s).setText(I18n.getOrDefault(meta.labelKey, meta.defaultLabel));
                        Tooltip.install(titles.get(s), new Tooltip(I18n.getOrDefault(meta.tooltipKey, meta.defaultTooltip)));
                    }
                }
            }

            updateSummary();
            updatePlanetContextDisplay();
            updateLegend();
        } finally {
            isUpdatingFromPreset = oldUpdating;
        }
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

    private PlanetPreset findPlanetPresetByName(String name) {
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

    public PlanetPreset getActivePlanetPreset() {
        return activePlanetPreset;
    }

    public boolean validateResourceSetup() {
        return validateResourceSetup(true);
    }

    private boolean isCustomFileRequired(ComboBox<String> domainCombo, ComboBox<String> masterCombo) {
        String val = null;
        if (domainCombo != null && domainCombo.getValue() != null && !"none".equalsIgnoreCase(domainCombo.getValue())) {
            val = domainCombo.getValue().trim().toLowerCase();
        } else if (masterCombo != null && masterCombo.getValue() != null && !"none".equalsIgnoreCase(masterCombo.getValue())) {
            val = masterCombo.getValue().trim().toLowerCase();
        }
        if (val == null) return false;
        return "custom".equals(val) || "file".equals(val);
    }

    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();
        boolean isBiomeImport = (radioImportBiome != null && radioImportBiome.isSelected()) || (radioImportEco != null && radioImportEco.isSelected());
        if (isBiomeImport) {
            if (customBiomeImage == null) {
                errors.add(I18n.getOrDefault("resource.validation.missing_biome_map", "Missing biome distribution map in import mode (Tab 2)."));
            } else {
                org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(customBiomeImage);
                if (!val.valid()) {
                    errors.add(I18n.getOrDefault("resource.validation.invalid_biome_map", "Biome map incompatible (Tab 2): ") + val.message());
                }
            }
        }
        if (radioImportHydro != null && radioImportHydro.isSelected()) {
            if (customHydroImage == null) {
                errors.add(I18n.getOrDefault("resource.validation.missing_hydro_map", "Missing hydrographic map in import mode (Tab 2)."));
            } else {
                org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(customHydroImage);
                if (!val.valid()) {
                    errors.add(I18n.getOrDefault("resource.validation.invalid_hydro_map", "Hydrographic map incompatible (Tab 2): ") + val.message());
                }
            }
        }
        if (radioImportGeology != null && radioImportGeology.isSelected()) {
            if (customResourceImage == null && (customGeologyLayerImages == null || customGeologyLayerImages.isEmpty())) {
                errors.add(I18n.getOrDefault("resource.validation.missing_geology_map", "Missing geological & deposit map in import mode (Tab 2)."));
            } else if (customResourceImage != null) {
                org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(customResourceImage);
                if (!val.valid()) {
                    errors.add(I18n.getOrDefault("resource.validation.invalid_geology_map", "Geological map incompatible (Tab 2): ") + val.message());
                }
            }
        }
        if (radioImportClimate != null && radioImportClimate.isSelected()) {
            if (customClimateImage == null) {
                errors.add(I18n.getOrDefault("resource.validation.missing_climate_map", "Missing macro-climate map in import mode (Tab 2)."));
            } else {
                org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(customClimateImage);
                if (!val.valid()) {
                    errors.add(I18n.getOrDefault("resource.validation.invalid_climate_map", "Macro-climate map incompatible (Tab 2): ") + val.message());
                }
            }
        }
        return errors;
    }

    public boolean validateResourceSetup(boolean showDialog) {
        List<String> errors = getValidationErrors();
        boolean isValid = errors.isEmpty();

        boolean isBiomeImport = (radioImportBiome != null && radioImportBiome.isSelected()) || (radioImportEco != null && radioImportEco.isSelected());
        boolean biomeBad = isBiomeImport && (customBiomeImage == null || !org.ether.society.data.ImageMapLoader.validateMapImage(customBiomeImage).valid());
        if (loadBiomeBtn != null) loadBiomeBtn.setStyle(biomeBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        boolean hydroBad = radioImportHydro != null && radioImportHydro.isSelected() && (customHydroImage == null || !org.ether.society.data.ImageMapLoader.validateMapImage(customHydroImage).valid());
        if (loadHydroBtn != null) loadHydroBtn.setStyle(hydroBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        boolean geoBad = radioImportGeology != null && radioImportGeology.isSelected() && (customResourceImage == null || !org.ether.society.data.ImageMapLoader.validateMapImage(customResourceImage).valid()) && (customGeologyLayerImages == null || customGeologyLayerImages.isEmpty());
        if (loadResourceBtn != null) loadResourceBtn.setStyle(geoBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        boolean climateBad = radioImportClimate != null && radioImportClimate.isSelected() && (customClimateImage == null || !org.ether.society.data.ImageMapLoader.validateMapImage(customClimateImage).valid());
        if (loadClimateBtn != null) loadClimateBtn.setStyle(climateBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        if (!isValid && showDialog) {
            StringBuilder errorMsg = new StringBuilder();
            for (String err : errors) {
                errorMsg.append("• ").append(err).append("\n");
            }
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(I18n.getOrDefault("resource.validation.title", "Ecological Validation (Tab 2)"));
            alert.setHeaderText(I18n.getOrDefault("resource.validation.header", "⚠️ Required ecological maps or parameters are invalid or missing:"));
            alert.setContentText(errorMsg.toString());
            alert.showAndWait();
        }

        return isValid;
    }

    private static Image bufferedImageToFXImage(java.awt.image.BufferedImage bImg) {
        if (bImg == null) return null;
        int w = bImg.getWidth();
        int h = bImg.getHeight();
        WritableImage fxImg = new WritableImage(w, h);
        PixelWriter pw = fxImg.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pw.setArgb(x, y, bImg.getRGB(x, y));
            }
        }
        return fxImg;
    }

    public void prepopulateEarthGeologyTensors() {
        try {
            java.nio.file.Path cacheDir = java.nio.file.Paths.get("data", "maps", "cache");
            if (!java.nio.file.Files.exists(cacheDir)) {
                java.nio.file.Files.createDirectories(cacheDir);
            }

            String[] cacheKeys = {
                "earth_coal.png",
                "earth_oil.png",
                "earth_gas.png",
                "earth_uranium.png",
                "earth_he3.png",
                "earth_ironcopper.png",
                "earth_preciousree.png",
                "earth_mantleheat.png",
                "earth_aquifer.png"
            };

            String[] resourceKeys = {
                "/maps/earth_coal.png",
                "/maps/earth_oil.png",
                "/maps/earth_gas.png",
                "/maps/earth_uranium.png",
                "/maps/earth_helium3.png",
                "/maps/earth_iron_copper.png",
                "/maps/earth_precious_metals.png",
                "/maps/earth_geothermal.png",
                "/maps/earth_aquifers.png"
            };

            for (int i = 0; i < cacheKeys.length; i++) {
                java.nio.file.Path fileCachePath = cacheDir.resolve(cacheKeys[i]);
                if (java.nio.file.Files.exists(fileCachePath)) {
                    try {
                        Image img = new Image(fileCachePath.toUri().toString());
                        if (img.getWidth() > 0) {
                            customGeologyLayerImages.put(i, img);
                        }
                    } catch (Exception ex) {
                        logger.warn("Could not load cached map {}", fileCachePath, ex);
                    }
                }

                if (!customGeologyLayerImages.containsKey(i) || customGeologyLayerImages.get(i) == null) {
                    try (var is = getClass().getResourceAsStream(resourceKeys[i])) {
                        if (is != null) {
                            Image img = new Image(is);
                            if (img.getWidth() > 0) {
                                customGeologyLayerImages.put(i, img);
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (!customGeologyLayerImages.containsKey(i) || customGeologyLayerImages.get(i) == null) {
                    BufferedImage bImg = switch (i) {
                        case 0 -> HistoricalMapGenerator.generateCleanCoalMap("EARTH", null);
                        case 1 -> HistoricalMapGenerator.generateCleanOilMap("EARTH", null);
                        case 2 -> HistoricalMapGenerator.generateCleanGasMap("EARTH", null);
                        case 3 -> HistoricalMapGenerator.generateCleanUraniumMap("EARTH", null);
                        case 4 -> HistoricalMapGenerator.generateCleanHelium3Map("EARTH", null);
                        case 5 -> HistoricalMapGenerator.generateCleanIronCopperMap("EARTH", null);
                        case 6 -> HistoricalMapGenerator.generateCleanPreciousMetalsMap("EARTH", null);
                        case 7 -> HistoricalMapGenerator.generateCleanMantleHeatMap("EARTH", null);
                        default -> HistoricalMapGenerator.generateCleanAquiferMap("EARTH", null);
                    };
                    if (bImg != null) {
                        try {
                            javax.imageio.ImageIO.write(bImg, "PNG", fileCachePath.toFile());
                        } catch (Exception ignored) {}
                        customGeologyLayerImages.put(i, bufferedImageToFXImage(bImg));
                    }
                }
            }

            for (int i = 0; i < 9; i++) {
                if (geologyFileLabels.containsKey(i)) {
                    geologyFileLabels.get(i).setText(getGeologyBaselineName(i));
                }
                if (geologyImportRadios.containsKey(i)) {
                    geologyImportRadios.get(i).setSelected(true);
                }
                if (geologySourceCombos.containsKey(i)) {
                    ComboBox<String> cb = geologySourceCombos.get(i);
                    if (cb != null && cb.getItems().size() > 1) {
                        cb.setValue(cb.getItems().get(1));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to pre-populate Earth geology tensors", e);
        }
    }

    private String getGeologyTensorTitle(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("resource.tensor.1.title", "⛏️ 4.1 Coal Deposits (COAL — USGS / BGR)");
            case 1 -> I18n.getOrDefault("resource.tensor.2.title", "🛢️ 4.2 Crude Oil & Fuel Reserves (CRUDE_OIL — WEP / BGR)");
            case 2 -> I18n.getOrDefault("resource.tensor.3.title", "🔥 4.3 Natural Gas Fields (NATURAL_GAS — WEP / BGR)");
            case 3 -> I18n.getOrDefault("resource.tensor.4.title", "⚛️ 4.4 Uranium & Fission Ores (URANIUM — IAEA UDEPO)");
            case 4 -> I18n.getOrDefault("resource.tensor.5.title", "🌌 4.5 Lunar Helium-3 & Fusion (HELIUM_3 — NASA / LPI)");
            case 5 -> I18n.getOrDefault("resource.tensor.6.title", "⛓️ 4.6 Industrial Metals BIF Iron & Copper (IRON_COPPER)");
            case 6 -> I18n.getOrDefault("resource.tensor.7.title", "💎 4.7 Rare Earths, Lithium Brines & Spodumene (PRECIOUS_REE / Li)");
            case 7 -> I18n.getOrDefault("resource.tensor.8.title", "🌋 4.8 Mantle Heat Flow & Geothermal (MANTLE_HEAT — IHFC / Davies 2013)");
            case 8 -> I18n.getOrDefault("resource.tensor.9.title", "💧 4.9 Deep Aquifers & Groundwater (FRESHWATER_AQUIFERS — WHYMAP)");
            default -> I18n.getOrDefault("resource.tensor.custom.title_prefix", "⛏️ 4.") + (index + 1) + I18n.getOrDefault("resource.tensor.custom.title_mid", " Tenseur Géologique ") + (index + 1);
        };
    }

    private String getGeologyTensorTooltip(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("resource.tensor.1.desc", "Coal basins and anthracite deposits. Source: USGS MRDS / BGR Germany.");
            case 1 -> I18n.getOrDefault("resource.tensor.2.desc", "Bituminous basins and offshore & continental oil fields. Source: World Energy Projection / BGR.");
            case 2 -> I18n.getOrDefault("resource.tensor.3.desc", "Conventional and non-conventional natural gas fields (shale gas). Source: WEP / BGR.");
            case 3 -> I18n.getOrDefault("resource.tensor.4.desc", "Pitchblende concentration and uranium/thorium deposits. Source: IAEA UDEPO / NFCIS.");
            case 4 -> I18n.getOrDefault("resource.tensor.5.desc", "Regolith enriched in Helium-3 (lunar basin & planetary deposits). Source: NASA PDS / LPI.");
            case 5 -> I18n.getOrDefault("resource.tensor.6.desc", "Banded iron formations (BIF) and porphyry copper deposits. Source: USGS Mineral Resources Program.");
            case 6 -> I18n.getOrDefault("resource.tensor.7.desc", "Gold, platinum, rare earth element (REE) deposits, and lithium salars. Source: USGS REE / Salars.");
            case 7 -> I18n.getOrDefault("resource.tensor.8.desc", "Geothermal mantle heat flux (mW/m²). Source: IHFC / Davies 2013 Global Heat Flow.");
            case 8 -> I18n.getOrDefault("resource.tensor.9.desc", "Deep groundwater tables and large fossil aquifers. Source: UNESCO WHYMAP.");
            default -> I18n.getOrDefault("resource.tensor.custom.desc", "Extensible geological layer.");
        };
    }

    private String getGeologyBaselineName(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("resource.tensor.1.source.baseline", "USGS MRDS / BGR Coal Deposits (Earth Baseline)");
            case 1 -> I18n.getOrDefault("resource.tensor.2.source.baseline", "World Energy Projection / BGR Oil Reserves (Earth Baseline)");
            case 2 -> I18n.getOrDefault("resource.tensor.3.source.baseline", "WEP / BGR Natural Gas Fields (Earth Baseline)");
            case 3 -> I18n.getOrDefault("resource.tensor.4.source.baseline", "IAEA UDEPO / NFCIS Uranium Database (Earth Baseline)");
            case 4 -> I18n.getOrDefault("resource.tensor.5.source.baseline", "NASA PDS / LPI Lunar Regolith (Planetary Baseline)");
            case 5 -> I18n.getOrDefault("resource.tensor.6.source.baseline", "USGS Mineral Resources BIF Iron & Copper (Earth Baseline)");
            case 6 -> I18n.getOrDefault("resource.tensor.7.source.baseline", "USGS REE & Lithium Salars (Earth Baseline)");
            case 7 -> I18n.getOrDefault("resource.tensor.8.source.baseline", "IHFC / Davies 2013 Global Heat Flow (Earth Baseline)");
            case 8 -> I18n.getOrDefault("resource.tensor.9.source.baseline", "UNESCO / WHYMAP Global Groundwater (Earth Baseline)");
            default -> I18n.getOrDefault("resource.status.earth_preset_loaded", "📷 Earth preset map loaded");
        };
    }

    private String getGeologyFormatHint(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("resource.tensor.1.format", "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 0 Gt | White (255) = 5.0 Gt (USGS MRDS / BGR Germany)");
            case 1 -> I18n.getOrDefault("resource.tensor.2.format", "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 0 Gt | White (255) = 5.0 Gt (World Energy Projection / BGR)");
            case 2 -> I18n.getOrDefault("resource.tensor.3.format", "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 0 10¹² m³ | White (255) = 5.0 10¹² m³ (WEP / BGR Germany)");
            case 3 -> I18n.getOrDefault("resource.tensor.4.format", "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 0 ppm | White (255) = 1,000 ppm U (IAEA UDEPO / NFCIS)");
            case 4 -> I18n.getOrDefault("resource.tensor.5.format", "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 0 ppb | White (255) = 100 ppb He-3 (NASA PDS / Lunar Prospector)");
            case 5 -> I18n.getOrDefault("resource.tensor.6.format", "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 0 Gt | White (255) = 5.0 Gt (USGS Mineral Resources / BIF)");
            case 6 -> I18n.getOrDefault("resource.tensor.7.format", "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 0 Mt | White (255) = 5.0 Mt (USGS REE / Salars & Carbonatites)");
            case 7 -> I18n.getOrDefault("resource.tensor.8.format", "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 20 mW/m² | White (255) = 250 mW/m² (IHFC / Davies 2013)");
            case 8 -> I18n.getOrDefault("resource.tensor.9.format", "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 0 10³ km³ | White (255) = 5.0 10³ km³ (UNESCO / WHYMAP)");
            default -> I18n.getOrDefault("resource.hint.geo_format", "PNG / GeoTIFF image in 2:1 equirectangular projection");
        };
    }

    private ComboBox<String> buildGeologySourceCombo(int index) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getItems().add("");
        switch (index) {
            case 0 -> combo.getItems().addAll(
                "USGS MRDS Coal Basins (Earth)",
                "BGR Germany Coal & Lignite (Earth)",
                "World Energy Council Coal Deposits (Earth)",
                "Paleo-Carboniferous Wetland Model (Earth)"
            );
            case 1 -> combo.getItems().addAll(
                "World Energy Projection (WEP / BGR — Earth)",
                "USGS World Petroleum Assessment (Earth)",
                "BP Statistical Review of World Energy (Earth)",
                "Marine Anoxic Source Rock Model (Earth)"
            );
            case 2 -> combo.getItems().addAll(
                "WEP / BGR Natural Gas & Shale (Earth)",
                "USGS Global Conventional Gas Fields (Earth)",
                "IEA World Energy Outlook Gas (Earth)",
                "Deep Thermogenic Cracking Model (Earth)"
            );
            case 3 -> combo.getItems().addAll(
                "IAEA UDEPO Uranium Database (Earth)",
                "NEA / IAEA Red Book Uranium Reserves (Earth)",
                "World Nuclear Association Database (Earth)",
                "Proterozoic Unconformity Ore Model (Earth)"
            );
            case 4 -> combo.getItems().addAll(
                "NASA PDS / LPI Lunar Prospector (Lunar Baseline)",
                "NASA Clementine UV-VIS Regolith (Lunar)",
                "Solar Wind Ion Implantation Model (Planetary)",
                "Mare Basalt Titanium-Ilmenite Trap (Lunar)"
            );
            case 5 -> combo.getItems().addAll(
                "USGS Mineral Resources Program — Iron & Copper (Earth)",
                "Banded Iron Formations (BIF) Global Atlas (Earth)",
                "Giant Porphyry Copper Metallogeny (Earth)",
                "Archean Great Oxidation Event Model (Earth)"
            );
            case 6 -> combo.getItems().addAll(
                "USGS Rare Earth Elements & Lithium Salars (Earth)",
                "USGS Global Gold & Platinum Deposits (Earth)",
                "Global Carbonatite & Alkaline Complex Atlas (Earth)",
                "Continental Salar Brine Evaporation Model (Earth)"
            );
            case 7 -> combo.getItems().addAll(
                "IHFC / Davies 2013 Global Heat Flow (mW/m² — Earth)",
                "Pollack et al. Continental Heat Flow (Earth)",
                "CRUST1.0 Lithospheric Heat Flux (Earth)",
                "Mantle Plume & Rift Dynamic Model (Earth)"
            );
            case 8 -> combo.getItems().addAll(
                "UNESCO / WHYMAP Global Groundwater Aquifers (Earth)",
                "BGR World Aquifer Resources Map (Earth)",
                "NASA GRACE Groundwater Depletion (Earth)",
                "Deep Hydrogeological Basin Permeability Model (Earth)"
            );
            default -> combo.getItems().addAll(
                "USGS Scientific Dataset (Earth)",
                "Global Planetary Survey (Earth)"
            );
        }
        combo.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item.isEmpty() ? I18n.getOrDefault("planet.combo.prompt_source", "— Select a data source —") : item);
            }
        });
        combo.setButtonCell(combo.getCellFactory().call(null));
        combo.setValue("");
        combo.setOnAction(e -> {
            if (isUpdatingFromPreset) return;
            String val = combo.getValue();
            if (val != null && !val.isEmpty()) {
                if (geologyImportRadios.containsKey(index)) {
                    geologyImportRadios.get(index).setSelected(true);
                }
                updatePreviewCanvas();
            }
        });
        combo.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.map_source_hint",
                "Select reference data source. The 'Load Map' button below allows importing your local file.")));
        return combo;
    }

    private VBox createGeologyVectorAndLayersSection() {
        geologyDomainSecHeader = new Label(I18n.getOrDefault("resource.section.geological_tensors", "4. DOMAINE GÉOLOGIE, TECTONIQUE ET MINERAIS"));

        geologyLayersDynamicContainer = new VBox(10);
        rebuildGeologyTensorSubBlocks();

        prepopulateEarthGeologyTensors();

        return createSection(geologyDomainSecHeader, geologyLayersDynamicContainer);
    }

    private static class TensorSliderMeta {
        final String labelKey;
        final String defaultLabel;
        final String tooltipKey;
        final String defaultTooltip;
        final double min;
        final double max;
        final double defaultValue;
        final String unit;
        final String formatPattern;

        TensorSliderMeta(String labelKey, String defaultLabel, String tooltipKey, String defaultTooltip, double min, double max, double defaultValue, String unit, String formatPattern) {
            this.labelKey = labelKey;
            this.defaultLabel = defaultLabel;
            this.tooltipKey = tooltipKey;
            this.defaultTooltip = defaultTooltip;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
            this.unit = unit;
            this.formatPattern = formatPattern;
        }
    }

    private static final TensorSliderMeta[][] GEOLOGY_SLIDER_SPECS = {
        // Tensor 4.1: COAL
        {
            new TensorSliderMeta("resource.tensor.1.param1", "Abondance & Épaisseur Veines Charbon", "resource.tensor.1.param1.tt", "Règle l'abondance globale et l'épaisseur moyenne des veines de charbon dans les bassins sédimentaires (Gt).", 0.1, 5.0, 1.0, "Gt", "%.2f Gt"),
            new TensorSliderMeta("resource.tensor.1.param2", "Seuil Paléo-Biomasse Marécages", "resource.tensor.1.param2.tt", "Pourcentage minimal de végétation paléolithique et zones humides requis pour l'accumulation de tourbe.", 0.0, 1.0, 0.35, "%", "%.0f %%"),
            new TensorSliderMeta("resource.tensor.1.param3", "Rang de Carbonisation (Anthracitisation)", "resource.tensor.1.param3.tt", "Degré de métamorphisme thermique (%Ro vitrinite). 0=Lignite, 0.5=Bitumineux, 1.0=Anthracite pure.", 0.0, 1.0, 0.50, "%Ro", "%.2f %%Ro"),
            new TensorSliderMeta("resource.tensor.1.param4", "Épaisseur Couvert Sédimentaire", "resource.tensor.1.param4.tt", "Profondeur d'enfouissement lithostatique dans les bassins paraliques favorisant la compaction.", 0.5, 10.0, 2.5, "km", "%.1f km")
        },
        // Tensor 4.2: CRUDE_OIL
        {
            new TensorSliderMeta("resource.tensor.2.param1", "Volume Réserves & Pièges Pétrole", "resource.tensor.2.param1.tt", "Volume total d'hydrocarbures liquides emmagasinés dans les réservoirs sous-terrains (Gt).", 0.1, 5.0, 1.0, "Gt", "%.2f Gt"),
            new TensorSliderMeta("resource.tensor.2.param2", "Maturation Roche-Mère Anoxique", "resource.tensor.2.param2.tt", "Taux de Carbone Organique Total (TOC) déposé dans les schistes marins en milieu anoxique.", 0.0, 1.0, 0.40, "% TOC", "%.0f %% TOC"),
            new TensorSliderMeta("resource.tensor.2.param3", "Densité Pièges Anticlinaux & Failles", "resource.tensor.2.param3.tt", "Fréquence des déformations tectoniques plissées et dômes salins stoppant la migration du pétrole.", 0.0, 1.0, 0.50, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.2.param4", "Température Fenêtre à Pétrole", "resource.tensor.2.param4.tt", "Plage de température de pyrolyse naturelle (60°C-175°C) convertissant le kérogène en pétrole brut.", 60.0, 180.0, 120.0, "°C", "%.0f °C")
        },
        // Tensor 4.3: NATURAL_GAS
        {
            new TensorSliderMeta("resource.tensor.3.param1", "Pression & Abondance Gaz", "resource.tensor.3.param1.tt", "Volume total de gaz naturel conventionnel et non-conventionnel emmagasiné sous pression (10¹² m³).", 0.1, 5.0, 1.0, "10¹² m³", "%.2f 10¹² m³"),
            new TensorSliderMeta("resource.tensor.3.param2", "Fraction Gaz de Schiste Thermogène", "resource.tensor.3.param2.tt", "Proportion de gaz formé par cracking thermique profond par rapport au gaz biogénique de surface.", 0.0, 1.0, 0.40, "%", "%.0f %%"),
            new TensorSliderMeta("resource.tensor.3.param3", "Profondeur Réservoir Cratonique", "resource.tensor.3.param3.tt", "Profondeur des formations gréseuses et carbonatées réservoirs impactant la rétention gazeuse.", 0.5, 8.0, 3.2, "km", "%.1f km"),
            new TensorSliderMeta("resource.tensor.3.param4", "Étanchéité Roches-Couverture", "resource.tensor.3.param4.tt", "Capacité d'étanchéité des couches d'évaporites et d'argilites empêchant la fuite vers la surface.", 0.0, 1.0, 0.60, "Indice", "%.2f")
        },
        // Tensor 4.4: URANIUM
        {
            new TensorSliderMeta("resource.tensor.4.param1", "Teneur Crustale Pegmatites Uranium", "resource.tensor.4.param1.tt", "Concentration moyenne en Uranium de la croûte magmatique et des granites fertiles (ppm).", 10.0, 1000.0, 250.0, "ppm U", "%.0f ppm"),
            new TensorSliderMeta("resource.tensor.4.param2", "Infiltration Hydrothermale & Altération", "resource.tensor.4.param2.tt", "Circulation des fluides hydrothermaux lessivant et précipitant la pechblende (UO₂) dans les filons.", 0.0, 1.0, 0.45, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.4.param3", "Rapport Massique Thorium/Uranium", "resource.tensor.4.param3.tt", "Rapport isotopique naturel Th²³²/U²³⁸. Un ratio élevé favorise les minéralisations en Thorium.", 0.5, 10.0, 3.5, "Th/U", "%.1f Th/U"),
            new TensorSliderMeta("resource.tensor.4.param4", "Fronts Redox & Discordances", "resource.tensor.4.param4.tt", "Intensité des pièges d'oxydo-réduction aux discordances Protérozoïques (type Athabasca).", 0.0, 1.0, 0.50, "Indice", "%.2f")
        },
        // Tensor 4.5: HELIUM_3
        {
            new TensorSliderMeta("resource.tensor.5.param1", "Concentration Régolithe Surface Hélium-3", "resource.tensor.5.param1.tt", "Densité d'Hélium-3 piégé dans la couche supérieure du régolithe par le vent solaire (ppb).", 1.0, 100.0, 15.0, "ppb", "%.1f ppb"),
            new TensorSliderMeta("resource.tensor.5.param2", "Taux Ilménite FeTiO₃", "resource.tensor.5.param2.tt", "Pourcentage d'ilménite dans la roche spatiale, minéral retenant préférentiellement les ions d'He-3.", 0.0, 1.0, 0.40, "%", "%.0f %%"),
            new TensorSliderMeta("resource.tensor.5.param3", "Flux Vent Solaire (Ionique)", "resource.tensor.5.param3.tt", "Intensité du bombardement de protons/alphas du vent solaire (inverse du champ magnétique).", 0.0, 5.0, 1.0, "×Terre", "%.1f ×Terre"),
            new TensorSliderMeta("resource.tensor.5.param4", "Jardinage Régolithe par Cratères", "resource.tensor.5.param4.tt", "Profondeur de brassage du régolithe par les micro-impacts météoritiques enfouissant l'He-3.", 0.5, 15.0, 3.0, "m", "%.1f m")
        },
        // Tensor 4.6: IRON_COPPER
        {
            new TensorSliderMeta("resource.tensor.6.param1", "Abondance Fer BIF & Cuivre", "resource.tensor.6.param1.tt", "Masse totale des formations de Fer rubané (BIF) et des gisements de cuivre porphyrique (Gt).", 0.1, 5.0, 1.0, "Gt", "%.2f Gt"),
            new TensorSliderMeta("resource.tensor.6.param2", "Activité Magmatique Métallogénique", "resource.tensor.6.param2.tt", "Intensité du volcanisme d'arc et des intrusions magmatiques injectant le cuivre hydrothermal.", 0.0, 1.0, 0.50, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.6.param3", "Précipitation Archéenne BIF (O₂ Indice)", "resource.tensor.6.param3.tt", "Événement de Grande Oxydation (GOE) précipitant le fer dissous océanique sous forme d'hématite.", 0.0, 1.0, 0.60, "Indice O₂", "%.2f"),
            new TensorSliderMeta("resource.tensor.6.param4", "Teneur Cuivre Porphyrique", "resource.tensor.6.param4.tt", "Teneur moyenne en cuivre du minerai brut (% massique de Cu). Porphyres géants: 0.4%-2.5% Cu.", 0.1, 3.0, 0.8, "% Cu", "%.1f %% Cu")
        },
        // Tensor 4.7: PRECIOUS_REE
        {
            new TensorSliderMeta("resource.tensor.7.param1", "Abondance Terres Rares & Précieux", "resource.tensor.7.param1.tt", "Volume de Terres Rares (Nd, Dy, Y), Lithium, Or et Platine en Megatonnes d'oxydes et métaux (Mt).", 0.1, 5.0, 1.0, "Mt", "%.2f Mt"),
            new TensorSliderMeta("resource.tensor.7.param2", "Concentration Salars Lithium", "resource.tensor.7.param2.tt", "Enrichissement évaporitique des saumures de Lithium (Li⁺) dans les salars continentaux.", 0.0, 1.0, 0.40, "%", "%.0f %%"),
            new TensorSliderMeta("resource.tensor.7.param3", "Intrusions Carbonatites & Alcalines", "resource.tensor.7.param3.tt", "Fréquence des dykes de carbonatite et roches alcalines concentrant la monazite et bastnäsite.", 0.0, 1.0, 0.50, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.7.param4", "Pegmatites Spodumène", "resource.tensor.7.param4.tt", "Concentration de Lithium roche-dur dans les filons pegmatitiques à spodumène (% Li₂O).", 0.1, 5.0, 1.5, "% Li₂O", "%.1f %% Li₂O")
        },
        // Tensor 4.8: MANTLE_HEAT
        {
            new TensorSliderMeta("resource.tensor.8.param1", "Flux Thermique Manteau", "resource.tensor.8.param1.tt", "Dissipation thermique conductive moyenne à travers la croûte planétaire en mW/m².", 20.0, 250.0, 65.0, "mW/m²", "%.0f mW/m²"),
            new TensorSliderMeta("resource.tensor.8.param2", "Intensité Plumes & Rifts Tectoniques", "resource.tensor.8.param2.tt", "Multiplicateur d'anomalie thermique aux limites de plaques divergentes et plumes mantelliques.", 0.0, 1.0, 0.50, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.8.param3", "Épaisseur Cratons Lithosphériques", "resource.tensor.8.param3.tt", "Épaisseur de la racine cratonique continentale isolant le flux thermique de surface.", 30.0, 300.0, 150.0, "km", "%.0f km"),
            new TensorSliderMeta("resource.tensor.8.param4", "Chaleur Radiogénique Crustale", "resource.tensor.8.param4.tt", "Production de chaleur par désintégration radioactive de K, U, Th dans la croûte supérieure.", 0.1, 5.0, 1.2, "µW/m³", "%.1f µW/m³")
        },
        // Tensor 4.9: FRESHWATER_AQUIFERS
        {
            new TensorSliderMeta("resource.tensor.9.param1", "Capacité Aquifères Subsurface", "resource.tensor.9.param1.tt", "Volume total d'eau douce souterraine emmagasinée dans les nappes phréatiques et bassins fossiles (10³ km³).", 0.1, 5.0, 1.0, "10³ km³", "%.2f 10³ km³"),
            new TensorSliderMeta("resource.tensor.9.param2", "Conductivité Hydraulique / Perméabilité", "resource.tensor.9.param2.tt", "Perméabilité de la roche aquifère permettant la vitesse de recharge et d'écoulement sous gravité.", 0.0, 1.0, 0.40, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.9.param3", "Porosité Roches Réservoirs", "resource.tensor.9.param3.tt", "Pourcentage de vides interstitiels dans les grès et calcaires retenant l'eau sous pression.", 5.0, 35.0, 18.0, "%", "%.1f %%"),
            new TensorSliderMeta("resource.tensor.9.param4", "Profondeur Permafrost / Cryosphère", "resource.tensor.9.param4.tt", "Épaisseur du sol gelé en permanence (permafrost) scellant les aquifères liquides sous-jacents.", 0.0, 2000.0, 300.0, "m", "%.0f m")
        }
    };

    private void rebuildGeologyTensorSubBlocks() {
        if (geologyLayersDynamicContainer == null) return;
        geologyLayersDynamicContainer.getChildren().clear();

        for (int i = 0; i < 9; i++) {
            final int layerIdx = i;

            VBox subBlock = new VBox(6);
            subBlock.setStyle("-fx-padding: 10 10 10 10; -fx-background-color: rgba(56,189,248,0.04); -fx-background-radius: 6; -fx-border-color: rgba(56,189,248,0.15); -fx-border-radius: 6;");

            Label subTitle = new Label(getGeologyTensorTitle(i));
            subTitle.getStyleClass().add("control-label");
            subTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            subTitle.setWrapText(true);
            Tooltip.install(subTitle, new Tooltip(getGeologyTensorTooltip(i)));
            geologySubTitles.put(layerIdx, subTitle);

            ToggleGroup tg = new ToggleGroup();
            RadioButton radioProc = new RadioButton(I18n.getOrDefault("resource.mode.procedural", "▶ Procedural Generation (Hotspots & Physics)"));
            RadioButton radioImport = new RadioButton(I18n.getOrDefault("resource.mode.import_file", "📂 External Source (PNG / GeoTIFF)"));
            radioProc.setToggleGroup(tg);
            radioImport.setToggleGroup(tg);
            radioProc.getStyleClass().add("radio-proc");
            radioImport.getStyleClass().add("radio-import");

            boolean hasImage = customGeologyLayerImages.containsKey(layerIdx) && customGeologyLayerImages.get(layerIdx) != null;
            if (hasImage) {
                radioImport.setSelected(true);
            } else {
                radioProc.setSelected(true);
            }

            geologyProcRadios.put(layerIdx, radioProc);
            geologyImportRadios.put(layerIdx, radioImport);

            // --- Procedural Configuration Sub-Box ---
            TextField seedTF = new TextField(String.valueOf(12345 + layerIdx * 777));
            seedTF.setPrefWidth(90);
            seedTF.setStyle("-fx-font-size: 11px;");
            geologyTensorSeeds.put(layerIdx, seedTF);

            Button randSeedBtn = new Button("🎲");
            randSeedBtn.getStyleClass().add("button-secondary");
            randSeedBtn.setStyle("-fx-font-size: 11px;");
            randSeedBtn.setOnAction(e -> {
                seedTF.setText(String.valueOf(new Random().nextLong(1000000)));
                updatePreviewCanvas();
            });

            Button btnGenTensor = new Button(I18n.getOrDefault("resource.btn.gen_single_tensor", "🪄 Generate"));
            btnGenTensor.getStyleClass().add("button");
            btnGenTensor.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
            btnGenTensor.setOnAction(e -> updatePreviewCanvas());
            geologyGenBtns.put(layerIdx, btnGenTensor);

            Label seedLbl = new Label(I18n.getOrDefault("resource.label.tensor_seed", "Generation Seed:"));
            geologySeedLabels.put(layerIdx, seedLbl);
            HBox seedBox = new HBox(6, seedLbl, seedTF, randSeedBtn, btnGenTensor);
            seedBox.setAlignment(Pos.CENTER_LEFT);

            VBox slidersContainer = new VBox(4);
            List<Label> sliderTitles = new ArrayList<>();

            for (int s = 0; s < 4; s++) {
                TensorSliderMeta meta = GEOLOGY_SLIDER_SPECS[i][s];

                String labelText = I18n.getOrDefault(meta.labelKey, meta.defaultLabel);
                String tooltipText = I18n.getOrDefault(meta.tooltipKey, meta.defaultTooltip);

                Label titleLabel = new Label(labelText);
                titleLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
                sliderTitles.add(titleLabel);

                Label valLabel = new Label(String.format(meta.formatPattern, meta.defaultValue));
                valLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #3b82f6; -fx-font-weight: bold;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox sliderHeader = new HBox(6, titleLabel, spacer, valLabel);

                Slider slider = new Slider(meta.min, meta.max, meta.defaultValue);
                slider.setShowTickMarks(false);
                slider.setShowTickLabels(false);

                Tooltip tt = new Tooltip(tooltipText);
                tt.setWrapText(true);
                tt.setMaxWidth(300);
                Tooltip.install(titleLabel, tt);
                Tooltip.install(valLabel, tt);
                Tooltip.install(slider, tt);

                slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    valLabel.setText(String.format(meta.formatPattern, newVal.doubleValue()));
                    updatePreviewCanvas();
                });

                if (s == 0) geologyAbundanceSliders.put(layerIdx, slider);
                else if (s == 1) geologyThresholdSliders.put(layerIdx, slider);
                else if (s == 2) geologyParam3Sliders.put(layerIdx, slider);
                else if (s == 3) geologyParam4Sliders.put(layerIdx, slider);

                VBox sBox = new VBox(1, sliderHeader, slider);
                slidersContainer.getChildren().add(sBox);
            }
            geologySliderTitleLabels.put(layerIdx, sliderTitles);

            VBox proceduralBox = new VBox(6, seedBox, slidersContainer);
            proceduralBox.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(56,189,248,0.2); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
            proceduralBox.setVisible(!hasImage);
            proceduralBox.setManaged(!hasImage);

            // --- Import Configuration Sub-Box ---
            Label sourceLbl = new Label(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
            geologySourceLabels.put(layerIdx, sourceLbl);

            ComboBox<String> sourceCombo = buildGeologySourceCombo(layerIdx);
            if (sourceCombo.getItems().size() > 1) {
                sourceCombo.setValue(sourceCombo.getItems().get(1));
            }
            geologySourceCombos.put(layerIdx, sourceCombo);

            Button btnLoad = new Button(I18n.getOrDefault("resource.btn.load_map", "Load Map"));
            btnLoad.getStyleClass().add("button-secondary");
            btnLoad.setStyle("-fx-font-size: 11px;");
            geologyLoadBtns.put(layerIdx, btnLoad);

            Button btnClear = new Button("❌");
            btnClear.getStyleClass().add("button-secondary");
            btnClear.setStyle("-fx-font-size: 11px;");

            HBox btnBox = new HBox(6, btnLoad, btnClear);
            btnBox.setAlignment(Pos.CENTER_LEFT);

            Label fileLbl = new Label(hasImage ? getGeologyBaselineName(layerIdx) : "—");
            fileLbl.getStyleClass().add("value-label");
            fileLbl.setStyle("-fx-font-size: 10px;");
            geologyFileLabels.put(layerIdx, fileLbl);

            Label formatHintLbl = new Label(getGeologyFormatHint(layerIdx));
            formatHintLbl.getStyleClass().add("card-description-muted");
            formatHintLbl.setStyle("-fx-font-size: 9px; -fx-font-style: italic;");
            formatHintLbl.setWrapText(true);
            geologyFormatLabels.put(layerIdx, formatHintLbl);

            VBox importBox = new VBox(6, sourceLbl, sourceCombo, btnBox, fileLbl, formatHintLbl);
            importBox.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(167,139,250,0.25); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
            importBox.setVisible(hasImage);
            importBox.setManaged(hasImage);

            tg.selectedToggleProperty().addListener((obs, oldV, sel) -> {
                boolean isProc = sel == radioProc;
                proceduralBox.setVisible(isProc);
                proceduralBox.setManaged(isProc);
                importBox.setVisible(!isProc);
                importBox.setManaged(!isProc);
                if (!isUpdatingFromPreset) {
                    if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
                    updatePreviewCanvas();
                }
            });

            btnLoad.setOnAction(e -> loadCustomGeologyLayer(layerIdx, img -> {
                customGeologyLayerImages.put(layerIdx, img);
                fileLbl.setText(I18n.getOrDefault("resource.status.geo_loaded", "📷 Custom geological layer loaded"));
                radioImport.setSelected(true);
                updatePreviewCanvas();
            }));

            btnClear.setOnAction(e -> {
                customGeologyLayerImages.remove(layerIdx);
                fileLbl.setText("—");
                radioProc.setSelected(true);
                updatePreviewCanvas();
            });

            subBlock.getChildren().addAll(subTitle, radioProc, proceduralBox, radioImport, importBox);
            geologyLayersDynamicContainer.getChildren().add(subBlock);
        }
    }

    private void loadCustomGeologyLayer(int layerIdx, Consumer<Image> onLoaded) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("resource.dialog.import_geology_title", "Import geological map ") + getGeologyTensorTitle(layerIdx));
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Données Spatiales & Tenseurs (PNG, ASC, GeoJSON, TIF, JPG)", "*.png", "*.asc", "*.geojson", "*.json", "*.tif", "*.tiff", "*.jpg", "*.jpeg")
        );
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                java.awt.Color fallbackCol = getGeologyTensorAwtColor(layerIdx);
                java.awt.image.BufferedImage bImg = ResourceDepositMapReader.readGeologicalDataset(file, 720, 360, fallbackCol);
                if (bImg != null) {
                    onLoaded.accept(bufferedImageToFXImage(bImg));
                } else {
                    Image img = new Image(new FileInputStream(file));
                    onLoaded.accept(img);
                }
            } catch (Exception ex) {
                logger.error("Failed to load geology layer {}", layerIdx, ex);
            }
        }
    }

    private java.awt.Color getGeologyTensorAwtColor(int layerIdx) {
        return switch (layerIdx) {
            case 0 -> new java.awt.Color(245, 158, 11);  // Coal
            case 1 -> new java.awt.Color(239, 68, 68);   // Oil
            case 2 -> new java.awt.Color(56, 189, 248);  // Gas
            case 3 -> new java.awt.Color(34, 197, 94);   // Uranium
            case 4 -> new java.awt.Color(168, 85, 247);  // Helium-3
            case 5 -> new java.awt.Color(217, 119, 6);   // Iron & Copper
            case 6 -> new java.awt.Color(234, 179, 8);   // Precious Metals & REE
            default -> new java.awt.Color(14, 165, 233); // Aquifers
        };
    }

    public boolean isDirty() {
        return ecologyPresetBar != null && ecologyPresetBar.isDirty();
    }

    public boolean promptSaveIfDirty(javafx.stage.Window owner) {
        if (ecologyPresetBar == null) return true;
        return ecologyPresetBar.promptSavePresetIfDirty(owner);
    }

    public PresetControlBar<EcologyPreset> getPresetBar() {
        return ecologyPresetBar;
    }
}
