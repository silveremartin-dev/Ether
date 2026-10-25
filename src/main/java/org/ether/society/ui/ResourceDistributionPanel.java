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
import org.ether.society.model.EcologyPreset;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.generation.SimplexNoise;
import org.ether.society.ui.resource.ResourcePlanetSectionBuilder;
import org.ether.society.ui.resource.ResourceMapExportHandler;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    private List<H3Cell> activeCells;
    private PlanetPreset activePlanetPreset;
    private final Consumer<List<H3Cell>> onResourcesAppliedCallback;
    private final ImageMapLoader mapLoader = new ImageMapLoader();
    private final OnlineMapService onlineMapService = new OnlineMapService();
    private final ProceduralGenerator generator = new ProceduralGenerator();

    private Image customResourceImage;

    // Geological & Energy Tensor Maps (Tab 2 Parity with Tab 1 & Tab 3)
    private final org.ether.society.ui.resource.ResourceGeologySectionBuilder geologySectionBuilder =
            new org.ether.society.ui.resource.ResourceGeologySectionBuilder(new org.ether.society.ui.resource.ResourceGeologySectionBuilder.HostContext() {
                @Override public void notifyParamChange() { if (!presetManager.isUpdatingFromPreset() && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged(); }
                @Override public void updatePreviewCanvas() { ResourceDistributionPanel.this.updatePreviewCanvas(); }
                @Override public int getGeologyViewComboIndex(int layerIdx) { return ResourceDistributionPanel.this.getGeologyViewComboIndex(layerIdx); }
                @Override public void selectViewMode(int viewModeIndex) { ResourceDistributionPanel.this.selectViewMode(viewModeIndex); }
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (getPlanetPresetCombo() != null ? getPlanetPresetCombo().getValue() : PlanetPreset.EARTH_LIKE); }
                @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
                @Override public boolean isUpdatingFromPreset() { return presetManager.isUpdatingFromPreset(); }
                @Override public long getEpochYear() {
                    return (ecologyPresetBar != null && ecologyPresetBar.getPresetCombo() != null && ecologyPresetBar.getPresetCombo().getValue() != null)
                            ? ecologyPresetBar.getPresetCombo().getValue().getAssociatedEpochYear() : 2026L;
                }
            });

    // Hydrographic Domain & Freshwater Aquifers (Tab 2 Parity)
    private final org.ether.society.ui.resource.ResourceHydroSectionBuilder hydroSectionBuilder =
            new org.ether.society.ui.resource.ResourceHydroSectionBuilder(new org.ether.society.ui.resource.ResourceHydroSectionBuilder.HostContext() {
                @Override public void notifyParamChange() { if (!presetManager.isUpdatingFromPreset() && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged(); }
                @Override public void updatePreviewCanvas() { ResourceDistributionPanel.this.updatePreviewCanvas(); }
                @Override public void selectViewMode(int viewModeIndex) { ResourceDistributionPanel.this.selectViewMode(viewModeIndex); }
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (getPlanetPresetCombo() != null ? getPlanetPresetCombo().getValue() : PlanetPreset.EARTH_LIKE); }
                @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
                @Override public boolean isUpdatingFromPreset() { return presetManager.isUpdatingFromPreset(); }
                @Override public OnlineMapService getOnlineMapService() { return onlineMapService; }
                @Override public String getMapSourceComboValue() { return "earth"; }
                @Override public void setMapStatus(String status) {}
            });

    // Biome & Flora Domain (Tab 2 Parity)
    private final org.ether.society.ui.resource.ResourceBiomeSectionBuilder biomeSectionBuilder =
            new org.ether.society.ui.resource.ResourceBiomeSectionBuilder(new org.ether.society.ui.resource.ResourceBiomeSectionBuilder.HostContext() {
                @Override public void notifyParamChange() { if (!presetManager.isUpdatingFromPreset() && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged(); }
                @Override public void updatePreviewCanvas() { ResourceDistributionPanel.this.updatePreviewCanvas(); }
                @Override public void updateSummary() { ResourceDistributionPanel.this.updateSummary(); }
                @Override public void selectViewMode(int viewModeIndex) { ResourceDistributionPanel.this.selectViewMode(viewModeIndex); }
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (getPlanetPresetCombo() != null ? getPlanetPresetCombo().getValue() : PlanetPreset.EARTH_LIKE); }
                @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
                @Override public boolean isUpdatingFromPreset() { return presetManager.isUpdatingFromPreset(); }
                @Override public OnlineMapService getOnlineMapService() { return onlineMapService; }
                @Override public String getMapSourceComboValue() { return "earth"; }
                @Override public void setMapStatus(String status) {}
                @Override public void applyPresetMapSource(String sourceKey) { planetSectionBuilder.applyPresetMapSource(sourceKey); }
            });

    // Climate & Atmosphere Domain Maps (Tab 2 Parity)
    private final org.ether.society.ui.resource.ResourceClimateSectionBuilder climateSectionBuilder =
            new org.ether.society.ui.resource.ResourceClimateSectionBuilder(new org.ether.society.ui.resource.ResourceClimateSectionBuilder.HostContext() {
                @Override public void notifyParamChange() { if (!presetManager.isUpdatingFromPreset() && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged(); }
                @Override public void updatePreviewCanvas() { ResourceDistributionPanel.this.updatePreviewCanvas(); }
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (getPlanetPresetCombo() != null ? getPlanetPresetCombo().getValue() : PlanetPreset.EARTH_LIKE); }
                @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
                @Override public boolean isUpdatingFromPreset() { return presetManager.isUpdatingFromPreset(); }
                @Override public OnlineMapService getOnlineMapService() { return onlineMapService; }
                @Override public String getMapSourceComboValue() { return "earth"; }
                @Override public void setMapStatus(String status) {}
            });

    private final org.ether.society.ui.resource.ResourcePreviewCanvasBuilder previewCanvasBuilder =
            new org.ether.society.ui.resource.ResourcePreviewCanvasBuilder(new org.ether.society.ui.resource.ResourcePreviewCanvasBuilder.HostContext() {
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (getPlanetPresetCombo() != null ? getPlanetPresetCombo().getValue() : PlanetPreset.EARTH_LIKE); }
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

    private final org.ether.society.ui.resource.ResourcePresetManager presetManager =
            new org.ether.society.ui.resource.ResourcePresetManager(new org.ether.society.ui.resource.ResourcePresetManager.HostContext() {
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (getPlanetPresetCombo() != null ? getPlanetPresetCombo().getValue() : PlanetPreset.EARTH_LIKE); }
                @Override public void setActivePlanetPreset(PlanetPreset p) { ResourceDistributionPanel.this.activePlanetPreset = p; }
                @Override public ComboBox<PlanetPreset> getPlanetPresetCombo() { return ResourceDistributionPanel.this.getPlanetPresetCombo(); }
                @Override public Supplier<PlanetPreset> getPlanetPresetSupplier() { return planetPresetSupplier; }
                @Override public Consumer<PlanetPreset> getPlanetPresetApplyCallback() { return planetPresetApplyCallback; }
                @Override public org.ether.society.ui.resource.ResourceBiomeSectionBuilder getBiomeSectionBuilder() { return biomeSectionBuilder; }
                @Override public org.ether.society.ui.resource.ResourceHydroSectionBuilder getHydroSectionBuilder() { return hydroSectionBuilder; }
                @Override public org.ether.society.ui.resource.ResourceClimateSectionBuilder getClimateSectionBuilder() { return climateSectionBuilder; }
                @Override public Slider getCrustalMetalSlider() { return crustalMetalSlider; }
                @Override public Slider getPreciousMetalSlider() { return preciousMetalSlider; }
                @Override public Slider getMantleHeatSlider() { return mantleHeatSlider; }
                @Override public TextField getSeedField() { return seedField; }
                @Override public Image getCustomResourceImage() { return customResourceImage; }
                @Override public void setCustomResourceImage(Image img) { ResourceDistributionPanel.this.customResourceImage = img; }
                @Override public void setResourceFileLabelText(String text) { if (resourceFileLabel != null) resourceFileLabel.setText(text); }
                @Override public boolean validateResourceSetup() { return ResourceDistributionPanel.this.validateResourceSetup(); }
                @Override public void selectViewMode(int viewModeIndex) { ResourceDistributionPanel.this.selectViewMode(viewModeIndex); }
                @Override public void updateSummary() { ResourceDistributionPanel.this.updateSummary(); }
                @Override public void updateLegend() { ResourceDistributionPanel.this.updateLegend(); }
                @Override public void updatePreviewCanvas() { ResourceDistributionPanel.this.updatePreviewCanvas(); }
                @Override public void prepopulateGeologyTensors(String planetKey, long epochYear) { ResourceDistributionPanel.this.prepopulateGeologyTensors(planetKey, epochYear); }
                @Override public String getGeologySourceDisplayName(String planetKey) { return ResourceDistributionPanel.this.getGeologySourceDisplayName(planetKey); }
            });

    private final ResourcePlanetSectionBuilder planetSectionBuilder =
            new ResourcePlanetSectionBuilder(new ResourcePlanetSectionBuilder.HostContext() {
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset; }
                @Override public void setActivePlanetPreset(PlanetPreset preset) { ResourceDistributionPanel.this.activePlanetPreset = preset; }
                @Override public boolean isUpdatingFromPreset() { return presetManager.isUpdatingFromPreset(); }
                @Override public void setUpdatingFromPreset(boolean updating) { presetManager.setUpdatingFromPreset(updating); }
                @Override public org.ether.society.ui.resource.ResourceBiomeSectionBuilder getBiomeSectionBuilder() { return biomeSectionBuilder; }
                @Override public org.ether.society.ui.resource.ResourceHydroSectionBuilder getHydroSectionBuilder() { return hydroSectionBuilder; }
                @Override public org.ether.society.ui.resource.ResourceClimateSectionBuilder getClimateSectionBuilder() { return climateSectionBuilder; }
                @Override public org.ether.society.ui.resource.ResourceGeologySectionBuilder getGeologySectionBuilder() { return geologySectionBuilder; }
                @Override public Slider getCrustalMetalSlider() { return crustalMetalSlider; }
                @Override public Slider getPreciousMetalSlider() { return preciousMetalSlider; }
                @Override public Slider getMantleHeatSlider() { return mantleHeatSlider; }
                @Override public Slider getSeismicActivitySlider() { return seismicActivitySlider; }
                @Override public Slider getVolcanicActivitySlider() { return volcanicActivitySlider; }
                @Override public Label getEcoCompatibilityLabel() { return ecoCompatibilityLabel; }
                @Override public Label getResourceFileLabel() { return resourceFileLabel; }
                @Override public PresetControlBar<EcologyPreset> getEcologyPresetBar() { return ecologyPresetBar; }
                @Override public void updateSummary() { ResourceDistributionPanel.this.updateSummary(); }
                @Override public void updatePreviewCanvas() { ResourceDistributionPanel.this.updatePreviewCanvas(); }
                @Override public void updateLegend() { ResourceDistributionPanel.this.updateLegend(); }
                @Override public int getActiveCellCount() { return activeCells != null ? activeCells.size() : 12482; }
                @Override public String getGeologySourceDisplayName(String body) { return ResourceDistributionPanel.this.getGeologySourceDisplayName(body); }
                @Override public void prepopulateGeologyTensors(String body) { ResourceDistributionPanel.this.prepopulateGeologyTensors(body); }
                @Override public Supplier<PlanetPreset> getPlanetPresetSupplier() { return planetPresetSupplier; }
            });

    private final ResourceMapExportHandler mapExportHandler =
            new ResourceMapExportHandler(new ResourceMapExportHandler.HostContext() {
                @Override public javafx.stage.Window getWindow() { return getScene() != null ? getScene().getWindow() : null; }
                @Override public int getViewModeIndex() { return ResourceDistributionPanel.this.getViewModeIndex(); }
                @Override public PlanetPreset getActivePlanetPreset() { return activePlanetPreset != null ? activePlanetPreset : (getPlanetPresetCombo() != null ? getPlanetPresetCombo().getValue() : PlanetPreset.EARTH_LIKE); }
                @Override public Double getSeismicActivityValue() { return seismicActivitySlider != null ? seismicActivitySlider.getValue() : null; }
                @Override public Double getVolcanicActivityValue() { return volcanicActivitySlider != null ? volcanicActivitySlider.getValue() : null; }
                @Override public ProceduralGenerator getGenerator() { return generator; }
                @Override public ImageMapLoader getMapLoader() { return mapLoader; }
                @Override public org.ether.society.ui.resource.ResourceBiomeSectionBuilder getBiomeSectionBuilder() { return biomeSectionBuilder; }
                @Override public org.ether.society.ui.resource.ResourceHydroSectionBuilder getHydroSectionBuilder() { return hydroSectionBuilder; }
                @Override public org.ether.society.ui.resource.ResourceGeologySectionBuilder getGeologySectionBuilder() { return geologySectionBuilder; }
            });

    private final Map<Long, SimplexNoise> noiseCache = new HashMap<>();
    private SimplexNoise getOrCreateNoise(long seed) {
        return noiseCache.computeIfAbsent(seed, SimplexNoise::new);
    }

    private PresetControlBar<EcologyPreset> ecologyPresetBar;
    private TextArea ecologyDescriptionArea;

    private Slider crustalMetalSlider;
    private Slider preciousMetalSlider;
    private Slider mantleHeatSlider;
    private Slider seismicActivitySlider;
    private Slider volcanicActivitySlider;

    private Label resourceFileLabel;
    private Button loadResourceBtn;
    private Label ecoCompatibilityLabel;
    private VBox validationWarningBanner;
    private Label validationWarningLabel;

    private Label headerLabel;
    private Label mineralSecHeader;
    private Label crustalMetalRowLabel;
    private Label preciousMetalRowLabel;
    private Label mantleHeatRowLabel;
    private Label seismicRowLabel;
    private Label volcanicRowLabel;

    private TextField seedField;
    private Button applyBtn;
    private Supplier<PlanetPreset> planetPresetSupplier;
    private Consumer<PlanetPreset> planetPresetApplyCallback;

    public void setPlanetPresetSupplier(Supplier<PlanetPreset> supplier) {
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
        I18n.addLanguageListener(lang -> updateTexts());
    }

    public void setActivePlanetPreset(PlanetPreset planetPreset) {
        planetSectionBuilder.setActivePlanetPreset(planetPreset);
    }

    public ComboBox<PlanetPreset> getPlanetPresetCombo() {
        return planetSectionBuilder.getPlanetPresetCombo();
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

        // 1. Inherited Planet Preset & Territory Section
        VBox planetSection = planetSectionBuilder.buildPlanetSection();

        // 2. Preset Control Bar for Ecology
        ecologyPresetBar = presetManager.getPresetBar();
        ecologyDescriptionArea = presetManager.getDescriptionArea();

        // Legacy resource loading controls
        resourceFileLabel = new Label(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
        resourceFileLabel.getStyleClass().add("value-label");
        loadResourceBtn = new Button(I18n.get("planet.map.btn_load"));
        loadResourceBtn.getStyleClass().add("button-secondary");
        loadResourceBtn.setOnAction(e -> loadCustomResourceMap());

        // Minerals & Underground Geology
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

        // Domain sections
        VBox biomeDomainSection = biomeSectionBuilder.buildBiomeSection();
        seedField = biomeSectionBuilder.getBiomeSeedField();
        VBox hydroDomainSection = hydroSectionBuilder.buildHydroSection();
        VBox climateDomainSection = climateSectionBuilder.buildClimateSection();
        VBox geologyDomainSection = geologySectionBuilder.buildGeologySection();

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

        VBox centerBox = previewCanvasBuilder.buildPreviewPane();
        setLeft(scrollControls);
        setCenter(centerBox);

        planetSectionBuilder.updatePlanetContextDisplay();
        updatePreviewCanvas();
    }

    public EcologyPreset getSelectedEcologyPreset() {
        return presetManager.getSelectedEcologyPreset();
    }

    public void applyEcologyPreset(EcologyPreset p) {
        presetManager.applyEcologyPreset(p);
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

    public void generateProceduralHydrography() {
        hydroSectionBuilder.generateProceduralHydrography();
        if (!presetManager.isUpdatingFromPreset() && ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
        updateLegend();
        updatePreviewCanvas();
        updateSummary();
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

    private Slider createSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.valueProperty().addListener((obs, old, val) -> {
            if (presetManager.isUpdatingFromPreset()) return;
            if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
            if (!slider.isValueChanging()) {
                updateSummary();
                updatePreviewCanvas();
            }
        });
        slider.setOnMouseReleased(e -> {
            if (presetManager.isUpdatingFromPreset()) return;
            if (ecologyPresetBar != null) ecologyPresetBar.notifyParametersChanged();
            updateSummary();
            updatePreviewCanvas();
        });
        return slider;
    }

    private void updateSummary() {
        Label summaryLabel = previewCanvasBuilder != null ? previewCanvasBuilder.getSummaryLabel() : null;
        if (summaryLabel == null) return;
        summaryLabel.setText(String.format(
                I18n.getOrDefault("resource.summary.format",
                        "🌲 Végétale : %.0f GtC  |  🌾 Sols : %.0f GtC  |  🦌 Faune : %.2f GtC\n" +
                        "⛏️ Métaux : %.0f Gt  |  💎 Précieux : %.0f Mt  |  🌋 Manteau : %.1f mW/m²  |  💧 Aquifères : %.0f x10³ km³"),
                biomeSectionBuilder.getTerrestrialBiomassSlider() != null ? biomeSectionBuilder.getTerrestrialBiomassSlider().getValue() : 450.0,
                biomeSectionBuilder.getSoilCarbonSlider() != null ? biomeSectionBuilder.getSoilCarbonSlider().getValue() : 1500.0,
                biomeSectionBuilder.getFaunaBiomassSlider() != null ? biomeSectionBuilder.getFaunaBiomassSlider().getValue() : 2.0,
                crustalMetalSlider != null ? crustalMetalSlider.getValue() : 80.0,
                preciousMetalSlider != null ? preciousMetalSlider.getValue() : 1200.0,
                mantleHeatSlider != null ? mantleHeatSlider.getValue() : 87.0,
                hydroSectionBuilder.getFreshwaterAquiferSlider() != null ? hydroSectionBuilder.getFreshwaterAquiferSlider().getValue() : 15000.0
        ));
    }

    public void updateTexts() {
        boolean oldUpdating = presetManager.isUpdatingFromPreset();
        presetManager.setUpdatingFromPreset(true);
        try {
            if (headerLabel != null) headerLabel.setText(I18n.getOrDefault("resource.title", "RESOURCE DISTRIBUTION & ECOLOGY (SCIENTIFIC METRICS)"));
            if (ecologyDescriptionArea != null && ecologyPresetBar != null && ecologyPresetBar.getPresetCombo() != null && ecologyPresetBar.getPresetCombo().getValue() != null) {
                ecologyDescriptionArea.setText(ecologyPresetBar.getPresetCombo().getValue().getPresetDescription());
            }
            if (mineralSecHeader != null) mineralSecHeader.setText(I18n.getOrDefault("resource.section.minerals", "GEOLOGY & HYDROLOGY (METRICS)"));
            if (crustalMetalRowLabel != null) crustalMetalRowLabel.setText(I18n.getOrDefault("resource.param.crustal_metal", "Crustal Metal Wealth (Gt):"));
            if (preciousMetalRowLabel != null) preciousMetalRowLabel.setText(I18n.getOrDefault("resource.param.precious_metal", "Precious Metal Concentration (Mt):"));
            if (mantleHeatRowLabel != null) mantleHeatRowLabel.setText(I18n.getOrDefault("resource.param.mantle_heat", "Mantle Heat Flux (mW/m²):"));
            if (seismicRowLabel != null) seismicRowLabel.setText(I18n.getOrDefault("resource.param.seismic", "Seismic & Tectonic Activity (Mag Richter):"));
            if (volcanicRowLabel != null) volcanicRowLabel.setText(I18n.getOrDefault("resource.param.volcanic", "Global Volcanic Activity (VEI):"));
            if (loadResourceBtn != null) loadResourceBtn.setText(I18n.get("planet.map.btn_load"));
            if (customResourceImage == null && resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
            if (applyBtn != null) applyBtn.setText(I18n.getOrDefault("resource.btn.apply", "✅ Apply scientific distribution to simulation"));

            planetSectionBuilder.updateTexts();
            if (previewCanvasBuilder != null) previewCanvasBuilder.updateTexts();
            if (biomeSectionBuilder != null) biomeSectionBuilder.updateTexts();
            if (hydroSectionBuilder != null) hydroSectionBuilder.updateTexts();
            if (climateSectionBuilder != null) climateSectionBuilder.updateTexts();
            if (geologySectionBuilder != null) geologySectionBuilder.updateTexts();
            updateSummary();
            updateLegend();
        } finally {
            presetManager.setUpdatingFromPreset(oldUpdating);
        }
    }

    public void applyResourceDistribution() {
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
                (layerIdx, lon, lat, plPreset) -> geologySectionBuilder.sampleProceduralGeologyTensor(layerIdx, lon, lat, plPreset)
        );
        if (onResourcesAppliedCallback != null) {
            onResourcesAppliedCallback.accept(activeCells);
        }
    }

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

    private long hostEpochYear() {
        return (ecologyPresetBar != null && ecologyPresetBar.getPresetCombo() != null && ecologyPresetBar.getPresetCombo().getValue() != null)
                ? ecologyPresetBar.getPresetCombo().getValue().getAssociatedEpochYear() : 2026L;
    }

    public Color getGeologyResourceColor(int layerIdx, double val) {
        return geologySectionBuilder.getGeologyResourceColor(layerIdx, val);
    }

    public Image generateProceduralBiomeRasterImage(int width, int height) {
        return mapExportHandler.generateProceduralBiomeRasterImage(width, height);
    }

    public void exportBiomeMap() {
        mapExportHandler.exportBiomeMap();
    }

    public Image generateProceduralHydroRasterImage(int width, int height) {
        return mapExportHandler.generateProceduralHydroRasterImage(width, height);
    }

    public void exportHydroMap() {
        mapExportHandler.exportHydroMap();
    }

    public Image generateProceduralGeologyRasterImage(int layerIdx, int width, int height) {
        return mapExportHandler.generateProceduralGeologyRasterImage(layerIdx, width, height);
    }

    public void exportGeologyTensor(int layerIdx) {
        mapExportHandler.exportGeologyTensor(layerIdx);
    }

    public void exportMapsWithWorldFiles() {
        mapExportHandler.exportMapsWithWorldFiles();
    }

    public void showEcologyImportFormatHelp() {
        mapExportHandler.showEcologyImportFormatHelp();
    }

    public boolean isDirty() {
        return presetManager.isDirty();
    }

    public boolean promptSaveIfDirty(javafx.stage.Window owner) {
        return presetManager.promptSaveIfDirty(owner);
    }

    public PresetControlBar<EcologyPreset> getPresetBar() {
        return presetManager.getPresetBar();
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
    public ResourcePlanetSectionBuilder getPlanetSectionBuilder() { return planetSectionBuilder; }
}
