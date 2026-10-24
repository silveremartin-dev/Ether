/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.scene.control.Control;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.model.EcologyPreset;
import org.ether.society.model.Scenario;
import org.ether.society.persistence.SaveMetadata;
import org.ether.society.persistence.ScenarioRepository;
import org.ether.society.ui.scenario.ScenarioBundleManager;
import org.ether.society.ui.scenario.ScenarioCulturalSectionBuilder;
import org.ether.society.ui.scenario.ScenarioDemographicSectionBuilder;
import org.ether.society.ui.scenario.ScenarioPlanetSyncManager;
import org.ether.society.ui.scenario.ScenarioPresetManager;
import org.ether.society.ui.scenario.ScenarioPreviewPaneBuilder;
import org.ether.society.ui.scenario.ScenarioSimulationLauncher;
import org.ether.society.ui.scenario.ScenarioSpatialBoundsSectionBuilder;
import org.ether.society.ui.scenario.ScenarioSpatiotemporalSectionBuilder;
import org.ether.society.ui.scenario.ScenarioThermodynamicAutocalc;

import java.util.List;
import java.util.function.Consumer;

/**
 * Factory constructing host contexts and callback bridges for all autonomous section builders
 * and subsystem controllers within {@link ScenarioSetupPanel}.
 *
 * <p>Extracted from {@code ScenarioSetupPanel} to satisfy the Single Responsibility Principle (SRP)
 * and eliminate verbose anonymous inner class boilerplate, establishing decoupling between the root
 * JavaFX container and delegated subsystem managers.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
final class ScenarioHostContextFactory {

    private ScenarioHostContextFactory() {
        // Utility factory class
    }

    /**
     * Creates host context for {@link ScenarioPlanetSyncManager}.
     *
     * @param panel the hosting scenario setup panel
     * @return initialized host context
     */
    static ScenarioPlanetSyncManager.HostContext createPlanetSyncHostContext(ScenarioSetupPanel panel) {
        return new ScenarioPlanetSyncManager.HostContext() {
            @Override public void notifyParamChange() { panel.notifyParamChange(); }
            @Override public void generatePreview() { panel.generatePreview(); }
            @Override public void drawPreview() { panel.drawPreview(); }
            @Override public boolean hasPreviewCells() { return panel.currentPreviewCells != null && !panel.currentPreviewCells.isEmpty(); }
        };
    }

    /**
     * Creates host context for {@link ScenarioSpatiotemporalSectionBuilder}.
     *
     * @param panel the hosting scenario setup panel
     * @return initialized host context
     */
    static ScenarioSpatiotemporalSectionBuilder.HostContext createSpatiotemporalHostContext(ScenarioSetupPanel panel) {
        return new ScenarioSpatiotemporalSectionBuilder.HostContext() {
            @Override public void notifyParamChange() { panel.notifyParamChange(); }
            @Override public void loadEarthHistoricalEvents() { panel.loadEarthHistoricalEvents(); }
            @Override public <T> void attachDefaultValueHandling(Control control, T defaultValue, Runnable resetAction) {
                panel.attachDefaultValueHandling(control, defaultValue, resetAction);
            }
        };
    }

    /**
     * Creates host context for {@link ScenarioSpatialBoundsSectionBuilder}.
     *
     * @param panel the hosting scenario setup panel
     * @return initialized host context
     */
    static ScenarioSpatialBoundsSectionBuilder.HostContext createSpatialBoundsHostContext(ScenarioSetupPanel panel) {
        return new ScenarioSpatialBoundsSectionBuilder.HostContext() {
            @Override public void notifyParamChange() { panel.notifyParamChange(); }
            @Override public void drawPreview() { panel.drawPreview(); }
            @Override public void resetPreviewCells() { panel.currentPreviewCells = null; }
            @Override public <T> void attachDefaultValueHandling(Control control, T defaultValue, Runnable resetAction) {
                panel.attachDefaultValueHandling(control, defaultValue, resetAction);
            }
        };
    }

    /**
     * Creates host context for {@link ScenarioBundleManager}.
     *
     * @param panel the hosting scenario setup panel
     * @return initialized host context
     */
    static ScenarioBundleManager.HostContext createBundleHostContext(ScenarioSetupPanel panel) {
        return new ScenarioBundleManager.HostContext() {
            @Override public Window getWindow() { return panel.getScene() != null ? panel.getScene().getWindow() : null; }
            @Override public PlanetPreset getActivePlanetPreset() {
                return panel.activePlanetPreset != null ? panel.activePlanetPreset : (panel.planetPresetCombo != null ? panel.planetPresetCombo.getValue() : null);
            }
            @Override public EcologyPreset getActiveEcologyPreset() { return panel.ecologyPresetCombo != null ? panel.ecologyPresetCombo.getValue() : null; }
            @Override public Scenario getScenario() { return panel.getScenario(); }
            @Override public void applyScenarioToUI(Scenario scenario) { panel.applyScenarioToUI(scenario); }
            @Override public void onBundleLoaded(PlanetPreset planet, EcologyPreset ecology) {
                if (planet != null) {
                    panel.activePlanetPreset = planet;
                    if (panel.planetPresetCombo != null) {
                        if (!panel.planetPresetCombo.getItems().contains(planet)) {
                            panel.planetPresetCombo.getItems().add(planet);
                        }
                        panel.planetPresetCombo.setValue(planet);
                    }
                }
                if (ecology != null && panel.ecologyPresetCombo != null) {
                    if (!panel.ecologyPresetCombo.getItems().contains(ecology)) {
                        panel.ecologyPresetCombo.getItems().add(ecology);
                    }
                    panel.ecologyPresetCombo.setValue(ecology);
                }
                if (panel.onScenarioLoadedCallback != null) {
                    panel.onScenarioLoadedCallback.accept(panel.activePlanetPreset, ecology != null ? ecology : EcologyPreset.EARTH_STANDARD);
                }
            }
            @Override public void generatePreview() { panel.generatePreview(); }
            @Override public void showIntegrityWarning(String message) {
                if (panel.validationErrorLabel != null && panel.validationErrorBanner != null) {
                    panel.validationErrorLabel.setText(message);
                    panel.validationErrorBanner.setVisible(true);
                    panel.validationErrorBanner.setManaged(true);
                }
            }
        };
    }

    /**
     * Creates host context and initializes {@link ScenarioSimulationLauncher}.
     *
     * @param panel the hosting scenario setup panel
     * @param scenarioRepo scenario persistence repository
     * @param onStartSimulation start simulation listener callback
     * @return initialized simulation launcher
     */
    static ScenarioSimulationLauncher createSimulationLauncher(ScenarioSetupPanel panel,
                                                              ScenarioRepository scenarioRepo,
                                                              Consumer<Scenario> onStartSimulation) {
        return new ScenarioSimulationLauncher(
            new ScenarioSimulationLauncher.HostContext() {
                @Override public Window getWindow() { return panel.getScene() != null ? panel.getScene().getWindow() : null; }
                @Override public javafx.scene.Node getContainerNode() { return panel; }
                @Override public boolean validateScenarioSetup() { return panel.validateScenarioSetup(); }
                @Override public Scenario getScenario() { return panel.getScenario(); }
                @Override public PlanetPreset getActivePlanetPreset() {
                    return panel.activePlanetPreset != null ? panel.activePlanetPreset : (panel.planetPresetCombo != null ? panel.planetPresetCombo.getValue() : null);
                }
                @Override public EcologyPreset getActiveEcologyPreset() { return panel.ecologyPresetCombo != null ? panel.ecologyPresetCombo.getValue() : null; }
                @Override public Integer getH3Resolution() { return panel.h3ResolutionCombo != null ? panel.h3ResolutionCombo.getValue() : 3; }
                @Override public boolean isClippingActive() { return panel.spatialBoundsBuilder.isClippingEnabled(); }
                @Override public double getClippingMinLat() { return panel.spatialBoundsBuilder.getMinLat(); }
                @Override public double getClippingMaxLat() { return panel.spatialBoundsBuilder.getMaxLat(); }
                @Override public double getClippingMinLng() { return panel.spatialBoundsBuilder.getMinLng(); }
                @Override public double getClippingMaxLng() { return panel.spatialBoundsBuilder.getMaxLng(); }
                @Override public boolean isResumeFromSnapshotSelected() { return panel.isResumeFromSnapshotSelected(); }
                @Override public SaveMetadata getSelectedSnapshotMetadata() { return panel.getSelectedSnapshotMetadata(); }
                @Override public boolean isSimulationCurrentlyRunning() {
                    return panel.isSimulationRunningSupplier != null && Boolean.TRUE.equals(panel.isSimulationRunningSupplier.get());
                }
                @Override public boolean isDirty() { return panel.isDirty(); }
                @Override public boolean promptSaveIfDirty(Window owner) { return panel.promptSaveIfDirty(owner); }
                @Override public void distributeInitialPopulation(List<H3Cell> cells) { panel.distributeInitialPopulation(cells); }
                @Override public void onCellsGenerated(List<H3Cell> cells) { panel.currentPreviewCells = cells; }
                @Override public void markClean() { if (panel.scenarioPresetBar != null) panel.scenarioPresetBar.markClean(); }
                @Override public void drawPreview() { panel.drawPreview(); }
                @Override public void setPreviewStatusText(String text) {
                    if (panel.previewStatusLabel != null) panel.previewStatusLabel.setText(text);
                }
            },
            scenarioRepo,
            scenario -> { if (onStartSimulation != null) onStartSimulation.accept(scenario); }
        );
    }

    /**
     * Creates host context for {@link ScenarioDemographicSectionBuilder}.
     *
     * @param panel the hosting scenario setup panel
     * @return initialized host context
     */
    static ScenarioDemographicSectionBuilder.HostContext createDemographicHostContext(ScenarioSetupPanel panel) {
        return new ScenarioDemographicSectionBuilder.HostContext() {
            @Override public void notifyParamChange() { panel.notifyParamChange(); }
            @Override public void drawPreview() { panel.drawPreview(); }
            @Override public void selectPreviewMode(int modeIndex) {
                if (panel.previewModeCombo != null && panel.previewModeCombo.getSelectionModel().getSelectedIndex() != modeIndex) {
                    panel.previewModeCombo.getSelectionModel().select(modeIndex);
                }
            }
            @Override public List<H3Cell> getPreviewCells() { return panel.getCells(); }
            @Override public void distributeInitialPopulation(List<H3Cell> cells) { panel.distributeInitialPopulation(cells); }
            @Override public Scenario getScenario() { return panel.getScenario(); }
            @Override public PlanetPreset getActivePlanetPreset() {
                return panel.activePlanetPreset != null ? panel.activePlanetPreset : (panel.planetPresetCombo != null ? panel.planetPresetCombo.getValue() : null);
            }
            @Override public Window getWindow() { return panel.getScene() != null ? panel.getScene().getWindow() : null; }
            @Override public boolean isUpdatingFromPreset() { return panel.isUpdatingFromPreset; }
            @Override public long getStartYear() {
                return panel.startYearSpinner != null && panel.startYearSpinner.getValue() != null ? panel.startYearSpinner.getValue().longValue() : -8000L;
            }
            @Override public double computeAutoCapitalFromYear(long year) { return ScenarioThermodynamicAutocalc.computeAutoCapitalFromYear(year); }
            @Override public void generatePreview() { panel.generatePreview(); }
            @Override public void updatePreviewTitleText() { panel.updatePreviewTitleText(); }
            @Override public void updateBottomLegend() { panel.updateBottomLegend(); }
        };
    }

    /**
     * Creates host context for {@link ScenarioCulturalSectionBuilder}.
     *
     * @param panel the hosting scenario setup panel
     * @return initialized host context
     */
    static ScenarioCulturalSectionBuilder.HostContext createCulturalHostContext(ScenarioSetupPanel panel) {
        return new ScenarioCulturalSectionBuilder.HostContext() {
            @Override public void notifyParamChange() { panel.notifyParamChange(); }
            @Override public void drawPreview() { panel.drawPreview(); }
            @Override public void updatePreviewModesCombo() { panel.updatePreviewModesCombo(); }
            @Override public Scenario getScenario() { return panel.getScenario(); }
            @Override public long getStartYear() {
                return panel.startYearSpinner != null && panel.startYearSpinner.getValue() != null ? panel.startYearSpinner.getValue() : -100000L;
            }
            @Override public PlanetPreset getActivePlanetPreset() { return panel.activePlanetPreset; }
            @Override public Window getWindow() { return panel.getScene() != null ? panel.getScene().getWindow() : null; }
            @Override public boolean isUpdatingFromPreset() { return panel.isUpdatingFromPreset; }
            @Override public void selectPreviewMode(int modeIndex) {
                if (panel.previewModeCombo != null && panel.previewModeCombo.getSelectionModel().getSelectedIndex() != modeIndex) {
                    panel.previewModeCombo.getSelectionModel().select(modeIndex);
                }
            }
            @Override public String getScenarioDescription() {
                return panel.scenarioDescriptionArea != null ? panel.scenarioDescriptionArea.getText() : "Ether Scenario";
            }
        };
    }

    /**
     * Creates host context for {@link ScenarioPreviewPaneBuilder}.
     *
     * @param panel the hosting scenario setup panel
     * @return initialized host context
     */
    static ScenarioPreviewPaneBuilder.HostContext createPreviewPaneHostContext(ScenarioSetupPanel panel) {
        return new ScenarioPreviewPaneBuilder.HostContext() {
            @Override public Scenario getScenario() { return panel.getScenario(); }
            @Override public PlanetPreset getActivePlanetPreset() {
                return panel.activePlanetPreset != null ? panel.activePlanetPreset : (panel.planetPresetCombo != null ? panel.planetPresetCombo.getValue() : null);
            }
            @Override public long getStartYear() {
                return panel.startYearSpinner != null && panel.startYearSpinner.getValue() != null ? panel.startYearSpinner.getValue() : -8000L;
            }
            @Override public int getCultureVectorDimensions() {
                return panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getCultureVectorDimensions() : 9;
            }
            @Override public double getCulturalDiffusionRate() {
                return panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getCulturalDiffusionRate() : 0.05;
            }
            @Override public double getCulturalMutationRate() {
                return panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getCulturalMutationRate() : 0.02;
            }
            @Override public Image getCustomTensorImage(int index) {
                return panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getCustomTensorImage(index) : null;
            }
            @Override public boolean isTensorImportActive(int index) {
                return panel.culturalSectionBuilder != null && panel.culturalSectionBuilder.isTensorImportActive(index);
            }
            @Override public double getTensorParam1(int index) {
                return panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getTensorParam1(index) : 1.0;
            }
            @Override public double getTensorParam2(int index) {
                return panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getTensorParam2(index) : 1.0;
            }
            @Override public double getTensorParam3(int index) {
                return panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getTensorParam3(index) : 1.0;
            }
            @Override public long getTensorSeed(int index) {
                return panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getTensorSeed(index) : (11235L + index * 11111L);
            }
            @Override public Color evaluateProceduralTensorColor(int tIdx, double lat, double lon, double elev, double p1, double p2, double p3, long seed) {
                return panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.evaluateProceduralTensorColor(tIdx, lat, lon, elev, p1, p2, p3, seed) : Color.GRAY;
            }
            @Override public String getCulturalTensorTitle(int index) {
                return panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getCulturalTensorTitle(index) : "";
            }
            @Override public String getCulturalTensorPreviewName(int index) {
                return panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getCulturalTensorPreviewName(index) : "";
            }
            @Override public Image getCustomDensityImage() {
                return panel.demographicSectionBuilder != null ? panel.demographicSectionBuilder.getCustomDensityImage() : null;
            }
            @Override public boolean isDensityImportActive() {
                return panel.demographicSectionBuilder != null && panel.demographicSectionBuilder.isDensityImportActive();
            }
            @Override public long getInitialHumanCount() {
                return panel.demographicSectionBuilder != null ? panel.demographicSectionBuilder.getInitialHumanCount() : 1_000_000L;
            }
            @Override public String getDensityPattern() {
                return panel.demographicSectionBuilder != null ? panel.demographicSectionBuilder.getDensityPattern() : "UNBIASED_NATURAL";
            }
            @Override public String getDemoSeed() { return panel.demoSeedField != null ? panel.demoSeedField.getText() : ""; }
            @Override public String getCultSeed() { return panel.cultSeedField != null ? panel.cultSeedField.getText() : ""; }
            @Override public int getH3Resolution() {
                return panel.h3ResolutionCombo != null && panel.h3ResolutionCombo.getValue() != null ? panel.h3ResolutionCombo.getValue() : 3;
            }
            @Override public boolean isUpdatingFromPreset() { return panel.isUpdatingFromPreset; }
            @Override public double computeAutoCapitalFromYear(long year) { return ScenarioThermodynamicAutocalc.computeAutoCapitalFromYear(year); }
            @Override public Window getWindow() { return panel.getScene() != null ? panel.getScene().getWindow() : null; }
            @Override public void notifyParamChange() { panel.notifyParamChange(); }
            @Override public void applySpatialClippingBounds(double minLat, double maxLat, double minLon, double maxLon) {
                panel.spatialBoundsBuilder.applySpatialClippingBounds(minLat, maxLat, minLon, maxLon);
            }
            @Override public boolean isGraphicSelectActive() { return panel.spatialBoundsBuilder.isGraphicSelectActive(); }
            @Override public void setGraphicSelectActive(boolean active) { panel.spatialBoundsBuilder.setGraphicSelectActive(active); }
            @Override public double getClippingMinLat() { return panel.spatialBoundsBuilder.getMinLat(); }
            @Override public double getClippingMaxLat() { return panel.spatialBoundsBuilder.getMaxLat(); }
            @Override public double getClippingMinLng() { return panel.spatialBoundsBuilder.getMinLng(); }
            @Override public double getClippingMaxLng() { return panel.spatialBoundsBuilder.getMaxLng(); }
            @Override public boolean isClippingEnabled() { return panel.spatialBoundsBuilder.isClippingEnabled(); }
            @Override public Image getElevationImageForPreset(PlanetPreset preset) { return panel.getElevationImageForPreset(preset); }
            @Override public void applyPresetMapToCells(List<H3Cell> cells, PlanetPreset cfg) {
                panel.simulationLauncher.applyPresetMapToCells(cells, cfg, panel.getScenario());
            }
            @Override public void onPreviewCellsGenerated(List<H3Cell> cells) { panel.currentPreviewCells = cells; }
        };
    }

    /**
     * Creates host context and initializes {@link ScenarioPresetManager}.
     *
     * @param panel the hosting scenario setup panel
     * @param scenarioRepo scenario persistence repository
     * @return initialized preset manager
     */
    static ScenarioPresetManager createPresetManager(ScenarioSetupPanel panel, ScenarioRepository scenarioRepo) {
        return new ScenarioPresetManager(new ScenarioPresetManager.HostContext() {
            @Override public Window getWindow() { return panel.getScene() != null ? panel.getScene().getWindow() : null; }
            @Override public boolean validateScenarioSetup() { return panel.validateScenarioSetup(); }
            @Override public Scenario getScenario() { return panel.getScenario(); }
            @Override public void applyScenarioToUI(Scenario s) { panel.applyScenarioToUI(s); }
            @Override public void selectPreviewMode(int modeIndex) {
                if (panel.previewModeCombo != null && panel.previewModeCombo.getSelectionModel().getSelectedIndex() != modeIndex) {
                    panel.previewModeCombo.getSelectionModel().select(modeIndex);
                }
            }
            @Override public void refreshSnapshotList() { panel.refreshSnapshotList(); }
            @Override public void refreshSnapshotListForScenario(Scenario s) { panel.refreshSnapshotListForScenario(s); }
            @Override public void updateStartButtonLabel() { panel.updateStartButtonLabel(); }
            @Override public void notifyParamChange() { panel.notifyParamChange(); }
        }, scenarioRepo);
    }
}
