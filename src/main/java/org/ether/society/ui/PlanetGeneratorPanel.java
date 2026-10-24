/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.data.OnlineMapService;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.planet.PlanetAstroSectionBuilder;
import org.ether.society.ui.planet.PlanetClimateSectionBuilder;
import org.ether.society.ui.planet.PlanetGeospatialExportHelper;
import org.ether.society.ui.planet.PlanetPreviewPaneBuilder;
import org.ether.society.ui.planet.PlanetTopographySectionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Integrated UI Panel for procedural planet generation, astronomical physics, 
 * topography & heightmap imports, satellite/moon dynamics, macroclimates, and 3-map climate imports.
 *
 * <p>Orchestrates specialized section builders for celestial mechanics, hypsometric relief,
 * atmospheric climate zones, and equirectangular preview rendering.</p>
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
    private final ImageMapLoader mapLoader;
    private final OnlineMapService onlineMapService;
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
    private Button loadBiomeBtn;
    private Button clearBiomeBtn;
    private Button loadResourceBtn;
    private Button clearResourceBtn;

    private Button fetchOnlineBtn;
    private Button fetchOnlineClimateBtn;
    private Button exportMapsBtn;
    private Label mapStatusLabel;

    // UI Titles & Validation
    private Label headerLabel;
    private Label presetsSecHeader;
    private Label generalSecHeader;
    private Label customMapsSecHeader;
    private Label resRowLabel;
    private Label biomeMapRowLabel;
    private Label resourceMapRowLabel;
    private VBox validationWarningBanner;
    private Label validationWarningLabel;

    boolean isUpdatingFromPreset = false;

    public ProceduralGenerator getGenerator() {
        return generator;
    }

    public PlanetGeneratorPanel(Consumer<List<H3Cell>> onPlanetGeneratedCallback) {
        this.generator = new ProceduralGenerator();
        this.mapLoader = new ImageMapLoader();
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

        // 1. Presets Control Bar
        PresetControlBar<PlanetPreset> topPresetBar = new PresetControlBar<>("planet.preset", "Préréglage Global");
        this.presetBar = topPresetBar;
        topPresetBar.setExportCategory("planetgenerator");
        presetCombo = topPresetBar.getPresetCombo();
        topPresetBar.setPresets(org.ether.society.persistence.PresetStorageService.loadAllPlanetPresets(), PlanetPreset.EARTH_LIKE);
        topPresetBar.setListener(new PresetControlBar.PresetActionsListener<PlanetPreset>() {
            @Override
            public void onPresetSelected(PlanetPreset preset) {
                if (!isUpdatingFromPreset) {
                    applyPreset(preset);
                    if (previewPaneBuilder != null) previewPaneBuilder.selectViewMode(0);
                }
            }

            @Override
            public void onSavePreset(String name) {
                if (!validatePlanetSetup()) return;
                PlanetPreset current = buildPresetFromUI();
                PlanetPreset custom = current.withName(name);
                topPresetBar.getPresetCombo().getItems().add(custom);
                topPresetBar.getPresetCombo().setValue(custom);
            }

            @Override
            public void onDeletePreset(PlanetPreset preset) {
                topPresetBar.getPresetCombo().getItems().remove(preset);
            }

            @Override
            public void onExportPreset(File targetFile, PlanetPreset preset) {
                try {
                    ObjectMapper mapper = org.ether.society.persistence.EtherJsonUtil.getMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.writeValue(targetFile, preset);
                    logger.info("Exported planet preset to {}", targetFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to export planet preset", ex);
                }
            }

            @Override
            public void onImportPreset(File sourceFile) {
                try {
                    ObjectMapper mapper = org.ether.society.persistence.EtherJsonUtil.getMapper();
                    PlanetPreset preset = mapper.readValue(sourceFile, PlanetPreset.class);
                    topPresetBar.getPresetCombo().getItems().add(preset);
                    topPresetBar.getPresetCombo().setValue(preset);
                    applyPreset(preset);
                    logger.info("Imported planet preset from {}", sourceFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to import planet preset", ex);
                }
            }
        });

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

        // 2. Resolution Combo
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

        // 3. Section Builders
        VBox astroSection = astroSectionBuilder.buildSection();
        VBox topoSection = topoSectionBuilder.buildSection();
        VBox climateSection = climateSectionBuilder.buildSection();

        controlsBox.getChildren().addAll(
                headerLabel,
                validationWarningBanner,
                presetSection,
                astroSection,
                topoSection,
                climateSection
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

        VBox previewPane = previewPaneBuilder.buildPreviewPane();

        setLeft(scrollControls);
        setCenter(previewPane);
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
            } else {
                if (p.customElevBase64() == null) clearCustomMaps();
            }

            if (presetBar != null) {
                presetBar.markClean(p);
            }
            isUpdatingFromPreset = false;
            updatePreview();
        } finally {
            WindowUtils.setBusyCursor(this, false);
        }
    }

    public PlanetPreset buildPresetFromUI() {
        long seed = topoSectionBuilder.getSeed();
        long tempSeed = climateSectionBuilder.getTempSeed(seed);
        long precipSeed = climateSectionBuilder.getPrecipSeed(seed);
        long seasonSeed = climateSectionBuilder.getSeasonSeed(seed);

        String presetName = presetCombo.getValue() != null ? presetCombo.getValue().name() : "Custom Planet";
        boolean isSat = astroSectionBuilder.isSatellite();

        Image customElevImage = topoSectionBuilder.getCustomElevationImage();
        String cachedElevB64 = topoSectionBuilder.getCachedElevationBase64();
        if (customElevImage != null && cachedElevB64 == null) {
            cachedElevB64 = ImageMapLoader.imageToBase64Png(customElevImage);
            topoSectionBuilder.setCachedElevationBase64(cachedElevB64);
        }

        if (customBiomeImage != null && cachedBiomeB64 == null) cachedBiomeB64 = ImageMapLoader.imageToBase64Png(customBiomeImage);
        if (customResourceImage != null && cachedResourceB64 == null) cachedResourceB64 = ImageMapLoader.imageToBase64Png(customResourceImage);

        Image customClimateImage = climateSectionBuilder.getCustomClimateImage();
        String cachedClimateB64 = climateSectionBuilder.getCachedClimateBase64();
        if (customClimateImage != null && cachedClimateB64 == null) {
            cachedClimateB64 = ImageMapLoader.imageToBase64Png(customClimateImage);
            climateSectionBuilder.setCachedClimateBase64(cachedClimateB64);
        }

        Image customRainfallImage = climateSectionBuilder.getCustomRainfallImage();
        String cachedRainfallB64 = climateSectionBuilder.getCachedRainfallBase64();
        if (customRainfallImage != null && cachedRainfallB64 == null) {
            cachedRainfallB64 = ImageMapLoader.imageToBase64Png(customRainfallImage);
            climateSectionBuilder.setCachedRainfallBase64(cachedRainfallB64);
        }

        Image customSeasonalityImage = climateSectionBuilder.getCustomSeasonalityImage();
        String cachedSeasonalityB64 = climateSectionBuilder.getCachedSeasonalityBase64();
        if (customSeasonalityImage != null && cachedSeasonalityB64 == null) {
            cachedSeasonalityB64 = ImageMapLoader.imageToBase64Png(customSeasonalityImage);
            climateSectionBuilder.setCachedSeasonalityBase64(cachedSeasonalityB64);
        }

        boolean elevImport = topoSectionBuilder.isElevationImportActive();
        String elevSrc = topoSectionBuilder.getElevationMapSource();

        boolean tempImport = climateSectionBuilder.isTempImportActive();
        String tempSrc = climateSectionBuilder.getTempSource();

        boolean precipImport = climateSectionBuilder.isPrecipImportActive();
        String precipSrc = climateSectionBuilder.getPrecipSource();

        boolean seasonImport = climateSectionBuilder.isSeasonImportActive();
        String seasonSrc = climateSectionBuilder.getSeasonSource();

        String customElevB64Final = (elevImport && customElevImage != null) ? cachedElevB64 : null;
        String customBiomeB64Final = (elevImport && customBiomeImage != null) ? cachedBiomeB64 : null;
        String customResourceB64Final = (elevImport && customResourceImage != null) ? cachedResourceB64 : null;
        String customClimateB64Final = (tempImport && customClimateImage != null) ? cachedClimateB64 : null;
        String customRainfallB64Final = (precipImport && customRainfallImage != null) ? cachedRainfallB64 : null;
        String customSeasonalityB64Final = (seasonImport && customSeasonalityImage != null) ? cachedSeasonalityB64 : null;

        return new PlanetPreset(
                presetName,
                resolutionCombo.getValue() != null ? resolutionCombo.getValue() : 6,
                astroSectionBuilder.getRadiusKm(),
                astroSectionBuilder.getDayLengthHours(),
                astroSectionBuilder.getAxialTiltDegrees(),
                astroSectionBuilder.getYearLengthDays(),
                astroSectionBuilder.getDistanceToSunAU(),
                astroSectionBuilder.getSolarLuminosity(),
                topoSectionBuilder.getMinAltitudeMeters(),
                topoSectionBuilder.getMaxAltitudeMeters(),
                astroSectionBuilder.getAverageTempC(),
                seed,
                topoSectionBuilder.getNoiseFrequency(),
                topoSectionBuilder.getNoiseScale(),
                topoSectionBuilder.getWaterLevel(),
                climateSectionBuilder.getTemperatureGradient(),
                climateSectionBuilder.getOxygenPercentage(),
                climateSectionBuilder.getAlbedo(),
                climateSectionBuilder.getAtmospherePressureAtm(),
                isSat,
                astroSectionBuilder.getParentMass(),
                astroSectionBuilder.getOrbitDistanceParent(),
                climateSectionBuilder.getCo2Ppm(),
                presetCombo.getValue() != null ? presetCombo.getValue().seismicActivityLevel() : 2.5,
                presetCombo.getValue() != null ? presetCombo.getValue().volcanicActivityLevel() : 1.5,
                customElevB64Final,
                customBiomeB64Final,
                customResourceB64Final,
                customClimateB64Final,
                customRainfallB64Final,
                customSeasonalityB64Final,
                elevImport,
                elevSrc,
                tempImport,
                tempSrc,
                tempSeed,
                precipImport,
                precipSrc,
                precipSeed,
                seasonImport,
                seasonSrc,
                seasonSeed
        );
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
                String prefix = switch (sourceKey) {
                    case "mars" -> "mars";
                    case "venus" -> "venus";
                    case "moon" -> "moon";
                    default -> "mercury";
                };
                topoSectionBuilder.setCustomElevationImage(ImageMapLoader.loadMapImage(prefix + "_elevation.png"));
                customBiomeImage = ImageMapLoader.loadMapImage(prefix + "_biomes.png");
                climateSectionBuilder.setCustomClimateImage(ImageMapLoader.loadMapImage(prefix + "_temperature.png"));
                climateSectionBuilder.setCustomRainfallImage(ImageMapLoader.loadMapImage(prefix + "_precipitation.png"));
                climateSectionBuilder.setCustomSeasonalityImage(ImageMapLoader.loadMapImage(prefix + "_seasonality.png"));
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
        PlanetPreset preset = buildPresetFromUI();
        long tSeed = climateSectionBuilder.getTempSeed(preset.seed());
        long pSeed = climateSectionBuilder.getPrecipSeed(preset.seed());
        long sSeed = climateSectionBuilder.getSeasonSeed(preset.seed());
        PlanetGeospatialExportHelper.exportProceduralHeightmap(
                getScene() != null ? getScene().getWindow() : null,
                preset, generator, tSeed, pSeed, sSeed,
                PlanetPreviewPaneBuilder::getHypsometricColor
        );
    }

    void exportProceduralClimateMap(String type) {
        PlanetPreset preset = buildPresetFromUI();
        long tSeed = climateSectionBuilder.getTempSeed(preset.seed());
        long pSeed = climateSectionBuilder.getPrecipSeed(preset.seed());
        long sSeed = climateSectionBuilder.getSeasonSeed(preset.seed());
        double co2Forcing = 3.0 * (Math.log(Math.max(1.0, preset.co2Ppm()) / 280.0) / Math.log(2.0));
        double pressureBoost = (preset.atmospherePressureAtm() - 1.0) * 8.0;
        double minTemp = preset.averageTempC() + co2Forcing + pressureBoost - preset.temperatureGradient() - 20.0;
        double maxTemp = preset.averageTempC() + co2Forcing + pressureBoost + preset.temperatureGradient();
        PlanetGeospatialExportHelper.exportProceduralClimateMap(
                getScene() != null ? getScene().getWindow() : null,
                type, preset, generator, tSeed, pSeed, sSeed,
                temp -> PlanetPreviewPaneBuilder.getTemperatureColor(temp, minTemp, maxTemp),
                PlanetPreviewPaneBuilder::getPrecipitationColor,
                PlanetPreviewPaneBuilder::getSeasonalityColor
        );
    }

    void fetchOnlineClimateData() {
        String sourceKey = topoSectionBuilder.getElevationMapSource();
        OnlineMapService.CelestialBody body = switch (sourceKey) {
            case "mars" -> OnlineMapService.CelestialBody.MARS;
            case "moon" -> OnlineMapService.CelestialBody.MOON;
            case "venus" -> OnlineMapService.CelestialBody.VENUS;
            default -> OnlineMapService.CelestialBody.EARTH;
        };

        onlineMapService.fetchClimateMapAsync(body).thenAccept(img -> {
            javafx.application.Platform.runLater(() -> {
                if (img != null) {
                    climateSectionBuilder.setCustomClimateImage(img);
                    updatePreview();
                }
            });
        });

        onlineMapService.fetchRainfallMapAsync(body).thenAccept(img -> {
            javafx.application.Platform.runLater(() -> {
                if (img != null) {
                    climateSectionBuilder.setCustomRainfallImage(img);
                    updatePreview();
                }
            });
        });
    }

    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();
        if (topoSectionBuilder.isElevationImportActive()) {
            Image elev = topoSectionBuilder.getCustomElevationImage();
            if (elev == null) {
                errors.add(I18n.getOrDefault("planet.validation.missing_elev_map", "Image heightmap d'altitude manquante en mode d'importation (Onglet 1)."));
            } else {
                var val = ImageMapLoader.validateMapImage(elev);
                if (!val.valid()) errors.add(I18n.getOrDefault("planet.validation.invalid_elev_map", "Image heightmap d'altitude incompatible (Onglet 1) : ") + val.message());
            }
        }
        if (climateSectionBuilder.isTempImportActive()) {
            Image temp = climateSectionBuilder.getCustomClimateImage();
            if (temp == null) {
                errors.add(I18n.getOrDefault("planet.validation.missing_temp_map", "Missing thermal map in import mode (Tab 1)."));
            } else {
                var val = ImageMapLoader.validateMapImage(temp);
                if (!val.valid()) errors.add(I18n.getOrDefault("planet.validation.invalid_temp_map", "Thermal map incompatible (Tab 1): ") + val.message());
            }
        }
        if (climateSectionBuilder.isPrecipImportActive()) {
            Image precip = climateSectionBuilder.getCustomRainfallImage();
            if (precip == null) {
                errors.add(I18n.getOrDefault("planet.validation.missing_precip_map", "Missing rainfall map in import mode (Tab 1)."));
            } else {
                var val = ImageMapLoader.validateMapImage(precip);
                if (!val.valid()) errors.add(I18n.getOrDefault("planet.validation.invalid_precip_map", "Rainfall map incompatible (Tab 1): ") + val.message());
            }
        }
        if (climateSectionBuilder.isSeasonImportActive()) {
            Image season = climateSectionBuilder.getCustomSeasonalityImage();
            if (season == null) {
                errors.add(I18n.getOrDefault("planet.validation.missing_season_map", "Missing seasonality map in import mode (Tab 1)."));
            } else {
                var val = ImageMapLoader.validateMapImage(season);
                if (!val.valid()) errors.add(I18n.getOrDefault("planet.validation.invalid_season_map", "Seasonality map incompatible (Tab 1): ") + val.message());
            }
        }
        if (topoSectionBuilder.getMinAltitudeMeters() >= topoSectionBuilder.getMaxAltitudeMeters()) {
            errors.add(I18n.getOrDefault("planet.validation.invalid_alt_range", "Minimum altitude must be strictly lower than maximum altitude (Tab 1)."));
        }
        return errors;
    }

    public boolean validatePlanetSetup() {
        return validatePlanetSetup(true);
    }

    public boolean validatePlanetSetup(boolean showDialog) {
        List<String> errors = getValidationErrors();
        boolean isValid = errors.isEmpty();

        boolean elevBad = topoSectionBuilder.isElevationImportActive() && (topoSectionBuilder.getCustomElevationImage() == null || !ImageMapLoader.validateMapImage(topoSectionBuilder.getCustomElevationImage()).valid());
        if (topoSectionBuilder.getLoadElevBtn() != null) topoSectionBuilder.getLoadElevBtn().setStyle(elevBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        boolean tempBad = climateSectionBuilder.isTempImportActive() && (climateSectionBuilder.getCustomClimateImage() == null || !ImageMapLoader.validateMapImage(climateSectionBuilder.getCustomClimateImage()).valid());
        if (climateSectionBuilder.getLoadClimateBtn() != null) climateSectionBuilder.getLoadClimateBtn().setStyle(tempBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        boolean precipBad = climateSectionBuilder.isPrecipImportActive() && (climateSectionBuilder.getCustomRainfallImage() == null || !ImageMapLoader.validateMapImage(climateSectionBuilder.getCustomRainfallImage()).valid());
        if (climateSectionBuilder.getLoadRainfallBtn() != null) climateSectionBuilder.getLoadRainfallBtn().setStyle(precipBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

        boolean seasonBad = climateSectionBuilder.isSeasonImportActive() && (climateSectionBuilder.getCustomSeasonalityImage() == null || !ImageMapLoader.validateMapImage(climateSectionBuilder.getCustomSeasonalityImage()).valid());
        if (climateSectionBuilder.getLoadSeasonalityBtn() != null) climateSectionBuilder.getLoadSeasonalityBtn().setStyle(seasonBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");

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

    public boolean isDirty() {
        return presetBar != null && presetBar.isDirty();
    }

    public boolean promptSaveIfDirty(javafx.stage.Window owner) {
        if (presetBar == null) return true;
        return presetBar.promptSavePresetIfDirty(owner);
    }

    public PresetControlBar<PlanetPreset> getPresetBar() {
        return presetBar;
    }
}
