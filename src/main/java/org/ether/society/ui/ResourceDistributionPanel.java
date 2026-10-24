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
                @Override public void selectViewMode(int viewModeIndex) { ResourceDistributionPanel.this.selectViewMode(viewModeIndex); }
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE); }
                @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
                @Override public boolean isUpdatingFromPreset() { return isUpdatingFromPreset; }
                @Override public long getEpochYear() {
                    return (ecologyPresetBar != null && ecologyPresetBar.getPresetCombo() != null && ecologyPresetBar.getPresetCombo().getValue() != null)
                            ? ecologyPresetBar.getPresetCombo().getValue().getAssociatedEpochYear() : 2026L;
                }
            });
    // Hydrographic Domain & Freshwater Aquifers (Tab 2 Parity)
    private final org.ether.society.ui.resource.ResourceHydroSectionBuilder hydroSectionBuilder =
            new org.ether.society.ui.resource.ResourceHydroSectionBuilder(new org.ether.society.ui.resource.ResourceHydroSectionBuilder.HostContext() {
                @Override public void notifyParamChange() { if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged(); }
                @Override public void updatePreviewCanvas() { ResourceDistributionPanel.this.updatePreviewCanvas(); }
                @Override public void selectViewMode(int viewModeIndex) { ResourceDistributionPanel.this.selectViewMode(viewModeIndex); }
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE); }
                @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
                @Override public boolean isUpdatingFromPreset() { return isUpdatingFromPreset; }
                @Override public OnlineMapService getOnlineMapService() { return onlineMapService; }
                @Override public String getMapSourceComboValue() { return mapSourceCombo != null ? mapSourceCombo.getValue() : "earth"; }
                @Override public void setMapStatus(String status) { if (mapStatusLabel != null) mapStatusLabel.setText(status); }
            });
    private final org.ether.society.ui.resource.ResourcePreviewCanvasBuilder previewCanvasBuilder =
            new org.ether.society.ui.resource.ResourcePreviewCanvasBuilder(new org.ether.society.ui.resource.ResourcePreviewCanvasBuilder.HostContext() {
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE); }
                @Override public double getSeismicActivityValue() { return seismicActivitySlider != null ? seismicActivitySlider.getValue() : 4.5; }
                @Override public double getVolcanicActivityValue() { return volcanicActivitySlider != null ? volcanicActivitySlider.getValue() : 2.0; }
                @Override public boolean isRadioImportBiomeSelected() { return radioImportBiome != null && radioImportBiome.isSelected(); }
                @Override public Image getCustomBiomeImage() { return customBiomeImage; }
                @Override public boolean isRadioImportHydroSelected() { return hydroSectionBuilder.isRadioImportHydroSelected(); }
                @Override public Image getCustomHydroImage() { return hydroSectionBuilder.getCustomHydroImage(); }
                @Override public org.ether.society.ui.resource.ResourceGeologySectionBuilder getGeologySectionBuilder() { return geologySectionBuilder; }
                @Override public ProceduralGenerator getProceduralGenerator() { return generator; }
                @Override public ImageMapLoader getMapLoader() { return mapLoader; }
                @Override public javafx.scene.Node getHostNode() { return ResourceDistributionPanel.this; }
            });
    // Inherited Planet Preset & Context Controls
    private Label planetSectionHeader;
    private ComboBox<PlanetPreset> planetPresetCombo;
    private Label planetContextLabel;
    private Label thermoSynthesisBadge;
    private Button syncPlanetBtn;
    private Button autoDeriveEcologyBtn;
    private Button autoDeriveGeologyBtn;
    private Label biomeStatusLabel;
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
    // Custom Map Controls & Buttons
    private Label mapsSecHeader;
    private ComboBox<String> mapSourceCombo;
    private Label mapSourceRowLabel;
    private Label biomeFileLabel;
    private Label resourceFileLabel;
    private Label biomeMapRowLabel;
    private Label resourceMapRowLabel;
    private Label biomeFormatHintLabel;
    private Label geologyFormatHintLabel;
    private Button loadBiomeBtn;
    private Button clearBiomeBtn;
    private Button loadResourceBtn;
    private Button clearResourceBtn;
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
    private TextField climateSeedField;
    private TextField geologySeedField;
    // Domain Source ComboBoxes
    private ComboBox<String> biomeSourceCombo;
    private ComboBox<String> climateSourceCombo;
    private ComboBox<String> geologySourceCombo;
    // Section header labels for live i18n update
    private Label seedSecHeader;
    private Label biomeDomainSecHeader;
    private Label climateSummaryLabel;
    private Label climateDomainSecHeader;
    private Label geologyDomainSecHeader;
    private Button btnExportProvenanceManifest;
    private Button autoDeriveMasterBtn;
    private Button exportBiomeBtn;
    private Button exportGeologyBtn;
    private Label biomeSourcePromptLabel;
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
    // Seed & Random Controls
    private TextField seedField;
    private Button randSeedBtn;
    private Button regenBtn;
    private Button applyBtn;
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
                selectViewMode(1);
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
                String customHydroB64 = hydroSectionBuilder.getCustomHydroImage() != null ? ImageMapLoader.imageToBase64Png(hydroSectionBuilder.getCustomHydroImage()) : null;
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
                        hydroSectionBuilder.getFreshwaterAquiferSlider().getValue(),
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
        seismicActivitySlider = createSlider(0.0, 10.0, 2.5);
        volcanicActivitySlider = createSlider(0.0, 8.0, 1.5);
        seismicRowLabel = new Label(I18n.getOrDefault("resource.label.seismic", "Seismic & Tectonic Activity (Mag Richter):"));
        volcanicRowLabel = new Label(I18n.getOrDefault("resource.label.volcanic", "Global Volcanic Activity (VEI):"));
        crustalMetalRowLabel = new Label(I18n.getOrDefault("resource.label.industrial_metals", "Industrial Metal Reserves (Iron/Copper):"));
        preciousMetalRowLabel = new Label(I18n.getOrDefault("resource.label.precious_metals", "Precious Ores & REE (Gold/Pt):"));
        mantleHeatRowLabel = new Label(I18n.getOrDefault("resource.param.mantle_heat", "Mantle Heat Flux (mW/m²):"));
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
                selectViewMode(1);
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
            selectViewMode(1);
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
        VBox hydroDomainSection = hydroSectionBuilder.buildHydroSection();
        
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
        // --- Center / Right View: Delegated to ResourcePreviewCanvasBuilder ---
        VBox centerBox = previewCanvasBuilder.buildPreviewPane();
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
        hydroSectionBuilder.autoDeriveHydroFromPlanet();
        updateSummary();
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
                if (hydroSectionBuilder.getFreshwaterAquiferSlider() != null) hydroSectionBuilder.getFreshwaterAquiferSlider().setValue(1200.0);
                if (seismicActivitySlider != null) seismicActivitySlider.setValue(1.2);
                if (volcanicActivitySlider != null) volcanicActivitySlider.setValue(0.8);
            } else if (p.waterLevel() > 0.4) { // Ocean World
                terrestrialBiomassSlider.setValue(120.0);
                aquaticBiomassSlider.setValue(25.0);
                if (hydroSectionBuilder.getFreshwaterAquiferSlider() != null) hydroSectionBuilder.getFreshwaterAquiferSlider().setValue(45000.0);
            } else { // Earth-Like
                terrestrialBiomassSlider.setValue(450.0);
                soilCarbonSlider.setValue(1500.0);
                faunaBiomassSlider.setValue(2.0);
                aquaticBiomassSlider.setValue(6.0);
                crustalMetalSlider.setValue(80.0);
                preciousMetalSlider.setValue(1200.0);
                mantleHeatSlider.setValue(87.0);
                if (hydroSectionBuilder.getFreshwaterAquiferSlider() != null) hydroSectionBuilder.getFreshwaterAquiferSlider().setValue(15000.0);
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
        if (hydroSectionBuilder.getFreshwaterAquiferSlider() != null) hydroSectionBuilder.getFreshwaterAquiferSlider().setValue(p.freshwaterReserveKm3());
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
        hydroSectionBuilder.applyPresetHydro(p, planetKey, epochYear);
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
        selectViewMode(1);
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
                selectViewMode(1);
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
        if (hydroSectionBuilder.getHydroSourceCombo() != null) hydroSectionBuilder.getHydroSourceCombo().setValue(sourceKey);
        isUpdatingFromPreset = false;
        // Now trigger the full map-load cascade
        applyPresetMapSource(sourceKey);
    }
    private void applyPresetMapSource(String sourceKey) {
        if ("none".equals(sourceKey)) {
            customBiomeImage = null;
            customResourceImage = null;
            biomeFileLabel.setText(I18n.get("planet.map.none"));
            resourceFileLabel.setText(I18n.get("planet.map.none"));
            hydroSectionBuilder.applyPresetMapSource(sourceKey, "none");
            if (ecoCompatibilityLabel != null) {
                ecoCompatibilityLabel.setText(I18n.getOrDefault("resource.status.no_map", "ðŸª No external map loaded — Procedural mode active"));
                ecoCompatibilityLabel.getStyleClass().setAll("compatibility-neutral");
            }
            // Revert all domain radio buttons to procedural mode
            isUpdatingFromPreset = true;
            if (radioProcBiome != null) radioProcBiome.setSelected(true);
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
        if (radioImportGeology != null) radioImportGeology.setSelected(true);
        if (radioImportClimate != null) radioImportClimate.setSelected(true);
        isUpdatingFromPreset = false;
        String body = sourceKey.toLowerCase();
        if (biomeSourceCombo != null) biomeSourceCombo.setValue(body);
        if (geologySourceCombo != null) geologySourceCombo.setValue(body);
        // Load Biomes
        customBiomeImage = ImageMapLoader.loadMapImage(body + "_biomes.png");
        if (biomeFileLabel != null) biomeFileLabel.setText(getBiomeSourceDisplayName(body));
        hydroSectionBuilder.applyPresetMapSource(sourceKey, body);
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
    public void generateProceduralHydrography() {
        hydroSectionBuilder.generateProceduralHydrography();
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
    public String getHydroSourceDisplayName(String planetKey) {
        return hydroSectionBuilder.getHydroSourceDisplayName(planetKey);
    }
    public String getGeologySourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase(java.util.Locale.ROOT)) {
            case "mars" -> "🪨 Mars CRISM / GRS Mineral Maps";
            case "moon" -> "🪨 Moon Clementine / Kaguya Mineralogy";
            case "venus" -> "🪨 Venus Magellan Synthetic Aperture Radar Geology";
            case "mercury" -> "🪨 Mercury MDIS Compositional Units";
            default -> I18n.getOrDefault("resource.source.earth_geology", "🪨 Earth USGS MRDS / World Minerals");
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
    public void updatePreviewCanvas() {
        if (previewCanvasBuilder != null) {
            previewCanvasBuilder.updatePreviewCanvas();
        }
    }
    public void updateLegend() {
        if (previewCanvasBuilder != null) {
            previewCanvasBuilder.updateLegend();
        }
    }
    public void selectViewMode(int viewModeIndex) {
        if (previewCanvasBuilder != null) {
            previewCanvasBuilder.selectViewMode(viewModeIndex);
        }
    }
    public Color blendColors(Color base, Color overlay, double opacity) {
        return previewCanvasBuilder != null ? previewCanvasBuilder.blendColors(base, overlay, opacity) : overlay;
    }
    public Color getReliefShadeColor(double elevation, double waterLevel, double declivity) {
        return previewCanvasBuilder != null ? previewCanvasBuilder.getReliefShadeColor(elevation, waterLevel, declivity) : Color.GRAY;
    }
    public int getGeologyViewComboIndex(int layerIdx) {
        return previewCanvasBuilder != null ? previewCanvasBuilder.getGeologyViewComboIndex(layerIdx) : 3;
    }
    public int getViewModeIndex() {
        return previewCanvasBuilder != null ? previewCanvasBuilder.getViewModeIndex() : 0;
    }
    public ComboBox<String> getViewModeCombo() {
        return previewCanvasBuilder != null ? previewCanvasBuilder.getViewModeCombo() : null;
    }
    public Canvas getMapPreviewCanvas() {
        return previewCanvasBuilder != null ? previewCanvasBuilder.getMapPreviewCanvas() : null;
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
        Label summaryLabel = previewCanvasBuilder != null ? previewCanvasBuilder.getSummaryLabel() : null;
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
                hydroSectionBuilder.getFreshwaterAquiferSlider() != null ? hydroSectionBuilder.getFreshwaterAquiferSlider().getValue() : 15000.0
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
            if (climateSummaryLabel != null) climateSummaryLabel.setText(I18n.getOrDefault("resource.climate.summary", "ðŸŒ¡ï¸ Climate Model: Inherited from Tab 1 planet parameters (Temperature, Gradient, Pressure, Oâ‚‚, COâ‚‚, Albedo)"));
            if (climateDomainSecHeader != null) climateDomainSecHeader.setText(I18n.getOrDefault("resource.domain.climate", "CLIMATE & ATMOSPHERE DOMAIN"));
            if (geologyDomainSecHeader != null) geologyDomainSecHeader.setText(I18n.getOrDefault("resource.domain.geology", "GEOLOGY, TECTONICS & ORES DOMAIN"));
            if (floraSecHeader != null) floraSecHeader.setText(I18n.getOrDefault("resource.section.flora", "PLANT ECOLOGY & SOILS (METRICS)"));
            if (faunaSecHeader != null) faunaSecHeader.setText(I18n.getOrDefault("resource.section.fauna", "ANIMAL & AQUATIC BIOMASS (METRICS)"));
            if (mineralSecHeader != null) mineralSecHeader.setText(I18n.getOrDefault("resource.section.minerals", "GEOLOGY & HYDROLOGY (METRICS)"));
            if (headerLabel != null) headerLabel.setText(I18n.getOrDefault("resource.title", "RESOURCE DISTRIBUTION & ECOLOGY (SCIENTIFIC METRICS)"));
            if (previewCanvasBuilder != null) previewCanvasBuilder.updateTexts();
            if (syncPlanetBtn != null) syncPlanetBtn.setText(I18n.getOrDefault("resource.btn.sync_planet", "🔄 Synchronize with Tab 1 Planet"));
            if (autoDeriveEcologyBtn != null) autoDeriveEcologyBtn.setText(I18n.getOrDefault("resource.btn.auto_derive_eco", "⚡ Auto-derive Ecology & Biomes from Physics"));
            if (autoDeriveGeologyBtn != null) autoDeriveGeologyBtn.setText(I18n.getOrDefault("resource.btn.auto_derive_geo", "🌋 Calculate Tectonics & Metals from Mantle"));
            if (mapsSecHeader != null) mapsSecHeader.setText(I18n.getOrDefault("resource.section.imports", "TAB 1 ECOLOGY, GEOLOGY & HYDROLOGY IMPORTS"));
            if (mapSourceRowLabel != null) mapSourceRowLabel.setText(I18n.getOrDefault("resource.param.map_source", "Celestial Body Model / Satellite:"));
            if (biomeMapRowLabel != null) biomeMapRowLabel.setText(I18n.getOrDefault("resource.param.biome_map", "Biome / Extraterrestrial Vegetation Map (PNG):"));
            if (resourceMapRowLabel != null) resourceMapRowLabel.setText(I18n.getOrDefault("resource.param.resource_map", "Geological & Multi-Channel Ore Map (PNG):"));
            if (climateMapRowLabel != null) climateMapRowLabel.setText(I18n.getOrDefault("planet.map.climate", "Climate Map (Thermal / RGB):"));
            if (rainfallMapRowLabel != null) rainfallMapRowLabel.setText(I18n.getOrDefault("planet.map.rainfall", "Precipitation Map (Humidity):"));
            if (seasonalityMapRowLabel != null) seasonalityMapRowLabel.setText(I18n.getOrDefault("planet.map.seasonality", "Seasonality / Variance Map:"));
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
            if (seismicRowLabel != null) seismicRowLabel.setText(I18n.getOrDefault("resource.param.seismic", "Seismic & Tectonic Activity (Mag Richter):"));
            if (volcanicRowLabel != null) volcanicRowLabel.setText(I18n.getOrDefault("resource.param.volcanic", "Global Volcanic Activity (VEI):"));
            if (radioProcBiome != null) radioProcBiome.setText(I18n.getOrDefault("resource.mode.procedural_biome", "▶ Procedural Biomes"));
            if (radioImportBiome != null) radioImportBiome.setText(I18n.getOrDefault("resource.mode.import_biome", "📂 Biome Map (PNG)"));
            if (radioProcClimate != null) radioProcClimate.setText(I18n.getOrDefault("resource.mode.procedural_climate", "▶ Procedural Climate"));
            if (radioImportClimate != null) radioImportClimate.setText(I18n.getOrDefault("resource.mode.import_climate", "📂 Climate Maps (PNG)"));
            if (radioProcGeology != null) radioProcGeology.setText(I18n.getOrDefault("resource.mode.procedural_geology", "▶ Procedural Geology"));
            if (radioImportGeology != null) radioImportGeology.setText(I18n.getOrDefault("resource.mode.import_geology", "📂 Geological Map (PNG)"));
            if (mapSourceCombo != null && mapSourceCombo.getCellFactory() != null) mapSourceCombo.setButtonCell(mapSourceCombo.getCellFactory().call(null));
            if (biomeSourceCombo != null && biomeSourceCombo.getCellFactory() != null) biomeSourceCombo.setButtonCell(biomeSourceCombo.getCellFactory().call(null));
            if (climateSourceCombo != null && climateSourceCombo.getCellFactory() != null) climateSourceCombo.setButtonCell(climateSourceCombo.getCellFactory().call(null));
            if (geologySourceCombo != null && geologySourceCombo.getCellFactory() != null) geologySourceCombo.setButtonCell(geologySourceCombo.getCellFactory().call(null));
            if (loadBiomeBtn != null) loadBiomeBtn.setText(I18n.get("planet.map.btn_load"));
            if (loadResourceBtn != null) loadResourceBtn.setText(I18n.get("planet.map.btn_load"));
            if (loadClimateBtn != null) loadClimateBtn.setText(I18n.get("planet.map.btn_load"));
            if (loadRainfallBtn != null) loadRainfallBtn.setText(I18n.get("planet.map.btn_load"));
            if (loadSeasonalityBtn != null) loadSeasonalityBtn.setText(I18n.get("planet.map.btn_load"));
            if (customBiomeImage == null && biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
            if (customResourceImage == null && resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
            if (customClimateImage == null && climateFileLabel != null) climateFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
            if (customRainfallImage == null && rainfallFileLabel != null) rainfallFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
            if (customSeasonalityImage == null && seasonalityFileLabel != null) seasonalityFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
            if (biomeFormatHintLabel != null) biomeFormatHintLabel.setText(I18n.getOrDefault("resource.format.biome_hint", "PNG / JPEG (2:1 equirectangular projection):\n  Biome image or vegetation cover (grayscale or RGB)."));
            if (geologyFormatHintLabel != null) geologyFormatHintLabel.setText(I18n.getOrDefault("resource.format.geology_hint", "Multi-channel PNG (2:1 equirectangular projection):\n • R Channel (Red) = Industrial crustal metals (Iron/Copper)\n • G Channel (Green) = Precious ores & REE (Gold/Pt)\n • B Channel (Blue) = Mantle heat flux & aquifers"));
            if (climateSummaryLabel != null) climateSummaryLabel.setText(I18n.getOrDefault("resource.climate.summary", "ðŸŒ¡ï¸ Climate Model: Inherited from Tab 1 planet parameters (Temperature, Gradient, Pressure, Oâ‚‚, COâ‚‚, Albedo)"));
            if (applyBtn != null) applyBtn.setText(I18n.getOrDefault("resource.btn.apply", "✅ Apply scientific distribution to simulation"));
            if (geologySectionBuilder != null) {
                geologySectionBuilder.updateTexts();
            }
            if (hydroSectionBuilder != null) {
                hydroSectionBuilder.updateTexts();
            }
            if (btnExportProvenanceManifest != null) {
                btnExportProvenanceManifest.setText(I18n.getOrDefault("resource.btn.provenance_manifest", "🔒 SHA-256 Provenance Manifest"));
                btnExportProvenanceManifest.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.gen_provenance", "Generate a cryptographic JSON manifest (provenance.json) containing SHA-256 hashes of geological datasets.")));
            }
            if (autoDeriveMasterBtn != null) autoDeriveMasterBtn.setText(I18n.getOrDefault("resource.btn.auto_derive_all", "⚡ Auto-derive All from Physics (Biomes, Aquifers & Geology)"));
            if (exportBiomeBtn != null) exportBiomeBtn.setText(I18n.getOrDefault("resource.btn.export_biome", "📤 Export Biome Map (PNG + WorldFile)"));
            if (exportGeologyBtn != null) exportGeologyBtn.setText(I18n.getOrDefault("resource.btn.export_geology", "📤 Export Geological Map (PNG + WorldFile)"));
            if (biomeSourcePromptLabel != null) biomeSourcePromptLabel.setText(I18n.getOrDefault("resource.param.map_source", "Celestial Body / Source:"));
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
        double totalAquifer = hydroSectionBuilder.getFreshwaterAquiferSlider() != null ? hydroSectionBuilder.getFreshwaterAquiferSlider().getValue() : 15000.0;
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
        if (hydroSectionBuilder.isRadioImportHydroSelected()) {
            if (hydroSectionBuilder.getCustomHydroImage() == null) {
                errors.add(I18n.getOrDefault("resource.validation.missing_hydro_map", "Missing hydrographic map in import mode (Tab 2)."));
            } else {
                org.ether.society.data.ImageMapLoader.ImageValidationResult val = org.ether.society.data.ImageMapLoader.validateMapImage(hydroSectionBuilder.getCustomHydroImage());
                if (!val.valid()) {
                    errors.add(I18n.getOrDefault("resource.validation.invalid_hydro_map", "Hydrographic map incompatible (Tab 2): " ) + val.message());
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
        boolean hydroBad = hydroSectionBuilder.isRadioImportHydroSelected() && (hydroSectionBuilder.getCustomHydroImage() == null || !org.ether.society.data.ImageMapLoader.validateMapImage(hydroSectionBuilder.getCustomHydroImage()).valid());
        if (hydroSectionBuilder.getLoadHydroBtn() != null) hydroSectionBuilder.getLoadHydroBtn().setStyle(hydroBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");
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
        return hydroSectionBuilder.generateProceduralHydroRasterImage(width, height);
    }
    public void exportHydroMap() {
        hydroSectionBuilder.exportHydroMap();
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
    public Image getCustomHydroImage() { return hydroSectionBuilder.getCustomHydroImage(); }
    public void setCustomHydroImage(Image img) { hydroSectionBuilder.setCustomHydroImage(img); }
    public boolean isRadioImportHydroSelected() { return hydroSectionBuilder.isRadioImportHydroSelected(); }
    public Slider getFreshwaterAquiferSlider() { return hydroSectionBuilder.getFreshwaterAquiferSlider(); }
    public TextField getHydroSeedField() { return hydroSectionBuilder.getHydroSeedField(); }
    public ComboBox<String> getHydroSourceCombo() { return hydroSectionBuilder.getHydroSourceCombo(); }
    public Label getHydroFileLabel() { return hydroSectionBuilder.getHydroFileLabel(); }
    public Button getLoadHydroBtn() { return hydroSectionBuilder.getLoadHydroBtn(); }
    public org.ether.society.ui.resource.ResourceHydroSectionBuilder getHydroSectionBuilder() { return hydroSectionBuilder; }
}
