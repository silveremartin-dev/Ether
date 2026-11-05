/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.data.OnlineMapService;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.i18n.I18n;
import org.ether.society.persistence.PresetStorageService;
import org.ether.society.ui.planet.PlanetAstroSectionBuilder;
import org.ether.society.ui.planet.PlanetClimateSectionBuilder;
import org.ether.society.ui.planet.PlanetPreviewPaneBuilder;
import org.ether.society.ui.planet.PlanetTopographySectionBuilder;
import org.ether.society.ui.planetgen.PlanetGeneratorDataAndValidationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Integrated UI Panel for procedural planet generation, astronomical physics,
 * topography &amp; heightmap imports, satellite/moon dynamics, macroclimates, and 3-map climate imports.
 *
 * <p>Orchestrates specialized section builders for celestial mechanics, hypsometric relief,
 * atmospheric climate zones, equirectangular preview rendering, and delegated data/validation helpers.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PlanetGeneratorPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(PlanetGeneratorPanel.class);

    // Public Constants for backwards compatibility
    public static final String CLIMATE_SRC_TEMP_EARTH = PlanetClimateSectionBuilder.CLIMATE_SRC_TEMP_EARTH;
    public static final String CLIMATE_SRC_TEMP_MARS = PlanetClimateSectionBuilder.CLIMATE_SRC_TEMP_MARS;
    public static final String CLIMATE_SRC_TEMP_VENUS = PlanetClimateSectionBuilder.CLIMATE_SRC_TEMP_VENUS;
    public static final String CLIMATE_SRC_TEMP_MOON = PlanetClimateSectionBuilder.CLIMATE_SRC_TEMP_MOON;
    public static final String CLIMATE_SRC_TEMP_MERCURY = PlanetClimateSectionBuilder.CLIMATE_SRC_TEMP_MERCURY;
    public static final String CLIMATE_SRC_TEMP_WMS = PlanetClimateSectionBuilder.CLIMATE_SRC_TEMP_WMS;

    public static final String CLIMATE_SRC_PRECIP_EARTH = PlanetClimateSectionBuilder.CLIMATE_SRC_PRECIP_EARTH;
    public static final String CLIMATE_SRC_PRECIP_MARS = PlanetClimateSectionBuilder.CLIMATE_SRC_PRECIP_MARS;
    public static final String CLIMATE_SRC_PRECIP_VENUS = PlanetClimateSectionBuilder.CLIMATE_SRC_PRECIP_VENUS;
    public static final String CLIMATE_SRC_PRECIP_MOON = PlanetClimateSectionBuilder.CLIMATE_SRC_PRECIP_MOON;
    public static final String CLIMATE_SRC_PRECIP_MERCURY = PlanetClimateSectionBuilder.CLIMATE_SRC_PRECIP_MERCURY;
    public static final String CLIMATE_SRC_PRECIP_WMS = PlanetClimateSectionBuilder.CLIMATE_SRC_PRECIP_WMS;

    public static final String CLIMATE_SRC_SEASON_EARTH = PlanetClimateSectionBuilder.CLIMATE_SRC_SEASON_EARTH;
    public static final String CLIMATE_SRC_SEASON_MARS = PlanetClimateSectionBuilder.CLIMATE_SRC_SEASON_MARS;
    public static final String CLIMATE_SRC_SEASON_VENUS = PlanetClimateSectionBuilder.CLIMATE_SRC_SEASON_VENUS;
    public static final String CLIMATE_SRC_SEASON_MOON = PlanetClimateSectionBuilder.CLIMATE_SRC_SEASON_MOON;
    public static final String CLIMATE_SRC_SEASON_MERCURY = PlanetClimateSectionBuilder.CLIMATE_SRC_SEASON_MERCURY;
    public static final String CLIMATE_SRC_SEASON_WMS = PlanetClimateSectionBuilder.CLIMATE_SRC_SEASON_WMS;

    final ProceduralGenerator generator;
    private final OnlineMapService onlineMapService;
    @SuppressWarnings("unused")
    private final Consumer<List<H3Cell>> onPlanetGeneratedCallback;

    // Delegated autonomous section builders
    final PlanetAstroSectionBuilder astroSectionBuilder = new PlanetAstroSectionBuilder(PlanetHostContextFactory.createAstroHostContext(this));
    final PlanetTopographySectionBuilder topoSectionBuilder = new PlanetTopographySectionBuilder(PlanetHostContextFactory.createTopographyHostContext(this));
    final PlanetClimateSectionBuilder climateSectionBuilder = new PlanetClimateSectionBuilder(PlanetHostContextFactory.createClimateHostContext(this));
    final PlanetPreviewPaneBuilder previewPaneBuilder = new PlanetPreviewPaneBuilder(PlanetHostContextFactory.createPreviewPaneHostContext(this));

    // Custom Biome & Resource Maps
    private Image customBiomeImage;
    private Image customResourceImage;
    private String cachedBiomeB64;
    private String cachedResourceB64;

    private PresetControlBar<PlanetPreset> presetBar;
    private ComboBox<PlanetPreset> presetCombo;
    private TextArea planetDescriptionArea;
    private ComboBox<Integer> resolutionCombo;

    private Label biomeFileLabel;
    private Label resourceFileLabel;

    // UI Titles & Validation
    private Label headerLabel;
    private Label presetsSecHeader;
    private VBox validationWarningBanner;
    private Label validationWarningLabel;

    boolean isUpdatingFromPreset = false;

    public ProceduralGenerator getGenerator() {
        return generator;
    }

    public PlanetGeneratorPanel(Consumer<List<H3Cell>> onPlanetGeneratedCallback) {
        this.generator = new ProceduralGenerator();
        this.onlineMapService = new OnlineMapService();
        this.onPlanetGeneratedCallback = onPlanetGeneratedCallback;

        getStyleClass().add("glass-panel");
        setPadding(new Insets(20));

        initUI();
        applyPreset(PlanetPreset.EARTH_LIKE);
        updateTexts();

        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    void notifyParamChange() {
        if (!isUpdatingFromPreset && presetBar != null) {
            presetBar.notifyParametersChanged();
        }
        updatePreview();
    }

    void updatePreview() {
        if (previewPaneBuilder != null) {
            previewPaneBuilder.updatePreview();
        }
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
                new Label(I18n.getOrDefault("planet.validation.header", "⚠️ Paramètres ou cartes requis manquants ou invalides :")),
                validationWarningLabel
        );
        validationWarningBanner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10;");
        validationWarningBanner.getChildren().get(0).setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 13px;");
        validationWarningBanner.setVisible(false);
        validationWarningBanner.setManaged(false);

        PresetControlBar<PlanetPreset> topPresetBar = new PresetControlBar<>("planet.preset", "Préréglage Global");
        this.presetBar = topPresetBar;
        topPresetBar.setExportCategory("planetgenerator");
        presetCombo = topPresetBar.getPresetCombo();
        topPresetBar.setPresets(PresetStorageService.loadAllPlanetPresets(), PlanetPreset.EARTH_LIKE);
        PlanetGeneratorDataAndValidationHelper.bindPresetBarListener(
                topPresetBar,
                () -> isUpdatingFromPreset,
                this::validatePlanetSetup,
                this::buildPresetFromUI,
                this::applyPreset,
                () -> { if (previewPaneBuilder != null) previewPaneBuilder.selectViewMode(0); }
        );

        presetsSecHeader = new Label(I18n.getOrDefault("planet.section.presets", "GLOBAL PRESETS & PLANET SAVE"));
        presetsSecHeader.getStyleClass().add("label-section-header");
        planetDescriptionArea = new TextArea();
        planetDescriptionArea.setPrefRowCount(4);
        planetDescriptionArea.setWrapText(true);
        planetDescriptionArea.setEditable(false);
        planetDescriptionArea.getStyleClass().add("glass-text-area");
        planetDescriptionArea.setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0; -fx-background-color: rgba(15, 23, 42, 0.6); -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox presetSection = new VBox(8, presetsSecHeader, new VBox(8, topPresetBar, planetDescriptionArea));
        presetSection.getStyleClass().add("card-section");

        resolutionCombo = new ComboBox<>();
        resolutionCombo.getItems().addAll(1, 2, 3, 4, 5, 6, 7, 8);
        resolutionCombo.setValue(3);
        resolutionCombo.setMaxWidth(Double.MAX_VALUE);
        resolutionCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : I18n.get("planet.param.resolution.res" + item));
            }
        });
        resolutionCombo.setButtonCell(resolutionCombo.getCellFactory().call(null));
        resolutionCombo.setOnAction(e -> {
            if (!isUpdatingFromPreset && presetBar != null) presetBar.notifyParametersChanged();
            updatePreview();
        });

        controlsBox.getChildren().addAll(
                headerLabel,
                validationWarningBanner,
                presetSection,
                astroSectionBuilder.buildSection(),
                topoSectionBuilder.buildSection(),
                climateSectionBuilder.buildSection()
        );
        controlsBox.setPrefWidth(460);
        controlsBox.setMinWidth(460);

        ScrollPane scrollControls = new ScrollPane(controlsBox);
        scrollControls.setFitToWidth(true);
        scrollControls.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollControls.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollControls.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollControls.setPrefWidth(480);
        scrollControls.setMinWidth(480);
        scrollControls.setMaxWidth(480);

        setLeft(scrollControls);
        setCenter(previewPaneBuilder.buildPreviewPane());
    }

    public void applyPreset(PlanetPreset p) {
        if (p == null) return;
        WindowUtils.setBusyCursor(this, true);
        try {
            isUpdatingFromPreset = true;
            if (planetDescriptionArea != null) {
                planetDescriptionArea.setText(p.getPresetDescription());
            }

            resolutionCombo.setValue(p.resolution());
            astroSectionBuilder.applyPreset(p);
            topoSectionBuilder.applyPreset(p);
            climateSectionBuilder.applyPreset(p);

            if (p.customBiomeBase64() != null) {
                customBiomeImage = ImageMapLoader.base64PngToImage(p.customBiomeBase64());
                cachedBiomeB64 = p.customBiomeBase64();
                if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.preset.biomes", "🌿 Preset Biomes"));
            }
            if (p.customResourceBase64() != null) {
                customResourceImage = ImageMapLoader.base64PngToImage(p.customResourceBase64());
                cachedResourceB64 = p.customResourceBase64();
                if (resourceFileLabel != null) resourceFileLabel.setText(I18n.getOrDefault("planet.preset.resources", "🪨 Preset Resources"));
            }

            String elevSrc = p.elevationMapSource();
            if (p.elevationUseImport() || p.customElevBase64() != null) {
                if ("earth".equals(elevSrc)) {
                    if (p.customElevBase64() == null) loadEarthPresetMaps(p.getAssociatedEpochYear());
                } else if ("mars".equals(elevSrc) || "venus".equals(elevSrc) || "moon".equals(elevSrc) || "mercury".equals(elevSrc)) {
                    if (p.customElevBase64() == null) applyMapSourcePreset(elevSrc);
                }
            } else if (p.customElevBase64() == null) {
                clearCustomMaps();
            }

            if (presetBar != null) presetBar.markClean(p);
            isUpdatingFromPreset = false;
            updatePreview();
        } finally {
            WindowUtils.setBusyCursor(this, false);
        }
    }

    public PlanetPreset buildPresetFromUI() {
        return PlanetGeneratorDataAndValidationHelper.buildPresetFromUI(
                presetCombo, resolutionCombo, astroSectionBuilder, topoSectionBuilder, climateSectionBuilder,
                customBiomeImage, cachedBiomeB64, b64 -> this.cachedBiomeB64 = b64,
                customResourceImage, cachedResourceB64, b64 -> this.cachedResourceB64 = b64);
    }

    public void updateTexts() {
        boolean oldUpdating = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            headerLabel.setText(I18n.get("planet.section.header"));
            presetsSecHeader.setText(I18n.getOrDefault("planet.section.presets", "GLOBAL PRESETS & PLANET SAVE"));
            if (planetDescriptionArea != null && presetCombo != null && presetCombo.getValue() != null) {
                planetDescriptionArea.setText(presetCombo.getValue().getPresetDescription());
            }
            astroSectionBuilder.updateTexts();
            topoSectionBuilder.updateTexts();
            climateSectionBuilder.updateTexts();
            previewPaneBuilder.updateTexts();
        } finally {
            isUpdatingFromPreset = oldUpdating;
        }
    }

    void applyMapSourcePreset(String sourceKey) {
        boolean oldUpdating = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            if ("none".equals(sourceKey)) {
                clearCustomMaps();
                return;
            }
            if ("earth".equals(sourceKey)) {
                loadEarthPresetMaps();
                return;
            }
            if ("mars".equals(sourceKey) || "venus".equals(sourceKey) || "moon".equals(sourceKey) || "mercury".equals(sourceKey)) {
                clearCustomMaps();
                PlanetGeneratorDataAndValidationHelper.loadSolarSystemBodyMaps(
                        sourceKey, topoSectionBuilder, climateSectionBuilder, img -> this.customBiomeImage = img);
            }
        } finally {
            isUpdatingFromPreset = oldUpdating;
        }
        updatePreview();
    }

    private void loadEarthPresetMaps() {
        long year = (presetBar != null && presetBar.getPresetCombo() != null && presetBar.getPresetCombo().getValue() != null)
                ? presetBar.getPresetCombo().getValue().getAssociatedEpochYear() : 2026L;
        loadEarthPresetMaps(year);
    }

    void loadEarthPresetMaps(long epochYear) {
        try {
            topoSectionBuilder.setCustomElevationImage(ImageMapLoader.loadMapImage("earth", epochYear, "elevation"));
            customBiomeImage = ImageMapLoader.loadMapImage("earth", epochYear, "biomes");
            climateSectionBuilder.setCustomClimateImage(ImageMapLoader.loadMapImage("earth", epochYear, "temperature"));
            climateSectionBuilder.setCustomRainfallImage(ImageMapLoader.loadMapImage("earth", epochYear, "precipitation"));
            climateSectionBuilder.setCustomSeasonalityImage(ImageMapLoader.loadMapImage("earth", epochYear, "seasonality"));
        } catch (Exception e) {
            logger.warn("Could not load internal Earth maps for epoch " + epochYear, e);
        }
    }

    void clearCustomMaps() {
        topoSectionBuilder.setCustomElevationImage(null);
        topoSectionBuilder.setCachedElevationBase64(null);
        customBiomeImage = null;
        cachedBiomeB64 = null;
        customResourceImage = null;
        cachedResourceB64 = null;
        climateSectionBuilder.setCustomClimateImage(null);
        climateSectionBuilder.setCachedClimateBase64(null);
        climateSectionBuilder.setCustomRainfallImage(null);
        climateSectionBuilder.setCachedRainfallBase64(null);
        climateSectionBuilder.setCustomSeasonalityImage(null);
        climateSectionBuilder.setCachedSeasonalityBase64(null);
    }

    void exportProceduralHeightmap() {
        PlanetGeneratorDataAndValidationHelper.exportProceduralMap(
                getScene() != null ? getScene().getWindow() : null,
                null, buildPresetFromUI(), generator, climateSectionBuilder);
    }

    void exportProceduralClimateMap(String type) {
        PlanetGeneratorDataAndValidationHelper.exportProceduralMap(
                getScene() != null ? getScene().getWindow() : null,
                type, buildPresetFromUI(), generator, climateSectionBuilder);
    }

    void fetchOnlineClimateData() {
        PlanetGeneratorDataAndValidationHelper.fetchOnlineClimateData(
                onlineMapService, topoSectionBuilder.getElevationMapSource(), climateSectionBuilder, this::updatePreview);
    }

    public List<String> getValidationErrors() {
        return PlanetGeneratorDataAndValidationHelper.getValidationErrors(topoSectionBuilder, climateSectionBuilder);
    }

    public boolean validatePlanetSetup() {
        return validatePlanetSetup(true);
    }

    public boolean validatePlanetSetup(boolean showDialog) {
        return PlanetGeneratorDataAndValidationHelper.applyValidationVisuals(
                getValidationErrors(), showDialog, topoSectionBuilder, climateSectionBuilder,
                validationWarningBanner, validationWarningLabel);
    }

    public boolean isDirty() {
        return presetBar != null && presetBar.isDirty();
    }

    public boolean promptSaveIfDirty(javafx.stage.Window owner) {
        return presetBar == null || presetBar.promptSavePresetIfDirty(owner);
    }

    public PresetControlBar<PlanetPreset> getPresetBar() {
        return presetBar;
    }
}
