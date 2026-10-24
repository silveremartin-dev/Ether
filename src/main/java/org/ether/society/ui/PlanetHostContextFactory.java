/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.scene.image.Image;
import javafx.stage.Window;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.ui.planet.PlanetAstroSectionBuilder;
import org.ether.society.ui.planet.PlanetClimateSectionBuilder;
import org.ether.society.ui.planet.PlanetPreviewPaneBuilder;
import org.ether.society.ui.planet.PlanetTopographySectionBuilder;

/**
 * Factory constructing host contexts and callback bridges for all autonomous section builders
 * within {@link PlanetGeneratorPanel}.
 *
 * <p>Extracted from {@code PlanetGeneratorPanel} to satisfy the Single Responsibility Principle (SRP)
 * and eliminate inline anonymous classes, establishing decoupling between the root JavaFX container
 * and delegated planetary subsystem controllers.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
final class PlanetHostContextFactory {

    private PlanetHostContextFactory() {
        // Utility factory
    }

    static PlanetAstroSectionBuilder.HostContext createAstroHostContext(PlanetGeneratorPanel panel) {
        return new PlanetAstroSectionBuilder.HostContext() {
            @Override public void notifyParamChange() { panel.notifyParamChange(); }
            @Override public void updatePreview() { panel.updatePreview(); }
            @Override public boolean isUpdatingFromPreset() { return panel.isUpdatingFromPreset; }
            @Override public double getAtmosphericPressure() {
                return panel.climateSectionBuilder != null ? panel.climateSectionBuilder.getAtmospherePressureAtm() : 1.0;
            }
            @Override public double getCo2Ppm() {
                return panel.climateSectionBuilder != null ? panel.climateSectionBuilder.getCo2Ppm() : 420.0;
            }
            @Override public double getAlbedo() {
                return panel.climateSectionBuilder != null ? panel.climateSectionBuilder.getAlbedo() : 0.30;
            }
        };
    }

    static PlanetTopographySectionBuilder.HostContext createTopographyHostContext(PlanetGeneratorPanel panel) {
        return new PlanetTopographySectionBuilder.HostContext() {
            @Override public void notifyParamChange() { panel.notifyParamChange(); }
            @Override public void updatePreview() { panel.updatePreview(); }
            @Override public boolean isUpdatingFromPreset() { return panel.isUpdatingFromPreset; }
            @Override public void selectViewMode(int modeIndex) {
                if (panel.previewPaneBuilder != null) panel.previewPaneBuilder.selectViewMode(modeIndex);
            }
            @Override public void applyMapSourcePreset(String sourceKey) { panel.applyMapSourcePreset(sourceKey); }
            @Override public void exportProceduralHeightmap() { panel.exportProceduralHeightmap(); }
            @Override public double getRadiusKm() {
                return panel.astroSectionBuilder != null ? panel.astroSectionBuilder.getRadiusKm() : 6371.0;
            }
            @Override public Window getWindow() { return panel.getScene() != null ? panel.getScene().getWindow() : null; }
        };
    }

    static PlanetClimateSectionBuilder.HostContext createClimateHostContext(PlanetGeneratorPanel panel) {
        return new PlanetClimateSectionBuilder.HostContext() {
            @Override public void notifyParamChange() { panel.notifyParamChange(); }
            @Override public void updatePreview() { panel.updatePreview(); }
            @Override public boolean isUpdatingFromPreset() { return panel.isUpdatingFromPreset; }
            @Override public void calculateStellarIrradiance() {
                if (panel.astroSectionBuilder != null) panel.astroSectionBuilder.calculateStellarIrradiance();
            }
            @Override public void selectViewMode(int modeIndex) {
                if (panel.previewPaneBuilder != null) panel.previewPaneBuilder.selectViewMode(modeIndex);
            }
            @Override public void exportProceduralClimateMap(String type) { panel.exportProceduralClimateMap(type); }
            @Override public void fetchOnlineClimateData() { panel.fetchOnlineClimateData(); }
            @Override public Window getWindow() { return panel.getScene() != null ? panel.getScene().getWindow() : null; }
        };
    }

    static PlanetPreviewPaneBuilder.HostContext createPreviewPaneHostContext(PlanetGeneratorPanel panel) {
        return new PlanetPreviewPaneBuilder.HostContext() {
            @Override public PlanetPreset buildPresetFromUI() { return panel.buildPresetFromUI(); }
            @Override public ProceduralGenerator getGenerator() { return panel.generator; }
            @Override public Image getCustomElevationImage() {
                return panel.topoSectionBuilder != null ? panel.topoSectionBuilder.getCustomElevationImage() : null;
            }
            @Override public boolean isElevationImportActive() {
                return panel.topoSectionBuilder != null && panel.topoSectionBuilder.isElevationImportActive();
            }
            @Override public Image getCustomClimateImage() {
                return panel.climateSectionBuilder != null ? panel.climateSectionBuilder.getCustomClimateImage() : null;
            }
            @Override public boolean isTempImportActive() {
                return panel.climateSectionBuilder != null && panel.climateSectionBuilder.isTempImportActive();
            }
            @Override public Image getCustomRainfallImage() {
                return panel.climateSectionBuilder != null ? panel.climateSectionBuilder.getCustomRainfallImage() : null;
            }
            @Override public boolean isPrecipImportActive() {
                return panel.climateSectionBuilder != null && panel.climateSectionBuilder.isPrecipImportActive();
            }
            @Override public Image getCustomSeasonalityImage() {
                return panel.climateSectionBuilder != null ? panel.climateSectionBuilder.getCustomSeasonalityImage() : null;
            }
            @Override public boolean isSeasonImportActive() {
                return panel.climateSectionBuilder != null && panel.climateSectionBuilder.isSeasonImportActive();
            }
            @Override public long getTempSeed(long baseSeed) {
                return panel.climateSectionBuilder != null ? panel.climateSectionBuilder.getTempSeed(baseSeed) : baseSeed + 100L;
            }
            @Override public long getPrecipSeed(long baseSeed) {
                return panel.climateSectionBuilder != null ? panel.climateSectionBuilder.getPrecipSeed(baseSeed) : baseSeed + 1000L;
            }
            @Override public long getSeasonSeed(long baseSeed) {
                return panel.climateSectionBuilder != null ? panel.climateSectionBuilder.getSeasonSeed(baseSeed) : baseSeed + 2000L;
            }
            @Override public void setBusyCursor(boolean busy) {
                WindowUtils.setBusyCursor(panel, busy);
            }
        };
    }
}
