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
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.generation.SimplexNoise;

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
 * @version 1.0.0-beta.2
 */
public class ResourceDistributionPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ResourceDistributionPanel.class);

    /** Internal state: active cells (List&lt;H3Cell&gt;). */
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
    /** Internal state: active loaded body key (String). */
    private String activeLoadedBodyKey = null;

    // Geological & Energy Tensor Maps (Tab 2 Parity with Tab 1 & Tab 3)
    private final org.ether.society.ui.resource.ResourceGeologySectionBuilder geologySectionBuilder =
            new org.ether.society.ui.resource.ResourceGeologySectionBuilder(new org.ether.society.ui.resource.ResourceGeologySectionBuilder.HostContext() {
                @Override public void notifyParamChange() { if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged(); }
                @Override public void updatePreviewCanvas() { ResourceDistributionPanel.this.updatePreviewCanvas(); }
                @Override public int getGeologyViewComboIndex(int layerIdx) { return ResourceDistributionPanel.this.getGeologyViewComboIndex(layerIdx); }
                @Override public void selectViewMode(int viewModeIndex) { if (viewModeCombo != null) viewModeCombo.getSelectionModel().select(viewModeIndex); }
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE); }
                @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
                @Override public boolean isUpdatingFromPreset() { return isUpdatingFromPreset; }
                @Override public long getEpochYear() {
                    return (ecologyPresetBar != null && ecologyPresetBar.getPresetCombo() != null && ecologyPresetBar.getPresetCombo().getValue() != null)
                            ? ecologyPresetBar.getPresetCombo().getValue().getAssociatedEpochYear() : 2026L;
                }
            });
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

    /** Internal state: is updating from preset (boolean). */
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




    // Section header labels for live i18n update
    private Label seedSecHeader;
    private Label biomeDomainSecHeader;
    private Label hydroDomainSecHeader;
    private Label climateSummaryLabel;
    private Label climateDomainSecHeader;
    private Label geologyDomainSecHeader;
    private Button btnExportProvenanceManifest;
    private Button autoDeriveMasterBtn;
    private Button exportBiomeBtn;
    private Button exportHydroBtn;
    private Button exportGeologyBtn;
    private Label biomeSourcePromptLabel;
    private Label hydroSourcePromptLabel;
    private Label climateSourcePromptLabel;
    private Label geologySourcePromptLabel;
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
    /** Internal state: zoom factor (double). */
    private double zoomFactor = 1.0;
    /** Internal state: pan x (double). */
    private double panX = 0.0;
    /** Internal state: pan y (double). */
    private double panY = 0.0;
    /** Internal state: drag start x (double). */
    private double dragStartX, dragStartY;

    private java.util.function.Supplier<PlanetPreset> planetPresetSupplier;
    private Consumer<PlanetPreset> planetPresetApplyCallback;

    /**
     * Set planet preset supplier.
     *
     * @param supplier the supplier parameter (java.util.function.Supplier&lt;PlanetPreset&gt;)
     */
    public void setPlanetPresetSupplier(java.util.function.Supplier<PlanetPreset> supplier) {
        this.planetPresetSupplier = supplier;
    }

    /**
     * Set planet preset apply callback.
     *
     * @param callback the callback parameter (Consumer&lt;PlanetPreset&gt;)
     */
    public void setPlanetPresetApplyCallback(Consumer<PlanetPreset> callback) {
        this.planetPresetApplyCallback = callback;
    }

    /**
     * Resource distribution panel.
     *
     * @param onResourcesAppliedCallback the on resources applied callback parameter (Consumer&lt;List&lt;H3Cell&gt;&gt;)
     */
    public ResourceDistributionPanel(Consumer<List<H3Cell>> onResourcesAppliedCallback) {
        this.onResourcesAppliedCallback = onResourcesAppliedCallback;

        getStyleClass().add("glass-panel");
        setPadding(new Insets(20));

        initUI();
        applyEcologyPreset(EcologyPreset.EARTH_STANDARD);
        updateTexts();

        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    /**
     * Set active planet preset.
     *
     * @param planetPreset the planet preset parameter (PlanetPreset)
     */
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

    /**
     * Set active cells.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     */
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
                new Label(I18n.getOrDefault("resource.validation.header", "⚠️ï¸ Paramètres ou cartes écologiques requis manquants ou invalides :")),
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
            /**
             * Update item.
             *
             * @param item the item parameter (PlanetPreset)
             * @param empty the empty parameter (boolean)
             */
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
                // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(I18n.getPlanetPresetDisplayName(item.name()));
                }
            }
        });
        planetPresetCombo.setButtonCell(new ListCell<PlanetPreset>() {
            /**
             * Update item.
             *
             * @param item the item parameter (PlanetPreset)
             * @param empty the empty parameter (boolean)
             */
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
                // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
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
            /**
             * To string.
             *
             * @param item the item parameter (PlanetPreset)
             * @return the resulting computation or state reference
             */
            @Override
            public String toString(PlanetPreset item) {
                return item == null ? I18n.getPlanetPresetDisplayName("Terre (Terran)") : I18n.getPlanetPresetDisplayName(item.name());
            }
            /**
             * From string.
             *
             * @param string the string parameter (String)
             * @return the resulting computation or state reference
             */
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

        planetContextLabel = new Label(I18n.getOrDefault("resource.territory.earth", "ðŸª Territory: Earth-Like Planet"));
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

        autoDeriveMasterBtn = new Button(I18n.getOrDefault("resource.btn.auto_derive_all", "⚡ Auto-derive All from Physics (Biomes, Aquifers & Geology)"));
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
        ecologyPresetBar.setPresets(org.ether.society.persistence.PresetStorageService.loadAllEcologyPresets(), EcologyPreset.EARTH_STANDARD);

        ecologyDescriptionArea = new TextArea();
        ecologyDescriptionArea.setPrefRowCount(4);
        ecologyDescriptionArea.setWrapText(true);
        ecologyDescriptionArea.setEditable(false);
        ecologyDescriptionArea.getStyleClass().add("glass-text-area");
        ecologyDescriptionArea.setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0; -fx-background-color: rgba(15, 23, 42, 0.6); -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 4; -fx-background-radius: 4;");

        ecologyPresetBar.setListener(new PresetControlBar.PresetActionsListener<EcologyPreset>() {
            /**
             * On preset selected.
             *
             * @param preset the preset parameter (EcologyPreset)
             */
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

            /**
             * On save preset.
             *
             * @param name the name parameter (String)
             */
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

            /**
             * On delete preset.
             *
             * @param preset the preset parameter (EcologyPreset)
             */
            @Override
            public void onDeletePreset(EcologyPreset preset) {
                ecologyPresetBar.getPresetCombo().getItems().remove(preset);
            }

            /**
             * On export preset.
             *
             * @param targetFile the target file parameter (File)
             * @param preset the preset parameter (EcologyPreset)
             */
            @Override
            public void onExportPreset(File targetFile, EcologyPreset preset) {
                try {
                    ObjectMapper mapper = org.ether.society.persistence.EtherJsonUtil.getMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.writeValue(targetFile, preset);
                    logger.info("Exported ecology preset to {}", targetFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to export ecology preset", ex);
                }
            }

            /**
             * On import preset.
             *
             * @param sourceFile the source file parameter (File)
             */
            @Override
            public void onImportPreset(File sourceFile) {
                try {
                    ObjectMapper mapper = org.ether.society.persistence.EtherJsonUtil.getMapper();
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
        org.ether.society.ui.util.DataSourceUIHelper.setupDetailedSourceCombo(
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

        fetchOnlineHydroBtn = new Button(I18n.getOrDefault("resource.btn.fetch_online_hydro", "ðŸŒ Download Satellite Hydrography (Earth WMS / Earth Preset)"));
        fetchOnlineHydroBtn.setMaxWidth(Double.MAX_VALUE);
        fetchOnlineHydroBtn.getStyleClass().add("button-secondary");
        fetchOnlineHydroBtn.setOnAction(e -> fetchOnlineHydroData());

        proceduralHydroBtn = new Button(I18n.getOrDefault("resource.btn.procedural_hydro", "⚡ Auto-generate Rivers & Watercourses (Procedural Slope)"));
        proceduralHydroBtn.setMaxWidth(Double.MAX_VALUE);
        proceduralHydroBtn.getStyleClass().add("button-secondary");
        proceduralHydroBtn.setOnAction(e -> generateProceduralHydrography());

        fetchOnlineBtn = new Button(I18n.getOrDefault("resource.btn.fetch_online", "ðŸŒ Download WMS Satellite Maps (USGS / NASA)"));
        fetchOnlineBtn.setMaxWidth(Double.MAX_VALUE);
        fetchOnlineBtn.getStyleClass().add("button-secondary");
        fetchOnlineBtn.setOnAction(e -> fetchOnlineSatelliteData());

        exportMapsBtn = new Button(I18n.getOrDefault("resource.btn.export_maps", "📤 Export Maps (PNG + WorldFile .tfw)"));
        exportMapsBtn.setMaxWidth(Double.MAX_VALUE);
        exportMapsBtn.getStyleClass().add("button-secondary");
        exportMapsBtn.setOnAction(e -> exportMapsWithWorldFiles());

        specsHelpBtn = new Button(I18n.getOrDefault("resource.btn.specs_help", "ℹ️ï¸ Biome & Geology Map Specifications"));
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

        fetchOnlineClimateBtn = new Button(I18n.getOrDefault("planet.map.btn_fetch_online_climate", "ðŸŒ Download Satellite Climate NASA/USGS (WMS)"));
        fetchOnlineClimateBtn.setMaxWidth(Double.MAX_VALUE);
        fetchOnlineClimateBtn.getStyleClass().add("button-secondary");
        fetchOnlineClimateBtn.setOnAction(e -> fetchOnlineClimateData());

        climateHelpBtn = new Button(I18n.getOrDefault("planet.map.btn_climate_help", "ℹ️ï¸ Climate Map Specifications"));
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

        exportBiomeBtn = new Button(I18n.getOrDefault("resource.btn.export_biome", "📤 Export Biome Map (PNG / JPEG)"));
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
        org.ether.society.ui.util.DataSourceUIHelper.setupDetailedSourceCombo(
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

        biomeSourcePromptLabel = new Label(I18n.getOrDefault("resource.param.map_source", "Celestial Body / Source:"));
        VBox biomeImportBox = new VBox(8,
                createControlRow(biomeSourcePromptLabel, biomeSourceCombo,
                        I18n.getOrDefault("resource.desc.import_biome_map", "Select a preset satellite source")),
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

        exportHydroBtn = new Button(I18n.getOrDefault("resource.btn.export_hydro", "📤 Export Hydrography Map (PNG / JPEG)"));
        exportHydroBtn.setMaxWidth(Double.MAX_VALUE);
        exportHydroBtn.getStyleClass().add("button-secondary");
        exportHydroBtn.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.export_hydro", "Export the active hydrographic map as high-resolution raster (PNG / JPEG).")));
        exportHydroBtn.setOnAction(e -> exportHydroMap());

        VBox hydroProcBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.seed.label", "Generation Seed:")), hydroSeedBox,
                        I18n.getOrDefault("resource.tooltip.seed_hydro", "Random seed for procedural rivers and watercourses")),
                autoDeriveHydroBtn,
                hydroStatusLabel,
                createControlRow(freshwaterAquiferRowLabel, freshwaterAquiferSlider, "%.0f x10³ km³", I18n.getOrDefault("resource.desc.freshwater_aquifer", "Total volume of groundwater and continental aquifers")),
                exportHydroBtn
        );
        hydroProcBox.getStyleClass().add("subcard-procedural-box");

        hydroSourceCombo = new ComboBox<>();
        hydroSourceCombo.getItems().addAll("none", "earth", "mars", "venus", "moon", "mercury");
        hydroSourceCombo.setValue("none");
        org.ether.society.ui.util.DataSourceUIHelper.setupDetailedSourceCombo(
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

        hydroSourcePromptLabel = new Label(I18n.getOrDefault("resource.param.map_source", "Celestial Body / Source:"));
        VBox hydroImportBox = new VBox(8,
                createControlRow(hydroSourcePromptLabel, hydroSourceCombo,
                        I18n.getOrDefault("resource.desc.import_hydro_map", "Satellite source for hydrographic map")),
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

        climateSummaryLabel = new Label(I18n.getOrDefault("resource.climate.summary", "ðŸŒ¡ï¸ Climate Model: Inherited from Tab 1 planet parameters (Temperature, Gradient, Pressure, Oâ‚‚, COâ‚‚, Albedo)"));
        climateSummaryLabel.getStyleClass().add("subcard-status-muted");
        climateSummaryLabel.setWrapText(true);

        VBox climateProcBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.seed.label", "Generation Seed:")), climateSeedBox,
                        I18n.getOrDefault("resource.tooltip.seed_climate", "Random seed for procedural climate model")),
                climateSummaryLabel
        );
        climateProcBox.getStyleClass().add("subcard-procedural-box");

        climateSourceCombo = new ComboBox<>();
        climateSourceCombo.getItems().addAll(
                "none",
                "earth", "mars", "venus", "moon", "mercury"
        );
        climateSourceCombo.setValue("none");
        org.ether.society.ui.util.DataSourceUIHelper.setupDetailedSourceCombo(
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

        climateSourcePromptLabel = new Label(I18n.getOrDefault("planet.climate.source_label", "Reference Source:"));
        VBox climateImportBox = new VBox(8,
                createControlRow(climateSourcePromptLabel, climateSourceCombo,
                        I18n.getOrDefault("planet.tooltip.climate_map", "Reference climate data source")),
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

        // --- DOMAIN 4: GEOLOGY, TECTONICS & MINERALS ---
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

        exportGeologyBtn = new Button(I18n.getOrDefault("resource.btn.export_geology", "📤 Export Geological Map (PNG + WorldFile)"));
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
                        I18n.getOrDefault("resource.tooltip.seed_geology", "Random seed for geological deposit distribution")),
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
        org.ether.society.ui.util.DataSourceUIHelper.setupDetailedSourceCombo(
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

        geologySourcePromptLabel = new Label(I18n.getOrDefault("resource.param.map_source", "Celestial Body / Source:"));
        VBox geologyImportBox = new VBox(8,
                createControlRow(geologySourcePromptLabel, geologySourceCombo,
                        I18n.getOrDefault("resource.desc.import_geology_map", "Satellite source for geological map")),
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
                I18n.getOrDefault("resource.view.coal", "â›ï¸ 3. Gisements de Charbon"),
                I18n.getOrDefault("resource.view.oil", "ðŸ›¢ï¸ 4. Réserves de Pétrole Brut"),
                I18n.getOrDefault("resource.view.gas", "🔥 5. Champs de Gaz Naturel"),
                I18n.getOrDefault("resource.view.uranium", "âš›ï¸ 6. Minerais d'Uranium & Fission"),
                I18n.getOrDefault("resource.view.helium3", "🌌 7. Hélium-3 & Fusion Lunaires"),
                I18n.getOrDefault("resource.view.iron_copper", "â›“ï¸ 8. Métaux Fer BIF & Cuivre"),
                I18n.getOrDefault("resource.view.precious_metals", "💎 9. Métaux Précieux (Or, Argent, PGM)"),
                I18n.getOrDefault("resource.view.rare_earths", "🔋 10. Terres Rares & Minéraux Critiques"),
                I18n.getOrDefault("resource.view.heat", "🌋 11. Flux Thermique du Manteau"),
                I18n.getOrDefault("resource.view.aquifer", "💧 12. Aquifères & Eau Douce"),
                I18n.getOrDefault("resource.view.section_derived", "────────── CARTES DÉDUITES / ANOMALIES ──────────"),
                I18n.getOrDefault("resource.view.temp", "ðŸŒ¡ï¸ 13. Températures Surface & Microclimats"),
                I18n.getOrDefault("resource.view.seismic", "🌋 14. Tectonique & Aléa Sismique/Volcanique"),
                I18n.getOrDefault("resource.view.aridity", "ðŸœï¸ 15. Aridité & Salinisation des Sols")
        );
        viewModeCombo.setValue(viewModeCombo.getItems().get(1));
        viewModeCombo.setMaxWidth(380);
        viewModeCombo.setCellFactory(p -> new ListCell<>() {
            /**
             * Update item.
             *
             * @param item the item parameter (String)
             * @param empty the empty parameter (boolean)
             */
            @Override
            protected void updateItem(String item, boolean empty) {
                // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
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

        btnReliefOverlay = new ToggleButton(I18n.getOrDefault("resource.btn.relief_overlay", "⛏️ï¸ Relief"));
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

        summaryLabel = new Label(I18n.getOrDefault("resource.prompt.load_cells", "ðŸŒ Generate or load planet cells to activate map."));
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
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (planetContextLabel == null) return;
        PlanetPreset preset = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        if (preset == null) preset = PlanetPreset.EARTH_LIKE;

        int cellCount = activeCells != null ? activeCells.size() : 12482;
        String text = I18n.getOrDefault(
                "planet.context.fmt",
                "ðŸª Planète Héritée : %s | Rayon : %,.0f km | Temp : %.1f°C | Eau : %.0f%% | Cellules : %,d",
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
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
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

    /**
     * Get selected ecology preset.
     *
     * @return the resulting computation or state reference
     */
    public EcologyPreset getSelectedEcologyPreset() {
        return ecologyPresetBar != null && ecologyPresetBar.getPresetCombo() != null ? ecologyPresetBar.getPresetCombo().getValue() : null;
    }

    /**
     * Apply ecology preset.
     *
     * @param p the p parameter (EcologyPreset)
     */
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
            if (climateFileLabel != null) climateFileLabel.setText(I18n.getOrDefault("planet.preset.climate", "ðŸŒ¡ï¸ Preset Climate"));
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
            if (rainfallFileLabel != null) rainfallFileLabel.setText(I18n.getOrDefault("planet.preset.rainfall", "ðŸŒ§ï¸ Preset Rainfall"));
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
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText(I18n.getOrDefault("planet.preset.seasonality", "â˜€ï¸ Preset Seasonality"));
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
                climateFileLabel.setText("ðŸŒ¡ï¸ " + file.getName());
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
                rainfallFileLabel.setText("ðŸŒ§ï¸ " + file.getName());
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
                seasonalityFileLabel.setText("ðŸ‚ " + file.getName());
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
                climateFileLabel.setText("ðŸŒ " + body.getName() + " MODIS Thermal WMS");
                if (climateStatusLabel != null) climateStatusLabel.setText(I18n.get("planet.map.status_success"));
                updatePreviewCanvas();
            }
        }));
        onlineMapService.fetchRainfallMapAsync(body).thenAccept(img -> javafx.application.Platform.runLater(() -> {
            if (img != null) {
                customRainfallImage = img;
                rainfallFileLabel.setText("ðŸŒ " + body.getName() + " GPM Rainfall WMS");
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
                "   • Le bouton « ðŸŒ Télécharger Climat Satellite » permet d'obtenir directement les flux officiels NASA MODIS (température) et GPM (pluviométrie)."
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
                ecoCompatibilityLabel.setText(String.format(I18n.getOrDefault("resource.status.incompatible_map", "⚠️ï¸ Incompatibilité : Carte « %s » sur terrain « %s » (Relief non concordant)"), sourceKey.toUpperCase(), activeDisplayName));
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
                ecoCompatibilityLabel.setText(I18n.getOrDefault("resource.status.no_map", "ðŸª No external map loaded — Procedural mode active"));
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
            ecoCompatibilityLabel.setText(String.format("ðŸª %s authentic maps loaded (Biomes, Hydro, Climate & 9 Geology Tensors)", body.toUpperCase()));
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
                if (hydroFileLabel != null) hydroFileLabel.setText("ðŸŒ " + body.getName() + " USGS HydroSHEDS / SWBD Hydro WMS");
                if (mapStatusLabel != null) mapStatusLabel.setText(I18n.getOrDefault("planet.map.status_success", "Hydrographic map downloaded successfully."));
                updatePreviewCanvas();
                updateSummary();
            } else {
                if ("earth".equals(sourceKey) && radioImportHydro != null && radioImportHydro.isSelected()) {
                    if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("resource.source.earth_hydro_wms", "🌊 Earth SWBD / USGS HydroSHEDS Hydrographie"));
                    if (mapStatusLabel != null) mapStatusLabel.setText(I18n.getOrDefault("resource.status.usgs_active", "ðŸŒ Earth Hydrographic Map USGS HydroSHEDS / SWBD active."));
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
                        biomeFileLabel.setText("ðŸŒ " + body.getName() + " WMS Biomes");
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
        org.ether.society.ui.export.GeospatialExportUIHelper.exportRasterWithDialog(
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
                "   • ðŸ›¢ï¸ Pétrole : Dégradé Rouge Rubis / Carmin (#991B1B â†’ #F87171)\n" +
                "   • 💨 Gaz Naturel : Dégradé Cyan / Bleu Ciel (#0E7490 → #38BDF8)\n" +
                "   • â˜¢ï¸ Uranium / Thorium : Vert Émeraude (#22C55E)\n" +
                "   • âš›ï¸ Hélium-3 : Violet Néon / Pourpre (#A855F7)\n" +
                "   • â›ï¸ Fer & Cuivre : Rouille / Cuivre Orangé (#8B4513 â†’ #F97316)\n" +
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

    private String getBiomeSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase()) {
            case "mars" -> "ðŸŒ¿ Mars MOLA & Biomes Reconstitution";
            case "moon" -> "ðŸŒ¿ Moon Clementine Regolith (Barren Biome)";
            case "venus" -> "ðŸŒ¿ Venus Magellan Radar (Supercritical Biome)";
            case "mercury" -> "ðŸŒ¿ Mercury MESSENGER Surface (Airless Biome)";
            default -> I18n.getOrDefault("planet.source.earth_modis_biomes", "ðŸŒ¿ Earth MODIS Biomes");
        };
    }

    private String getHydroSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase()) {
            case "mars" -> "ðŸŒŠ Mars MARSIS / Ancient Paleochannels Hydrography";
            case "moon" -> "ðŸŒŠ Moon Polar Traps (Dry Crust Hydrography)";
            case "venus" -> "ðŸŒŠ Venus Volcanic Channels (Anhydrous Hydrography)";
            case "mercury" -> "ðŸŒŠ Mercury Polar Cold Traps Hydrography";
            default -> I18n.getOrDefault("resource.source.earth_hydro", "ðŸŒŠ Earth SWBD Hydrography");
        };
    }

    private String getGeologySourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase()) {
            case "mars" -> "ðŸª¨ Mars MOLA & GRS Geology Tensors";
            case "moon" -> "ðŸª¨ Moon Clementine & KREEP Geology Tensors";
            case "venus" -> "ðŸª¨ Venus Magellan Radar Geology Tensors";
            case "mercury" -> "ðŸª¨ Mercury MESSENGER GRS/XRS Geology Tensors";
            default -> I18n.getOrDefault("resource.source.earth_geology", "ðŸª¨ Earth USGS Geology");
        };
    }

    private String getClimateSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase()) {
            case "mars" -> "ðŸŒ¡ï¸ Mars Viking/MGS Thermal Inertia Model";
            case "moon" -> "ðŸŒ¡ï¸ Moon Diviner Lunar Radiometer Experiment";
            case "venus" -> "ðŸŒ¡ï¸ Venus Venera/VIRTIS Thermal Greenhouse Model";
            case "mercury" -> "ðŸŒ¡ï¸ Mercury MESSENGER Thermal Radiometry";
            default -> "ðŸŒ¡ï¸ Earth ERA5 / MODIS Land Surface Temp";
        };
    }

    private String getRainfallSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase()) {
            case "mars" -> "ðŸŒ§ï¸ Mars Atmospheric Water Vapor & Sublimation";
            case "moon" -> "ðŸŒ§ï¸ Moon Vacuum Exosphere (0 mm/yr Precipitation)";
            case "venus" -> "ðŸŒ§ï¸ Venus Sulfuric Acid Cloud Condensation & Virga";
            case "mercury" -> "ðŸŒ§ï¸ Mercury Exospheric Zero Precipitation";
            default -> "ðŸŒ§ï¸ Earth NASA GPM IMERG Precipitation";
        };
    }

    private String getSeasonalitySourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase()) {
            case "mars" -> "â˜€ï¸ Mars Obliquity (25.2Â°) Thermal Seasonality";
            case "moon" -> "â˜€ï¸ Moon Diurnal Day/Night Extreme Seasonality";
            case "venus" -> "â˜€ï¸ Venus Slow Retrograde Thermal Inertia";
            case "mercury" -> "â˜€ï¸ Mercury 3:2 Spin-Orbit Thermal Resonance";
            default -> "â˜€ï¸ Earth Orbital Milankovitch Seasonality";
        };
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
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
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
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
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
                    boolean useImport = geologySectionBuilder.isGeologyImportActive(layerIdx);
                    Image gImg = geologySectionBuilder.getGeologyLayerImage(layerIdx);

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
        return geologySectionBuilder.sampleProceduralGeologyTensor(layerIdx, lon, lat, planet);
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
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (summaryLabel == null) return;

        summaryLabel.setText(String.format(
                I18n.getOrDefault("resource.summary.format",
                        "🌲 Végétale : %.0f GtC  |  🌾 Sols : %.0f GtC  |  🦌 Faune : %.2f GtC\n" +
                        "â›ï¸ Métaux : %.0f Gt  |  💎 Précieux : %.0f Mt  |  ðŸŒ‹ Manteau : %.1f mW/m²  |  ðŸ’§ Aquifères : %.0f x10³ km³"),
                terrestrialBiomassSlider != null ? terrestrialBiomassSlider.getValue() : 450.0,
                soilCarbonSlider != null ? soilCarbonSlider.getValue() : 1500.0,
                faunaBiomassSlider != null ? faunaBiomassSlider.getValue() : 2.0,
                crustalMetalSlider != null ? crustalMetalSlider.getValue() : 80.0,
                preciousMetalSlider != null ? preciousMetalSlider.getValue() : 1200.0,
                mantleHeatSlider != null ? mantleHeatSlider.getValue() : 87.0,
                freshwaterAquiferSlider != null ? freshwaterAquiferSlider.getValue() : 15000.0
        ));
    }

    /**
     * Update texts.
     *
     */
    public void updateTexts() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
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
            if (climateSummaryLabel != null) climateSummaryLabel.setText(I18n.getOrDefault("resource.climate.summary", "ðŸŒ¡ï¸ Climate Model: Inherited from Tab 1 planet parameters (Temperature, Gradient, Pressure, Oâ‚‚, COâ‚‚, Albedo)"));
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

            if (fetchOnlineHydroBtn != null) fetchOnlineHydroBtn.setText(I18n.getOrDefault("resource.btn.fetch_online_hydro", "ðŸŒ Download Satellite Hydrography (Earth WMS / Earth Preset)"));
            if (proceduralHydroBtn != null) proceduralHydroBtn.setText(I18n.getOrDefault("resource.btn.procedural_hydro", "⚡ Auto-generate Rivers & Watercourses (Procedural Slope)"));
            if (fetchOnlineBtn != null) fetchOnlineBtn.setText(I18n.getOrDefault("resource.btn.fetch_online", "ðŸŒ Download WMS Satellite Maps (USGS / NASA)"));
            if (fetchOnlineClimateBtn != null) fetchOnlineClimateBtn.setText(I18n.getOrDefault("planet.map.btn_fetch_online_climate", "ðŸŒ Download Satellite Climate NASA/USGS (WMS)"));
            if (climateHelpBtn != null) climateHelpBtn.setText(I18n.getOrDefault("planet.map.btn_climate_help", "ℹ️ï¸ Climate Map Specifications"));
            if (exportMapsBtn != null) exportMapsBtn.setText(I18n.getOrDefault("resource.btn.export_maps", "📤 Export Maps (PNG + WorldFile .tfw)"));
            if (specsHelpBtn != null) specsHelpBtn.setText(I18n.getOrDefault("resource.btn.specs_help", "ℹ️ï¸ Biome & Geology Map Specifications"));

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
                btnReliefOverlay.setText(I18n.getOrDefault("resource.btn.relief_overlay", "⛏️ï¸ Relief"));
            }

            if (viewModeCombo != null) {
                int selected = viewModeCombo.getSelectionModel().getSelectedIndex();
                viewModeCombo.getItems().setAll(
                    I18n.getOrDefault("resource.view.section_base", "────────── 12 CARTES CANONIQUES DE BASE ──────────"),
                    I18n.getOrDefault("resource.view.biomes", "🌿 1. Biomes & Couverture Végétale"),
                    I18n.getOrDefault("resource.view.hydro", "🌊 2. Hydrographie & Réseau Fluvial"),
                    I18n.getOrDefault("resource.view.coal", "â›ï¸ 3. Gisements de Charbon"),
                    I18n.getOrDefault("resource.view.oil", "ðŸ›¢ï¸ 4. Réserves de Pétrole Brut"),
                    I18n.getOrDefault("resource.view.gas", "🔥 5. Champs de Gaz Naturel"),
                    I18n.getOrDefault("resource.view.uranium", "âš›ï¸ 6. Minerais d'Uranium & Fission"),
                    I18n.getOrDefault("resource.view.helium3", "🌌 7. Hélium-3 & Fusion Lunaires"),
                    I18n.getOrDefault("resource.view.iron_copper", "â›“ï¸ 8. Métaux Fer BIF & Cuivre"),
                    I18n.getOrDefault("resource.view.precious_metals", "💎 9. Métaux Précieux (Or, Argent, PGM)"),
                    I18n.getOrDefault("resource.view.rare_earths", "🔋 10. Terres Rares & Minéraux Critiques / Lithium"),
                    I18n.getOrDefault("resource.view.heat", "🌋 11. Flux Thermique du Manteau"),
                    I18n.getOrDefault("resource.view.aquifer", "💧 12. Aquifères & Eau Douce"),
                    I18n.getOrDefault("resource.view.section_derived", "────────── CARTES DÉDUITES / ANOMALIES ──────────"),
                    I18n.getOrDefault("resource.view.temp", "ðŸŒ¡ï¸ 13. Températures Surface & Microclimats"),
                    I18n.getOrDefault("resource.view.seismic", "🌋 14. Tectonique & Aléa Sismique/Volcanique"),
                    I18n.getOrDefault("resource.view.aridity", "ðŸœï¸ 15. Aridité & Salinisation des Sols")
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
            if (climateSummaryLabel != null) climateSummaryLabel.setText(I18n.getOrDefault("resource.climate.summary", "ðŸŒ¡ï¸ Climate Model: Inherited from Tab 1 planet parameters (Temperature, Gradient, Pressure, Oâ‚‚, COâ‚‚, Albedo)"));

            if (applyBtn != null) applyBtn.setText(I18n.getOrDefault("resource.btn.apply", "✅ Apply scientific distribution to simulation"));

            if (geologySectionBuilder != null) {
                geologySectionBuilder.updateTexts();
            }
            if (btnExportProvenanceManifest != null) {
                btnExportProvenanceManifest.setText(I18n.getOrDefault("resource.btn.provenance_manifest", "🔒 SHA-256 Provenance Manifest"));
                btnExportProvenanceManifest.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.gen_provenance", "Generate a cryptographic JSON manifest (provenance.json) containing SHA-256 hashes of geological datasets.")));
            }
            if (autoDeriveMasterBtn != null) autoDeriveMasterBtn.setText(I18n.getOrDefault("resource.btn.auto_derive_all", "⚡ Auto-derive All from Physics (Biomes, Aquifers & Geology)"));
            if (exportBiomeBtn != null) exportBiomeBtn.setText(I18n.getOrDefault("resource.btn.export_biome", "📤 Export Biome Map (PNG + WorldFile)"));
            if (exportHydroBtn != null) exportHydroBtn.setText(I18n.getOrDefault("resource.btn.export_hydro", "📤 Export Hydrographic Map (PNG + WorldFile)"));
            if (exportGeologyBtn != null) exportGeologyBtn.setText(I18n.getOrDefault("resource.btn.export_geology", "📤 Export Geological Map (PNG + WorldFile)"));
            if (biomeSourcePromptLabel != null) biomeSourcePromptLabel.setText(I18n.getOrDefault("resource.param.map_source", "Celestial Body / Source:"));
            if (hydroSourcePromptLabel != null) hydroSourcePromptLabel.setText(I18n.getOrDefault("resource.param.map_source", "Celestial Body / Source:"));
            if (climateSourcePromptLabel != null) climateSourcePromptLabel.setText(I18n.getOrDefault("planet.climate.source_label", "Reference Source:"));
            if (geologySourcePromptLabel != null) geologySourcePromptLabel.setText(I18n.getOrDefault("resource.param.map_source", "Celestial Body / Source:"));

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

            // Distribute 10 physical geological and energy tensors (Coal, Oil, Gas, Uranium, Iron/Copper, Precious, REE, Geothermal, Aquifer, Clay)
            PlanetPreset plPreset = activePlanetPreset != null ? activePlanetPreset : PlanetPreset.EARTH_LIKE;
            double latVal = cell.getLatitude() != null ? cell.getLatitude() : 0.0;
            double lonVal = cell.getLongitude() != null ? cell.getLongitude() : 0.0;
            double coalDensity = sampleProceduralGeologyTensor(0, lonVal, latVal, plPreset);
            double oilDensity = sampleProceduralGeologyTensor(1, lonVal, latVal, plPreset);
            double gasDensity = sampleProceduralGeologyTensor(2, lonVal, latVal, plPreset);
            double uraniumDensity = sampleProceduralGeologyTensor(3, lonVal, latVal, plPreset);
            double ironCopperDensity = sampleProceduralGeologyTensor(5, lonVal, latVal, plPreset);
            double preciousDensity = sampleProceduralGeologyTensor(6, lonVal, latVal, plPreset);
            double reeDensity = sampleProceduralGeologyTensor(7, lonVal, latVal, plPreset);
            double mantleHotspot = sampleProceduralGeologyTensor(8, lonVal, latVal, plPreset);
            double aquiferSpot = sampleProceduralGeologyTensor(9, lonVal, latVal, plPreset);

            cell.setResourceCoal(coalDensity * 10000.0);
            cell.setResourceOil(oilDensity * 8000.0);
            cell.setResourceGas(gasDensity * 6000.0);
            cell.setResourceUranium(uraniumDensity * 500.0);
            cell.setResourceRareEarths(reeDensity * 300.0);

            // Augment base metal, precious metal, mantle heat, aquifer, and sedimentary clay reserves from geological hotspots
            double curMetal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
            double curPrecious = cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal() : 0.0;
            cell.setResourceMetal(curMetal + ironCopperDensity * 6000.0);
            cell.setResourcePreciousMetal(curPrecious + preciousDensity * 450.0);
            cell.setMantleHeatFlow(heatFlow * (0.7 + 0.9 * mantleHotspot));
            cell.setFreshwaterAquifer(baseAquifer * (0.5 + 1.2 * aquiferSpot));
            if (b != Biome.OCEAN && b != Biome.DEEP_OCEAN) {
                double clayDeposit = (500.0 + (cell.getRainfall() != null ? cell.getRainfall() : 500.0) * 1.5) * (0.4 + 0.6 * aquiferSpot);
                cell.setResourceClay(clayDeposit);
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

    /**
     * Get active planet preset.
     *
     * @return the resulting computation or state reference
     */
    public PlanetPreset getActivePlanetPreset() {
        return activePlanetPreset;
    }

    /**
     * Validate resource setup.
     *
     * @return the resulting computation or state reference
     */
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

    /**
     * Get validation errors.
     *
     * @return the resulting computation or state reference
     */
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();
        boolean isBiomeImport = radioImportBiome != null && radioImportBiome.isSelected();
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
            if (customResourceImage == null && (geologySectionBuilder.getCustomGeologyLayerImages() == null || geologySectionBuilder.getCustomGeologyLayerImages().isEmpty())) {
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

    /**
     * Validate resource setup.
     *
     * @param showDialog the show dialog parameter (boolean)
     * @return the resulting computation or state reference
     */
    public boolean validateResourceSetup(boolean showDialog) {
        List<String> errors = getValidationErrors();
        boolean isValid = errors.isEmpty();

        boolean isBiomeImport = radioImportBiome != null && radioImportBiome.isSelected();
        boolean biomeBad = isBiomeImport && (customBiomeImage == null || !org.ether.society.data.ImageMapLoader.validateMapImage(customBiomeImage).valid());
        if (loadBiomeBtn != null) loadBiomeBtn.setStyle(biomeBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        boolean hydroBad = radioImportHydro != null && radioImportHydro.isSelected() && (customHydroImage == null || !org.ether.society.data.ImageMapLoader.validateMapImage(customHydroImage).valid());
        if (loadHydroBtn != null) loadHydroBtn.setStyle(hydroBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        boolean geoBad = radioImportGeology != null && radioImportGeology.isSelected() && (customResourceImage == null || !org.ether.society.data.ImageMapLoader.validateMapImage(customResourceImage).valid()) && (geologySectionBuilder.getCustomGeologyLayerImages() == null || geologySectionBuilder.getCustomGeologyLayerImages().isEmpty());
        if (loadResourceBtn != null) loadResourceBtn.setStyle(geoBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        boolean climateBad = radioImportClimate != null && radioImportClimate.isSelected() && (customClimateImage == null || !org.ether.society.data.ImageMapLoader.validateMapImage(customClimateImage).valid());
        if (loadClimateBtn != null) loadClimateBtn.setStyle(climateBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        if (!isValid) {
            if (showDialog && validationWarningLabel != null && validationWarningBanner != null) {
                StringBuilder errorMsg = new StringBuilder();
                // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
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

    /**
     * Prepopulate geology tensors.
     *
     * @param planetKey the planet key parameter (String)
     */
    public void prepopulateGeologyTensors(String planetKey) {
        geologySectionBuilder.prepopulateGeologyTensors(planetKey, hostEpochYear());
    }

    public void prepopulateGeologyTensors(String planetKey, long year) {
        geologySectionBuilder.prepopulateGeologyTensors(planetKey, year);
    }

    public void prepopulateEarthGeologyTensors() {
        geologySectionBuilder.prepopulateEarthGeologyTensors();
    }

    private long hostEpochYear() {
        return (ecologyPresetBar != null && ecologyPresetBar.getPresetCombo() != null && ecologyPresetBar.getPresetCombo().getValue() != null)
                ? ecologyPresetBar.getPresetCombo().getValue().getAssociatedEpochYear() : 2026L;
    }

    private VBox createGeologyVectorAndLayersSection() {
        return geologySectionBuilder.buildGeologySection();
    }

    public Color getGeologyResourceColor(int layerIdx, double val) {
        return geologySectionBuilder.getGeologyResourceColor(layerIdx, val);
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
        return geologySectionBuilder.generateProceduralGeologyRasterImage(layerIdx, width, height);
    }

    public void exportGeologyTensor(int layerIdx) {
        geologySectionBuilder.exportGeologyTensor(layerIdx);
    }
    public boolean isDirty() {
        return ecologyPresetBar != null && ecologyPresetBar.isDirty();
    }

    /**
     * Prompt save if dirty.
     *
     * @param owner the owner parameter (javafx.stage.Window)
     * @return the resulting computation or state reference
     */
    public boolean promptSaveIfDirty(javafx.stage.Window owner) {
        if (ecologyPresetBar == null) return true;
        return ecologyPresetBar.promptSavePresetIfDirty(owner);
    }

    /**
     * Get preset bar.
     *
     * @return the resulting computation or state reference
     */
    public PresetControlBar<EcologyPreset> getPresetBar() {
        return ecologyPresetBar;
    }
}


