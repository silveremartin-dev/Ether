/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.resource;

import javafx.scene.image.Image;
import javafx.stage.Window;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.ui.export.ResourceDistributionRasterExporter;

/**
 * <h2>Resource Map Export Handler</h2>
 * <p>
 * Handles procedural raster generation, cartographic file export (PNG, ESRI World Files),
 * and format guidance dialogs for {@link org.ether.society.ui.ResourceDistributionPanel}.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public class ResourceMapExportHandler {

    public interface HostContext {
        Window getWindow();
        int getViewModeIndex();
        PlanetPreset getActivePlanetPreset();
        Double getSeismicActivityValue();
        Double getVolcanicActivityValue();
        ProceduralGenerator getGenerator();
        ImageMapLoader getMapLoader();
        ResourceBiomeSectionBuilder getBiomeSectionBuilder();
        ResourceHydroSectionBuilder getHydroSectionBuilder();
        ResourceGeologySectionBuilder getGeologySectionBuilder();
    }

    private final HostContext host;

    /**
     * Constructs the export handler with host context.
     *
     * @param host host callback interface
     */
    public ResourceMapExportHandler(HostContext host) {
        this.host = host;
    }

    /**
     * Exports multi-spectral cartographic raster layers accompanied by ESRI World Files (.tfw / .pgw).
     */
    public void exportMapsWithWorldFiles() {
        ResourceDistributionRasterExporter.exportMapsWithWorldFiles(
                host.getWindow(),
                host.getViewModeIndex(),
                host.getActivePlanetPreset(),
                host.getSeismicActivityValue(),
                host.getVolcanicActivityValue(),
                host.getGenerator(),
                host.getMapLoader(),
                (layerIdx, lon, lat, planet) -> host.getGeologySectionBuilder().sampleProceduralGeologyTensor(layerIdx, lon, lat, planet),
                (layerIdx, val) -> host.getGeologySectionBuilder().getGeologyResourceColor(layerIdx, val)
        );
    }

    /**
     * Shows modal help dialog detailing valid raster map import dimensions and projection standards.
     */
    public void showEcologyImportFormatHelp() {
        ResourceDistributionRasterExporter.showEcologyImportFormatHelp();
    }

    /**
     * Generates a procedural biome raster image at the specified resolution.
     *
     * @param width image pixel width
     * @param height image pixel height
     * @return generated JavaFX {@link Image}
     */
    public Image generateProceduralBiomeRasterImage(int width, int height) {
        return host.getBiomeSectionBuilder().generateProceduralBiomeRasterImage(width, height);
    }

    /**
     * Triggers file export of the active biome raster map.
     */
    public void exportBiomeMap() {
        host.getBiomeSectionBuilder().exportBiomeMap();
    }

    /**
     * Generates a procedural hydrology raster image at the specified resolution.
     *
     * @param width image pixel width
     * @param height image pixel height
     * @return generated JavaFX {@link Image}
     */
    public Image generateProceduralHydroRasterImage(int width, int height) {
        return host.getHydroSectionBuilder().generateProceduralHydroRasterImage(width, height);
    }

    /**
     * Triggers file export of the active hydrographic raster map.
     */
    public void exportHydroMap() {
        host.getHydroSectionBuilder().exportHydroMap();
    }

    /**
     * Generates a procedural geology raster image for a specific layer.
     *
     * @param layerIdx tensor layer index [0..8]
     * @param width image pixel width
     * @param height image pixel height
     * @return generated JavaFX {@link Image}
     */
    public Image generateProceduralGeologyRasterImage(int layerIdx, int width, int height) {
        return host.getGeologySectionBuilder().generateProceduralGeologyRasterImage(layerIdx, width, height);
    }

    /**
     * Triggers file export of a specific geological tensor layer.
     *
     * @param layerIdx tensor layer index [0..8]
     */
    public void exportGeologyTensor(int layerIdx) {
        host.getGeologySectionBuilder().exportGeologyTensor(layerIdx);
    }
}
