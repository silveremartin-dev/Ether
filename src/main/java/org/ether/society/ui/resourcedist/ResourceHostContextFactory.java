/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.resourcedist;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.data.OnlineMapService;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.i18n.I18n;
import org.ether.society.model.EcologyPreset;
import org.ether.society.ui.PresetControlBar;
import org.ether.society.ui.ResourceDistributionPanel;
import org.ether.society.ui.resource.ResourceBiomeSectionBuilder;
import org.ether.society.ui.resource.ResourceClimateSectionBuilder;
import org.ether.society.ui.resource.ResourceGeologySectionBuilder;
import org.ether.society.ui.resource.ResourceHydroSectionBuilder;
import org.ether.society.ui.resource.ResourceMapExportHandler;
import org.ether.society.ui.resource.ResourcePlanetSectionBuilder;
import org.ether.society.ui.resource.ResourcePresetManager;
import org.ether.society.ui.resource.ResourcePreviewCanvasBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Host context factory and UI metric helper for {@link ResourceDistributionPanel}.
 *
 * <p>Extracted in accordance with the Single Responsibility Principle (SRP) to construct
 * callback bridges for all Tab 2 domain builders (geology, hydrography, biome, climate,
 * preview canvas, preset manager, planet inheritance, and GIS export) without polluting
 * the root {@link ResourceDistributionPanel} layout class.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ResourceHostContextFactory {

    private static final Logger logger = LoggerFactory.getLogger(ResourceHostContextFactory.class);

    /**
     * Private constructor preventing instantiation of this stateless factory class.
     */
    private ResourceHostContextFactory() {
    }

    /**
     * Creates the {@link ResourceGeologySectionBuilder.HostContext} bridge.
     *
     * @param panel             host panel
     * @param notifyParamChange callback marking preset dirty on user edits
     * @param effectivePreset   supplier resolving active {@link PlanetPreset}
     * @param isUpdating        supplier indicating preset batch update state
     * @param epochSupplier     supplier resolving current historical epoch year
     * @return configured geology host context
     */
    public static ResourceGeologySectionBuilder.HostContext createGeologyContext(
            ResourceDistributionPanel panel,
            Runnable notifyParamChange,
            Supplier<PlanetPreset> effectivePreset,
            BooleanSupplier isUpdating,
            LongSupplier epochSupplier) {
        return new ResourceGeologySectionBuilder.HostContext() {
            @Override public void notifyParamChange() { notifyParamChange.run(); }
            @Override public void updatePreviewCanvas() { panel.updatePreviewCanvas(); }
            @Override public int getGeologyViewComboIndex(int layerIdx) { return panel.getGeologyViewComboIndex(layerIdx); }
            @Override public void selectViewMode(int viewModeIndex) { panel.selectViewMode(viewModeIndex); }
            @Override public PlanetPreset getActivePlanetPreset() { return effectivePreset.get(); }
            @Override public Window getWindow() { return panel.getScene() != null ? panel.getScene().getWindow() : null; }
            @Override public boolean isUpdatingFromPreset() { return isUpdating.getAsBoolean(); }
            @Override public long getEpochYear() { return epochSupplier.getAsLong(); }
        };
    }

    /**
     * Creates the {@link ResourceHydroSectionBuilder.HostContext} bridge.
     *
     * @param panel             host panel
     * @param notifyParamChange callback marking preset dirty on user edits
     * @param effectivePreset   supplier resolving active {@link PlanetPreset}
     * @param isUpdating        supplier indicating preset batch update state
     * @param onlineMapService  WMS map fetching service
     * @return configured hydrography host context
     */
    public static ResourceHydroSectionBuilder.HostContext createHydroContext(
            ResourceDistributionPanel panel,
            Runnable notifyParamChange,
            Supplier<PlanetPreset> effectivePreset,
            BooleanSupplier isUpdating,
            OnlineMapService onlineMapService) {
        return new ResourceHydroSectionBuilder.HostContext() {
            @Override public void notifyParamChange() { notifyParamChange.run(); }
            @Override public void updatePreviewCanvas() { panel.updatePreviewCanvas(); }
            @Override public void selectViewMode(int viewModeIndex) { panel.selectViewMode(viewModeIndex); }
            @Override public PlanetPreset getActivePlanetPreset() { return effectivePreset.get(); }
            @Override public Window getWindow() { return panel.getScene() != null ? panel.getScene().getWindow() : null; }
            @Override public boolean isUpdatingFromPreset() { return isUpdating.getAsBoolean(); }
            @Override public OnlineMapService getOnlineMapService() { return onlineMapService; }
            @Override public String getMapSourceComboValue() { return "earth"; }
            @Override public void setMapStatus(String status) {}
        };
    }

    /**
     * Creates the {@link ResourceBiomeSectionBuilder.HostContext} bridge.
     *
     * @param panel             host panel
     * @param notifyParamChange callback marking preset dirty on user edits
     * @param updateSummary     callback refreshing ecological summary metrics
     * @param effectivePreset   supplier resolving active {@link PlanetPreset}
     * @param isUpdating        supplier indicating preset batch update state
     * @param onlineMapService  WMS map fetching service
     * @return configured biome host context
     */
    public static ResourceBiomeSectionBuilder.HostContext createBiomeContext(
            ResourceDistributionPanel panel,
            Runnable notifyParamChange,
            Runnable updateSummary,
            Supplier<PlanetPreset> effectivePreset,
            BooleanSupplier isUpdating,
            OnlineMapService onlineMapService) {
        return new ResourceBiomeSectionBuilder.HostContext() {
            @Override public void notifyParamChange() { notifyParamChange.run(); }
            @Override public void updatePreviewCanvas() { panel.updatePreviewCanvas(); }
            @Override public void updateSummary() { updateSummary.run(); }
            @Override public void selectViewMode(int viewModeIndex) { panel.selectViewMode(viewModeIndex); }
            @Override public PlanetPreset getActivePlanetPreset() { return effectivePreset.get(); }
            @Override public Window getWindow() { return panel.getScene() != null ? panel.getScene().getWindow() : null; }
            @Override public boolean isUpdatingFromPreset() { return isUpdating.getAsBoolean(); }
            @Override public OnlineMapService getOnlineMapService() { return onlineMapService; }
            @Override public String getMapSourceComboValue() { return "earth"; }
            @Override public void setMapStatus(String status) {}
            @Override public void applyPresetMapSource(String sourceKey) { panel.getPlanetSectionBuilder().applyPresetMapSource(sourceKey); }
        };
    }

    /**
     * Creates the {@link ResourceClimateSectionBuilder.HostContext} bridge.
     *
     * @param panel             host panel
     * @param notifyParamChange callback marking preset dirty on user edits
     * @param effectivePreset   supplier resolving active {@link PlanetPreset}
     * @param isUpdating        supplier indicating preset batch update state
     * @param onlineMapService  WMS map fetching service
     * @return configured climate host context
     */
    public static ResourceClimateSectionBuilder.HostContext createClimateContext(
            ResourceDistributionPanel panel,
            Runnable notifyParamChange,
            Supplier<PlanetPreset> effectivePreset,
            BooleanSupplier isUpdating,
            OnlineMapService onlineMapService) {
        return new ResourceClimateSectionBuilder.HostContext() {
            @Override public void notifyParamChange() { notifyParamChange.run(); }
            @Override public void updatePreviewCanvas() { panel.updatePreviewCanvas(); }
            @Override public PlanetPreset getActivePlanetPreset() { return effectivePreset.get(); }
            @Override public Window getWindow() { return panel.getScene() != null ? panel.getScene().getWindow() : null; }
            @Override public boolean isUpdatingFromPreset() { return isUpdating.getAsBoolean(); }
            @Override public OnlineMapService getOnlineMapService() { return onlineMapService; }
            @Override public String getMapSourceComboValue() { return "earth"; }
            @Override public void setMapStatus(String status) {}
        };
    }

    /**
     * Creates the {@link ResourcePreviewCanvasBuilder.HostContext} bridge.
     *
     * @param panel           host panel
     * @param effectivePreset supplier resolving active {@link PlanetPreset}
     * @param seismicSlider   supplier for seismic activity slider
     * @param volcanicSlider  supplier for volcanic activity slider
     * @param geologySupplier supplier for geology section builder
     * @param generator       procedural terrain generator
     * @param mapLoader       image raster loader
     * @return configured preview canvas host context
     */
    public static ResourcePreviewCanvasBuilder.HostContext createPreviewCanvasContext(
            ResourceDistributionPanel panel,
            Supplier<PlanetPreset> effectivePreset,
            Supplier<Slider> seismicSlider,
            Supplier<Slider> volcanicSlider,
            Supplier<ResourceGeologySectionBuilder> geologySupplier,
            ProceduralGenerator generator,
            ImageMapLoader mapLoader) {
        return new ResourcePreviewCanvasBuilder.HostContext() {
            @Override public PlanetPreset getActivePlanetPreset() { return effectivePreset.get(); }
            @Override public double getSeismicActivityValue() { Slider s = seismicSlider.get(); return s != null ? s.getValue() : 4.5; }
            @Override public double getVolcanicActivityValue() { Slider s = volcanicSlider.get(); return s != null ? s.getValue() : 2.0; }
            @Override public boolean isRadioImportBiomeSelected() { return panel.isRadioImportBiomeSelected(); }
            @Override public Image getCustomBiomeImage() { return panel.getCustomBiomeImage(); }
            @Override public boolean isRadioImportHydroSelected() { return panel.isRadioImportHydroSelected(); }
            @Override public Image getCustomHydroImage() { return panel.getCustomHydroImage(); }
            @Override public ResourceGeologySectionBuilder getGeologySectionBuilder() { return geologySupplier.get(); }
            @Override public ProceduralGenerator getProceduralGenerator() { return generator; }
            @Override public ImageMapLoader getMapLoader() { return mapLoader; }
            @Override public Node getHostNode() { return panel; }
        };
    }

    /**
     * Creates the {@link ResourcePresetManager.HostContext} bridge.
     *
     * @param panel                     host panel
     * @param effectivePreset           supplier resolving active {@link PlanetPreset}
     * @param rawPresetSetter           setter updating raw active planet preset field
     * @param planetPresetSupplier      supplier for Tab 1 planet preset bridge
     * @param planetPresetApplyCallback consumer propagating planet preset back to Tab 1
     * @param crustalSlider             supplier for crustal metal slider
     * @param preciousSlider            supplier for precious metal slider
     * @param mantleSlider              supplier for mantle heat slider
     * @param seedFieldSupplier         supplier for resource seed text field
     * @param customResImgGetter        getter for custom resource image
     * @param customResImgSetter        setter for custom resource image
     * @param resFileLabelSetter        setter updating resource file label text
     * @param updateSummary             callback refreshing summary label
     * @return configured preset manager host context
     */
    public static ResourcePresetManager.HostContext createPresetManagerContext(
            ResourceDistributionPanel panel,
            Supplier<PlanetPreset> effectivePreset,
            Consumer<PlanetPreset> rawPresetSetter,
            Supplier<Supplier<PlanetPreset>> planetPresetSupplier,
            Supplier<Consumer<PlanetPreset>> planetPresetApplyCallback,
            Supplier<Slider> crustalSlider,
            Supplier<Slider> preciousSlider,
            Supplier<Slider> mantleSlider,
            Supplier<TextField> seedFieldSupplier,
            Supplier<Image> customResImgGetter,
            Consumer<Image> customResImgSetter,
            Consumer<String> resFileLabelSetter,
            Runnable updateSummary) {
        return new ResourcePresetManager.HostContext() {
            @Override public PlanetPreset getActivePlanetPreset() { return effectivePreset.get(); }
            @Override public void setActivePlanetPreset(PlanetPreset p) { rawPresetSetter.accept(p); }
            @Override public ComboBox<PlanetPreset> getPlanetPresetCombo() { return panel.getPlanetPresetCombo(); }
            @Override public Supplier<PlanetPreset> getPlanetPresetSupplier() { return planetPresetSupplier.get(); }
            @Override public Consumer<PlanetPreset> getPlanetPresetApplyCallback() { return planetPresetApplyCallback.get(); }
            @Override public ResourceBiomeSectionBuilder getBiomeSectionBuilder() { return panel.getBiomeSectionBuilder(); }
            @Override public ResourceHydroSectionBuilder getHydroSectionBuilder() { return panel.getHydroSectionBuilder(); }
            @Override public ResourceClimateSectionBuilder getClimateSectionBuilder() { return panel.getClimateSectionBuilder(); }
            @Override public Slider getCrustalMetalSlider() { return crustalSlider.get(); }
            @Override public Slider getPreciousMetalSlider() { return preciousSlider.get(); }
            @Override public Slider getMantleHeatSlider() { return mantleSlider.get(); }
            @Override public TextField getSeedField() { return seedFieldSupplier.get(); }
            @Override public Image getCustomResourceImage() { return customResImgGetter.get(); }
            @Override public void setCustomResourceImage(Image img) { customResImgSetter.accept(img); }
            @Override public void setResourceFileLabelText(String text) { resFileLabelSetter.accept(text); }
            @Override public boolean validateResourceSetup() { return panel.validateResourceSetup(); }
            @Override public void selectViewMode(int viewModeIndex) { panel.selectViewMode(viewModeIndex); }
            @Override public void updateSummary() { updateSummary.run(); }
            @Override public void updateLegend() { panel.updateLegend(); }
            @Override public void updatePreviewCanvas() { panel.updatePreviewCanvas(); }
            @Override public void prepopulateGeologyTensors(String planetKey, long epochYear) { panel.prepopulateGeologyTensors(planetKey, epochYear); }
            @Override public String getGeologySourceDisplayName(String planetKey) { return panel.getGeologySourceDisplayName(planetKey); }
        };
    }

    /**
     * Creates the {@link ResourcePlanetSectionBuilder.HostContext} bridge.
     *
     * @param panel                host panel
     * @param rawPresetGetter      getter for raw active planet preset field
     * @param rawPresetSetter      setter for raw active planet preset field
     * @param presetManager        supplier for resource preset manager
     * @param geologySupplier      supplier for geology section builder
     * @param crustalSlider        supplier for crustal metal slider
     * @param preciousSlider       supplier for precious metal slider
     * @param mantleSlider         supplier for mantle heat slider
     * @param seismicSlider        supplier for seismic activity slider
     * @param volcanicSlider       supplier for volcanic activity slider
     * @param ecoCompatLabel       supplier for ecological compatibility badge
     * @param resFileLabel         supplier for resource file label
     * @param ecoBarSupplier       supplier for ecology preset control bar
     * @param updateSummary        callback refreshing summary label
     * @param activeCellCount      supplier returning active cell count
     * @param planetPresetSupplier supplier for Tab 1 planet preset bridge
     * @return configured planet section host context
     */
    public static ResourcePlanetSectionBuilder.HostContext createPlanetSectionContext(
            ResourceDistributionPanel panel,
            Supplier<PlanetPreset> rawPresetGetter,
            Consumer<PlanetPreset> rawPresetSetter,
            Supplier<ResourcePresetManager> presetManager,
            Supplier<ResourceGeologySectionBuilder> geologySupplier,
            Supplier<Slider> crustalSlider,
            Supplier<Slider> preciousSlider,
            Supplier<Slider> mantleSlider,
            Supplier<Slider> seismicSlider,
            Supplier<Slider> volcanicSlider,
            Supplier<Label> ecoCompatLabel,
            Supplier<Label> resFileLabel,
            Supplier<PresetControlBar<EcologyPreset>> ecoBarSupplier,
            Runnable updateSummary,
            IntSupplier activeCellCount,
            Supplier<Supplier<PlanetPreset>> planetPresetSupplier) {
        return new ResourcePlanetSectionBuilder.HostContext() {
            @Override public PlanetPreset getActivePlanetPreset() { return rawPresetGetter.get(); }
            @Override public void setActivePlanetPreset(PlanetPreset preset) { rawPresetSetter.accept(preset); }
            @Override public boolean isUpdatingFromPreset() { return presetManager.get().isUpdatingFromPreset(); }
            @Override public void setUpdatingFromPreset(boolean updating) { presetManager.get().setUpdatingFromPreset(updating); }
            @Override public ResourceBiomeSectionBuilder getBiomeSectionBuilder() { return panel.getBiomeSectionBuilder(); }
            @Override public ResourceHydroSectionBuilder getHydroSectionBuilder() { return panel.getHydroSectionBuilder(); }
            @Override public ResourceClimateSectionBuilder getClimateSectionBuilder() { return panel.getClimateSectionBuilder(); }
            @Override public ResourceGeologySectionBuilder getGeologySectionBuilder() { return geologySupplier.get(); }
            @Override public Slider getCrustalMetalSlider() { return crustalSlider.get(); }
            @Override public Slider getPreciousMetalSlider() { return preciousSlider.get(); }
            @Override public Slider getMantleHeatSlider() { return mantleSlider.get(); }
            @Override public Slider getSeismicActivitySlider() { return seismicSlider.get(); }
            @Override public Slider getVolcanicActivitySlider() { return volcanicSlider.get(); }
            @Override public Label getEcoCompatibilityLabel() { return ecoCompatLabel.get(); }
            @Override public Label getResourceFileLabel() { return resFileLabel.get(); }
            @Override public PresetControlBar<EcologyPreset> getEcologyPresetBar() { return ecoBarSupplier.get(); }
            @Override public void updateSummary() { updateSummary.run(); }
            @Override public void updatePreviewCanvas() { panel.updatePreviewCanvas(); }
            @Override public void updateLegend() { panel.updateLegend(); }
            @Override public int getActiveCellCount() { return activeCellCount.getAsInt(); }
            @Override public String getGeologySourceDisplayName(String body) { return panel.getGeologySourceDisplayName(body); }
            @Override public void prepopulateGeologyTensors(String body) { panel.prepopulateGeologyTensors(body); }
            @Override public Supplier<PlanetPreset> getPlanetPresetSupplier() { return planetPresetSupplier.get(); }
        };
    }

    /**
     * Creates the {@link ResourceMapExportHandler.HostContext} bridge.
     *
     * @param panel           host panel
     * @param effectivePreset supplier resolving active {@link PlanetPreset}
     * @param seismicSlider   supplier for seismic activity slider
     * @param volcanicSlider  supplier for volcanic activity slider
     * @param generator       procedural terrain generator
     * @param mapLoader       image raster loader
     * @param geologySupplier supplier for geology section builder
     * @return configured map export host context
     */
    public static ResourceMapExportHandler.HostContext createMapExportContext(
            ResourceDistributionPanel panel,
            Supplier<PlanetPreset> effectivePreset,
            Supplier<Slider> seismicSlider,
            Supplier<Slider> volcanicSlider,
            ProceduralGenerator generator,
            ImageMapLoader mapLoader,
            Supplier<ResourceGeologySectionBuilder> geologySupplier) {
        return new ResourceMapExportHandler.HostContext() {
            @Override public Window getWindow() { return panel.getScene() != null ? panel.getScene().getWindow() : null; }
            @Override public int getViewModeIndex() { return panel.getViewModeIndex(); }
            @Override public PlanetPreset getActivePlanetPreset() { return effectivePreset.get(); }
            @Override public Double getSeismicActivityValue() { Slider s = seismicSlider.get(); return s != null ? s.getValue() : null; }
            @Override public Double getVolcanicActivityValue() { Slider s = volcanicSlider.get(); return s != null ? s.getValue() : null; }
            @Override public ProceduralGenerator getGenerator() { return generator; }
            @Override public ImageMapLoader getMapLoader() { return mapLoader; }
            @Override public ResourceBiomeSectionBuilder getBiomeSectionBuilder() { return panel.getBiomeSectionBuilder(); }
            @Override public ResourceHydroSectionBuilder getHydroSectionBuilder() { return panel.getHydroSectionBuilder(); }
            @Override public ResourceGeologySectionBuilder getGeologySectionBuilder() { return geologySupplier.get(); }
        };
    }

    /**
     * Returns the localized mineralogical data source display name for a given celestial body.
     *
     * @param planetKey celestial body identifier
     * @return human-readable geological data source label
     */
    public static String getGeologySourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase(Locale.ROOT)) {
            case "mars" -> "🪨 Mars CRISM / GRS Mineral Maps";
            case "moon" -> "🪨 Moon Clementine / Kaguya Mineralogy";
            case "venus" -> "🪨 Venus Magellan Synthetic Aperture Radar Geology";
            case "mercury" -> "🪨 Mercury MDIS Compositional Units";
            default -> I18n.getOrDefault("resource.source.earth_geology", "🪨 Earth USGS MRDS / World Minerals");
        };
    }

    /**
     * Opens a PNG/JPEG file chooser to load a custom geological/mineral resource raster image.
     *
     * @param owner         parent window
     * @param fileLabel     label displaying loaded file name
     * @param onImageLoaded consumer receiving the loaded {@link Image}
     */
    public static void promptLoadCustomResourceMap(Window owner, Label fileLabel, Consumer<Image> onImageLoaded) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("resource.chooser.geology", "Load Geological & Ore Map (PNG/JPEG)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(owner);
        if (file != null) {
            try {
                Image img = new Image(new FileInputStream(file));
                if (fileLabel != null) fileLabel.setText("🪨 " + file.getName());
                onImageLoaded.accept(img);
            } catch (Exception ex) {
                logger.error("Failed to load custom resource map", ex);
            }
        }
    }

    /**
     * Creates a configured metric slider firing parameter-change and preview refresh callbacks.
     *
     * @param min             minimum slider bound
     * @param max             maximum slider bound
     * @param value           initial slider value
     * @param isUpdating      supplier indicating preset batch update state
     * @param onDirty         callback marking preset bar dirty
     * @param onValueSettled  callback refreshing summary and preview canvas
     * @return configured {@link Slider}
     */
    public static Slider createMetricSlider(
            double min, double max, double value,
            BooleanSupplier isUpdating, Runnable onDirty, Runnable onValueSettled) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.valueProperty().addListener((obs, old, val) -> {
            if (isUpdating.getAsBoolean()) return;
            onDirty.run();
            if (!slider.isValueChanging()) onValueSettled.run();
        });
        slider.setOnMouseReleased(e -> {
            if (isUpdating.getAsBoolean()) return;
            onDirty.run();
            onValueSettled.run();
        });
        return slider;
    }

    /**
     * Refreshes localized captions for panel-level labels and buttons.
     *
     * @param headerLabel      panel title label
     * @param mineralSecHeader geology section header (nullable)
     * @param crustalLabel     crustal metal label
     * @param preciousLabel    precious metal label
     * @param mantleLabel      mantle heat label
     * @param seismicLabel     seismic activity label
     * @param volcanicLabel    volcanic activity label
     * @param loadResourceBtn  custom resource load button
     * @param customResImg     loaded custom resource image (nullable)
     * @param resFileLabel     resource file label
     * @param applyBtn         apply distribution button (nullable)
     */
    public static void updatePanelTexts(
            Label headerLabel, Label mineralSecHeader, Label crustalLabel, Label preciousLabel,
            Label mantleLabel, Label seismicLabel, Label volcanicLabel, Button loadResourceBtn,
            Image customResImg, Label resFileLabel, Button applyBtn) {
        if (headerLabel != null) headerLabel.setText(I18n.getOrDefault("resource.title", "RESOURCE DISTRIBUTION & ECOLOGY (SCIENTIFIC METRICS)"));
        if (mineralSecHeader != null) mineralSecHeader.setText(I18n.getOrDefault("resource.section.minerals", "GEOLOGY & HYDROLOGY (METRICS)"));
        if (crustalLabel != null) crustalLabel.setText(I18n.getOrDefault("resource.param.crustal_metal", "Crustal Metal Wealth (Gt):"));
        if (preciousLabel != null) preciousLabel.setText(I18n.getOrDefault("resource.param.precious_metal", "Precious Metal Concentration (Mt):"));
        if (mantleLabel != null) mantleLabel.setText(I18n.getOrDefault("resource.param.mantle_heat", "Mantle Heat Flux (mW/m²):"));
        if (seismicLabel != null) seismicLabel.setText(I18n.getOrDefault("resource.param.seismic", "Seismic & Tectonic Activity (Mag Richter):"));
        if (volcanicLabel != null) volcanicLabel.setText(I18n.getOrDefault("resource.param.volcanic", "Global Volcanic Activity (VEI):"));
        if (loadResourceBtn != null) loadResourceBtn.setText(I18n.get("planet.map.btn_load"));
        if (customResImg == null && resFileLabel != null) resFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
        if (applyBtn != null) applyBtn.setText(I18n.getOrDefault("resource.btn.apply", "✅ Apply scientific distribution to simulation"));
    }
}
