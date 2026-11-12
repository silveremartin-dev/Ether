/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.planetgen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.data.OnlineMapService;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.i18n.I18n;
import org.ether.society.persistence.EtherJsonHelper;
import org.ether.society.ui.PresetControlBar;
import org.ether.society.ui.planet.PlanetAstroSectionBuilder;
import org.ether.society.ui.planet.PlanetClimateSectionBuilder;
import org.ether.society.ui.planet.PlanetGeospatialExportHelper;
import org.ether.society.ui.planet.PlanetPreviewPaneBuilder;
import org.ether.society.ui.planet.PlanetTopographySectionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Data marshaling, geospatial raster export/import, WMS climate fetching, and setup validation
 * helper for {@link org.ether.society.ui.PlanetGeneratorPanel}.
 *
 * <p>Extracted in accordance with the Single Responsibility Principle (SRP) to decouple
 * {@link PlanetPreset} assembly, JSON persistence callbacks, celestial body raster loading,
 * radiative forcing thermal bounds, and validation styling from the UI container.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class PlanetGeneratorDataAndValidationHelper {

    private static final Logger logger = LoggerFactory.getLogger(PlanetGeneratorDataAndValidationHelper.class);

    /**
     * Private constructor preventing instantiation of this stateless helper class.
     */
    private PlanetGeneratorDataAndValidationHelper() {
    }

    /**
     * Configures the {@link PresetControlBar.PresetActionsListener} handling selection, save,
     * delete, JSON export, and JSON import operations for planetary presets.
     *
     * @param topPresetBar          the preset control bar to bind
     * @param isUpdatingSupplier    supplies whether the host panel is currently applying a preset
     * @param validateSupplier      validates current UI state prior to saving
     * @param buildPresetSupplier   constructs a {@link PlanetPreset} snapshot from active UI controls
     * @param applyPresetAction     applies a selected or imported {@link PlanetPreset}
     * @param resetPreviewMode      callback resetting preview mode index on preset selection
     */
    public static void bindPresetBarListener(
            PresetControlBar<PlanetPreset> topPresetBar,
            BooleanSupplier isUpdatingSupplier,
            BooleanSupplier validateSupplier,
            Supplier<PlanetPreset> buildPresetSupplier,
            Consumer<PlanetPreset> applyPresetAction,
            Runnable resetPreviewMode) {
        topPresetBar.setListener(new PresetControlBar.PresetActionsListener<PlanetPreset>() {
            @Override
            public void onPresetSelected(PlanetPreset preset) {
                if (!isUpdatingSupplier.getAsBoolean()) {
                    applyPresetAction.accept(preset);
                    resetPreviewMode.run();
                }
            }

            @Override
            public void onSavePreset(String name) {
                if (!validateSupplier.getAsBoolean()) return;
                PlanetPreset current = buildPresetSupplier.get();
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
                    ObjectMapper mapper = EtherJsonHelper.getMapper();
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
                    ObjectMapper mapper = EtherJsonHelper.getMapper();
                    PlanetPreset preset = mapper.readValue(sourceFile, PlanetPreset.class);
                    topPresetBar.getPresetCombo().getItems().add(preset);
                    topPresetBar.getPresetCombo().setValue(preset);
                    applyPresetAction.accept(preset);
                    logger.info("Imported planet preset from {}", sourceFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to import planet preset", ex);
                }
            }
        });
    }

    /**
     * Assembles an immutable {@link PlanetPreset} record from all active astronomical,
     * topographic, climatic, and custom base64 raster states in the UI.
     *
     * @param presetCombo           active preset selector combo box
     * @param resolutionCombo       H3 resolution selector combo box
     * @param astro                 astronomical section builder
     * @param topo                  topography section builder
     * @param climate               climate section builder
     * @param customBiomeImage      optional custom biome raster image
     * @param cachedBiomeB64        cached Base64 PNG encoding for biome image (nullable)
     * @param setCachedBiomeB64     callback storing newly encoded biome Base64 PNG
     * @param customResourceImage   optional custom resource raster image
     * @param cachedResourceB64     cached Base64 PNG encoding for resource image (nullable)
     * @param setCachedResourceB64  callback storing newly encoded resource Base64 PNG
     * @return assembled {@link PlanetPreset}
     */
    public static PlanetPreset buildPresetFromUI(
            ComboBox<PlanetPreset> presetCombo,
            ComboBox<Integer> resolutionCombo,
            PlanetAstroSectionBuilder astro,
            PlanetTopographySectionBuilder topo,
            PlanetClimateSectionBuilder climate,
            Image customBiomeImage,
            String cachedBiomeB64,
            Consumer<String> setCachedBiomeB64,
            Image customResourceImage,
            String cachedResourceB64,
            Consumer<String> setCachedResourceB64) {
        long seed = topo.getSeed();
        long tempSeed = climate.getTempSeed(seed);
        long precipSeed = climate.getPrecipSeed(seed);
        long seasonSeed = climate.getSeasonSeed(seed);

        String presetName = presetCombo.getValue() != null ? presetCombo.getValue().name() : "Custom Planet";
        boolean isSat = astro.isSatellite();

        Image customElevImage = topo.getCustomElevationImage();
        String cachedElevB64 = topo.getCachedElevationBase64();
        if (customElevImage != null && cachedElevB64 == null) {
            cachedElevB64 = ImageMapLoader.imageToBase64Png(customElevImage);
            topo.setCachedElevationBase64(cachedElevB64);
        }

        if (customBiomeImage != null && cachedBiomeB64 == null) {
            cachedBiomeB64 = ImageMapLoader.imageToBase64Png(customBiomeImage);
            setCachedBiomeB64.accept(cachedBiomeB64);
        }
        if (customResourceImage != null && cachedResourceB64 == null) {
            cachedResourceB64 = ImageMapLoader.imageToBase64Png(customResourceImage);
            setCachedResourceB64.accept(cachedResourceB64);
        }

        Image customClimateImage = climate.getCustomClimateImage();
        String cachedClimateB64 = climate.getCachedClimateBase64();
        if (customClimateImage != null && cachedClimateB64 == null) {
            cachedClimateB64 = ImageMapLoader.imageToBase64Png(customClimateImage);
            climate.setCachedClimateBase64(cachedClimateB64);
        }

        Image customRainfallImage = climate.getCustomRainfallImage();
        String cachedRainfallB64 = climate.getCachedRainfallBase64();
        if (customRainfallImage != null && cachedRainfallB64 == null) {
            cachedRainfallB64 = ImageMapLoader.imageToBase64Png(customRainfallImage);
            climate.setCachedRainfallBase64(cachedRainfallB64);
        }

        Image customSeasonalityImage = climate.getCustomSeasonalityImage();
        String cachedSeasonalityB64 = climate.getCachedSeasonalityBase64();
        if (customSeasonalityImage != null && cachedSeasonalityB64 == null) {
            cachedSeasonalityB64 = ImageMapLoader.imageToBase64Png(customSeasonalityImage);
            climate.setCachedSeasonalityBase64(cachedSeasonalityB64);
        }

        boolean elevImport = topo.isElevationImportActive();
        boolean tempImport = climate.isTempImportActive();
        boolean precipImport = climate.isPrecipImportActive();
        boolean seasonImport = climate.isSeasonImportActive();

        return new PlanetPreset(
                presetName,
                resolutionCombo.getValue() != null ? resolutionCombo.getValue() : 6,
                astro.getRadiusKm(),
                astro.getDayLengthHours(),
                astro.getAxialTiltDegrees(),
                astro.getYearLengthDays(),
                astro.getDistanceToSunAU(),
                astro.getSolarLuminosity(),
                topo.getMinAltitudeMeters(),
                topo.getMaxAltitudeMeters(),
                astro.getAverageTempC(),
                seed,
                topo.getNoiseFrequency(),
                topo.getNoiseScale(),
                topo.getWaterLevel(),
                climate.getTemperatureGradient(),
                climate.getOxygenPercentage(),
                climate.getAlbedo(),
                climate.getAtmospherePressureAtm(),
                isSat,
                astro.getParentMass(),
                astro.getOrbitDistanceParent(),
                climate.getCo2Ppm(),
                presetCombo.getValue() != null ? presetCombo.getValue().seismicActivityLevel() : 2.5,
                presetCombo.getValue() != null ? presetCombo.getValue().volcanicActivityLevel() : 1.5,
                (elevImport && customElevImage != null) ? cachedElevB64 : null,
                (elevImport && customBiomeImage != null) ? cachedBiomeB64 : null,
                (elevImport && customResourceImage != null) ? cachedResourceB64 : null,
                (tempImport && customClimateImage != null) ? cachedClimateB64 : null,
                (precipImport && customRainfallImage != null) ? cachedRainfallB64 : null,
                (seasonImport && customSeasonalityImage != null) ? cachedSeasonalityB64 : null,
                elevImport,
                topo.getElevationMapSource(),
                tempImport,
                climate.getTempSource(),
                tempSeed,
                precipImport,
                climate.getPrecipSource(),
                precipSeed,
                seasonImport,
                climate.getSeasonSource(),
                seasonSeed
        );
    }

    /**
     * Loads internal planetary raster layers (elevation, biomes, temperature, precipitation, seasonality)
     * for Mars, Venus, Moon, or Mercury.
     *
     * @param sourceKey     celestial body key ({@code "mars"}, {@code "venus"}, {@code "moon"}, {@code "mercury"})
     * @param topo          topography section builder
     * @param climate       climate section builder
     * @param setBiomeImage callback receiving loaded biome {@link Image}
     */
    public static void loadSolarSystemBodyMaps(
            String sourceKey,
            PlanetTopographySectionBuilder topo,
            PlanetClimateSectionBuilder climate,
            Consumer<Image> setBiomeImage) {
        String prefix = switch (sourceKey) {
            case "mars" -> "mars";
            case "venus" -> "venus";
            case "moon" -> "moon";
            default -> "mercury";
        };
        topo.setCustomElevationImage(ImageMapLoader.loadMapImage(prefix + "_elevation.png"));
        setBiomeImage.accept(ImageMapLoader.loadMapImage(prefix + "_biomes.png"));
        climate.setCustomClimateImage(ImageMapLoader.loadMapImage(prefix + "_temperature.png"));
        climate.setCustomRainfallImage(ImageMapLoader.loadMapImage(prefix + "_precipitation.png"));
        climate.setCustomSeasonalityImage(ImageMapLoader.loadMapImage(prefix + "_seasonality.png"));
    }

    /**
     * Exports procedural heightmap or climate raster with ESRI world file via {@link PlanetGeospatialExportHelper}.
     *
     * @param owner     parent window
     * @param type      {@code null} for heightmap, or climate map type ({@code "temperature"}, {@code "precipitation"}, {@code "seasonality"})
     * @param preset    active {@link PlanetPreset}
     * @param generator procedural terrain generator
     * @param climate   climate section builder providing deterministic seed offsets
     */
    public static void exportProceduralMap(
            Window owner,
            String type,
            PlanetPreset preset,
            ProceduralGenerator generator,
            PlanetClimateSectionBuilder climate) {
        long tSeed = climate.getTempSeed(preset.seed());
        long pSeed = climate.getPrecipSeed(preset.seed());
        long sSeed = climate.getSeasonSeed(preset.seed());
        if (type == null) {
            PlanetGeospatialExportHelper.exportProceduralHeightmap(
                    owner, preset, generator, tSeed, pSeed, sSeed,
                    PlanetPreviewPaneBuilder::getHypsometricColor);
        } else {
            double co2Forcing = 3.0 * (Math.log(Math.max(1.0, preset.co2Ppm()) / 280.0) / Math.log(2.0));
            double pressureBoost = (preset.atmospherePressureAtm() - 1.0) * 8.0;
            double minTemp = preset.averageTempC() + co2Forcing + pressureBoost - preset.temperatureGradient() - 20.0;
            double maxTemp = preset.averageTempC() + co2Forcing + pressureBoost + preset.temperatureGradient();
            PlanetGeospatialExportHelper.exportProceduralClimateMap(
                    owner, type, preset, generator, tSeed, pSeed, sSeed,
                    temp -> PlanetPreviewPaneBuilder.getTemperatureColor(temp, minTemp, maxTemp),
                    PlanetPreviewPaneBuilder::getPrecipitationColor,
                    PlanetPreviewPaneBuilder::getSeasonalityColor);
        }
    }

    /**
     * Asynchronously fetches online thermal and precipitation maps for the selected celestial body.
     *
     * @param onlineMapService online WMS/raster fetching service
     * @param sourceKey        elevation map source identifier
     * @param climate          climate section builder
     * @param onUpdated        UI callback invoked on JavaFX thread after image assignment
     */
    public static void fetchOnlineClimateData(
            OnlineMapService onlineMapService,
            String sourceKey,
            PlanetClimateSectionBuilder climate,
            Runnable onUpdated) {
        OnlineMapService.CelestialBody body = switch (sourceKey) {
            case "mars" -> OnlineMapService.CelestialBody.MARS;
            case "moon" -> OnlineMapService.CelestialBody.MOON;
            case "venus" -> OnlineMapService.CelestialBody.VENUS;
            default -> OnlineMapService.CelestialBody.EARTH;
        };
        onlineMapService.fetchClimateMapAsync(body).thenAccept(img -> Platform.runLater(() -> {
            if (img != null) {
                climate.setCustomClimateImage(img);
                onUpdated.run();
            }
        }));
        onlineMapService.fetchRainfallMapAsync(body).thenAccept(img -> Platform.runLater(() -> {
            if (img != null) {
                climate.setCustomRainfallImage(img);
                onUpdated.run();
            }
        }));
    }

    /**
     * Collects localized validation errors for missing or invalid imported rasters and altitude ranges.
     *
     * @param topo    topography section builder
     * @param climate climate section builder
     * @return list of localized error messages (empty when valid)
     */
    public static List<String> getValidationErrors(
            PlanetTopographySectionBuilder topo,
            PlanetClimateSectionBuilder climate) {
        List<String> errors = new ArrayList<>();
        checkImportedImage(errors, topo.isElevationImportActive(), topo.getCustomElevationImage(),
                "planet.validation.missing_elev_map", "Image heightmap d'altitude manquante en mode d'importation (Onglet 1).",
                "planet.validation.invalid_elev_map", "Image heightmap d'altitude incompatible (Onglet 1) : ");
        checkImportedImage(errors, climate.isTempImportActive(), climate.getCustomClimateImage(),
                "planet.validation.missing_temp_map", "Missing thermal map in import mode (Tab 1).",
                "planet.validation.invalid_temp_map", "Thermal map incompatible (Tab 1): ");
        checkImportedImage(errors, climate.isPrecipImportActive(), climate.getCustomRainfallImage(),
                "planet.validation.missing_precip_map", "Missing rainfall map in import mode (Tab 1).",
                "planet.validation.invalid_precip_map", "Rainfall map incompatible (Tab 1): ");
        checkImportedImage(errors, climate.isSeasonImportActive(), climate.getCustomSeasonalityImage(),
                "planet.validation.missing_season_map", "Missing seasonality map in import mode (Tab 1).",
                "planet.validation.invalid_season_map", "Seasonality map incompatible (Tab 1): ");
        if (topo.getMinAltitudeMeters() >= topo.getMaxAltitudeMeters()) {
            errors.add(I18n.getOrDefault("planet.validation.invalid_alt_range",
                    "Minimum altitude must be strictly lower than maximum altitude (Tab 1)."));
        }
        return errors;
    }

    private static void checkImportedImage(
            List<String> errors, boolean importActive, Image img,
            String missingKey, String missingDef, String invalidKey, String invalidDef) {
        if (!importActive) return;
        if (img == null) {
            errors.add(I18n.getOrDefault(missingKey, missingDef));
        } else {
            var val = ImageMapLoader.validateMapImage(img);
            if (!val.valid()) {
                errors.add(I18n.getOrDefault(invalidKey, invalidDef) + val.message());
            }
        }
    }

    /**
     * Applies red error border styling and updates the warning banner according to validation state.
     *
     * @param errors        validation errors list
     * @param showDialog    whether to reveal the inline warning banner when invalid
     * @param topo          topography section builder
     * @param climate       climate section builder
     * @param warningBanner validation warning container
     * @param warningLabel  validation warning message label
     * @return {@code true} if valid; {@code false} otherwise
     */
    public static boolean applyValidationVisuals(
            List<String> errors,
            boolean showDialog,
            PlanetTopographySectionBuilder topo,
            PlanetClimateSectionBuilder climate,
            VBox warningBanner,
            Label warningLabel) {
        boolean isValid = errors.isEmpty();
        String errBorder = "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;";

        boolean elevBad = topo.isElevationImportActive() && (topo.getCustomElevationImage() == null || !ImageMapLoader.validateMapImage(topo.getCustomElevationImage()).valid());
        if (topo.getLoadElevBtn() != null) topo.getLoadElevBtn().setStyle(elevBad ? errBorder : "");

        boolean tempBad = climate.isTempImportActive() && (climate.getCustomClimateImage() == null || !ImageMapLoader.validateMapImage(climate.getCustomClimateImage()).valid());
        if (climate.getLoadClimateBtn() != null) climate.getLoadClimateBtn().setStyle(tempBad ? errBorder : "");

        boolean precipBad = climate.isPrecipImportActive() && (climate.getCustomRainfallImage() == null || !ImageMapLoader.validateMapImage(climate.getCustomRainfallImage()).valid());
        if (climate.getLoadRainfallBtn() != null) climate.getLoadRainfallBtn().setStyle(precipBad ? errBorder : "");

        boolean seasonBad = climate.isSeasonImportActive() && (climate.getCustomSeasonalityImage() == null || !ImageMapLoader.validateMapImage(climate.getCustomSeasonalityImage()).valid());
        if (climate.getLoadSeasonalityBtn() != null) climate.getLoadSeasonalityBtn().setStyle(seasonBad ? errBorder : "");

        if (!isValid) {
            if (showDialog && warningLabel != null && warningBanner != null) {
                StringBuilder sb = new StringBuilder();
                for (String err : errors) sb.append("â€¢ ").append(err).append("\n");
                warningLabel.setText(sb.toString().trim());
                warningBanner.setVisible(true);
                warningBanner.setManaged(true);
            }
        } else if (warningBanner != null) {
            warningBanner.setVisible(false);
            warningBanner.setManaged(false);
        }
        return isValid;
    }
}

