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
    private Image customResourceImage;
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
                @Override public String getMapSourceComboValue() { return "earth"; }
                @Override public void setMapStatus(String status) {}
            });
    // Biome & Flora Domain (Tab 2 Parity)
    private final org.ether.society.ui.resource.ResourceBiomeSectionBuilder biomeSectionBuilder =
            new org.ether.society.ui.resource.ResourceBiomeSectionBuilder(new org.ether.society.ui.resource.ResourceBiomeSectionBuilder.HostContext() {
                @Override public void notifyParamChange() { if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged(); }
                @Override public void updatePreviewCanvas() { ResourceDistributionPanel.this.updatePreviewCanvas(); }
                @Override public void updateSummary() { ResourceDistributionPanel.this.updateSummary(); }
                @Override public void selectViewMode(int viewModeIndex) { ResourceDistributionPanel.this.selectViewMode(viewModeIndex); }
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE); }
                @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
                @Override public boolean isUpdatingFromPreset() { return isUpdatingFromPreset; }
                @Override public OnlineMapService getOnlineMapService() { return onlineMapService; }
                @Override public String getMapSourceComboValue() { return "earth"; }
                @Override public void setMapStatus(String status) {}
                @Override public void applyPresetMapSource(String sourceKey) { ResourceDistributionPanel.this.applyPresetMapSource(sourceKey); }
            });
    // Climate & Atmosphere Domain Maps (Tab 2 Parity)
    private final org.ether.society.ui.resource.ResourceClimateSectionBuilder climateSectionBuilder =
            new org.ether.society.ui.resource.ResourceClimateSectionBuilder(new org.ether.society.ui.resource.ResourceClimateSectionBuilder.HostContext() {
                @Override public void notifyParamChange() { if (!isUpdatingFromPreset && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged(); }
                @Override public void updatePreviewCanvas() { ResourceDistributionPanel.this.updatePreviewCanvas(); }
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE); }
                @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
                @Override public boolean isUpdatingFromPreset() { return isUpdatingFromPreset; }
                @Override public OnlineMapService getOnlineMapService() { return onlineMapService; }
                @Override public String getMapSourceComboValue() { return "earth"; }
                @Override public void setMapStatus(String status) {}
            });
    private final org.ether.society.ui.resource.ResourcePreviewCanvasBuilder previewCanvasBuilder =
            new org.ether.society.ui.resource.ResourcePreviewCanvasBuilder(new org.ether.society.ui.resource.ResourcePreviewCanvasBuilder.HostContext() {
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE); }
                @Override public double getSeismicActivityValue() { return seismicActivitySlider != null ? seismicActivitySlider.getValue() : 4.5; }
                @Override public double getVolcanicActivityValue() { return volcanicActivitySlider != null ? volcanicActivitySlider.getValue() : 2.0; }
                @Override public boolean isRadioImportBiomeSelected() { return biomeSectionBuilder.isRadioImportBiomeSelected(); }
                @Override public Image getCustomBiomeImage() { return biomeSectionBuilder.getCustomBiomeImage(); }
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
    private Button autoDeriveGeologyBtn;
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
    private Slider crustalMetalSlider;        // Gt (Industrial base metals: Fe, Cu, Al)
    private Slider preciousMetalSlider;       // Mt (Precious & rare earth ores: Au, Pt, REE)
    private Slider mantleHeatSlider;          // mW/m² (Mantle heat flow & geothermal/tectonic index)
    // Legacy resource image & controls
    private Label resourceFileLabel;
    private Button loadResourceBtn;
    // Seismic & Volcanic Activity Sliders for Tab 2
    private Slider seismicActivitySlider;
    private Slider volcanicActivitySlider;
    private Label seismicRowLabel;
    private Label volcanicRowLabel;
    private Label ecoCompatibilityLabel;
    private VBox validationWarningBanner;
    private Label validationWarningLabel;
    // Section & Row Labels for i18n
    private Label headerLabel;
    private Label mineralSecHeader;
    private Label crustalMetalRowLabel;
    private Label preciousMetalRowLabel;
    private Label mantleHeatRowLabel;
    // Seed & Random Controls
    private TextField seedField;
    private Button applyBtn;
    private Button autoDeriveMasterBtn;
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
            if (!bodyKey.equalsIgnoreCase(activeLoadedBodyKey) || biomeSectionBuilder.getCustomBiomeImage() == null) {
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
                String customBiomeB64 = biomeSectionBuilder.getCustomBiomeImage() != null ? ImageMapLoader.imageToBase64Png(biomeSectionBuilder.getCustomBiomeImage()) : null;
                String customResourceB64 = customResourceImage != null ? ImageMapLoader.imageToBase64Png(customResourceImage) : null;
                String customHydroB64 = hydroSectionBuilder.getCustomHydroImage() != null ? ImageMapLoader.imageToBase64Png(hydroSectionBuilder.getCustomHydroImage()) : null;
                String customClimateB64 = climateSectionBuilder.getCustomClimateImage() != null ? ImageMapLoader.imageToBase64Png(climateSectionBuilder.getCustomClimateImage()) : null;
                String customRainfallB64 = climateSectionBuilder.getCustomRainfallImage() != null ? ImageMapLoader.imageToBase64Png(climateSectionBuilder.getCustomRainfallImage()) : null;
                String customSeasonalityB64 = climateSectionBuilder.getCustomSeasonalityImage() != null ? ImageMapLoader.imageToBase64Png(climateSectionBuilder.getCustomSeasonalityImage()) : null;
                PlanetPreset currentPlanet = planetPresetSupplier != null ? planetPresetSupplier.get() : activePlanetPreset;
                if (currentPlanet == null) currentPlanet = PlanetPreset.EARTH_LIKE;
                String planetName = currentPlanet.name() != null ? currentPlanet.name() : PlanetPreset.EARTH_LIKE.name();
                EcologyPreset custom = new EcologyPreset(
                        name,
                        planetName,
                        currentPlanet,
                        biomeSectionBuilder.getTerrestrialBiomassSlider().getValue(),
                        biomeSectionBuilder.getSoilCarbonSlider().getValue(),
                        biomeSectionBuilder.getFaunaBiomassSlider().getValue(),
                        biomeSectionBuilder.getAquaticBiomassSlider().getValue(),
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
        // Legacy resource loading controls
        resourceFileLabel = new Label(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
        resourceFileLabel.getStyleClass().add("value-label");
        loadResourceBtn = new Button(I18n.get("planet.map.btn_load"));
        loadResourceBtn.getStyleClass().add("button-secondary");
        loadResourceBtn.setOnAction(e -> loadCustomResourceMap());
        // Note: Domain controls are assembled inside their respective domain section builders.
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
        VBox biomeDomainSection = biomeSectionBuilder.buildBiomeSection();
        seedField = biomeSectionBuilder.getBiomeSeedField(); // EcologyPreset compat
        
        // --- DOMAINE 2 : HYDROGRAPHIE & EAU DOUCE ---
        VBox hydroDomainSection = hydroSectionBuilder.buildHydroSection();
        
        // --- DOMAINE 3 : CLIMAT & ATMOSPHÈRE ---
        VBox climateDomainSection = climateSectionBuilder.buildClimateSection();
        
        // --- DOMAIN 4: GEOLOGY, TECTONICS & MINERALS ---
        VBox geologyDomainSection = createGeologyVectorAndLayersSection();
        controlsBox.getChildren().addAll(
                headerLabel,
                validationWarningBanner,
                ecologyPresetBar,
                ecologyDescriptionArea,
                planetSection,
                biomeDomainSection,
                hydroDomainSection,
                climateDomainSection,
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
        biomeSectionBuilder.autoDeriveEcologyFromPlanet();
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
                if (biomeSectionBuilder.getTerrestrialBiomassSlider() != null) biomeSectionBuilder.getTerrestrialBiomassSlider().setValue(10.0);
                if (biomeSectionBuilder.getSoilCarbonSlider() != null) biomeSectionBuilder.getSoilCarbonSlider().setValue(50.0);
                if (biomeSectionBuilder.getFaunaBiomassSlider() != null) biomeSectionBuilder.getFaunaBiomassSlider().setValue(0.01);
                crustalMetalSlider.setValue(150.0);
                mantleHeatSlider.setValue(25.0);
                if (seismicActivitySlider != null) seismicActivitySlider.setValue(1.0);
                if (volcanicActivitySlider != null) volcanicActivitySlider.setValue(0.5);
            } else if (p.waterLevel() < -0.2) { // Arid / Mars
                if (biomeSectionBuilder.getTerrestrialBiomassSlider() != null) biomeSectionBuilder.getTerrestrialBiomassSlider().setValue(50.0);
                if (biomeSectionBuilder.getSoilCarbonSlider() != null) biomeSectionBuilder.getSoilCarbonSlider().setValue(200.0);
                if (biomeSectionBuilder.getAquaticBiomassSlider() != null) biomeSectionBuilder.getAquaticBiomassSlider().setValue(0.2);
                if (hydroSectionBuilder.getFreshwaterAquiferSlider() != null) hydroSectionBuilder.getFreshwaterAquiferSlider().setValue(1200.0);
                if (seismicActivitySlider != null) seismicActivitySlider.setValue(1.2);
                if (volcanicActivitySlider != null) volcanicActivitySlider.setValue(0.8);
            } else if (p.waterLevel() > 0.4) { // Ocean World
                if (biomeSectionBuilder.getTerrestrialBiomassSlider() != null) biomeSectionBuilder.getTerrestrialBiomassSlider().setValue(120.0);
                if (biomeSectionBuilder.getAquaticBiomassSlider() != null) biomeSectionBuilder.getAquaticBiomassSlider().setValue(25.0);
                if (hydroSectionBuilder.getFreshwaterAquiferSlider() != null) hydroSectionBuilder.getFreshwaterAquiferSlider().setValue(45000.0);
            } else { // Earth-Like
                if (biomeSectionBuilder.getTerrestrialBiomassSlider() != null) biomeSectionBuilder.getTerrestrialBiomassSlider().setValue(450.0);
                if (biomeSectionBuilder.getSoilCarbonSlider() != null) biomeSectionBuilder.getSoilCarbonSlider().setValue(1500.0);
                if (biomeSectionBuilder.getFaunaBiomassSlider() != null) biomeSectionBuilder.getFaunaBiomassSlider().setValue(2.0);
                if (biomeSectionBuilder.getAquaticBiomassSlider() != null) biomeSectionBuilder.getAquaticBiomassSlider().setValue(6.0);
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
        if (biomeSectionBuilder.getTerrestrialBiomassSlider() != null) biomeSectionBuilder.getTerrestrialBiomassSlider().setValue(p.terrestrialBiomassGtC());
        if (biomeSectionBuilder.getSoilCarbonSlider() != null) biomeSectionBuilder.getSoilCarbonSlider().setValue(p.soilOrganicCarbonGtC());
        if (biomeSectionBuilder.getFaunaBiomassSlider() != null) biomeSectionBuilder.getFaunaBiomassSlider().setValue(p.faunaBiomassGtC());
        if (biomeSectionBuilder.getAquaticBiomassSlider() != null) biomeSectionBuilder.getAquaticBiomassSlider().setValue(p.aquaticBiomassGtC());
        crustalMetalSlider.setValue(p.crustalMetalOresGt());
        preciousMetalSlider.setValue(p.preciousMetalOresMt());
        mantleHeatSlider.setValue(p.mantleHeatFlowMwM2());
        if (hydroSectionBuilder.getFreshwaterAquiferSlider() != null) hydroSectionBuilder.getFreshwaterAquiferSlider().setValue(p.freshwaterReserveKm3());
        if (p.seed() != 0 && seedField != null) {
            seedField.setText(String.valueOf(p.seed()));
        }
        String planetKey = p.getCanonicalPlanet();
        long epochYear = p.getAssociatedEpochYear();
        biomeSectionBuilder.applyPresetBiome(p, planetKey, epochYear);
        if (p.customResourceBase64() != null) {
            customResourceImage = ImageMapLoader.base64PngToImage(p.customResourceBase64());
            if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("planet.preset.resources", "🪨 Preset Resources"));
        } else if ("earth".equals(planetKey)) {
            String yearStr = (epochYear <= 0) ? Math.abs(epochYear) + " BP" : epochYear + " AD";
            if (resourceFileLabel != null) resourceFileLabel.setText(getGeologySourceDisplayName("earth") + " (" + yearStr + ")");
            prepopulateGeologyTensors("earth", epochYear);
        } else if (planetKey != null && !"none".equals(planetKey)) {
            if (resourceFileLabel != null) resourceFileLabel.setText(getGeologySourceDisplayName(planetKey));
            prepopulateGeologyTensors(planetKey, epochYear);
        } else {
            customResourceImage = null;
            if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
        }
        hydroSectionBuilder.applyPresetHydro(p, planetKey, epochYear);
        climateSectionBuilder.applyPresetClimate(p, planetKey, epochYear);
        selectViewMode(1);
        if (ecologyPresetBar != null) {
            ecologyPresetBar.markClean(p);
        }
        isUpdatingFromPreset = false;
        updateSummary();
        updateLegend();
        updatePreviewCanvas();
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
            applyPresetMapSource("none");
            return;
        }
        isUpdatingFromPreset = true;
        if (biomeSectionBuilder.getBiomeSourceCombo() != null) biomeSectionBuilder.getBiomeSourceCombo().setValue(sourceKey);
        if (hydroSectionBuilder.getHydroSourceCombo() != null) hydroSectionBuilder.getHydroSourceCombo().setValue(sourceKey);
        if (climateSectionBuilder.getClimateSourceCombo() != null) climateSectionBuilder.getClimateSourceCombo().setValue(sourceKey);
        isUpdatingFromPreset = false;
        applyPresetMapSource(sourceKey);
    }
    private void applyPresetMapSource(String sourceKey) {
        if ("none".equals(sourceKey)) {
            customResourceImage = null;
            biomeSectionBuilder.applyPresetMapSource(sourceKey, "none");
            if (resourceFileLabel != null) resourceFileLabel.setText(I18n.get("planet.map.none"));
            hydroSectionBuilder.applyPresetMapSource(sourceKey, "none");
            climateSectionBuilder.applyPresetMapSource(sourceKey, "none");
            if (ecoCompatibilityLabel != null) {
                ecoCompatibilityLabel.setText(I18n.getOrDefault("resource.status.no_map", "🪐 No external map loaded — Procedural mode active"));
                ecoCompatibilityLabel.getStyleClass().setAll("compatibility-neutral");
            }
            updatePreviewCanvas();
            updateSummary();
            return;
        }
        if (!validateTerrainMapCompatibility(sourceKey)) {
            return;
        }
        String body = sourceKey.toLowerCase();
        biomeSectionBuilder.applyPresetMapSource(sourceKey, body);
        hydroSectionBuilder.applyPresetMapSource(sourceKey, body);
        climateSectionBuilder.applyPresetMapSource(sourceKey, body);
        if (resourceFileLabel != null) resourceFileLabel.setText(getGeologySourceDisplayName(body));
        prepopulateGeologyTensors(body);
        if (ecoCompatibilityLabel != null) {
            ecoCompatibilityLabel.setText(String.format("🪐 %s authentic maps loaded (Biomes, Hydro, Climate & 9 Geology Tensors)", body.toUpperCase()));
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
        biomeSectionBuilder.fetchOnlineSatelliteData();
    }
    private void exportMapsWithWorldFiles() {
        org.ether.society.ui.export.ResourceDistributionRasterExporter.exportMapsWithWorldFiles(
                getScene() != null ? getScene().getWindow() : null,
                getViewModeIndex(),
                activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE),
                seismicActivitySlider != null ? seismicActivitySlider.getValue() : null,
                volcanicActivitySlider != null ? volcanicActivitySlider.getValue() : null,
                generator,
                mapLoader,
                (layerIdx, lon, lat, planet) -> sampleProceduralGeologyTensor(layerIdx, lon, lat, planet),
                (layerIdx, val) -> getGeologyResourceColor(layerIdx, val)
        );
    }
    private void showEcologyImportFormatHelp() {
        org.ether.society.ui.export.ResourceDistributionRasterExporter.showEcologyImportFormatHelp();
    }
    public String getBiomeSourceDisplayName(String planetKey) {
        return biomeSectionBuilder.getBiomeSourceDisplayName(planetKey);
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
                biomeSectionBuilder.getTerrestrialBiomassSlider() != null ? biomeSectionBuilder.getTerrestrialBiomassSlider().getValue() : 450.0,
                biomeSectionBuilder.getSoilCarbonSlider() != null ? biomeSectionBuilder.getSoilCarbonSlider().getValue() : 1500.0,
                biomeSectionBuilder.getFaunaBiomassSlider() != null ? biomeSectionBuilder.getFaunaBiomassSlider().getValue() : 2.0,
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
            if (mineralSecHeader != null) mineralSecHeader.setText(I18n.getOrDefault("resource.section.minerals", "GEOLOGY & HYDROLOGY (METRICS)"));
            if (syncPlanetBtn != null) syncPlanetBtn.setText(I18n.getOrDefault("resource.btn.sync_planet", "🔄 Synchronize with Tab 1 Planet"));
            if (crustalMetalRowLabel != null) crustalMetalRowLabel.setText(I18n.getOrDefault("resource.param.crustal_metal", "Crustal Metal Wealth (Gt):"));
            if (preciousMetalRowLabel != null) preciousMetalRowLabel.setText(I18n.getOrDefault("resource.param.precious_metal", "Precious Metal Concentration (Mt):"));
            if (mantleHeatRowLabel != null) mantleHeatRowLabel.setText(I18n.getOrDefault("resource.param.mantle_heat", "Mantle Heat Flux (mW/m²):"));
            if (seismicRowLabel != null) seismicRowLabel.setText(I18n.getOrDefault("resource.param.seismic", "Seismic & Tectonic Activity (Mag Richter):"));
            if (volcanicRowLabel != null) volcanicRowLabel.setText(I18n.getOrDefault("resource.param.volcanic", "Global Volcanic Activity (VEI):"));
            if (loadResourceBtn != null) loadResourceBtn.setText(I18n.get("planet.map.btn_load"));
            if (customResourceImage == null && resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
            if (applyBtn != null) applyBtn.setText(I18n.getOrDefault("resource.btn.apply", "✅ Apply scientific distribution to simulation"));
            if (autoDeriveMasterBtn != null) autoDeriveMasterBtn.setText(I18n.getOrDefault("resource.btn.auto_derive_all", "⚡ Auto-derive All from Physics (Biomes, Aquifers & Geology)"));
            if (previewCanvasBuilder != null) previewCanvasBuilder.updateTexts();
            if (biomeSectionBuilder != null) biomeSectionBuilder.updateTexts();
            if (hydroSectionBuilder != null) hydroSectionBuilder.updateTexts();
            if (climateSectionBuilder != null) climateSectionBuilder.updateTexts();
            if (geologySectionBuilder != null) geologySectionBuilder.updateTexts();
            updateSummary();
            updatePlanetContextDisplay();
            updateLegend();
        } finally {
            isUpdatingFromPreset = oldUpdating;
        }
    }
    private void applyResourceDistribution() {
        if (activeCells == null || activeCells.isEmpty()) return;
        org.ether.society.ui.resource.ResourceSimulationCoupler.CouplingParameters params =
                new org.ether.society.ui.resource.ResourceSimulationCoupler.CouplingParameters(
                        biomeSectionBuilder.getTerrestrialBiomassSlider().getValue(),
                        biomeSectionBuilder.getSoilCarbonSlider().getValue(),
                        biomeSectionBuilder.getFaunaBiomassSlider().getValue(),
                        biomeSectionBuilder.getAquaticBiomassSlider().getValue(),
                        crustalMetalSlider.getValue(),
                        preciousMetalSlider.getValue(),
                        mantleHeatSlider.getValue(),
                        hydroSectionBuilder.getFreshwaterAquiferSlider() != null ? hydroSectionBuilder.getFreshwaterAquiferSlider().getValue() : 15000.0,
                        activePlanetPreset != null ? activePlanetPreset : PlanetPreset.EARTH_LIKE,
                        biomeSectionBuilder.isRadioImportBiomeSelected() ? biomeSectionBuilder.getCustomBiomeImage() : null,
                        customResourceImage
                );
        org.ether.society.ui.resource.ResourceSimulationCoupler.applyResourceDistribution(
                activeCells,
                params,
                generator,
                mapLoader,
                (layerIdx, lon, lat, plPreset) -> sampleProceduralGeologyTensor(layerIdx, lon, lat, plPreset)
        );
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
    public List<String> getValidationErrors() {
        return org.ether.society.ui.resource.ResourceValidationHelper.getValidationErrors(
                biomeSectionBuilder,
                hydroSectionBuilder,
                climateSectionBuilder,
                geologySectionBuilder,
                geologySectionBuilder.isAnyGeologyImportActive(),
                customResourceImage
        );
    }
    public boolean validateResourceSetup(boolean showDialog) {
        List<String> errors = getValidationErrors();
        return org.ether.society.ui.resource.ResourceValidationHelper.applyValidationVisuals(
                errors,
                showDialog,
                biomeSectionBuilder,
                hydroSectionBuilder,
                climateSectionBuilder,
                geologySectionBuilder,
                geologySectionBuilder.isAnyGeologyImportActive(),
                customResourceImage,
                loadResourceBtn,
                validationWarningBanner,
                validationWarningLabel
        );
    }
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
        return biomeSectionBuilder.generateProceduralBiomeRasterImage(width, height);
    }
    public void exportBiomeMap() {
        biomeSectionBuilder.exportBiomeMap();
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
    public Image getCustomClimateImage() { return climateSectionBuilder.getCustomClimateImage(); }
    public void setCustomClimateImage(Image img) { climateSectionBuilder.setCustomClimateImage(img); }
    public Image getCustomRainfallImage() { return climateSectionBuilder.getCustomRainfallImage(); }
    public void setCustomRainfallImage(Image img) { climateSectionBuilder.setCustomRainfallImage(img); }
    public Image getCustomSeasonalityImage() { return climateSectionBuilder.getCustomSeasonalityImage(); }
    public void setCustomSeasonalityImage(Image img) { climateSectionBuilder.setCustomSeasonalityImage(img); }
    public boolean isRadioImportClimateSelected() { return climateSectionBuilder.isRadioImportClimateSelected(); }
    public TextField getClimateSeedField() { return climateSectionBuilder.getClimateSeedField(); }
    public ComboBox<String> getClimateSourceCombo() { return climateSectionBuilder.getClimateSourceCombo(); }
    public org.ether.society.ui.resource.ResourceClimateSectionBuilder getClimateSectionBuilder() { return climateSectionBuilder; }
    public Image getCustomBiomeImage() { return biomeSectionBuilder.getCustomBiomeImage(); }
    public void setCustomBiomeImage(Image img) { biomeSectionBuilder.setCustomBiomeImage(img); }
    public boolean isRadioImportBiomeSelected() { return biomeSectionBuilder.isRadioImportBiomeSelected(); }
    public Slider getTerrestrialBiomassSlider() { return biomeSectionBuilder.getTerrestrialBiomassSlider(); }
    public Slider getSoilCarbonSlider() { return biomeSectionBuilder.getSoilCarbonSlider(); }
    public Slider getFaunaBiomassSlider() { return biomeSectionBuilder.getFaunaBiomassSlider(); }
    public Slider getAquaticBiomassSlider() { return biomeSectionBuilder.getAquaticBiomassSlider(); }
    public TextField getBiomeSeedField() { return biomeSectionBuilder.getBiomeSeedField(); }
    public ComboBox<String> getBiomeSourceCombo() { return biomeSectionBuilder.getBiomeSourceCombo(); }
    public org.ether.society.ui.resource.ResourceBiomeSectionBuilder getBiomeSectionBuilder() { return biomeSectionBuilder; }
}
