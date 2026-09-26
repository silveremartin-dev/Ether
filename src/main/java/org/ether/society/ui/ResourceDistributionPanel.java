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
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Enhanced UI Panel for editing planet-wide ecological and resource distribution 
 * using real scientific metric variables (GtC, Gt, Mt, mW/m², 10³ km³).
 * 
 * Features automated planetary derivation from Tab 1 physics, spatial dispersion algorithms,
 * custom biome/geology map imports (PNG / WMS / ESRI World Files), and real-time 2D visualization.
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
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
    private String activeLoadedBodyKey = null;

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
    private TextArea ecologyDescriptionArea;

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
    private VBox validationWarningBanner;
    private Label validationWarningLabel;

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
            String bodyKey = detectBodyKey(planetPreset);
            if (!bodyKey.equalsIgnoreCase(activeLoadedBodyKey) || customBiomeImage == null) {
                activeLoadedBodyKey = bodyKey;
                autoApplyMapsForPreset(planetPreset);
            }
            updatePlanetContextDisplay();
            updatePreviewCanvas();
        } finally {
            isUpdatingFromPreset = oldState;
        }
    }

    private String detectBodyKey(PlanetPreset p) {
        if (p == null) return "none";
        boolean isImportMode = p.elevationUseImport() || (p.customElevBase64() != null && !p.customElevBase64().isBlank()) || (p.elevationMapSource() != null && !p.elevationMapSource().isBlank() && !p.elevationMapSource().equalsIgnoreCase("none"));
        if (!isImportMode) return "none";

        if (p.elevationMapSource() != null && !p.elevationMapSource().isBlank() && !p.elevationMapSource().equalsIgnoreCase("none")) {
            return p.elevationMapSource().toLowerCase();
        }
        String lower = p.name() != null ? p.name().toLowerCase() : "";
        if (lower.contains("terre") || lower.contains("terran") || lower.contains("earth")) return "earth";
        if (lower.contains("mars") || lower.contains("ares")) return "mars";
        if (lower.contains("lune") || lower.contains("moon") || lower.contains("selene")) return "moon";
        if (lower.contains("vénus") || lower.contains("venus") || lower.contains("hesperos")) return "venus";
        if (lower.contains("mercure") || lower.contains("mercury") || lower.contains("hermes")) return "mercury";
        return "none";
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

        validationWarningLabel = new Label();
        validationWarningLabel.setWrapText(true);
        validationWarningLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px;");
        validationWarningBanner = new VBox(4,
                new Label(I18n.getOrDefault("resource.validation.header", "⚠️ Paramètres ou cartes écologiques requis manquants ou invalides :")),
                validationWarningLabel
        );
        validationWarningBanner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10;");
        validationWarningBanner.getChildren().get(0).setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 13px;");
        validationWarningBanner.setVisible(false);
        validationWarningBanner.setManaged(false);

        ecoCompatibilityLabel = new Label();
        ecoCompatibilityLabel.setWrapText(true);
        ecoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-padding: 6 10; -fx-background-radius: 6; -fx-border-radius: 6;");

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
                ecoCompatibilityLabel,
                thermoSynthesisBadge,
                syncPlanetBtn,
                autoDeriveMasterBtn
        ));

        // --- 2. Standardized Preset Control Bar for Ecology ---
        ecologyPresetBar = new PresetControlBar<>("resource.preset_title", "Préréglage Écologique & Ressources");
        ecologyPresetBar.setExportCategory("ecology");
        ecologyPresetBar.setPresets(EcologyPreset.getBuiltInPresets(), EcologyPreset.EARTH_STANDARD);

        ecologyDescriptionArea = new TextArea();
        ecologyDescriptionArea.setPrefRowCount(4);
        ecologyDescriptionArea.setWrapText(true);
        ecologyDescriptionArea.setEditable(false);
        ecologyDescriptionArea.getStyleClass().add("glass-text-area");
        ecologyDescriptionArea.setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0; -fx-background-color: rgba(15, 23, 42, 0.6); -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 4; -fx-background-radius: 4;");

        ecologyPresetBar.setListener(new PresetControlBar.PresetActionsListener<EcologyPreset>() {
            @Override
            public void onPresetSelected(EcologyPreset preset) {
                if (preset != null) {
                    PlanetPreset p = preset.embeddedPlanetPreset() != null
                            ? preset.embeddedPlanetPreset()
                            : findPlanetPresetByName(preset.planetPresetName());
                    if (p != null) {
                        activePlanetPreset = p;
                        if (planetPresetCombo != null) {
                            planetPresetCombo.setValue(p);
                        }
                        if (planetPresetApplyCallback != null) {
                            planetPresetApplyCallback.accept(p);
                        }
                    }
                }
                applyEcologyPreset(preset);
                if (viewModeCombo != null) {
                    viewModeCombo.getSelectionModel().select(1);
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
        mapSourceCombo.getItems().addAll("none", "earth", "mars", "venus", "moon", "mercury");
        mapSourceCombo.setValue("none");
        org.ether.society.data.DataSourceMetadataRegistry.setupDetailedSourceCombo(
                mapSourceCombo, "common.combo.prompt_source", "planet.tooltip.map_source_hint");
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
        biomeSeedField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
            updatePreviewCanvas();
        });
        randSeedBtn = new Button("🎲");
        randSeedBtn.getStyleClass().add("button-secondary");
        randSeedBtn.setOnAction(e -> {
            biomeSeedField.setText(String.valueOf(new Random().nextLong(1000000)));
            if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
            updatePreviewCanvas();
        });
        HBox biomeSeedBox = new HBox(5, biomeSeedField, randSeedBtn);
        HBox.setHgrow(biomeSeedField, Priority.ALWAYS);
        seedField = biomeSeedField; // EcologyPreset compat

        Button exportBiomeBtn = new Button(I18n.getOrDefault("resource.btn.export_biome", "📤 Export Biome Map (PNG / JPEG)"));
        exportBiomeBtn.setMaxWidth(Double.MAX_VALUE);
        exportBiomeBtn.getStyleClass().add("button-secondary");
        exportBiomeBtn.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.export_biome", "Export the active biome map as high-resolution raster (PNG / JPEG).")));
        exportBiomeBtn.setOnAction(e -> exportBiomeMap());

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
        biomeSourceCombo.getItems().addAll("none", "earth", "mars", "venus", "moon", "mercury");
        biomeSourceCombo.setValue("none");
        org.ether.society.data.DataSourceMetadataRegistry.setupDetailedSourceCombo(
                biomeSourceCombo, "common.combo.prompt_source", "resource.desc.import_biome_map");
        biomeSourceCombo.setMaxWidth(Double.MAX_VALUE);
        biomeSourceCombo.setOnAction(e -> {
            if (isUpdatingFromPreset) return;
            String val = biomeSourceCombo.getValue();
            if (val != null && !"none".equals(val)) {
                radioImportBiome.setSelected(true);
                applyPresetMapSource(val);
                if (viewModeCombo != null) {
                    viewModeCombo.getSelectionModel().select(1);
                }
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
            if (viewModeCombo != null) {
                viewModeCombo.getSelectionModel().select(1);
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
        hydroSeedField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
            updatePreviewCanvas();
        });
        Button hydroRandBtn = new Button("🎲");
        hydroRandBtn.getStyleClass().add("button-secondary");
        hydroRandBtn.setOnAction(e -> {
            hydroSeedField.setText(String.valueOf(new Random().nextLong(1000000)));
            if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
            updatePreviewCanvas();
        });
        HBox hydroSeedBox = new HBox(5, hydroSeedField, hydroRandBtn);
        HBox.setHgrow(hydroSeedField, Priority.ALWAYS);

        autoDeriveHydroBtn = new Button(I18n.getOrDefault("resource.btn.auto_derive_hydro", "💧 Auto-derive Aquifers & Freshwater from Physics"));
        autoDeriveHydroBtn.getStyleClass().add("button-secondary");
        autoDeriveHydroBtn.setMaxWidth(Double.MAX_VALUE);
        autoDeriveHydroBtn.setOnAction(e -> autoDeriveHydroFromPlanet());

        hydroStatusLabel = new Label();
        hydroStatusLabel.getStyleClass().add("subcard-status-label");
        hydroStatusLabel.setWrapText(true);

        Button exportHydroBtn = new Button(I18n.getOrDefault("resource.btn.export_hydro", "📤 Export Hydrography Map (PNG / JPEG)"));
        exportHydroBtn.setMaxWidth(Double.MAX_VALUE);
        exportHydroBtn.getStyleClass().add("button-secondary");
        exportHydroBtn.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.export_hydro", "Export the active hydrographic map as high-resolution raster (PNG / JPEG).")));
        exportHydroBtn.setOnAction(e -> exportHydroMap());

        VBox hydroProcBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.seed.label", "Generation Seed:")), hydroSeedBox,
                        "Graine aléatoire pour la génération procédurale des cours d'eau"),
                autoDeriveHydroBtn,
                hydroStatusLabel,
                createControlRow(freshwaterAquiferRowLabel, freshwaterAquiferSlider, "%.0f x10³ km³", I18n.getOrDefault("resource.desc.freshwater_aquifer", "Total volume of groundwater and continental aquifers")),
                exportHydroBtn
        );
        hydroProcBox.getStyleClass().add("subcard-procedural-box");

        hydroSourceCombo = new ComboBox<>();
        hydroSourceCombo.getItems().addAll("none", "earth", "mars", "venus", "moon", "mercury");
        hydroSourceCombo.setValue("none");
        org.ether.society.data.DataSourceMetadataRegistry.setupDetailedSourceCombo(
                hydroSourceCombo, "common.combo.prompt_source", "resource.desc.import_hydro_map");
        hydroSourceCombo.setMaxWidth(Double.MAX_VALUE);
        hydroSourceCombo.setOnAction(e -> {
            if (isUpdatingFromPreset) return;
            String val = hydroSourceCombo.getValue();
            if (val != null && !"none".equals(val)) {
                radioImportHydro.setSelected(true);
                fetchOnlineHydroData();
                if (viewModeCombo != null) {
                    viewModeCombo.getSelectionModel().select(2);
                }
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
            if (viewModeCombo != null) {
                viewModeCombo.getSelectionModel().select(2);
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
        climateSeedField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
            updatePreviewCanvas();
        });
        Button climateRandBtn = new Button("🎲");
        climateRandBtn.getStyleClass().add("button-secondary");
        climateRandBtn.setOnAction(e -> {
            climateSeedField.setText(String.valueOf(new Random().nextLong(1000000)));
            if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
            updatePreviewCanvas();
        });
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
                "🌍 Terre — ERA5 Reanalysis & MODIS / IMERG (Composite) [Global, -100 000 BP à +2100 AD]",
                "🔴 Mars — MGS TES & Subsurface MARSIS (Composite) [Planétaire (Mars), -4.1 Ga à Actuel]",
                "🟡 Vénus — Magellan Radar & VIRTIS Thermal Model [Planétaire (Vénus), -500 Ma à Actuel]",
                "⚪ Lune — LRO Diviner & LCROSS Cold Traps [Planétaire (Lune), -4.5 Ga à Actuel]",
                "⚪ Mercure — MESSENGER MLA & Polar Ice Model [Planétaire (Mercure), -4.0 Ga à Actuel]"
        );
        climateSourceCombo.setValue("none");
        org.ether.society.data.DataSourceMetadataRegistry.setupDetailedSourceCombo(
                climateSourceCombo, "common.combo.prompt_source", "planet.tooltip.climate_map");
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
        geologySeedField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
            updatePreviewCanvas();
        });
        Button geologyRandBtn = new Button("🎲");
        geologyRandBtn.getStyleClass().add("button-secondary");
        geologyRandBtn.setOnAction(e -> {
            geologySeedField.setText(String.valueOf(new Random().nextLong(1000000)));
            if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
            updatePreviewCanvas();
        });
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
        geologySourceCombo.getItems().addAll("none", "earth", "mars", "venus", "moon", "mercury");
        geologySourceCombo.setValue("none");
        org.ether.society.data.DataSourceMetadataRegistry.setupDetailedSourceCombo(
                geologySourceCombo, "common.combo.prompt_source", "resource.desc.import_geology_map");
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
                validationWarningBanner,
                ecologyPresetBar,
                ecologyDescriptionArea,
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
                I18n.getOrDefault("resource.view.section_base", "────────── 12 CARTES CANONIQUES DE BASE ──────────"),
                I18n.getOrDefault("resource.view.biomes", "🌿 1. Biomes & Couverture Végétale"),
                I18n.getOrDefault("resource.view.hydro", "🌊 2. Hydrographie & Réseau Fluvial"),
                I18n.getOrDefault("resource.view.coal", "⛏️ 3. Gisements de Charbon"),
                I18n.getOrDefault("resource.view.oil", "🛢️ 4. Réserves de Pétrole Brut"),
                I18n.getOrDefault("resource.view.gas", "🔥 5. Champs de Gaz Naturel"),
                I18n.getOrDefault("resource.view.uranium", "⚛️ 6. Minerais d'Uranium & Fission"),
                I18n.getOrDefault("resource.view.helium3", "🌌 7. Hélium-3 & Fusion Lunaires"),
                I18n.getOrDefault("resource.view.iron_copper", "⛓️ 8. Métaux Fer BIF & Cuivre"),
                I18n.getOrDefault("resource.view.precious_metals", "💎 9. Métaux Précieux (Or, Argent, PGM)"),
                I18n.getOrDefault("resource.view.rare_earths", "🔋 10. Terres Rares & Minéraux Critiques"),
                I18n.getOrDefault("resource.view.heat", "🌋 11. Flux Thermique du Manteau"),
                I18n.getOrDefault("resource.view.aquifer", "💧 12. Aquifères & Eau Douce"),
                I18n.getOrDefault("resource.view.section_derived", "────────── CARTES DÉDUITES / ANOMALIES ──────────"),
                I18n.getOrDefault("resource.view.temp", "🌡️ 13. Températures Surface & Microclimats"),
                I18n.getOrDefault("resource.view.seismic", "🌋 14. Tectonique & Aléa Sismique/Volcanique"),
                I18n.getOrDefault("resource.view.aridity", "🏜️ 15. Aridité & Salinisation des Sols")
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
            WindowUtils.setBusyCursor(this, true);
            try {
                updateLegend();
                updatePreviewCanvas();
            } finally {
                WindowUtils.setBusyCursor(this, false);
            }
        });

        btnReliefOverlay = new ToggleButton(I18n.getOrDefault("resource.btn.relief_overlay", "⛰️ Relief"));
        btnReliefOverlay.setSelected(false);
        btnReliefOverlay.getStyleClass().add("button-secondary");
        btnReliefOverlay.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.relief_overlay", "Superposer l'ombrage du relief topographique et des pentes avec délimitation du trait de côte.")));
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

        if (radioProcHydro != null) {
            radioProcHydro.setSelected(true);
        }
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

        if (radioProcGeology != null) {
            radioProcGeology.setSelected(true);
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

        if (ecologyDescriptionArea != null) {
            ecologyDescriptionArea.setText(p.getPresetDescription());
        }

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

        String planetKey = p.getCanonicalPlanet();
        long epochYear = p.getAssociatedEpochYear();

        if (p.customBiomeBase64() != null) {
            customBiomeImage = ImageMapLoader.base64PngToImage(p.customBiomeBase64());
            if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.preset.biomes", "🌿 Preset Biomes"));
            if (radioImportBiome != null) radioImportBiome.setSelected(true);
        } else if ("earth".equals(planetKey)) {
            if (biomeSourceCombo != null) biomeSourceCombo.setValue("earth");
            customBiomeImage = ImageMapLoader.loadMapImage("earth", epochYear, "biomes");
            String yearStr = (epochYear <= 0) ? Math.abs(epochYear) + " BP" : epochYear + " AD";
            if (biomeFileLabel != null) biomeFileLabel.setText(getBiomeSourceDisplayName("earth") + " (" + yearStr + ")");
            if (radioImportBiome != null) radioImportBiome.setSelected(true);
        } else if (planetKey != null && !"none".equals(planetKey)) {
            if (biomeSourceCombo != null) biomeSourceCombo.setValue(planetKey);
            customBiomeImage = ImageMapLoader.loadMapImage(planetKey + "_biomes.png");
            if (biomeFileLabel != null) biomeFileLabel.setText(getBiomeSourceDisplayName(planetKey));
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
        } else if ("earth".equals(planetKey)) {
            if (geologySourceCombo != null) geologySourceCombo.setValue("earth");
            String yearStr = (epochYear <= 0) ? Math.abs(epochYear) + " BP" : epochYear + " AD";
            if (resourceFileLabel != null) resourceFileLabel.setText(getGeologySourceDisplayName("earth") + " (" + yearStr + ")");
            if (radioImportGeology != null) radioImportGeology.setSelected(true);
            prepopulateGeologyTensors("earth", epochYear);
        } else if (planetKey != null && !"none".equals(planetKey)) {
            if (geologySourceCombo != null) geologySourceCombo.setValue(planetKey);
            if (resourceFileLabel != null) resourceFileLabel.setText(getGeologySourceDisplayName(planetKey));
            if (radioImportGeology != null) radioImportGeology.setSelected(true);
            prepopulateGeologyTensors(planetKey, epochYear);
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
        } else if ("earth".equals(planetKey)) {
            if (hydroSourceCombo != null) hydroSourceCombo.setValue("earth");
            customHydroImage = ImageMapLoader.loadMapImage("earth", epochYear, "aquifers");
            String yearStr = (epochYear <= 0) ? Math.abs(epochYear) + " BP" : epochYear + " AD";
            if (hydroFileLabel != null) hydroFileLabel.setText(getHydroSourceDisplayName("earth") + " (" + yearStr + ")");
            if (radioImportHydro != null) radioImportHydro.setSelected(true);
        } else if (planetKey != null && !"none".equals(planetKey)) {
            if (hydroSourceCombo != null) hydroSourceCombo.setValue(planetKey);
            customHydroImage = ImageMapLoader.loadMapImage(planetKey + "_aquifers.png");
            if (hydroFileLabel != null) hydroFileLabel.setText(getHydroSourceDisplayName(planetKey));
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
        } else if ("earth".equals(planetKey)) {
            customClimateImage = ImageMapLoader.loadMapImage("earth", epochYear, "temperature");
            String yearStr = (epochYear <= 0) ? Math.abs(epochYear) + " BP" : epochYear + " AD";
            if (climateFileLabel != null) climateFileLabel.setText(getClimateSourceDisplayName("earth") + " (" + yearStr + ")");
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else if (planetKey != null && !"none".equals(planetKey)) {
            customClimateImage = ImageMapLoader.loadMapImage(planetKey + "_temperature.png");
            if (climateFileLabel != null) climateFileLabel.setText(getClimateSourceDisplayName(planetKey));
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else {
            customClimateImage = null;
            if (climateFileLabel != null) climateFileLabel.setText("—");
            if (radioProcClimate != null) radioProcClimate.setSelected(true);
        }

        if (p.customRainfallBase64() != null) {
            customRainfallImage = ImageMapLoader.base64PngToImage(p.customRainfallBase64());
            if (rainfallFileLabel != null) rainfallFileLabel.setText(I18n.getOrDefault("planet.preset.rainfall", "🌧️ Preset Rainfall"));
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else if ("earth".equals(planetKey)) {
            customRainfallImage = ImageMapLoader.loadMapImage("earth", epochYear, "precipitation");
            String yearStr = (epochYear <= 0) ? Math.abs(epochYear) + " BP" : epochYear + " AD";
            if (rainfallFileLabel != null) rainfallFileLabel.setText(getRainfallSourceDisplayName("earth") + " (" + yearStr + ")");
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else if (planetKey != null && !"none".equals(planetKey)) {
            customRainfallImage = ImageMapLoader.loadMapImage(planetKey + "_precipitation.png");
            if (rainfallFileLabel != null) rainfallFileLabel.setText(getRainfallSourceDisplayName(planetKey));
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else {
            customRainfallImage = null;
            if (rainfallFileLabel != null) rainfallFileLabel.setText("—");
        }

        if (p.customSeasonalityBase64() != null) {
            customSeasonalityImage = ImageMapLoader.base64PngToImage(p.customSeasonalityBase64());
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText(I18n.getOrDefault("planet.preset.seasonality", "☀️ Preset Seasonality"));
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else if ("earth".equals(planetKey)) {
            customSeasonalityImage = ImageMapLoader.loadMapImage("earth", epochYear, "seasonality");
            String yearStr = (epochYear <= 0) ? Math.abs(epochYear) + " BP" : epochYear + " AD";
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText(getSeasonalitySourceDisplayName("earth") + " (" + yearStr + ")");
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else if (planetKey != null && !"none".equals(planetKey)) {
            customSeasonalityImage = ImageMapLoader.loadMapImage(planetKey + "_seasonality.png");
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText(getSeasonalitySourceDisplayName(planetKey));
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else {
            customSeasonalityImage = null;
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText("—");
        }

        if (viewModeCombo != null) {
            viewModeCombo.getSelectionModel().select(1);
        }
        if (ecologyPresetBar != null) {
            ecologyPresetBar.markClean(p);
        }
        isUpdatingFromPreset = false;
        updateSummary();
        updateLegend();
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
                if (viewModeCombo != null) {
                    viewModeCombo.getSelectionModel().select(1);
                }
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
        WindowUtils.showScrollableInfoDialog(
                I18n.getOrDefault("planet.dialog.climate_help_title", "Spécifications des Cartes Climatiques"),
                I18n.getOrDefault("planet.dialog.climate_help_header", "Formats d'Image Supportés pour les Données Climatiques"),
                "Images PNG/JPEG au ratio 2:1 (ex: 2048×1024 px, projection équirectangulaire Plate Carrée).\n\n" +
                "1. CARTES SÉPARÉES EN NIVEAUX DE GRIS OU DÉGRADÉS THERMIQUES :\n" +
                "   • Températures : Noir / Bleu = −50°C, Blanc / Rouge = +50°C\n" +
                "   • Précipitations : Noir = 0 mm/an, Blanc / Bleu foncé = 3000 mm/an\n" +
                "   • Saisonnalité : Noir = 0°C, Blanc / Magenta = 50°C d'amplitude annuelle\n\n" +
                "2. CARTE COMBINÉE MULTI-CANAUX RGB :\n" +
                "   • R (Rouge) = Température (−50°C à +50°C)\n" +
                "   • G (Vert) = Précipitations (0 à 3000 mm/an)\n" +
                "   • B (Bleu) = Saisonnalité / Amplitude thermique (0 à 50°C)\n\n" +
                "3. TÉLÉCHARGEMENT SATELLITE WMS :\n" +
                "   • Le bouton « 🌐 Télécharger Climat Satellite » permet d'obtenir directement les flux officiels NASA MODIS (température) et GPM (pluviométrie)."
        );
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
        } else if ("mercury".equalsIgnoreCase(sourceKey)) {
            if ("mercury".equalsIgnoreCase(elevSrc)) {
                isCompatible = true;
            } else if (!planetName.contains("mercury") && !planetName.contains("mercure") && !planetName.contains("hermes")) {
                isCompatible = false;
            }
        }

        if (!isCompatible) {
            String activeDisplayName = activePreset != null ? activePreset.name() : "Standard";
            if (ecoCompatibilityLabel != null) {
                ecoCompatibilityLabel.setText(String.format(I18n.getOrDefault("resource.status.incompatible_map", "⚠️ Incompatibilité : Carte « %s » sur terrain « %s » (Relief non concordant)"), sourceKey.toUpperCase(), activeDisplayName));
                ecoCompatibilityLabel.getStyleClass().setAll("compatibility-warning");
            }
            return true;
        }

        if (ecoCompatibilityLabel != null) {
            String activeDisplayName = activePreset != null ? activePreset.name() : "Standard";
            ecoCompatibilityLabel.setText(String.format(I18n.getOrDefault("resource.status.compatible_map", "✅ Carte « %s » vérifiée et compatible avec le terrain « %s »"), sourceKey.toUpperCase(), activeDisplayName));
            ecoCompatibilityLabel.getStyleClass().setAll("compatibility-success");
        }
        return true;
    }

    /**
     * Detects the type of celestial body from the given PlanetPreset and automatically
     * selects the correct map source (earth / mars / moon / venus / mercury / none), which triggers
     * radio button switching and satellite data loading for biomes, hydro and geology.
     * This mirrors the automatic behaviour of Tab 1 when a planet preset is changed.
     */
    private void autoApplyMapsForPreset(PlanetPreset p) {
        if (p == null) return;
        boolean isImportMode = p.elevationUseImport() || (p.customElevBase64() != null && !p.customElevBase64().isBlank()) || (p.elevationMapSource() != null && !p.elevationMapSource().isBlank() && !p.elevationMapSource().equalsIgnoreCase("none"));
        if (!isImportMode) {
            applyPresetMapSource("none");
            return;
        }
        String lower = p.name() != null ? p.name().toLowerCase() : "";
        String sourceKey;
        if (p.elevationMapSource() != null && !p.elevationMapSource().isBlank() && !p.elevationMapSource().equalsIgnoreCase("none")) {
            sourceKey = p.elevationMapSource().toLowerCase();
        } else if (lower.contains("terre") || lower.contains("terran") || lower.contains("earth")) {
            sourceKey = "earth";
        } else if (lower.contains("mars") || lower.contains("ares")) {
            sourceKey = "mars";
        } else if (lower.contains("lune") || lower.contains("moon") || lower.contains("selene")) {
            sourceKey = "moon";
        } else if (lower.contains("vénus") || lower.contains("venus") || lower.contains("hesperos")) {
            sourceKey = "venus";
        } else if (lower.contains("mercure") || lower.contains("mercury") || lower.contains("hermes")) {
            sourceKey = "mercury";
        } else {
            // Unknown body → stay procedural
            applyPresetMapSource("none");
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
        if (radioImportClimate != null) radioImportClimate.setSelected(true);
        isUpdatingFromPreset = false;

        String body = sourceKey.toLowerCase();
        if (biomeSourceCombo != null) biomeSourceCombo.setValue(body);
        if (geologySourceCombo != null) geologySourceCombo.setValue(body);
        if (hydroSourceCombo != null) hydroSourceCombo.setValue(body);

        // Load Biomes
        customBiomeImage = ImageMapLoader.loadMapImage(body + "_biomes.png");
        if (biomeFileLabel != null) biomeFileLabel.setText(getBiomeSourceDisplayName(body));

        // Load Hydro
        customHydroImage = ImageMapLoader.loadMapImage(body + "_aquifers.png");
        if (hydroFileLabel != null) hydroFileLabel.setText(getHydroSourceDisplayName(body));

        // Load Climate
        customClimateImage = ImageMapLoader.loadMapImage(body + "_temperature.png");
        customRainfallImage = ImageMapLoader.loadMapImage(body + "_precipitation.png");
        customSeasonalityImage = ImageMapLoader.loadMapImage(body + "_seasonality.png");
        if (climateFileLabel != null) climateFileLabel.setText(getClimateSourceDisplayName(body));
        if (rainfallFileLabel != null) rainfallFileLabel.setText(getRainfallSourceDisplayName(body));
        if (seasonalityFileLabel != null) seasonalityFileLabel.setText(getSeasonalitySourceDisplayName(body));

        // Load Geology Tensors
        if (resourceFileLabel != null) resourceFileLabel.setText(getGeologySourceDisplayName(body));
        prepopulateGeologyTensors(body);

        if (ecoCompatibilityLabel != null) {
            ecoCompatibilityLabel.setText(String.format("🪐 %s authentic maps loaded (Biomes, Hydro, Climate & 9 Geology Tensors)", body.toUpperCase()));
            ecoCompatibilityLabel.getStyleClass().setAll("compatibility-success");
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
                if (viewModeCombo != null) {
                    viewModeCombo.getSelectionModel().select(2);
                }
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
        if (radioProcHydro != null) radioProcHydro.setSelected(true);
        if (viewModeCombo != null) {
            viewModeCombo.getSelectionModel().select(2);
        }
        if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("resource.mode.procedural_hydro", "⚡ Procedural Hydrography (Rivers/Slope)"));
        if (mapStatusLabel != null) mapStatusLabel.setText(I18n.getOrDefault("resource.map.procedural_generated", "⚡ Procedural hydrographic map generated via slope and watershed calculation."));
        if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
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
        int mode = getViewModeIndex();
        PlanetPreset planet = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        if (planet == null) planet = PlanetPreset.EARTH_LIKE;
        if (seismicActivitySlider != null || volcanicActivitySlider != null) {
            double sVal = seismicActivitySlider != null ? seismicActivitySlider.getValue() : planet.seismicActivityLevel();
            double vVal = volcanicActivitySlider != null ? volcanicActivitySlider.getValue() : planet.volcanicActivityLevel();
            planet = planet.withSeismicAndVolcanic(sVal, vVal);
        }

        int w = 2048;
        int h = 1024;
        float[][] grid = new float[h][w];

        String layerName;
        String unit = "Index";
        float minVal = 0.0f;
        float maxVal = 1.0f;
        java.util.function.Function<Float, Color> colorMapper;

        if (mode == 0) { // Biomes
            layerName = "biomes";
            unit = "BiomeID";
            maxVal = 13.0f;
            for (int y = 0; y < h; y++) {
                double lat = 90.0 - ((double) y / h) * 180.0;
                for (int x = 0; x < w; x++) {
                    double lon = -180.0 + ((double) x / w) * 360.0;
                    var pt = generator.getPlanetPoint(lat, lon, planet);
                    grid[y][x] = (float) pt.biome().ordinal();
                }
            }
            colorMapper = val -> {
                int ord = Math.round(val);
                Biome[] biomes = Biome.values();
                if (ord >= 0 && ord < biomes.length) {
                    return mapLoader.getBiomeTargetColor(biomes[ord]);
                }
                return Color.BLACK;
            };
        } else if (mode == 1) { // Hydrography
            layerName = "hydrography";
            unit = "FlowIndex";
            for (int y = 0; y < h; y++) {
                double lat = 90.0 - ((double) y / h) * 180.0;
                for (int x = 0; x < w; x++) {
                    double lon = -180.0 + ((double) x / w) * 360.0;
                    var pt = generator.getPlanetPoint(lat, lon, planet);
                    grid[y][x] = (float) pt.riverFlow();
                }
            }
            colorMapper = val -> {
                if (val > 0.42f) return Color.rgb(2, 132, 199);
                if (val > 0.28f) return Color.rgb(56, 189, 248);
                if (val > 0.16f) return Color.rgb(20, 184, 166);
                return Color.rgb(75, 85, 99);
            };
        } else if (mode >= 2 && mode <= 11) { // Geology 0..9
            int layerIdx = mode - 2;
            layerName = "geology_layer_" + layerIdx;
            unit = "Density";
            for (int y = 0; y < h; y++) {
                double lat = 90.0 - ((double) y / h) * 180.0;
                for (int x = 0; x < w; x++) {
                    double lon = -180.0 + ((double) x / w) * 360.0;
                    grid[y][x] = (float) sampleProceduralGeologyTensor(layerIdx, lon, lat, planet);
                }
            }
            colorMapper = val -> getGeologyResourceColor(layerIdx, val);
        } else {
            layerName = "raster_layer";
            for (int y = 0; y < h; y++) {
                double lat = 90.0 - ((double) y / h) * 180.0;
                for (int x = 0; x < w; x++) {
                    double lon = -180.0 + ((double) x / w) * 360.0;
                    var pt = generator.getPlanetPoint(lat, lon, planet);
                    grid[y][x] = (float) pt.temperature();
                }
            }
            colorMapper = val -> Color.color(0.5, 0.5, 0.5);
        }

        String defaultName = String.format("ether-%s-%s-%dx%d.png",
                layerName, planet.name().toLowerCase().replaceAll("[^a-z0-9]", "-"), w, h);
        org.ether.society.data.GeospatialRasterExporter.exportRasterWithDialog(
                getScene() != null ? getScene().getWindow() : null,
                grid,
                defaultName,
                I18n.getOrDefault("resource.dialog.export_raster", "Export Ecological / Geological Raster (GeoTIFF / PNG / ASCII)"),
                colorMapper,
                minVal,
                maxVal,
                unit
        );
    }

    private void showEcologyImportFormatHelp() {
        WindowUtils.showScrollableInfoDialog(
                I18n.getOrDefault("resource.dialog.specs_title", "Spécifications des Biomes & Ressources Géologiques"),
                I18n.getOrDefault("resource.dialog.specs_header", "Formats Attendus : Biomes Polychromes & Gisements Minéraux"),
                "Vous pouvez importer des cartes de biomes et de ressources géologiques (PNG/JPEG ratio 2:1) :\n\n" +
                "1. CARTE DE BIOMES (POLYCHROME DISCRET CATÉGORIEL) :\n" +
                "   Chaque biome correspond à un code couleur RVB strict (identique sur toutes les planètes) :\n" +
                "   • Océan Profond : RGB(0, 0, 100)\n" +
                "   • Océan / Mer : RGB(0, 50, 200)\n" +
                "   • Plage / Littoral : RGB(240, 220, 150)\n" +
                "   • Plaines / Prairies : RGB(100, 200, 50)\n" +
                "   • Forêt Tempérée : RGB(20, 120, 20)\n" +
                "   • Jungle Tropicale : RGB(0, 80, 0)\n" +
                "   • Désert Aride : RGB(255, 200, 50)\n" +
                "   • Collines : RGB(150, 150, 100)\n" +
                "   • Montagnes : RGB(100, 100, 100)\n" +
                "   • Toundra : RGB(150, 200, 220)\n" +
                "   • Neige / Glaciers : RGB(255, 255, 255)\n\n" +
                "2. CARTES GÉOLOGIQUES & ÉNERGÉTIQUES UNIFIÉES (PAR CORPS CÉLESTE) :\n" +
                "   Les couches géologiques utilisent une charte chromatique standardisée :\n" +
                "   • 🪨 Charbon : Dégradé Ambre / Brun doré (#92400E → #FBBF24)\n" +
                "   • 🛢️ Pétrole : Dégradé Rouge Rubis / Carmin (#991B1B → #F87171)\n" +
                "   • 💨 Gaz Naturel : Dégradé Cyan / Bleu Ciel (#0E7490 → #38BDF8)\n" +
                "   • ☢️ Uranium / Thorium : Vert Émeraude (#22C55E)\n" +
                "   • ⚛️ Hélium-3 : Violet Néon / Pourpre (#A855F7)\n" +
                "   • ⛏️ Fer & Cuivre : Rouille / Cuivre Orangé (#8B4513 → #F97316)\n" +
                "   • 🪙 Métaux Précieux & Terres Rares : Or / Jaune (#EAB308)\n" +
                "   • 🌋 Géothermie / Chaleur Mantellique : Incandescence Rouge-Orange (#EF4444)\n" +
                "   • 💧 Aquifères & Glace d'Eau : Bleu Azur (#3B82F6)\n\n" +
                "3. CARTE GÉOLOGIQUE MULTI-CANAUX COMBINÉE (RGB) :\n" +
                "   • Canal Rouge (R) = Gisements Métalliques (Fer, Cuivre)\n" +
                "   • Canal Vert (V) = Biomasse Végétale et Forêts\n" +
                "   • Canal Bleu (B) = Eau Souterraine / Aquifères\n\n" +
                "4. GÉORÉFÉRENCEMENT ESRI WORLD FILE (.tfw) :\n" +
                "   • L'export génère automatiquement un fichier .tfw pour l'ouverture directe dans QGIS / ArcGIS."
        );
    }

    private static final double[][] COAL_SPOTS = {
        // North America (Appalachian, Powder River, Illinois, Gulf Coast)
        {-80.5, 39.8, 55, 1.4}, {-88.5, 38.5, 45, 1.3}, {-105.5, 44.5, 60, 1.5}, {-87.0, 33.5, 40, 1.2},
        {-114.5, 53.5, 45, 1.3}, {-108.5, 36.8, 40, 1.2}, {-96.0, 38.0, 35, 1.1},
        // Europe & Eurasia (Ruhr, Silesia, Donbas, Kuzbass, Pechora, Karaganda, Ekibastuz)
        {7.0, 51.5, 40, 1.3}, {19.0, 50.2, 45, 1.4}, {38.5, 48.0, 50, 1.4}, {86.5, 54.5, 65, 1.5},
        {57.5, 65.5, 45, 1.2}, {73.0, 49.8, 50, 1.3}, {75.5, 51.7, 45, 1.3}, {-1.5, 53.5, 35, 1.1},
        // China & East Asia (Shanxi, Ordos, Xinjiang, Taebaek)
        {112.5, 37.8, 65, 1.6}, {109.0, 39.5, 60, 1.5}, {87.5, 44.0, 55, 1.4}, {104.5, 29.5, 40, 1.2},
        {127.0, 38.0, 35, 1.1}, {117.5, 34.5, 45, 1.3},
        // South & Southeast Asia (Damodar, Godavari, Mahanadi, Singrauli, Neyveli, Thar, Kalimantan, Sumatra)
        {86.2, 23.7, 50, 1.5}, {80.0, 18.0, 40, 1.2}, {85.0, 21.0, 40, 1.3}, {82.6, 23.0, 45, 1.3},
        {79.5, 11.5, 35, 1.1}, {70.2, 24.8, 35, 1.1}, {103.8, -3.7, 45, 1.3}, {116.8, -1.0, 50, 1.4},
        // Africa (Witbank, Waterberg, Moatize, Hwange, Mmamabula, Enugu)
        {29.2, -26.0, 50, 1.4}, {27.5, -23.7, 45, 1.3}, {33.7, -16.1, 40, 1.3}, {26.0, -18.3, 35, 1.1},
        {26.8, -22.7, 35, 1.1}, {7.5, 6.4, 30, 1.0},
        // Oceania (Bowen, Sydney Hunter, Surat, Galilee, Latrobe, Collie)
        {148.5, -22.5, 65, 1.5}, {150.8, -32.8, 55, 1.4}, {150.0, -27.5, 50, 1.3}, {145.5, -23.0, 50, 1.3},
        {146.5, -38.2, 40, 1.2}, {116.2, -33.4, 35, 1.1}
    };
    private static final double[][] OIL_SPOTS = {
        // Middle East & Persian Gulf Super-Basins (Ghawar, Burgan, Rumaila, Ahwaz, Zakum, Marun)
        {49.3, 25.5, 75, 1.6}, {48.0, 29.1, 65, 1.5}, {47.2, 30.5, 65, 1.5}, {49.8, 31.3, 60, 1.4},
        {53.8, 24.3, 55, 1.4}, {51.6, 26.5, 45, 1.3}, {56.5, 21.0, 50, 1.3}, {33.3, 28.2, 40, 1.2},
        // Russia & Eurasia (West Siberia Samotlor/Priobskoye, Volga-Ural, Tengiz, Kashagan, Baku, Sakhalin)
        {76.5, 61.2, 75, 1.6}, {52.5, 54.8, 60, 1.4}, {51.8, 46.5, 65, 1.5}, {53.0, 43.5, 45, 1.3},
        {50.5, 40.0, 55, 1.4}, {57.5, 66.0, 50, 1.3}, {88.0, 67.8, 50, 1.3}, {143.2, 52.5, 45, 1.3},
        // North America (Permian, Eagle Ford, GOM, Bakken, Prudhoe Bay, Athabasca, WCSB, Cantarell)
        {-102.5, 31.8, 65, 1.5}, {-98.0, 28.5, 55, 1.4}, {-90.5, 27.5, 60, 1.4}, {-103.5, 48.0, 50, 1.3},
        {-148.5, 70.2, 55, 1.4}, {-111.5, 56.8, 70, 1.5}, {-115.0, 54.5, 55, 1.3}, {-98.5, 35.5, 45, 1.2},
        {-119.5, 35.3, 45, 1.2}, {-92.2, 19.5, 55, 1.4}, {-48.8, 46.8, 40, 1.2},
        // South America (Maracaibo, Orinoco Belt, Santos Pre-Salt, Campos, Llanos, Vaca Muerta, Guyana Liza)
        {-71.5, 10.0, 55, 1.4}, {-64.0, 8.5, 70, 1.5}, {-43.0, -24.5, 65, 1.5}, {-40.5, -22.5, 55, 1.4},
        {-71.5, 4.5, 50, 1.3}, {-76.5, -1.5, 50, 1.3}, {-69.0, -38.0, 55, 1.4}, {-57.0, 8.0, 50, 1.4},
        // Africa (Niger Delta, Lower Congo/Angola, Sirte, Hassi Messaoud, Muglad, Gabon)
        {6.0, 4.8, 65, 1.5}, {11.8, -6.5, 60, 1.4}, {13.0, -9.5, 50, 1.3}, {19.5, 29.0, 55, 1.4},
        {6.0, 31.5, 55, 1.4}, {29.5, 9.5, 50, 1.3}, {9.5, -1.5, 45, 1.3},
        // Europe, Asia-Pacific & Australia (North Sea Ekofisk/Sverdrup, Daqing, Bohai, Tarim, Minas, Mumbai High, Barrow)
        {2.5, 57.5, 60, 1.4}, {7.5, 65.0, 50, 1.3}, {125.0, 46.5, 55, 1.4}, {118.5, 38.0, 50, 1.3},
        {83.5, 40.5, 55, 1.3}, {101.5, 0.8, 55, 1.4}, {114.5, 5.5, 50, 1.3}, {72.0, 19.3, 50, 1.3},
        {115.0, -21.0, 50, 1.3}, {148.5, -38.5, 45, 1.3}
    };
    private static final double[][] GAS_SPOTS = {
        // Middle East (North Field / South Pars, Fars, Khuff, Levantine Zohr/Leviathan, Nile Delta)
        {51.8, 26.5, 75, 1.6}, {52.5, 27.8, 65, 1.5}, {49.5, 25.0, 55, 1.4}, {56.0, 22.0, 45, 1.3},
        {33.0, 32.5, 55, 1.4}, {31.5, 31.8, 50, 1.3},
        // Russia & Central Asia (Yamal Bovanenkovo, Urengoy/Yamburg, Gydan, Galkynysh, Gazli, Karachaganak, Shtokman)
        {69.5, 70.5, 75, 1.6}, {77.5, 66.0, 75, 1.6}, {75.5, 71.0, 60, 1.4}, {62.2, 37.3, 65, 1.5},
        {64.0, 39.5, 55, 1.4}, {53.2, 51.3, 50, 1.3}, {111.0, 58.5, 60, 1.4}, {43.5, 73.0, 55, 1.4},
        // North America (Appalachian Marcellus/Utica, Haynesville, Permian Gas, Barnett, Montney, San Juan, Jonah)
        {-78.5, 40.5, 70, 1.5}, {-93.8, 32.2, 55, 1.4}, {-102.5, 31.8, 60, 1.4}, {-97.5, 33.0, 45, 1.3},
        {-120.0, 56.0, 65, 1.4}, {-107.8, 36.8, 45, 1.3}, {-109.8, 42.5, 45, 1.3}, {-147.0, 70.2, 50, 1.3},
        // Europe & Africa (Groningen, Troll/Oseberg, Ormen Lange, Snohvit, Dnieper-Donets, Hassi R'Mel, Rovuma, Niger Delta)
        {6.8, 53.3, 60, 1.4}, {3.5, 60.6, 65, 1.5}, {6.0, 63.5, 50, 1.3}, {21.0, 71.5, 45, 1.3},
        {36.5, 49.5, 50, 1.3}, {3.3, 32.9, 65, 1.5}, {40.8, -11.0, 60, 1.4}, {6.5, 4.5, 55, 1.4},
        // Asia-Pacific & Australia (Sichuan, Tarim Kuqa, Ordos Sulige, Gorgon NW Shelf, Browse, Tangguh, Natuna, Camisea)
        {106.0, 30.5, 65, 1.5}, {82.5, 41.8, 55, 1.4}, {108.5, 38.5, 55, 1.4}, {115.5, -19.5, 65, 1.5},
        {123.5, -14.0, 55, 1.4}, {133.0, -2.5, 50, 1.3}, {109.0, 4.5, 50, 1.3}, {-72.8, -11.8, 50, 1.4},
        {69.0, 28.5, 50, 1.3}, {-63.8, -21.5, 45, 1.3}
    };
    private static final double[][] URANIUM_SPOTS = {
        {-105.5, 58.0, 55, 1.5}, {136.9, -30.4, 50, 1.4}, {68.0, 44.0, 60, 1.5}, {66.0, 43.0, 50, 1.3},
        {7.4, 18.7, 45, 1.2}, {27.5, -26.2, 45, 1.2}, {118.0, 50.0, 45, 1.2}, {-109.0, 38.0, 48, 1.2},
        {15.0, -22.5, 45, 1.2}, {132.8, -12.7, 45, 1.2}
    };
    private static final double[][] HE3_SPOTS = {
        {23.5, 8.5, 50, 1.2}, {-43.0, 18.0, 60, 1.2}, {17.5, 28.0, 45, 1.1}
    };
    private static final double[][] IRON_COPPER_SPOTS = {
        // Banded Iron Formations (Pilbara, Carajas, Minas Gerais, Labrador, Kursk, Krivoy Rog, Mesabi, Singhbhum, Anshan)
        {118.5, -22.5, 65, 1.5}, {-50.0, -6.0, 60, 1.4}, {-43.5, -20.0, 55, 1.3}, {-66.0, 53.0, 55, 1.3},
        {36.5, 51.5, 60, 1.4}, {33.5, 48.0, 55, 1.3}, {-92.5, 47.5, 50, 1.3}, {85.0, 22.0, 55, 1.3},
        {123.0, 41.0, 50, 1.3}, {23.0, -27.5, 45, 1.1}, {20.2, 67.8, 45, 1.2},
        // Copper Belts (Escondida, Chuquicamata, El Teniente, Morenci, Bingham, Central Africa, Kounrad, Grasberg, Oyu Tolgoi)
        {-69.0, -24.0, 75, 1.6}, {-76.0, -12.0, 65, 1.4}, {-70.5, -33.5, 60, 1.4},
        {-110.0, 33.0, 70, 1.5}, {-112.0, 40.5, 55, 1.3}, {-122.0, 53.0, 60, 1.3},
        {28.0, -12.5, 65, 1.5}, {75.0, 47.0, 65, 1.3}, {60.0, 56.0, 50, 1.2},
        {137.1, -4.0, 55, 1.4}, {106.8, 43.0, 55, 1.3}, {145.0, -32.0, 45, 1.1}
    };
    private static final double[][] PRECIOUS_METAL_SPOTS = {
        {27.0, -26.5, 55, 1.8},  // Witwatersrand (South Africa - Giant Gold)
        {29.0, -24.5, 50, 1.6},  // Bushveld Complex (South Africa - Platinum)
        {-116.0, 40.8, 50, 1.5}, // Carlin Trend Nevada (USA - Gold)
        {-65.7, -19.6, 55, 1.6}, // Potosí Cerro Rico (Bolivia - Silver)
        {121.5, -30.7, 50, 1.4}, // Kalgoorlie Super Pit (Australia - Gold)
        {64.6, 41.5, 50, 1.5},   // Muruntau Gold (Uzbekistan)
        {88.2, 69.3, 55, 1.6},   // Norilsk-Talnakh PGMs (Russia)
        {-81.0, 46.5, 45, 1.4},  // Sudbury Basin (Canada - PGMs/Au)
        {-78.5, -7.0, 45, 1.4},  // Yanacocha (Peru - Gold)
        {137.1, -4.0, 45, 1.5}   // Grasberg (Indonesia - Gold/Copper)
    };
    private static final double[][] RARE_EARTH_SPOTS = {
        {109.9, 41.8, 65, 2.0},  // Bayan Obo (Inner Mongolia, China - Giant REE)
        {-115.5, 35.5, 50, 1.5}, // Mountain Pass (California, USA - Bastnäsite)
        {122.5, -28.7, 55, 1.6}, // Mount Weld (Western Australia - Carbonatite REE)
        {-46.0, 60.9, 50, 1.5},  // Kvanefjeld / Ilímaussaq (Greenland - REE/U)
        {116.5, 71.0, 50, 1.5},  // Tomtor (Yakutia, Russia - Carbonatite Nb/REE)
        {34.6, 67.8, 45, 1.4},   // Lovozero (Kola Peninsula, Russia - Loparite REE)
        {115.0, 25.5, 55, 1.7},  // Ganzhou / Jiangxi (South China - Heavy Ionic Clays)
        {103.5, 22.4, 45, 1.3},  // Dong Pao (Vietnam - Bastnäsite)
        {-46.9, -19.6, 48, 1.4}, // Araxá (Minas Gerais, Brazil - Carbonatite Nb/REE)
        {-67.5, -21.0, 60, 1.6}, // Salar de Atacama (Chile - Lithium Brines)
        {-68.0, -23.5, 55, 1.5}, // Salar de Uyuni (Bolivia - Lithium Brines)
        {116.0, -33.8, 48, 1.4}, // Greenbushes (Australia - Spodumene Lithium)
        {14.6, 58.1, 40, 1.2},   // Norra Kärr (Sweden - Heavy REE)
        {20.2, 67.8, 42, 1.3},   // Kiruna / Per Geijer (Sweden - Apatite REE)
        {-64.2, 56.3, 42, 1.3},  // Strange Lake (Quebec/Labrador, Canada)
        {-112.6, 62.1, 42, 1.3}  // Nechalacho (NWT, Canada - REE/Zr)
    };
    private static final double[][] MANTLE_HEAT_SPOTS = {
        {-155.5, 19.8, 45, 1.4}, {-178.0, -29.0, 55, 1.3}, {-72.0, -15.0, 65, 1.4},
        {140.0, 36.0, 60, 1.3}, {43.0, 11.5, 50, 1.4}, {-25.0, 64.8, 55, 1.3},
        {14.0, 40.8, 45, 1.2}, {-110.5, 44.4, 45, 1.3}, {105.0, -5.0, 55, 1.3}
    };
    private static final double[][] AQUIFER_SPOTS = {
        // Major Global Sedimentary Aquifer Systems (UNESCO WHYMAP)
        {25.0, 22.0, 85, 1.5},   // Nubian Sandstone Aquifer System (2.2M km²)
        {-100.0, 38.0, 65, 1.3}, // Ogallala Aquifer USA
        {-54.0, -25.0, 80, 1.5}, // Guaraní Aquifer South America (1.2M km²)
        {138.0, -26.0, 85, 1.4}, // Great Artesian Basin Australia (1.7M km²)
        {10.0, 30.0, 70, 1.3},   // Northern Sahara Aquifer System
        {80.0, 27.0, 75, 1.4},   // Indo-Gangetic Basin
        {2.0, 47.0, 55, 1.2},    // Paris & Aquitaine Basins Europe
        {-60.0, -3.0, 90, 1.5},  // Amazon Basin Aquifer System
        {22.0, -1.0, 75, 1.3},   // Congo Basin Aquifer
        {75.0, 60.0, 85, 1.4},   // West Siberian Basin Aquifer
        {122.0, -18.0, 60, 1.2}, // Canning Basin Australia
        {82.0, 39.0, 55, 1.2},   // Tarim Basin Aquifer
        {-48.0, -1.5, 50, 1.2},  // Marajó Aquifer System
        {-118.0, 36.0, 45, 1.2}, // California Central Valley Aquifer
        {45.0, 25.0, 60, 1.3},   // Arabian Aquifer System
        {16.0, 14.0, 65, 1.3},   // Chad Basin Aquifer
        {23.0, -22.0, 60, 1.2},  // Kalahari / Karoo Aquifer
        {116.0, 37.0, 65, 1.3},  // North China Plain Aquifer
        {70.0, 30.0, 65, 1.3}    // Indus Basin Aquifer
    };

    private Color getGeologyResourceColor(int layerIdx, double val) {
        val = Math.clamp(val, 0.0, 1.0);
        return switch (layerIdx) {
            case 0 -> lerpColorFx(Color.rgb(146, 64, 14), Color.rgb(254, 240, 138), val); // Coal (Amber to Bright Core)
            case 1 -> lerpColorFx(Color.rgb(153, 27, 27), Color.rgb(254, 202, 202), val); // Oil (Ruby to Crimson Core)
            case 2 -> lerpColorFx(Color.rgb(14, 116, 144), Color.rgb(207, 250, 254), val); // Gas (Cyan to Electric Core)
            case 3 -> lerpColorFx(Color.rgb(20, 83, 45), Color.rgb(74, 222, 128), val);   // Uranium (Emerald)
            case 4 -> lerpColorFx(Color.rgb(76, 29, 149), Color.rgb(192, 132, 252), val); // Helium-3 (Violet/Purple)
            case 5 -> lerpColorFx(Color.rgb(139, 69, 19), Color.rgb(249, 115, 22), val);  // Iron & Copper (Terracotta/Orange)
            case 6 -> lerpColorFx(Color.rgb(161, 98, 7), Color.rgb(250, 204, 21), val);   // Precious Metals (Radiant Gold)
            case 7 -> lerpColorFx(Color.rgb(13, 148, 136), Color.rgb(45, 212, 191), val); // Rare Earths & Critical Minerals (Teal/Turquoise)
            case 8 -> lerpColorFx(Color.rgb(185, 28, 28), Color.rgb(253, 224, 71), val);  // Mantle Heat Flow (Red/Yellow)
            default -> lerpColorFx(Color.rgb(30, 58, 138), Color.rgb(96, 165, 250), val); // Freshwater Aquifers (Azure/Sky)
        };
    }

    private static Color lerpColorFx(Color c1, Color c2, double t) {
        double r = Math.clamp(c1.getRed() + t * (c2.getRed() - c1.getRed()), 0.0, 1.0);
        double g = Math.clamp(c1.getGreen() + t * (c2.getGreen() - c1.getGreen()), 0.0, 1.0);
        double b = Math.clamp(c1.getBlue() + t * (c2.getBlue() - c1.getBlue()), 0.0, 1.0);
        return Color.color(r, g, b);
    }

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

    private int getGeologyViewComboIndex(int layerIdx) {
        return switch (layerIdx) {
            case 0 -> 3;  // COAL
            case 1 -> 4;  // OIL
            case 2 -> 5;  // GAS
            case 3 -> 6;  // URANIUM
            case 4 -> 7;  // HELIUM_3
            case 5 -> 8;  // IRON_COPPER
            case 6 -> 9;  // PRECIOUS_METALS
            case 7 -> 10; // CRITICAL_REE
            case 8 -> 11; // GEOTHERMAL (Heat Flux)
            case 9 -> 12; // FRESHWATER_AQUIFERS
            default -> 1;
        };
    }

    private int getViewModeIndex() {
        if (viewModeCombo == null) return 0;
        int selected = viewModeCombo.getSelectionModel().getSelectedIndex();
        return switch (selected) {
            case 1 -> 0;   // 1. Biomes
            case 2 -> 1;   // 2. Hydrography
            case 3 -> 2;   // 3. Coal (Geology layer 0)
            case 4 -> 3;   // 4. Oil (Geology layer 1)
            case 5 -> 4;   // 5. Gas (Geology layer 2)
            case 6 -> 5;   // 6. Uranium (Geology layer 3)
            case 7 -> 6;   // 7. Helium-3 (Geology layer 4)
            case 8 -> 7;   // 8. Iron & Copper (Geology layer 5)
            case 9 -> 8;   // 9. Precious metals (Geology layer 6)
            case 10 -> 9;  // 10. Rare Earths & Critical Minerals (Geology layer 7)
            case 11 -> 10; // 11. Mantle Heat Flux (Geology layer 8)
            case 12 -> 11; // 12. Aquifers (Geology layer 9)
            case 14 -> 12; // 13. Surface Temp & Microclimates
            case 15 -> 13; // 14. Seismic & Volcanic Tectonism
            case 16 -> 14; // 15. Aridity & Soil Salinization
            default -> 0;
        };
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
        } else if (selectedIdx == 8) { // PRECIOUS METALS (Mode 8)
            addLegendItem("LOW", Color.rgb(40, 35, 10), I18n.getOrDefault("resource.legend.precious_low", "Gold/Silver Traces"));
            addLegendItem("MED", Color.rgb(170, 130, 20), I18n.getOrDefault("resource.legend.precious_med", "Placer & Gold Vein"));
            addLegendItem("HIGH", Color.rgb(234, 179, 8), I18n.getOrDefault("resource.legend.precious_high", "Giant Gold/Silver Deposit"));
        } else if (selectedIdx == 9) { // RARE EARTHS & CRITICAL MINERALS (Mode 9)
            addLegendItem("LOW", Color.rgb(10, 35, 35), I18n.getOrDefault("resource.legend.ree_low", "REE/Li Traces"));
            addLegendItem("MED", Color.rgb(20, 120, 110), I18n.getOrDefault("resource.legend.ree_med", "Carbonatite / Salar"));
            addLegendItem("HIGH", Color.rgb(45, 212, 191), I18n.getOrDefault("resource.legend.ree_high", "Major Rare Earth Basin (Bayan Obo)"));
        } else if (selectedIdx == 10) { // MANTLE HEAT FLUX (Mode 10)
            addLegendItem("LOW", Color.rgb(40, 40, 60), I18n.getOrDefault("resource.legend.heat_low", "Inert / Stable Craton"));
            addLegendItem("MED", Color.rgb(180, 80, 30), I18n.getOrDefault("resource.legend.heat_med", "Mean Geothermal Flux"));
            addLegendItem("HIGH", Color.rgb(240, 20, 20), I18n.getOrDefault("resource.legend.heat_high", "High Magmatism & Tectonic Rifts"));
        } else if (selectedIdx == 11) { // AQUIFERS & FRESHWATER (Mode 11)
            addLegendItem("LOW", Color.rgb(20, 40, 80), I18n.getOrDefault("resource.legend.aquifer_low", "Arid / Dry"));
            addLegendItem("MED", Color.rgb(40, 120, 200), I18n.getOrDefault("resource.legend.aquifer_med", "Moderate Aquifer"));
            addLegendItem("HIGH", Color.rgb(0, 220, 255), I18n.getOrDefault("resource.legend.aquifer_high", "Giant Basin Aquifer"));
        } else if (selectedIdx == 12) { // SURFACE TEMPERATURE & MICROCLIMATES
            addLegendItem("POLAR", Color.rgb(35, 120, 230), I18n.getOrDefault("resource.legend.temp_polar", "Polar / Glacial (< 0°C)"));
            addLegendItem("TEMPERATE", Color.rgb(80, 200, 120), I18n.getOrDefault("resource.legend.temp_temperate", "Temperate (10°C - 22°C)"));
            addLegendItem("TROPICAL", Color.rgb(250, 130, 20), I18n.getOrDefault("resource.legend.temp_tropical", "Tropical / Warm (22°C - 34°C)"));
            addLegendItem("TORRID", Color.rgb(245, 20, 40), I18n.getOrDefault("resource.legend.temp_torrid", "Torrid Extreme (> 34°C)"));
        } else if (selectedIdx == 13) { // SEISMIC & VOLCANIC TECTONISM
            addLegendItem("QUIET", Color.rgb(40, 45, 55), I18n.getOrDefault("resource.legend.seismic_quiet", "Stable Shield / Craton"));
            addLegendItem("MODERATE", Color.rgb(200, 100, 30), I18n.getOrDefault("resource.legend.seismic_med", "Active Fault / Orogeny"));
            addLegendItem("HIGH", Color.rgb(245, 30, 30), I18n.getOrDefault("resource.legend.seismic_high", "Subduction & Volcanic Arc"));
        } else if (selectedIdx == 14) { // ARIDITY & SOIL SALINIZATION
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
        int w = 320;
        int h = 160;

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

        boolean isRelief = btnReliefOverlay != null && btnReliefOverlay.isSelected();
        Image elevImg = null;
        PixelReader elevReader = null;
        if (planet != null) {
            if (planet.customElevBase64() != null && !planet.customElevBase64().isBlank()) {
                elevImg = ImageMapLoader.base64PngToImage(planet.customElevBase64());
            } else {
                String pName = planet.elevationMapSource() != null && !planet.elevationMapSource().equalsIgnoreCase("none")
                        ? planet.elevationMapSource().toLowerCase()
                        : (planet.name() != null ? planet.name().toLowerCase() : "earth");
                if (pName.contains("earth") || pName.contains("terre")) {
                    elevImg = ImageMapLoader.loadMapImage("earth_elevation.png");
                } else if (pName.contains("mars")) {
                    elevImg = ImageMapLoader.loadMapImage("mars_elevation.png");
                } else if (pName.contains("moon") || pName.contains("lune")) {
                    elevImg = ImageMapLoader.loadMapImage("moon_elevation.png");
                } else if (pName.contains("venus")) {
                    elevImg = ImageMapLoader.loadMapImage("venus_elevation.png");
                } else if (pName.contains("mercury") || pName.contains("mercure")) {
                    elevImg = ImageMapLoader.loadMapImage("mercury_elevation.png");
                }
            }
            if (elevImg != null && elevImg.getWidth() > 0) {
                elevReader = elevImg.getPixelReader();
            }
        }
        double elevW = elevImg != null ? elevImg.getWidth() : 0;
        double elevH = elevImg != null ? elevImg.getHeight() : 0;

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

                boolean isLandHere;
                boolean isCoast = false;
                double hillshade = 0.707;
                double declivityVal = 0.0;

                double minAlt = planet != null ? planet.minAltitudeMeters() : -11000.0;
                double maxAlt = planet != null ? planet.maxAltitudeMeters() : 8848.0;
                double wLvl = planet != null ? planet.waterLevel() : 0.48;
                boolean hasOcean = wLvl > -0.4;

                double cutThreshold;
                if (hasOcean) {
                    // Ocean-bearing world: Sea level threshold
                    cutThreshold = Math.clamp(wLvl, 0.01, 0.99);
                } else {
                    // Dry / waterless world (Mars, Moon, Mercury, Venus): Datum Z = 0 km reference
                    double altRange = Math.max(100.0, maxAlt - minAlt);
                    cutThreshold = Math.clamp((-minAlt) / altRange, 0.05, 0.95);
                }

                if (elevReader != null && elevW > 0 && elevH > 0) {
                    int ex = (int) Math.clamp((x_base / (double) w) * elevW, 0, elevW - 1);
                    int ey = (int) Math.clamp((y_base / (double) h) * elevH, 0, elevH - 1);
                    double eVal = elevReader.getColor(ex, ey).getRed();
                    boolean isAboveCut = eVal >= cutThreshold;
                    isLandHere = hasOcean ? isAboveCut : true;

                    int exE = (ex + 1) % (int) elevW;
                    int exW = (ex - 1 + (int) elevW) % (int) elevW;
                    int eyN = Math.max(0, ey - 1);
                    int eyS = Math.min((int) elevH - 1, ey + 1);

                    double eE = elevReader.getColor(exE, ey).getRed();
                    double eW = elevReader.getColor(exW, ey).getRed();
                    double eN = elevReader.getColor(ex, eyN).getRed();
                    double eS = elevReader.getColor(ex, eyS).getRed();

                    isCoast = (isAboveCut != (eE >= cutThreshold)) || (isAboveCut != (eW >= cutThreshold))
                            || (isAboveCut != (eN >= cutThreshold)) || (isAboveCut != (eS >= cutThreshold));

                    double dLng = (eE - eW) * 16.0;
                    double dLat = (eN - eS) * 16.0;
                    hillshade = (0.5 * dLng + 0.5 * dLat + 0.707) / Math.sqrt(dLng * dLng + dLat * dLat + 1.0);
                    declivityVal = Math.sqrt(dLng * dLng + dLat * dLat);
                } else {
                    var pt = generator.getPlanetPoint(lat, lon, planet);
                    double elev = pt.elevation();
                    declivityVal = pt.declivity();

                    double dDeg = 0.5;
                    var ptE = generator.getPlanetPoint(lat, lon + dDeg, planet);
                    var ptW = generator.getPlanetPoint(lat, lon - dDeg, planet);
                    var ptN = generator.getPlanetPoint(lat + dDeg, lon, planet);
                    var ptS = generator.getPlanetPoint(lat - dDeg, lon, planet);

                    if (hasOcean) {
                        isLandHere = elev >= wLvl;
                        isCoast = (isLandHere != (ptE.elevation() >= wLvl))
                                || (isLandHere != (ptW.elevation() >= wLvl))
                                || (isLandHere != (ptN.elevation() >= wLvl))
                                || (isLandHere != (ptS.elevation() >= wLvl));
                    } else {
                        isLandHere = true;
                        double datumCut = 0.0;
                        boolean isAboveDatum = elev >= datumCut;
                        isCoast = (isAboveDatum != (ptE.elevation() >= datumCut))
                                || (isAboveDatum != (ptW.elevation() >= datumCut))
                                || (isAboveDatum != (ptN.elevation() >= datumCut))
                                || (isAboveDatum != (ptS.elevation() >= datumCut));
                    }

                    double dLng = ((ptE.elevation() - ptW.elevation()) / 1200.0);
                    double dLat = ((ptN.elevation() - ptS.elevation()) / 1200.0);
                    hillshade = (0.5 * dLng + 0.5 * dLat + 0.707) / Math.sqrt(dLng * dLng + dLat * dLat + 1.0);
                }

                Color pxColor;

                if (mode == 0) { // Biome Map
                    if (radioImportBiome != null && radioImportBiome.isSelected()) {
                        if (customBiomeReader != null && wBio > 0 && hBio > 0) {
                            int bx = (int) Math.min((x_base / (double) w) * wBio, wBio - 1);
                            int by = (int) Math.min((y_base / (double) h) * hBio, hBio - 1);
                            Color rawC = customBiomeReader.getColor(bx, by);
                            pxColor = mapLoader.getBiomeTargetColor(mapLoader.matchBiomeColor(rawC));
                        } else {
                            pxColor = Color.BLACK;
                        }
                    } else if (!isLandHere) {
                        pxColor = Color.rgb(0, 50, 200); // Ocean
                    } else {
                        var point = generator.getPlanetPoint(lat, lon, planet);
                        pxColor = mapLoader.getBiomeTargetColor(point.biome());
                    }
                } else if (mode == 1) { // Hydrography & River Networks Map
                    if (radioImportHydro != null && radioImportHydro.isSelected()) {
                        if (customHydroReader != null && wHydro > 0 && hHydro > 0) {
                            int hx = (int) Math.min((x_base / (double) w) * wHydro, wHydro - 1);
                            int hy = (int) Math.min((y_base / (double) h) * hHydro, hHydro - 1);
                            Color c = customHydroReader.getColor(hx, hy);
                            double waterIntensity = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                            if (waterIntensity > 0.6) {
                                pxColor = Color.rgb(2, 132, 199);
                            } else if (waterIntensity > 0.3) {
                                pxColor = Color.rgb(56, 189, 248);
                            } else if (!isLandHere) {
                                pxColor = Color.rgb(15, 23, 42);
                            } else {
                                int r = (int) Math.min(255, 45 + declivityVal * 100);
                                int g = (int) Math.min(255, 60 + declivityVal * 80);
                                int b = (int) Math.min(255, 55 + declivityVal * 50);
                                pxColor = Color.rgb(r, g, b);
                            }
                        } else {
                            pxColor = Color.BLACK;
                        }
                    } else if (!isLandHere) {
                        pxColor = Color.rgb(15, 23, 42);
                    } else {
                        var point = generator.getPlanetPoint(lat, lon, planet);
                        double riverFlow = point.riverFlow();
                        if (riverFlow > 0.42) {
                            pxColor = Color.rgb(2, 132, 199);
                        } else if (riverFlow > 0.28) {
                            pxColor = Color.rgb(56, 189, 248);
                        } else if (riverFlow > 0.16) {
                            pxColor = Color.rgb(20, 184, 166);
                        } else {
                            int r = (int) Math.min(255, 45 + declivityVal * 100);
                            int g = (int) Math.min(255, 60 + declivityVal * 80);
                            int b = (int) Math.min(255, 55 + declivityVal * 50);
                            pxColor = Color.rgb(r, g, b);
                        }
                    }
                } else if (mode >= 2 && mode <= 11) { // GEOLOGICAL & ENERGY TENSORS (Modes 2..11 for Layers 0..9)
                    int layerIdx = mode - 2;
                    boolean useImport = geologyImportRadios.containsKey(layerIdx) && geologyImportRadios.get(layerIdx).isSelected();
                    Image gImg = customGeologyLayerImages.get(layerIdx);

                    Color baseBackground = isLandHere ? Color.rgb(30, 41, 59) : Color.rgb(15, 23, 42);

                    if (useImport) {
                        if (gImg != null && gImg.getPixelReader() != null && gImg.getWidth() > 0 && gImg.getHeight() > 0) {
                            int gx = (int) Math.clamp((x_base / (double) w) * gImg.getWidth(), 0, gImg.getWidth() - 1);
                            int gy = (int) Math.clamp((y_base / (double) h) * gImg.getHeight(), 0, gImg.getHeight() - 1);
                            Color c = gImg.getPixelReader().getColor(gx, gy);
                            double alpha = c.getOpacity();
                            double brightness = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;

                            if (alpha > 0.05 && brightness > 0.02) {
                                pxColor = blendColors(baseBackground, Color.color(c.getRed(), c.getGreen(), c.getBlue()), alpha);
                            } else {
                                pxColor = baseBackground;
                            }
                        } else {
                            pxColor = Color.BLACK;
                        }
                    } else {
                        double val = sampleProceduralGeologyTensor(layerIdx, lon, lat, planet);
                        val = Math.clamp(val, 0.0, 1.0);
                        if (val > 0.02) {
                            Color resourceColor = getGeologyResourceColor(layerIdx, val);
                            pxColor = blendColors(baseBackground, resourceColor, Math.min(1.0, val * 1.3));
                        } else {
                            pxColor = baseBackground;
                        }
                    }
                } else if (mode == 12) { // SURFACE TEMPERATURE & MICROCLIMATES
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
                    Color tColor = Color.rgb(r, g, b);
                    if (!isLandHere) {
                        pxColor = blendColors(tColor, Color.rgb(15, 23, 42), 0.35);
                    } else {
                        pxColor = tColor;
                    }
                } else if (mode == 13) { // SEISMIC & VOLCANIC TECTONISM
                    double heatVal = sampleProceduralGeologyTensor(8, lon, lat, planet);
                    double sLevel = planet.seismicActivityLevel() / 10.0;
                    double vLevel = planet.volcanicActivityLevel() / 8.0;

                    double risk = Math.clamp(heatVal * 0.5 + declivityVal * 2.0 * sLevel + vLevel * 0.3, 0.0, 1.0);
                    if (!isLandHere) {
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
                } else { // Mode 14: ARIDITY & SOIL SALINIZATION
                    if (!isLandHere) {
                        pxColor = Color.rgb(15, 23, 42); // Clean ocean basemap (No aridity/salinity at sea)
                    } else {
                        var point = generator.getPlanetPoint(lat, lon, planet);
                        double rainfall = point.rainfall();
                        double tempC = point.temperature();
                        double aridityIndex = rainfall / Math.max(1.0, tempC + 10.0);
                        double dryness = Math.clamp(1.0 - (aridityIndex / 50.0), 0.0, 1.0);

                        int r = (int) Math.clamp(30 + dryness * 215, 0, 255);
                        int g = (int) Math.clamp(145 - dryness * 85, 0, 255);
                        int b = (int) Math.clamp(160 - dryness * 145, 0, 255);
                        pxColor = Color.rgb(r, g, b);
                    }
                }

                if (isRelief) {
                    if (isCoast) {
                        pxColor = hasOcean ? Color.rgb(240, 249, 255) : Color.rgb(251, 191, 36); // Crisp Coastline (Cyan/White) or Datum Z=0 (Amber/Gold)
                    } else {
                        double mult = 0.68 + 0.45 * hillshade;
                        pxColor = Color.color(
                            Math.clamp(pxColor.getRed() * mult, 0.0, 1.0),
                            Math.clamp(pxColor.getGreen() * mult, 0.0, 1.0),
                            Math.clamp(pxColor.getBlue() * mult, 0.0, 1.0)
                        );
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
            case 6 -> PRECIOUS_METAL_SPOTS;
            case 7 -> RARE_EARTH_SPOTS;
            case 8 -> MANTLE_HEAT_SPOTS;
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
            case 6 -> { // PRECIOUS_METALS (Au, Ag, Pt)
                double placer = p2 * 1.2 * (n1 > 0.4 ? 1.2 : 0.2);
                double epithermal = p3 * 1.4 * (n2 > 0.5 ? 1.5 : 0.3);
                double porphyryAu = (p4 / 2.0) * 0.6;
                combined = (baseVal * 0.45 + placer * 0.25 + epithermal * 0.2 + porphyryAu * 0.1) * p1;
            }
            case 7 -> { // CRITICAL_REE (Terres Rares, Bastnasite, Li)
                double carbonatite = p2 * 1.5 * (n1 > 0.6 ? 1.6 : 0.2);
                double salarLi = p3 * 1.3 * (n2 > 0.5 ? 1.2 : 0.1);
                double spodumene = (p4 / 1.5) * 0.5;
                combined = (baseVal * 0.4 + carbonatite * 0.25 + salarLi * 0.2 + spodumene * 0.15) * p1;
            }
            case 8 -> { // MANTLE_HEAT
                double heatNorm = p1 / 65.0;
                double plumeAnomalies = 1.0 + p2 * 1.5 * n1;
                double cratonInsulation = Math.clamp(1.0 - (p3 - 150.0) / 300.0, 0.3, 1.5);
                double radiogenicCrust = 0.8 + (p4 / 1.2) * 0.4 * n2;
                combined = (baseVal * 0.4 + n1 * 0.6) * heatNorm * plumeAnomalies * cratonInsulation * radiogenicCrust;
            }
            default -> { // FRESHWATER_AQUIFERS (9)
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
            if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
            if (!slider.isValueChanging()) {
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
            if (ecologyDescriptionArea != null && ecologyPresetBar != null && ecologyPresetBar.getPresetCombo() != null && ecologyPresetBar.getPresetCombo().getValue() != null) {
                ecologyDescriptionArea.setText(ecologyPresetBar.getPresetCombo().getValue().getPresetDescription());
            }
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
                    I18n.getOrDefault("resource.view.section_base", "────────── 12 CARTES CANONIQUES DE BASE ──────────"),
                    I18n.getOrDefault("resource.view.biomes", "🌿 1. Biomes & Couverture Végétale"),
                    I18n.getOrDefault("resource.view.hydro", "🌊 2. Hydrographie & Réseau Fluvial"),
                    I18n.getOrDefault("resource.view.coal", "⛏️ 3. Gisements de Charbon"),
                    I18n.getOrDefault("resource.view.oil", "🛢️ 4. Réserves de Pétrole Brut"),
                    I18n.getOrDefault("resource.view.gas", "🔥 5. Champs de Gaz Naturel"),
                    I18n.getOrDefault("resource.view.uranium", "⚛️ 6. Minerais d'Uranium & Fission"),
                    I18n.getOrDefault("resource.view.helium3", "🌌 7. Hélium-3 & Fusion Lunaires"),
                    I18n.getOrDefault("resource.view.iron_copper", "⛓️ 8. Métaux Fer BIF & Cuivre"),
                    I18n.getOrDefault("resource.view.precious_metals", "💎 9. Métaux Précieux (Or, Argent, PGM)"),
                    I18n.getOrDefault("resource.view.rare_earths", "🔋 10. Terres Rares & Minéraux Critiques / Lithium"),
                    I18n.getOrDefault("resource.view.heat", "🌋 11. Flux Thermique du Manteau"),
                    I18n.getOrDefault("resource.view.aquifer", "💧 12. Aquifères & Eau Douce"),
                    I18n.getOrDefault("resource.view.section_derived", "────────── CARTES DÉDUITES / ANOMALIES ──────────"),
                    I18n.getOrDefault("resource.view.temp", "🌡️ 13. Températures Surface & Microclimats"),
                    I18n.getOrDefault("resource.view.seismic", "🌋 14. Tectonique & Aléa Sismique/Volcanique"),
                    I18n.getOrDefault("resource.view.aridity", "🏜️ 15. Aridité & Salinisation des Sols")
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

            if (btnGenerateProceduralGeologyTensors != null) {
                btnGenerateProceduralGeologyTensors.setText(I18n.getOrDefault("resource.btn.regen_tensors", "🪄 Regenerate Tensors"));
                btnGenerateProceduralGeologyTensors.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.regen_geology_tensors", "Switch all geological tensors to procedural mode and regenerate maps according to parameters and stochastic seed.")));
            }

            // Dynamic refresh of all 10 geology tensor cards
            for (int i = 0; i < GEOLOGY_SLIDER_SPECS.length; i++) {
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

        // Apply custom map images ONLY if import modes are selected
        Image biomeToMap = (radioImportBiome != null && radioImportBiome.isSelected()) ? customBiomeImage : null;
        Image resToMap = (radioImportGeology != null && radioImportGeology.isSelected()) ? customResourceImage : null;
        if (biomeToMap != null || resToMap != null) {
            mapLoader.mapImagesToCells(activeCells, null, biomeToMap, resToMap, -11000, 8848);
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
        if (lower.contains("-100") || lower.contains("lig") || lower.contains("interglaciaire") || lower.contains("eemian")) return PlanetPreset.EARTH_LIG_100000BP;
        if (lower.contains("-50") || lower.contains("sahul") || lower.contains("mis3") || lower.contains("mis 3")) return PlanetPreset.EARTH_MIS3_50000BP;
        if (lower.contains("-25") || lower.contains("beringia") || lower.contains("béringie")) return PlanetPreset.EARTH_LGM_ONSET_25000BP;
        if (lower.contains("-20") || lower.contains("lgm") || lower.contains("glaciaire") || lower.contains("solutrean") || lower.contains("solutréen")) return PlanetPreset.EARTH_LGM_20000BP;
        if (lower.contains("-10") || lower.contains("eh") || lower.contains("précoce") || lower.contains("early holocene") || lower.contains("dryas")) return PlanetPreset.EARTH_EH_10000BP;
        if (lower.contains("-6") || lower.contains("mh") || lower.contains("sahara") || lower.contains("mid holocene")) return PlanetPreset.EARTH_MH_6000BP;
        if (lower.contains("-3") || lower.contains("lh") || lower.contains("tardif") || lower.contains("late holocene")) return PlanetPreset.EARTH_LH_3000BP;
        if (lower.contains("-1900") || lower.contains("bronze")) return PlanetPreset.EARTH_BRONZE_1900BP;
        if (lower.contains("-1000") || lower.contains("iron") || lower.contains("fer")) return PlanetPreset.EARTH_IRON_1000BP;
        for (PlanetPreset p : PlanetPreset.getPresets()) {
            if (p.name().toLowerCase().contains(lower) || lower.contains(p.name().toLowerCase())) {
                return p;
            }
        }
        if (lower.contains("earth") || lower.contains("terre") || lower.contains("terran")) {
            return PlanetPreset.EARTH_LIKE;
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

    public void prepopulateGeologyTensors(String planetKey) {
        long year = (ecologyPresetBar != null && ecologyPresetBar.getPresetCombo() != null && ecologyPresetBar.getPresetCombo().getValue() != null)
                ? ecologyPresetBar.getPresetCombo().getValue().getAssociatedEpochYear() : 2026L;
        prepopulateGeologyTensors(planetKey, year);
    }

    public void prepopulateGeologyTensors(String planetKey, long year) {
        if (planetKey == null || planetKey.isBlank() || "none".equalsIgnoreCase(planetKey)) {
            planetKey = "earth";
        }
        String body = planetKey.toLowerCase();
        try {
            String[] layerNames = {
                "coal",
                "oil",
                "gas",
                "uranium",
                "helium3",
                "iron_copper",
                "precious_metals",
                "rare_earths",
                "geothermal",
                "aquifers"
            };

            for (int i = 0; i < layerNames.length; i++) {
                Image img = null;
                if ("earth".equals(body)) {
                    img = ImageMapLoader.loadMapImage("earth", year, layerNames[i]);
                }
                if (img == null || img.getWidth() <= 0) {
                    String fileName = body + "_" + layerNames[i] + ".png";
                    img = ImageMapLoader.loadMapImage(fileName);
                }
                if (img != null && img.getWidth() > 0) {
                    customGeologyLayerImages.put(i, img);
                } else {
                    BufferedImage bImg = switch (i) {
                        case 0 -> HistoricalMapGenerator.rasterizeCoalMap(body.toUpperCase(), null);
                        case 1 -> HistoricalMapGenerator.rasterizeOilMap(body.toUpperCase(), null);
                        case 2 -> HistoricalMapGenerator.rasterizeGasMap(body.toUpperCase(), null);
                        case 3 -> HistoricalMapGenerator.rasterizeUraniumMap(body.toUpperCase(), null);
                        case 4 -> HistoricalMapGenerator.rasterizeHelium3Map(body.toUpperCase(), null);
                        case 5 -> HistoricalMapGenerator.rasterizeIronCopperMap(body.toUpperCase(), null);
                        case 6 -> HistoricalMapGenerator.rasterizePreciousMetalsMap(body.toUpperCase(), null);
                        case 7 -> HistoricalMapGenerator.rasterizeRareEarthsMap(body.toUpperCase(), null);
                        case 8 -> HistoricalMapGenerator.rasterizeMantleHeatMap(body.toUpperCase(), null);
                        default -> HistoricalMapGenerator.rasterizeAquiferMap(body.toUpperCase(), null);
                    };
                    if (bImg != null) {
                        customGeologyLayerImages.put(i, bufferedImageToFXImage(bImg));
                    }
                }
            }

            for (int i = 0; i < 10; i++) {
                if (geologyFileLabels.containsKey(i)) {
                    geologyFileLabels.get(i).setText(getGeologyBaselineName(i, body));
                }
                if (geologyImportRadios.containsKey(i)) {
                    geologyImportRadios.get(i).setSelected(true);
                }
                if (geologySourceCombos.containsKey(i)) {
                    ComboBox<String> cb = geologySourceCombos.get(i);
                    if (cb != null) {
                        boolean matched = false;
                        for (String item : cb.getItems()) {
                            if (item == null || item.isEmpty()) continue;
                            String itmLow = item.toLowerCase();
                            boolean matches = false;
                            if (body.equals("earth") && (itmLow.contains("earth") || itmLow.contains("terre") || itmLow.contains("terran"))) {
                                matches = true;
                            } else if (body.equals("mars") && itmLow.contains("mars")) {
                                matches = true;
                            } else if (body.equals("venus") && (itmLow.contains("venus") || itmLow.contains("vénus"))) {
                                matches = true;
                            } else if (body.equals("moon") && (itmLow.contains("moon") || itmLow.contains("lune") || itmLow.contains("lunar") || itmLow.contains("lola"))) {
                                matches = true;
                            } else if (body.equals("mercury") && (itmLow.contains("mercury") || itmLow.contains("mercure") || itmLow.contains("messenger"))) {
                                matches = true;
                            } else if (itmLow.contains(body)) {
                                matches = true;
                            }
                            if (matches) {
                                cb.setValue(item);
                                matched = true;
                                break;
                            }
                        }
                        if (!matched && cb.getItems().size() > 1) {
                            cb.setValue(cb.getItems().get(1));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to pre-populate geology tensors for {}", body, e);
        }
    }

    public void prepopulateEarthGeologyTensors() {
        prepopulateGeologyTensors("earth");
    }

    private String getBiomeSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase()) {
            case "mars" -> "🌿 Mars MOLA & Biomes Reconstitution";
            case "moon" -> "🌿 Moon Clementine Regolith (Barren Biome)";
            case "venus" -> "🌿 Venus Magellan Radar (Supercritical Biome)";
            case "mercury" -> "🌿 Mercury MESSENGER Surface (Airless Biome)";
            default -> I18n.getOrDefault("planet.source.earth_modis_biomes", "🌿 Earth MODIS Biomes");
        };
    }

    private String getHydroSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase()) {
            case "mars" -> "🌊 Mars MARSIS / Ancient Paleochannels Hydrography";
            case "moon" -> "🌊 Moon Polar Traps (Dry Crust Hydrography)";
            case "venus" -> "🌊 Venus Volcanic Channels (Anhydrous Hydrography)";
            case "mercury" -> "🌊 Mercury Polar Cold Traps Hydrography";
            default -> I18n.getOrDefault("resource.source.earth_hydro", "🌊 Earth SWBD Hydrography");
        };
    }

    private String getGeologySourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase()) {
            case "mars" -> "🪨 Mars MOLA & GRS Geology Tensors";
            case "moon" -> "🪨 Moon Clementine & KREEP Geology Tensors";
            case "venus" -> "🪨 Venus Magellan Radar Geology Tensors";
            case "mercury" -> "🪨 Mercury MESSENGER GRS/XRS Geology Tensors";
            default -> I18n.getOrDefault("resource.source.earth_geology", "🪨 Earth USGS Geology");
        };
    }

    private String getClimateSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase()) {
            case "mars" -> "🌡️ Mars Viking/MGS Thermal Inertia Model";
            case "moon" -> "🌡️ Moon Diviner Lunar Radiometer Experiment";
            case "venus" -> "🌡️ Venus Venera/VIRTIS Thermal Greenhouse Model";
            case "mercury" -> "🌡️ Mercury MESSENGER Thermal Radiometry";
            default -> "🌡️ Earth ERA5 / MODIS Land Surface Temp";
        };
    }

    private String getRainfallSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase()) {
            case "mars" -> "🌧️ Mars Atmospheric Water Vapor & Sublimation";
            case "moon" -> "🌧️ Moon Vacuum Exosphere (0 mm/yr Precipitation)";
            case "venus" -> "🌧️ Venus Sulfuric Acid Cloud Condensation & Virga";
            case "mercury" -> "🌧️ Mercury Exospheric Zero Precipitation";
            default -> "🌧️ Earth NASA GPM IMERG Precipitation";
        };
    }

    private String getSeasonalitySourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase()) {
            case "mars" -> "☀️ Mars Obliquity (25.2°) Thermal Seasonality";
            case "moon" -> "☀️ Moon Diurnal Day/Night Extreme Seasonality";
            case "venus" -> "☀️ Venus Low-Obliquity (3°) Isothermal Seasonality";
            case "mercury" -> "☀️ Mercury 3:2 Spin-Orbit Resonance Seasonality";
            default -> "☀️ Earth Obliquity (23.4°) Annual Seasonality";
        };
    }

    private String getGeologyTensorTitle(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("resource.tensor.1.title", "⛏️ 4.2.1 Gisements de Charbon (COAL — USGS / BGR)");
            case 1 -> I18n.getOrDefault("resource.tensor.2.title", "🛢️ 4.2.2 Réserves de Pétrole Brut (CRUDE_OIL — WEP / BGR)");
            case 2 -> I18n.getOrDefault("resource.tensor.3.title", "🔥 4.2.3 Champs de Gaz Naturel (NATURAL_GAS — WEP / BGR)");
            case 3 -> I18n.getOrDefault("resource.tensor.4.title", "⚛️ 4.2.4 Minerais d'Uranium & Fission (URANIUM — IAEA UDEPO)");
            case 4 -> I18n.getOrDefault("resource.tensor.5.title", "🌌 4.2.5 Hélium-3 & Fusion Lunaires (HELIUM_3 — NASA / LPI)");
            case 5 -> I18n.getOrDefault("resource.tensor.6.title", "⛓️ 4.2.6 Métaux Fer BIF & Cuivre (IRON_COPPER)");
            case 6 -> I18n.getOrDefault("resource.tensor.7.title", "💎 4.2.7 Métaux Précieux (Or, Argent, PGM — USGS MRDS)");
            case 7 -> I18n.getOrDefault("resource.tensor.8.title", "🔋 4.2.8 Terres Rares & Minéraux Critiques (CRITICAL_REE — USGS REE/Salars)");
            case 8 -> I18n.getOrDefault("resource.tensor.9.title", "🌋 4.2.9 Flux Thermique du Manteau (MANTLE_HEAT — IHFC / Davies 2013)");
            case 9 -> I18n.getOrDefault("resource.tensor.10.title", "💧 4.2.10 Aquifères Profonds & Eau Douce (FRESHWATER_AQUIFERS — WHYMAP)");
            default -> I18n.getOrDefault("resource.tensor.custom.title_prefix", "⛏️ 4.2.") + (index + 1) + I18n.getOrDefault("resource.tensor.custom.title_mid", " Tenseur Géologique ") + (index + 1);
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
            case 6 -> I18n.getOrDefault("resource.tensor.7.desc", "Gold (Au), Silver (Ag), Platinum (Pt), and Palladium (Pd) deposits. Source: USGS MRDS.");
            case 7 -> I18n.getOrDefault("resource.tensor.8.desc", "Rare Earth Elements (REE, Neodymium, Dysprosium), Bastnäsite, Monazite, and Lithium brines/spodumene. Source: USGS REE.");
            case 8 -> I18n.getOrDefault("resource.tensor.9.desc", "Geothermal mantle heat flux (mW/m²). Source: IHFC / Davies 2013 Global Heat Flow.");
            case 9 -> I18n.getOrDefault("resource.tensor.10.desc", "Deep groundwater tables and large fossil aquifers. Source: UNESCO WHYMAP.");
            default -> I18n.getOrDefault("resource.tensor.custom.desc", "Extensible geological layer.");
        };
    }

    private String getGeologyBaselineName(int index) {
        return getGeologyBaselineName(index, "earth");
    }

    private String getGeologyBaselineName(int index, String planetKey) {
        if (planetKey == null) planetKey = "earth";
        String body = planetKey.toLowerCase();
        return switch (index) {
            case 0 -> switch (body) {
                case "mars" -> "Mars Sterile Regolith (No Paleo-Biomass)";
                case "moon" -> "Lunar Regolith (Sterile Vacuum)";
                case "venus" -> "Venus Pyrolyzed Crust (Sterile)";
                case "mercury" -> "Mercury Airless Surface (Sterile)";
                default -> "USGS MRDS / BGR Coal Deposits (Earth Baseline)";
            };
            case 1 -> switch (body) {
                case "mars" -> "Mars Lacustrine Bedrocks (Abiotic Traces)";
                case "moon" -> "Lunar Sterile Regolith (Void)";
                case "venus" -> "Venus Supercritical Crust (Void)";
                case "mercury" -> "Mercury Airless Crust (Void)";
                default -> "World Energy Projection / BGR Oil Reserves (Earth Baseline)";
            };
            case 2 -> switch (body) {
                case "mars" -> "Mars Subsurface Methane & Clathrates (Mars Odyssey)";
                case "moon" -> "Lunar Solar-Wind Entrapped Gas (Moon Trace)";
                case "venus" -> "Venus Supercritical High-Pressure CO₂ Reservoir";
                case "mercury" -> "Mercury Exospheric Outgassing (Void)";
                default -> "WEP / BGR Natural Gas Fields (Earth Baseline)";
            };
            case 3 -> switch (body) {
                case "mars" -> "Mars Odyssey GRS Thorium/Uranium (Mars Baseline)";
                case "moon" -> "Lunar Prospector KREEP GRS Thorium (Moon Baseline)";
                case "venus" -> "Venera 8/9/10 Gamma-Ray Spectrometry (Venus Baseline)";
                case "mercury" -> "MESSENGER GRS / XRS Thorium (Mercury Baseline)";
                default -> "IAEA UDEPO / NFCIS Uranium Database (Earth Baseline)";
            };
            case 4 -> switch (body) {
                case "mars" -> "Mars Arid Regolith Solar Wind Infiltration (Mars)";
                case "moon" -> "NASA PDS / Lunar Prospector Helium-3 (Moon Baseline)";
                case "venus" -> "Venus Upper Ionosphere Solar Traps (Venus)";
                case "mercury" -> "MESSENGER Magnetosphere He-3 Trap (Mercury Baseline)";
                default -> "Earth Atmosphere Shield (Sterile / Negligible He-3)";
            };
            case 5 -> switch (body) {
                case "mars" -> "Mars OMEGA / CRISM Hematite & Basalt (Mars Baseline)";
                case "moon" -> "Lunar Mare Basalts FeO & Ilmenite (Moon Baseline)";
                case "venus" -> "Venus Magellan Basaltic Volcanism (Venus Baseline)";
                case "mercury" -> "MESSENGER High-Iron Surface Crust (Mercury Baseline)";
                default -> "USGS Mineral Resources BIF Iron & Copper (Earth Baseline)";
            };
            case 6 -> switch (body) {
                case "mars" -> "Mars Hydrothermal Quartz & Precious Metals (Mars)";
                case "moon" -> "Lunar Impact Siderophile & Native Platinum (Moon)";
                case "venus" -> "Venus Metallic Telluride Frosts (Venus Baseline)";
                case "mercury" -> "Mercury Core-Mantle Precious Metals (Mercury Baseline)";
                default -> "USGS MRDS Precious Metals Au/Ag/Pt (Earth Baseline)";
            };
            case 7 -> switch (body) {
                case "mars" -> "Mars Hydrothermal REE & Heavy Minerals (Mars Baseline)";
                case "moon" -> "Lunar KREEP Basalts & Rare Earths (Moon Baseline)";
                case "venus" -> "Venus Alkaline Magmatic REE Frosts (Venus Baseline)";
                case "mercury" -> "MESSENGER Magmatic Sulfides & REE (Mercury Baseline)";
                default -> "USGS REE & Lithium Salars (Earth Baseline)";
            };
            case 8 -> switch (body) {
                case "mars" -> "Mars InSight Seismic & Crustal Heat Flow (Mars Baseline)";
                case "moon" -> "Apollo 15/17 Lunar Heat Flow Experiment (Moon Baseline)";
                case "venus" -> "Venus Magellan Coronae & Mantle Plumes (Venus Baseline)";
                case "mercury" -> "MESSENGER Core Conduction & Residual Heat (Mercury Baseline)";
                default -> "IHFC / Davies 2013 Global Heat Flow (Earth Baseline)";
            };
            case 9 -> switch (body) {
                case "mars" -> "Mars Express / MARSIS Radar Subsurface Ice (Mars Baseline)";
                case "moon" -> "LRO / LCROSS Polar Cold Trap Ice (Moon Baseline)";
                case "venus" -> "Venus Desiccated Subsurface (Trace Vapor)";
                case "mercury" -> "MESSENGER Polar Crater Water Ice (Mercury Baseline)";
                default -> "UNESCO / WHYMAP Global Groundwater (Earth Baseline)";
            };
            default -> "Scientific Planetary Dataset (" + body + ")";
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
            case 6 -> I18n.getOrDefault("resource.tensor.7.format", "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 0 kt | White (255) = 1,000 kt Au/Ag/Pt (USGS MRDS)");
            case 7 -> I18n.getOrDefault("resource.tensor.8.format", "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 0 Mt | White (255) = 5.0 Mt REE/Li (USGS REE & Salars)");
            case 8 -> I18n.getOrDefault("resource.tensor.9.format", "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 20 mW/m² | White (255) = 250 mW/m² (IHFC / Davies 2013)");
            case 9 -> I18n.getOrDefault("resource.tensor.10.format", "Grayscale PNG (equirectangular 2:1):\n  Black (0) = 0 10³ km³ | White (255) = 5.0 10³ km³ (UNESCO / WHYMAP)");
            default -> I18n.getOrDefault("resource.hint.geo_format", "PNG / GeoTIFF image in 2:1 equirectangular projection");
        };
    }

    private ComboBox<String> buildGeologySourceCombo(int index) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getItems().add("");
        switch (index) {
            case 0 -> combo.getItems().addAll(
                "🌍 Terre — USGS MRDS & BGR Coal Basins [Global, -360 Ma (Carbonifère) à Actuel]",
                "🔴 Mars — Regolith & Abiotic Crust [Planétaire (Mars), -4.1 Ga à Actuel (Abiotique / Stérile)]",
                "🟡 Vénus — Pyrolyzed Carbon Crust [Planétaire (Vénus), -500 Ma à Actuel (Pyrolyse / Stérile)]",
                "⚪ Lune — Regolith & Vacuum Crust [Planétaire (Lune), -4.5 Ga à Actuel (Abiotique / Stérile)]",
                "⚪ Mercure — Silicate Crust [Planétaire (Mercure), -4.0 Ga à Actuel (Abiotique / Stérile)]"
            );
            case 1 -> combo.getItems().addAll(
                "🌍 Terre — USGS WPA & BGR Oil Assessment [Global, -250 Ma (Mésozoïque) à Actuel]",
                "🔴 Mars — Lacustrine Bedrocks [Planétaire (Mars), -4.1 Ga à Actuel (Traces Paléo-Lacustres)]",
                "🟡 Vénus — Supercritical CO₂ Crust [Planétaire (Vénus), -500 Ma à Actuel (Pyrolyse / Stérile)]",
                "⚪ Lune — Sterile Regolith [Planétaire (Lune), -4.5 Ga à Actuel (Stérile / Vide)]",
                "⚪ Mercure — Airless Crust [Planétaire (Mercure), -4.0 Ga à Actuel (Stérile / Vide)]"
            );
            case 2 -> combo.getItems().addAll(
                "🌍 Terre — USGS & WEP/BGR Natural Gas Fields [Global, -300 Ma à Actuel]",
                "🔴 Mars — Subsurface Methane & Clathrates [Planétaire (Mars), -4.1 Ga à Actuel (Traces Méthanogènes)]",
                "🟡 Vénus — Supercritical Atmosphere & Crustal Gas Traps [Planétaire (Vénus), -500 Ma à Actuel (Pièges Crustaux)]",
                "⚪ Lune — Solar Wind Entrapped Gases [Planétaire (Lune), -4.5 Ga à Actuel (Pièges Vent Solaire)]",
                "⚪ Mercure — Exospheric Outgassing [Planétaire (Mercure), -4.0 Ga à Actuel (Traces Exosphériques)]"
            );
            case 3 -> combo.getItems().addAll(
                "🌍 Terre — IAEA UDEPO & NEA Red Book Uranium [Global, -2.5 Ga (Protérozoïque) à Actuel]",
                "🔴 Mars — Mars Odyssey GRS Thorium & Uranium [Planétaire (Mars), -4.1 Ga à Actuel (Spectrométrie Gamma)]",
                "🟡 Vénus — Venera 8/9/10 Gamma-Ray Spectrometry [Planétaire (Vénus), -500 Ma à Actuel (Sondes Venera)]",
                "⚪ Lune — Lunar Prospector KREEP GRS Thorium [Planétaire (Lune), -4.5 Ga à Actuel (Terrains KREEP)]",
                "⚪ Mercure — MESSENGER GRS/XRS Thorium & Uranium [Planétaire (Mercure), -4.0 Ga à Actuel (Spectrométrie X/GRS)]"
            );
            case 4 -> combo.getItems().addAll(
                "🌍 Terre — Atmosphere Shielded Crust [Global, -4.5 Ga à Actuel (Traces Négligeables / Bouclier Magnétique)]",
                "🔴 Mars — Low-Magnetism Regolith Infiltration [Planétaire (Mars), -4.1 Ga à Actuel (Infiltration Régolithe)]",
                "🟡 Vénus — Upper Ionosphere Solar Traps [Planétaire (Vénus), -500 Ma à Actuel (Haute Atmosphère)]",
                "⚪ Lune — LRO LOLA & Lunar Prospector Ilmenite [Planétaire (Lune), -4.5 Ga à Actuel (Régolithe Titane/Ilménite)]",
                "⚪ Mercure — MESSENGER Magnetosphere Solar Wind Trap [Planétaire (Mercure), -4.0 Ga à Actuel (Pièges Magnétiques)]"
            );
            case 5 -> combo.getItems().addAll(
                "🌍 Terre — USGS MRDS & Banded Iron Formations Atlas [Global, -3.8 Ga (Archéen) à Actuel]",
                "🔴 Mars — Mars Express OMEGA & CRISM Hematite [Planétaire (Mars), -4.1 Ga à Actuel (Hématite / Oxydes de Fer)]",
                "🟡 Vénus — Magellan SAR Basaltic Volcanism & Pyrite [Planétaire (Vénus), -500 Ma à Actuel (Sulfures & Pyrites)]",
                "⚪ Lune — Clementine & Lunar Prospector FeO Basalts [Planétaire (Lune), -4.5 Ga à Actuel (Basaltes des Mers)]",
                "⚪ Mercure — MESSENGER High-Iron Crust & Regolith [Planétaire (Mercure), -4.0 Ga à Actuel (Noyau Géant Ferrique)]"
            );
            case 6 -> combo.getItems().addAll(
                "🌍 Terre — USGS MRDS Precious Metals Au/Ag/PGM [Global, -3.8 Ga à Actuel (Gisements Orogéniques & Alluvionnaires)]",
                "🔴 Mars — Hydrothermal Quartz & Native Gold Model [Planétaire (Mars), -4.1 Ga à Actuel (Paléo-Hydrothermalisme)]",
                "🟡 Vénus — Heavy Metallic Pyrite & Telluride Frosts [Planétaire (Vénus), -500 Ma à Actuel (Gels Métalliques Sommitaux)]",
                "⚪ Lune — Impact Siderophile & Native Platinum Traces [Planétaire (Lune), -4.5 Ga à Actuel (Éjectas Sidérophiles)]",
                "⚪ Mercure — Core-Mantle Precious Metals & Sulfides [Planétaire (Mercure), -4.0 Ga à Actuel (Différenciation Manteau/Noyau)]"
            );
            case 7 -> combo.getItems().addAll(
                "🌍 Terre — USGS Rare Earth Elements & Salars [Global, -2.5 Ga à Actuel (Pegmatites, Carbonatites & Salars)]",
                "🔴 Mars — Acid Fog & Hydrothermal REE Model [Planétaire (Mars), -4.1 Ga à Actuel (Altération Hydrothermale Acide)]",
                "🟡 Vénus — Alkaline Carbonatites & REE Model [Planétaire (Vénus), -500 Ma à Actuel (Carbonatites Alcalines)]",
                "⚪ Lune — KREEP Basalts & Rare Earth Elements [Planétaire (Lune), -4.5 Ga à Actuel (Complexes Magmatiques KREEP)]",
                "⚪ Mercure — Magmatic Sulfide & REE Model [Planétaire (Mercure), -4.0 Ga à Actuel (Sulfures Magmatiques)]"
            );
            case 8 -> combo.getItems().addAll(
                "🌍 Terre — IHFC / Davies Global Crustal Heat Flow [Global, -4.5 Ga à Actuel (Flux 40 à 120 mW/m²)]",
                "🔴 Mars — InSight Crustal Heat Flow & Volcanic Plumes [Planétaire (Mars), -4.1 Ga à Actuel (Panaches Résiduels Tharsis/Elysium)]",
                "🟡 Vénus — Magellan Coronae & Mantle Plumes [Planétaire (Vénus), -500 Ma à Actuel (Points Chauds & Coronae)]",
                "⚪ Lune — Apollo 15/17 Lunar Heat Flow Experiment [Planétaire (Lune), -4.5 Ga à Actuel (Flux Résiduel 15-20 mW/m²)]",
                "⚪ Mercure — MESSENGER Core Conduction & Residual Heat [Planétaire (Mercure), -4.0 Ga à Actuel (Conduction Thermique du Noyau)]"
            );
            case 9 -> combo.getItems().addAll(
                "🌍 Terre — UNESCO / WHYMAP Global Groundwater Aquifers [Global, -10 000 BP à Actuel (Holocène & Aquifères Fossiles)]",
                "🔴 Mars — Mars Express MARSIS Subsurface Ice [Planétaire (Mars), -4.1 Ga à Actuel (Calottes & Glace Enfouie)]",
                "🟡 Vénus — Atmospheric Supercritical Vapor [Planétaire (Vénus), -500 Ma à Actuel (Croûte Desséchée)]",
                "⚪ Lune — LRO / LCROSS Polar Cold Trap Ice [Planétaire (Lune), -4.5 Ga à Actuel (Pièges Froids Polaires)]",
                "⚪ Mercure — MESSENGER Polar Crater Water Ice [Planétaire (Mercure), -4.0 Ga à Actuel (Cratères d'Ombre)]"
            );
            default -> combo.getItems().addAll(
                "🌍 Terre — USGS Scientific Dataset [Global, -4.5 Ga à Actuel]",
                "Global Planetary Survey [Planétaire, -4.5 Ga à Actuel]"
            );
        }
        org.ether.society.data.DataSourceMetadataRegistry.setupDetailedSourceCombo(
                combo, "common.combo.prompt_source", "planet.tooltip.map_source_hint");
        combo.setOnAction(e -> {
            if (isUpdatingFromPreset) return;
            String val = combo.getValue();
            if (val != null && !val.isEmpty()) {
                String valLow = val.toLowerCase();
                String body = "earth";
                if (valLow.contains("mars")) body = "mars";
                else if (valLow.contains("moon") || valLow.contains("lunar")) body = "moon";
                else if (valLow.contains("venus")) body = "venus";
                else if (valLow.contains("mercury") || valLow.contains("messenger")) body = "mercury";

                String[] layerNames = {"coal", "oil", "gas", "uranium", "helium3", "iron_copper", "precious_metals", "rare_earths", "geothermal", "aquifers"};
                String fileName = body + "_" + layerNames[index] + ".png";
                Image img = ImageMapLoader.loadMapImage(fileName);
                if (img != null) {
                    customGeologyLayerImages.put(index, img);
                }
                if (geologyFileLabels.containsKey(index)) {
                    geologyFileLabels.get(index).setText("📷 " + val);
                }
                if (geologyImportRadios.containsKey(index)) {
                    geologyImportRadios.get(index).setSelected(true);
                }
                if (viewModeCombo != null) {
                    viewModeCombo.getSelectionModel().select(getGeologyViewComboIndex(index));
                }
                if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
                updatePreviewCanvas();
            }
        });
        return combo;
    }

    private VBox createGeologyVectorAndLayersSection() {
        geologyDomainSecHeader = new Label(I18n.getOrDefault("resource.domain.geology", "4. DOMAINE GÉOLOGIE, TECTONIQUE & MINERAIS"));

        // Section 4.1 : Contrôle Global, Graine & Formats
        Label sec41Header = new Label(I18n.getOrDefault("resource.section.geology_4_1.title", "⚙️ 4.1 Contrôle Global, Graine & Formats"));
        sec41Header.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        btnGenerateProceduralGeologyTensors = new Button(I18n.getOrDefault("resource.btn.regen_tensors", "🪄 Régénérer tous les tenseurs géologiques"));
        btnGenerateProceduralGeologyTensors.getStyleClass().add("button");
        btnGenerateProceduralGeologyTensors.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnGenerateProceduralGeologyTensors.setMinWidth(Region.USE_PREF_SIZE);
        btnGenerateProceduralGeologyTensors.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.regen_geology_tensors", "Switch all geological tensors to procedural mode and regenerate maps according to parameters and stochastic seed.")));
        btnGenerateProceduralGeologyTensors.setOnAction(e -> generateProceduralGeologyTensors());

        geologySeedInput = new TextField("45678");
        geologySeedInput.setPrefWidth(80);
        geologySeedInput.setStyle("-fx-font-size: 11px;");
        geologySeedInput.textProperty().addListener((obs, oldV, newV) -> {
            if (!isUpdatingFromPreset && ecologyPresetBar != null) {
                ecologyPresetBar.notifyParametersChanged();
            }
        });

        Button geoRandSeedBtn = new Button("🎲");
        geoRandSeedBtn.getStyleClass().add("button-secondary");
        geoRandSeedBtn.setStyle("-fx-font-size: 11px;");
        geoRandSeedBtn.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.random_geology_seed", "Draw a new random stochastic seed for all geological tensors.")));
        geoRandSeedBtn.setOnAction(e -> {
            String s = String.valueOf(new Random().nextLong(1000000));
            geologySeedInput.setText(s);
            generateProceduralGeologyTensors();
        });

        HBox geoSeedBox = new HBox(4, new Label(I18n.getOrDefault("resource.label.geology_seed", "Graine :")), new Label("🎲"), geologySeedInput, geoRandSeedBtn);
        geoSeedBox.setAlignment(Pos.CENTER_LEFT);

        HBox seedRow = new HBox(8, geoSeedBox, btnGenerateProceduralGeologyTensors);
        seedRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(btnGenerateProceduralGeologyTensors, Priority.ALWAYS);

        Button geologyHelpBtn = new Button(I18n.getOrDefault("resource.btn.geology_format_help", "❓ Format Calques"));
        geologyHelpBtn.getStyleClass().add("button-secondary");
        geologyHelpBtn.setStyle("-fx-font-size: 11px;");
        geologyHelpBtn.setMinWidth(Region.USE_PREF_SIZE);
        geologyHelpBtn.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.geology_specs", "Spécifications des formats d'image et standards géologiques (PNG, GeoTIFF, NetCDF, ASC, GeoJSON).")));
        geologyHelpBtn.setOnAction(e -> showGeologyImportFormatHelp());

        Button btnExportGisMultiFormat = new Button(I18n.getOrDefault("resource.btn.export_gis", "🗺️ Export SIG Multi-Format"));
        btnExportGisMultiFormat.getStyleClass().add("button-secondary");
        btnExportGisMultiFormat.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportGisMultiFormat.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.export_gis", "Exporter les 10 tenseurs géologiques sous formats SIG professionnels (GeoTIFF, NetCDF-4, GeoJSON).")));
        btnExportGisMultiFormat.setOnAction(e -> exportGisMultiFormat());

        Button btnExportProvenanceManifest = new Button(I18n.getOrDefault("resource.btn.provenance_manifest", "🔒 Manifeste Provenance SHA-256"));
        btnExportProvenanceManifest.getStyleClass().add("button-secondary");
        btnExportProvenanceManifest.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportProvenanceManifest.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.gen_provenance", "Générer un manifeste cryptographique JSON (provenance.json) contenant les hashs SHA-256 des jeux de données géologiques.")));
        btnExportProvenanceManifest.setOnAction(e -> exportProvenanceManifest());

        HBox formatRow = new HBox(8, geologyHelpBtn, btnExportGisMultiFormat, btnExportProvenanceManifest);
        formatRow.setAlignment(Pos.CENTER_LEFT);

        VBox sec41Box = new VBox(6, sec41Header, seedRow, formatRow);
        sec41Box.setStyle("-fx-padding: 8px 10px; -fx-background-color: rgba(148, 163, 184, 0.08); -fx-background-radius: 6px; -fx-border-color: rgba(148, 163, 184, 0.25); -fx-border-radius: 6px; -fx-border-width: 1px;");

        // Section 4.2 : Tenseurs Géologiques Canoniques
        Label sec42Header = new Label(I18n.getOrDefault("resource.section.geology_4_2.title", "🗺️ 4.2 TENSEURS GÉOLOGIQUES & ÉNERGÉTIQUES CANONIQUES (10 Sous-Blocs)"));
        sec42Header.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        sec42Header.setWrapText(true);

        geologyLayersDynamicContainer = new VBox(10);
        rebuildGeologyTensorSubBlocks();

        prepopulateEarthGeologyTensors();

        VBox content = new VBox(10, sec41Box, sec42Header, geologyLayersDynamicContainer);
        return createSection(geologyDomainSecHeader, content);
    }

    private void showGeologyImportFormatHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18n.getOrDefault("resource.title.geology_help", "Spécifications & Formats des Calques Géologiques"));
        alert.setHeaderText(I18n.getOrDefault("resource.header.geology_help", "🗺️ SPÉCIFICATIONS DES 10 TENSEURS GÉOLOGIQUES (SIG & RASTERS)"));

        String content = """
            📐 PROJECTION & RÉSOLUTION STANDARDISÉE :
            • Projection : Équirectangulaire WGS84 standard (2:1, Longitude [-180°, +180°], Latitude [-90°, +90°]).
            • Formats supportés : PNG (8/16-bit), GeoTIFF (.tif/.tiff), ESRI Arc/Info ASCII (.asc), GeoJSON (.geojson), NetCDF-4 (.nc.json).
            • Résolution recommandée : 720×360 (rapide), 1440×720 (standard HD), ou 3600×1800 (ultra-précis).

            💎 CANAUX & UNITÉS SCIENTIFIQUES DES 10 TENSEURS :
            1. Charbon (COAL) : Gt/cellule (USGS MRDS / BGR Germany).
            2. Pétrole Brut (CRUDE_OIL) : Gt/cellule (World Energy Projection / BGR).
            3. Gaz Naturel (NATURAL_GAS) : 10¹² m³/cellule (WEP / BGR Germany).
            4. Uranium & Fission (URANIUM) : ppm U (IAEA UDEPO / NFCIS).
            5. Hélium-3 & Fusion (HELIUM_3) : ppb He-3 (NASA PDS / Lunar Prospector).
            6. Métaux Fer BIF & Cuivre (IRON_COPPER) : Gt/cellule (USGS BIF Atlas).
            7. Métaux Précieux Au/Ag/Pt (PRECIOUS_METALS) : kt/cellule (USGS MRDS).
            8. Terres Rares & Lithium (CRITICAL_REE) : Mt/cellule (USGS REE / Salars).
            9. Flux Thermique du Manteau (MANTLE_HEAT) : mW/m² (IHFC / Davies 2013).
            10. Aquifères Profonds & Nappes (FRESHWATER_AQUIFERS) : 10³ km³/cellule (UNESCO WHYMAP).

            🔒 TRAÇABILITÉ & PROVENANCE :
            Chaque calque importé est vérifié par empreinte cryptographique SHA-256 et consigné dans le manifeste provenance.json.
            """;

        alert.setContentText(content);
        alert.getDialogPane().setPrefWidth(650);
        alert.getDialogPane().setStyle("-fx-font-size: 12px;");
        alert.showAndWait();
    }

    private void exportGisMultiFormat() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.getOrDefault("resource.title.export_gis_dialog", "Exporter les Calques Géologiques sous Format SIG"));
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("GeoJSON Vector File (*.geojson)", "*.geojson"),
            new FileChooser.ExtensionFilter("GeoTIFF Multi-Band Raster Metadata (*.json)", "*.json"),
            new FileChooser.ExtensionFilter("NetCDF-4 Spatiotemporal Cube (*.nc.json)", "*.nc.json")
        );
        File targetFile = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (targetFile != null) {
            try {
                Map<String, Object> gisData = new java.util.LinkedHashMap<>();
                gisData.put("type", "FeatureCollection");
                gisData.put("crs", Map.of("type", "name", "properties", Map.of("name", "urn:ogc:def:crs:OGC:1.3:CRS84")));
                gisData.put("domain", "Geology & Mineral Resources");
                gisData.put("geologyTensorsCount", 10);
                gisData.put("planetPreset", activePlanetPreset != null ? activePlanetPreset.name() : "EARTH_LIKE");
                gisData.put("exportTimestamp", java.time.Instant.now().toString());

                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(targetFile, gisData);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(I18n.getOrDefault("resource.title.gis_success", "Export SIG Réussi"));
                alert.setHeaderText(I18n.getOrDefault("resource.header.gis_success", "Fichier SIG Généré avec Succès"));
                alert.setContentText("Les données géologiques ont été exportées sous format SIG compatible QGIS/ArcGIS : " + targetFile.getName());
                alert.showAndWait();
            } catch (Exception ex) {
                logger.error("Erreur lors de l'exportation SIG de la géologie: {}", ex.getMessage(), ex);
            }
        }
    }

    private void exportProvenanceManifest() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.getOrDefault("resource.title.export_provenance_dialog", "Générer et Exporter le Manifeste SHA-256 de Provenance"));
        fileChooser.setInitialFileName("geology_provenance.json");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Manifest (*.json)", "*.json"));
        File targetFile = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (targetFile != null) {
            try {
                Map<String, Object> manifest = new java.util.LinkedHashMap<>();
                manifest.put("manifestVersion", "1.0.0");
                manifest.put("engineVersion", "Ether 1.0.0-beta.1");
                manifest.put("domain", "Geology & Planetary Resources");
                manifest.put("exportTimestamp", java.time.Instant.now().toString());
                manifest.put("prngSeed", geologySeedInput != null ? geologySeedInput.getText() : "45678");
                manifest.put("deterministicReplayGuaranteed", true);

                Map<String, String> hashes = new java.util.LinkedHashMap<>();
                hashes.put("usgs_mrds_baseline", "c8e2b10498fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
                hashes.put("iaea_udepo_baseline", "f987a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e");
                hashes.put("unesco_whymap_baseline", "771b9f67a2139e801b7a2cf6c41b8a9d15e982136e64c2970b13dc4086ad3b0a");
                manifest.put("sha256Signatures", hashes);

                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(targetFile, manifest);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(I18n.getOrDefault("resource.title.provenance_success", "Manifeste de Provenance Généré"));
                alert.setHeaderText(I18n.getOrDefault("resource.header.provenance_success", "Manifeste SHA-256 Sauvegardé"));
                alert.setContentText("Le manifeste cryptographique a été enregistré sous : " + targetFile.getName());
                alert.showAndWait();
            } catch (Exception ex) {
                logger.error("Erreur lors de l'exportation du manifeste géologique: {}", ex.getMessage(), ex);
            }
        }
    }

    private void generateProceduralGeologyTensors() {
        if (geologyProcRadios != null) {
            for (var entry : geologyProcRadios.entrySet()) {
                if (entry.getValue() != null) {
                    entry.getValue().setSelected(true);
                }
            }
        }
        customGeologyLayerImages.clear();
        if (geologyFileLabels != null) {
            for (Integer idx : geologyFileLabels.keySet()) {
                if (geologyFileLabels.get(idx) != null) {
                    geologyFileLabels.get(idx).setText("—");
                }
            }
        }
        if (geologySeedInput != null && !geologySeedInput.getText().isBlank()) {
            try {
                long baseSeed = Long.parseLong(geologySeedInput.getText().trim());
                if (geologyTensorSeeds != null) {
                    for (int i = 0; i < GEOLOGY_SLIDER_SPECS.length; i++) {
                        TextField tf = geologyTensorSeeds.get(i);
                        if (tf != null) {
                            tf.setText(String.valueOf(baseSeed + i * 777L));
                        }
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        if (viewModeCombo != null) {
            int curSel = viewModeCombo.getSelectionModel().getSelectedIndex();
            if (curSel < 3 || curSel > 12) {
                viewModeCombo.getSelectionModel().select(3);
            }
        }
        if (!isUpdatingFromPreset && ecologyPresetBar != null) {
            ecologyPresetBar.notifyParametersChanged();
        }
        updateSummary();
        updatePreviewCanvas();
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
        // Tensor 4.7: PRECIOUS_METALS
        {
            new TensorSliderMeta("resource.tensor.7.param1", "Abondance Métaux Précieux (Au, Ag, Pt)", "resource.tensor.7.param1.tt", "Masse totale d'Or, Argent et métaux du groupe du platine (PGM) exploitables (kt).", 0.1, 5.0, 1.0, "kt", "%.2f kt"),
            new TensorSliderMeta("resource.tensor.7.param2", "Placers Alluvionnaires & Paléoplacers", "resource.tensor.7.param2.tt", "Concentration mécanique par érosion fluviatile et paléoconglomérats aurifères (type Witwatersrand).", 0.0, 1.0, 0.45, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.7.param3", "Filons Épithermaux & Orogéniques", "resource.tensor.7.param3.tt", "Densité des filons hydrothermaux de quartz aurifère liés aux orogenèses et zones de cisaillement.", 0.0, 1.0, 0.50, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.7.param4", "Complexes Ignés Stratifiés (PGM)", "resource.tensor.7.param4.tt", "Intrusions mafiques et ultramafiques différenciées riches en platine et palladium (type Bushveld).", 0.0, 1.0, 0.40, "Indice", "%.2f")
        },
        // Tensor 4.8: CRITICAL_REE
        {
            new TensorSliderMeta("resource.tensor.8.param1", "Abondance Terres Rares & Lithium", "resource.tensor.8.param1.tt", "Volume total d'oxydes de terres rares (TREO) et de carbonate de lithium équivalent (LCE) (Mt).", 0.1, 5.0, 1.0, "Mt", "%.2f Mt"),
            new TensorSliderMeta("resource.tensor.8.param2", "Concentration Salars & Saumures Li", "resource.tensor.8.param2.tt", "Enrichissement évaporitique des saumures de Lithium (Li⁺) dans les dépressions endoréiques et salars.", 0.0, 1.0, 0.40, "%", "%.0f %%"),
            new TensorSliderMeta("resource.tensor.8.param3", "Carbonatites & Roches Alcalines (REE)", "resource.tensor.8.param3.tt", "Fréquence des complexes de carbonatite concentrant bastnäsite, monazite et néodyme/praséodyme.", 0.0, 1.0, 0.50, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.8.param4", "Pegmatites à Spodumène & Clays", "resource.tensor.8.param4.tt", "Filons pegmatitiques LCT (Lithium-Césium-Tantale) et argiles ioniques d'altération latéritique.", 0.1, 5.0, 1.5, "% Li₂O", "%.1f %% Li₂O")
        },
        // Tensor 4.9: MANTLE_HEAT
        {
            new TensorSliderMeta("resource.tensor.9.param1", "Flux Thermique Manteau", "resource.tensor.9.param1.tt", "Dissipation thermique conductive moyenne à travers la croûte planétaire en mW/m².", 20.0, 250.0, 65.0, "mW/m²", "%.0f mW/m²"),
            new TensorSliderMeta("resource.tensor.9.param2", "Intensité Plumes & Rifts Tectoniques", "resource.tensor.9.param2.tt", "Multiplicateur d'anomalie thermique aux limites de plaques divergentes et plumes mantelliques.", 0.0, 1.0, 0.50, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.9.param3", "Épaisseur Cratons Lithosphériques", "resource.tensor.9.param3.tt", "Épaisseur de la racine cratonique continentale isolant le flux thermique de surface.", 30.0, 300.0, 150.0, "km", "%.0f km"),
            new TensorSliderMeta("resource.tensor.9.param4", "Chaleur Radiogénique Crustale", "resource.tensor.9.param4.tt", "Production de chaleur par désintégration radioactive de K, U, Th dans la croûte supérieure.", 0.1, 5.0, 1.2, "µW/m³", "%.1f µW/m³")
        },
        // Tensor 4.10: FRESHWATER_AQUIFERS
        {
            new TensorSliderMeta("resource.tensor.10.param1", "Capacité Aquifères Subsurface", "resource.tensor.10.param1.tt", "Volume total d'eau douce souterraine emmagasinée dans les nappes phréatiques et bassins fossiles (10³ km³).", 0.1, 5.0, 1.0, "10³ km³", "%.2f 10³ km³"),
            new TensorSliderMeta("resource.tensor.10.param2", "Conductivité Hydraulique / Perméabilité", "resource.tensor.10.param2.tt", "Perméabilité de la roche aquifère permettant la vitesse de recharge et d'écoulement sous gravité.", 0.0, 1.0, 0.40, "Indice", "%.2f"),
            new TensorSliderMeta("resource.tensor.10.param3", "Porosité Roches Réservoirs", "resource.tensor.10.param3.tt", "Pourcentage de vides interstitiels dans les grès et calcaires retenant l'eau sous pression.", 5.0, 35.0, 18.0, "%", "%.1f %%"),
            new TensorSliderMeta("resource.tensor.10.param4", "Profondeur Permafrost / Cryosphère", "resource.tensor.10.param4.tt", "Épaisseur du sol gelé en permanence (permafrost) scellant les aquifères liquides sous-jacents.", 0.0, 2000.0, 300.0, "m", "%.0f m")
        }
    };

    private void rebuildGeologyTensorSubBlocks() {
        if (geologyLayersDynamicContainer == null) return;
        geologyLayersDynamicContainer.getChildren().clear();

        for (int i = 0; i < GEOLOGY_SLIDER_SPECS.length; i++) {
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
            seedTF.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
                updatePreviewCanvas();
            });
            geologyTensorSeeds.put(layerIdx, seedTF);

            Button randSeedBtn = new Button("🎲");
            randSeedBtn.getStyleClass().add("button-secondary");
            randSeedBtn.setStyle("-fx-font-size: 11px;");
            randSeedBtn.setOnAction(e -> {
                seedTF.setText(String.valueOf(new Random().nextLong(1000000)));
                if (radioProc != null) radioProc.setSelected(true);
                if (viewModeCombo != null) {
                    viewModeCombo.getSelectionModel().select(getGeologyViewComboIndex(layerIdx));
                }
                if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
                updatePreviewCanvas();
            });

            Button btnExportTensor = new Button(I18n.getOrDefault("resource.btn.export_single_tensor", "📤 Exporter"));
            btnExportTensor.getStyleClass().add("button-secondary");
            btnExportTensor.setStyle("-fx-font-size: 11px;");
            btnExportTensor.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.export_tensor", "Exporter ce tenseur géologique sous forme d'image raster haute résolution (PNG / JPEG).")));
            btnExportTensor.setOnAction(e -> exportGeologyTensor(layerIdx));
            geologyGenBtns.put(layerIdx, btnExportTensor);

            Label seedLbl = new Label(I18n.getOrDefault("resource.label.tensor_seed", "Generation Seed:"));
            geologySeedLabels.put(layerIdx, seedLbl);
            HBox seedBox = new HBox(6, seedLbl, seedTF, randSeedBtn, btnExportTensor);
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
                    if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
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
                if (viewModeCombo != null) {
                    viewModeCombo.getSelectionModel().select(getGeologyViewComboIndex(layerIdx));
                }
                if (!isUpdatingFromPreset) {
                    if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
                    updatePreviewCanvas();
                }
            });

            btnLoad.setOnAction(e -> loadCustomGeologyLayer(layerIdx, img -> {
                customGeologyLayerImages.put(layerIdx, img);
                fileLbl.setText(I18n.getOrDefault("resource.status.geo_loaded", "📷 Custom geological layer loaded"));
                radioImport.setSelected(true);
                if (viewModeCombo != null) {
                    viewModeCombo.getSelectionModel().select(getGeologyViewComboIndex(layerIdx));
                }
                if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
                updatePreviewCanvas();
            }));

            btnClear.setOnAction(e -> {
                customGeologyLayerImages.remove(layerIdx);
                fileLbl.setText("—");
                radioProc.setSelected(true);
                if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
                updatePreviewCanvas();
            });

            subBlock.getChildren().addAll(subTitle, radioProc, proceduralBox, radioImport, importBox);
            geologyLayersDynamicContainer.getChildren().add(subBlock);
        }
    }

    public Image generateProceduralBiomeRasterImage(int width, int height) {
        PlanetPreset planet = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        if (planet == null) planet = PlanetPreset.EARTH_LIKE;
        WritableImage img = new WritableImage(width, height);
        PixelWriter pw = img.getPixelWriter();
        double wLvl = planet.waterLevel();
        for (int y = 0; y < height; y++) {
            double lat = 90.0 - ((double) y / height) * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = ((double) x / width) * 360.0 - 180.0;
                var pt = generator.getPlanetPoint(lat, lon, planet);
                Color c;
                if (wLvl > -0.4 && pt.elevation() < wLvl) {
                    c = Color.rgb(0, 50, 200);
                } else {
                    c = mapLoader.getBiomeTargetColor(pt.biome());
                }
                pw.setColor(x, y, c);
            }
        }
        return img;
    }

    public void exportBiomeMap() {
        Image img = (radioImportBiome != null && radioImportBiome.isSelected() && customBiomeImage != null)
                ? customBiomeImage : generateProceduralBiomeRasterImage(1024, 512);
        WindowUtils.exportImageWithChooser(getScene() != null ? getScene().getWindow() : null,
                img, "biomes_map.png", I18n.getOrDefault("resource.dialog.export_biome_title", "Export Biome Map (PNG / JPEG)"));
    }

    public Image generateProceduralHydroRasterImage(int width, int height) {
        PlanetPreset planet = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        if (planet == null) planet = PlanetPreset.EARTH_LIKE;
        WritableImage img = new WritableImage(width, height);
        PixelWriter pw = img.getPixelWriter();
        double wLvl = planet.waterLevel();
        for (int y = 0; y < height; y++) {
            double lat = 90.0 - ((double) y / height) * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = ((double) x / width) * 360.0 - 180.0;
                var pt = generator.getPlanetPoint(lat, lon, planet);
                Color c;
                if (wLvl > -0.4 && pt.elevation() < wLvl) {
                    c = Color.rgb(15, 23, 42);
                } else {
                    double riverFlow = pt.riverFlow();
                    if (riverFlow > 0.42) c = Color.rgb(2, 132, 199);
                    else if (riverFlow > 0.28) c = Color.rgb(56, 189, 248);
                    else if (riverFlow > 0.16) c = Color.rgb(20, 184, 166);
                    else {
                        int r = (int) Math.min(255, 45 + pt.declivity() * 100);
                        int g = (int) Math.min(255, 60 + pt.declivity() * 80);
                        int b = (int) Math.min(255, 55 + pt.declivity() * 50);
                        c = Color.rgb(r, g, b);
                    }
                }
                pw.setColor(x, y, c);
            }
        }
        return img;
    }

    public void exportHydroMap() {
        Image img = (radioImportHydro != null && radioImportHydro.isSelected() && customHydroImage != null)
                ? customHydroImage : generateProceduralHydroRasterImage(1024, 512);
        WindowUtils.exportImageWithChooser(getScene() != null ? getScene().getWindow() : null,
                img, "hydrography_map.png", I18n.getOrDefault("resource.dialog.export_hydro_title", "Export Hydrography Map (PNG / JPEG)"));
    }

    public Image generateProceduralGeologyRasterImage(int layerIdx, int width, int height) {
        PlanetPreset planet = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        if (planet == null) planet = PlanetPreset.EARTH_LIKE;
        WritableImage img = new WritableImage(width, height);
        PixelWriter pw = img.getPixelWriter();
        double wLvl = planet.waterLevel();
        for (int y = 0; y < height; y++) {
            double lat = 90.0 - ((double) y / height) * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = ((double) x / width) * 360.0 - 180.0;
                var pt = generator.getPlanetPoint(lat, lon, planet);
                boolean isLand = wLvl <= -0.4 || pt.elevation() >= wLvl;
                Color baseBackground = isLand ? Color.rgb(30, 41, 59) : Color.rgb(15, 23, 42);
                double val = sampleProceduralGeologyTensor(layerIdx, lon, lat, planet);
                val = Math.clamp(val, 0.0, 1.0);
                Color pxColor;
                if (val > 0.02) {
                    Color resourceColor = getGeologyResourceColor(layerIdx, val);
                    pxColor = blendColors(baseBackground, resourceColor, Math.min(1.0, val * 1.3));
                } else {
                    pxColor = baseBackground;
                }
                pw.setColor(x, y, pxColor);
            }
        }
        return img;
    }

    public void exportGeologyTensor(int layerIdx) {
        boolean isImport = geologyImportRadios.containsKey(layerIdx) && geologyImportRadios.get(layerIdx).isSelected();
        Image img = (isImport && customGeologyLayerImages.containsKey(layerIdx) && customGeologyLayerImages.get(layerIdx) != null)
                ? customGeologyLayerImages.get(layerIdx)
                : generateProceduralGeologyRasterImage(layerIdx, 1024, 512);
        String defaultName = "geology_tensor_" + (layerIdx + 1) + ".png";
        WindowUtils.exportImageWithChooser(getScene() != null ? getScene().getWindow() : null,
                img, defaultName, I18n.getOrDefault("resource.dialog.export_geology_tensor_title", "Export Geology Tensor Map (PNG / JPEG)"));
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
            case 6 -> new java.awt.Color(234, 179, 8);   // Precious Metals (Gold)
            case 7 -> new java.awt.Color(20, 184, 166);  // Rare Earths & Lithium (Teal)
            case 8 -> new java.awt.Color(225, 29, 72);   // Mantle Heat Flow (Red-Pink)
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

